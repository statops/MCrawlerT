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
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class PointProvider
{
	Object downloader;
	String cache_table;
	String filterstring = null;
	String[] filterargs;
	String filename;

	private SQLiteDatabase db;

	public static class access_lock_class
	{
		int dummy = 0;
	}

	public static access_lock_class access_lock = new access_lock_class();

	HashMap<String, String> agtlconf = new HashMap<String, String>();

	public PointProvider(String filename, Object downloader, String ctype, String table)
	{

		this.filename = filename;
		this.downloader = downloader;
		this.cache_table = table;

		agtlconf.put("app_version", new String(""));
		agtlconf.put("db_version", new String(""));
		agtlconf.put("environment", new String(""));

		SQLiteDatabase.CursorFactory cf = null;
		try
		{
			this.db = SQLiteDatabase.openOrCreateDatabase(this.filename, cf);
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}

		this.create_db();
	}

	public void reopen_db()
	{
		synchronized (PointProvider.access_lock)
		{
			try
			{
				// first close
				this.close();
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
			// now reopen again
			SQLiteDatabase.CursorFactory cf = null;
			try
			{
				this.db = SQLiteDatabase.openOrCreateDatabase(this.filename, cf);
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
			}
		}
	}

	public void create_db()
	{
		try
		{
			this.db.execSQL(String.format("CREATE TABLE IF NOT EXISTS agtlconf (prop long varchar,data long varchar);", this.cache_table));
		}
		catch (SQLiteException e)
		{
			// table already here!!
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}

		try
		{
			this.db.execSQL(String
					.format("CREATE TABLE IF NOT EXISTS %s (logs long varchar, loname long varchar, terrain INTEGER, waypoints long varchar, marked INTEGER, logas INTEGER, owner long varchar, images long varchar, guid long varchar, size INTEGER, title long varchar, lotitle long varchar, lon REAL, logdate long varchar, desc long varchar, type long varchar, status INTEGER, difficulty INTEGER, lat REAL, hints long varchar, name long varchar primary key, fieldnotes long varchar, notes long varchar, shortdesc long varchar, found INTEGER,aagtl_status INTEGER);",
							this.cache_table));
		}
		catch (SQLiteException e)
		{
			// table already here!!
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}

		try
		{
			this.db.execSQL(String.format("CREATE INDEX IF NOT EXISTS geocaches_latlon ON geocaches (lat ASC, lon ASC);", this.cache_table));
		}
		catch (SQLiteException e)
		{
			// index already here!!
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}

		try
		{
			this.db.execSQL(String.format("CREATE INDEX IF NOT EXISTS geocaches_name ON geocaches (name ASC);", this.cache_table));
		}
		catch (SQLiteException e)
		{
			// index already here!!
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
	}

	public void begin_trans()
	{
		//System.out.println("begin tran");
		try
		{
			db.beginTransaction();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			try
			{
				reopen_db();
				db.beginTransaction();
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
			}
		}
	}

	public void commit()
	{
		try
		{
			//System.out.println("commit");
			db.setTransactionSuccessful();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
	}

	public void end_trans()
	{
		try
		{
			//System.out.println("end tran");
			db.endTransaction();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
	}

	public void rollback()
	{

	}

	public void compact()
	{
		try
		{
			// release some unused memory by sqlite
			SQLiteDatabase.releaseMemory();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
	}

	public void _clear_database_()
	{
		synchronized (PointProvider.access_lock)
		{

			try
			{
				db.execSQL(String.format("delete from %s;", this.cache_table));
			}
			catch (SQLiteException e)
			{
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
			}

			try
			{
				db.execSQL(String.format("drop table %s;", this.cache_table));
			}
			catch (SQLiteException e)
			{
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
			}

			this.create_db();
		}
	}

	public GeocacheCoordinate get_point_full(String name)
	{
		synchronized (PointProvider.access_lock)
		{

			String whereClause;
			whereClause = String.format(" name='%s'", name);

			Cursor c = db.query(this.cache_table, null, whereClause, null, null, null, null);
			int col_logs = c.getColumnIndex("logs");
			//int col_loname = c.getColumnIndex("loname");
			int col_terrain = c.getColumnIndex("terrain");
			int col_waypoints = c.getColumnIndex("waypoints");
			int col_marked = c.getColumnIndex("marked");
			int col_logas = c.getColumnIndex("logas");
			int col_owner = c.getColumnIndex("owner");
			int col_images = c.getColumnIndex("images");
			int col_guid = c.getColumnIndex("guid");
			int col_size = c.getColumnIndex("size");
			int col_title = c.getColumnIndex("title");
			//int col_lotitle = c.getColumnIndex("lotitle");
			int col_lon = c.getColumnIndex("lon");
			int col_logdate = c.getColumnIndex("logdate");
			int col_desc = c.getColumnIndex("desc");
			int col_type = c.getColumnIndex("type");
			int col_status = c.getColumnIndex("status");
			int col_difficulty = c.getColumnIndex("difficulty");
			int col_lat = c.getColumnIndex("lat");
			int col_hints = c.getColumnIndex("hints");
			int col_name = c.getColumnIndex("name");
			int col_fieldnotes = c.getColumnIndex("fieldnotes");
			int col_notes = c.getColumnIndex("notes");
			int col_shortdesc = c.getColumnIndex("shortdesc");
			int col_found = c.getColumnIndex("found");
			int col_aagtl_status = c.getColumnIndex("aagtl_status");

			GeocacheCoordinate gc = null;

			/* Check if our result was valid. */
			if (c.getCount() > 0)
			{
				// use only first Result
				// normally (because of pirmary key) there should be only 1 result anyway!!
				c.moveToNext();
				gc = new GeocacheCoordinate(c.getDouble(col_lat), c.getDouble(col_lon), c.getString(col_name));

				// boolean "found"
				int temp_i = c.getInt(col_found);
				if (temp_i == 1)
				{
					gc.found = true;
				}
				else
				{
					gc.found = false;
				}

				// boolean "marked"
				temp_i = c.getInt(col_marked);
				if (temp_i == 1)
				{
					gc.marked = true;
				}
				else
				{
					gc.marked = false;
				}

				gc.logs = c.getString(col_logs);
				gc.waypoints = c.getString(col_waypoints);
				gc.owner = c.getString(col_owner);
				gc.images = c.getString(col_images);
				gc.guid = c.getString(col_guid);
				gc.title = c.getString(col_title);
				gc.log_date = c.getString(col_logdate);
				gc.desc = c.getString(col_desc);
				gc.type = c.getString(col_type);
				gc.hints = c.getString(col_hints);
				gc.fieldnotes = c.getString(col_fieldnotes);
				gc.notes = c.getString(col_notes);
				gc.shortdesc = c.getString(col_shortdesc);

				gc.terrain = c.getInt(col_terrain);
				gc.log_as = c.getInt(col_logas);
				gc.size = c.getInt(col_size);
				gc.status = c.getInt(col_status);
				gc.difficulty = c.getInt(col_difficulty);
				gc.aagtl_status = c.getInt(col_aagtl_status);

				// logs long varchar, loname long varchar,
				// terrain INTEGER, waypoints long varchar,
				// marked INTEGER, logas INTEGER, owner long varchar,
				// images long varchar, guid long varchar, size INTEGER,
				// title long varchar, lotitle long varchar, lon REAL,
				// logdate long varchar, desc long varchar,
				// type long varchar, status INTEGER, difficulty INTEGER, lat REAL,
				// hints long varchar, name long varchar primary key, fieldnotes long varchar,
				// notes long varchar, shortdesc long varchar, found INTEGER

				//System.out.println("DESC=" + gc.desc);
				//System.out.println("" + gc.title);
				//System.out.println("" + gc.name);
				//System.out.println("" + gc.found);

			}
			c.close();

			return gc;
		}
	}

	public List<GeocacheCoordinate> get_points_filter(Coordinate[] location, Boolean hide_found, int max_results)
	{
		// "max_results" is no honored at this moment!!
		synchronized (PointProvider.access_lock)
		{

			List<GeocacheCoordinate> caches_read = new ArrayList<GeocacheCoordinate>();
			caches_read.clear();

			String whereClause;
			Coordinate c1 = location[0];
			Coordinate c2 = location[1];

			//		whereClause = "((lat+0.001) >= (" + String.valueOf(Math.min(c1.lat, c2.lat))
			//				+ "-0.001) AND (lat-0.001) <= (" + String.valueOf(Math.max(c1.lat, c2.lat))
			//				+ "+0.001) ) AND ((lon+0.001) >= (" + String.valueOf(Math.min(c1.lon, c2.lon))
			//				+ "-0.001) AND (lon-0.001) <= (" + String.valueOf(Math.max(c1.lon, c2.lon))
			//				+ "+0.001) )";

			whereClause = "( lat >= '" + String.valueOf(Math.min(c1.lat, c2.lat)) + "' AND lat <= '" + String.valueOf(Math.max(c1.lat, c2.lat)) + "' AND lon >= '" + String.valueOf(Math.min(c1.lon, c2.lon)) + "' AND lon <= '" + String.valueOf(Math.max(c1.lon, c2.lon)) + "' )";

			//whereClause = " ( lat >= 48.30232421365064 AND lat <= 48.327839110917004 AND lon >=16.390586529361343 AND lon <= 16.419768963443374 )";

			if (hide_found)
			{
				whereClause = whereClause + " and (found = 0)";

			}

			if (this.filterstring != null)
			{
				whereClause = whereClause + " and ( " + this.filterstring + " )";
			}

			String[] cols = new String[8];
			cols[0] = "lat";
			cols[1] = "lon";
			cols[2] = "found";
			cols[3] = "title";
			cols[4] = "name";
			cols[5] = "type";
			cols[6] = "status";
			cols[7] = "aagtl_status";

			Cursor c = db.query(this.cache_table, cols, whereClause, null, null, null, null);
			int col_lat = c.getColumnIndex("lat");
			int col_lon = c.getColumnIndex("lon");
			int col_found = c.getColumnIndex("found");
			int col_title = c.getColumnIndex("title");
			int col_name = c.getColumnIndex("name");
			int col_type = c.getColumnIndex("type");
			int col_status = c.getColumnIndex("status");
			int col_aagtl_status = c.getColumnIndex("aagtl_status");

			/* Check if our result was valid. */
			if (c.getCount() > 0)
			{
				//System.out.println("" + c.getCount());
				int i = 0;
				/* Loop through all Results */
				while (c.moveToNext())
				{
					i++;
					/* Read Values and add current Entry to results. */
					//				System.out.println("wc=" + whereClause + " res=" + c.getDouble(col_lat) + " "
					//						+ c.getDouble(col_lon) + " " + c.getString(col_name));
					GeocacheCoordinate gc = new GeocacheCoordinate(c.getDouble(col_lat), c.getDouble(col_lon), c.getString(col_name));

					int temp_i = c.getInt(col_found);
					if (temp_i == 1)
					{
						gc.found = true;
					}
					else
					{
						gc.found = false;
					}

					gc.title = c.getString(col_title);
					gc.name = c.getString(col_name);
					gc.type = c.getString(col_type);
					gc.status = c.getInt(col_status);
					gc.aagtl_status = c.getInt(col_aagtl_status);
					caches_read.add(gc);

					//System.out.println("read from db type:" + String.valueOf(gc.type));
					//System.out.println("read from db type:" + String.valueOf(gc.found));
				}
			}
			c.close();

			return caches_read;
		}
	}

	public void add_point(GeocacheCoordinate p)
	{
		synchronized (PointProvider.access_lock)
		{

			ContentValues values = new ContentValues();

			values.put("title", p.title);
			values.put("name", p.name);
			values.put("type", p.type);
			values.put("status", p.status);
			String whereClause = String.format("name ='%s'", p.name);

			String[] tmp_str = new String[3];
			tmp_str[0] = "name";
			tmp_str[1] = "lat";
			tmp_str[2] = "lon";
			Cursor c = db.query(cache_table, tmp_str, whereClause, null, null, null, null);
			int must_update = c.getCount();

			int col_lat = c.getColumnIndex("lat");
			int col_lon = c.getColumnIndex("lon");

			double db_lat = 10101.0;
			double db_lon = 10101.0;

			/* Check if our result was valid. */
			if (c.getCount() > 0)
			{
				c.moveToNext();
				// get lat/lon from DB
				db_lat = c.getDouble(col_lat);
				db_lon = c.getDouble(col_lon);
			}

			//System.out.println("first=" + c.moveToFirst());
			//System.out.println("Wc: " + whereClause + " must_update: " + must_update);
			//System.out.println(String.valueOf(values.get("type")));
			//System.out.println(String.valueOf(values.get("found")));
			c.close();

			if (must_update > 0)
			{
				//System.out.println("xxxxx1 " + p.lat + " " + p.lon);
				if ((db_lat == 0.0) && (db_lon == 0.0))
				{
					// if lat/lon in Db are 0.0/0.0 then update lat/lon values also
					values.put("lat", p.lat);
					values.put("lon", p.lon);
				}

				//long ret =
				db.update(cache_table, values, whereClause, null);
				//System.out.println("update: " + ret);
			}
			else
			{
				// lat,lon seems to shift on every query. so only do on insert
				values.put("lat", p.lat);
				values.put("lon", p.lon);

				// seems to be buggy, so only do on insert
				if (p.found)
				{
					values.put("found", 1);
				}
				else
				{
					values.put("found", 0);
				}

				//long ret = 
				db.insert(cache_table, null, values);
				//System.out.println("insert result: " + ret);
			}
		}
	}

	public void close()
	{
		try
		{
			this.db.close();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
	}

	public void add_point_full(GeocacheCoordinate p)
	{
		synchronized (PointProvider.access_lock)
		{

			ContentValues values = new ContentValues();
			if (p.found)
			{
				values.put("found", 1);
			}
			else
			{
				values.put("found", 0);
			}

			if (p.marked)
			{
				values.put("marked", 1);
			}
			else
			{
				values.put("marked", 0);
			}

			values.put("title", p.title);
			//values.put("name", p.name); --> primary key, dont update!
			values.put("type", p.type);
			values.put("status", p.status);

			values.put("logs", p.logs);
			values.put("terrain", p.terrain);
			values.put("waypoints", p.waypoints);
			values.put("logas", p.log_as);
			values.put("owner", p.owner);
			values.put("images", p.images);
			values.put("guid", p.guid);
			values.put("size", p.size);
			values.put("title", p.title);
			values.put("lon", p.lon);
			values.put("logdate", p.log_date);
			values.put("desc", p.desc);
			values.put("type", p.type);
			values.put("status", p.status);
			values.put("difficulty", p.difficulty);
			values.put("lat", p.lat);
			values.put("hints", p.hints);
			values.put("name", p.name);
			values.put("fieldnotes", p.fieldnotes);
			values.put("notes", p.notes);
			values.put("shortdesc", p.shortdesc);

			String whereClause = String.format("name ='%s'", p.name);

			//
			//    ****** dont check for update/insert!!
			//    ****** assume always update here!!
			//
			//		String[] tmp_str = new String[1];
			//		tmp_str[0] = "name";
			//		Cursor c = db.query(cache_table, tmp_str, whereClause, null, null, null, null);
			//		int must_update = c.getCount();
			//		System.out.println("Wc: " + whereClause + " must_update: " + must_update);
			//		System.out.println(String.valueOf(values.get("type")));
			//		System.out.println(String.valueOf(values.get("found")));
			//		c.close();

			int must_update = 1;
			if (must_update > 0)
			{
				db.update(cache_table, values, whereClause, null);
			}
			else
			{
				// should not get here!!
				// call "add_point" first!!
			}
		}
	}

	public void reset_point_fn(GeocacheCoordinate p)
	{
		synchronized (PointProvider.access_lock)
		{
			ContentValues values = new ContentValues();
			values.put("logas", p.log_as);

			String whereClause = String.format("name ='%s'", p.name);
			//System.out.println(whereClause);
			db.update(cache_table, values, whereClause, null);
		}
	}

	public List<GeocacheCoordinate> get_new_fieldnotes()
	{
		synchronized (PointProvider.access_lock)
		{
			List<GeocacheCoordinate> caches_read = new ArrayList<GeocacheCoordinate>();
			caches_read.clear();

			String whereClause;
			whereClause = " logas <> " + GeocacheCoordinate.LOG_NO_LOG;

			String[] cols = new String[6];
			cols[0] = "found";
			cols[1] = "title";
			cols[2] = "name";
			cols[3] = "type";
			cols[4] = "fieldnotes";
			cols[5] = "logas";

			Cursor c = db.query(this.cache_table, cols, whereClause, null, null, null, null);
			int col_found = c.getColumnIndex("found");
			int col_title = c.getColumnIndex("title");
			int col_name = c.getColumnIndex("name");
			int col_type = c.getColumnIndex("type");
			int col_log_as = c.getColumnIndex("logas");
			int col_fieldnotes = c.getColumnIndex("fieldnotes");

			/* Check if our result was valid. */
			if (c.getCount() > 0)
			{
				//System.out.println("" + c.getCount());
				int i = 0;
				/* Loop through all Results */
				while (c.moveToNext())
				{
					i++;
					/* Read Values and add current Entry to results. */
					GeocacheCoordinate gc = new GeocacheCoordinate(0, 0, c.getString(col_name));

					int temp_i = c.getInt(col_found);
					if (temp_i == 1)
					{
						gc.found = true;
					}
					else
					{
						gc.found = false;
					}

					gc.title = c.getString(col_title);
					gc.name = c.getString(col_name);
					gc.type = c.getString(col_type);
					gc.log_as = c.getInt(col_log_as);
					gc.fieldnotes = c.getString(col_fieldnotes);
					caches_read.add(gc);
				}
			}
			c.close();

			return caches_read;
		}
	}

	public void add_point_fn(GeocacheCoordinate p)
	{
		synchronized (PointProvider.access_lock)
		{
			ContentValues values = new ContentValues();
			if (p.found)
			{
				values.put("found", 1);
			}
			else
			{
				values.put("found", 0);
			}

			values.put("aagtl_status", p.aagtl_status);
			values.put("logas", p.log_as);
			values.put("logdate", p.log_date);
			values.put("fieldnotes", p.fieldnotes);

			String whereClause = String.format("name ='%s'", p.name);
			//System.out.println(whereClause);
			//System.out.println("aagtl_status=" + p.aagtl_status);
			db.update(cache_table, values, whereClause, null);
		}
	}

	public void set_filter(String new_filter)
	{
		this.filterstring = new_filter;
	}

	public void clear_filter()
	{
		this.filterstring = null;
	}
}
