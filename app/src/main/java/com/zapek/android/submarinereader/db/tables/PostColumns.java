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

package com.zapek.android.submarinereader.db.tables;

import android.provider.BaseColumns;

public class PostColumns implements BaseColumns
{
	public static final String TABLENAME = "posts";

	public static final String CREATED = "created";
	public static final String MODIFIED = "modified";
	public static final String TITLE = "title";
	public static final String EXCERPT = "excerpt";
	public static final String CONTENT = "content";
	public static final String LINK = "link";
	public static final String STARRED = "starred";
}
