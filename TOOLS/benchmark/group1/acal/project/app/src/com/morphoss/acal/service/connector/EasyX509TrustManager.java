package com.morphoss.acal.service.connector;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

import com.morphoss.acal.AcalApplication;
import com.morphoss.acal.Constants;
import com.morphoss.acal.PrefNames;

/**
 * @author olamy
 * @version $Id: EasyX509TrustManager.java 765355 2009-04-15 20:59:07Z evenisse $
 * @since 1.2.3
 */
public class EasyX509TrustManager implements X509TrustManager {

	final static String TAG = "aCal EasyX509TrustManager";
	private X509TrustManager standardTrustManager = null;

	/**
	 * Constructor for EasyX509TrustManager.
	 */
	public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
		super();
		TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		factory.init(keystore);
		TrustManager[] trustmanagers = factory.getTrustManagers();
		if (trustmanagers.length == 0) {
			throw new NoSuchAlgorithmException("no trust manager found");
		}
		this.standardTrustManager = (X509TrustManager) trustmanagers[0];
	}

	/**
	 * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],String authType)
	 */
	public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
		standardTrustManager.checkClientTrusted(certificates, authType);
	}

	/**
	 * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],String authType)
	 */
	public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
		if ( (certificates == null) || (certificates.length < 1) ) {
			throw new CertificateException("Certificate is null!!!");
		}
		else {
			try {
				standardTrustManager.checkServerTrusted(certificates, authType);
			}
			catch( CertificateExpiredException ce ) {
				logCertificateException("CertificateExpiredException", ce, certificates );
			}
			catch( CertificateNotYetValidException ce ) {
				logCertificateException("CertificateNotYetValidException", ce, certificates );
			}
			catch( CertificateException e ) {
				if ( Constants.LOG_DEBUG ) {
					Log.println(Constants.LOGI, TAG, e.getClass().getSimpleName() + " checking certificate.");
					logCertificateException(e.getClass().getSimpleName(), e, certificates );
					Log.println(Constants.LOGD,TAG,"Checking validity as if it were a self-signed certificate..." );
				}
				if ( checkLocallyApprovedCertificates(certificates) ) return;
				
				int i=0;
				try {
					for( ; i < certificates.length; i++ ) {
						certificates[i].checkValidity();
					}
					if ( AcalApplication.getPreferenceBoolean(PrefNames.allowSelfSignedCerts, true) ) {
						if ( Constants.LOG_DEBUG )
							Log.println(Constants.LOGI,TAG,"Allowing self-signed certificate." );
						return;
					}
				}
				catch( CertificateExpiredException ce ) {
					logCertificateException("CertificateExpiredException", ce, certificates );
				}
				catch( CertificateNotYetValidException ce ) {
					logCertificateException("CertificateNotYetValidException", ce, certificates );
				}
				catch( Exception ee ) {
					String[] unApprovedCertificates = AcalApplication.getPreferenceString(PrefNames.unapprovedCertificates, "").split(",");
					String certificate = certificates[i].getEncoded().toString();
					int j=0;
					for( ; j<unApprovedCertificates.length && !unApprovedCertificates[j].equals(certificate); j++);
					if ( j == unApprovedCertificates.length ) {
						String newCertificates = AcalApplication.getPreferenceString(PrefNames.unapprovedCertificates, "")+","+certificate;
						AcalApplication.setPreferenceString(PrefNames.unapprovedCertificates, newCertificates);
					}
				}
				throw e;
			}
		}
	}

	private boolean checkLocallyApprovedCertificates(X509Certificate[] certificates) {
		String[] approvedCertificates = AcalApplication.getPreferenceString(PrefNames.approvedCertificates, "").split(",");
		for( int i=0; i < certificates.length; i++ ) {
			String certSig = Base64Coder.encode(certificates[i].getSignature()).toString();
			for( int j=0; j<approvedCertificates.length; j++) {
				if ( certSig.equals(approvedCertificates[j]) ) return true;
			}
		}
		return false;
	}

	private void logCertificateException(String string, Exception ce, X509Certificate[] certificates) {
		Log.w(TAG, string + ": " + ce.getMessage() );
		for( int i=0; i < certificates.length; i++ ) {
			Log.w(TAG,"Certificate for: " + certificates[i].getSubjectDN() );
			Log.w(TAG,"      issued by: " + certificates[i].getIssuerDN() );
			Log.w(TAG,"     valid from: " + certificates[i].getNotBefore() + ", to: " + certificates[0].getNotAfter() );
		}
	}

	/**
	 * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
	 */
	public X509Certificate[] getAcceptedIssuers() {
		return this.standardTrustManager.getAcceptedIssuers();
	}

}
