package kit.Scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import kit.Config.Config;

public class Transition extends IScenarioElement implements Cloneable {

	private State mSource;
	private State mDest;
	private HashSet<Action> mActions = new HashSet<Action>();
	private ArrayList<Widget> mWidgets = new ArrayList<Widget>();

	public Transition(String id) {
		super("", id);
		mSource = null;
		mDest = null;
	}

	public Transition(State source, Action action, State dest, String id) {
		super(source + ";" + dest, id);
		mSource = source;
		mSource.setType(Scenario.SOURCE);
		mDest = dest;
		mDest.setType(Scenario.DEST);
		setAction(action);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mActions == null) ? 0 : mActions.hashCode());
		result = prime * result + ((mDest == null) ? 0 : mDest.hashCode());
		result = prime * result + ((mSource == null) ? 0 : mSource.hashCode());
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
		Transition other = (Transition) obj;
		if (mActions == null) {
			if (other.mActions != null)
				return false;
		} else if (!mActions.equals(other.mActions))
			return false;
		if (mDest == null) {
			if (other.mDest != null)
				return false;
		} else if (!mDest.equals(other.mDest))
			return false;
		if (mSource == null) {
			if (other.mSource != null)
				return false;
		} else if (!mSource.equals(other.mSource))
			return false;
		return true;
	}

	public void setAction(Action action) {
		if (action != null)
			mActions.add(action);
	}

	/**
	 * @return the Source
	 */
	public State getSource() {
		return mSource;
	}

	/**
	 * @return the mDest
	 */
	public State getDest() {
		return mDest;
	}

	/**
	 * @param src
	 *            the mSource to set
	 */
	public void setSource(State src) {
		this.mSource = src;
		mSource.setType(Scenario.SOURCE);
	}

	/**
	 * @param dest
	 *            the mDest to set
	 */
	public void setDest(State dest) {
		this.mDest = dest;
		mDest.setType(Scenario.DEST);
	}

	/**
	 * @return the mAction
	 */
	public ArrayList<Action> getAction() {
		return new ArrayList<Action>(mActions);
	}

	/**
	 * @return the mWidgets
	 */
	public ArrayList<Widget> getWidgets() {
		for (Action act : mActions) {
			if (act.getWidget() != null && !mWidgets.contains(act.getWidget()))
				mWidgets.add(act.getWidget());
		}
		return mWidgets;
	}

	/**
	 * @param mWidgets
	 *            the mWidgets to set
	 */
	public void setWidgets(ArrayList<Widget> mWidgets) {
		this.mWidgets = mWidgets;
	}

	public void setWidgets(Widget widget) {
		this.mWidgets.add(widget);
	}

	public boolean contains(ArrayList<String> widgetNames) {
		ArrayList<String> transitionWidgetName = getWidgetTextName();
		for (String wig : widgetNames) {
			if (!transitionWidgetName.contains(wig)) {
				return false;
			}
		}
		return true;
	}

	private ArrayList<String> getWidgetTextName() {
		ArrayList<String> name = new ArrayList<String>();
		for (Widget editText : getWidgets()) {
			name.add(editText.getName());
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		String global = " id= " + getId() + "\n :src= " + getSource().toString() + "\n :dest="
				+ getDest().toString() + "\n :actions=  " + getAction().toString();
		String actions = "";
		for (Action act : getAction()) {
			actions = actions + "  , " + act.toString();
		}
		return global + " \n" + actions;
	}

	/**
	 * @param new_id
	 */
	public void setId(String new_id) {
		/**
		 * change Id of the states also
		 */
		mID = new_id;
		mSource.setId(new_id);
		mDest.setId(new_id);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Transition clone() throws CloneNotSupportedException {
		Transition clone = (Transition) super.clone();
		clone.mDest = mDest.clone();
		clone.mSource = mSource.clone();
		clone.mActions = (HashSet<Action>) mActions.clone();
		clone.mWidgets = (ArrayList<Widget>) mWidgets.clone();
		return clone;
	}

	/**
	 * @return
	 */
	public String getParentId() {

		return getParentId(mID);

	}

	/**
	 * @param string
	 * @return
	 */
	private String getParentId(String id) {
		StringTokenizer st = new StringTokenizer(id, Scenario.IDSEPARATOR);

		String value = "";
		if (!st.hasMoreTokens()) {
			value = "_" + id;
		}
		do {
			value = value + "_" + st.nextToken();

		} while (st.countTokens() > 1);
		if (Config.DEBUG) {
			System.out.println(value);
		}
		return value.substring(1);
	}

}
