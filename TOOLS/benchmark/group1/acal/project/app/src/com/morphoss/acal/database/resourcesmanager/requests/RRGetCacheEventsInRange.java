package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.database.CacheWindow;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ReadOnlyResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyResourceRequestWithResponse;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VComponent;

public class RRGetCacheEventsInRange extends ReadOnlyResourceRequestWithResponse<ArrayList<Resource>> {

	public static final String TAG = "aCal RRGetCacheEventsInRange";
	private CacheWindow window;
	
	
	public RRGetCacheEventsInRange(CacheWindow window, ResourceResponseListener<ArrayList<Resource>> callback) {
		super(callback);
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Instantiated for window "+window);
		this.window = window;
	}
	
	@Override
	public void process(ReadOnlyResourceTableManager processor) {
		ArrayList<Resource> result = new ArrayList<Resource>();
		// TODO At the moment this algorithm ignores pending resources nor does it handle floating events.
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Process Called...");
		AcalDateRange range = window.getRequestedWindow();
		if (window.getRequestedWindow() == null) {
			if ( CacheManager.DEBUG ) Log.println(Constants.LOGD, TAG, "Resource request cancelled - cache window already full");
			super.postResponse(new RREventsInRangeResponse<ArrayList<Resource>>(result));
			this.setProcessed();
			return;
		}
		
		//step 1 --> convert window range to long UTC start 
		long start = range.start.getMillis();
		long end = range.end.getMillis();
		

		String whereClause = 
				"("+ResourceTableManager.EFFECTIVE_TYPE +"='"+VComponent.VEVENT+
				"' OR "+ResourceTableManager.EFFECTIVE_TYPE +"='"+VComponent.VTODO+"' )" +
				" AND ("+ResourceTableManager.LATEST_END+" IS NULL OR " + ResourceTableManager.LATEST_END+" >= "+start+" )" +
				" AND ("+ResourceTableManager.EARLIEST_START+" IS NULL OR "+ResourceTableManager.EARLIEST_START+" <= "+end+" )";

		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD,TAG,"Getting Resource rows where:\n"+whereClause);

		//step 2 query db for resources in range
		ArrayList<ContentValues> rValues = processor.query(null,whereClause,null,null,null,null);
		
		//also need pendings
		ArrayList<ContentValues> pValues = processor.getPendingResources();
		HashMap<Long,ContentValues> pendingMap = new HashMap<Long, ContentValues>();
		
		if (!pValues.isEmpty()) {
			//we need to remove from rValues, any values that have a corresponding entry in pValues
			for (ContentValues cv : pValues) {
				pendingMap.put(cv.getAsLong(ResourceTableManager.PEND_RESOURCE_ID),cv);	
			}
			Iterator<ContentValues> it = rValues.iterator();
			while(it.hasNext()) {
				ContentValues rValue = it.next();
				long rid = rValue.getAsLong(ResourceTableManager.RESOURCE_ID);
				if (pendingMap.containsKey(rid)) {
					ContentValues pValue = pendingMap.get(rid);
					it.remove();
					String data = pValue.getAsString(ResourceTableManager.NEW_DATA);
					if (data == null || data.equals("")) pendingMap.remove(rid);
				}
			}
		}
		
		//Merge the lists
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD,TAG,rValues.size()+" Resource Rows retreived. and "+pValues.size()+" pending values. Converting into Resource Objects");
		for (Entry<Long, ContentValues> ent : pendingMap.entrySet()) {
			Resource r = Resource.fromContentValues(ent.getValue()); 
			r.setPending(true);
			result.add(r);
			
		}
		for (ContentValues cv : rValues) result.add(Resource.fromContentValues(cv));
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD,TAG, "Conversion complete. Populating VCalendars and appending events.");
		
		
		//post response
		super.postResponse(new RREventsInRangeResponse<ArrayList<Resource>>(result));

		this.setProcessed();
	}
	
	/**
	 * This class represents the response from a RREventsInRangeResponse Request. It will be passed to the callback if one was provided.
	 * @author Chris Noldus
	 *
	 * @param <E>
	 */
	public class RREventsInRangeResponse<E extends ArrayList<Resource>> extends ResourceResponse<ArrayList<Resource>> {
		
		private ArrayList<Resource> result;
		
		private RREventsInRangeResponse(ArrayList<Resource> result) {
			this.result = result;
		}
		
		/**
		 * Returns the result of the original Request.
		 */
		public ArrayList<Resource> result() {
			return this.result;
		}
		
	}



}
