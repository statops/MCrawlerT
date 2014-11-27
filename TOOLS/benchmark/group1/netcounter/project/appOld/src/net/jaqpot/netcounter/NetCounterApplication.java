/*
 * Copyright (C) 2009 Cyril Jaquier
 *
 * This file is part of NetCounter.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.jaqpot.netcounter;

import net.jaqpot.netcounter.activity.NetCounterActivity;
import net.jaqpot.netcounter.model.NetCounterModel;
import net.jaqpot.netcounter.service.NetCounterService;
import net.jaqpot.netcounter.service.WakefulService;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class NetCounterApplication extends Application {

	public static final boolean LOG_ENABLED = false;

	public static final int NOTIFICATION_DEBUG = -1234;

	public static CharSequence[] BYTE_UNITS;

	public static int[] BYTE_VALUES;

	public static CharSequence[] COUNTER_TYPES;

	public static int[] COUNTER_TYPES_POS;

	public static CharSequence[] COUNTER_SINGLE_DAY;

	public static CharSequence[] COUNTER_LAST_MONTH;

	public static CharSequence[] COUNTER_LAST_DAYS;

	public static CharSequence[] COUNTER_MONTHLY;

	public static CharSequence[] COUNTER_WEEKLY;

	public static final String SERVICE_POLLING = "polling";

	public static final int SERVICE_LOW = 0;

	public static final int SERVICE_HIGH = 1;

	public static final String INTENT_EXTRA_INTERFACE = "interface";

	private static Resources RESOURCES;

	private NotificationManager mNotification;

	private HandlerContainer mHandlerContainer;

	private NetCounterModel mModel;

	private SharedPreferences mPreferences;

	private static int sUpdatePolicy = SERVICE_LOW;

	public static Resources resources() {
		return RESOURCES;
	}

	public synchronized <T> T getAdapter(Class<T> clazz) {
		if (NetCounterModel.class == clazz) {
			if (mModel == null) {
				mModel = new NetCounterModel(this);
				Handler handler = getAdapter(HandlerContainer.class).getSlowHandler();
				handler.post(new Runnable() {
					public void run() {
						// Loads the model.
						mModel.load();
					}
				});
			}
			return clazz.cast(mModel);
		} else if (HandlerContainer.class == clazz) {
			if (mHandlerContainer == null) {
				HandlerThread looper = new HandlerThread("NetCounter Handler");
				looper.start();
				Handler handler = new Handler(looper.getLooper());
				mHandlerContainer = new HandlerContainer(new Handler(), handler);
			}
			return clazz.cast(mHandlerContainer);
		} else if (SharedPreferences.class == clazz) {
			if (mPreferences == null) {
				mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			}
			return clazz.cast(mPreferences);
		} else if (NotificationManager.class == clazz) {
			if (mNotification == null) {
				mNotification = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			}
			return clazz.cast(mNotification);
		}
		return null;
	}

	@Override
	public void onCreate() {
		BYTE_UNITS = getResources().getTextArray(R.array.byteUnits);
		BYTE_VALUES = getResources().getIntArray(R.array.byteValues);

		// Reorder the counter types.
		CharSequence[] temp = getResources().getTextArray(R.array.counterTypes);
		COUNTER_TYPES = new CharSequence[temp.length];
		COUNTER_TYPES_POS = getResources().getIntArray(R.array.counterTypesPos);
		for (int i = 0; i < temp.length; i++) {
			COUNTER_TYPES[COUNTER_TYPES_POS[i]] = temp[i];
		}

		COUNTER_SINGLE_DAY = getResources().getTextArray(R.array.counterSingleDay);
		COUNTER_LAST_MONTH = getResources().getTextArray(R.array.counterLastMonth);
		COUNTER_LAST_DAYS = getResources().getTextArray(R.array.counterLastDays);
		COUNTER_MONTHLY = getResources().getTextArray(R.array.counterMonthly);
		COUNTER_WEEKLY = getResources().getTextArray(R.array.counterWeekly);

		RESOURCES = getResources();

		super.onCreate();

		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "Application created");
		}
	}

	@Override
	public void onTerminate() {
		synchronized (this) {
			if (mHandlerContainer != null) {
				mHandlerContainer.getSlowHandler().getLooper().quit();
			}
		}

		BYTE_UNITS = null;
		BYTE_VALUES = null;
		COUNTER_TYPES = null;
		COUNTER_TYPES_POS = null;
		COUNTER_SINGLE_DAY = null;
		COUNTER_LAST_MONTH = null;
		COUNTER_LAST_DAYS = null;
		COUNTER_MONTHLY = null;
		COUNTER_WEEKLY = null;
		RESOURCES = null;

		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "Application terminated");
		}
	}

	public void startService() {
		WakefulService.acquireStaticLock(this);
		Intent intent = new Intent(this, NetCounterService.class);
		startService(intent);
	}

	public static synchronized void setUpdatePolicy(int updatePolicy) {
		sUpdatePolicy = updatePolicy;
	}

	public static synchronized int getUpdatePolicy() {
		return sUpdatePolicy;
	}

	public void toast(int message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	public void toast(CharSequence message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	public void notifyForDebug(CharSequence message) {
		SharedPreferences preferences = getAdapter(SharedPreferences.class);
		if (preferences.getBoolean("debug", false)) {
			Notification n = new Notification();
			n.when = System.currentTimeMillis();
			n.icon = R.drawable.icon;
			n.tickerText = message;
			Intent i = new Intent(this, NetCounterActivity.class);
			PendingIntent p = PendingIntent.getActivity(this, 0, i, 0);
			n.setLatestEventInfo(this, "NetCounter debug", message, p);
			getAdapter(NotificationManager.class).notify(NOTIFICATION_DEBUG, n);
		}
	}

}
