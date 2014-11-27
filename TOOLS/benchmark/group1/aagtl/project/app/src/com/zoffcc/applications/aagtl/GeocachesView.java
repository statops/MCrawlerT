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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.ImageView;

public class GeocachesView extends ImageView
{
	int gc_box_small_width = 8;
	int gc_box_small_height = 8;
	int gc_box_big_width = 15;
	int gc_box_big_height = 15;

	int TOO_MUCH_POINTS = 30;
	int MAX_DRAW_POINTS = 250;

	aagtl main_aagtl;

	Bitmap bitmap_main = null;
	Canvas image_main = null;

	Boolean double_buffer = false;

	int COLOR_FOUND = Color.parseColor("#bebebe");
	int COLOR_REGULAR = Color.parseColor("#11a011");
	int COLOR_MULTI = Color.parseColor("#b0a010");
	int COLOR_WAYPOINTS = Color.parseColor("#b0a010");
	int COLOR_DEFAULT = Color.parseColor("#1111ef");
	int COLOR_CURRENT_CACHE = Color.parseColor("#fe0000");
	int COLOR_CACHE_CENTER = Color.parseColor("#101010");

	Paint box_paint = new Paint(0);
	Paint text_paint = new Paint(0);

	List<GeocacheCoordinate> caches_loaded = new ArrayList<GeocacheCoordinate>();

	public GeocachesView(Context context, aagtl main_aagtl)
	{
		super(context);

		text_paint.setColor(Color.BLACK);
		//text_paint.setStyle(Paint.Style.FILL);
		text_paint.setTextSize(21);
		text_paint.setStrokeWidth(2);
		text_paint.setTypeface(Typeface.DEFAULT_BOLD);
		text_paint.setAntiAlias(true);

		bitmap_main = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
		image_main = new Canvas(bitmap_main);

		this.main_aagtl = main_aagtl;
		this.clear_stuff();
	}

	public void clear_stuff()
	{
		// clear caches loaded
		if (caches_loaded != null)
		{
			caches_loaded.clear();
		}
	}

	public void set_loaded_caches(List<GeocacheCoordinate> new_list)
	{
		this.caches_loaded = new_list;
		//System.out.println("iigcXX");
		this.invalidate();
	}

	public double[] deg2num_give_zoom(Coordinate coord, int zoom)
	{
		double[] f_ret = new double[2];
		f_ret[0] = (coord.lon + 180) / 360 * (1 << zoom);
		f_ret[1] = (1 - Math.log(Math.tan(Math.toRadians(coord.lat)) + 1 / Math.cos(Math.toRadians(coord.lat))) / Math.PI) / 2 * (1 << zoom);
		// return --> tile_x,tile_y
		return f_ret;
	}

	public int[] __coord2point_give_zoom(Coordinate coord, int zoom)
	{
		double[] point = new double[2];
		point = this.deg2num_give_zoom(coord, zoom);
		int[] i_ret = new int[2];
		i_ret[0] = (int) (point[0] * this.main_aagtl.rose.tile_size_x - this.main_aagtl.rose.map_center_x * this.main_aagtl.rose.tile_size_x + this.main_aagtl.rose.mCanvasWidth / 2);
		i_ret[1] = (int) (point[1] * this.main_aagtl.rose.tile_size_y - this.main_aagtl.rose.map_center_y * this.main_aagtl.rose.tile_size_y + this.main_aagtl.rose.mCanvasHeight / 2);
		return i_ret;
	}

	public void onDraw(Canvas c)
	{
		//System.out.println("GeocachesView: onDraw");

		GeocacheCoordinate this_gc = null;
		Coordinate this_c = null;

		if (this.main_aagtl.global_settings.options_turn_map_on_heading)
		{
			double_buffer = true;
		}
		else
		{
			double_buffer = false;
		}

		Canvas draw_on_me = c;
		if (double_buffer)
		{
			draw_on_me = this.image_main;
			this.bitmap_main.eraseColor(Color.TRANSPARENT);
		}

		if (this.caches_loaded == null)
		{
			return;
		}

		//		this.main_aagtl.status_text.setText(this.main_aagtl.status_text.getText() + " c:"
		//				+ this.caches_loaded.size());

		// too much caches to display
		if (this.caches_loaded.size() > this.MAX_DRAW_POINTS)
		{
			return;
		}

		int box_width = this.gc_box_big_width;
		int box_height = this.gc_box_big_height;

		if (this.caches_loaded.size() > this.TOO_MUCH_POINTS)
		{
			box_width = this.gc_box_small_width;
			box_height = this.gc_box_small_height;
		}

		Boolean target_drawn = false;

		for (int _cache = 0; _cache < this.caches_loaded.size(); _cache++)
		{
			this_gc = this.caches_loaded.get(_cache);

			//this_c = new Coordinate(this_gc.lat, this_gc.lon);
			this_c = (Coordinate) this_gc;

			//System.out.println("" + String.valueOf(this_c));

			int[] p = new int[2];
			p = this.__coord2point_give_zoom(this_c, this.main_aagtl.rose.zoom);

			//
			//
			if ((this_gc.found) || (this_gc.aagtl_status == GeocacheCoordinate.AAGTL_STATUS_FOUND))
			{
				//System.out.println("cache found!");
				box_paint.setColor(this.COLOR_FOUND);
			}
			else
			{
				if (this_gc.type == null)
				{
					box_paint.setColor(this.COLOR_DEFAULT);
				}
				else
				{
					if (this_gc.type.matches(GeocacheCoordinate.TYPE_REGULAR))
					{
						box_paint.setColor(this.COLOR_REGULAR);
					}
					else if (this_gc.type.matches(GeocacheCoordinate.TYPE_MULTI))
					{
						box_paint.setColor(this.COLOR_MULTI);
					}
					else
					{
						box_paint.setColor(this.COLOR_DEFAULT);
					}
				}
			}

			//
			//
			box_paint.setStrokeWidth(4);
			if (this.caches_loaded.size() > this.TOO_MUCH_POINTS)
			{
				box_paint.setStrokeWidth(3);
			}
			box_paint.setStyle(Paint.Style.STROKE);
			RectF gc_box = new RectF(p[0] - box_width, p[1] - box_height, p[0] + box_width, p[1] + box_height);

			draw_on_me.drawRect(gc_box, box_paint);
			//			gc_box = new RectF(p[0] - box_width + 1, p[1] - box_height + 1, p[0] + box_width - 1, p[1]
			//					+ box_height - 1);
			//			draw_on_me.drawRect(gc_box, box_paint);

			//
			//
			box_paint.setStrokeWidth(2);
			int inside = 3;
			int outside = 2;
			if (this.caches_loaded.size() > this.TOO_MUCH_POINTS)
			{
				box_paint.setStrokeWidth(1);
				inside = 1;
				outside = 2;
			}

			box_paint.setColor(Color.BLACK);
			gc_box = new RectF(p[0] - box_width - outside, p[1] - box_height - outside, p[0] + box_width + outside, p[1] + box_height + outside);
			draw_on_me.drawRect(gc_box, box_paint);
			gc_box = new RectF(p[0] - box_width + inside, p[1] - box_height + inside, p[0] + box_width - inside, p[1] + box_height - inside);
			draw_on_me.drawRect(gc_box, box_paint);

			// if this cache is disabled
			if (this_gc.status == GeocacheCoordinate.STATUS_DISABLED)
			{
				Paint temp_paint = new Paint();
				temp_paint.setColor(Color.RED);
				temp_paint.setAntiAlias(true);
				temp_paint.setStrokeWidth(4);
				draw_on_me.drawLine(p[0] + box_width, p[1] - box_height, p[0] - box_width - 1, p[1] + box_height - 1, temp_paint);
			}

			//
			//
			// draw GC-code
			if (this.main_aagtl.rose.zoom > 15)
			{
				text_paint.setTextScaleX(1.0f);
				text_paint.setTextSize(25);
				if (this.caches_loaded.size() > this.TOO_MUCH_POINTS)
				{
					text_paint.setTextSize(17);
				}
				draw_on_me.drawText(this_gc.name, p[0] - box_width, p[1] - box_height - 6, text_paint);
			}
			//
			// draw fullname (only first 20 letters!!)
			if (this.main_aagtl.rose.zoom > 17)
			{
				text_paint.setTextScaleX(0.9f);
				text_paint.setTextSize(19);
				if (this.caches_loaded.size() > this.TOO_MUCH_POINTS)
				{
					text_paint.setTextSize(15);
				}
				draw_on_me.drawText(this_gc.title.substring(0, Math.min(20, this_gc.title.length())), p[0] - box_width, p[1] + box_height + 19, text_paint);
			}

			if (this.main_aagtl.rose.current_target != null)
			{
				if (this.main_aagtl.rose.current_target.name.compareTo(this_gc.name) == 0)
				{
					// ok this is the current target
					box_paint.setColor(Color.BLUE);
					box_paint.setStrokeWidth(3);
					//this_c = new Coordinate(this_gc.lat, this_gc.lon);
					this_c = (Coordinate) this_gc;

					//System.out.println("draw target 001" + String.valueOf(this_c));

					draw_on_me.drawLine(p[0], p[1], this.getWidth() / 2, this.getHeight() / 2, box_paint);

					target_drawn = true;
				}
				else
				{
					// we have a manual target!!
				}
			}

			//System.out.println("yyyy1");

		}

		//System.out.println("zzzzz1");

		if (!target_drawn)
		{
			if (this.main_aagtl.rose.current_target != null)
			{
				// ok , manual target found!!
				// draw it!

				this_gc = this.main_aagtl.rose.current_target;
				this_c = (Coordinate) this_gc;
				int[] p = new int[2];
				p = this.__coord2point_give_zoom(this_c, this.main_aagtl.rose.zoom);

				// ok this is the current target
				box_paint.setColor(Color.BLUE);
				box_paint.setStrokeWidth(3);

				//System.out.println("draw target 002=" + this_c.lat + "," + this_c.lon);

				draw_on_me.drawLine(p[0], p[1], this.getWidth() / 2, this.getHeight() / 2, box_paint);

			}
		}

		if ((bitmap_main != null) && (this.double_buffer))
		{
			//System.out.println("GCView: doDraw");
			if (this.main_aagtl.global_settings.options_turn_map_on_heading)
			{
				c.save();
				c.rotate((int) -this.main_aagtl.cross.get_gps_heading(), this.getWidth() / 2, this.getHeight() / 2);
			}
			c.drawBitmap(bitmap_main, 0, 0, null);
			if (this.main_aagtl.global_settings.options_turn_map_on_heading)
			{
				c.restore();
			}
		}

	}

	public void onSizeChanged(int width, int height, int old_w, int old_h)
	{
		//System.out.println("GCView: onSizeChanged");
		if (bitmap_main != null)
		{
			bitmap_main.recycle();
		}
		bitmap_main = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		image_main = new Canvas(bitmap_main);
	}

}
