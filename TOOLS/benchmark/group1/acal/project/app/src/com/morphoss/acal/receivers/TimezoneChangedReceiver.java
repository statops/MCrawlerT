package com.morphoss.acal.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.morphoss.acal.database.alarmmanager.AlarmQueueManager;
import com.morphoss.acal.database.alarmmanager.requests.ARRebuildRequest;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.requests.CRClearCacheRequest;

public class TimezoneChangedReceiver extends BroadcastReceiver {

	private static final String TAG = "aCal TimezoneChangedReceiver";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG,"Oh look! Someone changed the timezone!");
		CacheManager cm = CacheManager.getInstance(context);
		if ( cm != null ) cm.sendRequest(new CRClearCacheRequest());
		
		AlarmQueueManager aqm = AlarmQueueManager.getInstance(context);
		if ( aqm != null ) aqm.sendRequest(new ARRebuildRequest());
	}

}
