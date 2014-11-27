package com.morphoss.acal.weekview;

import java.util.ArrayList;

/**
 * Calculates and stores a timetable of event data for a given set of data.
 * 
 * The timetable is constructed in a way that helps weekview draw events nicely. Full timetabling is a p time algorithm
 * the one used here is not complete, but is efficient. Further improvements would be appreciated.
 * @author Chris Noldus
 *
 */
public class WeekViewTimeTable {
	
	private WVCacheObject[][] timetable;
	private ArrayList<WVCacheObject> data;
	private boolean horizontal = false;
	
	//Some left over sideaffect data that needs to be kept for now
	public int HDepth = 0;
	public int lastMaxX = 0;
	
	public WeekViewTimeTable(ArrayList<WVCacheObject> data, boolean horizontal) {
		this.data = data;
		this.horizontal = horizontal;
		if (horizontal) constructHorizontal();
		else constructVertical();
	}
	
	public WVCacheObject[][] getTimetable() {
		return this.timetable;
	}
	
	/**
	 * 2 Timetables are equivalent if they contain the exact same data set and go in the same direction
	 */
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof WeekViewTimeTable)) return false;
		WeekViewTimeTable o = (WeekViewTimeTable)other;
		if (this.horizontal != o.horizontal) return false;
		if (this.data.size() != o.data.size()) return false;
		for (WVCacheObject wv: data) if (!o.data.contains(wv)) return false;
		return true;
	}
	
	private void constructHorizontal() {
		timetable = new WVCacheObject[data.size()][data.size()]; //maximum possible
		int depth = 0;
		for (int x = 0; x < data.size(); x++) {
			WVCacheObject co = data.get(x);
			int i = 0;
			boolean go = true;
			while(go) {
				WVCacheObject[] row = timetable[i];
				int j=0;
				while(true) {
					if (row[j] == null) { row[j] = co; go=false; break; }
					else if (!(row[j].getEnd() > (co.getStart()))) {j++; continue; }
					else break;
				}
				i++;
			}
			depth = Math.max(depth,i);
		}
		HDepth = depth; //TODO this sideaffect is needed by weekview days
	}
	
	private void constructVertical() {
		timetable = new WVCacheObject[data.size()][data.size()]; //maximum possible
		int maxX = 0;
		for (WVCacheObject co :data){
			int x = 0; int y = 0;
			while (timetable[y][x] != null) {
				if (co.overlaps(timetable[y][x])) {
					//if our end time is before [y][x]'s, we need to extend[y][x]'s range
					if (co.getEnd() < (timetable[y][x].getEnd())) timetable[y+1][x] = timetable[y][x];
					x++;
				} else {
					y++;
				}
			}
			timetable[y][x] = co;
			if (x > maxX) maxX = x;
		}
		lastMaxX = maxX;
	}
	

}
