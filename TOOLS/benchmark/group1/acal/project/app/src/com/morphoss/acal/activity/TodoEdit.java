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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.R;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDateTimeFormatter;
import com.morphoss.acal.acaltime.AcalDuration;
import com.morphoss.acal.acaltime.AcalRepeatRule;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedEvent;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedListener;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requests.RRRequestInstance;
import com.morphoss.acal.dataservice.CalendarInstance;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.dataservice.TodoInstance;
import com.morphoss.acal.davacal.AcalAlarm;
import com.morphoss.acal.davacal.AcalAlarm.ActionType;
import com.morphoss.acal.davacal.AcalAlarm.RelateWith;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VTodo;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.service.aCalService;
import com.morphoss.acal.widget.AlarmDialog;
import com.morphoss.acal.widget.DateTimeDialog;
import com.morphoss.acal.widget.DateTimeSetListener;

@SuppressWarnings("rawtypes")
public class TodoEdit extends AcalActivity
	implements OnCheckedChangeListener, OnSeekBarChangeListener,
				ResourceChangedListener, ResourceResponseListener {

	public static final String TAG = "aCal TodoEdit";

	private VTodo todo;

	public static final int ACTION_NONE = -1;
	public static final int ACTION_CREATE = 0;
	public static final int ACTION_MODIFY_SINGLE = 1;
	public static final int ACTION_MODIFY_ALL = 2;
	public static final int ACTION_MODIFY_ALL_FUTURE = 3;
	public static final int ACTION_DELETE_SINGLE = 4;
	public static final int ACTION_DELETE_ALL = 5;
	public static final int ACTION_DELETE_ALL_FUTURE = 6;
	public static final int ACTION_COMPLETE = 7;
	public static final int ACTION_EDIT = 8;
	public static final int ACTION_COPY = 9;

	private int action = ACTION_NONE;

	private static final int FROM_DIALOG = 10;
	private static final int DUE_DIALOG = 11;
	private static final int COMPLETED_DIALOG = 12;
	private static final int ADD_ALARM_DIALOG = 20;
	private static final int SET_REPEAT_RULE_DIALOG = 21;
	private static final int INSTANCES_TO_CHANGE_DIALOG = 30;
	private static final int LOADING_DIALOG = 0xfeed;
	private static final int SAVING_DIALOG = 0xbeef;

	boolean prefer24hourFormat = false;
	
	private String[] repeatRules;
	private String[] todoChangeRanges; // See strings.xml R.array.TodoChangeAffecting
		
	private String[] alarmRelativeTimeStrings;
	// Must match R.array.RelativeAlarmTimes (strings.xml)
	public static final AcalDuration[] alarmValues = new AcalDuration[] {
		new AcalDuration(),
		new AcalDuration("-PT10M"),
		new AcalDuration("-PT15M"),
		new AcalDuration("-PT30M"),
		new AcalDuration("-PT1H"),
		new AcalDuration("-PT2H"),
		//** Custom **//
	};

	public static final String	KEY_CACHE_OBJECT	= "CacheObject";
	public static final String	KEY_OPERATION		= "Operation";
	public static final String	KEY_RESOURCE		= "Resource";
	public static final String	KEY_VCALENDAR_BLOB	= "VCalendar";
	

	private String[] repeatRulesValues;
	
	//GUI Components
	private Button btnStartDate;
	private Button btnDueDate;
	private Button btnCompleteDate;
	private LinearLayout sidebar;
	private LinearLayout sidebarBottom;
	private TextView todoName;
	private TextView locationView;
	private TextView notesView;
	private TableLayout alarmsList;
	private LinearLayout repeatsLayout;
	private Button btnAddRepeat;
	private RelativeLayout alarmsLayout;
	private Button btnAddAlarm;
	private LinearLayout collectionsLayout;
	private Spinner spinnerCollection;
	private Button btnSaveChanges;	
	private Button btnCancelChanges;
	

	private int percentComplete = 0;
	private SeekBar percentCompleteBar;
	private TextView percentCompleteText;
	
	//Active collections for create mode
	private Collection currentCollection;	//currently selected collection
	private CollectionForArrayAdapter[] collectionsArray;

	private List<AcalAlarm> alarmList;
	
	private boolean originalHasOccurrence = false;
	private String originalOccurence = "";

	private int	currentOperation;
	private static final int REFRESH = 0;
	private static final int FAIL = 1;
	private static final int CONFLICT = 2;
	private static final int SHOW_LOADING = 3;
	private static final int GIVE_UP = 4;
	private static final int SAVE_RESULT = 5;
	private static final int SAVE_FAILED = 6;
	private static final int SHOW_SAVING = 7;
	
	private Dialog loadingDialog = null;
	private ResourceManager	resourceManager;

	private Dialog savingDialog = null;

	private long	rid = -1;

	private boolean saveSucceeded = false;
	private boolean isSaving = false;
	private boolean isLoading = false;

	private static TodoEdit handlerContext = null;
	
	private static Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if ( handlerContext != null ) handlerContext.messageHandler(msg);
		}
	};

	
	private void messageHandler(Message msg) {	
		switch( msg.what ) {
			case REFRESH:
				if ( loadingDialog != null ) {
					loadingDialog.dismiss();
					loadingDialog = null;
				}
				updateLayout();
				break;

			case CONFLICT:
				Toast.makeText(
						TodoEdit.this,
						"The resource you are editing has been changed or deleted on the server.",
						Toast.LENGTH_LONG).show();
				break;
			case SHOW_LOADING:
				if ( todo == null ) showDialog(LOADING_DIALOG);
				break;
		
			case FAIL:
				if ( isLoading ) {
					Toast.makeText(TodoEdit.this, "Error loading data.", Toast.LENGTH_LONG).show();
					isLoading = false;
				}
				else if ( isSaving ) {
					isSaving = false;
					if ( savingDialog != null ) savingDialog
							.dismiss();
					Toast.makeText( TodoEdit.this, "Something went wrong trying to save data.", Toast.LENGTH_LONG).show();
					setResult(Activity.RESULT_CANCELED, null);
				}
				finish();
				break;
			case GIVE_UP:
				if ( loadingDialog != null ) {
					loadingDialog.dismiss();
					Toast.makeText(
							TodoEdit.this,
							"Error loading event data.",
							Toast.LENGTH_LONG).show();
					finish();
					return;
				}
				break;

			case SHOW_SAVING:
				isSaving = true;
				showDialog(SAVING_DIALOG);
				break;

			case SAVE_RESULT:
				// dismiss
				// dialog
				mHandler.removeMessages(SAVE_FAILED);
				isSaving = false;
				if ( savingDialog != null ) savingDialog.dismiss();
				long res = (Long) msg.obj;
				if ( res >= 0 ) {
					Intent ret = new Intent();
					Bundle b = new Bundle();
					ret.putExtras(b);
					setResult(RESULT_OK, ret);
					saveSucceeded = true;

					finish();

				}
				else {
					Toast.makeText(TodoEdit.this, "Error saving event data.", Toast.LENGTH_LONG).show();
				}
				break;

			case SAVE_FAILED:
				isSaving = false;
				if ( savingDialog != null ) savingDialog.dismiss();
				if ( saveSucceeded ) {
					// Don't know why we get here, but we do! - cancel save failed when save succeeds.
					// we shouldn't see this anymore.
					Log.w(TAG, "This should have been fixed now yay!", new Exception());
				}
				else {
					Toast.makeText( TodoEdit.this, "Something went wrong trying to save data.", Toast.LENGTH_LONG).show();
					setResult(Activity.RESULT_CANCELED, null);
					finish();
				}
				break;
		}
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.todo_edit);

		//Ensure service is actually running
		startService(new Intent(this, aCalService.class));

		ContentValues[] taskCollections = DavCollections.getCollections( getContentResolver(), DavCollections.INCLUDE_TASKS );
		if ( taskCollections.length == 0 ) {
			Toast.makeText(this, getString(R.string.errorMustHaveActiveCalendar), Toast.LENGTH_LONG).show();
			this.finish();	// can't work if no active collections
			return;
		}

		this.collectionsArray = new CollectionForArrayAdapter[taskCollections.length];
		int count = 0;
		long collectionId;
		for (ContentValues cv : taskCollections ) {
			collectionId = cv.getAsLong(DavCollections._ID);
			collectionsArray[count++] = new CollectionForArrayAdapter(this,collectionId);
		}

		
		resourceManager = ResourceManager.getInstance(this,this);
		todo = null;
		requestTodoResource();

		// Get time display preference
		prefer24hourFormat = prefs.getBoolean(getString(R.string.prefTwelveTwentyfour), false);

		alarmRelativeTimeStrings = getResources().getStringArray(R.array.RelativeAlarmTimes);
		todoChangeRanges = getResources().getStringArray(R.array.TodoChangeAffecting);

		this.populateLayout();
		if ( this.todo != null ) this.updateLayout();
	}

	@Override
	public void onDestroy() {
		if ( handlerContext == this ) handlerContext = null;
		super.onDestroy();
	}

	@SuppressWarnings("unchecked")
	private void requestTodoResource() {
		currentOperation = ACTION_CREATE;
		try {
			Bundle b = this.getIntent().getExtras();
			if ( b != null && b.containsKey(KEY_OPERATION) ) {
				currentOperation = b.getInt(KEY_OPERATION);
			}
			if ( b != null && b.containsKey(KEY_CACHE_OBJECT) ) {
				if ( currentOperation == ACTION_CREATE ) currentOperation = ACTION_EDIT;
				CacheObject cacheTodo = (CacheObject) b.getParcelable(KEY_CACHE_OBJECT);
				this.rid = cacheTodo.getResourceId();
				handlerContext = this;
				resourceManager.sendRequest(new RRRequestInstance(this, rid, cacheTodo.getRecurrenceId()));
				mHandler.sendMessageDelayed(mHandler.obtainMessage(SHOW_LOADING), 50);
				mHandler.sendMessageDelayed(mHandler.obtainMessage(GIVE_UP), 10000);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "No bundle from caller.", e);
		}

		if ( currentOperation == ACTION_CREATE ) {
			long preferredCollectionId = -1;
			try {
				preferredCollectionId = Long.parseLong(prefs.getString(PrefNames.defaultTasksCollection, "-1"));
			}
			catch( Exception e ) {}
			if ( preferredCollectionId == -1 || Collection.getInstance(preferredCollectionId, this) == null )
				preferredCollectionId = collectionsArray[0].getCollectionId();

			currentCollection = Collection.getInstance(preferredCollectionId, this);

			this.todo = new VTodo();
			this.action = ACTION_CREATE;
			this.todo.setPercentComplete(0);
		}
	}

	
	private void setTodo( VTodo newTodo ) {
		this.todo = newTodo;
		long collectionId = -1;		
		if ( currentOperation == ACTION_EDIT ) {
			this.action = ACTION_MODIFY_ALL;
			if ( isModifyAction() ) {
				String rr = (String)  this.todo.getRRule();
				if (rr != null && !rr.equals("") && !rr.equals(AcalRepeatRule.SINGLE_INSTANCE)) {
					this.originalHasOccurrence = true;
					this.originalOccurence = rr;
				}
				if (this.originalHasOccurrence) {
					this.action = ACTION_MODIFY_SINGLE;
				}
				else {
					this.action = ACTION_MODIFY_ALL;
				}
			}
		}
		else if ( currentOperation == ACTION_COPY ) {
			this.action = ACTION_CREATE;
		}

		if ( Collection.getInstance(collectionId,this) != null )
			currentCollection = Collection.getInstance(collectionId,this);

		if ( todo.getCompleted() != null ) todo.setPercentComplete( 100 );
	}

	
	/**
	 * The ArrayAdapter needs something which can return a displayed value on toString() and it's
	 * not really reasonable to add that sort of oddity to Collection itself.
	 */
	private class CollectionForArrayAdapter {
		Collection c;
		public CollectionForArrayAdapter(Context cx, long id) {
			c = Collection.getInstance(id, cx);
		}

		public long getCollectionId() {
			return c.getCollectionId();
		}

		public String toString() {
			return c.getDisplayName();
		}
	}

	
	/**
	 * Populate the screen initially.
	 */
	private void populateLayout() {

		//Sidebar
		sidebar = (LinearLayout)this.findViewById(R.id.TodoEditColourBar);
		sidebarBottom = (LinearLayout)this.findViewById(R.id.EventEditColourBarBottom);

		//Title
		this.todoName = (TextView) this.findViewById(R.id.TodoName);
		todoName.setSelectAllOnFocus(action == ACTION_CREATE);

		//Collection
		collectionsLayout = (LinearLayout)this.findViewById(R.id.TodoCollectionLayout);
		spinnerCollection = (Spinner) this.findViewById(R.id.TodoEditCollectionSelect);
		if (collectionsArray.length < 2) {
			spinnerCollection.setEnabled(false);
			collectionsLayout.setVisibility(View.GONE);
		}
		else {
			spinnerCollection.setOnItemSelectedListener( new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					setSelectedCollection(collectionsArray[arg2].getCollectionId());
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
				
			});
		}


		//date/time fields
		btnStartDate = (Button) this.findViewById(R.id.TodoFromDateTime);
		btnDueDate = (Button) this.findViewById(R.id.TodoDueDateTime);
		btnCompleteDate = (Button) this.findViewById(R.id.TodoCompletedDateTime);

		btnSaveChanges = (Button) this.findViewById(R.id.todo_apply_button);
		btnCancelChanges = (Button) this.findViewById(R.id.todo_cancel_button);
		

		locationView = (TextView) this.findViewById(R.id.TodoLocationContent);
		notesView = (TextView) this.findViewById(R.id.TodoNotesContent);
		
		alarmsLayout = (RelativeLayout) this.findViewById(R.id.TodoAlarmsLayout);
		alarmsList = (TableLayout) this.findViewById(R.id.alarms_list_table);
		btnAddAlarm = (Button) this.findViewById(R.id.TodoAlarmsButton);
		
		repeatsLayout = (LinearLayout) this.findViewById(R.id.TodoRepeatsLayout);
		btnAddRepeat = (Button) this.findViewById(R.id.TodoRepeatsContent);
		
		// Button listeners
		setButtonDialog(btnStartDate, FROM_DIALOG);
		setButtonDialog(btnDueDate, DUE_DIALOG);
		setButtonDialog(btnCompleteDate, COMPLETED_DIALOG);
		setButtonDialog(btnAddAlarm, ADD_ALARM_DIALOG);
		setButtonDialog(btnAddRepeat, SET_REPEAT_RULE_DIALOG);

		AcalTheme.setContainerFromTheme(btnSaveChanges, AcalTheme.BUTTON);
		AcalTheme.setContainerFromTheme(btnCancelChanges, AcalTheme.BUTTON);
		
		btnSaveChanges.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				applyChanges();
			}
		});

		btnCancelChanges.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				finish();
			}
		});
		

		percentCompleteText = (TextView) this.findViewById(R.id.TodoPercentCompleteText);
		percentCompleteBar = (SeekBar) this.findViewById(R.id.TodoPercentCompleteBar);
		percentCompleteBar.setIndeterminate(false);
		percentCompleteBar.setMax(100);
		percentCompleteBar.setKeyProgressIncrement(5);
		percentCompleteBar.setOnSeekBarChangeListener(this);
		percentCompleteText.setText(Integer.toString(percentComplete)+"%");
		percentCompleteBar.setProgress(percentComplete);
		
	}


	/**
	 * Update the screen whenever something has changed.
	 */
	private void updateLayout() {
		AcalDateTime start = todo.getStart();
		AcalDateTime due = todo.getDue();
		AcalDateTime completed = todo.getCompleted();

		percentComplete = todo.getPercentComplete();
		percentComplete = (percentComplete < 0 ? 0 : (percentComplete > 100 ? 100 : percentComplete));
		String title = todo.getSummary();
		todoName.setText(title);

		String location = todo.getLocation();
		locationView.setText(location);

		String description = todo.getDescription();
		notesView.setText(description);

		Integer colour = (currentCollection == null ? 0x808080c0 : currentCollection.getColour());
		if ( colour == null ) colour = 0x70a0a0a0;
		sidebar.setBackgroundColor(colour);
		sidebarBottom.setBackgroundColor(colour);
		todoName.setTextColor(colour);
		AcalTheme.setContainerColour(spinnerCollection,colour);

		ArrayAdapter<CollectionForArrayAdapter> collectionAdapter = 
				new ArrayAdapter<CollectionForArrayAdapter>(this,android. R.layout.select_dialog_item, collectionsArray);
		int spinnerPosition = 0;
		while( spinnerPosition < collectionsArray.length &&
				collectionsArray[spinnerPosition].getCollectionId() != currentCollection.getCollectionId())
			spinnerPosition++;

		spinnerCollection.setAdapter(collectionAdapter);
		if ( spinnerPosition < collectionsArray.length )
			//set the default according to value
			spinnerCollection.setSelection(spinnerPosition);

		try {
			// Attempt to set text colour that works with (hopefully) background colour. 
			for( View v : StaticHelpers.getViewsInside(spinnerCollection, TextView.class) ) {
				((TextView) v).setTextColor(AcalTheme.pickForegroundForBackground(colour));
				((TextView) v).setMaxLines(1);
			}
		}
		catch( Exception e ) {
			// Oh well.  Some other way then... @journal.
			Log.i(TAG,"Think of another solution...",e);
		}

		btnSaveChanges.setText((isModifyAction() ? getString(R.string.Apply) : getString(R.string.Add)));
		
		btnStartDate.setText( AcalDateTimeFormatter.fmtFull( start, prefer24hourFormat) );
		btnDueDate.setText( AcalDateTimeFormatter.fmtFull( due, prefer24hourFormat) );
		btnCompleteDate.setText( AcalDateTimeFormatter.fmtFull( completed, prefer24hourFormat) );

		if ( start != null && due != null && due.before(start) ) {
			AcalTheme.setContainerColour(btnStartDate,0xffff3030);
		}
		else {
			AcalTheme.setContainerFromTheme(btnStartDate, AcalTheme.BUTTON);
		}
		
		if ( start == null && due == null ) {
			alarmList = todo.getAlarms();
			this.alarmsList.removeAllViews();
			alarmsLayout.setVisibility(View.GONE);
		}
		else {
			//Display Alarms
			alarmList = todo.getAlarms();
			this.alarmsList.removeAllViews();
			for (AcalAlarm alarm : alarmList) {
				this.alarmsList.addView(this.getAlarmItem(alarm, alarmsList));
			}
			alarmsLayout.setVisibility(View.VISIBLE);
		}
		
		//set repeat options
		if ( start == null && due == null ) {
			repeatsLayout.setVisibility(View.GONE);
		}
		else {
			AcalDateTime relativeTo = (start == null ? due : start);
			int dow = relativeTo.getWeekDay();;
			int weekNum = relativeTo.getMonthWeek();

			String dowStr = "";
			String dowLongString = "";
			String everyDowString = "";
			switch (dow) {
				case 0:
					dowStr="MO";
					dowLongString = getString(R.string.Monday);
					everyDowString = getString(R.string.EveryMonday);
					break;
				case 1:
					dowStr="TU";
					dowLongString = getString(R.string.Tuesday);
					everyDowString = getString(R.string.EveryTuesday);
					break;
				case 2:
					dowStr="WE";
					dowLongString = getString(R.string.Wednesday);
					everyDowString = getString(R.string.EveryWednesday);
					break;
				case 3:
					dowStr="TH";
					dowLongString = getString(R.string.Thursday);
					everyDowString = getString(R.string.EveryThursday);
					break;
				case 4:
					dowStr="FR";
					dowLongString = getString(R.string.Friday); 
					everyDowString = getString(R.string.EveryFriday);
					break;
				case 5:
					dowStr="SA";
					dowLongString = getString(R.string.Saturday); 
					everyDowString = getString(R.string.EverySaturday);
					break;
				case 6:
					dowStr="SU";
					dowLongString = getString(R.string.Sunday); 	
					everyDowString = getString(R.string.EverySunday);
					break;
			}
			String dailyRepeatName = getString(R.string.EveryWeekday);
			String dailyRepeatRule = "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;COUNT=260";
			if (relativeTo.get(AcalDateTime.DAY_OF_WEEK) == AcalDateTime.SATURDAY || relativeTo.get(AcalDateTime.DAY_OF_WEEK) == AcalDateTime.SUNDAY) {
				dailyRepeatName = getString(R.string.EveryWeekend);
				dailyRepeatRule = "FREQ=WEEKLY;BYDAY=SA,SU;COUNT=104";
			}
	
			this.repeatRules = new String[] {
						getString(R.string.OnlyOnce),
						getString(R.string.EveryDay),
						dailyRepeatName,
						everyDowString,
						String.format(this.getString(R.string.EveryNthOfTheMonth),
								relativeTo.getMonthDay()+AcalDateTime.getSuffix(relativeTo.getMonthDay())),
						String.format(this.getString(R.string.EveryMonthOnTheNthSomeday),
									weekNum+AcalDateTime.getSuffix(weekNum)+" "+dowLongString),
						getString(R.string.EveryYear)
			};
			this.repeatRulesValues = new String[] {
					"FREQ=DAILY;COUNT=400",
					dailyRepeatRule,
					"FREQ=WEEKLY;BYDAY="+dowStr,
					"FREQ=MONTHLY;COUNT=60",
					"FREQ=MONTHLY;COUNT=60;BYDAY="+weekNum+dowStr,
					"FREQ=YEARLY"
			};
			String repeatRuleString = todo.getRRule();
			if (repeatRuleString == null) repeatRuleString = "";
			AcalRepeatRule RRule;
			try {
				RRule = new AcalRepeatRule(relativeTo, repeatRuleString); 
			}
			catch( IllegalArgumentException  e ) {
				Log.i(TAG,"Illegal repeat rule: '"+repeatRuleString+"'");
				RRule = new AcalRepeatRule(relativeTo, null ); 
			}
			String rr = RRule.repeatRule.toPrettyString(this);
			if (rr == null || rr.equals("")) rr = getString(R.string.OnlyOnce);
			btnAddRepeat.setText(rr);
			repeatsLayout.setVisibility(View.VISIBLE);
		}
	}


	private void setSelectedCollection(long collectionId) {

		if ( Collection.getInstance(collectionId,this) != null && (currentCollection == null || collectionId != currentCollection.collectionId) ) {
			currentCollection = Collection.getInstance(collectionId,this);
			this.updateLayout();
		}
	}

	
	private void setButtonDialog(Button myButton, final int dialogIndicator) {
		myButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDialog(dialogIndicator);
			}
		});
		AcalTheme.setContainerFromTheme(myButton, AcalTheme.BUTTON);
	}


	public void applyChanges() {
		//check if text fields changed
		//summary
		String oldSum = todo.getSummary();
		String newSum = this.todoName.getText().toString() ;
		String oldLoc = todo.getLocation();
		String newLoc = this.locationView.getText().toString();
		String oldDesc = todo.getDescription();
		String newDesc = this.notesView.getText().toString() ;
		
		if (!oldSum.equals(newSum)) todo.setSummary(newSum);
		if (!oldLoc.equals(newLoc)) todo.setLocation(newLoc);
		if (!oldDesc.equals(newDesc)) todo.setDescription(newDesc);

		todo.setPercentComplete(percentComplete);
		
		if (action == ACTION_CREATE || action == ACTION_MODIFY_ALL ) {
			if ( !this.saveChanges() ){
				Toast.makeText(this, "Save failed: retrying!", Toast.LENGTH_LONG).show();
				this.saveChanges();
			}
		}
		finish();
	}

	@SuppressWarnings("unchecked")
	private boolean saveChanges() {
		
		try {
			VCalendar vc = (VCalendar) todo.getTopParent();
			vc.updateTimeZones();

			AcalDateTime dtStart = todo.getStart();
			if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
					"saveChanges: "+todo.getSummary()+
					", starts "+(dtStart == null ? "not set" : dtStart.toPropertyString(PropertyName.DTSTART)));

			int sendAction = RRResourceEditedRequest.ACTION_UPDATE;
			if (action == ACTION_CREATE ) sendAction = RRResourceEditedRequest.ACTION_CREATE;
			else if (action == ACTION_DELETE_ALL ) sendAction = RRResourceEditedRequest.ACTION_DELETE;
			ResourceManager.getInstance(this)
						.sendRequest(new RRResourceEditedRequest(this, currentCollection.collectionId, rid, vc, sendAction));

			handlerContext = this;

			//set message for 10 seconds to fail.
			mHandler.sendEmptyMessageDelayed(SAVE_FAILED, 100000);

			//showDialog(SAVING_DIALOG);
			mHandler.sendEmptyMessageDelayed(SHOW_SAVING,50);


		}
		catch (Exception e) {
			if ( e.getMessage() != null ) Log.println(Constants.LOGD,TAG,e.getMessage());
			if (Constants.LOG_DEBUG)Log.println(Constants.LOGD,TAG,Log.getStackTraceString(e));
			Toast.makeText(this, getString(R.string.ErrorSavingEvent), Toast.LENGTH_LONG).show();
			return false;
		}

		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		this.updateLayout();
	}

	//Dialogs
	protected Dialog onCreateDialog(int id) {
		switch ( id ) {
			case LOADING_DIALOG:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Loading...");
				builder.setCancelable(false);
				loadingDialog = builder.create();
				return loadingDialog;
		}
		if ( todo == null ) return null;

		// Any dialogs after this point depend on todo having been initialised
		AcalDateTime start = todo.getStart();
		AcalDateTime due = todo.getDue();
		AcalDateTime completed = todo.getCompleted();
		
		Boolean dateTypeIsDate = null;
		if ( start == null ) {
			start = new AcalDateTime().applyLocalTimeZone().addDays(1);
			int newSecond = ((start.getDaySecond() / 3600) + 2) * 3600;
			if ( newSecond > 86399 ) start.addDays(1);
			start.setDaySecond(newSecond % 86400);
		}
		else {
			dateTypeIsDate = start.isDate();
		}
		if ( due == null ) {
			due = new AcalDateTime().applyLocalTimeZone().addDays(1);
			int newSecond = start.getDaySecond() + 3600;
			if ( newSecond > 86399 ) due.addDays(1);
			due.setDaySecond(newSecond % 86400);
		}
		else if ( dateTypeIsDate == null ) {
			dateTypeIsDate = due.isDate();
		}
		if ( completed == null ) {
			completed = new AcalDateTime();
			if ( start != null || due != null ) completed.setAsDate((start!=null?start.isDate():due.isDate()));
		}
		else if ( dateTypeIsDate == null ) {
			dateTypeIsDate = completed.isDate();
		}
		if ( dateTypeIsDate == null ) dateTypeIsDate = true;
		start.setAsDate(dateTypeIsDate);
		due.setAsDate(dateTypeIsDate);
		completed.setAsDate(dateTypeIsDate);

		switch ( id ) {
			case FROM_DIALOG:
				return new DateTimeDialog( this, start, prefer24hourFormat, true, true,
						new DateTimeSetListener() {
							public void onDateTimeSet(AcalDateTime newDateTime) {
								todo.setStart( newDateTime );
								updateLayout();
							}
						});

			case DUE_DIALOG:
				return new DateTimeDialog( this, due, prefer24hourFormat, true, true,
						new DateTimeSetListener() {
							public void onDateTimeSet(AcalDateTime newDateTime) {
								todo.setDue( newDateTime );
								updateLayout();
							}
						});

			case COMPLETED_DIALOG:
				return new DateTimeDialog( this, completed, prefer24hourFormat, true, true,
						new DateTimeSetListener() {
							public void onDateTimeSet(AcalDateTime newDateTime) {
								todo.setCompleted( newDateTime );
								todo.setPercentComplete(100);
								todo.setStatus(VTodo.Status.COMPLETED);
								updateLayout();
							}
						});


			case ADD_ALARM_DIALOG:
				AlertDialog.Builder builder = new AlertDialog.Builder( this );
				builder.setTitle( getString( R.string.ChooseAlarmTime ) );
				builder.setItems( alarmRelativeTimeStrings, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						// translate item to equal alarmValue index
						RelateWith relateWith = RelateWith.START;
						AcalDateTime start = todo.getStart();
						if ( start == null ) {
							relateWith = (todo.getDue() == null ? RelateWith.ABSOLUTE : RelateWith.END);
							if ( relateWith == RelateWith.ABSOLUTE ) {
								start = new AcalDateTime();
								start.addDays( 1 );
							}
						}
						if ( item < 0 || item > alarmValues.length ) return;
						if ( item == alarmValues.length ) {
							customAlarmDialog();
						}
						else {
							alarmList.add( new AcalAlarm( relateWith, todo.getDescription(), alarmValues[item],
									ActionType.AUDIO, start, todo.getDue() ) );
							todo.updateAlarmComponents( alarmList );
							updateLayout();
						}
					}
				} );
				return builder.create();
			case INSTANCES_TO_CHANGE_DIALOG:
				builder = new AlertDialog.Builder( this );
				builder.setTitle( getString( R.string.ChooseInstancesToChange ) );
				builder.setItems( todoChangeRanges, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switch ( item ) {
							case 0:
								action = ACTION_MODIFY_SINGLE;
								saveChanges();
								return;
							case 1:
								action = ACTION_MODIFY_ALL;
								saveChanges();
								return;
							case 2:
								action = ACTION_MODIFY_ALL_FUTURE;
								saveChanges();
								return;
						}
					}
				} );
				return builder.create();
			case SET_REPEAT_RULE_DIALOG:
				builder = new AlertDialog.Builder( this );
				builder.setTitle( getString( R.string.ChooseRepeatFrequency ) );
				builder.setItems( this.repeatRules, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						String newRule = "";
						if ( item != 0 ) {
							item--;
							newRule = repeatRulesValues[item];
						}
						if ( isModifyAction() ) {
							if ( TodoEdit.this.originalHasOccurrence
									&& !newRule.equals( TodoEdit.this.originalOccurence ) ) {
								action = ACTION_MODIFY_ALL;
							}
							else if ( TodoEdit.this.originalHasOccurrence ) {
								action = ACTION_MODIFY_SINGLE;
							}
						}
						todo.setRepetition( newRule );
						updateLayout();

					}
				} );
				return builder.create();
			default:
				return null;
		}
	}

	protected void customAlarmDialog() {

		AlarmDialog.AlarmSetListener customAlarmListener = new AlarmDialog.AlarmSetListener() {

			@Override
			public void onAlarmSet(AcalAlarm alarmValue) {
				alarmList.add( alarmValue );
		    	todo.updateAlarmComponents(alarmList);
		    	updateLayout();
			}
			
		};

		AlarmDialog customAlarm = new AlarmDialog(this, customAlarmListener, null,
				todo.getStart(), todo.getDue(), VComponent.VTODO);
		customAlarm.show();
	}
	
	public View getAlarmItem(final AcalAlarm alarm, ViewGroup parent) {
		LinearLayout rowLayout;

		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TextView title = null; //, time = null, location = null;
		rowLayout = (TableRow) inflater.inflate(R.layout.alarm_list_item, parent, false);

		title = (TextView) rowLayout.findViewById(R.id.AlarmListItemTitle);
		title.setText(alarm.toPrettyString());
		
		ImageView cancel = (ImageView) rowLayout.findViewById(R.id.delete_button);

		rowLayout.setTag(alarm);
		cancel.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				alarmList.remove(alarm);
				updateLayout();
			}
		});
		return rowLayout;
	}
	

	public boolean isModifyAction() {
		return (action == ACTION_EDIT || (action > ACTION_CREATE && action <= ACTION_MODIFY_ALL));
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if ( fromUser ) {
			percentComplete = progress;
			percentCompleteText.setText(Integer.toString(percentComplete)+"%");
			if ( progress == 0 ) 							todo.setStatus(VTodo.Status.NEEDS_ACTION);
			else if ( progress > 0 && progress < 100 ) 		todo.setStatus(VTodo.Status.IN_PROCESS);
			else {
				todo.setStatus(VTodo.Status.COMPLETED);
				todo.setCompleted(new AcalDateTime() );
				updateLayout();
			}
		}
	}


	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}


	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}


	@Override
	public void resourceChanged(ResourceChangedEvent event) {
		// @todo Auto-generated method stub
		
	}


	@Override
	public void resourceResponse(ResourceResponse response) {
		int msg = FAIL;
		Object result = response.result();
		if (result == null) {
			mHandler.sendMessage(mHandler.obtainMessage(msg));
		}
		else if (result instanceof CalendarInstance) {
			if (response.wasSuccessful()) {
				setTodo( new VTodo((TodoInstance) response.result()) );
				msg = REFRESH;
			}
			mHandler.sendMessage(mHandler.obtainMessage(msg));		
		}
		else if (result instanceof Long) {
			mHandler.sendMessage(mHandler.obtainMessage(SAVE_RESULT, result));
		}
	}

}
