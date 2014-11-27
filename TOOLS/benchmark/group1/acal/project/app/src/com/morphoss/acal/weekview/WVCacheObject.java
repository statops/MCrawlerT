package com.morphoss.acal.weekview;

import com.morphoss.acal.database.cachemanager.CacheObject;

/**
 * This class is used by weekview to provide all the features of a cache object with the ability to store
 * useful temporary data.
 * 
 * @author Chris Noldus
 *
 */
public class WVCacheObject extends CacheObject {
	private int maxWidth;
	private int actualWidth;
	private int lastWidth;


	public WVCacheObject(CacheObject original) {
		super(original);
	}

	public int calulateMaxWidth(int screenWidth, int HSPP) {
		this.actualWidth = (int)(getEnd()-getStart())/HSPP;
		maxWidth = (actualWidth>screenWidth ? screenWidth : actualWidth);
		return maxWidth;
	}
	public int getMaxWidth() {
		return this.maxWidth;
	}
	public int getActualWidth() {
		return this.actualWidth;
	}
	public int getLastWidth() {
		return this.lastWidth;
	}
	public void setLastWidth(int w) {
		this.lastWidth = w;
	}

	public boolean overlaps(WVCacheObject other) {
		return (this.getRange().overlaps(other.getRange()));
	}
}
