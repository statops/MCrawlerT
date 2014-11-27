package com.hectorone.multismssender;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class DeliveryDbAdapter {


	public static final String KEY_DELIVERY_ENTRY_ROWID = "_id";
	public static final String KEY_DELIVERY_ENTRY_NAME = "name";
	public static final String KEY_DELIVERY_ENTRY_NUMBER = "number";
	public static final String KEY_DELIVERY_ENTRY_DELIVERED = "delivered";
	public static final String KEY_DELIVERY_ENTRY_DELIVERY_ID = "delivery_id";


	public static final String KEY_DELIVERY_ROWID = "_id";
	public static final String KEY_DELIVERY_NAME = "name";
	public static final String KEY_DELIVERY_DATE = "date";

	private static final String TAG = "deliveryDbAdapter";
	private DeliveryDbHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */

	public static final String DATABASE_NAME = "data";
	public static final String DATABASE_DELIVERY_ENTRY_TABLE = "delivery_entry";
	public static final String DATABASE_DELIVERY_TABLE = "delivery";
	public static final String DATABASE_DELIVERY_ENTRY_CREATE = "create table "
		+ DATABASE_DELIVERY_ENTRY_TABLE
		+ " ("+KEY_DELIVERY_ENTRY_ROWID+" integer primary key autoincrement, "
		+ KEY_DELIVERY_ENTRY_NAME + " text not null,"
		+ KEY_DELIVERY_ENTRY_NUMBER + " text not null,"
		+ KEY_DELIVERY_ENTRY_DELIVERED + " integer,"
		+ KEY_DELIVERY_ENTRY_DELIVERY_ID + " integer);";

	public static final String DATABASE_DELIVERY_CREATE = "create table "
		+ DATABASE_DELIVERY_TABLE
		+ " (" + KEY_DELIVERY_ROWID + " integer primary key autoincrement, "
		+ KEY_DELIVERY_NAME + " text not null,"
		+ KEY_DELIVERY_DATE + " text not null);";

	public static final int DATABASE_VERSION = 3;

	private final Context mCtx;

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
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_DELIVERY_TABLE);
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
	public DeliveryDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}


	/**
	 * Open the groups database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public DeliveryDbAdapter open() throws SQLException {
		mDbHelper = new DeliveryDbHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		
		return this;
	}

	public void close() {
		mDb.close();
		mDbHelper.close();
	}

	// ********************************* ENTRY FUNCTION ***********************

	/**
	 * Create a new entry using the name provided. If the entry is
	 * successfully created return the new rowId for that entry, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param name the name of the entry
	 * @param date the date of the entry
	 * @param deliveryID the deliveryID of the entry
	 */
	public long createEntry(String name, String number, long deliveryID) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_DELIVERY_ENTRY_NAME, name);
		initialValues.put(KEY_DELIVERY_ENTRY_NUMBER, number);
		initialValues.put(KEY_DELIVERY_ENTRY_DELIVERY_ID, deliveryID);
		initialValues.put(KEY_DELIVERY_ENTRY_DELIVERED, 0);
		
		return mDb.insert(DATABASE_DELIVERY_ENTRY_TABLE, null, initialValues);
	}

	/**
	 * Delete the entry with the given rowId
	 * 
	 * @param rowId id of entry to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteEntry(long rowId) {

		return mDb.delete(DATABASE_DELIVERY_ENTRY_TABLE, KEY_DELIVERY_ENTRY_ROWID + "=" + rowId, null) > 0 ;

	}

	/**
	 * Delete all the entries where the delivery_id is the given deliveryId
	 * 
	 * @param deliveryId id of the delivery
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteAllEntry(long deliveryId) {
		return mDb.delete(DATABASE_DELIVERY_ENTRY_TABLE, KEY_DELIVERY_ENTRY_DELIVERY_ID + "=" + deliveryId, null) > 0 ;
	}

	/**
	 * Return a Cursor over the list of all entry in the database associated with the given delivery_id
	 * 
	 * @param deliveryId id of the entry
	 * @return Cursor over all entries
	 */
	public Cursor fetchAllEntry(long deliveryId) {
		return mDb.query(DATABASE_DELIVERY_ENTRY_TABLE, new String[] {KEY_DELIVERY_ENTRY_ROWID, KEY_DELIVERY_ENTRY_NAME, KEY_DELIVERY_ENTRY_NUMBER, KEY_DELIVERY_ENTRY_DELIVERED}, KEY_DELIVERY_ENTRY_DELIVERY_ID + "=" + deliveryId, null, null, null , KEY_DELIVERY_ENTRY_NAME);
	}

	/**
	 * Return a Cursor over the list of all entry in the database associated with the given delivery_id
	 * 
	 * @param mDeliveryId id of the entry
	 * @return Cursor over all entries
	 */
	public Cursor fetchEntry(long entryId) {
		Cursor cursor =

			mDb.query(true, DATABASE_DELIVERY_ENTRY_TABLE, new String[] {KEY_DELIVERY_ENTRY_ROWID, KEY_DELIVERY_ENTRY_NAME, KEY_DELIVERY_ENTRY_NUMBER, KEY_DELIVERY_ENTRY_DELIVERED}, KEY_DELIVERY_ENTRY_DELIVERY_ID + "=" + entryId, null,
					null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;

	}
	
	public boolean setEntryDelivered(long entryId) {
		ContentValues content = new ContentValues();
		content.put(KEY_DELIVERY_ENTRY_DELIVERED, 1);
		return mDb.update(DATABASE_DELIVERY_ENTRY_TABLE, content, KEY_DELIVERY_ENTRY_ROWID +"="+entryId , null) > 0;
	}
	

	// ************************* DELIVERY *************************************



	/**
	 * Create a new delivery using the name provided. If the delivery is
	 * successfully created return the new rowId for that delivery, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param name the name of the delivery
	 * @param date the date of the delivery
	 */
	public long createDelivery(String name, String date) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_DELIVERY_NAME, name);
		initialValues.put(KEY_DELIVERY_DATE, date);

		return mDb.insert(DATABASE_DELIVERY_TABLE, null, initialValues);
	}

	/**
	 * Delete the delivery with the given rowId and all entry associated
	 * 
	 * @param rowId id of entry to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteDelivery(long rowId) {

		return mDb.delete(DATABASE_DELIVERY_TABLE, KEY_DELIVERY_ROWID + "=" + rowId, null) > 0 && deleteAllEntry(rowId) ;

	}
	
	public void deleteAllDeliveries() {
		mDb.delete(DATABASE_DELIVERY_TABLE, null, null);
		mDb.delete(DATABASE_DELIVERY_ENTRY_TABLE, null, null) ;
	}

	/**
	 * Return a Cursor over the list of all deliveries in the database
	 * 
	 * @param mDeliveryId id of the delivery
	 * @return Cursor over all delivery
	 */
	public Cursor fetchAllDeliveries() {
		return mDb.query(DATABASE_DELIVERY_TABLE, new String[] {KEY_DELIVERY_ROWID, KEY_DELIVERY_NAME, KEY_DELIVERY_DATE}, null, null, null, null , KEY_DELIVERY_DATE);
	}

	// *********************** HELPER ****************************************
	
    
    public String nameFromNumber(String number) {
    	 Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
    	 Cursor c = null;
    	 try {
    		 c = mCtx.getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME},null, null, null);
		} catch (Exception e) {
			return "";
		}
    	
    	/*Uri contactUri = Uri.withAppendedPath(Phones.CONTENT_FILTER_URL, Uri.encode(number));
    	Cursor c = mCtx.getContentResolver().query(contactUri, new String[] { Phones.DISPLAY_NAME }, null, null, null);
    	*/
    	 
    	if(c != null) {
    		c.moveToFirst();
    		if(c.isFirst()) {
    			return c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
    		}else {
    			return "";
    		}
    	}
    	return "";
    }



}
