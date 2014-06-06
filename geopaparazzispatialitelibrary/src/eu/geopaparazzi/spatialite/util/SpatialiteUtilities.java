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
import jsqlite.Stmt;
import android.content.Context;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryType;

/**
 * SpatialiteUtilities class.

 * @author Mark Johnson
 */
@SuppressWarnings("nls")
public class SpatialiteUtilities {
    /**
     * Name/path separator for spatialite table names.
     */
    public static final String UNIQUENAME_SEPARATOR = "#"; //$NON-NLS-1$

    /**
     * Extension for shapefiles prjs.
     */
    public static final String PRJ_EXTENSION = ".prj"; //$NON-NLS-1$

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
                    sqlite_db = DaoSpatialite.createDb(shape_db.getAbsolutePath());
                    i_spatialite_version = DaoSpatialite.getSpatialiteDatabaseVersion(sqlite_db, "");
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
        if (table.getStyle().labelvisible == 1) {
            qSb.append(",");
            qSb.append(table.getStyle().labelfield);
        }
        qSb.append(" FROM ");
        qSb.append(table.getTableName());
        // the SpatialIndex would be searching for a square, the ST_Intersects the Geometry
        // the SpatialIndex could be fulfilled, but checking the Geometry could return the result
        // that it is not
        qSb.append(" WHERE ST_Intersects(");
        qSb.append(table.getGeomName());
        qSb.append(", ");
        qSb.append(mbr);
        qSb.append(") = 1 AND ");
        qSb.append(table.getROWID());
        qSb.append("  IN (SELECT ");
        qSb.append(table.getROWID());
        qSb.append(" FROM Spatialindex WHERE f_table_name ='");
        qSb.append(table.getTableName());
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
     * Collects bounds and center as wgs84 4326.
     * - Note: use of getEnvelopeInternal() insures that, after transformation,
     * -- possible false values are given - since the transformed result might not be square
     * @param srid the source srid.
     * @param centerCoordinate the coordinate array to fill with the center.
     * @param boundsCoordinates the coordinate array to fill with the bounds as [w,s,e,n].
    */
    public static void collectBoundsAndCenter( Database sqlite_db, String srid, double[] centerCoordinate, double[] boundsCoordinates ) {
        String centerQuery = "";
        try {
            Stmt centerStmt = null;
            double bounds_west = boundsCoordinates[0];
            double bounds_south = boundsCoordinates[1];
            double bounds_east = boundsCoordinates[2];
            double bounds_north = boundsCoordinates[3];
            /*
            SELECT ST_Transform(BuildMBR(14121.000000,187578.000000,467141.000000,48006927.000000,23030),4326);
             SRID=4326;POLYGON((
             -7.364919057793379 1.69098037889473,
             -3.296335497384673 1.695910088657131,
             -131.5972302288043 89.99882674963366,
             -131.5972302288043 89.99882674963366,
             -7.364919057793379 1.69098037889473))
            SELECT MbrMaxX(ST_Transform(BuildMBR(14121.000000,187578.000000,467141.000000,48006927.000000,23030),4326));
            -3.296335
            */
            try {
                WKBReader wkbReader = new WKBReader();
                StringBuilder centerBuilder = new StringBuilder();
                centerBuilder.append("SELECT ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                // centerBuilder.append("select AsText(ST_Transform(MakePoint(");
                centerBuilder.append("(" + bounds_west + " + (" + bounds_east + " - " + bounds_west + ")/2), ");
                centerBuilder.append("(" + bounds_south + " + (" + bounds_north + " - " + bounds_south + ")/2), ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS Center,");
                centerBuilder.append("ST_AsBinary(CastToXY(ST_Transform(BuildMBR(");
                centerBuilder.append("" + bounds_west + "," + bounds_south + ", ");
                centerBuilder.append("" + bounds_east + "," + bounds_north + ", ");
                centerBuilder.append(srid);
                centerBuilder.append("),4326))) AS Envelope ");
                // centerBuilder.append("';");
                centerQuery = centerBuilder.toString();
                // GPLog.androidLog(-1, "SpatialiteUtilities.collectBoundsAndCenter Bounds[" + centerQuery + "]");
                centerStmt = sqlite_db.prepare(centerQuery);
                if (centerStmt.step()) {
                    byte[] geomBytes = centerStmt.column_bytes(0);
                    Geometry geometry = wkbReader.read(geomBytes);
                    Coordinate coordinate = geometry.getCoordinate();
                    centerCoordinate[0] = coordinate.x;
                    centerCoordinate[1] = coordinate.y;
                    geomBytes = centerStmt.column_bytes(1);
                    geometry = wkbReader.read(geomBytes);
                    Envelope envelope = geometry.getEnvelopeInternal();
                    boundsCoordinates[0] = envelope.getMinX();
                    boundsCoordinates[1] = envelope.getMinY();
                    boundsCoordinates[2] = envelope.getMaxX();
                    boundsCoordinates[3] = envelope.getMaxY();
                }
            } catch (java.lang.Exception e) {
                GPLog.androidLog(4, "SpatialiteUtilities.collectBoundsAndCenter Bounds[" + centerQuery + "]", e);
            } finally {
                if (centerStmt != null)
                    centerStmt.close();
            }
        } catch (java.lang.Exception e) {
            GPLog.androidLog(4, "SpatialiteUtilities[" + sqlite_db.getFilename() + "] sql[" + centerQuery + "]", e);
        }
    }

    /**
     * Retrieve rasterlite2 tile of a given bound [4326,wsg84] with the given size.
     *
     * https://github.com/geopaparazzi/Spatialite-Tasks-with-Sql-Scripts/wiki/RL2_GetMapImage
     * @param sqlite_db Database connection to use
     * @param destSrid the destination srid (of the rasterlite2 image).
     * @param table (coverageName) the table to use.
     * @param tileBounds [west,south,east,north] [minx, miny, maxx, maxy] bounds.
     * @param i_tile_size default 256 [Tile.TILE_SIZE].
     * @return the image data as byte[] as jpeg
     */
    public static byte[] rl2_GetMapImageTile( Database sqlite_db,String destSrid, String coverageName,double[] tileBounds,int i_tile_size ) {
        return DaoSpatialite.rl2_GetMapImage(sqlite_db,"4326",destSrid,coverageName,i_tile_size,i_tile_size,tileBounds,"default","image/jpeg","#ffffff",0,80,1 );
    }

}
