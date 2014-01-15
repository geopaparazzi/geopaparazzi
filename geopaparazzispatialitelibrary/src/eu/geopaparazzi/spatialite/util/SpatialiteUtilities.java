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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import android.content.Context;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryType;

/**
 * SpatialiteUtilities class.
 * Goal is to:
 * - determine which Spatialite Database version is being read
 * - create a new Spatialite Database
 * - convert a sqlite3 Database to a Spatialite Database
 * - convert older spatialite Database to present version
 * -- these spatialite function may not be accessible from sql
 * -->  SpatialiteUtilities.find_shapes(context, maps_dir);
 * @author Mark Johnson
 */
@SuppressWarnings("nls")
public class SpatialiteUtilities {
    private static final String PRJ_EXTENSION = ".prj"; //$NON-NLS-1$

    /**
     * From https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
     */
    public static final String METADATA_VECTOR_LAYERS_TABLE_NAME = " vector_layers";
    /**
     * From https://www.gaia-gis.it/fossil/libspatialite/wiki?name=metadata-4.0
     */
    public static final String METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME = " vector_layers_statistics";

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
    public static final String TEXTSIZE = "textsize";
    /**
     * 
     */
    public static final String TEXTFIELD = "textfield";
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
      * General Function to create jsqlite.Database with spatialite support.
      * <ol>
      * <li> parent diretories will be created, if needed</li>
      * <li> needed Tables/View and default values for metdata-table will be created</li>
      * </ol>
      * 
      * @param s_db_path name of Database file to create
      * @return sqlite_db: pointer to Database created
      * @throws IOException  if something goes wrong.
      */
    public static Database createDb( String s_db_path ) throws IOException {
        Database sqlite_db = null;
        File file_db = new File(s_db_path);
        if (!file_db.getParentFile().exists()) {
            File dir_db = file_db.getParentFile();
            if (!dir_db.mkdir()) {
                throw new IOException("SpatialiteUtilities: create_db: dir_db[" + dir_db.getAbsolutePath() + "] creation failed"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        sqlite_db = new jsqlite.Database();
        if (sqlite_db != null) {
            try {
                sqlite_db.open(file_db.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                        | jsqlite.Constants.SQLITE_OPEN_CREATE);
                int i_rc = createSpatialiteDb(sqlite_db, 0); // i_rc should be 4
            } catch (jsqlite.Exception e_stmt) {
                GPLog.androidLog(4, "SpatialiteUtilities: create_spatialite[spatialite] dir_file[" + file_db.getAbsolutePath() //$NON-NLS-1$
                        + "]", e_stmt); //$NON-NLS-1$
            }
        }
        return sqlite_db;
    }

    /**
      * General Function to create jsqlite.Database with spatialite support.
      * 
      * <ol> 
      * <li> parent diretories will be created, if needed</li>
      * <li> needed Tables/View and default values for metdata-table will be created</li>
      * </ol>
      * @param sqlite_db pointer to Database
      * @param i_parm 0=new Database - skip checking if it a patialite Database ; check Spatialite Version
      * @return i_rc: pointer to Database created
      * @throws Exception  if something goes wrong.
      */
    @SuppressWarnings("nls")
    public static int createSpatialiteDb( Database sqlite_db, int i_parm ) throws Exception {
        int i_rc = 0;
        if (i_parm == 1) {
            /*
             * 0=not a spatialite version ; 
             * 1=until 2.3.1 ; 
             * 2=until 2.4.0 ; 
             * 3=until 3.1.0-RC2 ;
             * 4=after 4.0.0-RC1
             */
            int i_spatialite_version = getSpatialiteVersion(sqlite_db, "");
            if (i_spatialite_version > 0) { // this is a spatialite Database, do not create
                i_rc = 1;
                if (i_spatialite_version < 3) { // TODO: logic for convertion to latest Spatialite
                                                // Version [open]
                    throw new Exception("Spatialite version < 3 not supported.");
                }
            }
        }
        if (i_rc == 0) {
            String s_sql_command = "SELECT InitSpatialMetadata();"; //$NON-NLS-1$
            try {
                sqlite_db.exec(s_sql_command, null);
            } catch (jsqlite.Exception e_stmt) {
                i_rc = sqlite_db.last_error();
                GPLog.androidLog(4, "SpatialiteUtilities: create_spatialite sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt); //$NON-NLS-1$ //$NON-NLS-2$
            }
            // GPLog.androidLog(2,
            // "SpatialiteUtilities: create_spatialite sql["+s_sql_command+"] rc="+i_rc+"]");
            i_rc = getSpatialiteVersion(sqlite_db, ""); //$NON-NLS-1$
            if (i_rc < 3) { // error, should be 3 or 4
                GPLog.androidLog(4, "SpatialiteUtilities: create_spatialite spatialite_version[" + i_rc + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        return i_rc;
    }

    /**
      * Determine the Spatialite version of the Database being used
      * 
      * <ul> 
      * <li> - if (sqlite3_exec(this_handle_sqlite3,"SELECT InitSpatialMetadata()",NULL,NULL,NULL) == SQLITE_OK)
      * <li>  - 'geometry_columns'
      * <li>-- SpatiaLite 2.0 until present version
      * <li>- 'spatial_ref_sys'
      * <li>-- SpatiaLite 2.0 until present version
      * <li>-- SpatiaLite 2.3.1 has no field 'srs_wkt' or 'srtext' field,only 'proj4text' and
      * <li>-- SpatiaLite 2.4.0 first version with 'srs_wkt' and 'views_geometry_columns'
      * <li>-- SpatiaLite 3.1.0-RC2 last version with 'srs_wkt'
      * <li>-- SpatiaLite 4.0.0-RC1 : based on ISO SQL/MM standard 'srtext'
      * <li>-- views: vector_layers_statistics,vector_layers
      * <li>-- SpatiaLite 4.0.0 : introduced
      * </ul>
      * 
      * <p>20131129: at the moment not possible to distinguish beteewn 2.4.0 and 3.0.0 [no '2']
      * 
      * @param sqlite_db Database connection to use
      * @param s_table name of table to read [if empty: list of tables in Database]
      * @return i_spatialite_version [0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0 ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1]
      */
    @SuppressWarnings("nls")
    private static int getSpatialiteVersion( Database sqlite_db, String s_table ) throws Exception {
        Stmt this_stmt = null;
        // views: vector_layers_statistics,vector_layers
        boolean b_vector_layers_statistics = false;
        boolean b_vector_layers = false;
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
        boolean b_views_geometry_columns = false;
        int i_spatialite_version = 0; // 0=not a spatialite version ; 1=until 2.3.1 ; 2=until 2.4.0
                                      // ; 3=until 3.1.0-RC2 ; 4=after 4.0.0-RC1
        String s_sql_command = "";
        if (!s_table.equals("")) { // pragma table_info(geodb_geometry)
            s_sql_command = "pragma table_info(" + s_table + ")";
        } else {
            s_sql_command = "SELECT name,type FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
        }
        String s_type = "";
        String s_name = "";
        this_stmt = sqlite_db.prepare(s_sql_command);
        try {
            while( this_stmt.step() ) {
                if (!s_table.equals("")) { // pragma table_info(berlin_strassen_geometry)
                    s_name = this_stmt.column_string(1);
                    // 'proj4text' must always exist - otherwise invalid
                    if (s_name.equals("proj4text"))
                        b_spatial_ref_sys = true;
                    if (s_name.equals("srs_wkt"))
                        i_srs_wkt = 1;
                    if (s_name.equals("srtext"))
                        i_srs_wkt = 2;
                }
                if (s_table.equals("")) {
                    s_name = this_stmt.column_string(0);
                    s_type = this_stmt.column_string(1);
                    if (s_type.equals("table")) {
                        if (s_name.equals("geometry_columns")) {
                            b_geometry_columns = true;
                        }
                        if (s_name.equals("spatial_ref_sys")) {
                            b_spatial_ref_sys = true;
                        }
                        if (s_name.equals("views_geometry_columns")) {
                            b_views_geometry_columns = true;
                        }
                    }
                    if (s_type.equals("view")) {
                        // SELECT name,type,sql FROM sqlite_master WHERE
                        // (type='view')
                        if (s_name.equals("vector_layers_statistics")) {
                            // An empty spatialite
                            // Database will not have
                            // this
                            b_vector_layers_statistics = true;
                        }
                        if (s_name.equals("vector_layers")) {
                            // An empty spatialite Database will
                            // not have this
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
            GPLog.androidLog(-1, "SpatialiteUtilities: get_table_fields sql[" + s_sql_command + "] geometry_columns["
                    + b_geometry_columns + "] spatial_ref_sys[" + b_spatial_ref_sys + "]");
            if ((b_geometry_columns) && (b_spatial_ref_sys)) {
                if (b_spatial_ref_sys) {
                    i_srs_wkt = getSpatialiteVersion(sqlite_db, "spatial_ref_sys");
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
                    i_spatialite_version = 1; // no 'srs_wkt' or 'srtext' fields
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
      * Create geometry Table from Shape Table.
      * 
      * <p>'RegisterVirtualGeometry' needs SpatiaLite 4.0.0
      * 
      * @param sqlite_db Database connection to use
      * @param s_table_path full path to Shape-Table [without .shp]
      * @param s_table_name Table name of Shape-Table [without path]
      * @param s_char_set Characterset used in Shape [default 'CP1252', Windows Latin 1]
      * @param i_srid srid of Shape-Table
      * @return i_rc 0 or last_error from Database
      */
    @SuppressWarnings("nls")
    private static int createShapeTable( Database sqlite_db, String s_table_path, String s_table_name, String s_char_set,
            int i_srid ) {
        int i_rc = 0;
        Stmt this_stmt = null;
        int i_geometry_type = 0;
        if (s_char_set.equals(""))
            s_char_set = "CP1252";
        String s_table_name_work = s_table_name + "_work";
        // GPLog.androidLog(-1,"SpatialiteUtilities create_shape_table[" + s_table_name +
        // "] srid["+i_srid+"] ["+s_table_path+"]");
        // CREATE VIRTUAL TABLE roads using virtualshape('/sdcard/maps/roads',CP1252,3857);
        String s_sql_command = "CREATE VIRTUAL TABLE " + s_table_name_work + " USING VirtualShape('" + s_table_path + "',"
                + s_char_set + "," + i_srid + ");";
        // GPLog.androidLog(-1,"SpatialiteUtilities create_shape_table[" + s_sql_command+"]");
        try {
            sqlite_db.exec(s_sql_command, null);
            // SELECT RegisterVirtualGeometry('roads');
            s_sql_command = "SELECT RegisterVirtualGeometry('" + s_table_name_work + "');";
            sqlite_db.exec(s_sql_command, null);
            // CREATE TABLE myroads AS SELECT * FROM roads;
            s_sql_command = "CREATE TABLE " + s_table_name + " AS SELECT * FROM " + s_table_name_work + ";";
            sqlite_db.exec(s_sql_command, null);
            s_sql_command = "SELECT geometry_type FROM vector_layers WHERE table_name='" + s_table_name + "'";
            this_stmt = sqlite_db.prepare(s_sql_command);
            try {
                if (this_stmt.step()) {
                    i_geometry_type = this_stmt.column_int(0);
                }
            } catch (jsqlite.Exception e_stmt) {
                i_rc = sqlite_db.last_error();
                GPLog.androidLog(4, "SpatialiteUtilities: create_shape_table sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
            }
            GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
            String s_geometry_type = geometry_type.toString();
            // SELECT RecoverGeometryColumn('myroads','Geometry',3857,'LINESTRING')
            s_sql_command = "SELECT RecoverGeometryColumn('" + s_table_name + "', 'Geometry'," + i_srid + ",'" + s_geometry_type
                    + "');";
            sqlite_db.exec(s_sql_command, null);
            // SELECT CreateSpatialIndex('myroads','Geometry');
            s_sql_command = "SELECT CreateSpatialIndex('" + s_table_name + "', 'Geometry');";
            sqlite_db.exec(s_sql_command, null);
            // SELECT DropVirtualGeometry('berlin_ortsteile_2010_work');
            s_sql_command = "SELECT DropVirtualGeometry('" + s_table_name_work + "');";
            sqlite_db.exec(s_sql_command, null);
        } catch (jsqlite.Exception e_stmt) {
            i_rc = sqlite_db.last_error();
            GPLog.androidLog(4, "SpatialiteUtilities: create_shape_table sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt);
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Attempt to determine srid from Shape .prj file
      * 
      * <p>Shape-WKT rarely conforms to that used in 'spatial_ref_sys'
      * 
      * @param sqlite_db Database connection to use
      * @param s_srs_wkt name of 'spatial_ref_sys' to search [dependent on spatilite version]
      * @param s_well_known_text read from the Shape .prj file
      * @return srid of  .prj file where possible
      */
    @SuppressWarnings("nls")
    private static int readShapeSrid( Database sqlite_db, String s_srs_wkt, String s_well_known_text ) {
        int i_srid = 0;
        if ((s_well_known_text.indexOf("GCS_WGS_1984") != -1) && (s_well_known_text.indexOf("D_WGS_1984") != -1)
                && (s_well_known_text.indexOf("Greenwich") != -1) && (s_well_known_text.indexOf("Degree") != -1)) {
            /*
             * GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",
             * SPHEROID["WGS_1984",6378137.0,298.257223563]],
             * PRIMEM["Greenwich",0.0],
             * UNIT["Degree",0.017453292519943295]]
             */
            i_srid = 4326;
        }
        if ((s_well_known_text.indexOf("GCS_DHDN") != -1) && (s_well_known_text.indexOf("D_Deutsches_Hauptdreiecksnetz") != -1)
                && (s_well_known_text.indexOf("Bessel_1841") != -1) && (s_well_known_text.indexOf("Greenwich") != -1)
                && (s_well_known_text.indexOf("Degree") != -1)) {
            /* 
             * PROJCS["Cassini",GEOGCS["GCS_DHDN",
             * DATUM["D_Deutsches_Hauptdreiecksnetz",
             * SPHEROID["Bessel_1841",6377397.155,299.1528128]],
             * PRIMEM["Greenwich",0],
             * UNIT["Degree",0.017453292519943295]]
             */
            if ((s_well_known_text.indexOf("Cassini") != -1) && (s_well_known_text.indexOf("52.4186482") != -1)
                    && (s_well_known_text.indexOf("13.62720") != -1)) {
                /*
                 * ,PROJECTION["Cassini"],
                 * PARAMETER["latitude_of_origin",52.41864827777778],
                 * PARAMETER["central_meridian",13.62720366666667],
                 */
                if ((s_well_known_text.indexOf("40000") != -1) && (s_well_known_text.indexOf("10000") != -1)) { // PARAMETER["false_easting",40000],PARAMETER["false_northing",10000],UNIT["Meter",1],PARAMETER["scale_factor",1.0]]
                    i_srid = 3068;
                }
            }
        }
        if (i_srid == 0) {
            /* 
             * TODO: do a lot of guessing
             * PROJCS["DHDN / Soldner Berlin",
             * GEOGCS["DHDN",DATUM["Deutsches_Hauptdreiecksnetz",
             * SPHEROID["Bessel 1841",6377397.155,299.1528128,
             * AUTHORITY["EPSG","7004"]],AUTHORITY["EPSG","6314"]],
             * PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],
             * UNIT["degree",0.01745329251994328,
             * AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4314"]],
             * UNIT["metre",1,AUTHORITY["EPSG","9001"]],
             * PROJECTION["Cassini_Soldner"],
             * PARAMETER["latitude_of_origin",52.41864827777778],
             * PARAMETER["central_meridian",13.62720366666667],
             * PARAMETER["false_easting",40000],
             * PARAMETER["false_northing",10000],
             * AUTHORITY["EPSG","3068"],AXIS["x",NORTH],AXIS["y",EAST]]
             * SELECT srid FROM spatial_ref_sys WHERE (srs_wkt LIKE
             * 'GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",
             * SPHEROID["WGS_1984",6378137.0,298.257223563]],
             * PRIMEM["Greenwich",0.0],UNIT["Degree",0.017453292519943295]]')
             */
        }
        return i_srid;
    }

    /**
      * General Function to search for Shape files
      * 
      * <p>
      * - A Shape-File(s) resides in a directory<br>
      * - - the Directory name is the Database-Name<br>
      * - each Shape-Table must have a '.shp','.prj','.shx' and '.dbf'<br>
      * - the name with extention is the Table-Name<br>
      * 
      * @param prjFile2ParentFolderMap File as found '.prj' files, File as directory
      */
    @SuppressWarnings("nls")
    private static void createDbForShapefile( HashMap<File, File> prjFile2ParentFolderMap ) {
        File shape_db = null;
        File shape_dir = null;
        Database sqlite_db = null;
        int i_spatialite_version = 0;
        String s_srs_wkt = "srs_wkt";
        String s_shape_path = "";
        String s_shape_name = "";
        for( Map.Entry<File, File> shape_list : prjFile2ParentFolderMap.entrySet() ) {
            File file_prj = shape_list.getKey();
            File file_directory = shape_list.getValue();
            if (sqlite_db == null) {
                shape_dir = file_directory;
                s_shape_path = shape_dir.getParentFile().getAbsolutePath();
                s_shape_name = shape_dir.getName(); // .substring(0,
                                                    // shape_dir.getName().lastIndexOf("."));
                shape_db = new File(s_shape_path + File.separator + s_shape_name + ".db");
                // GPLog.androidLog(-1,"SpatialiteUtilities create_shape_db[" +
                // shape_db.getAbsolutePath() +
                // "] shape_name["+s_shape_name+"] db.exists["+shape_db.exists()+"]");
                if (shape_db.exists()) { // A database exist - abort
                    return;
                }
                try {
                    sqlite_db = createDb(shape_db.getAbsolutePath());
                    i_spatialite_version = getSpatialiteVersion(sqlite_db, "");
                } catch (Throwable t) {
                    GPLog.androidLog(4, "SpatialiteUtilities create_shape_db[" + shape_db.getAbsolutePath()
                            + "] spatialite_version[" + i_spatialite_version + "]", t);
                }
                if (i_spatialite_version >= 3) { // created valid spatialite db
                    if (i_spatialite_version == 4) {
                        s_srs_wkt = "srtext";
                    }
                }
            }
            if (sqlite_db != null) {
                String s_table_name = file_prj.getName().substring(0, file_prj.getName().lastIndexOf("."));
                String s_well_known_text = "";
                try { // GEOGCS["GCS_WGS_1984",DATUM["D_WGS_1984",SPHEROID["WGS_1984",6378137.0,298.257223563]],PRIMEM["Greenwich",0.0],UNIT["Degree",0.017453292519943295]]
                    s_well_known_text = FileUtilities.readfile(file_prj);
                    int i_srid = readShapeSrid(sqlite_db, s_srs_wkt, s_well_known_text);
                    String s_char_set = "CP1252";
                    if (i_srid > 0) {
                        String s_table_path = s_shape_path + File.separator + s_shape_name + File.separator + s_table_name;
                        int i_rc = createShapeTable(sqlite_db, s_table_path, s_table_name, s_char_set, i_srid);
                        // GPLog.androidLog(-1,"SpatialiteUtilities create_shape_db[" + s_table_name
                        // + "] srid["+i_srid+"]");
                    }
                } catch (IOException e) {
                }
            }
        }
        if (sqlite_db != null) {
            try {
                sqlite_db.close();
            } catch (jsqlite.Exception e_stmt) {
                GPLog.androidLog(4, "SpatialiteUtilities: create_shape_db: close() : failed", e_stmt);
            }
            sqlite_db = null;
        }
    }

    /**
      * Collects a {@link HashMap} of prj files of shapefiles.
      * 
      * <p>
      * - A Shape-File(s) resides in a directory<br>
      * - - the Directory name is the Database-Name<br>
      * - each Shape-Table must have a '.shp','.prj','.shx' and '.dbf'<br>
      * - the name with extension is the Table-Name<br>
      * 
      * @param context 'this' of Application Activity class
      * @param mapsDir Directory to search [ResourcesManager.getInstance(this).getMapsDir();]
      * @return shapes_list: a {@link HashMap} that maps the prj file to the parent folder file.
      */
    public static HashMap<File, File> findShapefilePrjFiles( Context context, File mapsDir ) {
        File[] list_files = mapsDir.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String filename ) {
                return filename.endsWith(PRJ_EXTENSION);
            }
        });
        // each shape file must have a prj file, we will read the prj file later
        HashMap<File, File> shapes_list = new HashMap<File, File>();
        File this_directoy = mapsDir;
        for( File this_file : list_files ) {
            if (this_file.isDirectory()) {
                // read recursive directories inside the sdcard/maps directory
                shapes_list = findShapefilePrjFiles(context, this_file);
                if (shapes_list.size() > 0) {
                    // shape file Directory has been found: do something
                    // with it
                    // GPLog.androidLog(-1,"SpatialiteUtilities find_shapes["
                    // + this_file.getAbsolutePath() + "] shapes[" +
                    // shapes_list.size() + "]");
                    createDbForShapefile(shapes_list);
                }
            } else {
                // store each prj file and the directory found
                shapes_list.put(this_file, this_directoy);
            }
        }
        // GPLog.androidLog(-1,"SpatialiteUtilities find_shapes[" + mapsDir.getName() +
        // "] size["+shapes_list+"]");
        return shapes_list;
    }

    /**
     * Build a query to retrieve geometries from a table in a given bound.
     * 
     * @param destSrid the destination srid.
     * @param table the table to use.
     * @param n north bound.
     * @param s south bound.
     * @param e east bound.
     * @param w west bound.
     * @return the query.
     */
    public static String buildGeometriesInBoundsQuery( String destSrid, SpatialVectorTable table, double n, double s, double e,
            double w ) {
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
}
