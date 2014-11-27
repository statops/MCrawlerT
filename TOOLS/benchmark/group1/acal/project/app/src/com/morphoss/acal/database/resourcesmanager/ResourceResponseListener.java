package com.morphoss.acal.database.resourcesmanager;


/**
 * This class should be implemented by classes interested in receiving a response form ResourceRequests. The Paramater should be
 * set to the specific type Of response that is expected. Classes should implement as many types as needed to handle all the 
 * responses they expect. NB a given implementation of this will only receive responses for requests that they are the named
 * callback for. 
 *  
 * @author Chris Noldus
 *
 * @param <E>
 */
public interface ResourceResponseListener<E> {

	public void resourceResponse(ResourceResponse<E> response);

}
