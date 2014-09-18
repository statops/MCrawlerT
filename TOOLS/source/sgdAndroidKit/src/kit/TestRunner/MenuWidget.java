/**
 * 
 */
package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.robotium.solo.Solo;

/**
 * @author Stassia
 * 
 */
public class MenuWidget extends SgdView {
	/**
	 * @param context
	 * @param currentActivityName
	 * @param currentViewTocross
	 * @param tr
	 * @param solo
	 */
	public MenuWidget(Context context, String currentActivityName, View currentViewTocross, Transition tr,
			Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);
	}

	private static final String TAG = MenuWidget.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.SgdView#performAction()
	 */
	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleMenu();
		mErrorStatus = false;

	}

	/**
	 * 
	 */
	private void handleMenu() {

		if (!mActionStatus) {
			clickMenu();
			return;
		}
		addTransitionAction(MenuView.class.cast(mCurrentView), null, mTransition);
		if (isValidTransitionInScenarioData(mTransition, mScenarioData)) {
			if (Config.DEBUG) {
				Log.e(TAG, "handleMenu : Transition is valid");
			}
			try {
				saveTransition();
				clickMenu();
				waitAction();
			} catch (Error err) {
				if (Config.DEBUG) {
					Log.e(TAG, "Menu click error");
					err.printStackTrace();
				}
			} catch (InterruptedException e) {
				if (Config.DEBUG) {
					Log.e(TAG, "Menu click error");
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 */
	private void clickMenu() {
		if (Config.DEBUG) {
			Log.e(TAG, "click on menu");
		}
		mSolo.pressMenuItem((MenuView.class.cast(mCurrentView)).getIndex());
		if (Config.DEBUG) {
			Log.e(TAG, "menu clicked");
		}
	}
}
