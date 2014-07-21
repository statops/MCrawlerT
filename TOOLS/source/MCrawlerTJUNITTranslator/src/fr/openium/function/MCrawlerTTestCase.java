/**
 * 
 */
package fr.openium.function;

import java.util.ArrayList;

import kit.Scenario.ScenarioData;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.TestRunner.AbstractCrawler;
import kit.Utils.SgUtils;
import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import fr.openium.variable.MCrawlerVariable;
import fr.openium.variable.MCrawlerVariable.Action;

/**
 * @author Stassia
 * 
 */
public class MCrawlerTTestCase extends AbstractCrawler {
	/**
	 * @param pkg
	 * @param activityClass
	 */
	@SuppressWarnings("rawtypes")
	public MCrawlerTTestCase(String pkg, Class activityClass) {
		super(pkg, activityClass);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		mVariable = new MCrawlerVariable();
		super.setUp();
		if (!waitForActivity(INITIAL_STATE_ACTIVITY_FULL_CLASSNAME)) {
			assertTrue("Unreached activity " + INITIAL_STATE_ACTIVITY_FULL_CLASSNAME, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#setUp()
	 */

	protected fr.openium.function.MCrawlerFunctions mFCrawlerFunctions;
	protected fr.openium.variable.MCrawlerVariable mVariable;
	private static final String TAG = MCrawlerTTestCase.class.getName();

	public MCrawlerVariable.State getCurrentState() {
		Activity act = mSolo.getCurrentActivity();
		State source = new State(act.getClass().getName(), "");
		System.out.println("current state " + source.toString());
		source = SgUtils.addWidgetsInState(source, mSolo.getCurrentViews(), mContext);
		// source.setType(Scenario.SOURCE);
		return mVariable.new State(source);
	}

	/**
	 * @param l
	 * @param a1
	 * @param a2
	 */
	public void performAction(long l, Action... act) {
		MCrawlerFunctions.performAction(mContext, mSolo, l, act);
		waitProgressBar();
	}

	/**
	 * @param equalState
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#getEditTextSet(java.util.ArrayList,
	 * boolean)
	 */
	@Override
	protected ArrayList<String> getEditTextSet(ArrayList<EditText> currentViews, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * kit.TestRunner.AbstractCrawler#performActionOnWidgets(android.app.Activity
	 * , java.util.ArrayList, java.lang.String, kit.Scenario.Tree,
	 * kit.Scenario.State)
	 */
	@Override
	protected Tree performActionOnWidgets(Activity mainActivity, ArrayList<View> viewToHandle,
			String pairWiseSequence, Tree current_Tree, kit.Scenario.State current_state)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#handleInErrorViewCase(boolean,
	 * android.app.Activity, kit.Scenario.ScenarioData, kit.Scenario.Tree)
	 */
	@Override
	protected void handleInErrorViewCase(boolean viewError, Activity currentActivity,
			ScenarioData mScenarioData2, Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.AbstractCrawler#handleOtherCase(android.app.Activity,
	 * android.app.Activity, java.util.ArrayList, kit.Scenario.Transition,
	 * kit.Scenario.Tree)
	 */
	@Override
	protected void handleOtherCase(Activity currentActivity, Activity mainActivity,
			ArrayList<View> currentViews, Transition tr, Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * kit.TestRunner.AbstractCrawler#handleNewActivity(android.app.Activity,
	 * android.app.Activity, kit.Scenario.Transition, kit.Scenario.Tree)
	 */
	@Override
	protected void handleNewActivity(Activity currentActivity, Activity mainActivity, Transition tr,
			Tree current_Tree) throws InterruptedException {
		// TODO Auto-generated method stub

	}

}
