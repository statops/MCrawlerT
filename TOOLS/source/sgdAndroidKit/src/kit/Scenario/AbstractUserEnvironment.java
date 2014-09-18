package kit.Scenario;

import java.util.HashMap;

abstract class AbstractUserEnvironment extends IScenarioElement implements
		Cloneable {

	public final static String NAME = "name";
	public final static String STATUS = "status";

	public AbstractUserEnvironment(String name, String id,
			HashMap<String, String> prop) {
		super(name, id);
		setProperties(prop);
	}

	public boolean isChanged() {
		return Boolean.parseBoolean(mProperties.get("status"));
	}

	abstract boolean isEqualTo(UserEnvironment ue2);

	@Override
	public String toString() {
		String value = "";
		for (String prop : mProperties.keySet()) {
			value = value + prop + " : " + mProperties.get(prop) + " ";
		}

		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((mProperties == null) ? 0 : mProperties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserEnvironment other = (UserEnvironment) obj;

		if (mName == null)
			if (other.mName != null)
				return false;

		if (isChanged() != other.isChanged())
			return false;

		if (!mName.equalsIgnoreCase((other.mName)))
			return false;
		return true;
	}

}
