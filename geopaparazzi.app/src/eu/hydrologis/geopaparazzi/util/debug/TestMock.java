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
package eu.hydrologis.geopaparazzi.util.debug;

import java.util.Date;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;

/**
 * A class for when there is no gps cover.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMock {
    private static double lat = 45;
    private static double lon = 11;
    private static double alt = 100;
    private static long date = new Date().getTime();
    public static String MOCK_PROVIDER_NAME = LocationManager.GPS_PROVIDER;
    public static boolean isOn = false;

    /**
     * Starts to trigger mock locations.
     * 
     * @param locationManager the location manager.
     * @param applicationManager 
     */
    public static void startMocking( final LocationManager locationManager, ApplicationManager applicationManager ) {
        if (isOn) {
            return;
        }
        // Get some mock location data in the game
        // LocationProvider provider = locationManager.getProvider(MOCK_PROVIDER_NAME);
        // if (provider == null) {
        locationManager.addTestProvider(MOCK_PROVIDER_NAME, true, false, true, false, false, false, false, Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true);
        locationManager.requestLocationUpdates(TestMock.MOCK_PROVIDER_NAME, 3000, 0f, applicationManager);
        // }

        Runnable r = new Runnable(){
            public void run() {
                isOn = true;
                while( isOn ) {
                    Location location = new Location(MOCK_PROVIDER_NAME);
                    location.setLatitude(lat);
                    location.setLongitude(lon);
                    location.setTime(date);
                    location.setAltitude(alt);
                    locationManager.setTestProviderLocation(MOCK_PROVIDER_NAME, location);

                    lat = lat + 0.001;
                    lon = lon + 0.001;
                    alt = alt + 1.0;
                    date = date + 5000l;

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    /**
     * Stops the mocking.
     * 
     * @param locationManager the location manager.
     */
    public static void stopMocking( final LocationManager locationManager ) {
        isOn = false;
        locationManager.removeTestProvider(MOCK_PROVIDER_NAME);
    }
}
