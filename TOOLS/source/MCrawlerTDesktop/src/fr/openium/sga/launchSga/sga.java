package fr.openium.sga.launchSga;

import fr.openium.automaticOperation.AntManager;
import fr.openium.sga.ConfigApp;

public class sga {

	private static String mTestProjectPackage;
	private static int nTimes;
	private static String mAdbPath;
	private static String mEmu;
	private static String mClassTest;
	private static String mStop_if_Error_Indication;
	public static final String SGA_PACKAGE = "fr.openium.sga";

	/**
	 * @param args
	 *            -n number of occurence
	 * 
	 *            -tp the test project
	 * 
	 *            -adb adb path
	 * 
	 */
	public synchronized static void _launchTestOnDevice(String[] args) {
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
					mTestProjectPackage = args[i + 1];
				}
				if (args[i].equals("-adb")) {
					mAdbPath = args[i + 1];
				}

				if (args[i].equals("-emu")) {
					mEmu = args[i + 1];
				}

				if (args[i].equals("-class")) {
					mClassTest = args[i + 1];
				}

				if (args[i].equals("-stopError")) {
					mStop_if_Error_Indication = args[i + 1];
				}

				i++;
				// i++;
			}

		} else
			return;
		startRemoteTester();
	}

	/**
	 * Launch test With sga
	 * 
	 * "adb shell am start -a ACTION -e package mTestProjectPackage -e n nTimes -n ACTIVITYLAUNCHER"
	 * 
	 * @throws Exception
	 */
	private synchronized static void startRemoteTester() {
		/**
		 * thread is better
		 * 
		 */
		new Thread("service launcher") {
			@Override
			public void run() {
				AntManager ant = new AntManager();
				String command = mAdbPath
						+ " -s "
						+ mEmu
						+ " shell am start -a "
						+ ConfigApp.ACTION
						+ " -e package "
						+ mTestProjectPackage
						+ (mClassTest != null ? (" -e class " + mClassTest) : "")
						+ (mStop_if_Error_Indication != null ? (" -e stopError " + mStop_if_Error_Indication)
								: "") + " -e n " + nTimes + " -n " + ConfigApp.ACTIVITYLAUNCHER;
				System.out.print("Command:" + command);
				ant.exec(command);
				if (ant.getStdErr() != null) {
					throw new Error(ant.getStdErr());
				}
			}
		}.start();
	}

	public static String getPackageName() {
		return ConfigApp.SGA;
	}

}
