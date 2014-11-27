/**
 * Copyright (C) 2009 SC 4ViewSoft SRL
 * Copyright (C) 2009 Cyril Jaquier
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.achartengine;

import org.achartengine.chart.BarChart;
import org.achartengine.chart.TimeBarChart;
import org.achartengine.chart.XYChart;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;

/**
 * Utility methods for creating chart views or intents.
 */
public class ChartFactory {
	/** The key for the chart data. */
	public static final String CHART = "chart";

	/** The key for the chart graphical activity title. */
	public static final String TITLE = "title";

	private ChartFactory() {
		// empty for now
	}

	/**
	 * Creates a bar chart view.
	 * 
	 * @param context
	 *            the context
	 * @param dataset
	 *            the multiple series dataset (cannot be null)
	 * @param renderer
	 *            the multiple series renderer (cannot be null)
	 * @param type
	 *            the bar chart type
	 * @return a bar chart graphical view
	 * @throws IllegalArgumentException
	 *             if dataset is null or renderer is null or if the dataset and
	 *             the renderer don't include the same number of series
	 */
	public static final GraphicalView getBarChartView(Context context,
			XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, Type type) {
		checkParameters(dataset, renderer);
		XYChart chart = new BarChart(dataset, renderer, type);
		return new GraphicalView(context, chart);
	}

	public static final GraphicalView getTimeBarChartView(Context context,
			XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, Type type,
			String format) {
		checkParameters(dataset, renderer);
		TimeBarChart chart = new TimeBarChart(dataset, renderer, type);
		chart.setDateFormat(format);
		return new GraphicalView(context, chart);
	}

	/**
	 * Checks the validity of the dataset and renderer parameters.
	 * 
	 * @param dataset
	 *            the multiple series dataset (cannot be null)
	 * @param renderer
	 *            the multiple series renderer (cannot be null)
	 * @throws IllegalArgumentException
	 *             if dataset is null or renderer is null or if the dataset and
	 *             the renderer don't include the same number of series
	 */
	private static void checkParameters(XYMultipleSeriesDataset dataset,
			XYMultipleSeriesRenderer renderer) {
		if (dataset == null || renderer == null
				|| dataset.getSeriesCount() != renderer.getSeriesRendererCount()) {
			throw new IllegalArgumentException(
					"Dataset and renderer should be not null and should have the same number of series");
		}
	}

}
