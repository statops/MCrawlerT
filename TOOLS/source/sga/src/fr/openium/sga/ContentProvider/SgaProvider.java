package fr.openium.sga.ContentProvider;

import java.util.ArrayList;

import fr.openium.sga.ConfigApp;

import kit.Database.SgaBuilder;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class SgaProvider extends ContentProvider {

	private SgaDatabase mOpenHelper;
	private static final UriMatcher sUriMatcher = buildUriMatcher();

	private static final int SYSTEM_CP = 100;
	private static final int SYSTEM_CP_BASEID = 101;

	private static final int SYSTEM_BD = 200;
	private static final int SYSTEM_BD_BASEID = 201;

	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = SgaContract.CONTENT_AUTHORITY;
		matcher.addURI(authority, SgaContract.PATH_SYSTEM_CP, SYSTEM_CP);
		matcher.addURI(authority, SgaContract.PATH_SYSTEM_CP + "/"
				+ SgaContract.SystemCp.PATH_BASEID + "/#", SYSTEM_CP_BASEID);

		matcher.addURI(authority, SgaContract.PATH_SYSTEM_BD, SYSTEM_BD);
		matcher.addURI(authority, SgaContract.PATH_SYSTEM_BD + "/"
				+ SgaContract.SystemBd.PATH_BASEID + "/#", SYSTEM_BD_BASEID);

		return matcher;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new SgaDatabase(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case SYSTEM_CP:
		case SYSTEM_CP_BASEID:
			return SgaContract.SystemCp.CONTENT_ITEM_TYPE;
		case SYSTEM_BD:
		case SYSTEM_BD_BASEID:
			return SgaContract.SystemBd.CONTENT_ITEM_TYPE;

		default:
			throw new UnsupportedOperationException("Unknown uri: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (ConfigApp.DEBUG) {
			Log.i("", "insert" + uri.toString());
		}

		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case SYSTEM_CP: {
			long _id = db.insertOrThrow(
					SgaDatabase.Tables.SYSTEM_CONTENT_PROVIDER, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return SgaContract.SystemCp.buildBaseIdUri(_id);
		}

		case SYSTEM_BD: {
			long _id = db.insertOrThrow(
					SgaDatabase.Tables.SYSTEM_BROADCAST_PROVIDER, null, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return SgaContract.SystemCp.buildBaseIdUri(_id);
		}

		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (ConfigApp.DEBUG) {
			Log.i("", "delete  " + uri.toString());
		}

		int count = 0;
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final SgaBuilder builder = buildSimpleSelection(uri);
		count = builder.where(selection, selectionArgs).delete(db);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (ConfigApp.DEBUG) {
			Log.i("", "query " + uri.toString());
		}

		final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		SgaBuilder builder = null;
		Cursor c = null;
		builder = buildSimpleSelection(uri);
		builder.where(selection, selectionArgs);
		c = builder.query(db, projection, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (ConfigApp.DEBUG) {
			Log.i("", "update " + uri.toString());
		}
		int count = 0;
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final SgaBuilder builder = buildSimpleSelection(uri);
		count = builder.where(selection, selectionArgs).update(db, values);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private SgaBuilder buildSimpleSelection(Uri uri) {
		final SgaBuilder builder = new SgaBuilder();
		final int match = sUriMatcher.match(uri);
		switch (match) {
		case SYSTEM_CP:
			builder.table(SgaDatabase.Tables.SYSTEM_CONTENT_PROVIDER);
			break;
		case SYSTEM_CP_BASEID:
			builder.table(SgaDatabase.Tables.SYSTEM_CONTENT_PROVIDER).where(
					BaseColumns._ID + "=?",
					SgaContract.SystemCp.getBaseIdUri(uri));
			break;

		case SYSTEM_BD:
			builder.table(SgaDatabase.Tables.SYSTEM_BROADCAST_PROVIDER);
			break;
		case SYSTEM_BD_BASEID:
			builder.table(SgaDatabase.Tables.SYSTEM_BROADCAST_PROVIDER).where(
					BaseColumns._ID + "=?",
					SgaContract.SystemCp.getBaseIdUri(uri));
			break;

		}
		return builder;
	}

	@SuppressWarnings("finally")
	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		final int numOperations = operations.size();
		final ContentProviderResult[] results = new ContentProviderResult[numOperations];
		try {
			db.beginTransaction();
			for (int i = 0; i < numOperations; i++) {
				results[i] = operations.get(i).apply(this, results, i);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			return results;
		}

	}

}
