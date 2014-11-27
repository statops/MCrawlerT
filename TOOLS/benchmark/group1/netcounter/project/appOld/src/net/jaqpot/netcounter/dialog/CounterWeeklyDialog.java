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

public class CounterWeeklyDialog extends CounterSingleChoiceDialog {

	private final HandlerContainer mContainer;

	private final NetCounterModel mModel;

	public CounterWeeklyDialog(Context context, NetCounterApplication app) {
		super(context, app, R.array.counterWeeklyStart);

		mContainer = (HandlerContainer) app.getAdapter(HandlerContainer.class);
		mModel = (NetCounterModel) app.getAdapter(NetCounterModel.class);

		setTitle(R.string.dialogCounterWeeklyTitle);
	}

	@Override
	protected void updateContent(Counter counter) {
		String day = counter.getProperty(Counter.DAY, "0");
		int d = Integer.valueOf(day);
		setDefault(d);
	}

	@Override
	protected void onClick(int pos) {
		final Counter counter = getCounter();
		String day = counter.getProperty(Counter.DAY, "0");
		final String newDay = String.valueOf(pos);
		if (!day.equals(newDay)) {
			mContainer.getSlowHandler().post(new Runnable() {
				public void run() {
					counter.setProperty(Counter.DAY, newDay);
					mModel.commit();
				}
			});
			getApplication().toast(R.string.dialogCounterWeeklyChanged);
		}
		dismiss();
	}

}
