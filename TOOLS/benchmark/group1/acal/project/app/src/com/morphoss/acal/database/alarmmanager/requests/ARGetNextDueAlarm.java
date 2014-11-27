package com.morphoss.acal.database.alarmmanager.requests;

import com.morphoss.acal.database.alarmmanager.AlarmProcessingException;
import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.alarmmanager.AlarmQueueManager.AlarmTableManager;
import com.morphoss.acal.database.alarmmanager.requesttypes.AlarmResponse;
import com.morphoss.acal.database.alarmmanager.requesttypes.BlockingAlarmRequestWithResponse;

/**
 * Use this to get the next due alarm.
 * 
 * @author Chris Noldus
 *
 */
public class ARGetNextDueAlarm extends BlockingAlarmRequestWithResponse<AlarmRow> {

	@Override
	public void process(AlarmTableManager processor) throws AlarmProcessingException {
		AlarmRow res = processor.getNextDueAlarm();
		this.postResponse(new ARGetNextAlarmResult(res));
	}
	
	public class ARGetNextAlarmResult extends AlarmResponse<AlarmRow> {

		private AlarmRow result;
		
		public ARGetNextAlarmResult(AlarmRow result) { 
			this.result = result;
		}
		
		@Override
		public AlarmRow result() {return this.result;	}
		
	}

	@Override
	public String getLogDescription() {
		return "Request next due alarm";
	}
}
