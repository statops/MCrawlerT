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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Broadcast receiver that get launched after boot up. It delays the initial
 * callback to the service in order to speed up boot.
 */
public class OnBootReceiver extends BroadcastReceiver {

	private static final long DELAY = 2 * 60 * 1000;

	@Override
	public void onReceive(Context context, Intent intent) {
		AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent i = new Intent(context, OnAlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

		// Delays the initial start of the service as we don't need to start
		// right after boot.
		long t = SystemClock.elapsedRealtime() + DELAY;
		mgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, t, pi);
	}

}
