package com.morphoss.acal.database.cachemanager.requests;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequest;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;

public class CRResourceChanged implements CacheRequest {

	private DMQueryList queries;
	public static final String TAG = "aCal CRResourceChanged";
	
	public CRResourceChanged(DMQueryList queries) {
		this.queries = queries;
	}
	
	@Override
	public void process(CacheTableManager processor) throws CacheProcessingException {
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD, TAG, "Processing query set");
		processor.processActions(queries);
		if ( CacheManager.DEBUG ) Log.println(Constants.LOGD, TAG,"Done");
	}

}
