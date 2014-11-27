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

package com.morphoss.acal.activity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.PrefNames;
import com.morphoss.acal.R;
import com.morphoss.acal.aCal;

public class ShowUpgradeChanges extends AcalActivity implements OnClickListener {

	WebView upgradeNotes;
	Button seenEm;
	int thisRevision=0;

	boolean isRedisplay = false;
	
	private static final Pattern versionLinePattern = Pattern.compile("^v(\\d+)=([0-9.-]+)$");
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.changes_on_upgrade);
		upgradeNotes = (WebView) this.findViewById(R.id.UpgradeNotes);
		seenEm = (Button) this.findViewById(R.id.FinishedWithUpgradeNotes);
		AcalTheme.setContainerFromTheme(seenEm, AcalTheme.BUTTON);

		try {
			Bundle b = this.getIntent().getExtras();
			String x = b.getString("REDISPLAY");
			if ( x != null ) isRedisplay = true;
		}
		catch (Exception e) {}
		
		thisRevision = 1;
		try {
			thisRevision = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e) {
			Log.e(aCal.TAG,Log.getStackTraceString(e));
		}
		int lastRevision = prefs.getInt(PrefNames.lastRevision, thisRevision - 1);
		if ( isRedisplay ) lastRevision = thisRevision - 5;
		
		StringBuilder upNotes = new StringBuilder("<html><head>" +
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
					"<style type=\"text/css\">");
		upNotes.append("html,body{overflow:auto;color:#301060;}");
		upNotes.append("h1{font-size:1.5em}");
		upNotes.append("h2{font-size:1.3em}");
		upNotes.append("p,li{font-size:1.1em}");
		upNotes.append("a,a:visited{text-decoration:underline;color:#86c26c}");
		upNotes.append("</style></head><body><h1>");
		upNotes.append(getString(R.string.aCalVersionChanges));
		upNotes.append("</h1>");

		String versionName = null;
		int versionNum = 0;
		boolean newFeatures = false;
		StringBuilder aVersion = new StringBuilder();
		ArrayList<String> versions = new ArrayList<String>();
		Matcher versionLineMatcher;
		for( String line : readLines() ) {
			versionLineMatcher = versionLinePattern.matcher(line);
			if ( versionLineMatcher.matches() ) {
				versionNum = Integer.parseInt(versionLineMatcher.group(1));
				if ( versionNum < lastRevision ) continue;
				if ( versionName == null || ! versionName.equals(versionLineMatcher.group(2)) ) {
					versionName = versionLineMatcher.group(2);
					if ( newFeatures ) {
						// More than one new version
						aVersion.append("</ul>");
						versions.add(aVersion.toString());
						aVersion = new StringBuilder();
					}
					aVersion.append("<h2>");
					aVersion.append(getString(R.string.newWithVersion,versionName));
					aVersion.append("</h2><ul>");
				}
				newFeatures = true;
			}
			else if ( newFeatures ){
				aVersion.append("<li>");
				aVersion.append(line);
				aVersion.append("</li>");
			}
		}
		if ( newFeatures ) {
			aVersion.append("</ul>");
			versions.add(aVersion.toString());
			aVersion = new StringBuilder();

			for( int i=versions.size(); i-- > 0; ) {
				upNotes.append(versions.get(i));
			}
		}

		upNotes.append("<h2>"+getString(R.string.General_aCal_Information)+"</h2>");
		upNotes.append("</ul>");
		upNotes.append("<li>"+getString(R.string.aCal_Website, "<a href=\"http://wiki.acal.me/\">wiki.acal.me</a>")+"</li>");
		upNotes.append("<li>"+getString(R.string.aCal_IRC_Channel)+"</li>");
		upNotes.append("<li>"+getString(R.string.aCal_Mailing_Lists)+"</li>");
		upNotes.append("<li>aCal is developed by <a href=\"http://www.morphoss.com/\">Morphoss</a> and others.</li>");
		upNotes.append("</ul>");
		upNotes.append("<p>"+getString(R.string.aCal_is_Free_Software).replace("\"", "&quot;")+"</p>");

		upNotes.append("<p><br/></p></body></html>");
		
		upgradeNotes.loadData(upNotes.toString(), "text/html", "utf-8");
		upgradeNotes.setBackgroundColor(0); // transparent
		
		seenEm.setOnClickListener(this);
	}

	
    private ArrayList<String> readLines() {
        
        ArrayList<String> res = new ArrayList<String>(200);
        StringBuilder line = new StringBuilder();
        
        final int buffsize = 8192;
        char[] buf = new char[buffsize];
        int numRead;

        try {
	        InputStreamReader inputStream = new InputStreamReader(getResources().openRawResource(R.raw.upgrade_notes));
			while( (numRead = inputStream.read(buf,0,buffsize)) > 0 ) {
				int i=0, offset=0;
				for( ; i<numRead; i++ ) {
					if ( i == offset && (buf[i] == ' ' || buf[i] == '\t' || buf[i] == '\n') ) {
						// Trim leading whitespace from lines
						offset++;
						continue;
					}
					else if ( buf[i] == '\n' ) {
						line.append(buf, offset, i-offset);
						res.add(line.toString());
						line = new StringBuilder();

						offset = i+1; // Point after the \n
					}
				}
				if ( offset < i ) {
					line.append(buf, offset, i-offset);
				}
			}
			inputStream.close();
			if ( line.length() > 0 ) res.add(line.toString());
		}
		catch (IOException e) {
			Log.e(aCal.TAG, Log.getStackTraceString(e));
		}
 
        return res;
    }

	@Override
	public void onClick(View v) {

		if ( !isRedisplay ) {
			// Save the new version preference
			prefs.edit().putInt(PrefNames.lastRevision, thisRevision).commit();
			
			aCal.startPreferredView(prefs,this,true);
		}

		this.finish();
		
	}
}
