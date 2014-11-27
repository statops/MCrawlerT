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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;

public class CustomYearDrawable extends ImageView  {

	private static final String	TAG	= "aCal CustomYearDrawable";
	private float y;
	private AcalDateTime currentDate;
	private Context context;
	private YearViewLinkedList childViews;
	private int compWidth = this.getWidth()/2;
	private int compHeight = this.getHeight()/3;
	private int lastWidth = this.getWidth();
	private int lastheight = this.getHeight();
	private int startMonth;
	private int startYear;
	private MonthImageGenerator imageGenerator;
	
	
	
	//View options
	public static int PORTRAIT = 0;
	public static int LANDSCAPE = 1;
	
	/**INVARIANT - COLS MUST BE ONE OF: 1,2,3,4,6 **/
	private int NUM_COLS; 
	private int NUM_ROWS_VISIBLE;

	//These should always be at least 2 to prevent blank space from appearing
	private int NUM_ROWS_ABOVE = 2;
	private int NUM_ROWS_BELOW = 4;
	
	//Dont touch this!
	private int NUM_ROWS_TOTAL;

	
	public CustomYearDrawable(Context context, AttributeSet attrs) {
		super(context,attrs);
		this.context = context;
	}
	public CustomYearDrawable(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs,defStyle);
		this.context = context;
	}	
	
	public CustomYearDrawable(Context context) {
		super(context);
		this.context = context;
	}
	
	public void initialise(AcalDateTime currentDate, int layout) {
		this.y = 0;
		
		int num_cols = 2;
		int num_rows = 3;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int num_months = Integer.parseInt(prefs.getString(context.getString(R.string.YearViewSize_PrefKey), "6"));
		switch (num_months) {
			case 1:		num_cols=1;num_rows=1;break;
			case 2:		num_cols=1;num_rows=2;break;
			case 3:		num_cols=1;num_rows=3;break;
			case 4:		num_cols=2;num_rows=2;break;
			case 6:		num_cols=2;num_rows=3;break;
			case 8:		num_cols=2;num_rows=4;break;
			case 9:		num_cols=3;num_rows=3;break;
			case 12:	num_cols=3;num_rows=4;break;
		}

		if (layout == LANDSCAPE) {
			NUM_COLS = num_rows;
			NUM_ROWS_VISIBLE = num_cols;
		} else {
			NUM_COLS = num_cols;
			NUM_ROWS_VISIBLE = num_rows;
		}
		 
		NUM_ROWS_TOTAL = NUM_ROWS_VISIBLE + NUM_ROWS_ABOVE + NUM_ROWS_BELOW;
		
		//Invariant checker
		if (NUM_COLS <0 || NUM_COLS == 5 || NUM_COLS > 6) throw new IllegalStateException("Year View NUM_COLS is invalid");
		
		setSelectedDate(new AcalDateTime());

	}
	
	public void setSelectedDate(AcalDateTime date) {
		this.currentDate = date.clone();
		this.startYear = currentDate.getYear();
		this.startMonth = this.currentDate.getMonth();
		this.startMonth--;
		this.startMonth -= ((NUM_COLS * NUM_ROWS_ABOVE) + (this.startMonth % NUM_COLS));
		this.startMonth++;
		if (this.startMonth < AcalDateTime.JANUARY) {
			this.startMonth+=12;
			this.startYear--;
		}
		if (this.startMonth > AcalDateTime.DECEMBER) {
			this.startMonth-=12;
			this.startYear++;
		}
		this.y = 0;
		this.lastheight=0;
		this.lastWidth=0;
		Log.println(Constants.LOGD, TAG, "Setting display to start at "+startYear+"-"+startMonth);
		this.populateMonths();
		this.refreshDrawableState();
	}
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (this.isInEditMode()) return;
		//draw background first
		Drawable bg = (Drawable)this.context.getResources().getDrawable(R.drawable.morphossbg);
		bg.setBounds(0, 0, this.getRight(),this.getBottom());
		bg.draw(canvas);
	
		//forces recalculation if our size changes 
		if (this.getHeight() != this.lastheight || this.getWidth() != this.lastWidth) {
			populateMonths();
		}
		MonthImage first = childViews.getFirst().getMonthImage();
		int drawY =(int)( y-(NUM_ROWS_ABOVE*first.getHeight()));
		int f = first.getMonth();
		int s = f+(NUM_COLS*NUM_ROWS_ABOVE);
		if (f>s) {
			drawY+=childViews.getYearHeaderHeight();
		}
		
		
		if (childViews != null) childViews.draw(canvas,(int)drawY, NUM_COLS);
	}
	public AcalDateTime getClickedMonth(int x, int y) {
		return this.childViews.getClickedMonth(x,y,(int)this.y, NUM_ROWS_ABOVE, NUM_COLS);
	}

	private void populateMonths() {
		this.compHeight = this.getHeight()/NUM_ROWS_VISIBLE;
		this.compWidth = this.getWidth()/NUM_COLS;
		if (this.compHeight == 0 || this.compWidth == 0) return;
		this.lastWidth = getWidth(); this.lastheight = getHeight();
		imageGenerator = new MonthImageGenerator(compWidth,compHeight,this.getWidth(),this.context);
		childViews = new YearViewLinkedList();
		int year = startYear;
		int month = startMonth;
		childViews.createInitialRow(month,year,NUM_COLS, this.context, 0, this.imageGenerator, compWidth);
		for (int i = 1; i<NUM_ROWS_TOTAL; i++) {
			childViews.addRowToEnd(NUM_COLS, this.context, 0, this.imageGenerator, compWidth);
		}
		this.compHeight = childViews.getFirst().getMonthImage().getHeight();
	}
	
	public void moveY(float amnt) {
		if ( childViews == null ) return;  // Can sometimes happen if called during initialisation
		this.y-=amnt;
		if (y< 0-compWidth) {
			//recalculate start month
			startMonth+=NUM_COLS;
			while (startMonth > AcalDateTime.DECEMBER) {
				startMonth-=12; startYear++;
			}
			//remove 2 at front, add 2 to bottom
			y += childViews.removeFirstRow(NUM_COLS);
			childViews.addRowToEnd(NUM_COLS, this.context, 0, imageGenerator, compWidth);
		} else if (y>compWidth) {
			//recalculate start month
			startMonth-=NUM_COLS;
			if (startMonth<AcalDateTime.JANUARY) {startMonth+=12; startYear--;}
			//remove 2 from end, add 2 to start
			childViews.removeLastRow(NUM_COLS);
			y-=childViews.insertNewRow(NUM_COLS, this.context, 0, imageGenerator, compWidth);
		}
	}
	
	public AcalDateTime getDisplayedDate() {
		return childViews.getDisplayedDate(NUM_ROWS_ABOVE,NUM_COLS);
	}

}
