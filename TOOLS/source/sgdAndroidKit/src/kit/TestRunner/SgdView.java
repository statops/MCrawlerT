package kit.TestRunner;

import kit.Config.Config;
import kit.Database.RemoteProviderConfig;
import kit.Scenario.Scenario;
import kit.Scenario.ScenarioData;
import kit.Scenario.ScenarioGenerator;
import kit.Scenario.State;
import kit.Scenario.Transition;
import kit.Scenario.Tree;
import kit.Utils.SgUtils;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.robotium.solo.Solo;

public abstract class SgdView extends View {
	protected boolean mErrorStatus;
	protected boolean mActionStatus;
	protected Solo mSolo;
	protected String mActivityTargetName;
	protected Transition mTransition;
	protected View mCurrentView;
	protected int mIndex;
	protected Context mContext;
	protected ScenarioData mScenarioData;
	protected ScenarioGenerator mScenarioScenarioGenerator;

	public SgdView(Context context, String currentActivityName,
			View currentViewTocross, Transition tr, Solo solo) {
		super(context);
		mSolo = solo;
		mActivityTargetName = currentActivityName;
		mTransition = tr;
		mContext = context;
		mCurrentView = currentViewTocross;
		if (mCurrentView != null) {
			if (Config.DEBUG) {
				Log.i("SgdView ", "" + mCurrentView);
				Log.i("SgdView is Clickable ???",
						"" + mCurrentView.isClickable());
			}
		}
		initActiveUserEnvironment();
	}

	protected void check_error_status() {
		if (isTargetActivityStillValid()) {
			mErrorStatus = true;
		}
	}

	/**
	 * 
	 * @param status
	 *            : save in transition or not
	 */
	public abstract void performAction(boolean status);

	public boolean getStatus() {
		return mErrorStatus;
	}

	protected boolean isTargetActivityStillValid() {
		Activity aact = mSolo.getCurrentActivity();
		if (aact == null) {
			return false;
		}
		String currentAct = mSolo.getCurrentActivity().getClass()
				.getSimpleName();
		if (!currentAct.equalsIgnoreCase(mActivityTargetName))
			return true;
		return false;
	}

	/**
	 * 
	 * @param the
	 *            view to add in the transition
	 * @param value
	 *            inserted if editText
	 */
	public Transition addTransitionAction(View v, String value, Transition tr) {
		return SgUtils.addTransitionAction(v, value, tr, mContext);
	}

	/**
	 * Check if the action in the transition leads to END state
	 * 
	 * @param tr
	 *            : the transition that will be performed
	 * @param widgetName
	 *            : the name of the actual widget
	 * @return - true : if the transition's destination state is not END
	 * 
	 *         - false : if the transition leads to END state
	 */
	protected boolean isValidTransitionInScenarioData(Transition tr,
			ScenarioData scenario) {
		if (tr == null) {
			return true;
		}
		if (SgUtils.isValidTransitonIn(scenario, tr)) {
			return true;
		}
		return false;
	}

	public void setScenarioData(ScenarioData scenario) {
		mScenarioData = scenario;
	}

	protected void saveTransition() {
		if (Config.DEBUG) {
			Log.e("SgdView", "save transition");
		}
		/**
		 * add transition to currentTree
		 */
		/**
		 * add end Dest
		 */
		State end = mTransition.getDest();// end
		end.setId(mTransition.getId());
		end.setType(Scenario.DEST);
		mScenarioData.addStates(end);
		mCurrentTree.setTransitions(mTransition, true);
		mScenarioData.setTrees(mCurrentTree, true);
		SgUtils.saveScenario(mScenarioScenarioGenerator, mScenarioData);
		if (Config.DEBUG) {
			Log.e("", "Scenarion saved ");
			if (mScenarioData != null)
				Log.e("", mScenarioData.toString());

		}
	}

	public void setScenarioGenerator(ScenarioGenerator generator) {
		mScenarioScenarioGenerator = generator;

	}

	protected Tree mCurrentTree;

	public void setCurrenTree(Tree current_Tree) {
		mCurrentTree = current_Tree;

	}

	/**
	 * @return the id of the handled widget
	 */
	protected String getWidgetId(View view) {
		return mContext.getResources().getResourceName(view.getId())
				.replaceAll("id/", "");
	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	protected void waitAction() throws InterruptedException {
		Thread.sleep(2000);
	}

	protected void initActiveUserEnvironment() {
		ContentValues value_to_update = new ContentValues();
		value_to_update.put("status", "false");
		value_to_update.put("lock", "false");

		/*
		 * 
		 * mis ˆ jour cp
		 */
		mContext.getContentResolver().update(
				RemoteProviderConfig.SGA_CP_CONTENT_URI, value_to_update, null,
				null);

		/*
		 * 
		 * mis ˆ jour bd
		 */
		mContext.getContentResolver().update(
				RemoteProviderConfig.SGA_BD_CONTENT_URI, value_to_update, null,
				null);

	}

}
