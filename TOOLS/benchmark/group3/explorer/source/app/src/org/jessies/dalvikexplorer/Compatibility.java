package org.jessies.dalvikexplorer;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Build;
import android.view.Menu;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.File;

public abstract class Compatibility {
  public static Compatibility get() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      return new IceCreamSandwichCompatibility();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      return new HoneycombCompatibility();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
      return new GingerbreadCompatibility();
    } else {
      return new PreGingerbreadCompatibility();
    }
  }

  public abstract void configureActionBar(Activity activity);
  public abstract void configureFastScroll(ListView listView);
  public abstract void configureSearchView(ListActivity listActivity, Menu menu);
  public abstract void configureSearchView(TextViewActivity textViewActivity, Menu menu);
  public abstract String describeFs(String mountPoint, String type);
  public abstract boolean isInterestingFileSystem(String mountPoint);

  public static class PreGingerbreadCompatibility extends Compatibility {
    public void configureActionBar(Activity activity) {
      // Nothing to do, since there was no ActionBar pre-honeycomb.
    }
    public void configureFastScroll(ListView listView) {
      listView.setFastScrollEnabled(true);
    }
    public void configureSearchView(ListActivity listActivity, Menu menu) {
      // Nothing to do, since a SearchView couldn't possibly exist pre-honeycomb.
    }
    public void configureSearchView(TextViewActivity textViewActivity, Menu menu) {
      // Nothing to do, since a SearchView couldn't possibly exist pre-honeycomb.
    }
    public String describeFs(String mountPoint, String type) {
      // Pre-Gingerbread we couldn't statfs(3) in a portable way.
      return "(" + type + ")";
    }
    public boolean isInterestingFileSystem(String mountPoint) {
      // Pre-Gingerbread we couldn't statfs(3) in a portable way.
      return true;
    }
  }

  public static class GingerbreadCompatibility extends PreGingerbreadCompatibility {
    public String describeFs(String mountPoint, String type) {
      File f = new File(mountPoint);
      long totalBytes = f.getTotalSpace();
      long freeBytes = f.getFreeSpace();
      long usedBytes = totalBytes - freeBytes;
      return Utils.prettySize(usedBytes) + " of " + Utils.prettySize(totalBytes) + " used (" + type + ")";
    }
    public boolean isInterestingFileSystem(String mountPoint) {
      File f = new File(mountPoint);
      return (f.getTotalSpace() > 0 || mountPoint.equals("/"));
    }
  }

  public static class HoneycombCompatibility extends GingerbreadCompatibility {
    @Override public void configureFastScroll(ListView listView) {
      listView.setFastScrollEnabled(true);
      listView.setFastScrollAlwaysVisible(true);
    }

    @Override public void configureSearchView(final ListActivity listActivity, Menu menu) {
      SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
      searchView.setSubmitButtonEnabled(false);
      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        public boolean onQueryTextChange(String newText) {
          listActivity.getListView().setFilterText(newText);
          return true;
        }
        public boolean onQueryTextSubmit(String query) {
          // We've been filtering as we go, so there's nothing to do here.
          return true;
        }
      });
      searchView.setOnCloseListener(new SearchView.OnCloseListener() {
        public boolean onClose() {
          listActivity.getListView().clearTextFilter();
          return false;
        }
      });
    }

    @Override public void configureSearchView(final TextViewActivity textViewActivity, Menu menu) {
      SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
      searchView.setSubmitButtonEnabled(false);
      searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        public boolean onQueryTextChange(String newText) {
          textViewActivity.setSearchString(newText);
          return true;
        }
        public boolean onQueryTextSubmit(String query) {
          // We've been filtering as we go, so there's nothing to do here.
          return true;
        }
      });
      searchView.setOnCloseListener(new SearchView.OnCloseListener() {
        public boolean onClose() {
          textViewActivity.clearSearch();
          return false;
        }
      });
    }
  }

  public static class IceCreamSandwichCompatibility extends HoneycombCompatibility {
    @Override public void configureActionBar(Activity activity) {
      activity.getActionBar().setHomeButtonEnabled(true);
    }
  }
}
