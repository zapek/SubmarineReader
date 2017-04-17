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

package com.zapek.android.submarinereader.json;

public class MediaSizes
{
	private MediaSize thumbnail;
	private MediaSize medium;
	private MediaSize large;
	private MediaSize full;

	public MediaSize getThumbnail()
	{
		return thumbnail;
	}

	public MediaSize getMedium()
	{
		return medium;
	}

	public MediaSize getLarge()
	{
		return large;
	}

	public MediaSize getFull()
	{
		return full;
	}

	public MediaSize getBestMediaSize(int width)
	{
		if (checkMediaSize(full, width))
		{
			return full;
		}
		else if (checkMediaSize(large, width))
		{
			return large;
		}
		else if (checkMediaSize(medium, width))
		{
			return medium;
		}
		else
		{
			return thumbnail;
		}
	}

	private static boolean checkMediaSize(MediaSize mediaSize, int width)
	{
		return mediaSize != null && mediaSize.getWidth() > 0 && mediaSize.getWidth() < width;
	}
}
