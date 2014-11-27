package com.morphoss.acal.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class iMIPReceiver extends BroadcastReceiver {
	private static final String TAG = "aCal iMIPReceiver";
	
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG,"Cool!  iMIP!");
		
		Bundle b = intent.getExtras();
		for( String k : b.keySet() ) {
			Log.i(TAG,"Found key of '"+k+"' with content of "+b.get(k).getClass().toString());
		}
	}

}
