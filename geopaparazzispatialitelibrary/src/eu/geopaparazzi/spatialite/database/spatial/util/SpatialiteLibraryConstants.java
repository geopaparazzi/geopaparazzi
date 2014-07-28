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
package eu.geopaparazzi.spatialite.database.spatial.util;

/**
 * Some constants used in the lib.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface SpatialiteLibraryConstants {

    /**
     * Key used to define a that text is passed through the workflow. Generic. 
     */
    public static final String PREFS_KEY_TEXT = "PREFS_KEY_TEXT"; //$NON-NLS-1$

    /**
     * Key used to define the spatialite recovery mode.
     */
    public static final String PREFS_KEY_SPATIALITE_RECOVERY_MODE = "PREFS_KEY_SPATIALITE_RECOVERY_MODE"; //$NON-NLS-1$

    /**
     * Key used to pass a lat temporarily through bundles. 
     */
    public static final String LATITUDE = "LATITUDE"; //$NON-NLS-1$

    /**
     * Key used to pass a lon temporarily through bundles. 
     */
    public static final String LONGITUDE = "LONGITUDE"; //$NON-NLS-1$

    /**
     * Key used to pass a position temporarily through bundles. 
     */
    public static final String POSITION = "POSITION"; //$NON-NLS-1$

}
