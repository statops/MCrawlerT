package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;

import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.WriteableResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ResourceRequest;

public class RRDeleteByCollectionId implements ResourceRequest {

	private ArrayList<Long> ids;
	
	public RRDeleteByCollectionId(ArrayList<Long> ids) {
		this.ids = ids;
	}

	private boolean processed = false;
	@Override
	public boolean isProcessed() { return this.processed; }
	@Override
	public synchronized void setProcessed() { this.processed = true; }
	
	
	@Override
	public void process(WriteableResourceTableManager processor)	throws ResourceProcessingException {
		for (long id : ids) processor.deleteByCollectionId(id);
	}

}
