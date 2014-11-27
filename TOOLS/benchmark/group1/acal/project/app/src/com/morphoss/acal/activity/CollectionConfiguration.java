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

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.morphoss.acal.R;
import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.service.ServiceJob;
import com.morphoss.acal.service.SyncChangesToServer;
import com.morphoss.acal.service.SyncCollectionContents;
import com.morphoss.acal.service.WorkerClass;

/**
 * <p>This activity allows the user to configure a single collection. It MUST be handed a ContentValues object with as
 * an extra on the intent using key "CollectionData".</p>
 * 
 * <p>The ContentValues object MUST have a value with the key 
 * CollectionConfiguration.MODEKEY. If it is set to MODE_CREATE it must also contain all the fields for a collection
 * as defined by the dav_collection table schema. Failure to provide required details of the required type 
 * may cause unexpected behaviour.</p>
 * 
 * <p>This configuration screen utilises Android's Preferences library but does not persist data. If SharedPreferences
 * or data persistence is changed in the future this class may need to be corrected.</p>
 * 
 * @author Morphoss Ltd
 *
 */
public class CollectionConfiguration extends PreferenceActivity implements OnPreferenceChangeListener, OnClickListener  {
	
	//Tag for log messages
	public static final String TAG = "aCal CollectionConfiguration";
	
	//The data associated with the collection we are configuring.
	private ContentValues collectionData;
	private ContentValues originalCollectionData;	//Our snapshot of the original data. Used to determine if content has changed

	//Preferences with changeable states
	private EditTextPreference displayName;
	//private EditTextPreference colour;
	private EditTextPreference maxSyncAge3g;
	private EditTextPreference maxSyncAgeWifi;

	//Default summaries for prefs
	private HashMap<String, String> defaultSummaries = new HashMap<String,String>();

	//The root node of the preference screen
	private PreferenceScreen preferenceRoot;

	//The buttons
	private Button apply;
	private Button cancel;
	
	private ServiceManager serviceManager;
	
	/**
	 * <p>
	 * Called when activity starts. Ensures CollectionData object conforms to requirements, and exits if it
	 * does not. Calls createPreferenceHierarchy to construct preferences screen.
	 * </p>
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 * @author Morphoss Ltd
	 */
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.collection_config);
		apply = (Button) findViewById(R.id.CollectionConfigApplyButton);
		apply.setOnClickListener(this);
		apply.setEnabled(false);
		cancel = (Button) findViewById(R.id.CollectionConfigCancelButton);
		cancel.setOnClickListener(this);

		//Validate CollectionData
		try {
			collectionData = this.getIntent().getExtras().getParcelable("CollectionData");
		} catch (Exception e) {
			//unable to get data!
			this.finish();
		}
		if (collectionData == null) {
			//collection data not correctly set
			this.finish();
		}

		//ensure all required fields are present
		if (!	collectionData.containsKey(DavCollections._ID) &&
				collectionData.containsKey(DavCollections.DISPLAYNAME) &&
				collectionData.containsKey(DavCollections.SERVER_ID) &&
				collectionData.containsKey(DavCollections.ACTIVE_ADDRESSBOOK) &&
				collectionData.containsKey(DavCollections.ACTIVE_EVENTS) &&
				collectionData.containsKey(DavCollections.ACTIVE_JOURNAL) &&
				collectionData.containsKey(DavCollections.ACTIVE_TASKS) &&
				collectionData.containsKey(DavCollections.HOLDS_ADDRESSBOOK) &&
				collectionData.containsKey(DavCollections.HOLDS_EVENTS) &&
				collectionData.containsKey(DavCollections.HOLDS_JOURNAL) &&
				collectionData.containsKey(DavCollections.HOLDS_TASKS) &&
				collectionData.containsKey(DavCollections.COLLECTION_PATH) &&
				collectionData.containsKey(DavCollections.COLOUR) &&
//				collectionData.containsKey(DavCollections.DEFAULT_TIMEZONE) &&
				collectionData.containsKey(DavCollections.MAX_SYNC_AGE_3G) &&
				collectionData.containsKey(DavCollections.MAX_SYNC_AGE_WIFI) &&
				collectionData.containsKey(DavCollections.USE_ALARMS)) {
			//Required fields not supplied
			Log.e(TAG,"CollectionConfiguration called without incorrect data.");
			this.finish();
		}
		
		//CollectionData is correct, keep a copy so we can check if things are dirty later on.
		originalCollectionData = new ContentValues();
		originalCollectionData.putAll(collectionData);

		//Create a map of Field --> default summaries
		defaultSummaries.put(DavCollections.DISPLAYNAME, getString(R.string.A_name_for_this_collection));
		defaultSummaries.put(DavCollections.ACTIVE_ADDRESSBOOK, getString(R.string.Active_addressbook));
		defaultSummaries.put(DavCollections.ACTIVE_EVENTS, getString(R.string.Active_for_events));
		defaultSummaries.put(DavCollections.ACTIVE_JOURNAL, getString(R.string.Active_for_journal));
		defaultSummaries.put(DavCollections.ACTIVE_TASKS, getString(R.string.Active_for_tasks));
		defaultSummaries.put(DavCollections.COLOUR, getString(R.string.The_colour_associated_with_this_collection));
//		defaultSummaries.put(DavCollections.DEFAULT_TIMEZONE, getString(R.string.The_timezone_new_events_default_to));
		defaultSummaries.put(DavCollections.MAX_SYNC_AGE_3G, getString(R.string.The_maximum_age_for_data_while_on_3g));
		defaultSummaries.put(DavCollections.MAX_SYNC_AGE_WIFI, getString(R.string.The_maximum_age_for_data_while_on_wifi));
		defaultSummaries.put(DavCollections.USE_ALARMS, getString(R.string.Use_alarms_from_this_calendar));

		//TODO use R.String here
		defaultSummaries.put(DavCollections.COLOUR, "The colour associated with this collection.");
		
		
		//update summaries to values as required
		updateSummaries();

		//Create configuration screen
		createPreferenceHierarchy();
		setPreferenceScreen(this.preferenceRoot);
		this.preferenceRoot.setOnPreferenceChangeListener(this);
		
	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		if (this.serviceManager != null) this.serviceManager.close();
		this.serviceManager = null;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.serviceManager = new ServiceManager(this);
	}
	
	
	
	public void onClick(View v) {
		if (v.getId() == apply.getId())	applyButton();
		else if (v.getId() == cancel.getId()) cancelButton();
	}

	private void cancelButton() {
		this.finish();
	}

	private void applyButton() {
		if ( (collectionData.getAsInteger(DavCollections.ACTIVE_ADDRESSBOOK) != null && 1 == collectionData.getAsInteger(DavCollections.ACTIVE_ADDRESSBOOK) )
					|| (collectionData.getAsInteger(DavCollections.ACTIVE_EVENTS) != null && 1 == collectionData.getAsInteger(DavCollections.ACTIVE_EVENTS) )
					|| (collectionData.getAsInteger(DavCollections.ACTIVE_TASKS) != null && 1 == collectionData.getAsInteger(DavCollections.ACTIVE_TASKS) )
					|| (collectionData.getAsInteger(DavCollections.ACTIVE_JOURNAL) != null && 1 == collectionData.getAsInteger(DavCollections.ACTIVE_JOURNAL) )
							) {
			checkCollection();
		}
		else {
			saveData();
			this.finish();
		}
	}
	
	/**
	 * Confirms there's nothing too strange about our proposed data 
	 */
	private void checkCollection() {
		//Context cx = this.getBaseContext();
		// TODO might have a dialog in here at some point.
		saveData();
		this.finish();
	}
	
	public void saveData() {
		Uri provider = ContentUris.withAppendedId(DavCollections.CONTENT_URI, collectionData.getAsInteger(DavCollections._ID));
		getContentResolver().update(provider, collectionData, null, null);
		Collection.flush();

		//notify caller of change
		Intent res = new Intent();
		res.putExtra("UpdateRequired",collectionData.getAsInteger(DavCollections._ID) );
		this.setResult(RESULT_OK, res);

		ServiceJob job = null;
		if ( collectionData.getAsInteger(DavCollections.SYNC_METADATA) != null
					&& collectionData.getAsInteger(DavCollections.SYNC_METADATA) == 1 )
			job = new SyncChangesToServer();
		else
			job = new SyncCollectionContents(collectionData.getAsInteger(DavCollections._ID));

		WorkerClass.getExistingInstance().addJobAndWake(job);
		
	}
	

	private void checkTextSummary(EditTextPreference p) {
		String key = p.getKey();
		String curVal = collectionData.getAsString(key); 
		if (curVal == null  || curVal.equals("")) p.setSummary(defaultSummaries.get(key));
		else p.setSummary(p.getText());
	}
	
	private void updateSummaries() {
		//friendly_name
		createPreferenceHierarchy();
		checkTextSummary(displayName);
		setPreferenceScreen(this.preferenceRoot);
	}
	
	private void preferenceHelper(Preference preference, String title, String key, String summary) {
		preference.setPersistent(false);
		preference.setTitle(title);
		preference.setKey(key);
		preference.setSummary(summary);
		preference.setOnPreferenceChangeListener(this);
		try {
			this.preferenceRoot.addPreference(preference);
		}
		catch( Exception e ) {
			Log.e(TAG,"Failed to set preference for "+title+":::"+key+":::"+summary);
			Log.e(TAG,Log.getStackTraceString(e));
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
        
	    //Edit text preference
	    //Friendly Name
        displayName = new EditTextPreference(this);
        displayName.setDialogTitle(getString(R.string.Name));
        displayName.setText(collectionData.getAsString(DavCollections.DISPLAYNAME));
        preferenceHelper(displayName,getString(R.string.Name),DavCollections.DISPLAYNAME, defaultSummaries.get(DavCollections.DISPLAYNAME));

        if ( collectionData.getAsInteger(DavCollections.HOLDS_ADDRESSBOOK) == 1 ) {
            //active for addresses
            CheckBoxPreference activeAddressbook = new CheckBoxPreference(this);
            boolean check = (collectionData.getAsInteger(DavCollections.ACTIVE_ADDRESSBOOK) == 1);
            activeAddressbook.setChecked(check);
            activeAddressbook.setTitle(R.string.pActive_addressbook);
            activeAddressbook.setOnPreferenceChangeListener(this); 
            preferenceHelper(activeAddressbook, getString(R.string.Active), DavCollections.ACTIVE_ADDRESSBOOK, defaultSummaries.get(DavCollections.ACTIVE_ADDRESSBOOK));        	
        }
        else {
        	boolean check;
	        if ( (collectionData.getAsInteger(DavCollections.HOLDS_EVENTS) != null) && collectionData.getAsInteger(DavCollections.HOLDS_EVENTS) == 1) {
		        //active for events
		        CheckBoxPreference activeEvents = new CheckBoxPreference(this);
		        check = (collectionData.getAsInteger(DavCollections.ACTIVE_EVENTS)!= null && collectionData.getAsInteger(DavCollections.ACTIVE_EVENTS) == 1);
		        activeEvents.setChecked(check);
	            activeEvents.setTitle(R.string.pActive_for_events);
		        activeEvents.setOnPreferenceChangeListener(this); 
		        preferenceHelper(activeEvents, getString(R.string.pActive_for_events), DavCollections.ACTIVE_EVENTS, defaultSummaries.get(DavCollections.ACTIVE_EVENTS));
	        }
	

	        if ( (collectionData.getAsInteger(DavCollections.HOLDS_TASKS) != null) && collectionData.getAsInteger(DavCollections.HOLDS_TASKS) == 1) {
		        //active for tasks
		        CheckBoxPreference activeTasks = new CheckBoxPreference(this);
		        check = (collectionData.getAsInteger(DavCollections.ACTIVE_TASKS) != null && collectionData.getAsInteger(DavCollections.ACTIVE_TASKS) == 1);
		        activeTasks.setChecked(check);
	            activeTasks.setTitle(R.string.pActive_for_tasks);
		        activeTasks.setOnPreferenceChangeListener(this); 
		        preferenceHelper(activeTasks, getString(R.string.pActive_for_tasks), DavCollections.ACTIVE_TASKS, defaultSummaries.get(DavCollections.ACTIVE_TASKS));
	        }
	
	        if ( (collectionData.getAsInteger(DavCollections.HOLDS_JOURNAL) != null) && collectionData.getAsInteger(DavCollections.HOLDS_JOURNAL) == 1) {
		        //active for journal
		        CheckBoxPreference activeJournal = new CheckBoxPreference(this);
		        check = (collectionData.getAsInteger(DavCollections.ACTIVE_JOURNAL) != null && collectionData.getAsInteger(DavCollections.ACTIVE_JOURNAL) == 1);
		        activeJournal.setChecked(check);
	            activeJournal.setTitle(R.string.pActive_for_journal);
		        activeJournal.setOnPreferenceChangeListener(this); 
		        preferenceHelper(activeJournal, getString(R.string.pActive_for_journal), DavCollections.ACTIVE_JOURNAL, defaultSummaries.get(DavCollections.ACTIVE_JOURNAL));
	        }

	        //active for alarms
	        CheckBoxPreference activeAlarms = new CheckBoxPreference(this);
	        check = (collectionData.getAsInteger(DavCollections.USE_ALARMS)!= null && collectionData.getAsInteger(DavCollections.USE_ALARMS) == 1);
	        activeAlarms.setChecked(check);
            activeAlarms.setTitle(R.string.pUse_Alarms);
	        activeAlarms.setOnPreferenceChangeListener(this); 
	        preferenceHelper(activeAlarms, getString(R.string.Use_Alarms), DavCollections.USE_ALARMS, defaultSummaries.get(DavCollections.USE_ALARMS));

        }


        //timezone
//        ListPreference defaultTimezone = new ListPreference(this);
//        defaultTimezone.setEntries(StaticHelpers.getZoneNames());
//        defaultTimezone.setEntryValues(StaticHelpers.getZoneIDs());
//        defaultTimezone.setDialogTitle(getString(R.string.Default_timezone));
//        defaultTimezone.setDefaultValue(collectionData.getAsString(DavCollections.DEFAULT_TIMEZONE));
//        preferenceHelper(defaultTimezone, getString(R.string.Default_timezone), DavCollections.DEFAULT_TIMEZONE, defaultSummaries.get(DavCollections.DEFAULT_TIMEZONE));

        //colour
        ColourPickerPreference collectionColor = new ColourPickerPreference(this, null);
        try {
        	int col = Color.parseColor(collectionData.getAsString(DavCollections.COLOUR));
        	collectionColor.setColor(col);
        } catch (Exception e) {
        	collectionColor.setColor(0x00FF00);
        }
        collectionColor.setOnPreferenceChangeListener(this);
        preferenceHelper(collectionColor, getString(R.string.Colour), DavCollections.COLOUR, defaultSummaries.get(DavCollections.COLOUR));

    	// sync age
        maxSyncAge3g = new EditTextPreference(this);
        int currentSyncAge = 3600000;
        try {
        	currentSyncAge = collectionData.getAsInteger(DavCollections.MAX_SYNC_AGE_3G);
        }
        catch( Exception e ) {
        	currentSyncAge = 3600000;
        }
        maxSyncAge3g.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        maxSyncAge3g.setDialogTitle(getString(R.string.pMax_age_on_3g));
        maxSyncAge3g.setDefaultValue(Integer.toString( currentSyncAge / 60000));
        maxSyncAge3g.setText(Integer.toString(collectionData.getAsInteger(DavCollections.MAX_SYNC_AGE_3G) / 60000));
    	preferenceHelper(maxSyncAge3g, getString(R.string.pMax_age_on_3g), DavCollections.MAX_SYNC_AGE_3G, defaultSummaries.get(DavCollections.MAX_SYNC_AGE_3G));

        maxSyncAgeWifi = new EditTextPreference(this);
        try {
        	currentSyncAge = collectionData.getAsInteger(DavCollections.MAX_SYNC_AGE_WIFI);
        }
        catch( Exception e ) {
        	currentSyncAge = 300000;
        }
        maxSyncAgeWifi.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        maxSyncAgeWifi.setDialogTitle(getString(R.string.pMax_age_on_wifi));
        maxSyncAgeWifi.setDefaultValue(Integer.toString( currentSyncAge / 60000));
        maxSyncAgeWifi.setText(Integer.toString(collectionData.getAsInteger(DavCollections.MAX_SYNC_AGE_WIFI)/ 60000));
    	preferenceHelper(maxSyncAgeWifi, getString(R.string.pMax_age_on_wifi), DavCollections.MAX_SYNC_AGE_WIFI, defaultSummaries.get(DavCollections.MAX_SYNC_AGE_WIFI));

	}
	
	
	
	/**
	 * <p>
	 * Identifies which method changed and calls the appropriate method. Returns true
	 * if the value update was accepted.
	 * </p>
	 * 
	 * @return True if the requested value change was accepted.
	 * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
	 * @author Morphoss Ltd
	 */
	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) {
		String key = pref.getKey();
		boolean ret = false;
		if (key.equals(DavCollections.DISPLAYNAME)) ret = validateDisplayName(pref,newValue);
		else if (key.equals(DavCollections.ACTIVE_ADDRESSBOOK)) ret = validateActive(pref,newValue,DavCollections.ACTIVE_ADDRESSBOOK,DavCollections.HOLDS_ADDRESSBOOK);
		else if (key.equals(DavCollections.ACTIVE_EVENTS)) ret = validateActive(pref,newValue,DavCollections.ACTIVE_EVENTS,DavCollections.HOLDS_EVENTS);
		else if (key.equals(DavCollections.ACTIVE_JOURNAL)) ret = validateActive(pref,newValue,DavCollections.ACTIVE_JOURNAL,DavCollections.HOLDS_JOURNAL);
		else if (key.equals(DavCollections.ACTIVE_TASKS)) ret = validateActive(pref,newValue,DavCollections.ACTIVE_TASKS,DavCollections.HOLDS_TASKS);
		else if (key.equals(DavCollections.USE_ALARMS)) ret = validateAlarms(pref,newValue);
//		else if (key.equals(DavCollections.DEFAULT_TIMEZONE)) ret = validateDefaultTimezone(pref,newValue);
		else if (key.equals(DavCollections.COLOUR)) ret = validateColor(pref,newValue); 
		else if (key.equals(DavCollections.MAX_SYNC_AGE_3G))   ret = validateSyncAge(pref,newValue, DavCollections.MAX_SYNC_AGE_3G); 
		else if (key.equals(DavCollections.MAX_SYNC_AGE_WIFI)) ret = validateSyncAge(pref,newValue, DavCollections.MAX_SYNC_AGE_WIFI); 
		
		if (!this.originalCollectionData.equals(this.collectionData)) apply.setEnabled(true);
		updateSummaries();
		return ret;
	}
	
	/************************************************************************
	 * 							   VALIDATION METHODS                       *
	 * **********************************************************************
	 * 
	 * <p>
	 * Below is a set of all the validation methods for responding to user input.
	 * They all operate the same - Check what the user entered, if it is valid update
	 * collectionData and return true, otherwise return false.
	 * </p>
	 * 
	 * @param p The Preference that was changed
	 * @param v The new value that was entered
	 * @return True if the new value was accepted and collectionData was updated.
	 */
	
	private boolean validateDisplayName(Preference p, Object v) {
		if (v != null && v instanceof String && !v.equals("")) {
			this.collectionData.put(DavCollections.DISPLAYNAME, (String)v);
			return true;
		} 
		else {
			return false;
		}
	}

	public boolean validateAlarms(Preference p, Object v) {
		int enabled = ((Boolean)v ? 1 : 0);
		collectionData.put(DavCollections.USE_ALARMS, enabled);
		return true;
	}
	
	/**
	 * <p>
	 * OK. They don't quite *all* operate the same :-) This one takes an extra parameter which is the name of
	 * the field which masks it's possible values. So ACTIVE_ADDRESSBOOK can only be true if HOLDS_ADDRESSBOOK
	 * is true, for example.
	 * </p>
	 * 
	 * @param p
	 * @param v
	 * @param maskField
	 * @return
	 */
	private boolean validateActive(Preference p, Object v, String updateToField, String maskField ) {
		//CheckBoxPreference cbp = (CheckBoxPreference)p;
		boolean value = (Boolean)v;
		boolean maskValue = (1 == collectionData.getAsInteger(maskField));
		int toPut = (value & maskValue? 1 : 0);
		collectionData.put(updateToField, toPut);
		
		return true;
	}

//	private boolean validateDefaultTimezone(Preference p, Object v) { 
//		if (v != null && v instanceof String && !v.equals("")) {
//			this.collectionData.put(DavCollections.DEFAULT_TIMEZONE, (String)v);
//			return true;
//		} 
//		else {
//			return false;
//		}
//	}
	
	private boolean validateColor(Preference p, Object v) {
			//we don't need to validate colour - but we do need to turn it into a string
			this.collectionData.put(DavCollections.COLOUR, String.format("#%06x", (Integer)v));
			this.collectionData.put(DavCollections.SYNC_METADATA, 1);
			return true;
	}

	
	private boolean validateSyncAge(Preference p, Object v, String field) {
		String value = (String) v;
		if (value.equals("")) {
			// Blank is allowed
			collectionData.put(field, "1800000");
			return true;
		}
		else {
			long syncAge;
			try {
				syncAge = Long.parseLong(value) * 60000L;
			} catch (Exception e) {
				//Can't parse port!
				return false;
			}
			if ( syncAge <= 0 || syncAge > Integer.MAX_VALUE ) {
				syncAge = Integer.MAX_VALUE;
			}
			collectionData.put(field, syncAge);
			return true;
		}
	}
	
}
