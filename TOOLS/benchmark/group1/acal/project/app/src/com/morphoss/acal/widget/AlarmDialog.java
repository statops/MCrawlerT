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

package com.morphoss.acal.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDateTimeFormatter;
import com.morphoss.acal.acaltime.AcalDuration;
import com.morphoss.acal.davacal.AcalAlarm;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.AcalAlarm.ActionType;
import com.morphoss.acal.davacal.AcalAlarm.RelateWith;

/**
 * @author Morphoss Ltd
 */

public class AlarmDialog extends Dialog implements OnClickListener, OnSeekBarChangeListener {

	private static final int	DURATION_SEEK_RANGE	= 75;
	private Context context;
	private Button okButton;
	private Button cancelButton;
	private Button beforeButton;
	private Button relatedButton;
	private TextView alarmTimeText;
	private LinearLayout relativeDurationLayout;
	private TextView relativeDurationText;
	private SeekBar alarmAdjustDuration;
	private LinearLayout absoluteAlarmTimeLayout;
	private Button absoluteDateTimeButton;

	public interface AlarmSetListener {
		public void onAlarmSet(AcalAlarm alarmValue);
	}
	private AlarmSetListener dialogListener; 
	
	private AcalDuration relativeOffset;
	private boolean offsetBefore;
	private RelateWith relativeTo;
	private String alarmDescription;
	private AcalDateTime timeToFire;

	private final AcalDateTime originalStart;
	private final AcalDateTime originalEnd;
	private final String parentType;
	private final ActionType actionType;
	private boolean	prefer24hourFormat;


	/**
	 * Construct a new AlarmDialog for building custom alarms.
	 * @param context
	 * @param listener
	 * @param alarmValue An AcalAlarm which is being modified, possibly null.
	 * @param start The DTSTART date/time from the parentComponent, possibly null.
	 * @param end The DTEND / DUE from the parentComponent, possibly null.
	 * @param parentComponentType The type of the containing component, such as VEVENT, VTDODO, etc. 
	 */
	public AlarmDialog(Context context, AlarmSetListener listener, AcalAlarm alarmValue,
									AcalDateTime start, AcalDateTime end, String parentComponentType )  {
    	super(context);
    	this.context = context;
        this.dialogListener = listener;
        setContentView(R.layout.alarm_dialog);

        relativeOffset = (alarmValue != null ? alarmValue.relativeTime : new AcalDuration());
        offsetBefore = (relativeOffset.getTimeMillis() <= 0);
        relativeTo = (alarmValue != null ? alarmValue.relativeTo
        								 : (start != null ? RelateWith.START
        										 		  : (end != null ? RelateWith.END : RelateWith.ABSOLUTE )));
        actionType = (alarmValue != null ? alarmValue.actionType : ActionType.AUDIO);
        parentType = parentComponentType;
        originalStart = start;
        originalEnd = end;
        
        okButton = (Button)this.findViewById(R.id.AlarmOkButton);
        cancelButton = (Button)this.findViewById(R.id.AlarmCancelButton);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        beforeButton = (Button)this.findViewById(R.id.AlarmBeforeButton);
        beforeButton.setOnClickListener(this);
        AcalTheme.setContainerFromTheme(beforeButton, AcalTheme.BUTTON);
        
        relatedButton = (Button)this.findViewById(R.id.AlarmRelatedButton);
        relatedButton.setOnClickListener(this);
        AcalTheme.setContainerFromTheme(relatedButton, AcalTheme.BUTTON);

        absoluteDateTimeButton = (Button) findViewById(R.id.AbsoluteDateTime);
        AcalTheme.setContainerFromTheme(absoluteDateTimeButton, AcalTheme.BUTTON);
        absoluteDateTimeButton.setOnClickListener(this);

        alarmTimeText = (TextView) this.findViewById(R.id.alarmTimeText);
        relativeDurationText = (TextView) this.findViewById(R.id.alarmRelativeDuration);
        alarmAdjustDuration = (SeekBar) this.findViewById(R.id.alarmAdjustDuration);
        alarmAdjustDuration.setMax(DURATION_SEEK_RANGE);
        if ( offsetBefore ) alarmAdjustDuration.setProgress(DURATION_SEEK_RANGE);
        
        relativeDurationLayout = (LinearLayout) findViewById(R.id.RelativeDurationLayout);
        absoluteAlarmTimeLayout = (LinearLayout) findViewById(R.id.AbsoluteAlarmTimeLayout);
        if ( start == null && end == null ) {
        	((LinearLayout) findViewById(R.id.ButtonLayout)).setVisibility(View.GONE);
        	relatedButton.setEnabled(false);
        	beforeButton.setEnabled(false);
        	relativeDurationText.setText(context.getString(R.string.Exactly));
        }
        else {
        	alarmAdjustDuration.setOnSeekBarChangeListener(this);
        }
        
        setTitle(context.getString(R.string.SetCustomAlarm));
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefer24hourFormat = prefs.getBoolean(context.getString(R.string.prefTwelveTwentyfour), DateFormat.is24HourFormat(context));
		updateLayout();
    }

	
	private void updateLayout() {
		if ( RelateWith.ABSOLUTE == relativeTo ) {
        	relatedButton.setText(context.getString(R.string.Exactly));
        	beforeButton.setText(context.getString(R.string.Exactly));
        	relativeDurationText.setText(context.getString(R.string.Exactly));
        	relativeDurationLayout.setVisibility(View.GONE);
        	absoluteAlarmTimeLayout.setVisibility(View.VISIBLE);
        	absoluteDateTimeButton.setText(AcalDateTimeFormatter.fmtFull(timeToFire, prefer24hourFormat));
		}
		else {
        	relativeDurationLayout.setVisibility(View.VISIBLE);
        	absoluteAlarmTimeLayout.setVisibility(View.GONE);
			String relatedPart = context.getString(R.string.Start);
			if ( RelateWith.START == relativeTo ) { 
	        	relatedButton.setText(context.getString(R.string.Start));
	        	timeToFire = originalStart.clone(); 
			}
			else if ( RelateWith.END == relativeTo ) {
				relatedPart = context.getString(R.string.Finish);
	        	relatedButton.setText(context.getString( parentType.equals(VComponent.VTODO)? R.string.Due : R.string.Finish ));
	        	timeToFire = originalEnd.clone(); 
			}

        	beforeButton.setText(context.getString(offsetBefore ? R.string.Before : R.string.After));
        	timeToFire.setAsDate(false).addDuration(relativeOffset);

        	alarmAdjustDuration.setEnabled(true);
        	
        	relativeDurationText.setVisibility(View.VISIBLE);
        	relativeDurationText.setText(relativeOffset.toPrettyString(relatedPart));
		}
    	alarmTimeText.setText(AcalDateTimeFormatter.fmtFull(timeToFire, prefer24hourFormat));
	}


	private void toggleBeforeButton() {
        if ( originalStart == null && originalEnd == null ) return;

        offsetBefore = !offsetBefore;
		int newDays = Math.abs(relativeOffset.getDays());
		long newSeconds = Math.abs(relativeOffset.getTimeMillis()) / 1000L;
		newDays *= (offsetBefore?-1:1);
		newSeconds *= (offsetBefore?-1:1);
		relativeOffset.setDuration(newDays, (int) newSeconds);
		alarmAdjustDuration.setProgress(alarmAdjustDuration.getMax() - alarmAdjustDuration.getProgress());
		onProgressChanged(alarmAdjustDuration, alarmAdjustDuration.getProgress(), true);
		updateLayout();
	}

	
	private void toggleRelatedButton() {
        if ( originalStart == null && originalEnd == null ) return;

    	relativeTo = ( RelateWith.START == relativeTo
			    			? RelateWith.END
			    			: ( RelateWith.END == relativeTo
			    					? RelateWith.ABSOLUTE
			    					: RelateWith.START
			    			));

    	if ( relativeTo == RelateWith.START && originalStart == null ) toggleRelatedButton();
    	else if ( relativeTo == RelateWith.END && originalEnd == null ) toggleRelatedButton();

    	updateLayout();
	}

	
	protected void absoluteDateTimeDialog() {

		DateTimeSetListener absoluteDateTimeListener = new DateTimeSetListener() {

			@Override
			public void onDateTimeSet(AcalDateTime newDateTime) {
				// @todo Auto-generated method stub
				timeToFire = newDateTime;
				updateLayout();
			}
			
		};

		DateTimeDialog absoluteDateTime = new DateTimeDialog(context, timeToFire, prefer24hourFormat, false, false, absoluteDateTimeListener);
		absoluteDateTime.show();
	}

	
	@Override
	public void onClick(View v) {
		if (v == okButton) {
			AcalAlarm newAlarmValue = new AcalAlarm(relativeTo, alarmDescription, relativeOffset, actionType, 
					(RelateWith.START == relativeTo ? originalStart : timeToFire), originalEnd );
			dialogListener.onAlarmSet(newAlarmValue);
		}
		if (v == okButton || v == cancelButton ) this.dismiss();

		if ( v == beforeButton ) toggleBeforeButton();
		else if ( v == relatedButton ) toggleRelatedButton();
		else if ( v == absoluteDateTimeButton ) absoluteDateTimeDialog();

	}

	
	

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if ( fromUser ) {
			progress++;
			if ( offsetBefore ) progress = seekBar.getMax() - progress;
			int offsetMinutes = 0;
			if ( progress < 18 ) 		offsetMinutes = progress * 5;			//    0 -   85 by  5
			else if ( progress < 28 )	offsetMinutes = (progress - 12) *  15;	//   90 -  225 by 15
			else if ( progress < 44 )	offsetMinutes = (progress - 20) *  30;	//  240 -  720 by 30
			else if ( progress < 68 )	offsetMinutes = (progress - 32) *  60;	//  12h - 36h
			else if ( progress < 74 )	offsetMinutes = (progress - 50) * 120;	//  1.5-2 days
			else if ( progress < 80 )	offsetMinutes = (progress - 62) * 240;	//  2-3 days
			else if ( progress < 84 )	offsetMinutes = (progress - 68) * 360;	//  3-4 days
			else if ( progress < 90 )	offsetMinutes = (progress - 72) * 480;	//  4-6 days
			else if ( progress < 98 )	offsetMinutes = (progress - 84) * 1440;	//  6-14 days
			else						offsetMinutes = (progress - 96) * 10080;	//  3+ weeks

			int newDays = (offsetMinutes / 1440); 
			int newSeconds = (offsetMinutes % 1440) * 60;
			newDays *= (offsetBefore?-1:1);
			newSeconds *= (offsetBefore?-1:1);
			relativeOffset.setDuration(newDays, newSeconds);
			updateLayout();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}
}
