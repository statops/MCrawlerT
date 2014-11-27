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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.R;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.providers.Timezones;
import com.morphoss.acal.service.connector.AcalConnectionPool;
import com.morphoss.acal.service.connector.AcalRequestor;
import com.morphoss.acal.xml.DavNode;

public class UpdateTimezones extends ServiceJob {
	private static final String TAG = "aCal UpdateTimezones";
	private aCalService context;
	private ContentResolver cr;
	private AcalRequestor requestor;
	private String tzServerBaseUrl;

	/**
	 * Constructor
	 * @param serverId
	 * @param Context
	 */
	public UpdateTimezones( long timeToExecute ) {
		TIME_TO_EXECUTE = timeToExecute;
	}
	
	/**
	 * Loop through all active collections and 
	 */
	public void run(aCalService context) {
		this.context = context;
		this.cr = context.getContentResolver();
		tzServerBaseUrl = context.getPreferenceString(PrefNames.tzServerBaseUrl, context.getString(R.string.davicalOrgTzUrl) );
		this.requestor = new AcalRequestor();

		if (Constants.LOG_DEBUG) Log.d(TAG, "Refreshing Timezone data from "+tzServerBaseUrl);
		refreshTimezoneData();
		if (Constants.LOG_DEBUG) Log.d(TAG, "Timezone refresh complete.");
		scheduleNextUpdate();
	}

	private String tzUrl( String action, String tzid ) {
		StringBuilder url = new StringBuilder(tzServerBaseUrl);
		url.append("?action=").append(action).append("&lang=")
			.append(Locale.getDefault().getLanguage()).append('_').append(Locale.getDefault().getCountry());
		if ( tzid != null ) {
			url.append("&tzid=").append(StaticHelpers.urlescape(tzid, false));
		}
		return url.toString();
	}

	private void refreshTimezoneData() {
		try {
			HashMap<String,Long> currentZones = new HashMap<String,Long>();
			HashMap<String,Long> updatedZones = new HashMap<String,Long>();
			HashMap<String,Long> insertedZones = new HashMap<String,Long>();
			Cursor allZones = cr.query(Timezones.CONTENT_URI, new String[] { Timezones.TZID, Timezones.LAST_MODIFIED }, null, null, null);
			Long maxModified = 0L;
			for( allZones.moveToFirst(); !allZones.isAfterLast(); allZones.moveToNext()) {
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV, TAG, "Found existing zone of '"+allZones.getString(0)+"' modified: "+AcalDateTime.fromMillis(allZones.getLong(1)*1000L).toString());
				currentZones.put(allZones.getString(0), allZones.getLong(1));
				if ( allZones.getLong(1) > maxModified ) maxModified = allZones.getLong(1); 
			}
			AcalDateTime mostRecentChange = AcalDateTime.getUTCInstance().setEpoch(maxModified);
			Log.println(Constants.LOGI, TAG, "Found "+allZones.getCount()+" existing timezones, most recent change on "+mostRecentChange.toString());
			if ( allZones.getCount() > 350 && mostRecentChange.after(AcalDateTime.getUTCInstance().addDays(-30)) ) {
				Log.println(Constants.LOGI, TAG, "Skipping update - our database is pretty recent" );
				return;
			}

			requestor.interpretUriString(tzUrl("list",null));
			DavNode root = requestor.doXmlRequest("GET", null, null, null);
			if ( requestor.getStatusCode() != 200 ) {
				Log.println(Constants.LOGI, TAG, "Bad response from Timezone Server at " + tzUrl("list",null));
				return;
			}
			if ( root == null ) {
				Log.println(Constants.LOGI, TAG, "No XML from GET " + tzUrl("list",null));
				return;
			}

			
			String tzid;
			String tzData;
			long lastModified;
			StringBuilder localNames;
			StringBuilder aliases;
			ContentValues zoneValues = new ContentValues();
			
			for( DavNode zoneNode : root.getNodesFromPath("timezone-list/summary") ) {
				
				tzid = zoneNode.getFirstNodeText("tzid");
				if ( updatedZones.containsKey(tzid) || insertedZones.containsKey(tzid) ) continue;

				lastModified = AcalDateTime.fromString(zoneNode.getFirstNodeText("last-modified")).getEpoch();
				if ( currentZones.containsKey(tzid) && currentZones.get(tzid) <= lastModified ) {
					currentZones.remove(tzid);
					continue;
				}

				tzData = getTimeZone(tzid);
				if ( tzData == null ) continue;

				List<DavNode> localisedNameNodes = zoneNode.getNodesFromPath("local-name");
				localNames = new StringBuilder();
				for( DavNode localNameNode : localisedNameNodes ) {
					if ( localNames.length() > 0 ) localNames.append("\n");
					localNames.append(localNameNode.getAttribute("lang"))
							.append('~')
							.append(localNameNode.getText());
				}

				List<DavNode> aliasNodes = zoneNode.getNodesFromPath("alias");
				aliases = new StringBuilder();
				for( DavNode aliasNode : aliasNodes ) {
					if ( aliases.length() > 0 ) aliases.append("\n");
					aliases.append(aliasNode.getText());
				}

				zoneValues.put(Timezones.TZID, tzid);
				zoneValues.put(Timezones.ZONE_DATA, tzData);
				zoneValues.put(Timezones.LAST_MODIFIED, lastModified);
				zoneValues.put(Timezones.TZ_NAMES, localNames.toString());
				zoneValues.put(Timezones.TZID_ALIASES, aliases.toString());

				Uri tzUri = Uri.withAppendedPath(Timezones.CONTENT_URI, "tzid/"+StaticHelpers.urlescape(tzid,false));

				if ( currentZones.containsKey(tzid) ) {
					if ( cr.update(tzUri, zoneValues, null, null) != 1 ) {
						Log.e(TAG,"Failed update for TZID '"+tzid+"'");
					}
					updatedZones.put(tzid, currentZones.get(tzid));
					currentZones.remove(tzid);
				}
				else {
					if ( cr.insert(tzUri, zoneValues) == null )
						Log.e(TAG,"Failed insert for TZID '"+tzid+"'");
					insertedZones.put(tzid, currentZones.get(tzid));
				}

				// Let other stuff have a chance 
				Thread.sleep(50);
			}
			
			if ( currentZones.size() > 0 ) {
				StringBuilder s = new StringBuilder();
				for( String tz : currentZones.keySet() ) {
					if ( s.length() > 0 ) s.append(',');
					s.append("'").append(tz).append("'");
				}
				cr.delete(Timezones.CONTENT_URI, Timezones.TZID+" IN ("+s+")", null);
			}

			Log.println(Constants.LOGI, TAG, "Updated data for "+updatedZones.size()+" zones, added data for "+insertedZones.size()+" new zones." );
		}
		catch( Exception e ) {
			Log.e(TAG,Log.getStackTraceString(e));
		}
	}

	
	private String getTimeZone(String tzid) {
		requestor.interpretUriString(tzUrl("get",tzid));
		InputStream is = null;
		StringBuilder getResponse = new StringBuilder();
		try {
			is = requestor.doRequest("GET", null, null, null);
			if ( requestor.getStatusCode() != 200 ) {
				Log.println(Constants.LOGI, TAG, "Bad response from Timezone Server at " + tzUrl("get",tzid));
				return null;
			}
			BufferedReader r = new BufferedReader(new InputStreamReader(is),AcalConnectionPool.DEFAULT_BUFFER_SIZE);
			String line;
			while ( (line = r.readLine()) != null ) {
			    getResponse.append(line).append("\n");
			}
		}
		catch ( IllegalStateException e ) {
			Log.w(TAG,"Auto-generated catch block", e);
		}
		catch ( IOException e ) {
			Log.w(TAG,"Auto-generated catch block", e);
		}
		finally {
			try { is.close(); } catch( Exception e ) {};
		}
		try {
			VComponent vc = VComponent.createComponentFromBlob(getResponse.toString());
			List<VComponent> children = vc.getChildren();
			return children.get(0).getCurrentBlob();
		}
		catch( Exception e ) {
			Log.w(TAG,"Auto-generated catch block", e);
			return null;
		}
	}

	private void scheduleNextUpdate() {
		this.TIME_TO_EXECUTE = System.currentTimeMillis() + (86400000 * 7);
//		this.TIME_TO_EXECUTE = System.currentTimeMillis() + 90000;
		Log.println(Constants.LOGV,TAG,
					"Scheduling next instance at " + AcalDateTime.fromMillis(this.TIME_TO_EXECUTE).fmtIcal() );
		context.addWorkerJob(this);
	}

	
	@Override
	public String getDescription() {
		return "Refreshing timezones";
	}
}
