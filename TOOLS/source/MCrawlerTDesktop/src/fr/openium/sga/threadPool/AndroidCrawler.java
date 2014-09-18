package fr.openium.sga.threadPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kit.Command.PullCommand;
import kit.Command.PushCommand;
import kit.Config.Config;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
import fr.openium.sga.Utils.Utils;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.strategy.Emulator_checker;
import fr.openium.sga.strategy.IEmulateur_Client;
import fr.openium.taskPool.AbstractMobileCrawler;
import fr.openium.taskPool.ITask;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskPoolThread;

public class AndroidCrawler extends AbstractMobileCrawler implements
		IEmulateur_Client {
	CrawlResult mResult;
	SgdEnvironnement mSgdEnvironnement;

	public AndroidCrawler(SgdEnvironnement sgdEnvironnement, ITask tasks,
			ITaskManager<AndroidCrawler> manager, CrawlResult result)
			throws CloneNotSupportedException {
		super(tasks, manager);
		mResult = result;
		mSgdEnvironnement = sgdEnvironnement.clone();
		mSleeper = new ThreadSleeper();
	}

	@Override
	public int getRank() {
		return mTask.getRank();
	}

	private ThreadSleeper mSleeper;
	private boolean all_widget_is_tested = true;
	private boolean interrupted = false;
	private boolean softEnd = false;
	private static long MIN_SLEEP = 2000;
	private static boolean testIsfinshed = false;

	/**
	 * execution sur le mobile ici
	 */
	@Override
	public void run() {
		TaskPoolThread tp = (TaskPoolThread) Thread.currentThread();
		log(tp, "begin task");
		getEmulator(tp);
		try {
			mSgdEnvironnement.init(tp.getId());
		} catch (Exception e1) {
			info("initiation failed " + e1.toString());
			notifyNewTask(null);
			return;

		}
		try {
			init_To_perform_Task();
		} catch (Exception e1) {
			notifyNewTask(null);
			e1.printStackTrace();
			return;
		}

		testIsfinshed = true;
		Long initTime = System.currentTimeMillis();
		/**
		 * creer la tache � faire
		 */
		try {
			launchRemoteTester(tp, initTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		endJob(tp);
	}

	private void getEmulator(TaskPoolThread tp) {
		mSgdEnvironnement.setDevice(tp.getUsedEmulator());

	}

	private void log(TaskPoolThread tp, String log) {
		if (ConfigApp.DEBUG) {
			System.out.println("==========================================="
					+ tp.getId() + "====== log:" + log
					+ "=====================================");
		}

	}

	private void launchRemoteTester(TaskPoolThread tp, Long initTime)
			throws InterruptedException {
		log(tp, "begin launch sga");
		do {
			launchSga(tp);
			mSleeper.sleepLong();
			if (interrupted || Thread.interrupted()) {
				softEnd();
				return;
			}

		} while (!isTestFinished() && all_widget_is_tested);

	}

	private synchronized boolean isTestFinished() {

		return testIsfinshed;
	}

	protected void launchSga(TaskPoolThread tp) throws InterruptedException {
		if (isTestFinished()) {
			Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement,
					mSleeper);
			log(tp, "launch sga :");
			launchSgaWithClassDefined();
			// log(tp, " end of launch sga :");
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

	private void softEnd() {
		try {
			checkIfNewStateIsAvailable();
		} catch (IOException e) {
			e.printStackTrace();
			notifyNewTask(null);
		}
		if (ConfigApp.DEBUG) {
			System.out
					.println("=========================================== end: "
							+ Thread.currentThread().getId()
							+ "===========================================");
		}

	}

	private void endJob(TaskPoolThread tp) {
		log(tp, "test on device is finished");

		try {
			checkIfNewStateIsAvailable();
		} catch (IOException e) {
			e.printStackTrace();
			notifyNewTask(null);
		}
		log(tp, "end");

	}

	/**
	 * @throws IOException
	 * 
	 */
	protected void init_To_perform_Task() throws Exception {
		/**
		 * the task to perform
		 */
		CrawlerTask toPerform = (CrawlerTask) mTask;
		/**
		 * push scenario to Device
		 */
		if (!pushScenario(toPerform)) {
			return;
		}
	}

	protected boolean pushScenario(CrawlerTask toPerform) throws IOException {
		ScenarioData scen = toPerform.getPath();
		if (scen == null) {
			if (ConfigApp.DEBUG) {
				System.out.println("path to commit is null  " + "\n");
			}
			return false;
		}
		if (scen != null && scen.getInitialState() != null) {
			if (ConfigApp.DEBUG) {
				System.out.println("path to commit into device:  " + "\n"
						+ scen.toString());
			}
			String output = (mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);
			new ScenarioGenerator(output).generateXml(scen);
			PushCommand push = new PushCommand(mSgdEnvironnement.getAdb(),
					mSgdEnvironnement.getDevice(), output,
					ConfigApp.DEVICETESTRESULTS);
			push.execute();
			new File(output).delete();
		} else {
			throw new IllegalStateException("Invalid path:  "
					+ ((scen == null) ? "initial state is null"
							: scen.toString()));
		}
		return true;
	}

	/**
	 * Check if a new state is available from the mobile. Update and notify
	 * 
	 * @throws IOException
	 * 
	 * @throws CloneNotSupportedException
	 */
	protected void checkIfNewStateIsAvailable() throws IOException {
		if (ConfigApp.DEBUG) {
			System.out
					.println("======================== check if new states are available "
							+ "===========================================");
		}
		/**
		 * update coverage
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("coverage : "
					+ mSgdEnvironnement.isCodeCoverageLimitReached());
		}

		new Thread() {
			public void run() {
				save_time();

			};
		}.start();
		try {
			Thread.sleep(MIN_SLEEP);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		new Thread() {
			public void run() {

				try {
					save_screenShots();
				} catch (IOException e) {
					e.printStackTrace();
				}

			};
		}.start();

		save_non_generic_scenario();
		// pull time

		File scenario_outFile = new File(
				mSgdEnvironnement.getScenarioDirectory() + ConfigApp.OutXMLPath);
		if (!scenario_outFile.exists()) {
			notifyNewTask(null);
			return;
		}
		if (FileUtils.readLines(scenario_outFile) == null
				|| FileUtils.readLines(scenario_outFile).isEmpty()) {
			notifyNewTask(null);
			return;
		}
		ScenarioData scen = ScenarioParser.parse(scenario_outFile);
		if (scen == null) {
			notifyNewTask(null);
			return;
		}
		// new ScenarioGenerator(mSgdEnvironnement.getOutDirectory() +
		// "_temp").generateXml(scen);
		// Emma.savegeneric_file(mSgdEnvironnement.getOutDirectory() + "_temp",
		// mSgdEnvironnement.getOutDirectory() + File.separator +
		// ConfigApp.RECEIVED_PATH, ".xml");
		CrawlerTask task = new CrawlerTask(scen);
		if (!interrupted) {
			notifyNewTask(task);
		}

		try {
			Thread.sleep(MIN_SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @throws IOException
	 *             Heavy task run in a thread
	 * 
	 */
	protected void save_screenShots() throws IOException {

		SgdEnvironnement localue;
		try {
			localue = mSgdEnvironnement.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return;
		}

		File screenShootTemp = new File(localue.getOutDirectory()
				+ File.separator + Config.ROBOTIUM_SCREENSHOTS);
		System.out.println(screenShootTemp);
		if (!screenShootTemp.exists()) {
			screenShootTemp.mkdir();
		}
		PullCommand pull = new PullCommand(localue.getSdkPath()
				+ File.separator + ConfigApp.ADBPATH, localue.getDevice(),
				ConfigApp.DEVICESDCARD + "/" + Config.ROBOTIUM_SCREENSHOTS,
				screenShootTemp.getPath());

		System.out.println(ConfigApp.DEVICESDCARD + "/"
				+ Config.ROBOTIUM_SCREENSHOTS);
		pull.execute();
		mSleeper.sleepLong();
		screenShootTemp.deleteOnExit();
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void save_non_generic_scenario() throws IOException {
		Utils.pull(ConfigApp.OutXMLPath, mSgdEnvironnement);
		File outFile = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OutXMLPath);
		Utils.save_non_generic(outFile,
				new File(mSgdEnvironnement.getScenarioDirectory()));

	}

	/**
	 * 
	 */
	private void save_time() {
		SgdEnvironnement localue;
		try {
			localue = mSgdEnvironnement.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return;
		}

		Utils.pull(ConfigApp.TIME_PATH, localue);

		File timeFile = new File(localue.getOutDirectory()
				+ ConfigApp.TIME_PATH);
		Utils.savegeneric_file(timeFile, new File(localue.getTimeDirectory()));
	}

	private void checkIfTestOnDeviceIsFinished() {
		Emulator_checker checker = new Emulator_checker(this, mSgdEnvironnement);
		checker.run();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void set_sga_is_finished(boolean b) {
		testIsfinshed = b;

	}

	private void info(String log) {
		Utils.info("[ Thread:" + Thread.currentThread() + "]" + log);
	}

	/**
	 * if new State: submit to TaskPoolManagr
	 * 
	 * @param result
	 * @throws CloneNotSupportedException
	 */

	protected void notifyNewTask(CrawlerTask result) {
		/**
		 * le fichier out.xml n'existe pas
		 */
		if (result == null) {
			mTaskManager.update(null);
			return;
		}
		List<AndroidCrawler> tasksList = new ArrayList<AndroidCrawler>();
		synchronized (mResult) {
			do {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (!mResult.isAvailable());
			List<CrawlerTask> returned_Result = mResult.update(result);
			if (returned_Result != null) {
				AndroidCrawler job = null;
				try {
					for (CrawlerTask task : returned_Result) {
						/**
						 * addPheromon
						 */
						task.setPheromone(mTask.getRank());

						job = getNewTask(task);

						log("add a new job");
						// job.setPheromone();
						log("job rank : " + job.getRank());
						tasksList.add(job);
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
				mTaskManager.update(tasksList);
			} else {
				mTaskManager.update(null);
			}
		}
	}

	protected AndroidCrawler getNewTask(CrawlerTask task)
			throws CloneNotSupportedException {
		return new AndroidCrawler(mSgdEnvironnement, task, getTaskManager(),
				mResult);
	}

	protected void log(String log) {
		if (ConfigApp.DEBUG) {
			System.out.println("===========================================  "
					+ log + "===========================================");
		}

	}

	@Override
	public void update_state(boolean status) {

		// set_sga_is_finished(false);
		File ok = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OkPath);
		Utils.save_generic_ok(mSgdEnvironnement, ok);
		Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement,
				new ThreadSleeper());
		Utils.save_generic_rv_done(mSgdEnvironnement);
		Utils.save_genericTime(mSgdEnvironnement);
		/**
		 * s'il y a des widgets non crawler ou nombre de sequence non fini, �
		 * refaire
		 */
		set_sga_is_finished(status);
	}

	@Override
	public void interrupted() {
		interrupted = true;
		waitSoftShutDown();
	}

	private void waitSoftShutDown() {
		softEnd();
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		} while (softEnd);

	}

	@Override
	public boolean remoteTestState() {
		return remoteState;
	}

}
