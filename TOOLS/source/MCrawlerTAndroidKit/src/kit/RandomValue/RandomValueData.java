package kit.RandomValue;

import java.util.HashSet;

public class RandomValueData {

	private String mType;
	private HashSet<String> mValue = new HashSet<String>();

	public RandomValueData(String name) {
		setType(name);
	}

	public HashSet<String> getValue() {
		return mValue;
	}

	public boolean setValue(String value) {
		return mValue.add(value);
	}

	public String getType() {
		return mType;
	}

	public void setType(String mType) {
		this.mType = mType;
	}

}
