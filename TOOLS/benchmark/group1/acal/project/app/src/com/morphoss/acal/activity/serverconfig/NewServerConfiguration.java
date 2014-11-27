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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.R;
import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.aCal;
import com.morphoss.acal.activity.AcalActivity;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.ServiceRequest;

/**
 * <p>This activity allows the user to configure a new server.</p>
 */
public class NewServerConfiguration extends AcalActivity implements OnClickListener, ServerConfigurator  {
	
	//Tag for log messages
	public static final String TAG = "NewServerConfiguration";
	
	//The data associated with the server we are configuring.
	private ContentValues serverData;

	//The Key to be used in serverData for storing the image resourceId
	public static final String KEY_IMAGE = "IMAGE_RESOURCE";

	//The widgets
	private EditText etConfigName;
	private EditText etUserName;
	private EditText etPassword;
	private EditText etServerUrl;
	private Button btnConfigure;
	private Button btnSkip;
	
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

		// Don't harass people who are upgrading
		if ( checkIfServersAreConfigured() ) finish();

		setContentView(R.layout.new_server_config);

		serverData = new ContentValues();
		createDefaultValues();

		populateLayout();
	}


	/**
	 * Temporary function to keep in for a few versions for migrating people
	 * @return
	 */
	private boolean checkIfServersAreConfigured() {
		Cursor result = null;
		try {
			result = getContentResolver().query(Servers.CONTENT_URI, null, null, null, null);
			if ( result.getCount() == 0 ) return false;
		}
		catch( Exception e ) {
			Log.e(TAG, "Could not query servers!", e);
		}
		finally {
			if ( result != null && !result.isClosed() ) result.close();
		}
		prefs.edit().putInt(PrefNames.serverIsConfigured, 1).commit();
		return true;
	}


	private void populateLayout() {
		btnConfigure = (Button) findViewById(R.id.configure_button);
		btnConfigure.setOnClickListener(this);
		AcalTheme.setContainerFromTheme(btnConfigure, AcalTheme.BUTTON);

		btnSkip = (Button) findViewById(R.id.skip_button);
		btnSkip.setOnClickListener(this);
		AcalTheme.setContainerFromTheme(btnSkip, AcalTheme.BUTTON);

		etConfigName = (EditText) findViewById(R.id.configName);
		etUserName = (EditText) findViewById(R.id.userName);
		etPassword = (EditText) findViewById(R.id.password);
		etServerUrl = (EditText) findViewById(R.id.serverUrl);
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
	
	
	private void createDefaultValues() {
		serverData.put(Servers.FRIENDLY_NAME,"");
		serverData.put(Servers.SUPPLIED_USER_URL,"");
		serverData.put(Servers.USERNAME,"");
		serverData.put(Servers.PASSWORD,"");
		serverData.put(Servers.ACTIVE, 1);
	}
	
	public void onClick(View v) {
		if (v.getId() == btnConfigure.getId()) {
			if ( validateAndAssign() ) checkServer();
		}
		else if (v.getId() == btnSkip.getId()) {
			finish();
		}
	}

	/**
	 * Attempts to connect to server and get configuration. 
	 */
	private void checkServer() {
		Context cx = this.getBaseContext();
		CheckServerDialog csd = new CheckServerDialog(this, serverData, cx, this.serviceManager);
		csd.start();
	}

	
	public void saveData() {
		if (this.serverData.get(Servers.FRIENDLY_NAME).equals(""))
				this.serverData.put(Servers.FRIENDLY_NAME, serverData.getAsString(Servers.SUPPLIED_USER_URL));

		createRecord();

		// Start syncing server in background
		if (Constants.LOG_VERBOSE) Log.v(TAG, "Scheduling HomeSetDiscovery on new server config.");
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

	
	public void finishAndClose() {
		this.setResult(RESULT_OK);
		this.finish();
	}

	
	/**
	 * <p>Creates a new server record on success.</p>
	 */
	private void createRecord() {
		try {
			Uri result = getContentResolver().insert(Servers.CONTENT_URI, Servers.cloneValidColumns(serverData));
			int id = Integer.parseInt(result.getPathSegments().get(0));
			if (id < 0) throw new Exception("Failed to add server");
			serverData.put(Servers._ID, id);
			prefs.edit().putInt(PrefNames.serverIsConfigured, 1).commit();
			
		} catch (Exception e) {
			//error updating
			Toast.makeText(this, getString(R.string.errorSavingServerConfig), Toast.LENGTH_LONG).show();
		}
	}
	

	/**
	 * Validate the fields and assign them to appropriate ServerData fields if they pass.
	 * @return true if everything passed.
	 */
	private boolean validateAndAssign( ) {
		boolean passed = true;
		EditText fieldToFix = null;
		StringBuilder errorToast = new StringBuilder();
		if ( etConfigName.getText().toString().equals("") ) {
			errorToast.append(getString(R.string.errorConfigMustHaveName));
			passed = false;
			fieldToFix = etConfigName;
		}
		else {
			this.serverData.put(Servers.FRIENDLY_NAME, etConfigName.getText().toString());
		}

		if ( etUserName.getText().toString().equals("") ) {
			if ( !passed ) errorToast.append("\n");
			else fieldToFix = etUserName;
			errorToast.append(getString(R.string.errorConfigMustHaveUserName));
			passed = false;
		}
		else {
			this.serverData.put(Servers.USERNAME, etUserName.getText().toString());
		}

		if ( etPassword.getText().toString().equals("") ) {
			if ( !passed ) errorToast.append("\n");
			else fieldToFix = etPassword;
			errorToast.append(getString(R.string.errorConfigMustHavePassword));
			passed = false;
		}
		else {
			this.serverData.put(Servers.PASSWORD, etPassword.getText().toString());
		}
		
		if ( etServerUrl.getText().toString().equals("") ) {
			if ( !passed ) errorToast.append("\n");
			else fieldToFix = etServerUrl;
			errorToast.append(getString(R.string.errorConfigMustHaveURL));
			passed = false;
		}
		else {
			this.serverData.put(Servers.SUPPLIED_USER_URL, etServerUrl.getText().toString());
		}
		if ( !passed ) {
			fieldToFix.requestFocus();
		}
		
		return passed;
	}

	@Override
	public boolean isAdvancedInterface() {
		return false;
	}

	@Override
	public ConnectivityManager getConnectivityService() {
		return (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE) ;
	}

	public void finish() {
		aCal.startPreferredView(prefs,this,false);
		super.finish();
	}

}
