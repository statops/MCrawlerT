package com.morphoss.acal.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.activity.CollectionConfigList;

/**
 * Authenticator service that returns a subclass of AbstractAccountAuthenticator in onBind()
 */
public class AcalAuthenticator extends Service {
	private static final String				TAG						= "AcalAuthenticator";
	private static StaticAuthenticatorImplementation	realAuthenticator	= null;

	public static final String SERVER_ID = "server_id";
	public static final String COLLECTION_ID = "collection_id";
	public static final String USERNAME = "username";
	
	
	public AcalAuthenticator() {
		super();
		Log.println(Constants.LOGD,TAG,"AcalAuthenticator was created");
	}

	public IBinder onBind(Intent intent) {
		IBinder ret = null;
		if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
			ret = getAuthenticator().getIBinder();
		Log.println(Constants.LOGD,TAG,"onBind was called");
		return ret;
	}

	private StaticAuthenticatorImplementation getAuthenticator() {
		if (realAuthenticator == null) realAuthenticator = new StaticAuthenticatorImplementation(this);
		Log.println(Constants.LOGD,TAG,"StaticAuthenticatorImplementation was created");
		return realAuthenticator;
	}

	private static class StaticAuthenticatorImplementation extends AbstractAccountAuthenticator {
		private Context	context;

		public StaticAuthenticatorImplementation(Context context) {
			super(context);
			this.context = context;
			Log.println(Constants.LOGD,TAG,"addAccount was called");
		}

		/*
		 * The user has requested to add a new account to the system. We return an intent that will launch our
		 * login screen if the user has not logged in yet, otherwise our activity will just pass the user's
		 * credentials on to the account manager.
		 */
		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
					String[] requiredFeatures, Bundle options) throws NetworkErrorException {

			Log.println(Constants.LOGD,TAG,"addAccount was called");
			final Intent i = new Intent(context, CollectionConfigList.class);
			i.setAction(com.morphoss.acal.activity.CollectionConfigList.ACTION_CHOOSE_ADDRESSBOOK);
			i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			final Bundle reply = new Bundle();
			reply.putParcelable(AccountManager.KEY_INTENT, i);

			return reply;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
			Log.println(Constants.LOGD,TAG,"confirmCredentials was called");
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
			Log.println(Constants.LOGD,TAG,"editProperties was called");
			return null;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
					Bundle options) throws NetworkErrorException {
			Log.println(Constants.LOGD,TAG,"getAuthToken was called");
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			Log.println(Constants.LOGD,TAG,"getAuthTokenLabel was called");
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
					throws NetworkErrorException {
			Log.println(Constants.LOGD,TAG,"hasFeatures was called");
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
					Bundle options) {
			Log.println(Constants.LOGD,TAG,"updateCredentials was called");
			return null;
		}
	}
}
