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

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.fragments.ArticleListFragment;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.sync.SyncProgress;
import com.zapek.android.submarinereader.util.AlertRequester;
import com.zapek.android.submarinereader.util.Log;
import com.zapek.android.submarinereader.util.SyncUtils;
import com.zapek.android.submarinereader.util.iab.IabHelper;
import com.zapek.android.submarinereader.util.iab.IabResult;
import com.zapek.android.submarinereader.util.iab.Inventory;
import com.zapek.android.submarinereader.util.iab.Purchase;

public class ArticleListActivity extends AppCompatActivity implements ArticleListFragment.OnItemSelectedListener, NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener, AlertRequester.AlertDialogListener, IabHelper.OnIabSetupFinishedListener, IabHelper.QueryInventoryFinishedListener
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
	private DrawerLayout drawerLayout;

	private IabHelper iabHelper;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_articlelist);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		articleListFragment = (ArticleListFragment) getSupportFragmentManager().findFragmentById(R.id.articles);

		Spinner spinner = (Spinner) toolbar.findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, R.array.navigation, R.layout.support_simple_spinner_dropdown_item);
		arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		spinner.setAdapter(arrayAdapter);
		spinner.setOnItemSelectedListener(this);

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		actionBarDrawerToggle = new ActionBarDrawerToggle(
			this,
			drawerLayout,
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
		drawerLayout.addDrawerListener(actionBarDrawerToggle);

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPreferences.getBoolean(Settings.SHOW_NETWORK_SETTINGS, false))
		{
			AlertRequester.confirm(this, getString(R.string.alert_slow_network), getString(R.string.alert_slow_network_review), getString(R.string.alert_slow_network_not_now), REQUEST_REVIEW_SETTINGS, 0);
		}

		if (savedInstanceState != null)
		{
			navigationPosition = savedInstanceState.getInt(STATE_NAVIGATION_POSITION);
		}

		if (!sharedPreferences.contains(Settings.DONATION_SKU) && !TextUtils.isEmpty(BuildConfig.IAB_KEY))
		{
			iabHelper = new IabHelper(this, BuildConfig.IAB_KEY);
			iabHelper.enableDebugLogging(BuildConfig.logging);
			iabHelper.startSetup(this);
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
	public void onDialogPositiveClick(DialogFragment dialog, int returnCode, long userData)
	{
		switch (returnCode)
		{
			case REQUEST_REVIEW_SETTINGS:
			{
				sharedPreferences.edit().putBoolean(Settings.SHOW_NETWORK_SETTINGS, false).apply();

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
				sharedPreferences.edit().putBoolean(Settings.SHOW_NETWORK_SETTINGS, false).apply();
				SyncUtils.setSyncedAutomatically(true);
				SyncUtils.manualSync();
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
			if (SyncUtils.isSyncedAutomatically())
			{
				SyncUtils.manualSync();
			}
		}
		else
		{
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onBackPressed()
	{
		if (drawerLayout.isDrawerOpen(Gravity.START))
		{
			drawerLayout.closeDrawer(Gravity.START);
		}
		else
		{
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
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
	public boolean onOptionsItemSelected(MenuItem item)
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
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		actionBarDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		syncStatusReceiver = new SyncStatusReceiver();

		IntentFilter filter = new IntentFilter();
		filter.addAction(SyncProgress.ACTION_SYNC_START);
		filter.addAction(SyncProgress.ACTION_SYNC_PROGRESS);
		filter.addAction(SyncProgress.ACTION_SYNC_STOP);
		LocalBroadcastManager.getInstance(this).registerReceiver(syncStatusReceiver, filter);

		articleListFragment.setSyncStatus(SyncProgress.isSyncing(this));
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
	}

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
		drawerLayout.closeDrawer(Gravity.START);

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

	private class SyncStatusReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent)
		{
			switch (intent.getAction())
			{
				case SyncProgress.ACTION_SYNC_START:
					articleListFragment.setSyncStatus(true);
					break;

				case SyncProgress.ACTION_SYNC_STOP:
					articleListFragment.setSyncStatus(false);
					break;

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
	public void onIabSetupFinished(IabResult result)
	{
		if (result.isSuccess())
		{
			if (iabHelper != null)
			{
				try
				{
					iabHelper.queryInventoryAsync(this);
				}
				catch (IabHelper.IabAsyncInProgressException e)
				{
					Log.d("Error querying inventory. Operation already in progress.");
				}
			}
		}
		else
		{
			Log.d("Failed to setup IAB: " + result);
		}
	}

	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory)
	{
		if (iabHelper != null)
		{
			if (result.isSuccess())
			{
				Log.d("Inventory query successful.");

				Purchase coffePurchase = inventory.getPurchase(Constants.SKU_COFFEE);
				Purchase dinnerPurchase = inventory.getPurchase(Constants.SKU_DINNER);
				Purchase rentPurchase = inventory.getPurchase(Constants.SKU_RENT);

				String sku;

				if (rentPurchase != null)
				{
					sku = Constants.SKU_RENT;
				}
				else if (dinnerPurchase != null)
				{
					sku = Constants.SKU_DINNER;
				}
				else if (coffePurchase != null)
				{
					sku = Constants.SKU_COFFEE;
				}
				else
				{
					sku = null;
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
				Log.d("Failed to query inventory: " + result);
			}
		}
	}
}
