package com.hectorone.multismssender;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.util.Log;

/**
 * Simple groups database access helper class. Defines the basic CRUD operations
 * for the group add example, and gives the ability to list all groups as well
 * as retrieve or modify a specific group.
 * 
 */
public class GroupsDbAdapter {

	public static final String KEY_GROUP_NAME             = "name";
	public static final String KEY_GROUP_ROWID            = "_id";

	public static final String KEY_GROUP_TO_PHONE_ROWID   = "_id";
	public static final String KEY_GROUP_TO_PHONE_GROUPID = "gid";
	public static final String KEY_GROUP_TO_PHONE_PHONEID = "pid";

	private static final String TAG = "groupsDbAdapter";
	private GroupDbHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_GROUP_CREATE          = "create table groups (_id integer primary key autoincrement, "
			+ "name text not null);";

	private static final String DATABASE_GROUP_TO_PHONE_CREATE = "create table group_TO_PHONE (_id integer primary key autoincrement, "
			+ "gid integer not null, pid integer not null);";

	private static final String DATABASE_NAME                  = "dataGroup";
	private static final String DATABASE_GROUP_TABLE           = "groups";
	private static final String DATABASE_GROUP_TO_PHONE_TABLE  = "group_TO_PHONE";
	private static final int DATABASE_VERSION                  = 4;

	private final Context mCtx;

	private static class GroupDbHelper extends SQLiteOpenHelper {

		GroupDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_GROUP_CREATE);
			db.execSQL(DATABASE_GROUP_TO_PHONE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS groups");
			db.execSQL("DROP TABLE IF EXISTS group_TO_PHONE");
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public GroupsDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the groups database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public GroupsDbAdapter open() throws SQLException {
		mDbHelper = new GroupDbHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();

		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Create a new group using the name provided. If the group is successfully
	 * created return the new rowId for that group, otherwise return a -1 to
	 * indicate failure.
	 * 
	 * @param name
	 *            the name of the group
	 */
	public long createGroup(String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_GROUP_NAME, name);

		return mDb.insert(DATABASE_GROUP_TABLE, null, initialValues);
	}

	/**
	 * Delete the group with the given rowId
	 * 
	 * @param rowId
	 *            id of group to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteGroup(long rowId) {

		return mDb.delete(DATABASE_GROUP_TABLE, KEY_GROUP_ROWID + "=" + rowId,
				null) > 0
				&& mDb.delete(DATABASE_GROUP_TO_PHONE_TABLE,
						KEY_GROUP_TO_PHONE_GROUPID + "=" + rowId, null) > 0;

	}

	/**
	 * Return a Cursor over the list of all groups in the database
	 * 
	 * @return Cursor over all groups
	 */
	public Cursor fetchAllGroups() {

		return mDb.query(DATABASE_GROUP_TABLE, new String[] { KEY_GROUP_ROWID,
				KEY_GROUP_NAME }, null, null, null, null, KEY_GROUP_NAME);
	}

	/**
	 * Return a Cursor positioned at the group that matches the given rowId
	 * 
	 * @param rowId
	 *            id of group to retrieve
	 * @return Cursor positioned to matching group, if found
	 * @throws SQLException
	 *             if group could not be found/retrieved
	 */
	public Cursor fetchGroup(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_GROUP_TABLE, new String[] { KEY_GROUP_ROWID,
				KEY_GROUP_NAME }, KEY_GROUP_ROWID + "=" + rowId, null, null,
				null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * @param rowId
	 *            id of group to update
	 * @param name
	 *            value to set group name to
	 */
	public boolean updateGroup(long rowId, String name) {
		ContentValues args = new ContentValues();
		args.put(KEY_GROUP_NAME, name);

		return mDb.update(DATABASE_GROUP_TABLE, args, KEY_GROUP_ROWID + "="
				+ rowId, null) > 0;
	}

	public Cursor fetchPhonesFromGroup(long groupId) {
		Cursor mCursor    = mDb.query(true, DATABASE_GROUP_TO_PHONE_TABLE,
				new String[] { KEY_GROUP_TO_PHONE_PHONEID },
				KEY_GROUP_TO_PHONE_GROUPID + "=" + groupId, null, null, null,
				null, null);
		Cursor userCursor = null;
		int phoneIdIdx    = mCursor.getColumnIndex(KEY_GROUP_TO_PHONE_PHONEID);
		if (mCursor != null) {
			userCursor = mCtx.getContentResolver()
					.query(Data.CONTENT_URI,
							new String[] { Data._ID, Data.MIMETYPE,
									Phone.NUMBER, Phone.TYPE, Phone.LABEL,
									Contacts.DISPLAY_NAME },
							Data._ID + " IN "
									+ cursorToStringList(mCursor, phoneIdIdx),
							null, Contacts.DISPLAY_NAME);
		}
		mCursor.close();
		return userCursor;
	}

	public long addPhoneToGroup(long groupId, long phoneId) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_GROUP_TO_PHONE_GROUPID, groupId);
		initialValues.put(KEY_GROUP_TO_PHONE_PHONEID, phoneId);

		return mDb.insert(DATABASE_GROUP_TO_PHONE_TABLE, null, initialValues);
	}

	public boolean removePhoneToGroup(long groupId, long phoneId) {
		return mDb.delete(DATABASE_GROUP_TO_PHONE_TABLE,
				KEY_GROUP_TO_PHONE_GROUPID + "=" + groupId + " AND "
						+ KEY_GROUP_TO_PHONE_PHONEID + "=" + phoneId, null) > 0;
	}

	public String cursorToStringList(Cursor cursor, int columnIdx) {
		cursor.moveToFirst();
		String list = "( ";
		while (!cursor.isAfterLast()) {
			list += cursor.getString(columnIdx);
			if (!cursor.isLast()) {
				list += " , ";
			}
			cursor.moveToNext();
		}
		list += " )";
		return list;
	}
}
