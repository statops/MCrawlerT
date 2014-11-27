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

public class CounterTypeDialog extends CounterSingleChoiceDialog {

	private final HandlerContainer mContainer;

	private final NetCounterModel mModel;

	public CounterTypeDialog(Context context, NetCounterApplication app) {
		super(context, app, R.array.counterTypes);

		setTitle(R.string.dialogCounterTypeTitle);

		mContainer = (HandlerContainer) app.getAdapter(HandlerContainer.class);
		mModel = (NetCounterModel) app.getAdapter(NetCounterModel.class);
	}

	@Override
	protected void updateContent(Counter counter) {
		int type = counter.getType();
		for (int i = 0; i < NetCounterApplication.COUNTER_TYPES_POS.length; i++) {
			int value = NetCounterApplication.COUNTER_TYPES_POS[i];
			if (value == type) {
				setDefault(i);
				return;
			}
		}
	}

	@Override
	protected void onClick(int pos) {
		final Counter counter = getCounter();
		final int type = NetCounterApplication.COUNTER_TYPES_POS[pos];
		if (counter.getType() != type) {
			mContainer.getSlowHandler().post(new Runnable() {
				public void run() {
					counter.setType(type);
					mModel.commit();
				}
			});
			getApplication().toast(R.string.menuCounterChangeDone);
		}
		dismiss();
	}

}
