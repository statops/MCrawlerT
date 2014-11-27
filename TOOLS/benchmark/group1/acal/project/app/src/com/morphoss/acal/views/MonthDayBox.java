/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package com.morphoss.acal.views;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.dataservice.Collection;

public class MonthDayBox extends TextView {

	private final static String TAG = "Acal MonthDayBox";

	private List<CacheObject> events;
	private boolean isToday = false;
	private boolean isSelectedDay = false;
	private Context context;
	private AcalDateTime boxDate;

	private static int minBarHeight = -1;
	
	public MonthDayBox(Context context) {
		super(context);
		this.context = context;
	}
	
	public MonthDayBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}
	public MonthDayBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}	
	
	@Override
	public void draw(Canvas arg0) {
		super.draw(arg0);
		Paint p = new Paint();
		p.setStyle(Paint.Style.FILL);
		float width = getWidth();
		float height = getHeight();

		if ( minBarHeight < 0 ) minBarHeight = (int) (height / 8f) + 1;

		int x = 0;
		int y = 0;
		if ( isToday || isSelectedDay ) {
			x = (int) (width/16f);
			y = (int) (height/16f);
			if ( x < 1 ) x =1;
			if ( y < 1 ) y =1;
			if ( x < y ) x =y;
			if ( y < x ) y =x;
			p.setColor( ( isSelectedDay
						? context.getResources().getColor(R.color.MonthDayHighlightBox)
						: context.getResources().getColor(R.color.MonthDayTodayBox)
					));
			
			arg0.drawRect(0, 0, width, y, p);
			arg0.drawRect(0, 0, x, height, p);
			arg0.drawRect(width-x, 0, width, height, p);
			arg0.drawRect(0, height-y, width, height, p);
		}

		if (events != null && !events.isEmpty()) {
			long dayEpoch = boxDate.setDaySecond(0).getEpoch(); 
			//Get the range of hours for todays events (min = 9am -> 5pm)
			int dayStart  = 8 * AcalDateTime.SECONDS_IN_HOUR;
			int dayFinish = 20 * AcalDateTime.SECONDS_IN_HOUR;
			int eStart, eFinish;
			for (CacheObject e : events) {
				if ( e.isAllDay() ) continue;
				eStart = (int) (e.getStartDateTime().getMillis()/1000 - dayEpoch);
				if ( eStart < 0 ) eStart = 0;
				eFinish = (int) (e.getEndDateTime().getMillis()/1000 - dayEpoch);
				if ( eFinish > AcalDateTime.SECONDS_IN_DAY ) eFinish = AcalDateTime.SECONDS_IN_DAY;
				if (eStart < dayStart) dayStart = eStart;
				if (eFinish > dayFinish) dayFinish = eFinish;
			}
			if ( dayFinish > AcalDateTime.SECONDS_IN_DAY ) dayFinish = AcalDateTime.SECONDS_IN_DAY;
			int displaySecs = dayFinish - dayStart;
			
			int barWidth = (int) (width/5f);
			int secsPerPixel = (int) ((displaySecs / height) + 1);
			for (CacheObject event : events) {
				if ( event.isAllDay() ) {
					eStart = 0;
					eFinish = dayFinish - dayStart;
				}
				else {
					eStart = (int) (event.getStartDateTime().getMillis()/1000 - dayEpoch) - dayStart;
					if ( eStart < 0 ) eStart = 0;
					eFinish = (int) (event.getEndDateTime().getMillis()/1000 - dayEpoch) - dayStart;
				}
				if ( eFinish < (eStart + (secsPerPixel * minBarHeight)) )
					eFinish = eStart + (minBarHeight * secsPerPixel);
				//draw
				Collection collection = Collection.getInstance(event.getCollectionId(), this.context);
				try {
					p.setColor((collection.getColour()|0xff000000)-0x77000000);
				}
				catch( Exception ex ) {
					Log.e(TAG,Log.getStackTraceString(ex));
				}
				arg0.drawRect(x,(y+eStart/secsPerPixel), x+barWidth, y+(eFinish/secsPerPixel), p);
				
				if ( Constants.LOG_VERBOSE && Constants.debugMonthView )
					Log.v(TAG, String.format("%d - %d: %s (%ds - %ds, %dspp, %dx,%dy, %dw,%dh, %d-%d)",
								event.getStartDateTime().getMillis()/1000, event.getEndDateTime().getMillis()/1000,
								"["+event.getStartDateTime().fmtIcal()+".."+event.getEndDateTime().fmtIcal()+"] "+event.getSummary(),
								eStart, eFinish, secsPerPixel, x, y, barWidth, (int) height,
								(int) y+(eStart/secsPerPixel), (int) y+(eFinish/secsPerPixel)));
			}
		}
		else {
			if ( Constants.LOG_VERBOSE && Constants.debugMonthView )
				Log.v(TAG,"No events for day " + this.getText() );
		}
	}

	public void setEvents(List<CacheObject> events) {
		this.events = events;
	}
	
	public void setToday() {
		isToday = true;
	}

	public void setSelected() {
		isSelectedDay = true;
	}

	public void setDate(AcalDateTime bDate ) {
		boxDate = bDate;
		setText(Integer.toString(boxDate.getMonthDay()));
	}

	
}
