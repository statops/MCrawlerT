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
 * Based (with some changes) on:
 * 	http://code.google.com/p/android-color-picker/
 * which is available under the Apache 2 license.
 */

package com.morphoss.acal.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.morphoss.acal.activity.ColourPickerDialog.OnColourPickerListener;

public class ColourPickerPreference extends DialogPreference  {
	public static final String TAG = "aCal ColourPickerPreference";

	private Context context;
	private ColourPickerDialog dialog;
	private int colour = 0xFF808080;
	
	public ColourPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}
	
	@Override
	public void onDialogClosed(boolean positiveResult) {
		this.dialog = null;
	}
	
	public int getColour() {
		return this.colour;
	}
	
	public void setColor(int colour) {
		this.colour = colour;
	}
	
	protected void showDialog() {
		dialog.show();
		
	}
	
	@Override
	public View getView(View convertView, ViewGroup parent) {
		View v = super.getView(convertView, parent);
		TextView tv = (TextView) v.findViewById(android.R.id.title);
		colour = getPersistedInt(colour);
		tv.setTextColor(this.colour);
		return v;
	}
	
	
	public void onClick(DialogInterface dialog, int whichButton) {
		switch (whichButton) {
			case Dialog.BUTTON_POSITIVE: {
				this.colour = this.dialog.selectedColour;
				this.persistInt(colour);
				this.callChangeListener(this.dialog.selectedColour);
				break;
			}
		}
	}
	
	protected View onCreateDialogView() {
	dialog =  new ColourPickerDialog(this.context,colour, new OnColourPickerListener() {

			@Override
			public void onCancel(ColourPickerDialog dialog) {
				ColourPickerPreference.this.callChangeListener(colour);				
			}

			@Override
			public void onOk(ColourPickerDialog dialog, int color) {
				ColourPickerPreference.this.colour = color;
				ColourPickerPreference.this.callChangeListener(color);
			}
			
		});
		return dialog.primaryView;
	}
}
