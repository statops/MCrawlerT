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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;

public class MapDownloader extends Thread
{
	Handler global_handler = null;
	aagtl main_object;

	public static final int MAP_OSM = 1;
	public static final int MAP_OCM = 2;
	public static final int MAP_BIM = 3;

	public static final String TILE_FILENAME_EXT = "png_";
	public static final String TILE_FILENAME_WEBEXT = "png";

	// default map type
	int current_maptype = MAP_OCM;

	Boolean running = true;

	// for the download tile list
	class single_tile
	{
		int type;
		int zoom;
		int x;
		int y;
	}

	List<single_tile> download_list = new ArrayList<single_tile>();
	public static final int max_download_list_size = 30;

	public MapDownloader(Handler handler, aagtl main)
	{
		global_handler = handler;
		main_object = main;
	}

	public void clear_stuff()
	{
		synchronized (download_list)
		{
			download_list.clear();
			// System.out.println("DL: clear ");
			main_object.append_status_text(" dl:" + download_list.size());
		}
	}

	public void append_tile_to_list(int type, int zoom, int x, int y)
	{
		single_tile a = new single_tile();
		a.type = type;
		a.zoom = zoom;
		a.x = x;
		a.y = y;
		// System.out.println(String.valueOf(a));
		synchronized (download_list)
		{
			// System.out.println("DL: add oldsize=" + download_list.size());
			if (download_list.size() > max_download_list_size)
			{
				// remove the oldest entry, before adding a new one
				// System.out.println("DL: rm 0");
				download_list.remove(0);
			}
			download_list.add(a);
			// System.out.println("DL: add newsize=" + download_list.size());
			main_object.append_status_text(" dl:" + download_list.size());
		}
	}

	public void download_tile(String remote_fn, String local_fn)
	{
		// make dirs
		File dir1 = new File(local_fn);
		File dir2 = new File(dir1.getParent());
		dir2.mkdirs();

		ImageManager im = new ImageManager();
		im.DownloadFromUrl(remote_fn, local_fn);

		// System.out.println("notify_tile_loaded");
		main_object.rose.notify_tile_loaded_new(local_fn);

	}

	public Boolean is_downloaded(String full_local_filename)
	{
		File file1 = new File(full_local_filename);
		if (file1.length() == 0)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	public Bitmap get_bitmap_from_local_url(String full_local_filename) throws FileNotFoundException
	{
		Bitmap temp = null;
		try
		{
			temp = BitmapFactory.decodeFile(full_local_filename);
		}
		catch (OutOfMemoryError e)
		{
			e.printStackTrace();
			System.gc();
			try
			{
				temp = BitmapFactory.decodeFile(full_local_filename);
			}
			catch (OutOfMemoryError e2)
			{
				e2.printStackTrace();
			}
		}
		return temp;
	}

	public String get_local_url(int map_type, int zoom, int x, int y)
	{
		String ret = null;

		if (map_type == MAP_OSM)
		{
			ret = String.format(this.main_object.main_data_dir + "/maps/osm/%d/%d/%d." + TILE_FILENAME_EXT, zoom, x, y);
		}
		else if (map_type == MAP_OCM)
		{
			ret = String.format(this.main_object.main_data_dir + "/maps/ocm/%d/%d/%d." + TILE_FILENAME_EXT, zoom, x, y);
		}
		else if (map_type == MAP_BIM)
		{
			ret = String.format(this.main_object.main_data_dir + "/maps/sat/%d/%d/%d." + TILE_FILENAME_EXT, zoom, x, y);
		}

		return ret;
	}

	public String[] get_remote_url(int map_type, int zoom, int x, int y)
	{
		String[] ret = new String[2];
		ret[0] = null;
		ret[1] = null;

		if (map_type == MAP_OSM)
		{
			ret[0] = String.format("http://tile.openstreetmap.org/%d/%d/%d." + TILE_FILENAME_WEBEXT, zoom, x, y);
			ret[1] = String.format(this.main_object.main_data_dir + "/maps/osm/%d/%d/%d." + TILE_FILENAME_EXT, zoom, x, y);
		}
		else if (map_type == MAP_OCM)
		{
			ret[0] = String.format("http://andy.sandbox.cloudmade.com/tiles/cycle/%d/%d/%d." + TILE_FILENAME_WEBEXT, zoom, x, y);
			ret[1] = String.format(this.main_object.main_data_dir + "/maps/ocm/%d/%d/%d." + TILE_FILENAME_EXT, zoom, x, y);
		}
		else if (map_type == MAP_BIM)
		{
			int srv = 0;
			String quad_key = "";
			int mask = 0;
			for (int i = 1; i < (zoom + 1); i++)
			{
				int digit = 0;
				// int j = zoom - i;
				if (mask == 0)
				{
					mask = 1;
				}
				else
				{
					mask = mask * 2;
				}

				if ((mask & x) == mask)
				{
					digit = digit + 1;
				}

				if ((mask & y) == mask)
				{
					digit = digit + 2;
				}
				quad_key = String.valueOf(digit) + quad_key;
			}

			srv = (int) (Math.random() * 6);

			ret[0] = String.format("http://ecn.t%d.tiles.virtualearth.net/tiles/h%s.jpeg?g=373&mkt=de-DE", srv, quad_key);
			ret[1] = String.format(this.main_object.main_data_dir + "/maps/sat/%d/%d/%d." + TILE_FILENAME_EXT, zoom, x, y);
		}

		return ret;
	}

	public void request_stop()
	{
		running = false;
	}

	public void run()
	{

		while (running)
		{

			// should we stop running?
			if (Thread.interrupted())
			{
				return;
			}

			// System.out.println("MDL: run");

			String[] sa;

			if (download_list.size() > 0)
			{
				single_tile this_tile = null;
				synchronized (download_list)
				{
					// this_tile = download_list.get(0);
					this_tile = download_list.get(download_list.size() - 1);
					// System.out.println("DL: get " + (download_list.size() -
					// 1));
				}
				// System.out.println("got:" + String.valueOf(this_tile));
				sa = this.get_remote_url(this_tile.type, this_tile.zoom, this_tile.x, this_tile.y);
				this.download_tile(sa[0], sa[1]);
				synchronized (download_list)
				{
					// System.out.println("DL: rm ?" + (download_list.size() -
					// 1));
					download_list.remove(this_tile);
					main_object.append_status_text(" dl:" + download_list.size());
				}
				// System.out.println("removed:" + String.valueOf(this_tile));
				try
				{
					Thread.sleep(2);
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
			else
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					break;
				}
			}
		}

	}

}
