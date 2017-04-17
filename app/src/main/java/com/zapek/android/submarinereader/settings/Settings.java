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

package com.zapek.android.submarinereader.settings;

public class Settings
{
	public static final String SHOW_NETWORK_SETTINGS = "showNetworkSettings";

	public static final String DIRECT_NETWORK = "directNetwork";
	public static final boolean DIRECT_NETWORK_DEFAULT = true;

	public static final String ARTICLES_TO_SYNC = "articlesToSync";
	public static final int ARTICLES_TO_SYNC_DEFAULT = 100;

	public static final String SYNC_IMAGES = "syncImages";
	public static final boolean SYNC_IMAGES_DEFAULT = true;

	public static final String SYNC_FORCE_UPDATE = "syncForceUpdate";
	public static final boolean SYNC_FORCE_UPDATE_DEFAULT = false;

	public static final String DONATION_SKU = "donationSku";
}
