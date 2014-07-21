package kit.TestRunner;

import kit.Scenario.Transition;
import android.content.Context;
import android.view.View;

import com.robotium.solo.Solo;

public class ListViewWidget extends SgdView {

	public ListViewWidget(Context context, String currentActivityName, View currentViewTocross,
			Transition tr, Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);
		/**
		 * Split Listview
		 */
	}

	/**
	 * action on Listview is Peform by ListViewElement
	 */
	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		mErrorStatus = false;
	}

}
