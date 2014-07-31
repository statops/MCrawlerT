package fr.openium.sga.strategy;

import java.io.File;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;

public class FourmiStrategy extends AntStrategy {
	/**
	 * 
	 * @param env
	 * @param initActivity
	 */
	public FourmiStrategy(SgdEnvironnement env, String initActivity) {
		super(env, initActivity);
		mResult = new CrawlResult(new File(mSgdEnvironnement.getOutDirectory()), this);
	}

	/**
	 * @param st
	 * @param path
	 * @return
	 */
	public int getRank(State st, ScenarioData path) {
		return ((path != null && path.getTransitions() != null && !path.getTransitions().isEmpty()) ? path
				.getTransitions().size() : DEFAULT_RANK);
	}

}