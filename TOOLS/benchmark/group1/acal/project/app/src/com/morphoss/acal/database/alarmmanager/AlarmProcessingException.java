package com.morphoss.acal.database.alarmmanager;

public class AlarmProcessingException extends Exception {
	
	public AlarmProcessingException() {
		super();
	}
	
	public AlarmProcessingException(String string) {
		super(string);
	}
	
	public AlarmProcessingException(String string, Throwable cause) {
		super(cause);
	}
	
	public AlarmProcessingException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;
}
