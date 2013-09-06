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
package eu.geopaparazzi.library.util;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_ELEV;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_LAT;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_LON;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_MAPCENTER_LAT;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_MAPCENTER_LON;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_MAP_ZOOM;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Position and preferences related utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PositionUtilities {

    private static final String LOG_TAG = "POSITIONUTILITIES";
    private static final float NOVALUE_CHECKVALUE = -9998f;
    private static final float NOVALUE = -9999f;

    /**
     * Insert the gps position data in the preferences.
     * 
     * <p>This method handles double->float conversion of the values where necessary.</p>
     * 
     * @param preferences the preferences to use.
     * @param longitude the longitude in its real value.
     * @param latitude  the latitude in its real value.
     * @param elevation the elevation in meters.
     */
    @SuppressWarnings("nls")
    public static void putGpsLocationInPreferences( SharedPreferences preferences, double longitude, double latitude,
            double elevation ) {
        if (longitude > 1E5 || longitude < -1E5 //
                || latitude > 1E5 || latitude < -1E5) {
            throw new IllegalArgumentException("The method takes coordinate values in their real format.");
        }
        Editor editor = preferences.edit();
        float longFloat = (float) longitude * LibraryConstants.E6;
        float latFloat = (float) latitude * LibraryConstants.E6;
        if (GPLog.LOG_ABSURD) {
            GPLog.addLogEntry(LOG_TAG, "putGpsLocation: " + longFloat + "/" + latFloat);
        }
        editor.putFloat(PREFS_KEY_LON, longFloat);
        editor.putFloat(PREFS_KEY_LAT, latFloat);
        editor.putFloat(PREFS_KEY_ELEV, (float) elevation);
        editor.commit();
    }

    /**
     * Get the gps position data from the preferences.
     * 
     * <p>This method handles float->double conversion of the values where necessary.</p>
     * 
     * @param preferences the preferences to use.
     * @return the array containing [lon, lat, elevation].
     */
    @SuppressWarnings("nls")
    public static double[] getGpsLocationFromPreferences( SharedPreferences preferences ) {
        // these are *E6 values of the coordinates
        float lonFloat = preferences.getFloat(PREFS_KEY_LON, NOVALUE);
        float latFloat = preferences.getFloat(PREFS_KEY_LAT, NOVALUE);

        double lon = (double) lonFloat / LibraryConstants.E6;
        double lat = (double) latFloat / LibraryConstants.E6;
        if (lon < NOVALUE_CHECKVALUE || lat < NOVALUE_CHECKVALUE) {
            return null;
        }
        if (lon == 0f && lat == 0f) {
            // we also do not believe in 0,0
            return null;
        }
        if (GPLog.LOG_ABSURD) {
            GPLog.addLogEntry(LOG_TAG, "getGpsLocation: " + lon + "/" + lat);
        }
        double elevation = (double) preferences.getFloat(PREFS_KEY_ELEV, 0f);
        return new double[]{lon, lat, elevation};
    }

    /**
     * Insert the map center position data in the preferences.
     * 
     * <p>This method handles double->float conversion of the values where necessary.</p>
     * 
     * @param preferences the preferences to use.
     * @param longitude the longitude in its real value.
     * @param latitude  the latitude in its real value.
     * @param zoom the zoomlevel.
     */
    @SuppressWarnings("nls")
    public static void putMapCenterInPreferences( SharedPreferences preferences, double longitude, double latitude, float zoom ) {
        Editor editor = preferences.edit();
        float longFloat = (float) longitude * LibraryConstants.E6;
        float latFloat = (float) latitude * LibraryConstants.E6;
        if (GPLog.LOG_ABSURD) {
            GPLog.addLogEntry(LOG_TAG, "putMapCenter: " + longFloat + "/" + latFloat);
        }
        editor.putFloat(PREFS_KEY_MAPCENTER_LON, longFloat);
        editor.putFloat(PREFS_KEY_MAPCENTER_LAT, latFloat);
        editor.putFloat(PREFS_KEY_MAP_ZOOM, zoom);
        editor.commit();
    }

    /**
     * Get the map center position data from the preferences.
     * 
     * <p>This method handles float->double conversion of the values where necessary.</p>
     * 
     * @param preferences the preferences to use.
     * @param backOnGps if set to <code>true</code> and the map center was not set,
     *          it backs on the gps position.
     * @param backOnZero if set to <code>true</code> it assures that
     *          the result is never <code>null</code>. In case 0,0 is used.
     *          Note that this can be used only if backOnGps is true, else
     *          it will be ignored.
     * @return the array containing [lon, lat, zoom].
     */
    @SuppressWarnings("nls")
    public static double[] getMapCenterFromPreferences( SharedPreferences preferences, boolean backOnGps, boolean backOnZero ) {
        float lonFloat = preferences.getFloat(PREFS_KEY_MAPCENTER_LON, NOVALUE);
        float latFloat = preferences.getFloat(PREFS_KEY_MAPCENTER_LAT, NOVALUE);
        float zoom = preferences.getFloat(PREFS_KEY_MAP_ZOOM, 16f);
        double lon = (double) lonFloat / LibraryConstants.E6;
        double lat = (double) latFloat / LibraryConstants.E6;
        if (lon < NOVALUE_CHECKVALUE || lat < NOVALUE_CHECKVALUE) {
            if (backOnGps) {
                // try to get the last gps location
                double[] lastGpsLocation = getGpsLocationFromPreferences(preferences);
                if (lastGpsLocation != null) {
                    if (GPLog.LOG_ABSURD) {
                        GPLog.addLogEntry(LOG_TAG, "getMapCenter-fromgps: " + lastGpsLocation[0] + "/" + lastGpsLocation[1]);
                    }
                    return new double[]{lastGpsLocation[0], lastGpsLocation[1], zoom};
                } else {
                    if (backOnZero) {
                        // give up on 0,0
                        return new double[]{0.0, 0.0, zoom};
                    } else {
                        return null;
                    }
                }
            } else {
                return null;
            }
        }

        if (GPLog.LOG_ABSURD) {
            GPLog.addLogEntry(LOG_TAG, "getMapCenter-fromgps: " + lon + "/" + lat);
        }
        return new double[]{lon, lat, zoom};
    }

}
