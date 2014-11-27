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

import android.app.ListActivity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.requests.CRClearCacheRequest;
import com.morphoss.acal.service.SyncChangesToServer;
import com.morphoss.acal.service.WorkerClass;

public class DebugSettings extends ListActivity {
public static final String TAG = "aCal Settings";
	
	/** A list of setting that can be configured in DEBUG mode */
	private static final String[] TASKS = new String[] {
		"Save Database",
		"Revert Database", 
		"Full System Sync",
		"Home Set Discovery",
		"Update Home DavCollections",
		"Sync All DavCollections",
		"Clear Cache",
		"Sync local changes to server"
	};
	
	private ServiceManager serviceManager;
	
	
	
	/**
	 * <p>Creates a list of debug settings.</p>
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, TASKS));
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new SettingsListClickListener());
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (this.serviceManager != null) this.serviceManager.close();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.serviceManager = new ServiceManager(this);
	}
	
	
	
	/**
	 * Click listener for Settings List.
	 * 
	 * @author Morphoss Ltd
	 */
	
	private class SettingsListClickListener implements OnItemClickListener {
		 
		/**
		 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
		 */
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String task = TASKS[position];
			if (task.equals("Save Database")){
	    		try {
	    			DebugSettings.this.serviceManager.getServiceRequest().saveDatabase();
	    			return;
	    		} catch (RemoteException re) {
	    			Log.e(TAG, "Unable to send save database request to server: "+re.getMessage());
	    			Toast.makeText(DebugSettings.this, "Request failed: "+re.getMessage(), Toast.LENGTH_SHORT).show();
	    		}
	    	}
			else if (task.equals("Revert Database")){
	    		try {
	    			DebugSettings.this.serviceManager.getServiceRequest().revertDatabase();
	    			return;
	    		} catch (RemoteException re) {
	    			Log.e(TAG, "Unable to send Revert Database request to server: "+re.getMessage());
	    			Toast.makeText(DebugSettings.this, "Request failed: "+re.getMessage(), Toast.LENGTH_SHORT).show();
	    		}
	    	}
			else if (task.equals("Full System Sync")) {
	    		try {
	    			DebugSettings.this.serviceManager.getServiceRequest().fullResync();
	    			return;
	    		} catch (RemoteException re) {
	    			Log.e(TAG, "Unable to send Full System Sync request to server: "+re.getMessage());
	    			Toast.makeText(DebugSettings.this, "Request failed: "+re.getMessage(), Toast.LENGTH_SHORT).show();
	    		}
	    	}
			else if (task.equals("Home Set Discovery")) {
	    		try {
	    			DebugSettings.this.serviceManager.getServiceRequest().discoverHomeSets();
	    			return;
	    		} catch (RemoteException re) {
	    			Log.e(TAG, "Unable to send Home Set Discovery request to server: "+re.getMessage());
	    			Toast.makeText(DebugSettings.this, "Request failed: "+re.getMessage(), Toast.LENGTH_SHORT).show();
	    		}
	    	}
			else if (task.equals("Update Home DavCollections")) {
	    		try {
	    			DebugSettings.this.serviceManager.getServiceRequest().updateCollectionsFromHomeSets();
	    			return;
	    		} catch (RemoteException re) {
	    			Log.e(TAG, "Unable to send Update Home DavCollections request to server: "+re.getMessage());
	    			Toast.makeText(DebugSettings.this, "Request failed: "+re.getMessage(), Toast.LENGTH_SHORT).show();
	    		}
	    	} else if (task.equals("Clear Cache")) {
	    		CacheManager.getInstance(DebugSettings.this).sendRequest(new CRClearCacheRequest());
	    	}
	    	else if ( task.equals("Sync local changes to server") ) {
				WorkerClass.getExistingInstance().addJobAndWake(new SyncChangesToServer());
	    	}
		}
	}
}
