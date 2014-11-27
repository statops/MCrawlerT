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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDateTimeFormatter;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.cachemanager.CacheChangedEvent;
import com.morphoss.acal.database.cachemanager.CacheChangedListener;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheRequest;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.database.cachemanager.CacheResponseListener;
import com.morphoss.acal.database.cachemanager.requests.CRObjectsInRange;
import com.morphoss.acal.dataservice.Collection;

/**
 * <p>
 * Adapter for providing views for events.
 * </p>
 * 
 * @author Morphoss Ltd
 * 
 */
public class EventListAdapter extends BaseAdapter implements OnClickListener, ListAdapter, CacheChangedListener, CacheResponseListener<ArrayList<CacheObject>> {

	/**
	 * <p>
	 * Presently this view is only supported in month view. If we decide to extend this view further we should
	 * create An interface for providing callbacks.
	 * </p>
	 */ 
	private MonthView context;
	private AcalDateTime viewDate;
	private AcalDateTime viewDateEnd;
	public static final String TAG = "aCal EventListAdapter";
	private volatile boolean clickEnabled = true;
	public static final int CONTEXT_EDIT = 0;
	public static final int CONTEXT_DELETE_ALL = 0x10000;
	public static final int CONTEXT_DELETE_JUSTTHIS = 0x20000;
	public static final int CONTEXT_DELETE_FROMNOW = 0x30000;
	public static final int CONTEXT_COPY = 0x40000;

	private ArrayList<CacheObject> dayEvents = new ArrayList<CacheObject>();
	private CacheManager cacheManager;
	
	private static final int HANDLER_NEW_LIST = 0;
	
	private Handler mHandler = new Handler() {
		
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case HANDLER_NEW_LIST:
					dayEvents = new ArrayList<CacheObject>();
					for ( CacheObject co : (ArrayList<CacheObject>)msg.obj ) {
						if ( co.isEvent() ) dayEvents.add(co);
					}
					EventListAdapter.this.notifyDataSetChanged();
					
					break;
			}
		}
	};
	
//	private SharedPreferences prefs;	

	/**
	 * <p>Create a new adaptor with the attributes provided. The date range provided specifies the date range that all
	 * events provided by this adapter fall within. This may change as the calling class may have the required events
	 * in memory already.</p>
	 */
	public EventListAdapter(MonthView monthview, AcalDateTime date) {
		this.context = monthview;
		viewDate = date.clone().applyLocalTimeZone().setDaySecond(0);
		viewDateEnd = AcalDateTime.addDays(viewDate, 1);

		Log.w(TAG,"Now viewing events on date "+viewDate);

		cacheManager = CacheManager.getInstance(context, this);
		cacheManager.sendRequest(getCacheRequest());
	}

	private CacheRequest getCacheRequest() {
		return new CRObjectsInRange(new AcalDateRange(viewDate,viewDateEnd), this);
	}
	
	/**
	 * <p>Returns the number of elements in this adapter.</p>
	 * 
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		synchronized (dayEvents) {
			return dayEvents.size();
		}
	}

	/**
	 * <p>Returns the event at specified the position in this adapter or null if position is invalid.</p> 
	 * 
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		synchronized (dayEvents) {
			if ( position >= dayEvents.size() ) return null;
			return dayEvents.get(position);
		}
	}

	/**
	 * <p>Returns the id associated with the event at specified position. Currently not implemented (i.e. returns position)</p>
	 * 
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}


	/**
	 * <p>Returns the view associated with the event at the specified position. Currently, views
	 * do not respond to any events.</p> 
	 * 
	 * (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LinearLayout rowLayout;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		rowLayout = (LinearLayout) inflater.inflate(R.layout.event_list_item, parent, false);

		TextView title = (TextView) rowLayout.findViewById(R.id.EventListItemTitle);
		TextView time = (TextView) rowLayout.findViewById(R.id.EventListItemTime);
		TextView location = (TextView) rowLayout.findViewById(R.id.EventListItemLocation);
		
		LinearLayout sideBar = (LinearLayout) rowLayout.findViewById(R.id.EventListItemColorBar);
		CacheObject event = null;
		synchronized(dayEvents) {
			event = dayEvents.get(position);
		}
		if ( event == null ) return rowLayout;
		
		Collection eventCollection = Collection.getInstance(event.getCollectionId(), this.context);
		if ( eventCollection != null ) {
			rowLayout.findViewById(R.id.EventListItemIcons).setBackgroundColor(eventCollection.getColour());
			sideBar.setBackgroundColor(eventCollection.getColour()); 
			title.setTextColor(eventCollection.getColour());
		}

		title.setText((event.getSummary() == null  || event.getSummary().length() <= 0 ) ? "Untitled" : event.getSummary());

		if ( event.hasAlarms() ) {
			ImageView alarmed = (ImageView) rowLayout.findViewById(R.id.EventListItemAlarmBell);
			alarmed.setVisibility(View.VISIBLE);
			if ( eventCollection != null && ! eventCollection.alarmsEnabled() ) alarmed.setBackgroundColor(0xb0ffffff);
		}
		if ( event.isRecurring() ) {
			ImageView repeating = (ImageView) rowLayout.findViewById(R.id.EventListItemRepeating);
			repeating.setVisibility(View.VISIBLE);
		}

        if ( event.getStartDateTime() != null ) {
            time.setText(
                    AcalDateTimeFormatter.getDisplayTimeText(context, viewDate, viewDateEnd,
                            event.getStartDateTime(), event.getEndDateTime(),
                            MonthView.prefs.getBoolean(context.getString(R.string.prefTwelveTwentyfour), false),
                            event.isAllDay())
                        );
        }

		if (event.getLocation() != null && event.getLocation().length() > 0 )
			location.setText(event.getLocation());
		else
			location.setHeight(2);

		rowLayout.setTag(event);
		rowLayout.setOnTouchListener(this.context);
		rowLayout.setOnClickListener(this);

		// 'final' so we can refer to it below
		final boolean repeats = event.isRecurring();

		//add context menu
		this.context.registerForContextMenu(rowLayout);
		rowLayout.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
		

			@Override
			public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
				menu.setHeaderTitle(context.getString(R.string.Event));

				CacheObject event = null;
				synchronized(dayEvents) {
					event = dayEvents.get(position);
				}
				menu.add(0, CONTEXT_EDIT + position, 0, context.getString(R.string.editSomeEvent, event.getSummary()));
				menu.add(0, CONTEXT_COPY + position, 0, context.getString(R.string.newEventFromThis));
				if (repeats) {
					menu.add(0,CONTEXT_DELETE_ALL+position,0, context.getString(R.string.deleteAllInstances));
					menu.add(0,CONTEXT_DELETE_JUSTTHIS+position, 0, context.getString(R.string.deleteThisInstance));
					menu.add(0,CONTEXT_DELETE_FROMNOW+position,0, context.getString(R.string.deleteThisAndFuture));
				} else {
					menu.add(0,CONTEXT_DELETE_ALL+position,0, context.getString(R.string.Delete));
				}
			}
		});

		return rowLayout;
	}

	public void setClickEnabled(boolean enabled) {
		this.clickEnabled = enabled;
	}

	@Override
	public void onClick(View arg0) {
		if (clickEnabled) {
			CacheObject tag = (CacheObject) arg0.getTag();
			if (tag.isEvent()) {
				//start event activity
				Bundle bundle = new Bundle();
				bundle.putParcelable(EventView.CACHE_INSTANCE_KEY, tag);
				Intent eventViewIntent = new Intent(context, EventView.class);
				eventViewIntent.putExtras(bundle);
				context.startActivityForResult(eventViewIntent, MonthView.PICK_TODAY_FROM_EVENT_VIEW);
			}
		} else {
			clickEnabled = true;
		}

	}
	 
	/**
	 * TODO - refactor
	*/
	public boolean contextClick(MenuItem item) {

		try {
			int id = item.getItemId();
			int selectedAction = id & 0xf0000;
			id = id & 0xffff;

			CacheObject sae = (CacheObject)this.getItem(id);
			int action = EventEdit.ACTION_EDIT;
			switch( selectedAction ) {
				case CONTEXT_COPY:
					action = EventEdit.ACTION_COPY;
				case CONTEXT_EDIT:
					//start EventEdit activity
					Bundle bundle = new Bundle();
					bundle.putInt(EventEdit.ACTION_KEY,action);
					bundle.putLong(EventEdit.RESOURCE_ID_KEY, sae.getResourceId());
					bundle.putString(EventEdit.RECCURENCE_ID_KEY, sae.getRecurrenceId());
					Intent eventViewIntent = new Intent(context, EventEdit.class);
					eventViewIntent.putExtras(bundle);
					context.startActivity(eventViewIntent);
					return true;
				
				case CONTEXT_DELETE_ALL:
					this.context.deleteEvent(sae.getResourceId(), sae.getRecurrenceId(), EventEdit.ACTION_DELETE, EventEdit.INSTANCES_ALL);
					return true;

				case CONTEXT_DELETE_JUSTTHIS:
					this.context.deleteEvent(sae.getResourceId(), sae.getRecurrenceId(), EventEdit.ACTION_DELETE, EventEdit.INSTANCES_SINGLE);
					return true;

				case CONTEXT_DELETE_FROMNOW:
					this.context.deleteEvent(sae.getResourceId(), sae.getRecurrenceId(), EventEdit.ACTION_DELETE, EventEdit.INSTANCES_THIS_FUTURE);
					return true;
			}
			return false;
		}
		catch (ClassCastException e) {
			return false;
		}
	
	}
	
	

	public void cacheChanged(CacheChangedEvent change) {
		if (change.isWindowOnly()) return;
		AcalDateRange myRange = new AcalDateRange(viewDate,viewDateEnd);

		//up-date only if the change could have affected us
		boolean update = false;
		for (DataChangeEvent dce : change.getChanges()) {
			CacheObject event = CacheObject.fromContentValues(dce.getData());
			if ( myRange.overlaps(event.getStartDateTime(), event.getEndDateTime()) ) {
				update = true;
				break;
			}
		}
		
		if (update) cacheManager.sendRequest(new CRObjectsInRange(myRange,this));
		
		
	}

	/** 
	 * Warning - this runs under a different thread - need to use Handler to ensure calls are made by GUI Thread
	 */
	@Override
	public void cacheResponse(CacheResponse<ArrayList<CacheObject>> response) {
		mHandler.sendMessage(
				mHandler.obtainMessage(HANDLER_NEW_LIST, response.result())
		);
	}

}
