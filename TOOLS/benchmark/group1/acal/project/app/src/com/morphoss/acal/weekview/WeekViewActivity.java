/*
 * Copyright (C) 2012 Morphoss Ltd
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

import java.util.List;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.activity.AcalActivity;
import com.morphoss.acal.activity.EventEdit;
import com.morphoss.acal.activity.EventView;
import com.morphoss.acal.activity.MonthView;
import com.morphoss.acal.activity.YearView;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.widget.NumberPickerDialog;
import com.morphoss.acal.widget.NumberSelectedListener;

/**
 * This is the activity behind WeekView. It catches all UI Events and user interaction.
 *
 * Valid user input is passed on to the WeekViewLayout, responsible for drawing all the components in this activity.
 *  
 * 
 * @author Morphoss Ltd
 * @license GPL v3 or later
 */
public class WeekViewActivity extends AcalActivity implements OnGestureListener, OnTouchListener, OnClickListener, NumberSelectedListener {
	/* Fields relating to buttons */
	public static final int TODAY = 0;
	public static final int YEAR = 1;
	public static final int MONTH = 2;
	public static final int ADD = 3;

	public static final String TAG = "aCal WeekViewActivity";
	
	private WeekViewHeader 	header;
	private WeekViewSideBar sidebar;
	private WeekViewDays	days;

	private SharedPreferences prefs = null; 
	private boolean invokedFromView = false;

	//Text Size - some sizes differ, but are relative to this
	public static final float TEXT_SIZE = 11f;	//SP
	
	//Magic Numbers / Configurable values 
	public static int MINIMUM_DAY_EVENT_HEIGHT;
	public static final float[] DASHED_LINE_PARAMS = new float[] {5,5};
	
	public static final int EVENT_BORDER = 2;		//hard coded
	
	//Preference controlled values
	public static int DAY_WIDTH = 100;
	public static int SECONDS_PER_PIXEL = 1;
	public static int FIRST_DAY_OF_WEEK = 1;
	public static boolean TIME_24_HOUR = false;
	public static int FULLDAY_ITEM_HEIGHT = 20;  //1 row
	public static int START_HOUR = 9;
	public static int START_MINUTE = 0;
	public static int END_HOUR = 17;
	public static int END_MINUTE = 0;
	public static float PIXELS_PER_TEXT_ROW = 0f;

	/* Text sizes */
	public static final float TEXT_SIZE_SIDE = TEXT_SIZE * 0.9f;
	public static final float TEXT_SIZE_EVENT = TEXT_SIZE;
	
	/* Fields relating to Intent Results */
	public static final int PICK_MONTH_FROM_YEAR_VIEW = 0;
	public static final int PICK_TODAY_FROM_EVENT_VIEW = 1;
	public static final int PICK_DAY_FROM_MONTH_VIEW = 5;
	
	//Image/data caches
	private WeekViewImageCache imageCache;
	
	//Dialogs
	private static final int DATE_PICKER = 0;
	
	/* Fields Relating to Gesture Detection */
	private GestureDetector gestureDetector;
	private AcalDateTime selectedDate = new AcalDateTime();
	
	
	//Fields relating to scrolling
	private int scrollx = 0;
	private int scrolly = 0;
	int	WORK_START_SECONDS;
	int	WORK_FINISH_SECONDS;

	private int	StatusBarHeight;
	private float	SPscaler;
	
	
	/**
	 * Set up buttons, UI listeners and views.
	 * @param savedInstanceState Contains the day of the week that we start with
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle b = this.getIntent().getExtras();
		if ( b != null && b.containsKey("InvokedFromView") )
			invokedFromView = true;
		
		this.setContentView(R.layout.week_view);
		header 	= (WeekViewHeader) 	this.findViewById(R.id.week_view_header);
		sidebar = (WeekViewSideBar) this.findViewById(R.id.week_view_sidebar);
		days 	= (WeekViewDays) 	this.findViewById(R.id.week_view_days);
		
		gestureDetector = new GestureDetector(this);
		selectedDate = this.getIntent().getExtras().getParcelable("StartDay");
		
		if ( selectedDate == null ) {
			selectedDate = new AcalDateTime();
		}
		selectedDate.applyLocalTimeZone().setDaySecond(0);

		// Hack to calculate the status bar height 
		Rect rectgle= new Rect();
		Window window= getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		StatusBarHeight= rectgle.top;


		// Set up buttons
		this.setupButton(R.id.week_today_button, TODAY);
		this.setupButton(R.id.week_month_button, MONTH);
		this.setupButton(R.id.week_add_button, ADD);
		
		try {
			//TODO - vertical layout does not have a year view button, so we'll just ignore the exception.
			this.setupButton(R.id.week_year_button, YEAR);
		}
		catch( Exception e ) {}
	
		loadPrefs();
		days.setOnTouchListener(days);

		this.registerForContextMenu(days);
	}
	
	private void loadPrefs() {
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		selectedDate = new AcalDateTime().applyLocalTimeZone();
		if ( prefs.getLong(getString(R.string.prefSavedSelectedDate), 0) > (System.currentTimeMillis() - (3600000L * 6)))
			selectedDate.setMillis(prefs.getLong(getString(R.string.prefSelectedDate), System.currentTimeMillis()));
		selectedDate.setDaySecond(0);

		TIME_24_HOUR = prefs.getBoolean(this.getString(R.string.prefTwelveTwentyfour), false);
		try {
			FIRST_DAY_OF_WEEK = Integer.parseInt(prefs.getString(getString(R.string.firstDayOfWeek), "0"));
			if ( FIRST_DAY_OF_WEEK < AcalDateTime.MONDAY || FIRST_DAY_OF_WEEK > AcalDateTime.SUNDAY ) throw new Exception();
		}
		catch( Exception e ) {
			FIRST_DAY_OF_WEEK = AcalDateTime.MONDAY; 
		}
		
		SPscaler = this.getResources().getDisplayMetrics().scaledDensity;	//used for scaling our values to SP
		float DPscaler = this.getResources().getDisplayMetrics().density;		//used for scaling our values to SP

		PIXELS_PER_TEXT_ROW = TEXT_SIZE*SPscaler;
		float lph = 2f;
		try {
			lph = Float.parseFloat(prefs.getString(getString(R.string.prefWeekViewLinesPerHour), "2"));
		}
		catch ( NumberFormatException e ) { }
		if (lph <= 0) lph = 1;
		if (lph >= 10) lph = 10;
		SECONDS_PER_PIXEL = (int)(((float) AcalDateTime.SECONDS_IN_HOUR)/(lph*PIXELS_PER_TEXT_ROW));
		MINIMUM_DAY_EVENT_HEIGHT = (int) ((TEXT_SIZE_EVENT*SPscaler)*1.1f);  
		FULLDAY_ITEM_HEIGHT = (int)((float)(MINIMUM_DAY_EVENT_HEIGHT + 3) * 1.2f);
		
		int cpw = 70;
		try {
			cpw = (int) Float.parseFloat(prefs.getString(getString(R.string.prefWeekViewDayWidth), "70"));
		}
		catch ( NumberFormatException e ) { }
		if (cpw <= 0) lph = 10;
		if (cpw >= 1000) lph = 1000;
		
		DAY_WIDTH = (int)(cpw*DPscaler);

		WORK_START_SECONDS  = getTimePref(R.string.prefWorkdayStart, 9*3600);
		WORK_FINISH_SECONDS = getTimePref(R.string.prefWorkdayFinish, 17*3600);
		
		scrolly = days.checkScrollY( WORK_START_SECONDS / SECONDS_PER_PIXEL + days.getHeaderHeight() );
		
		//image cache may now be invalid
		imageCache = new WeekViewImageCache(this);
		
	}
	
	//force all displays to update
	public void refresh() {
		header.invalidate();
		days.invalidate();	
		sidebar.invalidate();
	}
	
	public int getScrollY() {
		return days.checkScrollY(this.scrolly);
	}
	public int getScrollX() {
		return this.scrollx;
	}
	
	public float getSideVerticalOffset() {
		return days.getHeaderHeight();
	}
	
	public void move(float dx, float dy) {		
		this.scrolly = days.checkScrollY(this.scrolly + (int) dy);
		this.scrollx-=dx;
		while (this.scrollx >= DAY_WIDTH) {
			decrementCurrentDate();
			this.scrollx-=DAY_WIDTH;
		} 
		while (this.scrollx <= 0-DAY_WIDTH) {
			incrementCurrentDate();
			this.scrollx+=DAY_WIDTH;
		}
		
		refresh();
	}
	
	
	public WeekViewImageCache getImageCache() {
		return this.imageCache;
	}
	
	public AcalDateTime getCurrentDate() {
		return selectedDate;
	}
	
	public void incrementCurrentDate() {
		selectedDate.addDays(1);
	}
	
	public void decrementCurrentDate() {
		selectedDate.addDays(-1);
	}
	
	
	@Override 
	public void onPause() {
		super.onPause();
		days.close();		//important - otherwise days will never be dereferenced causing memory hole.
		prefs.edit().putLong(getString(R.string.prefSelectedDate), selectedDate.getMillis()).commit();
		prefs.edit().putLong(getString(R.string.prefSavedSelectedDate), System.currentTimeMillis()).commit();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		days.open();		//important - days must be open on display or system will crash
		imageCache = new WeekViewImageCache(this);
		loadPrefs();
		days.dimensionsChanged();  // User may have been in the preferences screen, maybe indirectly.
	}

	
	public boolean daysInitialized(){ return days.isDimensionsCaclulated(); }
	
	/**
	 * <p>
	 * Called when user has selected 'Settings' from menu. Starts Settings
	 * Activity.
	 * </p>
	 */
	private void startSettings() {
		Intent settingsIntent = new Intent();
		settingsIntent.setClassName("com.morphoss.acal",
				"com.morphoss.acal.activity.Settings");
		this.startActivity(settingsIntent);
	}


	/**
	 * <p>
	 * Called when user has selected 'Tasks' from menu. Starts TodoListView
	 * Activity.
	 * </p>
	 */
	private void startTodoList() {
		if ( invokedFromView )
			this.finish();
		else {
			Intent todoListIntent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putInt("InvokedFromView",1);
			todoListIntent.putExtras(bundle);
			todoListIntent.setClassName("com.morphoss.acal",
					"com.morphoss.acal.activity.TodoListView");
			this.startActivity(todoListIntent);
		}
	}

	

	/**
	 * <p>
	 * Responsible for handling the menu button push.
	 * </p>
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.events_options_menu, menu);
		return true;
	}
	/**
	 * <p>
	 * Called when the user selects an option from the options menu. Determines
	 * what (if any) Activity should start.
	 * </p>
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.settingsMenuItem:
			startSettings();
			return true;
		case R.id.tasksMenuItem:
			startTodoList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	public static final int	INCLUDE_IN_DAY_EVENTS	= 0x02;
	public static final int	INCLUDE_ALL_DAY_EVENTS	= 0x04;


	/**
	 * <p>
	 * Helper method for setting up buttons
	 * </p>
	 */
	private void setupButton(int id, int val) {
		Button myButton = (Button) this.findViewById(id);
		if ( myButton == null ) {
			//Log.e(TAG, "Cannot find button '" + id + "' by ID, to set value '" + val + "'", new Exception());
		}
		else {
			myButton.setOnClickListener(this);
			myButton.setTag(val);
			AcalTheme.setContainerFromTheme(myButton, AcalTheme.BUTTON);
		}
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent touch) {
		return this.gestureDetector.onTouchEvent(touch);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent motion) {
		switch (motion.getAction()) {
			case (MotionEvent.ACTION_MOVE): {
	            int dx = (int) (motion.getX() * motion.getXPrecision() * DAY_WIDTH);
				int dy = (int) (motion.getY() * motion.getYPrecision() * FULLDAY_ITEM_HEIGHT );
				if ( Constants.LOG_VERBOSE && Constants.debugWeekView )
					Log.v(TAG,"Trackball event of size "+motion.getHistorySize()+" x/y"+motion.getX()+"/"+motion.getY()
								+ " - precision: " + motion.getXPrecision() +"/" + motion.getYPrecision());
				if (Math.abs(dx)>Math.abs(dy)) move(dx,0);
				else move(0,dy);
				break;
			}
			case (MotionEvent.ACTION_DOWN): {
				// logic for ACTION_DOWN motion event here
				break;
			}
			case (MotionEvent.ACTION_UP): {
				// logic for ACTION_UP motion event here
				break;
			}
		}
		return true;
	}

	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
		days.cancelLongPress();
		return false;
	}

	MotionEvent lastDown = null;
	@Override
	public boolean onDown(MotionEvent me) {
		lastDown = me;
		return false;
	}
	
	/**
	 * TODO - context menu items need to be brought in line to work with refactored activities.
	 */
	private static final int CONTEXT_CHOICE_NEW_ALLDAY = -1;
	private static final int CONTEXT_CHOICE_NEW_WITHIN_DAY = -2;
	private List<Object>	underList;
	private static final int	CONTEXT_ACTION_VIEW		= 0x100;
	private static final int	CONTEXT_ACTION_EDIT		= 0x200;
	private static final int	CONTEXT_ACTION_COPY		= 0x300;
	private static final int	CONTEXT_ACTION_DELETE	= 0x400;
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
		days.cancelLongPress();
		menu.setHeaderTitle("Week View");

		if ( Constants.LOG_VERBOSE && Constants.debugWeekView ) Log.v(TAG,"OnCreateContextMenu!");
		underList = days.whatWasUnderneath(lastDown.getRawX() - sidebar.getWidth(),
					lastDown.getRawY() - (header.getHeight() +StatusBarHeight ) );
		if ( underList == null ) return;
		AcalDateTime dayPressed = (AcalDateTime) underList.get(0);
        menu.add(Menu.NONE, CONTEXT_CHOICE_NEW_ALLDAY, Menu.NONE, getString(R.string.newAllDayEventOn, AcalDateTime.fmtDayMonthYear(dayPressed)));

        int secPressed = (Integer) underList.get(1);
        if ( secPressed >= 0 ) {
        	if ( secPressed > 86400 ) secPressed = 86400;
        	menu.add(Menu.NONE, CONTEXT_CHOICE_NEW_WITHIN_DAY, Menu.NONE, getString(R.string.newHourEventAt,
        				String.format("%02d:%02d", secPressed/AcalDateTime.SECONDS_IN_HOUR,
        							((secPressed % AcalDateTime.SECONDS_IN_HOUR) / 1800) * 30 )
        					)
        			);
        }

        for( int i=2; i< underList.size(); i++) {
        	menu.add(Menu.NONE, i | CONTEXT_ACTION_VIEW, Menu.NONE, ((WVCacheObject) underList.get(i)).getSummary() );  
        	menu.add(Menu.NONE, i | CONTEXT_ACTION_EDIT, Menu.NONE,
        				getString(R.string.editSomeEvent, ((WVCacheObject) underList.get(i)).getSummary() ));  
        	menu.add(Menu.NONE, i | CONTEXT_ACTION_COPY, Menu.NONE,
        				getString(R.string.copySomeEvent, ((WVCacheObject) underList.get(i)).getSummary() ));  
        	menu.add(Menu.NONE, i | CONTEXT_ACTION_DELETE, Menu.NONE,
       				getString(R.string.deleteSomeEvent, ((WVCacheObject) underList.get(i)).getSummary() ));  
        }
		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if ( Constants.LOG_VERBOSE && Constants.debugWeekView ) Log.v(TAG,"OnContextItemSelected!");
        switch( item.getItemId() ) {
        	case CONTEXT_CHOICE_NEW_ALLDAY:
        	case CONTEXT_CHOICE_NEW_WITHIN_DAY: {
        		//Edit Instance Selected
        		Bundle bundle = new Bundle();
        		AcalDateTime eventDate = (AcalDateTime) underList.get(0);
                int eventSecs = ((Integer) underList.get(1)) / 1800;
                if ( item.getItemId() == CONTEXT_CHOICE_NEW_WITHIN_DAY )
                	eventDate = eventDate.setDaySecond(eventSecs*1800);
				bundle.putParcelable(EventEdit.NEW_EVENT_DATE_TIME_KEY, eventDate);
				bundle.putInt(EventEdit.ACTION_KEY, EventEdit.ACTION_CREATE);
				Intent eventEditIntent = new Intent(this, EventEdit.class);
				eventEditIntent.putExtras(bundle);
				this.startActivity(eventEditIntent);
        		break;
        	}
        	default: {
        		//View Instance
        		CacheObject co = (CacheObject) underList.get(item.getItemId() & 0xFF);
        		int action = (item.getItemId() & 0xFF00);
        		if ( action == CONTEXT_ACTION_VIEW ) {
	        		Bundle bundle = new Bundle();
	    			bundle.putParcelable(EventView.CACHE_INSTANCE_KEY, co);
	    			Intent eventViewIntent = new Intent(this, EventView.class);
	    			eventViewIntent.putExtras(bundle);
	    			this.startActivity(eventViewIntent);
        		}
        		else if ( action == CONTEXT_ACTION_DELETE ) {
        			/**
        			 * TODO
        			 */
        		}
        		else if ( action == CONTEXT_ACTION_COPY || action == CONTEXT_ACTION_EDIT ) {

        			int editAction = EventEdit.ACTION_EDIT;
	        		if ( action == CONTEXT_ACTION_COPY )
	        			editAction = EventEdit.ACTION_COPY;

					//start EventEdit activity
					Bundle bundle = new Bundle();
					bundle.putInt(EventEdit.ACTION_KEY,editAction);
					bundle.putLong(EventEdit.RESOURCE_ID_KEY, co.getResourceId());
					bundle.putString(EventEdit.RECCURENCE_ID_KEY, co.getRecurrenceId());
					Intent eventViewIntent = new Intent(this, EventEdit.class);
					eventViewIntent.putExtras(bundle);
					this.startActivity(eventViewIntent);
					return true;
        		}
        		break;
        	}
        }

        return true;
	}
	
	@Override
	public void onLongPress(MotionEvent me) {
	}

	/**
	 * Called from a view if it wants us to cancel any possibility of a long
	 * press happening until the next one starts.  This ensures that when a
	 * long press *does* happen, the underList will be set first.
	 */
	public void cancelLongPress() {
		underList = null;
	}

	
	
	@Override
	public boolean onScroll(MotionEvent start, MotionEvent current, float dx, float dy) {
		if (Math.abs(dx)>Math.abs(dy)) move(dx,0);
		else move(0,dy);
		days.cancelLongPress();
		return true;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
	}

	MotionEvent lastClickMe = null;
	@Override
	public boolean onSingleTapUp(MotionEvent me) {
		if ( lastClickMe == null ) {
			lastClickMe = me;
			return false;
		}
		if ( Math.abs(lastClickMe.getX() - me.getX()) < (10 * SPscaler) 
					&& Math.abs(lastClickMe.getX() - me.getX()) < (10 * SPscaler)
					&& (me.getEventTime() - lastClickMe.getEventTime()) < 1000
				) {
			days.cancelLongPress();
			List<Object> under = days.whatWasUnderneath(me.getRawX() - sidebar.getWidth(),
											me.getRawY() - (header.getHeight()+StatusBarHeight) );
			
			if ( under.size() > 2 ) {
				// There's at least one event under the double-click
				if ( under.size() == 3 ) {
					// There's only one event under the tap, so we'll view it directly
					CacheObject co = ((CacheObject) under.get(2));
					Bundle bundle = new Bundle();
	    			bundle.putParcelable(EventView.CACHE_INSTANCE_KEY, co);
	    			Intent eventViewIntent = new Intent(this, EventView.class);
	    			eventViewIntent.putExtras(bundle);
	    			this.startActivity(eventViewIntent);
				}
				else {
					// There's more than one, so we need to show a context menu
					this.openContextMenu(days);
				}
			}
			lastClickMe = null;
			return true;
		}
		lastClickMe = me;
		return false;
	}
	
	
	@Override 
	public void onNumberSelected(int number) {
		selectedDate = new AcalDateTime(number,1,1,0,0,0,null).applyLocalTimeZone().setDaySecond(0);
	}

	protected Dialog onCreateDialog(int id) {
		switch(id) {
			case DATE_PICKER: {
				NumberPickerDialog dialog = new NumberPickerDialog(this,this,selectedDate.getYear(),1582,3999);
				return dialog;
			}
		}
		return null;
		
	}
	/**
	 * <p>
	 * Handles button Clicks
	 * </p>
	 */
	@Override
	public void onClick(View clickedView) {
		int button = (int) ((Integer) clickedView.getTag());
		Bundle bundle = new Bundle();
		switch (button) {
		case TODAY:
			selectedDate.setEpoch(System.currentTimeMillis()/1000);
			this.scrollx=0;
			scrolly = days.checkScrollY((WeekViewActivity.START_HOUR*AcalDateTime.SECONDS_IN_HOUR)/SECONDS_PER_PIXEL);
			selectedDate.setDaySecond(0);
			this.refresh();
			break;
		case ADD:
			bundle.putParcelable("DATE", selectedDate);
			Intent eventEditIntent = new Intent(this, EventEdit.class);
			eventEditIntent.putExtras(bundle);
			this.startActivity(eventEditIntent);
			break;
		case YEAR:
			//TODO not implemented properly
			bundle = new Bundle();
			bundle.putInt("StartYear", selectedDate.getYear());
			Intent yearIntent = new Intent(this, YearView.class);
			yearIntent.putExtras(bundle);
			this.startActivityForResult(yearIntent, PICK_MONTH_FROM_YEAR_VIEW);
			break;
		case MONTH:
			if ( prefs.getBoolean(getString(R.string.prefDefaultView), false) ) {
				Intent startIntent = null;
				startIntent = new Intent(this, MonthView.class);
				startIntent.putExtras(bundle);
				this.startActivity(startIntent);
			}
			else {
				Intent res = new Intent();
				res.putExtra("selectedDate", (Parcelable) selectedDate);
				this.setResult(RESULT_OK, res);
				this.finish();
			}
			break;
		default:
			Log.w(TAG, "Unrecognised button was pushed in MonthView.");
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//TODO Should be using constants for parecelable keys. We need to streamline how acitivy reuslt work across the board.
		if ( resultCode == RESULT_OK ) {
			switch ( requestCode ) {
			case PICK_DAY_FROM_MONTH_VIEW:
				if (data.hasExtra("selectedDate")) {
					try {
						AcalDateTime day = (AcalDateTime) data.getParcelableExtra("selectedDate");
						selectedDate = day;

					} catch (Exception e) {
						Log.w(TAG, "Error getting month back from year view: "+e);
					}
				}
				break;
				case PICK_MONTH_FROM_YEAR_VIEW:
					if (data.hasExtra("selectedDate")) {
						try {
							AcalDateTime month = (AcalDateTime) data.getParcelableExtra("selectedDate");
							selectedDate = month;
						} catch (Exception e) {
							Log.w(TAG, "Error getting month back from year view: "+e);
						}
					}
					break;
				case PICK_TODAY_FROM_EVENT_VIEW:
					try {
						AcalDateTime chosenDate = (AcalDateTime) data.getParcelableExtra("selectedDate");
						selectedDate = chosenDate;
					} catch (Exception e) {
						Log.w(TAG, "Error getting month back from year view: "+e);
					}
			}
			selectedDate.applyLocalTimeZone().setDaySecond(0);
		}
	}

	public String getStringPref(int resId, String defaultValue) {
		return prefs.getString(getString(resId), defaultValue);
	}
	
	public int getIntegerPref(int resId, int defaultValue) {
		return Integer.parseInt(prefs.getString(getString(resId), Integer.toString(defaultValue)));
	}
	
	public int getTimePref(int resId, int defaultValue) {
		String time = getStringPref(resId,(defaultValue/AcalDateTime.SECONDS_IN_HOUR)+":"+(defaultValue%AcalDateTime.SECONDS_IN_HOUR)/60);
		String[] hm = time.split(":");
		if ( hm.length < 2 ) return defaultValue;
		return Integer.parseInt(hm[0])*3600 + Integer.parseInt(hm[1])*60 ;
	}
}
