package com.hectorone.multismssender;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

/**
 * 
 * This class is not very usefull... It have been created so we can set Uri for deliveryEntry in the database.
 * Thanks to this, sending a SMS (function sendMultipartTextMessage in MultiSmsSender) with a PendingIntent for delivery reports will use different Intent (Intent i = new Intent(message, uri))
 * so PendingIntent.getBroadcast() will return different PendingIntent
 * 
 * @author mathieu
 *
 */
public class EntryContentProvider extends ContentProvider{
	public static final String PROVIDER_NAME="com.hectorone.multismssender";
	public static final Uri CONTENT_URI = 
        Uri.parse("content://"+PROVIDER_NAME+ "/entries");
	

	
    private static final int ENTRIES = 1;
    private static final int ENTRY_ID = 2;  
    
    private static final UriMatcher uriMatcher;
    static{
       uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
       uriMatcher.addURI(PROVIDER_NAME, "entries", ENTRIES);
       uriMatcher.addURI(PROVIDER_NAME, "entries/#", ENTRY_ID);      
    }
	

    
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
	      switch (uriMatcher.match(uri)){
	         case ENTRIES:
	            return "com.hectorone.multismssender.dir/entry ";
	         case ENTRY_ID:                
	            return "com.hectorone.multismssender.item/entry ";
	         default:
	            throw new IllegalArgumentException("Unsupported URI: " + uri);        
	      }  
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}
	

}
