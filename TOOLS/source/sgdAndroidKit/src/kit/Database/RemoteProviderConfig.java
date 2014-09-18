package kit.Database;

import android.net.Uri;

public class RemoteProviderConfig {
	/*
	 * 
	 * remote content provider Info
	 */
	public static final String CP_PATH = "cp";
	public static final String CONTENT_AUTHORITY = "fr.openium.sga.provider";
	public static final Uri BASE_CONTENT_URI = Uri.parse("content://"
			+ CONTENT_AUTHORITY);
	public static final Uri SGA_CP_CONTENT_URI = BASE_CONTENT_URI.buildUpon()
			.appendPath(CP_PATH).build();
	public static final String BD_PATH = "bd";
	public static final Uri SGA_BD_CONTENT_URI = BASE_CONTENT_URI.buildUpon()
			.appendPath(BD_PATH).build();

}
