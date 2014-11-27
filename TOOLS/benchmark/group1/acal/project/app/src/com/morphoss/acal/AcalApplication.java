package com.morphoss.acal;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AcalApplication extends Application {
	
	public static final String TAG = "AcalApplication";
	
    private static AcalApplication s_instance;
    private static SharedPreferences prefs;

    public AcalApplication(){
    	super();

    	s_instance = this;

    }

    private static Context getContext(){
        return s_instance;
    }

    public static String getResourceString(int resId){
        return getContext().getString(resId);       
    }

	public static String getPreferenceString(String key, String defValue) {
    	if ( prefs == null ) 
    		prefs = PreferenceManager.getDefaultSharedPreferences(s_instance);
    	return prefs.getString(key, defValue);
	}

	public static void setPreferenceString(String key, String value) {
    	if ( prefs == null ) 
    		prefs = PreferenceManager.getDefaultSharedPreferences(s_instance);
    	prefs.edit().putString(key, value).commit();
	}

	public static boolean getPreferenceBoolean(String key, boolean defValue) {
    	if ( prefs == null ) 
    		prefs = PreferenceManager.getDefaultSharedPreferences(s_instance);
    	return prefs.getBoolean(key, defValue);
	}

}
