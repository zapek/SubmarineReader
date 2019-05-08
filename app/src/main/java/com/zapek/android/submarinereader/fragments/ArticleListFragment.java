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

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.core.view.MenuItemCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.db.DataProvider;
import com.zapek.android.submarinereader.db.tables.PostColumns;
import com.zapek.android.submarinereader.sync.SyncProgress;
import com.zapek.android.submarinereader.util.ConnectivityUtils;
import com.zapek.android.submarinereader.util.Filter;
import com.zapek.android.submarinereader.util.Log;
import com.zapek.android.submarinereader.util.SyncUtils;

import java.io.File;
import java.util.Arrays;

public class ArticleListFragment extends AbsListFragment implements SearchView.OnQueryTextListener, SimpleCursorAdapter.ViewBinder, LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener, View.OnClickListener
{
	public interface OnItemSelectedListener
	{
		void onItemSelected(Uri entryUri);
	}

	private static final int LIST_LOADER = 1;

	private TextView count;
	private ViewGroup empty;
	private TextView emptyText;
	private ProgressBar progressBar;
	private Button syncButton;
	private View progressContainer;
	private View listContainer;
	private SwipeRefreshLayout swipeRefreshLayout;

	private static final String STATE_MODE = "mode";
	private int mode;

	private static final String STATE_FILTER = "filter";
	private Filter filter;

	private OnItemSelectedListener listener;

	private final static String[] FROM_COLUMNS = {
		PostColumns._ID,
		PostColumns.CREATED,
		PostColumns.TITLE,
		PostColumns.EXCERPT
	};

	private final static int[] TO_IDS = {
		R.id.image,
		R.id.date,
		R.id.title,
		R.id.excerpt
	};

	private final String[] PROJECTION = {
		PostColumns._ID,
		PostColumns.CREATED,
		PostColumns.TITLE,
		PostColumns.EXCERPT
	};

	private SimpleCursorAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_articlelist, container, false);
		count = view.findViewById(R.id.count);
		empty = view.findViewById(android.R.id.empty);
		emptyText = view.findViewById(R.id.emptyText);
		progressContainer = view.findViewById(R.id.progressContainer);
		progressBar = view.findViewById(R.id.progressBar);
		syncButton = view.findViewById(R.id.sync);
		listContainer = view.findViewById(R.id.listContainer);
		swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
		return view;
	}

	@Override
	public void onAttach(Context context)
	{
		super.onAttach(context);
		try
		{
			listener = (OnItemSelectedListener) context;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException(context.toString() + " must implement OnItemSelectedListener");
		}
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		Activity activity = getActivity();

		setHasOptionsMenu(true);

		updateListEmptyText();

		adapter = new SimpleCursorAdapter(activity,
			R.layout.article_item,
			null,
			FROM_COLUMNS,
			TO_IDS,
			0);
		adapter.setViewBinder(this);
		setListAdapter(adapter);
		setListShown(false);

		getAbsListView().setOnScrollListener(this);
		swipeRefreshLayout.setOnRefreshListener(this);
		syncButton.setOnClickListener(this);

		if (savedInstanceState != null)
		{
			mode = savedInstanceState.getInt(STATE_MODE);
			setNavigationMode(mode);
			filter = savedInstanceState.getParcelable(STATE_FILTER);
		}
		else
		{
			filter = new Filter();
		}
		getLoaderManager().initLoader(LIST_LOADER, null, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_MODE, mode);
		outState.putParcelable(STATE_FILTER, filter);
	}

	private void updateListEmptyText()
	{
		if (SyncUtils.isSyncedAutomatically() && SyncProgress.isSyncing(getContext()))
		{
			if (ConnectivityUtils.hasConnectivity(getContext()))
			{
				setEmptyText(getString(R.string.synchronization_progress));
			}
			else
			{
				setEmptyText(getString(R.string.no_connection));
			}
		}
		else
		{
			switch (mode)
			{
				case 0:
					setEmptyText(getString(R.string.no_articles));
					break;

				case 1:
					setEmptyText(getString(R.string.no_new_articles));
					break;

				case 2:
					setEmptyText(getString(R.string.no_starred_articles));
					break;
			}
		}
	}

	@Override
	public void setEmptyText(CharSequence text)
	{
		if (empty != null && emptyText != null)
		{
			emptyText.setText(text);
		}
	}

	@Override
	public void setListShown(boolean shown)
	{
		if (shown)
		{
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			progressContainer.setVisibility(View.VISIBLE);
			listContainer.setVisibility(View.GONE);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_articlelist, menu);

		MenuItem searchItem = menu.findItem(R.id.search);
		if (searchItem != null)
		{
			/* XXX: the searchview's content is not restored upon rotations.. */
			SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
			if (searchView != null) /* XXX: why is it null? */
			{
				searchView.setOnQueryTextListener(this);
			}
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args)
	{
		switch (id)
		{
			case LIST_LOADER:
				return new CursorLoader(
					getActivity(),
					DataProvider.CONTENT_URI,
					PROJECTION,
					filter.getSelection(),
					filter.getSelectionArguments(),
					PostColumns.CREATED + " DESC"
				);

		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor)
	{
		adapter.swapCursor(cursor);
		updateListEmptyText();
		setListShown(true);

		int articlesCount = cursor.getCount();
		count.setText(getResources().getQuantityString(R.plurals.status, articlesCount, articlesCount));

		/*
		 * We need that when rotating.
		 */
		if (getAbsListView().canScrollVertically(-1))
		{
			swipeRefreshLayout.setEnabled(false);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader)
	{
		adapter.swapCursor(null);
	}

	@Override
	public boolean onQueryTextSubmit(String s)
	{
		return true;
	}

	@Override
	public boolean onQueryTextChange(String newText)
	{
		if (TextUtils.isEmpty(newText))
		{
			filter.removeFilter(PostColumns.TITLE + " LIKE ?");
			getAbsListView().scrollTo(0, 0); /* scroll to the top if we remove the search filter so that the user notices */
		}
		else
		{
			filter.updateFilter(PostColumns.TITLE + " LIKE ?", "%" + newText.replaceAll("'", "''") + "%"); /* XXX: this fails when searching for stuff like [it's] */
		}
		applyFilter();
		return true;
	}

	public void clearFilters()
	{
		filter.clear();
		applyFilter();
	}

	private void applyFilter()
	{
		/*
		 * We check for changes to avoid flooding because some widgets can
		 * set filters in a pretty wild way.
		 */
		if (filter.hasChanged())
		{
			Log.d("filter selection: " + filter.getSelection() + ", arguments: " + Arrays.toString(filter.getSelectionArguments()));
			getLoaderManager().restartLoader(LIST_LOADER, null, this);
		}
	}

	public void setNavigationMode(int mode)
	{
		if (mode != this.mode)
		{
			switch (mode)
			{
				case 0: /* all */
					filter.removeFilter(PostColumns.CREATED + " > ?");
					filter.removeFilter(PostColumns.STARRED + " = ?");
					break;

				case 1: /* news */
					filter.updateFilter(PostColumns.CREATED + " > ?", Long.toString(System.currentTimeMillis() - (long) 1000 * 60 * 60 * 24 * 7));
					filter.removeFilter(PostColumns.STARRED + " = ?");
					break;

				case 2: /* favorites */
					filter.removeFilter(PostColumns.CREATED + " > ?");
					filter.updateFilter(PostColumns.STARRED + " = ?", "1");
					break;
			}
			updateListEmptyText();
			applyFilter(); /* XXX: double refresh? hm.. */
			this.mode = mode;
		}
	}

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex)
	{
		switch (view.getId())
		{
			case R.id.image:
				File file = new File(view.getContext().getFilesDir(), Constants.MEDIA_FILE_PREFIX + cursor.getLong(columnIndex));
				Picasso picasso = Picasso.get();
				RequestCreator request = file.exists() ? picasso.load(file) : picasso.load(R.mipmap.ic_launcher);
				request.fit()
					.centerInside();
					request.into((ImageView) view);
				return true;

			case R.id.date:
				((TextView)view).setText(DateUtils.getRelativeTimeSpanString(cursor.getLong(columnIndex)));
				return true;

			case R.id.excerpt:
				String text = cursor.getString(columnIndex);
				((TextView)view).setText(cursor.getString(columnIndex));
				if (TextUtils.isEmpty(text) && !getResources().getBoolean(R.bool.has_grid))
				{
					view.setVisibility(View.GONE);
				}
				else
				{
					view.setVisibility(View.VISIBLE);
				}
				return true;

			default:
				return false;
		}
	}

	@Override
	public void onListItemClick(AbsListView l, View v, int position, long id)
	{
		Uri uri = ContentUris.withAppendedId(DataProvider.CONTENT_URI, id);
		listener.onItemSelected(uri);
	}

	@Override
	public void onScrollStateChanged(AbsListView absListView, int scrollState)
	{
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
		{
			if (!getAbsListView().canScrollVertically(-1))
			{
				swipeRefreshLayout.setEnabled(true);
			}
		}
		else
		{
			swipeRefreshLayout.setEnabled(false);
		}
	}

	@Override
	public void onScroll(AbsListView absListView, int i, int i1, int i2)
	{

	}

	@Override
	public void onRefresh()
	{
		SyncUtils.manualSync();
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.sync:
				SyncUtils.manualSync(); /* XXX: there's no feedback in there, which sucks */
				break;
		}
	}

	public void setSyncStatus(boolean isSyncing)
	{
		updateListEmptyText();

		if (isSyncing)
		{
			progressBar.setIndeterminate(true);
			progressBar.setVisibility(View.VISIBLE);
		}
		else
		{
			progressBar.setVisibility(View.INVISIBLE);
			swipeRefreshLayout.setRefreshing(false);
		}

		if (SyncUtils.isSyncedAutomatically())
		{
			syncButton.setVisibility(View.GONE);
		}
		else
		{
			syncButton.setVisibility(View.VISIBLE);
		}
	}

	public void setSyncProgress(int current, int total)
	{
		progressBar.setIndeterminate(false);
		progressBar.setMax(total);
		progressBar.setProgress(current);
	}
}
