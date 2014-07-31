/**
 * 
 */
package fr.openium.sga.SecurityTesting.AbstractTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.strategy.IStrategy;

/**
 * @author Stassia
 * 
 */
public abstract class AbstractTaskManager implements IStrategy {
	protected static Long MAX_TIME = 1500000L;
	public static final int _STRATEGY_ID = 110;
	protected ScenarioData mTree;
	protected HashSet<AbstractTestTask> tasks = new HashSet<AbstractTestTask>();
	protected static ArrayList<SPReport> verdict = new ArrayList<SPReport>();
	protected ArrayList<String> mTestDataFile;
	protected SgdEnvironnement mSgdEnvironnement;
	protected int numberOfThread = 1;// par default
	protected static final String TAG = AbstractTaskManager.class.getName();
	protected static File report;

	protected abstract void setAbstracTask(HashSet<AbstractTestTask> tasks);

	/**
	 * how to read testData
	 * 
	 * @param testDataFile
	 * @return
	 */
	protected abstract ArrayList<String> read(File testDataFile);

	public abstract ArrayList<SPReport> launchSecurityTestJob()
			throws Exception;

	public abstract HashSet<AbstractTestTask> getGenerateTaskLists();

	protected AbstractTaskManager(ScenarioData model, File testFile,
			SgdEnvironnement env) {
		mSgdEnvironnement = env;
		mTree = model;
		mTestDataFile = read(testFile);
		mSgdEnvironnement.setDevice(null);
		try {
			mSgdEnvironnement.initOutDirectories();
		} catch (IOException e) {
			e.printStackTrace();
		}
		MAX_TIME = (mSgdEnvironnement.getMaxTime() == 0L) ? MAX_TIME
				: mSgdEnvironnement.getMaxTime();
		report = new File(mSgdEnvironnement.getOutDirectory() + File.separator
				+ "report");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.strategy.IStrategy#updateResult(java.lang.Object)
	 * result.
	 */
	@Override
	public abstract boolean updateResult(Object result);

	/**
	 * @return
	 */
	protected ScenarioData getInitTask() {
		ScenarioData init = SgUtils.get_init_Tree(mTree);
		return init;
	}

	public class SPReport {

		protected final State targetState;
		private final ScenarioData Path;
		private String ErrorMessage;
		private String Verdict;
		private ArrayList<String> ExecutedEvents;
		private Long code_coverage = 0L;

		/**
 * 
 */
		public SPReport(State state, ScenarioData path, String verdict,
				String message) {
			targetState = state;
			Path = path;
			ErrorMessage = message;
			Verdict = verdict;
		}

		public State getTargetState() {
			return targetState;
		}

		public ScenarioData getPath() {
			return Path;
		}

		public String getMessage() {
			return ErrorMessage;
		}

		public String getVerdict() {
			return Verdict;
		}

		@Override
		public String toString() {
			String path = "";

			for (Transition tr : (Path.getTransitions())) {
				HashMap<String, String> events = SgUtils.get_event(tr);
				for (String event : events.keySet()) {
					path = path + " : " + events.get(event);
				}

			}
			return "\n" + "Location :  " + targetState.toString() + "\n"
					+ "Location's Path : " + Path.toString() + "\n" + path
					+ "\n" + " verdict : " + getVerdict() + "\n"
					+ " Message : " + ErrorMessage + "\n\n\n";
		}

		public boolean isVulnerable() {
			return Boolean.parseBoolean(Verdict);
		}

		public void setErrorDescription(String out) {
			ErrorMessage = out;

		}

		public void setVerdict(String verdict) {
			Verdict = verdict;

		}

		public void setExecutedEvents(ArrayList<String> event_done) {
			ExecutedEvents = event_done;

		}

		public ArrayList<String> getExecutedEvents() {
			return ExecutedEvents;
		}

		public void setCoverage(Long currentCoverage) {
			setCode_coverage(currentCoverage);

		}

		public Long getCode_coverage() {
			return code_coverage;
		}

		public void setCode_coverage(Long code_coverage) {
			this.code_coverage = code_coverage;
		}

		private ScenarioData testedTransition;
		private ArrayList<Transition> vulnerableTransitions;

		public void setTestedSuccessor(ScenarioData successor) {
			setTestedTransition(successor);

		}

		public void setVulnerableTransition(ArrayList<Transition> tr) {
			vulnerableTransitions = tr;

		}

		public ArrayList<Transition> getVulnerableTransition() {
			return vulnerableTransitions;

		}

		public ScenarioData getTestedTransition() {
			return testedTransition;
		}

		public void setTestedTransition(ScenarioData testedTransition) {
			this.testedTransition = testedTransition;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */

	/**
	 * @return
	 */
	public abstract Object getTasks();

	/**
	 * @return
	 */
	public ArrayList<SPReport> getVerdict() {
		return verdict;
	}

	/**
	 * @param targetState
	 * @return
	 */
	protected boolean isInTasks(State targetState) {
		Iterator<AbstractTestTask> itTask = tasks.iterator();
		if (!itTask.hasNext()) {
			return false;
		}
		do {
			AbstractTestTask task = itTask.next();
			if (SgUtils.isEqualState(targetState, task.getTargetState()))
				return true;
		} while (itTask.hasNext());
		return false;
	}

	/**
	 * @param targetState
	 * @return
	 */
	protected boolean hasNoEditText(State targetState) {
		return !SgUtils.hasEditText(targetState);
	}

	protected void saveReport(String out) throws IOException {

		for (SPReport rep : getVerdict()) {
			if (ConfigApp.DEBUG) {
				System.out.println(rep.toString());
			}
		}

		/**
		 * save report in a file
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("SPReport available at " + out);
		}
		for (SPReport report : getVerdict()) {
			saveReport(out, report);
		}

	}

	/**
	 * may be parallelel access
	 * 
	 * @param out
	 * @param repor
	 * @throws IOException
	 */

	protected synchronized static void saveReport(String out, SPReport repor)
			throws IOException {
		FileUtils.write(new File(out), repor.toString(), true);

	}

}
