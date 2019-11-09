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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.activities.DonationActivity;
import com.zapek.android.submarinereader.activities.NetworkSettingsActivity;
import com.zapek.android.submarinereader.application.Init;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.NightModeUtils;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsFragment extends PreferenceFragment
{

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		if (!Init.isCrashReporterEnabled())
		{
			Preference preference = findPreference("sendLog");

			if (preference != null)
			{
				getPreferenceScreen().removePreference(preference);
			}
		}

		Intent intent = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE, null, getActivity(), NetworkSettingsActivity.class);
		List<ResolveInfo> resolveInfos = getActivity().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfos.size() == 0)
		{
			Preference preference = findPreference("networkSettings");
			if (preference != null)
			{
				getPreferenceScreen().removePreference(preference);
			}
		}

		if (!BuildConfig.enableDonations || getPreferenceManager().getSharedPreferences().contains(Settings.DONATION_SKU))
		{
			Preference preference = findPreference("donation");
			if (preference != null)
			{
				getPreferenceScreen().removePreference(preference);
			}
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			Preference preference = findPreference("autoNightMode");
			if (preference != null)
			{
				getPreferenceScreen().removePreference(preference);
			}
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		switch (preference.getKey())
		{
			case "sendLog":
				Init.sendCrashReport();
				break;

			case "networkSettings":
				Intent networkSettingsIntent = new Intent(getActivity(), NetworkSettingsActivity.class);
				startActivity(networkSettingsIntent);
				break;

			case "donation":
				Intent donationIntent = new Intent(getActivity(), DonationActivity.class);
				startActivity(donationIntent);
				break;

			case "autoNightMode":
				boolean autoNightMode = preference.getSharedPreferences().getBoolean(Settings.AUTO_NIGHT_MODE, Settings.AUTO_NIGHT_MODE_DEFAULT);
				AppCompatDelegate.setDefaultNightMode(autoNightMode ? NightModeUtils.getDefaultMode() : AppCompatDelegate.MODE_NIGHT_NO);
				getActivity().recreate();
				break;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}
