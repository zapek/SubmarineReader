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

package com.zapek.android.submarinereader.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

public class ConnectivityUtils
{
	public static boolean hasGoodConnection(Context context)
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
		{
			if (connectivityManager.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED)
			{
				return false;
			}
		}

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected())
		{
			if (networkInfo.isRoaming())
			{
				return false;
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			{
				if (networkInfo.getDetailedState().equals(NetworkInfo.DetailedState.VERIFYING_POOR_LINK))
				{
					return false;
				}
			}

			switch (networkInfo.getType())
			{
				case ConnectivityManager.TYPE_ETHERNET:
				case ConnectivityManager.TYPE_WIFI:
				case ConnectivityManager.TYPE_WIMAX:
					return true;

				/* tethering and VPN is probably slow */
				case ConnectivityManager.TYPE_BLUETOOTH:
				case ConnectivityManager.TYPE_VPN:
					return false;

				case ConnectivityManager.TYPE_MOBILE:
					switch (networkInfo.getSubtype())
					{
						case TelephonyManager.NETWORK_TYPE_1xRTT:
						case TelephonyManager.NETWORK_TYPE_CDMA:
						case TelephonyManager.NETWORK_TYPE_EDGE:
						case TelephonyManager.NETWORK_TYPE_GPRS:
						case TelephonyManager.NETWORK_TYPE_IDEN:
							return false;

						/* let's assume future protocols will be fast */
						default:
							return true;
					}
			}
		}
		return false;
	}

	public static boolean hasConnectivity(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();

		if (ni != null)
		{
			if (ni.isConnected())
			{
				return true;
			}
		}
		return false;
	}
}
