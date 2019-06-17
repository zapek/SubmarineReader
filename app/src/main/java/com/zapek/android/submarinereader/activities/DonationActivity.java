/**
 * Copyright 2017 by David Gerber, Zapek Software Engineering
 * https://zapek.com
 * <p>
 * This file is part of Submarine Reader.
 * <p>
 * Submarine Reader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Submarine Reader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Submarine Reader.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zapek.android.submarinereader.activities;

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

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DonationActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, PurchasesUpdatedListener, BillingClientStateListener, SkuDetailsResponseListener, AcknowledgePurchaseResponseListener
{
	private final boolean simulate = false;//!BuildConfig.BUILD_TYPE.equals("release");

	private RadioGroup paymentGroup;
	private RadioButton coffeeRadio;
	private RadioButton dinnerRadio;
	private RadioButton rentRadio;
	private TextView errorText;
	private ViewGroup donationGroup;
	private ViewGroup thankYouGroup;
	private Button payButton;
	private SharedPreferences sharedPreferences;
	private boolean hasError;

	private BillingClient billingClient;
	private List<SkuDetails> skuDetailsList;

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

		if (BuildConfig.enableDonations && !sharedPreferences.contains(Settings.DONATION_SKU))
		{
			billingClient = BillingClient.newBuilder(this)
				.setListener(this)
				.enablePendingPurchases()
				.build();
			billingClient.startConnection(this);
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

		if (billingClient != null && billingClient.isReady())
		{
			billingClient.endConnection();
			billingClient = null;
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
					if (simulate)
					{
						showError("");

						sharedPreferences.edit().putString(Settings.DONATION_SKU, sku).apply();
						donationGroup.setVisibility(View.GONE);
						thankYouGroup.setVisibility(View.VISIBLE);
					}
					else
					{
						SkuDetails skuDetails = getSkuDetails(sku);
						if (skuDetails != null)
						{
							BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
								.setSkuDetails(skuDetails)
								.build();

							BillingResult result = billingClient.launchBillingFlow(this, billingFlowParams);
							if (result.getResponseCode() != BillingResponseCode.OK)
							{
								Log.d("Failure to launch the billing flow, code: " + result.getResponseCode() + ", message: " + result.getDebugMessage());
								showError("Couldn't launch Play Store (-1)");
							}
						}
						else
						{
							Log.d("Missing SKU in the Play Store list, id: " + sku);
							showError("Failure to purchase (-2)");
						}
					}
				}
				else
				{
					Log.d("missing payment options");
					showError("Failure to purchase (-3)");
				}
				break;
		}
	}

	private SkuDetails getSkuDetails(String id)
	{
		if (skuDetailsList != null)
		{
			for (SkuDetails skuDetails : skuDetailsList)
			{
				if (id.equals(skuDetails.getSku()))
				{
					return skuDetails;
				}
			}
		}
		return null;
	}

	@Override
	public void onCheckedChanged(RadioGroup group, @IdRes int checkedId)
	{
		switch (group.getId())
		{
			case R.id.paymentGroup:
				if ((billingClient != null && billingClient.isReady() && !hasError) || simulate)
				{
					payButton.setEnabled(true);
				}
				break;
		}
	}

	@Override
	public void onBillingSetupFinished(BillingResult billingResult)
	{
		if (billingResult.getResponseCode() == BillingResponseCode.OK)
		{
			Log.d("Billing client successfully setup, querying list");

			ArrayList<String> skus = new ArrayList<>();
			skus.add(Constants.SKU_COFFEE);
			skus.add(Constants.SKU_DINNER);
			skus.add(Constants.SKU_RENT);

			SkuDetailsParams.Builder skuParams = SkuDetailsParams.newBuilder();
			skuParams.setSkusList(skus).setType(SkuType.INAPP);
			billingClient.querySkuDetailsAsync(skuParams.build(), this);
		}
		else
		{
			Log.d("Failed to setup Billing Client, code: " + billingResult.getResponseCode() + ", message: " + billingResult.getDebugMessage());

			if (!simulate)
			{
				showError("Couldn't setup Google Play's In-App billing system (" + billingResult.getResponseCode() + ")");
			}
		}
	}

	@Override
	public void onBillingServiceDisconnected()
	{
		/* XXX: we should retry the connection */
	}

	@Override
	public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList)
	{
		if (billingResult.getResponseCode() == BillingResponseCode.OK)
		{
			this.skuDetailsList = skuDetailsList;

			for (SkuDetails skuDetails : skuDetailsList)
			{
				switch (skuDetails.getSku())
				{
					case Constants.SKU_COFFEE:
						setPrice(coffeeRadio, R.string.sku_coffee_ask, skuDetails);
						break;

					case Constants.SKU_DINNER:
						setPrice(dinnerRadio, R.string.sku_dinner_ask, skuDetails);
						break;

					case Constants.SKU_RENT:
						setPrice(rentRadio, R.string.sku_rent_ask, skuDetails);
						break;

					default:
						Log.d("Unhandled SKU: " + skuDetails.getSku());
						break;
				}
			}
		}
		else
		{
			Log.d("Failed to get billing sku list, code: " + billingResult.getResponseCode() + ", message: " + billingResult.getDebugMessage());
			showError("Failed to get price list (-4)");
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
	public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases)
	{
		if (billingResult.getResponseCode() == BillingResponseCode.OK && purchases != null)
		{
			String sku = null;

			for (Purchase purchase : purchases)
			{
				if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
				{
					sku = purchase.getSku(); /* normally he can only purchase one thing at once */

					if (!purchase.isAcknowledged())
					{
						AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
							.setPurchaseToken(purchase.getPurchaseToken())
							.build();
						billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
					}
				}
			}

			if (sku != null)
			{
				showError("");

				sharedPreferences.edit().putString(Settings.DONATION_SKU, sku).apply();
				donationGroup.setVisibility(View.GONE);
				thankYouGroup.setVisibility(View.VISIBLE);
			}
			else
			{
				showError("Purchase failed");
			}
		}
		else if (billingResult.getResponseCode() == BillingResponseCode.USER_CANCELED)
		{
			showError("Purchase canceled");
		}
		else
		{
			Log.d("Purchase failed, message: " + billingResult.getDebugMessage());
			showError("Purchase failed (" + billingResult.getResponseCode() + ")");
		}
	}

	@Override
	public void onAcknowledgePurchaseResponse(BillingResult billingResult)
	{
		if (billingResult.getResponseCode() != BillingResponseCode.OK)
		{
			Log.d("Purchase acknowledgment failed, code: " + billingResult.getResponseCode() + ", message: " + billingResult.getDebugMessage());
		}
	}

	private void showError(String errorMessage)
	{
		if (!TextUtils.isEmpty(errorMessage))
		{
			errorText.setText(errorMessage);
			errorText.setVisibility(View.VISIBLE);
			hasError = true;
		}
		else
		{
			errorText.setVisibility(View.GONE);
			hasError = false;
		}
	}
}
