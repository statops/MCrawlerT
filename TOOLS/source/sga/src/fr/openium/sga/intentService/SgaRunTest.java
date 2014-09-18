package fr.openium.sga.intentService;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import kit.Config.Config;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.util.Log;
import fr.openium.sga.R;
import fr.openium.sga.SgaTestResult;

public class SgaRunTest extends IntentService {
	public static final String NAME = "SgaRunTest";
	private boolean stop_if_Error = true;

	public SgaRunTest() {
		super(NAME);
		Log.i(TAG, NAME);
	}

	@SuppressLint("SimpleDateFormat")
	public String getTime() {
		SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return mDateFormat.format(new Date());
	}

	private static Process mExec;
	private final String TAG = "SgaRuntest";
	private String mStdOut = "null";

	/***
	 * Launch all package
	 * 
	 * @param testName
	 * @param testPackage
	 * @return
	 */
	public String launchTestPackage(String testPackage) {
		if (Config.DEBUG) {
			Log.d(TAG, "launchTestPackage ");
		}
		String commandLine = getResources().getString(R.string.coveragecommand) + " -w " + testPackage + "/"
				+ getResources().getString(R.string.sgdInstrumentation);
		return exec(commandLine);
	}

	private Intent mIntent;
	private ScenarioData mPath_of_current_job;
	private ScenarioData mResulting_Tree_of_current_job;
	protected static final String SCPATH = Environment.getExternalStorageDirectory() + Config.TESTRESULTS;
	protected static final String SCENARIOPREVIOUSPATH = Environment.getExternalStorageDirectory()
			+ Config.TESTRESULTS + Config.OutXMLPath;

	protected static final String TESTRESULTS = Environment.getExternalStorageDirectory()
			+ Config.TESTRESULTS;

	protected static final String TEMP = Environment.getExternalStorageDirectory() + Config.TESTRESULTS
			+ File.separator + "temp";

	protected String crash_file = Environment.getExternalStorageDirectory() + Config.TESTRESULTS
			+ File.separator + Config.CRASH_FILE;
	protected String errorLog = Environment.getExternalStorageDirectory() + Config.TESTRESULTS
			+ File.separator + Config.error;

	@Override
	protected void onHandleIntent(Intent intent) {
		SgaTestResult tr = new SgaTestResult(getTime());
		mIntent = intent;
		String package_variable = mIntent.getStringExtra("package");
		String classe_variable = mIntent.getStringExtra("class");
		String stop_intent_variable = mIntent.getStringExtra("stopError");
		Log.i(TAG + " stop_intent_variable ", "" + stop_intent_variable);
		Log.i(TAG + " classe_variable ", "" + classe_variable);
		Log.i(TAG + " package_variable ", "" + package_variable);

		if (stop_intent_variable != null) {
			stop_if_Error = Boolean.parseBoolean(stop_intent_variable);
		}

		String out = "";
		File path = new File(SCENARIOPREVIOUSPATH);
		if (path.exists()) {
			try {
				mPath_of_current_job = ScenarioParser.parse(path);
			} catch (Exception parseExc) {
				Log.i("[Info]", "problem in parsing \n " + parseExc.getMessage());
				endService(tr, out, false);

			}
		}

		do {
			try {

				if (classe_variable == null)
					out = launchTestPackage(package_variable);
				else {
					out = launchTestClass(classe_variable, package_variable);
				}

			} catch (Exception e) {
				Log.i("[Info]", "Test failed");
				Log.i("[Info]", "Exceptions " + e.getMessage());

			}
			if (Config.DEBUG) {
				Log.d(TAG, "Out : " + out);
			}
		} while (!analyze(out));
		endService(tr, out, true);
	}

	/**
	 * 
	 */
	private void endService(SgaTestResult tr, String out, Boolean indice) {
		if (indice) {
			save_scenario();
			delete_crash_file();
		}
		ResultReceiver rsul = mIntent.getParcelableExtra("receiver");
		Bundle b = new Bundle();
		tr.setValue(out);
		tr.setEndingTime(getTime());
		b.putSerializable("result", tr);
		rsul.send(1, b);
		clear();
		Log.i("[Info]", "Service kill itsself ");
		stopSelf();
	}

	/**
	 * 
	 */
	private void clear() {
		File ue_notify = new File(Environment.getExternalStorageDirectory() + File.separator + Scenario.UE);
		Log.e(TAG, "ue_notify " + ue_notify.toString());
		if (ue_notify.exists()) {
			ue_notify.delete();
		}

	}

	/**
	 * @param classe
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private String launchTestClass(String classe, String testPackage) throws InterruptedException,
			IOException {
		/**
		 * delete errorLog
		 */
		// deleteError();
		if (Config.DEBUG) {
			Log.d(TAG, "launchTestClass ");
		}
		String out = "";
		// String commandLine =
		// getResources().getString(R.string.coveragecommand) + " -w -e class "
		// + classe
		// + " " + testPackage + "/" +
		// getResources().getString(R.string.sgdInstrumentation);
		// ajouter un condition d'arr�t
		File indicator = new File(TEMP);
		do {
			Thread.sleep(1000);
			if (Config.DEBUG) {
				Log.d(TAG, "waiting test results");
			}
		} while (!indicator.exists());
		if (Config.DEBUG) {
			Log.d(TAG, "test Finished");
		}
		out = FileUtils.readFileToString(indicator);
		indicator.delete();
		return out;
	}

	/**
	 * delete crash file
	 */
	private void delete_crash_file() {
		boolean status = false;
		if (new File(crash_file).exists()) {
			status = new File(crash_file).delete();
		} else {
			return;
		}
		if (status) {
			if (Config.DEBUG) {
				Log.d(TAG, "delete_crash_file DONE");
			}
		} else {
			if (Config.DEBUG) {
				Log.d(TAG, "delete_crash_file FAILED");
			}

		}
	}

	/**
	 * 
	 */
	private void save_scenario() {
		if (Config.DEBUG) {
			Log.d(TAG, "save_scenario ");
		}
		if (mResulting_Tree_of_current_job == null) {
			return;
		}
		/**
		 * generer l'arbre temporaire
		 */
		try {
			new ScenarioGenerator(Environment.getExternalStorageDirectory().getPath()
					+ getResources().getString(R.string.testResults) + getResources().getString(R.string.out))
					.generateXml(mResulting_Tree_of_current_job);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean analyze(String out) {
		/**
		 * delete error log if exist
		 */
		/**
		 * if error
		 */
		if (!SgUtils.report_not_contain_crash_(out)) {
			if (Config.DEBUG) {
				Log.e(TAG, "contains error ");
			}

			/**
			 * read scenario and complete with error
			 */
			join_scenarii();

			if (mResulting_Tree_of_current_job == null) {
				if (Config.DEBUG) {
					Log.e(TAG, "Please check if application can access to sdcard");
				}
				addErrorLog(out);
				return true;
			}
			addErrors(out);
			// generate the path and list of red widget (crash.xml) file
			System.out.println("Generate path file "
					+ new ScenarioGenerator(SCENARIOPREVIOUSPATH).generateXml(mPath_of_current_job));
			/**
			 * s'il faut s'arr�ter s'il y a une erreur renvoyer lee contraire
			 * pour arr�ter le relancement apr�s crash
			 */
			if (stop_if_Error) {
				return true;
			}
			/**
			 * si continuer meme cas de crash
			 */
			if (Config.DEBUG) {
				System.out.println("crash file :" + crash_file);
				System.out.println("Generate crash file "
						+ new ScenarioGenerator(crash_file).generateXml(mResulting_Tree_of_current_job));
				System.out.println("Generate crash TEMP file "
						+ new ScenarioGenerator(crash_file + ".temp")
								.generateXml(mResulting_Tree_of_current_job));
			}
			return false;

			/**
			 * if no error
			 */
		} else {
			if (Config.DEBUG) {
				Log.e(TAG, "Does not contain error ");
			}
			join_scenarii();
			return true;
		}
	}

	/**
	 * 
	 */
	private void addErrors(String out) {
		addErrorLog(out);
		if (Config.DEBUG) {
			Log.d(TAG, "begin addError ");
		}
		mResulting_Tree_of_current_job = SgUtils.addError(mResulting_Tree_of_current_job, out);
		if (Config.DEBUG) {
			Log.d(TAG, "end addError ");
		}
	}

	private void addErrorLog(String out) {
		try {
			FileUtils.write(new File(errorLog), out, Config.UTF8, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Delete errorLog
	 */
	@SuppressWarnings({ "unused" })
	private void deleteError() {
		if (Config.DEBUG) {
			Log.d(TAG, "deleteError()  ");
		}
		try {
			FileUtils.forceDelete(new File(errorLog));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private void join_scenarii() {
		if (Config.DEBUG) {
			Log.d(TAG, "join_scenarii ");
		}
		if (mResulting_Tree_of_current_job == null) {
			Log.d(TAG, "mResulting_Tree_of_current_job is null");
			File out = new File(SCENARIOPREVIOUSPATH);
			if (!out.exists()) {
				Log.d(TAG, "out does not exist");
				return;
			}
			mResulting_Tree_of_current_job = ScenarioParser.parse(out);
			return;
		}

		Log.d(TAG, "mResulting_Tree_of_current_job is not null");

		ScenarioData temp = ScenarioParser.parse(new File(SCENARIOPREVIOUSPATH));
		if (temp != null) {
			try {
				mResulting_Tree_of_current_job.add_(temp, true);
				// mResulting_Tree_of_current_job = temp;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * launch command line
	 * 
	 * @param the
	 *            command line to execute
	 * 
	 * @return the Std output
	 * */
	public String exec(String commandLine) {
		StringBuilder stdOut = new StringBuilder();
		StringBuilder stdErr = new StringBuilder();
		try {
			mExec = Runtime.getRuntime().exec(commandLine);
			InputStreamReader r = new InputStreamReader(mExec.getInputStream());
			final char buf[] = new char[1024];
			int read = 0;
			while ((read = r.read(buf)) != -1) {
				if (stdOut != null)
					stdOut.append(buf, 0, read);
			}
			try {
				mExec.waitFor();
				if (mExec.exitValue() != 255) {
				} else {
				}

			} catch (InterruptedException ne) {
				ne.printStackTrace();
			}
			Log.e(TAG, "stdOut:  " + stdOut.toString());
			mStdOut = stdOut.toString();
			r = new InputStreamReader(mExec.getErrorStream());
			read = 0;
			while ((read = r.read(buf)) != -1) {
				if (stdErr != null)
					stdErr.append(buf, 0, read);
			}

			Log.e(TAG, "stdErr:  " + stdErr.toString());
			stdErr.toString();
		} catch (Exception ex) {
			if (stdErr != null)
				stdErr.append("\n" + ex);
		}

		return mStdOut;
	}

	@Override
	public void onDestroy() {
		Log.e(TAG, NAME + "destroyed");
		super.onDestroy();
	}

}
