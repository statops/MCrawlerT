package fr.openium.sga.factories;

import kit.UserEnvironmentObserver.IUserEnvironmentObserver;
import kit.UserEnvironmentObserver.SgdContentObserver;
import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.contentObserver.SgdBrowserOserver;
import fr.openium.sga.contentObserver.SgdCallLogObserver;
import fr.openium.sga.contentObserver.SgdContactObserver;
import fr.openium.sga.contentObserver.SgdMediaExtContentObserver;
import fr.openium.sga.contentObserver.SgdMediaIntContentObserver;
import fr.openium.sga.contentObserver.SgdSmsObserver;
import fr.openium.sga.contentObserver.SgdSystemContentObserver;

@SuppressLint("NewApi")
public class ContentObserverFactory {

	public static SgdContentObserver getObserverfor(Uri uri,
			IUserEnvironmentObserver observersContext) {

		if (uri.equals(ContactsContract.Contacts.CONTENT_URI)) {
			return new SgdContactObserver(null, uri, observersContext);
		}
		if (uri.equals(android.provider.Settings.Secure.CONTENT_URI)) {
			return new SgdSystemContentObserver(null, uri,
					observersContext);
		}
		if (uri.equals(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
			return new SgdMediaExtContentObserver(null, uri,
					observersContext);
		}
		if (uri.equals(android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI)) {
			return new SgdMediaIntContentObserver(null, uri,
					observersContext);
		}
		if (uri.equals(android.provider.CallLog.CONTENT_URI)) {
			return new SgdCallLogObserver(null, uri, observersContext);
		}

		if (uri.equals(android.provider.Browser.BOOKMARKS_URI)
				|| uri.equals(android.provider.Browser.SEARCHES_URI)) {
			return new SgdBrowserOserver(null, uri, observersContext);
		}

		if (uri.equals(ConfigApp.SMS_IN_URI)
				|| uri.equals(ConfigApp.SMS_OUT_URI)
				|| uri.equals(ConfigApp.SMS_OUTBOX_URI)
				|| uri.equals(ConfigApp.SMS_URI)
				|| uri.equals(ConfigApp.SMS_SENT_URI)
		/**
		 * || uri.toString() .contains(Telephony.Sms.CONTENT_URI.toString())
		 */
		) {
			return new SgdSmsObserver(null, uri, observersContext);
		}

		return new SgdContentObserver(null, uri, observersContext);
	}
}
