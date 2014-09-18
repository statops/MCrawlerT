/**
 * 
 */
package kit.TestRunner;

import kit.Scenario.Transition;
import android.content.Context;
import android.view.View;

import com.robotium.solo.Solo;

/**
 * @author Stassia
 * 
 */
public class ActionBarView extends SgdView {
	/**
	 * @param context
	 * @param currentActivityName
	 * @param currentViewTocross
	 * @param tr
	 * @param solo
	 */
	public ActionBarView(Context context, String currentActivityName, View currentViewTocross, Transition tr,
			Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);
	}

	@SuppressWarnings("unused")
	private static final String TAG = ActionBarView.class.getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see kit.TestRunner.SgdView#performAction(boolean)
	 */
	@Override
	public void performAction(boolean status) {
		// TODO Auto-generated method stub

	}
}
