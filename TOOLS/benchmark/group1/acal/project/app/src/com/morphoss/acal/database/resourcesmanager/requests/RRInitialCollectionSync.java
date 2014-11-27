package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLHandshakeException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.DMQueryBuilder;
import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.DatabaseTableManager.QUERY_ACTION;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.WriteableResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ResourceRequest;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.HomeSetsUpdate;
import com.morphoss.acal.service.ServiceJob;
import com.morphoss.acal.service.SyncCollectionContents;
import com.morphoss.acal.service.SynchronisationJobs;
import com.morphoss.acal.service.aCalService;
import com.morphoss.acal.service.connector.AcalRequestor;
import com.morphoss.acal.xml.DavNode;

public class RRInitialCollectionSync implements ResourceRequest {

	private static final String TAG = "aCal RRInitialCollectionSync";
	
	private long collectionId = -2;
	private int serverId = -2;
	private String collectionPath = null;
	private ContentValues collectionValues = null;
	private boolean isCollectionIdAssigned = false;
	private boolean collectionNeedsSync = false;
	private AcalRequestor requestor;
	private aCalService acalService;
	private WriteableResourceTableManager processor;

	private final String syncData = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
										"<sync-collection xmlns=\"DAV:\">"+
											"<sync-token/>"+
											"<sync-level>1</sync-level>"+
											"<prop>"+
												"<getetag/>"+
											"</prop>"+
/**											"<limit>"+
												"<nresults>"+MAX_RESULTS+"</nresults>"+
											"</limit>"+
*/
										"</sync-collection>";
												
	private final String calendarQuery = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
	"<calendar-query xmlns:D=\"DAV:\" xmlns=\"urn:ietf:params:xml:ns:caldav\">\n"+
	"  <D:prop>\n"+
	"   <D:getetag/>\n"+
	"   <calendar-data/>\n"+
	"   <D:getlastmodified/>\n"+
	"   <D:getcontenttype/>\n"+
	"  </D:prop>\n"+
	"  <filter>\n"+
	"    <comp-filter name=\"VCALENDAR\">\n"+
	"      <comp-filter name=\"VEVENT\">\n"+
	"        <time-range start=\"%s\" end=\"%s\"/>\n"+
	"      </comp-filter>\n"+
	"    </comp-filter>\n"+
	"  </filter>\n"+
	"</calendar-query>";
	
	public RRInitialCollectionSync(long collectionId) {
		this.collectionId = collectionId;
		this.isCollectionIdAssigned = true;
		collectionPath = null;
	}

	public RRInitialCollectionSync(long collectionId, int serverId, String collectionPath) {
		this.collectionId = collectionId;
		this.serverId = serverId;
		this.collectionPath = collectionPath;
		if ( collectionPath == null || serverId < 0 || collectionId < 0 )
			throw new IllegalArgumentException("collectionPath, serverId and collectionId should be assigned real values!");
		this.isCollectionIdAssigned = true;
	}

	public RRInitialCollectionSync(int serverId, String collectionPath) {
		this.serverId = serverId;
		this.collectionPath = collectionPath;
	}

	private boolean processingComplete = false;

	@Override
	public boolean isProcessed() { return this.processingComplete; }
	@Override
	public synchronized void setProcessed() { this.processingComplete = true; }
	
	public void setService(aCalService svc) {
		this.acalService = svc;
	}

	@Override
	public void process(WriteableResourceTableManager processor) throws ResourceProcessingException {
		this.processor = processor;  

		if ( !getCollectionId() ) {
			setProcessed();
			return;
		}

		collectionNeedsSync = false;

		if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Starting initial sync process for server " + serverId + ", Collection: " + collectionPath);
		ContentValues serverData;
		try {
			// get serverData
			
			serverData = processor.getServerRow(serverId);
			if (serverData == null) {
				setProcessed();
				throw new ResourceProcessingException("No record for ID " + serverId);
			}
			requestor = AcalRequestor.fromServerValues(serverData);
			requestor.interpretUriString(collectionPath);
		}
		catch (Exception e) {
			// Error getting data
			Log.e(TAG, "Error getting server data: " + e.getMessage());
			Log.e(TAG, "Deleting invalid collection Record.");
			processor.deleteInvalidCollectionRecord(collectionId);			
			setProcessed();
			return;
		}

		if ( null == serverData.getAsInteger(Servers.HAS_SYNC) || 0 == serverData.getAsInteger(Servers.HAS_SYNC) ) {
			Log.i(TAG, "Skipping initial sync process since server does not support WebDAV synchronisation");
			collectionNeedsSync = true;

			if ( collectionValues.getAsInteger(DavCollections.ACTIVE_EVENTS) == 1 ) {
				syncRecentEvents();
			}
		}
		else {

			DavNode root;
			try {
				root = requestor.doXmlRequest("REPORT", collectionPath, SynchronisationJobs.getReportHeaders(1), syncData);
			}
			catch ( SSLHandshakeException e ) {
				Log.w(TAG,"Certificate Problem", e);
				return;
			}
			if (requestor.getStatusCode() == 404) {
				Log.i(TAG, "Sync REPORT got 404 on " + collectionPath + " so a HomeSetsUpdate is being scheduled.");
				ServiceJob sj = new HomeSetsUpdate(serverId);
				acalService.addWorkerJob(sj);
				setProcessed();
				return;
			}
			if ( root == null ) {
				Log.w(TAG, "Unable to do intial sync - no XML data from server.");
				collectionNeedsSync = true;
			}
			else {
				processSyncToDatabase(root);
			}
		}

		// Now schedule a sync contents on this.
		acalService.addWorkerJob(new SyncCollectionContents(collectionId,collectionNeedsSync));
		setProcessed();
	}

	private boolean getCollectionId() {
		try {
			if ( collectionPath == null ) {
				collectionValues = processor.getCollectionRow(collectionId);
				if ( collectionValues != null ) {
					serverId = collectionValues.getAsInteger(DavCollections.SERVER_ID);
					collectionPath = collectionValues.getAsString(DavCollections.COLLECTION_PATH);
					isCollectionIdAssigned = true;
				}
				else {
					Log.e(TAG,"Cannot find collection ID "+collectionId+" which I should sync!");
				}
			}
			else if ( serverId > 0 && collectionId < 0 ) {
				collectionValues = new ContentValues();
				if (this.getCollectionIdByPath(processor.getContext(), collectionValues, serverId, collectionPath)) {
					collectionId = collectionValues.getAsInteger(DavCollections._ID);
					isCollectionIdAssigned = true;
				}
				else {
					Log.e(TAG,"Cannot find collection "+collectionPath+" which I should sync!");
				}
			}
			else if ( collectionId < 0 ) {
				Log.e(TAG,"Cannot find collection which I should sync!");
				throw new IllegalStateException();
			}
		}
		catch ( Exception e ) {
			Log.e(TAG,"Error finding "+collectionPath+" in database: " + e.getMessage());
			Log.e(TAG,Log.getStackTraceString(e));
		}
		finally {
		}
		return ( isCollectionIdAssigned && collectionPath != null );
	}

	public boolean getCollectionIdByPath(Context context, ContentValues values, long serverId, String collectionPath ) {
		Cursor cursor = context.getContentResolver().query(DavCollections.CONTENT_URI, null, 
			DavCollections.SERVER_ID + "=? AND " + DavCollections.COLLECTION_PATH + "=?",
			new String[] { "" + serverId, collectionPath }, null);
	if ( cursor.moveToFirst() ) {
		DatabaseUtils.cursorRowToContentValues(cursor, values);
		cursor.close();
		return true;
	}
	cursor.close();
	return false;
}

	
	public void processSyncToDatabase( DavNode root ) {
		try {

			if (Constants.LOG_VERBOSE) Log.v(TAG, "processSyncToDatabase started.");

			Map<String, ContentValues>  databaseMap = processor.contentQueryMap(ResourceTableManager.COLLECTION_ID + " = ? ", new String[] { collectionId + "" });
			Collection<ContentValues> databaseList = databaseMap.values();
			Map<String, ContentValues> serverList = new HashMap<String,ContentValues>();
			List<DavNode> responseList = root.getNodesFromPath("multistatus/response");

			if ( Constants.LOG_VERBOSE ) {
				Log.v(TAG,"Database list has "+databaseList.size()+" rows.");
				Log.v(TAG,"Response list has "+responseList.size()+" rows.");
			}

			// This could potentially be a really big list of small response sections, so
			// we allocate variables here and re-use inside loop for performance.
			List<DavNode> propstats;
			String name;
			DavNode prop = null;

			//iterate through each response and add to serverList
			for (DavNode response : responseList) {
				//get each prop
				prop = null;
				propstats = response.getNodesFromPath("propstat");
				name = response.segmentFromFirstHref("href");
				for (DavNode propstat : propstats) {
					if ( !propstat.getFirstNodeText("status").equalsIgnoreCase("HTTP/1.1 200 OK") ) continue;
					prop = propstat.getNodesFromPath("prop").get(0); 
				}
				if ( name == null || prop == null ) continue;

				ContentValues cv = new ContentValues();
				cv.put(ResourceTableManager.COLLECTION_ID, collectionId);
				cv.put(ResourceTableManager.RESOURCE_NAME, name);
				cv.put(ResourceTableManager.NEEDS_SYNC, 1);
				cv.put(ResourceTableManager.IS_PENDING, false);

				serverList.put(name, cv);

				//Remove subtree to free up memory
				root.removeSubTree(response);
			}
			//Remove all duplicates
			removeDuplicates(databaseMap, serverList);

			DMQueryList newChangeList = processor.getNewQueryList();
			
			for ( ContentValues cv : databaseList ) {
				newChangeList.addAction(processor.getNewDeleteQuery(ResourceTableManager.RESOURCE_ID+" = ?", new String[] {cv.getAsString(ResourceTableManager.RESOURCE_ID)}));
			}
			String id;
			ContentValues cv;
			for ( Entry<String,ContentValues> e : serverList.entrySet() ) {
				cv = e.getValue();
				id = cv.getAsString(ResourceTableManager.RESOURCE_ID);
				if (id == null || id.equals("")) {
					newChangeList.addAction(processor.getNewInsertQuery(null, cv));
				} else {
					newChangeList.addAction(processor.getNewUpdateQuery(cv, ResourceTableManager.RESOURCE_ID+" = ?", new String[] { cv.getAsString(ResourceTableManager.RESOURCE_ID)}));
				}
			}
			
			
			String syncToken = null;
			if (root != null) syncToken = root.getFirstNodeText("multistatus/sync-token");
			processor.doSyncListAndToken(newChangeList, collectionId, syncToken);

			if (Constants.LOG_VERBOSE)
				Log.v(TAG, databaseList.size() + " records deleted, " + serverList.size() + " updated");			

			if ( collectionValues.getAsInteger(DavCollections.ACTIVE_EVENTS) == 1 ) {
				syncRecentEvents();
			}

			//We can now approve the transaction
		}
		catch (Exception e) {
			Log.w(TAG, "Initial Sync transaction failed. Data not will not be saved.");
			Log.e(TAG,Log.getStackTraceString(e));
		}

		//lastly, create new regular sync
		acalService.addWorkerJob(new SyncCollectionContents(this.collectionId)); 
	}


	/**
	 * When we have a lot of events to sync, we want to make sure that the period
	 * of time around the present day is in sync first.
	 */
	private void syncRecentEvents() {
		AcalDateTime from = new AcalDateTime().applyLocalTimeZone().addDays(-32).shiftTimeZone("UTC");
		AcalDateTime until = new AcalDateTime().applyLocalTimeZone().addDays(+68).shiftTimeZone("UTC");

		if (Constants.LOG_DEBUG)
			Log.println(Constants.LOGD,TAG, "Doing a recent sync of events from "+from.toString()+" to "+until.toString());			

		DavNode root;
		try {
			root = requestor.doXmlRequest("REPORT", collectionPath, SynchronisationJobs.getReportHeaders(1),
					String.format(calendarQuery, from.fmtIcal(), until.fmtIcal()));
		}
		catch ( SSLHandshakeException e ) {
			Log.w(TAG,"SSL Handshake Exception", e);
			return;
		}

		if ( root == null ) {
			Log.println(Constants.LOGD,TAG, "REPORT failed for events "+from.toString()+" to "+until.toString()+" on "+requestor.fullUrl());
			return;
		}

		//ArrayList<ResourceModification> changeList = new ArrayList<ResourceModification>(); 
		DMQueryList queryList = processor.getNewQueryList();

		List<DavNode> responses = root.getNodesFromPath("multistatus/response");

		for (DavNode response : responses) {
			String name = response.segmentFromFirstHref("href");
			ContentValues cv = null;
			ArrayList<ContentValues> cvList = processor.query(null,
										ResourceTableManager.COLLECTION_ID+"=? AND "+ResourceTableManager.RESOURCE_NAME+"=?",
										new String[] {Long.toString(collectionId), name}, null, null, null);
			if (!cvList.isEmpty()) cv = cvList.get(0);

			//WriteActions action = WriteActions.UPDATE;
			DMQueryBuilder builder = processor.getNewQueryBuilder();
			builder.setAction(QUERY_ACTION.UPDATE);
			if ( cv == null ) {
				cv = new ContentValues();
				cv.put(ResourceTableManager.COLLECTION_ID, collectionId);
				cv.put(ResourceTableManager.RESOURCE_NAME, name);
				cv.put(ResourceTableManager.NEEDS_SYNC, 1);
				builder.setAction(QUERY_ACTION.INSERT);
				//action= WriteActions.INSERT;
			} else {
				builder.setWhereClause(ResourceTableManager.RESOURCE_ID+" = ?")
					.setwhereArgs(new String[]{cv.getAsString(ResourceTableManager.RESOURCE_ID)});
			}
			if ( !parseResponseNode(response, cv) ) continue;

			//changeList.add( new ResourceModification(action,cv,null));
			builder.setValues(cv);
			queryList.addAction(builder.build());
		}

		//ResourceModification.commitChangeList(acalService, changeList, processor.getTableName());
		processor.processActions(queryList);

	}


	/**
	 * <p>
	 * Parse a single &lt;response&gt; node within a &lt;multistatus&gt;
	 * </p>
	 * @return true If we need to write to the database, false otherwise.
	 */
	private boolean parseResponseNode(DavNode responseNode, ContentValues cv) {

		List<DavNode> propstats = responseNode.getNodesFromPath("propstat");
		if ( propstats.size() < 1 ) return false;

		for (DavNode propstat : propstats) {
			if ( !propstat.getFirstNodeText("status").equalsIgnoreCase("HTTP/1.1 200 OK") ) {
				responseNode.removeSubTree(propstat);
				continue;
			}

			DavNode prop = propstat.getNodesFromPath("prop").get(0);

			String etag = prop.getFirstNodeText("getetag");
			if ( etag == null ) return false;

			String oldEtag = cv.getAsString(ResourceTableManager.ETAG);
			if ( oldEtag != null && oldEtag.equals(etag) ) {
				if ( Constants.LOG_VERBOSE ) Log.v(TAG,"ETag matches existing record, so no need to sync.");
				cv.put(ResourceTableManager.NEEDS_SYNC, 0 );
				return false;
			}

			String last_modified = prop.getFirstNodeText("getlastmodified");
			if ( last_modified != null ) cv.put(ResourceTableManager.LAST_MODIFIED, last_modified);

			String content_type = prop.getFirstNodeText("getcontenttype");
			if ( content_type != null ) cv.put(ResourceTableManager.CONTENT_TYPE, content_type);

			String data = prop.getFirstNodeText("calendar-data");
			if ( data != null ) {
				cv.put(ResourceTableManager.RESOURCE_DATA, data); 
				cv.put(ResourceTableManager.ETAG, etag);
				cv.put(ResourceTableManager.NEEDS_SYNC, 0 );
				if ( Constants.LOG_VERBOSE ) Log.v(TAG,"Got data now, so no need to sync later.");
				return true;
			}
			if ( Constants.LOG_VERBOSE ) Log.v(TAG,"Need to sync "+cv.getAsString(ResourceTableManager.RESOURCE_NAME));
			cv.put(ResourceTableManager.NEEDS_SYNC, 1 );
		}

		// We remove our references to this now, since we've finished with it.
		responseNode.getParent().removeSubTree(responseNode);

		return true;
	}

	public boolean equals(Object that) {
		if ( this == that ) return true;
		if ( !(that instanceof RRInitialCollectionSync) ) return false;
		RRInitialCollectionSync thatCis = (RRInitialCollectionSync)that;
		if ( this.collectionPath == null && thatCis.collectionPath == null ) return true;
		if ( this.collectionPath == null || thatCis.collectionPath == null ) return false;
		return (
				this.collectionPath.equals(thatCis.collectionPath) &&
				this.serverId == thatCis.serverId
		);
	}
	
	private void removeDuplicates(Map<String,ContentValues> db, Map<String,ContentValues> server) {
		String[] names = new String[server.size()];
		server.keySet().toArray(names);
		for (String name : names) {
			if (!db.containsKey(name)) continue;	//New value from server
			if ( db.get(name).getAsString(ResourceTableManager.ETAG) != null && server.get(name).getAsString(ResourceTableManager.ETAG) != null
					&& db.get(name).getAsString(ResourceTableManager.ETAG).equals(server.get(name).getAsString(ResourceTableManager.ETAG)))  {
				//records match, remove from both
				server.remove(name);
			}
			else {
				server.get(name).put(ResourceTableManager.RESOURCE_ID, db.get(name).getAsString(ResourceTableManager.RESOURCE_ID));	//record to be updated. Insert ID
			}
			db.remove(name);
		}
	}
}
