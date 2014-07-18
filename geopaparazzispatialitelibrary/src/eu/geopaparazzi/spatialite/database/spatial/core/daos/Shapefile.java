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

import android.content.Context;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteVersion;
import jsqlite.Database;
import jsqlite.Stmt;

/**
 * Created by hydrologis on 18/07/14.
 */
public class Shapefile {


    /**
     * Extension for shapefiles prjs.
     */
    public static final String PRJ_EXTENSION = ".prj"; //$NON-NLS-1$

    /**
     * Create geometry Table from Shape Table.
     * <p/>
     * <p>'RegisterVirtualGeometry' needs SpatiaLite 4.0.0
     *
     * @param sqlite_db    Database connection to use
     * @param s_table_path full path to Shape-Table [without .shp]
     * @param s_table_name Table name of Shape-Table [without path]
     * @param s_char_set   Characterset used in Shape [default 'CP1252', Windows Latin 1]
     * @param i_srid       srid of Shape-Table
     * @return i_rc 0 or last_error from Database
     */
    private static int createShapeTable(Database sqlite_db, String s_table_path, String s_table_name, String s_char_set,
                                        int i_srid) {
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
     * <p/>
     * <p>Shape-WKT rarely conforms to that used in 'spatial_ref_sys'
     *
     * @param sqlite_db         Database connection to use
     * @param s_srs_wkt         name of 'spatial_ref_sys' to search [dependent on spatilite version]
     * @param s_well_known_text read from the Shape .prj file
     * @return srid of  .prj file where possible
     */
    private static int readShapeSrid(Database sqlite_db, String s_srs_wkt, String s_well_known_text) {
        int i_srid = 0;
        if ((s_well_known_text.contains("GCS_WGS_1984")) && (s_well_known_text.contains("D_WGS_1984"))
                && (s_well_known_text.contains("Greenwich")) && (s_well_known_text.contains("Degree"))) {
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
     * <p/>
     * <p/>
     * - A Shape-File(s) resides in a directory<br>
     * - - the Directory name is the Database-Name<br>
     * - each Shape-Table must have a '.shp','.prj','.shx' and '.dbf'<br>
     * - the name with extention is the Table-Name<br>
     *
     * @param prjFile2ParentFolderMap File as found '.prj' files, File as directory
     */
    private static void createDbForShapefile(HashMap<File, File> prjFile2ParentFolderMap) {
        File shape_db = null;
        File shape_dir = null;
        Database sqlite_db = null;
        SpatialiteVersion spatialiteVersion = SpatialiteVersion.NO_SPATIALITE;
        String s_srs_wkt = "srs_wkt";
        String s_shape_path = "";
        String s_shape_name = "";
        for (Map.Entry<File, File> shape_list : prjFile2ParentFolderMap.entrySet()) {
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
                    sqlite_db = DatabaseCreationAndProperties.createDb(shape_db.getAbsolutePath());
                    spatialiteVersion = DatabaseCreationAndProperties.getSpatialiteDatabaseVersion(sqlite_db, "");
                } catch (Throwable t) {
                    GPLog.androidLog(4, "SpatialiteUtilities create_shape_db[" + shape_db.getAbsolutePath()
                            + "] spatialite_version[" + spatialiteVersion + "]", t);
                }
                if (spatialiteVersion.getCode() >= 3) { // created valid spatialite db
                    if (spatialiteVersion == SpatialiteVersion.AFTER_4_0_0_RC1) {
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
                    // TODO
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
     * <p/>
     * <p/>
     * - A Shape-File(s) resides in a directory<br>
     * - - the Directory name is the Database-Name<br>
     * - each Shape-Table must have a '.shp','.prj','.shx' and '.dbf'<br>
     * - the name with extension is the Table-Name<br>
     *
     * @param context 'this' of Application Activity class
     * @param mapsDir Directory to search [ResourcesManager.getInstance(this).getMapsDir();]
     * @return shapes_list: a {@link HashMap} that maps the prj file to the parent folder file.
     */
    public static HashMap<File, File> findShapefilePrjFiles(Context context, File mapsDir) {
        File[] list_files = mapsDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(PRJ_EXTENSION);
            }
        });
        // each shape file must have a prj file, we will read the prj file later
        HashMap<File, File> shapes_list = new HashMap<File, File>();
        File this_directoy = mapsDir;
        for (File this_file : list_files) {
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
}
