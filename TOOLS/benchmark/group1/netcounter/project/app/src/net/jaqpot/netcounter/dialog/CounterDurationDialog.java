/*
 * Copyright (C) 2009 Cyril Jaquier
 *
 * This file is part of NetCounter.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.jaqpot.netcounter.dialog;

import net.jaqpot.netcounter.HandlerContainer;
import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.model.Counter;
import net.jaqpot.netcounter.model.NetCounterModel;
import android.content.Context;

public class CounterDurationDialog extends CounterSingleChoiceDialog {

	private final HandlerContainer mContainer;

	private final NetCounterModel mModel;

	public CounterDurationDialog(Context context, NetCounterApplication app) {
		// Set a default array.
		super(context, app, R.array.counterLastDays);

		setTitle(R.string.dialogCounterDurationTitle);

		mContainer = app.getAdapter(HandlerContainer.class);
		mModel = app.getAdapter(NetCounterModel.class);
	}

	@Override
	protected void updateContent(Counter counter) {
		String n = counter.getProperty(Counter.NUMBER, "0");
		int selected = Integer.parseInt(n);

		switch (counter.getType()) {
		case Counter.MONTHLY:
			setArray(R.array.counterMonthly);
			break;
		case Counter.LAST_MONTH:
			setArray(R.array.counterLastMonth);
			break;
		case Counter.WEEKLY:
			setArray(R.array.counterWeekly);
			break;
		case Counter.LAST_DAYS:
			setArray(R.array.counterLastDays);
			break;
		case Counter.SINGLE_DAY:
			setArray(R.array.counterSingleDay);
			break;
		}

		setDefault(selected);
	}

	@Override
	protected void onClick(final int pos) {
		final Counter counter = getCounter();
		String n = counter.getProperty(Counter.NUMBER, "0");
		int selected = Integer.parseInt(n);
		if (selected != pos) {
			mContainer.getSlowHandler().post(new Runnable() {
				public void run() {
					String s = String.valueOf(pos);
					counter.setProperty(Counter.NUMBER, s);
					mModel.commit();
				}
			});
			getApplication().toast(R.string.menuCounterChangeDone);
		}
		dismiss();
	}

}
