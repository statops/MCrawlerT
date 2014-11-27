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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VTimezone;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.PathSets;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.connector.AcalRequestor;
import com.morphoss.acal.xml.DavNode;

public class HomeSetsUpdate extends ServiceJob {
	private static final String TAG = "aCal HomeSetsUpdate";
	private int serverId;
	private aCalService context;
	private ContentResolver cr;
	private AcalRequestor requestor;
	private ContentValues	serverData;

	private final static Header[] pCalendarHeaders = new Header[] {
		new BasicHeader("Depth","1"),
		new BasicHeader("Content-Type","text/xml; encoding=UTF-8")
	};

	private final static String pCalendarRequest =
"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
"<propfind xmlns=\""+Constants.NS_DAV+"\""+
"    xmlns:C=\""+Constants.NS_CALDAV+"\""+
"    xmlns:ACAL=\""+Constants.NS_ACAL+"\""+
"    xmlns:ICAL=\""+Constants.NS_ICAL+"\""+
"    xmlns:CS=\""+Constants.NS_CALENDARSERVER+"\">\n"+
" <prop>\n"+
"  <displayname/>\n"+
"  <resourcetype/>\n"+
"  <supported-report-set/>\n"+
"  <supported-method-set/>\n"+
"  <current-user-privilege-set/>\n"+
"  <sync-token/>\n"+
"  <CS:getctag/>\n"+
"  <C:supported-calendar-component-set/>\n"+
"  <C:calendar-timezone/>\n"+
"  <ACAL:collection-colour/>\n"+
"  <ICAL:calendar-color/>\n"+
" </prop>\n"+
"</propfind>";				                  


	/**
	 * Constructor
	 * @param serverId
	 * @param Context
	 */
	public HomeSetsUpdate(int serverId) {
		this.serverId = serverId;
	}
	
	/**
	 * Loop through all active collections and 
	 */
	public void run(aCalService context) {
		this.context = context;
		this.cr = context.getContentResolver();
		this.serverData = Servers.getRow(serverId, cr);
		this.requestor = AcalRequestor.fromServerValues(serverData);

		if (Constants.LOG_DEBUG) Log.d(TAG, "Refreshing DavCollections for server "+this.serverId);
		String homeSetPaths[] = fetchHomeSets();

		//No paths to process
		if (homeSetPaths == null || homeSetPaths.length < 1) return;

		for (String homePath : homeSetPaths) {
			updateCollectionsWithin(homePath);
		}
		if (Constants.LOG_DEBUG) Log.d(TAG,"DavCollections refresh on server "+this.serverId+" complete.");
	}

	/**
	 * Fetch the home sets we should be looking at.  We don't want
	 * to get duplicate paths here, as that would be a bit pointless!
	 * @return an array of String
	 */
	private String[] fetchHomeSets() {
		Cursor mCursor = null;
		String paths[] = null;

		if (Constants.LOG_VERBOSE) Log.v(TAG, "Retrieving home sets" );
		try {
			// Get Calendar sets
			Uri calendarSetsUri = Uri.parse(PathSets.CONTENT_URI.toString()+"/servers/"+this.serverId);
			mCursor = cr.query(calendarSetsUri, null, null, null, null);
			paths = new String[mCursor.getCount()];
			int count = 0;
			for( mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
				paths[count++] = mCursor.getString(mCursor.getColumnIndex(PathSets.PATH));
				if (Constants.LOG_VERBOSE) Log.v(TAG, "Retrieved home set path: " + PathSets.PATH );
			}
		}
		catch (Exception e) {
			Log.e(TAG,"Unknown error retrieving acquiring home sets: "+e.getMessage());
			Log.e(TAG,Log.getStackTraceString(e));
		}
		finally {
			if (mCursor != null) mCursor.close();
		}
		if (Constants.LOG_VERBOSE) Log.v(TAG, "Retrieved " + (paths == null ? 0 :paths.length) + " home-set paths.");
		return paths;
	}


	/**
	 * Update all of the collections we can find within a homeSet
	 * @param homeSet
	 */
	private void updateCollectionsWithin( String homeSet ) {

		if (Constants.LOG_DEBUG) Log.d(TAG,"Updating collections within "+homeSet);

		Map<String,ContentValues> deleteList = new HashMap<String,ContentValues>();
		String collectionPath = null;
		Cursor cursor = cr.query(DavCollections.CONTENT_URI, null,
					DavCollections.SERVER_ID+"="+serverId +" AND "+ DavCollections.COLLECTION_PATH+" LIKE ?",
					new String[] { homeSet + "%"}, null);
		for( cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext() ) {
			ContentValues cv = new ContentValues();
			DatabaseUtils.cursorRowToContentValues(cursor, cv);
			collectionPath = cv.getAsString(DavCollections.COLLECTION_PATH);
			deleteList.put(collectionPath, cv);
		}
		cursor.close();

		try {
			requestor.applyFromServer(serverData,false);
			DavNode root = requestor.doXmlRequest("PROPFIND", homeSet, pCalendarHeaders, pCalendarRequest);
			if (requestor.getStatusCode() == 404) {
				Log.i(TAG, "PROPFIND got 404 on " + homeSet + " so a HomeSetDiscovery is being scheduled.");
				ServiceJob sj = new HomeSetDiscovery(serverId);
				context.addWorkerJob(sj);
				return;
			}

			List<DavNode> responseList = root.getNodesFromPath("multistatus/response");
			for (DavNode response : responseList) {
				List<DavNode> propstats = response.getNodesFromPath("propstat");
				for ( DavNode propstat : propstats ) {
					if ( !propstat.getFirstNodeText("status").equalsIgnoreCase("HTTP/1.1 200 OK") ) continue;
	
					//Get current collection path
					collectionPath = response.getFirstNodeText("href");
					if ( collectionPath != null ) {
						requestor.interpretUriString(collectionPath);
						collectionPath = requestor.fullUrl();
						if ( collectionPath.equals(homeSet) ) {
							// Update CTAG and NEEDS_SYNC if this is the home-set URL
							String ctag = propstat.getFirstNodeText("prop/getctag");
							if ( ctag != null ) {
								ContentValues cv = new ContentValues();
								cv.put(PathSets.COLLECTION_TAG,ctag);
								cv.put(PathSets.NEEDS_SYNC,false);
								cv.put(PathSets.LAST_CHECKED,new AcalDateTime().fmtIcal());
								cr.update(PathSets.CONTENT_URI, cv,
											PathSets.SERVER_ID+"="+serverId+" AND "+PathSets.PATH+"=?",
											new String[] {homeSet});
							}
						}
						else {
							if ( updateCollectionFromPropstat( collectionPath, propstat ) ) {
								deleteList.remove(collectionPath);
							}
						}
					}
				}

				//Remove subtree to free up memory
				root.removeSubTree(response);
			}
			
			if ( !deleteList.isEmpty() ) {
				StringBuilder deleteIn = null;
				for( Entry<String,ContentValues> d : deleteList.entrySet() ) {
					if ( deleteIn == null ) {
						deleteIn = new StringBuilder(DavCollections.SERVER_ID);
						deleteIn.append("=");
						deleteIn.append(serverId);
						deleteIn.append(" AND ");
						deleteIn.append(DavCollections._ID);
						deleteIn.append(" IN (");
					}
					else {
						deleteIn.append(",");
					}
					deleteIn.append(d.getValue().getAsInteger(DavCollections._ID));

				}
				deleteIn.append(")");
				if ( Constants.LOG_DEBUG ) { Log.d(TAG,"Deleting collections from DB where:");
					Log.d(TAG,deleteIn.toString());
				}
				cr.delete(DavCollections.CONTENT_URI, deleteIn.toString(), null);
			}
	
		} catch (Exception ex2) {
			Log.e(TAG,"Unknown error when updating collections within home sets: "+ex2.getMessage());
			Log.e(TAG,Log.getStackTraceString(ex2));
		}
	}

	
	/**
	 * Does a database update (or insert) on the basis of this response
	 * subtree.
	 * @param response
	 * @return true / false if we did the update
	 */
	private boolean updateCollectionFromPropstat( String collectionPath, DavNode propstat ) {

		if (Constants.LOG_DEBUG) Log.d(TAG,"Updating collection from propstat for "+collectionPath);

		ContentValues cv = new ContentValues();
		cv.put(DavCollections.SERVER_ID, this.serverId);
		cv.put(DavCollections.COLLECTION_PATH, requestor.fullUrl());
		
		Log.w(TAG,"Found collection at "+requestor.fullUrl());

		boolean holds_addressbook=false;
		boolean holds_events=false;
		boolean holds_tasks=false;
		boolean holds_journal=false;
		String displayName = null;
		String collectionColour = null;
		boolean alarming = true;
		
		int serverHasSync = 0;
		int serverHasMultiget = 0;
		
		//is this a calendar or addressbook??
		List<DavNode> calendar = propstat.getNodesFromPath("prop/resourcetype/calendar");
		List<DavNode> addressBook = propstat.getNodesFromPath("prop/resourcetype/addressbook");
		if (addressBook.isEmpty() && calendar.isEmpty()) {
			return false;  // This is not a supported collection type
		}

		if ( !addressBook.isEmpty()) {
			serverHasMultiget = 1;
			holds_addressbook = true;  // There is an addressbook node			
		}
		else {
			List<DavNode> comps = propstat.getNodesFromPath("prop/supported-calendar-component-set/comp");
			for (DavNode comp : comps) {
				if (comp.hasAttribute("name")) {
					String curComp = comp.getAttribute("name");
					if (curComp.equalsIgnoreCase("VEVENT")){
						holds_events = true;
					}
					else if (curComp.equalsIgnoreCase("VTODO")) {
						holds_tasks = true;
					}
					else if (curComp.equalsIgnoreCase("VJOURNAL")) {
						holds_journal = true;
					}
				}
			}

			if ( !calendar.isEmpty() ) {
				serverHasMultiget = 1;
				if ( comps.isEmpty() ) {
					// It is a calendar, but they appear not to support the supported-calendar-component-set property
					holds_events = true;
					holds_tasks = true;
					holds_journal = true;
				}
			}

			//If we can't use this collection, no point saving it
			if (!holds_events && !holds_tasks && !holds_journal) return false;
		}
		cv.put(DavCollections.HOLDS_EVENTS, holds_events);
		cv.put(DavCollections.HOLDS_TASKS, holds_tasks);
		cv.put(DavCollections.HOLDS_JOURNAL, holds_journal);
		cv.put(DavCollections.HOLDS_ADDRESSBOOK, holds_addressbook);

		//Display Name
		displayName = propstat.getFirstNodeText("prop/displayname");

		// writeable
		cv.put(DavCollections.IS_WRITABLE, !propstat.getNodesFromPath("prop/current-user-privilege-set/privilege/write").isEmpty());

		// colour
		collectionColour = propstat.getFirstNodeText("prop/collection-colour");
		if ( collectionColour == null ) {
			collectionColour = propstat.getFirstNodeText("prop/calendar-color");
			if ( collectionColour != null && collectionColour.length() > 7 ) {
				// To make iCal RGBA fit Android ARGB spec we trim the alpha 
				collectionColour = collectionColour.substring(0, 7);
			}
		}
		
		// default timezone
		String tzid = null;	//Standard time offset in HH:mm format
		String tzVcalendar = propstat.getFirstNodeText("prop/calendar-timezone");
		try {
			VComponent vc = VComponent.createComponentFromBlob(tzVcalendar);
			tzid = ((VTimezone) vc.getChildren().get(0)).getTZID();
		} catch( Exception e) {};
		if (tzid != null) cv.put(DavCollections.DEFAULT_TIMEZONE, tzid);

		// Supported reports
		if ( !propstat.getNodesFromPath("prop/supported-report-set/supported-report/report/calendar-multiget").isEmpty()
					|| !propstat.getNodesFromPath("prop/supported-report-set/supported-report/report/addressbook-multiget").isEmpty() ) {
			serverHasMultiget = 1;
		}
		else if ( !propstat.getNodesFromPath("prop/supported-report-set/calendar-multiget").isEmpty()
						|| !propstat.getNodesFromPath("prop/supported-report-set/addressbook-multiget").isEmpty() ) {
			// at least Kerio has this bug, and probably others as well.
			serverHasMultiget = 1;
		}

		List<DavNode> syncReport = propstat.getNodesFromPath("prop/supported-report-set/supported-report/report/sync-collection"); 
		if ( !syncReport.isEmpty() ) {
			serverHasSync = 1;
		}
		else {
			syncReport = propstat.getNodesFromPath("prop/supported-report-set/sync-collection");
			if ( !syncReport.isEmpty() ) {
				// at least Kerio has this bug, and probably others as well.
				serverHasSync = 1;
			}
		}

		if (Constants.LOG_DEBUG) Log.d(TAG,"Updating collection from response for "+collectionPath);

		//begin DB Operations
		Cursor mCursor = null;
		ServiceJob job = null; 
		try {
			//First, check to see if a record exists
			// TODO Maybe we should know this before we start thinking about
			// overwriting stuff on it.
			Uri collectionUri = Uri.parse(DavCollections.CONTENT_URI.toString()+"/server/"+this.serverId);
			mCursor = cr.query(	collectionUri, null,
						DavCollections.SERVER_ID+"="+serverId+" AND "+DavCollections.COLLECTION_PATH+" = ? ",
						new String[]{collectionPath}, null);
			
			if (mCursor.getCount() == 0) {
				// First set some default values, since this is a new record.
				cv.put(DavCollections.ACTIVE_EVENTS, holds_events);
				cv.put(DavCollections.ACTIVE_TASKS, holds_tasks);
				cv.put(DavCollections.ACTIVE_JOURNAL, holds_journal);
				cv.put(DavCollections.ACTIVE_ADDRESSBOOK, holds_addressbook);
				
				cv.put(DavCollections.USE_ALARMS, alarming);
				cv.put(DavCollections.IS_VISIBLE, true);
				cv.put(DavCollections.MAX_SYNC_AGE_WIFI, Constants.DEFAULT_MAX_AGE_WIFI);
				cv.put(DavCollections.MAX_SYNC_AGE_3G, Constants.DEFAULT_MAX_AGE_3G);

				if ( displayName == null ) {
					//Default display name is the last 2 segments of the path
					String pathSegs[] = collectionPath.split("/");
					displayName = pathSegs[pathSegs.length-1];
					if (pathSegs.length >1) displayName = pathSegs[pathSegs.length-2]+" "+displayName;
				}
				cv.put(DavCollections.DISPLAYNAME, displayName);
				
				boolean sync_meta = false;
				if ( collectionColour == null ) {
					collectionColour = StaticHelpers.randomColorString();
					cv.put(DavCollections.SYNC_METADATA, 1);
					sync_meta = true;
				}
				cv.put(DavCollections.COLOUR, collectionColour);
				
				//Create new record
				mCursor.close();
				mCursor = cr.query( cr.insert(DavCollections.CONTENT_URI, cv), null, null, null, null);
				if ( mCursor.moveToFirst() )
					DatabaseUtils.cursorRowToContentValues(mCursor, cv);
				
				
				if ( sync_meta )
					WorkerClass.getExistingInstance().addJobAndWake(new SyncChangesToServer());

				if ( Constants.LOG_DEBUG ) Log.d(TAG, "Scheduling InitialCollectionSync on new collection.");
				job = new InitialCollectionSync( serverId, collectionPath);
			}
			else {
				// update record
				mCursor.moveToFirst();
				String collectionId = mCursor.getString(0);
				cr.update(DavCollections.CONTENT_URI, cv, DavCollections._ID + " = ?", new String[] { collectionId });

				cv.put(DavCollections._ID, collectionId);

				if ( Constants.LOG_DEBUG ) Log.d(TAG, "Scheduling SyncCollectionContents on existing collection.");
				job = new SyncCollectionContents(Integer.parseInt(collectionId));
			}
			context.addWorkerJob(job);
			
		}
		catch (Exception e) {
			Log.e(TAG, "There was an error writing to the collections DB: " + e.getMessage());
			Log.e(TAG,Log.getStackTraceString(e));
		}
		finally {
			if ( mCursor != null ) mCursor.close();
		}

		if ( serverHasSync == 1 || serverHasMultiget == 1 ) {
			/**
			 * If server has sync-collection report or *-multiget report we
			 * should save this in the server table.
			 */
			mCursor = null;
			try  {
				Uri serversUri = Uri.parse(Servers.CONTENT_URI.toString()+"/"+this.serverId);
				mCursor = cr.query(serversUri, new String[] { Servers._ID, Servers.HAS_MULTIGET, Servers.HAS_SYNC },
							null, null, null);

				if ( mCursor.moveToFirst() ) {
					if ( mCursor.getInt(mCursor.getColumnIndex(Servers.HAS_MULTIGET)) != serverHasMultiget
								|| mCursor.getInt(mCursor.getColumnIndex(Servers.HAS_SYNC)) != serverHasSync ) {
						
						cv = new ContentValues();
						cv.put(Servers._ID, serverId);
						cv.put(Servers.HAS_MULTIGET, serverHasMultiget);
						cv.put(Servers.HAS_SYNC, serverHasSync);
						cr.update(Servers.CONTENT_URI, cv, Servers._ID+"=?", new String[] { ""+serverId });
					}
				}
				
			}
			catch (Exception e) {
				Log.e(TAG, "There was an error writing to the dav_servers table: " + e.getMessage());
				Log.e(TAG,Log.getStackTraceString(e));
			}
			finally {
				if ( mCursor != null ) mCursor.close();
			}
		}
		
		return true;
	}

	

	@Override
	public String getDescription() {
		return "Refreshing collection lists for server "+serverId;
	}
}
