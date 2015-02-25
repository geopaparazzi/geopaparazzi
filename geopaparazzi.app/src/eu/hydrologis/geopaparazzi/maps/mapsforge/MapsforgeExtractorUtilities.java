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

package eu.hydrologis.geopaparazzi.maps.mapsforge;

import java.util.List;

import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import jsqlite.Database;

/**
 * Helper class for mapsforge data extraction.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgeExtractorUtilities {

    // supported ways
    public static final String tagHighway = "highway";
    public static final String tagRailway = "railway";
    public static final String tagRoute = "route"; // ex ferry
    public static final String tagAeroway = "aeroway"; // ex taxiway, terminal
    public static final String tagAerialway = "aerialway"; // ex gondola

    // supported waterlines
    public static final String tagWaterway = "waterway";

    // supported contours
    public static final String tagContours = "contour_ext";


    public static final String tagPoiElevation = "elev";

    public static final String TABLENAME_WAYS = "osm_roads";
    public static final String TABLENAME_WATERLINES = "osm_waterlines";
    public static final String TABLENAME_CONTOURS = "osm_contours";


    /**
     * Get the database for mapsforge extractions.
     *
     * @return the database.
     * @throws jsqlite.Exception
     */
    public static Database getDatabase() throws jsqlite.Exception {
        Database database = null;
        List<SpatialVectorTable> spatialVectorTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
        for (SpatialVectorTable spatialVectorTable : spatialVectorTables) {
            String uniqueNameBasedOnDbFilePath = spatialVectorTable.getUniqueNameBasedOnDbFilePath();
            if (uniqueNameBasedOnDbFilePath.startsWith(LibraryConstants.MAPSFORGE_EXTRACTED_DB_NAME)) {
                AbstractSpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(
                        spatialVectorTable);
                if (vectorHandler instanceof SpatialiteDatabaseHandler) {
                    SpatialiteDatabaseHandler spatialiteDatabaseHandler = (SpatialiteDatabaseHandler) vectorHandler;
                    database = spatialiteDatabaseHandler.getDatabase();
                }
            }
        }
        return database;
    }

    /**
     * Check if the tag is of a supported way type.
     *
     * @param key th etag key.
     * @return true, if supported.
     */
    public static boolean isWay(String key) {
        return
                key.equals(tagHighway) ||
                        key.equals(tagRailway) ||
                        key.equals(tagRoute) ||
                        key.equals(tagAerialway) ||
                        key.equals(tagAeroway);
    }

    /**
     * Check if the tag is of a supported waterlines type.
     *
     * @param key the tag key.
     * @return true, if supported.
     */
    public static boolean isWaterline(String key) {
        return
                key.equals(tagWaterway);
    }

    /**
     * Check if the tag is of a supported contour type.
     *
     * @param key the tag key.
     * @return true, if supported.
     */
    public static boolean isContour(String key) {
        return
                key.equals(tagContours);
    }

}
