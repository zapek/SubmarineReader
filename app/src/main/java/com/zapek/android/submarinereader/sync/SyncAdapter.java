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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.zapek.android.submarinereader.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter
{
	private final Context context;

	public SyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);
		this.context = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
	{
		Log.d("performing sync...");
		SyncProgress.syncStart(context);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		WordpressClient wordpressClient = new WordpressClient(context, provider, syncResult, prefs);
		wordpressClient.syncPosts();

		Log.d("syncing finished. " + syncResult);
		SyncProgress.syncStop(context);
	}
}
