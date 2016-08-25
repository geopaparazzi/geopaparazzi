/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library.webproject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;

import static eu.geopaparazzi.library.util.LibraryConstants.DATABASE_ID;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_PWD;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_URL;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_USER;

/**
 * Web projects listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WebDataUploadListActivity extends ListActivity {
    public static final int DOWNLOADDATA_RETURN_CODE = 667;

    private static final String ERROR = "error"; //$NON-NLS-1$

    private List<File> dataListToLoad = new ArrayList<>();

    private String user;
    private String pwd;
    private String url;

    private ProgressDialog downloadDataListDialog;
    private ProgressDialog cloudProgressDialog;
    private String[] databases;
    private ArrayAdapter<File> arrayAdapter;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.webdatauploadlist);

        Bundle extras = getIntent().getExtras();
        user = extras.getString(PREFS_KEY_USER);
        pwd = extras.getString(PREFS_KEY_PWD);
        url = extras.getString(PREFS_KEY_URL);
        databases = extras.getStringArray(DATABASE_ID);

        refreshList();
    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        refreshList();
//    }

//    @Override
//    protected void onPause() {
//        GPDialogs.dismissProgressDialog(downloadDataListDialog);
//        GPDialogs.dismissProgressDialog(cloudProgressDialog);
//        super.onPause();
//    }

//    protected void onDestroy() {
//        super.onDestroy();
//    }


    private void refreshList() {
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "refreshing projects list"); //$NON-NLS-1$
        for (String dbPath : databases) {
            File f = new File(dbPath);
            if (f.exists())
                dataListToLoad.add(f);
        }
        arrayAdapter = new ArrayAdapter<File>(this, R.layout.webdatauploadrow, dataListToLoad) {
            @Override
            public View getView(final int position, View cView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.webdatauploadrow, null);
                TextView titleText = (TextView) rowView.findViewById(R.id.titletext);
                titleText.setText(dataListToLoad.get(position).getName());
                TextView descrText = (TextView) rowView.findViewById(R.id.descriptiontext);
                descrText.setText(dataListToLoad.get(position).getParentFile().getAbsolutePath());

                Button uploadButton = (Button) rowView.findViewById(R.id.uploadButton);
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        upload(position);
                    }
                });

                return rowView;
            }
        };

        setListAdapter(arrayAdapter);
    }

    private void upload(final int position) {
        StringAsyncTask task = new StringAsyncTask(this) {
            protected String doBackgroundWork() {
                try {
                    String result = WebDataManager.INSTANCE.uploadData(WebDataUploadListActivity.this, dataListToLoad.get(position), url, user, pwd);
                    return result;
                } catch (Exception e) {
                    return "ERROR: " + e.getLocalizedMessage();
                }
            }

            protected void doUiPostWork(String response) {
                if (response == null) response = "";
                GPDialogs.infoDialog(WebDataUploadListActivity.this, response, new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        };
        task.setProgressDialog(null, "Uploading data to the cloud...", false, null);
        task.execute();


    }
}
