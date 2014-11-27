package com.morphoss.acal.dataservice;

import android.os.Parcel;
import android.os.Parcelable;

import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.davacal.VComponent;

public class ComponentResource extends Resource implements Parcelable {

	VComponent component = null;
	
	public ComponentResource(long cid, long rid, String name, String etag, String cType, String data, boolean sync,
			Long earliestStart, Long latestEnd, String eType, boolean pending, AcalDateTime lastModified) {
		super(cid, rid, name, etag, cType, data, sync, earliestStart, latestEnd, eType, pending, lastModified);
	}

	public ComponentResource(Parcel in) {
		super(in);
	}

	private void parseBlob() {
		if ( component == null )
			component = VComponent.createComponentFromBlob(getBlob());
	}

	public String getCurrentBlob() {
		parseBlob();
		return component.getCurrentBlob();
	}
	
	public void setEditable() {
		parseBlob();
		component.setEditable();
	}

}
