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

package com.morphoss.acal.activity.serverconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.content.ContentValues;
import android.util.Log;
import android.util.Xml;

import com.morphoss.acal.Constants;
import com.morphoss.acal.database.AcalDBHelper;
import com.morphoss.acal.providers.Servers;

public class ServerConfigData {

	static public final String TAG = "aCal ServerConfigData";
	private ContentValues server;

	public ServerConfigData(ContentValues cv) {
		server = cv;
	}

	/**This method should be called by any class that is going to write a ContentValues 
	 * object to the Server DB when the cv originated from this class.
	 * 
	 * @param cv The content Values object to prune
	 */
	public static final void removeNonDBFields(final ContentValues cv) {
		if (cv.containsKey("INFO")) cv.remove("INFO");	//information string describing server
	}
	
	public ContentValues getContentValues() {
		if ( server.getAsString(Servers.SUPPLIED_USER_URL) == null
				&& server.getAsString(Servers.OLD_SUPPLIED_DOMAIN) != null ) {
			server.put(Servers.SUPPLIED_USER_URL,
					server.getAsString(Servers.OLD_SUPPLIED_DOMAIN) +
					(server.getAsString(Servers.OLD_SUPPLIED_PATH) == null ? "/" : server.getAsString(Servers.OLD_SUPPLIED_PATH)));
		}
		
		return server;
	}

	public void writeToFile(File file) throws IOException {
		FileOutputStream fileos = null;
		try {
			fileos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			throw new IOException("File not found: "+e);
		}

		XmlSerializer serializer = Xml.newSerializer();

		//we set the FileOutputStream as output for the serializer, using UTF-8 encoding
		serializer.setOutput(fileos, "UTF-8");
		//Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null)
		serializer.startDocument("utf-8",null);
		//set indentation option
		serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		//set our namespace prefix
		serializer.setPrefix("", Constants.NS_ACALCONFIG);
		//start a root tag
		serializer.startTag(Constants.NS_ACALCONFIG, "acal");
		serializer.startTag(null, "db_version");
		serializer.text(AcalDBHelper.DB_VERSION+"");
		serializer.endTag(null, "db_version");
		serializer.startTag(null, Servers.DATABASE_TABLE);
		serializer.startTag(null, Servers.FRIENDLY_NAME);
		serializer.text(server.getAsString(Servers.FRIENDLY_NAME));
		serializer.endTag(null, Servers.FRIENDLY_NAME);
		serializer.startTag(null, "config_values");


		for (Entry<String,Object> s : server.valueSet()) {
			if (s.getValue() == null) continue;
			serializer.startTag(null, s.getKey());
			try {
				serializer.text(URLEncoder.encode(s.getValue().toString(),"utf-8"));
			} catch (NullPointerException e) {}
			serializer.endTag(null, s.getKey());	
		}


		serializer.endTag(null, "config_values");
		serializer.endTag(null, Servers.DATABASE_TABLE);
		serializer.endTag(Constants.NS_ACALCONFIG, "acal");
		serializer.endDocument();
		//write xml data into the FileOutputStream
		serializer.flush();
		//finally we close the file stream
		fileos.close();
	}

	public static List<ServerConfigData> getServerConfigDataFromFile(InputStream in) {
		//use Sax to deconstruct xml
		SAXParserFactory spf = SAXParserFactory.newInstance();
		ServerDataSaxParser sdsp = new ServerDataSaxParser();
		try {

			//get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			//parse the file and also register this class for call backs
			sp.parse(in, sdsp);

		}catch(SAXException se) {
			if (Constants.LOG_DEBUG)Log.d(TAG,Log.getStackTraceString(se));
		}catch(ParserConfigurationException pce) {
			if (Constants.LOG_DEBUG)Log.d(TAG,Log.getStackTraceString(pce));
		}catch (IOException ie) {
			if (Constants.LOG_DEBUG)Log.d(TAG,Log.getStackTraceString(ie));
		}

		return sdsp.getList();
	}

	public static List<ServerConfigData> getServerConfigDataFromFile(File file) {
		try {
			return getServerConfigDataFromFile(new FileInputStream(file));
		}
		catch ( FileNotFoundException e ) {
			Log.e(TAG,"File '"+file.getAbsolutePath()+"' not found", e);
		}
		return null;
	}

	private static class ServerDataSaxParser extends DefaultHandler {
		private StringBuffer tempVal = new StringBuffer();
		private boolean inServerSection = false;
		private List<ServerConfigData> serversList;
		ContentValues currentValues;

		public ServerDataSaxParser() {
			serversList = new ArrayList<ServerConfigData>();
		}

		public List<ServerConfigData> getList() {
			return this.serversList;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (localName.equalsIgnoreCase(Servers.DATABASE_TABLE)) {
				if (!inServerSection) {
					currentValues = new ContentValues();
					inServerSection = !inServerSection;
				} 
			}
			tempVal = new StringBuffer();
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (!inServerSection) return;

			if (localName.equalsIgnoreCase(Servers.DATABASE_TABLE)) {
				serversList.add(new ServerConfigData(currentValues));
				currentValues = null;
				inServerSection = !inServerSection;
				return;
			}
			if (localName.equalsIgnoreCase(Servers.FRIENDLY_NAME)) {
				currentValues.put(Servers.FRIENDLY_NAME,tempVal.toString());
			}
			if (localName.equalsIgnoreCase("INFO")) {
				currentValues.put("INFO",tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.HOSTNAME)) {
				currentValues.put(Servers.HOSTNAME,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.SUPPLIED_USER_URL)) {
				currentValues.put(Servers.SUPPLIED_USER_URL,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.USERNAME)) {
				currentValues.put(Servers.USERNAME,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.PASSWORD)) {
				currentValues.put(Servers.PASSWORD,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.PORT)) {
				currentValues.put(Servers.PORT,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.AUTH_TYPE)) {
				currentValues.put(Servers.AUTH_TYPE,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.ACTIVE)) {
				currentValues.put(Servers.ACTIVE,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.USE_SSL)) {
				currentValues.put(Servers.USE_SSL,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.OLD_SUPPLIED_PATH)) {
				currentValues.put(Servers.OLD_SUPPLIED_PATH,tempVal.toString());
			}
			else if (localName.equalsIgnoreCase(Servers.OLD_SUPPLIED_DOMAIN)) {
				currentValues.put(Servers.OLD_SUPPLIED_DOMAIN,tempVal.toString());
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			//try utf-8 decoding first, otherwise just dump the literal string
			try {
				tempVal.append(URLDecoder.decode(new String(ch,start,length),"utf-8"));
			} catch (Exception e) { tempVal.append(new String(ch,start,length)); }
		}
	}
}
