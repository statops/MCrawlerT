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

import java.util.ArrayList;

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

import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.database.AcalDBHelper;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRDeleteByCollectionId;
import com.morphoss.acal.dataservice.Collection;

/**
 * <P>This ContentProvider interfaces with the dav_server table in the database.</P>
 * 
 * @author Morphoss Ltd
 *
 */
public class Servers extends ContentProvider {

	public static final String TAG = "aCal ServersProvider";

	//Authority must match one defined in manifest!
	public static final String AUTHORITY = "servers";
    public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY);
    
    //Database + Table
    private SQLiteDatabase AcalDB;
    public static final String DATABASE_TABLE = "dav_server";
    
    //Path definitions
    private static final int ROOT = 0;
    private static final int SERVERS = 1;
    private static final int SERVER_ID = 2;   
       
    //Creates Paths and assigns Path Definition Id's
    public static final UriMatcher uriMatcher = new UriMatcher(ROOT);
    static{
         uriMatcher.addURI(AUTHORITY, null, SERVERS);
         uriMatcher.addURI(AUTHORITY, "#", SERVER_ID);
    }

	//Table Fields - All other classes should use these constants to access fields.
	public static final String		_ID					= "_id";
	public static final String		FRIENDLY_NAME		= "friendly_name";
	public static final String		LAST_CHECKED		= "last_checked";
	public static final String		SUPPLIED_USER_URL	= "supplied_user_url";
	public static final String		OLD_SUPPLIED_PATH	= "supplied_path";
	public static final String		OLD_SUPPLIED_DOMAIN	= "supplied_domain";
	public static final String		HOSTNAME			= "hostname";
	public static final String		PRINCIPAL_PATH		= "principal_path";
	public static final String		USERNAME			= "username";
	public static final String		PASSWORD			= "password";
	public static final String		PORT				= "port";
	public static final String		AUTH_TYPE			= "auth_type";
	public static final String		HAS_SRV				= "has_srv";
	public static final String		HAS_WELLKNOWN		= "has_wellknown";
	public static final String		HAS_CALDAV			= "has_caldav";
	public static final String		HAS_MULTIGET		= "has_multiget";
	public static final String		HAS_SYNC			= "has_sync";
	public static final String		ACTIVE				= "active";
	public static final String		USE_SSL				= "use_ssl";
	public static final String		USE_ADVANCED		= "use_advanced";
	public static final String		PREPARED_CONFIG		= "prepared_config";

	// Possible values for authentication
	public static final int			AUTH_NONE		= 0;
	public static final int			AUTH_BASIC		= 1;
	public static final int			AUTH_DIGEST		= 2;
	
	/*
	 * 	(non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count=0;
		switch (uriMatcher.match(uri)){
		case SERVERS:
			count = AcalDB.delete(
					DATABASE_TABLE,
					selection, 
					selectionArgs);
			break;
		case SERVER_ID:
			String id = uri.getPathSegments().get(0);
			count = AcalDB.delete( DATABASE_TABLE,
						_ID + " = " + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), 
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
		case SERVERS:
			return "vnd.android.cursor.dir/vnd.morphoss.servers";
		case SERVER_ID:
			return "vnd.android.cursor.item/vnd.morphoss.servers";
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

		if (uriMatcher.match(uri) == SERVER_ID)
			//---if getting a particular server---
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(0));                

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

		try {
			switch (uriMatcher.match(uri)) {
				case SERVERS:
					count = AcalDB.update(DATABASE_TABLE, values, selection, selectionArgs);
					break;
				case SERVER_ID:
					count = AcalDB.update(DATABASE_TABLE, values, _ID + " = " + uri.getPathSegments().get(0)
								+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
					break;
				default:
					throw new IllegalArgumentException("Unknown URI " + uri);
			}
		}
		catch (NullPointerException npe) {
			Log.e(TAG, Log.getStackTraceString(npe));
			return 0;
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}


	/**
	 * Delete the specified Server and all that sailed on her.
	 * 
	 * @param context
	 * @param serverId
	 */
	public static void deleteServer( Context context, int serverId ) {
		AcalDBHelper dbHelper = new AcalDBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		String[] params = new String[] { Integer.toString(serverId) };
		
		ArrayList<Long> collectionIds = null;
		Cursor c = db.query(DavCollections.DATABASE_TABLE, new String[]{DavCollections._ID},
				DavCollections.SERVER_ID+" = ? ", 
				params, null,null,null);
		
		if (c.getCount() > 0) {
			collectionIds = new ArrayList<Long>();
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
				collectionIds.add(c.getLong(0));
		}
		c.close();
		
		db.beginTransaction();
		try {
			db.delete(PathSets.DATABASE_TABLE, PathSets.SERVER_ID+"=?", params );
			db.delete(DavCollections.DATABASE_TABLE, DavCollections.SERVER_ID+"=?", params );
			db.delete(Servers.DATABASE_TABLE, Servers._ID+"=?", params );
			db.setTransactionSuccessful();
		}
		catch ( Exception e ){
			Log.w(AcalDBHelper.TAG,"Unexpected error deleting server "+serverId, e);
		}
		finally {
			db.endTransaction();
			db.close();
		}
		
		if ( collectionIds != null && !collectionIds.isEmpty() ) {
			//Ask resource manager to delete resources
			ResourceManager.getInstance(context).sendRequest(new RRDeleteByCollectionId(collectionIds));
		}
		
		Collection.flush();

	}

	/**
	 * Static method to clone <em>only</em> valid database fields into a new object.
	 * @param serverData
	 * @return
	 */
	public static ContentValues cloneValidColumns(ContentValues serverData) {
		ContentValues cloned = new ContentValues();
		StaticHelpers.copyContentValue(cloned, serverData, _ID);
		StaticHelpers.copyContentValue(cloned, serverData, FRIENDLY_NAME);
		StaticHelpers.copyContentValue(cloned, serverData, LAST_CHECKED);
		StaticHelpers.copyContentValue(cloned, serverData, SUPPLIED_USER_URL);
		StaticHelpers.copyContentValue(cloned, serverData, HOSTNAME);
		StaticHelpers.copyContentValue(cloned, serverData, PRINCIPAL_PATH);
		StaticHelpers.copyContentValue(cloned, serverData, USERNAME);
		StaticHelpers.copyContentValue(cloned, serverData, PASSWORD);
		StaticHelpers.copyContentValue(cloned, serverData, PORT);
		StaticHelpers.copyContentValue(cloned, serverData, AUTH_TYPE);
		StaticHelpers.copyContentValue(cloned, serverData, HAS_SRV);
		StaticHelpers.copyContentValue(cloned, serverData, HAS_WELLKNOWN);
		StaticHelpers.copyContentValue(cloned, serverData, HAS_CALDAV);
		StaticHelpers.copyContentValue(cloned, serverData, HAS_MULTIGET);
		StaticHelpers.copyContentValue(cloned, serverData, HAS_SYNC);
		StaticHelpers.copyContentValue(cloned, serverData, ACTIVE);
		StaticHelpers.copyContentValue(cloned, serverData, USE_SSL);
		StaticHelpers.copyContentValue(cloned, serverData, USE_ADVANCED);
		StaticHelpers.copyContentValue(cloned, serverData, PREPARED_CONFIG);
		return cloned;
	}

	/**
	 * Static method to retrieve a particular database row for a given serverId.
	 * @param serverId
	 * @param contentResolver
	 * @return A ContentValues which is the server row, or null
	 */
	public static ContentValues getRow(int serverId, ContentResolver contentResolver) {
		ContentValues serverData = null;
		Cursor c = null;
		try {
			c = contentResolver.query(Uri.withAppendedPath(CONTENT_URI,Long.toString(serverId)),
						null, null, null, null);
			if ( !c.moveToFirst() ) {
				Log.e(TAG, "No dav_server row in DB for " + Long.toString(serverId));
				c.close();
				return null;
			}
			serverData = new ContentValues();
			DatabaseUtils.cursorRowToContentValues(c,serverData);
		}
		catch (Exception e) {
			// Error getting data
			Log.e(TAG, "Error getting server data from DB: " + e.getMessage());
			Log.e(TAG, Log.getStackTraceString(e));
			c.close();
			return null;
		}
		finally {
			c.close();
		}
		return serverData;
	}

}
