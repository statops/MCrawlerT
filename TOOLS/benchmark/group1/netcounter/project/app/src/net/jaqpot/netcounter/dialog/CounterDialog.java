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
import net.jaqpot.netcounter.model.Counter;
import net.jaqpot.netcounter.model.Interface;
import net.jaqpot.netcounter.model.NetCounterModel;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

public abstract class CounterDialog extends AlertDialog {

	private static final String COUNTER_ID = "counterId";

	private static final String INTERFACE_ID = "interfaceId";

	private final NetCounterApplication mApp;

	private final NetCounterModel mModel;

	private long mInterfaceId;

	private long mCounterId;

	private Counter mCounter;

	public CounterDialog(Context context, NetCounterApplication app) {
		super(context);
		mApp = app;
		mModel = mApp.getAdapter(NetCounterModel.class);
	}

	protected NetCounterApplication getApplication() {
		return mApp;
	}

	public void setCounter(Counter counter) {
		assert counter != null;
		Log.i(getClass().getName(), "Model is loaded?: " + mModel.isLoaded());
		mInterfaceId = counter.getInterface().getId();
		mCounterId = counter.getId();

		mCounter = counter;

		updateContent(counter);
	}

	protected abstract void updateContent(Counter counter);

	protected Counter getCounter() {
		return mCounter;
	}

	@Override
	public Bundle onSaveInstanceState() {
		Bundle bundle = super.onSaveInstanceState();
		bundle.putLong(INTERFACE_ID, mInterfaceId);
		bundle.putLong(COUNTER_ID, mCounterId);
		return bundle;
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mInterfaceId = savedInstanceState.getLong(INTERFACE_ID);
		mCounterId = savedInstanceState.getLong(COUNTER_ID);

		Interface inter = mModel.getInterface(mInterfaceId);
		mCounter = inter.getCounter(mCounterId);

		updateContent(mCounter);
	}

	public String getString(int id, Object... formatArgs) {
		return getContext().getResources().getString(id, formatArgs);
	}

	public CharSequence getText(int id) {
		return getContext().getResources().getText(id);
	}

	public Resources getResources() {
		return getContext().getResources();
	}
}
