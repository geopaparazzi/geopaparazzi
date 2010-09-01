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
package eu.hydrologis.geopaparazzi.osm;

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
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;

/**
 * Data properties activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsDataPropertiesActivity extends Activity {
    private static List<String> colorList;
    private static List<String> widthsList;
    private MapItem item;
    private EditText textView;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.gpslog_properties);
        getResourcesAndColors();

        Bundle extras = getIntent().getExtras();
        Object object = extras.get(Constants.PREFS_KEY_GPSLOG4PROPERTIES);
        if (object instanceof MapItem) {
            item = (MapItem) object;

            textView = (EditText) findViewById(R.id.gpslogname);
            final Spinner colorView = (Spinner) findViewById(R.id.color_spinner);
            final Spinner widthView = (Spinner) findViewById(R.id.widthText);

            textView.setText(item.getName());
            textView.addTextChangedListener(new TextWatcher(){
                public void onTextChanged( CharSequence s, int start, int before, int count ) {
                    item.setDirty(true);
                }
                public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
                }
                public void afterTextChanged( Editable s ) {
                }
            });

            // width spinner
            ArrayAdapter< ? > widthSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_widths,
                    android.R.layout.simple_spinner_item);
            widthSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            widthView.setAdapter(widthSpinnerAdapter);
            int widthIndex = widthsList.indexOf(String.valueOf((int) item.getWidth()));
            widthView.setSelection(widthIndex);
            widthView.setOnItemSelectedListener(new OnItemSelectedListener(){
                public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                    Object selectedItem = widthView.getSelectedItem();
                    float width = item.getWidth();
                    float newWidth = Float.parseFloat(selectedItem.toString());
                    if (width != newWidth) {
                        item.setWidth(newWidth);
                        item.setDirty(true);
                    }
                }
                public void onNothingSelected( AdapterView< ? > arg0 ) {
                }
            });

            // color box
            ArrayAdapter< ? > colorSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_colornames,
                    android.R.layout.simple_spinner_item);
            colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            colorView.setAdapter(colorSpinnerAdapter);
            int colorIndex = colorList.indexOf(item.getColor());
            colorView.setSelection(colorIndex);
            colorView.setOnItemSelectedListener(new OnItemSelectedListener(){
                public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                    Object selectedItem = colorView.getSelectedItem();
                    String color = item.getColor();
                    String newColor = selectedItem.toString();
                    if (!color.equals(newColor)) {
                        item.setColor(newColor);
                        // rowView.setBackgroundColor(Color.parseColor(item.getColor()));
                        item.setDirty(true);
                    }
                }
                public void onNothingSelected( AdapterView< ? > arg0 ) {
                }
            });

            final Button chartButton = (Button) findViewById(R.id.gpslog_chart);
            chartButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    Intent intent = new Intent(Constants.VIEW_IN_CHART);
                    intent.putExtra(Constants.ID, item.getId());
                    startActivity(intent);
                }
            });
            final Button zoomButton = (Button) findViewById(R.id.gpslog_zoom);
            zoomButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        Line line = DaoGpsLog.getGpslogAsLine(item.getId());
                        if (line.getLonList().size() > 0) {
                            ApplicationManager.getInstance().getOsmView()
                                    .setGotoCoordinate(line.getLonList().get(0), line.getLatList().get(0));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            final Button deleteButton = (Button) findViewById(R.id.gpslog_delete);
            deleteButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        long id = item.getId();
                        DaoGpsLog.deleteGpslog(id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
    }

    @Override
    protected void onPause() {
        try {
            if (item != null && item.isDirty()) {
                String newText = textView.getText().toString();
                DaoGpsLog.updateLogProperties(item.getId(), item.getColor(), item.getWidth(), item.isVisible(), newText);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onPause();

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
