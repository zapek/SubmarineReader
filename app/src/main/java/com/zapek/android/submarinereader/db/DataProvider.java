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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.db.tables.PostColumns;

public class DataProvider extends ContentProvider implements BaseColumns
{

	public static final String PATH = "posts";
	public static final Uri CONTENT_URI = Uri.withAppendedPath(Uri.parse("content://" + BuildConfig.providerAuthority), PATH);

	private static final String MIMETYPE_ROWS = "vnd.android.cursor.dir/vnd.";
	private static final String MIMETYPE_ITEM = "vnd.android.cursor.item/vnd.";

	private static final int POST_URL_QUERY = 1;
	private static final int POST_URL_ID_QUERY = 2;

	private SQLiteOpenHelper sqLiteOpenHelper;

	private static final UriMatcher uriMatcher;
	private static final SparseArray<String> mimeTypes;

	static
	{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mimeTypes = new SparseArray<>();

		uriMatcher.addURI(
			BuildConfig.providerAuthority,
			PATH,
			POST_URL_QUERY);

		uriMatcher.addURI(
			BuildConfig.providerAuthority,
			PATH + "/#",
			POST_URL_ID_QUERY);

		mimeTypes.put(
			POST_URL_QUERY,
			MIMETYPE_ROWS + BuildConfig.providerAuthority + "." + PATH);

		mimeTypes.put(
			POST_URL_ID_QUERY,
			MIMETYPE_ITEM + BuildConfig.providerAuthority + "." + PATH);
	}

	@Override
	public boolean onCreate()
	{
		sqLiteOpenHelper = new DataProviderHelper(getContext());
		return true;
	}

	static private String getTableName(int query)
	{
		String tableName;

		switch (query)
		{
			case POST_URL_QUERY:
			case POST_URL_ID_QUERY:
				tableName = PostColumns.TABLENAME;
				break;

			default:
				tableName = null;
		}
		return tableName;
	}

	static private String getUpdatedSelection(String selection, int uriCode, Uri uri)
	{
		if (uriCode == POST_URL_ID_QUERY)
		{
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection))
			{
				selection = _ID + " = " + id;
			}
			else
			{
				selection += " and " + _ID + " = " + id;
			}
		}
		return selection;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();

		int uriCode = uriMatcher.match(uri);
		String tableName = getTableName(uriCode);

		if (tableName == null)
		{
			throw new IllegalArgumentException("query -- invalid URI: " + uri);
		}

		selection = getUpdatedSelection(selection, uriCode, uri);

		Cursor returnCursor;

		returnCursor = db.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
		returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
		return returnCursor;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri)
	{
		return mimeTypes.get(uriMatcher.match(uri));
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues contentValues)
	{
		int uriCode = uriMatcher.match(uri);

		String tableName = getTableName(uriCode);

		if (tableName == null)
		{
			throw new IllegalArgumentException("insert -- invalid URI: " + uri);
		}
		SQLiteDatabase localSQLiteDatabase = sqLiteOpenHelper.getWritableDatabase();

		long id = localSQLiteDatabase.insert(tableName, null, contentValues);
		if (id != -1)
		{
			getContext().getContentResolver().notifyChange(uri, null, false);
			return Uri.withAppendedPath(uri, Long.toString(id));
		}
		else
		{
			throw new SQLiteException("insert error: " + uri);
		}
	}

	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] contentValues)
	{
		int uriCode = uriMatcher.match(uri);

		String tableName = getTableName(uriCode);

		if (tableName == null)
		{
			throw new IllegalArgumentException("bulk insert -- invalid URI: " + uri);
		}

		SQLiteDatabase localSQLiteDatabase = sqLiteOpenHelper.getWritableDatabase();

		localSQLiteDatabase.beginTransaction();
		localSQLiteDatabase.delete(tableName, null, null);

		for (ContentValues value : contentValues)
		{
			localSQLiteDatabase.insert(tableName, null, value);
		}
		localSQLiteDatabase.setTransactionSuccessful();
		localSQLiteDatabase.endTransaction();

		getContext().getContentResolver().notifyChange(uri, null, false);
		return contentValues.length;
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs)
	{
		int uriCode = uriMatcher.match(uri);
		String tableName = getTableName(uriCode);

		if (tableName == null)
		{
			throw new IllegalArgumentException("delete -- invalid URI: " + uri);
		}

		selection = getUpdatedSelection(selection, uriCode, uri);

		SQLiteDatabase localSQLiteDatabase = sqLiteOpenHelper.getWritableDatabase();

		if (selection == null)
		{
			selection = "-1";
		}
		int rows = localSQLiteDatabase.delete(tableName, selection, selectionArgs);
		if (rows != -1)
		{
			getContext().getContentResolver().notifyChange(uri, null, false);
			return rows;
		}
		else
		{
			throw new SQLiteException("delete error: " + uri);
		}
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs)
	{
		int uriCode = uriMatcher.match(uri);
		String tableName = getTableName(uriCode);

		if (tableName == null)
		{
			throw new IllegalArgumentException("update -- invalid URI: " + uri);
		}

		selection = getUpdatedSelection(selection, uriCode, uri);

		SQLiteDatabase localSQLiteDatabase = sqLiteOpenHelper.getWritableDatabase();

		int rows = localSQLiteDatabase.update(tableName, contentValues, selection, selectionArgs);

		if (rows != 0)
		{
			getContext().getContentResolver().notifyChange(uri, null, false);
			return rows;
		}
		else
		{
			throw new SQLiteException("update error: " + uri);
		}
	}
}
