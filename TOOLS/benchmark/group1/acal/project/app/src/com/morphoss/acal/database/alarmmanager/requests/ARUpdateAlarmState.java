package com.morphoss.acal.database.alarmmanager.requests;

import com.morphoss.acal.database.alarmmanager.ALARM_STATE;
import com.morphoss.acal.database.alarmmanager.AlarmProcessingException;
import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.alarmmanager.AlarmQueueManager.AlarmTableManager;
import com.morphoss.acal.database.alarmmanager.requesttypes.BlockingAlarmRequest;

/**
 * Use this request to change the state of an alarm. You must provide the original row for this to work.
 * 
 * @author Chris Noldus
 */
public class ARUpdateAlarmState implements BlockingAlarmRequest {

	private boolean processed = false;
	private AlarmRow row;
	private ALARM_STATE newState;
	
	public ARUpdateAlarmState(AlarmRow row, ALARM_STATE newState) {
		this.row = row;
		this.newState = newState;
	}
	
	@Override
	public boolean isProcessed() {
		return processed;
	}

	@Override
	public void process(AlarmTableManager processor) throws AlarmProcessingException {
		processor.updateAlarmState(row, newState);
		this.processed = true;
	}

	@Override
	public String getLogDescription() {
		return "Update Alarm State to "+newState+" for alarm row: "+row.getId();
	}

}
