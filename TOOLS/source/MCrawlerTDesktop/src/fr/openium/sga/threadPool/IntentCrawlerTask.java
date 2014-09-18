package fr.openium.sga.threadPool;

import java.util.HashSet;

import kit.Scenario.ScenarioData;
import kit.Scenario.Widget;

public class IntentCrawlerTask extends CrawlerTask {

	public IntentCrawlerTask(String ActivityNAme, HashSet<Widget> wigs,
			ScenarioData path, boolean env, int rank) {
		super(ActivityNAme, wigs, path, env, rank);
	}

	public IntentCrawlerTask(CrawlerTask intenttask) {
		super(intenttask.getActivityName(), intenttask.getWigets(), intenttask
				.getPath(), intenttask.isUserEnvironment(), intenttask
				.getRank());
	}

}
