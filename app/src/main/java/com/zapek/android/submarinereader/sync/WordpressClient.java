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
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

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
	private final SyncStats syncStats;
	private final SharedPreferences sharedPreferences;
	private final WordpressApi wordpressApi;
	private final boolean forceRefresh;
	private final boolean syncImages;

	public WordpressClient(Context context, ContentProviderClient contentProviderClient, SyncStats syncStats, SharedPreferences sharedPreferences)
	{
		this.context = context;
		this.contentProviderClient = contentProviderClient;
		this.syncStats = syncStats;
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
					HashMap<Long, ContentValues> localValues = null;
					String[] PROJECTION = {
						PostColumns._ID,
						PostColumns.MODIFIED,
						PostColumns.STARRED
					};
					try (Cursor cursor = contentProviderClient.query(DataProvider.CONTENT_URI, PROJECTION, null, null, null))
					{
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
							Integer starred = localValue.getAsInteger(PostColumns.STARRED);
							if (starred == null || starred != 1)
							{
								deletePost(localValue.getAsLong(PostColumns._ID));
							}
						}
					}
				}
			}
			else
			{
				Log.d("HTTP error: " + response.code() + ", message: " + response.message());
				syncStats.addIoError();
			}
		}
		catch (IOException | RemoteException e)
		{
			Log.d("I/O error: " + e.getMessage());
			syncStats.addIoError(e.getMessage());
		}
		catch (RuntimeException e)
		{
			Log.d("Runtime exception: " + e.getMessage(), e);
			syncStats.addParseError(e.getMessage());
		}

		/*
		 * If forceRefresh was used to transition the settings from
		 * no images to load all images and there was no error,
		 * we are done and don't need a full refresh for the next
		 * sync.
		 */
		if (forceRefresh && !syncStats.hasErrors())
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
		syncStats.addInsert();
	}

	private void updatePost(Post post) throws RemoteException
	{
		Log.d("updating entry " + post.getId());
		RemoveAttachedMedia(post.getId());
		AddAttachedMedia(post);
		contentProviderClient.update(ContentUris.withAppendedId(DataProvider.CONTENT_URI, post.getId()), post.getAsContentValues(), null, null);
		syncStats.addUpdates();
	}

	private void deletePost(long id) throws RemoteException
	{
		Log.d("removing entry " + id);
		RemoveAttachedMedia(id);
		contentProviderClient.delete(ContentUris.withAppendedId(DataProvider.CONTENT_URI, id), null, null);
		syncStats.addDeletes();
	}

	private boolean AddAttachedMedia(Post post)
	{
		boolean success = true;
		boolean hasFeaturedMedia = false;

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

					hasFeaturedMedia = writeImageMedia(media, post.getId());
				}
			}
			catch (IOException e)
			{
				Log.d("failed to fetch media: " + e.getMessage());
				success = false;
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
					if (src.startsWith("data:image"))
					{
						Log.d("built-in image");
						success = true;
					}
					else
					{
						/*
						 * Android 9 doesn't accept HTTP anymore by default so
						 * we force everything to HTTPS. Hopefully external
						 * sites all implement it.
						 */
						if (src.startsWith("http://"))
						{
							src = "https://" + src.substring("http://".length());
						}

						Log.d("downloading image from " + src);

						/*
						 * Note that if there's no featured media, we'll use the first image
						 * as one.
						 */
						if (writeImageMedia(src, post.getId(), i + (hasFeaturedMedia ? 1 : 0)))
						{
							img.attr("src", getMediaFileUrl(post.getId(), i + (hasFeaturedMedia ? 1 : 0)));
						}
						else
						{
							success = false;
						}
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

		return success;
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
							result = writeImageMedia(mediaUrl, id, 0);
						}
					}
				}
			}
		}
		return result;
	}

	private boolean writeImageMedia(String url, long id, int index)
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
					BufferedSink sink = Okio.buffer(Okio.sink(new File(context.getFilesDir(), getMediaFilename(id, index))));
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

	private String getMediaFilename(long id, int index)
	{
		if (index == 0)
		{
			return Constants.MEDIA_FILE_PREFIX + id;
		}
		else
		{
			return Constants.MEDIA_FILE_PREFIX + id + "_" + index;
		}
	}

	private String getMediaFileUrl(long id, int index)
	{
		return "file:///" + context.getFilesDir() + "/" + getMediaFilename(id, index);
	}

	private void RemoveAttachedMedia(long id)
	{

		context.deleteFile(Constants.MEDIA_FILE_PREFIX + id);

		for (int i = 1; i < 100; i++) /* Yes this is silly. There should be a way to retry posts with failed images */
		{
			context.deleteFile(Constants.MEDIA_FILE_PREFIX + id + "_" + i);
		}
	}

	static private String validateContentType(String contentType)
	{
		if (contentType == null)
		{
			Log.d("no content type");
			return null;
		}

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
