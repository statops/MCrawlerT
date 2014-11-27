/*
 * Copyright (C) 2009 Cyril Jaquier
 *
 * This file is part of NetCounter.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.jaqpot.netcounter.model;

import android.database.sqlite.SQLiteDatabase;

public interface IModel {

	public boolean isNew();

	public boolean isDirty();

	public boolean isDeleted();

	public void load(SQLiteDatabase db);

	public void insert(SQLiteDatabase db);

	public void update(SQLiteDatabase db);

	public void remove(SQLiteDatabase db);

	public void addModelListener(IModelListener listener);

	public void removeModelListener(IModelListener listener);

}
