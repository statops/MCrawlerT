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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractModel implements IModel {

	private List<IModelListener> mListeners;

	private boolean mIsDirty = false;

	private boolean mIsNew = false;

	private boolean mIsDeleted = false;

	public void addModelListener(IModelListener listener) {
		if (mListeners == null) {
			mListeners = new ArrayList<IModelListener>();
		}
		mListeners.add(listener);
	}

	public void removeModelListener(IModelListener listener) {
		if (mListeners != null) {
			mListeners.remove(listener);
		}
	}

	protected void fireModelLoaded() {
		if (mListeners != null) {
			for (IModelListener listener : mListeners) {
				listener.modelLoaded(this);
			}
		}
	}

	protected void fireModelChanged() {
		if (mListeners != null) {
			for (IModelListener listener : mListeners) {
				listener.modelChanged(this);
			}
		}
	}

	protected void fireModelChanged(IModel object) {
		if (mListeners != null) {
			for (IModelListener listener : mListeners) {
				listener.modelChanged(object);
			}
		}
	}

	public boolean isDirty() {
		return mIsDirty;
	}

	protected void setDirty(boolean isDirty) {
		mIsDirty = isDirty;
	}

	public boolean isNew() {
		return mIsNew;
	}

	protected void setNew(boolean isNew) {
		mIsNew = isNew;
	}

	public boolean isDeleted() {
		return mIsDeleted;
	}

	protected void setDeleted(boolean isDeleted) {
		mIsDeleted = isDeleted;
	}

}
