package com.morphoss.acal.activity.serverconfig;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;

import android.content.ContentValues;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.providers.Servers;
import com.morphoss.acal.service.SynchronisationJobs;
import com.morphoss.acal.service.connector.AcalRequestor;
import com.morphoss.acal.service.connector.SendRequestFailedException;
import com.morphoss.acal.xml.DavNode;

public class TestPort {
	private static final String TAG = "aCal TestPort";
	private static final String pPathRequestData = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
			"<propfind xmlns=\"DAV:\">"+
				"<prop>"+
					"<resourcetype/>"+
					"<current-user-principal/>"+
					"<principal-collection-set/>"+
					"<owner/>"+
				"</prop>"+
			"</propfind>";

	private static final String pPrincipalMatchSelf = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
			"<principal-match " +
					"xmlns=\""+Constants.NS_DAV+"\" " +
					"xmlns:C=\""+Constants.NS_CALDAV+"\" "+
					"xmlns:A=\""+Constants.NS_CARDDAV+"\">"+
				"<self/>"+
				"<prop>"+
					"<resourcetype/>"+
					"<C:calendar-home-set/>"+
					"<A:addressbook-home-set/>"+
				"</prop>"+
			"</principal-match>";

	private final AcalRequestor requestor;
	int port;
	boolean useSSL;
	private String hostName;
	private String path;
	int connectTimeOut;
	int socketTimeOut			= 3000;
	private Boolean isOpen 		= null;
	private Boolean authOK 		= null;
	private Boolean hasDAV		= null;
	private Boolean hasCalDAV 	= null;
	private Boolean hasPrincipalURL 	= null;

	public final static int NO_CONNECTION = 0;
	public final static int PORT_IS_CLOSED = 3;
	public final static int PORT_IS_OPEN = 5;
	public final static int SSL_FAILED = 7;
	public final static int NO_DAV_RESPONSE = 10;
	public final static int SERVER_SUPPORTS_DAV = 11;
	public final static int AUTH_FAILED = 16;
	public final static int AUTH_SUCCEEDED = 17;
	public final static int HAS_PRINCIPAL_URL = 20;
	public final static int HAS_CALDAV = 25;
	public final static int IS_CALENDAR = 30;
	
	private int achievement = NO_CONNECTION;
	private String calendarCollectionHref = null;

	/**
	 * Construct based on values from the AcalRequestor
	 * @param requestorIn
	 */
	public TestPort(AcalRequestor requestorIn) {
		this.requestor = requestorIn;
		this.path = requestor.getPath();
		this.hostName = requestor.getHostName();
		this.port = requestor.getPort();
		this.useSSL = requestor.protocolEquals(AcalRequestor.PROTOCOL_HTTPS);
		connectTimeOut = 500 + (useSSL ? 300 : 0);
	}


	/**
	 * Construct based on values from the AcalRequestor, but overriding port/SSL
	 * @param requestorIn
	 * @param port
	 * @param useSSL
	 */
	TestPort(AcalRequestor requestorIn, int port, boolean useSSL) {
		this(requestorIn);
		this.port = port;
		this.useSSL = useSSL;
	}

	
	/**
	 * <p>
	 * Test whether the port is open.
	 * </p> 
	 * @return
	 */
	boolean isOpen() {
		if ( this.isOpen == null ) {
			requestor.setTimeOuts(connectTimeOut,socketTimeOut);
			requestor.setPath(path);
			requestor.setHostName(hostName);
			requestor.setPortProtocol( port, (useSSL?1:0) );
			if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Checking port open "+requestor.protocolHostPort());
			this.isOpen = false;
			try {
				requestor.doRequest("HEAD", null, null, null);
				if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Probe "+requestor.fullUrl()+" success: status " + requestor.getStatusCode());

				// No exception, so it worked!
				this.isOpen = true;
				if ( requestor.getStatusCode() == 401 ) {
					this.authOK = false;
					setAchievement(AUTH_FAILED);
				}
				checkCalendarAccess(requestor.getResponseHeaders());

				this.socketTimeOut = 15000;
				this.connectTimeOut = 15000;
				requestor.setTimeOuts(connectTimeOut,socketTimeOut);
			}
			catch (SSLHandshakeException e) {
				setAchievement(SSL_FAILED);
			}
			catch (Exception e) {
				if ( Constants.debugCheckServerDialog )
					Log.println(Constants.LOGD, TAG, "Probe "+requestor.fullUrl()+" failed: " + e.getMessage());
			}
			setAchievement( this.isOpen ? PORT_IS_OPEN : PORT_IS_CLOSED);
		}
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Port "+(isOpen ?"":"not")+" open on "+requestor.protocolHostPort() );
		return this.isOpen;
	}


	/**
	 * We can only increase our achievement level
	 * @param thisAchievement
	 */
	private void setAchievement(int thisAchievement) {
		if ( thisAchievement > achievement ) achievement = thisAchievement;
	}


	/**
	 * Increases the connection timeout and attempts another probe.
	 * @return
	 */
	boolean reProbe() {
		connectTimeOut += 1000;
		connectTimeOut *= 2;
		if ( isOpen != null && !isOpen ) isOpen = null;
		return isOpen();
	}


	/**
	 * <p>
	 * Checks whether the calendar supports CalDAV by looking through the headers for a "DAV:" header which
	 * includes "calendar-access". Appends to the successMessage we will return to the user, as well as
	 * setting the hasCalendarAccess for later update to the DB.
	 * </p>
	 * 
	 * @param headers
	 * @return true if the calendar does support CalDAV.
	 */
	private boolean checkCalendarAccess(Header[] headers) {
		if ( headers != null ) {
			for (Header h : headers) {
				if (h.getName().equalsIgnoreCase("DAV")) {
					if (h.getValue().toLowerCase().contains("calendar-access")) {
						if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG,
								"Discovered server supports CalDAV on URL "+requestor.fullUrl());
						hasCalDAV = true;
						hasDAV = true; // by implication
						return true;
					}
				}
			}
		}
		return false;
	}


	/**
	 * Does a PROPFIND request on the given path.
	 * @param requestPath
	 * @return
	 */
	private boolean doPropfindPrincipal( String requestPath ) {
		if ( requestPath != null ) requestor.setPath(requestPath);
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG,
				"Doing PROPFIND for current-user-principal on " + requestor.fullUrl() );
		try {
			requestor.setAuthRequired();
			DavNode root = requestor.doXmlRequest("PROPFIND", null, SynchronisationJobs.getReportHeaders(0), pPathRequestData);
			
			int status = requestor.getStatusCode();
			if ( Constants.debugCheckServerDialog )
				Log.println(Constants.LOGD,TAG, "PROPFIND request " + status + " on " + requestor.fullUrl() );

			checkCalendarAccess(requestor.getResponseHeaders());

			if ( status == 401 ) {
				authOK = false;
				setAchievement(AUTH_FAILED);
			}
			else if ( status == 207 ) {
				if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Checking for principal path in response...");
				List<DavNode> unAuthenticated = root.getNodesFromPath("multistatus/response/propstat/prop/current-user-principal/unauthenticated");
				if ( ! unAuthenticated.isEmpty() ) {
					if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Found unauthenticated principal");
					requestor.setAuthRequired();
					if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "We are unauthenticated, so try forcing authentication on");
					if ( requestor.getAuthType() == Servers.AUTH_NONE ) {
						requestor.setAuthType(Servers.AUTH_BASIC);
						if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Guessing Basic Authentication");
					}
					else if ( requestor.getAuthType() == Servers.AUTH_BASIC ) {
						requestor.setAuthType(Servers.AUTH_DIGEST);
						if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Guessing Digest Authentication");
					}
					return doPropfindPrincipal(requestPath);
				}
				
				authOK = true;
				setAchievement(AUTH_SUCCEEDED);

				calendarCollectionHref  = null;
				for ( DavNode response : root.getNodesFromPath("multistatus/response") ) {
					String responseHref = response.getFirstNodeText("href"); 
					if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Checking response for "+responseHref);
					for ( DavNode propStat : response.getNodesFromPath("propstat") ) {
						if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Checking in propstat for "+responseHref);
						if ( propStat.getFirstNodeText("status").equalsIgnoreCase("HTTP/1.1 200 OK") ) {
							if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Found propstat 200 OK response for "+responseHref);
							if ( !propStat.getNodesFromPath("prop/resourcetype/principal").isEmpty() ) {
								if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "This is a principal URL :-)");
								requestor.interpretUriString(responseHref);
								setFieldsFromRequestor();
								hasPrincipalURL = true;
								return true;
							}
							String href = null;
							if ( !propStat.getNodesFromPath("prop/resourcetype/calendar").isEmpty() ) {
								if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "This is a calendar URL :-|");
								href = propStat.getFirstNodeText("prop/owner/href");
								if ( href != null ) {
									requestor.interpretUriString(href);
									setFieldsFromRequestor();
									hasPrincipalURL = true;
									hasCalDAV = true;
									achievement = IS_CALENDAR;
									calendarCollectionHref = responseHref;
								}
								return true;
							}
							href = propStat.getFirstNodeText("prop/current-user-principal/href");
							if ( href != null && !href.equals("") ) {
								if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Found the current-user-principal URL :-) at '"+href+"'");
								requestor.interpretUriString(href);
								setFieldsFromRequestor();
								hasPrincipalURL = true;
								return true;
							}
							
							href = propStat.getFirstNodeText("prop/principal-collection-set/href");
							if ( href != null && !href.equals("") ) {
								String principalCollectionHref = href;
								if ( doPrincipalMatchSelf(href) ) {
									return true;
								}
								String userName = URLEncoder.encode(requestor.getUserName(), "UTF-8");
								if ( principalCollectionHref.length() > 0 && userName != null ) {
									principalCollectionHref = principalCollectionHref + 
											(principalCollectionHref.length() > 0 && principalCollectionHref.charAt(principalCollectionHref.length()-1) == '/' ? "" : "/") +
											userName + "/";
									if ( !principalCollectionHref.equals(requestPath) ) {
										return doPropfindPrincipal(principalCollectionHref);
									}
									if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG,
											"We've tried this URL already.  Let's move on... "+requestPath);
								}
								// @todo Next: Try a Depth: 1 propfind on the principalCollectionHref trying to match it to this user
							}
						}
					}
				}
			}
		}
		catch (SSLHandshakeException e) {
			setAchievement(SSL_FAILED);
		}
		catch (Exception e) {
			Log.e(TAG, "PROPFIND Error: " + e.getMessage());
			Log.println(Constants.LOGD,TAG, Log.getStackTraceString(e));
		}
		return false;
	}

	
	/**
	 * Does a principal-match REPORT on the given path.
	 * @param requestPath
	 * @return
	 */
	private boolean doPrincipalMatchSelf( String requestPath ) {
		if ( requestPath != null ) requestor.setPath(requestPath);
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG,
				"Doing PROPFIND for current-user-principal on " + requestor.fullUrl() );
		try {
			requestor.setAuthRequired();
			DavNode root = requestor.doXmlRequest("REPORT", null, SynchronisationJobs.getReportHeaders(0), pPrincipalMatchSelf);
			
			int status = requestor.getStatusCode();
			if ( Constants.debugCheckServerDialog )
				Log.println(Constants.LOGD,TAG, "PROPFIND request " + status + " on " + requestor.fullUrl() );

			checkCalendarAccess(requestor.getResponseHeaders());

			if ( status == 401 ) {
				authOK = false;
				setAchievement(AUTH_FAILED);
			}
			else if ( status == 207 ) {
				if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Checking for principal path in response...");
				
				for ( DavNode response : root.getNodesFromPath("multistatus/response") ) {
					String responseHref = response.getFirstNodeText("href"); 
					if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Checking response for "+responseHref);
					for ( DavNode propStat : response.getNodesFromPath("propstat") ) {
						if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Checking in propstat for "+responseHref);
						if ( propStat.getFirstNodeText("status").equalsIgnoreCase("HTTP/1.1 200 OK") ) {
							if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Found propstat 200 OK response for "+responseHref);
							if ( !propStat.getNodesFromPath("prop/resourcetype/principal").isEmpty() ) {
								if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD, TAG, "Got a principal URL from principal-match REPORT :-)");
								requestor.interpretUriString(responseHref);
								setFieldsFromRequestor();
								hasPrincipalURL = true;
								return true;
							}
						}
					}
				}
			}
		}
		catch (SSLHandshakeException e) {
			setAchievement(SSL_FAILED);
		}
		catch (Exception e) {
			Log.e(TAG, "PROPFIND Error: " + e.getMessage());
			Log.println(Constants.LOGD,TAG, Log.getStackTraceString(e));
		}
		return false;
	}

	
	private void setFieldsFromRequestor() {
		useSSL = requestor.getProtocol().equals("https");
		hostName = requestor.getHostName();
		path = requestor.getPath();
		port = requestor.getPort();
	}


	/**
	 * Probes for whether the server has DAV support.  It seems odd to use the PROPFIND
	 * for this, rather than OPTIONS which was intended for the purpose, but every working
	 * DAV server will support PROPFIND on every URL which supports DAV, whereas OPTIONS
	 * may only be available on some specific URLs in weird cases.
	 */
	boolean hasDAV() {
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "Starting DAV discovery on "+requestor.fullUrl());
		if ( !isOpen() ) return false;
		if ( hasDAV == null ) {
			hasDAV = false;
			if ( doPropfindPrincipal(this.path) ) 								hasDAV = true;
			else if ( !hasDAV && doPropfindPrincipal("/.well-known/caldav") )	hasDAV = true;
			else if ( !hasDAV && doPropfindPrincipal("/") )						hasDAV = true;
			setAchievement( this.hasDAV ? NO_DAV_RESPONSE : SERVER_SUPPORTS_DAV);
		}
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGD,TAG, "DAV "+(hasDAV?"":"not")+" found on "+requestor.fullUrl());
		return hasDAV;
	}

	
	/**
	 * Probes for CalDAV support on the server using previous path used for DAV.
	 */
	boolean hasCalDAV() {
		requestor.setTimeOuts(connectTimeOut,socketTimeOut);
		requestor.setPath(path);
		requestor.setHostName(hostName);
		requestor.setPortProtocol( port, (useSSL?1:0) );

		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG,
				"Starting CalDAV dependency discovery on "+requestor.fullUrl());
		if ( !isOpen() || !hasDAV() || !authOK() || !hasPrincipalUrl() ) return false;

		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG, "All CalDAV dependencies are present.");
		if ( hasCalDAV == null ) {
			if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG, "Still discovering actual CalDAV support.");
			hasCalDAV = false;
			try {
				path = requestor.getPath();
				if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG, "Starting OPTIONS on "+path);
				requestor.doRequest("OPTIONS", path, null, null);
				int status = requestor.getStatusCode();
				if ( Constants.debugCheckServerDialog )
					Log.println(Constants.LOGD,TAG, "OPTIONS request " + status + " on " + requestor.fullUrl() );
				checkCalendarAccess(requestor.getResponseHeaders());  // Updates 'hasCalDAV' if it finds it
			}
			catch (SSLHandshakeException e) {
				setAchievement(SSL_FAILED);
			}
			catch (SendRequestFailedException e) {
				Log.println(Constants.LOGD,TAG, "OPTIONS Error connecting to server: " + e.getMessage());
			}
			catch (Exception e) {
				Log.e(TAG,"OPTIONS Error: " + e.getMessage());
				if ( Constants.debugCheckServerDialog )
					Log.println(Constants.LOGD,TAG,Log.getStackTraceString(e));
			}
			if ( this.hasCalDAV ) setAchievement(HAS_CALDAV);
		}
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG,
				"CalDAV "+(hasCalDAV?"":"not")+" found on "+requestor.fullUrl());
		return hasCalDAV;
	}


	/**
	 * Return whether the auth was OK.  If nothing's managed to tell us it failed
	 * then we give it the benefit of the doubt.
	 * @return
	 */
	public boolean authOK() {
		if ( Constants.debugCheckServerDialog ) Log.println(Constants.LOGI,TAG,
				"Checking authOK which was: "+(authOK == null ? "uncertain, assumed OK" : (authOK ? "OK" : "bad")));
		return (authOK == null || authOK ? true : false);
	}


	/**
	 * Return whether or not we have a principal URL
	 */
	public boolean hasPrincipalUrl() {
		if ( hasPrincipalURL == null ) {
			doPropfindPrincipal(path);
			if ( hasPrincipalURL == null ) hasPrincipalURL = true; 
			
		}
		return hasPrincipalURL;
	}
	
	/**
	 * Returns a default ArrayList<TestPort> which can be used for probing a server to try
	 * and discover where the CalDAV / CardDAV server is hiding.  
	 * @param requestor The requestor which will be used for probing.
	 * @return The ArrayList of default ports.
	 */
	private static ArrayList<TestPort> testPortSet = null;
	public static Iterator<TestPort> defaultIterator(AcalRequestor requestor ) {
		if ( testPortSet == null )
			testPortSet = new ArrayList<TestPort>(10);
		else
			testPortSet.clear();

		// In the below we check https if they *didn't* specify http, and vice-versa.  This logic means that 
		// if they didn't specify *either* then we check *both*.
		if ( requestor.getPort() != -1 && requestor.getPort() != 80 && requestor.getPort() != 443 ) {
			if ( ! requestor.protocolEquals("http") )
				testPortSet.add( new TestPort(requestor,requestor.getPort(),true) );
			if ( ! requestor.protocolEquals("https") )
				testPortSet.add( new TestPort(requestor,requestor.getPort(),false) );
		}
		else {
			if ( ! requestor.protocolEquals("http") ) {
				testPortSet.add( new TestPort(requestor,443,true) );
				testPortSet.add( new TestPort(requestor,8443,true) );
			}
			if ( ! requestor.protocolEquals("https") ) {
				testPortSet.add( new TestPort(requestor,80,false) );
				testPortSet.add( new TestPort(requestor,8008,false) );
			}
			if ( ! requestor.protocolEquals("http") ) {
				testPortSet.add( new TestPort(requestor,8843,true) );
//				testPortSet.add( new TestPort(requestor,4443,true) );
				testPortSet.add( new TestPort(requestor,8043,true) );
			}
//			testPortSet.add( new TestPort(requestor,8800,false) );
//			testPortSet.add( new TestPort(requestor,8888,false) );
//			testPortSet.add( new TestPort(requestor,7777,false) );
		}
		return testPortSet.iterator();
	}

	public static Iterator<TestPort> reIterate() {
		if ( testPortSet == null ) return null;
		return testPortSet.iterator();
	}


	/**
	 * Using the dnsjava library, do a lookup for the SRV record for
	 * the user's domain and then process these in priority order to insert
	 * them into the start of the testPortSet.
	 * @param requestor
	 * @return
	 */
	public static boolean addSrvLookups(AcalRequestor requestor) {
		
		return false;

/*
 * The following code is tested, and works, but requires the DNSJava library.		
 */
/** /		
		Name baseDomain = null;
		Name sslPrefix = null;
		Name plainPrefix = null;
		try {
			baseDomain = new Name(requestor.getHostName());
			sslPrefix = new Name("_caldavs._tcp");
			plainPrefix = new Name("_caldav._tcp");
		}
		catch ( TextParseException e1 ) {
			Log.w(TAG,"Auto-generated catch block", e1);
			return false;
		}

		Name[] searchNames;
		try {
			searchNames = new Name[] {
					Name.concatenate(sslPrefix, baseDomain),
					Name.concatenate(sslPrefix, new Name(baseDomain,1)),
					Name.concatenate(plainPrefix, baseDomain),
					Name.concatenate(plainPrefix, new Name(baseDomain,1)),
			};
		}
		catch ( NameTooLongException e ) {
			Log.w(TAG,"Auto-generated catch block", e);
			return false;
		}
		
		boolean addedSome = false;
		SimpleResolver resolver;
		try {
			resolver = new SimpleResolver( ResolverConfig.getCurrentConfig().server() );
		}
		catch ( UnknownHostException e1 ) {
			// @todo Auto-generated catch block
			Log.w(TAG,"Auto-generated catch block", e1);
			return false;
		}
		resolver.setTimeout(5); // Set a short timeout

		Lookup dnsLookup = null;
		int inserted = 0;
		for( int n=0; n<searchNames.length; n++ ) {
			dnsLookup = new Lookup( searchNames[n], Type.SRV);
			dnsLookup.setResolver(resolver);
			Record[] answers = dnsLookup.run();
			if ( answers != null ) {
				for( int i=0; answers != null && i<answers.length; i++ ) {
					SRVRecord srv = (SRVRecord) answers[i];
					TestPort tp = new TestPort(requestor);
					tp.hostName = srv.getTarget().toString();
					tp.port = srv.getPort();
					tp.path = "/.well-known/caldav";
					tp.useSSL = (n < 2);   // The first two are SSL, the second two are not
					testPortSet.add( inserted + i, tp );
					addedSome = true;
					
					if ( Constants.debugCheckServerDialog ) 
						Log.println(Constants.LOGI, TAG,
							String.format("Got SRV response of '%s:%d' for %s query.", tp.hostName, tp.port, searchNames[n].toString()) );
				}
				inserted += answers.length;
			}
			else {
				if ( Constants.debugCheckServerDialog ) 
					Log.println(Constants.LOGI, TAG,
						String.format("No SRV response for %s query.", searchNames[n].toString()) );
			}
		}
		return addedSome;
/**/
	}

	
	/**
	 * Return a URL Prefix like 'https://'
	 * @return
	 */
	public String getProtocolUrlPrefix() {
		return "http" + (useSSL?"s":"") + "://";
	}


	public int getAchievement() {
		return achievement;
	}


	public String getCalendarPath() {
		return calendarCollectionHref;
	}


	public void applyToServerSettings(ContentValues serverData) {
		requestor.setPath(path);
		requestor.setHostName(hostName);
		requestor.setPortProtocol( port, (useSSL?1:0) );
		requestor.applyToServerSettings(serverData);
	}
	
}
