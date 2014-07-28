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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;

import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;
import jsqlite.Exception;

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
    private Spinner minZoomSpinner;
    private Spinner maxZoomSpinner;
    private EditText dashPatternText;

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

        int minZoom = spatialTable.getMinZoom();
        int tableMinZoom = 0; // spatialTable.getMinZoom();
        int tableMaxZoom = 22; // spatialTable.getMaxZoom();
        ArrayList<String> minMaxSequence = new ArrayList<String>();
        for( int i = tableMinZoom; i <= tableMaxZoom; i++ ) {
            minMaxSequence.add(String.valueOf(i));
        }
        minZoomSpinner = (Spinner) findViewById(R.id.minzoom_spinner);
        ArrayAdapter<String> queryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, minMaxSequence);
        queryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minZoomSpinner.setAdapter(queryAdapter);
        count = minZoomSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (minZoomSpinner.getItemAtPosition(i).equals(String.valueOf(minZoom))) {
                minZoomSpinner.setSelection(i);
                break;
            }
        }

        int maxZoom = spatialTable.getMaxZoom();
        maxZoomSpinner = (Spinner) findViewById(R.id.maxzoom_spinner);
        queryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        maxZoomSpinner.setAdapter(queryAdapter);
        count = maxZoomSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (maxZoomSpinner.getItemAtPosition(i).equals(String.valueOf(maxZoom))) {
                maxZoomSpinner.setSelection(i);
                break;
            }
        }

        String dashPattern = spatialTable.getStyle().dashPattern;
        dashPatternText = (EditText) findViewById(R.id.dashpattern_text);
        dashPatternText.setText(dashPattern);

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

        String minZoom = (String) minZoomSpinner.getSelectedItem();
        spatialTable.getStyle().minZoom = Integer.parseInt(minZoom);

        String maxZoom = (String) maxZoomSpinner.getSelectedItem();
        spatialTable.getStyle().maxZoom = Integer.parseInt(maxZoom);

        String dashPatternString = dashPatternText.getText().toString();
        spatialTable.getStyle().dashPattern = dashPatternString;

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
