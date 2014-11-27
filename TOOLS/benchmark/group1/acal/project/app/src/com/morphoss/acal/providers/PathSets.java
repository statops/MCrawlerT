/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.morphoss.acal.database.AcalDBHelper;

/**
 * <p>This ContentProvider interfaces with the dav_path_set table in the database.</p>
 *
 * <p>
 * This class accepts URI specifiers for it's operation in the following forms:
 * </p>
 * <ul>
 * <li>content://pathsets</li>
 * <li>content://pathsets/begin - Can only be used with update(), starts a new transaction.</li>
 * <li>content://pathsets/approve - Can only be used with update(), approves all changes since transaction started.</li>
 * <li>content://pathsets/commit - Ends transaction. Changes are only commited if transaction has been approved.</li>
 * <li>content://pathsets/# - The path_set of a specific row</li>
 * <li>content://pathsets/servers/# - A unique set of all path sets for a given server</li>
 * <li>content://pathsets/servers/#/type/# - Path sets for a given server of a given type</li>
 * </ul>
 * <p>
 * There is no ability to specify an individual path_set row other than to provide
 * additional selectionArgs to the delete(), update() or query() methods.
 * </p>
 *
 * @author Morphoss Ltd
 *
 */
public class PathSets extends ContentProvider {

	//Authority must match one defined in manifest!
	public static final String AUTHORITY = "pathsets";
    public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY);

    //Database + Table
    private SQLiteDatabase AcalDB;
    static final String DATABASE_TABLE = "dav_path_set";

    //Path definitions
    private static final int ROOT = 0;
    private static final int ALLSETS = 1;
    private static final int ROW_ID_SET = 2;
    private static final int SERVER_ID_SET = 3;
    private static final int SERVER_ID_TYPE_SET = 4;
    private static final int BEGIN_TRANSACTION = 5;
    private static final int END_TRANSACTION = 6;
    private static final int APPROVE_TRANSACTION = 7;

    //Creates Paths and assigns Path Definition Id's
    public static final UriMatcher uriMatcher = new UriMatcher(ROOT);
    static{
         uriMatcher.addURI(AUTHORITY, null, ALLSETS);
         uriMatcher.addURI(AUTHORITY, "#", ROW_ID_SET);
         uriMatcher.addURI(AUTHORITY, "servers/#", SERVER_ID_SET);
         uriMatcher.addURI(AUTHORITY, "servers/#/type/#", SERVER_ID_TYPE_SET);
         uriMatcher.addURI(AUTHORITY, "begin", BEGIN_TRANSACTION);
         uriMatcher.addURI(AUTHORITY, "commit", END_TRANSACTION);
         uriMatcher.addURI(AUTHORITY, "approve", APPROVE_TRANSACTION);
    }

    // Type definitions
    public static final int UNKNOWN = 0;
    public static final int PRINCIPAL_COLLECTION_SET = 1;
    public static final int CALENDAR_HOME_SET = 2;
    public static final int ADDRESSBOOK_HOME_SET = 3;

	//Table Fields - All other classes should use these constants to access fields.
    public static final String _ID = "_id";
	public static final String SERVER_ID = "server_id";
	public static final String SET_TYPE="set_type";
	public static final String PATH="path";
	public static final String COLLECTION_TAG="collection_tag";
	public static final String LAST_CHECKED="last_checked";
	public static final String NEEDS_SYNC="needs_sync";

	/*
	 * 	(non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count=0;
		String server_id;
		switch (uriMatcher.match(uri)){
		case ALLSETS:
			count = AcalDB.delete(
					DATABASE_TABLE,
					selection,
					selectionArgs);
			break;
		case ROW_ID_SET:
			String row_id = uri.getPathSegments().get(0);
			count = AcalDB.delete(
					DATABASE_TABLE,
					_ID + " = " + row_id +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		case SERVER_ID_SET:
			server_id = uri.getPathSegments().get(1);
			count = AcalDB.delete(
					DATABASE_TABLE,
					SERVER_ID + " = " + server_id +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		case SERVER_ID_TYPE_SET:
			server_id = uri.getPathSegments().get(1);
			String set_type = uri.getPathSegments().get(3);
			count = AcalDB.delete(
					DATABASE_TABLE,
					SERVER_ID + " = " + server_id + " AND " +
					SET_TYPE + " = " + set_type +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		default: throw new IllegalArgumentException(
				"Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		switch (uriMatcher.match(uri)) {
		//Get all Servers
		case ALLSETS:
		case ROW_ID_SET:
		case SERVER_ID_SET:
		case SERVER_ID_TYPE_SET:
			return "vnd.android.cursor.dir/vnd.morphoss.path_set";
		// We only deal with whole sets for this class
		//	return "vnd.android.cursor.item/vnd.morphoss.path_set";
		default:
			throw new IllegalArgumentException("Unsupported URI: "+uri);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//---add a new server---
		long rowID = AcalDB.insert(
				DATABASE_TABLE, "", values);

		//---if added successfully---
		if (rowID>0)
		{
			//TODO rowis does NOT translate correctly here!!!
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		Context context = getContext();
		AcalDBHelper dbHelper = new AcalDBHelper(context);
		AcalDB = dbHelper.getWritableDatabase();
		return (AcalDB == null)?false:true;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DATABASE_TABLE);
		
		String groupBy = null;

		if (uriMatcher.match(uri) == ROW_ID_SET)
			//---if getting a particular server---
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(0));

		if (uriMatcher.match(uri) == SERVER_ID_SET)
			//---if getting a particular server---
			sqlBuilder.appendWhere(SERVER_ID + " = " + uri.getPathSegments().get(1));

		if (uriMatcher.match(uri) == SERVER_ID_TYPE_SET)
			//---if getting a particular server && type ---
			sqlBuilder.appendWhere(SERVER_ID + " = " + uri.getPathSegments().get(1)+ " AND "+SET_TYPE + " = " + uri.getPathSegments().get(3));
		else {
			// We always group by path, unless we are filtering to a single type.
			groupBy = PATH;
		}

		// Not sure if we should return a sorted list.
//		if (sortOrder==null || sortOrder.equals("") )
//			sortOrder = SERVER_ID + ", " + SET_TYPE;

		Cursor c = sqlBuilder.query(
				AcalDB,
				projection,
				selection,
				selectionArgs,
				groupBy,
				null,
				sortOrder);

		//---register to watch a content URI for changes---
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 *
	 * I'm tempted to leave this out.  I think that the general approach for 'update'
	 * in this case is to do:
	 * beginTransaction()
	 * delete(set)
	 * insert()
	 * insert()
	 * insert()
	 * .
	 * .
	 * .
	 * commitTransaction()
	 *
	 * Of course we could also code in this routine to accept a list of some kind and
	 * do exactly that...
	 *
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = 0;

		switch (uriMatcher.match(uri)){
		case ALLSETS:
			count = AcalDB.update(
					DATABASE_TABLE,
					values,
					selection,
					selectionArgs);
			break;
		case ROW_ID_SET:
			count = AcalDB.update(
					DATABASE_TABLE,
					values,
					_ID + " = " + uri.getPathSegments().get(0) +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		case SERVER_ID_SET:
			count = AcalDB.update(
					DATABASE_TABLE,
					values,
					SERVER_ID + " = " + uri.getPathSegments().get(1) +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		case SERVER_ID_TYPE_SET:
			count = AcalDB.update(
					DATABASE_TABLE,
					values,
					SERVER_ID + " = " + uri.getPathSegments().get(1) + " AND " +
					SET_TYPE + " = " + uri.getPathSegments().get(3) +
					(!TextUtils.isEmpty(selection) ? " AND (" +
							selection + ')' : ""),
							selectionArgs);
			break;
		case BEGIN_TRANSACTION:	//Return 1 for success or 0 for failure
				//We are beginning a new transaction only (at this time we wont allow nested tx's)
				if (AcalDB.inTransaction()) return 0;
				AcalDB.beginTransaction();
				return 1;

		case END_TRANSACTION:	//Return 1 for success or 0 for failure
			//We are ending an existing transaction only
			if (!AcalDB.inTransaction()) return 0;
			AcalDB.endTransaction();
			return 1;
		case APPROVE_TRANSACTION:	//Return 1 for success or 0 for failure
			//We are ending an existing transaction only
			if (!AcalDB.inTransaction()) return 0;
			AcalDB.setTransactionSuccessful();
			return 1;
		default: throw new IllegalArgumentException(
				"Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
