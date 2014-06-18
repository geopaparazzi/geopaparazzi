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
package eu.hydrologis.geopaparazzi.maptools;

import java.util.ArrayList;
import java.util.List;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.util.DataType;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialiteDatabaseHandler;

/**
 * A spatial feature container.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FeatureUtilities {

    /**
     * Key to pass featuresLists through activities.
     */
    public static final String KEY_FEATURESLIST = "KEY_FEATURESLIST"; //$NON-NLS-1$

    /**
     * Key to pass a readonly flag through activities.
     */
    public static final String KEY_READONLY = "KEY_READONLY";

    /**
     * Build the features given by a query.
     * 
     * <b>Note that it is mandatory that the first item of the 
     * query is the id of the feature, which can be used at any time
     * to update the feature in the db. 
     * 
     * @param query the query to run.
     * @param spatialTable the parent Spatialtable.
     * @return the list of feature from the query. 
     * @throws Exception is something goes wrong.
     */
    public static List<Feature> build( String query, SpatialVectorTable spatialTable ) throws Exception {
        List<Feature> featuresList = new ArrayList<Feature>();
        SpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(spatialTable);
        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
            SpatialiteDatabaseHandler spatialiteDbHandler = (SpatialiteDatabaseHandler) vectorHandler;
            Database database = spatialiteDbHandler.getDatabase();

            String tableName = spatialTable.getTableName();
            String uniqueNameBasedOnDbFilePath = spatialTable.getUniqueNameBasedOnDbFilePath();

            Stmt stmt = database.prepare(query);
            try {
                while( stmt.step() ) {
                    int column_count = stmt.column_count();
                    // the first is the id, transparent to the user
                    String id = stmt.column_string(0);
                    Feature feature = new Feature(tableName, uniqueNameBasedOnDbFilePath, id);
                    for( int i = 1; i < column_count; i++ ) {
                        String cName = stmt.column_name(i);
                        String value = stmt.column_string(i);
                        int columnType = stmt.column_type(i);
                        DataType type = DataType.getType4SqliteCode(columnType);
                        feature.addAttribute(cName, value, type.name());
                    }
                    featuresList.add(feature);
                }
            } finally {
                stmt.close();
            }

        }
        return featuresList;
    }

    /**
     * Build the features given by a query.
     * 
     * <b>Note that this query needs to have 2 arguments, the first
     * being the ROWID and the second the geometry. Else if will fail. 
     * 
     * @param query the query to run.
     * @param spatialTable the parent Spatialtable.
     * @return the list of feature from the query. 
     * @throws Exception is something goes wrong.
     */
    public static List<Feature> buildRowidGeometryFeatures( String query, SpatialVectorTable spatialTable ) throws Exception {
        List<Feature> featuresList = new ArrayList<Feature>();
        SpatialDatabaseHandler vectorHandler = SpatialDatabasesManager.getInstance().getVectorHandler(spatialTable);
        if (vectorHandler instanceof SpatialiteDatabaseHandler) {
            SpatialiteDatabaseHandler spatialiteDbHandler = (SpatialiteDatabaseHandler) vectorHandler;
            Database database = spatialiteDbHandler.getDatabase();
            String tableName = spatialTable.getTableName();
            String uniqueNameBasedOnDbFilePath = spatialTable.getUniqueNameBasedOnDbFilePath();

            Stmt stmt = database.prepare(query);
            try {
                while( stmt.step() ) {
                    int column_count = stmt.column_count();
                    if (column_count != 2) {
                        throw new IllegalArgumentException("This query should return ROWID and Geometry: " + query);
                    }
                    // the first is the id, transparent to the user
                    String id = stmt.column_string(0);
                    byte[] geometryBytes = stmt.column_bytes(1);
                    Feature feature = new Feature(tableName, uniqueNameBasedOnDbFilePath, id, geometryBytes);
                    featuresList.add(feature);
                }
            } finally {
                stmt.close();
            }
        }
        return featuresList;
    }
}
