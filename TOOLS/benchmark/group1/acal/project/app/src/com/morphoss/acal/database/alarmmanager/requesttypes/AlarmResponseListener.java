package com.morphoss.acal.database.alarmmanager.requesttypes;


public interface AlarmResponseListener<E> {
	public void alarmResponse(AlarmResponse<E> response);
}
