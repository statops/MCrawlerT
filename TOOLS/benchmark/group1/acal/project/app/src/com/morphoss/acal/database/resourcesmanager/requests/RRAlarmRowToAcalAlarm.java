package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;

import android.content.ContentValues;

import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.WriteableResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.requesttypes.BlockingResourceRequestWithResponse;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.AcalAlarm;
import com.morphoss.acal.davacal.Masterable;
import com.morphoss.acal.davacal.RecurrenceId;
import com.morphoss.acal.davacal.VCalendar;

public class RRAlarmRowToAcalAlarm extends BlockingResourceRequestWithResponse<AcalAlarm> {

	private AlarmRow row;
	public RRAlarmRowToAcalAlarm(AlarmRow row) {
		this.row = row;
	}
	
	@Override
	public void process(WriteableResourceTableManager processor) throws ResourceProcessingException {
		try {
			Resource r = null;
		
			//first check to see if there is a pending version
			ArrayList<ContentValues> res = processor.getPendingResources();
			for (ContentValues cv : res) {
				if (cv.getAsLong(ResourceTableManager.PEND_RESOURCE_ID) == row.getResourceId())  {
					r = Resource.fromContentValues(cv);
					break;
				}
			}
			if (r == null)  r = Resource.fromContentValues(processor.getResource(row.getResourceId()));
			 
			VCalendar vc = (VCalendar) VCalendar.createComponentFromResource(r);
			
			Masterable master = vc.getChildFromRecurrenceId(RecurrenceId.fromString(row.getReccurenceId()));
			ArrayList<AcalAlarm> alarms = master.getAlarms();
			
			for (AcalAlarm alarm : alarms) {
				if (alarm.blob.equals(row.getBlob())) {
					this.postResponse(new RRAlarmRowToAcalAlarmResponse(alarm));
					return;
				}
			}
		} catch (Exception e) { }
		this.postResponse(new RRAlarmRowToAcalAlarmResponse(null));
	}
	
	public class RRAlarmRowToAcalAlarmResponse extends ResourceResponse<AcalAlarm> {

		private AcalAlarm result;
		
		public RRAlarmRowToAcalAlarmResponse (AcalAlarm result) {
			this.result = result;
		}
		
		@Override
		public AcalAlarm result() {
			return result;
		}
		
	}

}
