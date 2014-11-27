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

package net.jaqpot.netcounter.activity;

import net.jaqpot.netcounter.HandlerContainer;
import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.dialog.CounterAlertDialog;
import net.jaqpot.netcounter.dialog.CounterDetailsDialog;
import net.jaqpot.netcounter.dialog.CounterDialog;
import net.jaqpot.netcounter.dialog.CounterDurationDialog;
import net.jaqpot.netcounter.dialog.CounterMonthlyDialog;
import net.jaqpot.netcounter.dialog.CounterTypeDialog;
import net.jaqpot.netcounter.dialog.CounterWeeklyDialog;
import net.jaqpot.netcounter.model.Counter;
import net.jaqpot.netcounter.model.IModel;
import net.jaqpot.netcounter.model.IModelListener;
import net.jaqpot.netcounter.model.IOperation;
import net.jaqpot.netcounter.model.Interface;
import net.jaqpot.netcounter.model.NetCounterModel;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class NetCounterActivity extends ExpandableListActivity implements IModelListener,
		IOperation {

	private static final String EXPANDED_GROUP = "expand.group";

	private ListAdapter mAdapter = null;

	private NetCounterModel mModel = null;

	private static final int DIALOG_HELP = 0;

	private static final int DIALOG_ABOUT = 1;

	private static final int DIALOG_DETAILS = 2;

	private static final int DIALOG_ALERT = 3;

	private static final int DIALOG_TYPE = 4;

	private static final int DIALOG_DURATION = 5;

	private static final int DIALOG_MONTHLY = 6;

	private static final int DIALOG_WEEKLY = 7;

	private static final int CM_INTERFACE_ADD = ContextMenu.FIRST;

	private static final int CM_INTERFACE_RESET = ContextMenu.FIRST + 1;

	private static final int CM_COUNTER_CHANGE = ContextMenu.FIRST + 2;

	private static final int CM_COUNTER_REMOVE = ContextMenu.FIRST + 3;

	private static final int CM_COUNTER_MONTHLY = ContextMenu.FIRST + 4;

	private static final int CM_COUNTER_WEEKLY = ContextMenu.FIRST + 5;

	private static final int CM_COUNTER_ALERT = ContextMenu.FIRST + 6;

	private static final int CM_COUNTER_DURATION = ContextMenu.FIRST + 7;

	private static final int CM_INTERFACE_GRAPH = ContextMenu.FIRST + 8;

	private static final int MENU_HELP = Menu.FIRST;

	private static final int MENU_ABOUT = Menu.FIRST + 1;

	private static final int MENU_REFRESH = Menu.FIRST + 2;

	private static final int MENU_PREFERENCES = Menu.FIRST + 3;

	private Counter mCounter;

	private HandlerContainer mContainer;

	private NetCounterApplication mApp;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_HELP: {
			View v = LayoutInflater.from(this).inflate(R.layout.dialog, null);
			TextView text = (TextView) v.findViewById(R.id.dialogText);
			text.setText(R.string.helpText);
			return new AlertDialog.Builder(this).setTitle(R.string.helpTitle).setIcon(
					android.R.drawable.ic_menu_help).setPositiveButton(R.string.helpClose, null)
					.setView(v).create();
		}
		case DIALOG_ABOUT: {
			AlertDialog.Builder d = new AlertDialog.Builder(this);
			d.setTitle(R.string.aboutTitle);
			d.setIcon(android.R.drawable.ic_menu_info_details);
			d.setPositiveButton(R.string.aboutHomepage, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Uri uri = Uri.parse(getString(R.string.aboutHomepageUrl));
					startActivity(new Intent(Intent.ACTION_VIEW, uri));
				}
			});
			d.setNeutralButton(R.string.aboutDonation, new OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					Uri uri = Uri.parse(getString(R.string.aboutDonationUrl));
					startActivity(new Intent(Intent.ACTION_VIEW, uri));
				}
			});
			d.setNegativeButton(R.string.aboutClose, null);
			View v = LayoutInflater.from(this).inflate(R.layout.dialog, null);
			TextView text = (TextView) v.findViewById(R.id.dialogText);
			text.setText(getString(R.string.aboutText, getVersion()));
			d.setView(v);
			return d.create();
		}
		case DIALOG_DETAILS:
			return new CounterDetailsDialog(this, mApp);
		case DIALOG_ALERT:
			return new CounterAlertDialog(this, mApp);
		case DIALOG_TYPE:
			return new CounterTypeDialog(this, mApp);
		case DIALOG_DURATION:
			return new CounterDurationDialog(this, mApp);
		case DIALOG_MONTHLY:
			return new CounterMonthlyDialog(this, mApp);
		case DIALOG_WEEKLY:
			return new CounterWeeklyDialog(this, mApp);
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		switch (id) {
		case DIALOG_DETAILS:
		case DIALOG_ALERT:
		case DIALOG_TYPE:
		case DIALOG_DURATION:
		case DIALOG_MONTHLY:
		case DIALOG_WEEKLY:
			((CounterDialog) dialog).setCounter(mCounter);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
			int childPosition, long id) {
		mCounter = (Counter) mAdapter.getChild(groupPosition, childPosition);
		showDialog(DIALOG_DETAILS);
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide the title bar.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Set our own layout.
		setContentView(R.layout.main);

		mApp = (NetCounterApplication) getApplication();
		mModel = mApp.getAdapter(NetCounterModel.class);
		mContainer = mApp.getAdapter(HandlerContainer.class);

		// Restores mCounter if needed.
		if (savedInstanceState != null) {
			long[] counter = savedInstanceState.getLongArray("counter");
			if (counter != null) {
				Interface inter = mModel.getInterface(counter[0]);
				if (inter != null) {
					mCounter = inter.getCounter(counter[1]);
				}
			}
		}

		// Display what's new dialog.
		showWhatsNewDialog();

		// Set up our adapter.
		mAdapter = new ListAdapter(this);
		mAdapter.setInput(mModel);
		setListAdapter(mAdapter);

		registerForContextMenu(getExpandableListView());

		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "Activity created.");
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu m, View v, ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int i = ExpandableListView.getPackedPositionType(info.packedPosition);

		if (i == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			// Child.
			m.setHeaderTitle(R.string.menuCounterTitle);
			m.add(0, CM_COUNTER_CHANGE, 0, R.string.menuCounterChange);
			int g = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			int c = ExpandableListView.getPackedPositionChild(info.packedPosition);
			Counter counter = (Counter) mAdapter.getChild(g, c);
			// Monthly and weekly counters have a start value.
			switch (counter.getType()) {
			case Counter.MONTHLY:
			case Counter.LAST_MONTH:
				m.add(0, CM_COUNTER_MONTHLY, 0, R.string.menuCounterStartDate);
				break;
			case Counter.WEEKLY:
				m.add(0, CM_COUNTER_WEEKLY, 0, R.string.menuCounterStartDay);
				break;
			}
			// The only counter that does not have a duration is the total
			// counter.
			if (Counter.TOTAL != counter.getType()) {
				m.add(0, CM_COUNTER_DURATION, 0, R.string.menuCounterSetDuration);
			}
			m.add(0, CM_COUNTER_ALERT, 0, R.string.menuCounterSetAlert);
			m.add(0, CM_COUNTER_REMOVE, 0, R.string.menuCounterRemove);
		} else if (i == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			// Group.
			m.setHeaderTitle(R.string.menuInterfaceTitle);
			m.add(0, CM_INTERFACE_GRAPH, 0, R.string.menuInterfaceGraph);
			m.add(0, CM_INTERFACE_ADD, 0, R.string.menuInterfaceAdd);
			m.add(0, CM_INTERFACE_RESET, 0, R.string.menuInterfaceReset);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo i = (ExpandableListContextMenuInfo) item.getMenuInfo();
		long g = ExpandableListView.getPackedPositionGroup(i.packedPosition);
		final Interface inter = (Interface) mAdapter.getGroup((int) g);

		switch (item.getItemId()) {
		case CM_INTERFACE_ADD: {
			mContainer.getSlowHandler().post(new Runnable() {
				public void run() {
					Counter counter = new Counter(inter);
					inter.addCounter(counter);
					mModel.commit();
				}
			});
			mApp.toast(R.string.menuInterfaceAddDone);
			return true;
		}
		case CM_INTERFACE_RESET: {
			// Creates the dialog.
			AlertDialog.Builder d = new AlertDialog.Builder(this);
			d.setTitle(R.string.dialogInterfaceResetTitle);
			d.setMessage(R.string.dialogInterfaceResetText);
			d.setNegativeButton(R.string.no, null);
			d.setPositiveButton(R.string.yes, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mContainer.getSlowHandler().post(new Runnable() {
						public void run() {
							inter.reset();
							mModel.commit();
						}
					});
					mApp.toast(R.string.menuInterfaceResetDone);
				}
			});
			d.show();

			return true;
		}
		case CM_COUNTER_CHANGE: {
			g = ExpandableListView.getPackedPositionChild(i.packedPosition);
			mCounter = inter.getCounters().get((int) g);
			showDialog(DIALOG_TYPE);
			return true;
		}
		case CM_COUNTER_ALERT: {
			g = ExpandableListView.getPackedPositionChild(i.packedPosition);
			mCounter = inter.getCounters().get((int) g);
			showDialog(DIALOG_ALERT);
			return true;
		}
		case CM_COUNTER_REMOVE: {
			g = ExpandableListView.getPackedPositionChild(i.packedPosition);
			final Counter counter = inter.getCounters().get((int) g);
			mContainer.getSlowHandler().post(new Runnable() {
				public void run() {
					counter.getInterface().removeCounter(counter);
					mModel.commit();
				}
			});
			mApp.toast(R.string.menuCounterRemoveDone);
			return true;
		}
		case CM_COUNTER_MONTHLY: {
			g = ExpandableListView.getPackedPositionChild(i.packedPosition);
			mCounter = inter.getCounters().get((int) g);
			showDialog(DIALOG_MONTHLY);
			return true;
		}
		case CM_COUNTER_WEEKLY: {
			g = ExpandableListView.getPackedPositionChild(i.packedPosition);
			mCounter = inter.getCounters().get((int) g);
			showDialog(DIALOG_WEEKLY);
			return true;
		}
		case CM_COUNTER_DURATION: {
			g = ExpandableListView.getPackedPositionChild(i.packedPosition);
			mCounter = inter.getCounters().get((int) g);
			showDialog(DIALOG_DURATION);
			return true;
		}
		case CM_INTERFACE_GRAPH: {
			Intent intent = new Intent(this, BarGraphActivity.class);
			intent.putExtra(NetCounterApplication.INTENT_EXTRA_INTERFACE, inter.getName());
			startActivity(intent);
			return true;
		}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Help.
		MenuItem item = menu.add(0, MENU_HELP, 0, R.string.helpTitle);
		item.setIcon(android.R.drawable.ic_menu_help);
		// Refresh.
		item = menu.add(0, MENU_REFRESH, 0, R.string.appRefresh);
		item.setIcon(android.R.drawable.ic_menu_rotate);
		// Preferences.
		item = menu.add(0, MENU_PREFERENCES, 0, R.string.preferences);
		item.setIcon(android.R.drawable.ic_menu_preferences);
		// About.
		item = menu.add(0, MENU_ABOUT, 0, R.string.aboutTitle);
		item.setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_HELP:
			showDialog(DIALOG_HELP);
			return true;
		case MENU_ABOUT:
			showDialog(DIALOG_ABOUT);
			return true;
		case MENU_REFRESH:
			NetCounterApplication app = (NetCounterApplication) getApplication();
			app.startService();
			return true;
		case MENU_PREFERENCES:
			startActivity(new Intent(this, NetCounterPreferences.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		setListAdapter(null);
		mAdapter.setInput(null);
		unregisterForContextMenu(getExpandableListView());

		mApp = null;
		mModel = null;
		mContainer = null;
		mAdapter = null;
		mCounter = null;

		super.onDestroy();

		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "Activity destroyed.");
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		NetCounterApplication.setUpdatePolicy(NetCounterApplication.SERVICE_LOW);
		NetCounterApplication app = (NetCounterApplication) getApplication();
		app.startService();

		mModel.removeModelListener(this);
		mModel.removeOperationListener(this);

		saveListState();

		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "Activity paused.");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		NetCounterApplication.setUpdatePolicy(NetCounterApplication.SERVICE_HIGH);
		NetCounterApplication app = (NetCounterApplication) getApplication();
		app.startService();

		mModel.addModelListener(this);
		mModel.addOperationListener(this);

		if (mModel.isLoaded()) {
			restoreListState();
			animateList();
		}

		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "Activity resumed.");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mCounter != null) {
			long interfaceId = mCounter.getInterface().getId();
			long counterId = mCounter.getId();
			outState.putLongArray("counter", new long[] { interfaceId, counterId });
		}
		if (NetCounterApplication.LOG_ENABLED) {
			Log.d(getClass().getName(), "onSaveInstanceState");
		}
	}

	/**
	 * Returns the version of the application.
	 * 
	 * @return The version.
	 */
	private String getVersion() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// Ignore.
		}
		return "";
	}

	private void saveListState() {
		SharedPreferences prefs = mApp.getAdapter(SharedPreferences.class);
		ExpandableListView v = getExpandableListView();
		Editor edit = prefs.edit();
		int size = mAdapter.getGroupCount();
		for (int i = 0; i < size; i++) {
			edit.putBoolean(EXPANDED_GROUP + i, v.isGroupExpanded(i));
		}
		edit.commit();
	}

	private void restoreListState() {
		SharedPreferences pref = mApp.getAdapter(SharedPreferences.class);
		ExpandableListView v = getExpandableListView();
		for (int i = 0; i < mAdapter.getGroupCount(); i++) {
			if (pref.getBoolean(EXPANDED_GROUP + i, false)) {
				v.expandGroup(i);
			} else {
				v.collapseGroup(i);
			}
		}
	}

	private void animateList() {
		AnimationSet set = new AnimationSet(true);

		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(150);
		set.addAnimation(animation);

		animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f);
		animation.setDuration(150);
		set.addAnimation(animation);

		LayoutAnimationController c = new LayoutAnimationController(set, 0.5f);
		getExpandableListView().setLayoutAnimation(c);
	}

	/**
	 * Displays the What's New dialog if it is the first time the user starts the new version.
	 */
	private void showWhatsNewDialog() {
		final SharedPreferences preferences = mApp.getAdapter(SharedPreferences.class);
		final String current = getVersion();
		// preferences.edit().remove("version").commit();
		String version = preferences.getString("version", "");
		// Only shows the dialog if the version strings are different.
		if (!version.equals(current)) {
			Builder dialog = new Builder(this);
			dialog.setTitle(R.string.appWhatsNew);
			dialog.setMessage(R.string.helpWhatsNew);
			dialog.setNeutralButton(R.string.close, new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					preferences.edit().putString("version", current).commit();
				}
			});
			dialog.show();
		}
	}

	public void modelLoaded(IModel object) {
		runOnUiThread(new Runnable() {
			public void run() {
				restoreListState();
				animateList();
			}
		});
	}

	public void modelChanged(IModel object) {
		runOnUiThread(new Runnable() {
			public void run() {
				mAdapter.notifyDataSetChanged();
			}
		});
	}

	public void operationStarted() {
		runOnUiThread(new Runnable() {
			public void run() {
				getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
						Window.PROGRESS_VISIBILITY_ON);
			}
		});
	}

	public void operationEnded() {
		runOnUiThread(new Runnable() {
			public void run() {
				getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
						Window.PROGRESS_VISIBILITY_OFF);
			}
		});
	}

}