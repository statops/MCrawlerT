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
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class TextEntryActivity extends Activity
{
	private EditText			u;
	private EditText			p;

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

		// Userid : label and text field
		TextView lblUserid = new TextView(this);
		lblUserid.setText("Userid");
		lblUserid.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		lblUserid.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		u = new EditText(this);
		u.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		//  ttfUserid.setText("a");

		// Password : label and text field
		TextView lblPassword = new TextView(this);
		lblPassword.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		lblPassword.setText("Password");
		lblPassword.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		p = new EditText(this);
		p.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		p.setTransformationMethod(new PasswordTransformationMethod());
		//  ttfPassword.setText("a");

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
				executeDone();
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

		// username
		try
		{
			u = new EditText(this);
			u.setSingleLine();
			u.setText(getIntent().getExtras().getString("username"));
		}
		catch (Exception e)
		{
		}

		// password
		try
		{
			p = new EditText(this);
			p.setSingleLine();
			p.setText(getIntent().getExtras().getString("password"));
			p.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
		}
		catch (Exception e)
		{
		}


		// actually adding the views (that have layout set on them) to the panel
		// userid
		panel.addView(lblUserid);
		panel.addView(u);
		// password
		panel.addView(lblPassword);
		panel.addView(p);
		// loginbutton
		panel.addView(btnLogin);

		setContentView(panel);


		//
		//
		//
		//
		//
		//
		//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
		//				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		//
		//		requestWindowFeature(0);
		//		LayoutParams layoutParams_mainscreen_map_view = new RelativeLayout.LayoutParams(200, 300);
		//		mainscreen_map_view = new RelativeLayout(this);
		//		mainscreen_map_view.setLayoutParams(layoutParams_mainscreen_map_view);
		//		setContentView(mainscreen_map_view);
		//
		//		// title
		//		try
		//		{
		//			String s = getIntent().getExtras().getString("title");
		//			if (s.length() > 0)
		//			{
		//				this.setTitle(s);
		//			}
		//		}
		//		catch (Exception e)
		//		{
		//		}
		//
		//		// username
		//		try
		//		{
		//			u = new EditText(this);
		//			u.setText(getIntent().getExtras().getString("username"));
		//		}
		//		catch (Exception e)
		//		{
		//		}
		//
		//		// password
		//		try
		//		{
		//			p = new EditText(this);
		//			p.setText(getIntent().getExtras().getString("password"));
		//		}
		//		catch (Exception e)
		//		{
		//		}
		//
		//
		//		// button
		//		Button button = new Button(this);
		//		button.setOnClickListener(new OnClickListener()
		//		{
		//			public void onClick(View v)
		//			{
		//				executeDone();
		//			}
		//		});
		//
		//		RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(200, 55);
		//		lp1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		//		mainscreen_map_view.addView(u, lp1);
		//
		//		RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(200, 55);
		//		lp2.addRule(RelativeLayout.CENTER_VERTICAL);
		//		mainscreen_map_view.addView(p, lp2);
		//
		//		RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(200, 55);
		//		lp3.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//		mainscreen_map_view.addView(button, lp3);
	}

	@Override
	public void onBackPressed()
	{
		executeDone();
		super.onBackPressed();
	}

	private void executeDone()
	{
		Intent resultIntent = new Intent();
		resultIntent.putExtra("username", TextEntryActivity.this.u.getText().toString());
		resultIntent.putExtra("password", TextEntryActivity.this.p.getText().toString());
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

}
