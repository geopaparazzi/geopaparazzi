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

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.osm.OsmView;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class InsertCoordActivity extends Activity {
    private ApplicationManager applicationManager;
    private EditText latText;
    private EditText lonText;
    private EditText addressText;

    private final Handler openMapViewHandler = new Handler();

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.insertcoord);

        applicationManager = ApplicationManager.getInstance();

        lonText = (EditText) findViewById(R.id.longitudetext);
        latText = (EditText) findViewById(R.id.latitudetext);
        addressText = (EditText) findViewById(R.id.addresstext);

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
                    Toast.makeText(InsertCoordActivity.this, R.string.wrongLongitude,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                String latString = String.valueOf(latText.getText());
                try {
                    lat = Double.parseDouble(latString);
                    if (lat < -90 || lat > 90) {
                        throw new Exception();
                    }
                } catch (Exception e1) {
                    Toast.makeText(InsertCoordActivity.this, R.string.wrongLatitude,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                new Thread(){
                    public void run() {
                        openMapViewHandler.post(new Runnable(){
                            public void run() {
                                OsmView osmView = applicationManager.getOsmView();
                                osmView.requestFocus();
                                osmView.setGotoCoordinate(lon, lat);
                            }
                        });

                    }
                }.start();

                finish();
            }
        });

        Button fromAddressButton = (Button) findViewById(R.id.addressOkButton);
        fromAddressButton.setOnClickListener(new Button.OnClickListener(){
            private double lat;
            private double lon;

            public void onClick( View v ) {
                String addressString = String.valueOf(addressText.getText());
                if (addressString.length() < 1) {
                    Toast.makeText(InsertCoordActivity.this, R.string.emptyaddress,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Geocoder gc = new Geocoder(InsertCoordActivity.this);
                List<Address> foundAdresses = null;
                try {
                    foundAdresses = gc.getFromLocationName(addressString, 5);
                } catch (IOException e) {
                    Toast.makeText(InsertCoordActivity.this, R.string.cantretrievefromaddress,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (foundAdresses == null || foundAdresses.size() == 0) {
                    Toast.makeText(InsertCoordActivity.this, R.string.cantretrievefromaddress,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Address addressResult = foundAdresses.get(0);
                lat = addressResult.getLatitude();
                lon = addressResult.getLongitude();

                new Thread(){
                    public void run() {
                        openMapViewHandler.post(new Runnable(){
                            public void run() {
                                OsmView osmView = applicationManager.getOsmView();
                                osmView.requestFocus();
                                osmView.setGotoCoordinate(lon, lat);
                            }
                        });

                    }
                }.start();

                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.coordCancelButton);
        cancelButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                finish();
            }
        });
    }

}
