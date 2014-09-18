package fr.openium.sga.ContentProvider;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ContentProvider.SgaContract.systembdColumns;
import fr.openium.sga.ContentProvider.SgaContract.systemcpColumns;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class SgaDatabase extends SQLiteOpenHelper {
	private static final String TAG = SgaDatabase.class.getSimpleName();
	private static final boolean DEBUG = true;

	private static final String DB_NAME = "sga.db";
	private static final int DB_VERSION = 13;

	public interface Tables {
		public static final String SYSTEM_CONTENT_PROVIDER = "cp";
		public static final String SYSTEM_BROADCAST_PROVIDER = "bd";

	}

	public SgaDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		if (ConfigApp.DEBUG && DEBUG) {
			Log.d(TAG, "SgaDatabase");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		if (ConfigApp.DEBUG && DEBUG) {
			Log.d(TAG, "onCreate");
		}
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE ").append(Tables.SYSTEM_CONTENT_PROVIDER)
				.append(" ( ");
		sb.append(BaseColumns._ID).append(
				" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(systemcpColumns.IDENTIFIER).append(" TEXT, ");
		sb.append(systemcpColumns.NAME).append(" TEXT, ");
		sb.append(systemcpColumns.STATUS).append(" TEXT, ");
		sb.append(systemcpColumns.PREVIOUS).append(" TEXT, ");
		sb.append(systemcpColumns.LOCKSTATUS).append(" TEXT, ");
		sb.append(systemcpColumns.HISTORY).append(" TEXT)");

		db.execSQL(sb.toString());
		sb.setLength(0);

		sb.append("CREATE TABLE ").append(Tables.SYSTEM_BROADCAST_PROVIDER)
				.append(" ( ");
		sb.append(BaseColumns._ID).append(
				" INTEGER PRIMARY KEY AUTOINCREMENT, ");
		sb.append(systembdColumns.ACTION).append(" TEXT, ");
		sb.append(systembdColumns.NAME).append(" TEXT, ");
		sb.append(systembdColumns.STATUS).append(" TEXT, ");
		sb.append(systembdColumns.PREVIOUS).append(" TEXT, ");
		sb.append(systembdColumns.LOCKSTATUS).append(" TEXT, ");
		sb.append(systembdColumns.HISTORY).append(" TEXT)");
		// sb.append(systembdColumns.INTENT).append(" TEXT)");
		db.execSQL(sb.toString());

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (ConfigApp.DEBUG && DEBUG) {
			Log.d(TAG, "onUpgrade");
		}
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SYSTEM_CONTENT_PROVIDER);
		onCreate(db);
	}

}
