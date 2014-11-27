package com.morphoss.acal.database.resourcesmanager;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.PriorityBlockingQueue;

import android.content.ContentQueryMap;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.Process;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.database.DMAction;
import com.morphoss.acal.database.DMDeleteQuery;
import com.morphoss.acal.database.DMInsertQuery;
import com.morphoss.acal.database.DMQueryBuilder;
import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.DMUpdateQuery;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.DatabaseTableManager;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.resourcesmanager.requesttypes.BlockingResourceRequest;
import com.morphoss.acal.database.resourcesmanager.requesttypes.BlockingResourceRequestWithResponse;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyBlockingRequestWithResponse;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyResourceRequest;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ResourceRequest;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.ServiceJob;
import com.morphoss.acal.service.SyncChangesToServer;
import com.morphoss.acal.service.WorkerClass;

public class ResourceManager implements Runnable {
	// The current instance
	private static ResourceManager instance = null;

	@SuppressWarnings("unused")
	public static boolean DEBUG = false && Constants.DEBUG_MODE;

	private volatile int numReadsProcessing = 0;

	// Get an instance
	public synchronized static ResourceManager getInstance(Context context) {
		if (instance == null)
			instance = new ResourceManager(context);
		return instance;
	}

	public static final String TAG = "aCal ResourceManager";

	// get and instance and add a callback handler to receive notfications of
	// change
	// It is vital that classes remove their handlers when terminating
	public synchronized static ResourceManager getInstance(Context context,
			ResourceChangedListener listener) {
		if (instance == null)
			instance = new ResourceManager(context);
		instance.addListener(listener);
		return instance;
	}

	// Request Processor Instance
	// Instance
	private ResourceTableManager RPinstance;

	private ResourceTableManager getRPInstance() {
		if (RPinstance == null)
			RPinstance = new ResourceTableManager();
		return RPinstance;
	}

	private Context context;

	// ThreadManagement
	private ConditionVariable threadHolder = new ConditionVariable();
	private Thread workerThread;
	private boolean running = true;
	private final ConcurrentLinkedQueue<ResourceRequest> writeQueue = new ConcurrentLinkedQueue<ResourceRequest>();
	private final PriorityBlockingQueue<ReadOnlyResourceRequest> readQueue = new PriorityBlockingQueue<ReadOnlyResourceRequest>();

	/**
	 * IMPORTANT INVARIANT:
	 * listeners should only ever be told about changes by the worker thread calling dataChanged in the enclosed class.
	 * 
	 * Notifying listeners in any other way can lead to Race Conditions.
	 */
	private final CopyOnWriteArraySet<ResourceChangedListener> listeners = new CopyOnWriteArraySet<ResourceChangedListener>();

	private ResourceManager(Context context) {
		this.context = context;
		threadHolder.close();
		workerThread = new Thread(this);
		workerThread.start();
	}

	public void addListener(ResourceChangedListener ccl) {
		synchronized (listeners) {
			this.listeners.add(ccl);
		}
	}

	public void removeListener(ResourceChangedListener ccl) {
		synchronized (listeners) {
			this.listeners.remove(ccl);
		}
	}

	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND );
		while (running) {
			// do stuff
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Thread Opened...");

			while ( !readQueue.isEmpty() || !writeQueue.isEmpty() ){

				//process reads first
				if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,readQueue.size()+" items in read queue, "+writeQueue.size()+" items in write queue");

				if (!readQueue.isEmpty()) {
					//Tell the processor that we are about to send a bunch of reads.
					Log.i(TAG, "Begin a set of read queries.");
					getRPInstance().openReadQuerySet();
					
					//Start all read processes
					while (!readQueue.isEmpty()) {
						if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,readQueue.size()+" items in read queue.");
						final ReadOnlyResourceRequest request = readQueue.poll();
						if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Processing Read Request: "+request.getClass());
						this.numReadsProcessing++;
						try {
							new Thread(new Runnable() {
								public void run() {
									try {
										getRPInstance().processRead(request);
									} catch (Exception e) {
										Log.e(TAG, "Error processing read request: "+Log.getStackTraceString(e));
									}
								}
							}).start();
						} catch (Exception e) {
							Log.e(TAG, "Error processing read request: "+Log.getStackTraceString(e));
						}
					}

					//Wait until all processes have finished
					while (this.numReadsProcessing > 0) {
						try { Thread.sleep(10);	} catch (Exception e) {	}
					}

					//tell processor that we are done
					Log.i(TAG, "End the set of read queries.");
					getRPInstance().closeReadQuerySet();
					

				}
				else {
					//process writes
					CacheManager.setResourceInTx(context, true);
					while (!writeQueue.isEmpty()) {
						if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,writeQueue.size()+" items in write queue.");
						final ResourceRequest request = writeQueue.poll();
						if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Processing Write Request: "+request.getClass());
						try {
							getRPInstance().process(request);
						} catch (Exception e) {
							Log.e(TAG, "Error processing write request: "+Log.getStackTraceString(e));
						}
					}
					CacheManager.setResourceInTx(context, false);
				}
			}
			// do stuff
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Finished processing, closing & blocking.");

			// Wait till next time
			threadHolder.close();
			threadHolder.block();
		}

	}

	/**
	 * Ensures that this classes closes properly. MUST be called before it is
	 * terminated
	 */
	public synchronized void close() {
		this.running = false;
		// Keep waking worker thread until it dies
		while (workerThread.isAlive()) {
			threadHolder.open();
			Thread.yield();
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
		}
		instance = null;
	}

	// Request handlers
	public void sendRequest(ResourceRequest request) {
		if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,
				"Received Write Request: "+request.getClass());
		writeQueue.offer(request);
		threadHolder.open();
	}

	private void offerAndBlockUntilProcessed(BlockingResourceRequest request) {
		threadHolder.open();
		int priority = Process.getThreadPriority(Process.myTid());
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		while (!request.isProcessed()) {
			try { Thread.sleep(10); } catch (Exception e) {	}
		}
		Process.setThreadPriority(priority);
	}
	
	public void sendBlockingRequest(BlockingResourceRequest request) {
		if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Received Write Request: "+request.getClass());
		writeQueue.offer(request);
		offerAndBlockUntilProcessed(request);
	}

	public <E> ResourceResponse<E> sendBlockingRequest(BlockingResourceRequestWithResponse<E> request) {
		if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Received Blocking Request: "+request.getClass());
		writeQueue.offer(request);
		offerAndBlockUntilProcessed(request);
		return request.getResponse();
	}

	// Request handlers
	public void sendRequest(ReadOnlyResourceRequest request) {
		if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Received Read Request: "+request.getClass());
		readQueue.offer(request);
		threadHolder.open();
	}

	public <E> ResourceResponse<E> sendBlockingRequest(ReadOnlyBlockingRequestWithResponse<E> request) {
		if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Received Blocking Read Request: "+request.getClass());
		readQueue.offer(request);
		threadHolder.open();
		int priority = Process.getThreadPriority(Process.myTid());
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		while (!request.isProcessed()) {
			try { Thread.sleep(10); } catch (Exception e) {	}
		}
		Process.setThreadPriority(priority);
		return request.getResponse();
	}


	public interface WriteableResourceTableManager extends ReadOnlyResourceTableManager {
		public long insert(String nullColumnHack, ContentValues values);
		public int update(ContentValues values, String whereClause,	String[] whereArgs);
		
		public void deleteByCollectionId(long id);
		public void deleteInvalidCollectionRecord(long collectionId);
		public void deletePendingChange(long pendingId);
		public long addPending(long l, long m, String oldBlob, String newBlob, String uid);
		public void updateCollection(long collectionId, ContentValues collectionData);
		
		public DMQueryList getNewQueryList();
		public DMDeleteQuery getNewDeleteQuery(String whereClause, String[] whereArgs);
		public DMInsertQuery getNewInsertQuery(String nullColumnHack, ContentValues values);
		public DMUpdateQuery getNewUpdateQuery(ContentValues values, String whereClause, String[] whereArgs);
		public DMQueryBuilder getNewQueryBuilder();
		public boolean processActions(DMQueryList queryList);
		
		public boolean doSyncListAndToken(DMQueryList newChangeList, long collectionId, String syncToken);
		public boolean syncToServer(DMAction action, long resourceId, long pendingId);
		
		
	}

	public interface ReadOnlyResourceTableManager {
		public void process(ResourceRequest r);
		
		public ArrayList<ContentValues> query(String[] columns, String selection, String[] selectionArgs,
				String groupBy, String having, String orderBy);

		public Map<String, ContentValues> contentQueryMap(String selection, String[] selectionArgs);
		
		public ContentValues getResource(long rid);
		public ContentValues getResourceInCollection(long collectionId,	String name);
		public ArrayList<ContentValues> getPendingResources();
		public Context getContext();
		
		//These should provide a similar interface to this class at some point.
		public ContentValues getServerRow(int serverId);
		public ContentValues getCollectionRow(long collectionId);
		
	}

	// This special class provides encapsulation of database operations as its
	// set up to enforce Scope. I.e. ONLY ResourceManager can start a request
	public class ResourceTableManager extends DatabaseTableManager implements WriteableResourceTableManager, ReadOnlyResourceTableManager {

		// Resources Table Constants
		public static final String RESOURCE_DATABASE_TABLE = "dav_resource";
		public static final String RESOURCE_ID = "_id";
		public static final String COLLECTION_ID = "collection_id";
		public static final String RESOURCE_NAME = "name";
		public static final String ETAG = "etag";
		public static final String LAST_MODIFIED = "last_modified";
		public static final String CONTENT_TYPE = "content_type";
		public static final String RESOURCE_DATA = "data";
		public static final String NEEDS_SYNC = "needs_sync";
		public static final String EARLIEST_START = "earliest_start";
		public static final String LATEST_END = "latest_end";
		public static final String EFFECTIVE_TYPE = "effective_type";

		public static final String IS_PENDING = "is_pending";	//this is a quasi field that tells use weather a resource came from the pending
		//table or the resource table


		//PendingChanges Table Constants
		//Table Fields - All other classes should use these constants to access fields.
		public static final String		PENDING_DATABASE_TABLE		= "pending_change";
		public static final String		PENDING_ID					= "_id";
		public static final String		PEND_COLLECTION_ID			= "collection_id";
		public static final String		PEND_RESOURCE_ID			= "resource_id";
		public static final String		OLD_DATA					= "old_data";
		public static final String		NEW_DATA					= "new_data";
		public static final String		UID							= "uid";


		public static final String TYPE_EVENT = "'VEVENT'";
		public static final String TYPE_TASK = "'VTODO'";
		public static final String TYPE_JOURNAL = "'VJOURNAL'";
		public static final String TYPE_ADDRESS = "'VCARD'";

		public static final String TAG = "aCal ResourceTableManager";


		private ResourceTableManager() {
			super(ResourceManager.this.context);
		}


		public void processRead(ReadOnlyResourceRequest request) {
			try {
				request.process(this);
				this.yield();
			} catch (ResourceProcessingException e) {
				Log.e(TAG, "Error Processing Resource Request: "
						+ Log.getStackTraceString(e));
			} catch (Exception e) {
				Log.e(TAG,
						"INVALID TERMINATION while processing Resource Request: "
						+ Log.getStackTraceString(e));
			} finally {
				numReadsProcessing--;
			}
		}

		@Override
		protected String getTableName() {
			return RESOURCE_DATABASE_TABLE;
		}

		public void process(ResourceRequest r) {
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Begin Processing");
			try {
				r.process(this);
			} catch (Exception e) {
				Log.e(TAG, "Exception while processing Resource Request: " + Log.getStackTraceString(e));
			} finally {
				// make sure db was closed properly
				if (this.db != null) {
					if ( this.inTx ) {
						try { this.endTx(); } catch(Exception e) {};
					}
					Log.e(TAG, "INVALID TERMINATION while processing Resource Request: Database not closed by "+r.getClass().getSimpleName()+"!");
					try { closeDB(); } catch (Exception e) { }
				}
				r.setProcessed();
			}
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG,"End Processing");
		}


		/**
		 * Method to retrieve a particular database row for a given resource ID.
		 */
		public ContentValues getResource(long rid) {
			
			ArrayList<ContentValues> res = this.query( null, RESOURCE_ID + " = ?",	new String[] { rid + "" }, null, null, null);
			if (res == null || res.isEmpty()) return null;
			return res.get(0);
		}

		public Context getContext() {
			return context;
		}

		private ContentValues preProcessValues(ContentValues values) {
			ContentValues toWrite = new ContentValues(values);
			if (toWrite.containsKey(IS_PENDING)) toWrite.remove(IS_PENDING);
			values = toWrite;
			String effectiveType = null;
			String resourceData = values.getAsString(RESOURCE_DATA); 
			if ( resourceData != null ) {
				try {

					VComponent comp = VComponent.createComponentFromBlob(resourceData);

					if ( comp == null ) {
						Log.w(TAG, "Unable to parse VComponent from:\n"+resourceData);
					}
					else {
						effectiveType = comp.getEffectiveType();

						if (comp instanceof VCalendar) {
							AcalDateRange range = ((VCalendar)comp).getInstancesRange();
							if ( range.start != null )
								values.put(EARLIEST_START, range.start.getMillis());
							else
								values.putNull(EARLIEST_START);
							if ( range.end != null )
								values.put(LATEST_END, range.end.getMillis());
							else
								values.putNull(LATEST_END);
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "Error updating VComponent from resource: "+Log.getStackTraceString(e));
				}
			}
			if ( effectiveType != null ) values.put(EFFECTIVE_TYPE, effectiveType);
			return values;
		}

		/**
		 * This override is important to ensure earliest start and latest end are always set
		 */
		@Override
		public long insert(String nullColumnHack, ContentValues values) {
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Resource Insert Begin");
			return super.insert(nullColumnHack, preProcessValues(values));
		}

		/**
		 * This override is important to ensure earliest start and latest end are always set
		 */
		public int update(ContentValues values, String whereClause,	String[] whereArgs) {
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Resource Update Begin");
			return super.update(preProcessValues(values), whereClause, whereArgs);
		}

		/**
		 * Static method to retrieve a particular database row for a given
		 * collectionId & resource name.
		 * 
		 * @param collectionId
		 * @param name
		 * @param contentResolver
		 * @return A ContentValues which is the dav_resource row, or null
		 */
		public ContentValues getResourceInCollection(long collectionId,	String name) {
			ArrayList<ContentValues> res = this.query(  null, RESOURCE_NAME + "=? AND "+COLLECTION_ID+"=?",
					new String[] { name, Long.toString(collectionId) }, null, null, null);
			if (res == null || res.isEmpty()) return null;
			return res.get(0);
		}

		/**
		 * Provides a content query map for legacy classes.
		 * 
		 * @Deprecated
		 */
		@Deprecated
		public Map<String, ContentValues> contentQueryMap(String selection, String[] selectionArgs) {
			Map<String, ContentValues> result = null;

			boolean openedInternally = doWeNeedADatabase(OPEN_READ);

			Cursor mCursor = db.query( RESOURCE_DATABASE_TABLE, null, selection, selectionArgs, null, null, null);
			try {
				ContentQueryMap cqm = new ContentQueryMap(mCursor, ResourceTableManager.RESOURCE_NAME, false, null);
				this.yield();
				cqm.requery();
				this.yield();
				result = cqm.getRows();
				this.yield();
			}
			catch( Exception e ) {
				Log.i(TAG,Log.getStackTraceString(e));
			}
			finally {
				if ( mCursor != null && !mCursor.isClosed() ) mCursor.close();
				if ( openedInternally ) closeDB();
			}
			return result;
		}
	
		public void deleteByCollectionId(long id) {
			if ( ResourceManager.DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, ResourceManager.TAG, 
					"Deleting resources for collection "+id);

			boolean openedInternally = false;
			if ( db == null ) {
				openedInternally = true;
				openDB(OPEN_WRITE);
			}
			try {
				this.beginTx();
				db.delete(PENDING_DATABASE_TABLE, PEND_COLLECTION_ID+" = ?", new String[]{id+""});
				delete(COLLECTION_ID + " = ?", new String[] { id + "" });
				this.setTxSuccessful();
			}
			catch( Exception e) {
				Log.e(TAG,"Error deleting resources for collection "+id, e);
			}
			finally {
				try {
					this.endTx();
				}
				catch( Exception e ) {}
				if ( openedInternally ) closeDB();
			}
		}

		public boolean doSyncListAndToken(DMQueryList newChangeList, long collectionId, String syncToken) {

			boolean success = false;
			boolean openedInternally = doWeNeedADatabase(OPEN_WRITE);
			boolean transactionInternally = doWeNeedATransaction();
			try {
				success = this.processActions(newChangeList);
	
				if ( success && syncToken != null ) {
					//Update sync token
					ContentValues cv = new ContentValues();
					cv.put(DavCollections.SYNC_TOKEN, syncToken);
					db.update(DavCollections.DATABASE_TABLE, cv,
							DavCollections._ID+"=?", new String[] {collectionId+""});
				}
			}
			catch( Exception e) {
				Log.e(TAG,"Error updating synced resources for collection "+collectionId, e);
			}
			finally {
				if ( transactionInternally ) {
					if ( success ) setTxSuccessful();
					endTx();
				}
				if ( openedInternally ) closeDB();
			}
			return success;
		}

		public boolean syncToServer(DMAction action, long resourceId, long pendingId) {
			boolean openedInternally = doWeNeedADatabase(OPEN_WRITE);
			boolean transactionInternally = doWeNeedATransaction();
			try {
				action.process(this);

				// We can retire this change now
				int removed = db.delete(PENDING_DATABASE_TABLE, PENDING_ID+"="+pendingId, null);
	
				if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, 
						"Deleted "+removed+" pending_change record ID="+pendingId+" for resourceId="+resourceId);

			}
			catch( Exception e) {
				Log.e(TAG,"Error syncing resource "+resourceId+" for pending change "+pendingId+"\n  Action: "+action, e);
			}
			finally {
				if ( transactionInternally ) {
					setTxSuccessful();
					endTx();
				}
				if ( openedInternally ) closeDB();
			}
			return true;

		}

		//Never ever ever ever call resourceChanged on listeners anywhere else.
		@Override
		public void dataChanged(ArrayList<DataChangeEvent> changes) {
			if (changes.isEmpty()) return;
			synchronized (listeners) {
				for (ResourceChangedListener listener : listeners) {
					ResourceChangedEvent rce = new ResourceChangedEvent(new ArrayList<DataChangeEvent>(changes));
					listener.resourceChanged(rce);
				}
			}
		}

		public ContentValues getServerRow(int serverId) {
			return Servers.getRow(serverId, context.getContentResolver());
		}

		public void deleteInvalidCollectionRecord(long collectionId) {
			context.getContentResolver().delete(Uri.withAppendedPath(DavCollections.CONTENT_URI,Long.toString(collectionId)), null, null);
		}

		public ContentValues getCollectionRow(long collectionId) {
			return DavCollections.getRow(collectionId, context.getContentResolver());
		}
		
		public void deletePendingChange(long pendingId) {
			boolean openedInternally = doWeNeedADatabase(OPEN_WRITE);
			try {
				int count = db.delete(PENDING_DATABASE_TABLE, PENDING_ID+" = ?", new String[]{pendingId+""});
				if ( DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
						"Deleted "+count+" pending change records with ID "+pendingId);
			}
			finally {
				if ( openedInternally ) closeDB();
			}
		}

		
		public void updateCollection(long collectionId, ContentValues collectionData) {
			boolean openedInternally = doWeNeedADatabase(OPEN_WRITE);
			try {
				db.update(DavCollections.DATABASE_TABLE, collectionData, DavCollections._ID+" =?", new String[]{collectionId+""});
			}
			finally {
				if ( openedInternally ) closeDB();
			}

		}

		@Override
		public DMDeleteQuery getNewDeleteQuery(String whereClause, String[] whereArgs) {
			return new DMDeleteQuery(whereClause, whereArgs);
		}

		@Override
		public DMInsertQuery getNewInsertQuery(String nullColumnHack, ContentValues values) {
			return new DMInsertQuery(nullColumnHack, values);
		}

		@Override
		public DMQueryBuilder getNewQueryBuilder() {
			return new DMQueryBuilder();
		}

		@Override
		public DMQueryList getNewQueryList() {
			return new DMQueryList();
		}

		@Override
		public DMUpdateQuery getNewUpdateQuery(ContentValues values, String whereClause, String[] whereArgs) {
			return new DMUpdateQuery(values, whereClause, whereArgs);
		}


		@Override
		public long addPending(long cid, long rid, String oldBlob, String newBlob, String uid) {
			// add a new pending resource and return the resultant resource.
			// if oldBlob == null then this is a create not an edit
			QUERY_ACTION action = QUERY_ACTION.PENDING_RESOURCE;;
			Cursor mCursor = null;
			ContentValues newResource = new ContentValues();
			boolean openedInternally = doWeNeedADatabase(OPEN_WRITE);
			boolean transactionInternally = doWeNeedATransaction();
			boolean success = false;
			try {
				if (oldBlob == null || oldBlob.equals("")) {
					//Create New
					newResource.put(COLLECTION_ID, cid);
					newResource.put(RESOURCE_DATA, newBlob); // So that effective_type, earliest_start & latest_end get set correctly
					rid = this.insert(null, newResource);

					newResource = new ContentValues();
					newResource.put(PEND_COLLECTION_ID, cid);
					newResource.put(PEND_RESOURCE_ID, rid);
					newResource.putNull(OLD_DATA);
					newResource.put(NEW_DATA, newBlob);
					newResource.put(UID, uid);
					if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Inserting Pending Table row for new resource ID: "+rid);
					db.insert(PENDING_DATABASE_TABLE, null, newResource);
					
				}
				else {
					//Check if this resource already exists
					mCursor = db.query(PENDING_DATABASE_TABLE, null,  PEND_RESOURCE_ID+" = ? AND "+PEND_COLLECTION_ID+" = ?", 
							new String[]{rid+"",cid+""}, null,null,null);
					
					if (mCursor.getCount() >= 1) {
						//Update existing pending entry
						mCursor.moveToFirst();
						DatabaseUtils.cursorRowToContentValues(mCursor, newResource);
						newResource.put(NEW_DATA, newBlob);
						newResource.put(UID, uid);
						if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Updating Pending Table row for existing resource ID: "+rid);
						db.update(PENDING_DATABASE_TABLE, newResource,
									PEND_RESOURCE_ID+" = ? AND "+PEND_COLLECTION_ID+" = ?", 
									new String[] {rid+"",cid+""});
						
					} else {
						//create new pending entry from existing resource
						newResource.put(PEND_COLLECTION_ID, cid);
						newResource.put(PEND_RESOURCE_ID, rid);
						newResource.put(OLD_DATA, oldBlob);
						newResource.put(NEW_DATA, newBlob);
						newResource.put(UID, uid);
						if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Inserting Pending Table row for existing resource ID: "+rid);
						db.insert(PENDING_DATABASE_TABLE, null, newResource);
					}
					mCursor.close();
					
					
				}
				//trigger resource change event
				ContentValues toReport = Resource.fromContentValues(newResource).toContentValues();
				if ( newBlob == null )
					this.addChange(new DataChangeEvent(QUERY_ACTION.DELETE, toReport));
				else
					this.addChange(new DataChangeEvent(action, toReport));
				
				//done
				success = true;
			}
			catch ( Exception e ) {
				Log.e(TAG, "Error updating pending resource: ", e);
				success = false;
			}
			finally {
				if ( mCursor != null && !mCursor.isClosed() ) mCursor.close();
				if ( transactionInternally ) {
					if ( success ) setTxSuccessful();
					endTx();
				}
				if ( openedInternally ) closeDB();
			}
			
			ServiceJob syncChanges = new SyncChangesToServer();
			syncChanges.TIME_TO_EXECUTE = 2000;	// Wait two seconds before trying to sync to server.
			WorkerClass.getExistingInstance().addJobAndWake(syncChanges);
			
			return rid;
		}
		


		@Override
		public ArrayList<ContentValues> getPendingResources() {
			ArrayList<ContentValues> res = new ArrayList<ContentValues>();

			boolean openedInternally = doWeNeedADatabase(OPEN_READ);
			Cursor mCursor = null;
			try {
				// We need to explicitly specify all columns in this because otherwise we get the wrong _id
				// in the resulting join.
				mCursor = db.query(PENDING_DATABASE_TABLE+" JOIN "+RESOURCE_DATABASE_TABLE+
					" ON ("+
					PENDING_DATABASE_TABLE+"."+PEND_RESOURCE_ID+" = "+RESOURCE_DATABASE_TABLE+"."+RESOURCE_ID+
					")", new String[] {
						PENDING_DATABASE_TABLE+"."+PENDING_ID,
						PENDING_DATABASE_TABLE+"."+PEND_RESOURCE_ID,
						PENDING_DATABASE_TABLE+"."+PEND_COLLECTION_ID,
						PENDING_DATABASE_TABLE+"."+OLD_DATA,
						PENDING_DATABASE_TABLE+"."+NEW_DATA,
						PENDING_DATABASE_TABLE+"."+UID,
						RESOURCE_DATABASE_TABLE+"."+RESOURCE_NAME,
						RESOURCE_DATABASE_TABLE+"."+ETAG,
						RESOURCE_DATABASE_TABLE+"."+LAST_MODIFIED,
						RESOURCE_DATABASE_TABLE+"."+CONTENT_TYPE,
						RESOURCE_DATABASE_TABLE+"."+RESOURCE_DATA,
						RESOURCE_DATABASE_TABLE+"."+NEEDS_SYNC,
						RESOURCE_DATABASE_TABLE+"."+EARLIEST_START,
						RESOURCE_DATABASE_TABLE+"."+LATEST_END,
						RESOURCE_DATABASE_TABLE+"."+EFFECTIVE_TYPE
								
					}, null, null, null, null, null);
				this.yield();
				if (mCursor.getCount() > 0) {
					if ( ResourceManager.DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, 
							"Found "+mCursor.getCount()+" pending changes");
					for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
						ContentValues vals = new ContentValues();
						DatabaseUtils.cursorRowToContentValues(mCursor, vals);
						res.add(vals);
						this.yield();
					}
						
				}
			}
			finally {
				if ( mCursor != null ) mCursor.close();

				if ( openedInternally ) closeDB();
			}
			return res;
		}
	}
}