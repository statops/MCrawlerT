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

import java.util.List;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.providers.PathSets;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.connector.AcalRequestor;
import com.morphoss.acal.xml.DavNode;

public class HomeSetDiscovery extends ServiceJob {

	private static final String TAG = "aCal HomeSetDiscovery";
	private int serverId;
	private aCalService context;
	private ContentResolver cr;
	private AcalRequestor requestor;

	final private static Header[] pHomeHeaders = new Header[] {
			new BasicHeader("Depth","0"),
			new BasicHeader("Content-Type","text/xml; encoding=UTF-8")
	};

	final private static String pHomeData = 
"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
"<propfind xmlns=\""+Constants.NS_DAV+"\""+
"    xmlns:C=\""+Constants.NS_CALDAV+"\""+
"    xmlns:CARD=\""+Constants.NS_CARDDAV+"\">"+
" <prop>\n"+
"  <C:calendar-home-set/>\n"+
"  <CARD:addressbook-home-set/>\n"+
" </prop>\n"+
"</propfind>";

	
	public HomeSetDiscovery(int serverId) {
		this.serverId = serverId;
	}
	
	@Override
	public void run(aCalService context) {
		this.context = context;
		this.cr = context.getContentResolver();
		discoverHomeSetPaths();
	}
	
	public void discoverHomeSetPaths() {
		if (Constants.LOG_DEBUG) Log.d(TAG,"Beginning home set discovery on server "+this.serverId);
		DavNode root = null;
		try {
			ContentValues serverData = Servers.getRow(serverId, cr);
			requestor = AcalRequestor.fromServerValues(serverData);
			root = requestor.doXmlRequest("PROPFIND", null, pHomeHeaders, pHomeData);
		}
		catch (Exception e) {
			Log.e(TAG,"Error getting home set data for "+this.serverId);
			Log.d(TAG,Log.getStackTraceString(e));
		}
		if (root == null) {
			Log.w(TAG, "No home-set data from server for.");
			return;
		}


		try {
			//We are going to remove all data in pathData for this server and then re add all the results of this query
			//This must be done as a single transaction
			if (cr.update(Uri.withAppendedPath(PathSets.CONTENT_URI,"begin"), new ContentValues(), "", new String[0]) != 1) return;
			if (Constants.LOG_VERBOSE) Log.v(TAG,"Path sets DB Transaction started.");

			//Wipe existing data for this server'
			Uri path_sets_servers = Uri.parse(PathSets.CONTENT_URI.toString()+"/servers/"+this.serverId);
			cr.delete(path_sets_servers, null, null);


			//Search for calendar and address books in response
			List<DavNode> responseList = root.getNodesFromPath("multistatus/response");
			for (DavNode response : responseList) {
				List<DavNode> calendars = response.getNodesFromPath("propstat/prop/calendar-home-set/href");
				List<DavNode> addressbooks = response.getNodesFromPath("propstat/prop/addressbook-home-set/href");
				ContentValues[] inserts = new ContentValues[calendars.size() + addressbooks.size()];
				int i;
				for (i = 0; i < calendars.size(); i++) {
					DavNode curCalendar = calendars.get(i);
					ContentValues cv = new ContentValues();
					cv.put(PathSets.SERVER_ID, this.serverId);
					cv.put(PathSets.SET_TYPE, PathSets.CALENDAR_HOME_SET);
					cv.put(PathSets.PATH, curCalendar.getText());
					inserts[i] = cv;
				}
				
				for (int j = 0; j < addressbooks.size(); j++) {
					DavNode curAddressBook = addressbooks.get(j);
					ContentValues cv = new ContentValues();
					cv.put(PathSets.SERVER_ID, this.serverId);
					cv.put(PathSets.SET_TYPE, PathSets.ADDRESSBOOK_HOME_SET);
					cv.put(PathSets.PATH, curAddressBook.getText());
					inserts[i+j] = cv;
				}
				
				cr.bulkInsert(PathSets.CONTENT_URI, inserts);

				//Remove subtree to free up memory
				root.removeSubTree(response);
			}
			//We can now approve the transaction

			if (cr.update(Uri.withAppendedPath(PathSets.CONTENT_URI,"approve"), new ContentValues(), "", new String[0]) == 1) {
				if (Constants.LOG_VERBOSE) Log.v(TAG,"Path Sets DB Transaction Approved");
			} else {
				Log.w(TAG, "PathSets DB Transaction could not be approved. Data will not be saved.");
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Error writing home sets to DB: " + e);
		}
		finally {
			if (cr.update(Uri.withAppendedPath(PathSets.CONTENT_URI, "commit"), new ContentValues(), "", new String[0]) == 1) {
				if (Constants.LOG_VERBOSE) Log.v(TAG, "Path Sets Transaction closed.");
			}
			else {
				Log.w(TAG, "PathSets DB Transaction could not be closed. Data will not be saved.");
			}
		}
		if (Constants.LOG_VERBOSE) Log.v(TAG,"Home set discovery complete for server "+this.serverId);


		if ( Constants.LOG_DEBUG ) Log.d(TAG, "Scheduling HomeSetsUpdate on successful server config.");
		HomeSetsUpdate job = new HomeSetsUpdate(serverId);
		job.TIME_TO_EXECUTE = System.currentTimeMillis();
		context.addWorkerJob(job);
	}

	@Override
	public String getDescription() {
		return "Discovering home sets for server " + serverId;
	}

	
}
