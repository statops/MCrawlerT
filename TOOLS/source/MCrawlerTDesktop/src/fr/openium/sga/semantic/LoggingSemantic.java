package fr.openium.sga.semantic;

import java.util.ArrayList;

public class LoggingSemantic {
	public LoggingSemantic(ArrayList<String> set) {
		VALUE_SET.addAll(set);
	}

	public ArrayList<String> VALUE_SET = new ArrayList<String>();

	public void setVALUE_SET(ArrayList<String> logging_value) {
		VALUE_SET.addAll(logging_value);
	}
	
	public ArrayList<String> getValueSet(){
		return VALUE_SET;
	}

	public String toString() {
		return VALUE_SET.toString();
	}

}
