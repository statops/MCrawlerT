package com.morphoss.acal.database.cachemanager;


public abstract class BlockingCacheRequestWithResponse<E> extends CacheRequestWithResponse<E> implements BlockingCacheRequest {

	private boolean processed = false;
	private CacheResponse<E> response;
	
	public BlockingCacheRequestWithResponse() {
		super(null);
	}
	
	protected void postResponse(CacheResponse<E> r) {
		this.response = r;
		this.processed = true;
	}
	
	@Override
	public boolean isProcessed() { return this.processed; }
	
	public CacheResponse<E> getResponse() {
		return this.response;
	}

}
