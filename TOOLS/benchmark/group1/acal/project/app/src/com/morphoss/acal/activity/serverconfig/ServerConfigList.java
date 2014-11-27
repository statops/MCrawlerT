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

package com.morphoss.acal.activity.serverconfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import android.app.ListActivity;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.ServiceRequest;

/**
 * <h3>Server Configuration List - A list of servers that can be configured</h3>
 * 
 * <p>
 * This class generates and displays the list of servers available in the
 * dav_server table. Selecting a server will start the ServerConfig activity.
 * Selecting 'Add Server' will create a new entry in the server table and start
 * the Server Configuration Activity
 * </p>
 * 
 * @author Morphoss Ltd
 * 
 */
public class ServerConfigList extends ListActivity implements OnClickListener {

	public static final String TAG = "acal ServerConfigList";
	
	// Data from the Server Table
	private String[] serverNames;
	private Map<String, ContentValues> serverData;

	// Add Server list item
	private Button addServer;
	
	// Context Menu Options
	public static final int CONTEXT_DELETE = 1;
	public static final int CONTEXT_CANCEL = 2;
	public static final int CONTEXT_EXPORT = 3;
	public static final int CONTEXT_DISCOVER = 4;

	private Cursor mCursor;

	private ServiceManager serviceManager;
	
	/**
	 * Get the server list and create the list view.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * @author Morphoss Ltd
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.servers_list);
		updateListView();
		getListView().setOnItemClickListener(new ServerListClickListener());
		registerForContextMenu(getListView());

		addServer = (Button) findViewById(R.id.AddServerButton);
		addServer.setOnClickListener(this);
		AcalTheme.setContainerFromTheme(addServer, AcalTheme.BUTTON);
	}

	/**
	 * <P>
	 * This method connects to the database and gets all server information. It
	 * creates a new ListAdapter and applies it to the ListView. It also updates
	 * our Fields and causes this activity to redraw itself. Should be called
	 * whenever there has been a change to the server table
	 * </p>
	 */
	private void updateListView() {

		// Get Data
		ContentResolver cr = getContentResolver();
		mCursor = cr.query(Servers.CONTENT_URI, null, null, null, null);
		mCursor.moveToFirst();
		ContentQueryMap cqm = new ContentQueryMap(mCursor,
				Servers.FRIENDLY_NAME, false, null);
		cqm.requery();
		this.serverData = cqm.getRows();
		mCursor.close();
		cqm.close();
		
		// Store data in useful structures
		this.serverNames = new String[this.serverData.size()];
		this.serverData.keySet().toArray(this.serverNames);
		
		// Create ListAdapter for storing the list of server names to be
		// displayed
		ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, serverNames);

		// Bind to our new adapter.
		setListAdapter(mAdapter);

		// Set the context menu
		getListView().setOnCreateContextMenuListener( new ServerListCreateContextListener());

		// make sure the display is refreshed
		getListView().refreshDrawableState();
	}

	/**
	 * 
	 * 
	 /**
	 * <p>
	 * Called when a user selects 'Delete' from the context menu. Deletes the
	 * selected server from the database.
	 * </p>
	 * 
	 * @param position
	 *            The position in ServerNames of the name of the server we are
	 *            to delete
	 * @return true if operation was successful
	 */
	private void deleteServer(int position) {
		//also need to remove: All pending resources, collections, resources, path sets
		ContentValues toDelete = serverData.get(serverNames[position]);
		int serverId = toDelete.getAsInteger(Servers._ID);
		
		Servers.deleteServer(this, serverId);
	}

	public void exportServer(int id) {
		ContentValues server = serverData.get(serverNames[id]);
		server.put(Servers.FRIENDLY_NAME, serverNames[id]);
		
		File newxmlfile = new File(Constants.PUBLIC_DATA_DIR+"/"+serverNames[id].replace(' ', '_')+".acal");
		if (newxmlfile.exists()) {
			/** @todo we may wish to handle overwrites here */
		}
		try {
			File publicDataDirectory = new File(Constants.PUBLIC_DATA_DIR);
			if ( !publicDataDirectory.exists() ) publicDataDirectory.mkdirs();

			newxmlfile.createNewFile();
			
		} catch (IOException e) {
			Log.e(TAG, "Error creating XML File: "+e);
			Toast.makeText(this, getString(R.string.errorSavingFile), Toast.LENGTH_LONG).show();
			return;
		}
		try {
			new ServerConfigData(server).writeToFile(newxmlfile);
		    Toast.makeText(this, getString(R.string.ServerDataSaved) + newxmlfile.getAbsolutePath(), Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Log.e(TAG, "Error writing to XML File: "+e);
			Toast.makeText(this, getString(R.string.errorSavingFile), Toast.LENGTH_LONG).show();
		}
	}
	

	/**
	 * <P>
	 * Handles context menu clicks
	 * </P>
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		try {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			long id = getListAdapter().getItemId(info.position);
			switch (item.getItemId()) {
			case CONTEXT_DELETE:
				deleteServer((int) id);
 				updateListView();
				return true;
			case CONTEXT_CANCEL:
				return true;
			case CONTEXT_EXPORT:
				exportServer((int)id);
				return true;
			case CONTEXT_DISCOVER:
				if ( Constants.LOG_VERBOSE ) Log.v(TAG, "Scheduling HomeSetDiscovery on successful server config.");
				try {
					ContentValues server = serverData.get(serverNames[(int)id]);
					ServiceRequest sr = serviceManager.getServiceRequest(); 
					sr.homeSetDiscovery(server.getAsInteger(Servers._ID));
				} catch (Exception e) {
					if (Constants.LOG_VERBOSE)
						Log.v(TAG,"Error starting home set discovery: "+e.getMessage()+" "+Log.getStackTraceString(e));
				}
				return true;
			default:
				return false;
			}
		} catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * <h3>Click listener for Server Configuration List.</h3>
	 * 
	 * <P>
	 * Called when a server is selected from the list.
	 * </P>
	 */
	private class ServerListClickListener implements OnItemClickListener {

		/**
		 * <p>
		 * Gets the appropriate String from this.serverNames and uses it as a
		 * key to get the data from this.serverData. Will start the Server
		 * Configuration activity with this data. if 'Add Server' was selected,
		 * Server Configuration is sent a blank data set with
		 * MODEKEY=MODE_CREATE
		 * </p>
		 * 
		 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView,
		 *      android.view.View, int, long)
		 */
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			// Create Intent to start new Activity
			Intent serverConfigIntent = new Intent();

			// Get the server data for the selected server, and add mode key so
			// configuration activity knows what to do.
			ContentValues toPass = ServerConfigList.this.serverData.get(ServerConfigList.this.serverNames[position]);
			
			if (!toPass.containsKey(ServerConfiguration.KEY_MODE))
				toPass.put(ServerConfiguration.KEY_MODE, ServerConfiguration.MODE_EDIT);

			// We need to re-insert friendly name as it was removed when
			// creating this.serverNames
			toPass.put(Servers.FRIENDLY_NAME,
					ServerConfigList.this.serverNames[position]);

			// Begin new activity
			serverConfigIntent.setClassName("com.morphoss.acal",
						"com.morphoss.acal.activity.serverconfig.ServerConfiguration");
			serverConfigIntent.putExtra("ServerData", toPass);
			ServerConfigList.this.startActivity(serverConfigIntent);
		}
	}

	/**
	 * Creates the context menus for each item in the list.
	 */
	private class ServerListCreateContextListener implements
			OnCreateContextMenuListener {

		@Override
		public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
			menu.setHeaderTitle(getString(R.string.Server_Options));
			menu.add(0, ServerConfigList.CONTEXT_EXPORT, 0, "Export");
			menu.add(0, ServerConfigList.CONTEXT_DELETE, 0, "Delete");
			menu.add(0, ServerConfigList.CONTEXT_DISCOVER, 0, "Discover Collections");
			menu.add(0, ServerConfigList.CONTEXT_CANCEL, 0, "Cancel");
			
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.serviceManager != null) this.serviceManager.close();
	}
	
	/**
	 * Whenever this activity is resumed, update the server list as it may have
	 * changed.
	 */
	@Override
	public void onResume() {
		super.onResume();
		this.serviceManager = new ServiceManager(this);
		updateListView();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mCursor != null && !mCursor.isClosed()) mCursor.close();
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG,"Add Server was clicked!");
		Intent serverConfigIntent = new Intent();
		serverConfigIntent.setClassName("com.morphoss.acal",
							"com.morphoss.acal.activity.serverconfig.AddServerList");
		startActivity(serverConfigIntent);
	}
	

}
