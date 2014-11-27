/*
 * Copyright (C) 2011 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessies.dalvikexplorer;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

/**
 * A ListActivity that:
 *   Automatically enables filtering.
 *   Automatically offers a search view in the action bar (>= honeycomb).
 *   Automatically saves/restores the filter text and scroll position.
 */
public class BetterListActivity extends ListActivity {
  private static final String LIST_STATE = "BetterListActivity.listState";
  private Parcelable mListState = null;
  
  public BetterListActivity() {
  }
  
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ListView listView = getListView();
    listView.setTextFilterEnabled(true);
    Compatibility.get().configureFastScroll(listView);
  }
  
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options, menu);
    Compatibility compatibility = Compatibility.get();
    compatibility.configureActionBar(this);
    compatibility.configureSearchView(this, menu);
    return true;
  }
  
  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case android.R.id.home:
      Intent intent = new Intent(this, DalvikExplorerActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      return true;
    case R.id.menu_search:
      showSoftKeyboard();
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }
  
  @Override protected void onResume() {
    super.onResume();
    // If we have a remembered ListView filter text and scroll position, use it. 
    if (mListState != null) {
      getListView().post(new Runnable() {
        public void run() {
          getListView().onRestoreInstanceState(mListState);
          mListState = null;
        }
      });
    }
  }
  
  // Unpack the ListView's filter text and scroll position.
  @Override protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);
    mListState = state.getParcelable(LIST_STATE);
  }
  
  // Pack the ListView's filter text and scroll position.
  @Override protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    mListState = getListView().onSaveInstanceState();
    state.putParcelable(LIST_STATE, mListState);
  }
  
  private void showSoftKeyboard() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(getListView(), 0);
  }
}
