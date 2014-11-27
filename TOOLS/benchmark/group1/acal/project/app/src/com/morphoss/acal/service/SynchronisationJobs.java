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

package com.morphoss.acal.service;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.providers.DavCollections;
import com.morphoss.acal.providers.Servers;

public class SynchronisationJobs extends ServiceJob {

	public static final int		HOME_SET_DISCOVERY	= 0;
	public static final int		HOME_SETS_UPDATE	= 1;
	public static final int		CALENDAR_SYNC		= 2;
	public static final int		CACHE_RESYNC		= 3;

	public static final String	TAG					= "aCal SynchronisationJobs";

	public int					TIME_TO_EXECUTE		= 0;							// immediately
	private int					jobtype;
	private aCalService			context;

	public static enum WriteActions { UPDATE, INSERT, DELETE };

	
	public SynchronisationJobs( int jobtype ) {
		this.jobtype = jobtype;
	}

	@Override
	public void run( aCalService context ) {
		this.context = context;
		switch (jobtype) {
			case HOME_SET_DISCOVERY:
				refreshHomeSets();
				break;

			case HOME_SETS_UPDATE:
				refreshCollectionsFromHomeSets();
				break;

			case CACHE_RESYNC:
				if ( Constants.LOG_DEBUG ) Log.i(TAG,"Responding to internal cache revalidation request.");
				break;
		}
	}

	public synchronized void refreshHomeSets() {
		Cursor mCursor = context.getContentResolver().query(Servers.CONTENT_URI,
					new String[] { Servers._ID, Servers.ACTIVE }, null, null, null);
		mCursor.moveToFirst();
		//Set<HomeSetDiscovery> hsd = new HashSet<HomeSetDiscovery>();
		while (!mCursor.isAfterLast()) {
			if (mCursor.getInt(1) == 1) {
				ServiceJob sj = new HomeSetDiscovery(mCursor.getInt(0));
				context.addWorkerJob(sj);
			}
			mCursor.moveToNext();
		}
		mCursor.close();
	}

	public synchronized void refreshCollectionsFromHomeSets() {
		Cursor mCursor = context.getContentResolver().query(Servers.CONTENT_URI,
					new String[] { Servers._ID, Servers.ACTIVE }, null, null, null);
		mCursor.moveToFirst();
		while (!mCursor.isAfterLast()) {
			if (mCursor.getInt(1) == 1) {
				ServiceJob sj = new HomeSetsUpdate(mCursor.getInt(0));
				context.addWorkerJob(sj);
			}
			mCursor.moveToNext();
		}
		mCursor.close();

	}

	/**
	 * Creates a sync job for ALL active collections.  If the collection was
	 * last synchronised less than 14 days ago we do a syncCollectionContents
	 * otherwise we do an initialCollectionSync.  We try and do these sync
	 * jobs with gaps between them.
	 *  
	 * @param worker
	 * @param context
	 */
	public static void startCollectionSync(WorkerClass worker, Context context, long startInMillis) {
		ContentValues[] collectionsList = DavCollections.getCollections(context.getContentResolver(),
					DavCollections.INCLUDE_ALL_ACTIVE);
		String lastSyncString;
		AcalDateTime lastSync;
		int collectionId;

		for (ContentValues collectionValues : collectionsList) {
			collectionId = collectionValues.getAsInteger(DavCollections._ID);
			lastSyncString = collectionValues.getAsString(DavCollections.LAST_SYNCHRONISED);
			if (lastSyncString != null) {
				lastSync = AcalDateTime.fromString(lastSyncString);
				if (lastSync.addDays(14).getMillis() > System.currentTimeMillis()) {
					// In this case we will schedule a normal sync on the collection
					// which will hopefully be *much* lighter weight.
					SyncCollectionContents job = new SyncCollectionContents(collectionId);
					job.TIME_TO_EXECUTE = startInMillis;
					worker.addJobAndWake(job);
					startInMillis += 5000;
				}
			}
			else {
				InitialCollectionSync job = new InitialCollectionSync(collectionId);
				job.TIME_TO_EXECUTE = startInMillis;
				worker.addJobAndWake(job);
				startInMillis += 15000;
			}
		}

	}
	
	
	// The following overrides are to prevent duplication of these jobs in the queue
	public boolean equals(Object o) {
		if (!(o instanceof SynchronisationJobs)) return false;
		if (((SynchronisationJobs) o).jobtype == this.jobtype) return true;
		return false;
	}

	public int hashCode() {
		return this.jobtype;
	}



	/**
	 * Returns a minimal Header[] array for a REPORT request.
	 * @param depth
	 * @return
	 */
	public static Header[] getReportHeaders( int depth ) {
		return new Header[] {
					new BasicHeader("Content-Type", "text/xml; encoding=UTF-8"),
					new BasicHeader("Brief","T"),
					new BasicHeader("Depth", Integer.toString(depth))
				};
	}

	
	
	@Override
	public String getDescription() {
		switch( jobtype ) {
			case HOME_SETS_UPDATE:
				return "Updating collections in all home sets";
			case HOME_SET_DISCOVERY:
				return "Discovering home sets for all servers";
			case CACHE_RESYNC:
				return "Resync internal database cache";
		}
		Log.e(TAG,"No description defined for jobtype "+jobtype );
		return "Unknown SynchronisationJobs jobtype!";
	}

}
