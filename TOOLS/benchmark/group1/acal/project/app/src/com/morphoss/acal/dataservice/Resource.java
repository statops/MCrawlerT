package com.morphoss.acal.dataservice;

import java.util.Date;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.ResourceManager.ResourceTableManager;
import com.morphoss.acal.database.resourcesmanager.ResourceResponse;
import com.morphoss.acal.database.resourcesmanager.requests.RRRequestInstanceBlocking;


public class Resource implements Parcelable {

	private static final String TAG = "acal Resource";
	//This class is immutable!
	private final long collectionId;
	private final long resourceId;
	private final String name;
	private final String etag;
	private String contentType;
	private String data;
	private boolean needsSync;
	private final long earliestStart;
	private final long latestEnd;
	private final String effectiveType;
	private boolean pending;
	private final AcalDateTime lastModified;
	
	public Resource(long cid, long rid, String name, String etag, String cType, 
			String data, boolean sync, Long earliestStart, Long latestEnd, String eType, boolean pending, AcalDateTime lastModified) {
		this.collectionId = cid;
		this.resourceId = rid;
		this.name = name;
		this.etag = etag;
		this.contentType = cType;
		this.data = data;
		this.needsSync = sync;
		if (earliestStart == null) earliestStart = Long.MIN_VALUE;
		this.earliestStart = earliestStart;
		if (latestEnd == null) latestEnd = Long.MAX_VALUE;
		this.latestEnd = latestEnd;
		this.effectiveType = eType;
		this.pending = pending;
		this.lastModified = lastModified;
	}
	
	public Resource(Parcel in) {
		collectionId = in.readLong();
		resourceId = in.readLong();
		this.name = in.readString();
		this.etag = in.readString();
		this.contentType = in.readString();
		this.data = in.readString();
		this.needsSync = in.readByte() == 'T';
		this.earliestStart = in.readLong();
		this.latestEnd = in.readLong();
		this.effectiveType = in.readString();
		this.pending = in.readByte() == 'T';
		this.lastModified = new AcalDateTime(in);
	}
	
	public long getCollectionId() {
		return this.collectionId;
	}

	public long getResourceId() {
		return this.resourceId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(collectionId);
		dest.writeLong(resourceId);
		dest.writeString(name);
		dest.writeString(etag);
		dest.writeString(contentType);
		dest.writeString(data);
		dest.writeByte(this.needsSync ? (byte)'T' : (byte)'F');
		dest.writeLong(earliestStart);
		dest.writeLong(latestEnd);
		dest.writeString(effectiveType);
		dest.writeByte(this.pending ? (byte)'T' : (byte)'F');
		lastModified.writeToParcel(dest, flags);
	}

	public String getBlob() {
		return this.data;
	}

	public String getEtag() {
		return this.etag;
	}
	
	public ContentValues toContentValues() {
		ContentValues cv = new ContentValues();
		cv.put(ResourceTableManager.RESOURCE_ID,resourceId);
		cv.put(ResourceTableManager.COLLECTION_ID,collectionId);
		cv.put(ResourceTableManager.RESOURCE_NAME,name);
		cv.put(ResourceTableManager.ETAG,etag);
		cv.put(ResourceTableManager.CONTENT_TYPE,contentType);
		cv.put(ResourceTableManager.RESOURCE_DATA,data);
		cv.put(ResourceTableManager.NEEDS_SYNC,needsSync ? 1 : 0);
		cv.put(ResourceTableManager.EARLIEST_START, earliestStart);
		cv.put(ResourceTableManager.LATEST_END, latestEnd);
		cv.put(ResourceTableManager.EFFECTIVE_TYPE, effectiveType);
		cv.put(ResourceTableManager.LAST_MODIFIED, lastModified.httpDateString());
		return cv;
	}

	
	public static Resource fromContentValues(ContentValues cv) {
		long cid = -1;
		long rid = -1;
		boolean pending = false;
		String blob = null;
		long earliestStart = Long.MIN_VALUE;
		long latestEnd = Long.MAX_VALUE;
		boolean needsSync = false;
		String effectiveType = "";
		blob = cv.getAsString(ResourceTableManager.NEW_DATA);
		AcalDateTime modTime = new AcalDateTime();
		if ( blob != null && cv.containsKey(ResourceTableManager.PEND_RESOURCE_ID) ) {
			cid = cv.getAsLong(ResourceTableManager.PEND_COLLECTION_ID);
			rid = cv.getAsLong(ResourceTableManager.PEND_RESOURCE_ID);
			pending = true;
		}
		else if (cv.containsKey(ResourceTableManager.RESOURCE_ID) || cv.containsKey(ResourceTableManager.PEND_RESOURCE_ID)) {
			try {
				cid = cv.getAsLong(ResourceTableManager.COLLECTION_ID);
				if ( blob == null && cv.containsKey(ResourceTableManager.PEND_RESOURCE_ID) )
					rid = cv.getAsLong(ResourceTableManager.PEND_RESOURCE_ID);
				else
					rid = cv.getAsLong(ResourceTableManager.RESOURCE_ID);
				blob = cv.getAsString(ResourceTableManager.RESOURCE_DATA);
				effectiveType = cv.getAsString(ResourceTableManager.EFFECTIVE_TYPE);
				try {
					earliestStart = cv.getAsLong(ResourceTableManager.EARLIEST_START);
				} catch( Exception e ) {}
				try {
					latestEnd = cv.getAsLong(ResourceTableManager.LATEST_END);
				} catch( Exception e ) {}

				try {
					modTime = AcalDateTime.fromMillis(Date.parse(cv.getAsString(ResourceTableManager.LAST_MODIFIED)));
				} catch( Exception e ) {}
				modTime.setTimeZone(AcalDateTime.UTC.getID());
				needsSync = StaticHelpers.toBoolean(cv.getAsInteger(ResourceTableManager.NEEDS_SYNC),true);
			}
			catch (Exception e) {
				Log.println(Constants.LOGD, TAG,"Error in Resource: "+e+Log.getStackTraceString(e)); 
			}
		}
		else {
			Log.println(Constants.LOGD, TAG,"Resource ID Required"); 
			String v;
			for( Entry<String,Object> entry : cv.valueSet() ) {
				try {
					v = entry.getValue().toString();
				}
				catch( Exception e ) {
					v = "invalid";
				}
				Log.println(Constants.LOGD, TAG, entry.getKey()+"="+v);
			}
			throw new IllegalArgumentException("Resource ID Required");
		}

		
		return new Resource(
				cid,
				rid,
				cv.getAsString(ResourceTableManager.RESOURCE_NAME),
				cv.getAsString(ResourceTableManager.ETAG),
				cv.getAsString(ResourceTableManager.CONTENT_TYPE),
				blob,
				needsSync,
				earliestStart,
				latestEnd,
				effectiveType,
				pending,
				modTime
		);
		
	}

	/**
	 * Given a context and a resourceId we can retrieve a Resource from the database.
	 * @param context
	 * @param resourceId
	 * @return
	 */
	public static Resource fromDatabase( Context context, long resourceId ) {
		ResourceManager rm = ResourceManager.getInstance(context);
		
		ResourceResponse<ContentValues> cv = rm.sendBlockingRequest( new RRRequestInstanceBlocking(resourceId) );
		return Resource.fromContentValues(cv.result());
	}

	
	public boolean isPending() {
		return this.pending;
	}

	public Long getEarliestStart() {
		return earliestStart;
	}

	public Long getLatestEnd() {
		return latestEnd;
	}

	public AcalDateTime getLastModified() {
		return lastModified.clone();
	}

	public void setPending(boolean b) {
		this.pending = b;
	}
}
