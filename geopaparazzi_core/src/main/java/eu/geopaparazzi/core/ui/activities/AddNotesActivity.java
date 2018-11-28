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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoImages;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.database.objects.Note;
import eu.geopaparazzi.core.ui.dialogs.AddNoteLayoutDialogFragment;
import eu.geopaparazzi.library.camera.CameraNoteActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.dialogs.NoteDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.FormInfoHolder;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.sketch.SketchUtilities;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.TimeUtilities;

/**
 * Map tags adding activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class AddNotesActivity extends AppCompatActivity implements NoteDialogFragment.IAddNote, AddNoteLayoutDialogFragment.IAddNotesLayoutChangeListener {

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
    private Switch togglePositionTypeButtonGps;
    private BroadcastReceiver broadcastReceiver;

    public static String PREFS_KEY_GUITEXTSIZEFACTOR = "PREFS_KEY_GUI_TEXTSIZE_FACTOR"; //$NON-NLS-1$
    public static String PREFS_KEY_RETURNTOVIEWAFTERNOTE = "PREFS_KEY_RETURNTOVIEWAFTERNOTE"; //$NON-NLS-1$
    public static String PREFS_KEY_GUICOLUMNCOUNT = "PREFS_KEY_GUI_COLUMN_COUNT"; //$NON-NLS-1$
    public static int DEFAULT_GUICOLUMNCOUNT = 2;
    public static int DEFAULT_GUITEXTSIZEFACTOR = 1;
    private int textsizeFactor = DEFAULT_GUITEXTSIZEFACTOR;
    private ArrayAdapter<String> arrayAdapter;
    private GridView buttonGridView;
    private CheckBox returnToViewAfterNoteCheckBox;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_addnotes);

        Toolbar toolbar = findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (ProfilesHandler.INSTANCE.getActiveProfile() != null) {
            try {
                View mainAddNotesLayout = findViewById(R.id.mainaddnoteslayout);
                int color = ColorUtilities.toColor(ProfilesHandler.INSTANCE.getActiveProfile().color);
                if (mainAddNotesLayout != null) {
                    mainAddNotesLayout.setBackgroundColor(color);
                }
            } catch (Exception e) {
                // ignore
            }
        }


        togglePositionTypeButtonGps = findViewById(R.id.togglePositionTypeGps);
        togglePositionTypeButtonGps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Editor edit = preferences.edit();
                edit.putBoolean(FormActivity.USE_MAPCENTER_POSITION, !isChecked);
                edit.apply();
            }
        });

        textsizeFactor = preferences.getInt(PREFS_KEY_GUITEXTSIZEFACTOR,
                DEFAULT_GUITEXTSIZEFACTOR);
        setToggleSize();

        returnToViewAfterNoteCheckBox = findViewById(R.id.returnToViewAfterNoteCheckBox);
        boolean returnToViewAfterNote = preferences.getBoolean(PREFS_KEY_RETURNTOVIEWAFTERNOTE, false);
        returnToViewAfterNoteCheckBox.setChecked(returnToViewAfterNote);
        returnToViewAfterNoteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Editor editor = preferences.edit();
                editor.putBoolean(PREFS_KEY_RETURNTOVIEWAFTERNOTE, isChecked);
                editor.apply();
            }
        });

        double[] mapCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
        if (mapCenter != null) {
            mapCenterLatitude = mapCenter[1];
            mapCenterLongitude = mapCenter[0];
        }
        mapCenterElevation = 0.0;

        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                GpsServiceStatus gpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
                if (gpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    gpsLocation = GpsServiceUtilities.getPosition(intent);
                    boolean useMapCenterPosition = preferences.getBoolean(FormActivity.USE_MAPCENTER_POSITION, false);
                    if (useMapCenterPosition) {
                        togglePositionTypeButtonGps.setChecked(false);
                    } else {
                        togglePositionTypeButtonGps.setChecked(true);
                    }
                } else {
                    togglePositionTypeButtonGps.setChecked(false);
                    togglePositionTypeButtonGps.setEnabled(false);
                    Editor edit = preferences.edit();
                    edit.putBoolean(FormActivity.USE_MAPCENTER_POSITION, true);
                    edit.apply();
                }
            }
        };
        GpsServiceUtilities.registerForBroadcasts(this, broadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);

        buttonGridView = findViewById(R.id.osmgridview);
        try {
            Set<String> sectionNames = TagsManager.getInstance(this).getSectionNames();
            tagNamesArray = sectionNames.toArray(new String[sectionNames.size()]);
        } catch (Exception e1) {
            tagNamesArray = new String[]{getString(R.string.maptagsactivity_error_reading_tags)};
            GPLog.error(this, e1.getLocalizedMessage(), e1);
            e1.printStackTrace();
        }

        final int buttonTextColor = Compat.getColor(this, R.color.main_text_color);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tagNamesArray) {
            public View getView(final int position, View cView, ViewGroup parent) {

                Button tagButton = new Button(AddNotesActivity.this);

                Drawable buttonDrawable = Compat.getDrawable(AddNotesActivity.this, R.drawable.button_background_drawable);
                SpannableString spanString = new SpannableString(tagNamesArray[position]);
                spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                // tagButton.setText(tagNamesArray[position]);
                tagButton.setTransformationMethod(null);
                tagButton.setText(spanString);
                tagButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, tagButton.getTextSize() * textsizeFactor);
                tagButton.setTextColor(buttonTextColor);
                try {
                    String description = TagsManager.getInstance(getContext()).getSectionDescriptionByName(tagNamesArray[position]);
                    if (description != null) {
                        Compat.addTooltip(tagButton, description);
                    }
                } catch (Exception e) {
                    GPLog.error(this, "Error retrieving tagsManager.", e);
                }
                tagButton.setBackground(buttonDrawable);
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
                                JSONObject sectionObject = TagsManager.getInstance(AddNotesActivity.this).getSectionByName(sectionName);
                                String sectionObjectString = sectionObject.toString();
                                long noteId = DaoNotes.addNote(longitude, latitude, elevation, new java.util.Date().getTime(), sectionName, "POI", sectionObjectString, null);

                                // launch form activity
                                Intent formIntent = new Intent(AddNotesActivity.this, FormActivity.class);
                                FormInfoHolder formInfoHolder = new FormInfoHolder();
                                formInfoHolder.sectionName = sectionName;
                                formInfoHolder.formName = null;
                                formInfoHolder.noteId = noteId;
                                formInfoHolder.longitude = longitude;
                                formInfoHolder.latitude = latitude;
                                formInfoHolder.elevation = elevation;
                                formInfoHolder.objectExists = false;
                                formIntent.putExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER, formInfoHolder);
                                startActivityForResult(formIntent, FORM_RETURN_CODE);
                            } catch (Exception e) {
                                GPLog.error(this, null, e);
                                GPDialogs.warningDialog(AddNotesActivity.this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
                            }


                        } catch (Exception e) {
                            GPLog.error(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                            Toast.makeText(AddNotesActivity.this, R.string.notenonsaved, Toast.LENGTH_LONG).show();
                        }
                    }
                });

                return tagButton;
            }
        };


        // Set the number of columns for the buttons
        int gridColumnCount = preferences.getInt(PREFS_KEY_GUICOLUMNCOUNT,
                DEFAULT_GUICOLUMNCOUNT);
        buttonGridView.setNumColumns(gridColumnCount);


        // Set the text size for the buttons
        textsizeFactor = preferences.getInt(PREFS_KEY_GUITEXTSIZEFACTOR,
                DEFAULT_GUITEXTSIZEFACTOR);

        buttonGridView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onDestroy() {
        if (broadcastReceiver != null)
            GpsServiceUtilities.unregisterFromBroadcasts(this, broadcastReceiver);
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_addnotes, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_simplenote) {
            checkPositionCoordinates();
            NoteDialogFragment noteDialogFragment = NoteDialogFragment.newInstance(longitude, latitude, elevation);
            noteDialogFragment.show(getSupportFragmentManager(), "Simple Note");
        } else if (item.getItemId() == R.id.action_simplepicture) {
            checkPositionCoordinates();
            Intent intent = new Intent(AddNotesActivity.this, CameraNoteActivity.class);
            intent.putExtra(LibraryConstants.DATABASE_ID, -1l);
            intent.putExtra(LibraryConstants.LONGITUDE, longitude);
            intent.putExtra(LibraryConstants.LATITUDE, latitude);
            intent.putExtra(LibraryConstants.ELEVATION, elevation);

            AddNotesActivity.this.startActivityForResult(intent, CAMERA_RETURN_CODE);
        } else if (item.getItemId() == R.id.action_simplesketch) {
            try {
                checkPositionCoordinates();
                java.util.Date currentDate = new java.util.Date();
                String currentDatestring = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(currentDate);
                File tempFolder = ResourcesManager.getInstance(AddNotesActivity.this).getTempDir();
                File newImageFile = new File(tempFolder, "SKETCH_" + currentDatestring + ".png");

                double[] gpsLocation = new double[]{longitude, latitude, elevation};
                SketchUtilities.launchForResult(AddNotesActivity.this, newImageFile, gpsLocation, SKETCH_RETURN_CODE);
            } catch (Exception e) {
                GPLog.error(AddNotesActivity.this, null, e);
            }
        } else if (item.getItemId() == R.id.action_preferences) {
            AddNoteLayoutDialogFragment dialog = AddNoteLayoutDialogFragment.newInstance();
            dialog.show(getSupportFragmentManager(), "layout");
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPositionCoordinates() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean useMapCenterPosition = preferences.getBoolean(FormActivity.USE_MAPCENTER_POSITION, false);
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
        if (resultCode == Activity.RESULT_CANCELED && requestCode == FORM_RETURN_CODE) {
            long noteId = data.getLongExtra(LibraryConstants.DATABASE_ID, -1);
            if (noteId != -1) {
                // this note needs to be removed, since is was created but then
                // cancel was pressed
                try {
                    Note note = DaoNotes.getNoteById(noteId);
                    DaoNotes.deleteComplexNote(note);
                    return;
                } catch (IOException e) {
                    GPLog.error(this, null, e);
                }
            }
        }
        if (resultCode != Activity.RESULT_OK) {
            // only ok stuff passes here
            return;
        }
        switch (requestCode) {
            case (FORM_RETURN_CODE): {
                FormInfoHolder formInfoHolder = (FormInfoHolder) data.getSerializableExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER);
                if (formInfoHolder != null) {
                    try {
                        long noteId = formInfoHolder.noteId;
                        String nameStr = formInfoHolder.renderingLabel;
                        String jsonStr = formInfoHolder.sectionObjectString;

                        DaoNotes.updateForm(noteId, nameStr, jsonStr);
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                        GPDialogs.warningDialog(this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
                    }
                }
                break;
            }
            case (CAMERA_RETURN_CODE): {
                boolean imageExists = data.getBooleanExtra(LibraryConstants.OBJECT_EXISTS, false);
                if (!imageExists) {
                    GPDialogs.warningDialog(this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
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
                        GPLog.error(this, null, e); //$NON-NLS-1$

                        GPDialogs.warningDialog(this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
                    }
                }
                break;
            }
        }
        boolean returnToViewAfterNote = returnToViewAfterNoteCheckBox.isChecked();
        if (!returnToViewAfterNote) {
            finish();
        }
    }

    @Override
    public void addNote(double lon, double lat, double elev, long timestamp, String note) {
        try {
            DaoNotes.addNote(lon, lat, elev, timestamp, note, "POI", "",
                    null);
            boolean returnToViewAfterNote = returnToViewAfterNoteCheckBox.isChecked();
            if (!returnToViewAfterNote) {
                finish();
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.warningDialog(this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
        }
    }

    @Override
    public void onPropertiesChanged() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int gridColumnCount = preferences.getInt(PREFS_KEY_GUICOLUMNCOUNT,
                DEFAULT_GUICOLUMNCOUNT);
        buttonGridView.setNumColumns(gridColumnCount);
        textsizeFactor = preferences.getInt(PREFS_KEY_GUITEXTSIZEFACTOR,
                DEFAULT_GUITEXTSIZEFACTOR);
        setToggleSize();
        buttonGridView.setAdapter(arrayAdapter);
    }

    private void setToggleSize() {
        TextView textCenterText;
        TextView textGpsText;

        int topMargin = 50 + textsizeFactor * 20;
        int leftMargin = 10 + textsizeFactor * 30;
        int rightMargin = 10 + textsizeFactor * 50;

        textCenterText = findViewById(R.id.centerText);
        textCenterText.setPadding(0, topMargin, rightMargin, topMargin);  // left,top,right,bottom

        textGpsText = findViewById(R.id.gpsText);
        textGpsText.setPadding(leftMargin, topMargin, 0, topMargin);

        togglePositionTypeButtonGps.setScaleX(textsizeFactor);
        togglePositionTypeButtonGps.setScaleY(textsizeFactor);
    }
}
