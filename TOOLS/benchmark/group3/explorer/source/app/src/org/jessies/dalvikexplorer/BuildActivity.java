package org.jessies.dalvikexplorer;

import android.app.Activity;
import android.content.pm.FeatureInfo;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class BuildActivity extends TextViewActivity {
  protected CharSequence title(String unused) {
    return "Build Details";
  }

  protected String content(String unused) {
    return getBuildDetailsAsString(this, getWindowManager());
  }

  static String getBuildDetailsAsString(Activity context, WindowManager wm) {
    final Build build = new Build();

    // Fields keep being added to Build. These are the ones that aren't in our minSdkVersion.
    // http://developer.android.com/resources/dashboard/platform-versions.html
    final String bootloader = getFieldReflectively(build, "BOOTLOADER"); // API 8.
    final String cpuAbi2 = getFieldReflectively(build, "CPU_ABI2"); // API 8.
    final String hardware = getFieldReflectively(build, "HARDWARE"); // API 8.
    final String serial = getFieldReflectively(build, "SERIAL"); // API 9.

    final StringBuilder result = new StringBuilder();
    result.append("<html>");
    append(result, "Manufacturer", Build.MANUFACTURER); // "samsung"
    append(result, "Model", Build.MODEL); // "Galaxy Nexus"

    result.append("<p>");
    append(result, "Brand", Build.BRAND); // "google"
    append(result, "Board", Build.BOARD); // "tuna"
    append(result, "Device", Build.DEVICE); // "toro"
    append(result, "Hardware", hardware); // 8
    append(result, "Product", Build.PRODUCT); // "mysid"

    result.append("<p>");
    append(result, "Serial Number", serial);

    result.append("<p>");
    append(result, "Bootloader", bootloader); // "PRIMELA03"
    append(result, "Radio", getRadioVersion()); // "I515.XX V.FA02 / I515.FA02"

    result.append("<p>");
    append(result, "Build Fingerprint", Build.FINGERPRINT); // "verizon/voles/sholes/sholes:2.1/ERD76/22321:userdebug/test-keys"

    result.append("<p>");
    append(result, "Release", Build.VERSION.RELEASE); // "JellyBean"
    append(result, "Codename", Build.VERSION.CODENAME); // "JellyBean"
    append(result, "Build Version", Build.ID); // "JRM38"
    append(result, "Build Type", Build.TYPE); // "userdebug"
    append(result, "Build Tags", Build.TAGS); // "dev-keys"
    append(result, "Build Date", DateFormat.format("yyyy-MM-dd", Build.TIME)); // "2012-02-07"
    append(result, "Built By", Build.USER + "@" + Build.HOST);
    append(result, "Build Number", Build.VERSION.INCREMENTAL);

    result.append("<p>");
    append(result, "API level", Build.VERSION.SDK_INT); // 17

    result.append("<p>");
    append(result, "CPU ABIs", Build.CPU_ABI + " " + cpuAbi2); // "armeabi-v7a"

    result.append("<p>");
    append(result, "Kernel Version", Utils.readFile("/proc/version")); // "Linux version 3.0.8-g034fec9 (android-build@vpbs1.mtv.corp.google.com) (gcc version 4.4.3 (GCC) ) #1 SMP PREEMPT Tue Mar 13 15:46:20 PDT 2012"

    result.append("<p>");
    append(result, "DalvikVM Heap Size", Runtime.getRuntime().maxMemory() / (1024*1024) + " MiB"); // "64 MiB"

    try {
      Class<?> vmDebugClass = Class.forName("dalvik.system.VMDebug");
      Method getVmFeatureListMethod = vmDebugClass.getDeclaredMethod("getVmFeatureList");
      String[] vmFeatures = (String[]) getVmFeatureListMethod.invoke(null);
      result.append("<p><b>DalvikVM features</b>\n");
      result.append(Utils.sortedStringOfStrings("<br>&nbsp;&nbsp;", vmFeatures));
    } catch (Throwable ignored) {
    }

    String openGlEsVersion = null;
    ArrayList<String> features = new ArrayList<String>();
    for (FeatureInfo feature : context.getPackageManager().getSystemAvailableFeatures()) {
      if (feature.name != null) {
        features.add(feature.name);
      } else {
        openGlEsVersion = feature.getGlEsVersion();
      }
    }

    if (openGlEsVersion != null) {
      result.append("<p>");
      append(result, "OpenGL ES version", openGlEsVersion);
    }

    result.append("<p><b>Features</b>\n");
    result.append(Utils.sortedStringOfStrings("<br>&nbsp;&nbsp;", features.toArray(new String[0])));

    result.append("<p><b>Shared Java libraries</b>\n");
    result.append(Utils.sortedStringOfStrings("<br>&nbsp;&nbsp;", context.getPackageManager().getSystemSharedLibraryNames()));

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

  private static String getRadioVersion() {
    String radioVersion;
    try {
      final Method method = Build.class.getMethod("getRadioVersion");
      radioVersion = (String) method.invoke(null);
    } catch (Exception ex) {
      radioVersion = getFieldReflectively(new Build(), "RADIO");
    }
    if (radioVersion.length() == 0) {
      radioVersion = "(no radio)";
    }
    return radioVersion;
  }
}
