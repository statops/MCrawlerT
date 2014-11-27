package org.jessies.dalvikexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSystemsActivity extends BetterListActivity {
  private static class FsListItem implements Comparable<FsListItem>, BetterArrayAdapter.Subtitleable {
    private final String fs;
    private final String mountPoint;
    private final String type;
    private final String options;

    private FsListItem(String fs, String mountPoint, String type, String options) {
      this.fs = fs;
      this.mountPoint = mountPoint;
      this.type = type;
      this.options = options;
    }

    @Override public String toString() {
      return mountPoint;
    }

    public String toSubtitle() {
      return fs + "\n" +
          options + "\n" +
          Compatibility.get().describeFs(mountPoint, type);
    }

    @Override public int compareTo(FsListItem other) {
      return String.CASE_INSENSITIVE_ORDER.compare(this.mountPoint, other.mountPoint);
    }
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setListAdapter(new BetterArrayAdapter<FsListItem>(this, fileSystems(), true));
    setTitle("File Systems (" + getListAdapter().getCount() + ")");
  }

  private List<FsListItem> fileSystems() {
    ArrayList<FsListItem> result = new ArrayList<FsListItem>();

    String[] mountLines = Utils.readLines("/proc/mounts");
    for (String mountLine : mountLines) {
      String[] fields = mountLine.split(" ");
      if (Compatibility.get().isInterestingFileSystem(fields[1])) {
        result.add(new FsListItem(fields[0], fields[1], fields[2], fields[3]));
      }
    }

    Collections.sort(result);
    return result;
  }

  @Override protected void onListItemClick(ListView l, View v, int position, long id) {
    final FsListItem item = (FsListItem) l.getAdapter().getItem(position);
    final Intent intent = new Intent(this, FileSystemActivity.class);
    intent.putExtra("org.jessies.dalvikexplorer.Path", item.mountPoint);
    startActivity(intent);
  }
}
