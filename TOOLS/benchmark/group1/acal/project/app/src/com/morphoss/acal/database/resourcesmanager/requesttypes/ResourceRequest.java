package com.morphoss.acal.database.resourcesmanager.requesttypes;

import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;

public interface ResourceRequest {

	public boolean isProcessed();
	public void setProcessed();
	
	public void process(ResourceManager.WriteableResourceTableManager processor) throws ResourceProcessingException;

}