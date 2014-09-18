/**
 * 
 */
package kit.Stress;

import kit.Config.Config;

/**
 * @author Stassia
 * 
 */
public class Event {
	@SuppressWarnings("unused")
	private static final String TAG = Event.class.getName();
	private final String mName;
	private String mValue;
	private final String mType;

	/**
	 * 
	 * @param name
	 *            of the view/widget
	 * @param type
	 *            of the view/widget
	 * @param value
	 *            of the view/widget
	 */
	public Event(String name, String type, String value) {
		mName = name;
		mValue = value;
		mType = type;
	}

	public String getValue() {
		return mValue;
	}

	public String getName() {
		return mName;
	}

	public String getType() {
		return mType;
	}

	public void updateValue(String value) {
		mValue = value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return mName + Config.EVENTS_DELIMITERS + mType + Config.EVENTS_DELIMITERS + mValue;
	}

}
