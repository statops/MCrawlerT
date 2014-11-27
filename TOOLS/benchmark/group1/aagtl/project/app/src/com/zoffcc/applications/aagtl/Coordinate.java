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

public class Coordinate
{
	int RADIUS_EARTH = 6371000;
	int FORMAT_D = 0;
	int FORMAT_DM = 1;

	public double lat;
	public double lon;

	public static class coords_d_m_m
	{
		public boolean lat_plus_minus = true; // true = +, false = -
		public String lat_sign = "+";
		public int lat_deg = 0;
		public int lat_min = 0;
		public int lat_min_fractions = 0;

		public boolean lon_plus_minus = true; // true = +, false = -
		public String lon_sign = "+";
		public int lon_deg = 0;
		public int lon_min = 0;
		public int lon_min_fractions = 0;
	}

	public Coordinate(double lat1, double lon1)
	{
		this.lat = lat1;
		this.lon = lon1;
	}

	public static Coordinate dm_m_to_d(coords_d_m_m in)
	{
		Coordinate ret = new Coordinate(0, 0);

		ret.lat = (double) in.lat_deg + ((double) in.lat_min / (double) 60) + (((double) in.lat_min_fractions / (double) 60) / (double) 1000);
		if (!in.lat_plus_minus)
		{
			ret.lat = -ret.lat;
		}

		//System.out.println("lat=" + ret.lat);
		//System.out.println("lat_d_m_m=" + in.lat_sign + "" + in.lat_deg + "° " + in.lat_min + "." + in.lat_min_fractions);

		ret.lon = (double) in.lon_deg + ((double) in.lon_min / (double) 60) + (((double) in.lon_min_fractions / (double) 60) / (double) 1000);
		if (!in.lon_plus_minus)
		{
			ret.lon = -ret.lon;
		}

		//System.out.println("lon=" + ret.lon);
		//System.out.println("lon_d_m_m=" + in.lon_sign + "" + in.lon_deg + "° " + in.lon_min + "." + in.lon_min_fractions);
		return ret;
	}

	public static Coordinate parse_coord_string(String in)
	{
		Coordinate c_ = new Coordinate(0, 0);

		try
		{
			coords_d_m_m t = new coords_d_m_m();
			//  X lat        X lon
			//  N 48° 12.134 E 016° 21.629
			String lat = in.split("[EWew]", 2)[0].trim();
			t.lat_plus_minus = letter_to_sign(lat.substring(0, 1));
			lat = lat.substring(1).trim();
			t.lat_deg = Integer.parseInt(lat.split("°", 2)[0]);
			t.lat_min = Integer.parseInt(lat.split("°", 2)[1].split("\\.", 2)[0].trim());
			t.lat_min_fractions = Integer.parseInt(lat.split("°", 2)[1].split("\\.", 2)[1].trim());

			String lon = in.split("[EWew]", 2)[1].trim();
			if ((in.contains("E")) || (in.contains("e")))
			{
				t.lon_plus_minus = letter_to_sign("E");
			}
			else
			{
				t.lon_plus_minus = letter_to_sign("W");
			}
			lon = lon.substring(1).trim();
			t.lon_deg = Integer.parseInt(lon.split("°", 2)[0]);
			t.lon_min = Integer.parseInt(lon.split("°", 2)[1].split("\\.", 2)[0].trim());
			t.lon_min_fractions = Integer.parseInt(lon.split("°", 2)[1].split("\\.", 2)[1].trim());

			c_ = Coordinate.dm_m_to_d(t);
		}
		catch (Exception e)
		{
			// some parse error
			e.printStackTrace();
			return null;
		}

		return c_;
	}

	public static Boolean letter_to_sign(String letter)
	{
		if (letter.equalsIgnoreCase("n"))
		{
			// N = +
			return true;
		}
		else if (letter.equalsIgnoreCase("s"))
		{
			// S = -
			return false;
		}
		else if (letter.equalsIgnoreCase("e"))
		{
			// E = +
			return true;
		}
		//else if (letter.equalsIgnoreCase("w"))
		//{
		//	// W = -
		//}
		// W = -
		return false;
	}

	public coords_d_m_m d_to_dm_m()
	{
		coords_d_m_m ret = new coords_d_m_m();

		ret.lat_deg = (int) Math.abs(this.lat); // before the "."
		ret.lat_min = (int) (((double) Math.abs(this.lat) - (double) ret.lat_deg) * (double) 60);
		ret.lat_min_fractions = (int) (((((double) Math.abs(this.lat) - (double) ret.lat_deg) * (double) 60) - (double) ret.lat_min) * 1000);
		if (this.lat >= 0)
		{
			ret.lat_sign = "+";
			ret.lat_plus_minus = true;
		}
		else
		{
			ret.lat_sign = "-";
			ret.lat_plus_minus = false;
		}
		//System.out.println("lat=" + this.lat);
		//System.out.println("lat_d_m_m=" + ret.lat_sign + "" + ret.lat_deg + "° " + ret.lat_min + "." + ret.lat_min_fractions);

		ret.lon_deg = (int) Math.abs(this.lon); // before the "."
		ret.lon_min = (int) (((double) Math.abs(this.lon) - (double) ret.lon_deg) * (double) 60);
		ret.lon_min_fractions = (int) (((((double) Math.abs(this.lon) - (double) ret.lon_deg) * (double) 60) - (double) ret.lon_min) * 1000);
		if (this.lon >= 0)
		{
			ret.lon_sign = "+";
			ret.lon_plus_minus = true;
		}
		else
		{
			ret.lon_sign = "-";
			ret.lon_plus_minus = false;
		}
		//System.out.println("lon=" + this.lon);
		//System.out.println("lon_d_m_m=" + ret.lon_sign + "" + ret.lon_deg + "° " + ret.lon_min + "." + ret.lon_min_fractions);

		return ret;
	}

	public double distance_to(Coordinate target)
	{
		double dlat = Math.pow(Math.sin(Math.toRadians(target.lat - this.lat) / 2), 2);
		double dlon = Math.pow(Math.sin(Math.toRadians(target.lon - this.lon) / 2), 2);
		double a = dlat + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(target.lat)) * dlon;
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return this.RADIUS_EARTH * c;

		/*
		 * 
		 * 
		 * def distance_to_manual (src, target):
		 * dlat = math.pow(math.sin(math.radians(target.lat-src.lat) / 2), 2)
		 * dlon = math.pow(math.sin(math.radians(target.lon-src.lon) / 2), 2)
		 * a = dlat + math.cos(math.radians(src.lat)) * math.cos(math.radians(target.lat)) * dlon;
		 * c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a));
		 * return Coordinate.RADIUS_EARTH * c;
		 * distance_to = distance_to_manual
		 */

	}

	public double bearing_to(Coordinate target)
	{
		double lat1 = Math.toRadians(this.lat);
		double lat2 = Math.toRadians(target.lat);

		double dlon = Math.toRadians(target.lon - this.lon);
		double y = Math.sin(dlon) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dlon);
		double bearing = Math.toDegrees(Math.atan2(y, x));

		return (360 + bearing) % 360;
	}
}
