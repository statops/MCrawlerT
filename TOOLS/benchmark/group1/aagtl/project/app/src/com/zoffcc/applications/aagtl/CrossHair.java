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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.widget.ImageView;

public class CrossHair extends ImageView
{
	int		middle_size	= 10;
	double	gps_acc		= -1;
	double	gps_heading	= -1;
	int		used_sats	= 0;

	aagtl		main_aagtl;

	public CrossHair(Context context, aagtl main)
	{
		super(context);
		this.main_aagtl = main;
	}

	public void set_used_sats(int count)
	{
		this.used_sats = count;
	}

	public void set_gps_heading(double degrees)
	{
		this.gps_heading = degrees;
		if (this.main_aagtl.global_settings.options_turn_map_on_heading)
		{
			// user wants map to turn with heading
			// so draw map
			//this.main_aagtl.rose.invalidate();
			//System.out.println("iigcYYYYY");
			this.main_aagtl.rose.draw_view();
		}
	}

	public double get_gps_heading()
	{
		return this.gps_heading;
	}

	public void set_gps_acc(double meters)
	{
		this.gps_acc = meters;
	}

	public void onDraw(Canvas c)
	{
		//System.out.println("CrossHair: onDraw");

		Paint paint = new Paint(0);
		paint.setAntiAlias(false);
		paint.setColor(Color.GRAY);
		c.drawLine(this.getWidth() / 2, 0, this.getWidth() / 2, this.getHeight() / 2 - middle_size,
				paint);
		c.drawLine(this.getWidth() / 2, this.getHeight() / 2 + middle_size, this.getWidth() / 2, this
				.getHeight(), paint);

		c.drawLine(0, this.getHeight() / 2, this.getWidth() / 2 - middle_size, this.getHeight() / 2,
				paint);
		c.drawLine(this.getWidth() / 2 + middle_size, this.getHeight() / 2, this.getWidth(), this
				.getHeight() / 2, paint);


		// -- SUN --
		// -- SUN --
		// -- SUN --
		// set correct sun values
		this.main_aagtl.arrowview.calc_sun_stats();

		//c.drawText("sunrise: " + this.sunrise_cache, 10, this.getHeight() - 34 * 4, text_paint);
		//c.drawText("sunset: " + this.sunset_cache, 10, this.getHeight() - 34 * 3, text_paint);

		if (this.main_aagtl.arrowview.moon_evelation_cache < -0.83)
		{
			// moon not visible
		}
		else
		{
			// moon is visible!!
			double x1 = this.getWidth() / 2
					+ Math.sin(Math.toRadians(this.main_aagtl.arrowview.moon_azimuth_cache))
					* (this.getWidth() / 2) * 0.9;
			double y1 = this.getHeight() / 2
					- Math.cos(Math.toRadians(this.main_aagtl.arrowview.moon_azimuth_cache))
					* (this.getHeight() / 2) * 0.9;

			int radius_x = 6;
			int radius_y = 6;
			RectF sun_oval = null;
			// day

			// draw yellow sun
			Paint paint_gps = new Paint(0);
			paint_gps.setColor(Color.parseColor("#EBEBEB"));
			paint_gps.setStyle(Paint.Style.FILL);
			paint_gps.setAntiAlias(true);
			radius_x = 6;
			radius_y = 6;
			sun_oval = new RectF((int) (x1 - radius_x), (int) (y1 - radius_y), (int) (x1 + radius_x),
					(int) (y1 + radius_y));
			c.drawArc(sun_oval, 0, 360, false, paint_gps);
			//
			// draw black circle
			paint_gps.setColor(Color.parseColor("#000000"));
			paint_gps.setStyle(Paint.Style.STROKE);
			paint_gps.setAntiAlias(true);
			paint_gps.setStrokeWidth(1);
			radius_x = 6;
			radius_y = 6;
			sun_oval = new RectF((int) (x1 - radius_x), (int) (y1 - radius_y), (int) (x1 + radius_x),
					(int) (y1 + radius_y));
			c.drawArc(sun_oval, 0, 360, false, paint_gps);

		}

		if (this.main_aagtl.arrowview.elevation < -0.83)
		{
			// night
		}
		else
		{
			double x1 = this.getWidth() / 2
					+ Math.sin(Math.toRadians(this.main_aagtl.arrowview.azmiuth_cache))
					* (this.getWidth() / 2) * 0.9;
			double y1 = this.getHeight() / 2
					- Math.cos(Math.toRadians(this.main_aagtl.arrowview.azmiuth_cache))
					* (this.getHeight() / 2) * 0.9;

			int radius_x = 6;
			int radius_y = 6;
			RectF sun_oval = null;
			// day

			// draw yellow sun
			Paint paint_gps = new Paint(0);
			paint_gps.setColor(Color.parseColor("#FFFF66"));
			paint_gps.setStyle(Paint.Style.FILL);
			paint_gps.setAntiAlias(true);
			radius_x = 6;
			radius_y = 6;
			sun_oval = new RectF((int) (x1 - radius_x), (int) (y1 - radius_y), (int) (x1 + radius_x),
					(int) (y1 + radius_y));
			c.drawArc(sun_oval, 0, 360, false, paint_gps);
			//
			// draw black circle
			paint_gps.setColor(Color.parseColor("#000000"));
			paint_gps.setStyle(Paint.Style.STROKE);
			paint_gps.setAntiAlias(true);
			paint_gps.setStrokeWidth(1);
			radius_x = 6;
			radius_y = 6;
			sun_oval = new RectF((int) (x1 - radius_x), (int) (y1 - radius_y), (int) (x1 + radius_x),
					(int) (y1 + radius_y));
			c.drawArc(sun_oval, 0, 360, false, paint_gps);
		}

		// -- SUN --
		// -- SUN --
		// -- SUN --


		if (this.main_aagtl.isGPSFix)
		{
			if (!this.main_aagtl.global_settings.options_turn_map_on_heading)
			{
				// have gps fix
				if (this.gps_acc != -1)
				{
					Paint paint_gps = new Paint(0);
					paint_gps.setColor(Color.parseColor("#0000AA"));
					paint_gps.setStyle(Paint.Style.STROKE);
					paint_gps.setAntiAlias(true);

					// calc gps_acc (in meters) into pixels (in current zoomlevel)
					// zoom = 19 -> tile_size = 100/2m  =50*1m
					// zoom = 18 -> tile_size = 100m    =50*2m
					// zoom = 17 -> tile_size = 100*2m  =50*4m
					int temp = 19 - this.main_aagtl.rose.zoom;
					double n = (1 << temp);
					int radius_x = (int) (gps_acc * (this.main_aagtl.rose.tile_size_x / (50 * n)));
					int radius_y = (int) (gps_acc * (this.main_aagtl.rose.tile_size_y / (50 * n)));

					//System.out.println("acc radius x=" + radius_x);
					//System.out.println("acc radius y=" + radius_y);
					//System.out.println("zoom=" + this.main_aagtl.rose.zoom);
					//System.out.println("gps acc=" + gps_acc);
					//System.out.println("n=" + n);


					RectF gps_oval = new RectF(this.getWidth() / 2 - (int) radius_x, this.getHeight()
							/ 2 - (int) radius_y, this.getWidth() / 2 + (int) radius_x, this.getHeight()
							/ 2 + (int) radius_y);
					c.drawArc(gps_oval, 0, 360, false, paint_gps);


					// show the heading on map
					//System.out.println("-> heading=" + this.gps_heading);
					int size = 16;
					double s1 = Math.sin(Math.toRadians(this.gps_heading));
					double c1 = Math.cos(Math.toRadians(this.gps_heading));
					double radius_x2 = (this.getWidth() / 2) - 34;
					double radius_y2 = (this.getHeight() / 2) - 34;
					double[] my_shape = {0.7, +0, 0, -1, -0.7, 0};

					Path p = new Path();
					for (int i = 0; i < 3; i++)
					{
						if (i == 0)
						{
							//System.out.println("" + my_shape[(i + 1) * 2 - 2]);
							//System.out.println("" + my_shape[(i + 1) * 2 - 1]);
							p.moveTo((float) (my_shape[(i + 1) * 2 - 2] * size * c1 + this.getWidth() / 2
									- my_shape[(i + 1) * 2 - 1] * size * s1 + s1 * radius_x2),
									(float) (my_shape[(i + 1) * 2 - 1] * size * c1 + this.getHeight() / 2
											+ my_shape[(i + 1) * 2 - 2] * size * s1 - c1 * radius_y2));
						}
						else
						{
							//System.out.println(""
							//		+ (my_shape[(i + 1) * 2 - 2] * size * c1 + this.getWidth() / 2
							//				- my_shape[(i + 1) * 2 - 1] * size * s1 + s1 * radius));
							//System.out.println(""
							//		+ ((my_shape[(i + 1) * 2 - 1] * size * c1 + this.getHeight() / 2
							//				+ my_shape[(i + 1) * 2 - 2] * size * s1 - c1 * radius)));
							p.lineTo((float) (my_shape[(i + 1) * 2 - 2] * size * c1 + this.getWidth() / 2
									- my_shape[(i + 1) * 2 - 1] * size * s1 + s1 * radius_x2),
									(float) (my_shape[(i + 1) * 2 - 1] * size * c1 + this.getHeight() / 2
											+ my_shape[(i + 1) * 2 - 2] * size * s1 - c1 * radius_y2));
						}
					}
					p.close();
					paint_gps.setStyle(Paint.Style.STROKE);
					paint_gps.setStrokeWidth(4);
					paint_gps.setColor(Color.parseColor("#00EE00"));
					c.drawPath(p, paint_gps);
					paint_gps.setStrokeWidth(2);
					paint_gps.setColor(Color.parseColor("#157DEC"));
					c.drawPath(p, paint_gps);

					paint_gps.setTextSize(14);
					paint_gps.setStrokeWidth(1);
					paint_gps.setColor(Color.parseColor("#00EE00"));
					c.drawText("*gps fix* (" + this.used_sats + ")", this.getWidth() - 90, this
							.getHeight() - 17, paint_gps);
				}
			}
		}
		else
		{
			if (!this.main_aagtl.global_settings.options_turn_map_on_heading)
			{
				if (this.main_aagtl.global_settings.options_use_compass_heading)
				{
					// show the heading on map
					//System.out.println("-> heading=" + this.gps_heading);
					int size = 16;
					double s1 = Math.sin(Math.toRadians(this.gps_heading));
					double c1 = Math.cos(Math.toRadians(this.gps_heading));
					double radius_x2 = (this.getWidth() / 2) - 34;
					double radius_y2 = (this.getHeight() / 2) - 34;
					double[] my_shape = {0.7, +0, 0, -1, -0.7, 0};

					Path p = new Path();
					for (int i = 0; i < 3; i++)
					{
						if (i == 0)
						{
							//System.out.println("" + my_shape[(i + 1) * 2 - 2]);
							//System.out.println("" + my_shape[(i + 1) * 2 - 1]);
							p.moveTo((float) (my_shape[(i + 1) * 2 - 2] * size * c1 + this.getWidth() / 2
									- my_shape[(i + 1) * 2 - 1] * size * s1 + s1 * radius_x2),
									(float) (my_shape[(i + 1) * 2 - 1] * size * c1 + this.getHeight() / 2
											+ my_shape[(i + 1) * 2 - 2] * size * s1 - c1 * radius_y2));
						}
						else
						{
							//System.out.println(""
							//		+ (my_shape[(i + 1) * 2 - 2] * size * c1 + this.getWidth() / 2
							//				- my_shape[(i + 1) * 2 - 1] * size * s1 + s1 * radius));
							//System.out.println(""
							//		+ ((my_shape[(i + 1) * 2 - 1] * size * c1 + this.getHeight() / 2
							//				+ my_shape[(i + 1) * 2 - 2] * size * s1 - c1 * radius)));
							p.lineTo((float) (my_shape[(i + 1) * 2 - 2] * size * c1 + this.getWidth() / 2
									- my_shape[(i + 1) * 2 - 1] * size * s1 + s1 * radius_x2),
									(float) (my_shape[(i + 1) * 2 - 1] * size * c1 + this.getHeight() / 2
											+ my_shape[(i + 1) * 2 - 2] * size * s1 - c1 * radius_y2));
						}
					}
					p.close();
					Paint paint_gps = new Paint(0);
					paint_gps.setAntiAlias(true);
					paint_gps.setStyle(Paint.Style.STROKE);
					paint_gps.setStrokeWidth(4);
					paint_gps.setColor(Color.parseColor("#00EE00"));
					c.drawPath(p, paint_gps);
					paint_gps.setStrokeWidth(2);
					paint_gps.setColor(Color.parseColor("#157DEC"));
					c.drawPath(p, paint_gps);

				}
			}

			// no gps fix
			Paint paint_gps = new Paint(0);
			paint_gps.setAntiAlias(true);
			paint_gps.setColor(Color.parseColor("#EE0000"));
			paint_gps.setStyle(Paint.Style.STROKE);
			paint_gps.setTextSize(11);
			c.drawText("no gps fix (" + this.used_sats + ")", this.getWidth() - 90,
					this.getHeight() - 17, paint_gps);

		}

		// draw follow
		c.drawBitmap(this.main_aagtl.follow_current, this.main_aagtl.follow_button_rect.left,
				this.main_aagtl.follow_button_rect.top, null);

		// draw arrow button
		c.drawBitmap(this.main_aagtl.arrow_button, this.main_aagtl.arrow_button_rect.left,
				this.main_aagtl.arrow_button_rect.top, null);

	}

}
