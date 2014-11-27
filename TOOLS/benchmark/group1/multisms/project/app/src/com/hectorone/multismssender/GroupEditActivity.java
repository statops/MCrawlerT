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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;

public class GroupEditActivity extends ListActivity {

	GroupDataListAdapter mAdpater;
	GroupsDbAdapter mDb;
	EditText mGroupNameText;
	Long mGid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_group_list);
		mGroupNameText = (EditText) findViewById(R.id.groupName);

		// Cursor c = getContentResolver().query(Phones.CONTENT_URI, null, null,
		// null, Phones.NAME);
		Cursor c = getContentResolver().query(
				Data.CONTENT_URI,
				new String[] { Data._ID, Data.MIMETYPE, Phone.NUMBER,
						Phone.TYPE, Phone.LABEL, Contacts.DISPLAY_NAME },
				Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'", null,
				Contacts.DISPLAY_NAME);
		startManagingCursor(c);

		String[] mSelected = {};

		mDb = new GroupsDbAdapter(this);
		mDb.open();

		Long groupId;
		Bundle extras = getIntent().getExtras();
		groupId = extras != null ? extras
				.getLong(SelectGroupActivity.PARAM_GROUP_ID) : null;

		if (groupId != null) {
			Cursor groupNameCursor = mDb.fetchGroup(groupId);
			startManagingCursor(groupNameCursor);
			String groupName = groupNameCursor.getString(groupNameCursor
					.getColumnIndex(GroupsDbAdapter.KEY_GROUP_NAME));
			mGroupNameText.setText(groupName);
			Cursor numbers = mDb.fetchPhonesFromGroup(groupId);
			startManagingCursor(numbers);
			numbers.moveToFirst();
			int phoneNumIdx = numbers.getColumnIndex(Phone.NUMBER);
			mSelected = new String[numbers.getCount()];
			for (int i = 0; i < numbers.getCount(); i++) {
				mSelected[i] = numbers.getString(phoneNumIdx);
				numbers.moveToNext();
			}

			mGid = groupId;
		}

		mAdpater = new GroupDataListAdapter(this, R.layout.number_row, c,
				new String[] { Contacts.DISPLAY_NAME, Phone.NUMBER },
				new int[] { R.id.name, R.id.phone }, mSelected);
		setListAdapter(mAdpater);

		Button ok = (Button) findViewById(R.id.okGroups);
		ok.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				createGroup();
				reNameGroup();
				Intent i = new Intent();
				setResult(RESULT_OK, i);
				finish();

			}
		});
	}

	protected void onStart() {
		//Log.d("GroupEdit", "---onStart");
		mDb.open();
		super.onStart();
	}

	protected void onResume() {
		//Log.d("GroupEdit", "---onResume");
		super.onResume();
	}

	protected void onStop() {
		//Log.d("GroupEdit", "---OnStop");
		mDb.close();
		super.onStop();
	}

	private void createGroup() {
		if (mGid == null) {
			String name = mGroupNameText.getText().toString();
			if (name.equals("")) {
				name = getResources().getString(R.string.noName);
			}
			mGid = mDb.createGroup(name);
		}
	}

	private void reNameGroup() {
		if (mGid != null) {
			String name = mGroupNameText.getText().toString();
			if (name.equals("")) {
				name = getResources().getString(R.string.noName);
			}
			mDb.updateGroup(mGid, name);
		}
	}

	public class GroupDataListAdapter extends SimpleCursorAdapter {

		public HashSet<String> selected;
		int nameidx;
		int numberidx;
		int idIdx;
		Context mContext;

		public GroupDataListAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, String[] rawSelected) {
			super(context, layout, c, from, to);
			nameidx   = c.getColumnIndex(Contacts.DISPLAY_NAME);
			numberidx = c.getColumnIndex(Phone.NUMBER);
			idIdx     = c.getColumnIndex(Data._ID);
			mContext  = context;
			selected  = new HashSet<String>();
			for (int i = 0; i < rawSelected.length; i++) {
				selected.add(rawSelected[i].trim());
			}
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			Cursor c = getCursor();
			startManagingCursor(c);
			c.moveToPosition(position);
			

			String contactNumber    = c.getString(numberidx);
			long id                 = c.getLong(idIdx);
			View v                  = super.getView(position, convertView, parent);
			LinearLayout background = (LinearLayout) v
					.findViewById(R.id.row_background);

			CheckBox checkbox       = (CheckBox) v.findViewById(R.id.CheckBox);
			checkbox.setOnClickListener(new addNumberToGroupClickListener(id));
			checkbox.setChecked(selected.contains(contactNumber));

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

	private class addNumberToGroupClickListener implements OnClickListener {

		Long mId;

		public addNumberToGroupClickListener(Long id) {
			super();
			this.mId = id;
		}

		public void onClick(View v) {
			CheckBox cBox = (CheckBox) v;
			createGroup();
			if (cBox.isChecked()) {
				mDb.addPhoneToGroup(mGid, mId);
			} else {
				mDb.removePhoneToGroup(mGid, mId);
			}

		}

	}

}
