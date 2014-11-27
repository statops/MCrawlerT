package com.morphoss.acal.database;

import com.morphoss.acal.acaltime.AcalDateRange;

public interface CacheModifier {

	public void deleteRange(AcalDateRange range);
}
