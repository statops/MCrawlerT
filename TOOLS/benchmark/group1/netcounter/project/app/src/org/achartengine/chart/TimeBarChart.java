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

package org.achartengine.chart;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * The time chart rendering class.
 */
public class TimeBarChart extends BarChart {
	/** The number of milliseconds in a day. */
	public static final long DAY = 24 * 60 * 60 * 1000;
	/** The date format pattern to be used in formatting the X axis labels. */
	private String mDateFormat;

	/**
	 * Builds a new time chart instance.
	 * 
	 * @param dataset
	 *            the multiple series dataset
	 * @param renderer
	 *            the multiple series renderer
	 */
	public TimeBarChart(XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer,
			Type type) {
		super(dataset, renderer, type);
	}

	/**
	 * Returns the date format pattern to be used for formatting the X axis
	 * labels.
	 * 
	 * @return the date format pattern for the X axis labels
	 */
	public String getDateFormat() {
		return mDateFormat;
	}

	/**
	 * Sets the date format pattern to be used for formatting the X axis labels.
	 * 
	 * @param format
	 *            the date format pattern for the X axis labels. If null, an
	 *            appropriate default format will be used.
	 */
	public void setDateFormat(String format) {
		mDateFormat = format;
	}

	/**
	 * The graphical representation of the labels on the X axis.
	 * 
	 * @param xLabels
	 *            the X labels values
	 * @param xTextLabelLocations
	 *            the X text label locations
	 * @param canvas
	 *            the canvas to paint to
	 * @param paint
	 *            the paint to be used for drawing
	 * @param left
	 *            the left value of the labels area
	 * @param bottom
	 *            the bottom value of the labels area
	 * @param xPixelsPerUnit
	 *            the amount of pixels per one unit in the chart labels
	 * @param minX
	 *            the minimum value on the X axis in the chart
	 */
	@Override
	protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
			Paint paint, int left, int bottom, double xPixelsPerUnit, double minX) {
		int length = xLabels.size();
		DateFormat format = getDateFormat(xLabels.get(0), xLabels.get(length - 1));
		for (int i = 0; i < length; i++) {
			long label = Math.round(xLabels.get(i));
			float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
			canvas.drawLine(xLabel, bottom, xLabel, bottom + 4, paint);
			drawText(canvas, format.format(new Date(label)), xLabel, bottom + 12, paint, 0);
		}
	}

	/**
	 * Returns the date format pattern to be used, based on the date range.
	 * 
	 * @param start
	 *            the start date in milliseconds
	 * @param end
	 *            the end date in milliseconds
	 * @return the date format
	 */
	private DateFormat getDateFormat(double start, double end) {
		if (mDateFormat != null) {
			SimpleDateFormat format = null;
			try {
				format = new SimpleDateFormat(mDateFormat);
				return format;
			} catch (Exception e) {
				// do nothing here
			}
		}
		DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
		double diff = end - start;
		if (diff > DAY && diff < 5 * DAY) {
			format = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
					SimpleDateFormat.SHORT);
		} else if (diff < DAY) {
			format = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM);
		}
		return format;
	}
}
