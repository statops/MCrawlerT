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

package com.morphoss.acal.activity.serverconfig;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;
import com.morphoss.acal.providers.Servers;

public class AddServerListAdapter extends BaseAdapter {

	public static final String TAG = "Acal AddServerListAdapter";
	private AddServerList context;
	private ArrayList<ContentValues> data;
	private int lastSavedConfig=0;
	
	public AddServerListAdapter(AddServerList c) {
		this.context = c;
		populateData();
	}
	
	private void populateData() {
		data = new ArrayList<ContentValues>();
		
		lastSavedConfig=0;

		//first find all 'acal' files in appropriate directories
		try {
			File publicDir = new File(Constants.PUBLIC_DATA_DIR);
			String[] acalFiles = publicDir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.substring(name.length()-5).equalsIgnoreCase(".acal");
				}
			});
		
			if ( acalFiles != null ) {
				for (String filename : acalFiles) {
					try {
						List<ServerConfigData> l = ServerConfigData.getServerConfigDataFromFile(new File(publicDir.getAbsolutePath()+"/"+filename));
						for (ServerConfigData scd : l) {
							
							ContentValues cv = scd.getContentValues();
							cv.put(ServerConfiguration.KEY_MODE, ServerConfiguration.MODE_IMPORT);
							data.add(cv);
							lastSavedConfig++;;
						}
					} catch (Exception e) {
						Log.e(TAG, "Error parsing file: "+filename+" - "+e);
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error reading file list: "+e);
			Log.d(TAG,Log.getStackTraceString(e));
		}

		
		// Now we list all the preconfigured setups...
		//A Bit of magic to get all the right files from /raw
		Map<String,Integer> list = new HashMap<String,Integer>();
		ContentValues imageIds = new ContentValues();
		Field[] fields = R.raw.class.getFields();
		for(Field f : fields) {
			try {
				String name = f.getName();
				if (name == null || name.length() < 11) continue;
				if (name.substring(0,10).equalsIgnoreCase("serverconf")) { 
					list.put(name.substring(11), f.getInt(null));
					if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
								"Found preconfigured setup for '"+name.substring(11)+"'");
				}
				else if (name.substring(0,11).equalsIgnoreCase("serverimage")) {
					imageIds.put(name.substring(12), f.getInt(null));
					if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
							"Found image for '"+name.substring(12)+"'");
				}
				else if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
						"Skipping raw file '"+name+"'");
			} catch (IllegalArgumentException e) {
				Log.w(TAG,"Problem adding preconfigured setup.", e);
		    } catch (IllegalAccessException e) { 
				Log.w(TAG,"Problem adding preconfigured setup.", e);
		    }
		}
		    
		for ( Entry<String,Integer> confEntry : list.entrySet()) {
			try {
				InputStream in = context.getResources().openRawResource(confEntry.getValue());
				List<ServerConfigData> l = ServerConfigData.getServerConfigDataFromFile(in);
				if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
						"Found "+l.size()+" pre-written serverconfig entries for '"+confEntry.getKey()+"'");
				for (ServerConfigData scd : l) {
					ContentValues cv = scd.getContentValues();
					cv.put(ServerConfiguration.KEY_MODE, ServerConfiguration.MODE_IMPORT);
					if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
							"Added pre-written serverconfig for '"+confEntry.getKey()+"'");
					if ( imageIds.getAsInteger(confEntry.getKey()) != null ) {
						cv.put(ServerConfiguration.KEY_IMAGE, imageIds.getAsInteger(confEntry.getKey()));
						if ( Constants.LOG_DEBUG ) Log.println(Constants.LOGD,TAG, 
								"Setup image for '"+confEntry.getKey()+"'");
					}
					data.add(cv);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error parsing file: "+e);
			}
		}
		
	}
	
	@Override
	public int getCount() {
		return this.data.size(); //number of servers
	}

	@Override
	public Object getItem(int id) {
		return data.get(id);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convert, ViewGroup parent) {
		RelativeLayout rowLayout;

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TextView title = null, blurb = null;
		View icon = null;
		rowLayout = (RelativeLayout) inflater.inflate(R.layout.add_server_list_item, parent, false);

		title = (TextView) rowLayout.findViewById(R.id.AddServerItemTitle);
		blurb = (TextView) rowLayout.findViewById(R.id.AddServerItemBlurb);
		icon = rowLayout.findViewById(R.id.AddServerItemIcon);
		RelativeLayout thisRow = (RelativeLayout) rowLayout.findViewById(R.id.AddServerItem);
		
		final ContentValues item;
		boolean preconfig=false;
		item = data.get(position);

		preconfig = (position >= this.lastSavedConfig);
		
		//Icon
		if (!preconfig) {
			icon.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.icon));
			title.setText(item.getAsString(Servers.FRIENDLY_NAME));
			StringBuilder blurbString = new StringBuilder("");
			if ( item.getAsString(Servers.HOSTNAME) != null && !item.getAsString(Servers.HOSTNAME).equals("null")) {
				blurbString.append(item.getAsString(Servers.HOSTNAME));
				if ( item.getAsString(Servers.PRINCIPAL_PATH) != null ) blurbString.append(item.getAsString(Servers.PRINCIPAL_PATH));
			}
			else if ( item.getAsString(Servers.SUPPLIED_USER_URL) != null && !item.getAsString(Servers.SUPPLIED_USER_URL).equals("null") )
				blurbString.append(item.getAsString(Servers.SUPPLIED_USER_URL)); 
			
			if ( blurbString.equals("") ) blurbString.append(context.getString(R.string.SavedServerConfigurationBlurb)); 
			blurb.setText(blurbString);
		}
		else {
			if ( item.getAsInteger(ServerConfiguration.KEY_IMAGE) != null ) {
				Log.w(TAG, "Special lastSavedConfig image for '"+item.getAsString(Servers.FRIENDLY_NAME)+"'");
				thisRow.setBackgroundColor(context.getResources().getColor(android.R.color.white));
				thisRow.setBackgroundResource(item.getAsInteger(ServerConfiguration.KEY_IMAGE));
				icon.setBackgroundColor(0);
				title.setText(item.getAsString(""));
				blurb.setText(item.getAsString(""));
			}
			else {
				icon.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.question_icon));
				title.setText(item.getAsString(Servers.FRIENDLY_NAME));
				blurb.setText(item.getAsString("INFO"));
			}
			
		}
		rowLayout.setTag(item);
		rowLayout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Create Intent to start new Activity
				Intent serverConfigIntent = new Intent();

				// Begin new activity
				serverConfigIntent.setClassName("com.morphoss.acal",
						"com.morphoss.acal.activity.serverconfig.ServerConfiguration");
				serverConfigIntent.putExtra("ServerData", item);
				context.startActivityForResult(serverConfigIntent, AddServerList.KEY_CREATE_SERVER_REQUEST);
			}
		});
		return rowLayout;
	}
}
