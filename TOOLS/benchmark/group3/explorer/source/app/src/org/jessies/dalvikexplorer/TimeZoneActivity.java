package org.jessies.dalvikexplorer;

import android.util.TimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneActivity extends TextViewActivity {
  protected String extraName() {
    return "org.jessies.dalvikexplorer.TimeZone";
  }

  protected CharSequence title(String timeZoneId) {
    return "Time Zone \"" + timeZoneId + "\"";
  }

  protected String content(String timeZoneId) {
    return describeTimeZone(timeZoneId);
  }

  private String describeTimeZone(String id) {
    final StringBuilder result = new StringBuilder();
    result.append("<html>");

    final TimeZone timeZone = TimeZone.getTimeZone(id);
    final Date now = new Date();
    final DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss Z (EEEE)", Locale.US);

    append(result, "Long Display Name", timeZone.getDisplayName(false, TimeZone.LONG));
    if (timeZone.useDaylightTime()) {
      append(result, "Long Display Name (DST)", timeZone.getDisplayName(true, TimeZone.LONG));
    }

    result.append("<p>");
    append(result, "Short Display Name", timeZone.getDisplayName(false, TimeZone.SHORT));
    if (timeZone.useDaylightTime()) {
      append(result, "Short Display Name (DST)", timeZone.getDisplayName(true, TimeZone.SHORT));
    }

    result.append("<p>");
    iso8601.setTimeZone(TimeZone.getDefault());
    append(result, "Time Here", iso8601.format(now));
    iso8601.setTimeZone(timeZone);
    append(result, "Time There", iso8601.format(now));

    result.append("<p>");
    append(result, "Raw Offset", "UTC" + Utils.offsetString(timeZone.getRawOffset(), true, true));
    append(result, "Current Offset", "UTC" + Utils.offsetString(timeZone.getOffset(System.currentTimeMillis()), true, true));

    result.append("<p>");
    append(result, "Uses DST", timeZone.useDaylightTime());
    if (timeZone.useDaylightTime()) {
      append(result, "DST Savings", Utils.offsetString(timeZone.getDSTSavings(), false, true));
      append(result, "In DST Now", timeZone.inDaylightTime(now));
    }

    result.append("<p>");
    append(result, "Source", "tzdata" + TimeUtils.getTimeZoneDatabaseVersion());

    // TODO: make this available in Android, and get it from there (falling back to our hard-coded copy).
    InputStream is = getResources().openRawResource(R.raw.zone_tab);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      boolean found = false;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(id)) {
          String[] fields = line.split("\t");
          // 0: country code
          // 1: coordinates
          // 2: id
          // 3: comments
          if (!fields[2].equals(id)) {
            continue;
          }
          String countryCode = fields[0];
          String country = new Locale("", countryCode).getDisplayCountry(Locale.getDefault());
          result.append("<p>");
          append(result, "Country", countryCode + " (" + country + ")");
          String iso6709Coordinates = fields[1];
          String dmsCoordinates;
          if (iso6709Coordinates.length() == 11) {
            dmsCoordinates = iso6709Coordinates.replaceAll("([+-])(\\d{2})(\\d{2})([+-])(\\d{3})(\\d{2})", "$1$2\u00b0$3', $4$5\u00b0$6'");
          } else {
            dmsCoordinates = iso6709Coordinates.replaceAll("([+-])(\\d{2})(\\d{2})(\\d{2})([+-])(\\d{3})(\\d{2})(\\d{2})", "$1$2\u00b0$3'$4\", $5$6\u00b0$7'$8\"");
          }
          append(result, "Coordinates", dmsCoordinates);
          String notes = (fields.length > 3) ? fields[3] : "(no notes)";
          append(result, "Notes", notes);
          found = true;
        }
      }
      if (!found) {
        result.append("<p>(Not found in zone.tab.)\n");
      }
    } catch (IOException ex) {
      result.append("<p>(Failed to read zone.tab.)\n");
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }

    try {
      describeTransitions(result, timeZone);
    } catch (Exception unexpected) {
      result.append("(Couldn't find transition data in " + timeZone.getClass() + ".)\n");
      unexpected.printStackTrace();
    }

    return result.toString();
  }

  private static void describeTransitions(StringBuilder result, TimeZone tz) throws IllegalAccessException, NoSuchFieldException {
    Class<?> zoneInfoClass = tz.getClass();
    Field mTransitionsField = zoneInfoClass.getDeclaredField("mTransitions");
    mTransitionsField.setAccessible(true);
    int[] mTransitions = (int[]) mTransitionsField.get(tz);

    long unixNow = System.currentTimeMillis() / 1000;
    boolean shownNow = false;

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss Z", Locale.US);
    df.setTimeZone(tz);
    DateFormat weekdayFormat = new SimpleDateFormat("EEEE", Locale.US);
    weekdayFormat.setTimeZone(tz);

    if (mTransitions.length == 0) {
      // Some rare zones such as Africa/Bujumbura have never had a transition.
      result.append("<p>(This zone has never had a transition.)\n");
    } else {
      Formatter f = new Formatter(result);
      f.format("<p><b>Transitions</b>\n");
      for (int i = 0; i < mTransitions.length; ++i) {
        f.format("<br>");
        if (!shownNow && mTransitions[i] > unixNow) {
          f.format("<br>  -- now (%d) --<br><br>", unixNow);
          shownNow = true;
        }
        String fromTime = formatTime(df, mTransitions[i] - 1);
        String toTime = formatTime(df, mTransitions[i]);
        f.format("  %s ... %s (%d)", fromTime, toTime, mTransitions[i]);

        String weekday = weekdayFormat.format(new Date(1000L * mTransitions[i]));
        if (!weekday.equals("Sunday")) {
          f.format(" -- %s", weekday);
        }
      }
    }
  }

  private static String formatTime(DateFormat df, int s) {
    long ms = ((long) s) * 1000L;
    return df.format(new Date(ms));
  }
}
