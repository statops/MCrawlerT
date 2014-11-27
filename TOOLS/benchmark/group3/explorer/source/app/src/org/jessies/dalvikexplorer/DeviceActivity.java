package org.jessies.dalvikexplorer;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceActivity extends TextViewActivity {
  protected CharSequence title(String unused) {
    return "Device Details";
  }

  protected String content(String unused) {
    return getDeviceDetailsAsString(this, getWindowManager());
  }

  // sysconf _SC_NPROCESSORS_CONF and _SC_NPROCESSORS_ONLN have been broken
  // in bionic for various different reasons. /proc parsing was broken until
  // Gingerbread, and even then _SC_NPROCESSORS_CONF was broken because ARM
  // kernels remove offline processors from both /proc/stat and /proc/cpuinfo
  // unlike x86 ones; you need to look in /sys/devices/system/cpu to see all
  // the processors. This should be fixed some time post-JB.
  private static int countHardwareCores() {
    int result = 0;
    for (String file : new File("/sys/devices/system/cpu").list()) {
      if (file.matches("cpu[0-9]+")) {
        ++result;
      }
    }
    return result;
  }

  private static int countEnabledCores() {
    int count = 0;
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader("/proc/stat"));
      String line;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("cpu") && !line.startsWith("cpu ")) {
          ++count;
        }
      }
      return count;
    } catch (IOException ex) {
      return -1;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private static String valueForKey(String[] lines, String key) {
    // If you touch this, test on ARM, MIPS, and x86.
    Pattern p = Pattern.compile("(?i)" + key + "\t*: (.*)");
    for (String line : lines) {
      Matcher m = p.matcher(line);
      if (m.matches()) {
        return m.group(1);
      }
    }
    return null;
  }

  private static int numericValueForKey(String[] lines, String key) {
    String value = valueForKey(lines, key);
    if (value == null) {
      return -1;
    }
    int base = 10;
    if (value.startsWith("0x")) {
      base = 16;
      value = value.substring(2);
    }
    try {
      return Integer.valueOf(value, base);
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  private static String decodeImplementer(int implementer) {
    // From "ETMIDR bit assignments".
    // http://infocenter.arm.com/help/index.jsp?topic=/com.arm.doc.ihi0014q/Bcfihfdj.html
    if (implementer == 0x41) {
      return "ARM";
    } else if (implementer == 0x44) {
      return "Digital Equipment Corporation";
    } else if (implementer == 0x4d) {
      return "Motorola";
    } else if (implementer == 0x51) {
      return "Qualcomm";
    } else if (implementer == 0x56) {
      return "Marvell";
    } else if (implementer == 0x69) {
      return "Intel";
    } else {
      return "unknown (0x" + Integer.toHexString(implementer) + ")";
    }
  }

  private static String decodePartNumber(int part) {
    // TODO: if different implementers don't get discrete ranges,
    // we might need to test implementer here too.
    if (part == 0x920) {
      return "ARM920";
    } else if (part == 0x926) {
      return "ARM926";
    } else if (part == 0xa26) {
      return "ARM1026";
    } else if (part == 0xb02) {
      return "ARM11mpcore";
    } else if (part == 0xb36) {
      return "ARM1136";
    } else if (part == 0xb56) {
      return "ARM1156";
    } else if (part == 0xb76) {
      return "ARM1176";
    } else if (part == 0xc05) {
      return "Cortex-A5";
    } else if (part == 0xc07) {
      return "Cortex-A7";
    } else if (part == 0xc08) {
      return "Cortex-A8";
    } else if (part == 0xc09) {
      return "Cortex-A9";
    } else if (part == 0xc0f) {
      return "Cortex-A15";
    } else if (part == 0xc14) {
      return "Cortex-R4";
    } else if (part == 0xc15) {
      return "Cortex-R5";
    } else if (part == 0xc20) {
      return "Cortex-M0";
    } else if (part == 0xc21) {
      return "Cortex-M1";
    } else if (part == 0xc23) {
      return "Cortex-M3";
    } else if (part == 0xc24) {
      return "Cortex-M4";
    } else if (part == 0x00f) {
      return "Snapdragon S1 (Scorpion)";
    } else if (part == 0x02d) {
      return "Snapdragon S3 (Scorpion)";
    } else if (part == 0x04d) {
      return "Snapdragon S4 Plus (Krait)";
    } else if (part == 0x06f) {
      return "Snapdragon S4 Pro (Krait)";
    } else {
      return "unknown (0x" + Integer.toHexString(part) + ")";
    }
  }

  String getDeviceDetailsAsString(Activity context, WindowManager wm) {
    final StringBuilder result = new StringBuilder();
    result.append("<html>");

    String[] procCpuLines = Utils.readFile("/proc/cpuinfo").split("\n");
    // x86 kernels use "processor" as an integer to number sockets.
    // They use "model name" to describe the processor.
    // ARM kernels use "Processor" to describe the processor.
    String processor = valueForKey(procCpuLines, "model name");
    if (processor == null) {
      processor = valueForKey(procCpuLines, "Processor");
    }
    append(result, "Processor", processor);

    int hardwareCoreCount = countHardwareCores();
    int enabledCoreCount = countEnabledCores();
    String cores = Integer.toString(hardwareCoreCount);
    if (enabledCoreCount != hardwareCoreCount) {
      cores += " (enabled: " + enabledCoreCount + ")";
    }
    append(result, "Cores", cores);

    result.append("<p>");
    try {
      String minFrequency = Utils.readFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq").trim();
      String maxFrequency = Utils.readFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").trim();
      long minFrequencyHz = Long.parseLong(minFrequency) * 1000L;
      long maxFrequencyHz = Long.parseLong(maxFrequency) * 1000L;
      append(result, "CPU Speed", Utils.prettyHz(maxFrequencyHz) + " (idles at " + Utils.prettyHz(minFrequencyHz) + ")");
    } catch (Exception unexpected) {
      result.append("(Unable to determine CPU frequencies.)");
    }

    // ARM-specific.
    int implementer = numericValueForKey(procCpuLines, "CPU implementer");
    if (implementer != -1) {
      result.append("<p>");
      append(result, "CPU Implementer", decodeImplementer(implementer));
      append(result, "CPU Part", decodePartNumber(numericValueForKey(procCpuLines, "CPU part")));
      // These two are included in the kernel's formatting of "Processor".
      //result.append("CPU Architecture: " + numericValueForKey(procCpuLines, "CPU architecture") + "\n");
      //result.append("CPU Revision: " + numericValueForKey(procCpuLines, "CPU revision") + "\n");
      append(result, "CPU Variant", numericValueForKey(procCpuLines, "CPU variant"));
      result.append("<p>");
      append(result, "Hardware", valueForKey(procCpuLines, "Hardware"));
      append(result, "Revision", valueForKey(procCpuLines, "Revision"));
      append(result, "Serial", valueForKey(procCpuLines, "Serial"));
    }

    // MIPS-specific.
    // TODO: is "CPU architecture" ever more specific than "MIPS"?
    if ("MIPS".equals(valueForKey(procCpuLines, "CPU architecture"))) {
      result.append("<p>");
      append(result, "CPU Implementer", valueForKey(procCpuLines, "CPU implementer"));
      append(result, "CPU Model", valueForKey(procCpuLines, "cpu model"));
      result.append("<p>");
      append(result, "Hardware", valueForKey(procCpuLines, "Hardware"));
      append(result, "Revision", valueForKey(procCpuLines, "Revision"));
      append(result, "Serial", valueForKey(procCpuLines, "Serial"));
    }

    // Intel-specific.
    String cacheSize = valueForKey(procCpuLines, "cache size");
    String addressSizes = valueForKey(procCpuLines, "address sizes");
    if (cacheSize != null) {
      result.append("<p>");
      append(result, "Cache", cacheSize);
      append(result, "Address Sizes", addressSizes);
    }

    String features = valueForKey(procCpuLines, "Features");
    if (features == null) {
      features = valueForKey(procCpuLines, "flags");
    }
    result.append("<p><b>CPU Features</b>\n");
    result.append(Utils.sortedStringOfStrings("<br>&nbsp;&nbsp;", features.split(" ")));

    MemInfo memInfo = readMemInfo();
    result.append("<p><b>Memory</b>\n");
    result.append("<br>&nbsp;&nbsp;Total: " + Utils.prettySize(memInfo.total));
    result.append("<br>&nbsp;&nbsp;Used: " + Utils.prettySize(memInfo.used));
    result.append("<br>&nbsp;&nbsp;Free: " + Utils.prettySize(memInfo.free));
    result.append("<br>&nbsp;&nbsp;Buffers: " + Utils.prettySize(memInfo.buffers));

    try {
      ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
      Method isLowRamDeviceMethod = ActivityManager.class.getMethod("isLowRamDevice");
      boolean isLowRamDevice = (Boolean) isLowRamDeviceMethod.invoke(activityManager);
      result.append("<p><b>Is Low Memory Device:</b> " + isLowRamDevice);
    } catch (Exception ignored) {
    }

    Display display = wm.getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);
    result.append("<p>");
    append(result, "Screen Density", metrics.densityDpi + "dpi (" + metrics.density + "x DIP)");
    append(result, "Exact DPI", metrics.xdpi + " x " + metrics.ydpi);
    int widthPixels = metrics.widthPixels;
    int heightPixels = metrics.heightPixels;
    try {
      widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
      heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
    } catch (Exception ignored) {
    }
    try {
      Point realSize = new Point();
      Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
      widthPixels = realSize.x;
      heightPixels = realSize.y;
    } catch (Exception ignored) {
    }
    append(result, "Screen Size", widthPixels + " x " + heightPixels + " pixels");
    double widthInches = widthPixels/metrics.xdpi;
    double heightInches = heightPixels/metrics.ydpi;
    double diagonalInches = Math.sqrt(widthInches*widthInches + heightInches*heightInches);
    append(result, "Approximate Dimensions", String.format("%.1f\" x %.1f\" (%.1f\" diagonal)", widthInches, heightInches, diagonalInches));

    return result.toString();
  }

  private static String getFieldReflectively(Build build, String fieldName) {
    try {
      final Field field = Build.class.getField(fieldName);
      return field.get(build).toString();
    } catch (Exception ex) {
      return "unknown";
    }
  }

  static class MemInfo {
    long total;
    long used;
    long free;
    long buffers;
  }

  private static MemInfo readMemInfo() {
    MemInfo result = new MemInfo();
    String[] lines = Utils.readLines("/proc/meminfo");
    for (String line : lines) {
      String[] fields = line.split(" +");
      if (fields[0].equals("MemTotal:")) {
        result.total = Long.parseLong(fields[1]) * 1024;
      } else if (fields[0].equals("MemFree:")) {
        result.free = Long.parseLong(fields[1]) * 1024;
      } else if (fields[0].equals("Buffers:")) {
        result.buffers = Long.parseLong(fields[1]) * 1024;
      } else {
        break;
      }
    }
    result.used = result.total - result.free;
    return result;
  }
}
