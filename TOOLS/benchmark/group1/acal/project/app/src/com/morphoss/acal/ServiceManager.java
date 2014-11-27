/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package com.morphoss.acal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.morphoss.acal.service.ServiceRequest;
import com.morphoss.acal.service.aCalService;

/**
 * This class provides a convenient interface that will allow any of the activities to connect
 * with the service. 
 * 
 * @author Morphoss Ltd
 *
 */
public class ServiceManager {

	public static final String TAG = "aCal ServiceManager";
	
	//private EventService eventService;
	private ServiceRequest serviceRequest;
	private boolean isServiceConnected = false;
	private Context requestor;
	private ServiceManagerCallBack scb;

	public ServiceManager(Context context) {
		this.requestor = context;
		this.connectService();
	}
	
	public ServiceManager(Context c, ServiceManagerCallBack scb) {
		this(c);
		this.scb = scb;
	}
	
	public boolean isConnected() {
		return this.isServiceConnected;
	}
	public void connectService() {
		try {
			if (this.isServiceConnected) return;
			requestor.bindService(new Intent(this.requestor, aCalService.class),myConnection,Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			Log.e(TAG, "Error connecting to service: "+e.getMessage());
		}
	}
	
	public ServiceRequest getServiceRequest() {
		connectService();
		return this.serviceRequest;
	}

	public void close() {
		if (isServiceConnected) requestor.unbindService(myConnection);
	}

	private ServiceConnection myConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			//eventService = EventService.Stub.asInterface(service);
			serviceRequest = ServiceRequest.Stub.asInterface(service);
			ServiceManager.this.isServiceConnected = true;
			if (scb != null) scb.serviceConnected(serviceRequest);

		}
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			//eventService = null;
			Log.e(TAG, "Connection to AcalService unnexpectedly disrupted.");
			serviceRequest = null;
			ServiceManager.this.isServiceConnected = false;
		}
	};
}
