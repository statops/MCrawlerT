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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.R;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDateTimeFormatter;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedEvent;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedListener;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requests.RRRequestInstance;
import com.morphoss.acal.dataservice.CalendarInstance;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.dataservice.JournalInstance;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VJournal;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.service.aCalService;
import com.morphoss.acal.widget.DateTimeDialog;
import com.morphoss.acal.widget.DateTimeSetListener;

@SuppressWarnings("rawtypes")
public class JournalEdit extends AcalActivity
	implements OnCheckedChangeListener,
				ResourceChangedListener, ResourceResponseListener, OnFocusChangeListener {

	public static final String TAG = "aCal JournalEdit";

	private VJournal journal;
	public static final int ACTION_NONE = -1;
	public static final int ACTION_CREATE = 0;
	public static final int ACTION_MODIFY_ALL = 2;
	public static final int ACTION_DELETE_ALL = 5;
	public static final int ACTION_EDIT = 8;
	public static final int ACTION_COPY = 9;

	private int action = ACTION_NONE;
	private static final int FROM_DIALOG = 10;
	private static final int LOADING_DIALOG = 0xfeed;
	private static final int SAVING_DIALOG = 9;

	boolean prefer24hourFormat = false;


	public static final String	KEY_CACHE_OBJECT	= "CacheObject";
	public static final String	KEY_OPERATION		= "Operation";
	public static final String	KEY_RESOURCE		= "Resource";
	public static final String	KEY_VCALENDAR_BLOB	= "VCalendar";
	
	//GUI Components
	private Button btnStartDate;
	private LinearLayout sidebar;
	private TextView journalName;
	private LinearLayout collectionsLayout;
	private Spinner spinnerCollection;
	private Button btnSaveChanges;	
	private Button btnCancelChanges;

	
	//Active collections for create mode
	private Collection currentCollection;	//currently selected collection
	private CollectionForArrayAdapter[] collectionsArray;

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
	private Dialog savingDialog = null;

	private ResourceManager	resourceManager;

	private TextView journalContent;

	private long	rid = -1;
	
	private boolean saveSucceeded = false;
	private boolean isSaving = false;
	private boolean isLoading = false;
	private static JournalEdit handlerContext = null;
	
	private static Handler mHandler = new Handler() {
		public void handleMessage(Message m) {
			if ( handlerContext != null ) handlerContext.handleMessage(m);
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.journal_edit);

		//Ensure service is actually running
		startService(new Intent(this, aCalService.class));

		ContentValues[] journalCollections = DavCollections.getCollections( getContentResolver(), DavCollections.INCLUDE_JOURNAL );
		if ( journalCollections.length == 0 ) {
			Toast.makeText(this, getString(R.string.errorMustHaveActiveCalendar), Toast.LENGTH_LONG).show();
			this.finish();	// can't work if no active collections
			return;
		}

		this.collectionsArray = new CollectionForArrayAdapter[journalCollections.length];
		int count = 0;
		long collectionId;
		for (ContentValues cv : journalCollections ) {
			collectionId = cv.getAsLong(DavCollections._ID);
			collectionsArray[count++] = new CollectionForArrayAdapter(this,collectionId);
		}
		
		resourceManager = ResourceManager.getInstance(this,this);
		requestJournalResource();

		// Get time display preference
		prefer24hourFormat = prefs.getBoolean(getString(R.string.prefTwelveTwentyfour), false);

		this.populateLayout();
	}

	@Override
	public void onDestroy() {
		if ( handlerContext == this ) handlerContext = null;
		super.onDestroy();
	}

	
	private void handleMessage(Message msg) {
		
		switch ( msg.what ) {
			case REFRESH:
				if ( loadingDialog != null ) {
					loadingDialog.dismiss();
					loadingDialog = null;
				}
				updateLayout();
				break;
			case CONFLICT:
				Toast.makeText(JournalEdit.this,"The resource you are editing has been changed or deleted on the server.", Toast.LENGTH_LONG).show();

			case SHOW_LOADING:
				if ( journal == null ) showDialog(LOADING_DIALOG);
				break;
			case FAIL:
				if ( isLoading ) {
					Toast.makeText(JournalEdit.this,"Error loading data.", Toast.LENGTH_LONG).show();
					isLoading = false;
				}
				else if ( isSaving ) {
					isSaving = false;
					if ( savingDialog != null ) savingDialog
							.dismiss();
					Toast.makeText(JournalEdit.this, "Something went wrong trying to save data.", Toast.LENGTH_LONG).show();
					setResult(Activity.RESULT_CANCELED, null);
				}
				finish();
				break;
			case GIVE_UP:
				if ( loadingDialog != null ) {
					loadingDialog.dismiss();
					Toast.makeText(JournalEdit.this, "Error loading event data.", Toast.LENGTH_LONG).show();
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
				handlerContext = this;
				mHandler.removeMessages(SAVE_FAILED);
				isSaving = false;
				if ( savingDialog != null ) savingDialog.dismiss();
				long res = (Long) msg.obj;
				if ( res >= 0 ) {
					Intent ret = new Intent();
					Bundle b = new Bundle();
//					b.putParcelable(JournalView.KEY_CACHE_OBJECT, (Long) msg.obj);
//					b.putString( JournalView.RECURRENCE_ID_KEY, journal.getStart().toPropertyString(
//											PropertyName.RECURRENCE_ID));
					ret.putExtras(b);
					setResult(RESULT_OK, ret);
					saveSucceeded = true;

					finish();

				}
				else {
					Toast.makeText(JournalEdit.this, "Error saving event data.", Toast.LENGTH_LONG).show();
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
					Toast.makeText( JournalEdit.this, "Something went wrong trying to save data.", Toast.LENGTH_LONG).show();
					setResult(Activity.RESULT_CANCELED, null);
					finish();
				}
				break;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void requestJournalResource() {
		currentOperation = ACTION_EDIT;
		try {
			Bundle b = this.getIntent().getExtras();
			if ( b != null && b.containsKey(KEY_OPERATION) ) {
				currentOperation = b.getInt(KEY_OPERATION);
			}
			if ( b != null && b.containsKey(KEY_CACHE_OBJECT) ) {
				CacheObject cacheJournal = (CacheObject) b.getParcelable(KEY_CACHE_OBJECT);
				this.rid = cacheJournal.getResourceId();
				handlerContext = this;
				resourceManager.sendRequest(new RRRequestInstance(this, this.rid, cacheJournal.getRecurrenceId()));
				mHandler.sendMessageDelayed(mHandler.obtainMessage(SHOW_LOADING), 50);
				mHandler.sendMessageDelayed(mHandler.obtainMessage(GIVE_UP), 10000);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "No bundle from caller.", e);
		}

		if ( this.journal == null && currentOperation == ACTION_CREATE ) {
			long preferredCollectionId = -1;
			try {
				preferredCollectionId = Long.parseLong(prefs.getString(PrefNames.defaultNotesCollection, "-1"));
			}
			catch( Exception e ) {}

			if ( preferredCollectionId == -1 || Collection.getInstance(preferredCollectionId, this) == null )
				preferredCollectionId = collectionsArray[0].getCollectionId();

			currentCollection = Collection.getInstance(preferredCollectionId, this);

			this.action = ACTION_CREATE;
			setJournal(new VJournal());
			this.journal.setStart(new AcalDateTime());
		}
	}

	
	private void setJournal( VJournal newJournal ) {
		this.journal = newJournal;
		long collectionId = -1;		
		if ( currentOperation == ACTION_EDIT ) {
			this.action = ACTION_MODIFY_ALL;
		}
		else if ( currentOperation == ACTION_COPY ) {
			this.action = ACTION_CREATE;
		}

		if ( Collection.getInstance(collectionId,this) != null )
			currentCollection = Collection.getInstance(collectionId,this);

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
		sidebar = (LinearLayout)this.findViewById(R.id.JournalEditColourBar);

		//Title
		this.journalName = (TextView) this.findViewById(R.id.JournalName);
		journalName.setSelectAllOnFocus(action == ACTION_CREATE);

		//Content
		this.journalContent = (TextView) this.findViewById(R.id.JournalNotesContent);
		journalContent.setSelectAllOnFocus(action == ACTION_CREATE);
		
		//Collection
		collectionsLayout = (LinearLayout)this.findViewById(R.id.JournalCollectionLayout);
		spinnerCollection = (Spinner) this.findViewById(R.id.JournalEditCollectionSelect);
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
		btnStartDate = (Button) this.findViewById(R.id.JournalDateTime);

		btnSaveChanges = (Button) this.findViewById(R.id.journal_apply_button);
		btnCancelChanges = (Button) this.findViewById(R.id.journal_cancel_button);
		
	
		// Button listeners
		setButtonDialog(btnStartDate, FROM_DIALOG);

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
		
		if ( journal != null ) updateLayout();
	}

	
	/**
	 * Update the screen whenever something has changed.
	 */
	private void updateLayout() {
		AcalDateTime start = journal.getStart();

		String title = journal.getSummary();
		journalName.setText(title);

		String description = journal.getDescription();
		journalContent.setText(description);
		
		Integer colour = currentCollection.getColour();
		if ( colour == null ) colour = 0x70a0a0a0;
		sidebar.setBackgroundColor(colour);
		AcalTheme.setContainerColour(spinnerCollection,colour);

		journalName.setTextColor(colour);

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

	}
	
	public boolean isModifyAction() {
		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "isModify action = " + action);
		return (action == ACTION_MODIFY_ALL);
	}


	private void setSelectedCollection(long collectionId) {

		Collection newCollection = Collection.getInstance(collectionId,this); 
		if ( newCollection != null && newCollection != currentCollection ) {
			currentCollection = newCollection;
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
		String oldSum = journal.getSummary();
		String newSum = this.journalName.getText().toString() ;
		String oldDesc = journal.getDescription();
		String newDesc = this.journalContent.getText().toString() ;
		
		if (!oldSum.equals(newSum)) journal.setSummary(newSum);
		if (!oldDesc.equals(newDesc)) journal.setDescription(newDesc);

		
		if (action == ACTION_CREATE || action == ACTION_MODIFY_ALL ) {
			if ( !this.saveChanges() ){
				Toast.makeText(this, "Save failed: retrying!", Toast.LENGTH_LONG).show();
				this.saveChanges();
			}
		}
		
		this.finish();
	}

	@SuppressWarnings("unchecked")
	private boolean saveChanges() {
		
		try {
			VCalendar vc = (VCalendar) journal.getTopParent();

			AcalDateTime dtStart = journal.getStart();
			if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,
					"saveChanges: "+journal.getSummary()+
					", starts "+(dtStart == null ? "not set" : dtStart.toPropertyString(PropertyName.DTSTART)));

			int sendAction = RRResourceEditedRequest.ACTION_UPDATE;
			if (action == ACTION_CREATE ) sendAction = RRResourceEditedRequest.ACTION_CREATE;
			else if (action == ACTION_DELETE_ALL ) sendAction = RRResourceEditedRequest.ACTION_DELETE;
			ResourceManager.getInstance(this)
						.sendRequest(new RRResourceEditedRequest(this, currentCollection.collectionId, rid, vc, sendAction));

		}
		catch (Exception e) {
			if ( e.getMessage() != null ) Log.println(Constants.LOGD,TAG,e.getMessage());
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG,Log.getStackTraceString(e));
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
		AlertDialog.Builder builder;
		switch ( id ) {
			case SAVING_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.Saving));
				builder.setCancelable(false);
				savingDialog = builder.create();
				return savingDialog;
			case LOADING_DIALOG:
				builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.Loading));
				builder.setCancelable(false);
				loadingDialog = builder.create();
				return loadingDialog;
		}
		if ( journal == null ) return null;
		checkpointCurrentValues();

		// Any dialogs after this point depend on journal having been initialised
		AcalDateTime start = journal.getStart();
		
		if ( start == null ) {
			start = new AcalDateTime().applyLocalTimeZone().addDays(1);
			int newSecond = ((start.getDaySecond() / 3600) + 2) * 3600;
			if ( newSecond > 86399 ) start.addDays(1);
			start.setDaySecond(newSecond % 86400);
		}

		switch ( id ) {
			case FROM_DIALOG:
				return new DateTimeDialog( this, start, prefer24hourFormat, true, true,
						new DateTimeSetListener() {
							public void onDateTimeSet(AcalDateTime newDateTime) {
								journal.setStart( newDateTime );
								updateLayout();
							}
						});



			default:
				return null;
		}
	}

	@Override
	public void resourceChanged(ResourceChangedEvent event) {
		// @journal Auto-generated method stub
		
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
				setJournal( new VJournal((JournalInstance) response.result()) );
				msg = REFRESH;
			}
			mHandler.sendMessage(mHandler.obtainMessage(msg));		
		}
		else if (result instanceof Long) {
			mHandler.sendMessage(mHandler.obtainMessage(SAVE_RESULT, result));
		}
	}

	private void checkpointCurrentValues() {
		// Make sure the text fields are all preserved before we start any dialogs.
		journal.setSummary(journalName.getText().toString());
		journal.setDescription(journalContent.getText().toString());
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		Log.i(TAG,"Current view is a " + v.toString() + " " + (hasFocus?"has" : "not") + " focused");
		if ( !hasFocus ) checkpointCurrentValues();
	}
}
