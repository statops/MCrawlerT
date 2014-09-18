package fr.openium.sga.contentObserver;

import kit.UserEnvironmentObserver.IUserEnvironmentObserver;
import kit.UserEnvironmentObserver.SgdContentObserver;
import android.net.Uri;
import android.os.Handler;

public class SgdBrowserOserver extends SgdContentObserver {
	public SgdBrowserOserver(Handler handler, Uri handledUri,
			IUserEnvironmentObserver listner) {
		super(handler, handledUri, listner);
	}
}
