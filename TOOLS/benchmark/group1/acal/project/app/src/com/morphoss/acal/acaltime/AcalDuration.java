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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Parcel;
import android.os.Parcelable;

import com.morphoss.acal.HashCodeUtil;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.davacal.AcalProperty;
import com.morphoss.acal.davacal.PropertyName;

/**
 * <h2>
 * AcalDuration a class for handling RFC5545 durations.</h2>
 * 
 * <p>
 * From RFC5545, Section 3.3:
 * 
 * <pre>
 * Format Definition:  This value type is defined by the following
 * notation:
 * 
 *  dur-value  = (["+"] / "-") "P" (dur-date / dur-time / dur-week)
 * 
 *  dur-date   = dur-day [dur-time]
 *  dur-time   = "T" (dur-hour / dur-minute / dur-second)
 *  dur-week   = 1*DIGIT "W"
 *  dur-hour   = 1*DIGIT "H" [dur-minute]
 *  dur-minute = 1*DIGIT "M" [dur-second]
 *  dur-second = 1*DIGIT "S"
 *  dur-day    = 1*DIGIT "D"
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * In order to accommodate this we separate our durations into two parts:
 * </p>
 * <ul>
 * <li>A days component</li>
 * <li>A time component</li>
 * </ul>
 * 
 * <p>
 * This caters for daylight saving boundaries, where a day might gain or lose an hour, as well as catering for
 * the occasional leap second, where days are not exactly 86400 seconds long. Note that a round-trip through
 * this class does not guarantee to return you an identical string. PT60M *will* become PT1H, etc.
 * </p>
 * 
 * <p>
 * A zero duration will specifically return "PT0H" which is a common result.
 * </p>
 * 
 * @author Morphoss Ltd
 */
 public class AcalDuration implements Parcelable, Serializable {
	
	private static final long	serialVersionUID	= 1L;
	private static final Pattern rfc5545duration = Pattern.compile(
				"^([+-])?P" +      			// Sign    #1
				"(?:" +
					"([0-9]{1,4})W" + 			// Week    #2
					"|(?:([0-9]{1,5})D)?" +		// Day     #3
					"(?:T" +
						"(?:([0-9]{1,2})H)?" +	// Hour    #4
						"(?:([0-9]{1,4})M)?" +	// Minute  #5
						"(?:([0-9]{1,4})S)?" +	// Second  #6
					")?" +
				")\\s*$"
			);

	int days = 0;
	int seconds = 0;

	
	/**
	 * Rarely we might want to construct it and assign something to it that way
	 */
	public AcalDuration() {
		// Default of 0 is fine.
	}

	/**
	 * Duplicate an existing duration
	 */
	public AcalDuration( AcalDuration previousDuration) {
		if ( previousDuration == null ) return;
		days = previousDuration.days;
		seconds = previousDuration.seconds;
	}
	
	public AcalDuration(long millis) {
		long seconds = millis/1000L;
		this.days = (int)(millis/86400L);
		seconds -= (this.days*86400L);
		this.seconds = (int)seconds;
	}

	/**
	 * <p>
	 * Mostly we will construct these from RFC5545 compliant strings, which is what this constructor does.
	 * </p>
	 * 
	 * @param iCalDuration
	 */
	public AcalDuration( String iCalDuration ) {

		if ( iCalDuration == null )
			throw new IllegalArgumentException("Duration may not be constructed from a null.");

		Matcher m = rfc5545duration.matcher(iCalDuration);
		if ( ! m.matches() )
			throw new IllegalArgumentException("Duration '" + iCalDuration + "' is not in a recognised format.");

		if ( m.matches() ) {
			int sign = (m.group(1) == null || m.group(1).equals("+") || m.group(1).equals("") ? 1 : -1 );
			days = (StaticHelpers.safeToInt(m.group(2)) * 7 + StaticHelpers.safeToInt(m.group(3))) * sign; 
			seconds = ((StaticHelpers.safeToInt(m.group(4)) * 3600) + (StaticHelpers.safeToInt(m.group(5)) * 60) + StaticHelpers.safeToInt(m.group(6))) * sign;
		}

	}

	public AcalDuration(Parcel in) {
		this.days = in.readInt();
		this.seconds = in.readInt();
	}

	/**
	 * <p>
	 * Return the time part of the duration in milliseconds.
	 * </p>
	 * 
	 * @return
	 */
	public long getTimeMillis() {
		return (long) (seconds * 1000);
	}


	/**
	 * <p>
	 * Return the days part of the duration.
	 * </p>
	 * 
	 * @return
	 */
	public int getDays() {
		return days;
	}

	

	public long getDurationMillis() {
		return ((long) days * 86400000L) + ((long) seconds * 1000L);
	}

	/**
	 * <p>
	 * Set the duration from date & time parts.  Both parameters must have the same sign.
	 * </p>
	 * 
	 * @param newDays
	 * @param newSeconds
	 */
	public void setDuration( int newDays, int newSeconds ) {
		if ( (newDays > 0 && newSeconds < 0) || newDays < 0 && newSeconds > 0 )
			throw new IllegalArgumentException("The sign of both parameters must be in the same direction.");
		days = newDays;
		seconds = newSeconds;
	}

	
	/**
	 * Turn our duration internals back into an RFC5545 string.
	 */
	@Override
	public String toString() {
		if ( days == 0 && seconds == 0 ) return "PT0H"; // Some things like to see at least something there
		StringBuilder result = new StringBuilder("");
		if ( days < 0 || seconds < 0 ) result.append("-");
		result.append("P");
		if ( seconds == 0 && ((days / 7) * 7) == days ) {
			result.append(Integer.toString((Math.abs(days) / 7)));
			result.append("W");
		}
		else {
			if ( days != 0 ) {
				result.append(Math.abs(days));
				result.append("D");
			}
			if ( seconds != 0) {
				result.append("T");
				int s = Math.abs(seconds);
				int h = s / 3600;
				s -= (h*3600);
				int m = s / 60;
				s -= (m*60);
				if ( h > 0 ) {
					result.append(h);
					result.append("H");
				}
				if ( m > 0 ) {
					result.append(m);
					result.append("M");
				}
				if ( s > 0 ) {
					result.append(s);
					result.append("S");
				}
			}
		}
		return result.toString();
	}
 
	
	/**
	 * Turn our duration internals back into a pretty string.
	 * @param relatedPart The name of a related part like 'START' or 'END'. Or null.
	 * @todo Should be localized, somehow, but these things are very tricky.
	 */
	public String toPrettyString( String relatedPart ) {
		if ( days == 0 && seconds == 0 ) return (relatedPart == null ? "" : "At the "+relatedPart+".");
		StringBuilder result = new StringBuilder("");
		if ( seconds == 0 && ((days / 7) * 7) == days ) {
			result.append(Integer.toString((Math.abs(days) / 7)));
			result.append(" week");
			if ( Math.abs(days) > 7 ) result.append("s");
		}
		else {
			if ( days != 0 ) {
				result.append(Math.abs(days));
				result.append(" day");
				if ( Math.abs(days) > 1 ) result.append("s");
			}
			if ( seconds != 0) {
				if ( Math.abs(days) > 0 ) result.append(", ");
				int s = Math.abs(seconds);
				int h = s / 3600;
				s -= (h*3600);
				int m = s / 60;
				s -= (m*60);
				if ( h > 0 ) {
					result.append(h);
					result.append(" hour");
					if ( h > 1 ) result.append("s");
				}
				if ( m > 0 ) {
					if ( h > 0 ) result.append(", ");
					result.append(m);
					result.append(" minute");
					if ( m > 1 ) result.append("s");
				}
				if ( s > 0 ) {
					if ( m > 0 || (h > 0 && m == 0) ) result.append(", ");
					result.append(s);
					result.append(" second");
					if ( s > 1 ) result.append("s");
				}
			}
		}

		result.append( days < 0 || seconds < 0 ? " before" : " after" );
		if ( relatedPart != null ) { 
			result.append(" ");
			result.append(relatedPart);
		}
		result.append(".");

		return result.toString();
	}
 
	
	/**
	 * <p>
	 * Test for equality.  Note that PT24H is != P1D, whereas P60M == P1H.  Not sure if that violates the
	 * principal of least surprise or not.  The spec really isn't clear on that sort of thing.
	 * </p>
	 */
	@Override
	public boolean equals(Object that) {
		if ( this == that ) return true;
	    if ( !(that instanceof AcalDuration) ) return false;
	    AcalDuration thatDuration = (AcalDuration)that;
	    return (
	    			this.days == thatDuration.days
	    			&& this.seconds == thatDuration.seconds
	    	);
		
	}
	
	@Override
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash( result, this.days );
	    result = HashCodeUtil.hash( result, this.seconds );
	    return result;
	}

	public static AcalDuration fromProperty(AcalProperty durationProperty ) {
		if ( durationProperty == null ) return null;
		try {
			AcalDuration ret = new AcalDuration( durationProperty.getValue() );
			return ret;
		}
		catch ( Exception e ) {
		}
		return null;
	}
	
	public AcalProperty asProperty( String propertyName ) {
		return new AcalProperty(propertyName,toString());
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(days);
		out.writeInt(seconds);
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
	public AcalDateTime getEndDate(AcalDateTime startDate) {
		return AcalDateTime.addDuration(startDate,this);
	}

	public static final Parcelable.Creator<AcalDuration> CREATOR = new Parcelable.Creator<AcalDuration>() {
		public AcalDuration createFromParcel(Parcel in) {
			return new AcalDuration(in);
		}

		public AcalDuration[] newArray(int size) {
			return new AcalDuration[size];
		}
	};


	public AcalProperty asProperty(PropertyName pName) {
		return this.asProperty(pName.toString());
	}
	
}
