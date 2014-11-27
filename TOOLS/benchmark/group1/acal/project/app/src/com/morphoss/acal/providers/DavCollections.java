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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.AcalDBHelper;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;

/**
 * <p>This ContentProvider interfaces with the dav_collection table in the database.</p>
 * 
 * <p>
 * This class accepts URI specifiers for it's operation in the following forms:
 * </p>
 * <ul>
 * <li>content://collections</li>
 * <li>content://collections/# - A particular collection.</li>
 * <li>content://collections/server/# - All collections for a particular server.</li>
* </ul>
 * 
 * @author Morphoss Ltd
 *
 */
public class DavCollections extends ContentProvider {

	public static final String TAG = "aCal CollectionsProvider";
	
	//Authority must match one defined in manifest!
	public static final String AUTHORITY = "collections";
    public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY);
    
    //Database + Table
    private SQLiteDatabase AcalDB;
    public static final String DATABASE_TABLE = "dav_collection";

    // An ID to indicate no ID assigned yet.
	public static final int ID_NOT_ASSIGNED = -1;
    
    //Path definitions
    private static final int ROOT = 0;
    private static final int ALL_COLLECTIONS = 1;
    private static final int BY_COLLECTION_ID = 2;   
    private static final int BY_SERVER_ID = 3;
    private static final int BY_PATH_AND_SERVER_ID = 4;   
       
    //Creates Paths and assigns Path Definition Id's
    public static final UriMatcher uriMatcher = new UriMatcher(ROOT);
    static{
         uriMatcher.addURI(AUTHORITY, null, ALL_COLLECTIONS);
         uriMatcher.addURI(AUTHORITY, "#", BY_COLLECTION_ID);
         uriMatcher.addURI(AUTHORITY, "/path/*/server/#", BY_PATH_AND_SERVER_ID);
         uriMatcher.addURI(AUTHORITY, "/server/#", BY_SERVER_ID);
    }

	//Table Fields - All other classes should use these constants to access fields.
    /**
     *
CREATE TABLE dav_collection (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  server_id INTEGER REFERENCES dav_server(_id),
  collection_path TEXT,
  displayname TEXT,
  holds_events BOOLEAN,
  holds_tasks BOOLEAN,
  holds_journal BOOLEAN,
  holds_addressbook BOOLEAN,
  active_events BOOLEAN,
  active_tasks BOOLEAN,
  active_journal BOOLEAN,
  active_addressbook BOOLEAN,
  last_synchronised DATETIME,
  sync_token TEXT,
  collection_tag TEXT,
  default_timezone TEXT,
  colour TEXT,
  use alarms BOOLEAN,
  max_sync_age_wifi INTEGER,
  max_sync_age_3g INTEGER,
  is_writable BOOLEAN,
  is_visible BOOLEAN,
  needs_sync BOOLEAN,
  sync_metadata BOOLEAN,
  UNIQUE(server_id,collection_path)
);
     */
	public static final String _ID = "_id";
	public static final String SERVER_ID="server_id";
	public static final String COLLECTION_PATH="collection_path";
	public static final String DISPLAYNAME="displayname";
	public static final String HOLDS_EVENTS="holds_events";
	public static final String HOLDS_TASKS="holds_tasks";
	public static final String HOLDS_JOURNAL="holds_journal";
	public static final String HOLDS_ADDRESSBOOK="holds_addressbook";
	public static final String ACTIVE_EVENTS="active_events";
	public static final String ACTIVE_TASKS="active_tasks";
	public static final String ACTIVE_JOURNAL="active_journal";
	public static final String ACTIVE_ADDRESSBOOK="active_addressbook";
	public static final String LAST_SYNCHRONISED="last_synchronised";
	public static final String NEEDS_SYNC="needs_sync";
	public static final String SYNC_TOKEN="sync_token";
	public static final String COLLECTION_TAG="collection_tag";
	public static final String DEFAULT_TIMEZONE="default_timezone";
	public static final String COLOUR="colour";
	public static final String USE_ALARMS="use_alarms";
	public static final String MAX_SYNC_AGE_WIFI="max_sync_age_wifi"; 
	public static final String MAX_SYNC_AGE_3G="max_sync_age_3g";
	public static final String IS_WRITABLE="is_writable";
	public static final String IS_VISIBLE="is_visible";
	public static final String SYNC_METADATA="sync_metadata";

	/*
	 * 	(non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count=0;
		String server_id;
		String path;
		switch (uriMatcher.match(uri)){
		case ALL_COLLECTIONS:
			count = AcalDB.delete(
					DATABASE_TABLE,
					selection, 
					selectionArgs);
			break;
		case BY_COLLECTION_ID:
			String id = uri.getPathSegments().get(0);
			count = AcalDB.delete(
					DATABASE_TABLE,                        
					_ID + " = " + id + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
							selection + ')' : ""), 
							selectionArgs);
			break;
		case BY_SERVER_ID:
			server_id = uri.getPathSegments().get(1);
			count = AcalDB.delete(
					DATABASE_TABLE,                        
					SERVER_ID + " = " + server_id + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
							selection + ')' : ""), 
							selectionArgs);
			break;
		case BY_PATH_AND_SERVER_ID:
			server_id = uri.getPathSegments().get(1);
			path = uri.getPathSegments().get(3);
			count = AcalDB.delete(
					DATABASE_TABLE,                        
					SERVER_ID + " = " + server_id + " AND " + COLLECTION_PATH + " = " + path + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
							selection + ')' : ""), 
							selectionArgs);
			break;
		default: throw new IllegalArgumentException(
				"Unknown URI " + uri);    
		}
		
		/**
		 * Delete rows related to any collections that don't exist now.
		 */
		String[] riTables = {
				ResourceManager.ResourceTableManager.PENDING_DATABASE_TABLE,
				ResourceManager.ResourceTableManager.RESOURCE_DATABASE_TABLE,
				CacheManager.CacheTableManager.TABLE,
		};
		for( String table : riTables ) {
			AcalDB.delete(table, "NOT EXISTS(SELECT 1 FROM "+DATABASE_TABLE+" WHERE "+_ID+"="+table+".collection_id)", null);
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
		case ALL_COLLECTIONS:
		case BY_SERVER_ID:
			return "vnd.android.cursor.dir/vnd.morphoss.collection";
		case BY_COLLECTION_ID:
		case BY_PATH_AND_SERVER_ID:
			return "vnd.android.cursor.item/vnd.morphoss.collection";
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
		long rowID = -1;
		try {
		rowID = AcalDB.insertOrThrow(
				DATABASE_TABLE, "", values);
		} catch (Exception e) {
			Log.e(TAG,"Error inserting value to DB: "+e.getMessage());
		}
		//---if added successfully---
		if (rowID>0)
		{
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

		if (uriMatcher.match(uri) == BY_COLLECTION_ID)
			//---if getting a particular server---
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(0));                
		else if (uriMatcher.match(uri) == BY_SERVER_ID)
			//---if getting a particular server---
			sqlBuilder.appendWhere(SERVER_ID + " = " + uri.getPathSegments().get(1));
		else if (uriMatcher.match(uri) == BY_PATH_AND_SERVER_ID)
			//---if getting a particular record by server_id AND collection_path---
			sqlBuilder.appendWhere(SERVER_ID + " = " + uri.getPathSegments().get(1) + " AND "+
								   COLLECTION_PATH + " = " + uri.getPathSegments().get(3));

		if (sortOrder==null || sortOrder.equals("") )
			sortOrder = _ID;

		Cursor c = sqlBuilder.query(
				AcalDB, 
				projection, 
				selection, 
				selectionArgs, 
				null, 
				null, 
				sortOrder);

		//---register to watch a content URI for changes---
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count = 0;
	
		switch (uriMatcher.match(uri)){
		case ALL_COLLECTIONS:
			count = AcalDB.update(
					DATABASE_TABLE, 
					values,
					selection, 
					selectionArgs);
			break;
		case BY_COLLECTION_ID:                
			count = AcalDB.update(
					DATABASE_TABLE, 
					values,
					_ID + " = " + uri.getPathSegments().get(0) + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
							selection + ')' : ""), 
							selectionArgs);
			break;
		case BY_SERVER_ID:                
			count = AcalDB.update(
					DATABASE_TABLE, 
					values,
					SERVER_ID + " = " + uri.getPathSegments().get(1) + 
					(!TextUtils.isEmpty(selection) ? " AND (" + 
							selection + ')' : ""), 
							selectionArgs);
			break;
		case BY_PATH_AND_SERVER_ID:                
			count = AcalDB.update(
					DATABASE_TABLE, 
					values,
					SERVER_ID + " = " + uri.getPathSegments().get(1) + 
					COLLECTION_PATH + " = " + uri.getPathSegments().get(3) +
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

	
	/**
	 * <p>
	 * Utility method for retrieving a row by ID as a ContentValues object.
	 * </p>
	 * @param collectionId
	 * @param contentResolver
	 * @return A ContentValues object which is the database row, or null if not found. 
	 */
	public static ContentValues getRow(long collectionId, ContentResolver contentResolver) {
		ContentValues collectionData = null;
		Cursor c = null;
		try {
			c = contentResolver.query(Uri.withAppendedPath(CONTENT_URI,Long.toString(collectionId)),
						null, null, null, null);
			if ( !c.moveToFirst() ) {
				Log.e(TAG, "No dav_collection row in DB for " + Long.toString(collectionId));
				c.close();
				return null;
			}
			collectionData = new ContentValues();
			DatabaseUtils.cursorRowToContentValues(c,collectionData);
		}
		catch (Exception e) {
			// Error getting data
			Log.e(TAG, "Error getting collection data from DB: " + e.getMessage());
			Log.e(TAG, Log.getStackTraceString(e));
			c.close();
			return null;
		}
		finally {
			c.close();
		}
		return collectionData;
	}	


	public final static short INCLUDE_ALL_COLLECTIONS = 0x00;
	public final static short INCLUDE_EVENTS = 0x01;
	public final static short INCLUDE_TASKS = 0x02;
	public final static short INCLUDE_JOURNAL = 0x04;
	public final static short INCLUDE_ADDRESSBOOK = 0x08;
	public final static short INCLUDE_ALL_ACTIVE = 0x0f;
	public static ContentValues[] getCollections(ContentResolver cr, short includeFor ) {
		StringBuilder includeSelection = new StringBuilder();
		if ( includeFor != INCLUDE_ALL_COLLECTIONS ) {
			includeSelection.append("(");
			if ( (includeFor & INCLUDE_EVENTS) > 0 )
				includeSelection.append(DavCollections.ACTIVE_EVENTS+"=1 ");
			if ( (includeFor & INCLUDE_TASKS) > 0 ) {
				if ( includeSelection.length() > 1 ) includeSelection.append(" OR ");
				includeSelection.append(DavCollections.ACTIVE_TASKS+"=1 ");
			}
			if ( (includeFor & INCLUDE_JOURNAL) > 0 ) {
				if ( includeSelection.length() > 1 ) includeSelection.append(" OR ");
				includeSelection.append(DavCollections.ACTIVE_JOURNAL+"=1 ");
			}
			if ( (includeFor & INCLUDE_ADDRESSBOOK) > 0 ) {
				if ( includeSelection.length() > 1 ) includeSelection.append(" OR ");
				includeSelection.append(DavCollections.ACTIVE_ADDRESSBOOK+"=1");
			}
			includeSelection.append(") AND ");
		}
		includeSelection.append(" EXISTS (SELECT 1 FROM "+Servers.DATABASE_TABLE
					+" WHERE "+DavCollections.SERVER_ID+"="+Servers._ID+")");

		Cursor cursor = cr.query( DavCollections.CONTENT_URI, null, includeSelection.toString(),
										null, DavCollections._ID );

		cursor.moveToFirst();
		ContentValues[] ret = new ContentValues[cursor.getCount()];
		while ( !cursor.isAfterLast() ) {
			ContentValues toAdd = new ContentValues();
			DatabaseUtils.cursorRowToContentValues(cursor, toAdd);
			ret[cursor.getPosition()] = toAdd;
			cursor.moveToNext();
		}
		cursor.close();
		return ret;
	}
	
	
	/**
	 * Disable a collection
	 */
	public static final boolean collectionEnabled(boolean enabled, int id, ContentResolver cr) {
		if (Constants.LOG_DEBUG) Log.d(TAG,"Request to set collection id "+id+" active to "+enabled);
		Uri uri = ContentUris.withAppendedId(CONTENT_URI, id);
		//get original contentvalues
		Cursor mCursor = cr.query(uri, null, _ID+" = ?", new String[]{id+""}, null);
		if (mCursor.getCount() != 1) return false;
		mCursor.moveToFirst();
		ContentValues toSync = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(mCursor, toSync);
		mCursor.close();
		
		toSync.put(ACTIVE_ADDRESSBOOK, (enabled ? 1 : 0));
		toSync.put(ACTIVE_EVENTS, (enabled ? 1 : 0));
		toSync.put(ACTIVE_TASKS, (enabled ? 1 : 0));
		toSync.put(ACTIVE_JOURNAL, (enabled ? 1 : 0));
		int res = cr.update(uri,toSync, null, null);
		if (res < 1 && Constants.LOG_DEBUG) Log.d(TAG, "Update failed!");
		else if (res > 1) Log.e(TAG,"collectionEnabled() updates more than one row!!! "+res+" rows affected.");
		else if (Constants.LOG_DEBUG) Log.d(TAG,"Collection active status successfully changed.");
		return (res > 0);
	}
}
