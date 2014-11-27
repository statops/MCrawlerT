package org.jessies.dalvikexplorer;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Utils {
    // Original in salma-hayek "DebugMenu.java".
    public static String sortedStringOfMap(Map<String, String> hash) {
        StringBuilder builder = new StringBuilder();
        String[] keys = hash.keySet().toArray(new String[hash.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            builder.append(key + "=" + hash.get(key) + "\n");
        }
        return builder.toString();
    }

    public static String sortedStringOfStrings(String prefix, Set<String> strings) {
        return sortedStringOfStrings(prefix, strings.toArray(new String[strings.size()]));
    }

    public static String sortedStringOfStrings(String prefix, String[] strings) {
        String[] sortedStrings = strings.clone();
        Arrays.sort(sortedStrings, String.CASE_INSENSITIVE_ORDER);
        StringBuilder result = new StringBuilder();
        for (String s : sortedStrings) {
            result.append(prefix).append(s).append("\n");
        }
        return result.toString();
    }

    // Original in salma-hayek "StringUtilities.java".
    public static String escapeForJava(CharSequence s) {
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char c = s.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\n') {
                result.append("\\n");
            } else if (c == '\r') {
                result.append("\\r");
            } else if (c == '\t') {
                result.append("\\t");
            } else if (c < ' ' || c > '~') {
                result.append(String.format("\\u%04x", c)); // android-changed.
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // Original in salma-hayek "StringUtilities.java".
    public static String join(CharSequence[] strings, CharSequence separator) {
        StringBuilder result = new StringBuilder();
        for (CharSequence string : strings) {
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(string);
        }
        return result.toString();
    }

    public static String offsetString(int ms, boolean showHours, boolean showMinutes) {
        int minutes = ms/1000/60;
        String result = "";
        if (showHours) {
            result += String.format(Locale.US, "%+03d:%02d", minutes / 60, Math.abs(minutes % 60));
        }
        if (showMinutes) {
            result += String.format(Locale.US, "%s%+d minutes%s", showHours ? " (" : "", minutes, showHours ? ")" : "");
        }
        return result;
    }

    public static String appVersion(Context context) {
        String version = "unknown";
        try {
            String packageName = context.getPackageName();
            version = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (NameNotFoundException ignored) {
        }
        return version;
    }

  public static String readFile(String path) {
    StringBuilder sb = new StringBuilder();
    for (String line : readLines(path)) {
      sb.append(line).append('\n');
    }
    return sb.toString();
  }

  public static String[] readLines(String path) {
    ArrayList<String> lines = new ArrayList<String>();
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(path));
      String line;
      while ((line = in.readLine()) != null) {
        lines.add(line);
      }
      return lines.toArray(new String[0]);
    } catch (IOException ex) {
      return null;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  public static String prettySize(long bytes) {
    String unit = "";
    double n = bytes;
    if (n > 1024) {
      n /= 1024;
      unit = "Ki";
    }
    if (n > 1024) {
      n /= 1024;
      unit = "Mi";
    }
    if (n > 1024) {
      n /= 1024;
      unit = "Gi";
    }
    return String.format("%.1f %sB", n, unit);
  }

  public static String prettyHz(long hz) {
    String unit = "";
    double n = hz;
    if (n > 1000) {
      n /= 1000;
      unit = "K";
    }
    if (n > 1000) {
      n /= 1000;
      unit = "M";
    }
    if (n > 1000) {
      n /= 1000;
      unit = "G";
    }
    return String.format("%.1f %sHz", n, unit);
  }
}
