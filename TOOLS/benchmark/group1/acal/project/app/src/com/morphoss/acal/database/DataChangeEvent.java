package com.morphoss.acal.database;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.morphoss.acal.database.DatabaseTableManager.QUERY_ACTION;

public class DataChangeEvent  implements Parcelable {
	
	public static final String TAG = "aCal DataChangeEvent";
	
	public final QUERY_ACTION action;
	private final ContentValues data;
	public DataChangeEvent(QUERY_ACTION action, ContentValues data) { 
	
		if (action == null) {
			NullPointerException e = new NullPointerException("Change Event can not be created with a null action.");
			Log.e(TAG, "Invalid DataChangeEvent Creation: "+Log.getStackTraceString(e));
			throw e;
		}
		
		this.action = action; 
		this.data = data; 
	}
	public ContentValues getData() {
		return new ContentValues(data);
	}
	
	public DataChangeEvent(Parcel in) {
		action = QUERY_ACTION.values()[in.readInt()];
		data = ContentValues.CREATOR.createFromParcel(in);
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(action.ordinal());
		dest.writeParcelable(data, 0);
	}
	

	public static final Parcelable.Creator<DataChangeEvent> CREATOR = new Parcelable.Creator<DataChangeEvent>() {
		public DataChangeEvent createFromParcel(Parcel in) {
			return new DataChangeEvent(in);
		}

		public DataChangeEvent[] newArray(int size) {
			return new DataChangeEvent[size];
		}
	};

}