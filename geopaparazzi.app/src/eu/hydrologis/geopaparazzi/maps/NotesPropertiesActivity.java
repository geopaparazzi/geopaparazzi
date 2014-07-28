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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Notes properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NotesPropertiesActivity extends Activity {
    private SharedPreferences preferences;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.notes_properties);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // notes selection
        CheckBox notesVisibilityCheckbox = (CheckBox) findViewById(R.id.checkVisibility);
        notesVisibilityCheckbox.setChecked(DataManager.getInstance().areNotesVisible());
        notesVisibilityCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                DataManager.getInstance().setNotesVisible(isChecked);
            }
        });

        // use custom
        final CheckBox useCustomCheckbox = (CheckBox) findViewById(R.id.checkUseCustom);
        boolean doCustom = preferences.getBoolean(Constants.PREFS_KEY_NOTES_CHECK, false);
        useCustomCheckbox.setChecked(doCustom);
        useCustomCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_NOTES_CHECK, useCustomCheckbox.isChecked());
                editor.commit();
            }
        });

        int arraySizeId = R.array.array_size;
        int sizespinnerId = R.id.sizeSpinner;
        String prefsKey = Constants.PREFS_KEY_NOTES_SIZE;
        String defaultStr = "15";
        makeSpinner(arraySizeId, sizespinnerId, prefsKey, defaultStr);

        int arrayColorId = R.array.array_colornames;
        int colorSpinnerId = R.id.colorSpinner;
        prefsKey = Constants.PREFS_KEY_NOTES_CUSTOMCOLOR;
        defaultStr = "blue";
        makeSpinner(arrayColorId, colorSpinnerId, prefsKey, defaultStr);

        int arrayOpacityId = R.array.array_alpha;
        int opacitySpinnerId = R.id.alphaSpinner;
        prefsKey = Constants.PREFS_KEY_NOTES_OPACITY;
        defaultStr = "100";
        makeSpinner(arrayOpacityId, opacitySpinnerId, prefsKey, defaultStr);

        // show labels
        final CheckBox showLabelsCheckbox = (CheckBox) findViewById(R.id.checkShowLabels);
        boolean showLabels = preferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_VISIBLE, false);
        showLabelsCheckbox.setChecked(showLabels);
        showLabelsCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_NOTES_TEXT_VISIBLE, showLabelsCheckbox.isChecked());
                editor.commit();
            }
        });

        int fontSizeSpinnerId = R.id.fontSizeSpinner;
        prefsKey = Constants.PREFS_KEY_NOTES_TEXT_SIZE;
        defaultStr = "30";
        makeSpinner(arraySizeId, fontSizeSpinnerId, prefsKey, defaultStr);

        final CheckBox haloCheckbox = (CheckBox) findViewById(R.id.checkHalo);
        boolean doHalo = preferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_DOHALO, true);
        haloCheckbox.setChecked(doHalo);
        haloCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_KEY_NOTES_TEXT_DOHALO, haloCheckbox.isChecked());
                editor.commit();
            }
        });
    }

    private void makeSpinner( int arraySizeId, int sizespinnerId, final String prefsKey, String defaultStr ) {
        String sizeStr = preferences.getString(prefsKey, defaultStr);
        String[] stringArray = getResources().getStringArray(arraySizeId);
        int index = 0;
        for( int i = 0; i < stringArray.length; i++ ) {
            if (stringArray[i].equals(sizeStr)) {
                index = i;
                break;
            }
        }
        final Spinner sizeSpinner = (Spinner) findViewById(sizespinnerId);
        ArrayAdapter< ? > sizeSpinnerAdapter = ArrayAdapter.createFromResource(this, arraySizeId,
                android.R.layout.simple_spinner_item);
        sizeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeSpinnerAdapter);
        sizeSpinner.setSelection(index);
        sizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                Object selectedItem = sizeSpinner.getSelectedItem();
                String sizeStr = selectedItem.toString();
                Editor editor = preferences.edit();
                editor.putString(prefsKey, sizeStr);
                editor.commit();
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
                // ignore
            }
        });
    }

}
