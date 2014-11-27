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

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.morphoss.acal.AcalTheme;
import com.morphoss.acal.R;

public class AddServerList extends ListActivity implements OnClickListener {

	public static final String TAG = "acal AddServerList";

	public static final int KEY_CREATE_SERVER_REQUEST = 1;
	private Button manualConfiguration;
	public static final int ACTION_LOGIN = 1;
	public static final int ACTION_CREATE = 2;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.servers_list);
		updateListView();
		
		manualConfiguration = (Button) findViewById(R.id.AddServerButton);
		AcalTheme.setContainerFromTheme(manualConfiguration, AcalTheme.BUTTON);
		manualConfiguration.setOnClickListener(this);
		manualConfiguration.setText(getString(R.string.NewManualServerConfiguration));
		manualConfiguration.setTextSize(24);
	}
	
	private void updateListView() {
		// Bind to new adapter.
		setListAdapter(new AddServerListAdapter(this));

		// make sure the display is refreshed
		this.getListView().refreshDrawableState();		
	}

	@Override
	public void onClick(View v) {
		ContentValues newServer;
		
		newServer = new ContentValues();
		newServer.put(ServerConfiguration.KEY_MODE, ServerConfiguration.MODE_CREATE);

		Intent serverConfigIntent = new Intent();

		// Begin new activity
		serverConfigIntent.setClassName("com.morphoss.acal", "com.morphoss.acal.activity.serverconfig.ServerConfiguration");
		serverConfigIntent.putExtra("ServerData", newServer);
		startActivityForResult(serverConfigIntent, KEY_CREATE_SERVER_REQUEST);
	}
	
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	 if (requestCode == KEY_CREATE_SERVER_REQUEST ) {
             if (resultCode == RESULT_OK) finish();
         }
     }


 	public void signUp( String signUpUrl ) {
 		Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(signUpUrl));
 		this.startActivity(viewIntent);
 	}
     
}
