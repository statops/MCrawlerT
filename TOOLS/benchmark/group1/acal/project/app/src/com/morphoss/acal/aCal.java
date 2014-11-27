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

package com.morphoss.acal;

import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.morphoss.acal.activity.AcalActivity;
import com.morphoss.acal.activity.MonthView;
import com.morphoss.acal.activity.ShowUpgradeChanges;
import com.morphoss.acal.activity.serverconfig.NewServerConfiguration;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.service.ServiceRequest;
import com.morphoss.acal.service.aCalService;
import com.morphoss.acal.weekview.WeekViewActivity;

public class aCal extends AcalActivity {
	
	final public static String TAG = "aCal"; 

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// make sure aCalService is running
		Intent serviceIntent = new Intent(this, aCalService.class);
		serviceIntent.putExtra("UISTARTED", System.currentTimeMillis());
		this.startService(serviceIntent);
		
		// Set all default preferences to reasonable values
		PreferenceManager.setDefaultValues(this, R.xml.main_preferences, false);

		int lastRevision = prefs.getInt(PrefNames.lastRevision, 0);

		try {
			int thisRevision = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			if ( lastRevision < thisRevision ) {
				if ( lastRevision == 0 ) {
					// Default our 24hr pref to the system one.
					prefs.edit().putBoolean(getString(R.string.prefTwelveTwentyfour), DateFormat.is24HourFormat(this)).commit();
				}
				startActivity(new Intent(this, ShowUpgradeChanges.class));
			}
			else { 
				startPreferredView(prefs, this, true);
			}
		}
		catch (NameNotFoundException e) { }
		this.finish();
	}

	
	@Override
	protected void onResume() {
		super.onResume();
		ServiceManager serviceManager = new ServiceManager(this);
		ServiceRequest sr = serviceManager.getServiceRequest();
		for( Entry<Long, Collection> c : Collection.getAllCollections(this).entrySet() ) {
			try {
				sr.syncCollectionNow(c.getKey());
			}
			catch ( RemoteException e ) {
				Log.w(TAG,Log.getStackTraceString(e));
			}
		}
	}

	
	public static void startPreferredView( SharedPreferences sPrefs, Activity c, boolean mayStartServerConfig ) {
		Bundle bundle = new Bundle();
		Intent startIntent = null;
		
		if ( mayStartServerConfig && prefs.getInt(PrefNames.serverIsConfigured, 0) == 0 ) {
			startIntent = new Intent(c, NewServerConfiguration.class);
		}
		else if ( sPrefs.getBoolean(c.getString(R.string.prefDefaultView), false) ) {
			startIntent = new Intent(c, WeekViewActivity.class);
		}
		else {
			startIntent = new Intent(c, MonthView.class);
		}
		startIntent.putExtras(bundle);
		c.startActivity(startIntent);
	}
}
