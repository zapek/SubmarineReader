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

package com.zapek.android.submarinereader.application;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.R;
import com.zapek.android.submarinereader.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;

public class Init
{
	public static void initCrashReporter(Application app) {

		final ACRAConfiguration config;
		try
		{
			config = new ConfigurationBuilder(app)
				.setFormUri(BuildConfig.ACRAUri)
				.setReportingInteractionMode(ReportingInteractionMode.DIALOG)
				.setResDialogTitle(R.string.app_name)
				.setResDialogIcon(android.R.drawable.ic_dialog_info)
				.setResDialogText(R.string.crash_dialog_text)
				.setResDialogCommentPrompt(R.string.crash_dialog_comment)
				.setResDialogEmailPrompt(R.string.crash_dialog_email)
				.setResDialogOkToast(R.string.crash_dialog_toast)
				.setResDialogTheme(R.style.AppTheme_Dialog)
				.setBuildConfigClass(BuildConfig.class)
				.build();

			ACRA.init(app, config);
		}
		catch (ACRAConfigurationException e)
		{
			Log.d("can't initialize ACRA: " + e.getMessage());
		}
	}

	public static void sendCrashReport() {
		ACRA.getErrorReporter().handleException(null);
		/* XXX: no clue why this call crashes, it's supposed to be the right one */
		//ACRA.getErrorReporter().handleSilentException(null);
	}

	public static boolean isCrashReporterEnabled()
	{
		return true;
	}
}
