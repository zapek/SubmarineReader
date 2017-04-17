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

package com.zapek.android.submarinereader.widgets;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.Log;

public class CustomWebViewClient extends WebViewClient
{
	private boolean loaded;

	/*
	 * This is only called when the user click on some links (and also for
	 * iframes, which is why we have to mess around)
	 */
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url)
	{
		if (loaded)
		{
			Log.d("loading url: " + url);
			Uri uri = Uri.parse(url);

			view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));

			return true;
		}
		return !PreferenceManager.getDefaultSharedPreferences(view.getContext()).getBoolean(Settings.DIRECT_NETWORK, Settings.DIRECT_NETWORK_DEFAULT);
	}

	@Override
	public void onPageFinished(WebView view, String url)
	{
		loaded = true;
	}

	@Deprecated
	@Override
	public void onReceivedError(WebView webView, int errorCode, String description, String failingUrl)
	{
		Log.d("received error: " + errorCode + ", description: " + description + ", for url: " + failingUrl);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><center>Error ");
		sb.append(errorCode);
		sb.append("</center><br><center>");
		sb.append(description);
		sb.append("</center>");
		if (BuildConfig.DEBUG)
		{
			sb.append("<br><center>");
			sb.append(failingUrl);
			sb.append("</center>");
		}
		sb.append("</body></html>");

		webView.loadData(sb.toString(), "text/html; charset=UTF-8", null);
	}
}
