package org.jessies.dalvikexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CharsetsActivity extends BetterListActivity {
    private static class CharsetListItem implements Comparable<CharsetListItem>, BetterArrayAdapter.Subtitleable {
        private final String alias;
        private final Charset charset;
        
        private CharsetListItem(String alias, Charset charset) {
            this.alias = alias;
            this.charset = charset;
        }
        
        public int compareTo(CharsetListItem o) {
            return String.CASE_INSENSITIVE_ORDER.compare(alias, o.alias);
        }
        
        @Override public String toString() {
            String result = alias;
            if (alias.equals(Charset.defaultCharset().name())) {
                result += " (default)";
            }
            return result;
        }
        
        @Override public String toSubtitle() {
            String canonicalName = charset.name();
            if (alias.equals(canonicalName)) {
                return "Canonical";
            }
            return "Alias for " + canonicalName;
        }
    }
    private static final List<CharsetListItem> CHARSETS = gatherCharsets();
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new BetterArrayAdapter<CharsetListItem>(this, CHARSETS, true));
        setTitle("Charsets (" + CHARSETS.size() + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, CharsetActivity.class);
        final CharsetListItem item = (CharsetListItem) l.getAdapter().getItem(position);
        intent.putExtra("org.jessies.dalvikexplorer.Charset", item.charset.name());
        startActivity(intent);
    }
    
    private static List<CharsetListItem> gatherCharsets() {
        // Collect the canonical charsets...
        final List<CharsetListItem> canonicalCharsets = new ArrayList<CharsetListItem>();
        for (Charset charset : Charset.availableCharsets().values()) {
            canonicalCharsets.add(new CharsetListItem(charset.name(), charset));
        }
        Collections.sort(canonicalCharsets);
        
        // ...and their aliases.
        final List<CharsetListItem> aliases = new ArrayList<CharsetListItem>();
        for (Charset charset : Charset.availableCharsets().values()) {
            for (String alias : charset.aliases()) {
                if (!Charset.forName(alias).name().equals(alias)) {
                    aliases.add(new CharsetListItem(alias, charset));
                }
            }
        }
        Collections.sort(aliases);
        
        // Stitch everything together. Default first, then canonical, then the rest.
        final List<CharsetListItem> result = new ArrayList<CharsetListItem>();
        final Charset defaultCharset = Charset.defaultCharset();
        result.add(0, new CharsetListItem(defaultCharset.name(), defaultCharset));
        result.addAll(canonicalCharsets);
        result.addAll(aliases);
        return result;
    }
    
    static String describeCharsets() {
        StringBuilder result = new StringBuilder();
        for (CharsetListItem item : gatherCharsets()) {
            if (Thread.currentThread().isInterrupted()) return null;
            result.append(CharsetActivity.describeCharset(item.charset.name()));
            result.append('\n');
        }
        return result.toString();
    }
}
