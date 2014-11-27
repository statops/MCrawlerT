package com.morphoss.acal.database.cachemanager.requests;

import java.util.ArrayList;

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
public class CRJournalsByType extends CacheRequestWithResponse<ArrayList<CacheObject>> {

	private final static String TAG = "aCal CRJournalsByType"; 

	
	/**
	 * Request all CacheObjects in the range provided. Pass the result to the callback provided
	 * @param range
	 * @param callBack
	 */
	public CRJournalsByType(CacheResponseListener<ArrayList<CacheObject>> callBack) {
		super(callBack);
	}
	
	@Override
	public void process(CacheTableManager processor)  throws CacheProcessingException{
		final ArrayList<CacheObject> result = new ArrayList<CacheObject>();

		AcalDateTime rangeEnd = AcalDateTime.getInstance().addDays(7);
		AcalDateRange range = new AcalDateRange( AcalDateTime.getInstance().setMonthStart().addMonths(-1), rangeEnd );
		if (!processor.checkWindow(range)) {
			//Wait give up - caller can decide to rerequest or wait for cachechanged notification
			this.postResponse(new CRJournalsByTypeResponse<ArrayList<CacheObject>>(result));
			return;
		}

		String whereClause = CacheTableManager.FIELD_RESOURCE_TYPE +"= '"+CacheTableManager.RESOURCE_TYPE_VJOURNAL+"'";
		

		String[] whereArgs = null;


		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Fetching journals WHERE "+whereClause);
		
		ArrayList<ContentValues> data = processor.query(null, whereClause, whereArgs, null,null,
				CacheTableManager.FIELD_DTEND+" ASC, "+CacheTableManager.FIELD_DTSTART+" ASC ");
		
		for (ContentValues cv : data) 
				result.add(CacheObject.fromContentValues(cv));
		
		this.postResponse(new CRJournalsByTypeResponse<ArrayList<CacheObject>>(result));
	}

	/**
	 * This class represents the response from a CRJournalsByType Request. It will be passed to the callback if one was provided.
	 * @author Andrew McMillan
	 *
	 * @param <E>
	 */
	public class CRJournalsByTypeResponse<E extends ArrayList<CacheObject>> implements CacheResponse<ArrayList<CacheObject>> {
		
		private ArrayList<CacheObject> result;
		
		private CRJournalsByTypeResponse(ArrayList<CacheObject> result) {
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
