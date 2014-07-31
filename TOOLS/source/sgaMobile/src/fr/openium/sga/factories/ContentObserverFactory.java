package fr.openium.sga.factories;

import kit.UserEnvironmentObserver.IUserEnvironmentObserver;
import kit.UserEnvironmentObserver.SgdContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.contentObserver.SgdCallLogObserver;
import fr.openium.sga.contentObserver.SgdContactObserver;
import fr.openium.sga.contentObserver.SgdMediaExtContentObserver;
import fr.openium.sga.contentObserver.SgdMediaIntContentObserver;
import fr.openium.sga.contentObserver.SgdSmsObserver;
import fr.openium.sga.contentObserver.SgdSystemContentObserver;

public class ContentObserverFactory {

	public static SgdContentObserver getObserverfor(Uri uri, IUserEnvironmentObserver mainActivity) {

		if (uri.equals(ContactsContract.Contacts.CONTENT_URI)) {
			return new SgdContactObserver(new Handler(), uri, mainActivity);
		}
		if (uri.equals(android.provider.Settings.Secure.CONTENT_URI)) {
			return new SgdSystemContentObserver(new Handler(), uri, mainActivity);
		}
		if (uri.equals(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)) {
			return new SgdMediaExtContentObserver(new Handler(), uri, mainActivity);
		}
		if (uri.equals(android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI)) {
			return new SgdMediaIntContentObserver(new Handler(), uri, mainActivity);
		}
		if (uri.equals(android.provider.CallLog.CONTENT_URI)) {
			return new SgdCallLogObserver(new Handler(), uri, mainActivity);
		}
		if (uri.equals(ConfigApp.SMS_IN_URI) || uri.equals(ConfigApp.SMS_OUT_URI)
				|| uri.equals(ConfigApp.SMS_OUTBOX_URI) || uri.equals(ConfigApp.SMS_URI)
				|| uri.equals(ConfigApp.SMS_SENT_URI)) {
			return new SgdSmsObserver(new Handler(), uri, mainActivity);
		}
		return new SgdContentObserver(new Handler(), uri, mainActivity);
	}
}
