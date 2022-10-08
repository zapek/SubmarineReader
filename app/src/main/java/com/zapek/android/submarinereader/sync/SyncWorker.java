package com.zapek.android.submarinereader.sync;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.zapek.android.submarinereader.BuildConfig;
import com.zapek.android.submarinereader.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker
{
	public static final String SYNC_TAG = "sync";

	private static final Object syncWorkerLock = new Object();

	public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams)
	{
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork()
	{
		synchronized(syncWorkerLock) /* just to make sure */
		{
			Log.d("performing sync...");

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

			SyncStats syncStats = new SyncStats();

			ContentProviderClient contentProviderClient = getApplicationContext().getContentResolver().acquireContentProviderClient(BuildConfig.providerAuthority);
			if (contentProviderClient != null)
			{
				WordpressClient wordpressClient = new WordpressClient(getApplicationContext(), contentProviderClient, syncStats, prefs);
				syncStats.start();
				wordpressClient.syncPosts();
				syncStats.stop();

				contentProviderClient.release();

				Log.d("syncing finished. " + syncStats);

				return Result.success(syncStats.getData());
			}
			else
			{
				Log.d("syncing failed, content provider: " + BuildConfig.providerAuthority + " not found");
				syncStats.setError("Content Provider not found");
				return Result.failure(syncStats.getData());
			}
		}
	}

	static public void setAutoSync(boolean enabled)
	{
		if (enabled)
		{
			var builder = new Constraints.Builder()
				.setRequiresCharging(true)
				.setRequiredNetworkType(NetworkType.CONNECTED)
				.setRequiredNetworkType(NetworkType.UNMETERED)
				.setRequiresStorageNotLow(true);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			{
				builder.setRequiresDeviceIdle(true);
			}

			var constraints = builder
				.build();

			var periodicSyncRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 1, TimeUnit.DAYS, 1, TimeUnit.DAYS)
				.addTag(SyncWorker.SYNC_TAG)
				.setConstraints(constraints)
				.build();

			WorkManager.getInstance().enqueueUniquePeriodicWork("ScheduledSync", ExistingPeriodicWorkPolicy.KEEP, periodicSyncRequest);
		}
		else
		{
			WorkManager.getInstance().cancelUniqueWork("ScheduledSync");
		}
	}
}
