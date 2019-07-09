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

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.databinding.ActivityAboutBinding;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.NavigationUtils;
import com.zapek.android.submarinereader.util.NightModeUtils;
import com.zapek.android.submarinereader.util.RawResourcesUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ActivityAboutBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_about);

		setSupportActionBar(binding.toolbar.toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		if (BuildConfig.BUILD_TYPE.equals("release"))
		{
			binding.version.setText(BuildConfig.VERSION_NAME);
		}
		else
		{
			binding.version.setText(BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")");
		}

		setAsUrl(binding.web, getString(R.string.web_url));
		binding.web.setOnClickListener(this);

		setAsUrl(binding.support, getString(R.string.support_email));
		binding.support.setOnClickListener(this);

		if (!TextUtils.isEmpty(getString(R.string.development_url)))
		{
			setAsUrl(binding.development, getString(R.string.development_url));
			binding.development.setOnClickListener(this);
			binding.developmentTitle.setVisibility(View.VISIBLE);
			binding.development.setVisibility(View.VISIBLE);
		}

		String license = "";

		int copyrightId = RawResourcesUtils.getRawResourceId(this, "copyright");
		if (copyrightId != 0)
		{
			license += RawResourcesUtils.getRawResourceAsString(this, copyrightId);
		}

		license += RawResourcesUtils.getRawResourceAsString(this, R.raw.license);

		binding.license.setText(Html.fromHtml(license));
		binding.license.setMovementMethod(LinkMovementMethod.getInstance());
		setLinkColor(binding.license);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String sku = sharedPreferences.getString(Settings.DONATION_SKU, "");
		if (!TextUtils.isEmpty(sku))
		{
			int resId;

			switch (sku)
			{
				case Constants.SKU_COFFEE:
					resId = R.drawable.badge_bronze;
					break;

				case Constants.SKU_DINNER:
					resId = R.drawable.badge_silver;
					break;

				case Constants.SKU_RENT:
					resId = R.drawable.badge_gold;
					break;

				default:
					resId = 0;
					break;
			}

			if (resId != 0)
			{
				binding.badgeImage.setImageResource(resId);
				binding.badgeImage.setVisibility(View.VISIBLE);
				binding.badgeText.setVisibility(View.VISIBLE);
			}
		}
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

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.web:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.web_url))));
				break;

			case R.id.support:
				startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.support_email) + "?subject=" + Uri.encode(getString(R.string.email_support_subject, getString(R.string.app_name))))));
				break;

			case R.id.development:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.development_url))));
				break;
		}
	}

	static private void setAsUrl(TextView textView, String s)
	{
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		ssb.append(s);
		ssb.setSpan(new URLSpan("#"), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		textView.setText(ssb, TextView.BufferType.SPANNABLE);
		setLinkColor(textView);
	}

	static private void setLinkColor(TextView textView)
	{
		if (NightModeUtils.isInNightMode(textView.getContext()))
		{
			textView.setLinkTextColor(Color.parseColor("#00afff"));
		}
	}
}
