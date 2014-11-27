package com.morphoss.acal.database.resourcesmanager;


public class ResourceProcessingException extends Exception {

	public ResourceProcessingException() {
		super();
	}
	
	public ResourceProcessingException(String string) {
		super(string);
	}
	
	public ResourceProcessingException(String string, Throwable cause) {
		super(cause);
	}
	
	public ResourceProcessingException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

}
