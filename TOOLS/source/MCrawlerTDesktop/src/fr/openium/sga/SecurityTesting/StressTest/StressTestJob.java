/**
 * 
 */
package fr.openium.sga.SecurityTesting.StressTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import kit.Command.PushCommand;
import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Stress.StressDataGenerator;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTaskManager.SPReport;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
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
public class StressTestJob extends AbstractMobileCrawler implements Runnable,
		IEmulateur_Client {
	/**
	 * @param tasks
	 * @param manager
	 * @param stressTaskManager
	 * @throws CloneNotSupportedException
	 */
	public StressTestJob(ITask tasks, ITaskManager<?> manager,
			SgdEnvironnement env, StressTaskManager stressTaskManager)
			throws CloneNotSupportedException {
		super(tasks, manager);
		mSgdEnvironnement = env;
		mStressManager = stressTaskManager;
	}

	private StressTaskManager mStressManager;
	private SgdEnvironnement mSgdEnvironnement;
	private ThreadSleeper mSleeper = new ThreadSleeper();

	private static final String TAG = StressTestJob.class.getName();

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
			mStressManager.updateResult(null);
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
			launchStressJob(tp, initTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			check_stress_result();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (ConfigApp.DEBUG) {
			System.out
					.println("=========================================== Job is finished  "
							+ tp.getId());
		}
		// mTaskManager.update(null);
	}

	private void launchStressJob(TaskPoolThread tp, Long initTime)
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

	private synchronized boolean isTestFinished() {
		return testIsfinshed;
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

	/**
	 * check if there is a crash
	 * 
	 * @throws IOException
	 */
	private void check_stress_result() throws IOException {

		Utils.pull(ConfigApp.OutXMLPath, mSgdEnvironnement);
		Utils.pull(Config.error, mSgdEnvironnement);
		Utils.pull(Config.EXECUTED_EVENTS, mSgdEnvironnement);
		AbstractTestTask temp = ((StressTask) mTask);

		File erroFile = new File(mSgdEnvironnement.getOutDirectory()
				+ File.separator + Config.error);
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
		/**
		 * update coverage
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("coverage : "
					+ mSgdEnvironnement.isCodeCoverageLimitReached());
		}

		SPReport report = mStressManager.new SPReport(temp.getTargetState(),
				temp.getPath(), "" + false, null);
		report.setExecutedEvents(event_done);
		report.setCoverage(mSgdEnvironnement.getCurrentCoverage());

		if (erroFile.exists()) {
			String out = FileUtils.readFileToString(erroFile, Config.UTF8);
			report.setErrorDescription(out);
			report.setVerdict("" + true);
			mStressManager.updateResult(report);
			mSgdEnvironnement.initRemoteDirectory();
			return;

		}

		File outFile = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OutXMLPath);
		Utils.save_non_generic(outFile,
				new File(mSgdEnvironnement.getScenarioDirectory()));
		File scenario_outFile = new File(
				mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);

		if (!scenario_outFile.exists()) {
			if (ConfigApp.DEBUG) {
				System.out.println("out.xml file does not exist");
			}
			mStressManager.updateResult(report);
			return;
		}
		if (FileUtils.readLines(scenario_outFile) == null
				|| FileUtils.readLines(scenario_outFile).isEmpty()) {
			mStressManager.updateResult(report);
			return;
		}
		ScenarioData scen = ScenarioParser.parse(scenario_outFile);
		if (scen == null) {
			if (ConfigApp.DEBUG) {
				System.out.println("scenario null");
			}
			mStressManager.updateResult(report);
			return;
		}
		if (SgUtils.path_contains_error(scen)) {
			if (ConfigApp.DEBUG) {
				System.out.println("No error");
			}
			report.setErrorDescription(SgUtils.get_error(scen));
			report.setVerdict("" + true);
			mStressManager.updateResult(report);
			return;
		}

		mStressManager.updateResult(report);
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

		StressTask task = (StressTask) mTask;
		ScenarioData scen = task.getPath();
		if (scen == null) {
			throw new IllegalStateException("Path must not be null");
		}
		StressDataGenerator genData = new StressDataGenerator(
				task.getTargetState(), task.getTestData(),
				mSgdEnvironnement.getStressMaxEvent());
		genData.generateStressTest(mSgdEnvironnement.getStressEventFile());

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
	public synchronized boolean remoteTestState() {
		return remoteState;
	}

}
