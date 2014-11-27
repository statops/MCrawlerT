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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.alarmmanager.ALARM_STATE;
import com.morphoss.acal.database.alarmmanager.AlarmQueueManager;
import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.alarmmanager.requests.ARGetNextDueAlarm;
import com.morphoss.acal.database.alarmmanager.requests.ARUpdateAlarmState;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRAlarmRowToAcalAlarm;
import com.morphoss.acal.davacal.AcalAlarm;

/**
 * 
 * @author Morphoss.ltd
 * 
 * This Activity displays alarms as received from CalendarDataService.
 * 
 * TODO This class will need to be refactored, along with the alarm management code in CalendarDataService as
 * it has to many unsolvable flaws in current incarnation. Refactored version will probably require a short
 * lived Service of some kind to maintain all necessary state.
 *
 */
public class AlarmActivity extends AcalActivity implements OnClickListener  {

	public static final String TAG = "aCal AlarmActivity";

	//GUI Components
	private TextView header;
	private TextView title;
	private ImageView mapButton;
	private ImageView snoozeButton;
	private ImageView dismissButton;

	//Constants
	private static final int PAUSE_BEFORE_AUDIO = 1500;	// pause 1500 milliseconds after display before starting audio alarm
	private static final int SHORT_PAUSE_DURATION = 15;	// short pause 15 seconds if phone busy

	//Audio Management
	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;

	//Vibration settings
	private Vibrator vibrator;
	private final long[] pattern = { 0,1000, 2000,1000, 2000,1000, 2000,1000, 2000,1000, 2000,1000, 2000,1000, 2000,1000, 2000,1000, 2000,1000, 2000};

	//Phone State
	TelephonyManager telephonyManager;
	private boolean audioPlaying = false;
	private boolean audioInterupted = false;
	private boolean vibrateMode = false;
	private boolean inCall = false;
	private boolean shortPausing = false;

	//Other vars
	private static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	private SharedPreferences prefs;
	private AcalAlarm currentAlarm;
	private AlarmRow currentAlarmRow;
	private PowerManager.WakeLock wl;

	/** These values are not defined until Android 2.0 or later, so we have
	 * to define them ourselves.  They won't work unless you're on a 2.x or
	 * later device either, of course... */
	//private static final int WINDOW_FLAG_DISMISS_KEYGUARD = 0x00400000;
	private static final int WINDOW_FLAG_SHOW_WHEN_LOCKED = 0x00080000;
	private static final int WINDOW_FLAG_TURN_SCREEN_ON   = 0x00200000;


	/********************
	 * 		Overides	*
	 ********************/

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Acquire required System Services
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		audioManager = (AudioManager)this.getSystemService(AUDIO_SERVICE);
		vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		telephonyManager = (TelephonyManager)this.getSystemService(TELEPHONY_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		//Before continuing - check to see if we are already in a call
		switch (telephonyManager.getCallState()) {
			case TelephonyManager.CALL_STATE_OFFHOOK:
			case TelephonyManager.CALL_STATE_RINGING:
				shortPause();
				return;
		}
		
		//Configure Power
		wl = pm.newWakeLock(
				PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP
				| PowerManager.ON_AFTER_RELEASE
				, "aCal Alarm"
		);

		wl.acquire();	


		getWindow().addFlags( WINDOW_FLAG_SHOW_WHEN_LOCKED
				//					| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				//					| WINDOW_FLAG_DISMISS_KEYGUARD
				| WINDOW_FLAG_TURN_SCREEN_ON
		);

		this.setContentView(R.layout.alarm_activity);

		
		//Configure AudioManager
		String volumeType = prefs.getString(getString(R.string.AlarmVolumeType_PrefKey), "null" );
		if (volumeType.equals("null") || volumeType.equals("ALARM")) {
			this.setVolumeControlStream(AudioManager.STREAM_ALARM);
		}
		else {
			this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}

		//Configure Telephony Manager
		 telephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

		
		//prepare gui elements
		header = (TextView) this.findViewById(R.id.AlarmTitle);
		title = (TextView) this.findViewById(R.id.AlarmContentTitleTextView);
		mapButton = (ImageView) this.findViewById(R.id.map_button);
		snoozeButton = (ImageView) this.findViewById(R.id.snooze_button);
		dismissButton = (ImageView) this.findViewById(R.id.dismiss_button);

		
	}


	@Override
	public void onClick(View clickedThing) {
		if ( clickedThing == mapButton ) {
			//if ( Constants.LOG_DEBUG ) Log.d(TAG, "Starting Map");
			//String loc = location.getText().toString();
			// replace whitespaces with '+'
			//loc.replace("\\s", "+");
			//Uri target = Uri.parse("geo:0,0?q=" + loc);
			//startActivity(new Intent(android.content.Intent.ACTION_VIEW, target));
			// start map view
			return;
		}
		if ( clickedThing == snoozeButton ) {
			if ( Constants.LOG_DEBUG ) Log.d(TAG, "Snoozing Alarm");
				AlarmQueueManager.getInstance(this).sendBlockingRequest(new ARUpdateAlarmState(currentAlarmRow, ALARM_STATE.SNOOZED));
		}
		if ( clickedThing == dismissButton ) {
			if ( Constants.LOG_DEBUG ) Log.d(TAG, "Dismissing alarm.");
				AlarmQueueManager.getInstance(this).sendBlockingRequest(new ARUpdateAlarmState(currentAlarmRow, ALARM_STATE.DISMISSED));
		}
		this.showNextAlarm();
	}

	//We have been closed for some reason. Give up on any remaining alarms and notify cds of last triggered.
	//If there are any alarms left in our list, they will be re-triggered by cds immediately.
	@Override
	public void onPause() {
		super.onPause();
		audioPlaying = false;
		audioInterupted = false;
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.stop();
			// mediaPlayer.release();
		}
		vibrator.cancel();

		if (shortPausing)
			shortPausing = false;
		if (wl.isHeld()) wl.release();
	}

	public void onResume() {
		super.onResume();
		this.showNextAlarm();
	}

	/********************************
	 * 		Alarm Management		*
	 ********************************/
	
	/**
	 * Trigger the next alarm that is due to go off			
	 */
	private void showNextAlarm() {
		if (Constants.LOG_DEBUG) Log.d(TAG, "Showing next alarm....");
		currentAlarmRow = AlarmQueueManager.getInstance(this).sendBlockingRequest(new ARGetNextDueAlarm()).result();
		
		if (currentAlarmRow == null) {
			if (Constants.LOG_DEBUG)Log.d(TAG,"Next alarm is null. Finishing");
			mNotificationManager.cancelAll();
			finish();
			return;
		} else {
			//need to construct an AcalAlarm from the alarm row
			this.currentAlarm = ResourceManager.getInstance(this).sendBlockingRequest(new RRAlarmRowToAcalAlarm(currentAlarmRow)).result();
			if (this.currentAlarm == null ) {
				//error getting alarm. 
				//TODO dismiss this row then show next
				mNotificationManager.cancelAll();
				finish();
				return;
			}
		}
		
		this.updateAlarmView();
	}

	/**
	 * Called when there is an actual alarm to trigger.
	 * This method primarily takes care of updating the GUI components.
	 * Once the GUI is set up playAlarm() is called which takes care of audio/vibration
	 */
	private void updateAlarmView() {
		try {
			AcalDateTime now = new AcalDateTime();
			int minute = now.getMinute();
			String min = (minute < 10 ? "0"+minute : minute+"");
			header.setText((now.getHour())+":"+min);
			title.setText(currentAlarm.description);
			createNotification(currentAlarm.description);
			//EventInstance event = currentAlarm.getEvent();
			//if (event == null)
			//	throw new IllegalStateException("Alarms passed to AlarmActivity MUST have an associated event");
			//location.setText(event.getLocation());

			AcalDateTime viewDate = new AcalDateTime();
			viewDate.applyLocalTimeZone();
			viewDate.setDaySecond(0);
			
			snoozeButton.setOnClickListener(this);
			dismissButton.setOnClickListener(this);
			mapButton.setOnClickListener(this);
			/**
			 * TODO - fix this
			 */
			//
			//time.setText(event.getTimeText(viewDate, AcalDateTime.addDays(viewDate,1),prefs.getBoolean(getString(R.string.prefTwelveTwentyfour), false)));

			playAlarm();
		}
		catch ( Exception e ) {
			Log.e(TAG,"!!!ERROR UPDATING ALARM VIEW!!! - "+e.getMessage(), e );
		}
	}

	/**
	 * Method responsible for setting up and starting audio/vibration. And recroding the state
	 * of the mediaplayer/vibration
	 */
	private void playAlarm() {
		//Check to see if we are already playing an alarm
		if (mediaPlayer != null && mediaPlayer.isPlaying()) return;

		//reset state
		audioPlaying = false;
		audioInterupted = false;
		vibrateMode = false;
		vibrator.cancel();

		//If Phone is silent, Vibrate
		if ( audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT 
				|| audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE ) {
			vibrateMode = true;
		}

		//Otherwise set up media player
		else {
			//Get audio stream type and sound file
			String uri = prefs.getString(getString(R.string.DefaultAlarmTone_PrefKey), "null" );
			mediaPlayer = new MediaPlayer();
			String volumeType = prefs.getString(getString(R.string.AlarmVolumeType_PrefKey), "null" );
			if (volumeType.equals("null") || volumeType.equals("ALARM"))
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			else
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

			if (uri.equals("null")) {
				//Use Default Audio
				//1.3 Fixed this line to be more flexible and accurate.
				uri = ContentResolver.SCHEME_ANDROID_RESOURCE+"://"+this.getBaseContext().getPackageName()+"/" + R.raw.assembly;
			}

			//Prepare Mediaplayer
			try {
				mediaPlayer.setDataSource(this, Uri.parse(uri));
				mediaPlayer.prepare();
			} catch (java.io.IOException ex) {
				// something went wrong, abort.
				mediaPlayer = null;
				if ( Constants.LOG_DEBUG )
					Log.e(TAG,"IO Problem with alarm.", ex);

			} catch (java.lang.IllegalArgumentException ex) {
				// something went wrong - probably invalid URI
				//TODO This could occur if sound file deleted after preference set -
				//	should probably be able to work around this
				mediaPlayer = null;
				Log.e(TAG,"Illegal Argument Problem with alarm.", ex);
			}

			//If MediaPlayer is null, then something is wrong with sound - set up vibrator instead
			if ( mediaPlayer == null ) vibrateMode = true;

			//Finally, trigger media player or vibrator if we are NOT in a call
			if (vibrateMode) {
				if (!inCall) vibrator.vibrate(pattern, -1);
			}
			else {
				for( int i=0; i<(PAUSE_BEFORE_AUDIO / 20); i++ ) {
					try { Thread.sleep(20); } catch ( InterruptedException e ) { }
				}
				if (!inCall) {
					this.audioPlaying = true;
					this.audioInterupted = false;
					mediaPlayer.start();			
				}
				else {
					this.audioPlaying = false;
					this.audioInterupted = true;
				}
			} 
		}
	}

	/** 
	 * Trigger a notification in the notification bar.
	 * @param text	The text to display in the notification bar.
	 */
	private void createNotification(String text) {

		int icon = R.drawable.icon;
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, text, when);
		CharSequence contentTitle = "aCal Alarm";
		CharSequence contentText = text;
		Intent notificationIntent = new Intent(this, AlarmActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}

	/**
	 * This method is called if the phone was busy when we started/resumed 
	 * We will pause the Activity, and try to restart it in SHORT_PAUSE_DURATION seconds;
	 */
	private void shortPause() {
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				startActivity(new Intent(getApplicationContext(), AlarmActivity.this.getClass()));
			}
			
		}, SHORT_PAUSE_DURATION*1000);
		this.shortPausing = true;
		this.finish();
	}

	/********************************************************
	 *						PHONE STATE						*
	 *	Update media and Vibration when phone state changes *
	 ********************************************************/

	public class MyPhoneStateListener extends PhoneStateListener {
	
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (Constants.LOG_DEBUG) {
				Log.d(TAG, "Phone State has changed");
			}
			switch (state) {
				case TelephonyManager.CALL_STATE_RINGING: ringing();
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK: inCall();	
					break;
				case TelephonyManager.CALL_STATE_IDLE:	idle();
					break;
			}
		}
		
		private void ringing() {
			inCall = true;
			if (audioPlaying && !audioInterupted) {
				if (Constants.LOG_DEBUG) {
					Log.d(TAG, "Phone is ringing, Interupting audio playback");
				}
				audioInterupted = true;
				mediaPlayer.pause();
			}
		}
		private void inCall() {
			if (Constants.LOG_DEBUG) {
				Log.d(TAG, "Phone is off hook");
			}
			ringing();
		}
		private void idle() {
			if (Constants.LOG_DEBUG) {
				Log.d(TAG, "Phone is idle.");
			}
			inCall = false;
			if (audioInterupted && mediaPlayer != null) {
				if (Constants.LOG_DEBUG) {
					Log.d(TAG, "Restarting interupted audio");
				}
				audioInterupted = false;
				audioPlaying = true;
				mediaPlayer.start();
			}
		}
	};
}
