package org.jessies.dalvikexplorer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileSystemActivity extends BetterListActivity {
    private static class FileListItem implements BetterArrayAdapter.Subtitleable {
        private final File file;
        private final String label;
        
        private FileListItem(File file, String label) {
            this.file = file;
            this.label = label;
        }
        
        private FileListItem(File file) {
            this(file, makeLabel(file));
        }
        
        private static String makeLabel(File file) {
            String result = file.getName();
            return file.isDirectory() ? result + "/" : result;
        }
        
        @Override public String toString() {
            return label;
        }
        
        public String toSubtitle() {
            return Utils.prettySize(file.length());
        }
    }
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String path = getIntent().getStringExtra("org.jessies.dalvikexplorer.Path");
        if (path == null) {
            path = "/";
        }
        
        setListAdapter(new BetterArrayAdapter<FileListItem>(this, directoryItems(path), true));
        setTitle(path + " (" + getListAdapter().getCount() + ")");
    }
    
    private List<FileListItem> directoryItems(String path) {
        File[] files = new File(path).listFiles();
        if (files == null) {
            // Returning null is how the retarded java.io.File API reports failure.
            Toast.makeText(this, "Couldn't list directory '" + path + "'", Toast.LENGTH_SHORT).show();
            finish();
            return Collections.emptyList();
        }
        
        Arrays.sort(files);
        
        ArrayList<FileListItem> result = new ArrayList<FileListItem>();
        for (File file : files) {
            result.add(new FileListItem(file));
        }
        return result;
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final FileListItem item = (FileListItem) l.getAdapter().getItem(position);
        final File child = item.file;
        final Intent intent = new Intent(this, child.isDirectory() ? FileSystemActivity.class : FileViewerActivity.class);
        intent.putExtra("org.jessies.dalvikexplorer.Path", child.toString());
        startActivity(intent);
    }
}
