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
package eu.hydrologis.geopaparazzi.util;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maps.ViewportManager;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Activity to get to a location by coordinate.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class InsertCoordActivity extends Activity {
    private ApplicationManager applicationManager;
    private EditText latText;
    private EditText lonText;

    private final Handler openMapViewHandler = new Handler();

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.insertcoord);

        applicationManager = ApplicationManager.getInstance(this);

        lonText = (EditText) findViewById(R.id.longitudetext);
        latText = (EditText) findViewById(R.id.latitudetext);

        Button fromCoordsButton = (Button) findViewById(R.id.coordOkButton);
        fromCoordsButton.setOnClickListener(new Button.OnClickListener(){
            private double lat;
            private double lon;

            public void onClick( View v ) {
                String lonString = String.valueOf(lonText.getText());
                try {
                    lon = Double.parseDouble(lonString);
                    if (lon < -180 || lon > 180) {
                        throw new Exception();
                    }
                } catch (Exception e1) {
                    Logger.e(this, e1.getLocalizedMessage(), e1);
                    Toast.makeText(InsertCoordActivity.this, R.string.wrongLongitude, Toast.LENGTH_LONG).show();
                    return;
                }
                String latString = String.valueOf(latText.getText());
                try {
                    lat = Double.parseDouble(latString);
                    if (lat < -90 || lat > 90) {
                        throw new Exception();
                    }
                } catch (Exception e1) {
                    Logger.e(this, e1.getLocalizedMessage(), e1);
                    Toast.makeText(InsertCoordActivity.this, R.string.wrongLatitude, Toast.LENGTH_LONG).show();
                    return;
                }

                new Thread(){
                    public void run() {
                        openMapViewHandler.post(new Runnable(){
                            public void run() {
                                ViewportManager.INSTANCE.setCenterTo(lon, lat, true);
                                ViewportManager.INSTANCE.invalidateMap();
                            }
                        });

                    }
                }.start();

                finish();
            }
        });
    }

}
