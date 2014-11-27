package org.jessies.dalvikexplorer;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLConnection;

public class FileViewerActivity extends TextViewActivity {
    private static final int CONTEXT_MENU_VIEW = 2;

    protected String extraName() {
        return "org.jessies.dalvikexplorer.Path";
    }

    protected CharSequence title(String path) {
        return path;
    }

    protected String content(String path) {
        return readFile(path);
    }

    private String readFile(String path) {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
                result.append('\n');
            }
            in.close();
        } catch (IOException ex) {
            Toast.makeText(this, "Couldn't read '" + path + "'", Toast.LENGTH_SHORT).show();
            finish();
        }
        return result.toString();
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CONTEXT_MENU_VIEW,  0, "Send 'view' intent");
    }

    @Override public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CONTEXT_MENU_VIEW:
            return sendViewIntent();
        default:
            return super.onContextItemSelected(item);
        }
    }

    private boolean sendViewIntent() {
        File file = new File(getExtraValue());
        Uri path = Uri.fromFile(file);
        String mimeType = URLConnection.guessContentTypeFromName(file.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("View failed");
            builder.setMessage("No application responded.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
        return true;
    }
}
