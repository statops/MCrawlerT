package com.morphoss.acal.database.cachemanager.requests;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequest;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;


public class CRAddRangeResult implements CacheRequest {

	private DMQueryList queries;
	private AcalDateRange range;
	public static final String TAG = "aCal CRAddRangeResult";
	
	public CRAddRangeResult(DMQueryList queries, AcalDateRange range) {
		this.queries = queries;
		this.range = range;
	}
	
	@Override
	public void process(CacheTableManager processor) throws CacheProcessingException {
		if ( CacheManager.DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Processing query set and updating window");
		processor.processActions(queries);
		processor.updateWindowToInclude(range);
		if ( CacheManager.DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,"Done");
	}

	

}
