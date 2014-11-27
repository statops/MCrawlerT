package org.jessies.dalvikexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class TimeZonesActivity extends BetterListActivity {
    private static class TimeZoneListItem implements BetterArrayAdapter.Subtitleable {
        private final TimeZone timeZone;
        
        private TimeZoneListItem(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
        
        @Override public String toString() {
            String result = timeZone.getID();
            if (timeZone.equals(TimeZone.getDefault())) {
                result += " (default)";
            }
            return result;
        }
        
        public String toSubtitle() {
            String result = "UTC" + Utils.offsetString(timeZone.getRawOffset(), true, false);
            if (timeZone.useDaylightTime()) {
                result += " / DST " + Utils.offsetString(timeZone.getDSTSavings(), false, true);
                //String.format(Locale.US, " / DST %+d minutes", timeZone.getDSTSavings()/1000/60);
            }
            return result;
        }
    }
        
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateTimeZones();
    }
    
    private void updateTimeZones() {
        final List<TimeZoneListItem> timeZones = gatherTimeZones();
        setListAdapter(new BetterArrayAdapter<TimeZoneListItem>(this, timeZones, true));
        setTitle("Time Zones (" + timeZones.size() + ")");
    }
    
    // The system's default time zone might have changed while we slept.
    @Override protected void onResume() {
        super.onResume();
        updateTimeZones();
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, TimeZoneActivity.class);
        final TimeZoneListItem item = (TimeZoneListItem) l.getAdapter().getItem(position);
        intent.putExtra("org.jessies.dalvikexplorer.TimeZone", item.timeZone.getID());
        startActivity(intent);
    }
    
    private static List<TimeZoneListItem> gatherTimeZones() {
        final String[] availableIds = TimeZone.getAvailableIDs();
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        // Put the default time zone at the top of the list...
        final List<TimeZoneListItem> result = new ArrayList<TimeZoneListItem>(availableIds.length);
        result.add(new TimeZoneListItem(defaultTimeZone));
        // ...followed by all the others.
        for (String id : availableIds) {
            if (Thread.currentThread().isInterrupted()) return null;
            final TimeZone timeZone = TimeZone.getTimeZone(id);
            if (!timeZone.equals(defaultTimeZone)) {
                result.add(new TimeZoneListItem(timeZone));
            }
        }
        return result;
    }
}
