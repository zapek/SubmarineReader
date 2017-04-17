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

package com.zapek.android.submarinereader.rest;

import com.zapek.android.submarinereader.json.Media;
import com.zapek.android.submarinereader.json.Post;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WordpressApi
{
	/**
	 * Fetch several posts.
	 *
	 * @param offset the offset of the first post, starts at 0
	 * @param perPage the maximum number of posts in the result. Cannot be higher than 100 (wordpress' hardcoded limit)
	 * @return a list of <code>Post</code>
	 */
	@GET("posts")
	Call<List<Post>> listPosts(@Query("offset") int offset, @Query("per_page") int perPage);

	/**
	 * Fetch several posts past a certain date.
	 *
	 * @param offset the offset of the first post, starts at 0
	 * @param perPage the maximum number of posts in the result. Cannot be higher than 100 (wordpress' hardcoded limit)
	 * @param after only posts published after that date (ISO8601) will be returned
	 * @return a list of <code>Post</code>
	 */
	@GET("posts")
	Call<List<Post>> listPosts(@Query("offset") int offset, @Query("per_page") int perPage, @Query("after") String after);

	/**
	 * Fetch a post.
	 *
	 * @param id id of the post
	 * @return the <code>Post</code>
	 */
	@GET("posts/{id}")
	Call<Post> getPost(@Path("id") int id);

	/**
	 * Fetch a media.
	 * @param id id of the media
	 * @return the <code>Media</code>
	 */
	@GET("media/{id}")
	Call<Media> getMedia(@Path("id") int id);
}
