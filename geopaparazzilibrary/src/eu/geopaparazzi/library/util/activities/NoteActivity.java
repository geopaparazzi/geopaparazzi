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

import java.sql.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Notes taking activity.
 * 
 * <p>
 * Note that location and time of the note are taken at the moment 
 * of the saving of the note.
 * </p>
 * 
 * <p>
 * The activity returns with an array that contains the note's info in the following order
 * (use the key {@link LibraryConstants#PREFS_KEY_NOTE}):
 * </p>
 * <ul>
 *  <li>longitude</li>
 *  <li>latitude</li>
 *  <li>elevation</li>
 *  <li>note date in format: yyyy-MM-dd HH:mm:ss</li>
 *  <li>the text of the note</li>
 * </ul>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NoteActivity extends Activity {
    private EditText noteText;
    private int LANDSCAPE_LINES = 5;
    private int PORTRAIT_LINES = 14;
    private int linesNum = PORTRAIT_LINES;
    private double latitude;
    private double longitude;
    private double elevation;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.note);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getDouble(LibraryConstants.LATITUDE);
            longitude = extras.getDouble(LibraryConstants.LONGITUDE);
            elevation = extras.getDouble(LibraryConstants.ELEVATION);
        }

        noteText = (EditText) findViewById(R.id.noteentry);
        noteText.setLines(linesNum);

        Button saveButton = (Button) findViewById(R.id.ok);
        saveButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                try {
                    Date sqlDate = new Date(System.currentTimeMillis());
                    StringBuilder sB = new StringBuilder(noteText.getText());
                    String noteString = sB.toString();

                    String sqlDateString = TimeUtilities.INSTANCE.TIME_FORMATTER_UTC.format(sqlDate);

                    String[] noteArray = {//
                    String.valueOf(longitude), //
                            String.valueOf(latitude), //
                            String.valueOf(elevation), //
                            sqlDateString, //
                            noteString,//
                            "POI", // note category //$NON-NLS-1$
                            "" // form //$NON-NLS-1$
                            };

                    Intent intent = getIntent();
                    intent.putExtra(LibraryConstants.PREFS_KEY_NOTE, noteArray);
                    setResult(Activity.RESULT_OK, intent);

                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    Utilities.messageDialog(NoteActivity.this, R.string.notenonsaved, null);
                }
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Utilities.toast(NoteActivity.this, R.string.notecancel, Toast.LENGTH_LONG);
                finish();
            }
        });
    }

    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged(newConfig);
        linesNum = linesNum == PORTRAIT_LINES ? LANDSCAPE_LINES : PORTRAIT_LINES;
        noteText.setLines(linesNum);
        noteText.invalidate();
    }
}
