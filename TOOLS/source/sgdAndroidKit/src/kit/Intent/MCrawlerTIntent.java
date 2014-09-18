package kit.Intent;

import java.util.ArrayList;
import java.util.HashSet;

public class MCrawlerTIntent {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((mActions == null) ? 0 : mActions.hashCode());
		result = prime * result
				+ ((mCategories == null) ? 0 : mCategories.hashCode());
		result = prime * result
				+ ((mComponentName == null) ? 0 : mComponentName.hashCode());
		result = prime * result
				+ ((mIntentData == null) ? 0 : mIntentData.hashCode());
		return result;
	}

	public String id() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((mActions == null || mActions.isEmpty()) ? 0 : mActions
						.iterator().next().hashCode());
		result = prime
				* result
				+ ((mCategories == null || mCategories.isEmpty()) ? 0
						: mCategories.iterator().next().hashCode());
		result = prime * result
				+ ((mComponentName == null) ? 0 : mComponentName.hashCode());
		result = prime
				* result
				+ ((mIntentData == null || mIntentData.isEmpty()) ? 0
						: mIntentData.get(0).toString().hashCode());
		return INTENT + "_" + result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MCrawlerTIntent other = (MCrawlerTIntent) obj;
		if (mActions == null) {
			if (other.mActions != null)
				return false;
		} else if (!mActions.equals(other.mActions))
			return false;
		if (mCategories == null) {
			if (other.mCategories != null)
				return false;
		} else if (!mCategories.equals(other.mCategories))
			return false;
		if (mComponentName == null) {
			if (other.mComponentName != null)
				return false;
		} else if (!mComponentName.equals(other.mComponentName))
			return false;
		if (mIntentData == null) {
			if (other.mIntentData != null)
				return false;
		} else if (!mIntentData.equals(other.mIntentData))
			return false;
		return true;
	}

	public static final String INTENT = "intent";
	private ArrayList<IntentData> mIntentData = new ArrayList<MCrawlerTIntent.IntentData>();
	private HashSet<String> mActions = new HashSet<String>();
	private HashSet<String> mCategories = new HashSet<String>();
	private String mComponentName;

	public MCrawlerTIntent(String action) {
		addAction(action);
	}

	public MCrawlerTIntent(String action, String category) {
		addAction(action);
		addCategory(category);
	}

	public void addCategory(String category) {
		mCategories.add(category);

	}

	public MCrawlerTIntent() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * the Class that represent the data field of the intent
	 */
	public static final class IntentData {
		// private String mIntentType;
		private final String mHost;
		private final String mMimeType;
		private String mPath;
		private final String mScheme;

		public IntentData(String host, String scheme, String mimetype,
				String path) {
			mHost = host;
			mMimeType = mimetype;
			mScheme = scheme;
			setPath(path);

		}

		public String getHost() {
			return mHost;
		}

		public String getMimeType() {
			return mMimeType;
		}

		public String getPath() {
			return mPath;
		}

		public void setPath(String mPath) {
			this.mPath = mPath;
		}

		public String getScheme() {
			return mScheme;
		}

		@Override
		public String toString() {
			if (mScheme == null) {
				return "";
			}

			return mScheme + "://" + (mHost == null ? "" : mHost) + "/"
					+ (mPath == null ? "" : mPath);
		}

	}

	public void setIntentData(IntentData data) {
		if (data == null) {
			return;
		}

		mIntentData.add(data);

	}

	public void addAction(String action) {
		if (action == null || action.equalsIgnoreCase("")) {
			return;
		}
		mActions.add(action);

	}

	public HashSet<String> getActions() {
		return mActions;

	}

	public HashSet<String> getCategories() {
		return mCategories;
	}

	public ArrayList<IntentData> getIntentData() {
		return mIntentData;
	}

	public String getComponentName() {
		return mComponentName;
	}

	public void setComponentName(String comp) {
		this.mComponentName = comp;
	}

	public static class ActionElements {
		public final static String ACTION = "action";
		public final static String NAME = "name";
	}

	public static class ComponentElements {
		public final static String COMPONONENT = "component";
		public final static String NAME = "name";
	}

	public static class CategoryElements {
		public final static String NAME = "name";
		public static final String CATEGORY = "category";
	}

	public static class DataElements {
		public final static String URI = "uri";
		public final static String HOST = "host";
		public final static String SCHEME = "scheme";
		public final static String TYPE = "type";
		public final static String PATH = "path";
		public static final String DATA = "data";

	}

}
