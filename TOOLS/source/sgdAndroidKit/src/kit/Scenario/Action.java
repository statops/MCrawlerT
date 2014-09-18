package kit.Scenario;

public class Action extends IScenarioElement implements Cloneable {

	private Widget mWidget = null;
	private String mError = null;

	public Action(String name) {
		super(name, "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mWidget == null) ? 0 : mWidget.hashCode());
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
		Action other = (Action) obj;
		if (mWidget == null) {
			if (other.mWidget != null)
				return false;
		} else if (!mWidget.equals(other.mWidget))
			return false;
		return true;
	}

	public Action(String name, Widget wig) {
		super(name, "");
		mWidget = wig;
	}

	/**
	 * @return the mWidget
	 */
	public Widget getWidget() {
		return mWidget;
	}

	public void seWidget(Widget wig) {
		mWidget = wig;

	}

	public String getError() {
		return mError;
	}

	public void setError(String er) {
		if (mError == null)
			this.mError = er;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String action = getName();
		if (mWidget != null) {
			action = action + ":  " + mWidget.toString();
			;

		}
		if (mError != null) {
			action = action + ":  " + mError;

		}

		return action;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Action clon_action = (Action) super.clone();
		clon_action.mWidget = (Widget) mWidget.clone();
		return clon_action;
	}
}
