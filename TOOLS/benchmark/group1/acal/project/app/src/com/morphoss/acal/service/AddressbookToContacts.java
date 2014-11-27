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

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.contacts.VCardContact;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRGetResourcesInCollection;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VComponentCreationException;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.Servers;

public class AddressbookToContacts extends ServiceJob {

	private static final String TAG = "aCal AddressBookToContacts";
	private static final boolean DEBUG = true && Constants.DEBUG_MODE;
	private int collectionId;
	private aCalService context;
	private ContentResolver cr;
	private ContentValues collectionValues;
	private String acalAccountType;
	private Account	account;
	

	public AddressbookToContacts(int collectionId) {
		this.collectionId = collectionId;
	}

	
	@Override
	public void run(aCalService context) {
		this.context = context;
		this.cr = context.getContentResolver();
		this.acalAccountType = context.getString(R.string.AcalAccountType);
		this.collectionValues = DavCollections.getRow(collectionId, cr);
		
		this.account = getAndroidAccount();
		if ( account == null ) {
			Log.println(Constants.LOGD, TAG, "Addressbook "+collectionId+" '"+collectionValues.getAsString(DavCollections.DISPLAYNAME)+"' not marked for synchronisation to Contacts.");
			return;
		}

		Log.i(TAG,getDescription() + ": " + account.name);
		updateContactsFromAddressbook();
	}

	
	@Override
	public String getDescription() {
		return "Updating Android Contacts from Addressbook " + collectionId;
	}

	
	private Account getAndroidAccount() {
		AccountManager accountManager = AccountManager.get(context);
		int serverId = collectionValues.getAsInteger(DavCollections.SERVER_ID);
		ContentValues serverValues = Servers.getRow(serverId, cr);
		String collectionAccountName = serverValues.getAsString(Servers.FRIENDLY_NAME)
								+" - " + collectionValues.getAsString(DavCollections.DISPLAYNAME);

		Account[] accountList = accountManager.getAccountsByType(acalAccountType);

		int i=0;
		while ( i < accountList.length && !accountList[i].name.equals(collectionAccountName) ) {
			i++;
		}
		if ( i < accountList.length ) return accountList[i];
		return null;
	}

	
	private void updateContactsFromAddressbook() {
		long collectionId = Long.parseLong(AccountManager.get(context).getUserData(account, AcalAuthenticator.COLLECTION_ID));
		ArrayList<Resource> vCards = ResourceManager.getInstance(context).sendBlockingRequest(
					new RRGetResourcesInCollection(collectionId)
				).result();
		
		for( Resource vCardRow : vCards ) {
			try {
				VCardContact vCard = new VCardContact(vCardRow);

				Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
						.appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
						.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
						.build();
				Cursor cur = cr.query( rawContactUri, new String[] { BaseColumns._ID, Contacts.DISPLAY_NAME, RawContacts.VERSION, RawContacts.SYNC1 },
						RawContacts.SYNC1+"=?", new String[] { vCard.getUid() }, null);

				if ( cur != null && cur.getCount() > 1 ) {
					Log.println(Constants.LOGD, TAG, "Contact record from aCal is not present: inserting Android data.");
					for( cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext() ) {
						Log.w(TAG,String.format("UID:%s, _id:%d, name:%s, version:%s", vCard.getUid(), cur.getLong(0), cur.getString(1), cur.getInt(2)));
					}
					int count = cur.getCount();
					cur.close();
					Log.w(TAG,"Skipping contact with "+count+" RawContact rows for UID "+vCard.getUid()+". This Android Account should be removed and recreated.");
					return;
				}
				try {
					if ( cur.getCount() < 1 ) {
						Log.println(Constants.LOGD, TAG, "Contact record from aCal is not present: inserting Android data.");
						vCard.writeToContact(context, account, -1 );
					}
					else {
					    while (cur.moveToNext()) {
					        int id = cur.getInt( cur.getColumnIndex(Contacts._ID));
					        String name = cur.getString( cur.getColumnIndex(Contacts.DISPLAY_NAME));
					        int rawVersion = cur.getInt(cur.getColumnIndex(RawContacts.VERSION));
					        if ( rawVersion < vCard.getSequence() ) {
								Log.println(Constants.LOGD, TAG, "Contact record from aCal is newer: updating Android data for '"+name+"' ("+id+") "+vCard.getSequence()+">"+rawVersion);
					        	vCard.writeToContact(context, account, id);
					        }
					        else {
					        	if ( DEBUG ) Log.println(Constants.LOGV,TAG,
					        			"Existing Contact record up to date contact row for '"+name+"' ("+id+") "+vCard.getSequence()+"="+rawVersion);
					        }
				        }
				 	}
				}
				catch (Exception e) {
					Log.e(TAG,"Exception fetching Android contacts.", e);
				}
				finally {
					if (cur != null) cur.close();
				}

			}
			catch (VComponentCreationException e) {
				// TODO Auto-generated catch block
				Log.e(TAG,Log.getStackTraceString(e));
			}
		}
	}
	
}
