package com.morphoss.acal.database.cachemanager.requests;

import java.util.ArrayList;
import java.util.TimeZone;

import android.content.ContentValues;

import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequestWithResponse;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.weekview.WeekViewCache;

public class CRObjectsInWindow  extends CacheRequestWithResponse<ArrayList<CacheObject>> {

	private WeekViewCache caller;
	
	/**
	 * Request all CacheObjects in the range provided. Pass the result to the callback provided
	 * @param range
	 * @param callBack
	 */
	public CRObjectsInWindow(WeekViewCache caller) {
		super(caller);
		this.caller = caller;
	}
	
	@Override
	public void process(CacheTableManager processor)  throws CacheProcessingException{
		final ArrayList<CacheObject> result = new ArrayList<CacheObject>();
		AcalDateRange range = caller.getWindow().getRequestedWindow();
		
		//No longer need data?
		if (range == null) {
			this.postResponse(new CRObjectsInWindowResponse<ArrayList<CacheObject>>(result, null));
			return;
		}
		
		//is data available?
		if (!processor.checkWindow(range)) {
			//Wait give up - caller can decide to rerequest or waitf for cachechanged notification
			this.postResponse(new CRObjectsInWindowResponse<ArrayList<CacheObject>>(result, range));
			return;
		}

		String dtStart = range.start.getMillis()+"";
		String dtEnd = range.end.getMillis()+"";
		String offset = TimeZone.getDefault().getOffset(range.start.getMillis())+"";
		
		
		ArrayList<ContentValues> data = processor.query(null, 
				"( " + 
					"( "+CacheTableManager.FIELD_DTEND+" > ? AND NOT "+CacheTableManager.FIELD_DTEND_FLOAT+" )"+
						" OR "+
						"( "+CacheTableManager.FIELD_DTEND+" + ? > ? AND "+CacheTableManager.FIELD_DTEND_FLOAT+" )"+
						" OR "+
					"( "+CacheTableManager.FIELD_DTEND+" ISNULL )"+
				" ) AND ( "+
					"( "+CacheTableManager.FIELD_DTSTART+" < ? AND NOT "+CacheTableManager.FIELD_DTSTART_FLOAT+" )"+
						" OR "+
					"( "+CacheTableManager.FIELD_DTSTART+" + ? < ? AND "+CacheTableManager.FIELD_DTSTART_FLOAT+" )"+
						" OR "+
					"( "+CacheTableManager.FIELD_DTSTART+" ISNULL )"+
				")",
				new String[] {dtStart , offset, dtStart, dtEnd, offset, dtEnd},
				null,null,CacheTableManager.FIELD_DTSTART+" ASC");
		
		for (ContentValues cv : data) 
				result.add(CacheObject.fromContentValues(cv));
		caller.getWindow().expandWindow(range);
		this.postResponse(new CRObjectsInWindowResponse<ArrayList<CacheObject>>(result,range));
	}

	/**
	 * This class represents the response from a CRObjectsInRange Request. It will be passed to the callback if one was provided.
	 * @author Chris Noldus
	 *
	 * @param <E>
	 */
	public class CRObjectsInWindowResponse<E extends ArrayList<CacheObject>> implements CacheResponse<ArrayList<CacheObject>> {
		
		private ArrayList<CacheObject> result;
		private AcalDateRange range;
		
		private CRObjectsInWindowResponse(ArrayList<CacheObject> result, AcalDateRange range) {
			this.result = result;
			this.range = range;
		}
		
		public AcalDateRange rangeRetreived() {
			return this.range;
		}
		
		/**
		 * Returns the result of the original Request.
		 */
		public ArrayList<CacheObject> result() {
			return this.result;
		}
	}
}


