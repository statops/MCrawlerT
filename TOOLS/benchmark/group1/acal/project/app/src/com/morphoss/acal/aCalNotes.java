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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.morphoss.acal.activity.JournalListView;
import com.morphoss.acal.service.aCalService;

public class aCalNotes extends Activity {
	
	final public static String TAG = "aCalNotes"; 

	private SharedPreferences prefs;	

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make sure aCalService is running
		Intent serviceIntent = new Intent(this, aCalService.class);
		serviceIntent.putExtra("UISTARTED", System.currentTimeMillis());
		this.startService(serviceIntent);

		// Read our preference for which view should be the default
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Set all default preferences to reasonable values
		PreferenceManager.setDefaultValues(this, R.xml.main_preferences, false);

		int lastRevision = prefs.getInt(PrefNames.lastRevision, 0);
		if ( lastRevision == 0 ) {
			// Default our 24hr pref to the system one.
			prefs.edit().putBoolean(getString(R.string.prefTwelveTwentyfour), DateFormat.is24HourFormat(this)).commit();
		}

//		try {
//			int thisRevision = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
//			if ( lastRevision < thisRevision ) {
//				startActivity(new Intent(this, ShowUpgradeChanges.class));
//			}
//			else { 
				startPreferredView(prefs,this);
//			}
//		}
//		catch (NameNotFoundException e) { }
		this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	
	public static void startPreferredView( SharedPreferences sPrefs, Activity c ) {
		Bundle bundle = new Bundle();
		Intent startIntent = null;
//		if ( sPrefs.getBoolean(c.getString(R.string.prefTasksView), false) ) {
//			startIntent = new Intent(c, TodoListView.class);
//		}
//		else {
			startIntent = new Intent(c, JournalListView.class);
//		}
		startIntent.putExtras(bundle);
		c.startActivity(startIntent);
	}
}
