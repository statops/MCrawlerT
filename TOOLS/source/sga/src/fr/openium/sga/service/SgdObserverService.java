/**
 * 
 */
package fr.openium.sga.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.UserEnvironmentObserver.IUserEnvironmentObserver;
import kit.UserEnvironmentObserver.SgdContentObserver;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Telephony;
import android.util.Log;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.ContentProvider.SgaContract;
import fr.openium.sga.broadcastObserver.IServiceProviderUpdater;
import fr.openium.sga.broadcastObserver.SgaBroadcastReceiver;
import fr.openium.sga.broadcastObserver.SgaIntentListner;
import fr.openium.sga.factories.ContentObserverFactory;

/**
 * @author Stassia
 * 
 */
public class SgdObserverService extends Service implements
		IUserEnvironmentObserver, IServiceProviderUpdater {
	private static final String TAG = SgdObserverService.class.getName();
	private final IBinder mBinder = new Binder();
	private HashMap<String, SgaIntentListner> mReceiver = new HashMap<String, SgaIntentListner>();
	private ArrayList<ContentObserver> mContentObservers = new ArrayList<ContentObserver>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {

		if (intent.getAction().equalsIgnoreCase(
				"fr.openium.sga.systemObserver.service")) {
			if (intent.getExtras() != null
					&& intent.getExtras().getBoolean("stop") == true) {
				stopSelf();
			} else {
				registreToAllObservers();
			}
		} else {
			Log.d(TAG, "unknown intent");
		}
		return mBinder;
	}

	ContentObserver obs;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null
				&& intent.getAction() != null
				&& intent.getAction().equalsIgnoreCase(
						"fr.openium.sga.systemObserver.service")) {
			registreToAllObservers();

		} else {
			Log.d(TAG, "unknown intent");

		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, " ============== Service onDestroy =============");
		}
		unregisterToAllObservers();
		super.onDestroy();
	}

	private void unregisterToAllObservers() {
		unregisterContentObserver();
		unregisterToAllSystemBrodCastAction();
	}

	/**
	 * unregister to all type of observers
	 */

	private ArrayList<String> broadcastList = new ArrayList<String>();

	protected void registerToAllSystemBroadCastAction() {
		readBroadCastList();
		registerToBroadcastReceiver();

	}

	private void initToBroadCastProvider() {
		/*
		 * Clean all data
		 */
		getContentResolver().delete(SgaContract.SystemBd.CONTENT_URI, null,
				null);
		for (String actionkey : mReceiver.keySet()) {
			ContentValues uri = new ContentValues();
			uri.put(SgaContract.systembdColumns.ACTION, mReceiver
					.get(actionkey).getIntent().getAction());
			uri.put(SgaContract.systembdColumns.NAME, actionkey);
			uri.put(SgaContract.systembdColumns.LOCKSTATUS, "false");
			uri.put(SgaContract.systembdColumns.STATUS, "false");
			uri.put(SgaContract.systembdColumns.PREVIOUS, "false");
			uri.put(SgaContract.systembdColumns.HISTORY, "");
			/*
			 * uri.put(SgaContract.systembdColumns.INTENT, mReceiver
			 * .get(actionkey).toString());
			 */
			Uri insertV = getContentResolver().insert(
					(SgaContract.SystemBd.CONTENT_URI), uri);
			mReceiver.get(actionkey).updateBdUri(insertV);
		}

	}

	private void registerToBroadcastReceiver() {
		mReceiver.clear();
		SgaBroadcastReceiver receiver = new SgaBroadcastReceiver(this);
		IntentFilter mIntentFilter = new IntentFilter();
		for (String action : broadcastList) {
			mIntentFilter.addAction(action);
			mReceiver.put(action, new SgaIntentListner(new Intent(action),
					receiver));
		}
		initToBroadCastProvider();
		if (registerReceiver(receiver, mIntentFilter) == null) {
			throw new IllegalStateException("No observers have been registered");
		}
	}

	private void readBroadCastList() {
		InputStream listFile = this.getResources().openRawResource(
				fr.openium.sga.R.raw.broadcast);
		Scanner scan = new Scanner(listFile);
		while ((scan.hasNext())) {
			broadcastList.add(scan.nextLine());
		}
	}

	protected void unregisterToAllSystemBrodCastAction() {
		for (String actionKey : mReceiver.keySet()) {
			try {
				unregisterReceiver(mReceiver.get(actionKey).getReceiver());
			} catch (IllegalArgumentException ex) {
				break;
			}

		}

	}

	/**
	 * register to all type of observers
	 */
	protected void registreToAllObservers() {
		registerToSystemProviders();
		registerToAllSystemBroadCastAction();
	}

	private void registerToSystemProviders() {

		// read lis of CP
		readCpList();
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "registerToAllSystemProviderObservers ");
		}
		registerAndAdd();
		initProvider();

	}

	private void initProvider() {
		/*
		 * Clean all data
		 */
		getContentResolver().delete(SgaContract.SystemCp.CONTENT_URI, null,
				null);
		for (ContentObserver uriToListen : mContentObservers) {

			ContentValues uri = new ContentValues();
			uri.put(SgaContract.systemcpColumns.IDENTIFIER,
					((SgdContentObserver) uriToListen).getObservableUri()
							.toString());
			uri.put(SgaContract.systemcpColumns.NAME,
					((SgdContentObserver) uriToListen).getObservableUri()
							.toString());
			uri.put(SgaContract.systemcpColumns.LOCKSTATUS, "false");
			uri.put(SgaContract.systemcpColumns.STATUS, "false");
			uri.put(SgaContract.systemcpColumns.PREVIOUS, "false");
			uri.put(SgaContract.systemcpColumns.HISTORY, "");
			Uri insertV = getContentResolver().insert(
					(SgaContract.SystemCp.CONTENT_URI), uri);
			((SgdContentObserver) uriToListen).updateCurrentUriValue(insertV);
		}

	}

	private synchronized void updateProvider(SgdContentObserver obs) {
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "updateProvider ");
		}

		Uri toChange = obs.getObservableUriId();
		/**
		 * check if blocked by crawler
		 */

		Cursor cursor = getContentResolver().query(toChange, null, null, null,
				null);
		cursor.moveToFirst();
		String history = cursor.getString(cursor
				.getColumnIndex(SgaContract.systemcpColumns.HISTORY)) + "  \n";
		if (Boolean.parseBoolean(cursor.getString(cursor
				.getColumnIndex(SgaContract.systemcpColumns.LOCKSTATUS)))) {

			if (ConfigApp.DEBUG) {
				Log.i(TAG,
						"Not updated "
								+ Boolean.parseBoolean(cursor.getString(cursor
										.getColumnIndex(SgaContract.systemcpColumns.LOCKSTATUS))));
			}
			/**
			 * only update the history
			 */
			ContentValues vatToUpdate = new ContentValues();

			vatToUpdate.put(
					SgaContract.systemcpColumns.HISTORY,
					history
							+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
									.format(new Date()) + " true");

			if (ConfigApp.DEBUG) {
				Log.i(TAG, "updateProvider ");
			}

			updateProvider(toChange, vatToUpdate);
			cursor.close();
			return;

		} else {
			if (ConfigApp.DEBUG) {
				Log.i(TAG, "updated ");
			}
		}

		ContentValues uri = new ContentValues();
		uri.put(SgaContract.systemcpColumns.STATUS, "true");
		uri.put(SgaContract.systemcpColumns.PREVIOUS, cursor.getString(cursor
				.getColumnIndex(SgaContract.systemcpColumns.STATUS)));
		uri.put(SgaContract.systemcpColumns.HISTORY,
				history
						+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
								.format(new Date()) + " true");
		updateProvider(toChange, uri);
		cursor.close();
	}

	private synchronized void updateProvider(Uri toChange, ContentValues val) {
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "updateProvider ");
		}

		int updated = getContentResolver().update(toChange, val, null, null);

		if (ConfigApp.DEBUG) {
			Log.i(TAG, "updated row number " + updated);
		}

	}

	private void registerAndAdd() {
		mContentObservers.clear();
		for (int i = 0; i < mLListUri.size(); i++) {
			if (ConfigApp.DEBUG) {
				Log.i(TAG, i + "  " + mLListUri.get(i).toString());
			}
			registerAndAdd(mLListUri.get(i));
		}
	}

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
		for (ContentObserver obs : mContentObservers) {
			getContentResolver().unregisterContentObserver(obs);
		}
		mContentObservers.clear();
	}

	// @formatter:on
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
		if (Build.VERSION.SDK_INT > 15) {
			newApiUri();
		}

		File cp = new File(Environment.getExternalStorageDirectory()
				+ File.separator + Config.CP_LIST_FILE);
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

	@SuppressLint("NewApi")
	private void newApiUri() {
		mLListUri.add(android.provider.Settings.Global.CONTENT_URI);
		mLListUri
				.add(android.provider.CalendarContract.Events.CONTENT_EXCEPTION_URI);
		mLListUri.add(Telephony.Mms.CONTENT_URI);
		mLListUri.add(android.provider.CalendarContract.Events.CONTENT_URI);
		mLListUri.add(Telephony.Sms.Inbox.CONTENT_URI);
		mLListUri.add(Telephony.Sms.CONTENT_URI);
		mLListUri.add(Telephony.Sms.Outbox.CONTENT_URI);
		mLListUri.add(Telephony.Sms.Draft.CONTENT_URI);
		mLListUri.add(Telephony.Sms.Sent.CONTENT_URI);
		mLListUri.add(Telephony.Sms.Conversations.CONTENT_URI);
		mLListUri.add(Telephony.Mms.CONTENT_URI);
		mLListUri
				.add(android.provider.VoicemailContract.Voicemails.CONTENT_URI);

	}

	/**
	 * List of listened content provider
	 */
	// @SuppressLint("NewApi")
	// @SuppressLint("NewApi")
	private void initUri() {
		mLListUri = new ArrayList<Uri>();
		mLListUri.add(android.provider.Settings.Secure.CONTENT_URI);
		mLListUri
				.add(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		mLListUri
				.add(android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
		mLListUri.add(android.provider.CallLog.CONTENT_URI);
		mLListUri.add(android.provider.ContactsContract.Contacts.CONTENT_URI);
		mLListUri.add(Uri.parse(Config.SMS_IN_URI));
		mLListUri.add(Uri.parse(Config.SMS_OUT_URI));
		mLListUri.add(Uri.parse(Config.SMS_OUTBOX_URI));
		mLListUri.add(Uri.parse(Config.SMS_SENT_URI));
		mLListUri.add(Uri.parse(Config.SMS_URI));
		/*
		 * 
		 * New added
		 */
		mLListUri.add(android.provider.Browser.BOOKMARKS_URI);
		mLListUri.add(android.provider.Browser.SEARCHES_URI);

		mLListUri
				.add(android.provider.ContactsContract.Contacts.CONTENT_LOOKUP_URI);
		mLListUri
				.add(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI);
		mLListUri.add(android.provider.UserDictionary.Words.CONTENT_URI);

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
	public void userEvironmentOnChange(Object uri) {

		if (uri instanceof String) {
			handleUriStringChange((String) uri);
		}

		if (uri instanceof SgdContentObserver) {
			handleUriContentObserverChange((SgdContentObserver) uri);
		}

	}

	private void handleUriContentObserverChange(SgdContentObserver uri) {
		/*
		 * update or insert
		 */
		for (int i = 0; i < mContentObservers.size(); i++) {
			SgdContentObserver currentObs = (SgdContentObserver) mContentObservers
					.get(i);
			if (currentObs.getObservableUri().toString()
					.equalsIgnoreCase(uri.getObservableUri().toString())) {
				updateProvider((SgdContentObserver) mContentObservers.get(i));
			}
		}

	}

	private void handleUriStringChange(String uri) {
		// Toast.makeText(this, "userEvironmentOnChange(String uri) :" + uri,
		// Toast.LENGTH_LONG).show();

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

		File ue_notify = new File(Environment.getExternalStorageDirectory()
				+ File.separator + Scenario.UE);
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

		/*
		 * update or insert
		 */
		for (int i = 0; i < mContentObservers.size(); i++) {

			SgdContentObserver currentObs = (SgdContentObserver) mContentObservers
					.get(i);
			if (uri.contains(currentObs.getObservableUri().toString())) {
				if (ConfigApp.DEBUG) {
					Log.e(TAG, "Update CP " + uri);
				}

				updateProvider((SgdContentObserver) mContentObservers.get(i));
			}
		}
	}

	public ArrayList<ContentObserver> getObservers() {
		return mContentObservers;
	}

	public HashMap<String, SgaIntentListner> getBdObservers() {
		return mReceiver;
	}

	private synchronized void updateBroadcastProvider(String action) {

	}

	private void updateProvider(String action, ContentValues vatToUpdate) {
		String[] selectionArgs = { "" };
		String selectionClause = SgaContract.systembdColumns.ACTION + " = ?";
		selectionArgs[0] = action;

		int changed = getContentResolver().update(
				SgaContract.SystemBd.CONTENT_URI, vatToUpdate, selectionClause,
				selectionArgs);
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "updated row number in BDProvider" + changed);
		}

	}

	@Override
	public synchronized void updateBroadCastProvder(String action) {
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "update Broadcast Provider ");
		}

		/**
		 * check if blocked by crawler
		 */
		// projection
		// args
		String[] selectionArgs = { "" };
		String selectionClause = SgaContract.systembdColumns.ACTION + " = ?";
		selectionArgs[0] = action;

		Cursor cursor = getContentResolver().query(
				SgaContract.SystemBd.CONTENT_URI, null, selectionClause,
				selectionArgs, null);
		cursor.moveToFirst();

		String history = cursor.getString(cursor
				.getColumnIndex(SgaContract.systemcpColumns.HISTORY)) + "  \n";
		if (Boolean.parseBoolean(cursor.getString(cursor
				.getColumnIndex(SgaContract.systemcpColumns.LOCKSTATUS)))) {

			if (ConfigApp.DEBUG) {
				Log.i(TAG,
						"Not updated "
								+ Boolean.parseBoolean(cursor.getString(cursor
										.getColumnIndex(SgaContract.systemcpColumns.LOCKSTATUS))));
			}
			/**
			 * only update the history
			 */
			ContentValues vatToUpdate = new ContentValues();

			vatToUpdate.put(
					SgaContract.systemcpColumns.HISTORY,
					history
							+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
									.format(new Date()) + " true");

			if (ConfigApp.DEBUG) {
				Log.i(TAG, "updateProvider ");
			}

			updateProvider(action, vatToUpdate);
			cursor.close();
			return;

		} else {
			if (ConfigApp.DEBUG) {
				Log.i(TAG, "updated ");
			}
		}

		ContentValues uri = new ContentValues();
		uri.put(SgaContract.systemcpColumns.STATUS, "true");
		uri.put(SgaContract.systemcpColumns.PREVIOUS, cursor.getString(cursor
				.getColumnIndex(SgaContract.systemcpColumns.STATUS)));
		uri.put(SgaContract.systemcpColumns.HISTORY,
				history
						+ new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
								.format(new Date()) + " true");
		updateProvider(action, uri);
		cursor.close();

	}

}
