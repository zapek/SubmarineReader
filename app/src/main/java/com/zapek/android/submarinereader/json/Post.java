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

import android.content.ContentValues;
import android.text.Html;

import com.google.gson.annotations.SerializedName;
import com.zapek.android.submarinereader.db.tables.PostColumns;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Post
{
	private long id;

	@SerializedName("date_gmt")
	private Date created;

	@SerializedName("modified_gmt")
	private Date modified;

	private RenderedString title;

	private RenderedString content;

	private int author;

	private RenderedString excerpt;

	private String link;

	@SerializedName("featured_media")
	private int media;

	public long getId()
	{
		return id;
	}

	public long getCreated()
	{
		return created.getTime();
	}

	public long getModified()
	{
		return modified.getTime();
	}

	public String getTitle()
	{
		if (title != null)
		{
			return Html.fromHtml(title.rendered).toString();
		}
		return "";
	}

	public String getContent()
	{
		if (content != null)
		{
			return content.rendered;
		}
		return "";
	}

	public void setContent(String content)
	{
		this.content = new RenderedString();
		this.content.rendered = content;
	}

	public String getExcerpt()
	{
		if (excerpt != null)
		{
			return Html.fromHtml(excerpt.rendered).toString();
		}
		return "";
	}

	public String getLink()
	{
		return link;
	}

	public int getMediaId()
	{
		return media;
	}


	public static ContentValues[] toContentValuesArray(List<Post> posts)
	{
		ArrayList<ContentValues> contentValues = new ArrayList<>(posts.size());

		for (Post post : posts)
		{
			contentValues.add(post.getAsContentValues());
		}
		return contentValues.toArray(new ContentValues[0]);
	}

	public ContentValues getAsContentValues()
	{
		ContentValues contentValues = new ContentValues();
		contentValues.put(PostColumns._ID, getId());
		contentValues.put(PostColumns.CREATED, getCreated());
		contentValues.put(PostColumns.MODIFIED, getModified());
		contentValues.put(PostColumns.TITLE, getTitle());
		contentValues.put(PostColumns.EXCERPT, getExcerpt());
		contentValues.put(PostColumns.CONTENT, getContent());
		contentValues.put(PostColumns.LINK, getLink());
		return contentValues;
	}
}
