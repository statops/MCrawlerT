package org.jessies.dalvikexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocaleCountriesActivity extends BetterListActivity {    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String languageCode = getIntent().getStringExtra("org.jessies.dalvikexplorer.Language");
        List<LocaleListItem> locales = gatherLocales(languageCode);
        setListAdapter(new BetterArrayAdapter<LocaleListItem>(this, locales, true));
        setTitle(new Locale(languageCode).getDisplayLanguage() + " Locales (" + locales.size() + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, LocaleActivity.class);
        final LocaleListItem item = (LocaleListItem) l.getAdapter().getItem(position);
        intent.putExtra("org.jessies.dalvikexplorer.Locale", item.locale().toString());
        startActivity(intent);
    }
    
    private List<LocaleListItem> gatherLocales(String languageCode) {
        // List all the locales for this language.
        final ArrayList<LocaleListItem> result = new ArrayList<LocaleListItem>();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getLanguage().equals(languageCode)) {
                result.add(new LocaleListItem(locale));
            }
        }
        return result;
    }
}
