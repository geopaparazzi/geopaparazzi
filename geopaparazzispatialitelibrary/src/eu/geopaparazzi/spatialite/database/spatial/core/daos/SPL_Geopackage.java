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
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.VectorLayerQueryModes;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteDatabaseType;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Created by hydrologis on 18/07/14.
 */
public class SPL_Geopackage {
    public static final String LOGTAG = "SPL_GEOPACKAGE";
    public static VectorLayerQueryModes VECTORLAYER_QUERYMODE = VectorLayerQueryModes.STRICT;
    /**
     * Return hasGeoPackage
     * - can AutoGPKGStart() and AutoGPKGStop() be used
     * -- VirtualGPKG
     * --- needed to retrieve/update GPKG geometry tables as if they are spatialite tables
     */
    public static boolean hasGeoPackage = false;
    /**
     * Attemt to correction of geometries in error .
     * - if table_name and geometry_column are empty: for whole Database
     *
     * @param database               the database to check.
     * @param spatialVectorMap       the {@link HashMap} of Spatialite4+ Vector-data (Views/Tables Geometries) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid geometries.
     * @param databaseType           for Spatialite 3 and 4 specific Tasks
     * @throws Exception if something goes wrong.
     */
    private static void getGeoPackageMap_R10_Errors(Database dbSpatialite, HashMap<String, String> spatialVectorMap,
                                                   HashMap<String, String> spatialVectorMapErrors, SpatialiteDatabaseType databaseType) throws Exception {
        HashMap<String, String> spatialVectorMapCorrections = new HashMap<String, String>();
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_data = ""; // term used when building the sql
        String vector_extent = ""; // term used when building the sql
        String vector_value = ""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name = "";
        String style_name = "";
        String geometry_column = "";
        int spatialIndex = 1;
        if ((VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT) && (spatialVectorMapErrors.size() > 0)) {
            for (Map.Entry<String, String> vector_entry : spatialVectorMapErrors.entrySet()) {
                vector_key = vector_entry.getKey();
                // geometryzm_gpkg;geometry;GeoPackage_features;geometryzm_gpkg;;
                // vector_key[geometryzm_gpkg;geometry;GeoPackage_features;geometryzm_gpkg;;;0;2;4326;0;;-1;min_x,min_y,max_x,max_y;2015-04-22T14:58:01.000Z]
                String recovery_text = "";
                vector_value = vector_entry.getValue();
                String[] sa_string = vector_key.split(";");
                if (sa_string.length == 5) {
                    table_name = sa_string[0];
                    geometry_column = sa_string[1];
                    sa_string = vector_value.split(";");
                    if (sa_string.length == 7) { // vector_value[0;2;4326;0;;-1;min_x,min_y,max_x,max_y;2015-04-22T14:58:01.000Z]
                        String s_geometry_type = sa_string[0];
                        String s_coord_dimension = sa_string[1];
                        String s_srid = sa_string[2];
                        int i_spatial_index_enabled = Integer.parseInt(sa_string[3]); // 0
                        vector_data = s_geometry_type + ";" + s_coord_dimension + ";" + s_srid + ";" + i_spatial_index_enabled
                                + ";";
                        int i_row_count = -1;
                        // GeoPackage: has no row_count(-1)
                       String s_bounds = sa_string[5];
                       //SELECT count(geometry)||';'||Min(MbrMinX(geometry))||','||Min(MbrMinY(geometry))||','||Max(MbrMaxX(geometry))||','||Max(MbrMaxY(geometry))||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') FROM geometry_gpkg;
                       if (s_bounds.equals("extent_min_x,extent_min_y,extent_max_x,extent_max_y")) 
                       {
                         if ((VECTORLAYER_QUERYMODE == VectorLayerQueryModes.TOLERANT) && (spatialIndex == 1)) 
                         {
                                vector_extent = DaoSpatialite.getGeometriesBoundsString(dbSpatialite, table_name, geometry_column);
                                if (!recovery_text.equals(""))
                                    recovery_text += ",";
                                if (!vector_extent.equals(""))
                                {
                                    sa_string = vector_extent.split(";");
                                    if (sa_string.length == 5) 
                                    {
                                     String s_min_x = sa_string[0];
                                     String s_min_y = sa_string[1];
                                     String s_max_x = sa_string[0];
                                     String s_max_y = sa_string[1];
                                     StringBuilder sb_sql = new StringBuilder();
                                     sb_sql.append("UPDATE 'gpkg_contents' SET  ");
                                     sb_sql.append("min_x=").append(s_min_x).append(",");
                                     sb_sql.append("min_y=").append(s_min_y).append(",");
                                     sb_sql.append("max_x=").append(s_max_x).append(",");
                                     sb_sql.append("max_y=").append(s_max_y).append(" ");
                                     sb_sql.append("WHERE table_name='").append(table_name).append("'");
                                     String s_sql=sb_sql.toString();
                                     dbSpatialite.exec(s_sql, null);
                                     recovery_text += "GeometriesBoundsString[corrected]";
                                     spatialVectorMap.put(vector_key, vector_data + vector_extent);
                                     if (VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT && VECTORLAYER_QUERYMODE != VectorLayerQueryModes.TOLERANT) 
                                     { // remove from the errors, since they may have been permanently resolved, but not here    
                                      spatialVectorMapCorrections.put(vector_entry.getKey(), vector_entry.getValue());
                                     }
                                   }                                    
                                }
                                else
                                    recovery_text += "GeometriesBoundsString[failed]";
                        }
                       }
                        GPLog.addLogEntry(LOGTAG, "getGeoPackageMap_R10_Errors[" + databaseType
                                + "] [" + recovery_text + "] vector_key[" + vector_key + "] db[" + dbSpatialite.getFilename() + "]");
                    }
                }
            }
            // remove from the errors, since they may have been permanently resolved
            // hopefully arrivederci to the 'Error: null' alerts
            for (Map.Entry<String, String> vector_entry : spatialVectorMapCorrections.entrySet()) {
                try {
                    spatialVectorMapErrors.remove(vector_entry.getKey());
                } catch (java.lang.Exception e) {
                    GPLog.error(LOGTAG, "getGeoPackageMap_R10_Errors[" + databaseType + "] vector_key[" + vector_key
                            + "] db[" + dbSpatialite.getFilename() + "]", e);
                }
            }
        }
    }
    /**
     * Read GeoPackage Revision 10 specific Databases
     *
     * @param database               the database to check.
     * @param spatialVectorMap       the {@link java.util.HashMap} of GeoPackage data (Features/Tiles) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link java.util.HashMap} of of invalid entries.
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static void getGeoPackageMap_R10(Database dbSpatialite, HashMap<String, String> spatialVectorMap,
                                            HashMap<String, String> spatialVectorMapErrors) throws Exception {
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_data = ""; // term used when building the sql
        String vector_extent = ""; // term used when building the sql
        String s_vgpkg = "vgpkg_";
        Stmt statement = null;
        try {
            statement = dbSpatialite.prepare(GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_INVALID_R10.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = statement.column_string(2);
                spatialVectorMapErrors.put(vector_key, vector_data + vector_extent);
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getGeoPackageMap_R10[" + SpatialiteDatabaseType.GEOPACKAGE + "] sql["
                    + GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_INVALID_R10.getQuery() + "] db[" + dbSpatialite.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        if ((VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT) && (spatialVectorMapErrors.size() > 0)) {
            getGeoPackageMap_R10_Errors(dbSpatialite, spatialVectorMap, spatialVectorMapErrors, SpatialiteDatabaseType.GEOPACKAGE);
        }  
        try {
            statement = dbSpatialite.prepare(GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_VALID_R10.getQuery());
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
                            HashMap<String, String> fieldNamesToTypeMap = DaoSpatialite.collectTableFields(dbSpatialite, s_vgpkg + table_name);
                            if (fieldNamesToTypeMap.size() > 0)
                                b_valid = true; // vgpkg_table-name exists
                            else { // only when AutoGPKGStart must be called
                                int i_count = spatialiteAutoGPKG(dbSpatialite, 0, SpatialiteDatabaseType.GEOPACKAGE);
                                if (i_count > 0) { // there must be at least 1 table
                                    fieldNamesToTypeMap = DaoSpatialite.collectTableFields(dbSpatialite, s_vgpkg + table_name);
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
                    + GeneralQueriesPreparer.GEOPACKAGE_QUERY_EXTENT_VALID_R10.getQuery() + "] db[" + dbSpatialite.getFilename() + "]", e_stmt);
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
    private static int spatialiteAutoGPKG(Database dbSpatialite, int i_stop, SpatialiteDatabaseType databaseType) throws jsqlite.Exception {
        int i_count_tables = 0;
        if (!hasGeoPackage)
            return i_count_tables;
        // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_AutoGPKG = "SELECT AutoGPKGStart();";
        if (i_stop == 1)
            s_AutoGPKG = "SELECT AutoGPKGStop();";
        Stmt statement = null;
        try {
            statement = dbSpatialite.prepare(s_AutoGPKG);
            if (statement.step()) {
                i_count_tables = statement.column_int(0);
                return i_count_tables;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteAutoGPKG[" + databaseType + "] sql[" + s_AutoGPKG + "] db[" + dbSpatialite.getFilename()
                    + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_count_tables;
    }
}
