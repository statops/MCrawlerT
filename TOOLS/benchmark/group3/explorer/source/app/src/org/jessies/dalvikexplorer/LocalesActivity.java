package org.jessies.dalvikexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class LocalesActivity extends BetterListActivity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<LocaleListItem> languages = gatherLanguages();
        setListAdapter(new BetterArrayAdapter<LocaleListItem>(this, languages, true));
        int languageCount = languages.size() - 1; // Don't count the extra entry for the default locale.
        setTitle("Languages (" + languageCount + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final LocaleListItem item = (LocaleListItem) l.getAdapter().getItem(position);
        String languageName = item.locale().toString();
        final Intent intent;
        if (languageName.contains("_")) {
            intent = new Intent(this, LocaleActivity.class);
            final String localeName = languageName.replace(" (default)", "");
            intent.putExtra("org.jessies.dalvikexplorer.Locale", localeName);
        } else {
            intent = new Intent(this, LocaleCountriesActivity.class);
            intent.putExtra("org.jessies.dalvikexplorer.Language", languageName);
        }
        startActivity(intent);
    }
    
    private static List<LocaleListItem> gatherLanguages() {
        final Locale[] availableLocales = Locale.getAvailableLocales();
        final Locale defaultLocale = Locale.getDefault();
        // Put the default locale at the top of the list...
        final ArrayList<LocaleListItem> result = new ArrayList<LocaleListItem>(availableLocales.length);
        result.add(new LocaleListItem(defaultLocale));
        // ...followed by all the distinct languages...
        TreeSet<String> languages = new TreeSet<String>();
        for (Locale locale : availableLocales) {
            languages.add(locale.getLanguage());
        }
        for (String language : languages) {
            result.add(new LocaleListItem(new Locale(language)));
        }
        return result;
    }
}
