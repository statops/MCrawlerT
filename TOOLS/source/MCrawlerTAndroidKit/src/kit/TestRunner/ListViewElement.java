package kit.TestRunner;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

public class ListViewElement extends View {
	private String mName;
	private ListView mParent;
	private View mView;

	public ListViewElement(Context context, ListView parent, View view, int index) {
		super(context);
		mName = "" + index;
		mParent = parent;
		mView = view;

	}

	public String getIndex() {
		return mName;
	}

	public View getListParent() {
		return mParent;
	}

	/**
	 * @return
	 */
	public View getView() {
		return mView;
	}

}
