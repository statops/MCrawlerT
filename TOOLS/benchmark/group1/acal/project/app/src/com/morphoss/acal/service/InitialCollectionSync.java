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

import android.content.ContentValues;

import com.morphoss.acal.HashCodeUtil;
import com.morphoss.acal.database.resourcesmanager.ResourceManager;
import com.morphoss.acal.database.resourcesmanager.requests.RRInitialCollectionSync;

public class InitialCollectionSync extends ServiceJob {

	private int serverId = -2;
	private String collectionPath = null;
	ContentValues collectionValues = null;
	public static final int MAX_RESULTS = 100;
	
	private RRInitialCollectionSync request;

	public InitialCollectionSync (long collectionId ) {
		request = new RRInitialCollectionSync(collectionId);
		
	}
	
	public InitialCollectionSync (long collectionId, int serverId, String collectionPath) {
		request = new RRInitialCollectionSync(collectionId, serverId, collectionPath);
	}

	public InitialCollectionSync (int serverId, String collectionPath) {
		request = new RRInitialCollectionSync(serverId, collectionPath);
	}
	
	
	@Override
	public void run(aCalService context) {
		request.setService(context);
		ResourceManager rm = ResourceManager.getInstance(context);
		//send request
		rm.sendRequest(request);
		//block until response completed
		while (!request.isProcessed()) {
			Thread.yield();
			try { Thread.sleep(100); } catch (Exception e) {}
		}
	}
	
	public boolean equals(Object that) {
		if ( this == that ) return true;
	    if ( !(that instanceof InitialCollectionSync) ) return false;
	    InitialCollectionSync thatCis = (InitialCollectionSync)that;
	    return (this.request == thatCis.request);
	}
	
	public int hashCode() {
		int result = HashCodeUtil.SEED;
		result = HashCodeUtil.hash( result, this.serverId );
	    result = HashCodeUtil.hash( result, this.collectionPath );
	    return result;
	}
	
	
	@Override
	public String getDescription() {
		return "Initial collection sync";
	}
	


}
