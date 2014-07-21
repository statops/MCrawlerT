package fr.openium.sga.bissimulation;

public class GraphAction {
	public final String mAction;
	public final String mConstraint;
	public int mEdgeId;

	public GraphAction(String act) {
		mAction = act;
		mConstraint = "";
	}

	public GraphAction(String act, String constraint) {
		mAction = act;
		mConstraint = constraint;
	}

	public String getValue() {
		return mAction;
	}

	public String getConstraint() {
		return mConstraint;
	}

	public void setEdgeId(int e) {
		mEdgeId = e;

	}

	@Override
	public String toString() {
		return mAction + ": " + mConstraint;
	}

}
