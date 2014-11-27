package com.morphoss.acal.database.resourcesmanager.requesttypes;

import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;

public interface ReadOnlyResourceRequest extends Comparable<ReadOnlyResourceRequest> {
	
	public void process(ResourceManager.ReadOnlyResourceTableManager processor) throws ResourceProcessingException;
	public boolean isProcessed();
	public void setProcessed();
	public int priority();
}
