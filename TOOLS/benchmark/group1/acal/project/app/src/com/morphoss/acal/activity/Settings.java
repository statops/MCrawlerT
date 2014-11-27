/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.StaticHelpers;

/**
 * The main settings list. A list of settings that can be configured.
 * 
 * TODO Make this better able to be localised
 * 
 * @author Morphoss Ltd
 */
public class Settings extends ListActivity {
	
	public static final String TAG = "aCal Settings";

	/** A list of settings that can be configured. (Standard set) */
	private String[] TASKS		= null;
	
	/** A list of setting that can be configured in DEBUG mode */
	private String[] DEBUG_TASKS = new String[] { "Debug Options"};
	
	/** The result of adding TASKS to (DEBUG = true) ? DEBUG_TASKS : NULL */
	private String[] taskList;
	
	/**
	 * Creates a list of settings.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TASKS = new String[] { getString(R.string.appActivityServerConfigList),
					getString(R.string.appActivityCollectionConfigList),
					getString(R.string.showUpdateInformation),
					getString(R.string.appActivityPreference)};
		
		if (Constants.DEBUG_SETTINGS) {
			taskList = StaticHelpers.mergeArrays(TASKS, DEBUG_TASKS);
		} else taskList = TASKS;
		
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, taskList));
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new SettingsListClickListener());
	}
	
	/**
	 * Method for starting "Servers" activity. 
	 */
	public void serverConfig() {
		Intent serverConfigIntent = new Intent();
    	serverConfigIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.serverconfig.ServerConfigList");
    	this.startActivity(serverConfigIntent);
	}
	
	/**
	 * Method for starting "Calendars and Addressbooks" activity. 
	 */
	public void collectionConfig() {
		Intent collectionConfigIntent = new Intent();
    	collectionConfigIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.CollectionConfigList");
    	this.startActivity(collectionConfigIntent);
	}
	
	/**
	 * Method for starting "AcalPreferences" activity. 
	 */
	public void preferences() {
		Intent preferencesIntent = new Intent();
    	preferencesIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.AcalPreferences");
    	this.startActivity(preferencesIntent);
	}
	
	/**
	 * Method for starting "ShowUpgradeChanges" activity. 
	 */
	public void showUpdateInformation() {
		Intent upgradeChangesIntent = new Intent();
		Bundle b = new Bundle();
		b.putCharSequence("REDISPLAY", "REDISPLAY");
		upgradeChangesIntent.putExtras(b);
    	upgradeChangesIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.ShowUpgradeChanges");
    	this.startActivity(upgradeChangesIntent);
	}
	
	/**
	 * Method for starting "Debug Options" activity. 
	 */
	public void debugOptions() {
		Intent serverConfigIntent = new Intent();
    	serverConfigIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.DebugSettings");
    	this.startActivity(serverConfigIntent);
	}

	
	/**
	 * Click listener for Settings List.
	 * 
	 * @author Morphoss Ltd
	 */
	
	private class SettingsListClickListener implements OnItemClickListener {
		 
		/**
		 * Responds to button presses and starts the appropriate activity.
		 * 
		 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
		 */
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			String task = taskList[position];
			if ( task.equals(getString(R.string.appActivityServerConfigList)) ) {
				serverConfig();
			}
			else if ( task.equals(getString(R.string.appActivityCollectionConfigList)) ) {
				collectionConfig();
			}
			else if ( task.equals(getString(R.string.appActivityPreference)) ) {
				preferences();
			}
			else if ( task.equals(getString(R.string.showUpdateInformation)) ) {
				showUpdateInformation();
			}
			else if ( task.equals("Debug Options") ) {
				debugOptions();
			}
		}
	}
}
