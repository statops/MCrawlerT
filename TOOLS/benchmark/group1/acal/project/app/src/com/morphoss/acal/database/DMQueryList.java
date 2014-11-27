package com.morphoss.acal.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DMQueryList {
	private ArrayList<DMAction> actions = new ArrayList<DMAction>();
	public void addAction(DMAction action) { actions.add(action); }
	
	public List<DMAction> getActions() { return Collections.unmodifiableList(actions); }
	
	public boolean isEmpty() {
		return actions.isEmpty();
	}
	
	public int size() {
		return actions.size();
	}
}
