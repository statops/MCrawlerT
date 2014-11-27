package com.morphoss.acal.database;

import android.content.ContentValues;

public class DMUpdateQuery implements DMAction {


	private final ContentValues values;
	private final String whereClause;
	private final String[] whereArgs;

	public DMUpdateQuery(ContentValues values, String whereClause, String[] whereArgs) {
		this.values = values;
		this.whereClause = whereClause;
		this.whereArgs = whereArgs;
	}

	@Override
	public void process(DatabaseTableManager dm) {
		dm.update(values, whereClause, whereArgs);
	}

}
