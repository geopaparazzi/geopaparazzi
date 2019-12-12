/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

package eu.geopaparazzi.library.gpx.parser;

/**
 * Base class for Location aware points.
 */
public class LocationPoint {
    private double mLongitude;
    private double mLatitude;
    private boolean mHasElevation = false;
    private double mElevation;

    final void setLocation(double longitude, double latitude) {
        mLongitude = longitude;
        mLatitude = latitude;
    }

    /**
     * @return lon
     */
    public final double getLongitude() {
        return mLongitude;
    }

    /**
     * @return lat
     */
    public final double getLatitude() {
        return mLatitude;
    }

    final void setElevation(double elevation) {
        mElevation = elevation;
        mHasElevation = true;
    }

    /**
     * @return if <code>true</code> it has elevation.
     */
    public final boolean hasElevation() {
        return mHasElevation;
    }

    /**
     * @return elevation.
     */
    public final double getElevation() {
        return mElevation;
    }
}
