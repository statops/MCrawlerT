package fr.openium.sga.broadcastObserver;

import android.content.Intent;
import android.net.Uri;

public class SgaIntentListner {
	private Uri mUri;
	private final Intent mIntent;
	private SgaBroadcastReceiver mReceiver;
	public SgaIntentListner(Intent registerReceiver,
			SgaBroadcastReceiver receiver) {
		mReceiver=receiver;
		mIntent=registerReceiver;
	}

	public Uri getBdUri() {
		return mUri;
	}

	public void updateBdUri(Uri mUri) {
		this.mUri = mUri;
	}

	public SgaBroadcastReceiver getReceiver() {
		return mReceiver;
	}

	public void setReceiver(SgaBroadcastReceiver mReceiver) {
		this.mReceiver = mReceiver;
	}

	public Intent getIntent() {
		return mIntent;
	}
	

}
