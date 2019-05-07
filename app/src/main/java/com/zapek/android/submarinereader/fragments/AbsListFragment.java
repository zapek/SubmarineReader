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

import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.zapek.android.submarinereader.R;

public class AbsListFragment extends Fragment
{
	final private Handler handler = new Handler();

	final private Runnable requestFocus = new Runnable()
	{
		public void run()
		{
			absList.focusableViewAvailable(absList);
		}
	};

	final private AdapterView.OnItemClickListener onClickListener = new AdapterView.OnItemClickListener()
	{
		public void onItemClick(AdapterView<?> parent, View v, int position, long id)
		{
			onListItemClick((AbsListView) parent, v, position, id);
		}
	};

	private ListAdapter adapter;
	private AbsListView absList;
	private View standardEmptyView;
	private View progressContainer;
	private View listContainer;
	private CharSequence emptyText;
	private boolean listShown;

	public AbsListFragment()
	{
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.abslist_content, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		ensureList();
	}

	@Override
	public void onDestroyView()
	{
		handler.removeCallbacks(requestFocus);
		absList = null;
		listShown = false;
		progressContainer = null;
		listContainer = null;
		standardEmptyView = null;
		super.onDestroyView();
	}

	public void onListItemClick(AbsListView l, View v, int position, long id)
	{
	}

	public void setListAdapter(ListAdapter adapter)
	{
		boolean hadAdapter = this.adapter != null;
		this.adapter = adapter;
		if (absList != null)
		{
			absList.setAdapter(adapter);
			if (!listShown && !hadAdapter)
			{
				// The list was hidden, and previously didn't have an
				// adapter.  It is now time to show it.
				setListShown(true, getView().getWindowToken() != null);
			}
		}
	}

	public void setSelection(int position)
	{
		ensureList();
		absList.setSelection(position);
	}

	public int getSelectedItemPosition()
	{
		ensureList();
		return absList.getSelectedItemPosition();
	}

	public long getSelectedItemId()
	{
		ensureList();
		return absList.getSelectedItemId();
	}

	public AbsListView getAbsListView()
	{
		ensureList();
		return absList;
	}

	public void setEmptyText(CharSequence text)
	{
		ensureList();
		if (standardEmptyView == null || !(standardEmptyView instanceof TextView))
		{
			throw new IllegalStateException("Can't be used with a custom content view");
		}
		((TextView)standardEmptyView).setText(text);
		if (emptyText == null)
		{
			absList.setEmptyView(standardEmptyView);
		}
		emptyText = text;
	}

	public void setListShown(boolean shown)
	{
		setListShown(shown, true);
	}

	public void setListShownNoAnimation(boolean shown)
	{
		setListShown(shown, false);
	}

	private void setListShown(boolean shown, boolean animate)
	{
		ensureList();
		if (progressContainer == null)
		{
			throw new IllegalStateException("Can't be used with a custom content view");
		}
		if (listShown == shown)
		{
			return;
		}
		listShown = shown;
		if (shown)
		{
			if (animate)
			{
				progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
				listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
			}
			else
			{
				progressContainer.clearAnimation();
				listContainer.clearAnimation();
			}
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			if (animate)
			{
				progressContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
				listContainer.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
			}
			else
			{
				progressContainer.clearAnimation();
				listContainer.clearAnimation();
			}
			progressContainer.setVisibility(View.VISIBLE);
			listContainer.setVisibility(View.GONE);
		}
	}

	public ListAdapter getListAdapter()
	{
		return adapter;
	}

	private void ensureList()
	{
		if (absList != null)
		{
			return;
		}
		View root = getView();
		if (root == null)
		{
			throw new IllegalStateException("Content view not yet created");
		}
		if (root instanceof ListView || root instanceof GridView)
		{
			absList = (AbsListView) root;
		}
		else
		{
			standardEmptyView = root.findViewById(android.R.id.empty);
			if (standardEmptyView == null)
			{
				throw new RuntimeException("Missing empty view");
			}
			else
			{
				standardEmptyView.setVisibility(View.GONE);
			}
			progressContainer = root.findViewById(R.id.progressContainer);
			listContainer = root.findViewById(R.id.listContainer);
			View rawListView = root.findViewById(android.R.id.list);
			if (rawListView == null)
			{
				throw new RuntimeException("Your content must have a ListView or GridView whose id attribute is 'R.id.list'");
			}
			if (!(rawListView instanceof ListView) && !(rawListView instanceof GridView))
			{
				throw new RuntimeException("Content has view with id attribute 'R.id.list' that is not a ListView or GridView class");
			}
			absList = (AbsListView) rawListView;

			if (emptyText != null)
			{
				((TextView)standardEmptyView).setText(emptyText);
			}
			absList.setEmptyView(standardEmptyView);
		}
		listShown = true;
		absList.setOnItemClickListener(onClickListener);
		if (adapter != null)
		{
			ListAdapter adapter = this.adapter;
			this.adapter = null;
			setListAdapter(adapter);
		}
		else
		{
			// We are starting without an adapter, so assume we won't
			// have our data right away and start with the progress indicator.
			if (progressContainer != null)
			{
				setListShown(false, false);
			}
		}
		handler.post(requestFocus);
	}
}
