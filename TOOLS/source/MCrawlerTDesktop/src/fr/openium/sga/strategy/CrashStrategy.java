/**
 * 
 */
package fr.openium.sga.strategy;

import java.io.File;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.sga.result.CrawlResult;

/**
 * @author Stassia
 * 
 */
public class CrashStrategy extends AntStrategy implements IStrategy {
	/**
	 * @param arg
	 */
	public CrashStrategy(SgdEnvironnement env, String initactivity) {
		super(env, initactivity);
		mResult = new CrawlResult(new File(mSgdEnvironnement.getOutDirectory()), this);
	}

	private static final String TAG = CrashStrategy.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.strategy.IStrategy#getResult()
	 */
	@Override
	public CrawlResult getResult() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.sga.strategy.IStrategy#updateResult(java.lang.Object)
	 */
	@Override
	public boolean updateResult(Object result) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param st
	 * @param path
	 * @return
	 */
	public int getRank(State st, ScenarioData path) {
		// TODO Auto-generated method stub
		return 0;
	}
}
