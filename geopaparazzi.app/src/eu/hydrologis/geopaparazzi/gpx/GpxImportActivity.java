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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.CheckBox;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

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
            path = extras.getString(Constants.PATH);

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
                                boolean asPoint = input.isChecked();

                                File file = new File(path);

                                String name = file.getName();
                                GpxItem item = new GpxItem();
                                item.setFilename(name);
                                item.setFilepath(path);
                                item.setLine(!asPoint);
                                item.setWidth("2"); //$NON-NLS-1$
                                item.setVisible(false);
                                item.setColor("red"); //$NON-NLS-1$

                                if (!asPoint) {
                                    DaoMaps.importGpxToMap(GpxImportActivity.this, item,
                                            Constants.MAP_TYPE_LINE);
                                } else {
                                    DaoMaps.importGpxToMap(GpxImportActivity.this, item,
                                            Constants.MAP_TYPE_POINT);
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

            alert.setNegativeButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener(){
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
