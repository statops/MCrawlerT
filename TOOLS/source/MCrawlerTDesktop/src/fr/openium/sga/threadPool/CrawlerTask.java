package fr.openium.sga.threadPool;

import java.util.HashSet;

import kit.Scenario.ScenarioData;
import kit.Scenario.Widget;
import fr.openium.sga.emmatest.SgdEnvironnement;
import fr.openium.taskPool.ITask;

public class CrawlerTask implements ITask, Comparable<CrawlerTask> {
	public String emulateur;
	protected String mActivityName;
	protected HashSet<Widget> mWigets;
	protected ScenarioData mPath;
	protected boolean userEnvironment;
	protected int mRank;

	public CrawlerTask(SgdEnvironnement mSgdEnvironnement, String mainActivity, Object object,
			Object object2, boolean b, int value) {
		mRank = value;
	}

	public CrawlerTask(String ActivityNAme, HashSet<Widget> wigs, ScenarioData path, boolean env, int rank) {
		/*
		 * 
		 * à gerer si Acti,wigs, path sont null
		 */
		mActivityName = ActivityNAme;
		mWigets = wigs;
		mPath = path;
		userEnvironment = env;
		mRank = rank;
	}

	/**
	 * @param scen
	 * @param mSgdEnvironnement
	 */
	public CrawlerTask(ScenarioData scen) {
		mPath = scen;
	}

	public String getActivityName() {
		return mActivityName;
	}

	public void setActivityName(String activityName) {
		mActivityName = activityName;
	}

	public HashSet<Widget> getWigets() {
		return mWigets;
	}

	public void setWigets(HashSet<Widget> mWigets) {
		this.mWigets = mWigets;
	}

	public ScenarioData getPath() {
		return mPath;
	}

	public void setPath(ScenarioData mPath) {
		this.mPath = mPath;
	}

	public boolean isUserEnvironment() {
		return userEnvironment;
	}

	public void setUserEnvironment(boolean userEnvironment) {
		this.userEnvironment = userEnvironment;
	}

	@Override
	public int getRank() {
		return mRank;
	}

	@Override
	public int compareTo(CrawlerTask arg0) {

		if (this.getRank() < arg0.getRank()) {
			return 1;
		}
		if (this.getRank() == arg0.getRank()) {
			return 0;
		}
		return -1;
	}

	public int getId() {
		return mActivityName.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see fr.openium.taskPool.ITask#setRank(int)
	 */
	@Override
	public void setRank(int i) {
		mRank = mRank + i;

	}

	public void setPheromone(int i) {
		/*
		 * if (mRank == 0) mRank = (mPath == null || mPath.getTransitions() ==
		 * null) ? 0 : mPath.getTransitions().size();
		 */
		if (i == 0) {
			mRank++;
		}
		mRank = mRank + i;
	}

}
