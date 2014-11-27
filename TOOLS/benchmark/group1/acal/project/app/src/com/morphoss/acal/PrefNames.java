package com.morphoss.acal;

public class PrefNames {

	public static final String	lastRevision = AcalApplication.getResourceString(R.string.prefLastRevision);
	public static final String	serverIsConfigured	= AcalApplication.getResourceString(R.string.prefServerIsConfigured);

	// Certificate Signing preferences
	public static final String	approvedCertificates	= AcalApplication.getResourceString(R.string.prefApprovedCertificates);
	public static final String	unapprovedCertificates	= AcalApplication.getResourceString(R.string.prefUnapprovedCertificates); 
	public static final String	allowSelfSignedCerts	= AcalApplication.getResourceString(R.string.prefAllowSelfSignedCerts);
	public static final String	tzServerBaseUrl			= AcalApplication.getResourceString(R.string.prefTzServerBaseUrl);

	public static final String	ignoreValarmDescription = AcalApplication.getResourceString(R.string.prefIgnoreValarmDescription);

	public static final String	defaultEventsCollection	= AcalApplication.getResourceString(R.string.prefDefaultEventsCollection);
	public static final String	defaultTasksCollection	= AcalApplication.getResourceString(R.string.prefDefaultTasksCollection);
	public static final String	defaultNotesCollection	= AcalApplication.getResourceString(R.string.prefDefaultNotesCollection);
}
