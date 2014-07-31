package fr.openium.sga;

import java.io.Serializable;

public class SgaTestResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4951395103207101634L;
	private final String mStartingTime;
	private String mValue;
	private String mEndingTime;

	public SgaTestResult(String time) {
		mStartingTime = time;
	}

	public String getStaringTime() {
		return mStartingTime;
	}

	/**
	 * @return the mValue
	 */
	public String getValue() {
		return mValue;
	}

	/**
	 * @param mValue
	 *            the mValue to set
	 */
	public void setValue(String mValue) {
		this.mValue = mValue;
	}

	/**
	 * @return the mEndingTime
	 */
	public String getEndingTime() {
		return mEndingTime;
	}

	/**
	 * @param mEndingTime
	 *            the mEndingTime to set
	 */
	public void setEndingTime(String mEndingTime) {
		this.mEndingTime = mEndingTime;
	}

}
