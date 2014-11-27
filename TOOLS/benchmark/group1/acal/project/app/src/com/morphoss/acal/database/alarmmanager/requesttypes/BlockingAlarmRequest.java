package com.morphoss.acal.database.alarmmanager.requesttypes;

public interface BlockingAlarmRequest extends AlarmRequest {
	public boolean isProcessed();
}
