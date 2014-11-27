package com.hectorone.multismssender;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SelectGroupActivity extends ListActivity {

	GroupsDbAdapter mDbHelper;
	public static final int DELETE_ID  = Menu.FIRST;
	public static final int EDIT_ID    = Menu.FIRST + 1;
	public static final int CREATE_ID  = Menu.FIRST + 2;

	public static final String PARAM_GROUP_ID   = "gid";

	public static final int ACTIVITY_EDIT_GROUP = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Log.d("SelectGroup", "***Create");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_list);
		mDbHelper = new GroupsDbAdapter(this);
		mDbHelper.open();
		fillData();
		registerForContextMenu(getListView());
	}

	protected void onStart() {
		//Log.d("SelectGroup", "***onStart");
		mDbHelper.open();
		super.onStart();
	}

	protected void onStop() {
		//Log.d("SelectGroup", "***onStop");
		mDbHelper.close();
		super.onStop();
	}

	public void fillData() {
		Cursor groupsCursor = mDbHelper.fetchAllGroups();
		startManagingCursor(groupsCursor);

		String[] from = new String[] { GroupsDbAdapter.KEY_GROUP_NAME };

		int[] to = new int[] { R.id.groupNameTextView };

		SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.group_row, groupsCursor, from, to);
		setListAdapter(notes);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, CREATE_ID, 0, R.string.create_group);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.remove_group);
		menu.add(0, EDIT_ID, 0, R.string.edit_group);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Cursor phonesListCursor = mDbHelper.fetchPhonesFromGroup(id);
		startManagingCursor(phonesListCursor);
		int phoneNumberIdx = phonesListCursor.getColumnIndex(Phone.NUMBER);
		String[] res = new String[phonesListCursor.getCount()];
		phonesListCursor.moveToFirst();
		for (int i = 0; i < res.length; i++) {
			res[i] = phonesListCursor.getString(phoneNumberIdx);
			phonesListCursor.moveToNext();

		}

		Intent i = new Intent();
		Bundle bundle = new Bundle();

		bundle.putStringArray(MultiSmsSender.PARAM_NUMBERS_LIST, res);
		i.putExtras(bundle);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			mDbHelper.deleteGroup(info.id);
			fillData();
			return true;
		case EDIT_ID:
			AdapterContextMenuInfo infoEdit = (AdapterContextMenuInfo) item
					.getMenuInfo();
			editGroup(infoEdit.id);
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case CREATE_ID:
			editGroup(null);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public void editGroup(Long gid) {
		Intent i = new Intent(this, GroupEditActivity.class);

		if (gid != null) {
			i.putExtra(PARAM_GROUP_ID, gid);
		}

		startActivityForResult(i, ACTIVITY_EDIT_GROUP);
	}
}
