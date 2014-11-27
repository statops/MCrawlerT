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

package com.morphoss.acal.davacal;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.AcalApplication;
import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDuration;
import com.morphoss.acal.dataservice.EventInstance;

public class AcalAlarm implements Serializable, Parcelable, Comparable<AcalAlarm> {
	private static final long	serialVersionUID	= 1L;
	private static final String TAG = "AcalAlarm";
	
	public final RelateWith relativeTo;
	public final String description;
	public final AcalDuration relativeTime;
	public final AcalDateTime timeToFire;
	public final ActionType actionType;
	public boolean isSnooze = false;
	public AcalDateTime snoozeTime = null;
	public boolean hasEventAssociated = false;
	public EventInstance myEvent = null;
	public String blob;

	
	public String toString() {
		return "AcalAlarm: nextTriggerTime: "+this.getNextTimeToFire()+" Snooze: "+(isSnooze ? "Yes" : "No")+
			  " Action: "+actionType+" Relative: "+relativeTo;
	}
	
	@Override
	public boolean equals (Object o) {
		if (! (o instanceof AcalAlarm) || o == null ) return false;
		if (this == o) return true;
		AcalAlarm that = (AcalAlarm)o;
		return 	(this.relativeTo== that.relativeTo) &&
				(this.description != null ? (that.description != null) && (this.description.equals(that.description)) : that.description == null )&&
				(this.timeToFire != null ? (that.timeToFire != null) && (this.timeToFire.equals(that.timeToFire)) : that.timeToFire == null )&&
				(this.relativeTime != null ? (that.relativeTime != null) && (this.relativeTime.equals(that.relativeTime)) : that.relativeTime == null )&&
				(this.actionType != null ? (that.actionType != null) && (this.actionType.equals(that.actionType)) : that.actionType == null )&&
				(this.isSnooze == that.isSnooze);
	}

	public enum RelateWith {
		ABSOLUTE, START, END;
	}

	public enum ActionType {
		DISPLAY, AUDIO, IGNORED;

		public static ActionType fromString( String typeString ) {
			if ( typeString != null ) {
				if ( typeString.equalsIgnoreCase("DISPLAY")) return DISPLAY;
				if ( typeString.equalsIgnoreCase("AUDIO")) return AUDIO;
			}
			return IGNORED;
		}
	};

	/**
	 * Construct an AcalAlarm from the indicated values.
	 * @param relativeTo What the alarm is related to, or absolute
	 * @param description The alarm description
	 * @param relativeTime The duration relative to start/end.  Ignored for absolute alarms.
	 * @param actionType What action to take when the alarm fires.
	 * @param start  The datetime which the START or ABSOLUTE trigger is related to.
	 * @param end The datetime which the END trigger is related to.
	 */
	public AcalAlarm(RelateWith relativeTo, String description, AcalDuration relativeTime, ActionType actionType, AcalDateTime start, AcalDateTime end) {
		this.relativeTo = relativeTo;
		this.description = description;
		this.relativeTime = relativeTime;
		this.actionType = actionType;
		if ( relativeTo == RelateWith.START ) {
			if ( start == null ) throw new IllegalStateException("Can't relate an alarm to a non-existent DTSTART!");
			timeToFire = AcalDateTime.addDuration(start, relativeTime);
		}
		else if ( relativeTo == RelateWith.END ) {
			if ( end == null ) throw new IllegalStateException("Can't relate an alarm to a non-existent DTEND or DUE!");
			timeToFire = AcalDateTime.addDuration(end, relativeTime);
		}
		else {
			if ( start == null ) throw new NullPointerException("Absolute alarm must have non-null start time.");
			timeToFire = (start != null ? start.clone() : null);
		}
		
		if ( Constants.debugAlarms ) {
			Log.d(TAG,"Alarm created relative to "+relativeTo+
					" which is "+(relativeTo == RelateWith.END ? end : start ).fmtIcal()+
					" by "+relativeTime+
					" to fire at "+timeToFire.fmtIcal());
		}
	}


	/**
	 * Build an AcalAlarm from the VALARM component it came in.
	 * 
	 * @param component The VALARM component we are realising.
	 * @param parent The parent component, which we use to calculate a default description if we need to.
	 * @param start The start from the parent.  Passed in separately because we might be in repeat rule expansion.
	 * @param end The end from the parent.  Passed in separately because we might be in repeat rule expansion.
	 */
	public AcalAlarm( VAlarm component, Masterable parent, AcalDateTime start, AcalDateTime end ) {
		this.blob = component.getCurrentBlob();
		AcalProperty aProperty = component.getProperty("ACTION");
		if ( aProperty == null ) throw new InvalidCalendarComponentException("A VALARM component must have an ACTION property.");
		actionType = ( aProperty == null ? ActionType.IGNORED : ActionType.fromString(aProperty.getValue()));
		
		aProperty = component.getProperty(PropertyName.TRIGGER);
		if ( aProperty == null ) throw new InvalidCalendarComponentException("A VALARM component must have a TRIGGER property.");
		String related = aProperty.getParam("RELATED");
		String valueType =  aProperty.getParam("VALUE");
		AcalDuration tmpDuration = null;
		AcalDateTime tmpTriggerTime = null;
		if ( valueType == null || valueType.equalsIgnoreCase("DURATION") )
			tmpDuration = AcalDuration.fromProperty(aProperty); 		// returns null on failure
		if ( valueType == null || valueType.equalsIgnoreCase("DATE-TIME") || valueType.equalsIgnoreCase("DATE") )
			tmpTriggerTime = AcalDateTime.fromAcalProperty(aProperty); // returns null on failure 

		/**
		 * Sadly, while it's mandatory to specify both RELATED=START/END and VALUE=DURATION
		 * for relative triggers in RFC5545, RFC2245 said almost the opposite, appearing to
		 * have a default where the VALUE=DURATION and RELATED=START.  iCal does this.
		 * 
		 *  Given the variability we just have to cope with working it out from the content.
		 */
		if ( tmpDuration == null && tmpTriggerTime != null ) {
			relativeTo = RelateWith.ABSOLUTE;
			Log.i(TAG,"Absolute trigger from "+aProperty.toRfcString(), new Exception());
			timeToFire = tmpTriggerTime;
			relativeTime = new AcalDuration();  // i.e. PT0H
		}
		else {
			relativeTo = (related == null || related.equalsIgnoreCase("START") ? RelateWith.START : RelateWith.END);

			if ( relativeTo == RelateWith.START && start == null )
				throw new IllegalStateException("Can't relate an alarm to a non-existent DTSTART!");
			else if ( relativeTo == RelateWith.END && end == null )
				throw new IllegalStateException("Can't relate an alarm to a non-existent DTEND!");

			if ( tmpDuration == null )
				relativeTime = new AcalDuration();
			else
				relativeTime = tmpDuration;

			if ( relativeTo == RelateWith.START )
				timeToFire = AcalDateTime.addDuration(start, relativeTime);
			else
				timeToFire = AcalDateTime.addDuration(end, relativeTime);
		}

		aProperty = null;
		if ( AcalApplication.getPreferenceBoolean(PrefNames.ignoreValarmDescription, false) )
			aProperty = component.getProperty("DESCRIPTION");
		if ( aProperty == null || aProperty.getValue().equals("") || aProperty.getValue().equalsIgnoreCase("Default Mozilla Description") ) {
			aProperty = parent.getProperty("SUMMARY");
		}
		description = ( aProperty == null || aProperty.getValue().equals("") ? "Alarm" : aProperty.getValue());
	}

	
	public VAlarm getVAlarm( Masterable parent ) {
		VAlarm ret = new VAlarm( parent );
		
		AcalProperty aProperty;
		if ( relativeTo == RelateWith.ABSOLUTE ) {
			timeToFire.shiftTimeZone("UTC");
			aProperty = timeToFire.asProperty(PropertyName.TRIGGER);
			aProperty.setParam("VALUE", "DATE-TIME" );
		}
		else {
			aProperty = new AcalProperty(PropertyName.TRIGGER, relativeTime.toString());
			aProperty.setParam("RELATED", relativeTo.toString() );
			aProperty.setParam("VALUE", "DURATION" );
		}
		ret.addProperty(aProperty);

		aProperty = new AcalProperty(PropertyName.ACTION, this.actionType.toString());
		ret.addProperty(aProperty);

		aProperty = parent.getProperty(PropertyName.SUMMARY);
		if ( description != null && !description.equals("") && aProperty.getValue() != null && !description.equals(aProperty.getValue())) {
			ret.addProperty(new AcalProperty(PropertyName.DESCRIPTION, description));
		}
		else if ( this.actionType == ActionType.DISPLAY ) // Description is mandatory in this case
			ret.addProperty(new AcalProperty(PropertyName.DESCRIPTION, ""));
		
		return ret;
	}

	
	@Override
	public int compareTo(AcalAlarm another) {
		if ( this.getNextTimeToFire().before(another.getNextTimeToFire())) return -1;
		if ( this.getNextTimeToFire().after(another.getNextTimeToFire())) return 1;
		return 0;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		//TODO needs refactoring!!!
		out.writeByte((byte)(relativeTo.toString().charAt(0)));
		out.writeString(description);
		if ( relativeTime == null ) {
			throw new NullPointerException("relativeTime may not be null");
		}
		relativeTime.writeToParcel(out,flags);
		if ( timeToFire == null ) {
			throw new NullPointerException("timeToFire may not be null");
		}
		timeToFire.writeToParcel(out,flags);
		out.writeByte((byte)(isSnooze ? 'T' : 'F'));
		if (isSnooze) {
			snoozeTime.writeToParcel(out,flags);
		}

		out.writeByte((byte)(hasEventAssociated ? 'T' : 'F'));
		//TODO needs refactoring!!!
		if (hasEventAssociated) {
		//	myEvent.writeToParcel(out, flags);
		}
		out.writeString(actionType.toString());
	}

	public AcalAlarm(Parcel in) {
		byte b = in.readByte();
		relativeTo = (b == 'A' ? RelateWith.ABSOLUTE : (b == 'S' ? RelateWith.START : RelateWith.END));
		description = in.readString();
		relativeTime = new AcalDuration(in);
		timeToFire = new AcalDateTime(in);
	
		isSnooze = (in.readByte() == 'T');
		if (isSnooze) {
			this.snoozeTime = new AcalDateTime(in);
		}
		hasEventAssociated = (in.readByte() == 'T');
		if (hasEventAssociated) {
			//TODO needs refactoring!!!
			//myEvent = new AcalEvent(in);
			//myEvent = DefaultEventInstance.getInstance(in);
		}
		actionType = ActionType.fromString(in.readString());
	}

	public static final Parcelable.Creator<AcalAlarm> CREATOR = new Parcelable.Creator<AcalAlarm>() {
		public AcalAlarm createFromParcel(Parcel in) {
			return new AcalAlarm(in);
		}

		public AcalAlarm[] newArray(int size) {
			return new AcalAlarm[size];
		}
	};
	
	public long nextAlarmTime() {
		return timeToFire.getMillis();
	}

	public String prettyTimeToFire() {
		return timeToFire.fmtIcal();
	}

	public String toPrettyString() {
		if ( relativeTo == RelateWith.ABSOLUTE ) {
			return prettyTimeToFire();
		}

		return relativeTime.toPrettyString(relativeTo.toString().toLowerCase());
	}
	
	/**
	 * The following relate specifically to AlarmActivity/CDS
	 */
	public void snooze(AcalDuration howLong) {
		isSnooze = true; 
		this.snoozeTime = AcalDateTime.addDuration(new AcalDateTime(), howLong);
	}
	
	public void setEvent(EventInstance e) {
		this.hasEventAssociated = true;
		this.myEvent = e;
	}
	
	public EventInstance getEvent() {
		return this.myEvent;
	}
	
	public boolean isSnooze() {
		return this.isSnooze;
	}
	
	public AcalDateTime getNextTimeToFire() {
		if (!isSnooze) return timeToFire;
		return snoozeTime;
	}
	
	public void setToLocalTime() {
		this.timeToFire.applyLocalTimeZone();
		if (this.snoozeTime != null) this.snoozeTime.applyLocalTimeZone();
	}

}

