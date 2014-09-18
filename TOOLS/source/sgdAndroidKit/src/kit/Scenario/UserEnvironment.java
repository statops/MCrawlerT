package kit.Scenario;

import java.util.HashMap;

public class UserEnvironment extends AbstractUserEnvironment {
	public final static String _ID = "ue";

	

	/**
	 * 
	 * @param name
	 *            : the provider uri NAME
	 * @param id
	 *            : the state id
	 */
	public UserEnvironment(String name, String id, HashMap<String, String> prop) {
		super(name, id, prop);

	}

	public boolean isChanged() {
		return Boolean.parseBoolean(mProperties.get("status"));
	}

	public boolean isEqualTo(UserEnvironment ue2) {

		return equals(ue2);
	}

}
