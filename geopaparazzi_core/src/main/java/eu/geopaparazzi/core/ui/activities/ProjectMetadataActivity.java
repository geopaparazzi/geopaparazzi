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
package eu.geopaparazzi.core.ui.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.utilities.ISimpleChangeListener;
import eu.geopaparazzi.core.database.DaoMetadata;
import eu.geopaparazzi.core.database.objects.Metadata;
import eu.geopaparazzi.core.ui.dialogs.AddMetadataDialogFragment;

/**
 * Activity for viewing project metadata.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProjectMetadataActivity extends AppCompatActivity implements ISimpleChangeListener {

    private List<Metadata> projectMetadata;
    private FloatingActionButton saveButton;
    private Metadata currentSelectedMetadata = null;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_project_metadata);
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            String databaseName = ResourcesManager.getInstance(this).getDatabaseFile().getName();
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            TextView titleView = findViewById(R.id.metadataTitle);
            titleView.setText(databaseName);

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            saveButton = findViewById(R.id.saveButton);
            saveButton.hide();

            refreshDataView();

        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }


    }

    private void refreshDataView() throws IOException {
        LinearLayout container = findViewById(R.id.metadataContainer);
        container.removeAllViews();

        projectMetadata = DaoMetadata.getProjectMetadata();
        for (final Metadata metadata : projectMetadata) {
            final RelativeLayout view = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_project_metadata_row, null, false);
            TextInputLayout textInputLayout = view.findViewById(R.id.metadataView);
            textInputLayout.setHint(metadata.label);
            textInputLayout.bringToFront();

            final TextInputEditText editText = view.findViewById(R.id.metadataEditText);
            container.addView(view);

            if (metadata.key.equals("creationts") || metadata.key.equals("lastts")){
                String value = metadata.value;
                try{
                    Date date = new Date(Long.parseLong(value));
                    value = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(date);
                }catch (Exception e){
                    // ignore
                }
                editText.setText(value);
                editText.setKeyListener(null);
            }else {
                editText.setText(metadata.value);
            }
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

                    if (saveButton != null)
                        saveButton.show();
                }
            });

            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        currentSelectedMetadata = metadata;
                    }
                }
            });
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_metadata, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.action_add_metadata) {
            AddMetadataDialogFragment addMetadataDialogFragment = new AddMetadataDialogFragment();
            addMetadataDialogFragment.show(getSupportFragmentManager(), "add metadata item");
        } else if (i == R.id.action_remove_metadata) {
            if (currentSelectedMetadata != null) {
                GPDialogs.yesNoMessageDialog(this, "Are you sure you want to remove the entry: " + currentSelectedMetadata.label, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DaoMetadata.deleteItem(currentSelectedMetadata.key);
                            currentSelectedMetadata = null;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        refreshDataView();
                                    } catch (IOException e) {
                                        GPLog.error(this, null, e);
                                    }
                                }
                            });
                        } catch (IOException e) {
                            GPLog.error(this, null, e);
                        }
                    }
                }, null);
            }
        }
        return super.onOptionsItemSelected(item);
    }


    public void save(View view) {
        try {
            for (Metadata metadata : projectMetadata) {
                DaoMetadata.setValue(metadata.key, metadata.value);
            }
            finish();
        } catch (IOException e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(this, e, new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            });
        }
    }

    @Override
    public void changOccurred() {
        try {
            refreshDataView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
