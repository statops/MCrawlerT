package com.morphoss.acal.database;

import android.content.ContentValues;

public class DMInsertQuery implements DMAction {
	private final String nullColumnHack;
	private final ContentValues values;

	public DMInsertQuery(String nullColumnHack, ContentValues values) {
		this.nullColumnHack = nullColumnHack;
		this.values = values;
	}

	public void process(DatabaseTableManager dm) {
		dm.insert(nullColumnHack, values);
	}
}
