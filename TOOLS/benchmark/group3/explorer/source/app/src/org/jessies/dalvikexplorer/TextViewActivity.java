package org.jessies.dalvikexplorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract superclass for our TextView-based activities.
 */
public abstract class TextViewActivity extends Activity {
    private static final int CONTEXT_MENU_COPY = 0;
    private static final int CONTEXT_MENU_MAIL = 1;

    static public class GeoFilter implements Linkify.MatchFilter, Linkify.TransformFilter {
        private static final Pattern GEO_PATTERN = Pattern.compile("\\+?(-?\\d+)\u00b0(\\d+)'(\\d+)?\"?, \\+?(-?\\d+)\u00b0(\\d+)'(\\d+)?\"?");

        private static final String GEO_SCHEME = "geo:";

        public String transformUrl(Matcher m, String url) {
            return fromDms(m, 1, 2, 3) + "," + fromDms(m, 4, 5, 6) + "?z=7";
        }

        public boolean acceptMatch(CharSequence s, int start, int end) {
            return true;
        }

        private static double fromDms(Matcher matcher, int d, int m, int s) {
            int dd = Integer.parseInt(matcher.group(d), 10);
            int mm = Integer.parseInt(matcher.group(m), 10);
            int ss = (matcher.group(s) == null) ? 0 : Integer.parseInt(matcher.group(s), 10);
            return Math.signum(dd) * (Math.abs(dd) + mm/60.0 + ss/3600.0);
        }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final TextView textView = (TextView) findViewById(R.id.output);
        registerForContextMenu(textView);

        final String extraValue = getExtraValue();
        String content = content(extraValue);
        if (content.startsWith("<html>")) {
          textView.setText(Html.fromHtml(content), TextView.BufferType.SPANNABLE);
        } else {
          textView.setText(content, TextView.BufferType.SPANNABLE);
        }
        setTitle(title(extraValue));

        Linkify.addLinks(textView, GeoFilter.GEO_PATTERN, GeoFilter.GEO_SCHEME, new GeoFilter(), new GeoFilter());

        final EditText searchView = (EditText) findViewById(R.id.search);
        searchView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                setSearchString(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        Compatibility compatibility = Compatibility.get();
        compatibility.configureActionBar(this);
        compatibility.configureSearchView(this, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, DalvikExplorerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        case R.id.menu_search:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                final EditText searchView = (EditText) findViewById(R.id.search);
                searchView.setVisibility(View.VISIBLE);
                searchView.requestFocus();
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    protected String extraName() {
        return null;
    }

    protected abstract CharSequence title(String extraValue);

    protected abstract String content(String extraValue);

    protected static void append(StringBuilder sb, CharSequence key, Object value) {
      sb.append("<b>");
      sb.append(key);
      sb.append(":</b> ");
      sb.append(value);
      sb.append("<br>");
    }

    protected String getExtraValue() {
        final String extraName = extraName();
        return (extraName != null) ? getIntent().getStringExtra(extraName) : null;
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Details");
        menu.add(0, CONTEXT_MENU_COPY,  0, "Copy to clipboard"); // "Copy" might be ambiguous in FileViewerActivity.
        menu.add(0, CONTEXT_MENU_MAIL,  0, "Send as mail");
    }

    @Override public boolean onContextItemSelected(MenuItem item) {
        final TextView textView = (TextView) findViewById(R.id.output);
        final CharSequence title = getTitle();
        final CharSequence content = textView.getText();
        switch (item.getItemId()) {
        case CONTEXT_MENU_COPY:
            return copyToClipboard(title, content);
        case CONTEXT_MENU_MAIL:
            return mail(title, content);
        default:
            return super.onContextItemSelected(item);
        }
    }

    private boolean copyToClipboard(CharSequence title, CharSequence content) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(title + "\n\n" + content);
        return true;
    }

    private boolean mail(CharSequence title, CharSequence content) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Dalvik Explorer " + Utils.appVersion(this) + ": " + title);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(intent);
        return true;
    }

    public void setSearchString(String needle) {
        clearSearch();
        if (needle.length() == 0) {
            return;
        }
        final TextView textView = (TextView) findViewById(R.id.output);
        Spannable spannable = (Spannable) textView.getText();
        String haystack = spannable.toString();
        for (int index = 0; index != -1; ) {
            index = haystack.indexOf(needle, index);
            if (index != -1) {
                spannable.setSpan(new BackgroundColorSpan(0xff337733), index, index + needle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index += needle.length();
            }
        }
    }

    public void clearSearch() {
        final TextView textView = (TextView) findViewById(R.id.output);
        SpannableString spannable = (SpannableString) textView.getText();
        for (Object o : spannable.getSpans(0, spannable.length() - 1, BackgroundColorSpan.class)) {
            spannable.removeSpan(o);
        }
    }
}
