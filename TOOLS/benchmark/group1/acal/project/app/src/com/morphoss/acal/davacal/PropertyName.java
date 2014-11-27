package com.morphoss.acal.davacal;

public enum PropertyName {
	UID, DTSTAMP, CREATED, LAST_MODIFIED, DTSTART, DTEND, DUE, DURATION, LOCATION, SUMMARY,
	DESCRIPTION, RRULE, RDATE, EXDATE, PERCENT_COMPLETE, COMPLETED, STATUS, TRIGGER, ACTION,
	RECURRENCE_ID, VERSION, SEQUENCE, N, FN, REV, ARBITRARY, INVALID;

	private String arbitraryName = null;
	
	public String toString() {
		if ( arbitraryName != null ) return arbitraryName;
		return super.toString().replace('_', '-');
	}

	public static PropertyName arbitrary( String name ) {
		PropertyName p = ARBITRARY;
		ARBITRARY.arbitraryName = name;
		return p;
	}

	/**
	 * Returns a static array of the properties which can be localised with a TZID.
	 * @return
	 */
	public static PropertyName[] localisableDateProperties() {
		return new PropertyName[] { DTSTART, DTEND, DUE, COMPLETED };
	}
};

