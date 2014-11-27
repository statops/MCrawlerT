package com.morphoss.acal.database.cachemanager;

import java.util.ArrayList;

import com.morphoss.acal.database.DataChangeEvent;

public class CacheChangedEvent {

	private ArrayList<DataChangeEvent> changes;
	private boolean windowOnly;
	
	public CacheChangedEvent(ArrayList<DataChangeEvent> changes, boolean windowOnly) {
		this.changes = changes;
		this.windowOnly = windowOnly;
	}
	
	public boolean isWindowOnly() { return this.windowOnly; }
	public ArrayList<DataChangeEvent> getChanges() { return this.changes; }
}
