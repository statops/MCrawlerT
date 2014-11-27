package com.morphoss.acal.dataservice;

import com.morphoss.acal.davacal.RecurrenceId;
import com.morphoss.acal.davacal.VJournal;

public class JournalInstance extends CalendarInstance {

//	private final static boolean DEBUG = true && Constants.DEBUG_MODE;
//	private final static String TAG = "aCal JournalInstance";


	/**
	 * Construct a new JournalInstance based on the supplied VJournal with start / due dates possibly overridden
	 * @param vJournal
	 * @param dtstart
	 * @param due
	 */
	public JournalInstance( VJournal vJournal, long collectionId, long resourceId, RecurrenceId rrid ) {
		super(vJournal, collectionId, resourceId, rrid, false);

	}

}
