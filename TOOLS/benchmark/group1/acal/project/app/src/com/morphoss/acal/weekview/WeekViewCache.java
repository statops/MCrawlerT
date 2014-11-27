package com.morphoss.acal.weekview;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentValues;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.CacheModifier;
import com.morphoss.acal.database.CacheWindow;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.cachemanager.CacheChangedEvent;
import com.morphoss.acal.database.cachemanager.CacheChangedListener;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.database.cachemanager.CacheResponseListener;
import com.morphoss.acal.database.cachemanager.requests.CRObjectsInWindow;
import com.morphoss.acal.database.cachemanager.requests.CRObjectsInWindow.CRObjectsInWindowResponse;

/**
 * This class provides an in memory cache of event data for WeekView to prevent unnecessarily making cache requests. or
 * having to recalculate the same timetables.
 * 
 * It also has a handle on cache changes so that it can force the diplay to update if a collection changes.
 * 
 * @author Chris Noldus
 *
 */
public class WeekViewCache implements CacheChangedListener, CacheResponseListener<ArrayList<CacheObject>>, CacheModifier {

	private WeekViewDays callback;			//The view that is using this cache
	private CacheManager cm;				//CacheManager for getting data
	private CacheWindow window;
	//private int lastMaxX;					//Yucky coupling. Used by WeekViewDays in draw calculation
	
	//Cached Data
	//It is important that the Actual CacheWindow size is equivalent to the range covered by this cached data
	private SparseArray<ArrayList<WVCacheObject>> partDayEventsInRange = new SparseArray<ArrayList<WVCacheObject>>();
	private ArrayList<WVCacheObject> fullDayEventsInRange = new ArrayList<WVCacheObject>();
	private ArrayList<WVCacheObject> HSimpleList;  	//The last list of (simple) events for the header
	private WeekViewTimeTable HTimetable;  			//The last timetable used for the header
	private SparseArray<WeekViewTimeTable> DTimetables = new SparseArray<WeekViewTimeTable>();  //Cached timetables for each day

	private static final int HANDLE_SAVE_NEW_DATA = 1;
	private static final int HANDLE_RESET = 2;
	private static final String TAG = "aCal WeekViewCache";
	
	//Cache Window settings
	private final long lookForward = 86400000L*7L*10L;	//4 weeks
	private final long lookBack = 86400000L*7L*5L;	//1 week
	private final long maxSize = 86400000L*7L*26L;	//10 weeks
	private final long minPaddingForward = 86400000L*7L*5L;	//1 weeks
	private final long minPaddingBack = 86400000L*3L;	//3 days
	private final long increment = 86400000L*7L*5L;	//4 weeks


	private static WeekViewCache hContext = null;
	/**
	 * Handler for processing cache responses in GUI Thread
	 */
	private static Handler mHandler = new Handler() {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case HANDLE_SAVE_NEW_DATA:
					//Called when we have a cache response
					hContext.copyNewDataToDayMap((SparseArray<ArrayList<WVCacheObject>>)((Object[])msg.obj)[0]);
					hContext.copyNewDataToEventsInRqange((ArrayList<WVCacheObject>)((Object[])msg.obj)[1]);
					hContext.updateWindowToIncludeConcreteRange((AcalDateRange)((Object[])msg.obj)[2]);
					hContext.callback.requestRedraw();
					break;
				case HANDLE_RESET: {
					//Called when we have a cache change.
					hContext.resetCache();
				}
			}
		}
	};
	
	/** Constructor */
	public WeekViewCache(Context context, WeekViewDays callback) {
		this.callback = callback;
		cm = CacheManager.getInstance(context, this);
		window = new CacheWindow(lookForward, lookBack, maxSize, minPaddingBack,
				minPaddingForward, increment, this, new AcalDateTime());
	}
	
	/** must be called when no longer needed to prevent memory hole */
	public void close() {
		cm.removeListener(this);
	}

	/**
	 * Send a request for data in the specified range
	 * @param range
	 */
	private void loadDataForRange(AcalDateRange range) {
		window.addToRequestedRange(range);
		cm.sendRequest(new CRObjectsInWindow(this));
	}
	
	/**
	 * Handle Cache changes. At this point we don't really handle these properly. 
	 */
	@Override
	public void cacheChanged(CacheChangedEvent event) {
	
		/**
		 * TODO: resetting the cache like this causes ugly blinking crappiness.  For each of the incoming
		 * changes we need actually update the cache.  No more, no less.
		 * 
		 * Until we do that, just comment this out, since continuing to display the current data
		 * is the right thing in almost all cases.  In particular (e.g.) when we sync due to a birthday
		 * it's earliestStart/latestEnd almost certainly crosses this period, even though no instances
		 * occur within the visible range. 
		 */
		if ( event.getChanges().size() > 10000 ) return; // always return

		// 1 Check if any of the changes affect the current range
		AcalDateTime earliestStart = null;
		AcalDateTime latestEnd = null;
		for ( DataChangeEvent e : event.getChanges() ) {
			ContentValues cv = e.getData();
			CacheObject co = CacheObject.fromContentValues(cv);
			if (earliestStart == null) earliestStart = co.getStartDateTime();
			else if (co.getStartDateTime() != null && co.getStartDateTime().before(earliestStart)) earliestStart = co.getStartDateTime();
		
			if (latestEnd == null) latestEnd = co.getStartDateTime();
			else if (co.getEndDateTime() != null && co.getEndDateTime().after(latestEnd)) latestEnd = co.getEndDateTime();
		}

		// 2 if so, wipe existing data
		if (new AcalDateRange(earliestStart,latestEnd).overlaps(window.getCurrentWindow())) {
			// resetCache();
		}
	}
	
	/**
	 * Handle responses from the cache
	 */
	@Override
	public void cacheResponse(CacheResponse<ArrayList<CacheObject>> response) {
		CRObjectsInWindowResponse<ArrayList<CacheObject>> resobject = (CRObjectsInWindowResponse<ArrayList<CacheObject>>) response;
		
		ArrayList<WVCacheObject> fullDay = new ArrayList<WVCacheObject>();
		SparseArray<ArrayList<WVCacheObject>> dayMap = new SparseArray<ArrayList<WVCacheObject>>();
		
		for (CacheObject co: response.result()) {
			try {
				if ( co.getStartDateTime() == null ) continue;  // TODO / JOURNAL may not have a start.
				if (co.isAllDay()) fullDay.add(new WVCacheObject(co));
				else {
					int day = (int)(co.getStartDateTime().getYearDay());
					ArrayList<WVCacheObject> dayList = null;
					if (dayMap.get(day) == null ) {
						dayList = new ArrayList<WVCacheObject>();
					}
					else {
						dayList = dayMap.get(day);
					}
					dayList.add(new WVCacheObject(co));
					dayMap.put(day, dayList);
				}
			}
			catch( Exception e ) {
				co.logInvalidObject(callback.getContext(), TAG, e);
			}
		}
		Object[] obj = new Object[]{dayMap,fullDay,resobject.rangeRetreived()};
	
		hContext = this;
		mHandler.sendMessage(mHandler.obtainMessage(HANDLE_SAVE_NEW_DATA, obj));
	}
	
	//these 2 methods are in effect a continuation of the above code, however they will be run by the GUI thread.
	private void copyNewDataToEventsInRqange(ArrayList<WVCacheObject> arrayList) {
		boolean change = false;
		for (WVCacheObject wvco : arrayList) {
			if (!this.fullDayEventsInRange.contains(wvco)) {
				this.fullDayEventsInRange.add(wvco);
				change = true;
			}
		}
		if (change) {
			HSimpleList = null;
			HTimetable = null;
		}
	}

	private void copyNewDataToDayMap(SparseArray<ArrayList<WVCacheObject>> hashMap) {
		int key = 0;
		for(int i = 0; i < hashMap.size(); i++) {
			key = hashMap.keyAt(i);
			if (this.partDayEventsInRange.get(key) != null ) {
				this.partDayEventsInRange.remove(key);
				this.DTimetables.remove(key);
			}
			this.partDayEventsInRange.put(key, hashMap.get(key));
		}
	}
	
	/**
	 * Updates the concrete range of the window to include the given range. called on CacheResponse
	 * @param range
	 */
	private void updateWindowToIncludeConcreteRange(AcalDateRange range) {
		if (range == null) return;
		window.expandWindow(range);
	}
	
	public void resetCache() {
		window = new CacheWindow(lookForward, lookBack, maxSize, minPaddingBack,
				minPaddingForward, increment, this, new AcalDateTime());
		DTimetables.clear();
		HTimetable = null;
		HSimpleList = null;
		partDayEventsInRange.clear();
		fullDayEventsInRange.clear();
		callback.requestRedraw();
	}
	
	public CacheWindow getWindow() {
		return this.window;
	}

	
	/**
	 * Calculates a timetable of rows to place horizontal (multi-day) events in order
	 * that the events do not overlap one other.
	 * @param range A one-dimensional array of events
	 * @return A two-dimensional array of events
	 */
	public WeekViewTimeTable getMultiDayTimeTable(AcalDateRange range, long HDepth) {
		calcMultiDayTimeTable(range,HDepth);
		return HTimetable;
		
	}
	
	private void calcMultiDayTimeTable(AcalDateRange range, long HDepth) {
		//first check to see if we even have the requested range
		if (!window.isWithinWindow(range)) {
			this.loadDataForRange(range);
			HTimetable = new WeekViewTimeTable(new ArrayList<WVCacheObject>(), true);
			return;
		}

		//HST & HET define the range
		//HDepth is precalculated
		
		//first we need to construct a list of WVChacheObjects for the requested range
		ArrayList<WVCacheObject> eventsForMultiDays = new ArrayList<WVCacheObject>();
		
		for (WVCacheObject wvo : this.fullDayEventsInRange) if (wvo.getRange().overlaps(range)) eventsForMultiDays.add(wvo);
		
		//List<EventInstance> events = context.getEventsForDays(range, WeekViewActivity.INCLUDE_ALL_DAY_EVENTS );
		if (HTimetable != null && HSimpleList != null) {
			if (HSimpleList.containsAll(eventsForMultiDays) && eventsForMultiDays.size() == HSimpleList.size()) return;
		}
		HSimpleList = eventsForMultiDays;
		Collections.sort(HSimpleList);

		HTimetable = new WeekViewTimeTable(eventsForMultiDays, true);
		
	}

	/**
	 * Retreives the timetable for the specified day. returns null if there is none.
	 * @param day to do the timetable for
	 * @return An array of lists, one per column
	 */
	public WeekViewTimeTable getInDayTimeTable(AcalDateTime currentDay) {
		int day = (int)(currentDay.getYearDay());
		//first lets check to see if we already have this timetable
		if ( DTimetables.get(day) != null ) return this.DTimetables.get(day);
		//nope? then calculate it
		calcInDayTimeTable(currentDay);
		return this.DTimetables.get(day);
	}
	
	private void calcInDayTimeTable(AcalDateTime currentDay) {
		int day = (int)(currentDay.getYearDay());
		AcalDateTime dayStart = currentDay.clone().setDaySecond(0);
		AcalDateTime dayEnd = dayStart.clone().addDays(1);
		AcalDateRange range = new AcalDateRange(dayStart,dayEnd);
		
		//first check to see if we even have the requested range
		if (!window.isWithinWindow(range)) {
			this.loadDataForRange(range);
			return;
		}
		
		//first we need to construct a list of WVChacheObjects for the requested range
		ArrayList<WVCacheObject> eventsForDays = this.partDayEventsInRange.get((int)(currentDay.getYearDay()));
		if (eventsForDays == null) {
			
			//Special case - there are no events for this day. no point wasting memory storing empty days
			return;
		}
		
		Collections.sort(eventsForDays);
		
		WeekViewTimeTable timeTable = new WeekViewTimeTable(eventsForDays, false);
		
		this.DTimetables.put(day, timeTable);
	}

	@Override
	public void deleteRange(AcalDateRange range) {
		AcalDateTime cur = range.start.clone();
		while (cur.before(range.end)) {
			int doy = (int)cur.getYearDay();
			partDayEventsInRange.remove(doy);
			DTimetables.remove(doy);
			cur.addDays(1);
		}
		window.reduceWindow(range);
	}

}