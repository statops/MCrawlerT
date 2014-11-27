package com.morphoss.acal.dataservice;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.providers.DavCollections;

public class Collection {
	
	final private static String TAG = "aCal Collection";
	private ContentValues cv;
	private int collectionColour;
	public boolean alarmsEnabled;
	public final long collectionId;
	public final boolean useForEvents;
	public final boolean useForTasks;
	public final boolean useForJournal;
	public final boolean useForAddressbook;
	
	private static boolean haveAllCollections = false;
	private static final HashMap<Long,Collection> collections = new HashMap<Long,Collection>();
	
	public synchronized  static Collection getInstance(long id, Context context) {
		if (collections.containsKey(id)) 
			return collections.get(id);

		Collection instance = fromDatabase(id,context);
		collections.put(id, instance);
		return instance;
	}
	
	//Call this method if there are any changes to Collections table.
	public synchronized static void flush() {
		collections.clear();
		haveAllCollections = false;
	}

	private synchronized static void fetchAllCollections(Context context) {
		for( ContentValues row : DavCollections.getCollections( context.getContentResolver(), DavCollections.INCLUDE_ALL_COLLECTIONS ) ) {
			Collection c = new Collection(row);
			collections.put(c.collectionId, c);
		}
		haveAllCollections = true;
	}

	public static Map<Long,Collection> getAllCollections(Context context) {
		if ( !haveAllCollections ) fetchAllCollections(context);

		HashMap<Long,Collection> allCollections = new HashMap<Long,Collection>(collections.size());
		for( Collection c : collections.values() ) allCollections.put(c.collectionId, c);
		return allCollections;
	}
	
	public Collection( ContentValues collectionRow ) {
		cv = collectionRow;
		setColour(cv.getAsString(DavCollections.COLOUR));
		alarmsEnabled = StaticHelpers.toBoolean(cv.getAsInteger(DavCollections.USE_ALARMS), true);
		collectionId = cv.getAsLong(DavCollections._ID);
		useForEvents = StaticHelpers.toBoolean(cv.getAsInteger(DavCollections.ACTIVE_EVENTS), true);
		useForTasks = StaticHelpers.toBoolean(cv.getAsInteger(DavCollections.ACTIVE_TASKS), true);
		useForJournal = StaticHelpers.toBoolean(cv.getAsInteger(DavCollections.ACTIVE_JOURNAL), true);
		useForAddressbook = StaticHelpers.toBoolean(cv.getAsInteger(DavCollections.ACTIVE_ADDRESSBOOK), false);
		Log.println(Constants.LOGD, TAG, "Collection "+collectionId+" - "
					+ cv.getAsString(DavCollections.COLLECTION_PATH)
					+", alarmsEnabled:"+(alarmsEnabled?"yes":"no")+"-"+cv.getAsInteger(DavCollections.USE_ALARMS)
							);
	}

	private static Collection fromDatabase(long collectionId, Context context) {
		ContentValues collectionRow = DavCollections.getRow(collectionId, context.getContentResolver());
		if ( collectionRow == null ) return null;
		return new Collection(collectionRow);
	}
	

	public void updateCollectionRow( ContentValues collectionRow ) {
		if ( cv.getAsInteger(DavCollections._ID) != collectionId ) {
			Log.w(TAG,"Attempt to re-use AcalCollection with different Collection ID");
			try {
				throw new Exception("");
			}
			catch ( Exception e ) {
				Log.w(TAG,Log.getStackTraceString(e));
			}
			return;
		}
		cv.putAll(collectionRow);
		if (cv.containsKey(DavCollections.COLOUR)){
			setColour(cv.getAsString(DavCollections.COLOUR));
		}
		alarmsEnabled = StaticHelpers.toBoolean(cv.getAsInteger(DavCollections.USE_ALARMS), true);
	}

	public int getColour() {
		return collectionColour;
	}

	public int setColour( String colourString ) {
		if ( colourString == null ) colourString = StaticHelpers.randomColorString();
		try {
			collectionColour = Color.parseColor(colourString);
		} catch (IllegalArgumentException iae) {
			collectionColour = Color.parseColor("#00f");	//Default blue
		}
		return collectionColour;
	}

	public ContentValues getCollectionRow() {
		return cv;
	}

	public long getCollectionId() {
		return collectionId;
	}

	public String getDisplayName() {
		return cv.getAsString(DavCollections.DISPLAYNAME);
	}

	public boolean alarmsEnabled() {
		return this.alarmsEnabled;
	}
}
