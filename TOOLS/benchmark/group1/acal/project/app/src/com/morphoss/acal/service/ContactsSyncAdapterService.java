package com.morphoss.acal.service;

import java.util.ArrayList;
import java.util.HashMap;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.contacts.VCardContact;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRGetResourcesInCollection;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VComponentCreationException;

public class ContactsSyncAdapterService extends Service {
	private static final String		TAG					= "ContactsSyncAdapterService";
	private static SyncAdapterImpl	sSyncAdapter		= null;
	private static ContentResolver	mContentResolver	= null;

	public ContactsSyncAdapterService() {
		super();
	}

	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
		private Context	mContext;

		public SyncAdapterImpl(Context context) {
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
				SyncResult syncResult) {
			try {
				ContactsSyncAdapterService.performSync(mContext, account, extras, authority, provider, syncResult);
			}
			catch ( OperationCanceledException e ) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	private SyncAdapterImpl getSyncAdapter() {
		if ( sSyncAdapter == null ) sSyncAdapter = new SyncAdapterImpl(this);
		return sSyncAdapter;
	}

	private static void performSync(Context context, Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) throws OperationCanceledException {
		HashMap<String, Integer> androidContacts = new HashMap<String, Integer>();
		mContentResolver = context.getContentResolver();
		Log.i(TAG, "performSync: " + account.toString());
		
		 // Load the local contacts for this account
		Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
				.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
				.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build();
		Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID, RawContacts.SYNC1 }, null, null, null);
		try {
			while ( c1.moveToNext() ) {
				androidContacts.put(c1.getString(1), c1.getInt(0));
			}
		}
		catch (Exception e) {
			Log.e(TAG,"Exception fetching Android contacts.", e);
		}
		finally {
			if (c1 != null) c1.close();
		}
		

		long collectionId = Long.parseLong(AccountManager.get(context).getUserData(account, AcalAuthenticator.COLLECTION_ID));
		ArrayList<Resource> resources = ResourceManager.getInstance(context).sendBlockingRequest(
					new RRGetResourcesInCollection(collectionId)
				).result();
		
		for ( Resource resource : resources ) {
			VCardContact vc;
			try {
				vc = new VCardContact(resource);
			}
			catch ( VComponentCreationException e ) {
				Log.println(Constants.LOGD, TAG, "Could not make VCard from resource ID "+resource.getResourceId());
				Log.println(Constants.LOGV, TAG, resource.getBlob());
				continue;
			}
			Integer androidContactId = androidContacts.get(vc.getUid());
			if ( androidContactId == null ) {
				Log.println(Constants.LOGD, TAG, "Contact record from aCal not present: inserting Android data.");
				vc.writeToContact(context, account, -1);
			}
			else {
				Integer aCalSequence = vc.getSequence();
				ContentValues androidContact = VCardContact.getAndroidContact(context,androidContactId);
				int androidSequence = androidContact.getAsInteger(RawContacts.VERSION);
				if ( aCalSequence == null || aCalSequence < androidSequence ) {
					vc.writeToVCard(context, androidContact);
				}
				else if ( aCalSequence == androidSequence ) {
					
					Log.println(Constants.LOGD, TAG, "Contact record in sync for "+vc.getFullName()+" - "+aCalSequence+"="+androidSequence);
				}
				else {
					Log.println(Constants.LOGD, TAG, "Contact record from aCal is newer: updating Android data.");
					vc.writeToContact(context, account, androidContactId);
				}
				androidContacts.remove(vc.getUid());
			}
		}
		
		/**
		 * @todo: Here we should go through any remaining androidContacts and create the VCards
		 * for them.
		 */
	}
	
}
