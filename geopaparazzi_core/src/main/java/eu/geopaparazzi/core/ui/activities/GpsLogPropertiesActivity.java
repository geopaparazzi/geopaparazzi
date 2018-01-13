/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.core.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.core.dialogs.ColorStrokeDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorStrokeObject;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.objects.LogMapItem;
import eu.geopaparazzi.core.utilities.Constants;

/**
 * Data properties activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsLogPropertiesActivity extends AppCompatActivity implements ColorStrokeDialogFragment.IColorStrokePropertiesChangeListener {
    private static List<String> colorList;
    private static List<String> widthsList;
    private LogMapItem item;

    // properties
    private String newText;
    private float newWidth;
    private String newColor;
    private double newLengthm;
    private StringAsyncTask task = null;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_gpsdataproperties);

        Toolbar toolbar = findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        getResourcesAndColors();


        Bundle extras = getIntent().getExtras();
        Object object = extras.get(Constants.PREFS_KEY_GPSLOG4PROPERTIES);
        if (object instanceof LogMapItem) {
            item = (LogMapItem) object;

            final TextView startTimeTextView = findViewById(R.id.starttime_label);
            long startTime = item.getStartTime();
            String startTimeStr = " - ";
            try {
                startTimeStr = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(startTime));
            } catch (Exception e1) {
                GPLog.error(this, "error in start time conversion: " + startTime, e1); //$NON-NLS-1$
            }
            String startText = startTimeTextView.getText().toString();
            startTimeTextView.setText(startText + " " + startTimeStr);

            final TextView endTimeTextView = findViewById(R.id.endtime_label);
            long endTime = item.getEndTime();
            String endTimeStr = " - ";
            if (startTime != endTime) {
                try {
                    endTimeStr = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(endTime));
                } catch (Exception e1) {
                    GPLog.error(this, "error in end time conversion: " + endTime, e1); //$NON-NLS-1$
                }
            }

            String endText = endTimeTextView.getText().toString();
            endTimeTextView.setText(endText + " " + endTimeStr);

            final EditText lognameTextView = findViewById(R.id.gpslogname);

            lognameTextView.setText(item.getName());
            newText = item.getName();
            lognameTextView.addTextChangedListener(new TextWatcher() {

                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    newText = lognameTextView.getText().toString();
                }

                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // ignore
                }

                public void afterTextChanged(Editable s) {
                    // ignore
                }
            });

            // log (track) length field
            final TextView trackLengthTextView = findViewById(R.id.trackLength_label);
            String lengthm = item.getLengthInM();
            final String lengthText = trackLengthTextView.getText().toString();
            trackLengthTextView.setText(lengthText + " " + lengthm + "m"); //$NON-NLS-1$ //$NON-NLS-2$

            // button to update the log (track) length field
            final ImageButton refreshLogLenButton = findViewById(R.id.gpslog_refreshLogLength);
            refreshLogLenButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    final long logID = item.getLogID();
                    task = new StringAsyncTask(GpsLogPropertiesActivity.this) {
                        @Override
                        protected void doUiPostWork(String response) {
                            trackLengthTextView.setText(response);
                            dispose();
                        }

                        @Override
                        protected String doBackgroundWork() {
                            try {
                                newLengthm = DaoGpsLog.updateLogLength(logID);
                            } catch (IOException e) {
                                GPLog.error(GpsLogPropertiesActivity.this, "ERROR", e);
                                return "ERROR";
                            }
                            String newLen = Long.toString(Math.round(newLengthm));
                            return lengthText + " " + newLen + "m";
                        }
                    };
                    task.setProgressDialog(null, getString(R.string.calculate_length), false, null);
                    task.execute();

                }
            });

            newColor = item.getColor();
            newWidth = item.getWidth();
            final ImageButton paletteButton = findViewById(R.id.gpslog_palette);
            paletteButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    int color = ColorUtilities.toColor(newColor);
                    ColorStrokeObject colorStrokeObject = new ColorStrokeObject();
                    colorStrokeObject.hasFill = false;

                    colorStrokeObject.hasStroke = true;
                    colorStrokeObject.strokeColor = color;
                    colorStrokeObject.strokeAlpha = 255;

                    colorStrokeObject.hasStrokeWidth = true;
                    colorStrokeObject.strokeWidth = (int) newWidth;

                    ColorStrokeDialogFragment colorStrokeDialogFragment = ColorStrokeDialogFragment.newInstance(colorStrokeObject);
                    colorStrokeDialogFragment.show(getSupportFragmentManager(), "Color Stroke Dialog");
                }
            });

            final ImageButton chartButton = findViewById(R.id.gpslog_chart);
            chartButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(GpsLogPropertiesActivity.this, ProfileChartActivity.class);
                    intent.putExtra(Constants.ID, item.getId());
                    startActivity(intent);
                }
            });
            final ImageButton zoomToStartButton = findViewById(R.id.gpslog_zoom_start);
            zoomToStartButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
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
                    }
                    finish();
                }
            });
            final ImageButton zoomToEndButton = findViewById(R.id.gpslog_zoom_end);
            zoomToEndButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
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
                    }
                }
            });

            final ImageButton deleteButton = findViewById(R.id.gpslog_delete);
            deleteButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {


                    GPDialogs.yesNoMessageDialog(GpsLogPropertiesActivity.this, "The log will be removed. This can't be undone.", new Runnable() {
                        @Override
                        public void run() {
                            try {

                                long id = item.getId();
                                new DaoGpsLog().deleteGpslog(id);
                                finish();
                            } catch (IOException e) {
                                GPLog.error(this, null, e); //$NON-NLS-1$
                            }
                        }
                    }, null);

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (task != null) task.dispose();

        super.onDestroy();
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

    @Override
    public void onPropertiesChanged(ColorStrokeObject newColorStrokeObject) {
        newColor = ColorUtilities.getHex(newColorStrokeObject.strokeColor);
        newWidth = newColorStrokeObject.strokeWidth;
        updateWithNewValues();
    }
}
