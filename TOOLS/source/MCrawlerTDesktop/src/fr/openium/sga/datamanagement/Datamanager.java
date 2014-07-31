package fr.openium.sga.datamanagement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import kit.Scenario.ScenarioData;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;

/**
 * Singleton, that will handle the performed data test coverage
 * 
 * @author Stassia
 * 
 */
public class Datamanager implements Cloneable {

	public static Datamanager getInstance() {
		if (null == instance) {
			instance = new Datamanager();
		}
		return instance;
	}

	private Datamanager() {
	}

	private static Datamanager instance;

	/**
	 * Read coverage
	 * 
	 * @return
	 */
	public float coverage() {
		float left = getTestedData(testedRVFile).size() * 100;
		float right = getRV(allRVFile).size();
		float coverage = ((left) / right);
		return coverage;
	}

	private File allRVFile;

	/**
	 * Mandatory
	 * 
	 * set test Data
	 */
	public void setRVFile(File fi) {
		if (!fi.exists())
			throw new Error("test data does not exist: " + fi.getAbsolutePath());
		else
			allRVFile = fi;
	}

	private File testedRVFile;

	/**
	 * Mandatory
	 * 
	 * set Tested Data File
	 */
	/*
	 * if (fi.exists()) { fi.delete(); } try { fi.createNewFile(); } catch
	 * (IOException e) { throw new Error("tested data file is not created : " +
	 * fi.getPath() + e.getMessage()); }
	 */

	public void setTestedRVFile(File fi) {
		testedRVFile = fi;
	}

	/**
	 * Retrieve RV value
	 * 
	 * @param rv
	 *            : file where random value is stored
	 * @return
	 */
	private ArrayList<String> getRV(File rv) {
		try {
			return (ArrayList<String>) FileUtils.readLines(rv);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * @param rv_tested
	 *            file where tested random value is stored
	 * @return
	 */
	private ArrayList<String> getTestedData(File rv_tested) {
		return getRV(rv_tested);
	}

	/**
	 * Select an unused RV among allRV
	 * 
	 * @return
	 */

	public String get_A_rtestv() {
		ArrayList<String> all = new ArrayList<String>();
		all = getRV(allRVFile);
		ArrayList<String> done = new ArrayList<String>();
		done = getTestedData(testedRVFile);
		for (String value : all) {
			if (!done.contains(value)) {
				done.add(value);
				// saveTestedValue(value); //***** � ajoute ulterieurement quand
				// la mesure des donn�es utilis�e on �t� mis en place
				return value;
			}
		}
		return null;
	}

	public String get_A_rtestv(ArrayList<String> done) {

		for (String value : getRV(allRVFile)) {
			if (!done.contains(value)) {
				done.add(value);
				// saveTestedValue(value); //***** � enlever pour eviter les
				// repetitions
				return value;
			}
		}
		return null;
	}

	/**
	 * save into testedDatafile
	 * 
	 * @param done
	 */

	@SuppressWarnings("unused")
	private void saveTestedValue(String done) {
		/**
		 * add in last line
		 */
		try {
			FileUtils.write(testedRVFile, done, "UTF-8", true);
			FileUtils.write(testedRVFile, "\n", "UTF-8", true);
		} catch (IOException e) {
			throw new Error("tested value does not saved :" + e.getMessage());
		}

	}

	public long getDataCoverage(int numbeOfeditText, int numberOfRandomValue, int numberOfDoneRandomValue) {
		return (numberOfDoneRandomValue / (numberOfRandomValue ^ numbeOfeditText));
	}

	public String getTestDatafile() {
		return allRVFile.getPath();
	}

	/**
	 * get the data coverage from sequence of testedData and the Scenario
	 * editTextInformation.
	 * 
	 * @param outDirectory
	 */
	public long getTestDataCoverage(String outDirectory) {
		/**
		 * 
		 * TODO
		 */
		File scenarioDirectory = new File(outDirectory + ConfigApp.SCENARII);
		if (!scenarioDirectory.exists()) {
			return 0;
		}
		@SuppressWarnings("unused")
		ScenarioData scenario = readLastScenario(scenarioDirectory);
		return 0;
	}

	private ScenarioData readLastScenario(File scenarioDirectory) {

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Datamanager clone() throws CloneNotSupportedException {
		return (Datamanager) super.clone();
	}

}
