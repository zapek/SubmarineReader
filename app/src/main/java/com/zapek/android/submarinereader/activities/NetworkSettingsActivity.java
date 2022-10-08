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

package com.zapek.android.submarinereader.activities;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;

import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.databinding.ActivityNetworkSettingsBinding;
import com.zapek.android.submarinereader.util.AlertRequester;
import com.zapek.android.submarinereader.util.NavigationUtils;
import com.zapek.android.submarinereader.util.RawResourcesUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

public class NetworkSettingsActivity extends AppCompatActivity implements AlertRequester.AlertDialogListener
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ActivityNetworkSettingsBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_network_settings);

		setSupportActionBar(binding.toolbar.toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.activity_networksettings, menu);

		return true;
	}

	@SuppressLint("NonConstantResourceId")
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				NavigationUtils.navigateUp(this);
				return true;

			case R.id.help:
				AlertRequester.show(this, Html.fromHtml(RawResourcesUtils.getRawResourceAsString(this, R.raw.help_network)), getString(R.string.ok), 0, 0);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog, int returnCode, long userData)
	{

	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog, int returnCode, long userData)
	{

	}
}
