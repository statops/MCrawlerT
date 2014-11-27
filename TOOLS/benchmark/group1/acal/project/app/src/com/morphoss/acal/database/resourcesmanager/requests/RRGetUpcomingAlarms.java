package com.morphoss.acal.database.resourcesmanager.requests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ReadOnlyResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceProcessingException;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.requesttypes.ReadOnlyBlockingRequestWithResponse;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponentCreationException;

public class RRGetUpcomingAlarms extends ReadOnlyBlockingRequestWithResponse<ArrayList<AlarmRow>> {

	private Map<Long,Collection> alarmCollections = null;
	private AcalDateTime alarmsAfter = null;

	public RRGetUpcomingAlarms(AcalDateTime after) {
		super();
		alarmsAfter = after.clone();
	}

	@Override
	public void process(ReadOnlyResourceTableManager processor)	throws ResourceProcessingException {
		alarmCollections = Collection.getAllCollections(processor.getContext());
		ArrayList<AlarmRow> alarmList = new ArrayList<AlarmRow>(); 

		long start = alarmsAfter.getMillis();
		long end = start;
		start -= AcalDateTime.SECONDS_IN_HOUR * 36 * 1000L;
		end   += AcalDateTime.SECONDS_IN_DAY * 70 * 1000L;

		StringBuilder whereClause = new StringBuilder(ResourceTableManager.COLLECTION_ID);
		whereClause.append(" IN (");
		boolean pastFirst = false;
		for( Collection collection : alarmCollections.values() ) {
			if ( (!collection.useForEvents && !collection.useForTasks) || !collection.alarmsEnabled ) continue;
			if ( pastFirst ) whereClause.append(',');
			else pastFirst = true;
			whereClause.append(collection.collectionId);
		}
		if ( pastFirst ) {
			whereClause.append(')');
			whereClause.append(" AND (");
			whereClause.append(ResourceTableManager.LATEST_END);
			whereClause.append(" IS NULL OR ");
			whereClause.append(ResourceTableManager.LATEST_END);
			whereClause.append(" >= ");
			whereClause.append(start);
			whereClause.append(") AND (");
			whereClause.append(ResourceTableManager.EARLIEST_START);
			whereClause.append(" IS NULL OR ");
			whereClause.append(ResourceTableManager.EARLIEST_START);
			whereClause.append(" <= ");
			whereClause.append(end);
			whereClause.append(") AND (");
			whereClause.append(ResourceTableManager.RESOURCE_DATA);
			whereClause.append(" LIKE '%BEGIN:VALARM%' )");

			ArrayList<ContentValues> cvs = processor.query(null, whereClause.toString(), null, null,null,null);

			for (ContentValues cv : cvs) {
				Resource r = Resource.fromContentValues(cv);
				try {
					VCalendar vc = (VCalendar) VCalendar.createComponentFromResource(r);
					vc.appendAlarmInstancesBetween(alarmList, new AcalDateRange(alarmsAfter, AcalDateTime.addDays(alarmsAfter, 7)));
				}
				catch ( VComponentCreationException e ) {
					// @todo Auto-generated catch block
					Log.w(ResourceManager.TAG,"Auto-generated catch block", e);
					continue;
				}
				catch ( Exception e ) {
					// @todo Auto-generated catch block
					Log.w(ResourceManager.TAG,"Auto-generated catch block", e);
					continue;
				}
			}
		}
		Collections.sort(alarmList);
		RRGetUpcomingAlarmsResult response = new RRGetUpcomingAlarmsResult(alarmList);
		
		this.postResponse(response);
	}

	public class RRGetUpcomingAlarmsResult extends ResourceResponse<ArrayList<AlarmRow>> {

		private ArrayList<AlarmRow> result;
		
		public RRGetUpcomingAlarmsResult(ArrayList<AlarmRow> result) { 
			this.result = result;
			setProcessed();
		}
		
		@Override
		public ArrayList<AlarmRow> result() {return this.result;	}
		
	}

}
