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
package eu.geopaparazzi.spatialite.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import eu.geopaparazzi.spatialite.util.SpatialiteDatabaseType;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Spatialite support methods.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoSpatialite {
    /**
     * From https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
     */
    public static final String METADATA_VECTOR_LAYERS_TABLE_NAME = "vector_layers";
    /**
     * From https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
     */
    public static final String METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME = "vector_layers_statistics";
    /**
     * From not yet documented [2014-05-09]
     */
    public static final String METADATA_RASTERLITE2_RASTER_COVERAGES_TABLE_NAME = "raster_coverages";
    /**
     * From 12-128r9_OGC_GeoPackage_Encoding_Standard_accept_changes_.pdf
     */
    public static final String METADATA_GEOPACKAGE_TABLE_NAME = "gpkg_contents";
    /**
     * Starting from spatialite 2.4 to 3.1.0
     */
    public static final String METADATA_LAYER_STATISTICS_TABLE_NAME = "layer_statistics";
    /**
     * Starting from spatialite 2.4 to present
     */
    public static final String METADATA_GEOMETRY_COLUMNS_TABLE_NAME = "geometry_columns";
    /**
     * Starting from spatialite 2.4 to present
     */
    public static final String METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME = "views_geometry_columns";

    /**
     * The metadata table.
     */
    public final static String TABLE_METADATA = "metadata";
    /**
     * The metadata column name.
     */
    public final static String COL_METADATA_NAME = "name";
    /**
     * The metadata column value.
     */
    public final static String COL_METADATA_VALUE = "value";
    /**
     * The properties table name.
     */
    public static final String PROPERTIESTABLE = "dataproperties";
    /**
     * The properties table id field.
     */
    public static final String ID = "_id";
    /**
     *
     */
    public static final String NAME = "name";
    /**
     *
     */
    public static final String SIZE = "size";
    /**
     *
     */
    public static final String FILLCOLOR = "fillcolor";
    /**
     *
     */
    public static final String STROKECOLOR = "strokecolor";
    /**
     *
     */
    public static final String FILLALPHA = "fillalpha";
    /**
     *
     */
    public static final String STROKEALPHA = "strokealpha";
    /**
     *
     */
    public static final String SHAPE = "shape";
    /**
     *
     */
    public static final String WIDTH = "width";
    /**
     *
     */
    public static final String ENABLED = "enabled";
    /**
     *
     */
    public static final String ORDER = "layerorder";
    /**
     *
     */
    public static final String DECIMATION = "decimationfactor";
    /**
     *
     */
    public static final String DASH = "dashpattern";
    /**
     *
     */
    public static final String MINZOOM = "minzoom";
    /**
     *
     */
    public static final String MAXZOOM = "maxzoom";

    /**
     *
     */
    public static final String LABELFIELD = "labelfield";
    /**
     *
     */
    public static final String LABELSIZE = "labelsize";
    /**
     *
     */
    public static final String LABELVISIBLE = "labelvisible";

    /**
     * The complete list of fields in the properties table.
     */
    public static List<String> PROPERTIESTABLE_FIELDS_LIST;
    static {
        List<String> fieldsList = new ArrayList<String>();
        fieldsList.add(ID);
        fieldsList.add(NAME);
        fieldsList.add(SIZE);
        fieldsList.add(FILLCOLOR);
        fieldsList.add(STROKECOLOR);
        fieldsList.add(FILLALPHA);
        fieldsList.add(STROKEALPHA);
        fieldsList.add(SHAPE);
        fieldsList.add(WIDTH);
        fieldsList.add(LABELSIZE);
        fieldsList.add(LABELFIELD);
        fieldsList.add(LABELVISIBLE);
        fieldsList.add(ENABLED);
        fieldsList.add(ORDER);
        fieldsList.add(DASH);
        fieldsList.add(MINZOOM);
        fieldsList.add(MAXZOOM);
        fieldsList.add(DECIMATION);
        PROPERTIESTABLE_FIELDS_LIST = Collections.unmodifiableList(fieldsList);
    }

    /**
     * General sql query to retrieve vector data of the whole Database in 1 query
     * - this is Spatialite4+ specfic and will be called in checkDatabaseTypeAndValidity
     * - invalid entries are filtered out (row_count > 0 and min/max x+y NOT NULL)
     * -- and will returned spatialVectorMap with the 2 Fields returned by this query
     * -- the result will be sorted with views first and Tables second
     * - Field-Names and use:
     * -- 'vector_key'   : fields often needed and used in map.key [always valid]
     * -- 'vector_data'  : fields NOT often needed and used in map.value [first portion and always valid]
     * -- 'vector_extent': fields NOT often needed and used in map.value [second portion and NOT always valid]
     * Queries for Spatialite (all versions) at:
     *  https://github.com/mj10777/Spatialite-Tasks-with-Sql-Scripts/wiki/VECTOR_LAYERS_QUERYS-geopaparazzi-specific
     * Queries for RasterLite2 at:
     *  https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RASTER_COVERAGES_QUERYS-geopaparazzi-specific
     * Queries for GeoPackage R10 at:
     *  https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/GEOPACKAGE_QUERY_R10-geopaparazzi-specific
     * <ol>
     * <li>3 Fields will be returned with the following structure</li>
     * <li>0 table_name: berlin_stadtteile</li>
     * <li>1: geometry_column - soldner_polygon</li>
     * <li>2: layer_type - SpatialView or SpatialTable</li>
     * <li>3: ROWID - SpatialTable: default ; when SpatialView or will be replaced</li>
     * <li>4: view_read_only - SpatialTable: -1 ; when SpatialView: 0=read_only or 1 writable</li>
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
     * Validity: s_vector_key.split(";"); must return the length of 5
     * Validity: s_vector_data.split(";"); must return the length of 7
     *           sa_vector_data[5].split(","); must return the length of 4
     */
    // Mode Types: 0=strict ; 1=tolerant ; 2=corrective ; 3=corrective with CreateSpatialIndex
    // SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE
    // read in MapsDirManager.init. 
    // - Set to 3 if desired. After compleation of init, turn back to 0
    // - Set SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE to false
    public static int VECTOR_LAYERS_QUERY_MODE=0;
    // for spatialite 4.0 with valid vector_layers_statistics, all of which have a layers_statistics table
    public static String VECTOR_LAYERS_QUERY_EXTENT_LIST_V4;
    public static String VECTOR_LAYERS_QUERY_EXTENT_VALID_V4;
    public static String VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4;
    // for spatialite 4.0 with non-working vector_layers_statistics, but still has a valid layers_statistics table
    public static String LAYERS_QUERY_EXTENT_LIST_V4;
    public static String LAYERS_QUERY_EXTENT_VALID_V4;
    public static String LAYERS_QUERY_EXTENT_INVALID_V4;
    // for spatialite 2.4 until 3.1.0 [Tables-Only]
    public static String LAYERS_QUERY_EXTENT_LIST_V3;
    public static String LAYERS_QUERY_EXTENT_VALID_V3;
    public static String LAYERS_QUERY_EXTENT_INVALID_V3;
    // for spatialite 2.4 until 3.1.0 [Views-Only]
    public static String VIEWS_QUERY_EXTENT_LIST_V3;
    public static String VIEWS_QUERY_EXTENT_VALID_V3;
    public static String VIEWS_QUERY_EXTENT_INVALID_V3;
    // for spatialite 4.2 with valid raster_coverages, RasterLite2 support
    public static String RASTER_COVERAGES_QUERY_EXTENT_LIST_V42;
    public static String RASTER_COVERAGES_QUERY_EXTENT_VALID_V42;
    public static String RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42;
    // for GeoPackage R10 based on 20140101.world_Haiti.gpkg
    public static String GEOPACKAGE_QUERY_EXTENT_LIST_R10;
    public static String GEOPACKAGE_QUERY_EXTENT_VALID_R10;
    public static String GEOPACKAGE_QUERY_EXTENT_INVALID_R10;
    static {
       String VECTOR_LAYERS_QUERY_BASE="";
       String LAYERS_QUERY_BASE_V4="";
       String LAYERS_QUERY_BASE_V3="";
       String VIEWS_QUERY_BASE_V3="";
       String VECTOR_LAYERS_QUERY_FROM="";
       String LAYERS_QUERY_FROM_V3="";
       String LAYERS_QUERY_FROM_V4="";
       String VIEWS_QUERY_FROM_V3="";
       String VECTOR_LAYERS_QUERY_EXTENT_VALID="";
       String LAYERS_QUERY_EXTENT_VALID="";
       String VECTOR_LAYERS_QUERY_EXTENT_INVALID="";
       String LAYERS_QUERY_EXTENT_INVALID="";
       String VIEWS_QUERY_EXTENT_INVALID="";
       String VECTOR_LAYERS_QUERY_WHERE="";
       String LAYERS_QUERY_WHERE="";
       String VECTOR_LAYERS_QUERY_ORDER="";
       String LAYERS_QUERY_ORDER_V3="";
       String LAYERS_QUERY_ORDER_V4="";
       String VECTOR_KEY_BASE="";
       StringBuilder sb_query = new StringBuilder();
       sb_query.append("SELECT DISTINCT ");
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name"); // 0 of 1st field
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column"); // 1 of 1st field
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "layer_type"); //  2 of 1st field
       sb_query.append("||';ROWID;-1'"); // 3+4 of 1st field
       sb_query.append(" AS vector_key," + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "geometry_type"); // 0 of second field
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".coord_dimension"); // 2
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "srid"); // 3
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled||';' AS vector_data,"); // 4
       VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
       LAYERS_QUERY_BASE_V4=VECTOR_LAYERS_QUERY_BASE.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,METADATA_LAYER_STATISTICS_TABLE_NAME);
       LAYERS_QUERY_BASE_V4=LAYERS_QUERY_BASE_V4.replace(METADATA_LAYER_STATISTICS_TABLE_NAME+".layer_type",METADATA_VECTOR_LAYERS_TABLE_NAME+".layer_type");
       sb_query = new StringBuilder();
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
       sb_query = new StringBuilder();
       // SELECT f_table_name,f_geometry_column,geometry_type,coord_dimension,srid,spatial_index_enabled FROM geometry_columns;
       // SELECT f_table_name,f_geometry_column,type,coord_dimension,srid,spatial_index_enabled FROM geometry_columns
       sb_query.append("SELECT DISTINCT ");
       sb_query.append(" f_table_name"); // 0 of 1st field
       sb_query.append("||';'||f_geometry_column"); // 1 of 1st field
       sb_query.append("||';'||'SpatialTable'"); //  2 of 1st field
       sb_query.append("||';ROWID;-1'"); // 3+4 of 1st field
       sb_query.append(VECTOR_KEY_BASE);
       LAYERS_QUERY_BASE_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append("SELECT DISTINCT");
       sb_query.append(" view_name"); // 0 of 1st field
       sb_query.append("||';'||view_geometry"); // 1 of 1st field
       sb_query.append("||';'||'SpatialView'"); //  2 of 1st field
       sb_query.append("||';ROWID;-1'"); // 3+4 of 1st field
       sb_query.append(VECTOR_KEY_BASE);
       VIEWS_QUERY_BASE_V3 = sb_query.toString();
       // sb_query.append(" FROM FROM geometry_columns ORDER BY f_table_name ASC,f_geometry_column";
       sb_query = new StringBuilder();
       sb_query.append(" FROM " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + " INNER JOIN " + METADATA_VECTOR_LAYERS_TABLE_NAME);
       sb_query.append(" ON "+ METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name");
       sb_query.append(" = "+ METADATA_VECTOR_LAYERS_TABLE_NAME + ".table_name");
       sb_query.append(" AND "+ METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column");
       sb_query.append(" = "+ METADATA_VECTOR_LAYERS_TABLE_NAME + ".geometry_column");
       VECTOR_LAYERS_QUERY_FROM = sb_query.toString();
       sb_query = new StringBuilder();
       // V2.4: SELECT raster_layer,table_name,geometry_column,row_count,extent_min_x,extent_min_y,extent_max_x,extent_max_y FROM layer_statistics
       sb_query.append(" FROM " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME + " INNER JOIN " + METADATA_LAYER_STATISTICS_TABLE_NAME);
       sb_query.append(" ON "+ METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_table_name");
       sb_query.append(" = "+ METADATA_LAYER_STATISTICS_TABLE_NAME + ".table_name");
       sb_query.append(" AND "+ METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_geometry_column");
       sb_query.append(" = "+ METADATA_LAYER_STATISTICS_TABLE_NAME + ".geometry_column");
       LAYERS_QUERY_FROM_V3 = sb_query.toString();
       VIEWS_QUERY_FROM_V3 = LAYERS_QUERY_FROM_V3.replace(METADATA_GEOMETRY_COLUMNS_TABLE_NAME,METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME);
       VIEWS_QUERY_FROM_V3 = VIEWS_QUERY_FROM_V3.replace(".f_table_name",".view_name");
       VIEWS_QUERY_FROM_V3 = VIEWS_QUERY_FROM_V3.replace(".f_geometry_column",".view_geometry");
       // VIEWS_QUERY_FROM_V3 will be continued after finishing LAYERS_QUERY_FROM_V4
       sb_query.append(" INNER JOIN " + METADATA_VECTOR_LAYERS_TABLE_NAME);
       sb_query.append(" ON "+ METADATA_VECTOR_LAYERS_TABLE_NAME + ".table_name");
       sb_query.append(" = "+ METADATA_LAYER_STATISTICS_TABLE_NAME + ".table_name");
       sb_query.append(" AND "+ METADATA_VECTOR_LAYERS_TABLE_NAME + ".geometry_column");
       sb_query.append(" = "+ METADATA_LAYER_STATISTICS_TABLE_NAME + ".geometry_column");
       LAYERS_QUERY_FROM_V4 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VIEWS_QUERY_FROM_V3);
       sb_query.append(" INNER JOIN " + METADATA_GEOMETRY_COLUMNS_TABLE_NAME);
       sb_query.append(" ON "+ METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_table_name");
       sb_query.append(" = "+ METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME + ".f_table_name");
       sb_query.append(" AND "+ METADATA_GEOMETRY_COLUMNS_TABLE_NAME + ".f_geometry_column");
       sb_query.append(" = "+ METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME + ".f_geometry_column");
       // VIEWS_QUERY_FROM_V3 is now compleate
       VIEWS_QUERY_FROM_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       // if the record is invalid, only this field will be null
       // 'vector_key' and 'vector_data' will be use to attempt to recover from the error.
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count"); // 0
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x"); // 1.0
       sb_query.append("||','||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y"); // 1.1
       sb_query.append("||','||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x"); // 1.2
       sb_query.append("||','||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y"); // 1.3
       sb_query.append("||';'||" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".last_verified AS vector_extent"); // 2
       VECTOR_LAYERS_QUERY_EXTENT_VALID = sb_query.toString();
       LAYERS_QUERY_EXTENT_VALID = VECTOR_LAYERS_QUERY_EXTENT_VALID.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,METADATA_LAYER_STATISTICS_TABLE_NAME);
       LAYERS_QUERY_EXTENT_VALID = LAYERS_QUERY_EXTENT_VALID.replace(METADATA_LAYER_STATISTICS_TABLE_NAME + ".last_verified","strftime('%Y-%m-%dT%H:%M:%fZ','now')");
       sb_query = new StringBuilder();
       // if the record is invalid, only this field will what is invalid
       // - where 'field_name' is shown, that field is invalid
       sb_query.append("CASE WHEN "+METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".row_count IS NULL THEN 'row_count' ELSE "); // 0
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".row_count END "); // 0
       sb_query.append("||';'||CASE WHEN "+METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_min_x IS NULL THEN 'extent_min_x' ELSE "); // 1.0
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_min_x END "); // 1.0
       sb_query.append("||','||CASE WHEN "+METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_min_y IS NULL THEN 'extent_min_y' ELSE "); // 1.1
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_min_y END "); // 1.1
       sb_query.append("||','||CASE WHEN "+METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_max_x IS NULL THEN 'extent_max_x' ELSE "); // 1.2
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_max_x END "); // 1.2
       sb_query.append("||','||CASE WHEN "+METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_max_y IS NULL THEN 'extent_max_y' ELSE "); // 1.3
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".extent_max_y END "); // 1.3
       // LAYERS_STATISTICS has no last_verified. Store result now and the continue to append
       LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
       LAYERS_QUERY_EXTENT_INVALID = LAYERS_QUERY_EXTENT_INVALID.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,METADATA_LAYER_STATISTICS_TABLE_NAME);
       sb_query.append("||';'||CASE WHEN "+METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".last_verified IS NULL THEN 'last_verified' ELSE "); // 2
       sb_query.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME+".last_verified END AS vector_extent"); // 2
       VECTOR_LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_EXTENT_INVALID);
       sb_query.append("||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') AS vector_extent"); // 2
       LAYERS_QUERY_EXTENT_INVALID = sb_query.toString();
       VIEWS_QUERY_EXTENT_INVALID = LAYERS_QUERY_EXTENT_INVALID.replace(METADATA_LAYER_STATISTICS_TABLE_NAME,METADATA_VIEWS_GEOMETRY_COLUMNS_TABLE_NAME);
       sb_query = new StringBuilder();
       // first Views (Spatialview) then tables (SpatialTable), then Table-Name/Column
       sb_query.append(" ORDER BY " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "layer_type DESC");
       sb_query.append("," + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "table_name ASC");
       sb_query.append("," + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "." + "geometry_column ASC");
       VECTOR_LAYERS_QUERY_ORDER = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(" ORDER BY " + METADATA_LAYER_STATISTICS_TABLE_NAME + "." + "table_name ASC");
       sb_query.append("," + METADATA_LAYER_STATISTICS_TABLE_NAME + "." + "geometry_column ASC");
       LAYERS_QUERY_ORDER_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(" ORDER BY " + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "layer_type DESC");
       sb_query.append("," + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "table_name ASC");
       sb_query.append("," + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "geometry_column ASC");
       LAYERS_QUERY_ORDER_V4 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       VECTOR_LAYERS_QUERY_EXTENT_LIST_V4 = sb_query.toString();
       // remove comma - last field
       VECTOR_LAYERS_QUERY_EXTENT_LIST_V4=VECTOR_LAYERS_QUERY_EXTENT_LIST_V4.replace("AS vector_data,","AS vector_data");
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_BASE_V3);
       sb_query.append(LAYERS_QUERY_FROM_V3);
       sb_query.append(LAYERS_QUERY_ORDER_V3);
       LAYERS_QUERY_EXTENT_LIST_V3 = sb_query.toString();
       // remove comma - last field
       LAYERS_QUERY_EXTENT_LIST_V3=LAYERS_QUERY_EXTENT_LIST_V3.replace("AS vector_data,","AS vector_data");
       sb_query = new StringBuilder();
       sb_query.append(VIEWS_QUERY_BASE_V3);
       sb_query.append(VIEWS_QUERY_FROM_V3);
       sb_query.append(LAYERS_QUERY_ORDER_V3);
       VIEWS_QUERY_EXTENT_LIST_V3 = sb_query.toString();
       // remove comma - last field
       VIEWS_QUERY_EXTENT_LIST_V3=VIEWS_QUERY_EXTENT_LIST_V3.replace("AS vector_data,","AS vector_data");
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_BASE_V4);
       sb_query.append(LAYERS_QUERY_FROM_V4);
       sb_query.append(LAYERS_QUERY_ORDER_V4);
       LAYERS_QUERY_EXTENT_LIST_V4 = sb_query.toString();
       LAYERS_QUERY_EXTENT_LIST_V4=LAYERS_QUERY_EXTENT_LIST_V4.replace("AS vector_data,","AS vector_data"); // remove
       sb_query = new StringBuilder();
       // if the creation of a spatial-view fails, a record may exist with 'row_count=NULL': this is an invalid record and must be ignored
       sb_query.append(" WHERE (" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled = 1)");
       sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count IS NOT NULL)");
       sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count > 0)");
       sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x IS NOT NULL)");
       sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y IS NOT NULL)");
       sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x IS NOT NULL)");
       sb_query.append(" AND (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y IS NOT NULL)");
       VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
       // 'vector_layers.' to 'geometry_columns.' - without changing 'vector_layers_statistics.'
       LAYERS_QUERY_WHERE=VECTOR_LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_TABLE_NAME+".",METADATA_GEOMETRY_COLUMNS_TABLE_NAME+".");
       LAYERS_QUERY_WHERE=LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,METADATA_LAYER_STATISTICS_TABLE_NAME);
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       // priority_marks_joined_lincoln;geometry;SpatialTable;ROWID 1;2;2913;1 NULL
       VECTOR_LAYERS_QUERY_EXTENT_VALID_V4 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_BASE_V3);
       sb_query.append(LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(LAYERS_QUERY_FROM_V3);
       sb_query.append(LAYERS_QUERY_WHERE);
       sb_query.append(LAYERS_QUERY_ORDER_V3);
       // priority_marks_joined_lincoln;geometry;SpatialTable;ROWID 1;2;2913;1 NULL
       LAYERS_QUERY_EXTENT_VALID_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VIEWS_QUERY_BASE_V3);
       sb_query.append(LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(VIEWS_QUERY_FROM_V3);
       sb_query.append(LAYERS_QUERY_WHERE);
       sb_query.append(LAYERS_QUERY_ORDER_V3);
       // priority_marks_joined_lincoln;geometry;SpatialTable;ROWID 1;2;2913;1 NULL
       VIEWS_QUERY_EXTENT_VALID_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_BASE_V4);
       sb_query.append(LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(LAYERS_QUERY_FROM_V4);
       sb_query.append(LAYERS_QUERY_WHERE);
       sb_query.append(LAYERS_QUERY_ORDER_V4);
       // priority_marks_joined_lincoln;geometry;SpatialTable;ROWID 1;2;2913;1 NULL
       LAYERS_QUERY_EXTENT_VALID_V4 = sb_query.toString();
       sb_query = new StringBuilder();
       // if the creation of a spatial-view fails, a record may exist with 'row_count=NULL': this is an invalid record and must be ignored
       sb_query.append(" WHERE (" + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled = 0)");
       sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count IS NULL)");
       sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count == 0)");
       sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x IS NULL)");
       sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y IS NULL)");
       sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x IS NULL)");
       sb_query.append(" OR (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y IS NULL)");
       VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
       // 'vector_layers.' to 'geometry_columns.' - without changing 'vector_layers_statistics.'
       LAYERS_QUERY_WHERE=VECTOR_LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_TABLE_NAME+".",METADATA_GEOMETRY_COLUMNS_TABLE_NAME+".");
       LAYERS_QUERY_WHERE=LAYERS_QUERY_WHERE.replace(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME,METADATA_LAYER_STATISTICS_TABLE_NAME);
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_INVALID);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_BASE_V3);
       sb_query.append(LAYERS_QUERY_EXTENT_INVALID);
       sb_query.append(LAYERS_QUERY_FROM_V3);
       sb_query.append(LAYERS_QUERY_WHERE);
       sb_query.append(LAYERS_QUERY_ORDER_V3);
       LAYERS_QUERY_EXTENT_INVALID_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VIEWS_QUERY_BASE_V3);
       sb_query.append(LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(VIEWS_QUERY_FROM_V3);
       sb_query.append(LAYERS_QUERY_WHERE);
       sb_query.append(LAYERS_QUERY_ORDER_V3);
       // priority_marks_joined_lincoln;geometry;SpatialTable;ROWID 1;2;2913;1 NULL
       VIEWS_QUERY_EXTENT_INVALID_V3 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(LAYERS_QUERY_BASE_V4);
       sb_query.append(LAYERS_QUERY_EXTENT_INVALID);
       sb_query.append(LAYERS_QUERY_FROM_V4);
       sb_query.append(LAYERS_QUERY_WHERE);
       sb_query.append(LAYERS_QUERY_ORDER_V4);
       LAYERS_QUERY_EXTENT_INVALID_V4 = sb_query.toString();
       // -------------------
       // end of building of Spatialite Queries
       // -------------------
       // RasterLite2 support - begin
       // -------------------
       sb_query = new StringBuilder();
       sb_query.append("SELECT DISTINCT ");
       sb_query.append("coverage_name"); // 0 of 1st field
       sb_query.append("||';'||compression"); // 1 of 1st field
       sb_query.append("||';'||'RasterLite2'"); //  2 of 1st field
       sb_query.append("||';'||REPLACE(title,';','-')"); // 3 of 1st field
       sb_query.append("||';'||REPLACE(abstract,';','-')"); // 4 of 1st field
       sb_query.append(" AS vector_key,pixel_type"); // 0 of second field
       sb_query.append("||';'||tile_width"); // 2
       sb_query.append("||';'||srid"); // 3
       sb_query.append("||';'||horz_resolution||';' AS vector_data,"); // 4
       VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
       sb_query = new StringBuilder();
       // if the record is invalid, only this field will be null
       // 'vector_key' and 'vector_data' will be use to attempt to recover from the error.
       sb_query.append("num_bands"); // 0
       sb_query.append("||';'||extent_minx"); // 1.0
       sb_query.append("||','||extent_miny"); // 1.1
       sb_query.append("||','||extent_maxx"); // 1.2
       sb_query.append("||','||extent_maxy"); // 1.3
       sb_query.append("||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now') AS vector_extent"); // 2
       VECTOR_LAYERS_QUERY_EXTENT_VALID = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(" FROM " + METADATA_RASTERLITE2_RASTER_COVERAGES_TABLE_NAME);
       VECTOR_LAYERS_QUERY_FROM = sb_query.toString();
       sb_query = new StringBuilder();
       // first Views (Spatialview) then tables (SpatialTable), then Table-Name/Column
       sb_query.append(" ORDER BY coverage_name ASC");
       sb_query.append(",title ASC");
       VECTOR_LAYERS_QUERY_ORDER = sb_query.toString();
       sb_query = new StringBuilder();
       // if the SELECT RL2_LoadRaster(...) was not executed,
       // - a record may exist with 'statistics and extent=NULL':
       // - this is an invalid record and must be ignored
       sb_query.append(" WHERE (statistics IS NOT NULL)");
       sb_query.append(" AND (extent_minx IS NOT NULL)");
       sb_query.append(" AND (extent_miny IS NOT NULL)");
       sb_query.append(" AND (extent_maxx IS NOT NULL)");
       sb_query.append(" AND (extent_maxy IS NOT NULL)");
       VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       RASTER_COVERAGES_QUERY_EXTENT_VALID_V42 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       RASTER_COVERAGES_QUERY_EXTENT_LIST_V42 = sb_query.toString();
       RASTER_COVERAGES_QUERY_EXTENT_LIST_V42=RASTER_COVERAGES_QUERY_EXTENT_LIST_V42.replace("AS vector_data,","AS vector_data"); // remove
       sb_query = new StringBuilder();
       // if the SELECT RL2_LoadRaster(...) was not executed,
       // - a record may exist with 'statistics and extent=NULL':
       // - this is an invalid record and must be ignored
       sb_query.append(" WHERE (statistics IS NULL)");
       sb_query.append(" OR (extent_minx IS NULL)");
       sb_query.append(" OR (extent_miny IS NULL)");
       sb_query.append(" OR (extent_maxx IS NULL)");
       sb_query.append(" OR (extent_maxy IS NULL)");
       VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
       sb_query = new StringBuilder();
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
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_INVALID);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42 = sb_query.toString();
       // -------------------
       // RasterLite2 support - end
       // -------------------
       // GeoPackage support - begin
       // -------------------
       sb_query = new StringBuilder();
       sb_query.append("SELECT DISTINCT ");
       sb_query.append("table_name"); // 0 of 1st field
       sb_query.append("||';'||CASE"); // 1 of 1st field
       sb_query.append(" WHEN data_type = 'features' THEN ("); // 1 of 1st field
       sb_query.append("SELECT column_name FROM gpkg_geometry_columns WHERE table_name = ''||table_name||''"); // 1 of 1st field
       sb_query.append(") WHEN data_type = 'tiles' THEN 'tile_data'"); // 1 of 1st field
       sb_query.append(" END"); // 1 of 1st field
       sb_query.append(" ||';'||CASE"); // 2 of 1st field
       sb_query.append(" WHEN data_type = 'features' THEN 'GeoPackage_features'"); // 2 of 1st field
       sb_query.append(" WHEN data_type = 'tiles' THEN 'GeoPackage_tiles'"); // 2 of 1st field
       sb_query.append(" END"); // 2 of 1st field
       sb_query.append("||';'||REPLACE(identifier,';','-')"); // 3 of second field
       sb_query.append("||';'||REPLACE(description,';','-') AS vector_key,"); // 4 of second field
       // fromosm_tiles;tile_data;GeoPackage_tiles;© OpenStreetMap contributors, See http://www.openstreetmap.org/copyright;OSM Tiles;
       // geonames;geometry;GeoPackage_features;Data from http://www.geonames.org/, under Creative Commons Attribution 3.0 License;Geonames;
       sb_query.append("CASE"); // 0 of second field
       sb_query.append(" WHEN data_type = 'features' THEN ("); // 0 of second field
       sb_query.append(""); // 0 of second field
       // Now the horror begins ...
       LAYERS_QUERY_BASE_V3="SELECT geometry_type_name FROM gpkg_geometry_columns WHERE table_name = ''||table_name||''"; // 0 of second field
       sb_query.append("CASE WHEN ("+LAYERS_QUERY_BASE_V3+") = 'GEOMETRY' THEN '0'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'POINT' THEN '1'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'LINESTRING' THEN '2'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'POLYGON' THEN '3'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'MULTIPOINT' THEN '4'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'MULTILINESTRING' THEN '5'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'MULTIPOLYGON' THEN '6'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = 'GEOMETRYCOLLECTION' THEN '7' END");
       // ... to be continued ...
       sb_query.append(") WHEN data_type = 'tiles' THEN ("); // 1 of 1st field
       sb_query.append("SELECT min(zoom_level) FROM gpkg_tile_matrix WHERE table_name = ''||table_name||''"); // 1 of second field
       sb_query.append(") END"); // 0 of second field
       sb_query.append("||';'||CASE"); // 1 of second field
       sb_query.append(" WHEN data_type = 'features' THEN ("); // 1 of second field
       // ... and now for something completely different ...
       LAYERS_QUERY_BASE_V3="SELECT z||','||m FROM gpkg_geometry_columns WHERE table_name = ''||table_name||''"; // 1 of second field
       sb_query.append("CASE WHEN ("+LAYERS_QUERY_BASE_V3+") = '0,0' THEN '2'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = '1,0' THEN '3'");
       sb_query.append(" WHEN ("+LAYERS_QUERY_BASE_V3+") = '1,1' THEN '4' END");
       // ... ich habe fertig.
       sb_query.append(") WHEN data_type = 'tiles' THEN ("); // 1 of second field
       sb_query.append("SELECT max(zoom_level) FROM gpkg_tile_matrix WHERE table_name = ''||table_name||''"); // 1 of second field
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
       sb_query = new StringBuilder();
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
       sb_query = new StringBuilder();
       sb_query.append(" FROM " + METADATA_GEOPACKAGE_TABLE_NAME);
       VECTOR_LAYERS_QUERY_FROM = sb_query.toString();
       sb_query = new StringBuilder();
       // condition not known when this is NOT true
       // - this is an invalid record and must be ignored
       sb_query.append(" WHERE (last_change IS NOT NULL)");
       sb_query.append(" AND (min_x IS NOT NULL)");
       sb_query.append(" AND (min_y IS NOT NULL)");
       sb_query.append(" AND (max_x IS NOT NULL)");
       sb_query.append(" AND (max_y IS NOT NULL)");
       VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
       sb_query = new StringBuilder();
       // first Views (Spatialview) then tables (SpatialTable), then Table-Name/Column
       sb_query.append(" ORDER BY table_name ASC");
       sb_query.append(",identifier ASC");
       VECTOR_LAYERS_QUERY_ORDER = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_VALID);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       GEOPACKAGE_QUERY_EXTENT_VALID_R10 = sb_query.toString();
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       GEOPACKAGE_QUERY_EXTENT_LIST_R10 = sb_query.toString();
       GEOPACKAGE_QUERY_EXTENT_LIST_R10=GEOPACKAGE_QUERY_EXTENT_LIST_R10.replace("AS vector_data,","AS vector_data"); // remove
       sb_query = new StringBuilder();
      // if the SELECT RL2_LoadRaster(...) was not executed,
       // - a record may exist with 'statistics and extent=NULL':
       // - this is an invalid record and must be ignored
       sb_query.append(" WHERE (last_change IS NULL)");
       sb_query.append(" OR (min_x IS NULL)");
       sb_query.append(" OR (min_y IS NULL)");
       sb_query.append(" OR (max_x IS NULL)");
       sb_query.append(" OR (max_y IS NULL)");
       VECTOR_LAYERS_QUERY_WHERE = sb_query.toString();
       sb_query = new StringBuilder();
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
       sb_query = new StringBuilder();
       sb_query.append(VECTOR_LAYERS_QUERY_BASE);
       sb_query.append(VECTOR_LAYERS_QUERY_EXTENT_INVALID);
       sb_query.append(VECTOR_LAYERS_QUERY_FROM);
       sb_query.append(VECTOR_LAYERS_QUERY_WHERE);
       sb_query.append(VECTOR_LAYERS_QUERY_ORDER);
       GEOPACKAGE_QUERY_EXTENT_INVALID_R10 = sb_query.toString();
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
     * General Function to create jsqlite.Database with spatialite support.
     * <ol>
     * <li> parent directories will be created, if needed</li>
     * <li> needed Tables/View and default values for metadata-table will be created</li>
     * </ol>
     *
     * @param databasePath name of Database file to create
     * @return sqlite_db: pointer to Database created
     * @throws IOException  if something goes wrong.
     */
    public static Database createDb( String databasePath ) throws IOException {
        Database spatialiteDatabase = null;
        File file_db = new File(databasePath);
        if (!file_db.getParentFile().exists()) {
            File dir_db = file_db.getParentFile();
            if (!dir_db.mkdir()) {
                throw new IOException("DaoSpatialite: create_db: dir_db[" + dir_db.getAbsolutePath() + "] creation failed"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        spatialiteDatabase = new jsqlite.Database();
        if (spatialiteDatabase != null) {
            try {
                spatialiteDatabase.open(file_db.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                        | jsqlite.Constants.SQLITE_OPEN_CREATE);
                createSpatialiteDb(spatialiteDatabase, 0); // i_rc should be 4
            } catch (jsqlite.Exception e_stmt) {
                GPLog.androidLog(4, "DaoSpatialite: create_spatialite[spatialite] dir_file[" + file_db.getAbsolutePath() //$NON-NLS-1$
                        + "]", e_stmt); //$NON-NLS-1$
            }
        }
        return spatialiteDatabase;
    }

    /**
     * General Function to create jsqlite.Database with spatialite support.
     *
     * <ol>
     * <li> parent directories will be created, if needed</li>
     * <li> needed Tables/View and default values for metadata-table will be created</li>
     * </ol>
     * @param sqliteDatabase pointer to Database
     * @param i_parm 0=new Database - skip checking if it a spatialite Database ; check Spatialite Version
     * @return i_rc: pointer to Database created
     * @throws Exception  if something goes wrong.
     */
    public static int createSpatialiteDb( Database sqliteDatabase, int i_parm ) throws Exception {
        int i_rc = 0;
        if (i_parm == 1) {
            /*
             * 0=not a spatialite version ;
             * 1=until 2.3.1 ;
             * 2=until 2.4.0 ;
             * 3=until 3.1.0-RC2 ;
             * 4=after 4.0.0-RC1
             */
            int i_spatialite_version = getSpatialiteDatabaseVersion(sqliteDatabase, "");
            if (i_spatialite_version > 0) { // this is a spatialite Database, do not create
                i_rc = 1;
                if (i_spatialite_version < 3) {
                    // TODO: logic for conversion to latest Spatialite
                    // Version [open]
                    throw new Exception("Spatialite version < 3 not supported.");
                }
            }
        }
        if (i_rc == 0) {
            String s_sql_command = "SELECT InitSpatialMetadata();"; //$NON-NLS-1$
            try {
                sqliteDatabase.exec(s_sql_command, null);
            } catch (jsqlite.Exception e_stmt) {
                i_rc = sqliteDatabase.last_error();
                GPLog.androidLog(4, "DaoSpatialite: create_spatialite sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt); //$NON-NLS-1$ //$NON-NLS-2$
            }
            // GPLog.androidLog(2,
            // "DaoSpatialite: create_spatialite sql["+s_sql_command+"] rc="+i_rc+"]");
            i_rc = getSpatialiteDatabaseVersion(sqliteDatabase, ""); //$NON-NLS-1$
            if (i_rc < 3) { // error, should be 3 or 4
                GPLog.androidLog(4, "DaoSpatialite: create_spatialite spatialite_version[" + i_rc + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return i_rc;
    }

    /**
    * Checks if a table exists.
    *
    * @param database the db to use.
    * @param name the table name to check.
    * @return the number of columns, if the table exists or 0 if the table doesn't exist.
    * @throws Exception if something goes wrong.
    */
    public static int checkTableExistence( Database database, String name ) throws Exception {
        String checkTableQuery = "SELECT sql  FROM sqlite_master WHERE type='table' AND name='" + name + "';";
        Stmt statement = null;
        try {
            statement = database.prepare(checkTableQuery);
            if (statement.step()) {
                String creationSql = statement.column_string(0);
                if (creationSql != null) {
                    String[] split = creationSql.trim().split("\\(|\\)");
                    if (split.length != 2) {
                        throw new RuntimeException("Can't parse creation sql: " + creationSql);
                    }

                    String fieldsString = split[1];
                    String[] fields = fieldsString.split(",");
                    return fields.length;
                }
            }
            return 0;
        } finally {
            statement.close();
        }
    }

    /**
     * Create the properties table.
     *
     * @param database the db to use.
     * @throws Exception  if something goes wrong.
     */
    public static void createPropertiesTable( Database database ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(PROPERTIESTABLE);
        sb.append(" (");
        sb.append(ID);
        sb.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sb.append(NAME).append(" TEXT, ");
        sb.append(SIZE).append(" REAL, ");
        sb.append(FILLCOLOR).append(" TEXT, ");
        sb.append(STROKECOLOR).append(" TEXT, ");
        sb.append(FILLALPHA).append(" REAL, ");
        sb.append(STROKEALPHA).append(" REAL, ");
        sb.append(SHAPE).append(" TEXT, ");
        sb.append(WIDTH).append(" REAL, ");
        sb.append(LABELSIZE).append(" REAL, ");
        sb.append(LABELFIELD).append(" TEXT, ");
        sb.append(LABELVISIBLE).append(" INTEGER, ");
        sb.append(ENABLED).append(" INTEGER, ");
        sb.append(ORDER).append(" INTEGER,");
        sb.append(DASH).append(" TEXT,");
        sb.append(MINZOOM).append(" INTEGER,");
        sb.append(MAXZOOM).append(" INTEGER,");
        sb.append(DECIMATION).append(" REAL");
        sb.append(" );");
        String query = sb.toString();
        database.exec(query, null);
    }

    /**
     * Create a default properties table for a spatial table.
     *
     * @param database the db to use.
     * @param spatialTableUniqueName the spatial table's unique name to create the property record for.
     * @return the created style object.
     * @throws Exception  if something goes wrong.
     */
    public static Style createDefaultPropertiesForTable( Database database, String spatialTableUniqueName, String spatialTableLabelField ) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into ").append(PROPERTIESTABLE);
        sbIn.append(" ( ");
        sbIn.append(NAME).append(" , ");
        sbIn.append(SIZE).append(" , ");
        sbIn.append(FILLCOLOR).append(" , ");
        sbIn.append(STROKECOLOR).append(" , ");
        sbIn.append(FILLALPHA).append(" , ");
        sbIn.append(STROKEALPHA).append(" , ");
        sbIn.append(SHAPE).append(" , ");
        sbIn.append(WIDTH).append(" , ");
        sbIn.append(LABELSIZE).append(" , ");
        sbIn.append(LABELFIELD).append(" , ");
        sbIn.append(LABELVISIBLE).append(" , ");
        sbIn.append(ENABLED).append(" , ");
        sbIn.append(ORDER).append(" , ");
        sbIn.append(DASH).append(" ,");
        sbIn.append(MINZOOM).append(" ,");
        sbIn.append(MAXZOOM).append(" ,");
        sbIn.append(DECIMATION);
        sbIn.append(" ) ");
        sbIn.append(" values ");
        sbIn.append(" ( ");
        Style style = new Style();
        style.name = spatialTableUniqueName;
        style.labelfield = spatialTableLabelField;
        sbIn.append(style.insertValuesString());
        sbIn.append(" );");

        String insertQuery = sbIn.toString();
        database.exec(insertQuery, null);

        return style;
    }

    /**
     * Deletes the style properties table.
     *
     * @param database the db to use.
     * @throws Exception  if something goes wrong.
     */
    public static void deleteStyleTable( Database database ) throws Exception {
        GPLog.androidLog(-1, "Resetting style table for: " + database.getFilename());
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("drop table if exists " + PROPERTIESTABLE + ";");

        String selectQuery = sbSel.toString();
        Stmt stmt = database.prepare(selectQuery);
        try {
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    /**
     * Update the style name in the properties table.
     *
     * @param database the db to use.
     * @param name the new name.
     * @param id the record id of the style.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyleName( Database database, String name, long id ) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        sbIn.append(NAME).append("='").append(name).append("'");
        sbIn.append(" where ");
        sbIn.append(ID);
        sbIn.append("=");
        sbIn.append(id);

        String updateQuery = sbIn.toString();
        database.exec(updateQuery, null);
    }

    /**
     * Update a style definition.
     *
     * @param database the db to use.
     * @param style the {@link Style} to set.
     * @throws Exception  if something goes wrong.
     */
    public static void updateStyle( Database database, Style style ) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        // sbIn.append(NAME).append("='").append(style.name).append("' , ");
        sbIn.append(SIZE).append("=").append(style.size).append(" , ");
        sbIn.append(FILLCOLOR).append("='").append(style.fillcolor).append("' , ");
        sbIn.append(STROKECOLOR).append("='").append(style.strokecolor).append("' , ");
        sbIn.append(FILLALPHA).append("=").append(style.fillalpha).append(" , ");
        sbIn.append(STROKEALPHA).append("=").append(style.strokealpha).append(" , ");
        sbIn.append(SHAPE).append("='").append(style.shape).append("' , ");
        sbIn.append(WIDTH).append("=").append(style.width).append(" , ");
        sbIn.append(LABELSIZE).append("=").append(style.labelsize).append(" , ");
        sbIn.append(LABELFIELD).append("='").append(style.labelfield).append("' , ");
        sbIn.append(LABELVISIBLE).append("=").append(style.labelvisible).append(" , ");
        sbIn.append(ENABLED).append("=").append(style.enabled).append(" , ");
        sbIn.append(ORDER).append("=").append(style.order).append(" , ");
        sbIn.append(DASH).append("='").append(style.dashPattern).append("' , ");
        sbIn.append(MINZOOM).append("=").append(style.minZoom).append(" , ");
        sbIn.append(MAXZOOM).append("=").append(style.maxZoom).append(" , ");
        sbIn.append(DECIMATION).append("=").append(style.decimationFactor);
        sbIn.append(" where ");
        sbIn.append(NAME);
        sbIn.append("='");
        sbIn.append(style.name);
        sbIn.append("';");

        String updateQuery = sbIn.toString();
        database.exec(updateQuery, null);
    }

    /**
     * Retrieve the {@link Style} for a given table.
     *
     * @param database the db to use.
     * @param spatialTableUniqueName the table name.
     * @return the style.
     * @throws Exception  if something goes wrong.
     */
    public static Style getStyle4Table( Database database, String spatialTableUniqueName, String spatialTableLabelField ) throws Exception {
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(ID).append(" , ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(LABELSIZE).append(" , ");
        sbSel.append(LABELFIELD).append(" , ");
        sbSel.append(LABELVISIBLE).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DASH).append(" , ");
        sbSel.append(MINZOOM).append(" , ");
        sbSel.append(MAXZOOM).append(" , ");
        sbSel.append(DECIMATION);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);
        sbSel.append(" where ");
        sbSel.append(NAME).append(" ='").append(spatialTableUniqueName).append("';");

        String selectQuery = sbSel.toString();
        Stmt stmt = database.prepare(selectQuery);
        Style style = null;
        try {
            if (stmt.step()) {
                style = new Style();
                style.name = spatialTableUniqueName;
                style.id = stmt.column_long(0);
                style.size = (float) stmt.column_double(1);
                style.fillcolor = stmt.column_string(2);
                style.strokecolor = stmt.column_string(3);
                style.fillalpha = (float) stmt.column_double(4);
                style.strokealpha = (float) stmt.column_double(5);
                style.shape = stmt.column_string(6);
                style.width = (float) stmt.column_double(7);
                style.labelsize = (float) stmt.column_double(8);
                style.labelfield = stmt.column_string(9);
                style.labelvisible = stmt.column_int(10);
                style.enabled = stmt.column_int(11);
                style.order = stmt.column_int(12);
                style.dashPattern = stmt.column_string(13);
                style.minZoom = stmt.column_int(14);
                style.maxZoom = stmt.column_int(15);
                style.decimationFactor = (float) stmt.column_double(16);
            }
        } finally {
            stmt.close();
        }

        if (style == null) {
            style = createDefaultPropertiesForTable(database, spatialTableUniqueName,spatialTableLabelField);
        }

        return style;
    }
    /**
     * Retrieve the {@link Style} for all tables of a db.
     *
     * @param database the db to use.
     * @return the list of styles or <code>null</code> if something went wrong.
     */
    public static List<Style> getAllStyles( Database database ) {
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(ID).append(" , ");
        sbSel.append(NAME).append(" , ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(LABELSIZE).append(" , ");
        sbSel.append(LABELFIELD).append(" , ");
        sbSel.append(LABELVISIBLE).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DASH).append(" , ");
        sbSel.append(MINZOOM).append(" , ");
        sbSel.append(MAXZOOM).append(" , ");
        sbSel.append(DECIMATION);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);

        String selectQuery = sbSel.toString();
        Stmt stmt = null;
        try {
            stmt = database.prepare(selectQuery);
            List<Style> stylesList = new ArrayList<Style>();
            while( stmt.step() ) {
                Style style = new Style();
                style.id = stmt.column_long(0);
                style.name = stmt.column_string(1);
                style.size = (float) stmt.column_double(2);
                style.fillcolor = stmt.column_string(3);
                style.strokecolor = stmt.column_string(4);
                style.fillalpha = (float) stmt.column_double(5);
                style.strokealpha = (float) stmt.column_double(6);
                style.shape = stmt.column_string(7);
                style.width = (float) stmt.column_double(8);
                style.labelsize = (float) stmt.column_double(9);
                style.labelfield = stmt.column_string(10);
                style.labelvisible = stmt.column_int(11);
                style.enabled = stmt.column_int(12);
                style.order = stmt.column_int(13);
                style.dashPattern = stmt.column_string(14);
                style.minZoom = stmt.column_int(15);
                style.maxZoom = stmt.column_int(16);
                style.decimationFactor = (float) stmt.column_double(17);
                stylesList.add(style);
            }
            return stylesList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
     /**
     * Return info of supported versions.
     * - will be filled on first Database connection when empty
     * -- called in checkDatabaseTypeAndValidity
     */
     public static String JavaSqliteDescription = "";
    /**
     * Return info of supported versions in JavaSqlite.
     *
     * <br>- SQLite used by the Database-Driver
     * <br>- Spatialite
     * <br>- Proj4
     * <br>- Geos
     * <br>-- there is no Spatialite function to retrieve the Sqlite version
     * <br>-- the Has() functions do not work with spatialite 3.0.1
     * - this must always be called when checkDatabaseTypeAndValidity runs the first time
     * @param database the db to use.
     * @param name a name for the log.
     * @return info of supported versions in JavaSqlite.
     */
    public static String getJavaSqliteDescription( Database database, String name ) {
        if (JavaSqliteDescription.equals(""))
        {
         try {
            // s_javasqlite_description = "javasqlite[" + getJavaSqliteVersion() + "],";
            JavaSqliteDescription = "sqlite[" + getSqliteVersion(database) + "],";
            JavaSqliteDescription += "spatialite[" + getspatialiteVersion(database) + "],";
            JavaSqliteDescription += "proj4[" + getProj4Version(database) + "],";
            JavaSqliteDescription += "geos[" + getGeosVersion(database) + "],";
            JavaSqliteDescription += "spatialite_properties[" + getSpatialiteProperties(database) + "],";
            JavaSqliteDescription += "rasterlite2_properties[" + getRaster2Version(database) + "]]";
         } catch (Exception e) {
            if (i_SpatialiteVersion > 3)
            {
             JavaSqliteDescription += "rasterlite2_properties[none]]";
            }
            else
            {
             JavaSqliteDescription += "exception[? not a spatialite database, or spatialite < 4 ?]]";
             GPLog.androidLog(4, "DaoSpatialite[" + name + "].getJavaSqliteDescription[" + JavaSqliteDescription
                    + "]", e);
            }
         }
        } 
        return JavaSqliteDescription;
    }
     /**
     * Return SQLite version number as string.
     * - as used by the Driver that queries for Spatialite
     * @param database the db to use.
     * @return the version of sqlite.
     * @throws Exception  if something goes wrong.
     */
    public static String getSqliteVersion( Database database ) throws Exception {
        try {
                return database.dbversion();
        } finally {
        }
    }

    /**
     * Get the version of JavaSqlite.
     *
     * <p>known values: 20120209,20131124 as int
     *
     * @return the version of JavaSqlite in 'Constants.drv_minor'.
     */
    public static String getJavaSqliteVersion() {
        return "" + Constants.drv_minor;
    }
    public static int i_SpatialiteVersion = 0;
    /**
     * Get the version of Spatialite.
     *
     * @param database the db to use.
     * @return the version of Spatialite.
     * @throws Exception  if something goes wrong.
     */
    public static String getspatialiteVersion( Database database ) throws Exception {
        Stmt stmt = database.prepare("SELECT spatialite_version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                if (i_SpatialiteVersion == 0)
                {
                 i_SpatialiteVersion=Integer.parseInt(value.substring(0,1));
                }
                return value;
            }
        } finally {
            stmt.close();
        }
        return "-";
    }
    /**
     * Return info of Rasterlite2
     * - will be filled on first Database connection when empty
     * -- called in checkDatabaseTypeAndValidity
     * --- if this is empty, then the Driver has NOT been compiled for RasterLite2
     *  '0.8;x86_64-linux-gnu'
     */
     public static String Rasterlite2Version_CPU = "";
    /**
     * Get the version of Rasterlite2 with cpu-type.
     * - used by: mapsforge.mapsdirmanager.sourcesview.SourcesTreeListActivity 
     * -- to prevent RaterLite2 button being shown when empty
     * note: this is returning the version number of the first static lib being compilrd into it
     * - 2014-05-22: libpng 1.6.10
     * @param database the db to use.
     * @return the version of Spatialite.
     * @throws Exception  if something goes wrong.
     */
    public static String getRaster2Version( Database database ) throws Exception {
        Stmt stmt = database.prepare("SELECT RL2_Version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                if (Rasterlite2Version_CPU.equals(""))
                {
                 Rasterlite2Version_CPU=value;
                }
                return value;
            }
        }
        finally {
            stmt.close();
        }
        return "";
    }

    /**
     * Return hasGeoPackage
     * - can AutoGPKGStart() and AutoGPKGStop() be used
     * -- VirtualGPKG
     * --- needed to retrieve/update GPKG geometry tables as if they are spatialite tables
     */
     public static boolean hasGeoPackage = false;

    /**
     * Get the properties of Spatialite.
     *
     * <br>- use the known 'SELECT Has..' functions
     * <br>- when HasIconv=0: no VirtualShapes,VirtualXL
     *
     * @param database the db to use.
     * @return the properties of Spatialite.
     * @throws Exception  if something goes wrong.
     */
    public static String getSpatialiteProperties( Database database ) throws Exception {
        String s_value = "-";
        Stmt stmt = database
                .prepare("SELECT HasIconv(),HasMathSql(),HasGeoCallbacks(),HasProj(),HasGeos(),HasGeosAdvanced(),HasGeosTrunk(),HasLwGeom(),HasLibXML2(),HasEpsg(),HasFreeXL();");
        try {
            if (stmt.step()) {
                s_value = "HasIconv[" + stmt.column_int(0) + "],HasMathSql[" + stmt.column_int(1) + "],HasGeoCallbacks["
                        + stmt.column_int(2) + "],";
                s_value += "HasProj[" + stmt.column_int(3) + "],HasGeos[" + stmt.column_int(4) + "],HasGeosAdvanced["
                        + stmt.column_int(5) + "],";
                s_value += "HasGeosTrunk[" + stmt.column_int(6) + "],HasLwGeom[" + stmt.column_int(7) + "],HasLibXML2["
                        + stmt.column_int(8) + "],";
                s_value += "HasEpsg[" + stmt.column_int(9) + "],HasFreeXL[" + stmt.column_int(10) + "]";
            }
        } finally {
            stmt.close();
        }
        try { // since spatialite 4.2.0-rc1
            stmt = database.prepare("SELECT HasGeoPackage(),spatialite_target_cpu();");
            if (stmt.step()) {
                if (stmt.column_int(0)==1)
                 hasGeoPackage=true;
                s_value += ",HasGeoPackage[" + stmt.column_int(0) + "],target_cpu[" + stmt.column_string(1) + "]";
            }
        } finally {
            stmt.close();
        }
        return s_value;
    }

    /**
     * Get the version of proj.
     *
     * @param database the db to use.
     * @return the version of proj.
     * @throws Exception  if something goes wrong.
     */
    public static String getProj4Version( Database database ) throws Exception {
        Stmt stmt = database.prepare("SELECT proj4_version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                return value;
            }
        } finally {
            stmt.close();
        }
        return "-";
    }

    /**
     * Get the version of geos.
     *
     * @param database the db to use.
     * @return the version of geos.
     * @throws Exception  if something goes wrong.
     */
    public static String getGeosVersion( Database database ) throws Exception {
        Stmt stmt = database.prepare("SELECT geos_version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                return value;
            }
        } finally {
            stmt.close();
        }
        return "-";
    }

     /**
     * Attemt to count Triggers for a specific Table.
     * returned the number of Triggers 
     * - SpatialView read_only should be set to 0, if result is 0
     * -- called when SpatialView read_only = 1 in getViewRowid
     * --- a SpatialView with out INSERT,UPDATE and DELETE tringgers is invalid
     * --- there is no way to check if these triggers really work correctly
     * --- this the reason why writable Views can be VERY dangerous
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return count of Triggers found
     * @throws Exception  if something goes wrong.
     */
    private static int spatialiteCountTriggers( Database database, String table_name,SpatialiteDatabaseType databaseType) throws Exception {
        int  i_count=0;
        if (table_name.equals(""))
         return i_count;
        String s_CountTriggers = "SELECT count(name) FROM sqlite_master WHERE (type = 'trigger' AND tbl_name= '" + table_name + "');";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CountTriggers);
            if (statement.step()) {
                i_count = statement.column_int(0);
                return i_count;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteCountTriggers["+databaseType+"] sql["+s_CountTriggers+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_count;
    }

    /**
     * Get the Primary key of the SpatialView and read_only parameter.
     * - check if writable Views has at least 3 triggers, set to read only if not
     * @param database the db to use.
     * @param table_name the view of the db to use.
     * @param databaseType SPATIALITE4 for version of spatialite that have a 'read_only' field.
     * @return formatted version for map.key
     * @throws Exception  if something goes wrong.
     */
    private static String getViewRowid( Database database , String table_name, SpatialiteDatabaseType databaseType ) throws Exception {
        String s_sql = "SELECT view_rowid,read_only FROM views_geometry_columns WHERE (view_name='"+table_name+"')";
        if (databaseType == SpatialiteDatabaseType.SPATIALITE3)
         s_sql = "SELECT view_rowid FROM views_geometry_columns WHERE (view_name='"+table_name+"')";
        String ROWID_PK="";
        Stmt statement = null;
        try {
         statement = database.prepare(s_sql);
         if ((statement != null) && (statement.column_count()) > 0)
         {
          if (statement.step()) {
           ROWID_PK = statement.column_string(0);
           int i_read_only = 0;
           if ((databaseType == SpatialiteDatabaseType.SPATIALITE4) && (statement.column_count() > 1))
           {
            i_read_only = statement.column_int(1);
            if (i_read_only == 1)
            { // it is not possible to check the validity of the triggers
             if (spatialiteCountTriggers(database,table_name,databaseType) < 3)
             { // there must be at least 3 triggers, the view CANNOT be writable
              i_read_only=0;
             }
            }
           }
           ROWID_PK=ROWID_PK+";"+i_read_only;
          //  GPLog.androidLog(-1, "DaoSpatialite:getViewRowid["+databaseType+"] ["+statement.column_count()+"] ROWID_PK["+ROWID_PK+"] sql["+s_sql+"] db[" + database.getFilename() + "]");
          }
         }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:getViewRowid["+databaseType+"] sql["+s_sql+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         statement.close();
        }
        return ROWID_PK;
    }

    /**
     * Attemt to retrieve row-count and bounds for this geometry field.
     * returned result:
     * - 'rows_count;min_x,min_y,max_x,max_y;datetimestamp_now'
     * -- sa_string=split(";") == 3 ; sa_string[1].split(',') == 4
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @return formatted string with 3rd field used in DaoSpatialite.checkDatabaseTypeAndValidity() [vector_extent]
     * @throws Exception  if something goes wrong.
     */
    public static String spatialiteRetrieveBounds( Database database, String table_name, String geometry_column) throws Exception {
        StringBuilder sb_query = new StringBuilder();
        String s_vector_extent="";
        // return the format used in DaoSpatialite.checkDatabaseTypeAndValidity()
        sb_query.append("SELECT count(");
        sb_query.append(geometry_column);
        sb_query.append(")||';'||Min(MbrMinX(");
        sb_query.append(geometry_column);
        sb_query.append("))||','||Min(MbrMinY(");
        sb_query.append(geometry_column);
        sb_query.append("))||','||Max(MbrMaxX(");
        sb_query.append(geometry_column);
        sb_query.append("))||','||Max(MbrMaxY(");
        sb_query.append(geometry_column);
        sb_query.append("))||';'||strftime('%Y-%m-%dT%H:%M:%fZ','now')");
        sb_query.append(" FROM ");
        sb_query.append(table_name);
        sb_query.append(";");
        // ;617;7255796.59288944,246133.478270624,7395508.96772464,520956.218508861;2014-03-26T06:32:58.572Z
        String s_select_bounds = sb_query.toString();
        Stmt statement = null;
        try {
            statement = database.prepare(s_select_bounds);
            if (statement.step()) {
              if (statement.column_string(0) != null)
              { // The geometries may be null, thus returns null
                s_vector_extent = statement.column_string(0);
                return s_vector_extent;
              }
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteRetrieveBounds sql["+s_select_bounds+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return s_vector_extent;
    }

     /**
     * Attemt to count geometry field.
     * returned the number of Geometries that are NOT NULL
     * - no recovery attemts should be done when this returns 0
     * -- called from getSpatialiteUpdateLayerStatistics
     * --- will abort attemts to recover if returns 0
     * --- this speeds up the loading by 50% in my case
     * VECTOR_LAYERS_QUERY_MODE=3 : about 5 seconds [before about 10 seconds]
     * VECTOR_LAYERS_QUERY_MODE=0 : about 2 seconds
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return count of Geometries NOT NULL
     * @throws Exception  if something goes wrong.
     */
    public static int spatialiteCountGeometries( Database database, String table_name, String geometry_column,SpatialiteDatabaseType databaseType) throws Exception {
        int  i_count=0;
        if ((table_name.equals("")) || (geometry_column.equals("")))
         return i_count;
        // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_CountGeometries = "SELECT count('" + geometry_column + "') FROM '" + table_name + "' WHERE '" + geometry_column + "' IS NOT NULL;";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CountGeometries);
            if (statement.step()) {
                i_count = statement.column_int(0);
                // GPLog.androidLog(-1,"DaoSpatialite:spatialiteRecoverSpatialIndex["+databaseType+"] db["+database.getFilename()+"] sql["+s_CreateSpatialIndex+"]  result: i_spatialindex["+i_spatialindex+"] ");
                return i_count;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteCountGeometries["+databaseType+"] sql["+s_CountGeometries+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_count;
    }

    /**
     * Retrieve rasterlite2 image of a given bound and size.
     * - used by: SpatialiteUtilities.rl2_GetMapImageTile to retrieve tiles only
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImage
     * @param sqlite_db Database connection to use
     * @param sourceSrid the srid (of the n/s/e/w positions).
     * @param destSrid the destination srid (of the rasterlite2 image).
     * @param table (coverageName) the table to use.
     * @param width of image in pixel.
     * @param height of image in pixel.
     * @param tileBounds [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param styleName used in coverage. default: 'default'
     * @param mimeType 'image/tiff' etc. default: 'image/png'
     * @param bgColor html-syntax etc. default: '#ffffff'
     * @param transparent 0 to 100 (?).
     * @param quality 0-100 (for 'image/jpeg')
     * @param reaspect 1 = adapt image width,height if needed based on given bounds
     * @return the image data as byte[]
     */
    public static byte[] rl2_GetMapImage( Database sqlite_db,String sourceSrid, String destSrid, String coverageName, int width, int height, double[] tileBounds, String styleName, String mimeType, String bgColor, int transparent, int quality, int reaspect ) {
        boolean doTransform = false;
        if (!sourceSrid.equals(destSrid)) {
            doTransform = true;
        }
        // sanity checks
        if (styleName.equals(""))
         styleName="default";
        if (mimeType.equals(""))
         mimeType="image/png";
        if (bgColor.equals(""))
         bgColor="#ffffff";
        if ((transparent < 0) || (transparent > 100))
         transparent=0;
        if ((quality < 0) || (quality > 100))
         quality=0;
        if ((reaspect < 0) || (reaspect > 1))
         reaspect=1; // adapt image width,height if needed based on given bounds [needed for tiles]
        StringBuilder mbrSb = new StringBuilder();
        if (doTransform)
            mbrSb.append("ST_Transform(");
        mbrSb.append("BuildMBR(");
        mbrSb.append(tileBounds[0]);
        mbrSb.append(",");
        mbrSb.append(tileBounds[1]);
        mbrSb.append(",");
        mbrSb.append(tileBounds[2]);
        mbrSb.append(",");
        mbrSb.append(tileBounds[3]);
        if (doTransform) {
            mbrSb.append(",");
            mbrSb.append(sourceSrid);
            mbrSb.append("),");
            mbrSb.append(destSrid);
        }
        mbrSb.append(")");
        // SELECT RL2_GetMapImage('berlin_postgrenzen.1890',BuildMBR(20800.0,22000.0,24000.0,19600.0),1200,1920,'default','image/png','#ffffff',0,0,1);
        String mbr = mbrSb.toString();
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT RL2_GetMapImage('");
        qSb.append(coverageName);
        qSb.append("',");
        qSb.append(mbr);
        qSb.append(",");
        qSb.append(Integer.toString(width));
        qSb.append(",");
        qSb.append(Integer.toString(height));
        qSb.append(",'");
        qSb.append(styleName);
        qSb.append("','");
        qSb.append(mimeType);
        qSb.append("','");
        qSb.append(bgColor);
        qSb.append("',");
        qSb.append(Integer.toString(transparent));
        qSb.append(",");
        qSb.append(Integer.toString(quality));
        qSb.append(",");
        qSb.append(Integer.toString(reaspect));
        qSb.append(");");
        String s_sql_command = qSb.toString();
        // GPLog.androidLog(-1, "DaoSpatialite: rl2_GetMapImage sql[" + s_sql_command + "]");
        Stmt this_stmt = null;
        byte[] ba_image=null;
        if (!Rasterlite2Version_CPU.equals(""))
        { // only if rasterlite2 driver is active
         try {
             this_stmt = sqlite_db.prepare(s_sql_command);
             if (this_stmt.step()) {
               ba_image = this_stmt.column_bytes(0);
            }
         } catch (jsqlite.Exception e_stmt) {
           /*
             this internal lib error is not being caught and the application crashes
             - the request was for a image 1/3 of the orignal size of 10607x8292 (3535x2764)
             - big images should be avoided, since the application dies
             'libc    : Fatal signal 11 (SIGSEGV) at 0x80c7a000 (code=1), thread 4216 (AsyncTask #2)'
             '/data/app-lib/eu.hydrologis.geopaparazzi-2/libjsqlite.so (rl2_raster_decode+8248)'
             'I WindowState: WIN DEATH: Window{41ee0100 u0 eu.hydrologis.geopaparazzi/eu.hydrologis.geopaparazzi.GeoPaparazziActivity}'
           */
           int i_rc = sqlite_db.last_error();
           GPLog.androidLog(4, "DaoSpatialite: rl2_GetMapImage sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
         }
         finally {
           // this_stmt.close();
         }
        }
        return ba_image;
    }


     /**
     * Attemt to create VirtualGPKG wrapper for GeoPackage geometry tables.
     * This function will inspect the DB layout, 
     * - then automatically creating/refreshing a VirtualGPKG wrapper for each GPKG geometry table
     * @param database the db to use.
     * @param i_stop 0=AutoGPKGStart ; 1=AutoGPKGStop
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return returns amount of tables effected
     * @throws Exception  if something goes wrong.
     */
    private static int spatialiteAutoGPKG( Database database, int i_stop,SpatialiteDatabaseType databaseType) throws Exception {
        int  i_count_tables=0;
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
                // GPLog.androidLog(-1,"DaoSpatialite:spatialiteAutoGPKG["+databaseType+"] db["+database.getFilename()+"] sql["+s_AutoGPKG+"]  result: i_count_tables["+i_count_tables+"] ");
                return i_count_tables;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteAutoGPKG["+databaseType+"] sql["+s_AutoGPKG+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_count_tables;
    }

     /**
     * Attemt to create GeoPackage-SpatialIndex for this geometry field.
     * returned if the SpatialIndex was created (and therefore useable) or not
     * - This should NOT be the default behavior, there may be a reason why no SpatialIndex was created
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception  if something goes wrong.
     */
    private static int spatialitegpkgAddSpatialIndex( Database database, String table_name, String geometry_column,SpatialiteDatabaseType databaseType) throws Exception {
        int  i_spatialindex=0;
        if ((table_name.equals("")) || (geometry_column.equals("")))
         return i_spatialindex;
       // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_CreateSpatialIndex = "SELECT gpkgAddSpatialIndex('" + table_name + "','" + geometry_column + "');";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CreateSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                // GPLog.androidLog(-1,"DaoSpatialite:gpkgAddSpatialIndex["+databaseType+"] db["+database.getFilename()+"] sql["+s_CreateSpatialIndex+"]  result: i_spatialindex["+i_spatialindex+"] ");
                return i_spatialindex;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:gpkgAddSpatialIndex["+databaseType+"] sql["+s_CreateSpatialIndex+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_spatialindex;
    }

     /**
     * Attemt to create SpatialIndex for this geometry field.
     * returned if the SpatialIndex was created (and therefore useable) or not
     * - This should NOT be the default behavior, there may be a reason why no SpatialIndex was created
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception  if something goes wrong.
     */
    private static int spatialiteCreateSpatialIndex( Database database, String table_name, String geometry_column,SpatialiteDatabaseType databaseType) throws Exception {
        int  i_spatialindex=0;
        if ((table_name.equals("")) || (geometry_column.equals("")))
         return i_spatialindex;
       // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_CreateSpatialIndex = "SELECT CreateSpatialIndex('" + table_name + "','" + geometry_column + "');";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CreateSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                // GPLog.androidLog(-1,"DaoSpatialite:spatialiteRecoverSpatialIndex["+databaseType+"] db["+database.getFilename()+"] sql["+s_CreateSpatialIndex+"]  result: i_spatialindex["+i_spatialindex+"] ");
                return i_spatialindex;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteCreateSpatialIndex["+databaseType+"] sql["+s_CreateSpatialIndex+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_spatialindex;
    }

     /**
     * Create Virtual-table 'SpatialIndex' if it does not exist.
     * - Note: only needed for pre-spatiallite 3.0 Databases.
     * @param database the db to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception  if something goes wrong.
     */
    private static int spatialiteVirtualSpatialIndex( Database database,SpatialiteDatabaseType databaseType ) throws Exception {
        String s_VirtualSpatialIndex = "CREATE VIRTUAL TABLE SpatialIndex USING VirtualSpatialIndex();";
        int i_spatialindex=0;
        Stmt statement = null;
        try {
            database.exec(s_VirtualSpatialIndex, null);
            s_VirtualSpatialIndex = "SELECT count(*) FROM sqlite_master WHERE name = 'SpatialIndex';";
            statement = database.prepare(s_VirtualSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                return i_spatialindex;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteVirtualSpatialIndex["+databaseType+"] sql["+s_VirtualSpatialIndex+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Attemt to execute a RecoverSpatialIndex for this geometry field or whole Database.
     * - Note: only for SpatialTable, SpatialViews ALWAYS returns 0.
     * - if table_name and geometry_column are empty: for whole Database
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param i_spatialindex 0=recover on when needed [default], 1=force rebuild.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception  if something goes wrong.
     */
    private static int spatialiteRecoverSpatialIndex( Database database, String table_name, String geometry_column, int i_spatialindex,SpatialiteDatabaseType databaseType ) throws Exception {
        String s_RecoverSpatialIndex = "SELECT RecoverSpatialIndex("+i_spatialindex+");";
        if ((!table_name.equals("")) &&  (!geometry_column.equals("")))
         s_RecoverSpatialIndex = "SELECT RecoverSpatialIndex('" + table_name + "','" + geometry_column + "',"+i_spatialindex+");";
        i_spatialindex=0;
        Stmt statement = null;
        try {
            statement = database.prepare(s_RecoverSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                // GPLog.androidLog(-1,"DaoSpatialite:spatialiteRecoverSpatialIndex["+databaseType+"] db["+database.getFilename()+"] sql["+s_RecoverSpatialIndex+"]  result: i_spatialindex["+i_spatialindex+"] ");
                return i_spatialindex;
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteRecoverSpatialIndex["+databaseType+"] sql["+s_RecoverSpatialIndex+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Attemt to execute a UpdateLayerStatistics for this geometry field or whole Database.
     * - Note: only for SpatialTable, SpatialViews ALWAYS returns 0.
     * - Note: only for VirtualTable, returns 2.
     * - if table_name and geometry_column are empty: for whole Database
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param i_spatialindex 0=recover on when needed [default], 1=force rebuild.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception  if something goes wrong.
     */
    private static int spatialiteUpdateLayerStatistics( Database database, String table_name, String geometry_column, int i_spatialindex ,SpatialiteDatabaseType databaseType) throws Exception {
        if (i_spatialindex == 1)
        {
         try {
          i_spatialindex=spatialiteRecoverSpatialIndex(database,table_name,geometry_column,0,databaseType);
         } finally {
         }
         if (i_spatialindex == 0)
          return i_spatialindex; // Invalid for use with geopaparazzi
        }
        i_spatialindex=0;
        boolean b_valid=false;
        String s_UpdateLayerStatistics = "SELECT UpdateLayerStatistics();";
        if ((!table_name.equals("")) &&  (!geometry_column.equals("")))
         s_UpdateLayerStatistics = "SELECT UpdateLayerStatistics('" + table_name + "','" + geometry_column + "');";
        Stmt statement = null;
        try {
            // when done here it, will catch sql-syntax errors
            statement = database.prepare(s_UpdateLayerStatistics);
            if (statement.step()) {
              i_spatialindex = statement.column_int(0);
              if (i_spatialindex == 1)
              {
               HashMap<String, String> fieldNamesToTypeMap=collectTableFields(database,"layer_statistics");
               if (fieldNamesToTypeMap.size() > 0)
               { // SpatialTable virts_layer_statistics             
                b_valid=true;
               }
               else
               {
                fieldNamesToTypeMap=collectTableFields(database,"virts_layer_statistics");
                if (fieldNamesToTypeMap.size() > 0)
                { // VirtualTable virts_layer_statistics            
                 b_valid=true;
                 i_spatialindex=2;
                }
               }
               if (!b_valid)
                i_spatialindex=0;
              }
              // GPLog.androidLog(-1,"DaoSpatialite:UpdateLayerStatistics["+databaseType+"] db["+database.getFilename()+"] sql["+s_UpdateLayerStatistics+"]  result: i_spatialindex["+i_spatialindex+"] ");
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:spatialiteUpdateLayerStatistics["+databaseType+"] sql["+s_UpdateLayerStatistics+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Attemt to execute a UpdateLayerStatistics for this geometry field and retrieve the bounds.
     * - if table_name and geometry_column are empty: for whole Database
     * @param database the db to use.
     * @param table_name the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param i_spatialindex check and try to recover the Spatial Index [0=no, 1=yes [default]].
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return the retrieved bounds data, if possible (vector_extent).
     * @throws Exception  if something goes wrong.
     */
    private static String getSpatialiteUpdateLayerStatistics( Database database, String table_name, String geometry_column, int i_spatialindex, SpatialiteDatabaseType databaseType ) throws Exception {
        String s_vector_extent="";
         if (spatialiteCountGeometries(database,table_name,geometry_column,databaseType) == 0)
          return s_vector_extent;
         if (i_spatialindex == 1)
         {
          try {
           i_spatialindex=spatialiteUpdateLayerStatistics(database,table_name,geometry_column,i_spatialindex,databaseType);
          } finally {
          }
          if (i_spatialindex != 1)
           return s_vector_extent; // Invalid for use with geopaparazzi
         }
        if ((!table_name.equals("")) &&  (!geometry_column.equals("")))
        {  // for table/geometry support, otherwise for whole Database (Spatialite3+4)
         // try to retrieve the needed bounds again
         String s_LAYERS_QUERY_EXTENT_VALID=VECTOR_LAYERS_QUERY_EXTENT_VALID_V4;
         String s_METADATA_LAYERS_STATISTICS_TABLE_NAME=METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME;
         if (databaseType == SpatialiteDatabaseType.SPATIALITE3)
         { // for pre-Spatialite 4 versions
          s_LAYERS_QUERY_EXTENT_VALID=LAYERS_QUERY_EXTENT_VALID_V3;
          s_METADATA_LAYERS_STATISTICS_TABLE_NAME=METADATA_LAYER_STATISTICS_TABLE_NAME;
         }
         StringBuilder sb_query = new StringBuilder();
         sb_query.append(" AND ((");
         sb_query.append(s_METADATA_LAYERS_STATISTICS_TABLE_NAME + ".table_name='");
         sb_query.append(table_name);
         sb_query.append("') AND (" + s_METADATA_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column='");
         sb_query.append(geometry_column);
         sb_query.append("'))");
         String VECTOR_LAYERS_QUERY_BASE = sb_query.toString();
         // insert the extra WHERE condition into the prepaired sql
         VECTOR_LAYERS_QUERY_BASE = s_LAYERS_QUERY_EXTENT_VALID.replace("ORDER BY",VECTOR_LAYERS_QUERY_BASE+" ORDER BY");
         // GPLog.androidLog(-1,"DaoSpatialite: getSpatialiteUpdateLayerStatistics["+databaseType+"][  i_spatialindex["+i_spatialindex+"]  VECTOR_LAYERS_QUERY_BASE["+VECTOR_LAYERS_QUERY_BASE+"]");
         Stmt statement=null;
         try {
            // when done here it, will catch sql-syntax errors
            statement = database.prepare(VECTOR_LAYERS_QUERY_BASE);
            if (statement.step()) {
               if (statement.column_string(2) != null)
               { // without further checking, consider this valid
                s_vector_extent=statement.column_string(2);
               }
            }
         }
         catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:getSpatialiteUpdateLayerStatistics["+databaseType+"] sql["+VECTOR_LAYERS_QUERY_BASE+"] db[" + database.getFilename() + "]", e_stmt);
         }
          finally {
             statement.close();
          }
          if (s_vector_extent.equals(""))
          { // Last attempt, if this does not work - then the geometry must be considered invalid
           s_vector_extent=spatialiteRetrieveBounds(database,table_name,geometry_column);
          }
         }
        return s_vector_extent;
    }

     /**
     * Attemt to correction of geometries in error .
     * - if table_name and geometry_column are empty: for whole Database
     * @param database the database to check.
     * @param spatialVectorMap the {@link HashMap} of Spatialite4+ Vector-data (Views/Tables Geometries) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid geometries.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return nothing
     * @throws Exception  if something goes wrong.
     */
    private static void getSpatialVectorMap_Errors( Database database, HashMap<String, String> spatialVectorMap , HashMap<String, String> spatialVectorMapErrors, SpatialiteDatabaseType databaseType ) throws Exception {
        HashMap<String, String> spatialVectorMapCorrections = new HashMap<String, String>();
        String vector_key=""; // term used when building the sql, used as map.key
        String vector_data=""; // term used when building the sql
        String vector_extent=""; // term used when building the sql
        String vector_value=""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name="";
        String geometry_column="";
        int i_spatialindex=1;
         // GPLog.androidLog(-1,"DaoSpatialite: getSpatialVectorMap_Errors["+databaseType+"] db["+database.getFilename()+"] spatialVectorMap["+spatialVectorMap.size()+"]  spatialVectorMapErrors["+spatialVectorMapErrors.size()+"] ");
        if ((VECTOR_LAYERS_QUERY_MODE > 0) && (spatialVectorMapErrors.size() > 0))
        {
         for( Map.Entry<String, String> vector_entry : spatialVectorMapErrors.entrySet() )
         {
          vector_key = vector_entry.getKey();
          // soldner_polygon;14;3;2;3068;1;20847.6171111586,18733.613614603,20847.6171111586,18733.613614603
          // vector_key[priority_marks_joined_lincoln;geometry;SpatialTable;ROWID;-1]
          vector_value = vector_entry.getValue();
          vector_data="";
          String[] sa_string = vector_key.split(";");
          if (sa_string.length == 5)
          {
           table_name=sa_string[0];
           geometry_column=sa_string[1];
           String s_layer_type=sa_string[2];
           String s_ROWID_PK=sa_string[3];
           int i_view_read_only = Integer.parseInt(sa_string[4]);
           sa_string = vector_value.split(";");
           if (sa_string.length == 7)
           { // vector_value[1;2;2913;1;row_count;extent_min_x,extent_min_y,extent_max_x,extent_max_y;last_verified]
            String s_geometry_type = sa_string[0];
            String s_coord_dimension=sa_string[1];
            String s_srid=sa_string[2];
            int i_spatial_index_enabled=Integer.parseInt(sa_string[3]); // should always be 1
            if ((i_spatial_index_enabled == 0) && (VECTOR_LAYERS_QUERY_MODE > 2))
            { // This should NOT be the default behavior, there may be a reason why no SpatialIndex was created
             i_spatial_index_enabled=spatialiteCreateSpatialIndex(database,table_name,geometry_column,databaseType);
            }
            vector_data=s_geometry_type+";"+s_coord_dimension+";"+s_srid+";"+i_spatial_index_enabled+";";
            int i_row_count=-1;
            if (!sa_string[4].equals("row_count"))
             i_row_count = Integer.parseInt(sa_string[4]);
            String s_bounds = sa_string[5];
            String s_last_verified=sa_string[6];
            if (s_bounds.equals("extent_min_x,extent_min_y,extent_max_x,extent_max_y"))
            {
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
            if (i_row_count != 0)
            {
             i_spatialindex=1;
             if (vector_key.indexOf("SpatialView") != -1)
             {
              i_spatialindex=0;
             }
             if ((VECTOR_LAYERS_QUERY_MODE == 1) && (i_spatialindex == 1))
             { // we do not try to query the dementions of faulty SpatialView's
              vector_extent=spatialiteRetrieveBounds(database,table_name,geometry_column);
             }
             if (VECTOR_LAYERS_QUERY_MODE > 1)
             {
              try
              {
                /* RecoverSpatialIndex will be done if needed
                 UpdateLayerStatistics will then be called
                 afterwhich 2 attemts will be made to return valid result
                 - if empty: geometry is to be considered invalid
                */
                vector_extent=getSpatialiteUpdateLayerStatistics(database,table_name,geometry_column,i_spatialindex,databaseType);
              }
              finally
              {
              }
             }
            }
            if (!vector_extent.equals(""))
            { // all of the geomtries of this column may be NULL, thus unusable - do not add when 'vector_extent' is empty
             if (vector_key.indexOf("SpatialView") != -1)
             {
              try { // replace placeholder with used primary-key and read_only parameter of SpatialView
               String ROWID_PK=getViewRowid(database,table_name,databaseType);
               vector_key = vector_key.replace("ROWID;-1",ROWID_PK);
              }
              catch (Exception e) {
               GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_Errors["+databaseType+"] vector_key["+vector_key+"] db[" + database.getFilename() + "]", e);
              }
              finally {
             }
            }
            // one way or another, we have resolved the faulty geometry, add to the valid list
            spatialVectorMap.put(vector_key,vector_data+vector_extent);
            if (VECTOR_LAYERS_QUERY_MODE > 1)
            { // remove from the errors, since they may have been permanently resolved, but not here
             spatialVectorMapCorrections.put(vector_entry.getKey(),vector_entry.getValue());
            }
            // GPLog.androidLog(-1,"DaoSpatialite: getSpatialVectorMap_Errors[resolved]["+VECTOR_LAYERS_QUERY_MODE+"]  vector_key["+vector_key+"]  vector_value["+vector_value+"] vector_extent["+vector_extent+"]");
           }
           else
           {
             // GPLog.androidLog(-1,"DaoSpatialite: getSpatialVectorMap_Errors[not resolved]["+VECTOR_LAYERS_QUERY_MODE+"]  vector_key["+vector_key+"]  vector_value["+vector_value+"] vector_extent["+vector_extent+"]");
           }
          }
         }
        }
        // remove from the errors, since they may have been permanently resolved
        for( Map.Entry<String, String> vector_entry : spatialVectorMapCorrections.entrySet() )
        { // hopefully arrivederci to the 'Error: null' alerts
          try {
           spatialVectorMapErrors.remove(vector_entry.getKey());
          }
          catch (java.lang.Exception e) {
            GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_Errors["+databaseType+"] vector_key["+vector_key+"] db[" + database.getFilename() + "]", e);
          }
          finally
          {
          }
        }
       }
    }

    /**
     * Read Spatial-Geometries for pre-Spatialite 4.* specific Databases (2.4.0-3.1.0)
     *
     * @param database the database to check.
     * @param spatialVectorMap the {@link HashMap} of Spatialite4+ Vector-data (Views/Tables Geometries) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid geometries.
     * @param b_layers_statistics if a ayers_statistics had been found
     * @return nothing
     * @throws Exception  if something goes wrong.
     */
    private static void getSpatialVectorMap_V3( Database database, HashMap<String, String> spatialVectorMap , HashMap<String, String> spatialVectorMapErrors, boolean b_layers_statistics,boolean b_SpatialIndex ) throws Exception {
        int i_spatialindex=0;
         //GPLog.androidLog(-1,"-I-> DaoSpatialite: getSpatialVectorMap_V3["+database.getFilename()+"] b_layers_statistics["+b_layers_statistics+"] ");
        if (!b_SpatialIndex)
        { // pre-spatilite 3.0 Database may not have this Virtual-Table, it must be created to query the geometrys using the SpatialIndex
         i_spatialindex=spatialiteVirtualSpatialIndex(database,SpatialiteDatabaseType.SPATIALITE3);
         if (i_spatialindex == 0)
         { // if this fails then we may have to consider this Database invalid
           return;
         }
        }
        if (!b_layers_statistics)
        { // if layers_statistics does not exist a UpdateLayerStatistics() is needed for the whole Database
         i_spatialindex=spatialiteUpdateLayerStatistics(database,"","", i_spatialindex,SpatialiteDatabaseType.SPATIALITE3);
         // GPLog.androidLog(-1,"-I-> DaoSpatialite: getSpatialVectorMap_V3["+database.getFilename()+"] spatialiteUpdateLayerStatistics["+i_spatialindex+"] ");
         if (i_spatialindex != 1)
         { // if this fails then we may have to consider this Database invalid
          return;
         }
        }
        String vector_key=""; // term used when building the sql, used as map.key
        String vector_data=""; // term used when building the sql
        String vector_extent=""; // term used when building the sql
        String vector_value=""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name="";
        String geometry_column="";
        String[] sa_string;
        // for pre-Spatialite : Views and Table must be done in 2 steps
        // First: Views
        Stmt statement = null;
        try
        {
         statement = database.prepare(VIEWS_QUERY_EXTENT_INVALID_V3);
         while( statement.step() )
         {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent = statement.column_string(2);
          spatialVectorMapErrors.put(vector_key,vector_data+vector_extent);
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V3["+SpatialiteDatabaseType.SPATIALITE3+"] sql["+VIEWS_QUERY_EXTENT_INVALID_V3+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        // Second: Tables
        try
        {
         statement = database.prepare(LAYERS_QUERY_EXTENT_INVALID_V3);
         while( statement.step() )
         {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent = statement.column_string(2);
          spatialVectorMapErrors.put(vector_key,vector_data+vector_extent);
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V3["+SpatialiteDatabaseType.SPATIALITE3+"] sql["+LAYERS_QUERY_EXTENT_INVALID_V3+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        // First Views
        try {
         statement = database.prepare(VIEWS_QUERY_EXTENT_VALID_V3);
         while( statement.step() ) {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent="";
          if (vector_key.indexOf("SpatialView") != -1)
          { // berlin_1000;map_linestring;SpatialView;ROWID
           //Do not call RecoverSpatialIndex for SpatialViews
           sa_string = vector_key.split(";");
           if (sa_string.length == 5) {
            table_name=sa_string[0];
            try { // replace placeholder with used primary-key and read_only parameter of SpatialView
              String ROWID_PK=getViewRowid(database,table_name,SpatialiteDatabaseType.SPATIALITE3);
              vector_key = vector_key.replace("ROWID;-1",ROWID_PK);
             } finally {
            }
           }
          }
          vector_extent = statement.column_string(2);
          if (vector_extent != null)
          {
           spatialVectorMap.put(vector_key,vector_data+vector_extent);
          }
          else
          { //should never happen
            // GPLog.androidLog(-1, "-E-> DaoSpatialite: getSpatialVectorMap_V3 vector_key[" + vector_key + "] vector_data["+ vector_data+"] vector_extent["+  vector_extent + "] VIEWS_QUERY_EXTENT_VALID_V3["+ VIEWS_QUERY_EXTENT_VALID_V3 + "]");
          }
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V3["+SpatialiteDatabaseType.SPATIALITE3+"] sql["+VIEWS_QUERY_EXTENT_VALID_V3+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        // Second Tables
        try {
         statement = database.prepare(LAYERS_QUERY_EXTENT_VALID_V3);
         while( statement.step() ) {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent="";
          vector_extent = statement.column_string(2);
          if (vector_extent != null)
          {
           spatialVectorMap.put(vector_key,vector_data+vector_extent);
          }
          else
          { //should never happen
            // GPLog.androidLog(-1, "-E-> DaoSpatialite: getSpatialVectorMap_V3 vector_key[" + vector_key + "] vector_data["+ vector_data+"] vector_extent["+  vector_extent + "] LAYERS_QUERY_EXTENT_VALID_V3["+ LAYERS_QUERY_EXTENT_VALID_V3 + "]");
          }
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V3["+SpatialiteDatabaseType.SPATIALITE3+"] sql["+LAYERS_QUERY_EXTENT_VALID_V3+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        if ((VECTOR_LAYERS_QUERY_MODE > 0) && (spatialVectorMapErrors.size() > 0))
        { // if empty: there are nothing to correct
         getSpatialVectorMap_Errors(database, spatialVectorMap ,spatialVectorMapErrors, SpatialiteDatabaseType.SPATIALITE3 );
        }
        // GPLog.androidLog(-1,"-I-> DaoSpatialite: getSpatialVectorMap_V3["+database.getFilename()+"] spatialVectorMap["+spatialVectorMap.size()+"] spatialVectorMapErrors["+spatialVectorMapErrors.size()+"] ");
    }

    /**
     * Read GeoPackage Revision 9 specific Databases
     *
     * @param database the database to check.
     * @param spatialVectorMap the {@link HashMap} of GeoPackage data (Features/Tiles) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid entries.
     * @return nothing
     * @throws Exception  if something goes wrong.
     */
    private static void getGeoPackageMap_R10( Database database, HashMap<String, String> spatialVectorMap , HashMap<String, String> spatialVectorMapErrors) throws Exception {
        String vector_key=""; // term used when building the sql, used as map.key
        String vector_data=""; // term used when building the sql
        String vector_extent=""; // term used when building the sql
        String s_vgpkg="vgpkg_";
        Stmt statement = null;
        try
        {
         statement = database.prepare(GEOPACKAGE_QUERY_EXTENT_INVALID_R10);
         while( statement.step() )
         {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent = statement.column_string(2);
          spatialVectorMapErrors.put(vector_key,vector_data+vector_extent);
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getGeoPackageMap_R10["+SpatialiteDatabaseType.GEOPACKAGE+"] sql["+GEOPACKAGE_QUERY_EXTENT_INVALID_R10+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        try {
         statement = database.prepare(GEOPACKAGE_QUERY_EXTENT_VALID_R10);
         while( statement.step() ) {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent="";
          vector_extent = statement.column_string(2);
          if (vector_extent != null)
          { // geonames;geometry;GeoPackage_features;Geonames;Data from http://www.geonames.org/, under Creative Commons Attribution 3.0 License;
           boolean b_valid=true;
           if (vector_key.indexOf("GeoPackage_features") != -1)
           {
            b_valid=false;
            String[] sa_string = vector_key.split(";");
            if (sa_string.length == 5)
            {
             String table_name=sa_string[0];
             String geometry_column=sa_string[1];
             String s_layer_type=sa_string[2];
             String s_ROWID_PK=sa_string[3];
             String s_view_read_only = sa_string[4];
             HashMap<String, String> fieldNamesToTypeMap=collectTableFields(database,s_vgpkg+table_name);
             if (fieldNamesToTypeMap.size() > 0)
              b_valid=true; // vgpkg_table-name exists
             else
             { // only when AutoGPKGStart must be called
              int i_count=spatialiteAutoGPKG(database,0,SpatialiteDatabaseType.GEOPACKAGE);
              if (i_count > 0)
              { // there must be at least 1 table
               fieldNamesToTypeMap=collectTableFields(database,s_vgpkg+table_name);
               if (fieldNamesToTypeMap.size() > 0)
               { // vgpkg_table-name exists ; AutoGPKGStart worked              
                b_valid=true;
               }
              }
             }
             if (b_valid)
             { // return spatialite VirtualGPKG table-name instead of geopackage table-name
              vector_key=s_vgpkg+table_name+";"+geometry_column+";"+s_layer_type+";"+s_view_read_only+";";
             }
            }
           }
           if (b_valid)
           {
            spatialVectorMap.put(vector_key,vector_data+vector_extent);
           }
          }
          else
          { //should never happen
           // GPLog.androidLog(-1, "DaoSpatialite:getGeoPackageMap_R10 -W-> vector_key[" + vector_key + "] vector_data["+ vector_data+"] vector_extent["+  vector_extent + "] GEOPACKAGE_QUERY_EXTENT_VALID_R10["+ GEOPACKAGE_QUERY_EXTENT_VALID_R10 + "]");
          }
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getGeoPackageMap_R10["+SpatialiteDatabaseType.GEOPACKAGE+"] sql["+GEOPACKAGE_QUERY_EXTENT_VALID_R10+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        // GPLog.androidLog(-1,"DaoSpatialite: getGeoPackageMap_R10["+database.getFilename()+"] spatialVectorMap["+spatialVectorMap.size()+"]  spatialVectorMapErrors["+spatialVectorMapErrors.size()+"] ");
    }

    /**
     * Read Spatial-Geometries for Spatialite 4.* specific Databases
     *
     * @param database the database to check.
     * @param spatialVectorMap the {@link HashMap} of Spatialite4+ Vector-data (Views/Tables Geometries) to clear and repopulate.
     * @param spatialVectorMapErrors the {@link HashMap} of of invalid geometries.
     * @param b_layers_statistics if a layers_statistics had been found
     * @param b_raster_coverages if a raster_coverages had been found [RasterLite2 support]
     * @return nothing
     * @throws Exception  if something goes wrong.
     */
    private static void getSpatialVectorMap_V4( Database database, HashMap<String, String> spatialVectorMap , HashMap<String, String> spatialVectorMapErrors, boolean b_layers_statistics , boolean b_raster_coverages ) throws Exception {
        String vector_key=""; // term used when building the sql, used as map.key
        String vector_data=""; // term used when building the sql
        String vector_extent=""; // term used when building the sql
        String vector_value=""; // to retrieve map.value (=vector_data+vector_extent)
        String table_name="";
        String geometry_column="";
        String[] sa_string;
        Stmt statement = null;
        try
        {
         statement = database.prepare(VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4);
         while( statement.step() )
         {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent = statement.column_string(2);
          spatialVectorMapErrors.put(vector_key,vector_data+vector_extent);
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V4["+SpatialiteDatabaseType.SPATIALITE4+"] sql["+VECTOR_LAYERS_QUERY_EXTENT_INVALID_V4+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        try {
         statement = database.prepare(VECTOR_LAYERS_QUERY_EXTENT_VALID_V4);
         while( statement.step() ) {
          vector_key = statement.column_string(0);
          vector_data = statement.column_string(1);
          vector_extent="";
          if (vector_key.indexOf("SpatialView") != -1)
          { // berlin_1000;map_linestring;SpatialView;ROWID
           //Do not call RecoverSpatialIndex for SpatialViews
           sa_string = vector_key.split(";");
           if (sa_string.length == 5) {
            table_name=sa_string[0];
            try { // replace placeholder with used primary-key and read_only parameter of SpatialView
              String ROWID_PK=getViewRowid(database,table_name,SpatialiteDatabaseType.SPATIALITE4);
              vector_key = vector_key.replace("ROWID;-1",ROWID_PK);
             } finally {
            }
           }
          }
          vector_extent = statement.column_string(2);
          if (vector_extent != null)
          {
           spatialVectorMap.put(vector_key,vector_data+vector_extent);
          }
          else
          { //should never happen
            // GPLog.androidLog(-1, "DaoSpatialite: getSpatialVectorMap_V4 vector_key[" + vector_key + "] vector_data["+ vector_data+"] vector_extent["+  vector_extent + "] VECTOR_LAYERS_QUERY["+ VECTOR_LAYERS_QUERY_EXTENT_VALID_V4 + "]");
          }
         }
        }
        catch (jsqlite.Exception e_stmt) {
         GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V4["+SpatialiteDatabaseType.SPATIALITE4+"] sql["+VECTOR_LAYERS_QUERY_EXTENT_VALID_V4+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
         if (statement != null) {
          statement.close();
         }
        }
        if ((VECTOR_LAYERS_QUERY_MODE > 0) && (spatialVectorMapErrors.size() > 0))
        { // if empty: there are nothing to correct [do before RasterLite2 logic - there is no error control for that]
         getSpatialVectorMap_Errors(database, spatialVectorMap ,spatialVectorMapErrors, SpatialiteDatabaseType.SPATIALITE4 );
        }
        if ((!Rasterlite2Version_CPU.equals("")) && (b_raster_coverages))
        { // RasterLite2 support: a raster_coverages has been found and the driver supports it
         try
         {
          statement = database.prepare(RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42);
          while( statement.step() )
          {
           vector_key = statement.column_string(0);
           vector_data = statement.column_string(1);
           vector_extent = statement.column_string(2);
           spatialVectorMapErrors.put(vector_key,vector_data+vector_extent);
          }
         }
         catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V4["+SpatialiteDatabaseType.SPATIALITE4+"] sql["+RASTER_COVERAGES_QUERY_EXTENT_INVALID_V42+"] db[" + database.getFilename() + "]", e_stmt);
         }
         finally {
          if (statement != null) {
           statement.close();
          }
         }         
         try {
          statement = database.prepare(RASTER_COVERAGES_QUERY_EXTENT_VALID_V42);
          while( statement.step() ) {
           vector_key = statement.column_string(0);
           vector_data = statement.column_string(1);
           vector_extent="";
           vector_extent = statement.column_string(2);
           if (vector_extent != null)
           { // mj10777: for some reason, this is being filled twice
            spatialVectorMap.put(vector_key,vector_data+vector_extent);
           }
           else
           { //should never happen
            // GPLog.androidLog(-1, "DaoSpatialite: getSpatialVectorMap_V4 vector_key[" + vector_key + "] vector_data["+ vector_data+"] vector_extent["+  vector_extent + "] RASTER_COVERAGES_QUERY["+ RASTER_COVERAGES_QUERY_EXTENT_VALID_V42 + "]");
           }
          }
         }
         catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:getSpatialVectorMap_V4["+SpatialiteDatabaseType.SPATIALITE4+"] sql["+RASTER_COVERAGES_QUERY_EXTENT_VALID_V42+"] db[" + database.getFilename() + "]", e_stmt);
         }
         finally {
          if (statement != null) {
           statement.close();
          }
         }
        }
        // GPLog.androidLog(-1,"DaoSpatialite: getSpatialVectorMap_V4["+database.getFilename()+"] spatialVectorMap["+spatialVectorMap.size()+"]  spatialVectorMapErrors["+spatialVectorMapErrors.size()+"] ");
    }

    /**
     * Checks the database type and its validity.
     * - Spatialite 2.4 to present version are supported (2.4 will be set as 3)
     * @param database the database to check.
     * @param databaseViewsMap the {@link HashMap} of database views data to clear and repopulate.
     * @return the {@link SpatialiteDatabaseType}.
     */
    public static SpatialiteDatabaseType checkDatabaseTypeAndValidity( Database database, HashMap<String, String> spatialVectorMap , HashMap<String, String> spatialVectorMapErrors )
            throws Exception {
        // clear views
        spatialVectorMap.clear();
        spatialVectorMapErrors.clear();
        if (JavaSqliteDescription.equals(""))
        { // Rasterlite2Version_CPU will NOT be empty, if the Driver was compiled with RasterLite2 support
         JavaSqliteDescription=getJavaSqliteDescription(database,"DaoSpatialite.checkDatabaseTypeAndValidity");
         GPLog.androidLog(-1,"DaoSpatialite.JavaSqliteDescription["+JavaSqliteDescription+"] recovery_mode["+VECTOR_LAYERS_QUERY_MODE+"]");
        }
        // views: vector_layers_statistics,vector_layers
        // pre-spatialite 3.0 Databases often do not have a Virtual-SpatialIndex table
        boolean b_SpatialIndex = false;
        boolean b_vector_layers_statistics = false;
        boolean b_vector_layers = false;

        // tables: geometry_columns,raster_columns
        boolean b_geometry_columns = false;
        // spatialite 2.0, 2.1 and 2.3 do NOT have a views_geometry_columns table
        boolean b_views_geometry_columns = false;
        // spatialite 4.2.0 - RasterLite2 table [raster_coverages]
        boolean b_raster_coverages = false;
        // this table dissapered (maybe 4.1.0) - when vector_layers_statistics is not set, this may be used for the bounds
        boolean b_layers_statistics = false;
        // boolean b_raster_columns = false;
        boolean b_gpkg_contents = false;
        String sqlCommand = "SELECT name,type,sql FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
        String tableType = "";
        String sqlCreationString = "";
        String name = "";
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            while( statement.step() ) {
                name = statement.column_string(0);
                tableType = statement.column_string(1);
                sqlCreationString = statement.column_string(2);
                // GPLog.androidLog(-1,"DaoSpatialite.checkDatabaseTypeAndValidity["+s_table+"] tablename["+s_name+"] type["+s_type+"] sql["
                // + s_sql_create+ "] ");
                if (tableType.equals("table")) {
                    if ((name.equals("geometry_columns")) || (name.equals("sqlite_stat1"))) {
                        b_geometry_columns = true;
                    } else if (name.equals("SpatialIndex")) {
                       b_SpatialIndex = true;
                    } else if (name.equals("views_geometry_columns")) {
                       b_views_geometry_columns = true;
                    } else if (name.equals("raster_coverages")) {
                       b_raster_coverages = true;
                    } else if (name.equals("layers_statistics")) {
                        b_layers_statistics = true;
                    } else if (name.equals(METADATA_GEOPACKAGE_TABLE_NAME)) {
                        b_gpkg_contents = true;
                    }
                    // if (name.equals("raster_columns")) {
                    // b_raster_columns = true;
                    // }
                } else if (tableType.equals("view")) {
                    // we are looking for user-defined views only,
                    // filter out system known views.
                    if ((!name.equals("geom_cols_ref_sys")) && (!name.startsWith("vector_layers"))) {
                        // databaseViewsMap.put(name, sqlCreationString);
                    } else if (name.equals("vector_layers_statistics")) {
                        b_vector_layers_statistics = true;
                    } else if (name.equals("vector_layers")) {
                        b_vector_layers = true;
                    }
                }
             }
        } catch (Exception e) {
          GPLog.error("DAOSPATIALITE", "Error in checkDatabaseTypeAndValidity sql["+sqlCommand+"] db[" + database.getFilename() + "]", e);
        }
        finally {
                if (statement != null) {
                    statement.close();
                }
        }
        if (b_gpkg_contents) {
            // this is a GeoPackage, this can also have
            // vector_layers_statistics and vector_layers
            // - the results are empty, it does reference the table
            // also referenced in gpkg_contents
               getGeoPackageMap_R10(database,spatialVectorMap,spatialVectorMapErrors);
               if (spatialVectorMap.size() > 0)
                return SpatialiteDatabaseType.UNKNOWN;
                // return SpatialiteDatabaseType.GEOPACKAGE;
               else
                // if empty, nothing to load
                return SpatialiteDatabaseType.UNKNOWN;
        } else {
            if ((b_vector_layers_statistics) && (b_vector_layers)) { // Spatialite 4.0
                getSpatialVectorMap_V4(database,spatialVectorMap,spatialVectorMapErrors,b_layers_statistics,b_raster_coverages);
               if (spatialVectorMap.size() > 0)
                return SpatialiteDatabaseType.SPATIALITE4;
               else
                // if empty, nothing to load
                return SpatialiteDatabaseType.UNKNOWN;
            } else {
                if ((b_geometry_columns) && (b_views_geometry_columns)) { // Spatialite from 2.4 until 4.0
                     getSpatialVectorMap_V3(database,spatialVectorMap,spatialVectorMapErrors,b_layers_statistics,b_SpatialIndex);
                    if (spatialVectorMap.size() > 0)
                     return SpatialiteDatabaseType.SPATIALITE3;
                    else
                     // if empty, nothing to load
                     return SpatialiteDatabaseType.UNKNOWN;
            } 
           }
        }
        return SpatialiteDatabaseType.UNKNOWN;
    }

    /**
     * Determine the Spatialite version of the Database being used.
     *
     * <ul>
     * <li> - if (sqlite3_exec(this_handle_sqlite3,"SELECT InitSpatialMetadata()",NULL,NULL,NULL) == SQLITE_OK)
     * <li>  - 'geometry_columns'
     * <li>-- SpatiaLite 2.0 'sqlite_stat1' until 2.2
     * <li>- 'spatial_ref_sys'
     * <li>-- SpatiaLite 2.1 until present version
     * <li>-- SpatiaLite 2.3.1 has no field 'srs_wkt' or 'srtext' field,only 'proj4text' and
     * <li>-- SpatiaLite 2.4.0 first version with 'srs_wkt' and 'views_geometry_columns'
     * <li>-- SpatiaLite 3.1.0-RC2 last version with 'srs_wkt'
     * <li>-- SpatiaLite 4.0.0-RC1 : based on ISO SQL/MM standard 'srtext'
     * <li>-- views: vector_layers_statistics,vector_layers
     * <li>-- SpatiaLite 4.0.0 : introduced
     * </ul>
     *
     * <p>20131129: at the moment not possible to distinguish between 2.4.0 and 3.0.0 [no '2']
     *
     * @param database Database connection to use
     * @param table name of table to read [if empty: list of tables in Database]
     * @return i_spatialite_version [0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0 ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1]
     * @throws Exception if something goes wrong.
     */
    public static int getSpatialiteDatabaseVersion( Database database, String table ) throws Exception {
        Stmt this_stmt = null;
        // views: vector_layers_statistics,vector_layers
        // boolean b_vector_layers_statistics = false;
        // boolean b_vector_layers = false;
        // tables: geometry_columns,raster_columns

        /*
         * false = not a spatialite Database
         * true = a spatialite Database
         */
        boolean b_geometry_columns = false;

        /*
         * 0=not found = pre 2.4.0 ;
         * 1=2.4.0 to 3.1.0 ;
         * 2=starting with 4.0.0
         */
        int i_srs_wkt = 0;
        boolean b_spatial_ref_sys = false;
        // boolean b_views_geometry_columns = false;
        int i_spatialite_version = 0;
        // 0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0
        // ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1
        String s_sql_command = "";
        if (!table.equals("")) { // pragma table_info(geodb_geometry)
            s_sql_command = "pragma table_info(" + table + ")";
        } else {
            s_sql_command = "SELECT name,type FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
        }
        String s_type = "";
        String s_name = "";
        this_stmt = database.prepare(s_sql_command);
        try {
            while( this_stmt.step() ) {
                if (!table.equals("")) { // pragma table_info(berlin_strassen_geometry)
                    s_name = this_stmt.column_string(1);
                    // 'proj4text' must always exist - otherwise invalid
                    if (s_name.equals("proj4text"))
                        b_spatial_ref_sys = true;
                    if (s_name.equals("srs_wkt"))
                        i_srs_wkt = 1;
                    if (s_name.equals("srtext"))
                        i_srs_wkt = 2;
                }
                if (table.equals("")) {
                    s_name = this_stmt.column_string(0);
                    s_type = this_stmt.column_string(1);
                    if (s_type.equals("table")) {
                        // if (s_name.equals("geometry_columns")) {
                        // b_geometry_columns = true;
                        // }
                        if (s_name.equals("spatial_ref_sys")) {
                            b_spatial_ref_sys = true;
                        }
                    }
                    // if (s_type.equals("view")) {
                    // // SELECT name,type,sql FROM sqlite_master WHERE
                    // // (type='view')
                    // if (s_name.equals("vector_layers_statistics")) {
                    // // An empty spatialite
                    // // Database will not have
                    // // this
                    // b_vector_layers_statistics = true;
                    // }
                    // if (s_name.equals("vector_layers")) {
                    // // An empty spatialite Database will
                    // // not have this
                    // b_vector_layers = true;
                    // }
                    // }
                }
            }
        } finally {
            if (this_stmt != null) {
                this_stmt.close();
            }
        }
        if (table.equals("")) {
            GPLog.androidLog(-1, "DaoSpatialite: get_table_fields sql[" + s_sql_command + "] geometry_columns["
                    + b_geometry_columns + "] spatial_ref_sys[" + b_spatial_ref_sys + "]");
            if ((b_geometry_columns) && (b_spatial_ref_sys)) {
                if (b_spatial_ref_sys) {
                    i_srs_wkt = getSpatialiteDatabaseVersion(database, "spatial_ref_sys");
                    if (i_srs_wkt == 4) { // Spatialite 4.0
                        i_spatialite_version = 4;
                    } else {
                        i_spatialite_version = i_srs_wkt;
                    }
                }
            }
        } else {
            if (b_spatial_ref_sys) { // 'proj4text' must always exist - otherwise invalid
                switch( i_srs_wkt ) {
                case 0:
                    i_spatialite_version = 2; // no 'srs_wkt' or 'srtext' fields
                    break;
                case 1:
                    i_spatialite_version = 3; // 'srs_wkt'
                    break;
                case 2:
                    i_spatialite_version = 4; // 'srtext'
                    break;
                }
            }
        }
        return i_spatialite_version;
    }

    /**
     * Collects the fields of a table, also checking the database type.
     *
     * <br>- name of Field
     * <br>- type of field as defined in Database
     *
     * @param database the database to use.
     * @param tableName name of table to read.
     * @return the {@link HashMap} of fields: [name of field, type of field]
     * @throws Exception if something goes wrong.
     */
    public static HashMap<String, String> collectTableFields( Database database, String tableName ) throws Exception {

        HashMap<String, String> fieldNamesToTypeMap = new LinkedHashMap<String, String>();
        String s_sql_command = "pragma table_info('" + tableName + "')";
        String tableType = "";
        String sqlCreationString = "";
        Stmt statement = null;
        String name = "";
        try {
            statement = database.prepare(s_sql_command);
            while( statement.step() ) {
                name = statement.column_string(1);
                tableType = statement.column_string(2);
                sqlCreationString = statement.column_string(5); // pk
                // try to unify the data-types: varchar(??),int(11) mysql-syntax
                if (tableType.indexOf("int(") != -1)
                    tableType = "INTEGER";
                if (tableType.indexOf("varchar(") != -1)
                    tableType = "TEXT";
                // pk: 0 || 1;Data-TypeTEXT || DOUBLE || INTEGER || REAL || DATE || BLOB ||
                // geometry-types
                fieldNamesToTypeMap.put(name, sqlCreationString + ";" + tableType.toUpperCase(Locale.US));
            }
        }
        catch (jsqlite.Exception e_stmt) {
          GPLog.androidLog(4, "DaoSpatialite:collectTableFields["+tableName+"] sql["+s_sql_command+"] db[" + database.getFilename() + "]", e_stmt);
        }
        finally {
            if (statement != null) {
                statement.close();
            }
        }
        return fieldNamesToTypeMap;
    }
}
