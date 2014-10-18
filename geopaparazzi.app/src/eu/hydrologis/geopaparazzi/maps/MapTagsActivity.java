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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONObject;

import java.io.File;
import java.sql.Date;
import java.util.Set;

import eu.geopaparazzi.library.camera.CameraActivity;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.markers.MarkersUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.NoteActivity;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;

/**
 * Map tags adding activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class MapTagsActivity extends Activity {
    private static final String USE_MAPCENTER_POSITION = "USE_MAPCENTER_POSITION";
    private static final int NOTE_RETURN_CODE = 666;
    private static final int CAMERA_RETURN_CODE = 667;
    private static final int FORM_RETURN_CODE = 669;
    private static final int SKETCH_RETURN_CODE = 670;
    private double latitude;
    private double longitude;
    private double elevation;
    private double mapCenterLatitude;
    private double mapCenterLongitude;
    private double mapCenterElevation;
    private String[] tagNamesArray;
    private double[] gpsLocation;
    private ToggleButton togglePositionTypeButtonGps;
    private BroadcastReceiver broadcastReceiver;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.tags);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        togglePositionTypeButtonGps = (ToggleButton) findViewById(R.id.togglePositionTypeGps);
        togglePositionTypeButtonGps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Editor edit = preferences.edit();
                edit.putBoolean(USE_MAPCENTER_POSITION, !isChecked);
                edit.commit();
            }
        });

        double[] mapCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
        mapCenterLatitude = mapCenter[1];
        mapCenterLongitude = mapCenter[0];
        mapCenterElevation = 0.0;

        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                GpsServiceStatus gpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
                if (gpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    gpsLocation = GpsServiceUtilities.getPosition(intent);
                    boolean useMapCenterPosition = preferences.getBoolean(USE_MAPCENTER_POSITION, false);
                    if (useMapCenterPosition) {
                        togglePositionTypeButtonGps.setChecked(false);
                    } else {
                        togglePositionTypeButtonGps.setChecked(true);
                    }
                } else {
                    togglePositionTypeButtonGps.setChecked(false);
                    togglePositionTypeButtonGps.setEnabled(false);
                    Editor edit = preferences.edit();
                    edit.putBoolean(USE_MAPCENTER_POSITION, true);
                    edit.commit();
                }
            }
        };
        GpsServiceUtilities.registerForBroadcasts(this, broadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);

        ImageButton imageButton = (ImageButton) findViewById(R.id.imagefromtag);
        imageButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                checkPositionCoordinates();
                Intent intent = new Intent(MapTagsActivity.this, CameraActivity.class);
                intent.putExtra(LibraryConstants.DATABASE_ID, -1l);
                intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                intent.putExtra(LibraryConstants.LATITUDE, latitude);
                intent.putExtra(LibraryConstants.ELEVATION, elevation);

                MapTagsActivity.this.startActivityForResult(intent, CAMERA_RETURN_CODE);
            }
        });
        ImageButton noteButton = (ImageButton) findViewById(R.id.notefromtag);
        noteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                checkPositionCoordinates();
                Intent intent = new Intent(MapTagsActivity.this, NoteActivity.class);
                intent.putExtra(LibraryConstants.LONGITUDE, longitude);
                intent.putExtra(LibraryConstants.LATITUDE, latitude);
                intent.putExtra(LibraryConstants.ELEVATION, elevation);
                MapTagsActivity.this.startActivityForResult(intent, NOTE_RETURN_CODE);
            }
        });
        ImageButton sketchButton = (ImageButton) findViewById(R.id.sketchfromtag);
        sketchButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                try {
                    checkPositionCoordinates();
                    java.util.Date currentDate = new java.util.Date();
                    String currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(currentDate);
                    File tempFolder = ResourcesManager.getInstance(MapTagsActivity.this).getTempDir();
                    File newImageFile = new File(tempFolder, "SKETCH_" + currentDatestring + ".png");

                    double[] gpsLocation = new double[]{longitude, latitude, elevation};
                    MarkersUtilities.launchForResult(MapTagsActivity.this, newImageFile, gpsLocation, SKETCH_RETURN_CODE);
                } catch (Exception e) {
                    GPLog.error(MapTagsActivity.this, null, e);
                }
            }
        });

        GridView buttonGridView = (GridView) findViewById(R.id.osmgridview);
        try {
            Set<String> sectionNames = TagsManager.getInstance(this).getSectionNames();
            tagNamesArray = sectionNames.toArray(new String[sectionNames.size()]);
        } catch (Exception e1) {
            tagNamesArray = new String[]{getString(R.string.maptagsactivity_error_reading_tags)};
            GPLog.error(this, e1.getLocalizedMessage(), e1);
            e1.printStackTrace();
        }

        final int buttonTextColor = getResources().getColor(R.color.main_text_color);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, tagNamesArray) {
            public View getView(final int position, View cView, ViewGroup parent) {

                Button tagButton = new Button(MapTagsActivity.this);

                Drawable buttonDrawable = getResources().getDrawable(R.drawable.button_background_drawable);
                SpannableString spanString = new SpannableString(tagNamesArray[position]);
                spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                // tagButton.setText(tagNamesArray[position]);
                tagButton.setText(spanString);
                tagButton.setTextColor(buttonTextColor);
                tagButton.setBackgroundDrawable(buttonDrawable);
                int ind = 35;
                tagButton.setPadding(0, ind, 0, ind);
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) parent.getLayoutParams();
                mlp.setMargins(0, ind, 0, ind);

                // osmButton.setImageResource(R.drawable.gps);
                tagButton.setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            String sectionName = tagNamesArray[position];

                            checkPositionCoordinates();

                            // insert note and then work on it
                            try {
                                JSONObject sectionObject = TagsManager.getInstance(MapTagsActivity.this).getSectionByName(sectionName);
                                String sectionObjectString = sectionObject.toString();
                                long noteId = DaoNotes.addNote(longitude, latitude, elevation, new java.util.Date().getTime(), sectionName, "POI", sectionObjectString, null);

                                // launch form activity
                                Intent formIntent = new Intent(MapTagsActivity.this, FormActivity.class);
                                formIntent.putExtra(LibraryConstants.DATABASE_ID, noteId);
                                formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_NAME, sectionName);
                                formIntent.putExtra(LibraryConstants.LATITUDE, latitude);
                                formIntent.putExtra(LibraryConstants.LONGITUDE, longitude);
                                formIntent.putExtra(LibraryConstants.ELEVATION, elevation);
                                startActivityForResult(formIntent, FORM_RETURN_CODE);
                            } catch (Exception e) {
                                GPLog.error(this, null, e);
                                Utilities.messageDialog(MapTagsActivity.this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                            }


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

    @Override
    protected void onDestroy() {
        if (broadcastReceiver != null)
            GpsServiceUtilities.unregisterFromBroadcasts(this, broadcastReceiver);
        super.onDestroy();
    }

    private void checkPositionCoordinates() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useMapCenterPosition = preferences.getBoolean(USE_MAPCENTER_POSITION, false);
        if (useMapCenterPosition || gpsLocation == null) {
            latitude = mapCenterLatitude;
            longitude = mapCenterLongitude;
            elevation = mapCenterElevation;
        } else {
            latitude = gpsLocation[1];
            longitude = gpsLocation[0];
            elevation = gpsLocation[2];
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case (FORM_RETURN_CODE): {
                String[] formArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_FORM);
                if (formArray != null) {
                    try {
                        long noteId = Long.parseLong(formArray[0]);
                        //                        double lon = Double.parseDouble(formArray[1]);
                        //                        double lat = Double.parseDouble(formArray[2]);
                        //                        double elev = Double.parseDouble(formArray[3]);
                        //                        String dateStr = formArray[4];
                        String nameStr = formArray[5];
                        //                        String catStr = formArray[6];
                        String jsonStr = formArray[7];

                        DaoNotes.updateForm(noteId, nameStr, jsonStr);
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
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
                        DaoNotes.addNote(lon, lat, elev, Long.parseLong(noteArray[3]), noteArray[4], noteArray[5], noteArray[6],
                                null);
                    } catch (Exception e) {
                        e.printStackTrace();

                        Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                    }
                }
                break;
            }
            case (CAMERA_RETURN_CODE): {
                boolean imageExists = data.getBooleanExtra(LibraryConstants.OBJECT_EXISTS, false);
                if (!imageExists) {
                    Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
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

                        byte[][] imageAndThumbnailArray = ImageUtilities.getImageAndThumbnailFromPath(absoluteImagePath, 10);

                        java.util.Date currentDate = new java.util.Date();
                        String name = ImageUtilities.getSketchImageName(currentDate);
                        new DaoImages().addImage(lon, lat, elev, -9999.0, currentDate.getTime(), name, imageAndThumbnailArray[0], imageAndThumbnailArray[1], -1);

                        // delete the file after insertion in db
                        imgFile.delete();
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
