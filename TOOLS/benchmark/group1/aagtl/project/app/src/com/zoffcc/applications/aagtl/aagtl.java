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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.widget.NumberPicker;

public class aagtl extends Activity implements LocationListener, GpsStatus.Listener, SensorEventListener
// ,OnClickListener
{

	static final boolean EMULATOR = false;

	static final boolean _RELEASE_ = true;
	boolean __ZOFF_PHONE__ = false;

	// OSD buttons/icons -------- START -----------
	public Bitmap follow_on = null;
	public Bitmap follow_off = null;
	public Bitmap follow_current = null;
	public Boolean follow_mode = true;
	public RectF follow_button_rect = new RectF(-100, 1, 1, 1);

	public Bitmap arrow_button = null;
	public RectF arrow_button_rect = new RectF(-100, 1, 1, 1);
	// OSD buttons/icons -------- END -----------

	Button btn_ok = null;
	Button btn_cancel = null;
	ToggleButton orient_1_toggle = null;
	NumberPicker xx1 = null;
	NumberPicker xx2 = null;
	NumberPicker xx3 = null;
	Boolean is_lat_current_change_target_coords = true;

	// Instantiating the Handler associated with the main thread.
	private Handler global_messageHandler = new Handler();

	ProgressThread progressThread;
	ProgressDialog pbarDialog;

	public String main_dir = "/sdcard/zoffcc/applications/aagtl";
	public String main_data_dir = "/sdcard/external_sd/zoffcc/applications/aagtl";

	public String status_text_string = "";
	public String status_append_string = "";

	Location mLastLocation = null;
	long mLastLocationMillis = -1;
	Boolean isGPSFix = false;
	int used_sats = 0;

	String main_language = null;
	String sub_language = null;
	DisplayMetrics metrics;
	WakeLock wl = null;

	List<GeocacheCoordinate> downloaded_caches = null;

	public LocationManager lm = null;
	public LocationProvider low = null;
	public LocationProvider high = null;
	public SensorManager sensorManager = null;
	public long last_heading_update = 0L;

	static final int DISPLAY_VIEW_MAP = 1;
	static final int DISPLAY_VIEW_GC = 2;
	static final int DISPLAY_VIEW_ARROW = 3;

	private Boolean show_no_loc_warning = false;

	public int current_display_view = DISPLAY_VIEW_MAP;
	static int TOUCH_CACHES_AFTER_THIS_ZOOM_LEVEL = 16;

	public CharSequence[] route_file_items = null;
	public String current_routefile_name = null;
	public List<GeocacheCoordinate> route_file_caches = null;
	public AlertDialog alert = null;

	public Rose rose;
	public CrossHair cross;
	public GeocachesView gcview;
	public ArrowView arrowview;
	public GCacheView cacheview;
	public RelativeLayout mainscreen_map_view;
	public int mainscreen_map_view_statusbar_height = 30;
	public RelativeLayout.LayoutParams layoutParams_mainscreen_map_view;
	public TextView status_text;
	public WebView wv;

	public double cur_lat_gps_save = -1;
	public double cur_lon_gps_save = -1;

	public String db_path;

	MapDownloader mdl;
	HTMLDownloader wdl;
	PointProvider pv;
	CacheDownloader cdol;

	public static class settings implements Serializable
	{
		private static final long serialVersionUID = 171513126339437464L;
		public Boolean download_visible = true;
		public Boolean download_notfound = true;
		public Boolean download_new = true;
		public Boolean download_nothing = false;
		public Boolean download_create_index = true;
		public Boolean download_run_after = false;
		// download_run_after_string --> is now used as a generic config
		// container!!
		public String download_run_after_string = "CFG:v2.0:";
		//
		public String download_output_dir = "/sdcard/external_sd/zoffcc/applications/aagtl";
		public double map_position_lat = 48.23428;
		public double map_position_lon = 16.391514;
		public int map_zoom = 15;
		public int map_type = MapDownloader.MAP_OSM;
		public Boolean download_resize = true;
		public int download_resize_pixel = 400;
		public Boolean options_show_name = true;
		public String options_username = "Username";
		public String options_password = "Password";
		public double last_target_lat = 50.0;
		public double last_target_lon = 10.0;
		public String last_target_name = "default";
		// "default" -> no target set
		// "GC:GCXXXX" -> gc cache GC-Code
		// "M:GCXXXX" -> manually entered coord (for GC-Code cache)
		// "M:manual" -> manually entered coords
		public Boolean download_noimages = false;
		public String download_map_path = "/sdcard/external_sd/zoffcc/applications/aagtl";
		public Boolean options_hide_found = false;
		public Boolean options_use_compass_heading = false;
		public String options_gc_filter__type = "";
		public Boolean options_turn_map_on_heading = false;
	}

	public settings global_settings = new settings();

	/** this criteria will settle for less accuracy, high power, and cost */
	public static Criteria createCoarseCriteria()
	{

		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_HIGH);
		return c;

	}

	/** this criteria needs high accuracy, high power, and cost */
	public static Criteria createFineCriteria()
	{

		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setAltitudeRequired(false);
		c.setBearingRequired(false);
		c.setSpeedRequired(false);
		c.setCostAllowed(true);
		c.setPowerRequirement(Criteria.POWER_HIGH);
		return c;

	}

	public void load_settings()
	{
		System.out.println("load settings");

		// O P E N
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(this.main_dir + "/config/settings.cfg");
		}
		catch (FileNotFoundException e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("FileNotFoundException");
			}
			global_settings = new settings();
			return;
		}

		BufferedInputStream bis = new BufferedInputStream(fis, 65536 /* 64K bytes */);
		ObjectInputStream ois = null;
		try
		{
			ois = new ObjectInputStream(bis);
		}
		catch (StreamCorruptedException e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("StreamCorruptedException");
			}
			global_settings = new settings();
			return;
		}
		catch (IOException e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("IOException");
			}
			global_settings = new settings();
			return;
		}

		// R E A D
		try
		{
			this.global_settings = (aagtl.settings) ois.readObject();
			// check for encrypted password
			if (this.global_settings.download_run_after_string.startsWith("CFG:v2.0:"))
			{
				// ok v2.0 so password is already encrypted
				String inputString = this.global_settings.options_password;
				if (CacheDownloader.DEBUG_)
				{
					System.out.println(this.global_settings.download_run_after_string);
				}
				String salt = this.global_settings.download_run_after_string.split(":")[2];
				String cryptword = salt + "aagtl-" + "52-" + "Crypt";

				// Create encrypter/decrypter class
				StringEnc desEncrypter = new StringEnc(cryptword);

				// Decrypt the string
				String desDecrypted = desEncrypter.decrypt(inputString);

				// Print out values
				if (CacheDownloader.DEBUG_)
				{
					System.out.println("    Original String  : " + inputString);
					System.out.println("         Salt (read) : " + salt);
					System.out.println("    Decrypted String : " + desDecrypted);
					System.out.println();
				}

				this.global_settings.options_password = desDecrypted;
			}
			else
			{
				// old config so password is clear text
				// now generate a salt value
				String inputString = this.global_settings.options_password;
				String salt = String.valueOf((int) (1000f * Math.random()));
				if (CacheDownloader.DEBUG_)
				{
					System.out.println("    Original String  : " + inputString);
					System.out.println("          Salt (new) : " + salt);
				}
				this.global_settings.download_run_after_string = "CFG:v2.0:" + salt + ":";
			}
		}
		catch (OptionalDataException e)
		{
			global_settings = new settings();
			return;
		}
		catch (ClassNotFoundException e)
		{
			global_settings = new settings();
			return;
		}
		catch (IOException e)
		{
			global_settings = new settings();
			return;
		}
		catch (Exception e)
		{
			// fail safe, catch all!
			global_settings = new settings();
			return;
		}

		// C L O S E
		try
		{
			ois.close();
		}
		catch (IOException e)
		{
			global_settings = new settings();
			return;
		}

		// check if currently viewed cache exists, if not set it to something
		// sane
		if ((this.cacheview == null) || (this.cacheview.gc == null))
		{
			this.set_init_target_cache();
		}

		if (CacheDownloader.DEBUG_)
		{
			System.out.println("settings:" + this.global_settings.options_username);
			System.out.println("settings:" + this.global_settings.options_password);
		}
	}

	public void save_settings()
	{
		System.out.println("save settings");

		// make dirs
		File dir1 = new File(this.main_dir + "/config/settings.cfg");
		File dir2 = new File(dir1.getParent());
		dir2.mkdirs();

		// O P E N
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(this.main_dir + "/config/settings.cfg", false /* append */);
		}
		catch (FileNotFoundException e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("FileNotFoundException");
			}
			return;
		}

		BufferedOutputStream bos = new BufferedOutputStream(fos, 65536 /*
																		 * 64K
																		 * bytes
																		 */);
		ObjectOutputStream oos = null;

		try
		{
			oos = new ObjectOutputStream(bos);
		}
		catch (IOException e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("IOException");
			}
			return;
		}

		// W R I T E
		// object to write, and objects it points to must implement
		// java.io.Serializable
		// Ideally such objects should also have have a field:
		// static final long serialVersionUID = 1L;
		// to prevent spurious InvalidClassExceptions.

		try
		{
			// +++++++++++++++++++++++++
			// copy the settings object
			// +++++++++++++++++++++++++
			// Serialize to a byte array
			ByteArrayOutputStream bos_save = new ByteArrayOutputStream();
			ObjectOutputStream out_save = new ObjectOutputStream(bos_save);
			out_save.writeObject(this.global_settings);
			out_save.close();
			// Get the bytes of the serialized object
			byte[] buf_save = bos_save.toByteArray();
			// Deserialize from a byte array
			ObjectInputStream in_save = new ObjectInputStream(new ByteArrayInputStream(buf_save));
			settings save_settings = (settings) in_save.readObject();
			in_save.close();
			// +++++++++++++++++++++++++
			// copy the settings object
			// +++++++++++++++++++++++++

			if (save_settings.download_run_after_string.startsWith("CFG:v2.0:"))
			{
				// ok v2.0 so password is clear text new, encrypt it
				String inputString = save_settings.options_password;
				String salt = "";
				try
				{
					salt = save_settings.download_run_after_string.split(":")[2];
					if (CacheDownloader.DEBUG_)
					{
						System.out.println("using salt: " + salt);
					}
				}
				catch (Exception e)
				{
					// now generate a salt value
					salt = String.valueOf((int) (1000f * Math.random()));
					this.global_settings.download_run_after_string = "CFG:v2.0:" + salt + ":";
					save_settings.download_run_after_string = this.global_settings.download_run_after_string;
					if (CacheDownloader.DEBUG_)
					{
						System.out.println("generated salt: " + salt);
					}
				}
				String cryptword = salt + "aagtl-" + "52-" + "Crypt";

				// Create encrypter/decrypter class
				StringEnc desEncrypter = new StringEnc(cryptword);

				// Encrypt the string
				String desEncrypted = desEncrypter.encrypt(inputString);

				if (CacheDownloader.DEBUG_)
				{
					// Print out values
					System.out.println("    Original String  : " + inputString);
					System.out.println("         Salt (read) : " + salt);
					System.out.println("    Encrypted String : " + desEncrypted);
					System.out.println();
				}

				save_settings.options_password = desEncrypted;
			}

			oos.writeObject(save_settings);
			oos.flush();
			// C L O S E
			oos.close();

			// free some memory
			save_settings = null;
		}
		catch (IOException e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("IOException 2");
			}
			return;
		}
		catch (Exception e)
		{
			if (CacheDownloader.DEBUG_)
			{
				System.out.println("**Exception**");
			}
			// fail safe, catch all!
			return;
		}

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState)
	{
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.
		savedInstanceState.putDouble("global_settings.map_position_lat", global_settings.map_position_lat);
		savedInstanceState.putDouble("global_settings.map_position_lon", global_settings.map_position_lon);
		System.out.println(String.valueOf(this.cacheview.gc));
		savedInstanceState.putSerializable("cacheview.gc", this.cacheview.gc);
		savedInstanceState.putSerializable("rose.current_target", this.rose.current_target);
		this.save_settings();

		super.onSaveInstanceState(savedInstanceState);
		System.out.println("save bundle");
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		if (!savedInstanceState.isEmpty())
		{
			global_settings.map_position_lat = savedInstanceState.getDouble("global_settings.map_position_lat");
			global_settings.map_position_lon = savedInstanceState.getDouble("global_settings.map_position_lon");

			// try
			// {
			// System.out.println(String.valueOf(this.cacheview.gc));
			// this.cacheview.gc = (GeocacheCoordinate) savedInstanceState
			// .getSerializable("cacheview.gc");
			// System.out.println(String.valueOf(this.cacheview.gc));
			// this.rose.current_target = (GeocacheCoordinate)
			// savedInstanceState
			// .getSerializable("rose.current_target");
			// }
			// catch (Exception e)
			// {
			// System.out.println("error in restoring values");
			// }
		}

		System.out.println("restore bundle");

		// System.out.println("map_position_lat:" +
		// String.valueOf(global_settings.map_position_lat));
		// System.out.println("map_position_lon:" +
		// String.valueOf(global_settings.map_position_lon));
	}

	public void set_center(double lat, double lon)
	{
		global_settings.map_position_lat = lat;
		global_settings.map_position_lon = lon;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// choose the correct volume to change (for TTS)
		// setVolumeControlStream(AudioManager.STREAM_SYSTEM);
		// setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
		setVolumeControlStream(AudioManager.STREAM_MUSIC); // --> this seems to
		// be used for TTS
		// setVolumeControlStream(AudioManager.STREAM_DTMF);

		String default_sdcard_dir = Environment.getExternalStorageDirectory().getAbsolutePath();
		Log.e("aagtl", "sdcard dir=" + default_sdcard_dir);

		main_dir = default_sdcard_dir + "/zoffcc/applications/aagtl";
		main_data_dir = default_sdcard_dir + "/external_sd/zoffcc/applications/aagtl";

		// get the local language -------------
		Locale locale = java.util.Locale.getDefault();
		String lang = locale.getLanguage();
		String langu = lang;
		String langc = lang;
		Log.e("aagtl", "lang=" + lang);
		int pos = langu.indexOf('_');
		if (pos != -1)
		{
			langc = langu.substring(0, pos);
			langu = langc + langu.substring(pos).toUpperCase(locale);
			Log.e("aagtl", "substring lang " + langu.substring(pos).toUpperCase(locale));
			// set lang. for translation
			main_language = langc;
			sub_language = langu.substring(pos).toUpperCase(locale);
		}
		else
		{
			String country = locale.getCountry();
			Log.e("aagtl", "Country1 " + country);
			Log.e("aagtl", "Country2 " + country.toUpperCase(locale));
			langu = langc + "_" + country.toUpperCase(locale);
			// set lang. for translation
			main_language = langc;
			sub_language = country.toUpperCase(locale);
		}
		Log.e("aagtl", "Language " + lang);
		// get the local language -------------

		// make dirs
		File dir11 = new File(this.main_dir);
		dir11.mkdirs();
		File dir22 = new File(this.main_data_dir);
		dir22.mkdirs();
		// create nomedia files
		File nomedia_file = new File(this.main_dir + "/.nomedia");
		try
		{
			nomedia_file.createNewFile();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		nomedia_file = new File(this.main_data_dir + "/.nomedia");
		try
		{
			nomedia_file.createNewFile();
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		// create nomedia files

		// make dirs (for "route" files, you can select a file, and only those
		// GC will be shown)
		File dir71 = new File(this.main_dir + "/routes/");
		dir71.mkdirs();

		/*
		 * show info box for first time users
		 */
		AlertDialog.Builder infobox = new AlertDialog.Builder(this);
		infobox.setTitle(aagtlTextTranslations.INFO_BOX_TITLE); // TRANS
		infobox.setCancelable(false);
		final TextView message = new TextView(this);
		message.setFadingEdgeLength(20);
		message.setVerticalFadingEdgeEnabled(true);
		message.setVerticalScrollBarEnabled(true);
		RelativeLayout.LayoutParams rlpib = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

		// margins seem not to work, hmm strange
		// so add a " " at start of every line. well whadda you gonna do ...
		// rlp.leftMargin = 8; -> we use "m" string

		message.setLayoutParams(rlpib);
		final SpannableString s = new SpannableString(aagtlTextTranslations.INFO_BOX_TEXT); // TRANS
		Linkify.addLinks(s, Linkify.WEB_URLS);
		message.setText(s);
		message.setMovementMethod(LinkMovementMethod.getInstance());
		infobox.setView(message);

		// TRANS
		infobox.setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface arg0, int arg1)
			{
				Log.e("aagtl", "Ok, user saw the infobox");
			}
		});

		// TRANS
		infobox.setNeutralButton(aagtlTextTranslations.JAVA_MENU_MOREINFO, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface arg0, int arg1)
			{
				Log.e("aagtl", "user wants more info, show the website");
				String url = "http://aagtl.work.zoff.cc/";
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(url));
				startActivity(i);
			}
		});

		//
		//
		//
		show_no_loc_warning = false;
		try
		{
			// get location services
			// lm = LocationUtils.getLocationManager(ctx.getMyContext());
			this.lm = (LocationManager) getSystemService(LOCATION_SERVICE);
			// get low accuracy provider
			this.low = lm.getProvider(lm.getBestProvider(createCoarseCriteria(), true));

			// get high accuracy provider
			this.high = lm.getProvider(lm.getBestProvider(createFineCriteria(), true));
			// ++++++ lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 700L,
			// 1.0f, this);

			if (!EMULATOR)
			{
				sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			// Toast.makeText(getApplicationContext(), "You dont have any Location providers!\nthis program will not work on your device!", Toast.LENGTH_LONG).show();
			// set the infobox text to this error message, to tell the user it wont work correctly
			message.setText("\n You don't have any Location providers!\n This program will not work fully!\n");
			show_no_loc_warning = true;
		}

		//
		//
		//
		//

		String FIRST_STARTUP_FILE = main_dir + "/infobox_seen.txt";
		File aagtl_first_startup = new File(FIRST_STARTUP_FILE);
		// if file does NOT exist, show the info box
		// or if "no location providers", show info box
		if ((!aagtl_first_startup.exists()) || (show_no_loc_warning))
		{
			// if "no location providers" text, then dont show/save info box file
			if (!show_no_loc_warning)
			{
				FileOutputStream fos_temp;
				try
				{
					fos_temp = new FileOutputStream(aagtl_first_startup);
					fos_temp.write((int) 65); // just write an "A" to the file, but
					// really doesnt matter
					fos_temp.flush();
					fos_temp.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			try
			{
				infobox.show();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		/*
		 * show info box for first time users
		 */

		// get display properties
		Display display_ = getWindowManager().getDefaultDisplay();
		int width_ = display_.getWidth();
		int height_ = display_.getHeight();
		this.metrics = new DisplayMetrics();
		display_.getMetrics(this.metrics);
		Log.e("aagtl", "aagtl -> pixels x=" + width_ + " pixels y=" + height_);
		Log.e("aagtl", "aagtl -> dpi=" + this.metrics.densityDpi);
		Log.e("aagtl", "aagtl -> density=" + this.metrics.density);
		Log.e("aagtl", "aagtl -> scaledDensity=" + this.metrics.scaledDensity);
		// get display properties

		System.out.println("Create: " + String.valueOf(savedInstanceState));
		if (savedInstanceState != null)
		{
			this.onRestoreInstanceState(savedInstanceState);
		}

		this.load_settings();

		// Set full screen view
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFlags(0, 0);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		requestWindowFeature(0);

		// System.out.println("CR map_position_lat:" +
		// String.valueOf(global_settings.map_position_lat));
		// System.out.println("CR map_position_lon:" +
		// String.valueOf(global_settings.map_position_lon));

		// setContentView(R.layout.main);

		Boolean sensors_fullblown = false;
		if (sensors_fullblown)
		{
			List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
			Sensor ourSensor = null;
			for (int i = 0; i < sensors.size(); ++i)
			{
				ourSensor = sensors.get(i);
				// System.out.println(ourSensor.getName());
				if (ourSensor != null)
				{
					sensorManager.registerListener(this, ourSensor, SensorManager.SENSOR_DELAY_NORMAL);
				}
			}
		}

		// System.out.println("main: create");
		this.mdl = new MapDownloader(global_messageHandler, this);
		this.mdl.start();

		this.wdl = new HTMLDownloader(this);

		this.cdol = new CacheDownloader(this, wdl);

		// ------- MAP VIEW: CREATE -------
		layoutParams_mainscreen_map_view = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		mainscreen_map_view = new RelativeLayout(this);
		mainscreen_map_view.setLayoutParams(layoutParams_mainscreen_map_view);

		status_text = new TextView(this);
		status_text.setBackgroundColor(Color.BLACK);
		status_text.setTextColor(Color.WHITE);
		status_text.setMaxLines(1);
		status_text.setTextSize(12);
		status_text.setText("--- --- --- --- ---");
		status_text.setVisibility(0); // set to visible

		rose = new Rose(this, this);
		RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		rlp.bottomMargin = mainscreen_map_view_statusbar_height;
		rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mainscreen_map_view.addView(rose, rlp);

		RelativeLayout.LayoutParams tvlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, mainscreen_map_view_statusbar_height);
		tvlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mainscreen_map_view.addView(status_text, tvlp);

		this.wv = new WebView(this);
		this.wv.getSettings().setSupportZoom(true);
		this.wv.getSettings().setBuiltInZoomControls(true);
		this.wv.getSettings().setDefaultFontSize(13);

		// zoom seems to be NULL on some systems (e.g. honeycomb)
		final View zoom = this.wv.getZoomControls();

		try
		{
			zoom.setVisibility(View.INVISIBLE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		this.wv.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
		RelativeLayout.LayoutParams wvlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		mainscreen_map_view.addView(wv, wvlp);

		cross = new CrossHair(this, this);
		RelativeLayout.LayoutParams chlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

		gcview = new GeocachesView(this, this);
		RelativeLayout.LayoutParams gcwlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

		arrowview = new ArrowView(this, this);
		RelativeLayout.LayoutParams avlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

		cacheview = new GCacheView(this, this);
		RelativeLayout.LayoutParams cvlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

		// get the png's
		this.follow_on = BitmapFactory.decodeResource(getResources(), R.drawable.follow);
		this.follow_off = BitmapFactory.decodeResource(getResources(), R.drawable.follow_off);
		this.follow_current = this.follow_on;
		this.arrow_button = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_off);

		chlp.bottomMargin = mainscreen_map_view_statusbar_height;
		gcwlp.bottomMargin = mainscreen_map_view_statusbar_height;
		avlp.bottomMargin = mainscreen_map_view_statusbar_height;
		cvlp.bottomMargin = mainscreen_map_view_statusbar_height;
		wvlp.bottomMargin = mainscreen_map_view_statusbar_height;
		wvlp.topMargin = 80 + 4 * 40;
		mainscreen_map_view.addView(cross, chlp);
		mainscreen_map_view.addView(gcview, gcwlp);
		mainscreen_map_view.addView(arrowview, avlp);
		mainscreen_map_view.addView(cacheview, cvlp);
		// ------- MAP VIEW: CREATE -------

		// ------- VIEWS -------
		// make correct Z-order
		rose.bringToFront();
		gcview.bringToFront();
		cross.bringToFront();
		status_text.bringToFront();
		wv.bringToFront();
		arrowview.setVisibility(View.INVISIBLE);
		cacheview.setVisibility(View.INVISIBLE);
		wv.setVisibility(View.INVISIBLE);
		setContentView(mainscreen_map_view);
		// ------- VIEWS -------

		// ------- set default VIEW on display
		this.set_display_screen(aagtl.DISPLAY_VIEW_MAP);
		// ------- set default VIEW on display

		db_path = this.main_data_dir + "/config/caches.db";
		File dir1 = new File(db_path);
		File dir2 = new File(dir1.getParent());
		dir2.mkdirs();
		// pv = new PointProvider(db_path, this.mdl, "", "geocaches");
		// *** DANGER *** pv._clear_database_();

		// get position on first startup
		turn_off_gps();
		try
		{
			// wait for gsm cell fix
			Thread.sleep(1000L);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		// ok, now switch to real gps
		turn_on_gps();

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK ,
		// "aagtlWakeLock"); // i think this is not working
		this.wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "aagtlWakeLock");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		// System.out.println("onCreateOptionsMenu");
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		// this gets called every time the menu is opened!!
		// change menu items here!
		// System.out.println("onPrepareOptionsMenu");

		switch (this.current_display_view)
		{
		case DISPLAY_VIEW_MAP:
			// main map view

			menu.clear();
			// g-id,i-id,order
			menu.add(1, 2, 20, "zoom in");
			menu.add(1, 3, 22, "zoom out");

			menu.add(1, 4, 30, "maptype: OSM");
			if (__ZOFF_PHONE__)
			{
				menu.add(1, 5, 32, "maptype: Sat");
			}
			else
			{
				menu.add(1, 29, 33, "maptype: OCM");
			}

			menu.add(1, 1, 40, "get caches in view");
			menu.add(1, 20, 41, "get details in view");

			menu.add(1, 21, 42, "turn on GPS");
			menu.add(1, 22, 43, "turn off GPS");
			menu.add(1, 25, 44, "show arrow view");

			menu.add(1, 30, 45, "navigate to target");
			menu.add(1, 32, 46, "change target lat.");
			menu.add(1, 33, 47, "change target lon.");

			menu.add(1, 27, 49, "select routefile");
			menu.add(1, 28, 50, "get caches from routefile");
			menu.add(1, 23, 51, "upload fieldnotes");
			if (this.global_settings.options_turn_map_on_heading)
			{
				menu.add(1, 26, 52, "turn off rotating map");
			}
			else
			{
				menu.add(1, 26, 52, "turn on rotating map");
			}

			menu.add(1, 6, 78, "-");
			menu.add(1, 19, 79, "set username/password");
			menu.add(1, 6, 80, "-");
			menu.add(1, 7, 82, "empty database");

			break;
		case DISPLAY_VIEW_GC:
			// gc-view
			menu.clear();
			// g-id,i-id,order
			menu.add(1, 8, 20, "map view");
			menu.add(1, 14, 22, "update details");
			menu.add(1, 9, 24, "set as target -> map");
			menu.add(1, 10, 26, "set as target -> arrow");
			menu.add(1, 15, 28, "description");
			menu.add(1, 16, 30, "hints");
			menu.add(1, 17, 32, "logs");
			menu.add(1, 34, 33, "waypoints");
			menu.add(1, 18, 34, "shorttext");
			menu.add(1, 31, 35, "show Cache website");
			// seems to be buggy, so for the moment remove it
			// if (this.rose.current_target != null)
			// {
			// menu.add(1, 24, 36, "post fieldnote");
			// }
			break;
		case DISPLAY_VIEW_ARROW:
			// arrow view
			menu.clear();
			// g-id,i-id,order
			menu.add(1, 11, 20, "cache view");
			menu.add(1, 12, 22, "map view");
			String st_compass = "use compass heading";
			if (this.global_settings.options_use_compass_heading)
			{
				st_compass = "use gps heading";

			}
			menu.add(1, 13, 24, st_compass);
			if (this.rose.current_target != null)
			{
				menu.add(1, 24, 26, "post fieldnote");
			}
			break;
		}

		return true;
	}

	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			// System.out.println("Sensor.TYPE_MAGNETIC_FIELD");
			return;
		}

		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
		{
			// System.out.println("Sensor.TYPE_ORIENTATION");

			Boolean set_c_heading = false;
			if (this.global_settings.options_use_compass_heading)
			{
				// if "options_use_compass_heading" is set the use compass
				// always
				set_c_heading = true;
				// System.out.println("true 1");
			}
			else
			{
				if (!this.isGPSFix)
				{
					// if no gps-fix then use compass until gps fix found
					set_c_heading = true;
					// System.out.println("true 2");
				}
			}

			if (set_c_heading)
			{
				// compass
				double myAzimuth = event.values[0];
				// double myPitch = event.values[1];
				// double myRoll = event.values[2];

				// String out = String.format("Azimuth: %.2f", myAzimuth);
				// System.out.println("compass: " + out);

				if (this.current_display_view == DISPLAY_VIEW_MAP)
				{
					long normal_delay = 1000L; // update every 1.0 seconds
					if (this.global_settings.options_turn_map_on_heading)
					{
						normal_delay = 400L; // update every 0.4 seconds with
						// turning map!
					}
					if (System.currentTimeMillis() - last_heading_update < normal_delay)
					{
						// System.out.println("-> compass heading: update to fast, skipped");
					}
					else
					{
						// System.out.println("-> compass heading (slow)");
						last_heading_update = System.currentTimeMillis();
						this.cross.set_gps_heading(myAzimuth);
						this.cross.invalidate();
						this.arrowview.invalidate();
					}
				}
				else
				{
					// System.out.println("-> compass heading (fast)");
					last_heading_update = System.currentTimeMillis();
					this.cross.set_gps_heading(myAzimuth);
					this.cross.invalidate();
					this.arrowview.invalidate();
				}
			}

			// String out =
			// String.format("Azimuth: %.2f\n\nPitch:%.2f\n\nRoll:%.2f\n\n",
			// myAzimuth,
			// myPitch, myRoll);
			// System.out.println("compass: " + out);
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// compass
	}

	@Override
	public void onBackPressed()
	{
		// do something on back.
		// System.out.println("no back key!");

		if (this.current_display_view == DISPLAY_VIEW_MAP)
		{
			this.finish(); // -> this calls "onPause"
		}
		else if (this.current_display_view == DISPLAY_VIEW_GC)
		{
			this.set_display_screen(DISPLAY_VIEW_MAP);
		}
		else if (this.current_display_view == DISPLAY_VIEW_ARROW)
		{
			if (this.rose.current_target != null)
			{
				// show gc view, if we have a target
				this.set_display_screen(DISPLAY_VIEW_GC);
			}
			else
			{
				// no target -> show map view
				this.set_display_screen(DISPLAY_VIEW_MAP);
			}
		}

		return;
	}

	public void set_init_target_cache()
	{
		if (this.global_settings.last_target_name.compareTo("default") == 0)
		{
			// no target set!!
		}
		else
		{
			String my_tmp_name = "last target";
			GeocacheCoordinate tmp_gc = new GeocacheCoordinate(this.global_settings.last_target_lat, this.global_settings.last_target_lon, my_tmp_name);
			if (this.cacheview != null)
			{
				System.out.println("xxxxx1");
				this.cacheview.set_cache(tmp_gc);
				// this.set_display_screen(aagtl.DISPLAY_VIEW_GC);
				// this.set_display_screen(aagtl.DISPLAY_VIEW_MAP);
			}
			if (this.rose != null)
			{
				System.out.println("xxxxx2");
				if (this.global_settings != null)
				{
					System.out.println("xxxxx3");
					System.out.println("" + this.global_settings.last_target_name);
					System.out.println("" + this.global_settings.last_target_lat);
					System.out.println("" + this.global_settings.last_target_lon);
					this.rose.current_target = tmp_gc;
				}
			}
		}
	}

	public void set_target()
	{
		this.rose.current_target = this.cacheview.get_cache();
		this.global_settings.last_target_lat = this.rose.current_target.lat;
		this.global_settings.last_target_lon = this.rose.current_target.lon;
		this.global_settings.last_target_name = "GC:" + this.rose.current_target.name;
		// "default" -> no target set
		// "GC:GCXXXX" -> gc cache GC-Code
		// "M:GCXXXX" -> manually entered coord (for GC-Code cache)
		// "M:manual" -> manually entered coords
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case 1:
			// download caches (no full details)
			this.download_caches_in_visible_view(false);
			return true;
		case 2:
			this.rose.zoom_in();
			return true;
		case 3:
			this.rose.zoom_out();
			return true;
		case 4:
			this.rose.change_map_type(MapDownloader.MAP_OSM);
			return true;
		case 5:
			this.rose.change_map_type(MapDownloader.MAP_BIM);
			return true;
		case 6:
			// dummy, do nothing!!
			// dummy, do nothing!!
			// dummy, do nothing!!
			// dummy, do nothing!!
			// dummy, do nothing!!
			return true;
		case 7:
			// drop table, and create new -> in DB
			this.pv._clear_database_();
			return true;
		case 8:
			this.set_display_screen(DISPLAY_VIEW_MAP);
			return true;
		case 9:
			// set as target -> map
			this.set_target();
			this.set_display_screen(DISPLAY_VIEW_MAP);
			return true;
		case 10:
			// set as target -> arrow
			this.set_target();
			this.set_display_screen(DISPLAY_VIEW_ARROW);
			return true;
		case 11:
			// cache view
			this.set_display_screen(DISPLAY_VIEW_GC);
			return true;
		case 12:
			// map view
			this.set_display_screen(DISPLAY_VIEW_MAP);
			return true;
		case 13:
			// toggle compass/gps heading
			this.global_settings.options_use_compass_heading = !this.global_settings.options_use_compass_heading;
			return true;
		case 14:
			// download gc-details again
			this.cacheview.details_loaded = 0;
			this.cacheview.download_details_thread_finished = false;
			this.cacheview.gc_name_previous = "";
			this.cacheview.need_repaint = true;
			this.cacheview.override_download = true;
			this.cacheview.invalidate();
			return true;
		case 15:
			// show description
			this.cacheview.show_field = GCacheView.SHOW_DESC;
			this.cacheview.need_repaint = true;
			this.cacheview.invalidate();
			return true;
		case 16:
			// show hints
			this.cacheview.show_field = GCacheView.SHOW_HINTS;
			this.cacheview.need_repaint = true;
			this.cacheview.invalidate();
			return true;
		case 17:
			// show logs
			this.cacheview.show_field = GCacheView.SHOW_LOGS;
			this.cacheview.need_repaint = true;
			this.cacheview.invalidate();
			return true;
		case 18:
			// show shortdesc
			this.cacheview.show_field = GCacheView.SHOW_SHORT_DESC;
			this.cacheview.need_repaint = true;
			this.cacheview.invalidate();
			return true;
		case 19:
			// set gc.com user/pass
			// save setting before we leave this activity!!
			this.save_settings();
			// ok startup the user/pass activity
			Intent foo = new Intent(this, TextEntryActivity.class);
			foo.putExtra("title", "Enter geocaching.com userdetails");
			foo.putExtra("username", this.global_settings.options_username);
			foo.putExtra("password", this.global_settings.options_password);
			this.startActivityForResult(foo, 7);
			return true;
		case 20:
			// download caches (with full details!)
			this.download_caches_in_visible_view(true);
			return true;
		case 21:
			// turn gps on
			this.turn_on_gps();
			this.cross.invalidate();
			this.arrowview.invalidate();
			return true;
		case 22:
			// turn gps off
			this.turn_off_gps();
			this.cross.invalidate();
			this.arrowview.invalidate();
			return true;
		case 23:
			// upload fieldnotes
			this.upload_fieldnotes();
			return true;
		case 24:
			// open fieldnote entry form
			// ok startup the activity
			Intent foo2 = new Intent(this, PostLogEntryActivity.class);
			foo2.putExtra("title", "Fieldnote for " + this.rose.current_target.name);
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			foo2.putExtra("msg", "\nTFTC\n" + sdf2.format(cal.getTime()) + "");
			this.startActivityForResult(foo2, 8);
			return true;
		case 25:
			// set "arrow" as current view
			this.set_display_screen(DISPLAY_VIEW_ARROW);
			return true;
		case 26:
			// toggle "turn map with heading"
			this.global_settings.options_turn_map_on_heading = !this.global_settings.options_turn_map_on_heading;
			return true;
		case 27:
			// select routefile
			this.get_route_files();
			return true;
		case 28:
			// download caches from route file
			if ((this.route_file_caches != null) && (this.route_file_caches.size() > 0))
			{
				showDialog(3);
			}
			else
			{
				Toast.makeText(getApplicationContext(), "No caches in Route!", Toast.LENGTH_SHORT).show();
			}
			return true;
		case 29:
			// map type open cycle maps
			this.rose.change_map_type(MapDownloader.MAP_OCM);
			return true;
		case 30:
			// call navigation intent to drive to target
			this.navigate_to_target();
			return true;
		case 31:
			// show gc cache page in browser
			this.show_cache_page_in_browser();
			return true;
		case 32:
			// change target coords lat
			if (this.rose.current_target != null)
			{
				this.change_target_coords_lat();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "No target selected", Toast.LENGTH_SHORT).show();
			}
			return true;
		case 33:
			// change target coords lon
			if (this.rose.current_target != null)
			{
				this.change_target_coords_lon();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "No target selected", Toast.LENGTH_SHORT).show();
			}
			return true;
		case 34:
			// show waypoints
			this.cacheview.show_field = GCacheView.SHOW_WAYPOINTS;
			this.cacheview.need_repaint = true;
			this.cacheview.invalidate();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void toast_me(String in_text, int duration)
	{
		Toast.makeText(getApplicationContext(), in_text, duration).show();
	}

	public Coordinate pixmappoint2coord(int[] point)
	{
		Coordinate ret = null;
		ret = this.rose.num2deg(((double) point[0] + (double) (this.rose.map_center_x * this.rose.tile_size_x) - (double) (this.rose.mCanvasWidth / 2)) / (double) this.rose.tile_size_x, ((double) point[1] + (double) (this.rose.map_center_y * this.rose.tile_size_y) - (double) (this.rose.mCanvasHeight / 2)) / (double) this.rose.tile_size_y);
		return ret;
	}

	public void set_display_screen(int dt)
	{
		switch (dt)
		{
		case DISPLAY_VIEW_MAP:
			// main map view
			if (!this.global_settings.options_use_compass_heading)
			{
				this.turn_off_compass();
			}
			else
			{
				this.turn_on_compass();
			}
			this.current_display_view = DISPLAY_VIEW_MAP;

			arrowview.setVisibility(View.INVISIBLE);
			cacheview.setVisibility(View.INVISIBLE);
			rose.setVisibility(View.VISIBLE);
			gcview.setVisibility(View.VISIBLE);
			cross.setVisibility(View.VISIBLE);
			status_text.setVisibility(View.VISIBLE);
			wv.setVisibility(View.VISIBLE);

			rose.bringToFront();
			gcview.bringToFront();
			cross.bringToFront();
			status_text.bringToFront();
			cross.invalidate();
			gcview.invalidate();
			status_text.invalidate();

			break;
		case DISPLAY_VIEW_GC:
			// gc-view
			this.turn_off_compass();
			this.current_display_view = DISPLAY_VIEW_GC;

			rose.setVisibility(View.INVISIBLE);
			gcview.setVisibility(View.INVISIBLE);
			cross.setVisibility(View.INVISIBLE);
			arrowview.setVisibility(View.INVISIBLE);
			wv.setVisibility(View.INVISIBLE);
			status_text.setVisibility(View.VISIBLE);
			cacheview.setVisibility(View.VISIBLE);

			// make sure we dont start cachedownload immediately
			cacheview.override_download = true;
			cacheview.gc.desc = "please update details!!";
			cacheview.download_details_thread_finished = true;
			cacheview.details_loaded = 2;
			cacheview.get_gc_from_db = 1;

			status_text.bringToFront();
			cacheview.bringToFront();
			cacheview.invalidate();
			status_text.invalidate();

			break;
		case DISPLAY_VIEW_ARROW:
			// gc-view
			this.turn_on_compass();
			this.current_display_view = DISPLAY_VIEW_ARROW;

			rose.setVisibility(View.INVISIBLE);
			gcview.setVisibility(View.INVISIBLE);
			cross.setVisibility(View.INVISIBLE);
			cacheview.setVisibility(View.INVISIBLE);
			wv.setVisibility(View.INVISIBLE);
			status_text.setVisibility(View.VISIBLE);
			arrowview.setVisibility(View.VISIBLE);

			status_text.bringToFront();
			arrowview.bringToFront();
			arrowview.invalidate();
			status_text.invalidate();

			break;
		}
	}

	public Coordinate[] get_visible_area()
	{
		Coordinate[] ret = new Coordinate[2];
		int[] ii = new int[2];
		ii[0] = -10;
		ii[1] = -10;
		// System.out.println(" " + ii[0] + " " + ii[1]);
		ret[0] = this.pixmappoint2coord(ii);
		// System.out.println(" " + ret[0]);

		ii[0] = this.rose.mCanvasWidth + 10;
		ii[1] = this.rose.mCanvasHeight + 10;
		// System.out.println(" " + ii[0] + " " + ii[1]);
		ret[1] = this.pixmappoint2coord(ii);
		// System.out.println(" " + ret[1]);

		return ret;
	}

	public Coordinate[] get_visible_area_large()
	{
		Coordinate[] ret = new Coordinate[2];
		int[] ii = new int[2];
		ii[0] = -60;
		ii[1] = -60;
		// System.out.println(" " + ii[0] + " " + ii[1]);
		ret[0] = this.pixmappoint2coord(ii);
		// System.out.println(" " + ret[0]);

		ii[0] = this.rose.mCanvasWidth + 60;
		ii[1] = this.rose.mCanvasHeight + 60;
		// System.out.println(" " + ii[0] + " " + ii[1]);
		ret[1] = this.pixmappoint2coord(ii);
		// System.out.println(" " + ret[1]);

		return ret;
	}

	// Sets screen rotation as fixed to current rotation setting
	public void mLockScreenRotation()
	{
		// Stop the screen orientation changing during an event
		switch (this.getResources().getConfiguration().orientation)
		{
		case Configuration.ORIENTATION_PORTRAIT:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
		case Configuration.ORIENTATION_LANDSCAPE:
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		}
	}

	// allow screen rotations again
	public void mUnlockScreenRotation()
	{
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	}

	public void download_caches_in_visible_view(Boolean download_details)
	{
		if (download_details)
		{
			this.downloaded_caches = null;
			showDialog(2);
		}
		else
		{
			this.downloaded_caches = null;
			showDialog(0);
		}
	}

	public void set_bar(Handler h, String title, String text, int cur, int max, Boolean show)
	{
		Message msg = h.obtainMessage();
		Bundle b = new Bundle();
		b.putInt("max", max);
		b.putInt("cur", cur);
		b.putString("title", title);
		b.putString("text", text);
		b.putBoolean("show", true);
		msg.setData(b);
		h.sendMessage(msg);
		SystemClock.sleep(1);
	}

	public void set_bar_slow(Handler h, String title, String text, int cur, int max, Boolean show)
	{
		Message msg = h.obtainMessage();
		Bundle b = new Bundle();
		b.putInt("max", max);
		b.putInt("cur", cur);
		b.putString("title", title);
		b.putString("text", text);
		b.putBoolean("show", true);
		msg.setData(b);
		h.sendMessage(msg);
		SystemClock.sleep(50);
	}

	public void download_caches_from_route_file(Handler h)
	{
		if (this.route_file_caches == null)
		{
			return;
		}

		// dont go to sleep
		this.wl.acquire();

		int count_p = 0;
		int max_p = this.route_file_caches.size();
		this.mLockScreenRotation();
		set_bar(h, "get geocaches", "downloading caches from route file", 0, max_p, true);

		GeocacheCoordinate this_gc = null;
		int reopen_after = 30;
		int cur = 0;

		this.pv.begin_trans();
		try
		{

			for (int _cache = 0; _cache < this.route_file_caches.size(); _cache++)
			{
				this_gc = this.route_file_caches.get(_cache);
				set_bar(h, "get geocaches", "downloading caches from route file", _cache, max_p, true);
				this.pv.add_point(this_gc);
				this.cdol.update_coordinate(this_gc);

				cur++;
				if (cur > reopen_after)
				{
					this.pv.commit();
					this.pv.end_trans();
					this.pv.reopen_db();
					this.pv.begin_trans();
					cur = 0;
				}

			}
			this.pv.commit();
		}
		finally
		{
			this.pv.end_trans();
		}
		set_bar(h, "get geocaches", "finished", max_p, max_p, true);
	}

	public void download_caches_in_visible_view_wrapper(Handler h, Boolean download_details)
	{
		Coordinate[] location = this.get_visible_area();
		int count_p = 0;
		int max_p;
		if (this.rose.zoom > 11)
		{
			// this is just an estimate!! so that progressbar looks better
			max_p = (1 * 4) + (4 * 4);
		}
		else
		{
			max_p = (1 * 4) + (4 * 4) + (4 * 4 * 4);
		}

		// dont go to sleep
		this.wl.acquire();
		this.mLockScreenRotation();

		set_bar(h, "get geocaches", "downloading ...", 0, max_p, true);

		HTMLDownloader.get_geocaches_ret ps = null;
		ps = wdl.get_geocaches(location, count_p, max_p, 0, h, this.rose.zoom);

		set_bar(h, "get geocaches", "downloading ...", max_p, max_p, true);

		if (ps.points != null)
		{

			set_bar(h, "get geocaches", "inserting into DB ...", 0, ps.points.length, true);

			GeocacheCoordinate this_gc = null;

			int reopen_after = 30;
			int cur = 0;

			this.pv.begin_trans();
			try
			{
				for (int _cache = 0; _cache < ps.points.length; _cache++)
				{
					this_gc = ps.points[_cache];
					this.pv.add_point(this_gc);
					set_bar(h, "get geocaches", "inserting into DB ...", (_cache + 1), ps.points.length, true);

					cur++;
					if (cur > reopen_after)
					{
						this.pv.commit();
						this.pv.end_trans();
						this.pv.reopen_db();
						this.pv.begin_trans();
						cur = 0;
					}

				}
				this.pv.commit();
			}
			finally
			{
				this.pv.end_trans();
			}

			set_bar(h, "get geocaches", "inserting into DB ...", ps.points.length, ps.points.length, true);

			set_bar(h, "get geocaches", "parsing ...", 0, ps.points.length, true);

			this.downloaded_caches = new ArrayList<GeocacheCoordinate>();
			for (int _cache = 0; _cache < ps.points.length; _cache++)
			{
				this_gc = ps.points[_cache];
				this.downloaded_caches.add(this_gc);
				set_bar(h, "get geocaches", "parsing ...", (_cache + 1), ps.points.length, true);
			}

			if (download_details)
			{
				// now download cache details 1-by-1
				// this is going to be slow!!!!! so watch out!

				reopen_after = 15;
				cur = 0;

				long timestamp1 = 0;
				long timestamp2 = 0;
				float remain = 0;
				float timestamp3 = 0;

				timestamp1 = SystemClock.elapsedRealtime();
				this.pv.compact();
				set_bar(h, "get geocaches", "downloading details ...", 0, this.downloaded_caches.size(), true);
				this.pv.begin_trans();
				try
				{

					for (int _cache = 0; _cache < this.downloaded_caches.size(); _cache++)
					{
						timestamp2 = SystemClock.elapsedRealtime();
						this_gc = this.downloaded_caches.get(_cache);
						this.cdol.update_coordinate(this_gc);

						cur++;
						if (cur > reopen_after)
						{
							this.pv.commit();
							this.pv.end_trans();
							this.pv.reopen_db();
							this.pv.begin_trans();
							cur = 0;
						}
						timestamp3 = ((float) SystemClock.elapsedRealtime() - (float) timestamp2) / 1000f;
						remain = ((SystemClock.elapsedRealtime() - timestamp1) / (_cache + 1)) * (this.downloaded_caches.size() - (_cache + 1)) / 1000;
						set_bar(h, "get geocaches", String.format("this: %.1f s, remaining: %.0f s", timestamp3, remain), (_cache + 1), this.downloaded_caches.size(), true);
					}
					this.pv.commit();
				}
				finally
				{
					this.pv.end_trans();
				}
				set_bar(h, "get geocaches", "finished", this.downloaded_caches.size(), this.downloaded_caches.size(), true);
			}
			else
			{
				set_bar(h, "get geocaches", "finished", ps.points.length, ps.points.length, true);
			}
		}
	}

	public Handler toast_handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg44)
		{
			if (msg44.getData().getInt("command") == 1)
			{
				toast_me(msg44.getData().getString("text"), msg44.getData().getInt("duration"));
			}
		}
	};

	public Handler dl_handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg33)
		{
			// System.out.println("msg33");
			if (msg33.getData().getInt("command") == 1)
			{
				removeDialog(1);
			}
			else if (msg33.getData().getInt("command") == 2)
			{
				cacheview.invalidate();
			}
			else
			{
				showDialog(1);
			}
		}
	};

	private Handler progress_handler = new Handler()
	{
		public void handleMessage(Message msg22)
		{
			pbarDialog.setMax(msg22.getData().getInt("max"));
			pbarDialog.setProgress(msg22.getData().getInt("cur"));
			pbarDialog.setTitle(msg22.getData().getString("title"));
			pbarDialog.setMessage(msg22.getData().getString("text"));
			// System.out.println("msg: "
			// + msg22.getData().getInt("cur") + " "
			// + msg22.getData().getInt("max") + " "
			// + msg22.getData().getString("text"));
			// if (!msg22.getData().getBoolean("show"))
			// {
			// dismissDialog(0);
			// }
		}
	};

	Handler gcv_Refresh = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case 0:
				// gcview.set_loaded_caches(downloaded_caches);
				rose.load_caches_from_db();
				break;
			}
		}
	};

	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
		case 0:
			pbarDialog = new ProgressDialog(this);
			pbarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pbarDialog.setTitle("--");
			pbarDialog.setMessage("--");
			pbarDialog.setCancelable(true);
			pbarDialog.setProgress(0);
			pbarDialog.setMax(200);
			progressThread = new ProgressThread(progress_handler, 0);
			progressThread.start();
			return pbarDialog;
		case 1:
			ProgressDialog xDialog = new ProgressDialog(this);
			// xDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			xDialog.setTitle("Geocache");
			xDialog.setMessage("downloading details from internet");
			xDialog.setCancelable(false);
			xDialog.setIndeterminate(true);
			// xDialog.setProgress(0);
			// xDialog.setMax(1);
			return xDialog;
		case 2:
			pbarDialog = new ProgressDialog(this);
			pbarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pbarDialog.setTitle("--");
			pbarDialog.setMessage("--");
			pbarDialog.setCancelable(true);
			pbarDialog.setProgress(0);
			pbarDialog.setMax(200);
			progressThread = new ProgressThread(progress_handler, 1);
			progressThread.start();
			return pbarDialog;
		case 3:
			pbarDialog = new ProgressDialog(this);
			pbarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pbarDialog.setTitle("--");
			pbarDialog.setMessage("--");
			pbarDialog.setCancelable(true);
			pbarDialog.setProgress(0);
			pbarDialog.setMax(200);
			progressThread = new ProgressThread(progress_handler, 3);
			progressThread.start();
			return pbarDialog;
		default:
			return null;
		}
	}

	private class ProgressThread extends Thread
	{
		Handler mHandler;
		int with_details = 0;

		ProgressThread(Handler h, int with_details)
		{
			this.mHandler = h;
			this.with_details = with_details;
		}

		public void run()
		{
			try
			{
				pv.compact();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (with_details == 0)
			{
				download_caches_in_visible_view_wrapper(mHandler, false);
				dismissDialog(0);
				removeDialog(0);
			}
			else if (with_details == 1)
			{
				download_caches_in_visible_view_wrapper(mHandler, true);
				dismissDialog(2);
				removeDialog(2);
			}
			else if (with_details == 3)
			{
				download_caches_from_route_file(mHandler);
				dismissDialog(3);
				removeDialog(3);
			}
			// put the caches into the correct List
			gcv_Refresh.sendEmptyMessage(0);
			mUnlockScreenRotation();
			// allow sleep again
			wl.release();
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// System.out.println("main: resume");

		// mgpsl = new MyGPSListener();

		this.load_settings();
		this.wdl.loadCookies();

		if (mdl.running != true)
		{
			// System.out.println(mdl.getState());
			if (mdl.getState() == Thread.State.TERMINATED)
			{
				this.mdl = null;
				this.mdl = new MapDownloader(global_messageHandler, this);
				this.mdl.start();
				// System.out.println(mdl.getState());
			}
		}

		// assume we have no gps fix after resume
		isGPSFix = false;

		File dir1 = new File(this.db_path);
		File dir2 = new File(dir1.getParent());
		dir2.mkdirs();
		this.pv = null;
		this.pv = new PointProvider(this.db_path, this.mdl, "", "geocaches");
		// this.pv.set_filter("type='" + GeocacheCoordinate.TYPE_REGULAR + "'");

		// !!!!!!!!!!! DEVELOPMENT !!!!!!!!!!
		// !!!!!!!!!!! DEVELOPMENT !!!!!!!!!!
		// !!!!!!!!!!! DEVELOPMENT !!!!!!!!!!
		File dir3 = new File(dir2.toString() + "/zoff.txt");
		if (dir3.exists())
		{
			this.__ZOFF_PHONE__ = true;
		}
		// !!!!!!!!!!! DEVELOPMENT !!!!!!!!!!
		// !!!!!!!!!!! DEVELOPMENT !!!!!!!!!!
		// !!!!!!!!!!! DEVELOPMENT !!!!!!!!!!

		// start listening for GPS
		this.turn_on_locations();

		// start compass
		this.turn_on_compass();

		// System.out.println("x1=" + this.cacheview.gc_name_current);
		// System.out.println("x2=" + this.cacheview.gc_name_previous);
		// System.out.println("x3=" + this.cacheview.details_loaded);
		// System.out.println("x4=" + String.valueOf(this.cacheview.gc));

		this.rose.load_caches_from_db();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
		case 7:
			try
			{
				String value = data.getStringExtra("username");
				if (value != null && value.length() > 0)
				{
					this.global_settings.options_username = value;
					// System.out.println("u=" + String.valueOf(value));
				}

				value = data.getStringExtra("password");
				if (value != null && value.length() > 0)
				{
					this.global_settings.options_password = value;
					// System.out.println("p=" + String.valueOf(value));
				}
			}
			catch (Exception e)
			{
			}

			this.save_settings();

			break;
		case 8:
			try
			{
				String value2 = data.getStringExtra("msg");
				int value3 = data.getIntExtra("logtype", GeocacheCoordinate.LOG_AS_FOUND);

				if (value2 != null && value2.length() > 0)
				{
					System.out.println("fieldnote=" + String.valueOf(value2));
					Calendar cal = Calendar.getInstance();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
					sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
					GeocacheCoordinate temp_gc = this.rose.current_target;
					System.out.println("temp_gc=" + String.valueOf(temp_gc));
					temp_gc.fieldnotes = value2;
					temp_gc.log_date = sdf.format(cal.getTime());

					// set according to selected value!!
					if (value3 == GeocacheCoordinate.LOG_AS_FOUND)
					{
						temp_gc.aagtl_status = GeocacheCoordinate.AAGTL_STATUS_FOUND;
						temp_gc.found = true;
						temp_gc.log_as = value3;
					}
					else if (value3 == GeocacheCoordinate.LOG_AS_NOTFOUND)
					{
						temp_gc.aagtl_status = GeocacheCoordinate.AAGTL_STATUS_NORMAL;
						temp_gc.found = false;
						temp_gc.log_as = value3;
					}
					else if (value3 == GeocacheCoordinate.LOG_AS_NOTE)
					{
						temp_gc.aagtl_status = GeocacheCoordinate.AAGTL_STATUS_NORMAL;
						temp_gc.found = false;
						temp_gc.log_as = value3;
					}
					// set according to selected value!!

					// System.out.println("v3=" + value3 + " f=" + temp_gc.found
					// + " as="
					// + temp_gc.aagtl_status);

					this.pv.reopen_db();
					this.pv.compact();
					this.pv.begin_trans();
					try
					{
						this.pv.add_point_fn(temp_gc);
						this.pv.commit();
					}
					finally
					{
						this.pv.end_trans();
					}
					this.pv.close();
				}

			}
			catch (Exception e)
			{
				System.out.println("Error saving fieldnote!");
				e.printStackTrace();
			}

			break;
		default:
			break;
		}
	}

	public void onClickXXX(DialogInterface d, int i)
	{
		// Perform action on clicks
		// Toast.makeText(HelloFormStuff.this, "Beep Bop",
		// Toast.LENGTH_SHORT).show();
		System.out.println("button pressed");
	}

	public void turn_on_compass()
	{
		try
		{
			sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void turn_off_compass()
	{
		try
		{
			sensorManager.unregisterListener(this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause()
	{
		// System.out.println(lx.getHeight());
		// System.out.println(rose.getHeight());
		// System.out.println(cross.getHeight());
		// System.out.println(status_text.getHeight());

		// System.out.println("main: pause");

		// stop listening for GPS
		this.turn_off_locations();
		// stop compass
		this.turn_off_compass();

		this.save_settings();
		this.wdl.saveCookies();

		if (mdl != null)
		{
			mdl.interrupt();
			mdl.request_stop();
			try
			{
				mdl.join();
				// System.out.println(mdl.getState());
				// System.out.println("join mdl");
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		// close sql db
		pv.compact();
		pv.close();

		super.onPause();
	}

	public void turn_off_locations()
	{
		try
		{
			// stop listening (for any type of location)
			lm.removeUpdates(this);
			lm.removeGpsStatusListener(this);
			// we cant have a fix now
			isGPSFix = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void turn_on_locations()
	{
		// start with GPS
		this.turn_on_gps();
	}

	public void turn_on_follow_mode()
	{
		this.follow_mode = true;
		this.follow_current = this.follow_on;
	}

	public void turn_off_follow_mode()
	{
		this.follow_mode = false;
		this.follow_current = this.follow_off;
	}

	public void turn_off_gps()
	{
		try
		{

			// stop listening for GPS
			lm.removeUpdates(this);
			lm.removeGpsStatusListener(this);
			// we cant have a fix now
			isGPSFix = false;

			// start listening for network location
			lm.requestLocationUpdates(this.low.getName(), 0L, 0f, this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void turn_on_gps()
	{
		try
		{
			// stop listening for network location
			lm.removeUpdates(this);
			// assume we have no gps fix after turing on gps
			isGPSFix = false;

			// start listening for GPS
			lm.requestLocationUpdates(this.high.getName(), 0L, 0f, this);
			lm.addGpsStatusListener(this);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void onLocationChanged(Location location)
	{
		Boolean must_reload_caches_from_db = false;

		if (location == null) return;

		if (!follow_mode) return;

		// for external bluetooth GPS -----------------
		Boolean old_fix_status = isGPSFix;
		if (mLastLocation != null) isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;
		if (isGPSFix.compareTo(old_fix_status) != 0)
		{
			cross.invalidate();
		}
		mLastLocationMillis = SystemClock.elapsedRealtime();
		// for external bluetooth GPS -----------------

		mLastLocation = location;
		// check distance that we moved
		if ((cur_lat_gps_save == -1) || (cur_lon_gps_save == -1))
		{
			// System.out.println("gps 1");
			// first fix
			cur_lat_gps_save = location.getLatitude();
			cur_lon_gps_save = location.getLongitude();
		}
		else
		{
			// System.out.println("gps 2");

			// lat (48.2200 -> 48.2201) 0.0001 =~ 11.12m
			// lon (16.4000 -> 16.4002) 0.0002 =~ 14.82m

			double my_pos_delta_lat;
			double my_pos_delta_lon;

			my_pos_delta_lat = (double) (0.0001 * 4 * (20 - this.rose.zoom));
			my_pos_delta_lon = (double) (0.0002 * 4 * (20 - this.rose.zoom));

			// // lat,lon 1
			// Coordinate a = this.rose.num2deg(this.rose.map_center_x,
			// this.rose.map_center_y);
			// // lat,lon 2
			// Coordinate b = this.rose.num2deg(this.rose.map_center_x +
			// (this.rose.tile_size_x * 0.04),
			// this.rose.map_center_y + (this.rose.tile_size_y * 0.04));
			// // distance lat for 1 maptile (from pixel -> lat)
			// my_pos_delta_lat = Math.abs(b.lat - a.lat);
			// // distance lon for 1 maptile (from pixel -> lon)
			// my_pos_delta_lon = Math.abs(b.lon - a.lon);
			// // System.out.println("" + my_pos_delta_lat + " " +
			// my_pos_delta_lon);

			if (Math.abs(cur_lat_gps_save - location.getLatitude()) > my_pos_delta_lat)
			{
				// System.out.println("gps 3");

				must_reload_caches_from_db = true;
				// remember this location
				cur_lat_gps_save = location.getLatitude();
				cur_lon_gps_save = location.getLongitude();
			}
			else if (Math.abs(cur_lon_gps_save - location.getLongitude()) > my_pos_delta_lon)
			{
				// System.out.println("gps 4");

				must_reload_caches_from_db = true;
				// remember this location
				cur_lat_gps_save = location.getLatitude();
				cur_lon_gps_save = location.getLongitude();
			}
		}

		// update mapview
		this.rose.set_center(new Coordinate(location.getLatitude(), location.getLongitude()));
		this.rose.__calc_tiles_on_display();
		if (must_reload_caches_from_db)
		{
			// System.out.println("gps: reload caches from db");
			this.rose.load_caches_from_db();
		}
		if (this.current_display_view == DISPLAY_VIEW_MAP)
		{
			// System.out.println("iigc1");
			this.rose.main_object.gcview.invalidate();
		}
		this.rose.draw_me();
		// update mapview

		if (!this.global_settings.options_use_compass_heading)
		{
			// System.out.println("-> gps heading");
			this.cross.set_gps_heading(location.getBearing());
		}

		this.cross.set_gps_acc(location.getAccuracy());
		this.cross.invalidate();

		if (this.current_display_view == DISPLAY_VIEW_ARROW)
		{
			// now redraw arrow (if visible)
			this.arrowview.invalidate();
		}

		// status_text.setText(String.format("lat %.5f", location.getLatitude())
		// + " "
		// + String.format("lon %.5f", location.getLongitude()));
		this.change_status_text(String.format("lat %.5f", location.getLatitude()) + " " + String.format("lon %.5f", location.getLongitude()));
		// status_text.append(" " + String.format("%.1f",
		// this.rose.map_center_x) + " "
		// + String.format("%.1f", this.rose.map_center_y));

		// sb.append("Timestamp: ");
		// sb.append(location.getTime());
		// sb.append('\n');

		// System.out.println(sb.toString());

	}

	private Handler handle_status_text = new Handler()
	{
		public void handleMessage(Message msg)
		{
			Bundle b = msg.getData();
			int id = b.getInt("id");
			if (id == 0)
			{
				change_status_text_real(b.getString("text"));
			}
			else if (id == 1)
			{
				append_status_text_real(b.getString("text"));
			}
		}
	};

	public void change_status_text(String new_text)
	{
		Message msg = handle_status_text.obtainMessage();
		Bundle b = new Bundle();
		b.putString("text", new_text);
		b.putInt("id", 0);
		msg.setData(b);
		handle_status_text.sendMessage(msg);
	}

	public void change_status_text_real(String new_text)
	{
		this.status_text_string = new_text;
		status_text.setText(this.status_text_string + " " + this.status_append_string);
		this.status_text.postInvalidate();
	}

	public void append_status_text(String new_text)
	{
		Message msg = handle_status_text.obtainMessage();
		Bundle b = new Bundle();
		b.putString("text", new_text);
		b.putInt("id", 1);
		msg.setData(b);
		handle_status_text.sendMessage(msg);
	}

	public void append_status_text_real(String new_text)
	{
		this.status_append_string = new_text;
		status_text.setText(this.status_text_string + " " + this.status_append_string);
		this.status_text.postInvalidate();
	}

	public void onProviderDisabled(String provider)
	{
		// status_text.setText("onProviderDisabled");
		// System.out.println("onProviderDisabled");
	}

	public void onProviderEnabled(String provider)
	{
		// status_text.setText("onProviderEnabled");
		// System.out.println("onProviderEnabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		// status_text.setText("onStatusChanged " + String.valueOf(status) + " "
		// + extras.toString());
		// ** good ** // System.out.println("onStatusChanged " +
		// String.valueOf(status) + " " + extras.toString());
		// System.out.println(extras.toString());
		// GpsStatus.GPS_EVENT_SATELLITE_STATUS -> 4
		// GpsStatus.GPS_EVENT_STARTED -> 1
		// GpsStatus.GPS_EVENT_STOPPED; -> 2
	}

	public void onGpsStatusChanged(int event)
	{
		GpsStatus stat = lm.getGpsStatus(null);
		Iterable<GpsSatellite> iSatellites = stat.getSatellites();
		// System.out.println("" + String.valueOf(iSatellites.toString()));
		Iterator<GpsSatellite> it = iSatellites.iterator();

		int used_sats_new = 0;
		while (it.hasNext())
		{
			GpsSatellite oSat = (GpsSatellite) it.next();
			if (oSat.usedInFix())
			{
				used_sats_new++;
			}
			// System.out.println("SAT-info: " + oSat.toString());
		}
		if (this.used_sats != used_sats_new)
		{
			this.used_sats = used_sats_new;
			cross.set_used_sats(used_sats);
			cross.invalidate();
		}

		// for INTERNAL GPS -----------------
		Boolean old_fix_status = isGPSFix;
		switch (event)
		{
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			if (mLastLocation != null) isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;

			if (isGPSFix.compareTo(old_fix_status) != 0)
			{
				cross.invalidate();
			}
			old_fix_status = isGPSFix;

			if (isGPSFix)
			{ // A fix has been acquired.
				// Do something.
				// System.out.println("FIX found ##");
			}
			else
			{ // The fix has been lost.
				// Do something.
				// System.out.println("lost FIX ==");
			}

			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			// Do something.
			isGPSFix = true;
			// System.out.println("first FIX **");

			break;
		}
		// for INTERNAL GPS -----------------
	}

	public void upload_fieldnotes()
	{
		List<GeocacheCoordinate> caches = this.pv.get_new_fieldnotes();
		if ((caches == null) || (caches.size() == 0))
		{
			// no filednotes to upload
			Toast.makeText(getApplicationContext(), "No Fieldnotes to upload", Toast.LENGTH_SHORT).show();

		}
		else
		{
			FieldnotesUploader fu = new FieldnotesUploader(this.wdl, caches);
			boolean ret = fu.upload();
			if (ret)
			{
				Toast.makeText(getApplicationContext(), "" + caches.size() + " Fieldnotes uploaded", Toast.LENGTH_SHORT).show();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Error while uploading Fieldnotes", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void get_route_files()
	{
		File folder = new File(this.main_dir + "/routes/");
		File[] listOfFiles = folder.listFiles();
		this.current_routefile_name = null;
		this.route_file_items = new CharSequence[listOfFiles.length + 1];

		for (int i = 0; i < listOfFiles.length; i++)
		{
			if (listOfFiles[i].isFile())
			{
				System.out.println("File " + listOfFiles[i].getName());
				// current_routefile_name = listOfFiles[i].getName();
				this.route_file_items[i] = listOfFiles[i].getName();
			}
			else if (listOfFiles[i].isDirectory())
			{
				// System.out.println("Directory " +
				// listOfFiles[i].getName());
			}
		}
		// this on to clear "routes"
		this.route_file_items[listOfFiles.length] = "--None--";

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a Routefile");
		builder.setItems(route_file_items, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int j)
			{
				current_routefile_name = route_file_items[j].toString();
				System.out.println("crfn1=" + current_routefile_name);
				Toast.makeText(getApplicationContext(), "Routefile selected: " + route_file_items[j].toString(), Toast.LENGTH_SHORT).show();

				if (route_file_items[j].toString().equals("--None--"))
				{
					route_file_caches = null;
					pv.clear_filter();
					// load caches from DB, so that filter will be cleared
					rose.load_caches_from_db();
					return;
				}

				try
				{
					// Open the file
					FileInputStream fstream = new FileInputStream(main_dir + "/routes/" + current_routefile_name);
					// Get the object of DataInputStream
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String strLine;
					route_file_caches = new ArrayList<GeocacheCoordinate>();
					route_file_caches.clear();
					String new_pv_filter = "";
					String sep = "";
					// Read File Line By Line
					while ((strLine = br.readLine()) != null)
					{
						strLine = strLine.trim();
						if (!strLine.equals(""))
						{
							// Print the content on the console
							// System.out.println(strLine);
							// add GC to global list for route caches
							route_file_caches.add(new GeocacheCoordinate(0.0, 0.0, strLine));
							new_pv_filter = new_pv_filter + sep + " name='" + strLine + "' ";
							if (sep.equals(""))
							{
								sep = " or ";
							}
						}
					}
					// Close the input stream
					in.close();

					// System.out.println("new filter=" + new_pv_filter);
					pv.set_filter(new_pv_filter);
					// load caches from DB, so that filter will be active
					rose.load_caches_from_db();
				}
				catch (Exception e)
				{
					// System.err.println("Error: " + e.getMessage());
					route_file_caches = null;
					pv.clear_filter();
					// load caches from DB, so that filter will be cleared
					rose.load_caches_from_db();
				}
				alert.dismiss();
			}
		});
		this.alert = builder.create();
		this.alert.show(); // this will return immediately!!
	}

	public void change_target_coords_lat()
	{
		int num1 = 0;
		int num2 = 0;
		int num3 = 0;
		Boolean ns_ew_toggle = true;
		String toggle_text_on = "N";
		String toggle_text_off = "S";

		Coordinate.coords_d_m_m conv = this.rose.current_target.d_to_dm_m();
		// Coordinate.dm_m_to_d(conv);

		num1 = conv.lat_deg;
		num2 = conv.lat_min;
		num3 = conv.lat_min_fractions;
		ns_ew_toggle = true;
		if (!conv.lat_plus_minus)
		{
			ns_ew_toggle = false;

		}

		this.show_geocoord_picker(true, num1, num2, num3, ns_ew_toggle, toggle_text_on, toggle_text_off);
	}

	public void change_target_coords_lon()
	{
		int num1 = 0;
		int num2 = 0;
		int num3 = 0;
		Boolean ns_ew_toggle = true;
		String toggle_text_on = "E";
		String toggle_text_off = "W";

		Coordinate.coords_d_m_m conv = this.rose.current_target.d_to_dm_m();
		// Coordinate.dm_m_to_d(conv);

		num1 = conv.lon_deg;
		num2 = conv.lon_min;
		num3 = conv.lon_min_fractions;
		ns_ew_toggle = true;
		if (!conv.lon_plus_minus)
		{
			ns_ew_toggle = false;

		}

		this.show_geocoord_picker(false, num1, num2, num3, ns_ew_toggle, toggle_text_on, toggle_text_off);
	}

	public void show_geocoord_picker(Boolean is_lat, int num1, int num2, int num3, Boolean toggle, String t_on, String t_off)
	{
		this.is_lat_current_change_target_coords = is_lat;

		xx1 = new NumberPicker(this);
		xx1.setFormatter(NumberPicker.THREE_DIGIT_FORMATTER);
		xx1.setRange(0, 999);
		xx1.setCurrent(num1);
		RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(150, RelativeLayout.LayoutParams.FILL_PARENT);
		lp1.topMargin = 20;
		lp1.leftMargin = 5;
		lp1.height = 200;
		lp1.width = (int) (this.rose.mCanvasWidth * 0.3); // calc width
		lp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mainscreen_map_view.addView(xx1, lp1);

		orient_1_toggle = new ToggleButton(this);
		RelativeLayout.LayoutParams otb_1 = new RelativeLayout.LayoutParams(150, RelativeLayout.LayoutParams.FILL_PARENT);
		orient_1_toggle.setChecked(toggle);
		if (toggle)
		{
			orient_1_toggle.setText(t_on);
		}
		else
		{
			orient_1_toggle.setText(t_off);
		}
		orient_1_toggle.setTextOff(t_off);
		orient_1_toggle.setTextOn(t_on);
		otb_1.topMargin = 20 + 2;
		otb_1.rightMargin = 1;
		otb_1.leftMargin = 4 + lp1.width;
		otb_1.height = 200 - 10;
		otb_1.width = 55;
		otb_1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mainscreen_map_view.addView(orient_1_toggle, otb_1);

		// ToggleButton orient_2_toggle = new ToggleButton(this);
		// RelativeLayout.LayoutParams otb_2 = new
		// RelativeLayout.LayoutParams(150,
		// RelativeLayout.LayoutParams.FILL_PARENT);
		// orient_2_toggle.setChecked(false);
		// orient_2_toggle.setText("S");
		// orient_2_toggle.setTextOff("S");
		// orient_2_toggle.setTextOn("S");
		// otb_2.topMargin = 20 + 100;
		// otb_2.leftMargin = 5 + lp1.width;
		// otb_2.height = 100;
		// otb_2.width = 45;
		// otb_2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		// mainscreen_map_view.addView(orient_2_toggle, otb_2);

		xx2 = new NumberPicker(this);
		xx2.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
		xx2.setRange(0, 99);
		xx2.setCurrent(num2);
		RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(150, RelativeLayout.LayoutParams.FILL_PARENT);
		lp2.rightMargin = 5;
		lp2.topMargin = 20;
		lp2.height = 200;
		lp2.leftMargin = lp1.width + 2 + 55; // width of other number picker
		lp2.width = 110;
		lp2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mainscreen_map_view.addView(xx2, lp2);

		xx3 = new NumberPicker(this);
		xx3.setFormatter(NumberPicker.THREE_DIGIT_FORMATTER);
		xx3.setRange(0, 999);
		xx3.setCurrent(num3);
		RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(150, RelativeLayout.LayoutParams.FILL_PARENT);
		lp3.rightMargin = 5;
		lp3.topMargin = 20;
		lp3.height = 200;
		lp3.leftMargin = lp1.width + lp2.width + 2 + 55; // width of other
		// number picker
		lp3.width = this.rose.mCanvasWidth - lp1.width - lp2.width - 5 - 55; // take
		// rest
		// of
		// width
		lp3.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mainscreen_map_view.addView(xx3, lp3);

		RelativeLayout.LayoutParams bt_ok = new RelativeLayout.LayoutParams(150, RelativeLayout.LayoutParams.FILL_PARENT);
		btn_ok = new Button(this);
		bt_ok.width = ((int) (this.rose.mCanvasWidth * 0.4));
		bt_ok.height = 70;
		bt_ok.leftMargin = 5;
		bt_ok.topMargin = 20 + 3 + lp2.height;
		btn_ok.setText("OK");
		bt_ok.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		this.btn_ok.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				System.out.println("toggle=" + orient_1_toggle.isChecked());
				System.out.println("xx1=" + xx1.getCurrent());
				System.out.println("xx2=" + xx2.getCurrent());
				System.out.println("xx3=" + xx3.getCurrent());

				Coordinate ccc = new Coordinate(2, 2);
				Coordinate.coords_d_m_m conv2 = new Coordinate.coords_d_m_m();
				if (is_lat_current_change_target_coords)
				{
					conv2.lat_deg = xx1.getCurrent();
					conv2.lat_min = xx2.getCurrent();
					conv2.lat_min_fractions = xx3.getCurrent();
					if (orient_1_toggle.isChecked())
					{
						conv2.lat_plus_minus = true;
						conv2.lat_sign = "+";
					}
					else
					{
						conv2.lat_plus_minus = false;
						conv2.lat_sign = "-";
					}
					ccc = Coordinate.dm_m_to_d(conv2);
					ccc.lon = rose.current_target.lon;
				}
				else
				{
					conv2.lon_deg = xx1.getCurrent();
					conv2.lon_min = xx2.getCurrent();
					conv2.lon_min_fractions = xx3.getCurrent();
					if (orient_1_toggle.isChecked())
					{
						conv2.lon_plus_minus = true;
						conv2.lon_sign = "+";
					}
					else
					{
						conv2.lon_plus_minus = false;
						conv2.lon_sign = "-";
					}
					ccc = Coordinate.dm_m_to_d(conv2);
					ccc.lat = rose.current_target.lat;
				}
				System.out.println("ccc lat=" + ccc.lat);
				System.out.println("ccc lon=" + ccc.lon);
				// now set new target coords
				GeocacheCoordinate tmp_gc2 = new GeocacheCoordinate(ccc.lat, ccc.lon, "*GCmanual*");
				tmp_gc2.title = "*manual target";
				rose.current_target = tmp_gc2;

				mainscreen_map_view.removeView(btn_cancel);
				mainscreen_map_view.removeView(btn_ok);
				mainscreen_map_view.removeView(orient_1_toggle);
				mainscreen_map_view.removeView(xx1);
				mainscreen_map_view.removeView(xx2);
				mainscreen_map_view.removeView(xx3);
			}
		});
		mainscreen_map_view.addView(btn_ok, bt_ok);

		RelativeLayout.LayoutParams bt_cl = new RelativeLayout.LayoutParams(150, RelativeLayout.LayoutParams.FILL_PARENT);
		btn_cancel = new Button(this);
		bt_cl.width = ((int) (this.rose.mCanvasWidth * 0.4));
		bt_cl.height = 70;
		bt_cl.topMargin = 20 + 3 + lp2.height;
		bt_cl.leftMargin = this.rose.mCanvasWidth - 5 - bt_cl.width;
		bt_cl.rightMargin = 5;
		btn_cancel.setText("Cancel");
		bt_cl.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		this.btn_cancel.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				mainscreen_map_view.removeView(btn_cancel);
				mainscreen_map_view.removeView(btn_ok);
				mainscreen_map_view.removeView(orient_1_toggle);
				mainscreen_map_view.removeView(xx1);
				mainscreen_map_view.removeView(xx2);
				mainscreen_map_view.removeView(xx3);
			}
		});
		mainscreen_map_view.addView(btn_cancel, bt_cl);
		// mainscreen_map_view.removeView(btn_cancel);
	}

	public void navigate_to_target()
	{
		// save before starting new activity!!
		this.save_settings();
		try
		{
			// try to start navi. activity
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + this.rose.current_target.lat + "," + this.rose.current_target.lon)));
		}
		catch (Exception e)
		{
			// System.out.println("Error starting navigation");
			Toast.makeText(getApplicationContext(), "Error starting navigation!", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	public void show_cache_page_in_browser()
	{
		// save before starting new activity!!

		// http://www.geocaching.com/seek/cache_details.aspx?guid=b89040ae-5bca-4a14-960b-d9dcbe645d8a
		// or
		// http://www.geocaching.com/seek/cache_details.aspx?wp=GC2EVEY

		// System.out.println("1=" + this.cacheview.gc_name_current);
		// System.out.println("2=" + this.cacheview.gc_name_previous);
		// System.out.println("3=" + this.cacheview.details_loaded);
		// System.out.println("4=" + String.valueOf(this.cacheview.gc));

		this.save_settings();
		try
		{
			// try to start browser activity
			// old form, no more supported // startActivity(new
			// Intent(Intent.ACTION_VIEW,
			// ContentURI.create("http://www.myurl.com")));
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/seek/cache_details.aspx?wp=" + cacheview.gc.name)));
		}
		catch (Exception e)
		{
			// System.out.println("Error starting navigation");
			Toast.makeText(getApplicationContext(), "Error starting Browser!", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	// @Override
	public boolean XXXXXXXXXXXXXXonKeyDown(int keyCode, KeyEvent event)
	{
		int i;
		String s = null;
		i = event.getUnicodeChar();
		System.out.println("onKeyDown " + keyCode + " " + i);
		if (i == 0)
		{
			if (keyCode == android.view.KeyEvent.KEYCODE_DEL)
			{
				s = java.lang.String.valueOf((char) 8);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_MENU)
			{
				s = java.lang.String.valueOf((char) 1);
				return false;
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_SEARCH)
			{
				s = java.lang.String.valueOf((char) 19);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_BACK)
			{
				s = java.lang.String.valueOf((char) 27);
				return false;
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_CALL)
			{
				s = java.lang.String.valueOf((char) 3);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP)
			{
				s = java.lang.String.valueOf((char) 21);
				System.out.println("ss " + s);
				return false;
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN)
			{
				s = java.lang.String.valueOf((char) 4);
				System.out.println("ss " + s);
				return false;
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER)
			{
				s = java.lang.String.valueOf((char) 13);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN)
			{
				s = java.lang.String.valueOf((char) 16);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT)
			{
				s = java.lang.String.valueOf((char) 2);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
			{
				s = java.lang.String.valueOf((char) 6);
			}
			else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP)
			{
				s = java.lang.String.valueOf((char) 14);
			}
		}
		else if (i == 10)
		{
			s = java.lang.String.valueOf((char) 13);
		}
		else
		{
			s = java.lang.String.valueOf((char) i);
		}
		if (s != null)
		{
			// KeypressCallback(KeypressCallbackID, s);
			System.out.println("s: " + s);
		}
		return true;
	}

}
