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


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class PostLogEntryActivity extends Activity
{
	private EditText			msg;
	private RadioButton		log_type_found;
	private RadioButton		log_type_not_found;
	private RadioButton		log_type_note;

	private RadioGroup		group_log_type;

	public RelativeLayout	mainscreen_map_view;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		LinearLayout panel = new LinearLayout(this);
		panel.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		panel.setOrientation(LinearLayout.VERTICAL);

		// log type selection:
		TextView lbllogtype = new TextView(this);
		lbllogtype.setText("Logtype");
		lbllogtype.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		lbllogtype.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));


		log_type_found = new RadioButton(this);
		log_type_found.setText(GeocacheCoordinate.LOG_AS_HASH.get(GeocacheCoordinate.LOG_AS_FOUND));
		log_type_found.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));

		log_type_not_found = new RadioButton(this);
		log_type_not_found.setText(GeocacheCoordinate.LOG_AS_HASH
				.get(GeocacheCoordinate.LOG_AS_NOTFOUND));
		log_type_not_found.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));

		log_type_note = new RadioButton(this);
		log_type_note.setText(GeocacheCoordinate.LOG_AS_HASH.get(GeocacheCoordinate.LOG_AS_NOTE));
		log_type_note.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));

		group_log_type = new RadioGroup(this);
		group_log_type.addView(log_type_found);
		group_log_type.addView(log_type_not_found);
		group_log_type.addView(log_type_note);

		log_type_found.setChecked(true);
		log_type_not_found.setChecked(false);
		log_type_note.setChecked(false);


		// Userid : label and text field
		TextView lblUserid = new TextView(this);
		lblUserid.setText("Fieldnote");
		lblUserid.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		lblUserid.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		msg = new EditText(this);
		msg.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		//  ttfUserid.setText("a");

		// login button
		final Button btnLogin = new Button(this);
		btnLogin.setText("Save");
		btnLogin
				.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		btnLogin.setGravity(Gravity.CENTER);
		btnLogin.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				int l_type = GeocacheCoordinate.LOG_AS_FOUND;
				if (log_type_found.isChecked() == true)
				{
					l_type = GeocacheCoordinate.LOG_AS_FOUND;
				}
				else if (log_type_not_found.isChecked() == true)
				{
					l_type = GeocacheCoordinate.LOG_AS_NOTFOUND;
				}
				else if (log_type_note.isChecked() == true)
				{
					l_type = GeocacheCoordinate.LOG_AS_NOTE;
				}
				executeDone(l_type);
			}
		});


		// title
		try
		{
			String s = getIntent().getExtras().getString("title");
			if (s.length() > 0)
			{
				this.setTitle(s);
			}
		}
		catch (Exception e)
		{
		}

		// fieldnote text
		try
		{
			msg = new EditText(this);
			msg.setText(getIntent().getExtras().getString("msg"));
		}
		catch (Exception e)
		{
		}


		// actually adding the views (that have layout set on them) to the panel
		// selection of log type
		panel.addView(lbllogtype);
		panel.addView(group_log_type);
		// fieldnote text
		panel.addView(lblUserid);
		panel.addView(msg);
		// savebutton
		panel.addView(btnLogin);

		setContentView(panel);

	}

	@Override
	public void onBackPressed()
	{
		executeDone(GeocacheCoordinate.LOG_AS_FOUND);
		super.onBackPressed();
	}

	private void executeDone(int logtype)
	{
		Intent resultIntent = new Intent();
		resultIntent.putExtra("msg", PostLogEntryActivity.this.msg.getText().toString());
		resultIntent.putExtra("logtype", logtype);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}


}
