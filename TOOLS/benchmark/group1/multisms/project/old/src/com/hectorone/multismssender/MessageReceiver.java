package com.hectorone.multismssender;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class MessageReceiver extends BroadcastReceiver{
	public static final String MESSAGE_RECEIVED = "com.hectorone.multismssender.SMS_RECEIVED";
	public static final String ANDROID_MESSAGE_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public static final String DEBUG_TAG="-------MessageReceiver--------"; 

	@Override
	public void onReceive(Context context, Intent intent) {


		if (MESSAGE_RECEIVED.equals(intent.getAction())) {
			Log.d(DEBUG_TAG, "SMS_RECEIVED");
			Long entryId;
			//Bundle extras = intent.getExtras();
			//entryId = extras != null ? extras.getLong(MultiSmsSender.PARAM_ENTRY_ID): null;
			
			Uri entryURI = intent.getData();
			String entryStr = entryURI.getLastPathSegment();
			entryId = Long.parseLong(entryStr);
			
			//byte[] pdu = (byte[]) intent.getByteArrayExtra("pdu");
			//SmsMessage message = SmsMessage.createFromPdu(pdu);
			//int status = message.getStatus();
			if(entryId != null) {
				DeliveryDbAdapter mDbHelper = new DeliveryDbAdapter(context);
				mDbHelper.open();
				mDbHelper.setEntryDelivered(entryId);
				mDbHelper.close();
			}
		}
	}



}
