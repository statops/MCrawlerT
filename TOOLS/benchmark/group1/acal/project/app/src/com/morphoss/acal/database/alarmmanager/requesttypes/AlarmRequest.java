package com.morphoss.acal.database.alarmmanager.requesttypes;

import com.morphoss.acal.database.alarmmanager.AlarmProcessingException;
import com.morphoss.acal.database.alarmmanager.AlarmQueueManager.AlarmTableManager;


/**
 * This is the generic interface for AlarmRequests. Implement this interface if your AlarmRequest does not return data.
 * If your request returns data, extend @AlarmRequestWithResponse
 * 
 * @author Chris Noldus
 *
 */

public interface AlarmRequest {

	/**
	 * Called by AlarmTableManager to process the request. Processor provides an interface to the database, implementation
	 * for this specific request should be put in here.
	 * 
	 * @param processor
	 * @throws AlarmProcessingException - Thrown if any error occurs while processing. This is a significant exception, and if thrown,
	 * the cause should be identified and fixed.
	 */
	public void process(AlarmTableManager processor) throws AlarmProcessingException;
	
	/**
	 * Needed so the AlarmTableManager can write useful informationto the logs
	 */
	public String getLogDescription();

}

