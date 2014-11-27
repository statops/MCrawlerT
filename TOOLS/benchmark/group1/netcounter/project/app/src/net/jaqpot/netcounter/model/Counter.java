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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.model.DatabaseHelper.Counters;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class Counter extends AbstractModel {

	public static final String NUMBER = "number";

	public static final String DAY = "day";

	public static final String ALERT_VALUE = "alert-value";

	public static final String ALERT_UNIT = "alert-unit";

	public static final String ALERT_BYTES = "alert-bytes";

	public static final int TOTAL = 0;

	public static final int MONTHLY = 1;

	public static final int LAST_DAYS = 2;

	public static final int SINGLE_DAY = 3;

	public static final int WEEKLY = 4;

	public static final int LAST_MONTH = 5;

	private long mId;

	private final Interface mInterface;

	private int mType = 0;

	private String mStringType;

	private final long[] mBytes = { 0, 0 };

	private final String[] mStringBytes = { "", "" };

	private final String[] mStringDate = { "", "" };

	private String mStringTotal;

	private final Properties mProperties = new Properties();

	private String mWhere;

	private boolean mIsDirtyInternal = false;

	public Counter(long id, Interface inter) {
		this(inter);
		mId = id;
		mWhere = Counters._ID + "=" + id;
	}

	public Counter(Interface inter) {
		mInterface = inter;
		mStringType = prettyType();
	}

	public long getId() {
		return mId;
	}

	public Interface getInterface() {
		return mInterface;
	}

	public synchronized void setType(int type) {
		if (mType != type) {
			// Remove previously set properties.
			mProperties.remove(ALERT_VALUE);
			mProperties.remove(ALERT_UNIT);
			mProperties.remove(ALERT_BYTES);
			mProperties.remove(NUMBER);
			mProperties.remove(DAY);
			// Set the new type.
			setType0(type);
			mIsDirtyInternal = true;
			setDirty(true);
			fireModelChanged();
		}
	}

	private void setType0(int type) {
		mType = type;
		mStringType = prettyType();
	}

	public synchronized int getType() {
		return mType;
	}

	public synchronized String getTypeAsString() {
		return mStringType;
	}

	public synchronized void setProperty(String name, String value) {
		mProperties.setProperty(name, value);
		if (NUMBER.equals(name)) {
			mStringType = prettyType();
		}
		mIsDirtyInternal = true;
		setDirty(true);
		fireModelChanged();
	}

	public synchronized String getProperty(String name) {
		return mProperties.getProperty(name);
	}

	public synchronized String getProperty(String name, String defaultValue) {
		return mProperties.getProperty(name, defaultValue);
	}

	public synchronized Object removeProperty(String key) {
		mIsDirtyInternal = true;
		setDirty(true);
		fireModelChanged();
		return mProperties.remove(key);
	}

	private synchronized void setBytes(long[] bytes) {
		boolean change = false;
		for (int i = 0; i < 2; i++) {
			if (mBytes[i] != bytes[i]) {
				change = true;
				mBytes[i] = bytes[i];
				mStringBytes[i] = null;
			}
		}
		if (change) {
			mStringTotal = null;
		}
	}

	public synchronized long[] getBytes() {
		return mBytes;
	}

	public synchronized String[] getBytesAsString() {
		for (int i = 0; i < 2; i++) {
			if (mStringBytes[i] == null) {
				mStringBytes[i] = prettyBytes(mBytes[i]);
			}
		}
		return mStringBytes;
	}

	/**
	 * TODO This should not be in this model object. Use a wrapper for this!?
	 */
	public synchronized String getTotalAsString() {
		if (mStringTotal == null) {
			mStringTotal = prettyBytes(mBytes[0] + mBytes[1]);
		}
		return mStringTotal;
	}

	/**
	 * TODO Return a {@link Calendar} object instead of a formatted string.
	 */
	public synchronized String getStartDate() {
		return mStringDate[0];
	}

	/**
	 * TODO Return a {@link Calendar} object instead of a formatted string.
	 */
	public synchronized String getEndDate() {
		return mStringDate[1];
	}

	public void load(SQLiteDatabase db) {
		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(Counters.TABLE_NAME);

		Cursor c = query.query(db, null, mWhere, null, null, null, null);
		if (c.getCount() > 0) {
			c.moveToNext();
			String v = c.getString(c.getColumnIndex(Counters.VALUE));
			if (v != null) {
				try {
					mProperties.load(new ByteArrayInputStream(v.getBytes()));
				} catch (IOException e) {
					Log.e(getClass().getName(), "Unable to load properties", e);
				}
			}
			setType0(c.getInt(c.getColumnIndex(Counters.TYPE)));
		}
		c.close();

		// Loads bytes.
		loadBytes(db);
	}

	public void insert(SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(Counters.INTERFACE, mInterface.getName());
		values.put(Counters.TYPE, mType);
		putProperties(values);
		// values.put(Counters.POS, pos);
		mId = db.insert(Counters.TABLE_NAME, null, values);
		mWhere = Counters._ID + "=" + mId;

		setDirty(true);
		fireModelChanged();
	}

	public void remove(SQLiteDatabase db) {
		db.delete(Counters.TABLE_NAME, mWhere, null);
	}

	public void update(SQLiteDatabase db) {
		if (mIsDirtyInternal) {
			ContentValues values = new ContentValues();
			values.put(Counters.TYPE, mType);
			putProperties(values);
			db.update(Counters.TABLE_NAME, values, mWhere, null);
			mIsDirtyInternal = false;
		}

		// Loads bytes.
		loadBytes(db);
	}

	private static String prettyBytes(long value) {
		StringBuilder sb = new StringBuilder();
		int i;
		if (value < 1024L) {
			sb.append(String.valueOf(value));
			i = 0;
		} else if (value < 1048576L) {
			sb.append(String.format("%.1f", value / 1024.0));
			i = 1;
		} else if (value < 1073741824L) {
			sb.append(String.format("%.2f", value / 1048576.0));
			i = 2;
		} else if (value < 1099511627776L) {
			sb.append(String.format("%.3f", value / 1073741824.0));
			i = 3;
		} else {
			sb.append(String.format("%.4f", value / 1099511627776.0));
			i = 4;
		}
		sb.append(' ');
		sb.append(NetCounterApplication.BYTE_UNITS[i]);
		return sb.toString();
	}

	private String prettyType() {
		switch (mType) {
		case SINGLE_DAY:
			String n = getProperty(NUMBER, "0");
			int number = Integer.valueOf(n);
			return NetCounterApplication.COUNTER_SINGLE_DAY[number].toString();
		case LAST_MONTH:
			n = getProperty(NUMBER, "0");
			number = Integer.valueOf(n);
			return NetCounterApplication.COUNTER_LAST_MONTH[number].toString();
		case LAST_DAYS:
			n = getProperty(NUMBER, "0");
			number = Integer.valueOf(n);
			return NetCounterApplication.COUNTER_LAST_DAYS[number].toString();
		case MONTHLY:
			n = getProperty(NUMBER, "0");
			number = Integer.valueOf(n);
			String t = NetCounterApplication.COUNTER_TYPES[mType].toString();
			String m = NetCounterApplication.COUNTER_MONTHLY[number].toString();
			if (number == 0) {
				return t;
			}
			return t + " (" + m + ")";
		case WEEKLY:
			n = getProperty(NUMBER, "0");
			number = Integer.valueOf(n);
			t = NetCounterApplication.COUNTER_TYPES[mType].toString();
			m = NetCounterApplication.COUNTER_WEEKLY[number].toString();
			if (number == 0) {
				return t;
			}
			return t + " (" + m + ")";
		default:
			return NetCounterApplication.COUNTER_TYPES[mType].toString();
		}
	}

	private synchronized void loadBytes(SQLiteDatabase db) {
		// Loads bytes.
		Calendar c = Calendar.getInstance();
		switch (getType()) {
		case LAST_DAYS:
			int number = Integer.parseInt(getProperty(NUMBER, "0"));
			if (number == 0) {
				number = 7;
			} else if (number == 1) {
				number = 14;
			} else {
				number = 30;
			}
			c.add(Calendar.DAY_OF_YEAR, -number);
			setBytes(mInterface.getInterfaceBytes(db, c, null));
			// Updates start and end dates.
			mStringDate[0] = DatabaseHelper.getLocaleDate(c);
			c = Calendar.getInstance();
			mStringDate[1] = DatabaseHelper.getLocaleDate(c);
			break;
		case SINGLE_DAY:
			number = Integer.parseInt(getProperty(NUMBER, "0"));
			c.add(Calendar.DAY_OF_YEAR, -number);
			setBytes(mInterface.getInterfaceBytes(db, c, c));
			// Updates start and end dates.
			mStringDate[0] = DatabaseHelper.getLocaleDate(c);
			mStringDate[1] = mStringDate[0];
			break;
		case Counter.LAST_MONTH:
			String value = getProperty(DAY, "1981-01-01");
			number = Integer.valueOf(getProperty(NUMBER, "0"));

			String[] a = value.split("-");
			int v = Integer.valueOf(a[2]);
			Calendar cc = Calendar.getInstance();
			cc.setTimeInMillis(c.getTimeInMillis());
			cc.set(Calendar.DAY_OF_MONTH, v);
			if (c.before(cc)) {
				c.add(Calendar.MONTH, -1);
			}
			c.add(Calendar.MONTH, -number);
			c.set(Calendar.DAY_OF_MONTH, v);
			// We are allowed to reuse "cc".
			cc.setTimeInMillis(c.getTimeInMillis());
			cc.add(Calendar.MONTH, -1);
			// Do not include the first day of the new month...
			c.add(Calendar.DAY_OF_YEAR, -1);
			setBytes(mInterface.getInterfaceBytes(db, cc, c));
			// Updates start and end dates.
			mStringDate[0] = DatabaseHelper.getLocaleDate(cc);
			mStringDate[1] = DatabaseHelper.getLocaleDate(c);
			break;
		case Counter.MONTHLY:
			value = getProperty(DAY, "1981-01-01");
			number = Integer.valueOf(getProperty(NUMBER, "0"));

			a = value.split("-");
			v = Integer.valueOf(a[2]);
			cc = Calendar.getInstance();
			cc.setTimeInMillis(c.getTimeInMillis());
			cc.set(Calendar.DAY_OF_MONTH, v);
			if (c.before(cc)) {
				c.add(Calendar.MONTH, -1);
			}
			c.add(Calendar.MONTH, -number);
			c.set(Calendar.DAY_OF_MONTH, v);
			if (NetCounterApplication.LOG_ENABLED) {
				Log.d(getClass().getName(), "Monthly reset: " + c.getTime());
			}
			setBytes(mInterface.getInterfaceBytes(db, c, null));
			// Updates start and end dates.
			mStringDate[0] = DatabaseHelper.getLocaleDate(c);
			c = Calendar.getInstance();
			mStringDate[1] = DatabaseHelper.getLocaleDate(c);
			break;
		case Counter.WEEKLY:
			int day = Integer.valueOf(getProperty(DAY, "0"));
			number = Integer.valueOf(getProperty(NUMBER, "0"));

			cc = Calendar.getInstance();
			cc.setTimeInMillis(c.getTimeInMillis());
			cc.set(Calendar.DAY_OF_WEEK, day + 1);
			if (c.before(cc)) {
				c.add(Calendar.WEEK_OF_YEAR, -1);
			}
			c.add(Calendar.WEEK_OF_YEAR, -number);
			c.set(Calendar.DAY_OF_WEEK, day + 1);
			if (NetCounterApplication.LOG_ENABLED) {
				Log.d(getClass().getName(), "Weekly reset: " + c.getTime());
			}
			setBytes(mInterface.getInterfaceBytes(db, c, null));
			// Updates start and end dates.
			mStringDate[0] = DatabaseHelper.getLocaleDate(c);
			c = Calendar.getInstance();
			mStringDate[1] = DatabaseHelper.getLocaleDate(c);
			break;
		default:
			setBytes(mInterface.getInterfaceBytes(db, null, null));
			// Updates start and end dates.
			mStringDate[1] = DatabaseHelper.getLocaleDate(c);
			c = mInterface.getLastReset();
			mStringDate[0] = DatabaseHelper.getLocaleDate(c);
		}
	}

	private synchronized void putProperties(ContentValues values) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			mProperties.store(os, "");
			values.put(Counters.VALUE, os.toString());
		} catch (IOException e) {
			Log.e(getClass().getName(), "Unable to store properties", e);
		} finally {
			try {
				os.close();
			} catch (IOException e) {
			}
		}
	}

}
