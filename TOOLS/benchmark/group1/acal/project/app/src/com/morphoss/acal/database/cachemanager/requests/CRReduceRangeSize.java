package com.morphoss.acal.database.cachemanager.requests;

import java.util.TimeZone;

import android.util.Log;

import com.morphoss.acal.Constants;
import com.morphoss.acal.acaltime.AcalDateRange;
import com.morphoss.acal.database.cachemanager.CacheManager;
import com.morphoss.acal.database.cachemanager.CacheProcessingException;
import com.morphoss.acal.database.cachemanager.CacheRequest;
import com.morphoss.acal.database.cachemanager.CacheManager.CacheTableManager;


public class CRReduceRangeSize implements CacheRequest {

	private AcalDateRange range;
	public static final String TAG = "aCal CRReduceRangeSize";
	
	public CRReduceRangeSize(AcalDateRange range) {
		this.range = range;
	}
	
	@Override
	public void process(CacheTableManager processor) throws CacheProcessingException {
		if ( CacheManager.DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG, "Reducing cache size and notifying window");
		processor.setWindowOnlyTrue();
		String dtStart = range.start.getMillis()+"";
		String dtEnd = range.end.getMillis()+"";
		String offset = TimeZone.getDefault().getOffset(range.start.getMillis())+"";
		
		
		processor.delete( 
				"( " + 
					"( "+CacheTableManager.FIELD_DTEND+" > ? AND NOT "+CacheTableManager.FIELD_DTEND_FLOAT+" )"+
						" OR "+
						"( "+CacheTableManager.FIELD_DTEND+" + ? > ? AND "+CacheTableManager.FIELD_DTEND_FLOAT+" )"+
						" OR "+
					"( "+CacheTableManager.FIELD_DTEND+" ISNULL )"+
				" ) AND ( "+
					"( "+CacheTableManager.FIELD_DTSTART+" < ? AND NOT "+CacheTableManager.FIELD_DTSTART_FLOAT+" )"+
						" OR "+
					"( "+CacheTableManager.FIELD_DTSTART+" + ? < ? AND "+CacheTableManager.FIELD_DTSTART_FLOAT+" )"+
						" OR "+
					"( "+CacheTableManager.FIELD_DTSTART+" ISNULL )"+
				")",
				new String[] {dtStart , offset, dtStart, dtEnd, offset, dtEnd});
		
		processor.removeRangeFromWindow(range);
		if ( CacheManager.DEBUG && Constants.LOG_DEBUG ) Log.println(Constants.LOGD, TAG,"Done");
	}

	

}
