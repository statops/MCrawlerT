package com.morphoss.acal.database.cachemanager;

/**
 * This Exception signifies that a CacheRequest Object has some invalid/faulty or poorly constructed code. If thrown, developers
 * should be notified and correct the issue. This Exception should never be caught silently - always log to std error. 
 * @author Chris Noldus
 *
 */
public class CacheProcessingException extends Exception {
	
	public CacheProcessingException() {
		super();
	}
	
	public CacheProcessingException(String string) {
		super(string);
	}
	
	public CacheProcessingException(String string, Throwable cause) {
		super(cause);
	}
	
	public CacheProcessingException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;
}
