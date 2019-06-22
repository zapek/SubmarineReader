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

package com.zapek.android.submarinereader.sync;

import android.content.Context;
import android.content.Intent;

import com.zapek.android.submarinereader.BuildConfig;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SyncProgress
{
	public static final String ACTION_SYNC_PROGRESS = BuildConfig.APPLICATION_ID + ".SYNC_PROGRESS";
	public static final String SYNC_CURRENT = "sync_current";
	public static final String SYNC_TOTAL = "sync_total";

	public static void syncProgress(Context context, int current, int total)
	{
		Intent intent = new Intent(ACTION_SYNC_PROGRESS);
		intent.putExtra(SYNC_CURRENT, current);
		intent.putExtra(SYNC_TOTAL, total);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
