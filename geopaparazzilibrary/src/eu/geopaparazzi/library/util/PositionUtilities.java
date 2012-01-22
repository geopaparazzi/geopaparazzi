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

import static eu.geopaparazzi.library.util.LibraryConstants.*;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_LAT;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_LON;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Position and preferences related utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PositionUtilities {

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
        Editor editor = preferences.edit();
        float longFloat = (float) longitude * LibraryConstants.E6;
        float latFloat = (float) latitude * LibraryConstants.E6;
        if (Debug.D) {
            Logger.d("POSITIONUTILITIES", "putGpsLocation: " + longFloat + "/" + latFloat);
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
        float lonFloat = preferences.getFloat(PREFS_KEY_LON, -9999f);
        float latFloat = preferences.getFloat(PREFS_KEY_LAT, -9999f);
        if (lonFloat < -9998f || latFloat < -9998f) {
            return null;
        }
        double lon = (double) lonFloat / LibraryConstants.E6;
        double lat = (double) latFloat / LibraryConstants.E6;
        if (Debug.D) {
            Logger.d("POSITIONUTILITIES", "getGpsLocation: " + lon + "/" + lat);
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
        if (Debug.D) {
            Logger.d("POSITIONUTILITIES", "putMapCenter: " + longFloat + "/" + latFloat);
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
     * @param backOnGpsAndZero if set to <code>true</code> and the map center was not set,
     *          it backs on the gps position. If that is also unknown, it returns 0,0. This
     *          allows for the result to never be <code>null</code>.
     * @return the array containing [lon, lat, zoom].
     */
    @SuppressWarnings("nls")
    public static double[] getMapCenterFromPreferences( SharedPreferences preferences, boolean backOnGpsAndZero ) {
        float lonFloat = preferences.getFloat(PREFS_KEY_MAPCENTER_LON, -9999f);
        float latFloat = preferences.getFloat(PREFS_KEY_MAPCENTER_LAT, -9999f);
        float zoom = preferences.getFloat(PREFS_KEY_MAP_ZOOM, 16f);
        if (lonFloat < -9998f || latFloat < -9998f) {
            if (backOnGpsAndZero) {
                // try to get the last gps location
                double[] lastGpsLocation = getGpsLocationFromPreferences(preferences);
                if (lastGpsLocation != null) {
                    if (Debug.D) {
                        Logger.d("POSITIONUTILITIES", "getMapCenter-fromgps: " + lastGpsLocation[0] + "/" + lastGpsLocation[1]);
                    }
                    return new double[]{lastGpsLocation[0], lastGpsLocation[1], zoom};
                } else {
                    // give up on 0,0
                    return new double[]{0.0, 0.0, zoom};
                }
            } else {
                return null;
            }
        }
        double lon = (double) lonFloat / LibraryConstants.E6;
        double lat = (double) latFloat / LibraryConstants.E6;
        if (Debug.D) {
            Logger.d("POSITIONUTILITIES", "getMapCenter-fromgps: " + lon + "/" + lat);
        }
        return new double[]{lon, lat, zoom};
    }

}
