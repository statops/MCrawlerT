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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;

import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.morphoss.acal.AcalDebug;
import com.morphoss.acal.Constants;
import com.morphoss.acal.HashCodeUtil;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.DMDeleteQuery;
import com.morphoss.acal.database.DMQueryBuilder;
import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.DMUpdateQuery;
import com.morphoss.acal.database.DatabaseTableManager.QUERY_ACTION;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRBlockAndProcessQueryList;
import com.morphoss.acal.database.resourcesmanager.requests.RRGetResourceInCollection;
import com.morphoss.acal.database.resourcesmanager.requests.RRSyncQueryMap;
import com.morphoss.acal.database.resourcesmanager.requests.RRUpdateCollection;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.connector.AcalRequestor;
import com.morphoss.acal.service.connector.ConnectionFailedException;
import com.morphoss.acal.service.connector.SendRequestFailedException;
import com.morphoss.acal.xml.DavNode;

public class SyncCollectionContents extends ServiceJob {

	public static final String	TAG					= "aCal SyncCollectionContents";
	private long collectionId;
	
	private static final int	nPerMultiget		= 30;
	
	private long timeToWait = 0;
	private boolean scheduleNextInstance = false;

	private int					serverId			= -5;
	private String				collectionPath		= null;
	private String				syncToken			= null;
	private String				oldSyncToken		= null;
	private boolean				isAddressbook		= false;

	ContentValues				collectionData;
	private boolean				collectionChanged	= false;
	ContentValues				serverData;

	private String				dataType			= "calendar";
	private String				multigetReportTag	= "calendar-multiget";
	private String				nameSpace			= Constants.NS_CALDAV;

	// Note that this defines how often we wake up and see if we should be
	// doing a sync.  Not how often we actually wake up and hit the server
	// with a request.  Nevertheless we should not do this more than every
	// minute or so in production.
	private static final long	minBetweenSyncs		= (Constants.debugSyncCollectionContents || Constants.debugHeap ? 30000 : 300000);	// milliseconds

	private aCalService			context;
	private boolean	synchronisationForced			= false;
	private AcalRequestor 		requestor			= null;
	private boolean	syncWasCompleted;
	private int	errorCounter = 0;
	
	/**
	 * <p>
	 * Constructor
	 * </p>
	 * 
	 * @param collectionId2
	 *            <p>
	 *            The ID of the collection to be synced
	 *            </p>
	 * @param context
	 *            <p>
	 *            The context to use for all those things contexts are used for.
	 *            </p>
	 */
	public SyncCollectionContents(long collectionId) {
		this.collectionId = collectionId;
		this.TIME_TO_EXECUTE = 0;
	}


	/**
	 * <p>
	 * Schedule a sync of the contents of a collection, potentially forcing it to happen now even
	 * if this would otherwise be considered too early according to the normal schedule.
	 * </p>  
	 * @param collectionId
	 * @param forceSync
	 */
	public SyncCollectionContents(long collectionId, boolean forceSync ) {
		this.collectionId = collectionId;
		this.TIME_TO_EXECUTE = 0;
	}

	
	@Override
	public void run(aCalService context) {
		this.context = context;
		
		if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "SyncCollectionContents start");
		if ( collectionId < 0 || !getCollectionInfo()) {
			Log.w(TAG, "Could not read collection " + collectionId + " for server " + serverId
						+ " from collection table!");
			return;
		}

		if ( !(1 == serverData.getAsInteger(Servers.ACTIVE)) ) {
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, 
					"Server is no longer active - sync cancelled: " + serverData.getAsInteger(Servers.ACTIVE)
							+ " " + serverData.getAsString(Servers.FRIENDLY_NAME));
			return;
		}

		long start = System.currentTimeMillis();
		
		
		try {
			syncWasCompleted = true;

			// step 1 are there any 'needs_sync' in dav_resources?
			Map<String, ContentValues> originalData = 
				ResourceManager.getInstance(context).sendBlockingRequest(new RRSyncQueryMap(collectionId,true)).result();
				

			if ( originalData.size() < 1 && ! timeToRun() ) {
				calculateNextSchedulingTime();
				scheduleNextInstance();
				return;
			}

			if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
						"Starting sync on collection " + this.collectionPath + " (" + this.collectionId + ")");

			if (originalData.size() < 1) {
				if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents ) Log.println(Constants.LOGV,TAG,
						"No local resources marked as needing synchronisation.");
			}
			else 
				syncMarkedResources( originalData );
			
			if ( (StaticHelpers.toBoolean(serverData.getAsInteger(Servers.HAS_SYNC),false) && !this.synchronisationForced
										? doRegularSyncReport()
										: doRegularSyncPropfind() ) ) {
				originalData = 
					ResourceManager.getInstance(context).sendBlockingRequest(new RRSyncQueryMap(collectionId,true)).result();
				syncMarkedResources(originalData);
			}

			String lastSynchronized = AcalDateTime.getUTCInstance().fmtIcal();
			if ( syncWasCompleted ) {
				// update last checked flag for collection
				collectionData.put(DavCollections.LAST_SYNCHRONISED, lastSynchronized);
				collectionData.put(DavCollections.NEEDS_SYNC, 0);
				if ( syncToken != null ) {
					collectionData.put(DavCollections.SYNC_TOKEN, syncToken);
					if (Constants.LOG_DEBUG && Constants.debugSyncCollectionContents )
						Log.println(Constants.LOGD,TAG,"Updated collection record with new sync token '"+syncToken+"' at "+lastSynchronized);
				}
				
				ResourceManager.getInstance(context).sendBlockingRequest(
						new RRUpdateCollection(collectionId,collectionData));
				
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Error syncing collection " + this.collectionId + ": " + e.getMessage());
			Log.e(TAG, Log.getStackTraceString(e));
		}
	
		long finish = System.currentTimeMillis();
		if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
			Log.println(Constants.LOGV,TAG, "Collection sync finished in " + (finish - start) + "ms");

		calculateNextSchedulingTime();
		scheduleNextInstance();

		if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "SyncCollectionContents end");
	}
	
	/**
	 * <p>
	 * Checks for an old CalendarServer-style ctag for this collection
	 * </p>
	 * 
	 * @return <p>
	 *         Returns true if the CTag is different from the previous one, or if either is null.
	 *         </p>
	 */
	private boolean collectionTagChanged() {
		if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Requesting CTag on collection.");
		DavNode root = doCalendarRequest("PROPFIND", 0,
					"<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
					+ "<propfind xmlns=\"DAV:\" xmlns:CS=\"http://calendarserver.org/ns/\">"
						+ "<prop>"
	          				+ "<CS:getctag/>"
						+ "</prop>"
					+ "</propfind>"
				);
		
		if ( root == null ) {
			Log.i(TAG,"No response from server - deferring sync.");
			return false;
		}

		List<DavNode> responses = root.getNodesFromPath("multistatus/response");

		for (DavNode response : responses) {

			List<DavNode> propstats = response.getNodesFromPath("propstat");

			for (DavNode propstat : propstats) {
				if ( !propstat.getFirstNodeText("status").equalsIgnoreCase("HTTP/1.1 200 OK") )
					continue;

				DavNode prop = propstat.getNodesFromPath("prop").get(0);
				String ctag = prop.getFirstNodeText("getctag");
				String collectionTag = collectionData.getAsString(DavCollections.COLLECTION_TAG);
				
				if ( ctag == null || collectionTag == null ) return true;
				return ! ctag.equals(collectionTag);
			}
		}
		return true;
	}

	/**
	 * <p>
	 * Does a request against the collection path
	 * </p>
	 * 
	 * @return <p>
	 *         A DavNode which is the root of the multistatus response.
	 *         </p>
	 */
	private DavNode doCalendarRequest( String method, int depth, String xml) {
		DavNode root;
		try {
			root = requestor.doXmlRequest(method, collectionPath,
									SynchronisationJobs.getReportHeaders(depth), xml);
		}
		catch ( SSLHandshakeException e ) {
			Log.w(TAG,"Error validating certificate", e);
			return null;
		}
		if ( requestor.getStatusCode() == 404 ) {
			Log.w(TAG,"Sync PROPFIND got 404 on "+collectionPath+" so a HomeSetsUpdate is being scheduled.");
			ServiceJob sj = new HomeSetsUpdate(serverId);
			context.addWorkerJob(sj);
			return null;
		}
		return root;
	}

	
	/**
		 * <p>
		 * Do a sync run using a sync-collection REPORT against the collection, hopefully retrieving the -data
		 * pseudo-properties at the same time, but in any case getting a list of changed resources to process.
		 * Quick and light on the bandwidth, we hope.
		 * </p>
		 * 
		 * @return true if we still need to syncMarkedResources() afterwards.
		 */
		private boolean doRegularSyncReport() {
			if ( Constants.DISABLE_FEATURE_WEBDAV_SYNC ) {
				Log.w("aCal","Sync report is disabled. Falling back to PROPFIND.");
				return doRegularSyncPropfind();
			}

			DavNode root = doCalendarRequest("REPORT", 1,
						"<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
						+ "<sync-collection xmlns=\"DAV:\">"
							+ (oldSyncToken == null ? "<sync-token/>" : "<sync-token>" + oldSyncToken + "</sync-token>")
							+ "<sync-level>1</sync-level>"
							+ "<prop>"
								+ "<getetag/>"
	//							+ "<getlastmodified/>"
	//							+ "<" + dataType + "-data xmlns=\"" + nameSpace + "\"/>"
							+ "</prop>"
						+ "</sync-collection>"
					);
	
			boolean needSyncAfterwards = false; 
	
			if (root == null) {
				Log.i(TAG, "Unable to sync collection " + this.collectionPath + " (ID:" + this.collectionId
							+ " - no data from server.");
				syncWasCompleted = false;
				Log.i("aCal","Sync report did not work.  Attempting sync via PROPFIND.");
				syncToken = null;
				updateCollectionToken(syncToken);
				return doRegularSyncPropfind();
			}
	/**
	 * SOGO's sync-response looks like this (as of draft-1):
	 *
	<?xml version="1.0" encoding="utf-8"?>
	<D:multistatus xmlns:D="DAV:">
	 <D:sync-response>
	  <D:href>/SOGo/dav/sogo2/Calendar/personal/351dc1af-2aa3-4d14-9704-eadbcfecaf7e.ics</D:href>
	  <D:status>HTTP/1.1 200 OK</D:status>
	  <D:propstat>
	   <D:prop>
	   <D:getetag>&quot;gcs00000001&quot;</D:getetag></D:prop>
	   <D:status>HTTP/1.1 200 OK</D:status>
	  </D:propstat>
	 </D:sync-response>
	 <D:sync-token>1322100412</D:sync-token>
	</D:multistatus>
	 *
	 */
	/**
	 * Correct sync-response looks like this (as of draft-2 and later):
	 *
	<?xml version="1.0" encoding="utf-8" ?>
	<multistatus xmlns="DAV:">
	 <response>
	  <href>/caldav.php/user1/home/DAYPARTY-77C6-4FB7-BDD3-6882E2F1BE74.ics</href>
	  <propstat>
	   <prop>
	    <getetag>"165746adbab8bc0c8336a63cc5332ff2"</getetag>
	    <getlastmodified>Dow, 01 Jan 2000 00:00:00 GMT</getlastmodified>
	   </prop>
	   <status>HTTP/1.1 200 OK</status>
	  </propstat>
	 </response>
	 <sync-token>urn:,1322100412</sync-token>
	</multistatus>
	 * 
	 */
			
			//ArrayList<ResourceModification> changeList = new ArrayList<ResourceModification>(); 
			DMQueryList queryList = new DMQueryList();
	
			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Start processing response");
			List<DavNode> responses = root.getNodesFromPath("multistatus/response");
			if ( responses.isEmpty() ) {
				if ( errorCounter == 0 ) {
					responses = root.getNodesFromPath("error/valid-sync-token");
					errorCounter++;
					
					if ( ! responses.isEmpty() ) {
						Log.i("aCal","We sent an invalid sync-token.  Retrying without a sync-token.");
						syncToken = null;
						updateCollectionToken(syncToken);
						return doRegularSyncPropfind();
					}
				}
	
				responses = root.getNodesFromPath("multistatus/sync-response");
				if ( ! responses.isEmpty() ) {
					Log.e("aCal","CalDAV Server at "+requestor.getHostName()+" uses obsolete draft sync-response syntax. Falling back to inefficient PROPFIND.  Please upgrade your server.");
	/*
	 * We won't write it back to the server, because at least we can use this much as an indication that
	 * something has changed, so we'll just fall through and do a PROPFIND sync.
	 * 
					serverData.put(Servers.HAS_SYNC,0);
					Uri provider = ContentUris.withAppendedId(Servers.CONTENT_URI, serverData.getAsInteger(Servers._ID));
					cr.update(provider, Servers.cloneValidColumns(serverData), null, null);
	 */
					return doRegularSyncPropfind();
				}
				responses = root.getNodesFromPath("multistatus/sync-token");
				if ( responses.isEmpty() ) {
					Log.i("aCal","No sync-token in sync-report response. Falling back to PROPFIND.");
					updateCollectionToken(null);
					return doRegularSyncPropfind();
				}
				
			}
			else {
	
				for (DavNode response : responses) {
					String responseHref = response.segmentFromFirstHref("href");
					if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
						Log.println(Constants.LOGV,TAG, "Processing response for "+responseHref);
					//WriteActions action = WriteActions.UPDATE;
					DMQueryBuilder builder = new DMQueryBuilder();
					builder.setAction(QUERY_ACTION.UPDATE);
					
					ContentValues cv = 
						ResourceManager.getInstance(context).sendBlockingRequest(
								new RRGetResourceInCollection(collectionId,responseHref)).result();
	
					if ( cv == null ) {
						cv = new ContentValues();
						cv.put(ResourceTableManager.COLLECTION_ID, collectionId);
						cv.put(ResourceTableManager.RESOURCE_NAME, responseHref);
						cv.put(ResourceTableManager.NEEDS_SYNC, 1 );
						//action = WriteActions.INSERT;
						builder.setAction(QUERY_ACTION.INSERT);
					} else {
						builder.setWhereClause(ResourceTableManager.RESOURCE_ID+" = ?");
						builder.setwhereArgs(new String[] {cv.getAsString(ResourceTableManager.RESOURCE_ID)});
					}
					
					List<DavNode> aNode = response.getNodesFromPath("status");
					if ( aNode.isEmpty()
								|| aNode.get(0).getText().equalsIgnoreCase("HTTP/1.1 201 Created")
								|| aNode.get(0).getText().equalsIgnoreCase("HTTP/1.1 200 OK") ) {
		
						if ( Constants.LOG_DEBUG )
							Log.println(Constants.LOGD,TAG,"Updating node "+responseHref+" with "+builder.getAction().toString() );
						// We are dealing with an update or insert
						if ( !parseResponseNode(response, cv, false) ) continue;
						if ( cv.getAsInteger(ResourceTableManager.NEEDS_SYNC) == 1 ) needSyncAfterwards = true; 
		
					}
					//else if ( action == WriteActions.INSERT ) {
					else if ( builder.getAction()  == QUERY_ACTION.INSERT ) {				
						// It looked like an INSERT because it's not in our DB, but in fact
						// the status message was not 200/201 so it's a DELETE that we're
						// seeing reflected back at us.
						Log.i(TAG,"Ignoring delete sync on node '"+responseHref+"' which is already deleted from our DB." );
					}
					else {
						// This really *is* a DELETE, since the status could only
						// have said so.  Or we're getting invalid status messages
						// and their events all deserve to die anyway!
						if ( Constants.LOG_DEBUG )
							Log.println(Constants.LOGD,TAG,"Deleting node '"+responseHref+"'with status: "+aNode.get(0).getText() );
						//action = WriteActions.DELETE;
						builder.setAction(QUERY_ACTION.DELETE);
					}
					root.removeSubTree(response);
		
					//changeList.add( new ResourceModification(action, cv, null) );
					builder.setValues(cv);
					queryList.addAction(builder.build());
					
					
				}
			}
	
			// Pull the syncToken we will update with.
			syncToken = root.getFirstNodeText("multistatus/sync-token");
			if ( Constants.LOG_DEBUG )
				Log.println(Constants.LOGD,TAG,"Found sync token of '"+syncToken+"' in sync-report response." );
			
			//ResourceModification.commitChangeList(context, changeList, processor.getTableName(this));
			ResourceManager.getInstance(context).sendBlockingRequest(
					new RRBlockAndProcessQueryList(queryList));
		
			return needSyncAfterwards;
		}


	/**
	 * <p>
	 * Do a sync run using a PROPFIND against the collection and a pass through the DB comparing all resources
	 * currently on file with the ones we got from the PROPFIND. Laborious and potentially bandwidth hogging.
	 * </p>
	 * 
	 * @return true if we still need to syncMarkedResources() afterwards.
	 */
	private boolean doRegularSyncPropfind() {
		boolean needSyncAfterwards = false;
		if ( !collectionTagChanged() ) return false;

		DavNode root = 	doCalendarRequest("PROPFIND", 1,
					"<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
					+ "<propfind xmlns=\"DAV:\" xmlns:CS=\"http://calendarserver.org/ns/\">"
						+ "<prop>"
							+ "<getetag/>"
							+ "<CS:getctag/>"
							+ "<sync-token/>"
						+ "</prop>"
					+ "</propfind>"
				);

		if (root == null ) {
			Log.i(TAG, "Unable to PROPFIND collection " + this.collectionPath + " (ID:" + this.collectionId
						+ " - no data from server.");
			syncWasCompleted = false;
			return false;
		}


		Map<String, ContentValues> ourResourceMap = 
			ResourceManager.getInstance(context).sendBlockingRequest(new RRSyncQueryMap(collectionId,false)).result();

		DMQueryList queryList = new DMQueryList();

		try {
			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Start processing PROPFIND response");
			long start2 = System.currentTimeMillis();
			List<DavNode> responses = root.getNodesFromPath("multistatus/response");

			for (DavNode response : responses) {
				String responseHref = response.segmentFromFirstHref("href");
				if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
					Log.println(Constants.LOGV,TAG, "Processing response for "+responseHref);

				ContentValues cv = null;				
				//WriteActions action = WriteActions.UPDATE;
				DMQueryBuilder builder = new DMQueryBuilder();
				builder.setAction(QUERY_ACTION.UPDATE);
				if ( ourResourceMap != null && ourResourceMap.containsKey(responseHref)) {
					cv = ourResourceMap.get(responseHref);
					ourResourceMap.remove(responseHref);
					builder.setWhereClause(ResourceTableManager.RESOURCE_ID+" = ?");
					builder.setwhereArgs(new String[] {cv.getAsString(ResourceTableManager.RESOURCE_ID)});
				}
				else {
					cv = new ContentValues();
					cv.put(ResourceTableManager.COLLECTION_ID, collectionId);
					cv.put(ResourceTableManager.RESOURCE_NAME, responseHref);
					cv.put(ResourceTableManager.NEEDS_SYNC, 1);
					//action = WriteActions.INSERT;
					builder.setAction(QUERY_ACTION.INSERT);
				}

				if ( !parseResponseNode(response, cv, false) ) continue;
				if ( cv.getAsInteger(ResourceTableManager.NEEDS_SYNC) == 1 ) needSyncAfterwards = true; 

				needSyncAfterwards = true; 

				builder.setValues(cv);
				queryList.addAction(builder.build());

				if ( queryList.size() > nPerMultiget ) {
					ResourceManager.getInstance(context).sendBlockingRequest(new RRBlockAndProcessQueryList(queryList));
					queryList = new DMQueryList();
				}
			}
			
			if ( ourResourceMap != null ) {
				// Delete any records still in ourResourceMap (hence not on server any longer)
				// We build the delete as a single query.
				
				StringBuilder inList = new StringBuilder(ResourceTableManager.RESOURCE_ID);
				inList.append(" IN (");
				boolean pastFirst = false;
				for( String name : ourResourceMap.keySet() ) {
					ContentValues cv = ourResourceMap.get(name);
					if ( pastFirst ) inList.append(',');
					else pastFirst = true;
					inList.append(cv.getAsString(ResourceTableManager.RESOURCE_ID));
				}
				if ( pastFirst ) {
					inList.append(")");
					queryList.addAction(new DMDeleteQuery(inList.toString(), null));
				}
			}
			
			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Completed processing of PROPFIND sync in " + (System.currentTimeMillis() - start2) + "ms");
		}
		catch (Exception e) {
			Log.e(TAG, "Exception processing PROPFIND response: " + e.getMessage());
			Log.e(TAG, Log.getStackTraceString(e));
		}


		if ( !queryList.isEmpty() )
			ResourceManager.getInstance(context).sendBlockingRequest( new RRBlockAndProcessQueryList(queryList));
		
		return needSyncAfterwards;
	}

	
	
	/**
	 * Checks the resources we have in the DB currently flagged as needing synchronisation, and synchronises
	 * them if they are using an addressbook-multiget or calendar-multiget request, depending on the
	 * collection type.
	 */
	private void syncMarkedResources( Map<String, ContentValues> originalData ) {

		if (Constants.LOG_DEBUG)
			Log.println(Constants.LOGD,TAG, "Found " + originalData.size() + " resources marked as needing synchronisation.");

		Set<String> hrefSet = originalData.keySet();
		Object[] hrefs = hrefSet.toArray();

		if (serverData.getAsInteger(Servers.HAS_MULTIGET) != null && 1 == serverData.getAsInteger(Servers.HAS_MULTIGET)) {
			syncWithMultiget(originalData, hrefs);
		}
		else {
			syncWithGet(originalData, hrefs);
		}
	}
	
	/**
	 * <p>
	 * Performs a sync using a series of GET requests to retrieve each resource needing sync. This is a
	 * fallback strategy and we really expect multiget to work in almost all circumstances.
	 * </p>
	 * 
	 * @param originalData
	 *            <p>
	 *            The href => Contentvalues map.
	 *            </p>
	 * @param hrefs
	 *            <p>
	 *            The array of hrefs
	 *            </p>
	 */
	private void syncWithGet(Map<String, ContentValues> originalData, Object[] hrefs) {
		long fullMethod = System.currentTimeMillis();

		Header[] headers = new Header[] {};
		//List<ResourceModification> changeList = new ArrayList<ResourceModification>(hrefs.length);
		DMQueryList queryList = new DMQueryList();
		String path;
		InputStream in;
		int status;

		for (int hrefIndex = 0; hrefIndex < hrefs.length; hrefIndex++) {
			path = collectionPath + hrefs[hrefIndex];
			ContentValues originalValues = originalData.get(hrefs[hrefIndex]);

			try {
				in = requestor.doRequest("GET", path, headers, "");
			}
			catch (ConnectionFailedException e) {
				Log.i(TAG,"ConnectionFailedException ("+e.getMessage()+") on GET from "+path);
				continue;
			}
			catch (SendRequestFailedException e) {
				Log.i(TAG,"SendRequestFailedException ("+e.getMessage()+") on GET from "+path);
				continue;
			}
			catch (SSLException e) {
				Log.i(TAG,"SSLException on GET from "+path);
				continue;
			}
			if (in == null) {
				if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Error - Unable to get data stream from server.");
				continue;
			}
			else {
				status = requestor.getStatusCode();
				switch (status) {
					case 200: // Status O.K.
						StringBuilder resourceData = new StringBuilder();
						BufferedReader r = new BufferedReader(new InputStreamReader(in),4096);
						String line;
						try {
							while ((line = r.readLine() ) != null) {
								resourceData.append(line);
								resourceData.append("\n");
							}
						}
						catch (IOException e) {
							Log.i(TAG,Log.getStackTraceString(e));
						}
						originalValues.put(ResourceTableManager.RESOURCE_DATA, resourceData.toString() );
						for (Header hdr : requestor.getResponseHeaders()) {
							if (hdr.getName().equalsIgnoreCase("ETag")) {
								originalValues.put(ResourceTableManager.ETAG, hdr.getValue());
								break;
							}
						}
						originalValues.put(ResourceTableManager.NEEDS_SYNC, 0);
						queryList.addAction(new DMUpdateQuery(originalValues, ResourceTableManager.RESOURCE_ID+" = ?", new String[]{originalValues.getAsString(ResourceTableManager.RESOURCE_ID)}));
						//changeList.add( new ResourceModification(WriteActions.UPDATE, cv, null) );
						if (Constants.LOG_DEBUG)
							Log.println(Constants.LOGD,TAG, "Get response for "+hrefs[hrefIndex] );
						break;

					case 404: // Not found.
						queryList.addAction(new DMDeleteQuery(ResourceTableManager.RESOURCE_ID+" = ?",
								new String[]{originalValues.getAsString(ResourceTableManager.RESOURCE_ID)}));
						break;
						
					default: // Unknown code
						Log.w(TAG, "Unhandled status " + status + " on GET request for " + path);
				}
			}
			if ( queryList.size() > nPerMultiget ) {
				ResourceManager.getInstance(context).sendBlockingRequest(new RRBlockAndProcessQueryList(queryList));
				queryList = new DMQueryList();
			}
		}

		if ( !queryList.isEmpty() )
			ResourceManager.getInstance(context).sendBlockingRequest(new RRBlockAndProcessQueryList(queryList));
		

		if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
			Log.println(Constants.LOGV,TAG, "syncWithGet() for " + hrefs.length + " resources took "
						+ (System.currentTimeMillis() - fullMethod) + "ms");
		return;
	}

		
	
	/**
	 * <p>
	 * Parse a single &lt;response&gt; node within a &lt;multistatus&gt;
	 * </p>
	 * @return true If we need to write to the database, false otherwise.
	 */
	private boolean parseResponseNode(DavNode responseNode, ContentValues cv, boolean multiGetWithData) {

		DavNode prop = null;
		for ( DavNode testPs : responseNode.getNodesFromPath("propstat")) {
			String statusText = testPs.getFirstNodeText("status"); 
			if ( statusText.equalsIgnoreCase("HTTP/1.1 200 OK") || statusText.equalsIgnoreCase("HTTP/1.1 201 Created")) {
				prop = testPs.getNodesFromPath("prop").get(0);
				break;
			}

			if ( multiGetWithData && statusText.equalsIgnoreCase("HTTP/1.1 404 OK") || statusText.equalsIgnoreCase("HTTP/1.1 201 Created")) {
				prop = testPs.getNodesFromPath("prop").get(0);
				List<DavNode> dataNodes = prop.getNodesFromPath(dataType + "-data");
				if ( !dataNodes.isEmpty() ) {
					// Force a synchronisation after this.
					this.synchronisationForced = true;
				}
				break;
			}

		}
		
		if ( prop == null ) {
			return false;
		}
		else {
			String s = prop.getFirstNodeText("getctag");
			if ( s != null ) {
				collectionChanged = (collectionData.getAsString(DavCollections.COLLECTION_TAG) == null
									|| s.equals(collectionData.getAsString(DavCollections.COLLECTION_TAG)));
				if ( collectionChanged ) collectionData.put(DavCollections.COLLECTION_TAG, s);
				cv.put("COLLECTION", true);
				return false;
			}
			else {
				String etag = prop.getFirstNodeText("getetag");
				
				if ( etag != null ) {
					String oldEtag = cv.getAsString(ResourceTableManager.ETAG);
					
					if ( etag.equals(oldEtag) ) {
						cv.put(ResourceTableManager.NEEDS_SYNC, 0);
						if ( Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents ) Log.println(Constants.LOGD,TAG,
								"Found etag "+etag+" in response.  Old etag was "+oldEtag+".  No sync needed.");
						return true;
					}
					else {
						if ( Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents ) Log.println(Constants.LOGD,TAG,
								"Found etag "+etag+" in response.  Old etags was "+oldEtag+".  Sync will be needed.");
						cv.put(ResourceTableManager.NEEDS_SYNC, 1);
					}
				}

				String data = prop.getFirstNodeText(dataType + "-data");
				if ( data != null ) {
					if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents ) {
						Log.println(Constants.LOGV,TAG,"Found data in response:");
						Log.println(Constants.LOGV,TAG,data);
						Log.println(Constants.LOGV,TAG,StaticHelpers.toHexString(data.substring(0,40).getBytes()));
					}
					cv.put(ResourceTableManager.RESOURCE_DATA, data);
					cv.put(ResourceTableManager.ETAG, etag);
					cv.put(ResourceTableManager.NEEDS_SYNC, 0);
					if ( Constants.LOG_DEBUG && Constants.debugSyncCollectionContents ) Log.println(Constants.LOGD,TAG,
							"Found data for etag '"+etag+"' in response.  Sync not needed.");
				}
				else if ( Constants.LOG_DEBUG && Constants.debugSyncCollectionContents ) Log.println(Constants.LOGD,TAG,
						"Found no data for etag '"+etag+"' in response.  Sync is needed.");
					

				s = prop.getFirstNodeText("getlastmodified");
				if ( s != null ) cv.put(ResourceTableManager.LAST_MODIFIED, s);

				s = prop.getFirstNodeText("getcontenttype");
				if ( s != null ) cv.put(ResourceTableManager.CONTENT_TYPE, s);
			}
		}

		return true;
	}

	
	/**
	 * <p>
	 * Performs a sync using a series of multiget REPORT requests to retrieve the resources needing sync.
	 * </p>
	 * 
	 * @param originalData
	 *            <p>
	 *            The href => Contentvalues map.
	 *            </p>
	 * @param hrefs
	 *            <p>
	 *            The array of hrefs
	 *            </p>
	 */
	private void syncWithMultiget(Map<String, ContentValues> originalData, Object[] hrefs) {

		String baseXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
			+ "<" + multigetReportTag + " xmlns=\"" + nameSpace + "\" xmlns:D=\"DAV:\">\n"
				+ "<D:prop>\n"
					+ "<D:getetag/>\n"
					+ "<D:getcontenttype/>\n"
					+ "<D:getlastmodified/>\n"
					+ "<" + dataType + "-data/>\n"
				+ "</D:prop>\n"
				+ "%s"
				+ "</" + multigetReportTag + ">";

		ArrayList<String> toBeRemoved = new ArrayList<String>(hrefs.length);
		for( Object o : hrefs ) {
			if ( o == null ) continue;
			Matcher m = Constants.matchSegmentName.matcher(o.toString());
			if ( m.find() ) toBeRemoved.add(m.group(1));
		}

		String pathOnServer =  StaticHelpers.pathOnServer(collectionPath);
		int limit;
		StringBuilder hrefList; 
		for (int hrefIndex = 0; hrefIndex < hrefs.length; hrefIndex += nPerMultiget) {
			limit = nPerMultiget + hrefIndex;
			if ( limit > hrefs.length ) limit = hrefs.length;
			
			hrefList = new StringBuilder();
			for (int i = hrefIndex; i < limit; i++) {
				try {
					hrefList.append(String.format("<D:href>%s</D:href>\n", pathOnServer + hrefs[i].toString()));
					if (Constants.LOG_DEBUG)
						Log.w(TAG,"Fetching resource from: "+ pathOnServer + " " + hrefs[i].toString());
				}
				catch( Exception e) {
					Log.e(TAG,"Error syncing resource.", e);
				}
			}
		
			if (Constants.LOG_DEBUG)
				Log.println(Constants.LOGD,TAG, "Requesting " + multigetReportTag + " for " + nPerMultiget + " resources out of "+hrefs.length+"." );

			DavNode root = doCalendarRequest( "REPORT", 1, String.format(baseXml,hrefList.toString()) );

			if (root == null) {
				Log.w(TAG, "Unable to sync collection " + this.collectionPath + " (ID:" + this.collectionId
							+ " - no data from server).");
				return;
			}

			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Start processing response");
			List<DavNode> responses = root.getNodesFromPath("multistatus/response");
			//List<ResourceModification> changeList = new ArrayList<ResourceModification>(hrefList.length());
			DMQueryList queryList = new DMQueryList();

			for (DavNode response : responses) {
				try { Thread.sleep(2); } catch ( InterruptedException e ) { }  // Give the UI thread more of a chance to do stuff.
				String name = response.segmentFromFirstHref("href");
				if ( toBeRemoved.contains(name) ) {
					if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
						Log.println(Constants.LOGV,TAG,"Found href in our list.");
					toBeRemoved.remove(name);
				}

				ContentValues cv = originalData.get(name);
				DMQueryBuilder builder = new DMQueryBuilder();
				builder.setAction(QUERY_ACTION.UPDATE);
				
				if ( cv == null ) {
					cv = new ContentValues();
					cv.put(ResourceTableManager.COLLECTION_ID, collectionId);
					cv.put(ResourceTableManager.RESOURCE_NAME, name);
					builder.setAction(QUERY_ACTION.INSERT);
				} else {
					builder.setWhereClause(ResourceTableManager.RESOURCE_ID+" = ?");
					builder.setwhereArgs(new String[]{cv.getAsString(ResourceTableManager.RESOURCE_ID)});
				}
				if ( !parseResponseNode(response, cv, true) ) continue;
				if ( cv.getAsString("COLLECTION") != null ) continue;

				if (Constants.LOG_DEBUG)
					Log.println(Constants.LOGD,TAG, "Multiget response needs sync="+cv.getAsString(ResourceTableManager.NEEDS_SYNC)+" for "+name );
				
				//changeList.add( new ResourceModification(action, cv, null) );
				builder.setValues(cv);
				queryList.addAction(builder.build());
			}

			//ResourceModification.commitChangeList(context, changeList, processor.getTableName(this));
			ResourceManager.getInstance(context).sendBlockingRequest( new RRBlockAndProcessQueryList(queryList));
			try { Thread.sleep(300); } catch ( InterruptedException e ) { }
		}

		for( String href : toBeRemoved ) {
			Log.println(Constants.LOGV,TAG,"Did not find +"+href+"+ in the list.");
		}
		if ( toBeRemoved.size() > 0 ) {
			doRegularSyncPropfind();
			return;
		}

		return;
	}
	
	private void calculateNextSchedulingTime() {
		String lastSync = collectionData.getAsString(DavCollections.LAST_SYNCHRONISED);
		timeToWait = minBetweenSyncs;
		if ( lastSync != null ) {
			long maxAge3g = collectionData.getAsLong(DavCollections.MAX_SYNC_AGE_3G);
			long maxAgeWifi = collectionData.getAsLong(DavCollections.MAX_SYNC_AGE_WIFI);
			timeToWait = (maxAge3g > maxAgeWifi ? maxAgeWifi : maxAge3g);

			AcalDateTime lastRunTime = null;
			lastRunTime = AcalDateTime.fromString(lastSync);
			timeToWait += (lastRunTime.getMillis() - System.currentTimeMillis());
			
			if ( timeToWait > 7200000 ) {
				Log.println(Constants.LOGV,TAG, "lastSync='"+lastSync+"' and lastRunTime='"
						+ lastRunTime.toString()
						+"' so timeToWait=" + Long.toString(timeToWait / 1000) + " seconds.");
			}
			
			if ( minBetweenSyncs > timeToWait ) timeToWait = minBetweenSyncs;
		}

		if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
			Log.println(Constants.LOGV,TAG, "Scheduling next sync status check for "+collectionId+" - '"
						+ collectionData.getAsString(DavCollections.DISPLAYNAME)
						+"' in " + Long.toString(timeToWait / 1000) + " seconds.");
		this.scheduleNextInstance = true;
	}

	
	private void scheduleNextInstance() {
		this.collectionData = null;
		this.serverData = null;
		this.requestor = null;

		if (scheduleNextInstance) {
			this.TIME_TO_EXECUTE = timeToWait;
			Log.println(Constants.LOGV,TAG,
					"Scheduling next instance in " + this.TIME_TO_EXECUTE / 1000L + " seconds.");
			context.addWorkerJob(this);
		}

		this.context = null;

	}
	
	private boolean timeToRun() {
		if ( synchronisationForced ) {
			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Synchronising now, since a sync has been forced.");
			return true;
		}
		String needsSyncNow = collectionData.getAsString(DavCollections.NEEDS_SYNC);
		if ( needsSyncNow == null || needsSyncNow.equals("1") ) {
			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Synchronising now, since needs_sync is true.");
			return true; 
		}
		String lastSyncString = collectionData.getAsString(DavCollections.LAST_SYNCHRONISED);
		if ( lastSyncString == null ) { 
			if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
				Log.println(Constants.LOGV,TAG, "Synchronising now, since last_sync is null.");
			return true; 
		}
		AcalDateTime nextRunTime = null;

		nextRunTime = AcalDateTime.fromString(lastSyncString);
		if ( nextRunTime == null ) return true;
		
		AcalDateTime currentTime = AcalDateTime.getUTCInstance();
		
		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = conMan.getActiveNetworkInfo();
		long maxAgeMs = minBetweenSyncs;
		if ( netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_MOBILE )
			maxAgeMs = collectionData.getAsLong(DavCollections.MAX_SYNC_AGE_3G);
		else
			maxAgeMs = collectionData.getAsLong(DavCollections.MAX_SYNC_AGE_WIFI);

		if ( maxAgeMs < minBetweenSyncs ) maxAgeMs = minBetweenSyncs;
		nextRunTime.addSeconds(maxAgeMs/1000L);

		if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
			Log.println(Constants.LOGV,TAG, "Considering whether we are past "
						+ nextRunTime.fmtIcal() + "("+nextRunTime.getMillis()+") yet? "
						+ "Now: " + currentTime.fmtIcal() + "("+currentTime.getMillis()+")... So: "
						+ (nextRunTime.getMillis() <= currentTime.getMillis() ? "yes" : "no"));

		return (nextRunTime.getMillis() <= currentTime.getMillis());
	}


	/**
	 * <p>
	 * Called from the constructor to initialise all of the collection-related data we need to be able to sync
	 * properly. This includes a bunch of server related data pulled into serverData
	 * </p>
	 * 
	 * @return Whether it successfully read enough data to proceed
	 */
	private boolean getCollectionInfo() {
		long start = System.currentTimeMillis();
		collectionData = DavCollections.getRow(collectionId, context.getContentResolver());
			// last line was: Servers.getRow(serverId, context.getContentResolver()); ??????
		if ( collectionData == null ) {
			Log.e(TAG, "Error getting collection data from DB for collection ID " + collectionId);
			return false;
		}
		
		serverId = collectionData.getAsInteger(DavCollections.SERVER_ID);
		collectionPath = collectionData.getAsString(DavCollections.COLLECTION_PATH);
		oldSyncToken = collectionData.getAsString(DavCollections.SYNC_TOKEN);
		isAddressbook = (1 == collectionData.getAsInteger(DavCollections.ACTIVE_ADDRESSBOOK));
		dataType = "calendar";
		multigetReportTag = "calendar-multiget";
		nameSpace = Constants.NS_CALDAV;
		if (isAddressbook) {
			dataType = "address";
			multigetReportTag = dataType + "book-multiget";
			nameSpace = Constants.NS_CARDDAV;
		}

		try {
			// get serverData
			serverData = Servers.getRow(serverId, context.getContentResolver());
			if (serverData == null) throw new Exception("No record for ID " + serverId);
			requestor = AcalRequestor.fromServerValues(serverData);
			requestor.setPath(collectionPath);
		}
		catch (Exception e) {
			// Error getting data
			Log.e(TAG, "Error getting server data: " + e.getMessage());
			Log.e(TAG, "Deleting invalid collection Record.");
			context.getContentResolver().delete(Uri.withAppendedPath(DavCollections.CONTENT_URI,Long.toString(collectionId)), null, null);
			return false;
		}

		if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
			Log.println(Constants.LOGV,TAG,
				"getCollectionInfo() completed in " + (System.currentTimeMillis() - start) + "ms");
		return true;
	}

	
	private void updateCollectionToken(String newToken) {
		ContentValues updateData = new ContentValues();
		updateData.put(DavCollections.SYNC_TOKEN, newToken);
		if (Constants.LOG_VERBOSE && Constants.debugSyncCollectionContents )
			Log.i(TAG,"Updated collection record with new sync token '"+syncToken+"'");
		ResourceManager.getInstance(context).sendBlockingRequest(
				new RRUpdateCollection(collectionId,collectionData));
		
	}


	public boolean equals(Object that) {
		if (this == that) return true;
		if (!(that instanceof SyncCollectionContents)) return false;
		SyncCollectionContents thatCis = (SyncCollectionContents) that;
		return this.collectionId == thatCis.collectionId;
	}

	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash(result, this.collectionId);
		return result;
	}

	@Override
	public String getDescription() {
		return "Syncing collection contents of collection " + collectionId;
	}
	
}
