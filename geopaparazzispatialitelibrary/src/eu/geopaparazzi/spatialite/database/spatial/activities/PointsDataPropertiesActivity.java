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
 * Points Data properties activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointsDataPropertiesActivity extends Activity {
    private SpatialVectorTable spatialTable;
    private Spinner shapesSpinner;
    private Spinner sizeSpinner;
    private Spinner colorSpinner;
    private Spinner widthSpinner;
    private Spinner alphaSpinner;
    private Spinner fillColorSpinner;
    private Spinner fillAlphaSpinner;
    private Spinner minZoomSpinner;
    private Spinner maxZoomSpinner;
    private EditText dashPatternText;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.data_point_properties);

        Bundle extras = getIntent().getExtras();
        String tableName = extras.getString(SpatialiteLibraryConstants.PREFS_KEY_TEXT);
        try {
            spatialTable = SpatialDatabasesManager.getInstance().getVectorTableByName(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        shapesSpinner = (Spinner) findViewById(R.id.shape_spinner);
        String shape = spatialTable.getStyle().shape;
        int count = shapesSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (shapesSpinner.getItemAtPosition(i).equals(shape)) {
                shapesSpinner.setSelection(i);
                break;
            }
        }
        String size = String.valueOf((int) spatialTable.getStyle().size);
        sizeSpinner = (Spinner) findViewById(R.id.size_spinner);
        count = sizeSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (sizeSpinner.getItemAtPosition(i).equals(size)) {
                sizeSpinner.setSelection(i);
                break;
            }
        }

        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        String strokecolor = spatialTable.getStyle().strokecolor;
        count = colorSpinner.getCount();
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

        fillColorSpinner = (Spinner) findViewById(R.id.fill_color_spinner);
        String fillcolor = spatialTable.getStyle().fillcolor;
        count = fillColorSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (fillColorSpinner.getItemAtPosition(i).equals(fillcolor)) {
                fillColorSpinner.setSelection(i);
                break;
            }
        }

        String fillAlpha = String.valueOf((int) (spatialTable.getStyle().fillalpha * 100f));
        fillAlphaSpinner = (Spinner) findViewById(R.id.fill_alpha_spinner);
        count = fillAlphaSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (fillAlphaSpinner.getItemAtPosition(i).equals(fillAlpha)) {
                fillAlphaSpinner.setSelection(i);
                break;
            }
        }

        int minZoom = spatialTable.getMinZoom();
        int tableMinZoom = 0; // spatialTable.getMinZoom();
        int tableMaxZoom = 22; //spatialTable.getMaxZoom();
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

        String sizeString = (String) sizeSpinner.getSelectedItem();
        float size = Float.parseFloat(sizeString);
        spatialTable.getStyle().size = size;

        String widthString = (String) widthSpinner.getSelectedItem();
        float width = Float.parseFloat(widthString);
        spatialTable.getStyle().width = width;

        String alphaString = (String) alphaSpinner.getSelectedItem();
        float alpha100 = Float.parseFloat(alphaString);
        spatialTable.getStyle().strokealpha = alpha100 / 100f;

        String fillColor = (String) fillColorSpinner.getSelectedItem();
        spatialTable.getStyle().fillcolor = fillColor;

        String fillAlphaString = (String) fillAlphaSpinner.getSelectedItem();
        float fillAlpha100 = Float.parseFloat(fillAlphaString);
        spatialTable.getStyle().fillalpha = fillAlpha100 / 100f;

        String shapeColor = (String) shapesSpinner.getSelectedItem();
        spatialTable.getStyle().shape = shapeColor;

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
