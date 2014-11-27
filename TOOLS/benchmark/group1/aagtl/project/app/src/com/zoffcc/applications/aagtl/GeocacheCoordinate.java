/**
 * aagtl Advanced Geocaching Tool for Android
 * loosely based on agtl by Daniel Fett <fett@danielfett.de>
 * Copyright (C) 2010 - 2012 Zoff <aagtl@work.zoff.cc>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.aagtl;

import java.io.Serializable;
import java.util.HashMap;

public class GeocacheCoordinate extends Coordinate implements Serializable
{
	private static final long serialVersionUID = 9038943407829519828L;

	public final static int LOG_NO_LOG = 0;
	public final static int LOG_AS_FOUND = 1;
	public final static int LOG_AS_NOTFOUND = 2;
	public final static int LOG_AS_NOTE = 3;

	public final static HashMap<Integer, String> LOG_AS_HASH = new HashMap<Integer, String>()
	{
		/**
																								 * 
																								 */
		private static final long serialVersionUID = -7496572942942411474L;

		{
			put(LOG_NO_LOG, "XXXX");
			put(LOG_AS_FOUND, "Found it");
			put(LOG_AS_NOTFOUND, "Didn't find it");
			put(LOG_AS_NOTE, "Write note");
		}
	};

	public final static String TYPE_REGULAR = "regular";
	public final static String TYPE_MULTI = "multi";
	public final static String TYPE_VIRTUAL = "virtual";
	public final static String TYPE_EVENT = "event";
	public final static String TYPE_MYSTERY = "mystery";
	public final static String TYPE_WEBCAM = "webcam";
	public final static String TYPE_EARTH = "earth";
	public final static String TYPE_UNKNOWN = "unknown";

	public final static HashMap<String, String> GC_TYPE_HASH = new HashMap<String, String>()
	{
		/**
																								 * 
																								 */
		private static final long serialVersionUID = 1820707086245641420L;

		{
			put("2", TYPE_REGULAR);
			put("3", TYPE_MULTI);
			put("4", TYPE_VIRTUAL);
			put("6", TYPE_EVENT);
			put("8", TYPE_MYSTERY);
			put("11", TYPE_WEBCAM);
			put("137", TYPE_EARTH);
		}
	};

	//	public final static String[]						TYPES						= {TYPE_REGULAR, TYPE_MULTI,
	//			TYPE_VIRTUAL, TYPE_EVENT, TYPE_MYSTERY, TYPE_WEBCAM, TYPE_EARTH, TYPE_UNKNOWN};

	public final static int STATUS_NORMAL = 0;
	public final static int STATUS_DISABLED = 1;

	public final static HashMap<Integer, String> STATUS_HASH = new HashMap<Integer, String>()
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 7496571242948713474L;

		{
			put(STATUS_NORMAL, "normal");
			put(STATUS_DISABLED, "disabled");
		}
	};

	public final static int AAGTL_STATUS_NORMAL = 0;
	public final static int AAGTL_STATUS_FOUND = 1;

	public final static String LOG_TYPE_FOUND = "smile";
	public final static String LOG_TYPE_NOTFOUND = "sad";
	public final static String LOG_TYPE_NOTE = "note";
	public final static String LOG_TYPE_MAINTENANCE = "maint";

	double lat;
	double lon;
	String name;
	String title;
	String desc;
	String shortdesc;
	String hints;
	String type;
	int size;
	int difficulty;
	int terrain;
	String owner;
	Boolean found;
	String waypoints;
	String images;
	String notes;
	String fieldnotes;
	int log_as;
	String log_date;
	Boolean marked;
	String logs;
	String guid;
	int status;
	int aagtl_status;

	public GeocacheCoordinate(double lat, double lon, String name)
	{
		super(lat, lon);

		this.lat = lat;
		this.lon = lon;

		// this.name --> GC-CODE !!!
		this.name = name;

		// this.title --> cache name !!!
		this.title = "";

		this.shortdesc = "";
		this.desc = "";
		this.hints = "";
		this.type = "";
		this.size = -1;
		this.difficulty = -1;
		this.terrain = -1;
		this.owner = "";
		this.found = false;
		this.waypoints = "";
		this.images = "";
		this.notes = "";
		this.fieldnotes = "";
		this.log_as = LOG_NO_LOG;
		this.log_date = "";
		this.marked = false;
		this.logs = "";
		this.guid = "";
		this.status = STATUS_NORMAL;
		this.aagtl_status = AAGTL_STATUS_NORMAL;

	}
}
