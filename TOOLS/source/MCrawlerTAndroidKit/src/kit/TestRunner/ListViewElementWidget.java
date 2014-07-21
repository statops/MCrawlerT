package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.robotium.solo.Solo;

public class ListViewElementWidget extends SgdView {

	public ListViewElementWidget(Context context, String currentActivityName, View currentViewTocross,
			Transition tr, Solo solo) {
		super(context, currentActivityName, ((ListViewElement) currentViewTocross), tr, solo);
	}

	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleListElement();
		mErrorStatus = false;

	}

	/**
	 * Check the box
	 * 
	 * @param currentViewTocross
	 * @param tr
	 */
	private void handleListElement() {

		ListViewElement listElt = (ListViewElement) mCurrentView;
		if (!mActionStatus) {
			clickListElement(listElt);
			return;
		}
		try {
			addTransitionAction(listElt, listElt.getIndex(), mTransition);
		} catch (android.content.res.Resources.NotFoundException ns) {
			return;
		}
		if (isValidTransitionInScenarioData(mTransition, mScenarioData)) {
			saveTransition();
			// get_view_at_index((ListView) listElt.getParent(),
			// Integer.parseInt(listElt.getIndex()));
			clickListElement(listElt);
			try {
				waitAction();
			} catch (Exception e) {
				if (Config.DEBUG) {
					Log.e("check", "click on List error");
				}
			}

		}
	}

	/**
	 * @param listElt
	 * 
	 */
	private void clickListElement(ListViewElement listElt) {
		if (Config.DEBUG) {
			Log.e("list", "before click on List");
		}
		mSolo.clickOnView(listElt.getView());
		if (Config.DEBUG) {
			Log.e("list", "click on List index" + listElt.getIndex());
		}

	}

}
