package com.hectorone.multismssender;

import java.util.HashSet;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PhoneNumberSelection extends ListActivity {
	PhoneDataListAdapter mAdpater;

	HashSet<String> mSelectedSet;
	private static final int INSERT_ID       = Menu.FIRST;
	private static final int SELECT_ALL_ID   = Menu.FIRST + 1;
	private static final int DESELECT_ALL_ID = Menu.FIRST + 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.number_list);

		String[] selected;
		selected = savedInstanceState != null ? savedInstanceState
				.getStringArray(MultiSmsSender.PARAM_NUMBERS_LIST) : null;

		if (selected == null) {
			Bundle extras = getIntent().getExtras();
			selected = extras != null ? extras
					.getStringArray(MultiSmsSender.PARAM_NUMBERS_LIST) : null;
		}

		mSelectedSet = new HashSet<String>();
		for (int i = 0; i < selected.length; i++) {
			mSelectedSet.add(selected[i].trim());
		}
		fillData();

		Button ok = (Button) findViewById(R.id.okContacts);
		ok.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Intent i         = new Intent();
				String[] numbers = new String[mSelectedSet.size()];
				mSelectedSet.toArray(numbers);
				
				Bundle bundle   = new Bundle();
				bundle.putStringArray(MultiSmsSender.PARAM_NUMBERS_LIST,
						numbers);
				bundle.putBoolean(MultiSmsSender.PARAM_FLUSH, true);
				i.putExtras(bundle);
				
				setResult(RESULT_OK, i);
				finish();

			}
		});

	}

	private void fillData() {

		Cursor c = getContentResolver().query(
				Data.CONTENT_URI,
				new String[] { Data._ID, Data.MIMETYPE, Phone.NUMBER,
						Phone.TYPE, Phone.LABEL, Contacts.DISPLAY_NAME },
				Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'", null,
				Contacts.DISPLAY_NAME);
		startManagingCursor(c);
		mAdpater = new PhoneDataListAdapter(this, R.layout.number_row, c,
				new String[] { Contacts.DISPLAY_NAME, Phone.NUMBER },
				new int[] { R.id.name, R.id.phone }, mSelectedSet);

		setListAdapter(mAdpater);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, SELECT_ALL_ID, 0, R.string.select_all);
		menu.add(0, DESELECT_ALL_ID, 0, R.string.deselect_all);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			display_group_list();
			return true;
		case SELECT_ALL_ID:

			/*
			 * Cursor c = getContentResolver().query(Phones.CONTENT_URI, null,
			 * null, null, Phones.NAME); int numberIdx =
			 * c.getColumnIndex(Phones.NUMBER);
			 */
			Cursor c = getContentResolver().query(
					Data.CONTENT_URI,
					new String[] { Data._ID, Data.MIMETYPE, Phone.NUMBER,
							Phone.TYPE, Phone.LABEL, Contacts.DISPLAY_NAME },
					Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'", null,
					null);
			int numberIdx = c.getColumnIndex(Phone.NUMBER);
			startManagingCursor(c);
			c.moveToFirst();
			for (int i = 0; i < c.getCount(); i++) {
				mSelectedSet.add(c.getString(numberIdx));
				c.moveToNext();
			}
			fillData();
			return true;
		case DESELECT_ALL_ID:
			mSelectedSet = new HashSet<String>();
			fillData();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public void display_group_list() {

		Intent i = new Intent(this, SelectGroupActivity.class);
		startActivityForResult(i, MultiSmsSender.ACTIVITY_ADD_GROUP);
	}

	public class PhoneDataListAdapter extends SimpleCursorAdapter {

		public PhoneDataListAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, HashSet<String> selected) {
			super(context, layout, c, from, to);
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			View v = super.getView(position, convertView, parent);
			LinearLayout background = (LinearLayout) v
					.findViewById(R.id.row_background);
			String contactNumber = ((TextView) v.findViewById(R.id.phone))
					.getText().toString();

			CheckBox checkbox = (CheckBox) v.findViewById(R.id.CheckBox);
			checkbox.setOnClickListener(new addNumberToSelectedClickListener(
					contactNumber));
			checkbox.setChecked(mSelectedSet.contains(contactNumber));

			background.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					CheckBox checkbox = (CheckBox) v
							.findViewById(R.id.CheckBox);
					checkbox.performClick();

				}
			});
			return v;
		}

	}

	private class addNumberToSelectedClickListener implements OnClickListener {

		String contactNumber;

		public addNumberToSelectedClickListener(String contact) {
			this.contactNumber = contact;
		}

		public void onClick(View v) {
			CheckBox checkBox = (CheckBox) v;
			if (checkBox.isChecked()) {
				mSelectedSet.add(contactNumber);
			} else {
				mSelectedSet.remove(contactNumber);
			}

		}

	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (intent != null) {
			String[] numbers = intent.getExtras().getStringArray(
					MultiSmsSender.PARAM_NUMBERS_LIST);

			for (int i = 0; i < numbers.length; i++) {
				mSelectedSet.add(numbers[i]);
			}

			fillData();
		}
	}
}
