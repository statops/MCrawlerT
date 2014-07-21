package kit.Scenario;

public abstract class IScenarioElement {
	protected String mName;
	protected String mID;

	public IScenarioElement(String name, String id) {
		mName = name;
		mID = id;
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
}
