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

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.Spinner;

import eu.geopaparazzi.library.core.dialogs.ColorStrokeDialogFragment;
import eu.geopaparazzi.library.style.ColorStrokeObject;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.utilities.Constants;

import static eu.geopaparazzi.core.utilities.Constants.PREFS_KEY_NOTES_CUSTOMCOLOR;
import static eu.geopaparazzi.core.utilities.Constants.PREFS_KEY_NOTES_OPACITY;
import static eu.geopaparazzi.core.utilities.Constants.PREFS_KEY_NOTES_TEXT_VISIBLE;

/**
 * Notes properties activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NotesPropertiesActivity extends AppCompatActivity implements ColorStrokeDialogFragment.IColorStrokePropertiesChangeListener {
    private SharedPreferences mPreferences;
    private ColorStrokeObject notesColorStrokeObject;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_notesproperties);

        Toolbar toolbar = findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(GeopaparazziApplication.getInstance());

        // notes selection
        boolean notesVisible = mPreferences.getBoolean(Constants.PREFS_KEY_NOTES_VISIBLE, true);
        CheckBox notesVisibilityCheckbox = findViewById(R.id.checkVisibility);
        notesVisibilityCheckbox.setChecked(notesVisible);
        notesVisibilityCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_NOTES_VISIBLE, isChecked);
                editor.apply();
            }
        });


        // images selection
        CheckBox imagesView = findViewById(R.id.imagesvisible);

        boolean imagesVisible = mPreferences.getBoolean(Constants.PREFS_KEY_IMAGES_VISIBLE, true);
        imagesView.setChecked(imagesVisible);
        imagesView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_IMAGES_VISIBLE, isChecked);
                editor.apply();
            }
        });

        // use custom
        final CheckBox useCustomCheckbox = findViewById(R.id.checkUseCustom);
        boolean doCustom = mPreferences.getBoolean(Constants.PREFS_KEY_NOTES_CHECK, true);
        useCustomCheckbox.setChecked(doCustom);
        useCustomCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Editor editor = mPreferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_NOTES_CHECK, useCustomCheckbox.isChecked());
                editor.apply();
            }
        });

        int arraySizeId = R.array.array_size;
        int sizespinnerId = R.id.sizeSpinner;
        String prefsKey = Constants.PREFS_KEY_NOTES_SIZE;
        String defaultStr = "" + LibraryConstants.DEFAULT_NOTES_SIZE;
        makeSpinner(arraySizeId, sizespinnerId, prefsKey, defaultStr);

        String opacityStr = mPreferences.getString(PREFS_KEY_NOTES_OPACITY, "255"); //$NON-NLS-1$
        String colorStr = mPreferences.getString(PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.BLUE.getHex()); //$NON-NLS-1$
        int opacity = Integer.parseInt(opacityStr);
        int initColor = ColorUtilities.toColor(colorStr);

        notesColorStrokeObject = new ColorStrokeObject();
        notesColorStrokeObject.hasFill = true;
        notesColorStrokeObject.fillColor = initColor;
        notesColorStrokeObject.fillAlpha = opacity;

        final ImageButton paletteButton = findViewById(R.id.gpslog_palette);
        paletteButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                ColorStrokeDialogFragment colorStrokeDialogFragment = ColorStrokeDialogFragment.newInstance(notesColorStrokeObject);
                colorStrokeDialogFragment.show(getSupportFragmentManager(), "Color Stroke Dialog");
            }
        });

        // show labels
        final CheckBox showLabelsCheckbox = findViewById(R.id.checkShowLabels);
        boolean showLabels = mPreferences.getBoolean(PREFS_KEY_NOTES_TEXT_VISIBLE, true);
        showLabelsCheckbox.setChecked(showLabels);
        showLabelsCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Editor editor = mPreferences.edit();
                editor.putBoolean(PREFS_KEY_NOTES_TEXT_VISIBLE, showLabelsCheckbox.isChecked());
                editor.apply();
            }
        });

        int fontSizeSpinnerId = R.id.fontSizeSpinner;
        prefsKey = Constants.PREFS_KEY_NOTES_TEXT_SIZE;
        defaultStr = "" + LibraryConstants.DEFAULT_NOTES_SIZE;
        makeSpinner(arraySizeId, fontSizeSpinnerId, prefsKey, defaultStr);

        final CheckBox haloCheckbox = findViewById(R.id.checkHalo);
        boolean doHalo = mPreferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_DOHALO, true);
        haloCheckbox.setChecked(doHalo);
        haloCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Editor editor = mPreferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_NOTES_TEXT_DOHALO, haloCheckbox.isChecked());
                editor.apply();
            }
        });
    }

    private void makeSpinner(int arraySizeId, int sizespinnerId, final String prefsKey, String defaultStr) {
        String sizeStr = mPreferences.getString(prefsKey, defaultStr);
        String[] stringArray = getResources().getStringArray(arraySizeId);
        int index = 0;
        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i].equals(sizeStr)) {
                index = i;
                break;
            }
        }
        final Spinner sizeSpinner = findViewById(sizespinnerId);
        ArrayAdapter<?> sizeSpinnerAdapter = ArrayAdapter.createFromResource(this, arraySizeId,
                android.R.layout.simple_spinner_item);
        sizeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeSpinnerAdapter);
        sizeSpinner.setSelection(index);
        sizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Object selectedItem = sizeSpinner.getSelectedItem();
                String sizeStr = selectedItem.toString();
                Editor editor = mPreferences.edit();
                editor.putString(prefsKey, sizeStr);
                editor.apply();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // ignore
            }
        });
    }

    @Override
    public void onPropertiesChanged(ColorStrokeObject newColorStrokeObject) {
        notesColorStrokeObject.fillColor = newColorStrokeObject.fillColor;
        notesColorStrokeObject.fillAlpha = newColorStrokeObject.fillAlpha;
        Editor editor = mPreferences.edit();
        editor.putString(PREFS_KEY_NOTES_OPACITY, newColorStrokeObject.fillAlpha + "");
        editor.putString(PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.getHex(newColorStrokeObject.fillColor));
        editor.apply();
    }

}
