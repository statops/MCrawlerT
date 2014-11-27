package com.morphoss.acal.activity.serverconfig;

import android.net.ConnectivityManager;

public interface ServerConfigurator {

	boolean isAdvancedInterface();
	void saveData();
	void setResult(int resultOk);
	void finish();
	ConnectivityManager getConnectivityService();

}
