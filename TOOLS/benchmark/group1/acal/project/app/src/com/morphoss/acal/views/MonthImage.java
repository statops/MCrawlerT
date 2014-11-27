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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateTime;

public class MonthImage extends YearViewNode {

	public static final String TAG = "aCal MonthImage";
	
	private int x;
	private Bitmap headerBMP;
	private Bitmap dayHeadsBMP;
	private Bitmap daySectionBMP;

	final private int myYear;
	final private int myMonth;
	
	public MonthImage(Context context, int year, int month, int selectedDay, int x, MonthImageGenerator ig) {
		super();
		this.x=x;
		this.myYear = year;
		this.myMonth = month;
		this.headerBMP = ig.getMonthHeader(month);
		this.dayHeadsBMP = ig.getDayHeaders();
		this.daySectionBMP = ig.getDaySection(year, month);
		Log.println(Constants.LOGD, TAG, "Created month image for "+year+"-"+month);
	}
	
	@Override
	protected void draw(Canvas canvas, int y) {
		Paint paint = new Paint();
		
		canvas.drawBitmap(headerBMP, x, y, paint);
		canvas.drawBitmap(dayHeadsBMP, x, y+headerBMP.getHeight(), paint);
		canvas.drawBitmap(daySectionBMP, x, y+headerBMP.getHeight()+dayHeadsBMP.getHeight(), paint);
	}

	public int getHeight() { return headerBMP.getHeight()
			+dayHeadsBMP.getHeight()
			+daySectionBMP.getHeight(); }

	public int getMonth() { return this.myMonth; }
	public int getYear() { return this.myYear; }
	public AcalDateTime getDate() {
		return new AcalDateTime(myYear,myMonth,1,0,0,0,null);
	}

	@Override
	public boolean isUnder(int x) {
		return(x>= this.x  && x <= this.x+this.headerBMP.getWidth() );
	}

	@Override
	public MonthImage getMonthImage() {
		return this;
	}
	
}
