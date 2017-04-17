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

package com.zapek.android.submarinereader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.zapek.android.submarinereader.db.tables.PostColumns;

public class DataProviderHelper extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "SR";
	private static final int DATABASE_VERSION = 1;

	private static final String TYPE_TEXT = "TEXT";
	private static final String TYPE_PRIMARY_KEY = "INTEGER PRIMARY KEY";
	private static final String TYPE_INTEGER = "INTEGER";

	DataProviderHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private void dropTables(SQLiteDatabase db)
	{
		db.execSQL("DROP TABLE IF EXISTS " + PostColumns.TABLENAME);
		db.execSQL("DROP INDEX IF EXISTS " + PostColumns.TABLENAME + "_" + PostColumns.CREATED + "_idx");
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + PostColumns.TABLENAME + " (" + PostColumns._ID + " " + TYPE_PRIMARY_KEY
			+ ", " + PostColumns.CREATED + " " + TYPE_INTEGER
			+ ", " + PostColumns.MODIFIED + " " + TYPE_INTEGER
			+ ", " + PostColumns.TITLE + " " + TYPE_TEXT
			+ ", " + PostColumns.EXCERPT + " " + TYPE_TEXT
			+ ", " + PostColumns.CONTENT + " " + TYPE_TEXT
			+ ", " + PostColumns.LINK + " " + TYPE_TEXT
			+ ", " + PostColumns.STARRED + " " + TYPE_INTEGER
			+ ");");

		db.execSQL("CREATE INDEX " + PostColumns.TABLENAME + "_" + PostColumns.CREATED + "_idx ON " + PostColumns.TABLENAME + "(" + PostColumns.CREATED + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
	{

	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		dropTables(db);
		onCreate(db);
	}
}
