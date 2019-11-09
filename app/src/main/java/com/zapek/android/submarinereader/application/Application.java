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

import android.content.Context;
import android.preference.PreferenceManager;

import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.NightModeUtils;

import androidx.appcompat.app.AppCompatDelegate;

public class Application extends android.app.Application
{
	@Override
	public void onCreate()
	{
		super.onCreate();

		boolean autoNightMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Settings.AUTO_NIGHT_MODE, Settings.AUTO_NIGHT_MODE_DEFAULT);
		AppCompatDelegate.setDefaultNightMode(autoNightMode ? NightModeUtils.getDefaultMode() : AppCompatDelegate.MODE_NIGHT_NO);
	}

	@Override
	protected void attachBaseContext(Context base)
	{
		super.attachBaseContext(base);

		Init.initCrashReporter(this);
	}
}
