/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.AcalDBHelper;

public class DebugDatabase extends ServiceJob {

	public static final String TAG = "aCal DebugDatabase";

	public static final int REVERT = 0;
	public static final int SAVE = 1;

	private int jobtype;
	private aCalService context;

	public DebugDatabase(int jobtype) {
		this.jobtype = jobtype;
		this.TIME_TO_EXECUTE = System.currentTimeMillis();
	}

	@Override
	public void run(aCalService context) {
		this.context = context;
		try {
			switch (this.jobtype) {
			case REVERT: revertDatabase(); break;
			case SAVE:	 saveDatabase(); break;
			default:
				Log.w(TAG, "Unable to execute jobtype - invalid jobtype id provided.");
			}
		} catch (Exception e) {
			Log.e(TAG,"Unknown error executing jobtype: "+e.getMessage());
		}
	}

	private void revertDatabase() {
		if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG,"Reverting database...");
		try {
			AcalDBHelper dbHelper = new AcalDBHelper(this.context);
			dbHelper.onUpgrade(dbHelper.getWritableDatabase(), -1, -1);
			dbHelper.close();
			if (Constants.LOG_DEBUG) Log.println(Constants.LOGD,TAG,"Reversion complete.");
		} catch (Exception e) {
			Log.e(TAG,"Error reverting database: "+e.getMessage());
		}
	}

	private void saveDatabase() {
		Log.println(Constants.LOGI,TAG, "Database copy requested. Beginning file xfer to "+Constants.COPY_DB_TARGET);
		File inputFile = new File("/data/data/com.morphoss.acal/databases/acal.db");
		File outputFile = new File(Constants.COPY_DB_TARGET);

		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile);
			FileChannel inChannel = fileInputStream.getChannel();
			FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
			FileChannel outChannel = fileOutputStream.getChannel();
			inChannel.transferTo(0, inChannel.size(), outChannel);
			inChannel.close();
			fileInputStream.close();
			outChannel.close();
			fileOutputStream.close();
		} catch (Exception e) {
			Log.e(TAG,"Error copying '"+inputFile.getAbsolutePath()+"' to '"+outputFile.getAbsolutePath()+"'");
		}
		Log.println(Constants.LOGI,TAG, "Database copy completed.");
	}

	@Override
	public String getDescription() {
		switch( jobtype ) {
			case SAVE:
				return "Saving database to file "+ Constants.COPY_DB_TARGET;
			case REVERT:
				return "Reverting to empty database.";
		}
		Log.e(TAG,"No description defined for jobtype "+jobtype );
		return "Unknown DebugDatabase jobtype!";
	}



}
