package com.morphoss.acal.dataservice;

import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.davacal.RecurrenceId;
import com.morphoss.acal.davacal.VTodo;

public class TodoInstance extends CalendarInstance {

//	private final static boolean DEBUG = true && Constants.DEBUG_MODE;
//	private final static String TAG = "aCal TodoInstance";

	private final AcalDateTime completed;
	private int	percentComplete;

	/**
	 * Construct a new TodoInstance based on the supplied VTodo with start / due dates possibly overridden
	 * @param vTodo
	 * @param dtstart
	 * @param due
	 */
	public TodoInstance( VTodo vTodo, long collectionId, long resourceId, RecurrenceId rrid ) {
		super(vTodo, collectionId, resourceId, rrid, false);

		completed = vTodo.getCompleted();
		percentComplete = vTodo.getPercentComplete();
	}

	public AcalDateTime getDue() {
		return dtend;
	}

	public AcalDateTime getCompleted() {
		return completed;
	}

	public int getPercentComplete() {
		return percentComplete;
	}
}
