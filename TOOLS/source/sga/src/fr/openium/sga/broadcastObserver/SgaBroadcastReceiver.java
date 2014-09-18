package fr.openium.sga.broadcastObserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class SgaBroadcastReceiver extends BroadcastReceiver {

	private final IServiceProviderUpdater mContext;

	public SgaBroadcastReceiver(IServiceProviderUpdater ctx) {
		mContext = ctx;
	}

	private static final String TAG = SgaBroadcastReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("SgaBroadcastReceiver ",
				"SgaBroadcastReceiver: broadcast received");
		/**
		 * update content provider
		 */
		mContext.updateBroadCastProvder(intent.getAction());
	}

}
