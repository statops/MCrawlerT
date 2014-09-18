package fr.openium.sga.strategy;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;

public abstract class AbstractStrategy {

	public final static int DFS_STRATEGY_ID = 0;
	public final static int FOURMY_STRATEGY_ID = 1;
	// public final static int DFS_BFS_STRATEGY_ID = 101;
	public final static int LOGGING_STRATEGY_ID = 12;
	public final static int CRASH_STRATEGY_ID = 13;
	protected static SgdEnvironnement mSgdEnvironnement;

	protected CrawlResult mResult;

	public AbstractStrategy(SgdEnvironnement arg) {
		mSgdEnvironnement = arg;
	}

	private static Logger mEmmaLogger = Logger.getLogger("EmmaLogger");

	public abstract int getRank(State st, ScenarioData path);

	public abstract CrawlResult getResult() throws Exception;

	protected static void info(String string) {
		if (ConfigApp.DEBUG) {
			mEmmaLogger.log(Level.INFO, string);
		}

	}

	protected static void errorLog(String string) {
		if (ConfigApp.DEBUG) {
			mEmmaLogger.log(Level.SEVERE, string);
		}
	}

	protected static void save(File file, File directory, String ext) {
		Utils.savegeneric_file(file, directory, ext);
	}

	protected static void save_ok(SgdEnvironnement envToCheck, File ok) {
		Utils.save_generic_ok(envToCheck, ok);

	}

	protected void save_time(SgdEnvironnement envToCheck) {
		Utils.save_genericTime(envToCheck);

	}

	protected void save_rv_done_directory(SgdEnvironnement envToCheck) {
		Utils.save_generic_rv_done(envToCheck);

	}

	/**
	 * pull scenario
	 * 
	 * @param path
	 * @throws InterruptedException
	 */
	protected void pull(String valueInTestResults, SgdEnvironnement env) {
		Utils.pull(valueInTestResults, env);
	}

	protected void pullScenario(SgdEnvironnement envToCheck) {
		File ScenarioDirectory = new File(envToCheck.getOutDirectory()
				+ ConfigApp.SCENARII);
		File outxml = new File(envToCheck.getOutDirectory()
				+ ConfigApp.OutXMLPath);
		pull(ConfigApp.OutXMLPath, envToCheck);
		save(outxml, ScenarioDirectory, ".xml");
	}

	protected void pullMobileSystemObservers(SgdEnvironnement envToCheck) {
		File sgdMobileSystemObserversDirectory = new File(
				envToCheck.getOutDirectory()
						+ ConfigApp.SGD_OBSERVATION_REPORT_DIRECTORY);
		if (sgdMobileSystemObserversDirectory.exists()) {
			sgdMobileSystemObserversDirectory.mkdirs();
		}
		pull(envToCheck, ConfigApp.SGD_OBSERVATION_REPORT_DIRECTORY,
				sgdMobileSystemObserversDirectory);
	}

	protected void pull(SgdEnvironnement envToCheck,
			String sgdObservationReportDirectory,
			File sgdMobileSystemObserversDirectory) {
		pull(sgdObservationReportDirectory, envToCheck);
		pull(sgdMobileSystemObserversDirectory.getPath(), envToCheck);
	}

}
