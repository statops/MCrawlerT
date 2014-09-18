package kit.Scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class Tree extends IScenarioElement {

	private ArrayList<Transition> mTransitions = new ArrayList<Transition>();

	public Tree(String name, String id) {
		super(name, id);
	}

	public ArrayList<Transition> getTransitions() {
		return mTransitions;
	}

	public void setTransitions(Transition tr) {
		mTransitions.add(tr);
	}

	public void setTransitions(Transition tr, boolean erase) {
		/**
		 * verification si la transition id existe et le supprimer si erase
		 */
		if (!erase)
			setTransitions(tr);
		else {
			Iterator<Transition> traIt = mTransitions.iterator();
			while (traIt.hasNext()) {
				Transition temp = traIt.next();
				if (temp.getId().equalsIgnoreCase(tr.getId())) {
					traIt.remove();
					break;
				}
			}
			setTransitions(tr);
		}
	}

	public void setTransitions(ArrayList<Transition> transitionList) {
		mTransitions.addAll(transitionList);

	}

	public HashSet<State> getStates() {
		HashSet<State> st = new HashSet<State>();
		for (Transition tr : mTransitions) {
			st.add(tr.getSource());
			st.add(tr.getDest());

		}
		return st;
	}

	public HashSet<String> getTransitions_ids() {
		HashSet<String> st = new HashSet<String>();
		for (Transition tr : mTransitions) {
			st.add(tr.getId());
		}
		return st;
	}

}
