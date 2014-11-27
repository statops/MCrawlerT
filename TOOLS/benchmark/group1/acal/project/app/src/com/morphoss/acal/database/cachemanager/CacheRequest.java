package com.morphoss.acal.database.cachemanager;

import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;

/**
 * This is the generic interface for CacheRequests. Implement this interface if your CacheRequest does not return data.
 * If your request returns data, extend @CacheRequestWithResponse
 * 
 * @author Chris Noldus
 *
 */
public interface CacheRequest {
	
	/**
	 * Called by CacheManagerProcessor to process the request. Processor provides an interface to the database, implementation
	 * for this specific request should be put in here.
	 * 
	 * @param processor
	 * @throws CacheProcessingException - Thrown if any error occurs while processing. This is a significant exception, and if thrown,
	 * the cause should be identified and fixed.
	 */
	public void process(CacheTableManager processor) throws CacheProcessingException;
	
}