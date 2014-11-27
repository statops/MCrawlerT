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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.htmlparser.jericho.Source;
import android.os.Bundle;
import android.os.Message;
import android.widget.Toast;

public class CacheDownloader
{
	aagtl main_aagtl;
	GeocacheCoordinate current_cache = null;
	HTMLDownloader downloader = null;

	// to debug page parsing
	// to debug page parsing
	static final Boolean DEBUG_ = false;
	static final Boolean DEBUG2_ = false;

	// to debug page parsing
	// to debug page parsing

	public CacheDownloader(aagtl main_aagtl, HTMLDownloader dol)
	{
		this.main_aagtl = main_aagtl;
		this.downloader = dol;
	}

	public GeocacheCoordinate update_coordinate(GeocacheCoordinate coordinate)
	{
		GeocacheCoordinate gc_ret = coordinate;
		current_cache = gc_ret;

		// download from internet
		String data = this.__get_cache_page(coordinate.name);

		if (data == null)
		{
			// we got "null" return value!!
			return null;
		}

		// System.out.println(data);

		// split output into lines
		String[] response_lines = data.split("\n");
		// parse the HTML lines
		GeocacheCoordinate temp_gc = this.__parse_cache_page(response_lines, coordinate);

		if (temp_gc != null)
		{
			// write full data to DB
			this.main_aagtl.pv.begin_trans();
			try
			{
				this.main_aagtl.pv.add_point_full(gc_ret);
				this.main_aagtl.pv.commit();
			}
			finally
			{
				this.main_aagtl.pv.end_trans();
			}
		}
		else
		{
			// we got "null" return value!!
			return null;
		}

		return gc_ret;
	}

	public String __get_cache_page(String cacheid)
	{
		return this.downloader.getUrlData(String.format("http://www.geocaching.com/seek/cache_details.aspx?wp=%s", cacheid));
	}

	public static GeocacheCoordinate __parse_cache_page_print(String cache_page, GeocacheCoordinate coordinate)
	{
		try
		{
			Source source = new Source(cache_page);
			String title = source.getFirstElement("id", "pageTitle", false).getTextExtractor().toString();
			title = title.replace("(" + coordinate.name + ") ", "");
			//System.out.println("title=" + title);
			coordinate.title = title;
			//
			String lat_lon = source.getFirstElement("class", "LatLong Meta", false).getTextExtractor().toString();
			//System.out.println("lat_lon=" + lat_lon);
			Coordinate c_ = Coordinate.parse_coord_string(lat_lon);
			if (c_ == null)
			{
				return null;
			}
			else
			{
				coordinate.lat = c_.lat;
				coordinate.lon = c_.lon;
				// System.out.println("lat=" + c_.lat + " lon=" + c_.lon);
			}

			String type = source.getFirstElement("id", "Content", false).getFirstElement("h2").getFirstElement("img").getAttributeValue("src").toString();
			coordinate.type = GeocacheCoordinate.GC_TYPE_HASH.get(type.split("/")[3].split("\\.")[0]);
			//System.out.println("type=" + coordinate.type);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return coordinate;
	}

	public GeocacheCoordinate __parse_cache_page(String[] cache_page, GeocacheCoordinate coordinate)
	{
		String section = "";
		String section2 = "";
		String section_old = "XXXXXX";
		String shortdesc = "";
		String desc = "";
		String hints = "";
		String waypoints = "";
		String images = "";
		String logs = "";
		String head = "";
		Boolean found = false;

		String line = "";
		if (DEBUG_) System.out.println("lines=" + cache_page.length);
		for (int i = 0; i < cache_page.length; i++)
		{
			line = cache_page[i];
			// remove spaces at start and end of string
			line = line.trim();
			// line = line.replace("\r", "");
			// line = line.replace("\n", "");

			// if (DEBUG_) System.out.println("line=" + line);

			if (section.compareTo(section_old) != 0)
			{
				if (DEBUG_) System.out.println("##sec##=" + section);
				section_old = section;
			}

			if ((section.equals("")) && (line.startsWith("<div class=\"span-17 last")))
			{
				section = "head";
			}
			else if ((section.equals("head")) && (line.startsWith("<span id=\"ctl00_ContentBody_ShortDescription\">")))
			{
				section = "shortdesc";
			}
			else if (((section.equals("head")) || (section.equals("shortdesc"))) && (line.startsWith("<span id=\"ctl00_ContentBody_LongDescription\">")))
			{
				section = "desc";

			}
			else if (((section.equals("desc")) || (section.equals("shortdesc"))) && (line.startsWith("Additional Hints")))
			{
				section = "after-desc";
			}
			else if ((section.equals("after-desc")) && (line.contains("<div id=\"div_hint\"")))
			{
				section = "hints";
			}
			else if (line.startsWith("<div id=\"ctl00_ContentBody_uxStatusInformation\""))
			{
				section2 = "status-found-1";
			}
			else if ((section2.equals("status-found-1")) && (line.contains("id=\"ctl00_ContentBody_hlFoundItLog\"")))
			{
				section2 = "status-found-2";
				// we have found this cache already
				if (DEBUG_) System.out.println("-- found it --");
				found = true;
				section2 = "";
			}
			else if ((section.equals("hints")) && (line.startsWith("<div id='dk'")))
			{
				section = "after-hints";
			}
			else if (((section.equals("after-hints")) || (section.equals("after-desc"))) && (line.startsWith("<div class=\"CacheDetailNavigationWidget\">")))
			{
				section = "pre-waypoints";

			}
			else if (((section.equals("after-hints")) || (section.equals("pre-waypoints"))) && (line.startsWith("<table class=\"Table\" id=\"ctl00_ContentBody_Waypoints\">")))
			{
				section = "waypoints";

			}
			else if ((section.equals("waypoints")) && (line.contains("</tbody> </table>")))
			{
				section = "after-waypoints";

			}
			else if (((section.equals("pre-waypoints")) || (section.equals("after-waypoints"))) && (line.startsWith("<span id=\"ctl00_ContentBody_Images\">")))
			{
				section = "images";

			}
			else if (((section.equals("images")) || (section.equals("after-waypoints")) || (section.equals("pre-waypoints"))) && (line.contains("initalLogs = {")))
			{
				section = "logs";
				logs = line;
				if (DEBUG_) System.out.println("logline=" + logs);
			}

			if (section.equals("logs"))
			{
				// finish
				// the
				// for-loop
				//
				break;
			}

			if (section.equals("head"))
			{
				head = String.format("%s%s\n", head, line);
			}
			else if (section.equals("shortdesc"))
			{
				shortdesc = String.format("%s%s\n", shortdesc, line);
			}
			else if (section.equals("desc"))
			{
				desc = String.format("%s%s\n", desc, line);
			}
			else if (section.equals("hints"))
			{
				hints = String.format("%s%s\n", hints, line);
			}
			else if (section.equals("waypoints"))
			{
				waypoints = String.format("%s%s\n", waypoints, line);
			}
			else if (section.equals("images"))
			{
				images = String.format("%s%s\n", images, line);
			}
			//else if (section.equals("logs"))
			//{
			//	logs = logs + line;
			//}
		}

		String[] temp_str = new String[7];
		temp_str = this.__parse_head(head);
		coordinate.size = Integer.parseInt(temp_str[0]);
		coordinate.difficulty = Integer.parseInt(temp_str[1]);
		coordinate.terrain = Integer.parseInt(temp_str[2]);
		coordinate.owner = temp_str[3];
		coordinate.lat = Double.parseDouble(temp_str[4]);
		coordinate.lon = Double.parseDouble(temp_str[5]);
		coordinate.type = temp_str[6];
		coordinate.shortdesc = shortdesc;
		coordinate.desc = desc;
		coordinate.hints = this.__treat_hints(hints);
		coordinate.logs = this.__treat_logs(logs);
		coordinate.guid = this.__get_guid(head);
		coordinate.found = found;
		coordinate.waypoints = this.__treat_waypoints(waypoints);

		// seems we got bogus data!! -> return "null"
		if ((coordinate.guid == null) && (coordinate.owner.compareTo("dummy") == 0) && (coordinate.lat == 0.0))
		{
			return null;
		}

		// coordinate.shortdesc = self.__treat_shortdesc(shortdesc)
		// coordinate.desc = self.__treat_desc(desc)
		// coordinate.hints = self.__treat_hints(hints)
		// coordinate.set_waypoints(self.__treat_waypoints(waypoints))
		// coordinate.set_logs(self.__treat_logs(logs))
		// self.__treat_images(images)
		// coordinate.set_images(self.images)

		return coordinate;
	}

	public String _strip_html(String input)
	{
		// **old** return input.replaceAll("\\<[^>]*>", "");
		return HtmlToText.htmlToPlainText(input);
	}

	public String __treat_waypoints(String input)
	{
		String ret = input;
		if (DEBUG_) System.out.println("way1=" + ret);
		ret = ret.replaceAll("<tr>", "<br><tr>");
		ret = ret.replaceAll("<tr ", "<br><tr ");
		ret = HtmlToText.htmlToPlainText(ret);
		ret = ret.replaceAll("\n", "<br><br>");
		if (DEBUG_) System.out.println("way2=" + ret);

		// // OLD
		// ret = ret.replaceAll("<td>", "\t");
		// ret = ret.replaceAll("<tr>", "#~RETURN#~");
		// // remove all other HTML tags
		// ret = ret.replaceAll("<[^>]*>", "");
		// // make lots of spaces to only 1 space
		// ret = ret.replaceAll("[ \t]+", " ");
		// ret = ret.replaceAll("#~RETURN#~", "<br>");
		// // OLD
		return ret;
	}

	public String __treat_hints(String input)
	{
		String ret = input;
		ret = this._strip_html(ret);
		ret = ret + "\n<br>\n<br>" + this.convert_rot13(ret);
		return ret;
	}

	public String __treat_logs(String input)
	{
		String ret = input;
		// <img src="http://www.geocaching.com/images/icons/icon_smile.gif"
		// alt="Found it" title="Found it" />
		// <img src="http://www.geocaching.com/images/icons/icon_sad.gif"
		// alt="Didn't find it" title="Didn't find it" />

		// replace found,not found images
		ret = input.replaceAll("<img src=\"[^>]*icon_smile.gif[^>]*>", "\n<br><br>* FOUND *\n\n<br><br>");
		ret = ret.replaceAll("<img src=\"[^>]*icon_sad.gif[^>]*>", "\n<br><br># NOT FOUND #\n\n<br><br>");

		// if (DEBUG_) System.out.println("log1=" + ret);
		ret = HtmlToText.htmlToPlainText(ret);
		ret = ret.replaceAll("\n", "<br>");
		// if (DEBUG_) System.out.println("log2=" + ret);
		return ret;
	}

	public String convert_rot13(String in)
	{
		StringBuffer tempReturn = new StringBuffer();
		int abyte = 0;

		for (int i = 0; i < in.length(); i++)
		{
			abyte = in.charAt(i);
			int cap = abyte & 32;
			abyte &= ~cap;
			abyte = ((abyte >= 'A') && (abyte <= 'Z') ? ((abyte - 'A' + 13) % 26 + 'A') : abyte) | cap;
			tempReturn.append((char) abyte);
		}
		return tempReturn.toString();
	}

	public String __get_guid(String in)
	{
		String guid = "";
		Pattern p = Pattern.compile(".*guid=([a-z0-9-]+)\"");
		Matcher m = p.matcher(in);
		Boolean has_found = m.find();

		if (!has_found)
		{
			return null;
		}

		if (m.groupCount() > 0)
		{
			guid = m.group(1);
		}

		return guid;
	}

	public String[] __parse_head(String in)
	{
		String[] ret = new String[7];

		// System.out.println(""+in);

		String sizestring = "";

		// <img src="/images/icons/container/micro.gif" alt="Size: Micro"
		Pattern p = Pattern.compile("<img src=\"/images/icons/container/([^\\.]+)\\.gif\" alt=\"Size:");
		Matcher m = p.matcher(in);
		Boolean has_found = m.find();

		String size = "5";
		String lat = "0.0";
		String lon = "0.0";
		String diff = "1";
		String terr = "1";
		String type = GeocacheCoordinate.TYPE_REGULAR;
		String owner = "dummy";

		if (!has_found)
		{
			ret[0] = size;
			ret[1] = diff;
			ret[2] = terr;
			ret[3] = owner;
			ret[4] = lat;
			ret[5] = lon;

			System.out.println("__parse_head: problem -> no data!");
			return ret;
		}

		if (m.groupCount() > 0)
		{
			sizestring = m.group(1);

			if (sizestring.compareTo("micro") == 0)
			{
				size = "1";
			}
			else if (sizestring.compareTo("small") == 0)
			{
				size = "2";
			}
			else if (sizestring.compareTo("regular") == 0)
			{
				size = "3";
			}
			else if ((sizestring.compareTo("large") == 0) || (sizestring.compareTo("big") == 0))
			{
				size = "4";
			}
			else if ((sizestring.compareTo("not_chosen") == 0) || (sizestring.compareTo("other") == 0))
			{
				size = "5";
			}
			if (DEBUG_) System.out.println("size=" + size);
		}

		// <img src="/images/WptTypes/2.gif" alt="Traditional Cache"
		p = Pattern.compile("<img src=\"/images/WptTypes/.*\\.gif\" alt=\"([^\"]*)\"");
		m = p.matcher(in);
		has_found = m.find();
		if (has_found)
		{
			if (m.groupCount() > 0)
			{
				if (m.group(1).compareTo("Traditional Cache") == 0)
				{
					type = GeocacheCoordinate.TYPE_REGULAR;
				}
				else if (m.group(1).compareTo("Multi-cache") == 0)
				{
					type = GeocacheCoordinate.TYPE_MULTI;
				}
				else if (m.group(1).compareTo("Unknown Cache") == 0)
				{
					type = GeocacheCoordinate.TYPE_MYSTERY;
				}
				else
				{
					type = GeocacheCoordinate.TYPE_UNKNOWN;
				}
				if (DEBUG_) System.out.println("type=" + type);
			}
		}

		p = Pattern.compile("(?s)uxLegendScale\"[^>]*?><img src=\"http://www.geocaching.com/images/stars/stars[0-9_]+\\.gif\" alt=\"([0-9.]+) out");
		m = p.matcher(in);
		has_found = m.find();
		if (has_found)
		{
			if (m.groupCount() > 0)
			{
				diff = String.format("%.0f", Double.parseDouble(m.group(1)) * 10);
				if (DEBUG_) System.out.println("diff=" + diff);
			}
		}

		p = Pattern.compile("(?s)ContentBody_Localize6\"[^>]*?><img src=\"http://www.geocaching.com/images/stars/stars[0-9_]+\\.gif\" alt=\"([0-9.]+) out");
		m = p.matcher(in);
		has_found = m.find();
		if (has_found)
		{
			if (m.groupCount() > 0)
			{
				terr = String.format("%.0f", Double.parseDouble(m.group(1)) * 10);
				if (DEBUG_) System.out.println("terr=" + terr);
			}
		}

		// owner =
		// HTMLManipulations._decode_htmlentities(re.compile("\\sby <[^>]+>([^<]+)</a>",
		// re.MULTILINE).search(head).group(1))

		Pattern p2 = Pattern.compile("lat=([0-9.-]+)&amp;lon=([0-9.-]+)&amp;");
		Matcher m2 = p2.matcher(in);
		Boolean has_found2 = m2.find();

		if (!has_found2)
		{
			lat = "0.0";
			lon = "0.0";

			ret[0] = size;
			ret[1] = diff;
			ret[2] = terr;
			ret[3] = owner;
			ret[4] = lat;
			ret[5] = lon;

			Message msg = this.main_aagtl.toast_handler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("command", 1);
			b.putInt("duration", Toast.LENGTH_LONG);
			// b.putString("text", "__parse_head: problem parsing lat,lon -> " +
			// in);
			b.putString("text", "problem downloading cache\n\nplease check username/password!!\n\nor maybe network error");
			msg.setData(b);
			this.main_aagtl.toast_handler.sendMessage(msg);

			System.out.println("__parse_head: problem parsing lat,lon -> 0.0 0.0 !");
			System.out.println("" + in);
			return ret;
		}
		else
		{
			if (m2.groupCount() > 1)
			{
				lat = m2.group(1);
				lon = m2.group(2);
			}
		}

		ret[0] = size;
		ret[1] = diff;
		ret[2] = terr;
		ret[3] = owner;
		ret[4] = lat;
		ret[5] = lon;
		ret[6] = type;

		if (DEBUG_) System.out.println("lat=" + lat);
		if (DEBUG_) System.out.println("lon=" + lon);
		// System.out.println("size=" + size);
		// System.out.println("lat=" + lat);
		// System.out.println("lon=" + lon);

		return ret;
	}

}
