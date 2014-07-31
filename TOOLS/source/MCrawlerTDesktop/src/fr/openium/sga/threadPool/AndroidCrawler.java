package fr.openium.sga.threadPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kit.Command.PushCommand;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.ScenarioParser;

import org.apache.commons.io.FileUtils;

import fr.openium.sga.ConfigApp;
import fr.openium.sga.ThreadSleeper;
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
	private int mOccurrence = 0;
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
		if (ConfigApp.DEBUG) {
			System.out
					.println("===========================================begin : "
							+ tp.getId()
							+ "===========================================");
		}
		mSgdEnvironnement.setDevice(tp.getUsedEmulator());
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
		info("TEST NUMBER " + (mOccurrence++));
		testIsfinshed = true;
		Long initTime = System.currentTimeMillis();
		/**
		 * creer la tache � faire
		 */
		if (ConfigApp.DEBUG) {
			System.out.println("==========================================="
					+ tp.getId()
					+ "===========================================");
		}
		do {
			
			if (testIsfinshed) {
				info("TEST NUMBER " + mOccurrence++);
				Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement,
						mSleeper);
				if (ConfigApp.DEBUG) {
					System.out
							.println("===========================================launch sga :"
									+ tp.getId()
									+ "===========================================");
				}
				mSgdEnvironnement.launchSga_class_defined();
				if (ConfigApp.DEBUG) {
					System.out
							.println("=========================================== end of launch sga :"
									+ tp.getId()
									+ "===========================================");
				}
				set_sga_is_finished(false);
				checkIfTestOnDeviceIsFinished();
			}
			
			mSleeper.sleepLong();
			
			if (Emma.limit_time_isReached(initTime, Emma.TIME_LIMITE)) {
				break;
			}
			
			
			if (interrupted||Thread.interrupted()) {
				softEnd();
				return;
			}
			
		} while (!testIsfinshed && all_widget_is_tested);

		endJob(tp);
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
		if (ConfigApp.DEBUG) {
			System.out
					.println("===========================================: "
							+ tp.getId()
							+ " test on device is finished ===========================================");
		}

		try {
			checkIfNewStateIsAvailable();
		} catch (IOException e) {
			e.printStackTrace();
			notifyNewTask(null);
		}
		if (ConfigApp.DEBUG) {
			System.out
					.println("=========================================== end: "
							+ tp.getId()
							+ "===========================================");
		}

	}

	/**
	 * @throws IOException
	 * 
	 */
	private void init_To_perform_Task() throws Exception {
		/**
		 * the task to perform
		 */
		CrawlerTask toPerform = (CrawlerTask) mTask;
		/**
		 * push to Device
		 */
		ScenarioData scen = toPerform.getPath();
		if (scen == null) {
			if (ConfigApp.DEBUG) {
				System.out.println("path to commit is null  " + "\n");
			}
			return;
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
	}

	/**
	 * Check if a new state is available from the mobile. Update and notify
	 * 
	 * @throws IOException
	 * 
	 * @throws CloneNotSupportedException
	 */
	private void checkIfNewStateIsAvailable() throws IOException {
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

		save_non_generic_scenario();
		// pull time
		save_time();

		save_screenShots();

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
	 * 
	 */
	protected void save_screenShots() throws IOException {
		mSgdEnvironnement.saveScreenShot();
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void save_non_generic_scenario() throws IOException {
		Emma.pull(ConfigApp.OutXMLPath, mSgdEnvironnement);
		File outFile = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OutXMLPath);
		Emma.save_non_generic(outFile,
				new File(mSgdEnvironnement.getScenarioDirectory()));

	}

	/**
	 * 
	 */
	private void save_time() {
		Emma.pull(ConfigApp.TIME_PATH, mSgdEnvironnement);
		File timeFile = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.TIME_PATH);
		Emma.savegeneric_file(timeFile,
				new File(mSgdEnvironnement.getTimeDirectory()));
	}

	private void checkIfTestOnDeviceIsFinished() {
		Emulator_checker checker = new Emulator_checker(this, mSgdEnvironnement);
		checker.run();
	}

	public void set_sga_is_finished(boolean b) {
		testIsfinshed = b;

	}

	private void info(String log) {
		Emma.info("[ Thread:" + Thread.currentThread() + "]" + log);
	}

	/**
	 * if new State: submit to TaskPoolManagr
	 * 
	 * @param result
	 * @throws CloneNotSupportedException
	 */
	@SuppressWarnings("unchecked")
	private void notifyNewTask(CrawlerTask result) {
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

						job = new AndroidCrawler(mSgdEnvironnement, task,
								getTaskManager(), mResult);

						if (ConfigApp.DEBUG) {
							System.out
									.println("=========================================== add a new job "
											+ "===========================================");
						}
						// job.setPheromone();
						if (ConfigApp.DEBUG) {
							System.out
									.println("====== job rank : "
											+ job.getRank()
											+ "===========================================");
						}
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

	@Override
	public void update_state(boolean status) {
		File ok = new File(mSgdEnvironnement.getOutDirectory()
				+ ConfigApp.OkPath);
		Emma.save_generic_ok(mSgdEnvironnement, ok);
		Emma.delete_File_onDevice(ConfigApp.OkPath, mSgdEnvironnement,
				new ThreadSleeper());
		Emma.save_generic_rv_done(mSgdEnvironnement);
		Emma.save_genericTime(mSgdEnvironnement);
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

}
