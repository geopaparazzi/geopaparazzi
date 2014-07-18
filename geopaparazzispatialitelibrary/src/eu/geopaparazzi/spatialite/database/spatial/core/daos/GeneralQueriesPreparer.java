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

/**
 * General sql query to retrieve vector data of the whole Database in 1 query
 * <p/>
 * - this is Spatialite4+ specfic and will be called in checkDatabaseTypeAndValidity
 * - invalid entries are filtered out (row_count > 0 and min/max x+y NOT NULL)
 * -- and will returned spatialVectorMap with the 2 Fields returned by this query
 * -- the result will be sorted with views first and Tables second
 * - Field-Names and use:
 * -- 'vector_key'   : fields often needed and used in map.key [always valid]
 * -- 'vector_data'  : fields NOT often needed and used in map.value [first portion and always valid]
 * -- 'vector_extent': fields NOT often needed and used in map.value [second portion and NOT always valid]
 * <p/>
 * Queries for Spatialite (all versions) at:
 * https://github.com/mj10777/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific
 * <p/>
 * Queries for RasterLite2 at:
 * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RASTER_COVERAGES_QUERYS-geopaparazzi-specific
 * <p/>
 * Queries for GeoPackage R10 at:
 * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/GEOPACKAGE_QUERY_R10-geopaparazzi-specific
 * <p/>
 * <ol>
 * <li>3 Fields will be returned with the following structure</li>
 * <li>0 table_name: berlin_stadtteile</li>
 * <li>1: geometry_column - soldner_polygon</li>
 * <li>2: layer_type - SpatialView or AbstractSpatialTable</li>
 * <li>3: ROWID - AbstractSpatialTable: default ; when SpatialView or will be replaced</li>
 * <li>4: view_read_only - AbstractSpatialTable: -1 ; when SpatialView: 0=read_only or 1 writable</li>
 * <li>vector_data: Seperator: ';' 7 values</li>
 * <li>0: geometry_type - 3</li>
 * <li>1: coord_dimension - 2</li>
 * <li>2: srid - 3068</li>
 * <li>3: spatial_index_enabled - 0 or 1</li>
 * <li>4: rows 4
 * <li>5: extent_min/max - Seperator ',' - 4 values
 * <li>5.1:extent_min_x - 20847.6171111586</li>
 * <li>5.2:extent_min_y - 18733.613614603</li>
 * <li>5.3:extent_max_x - 20847.6171111586</li>
 * <li>5.4:extent_max_y - 18733.613614603</li></li>
 * <li>6:last_verified - 2014-03-12T12:22:39.688Z</li>
 * </ol>
 * <p/>
 * Validity: s_vector_key.split(";"); must return the length of 5
 * <p/>
 * Validity: s_vector_data.split(";"); must return the length of 7
 * <p/>
 * sa_vector_data[5].split(","); must return the length of 4
 *
 * @author Mark Johnson
 * @author Andrea Antonello - refactoring to enum
 */
public enum GeneralQueriesPreparer implements ISpatialiteTableAndFieldsNames {

    /**
     * The Sql-String to retrieve valid Vector-Layers from a Spatialite 4 Database.
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#VECTOR_LAYERS_QUERY_EXTENT_VALID_V4
     */
    VECTOR_LAYERS_QUERY_EXTENT_VALID_V4,
    /**
     * The Sql-String to retrieve invalid Vector-Layers from a Spatialite 4 Database.
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4
     */
    VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4,
    /**
     * The Sql-String to retrieve a minimal information on a valid or invalid Vector-Layers from a Spatialite 4 Database.
     * <p/>
     * Used for debugging.
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#VECTOR_LAYERS_QUERY_EXTENT_LIST_V4
     */
    VECTOR_LAYERS_QUERY_EXTENT_LIST_V4,

    /**
     * The Sql-String to retrieve valid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - that may have been changed with a Spatialite 4 software<br/>
     * - for spatialite 4.0 with non-working vector_layers_statistics, but still has a valid layers_statistics table<br/>
     * - This should only be needed for cases where `UpdateLayerStatistics` has failed
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#LAYERS_QUERY_EXTENT_VALID_V4
     */
    LAYERS_QUERY_EXTENT_VALID_V4,
    /**
     * The Sql-String to retrieve invalid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - that may have been changed with a Spatialite 4 software<br/>
     * - for spatialite 4.0 with non-working vector_layers_statistics, but still has a valid layers_statistics table<br/>
     * - This should only be needed for cases where `UpdateLayerStatistics` has failed<br/>
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#LAYERS_QUERY_EXTENT_INVALID_V4
     */
    LAYERS_QUERY_EXTENT_INVALID_V4,
    /**
     * The Sql-String to retrieve valid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - that may have been changed with a Spatialite 4 software<br/>
     * - for spatialite 4.0 with non-working vector_layers_statistics, but still has a valid layers_statistics table<br/>
     * - This should only be needed for cases where `UpdateLayerStatistics` has failed<br/>
     * - used for debugging<br/>
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#LAYERS_QUERY_EXTENT_LIST_V4
     */
    LAYERS_QUERY_EXTENT_LIST_V4,
    /**
     * The Sql-String to retrieve a minimal information on a valid or invalid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - that may have been changed with a Spatialite 4 software<br/>
     * - for spatialite 4.0 with non-working vector_layers_statistics, but still has a valid layers_statistics table<br/>
     * - used for debugging<br/>
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#LAYERS_QUERY_EXTENT_VALID_V3
     */
    LAYERS_QUERY_EXTENT_VALID_V3,
    /**
     * The Sql-String to retrieve invalid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - for spatialite 2.4 until 3.1.0 [Tables-Only]<br/>
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#LAYERS_QUERY_EXTENT_INVALID_V3
     */
    LAYERS_QUERY_EXTENT_INVALID_V3,
    /**
     * The Sql-String to retrieve a minimal information on a valid or invalid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - for spatialite 2.4 until 3.1.0 [Tables-Only]<br/>
     * - used for debugging<br/>
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#LAYERS_QUERY_EXTENT_LIST_V3
     */
    LAYERS_QUERY_EXTENT_LIST_V3,
    /**
     * The Sql-String to retrieve valid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - for spatialite 2.4 until 3.1.0 [Views-Only]<br/>
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific#VIEWS_QUERY_EXTENT_VALID_V3
     */
    VIEWS_QUERY_EXTENT_VALID_V3,
    /**
     * The Sql-String to retrieve invalid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - for spatialite 2.4 until 3.1.0 [Views-Only]<br/>
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specificVIEWS_QUERY_EXTENT_INVALID_V3
     */
    VIEWS_QUERY_EXTENT_INVALID_V3,
    /**
     * The Sql-String to retrieve a minimal information on a valid or invalid Vector-Layers from a Spatialite 3 Database.
     * <p/>
     * - for spatialite 2.4 until 3.1.0 [Views-Only]<br/>
     * - used for debugging<br/>
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specificVIEWS_QUERY_EXTENT_LIST_V3
     */
    VIEWS_QUERY_EXTENT_LIST_V3,
    /**
     * The Sql-String to retrieve valid RasterLite2-Layers from a Spatialite 4 Database.
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RASTER_COVERAGES_QUERYS-geopaparazzi-specific#RASTER_COVERAGES_QUERY_EXTENT_VALID_V42
     */
    RASTER_COVERAGES_QUERY_EXTENT_VALID_V42,
    /**
     * The Sql-String to retrieve invalid RasterLite2-Layers from a Spatialite 4 Database.
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RASTER_COVERAGES_QUERYS-geopaparazzi-specific#RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42
     */
    RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42,
    /**
     * The Sql-String to retrieve a minimal information on a valid or invalid Rasterlite2-Layers from a Spatialite 4 Database.
     * <p/>
     * - used for debugging
     * <p/>
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RASTER_COVERAGES_QUERYS-geopaparazzi-specificRASTER_COVERAGES_QUERY_EXTENT_LIST_V42
     */
    RASTER_COVERAGES_QUERY_EXTENT_LIST_V42,
    /**
     * The Sql-String to retrieve valid SPL_Geopackage-Layers from a SPL_Geopackage Revision 10 Database.
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/GEOPACKAGE_QUERY_R10-geopaparazzi-specific#GEOPACKAGE_QUERY_EXTENT_VALID_R10
     */
    GEOPACKAGE_QUERY_EXTENT_VALID_R10,
    /**
     * The Sql-String to retrieve valid SPL_Geopackage-Layers from a SPL_Geopackage Revision 10 Database.
     * <p/>
     * Results will be returned in a format used by Spatialite 4 Databases
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/GEOPACKAGE_QUERY_R10-geopaparazzi-specific#GEOPACKAGE_QUERY_EXTENT_INVALID_R10
     */
    GEOPACKAGE_QUERY_EXTENT_INVALID_R10,
    /**
     * The Sql-String to retrieve a minimal information on a valid or invalid SPL_Geopackage-Layers from a SPL_Geopackage Revision 10 Database.
     * <p/>
     * - used for debugging
     * <p/>
     * further documentation can be found here:
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/GEOPACKAGE_QUERY_R10-geopaparazzi-specificGEOPACKAGE_QUERY_EXTENT_LIST_R10
     */
    GEOPACKAGE_QUERY_EXTENT_LIST_R10;

    private String VIEWS_QUERY_EXTENT_INVALID = "";

    private HashMap<String, String> queriesMap = new HashMap<String, String>();

    private GeneralQueriesPreparer() {

        String VECTOR_LAYERS_QUERY_BASE = "";
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append("SELECT DISTINCT ");
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name");
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column");
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "layer_type");
            sb_query.append("||';ROWID;-1'");
            sb_query.append(" AS vector_key," + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "geometry_type");
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".coord_dimension");
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "srid");
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled||';' AS vector_data,");
            VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
        }

        String LAYERS_QUERY_BASE_V4 = "";
        String VECTOR_KEY_BASE = "";
        {
            LAYERS_QUERY_BASE_V4 = VECTOR_LAYERS_QUERY_BASE.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,
                    METADATA_LAYER_STATISTICS_TABLE_NAME);
            LAYERS_QUERY_BASE_V4 = LAYERS_QUERY_BASE_V4.replace(METADATA_LAYER_STATISTICS_TABLE_NAME + ".layer_type",
                    METADATA_VECTOR_LAYERS_TABLE_NAME + ".layer_type");
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" AS vector_key,CASE"); // 0 of second field
            sb_query.append(" WHEN type = 'GEOMETRY' THEN '0'");
            sb_query.append(" WHEN type = 'POINT' THEN '1'");
            sb_query.append(" WHEN type = 'LINESTRING' THEN '2'");
            sb_query.append(" WHEN type = 'POLYGON' THEN '3'");
            sb_query.append(" WHEN type = 'MULTIPOINT' THEN '4'");
            sb_query.append(" WHEN type = 'MULTILINESTRING' THEN '5'");
            sb_query.append(" WHEN type = 'MULTIPOLYGON' THEN '6'");
            sb_query.append(" WHEN type = 'GEOMETRYCOLLECTION' THEN '7'");
            sb_query.append(" END"); // 0
            sb_query.append("||';'||CASE"); // 2
            sb_query.append(" WHEN ((coord_dimension = '2') OR (coord_dimension = 'XY')) THEN '2'");
            sb_query.append(" WHEN ((coord_dimension = '3') OR (coord_dimension = 'XYZ') OR (coord_dimension = 'XYM')) THEN '3'");
            sb_query.append(" WHEN ((coord_dimension = '4') OR (coord_dimension = 'XYZM')) THEN '4'");
            sb_query.append(" END"); // 0
            sb_query.append("||';'||srid"); // 3
            sb_query.append("||';'||spatial_index_enabled||';' AS vector_data,"); // 4
            VECTOR_KEY_BASE = sb_query.toString();
        }

        String LAYERS_QUERY_BASE_V3 = "";
        {
            StringBuilder sb_query = new StringBuilder();
            // SELECT
            // f_table_name,f_geometry_column,geometry_type,coord_dimension,srid,spatial_index_enabled
            // FROM geometry_columns;
            // SELECT f_table_name,f_geometry_column,type,coord_dimension,srid,spatial_index_enabled
            // FROM geometry_columns
            sb_query.append("SELECT DISTINCT ");
            sb_query.append(" f_table_name"); // 0 of 1st field
            sb_query.append("||';'||f_geometry_column"); // 1 of 1st field
            sb_query.append("||';'||'AbstractSpatialTable'"); // 2 of 1st field
            sb_query.append("||';ROWID;-1'"); // 3+4 of 1st field
            sb_query.append(VECTOR_KEY_BASE);
            LAYERS_QUERY_BASE_V3 = sb_query.toString();
        }
        String VIEWS_QUERY_BASE_V3 = "";
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append("SELECT DISTINCT");
            sb_query.append(" view_name"); // 0 of 1st field
            sb_query.append("||';'||view_geometry"); // 1 of 1st field
            sb_query.append("||';'||'SpatialView'"); // 2 of 1st field
            sb_query.append("||';ROWID;-1'"); // 3+4 of 1st field
            sb_query.append(VECTOR_KEY_BASE);
            VIEWS_QUERY_BASE_V3 = sb_query.toString();
        }
        String VECTOR_LAYERS_QUERY_FROM = "";
        {
            // sb_query.append(" FROM FROM geometry_columns ORDER BY f_table_name ASC,f_geometry_column";
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" FROM " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + " INNER JOIN "
                    + METADATA_VECTOR_LAYERS_TABLE_NAME);
            sb_query.append(" ON " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name");
            sb_query.append(" = " + METADATA_VECTOR_LAYERS_TABLE_NAME + ".table_name");
            sb_query.append(" AND " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column");
            sb_query.append(" = " + METADATA_VECTOR_LAYERS_TABLE_NAME + ".geometry_column");
            VECTOR_LAYERS_QUERY_FROM = sb_query.toString();
        }
        String LAYERS_QUERY_FROM_V3 = "";
        String LAYERS_QUERY_FROM_V4 = "";
        String VIEWS_QUERY_FROM_V3 = "";
        {
            StringBuilder sb_query = new StringBuilder();
            // V2.4: SELECT
            // raster_layer,table_name,geometry_column,row_count,extent_min_x,extent_min_y,extent_max_x,extent_max_y
            // FROM layer_statistics
            sb_query.append(" FROM " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME + " INNER JOIN " + METADATA_LAYER_STATISTICS_TABLE_NAME);
            sb_query.append(" ON " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_table_name");
            sb_query.append(" = " + METADATA_LAYER_STATISTICS_TABLE_NAME + ".table_name");
            sb_query.append(" AND " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_geometry_column");
            sb_query.append(" = " + METADATA_LAYER_STATISTICS_TABLE_NAME + ".geometry_column");
            LAYERS_QUERY_FROM_V3 = sb_query.toString();
            VIEWS_QUERY_FROM_V3 = LAYERS_QUERY_FROM_V3.replace(METADATA_GEOMETRY_COLUMNS_TABLE_NAME,
                    METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME);
            VIEWS_QUERY_FROM_V3 = VIEWS_QUERY_FROM_V3.replace(".f_table_name", ".view_name");
            VIEWS_QUERY_FROM_V3 = VIEWS_QUERY_FROM_V3.replace(".f_geometry_column", ".view_geometry");
            // VIEWS_QUERY_FROM_V3 will be continued after finishing LAYERS_QUERY_FROM_V4
            sb_query.append(" INNER JOIN " + METADATA_VECTOR_LAYERS_TABLE_NAME);
            sb_query.append(" ON " + METADATA_VECTOR_LAYERS_TABLE_NAME + ".table_name");
            sb_query.append(" = " + METADATA_LAYER_STATISTICS_TABLE_NAME + ".table_name");
            sb_query.append(" AND " + METADATA_VECTOR_LAYERS_TABLE_NAME + ".geometry_column");
            sb_query.append(" = " + METADATA_LAYER_STATISTICS_TABLE_NAME + ".geometry_column");
            LAYERS_QUERY_FROM_V4 = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VIEWS_QUERY_FROM_V3);
            sb_query.append(" INNER JOIN " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME);
            sb_query.append(" ON " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_table_name");
            sb_query.append(" = " + METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME + ".f_table_name");
            sb_query.append(" AND " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_geometry_column");
            sb_query.append(" = " + METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME + ".f_geometry_column");
            // VIEWS_QUERY_FROM_V3 is now compleate
            VIEWS_QUERY_FROM_V3 = sb_query.toString();
        }
        String VECTOR_LAYERS_QUERY_EXTENT_VALID = "";
        {
            StringBuilder sb_query = new StringBuilder();
            // if the record is invalid, only this field will be null
            // 'vector_key' and 'vector_data' will be use to attempt to recover from the error.
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count"); // 0
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x"); // 1.0
            sb_query.append("||','||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y"); // 1.1
            sb_query.append("||','||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x"); // 1.2
            sb_query.append("||','||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y"); // 1.3
            sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".last_verified AS vector_extent"); // 2
            VECTOR_LAYERS_QUERY_EXTENT_VALID = sb_query.toString();
        }
        String LAYERS_QUERY_EXTENT_VALID = "";
        String VECTOR_LAYERS_QUERY_EXTENT_INVALID = "";
        String LAYERS_QUERY_EXTENT_INVALID = "";
        {
            LAYERS_QUERY_EXTENT_VALID = VECTOR_LAYERS_QUERY_EXTENT_VALID.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,
                    METADATA_LAYER_STATISTICS_TABLE_NAME);
            LAYERS_QUERY_EXTENT_VALID = LAYERS_QUERY_EXTENT_VALID.replace(METADATA_LAYER_STATISTICS_TABLE_NAME + ".last_verified",
                    "strftime('%Y-%m-%dT%H:%M:%fZ','now')");
            StringBuilder sb_query = new StringBuilder();
            // if the record is invalid, only this field will what is invalid
            // - where 'field_name' is shown, that field is invalid
            sb_query.append("CASE WHEN " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count IS NULL THEN 'row_count' ELSE "); // 0
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count END "); // 0
            sb_query.append("||';'||CASE WHEN " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME
                    + ".extent_min_x IS NULL THEN 'extent_min_x' ELSE "); // 1.0
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x END "); // 1.0
            sb_query.append("||','||CASE WHEN " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME
                    + ".extent_min_y IS NULL THEN 'extent_min_y' ELSE "); // 1.1
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y END "); // 1.1
            sb_query.append("||','||CASE WHEN " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME
                    + ".extent_max_x IS NULL THEN 'extent_max_x' ELSE "); // 1.2
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x END "); // 1.2
            sb_query.append("||','||CASE WHEN " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME
                    + ".extent_max_y IS NULL THEN 'extent_max_y' ELSE "); // 1.3
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y END "); // 1.3
            // LAYERS_STATISTICS has no last_verified. Store result now and the continue to append
            LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();

            LAYERS_QUERY_EXTENT_INVALID = LAYERS_QUERY_EXTENT_INVALID.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,
                    METADATA_LAYER_STATISTICS_TABLE_NAME);
            sb_query.append("||';'||CASE WHEN " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME
                    + ".last_verified IS NULL THEN 'last_verified' ELSE "); // 2
            sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".last_verified END AS vector_extent"); // 2
            VECTOR_LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
            sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_EXTENT_INVALID);
            sb_query.append("||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') AS vector_extent"); // 2
            LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
            VIEWS_QUERY_EXTENT_INVALID = LAYERS_QUERY_EXTENT_INVALID.replace(METADATA_LAYER_STATISTICS_TABLE_NAME,
                    METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME);
        }

        String VECTOR_LAYERS_QUERY_ORDER = "";
        {
            StringBuilder sb_query = new StringBuilder();
            // first Views (Spatialview) then tables (AbstractSpatialTable), then Table-Name/Column
            sb_query.append(" ORDER BY " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "layer_type DESC");
            sb_query.append("," + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "table_name ASC");
            sb_query.append("," + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "geometry_column ASC");
            VECTOR_LAYERS_QUERY_ORDER = sb_query.toString();
        }
        String LAYERS_QUERY_ORDER_V3 = "";
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" ORDER BY " + METADATA_LAYER_STATISTICS_TABLE_NAME + "." + "table_name ASC");
            sb_query.append("," + METADATA_LAYER_STATISTICS_TABLE_NAME + "." + "geometry_column ASC");
            LAYERS_QUERY_ORDER_V3 = sb_query.toString();
        }
        String LAYERS_QUERY_ORDER_V4 = "";
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" ORDER BY " + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "layer_type DESC");
            sb_query.append("," + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "table_name ASC");
            sb_query.append("," + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "geometry_column ASC");
            LAYERS_QUERY_ORDER_V4 = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            // remove comma - last field
            query = query.replace("AS vector_data,", "AS vector_data");
            queriesMap.put("VECTOR_LAYERS_QUERY_EXTENT_LIST_V4", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_BASE_V3);
            sb_query.append(LAYERS_QUERY_FROM_V3);
            sb_query.append(LAYERS_QUERY_ORDER_V3);
            String query = sb_query.toString();
            // remove comma - last field
            query = query.replace("AS vector_data,", "AS vector_data");
            queriesMap.put("LAYERS_QUERY_EXTENT_LIST_V3", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VIEWS_QUERY_BASE_V3);
            sb_query.append(VIEWS_QUERY_FROM_V3);
            sb_query.append(LAYERS_QUERY_ORDER_V3);
            String query = sb_query.toString();
            // remove comma - last field
            query = query.replace("AS vector_data,", "AS vector_data");
            queriesMap.put("VIEWS_QUERY_EXTENT_LIST_V3", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_BASE_V4);
            sb_query.append(LAYERS_QUERY_FROM_V4);
            sb_query.append(LAYERS_QUERY_ORDER_V4);
            String query = sb_query.toString();
            query = query.replace("AS vector_data,", "AS vector_data"); // remove
            queriesMap.put("LAYERS_QUERY_EXTENT_LIST_V4", query);
        }
        String VECTOR_LAYERS_QUERY_WHERE = "";
        {
            StringBuilder sb_query = new StringBuilder();
            // if the creation of a spatial-view fails, a record may exist with 'row_count=NULL': this
            // is an invalid record and must be ignored
            sb_query.append(" WHERE (" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled = 1)");
            sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count IS NOT NULL)");
            sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count > 0)");
            sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x IS NOT NULL)");
            sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y IS NOT NULL)");
            sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x IS NOT NULL)");
            sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y IS NOT NULL)");
            VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
        }
        String LAYERS_QUERY_WHERE = "";
        {
            // 'vector_layers.' to 'geometry_columns.' - without changing 'vector_layers_statistics.'
            LAYERS_QUERY_WHERE = VECTOR_LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_TABLE_NAME + ".",
                    METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".");
            LAYERS_QUERY_WHERE = LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,
                    METADATA_LAYER_STATISTICS_TABLE_NAME);
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            // priority_marks_joined_lincoln;geometry;AbstractSpatialTable;ROWID 1;2;2913;1 NULL
            String query = sb_query.toString();
            queriesMap.put("VECTOR_LAYERS_QUERY_EXTENT_VALID_V4", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_BASE_V3);
            sb_query.append(LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(LAYERS_QUERY_FROM_V3);
            sb_query.append(LAYERS_QUERY_WHERE);
            sb_query.append(LAYERS_QUERY_ORDER_V3);
            // priority_marks_joined_lincoln;geometry;AbstractSpatialTable;ROWID 1;2;2913;1 NULL
            String query = sb_query.toString();
            queriesMap.put("LAYERS_QUERY_EXTENT_VALID_V3", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VIEWS_QUERY_BASE_V3);
            sb_query.append(LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(VIEWS_QUERY_FROM_V3);
            sb_query.append(LAYERS_QUERY_WHERE);
            sb_query.append(LAYERS_QUERY_ORDER_V3);
            // priority_marks_joined_lincoln;geometry;AbstractSpatialTable;ROWID 1;2;2913;1 NULL
            String query = sb_query.toString();
            queriesMap.put("VIEWS_QUERY_EXTENT_VALID_V3", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_BASE_V4);
            sb_query.append(LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(LAYERS_QUERY_FROM_V4);
            sb_query.append(LAYERS_QUERY_WHERE);
            sb_query.append(LAYERS_QUERY_ORDER_V4);
            // priority_marks_joined_lincoln;geometry;AbstractSpatialTable;ROWID 1;2;2913;1 NULL
            String query = sb_query.toString();
            queriesMap.put("LAYERS_QUERY_EXTENT_VALID_V4", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the creation of a spatial-view fails, a record may exist with 'row_count=NULL': this
            // is an invalid record and must be ignored
            sb_query.append(" WHERE (" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled = 0)");
            sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count IS NULL)");
            sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count == 0)");
            sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x IS NULL)");
            sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y IS NULL)");
            sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x IS NULL)");
            sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y IS NULL)");
            VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
        }
        {
            // 'vector_layers.' to 'geometry_columns.' - without changing 'vector_layers_statistics.'
            LAYERS_QUERY_WHERE = VECTOR_LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_TABLE_NAME + ".",
                    METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".");
            LAYERS_QUERY_WHERE = LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,
                    METADATA_LAYER_STATISTICS_TABLE_NAME);
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_INVALID);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            queriesMap.put("VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_BASE_V3);
            sb_query.append(LAYERS_QUERY_EXTENT_INVALID);
            sb_query.append(LAYERS_QUERY_FROM_V3);
            sb_query.append(LAYERS_QUERY_WHERE);
            sb_query.append(LAYERS_QUERY_ORDER_V3);
            String query = sb_query.toString();
            queriesMap.put("LAYERS_QUERY_EXTENT_INVALID_V3", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VIEWS_QUERY_BASE_V3);
            sb_query.append(LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(VIEWS_QUERY_FROM_V3);
            sb_query.append(LAYERS_QUERY_WHERE);
            sb_query.append(LAYERS_QUERY_ORDER_V3);
            // priority_marks_joined_lincoln;geometry;AbstractSpatialTable;ROWID 1;2;2913;1 NULL
            String query = sb_query.toString();
            queriesMap.put("VIEWS_QUERY_EXTENT_INVALID_V3", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(LAYERS_QUERY_BASE_V4);
            sb_query.append(LAYERS_QUERY_EXTENT_INVALID);
            sb_query.append(LAYERS_QUERY_FROM_V4);
            sb_query.append(LAYERS_QUERY_WHERE);
            sb_query.append(LAYERS_QUERY_ORDER_V4);
            String query = sb_query.toString();
            queriesMap.put("LAYERS_QUERY_EXTENT_INVALID_V4", query);
        }
        {
            // -------------------
            // end of building of Spatialite Queries
            // -------------------
            // RasterLite2 support - begin
            // -------------------
            StringBuilder sb_query = new StringBuilder();
            sb_query.append("SELECT DISTINCT ");
            sb_query.append("coverage_name"); // 0 of 1st field
            sb_query.append("||';'||compression"); // 1 of 1st field
            sb_query.append("||';'||'RasterLite2'"); // 2 of 1st field
            sb_query.append("||';'||REPLACE(title,';','-')"); // 3 of 1st field
            sb_query.append("||';'||REPLACE(abstract,';','-')"); // 4 of 1st field
            sb_query.append(" AS vector_key,pixel_type"); // 0 of second field
            sb_query.append("||';'||tile_width"); // 2
            sb_query.append("||';'||srid"); // 3
            sb_query.append("||';'||horz_resolution||';' AS vector_data,"); // 4
            VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the record is invalid, only this field will be null
            // 'vector_key' and 'vector_data' will be use to attempt to recover from the error.
            sb_query.append("num_bands"); // 0
            sb_query.append("||';'||extent_minx"); // 1.0
            sb_query.append("||','||extent_miny"); // 1.1
            sb_query.append("||','||extent_maxx"); // 1.2
            sb_query.append("||','||extent_maxy"); // 1.3
            sb_query.append("||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') AS vector_extent"); // 2
            VECTOR_LAYERS_QUERY_EXTENT_VALID = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" FROM " + METADATA_RASTERLITE2_RASTER_COVERAGES_TABLE_NAME);
            VECTOR_LAYERS_QUERY_FROM = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // first Views (Spatialview) then tables (AbstractSpatialTable), then Table-Name/Column
            sb_query.append(" ORDER BY coverage_name ASC");
            sb_query.append(",title ASC");
            VECTOR_LAYERS_QUERY_ORDER = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the SELECT RL2_LoadRaster(...) was not executed,
            // - a record may exist with 'statistics and extent=NULL':
            // - this is an invalid record and must be ignored
            sb_query.append(" WHERE (statistics IS NOT NULL)");
            sb_query.append(" AND (extent_minx IS NOT NULL)");
            sb_query.append(" AND (extent_miny IS NOT NULL)");
            sb_query.append(" AND (extent_maxx IS NOT NULL)");
            sb_query.append(" AND (extent_maxy IS NOT NULL)");
            VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            queriesMap.put("RASTER_COVERAGES_QUERY_EXTENT_VALID_V42", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            query = query.replace("AS vector_data,", "AS vector_data"); // remove
            queriesMap.put("RASTER_COVERAGES_QUERY_EXTENT_LIST_V42", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the SELECT RL2_LoadRaster(...) was not executed,
            // - a record may exist with 'statistics and extent=NULL':
            // - this is an invalid record and must be ignored
            sb_query.append(" WHERE (statistics IS NULL)");
            sb_query.append(" OR (extent_minx IS NULL)");
            sb_query.append(" OR (extent_miny IS NULL)");
            sb_query.append(" OR (extent_maxx IS NULL)");
            sb_query.append(" OR (extent_maxy IS NULL)");
            VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the record is invalid, only this field will what is invalid
            // - where 'field_name' is shown, that field is invalid
            sb_query.append("CASE WHEN statistics IS NULL THEN 'statistics' ELSE "); // 0
            sb_query.append("pixel_type END "); // 0
            sb_query.append("||';'||CASE WHEN extent_minx IS NULL THEN 'extent_minx' ELSE extent_minx END"); // 1.0
            sb_query.append("||','||CASE WHEN extent_miny IS NULL THEN 'extent_miny' ELSE extent_miny END"); // 1.1
            sb_query.append("||','||CASE WHEN extent_maxx IS NULL THEN 'extent_maxx' ELSE extent_maxx END"); // 1.2
            sb_query.append("||','||CASE WHEN extent_maxy IS NULL THEN 'extent_maxy' ELSE extent_maxy END"); // 1.3
            sb_query.append("||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') AS vector_extent"); // 2
            VECTOR_LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_INVALID);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            queriesMap.put("RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42", query);
        }
        {
            // -------------------
            // RasterLite2 support - end
            // -------------------
            // GeoPackage support - begin
            // -------------------
            StringBuilder sb_query = new StringBuilder();
            sb_query.append("SELECT DISTINCT ");
            sb_query.append("table_name"); // 0 of 1st field
            sb_query.append("||';'||CASE"); // 1 of 1st field
            sb_query.append(" WHEN data_type = 'features' THEN ("); // 1 of 1st field
            sb_query.append("SELECT column_name FROM gpkg_geometry_columns WHERE table_name = ''||table_name||''"); // 1
            // of
            // 1st
            // field
            sb_query.append(") WHEN data_type = 'tiles' THEN 'tile_data'"); // 1 of 1st field
            sb_query.append(" END"); // 1 of 1st field
            sb_query.append(" ||';'||CASE"); // 2 of 1st field
            sb_query.append(" WHEN data_type = 'features' THEN 'GeoPackage_features'"); // 2 of 1st
            // field
            sb_query.append(" WHEN data_type = 'tiles' THEN 'GeoPackage_tiles'"); // 2 of 1st field
            sb_query.append(" END"); // 2 of 1st field
            sb_query.append("||';'||REPLACE(identifier,';','-')"); // 3 of second field
            sb_query.append("||';'||REPLACE(description,';','-') AS vector_key,"); // 4 of second field
            // fromosm_tiles;tile_data;GeoPackage_tiles;© OpenStreetMap contributors, See
            // http://www.openstreetmap.org/copyright;OSM Tiles;
            // geonames;geometry;GeoPackage_features;Data from http://www.geonames.org/, under Creative
            // Commons Attribution 3.0 License;Geonames;
            sb_query.append("CASE"); // 0 of second field
            sb_query.append(" WHEN data_type = 'features' THEN ("); // 0 of second field
            sb_query.append(""); // 0 of second field
            // Now the horror begins ...
            LAYERS_QUERY_BASE_V3 = "SELECT geometry_type_name FROM gpkg_geometry_columns WHERE table_name = ''||table_name||''"; // 0
            // of
            // second
            // field
            sb_query.append("CASE WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'GEOMETRY' THEN '0'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'POINT' THEN '1'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'LINESTRING' THEN '2'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'POLYGON' THEN '3'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'MULTIPOINT' THEN '4'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'MULTILINESTRING' THEN '5'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'MULTIPOLYGON' THEN '6'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = 'GEOMETRYCOLLECTION' THEN '7' END");
            // ... to be continued ...
            sb_query.append(") WHEN data_type = 'tiles' THEN ("); // 1 of 1st field
            sb_query.append("SELECT min(zoom_level) FROM gpkg_tile_matrix WHERE table_name = ''||table_name||''"); // 1
            // of
            // second
            // field
            sb_query.append(") END"); // 0 of second field
            sb_query.append("||';'||CASE"); // 1 of second field
            sb_query.append(" WHEN data_type = 'features' THEN ("); // 1 of second field
            // ... and now for something completely different ...
            LAYERS_QUERY_BASE_V3 = "SELECT z||','||m FROM gpkg_geometry_columns WHERE table_name = ''||table_name||''"; // 1
            // of
            // second
            // field
            sb_query.append("CASE WHEN (" + LAYERS_QUERY_BASE_V3 + ") = '0,0' THEN '2'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = '1,0' THEN '3'");
            sb_query.append(" WHEN (" + LAYERS_QUERY_BASE_V3 + ") = '1,1' THEN '4' END");
            // ... ich habe fertig.
            sb_query.append(") WHEN data_type = 'tiles' THEN ("); // 1 of second field
            sb_query.append("SELECT max(zoom_level) FROM gpkg_tile_matrix WHERE table_name = ''||table_name||''"); // 1
            // of
            // second
            // field
            sb_query.append(") END"); // 1 of second field
            sb_query.append("||';'||CASE"); // 2 of second field
            sb_query.append(" WHEN srs_id = '1' THEN '4326'"); // 2 of second field
            sb_query.append(" WHEN srs_id = '2' THEN '3857'"); // 2 of second field
            sb_query.append(" ELSE srs_id "); // 2 of second field
            sb_query.append(" END"); // 2 of 1st field
            sb_query.append("||';'||'-1'||';' AS vector_data,"); // 3 of second field
            // 0;10;3857;0;
            // 1;2;4326;0;
            VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the record is invalid, only this field will be null
            // 'vector_key' and 'vector_data' will be use to attempt to recover from the error.
            sb_query.append("'-1'"); // 0
            sb_query.append("||';'||min_x"); // 1.0
            sb_query.append("||','||min_y"); // 1.1
            sb_query.append("||','||max_x"); // 1.2
            sb_query.append("||','||max_y"); // 1.3
            sb_query.append("||';'||last_change AS vector_extent"); // 2
            // -1;-180.0;-90.0;180.0;90.0;2013-01-18T17:39:20.000Z
            VECTOR_LAYERS_QUERY_EXTENT_VALID = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(" FROM " + METADATA_GEOPACKAGE_TABLE_NAME);
            VECTOR_LAYERS_QUERY_FROM = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // condition not known when this is NOT true
            // - this is an invalid record and must be ignored
            sb_query.append(" WHERE (last_change IS NOT NULL)");
            sb_query.append(" AND (min_x IS NOT NULL)");
            sb_query.append(" AND (min_y IS NOT NULL)");
            sb_query.append(" AND (max_x IS NOT NULL)");
            sb_query.append(" AND (max_y IS NOT NULL)");
            VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // first Views (Spatialview) then tables (AbstractSpatialTable), then Table-Name/Column
            sb_query.append(" ORDER BY table_name ASC");
            sb_query.append(",identifier ASC");
            VECTOR_LAYERS_QUERY_ORDER = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_VALID);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            queriesMap.put("GEOPACKAGE_QUERY_EXTENT_VALID_R10", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            query = query.replace("AS vector_data,", "AS vector_data"); // remove
            queriesMap.put("GEOPACKAGE_QUERY_EXTENT_LIST_R10", query);
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the SELECT RL2_LoadRaster(...) was not executed,
            // - a record may exist with 'statistics and extent=NULL':
            // - this is an invalid record and must be ignored
            sb_query.append(" WHERE (last_change IS NULL)");
            sb_query.append(" OR (min_x IS NULL)");
            sb_query.append(" OR (min_y IS NULL)");
            sb_query.append(" OR (max_x IS NULL)");
            sb_query.append(" OR (max_y IS NULL)");
            VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            // if the record is invalid, only this field will what is invalid
            // - where 'field_name' is shown, that field is invalid
            sb_query.append("'-1'"); // 0
            sb_query.append("||';'||CASE WHEN min_x IS NULL THEN 'min_x' ELSE min_x END"); // 1.0
            sb_query.append("||','||CASE WHEN min_y IS NULL THEN 'min_y' ELSE min_y END"); // 1.1
            sb_query.append("||','||CASE WHEN max_x IS NULL THEN 'max_x' ELSE max_x END"); // 1.2
            sb_query.append("||','||CASE WHEN max_y IS NULL THEN 'max_y' ELSE max_y END"); // 1.3
            sb_query.append("||','||CASE WHEN max_y IS NULL THEN 'max_y' ELSE max_y END"); // 1.3
            sb_query.append("||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') AS vector_extent"); // 2
            VECTOR_LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
        }
        {
            StringBuilder sb_query = new StringBuilder();
            sb_query.append(VECTOR_LAYERS_QUERY_BASE);
            sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_INVALID);
            sb_query.append(VECTOR_LAYERS_QUERY_FROM);
            sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
            sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
            String query = sb_query.toString();
            queriesMap.put("GEOPACKAGE_QUERY_EXTENT_INVALID_R10", query);
        }
        // -------------------
        // GoPackage support - end
        // -------------------
        // This is not something that should be developed more than once ...
        /*
        GPLog.androidLog(-1, "DaoSpatialite: VECTOR_LAYERS_QUERY_EXTENT_VALID_V4["+ VECTOR_LAYERS_QUERY_EXTENT_VALID_V4+"]");
        GPLog.androidLog(-1, "DaoSpatialite: VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4[" + VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: VECTOR_LAYERS_QUERY_EXTENT_LIST_V4["+ VECTOR_LAYERS_QUERY_EXTENT_LIST_V4 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: LAYERS_QUERY_EXTENT_VALID_V4["+ LAYERS_QUERY_EXTENT_VALID_V4+"]");
        GPLog.androidLog(-1, "DaoSpatialite: LAYERS_QUERY_EXTENT_INVALID_V4[" + LAYERS_QUERY_EXTENT_INVALID_V4 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: LAYERS_QUERY_EXTENT_LIST_V4["+ LAYERS_QUERY_EXTENT_LIST_V4 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: LAYERS_QUERY_EXTENT_VALID_V3["+ LAYERS_QUERY_EXTENT_VALID_V3+"] ");
        GPLog.androidLog(-1, "DaoSpatialite: LAYERS_QUERY_EXTENT_INVALID_V3[" + LAYERS_QUERY_EXTENT_INVALID_V3 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: LAYERS_QUERY_EXTENT_LIST_V3["+ LAYERS_QUERY_EXTENT_LIST_V3 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: VIEWS_QUERY_EXTENT_VALID_V3["+ VIEWS_QUERY_EXTENT_VALID_V3+"]");
        GPLog.androidLog(-1, "DaoSpatialite: VIEWS_QUERY_EXTENT_INVALID_V3[" + VIEWS_QUERY_EXTENT_INVALID_V3 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: VIEWS_QUERY_EXTENT_LIST_V3["+ VIEWS_QUERY_EXTENT_LIST_V3 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: RASTER_COVERAGES_QUERY_EXTENT_VALID_V42["+ RASTER_COVERAGES_QUERY_EXTENT_VALID_V42+"]");
        GPLog.androidLog(-1, "DaoSpatialite: RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42[" + RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: RASTER_COVERAGES_QUERY_EXTENT_LIST_V42["+ RASTER_COVERAGES_QUERY_EXTENT_LIST_V42 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: GEOPACKAGE_QUERY_EXTENT_VALID_R10["+ GEOPACKAGE_QUERY_EXTENT_VALID_R10+"]");
        GPLog.androidLog(-1, "DaoSpatialite: GEOPACKAGE_QUERY_EXTENT_INVALID_R10[" + GEOPACKAGE_QUERY_EXTENT_INVALID_R10 + "] ");
        GPLog.androidLog(-1, "DaoSpatialite: GEOPACKAGE_QUERY_EXTENT_LIST_R10["+ GEOPACKAGE_QUERY_EXTENT_LIST_R10 + "] ");
        */
    }

    /**
     * Get the sql for the current query.
     *
     * @return the sql.
     */
    public String getQuery() {
        return queriesMap.get(name());
    }

}
