/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.activity;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDateTimeFormatter;
import com.morphoss.acal.acaltime.AcalRepeatRule;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedEvent;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedListener;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requests.RRRequestInstance;
import com.morphoss.acal.dataservice.CalendarInstance;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.dataservice.EventInstance;
import com.morphoss.acal.davacal.AcalAlarm;
import com.morphoss.acal.service.aCalService;

public class EventView extends AcalActivity implements  OnClickListener, ResourceChangedListener, ResourceResponseListener<CalendarInstance> {

	public static final String TAG = "aCal EventView";
	public static final int TODAY = 0;
	public static final int EDIT = 1;
	public static final int ADD = 2;
	public static final int SHOW_ON_MAP = 3;
	
	public static final int EDIT_EVENT = 0;
	public static final int EDIT_ADD = 0;
	
	public static final String CACHE_INSTANCE_KEY = "CacheInstance";
	public static final String RESOURCE_ID_KEY = "resourceid";
	public static final String RECURRENCE_ID_KEY = "recurrenceid";
	private static final int DEFAULT_SIDE_COLOUR = 0xff000000;
	
	private long rid;		//The resource ID for this event
	private String rrid;
	private EventInstance event = null;		//fully populated event instance with all the data we want
	private CacheObject cacheObject = null; //Small lightweight object that we start with.
	
	private ResourceManager resourceManager = null;	//needed for getting reesource data
	
	//Display Elements needed by populate layout
	private LinearLayout masterLayout;
	
	private Button mapButton;
	private LinearLayout sidebar;
	private LinearLayout sidebarBottom;
	private TextView textName; 
	private TextView textTime;
	
	private RelativeLayout locationLayout;
	private TextView textLocation;
	
	private RelativeLayout notesLayout;
	private TextView textNotes;
	
	private RelativeLayout alarmsLayout;
	private TextView textAlarms;
	private TextView textAlarmsWarning;
	
	private RelativeLayout collectionLayout;
	private TextView textCollection;
	
	private TextView textRepeats;
	
	private boolean show24Hour = false;
	
	private static final int REFRESH = 0;
	private static final int FAIL = 1;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == REFRESH) {
				populateLayout();
			} else if(msg.what == FAIL) {
				Toast.makeText(EventView.this, "The resource you are looking at has changed or been deleted.", Toast.LENGTH_LONG).show();
				finish();
			}
			
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.event_view);
		this.masterLayout = (LinearLayout) this.findViewById(R.id.EventViewLayout);
		
		//Ensure service is actually running
		this.startService(new Intent(this, aCalService.class));
		//gestureDetector = new GestureDetector(this);

		//Set up buttons
		this.setupButton(R.id.event_today_button, TODAY);
		this.setupButton(R.id.event_edit_button, EDIT);
		this.setupButton(R.id.event_add_button, ADD);
		
		
		this.resourceManager = ResourceManager.getInstance(this);
		this.resourceManager.addListener(this);
		
		Bundle b = this.getIntent().getExtras();
		try {
			if (b.containsKey(CACHE_INSTANCE_KEY)) {
				this.cacheObject = b.getParcelable(CACHE_INSTANCE_KEY);
				this.rid = cacheObject.getResourceId();
				this.rrid = cacheObject.getRecurrenceId();
				
				//request the fully implemented instance;
				resourceManager.sendRequest(new RRRequestInstance(this, this.rid, this.rrid));
				
			} else if (b.containsKey(RESOURCE_ID_KEY) && b.containsKey(RECURRENCE_ID_KEY)) {
				this.rid = b.getLong(RESOURCE_ID_KEY);
				this.rrid = b.getString(RECURRENCE_ID_KEY);
				//request the fully implemented instance;
				resourceManager.sendRequest(new RRRequestInstance(this, this.rid, this.rrid));
			}
			else {
				if (Constants.LOG_DEBUG)Log.d(TAG, "Calling activity has not provided required data.");
				this.finish();
				return;
			}
			show24Hour = prefs.getBoolean(getString(R.string.prefTwelveTwentyfour), false);
			this.loadLayouts();
			this.populateLayout();
		}
		catch (Exception e) {
			if (Constants.LOG_DEBUG)Log.d(TAG, "Error getting data from caller: "+e.getMessage());
		}
	}
	
	@Override
	public void onPause() {
		this.resourceManager.removeListener(this);
		this.resourceManager = null;
		super.onPause();
	}
	
	@Override
	public void onResume() {
		this.resourceManager = ResourceManager.getInstance(this);
		this.resourceManager.addListener(this);
		resourceManager.sendRequest(new RRRequestInstance(this,rid, this.rrid));
		super.onResume();
	}
	
	private void setupButton(int id, int val) {
		Button myButton = (Button) this.findViewById(id);
		myButton.setOnClickListener(this);
		myButton.setTag(val);
		AcalTheme.setContainerFromTheme(myButton, AcalTheme.BUTTON);
	}

	

	
	private void loadLayouts() {
		mapButton = (Button) this.findViewById(R.id.EventFindOnMapButton);
		sidebar = (LinearLayout)this.findViewById(R.id.EventViewColourBar);
		sidebarBottom = (LinearLayout)this.findViewById(R.id.EventViewColourBarBottom);;
		textName = (TextView) this.findViewById(R.id.EventName);
		textTime = (TextView) this.findViewById(R.id.EventTimeContent);
		textLocation  = (TextView) this.findViewById(R.id.EventLocationContent);
		locationLayout = (RelativeLayout) this.findViewById(R.id.EventLocationLayout);
		textNotes = (TextView) this.findViewById(R.id.EventNotesContent);
		notesLayout = (RelativeLayout) this.findViewById(R.id.EventNotesLayout);
		textAlarms = (TextView) this.findViewById(R.id.EventAlarmsContent);
		textAlarmsWarning = (TextView) this.findViewById(R.id.CalendarAlarmsDisabled);
		alarmsLayout = (RelativeLayout) this.findViewById(R.id.EventAlarmsLayout);
		collectionLayout = (RelativeLayout) this.findViewById(R.id.EventCollectionLayout);
		textRepeats = (TextView) this.findViewById(R.id.EventRepeatsContent);
		textCollection = (TextView) this.findViewById(R.id.EventCollectionContent);
		
	}
	
	private void populateLayout() {
		
		//vars needed
		AcalDateTime start = null;
		String title = null;
		String location = null;
		String description = null;
		List<AcalAlarm> alarmList = null;
		long collectionId = -1;
		String repetition = null;
		String timeText = null;
		
		AcalDateTime viewDate = new AcalDateTime().applyLocalTimeZone().setDaySecond(0);
		
		if (event != null) {
			//load from event
			start = event.getStart();
			title = event.getSummary();
			location = event.getLocation();
			description = event.getDescription();
			alarmList = event.getAlarms();
			collectionId = event.getCollectionId();
			repetition = event.getRRule();
			boolean isAllDay = start.isDate();
			timeText = AcalDateTimeFormatter.getDisplayTimeText(this,viewDate, AcalDateTime.addDays(viewDate,1),
													event.getStart(), event.getEnd(), show24Hour, isAllDay );
			
		} else if (cacheObject != null) {
			//load from cacheObject
			start = cacheObject.getStartDateTime();
			title = cacheObject.getSummary();
			location = cacheObject.getLocation();
			description = "Loading...";
			collectionId = cacheObject.getCollectionId();
			timeText = AcalDateTimeFormatter.getDisplayTimeText(this,viewDate, AcalDateTime.addDays(viewDate,1),
					cacheObject.getStartDateTime(), cacheObject.getEndDateTime(), show24Hour, cacheObject.isAllDay());
			
		} else {
			title = "Loading data...";
		}
		
		final String loc = location;
		if (loc!= null && !loc.equals("")) {
			mapButton.setClickable(true);
			mapButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					//replace whitespaces with '+'
					loc.replace("\\s", "+");
					Uri target = Uri.parse("geo:0,0?q="+loc);
					startActivity(new Intent(android.content.Intent.ACTION_VIEW, target)); 
					//start map view
					return;
				}
			});
		} else {
			mapButton.setClickable(false);
		}
		
		
		int colour = DEFAULT_SIDE_COLOUR; 
		
		Collection collection = null;
		if (collectionId >= 0) {
			collection = Collection.getInstance(collectionId,this);
			colour = collection.getColour();
		}
		

		//title and colour are ALWAYS set to something
		sidebar.setBackgroundColor(colour);
		sidebarBottom.setBackgroundColor(colour);

		textName.setText(title);
		textName.setTextColor(colour);
		
		
		if (timeText != null) {
			textTime.setText(timeText);
			textTime.setTextColor(colour);
		}
		
		if ( location != null && ! location.equals("") ) {
				textLocation.setText(location);
				locationLayout.setVisibility(View.VISIBLE);
		} else {
			locationLayout.setVisibility(View.GONE);
		}
		
		if ( description != null && ! description.equals("") ) {
			textNotes.setText(description);
			notesLayout.setVisibility(View.VISIBLE);
		} else {
			notesLayout.setVisibility(View.GONE);
		}
		
		
		StringBuilder alarms = new StringBuilder("");
		if (alarmList != null) {
			for (AcalAlarm alarm : alarmList) {
				if ( alarms.length() > 0 ) alarms.append('\n');
				alarms.append(alarm.toPrettyString());
			}
		}
		
		if ( alarms != null  && ! alarms.equals("") ) {
			textAlarms.setText(alarms);
			alarmsLayout.setVisibility(View.VISIBLE);
		} else {
			alarmsLayout.setVisibility(View.GONE);
		}
		
		if (collection != null) {
				textCollection.setText(collection.getDisplayName());
				collectionLayout.setVisibility(View.VISIBLE);
			
			if (collection.alarmsEnabled()) {
				textAlarmsWarning.setVisibility(View.GONE);
			} else {
				textAlarmsWarning.setVisibility(View.VISIBLE);
			}
		} else {
			collectionLayout.setVisibility(View.GONE);
		}
		
		if (start != null && repetition != null) {
			AcalRepeatRule RRule = new AcalRepeatRule(start, repetition); 
			String rr = RRule.repeatRule.toPrettyString(this);
			if (rr == null || rr.equals("")) rr = getString(R.string.OnlyOnce);
			textRepeats.setText(rr);
		} else {
			textRepeats.setText("");
		}
		
		masterLayout.refreshDrawableState();
	}

	
	@Override
	public void onClick(View arg0) {
		int button = (int)((Integer)arg0.getTag());
		Bundle bundle = new Bundle();
		bundle.putLong(EventEdit.RESOURCE_ID_KEY, rid);
		bundle.putString(EventEdit.RECCURENCE_ID_KEY, rrid);
		Intent eventEditIntent = new Intent(this, EventEdit.class);
		switch (button) {
			case EDIT: {
				//start event activity
				bundle.putInt(EventEdit.ACTION_KEY, EventEdit.ACTION_EDIT);
				eventEditIntent.putExtras(bundle);
				this.startActivityForResult(eventEditIntent,EDIT_EVENT);
				break;
			}
			case ADD: {
				AcalDateTime dateTime = new AcalDateTime();
				if (event != null) dateTime = event.getStart();
				else if (cacheObject != null) dateTime =  cacheObject.getStartDateTime();
				bundle.putInt(EventEdit.ACTION_KEY, EventEdit.ACTION_CREATE);
				bundle.putParcelable(EventEdit.NEW_EVENT_DATE_TIME_KEY, dateTime);
				eventEditIntent.putExtras(bundle);
				this.startActivityForResult(eventEditIntent,EDIT_ADD);
				break;
			}
			case TODAY: {
				AcalDateTime selectedDate = new AcalDateTime();
				Intent res = new Intent();
				res.putExtra("selectedDate", (Parcelable) selectedDate);
				this.setResult(RESULT_OK, res);
				this.finish();
				break;
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == EDIT_EVENT && resultCode == RESULT_OK) {
			Bundle b = data.getExtras();
			this.cacheObject = null;
			this.event = null;
			if (b.containsKey(RECURRENCE_ID_KEY))
				this.rrid = b.getString(RECURRENCE_ID_KEY);
			if (b.containsKey(RESOURCE_ID_KEY))
				this.rid = b.getLong(RESOURCE_ID_KEY);
			if (this.rrid == null)
			finish(); 
    	}
    }


	@Override
	public void resourceChanged(ResourceChangedEvent event) {
		for (DataChangeEvent dce: event.getChanges()) {
			if (dce.getData().getAsLong(ResourceTableManager.RESOURCE_ID) == rid) {
				//our data has changed!
				this.event = null;
				this.cacheObject = null;
				resourceManager.sendRequest(new RRRequestInstance(this,rid, this.rrid));
			}
		}
	}

	@Override
	public void resourceResponse(ResourceResponse<CalendarInstance> response) {
		if (!response.wasSuccessful()) {
			mHandler.sendMessage(mHandler.obtainMessage(FAIL));
			return;
		}
		CalendarInstance res = response.result();
		if (res instanceof EventInstance) {
			this.event = (EventInstance)res;
			this.rid = this.event.getResourceId();
			this.rrid = this.event.getRecurrenceId();
			mHandler.sendMessage(mHandler.obtainMessage(REFRESH));
		}
	}
	
}
