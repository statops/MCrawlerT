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

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;

import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;

public class MonthImageGenerator {

	private HashMap<Integer,Bitmap> daySection = new HashMap<Integer,Bitmap>();
	private Bitmap[] days = new Bitmap[31];
	private Bitmap shadowedDay;
	private HashMap<Integer,Bitmap> yearHeaders = new HashMap<Integer,Bitmap>();
	private Bitmap dayHeaders = null;
	private Bitmap[] monthHeaders = new Bitmap[AcalDateTime.DECEMBER+1];
	private int width;
	private int height;
	private int screenWidth;
	private Context context;
	private int firstCol;
	private int headerHeight;
	private int dayHeaderHeight;
	
	private Bitmap titleBg = null;
	private Bitmap monthHeaderBg = null;
	
	public MonthImageGenerator(int monthWidth, int height, int screenWidth, Context context) {
		this.width = monthWidth;
		this.height = height;
		this.screenWidth = screenWidth;
		this.context = context;
		titleBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.titlebg);
		monthHeaderBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.monthdayheadingsbg);
		this.getFirstDay(context);
	}
	
	public Bitmap getYearHeader(int year) {
		if (yearHeaders.containsKey(year)) return yearHeaders.get(year);
		generateYearHeader(year);
		return yearHeaders.get(year);
	}
	
	public Bitmap getMonthHeader(int month) {
		if (monthHeaders[month] == null) generateMonthHeader(month);
		return monthHeaders[month];
	}
		
	public Bitmap getDayHeaders() {
		if (dayHeaders == null) generateDayHeaders();
		return dayHeaders;
	}
	
	public Bitmap getDaySection(int year, int month) {
		int numDaysInMonth = AcalDateTime.monthDays(year, month);
		int numDaysInPrevious = (month == 1 ? 31 : AcalDateTime.monthDays(year, month - 1));
		AcalDateTime firstOfMonth = new AcalDateTime(year, month, 1, 0 , 0, 0, null);
		int dayOfFirst = (firstOfMonth.getWeekDay()+this.firstCol)%7;
		int hash = dayOfFirst+(numDaysInMonth*10) + (numDaysInPrevious * 1000);
		if (daySection.containsKey(hash)) return daySection.get(hash);
		generateDaySectionBitmap(year, month, dayOfFirst, numDaysInMonth, numDaysInPrevious, hash);
		return daySection.get(hash);
	}

	private void generateYearHeader(int year) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (View) inflater.inflate(R.layout.year_view_assets, null);
		TextView title = ((TextView) v.findViewById(R.id.YVyear_title));
		title.setText(year+"");
		title.measure(MeasureSpec.makeMeasureSpec(this.screenWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(this.height, MeasureSpec.UNSPECIFIED));
		title.layout(0, 0, screenWidth, title.getMeasuredHeight());
		Bitmap returnedBitmap = Bitmap.createScaledBitmap(titleBg, screenWidth, title.getMeasuredHeight(), false);
		Canvas tempCanvas = new Canvas(returnedBitmap);
		title.draw(tempCanvas);
		this.yearHeaders.put(year, returnedBitmap);
	}
	
	private void generateMonthHeader(int month) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (View) inflater.inflate(R.layout.year_view_assets, null);
		TextView title = ((TextView) v.findViewById(R.id.YVmonth_title));
		title.setText(AcalDateTime.getMonthName(month));
		title.measure(MeasureSpec.makeMeasureSpec(this.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(this.height, MeasureSpec.UNSPECIFIED));
		title.layout(0, 0, width, title.getMeasuredHeight());
		Bitmap returnedBitmap = Bitmap.createScaledBitmap(titleBg, width, title.getMeasuredHeight(), false);
		Canvas tempCanvas = new Canvas(returnedBitmap);
		title.draw(tempCanvas);
		this.headerHeight = title.getMeasuredHeight();
		this.monthHeaders[month] = returnedBitmap;
	}
	
	private void generateDayHeaders() {
		TextView[] headerViews = new TextView[7];
		int curDay = firstCol;
		for (int i = 0; i< 7; i++) {
			String colText = "";
			switch (curDay) {
				case AcalDateTime.MONDAY: colText=(context.getString(R.string.chMon)); break;
				case AcalDateTime.TUESDAY: colText=(context.getString(R.string.chTue)); break;
				case AcalDateTime.WEDNESDAY: colText=(context.getString(R.string.chWed)); break;
				case AcalDateTime.THURSDAY: colText=(context.getString(R.string.chThu)); break;
				case AcalDateTime.FRIDAY: colText=(context.getString(R.string.chFri)); break;
				case AcalDateTime.SATURDAY: colText=(context.getString(R.string.chSat)); break;
				case AcalDateTime.SUNDAY: colText=(context.getString(R.string.chSun)); break;
			}
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = (View) inflater.inflate(R.layout.year_view_assets, null);
			TextView cur = ((TextView) v.findViewById(R.id.YVDayBoxColumnHeader));
			cur.setText(colText);
			headerViews[i] = cur;
			curDay++;
			if (curDay > AcalDateTime.SUNDAY) curDay = AcalDateTime.MONDAY;
		}
		//generate the first header BMP so that we know the height;
		Bitmap[] headerBMP = new Bitmap[7];
		for (int i =0; i<7; i++) {
			headerViews[i].measure(MeasureSpec.makeMeasureSpec((this.width/7), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(this.height, MeasureSpec.UNSPECIFIED));
			headerViews[i].layout(0, 0, width/7, headerViews[i].getMeasuredHeight());
			headerBMP[i] = Bitmap.createScaledBitmap(monthHeaderBg, screenWidth, headerViews[i].getMeasuredHeight(), false);
			Canvas tempCanvas = new Canvas(headerBMP[i]);
			headerViews[i].draw(tempCanvas);
		}
		this.dayHeaderHeight = headerViews[0].getHeight();
		this.dayHeaders = Bitmap.createBitmap(width, headerViews[0].getMeasuredHeight(),Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(this.dayHeaders);
		for (int i = 0; i < 7; i++)
			canvas.drawBitmap(headerBMP[i], ((this.width/7)*i), 0, new Paint());
		
	}

	/**
	 * Generate a bitmap of the days in this month.
	 * 
	 * @param year The year for this month.
	 * @param month The month for this month (1 to 12)
	 * @param dowOfFirst The day of week of the first day of the month.
	 * @param daysInMonth The number of days in the month.
	 * @param daysInPrevious The number of days in the previous month.
	 */
	private void generateDaySectionBitmap(int year, int month, int dowOfFirst, int daysInMonth, int daysInPrevious, int hash) {
		int myHeight = 6*((this.height - this.headerHeight -this.dayHeaderHeight)/6);
		Bitmap myBitmap = Bitmap.createBitmap(width, myHeight,Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(myBitmap);
		Paint p = new Paint();
		
		boolean shadow = true;
		boolean before = true;
		int currentDay = (daysInPrevious - dowOfFirst)+1;
		for (int i = 0; i<6; i++) {
			for (int j=0; j<7;j++) {
				if (before && currentDay > daysInPrevious) { currentDay = 1; shadow = false; before = false; } //now in cur month
				else if (!before && currentDay > daysInMonth) { shadow = true; currentDay = 1; }
				Bitmap dayBMP = getDayBitmap(shadow,currentDay);
				c.drawBitmap(dayBMP, j*dayBMP.getWidth(), i*dayBMP.getHeight(), p);
				currentDay++;
			}
		}
		daySection.put(hash,myBitmap);
	}

	
	private Bitmap getDayBitmap(boolean shadow, int day) { 
		if (shadow) {
			if (shadowedDay != null) return shadowedDay;
			generateShadowedDay(day);
			 return shadowedDay;
		} else {
			if (days[day-1] != null) return days[day-1];
			generateDay(day);
			return days[day-1];
		}
	}
	private void generateShadowedDay(int day) {
		int wid = this.width/7;
		int hi = (this.height - this.headerHeight -this.dayHeaderHeight)/6;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (View) inflater.inflate(R.layout.year_view_assets, null);
		TextView dayView = ((TextView) v.findViewById(R.id.YVDayShadowed));
		shadowedDay = genDay(dayView,"",wid,hi);
		
	}
	private void generateDay(int day) {
		int wid = this.width/7;
		int hi = (this.height - this.headerHeight -this.dayHeaderHeight)/6;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = (View) inflater.inflate(R.layout.year_view_assets, null);
		TextView dayView = ((TextView) v.findViewById(R.id.YVDayBoxInMonth));
		days[day-1] = genDay(dayView,day+"",wid,hi);		
	}
	
	private Bitmap genDay(TextView dayView, String day, int wid, int hi) {
		dayView.setText(day);
		dayView.layout(0, 0, wid, hi);
		Bitmap dayBMP = Bitmap.createBitmap(wid, hi,Bitmap.Config.ARGB_8888);
		Canvas tempCanvas = new Canvas(dayBMP);
		dayView.draw(tempCanvas);
		return dayBMP;
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
}
