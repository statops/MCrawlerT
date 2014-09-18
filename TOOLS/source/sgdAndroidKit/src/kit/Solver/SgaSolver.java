package kit.Solver;

import java.util.ArrayList;

import kit.Scenario.UserEnvironment;

public class SgaSolver {

	public static boolean solve(ArrayList<UserEnvironment> currentUEList) {
		if (currentUEList == null || currentUEList.isEmpty()) {
			return false;
		}
		for (UserEnvironment ue:currentUEList){
			if(ue.isChanged()){
				return true;
			}
		}
		return false;
	}

}
