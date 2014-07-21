package kit.TestRunner;

import kit.Config.Config;
import kit.Scenario.Transition;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.robotium.solo.Solo;

public class ImageWidget extends SgdView {

	public ImageWidget(Context context, String currentActivityName, View currentViewTocross, Transition tr,
			Solo solo) {
		super(context, currentActivityName, currentViewTocross, tr, solo);

	}

	@Override
	public void performAction(boolean status) {
		mActionStatus = status;
		check_error_status();
		handleImageView();
		mErrorStatus = false;

	}

	private void handleImageView() {
		if (!mActionStatus) {
			clickImage();
			return;
		}
		try {
			if (Config.DEBUG) {
				Log.e("image", "add name of widget on Image");
			}
			addTransitionAction(ImageView.class.cast(mCurrentView), null, mTransition);
		} catch (android.content.res.Resources.NotFoundException ns) {
			if (Config.DEBUG) {
				Log.e("image", "add name of widget on Image failed");
				Log.e("image", "image will not be handled");
			}
			return;
		}
		if (isValidTransitionInScenarioData(mTransition, mScenarioData)) {
			try {
				saveTransition();
				clickImage();
			} catch (Error err) {
				err.printStackTrace();
			} catch (Exception e) {
				if (Config.DEBUG) {
					Log.e("check", "click on Image error");
				}
			}
		}

	}

	/**
	 * 
	 */
	private void clickImage() {
		if (Config.DEBUG) {
			Log.e("image", "before click on Image");
		}
		View image = ImageView.class.cast(mCurrentView);
		String id = getWidgetId(image);
		mSolo.clickOnView(mSolo.getView(id));
		if (Config.DEBUG) {
			Log.e("image", "click on Image");
		}
		try {
			waitAction();
		} catch (InterruptedException e) {
			if (Config.DEBUG) {
				Log.e("check", "click on Image error");
			}
			e.printStackTrace();
		}

	}

}
