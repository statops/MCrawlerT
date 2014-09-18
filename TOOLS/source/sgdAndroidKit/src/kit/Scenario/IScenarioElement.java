package kit.Scenario;

import java.util.HashMap;

public abstract class IScenarioElement {
	protected String mName;
	protected String mID;
	protected HashMap<String, String> mProperties;

	public IScenarioElement(String name, String id) {
		mName = name;
		mID = id;

		updateProperties("name", name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mID == null) ? 0 : mID.hashCode());
		result = prime * result + ((mName == null) ? 0 : mName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IScenarioElement other = (IScenarioElement) obj;
		if (mID == null) {
			if (other.mID != null)
				return false;
		} else if (!mID.equals(other.mID))
			return false;
		if (mName == null) {
			if (other.mName != null)
				return false;
		} else if (!mName.equals(other.mName))
			return false;
		return true;
	}

	public String getName() {
		return mName;
	}

	public String getId() {
		return mID;
	}

	protected void setProperties(HashMap<String, String> prop) {
		if (mProperties == null) {
			mProperties = prop;
			return;
		}
		mProperties.putAll(prop);

	}

	public HashMap<String, String> getProperties() {
		return mProperties;
	}

	public String getProperty(String name) {
		return mProperties.get(name);
	}

	public void updateProperties(String key, String value) {
		if (mProperties == null) {
			mProperties = new HashMap<String, String>();
		}
		mProperties.put(key, value);
	}

}
