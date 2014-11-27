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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDateTimeFormatter;
import com.morphoss.acal.database.DataChangeEvent;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedEvent;
import com.morphoss.acal.database.resourcesmanager.ResourceChangedListener;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.ResourceResponseListener;
import com.morphoss.acal.database.resourcesmanager.requests.RRRequestInstance;
import com.morphoss.acal.dataservice.CalendarInstance;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.dataservice.JournalInstance;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.service.aCalService;

public class JournalView extends AcalActivity
					implements OnGestureListener, OnTouchListener, OnClickListener,
					ResourceChangedListener, ResourceResponseListener<CalendarInstance> {

	public static final String TAG = "aCal JournalView";
	public static final int EDIT = 1;
	public static final int ADD = 2;
	public static final int SHOW_ON_MAP = 3;

	private final static boolean DEBUG = true && Constants.DEBUG_MODE;
	
	private CacheObject cacheJournal = null;
	private Collection collection = null;
	private JournalInstance journal = null;
	
	private ResourceManager	resourceManager = null;
	private Long	rid;
	private String	rrid;
	
	private static final int REFRESH = 0;
	private static final int FAIL = 1;
	public static final String KEY_CACHE_OBJECT = "cache_object";
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == REFRESH) {
				populateLayout();
			} else if(msg.what == FAIL) {
				Toast.makeText(JournalView.this, "The resource you are looking at has changed or been deleted.", Toast.LENGTH_LONG).show();
				finish();
			}
			
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Ensure service is actually running
		this.startService(new Intent(this, aCalService.class));
		//gestureDetector = new GestureDetector(this);

		this.resourceManager  = ResourceManager.getInstance(this);
		this.resourceManager.addListener(this);
		
		Bundle b = this.getIntent().getExtras();
		try {
			this.cacheJournal = (CacheObject) b.getParcelable(KEY_CACHE_OBJECT);
			rid = cacheJournal.getResourceId();
			rrid = cacheJournal.getRecurrenceId();
			resourceManager.sendRequest(new RRRequestInstance(this,rid, rrid));
		}
		catch (Exception e) {
			if (Constants.LOG_DEBUG) {
				Log.d(TAG, "Error getting data from caller: "+e.getMessage());
				Log.d(TAG, Log.getStackTraceString(e));
			}
		}
		this.setContentView(R.layout.journal_view);

		//Set up buttons
		this.setupButton(R.id.journal_edit_button, EDIT);
		
		this.collection = Collection.getInstance(cacheJournal.getCollectionId(), this);
		this.populateLayout();
	}

	
	private void populateLayout() {
		String title = cacheJournal.getSummary();
		String description = null;
		AcalDateTime dtStart = cacheJournal.getStartDateTime();
		if ( DEBUG ) Log.println(Constants.LOGD,TAG,
				"Populating view layout for '"+title+"' with journal "+(journal==null?"un":"")+"set.");

		if ( journal != null ) {
			description = journal.getDescription();
			title = journal.getSummary();
			dtStart = journal.getStart();
		}
		
		int colour = collection.getColour();
		LinearLayout sidebar = (LinearLayout)this.findViewById(R.id.JournalViewColourBar);
		sidebar.setBackgroundColor(colour);
		
		TextView name = (TextView) this.findViewById(R.id.JournalName);
		name.setText(title);
		name.setTextColor(colour);
		
		TextView time = (TextView) this.findViewById(R.id.JournalTimeContent);
		time.setTextColor(colour);
		
		
		time.setText(AcalDateTimeFormatter.getJournalTimeText(this, dtStart, prefs.getBoolean(getString(R.string.prefTwelveTwentyfour),true)));
		
		TextView notesView = (TextView) this.findViewById(R.id.JournalNotesContent);
		notesView.setText(description);
	}
	
	private void setupButton(int id, int val) {
		Button myButton = (Button) this.findViewById(id);
		myButton.setOnClickListener(this);
		myButton.setTag(val);
		AcalTheme.setContainerFromTheme(myButton, AcalTheme.BUTTON);
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onClick(View arg0) {
		int button = (int)((Integer)arg0.getTag());
		switch (button) {
			case EDIT: {
				//start journal activity
				Bundle bundle = new Bundle();
				bundle.putParcelable(JournalEdit.KEY_CACHE_OBJECT, cacheJournal);
				Intent journalEditIntent = new Intent(this, JournalEdit.class);
				journalEditIntent.putExtras(bundle);
				if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, 
						"Starting activity for result request="+JournalEdit.ACTION_EDIT);
				this.startActivityForResult(journalEditIntent,JournalEdit.ACTION_EDIT);
				break;
			}
			case ADD: {
				Bundle bundle = new Bundle();
				Intent journalEditIntent = new Intent(this, JournalEdit.class);
				journalEditIntent.putExtras(bundle);
				this.startActivityForResult(journalEditIntent,JournalEdit.ACTION_CREATE);
				break;
			}
		}
		
	}

	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, 
				"onActivityResult request="+requestCode+", result="+resultCode);
    	if (requestCode == JournalEdit.ACTION_EDIT && resultCode == RESULT_OK) {
			try {
				Bundle b = data.getExtras();
				CacheObject tmpCache = (CacheObject) b.getParcelable(JournalEdit.KEY_CACHE_OBJECT);
				if ( tmpCache != null ) cacheJournal = tmpCache;
				String blob = b.getString(JournalEdit.KEY_VCALENDAR_BLOB);
				Resource resource = b.getParcelable(JournalEdit.KEY_OPERATION);
				if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Received blob is:\n"+blob);
				this.journal = (JournalInstance) JournalInstance.fromResourceAndRRId(resource, rrid);
			}
			catch (Exception e) {
				if (Constants.LOG_DEBUG)Log.d(TAG, "Error getting data from caller: "+e.getMessage());
			}
			populateLayout();
    	}
    	else if (requestCode == JournalEdit.ACTION_CREATE && resultCode == RESULT_OK) {
			Intent res = new Intent();
			this.setResult(RESULT_OK, res);
			this.finish();
    	}
  }


	@Override
	public void resourceChanged(ResourceChangedEvent event) {
		for (DataChangeEvent dce: event.getChanges()) {
			if (dce.getData().getAsLong(ResourceTableManager.RESOURCE_ID) == rid) {
				//our data has changed!
				resourceManager.sendRequest(new RRRequestInstance(this,rid, this.rrid));
			}
		}
	}


	@Override
	public void resourceResponse(ResourceResponse<CalendarInstance> response) {
		if (!response.wasSuccessful()) {
			if (DEBUG) Log.println(Constants.LOGD,TAG, "Unsuccessful Resource Response Received.");
			mHandler.sendMessage(mHandler.obtainMessage(FAIL));
			return;
		}
		if (DEBUG) Log.println(Constants.LOGD,TAG, "Successful Resource Response Received.");
		CalendarInstance res = response.result();
		if (res instanceof JournalInstance) {
			this.journal = (JournalInstance) res;
			this.rid = this.journal.getResourceId();
			this.rrid = this.journal.getRecurrenceId();
			mHandler.sendMessage(mHandler.obtainMessage(REFRESH));
		}
		else {
			Log.e(TAG, "Resource Response was not a JournalInstance!");
		}
	}
	
}
