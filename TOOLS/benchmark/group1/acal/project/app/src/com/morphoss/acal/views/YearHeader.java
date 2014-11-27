/*
 * Copyright (C) 2011 Morphoss Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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

package com.morphoss.acal.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class YearHeader extends YearViewNode {
	
	private int x;
	private Bitmap myBMP;

	public YearHeader(Context context, int year, int x, MonthImageGenerator ig) {
		this.x = x;
		myBMP = ig.getYearHeader(year);
	}
	
	@Override
	protected void draw(Canvas canvas, int y) {
		Paint paint = new Paint();
		canvas.drawBitmap(myBMP, x, y, paint);
	}
	
	@Override
	public int getHeight() {
		return myBMP.getHeight();
	}

	@Override
	public int getMonth() {
		return -1;
	}

	@Override
	public boolean isUnder(int x) {
		return false;
	}

	@Override
	public MonthImage getMonthImage() {
		if ( this.getNext() != null ) return (MonthImage) this.getNext();
		return null;
	}
}
