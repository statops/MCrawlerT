package com.morphoss.acal.database;

import android.content.ContentValues;

import com.morphoss.acal.database.DatabaseTableManager.QUERY_ACTION;

public class DMQueryBuilder {
	private QUERY_ACTION action = null;
	private String nullColumnHack = null;
	private ContentValues values = null;
	private String whereClause = null;
	private String[] whereArgs = null;

	public DMQueryBuilder setAction(QUERY_ACTION action) {
		this.action = action;
		return this;
	}

	public QUERY_ACTION getAction() {
		return this.action;
	}

	public DMQueryBuilder setNullColumnHack(String nullColumnHack) {
		this.nullColumnHack = nullColumnHack;
		return this;
	}

	public DMQueryBuilder setValues(ContentValues values) {
		this.values = values;
		return this;
	}

	public DMQueryBuilder setWhereClause(String whereClause) {
		this.whereClause = whereClause;
		return this;
	}

	public DMQueryBuilder setwhereArgs(String whereArgs[]) {
		this.whereArgs = whereArgs;
		return this;
	}

	public DMAction build() throws IllegalArgumentException {
		if (action == null) throw new IllegalArgumentException("Can not build query without action set.");
		switch (action) {
		case INSERT:
			if (values == null) throw new IllegalArgumentException("Can not build INSERT query without content values");
			return new DMInsertQuery(nullColumnHack,values);
		case UPDATE:
			if (values == null) throw new IllegalArgumentException("Can not build UPDATE query without content values");
			return new DMUpdateQuery(values, whereClause, whereArgs);
		case DELETE:
			return new DMDeleteQuery(whereClause, whereArgs);
		default:
			throw new IllegalStateException("Invalid action specified!");
		}
	}
}
