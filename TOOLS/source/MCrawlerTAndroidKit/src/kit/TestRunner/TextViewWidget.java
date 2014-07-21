package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.robotium.solo.Solo;

public class TextViewWidget extends SgdView {

	public TextViewWidget(Context context, String currentActivityName, View currentViewTocross,
			Transition tr, Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);

	}

	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleTextView();
		mErrorStatus = false;

	}

	/**
	 * 
	 */
	private void clickText() {
		if (Config.DEBUG) {
			Log.e("TextView", "before click on text");
		}
		TextView text = TextView.class.cast(mCurrentView);
		String id = getWidgetId(text);
		mSolo.clickOnView(mSolo.getView(id));
		/**
		 * try click on text
		 */
		//mSolo.clickOnText((String) text.getText());
		/**
		 * click on parentview
		 */
		//View parent=(View) text.getParent();
		//if (Config.DEBUG) {
		//	Log.e("TextView", "**********click On Parent ********");
		//}
		//mSolo.clickOnView(parent);
		//if (Config.DEBUG) {
			//Log.e("TextView", "**********click On Parent ********");
		//}
		if (Config.DEBUG) {
			Log.d("TextVuew", "text clicked");
		}
		try {
			waitAction();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private final static String TAG = TextViewWidget.class.getSimpleName();

	private void handleTextView() {
		if (!mActionStatus) {
			clickText();
			return;
		}

		try {
			addTransitionAction(TextView.class.cast(mCurrentView), SgUtils.getValue(mCurrentView, mContext),
					mTransition);
		} catch (android.content.res.Resources.NotFoundException ns) {
			if (Config.DEBUG) {
				Log.e(TAG, "handleText ");
				Log.e(TAG, ns.getMessage());
				ns.printStackTrace();
			}
			return;
		}

		if (isValidTransitionInScenarioData(mTransition, mScenarioData)) {
			try {
				saveTransition();
				clickText();
			} catch (Error err) {
				if (Config.DEBUG) {
					err.printStackTrace();
				}
			}
		}

	}
}
