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

import com.morphoss.acal.acaltime.AcalDateTime;


public abstract class ServiceJob implements Comparable<ServiceJob> {
	
	public long TIME_TO_EXECUTE = 0;	//immediately
	
	@Override
	public int compareTo(ServiceJob arg0) {
		if ( this.TIME_TO_EXECUTE < 864000000 ) this.TIME_TO_EXECUTE += System.currentTimeMillis();
		if ( arg0.TIME_TO_EXECUTE < 864000000 ) arg0.TIME_TO_EXECUTE += System.currentTimeMillis();
		return (int)(this.TIME_TO_EXECUTE - arg0.TIME_TO_EXECUTE);
	}
	
	public abstract void run(aCalService context);
	
	public abstract String getDescription();
	
	public String toString() {
		StringBuilder sb = new StringBuilder("At: ");
		AcalDateTime when = AcalDateTime.getUTCInstance();
		when.setMillis(TIME_TO_EXECUTE + (TIME_TO_EXECUTE < 864000000 ? System.currentTimeMillis() : 0));
		sb.append(when.fmtIcal());
		sb.append(", Job: ");
		sb.append(getDescription());
		return sb.toString( );
	}
}
