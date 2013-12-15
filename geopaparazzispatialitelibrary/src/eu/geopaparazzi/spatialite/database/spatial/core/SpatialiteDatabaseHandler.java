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
package eu.geopaparazzi.spatialite.database.spatial.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ColorUtilities;

/**
 * An utility class to handle the spatial database.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDatabaseHandler implements ISpatialDatabaseHandler {

    // 3857
    // private GeometryFactory gf = new GeometryFactory();
    // private WKBWriter wr = new WKBWriter();
    // private WKBReader wkbReader = new WKBReader(gf);

    private static final String METADATA_TABLE_GEOPACKAGE_CONTENTS = "geopackage_contents";
    private static final String METADATA_TABLE_TILE_MATRIX = "tile_matrix_metadata";
    private static final String METADATA_TABLE_RASTER_COLUMNS = "raster_columns";
    private static final String METADATA_TABLE_GEOMETRY_COLUMNS = "geometry_columns";

    private static final String METADATA_GEOPACKAGECONTENT_TABLE_NAME = "table_name";
    private static final String METADATA_GEOPACKAGECONTENT_DATA_TYPE = "data_type";
    // private static final String METADATA_GEOPACKAGECONTENT_DATA_TYPE_TILES = "tiles";
    private static final String METADATA_GEOPACKAGECONTENT_DATA_TYPE_FEATURES = "features";
    private static final String METADATA_TILE_TABLE_NAME = "t_table_name";
    private static final String METADATA_ZOOM_LEVEL = "zoom_level";
    private static final String METADATA_RASTER_COLUMN = "r_raster_column";
    private static final String METADATA_RASTER_TABLE_NAME = "r_table_name";
    private static final String METADATA_SRID = "srid";
    private static final String METADATA_GEOMETRY_TYPE4 = "geometry_type";
    private static final String METADATA_GEOMETRY_TYPE3 = "type";
    private static final String METADATA_GEOMETRY_COLUMN = "f_geometry_column";
    private static final String METADATA_TABLE_NAME = "f_table_name";
    // https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
    private static final String METADATA_VECTOR_LAYERS_TABLE_NAME = " vector_layers";
    private static final String METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME = " vector_layers_statistics";
    // vector_layers
    // SELECT
    // layer_type,table_name,geometry_column,geometry_type,coord_dimension,srid,spatial_index_enabled
    // FROM vector_layers
    // SELECT * FROM vector_layers_statistics
    // SELECT
    // vector_layers_statistics.layer_type,vector_layers_statistics.table_name,vector_layers_statistics.geometry_column,vector_layers_statistics.row_count,vector_layers_statistics.extent_min_x,vector_layers_statistics.extent_min_y,vector_layers_statistics.extent_max_x,vector_layers_statistics.extent_max_y,vector_layers.geometry_type,vector_layers.coord_dimension,vector_layers.srid,vector_layers.spatial_index_enabled,vector_layers_statistics.last_verified
    // FROM vector_layers_statistics,vector_layers WHERE ((vector_layers_statistics.table_name =
    // vector_layers.table_name) AND (vector_layers_statistics.geometry_column =
    // vector_layers.geometry_column))
    // v4: SELECT f_table_name,f_geometry_column,geometry_type,srid FROM geometry_columns

    private static final String NAME = "name";
    private static final String SIZE = "size";
    private static final String FILLCOLOR = "fillcolor";
    private static final String STROKECOLOR = "strokecolor";
    private static final String FILLALPHA = "fillalpha";
    private static final String STROKEALPHA = "strokealpha";
    private static final String SHAPE = "shape";
    private static final String WIDTH = "width";
    private static final String TEXTSIZE = "textsize";
    private static final String TEXTFIELD = "textfield";
    private static final String ENABLED = "enabled";
    private static final String ORDER = "layerorder";
    private static final String DECIMATION = "decimationfactor";
    private static final String DASH = "dashpattern";
    private static final String MINZOOM = "minzoom";
    private static final String MAXZOOM = "maxzoom";

    private final String PROPERTIESTABLE = "dataproperties";

    private Database db_java;

    private HashMap<String, Paint> fillPaints = new HashMap<String, Paint>();
    private HashMap<String, Paint> strokePaints = new HashMap<String, Paint>();

    private List<SpatialVectorTable> vectorTableList;
    private List<SpatialRasterTable> rasterTableList;
    private File file_map; // all DatabaseHandler/Table classes should use these names
    private String s_map_file; // [with path] all DatabaseHandler/Table classes should use these
                               // names
    private String s_name_file; // [without path] all DatabaseHandler/Table classes should use these
                                // names
    private String s_name; // all DatabaseHandler/Table classes should use these names
    private String s_description; // all DatabaseHandler/Table classes should use these names
    private String s_map_type; // all DatabaseHandler/Table classes should use these names
    // will be set in class_bounds - values of all Rasters-Datasets
    private int minZoom = 0;
    private int maxZoom = 0;
    // will be set in class_bounds - values of all V-Datactorsets
    private double centerX = 0.0; // wsg84
    private double centerY = 0.0; // wsg84
    private double bounds_west = 0.0; // wsg84
    private double bounds_east = 0.0; // wsg84
    private double bounds_north = 0.0; // wsg84
    private double bounds_south = 0.0; // wsg84
    // -----------------------------------------------------
    private int defaultZoom;
    private boolean b_database_valid = true;
    private int i_database_type = 0;
    // List of all View of Database [name,sql_create] - search sql for geometry columns
    private HashMap<String, String> view_list;
    public SpatialiteDatabaseHandler( String dbPath ) {
        try {
            file_map = new File(dbPath);
            if (!file_map.getParentFile().exists()) {
                throw new RuntimeException();
            }
            s_map_file = file_map.getAbsolutePath();
            s_name_file = file_map.getName();
            s_name = file_map.getName().substring(0, file_map.getName().lastIndexOf("."));
            db_java = new jsqlite.Database();
            db_java.open(s_map_file, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
            get_tables(0); // 0=check if valid only ; 1=check if valid and fill
                           // vectorTableList,rasterTableList
            if (!isValid()) {
                close();
            }
        } catch (Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + file_map.getAbsolutePath() + "]", e);
        }
        if (isValid()) {
            setDescription(s_name);
            // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+s_name+"]["+getJavaSqliteDescription()+"]");
        }
        // GPLog.androidLog(-1,"SpatialiteDatabaseHandler[" + file_map.getAbsolutePath() +
        // "] name["+s_name+"] s_description["+s_description+"]");
    }
    // -----------------------------------------------
    /**
      * Is the database file considered valid
      * - metadata table exists and has data
      * - 'tiles' is either a table or a view and the correct fields exist
      * -- if a view: do the tables map and images exist with the correct fields
      * checking is done once when the 'metadata' is retrieved the first time [fetchMetadata()]
      * @return b_mbtiles_valid true if valid, otherwise false
      */
    @Override
    public boolean isValid() {
        // b_database_valid=true;
        return b_database_valid;
    }
    // -----------------------------------------------
    /**
      * Return long name of map/file
      *
      * <p>default: file name with path and extention
      * <p>mbtiles : will be a '.mbtiles' sqlite-file-name
      * <p>map : will be a mapforge '.map' file-name
      *
      * @return file_map.getAbsolutePath();
      */
    @Override
    public String getFileNamePath() {
        return this.s_map_file; // file_map.getAbsolutePath();
    }
    // -----------------------------------------------
    /**
      * Return short name of map/file
      *
      * <p>default: file name without path but with extention
      *
      * @return file_map.getAbsolutePath();
      */
    public String getFileName() {
        return this.s_name_file; // file_map.getName();
    }
    // -----------------------------------------------
    /**
      * Return short name of map/file
      *
      * <p>default: file name without path and extention
      * <p>mbtiles : metadata 'name'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_name as short name of map/file
      */
    public String getName() {
        if ((s_name == null) || (s_name.length() == 0)) {
            s_name = this.file_map.getName().substring(0, this.file_map.getName().lastIndexOf("."));
        }
        return this.s_name; // comment or file-name without path and extention
    }
    // -----------------------------------------------
    /**
      * Return String of bounds [wms-format]
      *
      * <p>x_min,y_min,x_max,y_max
      *
      * @return bounds formatted using wms format
      */
    public String getBounds_toString() {
        return bounds_west + "," + bounds_south + "," + bounds_east + "," + bounds_north;
    }
    // -----------------------------------------------
    /**
      * Return String of Map-Center with default Zoom
      *
      * <p>x_position,y_position,default_zoom
      *
      * @return center formatted using mbtiles format
      */
    public String getCenter_toString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }
    // -----------------------------------------------
    /**
      * Return Min/Max Zoom as string
      *
      * <p>default :  1-22
      * <p>mbtiles : taken from value of metadata 'min/maxzoom'
      *
      * @return String min/maxzoom
      */
    public String getZoom_Levels() {
        return getMinZoom() + "-" + getMaxZoom();
    }
    // -----------------------------------------------
    /**
      * Return long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_description long description of map/file
      */
    public String getDescription() {
        if ((this.s_description == null) || (this.s_description.length() == 0) || (this.s_description.equals(this.s_name)))
            setDescription(getName()); // will set default values with bounds and center if it is
                                       // the same as 's_name' or empty
        return this.s_description; // long comment
    }
    // -----------------------------------------------
    /**
      * Set long description of map/file
      *
      * <p>default: s_name with bounds and center
      * <p>mbtiles : metadata description'
      * <p>map : will be value of 'comment', if not null
      *
      * @return s_description long description of map/file
      */
    public void setDescription( String s_description ) {
        if ((s_description == null) || (s_description.length() == 0) || (s_description.equals(this.s_name))) {
            this.s_description = getName() + " bounds[" + getBounds_toString() + "] center[" + getCenter_toString() + "]";
        } else
            this.s_description = s_description;
    }
    // -----------------------------------------------
    /**
      * Return map-file as 'File'
      *
      * <p>if the class does not fail, this file exists
      * <p>mbtiles : will be a '.mbtiles' sqlite-file
      * <p>map : will be a mapforge '.map' file
      *
      * @return file_map as File
      */
    public File getFile() {
        return this.file_map;
    }
    // -----------------------------------------------
    /**
      * Return Min Zoom
      *
      * <p>default :  0
      * <p>mbtiles : taken from value of metadata 'minzoom'
      * <p>map : value is given in 'StartZoomLevel'
      *
      * @return integer minzoom
      */
    public int getMinZoom() {
        return minZoom;
    }
    // -----------------------------------------------
    /**
      * Return Max Zoom
      *
      * <p>default :  22
      * <p>mbtiles : taken from value of metadata 'maxzoom'
      * <p>map : value not defined, seems to calculate bitmap from vector data [18]
      *
      * @return integer maxzoom
      */
    public int getMaxZoom() {
        return maxZoom;
    }
    // -----------------------------------------------
    /**
      * Return West X Value [Longitude]
      *
      * <p>default :  -180.0 [if not otherwise set]
      * <p>mbtiles : taken from 1st value of metadata 'bounds'
      *
      * @return double of West X Value [Longitude]
      */
    public double getMinLongitude() {
        return bounds_west;
    }
    // -----------------------------------------------
    /**
      * Return South Y Value [Latitude]
      *
      * <p>default :  -85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 2nd value of metadata 'bounds'
      *
      * @return double of South Y Value [Latitude]
      */
    public double getMinLatitude() {
        return bounds_south;
    }
    // -----------------------------------------------
    /**
      * Return East X Value [Longitude]
      *
      * <p>default :  180.0 [if not otherwise set]
      * <p>mbtiles : taken from 3th value of metadata 'bounds'
      *
      * @return double of East X Value [Longitude]
      */
    public double getMaxLongitude() {
        return bounds_east;
    }
    // -----------------------------------------------
    /**
      * Return North Y Value [Latitude]
      *
      * <p>default :  85.05113 [if not otherwise set]
      * <p>mbtiles : taken from 4th value of metadata 'bounds'
      *
      * @return double of North Y Value [Latitude]
      */
    public double getMaxLatitude() {
        return bounds_north;
    }
    // -----------------------------------------------
    /**
      * Return Center X Value [Longitude]
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 1st value of metadata 'center'
      *
      * @return double of X Value [Longitude]
      */
    public double getCenterX() {
        return centerX;
    }
    // -----------------------------------------------
    /**
      * Return Center Y Value [Latitude]
      *
      * <p>default : center of bounds
      * <p>mbtiles : taken from 2nd value of metadata 'center'
      *
      * @return double of Y Value [Latitude]
      */
    public double getCenterY() {
        return centerY;
    }
    // -----------------------------------------------
    /**
      * Retrieve Zoom level
      *
      * <p>default : minZoom
      * <p>mbtiles : taken from 3rd value of metadata 'center'
      *
     * @return defaultZoom
      */
    public int getDefaultZoom() {
        return defaultZoom;
    }
    // -----------------------------------------------
    /**
      * Set default Zoom level
      *
      * <p>default : minZoom
      * <p>mbtiles : taken from 3rd value of metadata 'center'
      *
      * @param i_zoom desired Zoom level
      */
    public void setDefaultZoom( int i_zoom ) {
        defaultZoom = i_zoom;
    }
    // -----------------------------------------------
    /**
      * Return versions supported in JavaSqlite
      * - JavaSqlite
      * - Spatialite
      * - Proj4
      * - Geos
      * -- there is no Spatialite function to retrieve the Sqlite version
      * -- the Has() functions to not eork with spatialite 3.0.1
      * @return s_description long description of map/file
      */
    public String getJavaSqliteDescription() {
        String s_javasqlite_description = "";
        try { // javasqlite[20120209],spatialite[4.1.1], proj4[Rel. 4.8.0, 6 March
              // 2012],geos[3.4.2-CAPI-1.8.2 r3921],
              // spatialite_properties[HasIconv[1],HasMathSql[1],HasGeoCallbacks[0],HasProj[1],
              // HasGeos[1],HasGeosAdvanced[1],HasGeosTrunk[0],HasLwGeom[0],
              // HasLibXML2[0],HasEpsg[1],HasFreeXL[0]]]
              // javasqlite[20120209],spatialite[3.0.1],proj4[Rel. 4.7.1, 23 September
              // 2009],geos[3.2.2-CAPI-1.6.2],exception[? not a spatialite database, or spatialite <
              // 4 ?]]
            s_javasqlite_description = "javasqlite[" + getJavaSqliteVersion() + "],";
            s_javasqlite_description += "spatialite[" + getSpatialiteVersion() + "],";
            s_javasqlite_description += "proj4[" + getProj4Version() + "],";
            s_javasqlite_description += "geos[" + getGeosVersion() + "],";
            s_javasqlite_description += "spatialite_properties[" + getSpatialiteProperties() + "]]";

        } catch (Exception e) {
            s_javasqlite_description += "exception[? not a spatialite database, or spatialite < 4 ?]]";
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + s_name + "].getJavaSqliteDescription[" + s_javasqlite_description
                    + "]", e);
        }
        return s_javasqlite_description;
    }
    // -----------------------------------------------
    /**
     * Get the version of JavaSqlite.
     * known values: 20120209,20131124 as int
     * @return the version of JavaSqlite in 'Constants.drv_minor'.
     */
    public String getJavaSqliteVersion() {
        return "" + Constants.drv_minor;
    }
    // -----------------------------------------------
    /**
     * Get the version of Spatialite.
     *
     * @return the version of Spatialite.
     * @throws Exception
     */
    public String getSpatialiteVersion() throws Exception {
        Stmt stmt = db_java.prepare("SELECT spatialite_version();");
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
    // -----------------------------------------------
    /**
     * Get the properties of Spatialite.
     * - use the known 'SELECT Has..' functions
     * - when HasIconv=0: no VirtualShapes,VirtualXL
     * @return the properties of Spatialite.
     * @throws Exception
     */
    public String getSpatialiteProperties() throws Exception {
        String s_value = "-";
        Stmt stmt = db_java
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
        return s_value;
    }
    // -----------------------------------------------
    /**
     * Get the version of proj.
     *
     * @return the version of proj.
     * @throws Exception
     */
    public String getProj4Version() throws Exception {
        Stmt stmt = db_java.prepare("SELECT proj4_version();");
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
    // -----------------------------------------------
    /**
     * Get the version of geos.
     *
     * @return the version of geos.
     * @throws Exception
     */
    public String getGeosVersion() throws Exception {
        Stmt stmt = db_java.prepare("SELECT geos_version();");
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

    @Override
    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        if (vectorTableList == null || forceRead) {
            vectorTableList = new ArrayList<SpatialVectorTable>();
            get_tables(1); // 0=check if valid only ; 1=check if valid and fill
                           // vectorTableList,rasterTableList
        }
        return vectorTableList;
    }

    /**
     * Extract the center coordinate of a raster tileset.
     *
     * @param tableName the raster table name.
     * @param centerCoordinate the coordinate array to update with the extracted values.
     */
    private void getSpatialVector_4326( String srid, double[] centerCoordinate, double[] boundsCoordinates, int i_parm ) {
        String centerQuery = "";
        try {
            Stmt centerStmt = null;
            double bounds_west = boundsCoordinates[0];
            double bounds_south = boundsCoordinates[1];
            double bounds_east = boundsCoordinates[2];
            double bounds_north = boundsCoordinates[3];
            // srid=3068
            // 3460.411441 1208.430179 49230.152810 38747.958906
            // SELECT
            // CastToXY(ST_Transform(MakePoint((3460.411441+(49230.152810-3460.411441)/2),(1208.430179+(38747.958906-1208.430179)/2),3068),4326))
            // AS Center
            // SELECT CastToXY(ST_Transform(MakePoint(3460.411441,1208.430179,3068),4326)) AS
            // South_West
            // SELECT CastToXY(ST_Transform(MakePoint(49230.152810,38747.958906,3068),4326)) AS
            // North_East
            try {
                WKBReader wkbReader = new WKBReader();
                StringBuilder centerBuilder = new StringBuilder();
                centerBuilder.append("SELECT ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                // centerBuilder.append("select AsText(ST_Transform(MakePoint(");
                centerBuilder.append("(" + bounds_west + " + (" + bounds_east + " - " + bounds_west + ")/2), ");
                centerBuilder.append("(" + bounds_south + " + (" + bounds_north + " - " + bounds_south + ")/2), ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS Center,");
                centerBuilder.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                centerBuilder.append("" + bounds_west + "," + bounds_south + ", ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS South_West,");
                centerBuilder.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                centerBuilder.append("" + bounds_east + "," + bounds_north + ", ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS North_East ");
                if (i_parm == 0) {
                } else {
                }
                // centerBuilder.append("';");
                centerQuery = centerBuilder.toString();

                centerStmt = db_java.prepare(centerQuery);
                if (centerStmt.step()) {
                    byte[] geomBytes = centerStmt.column_bytes(0);
                    Geometry geometry = wkbReader.read(geomBytes);
                    Coordinate coordinate = geometry.getCoordinate();
                    centerCoordinate[0] = coordinate.x;
                    centerCoordinate[1] = coordinate.y;
                    geomBytes = centerStmt.column_bytes(1);
                    geometry = wkbReader.read(geomBytes);
                    coordinate = geometry.getCoordinate();
                    boundsCoordinates[0] = coordinate.x;
                    boundsCoordinates[1] = coordinate.y;
                    geomBytes = centerStmt.column_bytes(2);
                    geometry = wkbReader.read(geomBytes);
                    coordinate = geometry.getCoordinate();
                    boundsCoordinates[2] = coordinate.x;
                    boundsCoordinates[3] = coordinate.y;
                }
            } finally {
                if (centerStmt != null)
                    centerStmt.close();
            }
        } catch (java.lang.Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + file_map.getAbsolutePath() + "] sql[" + centerQuery + "]", e);
        }
    }
    @Override
    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();
            get_tables(1); // 0=check if valid only ; 1=check if valid and fill
                           // vectorTableList,rasterTableList
        }
        return rasterTableList;
    }

    /**
     * Check availability of style for the tables.
     *
     * @throws Exception
     */
    private void checkPropertiesTable() throws Exception {
        String checkTableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + PROPERTIESTABLE + "';";
        Stmt stmt = db_java.prepare(checkTableQuery);
        boolean tableExists = false;
        try {
            if (stmt.step()) {
                String name = stmt.column_string(0);
                if (name != null) {
                    tableExists = true;
                }
            }
        } finally {
            stmt.close();
        }
        if (!tableExists) {
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ");
            sb.append(PROPERTIESTABLE);
            sb.append(" (");
            sb.append(NAME).append(" TEXT, ");
            sb.append(SIZE).append(" REAL, ");
            sb.append(FILLCOLOR).append(" TEXT, ");
            sb.append(STROKECOLOR).append(" TEXT, ");
            sb.append(FILLALPHA).append(" REAL, ");
            sb.append(STROKEALPHA).append(" REAL, ");
            sb.append(SHAPE).append(" TEXT, ");
            sb.append(WIDTH).append(" REAL, ");
            sb.append(TEXTSIZE).append(" REAL, ");
            sb.append(TEXTFIELD).append(" TEXT, ");
            sb.append(ENABLED).append(" INTEGER, ");
            sb.append(ORDER).append(" INTEGER,");
            sb.append(DASH).append(" TEXT,");
            sb.append(MINZOOM).append(" INTEGER,");
            sb.append(MAXZOOM).append(" INTEGER,");
            sb.append(DECIMATION).append(" REAL");
            sb.append(" );");
            String query = sb.toString();
            db_java.exec(query, null);

            for( SpatialVectorTable spatialTable : vectorTableList ) {
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
                sbIn.append(TEXTSIZE).append(" , ");
                sbIn.append(TEXTFIELD).append(" , ");
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
                style.name = spatialTable.getUniqueName();
                sbIn.append(style.insertValuesString());
                sbIn.append(" );");

                String insertQuery = sbIn.toString();
                db_java.exec(insertQuery, null);
            }
        }
    }

    /**
     * Retrieve the {@link Style} for a given table.
     *
     * @param tableName
     * @return
     * @throws Exception
     */
    public Style getStyle4Table( String tableName ) throws Exception {
        Style style = new Style();
        style.name = tableName;

        StringBuilder sbSel = new StringBuilder();
        sbSel.append("select ");
        sbSel.append(SIZE).append(" , ");
        sbSel.append(FILLCOLOR).append(" , ");
        sbSel.append(STROKECOLOR).append(" , ");
        sbSel.append(FILLALPHA).append(" , ");
        sbSel.append(STROKEALPHA).append(" , ");
        sbSel.append(SHAPE).append(" , ");
        sbSel.append(WIDTH).append(" , ");
        sbSel.append(TEXTSIZE).append(" , ");
        sbSel.append(TEXTFIELD).append(" , ");
        sbSel.append(ENABLED).append(" , ");
        sbSel.append(ORDER).append(" , ");
        sbSel.append(DASH).append(" , ");
        sbSel.append(MINZOOM).append(" , ");
        sbSel.append(MAXZOOM).append(" , ");
        sbSel.append(DECIMATION);
        sbSel.append(" from ");
        sbSel.append(PROPERTIESTABLE);
        sbSel.append(" where ");
        sbSel.append(NAME).append(" ='").append(tableName).append("';");

        String selectQuery = sbSel.toString();
        Stmt stmt = db_java.prepare(selectQuery);
        try {
            if (stmt.step()) {
                style.size = (float) stmt.column_double(0);
                style.fillcolor = stmt.column_string(1);
                style.strokecolor = stmt.column_string(2);
                style.fillalpha = (float) stmt.column_double(3);
                style.strokealpha = (float) stmt.column_double(4);
                style.shape = stmt.column_string(5);
                style.width = (float) stmt.column_double(6);
                style.textsize = (float) stmt.column_double(7);
                style.textfield = stmt.column_string(8);
                style.enabled = stmt.column_int(9);
                style.order = stmt.column_int(10);
                style.dashPattern = stmt.column_string(11);
                style.minZoom = stmt.column_int(12);
                style.maxZoom = stmt.column_int(13);
                style.decimationFactor = (float) stmt.column_double(14);
            }
        } finally {
            stmt.close();
        }
        return style;
    }

    public void resetStyleTable() throws Exception {
        GPLog.androidLog(-1, "Resetting style table.");
        StringBuilder sbSel = new StringBuilder();
        sbSel.append("drop table " + PROPERTIESTABLE + ";");

        String selectQuery = sbSel.toString();
        Stmt stmt = db_java.prepare(selectQuery);
        try {
            stmt.step();
        } finally {
            stmt.close();
        }

        checkPropertiesTable();
    }

    // -----------------------------------------------
    /**
      * Check of the Bounds of all the Vector-Tables collected in this class
      * Goal: when painting the Geometries: check of viewport is inside these bounds
      * - if the Viewport is outside these Bounds: all Tables can be ignored
      * -- this is called when the Tables are created
      * @param boundsCoordinates [as wsg84]
      * @return true if the given bounds are inside the bounds of the all the Tables ; otherwise false
      */
    public boolean checkBounds( double[] boundsCoordinates ) {
        boolean b_rc = false;
        if ((boundsCoordinates[0] >= this.bounds_west) && (boundsCoordinates[1] >= this.bounds_south)
                && (boundsCoordinates[2] <= this.bounds_east) && (boundsCoordinates[3] <= this.bounds_north)) {
            b_rc = true;
        }
        return b_rc;
    }
    // -----------------------------------------------
    /**
      * Check of the Bounds of a specfic Vector-Tables
      * Goal: when painting the Geometries: check of viewport is inside these bounds
      * - if the Viewport is outside these Bounds: all Tables can be ignored
      * -- this is called when the Tables are created
      * @param boundsCoordinates [as wsg84]
      * @param spatialTable The table to check
      * @return true if the given bounds are inside the bounds of the all the Tables ; otherwise false
      */
    public boolean checkTableBounds( double[] boundsCoordinates, SpatialVectorTable spatialTable ) {
        boolean b_rc = false;
        if (checkBounds(boundsCoordinates)) {
            b_rc = spatialTable.checkBounds(boundsCoordinates);
        }
        return b_rc;
    }
    // -----------------------------------------------
    /**
      * Retrieve Bounds of a Vector-Tables (as float)
      * - this is calculated when the Tables are created and stored as wsg84
      * @param spatialTable The table to check
      * @param destSrid The table to check
      * @return bounds as wsg84 [floats]
      */
    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception {
        return spatialTable.getTableBounds();
    }

    /**
     * Update a style definition.
     *
     * @param style the {@link Style} to set.
     * @throws Exception
     */
    public void updateStyle( Style style ) throws Exception {
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
        sbIn.append(TEXTSIZE).append("=").append(style.textsize).append(" , ");
        sbIn.append(TEXTFIELD).append("='").append(style.textfield).append("' , ");
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
        db_java.exec(updateQuery, null);
    }

    @Override
    public Paint getFillPaint4Style( Style style ) {
        Paint paint = fillPaints.get(style.name);
        if (paint == null) {
            paint = new Paint();
            fillPaints.put(style.name, paint);
        }
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtilities.toColor(style.fillcolor));
        float alpha = style.fillalpha * 255f;
        paint.setAlpha((int) alpha);
        return paint;
    }

    @Override
    public Paint getStrokePaint4Style( Style style ) {
        Paint paint = strokePaints.get(style.name);
        if (paint == null) {
            paint = new Paint();
            strokePaints.put(style.name, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Cap.ROUND);
        paint.setStrokeJoin(Join.ROUND);
        paint.setColor(ColorUtilities.toColor(style.strokecolor));
        float alpha = style.strokealpha * 255f;
        paint.setAlpha((int) alpha);
        paint.setStrokeWidth(style.width);

        String dashPattern = style.dashPattern;
        if (dashPattern.trim().length() > 0) {
            String[] split = dashPattern.split(",");
            if (split.length > 1) {
                float[] dash = new float[split.length];
                for( int i = 0; i < split.length; i++ ) {
                    try {
                        float tmpDash = Float.parseFloat(split[i].trim());
                        dash[i] = tmpDash;
                    } catch (NumberFormatException e) {
                        // ignore and set default
                        dash = new float[]{20f, 10f};
                        break;
                    }
                }
                paint.setPathEffect(new DashPathEffect(dash, 0));
            }
        }

        return paint;
    }
    // -----------------------------------------------
    /**
      * Retrieve Bounds of a Vector-Tables (as float)
      * - this is calculated when the Tables are created and stored as wsg84
      * @param spatialTable The table to check
      * @param destSrid The table to check
      * @return bounds as wsg84 [floats]
      */
    public List<byte[]> getWKBFromTableInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e, double w ) {
        List<byte[]> list = new ArrayList<byte[]>();
        String query = buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        try {
            Stmt stmt = db_java.prepare(query);
            try {
                while( stmt.step() ) {
                    list.add(stmt.column_bytes(0));
                }
            } finally {
                stmt.close();
            }
            GPLog.androidLog(-1,
                    "SpatialiteDatabaseHandler.getWKBFromTableInBounds srid[" + destSrid + "] name[" + table.getGeomName()
                            + "] size[" + list.size() + "]query[" + query + "]");
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] getRasterTile( String query ) {
        try {
            Stmt stmt = db_java.prepare(query);
            try {
                if (stmt.step()) {
                    byte[] bytes = stmt.column_bytes(0);
                    return bytes;
                }
            } finally {
                stmt.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e,
            double w ) {
        String query = buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        GPLog.androidLog(-1, "SpatialiteDatabaseHandler.getGeometryIteratorInBounds[" + table.getUniqueName() + "]: query["
                + query + "]");
        return new GeometryIterator(db_java, query);
    }

    private String buildGeometriesInBoundsQuery( String destSrid, SpatialVectorTable table, double n, double s, double e, double w ) {
        boolean doTransform = false;
        if (!table.getSrid().equals(destSrid)) {
            doTransform = true;
        }
        StringBuilder mbrSb = new StringBuilder();
        if (doTransform)
            mbrSb.append("ST_Transform(");
        mbrSb.append("BuildMBR(");
        mbrSb.append(w);
        mbrSb.append(",");
        mbrSb.append(n);
        mbrSb.append(",");
        mbrSb.append(e);
        mbrSb.append(",");
        mbrSb.append(s);
        if (doTransform) {
            mbrSb.append(",");
            mbrSb.append(destSrid);
            mbrSb.append("),");
            mbrSb.append(table.getSrid());
        }
        mbrSb.append(")");
        String mbr = mbrSb.toString();
        StringBuilder qSb = new StringBuilder();
        qSb.append("SELECT ST_AsBinary(CastToXY(");
        if (doTransform)
            qSb.append("ST_Transform(");
        qSb.append(table.getGeomName());
        if (doTransform) {
            qSb.append(",");
            qSb.append(destSrid);
            qSb.append(")");
        }
        qSb.append("))");
        qSb.append(" FROM ");
        qSb.append(table.getName());
        // the SpatialIndex would be searching for a square, the ST_Intersects the Geometry
        // the SpatialIndex could be fulfilled, but checking the Geometry could return the result
        // that it is not
        qSb.append(" WHERE ST_Intersects(");
        qSb.append(table.getGeomName());
        qSb.append(", ");
        qSb.append(mbr);
        qSb.append(") = 1");
        qSb.append(" AND ROWID IN (");
        qSb.append("SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
        qSb.append(table.getName());
        qSb.append("'");
        // if a table has more than 1 geometry, the column-name MUST be given, otherwise no results.
        qSb.append(" AND f_geometry_column = '");
        qSb.append(table.getGeomName());
        qSb.append("'");
        qSb.append(" AND search_frame = ");
        qSb.append(mbr);
        qSb.append(");");
        String q = qSb.toString();
        return q;
    }
    /**
     * Close  all Databases that may be open
     * <p>sqlite 'SpatialRasterTable,SpatialVectorTable and MBTilesDroidSpitter' databases will be closed with '.close();' if active
     */
    public void close() throws Exception {
        if (db_java != null) {
            db_java.close();
        }
    }

    public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
            double w, StringBuilder sb, String indentStr ) throws Exception {
        boolean doTransform = false;
        if (!spatialTable.getSrid().equals(boundsSrid)) {
            doTransform = true;
        }

        String query = null;

        // SELECT che-cazzo-ti-pare-a-te
        // FROM qualche-tavola
        // WHERE ROWID IN (
        // SELECT ROWID
        // FROM SpatialIndex
        // WHERE f_table_name = 'qualche-tavola'
        // AND search_frame = il-tuo-bbox
        // );

        // {
        // StringBuilder sbQ = new StringBuilder();
        // sbQ.append("SELECT ");
        // sbQ.append("*");
        // sbQ.append(" from ").append(spatialTable.name);
        // sbQ.append(" where ROWID IN (");
        // sbQ.append(" SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
        // sbQ.append(spatialTable.name);
        // sbQ.append("' AND search_frame = ");
        // if (doTransform)
        // sbQ.append("ST_Transform(");
        // sbQ.append("BuildMBR(");
        // sbQ.append(w);
        // sbQ.append(", ");
        // sbQ.append(s);
        // sbQ.append(", ");
        // sbQ.append(e);
        // sbQ.append(", ");
        // sbQ.append(n);
        // if (doTransform) {
        // sbQ.append(", ");
        // sbQ.append(boundsSrid);
        // }
        // sbQ.append(")");
        // if (doTransform) {
        // sbQ.append(",");
        // sbQ.append(spatialTable.srid);
        // sbQ.append(")");
        // }
        // sbQ.append(");");
        //
        // query = sbQ.toString();
        // Logger.i(this, query);
        // }
        {
            StringBuilder sbQ = new StringBuilder();
            sbQ.append("SELECT ");
            sbQ.append("*");
            sbQ.append(" FROM ").append(spatialTable.getName());
            sbQ.append(" WHERE ST_Intersects(");
            if (doTransform)
                sbQ.append("ST_Transform(");
            sbQ.append("BuildMBR(");
            sbQ.append(w);
            sbQ.append(",");
            sbQ.append(s);
            sbQ.append(",");
            sbQ.append(e);
            sbQ.append(",");
            sbQ.append(n);
            if (doTransform) {
                sbQ.append(",");
                sbQ.append(boundsSrid);
                sbQ.append("),");
                sbQ.append(spatialTable.getSrid());
            }
            sbQ.append("),");
            sbQ.append(spatialTable.getGeomName());
            sbQ.append(");");

            query = sbQ.toString();

            // Logger.i(this, query);
        }

        Stmt stmt = db_java.prepare(query);
        try {
            while( stmt.step() ) {
                int column_count = stmt.column_count();
                for( int i = 0; i < column_count; i++ ) {
                    String cName = stmt.column_name(i);
                    if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
                        continue;
                    }

                    String value = stmt.column_string(i);
                    sb.append(indentStr).append(cName).append(": ").append(value).append("\n");
                }
                sb.append("\n");
            }
        } finally {
            stmt.close();
        }
    }

    public void intersectionToString4Polygon( String queryPointSrid, SpatialVectorTable spatialTable, double n, double e,
            StringBuilder sb, String indentStr ) throws Exception {
        boolean doTransform = false;
        if (!spatialTable.getSrid().equals(queryPointSrid)) {
            doTransform = true;
        }

        StringBuilder sbQ = new StringBuilder();
        sbQ.append("SELECT * FROM ");
        sbQ.append(spatialTable.getName());
        sbQ.append(" WHERE ST_Intersects(");
        sbQ.append(spatialTable.getGeomName());
        sbQ.append(",");
        if (doTransform)
            sbQ.append("ST_Transform(");
        sbQ.append("MakePoint(");
        sbQ.append(e);
        sbQ.append(",");
        sbQ.append(n);
        if (doTransform) {
            sbQ.append(",");
            sbQ.append(queryPointSrid);
            sbQ.append("),");
            sbQ.append(spatialTable.getSrid());
        }
        sbQ.append(")) = 1 ");
        sbQ.append("AND ROWID IN (");
        sbQ.append("SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
        sbQ.append(spatialTable.getName());
        sbQ.append("'");
        // if a table has more than 1 geometry, the column-name MUST be given, otherwise no results.
        sbQ.append(" AND f_geometry_column = '");
        sbQ.append(spatialTable.getGeomName());
        sbQ.append("'");
        sbQ.append(" AND search_frame = ");
        if (doTransform)
            sbQ.append("ST_Transform(");
        sbQ.append("MakePoint(");
        sbQ.append(e);
        sbQ.append(",");
        sbQ.append(n);
        if (doTransform) {
            sbQ.append(",");
            sbQ.append(queryPointSrid);
            sbQ.append("),");
            sbQ.append(spatialTable.getSrid());
        }
        sbQ.append("));");
        String query = sbQ.toString();

        Stmt stmt = db_java.prepare(query);
        try {
            while( stmt.step() ) {
                int column_count = stmt.column_count();
                for( int i = 0; i < column_count; i++ ) {
                    String cName = stmt.column_name(i);
                    if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
                        continue;
                    }

                    String value = stmt.column_string(i);
                    sb.append(indentStr).append(cName).append(": ").append(value).append("\n");
                }
                sb.append("\n");
            }
        } finally {
            stmt.close();
        }
    }
    // -----------------------------------------------
    /**
      * Load list of Table [Vector/Raster] for GeoPackage Files [gpkg]
      * - name of Field
      * - type of field as defined in Database
      * @param s_table name of table to read [if empty: list of tables in Database]
      * @param i_parm [for use when s_table is empty] 0=do not load table ; 1=load tables
      * @return fields_list [name of field, type of field]
      */
    private HashMap<String, String> get_tables_gpkg( int i_parm ) throws Exception {
        Stmt this_stmt = null;
        List<SpatialVectorTable> vector_TableList;
        List<SpatialRasterTable> raster_TableList;
        HashMap<String, String> table_fields = new HashMap<String, String>();
        String s_srid = "";
        int i_srid = 0;
        String s_table_name = "";
        String s_tiles_field_name = "";
        String s_data_type = "";
        String s_sql_layers = "";
        int[] zoomLevels = {0, 22};
        switch( i_database_type ) {
        case 10: { // GeoPackage Files [gpkg]
            StringBuilder sb_layers = new StringBuilder();
            s_sql_layers = "SELECT data_type,table_name,srid FROM geopackage_contents";
            // Luciad_GeoPackage.gpkg: Assume that 1=4326 ; 2=3857
            // [features] [lakemead_clipped] [1]
            // [tiles] [o18229_tif_tiles] [2]
            // [featuresWithRasters] [observations] [2]
            // this is a list of jpeg-images and points - the points have wsg84 values but are set
            // as 2
            // -- the srid for tiles can also be retrieved from raster_columns.srid [also 2]
            // Sample_Geopackage_Haiti.gpkg:
            // [tiles] [fromosm_tiles] [3857]
            // [features] [geonames] [4326]
            // 'features' == vector ; 'tiles' = raster
            // SELECT table_name,srid FROM geopackage_contents WHERE data_type = 'features';
            this_stmt = db_java.prepare(s_sql_layers);
            try {
                while( this_stmt.step() ) {
                    i_srid = 0;
                    s_data_type = this_stmt.column_string(0);
                    // filter out everything we have no idea how to deal with
                    if ((s_data_type.equals("features")) || (s_data_type.equals("tiles"))) { // 'featuresWithRasters'
                                                                                             // is
                                                                                             // being
                                                                                             // ignored
                                                                                             // until
                                                                                             // further
                                                                                             // notice
                        s_table_name = this_stmt.column_string(1);
                        s_srid = this_stmt.column_string(2);
                        if (!s_srid.equals("")) {
                            i_srid = Integer.parseInt(s_srid);
                            if ((i_srid > 0) && (i_srid < 3)) {
                                if (i_srid == 1)
                                    i_srid = 4326;
                                if (i_srid == 2)
                                    i_srid = 3857;
                            }
                            if (i_srid > 3)
                                table_fields.put(s_table_name, i_srid + ";" + s_data_type);
                        }
                    }
                }
            } finally {
                this_stmt.close();
            }
            vector_TableList = new ArrayList<SpatialVectorTable>();
            raster_TableList = new ArrayList<SpatialRasterTable>();
            HashMap<String, String> table_list = new HashMap<String, String>();
            table_fields = new HashMap<String, String>();
            for( int i = 0; i < table_fields.size(); i++ ) {
                for( Map.Entry<String, String> table_entry : table_list.entrySet() ) {
                    s_table_name = table_entry.getKey();
                    s_data_type = table_entry.getValue();
                    s_tiles_field_name = "";
                    String[] sa_split = s_data_type.split(";");
                    if (sa_split.length == 2) {
                        s_srid = sa_split[0];
                        i_srid = Integer.parseInt(s_srid);
                        s_data_type = sa_split[1];
                    }
                    // for 'tiles' the zoom levels
                    if ((!s_table_name.equals("")) && (s_data_type.equals("tiles"))) {
                        // SELECT min(zoom_level),max(zoom_level) FROM tile_matrix_metadata WHERE
                        // t_table_name = '' SELECT min(zoom_level),max(zoom_level) FROM
                        // tile_matrix_metadata WHERE t_table_name = 'o18229_tif_tiles' SELECT
                        // min(zoom_level),max(zoom_level) FROM tile_matrix_metadata WHERE
                        // t_table_name = 'fromosm_tiles'
                        sb_layers.append("SELECT min(");
                        sb_layers.append(METADATA_ZOOM_LEVEL);
                        sb_layers.append("),max(");
                        sb_layers.append(METADATA_ZOOM_LEVEL);
                        sb_layers.append(") FROM ");
                        sb_layers.append(METADATA_TABLE_TILE_MATRIX);
                        sb_layers.append(" WHERE ");
                        sb_layers.append(METADATA_TILE_TABLE_NAME);
                        sb_layers.append("='");
                        sb_layers.append(s_table_name);
                        sb_layers.append("';");
                        s_sql_layers = sb_layers.toString();
                        sb_layers = new StringBuilder();
                        this_stmt = db_java.prepare(s_sql_layers);
                        try {
                            if (this_stmt.step()) {
                                zoomLevels[0] = this_stmt.column_int(0);
                                zoomLevels[1] = this_stmt.column_int(1);
                            }
                        } finally {
                            if (this_stmt != null) {
                                this_stmt.close();
                            }
                        }
                        // SELECT r_table_name,r_raster_column,srid FROM raster_columns
                        // SELECT r_raster_column,srid FROM raster_columns WHERE r_table_name= ''
                        // we allready have the srid, do not retrieve it again
                        sb_layers.append("SELECT ");
                        sb_layers.append(METADATA_RASTER_COLUMN);
                        sb_layers.append(" FROM ");
                        sb_layers.append(METADATA_TABLE_RASTER_COLUMNS);
                        sb_layers.append(" WHERE ");
                        sb_layers.append(METADATA_RASTER_TABLE_NAME);
                        sb_layers.append("='");
                        sb_layers.append(s_table_name);
                        sb_layers.append(";");
                        s_sql_layers = sb_layers.toString();
                        sb_layers = new StringBuilder();
                        this_stmt = db_java.prepare(s_sql_layers);
                        try {
                            if (this_stmt.step()) {
                                s_tiles_field_name = this_stmt.column_string(0);
                            }
                        } finally {
                            if (this_stmt != null) {
                                this_stmt.close();
                            }
                        }
                    }
                    // for 'features' and 'tiles' the bounds
                    if (!s_table_name.equals("")) {
                        if (!s_srid.equals("4326")) { // SELECT
                                                      // CastToXY(ST_Transform(MakePoint((min_x +
                                                      // (max_x-min_x)/2), (min_y +
                                                      // (max_y-min_y)/2), srid),4326)) AS
                                                      // Center,CastToXY(ST_Transform(MakePoint(min_x,min_y,
                                                      // srid),4326)) AS
                                                      // South_West,CastToXY(ST_Transform(MakePoint(max_x,max_y,
                                                      // srid), 4326)) AS North_East FROM
                                                      // geopackage_contents WHERE data_type =
                                                      // 'features';
                                                      // srid has been properly set
                                                      // [Sample_Geopackage_Haiti.gpkg, but was 4326
                                                      // and does not need to be transformed]
                            sb_layers.append("SELECT ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("(min_x + (max_x-min_x)/2), ");
                            sb_layers.append("(min_y + (max_y-min_y)/2), ");
                            sb_layers.append(METADATA_SRID);
                            sb_layers.append("),4326))) AS Center,");
                            sb_layers.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("min_x,min_y, ");
                            sb_layers.append(METADATA_SRID);
                            sb_layers.append("),4326))) AS South_West,");
                            sb_layers.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("max_x,max_y, ");
                            sb_layers.append(METADATA_SRID);
                            sb_layers.append("),4326))) AS North_East FROM ");
                            sb_layers.append(METADATA_TABLE_GEOPACKAGE_CONTENTS);
                            sb_layers.append(" WHERE ");
                            sb_layers.append(METADATA_GEOPACKAGECONTENT_TABLE_NAME);
                            sb_layers.append("='");
                            sb_layers.append(s_table_name);
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE);
                            // sb_layers.append("='");
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE_FEATURES);
                            sb_layers.append("';");
                        } else { // SELECT CastToXY(MakePoint((min_x + (max_x-min_x)/2), (min_y +
                                 // (max_y-min_y)/2),4326)) AS
                                 // Center,CastToXY(MakePoint(min_x,min_y,4326)) AS
                                 // South_West,CastToXY(MakePoint(max_x,max_y, 4326)) AS North_East
                                 // FROM geopackage_contents WHERE data_type = 'features';
                                 // srid has NOT been properly set - should be 4326 is: (1,2,3)
                                 // [Luciad_GeoPackage.gpkg] ; try 4326 [wsg84]
                            sb_layers.append("SELECT ST_AsBinary(CastToXY(MakePoint(");
                            sb_layers.append("(min_x + (max_x-min_x)/2), ");
                            sb_layers.append("(min_y + (max_y-min_y)/2),");
                            sb_layers.append("4326))) AS Center,");
                            sb_layers.append("ST_AsBinary(CastToXY(MakePoint(");
                            sb_layers.append("min_x,min_y,");
                            sb_layers.append("4326))) AS South_West,");
                            sb_layers.append("ST_AsBinary(CastToXY(MakePoint(");
                            sb_layers.append("max_x,max_y,");
                            sb_layers.append("4326))) AS North_East FROM ");
                            sb_layers.append(METADATA_TABLE_GEOPACKAGE_CONTENTS);
                            sb_layers.append(" WHERE ");
                            sb_layers.append(METADATA_GEOPACKAGECONTENT_TABLE_NAME);
                            sb_layers.append("='");
                            sb_layers.append(s_table_name);
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE);
                            // sb_layers.append("='");
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE_FEATURES);
                            sb_layers.append("';");
                        }
                        s_sql_layers = sb_layers.toString();
                        if (!s_sql_layers.equals("")) {
                            b_database_valid = true;
                            String geometry_column = "";
                            // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+getFileNamePath()+"] sql["
                            // + s_sql_layers+ "] valid["+b_database_valid+"] ");
                            try {
                                this_stmt = db_java.prepare(s_sql_layers);
                                while( this_stmt.step() ) {
                                    String s_layer_type = "geometry";
                                    int geometry_type = 0;
                                    double[] centerCoordinate = {0.0, 0.0};
                                    double[] boundsCoordinates = {-180.0f, -85.05113f, 180.0f, 85.05113f};
                                    int i_row_count = 0;
                                    int i_coord_dimension = 0;
                                    int i_spatial_index_enabled = 0;
                                    String s_last_verified = "";
                                    int i_valid = 0;
                                    WKBReader wkbReader = new WKBReader();
                                    byte[] geomBytes = this_stmt.column_bytes(0);
                                    Geometry geometry = wkbReader.read(geomBytes);
                                    Coordinate coordinate = geometry.getCoordinate();
                                    centerCoordinate[0] = coordinate.x;
                                    centerCoordinate[1] = coordinate.y;
                                    geomBytes = this_stmt.column_bytes(1);
                                    geometry = wkbReader.read(geomBytes);
                                    coordinate = geometry.getCoordinate();
                                    boundsCoordinates[0] = coordinate.x;
                                    boundsCoordinates[1] = coordinate.y;
                                    geomBytes = this_stmt.column_bytes(2);
                                    geometry = wkbReader.read(geomBytes);
                                    coordinate = geometry.getCoordinate();
                                    boundsCoordinates[2] = coordinate.x;
                                    boundsCoordinates[3] = coordinate.y;
                                    class_bounds(boundsCoordinates, zoomLevels); // Zoom levels with
                                                                                 // non-vector data
                                    if (s_data_type.equals("features")) {
                                    }
                                    if (s_data_type.equals("tiles")) {
                                        SpatialRasterTable table = new SpatialRasterTable(getFileNamePath(), "", s_srid,
                                                zoomLevels[0], zoomLevels[1], centerCoordinate[0], centerCoordinate[1], null,
                                                boundsCoordinates);
                                        table.setMapType("gpkg");
                                        table.setTableName(s_table_name);
                                        table.setColumnName(s_tiles_field_name);
                                        setDescription(s_table_name);
                                        table.setDescription(this.s_description);
                                        raster_TableList.add(table);
                                    }
                                }
                            } catch (java.lang.Exception e) {
                            } finally {
                                if (this_stmt != null) {
                                    this_stmt.close();
                                }
                            }
                            if (vector_TableList.size() > 0)
                                vectorTableList = vector_TableList;
                            if (raster_TableList.size() > 0)
                                rasterTableList = raster_TableList;
                        }
                    }
                }
            }
        }
            break;
        }
        return table_fields;
    }
    // -----------------------------------------------
    /**
      * Load list of Table [Vector] for Spatialite Files
      * - name of Field
      * - type of field as defined in Database
      * @param s_table name of table to read [if empty: list of tables in Database]
      * @param i_parm [for use when s_table is empty] 0=do not load table ; 1=load tables
      * @return fields_list [name of field, type of field]
      */
    private HashMap<String, String> get_tables_spatialite( int i_parm ) throws Exception {
        Stmt this_stmt = null;
        List<SpatialVectorTable> vector_TableList;
        HashMap<String, String> table_fields = new HashMap<String, String>();
        StringBuilder sb_layers = new StringBuilder();
        String s_srid = "";
        int i_srid = 0;
        String table_name = "";
        String s_data_type = "";
        String s_sql_layers = "";
        int[] zoomLevels = {0, 22};
        switch( i_database_type ) {
        case 3: { // Spatialite Files version 2+3=3
            sb_layers.append("SELECT ");
            sb_layers.append(METADATA_TABLE_NAME);
            sb_layers.append(", ");
            sb_layers.append(METADATA_GEOMETRY_COLUMN);
            sb_layers.append(", ");
            sb_layers.append(METADATA_GEOMETRY_TYPE3);
            sb_layers.append(",");
            sb_layers.append(METADATA_SRID);
            sb_layers.append(" FROM ");
            sb_layers.append(METADATA_TABLE_GEOMETRY_COLUMNS);
            sb_layers.append("  ORDER BY " + METADATA_TABLE_NAME + ";");
            // version 3 ['type' instead of 'geometry_type']:
            // SELECT f_table_name,f_geometry_column,geometry_type,srid FROM geometry_columns ORDER
            // BY
            // f_table_name
            s_sql_layers = sb_layers.toString();
        }
            break;
        case 4: { // Spatialite Files version 4=4
            sb_layers.append("SELECT ");
            sb_layers.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name"); // 0
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column"); // 1
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + METADATA_GEOMETRY_TYPE4); // 2
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + METADATA_SRID); // 3
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".layer_type"); // 4
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".row_count"); // 5
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_x"); // 6
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_min_y"); // 7
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_x"); // 8
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".extent_max_y"); // 9
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_TABLE_NAME + ".coord_dimension"); // 10
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_TABLE_NAME + ".spatial_index_enabled"); // 11
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".last_verified"); // 12
            sb_layers.append(" FROM " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + "," + METADATA_VECTOR_LAYERS_TABLE_NAME);
            sb_layers.append(" WHERE((" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name="
                    + METADATA_VECTOR_LAYERS_TABLE_NAME + ".table_name) AND");
            sb_layers.append(" (" + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column="
                    + METADATA_VECTOR_LAYERS_TABLE_NAME + ".geometry_column))  ORDER BY "
                    + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name");
            // SELECT
            // vector_layers_statistics.table_name,vector_layers_statistics.geometry_column,vector_layers.geometry_type,
            // vector_layers.srid,vector_layers_statistics.layer_type,vector_layers_statistics.row_count,
            // vector_layers_statistics.extent_min_x,vector_layers_statistics.extent_min_y,vector_layers_statistics.extent_max_x,
            // vector_layers_statistics.extent_max_y,vector_layers.coord_dimension,vector_layers.spatial_index_enabled,
            // vector_layers_statistics.last_verified
            // FROM vector_layers_statistics,vector_layers WHERE
            // ((vector_layers_statistics.table_name = vector_layers.table_name) AND
            // (vector_layers_statistics.geometry_column = vector_layers.geometry_column)) ORDER BY
            // vector_layers_statistics.table_name
            s_sql_layers = sb_layers.toString();
            // version 4 ['geometry_type' instead of 'type']: SELECT
            // f_table_name,f_geometry_column,geometry_type,srid FROM geometry_columns ORDER BY
            // f_table_name
        }
            break;
        }
        if (!s_sql_layers.equals("")) {
            sb_layers = new StringBuilder();
            b_database_valid = true;
            vector_TableList = new ArrayList<SpatialVectorTable>();
            HashMap<String, String> table_list = new HashMap<String, String>();
            table_fields = new HashMap<String, String>();
            String geometry_column = "";
            // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+getFileNamePath()+"] sql[" +
            // s_sql_layers+ "] valid["+b_database_valid+"] ");
            // if a UpdateLayerStatistics is needed, do it only once [assume it is needed]
            boolean b_UpdateLayerStatistics = true;
            try {
                this_stmt = db_java.prepare(s_sql_layers);
                while( this_stmt.step() ) {
                    String s_layer_type = "geometry";
                    int i_geometry_type = 0;
                    String s_geometry_type = "";
                    double[] centerCoordinate = {0.0, 0.0};
                    double[] boundsCoordinates = {-180.0f, -85.05113f, 180.0f, 85.05113f};
                    int i_row_count = 0;
                    int i_coord_dimension = 0;
                    int i_spatial_index_enabled = 0;
                    String s_last_verified = "";
                    HashMap<String, String> fields_list = new HashMap<String, String>();
                    int i_valid = 0;
                    table_name = this_stmt.column_string(0);
                    // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+getFileNamePath()+"] tablename["+table_name+"]");
                    geometry_column = this_stmt.column_string(1);
                    i_srid = this_stmt.column_int(3);
                    s_srid = String.valueOf(i_srid);
                    sb_layers = new StringBuilder();
                    // SELECT Min(MbrMinX(coord_geometry)) AS min_x, Min(MbrMinY(coord_geometry)) AS
                    // min_y,Max(MbrMaxX(coord_geometry)) AS max_x, Max(MbrMaxY(coord_geometry)) AS
                    // max_y FROM geodb_geometry
                    sb_layers.append("SELECT Min(MbrMinX(");
                    sb_layers.append(geometry_column);
                    sb_layers.append(")) AS min_x, Min(MbrMinY(");
                    sb_layers.append(geometry_column);
                    sb_layers.append(")) AS min_y,");
                    sb_layers.append("Max(MbrMaxX(");
                    sb_layers.append(geometry_column);
                    sb_layers.append(")) AS max_x, Max(MbrMaxY(");
                    sb_layers.append(geometry_column);
                    sb_layers.append(")) AS max_y, count(");
                    sb_layers.append(geometry_column);
                    sb_layers.append(") AS i_row_count ");
                    sb_layers.append(" FROM ");
                    sb_layers.append(table_name);
                    sb_layers.append(";");
                    String s_select_bounds = sb_layers.toString();
                    Stmt bounds_stmt = null;
                    int i_test = 0; // i_CheckSpatialIndex is returning 0 all the time and can be
                                    // used
                    if ((!table_name.equals("")) && (!geometry_column.equals("")) && (i_test > 0)) { // checking
                                                                                                     // logic
                                                                                                     // :CheckSpatialIndex,CreateSpatialIndex,RecoverSpatialIndex
                                                                                                     // SELECT
                                                                                                     // CheckSpatialIndex('oddity','geometry');
                                                                                                     // NULL
                                                                                                     // will
                                                                                                     // be
                                                                                                     // returned
                                                                                                     // if
                                                                                                     // the
                                                                                                     // requested
                                                                                                     // RTree
                                                                                                     // doesn't
                                                                                                     // exists
                                                                                                     // -->
                                                                                                     // SELECT
                                                                                                     // CreateSpatialIndex("oddity","geometry");
                                                                                                     // 0
                                                                                                     // will
                                                                                                     // be
                                                                                                     // returned
                                                                                                     // if
                                                                                                     // it
                                                                                                     // needs
                                                                                                     // to
                                                                                                     // be
                                                                                                     // recovered
                                                                                                     // -->
                                                                                                     // SELECT
                                                                                                     // RecoverSpatialIndex("oddity","geometry",0);
                        String s_CheckSpatialIndex = "SELECT CheckSpatialIndex('" + table_name + "','" + geometry_column + "');";
                        int i_CheckSpatialIndex = -1;
                        try {
                            bounds_stmt = db_java.prepare(s_CheckSpatialIndex);
                            if (bounds_stmt.step()) {
                                i_CheckSpatialIndex = bounds_stmt.column_int(0);
                            }
                        } catch (Exception e) {
                        } finally {
                            if (bounds_stmt != null) {
                                bounds_stmt.close();
                            }
                            if (i_CheckSpatialIndex < 1) {
                                GPLog.androidLog(-1, "SpatialiteDatabaseHandler[" + getFileNamePath() + "] tablename["
                                        + table_name + "] geometry_column[" + geometry_column + "] i_CheckSpatialIndex["
                                        + i_CheckSpatialIndex + "]");
                            }
                        }
                    }
                    if (i_database_type == 3) { // for older spatialite v2+3 : Query extent of table
                                                // and fill boundsCoordinates
                        s_geometry_type = this_stmt.column_string(2);
                        i_geometry_type = GeometryType.forValue(s_geometry_type);
                        try {
                            bounds_stmt = db_java.prepare(s_select_bounds);
                            if (bounds_stmt.step()) {
                                boundsCoordinates[0] = bounds_stmt.column_double(0);
                                boundsCoordinates[1] = bounds_stmt.column_double(1);
                                boundsCoordinates[2] = bounds_stmt.column_double(2);
                                boundsCoordinates[3] = bounds_stmt.column_double(3);
                                i_row_count = bounds_stmt.column_int(4);
                            }
                        } catch (Exception e) {
                        } finally {
                            if (bounds_stmt != null) {
                                bounds_stmt.close();
                            }
                        }
                    }
                    if (i_database_type == 4) { // for older spatialite v4 : Retrieve extent of
                                                // table from Query result and fill
                                                // boundsCoordinates
                        i_geometry_type = this_stmt.column_int(2);
                        GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
                        s_geometry_type = geometry_type.toString();
                        s_layer_type = this_stmt.column_string(4);
                        i_row_count = this_stmt.column_int(5);
                        boundsCoordinates[0] = this_stmt.column_double(6);
                        boundsCoordinates[1] = this_stmt.column_double(7);
                        boundsCoordinates[2] = this_stmt.column_double(8);
                        boundsCoordinates[3] = this_stmt.column_double(9);
                        i_coord_dimension = this_stmt.column_int(10);
                        i_spatial_index_enabled = this_stmt.column_int(11);
                        s_last_verified = this_stmt.column_string(12);
                        if ((boundsCoordinates[0] == 0) && (boundsCoordinates[1] == 0) && (boundsCoordinates[2] == 0)
                                && (boundsCoordinates[3] == 0)) { // Sometimes there are no results
                                                                  // for GEOMETRYCOLLECTION
                                                                  // String
                                                                  // s_bounds_zoom_sent=boundsCoordinates[0]+","+boundsCoordinates[1]+","+boundsCoordinates[2]+","+boundsCoordinates[3]+";";
                                                                  // GPLog.androidLog(-1,
                                                                  // "getSpatialVectorTables: geometycolletion 01 boundsCoordinates["
                                                                  // + s_bounds_zoom_sent+ "] ");
                                                                  // SELECT UpdateLayerStatistics();
                            if ((!s_layer_type.equals("")) && (i_row_count == 0)) { // at the moment
                                                                                    // we are
                                                                                    // reading one
                                                                                    // row of
                                                                                    // possibly many
                                                                                    // rows
                                if (b_UpdateLayerStatistics) { // do this only for the first row,
                                                               // the next time the application is
                                                               // run it will have a proper table
                                    String s_UpdateLayerStatistics = "SELECT UpdateLayerStatistics();";
                                    int i_UpdateLayerStatistics = -1;
                                    try {
                                        bounds_stmt = db_java.prepare(s_UpdateLayerStatistics);
                                        if (bounds_stmt.step()) {
                                            i_UpdateLayerStatistics = this_stmt.column_int(0);
                                        }
                                    } catch (Exception e) {
                                    } finally {
                                        if (bounds_stmt != null) {
                                            bounds_stmt.close();
                                        }
                                        if (i_UpdateLayerStatistics == 1) { // the next time this
                                                                            // application reads
                                                                            // this database it will
                                                                            // have a proper table
                                            b_UpdateLayerStatistics = false; // UpdateLayerStatistics
                                                                             // is not needed
                                        }
                                    }
                                }
                            }
                            if ((boundsCoordinates[0] == 0) && (boundsCoordinates[1] == 0) && (boundsCoordinates[2] == 0)
                                    && (boundsCoordinates[3] == 0)) { // this time (after
                                                                      // UpdateLayerStatistics) wel
                                                                      // will retrieve this
                                                                      // Information in an otherway
                                try {
                                    bounds_stmt = db_java.prepare(s_select_bounds);
                                    if (bounds_stmt.step()) {
                                        boundsCoordinates[0] = bounds_stmt.column_double(0);
                                        boundsCoordinates[1] = bounds_stmt.column_double(1);
                                        boundsCoordinates[2] = bounds_stmt.column_double(2);
                                        boundsCoordinates[3] = bounds_stmt.column_double(3);
                                        i_row_count = bounds_stmt.column_int(4);
                                    }
                                } catch (Exception e) {
                                } finally {
                                    if (bounds_stmt != null) {
                                        bounds_stmt.close();
                                    }
                                }
                            }
                        } else { // we have found a valid record
                                 // this will prevent UpdateLayerStatistics being called on empty
                                 // tables - when they ARE not the first table
                            b_UpdateLayerStatistics = false; // UpdateLayerStatistics is not needed
                        }
                    }
                    // this should have a list of unique geometry-fields, we will look later for
                    // these in the views
                    if (table_fields.get(geometry_column) == null)
                        table_fields.put(geometry_column, s_geometry_type);
                    if (!s_srid.equals("4326")) { // Transfor into wsg84 if needed
                        getSpatialVector_4326(s_srid, centerCoordinate, boundsCoordinates, 0);
                    } else {
                        centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
                        centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
                    }
                    class_bounds(boundsCoordinates, null); // no Zoom levels with vector data
                    SpatialVectorTable table = new SpatialVectorTable(getFileNamePath(), table_name, geometry_column,
                            i_geometry_type, s_srid, centerCoordinate, boundsCoordinates, s_layer_type, i_row_count,
                            i_coord_dimension, i_spatial_index_enabled, s_last_verified);
                    fields_list = get_table_fields(table_name, 0); // compleate list of fields of
                                                                   // this table
                    table.setFieldsList(fields_list);
                    vector_TableList.add(table);
                }
            } catch (Exception e) {
            } finally {
                if (this_stmt != null) {
                    this_stmt.close();
                }
            }
            vectorTableList = vector_TableList;
        }
        return table_fields;
    }
    // -----------------------------------------------
    /**
      * Calulation of the Bounds of all the Vector-Tables collected in this class
      * Goal: when painting the Geometries: check of viewport is inside these bounds
      * - if the Viewport is outside these Bounds: all Tables can be ignored
      * -- this is called when the Tables are created
      * @param boundsCoordinates 0=do not load table ; 1=load tables
      * @return nothing [class bounds,center and min/max Zoom-Levels (for possible Raster Tables) are set]
      */
    private void class_bounds( double[] boundsCoordinates, int[] zoomLevels ) {
        if ((this.bounds_west == 0.0) && (this.bounds_south == 0.0) && (this.bounds_east == 0.0) && (this.bounds_north == 0.0)) {
            this.bounds_west = boundsCoordinates[0];
            this.bounds_south = boundsCoordinates[1];
            this.bounds_east = boundsCoordinates[2];
            this.bounds_north = boundsCoordinates[2];
        } else {
            if (boundsCoordinates[0] < this.bounds_west)
                this.bounds_west = boundsCoordinates[0];
            if (boundsCoordinates[1] < this.bounds_south)
                this.bounds_south = boundsCoordinates[1];
            if (boundsCoordinates[2] > this.bounds_east)
                this.bounds_east = boundsCoordinates[2];
            if (boundsCoordinates[3] < this.bounds_north)
                this.bounds_north = boundsCoordinates[3];
        }
        centerX = this.bounds_west + (this.bounds_east - this.bounds_west) / 2;
        centerY = this.bounds_south + (this.bounds_north - this.bounds_south) / 2;
        if ((zoomLevels != null) && (zoomLevels.length == 2)) {
            if ((this.minZoom == 0) && (this.maxZoom == 0)) {
                this.minZoom = zoomLevels[0];
                this.maxZoom = zoomLevels[1];
            } else {
                if (zoomLevels[0] < this.minZoom)
                    this.minZoom = zoomLevels[0];
                if (zoomLevels[1] > this.maxZoom)
                    this.maxZoom = zoomLevels[1];
            }
        }
    }
    // -----------------------------------------------
    /**
      * General Load list of Table depending on Database Type
      * - name of Field
      * - type of field as defined in Database
      * @param i_parm 0=do not load table ; 1=load tables
      * @return fields_list [name of field, type of field]
      */
    private HashMap<String, String> get_tables( int i_parm ) throws Exception {
        HashMap<String, String> table_fields = new HashMap<String, String>();
        if (view_list == null) {
            table_fields = get_table_fields("", 0); // guess what we forgot on the first attempt!
        }
        if (i_parm == 0) {
            return table_fields;
        }
        switch( i_database_type ) {
        case 10: { // GeoPackage Files [gpkg]
                   // b_database_valid=false;
            table_fields = get_tables_gpkg(i_parm);
        }
            break;
        case 3:
        case 4: { // Spatialite Files version 2+3=3 ; version 4=4
                  // this will return a unique list of geometry-fields from all tables
            table_fields = get_tables_spatialite(i_parm);
        }
            break;
        }
        switch( i_database_type ) {
        case 3:
        case 4: { // Spatialite Files version 2+3=3 ; version 4=4
                  // 'table_fields' will have a unique list of geometry-fields from all tables
            for( int i = 0; i < view_list.size(); i++ ) {
                for( Map.Entry<String, String> view_entry : view_list.entrySet() ) {
                    String s_view_name = view_entry.getKey();
                    String s_view_data = view_entry.getValue(); // TODO remove newlines
                    GPLog.androidLog(-1, "SpatialiteDatabaseHandler[" + getFileNamePath() + "] view[" + s_view_name + "]   ");
                    // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+getFileNamePath()+"] view["+s_view_name+"] sql["
                    // + s_view_data+ "]  ");
                    // TODO: parse 's_view_data' for fields in 'table_fields'
                    // TODO: create a SpatialVectorTable for the views
                }
            }
        }
            break;
        }
        if (vectorTableList != null) {
            // now read styles
            checkPropertiesTable();
            // assign the styles
            for( SpatialVectorTable spatialTable : vectorTableList ) {
                Style style4Table = null;
                try {
                    style4Table = getStyle4Table(spatialTable.getUniqueName());
                } catch (java.lang.Exception e) {
                    resetStyleTable();
                }
                if (style4Table == null) {
                    spatialTable.makeDefaultStyle();
                } else {
                    spatialTable.setStyle(style4Table);
                }
            }
            OrderComparator orderComparator = new OrderComparator();
            Collections.sort(vectorTableList, orderComparator);
        }
        return table_fields;
    }
    // -----------------------------------------------
    /**
      * Return list of fields for a specific table
      * - name of Field
      * - type of field as defined in Database
      * @param s_table name of table to read [if empty: list of tables in Database]
      * @param i_parm [for use when s_table is empty] 0=do not load table ; 1=load tables
      * @return fields_list [name of field, type of field]
      */
    private HashMap<String, String> get_table_fields( String s_table, int i_parm ) throws Exception {
        Stmt this_stmt = null;
        // views: vector_layers_statistics,vector_layers
        boolean b_vector_layers_statistics = false;
        boolean b_vector_layers = false;
        // tables: geometry_columns,raster_columns
        boolean b_geometry_columns = false;
        boolean b_raster_columns = false;
        boolean b_geopackage_contents = false;
        HashMap<String, String> fields_list = new LinkedHashMap<String, String>();
        String s_sql_command = "";
        if (!s_table.equals("")) { // pragma table_info(geodb_geometry)
            s_sql_command = "pragma table_info(" + s_table + ")";
        } else {
            s_sql_command = "SELECT name,type,sql FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
            if (view_list == null) {
                view_list = new HashMap<String, String>();
            } else {
                view_list.clear();
            }
            i_database_type = 0;
            b_database_valid = false;
        }
        String s_type = "";
        String s_sql_create = "";
        String s_name = "";
        this_stmt = db_java.prepare(s_sql_command);
        try {
            while( this_stmt.step() ) {
                if (!s_table.equals("")) { // pragma table_info(berlin_strassen_geometry)
                    s_name = this_stmt.column_string(1);
                    s_type = this_stmt.column_string(2);
                    s_sql_create = this_stmt.column_string(5); // pk
                    // try to unify the data-types: varchar(??),int(11) mysql-syntax
                    if (s_type.indexOf("int(") != -1)
                        s_type = "INTEGER";
                    if (s_type.indexOf("varchar(") != -1)
                        s_type = "TEXT";
                    // pk: 0 || 1;Data-TypeTEXT || DOUBLE || INTEGER || REAL || DATE || BLOB ||
                    // geometry-types
                    fields_list.put(s_name, s_sql_create + ";" + s_type.toUpperCase());
                }
                if (s_table.equals("")) {
                    s_name = this_stmt.column_string(0);
                    s_type = this_stmt.column_string(1);
                    s_sql_create = this_stmt.column_string(2);
                    // GPLog.androidLog(-1,"SpatialiteDatabaseHandler.get_table_fields["+s_table+"] tablename["+s_name+"] type["+s_type+"] sql["
                    // + s_sql_create+ "] ");
                    if (s_type.equals("table")) {
                        if (s_name.equals("geometry_columns")) {
                            b_geometry_columns = true;
                        }
                        if (s_name.equals("geopackage_contents")) {
                            b_geopackage_contents = true;
                        }
                        if (s_name.equals("raster_columns")) {
                            b_raster_columns = true;
                        }
                    }
                    if (s_type.equals("view")) { // SELECT name,type,sql FROM sqlite_master WHERE
                                                 // (type='view')
                        // we are looking for user-defined views only, filter out system known
                        // views.
                        if ((!s_name.equals("geom_cols_ref_sys")) && (!s_name.startsWith("vector_layers"))) {
                            view_list.put(s_name, s_sql_create);
                        }
                        if (s_name.equals("vector_layers_statistics")) {
                            b_vector_layers_statistics = true;
                        }
                        if (s_name.equals("vector_layers")) {
                            b_vector_layers = true;
                        }
                    }
                }
            }
        } finally {
            if (this_stmt != null) {
                this_stmt.close();
            }
        }
        if (s_table.equals("")) {
            if (b_geopackage_contents) {
                // this is a GeoPackage, this can also have
                // vector_layers_statistics and vector_layers
                // - the results are empty, it does reference the table
                // also referenced in geopackage_contents
                i_database_type = 10;
                b_database_valid = true;
            } else {
                if ((b_vector_layers_statistics) && (b_vector_layers)) { // Spatialite 4.0
                    i_database_type = 4;
                    b_database_valid = true;
                } else {
                    if (b_geometry_columns) { // Spatialite before 4.0
                        i_database_type = 3;
                        b_database_valid = true;
                    }
                }
            }
        }
        // GPLog.androidLog(-1,"SpatialiteDatabaseHandler.get_table_fields["+s_table+"] ["+getFileNamePath()+"] valid["+b_database_valid+"] database_type["+i_database_type+"] sql["
        // + s_sql_command+ "] ");
        return fields_list;
    }
    // public String queryComuni() {
    // sb.append(SEP);
    // sb.append("Query Comuni...\n");
    //
    // String query = "SELECT " + NOME + //
    // " from " + COMUNITABLE + //
    // " order by " + NOME + ";";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db_java.prepare(query);
    // int index = 0;
    // while( stmt.step() ) {
    // String nomeStr = stmt.column_string(0);
    // sb.append("\t").append(nomeStr).append("\n");
    // if (index++ > 5) {
    // break;
    // }
    // }
    // sb.append("\t...");
    // stmt.close();
    // } catch (Exception e) {
    // error(e);
    // }
    //
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryComuniWithGeom() {
    // sb.append(SEP);
    // sb.append("Query Comuni with AsText(Geometry)...\n");
    //
    // String query = "SELECT " + NOME + //
    // " , " + AS_TEXT_GEOMETRY + //
    // " as geom from " + COMUNITABLE + //
    // " where geom not null;";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db_java.prepare(query);
    // while( stmt.step() ) {
    // String nomeStr = stmt.column_string(0);
    // String geomStr = stmt.column_string(1);
    // String substring = geomStr;
    // if (substring.length() > 40)
    // substring = geomStr.substring(0, 40);
    // sb.append("\t").append(nomeStr).append(" - ").append(substring).append("...\n");
    // break;
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryGeomTypeAndSrid() {
    // sb.append(SEP);
    // sb.append("Query Comuni geom type and srid...\n");
    //
    // String query = "SELECT " + NOME + //
    // " , " + AS_TEXT_GEOMETRY + //
    // " as geom from " + COMUNITABLE + //
    // " where geom not null;";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db_java.prepare(query);
    // while( stmt.step() ) {
    // String nomeStr = stmt.column_string(0);
    // String geomStr = stmt.column_string(1);
    // String substring = geomStr;
    // if (substring.length() > 40)
    // substring = geomStr.substring(0, 40);
    // sb.append("\t").append(nomeStr).append(" - ").append(substring).append("...\n");
    // break;
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryComuniArea() {
    // sb.append(SEP);
    // sb.append("Query Comuni area sum...\n");
    //
    // String query = "SELECT ST_Area(Geometry) / 1000000.0 from " + COMUNITABLE + //
    // ";";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db_java.prepare(query);
    // double totalArea = 0;
    // while( stmt.step() ) {
    // double area = stmt.column_double(0);
    // totalArea = totalArea + area;
    // }
    // sb.append("\tTotal area by summing each area: ").append(totalArea).append("Km2\n");
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // query = "SELECT sum(ST_Area(Geometry) / 1000000.0) from " + COMUNITABLE + //
    // ";";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db_java.prepare(query);
    // double totalArea = 0;
    // if (stmt.step()) {
    // double area = stmt.column_double(0);
    // totalArea = totalArea + area;
    // }
    // sb.append("\tTotal area by summing in query: ").append(totalArea).append("Km2\n");
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public String queryComuniNearby() {
    // sb.append(SEP);
    // sb.append("Query Comuni nearby...\n");
    //
    // String query =
    // "SELECT Hex(ST_AsBinary(ST_Buffer(Geometry, 1.0))), ST_Srid(Geometry), ST_GeometryType(Geometry) from "
    // + COMUNITABLE + //
    // " where " + NOME + "= 'Bolzano';";
    // sb.append("Execute query: ").append(query).append("\n");
    // String bufferGeom = "";
    // String bufferGeomShort = "";
    // try {
    // Stmt stmt = db_java.prepare(query);
    // if (stmt.step()) {
    // bufferGeom = stmt.column_string(0);
    // String geomSrid = stmt.column_string(1);
    // String geomType = stmt.column_string(2);
    // sb.append("\tThe selected geometry is of type: ").append(geomType).append(" and of SRID: ").append(geomSrid)
    // .append("\n");
    // }
    // bufferGeomShort = bufferGeom;
    // if (bufferGeom.length() > 10)
    // bufferGeomShort = bufferGeom.substring(0, 10) + "...";
    // sb.append("\tBolzano polygon buffer geometry in HEX: ").append(bufferGeomShort).append("\n");
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    //
    // query = "SELECT " + NOME + ", AsText(ST_centroid(Geometry)) from " + COMUNITABLE + //
    // " where ST_Intersects( ST_GeomFromWKB(x'" + bufferGeom + "') , Geometry );";
    // // just for print
    // String tmpQuery = "SELECT " + NOME + " from " + COMUNITABLE + //
    // " where ST_Intersects( ST_GeomFromWKB(x'" + bufferGeomShort + "') , Geometry );";
    // sb.append("Execute query: ").append(tmpQuery).append("\n");
    // try {
    // sb.append("\tComuni nearby Bolzano: \n");
    // Stmt stmt = db_java.prepare(query);
    // while( stmt.step() ) {
    // String name = stmt.column_string(0);
    // String wkt = stmt.column_string(1);
    // sb.append("\t\t").append(name).append(" - with centroid in ").append(wkt).append("\n");
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    // }
    //
    // public byte[] getBolzanoWKB() {
    // String query = "SELECT ST_AsBinary(ST_Transform(Geometry, 4326)) from " + COMUNITABLE + //
    // " where " + NOME + "= 'Bolzano';";
    // try {
    // Stmt stmt = db_java.prepare(query);
    // byte[] theGeom = null;
    // if (stmt.step()) {
    // theGeom = stmt.column_bytes(0);
    // }
    // stmt.close();
    // return theGeom;
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // return null;
    // }
    //
    // public List<byte[]> getIntersectingWKB( double n, double s, double e, double w ) {
    // List<byte[]> list = new ArrayList<byte[]>();
    // Coordinate ll = new Coordinate(w, s);
    // Coordinate ul = new Coordinate(w, n);
    // Coordinate ur = new Coordinate(e, n);
    // Coordinate lr = new Coordinate(e, s);
    // Polygon bboxPolygon = gf.createPolygon(new Coordinate[]{ll, ul, ur, lr, ll});
    //
    // byte[] bbox = wr.write(bboxPolygon);
    // String query = "SELECT ST_AsBinary(ST_Transform(Geometry, 4326)) from " + COMUNITABLE + //
    // " where ST_Intersects(ST_Transform(Geometry, 4326), ST_GeomFromWKB(?));";
    // try {
    // Stmt stmt = db_java.prepare(query);
    // stmt.bind(1, bbox);
    // while( stmt.step() ) {
    // list.add(stmt.column_bytes(0));
    // }
    // stmt.close();
    // return list;
    // } catch (Exception ex) {
    // ex.printStackTrace();
    // }
    // return null;
    // }
    //
    // public String doSimpleTransform() {
    //
    // sb.append(SEP);
    // sb.append("Coordinate transformation...\n");
    //
    // String query = "SELECT AsText(Transform(MakePoint(" + TEST_LON + ", " + TEST_LAT +
    // ", 4326), 32632));";
    // sb.append("Execute query: ").append(query).append("\n");
    // try {
    // Stmt stmt = db_java.prepare(query);
    // if (stmt.step()) {
    // String pointStr = stmt.column_string(0);
    // sb.append("\t").append(TEST_LON + "/" + TEST_LAT + "/EPSG:4326").append(" = ")//
    // .append(pointStr + "/EPSG:32632").append("...\n");
    // }
    // stmt.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // sb.append(ERROR).append(e.getLocalizedMessage()).append("\n");
    // }
    // sb.append("Done...\n");
    //
    // return sb.toString();
    //
    // }

}
