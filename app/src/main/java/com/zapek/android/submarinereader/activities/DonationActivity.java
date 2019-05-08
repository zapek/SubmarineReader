/**
 * Copyright 2017 by David Gerber, Zapek Software Engineering
 * https://zapek.com
 *
 * This file is part of Submarine Reader.
 *
 * Submarine Reader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Submarine Reader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Submarine Reader.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zapek.android.submarinereader.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.Log;
import com.zapek.android.submarinereader.util.iab.IabHelper;
import com.zapek.android.submarinereader.util.iab.IabResult;
import com.zapek.android.submarinereader.util.iab.Inventory;
import com.zapek.android.submarinereader.util.iab.Purchase;
import com.zapek.android.submarinereader.util.iab.SkuDetails;

import java.util.ArrayList;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DonationActivity extends AppCompatActivity implements View.OnClickListener, IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener, IabHelper.OnIabPurchaseFinishedListener, RadioGroup.OnCheckedChangeListener
{
	private final boolean simulate = !BuildConfig.BUILD_TYPE.equals("release");

	private IabHelper iabHelper;
	private RadioGroup paymentGroup;
	private RadioButton coffeeRadio;
	private RadioButton dinnerRadio;
	private RadioButton rentRadio;
	private TextView errorText;
	private ViewGroup donationGroup;
	private ViewGroup thankYouGroup;
	private Button payButton;
	private SharedPreferences sharedPreferences;

	private static final int RC_PURCHASE = 1;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_donation);

		paymentGroup = findViewById(R.id.paymentGroup);
		paymentGroup.setOnCheckedChangeListener(this);
		coffeeRadio = findViewById(R.id.coffee);
		dinnerRadio = findViewById(R.id.dinner);
		rentRadio = findViewById(R.id.rent);
		donationGroup = findViewById(R.id.donationGroup);
		thankYouGroup = findViewById(R.id.thankyouGroup);
		errorText = findViewById(R.id.error);
		payButton = findViewById(R.id.pay);
		payButton.setOnClickListener(this);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (!sharedPreferences.contains(Settings.DONATION_SKU) && !TextUtils.isEmpty(BuildConfig.IAB_KEY))
		{
			iabHelper = new IabHelper(this, BuildConfig.IAB_KEY);
			iabHelper.enableDebugLogging(BuildConfig.logging);
			iabHelper.startSetup(this);
		}
		else
		{
			Log.d("donation already done");
			finish();
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if (iabHelper != null)
		{
			iabHelper.disposeWhenFinished();
			iabHelper = null;
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.pay:
				payButton.setEnabled(false);

				String sku;
				switch (paymentGroup.getCheckedRadioButtonId())
				{
					case R.id.coffee:
						sku = Constants.SKU_COFFEE;
						break;

					case R.id.dinner:
						sku = Constants.SKU_DINNER;
						break;

					case R.id.rent:
						sku = Constants.SKU_RENT;
						break;

					default:
						sku = null;
				}

				if (sku != null)
				{
					try
					{
						if (simulate)
						{
							showError("");

							sharedPreferences.edit().putString(Settings.DONATION_SKU, sku).apply();
							donationGroup.setVisibility(View.GONE);
							thankYouGroup.setVisibility(View.VISIBLE);
						}
						else
						{
							iabHelper.launchPurchaseFlow(this, sku, RC_PURCHASE, this, "");
						}
					}
					catch (IabHelper.IabAsyncInProgressException e)
					{
						Log.d("Async operation already in progress: " + e.getMessage()); /* shouldn't happen (tm) */
					}
				}
				else
				{
					Log.d("missing payment options");
				}
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (iabHelper != null)
		{
			if (iabHelper.handleActivityResult(requestCode, resultCode, data))
			{
				return;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, @IdRes int checkedId)
	{
		switch (group.getId())
		{
			case R.id.paymentGroup:
				if (iabHelper != null || simulate)
				{
					payButton.setEnabled(true);
				}
				break;
		}
	}

	@Override
	public void onIabSetupFinished(IabResult result)
	{
		if (result.isSuccess())
		{
			if (iabHelper != null)
			{
				Log.d("IAB successfully setup");
				try
				{
					ArrayList<String> skus = new ArrayList<>();
					skus.add(Constants.SKU_COFFEE);
					skus.add(Constants.SKU_DINNER);
					skus.add(Constants.SKU_RENT);

					iabHelper.queryInventoryAsync(true, skus, null, this);
				}
				catch (IabHelper.IabAsyncInProgressException e)
				{
					Log.d("Error querying inventory details. Operation already in progress.");
				}
			}
		}
		else
		{
			Log.d("Failed to setup IAB: " + result);
			iabHelper.disposeWhenFinished();
			iabHelper = null;
			if (!simulate)
			{
				showError("Couldn't setup Google Play's In-App billing system");
			}
		}
	}

	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory)
	{
		if (iabHelper != null)
		{
			if (result.isSuccess())
			{
				setPrice(coffeeRadio, R.string.sku_coffee_ask, inventory.getSkuDetails(Constants.SKU_COFFEE));
				setPrice(dinnerRadio, R.string.sku_dinner_ask, inventory.getSkuDetails(Constants.SKU_DINNER));
				setPrice(rentRadio, R.string.sku_rent_ask, inventory.getSkuDetails(Constants.SKU_RENT));
			}
			else
			{
				Log.d("Failed to query inventory for details: " + result);
			}
		}
	}

	private void setPrice(RadioButton view, int resId, SkuDetails skuDetails)
	{
		if (skuDetails != null)
		{
			String price = skuDetails.getPrice();

			if (!TextUtils.isEmpty(price))
			{
				view.setText(getString(resId) + " (" + price + ")");
			}
		}
	}

	@Override
	public void onIabPurchaseFinished(IabResult result, Purchase purchase)
	{
		if (iabHelper != null || simulate)
		{
			if (result.isSuccess() || simulate)
			{
				showError("");

				sharedPreferences.edit().putString(Settings.DONATION_SKU, purchase.getSku()).apply();
				donationGroup.setVisibility(View.GONE);
				thankYouGroup.setVisibility(View.VISIBLE);
			}
			else
			{
				showError(result.getMessage());
			}
		}
	}

	private void showError(String errorMessage)
	{
		if (!TextUtils.isEmpty(errorMessage))
		{
			errorText.setText(errorMessage);
			errorText.setVisibility(View.VISIBLE);
		}
		else
		{
			errorText.setVisibility(View.GONE);
		}
	}
}
