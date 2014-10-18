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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMetadata;

import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_CREATIONTS;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_CREATIONUSER;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_DESCRIPTION;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_LASTTS;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_LASTUSER;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_NAME;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.MetadataTableFields.KEY_NOTES;

/**
 * Activity for export tasks.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProjectMetadataActivity extends Activity {

    private SimpleDateFormat projectDatesFormatter = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL;
    private EditText nameText;
    private EditText descriptionText;
    private EditText notesText;
    private EditText creationUserText;
    private EditText lastUserText;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.project_metadata);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        try {
            TextView databasePathTextView = (TextView) findViewById(R.id.databasePathTextView);
            String databaseName = ResourcesManager.getInstance(this).getDatabaseFile().getName();
            databasePathTextView.setText(databaseName);

            HashMap<String, String> projectMetadata = DaoMetadata.getProjectMetadata();

            nameText = (EditText) findViewById(R.id.nameEditText);
            nameText.setText(projectMetadata.get(KEY_NAME.getFieldName()));

            descriptionText = (EditText) findViewById(R.id.descriptionEditText);
            descriptionText.setText(projectMetadata.get(KEY_DESCRIPTION.getFieldName()));

            notesText = (EditText) findViewById(R.id.notesEditText);
            notesText.setText(projectMetadata.get(KEY_NOTES.getFieldName()));

            EditText createTsText = (EditText) findViewById(R.id.creationDateEditText);
            createTsText.setKeyListener(null);
            String creationTsStr = projectMetadata.get(KEY_CREATIONTS.getFieldName());
            long creationTs = Long.parseLong(creationTsStr);
            Date creationDate = new Date(creationTs);
            String creationTsFormatted = projectDatesFormatter.format(creationDate);
            createTsText.setText(creationTsFormatted);

            EditText lastTsText = (EditText) findViewById(R.id.lastDateEditText);
            lastTsText.setKeyListener(null);
            String lastTsStr = projectMetadata.get(KEY_LASTTS.getFieldName());
            String lastTsFormatted;

            try {
                long lastTs = Long.parseLong(lastTsStr);
                Date lastDate = new Date(lastTs);
                lastTsFormatted = projectDatesFormatter.format(lastDate);
            } catch (NumberFormatException e) {
                lastTsFormatted = DaoMetadata.EMPTY_VALUE;
            }
            lastTsText.setText(lastTsFormatted);


            creationUserText = (EditText) findViewById(R.id.creationUserEditText);
            creationUserText.setText(projectMetadata.get(KEY_CREATIONUSER.getFieldName()));

            lastUserText = (EditText) findViewById(R.id.lastUserEditText);
            lastUserText.setText(projectMetadata.get(KEY_LASTUSER.getFieldName()));

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
        String name = nameText.getText().toString();
        String description = descriptionText.getText().toString();
        String notes = notesText.getText().toString();
        String creationUser = creationUserText.getText().toString();
        String lastUser = lastUserText.getText().toString();

        try {
            DaoMetadata.setValue(KEY_NAME.getFieldName(), name);
            DaoMetadata.setValue(KEY_DESCRIPTION.getFieldName(), description);
            DaoMetadata.setValue(KEY_NOTES.getFieldName(), notes);
            DaoMetadata.setValue(KEY_CREATIONUSER.getFieldName(), creationUser);
            DaoMetadata.setValue(KEY_LASTUSER.getFieldName(), lastUser);

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
