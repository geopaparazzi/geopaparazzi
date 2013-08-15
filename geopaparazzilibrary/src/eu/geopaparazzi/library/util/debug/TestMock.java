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

import java.lang.reflect.Method;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.SystemClock;
import android.util.Log;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsManager;

/**
 * A class for when there is no gps cover.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMock {
    public static String MOCK_PROVIDER_NAME = LocationManager.GPS_PROVIDER;
    public static boolean isOn = false;
    private static Method locationJellyBeanFixMethod = null;
    private static IFakeGpsLog fakeGpsLog = null;

    public static void setFakeGpsLog( IFakeGpsLog fakeGpsLog ) {
        TestMock.fakeGpsLog = fakeGpsLog;
    }

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

        if (fakeGpsLog == null) {
            fakeGpsLog = new DefaultFakeGpsLog();
        }

        // Get some mock location data in the game
        // LocationProvider provider = locationManager.getProvider(MOCK_PROVIDER_NAME);
        // if (provider == null) {
        locationManager.addTestProvider(//
                MOCK_PROVIDER_NAME, //
                false, //
                false, //
                false, //
                false, //
                true, //
                true, //
                false, //
                Criteria.POWER_LOW,//
                Criteria.ACCURACY_FINE//
                );
        locationManager.setTestProviderEnabled(MOCK_PROVIDER_NAME, true);

        locationManager.requestLocationUpdates(TestMock.MOCK_PROVIDER_NAME, 2000, 0f, gpsManager);

        try {
            locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
        } catch (Exception e1) {
            // ignore
        }

        Runnable r = new Runnable(){
            public void run() {
                isOn = true;

                long previousT = -1;
                long t = 0;
                while( isOn ) {
                    String nextLine = null;
                    if (fakeGpsLog.hasNext()) {
                        nextLine = fakeGpsLog.next();

                        String[] lineSplit = nextLine.split(",");
                        t = Long.parseLong(lineSplit[0]);
                        double lon = Double.parseDouble(lineSplit[1]);
                        double lat = Double.parseDouble(lineSplit[2]);
                        double alt = Double.parseDouble(lineSplit[3]);
                        float v = Float.parseFloat(lineSplit[4]);
                        float accuracy = Float.parseFloat(lineSplit[5]);

                        Location location = new Location(MOCK_PROVIDER_NAME);
                        location.setLatitude(lat);
                        location.setLongitude(lon);
                        location.setTime(t);
                        location.setAltitude(alt);
                        location.setAccuracy(accuracy);
                        location.setSpeed(v);
                        if (locationJellyBeanFixMethod != null) {
                            try {
                                locationJellyBeanFixMethod.invoke(location);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        location.setSpeed(v);

                        locationManager.setTestProviderStatus(//
                                MOCK_PROVIDER_NAME, //
                                LocationProvider.AVAILABLE, //
                                null, //
                                SystemClock.elapsedRealtime()//
                                );
                        locationManager.setTestProviderLocation(MOCK_PROVIDER_NAME, location);
                    } else {
                        fakeGpsLog.reset();
                    }

                    try {
                        if (previousT < 0) {
                            Thread.sleep(2000);
                        } else {
                            long millis = t - previousT;
                            // Log.i("MOCK_PROVIDER_NAME", nextLine + "///" + millis);
                            if (millis < 0) {
                                millis = 2000;
                            }
                            Thread.sleep(millis);
                        }
                        previousT = t;
                    } catch (InterruptedException e) {
                        GPLog.error(this, e.getLocalizedMessage(), e);
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
