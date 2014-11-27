package com.morphoss.acal.activity;

import com.morphoss.acal.AcalTheme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public abstract class AcalActivity extends Activity {

	public static SharedPreferences prefs;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		AcalTheme.initializeTheme(this);
	}	

	@Override
	protected void onResume() {
		super.onResume();
		AcalTheme.initializeTheme(this);
	}
}
