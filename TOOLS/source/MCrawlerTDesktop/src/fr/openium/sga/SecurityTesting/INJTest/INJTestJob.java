/**
 * 
 */
package fr.openium.sga.SecurityTesting.INJTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import kit.Command.PushCommand;
import kit.Config.Config;
import kit.Inj.InjDataGenerator;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.strategy.Emulator_checker;
import fr.openium.sga.strategy.IEmulateur_Client;
import fr.openium.taskPool.AbstractMobileCrawler;
import fr.openium.taskPool.ITask;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskPoolThread;

/**
 * @author Stassia
 * 
 */
public class INJTestJob extends AbstractMobileCrawler implements Runnable,
		IEmulateur_Client {
	private INJRequestTaskManager mManager;
	private SgdEnvironnement mSgdEnvironnement;
	private ThreadSleeper mSleeper = new ThreadSleeper();

	private static final String TAG = INJTestJob.class.getName();

	/**
	 * @param tasks
	 * @param manager
	 * @param injTaskManager
	 * @throws CloneNotSupportedException
	 */
	public INJTestJob(ITask tasks, ITaskManager<?> manager,
			SgdEnvironnement env, INJRequestTaskManager injTaskManager)
			throws CloneNotSupportedException {
		super(tasks, manager);
		mSgdEnvironnement = env;
		mManager = injTaskManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.strategy.IEmulateur_Client#update_state(boolean)
	 */
	@Override
	public void update_state(boolean status) {
		set_sga_is_finished(status);
	}

	private static boolean testIsfinshed = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// System.out.println("running");
		TaskPoolThread tp = (TaskPoolThread) Thread.currentThread();
		mSgdEnvironnement.setDevice(tp.getUsedEmulator());

		if (ConfigApp.DEBUG) {
			System.out.println(TAG + " run " + tp.getId());
			System.out.println("emulator: " + mSgdEnvironnement.getDevice());
		}
		try {
			mSgdEnvironnement.init(tp.getId());
		} catch (Exception e1) {
			return;

		}
		mSgdEnvironnement.initRemoteDirectory();
		try {
			// pusher le chemin

			// pusher les �v�nements

			init_To_perform_Task();
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
		testIsfinshed = true;
		Long initTime = System.currentTimeMillis();
		/**
		 * creer la tache � faire
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("\n==========================================="
					+ tp.getId()
					+ "===========================================");
		}
		try {
			launchInjJob(tp, initTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (ConfigApp.DEBUG) {
			System.out
					.println("===========================================: "
							+ tp.getId()
							+ " test on device is finished ===========================================");
		}

		try {
			check_inj_result();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (ConfigApp.DEBUG) {
			System.out
					.println("=========================================== end: "
							+ tp.getId()
							+ "===========================================");
		}
		if (ConfigApp.DEBUG) {
			System.out.println("Job is finished");
		}
		// mTaskManager.update(null);
	}

	private void launchInjJob(TaskPoolThread tp, Long initTime)
			throws InterruptedException {
		do {
			launchSga(tp);
		} while (!testIsfinshed);

	}

	protected void launchSga(TaskPoolThread tp) throws InterruptedException {
		if (isTestFinished()) {
			Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement,
					mSleeper);
			launchSgaWithClassDefined();
			checkIfTestOnDeviceIsFinished();
		} else {
			if (ConfigApp.DEBUG) {
				System.out.println(".");
			}
		}

	}

	protected void launchSgaWithClassDefined() throws InterruptedException {
		// in a thread
		new Thread() {
			@Override
			public void run() {
				mSgdEnvironnement.launchSga_class_defined();
				/*
				 * notify emulator checker if finished
				 */
				remoteState = true;
			};
		}.start();
		Thread.sleep(100);
		set_sga_is_finished(false);
	}

	private synchronized boolean isTestFinished() {
		return testIsfinshed;
	}

	/**
	 * check if there is a crash
	 * 
	 * @throws IOException
	 */
	private void check_inj_result() throws IOException {
		pullOutputFromDevice();
		INJTask temp = ((INJTask) mTask);
		SPReport injReport = mManager.new SPReport(temp.getTargetState(),
				temp.getPath(), "" + false, "");
		updateCoverage(injReport);
		addSuccessorInfo(injReport, temp);
		addExecutedEvents(injReport);
		translateCrashDescription(injReport);
		translateScenarioOutput(injReport);
		mManager.updateResult(injReport);

	}

	private void updateCoverage(SPReport injReport) {
		/**
		 * update coverage
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("coverage : "
					+ mSgdEnvironnement.isCodeCoverageLimitReached());
		}
		injReport.setCoverage(mSgdEnvironnement.getCurrentCoverage());

	}

	private void pullOutputFromDevice() {
		Utils.pull(ConfigApp.OutXMLPath, mSgdEnvironnement);
		Utils.pull(Config.error, mSgdEnvironnement);
		Utils.pull(Config.EXECUTED_EVENTS, mSgdEnvironnement);

	}

	private void addSuccessorInfo(SPReport injReport, INJTask temp) {
		ScenarioData successor = temp.getSuccessor();

		if (successor != null) {
			injReport.setTestedSuccessor(successor);
		}

	}

	private void addExecutedEvents(SPReport injReport) {
		File events_doneFile = new File(mSgdEnvironnement.getOutDirectory()
				+ File.separator + Config.EXECUTED_EVENTS);
		ArrayList<String> event_done = new ArrayList<String>();
		try {
			event_done = (ArrayList<String>) FileUtils
					.readLines(events_doneFile);
		} catch (Exception e) {
			if (ConfigApp.DEBUG) {
				System.out.println("No event done pulled");
			}
		}
		injReport.setExecutedEvents(event_done);

	}

	private void translateScenarioOutput(SPReport injReport) throws IOException {
		File outFile = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OutXMLPath);
		Utils.save_non_generic(outFile,
				new File(mSgdEnvironnement.getScenarioDirectory()));
		File scenario_outFile = new File(
				mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);
		if (scenario_outFile.exists()) {

			if (FileUtils.readLines(scenario_outFile) == null
					|| FileUtils.readLines(scenario_outFile).isEmpty()) {
				addError(injReport, "out.xml is empty");
				// mManager.updateResult(injReport);
				// return;
			} else {

				ScenarioData reached_states = null;
				try {
					reached_states = ScenarioParser.parse(scenario_outFile);
				} catch (Exception sax) {
					addError(injReport, sax.getMessage());
					return;
				}

				if (reached_states == null) {
					addError(injReport, "out.xml is null");
					// injReport.setErrorDescription("out.xml is null");
					// mManager.updateResult(injReport);
					// return;
				} else if (SgUtils.path_contains_error(reached_states)) {
					ArrayList<Transition> VulTr = SgUtils
							.contains_error(reached_states);
					setVerdict(injReport, true);
					injReport.setVulnerableTransition(VulTr);
					addError(injReport, SgUtils.get_error(reached_states));

				}

			}

		}

		/**
		 * read LogFail to detetec vulnerability failure
		 */

	}

	private void translateCrashDescription(SPReport injReport)
			throws IOException {

		File erroFile = new File(mSgdEnvironnement.getOutDirectory()
				+ File.separator + "error");
		if (erroFile.exists()) {
			String out = FileUtils.readFileToString(erroFile, Config.UTF8);
			addError(injReport, out);
			setVerdict(injReport, true);
			// mManager.updateResult(injReport);
			// mSgdEnvironnement.initRemoteDirectory();
			// return;

		}

	}

	private void setVerdict(SPReport injReport, boolean b) {
		if (!Boolean.parseBoolean(injReport.getVerdict()))
			injReport.setVerdict("" + b);
	}

	private void addError(SPReport injReport, String out) {

		injReport.setErrorDescription(injReport.getMessage() + "\n" + out);

	}

	private void checkIfTestOnDeviceIsFinished() {

		Emulator_checker checker = new Emulator_checker(this, mSgdEnvironnement);
		checker.run();
	}

	private void set_sga_is_finished(boolean b) {
		testIsfinshed = b;
	}

	/**
	 * pusher le chemin pusher
	 * 
	 * @throws Exception
	 */
	private void init_To_perform_Task() throws Exception {
		INJTask task = (INJTask) mTask;
		ScenarioData scen = task.getPath();
		if (scen == null) {
			throw new IllegalStateException("Path must not be null");
		}
		InjDataGenerator genData = new InjDataGenerator(task.getTargetState(),
				task.getTestData(), mSgdEnvironnement.getStressMaxEvent());
		genData.generateInjectionTestData(mSgdEnvironnement
				.getStressEventFile());

		String output = (mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);
		System.out.println("output file" + output);
		new ScenarioGenerator(output).generateXml(scen);
		/**
		 * push event
		 */
		PushCommand push = new PushCommand(mSgdEnvironnement.getAdb(),
				mSgdEnvironnement.getDevice(), mSgdEnvironnement
						.getStressEventFile().getPath(),
				ConfigApp.DEVICETESTRESULTS);
		push.execute();

		/**
		 * push out
		 */
		push = new PushCommand(mSgdEnvironnement.getAdb(),
				mSgdEnvironnement.getDevice(), output,
				ConfigApp.DEVICETESTRESULTS);
		push.execute();
		/**
		 * push db
		 */
		push = new PushCommand(mSgdEnvironnement.getAdb(),
				mSgdEnvironnement.getDevice(), mSgdEnvironnement.getDbPath(),
				ConfigApp.DEVICETESTRESULTS);
		push.execute();
		/**
		 * push succFile
		 */

		if (task.getSuccessor() != null) {
			String tr_out = mSgdEnvironnement.getSuccTransitionFile().getPath();
			System.out.println("tr_output file: " + output);
			new ScenarioGenerator(tr_out).generateXml(task.getSuccessor());
			push = new PushCommand(mSgdEnvironnement.getAdb(),
					mSgdEnvironnement.getDevice(), tr_out,
					ConfigApp.DEVICETESTRESULTS);
			push.execute();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.taskPool.AbstractMobileCrawler#getRank()
	 */
	@Override
	public int getRank() {
		return 0;
	}

	@Override
	public void interrupted() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean remoteTestState() {
		return remoteState;
	}

}
