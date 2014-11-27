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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

public class Rose extends SurfaceView implements OnTouchListener, SurfaceHolder.Callback
{

	ImageManager im;
	int newx = 0, newy = 0;
	int deltax = 0, deltay = 0;
	int prev_x = 0, prev_y = 0;
	int direction = 0;
	boolean start_move = false;
	static final int more_for_speed_x = 0;
	static final int more_for_speed_y = 0;
	static final int TOUCH_CACHE_RADIUS = 35;

	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	static final int PRESS = 3;
	int touch_mode = NONE;
	float oldDist = 0;

	PointF touch_now = new PointF(0, 0);
	PointF touch_start = new PointF(0, 0);
	PointF touch_prev = new PointF(0, 0);
	PointF touch_last_load_tiles = new PointF(0, 0);
	double map_center_x_before_move = 0;
	double map_center_y_before_move = 0;
	double map_heading_prev = 0;
	double map_heading_start = 0;
	double map_heading_last_load_tiles = 0;

	//
	//
	//
	static boolean use_parent_tiles = true;
	//
	//
	//

	public Boolean redraw_notify = false;

	static final int larger_offset_x = 100;
	static final int larger_offset_y = 100;

	int tile_size_x = 256;
	int tile_size_y = 256;
	int num_tiles_x = 0;
	int num_tiles_y = 0;
	double map_center_x = 0.0;
	double map_center_y = 0.0;
	int zoom = 0;
	Coordinate center_coord = null;
	GeocacheCoordinate current_target = null;

	public int mCanvasHeight = 1;
	public int mCanvasWidth = 1;

	Bitmap bitmap_main = null;
	Canvas image_main = null;
	Paint mainpaint = null;
	Paint rectpaint = null;

	Bitmap bitmap1 = null;
	Bitmap bitmap2 = null;
	Bitmap bitmap3 = null;
	Bitmap bitmap4 = null;
	Bitmap bitmap5 = null;
	Bitmap bitmap6 = null;

	Bitmap[][] map_tiles_onscreen = null;
	String[][] map_tile_filename = null;
	int[][][] map_tile_num_onscreen = null;
	Bitmap[][] copy_bitmaps = null;
	int[][][] copy_tiles = null;
	Bitmap[] map_tiles_cache = null;
	Bitmap[] map_tiles_parent_cache = null;
	int[][] map_tiles_caches_values = null;
	int[][] map_tiles_parent_caches_values = null;
	int map_tiles_cache_oldest_counter = 0;
	int map_tiles_parent_cache_oldest_counter = 0;
	static final int map_tiles_cache_size = 15 * 3;
	static final int map_tiles_parent_cache_size = 15 * 4;

	List<Integer> yit = new ArrayList<Integer>();
	List<Integer> xit = new ArrayList<Integer>();

	aagtl main_object;

	/** Handle to the surface manager object we interact with */
	private SurfaceHolder mSurfaceHolder;

	public static final int STATE_LOSE = 1;
	public static final int STATE_PAUSE = 2;
	public static final int STATE_READY = 3;
	public static final int STATE_RUNNING = 4;
	public static final int STATE_WIN = 5;

	public static final int TILE_NULL = -1;
	public static final int TILE_LOADING = 0;
	public static final int TILE_LOADED_NEW = 1;
	public static final int TILE_LOADED_DRAWN = 2;

	public double[] deg2num(Coordinate coord)
	{
		double[] f_ret = new double[2];
		f_ret[0] = (coord.lon + 180) / 360 * (1 << zoom);
		f_ret[1] = (1 - Math.log(Math.tan(Math.toRadians(coord.lat)) + 1 / Math.cos(Math.toRadians(coord.lat))) / Math.PI) / 2 * (1 << zoom);
		// return --> tile_x,tile_y
		return f_ret;
	}

	public double[] deg2num_give_zoom(Coordinate coord, int zoom2)
	{
		double[] f_ret = new double[2];
		f_ret[0] = (coord.lon + 180) / 360 * (1 << zoom2);
		f_ret[1] = (1 - Math.log(Math.tan(Math.toRadians(coord.lat)) + 1 / Math.cos(Math.toRadians(coord.lat))) / Math.PI) / 2 * (1 << zoom2);
		// return --> tile_x,tile_y
		return f_ret;
	}

	public Coordinate num2deg(double xtile, double ytile)
	{
		double n = (1 << this.zoom);
		double lon_deg = xtile / n * 360.0 - 180.0;
		double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * ytile / n)));
		double lat_deg = lat_rad * 180.0 / Math.PI;

		Coordinate ret = new Coordinate(lat_deg, lon_deg);
		return ret;
	}

	public Coordinate num2deg_give_zoom(double xtile, double ytile, int zoom2)
	{
		double n = (1 << zoom2);
		double lon_deg = xtile / n * 360.0 - 180.0;
		double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * ytile / n)));
		double lat_deg = lat_rad * 180.0 / Math.PI;

		Coordinate ret = new Coordinate(lat_deg, lon_deg);
		return ret;
	}

	public void set_center(Coordinate coord)
	{
		double[] f_arr = new double[2];
		f_arr = deg2num(coord);
		this.map_center_x = f_arr[0];
		this.map_center_y = f_arr[1];
		// System.out.println("set_center: " + map_center_x);
		// System.out.println("set_center: " + map_center_y);
		// System.out.println("set_center: " + coord.lat);
		// System.out.println("set_center: " + coord.lon);
		this.main_object.set_center(coord.lat, coord.lon);
	}

	synchronized public void __calc_tiles_on_display()
	{
		int xi = (int) map_center_x;
		int yi = (int) map_center_y;

		// long a = android.os.SystemClock.elapsedRealtime();
		// make copies
		if ((map_tiles_onscreen != null) && (map_tile_num_onscreen != null))
		{
			// System.out.println(String.valueOf(map_tiles_onscreen.length));
			// System.out.println(String.valueOf(map_tile_num_onscreen.length));
			for (int y = 0; y < map_tiles_onscreen.length; y++)
				for (int x = 0; x < map_tiles_onscreen[y].length; x++)
				{
					// System.out.println(String.valueOf(map_tiles_onscreen[y][x]));
					copy_bitmaps[y][x] = map_tiles_onscreen[y][x];
				}

			for (int y = 0; y < map_tile_num_onscreen.length; y++)
				for (int x = 0; x < map_tile_num_onscreen[y].length; x++)
					for (int z = 0; z < 3; z++)
					{
						// System.out.println(String.valueOf(map_tile_num_onscreen[y][x][z]));
						copy_tiles[y][x][z] = map_tile_num_onscreen[y][x][z];
					}
		}

		// System.out.println("1:" +
		// String.valueOf(android.os.SystemClock.elapsedRealtime() - a));
		// a = android.os.SystemClock.elapsedRealtime();

		int i;
		int j;
		int s_x = xit.size();
		int s_y = yit.size();
		for (int m = 0; m < s_x; m++)
		// for (int m = s_x - 1; m > -1; m--)
		{
			if (m % 2 == 0)
			{
				// even
				int n = m / 2;
				i = ((Integer) xit.get(n)).intValue();
			}
			else
			{
				// odd
				int n = m / 2;
				n = (s_x - 1) - n;
				i = ((Integer) xit.get(n)).intValue();
			}

			for (int k = 0; k < s_y; k++)
			// for (int k = s_y - 1; k > -1; k--)
			{
				if (k % 2 == 0)
				{
					// even
					int n2 = k / 2;
					j = ((Integer) xit.get(n2)).intValue();
				}
				else
				{
					// odd
					int n2 = k / 2;
					n2 = (s_y - 1) - n2;
					j = ((Integer) yit.get(n2)).intValue();
				}

				int ww_2 = ((int) (num_tiles_x / 2)) + 1;
				int hh_2 = ((int) (num_tiles_y / 2)) + 1;
				int xx = xi + i - (ww_2);
				int yy = yi + j - (hh_2);

				// System.out.println(String.valueOf(i));
				// System.out.println(String.valueOf(j));
				// System.out.println(String.valueOf(ww_2));
				// System.out.println(String.valueOf(hh_2));
				// System.out.println(String.valueOf(xx));
				// System.out.println(String.valueOf(yy));
				// System.out.println(String.valueOf(xi));
				// System.out.println(String.valueOf(yi));

				// long b = android.os.SystemClock.elapsedRealtime();

				check_tile(main_object.global_settings.map_type, zoom, xx, yy, i, j);

				// System.out.println("b:" +
				// String.valueOf(android.os.SystemClock.elapsedRealtime() -
				// b));

			}
		}

		// System.out.println("2:" +
		// String.valueOf(android.os.SystemClock.elapsedRealtime() - a));

	}

	public void change_map_type(int new_map_type)
	{
		main_object.global_settings.map_type = new_map_type;
		// this.main_object.gcview.clear_stuff(); // not need
		this.clear_stuff();
		this.clear_stuff_2();
		this.main_object.mdl.clear_stuff();
		__calc_tiles_on_display();
		draw_me();
		// this.main_object.gcview.invalidate(); // not need
	}

	synchronized public void check_tile(int map_type, int zoom, int xx, int yy, int i, int j)
	{
		if (map_tile_num_onscreen == null)
		{
			return;
		}

		try
		{
			// System.out.println("" + (i - 1) + " " + (j - 1));
			map_tile_num_onscreen[i - 1][j - 1][0] = xx;
			map_tile_num_onscreen[i - 1][j - 1][1] = yy;
			map_tile_filename[i - 1][j - 1] = this.main_object.mdl.get_local_url(map_type, zoom, xx, yy);

			Boolean showing_parent = false;

			Boolean found_it = false;
			// decide where to get the tile from:
			// 1.1 Memory ~10ms
			// 1.2 Memory Cache ~10ms
			// 2. File ~40ms
			// 3. Internet background operation
			// 3.x Parent tile on disk (optional) -> use_parent_tiles = t/f

			for (int a = 0; a < copy_tiles.length; a++)
				for (int b = 0; b < copy_tiles[a].length; b++)
				{
					if ((copy_tiles[a][b][0] == xx) && (copy_tiles[a][b][1] == yy))
					{
						// ok found it in memory
						if (copy_bitmaps[a][b] != null)
						{
							if (CacheDownloader.DEBUG_) System.out.println("found it in memory");
							map_tiles_onscreen[i - 1][j - 1] = copy_bitmaps[a][b];
							// set to loaded
							map_tile_num_onscreen[i - 1][j - 1][2] = TILE_LOADED_DRAWN;
							found_it = true;
							break;
						}
					}
				}

			if (!found_it)
			{
				Bitmap dummy_bitmap = get_tile_from_map_tile_cache(xx, yy);
				if (dummy_bitmap != null)
				{
					// ok found it in big Bitmap cache
					if (CacheDownloader.DEBUG_) System.out.println("found it in bitmap cache");
					found_it = true;
					map_tiles_onscreen[i - 1][j - 1] = dummy_bitmap;
					// set to loaded
					map_tile_num_onscreen[i - 1][j - 1][2] = TILE_LOADED_DRAWN;
				}
			}

			if (!found_it)
			{
				String l_url = this.main_object.mdl.get_local_url(map_type, zoom, xx, yy);
				Boolean is_dled = this.main_object.mdl.is_downloaded(l_url);
				if (is_dled)
				{
					try
					{
						if (CacheDownloader.DEBUG_) System.out.println("found it on disk");
						map_tiles_onscreen[i - 1][j - 1] = this.main_object.mdl.get_bitmap_from_local_url(l_url);
						// add to big Bitmap cache, only if NOT in
						add_to_map_tile_cache(map_tiles_onscreen[i - 1][j - 1], xx, yy);
						// System.out.println(String.valueOf(map_tiles_onscreen[i
						// - 1][j - 1]));
						map_tile_num_onscreen[i - 1][j - 1][2] = TILE_LOADED_NEW;
					}
					catch (Exception e)
					{

					}
				}
				else
				{
					if (CacheDownloader.DEBUG_) System.out.println("11 start downloading tile in background thread");
					//
					//
					// meanwhile see if we have parent tile on disk
					if ((this.zoom > 2) && (use_parent_tiles))
					{
						if (CacheDownloader.DEBUG_) System.out.println("loading parent tile");
						int less_zoom = zoom - 1;
						Coordinate coord1 = this.num2deg(xx, yy);
						double[] this_tile2 = new double[2];
						this_tile2 = this.deg2num_give_zoom(coord1, less_zoom);
						int this_tile3_x = (int) (this_tile2[0] + 0.001); // rounding fix!
						int this_tile3_y = (int) (this_tile2[1] + 0.001); // rounding fix!

						String[] new_file_names = this.main_object.mdl.get_remote_url(map_type, less_zoom, this_tile3_x, this_tile3_y);
						String l_low = new_file_names[1];

						// first look in parent bitmap cache for parent tile
						Boolean is_in_parent_bitmap_cache = false;
						Bitmap parent_tile = get_tile_from_map_tile_parent_cache(xx, yy);
						if (parent_tile != null)
						{
							is_in_parent_bitmap_cache = true;
							if (CacheDownloader.DEBUG_) System.out.println("found parent tile in bitmap cache");
						}
						else
						{
							Boolean is_dled_parent = this.main_object.mdl.is_downloaded(l_low);
							if (CacheDownloader.DEBUG_) System.out.println("found parent tile on disk");
							if (is_dled_parent)
							{
								if (CacheDownloader.DEBUG_) System.out.println("loading parent tile from disk");
								try
								{
									parent_tile = this.main_object.mdl.get_bitmap_from_local_url(l_low);
								}
								catch (FileNotFoundException e)
								{
									if (CacheDownloader.DEBUG_) System.out.println("++parent tile, file not found++");
								}
							}
							else
							{
								if (CacheDownloader.DEBUG_) System.out.println("**parent tile not here**");
							}
						}

						if (parent_tile != null)
						{
							Bitmap quarter_tile = null;
							if (!is_in_parent_bitmap_cache)
							{
								// resize tile
								int new_size_x = this.tile_size_x * 2;
								int new_size_y = this.tile_size_y * 2;
								Bitmap parent_tile_scaled = Bitmap.createScaledBitmap(parent_tile, new_size_x, new_size_y, false);
								//
								int x_diff = 0;
								int y_diff = 0;
								if ((this_tile2[0] - this_tile3_x) > 0.1)
								{
									x_diff = this.tile_size_x;
								}

								else
								{
									x_diff = 0;
								}
								if ((this_tile2[1] - this_tile3_y) > 0.1)
								{
									y_diff = this.tile_size_y;
								}
								else
								{
									y_diff = 0;
								}

								// System.out.println("x_diff=" + x_diff);
								// System.out.println("y_diff=" + y_diff);
								// System.out.println("this_tile3_x=" +
								// this_tile3_x);
								// System.out.println("this_tile3_y=" +
								// this_tile3_y);
								// System.out.println("this_tile2[0]=" +
								// this_tile2[0]);
								// System.out.println("this_tile2[1]=" +
								// this_tile2[1]);

								quarter_tile = Bitmap.createBitmap(parent_tile_scaled, x_diff, y_diff, this.tile_size_x, this.tile_size_y);

								add_to_map_tile_parent_cache(quarter_tile, xx, yy);
							}
							else
							{
								// parent_tile from bitmap cache is already the quarter tile!!
								quarter_tile = parent_tile;
							}

							add_to_map_tile_cache(null, xx, yy);
							map_tiles_onscreen[i - 1][j - 1] = quarter_tile;
							map_tile_num_onscreen[i - 1][j - 1][2] = TILE_LOADING;

							showing_parent = true;
						}
					}
					if (CacheDownloader.DEBUG_) System.out.println("22 start downloading tile in background thread");
					//
					//
					//
					if (showing_parent)
					{
						if (CacheDownloader.DEBUG_) System.out.println("-- showing parent");
					}
					else
					{
						if (CacheDownloader.DEBUG_) System.out.println("-- NOT showing parent");
						// System.out.println("bb1" + map_type + " " + zoom + " " + xx + " "
						// + yy);
						// Reset the Bitmap to null value
						map_tiles_onscreen[i - 1][j - 1] = null;
						map_tile_num_onscreen[i - 1][j - 1][2] = TILE_LOADING;
					}
					main_object.mdl.append_tile_to_list(map_type, zoom, xx, yy);
				}
			}
		}
		catch (Exception e)
		{
			if (CacheDownloader.DEBUG_) System.out.println("check_tile: Exception1");
		}

	}

	synchronized public void add_to_map_tile_cache(Bitmap b, int tile_x, int tile_y)
	{
		int dummy2 = map_tiles_cache_oldest_counter;
		int dummy3 = dummy2;
		dummy2++;
		if (dummy2 > map_tiles_cache_size - 1)
		{
			dummy2 = 0;
		}
		map_tiles_cache_oldest_counter = dummy2;
		map_tiles_cache[dummy3] = b;
		map_tiles_caches_values[dummy3][0] = tile_x;
		map_tiles_caches_values[dummy3][1] = tile_y;

	}

	synchronized public void add_to_map_tile_parent_cache(Bitmap b, int tile_x, int tile_y)
	{
		int dummy2 = map_tiles_parent_cache_oldest_counter;
		int dummy3 = dummy2;
		dummy2++;
		if (dummy2 > map_tiles_parent_cache_size - 1)
		{
			dummy2 = 0;
		}
		map_tiles_parent_cache_oldest_counter = dummy2;
		map_tiles_parent_cache[dummy3] = b;
		map_tiles_parent_caches_values[dummy3][0] = tile_x;
		map_tiles_parent_caches_values[dummy3][1] = tile_y;

	}

	public Bitmap get_tile_from_map_tile_parent_cache(int tile_x, int tile_y)
	{
		Bitmap ret = null;
		for (int dummy = 0; dummy < map_tiles_parent_cache_size; dummy++)
		{
			if ((map_tiles_parent_caches_values[dummy][0] == tile_x) && (map_tiles_parent_caches_values[dummy][1] == tile_y))
			{
				ret = map_tiles_parent_cache[dummy];
				break;
			}
		}
		return ret;
	}

	public Bitmap get_tile_from_map_tile_cache(int tile_x, int tile_y)
	{
		Bitmap ret = null;
		for (int dummy = 0; dummy < map_tiles_cache_size; dummy++)
		{
			if ((map_tiles_caches_values[dummy][0] == tile_x) && (map_tiles_caches_values[dummy][1] == tile_y))
			{
				ret = map_tiles_cache[dummy];
				break;
			}
		}
		return ret;
	}

	public void notify_tile_loaded_new(String local_filename)
	{
		// System.out.println("lf: " + local_filename);
		Boolean must_redraw = false;
		// synchronized (map_tile_num_onscreen)
		// {
		for (int a = 0; a < map_tile_num_onscreen.length; a++)
			for (int b = 0; b < map_tile_num_onscreen[a].length; b++)
			{
				// System.out.println("lf search: " + map_tile_filename[a][b]);
				if (map_tile_filename[a][b].equals(local_filename))
				{
					if (this.main_object.mdl.is_downloaded(local_filename))
					{
						try
						{
							map_tile_num_onscreen[a][b][2] = TILE_LOADED_NEW;
							// refresh it from disk!
							map_tiles_onscreen[a][b] = this.main_object.mdl.get_bitmap_from_local_url(local_filename);
							must_redraw = true;
							// System.out.println("tile loaded!!");
						}
						catch (FileNotFoundException e)
						{

						}
					}
				}
			}

		if (must_redraw)
		{
			// only draw if we are NOT in drag mode right now!
			if (touch_mode != DRAG)
			{
				draw_me();
			}
			else
			{
				redraw_notify = true;
				// System.out.println("redraw_notify ON 1");
			}
		}
		// }

	}

	public void clear_stuff()
	{
		map_tiles_cache = new Bitmap[map_tiles_cache_size];
		map_tiles_parent_cache = new Bitmap[map_tiles_parent_cache_size];
		map_tiles_caches_values = new int[map_tiles_cache_size][2];
		map_tiles_parent_caches_values = new int[map_tiles_parent_cache_size][2];
		for (int dummy = 0; dummy < map_tiles_cache_size; dummy++)
		{
			map_tiles_cache[dummy] = null;
			map_tiles_caches_values[dummy][0] = -1;
			map_tiles_caches_values[dummy][1] = -1;
		}
		for (int dummy = 0; dummy < map_tiles_parent_cache_size; dummy++)
		{
			map_tiles_parent_cache[dummy] = null;
			map_tiles_parent_caches_values[dummy][0] = -1;
			map_tiles_parent_caches_values[dummy][1] = -1;
		}
	}

	public void clear_stuff_2()
	{
		map_tiles_onscreen = new Bitmap[num_tiles_x][num_tiles_y];
		map_tile_filename = new String[num_tiles_x][num_tiles_y];
		map_tile_num_onscreen = new int[num_tiles_x][num_tiles_y][3];

		copy_bitmaps = new Bitmap[num_tiles_x][num_tiles_y];
		copy_tiles = new int[num_tiles_x][num_tiles_y][3];

		for (int y = 0; y < map_tiles_onscreen.length; y++)
			for (int x = 0; x < map_tiles_onscreen[y].length; x++)
			{
				// System.out.println("xxxxxxx:" + y + " " + x + " " +
				// String.valueOf(map_tiles_onscreen[y][x]));
				map_tiles_onscreen[y][x] = null;
			}

		for (int y = 0; y < map_tile_num_onscreen.length; y++)
			for (int x = 0; x < map_tile_num_onscreen[y].length; x++)
			{
				for (int z = 0; z < 2; z++)
				{
					map_tile_num_onscreen[y][x][z] = -1;
					// System.out.println("yyyyyyy:" + y + " " + x + " " + z +
					// " " + String.valueOf(map_tile_num_onscreen[y][x][z]));
				}
				map_tile_num_onscreen[y][x][2] = TILE_NULL;
				map_tile_filename[y][x] = "";
			}
	}

	public void init_me()
	{
		mainpaint = new Paint(0);
		mainpaint.setAntiAlias(false);
		mainpaint.setDither(false);

		rectpaint = new Paint(0);
		rectpaint.setAntiAlias(false);
		rectpaint.setDither(false);

		bitmap_main = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888);
		image_main = new Canvas(bitmap_main);

		this.clear_stuff();

		int width = this.getWidth();
		int height = this.getHeight();
		// how many tiles on screen?
		num_tiles_x = (width / tile_size_x) + 2;
		num_tiles_y = (height / tile_size_y) + 2;

		// if modulo 2, then add +1 (this always makes it an odd number!)
		num_tiles_x = num_tiles_x + Math.abs((num_tiles_x % 2) - 1);
		num_tiles_y = num_tiles_y + Math.abs((num_tiles_y % 2) - 1);

		// System.out.println("num t x 1:" + String.valueOf(num_tiles_x));
		// System.out.println("num t y 1:" + String.valueOf(num_tiles_y));

		zoom = main_object.global_settings.map_zoom;
		// System.out.println("rose:zoom3=" + zoom);

		center_coord = new Coordinate(main_object.global_settings.map_position_lat, main_object.global_settings.map_position_lon);
		// System.out.println(String.valueOf(main_object.global_settings.map_position_lat));
		set_center(center_coord);
		// System.out.println("Center coord x:" + String.valueOf(map_center_x));
		// System.out.println("Center coord y:" + String.valueOf(map_center_y));
		__calc_tiles_on_display();
	}

	public void zoom_in()
	{
		if (this.zoom < 19)
		{
			this.set_zoom(this.zoom + 1);
		}
	}

	public void zoom_out()
	{
		if (this.zoom > 1)
		{
			this.set_zoom(this.zoom - 1);
		}
	}

	public void set_zoom(int new_zoom)
	{
		int temp = new_zoom;
		this.main_object.gcview.clear_stuff();
		this.clear_stuff();
		this.clear_stuff_2();
		this.main_object.mdl.clear_stuff();
		this.zoom = temp;
		main_object.global_settings.map_zoom = this.zoom;
		this.set_center(new Coordinate(this.main_object.global_settings.map_position_lat, this.main_object.global_settings.map_position_lon));
		__calc_tiles_on_display();
		draw_me();
		this.load_caches_from_db();
		this.main_object.gcview.invalidate();

	}

	public int[] __num2point(int xtile, int ytile)
	{
		int[] ret = new int[2];
		// tiles to pixels on screen
		// System.out.println("xt:" + xtile + " yt:" + ytile);
		// System.out.println("mcx:" + map_center_x + " mcy:" + map_center_y);
		// double d1 = xtile * tile_size_x;
		// double d2 = map_center_x * tile_size_x;
		// double d3 = mCanvasWidth / 2;
		// System.out.println("DDD: " + d1 + " " + d2 + " " + d3);
		ret[0] = (int) ((xtile * tile_size_x) - (map_center_x * tile_size_x) + (mCanvasWidth / 2));
		ret[1] = (int) ((ytile * tile_size_y) - (map_center_y * tile_size_y) + (mCanvasHeight / 2));
		return ret;
	}

	synchronized public void draw_me()
	{
		// System.out.println(String.valueOf(c.getHeight()));
		// System.out.println(String.valueOf(c.getWidth()));
		// c.setDensity(480);
		// c.setDensity(160);

		// long a = android.os.SystemClock.elapsedRealtime();

		int[] dummy = new int[2];
		for (int xtemp = 0; xtemp < num_tiles_x; xtemp++)
		{
			for (int ytemp = 0; ytemp < num_tiles_y; ytemp++)
			{
				if (map_tile_num_onscreen == null)
				{
					return;
				}
				if (map_tiles_onscreen == null)
				{
					return;
				}

				// System.out.println("x,y: " + xtemp + " " + ytemp);
				dummy = __num2point(map_tile_num_onscreen[xtemp][ytemp][0], map_tile_num_onscreen[xtemp][ytemp][1]);
				// System.out.println("dummy: " + dummy[0] + " " + dummy[1]);
				// if (false)
				if (map_tiles_onscreen[xtemp][ytemp] != null)
				{
					image_main.drawBitmap(map_tiles_onscreen[xtemp][ytemp], dummy[0] + Rose.larger_offset_x, dummy[1] + Rose.larger_offset_y, mainpaint);
				}
				else
				{
					rectpaint.setColor(Color.BLACK);
					rectpaint.setStyle(Paint.Style.FILL);
					RectF rectf = new RectF(dummy[0] + Rose.larger_offset_x, dummy[1] + Rose.larger_offset_y, dummy[0] + tile_size_x + Rose.larger_offset_x, dummy[1] + Rose.larger_offset_y + tile_size_y);
					image_main.drawRect(rectf, rectpaint);

					rectpaint.setColor(Color.RED);
					rectpaint.setStyle(Paint.Style.STROKE);
					rectf = new RectF(dummy[0] + Rose.larger_offset_x, dummy[1] + Rose.larger_offset_y, dummy[0] + tile_size_x + Rose.larger_offset_x, dummy[1] + tile_size_y + Rose.larger_offset_y);
					image_main.drawRect(rectf, rectpaint);
				}
			}
		}

		// System.out.println("dm1:" +
		// String.valueOf(android.os.SystemClock.elapsedRealtime() - a));
		// a = android.os.SystemClock.elapsedRealtime();

		Canvas c = null;
		try
		{
			c = mSurfaceHolder.lockCanvas(null);
			synchronized (mSurfaceHolder)
			{
				// System.out.println(String.valueOf(c));
				doDraw(c);
			}
		}
		finally
		{
			if (c != null)
			{
				mSurfaceHolder.unlockCanvasAndPost(c);
			}
		}

		// System.out.println("dm2:" +
		// String.valueOf(android.os.SystemClock.elapsedRealtime() - a));

		// ok , reset global redraw notifier
		// System.out.println("redraw_notify *OFF* 1");
		redraw_notify = false;
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height)
	{
		// synchronized to make sure these all change atomically
		synchronized (mSurfaceHolder)
		{
			// System.out.println("x2:" + String.valueOf(width));
			// System.out.println("y2:" + String.valueOf(height));

			// mCanvasWidth = width;
			// mCanvasHeight = height;
		}
	}

	public int[] __coord2point(Coordinate c)
	{
		int[] ret = new int[2];
		double[] point = new double[2];

		point = this.deg2num(c);
		ret[0] = (int) (point[0] * this.tile_size_x - this.map_center_x * this.tile_size_x + this.mCanvasWidth / 2);
		ret[1] = (int) (point[1] * this.tile_size_y - this.map_center_y * this.tile_size_y + this.mCanvasHeight / 2);

		return ret;
	}

	public GeocacheCoordinate check_for_cache(int touch_x, int touch_y)
	{
		GeocacheCoordinate ret = null;
		GeocacheCoordinate tmp = null;

		for (int i = 0; i < this.main_object.gcview.caches_loaded.size(); i++)
		{
			int[] p = new int[2];
			try
			{
				tmp = this.main_object.gcview.caches_loaded.get(i);
				// p = this.__coord2point(new Coordinate(tmp.lat, tmp.lon));
				p = this.__coord2point((Coordinate) tmp);
				if ((Math.abs(p[0] - touch_x) < TOUCH_CACHE_RADIUS) && (Math.abs(p[1] - touch_y) < TOUCH_CACHE_RADIUS))
				{
					ret = tmp;
					return ret;
				}
			}
			catch (IndexOutOfBoundsException e)
			{

			}
		}

		return ret;
	}

	/** Show an event in the LogCat view, for debugging */
	private void dumpEvent(MotionEvent event)
	{
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP)
		{
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++)
		{
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount()) sb.append(";");
		}
		sb.append("]");
		Log.d("dump event", sb.toString());
	}

	private float spacing(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	private float spacing(PointF a, PointF b)
	{
		float x = a.x - b.x;
		float y = a.y - b.y;
		return FloatMath.sqrt(x * x + y * y);
	}

	public PointF turn_point(PointF point, double angle)
	{
		float x = point.x - (this.getWidth() / 2);
		float y = point.y - (this.getHeight() / 2);
		float x2 = 0;
		float y2 = 0;
		x2 = (this.getWidth() / 2) + FloatMath.cos((float) Math.toRadians(angle)) * x - FloatMath.sin((float) Math.toRadians(angle)) * y;
		y2 = (this.getHeight() / 2) + FloatMath.sin((float) Math.toRadians(angle)) * x + FloatMath.cos((float) Math.toRadians(angle)) * y;
		PointF point2 = new PointF(x2, y2);
		return point2;
	}

	private void midPoint(PointF point, MotionEvent event)
	{
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	public boolean onTouch(View view, MotionEvent event)
	{
		return onTouch_aagtl(view, event);
	}

	public boolean onTouch_aagtl(View view, MotionEvent event)
	{
		String TAG = "onTouch event";

		// dumpEvent(event);

		PointF touch_now2 = null;
		PointF touch_start2 = null;
		PointF touch_prev2 = null;
		PointF touch_last_load_tiles2 = null;

		// touch_now2 = turn_point(this.touch_now,
		// this.main_object.cross.get_gps_heading());
		// touch_start2 = turn_point(this.touch_start,
		// this.main_object.cross.get_gps_heading());
		// touch_prev2 = turn_point(this.touch_prev,
		// this.main_object.cross.get_gps_heading());
		// touch_last_load_tiles2 = turn_point(this.touch_last_load_tiles,
		// this.main_object.cross
		// .get_gps_heading());

		switch (event.getAction() & MotionEvent.ACTION_MASK)
		{
		case MotionEvent.ACTION_DOWN:
			this.touch_now.set(event.getX(), event.getY());
			this.touch_start.set(event.getX(), event.getY());
			map_heading_start = this.main_object.cross.get_gps_heading();
			this.touch_prev.set(event.getX(), event.getY());
			map_heading_prev = this.main_object.cross.get_gps_heading();
			this.touch_last_load_tiles.set(event.getX(), event.getY());
			map_heading_last_load_tiles = this.main_object.cross.get_gps_heading();
			map_center_x_before_move = this.map_center_x;
			map_center_y_before_move = this.map_center_y;
			touch_mode = DRAG;

			// Log.d(TAG, "touch_mode=(START!!)");
			// Log.d(TAG, "start=" + this.touch_start.x + "," +
			// this.touch_start.y);
			// Log.d(TAG, "prev=" + this.touch_prev.x + "," +
			// this.touch_prev.y);
			// Log.d(TAG, "now=" + this.touch_now.x + "," + this.touch_now.y);

			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			this.touch_now.set(event.getX(), event.getY());
			if (this.main_object.global_settings.options_turn_map_on_heading)
			{
				touch_now2 = turn_point(this.touch_now, this.main_object.cross.get_gps_heading());
				touch_start2 = turn_point(this.touch_start, map_heading_start);
			}
			else
			{
				touch_now2 = touch_now;
				touch_start2 = touch_start;
			}
			// Log.d(TAG, "hd1=" + this.main_object.cross.get_gps_heading());
			// Log.d(TAG, "hd2=" + map_heading_start);

			// Log.d(TAG, "spacing from start=" + spacing(touch_start2,
			// touch_now2));

			if ((touch_mode == DRAG) && (spacing(touch_start2, touch_now2) < 8f))
			{
				// just a single press down
				touch_mode = PRESS;

				// check if OSD-button was touched
				if (this.main_object.follow_button_rect.contains(touch_start2.x, touch_start2.y))
				{
					// toggle follow mode
					if (this.main_object.follow_mode)
					{
						this.main_object.turn_off_follow_mode();
						this.main_object.cross.invalidate();
					}
					else
					{
						this.main_object.turn_on_follow_mode();
						this.main_object.cross.invalidate();
					}
				}
				else if (this.main_object.arrow_button_rect.contains(touch_start2.x, touch_start2.y))
				{
					// arrow OSD button pressed
					this.main_object.set_display_screen(aagtl.DISPLAY_VIEW_ARROW);
					return true;
				}
				// check if a cache was touched
				else if (this.main_object.gcview.caches_loaded != null)
				{
					// only allow to touch caches if a certain zoom level is reached
					if (this.zoom >= aagtl.TOUCH_CACHES_AFTER_THIS_ZOOM_LEVEL)
					{
						System.out.println("XXXXXXXX zoom=" + this.zoom);
						if (this.main_object.gcview.caches_loaded.size() <= this.main_object.gcview.MAX_DRAW_POINTS)
						{
							GeocacheCoordinate cache_touched = this.check_for_cache((int) touch_start2.x, (int) touch_start2.y);
							if (cache_touched != null)
							{
								this.main_object.cacheview.set_cache(cache_touched);
								this.main_object.set_display_screen(aagtl.DISPLAY_VIEW_GC);
								return true;
							}
						}
					}
				}

				if (redraw_notify)
				{
					// System.out.println("redraw_notify 1");
					// __calc_tiles_on_display();
					draw_me();
				}
				// check if a cache was touched

				// Log.d(TAG, "touch_mode=PRESS");
				// Log.d(TAG, "start=" + touch_start.x + "," + touch_start.y);
				// Log.d(TAG, "start2=" + touch_start2.x + "," +
				// touch_start2.y);
				// Log.d(TAG, "prev=" +touch_prev.x + "," + touch_prev.y);
				// Log.d(TAG, "now=" + touch_now.x + "," + touch_now.y);
				// Log.d(TAG, "now2=" + touch_now2.x + "," + touch_now2.y);
				touch_mode = NONE;
			}
			else
			{
				if (touch_mode == DRAG)
				{
					if (this.main_object.global_settings.options_turn_map_on_heading)
					{
						touch_now2 = turn_point(this.touch_now, this.main_object.cross.get_gps_heading());
						touch_start2 = turn_point(this.touch_start, map_heading_start);
					}
					else
					{
						touch_now2 = touch_now;
						touch_start2 = touch_start;
					}

					// end of "drag" move
					// Log.d(TAG, "spacing from start=" +
					// spacing(this.touch_start, this.touch_now));

					// ------------ OLD routine --------------
					// ------------ OLD routine --------------
					// ------------ OLD routine --------------
					// PointF now_turned = turn_point(this.touch_now,
					// this.main_object.cross
					// .get_gps_heading());
					// PointF start_turned = turn_point(this.touch_start,
					// this.main_object.cross
					// .get_gps_heading());
					double map_center_x_t = map_center_x_before_move - ((float) (touch_now2.x - touch_start2.x) / (float) tile_size_x);
					double map_center_y_t = map_center_y_before_move - ((float) (touch_now2.y - touch_start2.y) / (float) tile_size_y);
					Coordinate temp_c = this.num2deg(map_center_x_t, map_center_y_t);
					set_center(temp_c);
					__calc_tiles_on_display();
					draw_me();
					// draw caches
					// this.main_object.gcview.invalidate();
					// load caches fresh from DB
					this.load_caches_from_db();

					this.main_object.change_status_text(String.format("lat %.5f", temp_c.lat) + " " + String.format("lon %.5f", temp_c.lon));
					// this.main_object.status_text.append(" " +
					// String.format("%.3f", this.map_center_x) + " "
					// + String.format("%.3f", this.map_center_y));
					// ------------ OLD routine --------------
					// ------------ OLD routine --------------
					// ------------ OLD routine --------------

					// Log.d(TAG, "touch_mode=NONE (END of DRAG) (" +
					// (event.getX() - this.touch_prev.x)
					// + "," + (event.getY() - this.touch_prev.y) + ")");
					// Log.d(TAG, "touch_mode=NONE (END of DRAG) ("
					// + (event.getX() - this.touch_start.x) + ","
					// + (event.getY() - this.touch_start.y) + ")");

					// Log.d(TAG, "start=" + this.touch_start.x + "," +
					// this.touch_start.y);
					// Log.d(TAG, "prev=" + this.touch_prev.x + "," +
					// this.touch_prev.y);
					// Log.d(TAG, "now=" + this.touch_now.x + "," +
					// this.touch_now.y);
					touch_mode = NONE;
				}
				else
				{
					if (touch_mode == ZOOM)
					{
						// end of "pinch zoom" move
						float newDist = spacing(event);
						float scale = 0;
						if (newDist > 10f)
						{
							scale = newDist / oldDist;
						}

						if (scale > 1.3)
						{
							// zoom in
							this.zoom_in();
						}
						else if (scale < 0.8)
						{
							// zoom out
							this.zoom_out();
						}

						// Log.d(TAG, "touch_mode=NONE (END of ZOOM part 1) (" +
						// scale + ")");
						// Log.d(TAG, "start=" + this.touch_start.x + "," +
						// this.touch_start.y);
						// Log.d(TAG, "prev=" + this.touch_prev.x + "," +
						// this.touch_prev.y);
						// Log.d(TAG, "now=" + this.touch_now.x + "," +
						// this.touch_now.y);
						touch_mode = NONE;
					}
					else
					{
						// Log.d(TAG, "touch_mode=NONE (END of ZOOM part 2)");
						touch_mode = NONE;
					}
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (touch_mode == DRAG)
			{
				this.touch_now.set(event.getX(), event.getY());
				if (this.main_object.global_settings.options_turn_map_on_heading)
				{
					touch_now2 = turn_point(this.touch_now, this.main_object.cross.get_gps_heading());
					touch_start2 = turn_point(this.touch_start, map_heading_start);
					touch_prev2 = turn_point(this.touch_prev, map_heading_prev);
					touch_last_load_tiles2 = turn_point(this.touch_last_load_tiles, map_heading_last_load_tiles);
				}
				else
				{
					touch_now2 = touch_now;
					touch_start2 = touch_start;
					touch_prev2 = touch_prev;
					touch_last_load_tiles2 = touch_last_load_tiles;
				}

				// Log.d(TAG, "spacing from start=" + spacing(touch_start2,
				// touch_now2));

				if ((Math.abs(touch_now2.x - touch_last_load_tiles2.x) < tile_size_x * 0.4) && (Math.abs(touch_now2.y - touch_last_load_tiles2.y) < tile_size_y * 0.4))
				{
					// only small move -> smear image
					// System.out.println("DRAG -> near");

					// ------------ OLD routine ------------
					// ------------ OLD routine ------------
					// ------------ OLD routine ------------
					// System.out.println("Ms d:" + deltax + " " + deltay +
					// " p:" + prev_x + " " + prev_y + "mc:"
					// + map_center_x + " " + map_center_y + " C" + X + " " +
					// Y);

					double tempxxx = ((float) (touch_now2.x - touch_prev2.x) / (float) tile_size_x);
					double tempyyy = ((float) (touch_now2.y - touch_prev2.y) / (float) tile_size_y);
					// System.out.println("tx:" + tempxxx + " ty:" + tempyyy);
					double map_center_x_t8 = map_center_x - tempxxx;
					double map_center_y_t8 = map_center_y - tempyyy;
					// System.out.println("tx:" + tile_size_x + " ty:" +
					// tile_size_y);
					// System.out.println("mxn:" + map_center_x_t8 + " myn:" +
					// map_center_y_t8);
					Coordinate temp_c8 = this.num2deg(map_center_x_t8, map_center_y_t8);
					set_center(temp_c8);

					Canvas c = null;
					try
					{
						c = mSurfaceHolder.lockCanvas(null);
						synchronized (mSurfaceHolder)
						{
							if ((c != null) && (bitmap_main != null))
							{
								if (this.main_object.global_settings.options_turn_map_on_heading)
								{
									c.rotate((int) -this.main_object.cross.get_gps_heading(), this.getWidth() / 2, this.getHeight() / 2);
								}
								c.drawBitmap(bitmap_main, touch_now2.x - touch_last_load_tiles2.x - larger_offset_x, touch_now2.y - touch_last_load_tiles2.y - larger_offset_y, null);
								// draw caches
								// System.out.println("iigc2");
								this.main_object.gcview.invalidate();
							}
						}
					}
					finally
					{
						if (c != null)
						{
							mSurfaceHolder.unlockCanvasAndPost(c);
						}
					}
					this.main_object.change_status_text(String.format("lat %.5f", temp_c8.lat) + " " + String.format("lon %.5f", temp_c8.lon));
					// this.main_object.status_text.append(" " +
					// String.format("%.3f", this.map_center_x) + " "
					// + String.format("%.3f", this.map_center_y));
					// ------------ OLD routine ------------
					// ------------ OLD routine ------------
					// ------------ OLD routine ------------
				}
				else
				{
					// bigger move -> load tiles new, draw new
					// System.out.println("DRAG -> far");

					// ------------ OLD routine ------------
					// ------------ OLD routine ------------
					// ------------ OLD routine ------------

					// System.out.println("Mb d:" + deltax + " " + deltay +
					// " p:" + prev_x + " " + prev_y + "mc:"
					// + map_center_x + " " + map_center_y + " C" + X + " " +
					// Y);
					if (this.main_object.global_settings.options_turn_map_on_heading)
					{
						touch_now2 = turn_point(this.touch_now, this.main_object.cross.get_gps_heading());
						touch_start2 = turn_point(this.touch_start, map_heading_start);
						touch_prev2 = turn_point(this.touch_prev, map_heading_prev);
						touch_last_load_tiles2 = turn_point(this.touch_last_load_tiles, map_heading_last_load_tiles);
					}
					else
					{
						touch_now2 = touch_now;
						touch_start2 = touch_start;
						touch_prev2 = touch_prev;
						touch_last_load_tiles2 = touch_last_load_tiles;
					}

					double tempxxx = ((float) (touch_now2.x - touch_prev2.x) / (float) tile_size_x);
					double tempyyy = ((float) (touch_now2.y - touch_prev2.y) / (float) tile_size_y);
					// System.out.println("tx:" + tempxxx + " ty:" + tempyyy);
					double map_center_x_t8 = map_center_x - tempxxx;
					double map_center_y_t8 = map_center_y - tempyyy;
					// System.out.println("tx:" + tile_size_x + " ty:" +
					// tile_size_y);
					// System.out.println("mxn:" + map_center_x_t8 + " myn:" +
					// map_center_y_t8);
					Coordinate temp_c8 = this.num2deg(map_center_x_t8, map_center_y_t8);
					set_center(temp_c8);

					// double map_center_x_t22 = map_center_x
					// - ((float) (this.touch_now.x - this.touch_prev.x) /
					// (float) tile_size_x);
					// double map_center_y_t22 = map_center_y
					// - ((float) (this.touch_now.x - this.touch_prev.y) /
					// (float) tile_size_y);
					// Coordinate temp_c22 = this.num2deg(map_center_x_t22,
					// map_center_y_t22);
					// set_center(temp_c22);

					__calc_tiles_on_display();
					draw_me();
					// draw caches
					// this.main_object.gcview.invalidate();
					// load caches fresh from DB
					this.load_caches_from_db();

					this.main_object.change_status_text(String.format("lat %.5f", temp_c8.lat) + " " + String.format("lon %.5f", temp_c8.lon));
					// this.main_object.status_text.append(" " +
					// String.format("%.3f", this.map_center_x) + " "
					// + String.format("%.3f", this.map_center_y));

					// ------------ OLD routine ------------
					// ------------ OLD routine ------------
					// ------------ OLD routine ------------

					// set "last load tiles" point
					this.touch_last_load_tiles.set(this.touch_now.x, this.touch_now.y);
					map_heading_last_load_tiles = this.main_object.cross.get_gps_heading();
				}

				this.touch_prev.set(event.getX(), event.getY());
				map_heading_prev = this.main_object.cross.get_gps_heading();
			}
			else if (touch_mode == ZOOM)
			{
				this.touch_now.set(event.getX(), event.getY());
				// float newDist = spacing(event);
				// Log.d(TAG, "newDist=" + newDist);
				// if (newDist > 10f)
				// {
				// matrix.set(savedMatrix);
				// float scale = newDist / oldDist;

				// Log.d(TAG, "touch_mode=ZOOM (" + scale + ")");
				// Log.d(TAG, "start=" + this.touch_start.x + "," +
				// this.touch_start.y);
				// Log.d(TAG, "prev=" + this.touch_prev.x + "," +
				// this.touch_prev.y);
				// Log.d(TAG, "now=" + this.touch_now.x + "," +
				// this.touch_now.y);
				// matrix.postScale(scale, scale, mid.x, mid.y);
				// }

				this.touch_prev.set(event.getX(), event.getY());
				map_heading_prev = this.main_object.cross.get_gps_heading();
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			oldDist = spacing(event);
			// Log.d(TAG, "oldDist=" + oldDist);
			if (oldDist > 10f)
			{
				// savedMatrix.set(matrix);
				// midPoint(mid, event);
				touch_mode = ZOOM;

				// Log.d(TAG, "touch_mode=ZOOM");
				// Log.d(TAG, "start=" + this.touch_start.x + "," +
				// this.touch_start.y);
				// Log.d(TAG, "prev=" + this.touch_prev.x + "," +
				// this.touch_prev.y);
				// Log.d(TAG, "now=" + this.touch_now.x + "," +
				// this.touch_now.y);
			}
			break;
		}
		return true;
	}

	public void load_caches_from_db()
	{
		List<GeocacheCoordinate> caches_read = new ArrayList<GeocacheCoordinate>();
		try
		{
			caches_read = this.main_object.pv.get_points_filter(this.main_object.get_visible_area_large(), this.main_object.global_settings.options_hide_found, 50);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		this.main_object.gcview.set_loaded_caches(caches_read);
	}

	public void draw_view()
	{
		Canvas c = null;
		try
		{
			c = mSurfaceHolder.lockCanvas(null);
			synchronized (mSurfaceHolder)
			{
				if ((c != null) && (bitmap_main != null))
				{
					this.doDraw(c);
				}
			}
		}
		finally
		{
			if (c != null)
			{
				mSurfaceHolder.unlockCanvasAndPost(c);
			}
		}
	}

	private void doDraw(Canvas canvas)
	{
		// System.out.println("Rose: doDraw");
		if ((canvas != null) && (bitmap_main != null))
		{
			// System.out.println("mcx:" + map_center_x);
			// System.out.println("mcy:" + map_center_y);
			if (this.main_object.global_settings.options_turn_map_on_heading)
			{
				canvas.rotate((int) -this.main_object.cross.get_gps_heading(), this.getWidth() / 2, this.getHeight() / 2);
			}
			canvas.drawBitmap(bitmap_main, -Rose.larger_offset_x, -Rose.larger_offset_y, null);
		}
	}

	public Rose(Context context, aagtl m)
	{
		super(context);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		mSurfaceHolder = holder;
		main_object = m;
		holder.addCallback(this);

		init_me();

		// System.out.println("x " + String.valueOf(this.getWidth()));
		// System.out.println("y " + String.valueOf(this.getHeight()));

		setFocusable(true); // make sure we get key events
		this.setOnTouchListener(this); // get touchscreen events
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus)
	{
		// System.out.println("rose:zoom2=" + zoom);
	}

	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		// System.out.println("surfaceChanged:" + String.valueOf(width));
		// System.out.println("surfaceChanged:" + String.valueOf(height));

		this.mCanvasWidth = width;
		this.mCanvasHeight = height;

		// -----------------------------------------
		// -----------------------------------------
		// OSD button positions
		this.main_object.follow_button_rect = new RectF(10, this.mCanvasHeight - this.main_object.follow_current.getHeight() - 10, 10 + this.main_object.follow_current.getWidth(), this.mCanvasHeight - 10);

		this.main_object.arrow_button_rect = new RectF(this.mCanvasWidth - 10 - this.main_object.arrow_button.getWidth(), this.mCanvasHeight - this.main_object.arrow_button.getHeight() - 10, this.mCanvasWidth - 10, this.mCanvasHeight - 10);
		// OSD button positions
		// -----------------------------------------
		// -----------------------------------------

		if (bitmap_main != null)
		{
			// try to free the memory of bitmap
			bitmap_main.recycle();
		}
		bitmap_main = Bitmap.createBitmap((this.getWidth() + Rose.larger_offset_x * 2), (this.getHeight() + Rose.larger_offset_x * 2), Bitmap.Config.ARGB_8888);
		image_main = new Canvas(bitmap_main);

		// how many tiles on screen?
		num_tiles_x = (width / tile_size_x) + 2 + more_for_speed_x;
		num_tiles_y = (height / tile_size_y) + 2 + more_for_speed_y;

		// if modulo 2, then add +1 (this always makes it an odd number!)
		num_tiles_x = num_tiles_x + Math.abs((num_tiles_x % 2) - 1);
		num_tiles_y = num_tiles_y + Math.abs((num_tiles_y % 2) - 1);

		// System.out.println("num t x:" + String.valueOf(num_tiles_x));
		// System.out.println("num t y:" + String.valueOf(num_tiles_y));

		this.clear_stuff_2();

		for (int xtemp = 1; xtemp < num_tiles_x + 1; xtemp++)
		{
			xit.add(new Integer(xtemp));
		}

		for (int ytemp = 1; ytemp < num_tiles_y + 1; ytemp++)
		{
			yit.add(new Integer(ytemp));
		}

		// zoom = main_object.global_settings.map_zoom;
		//
		// System.out.println("rose:zoom1=" + zoom);
		//
		// center_coord = new
		// Coordinate(main_object.global_settings.map_position_lat,
		// main_object.global_settings.map_position_lon);
		// set_center(center_coord);

		__calc_tiles_on_display();
		draw_me();
		this.load_caches_from_db();
		// System.out.println("iigc4");
		this.main_object.gcview.invalidate();
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	public void surfaceCreated(SurfaceHolder holder)
	{
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		// System.out.println("Rose: surfaceCreated");
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
	}

}
