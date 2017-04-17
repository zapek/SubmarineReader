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

package com.zapek.android.submarinereader.sync;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.Constants;
import com.zapek.android.submarinereader.db.DataProvider;
import com.zapek.android.submarinereader.db.tables.PostColumns;
import com.zapek.android.submarinereader.json.Media;
import com.zapek.android.submarinereader.json.MediaDetails;
import com.zapek.android.submarinereader.json.MediaSize;
import com.zapek.android.submarinereader.json.MediaSizes;
import com.zapek.android.submarinereader.json.Post;
import com.zapek.android.submarinereader.rest.WordpressApi;
import com.zapek.android.submarinereader.settings.Settings;
import com.zapek.android.submarinereader.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WordpressClient
{
	private final Context context;
	private final OkHttpClient okHttpClient;
	private final ContentProviderClient contentProviderClient;
	private final SyncResult syncResult;
	private final SharedPreferences sharedPreferences;
	private final WordpressApi wordpressApi;
	private final boolean forceRefresh;
	private final boolean syncImages;

	public WordpressClient(Context context, ContentProviderClient contentProviderClient, SyncResult syncResult, SharedPreferences sharedPreferences)
	{
		this.context = context;
		this.contentProviderClient = contentProviderClient;
		this.syncResult = syncResult;
		this.sharedPreferences = sharedPreferences;
		forceRefresh = sharedPreferences.getBoolean(Settings.SYNC_FORCE_UPDATE, Settings.SYNC_FORCE_UPDATE_DEFAULT);
		syncImages = sharedPreferences.getBoolean(Settings.SYNC_IMAGES, Settings.SYNC_IMAGES_DEFAULT);

		okHttpClient = new OkHttpClient.Builder()
			.connectTimeout(Constants.HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
			.writeTimeout(Constants.HTTP_WRITE_TIMEOUT, TimeUnit.SECONDS)
			.readTimeout(Constants.HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
			.build();

		Gson gson = new GsonBuilder()
			.setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
			.create();

		Retrofit retrofit = new Retrofit.Builder()
			.baseUrl(BuildConfig.serverUrl)
			.addConverterFactory(GsonConverterFactory.create(gson))
			.client(okHttpClient)
			.build();

		wordpressApi = retrofit.create(WordpressApi.class);
	}

	@SuppressLint("UseSparseArrays")
	public void syncPosts()
	{
		Log.d("Connecting to service: " + BuildConfig.serverUrl);

		Call<List<Post>> call = wordpressApi.listPosts(0, Integer.parseInt(sharedPreferences.getString(Settings.ARTICLES_TO_SYNC, Integer.toString(Settings.ARTICLES_TO_SYNC_DEFAULT))));
		try
		{
			Response<List<Post>> response = call.execute();
			Log.d("response success: " + response.isSuccessful());
			Log.d("response code: " + response.code() + ", message: " + response.message());

			if (response.isSuccessful())
			{
				List<Post> posts = response.body();

				Log.d("fetched " + posts.size() + " posts from server (total on server: " + response.headers().get("X-WP-Total") + ")");

				if (posts.size() > 0)
				{
					/*
					 * Load the local data.
					 */
					Cursor cursor = null;
					HashMap<Long, ContentValues> localValues = null;

					String[] PROJECTION = {
						PostColumns._ID,
						PostColumns.MODIFIED
					};

					try
					{
						cursor = contentProviderClient.query(DataProvider.CONTENT_URI, PROJECTION, null, null, null);

						if (cursor != null && cursor.moveToFirst())
						{
							localValues = new HashMap<>(cursor.getCount());

							do
							{
								ContentValues localValue = new ContentValues();
								DatabaseUtils.cursorRowToContentValues(cursor, localValue);
								localValues.put(cursor.getLong(cursor.getColumnIndex(PostColumns._ID)), localValue);
							}
							while (cursor.moveToNext());
						}
					}
					finally
					{
						if (cursor != null)
						{
							cursor.close();
						}
					}

					boolean hasLocalData = localValues != null && localValues.size() > 0;

					/*
					 * There is old data. Update and add from server
					 */
					if (hasLocalData)
					{
						/*
						 * In reverse order so they appear chronologically
						 */
						Collections.reverse(posts);
					}

					for (int i = 0; i < posts.size(); i++)
					{
						Post post = posts.get(i);
						if (!hasLocalData)
						{
							SyncProgress.syncProgress(context, i, posts.size());
						}

						ContentValues localValue = null;
						if (hasLocalData)
						{
							localValue = localValues.get(post.getId());
						}
						if (localValue != null)
						{
							/*
							 * Local entry exists, check if it's been updated server side.
							 */
							if (forceRefresh || localValue.getAsLong(PostColumns.MODIFIED) < post.getModified())
							{
								updatePost(post);
							}
							localValues.remove(post.getId());
						}
						else
						{
							/*
							 * The server entry doesn't exist locally. Add it.
							 */
							addPost(post);
						}
					}

					/*
					 * Finally, iterate the old data and remove everything
					 * that is not on the server anymore.
					 */
					if (hasLocalData)
					{
						for (HashMap.Entry<Long, ContentValues> entry : localValues.entrySet())
						{
							ContentValues localValue = entry.getValue();
							deletePost(localValue.getAsLong(PostColumns._ID));
						}
					}
				}
			}
			else
			{
				Log.d("HTTP error: " + response.code() + ", message: " + response.message());
				syncResult.stats.numIoExceptions++;
			}
		}
		catch (IOException | RemoteException e)
		{
			Log.d("I/O error: " + e.getMessage());
			syncResult.stats.numIoExceptions++;
		}
		catch (RuntimeException e)
		{
			Log.d("Runtime exception: " + e.getMessage());
			syncResult.stats.numParseExceptions++;
		}

		if (forceRefresh && syncResult.stats.numIoExceptions == 0 && syncResult.stats.numParseExceptions == 0)
		{
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putBoolean(Settings.SYNC_FORCE_UPDATE, false);
			editor.apply();
		}
	}

	private void addPost(Post post) throws RemoteException
	{
		Log.d("adding new entry " + post.getId());
		AddAttachedMedia(post);
		contentProviderClient.insert(ContentUris.withAppendedId(DataProvider.CONTENT_URI, post.getId()), post.getAsContentValues());
		syncResult.stats.numInserts++;
	}

	private void addPosts(List<Post> posts) throws RemoteException
	{
		Log.d("adding entries as bulk");
		AddAttachedMedia(posts);
		syncResult.stats.numInserts += contentProviderClient.bulkInsert(DataProvider.CONTENT_URI, Post.toContentValuesArray(posts));
	}

	private void updatePost(Post post) throws RemoteException
	{
		Log.d("updating entry " + post.getId());
		RemoveAttachedMedia(post.getId());
		AddAttachedMedia(post);
		contentProviderClient.update(ContentUris.withAppendedId(DataProvider.CONTENT_URI, post.getId()), post.getAsContentValues(), null, null);
		syncResult.stats.numUpdates++;
	}

	private void deletePost(long id) throws RemoteException
	{
		Log.d("removing entry " + id);
		RemoveAttachedMedia(id);
		contentProviderClient.delete(ContentUris.withAppendedId(DataProvider.CONTENT_URI, id), null, null);
		syncResult.stats.numDeletes++;
	}

	private void AddAttachedMedia(List<Post> posts)
	{
		for (int i = 0; i < posts.size(); i++)
		{
			SyncProgress.syncProgress(context, i + 1, posts.size());
			AddAttachedMedia(posts.get(i));
		}
	}

	private void AddAttachedMedia(Post post)
	{
		boolean hasMedia = false;

		if (syncImages && post.getMediaId() > 0)
		{
			Log.d("fetching featured media, id: " + post.getMediaId());

			Call<Media> call = wordpressApi.getMedia(post.getMediaId());
			try
			{
				Response<Media> response = call.execute();
				Log.d("response success: " + response.isSuccessful());
				Log.d("response code: " + response.code() + ", message: " + response.message());

				if (response.isSuccessful())
				{
					Media media = response.body();

					hasMedia = writeImageMedia(media, post.getId());
				}
			}
			catch (IOException e)
			{
				Log.d("failed to fetch media: " + e.getMessage());
			}
		}

		Document document = Jsoup.parse(post.getContent());
		if (syncImages)
		{
			/*
			 * Remove elements styles when their width if set
			 * with pixel width.
			 */
			Elements styles = document.select("[style*=width]");
			for (Element style : styles)
			{
				String styleString = style.attr("style");
				if (styleString.contains("px"))
				{
					style.removeAttr("style");
				}
			}

			Elements imgs = document.select("img");
			for (int i = 0; i < imgs.size(); i++)
			{
				Element img = imgs.get(i);

				String src = img.attr("src");

				img.removeAttr("srcset");
				img.removeAttr("sizes");

				if (!TextUtils.isEmpty(src))
				{
					Log.d("downloading image from " + src);

					Request request = new Request.Builder()
						.url(src)
						.build();

					try
					{
						okhttp3.Response response = okHttpClient.newCall(request).execute();

						if (response.isSuccessful())
						{
							Log.d("content-type: " + response.header("Content-Type"));
							String contentType = validateContentType(response.header("Content-Type"));

							if (!TextUtils.isEmpty(contentType))
							{
								byte[] body = response.body().bytes();
								img.attr("src", getDataUri(contentType, body));

								if (i == 0 && !hasMedia)
								{
									FileOutputStream out = context.openFileOutput(Constants.MEDIA_FILE_PREFIX + post.getId(), Context.MODE_PRIVATE);
									out.write(body);
									out.close();
								}
							}
							/* XXX */
						}
					}
					catch (IOException | OutOfMemoryError e)
					{
						Log.d("failed to download image: " + e.getMessage());
					}
				}
			}
		}

		Elements links = document.select("[src]");
		for (Element link : links)
		{
			Uri srcUri = Uri.parse(link.attr("src"));
			if (srcUri.getScheme() == null)
			{
				Uri.Builder builder = srcUri.buildUpon();
				builder.scheme(Constants.serverUri.getScheme());
				link.attr("src", builder.build().toString());
			}
		}

		Element head = document.select("head").first();
		head.append("<meta name='viewport' content='width=device-width'>");
		head.append("<link rel='stylesheet' href='file:///android_asset/css/main.css' media='all' />");
		head.append("<script type='text/javascript' src='file:///android_asset/js/jquery-3.1.1.min.js'></script>");
		head.append("<script type='text/javascript' src='file:///android_asset/js/tweaks.js'></script>");
		post.setContent(document.toString());
	}

	private boolean writeImageMedia(Media media, long id)
	{
		boolean result = false;

		if (!TextUtils.isEmpty(validateContentType(media.getMimeType())))
		{
			MediaDetails mediaDetails = media.getMediaDetails();

			if (mediaDetails != null)
			{
				MediaSizes mediaSizes = mediaDetails.getMediaSizes();

				if (mediaSizes != null)
				{
					MediaSize mediaSize = mediaSizes.getBestMediaSize(Resources.getSystem().getDisplayMetrics().widthPixels);

					if (mediaSize != null)
					{
						String mediaUrl = mediaSize.getSourceUrl();

						if (!TextUtils.isEmpty(mediaUrl))
						{
							result = writeImageMedia(mediaUrl, (int) id);
						}
					}
				}
			}
		}
		return result;
	}

	private boolean writeImageMedia(String url, int id)
	{
		boolean result = false;

		Request request = new Request.Builder()
			.url(url)
			.build();

		try
		{
			okhttp3.Response response = okHttpClient.newCall(request).execute();

			if (response.isSuccessful())
			{
				if (!TextUtils.isEmpty(validateContentType(response.header("Content-Type"))))
				{
					BufferedSink sink = Okio.buffer(Okio.sink(new File(context.getFilesDir(), Constants.MEDIA_FILE_PREFIX + id)));
					sink.writeAll(response.body().source());
					sink.close();
					result = true;
				}
			}
		}
		catch (IOException e)
		{
			Log.d("failed to download image: " + e.getMessage());
		}
		return result;
	}

	private void RemoveAttachedMedia(long id)
	{
		context.deleteFile(Constants.MEDIA_FILE_PREFIX + id);
	}

	static private String getDataUri(String mimeType, byte[] input)
	{
		return "data:" + mimeType + ";base64," + Base64.encodeToString(input, Base64.DEFAULT);
	}

	static private String validateContentType(String contentType)
	{
		switch (contentType)
		{
			case "image/jpeg":
				return contentType;

			case "image/png":
				return contentType;

			case "image/gif":
				return contentType;

			default:
				Log.d("unknown image content type " + contentType);
				return null;
		}
	}
}
