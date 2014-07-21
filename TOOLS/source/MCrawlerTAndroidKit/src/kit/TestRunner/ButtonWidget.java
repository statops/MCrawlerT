package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import com.robotium.solo.Solo;

@SuppressLint("ViewConstructor")
public class ButtonWidget extends SgdView {

	public ButtonWidget(Context context, String currentActivityName, View currentViewTocross, Transition tr,
			Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);

	}

	/**
 * 
 */
	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleButton();
		mErrorStatus = false;

	}

	private final static String TAG = ButtonWidget.class.getSimpleName();

	/**
	 * handle button widet
	 * 
	 * @param currentViewTocross
	 * @throws InterruptedException
	 */
	private void handleButton() {
		if (!mActionStatus) {
			clickButton();
			if (Config.DEBUG) {
				Log.e("button", "click on button");
			}
			try {
				waitAction();
				waitAction();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return;
		}
		try {
			if (SgUtils.isRadioButtonView(mCurrentView)) {
				addTransitionAction(RadioButton.class.cast(mCurrentView), null, mTransition);
			} else {
				addTransitionAction(Button.class.cast(mCurrentView), null, mTransition);
			}

		} catch (android.content.res.Resources.NotFoundException ns) {
			if (Config.DEBUG) {
				Log.e(TAG, "handleButton ");
				Log.e(TAG, ns.getMessage());
				ns.printStackTrace();
			}
			return;
		}

		if (isValidTransitionInScenarioData(mTransition, mScenarioData)) {
			if (Config.DEBUG) {
				Log.e(TAG, "handleButton : Transition is valid");
			}
			try {
				saveTransition();
				clickButton();
				waitAction();
				waitAction();
			} catch (Error err) {
				if (Config.DEBUG) {
					Log.e(TAG, "Button Click error : ");
					err.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			if (Config.DEBUG) {
				Log.e(TAG, "handleButton : Transition is not valid");
			}
		}
	}

	/**
	 * 
	 */
	private void clickButton() {
		if (Config.DEBUG) {
			Log.e("button", "before click on button");
		}
		View button = Button.class.cast(mCurrentView);
		String id = getWidgetId(button);
		mSolo.clickOnView(mSolo.getView(id));
		if (Config.DEBUG) {
			Log.d(TAG, "button clicked");
		}
	}

}
