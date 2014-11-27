package com.morphoss.acal.service.connector;

import org.apache.http.HttpVersion;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.os.Build;

import com.morphoss.acal.service.aCalService;

public class AcalConnectionPool {
	
	public static final int	DEFAULT_BUFFER_SIZE	= 4096;
	
	private static HttpParams httpParams = null;
	private static SchemeRegistry schemeRegistry = null;
	private static ThreadSafeClientConnManager connectionPool = null;
	
	private static String userAgent = null; 
	
	public static HttpParams defaultHttpParams(int socketTimeOut, int connectionTimeOut) {
		if ( httpParams == null ) {
			httpParams = new BasicHttpParams();
			httpParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			httpParams.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
			httpParams.setParameter(CoreProtocolPNames.USER_AGENT, getUserAgent() );
			httpParams.setParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,DEFAULT_BUFFER_SIZE);
			httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);

			httpParams.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);

			setTimeOuts(socketTimeOut,connectionTimeOut);
	
			// We need to set the MaxConnectionsPerRoute to a higher value so that we
			// don't get an inexplicable timeout on the third attempt.  We set this pretty
			// high to discourage weird timeouts when we're going through the discovery.
			// All credit to:
			//   http://androidisland.blogspot.com/2010/11/httpclient-and-connectionpooltimeoutexc.html
			// for the fix.
			//
			// 2011-11-23 Reduced from 100 to 10 in case this is causing our memory leakage.
			//
			ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRoute() {
			    @Override
			    public int getMaxForRoute(HttpRoute httproute)
			    {
			        return 1000;
			    }
			});
	
			ConnManagerParams.setTimeout(httpParams, 5000);
		}
		return httpParams;
	}


	public static ThreadSafeClientConnManager getHttpConnectionPool() {
		if ( connectionPool == null ) {
			schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			Scheme httpsScheme = new Scheme("https",  new EasySSLSocketFactory(), 443);
			schemeRegistry.register(httpsScheme);
			connectionPool = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
		}
		else
			connectionPool.closeExpiredConnections();
		return connectionPool;
	}


	public static void setTimeOuts(int socketTimeOut, int connectionTimeOut) {
		httpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeOut);
		httpParams.setIntParameter(CoreConnectionPNames.SO_LINGER, socketTimeOut);
		httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeOut);
		httpParams.setLongParameter(ConnManagerPNames.TIMEOUT, connectionTimeOut + 1000 ); 	
	}

	public static String getUserAgent() {
		if ( userAgent == null ) {
			userAgent = aCalService.aCalVersion;
	
			// User-Agent: aCal/0.3 (google; Nexus One; passion; HTC; passion; FRG83D)  Android/2.2.1 (75603)
			userAgent += " (" + Build.BRAND + "; " + Build.MODEL + "; " + Build.PRODUCT + "; "
						+ Build.MANUFACTURER + "; " + Build.DEVICE + "; " + Build.DISPLAY + "; " + Build.BOARD + ") "
						+ " Android/" + Build.VERSION.RELEASE + " (" + Build.VERSION.INCREMENTAL + ")";
		}
		return userAgent;
	}

}
