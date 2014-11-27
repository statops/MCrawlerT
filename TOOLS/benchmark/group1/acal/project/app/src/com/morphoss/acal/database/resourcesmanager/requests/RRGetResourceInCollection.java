package com.morphoss.acal.database.resourcesmanager.requests;

import android.content.ContentValues;

import com.morphoss.acal.database.resourcesmanager.ResourceManager.ReadOnlyResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyBlockingRequestWithResponse;

public class RRGetResourceInCollection extends
		ReadOnlyBlockingRequestWithResponse<ContentValues> {

	private long collectionId;
	private String responseHref;
	
	public RRGetResourceInCollection(long collectionId, String responseHref) {
		this.collectionId = collectionId;
		this.responseHref = responseHref;
	}
	@Override
	public void process(ReadOnlyResourceTableManager processor)	throws ResourceProcessingException {	
		this.postResponse(new RRGetResourceInCollectionResult(processor.getResourceInCollection(collectionId, responseHref)));
	}

	public class RRGetResourceInCollectionResult extends ResourceResponse<ContentValues> {

		private ContentValues result;
		
		public RRGetResourceInCollectionResult(ContentValues result) { this.result = result; }
		
		@Override
		public ContentValues result() {return this.result;	}
		
	}

}
