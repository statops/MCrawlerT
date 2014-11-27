/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package com.morphoss.acal.database;

import java.io.File;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.providers.Servers;

/**
 * <p>
 * This class is responsible for maintaining, creating and upgrading our
 * database. MUST be used by any other class that needs to access the
 * database directly.
 * </p>
 * 
 * @author Morphoss Ltd
 *
 */
public class AcalDBHelper extends SQLiteOpenHelper {

	public static final String TAG = "AcalDBHelper";
	
	/**
	 * The name of the database, which will be stored in:
	 *    /data/data/com.morphoss.acal/databases/[DB_NAME].db
	 */
	public static final String DB_NAME = "acal";
	
	/**
	 * The version of this database. Used to determine if an upgrade is required.
	 */
	public static final int DB_VERSION = 20;
	

	
	/**
	 * <p>The dav_server Table as stated in the specification.</p>
	 */
	public static final String DAV_SERVER_TABLE_SQL = 
			"CREATE TABLE dav_server ("
				+"_id INTEGER PRIMARY KEY AUTOINCREMENT"
				+",friendly_name TEXT"
				+",supplied_user_url TEXT"
				+",supplied_path TEXT"
				+",use_ssl BOOLEAN"
				+",hostname TEXT"
				+",port INTEGER"
				+",principal_path TEXT"
				+",auth_type INTEGER"
				+",username TEXT"
				+",password TEXT"
				+",has_srv BOOLEAN"
				+",has_wellknown BOOLEAN"
				+",has_caldav BOOLEAN"
				+",has_multiget BOOLEAN"
				+",has_sync BOOLEAN"
				+",active BOOLEAN"
				+",last_checked DATETIME"
				+",use_advanced BOOLEAN"
				+",prepared_config TEXT"
				+",UNIQUE(use_ssl,hostname,port,principal_path,username)"
			+")";

	/**
	 * <p>The dav_path_set table holds the paths which are collections containing
	 * the collections we are <em>really</em> interested in.  We use this for
	 * holding the responses to calendar-home-set, addressbook-home-set and
	 * principal-collection-set properties retrieved from the server.</p>
	 */
	public static final String DAV_PATH_SET_TABLE_SQL =
			"CREATE TABLE dav_path_set ("
				+"_id INTEGER PRIMARY KEY AUTOINCREMENT"
				+",server_id INTEGER REFERENCES dav_server(_id)"
				+",set_type INT"
				+",path TEXT"
				+",collection_tag TEXT"
				+",last_checked DATETIME"
				+",needs_sync BOOLEAN"
				+",UNIQUE(server_id, set_type, path)"
			+");";

	/**
	 * <p>The dav_collection holds information about the collections which we 
	 * synchronise with the server.</p>
	 */
	public static final String DAV_COLLECTION_TABLE_SQL = 
			"CREATE TABLE dav_collection ("
				+"_id INTEGER PRIMARY KEY AUTOINCREMENT"
				+",server_id INTEGER REFERENCES dav_server(_id)"
				+",collection_path TEXT"
				+",displayname TEXT"
				+",holds_events BOOLEAN"
				+",holds_tasks BOOLEAN"
				+",holds_journal BOOLEAN"
				+",holds_addressbook BOOLEAN"
				+",active_events BOOLEAN"
				+",active_tasks BOOLEAN"
				+",active_journal BOOLEAN"
				+",active_addressbook BOOLEAN"
				+",last_synchronised DATETIME"
				+",needs_sync BOOLEAN"
				+",sync_token TEXT"
				+",collection_tag TEXT"
				+",default_timezone TEXT"
				+",colour TEXT"
				+",use_alarms BOOLEAN"
				+",max_sync_age_wifi INTEGER"
				+",max_sync_age_3g INTEGER"
				+",is_writable BOOLEAN"
				+",is_visible BOOLEAN"
				+",sync_metadata BOOLEAN"
				+",manually_added BOOLEAN"
				+",UNIQUE(server_id,collection_path)"
			+");";
	
	/**
	 * <p>The dav_resource stores the resources (vevents, vtodos, vjournals & vcards)</p> 
	 */
	public static final String DAV_RESOURCE_TABLE_SQL =
			"CREATE TABLE dav_resource ("
				+"_id INTEGER PRIMARY KEY AUTOINCREMENT"
			  	+",collection_id INTEGER REFERENCES dav_collection(_id)"
			  	+",name TEXT"
			  	+",etag TEXT"
			  	+",last_modified DATETIME"
			  	+",content_type TEXT"
			  	+",data BLOB"
			  	+",needs_sync BOOLEAN"
			  	+",earliest_start NUMERIC"
			  	+",latest_end NUMERIC"
			  	+",effective_type TEXT"
			  	+",UNIQUE(collection_id,name)"
			+");";
	
	
	/**
	 * <p>Some indexes</p> 
	 */
	public static final String EVENT_INDEX_SQL =
		"CREATE UNIQUE INDEX event_select_idx ON dav_resource ( effective_type, collection_id, latest_end, _id );";
	public static final String TODO_INDEX_SQL =
		"CREATE UNIQUE INDEX todo_select_idx ON dav_resource ( effective_type, collection_id, _id );";
	
	
	/**
	 * The pending_change, containing the fully constructed resource we want
	 * to PUT to the server when we can.
	 * 
	 * In the event of a local CREATE pending the 'old_data' blob will be NULL.
	 * In the event of a local DELETE pending the 'new_data' blob will be NULL.
	 * 
	 * The SHOULD only be one pending_change active for a resource, and multiple
	 * changes SHOULD be merged where that is possible (i.e. where the status
	 * does not indicate it has already been submitted to the server and we are
	 * merely waiting to see it back again... 
	 */
	public static final String PENDING_CHANGE_TABLE_SQL = 
			"CREATE TABLE pending_change ("
		        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
				+",collection_id INTEGER REFERENCES dav_collection(_id)"
				+",resource_id INTEGER REFERENCES dav_resource(_id)"
				+",old_data BLOB"
				+",new_data BLOB"
				+",uid TEXT"
				+",UNIQUE(collection_id,resource_id)"
			+");";

	
	/**
	 * A Table for storing data pertinent to the Show Upcoming Widget.
	 * Introduced into version 13.
	 */
	public static final String SHOW_UPCOMING_WIDGET_TABLE_SQL = 
		"CREATE TABLE show_upcoming_widget_data ("
	        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+",resource_id INTEGER REFERENCES dav_resource(_id)"
			+",etag TEXT"
			+",colour INTEGER"
			+",dtstart NUMERIC"
			+",dtend NUMERIC"
			+",summary TEXT"
		+");";

	
	/**
	 * Used for caching event data
	 */
	public static final String RESOURCE_CACHE_TABLE_SQL = 
		"CREATE TABLE event_cache ("
	        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+",resource_id INTEGER REFERENCES dav_resource(_id)"
			+",resource_type TEXT"
			+",recurrence_id TEXT"
			+",collection_id NUMERIC"
			+",summary TEXT"
			+",location TEXT"
			+",dtstart NUMERIC"
			+",dtend NUMERIC"
			+",completed BOOLEAN"
			+",dtstartfloat BOOLEAN"
			+",dtendfloat BOOLEAN"
			+",completedfloat BOOLEAN"
			+",flags INTEGER"
		+");";
	
	public static final String RESOURCE_CACHE_META_TABLE_SQL = 
		"CREATE TABLE event_cache_meta ("
	        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+",dtstart NUMERIC"
			+",dtend NUMERIC"
			+",count INTEGER"
			+",closed BOOLEAN"
		+");";

	private static final long now = System.currentTimeMillis();
	public static final String SET_RESOURCE_CACHE_DIRTY_SQL = 
			"INSERT INTO event_cache_meta (dtstart, dtend, count, closed) VALUES("+now+","+now+",0,0)";

	public static final String ALARM_TABLE_SQL = 
		"CREATE TABLE alarms ("
	        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+",ttf NUMERIC"
			+",rid NUMERIC"
			+",rrid TEXT"
			+",state NUMERIC" 
			+", blob TEXT"
		+");";
	
	public static final String ALARM_META_TABLE_SQL = 
		"CREATE TABLE alarm_meta ("
	        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
			+",closed BOOLEAN"
		+");";
	public static final String CLEAR_ALARM_META_TABLE_SQL = "DELETE FROM alarm_meta"; 
	public static final String SET_ALARM_TABLE_DIRTY_SQL = 
			"INSERT INTO alarm_meta (closed) VALUES(0)";

	public static final String TIMEZONE_TABLE_SQL = 
			"CREATE TABLE timezone ("
		        +"_id INTEGER PRIMARY KEY AUTOINCREMENT"
				+",tzid TEXT"
				+",default_name TEXT"
		        +",last_modified NUMERIC"
		        +",zone_data BLOB"
		        +", UNIQUE(tzid)"
			+");";

	public static final String TIMEZONE_NAME_TABLE_SQL = 
			"CREATE TABLE timezone_name ("
				+"_id INTEGER PRIMARY KEY AUTOINCREMENT"
				+",tzid TEXT REFERENCES timezone(tzid)"
				+",tzname TEXT"
				+",locale TEXT"
				+",UNIQUE(tzid,locale)"
			+");";
	
	public static final String TIMEZONE_ALIAS_TABLE_SQL = 
			"CREATE TABLE timezone_alias ("
				+"alias TEXT PRIMARY KEY "
				+",tzid TEXT REFERENCES timezone(tzid)"
			+");";
	
	private Context	context;

	/**
	 * Visible single argument constructor. Calls super with default values.
	 * 
	 * @param context The context in which this DB will be used.
	 * @author Morphoss Ltd
	 */
	public AcalDBHelper (Context context) {
		super (context, DB_NAME+".db", null, DB_VERSION);
		this.context = context;
	}
	
	/**
	 * <p>
	 * Called when database is first instantiated. Creates default schema.
	 * </p>
	 * 
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 * @author Morphoss Ltd
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create Database:
		db.execSQL(DAV_SERVER_TABLE_SQL);
		createMostTables(db,false);
	}

	/**
	 * <p>
	 * Executed if exiting DB version does not match the one defined by DB_VERSION.
	 * Currently drops all but the dav_server table and recreates them.  It will
	 * eventually do a rescan of the servers after that, re-discovering collections
	 * and rebuilding our local cache. 
	 * </p>
	 * 
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 * @author Morphoss Ltd
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		Log.i(TAG,"Attempting to upgrade database from "+oldVersion+" to "+newVersion);
		
		// We drop tables in the reverse order to avoid constraint issues

		try {
			if ( oldVersion == 9 ) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				Log.i(TAG,"Updating database to version " + oldVersion);
			}
	
			if ( oldVersion == 10 ) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL("ALTER TABLE dav_server ADD COLUMN use_advanced BOOLEAN");
				db.execSQL("ALTER TABLE dav_server ADD COLUMN prepared_config TEXT");
			}
			if ( oldVersion == 11 ) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL("ALTER TABLE dav_resource ADD COLUMN effective_type TEXT");
				db.execSQL("UPDATE dav_resource SET effective_type = 'VCARD' WHERE lower(data) LIKE 'begin:vcard';");
				db.execSQL("UPDATE dav_resource SET effective_type = 'VEVENT' WHERE lower(data) LIKE 'begin:vevent';");
				db.execSQL("UPDATE dav_resource SET effective_type = 'VJOURNAL' WHERE lower(data) LIKE 'begin:vjournal';");
				db.execSQL("UPDATE dav_resource SET effective_type = 'VTODO' WHERE lower(data) LIKE 'begin:vtodo';");
				db.execSQL(EVENT_INDEX_SQL);
				db.execSQL(TODO_INDEX_SQL);
			}
			if ( oldVersion == 12 ) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL("PRAGMA writable_schema = 1");
				db.execSQL("UPDATE SQLITE_MASTER SET SQL = '"+DAV_SERVER_TABLE_SQL+"' WHERE name = '"+Servers.DATABASE_TABLE+"'");
				db.execSQL("PRAGMA writable_schema = 0");
			}
	
			if ( oldVersion == 13 ) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL(SHOW_UPCOMING_WIDGET_TABLE_SQL);
			}
			
			if (oldVersion == 14) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL("DROP TABLE show_upcoming_widget_data");
				db.execSQL(SHOW_UPCOMING_WIDGET_TABLE_SQL);
			}
			
			if (oldVersion == 15) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL(RESOURCE_CACHE_TABLE_SQL);
				db.execSQL(RESOURCE_CACHE_META_TABLE_SQL);
				db.execSQL(SET_RESOURCE_CACHE_DIRTY_SQL);
			}
			
			if (oldVersion == 16) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL("DROP TABLE event_cache");
				db.execSQL(RESOURCE_CACHE_TABLE_SQL);
				db.execSQL("DELETE FROM event_cache_meta");
				db.execSQL(SET_RESOURCE_CACHE_DIRTY_SQL);
				db.execSQL("UPDATE dav_collection SET needs_sync=1, sync_token=NULL, collection_tag=NULL");
				db.execSQL("DELETE FROM dav_resource");
			}
			
			if (oldVersion == 17) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL("DROP TABLE pending_change");
				db.execSQL(PENDING_CHANGE_TABLE_SQL);
			}
			if (oldVersion == 18) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL(ALARM_TABLE_SQL);
				db.execSQL(ALARM_META_TABLE_SQL);
				db.execSQL(SET_ALARM_TABLE_DIRTY_SQL);
			}
			if (oldVersion == 19) {
				Log.i(TAG,"Updating database from version " + oldVersion);
				oldVersion++;
				db.execSQL(TIMEZONE_TABLE_SQL);
				db.execSQL(TIMEZONE_NAME_TABLE_SQL);
				db.execSQL(TIMEZONE_ALIAS_TABLE_SQL);
				db.execSQL("ALTER TABLE dav_collection ADD COLUMN manually_added BOOLEAN");
			}
			
		}
		catch( Exception e ) {
			Log.e(TAG,"Failed to upgrade database carefully.", e);
		}
		finally {
		}
		
		if ( oldVersion != newVersion ) {
			// Fallback to try and drop all tables, except the server table and
			// then recreate them.
			recoverDatabase(db,true);
		}
		else {
			Log.i(TAG,"Database now upgraded to version " + newVersion);
		}
		
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
	}


	public SQLiteDatabase getReadableDatabase() {
		try {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(context.getDatabasePath(DB_NAME+".db").toString(), null, 
					SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			if ( db.getVersion() == DB_VERSION ) return db;
			db.close();
		}
		catch( SQLiteException e) {
			Log.i(TAG,e.getMessage());
		}
		return getWritableDatabase();
	}

	private SQLiteDatabase openWritableDatabase( String dbPath ) {
		SQLiteDatabase db = SQLiteDatabase.openDatabase( dbPath, null, 
				SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		if ( db.getVersion() == DB_VERSION ) return db;
		db.beginTransaction();
		onUpgrade(db, db.getVersion(), DB_VERSION);
		db.setVersion(DB_VERSION);
		db.setTransactionSuccessful();
		db.endTransaction();
		return db;
	}
	
	public SQLiteDatabase getWritableDatabase() {
		String dbPath = context.getDatabasePath(DB_NAME+".db").toString();

		if ( new File(dbPath).exists() ) {
			int attempts = 0;
			while( attempts++ < 500 ) {
				try {
					return openWritableDatabase(dbPath);
				}
				catch( SQLiteException e) {
					Log.println(Constants.LOGD,TAG,"Unable to get writable database - retrying");
				}
				try { Thread.sleep(10); } catch (Exception e) {}
			}
	
			try {
				// Once more to catch the failure message...
				return openWritableDatabase(dbPath);
			}
			catch( SQLiteException e) {
				Log.i(TAG,e.getMessage());
			}
		}
		return super.getWritableDatabase(); 
	}

	
	public synchronized void close(SQLiteDatabase db) {
		try {
			db.close();
			int counter = 500;
			while (db.isOpen() && counter-- > 0) {
				try { Thread.sleep(50); } catch ( InterruptedException e ) { } 
			}
		}
		catch( SQLiteException e ) {
			Log.e(TAG,Log.getStackTraceString(e));
		}
		super.close();
	}

	
	public static void createMostTables(SQLiteDatabase db, boolean keepCollections) {
		Log.i(TAG,"Creating database tables for version " + DB_VERSION);
		try {
			if ( !keepCollections ) {
				db.execSQL(DAV_PATH_SET_TABLE_SQL);
				db.execSQL(DAV_COLLECTION_TABLE_SQL);
			}
			db.execSQL(DAV_RESOURCE_TABLE_SQL);
			db.execSQL(PENDING_CHANGE_TABLE_SQL);
			db.execSQL(EVENT_INDEX_SQL);
			db.execSQL(TODO_INDEX_SQL);
			
			db.execSQL(RESOURCE_CACHE_TABLE_SQL);
			db.execSQL(RESOURCE_CACHE_META_TABLE_SQL);
			db.execSQL(SET_RESOURCE_CACHE_DIRTY_SQL);
			
			db.execSQL(SHOW_UPCOMING_WIDGET_TABLE_SQL);
			
			db.execSQL(ALARM_TABLE_SQL);
			db.execSQL(ALARM_META_TABLE_SQL);
			db.execSQL(SET_ALARM_TABLE_DIRTY_SQL);

			db.execSQL(TIMEZONE_TABLE_SQL);
			db.execSQL(TIMEZONE_NAME_TABLE_SQL);
			db.execSQL(TIMEZONE_ALIAS_TABLE_SQL);
		}
		catch( Exception e ) {
			Log.e(TAG, "Database error creating database tables", e);
		}
	}

	public static void recoverDatabase(SQLiteDatabase db, boolean keepCollections) {
		Log.i(TAG,"Recovering database to version " + DB_VERSION);
		try {
			// Drop all the tables except the dav_server one.
			try { db.execSQL("DROP TABLE timezone_alias"); } catch( Exception e ) {}
			try { db.execSQL("DROP TABLE timezone_name"); } catch( Exception e ) {}
			try { db.execSQL("DROP TABLE timezone"); } catch( Exception e ) {}
			
			try { db.execSQL("DROP TABLE alarm_meta"); } catch( Exception e ) {}
			try { db.execSQL("DROP TABLE alarms"); } catch( Exception e ) {}
			
			try { db.execSQL("DROP TABLE event_cache_meta"); } catch( Exception e ) {}
			try { db.execSQL("DROP TABLE event_cache"); } catch( Exception e ) {}
			
			try { db.execSQL("DROP TABLE show_upcoming_widget_data"); } catch( Exception e ) {}
			
			try { db.execSQL("DROP TABLE pending_change"); } catch( Exception e ) {}
			try { db.execSQL("DROP TABLE dav_resource"); } catch( Exception e ) {}
			if ( !keepCollections ) {
				try { db.execSQL("DROP TABLE dav_collection"); } catch( Exception e ) {}
				try { db.execSQL("DROP TABLE dav_path_set"); } catch( Exception e ) {}
			}
	
			// Recreate the tables we just dropped.
			createMostTables(db,keepCollections);
		}
		catch( Exception e ) {
			Log.e(TAG, "Database error recreating database", e);
		}
	}
}
