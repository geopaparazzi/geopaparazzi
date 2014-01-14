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

import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * Helper class for screen matters. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ScreenHelper {

    private static ScreenHelper screenHelper;
    private final Activity activity;

    private ScreenHelper( Activity activity ) {
        this.activity = activity;
    }

    /**
     * @param activity activity.
     * @return the singleton instance.
     * @throws Exception  if something goes wrong.
     */
    public static ScreenHelper getInstance( Activity activity ) throws Exception {
        if (screenHelper == null) {
            screenHelper = new ScreenHelper(activity);
        }
        return screenHelper;
    }

    /**
     * Gets the density of the screen.
     * 
     * Can be checked with:
     * <code>
     *  switch( metrics.densityDpi ) {
     *   case DisplayMetrics.DENSITY_LOW:
     *       break;
     *   case DisplayMetrics.DENSITY_MEDIUM:
     *       break;
     *   case DisplayMetrics.DENSITY_HIGH:
     *       break;
     *   }
     * </code>
     * 
     * @return the density of the screen.
     */
    public int getDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.densityDpi;
    }

}
