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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalRepeatRule.RRuleFreqType;

/**
 * <h2>
 * Class for parsing iCalendar repeat rules.  See RFC5545 :-)
 * </h2>
 * 
 * <p>
 * We attempt to be both efficient and comprehensive in what we parse in, but we strongly assume we have
 * valid data coming in, and we are *very* forgiving of errors in the data.
 * </p>
 * <p>
 * Note that this is only for parsing the RRULE / EXRULE properties though. Calculation of the repeat instances
 * needs other information (Start, End/Duration, RRULE, EXRULE, RDATE & EXDATE) which is handled in the
 * AcalRepeatRule class.
 * </p>
 * 
 * @author Morphoss Ltd
 *
 */

public abstract class AcalRepeatRuleParser {

	final public static String TAG = "AcalRepeatRuleParser";

	protected final static int MAX_REPEAT_COUNT = 1000;
	protected final static int INFINITE_REPEAT_COUNT = -1;

	protected final RRuleFreqType		frequency;
	protected int						count;
	public final int					interval;
	public final int[]					bysecond;
	public final int[]					byminute;
	public final int[]					byhour;
	public final int[]					bymonthday;
	public final int[]					byyearday;
	public final int[]					byweekno;
	public final AcalRepeatRuleDay[]	byday;
	public final int[]					bymonth;
	public final int[]					bysetpos;
	public final AcalRepeatRuleDay		wkst;

	protected AcalDateTime				until = null;  // Can't make this final because we set the timezone later
	
	protected AcalDateTime			originalBase= null;
	protected AcalDateTime			currentBase	= null;
	protected List<AcalDateTime>	currentSet	= null;
	
	protected String[] debugDates = null;
	
	public AcalRepeatRuleParser( String rRuleValue, RRuleFreqType f ) {
		frequency = f;
		if ( rRuleValue.substring(0, 6).equalsIgnoreCase("RRULE:") ) {
			rRuleValue = rRuleValue.substring(6);
		}
		if ( rRuleValue.length() < 10 ) {
			throw new IllegalArgumentException("RRULE '"+rRuleValue+"' does not contain a valid FREQ= element.");
		}

		String preCount = null;
		int preInterval = 1;
		AcalRepeatRuleDay preWkst = new AcalRepeatRuleDay("MO");
		int[] preBysecond = null;
		int[] preByminute = null;
		int[] preByhour = null;
		int[] preBymonthday = null;
		int[] preByyearday = null;
		int[] preByweekno = null;
		AcalRepeatRuleDay[] preByday = null;
		int[] preBymonth = null;
		int[] preBysetpos = null;

		String[] ruleParts = rRuleValue.toUpperCase().split(";");
		for( String part : ruleParts ) {
			String[] values = part.split("[,=]");
			if ( values.length < 2 ) {
				Log.e(TAG,"RRULE part '"+part+"' is not of the form A=B,C,D in '"+rRuleValue+"'");
//				throw new IllegalArgumentException("RRULE part '"+part+"' is not of the form A=B,C,D");
			}
			PartType p = PartType.fromString(values[0]); 
			if ( p == null ) {
				Log.e(TAG,"Part '"+values[0]+"' is invalid");
				continue;
			}
			switch ( p ) {
				case FREQ:
					// The non-abstract instances know what they do for this
					break;
				case UNTIL:
					// TODO: we should get the timezone && isDate here.
					until = AcalDateTime.fromIcalendar(values[1], "", "");
					break;
				case COUNT:
					preCount = values[1]; 
					break;
				case INTERVAL:
					preInterval = Integer.parseInt(values[1]); 
					break;
				case WKST:
					preWkst = new AcalRepeatRuleDay(values[1]); 
					break;

				case BYMONTH:		preBymonth = parseIntArray(values);	break;
				case BYSETPOS:		preBysetpos = parseIntArrayMinusOne(values);	break;

				case BYMONTHDAY:	preBymonthday = parseIntArray(values);		break;
				case BYYEARDAY:		preByyearday = parseIntArray(values);		break;
				case BYWEEKNO:		preByweekno = parseIntArray(values);		break;
				case BYHOUR:		preByhour = parseIntArray(values);			break;
				case BYMINUTE:		preByminute = parseIntArray(values);		break;
				case BYSECOND:		preBysecond = parseIntArray(values);		break;
				case BYDAY:
					preByday = new AcalRepeatRuleDay[values.length-1];
					for ( int i=1, j=0; i<values.length; i++,j++ ) {
						preByday[j] = new AcalRepeatRuleDay(values[i]);
					}
			}
		}
		if ( preCount == null ) count = INFINITE_REPEAT_COUNT;
		else count = Integer.parseInt(preCount) - 1;
		
		interval = preInterval;
		wkst = preWkst;
		bymonth = preBymonth;
		bysetpos = preBysetpos;
		bymonthday = preBymonthday;
		byyearday = preByyearday;
		byweekno = preByweekno;
		byhour = preByhour;
		byminute = preByminute;
		bysecond = preBysecond;
		byday = preByday;
	}

	public static AcalRepeatRuleParser parseRepeatRule( String rRule ) {
		RRuleFreqType f = RRuleFreqType.fromString(rRule);
		switch (f) {
			case DAILY:		return new AcalRepeatDaily(rRule, f);
			case WEEKLY:	return new AcalRepeatWeekly(rRule, f);
			case MONTHLY:	return new AcalRepeatMonthly(rRule, f);
			case YEARLY:	return new AcalRepeatYearly(rRule, f);
		}
		return null;
	}

	public AcalDateTime getUntil() {
		return until.clone();
	}

	public void setUntil( AcalDateTime newUntil ) {
		count = INFINITE_REPEAT_COUNT;
		until = newUntil.clone();
		count = INFINITE_REPEAT_COUNT;
	}

	public String toString() {
		StringBuilder s = new StringBuilder("RRULE:");
		s.append("FREQ=");
		s.append(getFrequencyName());
		if ( count != INFINITE_REPEAT_COUNT ) { s.append(";COUNT="); s.append((count+1)); }
		if ( until != null ) { s.append(";UNTIL="); s.append(until.fmtIcal()); }
		if ( interval != 1 ) { s.append(";INTERVAL="); s.append(interval); }
		if ( wkst.wDay != AcalDateTime.MONDAY ) { s.append(";WKST="); s.append(wkst); }
		if ( bymonth != null ) { s.append(";BYMONTH="); s.append(commaListInts(bymonth)); }
		if ( bymonthday != null ) { s.append(";BYMONTHDAY="); s.append(commaListInts(bymonthday)); }
		if ( byyearday != null ) { s.append(";BYYEARDAY="); s.append(commaListInts(byyearday)); }
		if ( byweekno != null ) { s.append(";BYWEEKNO="); s.append(commaListInts(byweekno)); }
		if ( bysetpos != null ) { s.append(";BYSETPOS="); s.append(commaListIntsPlusOne(bysetpos)); }
		if ( byhour != null ) { s.append(";BYHOUR="); s.append(commaListInts(byhour)); }
		if ( byminute != null ) { s.append(";BYMINUTE="); s.append(commaListInts(byminute)); }
		if ( bysecond != null ) { s.append(";BYSECOND="); s.append(commaListInts(bysecond)); }
		if ( byday != null ) {
			s.append(";BYDAY=");
			for( int i=0; i<byday.length; i++ ) {
				if ( i>0 ) s.append(",");
				s.append(byday[i]); 
			}
		}
		return s.toString();
	}

	public String toPrettyString(Context cx) {
		if ( count == 0 ) return cx.getString(R.string.OnlyOnce);

		StringBuilder s = new StringBuilder(getPrettyFrequencyName(cx));
		
		boolean firstOne = true;
		if ( bymonth != null ) {
			s.append(" in ");
			for( int month : bymonth ) {
				if ( firstOne ) firstOne = false;
				else s.append(", ");
				s.append( AcalDateTime.getMonthName(month));
			}
		}

		if ( byyearday != null )  s.append(prettyListInts(" on the ", byyearday, " days of the year"));
		if ( bymonthday != null ) s.append(prettyListInts(" on the ", bymonthday, " days of the month"));
		if ( byweekno != null )   s.append(prettyListInts(" in the ", byweekno, " weeks of the year"));

		if ( bysecond != null )   s.append(prettyListInts(" on second ", bysecond, ""));
		if ( byminute != null )   s.append(prettyListInts(" on minute ", byminute, ""));
		if ( byhour != null )   s.append(prettyListInts(" in hour ", byhour, ""));

		if ( byday != null ) {
			firstOne = true;
			s.append(" on ");
			StringBuilder s2 = new StringBuilder();
			int weekdays = 0;
			int weekends = 0;
			for( int i=0; i<byday.length; i++ ) {
				if ( firstOne ) firstOne = false;
				else s2.append(", ");
				s2.append(byday[i].toPrettyString());
				if ( byday[i].isWeekDay() )
					weekdays++;
				else
					weekends++;
			}
			if ( weekdays == 5 && weekends == 0 ) 
				s.append(cx.getString(R.string.weekdays));
			else if ( weekends == 2 && weekdays == 0 )
				s.append(cx.getString(R.string.weekends));
			else
				s.append(s2.toString());
		}

		if ( bysetpos != null ) {
			s.insert(0, prettyListInts("the ", bysecond, " instances of ("));
			s.append(")");
		}

		if ( wkst.wDay != AcalDateTime.MONDAY ) {
			s.append(" with weeks starting on ");
			s.append(wkst.toPrettyString());
		}

		if ( count == INFINITE_REPEAT_COUNT ) {
			s.append(" forever.");
		}
		else {
			s.append(", ");
			s.append(Integer.toString(count+1));
			s.append(" times.");
		}
		if ( until != null ) {
			s.append("until ");
			s.append(until.fmtIcal());
		}
		return s.toString();
	}

	private String commaListInts( int[] intList ) {
		StringBuilder s = null;
		for( int thisInt : intList ) {
			if ( s == null ) {
				 s = new StringBuilder(Integer.toString(thisInt));
			}
			else {
				s.append(",");
				s.append(Integer.toString(thisInt));
			}
		}
		if ( s == null ) return "";
		return s.toString();
	}

	private String prettyListInts( String prefix, int[] intList, String postfix ) {
		StringBuilder s = null;
		
		for( int thisInt : intList ) {
			if ( s == null ) {
				s = new StringBuilder(prefix + Integer.toString(thisInt));
			}
			else {
				s.append(", ");
				s.append(Integer.toString(thisInt));
			}
		}
		if ( s == null ) return "";
		s.append(postfix);
		return s.toString();
	}

	/**
	 * <p>
	 * Like commaListInts(), except we use it for bymonth / bysetpos which are
	 * zero-based in Java.
	 * </p>
	 * @param intList
	 * @return
	 */
	private String commaListIntsPlusOne(int[] intList) {
		int[] myList = new int[intList.length];
		for( int i=0; i<intList.length; i++ ) {
			myList[i] = intList[i] + 1;	// Since Java months are 0-based.
		}
		return commaListInts(myList);
	}

	protected abstract void nextFrequency();
	public abstract List<AcalDateTime> buildSet();
	public abstract String getFrequencyName();
	public abstract String getPrettyFrequencyName(Context cx);

	public void resetBaseDate() {
		currentBase  = null;
		originalBase = null;
	}

	public void nextBaseDate( AcalDateTime c ) {
		if ( originalBase == null || currentBase == null ) {
			originalBase = c.clone();
			currentBase = c.clone();
			if ( until != null ) until.setTimeZone(originalBase.getTimeZoneId());
		}
		else {
			nextFrequency();
		}
	}


	protected void startNewSet() {
		currentSet = new ArrayList<AcalDateTime>();
		currentSet.add(currentBase.clone());
	}
	
	
	protected void expandByMonth() {
		if ( bymonth == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		Set<String> daySet = new HashSet<String>();
		for( AcalDateTime c : currentSet ) {
			for( int month :  bymonth ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				if ( n.setMonth(month) ) {
					// We don't want to multiply add days, so we use a Set to catch that
					if ( daySet.add(n.fmtIcal()) ) finalSet.add(n);
				}
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandByWeekNo() {
		if ( byweekno == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		Set<String> daySet = new HashSet<String>();
		for( AcalDateTime c : currentSet ) {
			c.setWeekStart(wkst.wDay);
			for( int weekno :  byweekno ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				if ( n.setYearWeek((short) weekno) ) {
					// We don't want to multiply add days, so we use a Set to catch that
					if ( daySet.add(n.fmtIcal()) ) finalSet.add(n);
				}
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandByYearDay() {
		// TODO this should handle negative year days, but I think it probably doesn't at present...
		if ( byyearday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		Set<String> daySet = new HashSet<String>();
		for( AcalDateTime c : currentSet ) {
			for( int yearday :  byyearday ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				if ( n.setYearDay(yearday) ) {
					// We don't want to multiply add days, so we use a Set to catch that
					if ( daySet.add(n.fmtIcal()) ) finalSet.add(n);
				}
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandByMonthDay( int[] monthdayset) {
		if ( monthdayset == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		Set<String> daySet = new HashSet<String>();
		for( AcalDateTime c : currentSet ) {
			for( int monthday :  monthdayset ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				if ( n.setMonthDay(monthday) ) {
					// We don't want to multiply add days, so we use a Set to catch that
					if ( daySet.add(n.fmtIcal()) ) finalSet.add(n);
				}
			}
		}
		currentSet = finalSet;
	}
	

	protected void expandByMonthDay() {
		expandByMonthDay(bymonthday);
	}
	

	protected void expandByDay() {
		if ( byday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		Set<String> daySet = new HashSet<String>();
		for( AcalDateTime c : currentSet ) {
			c.setWeekStart(wkst.wDay);
			for( AcalRepeatRuleDay day :  byday ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				if ( n.setWeekDay(day.wDay) ) {
					// We don't want to multiply add days, so we use a Set to catch that
					if ( daySet.add(n.fmtIcal()) ) finalSet.add(n);
				}
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandByDayMonthly() {
		if ( byday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		AcalDateTime firstOfMonth = null;
		AcalDateTime lastOfMonth = null;
		int dowOfFirst;
		int dowOfLast = 0;
		for( AcalDateTime c : currentSet ) {
			c.setMonthDay(1);
			if ( firstOfMonth != null && firstOfMonth.equals(c)) continue;
			firstOfMonth = c;
			dowOfFirst = c.getWeekDay();
			boolean[] inMonth = new boolean[c.getActualMaximum(AcalDateTime.DAY_OF_MONTH)];
			for( AcalRepeatRuleDay day :  byday ) {
				if ( day.setPos == 0 ) {
					// Like all Wednesdays in month
					for( int d = day.wDay - dowOfFirst; d < inMonth.length; d += 7 ) {
						if ( d < 0 ) continue;
						inMonth[d] = true;
					}
				}
				else if ( day.setPos > 0 ){
					// Like 3rd Tuesday
					int d = day.wDay - dowOfFirst;
					if ( d < 0 ) d+= 7;
					d += (7 * (day.setPos - 1));
					if ( d < inMonth.length ) inMonth[d] = true;  
				}
				else {
					// Like 2nd to last Monday
					if ( lastOfMonth == null ) {
						c.setMonthDay(inMonth.length);
						dowOfLast = c.getWeekDay();
					}
					// dowOfLast == Monday (1)
					// wDay = Wednesday(3)
					// daysInMonth = 30
					// d = 2
					int d = (inMonth.length - dowOfLast) + day.wDay;
					d--; // Hack to deal with 0-based array offset
					if ( d >= inMonth.length ) d -= 7;
					d += (7 * (day.setPos + 1));  // Note that day.setPos *IS* negative	at this point
					if ( d >= 0 ) inMonth[d] = true;  
				}
			}
			for( int i=0; i<inMonth.length; i++ ) {
				if ( inMonth[i] ) {
					c.setMonthDay(i+1);
					finalSet.add((AcalDateTime) c.clone());
				}
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandByHour() {
		if ( byday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( AcalDateTime c : currentSet ) {
			for( int h :  byhour ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				n.set(AcalDateTime.HOUR, h);
				finalSet.add(n);
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandByMinute() {
		if ( byday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( AcalDateTime c : currentSet ) {
			for( int m :  byminute ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				n.set(AcalDateTime.MINUTE, m);
				finalSet.add(n);
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void expandBySecond() {
		if ( byday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( AcalDateTime c : currentSet ) {
			for( int s :  bysecond ) {
				AcalDateTime n = (AcalDateTime) c.clone();
				n.set(AcalDateTime.SECOND, s);
				finalSet.add(n);
			}
		}
		currentSet = finalSet;
	}
	
	
	protected void limitBySetPos() {
		if ( bysetpos == null ) return;
		Collections.sort(currentSet, new AcalDateTime.AcalDateTimeSorter());
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( int pos : bysetpos ) {
			if ( pos < 0 )
				finalSet.add(currentSet.get(currentSet.size() + pos ));
			else	
				finalSet.add(currentSet.get(pos));
		}
		currentSet = finalSet;
	}

	
	protected void limitByMonth() {
		if ( bymonth == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( int month : bymonth ) {
			for( AcalDateTime c : currentSet ) {
				if ( c.get(AcalDateTime.MONTH) == month ) finalSet.add(c);
			}
		}
		currentSet = finalSet;
	}

	
	protected void limitByMonthDay() {
		if ( bymonthday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( int monthday : bymonthday ) {
			for( AcalDateTime c : currentSet ) {
				if ( c.get(AcalDateTime.DAY_OF_MONTH) == monthday ) finalSet.add(c);
			}
		}
		currentSet = finalSet;
	}

	
	protected void limitByDay() {
		if ( byday == null ) return;
		List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
		for( AcalRepeatRuleDay day : byday ) {
			for( AcalDateTime c : currentSet ) {
				if ( c.get(AcalDateTime.DAY_OF_WEEK) == day.wDay ) finalSet.add(c);
			}
		}
		currentSet = finalSet;
	}

	
	private static int[] parseIntArray( String[] strValues ) {
		// Skips the first element in the input array, in case you want to re-use this code...
		int[] ret = new int[strValues.length-1];
		for ( int i=1,j=0; j<ret.length; i++,j++ ) {
			try {
				ret[j] = Integer.parseInt(strValues[i]);
			}
			catch (Exception e) {
				ret[j] = 0;
			}
		}
		return ret;
	}

	/**
	 * <p>
	 * Like parseIntArray, but for zero-based arrays like bymonth and bysetpos. We only shift positive
	 * values though, since bysetpos can use negative ones.
	 * </p>
	 * @param strValues
	 * @return
	 */
	private static int[] parseIntArrayMinusOne( String[] strValues ) {
		int[] ret = parseIntArray( strValues ); 
		for( int i=0; i<ret.length; i++ ) {
			if ( ret[i] > 0 ) ret[i]--;
		}
		return ret;
	}
	

	protected void debugCurrentSet( String whereAmI ) {
		if ( !Constants.LOG_VERBOSE ) return; 
		if ( currentSet.isEmpty() ) {
			Log.v(TAG, "Current set "+whereAmI+" is empty" );
			return;
		}
		debugDates = new String[currentSet.size()];
		String dateList = "";
		for( int i=0; i<debugDates.length; i++ ) {
			debugDates[i] = currentSet.get(i).fmtIcal();
			if ( i > 0 ) dateList += ", ";
			dateList += debugDates[i]; 
		}
		Log.v(TAG, whereAmI+" is: "+dateList + " -- " + this.toString() );
	}

	
	/**
	 * <p>
	 * Internal enum just used to make the parser more readable really.
	 * </p>
	 * @author Morphoss Ltd
	 */
	private static enum PartType	{
		BYMONTH, BYWEEKNO, BYYEARDAY, BYMONTHDAY, BYDAY, BYHOUR, BYMINUTE, BYSECOND, BYSETPOS,
		FREQ, UNTIL, COUNT, INTERVAL, WKST;


		/**
		 * <p>
		 * Convert a string into our enum type, with a minimum of tests.  While this approach will misrecognise some
		 * peculiar strings as valid that won't do us any harm, so long as we recognise valid strings correctly.
		 * </p>
		 * 
		 * @param stFreq
		 * @return
		 */
		public static PartType fromString( String stFreq ) {
			if ( stFreq.length() < 4 ) return null;
			switch( stFreq.charAt(0) ) {
				case 'F':	return FREQ;
				case 'I':	return INTERVAL;
				case 'U':	return UNTIL;
				case 'C':	return COUNT;
				case 'W':	return WKST;
				case 'B':
					if ( stFreq.length() < 5 ) return null;
					switch( stFreq.charAt(2) ) {
						case 'D':	return BYDAY;
						case 'Y':	return BYYEARDAY;
						case 'W':	return BYWEEKNO;
						case 'H':	return BYHOUR;
						case 'M':
							switch( stFreq.length() ) {
								case 7:		return BYMONTH;
								case 10:	return BYMONTHDAY;
								case 8:		return BYMINUTE;
							}
						case 'S':
							if ( stFreq.charAt(4) == 'T' )		return BYSETPOS;
							else								return BYSECOND;
					}
					break;
			}
			return null;
		}
	};
}
