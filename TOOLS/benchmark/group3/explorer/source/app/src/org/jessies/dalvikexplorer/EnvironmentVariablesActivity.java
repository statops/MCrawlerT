package org.jessies.dalvikexplorer;

public class EnvironmentVariablesActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "Environment Variables (" + System.getenv().size() + ")";
    }

    protected String content(String unused) {
        return getEnvironmentAsString();
    }

    // Original in salma-hayek "DebugMenu.java".
    static String getEnvironmentAsString() {
        return Utils.sortedStringOfMap(System.getenv());
    }
}
