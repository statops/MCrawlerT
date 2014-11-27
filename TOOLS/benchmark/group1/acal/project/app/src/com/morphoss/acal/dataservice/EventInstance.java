package com.morphoss.acal.dataservice;

import java.util.ArrayList;

import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.acaltime.AcalDuration;
import com.morphoss.acal.davacal.AcalAlarm;
import com.morphoss.acal.davacal.PropertyName;
import com.morphoss.acal.davacal.RecurrenceId;
import com.morphoss.acal.davacal.VEvent;

public class EventInstance extends CalendarInstance {

	public EventInstance(VEvent vEvent, long collectionId, long resourceId, RecurrenceId rrid ) {
		super(vEvent, collectionId, resourceId, rrid, false);

	}

	private EventInstance(EVENT_BUILDER builder) throws BadlyConstructedEventException {
		super(
				builder.collectionId, -1, builder.start, builder.duration.getEndDate(builder.start),
				builder.alarmList, null, builder.start.toPropertyString(PropertyName.RECURRENCE_ID),
				builder.summary, null, null, true);
	}
	
	public static class EVENT_BUILDER {
		private ArrayList<AcalAlarm> alarmList = new ArrayList<AcalAlarm>();
		private AcalDateTime start;
		private AcalDuration duration;
		private String summary;
		private long collectionId = -1;

		
		public AcalDateTime getStart() { return this.start; }
		public AcalDuration getDuration() { return this.duration; }

		public EVENT_BUILDER setStart(AcalDateTime start) {
			this.start = start;
			return this;
		}

		public EVENT_BUILDER setDuration(AcalDuration duration) {
			this.duration = duration;
			return this;
		}

		public EVENT_BUILDER setSummary(String summary) {
			this.summary = summary;
			return this;
		}

		public EVENT_BUILDER setCollection(long collectionId) {
			this.collectionId = collectionId;
			return this;
		}

		public EVENT_BUILDER addAlarm(AcalAlarm alarm) {
			this.alarmList.add(alarm);
			return this;
		}
		
		public EventInstance build() throws BadlyConstructedEventException {
			return new EventInstance(this);
		}


	}

	public static class BadlyConstructedEventException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
}
