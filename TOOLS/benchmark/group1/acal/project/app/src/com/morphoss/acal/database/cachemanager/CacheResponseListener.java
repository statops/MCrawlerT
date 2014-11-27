package com.morphoss.acal.database.cachemanager;

/**
 * This class should be implemented by classes interested in receiving a response form CacheRequests. The Paramater should be
 * set to the specific type Of response that is expected. Classes should implement as many types as needed to handle all the 
 * responses they expect. NB a given implementation of this will only receive responses for requests that they are the named
 * callback for. 
 *  
 * @author Chris Noldus
 *
 * @param <E>
 */
public interface CacheResponseListener<E> {

	public void cacheResponse(CacheResponse<E> response);

}
