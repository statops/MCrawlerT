package com.morphoss.acal.database.cachemanager.requests;

import java.util.ArrayList;

import android.content.ContentValues;

import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequestWithResponse;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.database.cachemanager.CacheResponseListener;

/**
 * A CacheRequest that returns a List of CacheObjects that occur in the specified range.
 * 
 * To get the result you should pass in a CacheResponseListenr of the type ArrayList&lt;CacheObject&gt;
 * If you don't care about the result (e.g. your forcing a window size change) you may pass a null callback.
 * 
 * @author Chris Noldus
 *
 */
public class CRObjectsInRange extends CacheRequestWithResponse<ArrayList<CacheObject>> {

	private AcalDateRange range;
	private String objectType = null;
	
	/**
	 * Request all CacheObjects in the range provided. Pass the result to the callback provided
	 * @param range
	 * @param callBack
	 */
	public CRObjectsInRange(AcalDateRange range, CacheResponseListener<ArrayList<CacheObject>> callBack) {
		super(callBack);
		this.range = range;
	}
	
	/**
	 * Request all VEVENT CacheObjects in the range provided. Pass the result to the callback provided
	 * @param range
	 * @param callBack
	 */
	public static CRObjectsInRange EventsInRange(AcalDateRange range, CacheResponseListener<ArrayList<CacheObject>> callBack) {
		CRObjectsInRange result = new CRObjectsInRange(range,callBack);
		result.objectType = CacheTableManager.RESOURCE_TYPE_VEVENT;
		return result;
	}
	
	@Override
	public void process(CacheTableManager processor)  throws CacheProcessingException{
		final ArrayList<CacheObject> result = new ArrayList<CacheObject>();
		if (!processor.checkWindow(range)) {
			//Wait give up - caller can decide to rerequest or waitf for cachechanged notification
			this.postResponse(new CRObjectsInRangeResponse<ArrayList<CacheObject>>(result));
			return;
		}

		ArrayList<ContentValues> data = processor.queryInRange(range,objectType);
		for (ContentValues cv : data) 
				result.add(CacheObject.fromContentValues(cv));
		
		this.postResponse(new CRObjectsInRangeResponse<ArrayList<CacheObject>>(result));
	}

	/**
	 * This class represents the response from a CRObjectsInRange Request. It will be passed to the callback if one was provided.
	 * @author Chris Noldus
	 *
	 * @param <E>
	 */
	public class CRObjectsInRangeResponse<E extends ArrayList<CacheObject>> implements CacheResponse<ArrayList<CacheObject>> {
		
		private ArrayList<CacheObject> result;
		
		private CRObjectsInRangeResponse(ArrayList<CacheObject> result) {
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
