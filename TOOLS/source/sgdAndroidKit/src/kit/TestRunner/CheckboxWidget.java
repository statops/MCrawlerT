package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import com.robotium.solo.Solo;

public class CheckboxWidget extends SgdView {

	public CheckboxWidget(Context context, String currentActivityName, View currentViewTocross,
			Transition tr, Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);

	}

	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleCheckBox();
		mErrorStatus = false;

	}

	/**
	 * Check the box
	 * 
	 * @param currentViewTocross
	 * @param tr
	 * @throws InterruptedException
	 */
	private void handleCheckBox() {
		CheckBox box = CheckBox.class.cast(mCurrentView);
		if (!mActionStatus) {
			CheckBox();

			if (Config.DEBUG) {
				Log.e("check", "click on Check");
			}
			return;
		}
		try {
			addTransitionAction(box, "" + box.isEnabled(), mTransition);
		} catch (android.content.res.Resources.NotFoundException ns) {
			return;
		}
		if (isValidTransitionInScenarioData(mTransition, mScenarioData))
			try {
				saveTransition();
				CheckBox();
				if (Config.DEBUG) {
					Log.e("check", "click on Check");
				}
			} catch (Error err) {
				err.printStackTrace();
				if (Config.DEBUG) {
					Log.e("check", "click on Check");
				}
			}

	}

	/**
	 * @throws InterruptedException
	 * 
	 */
	private void CheckBox() {
		if (Config.DEBUG) {
			Log.e("check box", "before click on checkBox");
		}
		View checkBox = CheckBox.class.cast(mCurrentView);
		String id = getWidgetId(checkBox);
		mSolo.clickOnView(mSolo.getView(id));
		if (Config.DEBUG) {
			Log.d("check box", "checked");
		}
		try {
			waitAction();
		} catch (InterruptedException e) {
		} catch (Exception e) {
			if (Config.DEBUG) {
				Log.e("check", "click on Check error");
			}
		}
	}
}
