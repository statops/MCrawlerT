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

import java.util.List;

import android.content.Context;

import com.morphoss.acal.R;
import com.morphoss.acal.acaltime.AcalRepeatRule.RRuleFreqType;

/**
 * @author Morphoss Ltd
 */

public class AcalRepeatMonthly extends AcalRepeatRuleParser {

	protected AcalRepeatMonthly(String rRuleValue, RRuleFreqType f) {
		super(rRuleValue,f);
	}

	@Override
	public String getFrequencyName() {
		return "MONTHLY";
	}

	@Override
	public String getPrettyFrequencyName(Context cx) {
		if ( interval != 1 ) return String.format(cx.getString(R.string.EveryNMonths), Integer.toString(interval));
		return cx.getString(R.string.EveryMonth);
	}

	
	@Override
	protected void nextFrequency() {
		if ( currentBase.get(AcalDateTime.MONTH) == AcalDateTime.FEBRUARY
					&& originalBase.get(AcalDateTime.DAY_OF_MONTH) == 29 ) { 
			currentBase.set(AcalDateTime.DAY_OF_MONTH, 28 );
			currentBase.addMonths(interval);
			if ( currentBase.getActualMaximum(AcalDateTime.DAY_OF_MONTH) == 29 ) {
				currentBase.set(AcalDateTime.DAY_OF_MONTH, 29 );
			}
		}
		else {
			currentBase.addMonths(interval);
		}
	}

	
	@Override
	public List<AcalDateTime> buildSet() {
		startNewSet();

		if ( bymonth    != null ) {
			limitByMonth();
//			debugCurrentSet( "Monthly, after BYMONTH expansion");
		}
		if ( bymonthday != null ) {
			expandByMonthDay();
//			debugCurrentSet( "Monthly, after BYMONTHDAY expansion");
		}
		else if ( originalBase.get(AcalDateTime.DAY_OF_MONTH) > 28 ) {
			expandByMonthDay(new int[] { originalBase.get(AcalDateTime.DAY_OF_MONTH) });
//			debugCurrentSet( "Monthly, after BYMONTHDAY expansion");
		}

		if ( byday      != null ) {
			specialMonthlyByDay();
//			debugCurrentSet( "Monthly, after special BYDAY expansion");
		}

		if ( byhour     != null ) {
			expandByHour();
//			debugCurrentSet( "Monthly, after BYHOUR expansion");
		}
		if ( byminute   != null ) expandByMinute();
		if ( bysecond   != null ) expandBySecond();
		
		if ( bysetpos   != null ) {
			limitBySetPos();
//			debugCurrentSet( "Monthly, after BYSETPOS expansion");
		}
		
		return currentSet;
	}

	
	public void specialMonthlyByDay() {
		/*
		 *Note 1:  Limit if BYMONTHDAY is present; otherwise, special expand for MONTHLY. 
		 */
		if ( bymonthday != null ) limitByDay();
		else expandByDayMonthly();
	}
}
