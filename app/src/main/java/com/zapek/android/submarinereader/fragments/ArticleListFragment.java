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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.databinding.FragmentArticlelistBinding;
import com.zapek.android.submarinereader.db.DataProvider;
import com.zapek.android.submarinereader.db.tables.PostColumns;
import com.zapek.android.submarinereader.sync.SyncWorker;
import com.zapek.android.submarinereader.util.ConnectivityUtils;
import com.zapek.android.submarinereader.util.Filter;
import com.zapek.android.submarinereader.util.Log;

import java.io.File;
import java.util.Arrays;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class ArticleListFragment extends AbsListFragment implements SearchView.OnQueryTextListener, SimpleCursorAdapter.ViewBinder, LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener, View.OnClickListener
{
	public interface OnItemSelectedListener
	{
		void onItemSelected(Uri entryUri);
	}

	private static final int LIST_LOADER = 1;

	private FragmentArticlelistBinding binding;
	private boolean syncStatus;

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
		binding = FragmentArticlelistBinding.inflate(inflater, container, false);
		return binding.getRoot();
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
		binding.swipeRefreshLayout.setOnRefreshListener(this);
		binding.syncButton.setOnClickListener(this);

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
		LoaderManager.getInstance(this).initLoader(LIST_LOADER, null, this);
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
		if (isSyncing())
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
					if (ConnectivityUtils.hasConnectivity(getContext()))
					{
						setEmptyText(getString(R.string.no_articles));
					}
					else
					{
						setEmptyText(getString(R.string.no_connection));
					}
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
		if (binding.empty != null && binding.emptyText != null)
		{
			binding.emptyText.setText(text);
		}
	}

	@Override
	public void setListShown(boolean shown)
	{
		if (shown)
		{
			binding.progressContainer.setVisibility(View.GONE);
			binding.listContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			binding.progressContainer.setVisibility(View.VISIBLE);
			binding.listContainer.setVisibility(View.GONE);
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
			SearchView searchView = (SearchView) searchItem.getActionView();
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
		binding.count.setText(getResources().getQuantityString(R.plurals.status, articlesCount, articlesCount));

		/*
		 * We need that when rotating.
		 */
		getView().post(() ->
		{
			if (getAbsListView().canScrollVertically(-1))
			{
				binding.swipeRefreshLayout.setEnabled(false);
			}
		});
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
			LoaderManager.getInstance(this).restartLoader(LIST_LOADER, null, this);
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
				binding.swipeRefreshLayout.setEnabled(true);
			}
		}
		else
		{
			binding.swipeRefreshLayout.setEnabled(false);
		}
	}

	@Override
	public void onScroll(AbsListView absListView, int i, int i1, int i2)
	{

	}

	@Override
	public void onRefresh()
	{
		syncArticles();
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.syncButton:
				syncArticles();
				break;
		}
	}

	public void setSyncStatus(boolean isSyncing)
	{
		syncStatus = isSyncing;

		updateListEmptyText();

		if (isSyncing)
		{
			binding.progressBar.setIndeterminate(true);
			binding.progressBar.setVisibility(View.VISIBLE);
		}
		else
		{
			binding.progressBar.setVisibility(View.INVISIBLE);
			binding.swipeRefreshLayout.setRefreshing(false);
		}
	}

	private boolean isSyncing()
	{
		return syncStatus;
	}

	public void syncArticles()
	{
		if (!isSyncing())
		{
			if (ConnectivityUtils.hasConnectivity(getActivity()))
			{

				OneTimeWorkRequest immediateSyncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
					.addTag(SyncWorker.SYNC_TAG)
					.build();

				WorkManager.getInstance(getContext()).enqueueUniqueWork("ImmediateSync", ExistingWorkPolicy.KEEP, immediateSyncRequest);
			}
			else
			{
				Log.d("no network connectivity");
				Toast.makeText(getActivity(), "No network connectivity", Toast.LENGTH_SHORT).show();
			}
		}
		else
		{
			Log.d("already syncing, dropping request");
		}
	}

	public void setSyncProgress(int current, int total)
	{
		binding.progressBar.setIndeterminate(false);
		binding.progressBar.setMax(total);
		binding.progressBar.setProgress(current);
	}
}
