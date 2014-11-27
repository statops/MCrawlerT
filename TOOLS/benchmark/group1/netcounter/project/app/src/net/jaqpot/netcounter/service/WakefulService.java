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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Modified version of the WakefulIntentService class from The Busy Coder's
 * Guide to Advanced Android Development by Mark Murphy. This one extends
 * Service instead of IntentService as we will manage our own thread.
 */
public abstract class WakefulService extends Service {

	public static final String LOCK_NAME_STATIC = "WakeLock.Static";
	public static final String LOCK_NAME_LOCAL = "WakeLock.Local";

	private static PowerManager.WakeLock mLockStatic = null;
	private PowerManager.WakeLock mLockLocal = null;

	public static void acquireStaticLock(Context context) {
		getLock(context).acquire();
	}

	private static synchronized PowerManager.WakeLock getLock(Context context) {
		if (mLockStatic == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			mLockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);
			mLockStatic.setReferenceCounted(true);
		}
		return mLockStatic;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mLockLocal = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_LOCAL);
		mLockLocal.setReferenceCounted(true);
	}

	@Override
	public void onStart(Intent intent, final int startId) {
		mLockLocal.acquire();
		super.onStart(intent, startId);
		getLock(this).release();
	}

	/**
	 * Releases the local lock. Must be called when the work is done.
	 */
	protected void releaseLocalLock() {
		mLockLocal.release();
	}
}
