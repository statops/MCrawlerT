package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.database.resourcesmanager.ResourceManager.ReadOnlyResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyBlockingRequestWithResponse;

public class RRRequestInstanceBlocking extends ReadOnlyBlockingRequestWithResponse<ContentValues> {

	public static final String TAG = "aCal RRRequestInstanceBlocking";
	long resourceId;

	public RRRequestInstanceBlocking( long resourceId) {
		super();
		this.resourceId = resourceId;
	}
	
	@Override
	public void process(ReadOnlyResourceTableManager processor) throws ResourceProcessingException {
		ArrayList<ContentValues> cv = processor.query(null, ResourceTableManager.RESOURCE_ID+" = ?", new String[]{resourceId+""}, null,null,null);
		ArrayList<ContentValues> pcv = processor.getPendingResources();
		try {
			//check pending first
			for (ContentValues val : pcv) {
				if (val.getAsLong(ResourceTableManager.PEND_RESOURCE_ID) == this.resourceId) {
					String blob = val.getAsString(ResourceTableManager.NEW_DATA);
					if ( blob == null || blob.equals("") ) {
						// this resource has been deleted
						throw new Exception("Resource deleted.");
					}
					else {
						this.postResponse(new RRRequestInstanceBlockingResult(val));
					}
					break;
				}
			}
			if (!isProcessed()) {
				this.postResponse(new RRRequestInstanceBlockingResult(cv.get(0)));
			}
		}
		catch ( Exception e ) {
			Log.e(TAG, e.getMessage() + Log.getStackTraceString(e));
			this.postResponse(new RRRequestInstanceBlockingResult(e));
			this.setProcessed();
		}
	}

	public class RRRequestInstanceBlockingResult extends ResourceResponse<ContentValues> {

		private ContentValues result;
		
		public RRRequestInstanceBlockingResult(ContentValues result) { 
			this.result = result;
			setProcessed();
		}
		public RRRequestInstanceBlockingResult(Exception e) { super(e); }
		
		@Override
		public ContentValues result() {return this.result;	}
		
	}
	
}
