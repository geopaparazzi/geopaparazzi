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
package eu.hydrologis.geopaparazzi.maps;

import java.io.File;
import java.sql.Date;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;
import eu.geopaparazzi.library.camera.CameraActivity;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.sketch.DrawingActivity;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.NoteActivity;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;

/**
 * Osm tags adding activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapTagsActivity extends Activity {
    private static final int NOTE_RETURN_CODE = 666;
    private static final int CAMERA_RETURN_CODE = 667;
    private static final int FORM_RETURN_CODE = 669;
    private static final int SKETCH_RETURN_CODE = 670;
    private double latitude;
    private double longitude;
    private double elevation;
    private String[] tagNamesArray;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.tags);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            latitude = extras.getDouble(LibraryConstants.LATITUDE);
            longitude = extras.getDouble(LibraryConstants.LONGITUDE);
            elevation = extras.getDouble(LibraryConstants.ELEVATION);
        }

        ImageButton imageButton = (ImageButton) findViewById(R.id.imagefromtag);
        imageButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(MapTagsActivity.this, CameraActivity.class);
                intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                intent.putExtra(LibraryConstants.LATITUDE, latitude);
                intent.putExtra(LibraryConstants.ELEVATION, elevation);

                MapTagsActivity.this.startActivityForResult(intent, CAMERA_RETURN_CODE);
            }
        });
        ImageButton noteButton = (ImageButton) findViewById(R.id.notefromtag);
        noteButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(MapTagsActivity.this, NoteActivity.class);
                intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                intent.putExtra(LibraryConstants.LATITUDE, latitude);
                intent.putExtra(LibraryConstants.ELEVATION, elevation);
                MapTagsActivity.this.startActivityForResult(intent, NOTE_RETURN_CODE);
            }
        });
        ImageButton sketchButton = (ImageButton) findViewById(R.id.sketchfromtag);
        sketchButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(MapTagsActivity.this, DrawingActivity.class);
                intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                intent.putExtra(LibraryConstants.LATITUDE, latitude);
                intent.putExtra(LibraryConstants.ELEVATION, elevation);
                MapTagsActivity.this.startActivityForResult(intent, SKETCH_RETURN_CODE);
            }
        });

        GridView buttonGridView = (GridView) findViewById(R.id.osmgridview);
        try {
            Set<String> sectionNames = TagsManager.getInstance(this).getSectionNames();
            tagNamesArray = sectionNames.toArray(new String[0]);
        } catch (Exception e1) {
            tagNamesArray = new String[]{getString(R.string.maptagsactivity_error_reading_tags)};
            GPLog.error(this, e1.getLocalizedMessage(), e1);
            e1.printStackTrace();
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, tagNamesArray){
            public View getView( final int position, View cView, ViewGroup parent ) {

                Button tagButton = new Button(MapTagsActivity.this);
                tagButton.setText(tagNamesArray[position]);
                // osmButton.setImageResource(R.drawable.gps);
                tagButton.setOnClickListener(new Button.OnClickListener(){
                    public void onClick( View v ) {
                        try {
                            String userDefinedButtonName = tagNamesArray[position];

                            // launch form activity
                            Intent formIntent = new Intent(MapTagsActivity.this, FormActivity.class);
                            formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_NAME, userDefinedButtonName);
                            formIntent.putExtra(LibraryConstants.LATITUDE, latitude);
                            formIntent.putExtra(LibraryConstants.LONGITUDE, longitude);
                            formIntent.putExtra(LibraryConstants.ELEVATION, elevation);
                            startActivityForResult(formIntent, FORM_RETURN_CODE);
                        } catch (Exception e) {
                            GPLog.error(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                            Toast.makeText(MapTagsActivity.this, R.string.notenonsaved, Toast.LENGTH_LONG).show();
                        }
                    }
                });

                return tagButton;
            }
        };

        // setListAdapter(arrayAdapter);
        buttonGridView.setAdapter(arrayAdapter);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch( requestCode ) {
        case (FORM_RETURN_CODE): {
            String[] formArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_FORM);
            if (formArray != null) {
                try {
                    double lon = Double.parseDouble(formArray[0]);
                    double lat = Double.parseDouble(formArray[1]);
                    double elev = Double.parseDouble(formArray[2]);
                    String dateStr = formArray[3];
                    String nameStr = formArray[4];
                    String catStr = formArray[5];
                    String jsonStr = formArray[6];
                    java.util.Date date = LibraryConstants.TIME_FORMATTER_SQLITE.parse(dateStr);
                    DaoNotes.addNote(lon, lat, elev, new Date(date.getTime()), nameStr, catStr, jsonStr, NoteType.POI.getTypeNum());
                } catch (Exception e) {
                    e.printStackTrace();
                    Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                }
            }
            break;
        }
        case (NOTE_RETURN_CODE): {
            String[] noteArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_NOTE);
            if (noteArray != null) {
                try {
                    double lon = Double.parseDouble(noteArray[0]);
                    double lat = Double.parseDouble(noteArray[1]);
                    double elev = Double.parseDouble(noteArray[2]);
                    java.util.Date date = LibraryConstants.TIME_FORMATTER.parse(noteArray[3]);
                    DaoNotes.addNote(lon, lat, elev, new Date(date.getTime()), noteArray[4], noteArray[5], noteArray[6], NoteType.POI.getTypeNum());
                } catch (Exception e) {
                    e.printStackTrace();

                    Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                }
            }
            break;
        }
        case (CAMERA_RETURN_CODE): {
            String relativeImagePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
            if (relativeImagePath != null) {
                try {
                    File imgFile = new File(ResourcesManager.getInstance(this).getMediaDir().getParentFile(), relativeImagePath);
                    if (!imgFile.exists()) {
                        return;
                    }
                    double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
                    double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
                    double elev = data.getDoubleExtra(LibraryConstants.ELEVATION, 0.0);
                    double azim = data.getDoubleExtra(LibraryConstants.AZIMUTH, 0.0);

                    DaoImages.addImage(lon, lat, elev, azim, new Date(new java.util.Date().getTime()), "", relativeImagePath);
                } catch (Exception e) {
                    e.printStackTrace();

                    Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                }
            }
            break;
        }
        case (SKETCH_RETURN_CODE): {
            String absoluteImagePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
            if (absoluteImagePath != null) {
                File imgFile = new File(absoluteImagePath);
                if (!imgFile.exists()) {
                    return;
                }
                try {
                    double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
                    double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
                    double elev = data.getDoubleExtra(LibraryConstants.ELEVATION, 0.0);

                    DaoImages.addImage(lon, lat, elev, -9999.0, new Date(new java.util.Date().getTime()), "", absoluteImagePath);
                } catch (Exception e) {
                    e.printStackTrace();

                    Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                }
            }
            break;
        }
        }
        finish();
    }

}
