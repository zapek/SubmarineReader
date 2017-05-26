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

package com.zapek.android.submarinereader.application;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.ConnectivityUtils;
import com.zapek.android.submarinereader.util.Log;
import com.zapek.android.submarinereader.util.SyncUtils;

public class Application extends android.app.Application
{
	static
	{
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO); /* XXX: make that settable */
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		Account account = new Account(BuildConfig.accountName, BuildConfig.accountType);
		AccountManager accountManager = AccountManager.get(this);
		if (accountManager.addAccountExplicitly(account, null, null))
		{
			Log.d("account added successfully!");

			/*
			 * Try to sync immediately if the connection is good.
			 */
			if (ConnectivityUtils.hasGoodConnection(this))
			{
				/*
			     * Enable syncing (like if the user ticked it in the prefs).
			     */
				SyncUtils.setSyncedAutomatically(true);
				SyncUtils.manualSync();
			}
			else
			{
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putBoolean(Settings.SHOW_NETWORK_SETTINGS, true);
				editor.apply();
			}
		}
	}

	@Override
	protected void attachBaseContext(Context base)
	{
		super.attachBaseContext(base);

		Init.initCrashReporter(this);
	}
}
