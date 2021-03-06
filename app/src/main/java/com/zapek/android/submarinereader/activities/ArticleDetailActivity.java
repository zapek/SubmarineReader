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

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.databinding.ActivityArticledetailBinding;
import com.zapek.android.submarinereader.fragments.ArticleDetailFragment;
import com.zapek.android.submarinereader.util.NavigationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

public class ArticleDetailActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ActivityArticledetailBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_articledetail);

		setSupportActionBar(binding.toolbar.toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		Uri articleUri = getIntent().getData();
		ArticleDetailFragment articleDetailFragment = (ArticleDetailFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_articledetail);
		articleDetailFragment.setArticleUri(articleUri);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				NavigationUtils.navigateUp(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
