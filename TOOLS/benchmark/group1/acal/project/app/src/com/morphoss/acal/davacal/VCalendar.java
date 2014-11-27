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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalRepeatRule;
import com.morphoss.acal.acaltime.AcalRepeatRuleParser;
import com.morphoss.acal.activity.EventEdit;
import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.dataservice.CalendarInstance;

public class VCalendar extends VComponent implements Cloneable {
	public static final String TAG = "aCal VCalendar";
	
	/**
	 * This holds the range of time which instances of this overlap, from the start of the
	 * earliest instance through to the end of the last instance (or null if it continues
	 * forever).
	 */
	private AcalDateRange calendarRange = null;
	
	/**
	 * This holds the repeat rule.
	 */
	private AcalRepeatRule repeatRule = null;
	
	/**
	 * This indicates whether the masterInstance has any overrides.  A single instance calendar
	 * will never have this set.  A repeating calendar will not have this set unless there are
	 * multiple VEVENT(/VTODO/VJOURNAL) in the VCALENDAR which override some of the repeats.
	 */
	private Boolean masterHasOverrides = null;
	private Boolean hasAlarms = null;
	private Boolean hasRepeatRule = null;
	private Long earliestStart;
	private Long latestEnd;

	private Long collectionId = VComponent.VALUE_NOT_ASSIGNED;
	private Long resourceId = VComponent.VALUE_NOT_ASSIGNED;
	
	public static final Pattern tzOlsonExtractor = Pattern.compile(".*((?:Antarctica|America|Africa|Atlantic|Asia|Australia|Indian|Europe|Pacific|US)/(?:(?:[^/\"]+)/)?[^/\"]+)\"?");
	private static final String	TZNAME_UTC	= "UTC";


	public VCalendar(ComponentParts splitter, long collectionId, long resourceId, Long earliestStart, Long latestEnd, VComponent parent) {
		super(splitter, parent);
		this.collectionId = collectionId;
		this.resourceId = resourceId;
		this.earliestStart = earliestStart;
		this.latestEnd = latestEnd;
		if ( earliestStart != null ) {
			this.calendarRange = new AcalDateRange(AcalDateTime.fromMillis(earliestStart),
					(latestEnd == null ? null : AcalDateTime.fromMillis(latestEnd)));
		}
	}


	public VCalendar() {
		super( VComponent.VCALENDAR, null );
		setEditable();
		addProperty(new AcalProperty("CALSCALE","GREGORIAN"));
		addProperty(new AcalProperty("PRODID","-//morphoss.com//aCal 1.0//EN"));
		addProperty(new AcalProperty("VERSION","2.0"));
	}


	public VCalendar clone() {
		// parent is null, since a VCalendar is a top-level object.
		return new VCalendar(this.content, this.collectionId, this.resourceId, this.earliestStart, this.latestEnd, null);
	}

	
	public String applyEventAction(CalendarInstance event, int action, int instances) throws InvalidCalendarActionException {

		this.setEditable();
		VEvent vEvent = (VEvent) this.getMasterChild();

		// first, strip any existing properties which we always modify
		vEvent.removeProperties( new PropertyName[] {PropertyName.DTSTAMP, PropertyName.LAST_MODIFIED } );

		// change DTStamp
		AcalDateTime lastModified = new AcalDateTime();
		lastModified.setTimeZone(TimeZone.getDefault().getID());
		lastModified.shiftTimeZone("UTC");

		vEvent.addProperty(AcalProperty.fromString(lastModified.toPropertyString(PropertyName.DTSTAMP)));
		vEvent.addProperty(AcalProperty.fromString(lastModified.toPropertyString(PropertyName.LAST_MODIFIED)));

		if ( collectionId != event.getCollectionId() )
			throw new InvalidCalendarActionException("Cannot modify event collection on edit (from: "+event.getCollectionId()+" to: "+collectionId+")");

		switch (action) {
			case EventEdit.ACTION_EDIT:	  return doEdit(event, instances);
			case EventEdit.ACTION_DELETE: return doDelete(event, instances);
			default: throw new InvalidCalendarActionException();
		}
	}

	
	private String doEdit(CalendarInstance calendarInstance, int instances) throws InvalidCalendarActionException {
		Masterable childInstance = null;

		if ( instances == EventEdit.INSTANCES_ALL ) {
			childInstance = getMasterChild();
		}
		else {
			RecurrenceId rrid = RecurrenceId.fromString(calendarInstance.getRecurrenceId());
			childInstance = getChildFromRecurrenceId(rrid); 

			RecurrenceId baseRecurrenceId = null;
			try {
				baseRecurrenceId = (RecurrenceId) childInstance.getProperty(PropertyName.RECURRENCE_ID);
			}
			catch( ClassCastException e ) {
			}
			if ( instances == EventEdit.INSTANCES_THIS_FUTURE ) rrid.setThisAndFuture(true);

			if ( baseRecurrenceId == null || !rrid.equals(baseRecurrenceId) ) {
				childInstance = (Masterable) createComponentFromBlob(childInstance.getCurrentBlob());
			}

			childInstance.removeProperties(new PropertyName[] { PropertyName.RECURRENCE_ID } );
			childInstance.addProperty(rrid);
		}

		childInstance.setEditable();
		childInstance.removeProperties( new PropertyName[] {PropertyName.DTSTART, PropertyName.DTEND, PropertyName.DURATION,
				PropertyName.SUMMARY, PropertyName.LOCATION, PropertyName.DESCRIPTION, PropertyName.RRULE } );

		AcalDateTime dtStart = calendarInstance.getStart();
		childInstance.addProperty( dtStart.asProperty(PropertyName.DTSTART));

		AcalDateTime dtEnd = calendarInstance.getEnd();
		if ( (dtEnd.getTimeZoneId() == null && dtStart.getTimeZoneId() == null) || 
				(dtEnd.getTimeZoneId() != null && dtEnd.getTimeZoneId().equals(dtStart.getTimeZoneId())) )
			childInstance.addProperty(calendarInstance.getDuration().asProperty(PropertyName.DURATION) );
		else
			childInstance.addProperty( dtEnd.asProperty( PropertyName.DTEND ) );

		childInstance.addProperty(new AcalProperty(PropertyName.SUMMARY, calendarInstance.getSummary()));

		String location = calendarInstance.getLocation();
		if ( !location.equals("") )
			childInstance.addProperty(new AcalProperty(PropertyName.LOCATION,location));

		String description = calendarInstance.getDescription();
		if ( !description.equals("") )
			childInstance.addProperty(new AcalProperty(PropertyName.DESCRIPTION,description));

		String rrule = calendarInstance.getRRule();
		if ( rrule != null && !rrule.equals(""))
			childInstance.addProperty(new AcalProperty(PropertyName.RRULE,rrule));

		childInstance.updateAlarmComponents( calendarInstance.getAlarms() );

		updateTimeZones();
		if ( Constants.debugVComponent )
			Log.println(Constants.LOGD, TAG, "Constructed Blob for\n"+this.getCurrentBlob());
		return this.getCurrentBlob();
	}

	
	private String doDelete(CalendarInstance calendarInstance, int instances ) throws InvalidCalendarActionException {
		Masterable m = getMasterChild();
		m.setEditable();

		switch (instances) {
			case EventEdit.INSTANCES_SINGLE:
				AcalProperty exDate = m.getProperty(PropertyName.EXDATE);
				if ( exDate == null || exDate.getValue().equals("") ) 
					exDate = AcalProperty.fromString(calendarInstance.getStart().toPropertyString(PropertyName.EXDATE));
				else {
					m.removeProperties( new PropertyName[] {PropertyName.EXDATE} );
					exDate = AcalProperty.fromString(exDate.toRfcString() + "," + calendarInstance.getStart().fmtIcal() );
				}
				m.addProperty(exDate);
				break;

			case EventEdit.INSTANCES_THIS_FUTURE:
				AcalRepeatRuleParser parsedRule = AcalRepeatRuleParser.parseRepeatRule(calendarInstance.getRRule());
				AcalDateTime until = calendarInstance.getStart().clone();
				until.addSeconds(-1);
				parsedRule.setUntil(until);
				String rrule = parsedRule.toString();
				m.removeProperties( new PropertyName[] {PropertyName.RRULE} );
				m.addProperty(new AcalProperty(PropertyName.RRULE,rrule));
				break;
			
			case EventEdit.INSTANCES_ALL:
				return null;

			default: throw new InvalidCalendarActionException("Invalid action/instances combination.");
		}

		if ( Constants.debugVComponent )
			Log.println(Constants.LOGD, TAG, "Reconstructed Blob for\n"+this.getCurrentBlob());

		return this.getCurrentBlob();
	}
					

	/**
	 * TODO - needs to be refactored
	 */
	/**
	private void applyModify(Masterable mast, CalendarInstance action) {
		//there are 3 possible modify actions:
		if (action.getAction() == WriteableEventInstance.ACTION_MODIFY_SINGLE) {
			// Only modify the single instance
		}
		else if (action.getAction() == WriteableEventInstance.ACTION_MODIFY_ALL_FUTURE) {
			// Modify this instance, and all future instances.

		}
		else if (action.getAction() == WriteableEventInstance.ACTION_MODIFY_ALL) {
			// Modify all instances

			// First, strip any existing properties which we modify
			mast.removeProperties( new PropertyName[] {PropertyName.DTSTART, PropertyName.DTEND, PropertyName.DURATION,
					PropertyName.SUMMARY, PropertyName.LOCATION, PropertyName.DESCRIPTION, PropertyName.RRULE } );

			AcalDateTime dtStart = action.getStart();
			mast.addProperty( dtStart.asProperty(PropertyName.DTSTART));

			AcalDateTime dtEnd = action.getEnd();
			if ( (dtEnd.getTimeZoneId() == null && dtStart.getTimeZoneId() == null) || 
					(dtEnd.getTimeZoneId() != null && dtEnd.getTimeZoneId().equals(dtStart.getTimeZoneId())) )
				mast.addProperty(action.getDuration().asProperty(PropertyName.DURATION) );
			else
				mast.addProperty( dtEnd.asProperty( PropertyName.DTEND ) );

			mast.addProperty(new AcalProperty(PropertyName.SUMMARY, action.getSummary()));

			String location = action.getLocation();
			if ( !location.equals("") )
				mast.addProperty(new AcalProperty(PropertyName.LOCATION,location));

			String description = action.getDescription();
			if ( !description.equals("") )
				mast.addProperty(new AcalProperty(PropertyName.DESCRIPTION,description));

			String rrule = action.getRRule();
			if ( rrule != null && !rrule.equals(""))
				mast.addProperty(new AcalProperty(PropertyName.RRULE,rrule));

			mast.updateAlarmComponents( action.getAlarms() );
		}
	}
		*/

	public void updateTimeZones() {
		HashSet<String> tzIdSet = new HashSet<String>();
		for( VComponent comp : getChildren() ) {
			if ( !(comp instanceof Masterable) ) continue;
			Masterable mast = (Masterable) comp;
			for( PropertyName pn : PropertyName.localisableDateProperties() ) {
				AcalProperty p = mast.getProperty(pn);
				if ( p != null ) {
					String tzId = p.getParam("TZID");
					if ( tzId != null ) {
						tzIdSet.add(p.getParam("TZID"));
						if ( Constants.LOG_DEBUG )
							Log.println(Constants.LOGD,TAG,"Found reference to timezone '"+tzId+"' in event.");
					}
				}
			}
		}
	
		List<VComponent> removeChildren = new ArrayList<VComponent>();
		for (VComponent child : getChildren() ) {
			if ( child.name.equals(VComponent.VTIMEZONE) ) {
				String tzId = null;
				try {
					VTimezone vtz = (VTimezone) child;
					tzId = vtz.getTZID();
				}
				catch( Exception e ) {};
				if ( tzIdSet.contains(tzId) ) {
					if ( Constants.LOG_DEBUG )
						Log.println(Constants.LOGD,TAG,"Found child vtimezone for '"+tzId+"' in event.");
					tzIdSet.remove(tzId);
				}
				else {
					if ( Constants.LOG_DEBUG )
						Log.println(Constants.LOGD,TAG,"Removing vtimezone for '"+tzId+"' from event.");
					removeChildren.add(child);
				}
			}
			else {
				if ( Constants.LOG_DEBUG )
					Log.println(Constants.LOGD,TAG,"Found "+child.name+" component in event.");
			}
		}
		// Have to avoid the concurrent modification
		for(VComponent child : removeChildren ) {
			this.removeChild(child);
		}
	
		for ( String tzId : tzIdSet ) {
			VTimezone vtz;
			try {
				String tzBlob = VTimezone.getZoneDefinition(tzId);
				if ( Constants.debugVComponent ) {
					Log.println(Constants.LOGD,TAG,"Including timezone for '"+tzId+"' in VCALENDAR.");
					if ( Constants.LOG_VERBOSE )
						Log.println(Constants.LOGV,TAG,tzBlob);
					Log.i(TAG,Log.getStackTraceString(new Exception("not an error")));
				}
				vtz = (VTimezone) VComponent.createComponentFromBlob(tzBlob);
				vtz.setEditable();
				this.addChild(vtz);
			}
			catch ( UnrecognizedTimeZoneException e ) {
				Log.i(TAG,"Unable to build a timezone for '"+tzId+"'");
			}
			
		}
	}


	public void checkRepeatRule() {
		try {
			if (repeatRule == null) repeatRule = AcalRepeatRule.fromVCalendar(this,collectionId,resourceId);
		}
		catch ( Exception e ) {
			Log.e(TAG,"Exception getting repeat rule from VCalendar", e);
			Log.i(TAG,getCurrentBlob());
			repeatRule = null;
		}
		hasRepeatRule = ( repeatRule != null && repeatRule.repeatRule != AcalRepeatRule.SINGLE_INSTANCE );
	}

	public boolean appendAlarmInstancesBetween(ArrayList<AlarmRow> alarmList, AcalDateRange rangeRequested) {
		try {
			if ( Constants.debugAlarms && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, 
					"Appending alarm instances for VCALENDAR (resourceId "+this.resourceId+") within "+rangeRequested);
			if ( hasRepeatRule == null && repeatRule == null ) checkRepeatRule();
			if ( !hasRepeatRule ) {
				if ( this.hasAlarm() ) {
					if ( Constants.debugAlarms ) Log.println(Constants.LOGV,TAG,"Event has alarms");
					Masterable master = this.getMasterChild();
					CalendarInstance instance = CalendarInstance.getInstance(master, this.collectionId, this.resourceId, master.getRecurrenceId() );
	
					for (AcalAlarm alarm : instance.getAlarms()) {
						alarm.setToLocalTime();
						if ( Constants.debugAlarms && Constants.LOG_VERBOSE )
							Log.println(Constants.LOGV,TAG,"Alarm next time to fire is "+alarm.getNextTimeToFire().fmtIcal());
	
						if ( rangeRequested.contains(alarm.getNextTimeToFire()) ) {
								//the alarm needs to have event data associated
								AlarmRow row = new AlarmRow(
										alarm.getNextTimeToFire().applyLocalTimeZone().getMillis(),
										instance.getResourceId(),
										instance.getRecurrenceId(),
										alarm.blob
										);
								alarmList.add(row);
							}
						}
					
				}
			}
			else this.repeatRule.appendAlarmInstancesBetween(alarmList, rangeRequested);
		}
		catch( Exception e ) {
			Log.i(TAG,"Error adding alarm instances for VCALENDAR (resourceId "+this.resourceId+"): "+e.getMessage());
			if ( Constants.debugVComponent && Constants.LOG_DEBUG ) {
				Log.println(Constants.LOGD, TAG, Log.getStackTraceString(e)+"\nResource in error is:\n"+getCurrentBlob());
			}
		}
		return true;
	}

	
	public boolean appendCacheEventInstancesBetween(List<CacheObject> eventList, AcalDateRange rangeRequested ) {
		try {
			Masterable m = getMasterChild();
			if ( m != null ) {
				AcalProperty rProperty = m.getProperty(PropertyName.RRULE);
				if ( rProperty == null )
					rProperty = m.getProperty(PropertyName.RDATE);
				if ( rProperty == null ) {
					if ( Constants.LOG_DEBUG && CacheManager.DEBUG )
						Log.println(Constants.LOGD,TAG, "Individual instance CacheObject being created for id "+resourceId+", Summary: "+m.getSummary());
					eventList.add(new CacheObject(m,collectionId,resourceId));
					return true;
				}
				else {
					if ( Constants.LOG_DEBUG && CacheManager.DEBUG )
						Log.println(Constants.LOGD,TAG, "Building CacheObject instances in "+calendarRange+" from RepeatRule for id "+collectionId+"/"+resourceId+", Summary: "+m.getSummary());

					if ( calendarRange != null ) {
						AcalDateRange intersection = rangeRequested.getIntersection(this.calendarRange);
						if ( intersection != null ) {
							if ( hasRepeatRule == null && repeatRule == null ) checkRepeatRule();
							if ( hasRepeatRule ) {
								this.repeatRule.appendCacheEventInstancesBetween(eventList, intersection);
								return true;
							}
						}
					}
					else {
						checkRepeatRule();
						if ( repeatRule != null ) {
							this.repeatRule.appendCacheEventInstancesBetween(eventList, rangeRequested);
							return true;
						}
					}
				}
			}
			else
				Log.println(Constants.LOGD,TAG, "VCalendar master instance was null for "+getEffectiveType());
					
		}
		catch(Exception e) {
			Log.println(Constants.LOGI,TAG,"Exception in RepeatRule handling: "+ Log.getStackTraceString(e)+"\n"+getCurrentBlob());
		}
		return false;
	}

	public Masterable getMasterChild() {
		if (childrenSet) {
			for (VComponent vc : this.getChildren()) {
				if ( vc instanceof VEvent)   	return (VEvent) vc;
				if ( vc instanceof VTodo )		return (VTodo) vc;
				if ( vc instanceof VJournal )	return (VJournal) vc;
			}
		}
		for( PartInfo childInfo : content.partInfo ) {
			if ( childInfo.type.equals(VEVENT) ) {
				return new VEvent(new ComponentParts(childInfo.getComponent(content.componentString)), this);
			}
			else if ( childInfo.type.equals(VTODO) ) {
				return new VTodo(new ComponentParts(childInfo.getComponent(content.componentString)), this);
			}
			else if ( childInfo.type.equals(VJOURNAL) ) {
				return new VJournal(new ComponentParts(childInfo.getComponent(content.componentString)), this);
			}
		}
		return null;
	}

	public AcalDateTime getRangeEnd() {
		if ( calendarRange == null ) return null;
		return calendarRange.end;
	}

	@Override
	public String getEffectiveType() {
		Masterable masterInstance =  this.getMasterChild();
		if ( masterInstance != null ) return masterInstance.getEffectiveType();
		if ( childrenSet && getChildren().size() > 0) {
			VComponent vc = getChildren().get(0);
			return vc.name;
		}
		if ( content.partInfo.size() > 0 ) {
			return content.partInfo.get(0).type;
		}
		return name;
	}


	public boolean masterHasOverrides() {
		if ( masterHasOverrides == null ) {
			int countMasterables = 0;
			for( PartInfo childInfo : content.partInfo ) {
				if ( childInfo.type.equals(VEVENT) || childInfo.type.equals(VTODO) || childInfo.type.equals(VJOURNAL) ) {
					countMasterables++;
					if ( countMasterables > 1 ) break;
				}
			}
			if ( masterHasOverrides == null ) masterHasOverrides = (countMasterables > 1);
		}
		return masterHasOverrides;
	}


	/**
	 * Will return a Masterable based on the master instance for this VCalendar, or the last overriding master
	 * with a RANGE=THISANDFUTURE which is prior to the specified recurrence.  An exact override will always
	 * be preferred.
	 * 
	 * No check is made to ensure that the repetition rule for the master instance actually specifies whether
	 * a recurrence should occur on the given time.  You're expected to know that already :-)
	 *  
	 * @param recurrenceProperty
	 * @return
	 */
	public Masterable getChildFromRecurrenceId(RecurrenceId recurrenceProperty) {
		Masterable masterInstance = this.getMasterChild();
		if ( recurrenceProperty == null ) return masterInstance;

		RecurrenceId testRecurrence = null;
		boolean recalculateTimes = true;
		if ( masterHasOverrides() ) {
			Masterable override = null;
			try {
				this.setPersistentOn();
				List<Masterable> matchingChildren = new ArrayList<Masterable>();
				for (VComponent vc: this.getChildren()) {
					if (vc.containsPropertyKey(recurrenceProperty.getName()) && vc instanceof Masterable)
						matchingChildren.add((Masterable) vc);
				}
				if (matchingChildren.isEmpty()) {
					// Won't happen since we test for this in masterHasOverrides()
					return this.getMasterChild();
				}
				Collections.sort(matchingChildren, RecurrenceId.getVComponentComparatorByRecurrenceId());
				for ( int i = 0; i < matchingChildren.size(); i++ ) {
					testRecurrence = matchingChildren.get(i).getRecurrenceId();
					if ( testRecurrence.equals(recurrenceProperty) ) {
						recalculateTimes = false;
						override = matchingChildren.get(i);
						break;
					}
					if ( testRecurrence.overrides(recurrenceProperty) ) {
						override = matchingChildren.get(i);
						recalculateTimes = true;
					}
					override = matchingChildren.get(i);
				}
			} catch (YouMustSurroundThisMethodInTryCatchOrIllEatYouException e) {
				Log.w(TAG,Log.getStackTraceString(e));
			} finally {
				this.setPersistentOff();	
			}
			if ( !recalculateTimes ) return override;
			masterInstance = override;
		}

		masterInstance.setToRecurrence(recurrenceProperty);

		return masterInstance;
	}

	private static String checkKnownAliases( String tzId ) {
		if ( tzId.equals(TZNAME_UTC) ) return tzId;
		if ( tzId.equals("GMT") ) return TZNAME_UTC;
		if ( tzId.equals("Etc/UTC") ) return TZNAME_UTC;
		if ( tzId.equals("Etc/GMT") ) return TZNAME_UTC;
		if ( tzId.equals("Eastern Standard Time") || tzId.equals("Eastern Daylight Time")
				|| tzId.equals("Eastern Time (US & Canada)")
				|| tzId.equals("GMT -0500 (Standard) / GMT -0400 (Daylight)")
				) return "America/New_York";
		if ( tzId.equals("Pacific Standard Time") || tzId.equals("Pacific Daylight Time") ) return "America/Los_Angeles";
		if ( tzId.equals("Central Standard Time") || tzId.equals("Central Daylight Time") ) return "America/Chicago";
		if ( tzId.equals("Mountain Standard Time") || tzId.equals("Mountain Daylight Time") ) return "America/Denver";
		if ( tzId.equals("New Zealand Standard Time") || tzId.equals("New Zealand Daylight Time") ) return "Pacific/Auckland";
		if ( tzId.equals("Chennai") ) return "Asia/Mumbai";
		return null;
	}
	
	public static String staticGetOlsonName( String tzId ) {
		Matcher m = VCalendar.tzOlsonExtractor.matcher(tzId);
		if ( m.matches() ) {
			return m.group(1);
		}
		String aliasResult = checkKnownAliases(tzId);
		if ( aliasResult != null ) return aliasResult;
		Log.w(TAG,"Could not get Olson name from "+tzId, new Exception("Unrecognized Time Zone"));
		return tzId;
	}

	
	public String getOlsonName( String tzId ) {
		Matcher m = VCalendar.tzOlsonExtractor.matcher(tzId);
		if ( m.matches() ) {
			return m.group(1);
		}

		String aliasResult = checkKnownAliases(tzId);
		if ( aliasResult != null ) return aliasResult;

		if (childrenSet) {
			for (VComponent vc : this.getChildren()) {
				if ( vc instanceof VTimezone ) {
					if ( vc.getProperty("TZID").getValue() == tzId ) {
						AcalProperty idProperty = vc.getProperty("X-MICROSOFT-CDO-TZID");
						if ( idProperty != null && idProperty.getValue() != null ) {
							switch( Integer.parseInt(idProperty.getValue()) ) {
								case 0:    return("UTC");
								case 1:    return("Europe/London");
								case 2:    return("Europe/Lisbon");
								case 3:    return("Europe/Paris");
								case 4:    return("Europe/Berlin");
								case 5:    return("Europe/Bucharest");
								case 6:    return("Europe/Prague");
								case 7:    return("Europe/Athens");
								case 8:    return("America/Brasilia");
								case 9:    return("America/Halifax");
								case 10:   return("America/New_York");
								case 11:   return("America/Chicago");
								case 12:   return("America/Denver");
								case 13:   return("America/Los_Angeles");
								case 14:   return("America/Anchorage");
								case 15:   return("Pacific/Honolulu");
								case 16:   return("Pacific/Apia");
								case 17:   return("Pacific/Auckland");
								case 18:   return("Australia/Brisbane");
								case 19:   return("Australia/Adelaide");
								case 20:   return("Asia/Tokyo");
								case 21:   return("Asia/Singapore");
								case 22:   return("Asia/Bangkok");
								case 23:   return("Asia/Kolkata");
								case 24:   return("Asia/Muscat");
								case 25:   return("Asia/Tehran");
								case 26:   return("Asia/Baghdad");
								case 27:   return("Asia/Jerusalem");
								case 28:   return("America/St_Johns");
								case 29:   return("Atlantic/Azores");
								case 30:   return("America/Noronha");
								case 31:   return("Africa/Casablanca");
								case 32:   return("America/Argentina/Buenos_Aires");
								case 33:   return("America/La_Paz");
								case 34:   return("America/Indiana/Indianapolis");
								case 35:   return("America/Bogota");
								case 36:   return("America/Regina");
								case 37:   return("America/Tegucigalpa");
								case 38:   return("America/Phoenix");
								case 39:   return("Pacific/Kwajalein");
								case 40:   return("Pacific/Fiji");
								case 41:   return("Asia/Magadan");
								case 42:   return("Australia/Hobart");
								case 43:   return("Pacific/Guam");
								case 44:   return("Australia/Darwin");
								case 45:   return("Asia/Shanghai");
								case 46:   return("Asia/Novosibirsk");
								case 47:   return("Asia/Karachi");
								case 48:   return("Asia/Kabul");
								case 49:   return("Africa/Cairo");
								case 50:   return("Africa/Harare");
								case 51:   return("Europe/Moscow");
								case 53:   return("Atlantic/Cape_Verde");
								case 54:   return("Asia/Yerevan");
								case 55:   return("America/Panama");
								case 56:   return("Africa/Nairobi");
								case 58:   return("Asia/Yekaterinburg");
								case 59:   return("Europe/Helsinki");
								case 60:   return("America/Godthab");
								case 61:   return("Asia/Rangoon");
								case 62:   return("Asia/Kathmandu");
								case 63:   return("Asia/Irkutsk");
								case 64:   return("Asia/Krasnoyarsk");
								case 65:   return("America/Santiago");
								case 66:   return("Asia/Colombo");
								case 67:   return("Pacific/Tongatapu");
								case 68:   return("Asia/Vladivostok");
								case 69:   return("Africa/Ndjamena");
								case 70:   return("Asia/Yakutsk");
								case 71:   return("Asia/Dhaka");
								case 72:   return("Asia/Seoul");
								case 73:   return("Australia/Perth");
								case 74:   return("Asia/Riyadh");
								case 75:   return("Asia/Taipei");
								case 76:   return("Australia/Sydney");
								
								case 57: // null
								case 52: // null
								default: // null
							}
						}
					}
				}
			}
		}

		Log.w(TAG,"Could not get Olson name from "+tzId, new Exception("Unrecognized Time Zone"));

		/**
		 * @todo: We should 
		 */
		return null; // We failed :-(
	}

	
	public boolean hasAlarm() {
		if ( this.hasAlarms != null ) return this.hasAlarms;
		for( PartInfo childInfo : content.partInfo ) {
			if ( childInfo.type.equals(VEVENT) ) {
				VEvent vc = new VEvent(new ComponentParts(childInfo.getComponent(content.componentString)), this);
				for( PartInfo childChildInfo : vc.content.partInfo ) {
					if ( childChildInfo.type.equals(VALARM)) {
						this.hasAlarms = true;
						return true;
					}
				}
			}
			else if ( childInfo.type.equals(VTODO) ) {
				VTodo vc = new VTodo(new ComponentParts(childInfo.getComponent(content.componentString)), this);
				for( PartInfo childChildInfo : vc.content.partInfo ) {
					if ( childChildInfo.type.equals(VALARM))  {
						this.hasAlarms = true;
						return true;
					}
				}
			}
			else if ( childInfo.type.equals(VALARM))  {
				this.hasAlarms = true;
				return true;
			}
		}
		this.hasAlarms = false;
		return false;
	}

	
	public VCalendar(Parcel in) {
		super(in);
	}

	public static final Parcelable.Creator<VCalendar> CREATOR = new Parcelable.Creator<VCalendar>() {
		public VCalendar createFromParcel(Parcel in) {
			return new VCalendar(in);
		}

		public VCalendar[] newArray(int size) {
			return new VCalendar[size];
		}
	};


	public AcalDateRange getInstancesRange() {
		if ( calendarRange != null ) return calendarRange;
		
		if ( hasRepeatRule == null && repeatRule == null ) checkRepeatRule();
		if ( !hasRepeatRule ) {
			Masterable m = getMasterChild();
			AcalDateTime dtStart = m.getStart(); 
			earliestStart = (dtStart == null ? null : dtStart.applyLocalTimeZone().getMillis());
			AcalDateTime dtEnd = m.getEnd();
			latestEnd = (dtEnd == null ? null : dtEnd.applyLocalTimeZone().getMillis());
			calendarRange = new AcalDateRange(dtStart,dtEnd);
		}
		else {
			calendarRange = repeatRule.getInstancesRange();
		}
		return calendarRange;
	}

}
