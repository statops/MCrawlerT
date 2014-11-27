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

import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.model.Counter;
import net.jaqpot.netcounter.model.Interface;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class CounterDetailsDialog extends CounterDialog {

	private static final String TITLE = "title";

	private final View mView;

	public CounterDetailsDialog(Context context, NetCounterApplication app) {
		super(context, app);

		setTitle(TITLE);

		mView = getLayoutInflater().inflate(R.layout.details, null);
		setView(mView);

		setButton(getText(R.string.close), (OnClickListener) null);
	}

	@Override
	protected void updateContent(Counter counter) {
		Interface inter = counter.getInterface();

		setIcon(counter.getInterface().getIcon());

		setTitle(inter.getPrettyName() + " - " + counter.getTypeAsString());

		String[] bytes = counter.getBytesAsString();
		long[] b = counter.getBytes();

		long sum = b[0] + b[1];
		long p1 = (sum != 0) ? 100 * b[0] / sum : 0;
		long p2 = (sum != 0) ? 100 - p1 : 0;

		TextView tv = (TextView) mView.findViewById(R.id.dlg_start_date);
		tv.setText(counter.getStartDate());
		tv = (TextView) mView.findViewById(R.id.dlg_end_date);
		tv.setText(counter.getEndDate());

		tv = (TextView) mView.findViewById(R.id.dlg_received);
		tv.setText(getString(R.string.dialogDetailsByte, bytes[0], p1));
		tv = (TextView) mView.findViewById(R.id.dlg_sent);
		tv.setText(getString(R.string.dialogDetailsByte, bytes[1], p2));
		tv = (TextView) mView.findViewById(R.id.dlg_total);
		tv.setText(counter.getTotalAsString());

		String alert = getString(R.string.dialogDetailsNoAlert);
		String alertValue = counter.getProperty(Counter.ALERT_VALUE);
		if (alertValue != null) {
			int i = Integer.valueOf(counter.getProperty(Counter.ALERT_UNIT));
			alert = alertValue + " " + NetCounterApplication.BYTE_UNITS[i];
		}

		tv = (TextView) mView.findViewById(R.id.dlg_alert);
		tv.setText(alert);
	}

}
