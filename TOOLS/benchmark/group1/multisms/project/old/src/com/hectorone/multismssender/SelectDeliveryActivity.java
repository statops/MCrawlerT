package com.hectorone.multismssender;


import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class SelectDeliveryActivity extends ListActivity {

	
	DeliveryDbAdapter mDbHelper;
	public static final int DELETE_ID = Menu.FIRST;
	public static final int DELETE_ALL_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID = Menu.FIRST + 2;
	
	public static final String PARAM_DELIVERY_ID = "param_delivery_id";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.delivery_list);
		mDbHelper = new DeliveryDbAdapter(this);
		mDbHelper.open();
		fillData();
		registerForContextMenu(getListView());
	}
	

	protected void onDestroy() {
		mDbHelper.close();
		super.onDestroy();
	}
	
	public void fillData() {
		Cursor deliveryCursor = mDbHelper.fetchAllDeliveries();

		startManagingCursor(deliveryCursor);

		String[] from = new String[]{DeliveryDbAdapter.KEY_DELIVERY_DATE, DeliveryDbAdapter.KEY_DELIVERY_NAME };

		int[] to = new int[]{R.id.date, R.id.name};

		SimpleCursorAdapter notes = 
			new SimpleCursorAdapter(this, R.layout.delivery_row, deliveryCursor, from, to);
		setListAdapter(notes);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, DELETE_ALL_ID,0, R.string.remove_all);
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
		switch(item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDbHelper.deleteDelivery(info.id);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case DELETE_ALL_ID:
			mDbHelper.deleteAllDeliveries();
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
