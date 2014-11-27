package com.morphoss.acal.desktop;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.morphoss.acal.AcalDebug;
import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.aCal;
import com.morphoss.acal.acaltime.AcalDateTime;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheObject;
import com.morphoss.acal.database.cachemanager.requests.CRGetNextNObjects;
import com.morphoss.acal.dataservice.Collection;

public class ShowUpcomingWidgetProvider extends AppWidgetProvider {
	
	public static final String TAG = "aCal ShowUpcomingWidgetProvider";
	
	public static final int NUMBER_OF_EVENTS_TO_SHOW = 4;
	public static final int NUM_DAYS_TO_LOOK_AHEAD = 7;
	
	public static final String TABLE = "show_upcoming_widget_data";
	
	public static final String FIELD_ID = "_id";
	public static final String FIELD_RESOURCE_ID = "resource_id";
	public static final String FIELD_COLOUR = "colour";
	public static final String FIELD_DTSTART = "dtstart";
	public static final String FIELD_DTEND = "dtend";
	public static final String FIELD_SUMMARY = "summary";

	public static final String SHOW_UPCOMING_WIDGET_IDS_KEY ="acalshowupcomingwidgetids";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			if ( intent.hasExtra(SHOW_UPCOMING_WIDGET_IDS_KEY) ) {
				int[] ids = intent.getExtras().getIntArray(SHOW_UPCOMING_WIDGET_IDS_KEY);
				this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
				if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "Widget onReceive ended update");
			}
			else
				super.onReceive(context, intent);
		}
		catch ( Exception e ) {
			Log.w(TAG, "Unexpected exception in OnReceive", e);
		}
	}

	
	@TargetApi(10)
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		if (Constants.LOG_DEBUG && Constants.debugWidget) Log.println(Constants.LOGD, TAG,
				"onUpdate Called...");
		if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "Widget onUpdate started");
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		for (int widgetId : appWidgetIds) {

			Intent updateIntent = new Intent();	
			updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			updateIntent.putExtra(SHOW_UPCOMING_WIDGET_IDS_KEY, appWidgetIds);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			if (Constants.LOG_DEBUG && Constants.debugWidget) Log.println(Constants.LOGD, TAG,
					"Processing for widget id: "+widgetId);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			boolean prefer24Hour = prefs.getBoolean(context.getString(R.string.prefTwelveTwentyfour),false);
			
			
			RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.show_upcoming_widget_layout);
			if ( Build.VERSION.SDK_INT >= 7 ) {
				try {
					views.removeAllViews(R.id.upcoming_container);
				}
				catch( Exception e ) {
					Log.i(TAG,"Probably running an old version of Android :-(",e);
				}
			}
			else {
				ViewGroup upcomingContainerGroup = (ViewGroup) inflater.inflate(R.layout.show_upcoming_widget_layout, null);
				upcomingContainerGroup.removeAllViews();
			}
			
			//Used to calculate when we should trigger an update.
			long timeOfNextEventEnd = Long.MAX_VALUE;
			long timeOfNextEventStart = Long.MAX_VALUE;

			//set up on click intent
			Intent startApp = new Intent(context, aCal.class);
			PendingIntent onClickIntent = PendingIntent.getActivity(context, 0, startApp, PendingIntent.FLAG_UPDATE_CURRENT);
				
			//Get Data
			ArrayList<CacheObject> data = getCurrentData(context);
			for (CacheObject object : data) {
				if (Constants.LOG_VERBOSE) Log.println(Constants.LOGV, TAG, "Processing event "+object.getSummary());

				try {
					AcalDateTime dtstart = object.getStartDateTime();
					AcalDateTime dtend = object.getEndDateTime();

					//inflate row
					RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.show_upcoming_widget_base_row);
					LayoutInflater lf = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ShowUpcomingRowLayout rowLayout = (ShowUpcomingRowLayout)lf.inflate(R.layout.show_upcoming_widget_custom_row, null);

					row.setImageViewBitmap(R.id.upcoming_row_image, rowLayout.setData(
								Collection.getInstance(object.getCollectionId(), context).getColour(),
								object.getSummary(),
								getNiceDateTime(context,dtstart,dtend,prefer24Hour) ));

					row.setOnClickPendingIntent(R.id.upcoming_row, onClickIntent);
					row.setOnClickPendingIntent(R.id.upcoming_row_image, onClickIntent);
					
					if (timeOfNextEventEnd > dtend.getMillis()) timeOfNextEventEnd = dtend.getMillis();
					if (timeOfNextEventStart > dtstart.getMillis()) timeOfNextEventStart = dtstart.getMillis();
					
					//addview
					views.addView(R.id.upcoming_container, row);
				}
				catch( Exception e ) {
					Log.e(TAG,"Error getting widget datetime",e);
				}
				
			}
			if ( data.isEmpty()) {
				//inflate row
				RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.show_upcoming_widget_base_row);
				LayoutInflater lf = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				ShowUpcomingRowLayout rowLayout = (ShowUpcomingRowLayout)lf.inflate(R.layout.show_upcoming_widget_custom_row, null);

				row.setImageViewBitmap(R.id.upcoming_row_image,
						rowLayout.setData(Color.BLACK,context.getString(R.string.noScheduledEvents), "" ));

				row.setOnClickPendingIntent(R.id.upcoming_row, onClickIntent);
				row.setOnClickPendingIntent(R.id.upcoming_row_image, onClickIntent);
				
				//addview
				views.addView(R.id.upcoming_container, row);
			}
			
			
			//views.setOnClickPendingIntent(R.id.upcoming_container, pendingIntent);
			
			if (Constants.LOG_DEBUG && Constants.debugWidget) Log.println(Constants.LOGD, TAG, 
					"Processing widget "+widgetId+" completed.");
		
			appWidgetManager.updateAppWidget(widgetId, views);
			
			//schedule alarm to wake us if next event starts/ends within refresh period (30 mins);
			//ignore start if negative
			long now = System.currentTimeMillis();
			long start = timeOfNextEventStart - now;
			long end = timeOfNextEventEnd - now;
			long timeTillNextAlarm = end;
			if (start > 0 && start<end) timeTillNextAlarm = start;
			
			if (Constants.LOG_DEBUG && Constants.debugWidget) Log.println(Constants.LOGD, TAG, 
					"Next Event start/finish = "+timeTillNextAlarm);
			if (timeTillNextAlarm< 1800000L) {
				if (Constants.LOG_DEBUG && Constants.debugWidget) Log.println(Constants.LOGD, TAG, 
						"Setting update alarm for "+(timeTillNextAlarm/1000)+" seconds from now. due to event starting or ending");
				// Get the AlarmManager service
				 AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				 am.set(AlarmManager.RTC, System.currentTimeMillis()+timeTillNextAlarm, pendingIntent);
			} else {
				if (Constants.LOG_DEBUG && Constants.debugWidget) Log.println(Constants.LOGD, TAG, 
						"No Events starting or ending in the next 30 mins, not setting alarm.");
			}
		}
	}
	

	
	/**
	 * Get the current next V events and return as an array of CacheObjects
	 * Array Size is always <= NUMBER_OF_EVENTS_TO_SHOW, Returned array is in order of events
	 * 
	 * @param context
	 * @return
	 */
	public synchronized static ArrayList<CacheObject> getCurrentData(Context context) {
		if ( Constants.debugHeap ) AcalDebug.heapDebug(TAG, "Widget getCurrentData");
		if (Constants.LOG_DEBUG) Log.println(Constants.LOGD, TAG, "Retrieving current data");
		
		return CacheManager.getInstance(context).sendRequest(CRGetNextNObjects.GetNextNEvents(NUMBER_OF_EVENTS_TO_SHOW)).result();
	}

	
	
	private String getNiceDateTime(Context context, AcalDateTime start, AcalDateTime end, boolean use24HourFormat) {
		AcalDateTime now = new AcalDateTime().applyLocalTimeZone();

		String time;
		DateFormat format;
		if (use24HourFormat) format = new SimpleDateFormat("HH:mm");
		else format = new SimpleDateFormat("hh:mmaa");
		if ( start.getMillis() <= System.currentTimeMillis() ) {
			if ( end.getMillis() < System.currentTimeMillis() ) {
				return context.getString(R.string.Finished);
			}
			else if (end.getYear() == now.getYear() && end.getYearDay() == now.getYearDay())
				time = format.format(end.toJavaDate()).toLowerCase();
			else { 
				if ( now.getDurationTo(end).getDays() == 0 ) {
					return context.getString(R.string.Today);
				}
				else {
					//multiday event
					time = now.getDurationTo(end).getDays()+" "+context.getString(R.string.days);
				}
			}
		}
		else time = format.format(start.toJavaDate()).toLowerCase();

		if (start.getMillis() <= System.currentTimeMillis() ) {
			return context.getString(R.string.endsAt, time); ///Event is occuring now
		}
		else if ( start.getEpochDay() == now.getEpochDay() ) {
			return time;  // Leave day of week off to identify it as 'today'
		}
		else {
			StringBuilder result = new StringBuilder(time);
			result.append(" (");
			int dow = start.getWeekDay();
			switch (dow) {
				case AcalDateTime.MONDAY: result.append(context.getString(R.string.Mon)); break;
				case AcalDateTime.TUESDAY: result.append(context.getString(R.string.Tue)); break;
				case AcalDateTime.WEDNESDAY: result.append(context.getString(R.string.Wed)); break;
				case AcalDateTime.THURSDAY: result.append(context.getString(R.string.Thu)); break;
				case AcalDateTime.FRIDAY: result.append(context.getString(R.string.Fri)); break;
				case AcalDateTime.SATURDAY: result.append(context.getString(R.string.Sat)); break;
				case AcalDateTime.SUNDAY: result.append(context.getString(R.string.Sun)); break;
			}
			result.append(")");
			return result.toString();
		}
	}
	
}
