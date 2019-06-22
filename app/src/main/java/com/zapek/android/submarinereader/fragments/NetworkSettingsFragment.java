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

package com.zapek.android.submarinereader.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.sync.SyncWorker;

import androidx.annotation.Nullable;

public class NetworkSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.network_settings);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		switch (key)
		{
			case "autoSync":
				SyncWorker.setSyncArticlesAutomatically(sharedPreferences.getBoolean("autoSync", true)); /* not in Settings as a constant because it must not be used directly */
				break;

			case Settings.SYNC_IMAGES:
				sharedPreferences.edit().putBoolean(Settings.SYNC_FORCE_UPDATE, true).apply(); /* the next sync will refresh everything */
				break;
		}
	}

	@Override
	public void onDestroy()
	{
		SyncWorker.setSyncArticlesAutomatically(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("autoSync", true));
		super.onDestroy();
	}
}
