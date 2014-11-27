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

package net.jaqpot.netcounter.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "network.db";

	private static final int DATABASE_VERSION = 2;

	private static final DateFormat DF_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	private static final DateFormat DF_DATE = new SimpleDateFormat("yyyy-MM-dd");

	private static final DateFormat DF_LOCALE = DateFormat.getDateTimeInstance();

	private static final DateFormat DF_DATE_LOCALE = DateFormat.getDateInstance();

	/**
	 * NetCounter table.
	 */
	public static final class NetCounter implements BaseColumns {
		// This class cannot be instantiated
		private NetCounter() {
		}

		public static final String TABLE_NAME = "counter";
		public static final String INTERFACE = "interface";
		public static final String LAST_RX = "last_rx";
		public static final String LAST_TX = "last_tx";
		public static final String LAST_UPDATE = "last_update";
		public static final String LAST_RESET = "last_reset";
	}

	/**
	 * Counters table.
	 */
	public static final class Counters implements BaseColumns {
		// This class cannot be instantiated
		private Counters() {
		}

		public static final String TABLE_NAME = "counters";
		public static final String INTERFACE = "interface";
		public static final String TYPE = "type";
		public static final String VALUE = "value";
		public static final String POS = "position";
	}

	/**
	 * NetCounter table.
	 */
	public static final class DailyCounter implements BaseColumns {
		// This class cannot be instantiated
		private DailyCounter() {
		}

		public static final String TABLE_NAME = "daily";
		public static final String INTERFACE = "interface";
		public static final String DAY = "day";
		public static final String RX = "rx";
		public static final String TX = "tx";
		public static final String LAST_UPDATE = "last_update";
	}

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + NetCounter.TABLE_NAME + " (" + NetCounter._ID
				+ " INTEGER PRIMARY KEY," + NetCounter.INTERFACE + " TEXT," + NetCounter.LAST_RX
				+ " LONG," + NetCounter.LAST_TX + " LONG," + NetCounter.LAST_UPDATE + " DATETIME,"
				+ NetCounter.LAST_RESET + " DATETIME);");
		db.execSQL("CREATE TABLE " + Counters.TABLE_NAME + " (" + Counters._ID
				+ " INTEGER PRIMARY KEY," + Counters.INTERFACE + " TEXT," + Counters.TYPE
				+ " INTEGER," + Counters.VALUE + " TEXT," + Counters.POS + " INTEGER);");
		db.execSQL("CREATE TABLE " + DailyCounter.TABLE_NAME + " (" + DailyCounter._ID
				+ " INTEGER PRIMARY KEY," + DailyCounter.INTERFACE + " TEXT," + DailyCounter.DAY
				+ " DATE," + DailyCounter.RX + " LONG," + DailyCounter.TX + " LONG,"
				+ DailyCounter.LAST_UPDATE + " DATETIME);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(getClass().getName(), "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");

		if (oldVersion == 1) {
			db.execSQL("DROP TABLE IF EXISTS " + NetCounter.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Counters.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + DailyCounter.TABLE_NAME);
			onCreate(db);
		}
	}

	public static String getDateTime(Calendar date) {
		return DF_DATETIME.format(date.getTime());
	}

	public static String getDate(Calendar date) {
		return DF_DATE.format(date.getTime());
	}

	public static String getLocaleDateTime(Calendar date) {
		return DF_LOCALE.format(date.getTime());
	}

	public static String getLocaleDate(Calendar date) {
		return DF_DATE_LOCALE.format(date.getTime());
	}

	public static Calendar parseDateTime(String date) {
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(DF_DATETIME.parse(date));
		} catch (ParseException e) {
			Log.e(DatabaseHelper.class.getName(), "Unable to parse date", e);
		}
		return c;
	}

	public static Calendar parseDate(String date) {
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(DF_DATE.parse(date));
		} catch (ParseException e) {
			Log.e(DatabaseHelper.class.getName(), "Unable to parse date", e);
		}
		return c;
	}

}
