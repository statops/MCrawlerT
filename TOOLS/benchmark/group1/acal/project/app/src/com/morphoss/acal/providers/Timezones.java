/*
 * Copyright (C) 2012 Morphoss Ltd
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.AcalDBHelper;

/**
 * <p>This ContentProvider interfaces with the timezone, timezone_name and timezone_alias tables in the database.</p>
 *
 * <p>
 * This class accepts URI specifiers for it's operation in the following forms:
 * </p>
 * <ul>
 * <li>content://timezones</li>
 * <li>content://timezones/# - The path_set of a specific row</li>
 * <li>content://timezones/tzid/# - Retrieve a timezone for a specific TZID</li>
 * </ul>
 *
 * @author Morphoss Ltd
 *
 */
public class Timezones extends ContentProvider {

	public static final String TAG = "aCal Timezones ContentProvider";

	//Authority must match one defined in manifest!
	public static final String AUTHORITY = "timezones";
    public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY);

    //Database + Table
    private SQLiteDatabase AcalDB;
    private static final String TIMEZONE_TABLE = "timezone";
    private static final String TZ_ALIAS_TABLE = "timezone_alias";
    private static final String TZ_NAME_TABLE = "timezone_name";

    //Path definitions
    private static final int ROOT = 0;
    private static final int ALLSETS = 1;
    private static final int ROW_ID_SET = 2;
    private static final int TZID_SET = 3;

    //Creates Paths and assigns Path Definition Id's
    public static final UriMatcher uriMatcher = new UriMatcher(ROOT);
    static{
         uriMatcher.addURI(AUTHORITY, null, ALLSETS);
         uriMatcher.addURI(AUTHORITY, "#", ROW_ID_SET);
         uriMatcher.addURI(AUTHORITY, "tzid/#", TZID_SET);
    }

    // Type definitions
    public static final int UNKNOWN = 0;
    public static final int PRINCIPAL_COLLECTION_SET = 1;
    public static final int CALENDAR_HOME_SET = 2;
    public static final int ADDRESSBOOK_HOME_SET = 3;

	//Table Fields - All other classes should use these constants to access fields.
    public static final String _ID = "_id";
	public static final String TZID = "tzid";
	public static final String DEFAULT_NAME="default_name";
	public static final String LAST_MODIFIED="last_modified";
	public static final String ZONE_DATA="ZONE_DATA";

	private static final String TZ_NAME="tzname";
	private static final String TZ_NAME_LOCALE="locale";
	public static final String TZ_NAMES="names";

	private static final String TZID_ALIAS="alias";
	public static final String TZID_ALIASES="aliases";
	
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
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		switch (uriMatcher.match(uri)) {
		//Get all Timezones
		case ALLSETS:
		case ROW_ID_SET:
		case TZID_SET:
			return "vnd.android.cursor.dir/vnd.morphoss.tzuri_set";
		// We only deal with whole sets for this class
		//	return "vnd.android.cursor.item/vnd.morphoss.path_set";
		default:
			throw new IllegalArgumentException("Unsupported URI: "+uri);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(TIMEZONE_TABLE);
		
		String groupBy = null;

		if (uriMatcher.match(uri) == ROW_ID_SET)
			//---if getting a particular server---
			sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(0));
		else if (uriMatcher.match(uri) == TZID_SET) {
			//---if getting a particular server---
			sqlBuilder.appendWhere( TZID + " = '" + uri.getPathSegments().get(1)+"'"+
					" OR EXISTS(SELECT 1 FROM "+TZ_ALIAS_TABLE+" WHERE "+TZID+"='"+uri.getPathSegments().get(1)+"')"
					);
		}

		Cursor c = sqlBuilder.query( AcalDB, projection, selection, selectionArgs, groupBy, null, sortOrder);

		//---register to watch a content URI for changes---
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	
	/*
	 * 	(non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count=0;
		switch (uriMatcher.match(uri)){
		case ALLSETS:
			count = AcalDB.delete( TIMEZONE_TABLE, selection, selectionArgs);
			break;
		case ROW_ID_SET:
			String row_id = uri.getPathSegments().get(0);
			count = AcalDB.delete( TIMEZONE_TABLE,
						_ID + " = " + row_id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
						selectionArgs);
			break;
		case TZID_SET:
			String tzid = uri.getPathSegments().get(1);
			AcalDB.beginTransaction();
			try {
				count = AcalDB.delete( TIMEZONE_TABLE,
							TZID + " = " + tzid + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
							selectionArgs);
				// The ... AND NOT EXISTS ... stuff is needed in case the selection restricted the delete from actually happening
				AcalDB.delete( TZ_ALIAS_TABLE, TZID + " =? AND NOT EXISTS( SELECT 1 FROM "+TIMEZONE_TABLE+" WHERE "+TZID+"=?)",
						new String[] { tzid, tzid } );
				AcalDB.delete( TZ_NAME_TABLE, TZID + " =? AND NOT EXISTS( SELECT 1 FROM "+TIMEZONE_TABLE+" WHERE "+TZID+"=?)",
						new String[] { tzid, tzid } );
				AcalDB.setTransactionSuccessful();
			}
			catch( SQLException e ) {
				Log.println(Constants.LOGW, TAG, Log.getStackTraceString(e));
			}
			finally {
				AcalDB.endTransaction();
			}
			break;
		default: throw new IllegalArgumentException(
				"Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private String[] buildAliasList(String aliasesString) {
		String[] aliases = new String[] {};
		try { 
			aliases = aliasesString.split("\n");
		} catch( Exception e ) {}
		return aliases;
	}

	private Map<String, String> buildNamesMap(String namesString) {
		Map<String, String> names = new HashMap<String,String>();
		try {
			for ( String localeName : namesString.split("\n") ) {
				String[] keyValuePair = localeName.split("~");
				names.put(keyValuePair[0], keyValuePair[1]);
			}
		}
		catch ( Exception e ) { }
		return names;
	}

	
	/*
	 * (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String tzid = values.getAsString(TZID);
		String[] aliases = buildAliasList( (String) values.getAsString(TZID_ALIASES)); 
		values.remove(TZID_ALIASES);

		Map<String,String> names = buildNamesMap( (String) values.get(TZ_NAMES) ); 
		values.remove(TZ_NAMES);

		ContentValues aliasValues = new ContentValues();
		aliasValues.put(TZID, tzid);
		ContentValues nameValues = new ContentValues();
		nameValues.put(TZID, tzid);
		boolean success = false;
		long rowID = -1;
		AcalDB.beginTransaction();
		try {
			rowID = AcalDB.insert( TIMEZONE_TABLE, null, values);
			HashSet<String> existing = getAliasesFor(tzid);
			for( String alias : aliases ) {
				if ( alias.equals("") ) continue;
				if ( existing.contains(alias) ) {
					existing.remove(alias);
					continue;
				}
				aliasValues.put(TZID_ALIAS,alias);
				try {
					AcalDB.insert(TZ_ALIAS_TABLE, null, aliasValues);
				}
				catch( Exception sqe ) {
					Log.println(Constants.LOGW, TAG, "Unable to insert alias '"+alias+"' for '"+values.getAsString(TZID));
				}
			}
			for( String alias : existing ) {  // It seems these no longer apply
				AcalDB.delete( TZ_ALIAS_TABLE, TZID+"=? AND "+TZID_ALIAS+"=?", new String[] {tzid, alias} );
			}

			existing = getNamesFor(tzid);
			for( Entry<String,String> e : names.entrySet() ) {
				String locale = e.getKey();
				if ( locale == null || locale.equals("") ) continue;
				nameValues.put(TZ_NAME_LOCALE, locale);
				nameValues.put(TZ_NAME, e.getValue());
				try {
					if ( existing.contains(locale) ) {
						AcalDB.update(TZ_NAME_TABLE, nameValues, TZID+"=? AND " + TZ_NAME_LOCALE+"=?", new String[] { tzid, locale } );
					}
					else {
						AcalDB.insert(TZ_NAME_TABLE, null, nameValues);
					}
				}
				catch( Exception sqe ) {
					Log.println(Constants.LOGW, TAG, "Unable to insert name '"+e.getValue()+"'for locale '"+locale+"' for TZID '"+values.getAsString(TZID));
				}
			}
			AcalDB.setTransactionSuccessful();
			success = true;
		}
		catch( SQLException e ) {
			Log.println(Constants.LOGW, TAG, Log.getStackTraceString(e));
		}
		finally {
			AcalDB.endTransaction();
		}

		//---if added successfully---
		if (success) {
			Log.println(Constants.LOGI, TAG, "Added timezone details for '"+tzid+"' with aliases "+aliases.toString());
			//TODO rowid does NOT translate correctly here!!!
			Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}
		throw new SQLException("Failed to insert row into " + uri);
	}


	/**
	 * 
	 * @param tzid
	 * @return
	 */
	private HashSet<String> getAliasesFor(String tzid) {
		Cursor c = AcalDB.query(TZ_ALIAS_TABLE, new String[] { TZID_ALIAS }, TZID+"=?", new String[] { tzid }, null, null, null);
		HashSet<String> existingAliases = new HashSet<String>(c.getCount());
		try {
			for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				existingAliases.add(c.getString(0));
			}
		}
		finally {
			c.close();
		}
		return existingAliases;
	}

	/**
	 * 
	 * @param tzid
	 * @return
	 */
	private HashSet<String> getNamesFor(String tzid) {
		Cursor c = AcalDB.query(TZ_NAME_TABLE, new String[] { TZ_NAME_LOCALE }, TZID+"=?", new String[] { tzid }, null, null, null);
		HashSet<String> existingNames = new HashSet<String>(c.getCount());
		try {
			for( c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				existingNames.add(c.getString(0));
			}
		}
		finally {
			c.close();
		}
		return existingNames;
	}

	private void updateAliasSet(String tzid, String[] aliases, ContentValues aliasValues ) {
		Set<String> deleteAliases = new HashSet<String>();
		Cursor c = AcalDB.queryWithFactory(null, false, TZ_ALIAS_TABLE, new String[] { TZID_ALIAS }, TZID+"='"+tzid+"'",
				null, null, null, null, null);
		for( c.moveToFirst(); c.isAfterLast(); c.moveToNext() )
			deleteAliases.add(c.getString(0));

		for( Object alias : aliases ) {
			if ( alias instanceof String ) {
				if ( deleteAliases.contains((String) alias) ) {
					deleteAliases.remove((String) alias);
				}
				else {
					aliasValues.put(TZID_ALIAS, (String) alias);
					AcalDB.insert(TZ_ALIAS_TABLE, null, aliasValues);
				}
			}
		}
		StringBuilder deleteList = new StringBuilder();
		for( String alias : deleteAliases ) {
			if ( deleteList.length() != 0 ) deleteList.append(',');
			deleteList.append(alias);
		}
		AcalDB.delete(TZ_ALIAS_TABLE, TZID+"='"+tzid+"' AND "+TZID_ALIAS+" IN ("+deleteList+")", null);
	}


	private void updateNameSet(String tzid, Map<?,?> names, ContentValues nameValues ) {
		Set<String> deleteLocales = new HashSet<String>();
		Cursor c = AcalDB.queryWithFactory(null, false, TZ_NAME_TABLE, new String[] { TZ_NAME_LOCALE }, TZID+"='"+tzid+"'",
				null, null, null, null, null);
		for( c.moveToFirst(); c.isAfterLast(); c.moveToNext() )
			deleteLocales.add(c.getString(0));

		for( Entry<?,?> alias : names.entrySet() ) {
			String key = (String) alias.getKey();
			nameValues.put(TZ_NAME_LOCALE, key);
			nameValues.put(TZ_NAME, (String) alias.getValue());
			if ( deleteLocales.contains(key) ) {
				deleteLocales.remove(key);
				AcalDB.update(TZ_NAME_TABLE, nameValues, TZID+"='"+tzid+"' AND "+TZ_NAME_LOCALE+"='"+key+"'", null);
			}
			else {
				AcalDB.insert(TZ_NAME_TABLE, null, nameValues);
			}
		}
		StringBuilder deleteList = new StringBuilder();
		for( String alias : deleteLocales ) {
			if ( deleteList.length() != 0 ) deleteList.append(',');
			deleteList.append(alias);
		}
		AcalDB.delete(TZ_ALIAS_TABLE, TZID+"='"+tzid+"' AND "+TZID_ALIAS+" IN ("+deleteList+")", null);
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

		String[] aliases = buildAliasList( (String) values.getAsString(TZID_ALIASES)); 
		values.remove(TZID_ALIASES);

		Map<String,String> names = buildNamesMap( (String) values.get(TZ_NAMES) ); 
		values.remove(TZ_NAMES);

		AcalDB.beginTransaction();
		try {
			
			switch ( uriMatcher.match(uri) ) {
				case ALLSETS:
					count = AcalDB.update(TIMEZONE_TABLE, values, selection, selectionArgs);
					break;
				case ROW_ID_SET:
					count = AcalDB.update(TIMEZONE_TABLE, values,
							_ID + " = " + uri.getPathSegments().get(0)
									+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
					break;
				case TZID_SET:
					count = AcalDB.update(TIMEZONE_TABLE, values,
							TZID + " = " + uri.getPathSegments().get(1)
									+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
					break;
				default:
					throw new IllegalArgumentException("Unknown URI " + uri);
			}

			if ( count == 1 ) {
				ContentValues aliasValues = new ContentValues();
				ContentValues nameValues = new ContentValues();
				String tzid;
				Cursor updatedRows = this.query(uri, new String[]{ TZID }, selection, selectionArgs, null);
				for( updatedRows.moveToFirst(); updatedRows.isAfterLast(); updatedRows.moveToNext() ) {
					tzid = updatedRows.getString(0);
					aliasValues.put(TZID, tzid);
					updateAliasSet(tzid, aliases, aliasValues);
					nameValues.put(TZID, tzid);
					updateNameSet(tzid, names, nameValues);
				}
			}
			else if ( aliases != null || names != null ) {
				throw new IllegalArgumentException("Update affects more than one row and aliases or localised names were supplied");
			}
			AcalDB.setTransactionSuccessful();
		}
		finally {
			AcalDB.endTransaction();
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
