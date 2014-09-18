package fr.openium.sga.SecurityTesting.BruteForceTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import kit.Command.PushCommand;
import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;
import kit.Scenario.State;
import kit.Utils.SgUtils;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.SecurityTesting.AbstractTest.AbstractTestTask;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.strategy.Emulator_checker;
import fr.openium.sga.strategy.IEmulateur_Client;
import fr.openium.taskPool.AbstractMobileCrawler;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskPoolThread;

public class BruteForceJob extends AbstractMobileCrawler implements Runnable,
		IEmulateur_Client {

	private BruteForceManager mBruteForceManager;
	private SgdEnvironnement mSgdEnvironnement;
	private ThreadSleeper mSleeper = new ThreadSleeper();
	private static boolean testIsfinshed = false;
	private static final String TAG = BruteForceJob.class.getName();
	private ArrayList<String> mGeneratedSequence;

	public BruteForceJob(BruteForceTask task,
			ITaskManager<BruteForceJob> manager, SgdEnvironnement env,
			BruteForceManager bruteForceManager) {
		super(task, manager);
		mSgdEnvironnement = env;
		mBruteForceManager = bruteForceManager;
	}

	@Override
	public void update_state(boolean status) {
		set_sga_is_finished(status);
	}

	private void set_sga_is_finished(boolean b) {
		testIsfinshed = b;
	}

	@Override
	public void run() {

		if (!initTest()) {
			mBruteForceManager.updateResult(null);
			return;
		}
		try {
			generateBruteEventsAndPushTasksToDevice();
		} catch (Exception e1) {
			e1.printStackTrace();
			mBruteForceManager.updateResult(null);
			return;
		}
		testIsfinshed = true;
		Long initTime = System.currentTimeMillis();
		do {
			if (testIsfinshed) {
				Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement,
						mSleeper);
				mSgdEnvironnement.launchSga_class_defined();

				set_sga_is_finished(false);
				remoteState = true;
				checkIfTestOnDeviceIsFinished();
			}
			mSleeper.sleepLong();
			/*
			 * if (Utils.limit_time_isReached(initTime, Emma.TIME_LIMITE)) {
			 * break; }
			 */} while (!testIsfinshed);

		try {
			check_brute_result();
		} catch (IOException e) {
			e.printStackTrace();
			mBruteForceManager.updateResult(null);
		}
		if (ConfigApp.DEBUG) {
			System.out.println("Job is finished");
		}

	}

	private void generateBruteEventsAndPushTasksToDevice() throws IOException,
			NullPointerException {

		BruteForceTask task = (BruteForceTask) mTask;
		ScenarioData scen = task.getPath();
		if (scen == null) {
			throw new IllegalStateException("Path must not be null");
		}
		/**
		 * Creat a bruteForceTestGenerator
		 * 
		 */
		BruteForceEventsGenerator genData = new BruteForceEventsGenerator(
				task.getTargetState(), new File(
						mSgdEnvironnement.getAllTestDataPath()), new File(
						mSgdEnvironnement.getDicoPath()),
				mSgdEnvironnement.getStressMaxEvent());
		genData.generateBrutForceTest(mSgdEnvironnement.getBruteEventFile());

		String output = (mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);
		System.out.println("output file" + output);
		new ScenarioGenerator(output).generateXml(scen);

		/**
		 * push event
		 */
		PushCommand push = new PushCommand(mSgdEnvironnement.getAdb(),
				mSgdEnvironnement.getDevice(), mSgdEnvironnement
						.getBruteEventFile().getPath(),
				ConfigApp.DEVICETESTRESULTS);
		push.execute();

		/**
		 * push out tasks
		 */
		push = new PushCommand(mSgdEnvironnement.getAdb(),
				mSgdEnvironnement.getDevice(), output,
				ConfigApp.DEVICETESTRESULTS);
		push.execute();

	}

	private boolean initTest() {
		associateAnEmulator();
		try {
			initLocalDirectory();
			initRemoteDirectory();
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}
		return true;
	}

	private void associateAnEmulator() {
		// System.out.println("running");
		TaskPoolThread tp = (TaskPoolThread) Thread.currentThread();
		mSgdEnvironnement.setDevice(tp.getUsedEmulator());
		if (ConfigApp.DEBUG) {
			System.out.println(TAG + " run " + tp.getId());
			System.out.println("emulator: " + mSgdEnvironnement.getDevice());
		}

	}

	private void initRemoteDirectory() {
		mSgdEnvironnement.initRemoteDirectory();
	}

	private void initLocalDirectory() throws IOException {
		/**
		 * initDirectoryname with thread targetStateId
		 */
		System.out.println("initLocalDirectory() of thread number: "
				+ Thread.currentThread().getId());
		BruteForceTask btask = (BruteForceTask) mTask;
		State targetState = SgUtils.get_last_state(btask.getPath());
		String toBruteId = (targetState == null ? ""
				+ Thread.currentThread().getId() : targetState.getId());
		System.out.println("to Brute id " + toBruteId);
		System.out.println(toBruteId);
		mSgdEnvironnement.init(toBruteId);

	}

	@Override
	public int getRank() {
		return mTask.getRank();
	}

	private void checkIfTestOnDeviceIsFinished() {

		Emulator_checker checker = new Emulator_checker(this, mSgdEnvironnement);
		checker.run();
	}

	private void check_brute_result() throws IOException {

		pullingFromDevice();
		mSgdEnvironnement.saveScreenShot();
		AbstractTestTask temp = ((BruteForceTask) mTask);
		/**
		 * update coverage
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("coverage : "
					+ mSgdEnvironnement.isCodeCoverageLimitReached());
		}
		BruteForceReport report = buildReport(temp);
		mBruteForceManager.updateResult(report);
		// mSgdEnvironnement.initRemoteDirectory();

	}

	private BruteForceReport buildReport(AbstractTestTask temp)
			throws IOException {
		ScenarioData destState = null;

		File outFile = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OutXMLPath);
		Utils.save_non_generic(outFile,
				new File(mSgdEnvironnement.getScenarioDirectory()));
		File scenario_outFile = new File(
				mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);
		if (!scenario_outFile.exists()) {
			return null;
		}

		if (FileUtils.readLines(scenario_outFile) == null
				|| FileUtils.readLines(scenario_outFile).isEmpty()) {
			return null;
		}
		destState = ScenarioParser.parse(scenario_outFile);
		if (destState == null) {
			return null;
		}

		BruteForceReport report = new BruteForceReport(mBruteForceManager,
				temp.getTargetState(), temp.getPath(), "", "", destState,
				mGeneratedSequence, (ArrayList<String>) FileUtils.readLines(
						new File(mSgdEnvironnement.getOutDirectory()
								+ File.separator + Config.BRUTE_TIME),
						Config.UTF8));
		return report;
	}

	private void pullingFromDevice() {
		Utils.pull(Config.BRUTE_TIME, mSgdEnvironnement);
		Utils.pull(ConfigApp.OutXMLPath, mSgdEnvironnement);
		Utils.pull(Config.BRUTE_TIME, mSgdEnvironnement);
		// Emma.pull(Config.error, mSgdEnvironnement);
		// Emma.pull(Config.EXECUTED_EVENTS, mSgdEnvironnement);

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
/**
 * Pull out.xml, error file, executed_event
 * 
 */

