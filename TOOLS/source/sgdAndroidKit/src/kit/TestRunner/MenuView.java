/**
 * 
 */
package kit.TestRunner;

import android.app.Activity;
import android.content.Context;
import android.view.View;

/**
 * @author Stassia
 * 
 */
public class MenuView extends View {
	/**
	 * @param context
	 */
	private final int mIndex;
	private final Activity mActivity;

	public MenuView(Context context, int index, Activity activity) {
		super(context);
		mIndex = index;
		mActivity = activity;
	}

	public int getIndex() {
		return mIndex;
	}

	public Activity getActivity() {
		return mActivity;
	}

	@SuppressWarnings("unused")
	private static final String TAG = MenuView.class.getName();
}
