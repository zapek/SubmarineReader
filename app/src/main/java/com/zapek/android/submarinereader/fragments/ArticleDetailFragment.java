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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.db.tables.PostColumns;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.JavaScriptInterface;
import com.zapek.android.submarinereader.widgets.CustomWebChromeClient;
import com.zapek.android.submarinereader.widgets.CustomWebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, CustomWebChromeClient.Listener
{
	private static final int LOCAL_LOADER = 0;

	private Uri articleUri;
	private boolean starred;

	private WebView webView;

	private String shareUrl;

	private final static String[] PROJECTION = {
		PostColumns.TITLE,
		PostColumns.CREATED,
		PostColumns.MODIFIED,
		PostColumns.CONTENT,
		PostColumns.LINK,
		PostColumns.STARRED
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_articledetail, container, false);
		webView = view.findViewById(R.id.webView);
		return view;
	}

	@SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		boolean directNetwork = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(Settings.DIRECT_NETWORK, Settings.DIRECT_NETWORK_DEFAULT);

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDomStorageEnabled(directNetwork);
		webSettings.setBlockNetworkLoads(!directNetwork);
		webSettings.setCacheMode(directNetwork ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
		webView.setWebViewClient(new CustomWebViewClient());
		webView.setWebChromeClient(new CustomWebChromeClient(this));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			webView.addJavascriptInterface(new JavaScriptInterface(view.getContext()), "SubmarineReader");
		}
		webView.setBackgroundColor(Color.TRANSPARENT); /* avoids flickers in night mode */
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		LoaderManager.getInstance(this).initLoader(LOCAL_LOADER, null, this);
	}

	public void setArticleUri(Uri articleUri)
	{
		this.articleUri = articleUri;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		switch (id)
		{
			case LOCAL_LOADER:
				return (new CursorLoader(
					getActivity(),
					articleUri,
					PROJECTION,
					null,
					null,
					null
				));
		}
		return null;
	}

	private static void updateStarredMenu(Menu menu, boolean isStarred)
	{
		MenuItem menuItem = menu.findItem(R.id.starred);
		if (isStarred)
		{
			menuItem.setIcon(R.drawable.ic_star_black_24dp);
		}
		else
		{
			menuItem.setIcon(R.drawable.ic_star_border_black_24dp);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_articledetail, menu);
		updateStarredMenu(menu, starred);
	}

	private Intent createShareIntent()
	{
		ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(getActivity());
		intentBuilder.setChooserTitle(getString(R.string.share_title));
		intentBuilder.setSubject(getString(R.string.share_subject));
		intentBuilder.setText(getString(R.string.share_text, shareUrl));
		intentBuilder.setType("text/plain");
		return intentBuilder.getIntent();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.starred:
			{
				toggleStarred();
				return true;
			}

			case R.id.share:
				startActivity(Intent.createChooser(createShareIntent(), getString(R.string.share_action)));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void toggleStarred()
	{
		starred = !starred;
		Activity activity = getActivity();
		activity.invalidateOptionsMenu();

		Toast.makeText(activity, starred ? R.string.starred : R.string.unstarred, Toast.LENGTH_SHORT).show();
		ContentValues values = new ContentValues(1);
		values.put(PostColumns.STARRED, starred);
		activity.getContentResolver().update(articleUri, values, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		if (cursor.moveToFirst())
		{
			String content;

			try
			{
				final String title = cursor.getString(cursor.getColumnIndex(PostColumns.TITLE));
				/*final String*/ content = cursor.getString(cursor.getColumnIndex(PostColumns.CONTENT));

				starred = cursor.getInt(cursor.getColumnIndex(PostColumns.STARRED)) == 1;

				shareUrl = cursor.getString(cursor.getColumnIndex(PostColumns.LINK));

				getActivity().setTitle(title);

				String subTitle = getCreatedModifiedString(getContext(), cursor.getLong(cursor.getColumnIndex(PostColumns.CREATED)), cursor.getLong(cursor.getColumnIndex(PostColumns.MODIFIED)));
				((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(subTitle);
			}
			catch (IllegalStateException e)
			{
				content = "Article size exceeded (will be fixed in a later release, sorry)";
			}

			/*
			 * The following allows to set the encoding and open local files (assets).
			 */
			webView.loadDataWithBaseURL("file:///", content, "text/html", "UTF-8", null);

			getActivity().invalidateOptionsMenu();
		}
	}

	private static String getCreatedModifiedString(Context context, long created, long modified)
	{
		if (modified > 0 && created > 0 && created != modified)
		{
			return context.getString(R.string.modified, DateUtils.formatDateTime(null, created, 0), DateUtils.formatDateTime(null, modified, 0));
		}
		else
		{
			return DateUtils.formatDateTime(null, created, 0);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{

	}

	@Override
	public void onLoading(int progress)
	{

	}
}
