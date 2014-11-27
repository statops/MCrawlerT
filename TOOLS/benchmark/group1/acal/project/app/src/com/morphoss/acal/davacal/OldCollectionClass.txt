/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.morphoss.acal.davacal;

import android.content.ContentValues;
import android.graphics.Color;
import android.os.Parcel;
import android.util.Log;

import com.morphoss.acal.service.aCalService;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.dataservice.Collection;
import com.morphoss.acal.providers.DavCollections;

public class AcalCollection extends Collection {
	final private static String TAG = "AcalCollection";
	private ContentValues cv;
	private int collectionColour;
	public boolean alarmsEnabled;
	public final int collectionId;
	public final boolean useForEvents;
	public final boolean useForTasks;
	public final boolean useForJournal;
	public final boolean useForAddressbook;

	public AcalCollection( ContentValues collectionRow ) {
		cv = collectionRow;
		setColour(cv.getAsString(DavCollections.COLOUR));
		alarmsEnabled = (cv.getAsInteger(DavCollections.USE_ALARMS) == 1);
		collectionId = cv.getAsInteger(DavCollections._ID);
		useForEvents = (cv.getAsInteger(DavCollections.ACTIVE_EVENTS) == 1);
		useForTasks = (cv.getAsInteger(DavCollections.ACTIVE_TASKS) == 1);
		useForJournal = (cv.getAsInteger(DavCollections.ACTIVE_JOURNAL) == 1);
		useForAddressbook = (cv.getAsInteger(DavCollections.ACTIVE_ADDRESSBOOK) == 1);
	}

	public static AcalCollection fromDatabase(long collectionId ) {
		ContentValues collectionRow = DavCollections.getRow(collectionId, aCalService.context.getContentResolver());
		if ( collectionRow == null ) return null;
		return new AcalCollection(collectionRow);
	}
	

	public void updateCollectionRow( ContentValues collectionRow ) {
		if ( cv.getAsInteger(DavCollections._ID) != collectionId ) {
			Log.w(TAG,"Attempt to re-use AcalCollection with different Collection ID");
			try {
				throw new Exception("");
			}
			catch ( Exception e ) {
				Log.w(TAG,Log.getStackTraceString(e));
			}
			return;
		}
		cv.putAll(collectionRow);
		if (cv.containsKey(DavCollections.COLOUR)){
			setColour(cv.getAsString(DavCollections.COLOUR));
		}
		if (cv.containsKey(DavCollections.USE_ALARMS)){
			alarmsEnabled = (cv.getAsInteger(DavCollections.USE_ALARMS) == 1);
		}
	}

	public int getColour() {
		return collectionColour;
	}

	public int setColour( String colourString ) {
		if ( colourString == null ) colourString = StaticHelpers.randomColorString();
		try {
			collectionColour = Color.parseColor(colourString);
		} catch (IllegalArgumentException iae) {
			collectionColour = Color.parseColor("#00f");	//Default blue
		}
		return collectionColour;
	}

	public ContentValues getCollectionRow() {
		return cv;
	}

	public long getCollectionId() {
		return collectionId;
	}

	public CharSequence getDisplayName() {
		return cv.getAsString(DavCollections.DISPLAYNAME);
	}

	@Override
	public boolean alarmsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}
}
