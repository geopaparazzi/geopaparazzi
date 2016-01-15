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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
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

import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.core.ISimpleChangeListener;
import eu.hydrologis.geopaparazzi.database.DaoMetadata;
import eu.hydrologis.geopaparazzi.database.objects.Metadata;
import eu.hydrologis.geopaparazzi.dialogs.AddMetadataDialogFragment;

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
            Toolbar toolbar = (Toolbar) findViewById(eu.hydrologis.geopaparazzi.R.id.toolbar);
            String databaseName = ResourcesManager.getInstance(this).getDatabaseFile().getName();
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            saveButton = (FloatingActionButton) findViewById(R.id.saveButton);
            saveButton.hide();

            refreshDataView();

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

    private void refreshDataView() throws IOException {
        LinearLayout container = (LinearLayout) findViewById(R.id.metadataContainer);
        container.removeAllViews();

        projectMetadata = DaoMetadata.getProjectMetadata();
        for (final Metadata metadata : projectMetadata) {
            final RelativeLayout view = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_project_metadata_row, null, false);
            TextInputLayout textInputLayout = (TextInputLayout) view.findViewById(R.id.metadataView);
            textInputLayout.setHint(metadata.label);
            textInputLayout.bringToFront();

            final EditText editText = (EditText) view.findViewById(R.id.metadataEditText);
            container.addView(view);
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
        switch (item.getItemId()) {
            case R.id.action_add_metadata: {
                AddMetadataDialogFragment addMetadataDialogFragment = new AddMetadataDialogFragment();
                addMetadataDialogFragment.show(getSupportFragmentManager(), "add metadata item");
                break;
            }
            case R.id.action_remove_metadata: {
                if (currentSelectedMetadata != null) {
                    Utilities.yesNoMessageDialog(this, "Are you sure you want to remove the entry: " + currentSelectedMetadata.label, new Runnable() {
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
                break;
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
            Utilities.errorDialog(this, e, new Runnable() {
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
