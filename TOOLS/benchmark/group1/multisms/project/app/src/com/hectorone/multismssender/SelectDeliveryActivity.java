package com.hectorone.multismssender;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SelectDeliveryActivity extends ListActivity {

	DeliveryDbAdapter mDbHelper;
	public static final int DELETE_ID     = Menu.FIRST;
	public static final int DELETE_ALL_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID    = Menu.FIRST + 2;

	public static final String PARAM_DELIVERY_ID = "param_delivery_id";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.delivery_list);
		fillData();
		registerForContextMenu(getListView());
	}

	protected void onDestroy() {
		super.onDestroy();
	}

	public void fillData() {
		Cursor deliveryCursor = getContentResolver().query(
				DeliveryDbAdapter.CONTENT_MESSAGE_URI, null, null, null, null);

		startManagingCursor(deliveryCursor);

		String[] from = new String[] { DeliveryDbAdapter.KEY_MESSAGE_DATE,
				DeliveryDbAdapter.KEY_MESSAGE_NAME };

		int[] to = new int[] { R.id.date, R.id.name };

		SimpleCursorAdapter notes = new SimpleCursorAdapter(this,
				R.layout.delivery_row, deliveryCursor, from, to);
		setListAdapter(notes);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, DELETE_ALL_ID, 0, R.string.remove_all);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.remove);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
					.getMenuInfo();
			getContentResolver().delete(DeliveryDbAdapter.CONTENT_MESSAGE_URI,
					DeliveryDbAdapter.KEY_MESSAGE_ROWID + "=" + info.id, null);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ALL_ID:
			getContentResolver().delete(DeliveryDbAdapter.CONTENT_MESSAGE_URI,
					null, null);
			fillData();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, ListEntryActivity.class);
		i.putExtra(PARAM_DELIVERY_ID, id);
		startActivity(i);
		super.onListItemClick(l, v, position, id);
	}
}
