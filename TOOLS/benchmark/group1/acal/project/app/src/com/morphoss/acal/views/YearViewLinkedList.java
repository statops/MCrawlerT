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
import android.graphics.Canvas;

import com.morphoss.acal.acaltime.AcalDateTime;

public class YearViewLinkedList {
	
	private YearViewNode root;
	
	public YearViewLinkedList() {
		
	}

	public AcalDateTime getClickedMonth(int x, int y, int offset, int numRowsAbove, int cols) {
		if (root == null) return null;
		YearViewNode cur = root;
		int count = 0;
		while (count < numRowsAbove && cur != null) {
			for (int i = 0; i < cols; i++) cur=cur.getNext();
			if (cur instanceof YearHeader) cur=cur.getNext();
			count++;
		}
		if (cur == null) return null;
		int topY = offset;
		int botY = topY+cur.getHeight();
		while (cur != null) {
			for (int i = 0; i< cols; i++) {
				if (y>=topY && y<botY && cur.isUnder(x)) {
					return ((MonthImage)cur).getDate();
				}
				cur = cur.getNext();
				if (cur instanceof YearHeader) cur = cur.getNext();
			}
			topY=botY;
			botY=topY+cur.getHeight();
		}
		return null;
	}
	public void draw(Canvas c, int y, int cols) {
		if (root != null) root.draw(c,y,cols,cols);
	}

	public YearViewNode removeLastChild() {
		YearViewNode ret = null;
		if (root == null) return ret;
		else if (root.getNext() == null) {
			ret = root;
			root = null;
			return ret;
		}
		else {
			YearViewNode cur = root.getNext().getNext();
			while (cur.getNext().getNext() != null) cur = cur.getNext();
			ret = cur.getNext();
			cur.setNext(null);
			return ret;
		}
	}
	
	public void removeFirstChild() {
		if (root == null) return;
		if (root.getNext() == null) root = null;
		else root = root.getNext();
	}

	
	public YearViewNode getLastChild() {
		if (root == null) return null;
		else {
			YearViewNode cur = root;
			while (cur.getNext() != null) cur = cur.getNext();
			return cur;
		}
	}
	
	//return the totol height of all elements removed
	public int removeFirstRow(int cols) {
		if (root == null) return 0;
		int ret = 0;
		if (root instanceof YearHeader) { 
			ret = root.getHeight();
			removeFirstChild(); 
			return ret+removeFirstRow(cols); 
		}
		for (int i = 0; i<cols; i++) {
			ret= root.getHeight();
			removeFirstChild();
		}
		//make sure that root isnt a year header
		if (root != null && root instanceof YearHeader) {
			ret+=root.getHeight();
			root = root.getNext();
		}
		return ret;
	}
	
	public void removeLastRow(int cols) {
		if (root == null) return;
		if (getLastChild() instanceof YearHeader) { removeLastChild(); removeLastRow(cols); }
		for (int i = 0; i<cols; i++) removeLastChild();
		//ensure last child is not a yearheader
		if (getLastChild() instanceof YearHeader) { removeLastChild();}
	}

	public void createInitialRow(int initialMonth, int initialYear, int cols, Context c, int x, MonthImageGenerator ig, int compWidth) {
		root = new MonthImage( c, initialYear, initialMonth++, 1, x, ig); 

		YearViewNode cur = root;
		for (int i = 1; i < cols && initialMonth <= AcalDateTime.DECEMBER; i++) {
			cur.setNext(new MonthImage(c,initialYear,initialMonth++,1,(x+(i*compWidth)),ig));
			cur=cur.getNext();
		}
	}

	public int insertNewRow(int cols, Context c, int x, MonthImageGenerator ig, int compWidth) {
		if (root == null) return 0;
		MonthImage first = root.getMonthImage();
		int month = first.getMonth();
		int year = first.getYear();
		int ret = 0;
		if (month == AcalDateTime.JANUARY) {
			//insert header row
			YearHeader yh = new YearHeader(c, year, x, ig);
			yh.setNext(root);
			root = yh;
			ret+=yh.getHeight();
		}
		month-=cols;
		if (month < AcalDateTime.JANUARY) { month+=12; year--;}
		YearViewNode start = new MonthImage(c,year,month++,1,x,ig);
		YearViewNode cur = start;
		for (int i = 1; i < cols && month <= AcalDateTime.DECEMBER; i++) {
			cur.setNext(new MonthImage(c,year,month++,1,x+(i*compWidth),ig));
			cur = cur.getNext();
		}
		cur.setNext(root);
		root = start;
		return ret+root.getHeight();
	}

	
	public void addRowToEnd(int cols, Context c, int x, MonthImageGenerator ig, int compWidth) {
		//Some invariants to note:
		//It is assumed that DECEMBER can only ever be the last value in a row
		//The last item in the list must ALWAYS be a MonthImage
		if (root == null) return;
		YearViewNode cur = root;
		while (cur.getNext() != null) cur = cur.getNext();
		
		MonthImage curMonth = cur.getMonthImage();
		int curYear = curMonth.getYear();
		int month = curMonth.getMonth();

		//if the last node was December, add the year header and make month january next year
		if (month == AcalDateTime.DECEMBER) {
			cur.setNext(new YearHeader(c,curYear+1,x,ig));
			cur = cur.getNext();
		}

		
		for (int i = 0; i < cols; i++) {
			month++;
			//if the current last month is larger than december, increment year etc
			if (month > AcalDateTime.DECEMBER) {
				curYear++;
				month-=12;
			}
			cur.setNext(new MonthImage(c,curYear,month,1,(x+(i*compWidth)),ig));
			cur=cur.getNext();
		}
	}
	
	public YearViewNode getFirst() {
		return this.root;
	}
	public int getYearHeaderHeight() {
		if (root == null) return 0;
		YearViewNode cur = root;
		while (cur != null && !(cur instanceof YearHeader)) cur=cur.getNext();
		if (cur == null) return 0;
		return cur.getHeight();
	}
	
	public AcalDateTime getDisplayedDate(int rowsAbove, int cols) {
		return root.getDisplayedDate(rowsAbove, cols, cols);
	}
}
