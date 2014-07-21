package kit.Utils;

import java.util.Comparator;

import kit.Scenario.Transition;

public class TransitionIdComparator implements Comparator<Transition> {

	@Override
	public int compare(Transition t1, Transition t2) {
		/**
		 * case of integerType
		 */
		try {
			
			return compare(Integer.parseInt(t1.getId()),Integer.parseInt(t2.getId()));
			
		} catch (NumberFormatException ex){
			//String id
			if (t1.getId().length()<t2.getId().length()){
				return 1;
			}
			if ((t1.getId()).length() == (t2.getId()).length()) {
				/**
				 * the last id
				 */
				int t1id=SgUtils.get_end_id(t1.getId());
				int t2id=SgUtils.get_end_id(t2.getId());
				return compare(t1id, t2id);
				
				
			}
			return -1;
			
			
		}
		
	}
/*
 * 
 * 
 */
	private int compare(int t1id, int t2id) {
		if (t1id < t2id) {
			return 1;
		}
		if (t1id == t2id) {
			return 0;
		}
		return -1;
		
		
	}
}
