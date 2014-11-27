package com.morphoss.acal.database.resourcesmanager.requests;

import com.morphoss.acal.database.DMQueryList;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.WriteableResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.requesttypes.BlockingResourceRequest;

public class RRBlockAndProcessQueryList implements BlockingResourceRequest {

	private boolean isProcessed;
	private DMQueryList list;
	
	
	public RRBlockAndProcessQueryList(DMQueryList list) {
		this.list = list;
	}
	
	@Override
	public boolean isProcessed() {
		return isProcessed;
	}

	@Override
	public void process(WriteableResourceTableManager processor)
			throws ResourceProcessingException {	
		processor.processActions(list);
		this.setProcessed();

	}

	@Override
	public void setProcessed() {
		this.isProcessed = true;
	}

}
