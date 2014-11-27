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

import java.util.ArrayList;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;



public class VTimezone extends VComponent {

	public static final String TAG = "aCal VTimezone";
	protected final String name = "VTimezone";

	protected TimeZone tz = null;
	protected String tzid = null;
	
	public VTimezone(ComponentParts splitter, VComponent parent) {
		super(splitter, parent);
	}

	public String getTZID() {
		if ( tzid == null ) guessOlsonTimeZone();
		return tzid;
	}

	public TimeZone getTZ() {
		if ( tz == null ) guessOlsonTimeZone();
		return tz;
	}

	private boolean guessOlsonTimeZone() {
		if ( tryTz(getProperty("TZID")) ) return true;
		if ( tryTz(getProperty("TZNAME")) ) return true;
		if ( tryTz(getProperty("X-LIC-LOCATION")) ) return true;
		if ( tryTz(getProperty("X-ENTOURAGE-CFTIMEZONE")) ) return true;
		tzid = getOlsonFromMsID();
		if ( tzid != null ) {
			tz = TimeZone.getTimeZone(tzid);
			return true;
		}
		
		String[] matchingZones = getMatchingZones();
		if ( matchingZones != null && matchingZones.length == 1 ) {
			tzid = matchingZones[0];
			tz = TimeZone.getTimeZone(tzid); 
			return true;
		}
		return false;
	}

	private boolean tryTz(AcalProperty testProperty) {
		if ( testProperty != null ) {
			tzid = testProperty.getValue(); 
			if ( tzid != null ) {
				tz = TimeZone.getTimeZone(tzid);
				if ( tz != null ) {
					tzid = tz.getID();
					return true;
				}
				tzid = VCalendar.staticGetOlsonName(tzid);
				if ( tzid != null ) {
					tz = TimeZone.getTimeZone(tzid);
					if ( tz != null ) {
						tzid = tz.getID();
						return true;
					}
					tzid = null;
				}
			}
		}
		return false;
	}


	private String getOlsonFromMsID() {
		AcalProperty idProperty = getProperty("X-MICROSOFT-CDO-TZID");
		if ( idProperty != null && idProperty.getValue() != null ) {
			switch( Integer.parseInt(idProperty.getValue()) ) {
				case 0:    return("UTC");
				case 1:    return("Europe/London");
				case 2:    return("Europe/Lisbon");
				case 3:    return("Europe/Paris");
				case 4:    return("Europe/Berlin");
				case 5:    return("Europe/Bucharest");
				case 6:    return("Europe/Prague");
				case 7:    return("Europe/Athens");
				case 8:    return("America/Sao_Paulo");
				case 9:    return("America/Halifax");
				case 10:   return("America/New_York");
				case 11:   return("America/Chicago");
				case 12:   return("America/Denver");
				case 13:   return("America/Los_Angeles");
				case 14:   return("America/Anchorage");
				case 15:   return("Pacific/Honolulu");
				case 16:   return("Pacific/Apia");
				case 17:   return("Pacific/Auckland");
				case 18:   return("Australia/Brisbane");
				case 19:   return("Australia/Adelaide");
				case 20:   return("Asia/Tokyo");
				case 21:   return("Asia/Singapore");
				case 22:   return("Asia/Bangkok");
				case 23:   return("Asia/Kolkata");
				case 24:   return("Asia/Muscat");
				case 25:   return("Asia/Tehran");
				case 26:   return("Asia/Baghdad");
				case 27:   return("Asia/Jerusalem");
				case 28:   return("America/St_Johns");
				case 29:   return("Atlantic/Azores");
				case 30:   return("America/Noronha");
				case 31:   return("Africa/Casablanca");
				case 32:   return("America/Argentina/Buenos_Aires");
				case 33:   return("America/La_Paz");
				case 34:   return("America/Indiana/Indianapolis");
				case 35:   return("America/Bogota");
				case 36:   return("America/Regina");
				case 37:   return("America/Tegucigalpa");
				case 38:   return("America/Phoenix");
				case 39:   return("Pacific/Kwajalein");
				case 40:   return("Pacific/Fiji");
				case 41:   return("Asia/Magadan");
				case 42:   return("Australia/Hobart");
				case 43:   return("Pacific/Guam");
				case 44:   return("Australia/Darwin");
				case 45:   return("Asia/Shanghai");
				case 46:   return("Asia/Novosibirsk");
				case 47:   return("Asia/Karachi");
				case 48:   return("Asia/Kabul");
				case 49:   return("Africa/Cairo");
				case 50:   return("Africa/Harare");
				case 51:   return("Europe/Moscow");
				case 53:   return("Atlantic/Cape_Verde");
				case 54:   return("Asia/Yerevan");
				case 55:   return("America/Panama");
				case 56:   return("Africa/Nairobi");
				case 58:   return("Asia/Yekaterinburg");
				case 59:   return("Europe/Helsinki");
				case 60:   return("America/Godthab");
				case 61:   return("Asia/Rangoon");
				case 62:   return("Asia/Kathmandu");
				case 63:   return("Asia/Irkutsk");
				case 64:   return("Asia/Krasnoyarsk");
				case 65:   return("America/Santiago");
				case 66:   return("Asia/Colombo");
				case 67:   return("Pacific/Tongatapu");
				case 68:   return("Asia/Vladivostok");
				case 69:   return("Africa/Ndjamena");
				case 70:   return("Asia/Yakutsk");
				case 71:   return("Asia/Dhaka");
				case 72:   return("Asia/Seoul");
				case 73:   return("Australia/Perth");
				case 74:   return("Asia/Riyadh");
				case 75:   return("Asia/Taipei");
				case 76:   return("Australia/Sydney");
				
				case 57: // null
				case 52: // null
				default: // null
			}
		}
		return null;
	}

	private String[] getMatchingZones() {
		List<VComponent> subComponents = this.getChildren();
		List<Integer> offsets = new ArrayList<Integer>();
		List<String> onsets = new ArrayList<String>();
		List<Boolean> types = new ArrayList<Boolean>();
		int offset;
		boolean isDaylight;
		int dstMillis = 2000000000;
		int stdMillis = 2000000000; // An out of range value that will match nothing
		for( VComponent child : subComponents ) {
			isDaylight = child.getName().equalsIgnoreCase("daylight"); 
			types.add(isDaylight);
			onsets.add(child.getProperty("RRULE").getValue());
			offset = Integer.parseInt(child.getProperty("TZOFFSETTO").getValue());
			offset = (((int)(offset/100)*3600) + (offset%100)) * 1000;
			offsets.add(offset);
			if ( isDaylight )
				dstMillis = offset;
			else
				stdMillis = offset;
		}
		if ( stdMillis > 864000000 ) return null;

		String[] ids = TimeZone.getAvailableIDs(stdMillis);
		List<String> zoneList = new ArrayList<String>();
		SimpleTimeZone zone = null;
		for (String id : ids) {
			 zone = (SimpleTimeZone) TimeZone.getTimeZone(id);
			 if ( zone.getDSTSavings() == dstMillis ) zoneList.add(zone.getID());
		 }
		
		return (String[]) zoneList.toArray();

	}

	
	/**
	 * Returns an iCalendar VTIMEZONE definition as a string
	 * 
	 * @param timeZoneId The TZID we want to find.
	 * @return The VTIMEZONE component as a string.
	 * @throws UnrecognizedTimeZoneException
	 */
	public static String getZoneDefinition(String timeZoneId) throws UnrecognizedTimeZoneException {
		if ( timeZoneId == null ) throw new UnrecognizedTimeZoneException("null");

		int i=0;
		while( i < ZoneData.zones.length && !ZoneData.zones[i][0].equals(timeZoneId)) {
			i++;
		}

		TimeZone tz = TimeZone.getTimeZone(timeZoneId);
		if ( i == ZoneData.zones.length ) {
			String testTzId = tz.getID();

			//  At this point we rather optimistically hope that perhaps Java recognises it
			i=0;
			while( i < ZoneData.zones.length && !ZoneData.zones[i][0].equals(testTzId)) {
				i++;
			}
			if ( i == ZoneData.zones.length )
				throw new UnrecognizedTimeZoneException(timeZoneId);
		}
		return ZoneData.zones[i][1];
	}

	@Override
	public String getEffectiveType() {
		return this.getTopParent().getEffectiveType();
	}

}
