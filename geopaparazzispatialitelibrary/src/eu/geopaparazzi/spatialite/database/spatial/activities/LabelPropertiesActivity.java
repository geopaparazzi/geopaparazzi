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
package eu.geopaparazzi.spatialite.database.spatial.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

import java.util.List;

import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;
import jsqlite.Exception;

/**
 * Notes properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LabelPropertiesActivity extends Activity {
    private SpatialVectorTable spatialTable;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.vector_label_properties);

        Bundle extras = getIntent().getExtras();
        String tableName = extras.getString(SpatialiteLibraryConstants.PREFS_KEY_TEXT);
        try {
            spatialTable = SpatialDatabasesManager.getInstance().getVectorTableByName(tableName);
        } catch (Exception e) {
            e.printStackTrace();
            Utilities.errorDialog(this, e, null);
            return;
        }

        // notes selection
        CheckBox notesVisibilityCheckbox = (CheckBox) findViewById(R.id.checkVisibility);

        notesVisibilityCheckbox.setChecked(spatialTable.getStyle().labelvisible == 1);
        notesVisibilityCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                spatialTable.getStyle().labelvisible = isChecked ? 1 : 0;
            }
        });

        makeSizeSpinner();

        makeFieldsSpinner();
    }

    @Override
    public void finish() {
        try {
            SpatialDatabasesManager.getInstance().updateStyle(spatialTable);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.finish();
    }

    private void makeSizeSpinner() {
        String labelsizeStr = "" + (int) spatialTable.getStyle().labelsize; //$NON-NLS-1$

        int arraySizeId = R.array.array_sizes;
        String[] stringArray = getResources().getStringArray(arraySizeId);
        int index = 0;
        for( int i = 0; i < stringArray.length; i++ ) {
            if (stringArray[i].equals(labelsizeStr)) {
                index = i;
                break;
            }
        }
        final Spinner sizeSpinner = (Spinner) findViewById(R.id.fontSizeSpinner);
        ArrayAdapter< ? > sizeSpinnerAdapter = ArrayAdapter.createFromResource(this, arraySizeId,
                android.R.layout.simple_spinner_item);
        sizeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeSpinnerAdapter);
        sizeSpinner.setSelection(index);
        sizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                Object selectedItem = sizeSpinner.getSelectedItem();
                String sizeStr = selectedItem.toString();
                float size = Float.parseFloat(sizeStr);
                spatialTable.getStyle().labelsize = size;
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
                // ignore
            }
        });
    }

    private void makeFieldsSpinner() {
        List<String> labelFieldsList = spatialTable.getTableFieldNamesList();
        String labelField = spatialTable.getStyle().labelfield;

        int index = 0;
        for( int i = 0; i < labelFieldsList.size(); i++ ) {
            if (labelFieldsList.get(i).equals(labelField)) {
                index = i;
                break;
            }
        }
        final Spinner fieldsSpinner = (Spinner) findViewById(R.id.labelFieldSpinner);
        ArrayAdapter<String> fieldsSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                labelFieldsList);
        fieldsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fieldsSpinner.setAdapter(fieldsSpinnerAdapter);
        fieldsSpinner.setSelection(index);
        fieldsSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                Object selectedItem = fieldsSpinner.getSelectedItem();
                String fieldStr = selectedItem.toString();
                spatialTable.getStyle().labelfield = fieldStr;
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
                // ignore
            }
        });
    }

}
