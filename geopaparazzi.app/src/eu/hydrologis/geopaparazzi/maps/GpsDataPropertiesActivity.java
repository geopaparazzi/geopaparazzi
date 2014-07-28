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
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.chart.ProfileChartActivity;
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
    private LogMapItem item;

    // properties
    private String newText;
    private float newWidth;
    private String newColor;
    private double newLengthm;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.gpslog_properties);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        getResourcesAndColors();

        Bundle extras = getIntent().getExtras();
        Object object = extras.get(Constants.PREFS_KEY_GPSLOG4PROPERTIES);
        if (object instanceof LogMapItem) {
            item = (LogMapItem) object;

            final TextView startTimeTextView = (TextView) findViewById(R.id.starttime_label);
            String startTime = item.getStartTime();
            try {
                startTime = TimeUtilities.utcToLocalTime(startTime);
            } catch (Exception e1) {
                GPLog.error(this, "error in start time conversion: " + startTime, e1); //$NON-NLS-1$
            }
            String startText = startTimeTextView.getText().toString();
            startTimeTextView.setText(startText + startTime);
            final TextView endTimeTextView = (TextView) findViewById(R.id.endtime_label);
            String endTime = item.getEndTime();
            try {
                endTime = TimeUtilities.utcToLocalTime(endTime);
            } catch (Exception e1) {
                GPLog.error(this, "error in end time conversion: " + startTime, e1); //$NON-NLS-1$
            }

            if (startTime.equals(endTime)) {
                endTime = " - "; //$NON-NLS-1$
            }

            String endText = endTimeTextView.getText().toString();
            endTimeTextView.setText(endText + endTime);
            final EditText lognameTextView = (EditText) findViewById(R.id.gpslogname);
            final Spinner colorView = (Spinner) findViewById(R.id.color_spinner);
            final Spinner widthView = (Spinner) findViewById(R.id.widthText);

            lognameTextView.setText(item.getName());
            newText = item.getName();
            lognameTextView.addTextChangedListener(new TextWatcher(){

                public void onTextChanged( CharSequence s, int start, int before, int count ) {
                    newText = lognameTextView.getText().toString();
                }
                public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
                    // ignore
                }
                public void afterTextChanged( Editable s ) {
                    // ignore
                }
            });

            // log (track) length field
            final TextView trackLengthTextView = (TextView) findViewById(R.id.trackLength_label);
            String lengthm = item.getLengthInM();
            final String lengthText = trackLengthTextView.getText().toString();
            trackLengthTextView.setText(lengthText + " " + lengthm + "m"); //$NON-NLS-1$ //$NON-NLS-2$

            // button to update the log (track) length field
            final ImageButton refreshLogLenButton = (ImageButton) findViewById(R.id.gpslog_refreshLogLength);
            refreshLogLenButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    final long logID = item.getLogID();
                    @SuppressWarnings("nls")
                    StringAsyncTask task = new StringAsyncTask(GpsDataPropertiesActivity.this){
                        @Override
                        protected void doUiPostWork( String response ) {
                            trackLengthTextView.setText(response);
                            dispose();
                        }
                        @Override
                        protected String doBackgroundWork() {
                            try {
                                newLengthm = DaoGpsLog.updateLogLength(logID);
                            } catch (IOException e) {
                                GPLog.error(GpsDataPropertiesActivity.this, "ERROR", e);
                                return "ERROR";
                            }
                            String newLen = Long.toString(Math.round(newLengthm));
                            return lengthText + " " + newLen + "m";
                        }
                    };
                    task.startProgressDialog(getString(R.string.info), getString(R.string.calculate_length), false, null);
                    task.execute();

                }
            });

            // line width
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
                    // ignore
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
                    // ignore
                }
            });

            final Button chartButton = (Button) findViewById(R.id.gpslog_chart);
            chartButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    Intent intent = new Intent(GpsDataPropertiesActivity.this, ProfileChartActivity.class);
                    intent.putExtra(Constants.ID, item.getId());
                    startActivity(intent);
                }
            });
            final Button zoomToStartButton = (Button) findViewById(R.id.gpslog_zoom_start);
            zoomToStartButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        double[] firstPoint = DaoGpsLog.getGpslogFirstPoint(item.getId());
                        if (firstPoint != null) {
                            Intent intent = getIntent();
                            intent.putExtra(LibraryConstants.LATITUDE, firstPoint[1]);
                            intent.putExtra(LibraryConstants.LONGITUDE, firstPoint[0]);
                            setResult(Activity.RESULT_OK, intent);
                        }
                    } catch (IOException e) {
                        GPLog.error(this, e.getLocalizedMessage(), e);
                        e.printStackTrace();
                    }
                    finish();
                }
            });
            final Button zoomToEndButton = (Button) findViewById(R.id.gpslog_zoom_end);
            zoomToEndButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        double[] firstPoint = DaoGpsLog.getGpslogLastPoint(item.getId());
                        if (firstPoint != null) {
                            Intent intent = getIntent();
                            intent.putExtra(LibraryConstants.LATITUDE, firstPoint[1]);
                            intent.putExtra(LibraryConstants.LONGITUDE, firstPoint[0]);
                            setResult(Activity.RESULT_OK, intent);
                        }
                        finish();
                    } catch (IOException e) {
                        GPLog.error(this, e.getLocalizedMessage(), e);
                        e.printStackTrace();
                    }
                }
            });

            final Button deleteButton = (Button) findViewById(R.id.gpslog_delete);
            deleteButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    try {
                        long id = item.getId();
                        new DaoGpsLog().deleteGpslog(id);
                        finish();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void finish() {
        updateWithNewValues();
        super.finish();
    }

    private void updateWithNewValues() {
        try {
            DaoGpsLog.updateLogProperties(item.getId(), newColor, newWidth, item.isVisible(), newText);
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
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
