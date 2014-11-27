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

public class AcalRepeatWeekly extends AcalRepeatRuleParser {

	protected AcalRepeatWeekly(String rRuleValue, RRuleFreqType f) {
		super(rRuleValue, f);
	}

	@Override
	public String getFrequencyName() {
		return "WEEKLY";
	}

	@Override
	protected void nextFrequency() {
		currentBase.addDays(7 * interval);
	}

	@Override
	public List<AcalDateTime> buildSet() {
		startNewSet();

		if ( byday    != null ) expandByDay();
		if ( byhour   != null ) expandByHour();
		if ( byminute != null ) expandByMinute();
		if ( bysecond != null ) expandBySecond();
		
		if ( bysetpos != null ) limitBySetPos();

		return currentSet;
	}

	@Override
	public String getPrettyFrequencyName(Context cx) {
		if ( interval != 1 ) return String.format(cx.getString(R.string.EveryNWeeks), Integer.toString(interval));
		return cx.getString(R.string.EveryWeek);
	}

}
