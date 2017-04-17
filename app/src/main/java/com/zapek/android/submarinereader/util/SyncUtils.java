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

package com.zapek.android.submarinereader.util;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

import com.zapek.android.submarinereader.BuildConfig;

public class SyncUtils
{
	private static Account getAccount()
	{
		return new Account(BuildConfig.accountName, BuildConfig.accountType);
	}

	public static void setSyncedAutomatically(boolean enabled)
	{
		ContentResolver.setSyncAutomatically(getAccount(), BuildConfig.providerAuthority, enabled);
	}

	public static void manualSync()
	{
		Bundle params = new Bundle();
		params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
		ContentResolver.requestSync(getAccount(), BuildConfig.providerAuthority, params);
	}

	public static boolean isSyncedAutomatically()
	{
		return ContentResolver.getSyncAutomatically(getAccount(), BuildConfig.providerAuthority);
	}
}
