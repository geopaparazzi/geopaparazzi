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
package eu.hydrologis.geopaparazzi.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMetadata;
import eu.hydrologis.geopaparazzi.database.objects.Metadata;

/**
 * Activity for viewing project metadata.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProjectMetadataActivity extends AppCompatActivity {

    private List<Metadata> projectMetadata;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_project_metadata);

        Toolbar toolbar = (Toolbar) findViewById(eu.hydrologis.geopaparazzi.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        try {
            String databaseName = ResourcesManager.getInstance(this).getDatabaseFile().getName();
            toolbar.setTitle(databaseName);

            projectMetadata = DaoMetadata.getProjectMetadata();

            LinearLayout container = (LinearLayout) findViewById(R.id.metadataContainer);

            LayoutInflater layoutInflater = LayoutInflater.from(this);
            for (final Metadata metadata : projectMetadata) {
                View view = layoutInflater.inflate(R.layout.activity_project_metadata_row, container);
                EditText editText = (EditText) view.findViewById(R.id.metadataEditText);
                editText.setHint(metadata.label);
                editText.setText(metadata.value);
                editText.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void afterTextChanged(Editable s) {
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start,
                                                  int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start,
                                              int before, int count) {
                        metadata.value = s.toString();
                    }
                });
            }


        } catch (Exception e) {
            GPLog.error(this, null, e);
            Utilities.errorDialog(this, e, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }

    }


    public void save(View view) {

        try {
            for (Metadata metadata : projectMetadata) {
                DaoMetadata.setValue(metadata.key, metadata.value);
            }

            finish();
        } catch (IOException e) {
            GPLog.error(this, null, e);
            Utilities.errorDialog(this, e, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }


    }
}
