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

import java.util.Calendar;

import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.model.DatabaseHelper;
import net.jaqpot.netcounter.model.NetCounterModel;
import net.jaqpot.netcounter.model.DatabaseHelper.DailyCounter;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 *
 */
public class BarGraphActivity extends Activity implements OnItemSelectedListener {

	private Spinner mSpinner;

	private ViewFlipper mView;

	private GraphWorker mGraphWorker;

	private Cursor mCursor;

	private String mInterface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mInterface = getIntent().getStringExtra(NetCounterApplication.INTENT_EXTRA_INTERFACE);

		setContentView(R.layout.graph);
		mSpinner = (Spinner) findViewById(R.id.spinner);
		mView = (ViewFlipper) findViewById(R.id.graph);

		mSpinner.setOnItemSelectedListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGraphWorker != null) {
			mGraphWorker.cancel(true);
		}
	}

	/**
	 * Builds a bar multiple series dataset using the provided values.
	 * 
	 * @return the XY multiple bar dataset
	 */
	protected XYMultipleSeriesDataset buildBarDataset(int arg) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

		TimeSeries rx = new TimeSeries(getString(R.string.graphReceivedLabel));
		TimeSeries tx = new TimeSeries(getString(R.string.graphSentLabel));

		Cursor c = mCursor;

		for (int i = 0; i < c.getCount(); i++) {
			c.moveToNext();
			String d = c.getString(c.getColumnIndex(DailyCounter.DAY));
			Calendar day = DatabaseHelper.parseDate(d);
			day.set(Calendar.SECOND, 0);
			day.set(Calendar.MINUTE, 0);
			day.set(Calendar.HOUR, 0);
			switch (arg) {
			case 0:
				break;
			default:
				day.set(Calendar.DAY_OF_MONTH, 1);
			}
			long cRx = c.getLong(c.getColumnIndex(DailyCounter.RX));
			long cTx = c.getLong(c.getColumnIndex(DailyCounter.TX));
			rx.add(day.getTime(), cRx / 1024);
			tx.add(day.getTime(), cTx / 1024);
		}

		dataset.addSeries(rx);
		dataset.addSeries(tx);

		return dataset;
	}

	/**
	 * Builds a bar multiple series renderer to use the provided colors.
	 * 
	 * @return the bar multiple series renderer
	 */
	protected XYMultipleSeriesRenderer buildBarRenderer(int arg) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		SimpleSeriesRenderer r = new SimpleSeriesRenderer();
		r.setColor(Color.rgb(185, 190, 221));
		renderer.addSeriesRenderer(r);

		r = new SimpleSeriesRenderer();
		r.setColor(Color.rgb(138, 148, 198));
		renderer.addSeriesRenderer(r);

		renderer.setYTitle(getString(R.string.graphYTitle));

		switch (arg) {
		case 0:
			renderer.setXLabels(15);
			break;
		default:
			renderer.setXLabels(12);
		}

		return renderer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android
	 * .widget.AdapterView, android.view.View, int, long)
	 */
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		NetCounterApplication app = (NetCounterApplication) getApplication();
		NetCounterModel model = app.getAdapter(NetCounterModel.class);

		if (mGraphWorker != null) {
			mGraphWorker.cancel(true);
			mGraphWorker = null;
		}

		if (mCursor != null) {
			stopManagingCursor(mCursor);
			mCursor.close();
		}

		switch ((int) arg3) {
		case 0:
			mCursor = model.getLastDaysCursor(mInterface, 30);
			break;
		default:
			mCursor = model.getLastMonthsCursor(mInterface, 12);
		}

		startManagingCursor(mCursor);

		mGraphWorker = new GraphWorker();
		mGraphWorker.execute((int) arg3);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android
	 * .widget.AdapterView)
	 */
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	private class GraphWorker extends AsyncTask<Integer, Void, GraphicalView> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			// Shows the progress widget.
			mView.setDisplayedChild(0);
			if (mView.getChildCount() > 1) {
				mView.removeViewAt(1);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected GraphicalView doInBackground(Integer... id) {
			XYMultipleSeriesDataset d = buildBarDataset(id[0]);
			XYMultipleSeriesRenderer r = buildBarRenderer(id[0]);

			XYSeries s = d.getSeriesAt(0);
			double diff = s.getMaxX() - s.getMinX();

			String format;
			switch (id[0]) {
			case 0:
				format = "d";
				diff /= 15;
				break;
			default:
				format = "M";
				diff /= 6;
			}

			r.setXAxisMin(s.getMinX() - diff);
			r.setXAxisMax(s.getMaxX() + diff);

			return ChartFactory.getTimeBarChartView(BarGraphActivity.this, d, r, Type.DEFAULT,
					format);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(GraphicalView result) {
			mView.addView(result);
			mView.setDisplayedChild(1);
		}
	}

}
