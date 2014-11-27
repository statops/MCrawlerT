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

package net.jaqpot.netcounter.dialog;

import net.jaqpot.netcounter.NetCounterApplication;
import net.jaqpot.netcounter.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public abstract class CounterSingleChoiceDialog extends CounterDialog {

	private final ListView mListView;

	public CounterSingleChoiceDialog(Context context, NetCounterApplication app, int array) {
		super(context, app);

		LayoutInflater inflater = getLayoutInflater();
		mListView = (ListView) inflater.inflate(R.layout.dialog_list, null);
		mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> a, View v, int p, long i) {
				onClick(p);
			}
		});

		setArray(array);

		setInverseBackgroundForced(true);
		setView(mListView);
	}

	protected void setArray(int array) {
		CharSequence[] a = getResources().getTextArray(array);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getContext(),
				android.R.layout.select_dialog_singlechoice, android.R.id.text1, a);
		mListView.setAdapter(adapter);
	}

	protected void setDefault(int pos) {
		mListView.clearChoices();
		mListView.setItemChecked(pos, true);
		mListView.setSelection(pos);
	}

	protected abstract void onClick(int pos);

}
