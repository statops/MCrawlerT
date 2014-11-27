/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.acaltime;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.HashCodeUtil;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.davacal.AcalProperty;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.davacal.VCalendar;

/**
 * <h1>AcalDateTime</h1>
 * <p>
 * This is a class for handling dates, with times, possibly with timezones.
 * </p>
 * <p>
 * What! I hear you say: "ANOTHER date time class!!!" Er: yes.  You see the Date and/or
 * Calendar classes in Java bring along with them a large amount of baggage.  In many
 * cases this baggage is mighty useful, but for us it was causing problems, in particular
 * it was causing problems with certainty around exactly when midnight occurred with some
 * weird millisecond unreliability.  The Java classes also aren't exactly keen on letting
 * one control exactly when you care about timezones, and they don't seem to want to be
 * helpful in allowing floating times.  Bugger.  And believe me: I tried. 
 * </p>
 * <p>
 * This class does include support for leap seconds, for setting the week start day and
 * calculating week numbers according to ISO8601.  The getMillis() and setMillis() functions
 * are designed to round-trip through Java's native classes which do not support leap seconds,
 * so the setEpoch()/getEpoch() methods are not *Millis/1000. Months are <em>not</em> zero-
 * based here, so '1' is January.  It seems likely that this is more memory-efficient than
 * the native code, but while I've endeavoured to make it fast my lack of knowledge of
 * Java may well have screwed that up.
 * </p>
 * <p>
 * This class <em>does not</em> attempt to deal with iCalendar-style timezones.  It assumes
 * that events will have timezone names which will contain a recognisable Olson substring
 * name and uses the Java timezone as a base.  We might have to revisit this at some future
 * point to attempt deeper recognition of timezone information, but it's a good first cut. 
 * </p>
 * <p>
 * Enjoy!
 * </p>
 * 
 * @author Morphoss Ltd
 *
 */
public class AcalDateTime implements Parcelable, Serializable, Cloneable, Comparable<AcalDateTime> {

	private static final String			TAG							= "AcalDateTime";
	private static final long			serialVersionUID			= 1L;

	private static final Pattern isoDatePattern = Pattern.compile(
				"^((?:[123]\\d)?\\d{2})" +	// 1 = year
				"-?(0[1-9]|1[0-2])" +		// 2 = month
				"-?([0-3]\\d)" +			// 3 = day
				"(?:[T ]" +
					"([0-2]\\d)" +			// 4 = hour
					":?([0-5]\\d)" +		// 5 = minute
					":?([0-6]\\d)" +		// 6 = second
					"(Z)? *" +				// 7 = UTC indicator
					"([aApP]\\.?[mM])?" +	// 8 = am/pm indicator
				")?" +
				"(...(?::\\d\\d|.)?|" +					// 9 = non-Olson timezone
					"((?:Antarctica|America|Africa|Atlantic|Asia|Australia|Indian|Europe|Pacific|US)/(?:(?:[^/]+)/)?[^/]+)" +
				")?" 						// 10 = Olson timezone
			);

	public static final String			UTC_NAME			= "UTC";
	public static final TimeZone		UTC					= TimeZone.getTimeZone(UTC_NAME);

	public static final int				SECONDS_IN_DAY		= 86400;
	public static final int				SECONDS_IN_HOUR		= 3600;
	public static final int				SECONDS_IN_MINUTE	= 60;
	public static final int				DAYS_IN_YEAR		= 365;
	

	public static final short			MIN_YEAR_VALUE		= 1582;
	public static final short			MAX_YEAR_VALUE		= 32766;
	
	public static final long			MAX_EPOCH_VALUE		= (long) ((long) (MAX_YEAR_VALUE - 1971L) * SECONDS_IN_DAY * DAYS_IN_YEAR);
	public static final long			MIN_EPOCH_VALUE		= (long) ((long) (MIN_YEAR_VALUE - 1971L) * SECONDS_IN_DAY * DAYS_IN_YEAR);

	public static final AcalDateTime	MIN					= new AcalDateTime(MIN_YEAR_VALUE, 1, 1, 0, 0, 0, null);
	public static final AcalDateTime	MAX					= new AcalDateTime(MAX_YEAR_VALUE, 12, 31, 23, 59, 59, null);

	public static final short			MONDAY				= 0;
	public static final short			TUESDAY				= 1;
	public static final short			WEDNESDAY			= 2;
	public static final short			THURSDAY			= 3;
	public static final short			FRIDAY				= 4;
	public static final short			SATURDAY			= 5;
	public static final short			SUNDAY				= 6;

	public static final short			JANUARY				= 1;
	public static final short			FEBRUARY			= 2;
	public static final short			MARCH				= 3;
	public static final short			APRIL				= 4;
	public static final short			MAY					= 5;
	public static final short			JUNE				= 6;
	public static final short			JULY				= 7;
	public static final short			AUGUST				= 8;
	public static final short			SEPTEMBER			= 9;
	public static final short			OCTOBER				= 10;
	public static final short			NOVEMBER			= 11;
	public static final short			DECEMBER			= 12;
	
	protected static final short YEAR_NOT_SET = Short.MIN_VALUE;
	protected short year = YEAR_NOT_SET;
	protected short month;
	protected short day;
	protected short hour = 0;
	protected short minute = 0;
	protected short second = 0;
	
	protected short weekStart = 0;
	protected boolean isDate = false;

	protected static final long EPOCH_NOT_SET = Long.MIN_VALUE;
	protected long epoch = EPOCH_NOT_SET;
	
	protected TimeZone tz = null;
	protected String tzName = null;


	/**
	 * <p>
	 * Construct a new AcalDateTime which will be floating, but with the current 'clock' time
	 * in the current timezone.  This will mean that you should call the setTimeZone() method
	 * to anchor this to a timezone.
	 * </p> 
	 */
	public AcalDateTime() {
		epoch = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 1000;
		if ( Constants.debugDateTime ) checkEpoch();
	}


	/**
	 * <p>
	 * Return a floating time which will represent the specified milliseconds from Epoch.  This
	 * will mean that you should call the shiftTimeZone() method to anchor this to a timezone.
	 * </p> 
	 * @param millisecondsSinceEpoch
	 * @return
	 */
	public static AcalDateTime fromMillis(long millisecondsSinceEpoch) {
		AcalDateTime ret = new AcalDateTime();
		ret.epoch = millisecondsSinceEpoch / 1000;
		if ( Constants.debugDateTime ) ret.checkEpoch();
		return ret;
	}

	
	/**
	 * <p>
	 * Return a localised time which will represent the specified milliseconds from Epoch.
	 * </p> 
	 * @param millisecondsSinceEpoch
	 * @return
	 */
	public static AcalDateTime localTimeFromMillis(long millisecondsSinceEpoch, boolean fromFloating) {
		AcalDateTime ret = fromMillis(millisecondsSinceEpoch);
		if ( fromFloating ) {
			ret.tz = UTC;
			ret.tzName = UTC_NAME;
			ret.setTimeZone(TimeZone.getDefault().getID());
		}
		else
			ret.shiftTimeZone(TimeZone.getDefault().getID());
		return ret;
	}

	
	/**
	 * 
	 * @param yy Between 1582 and 32767
	 * @param mm Between 1 and 12
	 * @param dd Between 1 and the number of days in the month
	 * @param hh Between 0 and 23
	 * @param minute Between 0 and 59
	 * @param second Between 0 and 61 (to allow for possible leap second times)
	 * @param tz
	 */
	public AcalDateTime( int yy, int mm, int dd, int hh, int minute, int second, String tzName ) {
		if ( yy < MIN_YEAR_VALUE || yy > MAX_YEAR_VALUE ) throw new IllegalArgumentException();
		year = (short) yy;
		if ( mm < 1 || mm > 12 ) throw new IllegalArgumentException();
		month = (short) mm;
		if ( dd < 1  || dd > monthDays(yy,mm) ) throw new IllegalArgumentException();
		day = (short) dd;
		if ( hh < 0 || hh > 23 )
			throw new IllegalArgumentException();
		hour = (short) hh;
		if ( minute < 0 || minute > 59 )
			throw new IllegalArgumentException();
		this.minute = (short) minute;
		if ( second < 0 || second > 59 ) throw new IllegalArgumentException();
		this.second = (short) second;
		
		if ( tzName != null ) {
			tz = TimeZone.getTimeZone(tzName);
			if ( tz != null ) this.tzName = tzName; 
		}
		epoch = EPOCH_NOT_SET;
	}

	/**
	 * Construct from an AcalProperty Object, returning null if it is invalid in some way.
	 */
	public static AcalDateTime fromAcalProperty(AcalProperty prop) {
		if ( prop == null ) return null;
		try {
			return AcalDateTime.fromIcalendar(prop.getValue(),prop.getParam(AcalProperty.PARAM_VALUE),prop.getParam(AcalProperty.PARAM_TZID));
		}
		catch ( NullPointerException e ) {}
		catch ( IllegalArgumentException e ) {}
		return null;
	}

	/**
	 * Returns the number of days in a year/month pair
	 * @param year
	 * @param month
	 * @return the number of days in the month
	 */
	public static int monthDays(int year, int month) {
		if (month == 4 || month == 6 || month == 9 || month == 11) return 30;
		else if (month != 2) return 31;
		return 28 + leapDay(year);
	}


	/**
	 * Returns a 1 if this year is a leap year, otherwise a 0
	 * @param year
	 * @return 1 if this is a leap year, 0 otherwise
	 */
	private static int leapDay(int year) {
		if ( (year % 4) == 0 && ((year % 100) != 0 || (year % 400) == 0) ) return 1;
		return 0;
	}


	/**
	 * <p>
	 * Returns a count of the number of leap days between epoch and January 1st of this 
	 * year. Years in the range 1800 to 59999 should be OK, or beyond that to some extent,
	 * but we don't, frankly, give a damn outside that range.
	 * </p>
	 * @param year
	 * @return The number of leap days between 1970-01-01 and January 1st of the specified year.
	 */
	private static int epochLeapDays(int year) {
		if ( year < 1972 ) {
			year = 1999 - year;
			return 7 - ((year / 4) - (year/100) + (year/400));
		}
		year -= 1969;
		if ( year < 130 ) return (year / 4);

		year -= 32;
		return 8 + ((year / 4) - (year / 100) + (year / 400));
	}


	/**
	 * <p>
	 * Construct an AcalDate from a string.  This will only handle parsing an ISO format string
	 * at present, which includes parsing an iCalendar DATE-TIIME or DATE string as they are subsets
	 * of the ISO format.
	 * </p>
	 * <p>
	 * Strings that we can handle look like this kind of thing:
	 * </p>
	 * <ul>
	 * <li>2001-12-15 14:23:15+13:00</li>
	 * <li>20011215T012315Z</li>
	 * <li>2001-12-15</li>
	 * <li>2001-12-15 14:23:15 Pacific/Auckland</li>
	 * <li>20011215</li>
	 * </ul>
	 * 
	 * @param datestring
	 * @return A new AcalDate object.
	 * @throws IllegalArgumentException
	 */
	public static AcalDateTime fromString(String dateString) throws IllegalArgumentException {
		if ( dateString == null )
			throw new IllegalArgumentException("Date may not be null.");

		Matcher m = isoDatePattern.matcher(dateString);
		if ( ! m.matches() ) {
			throw new IllegalArgumentException("Date '" + dateString + "' is not in a recognised format.");
		}

		AcalDateTime newDateTime = null;
		
		int year = Integer.parseInt(m.group(1));
		int month = Integer.parseInt(m.group(2));  
		int day = Integer.parseInt(m.group(3));
		int hour = 0;
		int minute = 0;
		int second = 0;
		
		if ( m.group(4) != null && !m.group(4).equals("") ) {
			hour = Integer.parseInt(m.group(4));
			minute = Integer.parseInt(m.group(5));
			second = Integer.parseInt(m.group(6));
			if ( m.group(8) != null && m.group(8).equalsIgnoreCase("p") ) hour += 12;
			newDateTime = new AcalDateTime(year, month, day, hour, minute, second, null);
			if (m.group(7) != null && m.group(7).equals("Z") ) {
				newDateTime.tz = UTC;
				newDateTime.tzName = UTC_NAME;
			}
			else if (m.group(10) != null && !m.group(10).equals("") ) {
				newDateTime.overwriteTimeZone(m.group(10));
			}
			else if ( m.group(9) != null && m.group(9).equals("") ) {
				// This is unlikely to be even close to working, since for this
				// format we only got an offset, and all we could really do is guess
				// what that might mean, in any case.
				newDateTime.overwriteTimeZone(m.group(9));
			}
		}
		else if (m.matches() ) {
			newDateTime = new AcalDateTime(year, month, day, 0, 0, 0, null);
			newDateTime.isDate = true;
		}
		if ( Constants.debugDateTime ) newDateTime.checkEpoch();
		return newDateTime;
	}


	/**
	 * Simply write the named timezone to the current object making no attempt to adjust
	 * the validity of the current date / time information. 
	 * @param tzName
	 */
	private void overwriteTimeZone( String newTzName ) {
		if ( newTzName == null || newTzName.equals("")) {
			tzName = null;
			tz = null;
			return;
		}
		tzName = VCalendar.staticGetOlsonName(newTzName);
		tz = TimeZone.getTimeZone(tzName);
		if ( tz == null ) tzName = null;
	}

	/**
	 * hashCode for Serializable support.
	 */
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash( result, this.tzName );
	    result = HashCodeUtil.hash( result, this.getEpoch() );
	    return result;
	}


	/**
	 * <p>
	 * Parse an input in RFC5545 format represented as an AcalProperty object and
	 * return a date localised to any TZID supplied, and expanded to a midnight time for a VALUE=DATE parameter.
	 * </p>
	 * <p>
	 * It is expected that this will be called something like:<br/>
	 * <pre>
	 * new AcalDateTime = AcalDateTime.fromProperty( dateProperty.Value(), dateProperty.getParam(AcalProperty.PARAM_VALUE), dateProperty.getParam(AcalProperty.PARAM_TZID) );
	 * </pre>
	 * It's fine for either of the second two to be null.  The first one can also be null, but you'll
	 * just get a null back in that case.
	 * </p>
	 * 
	 * @param dateString  - The content of an iCalendar DATE-TIME property
	 * @param isDateParam - the VALUE parameter from an iCalendar DATE-TIME value
	 * @param tzIdParam   - the TZID parameter from an iCalendar DATE-TIME value
	 * @return A shiny new AcalDateTime
	 */
	public static AcalDateTime fromIcalendar(String dateString, String isDateParam, String tzIdParam ) {

		if ( dateString == null || dateString.equals("") ) return null;

		AcalDateTime result = fromString( dateString );

		if ( isDateParam != null ) result.isDate = isDateParam.equalsIgnoreCase("DATE");
		if ( tzIdParam != null ) result.overwriteTimeZone(tzIdParam);
		if ( Constants.debugDateTime ) result.checkEpoch();
		return result;
	}


	/**
	 * <p>
	 * Rerurn the year of this date, sometime after 1582.
	 * </p>
	 * @return year
	 */
	public short getYear() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return year;
	}


	/**
	 * <p>
	 * Try to set the year for this date.  If the resulting date would be invalid
	 * then return false and don't change the date.
	 * </p>
	 * @param yy
	 * @return true if a legal date would result, and has been set.
	 */
	public synchronized boolean setYear( int yy) {
		if ( yy < MIN_YEAR_VALUE || yy > MAX_YEAR_VALUE ) throw new IllegalArgumentException();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( day > monthDays(yy,month) ) return false;
		if ( yy == year ) return true;
		year = (short) yy;
		epoch = EPOCH_NOT_SET;
		return true;
	}

	/**
	 * <p>
	 * Rerurn the month of this date, 1 to 12
	 * </p>
	 * @return month
	 */
	public short getMonth() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return month;
	}


	/**
	 * <p>
	 * Try to set the month for this date.  If the resulting date would be invalid
	 * then return false and don't change the date.
	 * </p>
	 * @param mm
	 * @return true if a legal date would result, and has been set.
	 */
	public synchronized boolean setMonth( int mm) {
		if ( mm < 1 || mm > 12 ) throw new IllegalArgumentException();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( mm == month ) return true;
		if ( day > monthDays(year,mm) ) return false;
		month = (short) mm;
		epoch = EPOCH_NOT_SET;
		return true;
	}
	

	/**
	 * <p>
	 * Rerurn the day in month of this date, sometime after 1582.
	 * </p>
	 * @return year
	 */
	public short getMonthDay() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return day;
	}


	/**
	 * <p>
	 * Get the week of the month.  Much simpler than the calculation of the week of the year,
	 * since we want to use this for things like 1st tuesday, 3rd thursday, etc.
	 * </p>
	 * <p>For example, the 1st-7th are the first week, the 8th-14th are the second, etc.</p>
	 * @return
	 */
	public short getMonthWeek() {
		return (short) (1 + ((getMonthDay() - 1) / 7));
	}


	/**
	 * <p>
	 * Try to set the day of the month for this date.  If the resulting date would be invalid
	 * then return false and don't change the date.  The day of month may be negative, in which
	 * case it will be counted backwards with -1 being the last day of the month.
	 * </p>
	 * @param monthDay
	 * @return true if a legal date would result, and has been set.
	 */
	public synchronized boolean setMonthDay( int newDay ) {
		if ( newDay == 0 || newDay < -31 || newDay > 31 ) throw new IllegalArgumentException();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( newDay < 0 ) newDay += 1 + monthDays(year,month); // backwards from end of month
		if ( newDay > monthDays(year,month) ) return false;
		if ( day == newDay ) return true;
		day = (short) newDay;
		epoch = EPOCH_NOT_SET;
		return true;
	}


	/**
	 * <p>Set the year, month and day of this AcalDateTime.</p>
	 * <p>If the day is invalid for the month in question it will be coerced to the maximum
	 * for that actual month (i.e. 31st of Feb will be coerced to 29th or 28th depending on the
	 * year, 31st April will become 30th, etc.)</p>
	 * <p>If you want the adjustment to fail you should call setMonthDay() instead, which also
	 * handles setting negative days as offsets from the end of the month, and this method does not.</p>
	 * @param newYear The year to set
	 * @param newMonth The month to set, from 1 to 12
	 * @param newDay The day to try and set, from 1 to 31
	 * @return this, for chaining.
	 */
	public synchronized AcalDateTime setYearMonthDay(int newYear, int newMonth, int newDay) {
		if ( newYear < MIN_YEAR_VALUE || newYear > MAX_YEAR_VALUE ) throw new IllegalArgumentException("Year must be between "+MIN_YEAR_VALUE +" and "+ MAX_YEAR_VALUE);
		if ( newMonth < 1 || newMonth > 12 ) throw new IllegalArgumentException("Month must be from 1 to 12");
		if ( newDay < 1 || newDay > 31 ) throw new IllegalArgumentException("Day must be from 1 to 31");
		if ( newDay > monthDays(year,month) ) newDay = monthDays(year,month);
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		year = (short) newYear;
		month = (short) newMonth;
		day = (short) newDay;
		epoch = EPOCH_NOT_SET;
		return this;
	}

	
	/**
	 * <p>
	 * Returns the day of year.  January the first is 1
	 * </p>
	 * @return the day of year.
	 */
	public short getYearDay() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		final int[] daysBeforeMonth = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
		return (short) (day + (month > 2 ? leapDay(year) : 0) + daysBeforeMonth[month-1]);
	}


	/**
	 * <p>
	 * Try to set the day of the year for this date.  If the resulting date would be invalid
	 * then return false and don't change the date.  The day of year may be negative, in which
	 * case it will be counted backwards with -1 being the last day of the year.
	 * </p>
	 * @param yearDay
	 * @return true if a legal date would result, and has been set.
	 */
	public synchronized boolean setYearDay( int yearDay ) {
		if ( yearDay == 0 || yearDay < -366 || yearDay > 366 ) throw new IllegalArgumentException();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( month >= JANUARY && month <= DECEMBER && yearDay == getYearDay() ) return true;
		int daysInYear = DAYS_IN_YEAR + leapDay(year);
		if ( yearDay < 0 ) {
//			if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//				Log.v(TAG,"Got negative yearDay " + yearDay + " Will use: " + (yearDay + daysInYear + 1) );
			yearDay += daysInYear;
			yearDay++;
		}
		if ( yearDay < 1 || yearDay > daysInYear ) return false;
		
		privateSetYearDay((short) yearDay, (short) daysInYear);

		epoch = EPOCH_NOT_SET;

		return true;
	}


	private void privateSetYearDay( short yearDay, short daysInYear ) {
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"Setting year day for " + yearDay + " with " + daysInYear + " days in year " + year );
		if ( yearDay < 60 ) {
			if ( yearDay < 32 ) {
				month =  JANUARY;
				day = yearDay;
			}
			else {
				month =  FEBRUARY;
				day = (short) (yearDay - 31); 
			}
		}  
		else {
			// Others are fixed relative to the end of the year
			// Approximate binary nesting of if will minimise # of comparisons
			// The really efficient way to code this would be an array of what month each day of the 
			yearDay = (short) (daysInYear - yearDay);
			if ( yearDay < 184 ) { 
				if ( yearDay <  92 ) { 
					if 		( yearDay <  31 )	{ month = DECEMBER;   day = (short) ( 31 - yearDay); } 
					else if ( yearDay <  61 )	{ month = NOVEMBER;   day = (short) ( 61 - yearDay); } 
					else						{ month = OCTOBER;    day = (short) ( 92 - yearDay); }
				}
				else {
					if 		( yearDay < 122 )	{ month =  SEPTEMBER; day = (short) (122 - yearDay); } 
					else if ( yearDay < 153 )	{ month =  AUGUST;    day = (short) (153 - yearDay); } 
					else 						{ month =  JULY;      day = (short) (184 - yearDay); }
				}
			}
			else {
				if ( yearDay < 275 ) {
					if 		( yearDay < 214 )	{ month =  JUNE;      day = (short) (214 - yearDay); } 
					else if ( yearDay < 245 )	{ month =  MAY;       day = (short) (245 - yearDay); } 
					else						{ month =  APRIL;     day = (short) (275 - yearDay); }
				}
				else {
					if ( yearDay < 306 )		{ month =  MARCH;     day = (short) (306 - yearDay); } 
					else 						{ month =  FEBRUARY;  day = 29; }
				}
			}
		}
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"Got " + year + "-" + month + "-" + day );
	}

	
	/**
	 * Get the day as numbers of days since Jan 1st 1970
	 * @return
	 */
	public long getEpochDay() {
		if ( epoch != EPOCH_NOT_SET ) {
			long offset = (tz != null ? tz.getOffset(epoch*1000) / 1000 : 0);
			return (long) Math.floor((epoch+offset) / SECONDS_IN_DAY);
		}
		
		// Otherwise work it out from the date fields.
		return (long) (
							((year - 1970) * DAYS_IN_YEAR)
							+ epochLeapDays(year)
							+ ( getYearDay() - 1 )
						);
	}


	/**
	 * Set the day as numbers of days since Jan 1st 1970
	 * @return
	 */
	public synchronized void setEpochDay( long newEpochDay ) {
		if ( Constants.debugDateTime ) checkEpoch();
		if ( epoch == EPOCH_NOT_SET ) calculateEpoch();
		epoch = (newEpochDay * SECONDS_IN_DAY) + getDaySecond();
		year = YEAR_NOT_SET;
	}


	/**
	 * Get the day of week.
	 * @return
	 */
	public short getWeekDay() {
		return (short) ((getEpochDay() - 4) % 7);
	}


	/**
	 * <p>
	 * Set the day of week, taking the weekStart into account.  In some circumstances the
	 * generated date might be invalid (monday before 1st jan, sunday after 31st december)
	 * and in that case the method will return false.
	 * </p>
	 * @param weekDay
	 * @return true If a valid day of week was found within the year
	 */
	public boolean setWeekDay( short weekDay ) {
		if ( weekDay < MONDAY || weekDay > SUNDAY ) throw new IllegalArgumentException();
		short firstOfWeek = (short) (getYearDay() - ((getWeekDay() + 7 - weekStart) % 7));
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"First day of week is " + firstOfWeek + " for year day: " + getYearDay() + " which is weekDay " + getWeekDay() );
		short newDay = (short) (firstOfWeek + ((weekDay + 7 - weekStart) % 7));
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"New day of week is " + newDay + " for week day: " + weekDay + " and weekStart: " + weekStart );
		if ( newDay < 1 || newDay > 366 ) return false;
		if ( Constants.debugDateTime ) checkEpoch();
		return setYearDay( newDay ); 
	}


	/**
	 * <p>
	 * Get the week of the year.  The first week of the year is the one which contains at least
	 * four days.  There might be 53 weeks in a year, e.g. if it started on a saturday or sunday
	 * and finished on a monday or tuesday (or equivalent for non-Monday weekstart).
	 * </p>
	 * <p>
	 * This function will return 0 for a week of the year which does not contain the 4th day of the
	 * year but which does contain the first.  By specification that is the 52nd/53rd week of the
	 * previous year, but we have no way of indicating previous year except by that.  Similarly it
	 * will return 53 for that week of the year containing 31st December, which does not also
	 * contain the 28th.  Strictly, that would be week 1 of the following year.
	 * </p>
	 * <p>
	 * See RFC5545 section 3.3.10, page 41.
	 * </p>
	 * @return
	 */
	public short getYearWeek() {
		short firstOfNextWeek = (short) (7 + getYearDay() - ((getWeekDay() +(7 - weekStart)) % 7));
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG, "Getting week of " + year + "-" + month + "-" + day + " YearDay is " + getYearDay()
//					+ " first of next week is " + firstOfNextWeek );
		int weekOfYear = ((firstOfNextWeek / 7) + (((firstOfNextWeek) % 7) > 4 ? 1 : 0)); 
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG, "Got week of " + weekOfYear + " from " + Integer.toString(firstOfNextWeek / 7)
//						+ " and " + Integer.toString((firstOfNextWeek % 7) > 4 ? 1 : 0) );
		return (short) weekOfYear;
	}


	/**
	 * <p>
	 * Set the week number, from 0 to 53, where 0 is a week containing 1st January but not containing
	 * the 4th.  
	 * </p>
	 * @see getYearWeek
	 * @param weekNo
	 * @return
	 */
	public boolean setYearWeek( short weekNo ) {
		if ( weekNo < 0 || weekNo > 53 ) throw new IllegalArgumentException();
		int newDay = getYearDay() + ((weekNo - getYearWeek()) * 7);
		if ( newDay < 1 ) return false;
		return setYearDay( newDay ); 
	}

	
	/**
	 * <p>
	 * Rerurn the hour of this date.
	 * </p>
	 * @return hour
	 */
	public short getHour() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return hour;
	}


	/**
	 * <p>
	 * Try to set the hour for this date.  Will throw an exception if the hour is not valid.
	 * </p>
	 * @param newHour
	 * @return true.
	 */
	public synchronized AcalDateTime setHour( int newHour ) {
		if ( newHour < 0 || newHour > 23 ) throw new IllegalArgumentException();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( hour == newHour ) return this;
		if ( epoch != EPOCH_NOT_SET ) epoch += (newHour - hour) * SECONDS_IN_HOUR;
		hour = (short) newHour;
		if ( Constants.debugDateTime ) checkEpoch();
		return this;
	}


	/**
	 * <p>
	 * Rerurn the minute of this date.
	 * </p>
	 * @return minute
	 */
	public short getMinute() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return minute;
	}


	/**
	 * <p>
	 * Try to set the minute for this date.  Will throw an exception if the minute is not valid.
	 * </p>
	 * @param newMinute
	 * @return true.
	 */
	public synchronized AcalDateTime setMinute( int newMinute ) {
		if ( newMinute < 0 || newMinute > 59 ) throw new IllegalArgumentException();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( minute == newMinute ) return this;
		if ( epoch != EPOCH_NOT_SET ) epoch += (newMinute - minute) * SECONDS_IN_MINUTE;
		minute = (short) newMinute;
		if ( Constants.debugDateTime ) checkEpoch();
		return this;
	}


	/**
	 * <p>
	 * Rerurn the second of this date.
	 * </p>
	 * @return second
	 */
	public short getSecond() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return second;
	}


	/**
	 * <p>
	 * Try to set the second (within the hour) for this date.  Will throw an exception if the second
	 * is outside the range 0 - 60. Returns false if second is 60 but the resulting time was not a
	 * leap second.
	 * </p>
	 * @param newSecond
	 * @return true, unless you try and set a leap second that doesn't exist.
	 */
	public synchronized AcalDateTime setSecond( int newSecond ) {
		if ( newSecond < 0 || newSecond > 59 ) throw new IllegalArgumentException();
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		if ( second == newSecond ) return this;
		if ( epoch != EPOCH_NOT_SET ) epoch += newSecond - second;
		second = (short) newSecond;
		if ( Constants.debugDateTime ) checkEpoch();
		return this;
	}


	/**
	 * <p>
	 * Get the second for this date, from 0 to 86401.
	 * </p>
	 * @return 0 to 86401
	 */
	public int getDaySecond( ) {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return (hour * SECONDS_IN_HOUR) + (minute * 60) + second;
	}

	
	/**
	 * <p>
	 * Try to set the second for this date.  Will throw an exception if it's outside the
	 * range 0 - 86399.
	 * </p>
	 * @param newSecond
	 * @return this, for chaining
	 */
	public synchronized AcalDateTime setDaySecond( int newSecond ) {
		if ( Constants.debugDateTime ) checkEpoch();
		if ( newSecond < 0 || newSecond >= SECONDS_IN_DAY ) throw new IllegalArgumentException("Attempt to setDaySecond("+Integer.toString(newSecond)+")");
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		short newHour = (short) (newSecond / SECONDS_IN_HOUR);
		short newMinute = (short) ((newSecond % SECONDS_IN_HOUR) / 60);
		newSecond %= 60;
		if ( hour == newHour && minute == newMinute && second == newSecond ) return this;
		if ( epoch != EPOCH_NOT_SET ) {
			epoch += ((newHour - hour) * SECONDS_IN_HOUR)
					+ ((newMinute - minute) * SECONDS_IN_MINUTE)
					+ (newSecond - second);
		}
		hour = (short) newHour;
		minute = (short) newMinute;
		second = (short) newSecond;
		if ( Constants.debugDateTime ) checkEpoch();
		return this;
	}


	/**
	 * <p>
	 * Get the timezone, which may be null.
	 * </p>
	 * @return timezone object or null 
	 */
	public TimeZone getTimeZone() {
		return tz;
	}

	
	/**
	 * <p>
	 * Get the timezone ID, which may be null, but which is hopefully an Olson name
	 * </p>
	 * @return timezone name 
	 */
	public String getTimeZoneId() {
		if ( tz == null ) return null;
		return tzName;
	}

	
	/**
	 * <p>
	 * Set the timezone for this date, keeping the date & time constant.
	 * </p>
	 * @return this, for chaining.
	 */
	public synchronized AcalDateTime setTimeZone( String newTz ) {
		if ( tzName == newTz || (tzName != null && tzName.equals(newTz)) ) return this;
		if ( year == YEAR_NOT_SET ) calculateDateTime();  // Because we're going to invalidate the epoch...
		this.overwriteTimeZone(newTz);
		epoch = EPOCH_NOT_SET;
		return this;
	}

	
	/**
	 * <p>
	 * Set the timezone for this date, shifting the clock time to keep the UTC epoch constant.
	 * </p>
	 * @return this, for chaining.
	 */
	public synchronized AcalDateTime shiftTimeZone( String newTz ) {
		if ( tzName == newTz || (tzName != null && tzName.equals(newTz)) ) return this;
		if ( epoch == EPOCH_NOT_SET ) calculateEpoch();  // Because we're going to invalidate the date...
		this.overwriteTimeZone(newTz);
		year = YEAR_NOT_SET;
		if ( Constants.debugDateTime ) calculateDateTime();
		return this;
	}

	
	/**
	 * <p>
	 * Apply the local time to this DateTime.  If the time is currently floating we will
	 * use setTimeZone to keep the clock time constant, otherwise we will call shiftTimeZone
	 * to keep the epoch constant.
	 * </p>
	 * @return this, for chaining.
	 */
	public synchronized AcalDateTime applyLocalTimeZone() {
		String newTimeZone = TimeZone.getDefault().getID();
//		if ( Constants.LOG_VERBOSE && isFloating() ) { // && Constants.debugDateTime )
//			Log.println(Constants.LOGV,TAG,"Applying local ("+newTimeZone+") to date which is "+(tzName==null?"floating":tzName));
//			Log.w(TAG,Log.getStackTraceString(new Exception("convert from floating.")));
//		}
		if ( isFloating() )
			return setTimeZone(newTimeZone);
		else
			return shiftTimeZone(newTimeZone);
	}

	
	/**
	 * Get the flag that marks this as a date.
	 * @return whether this is marked as a date.
	 */
	public boolean isDate() {
		return isDate;
	}
	

	/**
	 * Set the flag that marks this as a date.  If it is marked as a date
	 * it will print as a date via fmtIcal() and toPropertyString() methods.
	 * 
	 * This also will throw away any 'seconds' value. The timezone information
	 * will be retained, though possibly that should be chucked also.
	 * 
	 * @return this, for chaining.
	 */
	public AcalDateTime setAsDate(boolean newValue) {
		if (newValue) setDaySecond(0);
		isDate = newValue;
		return this;
	}

	
	/**
	 * Returns a number of milliseconds from epoch.  Seconds really, since it will always
	 * be an exact multiple of 1000.  This function is less accurate than getEpoch, since it
	 * removes the leap seconds from the result for compatibility with the standard Java
	 * libraries.
	 * @return milliseconds since epoch, without leap seconds.
	 */
	public long getMillis() {
		return (getEpoch() * 1000L);
	}

	
	/**
	 * <p>
	 * Set the current time to the given milliSeconds from epoch.  Note that we round to
	 * the nearest second.  We assume that these milliseconds don't include leap seconds
	 * for comatibility with the standard Java libraries which know not of these things.
	 * </p>
	 * @param milliSeconds
	 * @return this, for chaining.
	 */
	public AcalDateTime setMillis( long milliSeconds ) {
		setEpoch( Math.round(milliSeconds / 1000.0) );
		return this;
	}

	
	/**
	 * Returns a number of seconds from epoch, including leap seconds (up to 2008-12-31)
	 * @return
	 */
	public long getEpoch() {
		if ( epoch == EPOCH_NOT_SET ) calculateEpoch();
		return epoch;
	}

	
	/**
	 * Set the current time to the given seconds from epoch, which we assume includes leap
	 * seconds.
	 * @param newEpoch, the new epoch time to set
	 * @return this, for chaining
	 */
	public synchronized AcalDateTime setEpoch(long newEpoch) {
		epoch = newEpoch;
		if ( epoch > MAX_EPOCH_VALUE ) epoch = MAX_EPOCH_VALUE; 
		if ( epoch < MIN_EPOCH_VALUE ) epoch = MIN_EPOCH_VALUE; 
		year = YEAR_NOT_SET;
		if ( Constants.debugDateTime ) calculateDateTime();
		return this;
	}


	
	public static final short YEAR = 1001;
	public static final short MONTH_OF_YEAR = 1002;
	public static final short MONTH = 1002;
	public static final short DAY_OF_MONTH = 1003;
	public static final short WEEK_OF_MONTH = 1004;
	public static final short DAY = 1005;
	public static final short DAY_OF_YEAR = 1006;
	public static final short DAY_OF_EPOCH = 1007;
	public static final short HOUR = 1020;
	public static final short MINUTE = 1021;
	public static final short SECOND = 1022;
	public static final short SECOND_OF_DAY = 1023;
	public static final short WEEK_OF_YEAR = 1030;
	public static final short DAY_OF_WEEK = 1031;
	private static final short NEED_EPOCH_SET = 1099; 
	public static final short EPOCH = 1101;

	/**
	 * <p>
	 * Get some field from the date.
	 * </p>
	 * @param whatToGet
	 * @return
	 */
	public int get(short whatToGet ) {
		if ( year == YEAR_NOT_SET  )
			calculateDateTime();
			
		switch( whatToGet ) {
			case YEAR: 			return year;
			case MONTH_OF_YEAR: return month;
			case DAY_OF_MONTH: 	return day;
			case WEEK_OF_MONTH: return getMonthWeek();
			case DAY_OF_YEAR: 	return getYearDay();
			case HOUR: 			return hour;
			case MINUTE: 		return minute;
			case SECOND: 		return second;
			case SECOND_OF_DAY:	return getDaySecond();
			case WEEK_OF_YEAR: 	return getYearWeek();
			case DAY_OF_WEEK: 	return getWeekDay();
		}
		throw new IllegalArgumentException();
	}
	

	public boolean set(short whatToSet, int setTo ) {
		if ( year == YEAR_NOT_SET && whatToSet < NEED_EPOCH_SET )
			calculateDateTime();
			
		switch( whatToSet ) {
			case YEAR: 			return setYear((int) setTo);
			case MONTH_OF_YEAR: return setMonth((int) setTo);
			case DAY_OF_MONTH: 	return setMonthDay((int) setTo);
			case DAY_OF_YEAR: 	return setYearDay((int) setTo);
			case HOUR: 			setHour((int) setTo); return true;
			case MINUTE: 		setMinute((int) setTo); return true;
			case SECOND: 		setSecond((int) setTo); return true;
			case SECOND_OF_DAY:	setDaySecond((int) setTo); return true;
			case WEEK_OF_YEAR: 	setYearWeek((short) setTo); return true;
			case DAY_OF_WEEK: 	return setWeekDay((short) setTo);
		}
		throw new IllegalArgumentException();
	}
	

	public int getActualMaximum(short whatToGet) {
		if ( year == YEAR_NOT_SET && whatToGet < NEED_EPOCH_SET )
			calculateDateTime();
		switch( whatToGet ) {
			case YEAR: 			return MAX_YEAR_VALUE;
			case MONTH_OF_YEAR: return 12;
			case DAY_OF_MONTH: 	return monthDays(year,month);
			case DAY_OF_YEAR: 	return DAYS_IN_YEAR + leapDay(year);
			case HOUR: 			return 23;
			case MINUTE: 		return 59;
			case SECOND: 		return 59;
			case SECOND_OF_DAY:	return SECONDS_IN_DAY;
			case DAY_OF_WEEK: 	return SUNDAY;
		}
		throw new IllegalArgumentException();
	}

	
	/**
	 * Calculates the internal epoch value, because we don't do that unless we need it.
	 */
	protected synchronized void calculateEpoch() {
		if ( year == YEAR_NOT_SET ) throw new IllegalStateException("Uninitialised object");
		epoch = (((year - 1970) * DAYS_IN_YEAR) + epochLeapDays(year) + ( getYearDay() - 1 )) * SECONDS_IN_DAY;
		epoch += (hour * SECONDS_IN_HOUR) + (minute * 60) + second;
		if ( tz == null ) return;
		long offset = tz.getOffset(this.getMillis()) / 1000;
		if ( offset == 0 ) return;
		epoch -= offset;
		if ( offset == tz.getOffset(this.getMillis()) / 1000 ) return;
		epoch += (offset - (tz.getOffset(this.getMillis()) / 1000));
	}

	
	final private static short TWENTY_YEARS = ((4 * DAYS_IN_YEAR) + 1) * 5;
	final private static short FORTY_YEARS = ((4 * DAYS_IN_YEAR) + 1) * 10;

	/**
	 * Calculates the date + time values on the basis of the epoch value. We're lazy though
	 * so we only calculate this if we have to.
	 */
	protected synchronized void calculateDateTime() {
		if ( epoch == EPOCH_NOT_SET ) throw new IllegalStateException("Uninitialised object");
		if ( epoch > MAX_EPOCH_VALUE ) epoch = MAX_EPOCH_VALUE; 
		if ( epoch < MIN_EPOCH_VALUE ) epoch = MIN_EPOCH_VALUE; 
		long nDays = (epoch / SECONDS_IN_DAY);
		long nSeconds = epoch % SECONDS_IN_DAY;
		
		if ( nSeconds < 0 ) {
			nSeconds += SECONDS_IN_DAY;
			nDays -= 1;
		}

		year = 1970;
		// TODO: Work out a quicker way.  We should be able to do this with
		// arithmetic directly
		short daysInYear = 0;
		if ( nDays > 0 ) {
			if ( nDays > FORTY_YEARS )  { nDays -= FORTY_YEARS;  year += 40; }
			if ( nDays > TWENTY_YEARS ) { nDays -= TWENTY_YEARS; year += 20; }
			
			while( nDays >= (daysInYear = (short) (DAYS_IN_YEAR + leapDay(year))) ) {
				nDays -= daysInYear;
				year++;
			}
		}
		else {
			if ( nDays < - FORTY_YEARS )  { nDays += FORTY_YEARS;  year -= 40; }
			if ( nDays < - TWENTY_YEARS ) { nDays += TWENTY_YEARS; year -= 20; }
			while( nDays < 0 ) {
				year--;
				daysInYear = (short) (DAYS_IN_YEAR + leapDay(year));
				nDays += daysInYear;
			}
		}

		hour = 0;
		minute = 0;
		second = 0;
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG, "Setting year days to " + (nDays + 1) + ", seconds to " + nSeconds );
		privateSetYearDay((short) ++nDays, daysInYear);

		hour   = (short) (nSeconds / SECONDS_IN_HOUR);
		minute = (short) ((nSeconds % SECONDS_IN_HOUR) / 60);
		second = (short) (nSeconds % 60);
		localiseToZone();
//		checkEpoch();
		if ( Constants.debugDateTime ) checkSanity();
	}

	
	private void checkEpoch() {
		if ( year != YEAR_NOT_SET && epoch != EPOCH_NOT_SET ) {
			long saveEpoch = epoch;
			calculateEpoch();
			if ( saveEpoch != epoch ) {
				try {
					throw new Exception();
				}
				catch (Exception e) {
					Log.w(TAG,"SUSPICIOUS: " + year + "-" + month + "-" + day
								+ " T " + hour + ":" + minute + ":" + second );
					Log.w(TAG, "Epoch "+saveEpoch+" did not equal calculated epoch "+epoch);
					Log.w(TAG, "Difference is "+(saveEpoch - epoch)+" - "
								+((saveEpoch - epoch)/86400) + " days, "
								+((saveEpoch - epoch)%86400) + " seconds " );
					Log.i(TAG,Log.getStackTraceString(e));
				}
			}
		}
	}
	
	private void checkSanity() {
		if ( year < 1950 || year > 2050
					|| month < 1 || month > 12
					|| day < 0 || day > 31
					|| hour < 0 || hour > 23
					|| minute < 0 || minute > 59
					|| second < 0 || second > 59
					) {
			Log.w(TAG,"SUSPICIOUS: " + year + "-" + month + "-" + day
						+ " T " + hour + ":" + minute + ":" + second );
			Log.i(TAG,Log.getStackTraceString(new Exception(this.toPropertyString(PropertyName.INVALID))));
		}
	}


	/**
	 * <p>
	 * Localises the date + time (which is assumed to actually represent some UTC value) to the
	 * currently set timezone.  Typically we need to do this after we've calculated the UTC date
	 * and time from the epoch.
	 * </p>
	 */
	private void localiseToZone() {
		if ( tz == null ) return;
		long offset = tz.getOffset(this.getMillis()) / 1000;
		if ( offset == 0 ) return;
		hour += (offset / SECONDS_IN_HOUR);
		minute += ((offset % SECONDS_IN_HOUR) / 60);
		second += (offset % 60);
		fixupTimeFields();
	}


	private void fixupTimeFields() {
		if ( second < 0 )		{ second += 60; minute--; }
		else if ( second > 59 )	{ second -= 60; minute++; }
		if ( minute < 0 )		{ minute += 60; hour--; }
		else if ( minute > 59 )	{ minute -= 60; hour++; }
		if ( hour < 0 )			{ hour += 24; day--; }
		else if ( hour > 23 )	{ hour -= 24; day++; }
		if ( day < 1 ) {
			while( day < 1 ) {
				month--;
				if ( month < 1 ) {
					year--;
					month += 12;
				}
				day += monthDays(year,month);
			}
		}
		else while ( day > monthDays(year,month) ) {
			day -= monthDays(year,month);
			month++;
			if ( month > 12 ) {
				month -= 12;
				year++;
			}
		}
	}
	

	/**
	 * <p>
	 * Format the output as an iCalendar date / date-time string, with a trailing 'Z' if the zone is
	 * UTC.  This happens a lot, so we're trying to be as fast as possible.
	 * </p>
	 * <p>
	 * Note that timezones other than UTC are not represented in this format, as they are expected
	 * to be included as a TZID parameter to the iCalendar property.
	 * </p>
	 * @return The string, e.g. 20110411T095030 or 20110410T215030Z
	 */
	public String fmtIcal() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();

		StringBuilder ret = new StringBuilder(Integer.toString(year));
		if ( month < 10 ) ret.append("0");
		ret.append(month);
		if ( day < 10 ) ret.append("0");
		ret.append(day);
		
		if ( isDate ) return ret.toString();
		
		ret.append("T");
		if ( hour < 10 ) ret.append("0");
		ret.append(hour);
		if ( minute < 10 ) ret.append("0");
		ret.append(minute);
		if ( second < 10 ) ret.append("0");
		ret.append(second);

		if ( tz != null && tzName != null && tzName.equals(UTC_NAME) ) ret.append('Z');

		return ret.toString();
	}

	
	/**
	 * <p>
	 * Returns an iCalendar property string for this DateTime value, given the name
	 * for the property.  For example if the 'name' is 'RECURRENCE-ID' we might get
	 * a string like:
	 * <pre>RECURRENCE-ID;VALUE=DATE:20110107</pre>
	 * or
	 * <pre>RECURRENCE-ID;TZID=Pacific/Auckland:20110107T162347</pre>
	 * etc, as appropriate for the DateTime value.
	 * </p>
	 * <p>
	 * The iCalendar property string is <em>not</em> wrapped at 75 octets. You will need to
	 * do that yourself... :-)
	 * </p>
	 * @param name
	 * @return a string which is formatted like an iCalendar property.
	 */
	public String toPropertyString( PropertyName name ) {
		StringBuilder ret = new StringBuilder(name.toString());
		if ( isDate ) {
			ret.append(";VALUE=DATE");
			// VALUE=DATE *MUST NOT* contain a TZID (RFC5545 3.2.19)
		}
		else if ( tz != null && !tzName.equals(UTC_NAME) ) {
			ret.append(";TZID=");
			ret.append(tzName);
		}
		ret.append(":");
		ret.append(fmtIcal());
		return ret.toString();
	}


	/**
	 * Returns the data as for fmtIcal(), but with any non-UTC timezone name appended, like: "20110411T095030 Pacific/Auckland"
	 */
	public String toString() {
		StringBuilder ret = new StringBuilder( (epoch == EPOCH_NOT_SET ? "EPOCH_NOT_SET" : Long.toString(epoch)) );
		ret.append(" - ");
		ret.append(fmtIcal());
		if ( tz != null && !tzName.equals(UTC_NAME) ) {
			ret.append(" ");
			ret.append(tzName);
		}
		return ret.toString();
	}


	final private static String[] enWeekDayNames = new String[] { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
	/**
	 * Returns the aCalDateTime in a format for HTTP dates, i.e. a string like "Mon, 11 Apr 2011 09:50:30 GMT" 
	 * @return
	 */
	public String httpDateString() {
		// To look like: Mon, 11 Apr 2011 09:50:30 GMT
		return enWeekDayNames[getWeekDay()] + ", " + toJavaDate().toGMTString();
	}

	
	/**
	 * Compare this AcalDateTime to another.  If this is earlier than the other return a negative
	 * integer and if this is after return a positive integer.  If they are the same return 0.
	 * @param another
	 * @return -1, 1 or 0
	 */
	@Override
	public int compareTo( AcalDateTime another ) {
		if ( this == another ) return 0;
		if ( Constants.debugDateTime ) checkEpoch();
		if ( this.epoch == EPOCH_NOT_SET ) this.calculateEpoch();
		if ( another.epoch == EPOCH_NOT_SET ) another.calculateEpoch();
		return ( this.epoch == another.epoch ? 0 : (this.epoch < another.epoch ? -1 : 1));
	}


	@Override
	public boolean equals( Object another ) {
		if ( another == null || !(another instanceof AcalDateTime) ) return false;
		if ( this == another ) return true;
		return (this.compareTo((AcalDateTime)another) == 0);
	}

	
	/**
	 * Checks whether this date is before some other date.
	 * @param another AcalDateTime
	 * @return true, iff this date is earlier than the other date, false otherwise, including if the other date is null
	 */
	public boolean before( AcalDateTime another ) {
		if ( another == null ) return false;
		return (this.compareTo(another) < 0);
	}


	/**
	 * Checks whether this date is after some other date.
	 * @param another
	 * @return true, iff this date is later than the other date, false otherwise, including if the other date is null
	 */
	public boolean after( AcalDateTime another ) {
		if ( another == null ) return false;
		return (this.compareTo(another) > 0);
	}


	/**
	 * Returns a java.util.Date representation of this AcalDateTime. Particularly useful
	 * with SimpleDateFormat to produce localised.
	 * @return
	 */
	public Date toJavaDate() {
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		return new Date(year-1900, month-1, day, hour, minute, second);
	}

	public synchronized AcalDateTime clone() {
//		if ( Constants.debugDateTime ) checkEpoch();
		AcalDateTime c;
		if ( year == YEAR_NOT_SET ) {
			c = new AcalDateTime();
			c.year = YEAR_NOT_SET;
		}
		else {
			calculateEpoch();
			try {
				c = new AcalDateTime( year, month, day, hour, minute, second, tzName );
			}
			catch ( IllegalArgumentException e ) {
				Log.e(TAG, "Some part of this date is wrong: "+year+"-"+month+"-"+day+" "+hour+":"+minute+":"+second);
				Log.e(TAG, Log.getStackTraceString(e));
				c = new AcalDateTime();
				c.year = year;
				c.month = (month < 1 ? 1 : (month > 12 ? 12 : month));
				c.day = (short) (day < 1 ? 1 : (day > monthDays(year,month) ? monthDays(year,month) : day));
				c.hour = (hour < 0 ? 0 : (hour > 23 ? 23 : hour));
				c.minute = (minute < 0 ? 0 : (minute > 59 ? 59 : minute));
				c.second = (second < 0 ? 0 : (second > 59 ? 59 : second));
			}
		}
		
		c.weekStart = weekStart;
		c.isDate = isDate;

		c.epoch = epoch;
		c.tzName = tzName;
		c.tz = tz;
		if ( Constants.debugDateTime ) c.checkEpoch();
		return c;
	}


	/**
	 * Adds (or subtracts) a number of seconds from this AcalDateTime
	 * @param delta
	 * @return The current object, for chaining.
	 */
	public synchronized AcalDateTime addSeconds(long delta) {
		if ( Constants.debugDateTime ) checkEpoch();
//		final long MAX_SECONDS_DELTA = SECONDS_IN_DAY * 28;
//		final long MIN_SECONDS_DELTA = -MAX_SECONDS_DELTA;

		if ( epoch == EPOCH_NOT_SET ) {
/*
			if ( MIN_SECONDS_DELTA < delta && delta < MAX_SECONDS_DELTA ) {
				epoch += delta;
				day += (delta / SECONDS_IN_DAY) + (delta < 0 ? -1 : 0);
				hour += (delta % SECONDS_IN_DAY) / SECONDS_IN_HOUR;
				minute += (delta % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE;
				second += (delta % SECONDS_IN_MINUTE);
				fixupTimeFields();
				return;
			}
*/
			calculateEpoch();
		}

		//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"Adding "+delta+" to "+epoch);
		epoch += delta;
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"Got epoch of "+epoch);
		year = YEAR_NOT_SET;
		if ( Constants.debugDateTime ) calculateDateTime();
		return this;
	}


	/**
	 * Adds an integer number of days to this AcalDateTime.  And like all integers 'days'
	 * could be negative, and that's OK too.
	 * @param days
	 * @return this, for chaining
	 */
	public synchronized AcalDateTime addDays(int days) {
		if ( days == 0 ) return this;
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();

		int tmpInt = this.day + days;
		if ( tmpInt > 0 && tmpInt <= monthDays(year,month) ) {
			this.epoch = EPOCH_NOT_SET;
			if ( epoch != EPOCH_NOT_SET ) {
				Log.w(TAG,"Adding " + Integer.toString(days) + " days to "
							+fmtIcal()+", epoch ("+epoch+"). Day "
							+day+", new day "+tmpInt );
				this.epoch += ((long) days * SECONDS_IN_DAY);
			}
			this.day = (short) tmpInt;
			if ( Constants.debugDateTime ) checkEpoch();
			return this;
		}
		tmpInt = this.getYearDay() + days;
		if ( tmpInt > 0 && tmpInt <= (DAYS_IN_YEAR + leapDay(year)) ) {
			setYearDay(tmpInt);
			return this;
		}

		if ( tz != null ) tmpInt = this.getDaySecond();
		if ( epoch == EPOCH_NOT_SET ) calculateEpoch();
		epoch += days * SECONDS_IN_DAY;
		year = YEAR_NOT_SET;
		if ( tz != null ) {
			calculateDateTime();
			if ( getDaySecond() != tmpInt ) {
				// There was a DST boundary between the two times, so fix it up. 
				tmpInt -= getDaySecond();
				if ( tmpInt < (SECONDS_IN_HOUR * -12) ) tmpInt += SECONDS_IN_DAY;
				else if ( tmpInt > (SECONDS_IN_HOUR * 12) ) tmpInt -= SECONDS_IN_DAY;
				epoch += tmpInt;
			}
		}
		if ( Constants.debugDateTime ) checkEpoch();
		return this;
	}


	/**
	 * Clones the supplied AcalDateTime and then adds an integer number of days to
	 * the cloned value.  And like all integers 'days' could be negative, and that's OK too.
	 * @param c
	 * @param days
	 * @return
	 */
	public static AcalDateTime addDays(AcalDateTime c, int days) {
		AcalDateTime r = (AcalDateTime) c.clone();
		r.addDays(days);
		return r;
	}


	public AcalDateTime addMonths(int months) {
		if ( months == 0 ) return this;
		if ( Constants.debugDateTime ) checkEpoch();
		if ( year == YEAR_NOT_SET ) calculateDateTime();
		month--;
		month += months;
		if ( month < 0 ) {
			year -= (-month+11) / 12;
			month += ((-month+11) / 12) * 12;
		}
		else {
			year += month / 12;
		}
		month %= 12;
		month++;
		if ( day > monthDays(year,month) ) day = (short) monthDays(year,month);
		epoch = EPOCH_NOT_SET;
		return this;
	}


	public boolean isFloating() {
		return tz == null;
	}


	public AcalDuration getDurationTo(AcalDateTime end) {
		AcalDuration ret = new AcalDuration();
		if ( end == null ) {
			if ( isDate ) ret.setDuration(1, 0); 
		}
		else {
			if ( epoch == EPOCH_NOT_SET ) calculateEpoch();
			if ( end.epoch == EPOCH_NOT_SET ) end.calculateEpoch();
			long seconds = end.epoch - epoch;
			long days = (seconds / SECONDS_IN_DAY);
			seconds %= SECONDS_IN_DAY;
			ret.setDuration((int) days, (int) seconds);
		}
//		if ( Constants.debugDateTime  && Constants.LOG_VERBOSE )
//			Log.v(TAG,"Duration from "+this.fmtIcal()+" to "+(end == null ? "null": end.fmtIcal()+ " = " + ret.toString()));
		return ret;
	}


	public AcalDateTime addDuration(AcalDuration relativeTime) {
		if ( Constants.debugDateTime ) checkEpoch();
		if ( relativeTime.days != 0 ) this.addDays(relativeTime.days);
		if ( relativeTime.seconds != 0 ) this.addSeconds(relativeTime.seconds);
		return this;
	}


	public static AcalDateTime addDuration(AcalDateTime start, AcalDuration relativeTime) {
		if ( Constants.debugDateTime ) start.checkEpoch();
		AcalDateTime newTime = start.clone();
		newTime.addDuration(relativeTime);
		return newTime;
	}


	/**
	 * We do it this way to more easily get a localised month name.
	 * @param month
	 * @return
	 */
	public static String getMonthName(int month) {
		final SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM");
		String monthName = monthFormatter.format(new Date(2011,(month - 1),1));
		return monthName.substring(0, 1).toUpperCase() + monthName.substring(1); 
	}

	public String getMonthName() {
		return getMonthName(month);
	}

	public static String fmtMonthYear(AcalDateTime calendar) {
		if (calendar.year == YEAR_NOT_SET) calendar.calculateDateTime();
		return getMonthName(calendar.month) + " " + calendar.get(YEAR);
	}

	/**
	 * <p>
	 * Works out what suffix a date should have. English only.
	 * </p>
	 * 
	 * @param num
	 * @return
	 */
	public static String getSuffix(int num) {
		if (num % 100 > 3 && num % 100 < 21) return "th";
		switch (num % 10) {
			case 1:		return "st";
			case 2:		return "nd";
			case 3:		return "rd";
			default:	return "th";
		}
	}

	public static String fmtDayMonthYear(AcalDateTime c) {
		final DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.LONG);
		return StaticHelpers.capitaliseWords(dateFormatter.format(c.toJavaDate()));
	}


	public static boolean isWithinMonth(AcalDateTime selectedDate, AcalDateTime displayedMonth) {
		return displayedMonth.getMonth() == selectedDate.getMonth();
	}


	public AcalDateTime setWeekStart(short wDay) {
		if ( wDay < MONDAY || wDay > SUNDAY ) throw new IllegalArgumentException();
		weekStart = wDay;
		return this;
	}

	public static class AcalDateTimeSorter implements Comparator<AcalDateTime> {
		@Override
		public int compare(AcalDateTime arg0, AcalDateTime arg1) {
			if ( arg0.before(arg1) ) return -1;
			else if ( arg0.after(arg1)) return 1;
			else return 0;
		}
	}

	public AcalDateTime(Parcel in) {
		this();
		epoch = in.readLong();
		weekStart = (short) in.readInt();
		isDate = (in.readByte() == 'D');
		boolean tzIsSet = (in.readByte() == '1');
		if ( tzIsSet ) overwriteTimeZone(in.readString());
		year = AcalDateTime.YEAR_NOT_SET;
	}

	public static final Parcelable.Creator<AcalDateTime> CREATOR = new Parcelable.Creator<AcalDateTime>() {
        public AcalDateTime createFromParcel(Parcel in) {
            return new AcalDateTime(in);
        }

        public AcalDateTime[] newArray(int size) {
            return new AcalDateTime[size];
        }
    };

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if ( epoch == EPOCH_NOT_SET ) calculateEpoch();
		dest.writeLong(epoch);
		dest.writeInt(weekStart);
		dest.writeByte((byte) (this.isDate ? 'D' : 'T'));
		dest.writeByte((byte) (tzName == null ? '0' : '1'));
		if ( tzName != null ) dest.writeString(tzName);
	}


	public AcalProperty asProperty(String propertyName) {
		AcalProperty ret = new AcalProperty(propertyName, fmtIcal());
		if ( isDate ) ret.setParam(AcalProperty.PARAM_VALUE, "DATE");
		else if ( tz != null && !tzName.equals(UTC_NAME) )	ret.setParam(AcalProperty.PARAM_TZID,tzName);
		return ret;
	}


	public AcalProperty asProperty(PropertyName pName) {
		return asProperty(pName.toString());
	}


	/**
	 * Gets an instance of AcalDateTime at the current time in the UTC timezone. 
	 * @return the new instance
	 */
	public static AcalDateTime getUTCInstance() {
		AcalDateTime answer = new AcalDateTime();
		answer.epoch = System.currentTimeMillis() / 1000L;
		answer.tz = UTC;
		answer.tzName = UTC_NAME;
		answer.year = YEAR_NOT_SET;
		return answer;
	}


	/**
	 * Gets an instance of AcalDateTime at the current clock time in the current timezone. 
	 * @return the new instance
	 */
	public static AcalDateTime getInstance() {
		return AcalDateTime.getUTCInstance().applyLocalTimeZone();
	}


	/**
	 * Set this AcalDateTime to the start of the month
	 * @return this, for chaining
	 */
	public AcalDateTime setMonthStart() {
		setMonthDay(1);
		return setDaySecond(0);
	}

}
