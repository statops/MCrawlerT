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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import moz.http.HttpData;
import moz.http.HttpRequest;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;


public class FieldnotesUploader
{
	String							URL			= "http://www.geocaching.com/my/uploadfieldnotes.aspx";
	HTMLDownloader					downloader	= null;
	List<GeocacheCoordinate>	gc_with_fn	= null;

	public class data_ret
	{
		String						encoding;
		ByteArrayOutputStream	data;
	}

	public FieldnotesUploader(HTMLDownloader dl, List<GeocacheCoordinate> gc_with_fieldnotes)
	{
		this.gc_with_fn = gc_with_fieldnotes;
		this.downloader = dl;
	}

	public Boolean upload()
	{
		return this.upload_v3();
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

			char[] buffer = new char[HTMLDownloader.large_buffer_size];
			try
			{
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"),
						HTMLDownloader.large_buffer_size);
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
			return "";
		}
	}

	public String SendPost(String httpURL, String data, String _cookie) throws IOException
	{
		URL url = new URL(httpURL);
		//URL url = new URL("http://zoff.cc/xx.cgi");

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty("Connection", "Keep-Alive");

		//System.out.println("C=" + _cookie);
		connection
				.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.8.1.10) Gecko/20071115 Firefox/2.0.0.10");
		connection.setRequestProperty("Cookie", _cookie);
		connection.connect();

		if (data != "")
		{
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(data);
			out.flush();
			out.close();
		}

		// Save Cookie
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()),
				HTMLDownloader.default_buffer_size);
		String headerName = null;
		//_cookies.clear();
		if (_cookie == "")
		{
			for (int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++)
			{
				if (headerName.equalsIgnoreCase("Set-Cookie"))
				{
					String cookie = connection.getHeaderField(i);
					_cookie += cookie.substring(0, cookie.indexOf(";")) + "; ";
				}
			}
		}

		// Get HTML from Server
		String getData = "";
		String decodedString;
		while ((decodedString = in.readLine()) != null)
		{
			getData += decodedString + "\n";
		}
		in.close();

		return getData;
	}

	public data_ret _encode_multipart_formdata(Hashtable<String, String> values, String file_field,
			String filename, byte[] file_data) throws IOException
	{
		data_ret ret2 = new data_ret();
		//byte[] ret = new byte[1];
		ByteArrayOutputStream b = new ByteArrayOutputStream();


		String BOUNDARY = "----------ThIs_Is_tHe_bouNdaRY_$";
		String CRLF = "\r\n";

		Enumeration<String> i = values.keys();
		String x = null;
		String y = null;

		// values
		while (i.hasMoreElements())
		{
			x = i.nextElement();
			y = values.get(x);

			if (b.size() > 0)
			{
				b.write(CRLF.getBytes());
			}
			else
			{
				//ret = "";
			}
			//ret = ret + "--" + BOUNDARY + CRLF;
			//ret = ret + "Content-Disposition: form-data; name=\"" + x + "\"" + CRLF;
			//ret = ret + CRLF;
			//ret = ret + y;
			b.write(("--" + BOUNDARY + CRLF).getBytes());
			b.write(("Content-Disposition: form-data; name=\"" + x + "\"" + CRLF).getBytes());
			b.write((CRLF + y).getBytes());
		}

		// file
		if (b.size() > 0)
		{
			//ret = ret + CRLF;
			b.write(CRLF.getBytes());
		}
		else
		{
			//ret = "";
		}
		b
				.write(("--" + BOUNDARY + CRLF + "Content-Disposition: form-data; name=\"" + file_field
						+ "\"; filename=\"" + filename + "\"" + CRLF + "Content-Type: " + "text/plain"
						+ CRLF + CRLF).getBytes());
		b.write(file_data);

		// finish
		if (b.size() > 0)
		{
			// ret = ret + CRLF;
			b.write(CRLF.getBytes());
		}
		else
		{
			// ret = "";
		}
		b.write(("--" + BOUNDARY + "--" + CRLF + CRLF).getBytes());

		ret2.data = b;
		ret2.encoding = String.format("multipart/form-data; boundary=%s", BOUNDARY);
		return ret2;
	}

	public Boolean upload_v3()
	{
		Boolean succ = true;

		this.downloader.login();
		String page = this.downloader.getUrlData(this.URL);
		String viewstate = "";
		Pattern p = Pattern
				.compile("<input type=\"hidden\" name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"([^\"]+)\" />");
		Matcher m = p.matcher(page);
		m.find();
		viewstate = m.group(1);

		//System.out.println("viewstate=" + viewstate);
		// got viewstate

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");

		String cookies_string = this.downloader.getCookies();

		// loop through fieldnotes
		// loop through fieldnotes
		// loop through fieldnotes
		// loop through fieldnotes
		byte[] raw_upload_data2 = null;
		GeocacheCoordinate element = null;
		Hashtable<String, String> ht = new Hashtable<String, String>();
		ht.put("ctl00$ContentBody$btnUpload", "Upload Field Note");
		ht.put("ctl00$ContentBody$chkSuppressDate", "");
		ht.put("__VIEWSTATE", viewstate);
		List<NameValuePair> values_list = null;
		data_ret temp = null;
		Iterator<GeocacheCoordinate> itr = this.gc_with_fn.iterator();
		while (itr.hasNext())
		{
			// loop through fieldnotes
			// loop through fieldnotes
			// loop through fieldnotes
			// loop through fieldnotes
			element = itr.next();

			raw_upload_data2 = null;

			String fn_status_string = null;
			try
			{
				fn_status_string = GeocacheCoordinate.LOG_AS_HASH.get(element.log_as);
				//System.out.println("LOG_AS=" + element.log_as);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unknown LOG_AS type!!");
				break;
			}

			String raw = element.name + "," + sdf.format(cal.getTime()) + "," + fn_status_string
					+ ",\"" + element.fieldnotes + "\"";
			//System.out.println(raw);

			try
			{
				raw_upload_data2 = raw.getBytes("UTF-16");
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}

			values_list = new ArrayList<NameValuePair>();
			temp = null;
			try
			{
				temp = this._encode_multipart_formdata(ht, "ctl00$ContentBody$FieldNoteLoader",
						"geocache_visits.txt", raw_upload_data2);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			//System.out.println("\r\n1" + temp.encoding);
			//System.out.println("\r\n2" + temp.data);

			values_list.add(new BasicNameValuePair("Content-Type", temp.encoding));
			values_list
					.add(new BasicNameValuePair("Content-Length", String.valueOf(temp.data.size())));
			values_list.add(new BasicNameValuePair("User-Agent",
					"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)"));
			values_list.add(new BasicNameValuePair("Pragma", "no-cache"));
			String the_page = this.downloader
					.get_reader_stream(this.URL, values_list, temp.data, true);


			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			if (the_page.indexOf("records were successfully uploaded") != -1)
			{
				//System.out.println("Fieldnote uploaded OK");
				// reset status in DB, so field note won't get uploaded again

				GeocacheCoordinate temp_gc2 = element;
				temp_gc2.log_as = GeocacheCoordinate.LOG_NO_LOG;
				this.downloader.main_aagtl.pv.begin_trans();
				try
				{
					this.downloader.main_aagtl.pv.reset_point_fn(temp_gc2);
					this.downloader.main_aagtl.pv.commit();
				}
				finally
				{
					this.downloader.main_aagtl.pv.end_trans();
				}
			}
			else
			{
				System.out.println(the_page);
				System.out.println("Fieldnote upload ERROR");

				// set error status
				succ = false;
			}
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!
			// check here if fieldnotes uploaded ok!!!!!!!!!


			// loop through fieldnotes
			// loop through fieldnotes
			// loop through fieldnotes
			// loop through fieldnotes
		}
		// loop through fieldnotes
		// loop through fieldnotes
		// loop through fieldnotes
		// loop through fieldnotes

		return succ;
	}

	public Boolean upload_v2()
	{
		this.downloader.login();
		String page = this.downloader.getUrlData(this.URL);
		String viewstate = "";
		Pattern p = Pattern
				.compile("<input type=\"hidden\" name=\"__VIEWSTATE\" id=\"__VIEWSTATE\" value=\"([^\"]+)\" />");
		Matcher m = p.matcher(page);
		m.find();
		viewstate = m.group(1);

		//System.out.println("viewstate=" + viewstate);
		// got viewstate

		InputStream fn_is = null;
		String raw_upload_data = "";
		try
		{
			fn_is = new ByteArrayInputStream(("GC2BNHP,2010-11-07T14:00Z,Write note,\"bla bla\"")
					.getBytes("UTF-8"));
			raw_upload_data = "GC2BNHP,2010-11-07T20:50Z,Write note,\"bla bla\"".getBytes("UTF-8")
					.toString();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}


		String cookies_string = this.downloader.getCookies();

		ArrayList<InputStream> files = new ArrayList();
		files.add(fn_is);

		Hashtable<String, String> ht = new Hashtable<String, String>();
		ht.put("ctl00$ContentBody$btnUpload", "Upload Field Note");
		ht.put("ctl00$ContentBody$chkSuppressDate", "");
		//	ht.put("ctl00$ContentBody$FieldNoteLoader", "geocache_visits.txt");
		ht.put("__VIEWSTATE", viewstate);

		HttpData data = HttpRequest.post(this.URL, ht, files, cookies_string);
		//System.out.println(data.content);


		String boundary = "----------ThIs_Is_tHe_bouNdaRY_$";
		String crlf = "\r\n";

		URL url = null;
		try
		{
			url = new URL(this.URL);
		}
		catch (MalformedURLException e2)
		{
			e2.printStackTrace();
		}
		HttpURLConnection con = null;
		try
		{
			con = (HttpURLConnection) url.openConnection();
		}
		catch (IOException e2)
		{
			e2.printStackTrace();
		}
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		try
		{
			con.setRequestMethod("POST");
		}
		catch (java.net.ProtocolException e)
		{
			e.printStackTrace();
		}

		con.setRequestProperty("Cookie", cookies_string);
		//System.out.println("Cookie: " + cookies_string[0] + "=" + cookies_string[1]);

		con.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)");
		con.setRequestProperty("Pragma", "no-cache");
		//con.setRequestProperty("Connection", "Keep-Alive");
		String content_type = String.format("multipart/form-data; boundary=%s", boundary);
		con.setRequestProperty("Content-Type", content_type);


		DataOutputStream dos = null;
		try
		{
			dos = new DataOutputStream(con.getOutputStream());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		String raw_data = "";

		//
		raw_data = raw_data + "--" + boundary + crlf;
		raw_data = raw_data
				+ String.format("Content-Disposition: form-data; name=\"%s\"",
						"ctl00$ContentBody$btnUpload") + crlf;
		raw_data = raw_data + crlf;
		raw_data = raw_data + "Upload Field Note" + crlf;
		//

		//
		raw_data = raw_data + "--" + boundary + crlf;
		raw_data = raw_data
				+ String.format("Content-Disposition: form-data; name=\"%s\"",
						"ctl00$ContentBody$chkSuppressDate") + crlf;
		raw_data = raw_data + crlf;
		raw_data = raw_data + "" + crlf;
		//

		//
		raw_data = raw_data + "--" + boundary + crlf;
		raw_data = raw_data
				+ String.format("Content-Disposition: form-data; name=\"%s\"", "__VIEWSTATE") + crlf;
		raw_data = raw_data + crlf;
		raw_data = raw_data + viewstate + crlf;
		//

		//
		raw_data = raw_data + "--" + boundary + crlf;
		raw_data = raw_data
				+ String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"",
						"ctl00$ContentBody$FieldNoteLoader", "geocache_visits.txt") + crlf;
		raw_data = raw_data + String.format("Content-Type: %s", "text/plain") + crlf;
		raw_data = raw_data + crlf;
		raw_data = raw_data + raw_upload_data + crlf;
		//

		//
		raw_data = raw_data + "--" + boundary + "--" + crlf;
		raw_data = raw_data + crlf;

		try
		{
			this.SendPost(this.URL, raw_data, cookies_string);
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}


		//System.out.println(raw_data);

		try
		{
			dos.writeBytes(raw_data);
			//dos.writeChars(raw_data);
			dos.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		HttpData ret2 = new HttpData();
		BufferedReader rd = null;
		try
		{
			rd = new BufferedReader(new InputStreamReader(con.getInputStream()),
					HTMLDownloader.large_buffer_size);
			String line;
			while ((line = rd.readLine()) != null)
			{
				ret2.content += line + "\r\n";
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		//get headers
		Map<String, List<String>> headers = con.getHeaderFields();
		Set<Entry<String, List<String>>> hKeys = headers.entrySet();
		for (Iterator<Entry<String, List<String>>> i = hKeys.iterator(); i.hasNext();)
		{
			Entry<String, List<String>> m99 = i.next();

			//System.out.println("HEADER_KEY" + m99.getKey() + "=" + m99.getValue());
			ret2.headers.put(m99.getKey(), m99.getValue().toString());
			if (m99.getKey().equals("set-cookie"))
				ret2.cookies.put(m99.getKey(), m99.getValue().toString());
		}
		try
		{
			dos.close();
			rd.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		//System.out.println(ret2.content);


		//System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFF");
		ClientHttpRequest client_req;
		try
		{
			client_req = new ClientHttpRequest(this.URL);
			String[] cookies_string2 = this.downloader.getCookies2();
			for (int jk = 0; jk < cookies_string2.length; jk++)
			{
				System.out.println(cookies_string2[jk * 2] + "=" + cookies_string2[(jk * 2) + 1]);
				client_req.setCookie(cookies_string2[jk * 2], cookies_string2[(jk * 2) + 1]);
			}
			client_req.setParameter("ctl00$ContentBody$btnUpload", "Upload Field Note");
			client_req.setParameter("ctl00$ContentBody$FieldNoteLoader", "geocache_visits.txt", fn_is);
			InputStream response = client_req.post();
			//System.out.println(this.convertStreamToString(response));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		//ArrayList<InputStream> files = new ArrayList();
		files.clear();
		files.add(fn_is);

		Hashtable<String, String> ht2 = new Hashtable<String, String>();
		ht2.put("ctl00$ContentBody$btnUpload", "Upload Field Note");
		ht2.put("ctl00$ContentBody$chkSuppressDate", "");
		//	ht.put("ctl00$ContentBody$FieldNoteLoader", "geocache_visits.txt");
		ht2.put("__VIEWSTATE", viewstate);

		HttpData data3 = HttpRequest.post(this.URL, ht2, files, cookies_string);
		//System.out.println(data3.content);


		//		String the_page2 = this.downloader.get_reader_mpf(this.URL, raw_data, null, true, boundary);
		//System.out.println("page2=\n" + the_page2);

		Boolean ret = false;
		return ret;
	}

}
