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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

import com.morphoss.acal.Constants;
import com.morphoss.acal.R;

@SuppressWarnings("deprecation")
public class ColourPickerDialog {
	private static final String TAG = "aCal ColourPickerDialog";
	private AlertDialog dialog;
	private OnColourPickerListener listener;
	private View viewHue;
	private ColourPickerView viewKotak;
	private ImageView hueSlider;
	private View viewWarnaLama;
	private View viewWarnaBaru;
	private ImageView viewPointer;
	public View primaryView;
	
	private float satudp;
	public int selectedColour;
	private float hue;
	private float sat;
	private float val;
	private float ukuranUiDp = 240.f;
	private float ukuranUiPx; // diset di constructor
	
	public interface OnColourPickerListener {
		void onCancel(ColourPickerDialog dialog);
		void onOk(ColourPickerDialog dialog, int color);
	}
	
	
	
	public ColourPickerDialog(Context context, int color, OnColourPickerListener listener) {
		this.listener = listener;
		this.selectedColour = color;
		Color.colorToHSV(color, tmp01);
		this.hue = tmp01[0];
		this.sat = tmp01[1];
		this.val = tmp01[2];
		
		this.satudp = context.getResources().getDimension(R.dimen.ambilwarna_satudp);
		this.ukuranUiPx = ukuranUiDp * satudp;
		if (Constants.LOG_DEBUG)Log.d(TAG, "satudp = " + satudp + ", ukuranUiPx=" + ukuranUiPx);  //$NON-NLS-1$//$NON-NLS-2$
		
		View view = LayoutInflater.from(context).inflate(R.layout.colourpicker_dialog, null);
		this.primaryView = view;
		this.viewHue = view.findViewById(R.id.colourpicker_viewHue);
		this.viewKotak = (ColourPickerView) view.findViewById(R.id.ambilwarna_viewKotak);
		this.hueSlider = (ImageView) view.findViewById(R.id.ambilwarna_panah);
		this.viewWarnaLama = view.findViewById(R.id.ambilwarna_warnaLama);
		this.viewWarnaBaru = view.findViewById(R.id.ambilwarna_warnaBaru);
		this.viewPointer = (ImageView) view.findViewById(R.id.colourpicker_pointer);

		positionSlider();
		positionPointer();
		this.viewKotak.setHue(hue);
		this.viewWarnaLama.setBackgroundColor(color);
		this.viewWarnaBaru.setBackgroundColor(color);

		this.viewHue.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE 
						|| event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_UP) {
					
					float y = event.getY(); // dalam px, bukan dp
					if (y < 0.f) y = 0.f;
					if (y > ukuranUiPx) y = ukuranUiPx - 0.001f;
					
					hue = 360.f - 360.f / ukuranUiPx * y;
					if (hue == 360.f) hue = 0.f;
					
					selectedColour = hitungWarna();
					// update view
					viewKotak.setHue(hue);
					positionSlider();
					viewWarnaBaru.setBackgroundColor(selectedColour);
					
					return true;
				}
				return false;
			}
		});
		
		this.viewKotak.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_MOVE 
						|| event.getAction() == MotionEvent.ACTION_DOWN
						|| event.getAction() == MotionEvent.ACTION_UP) {
					
					float x = event.getX(); // dalam px, bukan dp
					float y = event.getY(); // dalam px, bukan dp
					
					if (x < 0.f) x = 0.f;
					if (x > ukuranUiPx) x = ukuranUiPx;
					if (y < 0.f) y = 0.f;
					if (y > ukuranUiPx) y = ukuranUiPx;

					sat = (1.f / ukuranUiPx * x);
					val = 1.f - (1.f / ukuranUiPx * y);

					selectedColour = hitungWarna();
					// update view
					positionPointer();
					viewWarnaBaru.setBackgroundColor(selectedColour);
					
					return true;
				}
				return false;
			}
		});
		
		AlertDialog.Builder db  = new AlertDialog.Builder(context);
		db.setView(view);
		
		db.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (ColourPickerDialog.this.listener != null) {
					ColourPickerDialog.this.listener.onOk(ColourPickerDialog.this, selectedColour);
				}
			}
		});
		
		db.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (ColourPickerDialog.this.listener != null) {
					ColourPickerDialog.this.listener.onCancel(ColourPickerDialog.this);
				}
			}
		});
		
		this.dialog = db.create();
		
	}
	
	protected void positionSlider() {
		float y = ukuranUiPx - (hue * ukuranUiPx / 360.f);
		if (y == ukuranUiPx) y = 0.f;
		
		AbsoluteLayout.LayoutParams layoutParams = (AbsoluteLayout.LayoutParams) hueSlider.getLayoutParams();
		layoutParams.y = (int) (y + 4);
		hueSlider.setLayoutParams(layoutParams);
	}

	protected void positionPointer() {
		float x = sat * ukuranUiPx;
		float y = (1.f - val) * ukuranUiPx;
		
		AbsoluteLayout.LayoutParams layoutParams = (AbsoluteLayout.LayoutParams) viewPointer.getLayoutParams();
		layoutParams.x = (int) (x + 3);
		layoutParams.y = (int) (y + 3);
		viewPointer.setLayoutParams(layoutParams);
	}

	float[] tmp01 = new float[3];
	private int hitungWarna() {
		tmp01[0] = hue;
		tmp01[1] = sat;
		tmp01[2] = val;
		return Color.HSVToColor(tmp01);
	}

	public void show() {
		dialog.show();
	}
}
