/**
 * 
 */
package fr.openium.sga.SecurityTesting.StressTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import kit.RandomValue.RandomValue;
import kit.RandomValue.RandomValueData;
import kit.RandomValue.RandomValueParser;
import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.reporter.StressTestReporter;
import fr.openium.taskPool.EmulatorManager;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskComparator;
import fr.openium.taskPool.TaskManagerRunnable;

/**
 * @author Stassia
 * 
 *         prend un model et effectue du stress testing sur chaque etat
 * 
 */
public class StressTaskManager extends AbstractTaskManager {
	@SuppressWarnings("unused")
	private static final String TAG = StressTaskManager.class.getName();
	public static final int _STRATEGY_ID = 100;
	private ArrayList<String> stressTestData;

	private int numberOfThread = 1;// par default

	public static void main(String[] args) throws Exception {
		SgdEnvironnement env = Emma.init_environment_(args);
		StressTaskManager stress = new StressTaskManager(env.getModel(),
				new File(env.getAllTestDataPath()), env);
		stress.launchSecurityTestJob();
		if (ConfigApp.DEBUG) {
			System.out.println("Stress Report: ");
		}
		for (SPReport rep : stress.getVerdict()) {
			if (ConfigApp.DEBUG) {
				System.out.println(rep.toString());
			}
		}
		StressTestReporter readableReport = new StressTestReporter(new File(
				stress.getOutputDirectory() + File.separator
						+ StressTestReporter.STRESSREPORTFILE),
				stress.getVerdict());
		readableReport.generate();
	}

	private String getOutputDirectory() {

		return mSgdEnvironnement.getOutDirectory();
	}

	public StressTaskManager(ScenarioData model, File testDataFile,
			SgdEnvironnement env) {
		super(model, testDataFile, env);
		stressTestData = read(testDataFile);
		getGenerateTaskLists();
	}

	/**
	 * @param testDataFile
	 * @return
	 */
	@Override
	protected ArrayList<String> read(File testDataFile) {
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(testDataFile);
		stressTestData = SgUtils.getTestDataSet(data, RandomValue.STRESS);
		return stressTestData;
	}

	/**
	 * generate state to stress
	 */

	@Override
	public HashSet<AbstractTestTask> getGenerateTaskLists() {
		/**
		 * cas initial
		 */
		ScenarioData init_path = getInitTask();
		State initState = init_path.getInitialState();
		if (initState != null) {
			tasks.add(new StressTask(init_path.getInitialState(), init_path,
					stressTestData));
		} else
			return null;
		/**
		 * other cases
		 */
		for (State targetState : mTree.getStates()) {

			if (targetState.isInit()) {
				continue;
			}

			if (!targetState.isDest()) {
				continue;
			}

			if (targetState.isFinal()) {
				continue;
			}

			/**
			 * d�j� ajout� dans tasks
			 */

			if (isInTasks(targetState)) {
				continue;
			}
			/**
			 * � ajouter le cas sans exception
			 */
			if (hasSuccessorCrash(targetState)) {
				continue;
			}

			try {
				// ScenarioData path = SgUtils._getPath(targetState, mTree);
				ScenarioData path = SgUtils.getConcretePath(targetState, mTree);
				tasks.add(new StressTask(targetState, path, stressTestData));
				System.out.println("add task : " + targetState.toString());
				System.out.println("path task : " + path.toString());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				continue;
			}
		}
		return tasks;
	}

	/**
	 * @param targetState
	 * @return
	 */
	private boolean hasSuccessorCrash(State targetState) {
		// get list_of_transition derived from targetState
		ArrayList<Transition> out_transition = SgUtils.get_out_transitions(
				targetState, mTree);
		for (Transition tr : out_transition) {
			if (SgUtils.contains_error(tr.getAction())) {
				return true;
			}
		}
		return false;
	}

	protected ITaskManager<StressTestJob> tm;

	public ArrayList<SPReport> launchSecurityTestJob() throws Exception {

		EmulatorManager emManager = new EmulatorManager(
				mSgdEnvironnement.getAdb(), null,
				EmulatorManager.GET_AVAILABLE_EMULATEUR_LIST, null);
		for (String emulator : emManager.getList_Available_Emulator()) {
			if (ConfigApp.DEBUG) {
				System.out.println("emulator" + emulator);
			}
			mSgdEnvironnement.setDevice(emulator);
			if (!ConfigApp.ISTEST) {
				mSgdEnvironnement.installSga();
				mSgdEnvironnement.instrument_with_Emma_and_install();
			}
		}
		mSgdEnvironnement.setDevice(null);

		numberOfThread = mSgdEnvironnement.getThread_number();
		tm = new TaskManagerRunnable<StressTestJob>(get_Stress_Jobs(),
				numberOfThread, new TaskComparator<StressTestJob>(),
				mSgdEnvironnement.getAdb());
		Long initTime = System.currentTimeMillis();
		tm.execute();
		int n = 1;
		boolean taskState = false;
		do {
			taskState = ((TaskManagerRunnable<StressTestJob>) tm)
					.isAllResultReceived();
			try {
				Thread.sleep(5000);
				n = n + 5;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Emma.limit_time_isReached(initTime, MAX_TIME)) {
				System.out.println("Time " + MAX_TIME + " seconds is reached");
				tm.stop();
				break;
			}
			if ((verdict.size() == tasks.size())) {
				break;
			}
		} while (!taskState);
		System.out.println("Stress Test must normally be finished after" + n
				+ " seconds");
		System.out.println("Waiting for the end of all  jobs");

		do {
			taskState = ((TaskManagerRunnable<StressTestJob>) tm)
					.isTerminated();
			try {
				Thread.sleep(1000);
				n = n + 1;
				System.out.print(".");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!taskState);

		/**
		 * est vulnerable si contient true
		 */

		return verdict;

	}

	private ArrayList<StressTestJob> get_Stress_Jobs() {
		ArrayList<StressTestJob> jobs = new ArrayList<StressTestJob>();
		for (AbstractTestTask task : tasks) {
			try {
				jobs.add(new StressTestJob(task, tm, mSgdEnvironnement.clone(),
						this));
			} catch (CloneNotSupportedException e) {

				if (ConfigApp.DEBUG) {
					System.out.println("Job is not added due to: "
							+ e.getMessage());
				}
			}
		}
		System.out.println("List of job: " + jobs.size());
		return jobs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.strategy.IStrategy#updateResult(java.lang.Object)
	 * result.
	 */
	@Override
	public synchronized boolean updateResult(Object result) {
		if (result == null) {
			return true;
		}
		SPReport v = (SPReport) result;
		verdict.add(v);
		try {
			saveReport(report.getAbsolutePath(), v);
		} catch (IOException e) {
			System.err.println("report is not saved");
		} finally {
			checkJobStopCondition();
		}
		return Boolean.parseBoolean(v.getVerdict());
	}

	/**
	 * stip when tasks excution is finished
	 */

	protected void checkJobStopCondition() {

		if (ConfigApp.DEBUG) {
			System.out.println("checkJobStopCondition");

			System.out.println("tasks.size() :" + tasks.size());

			System.out.println("verdict.size() :" + verdict.size());
		}

		if (tasks.size() == verdict.size()) {
			// System.out.println("tm.stop()");
			tm.stop();
			System.out.println("tm.stop()");

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager#
	 * setAbstracTask(java.util.HashSet)
	 */
	@Override
	protected void setAbstracTask(HashSet<AbstractTestTask> tasks) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager#getTasks
	 * ()
	 */
	@Override
	public Object getTasks() {
		// TODO Auto-generated method stub
		return null;
	}
}
