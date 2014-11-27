package com.morphoss.acal.database.cachemanager;

import java.util.TimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;
import com.morphoss.acal.dataservice.Resource;
import com.morphoss.acal.davacal.Masterable;
import com.morphoss.acal.davacal.PropertyName;

/**
 * Represents a single row in the cache
 * @author Chris Noldus
 *
 */
public class CacheObject implements Parcelable, Comparable<CacheObject> {
	private final long rid;
	private final String resourceType;
	private final String rrid;	//Recurence id, -1 if not recurs.
	private final long cid;
	private final String summary;
	private final String location;
	private final long start;
	private final long end;
	private final boolean startFloating;
	private final boolean endFloating;
	private final boolean completeFloating;
	private final long completed;
	private final int flags;
	
	public static final int HAS_ALARM_FLAG = 		1;
	public static final int RECURS_FLAG =			1<<1;
	public static final int FLAG_ALL_DAY = 			1<<2;
	

	
	//builds a clone of an existing cache object - useful for classes that wish to extend this one.
	protected CacheObject(CacheObject original) {
		this.rid = original.rid;
		this.resourceType = original.resourceType;
		this.rrid = original.rrid;
		this.cid = original.cid;
		this.summary = original.summary;
		this.location= original.location;
		this.start = original.start;
		this.end = original.end;
		this.completed = original.completed;
		this.startFloating = original.startFloating;
		this.endFloating = original.endFloating;
		this.completeFloating = original.completeFloating;
		this.flags = original.flags;
	}
	
	public CacheObject(long rid, String resourceType, String rrid,  long cid, String sum, String loc, long st, long end, long completed, boolean sfloat, boolean efloat, boolean cfloat, int flags) {
		this.rid = rid;
		this.resourceType = resourceType;
		this.rrid = rrid;
		this.cid = cid;
		this.summary = sum;
		this.location= loc;
		this.start = st;
		this.end = end;
		this.completed = completed;
		this.startFloating = sfloat;
		this.endFloating = efloat;
		this.completeFloating = cfloat;
		this.flags = flags;
	}

	public CacheObject( Masterable masterInstance, long collectionId, long resourceId ) {
		this.rid = resourceId;
		this.resourceType = masterInstance.name;
		this.cid = collectionId;
		this.summary = masterInstance.getSummary();
		this.location = masterInstance.getLocation();

		int flags = 0;
		String recurrenceId = null;

		AcalDateTime aDate = masterInstance.getStart();
		this.start = (aDate == null ? Long.MAX_VALUE : aDate.getMillis());
		startFloating = (aDate == null ? true : aDate.isFloating());
		if ( aDate != null ) {
			recurrenceId = aDate.toPropertyString(PropertyName.RECURRENCE_ID);
			if (aDate.isDate()) flags+= FLAG_ALL_DAY;
		}

		aDate = masterInstance.getEnd();
		this.end = (aDate == null ? Long.MAX_VALUE : aDate.getMillis());
		endFloating = (aDate == null ? true : aDate.isFloating());
		if ( aDate != null && recurrenceId == null)
			recurrenceId = aDate.toPropertyString(PropertyName.RECURRENCE_ID);

		aDate = AcalDateTime.fromAcalProperty(masterInstance.getProperty(PropertyName.COMPLETED));
		this.completed = (aDate == null ? Long.MAX_VALUE : aDate.getMillis());
		completeFloating = (aDate == null ? true : aDate.isFloating());
		if ( aDate != null  && recurrenceId == null )
			recurrenceId = aDate.toPropertyString(PropertyName.RECURRENCE_ID);
		this.rrid = recurrenceId;
		
		if (!masterInstance.getAlarms().isEmpty()) flags+=HAS_ALARM_FLAG;
		if (masterInstance.getProperty(PropertyName.RRULE) != null) flags+=RECURS_FLAG;

		this.flags = flags;
	}
	
	//Generate a cacheObject from a Masterable with dates
	public CacheObject( Masterable masterInstance, long collectionId, long resourceId, AcalDateTime dtstart, AcalDateTime dtend, AcalDateTime completed) {
		this.rid = resourceId;
		this.resourceType = masterInstance.getEffectiveType();
		this.cid = collectionId;
		this.summary = masterInstance.getSummary();
		this.location = masterInstance.getLocation();

		int flags = 0;
		String recurrenceId = null;

		this.start = (dtstart == null ? Long.MAX_VALUE : dtstart.getMillis());
		startFloating = (dtstart == null ? true : dtstart.isFloating());
		if ( dtstart != null )
			recurrenceId = dtstart.toPropertyString(PropertyName.RECURRENCE_ID);
		if (dtstart.isDate()) flags+= FLAG_ALL_DAY;

		this.end = (dtend == null ? Long.MAX_VALUE : dtend.getMillis());
		endFloating = (dtend == null ? true : dtend.isFloating());
		if ( dtend != null  && recurrenceId == null)
			recurrenceId = dtend.toPropertyString(PropertyName.RECURRENCE_ID);

		this.completed = (completed == null ? Long.MAX_VALUE : completed.getMillis());
		completeFloating = (completed == null ? true : completed.isFloating());
		if ( completed != null  && recurrenceId == null)
			recurrenceId = completed.toPropertyString(PropertyName.RECURRENCE_ID);
		this.rrid = recurrenceId;
		
		if (!masterInstance.getAlarms().isEmpty()) flags+=HAS_ALARM_FLAG;
		if (masterInstance.getProperty(PropertyName.RRULE) != null) flags+=RECURS_FLAG;

		this.flags = flags;
	}

	
	private CacheObject(Parcel in) {
		rid = in.readLong();
		resourceType = in.readString();
		rrid = in.readString();
		cid = in.readLong();
		summary = in.readString();
		location = in.readString();
		startFloating = (in.readByte() == 'T' ? true : false);
		endFloating = (in.readByte() == 'T' ? true : false);
		completeFloating = (in.readByte() == 'T' ? true : false);
		start = in.readLong();
		end = in.readLong();
		completed = in.readLong();
		flags = in.readInt();

	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(rid);
		dest.writeString(resourceType);
		dest.writeString(rrid);
		dest.writeLong(cid);
		dest.writeString(summary);
		dest.writeString(location);
		dest.writeByte((byte) (startFloating ? 'T' : 'F'));
		dest.writeByte((byte) (endFloating ? 'T' : 'F'));
		dest.writeByte((byte) (completeFloating ? 'T' : 'F'));
		dest.writeLong(start);
		dest.writeLong(end);
		dest.writeLong(completed);
		dest.writeInt(flags);
	}
	
	public static final Parcelable.Creator<CacheObject> CREATOR = new Parcelable.Creator<CacheObject>() {
		public CacheObject createFromParcel(Parcel in) {
			return new CacheObject(in);
		}

		public CacheObject[] newArray(int size) {
			return new CacheObject[size];
		}
	};

	/**
	 * The summary of this resource
	 * @return
	 */
	public String getSummary() {
		return this.summary;
	}

	/**
	 * Whether this resource has associated alarms
	 * @return
	 */
	public boolean hasAlarms() {
		return (flags&HAS_ALARM_FLAG)>0;
	}
	
	/**
	 * The collection id
	 * @return
	 */
	public long getCollectionId() {
		return cid;
	}

	/**
	 * The resource id
	 * @return
	 */
	public long getResourceId() {
		return rid;
	}

	
	/**
	 * Whether this resource has recurrences
	 * @return
	 */
	public boolean isRecurring() {
		return (flags&RECURS_FLAG)>0;
	}

	/**
	 * Whether this task is overdue
	 * @return
	 */
	public boolean isOverdue() {
		return CacheTableManager.RESOURCE_TYPE_VTODO.equals(resourceType) &&
				(completed == Long.MAX_VALUE) &&
				(end - (endFloating?TimeZone.getDefault().getOffset(end) : 0)) < System.currentTimeMillis();
	}

	/**
	 * Whether this task is completed
	 * @return
	 */
	public boolean isCompleted() {
		return CacheTableManager.RESOURCE_TYPE_VTODO.equals(resourceType) && (completed != Long.MAX_VALUE);
	}

	/**
	 * The location associated with this resource
	 * @return
	 */
	public String getLocation() {
		return this.location;
	}
	
	/**
	 * The start time (in millis) as UTC of this resource
	 * @return
	 */
	public long getStart() {
		return start;
	}
	
	/**
	 * The end time as UTC of this resource
	 * @return
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * Whether this resource has an all day date range.
	 * @return
	 */
	public boolean isAllDay() {
		return (flags&FLAG_ALL_DAY)>0;
	}
	
	public ContentValues getCacheCVs() {
		ContentValues cv =  new ContentValues();
		
		cv.put(CacheTableManager.FIELD_RESOURCE_ID,rid);
		cv.put(CacheTableManager.FIELD_RESOURCE_TYPE, this.resourceType);
		cv.put(CacheTableManager.FIELD_RECURRENCE_ID, this.rrid);
		cv.put(CacheTableManager.FIELD_CID,cid);
		cv.put(CacheTableManager.FIELD_SUMMARY,this.summary);
		cv.put(CacheTableManager.FIELD_LOCATION,this.location);
		cv.put(CacheTableManager.FIELD_DTSTART, this.start);
		cv.put(CacheTableManager.FIELD_DTEND, this.end);
		cv.put(CacheTableManager.FIELD_COMPLETED, this.completed);
		cv.put(CacheTableManager.FIELD_DTSTART_FLOAT, this.startFloating?1:0);
		cv.put(CacheTableManager.FIELD_DTEND_FLOAT, this.endFloating?1:0);
		cv.put(CacheTableManager.FIELD_COMPLETE_FLOAT, this.completeFloating?1:0);
		cv.put(CacheTableManager.FIELD_FLAGS, this.flags);
		return cv;
	}
	
	public static CacheObject fromContentValues(ContentValues row) {
		return new CacheObject(
					row.getAsLong(CacheTableManager.FIELD_RESOURCE_ID), 
					row.getAsString(CacheTableManager.FIELD_RESOURCE_TYPE), 
					row.getAsString(CacheTableManager.FIELD_RECURRENCE_ID),
					row.getAsInteger(CacheTableManager.FIELD_CID),
					row.getAsString(CacheTableManager.FIELD_SUMMARY),
					row.getAsString(CacheTableManager.FIELD_LOCATION),
					row.getAsLong(CacheTableManager.FIELD_DTSTART),
					row.getAsLong(CacheTableManager.FIELD_DTEND),
					row.getAsLong(CacheTableManager.FIELD_COMPLETED),
					row.getAsInteger(CacheTableManager.FIELD_DTSTART_FLOAT) ==1,
					row.getAsInteger(CacheTableManager.FIELD_DTEND_FLOAT) ==1,
					row.getAsInteger(CacheTableManager.FIELD_COMPLETE_FLOAT) ==1,
					row.getAsInteger(CacheTableManager.FIELD_FLAGS)
				);
	}

	@Override
	public int compareTo(CacheObject another) {
		return (int) (this.start - another.start);
	}

	/**
	 * Is this a CacheObject of a VEVENT
	 * @return
	 */
	public boolean isEvent() {
		return CacheTableManager.RESOURCE_TYPE_VEVENT.equals(resourceType);
	}

	/**
	 * Is this a CacheObject of a VTODO
	 * @return
	 */
	public boolean isTodo() {
		return CacheTableManager.RESOURCE_TYPE_VTODO.equals(resourceType);
	}

	public String getRecurrenceId() {
		return this.rrid;
	}

	public AcalDateTime getStartDateTime() {
		if ( start == Long.MAX_VALUE ) return null;
		return AcalDateTime.localTimeFromMillis(start,startFloating);
	}

	public AcalDateTime getEndDateTime() {
		if ( end == Long.MAX_VALUE ) return null;
		return AcalDateTime.localTimeFromMillis(end,endFloating);
	}

	public AcalDateTime getCompletedDateTime() {
		if ( completed == Long.MAX_VALUE ) return null;
		return AcalDateTime.localTimeFromMillis(completed,completeFloating);
	}
	
	public AcalDateRange getRange() {
		return new AcalDateRange(getStartDateTime(),getEndDateTime());
	}
	
	public boolean equals(Object other) {
		if (this == other) return true;
		if (! (other instanceof CacheObject)) return false;
		return (this.cid == ((CacheObject)other).cid);
	}

	public void logInvalidObject( Context c, String TAG, Exception e) {
		Log.println(Constants.LOGI, TAG, Log.getStackTraceString(e) );
		try {
			Resource resource = Resource.fromDatabase(c, rid);
			Log.println(Constants.LOGI, TAG, "Dumping blob for resource ID "+rid);
			Log.println(Constants.LOGI, TAG, resource.getBlob());
		}
		catch( Exception e1 ) {
			Log.w(TAG,"Exception trying to log invalid object!", e1);
		}
	}
}
