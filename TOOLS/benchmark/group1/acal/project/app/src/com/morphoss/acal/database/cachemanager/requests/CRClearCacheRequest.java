package com.morphoss.acal.database.cachemanager.requests;

import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequest;

public class CRClearCacheRequest implements CacheRequest {

	@Override
	public void process(CacheTableManager processor) throws CacheProcessingException {
		processor.rebuildCache();
	}

}
