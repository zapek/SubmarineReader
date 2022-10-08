package com.zapek.android.submarinereader.sync;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;

public class SyncStats
{
	private final Data.Builder builder;
	private int ioErrorsCount;
	private String ioErrorReason;
	private int parseErrorsCount;
	private String parseErrorReason;
	private int insertsCount;
	private int updatesCount;
	private int deletesCount;
	private long elapsedTime;
	private String error;

	public static final String KEY_IO_ERRORS = "ioErrors";
	public static final String KEY_IO_ERROR_REASON = "ioErrorReason";
	public static final String KEY_PARSE_ERRORS = "parseErrors";
	public static final String KEY_PARSE_ERROR_REASON = "parseErrorReason";
	public static final String KEY_INSERTS = "inserts";
	public static final String KEY_UPDATES = "updates";
	public static final String KEY_DELETES = "deletes";
	public static final String KEY_ELAPSED_TIME = "elapsedTime";
	public static final String KEY_ERROR = "error";

	public SyncStats()
	{
		builder = new Data.Builder();
	}

	public void addIoError(String reason)
	{
		addIoError();
		if (TextUtils.isEmpty(ioErrorReason) && !TextUtils.isEmpty(reason))
		{
			ioErrorReason = reason;
		}
	}

	public void addIoError()
	{
		ioErrorsCount++;
	}

	public void addParseError(String reason)
	{
		addParseError();
		if (TextUtils.isEmpty(parseErrorReason) && !TextUtils.isEmpty(reason))
		{
			parseErrorReason = reason;
		}
	}

	public void addParseError()
	{
		parseErrorsCount++;
	}

	public void addInsert()
	{
		insertsCount++;
	}

	public void addUpdates()
	{
		updatesCount++;
	}

	public void addDeletes()
	{
		deletesCount++;
	}

	public boolean hasErrors()
	{
		return ioErrorsCount != 0 || parseErrorsCount != 0;
	}

	public void start()
	{
		elapsedTime = SystemClock.elapsedRealtime();
	}

	public void stop()
	{
		elapsedTime -= SystemClock.elapsedRealtime();
	}

	public void setError(String reason)
	{
		error = reason;
	}

	public String getError()
	{
		return error;
	}

	public long getElapsedTime()
	{
		return elapsedTime;
	}

	public Data getData()
	{
		builder.putInt(KEY_IO_ERRORS, ioErrorsCount);
		builder.putString(KEY_IO_ERROR_REASON, ioErrorReason);
		builder.putInt(KEY_PARSE_ERRORS, parseErrorsCount);
		builder.putString(KEY_PARSE_ERROR_REASON, parseErrorReason);
		builder.putInt(KEY_INSERTS, insertsCount);
		builder.putInt(KEY_UPDATES, updatesCount);
		builder.putInt(KEY_DELETES, deletesCount);
		builder.putLong(KEY_ELAPSED_TIME, elapsedTime);
		builder.putString(KEY_ERROR, error);

		return builder.build();
	}

	@NonNull
	@Override
	public String toString()
	{
		String result = "";
		if (ioErrorsCount > 0)
		{
			result += "I/O errors: " + ioErrorsCount;
			if (ioErrorReason != null)
			{
				result += " (" + ioErrorReason + ")";
			}
			result += "; ";
		}

		if (parseErrorsCount > 0)
		{
			result += "Parse errors: " + parseErrorsCount;
			if (parseErrorReason != null)
			{
				result += " (" + parseErrorReason + ")";
			}
			result += "; ";
		}

		if (insertsCount > 0)
		{
			result += "Inserts: " + insertsCount + "; ";
		}

		if (updatesCount > 0)
		{
			result += "Updates: " + updatesCount + "; ";
		}

		if (deletesCount > 0)
		{
			result += "Deletes: " + deletesCount + "; ";
		}

		if (elapsedTime > 0)
		{
			result += "Elapsed time: " + elapsedTime + " ms; ";
		}

		if (!TextUtils.isEmpty(error))
		{
			result += "External error: " + error + "; ";
		}

		if (TextUtils.isEmpty(result))
		{
			result = "Nothing done";
		}
		return result;
	}
}
