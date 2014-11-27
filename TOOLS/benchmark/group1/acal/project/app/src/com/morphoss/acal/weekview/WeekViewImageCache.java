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
package com.morphoss.acal.weekview;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;

import com.morphoss.acal.R;

public class WeekViewImageCache {

	private float dayWidth;
	private Context c;

	private Bitmap sidebar;
	private int sidebarWidth = -1;

	private Bitmap hourbox;
	private Bitmap daybox;
	private int lastDaySPP = 0;
	private int lastDayHeight = 0;

	private HashMap<Long,Bitmap> eventMap = new HashMap<Long, Bitmap>();
	private Queue<Long> eventQueue = new LinkedList<Long>();

	public WeekViewImageCache(Context c) {
		this.c=c;
		this.dayWidth=WeekViewActivity.DAY_WIDTH;
	}

	public void cacheDayBoxes(int minHeight) {
		float halfHeight = 1800F/(float)WeekViewActivity.SECONDS_PER_PIXEL;

		Bitmap returnedBitmap;
		Canvas canvas;
		Paint p;

		if (hourbox == null) {
			//First hour box
			returnedBitmap = Bitmap.createBitmap((int)dayWidth, (int)(halfHeight*2),Bitmap.Config.ARGB_4444);
			canvas = new Canvas(returnedBitmap);
			p = new Paint();
			p.setStyle(Paint.Style.STROKE);
			p.setColor(c.getResources().getColor(R.color.WeekViewDayGridBorder));
			canvas.drawRect(0,0,dayWidth,(int)(halfHeight*2),p);

			p.setStyle(Paint.Style.STROKE);
			p.setColor(c.getResources().getColor(R.color.WeekViewDayGridBorder));
			//draw dotted center line
			DashPathEffect dashes = new DashPathEffect(WeekViewActivity.DASHED_LINE_PARAMS,0);
			p.setPathEffect(dashes);
			canvas.drawLine(0,halfHeight, dayWidth, halfHeight, p);
			this.hourbox = returnedBitmap;
		}

		//now do whole day
		returnedBitmap = Bitmap.createBitmap((int)dayWidth,(int)( minHeight+halfHeight*2),Bitmap.Config.ARGB_4444);
		canvas = new Canvas(returnedBitmap);
		p = new Paint();
		p.setStyle(Paint.Style.FILL);
		for (float y = 0; y<= minHeight; y+=(halfHeight*2)) {
			canvas.drawBitmap(hourbox, 0, y, p);
		}
		this.daybox = returnedBitmap;
		this.lastDayHeight = minHeight;
		this.lastDaySPP = WeekViewActivity.SECONDS_PER_PIXEL;
	}

	public Bitmap getDayBox(int minHeight) {
		if (WeekViewActivity.SECONDS_PER_PIXEL != lastDaySPP || minHeight > lastDayHeight)
			cacheDayBoxes(minHeight);
		return daybox;
	}
	
	public Bitmap getSideBar(int width) {
		if (width == sidebarWidth) return sidebar;	
		float halfHeight = 1800F/(float)WeekViewActivity.SECONDS_PER_PIXEL;
		float SPP = (float)WeekViewActivity.SECONDS_PER_PIXEL;
		boolean half = false;
		boolean byHalves = (halfHeight > WeekViewActivity.PIXELS_PER_TEXT_ROW+3);
		float rowHeight = halfHeight * (byHalves?1f:2f);
		float offset = -rowHeight/2;
		if (!byHalves) offset=-(rowHeight/4f);
		int hour = 0;
		Bitmap master = Bitmap.createBitmap((int)width, (86400/WeekViewActivity.SECONDS_PER_PIXEL),Bitmap.Config.ARGB_4444);
		Canvas masterCanvas = new Canvas(master);
		
		String am = c.getString(R.string.oneCharMorning);
		String pm = c.getString(R.string.oneCharAfternoon);
		
		int currentSecond = 0;
		while ( currentSecond<=86400 ) {
			LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = (View) inflater.inflate(R.layout.week_view_assets, null);
			TextView box;
			if ( !half ) box= ((TextView) v.findViewById(R.id.WV_side_box));
			else box= ((TextView) v.findViewById(R.id.WV_side_box_half));
			box.setVisibility(View.VISIBLE);
			String text = "";

			if (WeekViewActivity.TIME_24_HOUR) {
				if (half) text=":30 ";
				else text = hour+" ";
			} else {
				if (half) text=":30 ";
				else if (hour == 0)text="12 "+am+" ";
				else {
					int hd = hour;
					if (hour >= 13) hd-=12; 
					text=(int)hd+" "+(hour<12?am:pm)+" ";
				}
			}
			
			box.setText(text);
			box.setTextSize(TypedValue.COMPLEX_UNIT_SP, WeekViewActivity.TEXT_SIZE_SIDE);
			box.measure(MeasureSpec.makeMeasureSpec((int) width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) rowHeight, MeasureSpec.EXACTLY));
			box.layout(0,0, (int)width, (int)rowHeight);
			Bitmap returnedBitmap = Bitmap.createBitmap((int)width, (int)rowHeight,Bitmap.Config.ARGB_4444);
			Canvas tempCanvas = new Canvas(returnedBitmap);
			box.draw(tempCanvas);
			masterCanvas.drawBitmap(returnedBitmap, 0, offset+(currentSecond/SPP), new Paint());

			if ( byHalves ) {
				half = !half;
				if ( !half ) hour++;
				currentSecond+=1800;
			}
			else {
				hour++;
				currentSecond+=3600;
			}
		}
		sidebar = master;
		sidebarWidth=width;
		return sidebar;
	}

	public Bitmap getEventBitmap(long resourceId, String summary, int colour,
							int width, int height, int maxWidth, int maxHeight) {
		long hash = getEventHash(resourceId,maxWidth,maxHeight);
		if (eventMap.containsKey(hash)) {
			eventQueue.remove(hash);
			eventQueue.offer(hash);	//re prioritise
			Bitmap base = eventMap.get(hash);
			if (base.getHeight() < height) height = base.getHeight();
			if (base.getWidth() < width) width = base.getWidth();
			return Bitmap.createBitmap(base, 0, 0, width, height, null, false);
		}
		if (eventMap.size() > 100) eventQueue.poll(); //make space
		//now construct the Bitmap
		if ( height > maxHeight ) maxHeight = height;
		if ( width > maxWidth ) maxWidth = width;
		LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (View) inflater.inflate(R.layout.week_view_assets, null);
		TextView title = ((TextView) v.findViewById(R.id.WV_event_box));
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, WeekViewActivity.TEXT_SIZE_EVENT);
		title.setBackgroundColor((colour&0x00ffffff)|0xA0000000); //add some transparancy
		title.setVisibility(View.VISIBLE);
		title.setText(summary);
		title.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
		title.layout(0, 0, maxWidth, maxHeight);
		Bitmap returnedBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
		Canvas tempCanvas = new Canvas(returnedBitmap);
		title.draw(tempCanvas);
		//draw a border
		Paint p = new Paint();
		p.setStyle(Paint.Style.STROKE);
		p.setColor(colour|0xff000000);
		for (int i = 0; i<WeekViewActivity.EVENT_BORDER; i++) {
			tempCanvas.drawRect(i, i, maxWidth-i, maxHeight-i, p);
		}
		eventMap.put(hash, returnedBitmap);
		eventQueue.offer(hash);
		return Bitmap.createBitmap(returnedBitmap, 0, 0, width, height, null, false);
	}
	public long getEventHash(long resourceId, int width, int height) {
		return (long)width + ((long)dayWidth * (long)resourceId);
	}
	 
}