/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.activity.serverconfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteConstraintException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.morphoss.acal.CheckServerFailedError;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.ServiceManager;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.connector.AcalRequestor;


/**
 * <p>
 *  When a server is first configured we run a bunch of tests on it to try and figure
 *  out WTF we are dealing with.  This is where most of that happens.
 *  </p>
 * 
 * @author Morphoss Ltd
 *
 */
public class CheckServerDialog {


	private static final String TAG = "aCal CheckServerDialog";
	private final Context context;
	private final ContentValues serverData;
	private final ServerConfigurator sc;
	private AcalRequestor requestor;
	private ProgressDialog dialog;

	private static final int SHOW_FAIL_DIALOG = 0;
	private static final int CHECK_COMPLETE = 1;
	private static final int REFRESH_PROGRESS = 2;
	private static final String MESSAGE = "MESSAGE";
	private static final String REFRESH = "REFRESH";
	private static final String TYPE = "TYPE";

	private List<String> successMessages = new ArrayList<String>();
	private boolean advancedMode = false;

	//dialog types
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			int type = b.getInt(TYPE);
			switch (type) {
				case REFRESH_PROGRESS: 	
					if (dialog != null)
						dialog.setMessage(b.getString(REFRESH));
					break;

				case SHOW_FAIL_DIALOG: 	
					if (dialog != null) {
						dialog.dismiss();
						dialog=null;
					}
					showFailDialog(b.getString(MESSAGE));
					break;
				case CHECK_COMPLETE:
					if (dialog != null) {
						dialog.dismiss();
						dialog = null;
					}
					showSuccessDialog(b.getString(MESSAGE));
					break;
			}
		}
	};
	private RunAllTests	testRunner;
	
	
	public CheckServerDialog(ServerConfigurator serverConfiguration, ContentValues serverData, Context cx, ServiceManager sm) {
		this.advancedMode = (serverConfiguration.isAdvancedInterface());
		this.context = cx;
		this.sc = serverConfiguration;
		

		//we must remove any values that may have leaked through from XML that are not part of the DB table
		ServerConfigData.removeNonDBFields(serverData);
		this.serverData = serverData;
	}


	private class RunAllTests extends AsyncTask<Boolean, Integer, Void> {
		
		private class TestsCancelledException extends Exception {
			private static final long	serialVersionUID	= 1L;
		};
		
		protected Void doInBackground(Boolean... params) {
			if ( advancedMode )
				requestor = AcalRequestor.fromServerValues(serverData);
			else {
				requestor = AcalRequestor.fromSimpleValues(serverData);
			}
			try {
				checkServer();
			}
			catch(  TestsCancelledException t ) { }
			return null;
		}
		
		protected void onProgressUpdate(Integer... progress) {
		}
		
	    protected void onPostExecute(Void result) {
	    }
		
		/**
		 * <p>
		 * Checks the server they have entered for validity. Endeavouring to profile it in some ways that we can
		 * use later to control how we will conduct synchronisations and discovery against it.
		 * </p>
		 */
		private void checkServer() throws TestsCancelledException {
			Iterator<TestPort> testers;
			TestPort tester = null;
			successMessages = new ArrayList<String>();
			try {
				Log.w(TAG, "Checking server with hostName: " + requestor.getHostName());

				// Step 1, check for internet connectivity
				updateProgress(context.getString(R.string.checkServer_Internet));
				if ( !checkInternetConnected() ) {
					throw new CheckServerFailedError(context.getString(R.string.internetNotAvailable));
				}

				// Step 2, check we can connect to the server on the given port
				if ( advancedMode ) {
					tester = new TestPort(requestor);
					updateProgress(context.getString(R.string.checkServer_SearchingForServer, requestor.fullUrl()));
					if ( !tester.hasCalDAV() ) {
						// Try harder!
						requestor.applyFromServer(serverData, false);
						tester.reProbe();
					}
				}
				else {
					testers = TestPort.defaultIterator(requestor);
					if ( TestPort.addSrvLookups(requestor) ) testers = TestPort.reIterate();

					do {
						tester = testers.next();
						requestor.applyFromServer(serverData, true);
						requestor.setPortProtocol(tester.port, (tester.useSSL?1:0));
						updateProgress(context.getString(R.string.checkServer_SearchingForServer, requestor.fullUrl()));
					}
					while ( !tester.hasCalDAV() && testers.hasNext() );

					if ( !tester.hasCalDAV() ) {
						// Try harder!
						testers = TestPort.reIterate();
						do {
							tester = testers.next();
							requestor.applyFromServer(serverData, true);
							requestor.setPortProtocol(tester.port, (tester.useSSL?1:0));
							tester.reProbe(); // Extend the timeout
							updateProgress(context.getString(R.string.checkServer_SearchingForServer, requestor.fullUrl()));
						}
						while ( !tester.hasCalDAV() && testers.hasNext() );
					}
				}

				if ( tester.hasCalDAV() ) {
					Log.w(TAG, "Found CalDAV: " + requestor.fullUrl());
					serverData.put(Servers.HAS_CALDAV, 1);
					successMessages.add(context.getString(R.string.serverSupportsCalDAV));
					successMessages.add(String.format(context.getString(R.string.foundPrincipalPath), requestor.fullUrl()));
					tester.applyToServerSettings(serverData);
				}
				else {
					int maxAchievement = TestPort.PORT_IS_CLOSED;
					if ( advancedMode ) {
						if ( tester.getAchievement() > maxAchievement ) maxAchievement = tester.getAchievement(); 
					}
					else {
						testers = TestPort.reIterate();
						do {
							tester = testers.next();
							if ( tester.getAchievement() > maxAchievement ) maxAchievement = tester.getAchievement();
/*
							if ( tester.getAchievement() == TestPort.HAS_CALDAV ) {
								serverData.put(Servers.HAS_CALDAV, 1);
								successMessages.add(context.getString(R.string.serverSupportsCalDAV));
								successMessages.add(String.format(context.getString(R.string.foundPrincipalPath), requestor.fullUrl()));
								tester.applyToServerSettings(serverData);
							}
*/
						}
						while ( testers.hasNext() );
					}

					if ( maxAchievement == TestPort.SSL_FAILED ) {
						Log.w(TAG, "Failed SSL Validation: " + requestor.fullUrl());
						successMessages.add(context.getString(R.string.SslCertificateError));
					}
					if ( maxAchievement == TestPort.AUTH_FAILED ) {
						Log.w(TAG, "Failed Auth: " + requestor.fullUrl());
						successMessages.add(context.getString(R.string.authenticationFailed));
					}
					else if ( maxAchievement == TestPort.AUTH_SUCCEEDED ) {
						Log.w(TAG, "Found DAV but not CalDAV: " + requestor.fullUrl());
						successMessages.add(context.getString(R.string.serverHasDAVNoCalDAV));
					}
					else if ( maxAchievement < TestPort.NO_DAV_RESPONSE ) {
						successMessages.add(context.getString(R.string.couldNotDiscoverPort));
					}
					else {
						Log.w(TAG, "Found no CalDAV");
						serverData.put(Servers.HAS_CALDAV, 0);
						successMessages.add(context.getString(R.string.serverLacksCalDAV));
					}
				}


				// Step 6, Exit with success message
				Message m = Message.obtain();
				Bundle b = new Bundle();
				b.putInt(TYPE, (tester.hasCalDAV() ? CHECK_COMPLETE : SHOW_FAIL_DIALOG));

				StringBuilder successMessage = new StringBuilder("");
				for( String msg : successMessages ) {
					successMessage.append("\n");
					successMessage.append(msg);
				}
				b.putString(MESSAGE, successMessage.toString());

				m.setData(b);
				handler.sendMessage(m);

			}
			catch (CheckServerFailedError e) {

				// Getting server details failed
				if (Constants.LOG_DEBUG)
					Log.d(TAG, "Connect failed: " + e.getMessage());
				Message m = Message.obtain();
				Bundle b = new Bundle();
				b.putInt(TYPE, SHOW_FAIL_DIALOG);
				b.putString(MESSAGE, e.getMessage());
				m.setData(b);
				handler.sendMessage(m);

			}
			catch (TestsCancelledException e) {
				Message m = Message.obtain();
				Bundle b = new Bundle();
				b.putInt(TYPE, SHOW_FAIL_DIALOG);
				b.putString(MESSAGE, context.getString(R.string.checkServer_DiscoveryCancelled));
				m.setData(b);
				handler.sendMessage(m);
			}
			catch (Exception e) {
				// Something unknown went wrong
				Log.w(TAG, "Unexpected Failure: ", e);
				Message m = Message.obtain();
				Bundle b = new Bundle();
				b.putInt(TYPE, SHOW_FAIL_DIALOG);
				b.putString(MESSAGE, "Unknown Error: " + e.getMessage());
				m.setData(b);
				handler.sendMessage(m);
			}
		}

		/**
		 * Update the progress dialog with a friendly string.
		 * @param newMessage
		 * @throws TestsCancelledException 
		 */
		private void updateProgress( String newMessage ) throws TestsCancelledException {
			if ( this.isCancelled() ) throw new TestsCancelledException();
			Message m = Message.obtain();
			Bundle b = new Bundle();
			b.putInt(TYPE, REFRESH_PROGRESS);
			b.putString(REFRESH, newMessage);
			m.setData(b);
			handler.sendMessage(m);
		}

	}

	private void createProgressDialog(String title, String message, String buttonText)
	{
		dialog = new ProgressDialog((Context) sc);
	    dialog.setTitle(title);
	    dialog.setMessage(message);
	    dialog.setButton(buttonText, new DialogInterface.OnClickListener() 
	    {
	        public void onClick(DialogInterface dialog, int which) 
	        {
	            // Use either finish() or return() to either close the activity or just the dialog
	        	testRunner.cancel(true);
	            return;
	        }
	    });
	    dialog.show();
	}

	public void start() {
		createProgressDialog( context.getString(R.string.checkServer), context.getString(R.string.checkServer_Connecting), context.getString(R.string.cancel));
		dialog.setIndeterminate(true);
		
		testRunner = new RunAllTests();
		testRunner.execute();
	}
	

	private void showFailDialog(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder((Context) sc);
		builder.setMessage(
					context.getString(R.string.serverFailedValidation)
					+"\n\n" + msg +"\n\n"
					+ context.getString(R.string.saveSettingsAnyway)
			);
		builder.setPositiveButton(context.getString(android.R.string.yes), dialogClickListener);
		builder.setNegativeButton(context.getString(android.R.string.no), dialogClickListener);
		builder.show();
	}

	private void showSuccessDialog(String msg) {

		AlertDialog.Builder builder = new AlertDialog.Builder((Context) sc);

		try {
			// Before we display the success dialog and especially before we start syncing it.
			sc.saveData();
			sc.setResult(Activity.RESULT_OK);

			builder.setMessage(msg + "\n\n" + context.getString(R.string.serverValidationSuccess));
			
		}
		catch( SQLiteConstraintException e ) {
			builder.setMessage(msg + "\n\n" + context.getString(R.string.serverValidationSuccess)
						+ "\n\n" + context.getString(R.string.serverRecordAlreadyExists)
				);
		}

		// We don't set a positive button here since we already saved, above, and
		// the background actions may have already updated the server table further!
		// Or worse: we couldn't save, so trying again would be futile...
		builder.setNeutralButton(context.getString(android.R.string.ok), dialogClickListener);

		// builder.setNegativeButton("No", dialogClickListener);
		builder.show();
	}

	DialogInterface.OnClickListener	dialogClickListener	= new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					sc.setResult(Activity.RESULT_FIRST_USER);
					sc.saveData();
					// fall through
				case DialogInterface.BUTTON_NEUTRAL:
					// We already saved before displaying the success dialog to give sync a headstart
					sc.finish();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					sc.setResult(Activity.RESULT_CANCELED);
					break;
			}
		}
	};

	/** Check server methods: Each of these methods represents a different step in the check server process */
	
	/**
	 *	Checks to see if the device has a connection to the internet. Throws CheckServerFailed Error if no connection
	 *	can be established, or if System throws an error while trying to find out 
	 * @throws CheckServerFailedError
	 */
	private boolean checkInternetConnected() {
		try {
			ConnectivityManager connec = sc.getConnectivityService();
			NetworkInfo netInfo = connec.getActiveNetworkInfo();
			return  (netInfo != null && netInfo.isConnected());
		}
		catch (Exception e) {
			return false;
		}
	}


	
}
