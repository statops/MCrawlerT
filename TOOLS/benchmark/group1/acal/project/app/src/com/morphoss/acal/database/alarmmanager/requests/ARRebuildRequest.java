package com.morphoss.acal.database.alarmmanager.requests;

import com.morphoss.acal.database.alarmmanager.AlarmProcessingException;
import com.morphoss.acal.database.alarmmanager.AlarmQueueManager.AlarmTableManager;
import com.morphoss.acal.database.alarmmanager.requesttypes.AlarmRequest;

/**
 * This is used only by Alarm manager if its database table has (possibly) become inconsistant. Forces a rebuild of the table.
 * @author Chris Noldus
 *
 */
public class ARRebuildRequest implements AlarmRequest {

	@Override
	public void process(AlarmTableManager processor) throws AlarmProcessingException {
		processor.rebuild();
	}

	@Override
	public String getLogDescription() {
		return "Notify Alarm Manager that database needs to be rebuilt";
	}

}
