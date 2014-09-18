package fr.openium.sga.SecurityTesting.BruteForceTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.State;
import kit.Utils.SgUtils;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.semantic.LoggingSemantic;
import fr.openium.sga.strategy.SemanticStrategy;
import fr.openium.taskPool.EmulatorManager;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskComparator;
import fr.openium.taskPool.TaskManagerRunnable;

public class BruteForceManager extends AbstractTaskManager {

	public BruteForceManager(SgdEnvironnement env) {
		super(env.getModel(), new File(env.getAllTestDataPath()), env);
		/**
		 * setup scenario generator to generate Paths
		 */
		String out = mSgdEnvironnement.getOutDirectory();
		// en dure à voir
		String bruteForce = "bruteForce";
		mScenarioGeneratorFilePath = (out + File.separator + bruteForce);
	}

	private ScenarioGenerator mScenarioGenerator;
	private String mScenarioGeneratorFilePath;
	public final static int _STRATEGY_ID = 122;

	@Override
	protected void setAbstracTask(HashSet<AbstractTestTask> tasks) {

	}

	public static void main(String[] args) throws Exception {
		SgdEnvironnement env = Emma.init_environment_(args);
		BruteForceManager bruteForce = new BruteForceManager(env);
		bruteForce.launchSecurityTestJob();
		if (ConfigApp.DEBUG) {
			System.out.println("Brute force Report: ");
		}

		for (SPReport rep : verdict) {
			if (ConfigApp.DEBUG && rep != null) {
				System.out.println(rep.getTargetState().toString() + "   "
						+ rep.getVerdict());
			}
		}
	}

	/**
	 * It will be read from the brute force dictionnary
	 */
	@Override
	protected ArrayList<String> read(File testDataFile) {
		return null;
	}

	protected ITaskManager<BruteForceJob> tm;

	/**
	 * 
	 */
	@Override
	public ArrayList<SPReport> launchSecurityTestJob() throws Exception {
		// generate Task List
		getGenerateTaskLists();
		// install sga on device
		installOnDevice();
		numberOfThread = mSgdEnvironnement.getThread_number();
		commitJobs(numberOfThread);
		return verdict;
	}

	private void commitJobs(int numberOfThread) {
		tm = new TaskManagerRunnable<BruteForceJob>(get_bruteforce_Jobs(),
				numberOfThread, new TaskComparator<BruteForceJob>(),
				mSgdEnvironnement.getAdb());
		Long initTime = System.currentTimeMillis();
		tm.execute();
		int n = 1;
		boolean tasksStateFinished = false;
		do {
			tasksStateFinished = ((TaskManagerRunnable<BruteForceJob>) tm)
					.isAllResultReceived();
			waitBruteJobs(n);
			if (Utils.limit_time_isReached(initTime, MAX_TIME)) {
				System.out.println("Time " + MAX_TIME + " seconds is reached");
				tm.stop();
				break;
			}
			if (checkBrutForceJobStopCondition()) {
				break;
			}
		} while (stopCondition(tasksStateFinished));
		System.out.println("Brute Force Test, finished after  " + n
				+ " seconds");
	}

	private boolean stopCondition(boolean taskState) {
		// || (verdict.size() != tasks.size())

		return !taskState;
	}

	private void waitBruteJobs(int n) {
		try {
			Thread.sleep(5000);
			n = n + 5;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return list of locations where brute force will be executed
	 */
	private ArrayList<BruteForceJob> get_bruteforce_Jobs() {
		ArrayList<BruteForceJob> jobs = new ArrayList<BruteForceJob>();
		for (AbstractTestTask task : tasks) {
			try {
				jobs.add(new BruteForceJob((BruteForceTask) task, tm,
						mSgdEnvironnement.clone(), this));
			} catch (CloneNotSupportedException e) {
				if (ConfigApp.DEBUG) {
					System.out.println("Job is not added due to: "
							+ e.getMessage());
				}
			}
		}
		System.out.println("List of job to commit: " + jobs.size());
		return jobs;
	}

	private void installOnDevice() throws Exception {
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
		// reset device
		mSgdEnvironnement.setDevice(null);
	}

	@Override
	public HashSet<AbstractTestTask> getGenerateTaskLists() {
		/**
		 * partir d'un modèle extrapolé
		 */
		/**
		 * getExtrapolated Mobile
		 */
		/**
		 * pour le moment, partir du scénario
		 */
		/**
		 * init state
		 */
		ScenarioData init_path = getInitTask();
		State initState = init_path.getInitialState();
		if (initState != null && isAuthentificationState(initState)) {
			tasks.add(new BruteForceTask(init_path.getInitialState(),
					init_path, mTestDataFile));
			System.out.println("add task : " + initState.toString());
			System.out.println("path task : " + null);

		}

		/**
		 * other cases
		 */

		for (State targetState : mTree.getStates()) {

			if (!isAuthentificationState(targetState)) {
				continue;
			}

			try {
				// ScenarioData path = SgUtils._getPath(targetState, mTree);
				ScenarioData path = SgUtils.getConcretePath(targetState, mTree);
				tasks.add(new BruteForceTask(targetState, path, mTestDataFile));
				System.out.println("add task : " + targetState.toString());
				System.out.println("path task : " + path.toString());
				mScenarioGenerator = new ScenarioGenerator(
						mScenarioGeneratorFilePath + targetState.getId());
				mScenarioGenerator.generateXml(path);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				continue;
			}
		}
		return tasks;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager#updateResult
	 * (java.lang.Object) For BruteForce, result to update BruteForceReport
	 */

	private boolean isAuthentificationState(State targetState) {
		if (targetState.isInit()) {
			return false;
		}
		if (isInTasks(targetState)) {
			return false;
		}
		if (!targetState.isDest()) {
			return false;
		}

		if (targetState.isFinal()) {
			return false;
		}

		/**
		 * has at least 1 editText
		 */
		if (!SgUtils.hasEditText(targetState)) {
			return false;
		}

		LoggingSemantic log = SemanticStrategy
				.getLoggingSemantic(mSgdEnvironnement.getAllTestDataPath());
		return (SgUtils.isLogginState(targetState, log.VALUE_SET) > 0 ? true
				: false);
	}

	@Override
	public synchronized boolean updateResult(Object result) {
		System.out.println("**report received****");

		if (result == null) {
			System.out.println("null");
			verdict.add(null);
			checkBrutForceJobStopCondition();
			return true;
		}
		BruteForceReport v = (BruteForceReport) result;
		verdict.add(v);
		try {
			saveReport(report.getAbsolutePath(), v);
		} catch (IOException e) {
			System.err.println("report is not saved");
		} finally {
			checkBrutForceJobStopCondition();
		}
		return Boolean.parseBoolean(v.getVerdict());

	}

	private boolean checkBrutForceJobStopCondition() {
		if (ConfigApp.DEBUG) {
			System.out.println("checkJobStopCondition");

			System.out.println("tasks.size() :" + tasks.size());

			System.out.println("verdict.size() :" + verdict.size());
		}

		if (tasks.size() == verdict.size()) {
			tm.stop();
			return true;
		}
		return false;

	}

	@Override
	public Object getTasks() {
		// TODO Auto-generated method stub
		return null;
	}

}
