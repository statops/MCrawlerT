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



public class VAlarm extends VComponent {
	public static final String TAG = "aCal VAlarm";
	
	public VAlarm(ComponentParts splitter, VComponent parent) {
		super(splitter, parent);
	}

	public VAlarm( VComponent parent ) {
		super( VComponent.VALARM, parent );
		setEditable();
	}

	public String toPrettyString() {
		String description = null;
		String action = null;
		String trigger = null;
		String tRelated = null;
		String tValue = null;
		String direction = null;
		
		try {
		  description = getProperty(PropertyName.DESCRIPTION).getValue();
		}
		catch (Exception e) {
		}
		if ( description == null ) description = "";

		try {
		  action = getProperty("ACTION").getValue();
		}
		catch (Exception e) {
		}
		if ( action == null ) action = "";

		try {
		  trigger = getProperty("TRIGGER").getValue();
		}
		catch (Exception e) {
		}
		if ( trigger == null ) trigger = "";

		try {
			  tRelated = getProperty("TRIGGER").getParam("RELATED");
		}
		catch (Exception e) {
		}
		if (tRelated == null) tRelated = "START";
		
		try {
			  tValue = getProperty("TRIGGER").getParam("VALUE");
		}
		catch (Exception e) {
		}
		if ( tValue == null ) tValue = "DURATION";
		
		if ( tValue.equalsIgnoreCase("DURATION") ) {
			if ( trigger.length() > 1 && trigger.substring(0,1).equals("-") )
				direction = "before";
			else
				direction = "after";

			if ( tRelated.equalsIgnoreCase("END") )
				tRelated = "event end time";
			else
				tRelated = "event start time";
		}
		
		return description + ": " + action + " - " + trigger + " " + direction + " " + tRelated;
	}

	@Override
	public String getEffectiveType() {
		return parent.getEffectiveType();
	}
	
}
