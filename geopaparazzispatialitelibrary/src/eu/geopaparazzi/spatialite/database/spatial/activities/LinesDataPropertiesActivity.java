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

import jsqlite.Exception;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.util.SpatialiteLibraryConstants;

/**
 * Line Data properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LinesDataPropertiesActivity extends Activity {
    private SpatialVectorTable spatialTable;
    private Spinner colorSpinner;
    private Spinner widthSpinner;
    private Spinner alphaSpinner;
    private EditText decimationText;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.data_line_properties);

        Bundle extras = getIntent().getExtras();
        String tableName = extras.getString(SpatialiteLibraryConstants.PREFS_KEY_TEXT);
        try {
            spatialTable = SpatialDatabasesManager.getInstance().getVectorTableByName(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        String strokecolor = spatialTable.getStyle().strokecolor;
        int count = colorSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (colorSpinner.getItemAtPosition(i).equals(strokecolor)) {
                colorSpinner.setSelection(i);
                break;
            }
        }
        String width = String.valueOf((int) spatialTable.getStyle().width);
        widthSpinner = (Spinner) findViewById(R.id.width_spinner);
        count = widthSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (widthSpinner.getItemAtPosition(i).equals(width)) {
                widthSpinner.setSelection(i);
                break;
            }
        }
        String alpha = String.valueOf((int) (spatialTable.getStyle().strokealpha * 100f));
        alphaSpinner = (Spinner) findViewById(R.id.alpha_spinner);
        count = alphaSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (alphaSpinner.getItemAtPosition(i).equals(alpha)) {
                alphaSpinner.setSelection(i);
                break;
            }
        }

        String decimation = String.valueOf(spatialTable.getStyle().decimationFactor);
        decimationText = (EditText) findViewById(R.id.decimation_text);
        decimationText.setText(decimation);
    }

    public void onOkClick( View view ) {
        String color = (String) colorSpinner.getSelectedItem();
        spatialTable.getStyle().strokecolor = color;

        String widthString = (String) widthSpinner.getSelectedItem();
        float width = 1f;
        try {
            width = Float.parseFloat(widthString);
        } catch (java.lang.Exception e) {
        }
        spatialTable.getStyle().width = width;

        String alphaString = (String) alphaSpinner.getSelectedItem();
        float alpha100 = Float.parseFloat(alphaString);
        spatialTable.getStyle().strokealpha = alpha100 / 100f;

        String decimationString = decimationText.getText().toString();
        float decimation = 0.0f;
        try {
            decimation = Float.parseFloat(decimationString);
        } catch (java.lang.Exception e) {
        }
        spatialTable.getStyle().decimationFactor = decimation;

        try {
            SpatialDatabasesManager.getInstance().updateStyle(spatialTable);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onCancelClick( View view ) {
        finish();
    }

}
