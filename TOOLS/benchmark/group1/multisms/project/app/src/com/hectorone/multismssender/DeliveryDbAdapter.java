package com.hectorone.multismssender;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.UserDictionary.Words;
import android.text.TextUtils;
import android.util.Log;

public class DeliveryDbAdapter extends ContentProvider {

	public static final String PROVIDER_NAME       = "com.hectorone.multismssender.provider";
	public static final Uri CONTENT_DELIVERY_URI   = Uri.parse("content://"
			+ PROVIDER_NAME + "/delivery");
	public static final Uri CONTENT_MESSAGE_URI    = Uri.parse("content://"
			+ PROVIDER_NAME + "/message");

	private static final int ENTRIES     = 1;
	private static final int ENTRY_ID    = 2;
	private static final int MESSAGES    = 3;
	private static final int MESSAGES_ID = 4;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(PROVIDER_NAME, "delivery", ENTRIES);
		uriMatcher.addURI(PROVIDER_NAME, "delivery/#", ENTRY_ID);
		uriMatcher.addURI(PROVIDER_NAME, "message", MESSAGES);
		uriMatcher.addURI(PROVIDER_NAME, "message/#", MESSAGES_ID);
	}

	public static final String KEY_DELIVERY_ENTRY_ROWID      = "_id";
	public static final String KEY_DELIVERY_ENTRY_NAME       = "name";
	public static final String KEY_DELIVERY_ENTRY_NUMBER     = "number";
	public static final String KEY_DELIVERY_ENTRY_DELIVERED  = "delivered";
	public static final String KEY_DELIVERY_ENTRY_MESSAGE_ID = "message_id";

	public static final String KEY_MESSAGE_ROWID             = "_id";
	public static final String KEY_MESSAGE_NAME              = "name";
	public static final String KEY_MESSAGE_DATE              = "date";

	private static final String TAG                          = "deliveryDbAdapter";
	private DeliveryDbHelper mDbHelper;
	// private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */

	public static final String DATABASE_NAME                  = "delivery";
	public static final String DATABASE_DELIVERY_ENTRY_TABLE  = "delivery_entry";
	public static final String DATABASE_MESSAGE_TABLE         = "message";
	public static final String DATABASE_DELIVERY_ENTRY_CREATE = "create table "
			+ DATABASE_DELIVERY_ENTRY_TABLE + " (" + KEY_DELIVERY_ENTRY_ROWID
			+ " integer primary key autoincrement, " + KEY_DELIVERY_ENTRY_NAME
			+ " text not null," + KEY_DELIVERY_ENTRY_NUMBER + " text not null,"
			+ KEY_DELIVERY_ENTRY_DELIVERED + " integer,"
			+ KEY_DELIVERY_ENTRY_MESSAGE_ID + " integer);";

	public static final String DATABASE_DELIVERY_CREATE       = "create table "
			+ DATABASE_MESSAGE_TABLE + " (" + KEY_MESSAGE_ROWID
			+ " integer primary key autoincrement, " + KEY_MESSAGE_NAME
			+ " text not null," + KEY_MESSAGE_DATE + " text not null);";

	public static final int DATABASE_VERSION                 = 4;

	private static HashMap<String, String> sDeliveryProjectionMap;
	private static HashMap<String, String> sMessageProjectionMap;

	static {
		sDeliveryProjectionMap = new HashMap<String, String>();
		sMessageProjectionMap  = new HashMap<String, String>();
		sDeliveryProjectionMap.put(KEY_DELIVERY_ENTRY_ROWID,
				KEY_DELIVERY_ENTRY_ROWID);
		sDeliveryProjectionMap.put(KEY_DELIVERY_ENTRY_NAME,
				KEY_DELIVERY_ENTRY_NAME);
		sDeliveryProjectionMap.put(KEY_DELIVERY_ENTRY_NUMBER,
				KEY_DELIVERY_ENTRY_NUMBER);
		sDeliveryProjectionMap.put(KEY_DELIVERY_ENTRY_DELIVERED,
				KEY_DELIVERY_ENTRY_DELIVERED);
		sDeliveryProjectionMap.put(KEY_DELIVERY_ENTRY_MESSAGE_ID,
				KEY_DELIVERY_ENTRY_MESSAGE_ID);

		sMessageProjectionMap.put(KEY_MESSAGE_ROWID, KEY_MESSAGE_ROWID);
		sMessageProjectionMap.put(KEY_MESSAGE_NAME, KEY_MESSAGE_NAME);
		sMessageProjectionMap.put(KEY_MESSAGE_DATE, KEY_MESSAGE_DATE);
	}

	private static class DeliveryDbHelper extends SQLiteOpenHelper {

		DeliveryDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);

		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_DELIVERY_ENTRY_CREATE);
			db.execSQL(DATABASE_DELIVERY_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_DELIVERY_ENTRY_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_MESSAGE_TABLE);
			onCreate(db);
		}
	}

	// *********************** Content Provider ****************************************

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;

		switch (uriMatcher.match(uri)) {
		case ENTRIES:
			count = db.delete(DATABASE_DELIVERY_ENTRY_TABLE, selection,
					selectionArgs);
			break;
		case ENTRY_ID: {
			String id = uri.getPathSegments().get(1);
			count = db.delete(
					DATABASE_DELIVERY_ENTRY_TABLE,
					KEY_DELIVERY_ENTRY_ROWID
							+ "="
							+ id
							+ (!TextUtils.isEmpty(selection) ? " AND ("
									+ selection + ')' : ""), selectionArgs);

			break;
		}
		case MESSAGES:
			count = db.delete(DATABASE_MESSAGE_TABLE, selection, selectionArgs);

			break;
		case MESSAGES_ID: {
			String id = uri.getPathSegments().get(1);
			count = db.delete(DATABASE_DELIVERY_ENTRY_TABLE, KEY_MESSAGE_ROWID
					+ "="
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);

			break;
		}
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}
		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case ENTRIES:
			return "vnd.android.cursor.dir/vnd." + PROVIDER_NAME + ".entry";
		case ENTRY_ID:
			return "vnd.android.cursor.item/vnd." + PROVIDER_NAME + ".entry";
		case MESSAGES:
			return "vnd.android.cursor.dir/vnd." + PROVIDER_NAME + ".message";
		case MESSAGES_ID:
			return "vnd.android.cursor.item/vnd." + PROVIDER_NAME + ".message";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		ContentValues initialValues;
		if (values != null) {
			initialValues = new ContentValues(values);
		} else {
			initialValues = new ContentValues();
		}

		switch (uriMatcher.match(uri)) {
		case ENTRIES: {
			if (initialValues.containsKey(KEY_DELIVERY_ENTRY_NAME) == false) {
				throw new SQLException(KEY_DELIVERY_ENTRY_NAME
						+ " must be specified");
			}
			if (initialValues.containsKey(KEY_DELIVERY_ENTRY_NUMBER) == false) {
				throw new SQLException(KEY_DELIVERY_ENTRY_NUMBER
						+ " must be specified");
			}
			if (initialValues.containsKey(KEY_DELIVERY_ENTRY_MESSAGE_ID) == false) {
				throw new SQLException(KEY_DELIVERY_ENTRY_MESSAGE_ID
						+ " must be specified");
			}
			initialValues.put(KEY_DELIVERY_ENTRY_DELIVERED, 0);

			SQLiteDatabase db = mDbHelper.getWritableDatabase();
			long rowId = db.insert(DATABASE_DELIVERY_ENTRY_TABLE, null,
					initialValues);
			if (rowId > 0) {
				Uri newUri = ContentUris.withAppendedId(CONTENT_DELIVERY_URI,
						rowId);
				getContext().getContentResolver().notifyChange(newUri, null);
				return newUri;
			}
			throw new SQLException("Failed to insert row into " + uri);

		}

		case MESSAGES: {
			if (initialValues.containsKey(KEY_MESSAGE_NAME) == false) {
				throw new SQLException(KEY_MESSAGE_NAME + " must be specified");
			}
			if (initialValues.containsKey(KEY_MESSAGE_DATE) == false) {
				throw new SQLException(KEY_MESSAGE_DATE + " must be specified");
			}

			SQLiteDatabase db = mDbHelper.getWritableDatabase();
			long rowId = db.insert(DATABASE_MESSAGE_TABLE, null, initialValues);
			if (rowId > 0) {
				Uri newUri = ContentUris.withAppendedId(CONTENT_MESSAGE_URI,
						rowId);
				getContext().getContentResolver().notifyChange(newUri, null);
				return newUri;
			}
			throw new SQLException("Failed to insert row into " + uri);

		}

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}
	}

	@Override
	public boolean onCreate() {
		mDbHelper = new DeliveryDbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (uriMatcher.match(uri)) {
		case ENTRIES:
			qb.setTables(DATABASE_DELIVERY_ENTRY_TABLE);
			qb.setProjectionMap(sDeliveryProjectionMap);

			break;
		case ENTRY_ID:
			qb.setTables(DATABASE_DELIVERY_ENTRY_TABLE);
			qb.setProjectionMap(sDeliveryProjectionMap);
			qb.appendWhere(KEY_DELIVERY_ENTRY_ROWID + "="
					+ uri.getPathSegments().get(1));

			break;
		case MESSAGES:
			qb.setTables(DATABASE_MESSAGE_TABLE);
			qb.setProjectionMap(sMessageProjectionMap);

			break;
		case MESSAGES_ID:
			qb.setTables(DATABASE_MESSAGE_TABLE);
			qb.setProjectionMap(sMessageProjectionMap);
			qb.appendWhere(KEY_MESSAGE_ROWID + "="
					+ uri.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}

		// Run the query
		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, sortOrder);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;

		switch (uriMatcher.match(uri)) {
		case ENTRIES:
			count = db.update(DATABASE_DELIVERY_ENTRY_TABLE, values, selection,
					selectionArgs);
			break;
		case ENTRY_ID: {
			String id = uri.getPathSegments().get(1);
			count = db.update(DATABASE_DELIVERY_ENTRY_TABLE, values, Words._ID
					+ "="
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);

			break;
		}
		case MESSAGES:
			count = db.update(DATABASE_MESSAGE_TABLE, values, selection,
					selectionArgs);

			break;
		case MESSAGES_ID: {
			String id = uri.getPathSegments().get(1);
			count = db.update(DATABASE_MESSAGE_TABLE, values, Words._ID
					+ "="
					+ id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);

			break;
		}

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);

		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
