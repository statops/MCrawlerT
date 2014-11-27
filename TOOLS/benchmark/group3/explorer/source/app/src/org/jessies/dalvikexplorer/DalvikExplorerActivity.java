package org.jessies.dalvikexplorer;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.Arrays;

public class DalvikExplorerActivity extends ListActivity {
  // A (list view label, activity class) pair.
  private static class NamedActivity {
    private final String name;
    private final Class<?> activityClass;
    private NamedActivity(String name, Class<?> activityClass) {
      this.name = name;
      this.activityClass = activityClass;
    }
    @Override public String toString() {
      return name;
    }
  }

  private static final NamedActivity[] ACTIVITIES = new NamedActivity[] {
    new NamedActivity("Build Details", BuildActivity.class),
    new NamedActivity("Charsets", CharsetsActivity.class),
    new NamedActivity("Device Details", DeviceActivity.class),
    new NamedActivity("Environment Variables", EnvironmentVariablesActivity.class),
    new NamedActivity("File Systems", FileSystemsActivity.class),
    new NamedActivity("Locales", LocalesActivity.class),
    new NamedActivity("Sensors", SensorsActivity.class),
    new NamedActivity("System Properties", SystemPropertiesActivity.class),
    new NamedActivity("Time Zones", TimeZonesActivity.class)
  };

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setListAdapter(new BetterArrayAdapter<NamedActivity>(this, Arrays.asList(ACTIVITIES), false));

    String title = "Dalvik Explorer";
    try {
      title += " " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException ignored) {
    }
    setTitle(title);
  }

  @Override protected void onListItemClick(ListView l, View v, int position, long id) {
    final NamedActivity destinationActivity = ACTIVITIES[position];
    startActivity(new Intent(this, destinationActivity.activityClass));
  }
}
