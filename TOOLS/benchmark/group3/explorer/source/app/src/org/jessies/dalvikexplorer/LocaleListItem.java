package org.jessies.dalvikexplorer;

import java.util.Locale;

public class LocaleListItem implements BetterArrayAdapter.Subtitleable {
    private final Locale locale;
    
    public LocaleListItem(Locale locale) {
        this.locale = locale;
    }
    
    public Locale locale() {
        return locale;
    }
    
    @Override public String toString() {
        String result = locale.toString();
        if (locale.equals(Locale.getDefault())) {
            result += " (default)";
        }
        return result;
    }
    
    @Override public String toSubtitle() {
        return locale.getDisplayName();
    }
}
