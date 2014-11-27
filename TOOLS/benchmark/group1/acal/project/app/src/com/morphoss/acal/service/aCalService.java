/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.database.alarmmanager.AlarmQueueManager;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;

public class aCalService extends IntentService {


	private ServiceRequest.Stub serviceRequest = new ServiceRequestHandler();
	private WorkerClass worker;
	public static final String TAG = "aCalService";
	public static String aCalVersion = "aCal/1.0"; // Updated at start of program.
	//public static final DatabaseEventDispatcher databaseDispatcher = new DatabaseEventDispatcher();
	
	private final static long serviceStartedAt = System.currentTimeMillis();
	private ResourceManager rm;
	private CacheManager cm;
	private AlarmQueueManager am;
	
	private static SharedPreferences prefs = null;

	public aCalService() {
		super(TAG);
	}

	
	public void onCreate() {
		super.onCreate();

		aCalVersion = getString(R.string.appName) + "/";
		try {
			aCalVersion += getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e) {
			Log.e(TAG,"Can't find our good self in the PackageManager!");
			Log.e(TAG,Log.getStackTraceString(e));
		}

		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		startService();
	}

	
	private synchronized void startService() {

		rm = ResourceManager.getInstance(this);
		cm = CacheManager.getInstance(this);
		am = AlarmQueueManager.getInstance(this);
		
		worker = WorkerClass.getInstance(this);
				
		// Schedule immediate sync of any changes to the server
		worker.addJobAndWake(new SyncChangesToServer());

		// Start sync running for all active collections
		SynchronisationJobs.startCollectionSync(worker, this, 35000L);

		if ( ! Constants.DISABLE_FEATURE_TZSERVER_SUPPORT ) {
			// Start periodic syncing of timezone data
			worker.addJobAndWake(new UpdateTimezones(15000L));
		}
	}

	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return Service.START_STICKY;
	}

	// The actual start command, regardless of whether we're running under
	// 1.x or 2.x
	private void handleCommand( Intent inRequest ) {
		if ( inRequest == null ) return;
		if ( inRequest.hasExtra("UISTARTED") ) {
			// The UI is currently starting, so we might schedule some stuff
			// to happen soon.
			long uiStarted = inRequest.getLongExtra("UISTARTED", System.currentTimeMillis());
			if ( serviceStartedAt > uiStarted ) return; // Not if everything just started!

			// Tell the dataService to rebuild it's caches, just to be sure.
			if ( Constants.LOG_DEBUG )
				Log.i(TAG,"UI Started, requesting internal cache revalidation.");

			ServiceJob job = new SynchronisationJobs(SynchronisationJobs.CACHE_RESYNC);
			job.TIME_TO_EXECUTE = 5000L;
			worker.addJobAndWake(job);

			// Start sync running for all active collections
			SynchronisationJobs.startCollectionSync(worker, this, 25000L);
		}
	}

	

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "On destroy called. Killing worker thread.");
		//Ensure database is closed properly and worker is terminated.
		if ( worker != null ) worker.killWorker();
		worker = null;
		am.close();
		rm.close();
		cm.close();
		cm = null;
		rm = null;
		if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Worker killed.");
	}
	


	private synchronized void scheduleServiceRestart() {
		long restartTime = System.currentTimeMillis() + 60000;
		 
		Intent serviceIntent = new Intent(this, aCalService.class);
		serviceIntent.putExtra("RESTARTED", System.currentTimeMillis());

		PendingIntent ourFutureSelf = PendingIntent.getService(getApplicationContext(), 0, serviceIntent, 0);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, restartTime, ourFutureSelf);
		Log.e(TAG, "Scheduling aCalService restart in 60 seconds.");
		this.stopSelf();
	}

	
	//@Override
	public IBinder onBind(Intent arg0) {
		return serviceRequest;
	}
	
	public void addWorkerJob(ServiceJob s) {
		Runtime r = Runtime.getRuntime();
		if ( ((r.totalMemory() * 100) / r.maxMemory()) > 115 ) {
			scheduleServiceRestart();
		}
		else {
			if ( worker == null ) startService();
			this.worker.addJobAndWake(s);
		}
	}

	public String getPreferenceString(String key, String defValue) {
    	if ( prefs == null ) 
    		prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	return prefs.getString(key, defValue);
	}

	
	private class ServiceRequestHandler extends ServiceRequest.Stub {

		@Override
		public void discoverHomeSets() throws RemoteException {
			ServiceJob job = new SynchronisationJobs(SynchronisationJobs.HOME_SET_DISCOVERY);
			job.TIME_TO_EXECUTE = System.currentTimeMillis();
			worker.addJobAndWake(job);
		}

		@Override
		public void updateCollectionsFromHomeSets() throws RemoteException {
			ServiceJob job = new SynchronisationJobs(SynchronisationJobs.HOME_SETS_UPDATE);
			job.TIME_TO_EXECUTE = System.currentTimeMillis();
			worker.addJobAndWake(job);
		}

		@Override
		public void fullResync() throws RemoteException {
			ServiceJob[] jobs = new ServiceJob[2];
			jobs[0] = new SynchronisationJobs(SynchronisationJobs.HOME_SET_DISCOVERY);
			jobs[1] = new SynchronisationJobs(SynchronisationJobs.HOME_SETS_UPDATE);
			worker.addJobsAndWake(jobs);
			SynchronisationJobs.startCollectionSync(worker, aCalService.this, 15000L);
		}

		@Override
		public void revertDatabase() throws RemoteException {
			worker.addJobAndWake(new DebugDatabase(DebugDatabase.REVERT));
		}
		
		public void saveDatabase() throws RemoteException {
			worker.addJobAndWake(new DebugDatabase(DebugDatabase.SAVE));
		}

		@Override
		public void homeSetDiscovery(int server) throws RemoteException {
			HomeSetDiscovery job = new HomeSetDiscovery(server);
			worker.addJobAndWake(job);
		}

		@Override
		public void syncCollectionNow(long collectionId) throws RemoteException {
			SyncCollectionContents job = new SyncCollectionContents(collectionId, true);
			worker.addJobAndWake(job);
		}

		@Override
		public void fullCollectionResync(long collectionId) throws RemoteException {
			InitialCollectionSync job = new InitialCollectionSync(collectionId);
			worker.addJobAndWake(job);
		}
	}


	@Override
	protected void onHandleIntent(Intent intent) {
        if (Constants.LOG_DEBUG) Log.println(Constants.LOGD, TAG, "Service starting via onHandleIntent()");
        startService();
	}
	
	
}

