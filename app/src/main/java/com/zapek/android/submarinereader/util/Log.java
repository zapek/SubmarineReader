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

import com.zapek.android.submarinereader.BuildConfig;

public class Log
{
	private static final String TAG = "SR";

	public static void d(String s)
	{
		if (BuildConfig.logging)
		{
			StackTraceElement ste = new Throwable().getStackTrace()[1];
			String className = ste.getClassName();
			int lineNumber = ste.getLineNumber();

			android.util.Log.d(TAG, className.substring(className.lastIndexOf(".") + 1) + "/" + ste.getMethodName() + "()[" + lineNumber + "]: " + s);
		}
	}

	public static void d(String s, Throwable th)
	{
		if (BuildConfig.logging)
		{
			android.util.Log.d(TAG, s, th);
		}
	}

	public static void i(String s)
	{
		android.util.Log.i(TAG, s);
	}

	public static void e(String s)
	{
		android.util.Log.e(TAG, s);
	}
}
