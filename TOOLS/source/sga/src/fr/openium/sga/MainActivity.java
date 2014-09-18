package fr.openium.sga;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import fr.openium.sga.ScenarioReceiver.Receiver;
import fr.openium.sga.intentService.SgaRunTest;
import fr.openium.sga.service.SgdObserverService;

public class MainActivity extends Activity implements Receiver {

	private ScenarioReceiver mScenarioReceiver;
	private final static String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onCreate ");
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// unregisterContentObserver();
		// registreToAllSystemProviderObservers();
		// launchTestingWithIntent(null);

		final Intent sintent = new Intent(MainActivity.this,
				SgdObserverService.class);
		sintent.setAction("fr.openium.sga.systemObserver.service");

		Thread startsUEService = new Thread() {
			public void run() {
				startService(sintent);
			};
		};

		startsUEService.start();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (ConfigApp.DEBUG) {
			Log.i(TAG, "onNewIntent ");
			Log.i("[Info]", "Restart with " + intent.toString());
		}
		super.onNewIntent(intent);
		if (isTestRunning()) {
			if (ConfigApp.DEBUG) {
				Log.i(TAG, "Test is Running ");
			}
			super.onPause();
		}
	}

	private void launchTestingWithIntent(Intent intent) {
		/**
		 * register to observers first
		 */
		// unregisterContentObserver();
		// registreToAllSystemProviderObservers();
		deleteOlderOkile();
		if (intent == null) {
			EditText packageEditText = (EditText) findViewById(R.id.editText1);
			EditText numberEditText = (EditText) findViewById(R.id.editText2);
			startTestIntentService(packageEditText.getText().toString(),
					numberEditText.getText().toString(), null, null);
		} else {
			try {
				startTestIntentService(
						intent.getExtras().getString(
								getResources()
										.getString(R.string.extra_package)),
						intent.getExtras().getString(
								getResources().getString(R.string.nTime)),
						intent.getExtras().getString("class"), intent
								.getExtras().getString("stopError"));
			} catch (NullPointerException e) {
				((TextView) findViewById(R.id.text))
						.setText("No extra value ...");
			}
		}

	}

	private void deleteOlderOkile() {
		File f = new File(Environment.getExternalStorageDirectory().getPath()
				+ getResources().getString(R.string.testResults)
				+ getResources().getString(R.string.okfile));
		if (f.exists()) {
			f.delete();
		}

	}

	@Override
	protected void onResume() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onResume ");
		}

		final Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				/**
				 * launch test{
				 * 
				 */
				launchTestingWithIntent(null);
			}
		});

		if (!isTestRunning() && !serviceStatus) {
			if (ConfigApp.DEBUG) {
				Log.i(TAG, "Test is not Running ");
			}
			if (getIntent().getAction().equalsIgnoreCase(
					getResources().getString((R.string.action_name)))) {

				serviceStatus = true;
				/**
				 * delete okFile if exist
				 */
				launchTestingWithIntent(getIntent());
			}
		}
		super.onResume();
	}

	@Override
	protected void onRestart() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onRestart ");
		}
		super.onPause();
	}

	@Override
	protected void onPause() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onPause ");
		}
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onStop ");
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onDestroy ");
		}
		// unregisterContentObserver();
		super.onDestroy();
	}

	/**
	 * 
	 * @param packageS
	 * @param number
	 * @param _class
	 * @param _ErrorStatus
	 */
	private void startTestIntentService(String _package, String _number,
			String _class, String _ErrorStatus) {

		Intent intentService = new Intent(this, SgaRunTest.class);
		// test
		intentService.setAction(null);
		// endTest

		intentService.putExtra(
				getResources().getString(R.string.extra_package), _package);
		intentService.putExtra(getResources().getString(R.string.nTime),
				_number);
		intentService.putExtra("class", _class);
		intentService.putExtra("stopError", _ErrorStatus);

		mScenarioReceiver = new ScenarioReceiver(new Handler());
		mScenarioReceiver.setReceiver(this);
		intentService.putExtra("receiver", mScenarioReceiver);
		startService(intentService);
		((TextView) findViewById(R.id.text)).setText("Starting ...");
	}

	private boolean isTestRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (SgaRunTest.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onStart ");
		}
		super.onStart();
	}

	private static boolean serviceStatus = false;

	/**
	 * send message to desktop appli that test is finished.
	 */
	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		if (ConfigApp.DEBUG) {
			Log.d(TAG, "onReceiveResult ");
		}
		if (resultCode == 1) {
			SgaTestResult result = (SgaTestResult) resultData
					.getSerializable("result");
			((TextView) findViewById(R.id.time)).setText("finish ...: "
					+ result.getValue() + "time ...: "
					+ result.getStaringTime() + "  " + result.getEndingTime());
			((EditText) findViewById(R.id.editText1))
					.setVisibility(View.INVISIBLE);
			((EditText) findViewById(R.id.editText2))
					.setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.textView2))
					.setVisibility(View.INVISIBLE);
			((TextView) findViewById(R.id.textView1))
					.setVisibility(View.INVISIBLE);
			((Button) findViewById(R.id.button1)).setEnabled(false);
			/**
			 * save time
			 */
			save("1", new File(Environment.getExternalStorageDirectory()
					.getPath()
					+ getResources().getString(R.string.testResults)
					+ getResources().getString(R.string.okfile)));
			save_time(result);
		}
		serviceStatus = false;

		/**
		 * 
		 * unregister
		 */
		// stop service
		final Intent sintent = new Intent(MainActivity.this,
				SgdObserverService.class);
		sintent.setAction("fr.openium.sga.systemObserver.service");
		sintent.putExtra("stop", true);
		Thread startsUEService = new Thread() {
			public void run() {
				startService(sintent);
			};
		};

		startsUEService.start();

		finish();
	}

	/**
	 * @param result
	 */
	private void save_time(SgaTestResult result) {

		save("time from " + result.getStaringTime() + "to "
				+ result.getEndingTime(), new File(Environment
				.getExternalStorageDirectory().getPath()
				+ getResources().getString(R.string.testResults)
				+ getResources().getString(R.string.okfile)));

	}

	/**
	 * save trace in a file
	 * 
	 * @param name
	 */
	private static void save(String value, File file) {

		FileWriter write;
		try {
			write = new FileWriter(file.getAbsoluteFile());
			BufferedWriter out = new BufferedWriter(write);
			out.write(value);
			out.write("  ");
			out.close();
			if (ConfigApp.DEBUG) {
				Log.i(TAG, "save ");
				Log.i("[Writting Ok file]", "OK");
			}

		} catch (IOException e1) {
			e1.printStackTrace();
			if (ConfigApp.DEBUG) {
				Log.e(TAG, "save ");
				Log.e("[Writting ok file]", "fail");
			}

		}

	}

}
