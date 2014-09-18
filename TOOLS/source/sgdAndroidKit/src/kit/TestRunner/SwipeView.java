/**
 * 
 */
package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.View;

import com.robotium.solo.Solo;

/**
 * @author Stassia
 * 
 */
public class SwipeView extends SgdView {
	/**
	 * @param context
	 * @param currentActivityName
	 * @param currentViewTocross
	 * @param tr
	 * @param solo
	 */
	public SwipeView(Context context, String currentActivityName, View currentViewTocross, Transition tr,
			Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);
		
	}

	private static final String TAG = SwipeView.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.SgdView#performAction(boolean)
	 */
	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleSwipe();
		mErrorStatus = false;

	}

	/**
	 * 
	 */
	@SuppressWarnings("deprecation")
	private void handleSwipe() {
		Display display = mSolo.getCurrentActivity().getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		float xStart = width - 10;
		float xEnd = 10;
		if (Config.DEBUG) {
			Log.d(TAG, "handleSwipe ");
		}
		mSolo.drag(xStart, xEnd, height / 2, height / 2, 1);

	}
}
