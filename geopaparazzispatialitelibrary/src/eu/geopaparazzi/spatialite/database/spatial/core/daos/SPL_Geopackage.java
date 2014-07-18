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

package eu.geopaparazzi.spatialite.database.spatial.core.daos;

import java.util.HashMap;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteDatabaseType;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Created by hydrologis on 18/07/14.
 */
public class SPL_Geopackage {
    /**
     * Return hasGeoPackage
     * - can AutoGPKGStart() and AutoGPKGStop() be used
     * -- VirtualGPKG
     * --- needed to retrieve/update GPKG geometry tables as if they are spatialite tables
     */
    public static boolean hasGeoPackage = false;

    /**
     * Read GeoPackage Revision 9 specific Databases
     *
     * @param database               the database to check.
     * @param spatialVectorMap       the {@link java.util.HashMap} of GeoPackage data (Features/Tiles) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link java.util.HashMap} of of invalid entries.
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static void getGeoPackageMap_R10(Database database, HashMap<String, String> spatialVectorMap,
                                             HashMap<String, String> spatialVectorMapErrors) throws Exception {
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_data = ""; // term used when building the sql
        String vector_extent = ""; // term used when building the sql
        String s_vgpkg = "vgpkg_";
        Stmt statement = null;
        try {
            statement = database.prepare(GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_INVALID_R10.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = statement.column_string(2);
                spatialVectorMapErrors.put(vector_key, vector_data + vector_extent);
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getGeoPackageMap_R10[" + SpatialiteDatabaseType.GEOPACKAGE + "] sql["
                    + GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_INVALID_R10.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        try {
            statement = database.prepare(GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_VALID_R10.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = "";
                vector_extent = statement.column_string(2);
                if (vector_extent != null) {
                    // geonames;geometry;GeoPackage_features;Geonames;Data
                    // from http://www.geonames.org/, under Creative
                    // Commons Attribution 3.0 License;
                    boolean b_valid = true;
                    if (vector_key.contains("GeoPackage_features")) {
                        b_valid = false;
                        String[] sa_string = vector_key.split(";");
                        if (sa_string.length == 5) {
                            String table_name = sa_string[0];
                            String geometry_column = sa_string[1];
                            String s_layer_type = sa_string[2];
                            String s_ROWID_PK = sa_string[3];
                            String s_view_read_only = sa_string[4];
                            HashMap<String, String> fieldNamesToTypeMap = DaoSpatialite.collectTableFields(database, s_vgpkg + table_name);
                            if (fieldNamesToTypeMap.size() > 0)
                                b_valid = true; // vgpkg_table-name exists
                            else { // only when AutoGPKGStart must be called
                                int i_count = spatialiteAutoGPKG(database, 0, SpatialiteDatabaseType.GEOPACKAGE);
                                if (i_count > 0) { // there must be at least 1 table
                                    fieldNamesToTypeMap = DaoSpatialite.collectTableFields(database, s_vgpkg + table_name);
                                    if (fieldNamesToTypeMap.size() > 0) { // vgpkg_table-name exists
                                        // ; AutoGPKGStart worked
                                        b_valid = true;
                                    }
                                }
                            }
                            if (b_valid) { // return spatialite VirtualGPKG table-name instead of
                                // geopackage table-name
                                vector_key = s_vgpkg + table_name + ";" + geometry_column + ";" + s_layer_type + ";"
                                        + s_view_read_only + ";";
                            }
                        }
                    }
                    if (b_valid) {
                        spatialVectorMap.put(vector_key, vector_data + vector_extent);
                    }
                } else { // should never happen
                    // GPLog.addLogEntry(-1,
                    // "DaoSpatialite:getGeoPackageMap_R10 -W-> vector_key[" + vector_key +
                    // "] vector_data["+ vector_data+"] vector_extent["+ vector_extent +
                    // "] GEOPACKAGE_QUERY_EXTENT_VALID_R10["+
                    // GEOPACKAGE_QUERY_EXTENT_VALID_R10 + "]");
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getGeoPackageMap_R10[" + SpatialiteDatabaseType.GEOPACKAGE + "] sql["
                    + GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_VALID_R10.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // GPLog.asd(-1,"getGeoPackageMap_R10["+database.getFilename()+"] spatialVectorMap["+spatialVectorMap.size()+"]  spatialVectorMapErrors["+spatialVectorMapErrors.size()+"] ");
    }

    /**
     * Attemt to create VirtualGPKG wrapper for GeoPackage geometry tables.
     * This function will inspect the DB layout,
     * - then automatically creating/refreshing a VirtualGPKG wrapper for each GPKG geometry table
     *
     * @param database     the db to use.
     * @param i_stop       0=AutoGPKGStart ; 1=AutoGPKGStop
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return returns amount of tables effected
     * @throws jsqlite.Exception if something goes wrong.
     */
    private static int spatialiteAutoGPKG(Database database, int i_stop, SpatialiteDatabaseType databaseType) throws jsqlite.Exception {
        int i_count_tables = 0;
        if (!hasGeoPackage)
            return i_count_tables;
        // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_AutoGPKG = "SELECT AutoGPKGStart();";
        if (i_stop == 1)
            s_AutoGPKG = "SELECT AutoGPKGStop();";
        Stmt statement = null;
        try {
            statement = database.prepare(s_AutoGPKG);
            if (statement.step()) {
                i_count_tables = statement.column_int(0);
                return i_count_tables;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteAutoGPKG[" + databaseType + "] sql[" + s_AutoGPKG + "] db[" + database.getFilename()
                    + "]", e_stmt);
        } finally {
            statement.close();
        }
        return i_count_tables;
    }
}
