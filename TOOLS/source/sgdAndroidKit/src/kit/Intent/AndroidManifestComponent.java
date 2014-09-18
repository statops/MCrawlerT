package kit.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/*
 * Component format present in AndroidManifetsFile
 * */

public class AndroidManifestComponent {
	/**
	 * the name of the component it is used as its ID
	 * */
	protected final String mName;
	protected final boolean mIsExported;
	protected ArrayList<MCrawlerTIntent> mIntents = new ArrayList<MCrawlerTIntent>();
	protected boolean mHasIntentFilter = false;
	protected boolean mHasInternalIntent = false;
	protected boolean mAccessContentProvider;
	private final String mType;
	/** Application package */
	private final String mPackage;

	Set<String> mProcesses = null;

	private HashMap<String, ArrayList<String>> mProperties = new HashMap<String, ArrayList<String>>();

	public AndroidManifestComponent(String name, boolean exported, String type,
			String pack) {
		mName = name;
		mIsExported = exported;
		mType = type;
		mPackage = pack;
	}

	public AndroidManifestComponent(String name, String type, String pack) {
		mName = name;
		mIsExported = false;
		mType = type;
		mPackage = pack;
	}

	public String getName() {
		return mName;
	}

	public boolean isExported() {
		return mIsExported;
	}

	/** set the List of Handled Intents */
	public void addIntent(MCrawlerTIntent intent) {
		mIntents.add(intent);

	}

	public boolean hasIntentFilter() {
		return mHasIntentFilter;
	}

	public void setHasIntentFilter(boolean mHasIntentFilter) {
		this.mHasIntentFilter = mHasIntentFilter;
	}

	public void setIntent(ArrayList<MCrawlerTIntent> intent) {
		mIntents.addAll(intent);

	}

	public void setIntent(MCrawlerTIntent intent) {
		if (intent != null)
			mIntents.add(intent);

	}

	public ArrayList<MCrawlerTIntent> getIntent() {
		return mIntents;
	}

	/**
	 * Information about the content provider accessed through teh component
	 */
	public void setAccessToCP(boolean access) {
		mAccessContentProvider = access;

	}

	public boolean getAccessToCp() {
		return mAccessContentProvider;
	}

	public boolean hasInternalIntent() {
		return mHasInternalIntent;
	}

	public String getType() {
		return mType;
	}

	public HashMap<String, ArrayList<String>> getProperties() {
		return mProperties;
	}

	public void setProperties(String key, String value) {
		if (mProperties.isEmpty() || mProperties.get(value) == null) {
			ArrayList<String> initValue = new ArrayList<String>();
			initValue.add(value);
			mProperties.put(key, initValue);
		}

		mProperties.get(key).add(value);
	}

	public String getPackage() {
		return mPackage;
	}

	public static class IntentLauncher {

	}

	public static class typeElement {
		public static final String ACTIVITY = "activity";
	}
}
