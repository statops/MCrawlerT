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

package net.jaqpot.netcounter.activity;

import net.jaqpot.netcounter.R;
import net.jaqpot.netcounter.model.Counter;
import net.jaqpot.netcounter.model.Interface;
import net.jaqpot.netcounter.model.NetCounterModel;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListAdapter extends BaseExpandableListAdapter {

	private NetCounterModel mInput;

	private final LayoutInflater mInflater;

	public ListAdapter(Context context) {
		mInflater = LayoutInflater.from(context);
	}

	public void setInput(NetCounterModel root) {
		mInput = root;
		if (root != null) {
			notifyDataSetChanged();
		}
	}

	public NetCounterModel getInput() {
		return mInput;
	}

	public Object getChild(int groupPosition, int childPosition) {
		Interface i = (Interface) getGroup(groupPosition);
		return i.getCounters().get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {
		View view = convertView;
		ChildViewWrapper wrapper;

		if (view == null) {
			view = mInflater.inflate(R.layout.child, null);
			wrapper = new ChildViewWrapper(view);
			view.setTag(wrapper);
		} else {
			wrapper = (ChildViewWrapper) view.getTag();
		}

		Counter counter = (Counter) getChild(groupPosition, childPosition);
		wrapper.getTitle().setText(counter.getTypeAsString());
		wrapper.getTotal().setText(counter.getTotalAsString());

		return view;
	}

	public int getChildrenCount(int groupPosition) {
		Interface i = (Interface) getGroup(groupPosition);
		return i.getCounters().size();
	}

	public Object getGroup(int groupPosition) {
		return mInput.getInterfaces().get(groupPosition);
	}

	public int getGroupCount() {
		if (mInput == null) {
			return 0;
		}
		return mInput.getInterfaces().size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {
		View view = convertView;
		GroupViewWrapper wrapper;

		if (view == null) {
			view = mInflater.inflate(R.layout.group, null);
			wrapper = new GroupViewWrapper(view);
			view.setTag(wrapper);
		} else {
			wrapper = (GroupViewWrapper) view.getTag();
		}

		Interface i = (Interface) getGroup(groupPosition);
		wrapper.getIcon().setImageResource(i.getIcon());
		wrapper.getTitle().setText(i.getPrettyName());
		wrapper.getUpdate().setText(i.getLastUpdateAsString());

		return view;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	/**
	 * View wrapper for child row as described in The Busy Coder's Guide to
	 * Android Development by Mark L. Murphy.
	 */
	static class ChildViewWrapper {

		private final View mBase;
		private TextView mTitel;
		private TextView mTotal;

		ChildViewWrapper(View base) {
			mBase = base;
		}

		TextView getTitle() {
			if (mTitel == null) {
				mTitel = (TextView) mBase.findViewById(R.id.inter);
			}
			return mTitel;
		}

		TextView getTotal() {
			if (mTotal == null) {
				mTotal = (TextView) mBase.findViewById(R.id.total_bytes);
			}
			return mTotal;
		}
	}

	/**
	 * View wrapper for group row as described in The Busy Coder's Guide to
	 * Android Development by Mark L. Murphy.
	 */
	static class GroupViewWrapper {

		private final View mBase;
		private ImageView mIcon;
		private TextView mTitel;
		private TextView mUpdate;

		GroupViewWrapper(View base) {
			mBase = base;
		}

		ImageView getIcon() {
			if (mIcon == null) {
				mIcon = (ImageView) mBase.findViewById(R.id.icon);
			}
			return mIcon;
		}

		TextView getTitle() {
			if (mTitel == null) {
				mTitel = (TextView) mBase.findViewById(R.id.title_left);
			}
			return mTitel;
		}

		TextView getUpdate() {
			if (mUpdate == null) {
				mUpdate = (TextView) mBase.findViewById(R.id.title_right);
			}
			return mUpdate;
		}
	}

}
