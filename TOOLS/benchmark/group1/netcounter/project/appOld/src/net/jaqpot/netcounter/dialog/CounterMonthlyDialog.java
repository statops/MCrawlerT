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

import java.util.Calendar;

import net.jaqpot.netcounter.HandlerContainer;
import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.model.Counter;
import net.jaqpot.netcounter.model.NetCounterModel;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.DatePicker;

public class CounterMonthlyDialog extends CounterDialog implements OnClickListener {

	private final HandlerContainer mContainer;

	private final NetCounterModel mModel;

	private final DatePicker mDatePicker;

	public CounterMonthlyDialog(Context context, NetCounterApplication app) {
		super(context, app);

		mContainer = (HandlerContainer) app.getAdapter(HandlerContainer.class);
		mModel = (NetCounterModel) app.getAdapter(NetCounterModel.class);

		setTitle(R.string.dialogCounterMonthlyTitle);

		mDatePicker = new DatePicker(context);

		setView(mDatePicker);

		setButton(getText(R.string.ok), this);
		setButton2(getText(R.string.cancel), (OnClickListener) null);
	}

	@Override
	protected void updateContent(Counter counter) {
		int[] values = new int[3];
		String day = counter.getProperty(Counter.DAY);
		if (day == null) {
			Calendar c = Calendar.getInstance();
			values[0] = c.get(Calendar.YEAR);
			values[1] = c.get(Calendar.MONTH);
			values[2] = 1;
		} else {
			String[] a = day.split("-");
			for (int j = 0; j < 3; j++) {
				values[j] = Integer.valueOf(a[j]);
			}
		}
		mDatePicker.updateDate(values[0], values[1], values[2]);
	}

	public void onClick(DialogInterface dialog, int which) {
		int day = mDatePicker.getDayOfMonth();
		int month = mDatePicker.getMonth();
		int year = mDatePicker.getYear();

		final String value = year + "-" + month + "-" + day;
		mContainer.getSlowHandler().post(new Runnable() {
			public void run() {
				getCounter().setProperty(Counter.DAY, value);
				mModel.commit();
			}
		});
		getApplication().toast(R.string.dialogCounterMonthlyChanged);
	}

}
