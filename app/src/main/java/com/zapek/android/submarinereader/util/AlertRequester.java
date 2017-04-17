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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;

public class AlertRequester extends DialogFragment implements DialogInterface.OnClickListener
{

	public interface AlertDialogListener
	{
		void onDialogPositiveClick(DialogFragment dialog, int returnCode, long userData);

		void onDialogNegativeClick(DialogFragment dialog, int returnCode, long userData);
	}

	private static final String KEY_MESSAGE = "message";
	private static final String KEY_POSITIVE = "positive";
	private static final String KEY_NEGATIVE = "negative";
	private static final String KEY_RETURNCODE = "returnCode";
	private static final String KEY_USERDATA = "userData";

	private AlertDialogListener listener;
	private int returnCode;
	private long userData;

	public static void confirm(Activity activity, CharSequence message, CharSequence positive, CharSequence negative, int returnCode, long userData)
	{
		AlertRequester dialog = new AlertRequester();
		confirmInternal(dialog, activity, message, positive, negative, returnCode, userData);
	}

	public static void confirm(Fragment fragment, CharSequence message, CharSequence positive, CharSequence negative, int returnCode, long userData)
	{
		AlertRequester dialog = new AlertRequester();
		dialog.setTargetFragment(fragment, returnCode);
		confirmInternal(dialog, fragment.getActivity(), message, positive, negative, returnCode, userData);
	}

	public static void show(Activity activity, CharSequence message, CharSequence ok, int returnCode, long userData)
	{
		AlertRequester dialog = new AlertRequester();
		confirmInternal(dialog, activity, message, ok, null, returnCode, userData);
	}

	public static void show(Fragment fragment, CharSequence message, CharSequence ok, int returnCode, long userData)
	{
		AlertRequester dialog = new AlertRequester();
		dialog.setTargetFragment(fragment, returnCode);
		confirmInternal(dialog, fragment.getActivity(), message, ok, null, returnCode, userData);
	}

	private static void confirmInternal(AlertRequester dialog, Activity activity, CharSequence message, CharSequence positive, CharSequence negative, int returnCode, long userData)
	{
		Bundle args = new Bundle(5);
		args.putCharSequence(KEY_MESSAGE, message);
		args.putCharSequence(KEY_POSITIVE, positive);
		if (!TextUtils.isEmpty(negative))
		{
			args.putCharSequence(KEY_NEGATIVE, negative);
		}
		args.putInt(KEY_RETURNCODE, returnCode);
		args.putLong(KEY_USERDATA, userData);
		dialog.setArguments(args);
		dialog.show(activity.getFragmentManager(), "AlertRequesterFragment");
	}

	private void setFragmentListener(Fragment fragment)
	{
		if (listener == null)
		{
			try
			{
				listener = (AlertDialogListener) fragment;
			}
			catch (ClassCastException e)
			{
				throw new ClassCastException(fragment.toString() + " must implement AlertDialogListener");
			}
		}
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		Fragment fragment = getTargetFragment();
		if (fragment != null)
		{
			setFragmentListener(fragment);
		}
		else
		{
			try
			{
				listener = (AlertDialogListener) activity;
			}
			catch (ClassCastException e)
			{
				throw new ClassCastException(activity.toString() + " must implement AlertDialogListener");
			}
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Bundle args = getArguments();
		returnCode = args.getInt(KEY_RETURNCODE);
		userData = args.getLong(KEY_USERDATA);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(args.getCharSequence(KEY_MESSAGE));
		builder.setPositiveButton(args.getCharSequence(KEY_POSITIVE), this);
		CharSequence negative = args.getCharSequence(KEY_NEGATIVE);
		if (!TextUtils.isEmpty(negative))
		{
			builder.setNegativeButton(args.getCharSequence(KEY_NEGATIVE), this);
		}
		return builder.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
			case AlertDialog.BUTTON_POSITIVE:
				listener.onDialogPositiveClick(this, returnCode, userData);
				break;

			case AlertDialog.BUTTON_NEGATIVE:
				listener.onDialogNegativeClick(this, returnCode, userData);
				break;
		}
	}

	@Override
	public void onCancel(DialogInterface dialog)
	{
		listener.onDialogNegativeClick(this, returnCode, userData);
	}
}