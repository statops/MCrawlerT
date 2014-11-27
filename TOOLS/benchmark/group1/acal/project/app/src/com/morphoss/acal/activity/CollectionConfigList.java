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

package com.morphoss.acal.activity;

import java.util.Map;
import java.util.TreeMap;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.ServiceManagerCallBack;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.AcalAuthenticator;
import com.morphoss.acal.service.ServiceRequest;

/**
 * <h3>Collection Configuration List - A list of collections that can be configured</h3>
 * 
 * <p>
 * This class generates and displays the list of collections available in the dav_collection table. Selecting
 * a collection will start the CollectionConfig activity.
 * </p>
 * 
 * @author Morphoss Ltd
 * 
 */
public class CollectionConfigList extends PreferenceActivity 
			implements OnPreferenceClickListener, OnCreateContextMenuListener {

	public static final String TAG = "aCal CollectionConfigList";
	
	
	// Data from the Collection Table
	int									collectionListCount	= 0;
	private int[]						collectionListIds;
	private Map<Integer, ContentValues>	collectionData;

	private Map<Integer, ContentValues>	serverData;

	// Context Menu Options
	public static final int				CONTEXT_SYNC_NOW	= 1;
	public static final int				CONTEXT_DISABLE		= 2;
	
	private static final int	CONTEXT_FORCE_FULL_RESYNC	= 3;
	
	
	public static final int UPDATE_COLLECTION_CONFIG = 0;
	private boolean updateRequested = false;
	private int updateId = -1;

	private Cursor						mCursor;

	private ServiceManager				serviceManager		= null;

	private PreferenceScreen			preferenceRoot;

	private int	serverListCount;

	private int[]	preferenceListIds;

	// Needed for AcalAuthenticator
//	public static final String ACTION_CHOOSE = "com.morphoss.acal.activity.CollectionConfigList.ACTION_CHOOSE";
	public static final String ACTION_CHOOSE_ADDRESSBOOK = "com.morphoss.acal.ACTION_CHOOSE_ADDRESSBOOK";
	
	/**
	 * Get the list of collections and create the list view.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * @author Morphoss Ltd
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.collections_list);

		// Get all of the collections from the database
		getCollectionListItems();
		
		// Create configuration screen
		createPreferenceHierarchy();

		registerForContextMenu(this.getListView());
	}


	/**
	 * <p>
	 * This method connects to the database and gets all collection information. It creates a new ListAdapter and
	 * applies it to the ListView. It also updates our Fields and causes this activity to redraw itself.
	 * Should be called whenever there has been a change to the collection table
	 * </p>
	 * 
	 * @author Morphoss Ltd
	 */
	private void getCollectionListItems() {

		// Get Servers Data
		ContentResolver cr = getContentResolver();
		mCursor = cr.query(Servers.CONTENT_URI, null, Servers.ACTIVE, null, Servers._ID );
		try {
			this.serverListCount = mCursor.getCount();
			
			this.serverData = new TreeMap<Integer,ContentValues>();
			mCursor.moveToFirst();
			while( ! mCursor.isAfterLast() ) {
				ContentValues cv = new ContentValues(); 
				DatabaseUtils.cursorRowToContentValues(mCursor, cv);
				int serverId = cv.getAsInteger(Servers._ID);
				this.serverData.put(serverId, cv);
				mCursor.moveToNext();
			}
		}
		catch( Exception e ) {
			Log.w(TAG, "Error getting server list",e);
		}
		finally {
			if ( mCursor != null ) mCursor.close();
		}

		// Get Collections Data
		mCursor = cr.query(DavCollections.CONTENT_URI, null, null, null, DavCollections.SERVER_ID+",lower("+DavCollections.DISPLAYNAME+")" );
		try {
			collectionListCount = mCursor.getCount();
	
			// Store data in useful structures
			this.collectionListIds = new int[collectionListCount];
			
			this.collectionData = new TreeMap<Integer,ContentValues>();
			mCursor.moveToFirst();
			int i = 0;
			while( ! mCursor.isAfterLast() ) {
				ContentValues cv = new ContentValues(); 
				DatabaseUtils.cursorRowToContentValues(mCursor, cv);
				int collectionId = cv.getAsInteger(DavCollections._ID);
				this.collectionListIds[i++] = collectionId; 
				this.collectionData.put(collectionId, cv);
				mCursor.moveToNext();
			}
		}
		catch( Exception e ) {
			Log.w(TAG, "Error getting collection list",e);
		}
		finally {
			if ( mCursor != null ) mCursor.close();
		}
	}

	/**
	 * <p>This method constructs all of the preference elements required</p>
	 * @return The preference screen that was created.
	 */
	private void createPreferenceHierarchy() {
		
		// Root
	    this.preferenceRoot = getPreferenceManager().createPreferenceScreen(this);
	    this.preferenceRoot.setPersistent(false);	//We are not using the SharedPrefs system, we will persist data manually.

		this.preferenceListIds = new int[collectionListCount+serverListCount];
	    PreferenceCategory currentCategory = null;
		int lastServerId = -1;
		int prefRowId = 0;
		for (int i = 0; i < this.collectionListCount; i++) {
			int collectionId = collectionListIds[i];
			ContentValues cv = collectionData.get(collectionId);
			int serverId = cv.getAsInteger(DavCollections.SERVER_ID);
			if (serverData.get(serverId) == null || 1 != serverData.get(serverId).getAsInteger(Servers.ACTIVE))
				continue;
			if (lastServerId != serverId) {
				currentCategory = new PreferenceCategory(this);
				currentCategory.setTitle(serverData.get(serverId).getAsString(Servers.FRIENDLY_NAME));
				currentCategory.setPersistent(false);
				preferenceRoot.addPreference(currentCategory);
				preferenceListIds[prefRowId++] = 0;
				lastServerId = serverId;
			}
			String collectionColour = cv.getAsString(DavCollections.COLOUR);
			CollectionConfigListItemPreference thisPreference = new CollectionConfigListItemPreference(this);
			thisPreference.setLayoutResource(R.layout.collections_list_item);
			currentCategory.addPreference(thisPreference);
			thisPreference.setTitle(cv.getAsString(DavCollections.DISPLAYNAME));
			thisPreference.setSummary(cv.getAsString(DavCollections.COLLECTION_PATH));
			thisPreference.setCollectionColour(collectionColour);
			thisPreference.setPersistent(false);
			thisPreference.setKey(Integer.toString(collectionId));
			thisPreference.setOnPreferenceClickListener(this);
			preferenceListIds[prefRowId++] = collectionId;
			thisPreference.setEnabled(true);
			Log.println(Constants.LOGD, TAG, "Created preference for "+thisPreference.getTitle());
	    }

		setPreferenceScreen(this.preferenceRoot);
		this.preferenceRoot.setOnPreferenceClickListener(this);
   	}


	/**
	 * <p>
	 * Called when a user selects 'Sync Now' from the context menu. Schedules an immediate sync
	 * for this collection.
	 * </p>
	 * 
	 * @param position
	 *            The position in CollectionNames of the name of the collection we are to synchronise
	 * @return true if operation was successful
	 * 
	 * @author Morphoss Ltd
	 */
	private boolean syncCollection( int collectionId, boolean fullCollectionResync ) {
		try {
			if ( serviceManager == null ) serviceManager = new ServiceManager(this);
			if ( fullCollectionResync ) {
				serviceManager.getServiceRequest().fullCollectionResync(collectionId);
			}
			else {
				serviceManager.getServiceRequest().syncCollectionNow(collectionId);
			}
			return true;
		}
		catch ( RemoteException re ) {
			Log.e(TAG, "Unable to send synchronisation request to service: "+re.getMessage());
			Toast.makeText(CollectionConfigList.this, "Request failed: "+re.getMessage(), Toast.LENGTH_SHORT).show();
		}
		return false;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == UPDATE_COLLECTION_CONFIG  && resultCode == RESULT_OK) {
			if (data.hasExtra("UpdateRequired")) {
				int cId = data.getIntExtra("UpdateRequired", -1);
				if (cId < 0) return;
				updateId = cId;
				updateRequested = true;
			}
		}
	}
	

	/**
	 * <p>
	 * Called when a user selects 'Disable Collection' from the context menu.
	 * </p>
	 * 
	 * @param position
	 *            The position in CollectionNames of the name of the collection we are to disable
	 * @return true if operation was successful
	 * 
	 * @author Morphoss Ltd
	 */
	private boolean disableCollection(int collectionId) {
		return DavCollections.collectionEnabled(false, collectionId, getContentResolver());
	}

	public void createAuthenticatedAccount(int collectionId) {
		ContentValues collectionValues = collectionData.get(collectionId);
		int serverId = collectionValues.getAsInteger(DavCollections.SERVER_ID);
		ContentValues serverValues = Servers.getRow(serverId, getContentResolver());
		String collectionName = collectionValues.getAsString(DavCollections.DISPLAYNAME);
		String serverName = serverValues.getAsString(Servers.FRIENDLY_NAME);
		Account account = new Account(serverName + " - " + collectionName, getString(R.string.AcalAccountType));
		Bundle userData = new Bundle();
		userData.putString(AcalAuthenticator.SERVER_ID, serverValues.getAsString(Servers._ID));
		userData.putString(AcalAuthenticator.COLLECTION_ID, collectionValues.getAsString(DavCollections._ID));
		userData.putString(AcalAuthenticator.USERNAME, serverValues.getAsString(Servers.USERNAME));
		AccountManager am = AccountManager.get(this);
		boolean accountCreated = false;
		try {
		  accountCreated = am.addAccountExplicitly(account, "", userData);
		} catch( Exception e) {
			Log.println(Constants.LOGD, TAG, Log.getStackTraceString(e));
		}
		
		if ( accountCreated ) {
		}
		
		Intent creator = getIntent();
		Bundle extras = creator.getExtras();
		if (accountCreated && extras != null) {
			AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.AcalAccountType));
			response.onResult(result);
		}
		finish();
	}

	/**
	 * <P>
	 * Handles context menu clicks
	 * </P>
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 * @author Morphoss Ltd
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		try {
		    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		    int id = preferenceListIds[info.position];
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD, TAG, "Context menu on preferenceItem " + info.position + " which I reckon is id " + id);
		    switch (item.getItemId()) {
				case CONTEXT_SYNC_NOW:
					return syncCollection(id, false);
				case CONTEXT_DISABLE:
					return disableCollection(id);
				case CONTEXT_FORCE_FULL_RESYNC:
					return syncCollection(id, true);
				default:
					return false;
			}
		}
		catch (ClassCastException e) {
			return false;
		}
	}

	
	/**
	 * <h3>Click listener for Collection Configuration List.</h3>
	 * 
	 * <P>
	 * Called when a collection is selected from the list.
	 * <p>
	 * Gets the appropriate String from this.collectionNames and uses it as a key to get the data from
	 * this.collectionData. Will start the Collection Configuration activity with this data. if 'Add Collection' was
	 * selected, Collection Configuration is sent a blank data set with MODEKEY=MODE_CREATE
	 * </p>
	 * 
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView,
	 *      android.view.View, int, long)
	 * @author Morphoss Ltd
	 */
	public boolean onPreferenceClick(Preference id) {
		int collectionId = Integer.parseInt(id.getKey());
		Intent i = this.getIntent();
		if ( android.os.Build.VERSION.SDK_INT >= 7 && i != null && ACTION_CHOOSE_ADDRESSBOOK.equals(i.getAction()) ) {
			createAuthenticatedAccount(collectionId);
		}
		else {
			// Create Intent to start new Activity
			Intent collectionConfigIntent = new Intent();
			

			// Get the collection data for the selected collection
			ContentValues toPass = collectionData.get(collectionId);

			// Begin new activity
			collectionConfigIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.CollectionConfiguration");
			collectionConfigIntent.putExtra("CollectionData", toPass);
			CollectionConfigList.this.startActivityForResult(collectionConfigIntent, UPDATE_COLLECTION_CONFIG);
		}
		return true;
	}

	/**
	 * Creates the context menus for each item in the list.
	 * 
	 * @author Morphoss Ltd
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
		menu.setHeaderTitle(getString(R.string.Collection_Options));
		menu.add(0, CollectionConfigList.CONTEXT_SYNC_NOW, 0, getString(R.string.Sync_collection_now));
		menu.add(0, CollectionConfigList.CONTEXT_DISABLE, 0, getString(R.string.Disable_collection));
		menu.add(0, CollectionConfigList.CONTEXT_FORCE_FULL_RESYNC, 0, getString(R.string.Force_full_resync));
	}

	/**
	 * Whenever this activity is resumed, update the collection list as it may have changed.
	 */
	protected void onResume() {
		super.onResume();
		
		//we must have a connection to continue
		if (updateRequested) {
			Log.println(Constants.LOGI, TAG, "Collection updated: " + updateId);
			updateRequested = false;
			if (updateId > 0) {
				try {
					if (serviceManager != null) serviceManager.close();
				}
				catch ( Exception e ) {};
				this.serviceManager = new ServiceManager(this, new ServiceManagerCallBack() {

					@Override
					public void serviceConnected(ServiceRequest serviceRequest) {
						// TODO Auto-generated method stub
						try {
							serviceRequest.fullCollectionResync(updateId);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							Log.w(TAG,Log.getStackTraceString(e));
						}
					}
					
				});
				getCollectionListItems();
				createPreferenceHierarchy();
			}
			updateId = -1;
		}
		if (this.serviceManager == null) serviceManager = new ServiceManager(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.serviceManager != null) this.serviceManager.close();
		if (mCursor != null && !mCursor.isClosed()) mCursor.close();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.serviceManager != null) this.serviceManager.close();
		serviceManager = null;
	}
	
}
