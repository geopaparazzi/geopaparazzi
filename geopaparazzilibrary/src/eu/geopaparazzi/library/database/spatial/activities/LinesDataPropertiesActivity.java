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
package eu.geopaparazzi.library.database.spatial.activities;

import jsqlite.Exception;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.library.database.spatial.core.SpatialTable;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Line Data properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LinesDataPropertiesActivity extends Activity implements OnItemSelectedListener {
    private SpatialTable spatialTable;
    private Spinner colorSpinner;
    private Spinner widthSpinner;
    private Spinner alphaSpinner;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.data_line_properties);

        Bundle extras = getIntent().getExtras();
        String tableName = extras.getString(LibraryConstants.PREFS_KEY_TEXT);
        try {
            spatialTable = SpatialDatabasesManager.getInstance().getTableByName(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        colorSpinner = (Spinner) findViewById(R.id.color_spinner);
        colorSpinner.setOnItemSelectedListener(this);
        String strokecolor = spatialTable.style.strokecolor;
        int count = colorSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (colorSpinner.getItemAtPosition(i).equals(strokecolor)) {
                colorSpinner.setSelection(i);
                break;
            }
        }
        String width = String.valueOf((int) spatialTable.style.width);
        widthSpinner = (Spinner) findViewById(R.id.width_spinner);
        widthSpinner.setOnItemSelectedListener(this);
        count = widthSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (widthSpinner.getItemAtPosition(i).equals(width)) {
                widthSpinner.setSelection(i);
                break;
            }
        }
        String alpha = String.valueOf((int) (spatialTable.style.strokealpha * 100f));
        alphaSpinner = (Spinner) findViewById(R.id.alpha_spinner);
        alphaSpinner.setOnItemSelectedListener(this);
        count = alphaSpinner.getCount();
        for( int i = 0; i < count; i++ ) {
            if (alphaSpinner.getItemAtPosition(i).equals(alpha)) {
                alphaSpinner.setSelection(i);
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
    public void onItemSelected( AdapterView< ? > arg0, View callingView, int arg2, long arg3 ) {
        if (callingView.equals(colorSpinner)) {
            String color = (String) colorSpinner.getSelectedItem();
            spatialTable.style.strokecolor = color;
        } else if (callingView.equals(widthSpinner)) {
            String widthString = (String) widthSpinner.getSelectedItem();
            float width = Float.parseFloat(widthString);
            spatialTable.style.width = width;
        } else if (callingView.equals(alphaSpinner)) {
            String alphaString = (String) alphaSpinner.getSelectedItem();
            float alpha100 = Float.parseFloat(alphaString);
            spatialTable.style.width = alpha100 / 100f;
        }
    }

    @Override
    public void onNothingSelected( AdapterView< ? > arg0 ) {
    }

}
