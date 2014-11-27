package jp.gr.java_conf.hatalab.mnv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class myTemplateText {
	public static String PREFIX_NORMAL     = "normal:";
	public static String PREFIX_TIMEFORMAT = "timeformat:";
	public static String PREFIX_WITHNUMBER = "withnumber:";
	public static String PREFIX_TIME_NUMBER = "time_number:";

	
	private String mText = "";
	private boolean isTimeFormat = false;
	private boolean isWithNumber = false;
//	private boolean isWithNumber = true;
	
	
	
	public myTemplateText() {
		super();
		mText = "";
	}

	public myTemplateText(String text) {
		super();
		mText = text;
	}

	public myTemplateText(String text, boolean isTimeFormat) {
		super();
		mText = text;
		this.isTimeFormat = isTimeFormat;
	}
	
	public String getText() {
		return mText;
	}
	public void setText(String text) {
		mText = text;
	}
	public boolean isTimeFormat() {
		return isTimeFormat;
	}
	public void setTimeFormat(boolean isTimeFormat) {
		this.isTimeFormat = isTimeFormat;
	}

	public boolean isWithNumber() {
		return isWithNumber;
	}
	public void setWithNumber(boolean isWithNumber) {
		this.isWithNumber = isWithNumber;
	}

	
	public String getPrefString() {
		String prefix = PREFIX_NORMAL;
		if(isTimeFormat){
			prefix = PREFIX_TIMEFORMAT;
		}else if(isWithNumber){
			prefix = PREFIX_WITHNUMBER;			
		}
		if(isTimeFormat && isWithNumber){
			prefix = PREFIX_TIME_NUMBER;
		}
		
		return prefix + mText;
	}

	public void setPrefString(String prefString) {
		if(prefString.startsWith(PREFIX_TIMEFORMAT))isTimeFormat=true;
		if(prefString.startsWith(PREFIX_WITHNUMBER))isWithNumber=true;
		if(prefString.startsWith(PREFIX_TIME_NUMBER)){
			isTimeFormat=true;
			isWithNumber=true;
		}
		
		mText = prefString.replaceFirst("^.+?:", "");
	}
	
	public String toString() {
		String text = mText;
		if(isTimeFormat){
			text = getTimeFormat(text);
		}
//		if(isWithNumber){
//			text = getNumberFormat(text);
//		}	
		return text;
	}

	public String toString(int number) {
		String text = mText;
		if(isTimeFormat){
			text = getTimeFormat(text);
		}
		if(isWithNumber){
			text = getNumberFormat(text,number);
		}	
		return text;
	}

	private String getTimeFormat(String text){
		String s;
		try{
			Date inDate = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat(text);
			s = sdf.format(inDate);
		}catch(Exception e){
			s = text;
		}
		return s;
	}
	
	private String NUBER_REGEX = "(%[ 0]{0,1}[1-9][0-9]*?d|%d)";
	private String getNumberFormat(String text,int i){
		String s;
		String regex =  NUBER_REGEX;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);
		
		if(m.find()){
			if(m.start()>0 && text.substring(m.start()-1).startsWith("\\%")){
//				Log.d("myTemplateText",text);
				text = text.replaceFirst("\\%", "%");
//				Log.d("myTemplateText",text);
			}else{
				String format = m.group();
				text = text.replaceFirst(format, String.format(format,i));
			}
		}
		
		
		return text;
	}
	public String getNumberRegex(){
		String text = mText;
		if(isTimeFormat){
			text = getTimeFormat(text);
		}
		
		String s;
		String regex =  NUBER_REGEX;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(text);
		
		if(m.find()){
			if(m.start()!=0 && text.substring(m.start()-1).startsWith("\\%")){
				return null;
			}else{
				String format = m.group();
				text = text.replaceFirst(format, "[ ]*([0-9]+)");
				return "^" + text + "$";
			}
		}
		return null;
		
	}
}
