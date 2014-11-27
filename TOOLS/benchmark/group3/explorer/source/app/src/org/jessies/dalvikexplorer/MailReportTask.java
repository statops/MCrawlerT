package org.jessies.dalvikexplorer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

public class MailReportTask extends AsyncTask<Void, Void, String> implements DialogInterface.OnCancelListener {
    private final Activity activity;
    private final ProgressDialog progressDialog;
    
    public MailReportTask(Activity activity) {
        this.activity = activity;
        this.progressDialog = ProgressDialog.show(activity, "Generating Report", "Please wait. This can take tens of seconds...", true, true, this);
    }
    
    @Override protected String doInBackground(Void... unused) {
        return generateReport();
    }
    
    @Override protected void onPostExecute(String report) {
        progressDialog.dismiss();
        if (isCancelled()) {
            return;
        }
                
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Dalvik Explorer " + Utils.appVersion(activity) + " report");
        intent.putExtra(Intent.EXTRA_TEXT, report); 
        activity.startActivity(intent);
    }
    
    /**
     * Invoked if the user presses "back" while our progress dialog is up.
     */
    @Override public void onCancel(DialogInterface dialog) {
        cancel(true);
    }
    
    private String generateReport() {
        StringBuilder result = new StringBuilder();
        //appendReport(result, "Build/Device Details", BuildActivity.getBuildDetailsAsString(activity, activity.getWindowManager()));
        appendReport(result, "Charsets", CharsetsActivity.describeCharsets());
        appendReport(result, "Environment Variables", EnvironmentVariablesActivity.getEnvironmentAsString());
        // FIXME: this doesn't scale well.
        //appendReport(result, "Locales", LocalesActivity.describeLocales());
        appendReport(result, "System Properties", SystemPropertiesActivity.getSystemPropertiesAsString());
        // FIXME: this doesn't scale well.
        //appendReport(result, "Time Zones", TimeZonesActivity.describeTimeZones());
        return result.toString();
    }
    
    private void appendReport(StringBuilder report, String title, String details) {
        report.append(title + "\n");
        for (int i = 0; i < 76; ++i) {
            report.append('=');
        }
        report.append("\n\n");
        report.append(details);
        report.append("\n\n\n\n");
    }
}
