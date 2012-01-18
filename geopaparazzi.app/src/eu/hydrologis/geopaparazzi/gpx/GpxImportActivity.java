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
package eu.hydrologis.geopaparazzi.gpx;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.CheckBox;
import eu.geopaparazzi.library.gpx.GpxItem;
import eu.geopaparazzi.library.gpx.parser.GpxParser;
import eu.geopaparazzi.library.gpx.parser.GpxParser.Route;
import eu.geopaparazzi.library.gpx.parser.GpxParser.TrackSegment;
import eu.geopaparazzi.library.gpx.parser.WayPoint;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;

/**
 * Gpx file import activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpxImportActivity extends Activity {

    private String path;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        // setContentView(R.layout.note);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            path = extras.getString(LibraryConstants.PREFS_KEY_PATH);

            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle(getString(R.string.gpx_import_title));
            alert.setMessage(getString(R.string.gpx_import_message));

            // Set an EditText view to get user input
            final CheckBox input = new CheckBox(this);
            input.setText(getString(R.string.gpx_import_checkbox_message));
            alert.setView(input);

            alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener(){
                public void onClick( DialogInterface dialog, int whichButton ) {
                    gpxImportProgressDialog = ProgressDialog.show(GpxImportActivity.this,
                            getString(R.string.gpx_import_processing), "", true, true); //$NON-NLS-1$
                    new Thread(){

                        public void run() {
                            try {
                                boolean asLines = input.isChecked();

                                File file = new File(path);
                                GpxParser parser = new GpxParser(path);
                                if (parser.parse()) {
                                    List<WayPoint> wayPoints = parser.getWayPoints();
                                    if (wayPoints.size() > 0) {
                                        String name = file.getName();
                                        GpxItem item = new GpxItem();
                                        item.setName(name);
                                        item.setWidth("2"); //$NON-NLS-1$
                                        item.setVisible(false);
                                        item.setColor("blue"); //$NON-NLS-1$
                                        item.setData(wayPoints);
                                        DaoMaps.importGpxToMap(GpxImportActivity.this, item, asLines);
                                    }
                                    List<TrackSegment> tracks = parser.getTracks();
                                    if (tracks.size() > 0) {
                                        for( TrackSegment trackSegment : tracks ) {
                                            String name = trackSegment.getName();
                                            GpxItem item = new GpxItem();
                                            item.setName(name);
                                            item.setWidth("2"); //$NON-NLS-1$
                                            item.setVisible(false);
                                            item.setColor("red"); //$NON-NLS-1$
                                            item.setData(trackSegment);
                                            DaoMaps.importGpxToMap(GpxImportActivity.this, item, asLines);
                                        }
                                    }
                                    List<Route> routes = parser.getRoutes();
                                    if (routes.size() > 0) {
                                        for( Route route : routes ) {
                                            String name = route.getName();
                                            GpxItem item = new GpxItem();
                                            item.setName(name);
                                            item.setWidth("2"); //$NON-NLS-1$
                                            item.setVisible(false);
                                            item.setColor("green"); //$NON-NLS-1$
                                            item.setData(route);
                                            DaoMaps.importGpxToMap(GpxImportActivity.this, item, asLines);
                                        }
                                    }
                                } else {
                                    if (Debug.D) Logger.d(this, "ERROR"); //$NON-NLS-1$
                                }

                            } catch (IOException e) {
                                Logger.e(this, e.getLocalizedMessage(), e);
                                e.printStackTrace();
                            } finally {
                                gpsImportHandler.sendEmptyMessage(0);
                                finish();
                            }
                        }
                    }.start();

                }
            });

            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){
                public void onClick( DialogInterface dialog, int whichButton ) {
                    finish();
                }
            });

            alert.show();

        }

    }
    private ProgressDialog gpxImportProgressDialog;
    private Handler gpsImportHandler = new Handler(){
        public void handleMessage( android.os.Message msg ) {
            gpxImportProgressDialog.dismiss();
        };
    };
}
