package net.fercanet.LNM;

import android.content.Context;
import android.media.MediaPlayer;

public class Player {
	private static MediaPlayer mp = new MediaPlayer();
	static int noteFiles[] = { 0,           R.raw.re_0, R.raw.mi_0, R.raw.fa_0, R.raw.sol_0, R.raw.la_0, R.raw.si_0,
		                       R.raw.don_1, R.raw.re_1, R.raw.mi_1, R.raw.fa_1, R.raw.sol_1, R.raw.la_1, R.raw.si_1,
		                       R.raw.don_2, R.raw.re_2, R.raw.mi_2, R.raw.fa_2, R.raw.sol_2, R.raw.la_2, R.raw.si_2,
		                       R.raw.don_3, R.raw.re_3, R.raw.mi_3 };
	
	static String[] notesList = { "don", "re", "mi", "fa", "sol", "la", "si" };
	
	public static void play(Context context, String clickedNote, String correctNote) {
		mp.release();
		
		int noteFile;
		try {
			noteFile = noteFiles[fileIndex(clickedNote, correctNote)];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		mp = MediaPlayer.create(context, noteFile);
		mp.start(); 
	}
	
	private static int fileIndex(String clickedNote, String correctNoteWithPitch) throws Exception {
		String[] parsedNote = correctNoteWithPitch.split("_");
		String correctNote = parsedNote[0];
		int pitch = Integer.parseInt(parsedNote[1]);
		
		int clickedNoteIndex = indexOf(clickedNote);
		int correctNoteIndex = indexOf(correctNote);
		int diffNote = clickedNoteIndex - correctNoteIndex;
		
		if (diffNote > 0) {
			if (diffNote > 3) {
				pitch--;
			}
		} else if (diffNote < 0) {
			if (diffNote < -3) {
				pitch++;
			}
		}
		
		return clickedNoteIndex + pitch * 7;
	}
	
	// I cannot believe there is nothing like this in Java
	// This is basic in Ruby!! Damn it!
	private static int indexOf(String element) throws Exception {
		for(int i = 0; i < notesList.length; i++) {
			if (element.equals(notesList[i])) {
				return i;
			}
		}
		throw new Exception(element + ": Unknown note!");
	}
}
