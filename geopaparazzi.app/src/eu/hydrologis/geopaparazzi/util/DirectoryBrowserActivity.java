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
package eu.hydrologis.geopaparazzi.util;
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
import eu.hydrologis.geopaparazzi.R;

public class DirectoryBrowserActivity extends ListActivity {

    private List<String> items = null;
    private File geoPaparazziDir;
    private String intentId;
    private String extention;
    private FileFilter fileFilter;
    public static final String FOLDER = "folder";

    private File currentDir;
    private boolean doFolder;

    @Override
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.browse);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intentId = extras.getString(Constants.INTENT_ID);
            extention = extras.getString(Constants.EXTENTION);
            if (extention.equals(FOLDER)) {
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
            okButton.setVisibility(View.INVISIBLE);
        }
        Button upButton = (Button) findViewById(R.id.upbutton);
        upButton.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                goUp();
            }
        });

        geoPaparazziDir = ApplicationManager.getInstance(this).getGeoPaparazziDir();
        getFiles(geoPaparazziDir, geoPaparazziDir.listFiles(fileFilter));
    }
    private void handleIntent( String absolutePath ) {
        if (intentId != null) {
            Intent intent = new Intent(intentId);
            intent.putExtra(Constants.PATH, absolutePath);
            startActivity(intent);
        } else {
            Intent intent = new Intent((String) null);
            intent.putExtra(Constants.PATH, absolutePath);
            setResult(Activity.RESULT_OK, intent);
        }
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {
        int selectedRow = (int) id;
        File file = new File(items.get(selectedRow));
        if (file.isDirectory()) {
            currentDir = file;
            getFiles(currentDir, currentDir.listFiles(fileFilter));
        } else {
            String absolutePath = file.getAbsolutePath();
            handleIntent(absolutePath);
            finish();
        }
    }

    private void goUp() {
        File tmpDir = currentDir.getParentFile();
        if (tmpDir != null && tmpDir.exists()) {
            currentDir = tmpDir;
        }
        getFiles(currentDir, currentDir.listFiles(fileFilter));
    }

    private void getFiles( File parent, File[] files ) {
        Arrays.sort(files);
        currentDir = parent;
        items = new ArrayList<String>();
        for( File file : files ) {
            items.add(file.getPath());
        }
        ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.browse_file_row, items);
        setListAdapter(fileList);
    }
}