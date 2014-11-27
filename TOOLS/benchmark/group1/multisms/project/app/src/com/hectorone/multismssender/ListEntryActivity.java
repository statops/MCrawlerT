package com.hectorone.multismssender;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

public class ListEntryActivity extends ListActivity {
	DeliveryDbAdapter mDbHelper;
	Long mDeliveryId;
	public static final int REFRESH_ID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entry_list);
		Bundle extras = getIntent().getExtras();
		mDeliveryId   = extras != null ? extras
				.getLong(SelectDeliveryActivity.PARAM_DELIVERY_ID) : null;

		fillData();
		registerForContextMenu(getListView());

	}

	public void fillData() {

		Cursor deliveryCursor = getContentResolver().query(
				DeliveryDbAdapter.CONTENT_DELIVERY_URI,
				null,
				DeliveryDbAdapter.KEY_DELIVERY_ENTRY_MESSAGE_ID + " = "
						+ mDeliveryId, null, null);

		startManagingCursor(deliveryCursor);

		String[] from = new String[] {
				DeliveryDbAdapter.KEY_DELIVERY_ENTRY_NAME,
				DeliveryDbAdapter.KEY_DELIVERY_ENTRY_NUMBER };

		int[] to = new int[] { R.id.name, R.id.number };

		EntryCursorAdapter notes = new EntryCursorAdapter(this,
				R.layout.entry_row, deliveryCursor, from, to);
		setListAdapter(notes);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH_ID, 0, R.string.refresh);
		return true;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case REFRESH_ID:
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private class EntryCursorAdapter extends SimpleCursorAdapter {
		Cursor c;
		int deliveredIdx;

		public EntryCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			// TODO this.getCursor()
			this.c = c;
			deliveredIdx = c
					.getColumnIndex(DeliveryDbAdapter.KEY_DELIVERY_ENTRY_DELIVERED);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);
			ImageView image = (ImageView) v.findViewById(R.id.delivered);

			c.moveToPosition(position);
			int delivered = c.getInt(deliveredIdx);
			if (delivered != 0) {
				image.setImageResource(R.drawable.btn_check_buttonless_on);
			} else {
				image.setImageResource(R.drawable.btn_check_buttonless_off);
			}

			return v;
		}
	}

}
