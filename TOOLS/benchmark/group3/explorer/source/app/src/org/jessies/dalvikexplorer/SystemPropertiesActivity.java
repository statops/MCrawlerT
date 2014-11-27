package org.jessies.dalvikexplorer;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SystemPropertiesActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "System Properties (" + System.getProperties().size() + ")";
    }

    protected String content(String unused) {
        return getSystemPropertiesAsString();
    }

    // Original in salma-hayek "DebugMenu.java".
    static String getSystemPropertiesAsString() {
        return Utils.sortedStringOfMap(getSystemProperties());
    }

    // Original in salma-hayek "DebugMenu.java".
    private static Map<String, String> getSystemProperties() {
        HashMap<String, String> result = new HashMap<String, String>();
        Properties properties = System.getProperties();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            result.put(key, Utils.escapeForJava(properties.getProperty(key)));
        }
        return result;
    }
}
