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
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.LibraryConstants;

public class DirectoryBrowserActivity extends ListActivity {
    public static final String STARTFOLDERPATH = "STARTFOLDERPATH"; //$NON-NLS-1$
    /**
     * Key for a new intent to launch on the resulting path.
     */
    public static final String INTENT_ID = "INTENT_ID"; //$NON-NLS-1$
    public static final String EXTENTION = "EXTENTION"; //$NON-NLS-1$
    public static final String SHOWHIDDEN = "SHOWHIDDEN"; //$NON-NLS-1$
    public static final String FOLDER = "folder"; //$NON-NLS-1$

    private List<String> items = null;
    private List<String> itemsNames = null;
    private File startFolderFile;
    private String intentId;
    private String extention;
    private FileFilter fileFilter;

    private File currentDir;
    private boolean doFolder;
    private boolean doHidden;
    private String startFolder;

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
        File file = new File(items.get(selectedRow));
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
        items = new ArrayList<String>();
        itemsNames = new ArrayList<String>();
        for( File file : files ) {
            if (!doHidden && file.getName().startsWith(".")) { //$NON-NLS-1$
                continue;
            }
            items.add(file.getAbsolutePath());
            itemsNames.add(file.getName());
        }
        ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.browse_file_row, itemsNames);
        setListAdapter(fileList);
    }
}