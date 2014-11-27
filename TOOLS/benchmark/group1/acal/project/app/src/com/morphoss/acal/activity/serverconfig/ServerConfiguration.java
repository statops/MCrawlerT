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

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.R;
import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.ServiceRequest;

/**
 * <p>This activity allows the user to configure a single server. It MUST be handed a ContentValues object with as
 * an extra on the intent using key "ServerData".</p>
 * 
 * <p>The ContentValues object MUST have a value with the key 
 * ServerConfiguration.MODEKEY. If it is set to MODE_CREATE it must also contain all the fields for a server
 * as defined by the dav_server table schema. Failure to provide required details of the required type 
 * may cause unexpected behaviour.</p>
 * 
 * <p>This configuration screen utilises Android's Preferences library but does not persist data. If SharedPreferences
 * or data persistence is changed in the future this class may need to be corrected.</p>
 *
 */
public class ServerConfiguration extends PreferenceActivity implements OnPreferenceChangeListener, OnClickListener, ServerConfigurator  {
	
	//Tag for log messages
	public static final String TAG = "ServerConfiguration";
	
	//The data associated with the server we are configuring.
	private ContentValues serverData;
	private ContentValues originalServerData;	//Our snapshot of the original data. Used to detemine if content has changed
	public int iface = INTERFACE_SIMPLE;

	//The Key to be used in serverData for storing the mode value.
	public static final String KEY_MODE = "MODE";

	//The Key to be used in serverData for storing the image resourceId
	public static final String KEY_IMAGE = "IMAGE_RESOURCE";


	//The Modes available for this activity. serverData MUST have one of these or the activity will abort
	public static final int MODE_EDIT = 1;
	public static final int MODE_CREATE = 2;
	public static final int MODE_IMPORT = 3;
	
	public static final int INTERFACE_SIMPLE = 0;
	public static final int INTERFACE_ADVANCED = 1;
	
	//Preferences with changeable states
	private EditTextPreference friendlyName;
	private EditTextPreference calendarUserURL;
	private EditTextPreference hostname;	
	private EditTextPreference port;
	private EditTextPreference principalPath;
	private EditTextPreference username;
	private EditTextPreference password;
	
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
	 * Called when activity starts. Ensures ServerData object conforms to requirements, and exits if it
	 * does not. Calls createPreferenceHierarchy to construct preferences screen.
	 * </p>
	 * 
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 * @author Morphoss Ltd
	 */
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.server_config);
		apply = (Button) findViewById(R.id.ServerConfigApplyButton);
		apply.setOnClickListener(this);
		apply.setEnabled(false);
		cancel = (Button) findViewById(R.id.ServerConfiCancelButton);
		cancel.setOnClickListener(this);
		

		//Validate ServerData
		try {
			serverData = this.getIntent().getExtras().getParcelable("ServerData");
		} catch (Exception e) {
			serverData = new ContentValues();
			serverData.put(KEY_MODE, MODE_CREATE);
		}
		if ( serverData == null || !serverData.containsKey(KEY_MODE) ) {
			//server data not correctly set
			this.finish();
		}
		if ( serverData == null || serverData.getAsInteger(KEY_MODE) == MODE_CREATE) {
			createDefaultValues();
		}
		else if (serverData.getAsInteger(KEY_MODE) == MODE_IMPORT) {
			createDefaultValuesForMissing();
		}
		else if (serverData.getAsInteger(KEY_MODE) == MODE_EDIT){
			//ensure all required fields are present
			if (!	serverData.containsKey(Servers.FRIENDLY_NAME) &&
					serverData.containsKey(Servers.SUPPLIED_USER_URL) &&
//					serverData.containsKey(Servers.SUPPLIED_PATH) &&
					serverData.containsKey(Servers.HOSTNAME) &&
					serverData.containsKey(Servers.PRINCIPAL_PATH) &&
					serverData.containsKey(Servers.USERNAME) &&
					serverData.containsKey(Servers.PASSWORD) &&
					serverData.containsKey(Servers.PORT) &&
					serverData.containsKey(Servers.AUTH_TYPE) &&
					serverData.containsKey(Servers.ACTIVE) &&
					serverData.containsKey(Servers.USE_SSL))
				//Required fields not supplied
				this.finish();
		} else {
			//invalid mode
			this.finish();
		}
		
		//ServerData is correct, keep a copy so we can check if things are dirty later on.
		originalServerData = new ContentValues();
		originalServerData.putAll(serverData);

		//Create a map of Field --> default summaries
		defaultSummaries.put(Servers.FRIENDLY_NAME, getString(R.string.A_name_for_this_server_configuration));
		defaultSummaries.put(Servers.SUPPLIED_USER_URL, getString(R.string.A_URL_or_domain_name));
//		defaultSummaries.put(Servers.SUPPLIED_PATH, getString(R.string.The_path_on_the_server));
		defaultSummaries.put(Servers.USE_SSL, getString(R.string.Whether_to_use_encryption));
		defaultSummaries.put(Servers.HOSTNAME, getString(R.string.The_servers_hostname));
		defaultSummaries.put(Servers.PORT, getString(R.string.The_port_to_connect_to));
		defaultSummaries.put(Servers.PRINCIPAL_PATH, getString(R.string.The_path_on_the_server));
		defaultSummaries.put(Servers.AUTH_TYPE, getString(R.string.The_authentication_type_used));
		defaultSummaries.put(Servers.USERNAME, getString(R.string.The_username_to_use));
		defaultSummaries.put(Servers.PASSWORD, getString(R.string.The_password_to_use));
		defaultSummaries.put(Servers.ACTIVE, getString(R.string.Whether_this_server_is_active));
		
		//Create configuration screen
		createPreferenceHierarchy();
		setPreferenceScreen(this.preferenceRoot);
		this.preferenceRoot.setOnPreferenceChangeListener(this);

		//update summaries to values as required
		updateSummaries();
	
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
	 * <p>Populate serverData with default values. Called when in create mode.</p>
	 */
	private void createDefaultValuesForMissing() {
		if (!	serverData.containsKey(Servers.FRIENDLY_NAME)) serverData.put(Servers.FRIENDLY_NAME,"");
		if (!	serverData.containsKey(Servers.HOSTNAME)) serverData.put(Servers.HOSTNAME,"");
		if (!	serverData.containsKey(Servers.SUPPLIED_USER_URL)) serverData.put(Servers.SUPPLIED_USER_URL,"");
		if (!	serverData.containsKey(Servers.USERNAME)) serverData.put(Servers.USERNAME,"");
		if (!	serverData.containsKey(Servers.PASSWORD)) serverData.put(Servers.PASSWORD,"");
		if (!	serverData.containsKey(Servers.PORT)) serverData.put(Servers.PORT,"");
		if (!	serverData.containsKey(Servers.AUTH_TYPE)) serverData.put(Servers.AUTH_TYPE,1);
		if (!	serverData.containsKey(Servers.ACTIVE)) serverData.put(Servers.ACTIVE,1);
		if (!	serverData.containsKey(Servers.USE_SSL)) serverData.put(Servers.USE_SSL,1);
		apply.setEnabled(true);
		serverData.put(KEY_MODE, MODE_CREATE);
	}
	
	private void createDefaultValues() {
		serverData.put(Servers.FRIENDLY_NAME,"");
		serverData.put(Servers.HOSTNAME,"");
		serverData.put(Servers.SUPPLIED_USER_URL,"");
		serverData.put(Servers.USERNAME,"");
		serverData.put(Servers.PASSWORD,"");
		serverData.put(Servers.PORT,"");
		serverData.put(Servers.AUTH_TYPE,1);
		serverData.put(Servers.ACTIVE,1);
		serverData.put(Servers.USE_SSL,1);
	}
	
	public void onClick(View v) {
		if (v.getId() == apply.getId())	applyButton();
		else if (v.getId() == cancel.getId()) cancelButton();
	}

	private void cancelButton() {
		this.finish();
	}

	private void applyButton() {
		if ( serverData.getAsInteger(Servers.ACTIVE) == 1 ) {
			checkServer();
		}
		else {
			saveData();
			this.finish();
		}
	}
	
	/**
	 * Attempts to connect to server and get configuration. 
	 */
	private void checkServer() {
		Context cx = this.getBaseContext();
		CheckServerDialog csd = new CheckServerDialog(this,serverData, cx, this.serviceManager);
		csd.start();
	}
	
	public void saveData() {
		switch (serverData.getAsInteger(KEY_MODE)) {
			case MODE_EDIT:
				updateRecord();
				break;
			case MODE_CREATE:
				if (this.serverData.get(Servers.FRIENDLY_NAME).equals(""))
					this.serverData.put(Servers.FRIENDLY_NAME, getString(R.string.UnNamedServer));
				createRecord();
				break;
		}

		if ( serverData.getAsInteger(Servers.ACTIVE) == 1 ) {
			// Start syncing server in background
			if (Constants.LOG_VERBOSE) Log.v(TAG, "Scheduling HomeSetDiscovery on successful server config.");
			try {
				int serverId = serverData.getAsInteger(Servers._ID);
				ServiceRequest sr = serviceManager.getServiceRequest();
				sr.homeSetDiscovery(serverId);
			}
			catch (RemoteException e) {
				if (Constants.LOG_VERBOSE)
					Log.v(TAG, "Error starting home set discovery: " + e.getMessage() + " "
								+ Log.getStackTraceString(e));
			}
			catch ( Exception e ) {
				Log.e(TAG,Log.getStackTraceString(e));
			}
		}
    }

	public void finishAndClose() {
		this.setResult(RESULT_OK);
		this.finish();
	}

	/**
	 * <p>Called when exiting/pausing activity in CREATE Mode. Creates a new record. Changes activity from
	 * CREATE mode to EDIT mode if update was successful.</p>
	 */
	private void createRecord() {
		try {
			Uri result = getContentResolver().insert(Servers.CONTENT_URI, Servers.cloneValidColumns(serverData));
			int id = Integer.parseInt(result.getPathSegments().get(0));
			if (id < 0) throw new Exception("Failed to add server");
			serverData.put(Servers._ID, id);
			//IMPORTANT if we don't change the mode its possible more than one record will be created.
			serverData.put(KEY_MODE, MODE_EDIT);

			//Update that we have a configured server now.
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			prefs.edit().putInt(PrefNames.serverIsConfigured, 1).commit();
			
		} catch (Exception e) {
			//error updating
			serverData.put(KEY_MODE, MODE_CREATE);
			Toast.makeText(this, getString(R.string.errorSavingServerConfig), Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * <p>
	 * Called when exiting/pausing activity. Writes the current configuration
	 * values to the DB
	 * </p>
	 */
	private void updateRecord() {
		Uri provider = ContentUris.withAppendedId(Servers.CONTENT_URI, serverData.getAsInteger(Servers._ID));
		getContentResolver().update(provider, Servers.cloneValidColumns(serverData), null, null);
	}

	
	private void checkTextSummary(EditTextPreference p) {
		String key = p.getKey();
		String curVal = serverData.getAsString(key); 
		if (curVal == null  || curVal.equals("")) p.setSummary(defaultSummaries.get(key));
		else p.setSummary(p.getText());
	}
	
	private void updateSummaries() {
		//friendly_name
		createPreferenceHierarchy();
		checkTextSummary(friendlyName);
		checkTextSummary(username);
		checkTextSummary(calendarUserURL);
		if ( iface != INTERFACE_SIMPLE) {
			checkTextSummary(hostname);
			checkTextSummary(principalPath);
			checkTextSummary(port);
		}
		setPreferenceScreen(this.preferenceRoot);
	}
	
	private void preferenceHelper(Preference preference, String title, String key, String summary) {
		preference.setPersistent(false);
		preference.setTitle(title);
		preference.setKey(key);
		preference.setSummary(summary);
		preference.setOnPreferenceChangeListener(this); 
        this.preferenceRoot.addPreference(preference);
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
        friendlyName = new EditTextPreference(this);
        friendlyName.getEditText().setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        friendlyName.setDialogTitle(getString(R.string.Name));
        friendlyName.setText(serverData.getAsString(Servers.FRIENDLY_NAME));
        preferenceHelper(friendlyName,getString(R.string.Name),Servers.FRIENDLY_NAME, defaultSummaries.get(Servers.FRIENDLY_NAME));

        //active
        CheckBoxPreference togglePref = new CheckBoxPreference(this);
        boolean check = (serverData.getAsInteger(Servers.ACTIVE) == 1);
        togglePref.setChecked(check);
        togglePref.setOnPreferenceChangeListener(this); 
        preferenceHelper(togglePref, getString(R.string.Active), Servers.ACTIVE, defaultSummaries.get(Servers.ACTIVE));

        //username
        username = new EditTextPreference(this);
        username.setDialogTitle(getString(R.string.Username));
        username.setDefaultValue(serverData.getAsString(Servers.USERNAME));
        username.setOnPreferenceChangeListener(this); 
        preferenceHelper(username, getString(R.string.Username), Servers.USERNAME,  defaultSummaries.get(Servers.USERNAME));
        
        //password
        password = new EditTextPreference(this);
        password.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);  // So we don't show the password through predictive dictionary
        password.getEditText().setTransformationMethod(new PasswordTransformationMethod());
        password.setDialogTitle(getString(R.string.Password));
        password.setDefaultValue(serverData.get(Servers.PASSWORD));
        password.setOnPreferenceChangeListener(this);
        preferenceHelper(password, getString(R.string.Password), Servers.PASSWORD, defaultSummaries.get(Servers.PASSWORD));

        if (this.iface == INTERFACE_SIMPLE) {
        	//supplied_domain
        	calendarUserURL = new EditTextPreference(this);
        	calendarUserURL.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        	calendarUserURL.setDialogTitle(getString(R.string.Simple_User_URL));
        	calendarUserURL.setDefaultValue(serverData.getAsString(Servers.SUPPLIED_USER_URL));
        	preferenceHelper(calendarUserURL, getString(R.string.Simple_User_URL), Servers.SUPPLIED_USER_URL, defaultSummaries.get(Servers.SUPPLIED_USER_URL)); 

        	/*
        	//supplied_path
        	suppliedPath = new EditTextPreference(this);
        	suppliedPath.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        	suppliedPath.setDialogTitle(getString(R.string.Simple_Path));
        	suppliedPath.setDefaultValue(serverData.getAsString(Servers.SUPPLIED_PATH));
        	preferenceHelper(suppliedPath, getString(R.string.Simple_Path), Servers.SUPPLIED_PATH, defaultSummaries.get(Servers.SUPPLIED_PATH));
        	*/
       }
        
        if (this.iface == INTERFACE_ADVANCED) {
        	//hostname
        	hostname = new EditTextPreference(this);
        	hostname.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        	hostname.setDialogTitle(getString(R.string.Server_Name));
        	hostname.setDefaultValue(serverData.getAsString(Servers.HOSTNAME));
        	preferenceHelper(hostname, getString(R.string.Server_Name), Servers.HOSTNAME, defaultSummaries.get(Servers.HOSTNAME));
        
        	//principal_path
        	principalPath = new EditTextPreference(this);
        	principalPath.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        	principalPath.setDialogTitle(getString(R.string.Server_Path));
        	principalPath.setDefaultValue(serverData.getAsString(Servers.PRINCIPAL_PATH));
        	preferenceHelper(principalPath, getString(R.string.Server_Path), Servers.PRINCIPAL_PATH, defaultSummaries.get(Servers.PRINCIPAL_PATH));

        	//auth
        	ListPreference listPref = new ListPreference(this);
        	listPref.setEntries(new String[] { "None", "Basic", "Digest"});
        	listPref.setEntryValues(new String[] {
        				Integer.toString(Servers.AUTH_NONE),
        				Integer.toString(Servers.AUTH_BASIC), 
        				Integer.toString(Servers.AUTH_DIGEST)
        			});
        	listPref.setDialogTitle(getString(R.string.Authentication_Type));
        	listPref.setDefaultValue(serverData.getAsString(Servers.AUTH_TYPE));
        	preferenceHelper(listPref, getString(R.string.Authentication_Type), Servers.AUTH_TYPE, defaultSummaries.get(Servers.AUTH_TYPE));
                        
        	//use_ssl
        	togglePref = new CheckBoxPreference(this);
        	check = (serverData.getAsInteger(Servers.USE_SSL) == 1);
        	togglePref.setChecked(check);
        	preferenceHelper(togglePref,getString(R.string.Use_SSL), Servers.USE_SSL, defaultSummaries.get(Servers.USE_SSL) );

        	//port
        	port = new EditTextPreference(this);
        	port.setDialogTitle(getString(R.string.Server_Port));
        	port.setDefaultValue(serverData.getAsString(Servers.PORT));
            port.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        	preferenceHelper(port, getString(R.string.Server_Port), Servers.PORT, defaultSummaries.get(Servers.PORT));
        }
        
   	}
	
	
	
	/**
	 * <p>
	 * Identifies which method changed and calls the appropriate method. Returns true
	 * if the value update was accepted.
	 * </p>
	 * 
	 * @return True if the requested value change was accepted.
	 * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange(android.preference.Preference, java.lang.Object)
	 */
	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) {
		String key = pref.getKey();
		boolean ret = false;
		if ( key.equals(Servers.FRIENDLY_NAME) ) ret = validateFriendlyName(pref, newValue);
		else if ( key.equals(Servers.USE_SSL) ) ret = validateUseSSL(pref, newValue);
		else if ( key.equals(Servers.HOSTNAME) ) ret = validateHostName(pref, newValue);
		else if ( key.equals(Servers.PORT) ) ret = validatePort(pref, newValue);
		else if ( key.equals(Servers.SUPPLIED_USER_URL) ) ret = validateUrl(pref, newValue);
		else if ( key.equals(Servers.PRINCIPAL_PATH) ) ret = validatePrincipalPath(pref, newValue);
		else if ( key.equals(Servers.AUTH_TYPE) ) ret = validateAuth(pref, newValue);
		else if ( key.equals(Servers.USERNAME) ) ret = validateUsername(pref, newValue);
		else if ( key.equals(Servers.PASSWORD) ) ret = validatePassword(pref, newValue);
		else if ( key.equals(Servers.ACTIVE) ) ret = validateActive(pref, newValue);
		if (!this.originalServerData.equals(this.serverData)) apply.setEnabled(true);
		updateSummaries();
		return ret;
	}
	
	/**
	 * <p>
	 * Responsible for handling the menu button push.
	 * </p>
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.advanced_settings_menu, menu);
		return true;
	}

	/**
	 * <p>
	 * Called when the user selects an option from the options menu. Determines
	 * what (if any) Activity should start.
	 * </p>
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch ( item.getItemId() ) {
			case R.id.advancedMenuItem:
				if ( this.iface == INTERFACE_SIMPLE ) {
					this.iface = INTERFACE_ADVANCED;
					updateSummaries();
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/************************************************************************
	 * 							   VALIDATION METHODS                       *
	 * **********************************************************************
	 * 
	 * <p>
	 * Below is a set of all the validation methods for responding to user input.
	 * They all operate the same - Check what the user entered, if it is valid update
	 * serverData and return true, otherwise return false.
	 * </p>
	 * 
	 * @param p The Preference that was changed
	 * @param v The new value that was entered
	 * @return True if the new value was accepted and serverData was updated.
	 */
	
	private boolean validateFriendlyName(Preference p, Object v) {
		if (v != null && v instanceof String && !v.equals("")) {
			this.serverData.put(Servers.FRIENDLY_NAME, (String)v);
			return true;
		} 
		else {
			return false;
		}
	}

	private boolean validateUseSSL(Preference p, Object v) {
		CheckBoxPreference cbp = (CheckBoxPreference)p;
		boolean value = (Boolean)v;
		int toPut = (value? 1 : 0);
		serverData.put(Servers.USE_SSL, toPut);
		int curPort = 0;
		if ( !serverData.getAsString(Servers.PORT).equals("") ) {
			curPort = serverData.getAsInteger(Servers.PORT);
		}
		
		//Auto update if using default ports
		if (value) {
			if (!cbp.isChecked() && curPort == 80) {
				port.setText("443");
				serverData.put(Servers.PORT,443);
			}
		}
		else {
			if (cbp.isChecked() && curPort == 443) {
				port.setText("80");
				serverData.put(Servers.PORT,80);
			}
		}
		
		return true;
	}

	private boolean validateHostName(Preference p, Object v) { 
		if (v != null && v instanceof String && !v.equals("")) {
			serverData.put(Servers.HOSTNAME, (String)v);
		}
		return false;
	}

	private boolean validateUrl(Preference p, Object v) { 
		if (v != null && v instanceof String && !v.equals("")) {
			serverData.put(Servers.SUPPLIED_USER_URL, (String)v);
		}
		return false;
	}
	
	private boolean validatePort(Preference p, Object v) {
		String value = (String) v;
		if (value.equals("")) {
			//Blank is allowed
			serverData.put(Servers.PORT, "");
			return true;
		}
		else {
			int port;
			try {
				port = Integer.parseInt(value);
			} catch (Exception e) {
				//Cant parse port!
				return false;
			}
			if (port < 0) return false;
			serverData.put(Servers.PORT, port);
			return true;
		}
	}
	
	private boolean validatePrincipalPath(Preference p, Object v) {
		if (v == null || v.equals("")) {
			v = "/";
		}
		if ( ! ((String)v).substring(0,1).equals("/") ) {
			v = "/".concat((String)v); 
		}
		serverData.put(Servers.PRINCIPAL_PATH, (String)v);
		return true;
	}

	private boolean validateAuth(Preference p, Object v) {
		serverData.put(Servers.AUTH_TYPE, Integer.parseInt((String)v));
		return true;
	}
	private boolean validateUsername(Preference p, Object v) { 
		this.serverData.put(Servers.USERNAME, (String)v);
		return true;
	}
	private boolean validatePassword(Preference p, Object v) { 
		this.serverData.put(Servers.PASSWORD, (String)v);
		return true;
	}
	private boolean validateActive(Preference p, Object v) { 
		int toPut = ((Boolean)v? 1 : 0);
		serverData.put(Servers.ACTIVE, toPut);
		return true;
	}

	@Override
	public boolean isAdvancedInterface() {
		return iface == INTERFACE_ADVANCED;
	}

	@Override
	public ConnectivityManager getConnectivityService() {
		return (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE) ;
	}
}
