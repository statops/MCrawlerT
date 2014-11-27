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

package net.jaqpot.netcounter.service;

import java.util.Calendar;

import net.jaqpot.netcounter.NetCounterApplication;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * {@link NetCounterAlarm} registers a broadcast receiver that will be triggered
 * by the alarm manager.
 */
public class NetCounterAlarm {

	/**
	 * State of the alarm.
	 */
	enum State {
		LOW, HIGH
	}

	private static final long INTER_ACTIVE = 30 * 1000;

	private static final long INTER_STANDBY = 60 * 60 * 1000;

	private State mCurrentState = null;

	private final NetCounterApplication mApp;

	private final AlarmManager mAm;

	private final PendingIntent mAs;

	/**
	 * The constructor.
	 * 
	 * @param srv
	 *            The {@link Service}.
	 * @param cls
	 *            The {@link BroadcastReceiver} triggered by the alarm.
	 */
	public NetCounterAlarm(Service srv, Class<? extends BroadcastReceiver> cls) {
		mAm = (AlarmManager) srv.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(srv, cls);
		mAs = PendingIntent.getBroadcast(srv, 0, i, 0);
		mApp = (NetCounterApplication) srv.getApplication();
	}

	/**
	 * Returns the current state of the alarm.
	 * 
	 * @return The current {@link State}.
	 */
	public State getCurrentState() {
		return mCurrentState;
	}

	/**
	 * Registers a new alarm if needed.
	 * 
	 * @param state
	 *            The {@link State}.
	 * @return <code>true</code> if a new alarm has been registered.
	 *         <code>false</code> otherwise.
	 */
	public boolean registerAlarm(State state) {
		if (mCurrentState != state) {
			switch (state) {
			case HIGH:
				registerActiveAlarm();
				break;
			case LOW:
				registerStandbyAlarm();
				break;
			}
			mCurrentState = state;
			return true;
		}
		return false;
	}

	/**
	 * Sets the alarm active.
	 */
	private void registerActiveAlarm() {
		mApp.notifyForDebug("Register ACTIVE alarm");
		long t = SystemClock.elapsedRealtime() + INTER_ACTIVE;
		// Sets the alarm.
		mAm.setRepeating(AlarmManager.ELAPSED_REALTIME, t, INTER_ACTIVE, mAs);
		// Logging.
		if (NetCounterApplication.LOG_ENABLED) {
			Log.i(getClass().getName(), "Set alarm to active mode.");
		}
	}

	/**
	 * Sets the alarm to standby. Every hour at :59.
	 */
	private void registerStandbyAlarm() {
		mApp.notifyForDebug("Register STANDBY alarm");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 0);
		long t = calendar.getTimeInMillis();
		// Sets the alarm.
		mAm.setRepeating(AlarmManager.RTC_WAKEUP, t, INTER_STANDBY, mAs);
		// Logging.
		if (NetCounterApplication.LOG_ENABLED) {
			Log.i(getClass().getName(), "Set alarm to standby mode.");
		}
	}

}
