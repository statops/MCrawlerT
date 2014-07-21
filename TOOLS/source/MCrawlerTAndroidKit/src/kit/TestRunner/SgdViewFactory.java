package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import kit.Utils.SgUtils;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.robotium.solo.Solo;

public class SgdViewFactory {

	private static final String TAG = null;

	public static SgdView createView(Context context,
			Activity currentActivityName, View currentViewTocross,
			Transition tr, Solo solo) {
		if (currentActivityName == null) {
			return null;
		}
		if (SgUtils.isListElementView(currentViewTocross)) {
			return new ListViewElementWidget(context, currentActivityName
					.getClass().getSimpleName(),
					ListViewElement.class.cast(currentViewTocross), tr, solo);
		}
		if (SgUtils.isMenu(currentViewTocross)) {
			return new MenuWidget(context, currentActivityName.getClass()
					.getSimpleName(), currentViewTocross, tr, solo);
		}

		if (SgUtils.isImageView(currentViewTocross)) {
			/**
			 * 
			 */
			if (Config.DEBUG) {
				Log.d(TAG, "create Image View Widget " + currentViewTocross);

			}
			if (currentViewTocross.getVisibility() != View.INVISIBLE) {
				return new ImageWidget(context, currentActivityName.getClass()
						.getSimpleName(), currentViewTocross, tr, solo);

			}
			if (Config.DEBUG) {
				Log.d("Visibility", "not Visible ");

			}

		}
		if (SgUtils.isCheckBox(currentViewTocross)) {
			if (currentViewTocross.getVisibility() != View.INVISIBLE) {
				return new CheckboxWidget(context, currentActivityName
						.getClass().getSimpleName(), currentViewTocross, tr,
						solo);

			}
			if (Config.DEBUG) {
				Log.d("Visibility", "not Visible ");

			}
		}
		if (SgUtils.isButtonView(currentViewTocross)) {
			if (currentViewTocross.getVisibility() != View.INVISIBLE) {
				return new ButtonWidget(context, currentActivityName.getClass()
						.getSimpleName(), currentViewTocross, tr, solo);

			}
			if (Config.DEBUG) {
				Log.d("Visibility", "not Visible ");

			}
		}

		if (SgUtils.isTextView(currentViewTocross)) {
			if (currentViewTocross.getVisibility() != View.INVISIBLE) {
				return new TextViewWidget(context, currentActivityName
						.getClass().getSimpleName(), currentViewTocross, tr,
						solo);

			}
			if (Config.DEBUG) {
				Log.d("Visibility", "not Visible ");

			}
		}

		if (SgUtils.isListView(currentViewTocross)) {
			if (currentViewTocross.getVisibility() != View.INVISIBLE) {
				return new ListViewWidget(context, currentActivityName
						.getClass().getSimpleName(), currentViewTocross, tr,
						solo);

			}
			if (Config.DEBUG) {
				Log.d("Visibility", "not Visible ");

			}
		}
		
		
		if (SgUtils.isClickable(currentViewTocross)) {
			if (currentViewTocross.getVisibility() != View.INVISIBLE) {
				return new ClickableView(context, currentActivityName.getClass()
						.getSimpleName(), currentViewTocross, tr, solo);

			}
		}
		
		
		
		return null;
	}

}
