package kit.Scenario;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class State extends IScenarioElement implements Cloneable {
	private boolean isFinal = false;
	private boolean isInit = false;
	private boolean isCalledWithIntent = false;
	private TreeSet<Widget> mWidgets = new TreeSet<Widget>();
	private String mTime = "";
	private boolean userEnvironment = false;// by default
	private ArrayList<UserEnvironment> mUserEnvironments = new ArrayList<UserEnvironment>();
	private ArrayList<BroadCastEnvironment> mBroadcastEnvironments = new ArrayList<BroadCastEnvironment>();

	/**
	 * src or dest
	 */
	private String mType = "";

	public State(String name, String id) {
		super(name, id);
	}

	public State(String name, boolean initStatus, boolean finalstatus, String id) {
		super(name, id);
		setInit(initStatus);
		isFinal = finalstatus;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		// result = prime * result + (isFinal ? 1231 : 1237);
		// result = prime * result + ((mTime == null) ? 0 : mTime.hashCode());
		// result = prime * result + ((mType == null) ? 0 : mType.hashCode());
		result = prime * result
				+ ((mWidgets == null) ? 0 : mWidgets.hashCode());
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
		State other = (State) obj;
		/*
		 * if (isFinal != other.isFinal) return false;
		 */
		/*
		 * if (mTime == null) { if (other.mTime != null) return false; } else if
		 * (!mTime.equals(other.mTime)) return false;
		 */
		/*
		 * if (mType == null) { if (other.mType != null) return false; } else if
		 * (!mType.equals(other.mType)) return false;
		 */
		if (mWidgets == null) {
			if (other.mWidgets != null)
				return false;
		} else if (!mWidgets.equals(other.mWidgets))
			return false;
		return true;
	}

	public ArrayList<Widget> getWidgets() {
		return new ArrayList<Widget>(mWidgets);
	}

	public void setWidget(Widget wig) {
		/**
		 * comparer avant d'ajouter
		 */
		boolean isEqual = false;
		for (Widget wigdet : mWidgets) {
			if (wigdet.isEqualTo(wig)) {
				isEqual = true;
				break;
			}
		}
		if (!isEqual) {
			mWidgets.add(wig);
		}

	}

	/**
	 * @return the isFinal
	 */
	public boolean isFinal() {
		return isFinal;
	}

	/**
	 * @return the isInit
	 */
	public boolean isInit() {
		return isInit;
	}

	/**
	 * @param isInit
	 *            the isInit to set
	 */
	public void setInit(boolean isInit) {
		this.isInit = isInit;
	}

	public String getType() {
		if (mType == null) {
			return "";
		}
		return mType;
	}

	public void setType(String type) {
		this.mType = type;
	}

	public String getTime() {
		if (mTime == null) {
			return "";
		}
		return mTime;
	}

	public void setTime(String mTime) {
		this.mTime = mTime;
	}

	public void setInit(boolean initState, boolean finalState) {
		setInit(initState);
		isFinal = finalState;
	}

	public String getGraphStateId() {
		String widgINumber = "" + getWidgets().size();
		TreeSet<Integer> hashcode = new TreeSet<Integer>();
		for (Widget wig : mWidgets) {
			hashcode.add(wig.getStatehashCode());
		}
		/**
		 * final or not final state
		 */
		String finalId = (isFinal) ? "END" : "NOT_END";
		return shortName(mName) + Scenario.IDSEPARATOR + widgINumber + finalId
				+ hashcode.toString();
	}

	public String getTraceGraphStateId() {
		String widgINumber = "" + getWidgets().size();
		TreeSet<Integer> hashcode = new TreeSet<Integer>();
		for (Widget wig : mWidgets) {
			hashcode.add(wig.getTraceStatehashCode());
		}
		String finalId = (isFinal) ? "END" : "NOT_END";
		return shortName(mName) + widgINumber + finalId + hashcode.toString();
	}

	private String shortName(String mName) {
		StringTokenizer stk = new StringTokenizer(mName, ".");
		String value = mName;
		while (stk.hasMoreTokens()) {
			value = stk.nextToken();
		}
		return value;
	}

	public String getShortName() {
		return shortName(mName);
	}

	public void setEndState(String attributeValue) {
		if (attributeValue != null)
			isFinal = Boolean.parseBoolean(attributeValue);

	}

	public boolean isUserEnvironment() {
		return userEnvironment;
	}

	public void setUserEnvironment(boolean userEnvironment) {
		this.userEnvironment = userEnvironment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// String wig = mWidgets.toString();
		return mName + " id:" + mID + " type:" + mType + " ue:"
				+ userEnvironment + " isFinal:" + isFinal + " isInit: "
				+ isInit + " isCalledbyIntent: " + isCalledWithIntent;// +
																		// "\n wig:"
																		// +
																		// wig;
	}

	/**
	 * @return
	 */
	public boolean get_user_environment() {
		return userEnvironment;
	}

	/**
	 * @param new_id
	 */
	public void setId(String new_id) {
		mID = new_id;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public State clone() throws CloneNotSupportedException {
		State state_clone = (State) super.clone();
		// if (mWidgets!=null&!mWidgets.isEmpty())
		state_clone.mWidgets = (TreeSet<Widget>) mWidgets.clone();
		state_clone.setUserEnvironment(userEnvironment);
		return state_clone;
	}

	/**
	 * @return
	 */
	public boolean isDest() {
		return mType != null && mType.equalsIgnoreCase(Scenario.DEST);
	}

	/**
	 * @param ue
	 */
	public void setUserEnvironment(String ue) {
		if (ue == null || ue.equalsIgnoreCase("")) {
			userEnvironment = false;
			return;
		}
		userEnvironment = Boolean.parseBoolean(ue);
	}

	public void setUserEnvironment(UserEnvironment currenUE) {
		mUserEnvironments.add(currenUE);
	}

	public void setUserEnvironment(ArrayList<UserEnvironment> userEnvLists) {
		if (userEnvLists == null || userEnvLists.isEmpty()) {
			return;
		}
		mUserEnvironments.addAll(userEnvLists);
	}

	public ArrayList<UserEnvironment> getUserEnvironments() {
		return mUserEnvironments;
	}

	public void setBroadCastEnvironment(BroadCastEnvironment broadcastElement) {
		if (broadcastElement == null) {
			return;
		}
		mBroadcastEnvironments.add(broadcastElement);

	}

	public ArrayList<BroadCastEnvironment> getBroadCastEnvironments() {

		return mBroadcastEnvironments;
	}

	public void setBroadCastEnvironment(
			ArrayList<BroadCastEnvironment> currentBDList) {
		if (currentBDList == null || currentBDList.isEmpty()) {
			return;
		}
		mBroadcastEnvironments.addAll(currentBDList);

	}

	public boolean isCalledWithIntent() {
		return isCalledWithIntent;
	}

	public void setCalledWithIntent(String bool) {
		try {
			isCalledWithIntent = Boolean.parseBoolean(bool);
		} catch (Exception e) {

		}

	}

}
