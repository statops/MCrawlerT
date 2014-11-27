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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import net.htmlparser.jericho.FormFields;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

import com.byarger.exchangeit.EasySSLSocketFactory;

public class HTMLDownloader
{

	boolean logged_in = false;
	public static CookieStore cookie_jar = null;
	public static int GC_DOWNLOAD_MAX_REC_DEPTH = 6;
	aagtl main_aagtl;
	public static int large_buffer_size = 50 * 1024;
	public static int default_buffer_size = 50 * 1024;

	// patterns from c:geo opensource
	private static final Pattern patternLoggedIn = Pattern.compile("<span class=\"Success\">You are logged in as[^<]*<strong[^>]*>([^<]+)</strong>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern patternLogged2In = Pattern.compile("<strong>\\W*Hello,[^<]*<a[^>]+>([^<]+)</a>[^<]*</strong>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern patternViewstateFieldCount = Pattern.compile("id=\"__VIEWSTATEFIELDCOUNT\"[^(value)]+value=\"(\\d+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern patternViewstates = Pattern.compile("id=\"__VIEWSTATE(\\d*)\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern patternUserToken = Pattern.compile("userToken\\s*=\\s*'([^']+)'");

	// patterns from c:geo opensource

	public HTMLDownloader(aagtl main)
	{
		this.main_aagtl = main;
	}

	class get_geocaches_ret
	{
		int count_p;
		GeocacheCoordinate[] points;
	}

	public String convertStreamToString(InputStream is) throws IOException
	{
		/*
		 * To convert the InputStream to String we use the
		 * Reader.read(char[] buffer) method. We iterate until the
		 * Reader return -1 which means there's no more data to
		 * read. We use the StringWriter class to produce the string.
		 */
		if (is != null)
		{
			Writer writer = new StringWriter();

			char[] buffer = new char[default_buffer_size];
			try
			{
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), large_buffer_size);
				int n;
				while ((n = reader.read(buffer)) != -1)
				{
					writer.write(buffer, 0, n);
				}
			}
			finally
			{
				is.close();
			}
			return writer.toString();
		}
		else
		{
			return null;
		}
	}

	public String get_reader_stream(String url, List<NameValuePair> values, ByteArrayOutputStream data, Boolean need_login)
	{

		if ((need_login) && (!this.logged_in))
		{
			System.out.println("--2--- LOGIN START -----");
			this.logged_in = login();
			System.out.println("--2--- LOGIN END   -----");
		}

		if ((values == null) && (data == null))
		{
			return null;
		}
		else if (data == null)
		{
			String websiteData = null;
			URI uri = null;
			DefaultHttpClient client = new DefaultHttpClient();

			// insert cookies from cookie_jar
			client.setCookieStore(cookie_jar);

			try
			{
				uri = new URI(url);
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
				return null;
			}

			HttpPost method = new HttpPost(uri);
			method.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)");
			method.addHeader("Pragma", "no-cache");
			method.addHeader("Content-Type", "application/x-www-form-urlencoded");

			HttpEntity entity = null;
			try
			{
				entity = new UrlEncodedFormEntity(values, HTTP.UTF_8);
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
				return null;
			}

			method.addHeader(entity.getContentType());
			method.setEntity(entity);

			try
			{
				HttpResponse res2 = client.execute(method);
				// System.out.println("login response ->" +
				// String.valueOf(res2.getStatusLine()));
				InputStream data2 = res2.getEntity().getContent();
				websiteData = generateString(data2);

			}
			catch (ClientProtocolException e)
			{
				// e.printStackTrace();
				return null;
			}
			catch (IOException e)
			{
				// e.printStackTrace();
				return null;
			}

			client.getConnectionManager().shutdown();

			return websiteData;

		}
		else
		{
			try
			{
				// url Header textdata
				InputStream i = this.doPost2(url, values, data);
				return this.convertStreamToString(i);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
	}

	private InputStream doPost2(String urlString, List<NameValuePair> values, ByteArrayOutputStream content) throws IOException
	{
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		InputStream in = null;
		OutputStream out;
		byte[] buff;
		con.setRequestMethod("POST");
		for (int j = 0; j < values.size(); j++)
		{
			con.addRequestProperty(values.get(j).getName(), values.get(j).getValue());
		}
		String my_cookies = this.getCookies();
		con.addRequestProperty("Cookie", my_cookies);
		con.setDoOutput(true);
		con.setDoInput(true);
		con.connect();
		out = con.getOutputStream();
		buff = content.toByteArray();
		out.write(buff);
		out.flush();
		out.close();
		in = con.getInputStream();

		return in;
	}

	public String get_user_token()
	{
		String ret = "";
		String url = "http://www.geocaching.com/map/default.aspx?lat=6&lng=9";
		List<NameValuePair> values_list = new ArrayList<NameValuePair>();
		String the_page = get_reader_stream(url, values_list, null, true);

		if (the_page == null)
		{
			if (CacheDownloader.DEBUG_) System.out.println("page = NULL");
			return "";
		}

		String[] response_lines = the_page.split("\n");
		String line = null;
		Pattern p = null;
		Matcher m = null;
		for (int i = 0; i < response_lines.length; i++)
		{
			line = response_lines[i];
			// remove spaces at start and end of string
			line = line.trim();

			if (line.startsWith("var uvtoken"))
			{
				if (CacheDownloader.DEBUG_) System.out.println("usertoken=" + line);
				p = Pattern.compile("userToken[ =]+'([^']+)'");
				m = p.matcher(line);
				m.find();
				if (m.groupCount() > 0)
				{
					if (CacheDownloader.DEBUG_) System.out.println("usertoken parsed=" + m.group(1));
					return m.group(1);
				}
			}
		}

		// + for line in page:
		//
		// + if line.startswith('var uvtoken'):
		//
		// + self.user_token =
		// re.compile("userToken[ =]+'([^']+)'").search(line).group(1)
		//
		// + page.close()
		//
		// + return
		//
		// + raise
		// Exception("Website contents unexpected. Please check connection.")

		return ret;
	}

	public get_geocaches_ret get_geocaches(Coordinate[] location, int count_p, int max_p, int rec_depth, Handler h, int zoom_level)
	{
		return get_geocaches_v3(location, count_p, max_p, rec_depth, h, zoom_level);
	}

	public get_geocaches_ret get_geocaches_v3(Coordinate[] location, int count_p, int max_p, int rec_depth, Handler h, int zoom_level)
	{
		this.main_aagtl.set_bar_slow(h, "get geocaches", "downloading ...", count_p, max_p, true);

		Coordinate c1 = location[0];
		Coordinate c2 = location[1];

		Coordinate center = new Coordinate((c1.lat + c2.lat) / 2, (c1.lon + c2.lon) / 2);
		double dist = (center.distance_to(c1) / 1000) / 2;
		//System.out.println("distance is " + dist + " meters");

		if (dist > 100)
		{
			// dist too large
			count_p = count_p + 1;
			get_geocaches_ret r = new get_geocaches_ret();
			r.count_p = count_p;
			r.points = null;
			return r;
		}

		// use "." as comma seperator!!
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(',');
		DecimalFormat format_lat_lon = new DecimalFormat("#.#####", otherSymbols);
		// use "." as comma seperator!!

		String lat_str = format_lat_lon.format(center.lat);
		String lon_str = format_lat_lon.format(center.lon);
		String dist_str = format_lat_lon.format(dist);
		String url = "http://www.geocaching.com/seek/nearest.aspx?lat=" + lat_str + "&lng=" + lon_str + "&dist=" + dist_str;
		//System.out.println("url=" + url);

		List<NameValuePair> values_list = new ArrayList<NameValuePair>();
		//values_list.add(new BasicNameValuePair("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)"));
		//values_list.add(new BasicNameValuePair("Pragma", "no-cache"));
		// ByteArrayOutputStream bs = new ByteArrayOutputStream();
		String the_page = get_reader_stream(url, values_list, null, true);

		get_geocaches_ret r2 = new get_geocaches_ret();
		r2.count_p = count_p;
		r2.points = null;

		Boolean cont = true;
		Source source = null;

		List<GeocacheCoordinate> gc_list = new ArrayList<GeocacheCoordinate>();
		count_p = 0;
		max_p = 0;
		while (cont)
		{
			source = new Source(the_page);
			List<? extends Segment> segments = (source.getFirstElement("id", "ctl00_ContentBody_ResultsPanel", false).getContent().getFirstElement("class", "PageBuilderWidget", false).getContent().getAllElements("b"));
			if (segments.size() < 3)
			{
				// no results
				return r2;
			}

			int count = Integer.parseInt(segments.get(0).getTextExtractor().toString());
			int page_current = Integer.parseInt(segments.get(1).getTextExtractor().toString());
			int page_max = Integer.parseInt(segments.get(2).getTextExtractor().toString());
			//System.out.println("count=" + count + " cur=" + page_current + " max=" + page_max);
			max_p = count;

			String guid = "";
			String gccode = "";
			Boolean disabled = false;
			List<? extends Segment> segments2 = (source.getFirstElement("class", "SearchResultsTable Table", false).getContent().getAllElements(HTMLElementName.TR));
			// displaySegments(segments2);
			try
			{
				for (Segment s_ : segments2)
				{
					guid = "";
					disabled = false;
					gccode = null;
					try
					{
						List<? extends Segment> segments3 = s_.getAllElements("class", "Merge", false);
						// displaySegments(segments2);
						guid = segments3.get(0).getFirstElement(HTMLElementName.A).getAttributeValue("href");
						guid = guid.split("guid=", 3)[1];
						//System.out.println("guid=:" + guid);

						try
						{
							// <a href="/seek/cache_details.aspx?guid=d9dbf39a-e2e6-4640-b951-d1d6307b16bd" class="lnk  Strike"><span>Cineasten sehen mehr</span></a>
							if (segments3.get(1).getFirstElement(HTMLElementName.A).getAttributeValue("class").equalsIgnoreCase("lnk  Strike"))
							{
								// System.out.println("disabled=:" + disabled);
								disabled = true;
							}
						}
						catch (Exception e3)
						{
						}

						gccode = segments3.get(1).getFirstElement("class", "small", false).getTextExtractor().toString();
						gccode = gccode.split("\\|")[1].trim();
						//System.out.println("gccode=:" + gccode);
					}
					catch (Exception e2)
					{
						e2.printStackTrace();
					}

					if (gccode != null)
					{
						GeocacheCoordinate c__ = null;
						c__ = new GeocacheCoordinate(0, 0, gccode);
						if (disabled)
						{
							c__.status = GeocacheCoordinate.STATUS_DISABLED;
						}

						String url2 = "http://www.geocaching.com/seek/cdpf.aspx?guid=" + guid;
						//System.out.println("url=" + url);

						values_list = new ArrayList<NameValuePair>();
						//values_list.add(new BasicNameValuePair("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)"));
						//values_list.add(new BasicNameValuePair("Pragma", "no-cache"));
						// bs = new ByteArrayOutputStream();
						String the_page2 = get_reader_stream(url2, values_list, null, true);
						c__ = CacheDownloader.__parse_cache_page_print(the_page2, c__);
						if (c__ != null)
						{
							gc_list.add(c__);
							count_p = count_p + 1;
							this.main_aagtl.set_bar_slow(h, "get geocaches", c__.title, count_p, max_p, true);
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			cont = false;

			// ----------- check for paging ----------------
			// ----------- and run through pages -----------
			//
			if (page_current < page_max)
			{
				FormFields formFields = source.getFormFields();
				String vs1 = null;
				try
				{
					vs1 = formFields.getValues("__VIEWSTATE1").get(0);
				}
				catch (Exception e)
				{

				}
				String vs = null;
				try
				{
					vs = formFields.getValues("__VIEWSTATE").get(0);
				}
				catch (Exception e)
				{

				}

				//System.out.println("vs=" + vs);
				//System.out.println("vs1=" + vs1);

				List<NameValuePair> values_list2 = new ArrayList<NameValuePair>();
				values_list2.add(new BasicNameValuePair("__EVENTTARGET", "ctl00$ContentBody$pgrTop$ctl08"));
				values_list2.add(new BasicNameValuePair("__VIEWSTATEFIELDCOUNT", "2"));
				values_list2.add(new BasicNameValuePair("__VIEWSTATE", vs));
				values_list2.add(new BasicNameValuePair("__VIEWSTATE1", vs1));
				// ByteArrayOutputStream bs = new ByteArrayOutputStream();
				the_page = get_reader_stream(url, values_list2, null, true);
				cont = true;
			}
			// ----------- check for paging ----------------
			// ----------- and run through pages -----------
		}

		int jk;
		r2.count_p = gc_list.size();
		r2.points = new GeocacheCoordinate[gc_list.size()];

		for (jk = 0; jk < gc_list.size(); jk++)
		{
			r2.points[jk] = gc_list.get(jk);
		}
		// gc_list.clear();

		return r2;
	}

	private static void displaySegments(List<? extends Segment> segmentsx)
	{
		for (Segment segment1 : segmentsx)
		{
			System.out.println("-------------------------------------------------------------------------------");
			System.out.println(segment1.getDebugInfo());
			System.out.println(segment1);
		}
		System.out.println("\n*******************************************************************************\n");
	}

	public get_geocaches_ret get_geocaches_v2(Coordinate[] location, int count_p, int max_p, int rec_depth, Handler h, int zoom_level)
	{
		this.main_aagtl.set_bar_slow(h, "get geocaches", "downloading ...", count_p, max_p, true);

		if (zoom_level > 16)
		{
			zoom_level = 16;
		}
		else if (zoom_level < 6)
		{
			zoom_level = 6;
		}

		get_geocaches_ret rr2 = new get_geocaches_ret();
		Coordinate c1 = location[0];
		Coordinate c2 = location[1];

		String zoomlevel_ = "" + zoom_level;

		// use "." as comma seperator!!
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.US);
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(',');
		DecimalFormat format_lat_lon = new DecimalFormat("#.#####", otherSymbols);
		// use "." as comma seperator!!

		// http://www.geocaching.com/map/default.aspx?ll=48.22607,16.36997
		// GET http://www.geocaching.com/map/default.aspx?ll=48.22607,16.36997 HTTP/1.1
		String lat_str = format_lat_lon.format((c1.lat + c2.lat) / 2);
		String lon_str = format_lat_lon.format((c1.lon + c2.lon) / 2);
		//String url = "http://www.geocaching.com/map/?ll=" + lat_str + "," + lon_str + "&z=" + zoomlevel_;
		String url = "http://www.geocaching.com/map/default.aspx?ll=" + lat_str + "," + lon_str;
		System.out.println("url=" + url);

		List<NameValuePair> values_list = new ArrayList<NameValuePair>();
		//*values_list.add(new BasicNameValuePair("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)"));
		//*values_list.add(new BasicNameValuePair("Pragma", "no-cache"));
		// values_list.add(new BasicNameValuePair("Referer","http://www.geocaching.com/map/default.aspx"));
		//ByteArrayOutputStream bs = new ByteArrayOutputStream();
		String the_page = get_reader_stream(url, values_list, null, true);
		//String the_page = getUrlData(url);

		//System.out.println(the_page);

		// get values --------------------------
		// get values --------------------------
		// Groundspeak.Map.UserSession('xxxx', 
		Pattern p = Pattern.compile("Groundspeak\\.Map\\.UserSession.'([^']*)'");
		Matcher m = p.matcher(the_page);
		Boolean has_found = true;
		String kk_ = "";
		try
		{
			has_found = m.find();
			kk_ = m.group(1);
		}
		catch (Exception e3)
		{
			e3.printStackTrace();
		}
		System.out.println("kk=" + kk_);

		// sessionToken:'
		p = Pattern.compile("sessionToken:'([^']*)'");
		m = p.matcher(the_page);
		has_found = m.find();
		String sess_token_ = m.group(1);

		System.out.println("sess_token=" + sess_token_);
		// get values --------------------------
		// get values --------------------------

		java.util.Date date = new java.util.Date();
		System.out.println("ts=" + date.getTime());

		String timestamp_ = "" + date.getTime(); // ..seconds..

		// lat, lon -> tile
		Coordinate coord = new Coordinate((c1.lat + c2.lat) / 2, (c1.lon + c2.lon) / 2);
		double[] tile = main_aagtl.rose.deg2num_give_zoom(coord, zoom_level);
		String xtile_num = "" + ((int) tile[0]);
		String ytile_num = "" + ((int) tile[1]);
		// lat, lon -> tile

		// X-Requested-With: XMLHttpRequest
		//Referer: http://www.geocaching.com/map/default.aspx?ll=48.22607,16.36997
		values_list = new ArrayList<NameValuePair>();
		//values_list.add(new BasicNameValuePair("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)"));
		//values_list.add(new BasicNameValuePair("Pragma", "no-cache"));
		//values_list.add(new BasicNameValuePair("X-Requested-With", "XMLHttpRequest"));
		//values_list.add(new BasicNameValuePair("Referer", "http://www.geocaching.com/map/default.aspx?ll=" + lat_str + "," + lon_str));

		String url2 = "http://www.geocaching.com/map/map.info?x=" + xtile_num + "&y=" + ytile_num + "&z=" + zoomlevel_ + "&k=" + kk_ + "&st=" + sess_token_ + "&ep=1&_=" + timestamp_;

		System.out.println("url2=" + url2);

		the_page = get_reader_stream(url2, values_list, null, true);
		// the_page = getUrlData(url2);
		try
		{
			System.out.println(the_page);
		}
		catch (Exception e3)
		{

		}

		// we need to add one, in any case!
		count_p = count_p + 1;
		get_geocaches_ret r = new get_geocaches_ret();
		r.count_p = count_p;
		r.points = null;
		return r;
	}

	public get_geocaches_ret get_geocaches_v1(Coordinate[] location, int count_p, int max_p, int rec_depth, Handler h, int zoom_level)
	{

		if (rec_depth > GC_DOWNLOAD_MAX_REC_DEPTH)
		{
			// we need to add one, in any case!
			count_p = count_p + 1;
			get_geocaches_ret r = new get_geocaches_ret();
			r.count_p = count_p;
			r.points = null;
			return r;
		}

		this.main_aagtl.set_bar_slow(h, "get geocaches", "downloading ...", count_p, max_p, true);

		get_geocaches_ret rr2 = new get_geocaches_ret();

		Coordinate c1 = location[0];
		Coordinate c2 = location[1];

		String url = "http://www.geocaching.com/map/default.aspx/MapAction?lat=9&lng=6";

		String user_token = "";
		user_token = get_user_token();

		List<NameValuePair> values_list = new ArrayList<NameValuePair>();
		DecimalFormat format = new DecimalFormat("#.##########");
		String body_data = String.format("{\"dto\":{\"data\":{\"c\":1,\"m\":\"\",\"d\":\"%s|%s|%s|%s\"},\"ut\":\"%s\"}}", format.format(Math.max(c1.lat, c2.lat)).replace(",", "."), format.format(Math.min(c1.lat, c2.lat)).replace(",", "."), format.format(Math.max(c1.lon, c2.lon)).replace(",", "."), format.format(Math.min(c1.lon, c2.lon)).replace(",", "."), user_token);

		// System.out.println(body_data);

		values_list.add(new BasicNameValuePair("Content-Type", "application/json"));
		values_list.add(new BasicNameValuePair("Content-Length", String.valueOf(body_data.length())));
		//values_list.add(new BasicNameValuePair("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)"));
		//values_list.add(new BasicNameValuePair("Pragma", "no-cache"));
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try
		{
			bs.write(body_data.getBytes());
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		String the_page = get_reader_stream(url, values_list, bs, true);

		Boolean found = false;
		JSONObject a = null;

		try
		{
			a = new JSONObject(the_page);
			// System.out.println(a.get("d"));
			// get "d:" which seems to be only String? whatever, this works
			// anyway
			a = new JSONObject(a.get("d").toString());
			found = true;
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			System.out.println("get_geocaches_ret: JSONException1");

			rr2.count_p = count_p;
			rr2.points = null;
			return rr2;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("get_geocaches_ret: general Exception");
		}

		// string not found -> no caches got from website!
		if (!found)
		{
			rr2.count_p = count_p;
			rr2.points = null;
			return rr2;
		}

		try
		{
			JSONObject _cs = a.getJSONObject("cs");

			Boolean has_cc = false;
			try
			{
				JSONArray _check = _cs.getJSONArray("cc");
				has_cc = true;
			}
			catch (JSONException e)
			{
				// cs not found
				has_cc = false;
			}

			if (!has_cc)
			{
				// 1nd part ----------------------
				// 1nd part ----------------------
				// System.out.println("part 1");

				Boolean has_count_greater_zero = false;
				int count_parsed = 0;
				try
				{
					count_parsed = _cs.getInt("count");
					if (count_parsed > 0)
					{
						// System.out.println("count_parsed: " + count_parsed);
						has_count_greater_zero = true;
					}
				}
				catch (JSONException e)
				{
					// has_count_greater_zero not found
					has_count_greater_zero = false;
				}

				if (has_count_greater_zero)
				{

					double middle_lat = (c1.lat + c2.lat) / 2;
					double middle_lon = (c1.lon + c2.lon) / 2;
					double lat_left_top = Math.min(c1.lat, c2.lat);
					double lon_left_top = Math.min(c1.lon, c2.lon);
					double lat_right_bottom = Math.max(c1.lat, c2.lat);
					double lon_right_bottom = Math.max(c1.lon, c2.lon);

					Coordinate t1 = new Coordinate(lat_left_top, lon_left_top);
					Coordinate t2 = new Coordinate(middle_lat, middle_lon);
					Coordinate[] coord_temp = new Coordinate[2];
					coord_temp[0] = t1;
					coord_temp[1] = t2;
					get_geocaches_ret r_temp = new get_geocaches_ret();
					r_temp = this.get_geocaches(coord_temp, count_p, max_p, rec_depth + 1, h, zoom_level);
					count_p = r_temp.count_p;

					t1 = new Coordinate(middle_lat, lon_left_top);
					t2 = new Coordinate(lat_right_bottom, middle_lon);
					coord_temp = new Coordinate[2];
					coord_temp[0] = t1;
					coord_temp[1] = t2;
					get_geocaches_ret r_temp2 = new get_geocaches_ret();
					r_temp2 = this.get_geocaches(coord_temp, count_p, max_p, rec_depth + 1, h, zoom_level);
					count_p = r_temp2.count_p;

					t1 = new Coordinate(lat_left_top, middle_lon);
					t2 = new Coordinate(middle_lat, lon_right_bottom);
					coord_temp = new Coordinate[2];
					coord_temp[0] = t1;
					coord_temp[1] = t2;
					get_geocaches_ret r_temp3 = new get_geocaches_ret();
					r_temp3 = this.get_geocaches(coord_temp, count_p, max_p, rec_depth + 1, h, zoom_level);
					count_p = r_temp3.count_p;

					t1 = new Coordinate(middle_lat, middle_lon);
					t2 = new Coordinate(lat_right_bottom, lon_right_bottom);
					coord_temp = new Coordinate[2];
					coord_temp[0] = t1;
					coord_temp[1] = t2;
					get_geocaches_ret r_temp4 = new get_geocaches_ret();
					r_temp4 = this.get_geocaches(coord_temp, count_p, max_p, rec_depth + 1, h, zoom_level);
					count_p = r_temp4.count_p;

					int l1 = 0;
					int l2 = 0;
					int l3 = 0;
					int l4 = 0;
					if (r_temp.points != null)
					{
						l1 = r_temp.points.length;
					}
					if (r_temp2.points != null)
					{
						l2 = r_temp2.points.length;
					}
					if (r_temp3.points != null)
					{
						l3 = r_temp3.points.length;
					}
					if (r_temp4.points != null)
					{
						l4 = r_temp4.points.length;
					}
					GeocacheCoordinate[] points2 = new GeocacheCoordinate[l1 + l2 + l3 + l4];
					int j = 0;
					int j_old = 0;
					if (l1 > 0)
					{
						for (j = 0; j < r_temp.points.length; j++)
						{
							points2[j_old] = r_temp.points[j];
							j_old++;
						}
					}
					if (l2 > 0)
					{
						for (j = 0; j < r_temp2.points.length; j++)
						{
							points2[j_old] = r_temp2.points[j];
							j_old++;
						}
					}
					if (l3 > 0)
					{

						for (j = 0; j < r_temp3.points.length; j++)
						{
							points2[j_old] = r_temp3.points[j];
							j_old++;
						}
					}
					if (l4 > 0)
					{

						for (j = 0; j < r_temp4.points.length; j++)
						{
							points2[j_old] = r_temp4.points[j];
							j_old++;
						}
					}

					rr2.count_p = count_p;
					rr2.points = new GeocacheCoordinate[l1 + l2 + l3 + l4];
					rr2.points = points2;
					return rr2;

				}

				// 1nd part ----------------------
				// 1nd part ----------------------

			}
			else
			{
				// 2nd part ----------------------
				// 2nd part ----------------------
				// System.out.println("part 2");

				// make progressbar look better
				count_p = count_p + 3;

				JSONArray _b = _cs.getJSONArray("cc");
				GeocacheCoordinate[] points2 = new GeocacheCoordinate[_b.length()];

				int zw_count = 0;
				int zw_count_draw = 0;
				int zw_max_p_2 = _b.length();
				int zw_draw_every = 1 + (int) ((float) (zw_max_p_2) / (float) (11));

				for (int i = 0; i < _b.length(); i++)
				{
					GeocacheCoordinate c__ = null;
					c__ = new GeocacheCoordinate(_b.getJSONObject(i).getDouble("lat"), _b.getJSONObject(i).getDouble("lon"), _b.getJSONObject(i).getString("gc").toString());

					// System.out.println("11 " +
					// _b.getJSONObject(i).getDouble("lat") + " "
					// + _b.getJSONObject(i).getDouble("lon"));

					// System.out.println("22 " + c__.lat + " " + c__.lon);
					// System.out.println("33 " +
					// _b.getJSONObject(i).toString());

					// title
					c__.title = _b.getJSONObject(i).getString("nn");

					// System.out.println("dd1");

					// type
					String temp = _b.getJSONObject(i).getString("ctid");

					try
					{
						c__.type = GeocacheCoordinate.GC_TYPE_HASH.get(temp);
					}
					catch (Exception e)
					{
						System.out.println("Unknown GC Type!!");
						c__.type = GeocacheCoordinate.TYPE_UNKNOWN;
					}

					// System.out.println("dd2");

					// found
					c__.found = _b.getJSONObject(i).getBoolean("f");

					// System.out.println("" + _b.getJSONObject(i).toString());

					// status
					Boolean temp_b = _b.getJSONObject(i).getBoolean("ia");
					if (temp_b)
					{
						c__.status = GeocacheCoordinate.STATUS_NORMAL;
					}
					else
					{
						c__.status = GeocacheCoordinate.STATUS_DISABLED;
					}

					points2[i] = c__;

					zw_count++;
					zw_count_draw++;
					this.main_aagtl.set_bar_slow(h, "get geocaches", c__.title, zw_count, zw_max_p_2, true);

					if (zw_count_draw >= zw_draw_every)
					{
						zw_count_draw = 0;
					}

				}

				count_p = count_p + 1;
				this.main_aagtl.set_bar_slow(h, "get geocaches", "downloading ...", count_p, max_p, true);

				rr2.count_p = count_p;
				rr2.points = points2;
				return rr2;

				// 2nd part ----------------------
				// 2nd part ----------------------
			}
		}
		catch (JSONException e)
		{
			System.out.println("get_geocaches_ret: JSONException2");

			rr2.count_p = count_p;
			rr2.points = null;
			return rr2;
		}

		return rr2;
	}

	public static boolean isEmpty(String[] a)
	{
		if (a == null)
		{
			return true;
		}

		for (String s : a)
		{
			if ((s == null) || (s.equals("")))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * read all viewstates from page
	 * 
	 * @return String[] with all view states
	 */
	public static String[] getViewstates(String page)
	{
		// Get the number of viewstates.
		// If there is only one viewstate, __VIEWSTATEFIELDCOUNT is not present
		int count = 1;
		final Matcher matcherViewstateCount = patternViewstateFieldCount.matcher(page);
		if (matcherViewstateCount.find())
		{
			count = Integer.parseInt(matcherViewstateCount.group(1));
		}

		String[] viewstates = new String[count];

		// Get the viewstates
		int no;
		final Matcher matcherViewstates = patternViewstates.matcher(page);
		while (matcherViewstates.find())
		{
			String sno = matcherViewstates.group(1); // number of viewstate
			if ("".equals(sno))
			{
				no = 0;
			}
			else
			{
				no = Integer.parseInt(sno);
			}
			// System.out.println("1 v " + no + "=" + matcherViewstates.group(2));
			viewstates[no] = matcherViewstates.group(2);
		}

		return viewstates;
	}

	/**
	 * put viewstates into request parameters
	 */
	private static void putViewstates(List<NameValuePair> params, String[] viewstates)
	{
		if (isEmpty(viewstates))
		{
			//System.out.println("EEEEEEEEEEEEEEE*EEEEEEEE");
			return;// params;
		}
		// System.out.println("***** __VIEWSTATE=" + viewstates[0]);
		params.add(new BasicNameValuePair("__VIEWSTATE", viewstates[0]));
		if (viewstates.length > 1)
		{
			for (int i = 1; i < viewstates.length; i++)
			{
				params.add(new BasicNameValuePair("__VIEWSTATE" + i, viewstates[i]));
			}
			params.add(new BasicNameValuePair("__VIEWSTATEFIELDCOUNT", viewstates.length + ""));
		}
		return;// params;
	}

	/**
	 * transfers the viewstates variables from a page (response) to parameters
	 * (next request)
	 */
	public static void transferViewstates(String page, List<NameValuePair> params)
	{
		putViewstates(params, getViewstates(page));
	}

	private void trust_Every_ssl_cert()
	{
		// NEVER enable this on a production release!!!!!!!!!!
		try
		{
			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
			{
				public boolean verify(String hostname, SSLSession session)
				{
					Log.d("aagtl", "DANGER !!! trusted hostname=" + hostname + " DANGER !!!");
					// return true -> mean we trust this cert !! DANGER !! DANGER !!
					return true;
				}
			});
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new X509TrustManager[] { new X509TrustManager()
			{
				public java.security.cert.X509Certificate[] getAcceptedIssuers()
				{
					Log.d("aagtl", "DANGER !!! 222222222");
					return new java.security.cert.X509Certificate[0];
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException
				{
					Log.d("aagtl", "DANGER !!! 333333333");
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException
				{
					Log.d("aagtl", "DANGER !!! 444444444444");
				}
			} }, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// NEVER enable this on a production release!!!!!!!!!!
	}

	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException
	{
		SSLContext sslContext = null;
		try
		{
			sslContext = SSLContext.getInstance("TLS");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	}

	public boolean login()
	{
		// System.out.println("--L--- LOGIN START -----");
		
		String login_url = "https://www.geocaching.com/login/default.aspx";

		DefaultHttpClient client2 = null;
		HttpHost proxy = null;

		if (CacheDownloader.DEBUG2_)
		{
			// NEVER enable this on a production release!!!!!!!!!!
			// NEVER enable this on a production release!!!!!!!!!!
			trust_Every_ssl_cert();

			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

			HttpParams params = new BasicHttpParams();
			params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
			params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
			params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);

			proxy = new HttpHost("192.168.0.1", 8888);
			params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

			ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
			client2 = new DefaultHttpClient(cm, params);
			// NEVER enable this on a production release!!!!!!!!!!
			// NEVER enable this on a production release!!!!!!!!!!
		}
		else
		{
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 10000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

			client2 = new DefaultHttpClient(httpParameters);
		}

		if (CacheDownloader.DEBUG2_)
		{
			client2.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}

		// read first page to check for login
		// read first page to check for login
		// read first page to check for login

		String websiteData2 = null;
		URI uri2 = null;
		try
		{
			uri2 = new URI(login_url);
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
			return false;
		}

		client2.setCookieStore(cookie_jar);

		HttpGet method2 = new HttpGet(uri2);

		method2.addHeader("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.8.1.10) Gecko/20071115 Firefox/2.0.0.10");
		method2.addHeader("Pragma", "no-cache");
		method2.addHeader("Accept-Language", "en");

		HttpResponse res = null;
		try
		{
			res = client2.execute(method2);
		}
		catch (ClientProtocolException e1)
		{
			e1.printStackTrace();
			return false;
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}

		InputStream data = null;
		try
		{
			data = res.getEntity().getContent();
		}
		catch (IllegalStateException e1)
		{
			e1.printStackTrace();
			return false;
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}

		websiteData2 = generateString(data);

		if (CacheDownloader.DEBUG_) System.out.println("111 cookies=" + dumpCookieStore(client2.getCookieStore()));
		cookie_jar = client2.getCookieStore();

		// on every page
		final Matcher matcherLogged2In = patternLogged2In.matcher(websiteData2);
		if (matcherLogged2In.find())
		{
			if (CacheDownloader.DEBUG_) System.out.println("login: -> already logged in (1)");
			return true;
		}

		// after login
		final Matcher matcherLoggedIn = patternLoggedIn.matcher(websiteData2);
		if (matcherLoggedIn.find())
		{
			if (CacheDownloader.DEBUG_) System.out.println("login: -> already logged in (2)");
			return true;
		}
		// read first page to check for login
		// read first page to check for login
		// read first page to check for login

		// ok post login data as formdata
		// ok post login data as formdata
		// ok post login data as formdata
		// ok post login data as formdata
		HttpPost method = new HttpPost(uri2);

		method.addHeader("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0) Firefox/7.0");
		method.addHeader("Pragma", "no-cache");
		method.addHeader("Content-Type", "application/x-www-form-urlencoded");
		method.addHeader("Accept-Language", "en");

		List<NameValuePair> loginInfo = new ArrayList<NameValuePair>();

		loginInfo.add(new BasicNameValuePair("__EVENTTARGET", ""));
		loginInfo.add(new BasicNameValuePair("__EVENTARGUMENT", ""));

		String[] viewstates = getViewstates(websiteData2);
		if (CacheDownloader.DEBUG_) System.out.println("vs==" + viewstates[0]);
		putViewstates(loginInfo, viewstates);

		loginInfo.add(new BasicNameValuePair("ctl00$ContentBody$tbUsername", this.main_aagtl.global_settings.options_username));
		loginInfo.add(new BasicNameValuePair("ctl00$ContentBody$tbPassword", this.main_aagtl.global_settings.options_password));
		loginInfo.add(new BasicNameValuePair("ctl00$ContentBody$btnSignIn", "Login"));
		loginInfo.add(new BasicNameValuePair("ctl00$ContentBody$cbRememberMe", "on"));

		//for (int i = 0; i < loginInfo.size(); i++)
		//{
		//	System.out.println("x*X " + loginInfo.get(i).getName() + " " + loginInfo.get(i).getValue());
		//}

		// set cookies
		client2.setCookieStore(cookie_jar);

		HttpEntity entity = null;
		try
		{
			entity = new UrlEncodedFormEntity(loginInfo, HTTP.UTF_8);
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return false;
		}

		//		try
		//		{
		//			System.out.println("1 " + entity.toString());
		//			InputStream in2 = entity.getContent();
		//			InputStreamReader ir2 = new InputStreamReader(in2, "utf-8");
		//			BufferedReader r = new BufferedReader(ir2);
		//			System.out.println("line=" + r.readLine());
		//		}
		//		catch (Exception e2)
		//		{
		//			e2.printStackTrace();
		//		}

		method.setEntity(entity);

		HttpResponse res2 = null;
		try
		{
			res2 = client2.execute(method);
			if (CacheDownloader.DEBUG_) System.out.println("login response ->" + String.valueOf(res2.getStatusLine()));
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}

		//		for (int x = 0; x < res2.getAllHeaders().length; x++)
		//		{
		//			System.out.println("## n=" + res2.getAllHeaders()[x].getName());
		//			System.out.println("## v=" + res2.getAllHeaders()[x].getValue());
		//		}

		InputStream data2 = null;
		try
		{
			data2 = res2.getEntity().getContent();
		}
		catch (IllegalStateException e1)
		{
			e1.printStackTrace();
			return false;
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}

		websiteData2 = generateString(data2);

		if (CacheDownloader.DEBUG_) System.out.println("2222 cookies=" + dumpCookieStore(client2.getCookieStore()));
		cookie_jar = client2.getCookieStore();

		String[] viewstates2 = getViewstates(websiteData2);
		if (CacheDownloader.DEBUG_) System.out.println("vs==" + viewstates2[0]);

		//		System.out.println("******");
		//		System.out.println("******");
		//		System.out.println("******");
		//		System.out.println(websiteData2);
		//		System.out.println("******");
		//		System.out.println("******");
		//		System.out.println("******");

		return true;

	}

	public static String dumpCookieStore(CookieStore cookieStore)
	{
		StringBuilder cookies = new StringBuilder();
		for (Cookie cookie : cookieStore.getCookies())
		{
			System.out.println("CC**\n\n");
			cookies.append(cookie.getName());
			cookies.append("=");
			cookies.append(cookie.getValue());
			cookies.append("=");
			cookies.append(cookie.getDomain());
			cookies.append(";" + "\n");
		}
		return cookies.toString();
	}

	public void saveCookies()
	{
		FileOutputStream fOut = null;
		OutputStreamWriter osw = null;
		// make dirs
		File dir1 = new File(this.main_aagtl.main_dir + "/config");
		dir1.mkdirs();
		// save cookies to file
		try
		{
			File cookie_file = new File(this.main_aagtl.main_dir + "/config/cookie.txt");
			fOut = new FileOutputStream(cookie_file);
			osw = new OutputStreamWriter(fOut);
			osw.write(cookie_jar.toString());
			osw.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("saveCookies: Exception1");
		}
		finally
		{
			try
			{
				osw.close();
				fOut.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.out.println("saveCookies: Exception2");
			}
		}
	}

	public String getCookies()
	{
		String ret = "";

		// System.out.println("getCookies size: " +
		// cookie_jar.getCookies().size());
		try
		{
			for (int i = 0; i < cookie_jar.getCookies().size(); i++)
			{
				// System.out.println("getCookies: " +
				// cookie_jar.getCookies().get(i).toString());
				if (i > 0)
				{
					ret = ret + "; ";
				}
				ret = ret + cookie_jar.getCookies().get(i).getName() + "=" + cookie_jar.getCookies().get(i).getValue();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// System.out.println("getCookies: Exception");
		}
		// System.out.println("getCookies: ret=" + ret);
		return ret;
	}

	public String[] getCookies2()
	{
		if (cookie_jar.getCookies().size() == 0)
		{
			return null;
		}
		String[] ret = new String[cookie_jar.getCookies().size() * 2];

		// System.out.println("getCookies size: " +
		// cookie_jar.getCookies().size());
		try
		{
			for (int i = 0; i < cookie_jar.getCookies().size(); i++)
			{
				// System.out.println("getCookies: " +
				// cookie_jar.getCookies().get(i).toString());
				ret[i * 2] = cookie_jar.getCookies().get(i).getName();
				ret[(i * 2) + 1] = cookie_jar.getCookies().get(i).getValue();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// System.out.println("getCookies: Exception");
		}
		return ret;
	}

	public void loadCookies()
	{
		String[] ret = new String[2];

		// make dirs
		File dir1 = new File(this.main_aagtl.main_dir + "/config");
		dir1.mkdirs();
		// load cookies from file
		File cookie_file = new File(this.main_aagtl.main_dir + "/config/cookie.txt");

		FileInputStream fIn = null;
		InputStreamReader isr = null;
		char[] inputBuffer = new char[255];
		Writer writer = new StringWriter();
		String data = null;
		try
		{
			fIn = new FileInputStream(cookie_file);
			isr = new InputStreamReader(fIn);
			int n = 0;
			while ((n = isr.read(inputBuffer)) != -1)
			{
				writer.write(inputBuffer, 0, n);
			}
			data = writer.toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("loadCookies: Exception1");
			return;
		}
		finally
		{
			try
			{
				isr.close();
				fIn.close();
			}
			catch (NullPointerException e2)
			{
				System.out.println("loadCookies: Exception2");
				return;
			}
			catch (IOException e)
			{
				System.out.println("loadCookies: Exception3");

				e.printStackTrace();
				return;
			}
		}

		if (cookie_jar == null)
		{
			// cookie_jar = new CookieStore();
			return;
		}
		else
		{
			cookie_jar.clear();
		}

		// Log.d("load cookie:", "->" + String.valueOf(data));

		// [[version: 0][name: ASP.NET_SessionId]
		// [value: cyuoctxrwio1x13vivqzlxgi][domain: www.geocaching.com]
		// [path: /][expiry: null], [version: 0][name: userid]
		// [value: 8a72e55f-419c-4da7-8de3-7813a3fda9c7][domain:
		// www.geocaching.com]
		// [path: /][expiry: Tue Apr 26 15:41:14 Europe/Belgrade 2011]]

		if (data.length() > 1)
		{
			// check for multpile cookies
			if (data.startsWith("[["))
			{
				// strip [ and ] at begin and end of string
				data = data.substring(1, data.length() - 1);
				String s3 = "\\], \\[";
				String[] a3 = data.split(s3);
				String data_cookie;
				for (int j3 = 0; j3 < a3.length; j3++)
				{
					data_cookie = a3[j3];
					if (j3 == 0)
					{
						data_cookie = data_cookie + "]";
					}
					else
					{
						data_cookie = "[" + data_cookie;
					}
					// System.out.println("parsing cookie #" + j3 + ": " +
					// data_cookie);

					String s2 = "]";
					String[] a = data_cookie.split(s2);
					String x = null;
					String c1, c2 = null;
					String c_name = null, c_value = null, c_domain = null, c_path = null;
					String c_version = null;
					BasicClientCookie this_cookie = null;

					for (int j = 0; j < a.length; j++)
					{
						x = a[j].replace("[", "").trim();
						c1 = x.split(":")[0];
						c2 = x.split(":")[1].substring(1);
						// Log.d("load cookie:", "->" + String.valueOf(c1));
						// Log.d("load cookie:", "->" + String.valueOf(c2));
						if (c1.matches("name") == true)
						{
							// Log.d("name:", "->" + String.valueOf(c1));
							c_name = c2;
						}
						else if (c1.matches("value") == true)
						{
							c_value = c2;
						}
						else if (c1.matches("domain") == true)
						{
							c_domain = c2;
						}
						else if (c1.matches("path") == true)
						{
							c_path = c2;
						}
						else if (c1.matches("version") == true)
						{
							c_version = c2;
						}
					}
					this_cookie = new BasicClientCookie(c_name, c_value);
					this_cookie.setDomain(c_domain);
					this_cookie.setPath(c_path);
					// System.out.println("created cookie: ->" +
					// String.valueOf(this_cookie));

					this.cookie_jar.addCookie(this_cookie);

				}
			}
			// single cookie
			else
			{
				String s2 = "]";
				String[] a = data.split(s2);
				String x = null;
				String c1, c2 = null;
				String c_name = null, c_value = null, c_domain = null, c_path = null;
				String c_version = null;
				BasicClientCookie this_cookie = null;
				for (int j = 0; j < a.length; j++)
				{
					x = a[j].replace("[", "").trim();
					c1 = x.split(":")[0];
					c2 = x.split(":")[1].substring(1);
					// Log.d("load cookie:", "->" + String.valueOf(c1));
					// Log.d("load cookie:", "->" + String.valueOf(c2));
					if (c1.matches("name") == true)
					{
						// Log.d("name:", "->" + String.valueOf(c1));
						c_name = c2;
					}
					else if (c1.matches("value") == true)
					{
						c_value = c2;
					}
					else if (c1.matches("domain") == true)
					{
						c_domain = c2;
					}
					else if (c1.matches("path") == true)
					{
						c_path = c2;
					}
					else if (c1.matches("version") == true)
					{
						c_version = c2;
					}
				}
				this_cookie = new BasicClientCookie(c_name, c_value);
				this_cookie.setDomain(c_domain);
				this_cookie.setPath(c_path);
				// System.out.println("created cookie: ->" +
				// String.valueOf(this_cookie));

				this.cookie_jar.addCookie(this_cookie);
			}
		}

		return;
	}

	public String getUrlData(String url)
	{
		String websiteData = null;

		if (!this.logged_in)
		{
			System.out.println("--1--- LOGIN START -----");
			this.logged_in = login();
			System.out.println("--1--- LOGIN END   -----");
		}

		try
		{
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is
			// established.
			int timeoutConnection = 10000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 10000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

			DefaultHttpClient client = new DefaultHttpClient(httpParameters);
			client.setCookieStore(cookie_jar);
			// Log.d("cookie_jar", "->" + String.valueOf(cookie_jar));

			URI uri = new URI(url);
			HttpGet method = new HttpGet(uri);

			method.addHeader("User-Agent", "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.8.1.10) Gecko/20071115 Firefox/2.0.0.10");
			method.addHeader("Pragma", "no-cache");

			HttpResponse res = client.execute(method);
			InputStream data = res.getEntity().getContent();
			websiteData = generateString(data);
			client.getConnectionManager().shutdown();

		}
		catch (SocketTimeoutException e2)
		{
			Log.d("HTMLDownloader", "Connectiont timout: " + e2);
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (UnknownHostException x)
		{
			Log.d("HTMLDownloader", ": " + x);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return websiteData;
	}

	public String generateString(InputStream stream)
	{
		InputStreamReader reader = new InputStreamReader(stream);
		BufferedReader buffer = new BufferedReader(reader, large_buffer_size);
		// StringBuilder sb = new StringBuilder();
		String ret = "";

		try
		{
			// ***DEBUG*** System.out.println("int=" +(char) buffer.read());
			String cur;
			while ((cur = buffer.readLine()) != null)
			{
				// System.out.println("int=" + cur);
				// System.out.println("**"+cur);
				// sb.append(cur + "\n");
				ret = ret + cur + "\n";
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			stream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		//return sb.toString();
		return ret;
	}

}
