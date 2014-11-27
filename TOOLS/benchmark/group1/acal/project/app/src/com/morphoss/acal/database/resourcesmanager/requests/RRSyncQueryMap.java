package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.Map;

import android.content.ContentValues;

import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.WriteableResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.requesttypes.BlockingResourceRequestWithResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;

public class RRSyncQueryMap extends BlockingResourceRequestWithResponse<Map<String,ContentValues>> {

	private long collectionId;
	private boolean needsSyncOnly;
	
	public RRSyncQueryMap(long collectionId, boolean needsSyncOnly) {
		this.collectionId = collectionId;
		this.needsSyncOnly = needsSyncOnly;
	}
	
	@Override
	public void process(WriteableResourceTableManager processor) throws ResourceProcessingException {
		String syncSelection = "";
		if ( needsSyncOnly )
			syncSelection = " AND (" + ResourceTableManager.NEEDS_SYNC + " = 1 OR " + ResourceTableManager.RESOURCE_DATA + " IS NULL)";

		Map<String, ContentValues> data = processor.contentQueryMap(
				ResourceTableManager.COLLECTION_ID + " = ?"+ syncSelection,
				new String[] { collectionId + "" });
		this.postResponse(new RRSyncQueryMapResponse(data));
	}

	public class RRSyncQueryMapResponse extends ResourceResponse<Map<String,ContentValues>> {

		Map<String,ContentValues> data;
		
		public RRSyncQueryMapResponse(Map<String,ContentValues> data) {
			this.data = data;
		}
		@Override
		public Map<String, ContentValues> result() {
			return data;
		}
		
	}
}
