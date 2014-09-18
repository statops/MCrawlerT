package kit.Scenario;

import java.util.HashMap;

public class BroadCastEnvironment extends AbstractUserEnvironment {
	public final static String _ID = "broadcast";

	public BroadCastEnvironment(String name, String id,
			HashMap<String, String> prop) {
		super(name, id, prop);
	}

	public boolean isEqualTo(UserEnvironment ue2) {
		return equals(ue2);
	}

}
