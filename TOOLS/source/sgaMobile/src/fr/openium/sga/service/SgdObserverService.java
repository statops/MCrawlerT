/**
 * 
 */
package fr.openium.sga.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.UserEnvironmentObserver.IUserEnvironmentObserver;
import kit.UserEnvironmentObserver.SgdContentObserver;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.factories.ContentObserverFactory;

/**
 * @author Stassia
 * 
 */
public class SgdObserverService extends Service implements IUserEnvironmentObserver {
	private static final String TAG = SgdObserverService.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	ContentObserver obs;

	@Override
	public void onCreate() {
		registreToAllSystemProviderObservers();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return Service.START_NOT_STICKY;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (ConfigApp.DEBUG) {
			Log.d(TAG, " ============== Service onDestroy =============");
		}
		unregisterContentObserver();
	}

	protected void registreToAllSystemProviderObservers() {
		// read lis of CP
		readCpList();
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "registerToAllSystemProviderObservers ");
		}
		for (int i = 0; i < mListUri.length; i++) {
			if (ConfigApp.DEBUG) {
				Log.i(TAG, i + "  " + mListUri[i].toString());
			}
			registerAndAdd(mListUri[i]);
		}
	}

	private ArrayList<SgdContentObserver> mContentObservers = new ArrayList<SgdContentObserver>();

	private void registerAndAdd(Uri uri) {
		SgdContentObserver observer;
		observer = getAnObserverFor(uri);
		if (!mContentObservers.contains(observer)) {
			if (ConfigApp.DEBUG) {
				Log.d(TAG, "registerAndAdd ");
			}

			getContentResolver().registerContentObserver(uri, true, observer);
			mContentObservers.add(observer);
		}

	}

	private void unregisterContentObserver() {
		if (ConfigApp.DEBUG) {
			Log.e(TAG, "=========unregisterContentObserver==========");
		}
		for (SgdContentObserver obs : mContentObservers) {
			getContentResolver().unregisterContentObserver(obs);
		}
		mContentObservers.clear();
	}

	//@formatter:off 
		public Uri[] mListUri = { ContactsContract.Contacts.CONTENT_URI,
				                   android.provider.Settings.Secure.CONTENT_URI,
				                   android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				                   android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
				                   android.provider.CallLog.CONTENT_URI,
				                   Uri.parse(ConfigApp.SMS_IN_URI),
				                   Uri.parse(ConfigApp.SMS_OUT_URI),
				                   Uri.parse(ConfigApp.SMS_OUTBOX_URI),
				                   Uri.parse(ConfigApp.SMS_SENT_URI),
				                   Uri.parse(ConfigApp.SMS_URI),
				                   Uri.parse("content://com.example.notepad.provider.NotePad"),
				                   Uri.parse("content://sms"),
				                                 
				                   /*
				                   android.provider.CalendarContract.CONTENT_URI,
				                   android.provider.Settings.Global.CONTENT_URI, */};
		//@formatter:on
	/**
	 * Register to the system Content provider
	 */

	private SgdContentObserver getAnObserverFor(Uri uri) {
		return ContentObserverFactory.getObserverfor(uri, this);
	}

	/**
	 * 
	 */

	private void readCpList() {
		initUri();
		File cp = new File(Environment.getExternalStorageDirectory() + File.separator + Config.CP_LIST_FILE);
		if (!cp.exists())
			return;
		try {
			List<String> cpList = FileUtils.readLines(cp, Config.UTF8);
			addToListUri(cpList);
			if (Config.DEBUG) {
				Log.d(TAG, "readCpList ");
				Log.d(TAG, "list: ");
				SgUtils.plot(mLListUri);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param cpList
	 */
	private void addToListUri(List<String> cpList) {
		for (String content : cpList) {
			if (content.equalsIgnoreCase("")) {
				continue;
			}

			mLListUri.add(Uri.parse(content));

		}
	}

	/**
	 * 
	 */
	private void initUri() {
		mLListUri = new ArrayList<Uri>();
		mLListUri.add(android.provider.Settings.Secure.CONTENT_URI);
		mLListUri.add(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		mLListUri.add(android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
		mLListUri.add(android.provider.CallLog.CONTENT_URI);
		mLListUri.add(android.provider.ContactsContract.Contacts.CONTENT_URI);
		// mLListUri.add(android.provider.CalendarContract.CONTENT_URI);
		mLListUri.add(Uri.parse(Config.SMS_IN_URI));
		mLListUri.add(Uri.parse(Config.SMS_OUT_URI));
		mLListUri.add(Uri.parse(Config.SMS_OUTBOX_URI));
		mLListUri.add(Uri.parse(Config.SMS_SENT_URI));
		mLListUri.add(Uri.parse(Config.SMS_URI));
	}

	protected ArrayList<Uri> mLListUri;

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.UserEnvironmentObserver.IUserEnvironmentObserver#sourceName()
	 */
	@Override
	public String sourceName() {
		return SgdObserverService.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * kit.UserEnvironmentObserver.IUserEnvironmentObserver#userEvironmentOnChange
	 * (java.lang.String)
	 */
	@Override
	public void userEvironmentOnChange(String uri) {
		if (ConfigApp.DEBUG) {
			Log.e(TAG, "=========received by service==========");
			Log.e(TAG, "uri :" + uri);
		}
		if (ConfigApp.DEBUG) {
			Log.e(TAG, "userEvironmentOnChange ");
			Log.e(TAG, "write in UE file to indicate change");
		}

		// Notify sdcard/ue_state
		/**
		 * if file exist does not notify else create file and write there the
		 * URI that have been changed. "incovenient" multiple change
		 */

		File ue_notify = new File(Environment.getExternalStorageDirectory() + File.separator + Scenario.UE);
		Log.e(TAG, "ue_notify " + ue_notify.toString());
		if (!ue_notify.exists()) {
			try {
				ue_notify.createNewFile();
				FileUtils.write(ue_notify, uri, true);
			} catch (IOException e) {
				if (ConfigApp.DEBUG) {
					Log.e(TAG, "userEvironmentOnChange ");
					Log.e(TAG, "ue_notify cannot be written ");
					e.printStackTrace();
				}
			}
		} else {
			if (ConfigApp.DEBUG) {
				Log.e(TAG, "userEvironmentOnChange but:  ");
				Log.e(TAG, "may be ue_notify busy? ");
			}
		}

	}

}
