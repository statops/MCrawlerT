/**
 * 
 */
package fr.openium.sga.strategy;

import java.io.IOException;
import java.util.ArrayList;

import kit.Scenario.ScenarioGenerator;
import fr.openium.sga.ConfigApp;
import fr.openium.sga.emmatest.Emma;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;
import fr.openium.sga.threadPool.AndroidCrawler;
import fr.openium.sga.threadPool.CrawlerTask;
import fr.openium.taskPool.EmulatorManager;
import fr.openium.taskPool.ITaskManager;
import fr.openium.taskPool.TaskComparator;
import fr.openium.taskPool.TaskManagerRunnable;

/**
 * @author Stassia
 * 
 */
public abstract class AntStrategy extends AbstractStrategy {
	protected final int numberOfThread;
	protected final String mainActivity;
	protected ITaskManager<AndroidCrawler> tm;

	protected static long MAX_TIME = 20000000;
	protected static int MAX_RANK = 100;
	protected static int DEFAULT_RANK = 1;

	/**
	 * @param arg
	 */
	public AntStrategy(SgdEnvironnement env, String initActivity) {
		super(env);
		numberOfThread = env.getThread_number();
		mainActivity = initActivity;
		try {
			mSgdEnvironnement.initOutDirectories();
		} catch (IOException e) {
			e.printStackTrace();
		}
		MAX_TIME = (mSgdEnvironnement.getMaxTime() == 0L) ? MAX_TIME
				: mSgdEnvironnement.getMaxTime();
	}

	//private static final String TAG = AntStrategy.class.getName();

	@Override
	public CrawlResult getResult() throws Exception {
		/**
		 * installer les projets aux diff�rents �mulateurs
		 */
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
		tm = new TaskManagerRunnable<AndroidCrawler>(get_init_Tasks(),
				numberOfThread, new TaskComparator<AndroidCrawler>(),
				mSgdEnvironnement.getAdb());

		/**
		 * check available emulator : déjà effectué par le taskManager
		 */

		/**
		 * test emulator
		 */

		Long initTime = System.currentTimeMillis();
		tm.execute();
		int n = 1;
		boolean taskState = false;

		do {
			taskState = ((TaskManagerRunnable<AndroidCrawler>) tm)
					.isAllResultReceived();
			try {
				Thread.sleep(5000);
				n = n + 5;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			/*
			 * if (mResult != null && mResult.getScenarioData() != null &&
			 * mResult.getScenarioData().getTransitions().isEmpty() &&
			 * taskState) {
			 *//**
			 * create task with all states
			 */
			/*
			 * }
			 */
			if (Emma.limit_time_isReached(initTime, MAX_TIME)) {
				System.out.println("Time " + MAX_TIME + " seconds is reached");
				/**
				 * soft stop.
				 */
				tm.stop();
				break;
			}
		} while (!taskState);
		System.out.println("Wait of the end of executed tasks \n");
		do {
			taskState = ((TaskManagerRunnable<AndroidCrawler>) tm)
					.isTerminated();
			try {
				Thread.sleep(1000);
				n = n + 1;
				System.out.print("."); 
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!taskState);
		
		System.out.println("Test, finished after" + n + " seconds");
		/*
		 * fin de crawl
		 */

		/*
		 * Bissimulation
		 */
		mResult.compute_bissimilation();
		/*
		 * 
		 */
		/*for (String emulator : emManager.getList_Available_Emulator()) {
			mSgdEnvironnement.setDevice(emulator);
			if (!ConfigApp.DEBUG) {
				// mSgdEnvironnement.finish();
			}

		}*/
		ScenarioGenerator gen = new ScenarioGenerator(
				mSgdEnvironnement.getOutDirectory());
		gen.generateXml(mResult.getScenarioData());
		return mResult;
	}

	private ArrayList<AndroidCrawler> get_init_Tasks()
			throws CloneNotSupportedException, IOException {
		/**
		 * serialiser
		 * 
		 */
		AndroidCrawler init_task1 = new AndroidCrawler(
				mSgdEnvironnement.clone(), new CrawlerTask(mainActivity, null,
						null, true, 0), tm, mResult);
		ArrayList<AndroidCrawler> initList = new ArrayList<AndroidCrawler>();
		initList.add(init_task1);
		return initList;

	}

}
