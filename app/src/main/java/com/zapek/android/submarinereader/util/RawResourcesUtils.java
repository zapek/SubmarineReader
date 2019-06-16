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

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RawResourcesUtils
{
	public static String getRawResourceAsString(Context context, int resId)
	{
		InputStream in = context.getResources().openRawResource(resId);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		byte[] buf = new byte[1024];
		int len;
		try
		{
			while ((len = in.read(buf)) != -1)
			{
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		}
		catch (IOException e)
		{
			Log.d("error while reading license: " + e);
		}
		return out.toString();
	}

	public static int getRawResourceId(Context context, String name)
	{
		return context.getResources().getIdentifier(name, "raw", context.getPackageName());
	}
}
