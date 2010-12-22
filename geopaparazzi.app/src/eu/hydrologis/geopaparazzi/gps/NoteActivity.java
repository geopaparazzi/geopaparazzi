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
package eu.hydrologis.geopaparazzi.gps;

import java.io.IOException;
import java.sql.Date;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Notes taking activity.
 * 
 * <p>
 * Note that location and time of the note are taken at the moment 
 * of the saving of the note.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NoteActivity extends Activity {
    private ApplicationManager deviceManager;
    private EditText noteText;
    private int LANDSCAPE_LINES = 5;
    private int PORTRAIT_LINES = 14;
    private int linesNum = PORTRAIT_LINES;
    private boolean coordsFromExtras = false;
    private float latitude;
    private float longitude;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.note);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getFloat(Constants.PREFS_KEY_LAT);
            longitude = extras.getFloat(Constants.PREFS_KEY_LON);
            coordsFromExtras = true;
        }

        deviceManager = ApplicationManager.getInstance(this);

        noteText = (EditText) findViewById(R.id.noteentry);
        noteText.setLines(linesNum);

        Button saveButton = (Button) findViewById(R.id.ok);
        saveButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                GpsLocation loc = deviceManager.getLoc();
                try {

                    double altitude;
                    Date sqlDate;
                    if (!coordsFromExtras) {
                        if (loc == null) {
                            longitude = 0f;
                            latitude = 0f;
                            altitude = 0f;
                            sqlDate = new Date(System.currentTimeMillis());
                        } else {
                            longitude = (float) loc.getLongitude();
                            latitude = (float) loc.getLatitude();
                            altitude = loc.getAltitude();
                            sqlDate = loc.getSqlDate();
                        }
                    } else {
                        altitude = 0.0;
                        sqlDate = new Date(System.currentTimeMillis());
                    }

                    StringBuilder sB = new StringBuilder(noteText.getText());
                    String noteString = sB.toString();

                    DaoNotes.addNote(NoteActivity.this, longitude, latitude, altitude, sqlDate, noteString, null);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(NoteActivity.this, R.string.notenonsaved, Toast.LENGTH_LONG).show();
                }
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Toast.makeText(NoteActivity.this, R.string.notecancel, Toast.LENGTH_LONG).show();
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
