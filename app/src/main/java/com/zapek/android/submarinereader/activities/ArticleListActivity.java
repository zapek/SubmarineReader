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

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchaseState;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.material.navigation.NavigationView;
import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.databinding.ActivityArticlelistBinding;
import com.zapek.android.submarinereader.fragments.ArticleListFragment;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.sync.SyncProgress;
import com.zapek.android.submarinereader.sync.SyncWorker;
import com.zapek.android.submarinereader.util.AlertRequester;
import com.zapek.android.submarinereader.util.ConnectivityUtils;
import com.zapek.android.submarinereader.util.Log;
import com.zapek.android.submarinereader.util.NightModeUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class ArticleListActivity extends AppCompatActivity implements ArticleListFragment.OnItemSelectedListener, NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener, AlertRequester.AlertDialogListener, View.OnClickListener, BillingClientStateListener, PurchasesUpdatedListener, Observer<List<WorkInfo>>
{
	private static final int REQUEST_REVIEW_SETTINGS = 1;
	private static final int REQUEST_DONATE = 2;

	private static final int ACTIVITY_RESULT_SYNC = 1;

	private static final String STATE_NAVIGATION_POSITION = "navigationPosition"; /* we need to save it ourselves to avoid spurious events */
	private int navigationPosition;

	private SyncStatusReceiver syncStatusReceiver;
	private SharedPreferences sharedPreferences;
	private ArticleListFragment articleListFragment;
	private ActionBarDrawerToggle actionBarDrawerToggle;
	private ActivityArticlelistBinding binding;
	private boolean nightMode;

	private BillingClient billingClient;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		binding = DataBindingUtil.setContentView(this, R.layout.activity_articlelist);

		setSupportActionBar(binding.toolbar);

		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		articleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.articles);

		nightMode = NightModeUtils.isInNightMode(this);

		ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.navigation, R.layout.support_simple_spinner_dropdown_item);
		arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		binding.spinner.setAdapter(arrayAdapter);
		binding.spinner.setOnItemSelectedListener(this);

		actionBarDrawerToggle = new ActionBarDrawerToggle(
			this,
			binding.drawerLayout,
			R.string.navigation_drawer_open,
			R.string.navigation_drawer_close
		)
		{
			@Override
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
			}

			@Override
			public void onDrawerClosed(View drawerView)
			{
				super.onDrawerClosed(drawerView);
			}
		};
		binding.drawerLayout.addDrawerListener(actionBarDrawerToggle);

		binding.navView.setNavigationItemSelectedListener(this);

		View headerView = binding.navView.getHeaderView(0);
		ImageView nightButton = headerView.findViewById(R.id.night);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			nightButton.setImageResource(NightModeUtils.isInNightMode(this) ? R.drawable.ic_nightmode_day_24dp : R.drawable.ic_nightmode_night_24dp);
			nightButton.setOnClickListener(this);
		}
		else
		{
			nightButton.setVisibility(View.GONE);
		}

		LiveData<List<WorkInfo>> savedWorkInfo = WorkManager.getInstance(this).getWorkInfosByTagLiveData(SyncWorker.SYNC_TAG);
		savedWorkInfo.observe(this, this);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPreferences.getBoolean(Settings.NETWORK_SETTINGS_REVIEWED, Settings.NETWORK_SETTINGS_REVIEWED_DEFAULT))
		{
			setAutoSync(); /* to stay on the safe side */
		}
		else
		{
			if (ConnectivityUtils.hasGoodConnection(this))
			{
				sharedPreferences.edit().putBoolean(Settings.NETWORK_SETTINGS_REVIEWED, true).apply();
				setAutoSync();
				articleListFragment.syncArticles();
			}
			else
			{
				AlertRequester.confirm(this, getString(R.string.alert_slow_network), getString(R.string.alert_slow_network_review), getString(R.string.alert_slow_network_not_now), REQUEST_REVIEW_SETTINGS, 0);
			}
		}

		if (savedInstanceState != null)
		{
			navigationPosition = savedInstanceState.getInt(STATE_NAVIGATION_POSITION);
		}

		if (BuildConfig.enableDonations && !sharedPreferences.contains(Settings.DONATION_SKU))
		{
			billingClient = BillingClient.newBuilder(this)
				.setListener(this)
				.enablePendingPurchases()
				.build();
			billingClient.startConnection(this);
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
	public void onDialogPositiveClick(DialogFragment dialog, int returnCode, long userData)
	{
		switch (returnCode)
		{
			case REQUEST_REVIEW_SETTINGS:
			{
				sharedPreferences.edit().putBoolean(Settings.NETWORK_SETTINGS_REVIEWED, true).apply();

				Intent intent = new Intent(this, NetworkSettingsActivity.class);
				startActivityForResult(intent, ACTIVITY_RESULT_SYNC);
			}
			break;

			case REQUEST_DONATE:
			{
				sharedPreferences.edit().putLong(Settings.DONATION_INSTALL_TIME, 0).apply(); /* shut it */

				Intent donationIntent = new Intent(this, DonationActivity.class);
				startActivity(donationIntent);
			}
			break;
		}
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog, int returnCode, long userData)
	{
		switch (returnCode)
		{
			case REQUEST_REVIEW_SETTINGS:
			{
				setAutoSync();
				articleListFragment.syncArticles();
			}
			break;

			case REQUEST_DONATE:
			{
				sharedPreferences.edit().putLong(Settings.DONATION_INSTALL_TIME, 0).apply(); /* shut it */
			}
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == ACTIVITY_RESULT_SYNC)
		{
			setAutoSync();
			if (sharedPreferences.getBoolean("autoSync", true))
			{
				articleListFragment.syncArticles();
			}
		}
		else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void setAutoSync()
	{
		SyncWorker.setAutoSync(sharedPreferences.getBoolean("autoSync", true));

	}

	@Override
	public void onBackPressed()
	{
		if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
		{
			binding.drawerLayout.closeDrawer(GravityCompat.START);
		}
		else
		{
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_NAVIGATION_POSITION, navigationPosition);
	}

	@Override
	public void onItemSelected(Uri entryUri)
	{
		Intent intent = new Intent(this, ArticleDetailActivity.class);
		intent.setData(entryUri);
		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		return actionBarDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		actionBarDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		if (nightMode != NightModeUtils.isInNightMode(this))
		{
			recreate();
		}
		syncStatusReceiver = new SyncStatusReceiver();

		IntentFilter filter = new IntentFilter();
		filter.addAction(SyncProgress.ACTION_SYNC_PROGRESS);
		LocalBroadcastManager.getInstance(this).registerReceiver(syncStatusReceiver, filter);
	}

	@Override
	public void onChanged(List<WorkInfo> workInfos)
	{
		boolean running = false;

		/*
		 * We iterate all WorkInfos here because there might
		 * be 2 (autosync and immediate sync). As soon as we
		 * detect that one of them is in the RUNNING state, it
		 * means a sync is happening.
		 */
		for (WorkInfo workInfo : workInfos)
		{
			switch (workInfo.getState())
			{
				case RUNNING:
					running = true;
					break;
			}
		}
		articleListFragment.setSyncStatus(running);
	}

	private class SyncStatusReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
				case SyncProgress.ACTION_SYNC_PROGRESS:
					articleListFragment.setSyncProgress(intent.getIntExtra(SyncProgress.SYNC_CURRENT, 0), intent.getIntExtra(SyncProgress.SYNC_TOTAL, 0));
					break;

				default:
					Log.d("missing action for " + intent.getAction());
					break;
			}
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if (syncStatusReceiver != null)
		{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(syncStatusReceiver);
			syncStatusReceiver = null;
		}
		nightMode = NightModeUtils.isInNightMode(this);
	}

	@SuppressLint("NonConstantResourceId")
	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.nav_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				break;

			case R.id.nav_about:
				startActivity(new Intent(this, AboutActivity.class));
				break;
		}
		binding.drawerLayout.closeDrawer(GravityCompat.START);

		return false;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		articleListFragment.setNavigationMode(position);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
		/* not used */
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.night:
				AppCompatDelegate.setDefaultNightMode(NightModeUtils.isInNightMode(this) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
				recreate();
				break;
		}
	}

	@Override
	public void onBillingSetupFinished(BillingResult billingResult)
	{
		if (billingResult.getResponseCode() == BillingResponseCode.OK)
		{
			billingClient.queryPurchasesAsync(
				QueryPurchasesParams.newBuilder()
					.setProductType(BillingClient.ProductType.INAPP)
					.build(),
				(purchaseResult, purchases) -> {
					if (purchaseResult.getResponseCode() == BillingResponseCode.OK)
					{
						String sku = null;

						Log.d("Billing purchase query successful");

						for (Purchase purchase : purchases)
						{
							if (purchase.getPurchaseState() == PurchaseState.PURCHASED)
							{
								sku = purchase.getProducts().get(0);
							}
						}

						if (sku != null)
						{
							sharedPreferences.edit().putString(Settings.DONATION_SKU, sku).apply();
						}
						else
						{
							if (sharedPreferences.contains(Settings.DONATION_INSTALL_TIME))
							{
								long installTime = sharedPreferences.getLong(Settings.DONATION_INSTALL_TIME, 0);
								if (installTime != 0)
								{
									if (Math.abs(System.currentTimeMillis() - installTime) > DateUtils.WEEK_IN_MILLIS * 2)
									{
										AlertRequester.confirm(this, getString(R.string.donation_requester), getString(R.string.donation_requester_positive), getString(R.string.donation_requester_negative), REQUEST_DONATE, 0);
									}
								}
							}
							else
							{
								sharedPreferences.edit().putLong(Settings.DONATION_INSTALL_TIME, System.currentTimeMillis()).apply();
							}
						}
					}
					else
					{
						Log.d("Failed to get billing purchases, code: " + purchaseResult.getResponseCode() + ", message: " + purchaseResult.getDebugMessage());
					}
				}
			);
		}
		else
		{
			Log.d("Failed to setup Billing Client, code: " + billingResult.getResponseCode() + ", message: " + billingResult.getDebugMessage());
		}
	}

	@Override
	public void onBillingServiceDisconnected()
	{
		/* we don't retry the connection because it's not a critical feature */
	}

	@Override
	public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases)
	{
		Log.d("Unhandled purchase updates");
	}
}
