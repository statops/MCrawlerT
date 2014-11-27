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

package com.morphoss.acal.activity;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.dataservice.CalendarInstance;
import com.morphoss.acal.dataservice.EventInstance;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.service.aCalService;
import com.morphoss.acal.weekview.WeekViewActivity;
import com.morphoss.acal.widget.AcalViewFlipper;

/**
 * <h1>Month View</h1>
 * 
 * <h3>This is the activity that is started when aCal is run and will likely be
 * the most used interface in the program.</h3>
 * 
 * <p>
 * This view is split into 3 sections:
 * </p>
 * <ul>
 * <li>Month View - A grid view controlled by a View Flipper displaying all the
 * days of a calendar month.</li>
 * <li>Event View - A grid view controlled by a View Flipper displaying all the
 * events for the current selected day</li>
 * <li>Buttons - A set of buttons at the bottom of the screen</li>
 * </ul>
 * <p>
 * As well as this, there is a menu accessible through the menu button.
 * </p>
 * 
 * <p>
 * Each of the view flippers listens to gestures, Side swipes on either will
 * result in the content of the flipper moving forward or back. Content for the
 * flippers is provided by Adapter classes that contain the data the view is
 * representing.
 * </p>
 * 
 * <p>
 * At any time there are 2 important pieces of information that make up this
 * views state: The currently selected day, which is highlighted when visible in
 * the month view and determines which events are visible in the event list. The
 * other is the currently displayed date, which determines which month we are
 * looking at in the month view. This state information is written to and read
 * from file when the view loses and gains focus.
 * </p>
 * 
 * 
 * @author Morphoss Ltd
 * @license GPL v3 or later
 * 
 */
public class MonthView extends AcalActivity implements OnGestureListener,
		OnTouchListener, OnClickListener, ResourceResponseListener<Long> {

	public static final String TAG = "aCal MonthView";

	/** The file that we save state information to */
	public static final String STATE_FILE = "/data/data/com.morphoss.acal/monthview.dat";

	private boolean invokedFromView = false;
	
	/* Fields relating to the Month View: */

	/** The flipper for the month view */
	private AcalViewFlipper monthGridFlipper;
	/** The root view containing the GridView Object for the Month View */
	private View monthGridRoot;
	/** The GridView object that displays the month */
	private GridView monthGrid = null;
	/** The GridView object that displays the month */
	private MonthAdapter monthAdapter= null;
	/** The TextView that displays which month we are looking at */
	private TextView monthTitle;

	/* Fields relating to the Event View */

	/** The flipper for the Event View */
	private AcalViewFlipper listViewFlipper;
	/** The root view containing the ListView Object for the Event View */
	private View eventListRoot;
	/** The ListView object that displays the Event View */
	private ListView eventList = null;
	/** The TextView that displays which day we are looking at */
	private TextView eventListTitle;
	/** The current event list adapter */
	private EventListAdapter eventListAdapter;

	/* Fields relating to state */

	/** The month that our Month View should display */
	private AcalDateTime displayedMonth;
	/** The day that our Event View should display */
	private AcalDateTime selectedDate;

	/* Fields relating to buttons */
	public static final int TODAY = 0;
	public static final int WEEK = 1;
	public static final int YEAR = 2;
	public static final int ADD = 3;

	/* Fields Relating to Gesture Detection */
	private GestureDetector gestureDetector;
	private long consumedTime;
	private static final int maxAngleDev = 30;
	private static final int minDistanceSquared = 3600;

	/* Fields relating to Intent Results */
	public static final int PICK_MONTH_FROM_YEAR_VIEW = 0;
	public static final int PICK_TODAY_FROM_EVENT_VIEW = 1;
	public static final int PICK_DAY_FROM_WEEK_VIEW = 2;

	protected static final int	SHOW_DELETING	= 0x100;
	protected static final int	DELETE_SUCCEEDED	= 0x101;
	protected static final int	DELETE_FAILED	= 0x102;

	protected static final int	DELETING_DIALOG	= 0x200;

	// Animations
	Animation leftIn = null;
	Animation leftOut = null;
	Animation rightIn = null;
	Animation rightOut = null;

	private int	eventListIndex = 0;
	private int	eventListTop = 0;

	/********************************************************
	 * Activity Overrides *
	 ********************************************************/

	/**
	 * <p>
	 * Called when Activity is first created. Initialises all appropriate fields
	 * and Constructs the Views for display.
	 * </p>
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.month_view);

		Bundle b = this.getIntent().getExtras();
		if ( b != null && b.containsKey("InvokedFromView") )
			invokedFromView = true;

		// make sure aCalService is running
		this.startService(new Intent(this, aCalService.class));

		gestureDetector = new GestureDetector(this);

		AcalDateTime currentDate = new AcalDateTime().applyLocalTimeZone();
		selectedDate = currentDate.clone();
		displayedMonth = currentDate;

		leftIn = AnimationUtils.loadAnimation(this, R.anim.push_left_in);
		leftOut = AnimationUtils.loadAnimation(this, R.anim.push_left_out);
		rightIn = AnimationUtils.loadAnimation(this, R.anim.push_right_in);
		rightOut = AnimationUtils.loadAnimation(this, R.anim.push_right_out);
	}

	/**
	 * <p>
	 * Called when Activity regains focus. Try's to load the saved State.
	 * </p>
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.v(TAG,TAG + " - onResume");

		// Set up buttons
		this.setupButton(R.id.month_today_button, TODAY, getString(R.string.Today));
		this.setupButton(R.id.month_week_button, WEEK, getString(R.string.Week));
		this.setupButton(R.id.month_year_button, YEAR, getString(R.string.Year));
		this.setupButton(R.id.month_add_button, ADD, "+");
		
		selectedDate = new AcalDateTime().applyLocalTimeZone();
		displayedMonth = new AcalDateTime().applyLocalTimeZone();
		if ( prefs.getLong(getString(R.string.prefSavedSelectedDate), 0) > (System.currentTimeMillis() - (3600000L * 6))) {
			selectedDate.setMillis(prefs.getLong(getString(R.string.prefSelectedDate), System.currentTimeMillis()));
			displayedMonth.setMillis(prefs.getLong(getString(R.string.prefDisplayedMonth), System.currentTimeMillis()));
		}
		selectedDate.setDaySecond(0);
		displayedMonth.setDaySecond(0).setMonthDay(1);
		if ( this.monthGrid == null ) createGridView(true);
		changeDisplayedMonth(displayedMonth);
		changeSelectedDate(selectedDate);
		
	}

	/**
	 * <p>
	 * Called when activity loses focus or is closed. Try's to save the current
	 * State
	 * </p>
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();

		rememberCurrentPosition();
		eventList = null;		

		// Save state
		prefs.edit().putLong(getString(R.string.prefSavedSelectedDate), System.currentTimeMillis()).commit();
		prefs.edit().putLong(getString(R.string.prefSelectedDate), selectedDate.getMillis()).commit();
		prefs.edit().putLong(getString(R.string.prefDisplayedMonth), displayedMonth.getMillis()).commit();

	}

	private void rememberCurrentPosition() {
    	// save index and top position
    	if ( eventList != null ) {
        	eventListIndex = eventList.getFirstVisiblePosition();
        	View v = eventList.getChildAt(0);
        	eventListTop = (v == null) ? 0 : v.getTop();
        	if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
    				"Saved list view position of "+eventListIndex+", "+eventListTop);
    	}
	}

    
    private void restoreCurrentPosition() {
    	if ( eventList != null ) {
        	eventList.setSelectionFromTop(eventListIndex, eventListTop);
    	}
    	if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
    				"Set list view to position "+eventListIndex+", "+eventListTop);
	}

	/****************************************************
	 * Private Methods *
	 ****************************************************/

	/**
	 * <p>
	 * Helper method for setting up buttons
	 * </p>
	 * @param buttonLabel 
	 */
	private void setupButton(int id, int val, String buttonLabel) {
		Button myButton = (Button) this.findViewById(id);
		if (myButton == null) {
			Log.i(TAG, "Cannot find button '" + id + "' by ID, to set value '" + val + "'");
		}
		else {
			myButton.setText(buttonLabel);
			myButton.setOnClickListener(this);
			myButton.setTag(val);
			AcalTheme.setContainerFromTheme(myButton, AcalTheme.BUTTON);
		}
	}

	/**
	 * <p>
	 * Creates a new GridView object based on this Activities current state. The
	 * GridView created Will display this Activities MonthView
	 * </p>
	 * 
	 * @param addParent
	 *            <p>
	 *            Whether or not to set the ViewFlipper as the new GridView's
	 *            Parent. if set to false the caller is contracted to add a
	 *            parent to the GridView.
	 *            </p>
	 */
	private void createGridView(boolean addParent) {

		try {
			LayoutInflater inflater = (LayoutInflater) this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			// Get grid flipper and add month grid
			monthGridFlipper = (AcalViewFlipper) findViewById(R.id.month_grid_flipper);
			monthGridFlipper.setAnimationCacheEnabled(true);

			// Add parent if directed to do so
			monthGridRoot = inflater.inflate(R.layout.month_grid_view, (addParent?monthGridFlipper:null));

			// Title
			monthTitle = (TextView) monthGridRoot
					.findViewById(R.id.month_grid_title);
			// Grid
			monthGrid = (GridView) monthGridRoot
					.findViewById(R.id.month_default_gridview);
			monthGrid.setSelector(R.drawable.no_border);
			monthGrid.setOnTouchListener(this);
			monthGrid.setEnabled(false);
		} catch (Exception e) {
			Log.e(TAG, "Error occured creating gridview: " + e.getMessage());
		}
	}

	/**
	 * <p>
	 * Creates a new GridView object based on this Activities current state. The
	 * GridView created will display this Activities ListView
	 * </p>
	 * 
	 * @param addParent
	 *            <p>
	 *            Whether or not to set the ViewFlipper as the new GridView's
	 *            Parent. if set to false the caller is contracted to add a
	 *            parent to the GridView.
	 *            </p>
	 */
	private void createListView(boolean addParent) {
		try {
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			// Get List Flipper and add list
			listViewFlipper = (AcalViewFlipper) findViewById(R.id.month_list_flipper);
			listViewFlipper.setAnimationCacheEnabled(true);

			// Add parent if directed to do so
			if (addParent)
				eventListRoot = inflater.inflate(R.layout.month_list_view,
						listViewFlipper);
			else
				eventListRoot = inflater.inflate(R.layout.month_list_view, null);

			// Title
			eventListTitle = (TextView) eventListRoot.findViewById(R.id.month_list_title);

			// List
			eventList = (ListView) eventListRoot.findViewById(R.id.month_default_list);
			eventList.setSelector(R.drawable.no_border);
			eventList.setOnTouchListener(this);

		} catch (Exception e) {
			Log.e(TAG, "Error occured creating listview: " + e.getMessage(), e);
		}
	}

	/**
	 * <p>
	 * Flips the current month view either forward or back depending on flip
	 * amount parameter.
	 * </p>
	 * 
	 * @param flipAmount
	 *            <p>
	 *            The number of months to move forward(Positive) or
	 *            back(negative). Setting to 0 may cause unexpected behaviour.
	 *            </p>
	 */
	private void flipMonth(int flipAmount) {
		try {
			int cur = monthGridFlipper.getDisplayedChild();
			createGridView(false); // We will attach the parent ourselves.
			AcalDateTime newDate = (AcalDateTime) displayedMonth.clone();

			// Handle year change
			newDate.set(AcalDateTime.DAY_OF_MONTH, 1);
			int newMonth = newDate.get(AcalDateTime.MONTH) + flipAmount;
			int newYear = newDate.get(AcalDateTime.YEAR);
			while (newMonth > AcalDateTime.DECEMBER) {
				newMonth -= 12;
				newYear++;
			}
			while (newMonth < AcalDateTime.JANUARY) {
				newMonth += 12;
				newYear--;
			}
			// Set newDate values
			newDate.set(AcalDateTime.YEAR, newYear);
			newDate.set(AcalDateTime.MONTH, newMonth);

			// Change this Activities state
			changeDisplayedMonth(newDate);

			// Set the new views parent. We need to ensure that the view does
			// not already have one.
			if (monthGrid.getParent() == monthGridFlipper)
				monthGridFlipper.removeView(monthGrid);
			monthGridFlipper.addView(monthGridRoot, cur + 1);

			// Make sure View responds to gestures
			monthGrid.setFocusableInTouchMode(true);
		} catch (Exception e) {
			Log.e(TAG, "Error occured in flipMonth: " + e.getMessage());
		}
	}

	
	/**
	 * <p>
	 * Flips the current Event view either forward or back depending on flip
	 * amount parameter.
	 * </p>
	 * 
	 * @param flipAmount
	 *            <p>
	 *            The number of days to move forward(Positive) or
	 *            back(negative). Setting to 0 may cause unexpected behaviour.
	 *            </p>
	 */
	private void flipDay() {
		try {
			int cur = listViewFlipper.getDisplayedChild();
			createListView(false);
			listViewFlipper.addView(eventListRoot, cur + 1);
		} catch (Exception e) {
			Log.e(TAG, "Error occured in flipDay: " + e.getMessage());
		}
	}

	/**
	 * <p>
	 * Called when user has selected 'Settings' from menu. Starts Settings
	 * Activity.
	 * </p>
	 */
	private void startSettings() {
		this.startActivity(new Intent(this, Settings.class));
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
	 * Responsible for nice animation when ViewFlipper changes from one View to
	 * the Next.
	 * </p>
	 * 
	 * @param objectTouched
	 *            <p>
	 *            The Object that was 'swiped'
	 *            </p>
	 * @param left
	 *            <p>
	 *            Indicates swipe direction. If true, swipe left, else swipe
	 *            right.
	 *            </p>
	 * 
	 * @return <p>
	 *         Swipe success. False if object passed is not 'swipable'
	 *         </p>
	 */
	private boolean swipe(Object objectTouched, boolean left) {
		if (objectTouched == null)
			return false;
		else if (objectTouched == monthGrid) {
			if (left) {
				monthGridFlipper.setInAnimation(leftIn);
				monthGridFlipper.setOutAnimation(leftOut);
				if (monthAdapter != null) monthAdapter.animationInit();
				flipMonth(1);
			} else {
				monthGridFlipper.setInAnimation(rightIn);
				monthGridFlipper.setOutAnimation(rightOut);
				if (monthAdapter != null) monthAdapter.animationInit();
				flipMonth(-1);
			}
			int cur = monthGridFlipper.getDisplayedChild();
			
			//TODO We need to prevent cache updates from upsetting this animation
			monthGridFlipper.showNext();
			monthGridFlipper.removeViewAt(cur);
			return true;

		} else if (objectTouched == eventList) {
			int curMonth = selectedDate.get(AcalDateTime.MONTH);
			int dispMonth = displayedMonth.get(AcalDateTime.MONTH);
			AcalDateTime newDate = null;
			listViewFlipper.setFlipInterval(0);
			if (left) {
				listViewFlipper.setInAnimation(leftIn);
				listViewFlipper.setOutAnimation(leftOut);
				newDate = AcalDateTime.addDays(selectedDate, 1);
				flipDay();
			} else {
				listViewFlipper.setInAnimation(rightIn);
				listViewFlipper.setOutAnimation(rightOut);
				newDate = AcalDateTime.addDays(selectedDate, -1);
				flipDay();

			}
			int cur = listViewFlipper.getDisplayedChild();
			listViewFlipper.showNext();
			listViewFlipper.removeViewAt(cur);
			changeSelectedDate(newDate);
			if (eventList.getParent() == listViewFlipper)
				listViewFlipper.removeView(eventList);
			eventList.setFocusableInTouchMode(true);

			// Did the month change?
			if ((curMonth == dispMonth)
					&& (curMonth != selectedDate.get(AcalDateTime.MONTH))) {
				// Flip month as well
				swipe(monthGrid, left);
			}
			return true;
		}
		return false;
	}

	/**
	 * <p>
	 * Determines what object was under the 'finger' of the user when they
	 * started a gesture.
	 * </p>
	 * 
	 * @param x
	 *            <p>
	 *            The X co-ordinate of the press.
	 *            </p>
	 * @param y
	 *            <p>
	 *            The Y co-ordinate of the press.
	 *            </p>
	 * @return <p>
	 *         The object beneath the press, or null if none.
	 *         </p>
	 */
	private Object getTouchedObject(double x, double y) {
		int[] lvc = new int[2];
		this.eventList.getLocationOnScreen(lvc);
		int lvh = this.eventList.getHeight();
		int lvw = this.eventList.getWidth();
		if ((x >= lvc[0]) && (x <= lvc[0] + lvw) && (y >= lvc[1])
				&& (y <= lvc[1] + lvh))
			return this.eventList;

		int[] gvc = new int[2];
		this.monthGrid.getLocationOnScreen(gvc);
		int gvh = this.monthGrid.getHeight();
		int gvw = this.monthGrid.getWidth();
		if ((x >= gvc[0]) && (x <= gvc[0] + gvw) && (y >= gvc[1])
				&& (y <= gvc[1] + gvh))
			return this.monthGrid;

		return null;
	}

	/****************************************************
	 * Public Methods *
	 ****************************************************/

	/**
	 * <p>
	 * Changes the displayed month to the month represented by the provided
	 * calendar.
	 * </p>
	 */
	public void changeDisplayedMonth(AcalDateTime calendar) {
		displayedMonth = calendar.applyLocalTimeZone();
		monthTitle.setText(AcalDateTime.fmtMonthYear(calendar));
		monthGrid.setScrollBarStyle(GridView.SCROLLBARS_INSIDE_OVERLAY);
		monthGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		monthGrid.setPadding(2, 0, 2, 0);

		/*
		ViewParent vp = gridView.getParent();
		while( vp != null && !(vp instanceof View) ) {
			vp = vp.getParent();
		}
		if ( vp != null ) {
			int spare = ((View) vp).getWidth() - gridView.getWidth();
			gridView.setPadding(spare/2, 0, spare/2, 0);
		}
		*/
		
		if (AcalDateTime.isWithinMonth(selectedDate, displayedMonth)) {
			if (monthAdapter != null) monthAdapter.close();
			monthAdapter = new MonthAdapter(this, selectedDate, selectedDate,new Animation[]{leftIn,leftOut,rightIn,rightOut});
			monthGrid.setAdapter(monthAdapter);
		}
		else {
			if (monthAdapter != null) monthAdapter.close();
			monthAdapter = new MonthAdapter(this, displayedMonth, selectedDate,new Animation[]{leftIn,leftOut,rightIn,rightOut});
			monthGrid.setAdapter(monthAdapter);
			monthGrid.setAdapter(monthAdapter);
		}
		monthGrid.refreshDrawableState();
	}

	
	/**
	 * <p>
	 * Changes the selected date to the date represented by the provided
	 * calendar.
	 * </p>
	 */
	public void changeSelectedDate(AcalDateTime c) {

		if ( selectedDate != null && selectedDate.equals(c) ) {
			rememberCurrentPosition();
		}
		else {
			selectedDate = c.applyLocalTimeZone();
			eventListTop = 0;
			eventListIndex = 0;
		}
		eventListAdapter = new EventListAdapter(this, selectedDate.clone());

		if ( eventList == null ) createListView(true);
		eventList.setAdapter(eventListAdapter);
		eventList.refreshDrawableState();
		restoreCurrentPosition();
		
		eventListTitle.setText(AcalDateTime.fmtDayMonthYear(c));

		if (AcalDateTime.isWithinMonth(selectedDate, displayedMonth)) {
			if ( monthAdapter == null )  {
				monthAdapter = new MonthAdapter(this, displayedMonth.clone(), selectedDate.clone(),new Animation[]{leftIn,leftOut,rightIn,rightOut});
				this.monthGrid.setAdapter(monthAdapter);
			} else {
				monthAdapter.updateSelectedDay(selectedDate);
				monthAdapter.notifyDataSetChanged();
			}
		} else {
			if ( monthAdapter == null ) {
				monthAdapter = new MonthAdapter(this, displayedMonth.clone(), selectedDate.clone(),new Animation[]{leftIn,leftOut,rightIn,rightOut});
				this.monthGrid.setAdapter(monthAdapter);
			}
			monthAdapter.updateSelectedDay(selectedDate);
			monthAdapter.notifyDataSetChanged();
		}
	}

	private Dialog	deletingDialog;
	private Handler mHandler = new Handler() {
		private boolean	deleteSucceeded = false;
		
		public void handleMessage(Message msg) {

			switch (msg.what) {

			case SHOW_DELETING: 
				showDialog(DELETING_DIALOG);
				break;

			case DELETE_SUCCEEDED:
				//dismiss dialog
				mHandler.removeMessages(DELETE_FAILED);
				if (deletingDialog != null) deletingDialog.dismiss();
				deleteSucceeded = true;
				break;

			case DELETE_FAILED:
				if (deletingDialog != null) deletingDialog.dismiss();
				if ( deleteSucceeded ) {
					// Don't know why we get here, but we do! - cancel save failed when save succeeds. we shouldn't see this anymore.
					Log.w(TAG,"This should have been fixed now yay!",new Exception());
				}
				else {
					Toast.makeText(MonthView.this, "Something went wrong trying to save the change.", Toast.LENGTH_LONG).show();
				}
				break;
			}

		}
	};



	public void deleteEvent(long resourceId, String recurrenceId, int action, int instances ) {

		try {
			Resource r = Resource.fromDatabase(this, resourceId);
			EventInstance event = (EventInstance) CalendarInstance.fromResourceAndRRId(r, recurrenceId);

			if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
					"saveChanges: "+event.getSummary()+
					", starts "+event.getStart().toPropertyString(PropertyName.DTSTART)+
					", with "+event.getAlarms().size()+" alarms.");
			//display savingdialog

			ResourceManager.getInstance(this).sendRequest(new RREventEditedRequest(this, event, action, instances));

			//set message for 10 seconds to fail.
			mHandler.sendEmptyMessageDelayed(DELETE_FAILED, 100000);

			//showDialog(SAVING_DIALOG);
			mHandler.sendEmptyMessageDelayed(SHOW_DELETING,50);


		}
		catch (Exception e) {
			if ( e.getMessage() != null ) Log.d(TAG,e.getMessage());
			if (Constants.LOG_DEBUG)Log.d(TAG,Log.getStackTraceString(e));
			Toast.makeText(this, getString(R.string.ErrorSavingEvent), Toast.LENGTH_LONG).show();
		}
		
	}

	//Dialogs
	protected Dialog onCreateDialog(int id) {

		// These dialogs don't depend on 'event' having been initialised.
		switch (id) {
		case DELETING_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(this.getString(R.string.Deleting));
			builder.setCancelable(false);
			deletingDialog = builder.create();
			return deletingDialog;
		}
		return null;
	}

	
	/********************************************************************
	 * Implemented Interface Overrides *
	 ********************************************************************/

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
	 * Called when user has touched the screen. Handled by our Gesture Detector.
	 * </p>
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (Constants.debugMonthView && Constants.LOG_VERBOSE)	Log.v(TAG, "onTouchEvent called at (" + event.getRawX() + ","
					+ event.getRawY() + ")");
		return gestureDetector.onTouchEvent(event);
	}

	/**
	 * <p>
	 * Called when user has touched the screen. Handled by our Gesture Detector.
	 * </p>
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
	 *      android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View view, MotionEvent touch) {
		if (Constants.debugMonthView && Constants.LOG_VERBOSE) 		Log.v(TAG, "onTouch called with touch at (" + touch.getRawX() + ","
					+ touch.getRawY() + ") touching view " + view.getId());
		return this.gestureDetector.onTouchEvent(touch);
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

	/**
	 * <p>
	 * The main handler for Gestures in this activity. At this time we are only
	 * interested in scroll gestures. Determines what object the gesture
	 * occurred on, whether the gesture is suitable to respond to, and finally,
	 * kicks of an appropriate response.
	 * </p>
	 */
	@Override
	public boolean onScroll(MotionEvent start, MotionEvent current, float dx,
			float dy) {
		try {
			// We can't work with null objects
			if (start == null || current == null)
				return false;

			// Calculate all values required to identify if we need to react
			double startX = start.getRawX();
			double startY = start.getRawY();
			double distX = current.getRawX() - startX;
			double distY = current.getRawY() - startY;
			double totalDistSq = distX*distX+distY*distY; // Math.sqrt(Math.pow(distX, 2) + Math.pow(distY, 2));
			double angle = 180 + ((Math.atan2(distY, distX)) * (180.0 / Math.PI));
			Object scrolledOn = getTouchedObject(startX, startY);
			boolean isHorizontal = false;
			boolean isVertical = false;
			if ((angle > 360 - maxAngleDev || angle < 0 + maxAngleDev)
					|| (angle > 180 - maxAngleDev && angle < 180 + maxAngleDev)) {
				isHorizontal = true;
			} else if ((angle > 90 - maxAngleDev && angle < 90 + maxAngleDev)
					|| (angle > 270 - maxAngleDev && angle < 270 + maxAngleDev)) {
				isVertical = true;
			}

			// Report calculations
			if (Constants.debugMonthView && Constants.LOG_DEBUG)	Log.d(TAG, "onScroll called with onDown at (" + startX + ","
						+ startY + ") " + "and with distance (" + distX + ","
						+ distY + "), " + "angle is" + angle);

			// Some conditions that work out if we are interested in this event.
			if (    (consumedTime == start.getDownTime()) || // We've already consumed the event
					(totalDistSq < minDistanceSquared) || // Not a long enough swipe
					(scrolledOn == null) || // Nothing underneath touch of interest
					(!isHorizontal && !isVertical) // Direction is not of intrest
			) {
				if (Constants.debugMonthView && Constants.LOG_DEBUG)Log.d(TAG, "onScroll ignored.");
				return false;
			}

			long startFlip = System.currentTimeMillis();
			if (Constants.debugMonthView && Constants.LOG_DEBUG)Log.d(TAG, "Valid onScroll detected.");

			// If we are here, we have a valid scroll event to process
			if (monthGrid != null && scrolledOn == monthGrid) {
				if (isHorizontal) {
					if (distX > 0)
						swipe(monthGrid, false);
					else
						swipe(monthGrid, true);
					consumedTime = start.getDownTime();
					return true;
				} else if (isVertical) {
					return false;
				}
			} else if (eventList != null && scrolledOn == eventList) {
				if (isHorizontal) {
					this.eventListAdapter.setClickEnabled(false);
					if (distX > 0)
						swipe(eventList, false);
					else
						swipe(eventList, true);
					consumedTime = start.getDownTime();;
					if (Constants.debugMonthView && Constants.LOG_DEBUG) Log.d(TAG,
								"Scroll took ."
										+ (System.currentTimeMillis() - startFlip)
										+ "ms to process.");
					return true;
				}
			}
		} catch (Exception e) {
			Log.e(TAG,
					"Unknown error occurred processing scroll event: "
							+ e.getMessage() + Log.getStackTraceString(e));
		}
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent motion) {
		switch (motion.getAction()) {
			case (MotionEvent.ACTION_MOVE): {
	            int dx = (int) (motion.getX() * motion.getXPrecision() );
				int dy = (int) (motion.getY() * motion.getYPrecision() );
//				if ( Constants.LOG_VERBOSE )
//					Log.v(TAG,"Trackball event of size "+motion.getHistorySize()+" x/y"+motion.getX()+"/"+motion.getY()
//								+ " - precision: " + motion.getXPrecision() +"/" + motion.getYPrecision());
				if ( dx > 0 )
					swipe(eventList, true);
				else if ( dx < 0 )
					swipe(eventList, false);
				else if ( dy > 0 )
					changeSelectedDate(selectedDate.addDays(7));
				else if ( dy < 0 )
					changeSelectedDate(selectedDate.addDays(-7));

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

	/**
	 * <p>
	 * Handles button Clicks
	 * </p>
	 */
	@Override
	public void onClick(View clickedView) {
		int button = (int) ((Integer) clickedView.getTag());
		switch (button) {
			case TODAY:
				AcalDateTime cal = new AcalDateTime();

				if (cal.getEpochDay() == this.selectedDate.getEpochDay()) {
					this.monthGridFlipper.setAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
					this.listViewFlipper.setAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
				}
				this.changeSelectedDate(cal);
				this.changeDisplayedMonth(cal);

				break;
			case ADD:
				Bundle bundle = new Bundle();
				bundle.putParcelable(EventEdit.NEW_EVENT_DATE_TIME_KEY, this.selectedDate.clone().applyLocalTimeZone().setHour(9).setMinute(0));
				bundle.putInt(EventEdit.ACTION_KEY, EventEdit.ACTION_CREATE);
				Intent eventEditIntent = new Intent(this, EventEdit.class);
				eventEditIntent.putExtras(bundle);
				this.startActivity(eventEditIntent);
				break;
			case WEEK:
				if (prefs.getBoolean(getString(R.string.prefDefaultView), false)) {
					this.finish();
				}
				else {
					bundle = new Bundle();
					bundle.putParcelable("StartDay", selectedDate);
					Intent weekIntent = new Intent(this, WeekViewActivity.class);
					weekIntent.putExtras(bundle);
					this.startActivityForResult(weekIntent, PICK_DAY_FROM_WEEK_VIEW);
				}
				break;
			case YEAR:
				bundle = new Bundle();
				bundle.putInt("StartYear", selectedDate.getYear());
				Intent yearIntent = new Intent(this, YearView.class);
				yearIntent.putExtras(bundle);
				this.startActivityForResult(yearIntent, PICK_MONTH_FROM_YEAR_VIEW);
				break;
			default:
				Log.w(TAG, "Unrecognised button was pushed in MonthView.");
		}
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return this.eventListAdapter.contextClick(item);
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( resultCode == RESULT_OK ) {
			switch ( requestCode ) {
			case PICK_DAY_FROM_WEEK_VIEW:
				if (data.hasExtra("selectedDate")) {
					try {
						AcalDateTime day = (AcalDateTime) data.getParcelableExtra("selectedDate");
						this.changeSelectedDate(day);
						this.changeDisplayedMonth(day);
					} catch (Exception e) {
						Log.w(TAG, "Error getting month back from year view: "+e);
					}
				}
				break;
				case PICK_MONTH_FROM_YEAR_VIEW:
					if (data.hasExtra("selectedDate")) {
						try {
							AcalDateTime month = (AcalDateTime) data.getParcelableExtra("selectedDate");
							this.changeDisplayedMonth(month);
						} catch (Exception e) {
							Log.w(TAG, "Error getting month back from year view: "+e);
						}
					}
					break;
				case PICK_TODAY_FROM_EVENT_VIEW:
					try {
						AcalDateTime chosenDate = (AcalDateTime) data.getParcelableExtra("selectedDate");
						this.changeDisplayedMonth(chosenDate);
						this.changeSelectedDate(chosenDate);
					} catch (Exception e) {
						Log.w(TAG, "Error getting month back from year view: "+e);
					}
			}
			// Save state
			if (Constants.LOG_DEBUG) Log.d(TAG, "Writing month view state to file.");
			ObjectOutputStream outputStream = null;
			try {
				outputStream = new ObjectOutputStream(new FileOutputStream(
						STATE_FILE));
				outputStream.writeObject(this.selectedDate);
				outputStream.writeObject(this.displayedMonth);
			} catch (FileNotFoundException ex) {
				Log.w(TAG,
						"Error saving MonthView State - File Not Found: "
								+ ex.getMessage());
			} catch (IOException ex) {
				Log.w(TAG,
						"Error saving MonthView State - IO Error: "
								+ ex.getMessage());
			} finally {
				// Close the ObjectOutputStream
				try {
					if (outputStream != null) {
						outputStream.flush();
						outputStream.close();
					}
				} catch (IOException ex) {
					Log.w(TAG, "Error closing MonthView file - IO Error: "
							+ ex.getMessage());
				}
			}
		}
	}

	/************************************************************************
	 * Required Overrides that aren't used *
	 ************************************************************************/

	@Override
	public boolean onDown(MotionEvent downEvent) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent downEvent) {
	}

	@Override
	public void onShowPress(MotionEvent downEvent) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent upEvent) {
		return false;
	}

	@Override
	public void resourceResponse(ResourceResponse<Long> response) {
		Object result = response.result();
		if (result != null) {
			if (result instanceof Long) {
				mHandler.sendMessage(mHandler.obtainMessage(DELETE_SUCCEEDED, result));
			}
		}
	}
}
