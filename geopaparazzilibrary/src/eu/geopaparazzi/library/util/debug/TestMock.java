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
package eu.geopaparazzi.library.util.debug;

import java.util.Date;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.SystemClock;
import eu.geopaparazzi.library.gps.GpsManager;

/**
 * A class for when there is no gps cover.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMock {
    private static double lat = 46.681034;
    private static double lon = 11.13507645;

    static {
        // set office
        lat = 46.48193;
        lon = 11.330099;
        // set home
        // lat = 46.681034;
        // lon = 11.13507645;

    }

    private static double alt = 100;
    private static long date = new Date().getTime();
    public static String MOCK_PROVIDER_NAME = LocationManager.GPS_PROVIDER;
    public static boolean isOn = false;

    public static double a = 0.001;
    public static double radius = 0.1;

    private static double t = 1.0;

    /**
     * Starts to trigger mock locations.
     * 
     * @param locationManager the location manager.
     * @param gpsManager 
     */
    public static void startMocking( final LocationManager locationManager, GpsManager gpsManager ) {
        if (isOn) {
            return;
        }

        // Get some mock location data in the game
        // LocationProvider provider = locationManager.getProvider(MOCK_PROVIDER_NAME);
        // if (provider == null) {
        locationManager.addTestProvider(MOCK_PROVIDER_NAME, true, false, true, false, false, false, false, Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE);
        // locationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true);
        // }
        locationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true);
        locationManager
                .setTestProviderStatus(MOCK_PROVIDER_NAME, LocationProvider.AVAILABLE, null, SystemClock.elapsedRealtime());
        locationManager.requestLocationUpdates(TestMock.MOCK_PROVIDER_NAME, 3000, 0f, gpsManager);

        Runnable r = new Runnable(){
            public void run() {
                isOn = true;

                while( isOn ) {
                    try {
                        Location location = new Location(MOCK_PROVIDER_NAME);
                        location.setLatitude(lat);
                        location.setLongitude(lon);
                        location.setTime(date);
                        location.setAltitude(alt);
                        location.setAccuracy(1f);
                        location.setSpeed(1f);
                        locationManager.setTestProviderLocation(MOCK_PROVIDER_NAME, location);

                        lon = lon + a * radius * Math.cos(t);
                        lat = lat + a * radius * Math.sin(t);
                        t = t + 1;
                        alt = alt + 1.0;
                        date = date + 5000l;
                    } catch (Exception e) {
                        // ignore it
                    } finally {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Logger.e(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                        }
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
