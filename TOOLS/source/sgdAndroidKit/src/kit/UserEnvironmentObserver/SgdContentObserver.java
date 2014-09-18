package kit.UserEnvironmentObserver;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import kit.Config.Config;
import kit.Utils.SgUtils;
import android.annotation.SuppressLint;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class SgdContentObserver extends ContentObserver {

	private Uri mUri;
	private Uri mUriLocation;
	protected String mFileName;
	private static String mReportDirectory = Environment
			.getExternalStorageDirectory()
			+ File.separator
			+ "SgdObservationReport";
	private IUserEnvironmentObserver mSgdTestCase;

	public SgdContentObserver(Handler handler, Uri handledUri,
			IUserEnvironmentObserver sgdTestCase) {
		super(handler);
		mSgdTestCase = sgdTestCase;
		if (Config.DEBUG) {
			Log.e(TAG, "User environment change will be notified to :  "
					+ mSgdTestCase);
		}
		mUri = handledUri;
		mFileName = mUri.getAuthority();
		if (!new File(mReportDirectory).exists()) {
			new File(mReportDirectory).mkdir();
		}
		if (!uriReportFile().exists()) {
			try {
				uriReportFile().createNewFile();
			} catch (IOException e) {
				// e.printStackTrace();
				if (Config.DEBUG) {
					Log.e(TAG, "File is not created ");
				}
			}
		}
	}

	private final static String TAG = SgdContentObserver.class.getSimpleName();

	@Override
	public boolean deliverSelfNotifications() {
		return false;
	}

	@SuppressLint("NewApi")
	@Override
	public void onChange(boolean selfChange, Uri uri) {
		onChange(selfChange);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.database.ContentObserver#onChange(boolean)
	 */
	@Override
	public void onChange(boolean selfChange) {
		Log.e(TAG,
				"====================onChange=========================================");
		if (Config.DEBUG) {
			Log.i(TAG, "URI changed " + "change has been performed");
			Log.i(TAG, "URI Value " + mUri.toString());
			Log.i(TAG, "mSgdTestCase " + mSgdTestCase);

		}
		if (mSgdTestCase != null)
			mSgdTestCase.userEvironmentOnChange(mUri.toString());
		save(uriReportFormat(mUri), uriReportFile());
	}

	private File uriReportFile() {
		return new File(mReportDirectory + File.separator + mFileName);
	}

	private String uriReportFormat(Uri uri) {
		return uri.toString()
				+ "   "
				+ java.text.DateFormat.getDateTimeInstance().format(
						Calendar.getInstance().getTime()) + "\n";
	}

	private static void save(String value, File file) {
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			SgUtils.save(value, file);
		} catch (IOException e) {
			Log.e(" URI ", " file not saved ");
		}

	}

	public Uri getObservableUri() {
		return mUri;
	}

	public Uri getObservableUriId() {
		return mUriLocation;
	}

	public void updateCurrentUriValue(Uri uri) {
		mUriLocation = uri;
	}
}
