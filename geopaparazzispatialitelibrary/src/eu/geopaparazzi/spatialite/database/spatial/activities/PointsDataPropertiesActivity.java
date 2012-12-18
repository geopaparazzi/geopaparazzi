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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.util.SpatialiteLibraryConstants;

/**
 * Points Data properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointsDataPropertiesActivity extends Activity implements OnItemSelectedListener {
    private SpatialVectorTable spatialTable;
    private Spinner shapesSpinner;
    private Spinner sizeSpinner;
    private Spinner colorSpinner;
    private Spinner widthSpinner;
    private Spinner alphaSpinner;
    private Spinner fillColorSpinner;
    private Spinner fillAlphaSpinner;

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
        shapesSpinner.setOnItemSelectedListener(this);
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
        sizeSpinner.setOnItemSelectedListener(this);
        count = sizeSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (sizeSpinner.getItemAtPosition(i).equals(size)) {
                sizeSpinner.setSelection(i);
                break;
            }
        }
        
        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        colorSpinner.setOnItemSelectedListener(this);
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
        widthSpinner.setOnItemSelectedListener(this);
        count = widthSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (widthSpinner.getItemAtPosition(i).equals(width)) {
                widthSpinner.setSelection(i);
                break;
            }
        }
        String alpha = String.valueOf((int) (spatialTable.getStyle().strokealpha * 100f));
        alphaSpinner = (Spinner) findViewById(R.id.alpha_spinner);
        alphaSpinner.setOnItemSelectedListener(this);
        count = alphaSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (alphaSpinner.getItemAtPosition(i).equals(alpha)) {
                alphaSpinner.setSelection(i);
                break;
            }
        }

        fillColorSpinner = (Spinner) findViewById(R.id.fill_color_spinner);
        fillColorSpinner.setOnItemSelectedListener(this);
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
        fillAlphaSpinner.setOnItemSelectedListener(this);
        count = fillAlphaSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (fillAlphaSpinner.getItemAtPosition(i).equals(fillAlpha)) {
                fillAlphaSpinner.setSelection(i);
                break;
            }
        }

    }

    public void onOkClick( View view ) {
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

    @Override
    public void onItemSelected( AdapterView< ? > callingView, View view, int arg2, long arg3 ) {
        if (callingView.equals(colorSpinner)) {
            String color = (String) colorSpinner.getSelectedItem();
            spatialTable.getStyle().strokecolor = color;
        } else if (callingView.equals(sizeSpinner)) {
            String sizeString = (String) sizeSpinner.getSelectedItem();
            float size = Float.parseFloat(sizeString);
            spatialTable.getStyle().size = size;
        } else if (callingView.equals(widthSpinner)) {
            String widthString = (String) widthSpinner.getSelectedItem();
            float width = Float.parseFloat(widthString);
            spatialTable.getStyle().width = width;
        } else if (callingView.equals(alphaSpinner)) {
            String alphaString = (String) alphaSpinner.getSelectedItem();
            float alpha100 = Float.parseFloat(alphaString);
            spatialTable.getStyle().strokealpha = alpha100 / 100f;
        } else if (callingView.equals(fillColorSpinner)) {
            String color = (String) fillColorSpinner.getSelectedItem();
            spatialTable.getStyle().fillcolor = color;
        } else if (callingView.equals(fillAlphaSpinner)) {
            String alphaString = (String) fillAlphaSpinner.getSelectedItem();
            float alpha100 = Float.parseFloat(alphaString);
            spatialTable.getStyle().fillalpha = alpha100 / 100f;
        } else if (callingView.equals(shapesSpinner)) {
            String color = (String) shapesSpinner.getSelectedItem();
            spatialTable.getStyle().shape = color;
        }
    }

    @Override
    public void onNothingSelected( AdapterView< ? > arg0 ) {
    }

}
