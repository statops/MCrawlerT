package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.robotium.solo.Solo;

public class ClickableView extends SgdView {

	public ClickableView(Context context, String currentActivityName,
			View currentViewTocross, Transition tr, Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);
	}

	@Override
	public void performAction(boolean status) {
		mActionStatus = status;

		check_error_status();
		handleCustomView();
		mErrorStatus = false;

	}

	private void handleCustomView() {
		if (!mActionStatus) {
			clickLinearLayout();
			if (Config.DEBUG) {
				Log.e("CustomView", "click on customView");
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

			addTransitionAction(mCurrentView, null, mTransition);

		} catch (android.content.res.Resources.NotFoundException ns) {

			return;
		}
		if (isValidTransitionInScenarioData(mTransition, mScenarioData)) {
			try {
				saveTransition();
				clickLinearLayout();
				waitAction();
				waitAction();
			} catch (Error err) {
				if (Config.DEBUG) {
					err.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			if (Config.DEBUG) {
				Log.e("CustomView", "handleButton : Transition is not valid");
			}
		}

	}

	private void clickLinearLayout() {
		if (Config.DEBUG) {
			Log.e("button", "befor CustomView clicked");
		}
		String id = getWidgetId(mCurrentView);
		mSolo.clickOnView(mSolo.getView(id));
		if (Config.DEBUG) {
			Log.d("CustomView", "CustomView clicked");
		}

	}

}
