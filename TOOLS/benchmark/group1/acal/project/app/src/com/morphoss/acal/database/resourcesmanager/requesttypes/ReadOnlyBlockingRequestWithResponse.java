package com.morphoss.acal.database.resourcesmanager.requesttypes;

import com.morphoss.acal.database.resourcesmanager.ResourceResponse;


public abstract class ReadOnlyBlockingRequestWithResponse<E> extends ReadOnlyResourceRequestWithResponse<E>   {

	private ResourceResponse<E> response;
	
	public ReadOnlyBlockingRequestWithResponse() {
		super(null);
	}
	
	public ReadOnlyBlockingRequestWithResponse(int priority) {
		super(null,priority);
	}
	
	protected void postResponse(ResourceResponse<E> r) {
		this.response = r;
		this.setProcessed();
	}
	
	public ResourceResponse<E> getResponse() {
		return this.response;
	}
}
