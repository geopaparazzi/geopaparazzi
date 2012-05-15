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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.chart.ChartActivity;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Data properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsDataPropertiesActivity extends Activity {
    private static List<String> colorList;
    private static List<String> widthsList;
    private MapItem item;

    // properties
    private String newText;
    private float newWidth;
    private String newColor;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.gpslog_properties);
        getResourcesAndColors();

        Bundle extras = getIntent().getExtras();
        Object object = extras.get(Constants.PREFS_KEY_GPSLOG4PROPERTIES);
        if (object instanceof MapItem) {
            item = (MapItem) object;

            final EditText textView = (EditText) findViewById(R.id.gpslogname);
            final Spinner colorView = (Spinner) findViewById(R.id.color_spinner);
            final Spinner widthView = (Spinner) findViewById(R.id.widthText);

            textView.setText(item.getName());
            newText = item.getName();
            textView.addTextChangedListener(new TextWatcher(){

                public void onTextChanged( CharSequence s, int start, int before, int count ) {
                    newText = textView.getText().toString();
                }
                public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
                }
                public void afterTextChanged( Editable s ) {
                }
            });

            newWidth = item.getWidth();
            ArrayAdapter< ? > widthSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_widths,
                    android.R.layout.simple_spinner_item);
            widthSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            widthView.setAdapter(widthSpinnerAdapter);
            int widthIndex = widthsList.indexOf(String.valueOf((int) item.getWidth()));
            widthView.setSelection(widthIndex);
            widthView.setOnItemSelectedListener(new OnItemSelectedListener(){
                public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                    Object selectedItem = widthView.getSelectedItem();
                    newWidth = Float.parseFloat(selectedItem.toString());
                }
                public void onNothingSelected( AdapterView< ? > arg0 ) {
                }
            });

            // color box
            newColor = item.getColor();
            ArrayAdapter< ? > colorSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_colornames,
                    android.R.layout.simple_spinner_item);
            colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            colorView.setAdapter(colorSpinnerAdapter);
            int colorIndex = colorList.indexOf(item.getColor());
            colorView.setSelection(colorIndex);
            colorView.setOnItemSelectedListener(new OnItemSelectedListener(){

                public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                    Object selectedItem = colorView.getSelectedItem();
                    newColor = selectedItem.toString();
                }
                public void onNothingSelected( AdapterView< ? > arg0 ) {
                }
            });

            final Button chartButton = (Button) findViewById(R.id.gpslog_chart);
            chartButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    Intent intent = new Intent(GpsDataPropertiesActivity.this, ChartActivity.class);
                    intent.putExtra(Constants.ID, item.getId());
                    startActivity(intent);
                }
            });
            final Button zoomButton = (Button) findViewById(R.id.gpslog_zoom);
            zoomButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        double[] firstPoint = DaoGpsLog.getGpslogFirstPoint(GpsDataPropertiesActivity.this, item.getId());
                        if (firstPoint != null) {
                            Intent intent = getIntent();
                            intent.putExtra(LibraryConstants.LATITUDE, firstPoint[0]);
                            intent.putExtra(LibraryConstants.LONGITUDE, firstPoint[1]);
                            setResult(Activity.RESULT_OK, intent);
                        }
                    } catch (IOException e) {
                        Logger.e(this, e.getLocalizedMessage(), e);
                        e.printStackTrace();
                    }
                }
            });
            final Button deleteButton = (Button) findViewById(R.id.gpslog_delete);
            deleteButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        long id = item.getId();
                        new DaoGpsLog().deleteGpslog(GpsDataPropertiesActivity.this, id);
                        finish();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            final Button okButton = (Button) findViewById(R.id.gpslog_ok);
            okButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        DaoGpsLog.updateLogProperties(GpsDataPropertiesActivity.this, item.getId(), newColor, newWidth,
                                item.isVisible(), newText);
                    } catch (IOException e) {
                        Logger.e(this, e.getLocalizedMessage(), e);
                        e.printStackTrace();
                    }
                    finish();
                }
            });

            final Button cancelButton = (Button) findViewById(R.id.gpslog_cancel);
            cancelButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    finish();
                }
            });

        }
    }
    
    private void getResourcesAndColors() {
        if (colorList == null) {
            String[] colorArray = getResources().getStringArray(R.array.array_colornames);
            colorList = Arrays.asList(colorArray);
            String[] widthsArray = getResources().getStringArray(R.array.array_widths);
            widthsList = Arrays.asList(widthsArray);
        }

    }

}
