package fr.openium.sga.ContentProvider;

import android.net.Uri;
import android.provider.BaseColumns;

public class SgaContract {

	public SgaContract() {
	}

	public interface systemcpColumns {
		public static final String IDENTIFIER = "identifier";// content://.....
		public static final String NAME = "name";// content://.....
		public static final String STATUS = "status";// true or false
		public static final String PREVIOUS = "previousstatus"; // true or false
		public static final String LOCKSTATUS = "lock"; // true or false
		public static final String HISTORY = "history"; // all change history

	}

	public interface systembdColumns {

		public static final String ACTION = "action";// content://.....
		public static final String NAME = "name";// content://.....
		public static final String STATUS = "status";// true or false
		public static final String PREVIOUS = "previousstatus"; // true or false
		public static final String LOCKSTATUS = "lock"; // true or false
		public static final String HISTORY = "history"; // all change history
		//public static final String INTENT = "intent";

	}

	/*
	 * Provider URI management
	 */
	public static final String CONTENT_AUTHORITY = "fr.openium.sga.provider";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://"
			+ CONTENT_AUTHORITY);
	private static final String CONTENT_TYPE_FORMAT = "vnd.android.cursor.dir/vnd."
			.concat(CONTENT_AUTHORITY).concat(".");
	private static final String CONTENT_ITEM_TYPE_FORMAT = "vnd.android.cursor.item/vnd."
			.concat(CONTENT_AUTHORITY).concat(".");

	public static final String PATH_SYSTEM_CP = "cp";
	public static final String PATH_SYSTEM_BD = "bd";
	
	/*
	 * 
	 */

	public static class SystemCp implements systemcpColumns, BaseColumns {
		private static final String PATH = PATH_SYSTEM_CP;
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH).build();
		public static final String CONTENT_TYPE = CONTENT_TYPE_FORMAT
				.concat(PATH);
		public static final String CONTENT_ITEM_TYPE = CONTENT_ITEM_TYPE_FORMAT
				.concat(PATH);

		public static final String PATH_BASEID = "baseid";

		public static Uri buildBaseIdUri(final long _id) {
			return CONTENT_URI.buildUpon().appendPath(PATH_BASEID)
					.appendPath(Long.toString(_id)).build();
		}

		public static String getBaseIdUri(final Uri uri) {
			return uri.getLastPathSegment();
		}
	}

	public static class SystemBd implements systembdColumns, BaseColumns {
		private static final String PATH = PATH_SYSTEM_BD;
		public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
				.appendPath(PATH).build();
		public static final String CONTENT_TYPE = CONTENT_TYPE_FORMAT
				.concat(PATH);
		public static final String CONTENT_ITEM_TYPE = CONTENT_ITEM_TYPE_FORMAT
				.concat(PATH);

		public static final String PATH_BASEID = "baseid";

		public static Uri buildBaseIdUri(final long _id) {
			return CONTENT_URI.buildUpon().appendPath(PATH_BASEID)
					.appendPath(Long.toString(_id)).build();
		}

		public static String getBaseIdUri(final Uri uri) {
			return uri.getLastPathSegment();
		}
	}

}
