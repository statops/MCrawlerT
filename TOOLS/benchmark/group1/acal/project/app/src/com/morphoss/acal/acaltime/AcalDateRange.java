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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.Constants;

/**
 * @author Morphoss Ltd
 */

public class AcalDateRange implements Parcelable, Cloneable {

	private final static String TAG = "AcalDateRange";

	final public AcalDateTime start;
	final public AcalDateTime end;

	public AcalDateRange(AcalDateTime start, AcalDateTime end) {
		this.start = (start==null? null : start.clone());
		this.end = (end==null ? null : end.clone());
	}
	
	public static final Parcelable.Creator<AcalDateRange> CREATOR = new Parcelable.Creator<AcalDateRange>() {
        public AcalDateRange createFromParcel(Parcel in) {
            return new AcalDateRange(in);
        }

        public AcalDateRange[] newArray(int size) {
            return new AcalDateRange[size];
        }
    };

    public AcalDateRange(Parcel in) {
		this.start = new AcalDateTime(in);
		this.end = new AcalDateTime(in);
    }
    
	@Override
	public int describeContents() {
		return 0;
	}
	
	public AcalDateRange getIntersection(AcalDateRange other) {
		if ( end != null && end.before(other.start) ) return null;

		AcalDateTime newStart = start;
		if ( newStart.before(other.start) ) newStart = other.start;

		AcalDateTime newEnd = (end == null ? other.end : end );
		if ( newEnd != null && other.end != null && newEnd.after(other.end) ) newEnd = other.end;
		if ( newEnd != null && newEnd.before(newStart)) return null;

		if ( Constants.debugDateTime && Constants.LOG_VERBOSE )	Log.println(Constants.LOGV,TAG,"Intersection of ("+start.fmtIcal()+","+(end==null?"null":end.fmtIcal())+") & ("
						+other.start.fmtIcal()+","+(other.end==null?"null":other.end.fmtIcal())+") is ("
						+newStart.fmtIcal()+","+(newEnd==null?"null":newEnd.fmtIcal())+")"
					);
		
		return new AcalDateRange(newStart,newEnd);
		
	}

	/**
	 * Test whether this range overlaps the test range
	 * @param start
	 * @param finish
	 * @return true, if the ranges overlap.
	 */
	public boolean overlaps( AcalDateRange dTest ) {
		if ( dTest == null ) return false;
		return overlaps(dTest.start,dTest.end);
	}

	/**
	 * Test whether this range overlaps the period from otherStart (inclusive) to otherEnd (non-inclusive). A null
	 * at either end extends the range to infinity in that direction.
	 * @param otherStart
	 * @param otherEnd
	 * @return true, if the ranges overlap.
	 */
	public boolean overlaps( AcalDateTime otherStart, AcalDateTime otherEnd ) {
		if ( (otherStart == null && otherEnd == null)
					|| (start == null && end == null) )
			return true;
		boolean answer;
		if ( end == null ) {
			answer = (otherEnd == null || otherEnd.after(start));
		}
		else if ( start == null ) {
			answer = (otherStart == null || otherStart.before(end));
		}
		else if ( otherEnd == null ) {
			answer = otherStart.before(end);
		}
		else if ( otherStart == null ) {
			answer = otherEnd.after(start);
		}
		else if ( otherEnd.equals(start) || otherStart.equals(end) ) {
			// end -to- start abutted events do not overlap
			return false;
		}
		else {
			answer = ( !otherEnd.before(start) && !otherStart.after(end) );
		}
		if ( Constants.debugDateTime &&  Constants.LOG_VERBOSE )
			Log.println(Constants.LOGV,TAG,"Overlap of "+toString()+
					" with range("+(otherStart==null?"<forever<":otherStart.fmtIcal())+
					","+ (otherEnd==null?">forever>":otherEnd.fmtIcal())+") is: "
						+ (answer? "yes":"no")
					);

		return answer;
	}

	public String toString() {
		return "range("+(start==null?"<forever<":start.fmtIcal())+","+(end==null?">forever>":end.fmtIcal())+")";
	}

	public AcalDateRange clone() {
		return new AcalDateRange(start.clone(),end.clone());
	}
	
	@Override
	public void writeToParcel(Parcel out, int flags) {
		start.writeToParcel(out, flags);
		end.writeToParcel(out, flags);
	}

	/**
	 * Returns true if someDate is contained within the range from start (inclusive) to end (exclusive), so
	 * for example the time 14:00 would be within the range 14:00 to 15:00, but not the range 13:00 to 14:00,
	 * given that the date was the same in all cases.
	 * 
	 * Null for either end of the range is considered to mean -infinity or +infinity
	 * 
	 * @param someDate
	 * @return
	 */
	public boolean contains(AcalDateTime someDate) {
		if ( start == null || !start.after(someDate) )
			if ( end == null || end.after(someDate) ) return true;
		return false;
	}

	
	/**
	 * Returns true if 'this' range completely overlaps 'someRange'
	 * 
	 * Null for either end of either range is considered to mean -infinity or +infinity
	 * 
	 * @param someRange
	 * @return
	 */
	public boolean contains(AcalDateRange someRange) {
		if ( start == null || !start.after(someRange.end) )
			if ( end == null || end.after(someRange.start) ) return true;
		return false;
	}

	/**
	 * Returns a new range which will include this and a new range.  If there are gaps between the
	 * two ranges then the will also be part of the new range. 
	 * 
	 * Null for either end of either range is considered to mean -infinity or +infinity
	 * 
	 * @param someRange
	 * @return
	 */
	public AcalDateRange extendTo(AcalDateRange someRange) {
		AcalDateTime newStart = start;
		if ( start != null && someRange.start == null )
			newStart = null;
		else if ( start != null && someRange.start.before(start) )
			newStart = someRange.start;
		
		AcalDateTime newEnd = end;
		if ( end != null && someRange.end == null )
			newEnd = null;
		else if ( end != null && someRange.end.after(end) )
			newEnd = someRange.end;

		return new AcalDateRange(newStart.clone(),newEnd.clone());
	}
}
