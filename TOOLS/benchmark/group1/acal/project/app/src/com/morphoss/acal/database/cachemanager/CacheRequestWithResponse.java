package com.morphoss.acal.database.cachemanager;


/**
 * Parent class of all CacheRequests that return data. Extend this class if your cache request returns data. If your CacheRequest
 * does not return data, implement @CacheRequest directly.
 * 
 * @author Chris Noldus
 *
 * @param <E> The Type of Object the response will contain.
 */
public abstract class CacheRequestWithResponse<E> implements CacheRequest {

	//The CallBack
	private CacheResponseListener<E> callBack = null;
	
	/**
	 * Mandatory constructor - stores the callBack to notify when posting response. CallBack can be null if requester doesn't care about
	 * response;
	 * @param callBack
	 */
	protected CacheRequestWithResponse(CacheResponseListener<E> callBack ){
		this.callBack = callBack;
	}
	
	/**
	 * Called by child classes to send response to the callback. Sends response on its own Thread so will usually return immediately.
	 * Beware of Race conditions when sending multiple requests - callbacks may come back in an arbitrary order.
	 * @param response
	 */
	protected void postResponse(final CacheResponse<E> response) {
		if (callBack == null) return;
		new Thread(new Runnable() {

			@Override
			public void run() {
				callBack.cacheResponse(response);
			}
		}).start();
	}
}
