package com.morphoss.acal.activity;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.WriteableResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ResourceRequestWithResponse;
import com.morphoss.acal.dataservice.EventInstance;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VEvent;


public class RREventEditedRequest extends ResourceRequestWithResponse<Long> {

	private static final String TAG = "aCal RREventEditedRequest"; 
	private EventInstance event;
	private int action;
	private int instances;
	
	protected RREventEditedRequest(ResourceResponseListener<Long> callBack, EventInstance event, int action, int instances) {
		super(callBack);
		this.action = action;
		this.event = event;
		this.instances = instances;
	}

	@Override
	public void process(WriteableResourceTableManager processor) throws ResourceProcessingException {
		String newBlob = "";
		String oldBlob = null;
		String uid = null;

		try {
			if (action == EventEdit.ACTION_EDIT || action == EventEdit.ACTION_DELETE) {
				Resource res = Resource.fromContentValues(
						processor.query(null, ResourceTableManager.RESOURCE_ID+" = ?",
								new String[]{event.getResourceId()+""},null,null,null)
								.get(0));
				oldBlob = res.getBlob();
				VCalendar vc = ((VCalendar)VComponent.createComponentFromResource(res));
				newBlob = vc.applyEventAction(event, action, instances);
				uid = vc.getMasterChild().getUID();
				
			} else {
				VEvent comp = new VEvent(event);
				newBlob = comp.getTopParent().getCurrentBlob();  // We need to get the blob from the VCALENDAR not the VEVENT.
				uid = comp.getUID();
			}
			
			if ( ResourceManager.DEBUG ) Log.println(Constants.LOGD, TAG, 
					"Adding Pending Table row for collection ID:"+event.getCollectionId()+", resource ID: "+event.getResourceId());
			long result = processor.addPending(event.getCollectionId(), event.getResourceId(), oldBlob, newBlob, uid);
			if ( result < 0 ) 
				this.fail();
			else 
				this.postResponse(new RREventEditedResponse(result));
			
		}
		catch ( Exception e ) {
			Log.w(TAG, "Failed to add to Pending Table row for collection ID:" + event.getCollectionId()
					+ ", resource ID: " + event.getResourceId(), e);
			this.fail();
			return;
		}
	}
	
	private void fail() {
		this.postResponse(new RREventEditedResponse(null));
	}
	
	
	public class RREventEditedResponse extends ResourceResponse<Long> {

		private Long resource;
		
		//Can be null if failed.
		public RREventEditedResponse(Long r) {
			this.resource = r;
		}
		
		@Override
		public Long result() {
			return resource;
		}
		
	}


}
