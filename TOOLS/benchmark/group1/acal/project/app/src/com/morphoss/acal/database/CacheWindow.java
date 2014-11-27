package com.morphoss.acal.database;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.acaltime.AcalDateTime;

public class CacheWindow {

	private AcalDateTime windowStart = null;
	private AcalDateTime windowEnd = null;
	private AcalDateRange requestedWindow = null;

	//The midpoint of the last requested time. NB this is calculated from the range
	//asked for via addToRequestedRange, not the actual value of requestedWindow.
	private AcalDateTime lastRequestedMidPoint = null; 

	public static final String TAG = "acal CacheWindow";

	//Window sizing variables
	private long lookForward;
	private long lookBack;
	private long maxSize;
	private long minPaddingBack;
	private long minPaddingForward;
	private boolean goingForward = true;

	private CacheModifier callBack;		//needed for shrinking

	//Default look forward
	private final long lfd = 86400000L*7L*15L; //15 weeks

	/**
	 * Create a window object for maintaining cache window state. 
	 * either param can be null. Sizing vars are set to default
	 */
	public CacheWindow(CacheModifier callback, AcalDateTime startPoint) {
		this(-1,-1,-1,-1,-1,-1,callback,startPoint);
	}

	/**
	 * Create a window with specified sizing options
	 * All duration vars are in milliseconds
	 */
	public CacheWindow(long lookForward, long lookBack, long maxSize,
			long minPaddingBack, long minPaddingForward, long increment,
			CacheModifier callBack, AcalDateTime startPoint) {
		if (startPoint == null) startPoint = new AcalDateTime();
		if (lookForward <= 0) lookForward = lfd;
		lookBack = Math.max((lookForward/3), 86400000L);
		if (maxSize <= 0) maxSize = (lookForward+lookBack)*4;
		if (minPaddingBack <= 0) minPaddingBack = lookBack/2;
		if (minPaddingForward <= 0) minPaddingForward = lookForward/2;
		if (increment <= 0) increment = Math.max(lookForward, lookBack);
		this.lookForward = lookForward;
		this.lookBack = lookBack;
		this.maxSize = maxSize;
		this.minPaddingBack = minPaddingBack;
		this.minPaddingForward = minPaddingForward;
		this.callBack = callBack;
		this.lastRequestedMidPoint = startPoint;
		//calculate initial window request size
		AcalDateTime start = startPoint.clone().addSeconds((lookBack/1000));
		AcalDateTime end = startPoint.clone().addSeconds((lookForward/1000));
		this.addToRequestedRange(new AcalDateRange(start,end));

	}

	/**
	 * Returns true if the requested range is within the window. otherwise false
	 */
	public boolean isWithinWindow(AcalDateRange range) {
		if (windowStart == null || windowEnd == null ) return false;
		if ( windowStart.after(range.start) ) return false;
		if ( windowEnd.before(range.end) ) return false;
		return true;
	}

	/**
	 * Expand the requested range to incorporate the provided range
	 * 
	 * There is some logic here to expand the window beyond the requested range
	 * in certain situations. 
	 * @param range
	 */
	public void addToRequestedRange(AcalDateRange range) {
		if (requestedWindow == null) {
			this.requestedWindow = range.clone();
		}
		else if ( requestedWindow.start.before(range.start) && requestedWindow.end.after(range.end) )
			return;
		else {
			this.requestedWindow = new AcalDateRange(
					(requestedWindow.start.before(range.start) ? requestedWindow.start : range.start.clone()),
					(requestedWindow.end.after(range.end) ? requestedWindow.end : range.end.clone())
			);
		}

		AcalDateTime lastMidPoint = this.lastRequestedMidPoint;
		//calculate the midpoint of the requested range
		this.lastRequestedMidPoint = AcalDateTime.fromMillis(((range.end.getMillis()+range.start.getMillis())/2));

		//check to see if we are within padding zone, if so expand requested
		//range by appropriate look ahead. otherwise snap back to current window size

		//this is only relevant if the window has been set
		if (this.windowEnd == null || this.windowStart == null) {
			if (Constants.LOG_DEBUG)
				Log.d(TAG, "Set Requested Window to "+this.requestedWindow);
			return;
		}

		AcalDateTime rEnd = this.requestedWindow.end.clone();
		AcalDateTime rStart = this.requestedWindow.start.clone();

		//set direction
		if (lastMidPoint.after(this.lastRequestedMidPoint)) this.goingForward = false;
		else this.goingForward = true;

		if (goingForward) {
			//if the end of the requested range is within the padding period of the current
			//window end then the requested range end should be lookForward millis after the current end
			if ((rEnd.clone().addSeconds((minPaddingForward/1000)).after(this.windowEnd))) rEnd = this.windowEnd.clone().addSeconds(lookForward/1000);
			//otherwise we don't need to expand forward
			else {
				rEnd = this.windowEnd.clone();
			}
		} else {
			//if the start of the requested range is within the padding period of the current
			//window end then the requested range end should be lookBack millis before the current end
			if ((rStart.clone().addSeconds((minPaddingBack/1000)).before(this.windowStart))) rStart = this.windowStart.clone().addSeconds(-(lookBack/1000));
			//otherwise we don't need to expand back
			else {
				rStart = this.windowStart.clone();
			}
		}

		//If the requested range is already within the current window, don't bother.
		if (isWithinWindow(requestedWindow)) requestedWindow = null;
		if (Constants.LOG_DEBUG)
			Log.d(TAG, "Set Requested Window to "+this.requestedWindow);
	}

	/**
	 * Returns the currently requested window range. can be null. 
	 * @return
	 */
	public AcalDateRange getRequestedWindow() {
		if (requestedWindow == null) return null;
		return this.requestedWindow.clone();
	}

	/**
	 * Arbitrarily set the window size. Will reset requested range if new window covers the current requested range
	 * 
	 * Warning - this method will not check the set size against the window size vars.
	 * @param range
	 */
	public void setWindowSize(AcalDateRange range) {
		if (range == null) { windowStart = null; return; }
		windowStart = range.start.clone();
		windowEnd = range.end.clone();

		//check requested ranges validity
		if (requestedWindow != null && isWithinWindow(requestedWindow)) requestedWindow = null;
		if (Constants.LOG_DEBUG) {
			Log.d(TAG, "Set Window to "+this.windowStart+"-->"+this.windowEnd);
			Log.d(TAG, "Requested Window: "+this.requestedWindow);
		}
	}

	/**
	 * Grow window size. the new window will be the union of the current window and the given range.
	 * Will reset requested range if new window covers the current requested range
	 * @param range
	 */

	public void expandWindow(AcalDateRange range) {
		if (windowStart == null) {
			windowStart = range.start.clone();
			windowEnd = range.end.clone();
			//check requested ranges validity
			if (requestedWindow != null && isWithinWindow(requestedWindow)) requestedWindow = null;
			if (Constants.LOG_DEBUG) {
				Log.d(TAG, "Set Window to "+this.windowStart+"-->"+this.windowEnd);
				Log.d(TAG, requestedWindow == null ? "No Requested Window" : "requested Window: "+requestedWindow);
			}
			return;
		}
		if (range.start.before(windowStart)) windowStart = range.start.clone();
		if (range.end.after(windowEnd)) windowEnd = range.end.clone();
		//check to see if we have covered the requested range
		if (requestedWindow != null && isWithinWindow(requestedWindow)) requestedWindow = null;

		//apply shrink rules - can only be done if we have a callback
		if (	callBack != null &&
				this.windowEnd.getMillis()-this.windowEnd.getMillis() > this.maxSize) {
			//window is to big, calculate the range it should cover
			//wiping is done in the oppisite direction of travel
			//if maxSize is inappropriate and/or use is switching direction a lot
			//this could have undesireable performance affects.

			//All we are doing is requesting the cache delete events in the invalid range
			//we expect the callback to notify us to change our window size when this is done
			//this should be done via reduceWindow
			if (goingForward) {
				//bring the window start forward.
				AcalDateTime newStart = AcalDateTime.fromMillis(windowEnd.getMillis()-maxSize);
				callBack.deleteRange(new AcalDateRange(windowStart,newStart.addSeconds(-1)));
			} else {
				//bring the window end back.
				AcalDateTime newEnd = AcalDateTime.fromMillis(windowStart.getMillis()+maxSize);
				callBack.deleteRange(new AcalDateRange(newEnd.addSeconds(1), windowEnd));
			}
		}
		if (Constants.LOG_DEBUG) {
			Log.d(TAG, "Set Window to "+this.windowStart+"-->"+this.windowEnd);
			Log.d(TAG, requestedWindow == null ? "No Requested Window" : "requested Window: "+requestedWindow);
		}
	}

	/**
	 * Reduce window size to the intersection of the current window and the given range.
	 * @param rangeToRemove
	 */
	public void reduceWindow(AcalDateRange rangeToRemove) {
		if (windowStart == null) {
			windowStart = rangeToRemove.start.clone();
			windowEnd = rangeToRemove.end.clone();
			//check requested ranges validity
			if (requestedWindow != null && isWithinWindow(requestedWindow)) requestedWindow = null;
			if (Constants.LOG_DEBUG) {
				Log.d(TAG, "Set Window to "+this.windowStart+"-->"+this.windowEnd);
				Log.d(TAG, requestedWindow == null ? "No Requested Window" : "requested Window: "+requestedWindow);
			}
			return;
		}

		if (rangeToRemove.start.after(windowStart)) windowStart = rangeToRemove.start.clone();
		if (rangeToRemove.end.before(windowEnd)) windowEnd = rangeToRemove.end.clone();
		//check requested ranges validity
		if (requestedWindow != null && isWithinWindow(requestedWindow)) requestedWindow = null;
		if (Constants.LOG_DEBUG) {
			Log.d(TAG, "Set Window to "+this.windowStart+"-->"+this.windowEnd);
			Log.d(TAG, requestedWindow == null ? "No Requested Window" : "requested Window: "+requestedWindow);
		}
	}

	/**
	 * returns a clone of the current window range or null if there is none.
	 * @return
	 */
	public AcalDateRange getCurrentWindow() {
		if (windowStart == null) return null;
		return new AcalDateRange(windowStart.clone(), windowEnd.clone());
	}

	@Override
	public String toString() {
		return "CacheWindow is ("+(windowStart==null?"<null<":windowStart.fmtIcal())+","+(windowEnd==null?">null>":windowEnd.fmtIcal())+") " +
		(requestedWindow == null ? "" : " requested "+requestedWindow);
	}
}
