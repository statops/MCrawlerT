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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.cachemanager.CacheChangedEvent;
import com.morphoss.acal.database.cachemanager.CacheChangedListener;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.CacheResponse;
import com.morphoss.acal.database.cachemanager.CacheResponseListener;
import com.morphoss.acal.database.cachemanager.requests.CRObjectsInMonthByDay;
import com.morphoss.acal.views.MonthDayBox;

/**
 * @author Morphoss Ltd
 *
 */
public class MonthAdapter extends BaseAdapter implements CacheChangedListener, CacheResponseListener<HashMap<Short,ArrayList<CacheObject>>>, AnimationListener {

	private final static String TAG = "aCal MonthAdapter";
	private MonthView context;
	private AcalDateTime prevMonth;
	private AcalDateTime nextMonth;
	private AcalDateTime displayDate;
	private AcalDateTime selectedDate;
	private AcalDateTime displayMonthLocalized;
	private int daysInLastMonth;
	private int daysInThisMonth;
	private int firstOffset;
	private int firstCol;

	private ConcurrentHashMap<Short,ArrayList<CacheObject>> eventsByDay = new ConcurrentHashMap<Short,ArrayList<CacheObject>>();
	private CacheManager cacheManager;
	
	private volatile long wait = 0;
	private static final int HANDLER_NEW_DATA = 0;
	private Handler mHandler = new Handler() {
			
	
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Handler has received messsge.");
			switch (msg.what) {
				case HANDLER_NEW_DATA:
					if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "New data for display.");
					HashMap<Short,ArrayList<CacheObject>> data = (HashMap<Short,ArrayList<CacheObject>>)msg.obj;
					for (short i = 1; i<= 31; i++) {
						ArrayList<CacheObject> dayEvents = new ArrayList<CacheObject>();
						if (data.containsKey(i)) {
							for ( CacheObject co : data.get(i) ) {
								if ( co.isEvent() ) dayEvents.add(co);
							}
						}
						eventsByDay.put(i, dayEvents);
					}
					while (System.currentTimeMillis() < wait) {
						Log.println(Constants.LOGD,TAG, "Waiting "+(wait-System.currentTimeMillis())+"ms for animation end.");
						try { Thread.sleep(wait-System.currentTimeMillis()); } catch (Exception e) { }
					}
					MonthAdapter.this.notifyDataSetChanged();
					break;
				default: break;
			}
		}
	};
	
	
	public MonthAdapter(MonthView monthview, AcalDateTime displayDate, AcalDateTime selectedDate, Animation[] animations) {
		for (Animation a : animations) 
			if (a != null)a.setAnimationListener(this);
		this.displayDate = displayDate;
		this.selectedDate = selectedDate;
		this.context = monthview;
		this.cacheManager = CacheManager.getInstance(monthview, this);
		for (short i = 1; i<=31; i++) eventsByDay.put(i, new ArrayList<CacheObject>());
		getFirstDay(monthview);

		//Get next and previous months
		this.prevMonth = (AcalDateTime) displayDate.clone();
		this.prevMonth.set(AcalDateTime.DAY_OF_MONTH, 1);
		this.nextMonth = (AcalDateTime) this.prevMonth.clone();
		int curMonth = displayDate.get(AcalDateTime.MONTH);
		int curYear = displayDate.get(AcalDateTime.YEAR);
		if (curMonth > AcalDateTime.JANUARY) this.prevMonth.set(AcalDateTime.MONTH, curMonth-1);
		else {
			this.prevMonth.set(AcalDateTime.MONTH, AcalDateTime.DECEMBER);
			this.prevMonth.set(AcalDateTime.YEAR, curYear-1);
		}


		if (curMonth < AcalDateTime.DECEMBER) this.nextMonth.set(AcalDateTime.MONTH, curMonth+1);
		else {
			this.nextMonth.set(AcalDateTime.MONTH, AcalDateTime.JANUARY);
			this.nextMonth.set(AcalDateTime.YEAR, curYear+1);
		}

		//How many days in prev month?
		this.daysInLastMonth = this.prevMonth.getActualMaximum(AcalDateTime.DAY_OF_MONTH);

		//How many days in this month?
		this.daysInThisMonth = displayDate.getActualMaximum(AcalDateTime.DAY_OF_MONTH);

		//what day does the first fall on?
		int curDay = displayDate.get(AcalDateTime.DAY_OF_MONTH);
		displayDate.setMonthDay(1);
		this.firstOffset = displayDate.get(AcalDateTime.DAY_OF_WEEK);
		displayDate.set(AcalDateTime.DAY_OF_MONTH, curDay);
		
		displayMonthLocalized = displayDate.clone().applyLocalTimeZone().setDaySecond(0);
		displayMonthLocalized.setMonthDay(1);
		
		//request data		
		cacheManager.sendRequest(CRObjectsInMonthByDay.EventsInMonthByDay(displayMonthLocalized.getMonth(), displayMonthLocalized.getYear(), this));
	}


	private void getFirstDay(Context context) {
		//check preferred first day
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			this.firstCol = Integer.parseInt(prefs.getString(context.getString(R.string.firstDayOfWeek), "0"));
			if ( this.firstCol < AcalDateTime.MONDAY || this.firstCol > AcalDateTime.SUNDAY ) throw new Exception();
		}
		catch( Exception e ) {
			this.firstCol = AcalDateTime.MONDAY; 
		}
	}

	
	public int getCount() {
		return 7*7;	//number of rows needed * 7
	}

	public Object getItem(int position) { return null; }

	public long getItemId(int position) { return 0; }

	public View getView(int position, View contentView, ViewGroup parent) {

		int gridHeight = 0;
		int headerHeight = 0;
		int boxHeight = 0;
		float boxScaleFactor = 1.0f;
		if ( parent != null ) {
			gridHeight = parent.getHeight();
			int boxWidth = (parent.getWidth() / 7) - 1;
			boxHeight = (gridHeight / 7) + 1;
			headerHeight = boxHeight - 10;
			gridHeight = (boxHeight * 6) + headerHeight + 1;
			if ( boxWidth > (boxHeight * 1.3) ) boxScaleFactor = 1.2f;
			else if ( boxWidth < (boxHeight * 0.9) )  boxScaleFactor = 0.9f;
		}
		
		if (position <7) {
			//Column headers
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			TextView dayColumnHeader = null;
			View v = (View) inflater.inflate(R.layout.month_view_assets, null);
			dayColumnHeader = (TextView) v.findViewById(R.id.DayColumnHeader);

			int day = (position+firstCol)%7;
			String colText = "";
			switch (day) {
				case AcalDateTime.MONDAY: colText=(context.getString(R.string.Mon)); break;
				case AcalDateTime.TUESDAY: colText=(context.getString(R.string.Tue)); break;
				case AcalDateTime.WEDNESDAY: colText=(context.getString(R.string.Wed)); break;
				case AcalDateTime.THURSDAY: colText=(context.getString(R.string.Thu)); break;
				case AcalDateTime.FRIDAY: colText=(context.getString(R.string.Fri)); break;
				case AcalDateTime.SATURDAY: colText=(context.getString(R.string.Sat)); break;
				case AcalDateTime.SUNDAY: colText=(context.getString(R.string.Sun)); break;
			}
			dayColumnHeader.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
			dayColumnHeader.setText(colText);
			dayColumnHeader.setTextSize( TypedValue.COMPLEX_UNIT_PX, (float) 0.55 * boxScaleFactor * headerHeight);
			
			ViewParent vp = dayColumnHeader.getParent();
			if ( vp instanceof View ) {
				((View) vp).setBackgroundColor(AcalTheme.getElementColour(AcalTheme.BUTTON));
				dayColumnHeader.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.dayheadings_fg));
				dayColumnHeader.setTextColor(AcalTheme.pickForegroundForBackground(AcalTheme.getElementColour(AcalTheme.BUTTON)));
			}
			
			if ( headerHeight != 0 ) dayColumnHeader.setHeight(headerHeight - dayColumnHeader.getCompoundPaddingBottom());

			dayColumnHeader.setVisibility(View.VISIBLE);
			return v;
		}

		position -=7;
		//we need to correct for offset
		int offset = this.firstOffset - this.firstCol;
		if (offset<0) offset+=7;


		AcalDateTime bDate = null;
		boolean inMonth = false;
		AcalDateTime today = new AcalDateTime().applyLocalTimeZone();

		//What day of the month are we?
		if (position < offset) {
			//previous month
			bDate = prevMonth.clone();
			bDate.setMonthDay(this.daysInLastMonth - ((offset-1)-position) ); 
		} else if (position >= offset+this.daysInThisMonth) {
			//next month
			bDate = nextMonth.clone();
			bDate.setMonthDay( (position+1) - (offset+this.daysInThisMonth));
		} else {
			//this month
			inMonth = true;
			bDate = displayDate.clone();
			bDate.setMonthDay(position-offset+1);
		}
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (View) inflater.inflate(R.layout.month_view_assets, null);
		
		MonthDayBox mDayBox = null; 
		float textScaleFactor = 0.0f;
		short mDay = bDate.getMonthDay();
		if ( inMonth ) {
			if ( bDate.getYearDay() == selectedDate.getYearDay() && bDate.getYear() == selectedDate.getYear() ) {
				mDayBox = (MonthDayBox) v.findViewById(R.id.DayBoxHighlightDay);
				textScaleFactor = 0.6f;
				mDayBox.setSelected();
			}
			else {
				mDayBox = (MonthDayBox) v.findViewById(R.id.DayBoxInMonth);
				textScaleFactor = 0.55f;
			}
			if ( Constants.LOG_VERBOSE && Constants.debugMonthView ) {
				List<CacheObject> eventList = eventsByDay.get(mDay);
				Log.println(Constants.LOGV,TAG,"MonthAdapter for "+bDate.fmtIcal());
				for( CacheObject event : eventList ) {
					Log.println(Constants.LOGV,TAG, String.format("%d - %d: %s", event.getStart(), event.getEnd(), event.getSummary()));
				}
			}
			mDayBox.setEvents(eventsByDay.get(mDay));
		}
		else if ( bDate.getYearDay() == selectedDate.getYearDay() && bDate.getYear() == selectedDate.getYear() ) {
			mDayBox = (MonthDayBox) v.findViewById(R.id.DayBoxOutMonthHighlighted);
			textScaleFactor = 0.55f;
			mDayBox.setSelected();
		} else {
			mDayBox = (MonthDayBox) v.findViewById(R.id.DayBoxOutMonth);
			textScaleFactor = 0.5f;
		}
		if ( today.getYearDay() == bDate.getYearDay() && today.getYear() == bDate.getYear() ) {
			mDayBox.setToday();
		}
		if ( boxHeight != 0 ) {
			mDayBox.setHeight(boxHeight - mDayBox.getCompoundPaddingBottom());
			mDayBox.setTextSize( TypedValue.COMPLEX_UNIT_PX, textScaleFactor * boxScaleFactor * (float) boxHeight);
		}
		
		mDayBox.setVisibility(View.VISIBLE);
		mDayBox.setDate(bDate.clone());
		
		v.setTag(bDate);
		v.setOnClickListener(new MonthButtonListener());
		v.setOnTouchListener(this.context);
		return v;
	}
	
			

	private class MonthButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			AcalDateTime date = (AcalDateTime)arg0.getTag();
			if ( !AcalDateTime.isWithinMonth(date, displayDate)) {
				context.changeDisplayedMonth(date);
			}
			context.changeSelectedDate(date);
		}

	}

	public void updateSelectedDay(AcalDateTime selectedDate) {
		this.selectedDate = selectedDate;
		this.notifyDataSetChanged();
	}


	@Override
	public void cacheChanged(CacheChangedEvent event) {
		if (event.isWindowOnly()) return;
		AcalDateRange myRange = new AcalDateRange(displayMonthLocalized,displayMonthLocalized.clone().addMonths(1));
		//up-date only if the change could have affected us
		boolean update = false;
		for (DataChangeEvent dce : event.getChanges()) {
			Long sMills = dce.getData().getAsLong(CacheTableManager.FIELD_DTSTART);
			Long eMills = dce.getData().getAsLong(CacheTableManager.FIELD_DTEND);
			if (sMills == null || eMills == null) { update = true; break; }
			if (sMills < myRange.end.getMillis() && eMills > myRange.start.getMillis()) { update = true; break; }
		}
		
		if (update) cacheManager.sendRequest(new CRObjectsInMonthByDay(displayMonthLocalized.getMonth(), displayMonthLocalized.getYear(), this));
		
	}


	@Override
	public void cacheResponse(CacheResponse<HashMap<Short,ArrayList<CacheObject>>> response) {
		if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Cache Response Received.");
		
		//long waitTime = Math.max(wait-System.currentTimeMillis(),100);

		mHandler.sendMessage(mHandler.obtainMessage(HANDLER_NEW_DATA, response.result()));
	}
	

	@Override
	public void onAnimationEnd(Animation animation) {
//		if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Releasing MonthView updates due to animation end."+animation.toString());

		
	}


	@Override
	public void onAnimationRepeat(Animation animation) {
		//not used
	}


	@Override
	public void onAnimationStart(Animation animation) {
		//long now = System.currentTimeMillis();
	//	if (now < wait) {
		//	wait += animation.getDuration();
	//	} else {
	//		wait = System.currentTimeMillis() + animation.getDuration();	
	//	}
		
	//	if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Blocking MonthView updates due to animation start. Duration: "+animation.getDuration());
	}


	//An animation is about to start - we will block for half a second. onAnimation start will adjust blocking as required.
	public void animationInit() {
	//	if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG, "Animation is about to start. blocking for 1000ms.");
		//wait = System.currentTimeMillis() + 1000;
	}

	public void close() {
		this.cacheManager.removeListener(this);
	}
	
}
