package fr.openium.sgdInstrumentationTestRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.test.AndroidTestRunner;
import android.test.InstrumentationTestRunner;
import android.test.TestSuiteProvider;
import android.util.Log;

public class SgdInstrumentationTestRunner extends InstrumentationTestRunner implements TestSuiteProvider {

	public static long CURRENT_TIME;
	private boolean mSgdCoverage;
	@SuppressLint("SdCardPath")
	private static String COVERAGE_RESULTS = Environment.getExternalStorageDirectory() + File.separator;
	private static String SCENARIO_DIRECTORY = "scenario";
	private static String COVERAGE_DIRECTORY = "coverage";
	private static String COVERAGE_DEFAULT_PATH = COVERAGE_RESULTS + COVERAGE_DIRECTORY;
	private static String SCENARIO_DIRECTORY_DEFAULT_PATH = COVERAGE_RESULTS + File.separator
			+ SCENARIO_DIRECTORY;

	private static String TAG = "SGD_INSTRUMENTATION";
	private static String TIME = "time";
	private String mSgdCoverageFilePath;
	private Timer coverageTimer;
	public static final int COVERAGESYNCTIME = 10000;

	/**
	 * exception handling
	 */
	@Override
	public boolean onException(Object obj, Throwable e) {

		return true;
	}

	@Override
	public void onCreate(final Bundle arguments) {
		Log.d(TAG, "SGD Test Runner arguments: " + arguments.keySet());
		if (arguments != null) {
			mSgdCoverage = getBooleanArgument(arguments, "coverage", true);
			mSgdCoverageFilePath = getTargetContext().getFilesDir().getAbsolutePath() + File.separator
					+ "coverage.ec";
			if (ConfigApp.DEBUG) {
				Log.d(TAG, "Path: " + mSgdCoverageFilePath);
			}

		}
		init();
		super.onCreate(arguments);
	}

	private void init() {
		File coverageDirectory = new File(COVERAGE_DEFAULT_PATH);
		if (!coverageDirectory.exists()) {
			if (!coverageDirectory.mkdirs()) {
				throw new Error("Coverage Directory :" + COVERAGE_DEFAULT_PATH + " does not exist");
			}
		}
		File scenarioDirectory = new File(SCENARIO_DIRECTORY_DEFAULT_PATH);
		if (!scenarioDirectory.exists()) {
			scenarioDirectory.mkdirs();
		}
		// set a task that save coverage every 5 seconde
		TimerTask CoverageUpdateTimeTask = new TimerTask() {
			@Override
			public void run() {
				if (ConfigApp.DEBUG) {
					Log.e(TAG, "begin writing ");
				}
				writeCoverageIn(COVERAGE_DEFAULT_PATH);
				if (ConfigApp.DEBUG) {
					Log.e(TAG, "endWriting ");
				}
			}
		};
		if (mSgdCoverage) {
			if (ConfigApp.DEBUG) {
				Log.d(TAG, "init ");
				Log.d(TAG, "mSCoverage is true");
			}
			coverageTimer = new Timer();
			coverageTimer.schedule(CoverageUpdateTimeTask, 0, COVERAGESYNCTIME);
		} else {
			Log.d(TAG, "init ");
			Log.d(TAG, "mSCoverage is false");
		}
	}

	/**
	 * @param src
	 *            : - value to insert
	 * 
	 * @param dest
	 *            : destination file
	 */
	public void write(String src, File dst) {
		if (src == null) {
			return;
		}
		OutputStream out = null;
		try {
			if (!dst.exists()) {
				dst.createNewFile();
			}
			out = new FileOutputStream(dst);
			byte[] buf = src.getBytes();
			out.write(buf);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean getBooleanArgument(final Bundle arguments, final String tag, final boolean defaultValue) {
		final String tagString = arguments.getString(tag);
		Log.i(TAG, "Argument path: " + tagString);
		if (tagString == null) {
			return defaultValue;
		}
		return Boolean.parseBoolean(tagString);
	}

	public static void writeCoverageIn(String file) {
		String genericName = "mCoverage";
		int i = 0;
		if (ConfigApp.DEBUG) {
			Log.e(TAG, "writeCoverageIn ");
		}
		boolean name_available = false;
		String coverageFormat = file + File.separator + genericName + "%d.ec";
		String coverageFilePath = String.format(coverageFormat, i);
		do {
			if (!(new File(coverageFilePath)).exists()) {
				name_available = true;
			} else {
				i++;
				coverageFilePath = String.format(coverageFormat, i);
			}
			if (ConfigApp.DEBUG) {
				Log.e(TAG, "coveragePath: " + i + " " + coverageFilePath);
			}
		} while (!name_available);
		if (ConfigApp.DEBUG) {
			Log.e(TAG, "writeCoverageIn: " + coverageFilePath);
		}
		/**
		 * call emma by reflection
		 */
		java.io.File coverageFile = new java.io.File(coverageFilePath);
		try {
			Class emmaRTClass = Class.forName("com.vladium.emma.rt.RT");
			Method dumpCoverageMethod = emmaRTClass.getMethod("dumpCoverageData", coverageFile.getClass(),
					boolean.class, boolean.class);
			// dumpCoverageMethod.invoke(null, coverageFile, true, true);
			dumpCoverageMethod.invoke(null, coverageFile, true, false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			EmmaReport(e);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			EmmaReport(e);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			EmmaReport(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			EmmaReport(e);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			EmmaReport(e);
		}
	}

	private static void EmmaReport(Exception e) {
		e.printStackTrace();
	}

	@Override
	public ClassLoader getLoader() {
		return SgdInstrumentationTestRunner.class.getClassLoader();
	}

	public class CoverageTestListner implements TestListener {
		@Override
		public void startTest(Test test) {
			CURRENT_TIME = System.currentTimeMillis();
			Log.e(TAG, "Get coverage begin of test");
			if (mSgdCoverage) {
				writeCoverageIn(COVERAGE_DEFAULT_PATH);
				Log.e(TAG, "Copy existing");
			}
		}

		@Override
		public void addError(Test test, Throwable t) {
			if (mSgdCoverage) {
				writeCoverageIn(COVERAGE_DEFAULT_PATH);
			}
		}

		@Override
		public void addFailure(Test test, AssertionFailedError t) {
			if (mSgdCoverage) {
				writeCoverageIn(COVERAGE_DEFAULT_PATH);
			}
			coverageTimer.cancel();
		}

		@Override
		public void endTest(Test test) {
			Log.e(TAG, "Get coverage end of test");
			if (mSgdCoverage) {
				writeCoverageIn(COVERAGE_DEFAULT_PATH);
			}
			write("" + (System.currentTimeMillis() - CURRENT_TIME), new File(COVERAGE_RESULTS
					+ File.separator + "testResults" + File.separator + TIME));
			coverageTimer.cancel();
		}

	}

	private AndroidTestRunner mRunner;

	@Override
	protected AndroidTestRunner getAndroidTestRunner() {
		mRunner = super.getAndroidTestRunner();
		mRunner.addTestListener(new CoverageTestListner());
		return mRunner;
	}

	@Override
	protected void finalize() throws Throwable {
		coverageTimer.cancel();
		super.finalize();
	}
}
