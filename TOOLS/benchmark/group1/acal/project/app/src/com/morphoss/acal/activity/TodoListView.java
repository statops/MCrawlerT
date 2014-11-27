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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VTodo;
import com.morphoss.acal.service.aCalService;

/**
 * <h1>Todo List View</h1>
 * 
 * <p>
 * This view is split into 2 sections:
 * </p>
 * <ul>
 * <li>Todo List - A grid view controlled by a View Flipper displaying all the
 * outstanding Todo items</li>
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
public class TodoListView extends AcalActivity implements OnClickListener {

	public static final String TAG = "aCal TodoListView";
	private final static boolean DEBUG = true && Constants.DEBUG_MODE;

	private boolean invokedFromView = false;

	private ListView todoList;
	private TodoListAdapter todoListAdapter;

	/* Fields relating to state */
	private boolean showFuture = true;
	private boolean showCompleted = false;

	/* Fields relating to buttons */
	public static final int DUE = 0;
	public static final int TODO = 1;
	public static final int ADD = 3;

	private int	todoListTop;

	private int	todoListIndex;

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
		this.setContentView(R.layout.todo_list_view);

		Bundle b = this.getIntent().getExtras();
		if ( b != null && b.containsKey("InvokedFromView") )
			invokedFromView = true;

		// make sure aCalService is running
		this.startService(new Intent(this, aCalService.class));

		// Set up buttons
		this.setupButton(R.id.todo_add_button, ADD, "+");
		this.setSelections();

	}


	@Override
	public void onPause() {
		super.onPause();
		rememberCurrentPosition();
		todoList = null;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateLayout();
	}
	
	private void updateLayout() {
		if ( this.todoList == null ) createListView(true);
		this.todoListAdapter = new TodoListAdapter(this, showCompleted, showFuture );
		this.todoList.setAdapter(todoListAdapter);
		restoreCurrentPosition();
	}


	private void rememberCurrentPosition() {
		// save index and top position
		if ( todoList != null ) {
			todoListIndex = todoList.getFirstVisiblePosition();
			View v = todoList.getChildAt(0);
			todoListTop = (v == null) ? 0 : v.getTop();
			if ( DEBUG ) Log.println(Constants.LOGD, TAG,
					"Saved list view position of "+todoListIndex+", "+todoListTop);
		}
	}


	private void restoreCurrentPosition() {
		if ( todoList != null ) {
			todoList.setSelectionFromTop(todoListIndex, todoListTop);
		}
		if ( DEBUG ) Log.println(Constants.LOGD, TAG,
				"Set list view to position "+todoListIndex+", "+todoListTop);
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
			Log.e(TAG, "Cannot find button '" + id + "' by ID, to set value '" + val + "'");
			Log.i(TAG, Log.getStackTraceString(new Exception()));
		}
		else {
			myButton.setText(buttonLabel);
			myButton.setOnClickListener(this);
			myButton.setTag(val);
			AcalTheme.setContainerFromTheme(myButton, AcalTheme.BUTTON);
		}
	}


	private void setSelections() {
		this.setupButton(R.id.todo_due_button, DUE, (this.showFuture ? getString(R.string.Due) : getString(R.string.All) ));
		this.setupButton(R.id.todo_all_button, TODO, (this.showCompleted ? getString(R.string.Todo) : getString(R.string.All)));

		TextView title = (TextView) this.findViewById(R.id.todo_list_title);
		title.setText( showFuture && showCompleted ? R.string.allTasksTitle
				: ( showFuture ? R.string.incompleteTasksTitle 
						: (showCompleted ? R.string.dueTasksTitle
								: R.string.incompleteTasksDue)
				)
		);

		this.todoListAdapter = null;
		updateLayout();
	}


	/**
	 * <p>
	 * Creates a new ListView object based on this Activities current state. The
	 * ListView created will display this Activities ListView
	 * </p>
	 * 
	 * @param addParent
	 *            <p>
	 *            Whether or not to set the ViewFlipper as the new GridView's
	 *            Parent. if set to false the caller is contracted to add a
	 *            parent to the ListView.
	 *            </p>
	 */
	private void createListView(boolean addParent) {
		try {
			// List
			todoList = (ListView) findViewById(R.id.todo_list);
			todoList.setSelector(R.drawable.no_border);

		} catch (Exception e) {
			Log.e(TAG, "Error occured creating listview: " + e.getMessage());
		}
	}


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
	 * Called when user has selected 'Events' from menu. Starts MonthView
	 * Activity.
	 * </p>
	 */
	private void startMonthView() {
		if ( invokedFromView )
			this.finish();
		else {
			Intent monthViewIntent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putInt("InvokedFromView",1);
			monthViewIntent.putExtras(bundle);
			monthViewIntent.setClassName("com.morphoss.acal",
			"com.morphoss.acal.activity.MonthView");
			this.startActivity(monthViewIntent);
		}
	}


	/****************************************************
	 * Public Methods *
	 ****************************************************/

	public void deleteTodo( long resourceId, int action ) {
		try {
			Resource r = Resource.fromDatabase(this, resourceId);

			ResourceManager.getInstance(this).sendRequest(
					new RRResourceEditedRequest(
							new ResourceResponseListener<Long>() {
								@Override
								public void resourceResponse(
										ResourceResponse<Long> response) {
								}

							},
							r.getCollectionId(), resourceId, 
							VComponent.createComponentFromResource(r),
							RRResourceEditedRequest.ACTION_DELETE)
					);
		}
		catch (Exception e) {
			if ( e.getMessage() != null ) Log.d(TAG,e.getMessage());
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD, TAG, Log.getStackTraceString(e));
			Toast.makeText(this, getString(R.string.ErrorDeletingTask), Toast.LENGTH_LONG).show();
		}
	}

	public void completeTodo( long resourceId, int action ) {
		try {
			Resource r = Resource.fromDatabase(this, resourceId);
			VCalendar todoCalendarResource = (VCalendar) VComponent.createComponentFromResource(r);
			todoCalendarResource.setEditable();
			VTodo task = (VTodo) todoCalendarResource.getMasterChild();
			task.setCompleted(AcalDateTime.getInstance());
			task.setPercentComplete(100);
			todoCalendarResource.updateTimeZones();

			ResourceManager.getInstance(this).sendRequest(
					new RRResourceEditedRequest(
							new ResourceResponseListener<Long>() {
								@Override
								public void resourceResponse(
										ResourceResponse<Long> response) {
								}

							},
							r.getCollectionId(), resourceId, 
							todoCalendarResource,
							RRResourceEditedRequest.ACTION_UPDATE)
					);
			
		}
		catch (Exception e) {
			if ( e.getMessage() != null ) Log.d(TAG,e.getMessage());
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD, TAG, Log.getStackTraceString(e));
			Toast.makeText(this, getString(R.string.ErrorCompletingTask), Toast.LENGTH_LONG).show();
		}
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
		inflater.inflate(R.menu.tasks_options_menu, menu);
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
		case R.id.eventsMenuItem:
			startMonthView();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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
		case DUE:
			this.showFuture = !this.showFuture;
			this.setSelections();
			break;
		case TODO:
			this.showCompleted = !this.showCompleted;
			this.setSelections();
			break;
		case ADD:
			Intent todoEditIntent = new Intent(this, TodoEdit.class);
			todoEditIntent.putExtra(TodoEdit.KEY_OPERATION, TodoEdit.ACTION_CREATE);
			this.startActivity(todoEditIntent);
			break;
		default:
			Log.w(TAG, "Unrecognised button was pushed in TodoListView.");
		}
	}


	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return this.todoListAdapter.contextClick(item);
	}


	/************************************************************************
	 * Required Overrides that aren't used *
	 ************************************************************************/

}
