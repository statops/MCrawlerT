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


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.Chronometer.*;
import android.graphics.Color;



// Main Class
public class Game extends Activity {
	
	String notes[]={"sol_0","la_0","si_0","don_1","re_1","mi_1","fa_1","sol_1","la_1","si_1","don_2","re_2","mi_2","fa_2","sol_2","la_2","si_2"};
	int notesbt[]={R.id.re,R.id.si,R.id.mi,R.id.sol,R.id.don,R.id.fa,R.id.la};
	String btseeuropeantext[]={"re","si","mi","sol","do","fa","la"};
	String btenglishtext[]={"D","B","E","G","C","F","A"};
	String btneuropeantext[]={"D","H","E","G","C","F","A"};
	String btbizantinetext[]={"pa","zo","vu","di","ni","ga","ke"};
	String btjapanesetext[]={"ロ","ト","ハ","ホ","イ","ニ","ヘ"};
	String btindiantext[]={"re","ni","ga","pa","sa","ma","dha"};
	
	int prevnotenum;
	int correct, fail;
//	long scoresnum;   // scores showed in hall of fame
	boolean omt;
	long elapsedtime;
	int countdown;
	Chronometer chrono;
	String currenttime;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game); 
        
        omt = this.getIntent().getExtras().getBoolean("omt");
        if (savedInstanceState != null){                              // game activity has been restarted by a runtime change (orientation change)
        	correct = savedInstanceState.getInt("correct");
        	fail = savedInstanceState.getInt("fail");
        	elapsedtime = savedInstanceState.getLong("elapsedtime");
        	countdown = savedInstanceState.getInt("countdown");
        }
        else {
        	correct = 0;
        	fail = 0;
        	if (omt==true) {countdown = 61;}
        	else {elapsedtime = SystemClock.elapsedRealtime();}
        }
        buttonsInitialization();
        chrono = (Chronometer) findViewById(R.id.chrono);
        chrono.setOnChronometerTickListener(ChronometerTickListener);
        chrono.setBase(elapsedtime); 
        chrono.start();
        showNextNote();
    }
    
    
    // Set the OnClick Listeners and the text for all the buttons
    private void buttonsInitialization() {
        Button bt;
        int x;
        
        for (x=0;x<notesbt.length;x++) {
        	bt = (Button) findViewById(notesbt[x]);
        	Preferences prefs = new Preferences(this);
        	if (prefs.notationstyle.equals("english")) {
        		bt.setText(btenglishtext[x]);		
        	} else if (prefs.notationstyle.equals("seeuropean")) {
        		bt.setText(btseeuropeantext[x]);
        	} else if (prefs.notationstyle.equals("northeneuropean")) {
        		bt.setText(btneuropeantext[x]);
        	} else if (prefs.notationstyle.equals("bizantine")) {
        		bt.setText(btbizantinetext[x]);
        	} else if (prefs.notationstyle.equals("japanese")) {
        		bt.setText(btjapanesetext[x]);
        	} else if (prefs.notationstyle.equals("indian")) {
        		bt.setText(btindiantext[x]);
        	}
        	bt.setOnClickListener(ClickListener);
        }
        	
        bt = (Button) findViewById(R.id.endgame);
        bt.setOnClickListener(ClickListener); 
    }
    
    
    // Set a red pressed color button for all note buttons except notebt (green color = right note button)
    private void setRedBackgroundToAllBtExceptThis(String notebt) {
        Button bt;
        String note;
        int x;
        
        for (x=0;x<notesbt.length;x++) {
        
        	bt = (Button) findViewById(notesbt[x]);
        	bt.setTextColor(Color.BLACK);
       
        	note = String.valueOf(bt.getTag());
        	if (note.equals(notebt)) { 
        		bt.setBackgroundResource(R.drawable.custom_button_green); 
        	}
        	else { 
        		bt.setBackgroundResource(R.drawable.custom_button_red); 
        	}
        }
    }
    
    
     
    // Randomly gets and show the next note
    private void showNextNote() {
    	try {
    		if (omt==false) {
    			Thread.sleep(1000);
    		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Random randomGen = new Random();
    	int notenum = randomGen.nextInt(17);
    	while (notenum == prevnotenum) { notenum = randomGen.nextInt(12);}
    	prevnotenum = notenum;
    	String note = notes[notenum];
        ImageView scoreimg = (ImageView) findViewById(R.id.imgnota);
        int resid = getResources().getIdentifier(note, "drawable", this.getPackageName());
        scoreimg.setImageResource(resid);
        String notebt = note.substring(0, note.length()-2);
        setRedBackgroundToAllBtExceptThis(notebt);
        Player.play(getBaseContext(), notebt, notes[prevnotenum]);
    }
    

    
    // ToFix dirty trick because i can't call Utils.saveuserScore directly from public void onclick because the context is no the same.
    public void saveUserScoreCall(Score userscore) {
    	Utils.saveUserScore(userscore, this);
    }
    
    
    // Show the end dialog, save the score (only if is in topX) and return to game menu 
    private void showOMTEndDialog() {
    	final AlertDialog.Builder playername = new AlertDialog.Builder(this);
    	final EditText input = new EditText(this);

    	int points = correct-fail;
    	
    	if (Utils.isInTheTopScores(points,this)){
    		
    		Preferences prefs = new Preferences(this); 
   	
	    	playername.setMessage( correct + " correct notes and " + fail + " fails in one minute test. \n \n Congratulations you are in top "+prefs.scoresnum+" scores! \n \n Please, insert your name:"); 
	    	playername.setIcon(R.drawable.dialog);
	    	playername.setTitle("Results");
	    	playername.setView(input);
	    	playername.setPositiveButton("Save score", new DialogInterface.OnClickListener() {
	    		
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    			
	    			String name = input.getText().toString().trim();
	    			if (name.length()>12)  name = name.substring(0, 12);                                // 12 chars max.
	    	        name = name.replaceAll(",", "");                             // avoiding presence of "," and ";" because is the char used to separate entries in the file (name1,score1;name2,score2;...)
	    	        name = name.replaceAll(";", "");
	    			int points = correct-fail;
	    			Calendar cal = Calendar.getInstance();
	    		    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
	    		    String date = sdf.format(cal.getTime());
	    			
	    			saveUserScoreCall(new Score(points, name, date));
	    			
		        	Intent intent = new Intent();
		        	intent.setClassName("net.fercanet.LNM", "net.fercanet.LNM.MainMenu");
		        	startActivity(intent);
	    		}
	    	});
	    	
	    	playername.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    			Intent intent = new Intent();
	    			intent.setClassName("net.fercanet.LNM", "net.fercanet.LNM.MainMenu");
	    			startActivity(intent);
	    		}
	        });
	    	
	    	playername.show();
    	}
    	else {
    		
    		playername.setIcon(R.drawable.dialog);
    		playername.setTitle("Results");
    		playername.setMessage( correct + " correct notes and " + fail + " fails in one minute test!"); 
    		playername.setCancelable(false);
    		playername.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   Intent intent = new Intent();
        	        	   intent.setClassName("net.fercanet.LNM", "net.fercanet.LNM.MainMenu");
        	        	   startActivity(intent);
        	           }
        	       });
    		playername.show();	  				
    	}    
    }
    
    
    // Show the end dialog and return to game menu 
    private void showTrainingEndDialog() {
    	AlertDialog.Builder dlgbuilder = new AlertDialog.Builder(this);
    	dlgbuilder.setIcon(R.drawable.dialog);
    	dlgbuilder.setTitle("Results");
    	dlgbuilder.setMessage( correct + " correct notes and " + fail + " fails in " + currenttime + " minutes!");
    	dlgbuilder.setCancelable(false);
    	dlgbuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   Intent intent = new Intent();
    	        	   intent.setClassName("net.fercanet.LNM", "net.fercanet.LNM.MainMenu");
    	        	   startActivity(intent);
    	           }
    	       });
    	dlgbuilder.show();	
    }
    
    
    // Click listener for all the buttons
    OnClickListener ClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId())
			{
			case R.id.endgame:
				chrono.stop();
				if (omt == true) {
					Intent intent = new Intent();
					intent.setClassName("net.fercanet.LNM", "net.fercanet.LNM.MainMenu");
					startActivity(intent);
				}
				else showTrainingEndDialog();
			break;
			default:	
				Button bt = (Button) v;				
			    String bttext = String.valueOf(bt.getTag());
			    if (omt==false){
			    	Player.play(getBaseContext(), bttext, notes[prevnotenum]);
			    }
 			    
			    if ((notes[prevnotenum].equals(bttext+"_0")) || (notes[prevnotenum].equals(bttext+"_1")) || (notes[prevnotenum].equals(bttext+"_2"))){
					correct++;
					showNextNote();
				}
				else {
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					// Vibrate for 300 milliseconds
					vib.vibrate(300);
					fail++;
					// In training mode set the correct button text to bold
					Preferences prefs = new Preferences(getBaseContext());
					if (omt==false && prefs.informer==true) {
						String note = notes[prevnotenum];
						String notebt = note.substring(0, note.length()-2);
				        for (int x=0;x<notesbt.length;x++) {
				   
				        	bt = (Button) findViewById(notesbt[x]);
				       
				        	note = String.valueOf(bt.getTag());
				        	if (note.equals(notebt)) { 
				        		bt.setTextColor(Color.GREEN);
				        		
				        	}
				        }
					}
				}	
			}
		}
    };	

    // Chrono ticklistener to control the time and update the timer label
    OnChronometerTickListener ChronometerTickListener = new OnChronometerTickListener() {
    	@Override
		public void onChronometerTick(Chronometer chronometer) { 		
    		if (omt==true){
    			countdown--;
    			chronometer.setText(String.valueOf(countdown));
    			if (countdown<=0) {
    				chrono.stop();
    				showOMTEndDialog();
    			}
    		}
    		else {
    			long minutes=((SystemClock.elapsedRealtime()-chrono.getBase())/1000)/60;
	    		long seconds=((SystemClock.elapsedRealtime()-chrono.getBase())/1000)%60;
	    		currenttime=minutes+":"+seconds;
	    		chronometer.setText(currenttime);
	    		elapsedtime=SystemClock.elapsedRealtime();
    		}
		}
    };
    
    // If game activity is restarted by a runtime change (orientation change) this save some data to restore the game 
    protected void onSaveInstanceState(Bundle outState)
    {
      super.onSaveInstanceState(outState);
      outState.putInt("correct", correct);
      outState.putInt("fail", fail);
      outState.putLong("elapsedtime", chrono.getBase());
      outState.putInt("countdown", countdown);
    }

    
};


	

