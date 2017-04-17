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

package com.zapek.android.submarinereader;

import android.net.Uri;

public class Constants
{
	static
	{
		serverUri = Uri.parse(BuildConfig.serverUrl);
	}

	public static final Uri serverUri;

	public static final int HTTP_CONNECT_TIMEOUT = 20;
	public static final int HTTP_READ_TIMEOUT = 30;
	public static final int HTTP_WRITE_TIMEOUT = 20;

	public static final String MEDIA_FILE_PREFIX = "media_";

	public static final String SKU_COFFEE = "coffee";
	public static final String SKU_DINNER = "dinner";
	public static final String SKU_RENT = "rent";
}
