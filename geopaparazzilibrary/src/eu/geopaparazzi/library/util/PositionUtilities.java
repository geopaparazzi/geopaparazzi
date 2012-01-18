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
    public static void putGpsLocationInPreferences( SharedPreferences preferences, double longitude, double latitude,
            double elevation ) {
        Editor editor = preferences.edit();
        editor.putFloat(PREFS_KEY_LON, (float) longitude * LibraryConstants.E6);
        editor.putFloat(PREFS_KEY_LAT, (float) latitude * LibraryConstants.E6);
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
    public static double[] getGpsLocationFromPreferences( SharedPreferences preferences ) {
        float lonFloat = preferences.getFloat(PREFS_KEY_LON, -9999f);
        float latFloat = preferences.getFloat(PREFS_KEY_LAT, -9999f);
        if (lonFloat < -9998f || latFloat < -9998f) {
            return null;
        }
        double lon = (double) lonFloat / LibraryConstants.E6;
        double lat = (double) latFloat / LibraryConstants.E6;
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
    public static void putMapCenterInPreferences( SharedPreferences preferences, double longitude, double latitude, float zoom ) {
        Editor editor = preferences.edit();
        editor.putFloat(PREFS_KEY_MAPCENTER_LON, (float) longitude * LibraryConstants.E6);
        editor.putFloat(PREFS_KEY_MAPCENTER_LAT, (float) latitude * LibraryConstants.E6);
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
    public static double[] getMapCenterFromPreferences( SharedPreferences preferences, boolean backOnGpsAndZero ) {
        float lonFloat = preferences.getFloat(PREFS_KEY_MAPCENTER_LON, -9999f);
        float latFloat = preferences.getFloat(PREFS_KEY_MAPCENTER_LAT, -9999f);
        float zoom = preferences.getFloat(PREFS_KEY_MAP_ZOOM, 16f);
        if (lonFloat < -9998f || latFloat < -9998f) {
            if (backOnGpsAndZero) {
                // try to get the last gps location
                double[] lastGpsLocation = getGpsLocationFromPreferences(preferences);
                if (lastGpsLocation != null) {
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
        return new double[]{lon, lat, zoom};
    }

}
