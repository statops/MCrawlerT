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
import java.util.List;

import android.content.Context;

import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalRepeatRule.RRuleFreqType;

/**
 * @author Morphoss Ltd
 */

public class AcalRepeatYearly extends AcalRepeatRuleParser {

	protected AcalRepeatYearly(String rRuleValue, RRuleFreqType f) {
		super(rRuleValue,f);
	}

	@Override
	public String getFrequencyName() {
		return "YEARLY";
	}

	@Override
	public String getPrettyFrequencyName(Context cx) {
		if ( interval != 1 ) return String.format(cx.getString(R.string.EveryNYears), Integer.toString(interval));
		return cx.getString(R.string.EveryYear);
	}

	@Override
	protected void nextFrequency() {
		if ( currentBase.get(AcalDateTime.MONTH) == AcalDateTime.FEBRUARY
					&& originalBase.get(AcalDateTime.DAY_OF_MONTH) == 29 ) { 
			currentBase.set(AcalDateTime.DAY_OF_MONTH, 28 );
			currentBase.setYear(currentBase.getYear() + interval);
			if ( currentBase.getActualMaximum(AcalDateTime.DAY_OF_MONTH) == 29 ) {
				currentBase.set(AcalDateTime.DAY_OF_MONTH, 29 );
			}
		}
		else {
			currentBase.setYear(currentBase.getYear() + interval);
		}
	}

	
	@Override
	public List<AcalDateTime> buildSet() {
		startNewSet();

		if ( bymonth    != null ) {
			expandByMonth();
//			debugCurrentSet( "Yearly, after BYMONTH expansion");
		}
		if ( byweekno   != null ) {
			expandByWeekNo();
//			debugCurrentSet("Yearly, after BYWEEKNO expansion");
		}
		if ( byyearday  != null ) {
			expandByYearDay();
//			debugCurrentSet("Yearly, after BYYEARDAY expansion");
		}
		if ( bymonthday != null ) {
			expandByMonthDay();
//			debugCurrentSet("Yearly, after BYMONTHDAY expansion");
		}
		else if ( byyearday == null && byweekno == null && originalBase.get(AcalDateTime.DAY_OF_MONTH) > 28 ) {
			expandByMonthDay(new int[] { originalBase.get(AcalDateTime.DAY_OF_MONTH) });
//			debugCurrentSet("Yearly, after base BYMONTHDAY expansion");
		}

		if ( byday      != null ) {
			specialYearlyByDay();
//			debugCurrentSet("Yearly, after BYDAY expansion / limit");
		}

		if ( byhour     != null ) expandByHour();
		if ( byminute   != null ) expandByMinute();
		if ( bysecond   != null ) expandBySecond();
		
		if (bysetpos != null) {
			limitBySetPos();
//			debugCurrentSet("Yearly, after BYSETPOS limits");
		}
		
		return currentSet;
	}


	private void specialYearlyByDay() {
		/*
		 * Note 2:  Limit if BYYEARDAY or BYMONTHDAY is present; otherwise,
         *        special expand for WEEKLY if BYWEEKNO present; otherwise,
         *        special expand for MONTHLY if BYMONTH present; otherwise,
         *        special expand for YEARLY.
		 */
		if ( byyearday != null || bymonthday != null ) limitByDay();
		else if ( byweekno != null ) expandByDay();
		else if ( bymonth != null )  expandByDayMonthly();
		else {
			List<AcalDateTime> finalSet = new ArrayList<AcalDateTime>();
			AcalDateTime firstOfYear = null;
			AcalDateTime lastOfYear = null;
			int dowOfFirst;
			int dowOfLast = 0;
			for( AcalDateTime c : currentSet ) {
				c.set(AcalDateTime.DAY_OF_YEAR, 1);
				if ( firstOfYear != null && firstOfYear.equals(c)) continue;
				firstOfYear = c;
				dowOfFirst = c.get(AcalDateTime.DAY_OF_WEEK);
				boolean[] daysInYear = new boolean[c.getActualMaximum(AcalDateTime.DAY_OF_YEAR)];
				for( AcalRepeatRuleDay day :  byday ) {
					if ( day.setPos == 0 ) {
						// Like all Wednesdays in month
						for( int d = day.wDay - dowOfFirst; d < daysInYear.length; d += 7 ) {
							if ( d < 0 ) continue;
							daysInYear[d] = true;
						}
					}
					else if ( day.setPos > 0 ){
						// Like 3rd Tuesday
						int d = day.wDay - dowOfFirst;
						if ( d < 0 ) d+= 7;
						d += (7 * (day.setPos - 1));
						if ( d < daysInYear.length ) daysInYear[d] = true;  
					}
					else {
						// Like 2nd to last Monday
						if ( lastOfYear == null ) {
							c.set(AcalDateTime.DAY_OF_YEAR,c.getActualMaximum(AcalDateTime.DAY_OF_YEAR));
							dowOfLast = c.get(AcalDateTime.DAY_OF_WEEK);
						}
						int d = (daysInYear.length - dowOfLast) + day.wDay;
						if ( d >= daysInYear.length ) d -= 7;
						d += (7 * (day.setPos + 1));  // Note that day.setPos *IS* negative	
						if ( d >= 0 ) daysInYear[d] = true;  
					}
				}
				for( int i=0; i<daysInYear.length; i++ ) {
					if ( daysInYear[i] ) {
						c.set(AcalDateTime.DAY_OF_YEAR,i+1);
						c.set(AcalDateTime.SECOND, originalBase.get(AcalDateTime.SECOND));
						finalSet.add((AcalDateTime) c.clone());
					}
				}
			}
			currentSet = finalSet;
		}
		Collections.sort(currentSet, new AcalDateTime.AcalDateTimeSorter());
	}
}
