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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.NavigationUtils;
import com.zapek.android.submarinereader.util.RawResourcesUtils;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_about);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

		ImageView badgeImage = (ImageView) findViewById(R.id.badge_image);
		TextView badgeText = (TextView) findViewById(R.id.badge_text);

		TextView version = (TextView) findViewById(R.id.version);
		if (BuildConfig.BUILD_TYPE.equals("release"))
		{
			version.setText(BuildConfig.VERSION_NAME);
		}
		else
		{
			version.setText(BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")");
		}

		TextView webLink = (TextView) findViewById(R.id.web);
		setAsUrl(webLink, getString(R.string.web_url));
		webLink.setOnClickListener(this);

		TextView supportLink = (TextView) findViewById(R.id.support);
		setAsUrl(supportLink, getString(R.string.support_email));
		supportLink.setOnClickListener(this);

		if (!TextUtils.isEmpty(getString(R.string.development_url)))
		{
			TextView developmentTitle = (TextView) findViewById(R.id.development_title);
			TextView developmentLink = (TextView) findViewById(R.id.development);
			setAsUrl(developmentLink, getString(R.string.development_url));
			developmentLink.setOnClickListener(this);
			developmentTitle.setVisibility(View.VISIBLE);
			developmentLink.setVisibility(View.VISIBLE);
		}

		TextView licenseText = (TextView) findViewById(R.id.license);
		licenseText.setText(Html.fromHtml(RawResourcesUtils.getRawResourceAsString(this, R.raw.license)));
		licenseText.setMovementMethod(LinkMovementMethod.getInstance());

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
				badgeImage.setImageResource(resId);
				badgeImage.setVisibility(View.VISIBLE);
				badgeText.setVisibility(View.VISIBLE);
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
	}
}
