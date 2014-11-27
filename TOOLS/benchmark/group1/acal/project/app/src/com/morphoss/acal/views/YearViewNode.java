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

import android.graphics.Canvas;

import com.morphoss.acal.acaltime.AcalDateTime;

public abstract class YearViewNode {
	
	private YearViewNode next;
	
	public void setNext(YearViewNode next) {
		this.next = next;
	}
	
	public abstract int getHeight();
	

	public AcalDateTime getDisplayedDate(int rowsLeft, int colsLeft, int numCols) {
		if (this instanceof YearHeader) return next.getDisplayedDate(rowsLeft, colsLeft, numCols);
		if (rowsLeft == 0 && colsLeft == 1) {
			return ((MonthImage)this).getDate();
		}
		if (colsLeft > 1) return next.getDisplayedDate(rowsLeft, colsLeft-1, numCols);
		colsLeft = numCols;
		rowsLeft--;
		return next.getDisplayedDate(rowsLeft, colsLeft, numCols);
	}
	
	public YearViewNode getNext() {
		return this.next;
	}
	
	public void draw(Canvas c, int y, int cols, int numLeft) {
		this.draw(c, y);
		if (next == null) return;
		if (this instanceof YearHeader) next.draw(c, y+this.getHeight(), cols, cols);
		else if (this instanceof MonthImage) {
			numLeft--;
			if (numLeft == 0) next.draw(c, y+this.getHeight(), cols,cols);
			else next.draw(c, y, cols, numLeft);
		}
	}
	
	protected abstract void draw(Canvas c, int y);
	
	public int size() {
		if (next == null) return 1;
		return 1+next.size();
	}	
	
	public abstract int getMonth();
	public abstract boolean isUnder(int x);

	public abstract MonthImage getMonthImage();
}
