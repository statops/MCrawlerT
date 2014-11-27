package com.morphoss.acal.database.cachemanager.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequestWithResponse;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.database.cachemanager.CacheResponseListener;

/**
 * A CacheRequest that returns a Map CacheObjects that occur in the specified month.
 * The Map Keys are Days of the Month, the values are lists of events.
 * 
 * To get the result you should pass in a CacheResponseListenr of the type ArrayList&lt;CacheObject&gt;
 * If you don't care about the result (e.g. your forcing a window size change) you may pass a null callback.
 * 
 * @author Chris Noldus
 *
 */
public class CRObjectsInMonthByDay extends CacheRequestWithResponse<HashMap<Short,ArrayList<CacheObject>>> {

	private int month;
	private int year;
	private String objectType = null;
	
	public static final String TAG = "aCal CRObjectsInMonthByDay";
	
	//metrics
	private long construct =-1;
	private long pstart=-1;
	private long qstart=-1;
	private long qend=-1;
	private long pend=-1;
	
	
	/**
	 * Request all for the month provided. Pass the result to the callback provided
	 * @param month
	 * @param year
	 * @param callBack
	 */
	public CRObjectsInMonthByDay(int month, int year, CacheResponseListener<HashMap<Short,ArrayList<CacheObject>>> callBack) {
		super(callBack);
		construct = System.currentTimeMillis();
		this.month = month;
		this.year = year;
	}

	/**
	 * Request all VEVENT CacheObjects for the month provided. Pass the result to the callback provided
	 * @param month
	 * @param year
	 * @param callBack
	 */
	public static CRObjectsInMonthByDay EventsInMonthByDay(int month, int year, CacheResponseListener<HashMap<Short,ArrayList<CacheObject>>> callBack) {
		CRObjectsInMonthByDay result = new CRObjectsInMonthByDay(month,year,callBack);
		result.objectType = CacheTableManager.RESOURCE_TYPE_VEVENT;
		return result;
	}
	
	@Override
	public void process(CacheTableManager processor) throws CacheProcessingException {
		pstart = System.currentTimeMillis();
		final HashMap<Short,ArrayList<CacheObject>> result = new HashMap<Short,ArrayList<CacheObject>>();
		AcalDateTime start = new AcalDateTime( year, month, 1, 0, 0, 0, TimeZone.getDefault().getID()); 
		AcalDateTime end = start.clone().addMonths(1).applyLocalTimeZone();
		AcalDateRange range = new AcalDateRange(start,end);
		
		if (!processor.checkWindow(range)) {
			//Wait give up - caller can decide to rerequest or wait for cachechanged notification
			this.postResponse(new CREventsInMonthByDayResponse<HashMap<Short,ArrayList<CacheObject>>>(result));
			pend = System.currentTimeMillis();
			printMetrics();
			return;
		}
		
		qstart  = System.currentTimeMillis();
		ArrayList<ContentValues> data = processor.queryInRange(range,objectType);
		qend  = System.currentTimeMillis();
		int daysInMonth = start.getActualMaximum(AcalDateTime.DAY_OF_MONTH);
		for (ContentValues value : data ) {
			try {
				CacheObject co = CacheObject.fromContentValues(value);
				start = co.getStartDateTime();
				end = co.getEndDateTime();
				if ( start == null ) start = end;
				if ( start == null ) continue;
				if ( end == null ) end = start;
				else end.addSeconds(-1);
				if (start.getMonth() < month) start.setMonthDay(1);
				if (end.getMonth() > month) end.setMonthDay(-1);
				for( short dayOfMonth = start.getMonthDay()
						; dayOfMonth <= (end.getMonthDay() < start.getMonthDay() ? daysInMonth : end.getMonthDay())
						; dayOfMonth++ ) {
					if ( !result.containsKey(dayOfMonth) ) result.put(dayOfMonth, new ArrayList<CacheObject>());
					result.get(dayOfMonth).add(co);
				}
			}
			catch( Exception e) {
				Log.w(TAG,Log.getStackTraceString(e));
			}
		}
		
		this.postResponse(new CREventsInMonthByDayResponse<HashMap<Short,ArrayList<CacheObject>>>(result));
		pend = System.currentTimeMillis();
		printMetrics();
	}
	
	private void printMetrics() {
		long total = pend-construct;
		long process = pend-pstart;
		long queuetime = pstart-construct;
		long query = qend-qstart;
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD, TAG,
				String.format("Metrics: Queue Time:%5d, Process Time:%5d,  Query Time:%4d,  Total Time:%6d", queuetime, process, query, total) );
	}
	

	/**
	 * This class represents the response from a CRObjectsInMonthByDay Request. It will be passed to the callback if one was provided.
	 * @author Chris Noldus
	 *
	 * @param <E>
	 */
public class CREventsInMonthByDayResponse<E extends HashMap<Short,ArrayList<CacheObject>>> implements CacheResponse<HashMap<Short,ArrayList<CacheObject>>> {
		
		private HashMap<Short,ArrayList<CacheObject>> result;
		
		public CREventsInMonthByDayResponse(HashMap<Short,ArrayList<CacheObject>> result) {
			this.result = result;
		}
		
		public HashMap<Short,ArrayList<CacheObject>> result() {
			return this.result;
		}
	}
	
}
