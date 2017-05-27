package com.zapek.android.submarinereader.util;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Created by zapek on 2017-05-27.
 */

public class NightModeUtils
{
	public static boolean isInNightMode(Context context)
	{
		int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
	}
}
