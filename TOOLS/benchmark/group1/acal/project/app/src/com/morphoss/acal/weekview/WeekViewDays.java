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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.dataservice.Collection;

public class WeekViewDays extends ImageView implements OnTouchListener {

	public static final String TAG = "aCal - WeekViewDays";

	private WeekViewActivity context; 
	private WeekViewCache dataCache = null;
	private WVCacheObject[][] headerTimeTable;


	//Drawing vars - easier to set these as class fields than to send them as parmaeters
	//All these vars are used in drawing and are recaculated each time draw() is called and co-ordinates have changed

	// These need only be calculated once 
	private int viewWidth;			//The current screen viewWidth in pixels
	private int TpX;				//The current Screen height in pixels
	private int HSPP;				//Horizontal Seconds per Pixel
	private int HNS;				//the number of visible horizontal seconds
	private int HIH;				//The height of a Horizontal event

	// These ones do change as things move around
	private int PxD;				//The current height of days section
	private int PxH;				//The current height of the Header section
	private int topSec;				//The first second of the day that is visible in main view
	private long HST;				//The UTC epoch time of the first visible horizontal second
	private long HET;				//The UTC epoch time of the last visible horizontal second
	private int HDepth;				//The number of horizontal rows
	

	private boolean dimensionCalculated = false;	//Set to True once screen dimensions are calculated.

	private class Rectangle {
		int x1, y1, x2, y2;
		WVCacheObject event;
		Rectangle(int x1, int y1, int x2, int y2, WVCacheObject event ) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.event = event;
		}
	}
	
	//The current set of events that are visible on the screen
	private List<Rectangle> eventsDisplayed;

	private Paint workPaint;

	/** Default Constructor */
	public WeekViewDays(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		construct(context);
	}

	/** Default Constructor */
	public WeekViewDays(Context context, AttributeSet attrs) {
		super(context,attrs);
		construct(context);
	}

	/** Default Constructor */
	public WeekViewDays(Context context) {
		super(context);
		construct(context);
	}

	/**
	 * Called by all constructors to ensure class is set up correctly.
	 * @param context
	 */
	private void construct(Context context) {
		if (this.isInEditMode()) {
			return;
		}
		if (!(context instanceof WeekViewActivity))
			throw new IllegalStateException("Week View Started with invalid context.");

		this.context = (WeekViewActivity) context;
	}

	/**
	 * The Current height of the header.
	 * @return
	 */
	public int getHeaderHeight() {
		return this.PxH; 
	}

	/**
	 * Return the nearest valid 'y' to the one given, ensuring we don't scroll above / below the day.
	 *  
	 * @param y A position we are attempting to scroll to
	 * @return The position closest to that which is reasonable.
	 */
	int checkScrollY(int y) {
		if ( y < -PxH ) return -PxH;
		int max = ((AcalDateTime.SECONDS_IN_DAY/WeekViewActivity.SECONDS_PER_PIXEL) - TpX)+5;
		if ( y > max ) return max;
		return y;
	}

	public void dimensionsChanged() {
		if ( Constants.LOG_DEBUG )
			Log.d(TAG,"Dimensions may have changed, recalculating...");
		this.dimensionCalculated = false;
	}

	public boolean isDimensionsCaclulated() { return this.dimensionCalculated; }

	/**
	 * Called once during first onDraw to calculate dimensions. We cant do this in the constructor as we need the screen
	 * to be populated first and this happens after construction.
	 */
	private void calculateDimensions() {
	
		//Vars needed for drawing
		viewWidth = this.getWidth();
		TpX = this.getHeight();

		//Seconds per pixel min/max values (MAX full day visible MIN 1 pixel per minute)
		WeekViewActivity.SECONDS_PER_PIXEL = Math.max(WeekViewActivity.SECONDS_PER_PIXEL,60);
		WeekViewActivity.SECONDS_PER_PIXEL = Math.min(WeekViewActivity.SECONDS_PER_PIXEL,(86400/TpX)+1);

		//Horizontal SecsPerPix and Horizontal Number Visible Seconds and Horizontal Item Height
		HSPP = 86400/WeekViewActivity.DAY_WIDTH;
		HNS = viewWidth*HSPP;
		HIH = (int)WeekViewActivity.FULLDAY_ITEM_HEIGHT;

		workPaint = new Paint();
		workPaint.setStyle(Paint.Style.FILL);
		workPaint.setColor(context.getResources().getColor(R.color.WeekViewDayGridWorkTimeBG));

		this.dimensionCalculated = true;
		context.refresh();
	}
	
	/**
	 * This will calculate the header vars. While it requires construction of the header timetable, this
	 * timetable is cached and if header height is calculated its a certainty that the timetable will
	 * be used again shortly.
	 */
	private void calculateHeaderVars() {
		//1 Calculate per frame vars
		int scrollx = context.getScrollX();
		int scrolly = context.getScrollY();
		
		topSec = scrolly * WeekViewActivity.SECONDS_PER_PIXEL;
		HST = this.context.getCurrentDate().getEpoch() - (scrollx*HSPP);
		HET = HST+HNS;
		
		AcalDateTime startTime = new AcalDateTime().setEpoch(HST).applyLocalTimeZone();
		AcalDateTime endTime = startTime.clone().setEpoch(HET);
		AcalDateRange range = new AcalDateRange(startTime,endTime);
		
		//Get the current timetable
		try {
			open();
			WeekViewTimeTable timeTable = dataCache.getMultiDayTimeTable(range, HDepth);
			headerTimeTable = timeTable.getTimetable();
			HDepth = timeTable.HDepth;	//TODO yucky side affect stuff
			
			if (headerTimeTable.length <=0) {	
				this.PxH =0; 
				PxD = TpX;
				return; 
			}
			PxH = HDepth*HIH;
			
			//save affected vars
			PxD = TpX - PxH;
		}
		catch( Exception e ) {
			Log.e(TAG,Log.getStackTraceString(e));
		}
	}


	/****************************************************************
	 * 							DRAW METHODS						*
	 ****************************************************************/
	
	/**
	 * The primary draw method. Called whenever we need to redraw the view.
	 * 1) Check dimensions are calculated
	 * 2) Caclulate transient variables
	 * 3) Draw Background
	 * 4) Draw header 
	 * 5) Draw grid
	 * 6) Draw main body
	 * 7) Draw Border
	 * 8) Draw shading
	 * 
	 */
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		//First check that we can draw anything at all
		if (this.getWidth() == 0) return;
		if (this.getHeight() == 0) return;
		if (this.isInEditMode()) return;	//can't draw in edit mode
		
		//Calculate dimensions if we haven't already done so
		if (!this.dimensionCalculated) { this.calculateDimensions(); return; }
		
		//reset the displayed events list
		eventsDisplayed = new ArrayList<Rectangle>();
		
		//Create one paint that all methods can share
		Paint p = new Paint();
		
		//Calculate the required header variables drawHeader and drawBody are dependant on this calculation.
		calculateHeaderVars();

		//Warning! the order of calls here is important! Wrong order will produce a pretty mangled UI.
		drawBackground(canvas,p);
		drawHeader(canvas,p);
		drawGrid(canvas,p);
		drawEvents(canvas,p);
		drawBorder(canvas,p);
		drawShading(canvas,p);
	}
	
	/**
	 * The first of the draw methods
	 * Draw a bland background underneath everything else.
	 * @param canvas
	 */
	private void drawBackground(Canvas canvas, Paint p) {
		p.setStyle(Paint.Style.FILL);
		p.setColor(Color.parseColor("#AAf0f0f0"));
		canvas.drawRect(0, 0, viewWidth, TpX, p);
	}
	
	/**
	 * Next is the header - this method has side-affects that are important to other draw calls.
	 * @param canvas
	 * @param p
	 */
	private void drawHeader(Canvas canvas, Paint p) {

		int scrollx = context.getScrollY();

		//draw day boxes
		p.setStyle(Paint.Style.STROKE);
		p.setColor(context.getResources().getColor(R.color.WeekViewDayGridBorder));
		canvas.drawRect(0, 0, viewWidth, PxH, p);
		for (int curx =0-(WeekViewActivity.DAY_WIDTH-scrollx); curx<=viewWidth; curx+= WeekViewActivity.DAY_WIDTH) {
			canvas.drawRect(curx, 0, curx+WeekViewActivity.DAY_WIDTH,PxH,p);
		}

		for (int i = 0; i<headerTimeTable.length;i++)  {
			boolean hasEvent = false;
			for(int j=0;j < headerTimeTable[i].length && headerTimeTable[i][j] != null;j++) {
				WVCacheObject event = headerTimeTable[i][j];
				drawHorizontal(event, canvas,i);
				hasEvent=true;
			}
			if (!hasEvent) break;
		}
	}
	
	/**
	 * Draw the time/date grid to go behind the events.
	 * @param canvas
	 * @param p
	 */
	public void drawGrid(Canvas canvas, Paint p) {
		int scrollx = context.getScrollX();
		p.setStyle(Paint.Style.FILL);
		int dayX = (int)(0-WeekViewActivity.DAY_WIDTH+scrollx);
		AcalDateTime currentDay = context.getCurrentDate().clone();
		currentDay.addDays(-1);

		//get the grid for each day
		Bitmap dayGrid = context.getImageCache().getDayBox(TpX+(3600/WeekViewActivity.SECONDS_PER_PIXEL));
		int y = PxH;
		int offset = PxH + (((topSec+3600)%3600)/WeekViewActivity.SECONDS_PER_PIXEL);

		// The location of the work part of the day
		int workTop = (context.WORK_START_SECONDS - topSec) / WeekViewActivity.SECONDS_PER_PIXEL;
		int workBot = (context.WORK_FINISH_SECONDS- topSec) / WeekViewActivity.SECONDS_PER_PIXEL;
		if ( workTop < PxH ) workTop = PxH;

		try {
			dayGrid = Bitmap.createBitmap(dayGrid, 0, offset, dayGrid.getWidth(), PxD);
		}
		catch( Exception e ) {
			Log.e(TAG,"Failed to createBitmap for dayGrid: offset="+offset+", width="+dayGrid.getWidth()+", PxH="+PxH+", PxD="+PxD);
			return;
		}
		Rect src = new Rect(0,offset,dayGrid.getWidth(),offset+PxD);
		while ( dayX <= viewWidth) {
			if (!(currentDay.getWeekDay() == AcalDateTime.SATURDAY || currentDay.getWeekDay() == AcalDateTime.SUNDAY)) {
				//add a yellow background around to the work hours
				canvas.drawRect(dayX, workTop, dayX+WeekViewActivity.DAY_WIDTH, workBot, workPaint);
			}

			Rect dst = new Rect(dayX,y,dayX+dayGrid.getWidth(),y+PxD);
			canvas.drawBitmap(dayGrid, src, dst, p);
			dayX += WeekViewActivity.DAY_WIDTH;
			currentDay.addDays(1);
		}
	}
	
	/**
	 * The most significant of the draw methods. Optimising the method will inevitably give good performance gains.
	 * @param canvas
	 * @param p
	 */
	private void drawEvents(Canvas canvas, Paint p) {
		//get the first fully visible day
		AcalDateTime currentDay = this.context.getCurrentDate().clone();
		
		//The x-axis offset of the first day
		int dayX = (int)(0-WeekViewActivity.DAY_WIDTH+context.getScrollX());
		
		//Its nicer to store this value in a var with a short name
		int dayWidth = WeekViewActivity.DAY_WIDTH;

		//Subtract a day as we may need to partially draw the day before the first fully visible day
		currentDay.addDays(-1);
		
		//draw events
		while (dayX<= viewWidth) {		//while x position is left of the right side of the screen

			if ( Constants.LOG_DEBUG && Constants.debugWeekView )
				Log.d(TAG,"Starting new day "+AcalDateTime.fmtDayMonthYear(currentDay)+
						" epoch="+currentDay.getEpoch()+" dayX="+dayX);

			//Get the timetable for the current day.
			open();
			WeekViewTimeTable timeTable = dataCache.getInDayTimeTable(currentDay);
			if (timeTable == null) {
				currentDay.addDays(1);
				dayX+=dayWidth;
				continue;
			}
			WVCacheObject[][] timeTableData = timeTable.getTimetable(); 

			//draw visible events
			if ( timeTableData.length > 0) {
				p.reset();
				p.setStyle(Paint.Style.FILL);
				long thisDayEpoch = currentDay.getEpoch();

				//events can show up several times in the timetable, keep a record of what's already drawn
				Set<WVCacheObject> drawn = new HashSet<WVCacheObject>();

				//for each set of overlapping events
				for (int i = 0; i < timeTableData.length; i++)  {
					int curX = 0;
					//for each overlapping event in a set
					for(int j=0; j < timeTableData[i].length; j++) {
						if (timeTableData[i][j] != null) {
							if (drawn.contains(timeTableData[i][j])) {
								//if we have already drawn this event, we need to increment x by its width
								curX+=timeTableData[i][j].getLastWidth();
								continue;
							}
							drawn.add(timeTableData[i][j]);

							//calculate depth - i.e how many events this event overlaps.
							int depth  = 0;
							for ( int k = j;
								k<=timeTable.lastMaxX && (timeTableData[i][k] == null || timeTableData[i][k] == timeTableData[i][j]);
								k++)
								depth++;
							//Calculate the width we should draw by dividing the available space by the number of events.
							float singleWidth = (dayWidth/(timeTable.lastMaxX+1)) * depth;	
							WVCacheObject event = timeTableData[i][j];
							//draw the event
							drawVertical(event, canvas, (int)dayX+curX, (int)singleWidth, thisDayEpoch);
							//save its width for future reference
							event.setLastWidth((int)singleWidth);
							//increment x pointer
							curX+=singleWidth;
						}
					} //end this event in set  
				} //end this set of overlapping events 
			} //end this day 
			currentDay.addDays(1);
			dayX+=dayWidth;
		} //end visible days
	}
	
	/**
	 * Draw the border arround the main grid
	 * @param canvas
	 * @param p
	 */
	private void drawBorder(Canvas canvas, Paint p) {
		//border
		p.reset();
		p.setStyle(Paint.Style.STROKE);
		p.setColor(0xff333333);
		canvas.drawRect(0, 0, viewWidth, PxH, p);
	}
	
	/**
	 * Draw the shading between the header and the main grid
	 * @param canvas
	 * @param p
	 */
	private void drawShading(Canvas canvas, Paint p) {
		//draw shading effect below header
		if (PxH != 0 ) {
			int hhh = 1200/WeekViewActivity.SECONDS_PER_PIXEL;

			int base = 0x444444;
			int current = 0xc0;
			int decrement = current/hhh;
			hhh += PxH;

			int color = (current << 24)+base; 
			p.setColor(color);
			canvas.drawLine(0, PxH, viewWidth, PxH, p);

			for (int i=PxH; i<hhh; i++) {
				current-=decrement;
				color = (current << 24)+base; 
				p.setColor(color);
				canvas.drawLine(0, i, viewWidth, i, p);
			}
		}
	}

	/**
	 * This ed the primary draw methods. The next 2 methods are responsible for the actual drawing of events
	 */


	/**
	 * Draw an event in the main part of the day.
	 *  
	 * @param event to be drawn
	 * @param canvas to draw on
	 * @param x position from left, in pixels
	 * @param width of the event to draw, in pixels
	 * @param dayStart in seconds from epoch
	 */
	public void drawVertical(WVCacheObject event, Canvas canvas, int x,  int width, long dayStart) {

		long topStart = dayStart + topSec;
		if ( Constants.LOG_VERBOSE && Constants.debugWeekView ) {
			//TODO fix time text
			Log.v(TAG,"Drawing event "+/**event.getTimeText(context, dayStart, dayStart, true)+*/
					": '"+event.getSummary()+"' at "+x+" for "+width+" ~ "+
					event.getStart()+","+event.getEnd()+" ~ "+dayStart+", topSec: "+topSec);
			Log.v(TAG,"Top="+(event.getStart() - topStart)+
					", Bottom="+(event.getEnd() - topStart) );
		}

		int maxWidth = width;
		if ( x < 0 ) {
			width = width + x;
			x = 0;
		}

		if ( width < 1f ) {
			if ( Constants.LOG_VERBOSE && Constants.debugWeekView ) Log.v(TAG,"Event is width "+ width);
			return;
		}

		int bottom = (int) ((event.getEnd()/1000 - topStart)/WeekViewActivity.SECONDS_PER_PIXEL);
		if ( bottom < PxH )  {  // Event is off top
			if ( Constants.LOG_VERBOSE && Constants.debugWeekView ) Log.v(TAG,"Event is off top by "+ bottom + " vs. " + PxH);
			return;
		}

		int top = (int) ((event.getStart()/1000 - topStart)/WeekViewActivity.SECONDS_PER_PIXEL);
		if ( top > TpX ) { // Event is off bottom
			if ( Constants.LOG_VERBOSE && Constants.debugWeekView ) Log.v(TAG,"Event is off bottom by "+ top + " vs. " + TpX);
			return;
		}

		int maxHeight = Math.max((bottom - top), WeekViewActivity.MINIMUM_DAY_EVENT_HEIGHT );
		int height = maxHeight;

		if ( top < PxH ) {
			height -= (PxH-top);
			top = PxH;
		}
		if ( bottom > TpX ) bottom = TpX;
		if ( height < 1 ) return;

		// Paint a gray border
		Paint p = new Paint();
		p.setStyle(Paint.Style.FILL);
		p.setColor(0xff555555);

		if ( Constants.LOG_VERBOSE && Constants.debugWeekView )
			//TODO fix time text
			Log.v(TAG,"Drawing event "+/**event.getTimeText(context, dayStart, dayStart, true)+*/
					": '"+event.getSummary()+"' at "+x+","+top+" for "+width+","+height+
					" ("+maxWidth+","+maxHeight+")"+
					" - "+event.getStart()+","+event.getEnd());

		Collection collection = Collection.getInstance(event.getCollectionId(),this.context);
		canvas.drawBitmap(context.getImageCache().getEventBitmap(event.getResourceId(),event.getSummary(),collection.getColour(),
				width, height, maxWidth, maxHeight), x, top, new Paint());

		eventsDisplayed.add( new Rectangle( x, top, x+width, top+height, event) );
	}


	/**
	 * Draw an (all day/multi day) event in the header
	 * @param event to be drawn.
	 * @param c The canvas
	 * @param depth layer for the event
	 */
	public void drawHorizontal(WVCacheObject event, Canvas c, int depth) {
		int maxWidth = event.calulateMaxWidth(viewWidth, HSPP);
		if ( maxWidth < 0 ) return;
		int x = (int)(event.getStart()-HST)/HSPP;
		if ( x < 0 ) x = 0;
		int y = HIH*depth;
		int actualWidth = (int)Math.min(Math.min(event.getActualWidth(), viewWidth-x),(event.getEnd()-HST)/HSPP); 
		if ( actualWidth<=0 ) return;
		Collection collection = Collection.getInstance(event.getCollectionId(),this.context);
		c.drawBitmap(context.getImageCache().getEventBitmap(event.getResourceId(),event.getSummary(),collection.getColour(),
				actualWidth, HIH, maxWidth, HIH), x,y, new Paint());
		eventsDisplayed.add( new Rectangle( x, y, x+actualWidth, y+HIH, event) );
	}

	
	/************************************************
	 * 				End Draw Methods				*
	 ************************************************/
	

	/**
	 * Finds the event that the click was on, or the day+time that it was on (or maybe
	 * just the day.
	 */
	public ArrayList<Object> whatWasUnderneath(float x, float y) {
		int scrolly = context.getScrollY();
		int scrollx = context.getScrollX();
		ArrayList<Object> result = new ArrayList<Object>();
		result.add(AcalDateTime.addDays(context.getCurrentDate(),(int) ((x - scrollx) / WeekViewActivity.DAY_WIDTH)));
		if ( y < PxH ) result.add(-1);
		else {
			result.add((Integer) (int) ((y+scrolly) * WeekViewActivity.SECONDS_PER_PIXEL));
		}
		int fuzzX = 45;
		int fuzzY = 45;
		for( Rectangle r : eventsDisplayed ) {
			if ( r.x1 <= (x + fuzzX) && r.x2 >= (x - fuzzX)
					&& r.y1 <= (y + fuzzY) && r.y2 >= (y - fuzzY) ) {
				result.add(r.event); 
			}
		}

		if ( Constants.LOG_DEBUG ) {
			Log.d(TAG,"Background scrollX="+scrollx+" scrollY="+scrolly+", topSec="+topSec );
			Log.d(TAG,"Underneath "+(int)x+" is day: "+((AcalDateTime) result.get(0)).toString() );
			Log.d(TAG,"Underneath "+(int)y+" is sec: "+((Integer) result.get(1)).toString() );

			for(int i=2; i<result.size(); i++ ) {
				//TODO fix log message text.
				WVCacheObject e = (WVCacheObject) result.get(i); 
				Log.d(TAG,"Underneath event: " /**+.getTimeText(context, currentEpoch, currentEpoch, true) */ +
						", Summary: " + e.getSummary() );
			}
		}

		return result;
	}
	
	/**
	 * Must be called when activity quits or we will never be GC'd
	 */
	public void close() {
		if (dataCache != null) {
			dataCache.close();
			dataCache = null;
		}
	}
	
	/**
	 * Should be called if we wish to reuse this class after a close
	 */
	public synchronized void open() {
		if (dataCache == null)
			dataCache = new WeekViewCache(context,this);
	}

	/**
	 * Interaction overrides
	 */
	
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return context.onTouch(v,event);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		context.cancelLongPress();
	}

	public void requestRedraw() {
		this.invalidate();
	}
}
