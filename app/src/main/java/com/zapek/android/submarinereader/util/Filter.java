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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;

public class Filter implements Parcelable
{

	private Bundle bundle = new Bundle();
	private boolean hasChanged;
	private String selection;
	private String[] arguments;

	public Filter()
	{
	}

	private Filter(Parcel in)
	{
		readFromParcel(in);
	}

	/**
	 * Updates a filter
	 * @param condition filter condition
	 * @param parameter filter parameter, if null or "", then the filter condition is removed
	 */
	public void updateFilter(String condition, String parameter)
	{
		if (condition == null)
		{
			throw new IllegalArgumentException("empty condition");
		}

		if (TextUtils.isEmpty(parameter))
		{
			bundle.remove(condition);
			setHasChanged();
		}
		else
		{
			if (bundle.containsKey(condition))
			{
				if (bundle.getString(condition).equals(parameter))
				{
					return;
				}
			}
			bundle.putString(condition, parameter);
			setHasChanged();
		}
	}

	/**
	 * Removes a filter
	 * @param condition filter condition
	 */
	public void removeFilter(String condition)
	{
		if (condition == null)
		{
			throw new IllegalArgumentException("empty condition");
		}

		bundle.remove(condition);
		setHasChanged();
	}

	public void clear()
	{
		bundle.clear();
		setHasChanged();
	}

	/**
	 * @return true if the filter changed between the previous call
	 */
	public boolean hasChanged()
	{
		boolean returnValue = hasChanged;
		hasChanged = false;
		return returnValue;
	}

	private void setHasChanged()
	{
		hasChanged = true;
		selection = null;
		arguments = null;
	}

	public String getSelection()
	{
		generateSelectionAndArgumentsIfNeeded();
		return selection;
	}

	public String[] getSelectionArguments()
	{
		generateSelectionAndArgumentsIfNeeded();
		return arguments;
	}

	private void appendToSelection(String add)
	{
		if (TextUtils.isEmpty(selection))
		{
			selection += add;
		}
		else
		{
			selection += " AND " + add;
		}
	}

	private void generateSelectionAndArgumentsIfNeeded()
	{
		if (selection == null || arguments == null)
		{
			selection = "";
			ArrayList<String> args = new ArrayList<>();

			for (String key : bundle.keySet())
			{
				appendToSelection(key);
				args.add(bundle.getString(key));
			}
			arguments = args.size() > 0 ? args.toArray(new String[args.size()]) : null;
		}
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeInt(hasChanged ? 1 : 0);
		dest.writeBundle(bundle);
	}

	void readFromParcel(Parcel in)
	{
		hasChanged = in.readInt() == 1;
		bundle = in.readBundle();
	}

	public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>()
	{

		@Override
		public Filter createFromParcel(Parcel source)
		{
			return new Filter(source);
		}

		@Override
		public Filter[] newArray(int size)
		{
			return new Filter[size];
		}
	};
}
