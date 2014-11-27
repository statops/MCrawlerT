/**
 * aagtl Advanced Geocaching Tool for Android
 * loosely based on agtl by Daniel Fett <fett@danielfett.de>
 * Copyright (C) 2010 - 2012 Zoff <aagtl@work.zoff.cc>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.aagtl;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.widget.ImageView;
import bpi.sdbm.illuminance.SolarPosition;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.calculator.SolarEventCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location2;

public class ArrowView extends ImageView
{

	Paint									arrow_paint					= new Paint(0);
	Paint									text_paint					= new Paint(0);

	aagtl									main_aagtl;

	long									mLastCalcSunMillis		= -1;
	public double						azmiuth_cache				= -1;
	public double						zenith_cache				= -1;
	public String						sunrise_cache				= "";
	public String						sunset_cache				= "";
	public double						elevation					= 0;

	public double						moon_azimuth_cache		= -1;
	public double						moon_evelation_cache		= -1;

	Boolean								must_calc_new				= true;
	SunriseSunsetCalculator			calc							= null;
	Calendar								cx								= null;
	SolarPosition.SunCoordinates	sc								= null;

	int									COLOR_QUALITY_INNER		= Color.parseColor("#348017");
	int									COLOR_QUALITY_OUTER		= Color.parseColor("#150517");
	int									COLOR_ARROW_ATTARGET		= Color.parseColor("#C11B17");
	int									COLOR_ARROW_NEAR			= Color.parseColor("#F87217");
	int									COLOR_ARROW_DEFAULT		= Color.parseColor("#348017");
	int									COLOR_ARROW_OUTER_LINE	= Color.parseColor("#150517");
	int									COLOR_CIRCLE_OUTLINE		= Color.parseColor("#424242");
	int									DISTANCE_DISABLE_ARROW	= 2;										// 2 meters
	int									NORTH_INDICATOR_SIZE		= 20;									// 20 pixels

	double								ARROW_OFFSET				= 1.0 / 3.0;							// Offset to center of arrow, calculated as 2-x = sqrt(1^2+(x+1)^2)

	public ArrowView(Context context, aagtl main_aagtl)
	{
		super(context);

		text_paint.setColor(Color.WHITE);
		//text_paint.setStyle(Paint.Style.FILL);
		text_paint.setTextSize(19);
		text_paint.setTypeface(Typeface.DEFAULT_BOLD);
		text_paint.setAntiAlias(true);

		this.main_aagtl = main_aagtl;
		this.clear_stuff();
	}

	public String roundTwoDecimals(double d)
	{
		return String.format("%.2f", d);
	}

	public void calc_sun_stats()
	{
		//
		//
		// SUN ----------------
		//
		//
		this.must_calc_new = (SystemClock.elapsedRealtime() - mLastCalcSunMillis) > 60000; // every 60 seconds calc new

		if ((this.must_calc_new) || (this.azmiuth_cache == -1))
		{
			this.mLastCalcSunMillis = SystemClock.elapsedRealtime();
			TimeZone t = TimeZone.getDefault();
			//System.out.println(t.getID());
			calc = new SunriseSunsetCalculator(new Location2(String
					.valueOf(this.main_aagtl.global_settings.map_position_lat), String
					.valueOf(this.main_aagtl.global_settings.map_position_lon)), t.getID());
			cx = Calendar.getInstance();
			sc = SolarPosition.getSunPosition(new Date(),
					this.main_aagtl.global_settings.map_position_lat,
					this.main_aagtl.global_settings.map_position_lon);

			this.azmiuth_cache = sc.azimuth;
			this.zenith_cache = sc.zenithAngle;
			this.sunrise_cache = calc.getOfficialSunriseForDate(cx);
			this.sunset_cache = calc.getOfficialSunsetForDate(cx);
			//System.out.println("calc moon");
			SolarEventCalculator.moonCoor_ret moon_stats = calc.computeMoon(cx);
			moon_azimuth_cache = moon_stats.az;
			moon_evelation_cache = moon_stats.alt;
		}
		//
		this.elevation = 90 - this.zenith_cache;
		//
		// SUN ----------------
		//
		//
	}

	public void onDraw(Canvas c)
	{
		//System.out.println("ArrowView: onDraw");


		// draw grey circle around the arrow
		// draw grey circle around the arrow
		int indicator_radius = (int) ((Math.min(this.getWidth(), this.getHeight()) / 4) * 1.1f);
		int indicator_dist = (Math.min(this.getHeight(), this.getWidth()) / 2) - indicator_radius / 2;
		int[] center = new int[2];
		center[0] = this.getWidth() / 2;
		center[1] = this.getHeight() / 2;

		RectF coord_rect = new RectF(center[0] - indicator_dist, center[1] - indicator_dist,
				center[0] - indicator_dist + indicator_dist * 2, center[1] - indicator_dist
						+ indicator_dist * 2);

		int start = 0; // 0°
		int end = 360; // 360°
		arrow_paint.setAntiAlias(true);
		arrow_paint.setColor(COLOR_CIRCLE_OUTLINE);
		arrow_paint.setStyle(Paint.Style.STROKE);
		arrow_paint.setStrokeWidth(3);
		c.drawArc(coord_rect, start, end, false, arrow_paint);
		// draw grey circle around the arrow
		// draw grey circle around the arrow


		Coordinate my_pos = new Coordinate(this.main_aagtl.global_settings.map_position_lat,
				this.main_aagtl.global_settings.map_position_lon);

		double display_distance = 0;
		double display_bearing = 0;
		if (this.main_aagtl.rose.current_target != null)
		{
			display_distance = my_pos.distance_to((Coordinate) (this.main_aagtl.rose.current_target));

			display_bearing = my_pos.bearing_to((Coordinate) (this.main_aagtl.rose.current_target))
					- this.main_aagtl.cross.gps_heading;
		}

		//if ((this.main_aagtl.cross.gps_heading != -1) && (this.main_aagtl.isGPSFix))
		if (this.main_aagtl.cross.gps_heading != -1)
		{
			double display_north = Math.toRadians(this.main_aagtl.cross.gps_heading);
			int position_x = (int) (this.getWidth() / 2 - Math.sin(display_north)
					* (indicator_dist * 1.15));
			int position_y = (int) (this.getHeight() / 2 - Math.cos(display_north)
					* (indicator_dist * 1.15));

			text_paint.setColor(Color.WHITE);
			c.drawText("N", position_x, position_y, text_paint);
		}

		text_paint.setColor(Color.WHITE);
		c.drawText(
				"distance to target: " + String.valueOf(roundTwoDecimals(display_distance)) + " m", 10,
				34, text_paint);

		c.drawText("target: " + String.valueOf((int) display_bearing) + " °", 10, 2 * 34, text_paint);

		c.drawText("my heading: " + String.valueOf((int) this.main_aagtl.cross.gps_heading) + " °",
				this.getWidth() / 2, 2 * 34, text_paint);

		c.drawText("gps accuracy: " + String.valueOf(roundTwoDecimals(this.main_aagtl.cross.gps_acc))
				+ " m", 10, 3 * 34, text_paint);
		c.drawText("gps Sats.: " + String.valueOf((int) this.main_aagtl.cross.used_sats), 10, 4 * 34,
				text_paint);


		//
		//
		// SUN ----------------
		//
		//
		this.calc_sun_stats();
		c.drawText("sunrise: " + this.sunrise_cache, 10, this.getHeight() - 34 * 4, text_paint);
		c.drawText("sunset: " + this.sunset_cache, 10, this.getHeight() - 34 * 3, text_paint);


		if (elevation < -0.83)
		{
			c.drawText("elevation: *night*", this.getWidth() / 2, this.getHeight() - 34 * 4,
					text_paint);
		}
		else
		{
			c.drawText("elevation: " + roundTwoDecimals(elevation), this.getWidth() / 2, this
					.getHeight() - 34 * 4, text_paint);
		}

		c.drawText("azimuth: " + roundTwoDecimals(this.azmiuth_cache), this.getWidth() / 2, this
				.getHeight() - 34 * 3, text_paint);
		//
		//
		// SUN ----------------
		//
		//


		if (this.main_aagtl.rose.current_target != null)
		{
			// gc-code
			c.drawText("Geocache: " + this.main_aagtl.rose.current_target.name, 10,
					this.getHeight() - 34 * 2, text_paint);
			// cache name
			c.drawText(this.main_aagtl.rose.current_target.title, 10, this.getHeight() - 34,
					text_paint);
		}

		// draw signal indicator , part 1
		int signal_width = 15;
		RectF coord_rect3 = new RectF(this.getWidth() - signal_width - 2, 0, this.getWidth(), this
				.getHeight());
		arrow_paint.setColor(COLOR_QUALITY_OUTER);
		arrow_paint.setStyle(Paint.Style.STROKE);
		arrow_paint.setStrokeWidth(3);
		c.drawRect(coord_rect3, arrow_paint);

		// draw signal indicator , part 2
		int usable_height = this.getHeight() - 1;
		int target_height = (int) (usable_height * ((float) (this.main_aagtl.cross.used_sats) / (float) (13)));
		RectF coord_rect7 = new RectF(this.getWidth() - signal_width - 1, usable_height
				- target_height, this.getWidth() - 1, usable_height);
		arrow_paint.setColor(COLOR_QUALITY_INNER);
		arrow_paint.setStyle(Paint.Style.FILL);
		c.drawRect(coord_rect7, arrow_paint);


		if (this.main_aagtl.rose.current_target != null)
		{
			arrow_paint.setStrokeWidth(1);
			int arrow_color;
			if (display_distance < 50)
			{
				arrow_color = COLOR_ARROW_ATTARGET;
			}
			else if (display_distance < 150)
			{
				arrow_color = COLOR_ARROW_NEAR;
			}
			else
			{
				arrow_color = COLOR_ARROW_DEFAULT;
			}

			if (display_distance > DISTANCE_DISABLE_ARROW)
			{
				//c.drawLines(pts, paint)	;
				double[] arrow_transformed = new double[8];
				arrow_transformed = this.__get_arrow_transformed(0, 0, this.getWidth(), this
						.getHeight(), display_bearing, 0.22);
				Path p = new Path();
				for (int i = 0; i < 4; i++)
				{
					if (i == 0)
					{
						p.moveTo((float) arrow_transformed[(i + 1) * 2 - 2],
								(float) arrow_transformed[(i + 1) * 2 - 1]);
					}
					else
					{
						p.lineTo((float) arrow_transformed[(i + 1) * 2 - 2],
								(float) arrow_transformed[(i + 1) * 2 - 1]);
					}
				}
				p.close();
				arrow_paint.setStyle(Paint.Style.FILL);
				arrow_paint.setAntiAlias(true);
				arrow_paint.setColor(arrow_color);
				c.drawPath(p, arrow_paint);
			}
			else
			{
				double circle_size = Math.max(this.getHeight() / 2.5, this.getWidth() / 2.5);
				RectF coord_rect2 = new RectF((int) (this.getWidth() / 2 - circle_size / 2),
						(int) (this.getHeight() / 2 - circle_size / 2),
						(int) (this.getWidth() / 2 + circle_size / 2),
						(int) (this.getHeight() / 2 + circle_size / 2));
				arrow_paint.setStyle(Paint.Style.FILL);
				arrow_paint.setAntiAlias(true);
				arrow_paint.setColor(arrow_color);
				c.drawArc(coord_rect2, 0, 360, false, arrow_paint);
			}
		}
	}

	public double[] __get_arrow_transformed(int x1, int y1, int width, int height, double angle,
			double size_offset)
	{

		double[] ARROW_SHAPE = new double[8];
		ARROW_SHAPE[0] = 0;
		ARROW_SHAPE[1] = -2 + this.ARROW_OFFSET;
		ARROW_SHAPE[2] = 1;
		ARROW_SHAPE[3] = 1 + this.ARROW_OFFSET;
		ARROW_SHAPE[4] = 0;
		ARROW_SHAPE[5] = 0 + this.ARROW_OFFSET;
		ARROW_SHAPE[6] = -1;
		ARROW_SHAPE[7] = 1 + this.ARROW_OFFSET;
		//	   ARROW_SHAPE[0]=1.1;
		//	 //  ARROW_SHAPE=(0, -2 + this.ARROW_OFFSET, 1, + 1 + this.ARROW_OFFSET
		// , 0, 0 + this.ARROW_OFFSET, -1, 1 + this.ARROW_OFFSET);


		double multiply = (Math.min(width, height) * 0.7) / (2 * (this.ARROW_OFFSET)) * size_offset;
		double offset_x = width / 2;
		double offset_y = height / 2;

		double s = Math.sin(Math.toRadians(angle));
		double c = Math.cos(Math.toRadians(angle));

		double[] arrow_transformed = new double[8];

		for (int i = 0; i < 4; i++)
		{
			double x;
			double y;
			x = ARROW_SHAPE[(i + 1) * 2 - 2];
			y = ARROW_SHAPE[(i + 1) * 2 - 1];
			arrow_transformed[(i + 1) * 2 - 2] = x * multiply * c + offset_x - y * multiply * s;
			arrow_transformed[(i + 1) * 2 - 1] = y * multiply * c + offset_y + x * multiply * s;
		}
		return arrow_transformed;
	}

	public void clear_stuff()
	{
		//
	}

	public void onSizeChanged(int width, int height, int old_w, int old_h)
	{
		//System.out.println("ArrowView: onSizeChanged");
	}

}
