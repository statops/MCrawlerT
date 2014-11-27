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

package com.morphoss.acal.service.connector;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.morphoss.acal.AcalDebug;
import com.morphoss.acal.Constants;
import com.morphoss.acal.StaticHelpers;
import com.morphoss.acal.activity.serverconfig.AuthenticationFailure;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.xml.DavNode;
import com.morphoss.acal.xml.DavParserFactory;

public class AcalRequestor {

	final private static String TAG = "AcalRequestor";

	private static final int	LONG_LINE_WRAP_FOR_DEBUG	= 500;

	private boolean initialised = false;
	
	// Basic URI components
	private String hostName = null;
	private String path = null;
	private String protocol = null;
	private int port = 0;
	private String method = "PROPFIND";

	// Authentication crap.
	private boolean authRequired = false;
	private int authType  = Servers.AUTH_NONE; 
//	private Header wwwAuthenticate = null;
	private String authRealm = null;
	private String nonce = null;
	private String opaque = null;
	private String cnonce = null;
	private String qop = null;
	private int authNC = 0;
	private String algorithm = null;

	private String username = null;
	private String password = null;
	
	private HttpParams httpParams;
	private HttpClient httpClient;
	private ThreadSafeClientConnManager connManager;
	private Header responseHeaders[];
	private int statusCode = -1;
	private int connectionTimeOut = 30000;
	private int socketTimeOut = 60000;
	private int redirectLimit = 5;
	private int redirectCount = 0;

	private DavRequest		request  = null;
	private HttpResponse	response = null;

	private boolean	debugThisRequest = false;


	public final static String PROTOCOL_HTTP = "http";
	public final static String PROTOCOL_HTTPS = "https";
	
	/**
	 * Construct an uninitialised AcalRequestor.  After calling this you will need to
	 * initialise things by either calling setFromServer() or interpretUriString() before
	 * you will be able to make a request. 
	 */
	public AcalRequestor() {
		debugThisRequest = Constants.debugDavCommunication;
	}

	public void enableDebugging() {
		debugThisRequest = true;
	}
	
	/**
	 * Construct a new contentvalues from these path components.
	 * @param hostIn
	 * @param proto
	 * @param portIn
	 * @param pathIn
	 * @param user
	 * @param pass
	 */
	public AcalRequestor( String hostIn, Integer proto, Integer portIn, String pathIn, String user, String pass ) {
		setHostName(hostIn);
		setPortProtocol(portIn,proto);
		setPath(pathIn);
		username = user;
		password = pass;

		initialise();
	}

	/**
	 * Construct a new AcalRequestor from the values in a ContentValues which has been read
	 * from a Server row in the database.  In this case the hostname / path will be set from
	 * the 'simple' configuration values.
	 * @param cvServerData
	 * @return
	 */
	public static AcalRequestor fromSimpleValues( ContentValues cvServerData ) {
		AcalRequestor result = new AcalRequestor();
		result.applyFromServer(cvServerData, true);
		return result;
	}


	/**
	 * Construct a new AcalRequestor from the values in a ContentValues which has been read
	 * from a Server row in the database.  The path will be set to the principal-path value
	 * so you may need to specify a different path on the actual request(s).
	 * @param cvServerData
	 * @return
	 */
	public static AcalRequestor fromServerValues( ContentValues cvServerData ) {
		AcalRequestor result = new AcalRequestor();
		result.applyFromServer(cvServerData, false);
		return result;
	}


	/**
	 * Adjust the current URI values to align with those in a ContentValues which has been read
	 * from a Server row in the database.  The path will be set to the principal-path value
	 * so you may need to specify a different path on the actual request(s)
	 * @param cvServerData
	 * @param simpleSetup true/false whether to use only the 'simple' values to initialise from
	 */
	public void applyFromServer( ContentValues cvServerData, boolean simpleSetup ) {
		if ( simpleSetup ) {
			protocol = null;
			hostName = null;
			port = 0;
			path = null;
			authType = Servers.AUTH_NONE;
			interpretUriString(cvServerData.getAsString(Servers.SUPPLIED_USER_URL));
		}
		else {
			setHostName(cvServerData.getAsString(Servers.HOSTNAME));
			setPath(cvServerData.getAsString(Servers.PRINCIPAL_PATH));
	
			String portString = cvServerData.getAsString(Servers.PORT);
			int tmpPort = 0;
			if ( portString != null && portString.length() > 0 ) tmpPort = Integer.parseInt(portString);
			setPortProtocol(tmpPort, cvServerData.getAsInteger(Servers.USE_SSL));
	
			setAuthType(cvServerData.getAsInteger(Servers.AUTH_TYPE));
	
		}
		if ( hostName == null ) hostName = "invalid";
		if ( path == null ) path = "/";

		authRequired = ( authType != Servers.AUTH_NONE );
		username = cvServerData.getAsString(Servers.USERNAME);
		password = cvServerData.getAsString(Servers.PASSWORD);

		if ( !initialised ) initialise();
	}

	
	private void initialise() {
		httpParams = AcalConnectionPool.defaultHttpParams(socketTimeOut, connectionTimeOut);
		connManager = AcalConnectionPool.getHttpConnectionPool();
		httpClient = new DefaultHttpClient(connManager, httpParams);

		initialised = true;
	}

	/**
	 * Takes the current AcalRequestor values and applies them to the Server ContentValues
	 * to be saved back in the database.  Used during the server discovery process.
	 * @param cvServerData
	 */
	public void applyToServerSettings(ContentValues cvServerData) {
		cvServerData.put(Servers.HOSTNAME, hostName);
		cvServerData.put(Servers.USE_SSL, (protocol.equals(PROTOCOL_HTTPS)?1:0));
		cvServerData.put(Servers.PORT, port);
		cvServerData.put(Servers.PRINCIPAL_PATH, path);
		cvServerData.put(Servers.AUTH_TYPE, authType );
	}

	
	/**
	 * Retrieve the HTTP headers received with the most recent response. 
	 * @return
	 */
	public Header[] getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * Retrieve the HTTP status code of the most recent response.
	 * @return
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Interpret the URI in the string to set protocol, host, port & path for the next request.
	 * If the URI only matches a path part then protocol/host/port will be unchanged. This call
	 * will only allow for path parts that are anchored to the web root.  This is used internally
	 * for following Location: redirects.
	 * 
	 * This is also used to interpret the 'path' parameter to the request calls generally.
	 * 
	 * @param uriString
	 */
	public void interpretUriString(String uriString) {

		if ( uriString == null ) return;

		// Match a URL, including an ipv6 address like http://[DEAD:BEEF:CAFE:F00D::]:8008/
		final Pattern uriMatcher = Pattern.compile(
					"^(?:(https?)://)?" + // Protocol
					"(" + // host spec
					"(?:(?:[a-z0-9-]+[.]){1,7}(?:[a-z0-9-]+))" +  // Hostname or IPv4 address
					"|(?:\\[(?:[0-9a-f]{0,4}:)+(?:[0-9a-f]{0,4})?\\])" + // IPv6 address
					")" +
					"(?:[:]([0-9]{2,5}))?" + // Port number
					"(/.*)?$" // Path bit.
					,Pattern.CASE_INSENSITIVE | Pattern.DOTALL );  

		final Pattern pathMatcher = Pattern.compile("^(/.*)$");
		
		if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Interpreting '"+uriString+"'");
		Matcher m = uriMatcher.matcher(uriString);
		if ( m.matches() ) {
			if ( m.group(1) != null && !m.group(1).equals("") ) {
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Found protocol '"+m.group(1)+"'");
				protocol = m.group(1);
				if ( m.group(3) == null || m.group(3).equals("") ) {
					port = (protocol.equals(PROTOCOL_HTTP) ? 80 : 443);
				}
			}
			if ( m.group(2) != null ) {
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Found hostname '"+m.group(2)+"'");
				setHostName( m.group(2) );
			}
			if ( m.group(3) != null && !m.group(3).equals("") ) {
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Found port '"+m.group(3)+"'");
				port = Integer.parseInt(m.group(3));
				if ( m.group(1) != null && (port == 0 || port == 80 || port == 443) ) {
					port = (protocol.equals(PROTOCOL_HTTP) ? 80 : 443);
				}
			}
			if ( m.group(4) != null && !m.group(4).equals("") ) {
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Found path '"+m.group(4)+"'");
				setPath(m.group(4));
			}
			if ( !initialised ) initialise();
		}
		else {
			m = pathMatcher.matcher(uriString);
			if (m.find()) {
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Found relative path '"+m.group(1)+"'");
				setPath( m.group(1) );
			}
			else {
				if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, "Using Uri class to process redirect...");
				Uri newLocation = Uri.parse(uriString);
				if ( newLocation.getHost() != null ) setHostName( newLocation.getHost() );
				setPortProtocol( newLocation.getPort(), newLocation.getScheme());
				setPath( newLocation.getPath() );
				if ( Constants.LOG_VERBOSE ) Log.println(Constants.LOGV,TAG,"Found new location at '"+fullUrl()+"'");
				
			}
		}
	}


	/**
	 * When a request fails with a 401 Unauthorized you can call this with the content
	 * of the WWW-Authenticate header in the response and it will modify the URI so that
	 * if you repeat the request the correct authentication should be used.
	 * 
	 * If you then get a 401, and this gets called again on that same Uri, it will throw
	 * an AuthenticationFailure exception rather than continue futilely.
	 * 
	 * @param authRequestHeader
	 * @throws AuthenticationFailure
	 */
	public void interpretRequestedAuth( Header authRequestHeader ) throws AuthenticationFailure {
		// Adjust our authentication setup so the next request will be able
		// to send the correct authentication headers...

		// WWW-Authenticate: Digest realm="DAViCal CalDAV Server", qop="auth", nonce="55a1a0c53c0f337e4675befabeff6a122b5b78de", opaque="52295deb26cc99c2dcc6614e70ed471f7a163e7a", algorithm="MD5"
		// WWW-Authenticate: Digest realm="SabreDAV",qop="auth",nonce="4f08e719a85d0",opaque="df58bdff8cf60599c939187d0b5c54de"
		// WWW-Authenticate:digest nonce="130183646896936966342199963268042751958404602087869166446", realm="Test Realm", algorithm="md5"

				
		if ( debugThisRequest )
			Log.println(Constants.LOGV,TAG,"Interpreting '"+authRequestHeader+"'");

		String name;
		for( HeaderElement he : authRequestHeader.getElements() ) {
			if ( debugThisRequest )
				Log.println(Constants.LOGV,TAG,"Interpreting Element: '"+he.toString()+"' ("+he.getName()+":"+he.getValue()+")");
			name = he.getName();

			if ( name.length() > 7 && name.substring(0, 7).equalsIgnoreCase("Digest ") ) { 
				authType = Servers.AUTH_DIGEST;
				qop = null;
				algorithm = "md5";
				name = name.substring(7);
				if ( debugThisRequest )
					Log.println(Constants.LOGV,TAG,"Found '"+getAuthTypeName(authType)+"' auth, realm: "+authRealm);
			}
			else if ( name.length() > 6 && name.substring(0, 6).equalsIgnoreCase("Basic ") ) { 
				authType = Servers.AUTH_BASIC;
				name = name.substring(6);
			}

			if ( name.equalsIgnoreCase("realm") ) {
				authRealm = he.getValue();
			}
			else if ( name.equalsIgnoreCase("nonce") ) {
				nonce = he.getValue();
			}
			else if ( name.equalsIgnoreCase("opaque") ) {
				opaque = he.getValue();
			}
			else if ( name.equalsIgnoreCase("qop") ) {
				if ( !he.getValue().equalsIgnoreCase("auth") ) {
					Log.w(TAG, "Digest Auth requested qop of '"+he.getValue()+"' but we only support 'auth'");
				}
			}
			else if ( name.equalsIgnoreCase("algorithm") ) {
				if ( !he.getValue().equalsIgnoreCase(algorithm) ) {
					Log.w(TAG, "Digest Auth requested algorithm of '"+he.getValue()+"' but we only support '"+algorithm+"'");
				}
			}
			else {
				Log.w(TAG, "Digest Auth requested algorithm of '"+he.getValue()+"' but we only support '"+algorithm+"'");
			}
		}

		authRequired = true;
	}

	
	private String md5( String in ) {
		// Create MD5 Hash
		MessageDigest digest;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(in.getBytes());
			return StaticHelpers.toHexString(digest.digest());
		}
		catch (NoSuchAlgorithmException e) {
			Log.e(TAG, e.getMessage());
			Log.println(Constants.LOGV,TAG, Log.getStackTraceString(e));
		}
	    return "";
	}

	
	private Header basicAuthHeader() {
		String authValue = String.format("Basic %s", Base64Coder.encodeString(username+":"+password));
		if ( Constants.LOG_VERBOSE )
					Log.println(Constants.LOGV,TAG, "BasicAuthDebugging: '"+authValue+"'" );
		return new BasicHeader("Authorization", authValue );
	}

	
	private Header digestAuthHeader() {
		String authValue;
		String A1 = md5( username + ":" + authRealm + ":" + password);
		String A2 = md5( method + ":" + path );
		cnonce = md5(AcalConnectionPool.getUserAgent());
		String printNC = String.format("%08x", ++authNC);
		String responseString = A1+":"+nonce+":"+printNC+":"+cnonce+":auth:"+A2;
		if ( debugThisRequest )
			Log.println(Constants.LOGV,TAG, "DigestDebugging: '"+responseString+"'" );
		String response = md5(responseString);
		authValue = String.format("Digest realm=\"%s\", username=\"%s\", nonce=\"%s\", uri=\"%s\""
					+ ", response=\"%s\", algorithm=\"%s\", cnonce=\"%s\", opaque=\"%s\", nc=\"%s\""
					+ (qop == null ? "" : ", qop=\"auth\""),
					authRealm, username, nonce, path,
					response, algorithm, cnonce, opaque, printNC );
		return new BasicHeader("Authorization", authValue );
	}

	
	private Header buildAuthHeader() throws AuthenticationFailure {
		switch( authType ) {
			case Servers.AUTH_BASIC:	return basicAuthHeader();
			case Servers.AUTH_DIGEST:	return digestAuthHeader();
			default:
				throw new AuthenticationFailure("Unknown authentication type");
		}
	}

	
	/**
	 * Get the current path used for the last request, or recently set.
	 * @return
	 */
	public String getPath() {
		return path;
	}

	
	/**
	 * Get the current authentication type used for the last request, or recently set.
	 * @return
	 */
	public int getAuthType() {
		return authType;
	}

	
	/**
	 * Set the port and protocol to the supplied values, with sanity checking.
	 * @param newPort As an integer.  Numbers < 1 or > 65535 are ignored.
	 * @param newProtocol As an integer where 1 is https and anything else is http
	 */
	public void setPortProtocol(Integer newPort, Integer newProtocol) {
		protocol = (newProtocol == null || newProtocol == 1 ? PROTOCOL_HTTPS : PROTOCOL_HTTP);
		if ( newPort == null || newPort < 1 || newPort > 65535 || newPort == 80 || newPort == 443 )
			port = (protocol.equals(PROTOCOL_HTTP) ? 80 : 443);
		else
			port = newPort;
	}


	/**
	 * Set the port and protocol to the supplied values, with sanity checking.  If the supplied
	 * newProtocol is null then we initially fall back to the current protocol, or http if that
	 * is null.
	 * @param newPort As an integer.  Numbers < 1 or > 65535 are ignored.
	 * @param newProtocol As a string like 'http' or 'https'
	 */
	public void setPortProtocol(Integer newPort, String newProtocol) {
		protocol = (newProtocol == null ? protocol : (newProtocol.equals(PROTOCOL_HTTPS) ? PROTOCOL_HTTPS : PROTOCOL_HTTP));
		if ( newPort == null || newPort < 1 || newPort > 65535 || newPort == 80 || newPort == 443 )
			port = (protocol.equals(PROTOCOL_HTTP) ? 80 : 443);
		else
			port = newPort;
	}


	/**
	 * Set the timeouts to use for subsequent requests, in milliseconds. The connectionTimeOut
	 * says how long to wait for the connection to be established, and the socketTimeOut says
	 * how long to wait for data after the connection is established. 
	 * @param newConnectionTimeOut
	 * @param newSocketTimeOut
	 */
	public void setTimeOuts( int newConnectionTimeOut, int newSocketTimeOut ) {
		if ( socketTimeOut == newSocketTimeOut && connectionTimeOut == newConnectionTimeOut ) return;
		socketTimeOut = newSocketTimeOut;
		connectionTimeOut = newConnectionTimeOut;
		if ( !initialised ) return;
		AcalConnectionPool.setTimeOuts(socketTimeOut,connectionTimeOut);
		httpClient = new DefaultHttpClient(connManager, httpParams);
	}

	
	/**
	 * Set the path for the next request, with some sanity checking to force the path
	 * to start with a '/'.
	 * @param newPath
	 */
	public void setPath(String newPath) {
		if ( newPath == null || newPath.equals("") ) {
			path = "/";
			return;
		}
		if ( !newPath.substring(0, 1).equals("/") ) {
			path = "/" + newPath;
		}
		else
			path = newPath;
	}


	/**
	 * Set the authentication type to be used for the next request.
	 * @param newAuthType
	 */
	public void setAuthType( Integer newAuthType ) {
		if ( newAuthType == Servers.AUTH_BASIC || newAuthType == Servers.AUTH_DIGEST ) { 
			authType = newAuthType;
			return;
		}
		authType = Servers.AUTH_NONE;
	}

	
	/**
	 * Force the next request to use authentication pre-emptively.
	 */
	public void setAuthRequired() {
		authRequired = true;
	}

	
	/**
	 * Return the current protocol://host:port as the start of a URL.
	 * @return
	 */
	public String protocolHostPort() {
		return protocol
				+ "://"
				+ hostName
				+ ((protocol.equals(PROTOCOL_HTTP) && port == 80) || (protocol.equals(PROTOCOL_HTTPS) && port == 443) ? "" : ":"+Integer.toString(port));
	}

	
	/**
	 * Return the current protocol://host.example.com:port/path/to/resource as a URL.
	 * @return
	 */
	public String fullUrl() {
		return protocolHostPort() + path;
	}

	
	/**
	 * Retrieve the unlocalised name of the authentication scheme currently in effect.
	 * @return
	 */
	public static String getAuthTypeName(int authCode) {
		switch (authCode) {
			// Only used in debug logging so don't need l10n
			case Servers.AUTH_BASIC:	return "Basic";
			case Servers.AUTH_DIGEST:	return "Digest";
			default:					return "NoAuth";
		}
	}

	
	private String getLocationHeader() {
		for( Header h : responseHeaders ) {
			if (debugThisRequest)
				Log.println(Constants.LOGV,TAG, "Looking for redirect in Header: " + h.getName() + ":" + h.getValue());
			if (h.getName().equalsIgnoreCase("Location"))
				return h.getValue();
		}
		return "";
	}

	
	private Header getAuthHeader() {
		Header selectedAuthHeader = null;
		for( Header h : responseHeaders ) {
			if (debugThisRequest)
				Log.println(Constants.LOGV,TAG, "Looking for auth in Header: " + h.getName() + ":" + h.getValue());
			if ( h.getName().equalsIgnoreCase("WWW-Authenticate") ) {
				// If this is a digest Auth header we will return with it
				for( HeaderElement he : h.getElements() ) {
					
					if ( he.getName().substring(0, 7).equalsIgnoreCase("Digest ") ) {
						return h;
					}
					else if ( he.getName().substring(0, 6).equalsIgnoreCase("Basic ") ) { 
						if ( selectedAuthHeader == null ) selectedAuthHeader = h;
					}
				}
		}
		}
		return selectedAuthHeader;
	}

	private String entityToString(HttpEntity entity) {
		InputStream in;
		StringBuilder total = new StringBuilder();
		try {
			in = entity.getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(in),AcalConnectionPool.DEFAULT_BUFFER_SIZE);
			String line;
			while ( (line = r.readLine()) != null ) {
			    total.append(line).append("\n");
			}
			in.close();
		}
		catch ( IllegalStateException e ) {
			Log.w(TAG,"Auto-generated catch block", e);
		}
		catch ( IOException e ) {
			Log.w(TAG,"Auto-generated catch block", e);
		}
		return total.toString();
	}

	
	private void logEntityLines(int logLevel, String prefix, String entityString) {
		for( String line : entityString.toString().split("\n") ) {
			if ( line.length() == entityString.toString().length() ) {
				int end;
				int length = line.length();
				for( int pos=0; pos < length; pos += LONG_LINE_WRAP_FOR_DEBUG ) {
					end = pos+LONG_LINE_WRAP_FOR_DEBUG;
					if ( end > length ) end = length;
					Log.println(logLevel,TAG, prefix + line.substring(pos, end) );
				}
			}
			else {
				Log.println(logLevel,TAG, prefix + line.replaceAll("\r$", "") );
			}
		}
	}

	
	/**
	 * Log the full details of the request.
	 * @param logLevel
	 */
	public void logRequest(int logLevel) {
		Log.println(logLevel,TAG, method+" "+this.fullUrl());
		if ( request == null ) {
			Log.w(TAG,"Attempting to log request entity but request is null!");
			return;
		}

		for ( Header h : request.getAllHeaders() ) {
			Log.println(logLevel,TAG,"H>  "+h.getName()+":"+h.getValue() );
		}
		if ( request.getEntity() == null ) return;

		String entityString = entityToString(request.getEntity()); 
		if (entityString != null) {
			Log.println(logLevel,TAG, "----------------------- vvv Request Body vvv -----------------------" );
			logEntityLines(logLevel, "R>  ", entityString);
			Log.println(logLevel,TAG, "----------------------- ^^^ Request Body ^^^ -----------------------" );
		}
	}

	public InputStream logResponse(int logLevel) {
		if ( response == null ) {
			Log.w(TAG,"Attempting to log response entity but response is null!");
			return null;
		}
		Log.println(logLevel,TAG, "RESPONSE: "+response.getStatusLine().getProtocolVersion()+" "+response.getStatusLine().getStatusCode()+" "+response.getStatusLine().getReasonPhrase() );

		for (Header h : responseHeaders) {
			Log.println(logLevel,TAG,"H<  "+h.getName()+": "+h.getValue() );
		}

		if ( response.getEntity() == null ) {
			Log.println(logLevel,TAG,"Attempting to log response entity but response.getEntity() is null :-(");
			return null;
		}
		
		String entityString = entityToString(response.getEntity());
		if (entityString != null) {
			Log.println(logLevel,TAG, "----------------------- vvv Response Body vvv -----------------------" );
			logEntityLines(logLevel, "R<  ", entityString);
			Log.println(logLevel,TAG, "----------------------- ^^^ Response Body ^^^ -----------------------" );
		}
		return new ByteArrayInputStream( entityString.getBytes() );
	}

	/**
	 * Marshall and send the request.
	 * @param headers
	 * @param entityString
	 * @return
	 * @throws SendRequestFailedException
	 * @throws SSLException
	 * @throws AuthenticationFailure
	 * @throws ConnectionFailedException
	 * @throws ConnectionPoolTimeoutException
	 */
	private synchronized InputStream sendRequest( Header[] headers, String entityString )
									throws SendRequestFailedException, SSLException, AuthenticationFailure,
									ConnectionFailedException, ConnectionPoolTimeoutException {
		long down = 0;
		long up = 0;
		long start = System.currentTimeMillis();

		if ( !initialised ) throw new IllegalStateException("AcalRequestor has not been initialised!");
		statusCode = -1;
		try {
			// Create request and add headers and entity
			request = new DavRequest(method, this.fullUrl());
//			request.addHeader(new BasicHeader("User-Agent", AcalConnectionPool.getUserAgent()));
			if ( headers != null ) for (Header h : headers) request.addHeader(h);

			if ( authRequired && authType != Servers.AUTH_NONE)
				request.addHeader(buildAuthHeader());
			else if ( authRequired ) {
				// Assume basicAuth
				request.addHeader(basicAuthHeader());
			}
			
			if (entityString != null) {
				request.setEntity(new StringEntity(entityString.toString(),"UTF-8"));
				up = request.getEntity().getContentLength();
			}
			

			// This trick greatly reduces the occurrence of host not found errors. 
			try { InetAddress.getByName(this.hostName); } catch (UnknownHostException e1) {
				Thread.sleep(100);
				try { InetAddress.getByName(this.hostName); } catch (UnknownHostException e2) {
					Thread.sleep(100);
				}
			}
			
			int requestPort = -1;
			if ( this.protocol == null ) this.protocol = PROTOCOL_HTTP;
			String requestProtocol = this.protocol;
			if ( (this.protocol.equals(PROTOCOL_HTTP) && this.port != 80 )
						|| ( this.protocol.equals(PROTOCOL_HTTPS) && this.port != 443 )
				) {
				requestPort = this.port;
			}

			if ( Constants.LOG_DEBUG || debugThisRequest ) {
				Log.println(Constants.LOGD,TAG, String.format("Method: %s, Protocol: %s, Hostname: %s, Port: %d, Path: %s",
							method, requestProtocol, hostName, requestPort, path) );
			}
			HttpHost host = new HttpHost(this.hostName, requestPort, requestProtocol);

			if ( debugThisRequest ) logRequest(Constants.LOGV);
			
			// Send request and get response 
			response = null;

			if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "Making HTTP request");
			try {
				response = httpClient.execute(host,request);
			}
			catch (ConnectionPoolTimeoutException e)		{
				Log.println(Constants.LOGI,TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
				Log.println(Constants.LOGI,TAG, "Retrying...");
				response = httpClient.execute(host,request);
			}
			if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "Finished HTTP request");

			this.responseHeaders = response.getAllHeaders();
			this.statusCode = response.getStatusLine().getStatusCode();

			HttpEntity entity = response.getEntity();
			down = (entity == null ? 0 : entity.getContentLength());
			
			long finish = System.currentTimeMillis();
			double timeTaken = ((double)(finish-start))/1000.0;

			if ( Constants.LOG_DEBUG || debugThisRequest )
				Log.println(Constants.LOGD,TAG, "Response: "+statusCode+", Sent: "+up+", Received: "+down+", Took: "+timeTaken+" seconds");
			
			if ( debugThisRequest ) {
				return logResponse(Constants.LOGV);
			}
			else if (entity != null) {
				if ( entity.getContentLength() > 0 ) return entity.getContent();

				// Kind of admitting defeat here, but I can't track down why we seem
				// to end up in never-never land if we just return entity.getContent()
				// directly when entity.getContentLength() is -1 ('unknown', apparently).
				// Horribly inefficient too.
				//
				// @todo: Check whether this problem was caused by failing to close the InputStream 
				// and this hack can be removed...  Need to find a server which does not send Content-Length headers.
				//
				String tmpEntity = entityToString(entity);
				return new ByteArrayInputStream( tmpEntity.getBytes() );
			}

		}
		catch (SSLProtocolException e) {
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			return null;
		}
		catch (SSLHandshakeException e)		{
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			throw e;
		}
		catch (SSLException e) {
			if ( debugThisRequest )
				Log.println(Constants.LOGD,TAG,Log.getStackTraceString(e));
			throw e;
		}
		catch (AuthenticationFailure e) {
			if ( debugThisRequest )
				Log.println(Constants.LOGD,TAG,Log.getStackTraceString(e));
			throw e;
		}
		catch (ConnectionPoolTimeoutException e)		{
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			throw e;
		}
		catch (SocketTimeoutException e)		{
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			return null;
		}
		catch (ConnectTimeoutException e)		{
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			return null;
		}
		catch ( UnknownHostException e ) {
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			return null;
		}
		catch ( IOException e ) {
			Log.i(TAG, e.getClass().getSimpleName() + ": " + e.getMessage() + " to " + fullUrl() );
			return null;
		}
		catch (Exception e) {
			Log.println(Constants.LOGD,TAG,Log.getStackTraceString(e));
			if ( statusCode < 300 || statusCode > 499 )
				throw new SendRequestFailedException(e.getMessage());
		}
		return null;
	}


	/**
	 * Do a new HTTP <method> request with these headers and entity (request body) against
	 * this path (or the current path, if null).  The headers & entity may also be null in
	 * some simple cases.
	 * 
	 * If the server requests Digest or Basic authentication a second request will be made
	 * supplying these (if possible).  Likewise the method will follow up to five redirects
	 * before giving up on a request.
	 * @param method
	 * @param pathOrUrl
	 * @param headers
	 * @param entity
	 * @return
	 * @throws SendRequestFailedException
	 * @throws SSLException
	 * @throws ConnectionFailedException 
	 */
	public InputStream doRequest( String method, String pathOrUrl, Header[] headers, String entity )
			throws SendRequestFailedException, SSLException, ConnectionFailedException {

		if ( Constants.LOG_DEBUG || debugThisRequest )
			Log.println(Constants.LOGD,TAG, String.format("%s request on %s", method, fullUrl()) );
		
		InputStream result = null;
		this.method = method;
		interpretUriString(pathOrUrl);
		try {
			result = sendRequest( headers, entity );
		}
		catch (SSLHandshakeException e) 		{ throw e; }
		catch (SSLException e) 					{ throw e; }
		catch (SendRequestFailedException e)	{ throw e; }
		catch (ConnectionFailedException e)		{ throw e; }
		catch (AuthenticationFailure e1) 		{ statusCode = 401; }
		catch (Exception e) {
			Log.e(TAG,Log.getStackTraceString(e));
		}

		if ( statusCode == 401 ) {
			// In this case we didn't send auth credentials the first time, so
			// we need to try again after we interpret the auth request.
			try {
				interpretRequestedAuth(getAuthHeader());
				return sendRequest( headers, entity );
			}
			catch (AuthenticationFailure e1) {
				throw new SendRequestFailedException("Authentication Failed: "+e1.getMessage());
			}
			catch (Exception e) {
				Log.e(TAG,Log.getStackTraceString(e));
			}
		}

		if ( (statusCode >= 300 && statusCode <= 303) || statusCode == 307 ) {
/**
 * Other than 301/302 these are all pretty unlikely
 *		300:  Multiple choices, but we take the one in the Location header anyway
 *		301:  Moved permanently
 *		302:  Found (was 'temporary redirect' once in prehistory)
 *		303:  See other
 *		307:  Temporary redirect. Meh.
 */
			String oldUrl = fullUrl();
			interpretUriString(getLocationHeader());
			if (debugThisRequest)
				Log.println(Constants.LOGD,TAG, method + " " +oldUrl+" redirected to: "+fullUrl());
			if ( redirectCount++ < redirectLimit ) {
				// Follow redirect
				return doRequest( method, null, headers, entity ); 
			}
		}

		return result;
	}

	
	/**
	 * <p>
	 * Does an XML request against the specified path (or the previously set path, if null),
	 * following redirects and returning the root DavNode of an XML tree.
	 * </p>
	 * 
	 * @return <p>
	 *         A DavNode which is the root of the multistatus response, or null if it couldn't be parsed.
	 *         </p>
	 * @throws SSLHandshakeException 
	 */
	public DavNode doXmlRequest( String method, String requestPath, Header[] headers, String xml) throws SSLHandshakeException {
		long start = System.currentTimeMillis();

		InputStream responseStream = null;
		DavNode root = null;
		try {
			responseStream = doRequest(method, requestPath, headers, xml);
			if ( responseHeaders == null ) {
				return root;
			}
			for( Header h : responseHeaders ) {
				if ( "Content-Type".equals(h.getName()) ) {
					for( HeaderElement he : h.getElements() ) {
						if ( "text/plain".equals(he.getName()) || "text/html".equals(he.getName()) ) {
							Log.println(Constants.LOGI, TAG, "Response is not an XML document");
							if ( responseStream != null ) responseStream.close();
							return root;
						}
					}
				}
			}
			if ( statusCode == 404 || statusCode == 401 ) {
				return root;
			}
			root = DavParserFactory.buildTreeFromXml(Constants.XMLParseMethod, responseStream );
		}
		catch (SSLHandshakeException e) 		{ throw e; }
		catch (Exception e) {
			Log.i(TAG, e.getMessage(), e);
			return null;
		}
		finally {
			if ( responseStream != null )
				try { responseStream.close(); } catch ( IOException e ) {}
		}
		
		if (debugThisRequest)
			Log.println(Constants.LOGV,TAG, "Request and parse completed in " + (System.currentTimeMillis() - start) + "ms");
		return root;
	}

	/**
	 * Get the current hostname used for the last request, or recently set.
	 * @return
	 */
	public String getHostName() {
		return this.hostName;
	}

	public void setHostName(String hostIn) {
		if ( hostIn == null ) throw new NullPointerException("May not set hostName to null");
		// This trick greatly reduces the occurrence of host not found errors. 
		try { InetAddress.getByName(hostIn); } catch (UnknownHostException e1) { }
		this.hostName = hostIn;
	}

	public int getPort() {
		return port;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getUserName() {
		return this.username;
	}

	public boolean protocolEquals(String otherProtocol) {
		if ( protocol == null && otherProtocol == null ) return true;
		else if ( protocol == null ) return false;

		return protocol.equals(otherProtocol);
	}
	
}
