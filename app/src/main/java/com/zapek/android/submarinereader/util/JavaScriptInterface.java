package com.zapek.android.submarinereader.util;

import android.content.Context;
import android.content.res.Configuration;
import android.webkit.JavascriptInterface;

/**
 * Created by zapek on 2017-05-26.
 */

public class JavaScriptInterface
{
	private Context context;

	public JavaScriptInterface(Context context)
	{
		this.context = context;
	}

	@JavascriptInterface
	public boolean isNightModeEnabled()
	{
		int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
	}
}
