package fr.openium.sga.strategy;

import java.io.IOException;
import java.util.ArrayList;

import kit.Intent.MCrawlerTIntent;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.threadPool.AndroidCrawler;
import fr.openium.sga.threadPool.IntentAndroidCrawler;
import fr.openium.sga.threadPool.IntentCrawlerTask;

public class IntentLauncherStrategy extends FourmiStrategy {
	public static final int _STRATEGY_ID = 500;
	private MCrawlerTIntent mLauncherIntent;

	public IntentLauncherStrategy(SgdEnvironnement env, String initActivity,
			MCrawlerTIntent launcherIntent) {
		super(env, initActivity);
		mLauncherIntent = launcherIntent;
	}

	@Override
	protected ArrayList<AndroidCrawler> get_init_Tasks()
			throws CloneNotSupportedException, IOException {
		/**
		 * serialiser
		 * 
		 */
		IntentAndroidCrawler init_task1 = new IntentAndroidCrawler(
				mSgdEnvironnement.clone(), new IntentCrawlerTask(mainActivity,
						null, null, true, 0), tm, mResult, mLauncherIntent);
		ArrayList<AndroidCrawler> initList = new ArrayList<AndroidCrawler>();
		initList.add(init_task1);
		return initList;

	}

}
