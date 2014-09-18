package fr.openium.sga.Utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import kit.Command.PullCommand;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.emmatest.SgdEnvironnement;

public class Utils {

	public static void pull(String valueInTestResults, SgdEnvironnement env) {
		pull(valueInTestResults, env, env.getOutDirectory());
	}

	/**
	 * 
	 * @param valueInTestResults
	 *            : path stored in ConfigApp.DEVICETESTRESULTS
	 * @param env
	 *            :
	 * @param outDirectory
	 *            : the destination directory
	 */
	public static void pull(String valueInTestResults, SgdEnvironnement env,
			String outDirectory) {
		pull(valueInTestResults, outDirectory, env.getAdb(), env.getDevice());
	}

	/**
	 * 
	 * @param valueInTestResults
	 * @param outDirectory
	 * @param adb
	 * @param device
	 */
	public static void pull(String valueInTestResults, String outDirectory,
			String adb, String device) {
		File out = new File(outDirectory);
		PullCommand pull = new PullCommand(adb, device,
				ConfigApp.DEVICETESTRESULTS + "/" + valueInTestResults,
				out.getPath());
		pull.execute();
		new ThreadSleeper().sleepMedium();
	}

	public static boolean limit_time_isReached(Long initTime, Long timLimit) {
		return (System.currentTimeMillis() - initTime) > timLimit;
	}

	public static synchronized void save_generic_ok(
			SgdEnvironnement envToCheck, File ok) {
		File okDirectory = new File(envToCheck.getOutDirectory()
				+ ConfigApp.OkDirectory);
		savegeneric_file(ok, okDirectory);

	}

	public static synchronized void save_genericTime(SgdEnvironnement envToCheck) {
		pull(ConfigApp.TIME, envToCheck);
		savegeneric_file(new File(envToCheck.getOutDirectory() + File.separator
				+ ConfigApp.TimeDirectory + File.separator + ConfigApp.TIME),
				new File(envToCheck.getOutDirectory() + File.separator
						+ ConfigApp.TimeDirectory));

	}

	public static void save_generic_rv_done(SgdEnvironnement envToCheck) {
		File rv_done_directory = new File(envToCheck.getAllTestDataPath())
				.getParentFile();
		File rv_done = new File(envToCheck.getOutDirectory() + ConfigApp.RVDONE);
		pull(ConfigApp.RVDONE, envToCheck);
		savegeneric_file(rv_done, rv_done_directory);

	}

	public static void savegeneric_file(File file, File directory) {
		savegeneric_file(file, directory, null);
	}

	/**
	 * @param file
	 * @param mTree_directory
	 * @param extension
	 */
	public static void savegeneric_file(File file, File directory, String ext) {
		if (!file.exists()) {
			info(" File does not exist " + file.getPath());
			return;
		}
		if (!directory.exists()) {
			if (ConfigApp.DEBUG) {
				System.out.println("savegeneric_file");
				System.out.println("create Directory : " + directory.getPath());

			}
			directory.mkdirs();
		}
		try {
			if (directory.listFiles().length != 0) {

				int i = 0;
				do {
					if (new File(directory + File.separator + file.getName()
							+ i + ((ext == null) ? "" : ext)).exists()) {
						i++;
					} else {
						if (file.renameTo(new File(file.getAbsolutePath() + i
								+ ((ext == null) ? "" : ext)))) {
							file = new File(file.getAbsolutePath() + i
									+ ((ext == null) ? "" : ext));
						}
						break;
					}
				} while (true);

			}
			FileUtils.copyFileToDirectory(file, directory, true);
		} catch (IOException e) {
			throw new Error(" [error]: File " + file.getName()
					+ " is not saved, " + e.getMessage());

		} finally {
			info(" File " + file.getName() + " is available in: "
					+ directory.getPath());
			file.delete();
		}
	}

	/**
	 * @param string
	 * @param outDirectory
	 * @param ext
	 */
	public static void savegeneric_file(String fileNme, String outDirectory,
			String ext) {
		savegeneric_file(new File(fileNme), new File(outDirectory), ext);

	}

	public static void save_non_generic(File file, File directory) {
		if (!file.exists()) {
			info(" File does not exist " + file.getPath());
			return;
		}
		if (!directory.exists()) {
			directory.mkdirs();
		}
		try {
			FileUtils.copyFileToDirectory(file, directory, true);
		} catch (IOException e) {
			throw new Error(" [error]: Scenario is not saved, "
					+ e.getMessage());

		} finally {
			info(" File " + file.getName() + " vailable in: "
					+ directory.getPath());
			file.delete();
		}
	}

	public static Logger mMainLogger = Logger.getLogger("EmmaLogger");

	public static void info(String string) {
		if (ConfigApp.DEBUG) {
			mMainLogger.log(Level.INFO, string);
		}

	}

	public static Logger getLogger() {
		return mMainLogger;
	}
}
