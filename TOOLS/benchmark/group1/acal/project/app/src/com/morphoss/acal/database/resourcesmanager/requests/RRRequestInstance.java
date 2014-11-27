package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ReadOnlyResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyResourceRequestWithResponse;
import com.morphoss.acal.dataservice.CalendarInstance;
import com.morphoss.acal.dataservice.Resource;

public class RRRequestInstance extends ReadOnlyResourceRequestWithResponse<CalendarInstance> {

	public static final String TAG = "aCal RRRequestInstance";
	
	private long rid;
	private String rrid;
	
	public RRRequestInstance(ResourceResponseListener<CalendarInstance> callBack, long resourceId, String recurrenceId) {
		super(callBack);
		this.rid = resourceId;
		this.rrid = recurrenceId;
		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Resource "+rid+", recurrence "+rrid+" is requested.");
	}

	
	public RRRequestInstance(ResourceResponseListener<CalendarInstance> callBack, CacheObject co) {
		this(callBack, co.getResourceId(), co.getRecurrenceId());
	}
	

	@Override
	public void process(ReadOnlyResourceTableManager processor) throws ResourceProcessingException {
		ArrayList<ContentValues> cv = processor.query(null, ResourceTableManager.RESOURCE_ID+" = ?", new String[]{rid+""}, null,null,null);
		ArrayList<ContentValues> pcv = processor.getPendingResources();
		try {
			//check pending first
			for (ContentValues val : pcv) {
				if (val.getAsLong(ResourceTableManager.PEND_RESOURCE_ID) == this.rid) {
					String blob = val.getAsString(ResourceTableManager.NEW_DATA);
					if ( blob == null || blob.equals("") ) {
						// this resource has been deleted
						throw new Exception("Resource deleted.");
					}
					else {
						CalendarInstance ci = CalendarInstance.fromPendingRowAndRRID(val, rrid);
						this.postResponse(new RRRequestInstanceResponse(ci));
						this.setProcessed();
					}
				}
			}
			if (!isProcessed()) {
				Resource res = Resource.fromContentValues(cv.get(0));
				CalendarInstance ci = CalendarInstance.fromResourceAndRRId(res, rrid);
				this.postResponse(new RRRequestInstanceResponse(ci));
				this.setProcessed();
			}
		}
		catch ( Exception e ) {
			Log.e(TAG, e.getMessage() + Log.getStackTraceString(e));
			this.postResponse(new RRRequestInstanceResponse(e));
			this.setProcessed();
		}
		
	}

	public class RRRequestInstanceResponse extends ResourceResponse<CalendarInstance> {

		private CalendarInstance result = null;
		public RRRequestInstanceResponse(CalendarInstance ci) {
			this.result = ci;
			if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
					"Resource "+ci.getResourceId()+", recurrence "+ci.getRecurrenceId()+" is returned.");
		}
		public RRRequestInstanceResponse(Exception e) { super(e); }
		
		@Override
		public CalendarInstance result() {
			return this.result;
		}
		
	}

}
