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

package com.morphoss.acal.acaltime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.alarmmanager.AlarmRow;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.dataservice.EventInstance;
import com.morphoss.acal.davacal.AcalAlarm;
import com.morphoss.acal.davacal.AcalProperty;
import com.morphoss.acal.davacal.Masterable;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.davacal.RecurrenceId;
import com.morphoss.acal.davacal.VCalendar;
import com.morphoss.acal.davacal.VComponent;
import com.morphoss.acal.davacal.VEvent;

/**
 * @author Morphoss Ltd
 */


public class AcalRepeatRule {

	final static public String			TAG					= "AcalRepeatRule";

	private final AcalDateTime			baseDate;
	public final AcalRepeatRuleParser	repeatRule;

	private AcalDateTime[]				rDate				= null;
	private int							rDatePos			= -1;
	private AcalDateTime[]				exDate				= null;
	private int							exDatePos			= -1;

	private List<AcalDateTime>			recurrences			= null;
	private Map<Long, LocalEventInstance>	eventTimes			= null;
	private int							lastCalc			= -1;
	private int							currentPos			= -1;
	private boolean						finished			= false;
	private boolean						started				= false;
	private AcalDuration				baseDuration		= null;
	private AcalDuration				lastDuration		= null;

	private VCalendar					sourceVCalendar		= null;

	private long	collectionId = VComponent.VALUE_NOT_ASSIGNED;
	private long	resourceId = VComponent.VALUE_NOT_ASSIGNED;

	final public static AcalRepeatRuleParser SINGLE_INSTANCE = AcalRepeatRuleParser.parseRepeatRule("FREQ=DAILY;COUNT=1");
	
	final private static int			MAX_REPEAT_INSTANCES	= 100;
	
	public AcalRepeatRule(AcalDateTime dtStart, String rRule) {
		baseDate = dtStart.clone();
		if ( rRule == null || rRule.equals("")) {
			recurrences = new ArrayList<AcalDateTime>(1);
			recurrences.add(baseDate);
			currentPos	= -1;
			lastCalc	= 0;
			started 	= true;
			finished 	= true;
			repeatRule = AcalRepeatRule.SINGLE_INSTANCE;
		}
		else
			repeatRule = AcalRepeatRuleParser.parseRepeatRule(rRule);
		eventTimes = new HashMap<Long,LocalEventInstance>();
	}


	public void setUntil(AcalDateTime newUntil) {
		repeatRule.setUntil(newUntil);
	}	


	/**
	 * 
	 * @param vCal
	 * @param collectionId The collectionId to include in any returned CalendarInstance object.
	 * @param resourceId The resourceId to include in any returned CalendarInstance object.
	 * @return
	 */
	public static AcalRepeatRule fromVCalendar( VCalendar vCal, long collectionId, long resourceId ) {
		Masterable masterComponent = vCal.getMasterChild();
		if ( masterComponent == null ) {
			if ( Constants.debugRepeatRule && Constants.LOG_VERBOSE ) {
				Log.w(TAG, "Cannot find master instance inside " + vCal.getName() );
				Log.println(Constants.LOGV,TAG, "Original blob is\n"+vCal.getOriginalBlob() );
			}
			return null;
		}

		AcalProperty repeatFromDate = masterComponent.getProperty(PropertyName.DTSTART);
		if ( repeatFromDate == null )
			repeatFromDate = masterComponent.getProperty(PropertyName.DUE);
		if ( repeatFromDate == null )
			repeatFromDate = masterComponent.getProperty(PropertyName.DTEND);
		if ( repeatFromDate == null )
			repeatFromDate = masterComponent.getProperty(PropertyName.COMPLETED);
		if ( repeatFromDate == null ) {
//			if ( Constants.debugRepeatRule && Constants.LOG_DEBUG ) {
				Log.println(Constants.LOGD,TAG,"Cannot calculate instances of "+masterComponent.getName()+" without DTSTART/DUE inside " + vCal.getName() );
				repeatFromDate = masterComponent.getProperty(PropertyName.DTSTART);
				masterComponent = vCal.getMasterChild();
				repeatFromDate = masterComponent.getProperty(PropertyName.DTSTART);
				Log.println(Constants.LOGD,TAG, "Original blob is\n"+vCal.getOriginalBlob() );
//			}
			return null;
		}
		AcalProperty rRuleProperty = masterComponent.getProperty(PropertyName.RRULE);
		if ( rRuleProperty == null ) rRuleProperty = new AcalProperty( PropertyName.RRULE, AcalRepeatRule.SINGLE_INSTANCE.toString());

		AcalDateTime baseDate = AcalDateTime.fromIcalendar(repeatFromDate.getValue(),
				repeatFromDate.getParam(AcalProperty.PARAM_VALUE),repeatFromDate.getParam(AcalProperty.PARAM_TZID));

		AcalRepeatRule ret = null;
		try {
			ret = new AcalRepeatRule( baseDate, rRuleProperty.getValue());
		}
		catch( NullPointerException npe ) {}
		catch( IllegalArgumentException e ) {
			Log.i(TAG,e.getMessage());
		}
		if ( ret == null ) {
			Log.i(TAG,"Failed to parse repeat rule from:\n"+repeatFromDate+"\n"+rRuleProperty);
			return ret;
		}

		ret.sourceVCalendar = vCal;
		ret.collectionId = collectionId;
		ret.resourceId = resourceId;
		
		ret.baseDuration = masterComponent.getDuration();

		PropertyName dateLists[] = {PropertyName.RDATE,PropertyName.EXDATE};
		for( PropertyName dListPName : dateLists ) {
			AcalProperty dateListProperty = masterComponent.getProperty(dListPName);
			if ( dateListProperty == null )	continue;
		
			String value = dateListProperty.getValue();
			if ( value == null )	continue;
		
			String isDateParam = dateListProperty.getParam(AcalProperty.PARAM_VALUE);
			String tzIdParam = dateListProperty.getParam(AcalProperty.PARAM_TZID);
			
			final String[] dateList = Constants.splitOnCommas.split(value);
			AcalDateTime[] timeList = new AcalDateTime[dateList.length];
			
			for( int i=0; i < dateList.length; i++ ) {
				timeList[i] = AcalDateTime.fromIcalendar( dateList[i], isDateParam, tzIdParam );
			}
			Arrays.sort(timeList);
			if ( dListPName.equals(PropertyName.RDATE) ) {
				ret.rDate = timeList;
				ret.rDatePos = 0;
			}
			else if ( dListPName.equals(PropertyName.EXDATE) ) {
				ret.exDate = timeList;
				ret.exDatePos = 0;
			}
		}
		

		return ret;
	}
	
	
	public void reset() {
		currentPos = -1;
	}

	public AcalDateTime next() {
		if (currentPos > lastCalc && finished) return null;
		currentPos++;
		getMoreInstances();
		if (currentPos > lastCalc && finished) return null;
		if ( currentPos >= recurrences.size() ) {
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG,"Managed to exceed recurrences.size() at " + currentPos+"/"+recurrences.size()
						+" Last"+lastCalc+(finished?" (finished)":"")
						+" processing: " + this.repeatRule.toString() );
		}
		return recurrences.get(currentPos);
	}

	public boolean hasNext() {
		if ( currentPos < lastCalc ) return true;
		getMoreInstances();
		if (currentPos >= lastCalc && finished) return false;
		return true;
	}

	/**
	 * <h3>Internal enum for types of FREQ=type</h3>
	 * <p>
	 * At present we only support Yearly, Monthly, Weekly Daily.  Hourly, Minutely and Secondly are
	 * omitted, although the code is fairly trivial, and we should probably write it for completeness.
	 * </p>
	 * 
	 * @author Morphoss Ltd
	 */
	public enum RRuleFreqType {
		YEARLY, MONTHLY, WEEKLY, DAILY; //, HOURLY, MINUTELY, SECONDLY;

		private final static Pattern	freqPattern	= Pattern.compile(
						".*FREQ=((?:WEEK|DAI|YEAR|MONTH|HOUR|MINUTE|SECOND)LY).*",
						Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		
		/**
		 * <p>
		 * Assumes we have essentially correct strings and efficiently turns them into enums efficiently.
		 * Maybe we could be marginally more efficient if we purely treated the two-byte strings as a short
		 * int, but hey.
		 * </p>
		 * 
		 * @param stFreq
		 * @return
		 */
		public static RRuleFreqType fromString(String stFreq) {
			Matcher m = freqPattern.matcher(stFreq);
			if ( !m.matches() )
				throw new IllegalArgumentException("RRULE '"+stFreq+"' is not a valid frequency specifier.");
			switch (m.group(1).charAt(0)) {
				case 'Y':
					return YEARLY;
				case 'W':
					return WEEKLY;
				case 'D':
					return DAILY;
//				case 'H':
//					return HOURLY;
//				case 'S':
//					return SECONDLY;
				case 'M':
					switch (m.group(1).charAt(1)) {
						case 'O':
							return MONTHLY;
//						case 'I':
//							return MINUTELY;
					}
			}
			throw new IllegalArgumentException("Invalid frequency 'FREQ="+m.group(1)+"' in RRULE definition: "+stFreq);
		}
	};



	private boolean getMoreInstances() {
	    if ( finished ) return false;
	    if ( currentPos < lastCalc ) return true;
	    
	    if ( recurrences != null && recurrences.size() > 3000 ) {
			Log.e(TAG,"Too many instances (3000):");
			Log.e(TAG,"Too many " +baseDate.toPropertyString(PropertyName.DTSTART));
			Log.e(TAG,"Too many " +repeatRule.toString());
		    throw new RuntimeException("ETOOTOOMUCHREPETITIONKTHXBAI");
		}

	    boolean foundSome = false;
	    int emptySets = 0;

		while ( !finished && currentPos >= lastCalc ) {
		   	repeatRule.nextBaseDate(baseDate);
	    	List<AcalDateTime> newSet = repeatRule.buildSet();
	    	if ( newSet.isEmpty() ) {
	    		if ( emptySets++ > 50 ) {
	    			finished = true;
	    			Log.e(TAG,"Too many empty sets processing "+repeatRule.toString());
	    		}
	    		continue;
	    	}
			Collections.sort(newSet, new AcalDateTime.AcalDateTimeSorter());

			emptySets = 0;
	    	AcalDateTime thisInstance = null;
    		int i=0;
	    	while( !finished && i < newSet.size()
	    				&& ( repeatRule.count == AcalRepeatRuleParser.INFINITE_REPEAT_COUNT 
	    							||  lastCalc < repeatRule.count ) ) {
	    		thisInstance = newSet.get(i++);
	    		if ( !started ) {
	    			if ( thisInstance.before(baseDate)) continue;
	    		    if ( recurrences == null ) {
	    		    	recurrences = new ArrayList<AcalDateTime>();
	    		    }
	    			if ( thisInstance.after(baseDate) ) recurrences.add(baseDate);
	    			started = true;
		    		foundSome = true;
	    		}
	    		if ( repeatRule.until != null && thisInstance.after(repeatRule.until) ) {
	    			finished = true;
	    			break;
	    		}
	    		if ( exDate != null && exDatePos < exDate.length ) {
	    			if ( exDate[exDatePos] == null || exDate[exDatePos].equals(thisInstance) || exDate[exDatePos].before(thisInstance) ) {
	    				exDatePos++;
	    				continue;
	    			}
	    		}
	    		while ( rDate != null && rDatePos < rDate.length && 
	    					(rDate[rDatePos] == null || rDate[rDatePos].before(thisInstance)) ) {
		    		recurrences.add(rDate[rDatePos++].clone());
		    		lastCalc++;
		    		foundSome = true;

		    		if ( repeatRule.count != AcalRepeatRuleParser.INFINITE_REPEAT_COUNT
			    				&& lastCalc >= repeatRule.count ) {
			    		finished = true;
			    		break;
			    	}
	    		}
	    		recurrences.add(thisInstance.clone());
	    		lastCalc++;
	    		foundSome = true;

		    	if ( repeatRule.count != AcalRepeatRuleParser.INFINITE_REPEAT_COUNT
		    				&& lastCalc >= repeatRule.count ) {
		    		finished = true;
		    	}
	    	}
		}
//		debugInstanceList("Got More Instances");
		return foundSome;
	}


	public List<AcalDateTime> getInstancesInRange( AcalDateTime start, AcalDateTime end ) {
		if ( end == null )
			throw new IllegalArgumentException("getInstancesInRange: End of range may not be null.");

		if ( start == null )
			start = baseDate;
		else if ( repeatRule.until != null && start.after(repeatRule.until) )
			return new ArrayList<AcalDateTime>(0);

		if ( recurrences != null ) {
			for ( currentPos=0; currentPos<=lastCalc && recurrences.get(currentPos).before(start); currentPos++)
				;
		}

		AcalDateTime thisDate = null;
		do {
			thisDate = next();
		}
		while( thisDate.before(start) );

		List<AcalDateTime> ret = new ArrayList<AcalDateTime>();
		while( thisDate != null && thisDate.before(end) ) {
			ret.add(thisDate);
			thisDate = next();
		}
		return ret;
	}


	/**
	 * Returns a range which is from the earliest start date to the latest end date
	 * for the recurrence of a VCALENDAR-based rule.
	 * 
	 * Instances without an UNTIL or with a COUNT > 3000 will be considered 'infinite'
	 * and the range end will be null.
	 * 
	 * @return
	 * @throws Exception 
	 */
	public AcalDateRange getInstancesRange() {
		AcalDateTime endDate = null;
		if ( repeatRule.until != null ) endDate = repeatRule.until;
		else if ( repeatRule.count != AcalRepeatRuleParser.INFINITE_REPEAT_COUNT
					&& repeatRule.count < MAX_REPEAT_INSTANCES ) {

			if ( Constants.debugRepeatRule && Constants.LOG_DEBUG )
				Log.println(Constants.LOGD,TAG,"Calculating instance range for count limited repeat: " + repeatRule.toString() );

			while( hasNext() ) {
				next();
			}
			endDate = recurrences.get(currentPos);
			try {
				sourceVCalendar.setPersistentOn();
				RecurrenceId ourRecurrenceId = (RecurrenceId) AcalProperty.fromString(endDate.toPropertyString(PropertyName.RECURRENCE_ID));
				Masterable vMaster = sourceVCalendar.getChildFromRecurrenceId(ourRecurrenceId);
				LocalEventInstance instance = getRecurrence(endDate,vMaster);
				eventTimes.put(endDate.getEpoch(), instance );
				endDate = instance.dtend;
			}
			catch ( Exception e ) {
				Log.w(TAG,"Exception while calculating instance range");
				Log.w(TAG,Log.getStackTraceString(e));
			}
			finally {
				sourceVCalendar.setPersistentOff();
			}
		}
		return new AcalDateRange(baseDate,endDate);
	}

	private final static AcalDateTime futureish = AcalDateTime.fromMillis(System.currentTimeMillis() + (86400000L*365L*10));

	//TODO dirty hack to get alarms in range.
	public void appendAlarmInstancesBetween(ArrayList<AlarmRow> alarmList, AcalDateRange range) {
		
		List<EventInstance> events = new ArrayList<EventInstance>();
		if ( this.sourceVCalendar.hasAlarm() ) {
			if ( Constants.debugAlarms ) Log.println(Constants.LOGV,TAG,"Event has alarms");
			this.appendEventsInstancesBetween(events, range, false);
			for( EventInstance event : events ) {
				for (AcalAlarm alarm : event.getAlarms()) {
					alarm.setToLocalTime();
					if ( Constants.debugAlarms && Constants.LOG_VERBOSE )
						Log.println(Constants.LOGV,TAG,"Alarm next time to fire is "+alarm.getNextTimeToFire().fmtIcal());

					if ( range.contains(alarm.getNextTimeToFire()) ) {
						//the alarm needs to have event data associated
						AlarmRow row = new AlarmRow(
								alarm.getNextTimeToFire().applyLocalTimeZone().getMillis(),
								event.getResourceId(),
								event.getRecurrenceId(),
								alarm.blob
								);
						alarmList.add(row);
					}
				}
			}
		}

	}

	public void appendCacheEventInstancesBetween(List<CacheObject> cacheList, AcalDateRange range) {
		this.appendEventsInstancesBetween(cacheList, range, true);
	}
	
	@SuppressWarnings("unchecked") 
	private void appendEventsInstancesBetween( @SuppressWarnings("rawtypes") List eventList, AcalDateRange range, boolean cacheObjects) {
	
		if ( range.start == null || range.end == null || eventList == null ) return;

		Masterable thisEvent = sourceVCalendar.getMasterChild();
		if ( thisEvent == null ) return;
		
		if ( Constants.debugDateTime && range.start.after(futureish) ) {
			throw new IllegalArgumentException("The date: " + range.start.fmtIcal() + " is way too far in the future! (after " + futureish.fmtIcal() );
		}
		if ( Constants.debugRepeatRule && Constants.LOG_DEBUG ) {
			 Log.println(Constants.LOGD, TAG, "Appending instances in "+range.toString()+
					 ", Base is: "+this.baseDate.fmtIcal()+", Rule is: "+repeatRule.toString() );
		}

		if ( repeatRule.until != null && repeatRule.until.before(range.start) )
			return ;

		int found = 0;
		long processingStarted = System.currentTimeMillis();
		AcalDateTime thisDate = null;
		LocalEventInstance instance = null;
		Masterable ourVEvent = null;
		int possiblyInfinite = 0;
		try {
			reset();
			sourceVCalendar.setPersistentOn();
			ourVEvent = sourceVCalendar.getMasterChild();
			ourVEvent.setPersistentOn();
			do {
				thisDate = next();
				if ( thisDate == null ) {
					if ( Constants.debugRepeatRule && Constants.LOG_DEBUG )
						Log.println(Constants.LOGD,TAG, "Null before finding useful instance for " +repeatRule.toString() );
					break;
				}

				instance = eventTimes.get(thisDate.getEpoch());
				if ( instance == null ) {
					instance = getRecurrence(thisDate, ourVEvent);
					eventTimes.put(thisDate.getEpoch(), instance);
				}

				if ( Constants.debugRepeatRule && Constants.LOG_DEBUG ) {
					if ( instance.dtend.after(range.start) ) break;
					Log.println(Constants.LOGD,TAG, "Skipping Instance with recurrenceId: "+thisDate.fmtIcal()+" of " +repeatRule.toString()+
							"\n       scheduled from: "+instance.dtstart.fmtIcal()+" to "+instance.dtend.fmtIcal()+" which ends before "+ range.start);
				}
			}
			while( thisDate != null && ! instance.dtend.after(range.start) && possiblyInfinite < 20 );

			while( thisDate != null
						&& instance.dtstart.before(range.end)
						&& possiblyInfinite < 40 ) {

				instance = eventTimes.get(thisDate.getEpoch());
				if ( instance == null ) {
					instance = getRecurrence(thisDate, ourVEvent);
					eventTimes.put(thisDate.getEpoch(), instance);
				}
				if( ! instance.dtstart.before(range.end) ) break;

				if (cacheObjects) {
					eventList.add(instance.getCacheObject());
				} else {
					eventList.add(instance.getEventInstance());
				}
				
				if ( Constants.debugRepeatRule && Constants.LOG_DEBUG ) {
					Log.println(Constants.LOGD,TAG, "Adding Instance: "+thisDate.fmtIcal()+" of " +repeatRule.toString() );
					Log.println(Constants.LOGD,TAG, "Adding Instance range: "+instance.dtstart.fmtIcal()+" - "+instance.dtend.fmtIcal() );
				}
				
				thisDate = next();

				found++;
			}
		}
		catch ( Exception e ) {
			if ( Constants.LOG_VERBOSE ) {
				Log.println(Constants.LOGV,TAG,"Exception while appending event instances between "+range.start.fmtIcal()+" and "+range.end.fmtIcal());
				Log.println(Constants.LOGV,TAG,Log.getStackTraceString(e));
			}
		}
		finally {
			ourVEvent.setPersistentOff();
			sourceVCalendar.setPersistentOff();
		}
		if ( Constants.debugRepeatRule && Constants.LOG_DEBUG ) {
			Log.println(Constants.LOGD,TAG, "Took "+(System.currentTimeMillis()-processingStarted )+"ms to find "+found+" in "+repeatRule.toString() );
			if ( found > 0 ) {
				Log.println(Constants.LOGD,TAG, "Found "+found+" instances in "+ range.toString());
				for( int i=0; i<found; i++ ) {
					if (cacheObjects) {
						CacheObject thisOne = (CacheObject)eventList.get(i);
						Log.println(Constants.LOGV,TAG, "["+i+"] Start: " + AcalDateTime.fromMillis(thisOne.getStart()).fmtIcal() + 
								", End: " + AcalDateTime.fromMillis(thisOne.getEnd()).fmtIcal() );
					} else {
						EventInstance thisOne = (EventInstance)eventList.get(i);
						Log.println(Constants.LOGV,TAG, "["+i+"] Start: " + thisOne.getStart().fmtIcal() + ", End: " + thisOne.getEnd().fmtIcal() );	
					}
					
				}
			}
		}
		return;
	}

	private LocalEventInstance getRecurrence(AcalDateTime thisDate, Masterable ourVEvent ) {
		AcalDateTime instanceStart = thisDate.clone();

		if ( lastDuration == null ) lastDuration = baseDuration;
		AcalDuration ourDuration = lastDuration;

		if ( sourceVCalendar.masterHasOverrides() ) {
			RecurrenceId instanceId = RecurrenceId.fromString( thisDate.toPropertyString(PropertyName.RECURRENCE_ID));
			ourVEvent = sourceVCalendar.getChildFromRecurrenceId(instanceId);
			ourDuration = ourVEvent.getDuration();
		}

		lastDuration = ourDuration;
		
		LocalEventInstance ret = new LocalEventInstance(ourVEvent, instanceStart, ourDuration); 

		return ret;
	}

	private class LocalEventInstance {
		final Masterable masterInstance;
		final AcalDateTime dtstart;
		final AcalDateTime dtend;
		private RecurrenceId rrid = null;
		
		LocalEventInstance( Masterable masterIn, AcalDateTime dtstart, AcalDuration duration ) {
			if ( duration.seconds < 0 || duration.days < 0 )
				throw new IllegalArgumentException("Resource duration must be positive. UID: "+masterIn.getUID() );
			if ( Constants.debugRepeatRule && duration.days > 10 )
				throw new IllegalArgumentException();
			this.masterInstance = masterIn;
			this.dtstart = dtstart;
			this.dtend = AcalDateTime.addDuration(dtstart, duration);
		}

		EventInstance getEventInstance() {
			if ( collectionId == VComponent.VALUE_NOT_ASSIGNED || resourceId == VComponent.VALUE_NOT_ASSIGNED ) {
				throw new IllegalArgumentException("To retrieve CalendarInstances the RepeatRule must have valid collectionId and resourceId");
			}
			if ( dtstart != null ) 
				this.rrid = RecurrenceId.fromString(dtstart.toPropertyString(PropertyName.RECURRENCE_ID));

			return new EventInstance( (VEvent) masterInstance, collectionId, resourceId, rrid);
		}
		
		CacheObject getCacheObject() {
			if ( collectionId == VComponent.VALUE_NOT_ASSIGNED || resourceId == VComponent.VALUE_NOT_ASSIGNED ) {
				throw new IllegalArgumentException("To retrieve CacheObjects the RepeatRule must have valid collectionId and resourceId");
			}
			Thread.yield();
			return new CacheObject(masterInstance, collectionId, resourceId, dtstart, dtend, null);
		}
	}
/*
	private String[] debugDates = null;
	protected void debugInstanceList( String whereAmI ) {
		if ( recurrences == null || recurrences.isEmpty() ) {
			if ( Constants.debugRepeatRule && Constants.LOG_VERBOSE )
				Log.println(Constants.LOGV,TAG, "Instances at "+whereAmI+" is empty" );
			return;
		}
		debugDates = new String[recurrences.size()];
		String dateList = "";

		int startFrom = debugDates.length - 7;
		for( int i=0; i<debugDates.length; i++ ) {
			debugDates[i] = recurrences.get(i).fmtIcal();
			if ( i >= startFrom ) {
				if ( i > startFrom && i> 0 ) dateList += ", ";
				dateList += debugDates[i]; 
			}
			if ( i == 3 && debugDates[0].equals(debugDates[1]) && debugDates[1].equals(debugDates[2]) ) {
				if ( Constants.debugRepeatRule && Constants.LOG_VERBOSE ) {
					Log.println(Constants.LOGV,TAG, "Managed to build a duplicate list of dates by now" );
					try {
						throw new Exception("fake");
					}
					catch( Exception e ) {
						Log.println(Constants.LOGV,TAG, Log.getStackTraceString(e) );
					}
				}
			}
		}

		if ( Constants.debugRepeatRule && Constants.LOG_VERBOSE )
			Log.println(Constants.LOGV,TAG, "Instances at "+whereAmI+" ["+debugDates.length+"]: "+dateList );
	}

*/

}
