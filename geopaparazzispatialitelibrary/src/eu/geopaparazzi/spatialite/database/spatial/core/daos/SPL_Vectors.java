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

import static eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite.collectTableFields;

/**
 * Vector related database utils.
 *
 * @author Mark Johnson
 * @author Andrea Antonello
 */
public class SPL_Vectors implements ISpatialiteTableAndFieldsNames {
    public static VectorLayerQueryModes VECTORLAYER_QUERYMODE = VectorLayerQueryModes.STRICT;

    /**
     * Attemt to execute a UpdateLayerStatistics for this geometry field or whole Database.
     * - Note: only for AbstractSpatialTable, SpatialViews ALWAYS returns 0.
     * - Note: only for VirtualTable, returns 2.
     * - if table_name and geometry_column are empty: for whole Database
     *
     * @param database        the db to use.
     * @param table_name      the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param i_spatialindex  0=recover on when needed [default], 1=force rebuild.
     * @param databaseType    for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws jsqlite.Exception if something goes wrong.
     */
    private static int spatialiteUpdateLayerStatistics(Database database, String table_name, String geometry_column,
                                                       int i_spatialindex, SpatialiteDatabaseType databaseType) throws jsqlite.Exception {
        if (i_spatialindex == 1) {
            i_spatialindex = SpatialiteIndexing.spatialiteRecoverSpatialIndex(database, table_name, geometry_column, 0, databaseType);
            if (i_spatialindex == 0) {
              GPLog.addLogEntry("DAOSPATIALIE", "spatialiteUpdateLayerStatistics[" + databaseType
                                            + "] [spatialiteRecoverSpatialIndex failed] table_name[" + table_name + "] geometry_column[" + geometry_column + "]db[" + database.getFilename() + "]");
                return i_spatialindex; // Invalid for use with geopaparazzi
            }
        }
        i_spatialindex = 0;
        boolean b_valid = false;
        String s_UpdateLayerStatistics = "SELECT UpdateLayerStatistics();";
        String s_layer_statistics = "layer_statistics";
        if (databaseType == SpatialiteDatabaseType.SPATIALITE4)
         s_layer_statistics = "vector_layers_statistics";
        if ((!table_name.equals("")) && (!geometry_column.equals("")))
            s_UpdateLayerStatistics = "SELECT UpdateLayerStatistics('" + table_name + "','" + geometry_column + "');";
        Stmt statement = null;
        try {
            // when done here it, will catch sql-syntax errors
            statement = database.prepare(s_UpdateLayerStatistics);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                if (i_spatialindex == 1) {
                    HashMap<String, String> fieldNamesToTypeMap = collectTableFields(database, s_layer_statistics);
                    if (fieldNamesToTypeMap.size() > 0) { // AbstractSpatialTable virts_layer_statistics
                        b_valid = true;
                    } else {
                        fieldNamesToTypeMap = collectTableFields(database, "virts_layer_statistics");
                        if (fieldNamesToTypeMap.size() > 0) { // VirtualTable virts_layer_statistics
                            b_valid = true;
                            i_spatialindex = 2;
                        }
                    }
                    if (!b_valid) {
                        i_spatialindex = 0;
                     GPLog.addLogEntry("DAOSPATIALIE", "spatialiteUpdateLayerStatistics[" + databaseType
                                            + "] [no valid layer_statistics table found] table_name[" + table_name + "] geometry_column[" + geometry_column + "]db[" + database.getFilename() + "]");
                    }
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteUpdateLayerStatistics[" + databaseType + "] sql["
                    + s_UpdateLayerStatistics + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Attemt to execute a UpdateLayerStatistics for this geometry field and retrieve the bounds.
     * - if table_name and geometry_column are empty: for whole Database
     *
     * @param database        the db to use.
     * @param tableName      the table of the db to use.
     * @param geometryColumn the geometry field of the table to use.
     * @param i_spatialindex  check and try to recover the Spatial Index [0=no, 1=yes [default]].
     * @param databaseType    for Spatialite 3 and 4 specific Tasks
     * @return the retrieved bounds data, if possible (vector_extent).
     * @throws jsqlite.Exception if something goes wrong.
     */
    private static String getSpatialiteUpdateLayerStatistics(Database database, String tableName, String geometryColumn,
                                                             int i_spatialindex, SpatialiteDatabaseType databaseType) throws Exception {
        String s_vector_extent = "";
        if (DaoSpatialite.getGeometriesCount(database, tableName, geometryColumn) == 0) {
           GPLog.addLogEntry("DAOSPATIALIE", "getSpatialiteUpdateLayerStatistics[" + databaseType + "] error[getGeometriesCount == 0] db[" + database.getFilename() + "]");
            return s_vector_extent;
        }
        if (i_spatialindex == 1) {
            try {
                i_spatialindex = spatialiteUpdateLayerStatistics(database, tableName, geometryColumn, i_spatialindex,
                        databaseType);
            } finally {
            }
            if (i_spatialindex != 1) {
            GPLog.addLogEntry("DAOSPATIALIE", "getSpatialiteUpdateLayerStatistics[" + databaseType + "] error[UpdateLayerStatistic != 1 ]["+i_spatialindex+"] db[" + database.getFilename() + "]");
                return s_vector_extent; // Invalid for use with geopaparazzi
         }
        }
        // for table/geometry support, otherwise for whole Database (Spatialite3+4) try to retrieve the needed bounds again
        if ((!tableName.equals("")) && (!geometryColumn.equals(""))) {
            String s_LAYERS_QUERY_EXTENT_VALID = GeneralQueriesPreparer.VECTOR_LAYERS_QUERY_EXTENT_VALID_V4.getQuery();
            String s_METADATA_LAYERS_STATISTICS_TABLE_NAME = METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME;
            if (databaseType == SpatialiteDatabaseType.SPATIALITE3) { // for pre-Spatialite 4
                // versions
                s_LAYERS_QUERY_EXTENT_VALID = GeneralQueriesPreparer.LAYERS_QUERY_EXTENT_VALID_V3.getQuery();
                s_METADATA_LAYERS_STATISTICS_TABLE_NAME = METADATA_LAYER_STATISTICS_TABLE_NAME;
            }
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" AND ((");
            sb_query.append(s_METADATA_LAYERS_STATISTICS_TABLE_NAME + ".table_name='");
            sb_query.append(tableName);
            sb_query.append("') AND (" + s_METADATA_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column='");
            sb_query.append(geometryColumn);
            sb_query.append("'))");
            String VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
            // insert the extra WHERE condition into the prepaired sql
            VECTOR_LAYERS_QUERY_BASE = s_LAYERS_QUERY_EXTENT_VALID.replace("ORDER BY", VECTOR_LAYERS_QUERY_BASE + " ORDER BY");
            Stmt statement = null;
            try {
                // when done here it, will catch sql-syntax errors
                statement = database.prepare(VECTOR_LAYERS_QUERY_BASE);
                if (statement.step()) {
                    if (statement.column_string(2) != null) { // without further checking, consider
                        // this valid
                        s_vector_extent = statement.column_string(2);
                    }
                }
            } catch (jsqlite.Exception e_stmt) {
                GPLog.error("DAOSPATIALIE", "getSpatialiteUpdateLayerStatistics[" + databaseType + "] sql["
                        + VECTOR_LAYERS_QUERY_BASE + "] db[" + database.getFilename() + "]", e_stmt);
            } finally {
                statement.close();
            }
            // Last attempt, if this does not work - then the geometry must be considered invalid
            if (s_vector_extent.equals("")) {
                s_vector_extent = DaoSpatialite.getGeometriesBoundsString(database, tableName, geometryColumn);
             if (s_vector_extent.equals(""))
              GPLog.addLogEntry("DAOSPATIALIE", "getSpatialiteUpdateLayerStatistics[" + databaseType + "] error[GeometriesBoundsString empty] db[" + database.getFilename() + "]");
            }           
        }
        return s_vector_extent;
    }

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
    private static void getSpatialVectorMap_Errors(Database database, HashMap<String, String> spatialVectorMap,
                                                   HashMap<String, String> spatialVectorMapErrors, SpatialiteDatabaseType databaseType) throws Exception {
        HashMap<String, String> spatialVectorMapCorrections = new HashMap<String, String>();
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_data = ""; // term used when building the sql
        String vector_extent = ""; // term used when building the sql
        String vector_value = ""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name = "";
        String geometry_column = "";
        int i_spatialindex = 1;
        if ((VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT) && (spatialVectorMapErrors.size() > 0)) {
            for (Map.Entry<String, String> vector_entry : spatialVectorMapErrors.entrySet()) {
                vector_key = vector_entry.getKey();
                // soldner_polygon;14;3;2;3068;1;20847.6171111586,18733.613614603,20847.6171111586,18733.613614603
                // vector_key[priority_marks_joined_lincoln;geometry;AbstractSpatialTable;ROWID;-1]
                String recovery_text = "";
                vector_value = vector_entry.getValue();
                vector_data = "";
                String[] sa_string = vector_key.split(";");
                if (sa_string.length == 5) {
                    table_name = sa_string[0];
                    geometry_column = sa_string[1];
                    String s_layer_type = sa_string[2];
                    String s_ROWID_PK = sa_string[3];
                    int i_view_read_only = Integer.parseInt(sa_string[4]);
                    sa_string = vector_value.split(";");
                    if (sa_string.length == 7) { // vector_value[1;2;2913;1;row_count;extent_min_x,extent_min_y,extent_max_x,extent_max_y;last_verified]
                        String s_geometry_type = sa_string[0];
                        String s_coord_dimension = sa_string[1];
                        String s_srid = sa_string[2];
                        int i_spatial_index_enabled = Integer.parseInt(sa_string[3]); // should
                        // always be 1
                        // This should NOT be the default behavior, there may be a reason why no SpatialIndex was created
                        if ((i_spatial_index_enabled == 0) && (VECTORLAYER_QUERYMODE == VectorLayerQueryModes.CORRECTIVE || VECTORLAYER_QUERYMODE == VectorLayerQueryModes.CORRECTIVEWITHINDEX)) {
                            i_spatial_index_enabled = SpatialiteIndexing.spatialiteCreateSpatialIndex(database, table_name, geometry_column,
                                    databaseType);
                          if (!recovery_text.equals(""))
                           recovery_text+=",";
                          if (i_spatial_index_enabled==1)
                           recovery_text+="CreateSpatialIndex[corrected]";
                          else
                           recovery_text+="CreateSpatialIndex[failed]";
                        }
                        vector_data = s_geometry_type + ";" + s_coord_dimension + ";" + s_srid + ";" + i_spatial_index_enabled
                                + ";";
                        int i_row_count = -1;
                        if (!sa_string[4].equals("row_count"))
                            i_row_count = Integer.parseInt(sa_string[4]);
                        if (i_row_count == 0)
                        {
                         if (!recovery_text.equals(""))
                          recovery_text+=",";
                         recovery_text+="row_count=0";
                        }
                        String s_bounds = sa_string[5];
                        String s_last_verified = sa_string[6];
                        if (s_bounds.equals("extent_min_x,extent_min_y,extent_max_x,extent_max_y")) {
                            /*
                             *  if i_row_count == 0 :
                             *  -- then the table is empty - no need to do anything
                             *  if i_row_count > 0 :
                             *  -- then the geometries of this column are NULL  - no need to do anything
                             * --- OR UpdateLayerStatistics has not been called
                            */
                            // if (i_row_count > 0)
                            // i_row_count = 0;
                        }
                        /*
                         *  if i_row-count == -1 OR
                         *  one of the values of s_bounds is a number,
                         * -- then UpdateLayerStatistics is needed
                         * */
                        if (i_spatial_index_enabled == 0)
                            i_row_count = 0;
                        if (i_row_count != 0) {
                            i_spatialindex = 1;
                            if (vector_key.contains("SpatialView")) {
                                i_spatialindex = 0;
                            }
                            // we do not try to query the dementions of faulty SpatialView's
                            if ((VECTORLAYER_QUERYMODE == VectorLayerQueryModes.TOLERANT) && (i_spatialindex == 1)) {
                                vector_extent = DaoSpatialite.getGeometriesBoundsString(database, table_name, geometry_column);
                             if (!recovery_text.equals(""))
                              recovery_text+=",";
                             if (!vector_extent.equals(""))
                              recovery_text+="GeometriesBoundsString[corrected]";
                             else
                              recovery_text+="GeometriesBoundsString[failed]";
                            }
                            if (VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT && VECTORLAYER_QUERYMODE != VectorLayerQueryModes.TOLERANT) {
                                    /* RecoverSpatialIndex will be done if needed
                                     UpdateLayerStatistics will then be called
                                     afterwhich 2 attemts will be made to return valid result
                                     - if empty: geometry is to be considered invalid
                                    */
                                vector_extent = getSpatialiteUpdateLayerStatistics(database, table_name, geometry_column,
                                        i_spatialindex, databaseType);
                             if (!recovery_text.equals(""))
                              recovery_text+=",";
                             if (!vector_extent.equals(""))
                              recovery_text+="UpdateLayerStatistics[corrected]";
                             else
                              recovery_text+="UpdateLayerStatistics[failed]";
                            }
                        }
                        if (!vector_extent.equals("")) { // all of the geomtries of this column may
                            // be NULL, thus unusable - do not add when
                            // 'vector_extent' is empty
                            if (vector_key.contains("SpatialView")) {
                                try { // replace placeholder with used primary-key and read_only
                                    // parameter of SpatialView
                                    String ROWID_PK = SPL_Views.getViewRowid(database, table_name, databaseType);
                                    vector_key = vector_key.replace("ROWID;-1", ROWID_PK);
                                } catch (Exception e) {
                                    GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_Errors[" + databaseType
                                            + "] vector_key[" + vector_key + "] db[" + database.getFilename() + "]", e);
                                }
                            }
                            // one way or another, we have resolved the faulty geometry, add to the
                            // valid list
                            spatialVectorMap.put(vector_key, vector_data + vector_extent);
                            if (VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT && VECTORLAYER_QUERYMODE != VectorLayerQueryModes.TOLERANT) { // remove from the errors, since
                                // they may have been permanently
                                // resolved, but not here    
                                spatialVectorMapCorrections.put(vector_entry.getKey(), vector_entry.getValue());
                            }
                        } else {
                            // GPLog.asd(-1,"getSpatialVectorMap_Errors[not resolved]["+VECTOR_LAYERS_QUERY_MODE+"]  vector_key["+vector_key+"]  vector_value["+vector_value+"] vector_extent["+vector_extent+"]");
                        }
                        GPLog.addLogEntry("DAOSPATIALIE", "getSpatialVectorMap_Errors[" + databaseType
                                            + "] ["+recovery_text+"] vector_key[" + vector_key + "] db[" + database.getFilename() + "]");
                    }
                }
            }
            // remove from the errors, since they may have been permanently resolved
            // hopefully arrivederci to the 'Error: null' alerts
            for (Map.Entry<String, String> vector_entry : spatialVectorMapCorrections.entrySet()) {
                try {
                    spatialVectorMapErrors.remove(vector_entry.getKey());
                } catch (java.lang.Exception e) {
                    GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_Errors[" + databaseType + "] vector_key[" + vector_key
                            + "] db[" + database.getFilename() + "]", e);
                }
            }
        }
    }

    /**
     * Read Spatial-Geometries for pre-Spatialite 4.* specific Databases (2.4.0-3.1.0)
     *
     * @param database               the database to check.
     * @param spatialVectorMap       the {@link HashMap} of Spatialite4+ Vector-data (Views/Tables Geometries) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid geometries.
     * @param b_layers_statistics    if a layers_statistics had been found
     * @return nothing
     * @throws Exception if something goes wrong.
     */
    static void getSpatialVectorMap_V3(Database database, HashMap<String, String> spatialVectorMap,
                                       HashMap<String, String> spatialVectorMapErrors, boolean b_layers_statistics, boolean b_SpatialIndex)
            throws Exception {
        int i_spatialindex = 0;
        if (!b_SpatialIndex) { // pre-spatilite 3.0 Database may not have this Virtual-Table, it
            // must be created to query the geometrys using the SpatialIndex
            i_spatialindex = SpatialiteIndexing.spatialiteVirtualSpatialIndex(database, SpatialiteDatabaseType.SPATIALITE3);
            if (i_spatialindex == 0) { // if this fails then we may have to consider this Database
                // invalid
                return;
            }
        }
        if (!b_layers_statistics) { // if layers_statistics does not exist a UpdateLayerStatistics()
            // is needed for the whole Database
            i_spatialindex = spatialiteUpdateLayerStatistics(database, "", "", i_spatialindex, SpatialiteDatabaseType.SPATIALITE3);
            if (i_spatialindex != 1) { // if this fails then we may have to consider this Database
                // invalid
                return;
            }
        }
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_data = ""; // term used when building the sql
        String vector_extent = ""; // term used when building the sql
        String vector_value = ""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name = "";
        String geometry_column = "";
        String[] sa_string;
        // for pre-Spatialite : Views and Table must be done in 2 steps
        // First: Views
        Stmt statement = null;
        try {
            statement = database.prepare(GeneralQueriesPreparer.VIEWS_QUERY_EXTENT_INVALID_V3.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = statement.column_string(2);
                spatialVectorMapErrors.put(vector_key, vector_data + vector_extent);
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V3[" + SpatialiteDatabaseType.SPATIALITE3 + "] sql["
                    + GeneralQueriesPreparer.VIEWS_QUERY_EXTENT_INVALID_V3.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // Second: Tables
        try {
            statement = database.prepare(GeneralQueriesPreparer.LAYERS_QUERY_EXTENT_INVALID_V3.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = statement.column_string(2);
                spatialVectorMapErrors.put(vector_key, vector_data + vector_extent);
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V3[" + SpatialiteDatabaseType.SPATIALITE3 + "] sql["
                    + GeneralQueriesPreparer.LAYERS_QUERY_EXTENT_INVALID_V3.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // First Views
        try {
            statement = database.prepare(GeneralQueriesPreparer.VIEWS_QUERY_EXTENT_VALID_V3.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = "";
                if (vector_key.contains("SpatialView")) { // berlin_1000;map_linestring;SpatialView;ROWID
                    // Do not call RecoverSpatialIndex
                    // for SpatialViews
                    sa_string = vector_key.split(";");
                    if (sa_string.length == 5) {
                        table_name = sa_string[0];
                        // replace placeholder with used primary-key and read_only parameter
                        // of SpatialView
                        String ROWID_PK = SPL_Views.getViewRowid(database, table_name, SpatialiteDatabaseType.SPATIALITE3);
                        vector_key = vector_key.replace("ROWID;-1", ROWID_PK);
                    }
                }
                vector_extent = statement.column_string(2);
                if (vector_extent != null) {
                    spatialVectorMap.put(vector_key, vector_data + vector_extent);
                } else { // should never happen
                    // GPLog.asd(-1,
                    // "-E-> getSpatialVectorMap_V3 vector_key[" + vector_key +
                    // "] vector_data["+ vector_data+"] vector_extent["+ vector_extent +
                    // "] VIEWS_QUERY_EXTENT_VALID_V3["+ VIEWS_QUERY_EXTENT_VALID_V3 + "]");
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V3[" + SpatialiteDatabaseType.SPATIALITE3 + "] sql["
                    + GeneralQueriesPreparer.VIEWS_QUERY_EXTENT_VALID_V3.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // Second Tables
        try {
            statement = database.prepare(GeneralQueriesPreparer.LAYERS_QUERY_EXTENT_VALID_V3.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = "";
                vector_extent = statement.column_string(2);
                if (vector_extent != null) {
                    spatialVectorMap.put(vector_key, vector_data + vector_extent);
                } else { // should never happen
                    // GPLog.asd(-1,
                    // "-E-> getSpatialVectorMap_V3 vector_key[" + vector_key +
                    // "] vector_data["+ vector_data+"] vector_extent["+ vector_extent +
                    // "] LAYERS_QUERY_EXTENT_VALID_V3["+ LAYERS_QUERY_EXTENT_VALID_V3 + "]");
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V3[" + SpatialiteDatabaseType.SPATIALITE3 + "] sql["
                    + GeneralQueriesPreparer.LAYERS_QUERY_EXTENT_VALID_V3.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // if empty: there are nothing to correct
        if ((VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT) && (spatialVectorMapErrors.size() > 0)) {
            getSpatialVectorMap_Errors(database, spatialVectorMap, spatialVectorMapErrors, SpatialiteDatabaseType.SPATIALITE3);
        }
    }


    /**
     * Read Spatial-Geometries for Spatialite 4.* specific Databases
     *
     * @param database               the database to check.
     * @param spatialVectorMap       the {@link HashMap} of Spatialite4+ Vector-data (Views/Tables Geometries) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid geometries.
     * @param b_layers_statistics    if a layers_statistics had been found
     * @param b_raster_coverages     if a raster_coverages had been found [RasterLite2 support]
     * @throws Exception if something goes wrong.
     */
    static void getSpatialVectorMap_V4(Database database, HashMap<String, String> spatialVectorMap,
                                       HashMap<String, String> spatialVectorMapErrors, boolean b_layers_statistics, boolean b_raster_coverages)
            throws Exception {
        String vector_key = ""; // term used when building the sql, used as map.key
        String vector_data = ""; // term used when building the sql
        String vector_extent = ""; // term used when building the sql
        String vector_value = ""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name = "";
        String geometry_column = "";
        String[] sa_string;
        Stmt statement = null;
        try {
            statement = database.prepare(GeneralQueriesPreparer.VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = statement.column_string(2);
                spatialVectorMapErrors.put(vector_key, vector_data + vector_extent);
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V4[" + SpatialiteDatabaseType.SPATIALITE4 + "] sql["
                    + GeneralQueriesPreparer.VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        try {
            statement = database.prepare(GeneralQueriesPreparer.VECTOR_LAYERS_QUERY_EXTENT_VALID_V4.getQuery());
            while (statement.step()) {
                vector_key = statement.column_string(0);
                vector_data = statement.column_string(1);
                vector_extent = "";
                if (vector_key.contains("SpatialView")) { // berlin_1000;map_linestring;SpatialView;ROWID
                    // Do not call RecoverSpatialIndex
                    // for SpatialViews
                    sa_string = vector_key.split(";");
                    if (sa_string.length == 5) {
                        table_name = sa_string[0];
                        try { // replace placeholder with used primary-key and read_only parameter
                            // of SpatialView
                            String ROWID_PK = SPL_Views.getViewRowid(database, table_name, SpatialiteDatabaseType.SPATIALITE4);
                            vector_key = vector_key.replace("ROWID;-1", ROWID_PK);
                        } finally {
                        }
                    }
                }
                vector_extent = statement.column_string(2);
                if (vector_extent != null) {
                    spatialVectorMap.put(vector_key, vector_data + vector_extent);
                } else { // should never happen
                    // GPLog.addLogEntry("getSpatialVectorMap_V4 vector_key["
                    // + vector_key + "] vector_data["+ vector_data+"] vector_extent["+
                    // vector_extent + "] VECTOR_LAYERS_QUERY["+
                    // VECTOR_LAYERS_QUERY_EXTENT_VALID_V4 + "]");
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V4[" + SpatialiteDatabaseType.SPATIALITE4 + "] sql["
                    + GeneralQueriesPreparer.VECTOR_LAYERS_QUERY_EXTENT_VALID_V4.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        // if empty: there are nothing to correct [do before RasterLite2 logic - there is no error control for that]
        if ((VECTORLAYER_QUERYMODE != VectorLayerQueryModes.STRICT) && (spatialVectorMapErrors.size() > 0)) {
            getSpatialVectorMap_Errors(database, spatialVectorMap, spatialVectorMapErrors, SpatialiteDatabaseType.SPATIALITE4);
        }
        // RasterLite2 support: a raster_coverages has been found and the driver supports it
        if ((!SPL_Rasterlite.Rasterlite2Version_CPU.equals("")) && (b_raster_coverages)) {
            try {
                statement = database.prepare(GeneralQueriesPreparer.RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42.getQuery());
                while (statement.step()) {
                    vector_key = statement.column_string(0);
                    vector_data = statement.column_string(1);
                    vector_extent = statement.column_string(2);
                    spatialVectorMapErrors.put(vector_key, vector_data + vector_extent);
                }
            } catch (jsqlite.Exception e_stmt) {
                GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V4[" + SpatialiteDatabaseType.SPATIALITE4 + "] sql["
                        + GeneralQueriesPreparer.RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
            try {
                statement = database.prepare(GeneralQueriesPreparer.RASTER_COVERAGES_QUERY_EXTENT_VALID_V42.getQuery());
                while (statement.step()) {
                    vector_key = statement.column_string(0);
                    vector_data = statement.column_string(1);
                    vector_extent = "";
                    vector_extent = statement.column_string(2);
                    if (vector_extent != null) { // mj10777: for some reason, this is being filled
                        // twice
                        spatialVectorMap.put(vector_key, vector_data + vector_extent);
                    } else { // should never happen
                        // GPLog.asd(-1,
                        // "getSpatialVectorMap_V4 vector_key[" + vector_key +
                        // "] vector_data["+ vector_data+"] vector_extent["+ vector_extent +
                        // "] RASTER_COVERAGES_QUERY["+ RASTER_COVERAGES_QUERY_EXTENT_VALID_V42
                        // + "]");
                    }
                }
            } catch (jsqlite.Exception e_stmt) {
                GPLog.error("DAOSPATIALIE", "getSpatialVectorMap_V4[" + SpatialiteDatabaseType.SPATIALITE4 + "] sql["
                        + GeneralQueriesPreparer.RASTER_COVERAGES_QUERY_EXTENT_VALID_V42.getQuery() + "] db[" + database.getFilename() + "]", e_stmt);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    }
}
