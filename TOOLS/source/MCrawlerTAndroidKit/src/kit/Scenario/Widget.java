package kit.Scenario;

import kit.Config.Config;
import kit.Utils.SgUtils;
import android.widget.EditText;

public class Widget extends IScenarioElement implements Comparable<Widget>, Cloneable {

	private final String mType;
	private final String mValue;
	/**
	 * Widget properties
	 */
	private final String mPosX;
	private final String mPosY;
	private final String mVisibility;
	private final String mSatusEnable;
	private final String mStatusPressed;
	private final String mStatusShown;
	private final String mStatusSelection;

	private final String isLongClickable;
	private final String isClickable;
	private final String isDirty;
	private final String isFocusable;
	private final String isFocus;
	private final String isInEditMode;
	private final String isVerticalScrollBarEnabled;
	/**
	 * only for EditText
	 */
	private  String inputType;

	/**
	 * Widgets properties
	 * 
	 * @param name
	 * @param type
	 * @param posX
	 * @param posY
	 * @param visibility
	 * @param statusEnable
	 * @param statusPressed
	 * @param statusShown
	 * @param statusSelection
	 * @param value
	 * @param LongClickable
	 * @param Clickable
	 * @param Dirty
	 * @param Focusable
	 * @param Focus
	 * @param InEditMode
	 * @param VerticalScrollBarEnabled
	 */
	public Widget(String name, String type, String posX, String posY, String visibility, String statusEnable,
			String statusPressed, String statusShown, String statusSelection, String value,
			String LongClickable, String Clickable, String Dirty, String Focusable, String Focus,
			String InEditMode, String VerticalScrollBarEnabled) {
		super(name, "");
		mType = type;
		mValue = value;
		mPosX = posX;
		mPosY = posY;
		mVisibility = visibility;
		mSatusEnable = statusEnable;
		mStatusPressed = statusPressed;
		mStatusShown = statusShown;
		mStatusSelection = statusSelection;
		isLongClickable = LongClickable;
		isClickable = Clickable;
		isDirty = Dirty;
		isFocusable = Focusable;
		isFocus = Focus;
		isInEditMode = InEditMode;
		isVerticalScrollBarEnabled = VerticalScrollBarEnabled;

	}

	/**
	 * @return the type of the wiget
	 */
	public String getType() {
		return mType;
	}

	@SuppressWarnings("unused")
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mPosX == null) ? 0 : mPosX.hashCode());
		result = prime * result + ((mPosY == null) ? 0 : mPosY.hashCode());
		result = prime * result + ((mSatusEnable == null) ? 0 : mSatusEnable.hashCode());
		result = prime * result + ((mStatusPressed == null) ? 0 : mStatusPressed.hashCode());
		result = prime * result + ((mStatusSelection == null) ? 0 : mStatusSelection.hashCode());
		result = prime * result + ((mStatusShown == null) ? 0 : mStatusShown.hashCode());
		result = prime * result + ((mType == null) ? 0 : mType.hashCode());
		result = prime * result + ((mVisibility == null) ? 0 : mVisibility.hashCode());
		result = prime * result + ((isClickable == null) ? 0 : isClickable.hashCode());
		result = prime * result + ((isVerticalScrollBarEnabled== null) ? 0 : isVerticalScrollBarEnabled.hashCode());
		result = prime * result + ((isInEditMode== null) ? 0 : isInEditMode.hashCode());
		boolean isTextView = mType.equalsIgnoreCase(SgUtils.TEXTVIEW);
		if (Config.TAKE_ACCOUNT_TEXTVIEWVALUE && isTextView) {
			return   prime * result + ((mValue == null) ? 0 : mValue.hashCode());
		}
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
		Widget other = (Widget) obj;
		if (mPosX == null) {
			if (other.mPosX != null)
				return false;
		} else if (!mPosX.equals(other.mPosX))
			return false;
		if (mPosY == null) {
			if (other.mPosY != null)
				return false;
		} else if (!mPosY.equals(other.mPosY))
			return false;
		if (mSatusEnable == null) {
			if (other.mSatusEnable != null)
				return false;
		} else if (!mSatusEnable.equals(other.mSatusEnable))
			return false;
		if (mStatusPressed == null) {
			if (other.mStatusPressed != null)
				return false;
		} else if (!mStatusPressed.equals(other.mStatusPressed))
			return false;
		if (mStatusSelection == null) {
			if (other.mStatusSelection != null)
				return false;
		} else if (!mStatusSelection.equals(other.mStatusSelection))
			return false;
		if (mStatusShown == null) {
			if (other.mStatusShown != null)
				return false;
		} else if (!mStatusShown.equals(other.mStatusShown))
			return false;
		if (mType == null) {
			if (other.mType != null)
				return false;
		} else if (!mType.equals(other.mType))
			return false;
		if (mVisibility == null) {
			if (other.mVisibility != null)
				return false;
		} else if (!mVisibility.equals(other.mVisibility))
			return false;
		return true;
	}

	public String getValue() {
		return mValue;
	}

	public String getPosX() {
		return (mPosX != null ? mPosX : "");
	}

	public String getPosY() {
		return mPosY != null ? mPosY : "";
	}

	public String getVisibility() {
		return mVisibility != null ? mVisibility : "";
	}

	public String getSatusEnable() {
		return mSatusEnable != null ? mSatusEnable : "";
	}

	public String getStatusPressed() {
		return mStatusPressed != null ? mStatusPressed : "";
	}

	public String getStatusShown() {
		return mStatusShown != null ? mStatusShown : "";
	}

	public String getStatusSelection() {
		return mStatusSelection != null ? mStatusSelection : "";
	}

	public String getIsLongClickable() {
		return getValue(isLongClickable);
	}

	/**
	 * @param value
	 *            null or ""
	 * @return
	 */
	private String getValue(String value) {
		return value != null ? value : "";
	}

	public String getIsClickable() {
		return getValue(isClickable);
	}

	public String getIsDirty() {
		return getValue(isDirty);
	}

	public String getIsFocusable() {
		return getValue(isFocusable);
	}

	public String getIsFocus() {
		return getValue(isFocus);
	}

	public String getIsInEditMode() {
		return getValue(isInEditMode);
	}

	public String getIsVerticalScrollBarEnabled() {
		return getValue(isVerticalScrollBarEnabled);
	}

	@Override
	public int compareTo(Widget another) {
		if (!mName.equalsIgnoreCase(another.getName())) {
			return -1;
		}
		if (this.equals(another) && (another.hashCode() == hashCode()))
			return 0;
		return -1;
	}

	public String gePropertyToString() {
		return streq("mType", mType) + " " + streq("mID", mID) + " " + streq("mPosX", mPosX) + " "
				+ streq("mPosY", "mPosY") + " " + streq("mSatusEnable", " ") + " "
				+ streq("mStatusPressed", mStatusPressed) + " " + streq("mType", mType);
	}

	public String geValueToString() {
		return streq("mType", mType) + " " + streq("value", mValue);
	}

	/*
	 * mType = type; mValue = value; mPosX = ""; mPosY = ""; mVisibility = "";
	 * mSatusEnable = ""; mStatusPressed = ""; mStatusShown = "";
	 * mStatusSelection = "";
	 */
	private String streq(String s1, String s2) {
		return "streq(" + s1 + "," + s2 + ")";
	}

	@SuppressWarnings("unused")
	public boolean isEqualTo(Widget other) {
		/**
		 * mName mType = type; mValue = value; mPosX = posX; mPosY = posY;
		 * mVisibility = visibility; mSatusEnable = statusEnable; mStatusPressed
		 * = statusPressed; mStatusShown = statusShown; mStatusSelection =
		 * statusSelection;
		 */

		if (other == null) {
			return false;
		}
		if (!mName.equalsIgnoreCase(other.getName())) {
			if (Config.DEBUG) {
				// System.out.println("name is not equal");
			}
			return false;
		}
		if (!mType.equalsIgnoreCase(other.getType())) {
			if (Config.DEBUG) {
				System.out.println("type is not equal");
			}
			return false;
		}
		if (!mStatusPressed.equalsIgnoreCase(other.getStatusPressed())) {
			if (Config.DEBUG) {
				System.out.println("mStatusPressed is not equal");
			}
			return false;
		}
		if (!mStatusShown.equalsIgnoreCase(other.getStatusShown())) {
			if (Config.DEBUG) {
				System.out.println("mStatusShownis not equal");
			}
			return false;
		}
		if (!mVisibility.equalsIgnoreCase(other.getVisibility())) {
			if (Config.DEBUG) {
				System.out.println("mVisibility not equal");
			}
			return false;
		}
		if (!mStatusSelection.equalsIgnoreCase(other.getStatusSelection())) {
			if (Config.DEBUG) {
				System.out.println("mStatusSelection not equal");
			}

			return false;
		}
		if (!mSatusEnable.equalsIgnoreCase(other.getSatusEnable())) {
			if (Config.DEBUG) {
				System.out.println("mSatusEnable not equal");
			}
			return false;
		}

		if (mType.equalsIgnoreCase(EditText.class.getName())) {
			return true;
		}
		/**
		 * valeur des textView
		 */
		if (Config.TAKE_ACCOUNT_TEXTVIEWVALUE && mType.equalsIgnoreCase(SgUtils.TEXTVIEW)
				&& other.getType().equalsIgnoreCase(SgUtils.TEXTVIEW)) {
			if (!mValue.equalsIgnoreCase(other.getValue())) {
				if (Config.DEBUG) {
					System.out.println("mValue not equal");
					System.out.println("V1 :" + mValue);
					System.out.println("V2 :" + other.getValue());
				}
				return false;
			}
		}

		return true;
	}

	public Integer getStatehashCode() {
		return hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {

		return (Widget) super.clone();
	}

	@Override
	public String toString() {

		return mName + "   " + mType + "   " + mValue; // + "   " + mStatusSelection;
	}

	/**
	 * @return
	 */
	public Integer getTraceStatehashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mPosX == null) ? 0 : mPosX.hashCode());
		result = prime * result + ((mPosY == null) ? 0 : mPosY.hashCode());
		result = prime * result + ((mSatusEnable == null) ? 0 : mSatusEnable.hashCode());
		result = prime * result + ((mStatusPressed == null) ? 0 : mStatusPressed.hashCode());
		result = prime * result + ((mStatusSelection == null) ? 0 : mStatusSelection.hashCode());
		result = prime * result + ((mStatusShown == null) ? 0 : mStatusShown.hashCode());
		result = prime * result + ((mType == null) ? 0 : mType.hashCode());
		result = prime * result + ((mVisibility == null) ? 0 : mVisibility.hashCode());
		if (mType.equalsIgnoreCase(SgUtils.EDITTEXT) || mType.equalsIgnoreCase(SgUtils.TEXTVIEW)) {
			result = prime * result + ((mValue == null) ? 0 : mValue.hashCode());
		}
		return result;
	}

	/**
	 * @param wig
	 * @return
	 */
	public boolean isStrictEqualTo(Widget other) {
		if (!isEqualTo(other)) {
			return false;
		}
		if (mValue == null || other.getValue() == null) {
			return true;
		}
		if (!mValue.equalsIgnoreCase(other.getValue())) {
			if (Config.DEBUG) {
				System.out.println("mValue not equal");
				System.out.println("V1 :" + mValue);
				System.out.println("V2 :" + other.getValue());
			}
			return false;
		}
		return true;

	}

	

	public String getInputType() {
		return inputType!=null?inputType:"";
	}

	public void setInputType(String inputType) {
		this.inputType = inputType;
	}

	

}
