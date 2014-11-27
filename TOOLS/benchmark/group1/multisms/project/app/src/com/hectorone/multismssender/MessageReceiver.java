package com.hectorone.multismssender;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class MessageReceiver extends BroadcastReceiver{
	public static final String MESSAGE_RECEIVED         = "com.hectorone.multismssender.SMS_RECEIVED";
	public static final String ANDROID_MESSAGE_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public static final String DEBUG_TAG                = "-------MessageReceiver--------"; 

	@Override
	public void onReceive(Context context, Intent intent) {


		if (MESSAGE_RECEIVED.equals(intent.getAction())) {
			//Log.d(DEBUG_TAG, "SMS_RECEIVED");
			
			Uri entryURI = intent.getData();
			if (entryURI != null){
				ContentValues values = new ContentValues(1);
				values.put(DeliveryDbAdapter.KEY_DELIVERY_ENTRY_DELIVERED, 1);
				context.getContentResolver().update(entryURI, values, null, null);
				
			}
		}
	}



}
