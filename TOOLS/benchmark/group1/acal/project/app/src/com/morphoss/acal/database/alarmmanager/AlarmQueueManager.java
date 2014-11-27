package com.morphoss.acal.database.alarmmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.ConditionVariable;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.activity.AlarmActivity;
import com.morphoss.acal.database.AcalDBHelper;
import com.morphoss.acal.database.DMInsertQuery;
import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.DatabaseTableManager;
import com.morphoss.acal.database.alarmmanager.requests.ARResourceChanged;
import com.morphoss.acal.database.alarmmanager.requesttypes.AlarmRequest;
import com.morphoss.acal.database.alarmmanager.requesttypes.AlarmResponse;
import com.morphoss.acal.database.alarmmanager.requesttypes.BlockingAlarmRequest;
import com.morphoss.acal.database.alarmmanager.requesttypes.BlockingAlarmRequestWithResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedEvent;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedListener;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRGetUpcomingAlarms;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VComponentCreationException;

/**
 * This manager manages the Alarm Database Table(s). It will listen to changes to resources and update the DB
 * automatically. AlarmRequests can be sent to query the table and to notify of changes in alarm state (e.g. dismiss/snooze)
 * 
 * @author Chris Noldus
 *
 */
public class AlarmQueueManager implements Runnable, ResourceChangedListener  {

	//The current instance
	private static AlarmQueueManager instance = null;
	public static final String TAG = "aCal AlarmQueueManager";

	//Get an instance
	public synchronized static AlarmQueueManager getInstance(Context context) {
		if (instance == null) instance = new AlarmQueueManager(context);
		return instance;
	}

	//get and instance and add a callback handler to receive notfications of change
	//It is vital that classes remove their handlers when terminating
	public synchronized static AlarmQueueManager getInstance(Context context, AlarmChangedListener listener) {
		if (instance == null) {
			instance = new AlarmQueueManager(context);
		}
		instance.addListener(listener);
		return instance;
	}

	private Context context;

	//ThreadManagement
	private ConditionVariable threadHolder = new ConditionVariable();
	private Thread workerThread;
	private boolean running = true;
	private final ConcurrentLinkedQueue<AlarmRequest> queue = new ConcurrentLinkedQueue<AlarmRequest>();
	
	//Meta Table Management
	private static Semaphore lockSem = new Semaphore(1, true);
	private static volatile boolean lockdb = false;
	private long metaRow = 0;
	//DB Constants
	private static final String META_TABLE = "alarm_meta";
	private static final String FIELD_ID = "_id";
	private static final String FIELD_CLOSED = "closed";

	
	
	//Comms
	private final CopyOnWriteArraySet<AlarmChangedListener> listeners = new CopyOnWriteArraySet<AlarmChangedListener>();
	private ResourceManager rm;
	
	//Request Processor Instance
	private AlarmTableManager ATMinstance;
	
	private AlarmTableManager getATMInstance() {
		if (instance == null) ATMinstance = new AlarmTableManager();
		return ATMinstance;
	}
	
	/**
	 * CacheManager needs a context to manage the DB. Should run under AcalService.
	 * Loadstate ensures that our DB is consistant and should be run before any resource
	 * modifications can be made by any other part of the system.
	 */
	private AlarmQueueManager(Context context) {
		this.context = context;
		this.ATMinstance = this.getATMInstance();
		rm = ResourceManager.getInstance(context);
		loadState();
		workerThread = new Thread(this);
		workerThread.start();

	}
	
	/**
	 * Add a lister to change events. Change events are fired whenever a change to the DB occurs
	 * @param ccl
	 */
	public void addListener(AlarmChangedListener ccl) {
		synchronized (listeners) {
			this.listeners.add(ccl);
		}
	}
	
	/**
	 * Remove an existing listener. Listeners should always be removed when they no longer require changes.
	 * @param ccl
	 */
	public void removeListener(AlarmChangedListener ccl) {
		synchronized (listeners) {
			this.listeners.remove(ccl);
		}
	}
	
	private synchronized static void acquireMetaLock() {
		try { lockSem.acquire(); } catch (InterruptedException e1) {}
		while (lockdb) try { Thread.sleep(10); } catch (Exception e) { }
		lockdb = true;
		lockSem.release();
	}
	
	private synchronized static void releaseMetaLock() {
		if (!lockdb) throw new IllegalStateException("Cant release a lock that hasnt been obtained!");
		lockdb = false;
	}

	
	/**
	 * Called on start up. if safe==false flush cache. set safe to false regardless.
	 */
	private void loadState() {
		acquireMetaLock();
		ContentValues data = new ContentValues();
		AcalDBHelper dbHelper = new AcalDBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		//load start/end range from meta table
		Cursor mCursor = db.query(META_TABLE, null, null, null, null, null, null);
		boolean wasClosedCleanly = false;
		try {
			if (mCursor.getCount() < 1) {
				Log.i(TAG, "Initializing cache for first use.");
			} else  {
				mCursor.moveToFirst();
				DatabaseUtils.cursorRowToContentValues(mCursor, data);
				wasClosedCleanly = StaticHelpers.toBoolean(data.getAsInteger(FIELD_CLOSED), false);
			}
		}
		catch( Exception e ) {
			Log.i(TAG,Log.getStackTraceString(e));
		}
		finally {
			if ( mCursor != null ) mCursor.close();
		}

		if ( !wasClosedCleanly ) {
			Log.i(TAG, "Rebuiliding alarm cache.");
			rebuild();
		}
		data.put(FIELD_CLOSED, 0);
		db.delete(META_TABLE, null, null);
		data.remove(FIELD_ID);
		this.metaRow = db.insert(META_TABLE, null, data);
		dbHelper.close(db);
		rm.addListener(this);
		releaseMetaLock();

	}
	
	
	/**
	 * MUST set SAFE to true or cache will be flushed on next load.
	 * Nothing should be able to modify resources after this point.
	 *
	 */
	private void saveState() {
		//save start/end range to meta table
		acquireMetaLock();
		ContentValues data = new ContentValues();
		data.put(FIELD_CLOSED, 1);

		AcalDBHelper dbHelper = new AcalDBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		//set CLOSED to true
		db.update(META_TABLE, data, FIELD_ID+" = ?", new String[] {metaRow+""});
		dbHelper.close(db);
		
		//dereference ourself so GC can clean up
		instance = null;
		this.ATMinstance = null;
		rm.removeListener(this);
		rm = null;
		releaseMetaLock();
	}
	
	/**
	 * Ensures that this classes closes properly. MUST be called before it is terminated
	 */
	public void close() {
		this.running = false;
		//Keep waking worker thread until it dies 
		while (workerThread.isAlive()) {
			threadHolder.open();
			Thread.yield();
			try { Thread.sleep(100); } catch (Exception e) { }
		}
		workerThread = null;
		saveState();
	}
	
	/**
	 * Forces AlarmManager to rebuild alarms from scratch. Should only be called if table has become invalid.
	 */
	private void rebuild() {
		ATMinstance.rebuild();
	}
	
	/**
	 * Method for responding to requests from activities.
	 */
	@Override
	public void run() {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		while (running) {
			//do stuff
			while (!queue.isEmpty()) {
				AlarmRequest request = queue.poll();
				ATMinstance.process(request);
			}
			//Wait till next time
			threadHolder.close();
			threadHolder.block();
		}
	}
	
	/**
	 * A resource has changed. we need to see if this affects our table
	 */
	@Override
	public void resourceChanged(ResourceChangedEvent event) {
		this.sendRequest(new ARResourceChanged(event));
	}
	
	/**
	 * Send a request to the AlarmManager. Requests are queued and processed a-synchronously. No guarantee is given as to 
	 * the order of processing, so if sending multiple requests, consider potential race conditions.
	 * @param request
	 * @throws IllegalStateException thrown if close() has been called.
	 */
	public void sendRequest(AlarmRequest request) throws IllegalStateException {
		if (instance == null || this.workerThread == null || this.ATMinstance == null) 
			throw new IllegalStateException("AM in illegal state - probably because sendRequest was called after close() has been called.");
		queue.offer(request);
		threadHolder.open();
	}
	
	public <E> AlarmResponse<E> sendBlockingRequest(BlockingAlarmRequestWithResponse<E> request) {
		queue.offer(request);
		threadHolder.open();
		int priority = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		while (!request.isProcessed()) {
			try { Thread.sleep(10); } catch (Exception e) {	}
		}
		Thread.currentThread().setPriority(priority);
		return request.getResponse();
	}
	public void sendBlockingRequest(BlockingAlarmRequest request) {
		queue.offer(request);
		threadHolder.open();
		int priority = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		while (!request.isProcessed()) {
			try { Thread.sleep(10); } catch (Exception e) {	}
		}
		Thread.currentThread().setPriority(priority);
	}
	
	
	public final class AlarmTableManager extends DatabaseTableManager {
		/**
		 * Generate a new instance of this processor.
		 * WARNING: Only 1 instance of this class should ever exist. If multiple instances are created bizarre 
		 * side affects may occur, including Database corruption and program instability
		 */
		
		private static final String TABLENAME = "alarms";
        public static final String FIELD_ID = "_id";
		public static final String FIELD_TIME_TO_FIRE = "ttf";
		public static final String FIELD_RID = "rid";
		public static final String FIELD_RRID = "rrid";
		public static final String FIELD_STATE ="state";
		public static final String FIELD_BLOB ="blob";
		
		//Change this to set how far back we look for alarms and database first time use/rebuild
		private static final int LOOKBACK_SECONDS = 4*60*60;		//default 4 hours
        
		private static final String TAG = "aCal AlarmQueueManager";
		
		private AlarmManager alarmManager;
		
		private AlarmTableManager() {
			super(AlarmQueueManager.this.context);
			alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		}

		public void process(AlarmRequest request) {
			long start = System.currentTimeMillis();
			if (Constants.debugAlarms) {
				Log.d(TAG,"Processing "+request.getClass()+": "+request.getLogDescription());
			}
			try {
				request.process(this);
			} catch (AlarmProcessingException e) {
				Log.e(TAG, "Error Processing Alarm Request: "+Log.getStackTraceString(e));
			} catch (Exception e) {
				Log.e(TAG, "INVALID TERMINATION while processing Alarm Request: "+Log.getStackTraceString(e));
			} finally {
				//make sure db was closed properly
				if (this.db != null) {
					if ( this.inTx ) this.endTx();
					Log.e(TAG, "INVALID TERMINATION while processing "+request.getClass().getSimpleName()+" Alarm Request: Database not closed!");
					try { closeDB(); } catch (Exception e) { }
				}
			}
			if (Constants.debugAlarms) {
				Log.d(TAG, "Processing of "+request.getClass()+" complete in "+(System.currentTimeMillis()-start)+"ms");
			}
		}

		@Override
		public void dataChanged(ArrayList<DataChangeEvent> changes) {
			AlarmChangedEvent event = new AlarmChangedEvent(changes);
			for (AlarmChangedListener acl : listeners) acl.alarmChanged(event);
		}

		@Override
		protected String getTableName() {
			return TABLENAME;
		}

	
		
		//Custom operations
		
		/**
		 * Wipe and rebuild alarm table - called if it has become corrupted.
		 */
		public void rebuild() {
			Log.i(TAG, "Clearing Alarm Cache of possibly corrupt data and rebuilding...");
			//Display up to the last x hours of alarms.
			AcalDateTime after = new AcalDateTime();
			after.addSeconds(-LOOKBACK_SECONDS);
			
			//Step 1 - request a list of all resources so we can find the next alarm trigger for each
			RRGetUpcomingAlarms request = new RRGetUpcomingAlarms(after);
			rm.sendBlockingRequest(request);
			ArrayList<AlarmRow> alarms = request.getResponse().result();
			int count = 0;
			//Create query List
			DMQueryList list = new DMQueryList();
			for (AlarmRow alarm : alarms) {
				list.addAction(new DMInsertQuery(
							null,
							alarm.toContentValues()
						));
				count++;
			}
			
			//step 2 - begin db transaction, delete all existing and insert new list
			super.openDB(OPEN_WRITE);
			super.beginTx();
			super.delete(null, null);
			super.processActions(list);
			super.setTxSuccessful();
			super.endTx();
			super.closeDB();
			Log.i(TAG, count+" entries added.");
			//step 3 schedule alarm intent
			scheduleAlarmIntent();
		}
		
		/**
		 * Get the next alarm to go off
		 * @return
		 */
		public AlarmRow getNextAlarm() {
			ArrayList<ContentValues> res = super.query(null, 
					FIELD_STATE +" = ? OR "+FIELD_STATE +" = ?", 
					new String[] {ALARM_STATE.PENDING.ordinal()+"", ALARM_STATE.SNOOZED.ordinal()+""} , 
					null, 
					null, 
					FIELD_TIME_TO_FIRE+" ASC");
			if (res.isEmpty()) return null;
			return AlarmRow.fromContentValues(res.get(0));
		}
		
		/**
		 * Get the next alarm that is overdue or null. If null, then schedule next alarm intent.
		 * @return
		 */
		public AlarmRow getNextDueAlarm() {
			ArrayList<ContentValues> res = super.query(null, 
					"("+FIELD_STATE +" = ? OR "+FIELD_STATE +" = ? ) AND "+FIELD_TIME_TO_FIRE+" < ?", 
					new String[] {ALARM_STATE.PENDING.ordinal()+"", ALARM_STATE.SNOOZED.ordinal()+"", System.currentTimeMillis()+""} , 
					null, 
					null, 
					FIELD_TIME_TO_FIRE+" ASC");
			if (res.isEmpty()) {
				this.scheduleAlarmIntent();
				return null;
			}
			return AlarmRow.fromContentValues(res.get(0));
		}
		
		/**
		 * Snooze/Dismiss a specific alarm
		 * @param alarm
		 */
		public void updateAlarmState(AlarmRow row, ALARM_STATE state) {
			//TODO add snooze capability - for now just dismiss.
			super.openDB(OPEN_WRITE);
			super.beginTx();
			
			//first remove any dismissed alarms
			super.delete(FIELD_STATE+" = ?", new String[]{ALARM_STATE.DISMISSED.ordinal()+""});
			
			//set alarm row to dismissed
			row.setState(ALARM_STATE.DISMISSED);
			
			//attempt update
			int res = super.update(row.toContentValues(), FIELD_ID+" = ?", new String[]{row.getId()+""});
			if (res >0) {
				//success
				super.setTxSuccessful();
			}
			super.endTx();
			super.closeDB();
			
			
			//Reschedule next intent.
			scheduleAlarmIntent();
		}
		

		
		/**
		 * Schedule the next alarm intent - Should be called whenever there is a change to the db.
		 */
		public void scheduleAlarmIntent() {
			AlarmRow next = getNextAlarm();
			if (next == null) return; //nothing to schedule.
			long ttf = next.getTimeToFire();
			Log.i(TAG, "Scheduled Alarm for "+ ((ttf-System.currentTimeMillis())/1000)+" Seconds from now.");
			Intent intent = new Intent(context, AlarmActivity.class);
			PendingIntent alarmIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
			alarmManager.set(AlarmManager.RTC_WAKEUP, ttf, alarmIntent);
			
		}

		//Deal with resource changes
		public void processChanges(ArrayList<DataChangeEvent> changes) {

			super.openDB(OPEN_WRITE);
			super.beginTx();
			try {
				for (DataChangeEvent change : changes) {
					this.yield();
					super.delete(FIELD_RID+" = ?", new String[]{change.getData().getAsLong(ResourceTableManager.RESOURCE_ID)+""});
					switch (change.action) {
						case INSERT:
						case UPDATE:
						case PENDING_RESOURCE:
							populateTableFromResource(change.getData());
							break;
						default: break;
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Error processing resource changes: "+e+"\n"+Log.getStackTraceString(e));
			}
			super.setTxSuccessful();
			super.endTx();
			super.closeDB();

			//schedule alarm intent
			scheduleAlarmIntent();

		}

		private void populateTableFromResource(ContentValues data) {
			if ( data == null
					|| VComponent.VCARD.equalsIgnoreCase(data.getAsString(ResourceTableManager.EFFECTIVE_TYPE))
					|| ( data.getAsString(ResourceTableManager.RESOURCE_DATA) == null
						&&  data.getAsString(ResourceTableManager.NEW_DATA) == null )
					) return;

			//default start timestamp
			AcalDateTime after = new AcalDateTime().applyLocalTimeZone();

			ArrayList<AlarmRow> alarmList = new ArrayList<AlarmRow>();
			//use last dismissed to calculate start
			ArrayList<ContentValues> cvs = super.query(null, FIELD_STATE+" = ?", new String[]{ALARM_STATE.DISMISSED.ordinal()+""}, null, null, FIELD_TIME_TO_FIRE+" DESC");
			if (!cvs.isEmpty()) after = AcalDateTime.fromMillis(cvs.get(0).getAsLong(FIELD_TIME_TO_FIRE));
			
			Resource r = Resource.fromContentValues(data);
			VCalendar vc;
			try {
				vc = (VCalendar) VComponent.createComponentFromResource(r);
			}
			catch ( ClassCastException e ) {
				return;
			}
			catch ( VComponentCreationException e ) {
				// @todo Auto-generated catch block
				Log.w(TAG,"Auto-generated catch block", e);
				return;
			}
			if ( vc == null ) {
				Log.w(TAG,"Couldn't create VCalendar from resource "+r.getResourceId()+":\n"+r.getBlob());
				return;
			}
			vc.appendAlarmInstancesBetween(alarmList, new AcalDateRange(after, AcalDateTime.addDays(after, 7)));
		
			Collections.sort(alarmList);
			
			//Create query List
			DMQueryList list = new DMQueryList();
			for (AlarmRow alarm : alarmList) {
				list.addAction(new DMInsertQuery(
							null,
							alarm.toContentValues()
						));
			}
			
			super.processActions(list);

		}
	}


	
}
