package com.morphoss.acal.database.cachemanager.requests;

import java.util.ArrayList;
import java.util.TimeZone;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequestWithResponse;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.database.cachemanager.CacheResponseListener;

/**
 * A CacheRequest that returns a List of CacheObjects which are tasks matching the specified criteria
 * 
 * To get the result you should pass in a CacheResponseListenr of the type ArrayList&lt;CacheObject&gt;
 * If you don't care about the result (e.g. your forcing a window size change) you may pass a null callback.
 * 
 * @author Andrew McMillan
 *
 */
public class CRTodosByType extends CacheRequestWithResponse<ArrayList<CacheObject>> {

	private final static String TAG = "aCal CRTodosByType"; 

	private boolean includeCompleted;
	private boolean includeFuture;
	
	/**
	 * Request all CacheObjects in the range provided. Pass the result to the callback provided
	 * @param range
	 * @param callBack
	 */
	public CRTodosByType(boolean showCompleted, boolean showFuture, CacheResponseListener<ArrayList<CacheObject>> callBack) {
		super(callBack);
		this.includeCompleted = showCompleted;
		this.includeFuture = showFuture;
	}
	
	@Override
	public void process(CacheTableManager processor)  throws CacheProcessingException{
		final ArrayList<CacheObject> result = new ArrayList<CacheObject>();

		AcalDateTime rangeEnd = AcalDateTime.getInstance().addDays(7);
		if ( includeFuture ) rangeEnd.setMonthStart().addMonths(3);
		AcalDateRange range = new AcalDateRange( AcalDateTime.getInstance().setMonthStart().addMonths(-1), rangeEnd );
		if (!processor.checkWindow(range)) {
			//Wait give up - caller can decide to rerequest or wait for cachechanged notification
			this.postResponse(new CRTodosByTypeResponse<ArrayList<CacheObject>>(result));
			return;
		}

		String dtEnd = rangeEnd.getMillis()+"";
		String offset = TimeZone.getDefault().getOffset(range.start.getMillis())+"";

		String whereClause = CacheTableManager.FIELD_RESOURCE_TYPE +"= '"+CacheTableManager.RESOURCE_TYPE_VTODO+"'";
		
		if ( !includeCompleted )
			whereClause += " AND "+CacheTableManager.FIELD_COMPLETED+" = "+Long.MAX_VALUE;    

		String[] whereArgs = null;
		if ( !includeFuture ) {
			whereClause +=
				" AND ( "+CacheTableManager.FIELD_DTEND+" = "+Long.MAX_VALUE + " OR " +
					"( "+CacheTableManager.FIELD_DTEND+" < ? AND NOT "+CacheTableManager.FIELD_DTEND_FLOAT+" )"+ " OR "+
					"( "+CacheTableManager.FIELD_DTEND+" + ? < ? AND "+CacheTableManager.FIELD_DTEND_FLOAT+" )"+
				")";
			whereArgs = new String[] { dtEnd, offset, dtEnd }; 
		}

		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Fetching todos WHERE "+whereClause);
		
		ArrayList<ContentValues> data = processor.query(null, whereClause, whereArgs, null,null,
				CacheTableManager.FIELD_DTEND+" ASC, "+CacheTableManager.FIELD_DTSTART+" ASC ");
		
		for (ContentValues cv : data) 
				result.add(CacheObject.fromContentValues(cv));
		
		this.postResponse(new CRTodosByTypeResponse<ArrayList<CacheObject>>(result));
	}

	/**
	 * This class represents the response from a CRTodosByType Request. It will be passed to the callback if one was provided.
	 * @author Andrew McMillan
	 *
	 * @param <E>
	 */
	public class CRTodosByTypeResponse<E extends ArrayList<CacheObject>> implements CacheResponse<ArrayList<CacheObject>> {
		
		private ArrayList<CacheObject> result;
		
		private CRTodosByTypeResponse(ArrayList<CacheObject> result) {
			this.result = result;
		}
		
		/**
		 * Returns the result of the original Request.
		 */
		public ArrayList<CacheObject> result() {
			return this.result;
		}
	}

}
