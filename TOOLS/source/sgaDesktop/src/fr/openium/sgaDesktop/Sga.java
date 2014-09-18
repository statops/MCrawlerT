/**
 * 
 */
package fr.openium.sgaDesktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;

import kit.Command.AntManager;
import kit.Command.PullCommand;
import kit.Command.PushCommand;
import kit.Config.Config;

import org.apache.commons.io.FileUtils;

/**
 * @author Stassia
 * 
 */
public class Sga extends Observable {

	private static final String TAG = Sga.class.getName();
	private String mTestClassName;
	private String mStopIfError;
	private String mPackage;
	private int nTimes;
	private String deviceSdcard;
	private String mEmulator;
	private final String mOutFile;

	private final static String COVERAGECOMMAND = " shell am instrument -e coverage true";
	private final static String INSTRUMENTATION = "fr.openium.sgdInstrumentationTestRunner.SgdInstrumentationTestRunner";
	public static final String SGA_PACKAGE = "fr.openium.sga";
	private String mAdb;

	public Sga(String testClassName, String stopIfError, String _package,
			int times, String sdcard, String emulator, String threadID,
			String adb) {
		mTestClassName = testClassName;
		mStopIfError = stopIfError;
		mPackage = _package;
		nTimes = times;
		deviceSdcard = sdcard;
		mEmulator = emulator;
		mOutFile = threadID;
		mAdb = adb;
	}

	public Sga(String[] args, String threadID, String sdcard) throws Exception {

		deviceSdcard = sdcard;
		mOutFile = threadID;

		if (args.length > 5) {
			for (int i = 0; i < args.length - 1;) {
				if (ConfigApp.DEBUG) {
					System.out.print("launchTestOnDevice : ");
					System.out.println(args[i]);
				}
				if (args[i].equals("-n")) {
					nTimes = Integer.parseInt(args[i + 1]);
				}
				if (args[i].equals("-tp")) {
					mPackage = args[i + 1];
				}
				if (args[i].equals("-adb")) {
					mAdb = args[i + 1];
				}

				if (args[i].equals("-emu")) {
					mEmulator = args[i + 1];
				}

				if (args[i].equals("-class")) {
					mTestClassName = args[i + 1];
				}

				if (args[i].equals("-stopError")) {
					mStopIfError = args[i + 1];
				}
				i++;
			}

		} else
			throw new IllegalStateException("Invaild parameters for Sga");

	}

	/**
	 * @param string
	 * @return
	 * @throws InterruptedException
	 */

	public String launchTest() throws IOException, InterruptedException {
		if (deviceSdcard.equalsIgnoreCase("")) {
			deviceSdcard = Config.DEVICETESTRESULTS;
		}

		if (ConfigApp.DEBUG) {
			System.out.println("launchTest");
			System.out.println("deviceSdcard :" + deviceSdcard);
			System.out.println("start sga on Device");
		}
		// lancer Sga normalement
		startRemoteTester();
		// lancer le test et envoyer la reponse a l'intentService
		String out = "";

		do {
			out = launchInstrument(out);
			// push
			pushTempFile(out);
			/**
			 * check if crash file exist before pushing testResults
			 */
		} while (pullCrashFile(out));

		if (ConfigApp.DEBUG) {
			System.out.println("test on Device finished");
		}
		/**
		 * broadcast to receiver
		 */
		notifyObservers();
		return out;
	}

	/**
	 * @throws IOException
	 * 
	 */
	private String launchInstrument(String out) throws IOException {
		if (ConfigApp.DEBUG) {
			System.out.println("launchInstrument");
		}
		String commandLine = mAdb + " -s " + mEmulator + COVERAGECOMMAND
				+ " -w -e class " + mTestClassName + " " + mPackage + "/"
				+ INSTRUMENTATION;

		if (ConfigApp.DEBUG) {
			System.out.println("start test on Device");
			System.out.println(commandLine);
		}
		if (ConfigApp.DEBUG) {
			System.out.println(commandLine);
		}

		out = exec(commandLine);
		// pusher la reponse
		// creer un fichier temporaire puis supprimer
		if (ConfigApp.DEBUG) {
			System.out.println("test Result is Received \n");
			System.out.println(":" + out);
		}

		return out;

	}

	/**
	 * @throws IOException
	 * 
	 */
	private void pushTempFile(String out) throws IOException {

		File toPush = new File(mOutFile + File.separator + "temp");

		if (ConfigApp.DEBUG) {
			System.out.println("start push : " + toPush.toString());
		}
		if (ConfigApp.DEBUG) {
			System.out.println("Out :" + out);
		}
		if (ConfigApp.DEBUG) {
			System.out.println("create temp file : " + toPush.createNewFile());
		}

		FileUtils.write(toPush, out, Config.UTF8, false);
		PushCommand com = new PushCommand(mAdb, mEmulator, toPush.getPath(),
				deviceSdcard);
		boolean val = com.execute();
		// Thread.sleep(5000);
		// toPush.delete();
		if (ConfigApp.DEBUG) {
			System.out.println("end push : " + val);
		}

		// Wait
	}

	/**
	 * check if there is an error
	 */
	public boolean pullCrashFile(String out) {
		/**
		 * analyser la sortie et attendre que si et seulement si il y a une
		 * erreur
		 */
		boolean crashIndicator = false;
		boolean stopError = Boolean.parseBoolean(mStopIfError);
		if ((mStopIfError != null && (!stopError))
				&& !report_not_contain_crash_(out)) {
			/**
			 * wait crash file
			 */
			if (Config.DEBUG) {
				if (ConfigApp.DEBUG) {
					System.out.println("contains error");
				}
				int n = 0;
				do {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					crashIndicator = crashCheck();
					n++;
					if (n > 5) {
						break;
					}
					if (ConfigApp.DEBUG) {
						System.out.println("Waiting crash file for :"
								+ (n * 5000) + "  secondes");
					}
				} while (!crashIndicator);
			}
		}
		return crashIndicator;
	}

	private final static String Excp = "Exception";
	private final static String Crash = "Crash";
	private final static String Crashed = "crashed";
	private final static String Process_Crash = "Process crashed";

	/***
	 * handle output error during test
	 * 
	 * @param out
	 * @return
	 */
	public static boolean report_not_contain_crash_(String out) {
		if (out.contains(Excp) || out.contains(Crash)
				|| out.contains(Process_Crash) || out.contains(Crashed)) {
			return false;
		} else
			return true;
	}

	/**
	 * @return
	 */
	private boolean crashCheck() {
		if (ConfigApp.DEBUG) {
			System.out.println("pullCrashFile(): ");
		}
		String fileToPull = deviceSdcard + "/" + Config.CRASH_FILE;
		String toPull = (mOutFile);
		if (ConfigApp.DEBUG) {
			System.out.println("from " + fileToPull + " to " + toPull);
		}
		PullCommand pull = new PullCommand(mAdb, mEmulator, fileToPull, toPull);
		pull.execute();
		File CrashFile = new File(mOutFile + File.separator + Config.CRASH_FILE);
		boolean exist = (CrashFile).exists();
		if (exist) {
			(CrashFile).delete();
			if (ConfigApp.DEBUG) {
				System.out.println("crash file exist");
			}
		} else {
			if (ConfigApp.DEBUG) {
				System.out.println("crash file does not exist ");
			}
		}
		return exist;
	}

	private static Process mExec;
	private String mStdOut = "null";

	public String exec(String commandLine) throws IOException {
		StringBuilder stdOut = new StringBuilder();
		StringBuilder stdErr = new StringBuilder();

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

		mStdOut = stdOut.toString();

		r = new InputStreamReader(mExec.getErrorStream());
		read = 0;
		while ((read = r.read(buf)) != -1) {
			if (stdErr != null)
				stdErr.append(buf, 0, read);
		}
		if (stdErr.length() != 0) {
			mStdOut = stdErr.toString();
			throw new Error(mStdOut);
		}

		return mStdOut;
	}

	/**
	 * launch sga
	 * 
	 * @throws InterruptedException
	 */
	private void startRemoteTester() throws InterruptedException {
		AntManager ant = new AntManager();
		String command = mAdb
				+ " -s "
				+ mEmulator
				+ " shell am start -a "
				+ ConfigApp.ACTION
				+ " -e package "
				+ mPackage
				+ (mTestClassName != null ? (" -e class " + mTestClassName)
						: "")
				+ (mStopIfError != null ? (" -e stopError " + mStopIfError)
						: "") + " -e n " + nTimes + " -n "
				+ ConfigApp.ACTIVITYLAUNCHER;
		System.out.print("Command: " + command);
		ant.exec(command);
		if (ant.getStdErr() != null) {
			throw new Error(ant.getStdErr());
		}
		Thread.sleep(5000);
	}

}
