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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h3>Internal class for days of the week.</h3>
 *  
 * @author Morphoss Ltd
 */
public class AcalRepeatRuleDay {
	public final short setPos;
	public final short wDay;
	
	private final static Pattern	dayPattern	= Pattern.compile("^(-?\\d*)((?:S[UA])|MO|(?:T[UH])|WE|FR)$",
															Pattern.CASE_INSENSITIVE);

	public AcalRepeatRuleDay(String stDay) {
		Matcher m = dayPattern.matcher(stDay);
		if ( !m.matches() )
			throw new IllegalArgumentException("RRULE '"+stDay+"' is not a valid day of week specifier.");

		if ( m.group(1) != null && !m.group(1).equals("") ) {
			setPos = Short.parseShort(m.group(1));
		}
		else setPos = 0;

		switch (m.group(2).charAt(1)) { // Greatest selectivity in second character
			case 'O':	wDay = AcalDateTime.MONDAY;			return;
			case 'E':	wDay = AcalDateTime.WEDNESDAY;		return;
			case 'H':	wDay = AcalDateTime.THURSDAY;		return;
			case 'R':	wDay = AcalDateTime.FRIDAY;			return;
			case 'A':	wDay = AcalDateTime.SATURDAY;		return;
		}
		switch (m.group(2).charAt(0)) {
			case 'S':	wDay = AcalDateTime.SUNDAY;			return;
			case 'T':	wDay = AcalDateTime.TUESDAY;		return;
		}
		throw new IllegalArgumentException("RRULE '"+stDay+"' is not a valid day of week specifier.");
	}
	
	public String toString() {
		String ret = "";
		if ( setPos != 0 ) ret += Integer.toString(setPos);
		switch( wDay ) {
			case AcalDateTime.MONDAY:		ret += "MO";	break;
			case AcalDateTime.TUESDAY:		ret += "TU";	break;
			case AcalDateTime.WEDNESDAY:	ret += "WE";	break;
			case AcalDateTime.THURSDAY:		ret += "TH";	break;
			case AcalDateTime.FRIDAY:		ret += "FR";	break;
			case AcalDateTime.SATURDAY:		ret += "SA";	break;
			case AcalDateTime.SUNDAY:		ret += "SU";	break;
		}
		return ret;
	}

	public String toPrettyString() {
		String ret = "";
		if ( setPos != 0 ) {
			ret += "the "+Integer.toString(setPos) + AcalDateTime.getSuffix(setPos) + " ";
		}
		switch( wDay ) {
			case AcalDateTime.MONDAY:		ret += "Monday";	break;
			case AcalDateTime.TUESDAY:		ret += "Tuesday";	break;
			case AcalDateTime.WEDNESDAY:	ret += "Wednesday";	break;
			case AcalDateTime.THURSDAY:		ret += "Thursday";	break;
			case AcalDateTime.FRIDAY:		ret += "Friday";	break;
			case AcalDateTime.SATURDAY:		ret += "Saturday";	break;
			case AcalDateTime.SUNDAY:		ret += "Sunday";	break;
		}
		return ret;
	}

	public boolean isWeekDay() {
		switch( wDay ) {
			case AcalDateTime.SATURDAY:
			case AcalDateTime.SUNDAY:
				return false;
		}
		return true;
	}
}


