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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class CounterAlertDialog extends CounterDialog implements OnClickListener {

	private final HandlerContainer mContainer;

	private final NetCounterModel mModel;

	private final EditText mText;

	private final Spinner mSpinner;

	public CounterAlertDialog(Context context, NetCounterApplication app) {
		super(context, app);

		mContainer = (HandlerContainer) app.getAdapter(HandlerContainer.class);
		mModel = (NetCounterModel) app.getAdapter(NetCounterModel.class);

		setTitle(R.string.dialogCounterLimitTitle);

		View view = getLayoutInflater().inflate(R.layout.limit, null);
		mText = (EditText) view.findViewById(R.id.limit);
		mSpinner = (Spinner) view.findViewById(R.id.spinner);
		setView(view);

		setButton(getText(R.string.ok), this);
		setButton2(getText(R.string.cancel), (OnClickListener) null);
	}

	@Override
	protected void updateContent(Counter counter) {
		String value = counter.getProperty(Counter.ALERT_VALUE);
		if (value == null) {
			mText.setText("");
			mSpinner.setSelection(0);
		} else {
			mText.setText(value);
			value = counter.getProperty(Counter.ALERT_UNIT);
			mSpinner.setSelection(Integer.valueOf(value));
		}
	}

	public void onClick(DialogInterface dialog, int which) {
		String text = mText.getText().toString();
		if (text.length() != 0) {
			try {
				setNewValues(text);
			} catch (NumberFormatException e) {
				getApplication().toast(R.string.dialogCounterLimitWrong);
			}
		} else {
			resetValues();
		}
	}

	private void setNewValues(String text) {
		final double value = Double.valueOf(text);
		int[] bValues = getResources().getIntArray(R.array.byteValues);

		final int position = mSpinner.getSelectedItemPosition();
		final long bytes = Math.round(value * bValues[position]);

		mContainer.getSlowHandler().post(new Runnable() {
			public void run() {
				Counter counter = getCounter();
				String v = String.valueOf(value);
				counter.setProperty(Counter.ALERT_VALUE, v);
				v = String.valueOf(position);
				counter.setProperty(Counter.ALERT_UNIT, v);
				v = String.valueOf(bytes);
				counter.setProperty(Counter.ALERT_BYTES, v);
				mModel.commit();
				// Refresh.
				mContainer.getGuiHandler().post(new Runnable() {
					public void run() {
						getApplication().startService();
					}
				});
			}
		});
	}

	private void resetValues() {
		mContainer.getSlowHandler().post(new Runnable() {
			public void run() {
				Counter counter = getCounter();
				counter.removeProperty(Counter.ALERT_VALUE);
				counter.removeProperty(Counter.ALERT_UNIT);
				counter.removeProperty(Counter.ALERT_BYTES);
				mModel.commit();
				// Refresh.
				mContainer.getGuiHandler().post(new Runnable() {
					public void run() {
						getApplication().startService();
					}
				});
			}
		});
	}

}
