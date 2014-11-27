package com.morphoss.acal.database.alarmmanager.requesttypes;


public abstract class BlockingAlarmRequestWithResponse<E> extends AlarmRequestWithResponse<E> implements BlockingAlarmRequest {

	private boolean processed = false;
	private AlarmResponse<E> response;
	
	public BlockingAlarmRequestWithResponse() {
		super(null);
	}
	
	protected void postResponse(AlarmResponse<E> r) {
		this.response = r;
		this.processed = true;
	}
	
	@Override
	public boolean isProcessed() { return this.processed; }
	
	public AlarmResponse<E> getResponse() {
		return this.response;
	}
		
}
