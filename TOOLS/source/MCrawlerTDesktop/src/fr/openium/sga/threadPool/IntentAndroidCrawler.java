package fr.openium.sga.threadPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kit.Command.PushCommand;
import kit.Intent.IntentXmlGenerator;
import kit.Intent.MCrawlerTIntent;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.taskPool.ITask;
import fr.openium.taskPool.ITaskManager;

public class IntentAndroidCrawler extends AndroidCrawler {
	private MCrawlerTIntent mLauncherIntent;

	public IntentAndroidCrawler(SgdEnvironnement sgdEnvironnement, ITask tasks,
			ITaskManager<AndroidCrawler> manager, CrawlResult result,
			MCrawlerTIntent launcherIntent) throws CloneNotSupportedException {
		super(sgdEnvironnement, tasks, manager, result);
		mLauncherIntent = launcherIntent;
	}

	@Override
	protected void init_To_perform_Task() throws Exception {

		if (ConfigApp.DEBUG) {
			System.out.println("init_To_perform_Task()");
		}
		/**
		 * the task to perform
		 */
		IntentCrawlerTask toPerform = (IntentCrawlerTask) mTask;
		/**
		 * push scenario to Device
		 */
		pushScenario(toPerform);

		/**
		 * push launcherIntent
		 */
		pushLauncherIntent(toPerform);

	}

	@Override
	protected void launchSgaWithClassDefined() throws InterruptedException {
		mSgdEnvironnement.setTargetClass(mSgdEnvironnement.getProjectPackape()
				+ "" + ConfigApp.INTENTCRAWLER_TEST);
		super.launchSgaWithClassDefined();
	}

	private boolean pushLauncherIntent(IntentCrawlerTask toPerform)
			throws IOException {

		if (mLauncherIntent == null) {
			if (ConfigApp.DEBUG) {
				System.out.println("Intent to commit is null  " + "\n");
			}
			return false;
		}
		if (ConfigApp.DEBUG) {
			System.out.println("*****Push intent " + "\n");
		}

		if (mLauncherIntent != null && mLauncherIntent.getActions() != null
				&& !mLauncherIntent.getActions().isEmpty()
				&& mLauncherIntent.getComponentName() != null) {
			if (ConfigApp.DEBUG) {
				System.out.println("Intent to commit into device :  " + "\n"
						+ mLauncherIntent.getActions().iterator().next() + "\n"
						+ mLauncherIntent.getComponentName());
			}
			String output = (mSgdEnvironnement.getScenarioDirectory()
					+ File.separator + ConfigApp.INTENTXML);
			new IntentXmlGenerator(mLauncherIntent, new File(output))
					.generateXml();
			if (!new File(output).exists()) {
				throw new IllegalStateException("Invalid intent:  "
						+ "intent xml generation failed");
			}
			PushCommand push = new PushCommand(mSgdEnvironnement.getAdb(),
					mSgdEnvironnement.getDevice(), output,
					ConfigApp.DEVICETESTRESULTS);
			if (ConfigApp.DEBUG) {
				System.out.println("Push:  " + output);
			}
			push.execute();
			// new File(output).delete();
		} else {
			throw new IllegalStateException("Invalid intent to push  "
					+ mLauncherIntent);
		}
		return true;
	}

	@Override
	protected void checkIfNewStateIsAvailable() throws IOException {
		/*
		 * handle error execution. If first Launch error with Intent create an
		 * simplified scenario with intent action to notify error
		 * 
		 * s1 == (a1 = intent + error) ==> s2End
		 */
		/**
		 * readLogfile
		 */

		super.checkIfNewStateIsAvailable();

	}

	@Override
	protected IntentAndroidCrawler getNewTask(CrawlerTask intenttask)
			throws CloneNotSupportedException {
		return new IntentAndroidCrawler(mSgdEnvironnement.clone(),
				new IntentCrawlerTask(intenttask), getTaskManager(), mResult,
				mLauncherIntent);
	}

	@Override
	protected void notifyNewTask(CrawlerTask result) {
		/**
		 * le fichier out.xml n'existe pas
		 */
		if (result == null) {
			mTaskManager.update(null);
			return;
		}
		List<IntentAndroidCrawler> tasksList = new ArrayList<IntentAndroidCrawler>();
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
				IntentAndroidCrawler job = null;
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
}
