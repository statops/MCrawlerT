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

import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.dataservice.TodoInstance;



public class VTodo extends Masterable {
	public static final String TAG = "aCal VTodo";

	public enum	Status {
		NEEDS_ACTION, IN_PROCESS, COMPLETED, CANCELLED;
		
		public String toString() {
			switch(this) {
				case NEEDS_ACTION: 	return "NEEDS-ACTION";
				case IN_PROCESS:	return "IN-PROCESS";
				default:
					return super.toString();
			}
		}

	}

	
	public VTodo(ComponentParts splitter, VComponent parent) {
		super(splitter, parent);
	}

	public VTodo( VCalendar parent ) {
		super(VComponent.VTODO, parent );
	}

	public VTodo() {
		this( new VCalendar() );
	}

	public AcalDateTime getDue() {
		AcalProperty aProp = getProperty(PropertyName.DUE);
		if ( aProp == null ) return null;
		return AcalDateTime.fromAcalProperty(aProp);
	}

	
	public void setDue( AcalDateTime newValue ) {
		setUniqueProperty(newValue.asProperty(PropertyName.DUE));
	}

	public AcalDateTime getCompleted() {
		AcalProperty aProp = getProperty(PropertyName.COMPLETED);
		if ( aProp == null ) return null;
		return AcalDateTime.fromAcalProperty(aProp);
	}

	public void setCompleted( AcalDateTime newValue ) {
		setUniqueProperty(newValue.asProperty(PropertyName.COMPLETED));
	}

	public int getPercentComplete() {
		AcalProperty aProp = getProperty(PropertyName.PERCENT_COMPLETE);
		if ( aProp == null || aProp.getValue() == null ) return 0;
		return Integer.parseInt(aProp.getValue());
	}

	public void setPercentComplete( int newValue ) {
		setUniqueProperty(new AcalProperty(PropertyName.PERCENT_COMPLETE, Integer.toString(newValue)));
	}

	public void setStatus(Status newValue) {
		setUniqueProperty(new AcalProperty(PropertyName.STATUS, newValue.toString()));
	}

	public  VTodo(TodoInstance todo) {
		super(VComponent.VTODO,todo);

		if ( todo.getDue() != null ) setEnd(todo.getDue());
		if ( todo.getCompleted() != null ) setCompleted(todo.getCompleted());
	}

}
