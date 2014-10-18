/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.library.util.activities;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Folder browser activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DirectoryBrowserActivity extends ListActivity {
    /**
     */
    public static final String STARTFOLDERPATH = "STARTFOLDERPATH"; //$NON-NLS-1$

    /**
     * Key that brings the preference key in which to place the selected path.
     */
    public static final String PUT_PATH_PREFERENCE = "PUT_PATH_PREFERENCE"; //$NON-NLS-1$
    /**
     * Key for a new intent to launch on the resulting path.
     */
    public static final String INTENT_ID = "INTENT_ID"; //$NON-NLS-1$
    /**
     * 
     */
    public static final String EXTENTION = "EXTENTION"; //$NON-NLS-1$
    /**
     * 
     */
    public static final String SHOWHIDDEN = "SHOWHIDDEN"; //$NON-NLS-1$
    /**
     * 
     */
    public static final String FOLDER = "folder"; //$NON-NLS-1$

    private List<File> filesList = new ArrayList<File>();
    private File startFolderFile;
    private String intentId;
    private String extention;
    private FileFilter fileFilter;

    private File currentDir;
    private boolean doFolder;
    private boolean doHidden;
    private String startFolder;
    private FileArrayAdapter fileListAdapter;
    private String preferencesKey;

    @Override
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.browse);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intentId = extras.getString(INTENT_ID);
            extention = extras.getString(EXTENTION);
            startFolder = extras.getString(STARTFOLDERPATH);
            doHidden = extras.getBoolean(SHOWHIDDEN, false);
            preferencesKey = extras.getString(PUT_PATH_PREFERENCE);

            if (extention != null && extention.equals(FOLDER)) {
                doFolder = true;
            }

            fileFilter = new FileFilter(){
                public boolean accept( File pathname ) {
                    if (pathname.isDirectory()) {
                        return true;
                    }
                    if (!doFolder) {
                        return pathname.getAbsolutePath().toLowerCase().endsWith(extention.toLowerCase());
                    }
                    return false;
                }
            };
        }

        Button okButton = (Button) findViewById(R.id.okbutton);
        if (doFolder) {
            okButton.setOnClickListener(new OnClickListener(){
                public void onClick( View v ) {
                    String absolutePath = currentDir.getAbsolutePath();

                    if (preferencesKey!=null) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(v.getContext());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(preferencesKey, absolutePath);
                        editor.commit();
                    }

                    handleIntent(absolutePath);
                    finish();
                }
            });
        } else {
            okButton.setEnabled(false);
        }
        Button upButton = (Button) findViewById(R.id.upbutton);
        upButton.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                goUp();
            }
        });

        startFolderFile = new File(startFolder);
        getFiles(startFolderFile, startFolderFile.listFiles(fileFilter));

    }

    private void handleIntent( String absolutePath ) {
        if (intentId != null) {
            Intent intent = new Intent(intentId);
            intent.putExtra(LibraryConstants.PREFS_KEY_PATH, absolutePath);
            startActivity(intent);
        } else {
            Intent intent = new Intent((String) null);
            intent.putExtra(LibraryConstants.PREFS_KEY_PATH, absolutePath);
            setResult(Activity.RESULT_OK, intent);
        }
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {
        int selectedRow = (int) id;
        File file = filesList.get(selectedRow);
        if (file.isDirectory()) {
            File[] filesArray = file.listFiles(fileFilter);
            if (filesArray != null) {
                currentDir = file;
                getFiles(currentDir, filesArray);
            } else {
                filesArray = currentDir.listFiles(fileFilter);
                getFiles(currentDir, filesArray);
            }
        } else {
            String absolutePath = file.getAbsolutePath();
            handleIntent(absolutePath);
            finish();
        }
    }

    private void goUp() {
        File tmpDir = currentDir.getParentFile();
        if (tmpDir != null && tmpDir.exists()) {
            if (tmpDir.canRead()) {
                currentDir = tmpDir;
            }
        }
        getFiles(currentDir, currentDir.listFiles(fileFilter));
    }

    private void getFiles( File parent, File[] files ) {
        Arrays.sort(files);
        currentDir = parent;
        filesList.clear();
        for( File file : files ) {
            if (!doHidden && file.getName().startsWith(".")) { //$NON-NLS-1$
                continue;
            }
            filesList.add(file);
        }

        if (fileListAdapter == null) {
            fileListAdapter = new FileArrayAdapter(this, filesList);
            setListAdapter(fileListAdapter);
        } else {
            fileListAdapter.notifyDataSetChanged();
        }
    }

    private class FileArrayAdapter extends ArrayAdapter<File> {
        private final Activity context;
        private final List<File> files;

        public FileArrayAdapter( Activity context, List<File> files ) {
            super(context, R.id.browselist_text, files);
            this.context = context;
            this.files = files;
        }

        class ViewHolder {
            public TextView textView;
            public ImageView imageView;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent ) {
            ViewHolder holder;
            // Recycle existing view if passed as parameter
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                rowView = inflater.inflate(R.layout.browse_file_row, null, true);
                holder = new ViewHolder();
                holder.textView = (TextView) rowView.findViewById(R.id.browselist_text);
                holder.imageView = (ImageView) rowView.findViewById(R.id.fileIconView);
                rowView.setTag(holder);
            } else {
                holder = (ViewHolder) rowView.getTag();
            }
            File file = files.get(position);
            String fileName = file.getName();
            holder.textView.setText(fileName);
            if (file.isDirectory()){
                holder.imageView.setImageResource(R.drawable.ic_filebrowser_folder);
            }else {
                if (!doFolder && fileName.endsWith(extention.toLowerCase())) {
                    holder.imageView.setImageResource(R.drawable.ic_filebrowser_geopap);
                }else{
                    holder.imageView.setImageResource(R.drawable.ic_filebrowser_file);
                }
            }

            return rowView;
        }
    }

}