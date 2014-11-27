//   -----------------------------------------------------------------------------
//    Copyright 2010 Ferran Caellas Puig

//    This file is part of Learn Music Notes.
//
//    Learn Music Notes is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.

//    Learn Music Notes is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.

//    You should have received a copy of the GNU General Public License
//    along with Learn Music Notes.  If not, see <http://www.gnu.org/licenses/>.
//   -----------------------------------------------------------------------------


package net.fercanet.LNM;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import net.fercanet.LNM.Preferences;


public class Settings extends Activity {
	
	Preferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        
        Button endgame = (Button) findViewById(R.id.endgame);
        endgame.setOnClickListener(ClickListener);
              
        Spinner spinner = (Spinner) findViewById(R.id.sphofentries);
        ArrayAdapter adapter = ArrayAdapter.createFromResource( this, R.array.hofentries_array , android.R.layout.simple_spinner_dropdown_item); adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(ItemSelectedListener);
        
        RadioButton rbseeuropean = (RadioButton) findViewById(R.id.rbseeuropean);
        RadioButton rbenglish = (RadioButton) findViewById(R.id.rbenglish);
        RadioButton rbnortheneuropean = (RadioButton) findViewById(R.id.rbnortheneuropean);
        RadioButton rbbizantine = (RadioButton) findViewById(R.id.rbbizantine);
        RadioButton rbjapanese = (RadioButton) findViewById(R.id.rbjapanese);
        RadioButton rbindian = (RadioButton) findViewById(R.id.rbindian);
        rbseeuropean.setOnClickListener(RadioListener);
        rbenglish.setOnClickListener(RadioListener);
        rbnortheneuropean.setOnClickListener(RadioListener);
        rbbizantine.setOnClickListener(RadioListener);
        rbjapanese.setOnClickListener(RadioListener);
        rbindian.setOnClickListener(RadioListener);
        
        RadioButton rbinformeryes = (RadioButton) findViewById(R.id.rbyes);
        RadioButton rbinformerno = (RadioButton) findViewById(R.id.rbno);
        rbinformeryes.setOnClickListener(RadioListenerInformer);
        rbinformerno.setOnClickListener(RadioListenerInformer);
        
        prefs = new Preferences(this);             // Instantiating Preferences in prefs global variable

        spinner.setSelection(prefs.scoresnumpos);
    
        rbseeuropean.setChecked(false);
        rbenglish.setChecked(false);
        rbnortheneuropean.setChecked(false);
        rbbizantine.setChecked(false);
        rbjapanese.setChecked(false);
        rbindian.setChecked(false);
        
        if (prefs.notationstyle.equals("seeuropean")) {rbseeuropean.setChecked(true);}
        else if (prefs.notationstyle.equals("english")) {rbenglish.setChecked(true);}
        else if (prefs.notationstyle.equals("northeneuropean")) {rbnortheneuropean.setChecked(true);}
        else if (prefs.notationstyle.equals("bizantine")) {rbbizantine.setChecked(true);}
        else if (prefs.notationstyle.equals("japanese")) {rbjapanese.setChecked(true);}
        else if (prefs.notationstyle.equals("bindian")) {rbindian.setChecked(true);}
        
        if (prefs.informer == true) {
        	rbinformeryes.setChecked(true);
        	rbinformerno.setChecked(false);
        }
        else if (prefs.informer == false) {
        	rbinformeryes.setChecked(false);
        	rbinformerno.setChecked(true);
        }
    }
    
    
    // ToFix dirty trick because i can't call Utils.reloadScores directly from the clickListener because the context is not the same.
    private void reloadScoresCall() {
    	Utils.reloadScores(this);
    }
    
    // Click listener for exit button
    OnClickListener ClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			
		    reloadScoresCall();
			
     	   	Intent intent = new Intent();
     	   	intent.setClassName("net.fercanet.LNM", "net.fercanet.LNM.MainMenu");
     	   	startActivity(intent);
		}
    };
    
    
    // Click listener for Notation style RadioButtons
    OnClickListener RadioListener = new OnClickListener() {
        public void onClick(View v) {
            RadioButton rb = (RadioButton) v;
            if (rb.getId() == R.id.rbseeuropean) prefs.notationstyle = "seeuropean";
            else if (rb.getId() == R.id.rbenglish) prefs.notationstyle = "english";
            else if (rb.getId() == R.id.rbnortheneuropean) prefs.notationstyle = "northeneuropean";
            else if (rb.getId() == R.id.rbbizantine) prefs.notationstyle = "bizantine";
            else if (rb.getId() == R.id.rbjapanese) prefs.notationstyle = "japanese";
            else if (rb.getId() == R.id.rbindian) prefs.notationstyle = "indian";
            prefs.SavePreferences();
        }
    };
    
    
    // OnItemSelectedListener for sphofentries (spinner with the number of hall of fame entries)
    OnItemSelectedListener ItemSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			Object item = parent.getItemAtPosition(pos);
			String value = item.toString();
			prefs.scoresnumpos = pos;
            prefs.scoresnum = Long.parseLong(value);			
			prefs.SavePreferences();
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			
		}
		
    }; 
    
    
    // Click listener for Notation style RadioButtons
    OnClickListener RadioListenerInformer = new OnClickListener() {
        public void onClick(View v) {
            RadioButton rb = (RadioButton) v;
            if (rb.getId() == R.id.rbyes) {
            		prefs.informer = true;
            		prefs.SavePreferences();
            }
            
            else if (rb.getId() == R.id.rbno) {
            		prefs.informer = false;
            		prefs.SavePreferences();
            }
        }
    };
    
    
   
	
}






