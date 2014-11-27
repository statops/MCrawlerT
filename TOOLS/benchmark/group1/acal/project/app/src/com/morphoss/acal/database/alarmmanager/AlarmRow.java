package com.morphoss.acal.database.alarmmanager;

import android.content.ContentValues;

import com.morphoss.acal.database.alarmmanager.AlarmQueueManager.AlarmTableManager;

public class AlarmRow implements Comparable<AlarmRow> {

	private long id;
	private long ttf;
	private long rid;
	private String rrid;
	private ALARM_STATE state;
	private String blob;
	
	public AlarmRow(long id, long ttf, long rid, String rrid, ALARM_STATE state, String blob) {
		this.id = id;
		this.ttf = ttf;
		this.rid = rid;
		this.rrid = rrid;
		this.state = state;
		this.blob = blob;
	}
	
	public AlarmRow(long ttf, long rid, String rrid, ALARM_STATE state, String blob) {
		this(-1,ttf, rid,rrid,state, blob);
	}
	
	public AlarmRow(long ttf, long rid, String rrid, String blob) {
		this(-1,ttf, rid,rrid,ALARM_STATE.PENDING,blob);
	}
	
	public ContentValues toContentValues() {
		ContentValues cv = new ContentValues();
		if (id > 0) cv.put(AlarmTableManager.FIELD_ID, id);
		cv.put(AlarmTableManager.FIELD_TIME_TO_FIRE, ttf);
		cv.put(AlarmTableManager.FIELD_RID, rid);
		cv.put(AlarmTableManager.FIELD_RRID, rrid);
		cv.put(AlarmTableManager.FIELD_STATE, state.ordinal());
		cv.put(AlarmTableManager.FIELD_BLOB, blob);
		return cv;
	}
	
	public static AlarmRow fromContentValues(ContentValues cv) {
		
		if (!cv.containsKey(AlarmTableManager.FIELD_ID)) {
			cv = new ContentValues(cv);
			cv.put(AlarmTableManager.FIELD_ID, -1);
		}
		
		return new AlarmRow(
				cv.getAsLong(AlarmTableManager.FIELD_ID),
				cv.getAsLong(AlarmTableManager.FIELD_TIME_TO_FIRE),
				cv.getAsLong(AlarmTableManager.FIELD_RID),
				cv.getAsString(AlarmTableManager.FIELD_RRID),
				ALARM_STATE.values()[cv.getAsInteger(AlarmTableManager.FIELD_STATE)],
				cv.getAsString(AlarmTableManager.FIELD_BLOB)
		);
		
	}

	@Override
	public int compareTo(AlarmRow another) {
		return (int)(this.ttf - another.ttf);
	}

	public long getTimeToFire() {
		return this.ttf;
	}

	public void setState(ALARM_STATE state) {
		this.state = state;
		
	}

	public long getId() {
		return this.id;
	}

	public long getResourceId() {
		return this.rid;
	}

	public String getReccurenceId() {
		return this.rrid;
	}

	public String getBlob() {
		return this.blob;
	}

	public long getTTF() {
		return this.ttf;
	}
	
}
