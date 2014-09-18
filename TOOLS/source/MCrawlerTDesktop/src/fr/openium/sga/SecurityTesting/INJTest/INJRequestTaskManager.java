/**
 * 
 */
package fr.openium.sga.SecurityTesting.INJTest;

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
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.reporter.InjTestReporter;
import fr.openium.taskPool.EmulatorManager;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskComparator;
import fr.openium.taskPool.TaskManagerRunnable;

/**
 * @author Stassia
 * 
 * 
 * 
 */
public class INJRequestTaskManager extends AbstractTaskManager {
	@SuppressWarnings("unused")
	private static final String TAG = INJRequestTaskManager.class.getName();
	protected ITaskManager<INJTestJob> tm;
	public static final int _STRATEGY_ID = 111;

	public static void main(String[] args) throws Exception {
		SgdEnvironnement env = Emma.init_environment_(args);
		String out = env.getOutDirectory();
		INJRequestTaskManager injManager = null;
		switch (env.getStrategyType()) {
		case INJRequestTaskManager._STRATEGY_ID:
			injManager = new INJRequestTaskManager(env.getModel(), new File(
					env.getAllTestDataPath()), env);
			break;
		case INJRandomTaskManager._STRATEGY_ID:
			injManager = new INJRandomTaskManager(env.getModel(), new File(
					env.getAllTestDataPath()), env);
			break;

		default:
			throw new NullPointerException("Strategy type is null");
		}

		injManager.launchSecurityTestJob();
		injManager.saveReport(out + File.separator + "report");
		new InjTestReporter(new File(out + File.separator
				+ InjTestReporter.INJREPORT_FILE), injManager.getVerdict())
				.generate();
	}

	public INJRequestTaskManager(ScenarioData model, File testDataFile,
			SgdEnvironnement env) {
		super(model, testDataFile, env);
		getGenerateTaskLists();
	}

	/**
	 * @param testDataFile
	 * @return
	 */
	protected ArrayList<String> read(File testFile) {
		HashSet<RandomValueData> data = new HashSet<RandomValueData>();
		RandomValueParser parser = new RandomValueParser();
		data = parser.parse(testFile);
		mTestDataFile = SgUtils.getTestDataSet(data, RandomValue.INJ);
		return mTestDataFile;
	}

	public HashSet<AbstractTestTask> getGenerateTaskLists() {
		/**
		 * cas initial
		 */
		ScenarioData init_path = getInitTask();
		State initState = init_path.getInitialState();
		if (initState != null && SgUtils.hasEditText(initState)) {
			tasks.add(new INJTask(init_path.getInitialState(), init_path,
					mTestDataFile));
		} else if (initState == null)
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
			if (hasNoEditText(targetState)) {
				continue;
			}
			try {
				ScenarioData path = SgUtils.getConcretePath(targetState, mTree);
				/**
				 * creer tous les chemins apres targetState
				 */
				tasks.add(new INJTask(targetState, path, mTestDataFile));
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
	protected boolean hasSuccessorCrash(State targetState) {
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

	// public static abstract void main(String[] args) throws Exception;

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
		tm = new TaskManagerRunnable<INJTestJob>(get_S_test_Jobs(),
				numberOfThread, new TaskComparator<INJTestJob>(),
				mSgdEnvironnement.getAdb());
		Long initTime = System.currentTimeMillis();
		tm.execute();
		int n = 1;
		boolean taskState = false;
		do {
			taskState = ((TaskManagerRunnable<INJTestJob>) tm)
					.isAllResultReceived();
			try {
				Thread.sleep(5000);
				n = n + 5;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (Utils.limit_time_isReached(initTime, MAX_TIME)) {
				System.out.println("Time " + MAX_TIME + " seconds is reached");
				tm.stop();
				break;
			}
			if ((verdict.size() == tasks.size())) {
				break;
			}
		} while (!taskState);
		System.out.println("Injection Test, finished after" + n + " seconds");
		System.out.println("Waiting for the end of all  jobs");
		do {
			taskState = ((TaskManagerRunnable<INJTestJob>) tm).isTerminated();
			try {
				Thread.sleep(1000);
				n = n + 1;
				System.out.print(".");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!taskState);

		return verdict;

	}

	private ArrayList<INJTestJob> get_S_test_Jobs() {
		ArrayList<INJTestJob> jobns = new ArrayList<INJTestJob>();
		for (AbstractTestTask task : tasks) {
			try {
				jobns.add(new INJTestJob((INJTask) task, tm, mSgdEnvironnement
						.clone(), this));
			} catch (CloneNotSupportedException e) {
				if (ConfigApp.DEBUG) {
					System.out.println("Job is not added due to: "
							+ e.getMessage());
				}
			}
		}
		System.out.println("List of job: " + jobns.size());
		return jobns;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager#updateResult
	 * (java.lang.Object)
	 */
	@Override
	public synchronized boolean updateResult(Object result) {
		SPReport v = (SPReport) result;
		verdict.add(v);
		try {
			saveReport(report.getAbsolutePath(), v);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			checkJobStopCondition();
		}
		return Boolean.parseBoolean(v.getVerdict());
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
		return tasks;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager#
	 * setAbstracTask(java.util.HashSet)
	 */
	@Override
	protected void setAbstracTask(HashSet<AbstractTestTask> tasks) {

	}

	protected void checkJobStopCondition() {

		if (ConfigApp.DEBUG) {
			System.out.println("checkJobStopCondition");

			System.out.println("tasks.size() :" + tasks.size());

			System.out.println("verdict.size() :" + verdict.size());
		}

		if (tasks.size() == verdict.size()) {

			tm.stop();
		}

	}

}
