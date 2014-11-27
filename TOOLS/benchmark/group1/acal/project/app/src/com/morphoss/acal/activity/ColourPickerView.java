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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

import com.morphoss.acal.R;

public class ColourPickerView extends View {
	
	Paint paint;
	Shader dalam;
	Shader luar;
	float hue;
	float satudp;
	float ukuranUiDp = 240.f;
	float ukuranUiPx; // diset di constructor
	float[] tmp00 = new float[3];
	private ComposeShader shader;

	public ColourPickerView(Context context) {
		this(context, null);
	}

	public ColourPickerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ColourPickerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		satudp = context.getResources().getDimension(R.dimen.ambilwarna_satudp);
		ukuranUiPx = ukuranUiDp * satudp;

		setHue(0.f);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		paint.setShader(shader);
		
		canvas.drawRect(0.f, 0.f, ukuranUiPx, ukuranUiPx, paint);
	}
	
	void setHue(float hue) {
		this.hue = hue;

		tmp00[1] = tmp00[2] = 1.f;
		tmp00[0] = hue;
		int rgb = Color.HSVToColor(tmp00);
		dalam = new LinearGradient(0.f, 0.f, ukuranUiPx, 0.f, 0xffffffff, rgb, TileMode.CLAMP);

		if (paint == null) {
			paint = new Paint();
			luar = new LinearGradient(0.f, 0.f, 0.f, ukuranUiPx, 0xffffffff, 0xff000000, TileMode.CLAMP);
		}
		
		shader = new ComposeShader(luar, dalam, PorterDuff.Mode.MULTIPLY);

		invalidate();
	}
}
