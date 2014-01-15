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

import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.DASH;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.DECIMATION;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.ENABLED;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.FILLALPHA;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.FILLCOLOR;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.MAXZOOM;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.METADATA_VECTOR_LAYERS_TABLE_NAME;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.MINZOOM;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.NAME;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.ORDER;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.PROPERTIESTABLE;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.SHAPE;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.SIZE;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.STROKEALPHA;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.STROKECOLOR;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.TEXTFIELD;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.TEXTSIZE;
import static eu.geopaparazzi.spatialite.util.SpatialiteUtilities.WIDTH;

import java.io.File;
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
import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteContextHolder;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryType;
import eu.geopaparazzi.spatialite.util.OrderComparator;
import eu.geopaparazzi.spatialite.util.SpatialiteUtilities;
import eu.geopaparazzi.spatialite.util.Style;

/**
 * An utility class to handle the spatial database.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SpatialiteDatabaseHandler implements ISpatialDatabaseHandler {

    private static final int i_style_column_count = 15 + 1; // 0-15
    private String uniqueDbName4DataProperties = "";

    private Database db_java;

    private HashMap<String, Paint> fillPaints = new HashMap<String, Paint>();
    private HashMap<String, Paint> strokePaints = new HashMap<String, Paint>();

    private List<SpatialVectorTable> vectorTableList;
    private List<SpatialRasterTable> rasterTableList;
    // all DatabaseHandler/Table classes should use these names
    private File dbFile;
    // [with path] all DatabaseHandler/Table classes should use these
    // names
    private String databasePath;
    // [without path] all DatabaseHandler/Table classes should use
    // these
    // names
    private String databaseFileName;
    // all DatabaseHandler/Table classes should use these
    // names
    private String databaseFileNameNoExt;

    // will be set in class_bounds - values of all Rasters-Datasets
    private int minZoom = 0;
    private int maxZoom = 0;
    // will be set in class_bounds - values of all V-Datactorsets
    private double centerX = 0.0; // wsg84
    private double centerY = 0.0; // wsg84
    private double boundsWest = 0.0; // wsg84
    private double boundsEast = 0.0; // wsg84
    private double boundsNorth = 0.0; // wsg84
    private double boundsSouth = 0.0; // wsg84
    // -----------------------------------------------------
    private int defaultZoom;
    private boolean isDatabaseValid = true;

    /**
     * The database type.
     * 
     * <ul>
     * <li>10 == GeoPackage
     * <li>3  == spatialite 3
     * <li>4  == spatialite 4
     * </ul>
     */
    private int databaseType = 0;

    // List of all View of Database [name,sql_create] - search sql for geometry columns
    private HashMap<String, String> view_list;

    /**
     * Contructor.
     * 
     * @param dbPath the path to the database this handler connects to.
     */
    public SpatialiteDatabaseHandler( String dbPath ) {
        try {
            dbFile = new File(dbPath);
            if (!dbFile.getParentFile().exists()) {
                throw new RuntimeException();
            }
            databasePath = dbFile.getAbsolutePath();
            databaseFileName = dbFile.getName();
            databaseFileNameNoExt = dbFile.getName().substring(0, dbFile.getName().lastIndexOf("."));
            try {
                Context context = SpatialiteContextHolder.INSTANCE.getContext();
                ResourcesManager resourcesManager = ResourcesManager.getInstance(context);
                File mapsDir = resourcesManager.getMapsDir();
                String mapsPath = mapsDir.getAbsolutePath();
                if (databasePath.startsWith(mapsPath)) {
                    // this should always be true
                    String relativePath = databasePath.substring(mapsPath.length());
                    StringBuilder sb = new StringBuilder();
                    if (relativePath.startsWith(File.separator)) {
                        relativePath = relativePath.substring(1);
                    }
                    sb.append(relativePath);
                    sb.append(File.separator);
                    uniqueDbName4DataProperties = sb.toString();
                }
            } catch (java.lang.Exception e) {
                GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + dbFile.getAbsolutePath() + "]", e);
            }
            db_java = new jsqlite.Database();
            db_java.open(databasePath, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);
            /*
             * 0=check if valid only ; 
             * 1=check if valid and fill vectorTableList,rasterTableList
             */
            checkAndCollectTables(0);
            if (!isValid()) {
                close();
            }
        } catch (Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + dbFile.getAbsolutePath() + "]", e);
        }
        // if (isValid()) {
        // setDescription(databaseFileNameNoExt);
        // GPLog.androidLog(-1,"SpatialiteDatabaseHandler["+s_name+"]["+getJavaSqliteDescription()+"]");
        // }
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
        return isDatabaseValid;
    }

    @Override
    public String getDatabasePath() {
        return this.databasePath;
    }

    public String getFileName() {
        return databaseFileName;
    }

    public String getName() {
        return this.databaseFileNameNoExt;
    }

    public String getBoundsAsString() {
        return boundsWest + "," + boundsSouth + "," + boundsEast + "," + boundsNorth;
    }

    public String getCenterAsString() {
        return centerX + "," + centerY + "," + defaultZoom;
    }

    public String getMinMaxZoomLevelsAsString() {
        return getMinZoom() + "-" + getMaxZoom();
    }

    public File getFile() {
        return this.dbFile;
    }

    public int getMinZoom() {
        return minZoom;
    }

    public int getMaxZoom() {
        return maxZoom;
    }

    public double getMinLongitude() {
        return boundsWest;
    }

    public double getMinLatitude() {
        return boundsSouth;
    }

    public double getMaxLongitude() {
        return boundsEast;
    }

    public double getMaxLatitude() {
        return boundsNorth;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public int getDefaultZoom() {
        return defaultZoom;
    }

    public void setDefaultZoom( int defaultZoom ) {
        this.defaultZoom = defaultZoom;
    }

    /**
      * Return info of supported versions in JavaSqlite.
      * 
      * <br>- JavaSqlite
      * <br>- Spatialite
      * <br>- Proj4
      * <br>- Geos
      * <br>-- there is no Spatialite function to retrieve the Sqlite version
      * <br>-- the Has() functions to not eork with spatialite 3.0.1
      * 
      * @return info of supported versions in JavaSqlite.
      */
    public String getJavaSqliteDescription() {
        String s_javasqlite_description = "";
        try {
            s_javasqlite_description = "javasqlite[" + getJavaSqliteVersion() + "],";
            s_javasqlite_description += "spatialite[" + getSpatialiteVersion() + "],";
            s_javasqlite_description += "proj4[" + getProj4Version() + "],";
            s_javasqlite_description += "geos[" + getGeosVersion() + "],";
            s_javasqlite_description += "spatialite_properties[" + getSpatialiteProperties() + "]]";
        } catch (Exception e) {
            s_javasqlite_description += "exception[? not a spatialite database, or spatialite < 4 ?]]";
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + databaseFileNameNoExt + "].getJavaSqliteDescription["
                    + s_javasqlite_description + "]", e);
        }
        return s_javasqlite_description;
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
    // -----------------------------------------------
    /**
     * Get the version of Spatialite.
     *
     * @return the version of Spatialite.
     * @throws Exception  if something goes wrong.
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
     * 
     * - use the known 'SELECT Has..' functions
     * - when HasIconv=0: no VirtualShapes,VirtualXL
     * 
     * @return the properties of Spatialite.
     * @throws Exception  if something goes wrong.
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
     * @throws Exception  if something goes wrong.
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
     * @throws Exception  if something goes wrong.
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
            // 0=check if valid only ; 1=check if valid and fill
            // vectorTableList,rasterTableList
            checkAndCollectTables(1);
        }
        return vectorTableList;
    }

    /**
     * Collects bounds and center as wgs84 4326.
     *
     * @param srid the source srid.
     * @param centerCoordinate the coordinate array to fill with the center.
     * @param boundsCoordinates the coordinate array to fill with the bounds as  [w,s,e,n].
     */
    private void collectBoundsAndCenter( String srid, double[] centerCoordinate, double[] boundsCoordinates ) {
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
                    // South_West
                    boundsCoordinates[0] = coordinate.x;
                    boundsCoordinates[1] = coordinate.y;
                    geomBytes = centerStmt.column_bytes(2);
                    geometry = wkbReader.read(geomBytes);
                    coordinate = geometry.getCoordinate();
                    // North_East
                    boundsCoordinates[2] = coordinate.x;
                    boundsCoordinates[3] = coordinate.y;
                }
            } catch (java.lang.Exception e) {
                GPLog.androidLog(4, "SpatialiteDatabaseHandler.collectBoundsAndCenter Bounds[" + centerQuery + "]", e);
            } finally {
                if (centerStmt != null)
                    centerStmt.close();
            }
        } catch (java.lang.Exception e) {
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + dbFile.getAbsolutePath() + "] sql[" + centerQuery + "]", e);
        }
    }

    @Override
    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        if (rasterTableList == null || forceRead) {
            rasterTableList = new ArrayList<SpatialRasterTable>();
            // 0=check if valid only ; 1=check if valid and fill
            // vectorTableList,rasterTableList
            checkAndCollectTables(1);
        }
        return rasterTableList;
    }

    /**
     * Check availability of style for the tables.
     *
     * @throws Exception
     */
    private void checkPropertiesTable() throws Exception {
        String checkTableQuery = "SELECT name,sql FROM sqlite_master WHERE type='table' AND name='" + PROPERTIESTABLE + "';";
        Stmt stmt = db_java.prepare(checkTableQuery);
        String s_create_sql = "";
        boolean tableExists = false;
        int i_column_count = 0;
        try {
            if (stmt.step()) {
                String name = stmt.column_string(0);
                if (name != null) {
                    tableExists = true;
                    s_create_sql = stmt.column_string(1);
                    i_column_count = (s_create_sql.length() - s_create_sql.replace(",", "").length()) + 1;
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
        } else {
            // i_column_count
            // SELECT count(name) FROM dataproperties WHERE name LIKE 'aurina/aurina.sqlite/%'
            int i_record_count = 0;
            // we are assumining that if the table exists there is a least 1
            // record
            checkTableQuery = "SELECT count(name)  FROM " + PROPERTIESTABLE + " WHERE name LIKE '" + uniqueDbName4DataProperties
                    + "%';";
            stmt = db_java.prepare(checkTableQuery);
            try {
                if (stmt.step()) {
                    i_record_count = stmt.column_int(0);
                }
            } finally {
                stmt.close();
            }
            if (i_record_count < 1) {
                for( SpatialVectorTable spatialTable : vectorTableList ) {
                    String s_unique_name_base = spatialTable.getUniqueNameBase();
                    checkTableQuery = "UPDATE " + PROPERTIESTABLE + "  SET  name  = '" + uniqueDbName4DataProperties
                            + s_unique_name_base + "' WHERE name LIKE '%" + s_unique_name_base + "';";
                    // GPLog.androidLog(-1, "SpatialiteDatabaseHandler[" +
                    // file_map.getAbsolutePath() + "] col_count["+i_column_count+"] sql[" +
                    // checkTableQuery + "]");
                    db_java.exec(checkTableQuery, null);
                }
            }
            if (i_column_count != i_style_column_count) {
                // structure of table has changed - we
                // don't know from what version this is
                s_create_sql = s_create_sql.replace("CREATE TABLE " + PROPERTIESTABLE + " (", "");
                s_create_sql = s_create_sql.replace(")", "");
                String[] sa_split = s_create_sql.split(",");
                String[] sa_columms = new String[sa_split.length];
                for( int i = 0; i < sa_split.length; i++ ) {
                    String[] sa_field = sa_split[i].split(" ");
                    sa_columms[i] = sa_field[0].trim();
                }
                // sa_columms now has the list of fields of the unknown structure
                // this will create and copy the old table
                checkTableQuery = "CREATE TABLE " + PROPERTIESTABLE + "_save AS SELECT * FROM " + PROPERTIESTABLE + ";";
                db_java.exec(checkTableQuery, null);
                // this will drop and recall this function [nasty] creating the new table and
                // filling it with default values
                resetStyleTable();
                // TODO: add check if all fields of old_table are in the new_table
                // TODO: set 'sa_columms[i]="";' if it the field does not exist in the new table
                // TODO: this portion HAS NOT been checked
                StringBuilder sb_update = new StringBuilder();
                sb_update.append("UPDATE " + PROPERTIESTABLE + "SET  ");
                for( int i = 0; i < sa_columms.length; i++ ) { // assums that the old fields also
                                                               // exist in the new table, empty
                                                               // sa_columms[i] if it is
                    if (!sa_columms[i].equals("")) {
                        sb_update
                                .append(sa_columms[i] + "= (SELECT " + PROPERTIESTABLE + "_save." + sa_columms[i] + " FROM "
                                        + PROPERTIESTABLE + "_save WHERE " + PROPERTIESTABLE + "_save.name=" + PROPERTIESTABLE
                                        + ".name)");
                        sb_update.append(",");
                    }
                }
                checkTableQuery = sb_update.toString();
                checkTableQuery = checkTableQuery.substring(0, checkTableQuery.length() - 1) + ";";
                GPLog.androidLog(-1, "SpatialiteDatabaseHandler[" + dbFile.getAbsolutePath() + "] col_count[" + i_column_count
                        + "] sql[" + checkTableQuery + "]");
                db_java.exec(checkTableQuery, null);
                checkTableQuery = "DROP TABLE " + PROPERTIESTABLE + "_save ;";
                db_java.exec(checkTableQuery, null);
            }
        }
    }

    /**
     * Retrieve the {@link Style} for a given table.
     *
     * @param tableName the table name.
     * @return the style.
     * @throws Exception  if something goes wrong.
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

    /**
     * Resets the style properties table through removal and default recreation.
     * 
     * @throws Exception  if something goes wrong.
     */
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
    // /**
    // * Check if the supplied bounds are inside the overall db bounds.
    // *
    // * @param boundsCoordinates the bounds to check as wsg84.
    // * @return true if the given bounds are inside the bounds of the all the Tables ; otherwise
    // false
    // */
    // public boolean checkBounds( double[] boundsCoordinates ) {
    // boolean b_rc = false;
    // if ((boundsCoordinates[0] >= this.boundsWest) && (boundsCoordinates[1] >= this.boundsSouth)
    // && (boundsCoordinates[2] <= this.boundsEast) && (boundsCoordinates[3] <= this.boundsNorth)) {
    // b_rc = true;
    // }
    // return b_rc;
    // }
    // -----------------------------------------------
    // /**
    // * Check of the Bounds of a specfic Vector-Tables.
    // *
    // * @param boundsCoordinates [as wsg84]
    // * @param spatialTable The table to check
    // * @return true if the given bounds are inside the bounds of the all the Tables
    // * ; otherwise false
    // */
    // public boolean checkTableInDatabaseBounds( double[] boundsCoordinates, SpatialVectorTable
    // spatialTable ) {
    // boolean b_rc = false;
    // if (checkBounds(boundsCoordinates)) {
    // b_rc = spatialTable.checkBounds(boundsCoordinates);
    // }
    // return b_rc;
    // }

    /**
      * Retrieve Bounds of a Vector-Tables (as float array)
      * 
      * <p>this is calculated when the Tables are created and stored as wsg84
      * 
      * @param spatialTable The table to check
      * @param destSrid The table to check
      * @return bounds as wsg84 [n, s, e, w]
      */
    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception {
        return spatialTable.getTableBounds();
    }

    /**
     * Update a style definition.
     *
     * @param style the {@link Style} to set.
     * @throws Exception  if something goes wrong.
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

    /**
      * Retrieve list of WKB geometries from the given table in the given bounds.
      *  
     * @param destSrid the destination srid.
     * @param table the vector table.
     * @param n north bound.
     * @param s south bound.
     * @param e east bound.
     * @param w west bound.
     * @return list of WKB geometries.
     */
    public List<byte[]> getWKBFromTableInBounds( String destSrid, SpatialVectorTable table, double n, double s, double e, double w ) {
        List<byte[]> list = new ArrayList<byte[]>();
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
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
        String query = SpatialiteUtilities.buildGeometriesInBoundsQuery(destSrid, table, n, s, e, w);
        // GPLog.androidLog(-1, "SpatialiteDatabaseHandler.getGeometryIteratorInBounds[" +
        // table.getUniqueName() + "]: query["
        // + query + "]");
        return new GeometryIterator(db_java, query);
    }

    /**
     * Close the database.
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

    // public void intersectionToString4Polygon( String queryPointSrid, SpatialVectorTable
    // spatialTable, double n, double e,
    // StringBuilder sb, String indentStr ) throws Exception {
    // boolean doTransform = false;
    // if (!spatialTable.getSrid().equals(queryPointSrid)) {
    // doTransform = true;
    // }
    //
    // StringBuilder sbQ = new StringBuilder();
    // sbQ.append("SELECT * FROM ");
    // sbQ.append(spatialTable.getName());
    // sbQ.append(" WHERE ST_Intersects(");
    // sbQ.append(spatialTable.getGeomName());
    // sbQ.append(",");
    // if (doTransform)
    // sbQ.append("ST_Transform(");
    // sbQ.append("MakePoint(");
    // sbQ.append(e);
    // sbQ.append(",");
    // sbQ.append(n);
    // if (doTransform) {
    // sbQ.append(",");
    // sbQ.append(queryPointSrid);
    // sbQ.append("),");
    // sbQ.append(spatialTable.getSrid());
    // }
    // sbQ.append(")) = 1 ");
    // sbQ.append("AND ROWID IN (");
    // sbQ.append("SELECT ROWID FROM Spatialindex WHERE f_table_name ='");
    // sbQ.append(spatialTable.getName());
    // sbQ.append("'");
    // // if a table has more than 1 geometry, the column-name MUST be given, otherwise no results.
    // sbQ.append(" AND f_geometry_column = '");
    // sbQ.append(spatialTable.getGeomName());
    // sbQ.append("'");
    // sbQ.append(" AND search_frame = ");
    // if (doTransform)
    // sbQ.append("ST_Transform(");
    // sbQ.append("MakePoint(");
    // sbQ.append(e);
    // sbQ.append(",");
    // sbQ.append(n);
    // if (doTransform) {
    // sbQ.append(",");
    // sbQ.append(queryPointSrid);
    // sbQ.append("),");
    // sbQ.append(spatialTable.getSrid());
    // }
    // sbQ.append("));");
    // String query = sbQ.toString();
    //
    // Stmt stmt = db_java.prepare(query);
    // try {
    // while( stmt.step() ) {
    // int column_count = stmt.column_count();
    // for( int i = 0; i < column_count; i++ ) {
    // String cName = stmt.column_name(i);
    // if (cName.equalsIgnoreCase(spatialTable.getGeomName())) {
    // continue;
    // }
    //
    // String value = stmt.column_string(i);
    // sb.append(indentStr).append(cName).append(": ").append(value).append("\n");
    // }
    // sb.append("\n");
    // }
    // } finally {
    // stmt.close();
    // }
    // }

    /**
      * Load list of Table [Vector/Raster] for GeoPackage Files [gpkg]
      * 
      * <b>THIS METHOD IS VERY EXPERIMENTAL AND A WORK IN PROGRESS</b>
      * 
      * <br>- name of Field
      * <br> - type of field as defined in Database
      * <br>- OGC 12-128r9 from 2013-11-19
      * <br>-- older versions will not be supported
      * <br>- With SQLite versions 3.7.17 and later : 'PRAGMA application_id' [1196437808]
      * <br>-- older (for us invalid)  Geopackage Files return 0
      * 
      * @param doLoadTable [for use when s_table is empty] 0=do not load table ; 1=load tables
      * @return the {@link HashMap} of field name to its type. 
      */
    private HashMap<String, String> collectGpkgTables( int doLoadTable ) throws Exception {
        Stmt this_stmt = null;
        HashMap<String, String> fieldName2TypeMap = new HashMap<String, String>();
        String s_srid = "";
        String s_gpkg = "gpkg"; // SELECT data_type,table_name,srs_id FROM gpkg_contents
        int i_srid = 0;
        String s_table_name = "";
        String s_tiles_field_name = "";
        String s_data_type = "";
        String s_sql_layers = "";
        int[] zoomLevels = {0, 22};
        switch( databaseType ) {
        case 10: { // GeoPackage Files [gpkg]
            StringBuilder sb_layers = new StringBuilder();
            s_sql_layers = "SELECT data_type,table_name,srs_id FROM " + s_gpkg + "_contents";
            // 20140101.world_Haiti.gpkg
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
            // SELECT table_name,srs_id FROM gpkg_contents WHERE data_type = 'features';
            try {
                this_stmt = db_java.prepare(s_sql_layers);
                while( this_stmt.step() ) {
                    i_srid = 0;
                    s_data_type = this_stmt.column_string(0);
                    // filter out everything we have no idea how to deal with
                    if ((s_data_type.equals("features")) || (s_data_type.equals("tiles"))) {
                        // 'featuresWithRasters' is being ignored until further notice
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
                                fieldName2TypeMap.put(s_table_name, i_srid + ";" + s_data_type);
                        }
                    }
                }
            } catch (java.lang.Exception e) {
                // invalid gpkg file when gpkg_contents does not exist
                isDatabaseValid = false;
                return fieldName2TypeMap;
            } finally {
                this_stmt.close();
            }
            ArrayList<SpatialVectorTable> vector_TableList = new ArrayList<SpatialVectorTable>();
            ArrayList<SpatialRasterTable> raster_TableList = new ArrayList<SpatialRasterTable>();
            HashMap<String, String> table_list = new HashMap<String, String>();
            fieldName2TypeMap = new HashMap<String, String>();
            for( int i = 0; i < fieldName2TypeMap.size(); i++ ) {
                for( Map.Entry<String, String> table_entry : table_list.entrySet() ) {
                    s_table_name = table_entry.getKey();
                    s_data_type = table_entry.getValue();
                    s_tiles_field_name = "tile_data";
                    String[] sa_split = s_data_type.split(";");
                    if (sa_split.length == 2) {
                        s_srid = sa_split[0];
                        i_srid = Integer.parseInt(s_srid);
                        s_data_type = sa_split[1];
                    }
                    // for 'tiles' the zoom levels
                    if ((!s_table_name.equals("")) && (s_data_type.equals("tiles"))) {
                        sb_layers.append("SELECT min(");
                        sb_layers.append("zoom_level");
                        sb_layers.append("),max(");
                        sb_layers.append("zoom_level");
                        sb_layers.append(") FROM ");
                        sb_layers.append(s_gpkg + "_tile_matrix");
                        sb_layers.append(" WHERE ");
                        sb_layers.append("table_name");
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
                        } catch (java.lang.Exception e) {
                            GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_gpkg [tiles - min/max zoom] prepair["
                                    + s_sql_layers + "]", e);
                        } finally {
                            if (this_stmt != null) {
                                this_stmt.close();
                            }
                        }
                    }
                    // for 'features' and 'tiles' the bounds
                    if (!s_table_name.equals("")) {
                        if (!s_srid.equals("4326")) {
                            // [Sample_Geopackage_Haiti.gpkg, but was 4326
                            // and does not need to be transformed]
                            sb_layers.append("SELECT ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("(min_x + (max_x-min_x)/2), ");
                            sb_layers.append("(min_y + (max_y-min_y)/2), ");
                            sb_layers.append("srs_id");
                            sb_layers.append("),4326))) AS Center,");
                            sb_layers.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("min_x,min_y, ");
                            sb_layers.append("srs_id");
                            sb_layers.append("),4326))) AS South_West,");
                            sb_layers.append("ST_AsBinary(CastToXY(ST_Transform(MakePoint(");
                            sb_layers.append("max_x,max_y, ");
                            sb_layers.append("srs_id");
                            sb_layers.append("),4326))) AS North_East FROM ");
                            sb_layers.append(s_gpkg + "_contents");
                            sb_layers.append(" WHERE ");
                            sb_layers.append("table_name");
                            sb_layers.append("='");
                            sb_layers.append(s_table_name);
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE);
                            // sb_layers.append("='");
                            // sb_layers.append(METADATA_GEOPACKAGECONTENT_DATA_TYPE_FEATURES);
                            sb_layers.append("';");
                        } else {
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
                            sb_layers.append(s_gpkg + "_contents");
                            sb_layers.append(" WHERE ");
                            sb_layers.append("table_name");
                            sb_layers.append("='");
                            sb_layers.append(s_table_name);
                            sb_layers.append("';");
                        }
                        s_sql_layers = sb_layers.toString();
                        if (!s_sql_layers.equals("")) {
                            isDatabaseValid = true;
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
                                    // Zoom levels with non-vector data
                                    checkAndAdaptDatabaseBounds(boundsCoordinates, zoomLevels);
                                    if (s_data_type.equals("features")) {
                                        // TODO
                                    }
                                    if (s_data_type.equals("tiles")) {
                                        SpatialRasterTable table = new SpatialRasterTable(getDatabasePath(), "", s_srid,
                                                zoomLevels[0], zoomLevels[1], centerCoordinate[0], centerCoordinate[1], null,
                                                boundsCoordinates);
                                        table.setMapType(s_gpkg);
                                        table.setTableName(s_table_name);
                                        table.setColumnName(s_tiles_field_name);
                                        // setDescription(s_table_name);
                                        // table.setDescription(this.databaseDescription);
                                        raster_TableList.add(table);
                                    }
                                }
                            } catch (java.lang.Exception e) {
                                GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_gpkg [bounds] prepair[" + s_sql_layers
                                        + "]", e);
                            } finally {
                                if (this_stmt != null) {
                                    this_stmt.close();
                                }
                            }
                            if (vector_TableList.size() > 0)
                                this.vectorTableList = vector_TableList;
                            if (raster_TableList.size() > 0)
                                this.rasterTableList = raster_TableList;
                        }
                    }
                }
            }
        }
            break;
        }
        return fieldName2TypeMap;
    }

    /**
      * Load list of Table [Vector] for Spatialite Files
      * 
      * <br>- name of Field
      * <br>- type of field as defined in Database
      * 
      * @param doLoadTable [for use when s_table is empty] 
      *                 <br>0=do not load table
      *                 <br>1=load tables
      * @return the {@link HashMap} of field name to its type. 
      */
    private HashMap<String, String> collectSpatialiteTables( int doLoadTable ) throws Exception {
        Stmt this_stmt = null;
        List<SpatialVectorTable> vectorTableList;
        HashMap<String, String> table_fields = new HashMap<String, String>();
        StringBuilder sb_layers = new StringBuilder();
        String s_srid = "";
        int i_srid = 0;
        String table_name = "";
        String s_sql_layers = "";
        switch( databaseType ) {
        case 3: { // Spatialite Files version 2+3=3
            sb_layers.append("SELECT ");
            sb_layers.append("f_table_name");
            sb_layers.append(", ");
            sb_layers.append("f_geometry_column");
            sb_layers.append(", ");
            sb_layers.append("type");
            sb_layers.append(",");
            sb_layers.append("srid");
            sb_layers.append(" FROM ");
            sb_layers.append("geometry_columns");
            sb_layers.append("  ORDER BY f_table_name;");
            // version 3 ['type' instead of 'geometry_type']:
            // SELECT f_table_name,f_geometry_column,geometry_type,srid FROM geometry_columns ORDER
            // BY
            // f_table_name
            s_sql_layers = sb_layers.toString();
            break;
        }
        case 4: { // Spatialite Files version 4=4
            sb_layers.append("SELECT ");
            sb_layers.append(METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".table_name"); // 0
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_STATISTICS_TABLE_NAME + ".geometry_column"); // 1
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "geometry_type"); // 2
            sb_layers.append(", " + METADATA_VECTOR_LAYERS_TABLE_NAME + "." + "srid"); // 3
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
            s_sql_layers = sb_layers.toString();
            // version 4 ['geometry_type' instead of 'type']: SELECT
            // f_table_name,f_geometry_column,geometry_type,srid FROM geometry_columns ORDER BY
            // f_table_name
            break;
        }
        }
        if (!s_sql_layers.equals("")) {
            sb_layers = new StringBuilder();
            isDatabaseValid = true;
            vectorTableList = new ArrayList<SpatialVectorTable>();
            table_fields = new HashMap<String, String>();
            String geometry_column = "";
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
                    int i_test = 0;
                    // i_CheckSpatialIndex is returning 0 all the time and can be
                    // used
                    if ((!table_name.equals("")) && (!geometry_column.equals("")) && (i_test > 0)) {
                        String s_CheckSpatialIndex = "SELECT CheckSpatialIndex('" + table_name + "','" + geometry_column + "');";
                        int i_CheckSpatialIndex = -1;
                        try {
                            bounds_stmt = db_java.prepare(s_CheckSpatialIndex);
                            if (bounds_stmt.step()) {
                                i_CheckSpatialIndex = bounds_stmt.column_int(0);
                            }
                        } catch (Exception e) {
                            GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_spatialite prepair[" + s_CheckSpatialIndex
                                    + "]", e);
                        } finally {
                            if (bounds_stmt != null) {
                                bounds_stmt.close();
                            }
                            if (i_CheckSpatialIndex < 1) {
                                GPLog.androidLog(-1, "SpatialiteDatabaseHandler[" + getDatabasePath() + "] tablename["
                                        + table_name + "] geometry_column[" + geometry_column + "] i_CheckSpatialIndex["
                                        + i_CheckSpatialIndex + "]");
                            }
                        }
                    }
                    if (databaseType == 3) {
                        // for older spatialite v2+3 : Query extent of table
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
                            GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_spatialite prepair[" + s_select_bounds
                                    + "]", e);
                        } finally {
                            if (bounds_stmt != null) {
                                bounds_stmt.close();
                            }
                        }
                    } else if (databaseType == 4) {
                        // for older spatialite v4 : Retrieve extent of
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
                                && (boundsCoordinates[3] == 0)) {
                            if ((!s_layer_type.equals("")) && (i_row_count == 0)) {
                                // at the moment we are reading one row of possibly many rows
                                if (b_UpdateLayerStatistics) {
                                    // do this only for the first row,
                                    // the next time the application is
                                    // run it will have a proper table
                                    String s_UpdateLayerStatistics = "SELECT UpdateLayerStatistics();";
                                    int i_UpdateLayerStatistics = -1;
                                    try {
                                        bounds_stmt = db_java.prepare(s_UpdateLayerStatistics);
                                        if (bounds_stmt.step()) {
                                            i_UpdateLayerStatistics = this_stmt.column_int(0);
                                        }
                                    } finally {
                                        if (bounds_stmt != null) {
                                            bounds_stmt.close();
                                        }
                                        // the next time this
                                        // application reads
                                        // this database it will
                                        // have a proper table
                                        if (i_UpdateLayerStatistics == 1) {
                                            // UpdateLayerStatistics
                                            // is not needed
                                            b_UpdateLayerStatistics = false;
                                        }
                                    }
                                }
                            }
                            if ((boundsCoordinates[0] == 0) && (boundsCoordinates[1] == 0) && (boundsCoordinates[2] == 0)
                                    && (boundsCoordinates[3] == 0)) {
                                // this time (after
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
                                    GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_spatialite prepair["
                                            + s_select_bounds + "]", e);
                                } finally {
                                    if (bounds_stmt != null) {
                                        bounds_stmt.close();
                                    }
                                }
                            }
                        } else {
                            // we have found a valid record
                            // this will prevent UpdateLayerStatistics being called on empty
                            // tables - when they ARE not the first table
                            b_UpdateLayerStatistics = false; // UpdateLayerStatistics is not needed
                        }
                    }
                    // this should have a list of unique geometry-fields, we will look later for
                    // these in the views
                    if (table_fields.get(geometry_column) == null)
                        table_fields.put(geometry_column, s_geometry_type);
                    if (!s_srid.equals("4326")) { // Transform into wsg84 if needed
                        collectBoundsAndCenter(s_srid, centerCoordinate, boundsCoordinates);
                    } else {
                        centerCoordinate[0] = boundsCoordinates[0] + (boundsCoordinates[2] - boundsCoordinates[0]) / 2;
                        centerCoordinate[1] = boundsCoordinates[1] + (boundsCoordinates[3] - boundsCoordinates[1]) / 2;
                    }
                    checkAndAdaptDatabaseBounds(boundsCoordinates, null);
                    // no Zoom levels with
                    // vector data
                    SpatialVectorTable table = new SpatialVectorTable(getDatabasePath(), table_name, geometry_column,
                            i_geometry_type, s_srid, centerCoordinate, boundsCoordinates, s_layer_type, i_row_count,
                            i_coord_dimension, i_spatial_index_enabled, s_last_verified);
                    // compleate list of fields of
                    // this table
                    fields_list = collectFields(table_name, 0);
                    table.setFieldsList(fields_list);
                    vectorTableList.add(table);
                }
            } catch (Exception e) {
                GPLog.androidLog(4, "SpatialiteDatabaseHandler.get_tables_spatialite prepair[" + s_sql_layers + "]", e);
            } finally {
                if (this_stmt != null) {
                    this_stmt.close();
                }
            }
            this.vectorTableList = vectorTableList;
        }
        return table_fields;
    }

    /**
      * Checks (and adapts) the overall database bounds based on the passed coordinates.
      * 
      * <p>Goal: when painting the Geometries: check of viewport is inside these bounds.
      * <br>- if the Viewport is outside these Bounds: all Tables can be ignored
      * <br>-- this is called when the Tables are created
      * 
      * @param boundsCoordinates bounds to check against the overall.
      */
    private void checkAndAdaptDatabaseBounds( double[] boundsCoordinates, int[] zoomLevels ) {
        if ((this.boundsWest == 0.0) && (this.boundsSouth == 0.0) && (this.boundsEast == 0.0) && (this.boundsNorth == 0.0)) {
            this.boundsWest = boundsCoordinates[0];
            this.boundsSouth = boundsCoordinates[1];
            this.boundsEast = boundsCoordinates[2];
            this.boundsNorth = boundsCoordinates[2];
        } else {
            if (boundsCoordinates[0] < this.boundsWest)
                this.boundsWest = boundsCoordinates[0];
            if (boundsCoordinates[1] < this.boundsSouth)
                this.boundsSouth = boundsCoordinates[1];
            if (boundsCoordinates[2] > this.boundsEast)
                this.boundsEast = boundsCoordinates[2];
            if (boundsCoordinates[3] < this.boundsNorth)
                this.boundsNorth = boundsCoordinates[3];
        }
        centerX = this.boundsWest + (this.boundsEast - this.boundsWest) / 2;
        centerY = this.boundsSouth + (this.boundsNorth - this.boundsSouth) / 2;
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

    /**
      * Collects tables.
      * 
      * <ul>
      * <li>name of Field
      * <li>type of field as defined in Database
      * </ul>
      * 
      * @param doLoadTables 0 = do not load table; 1=load tables
      * @return fields_list [name of field, type of field]
      */
    private HashMap<String, String> checkAndCollectTables( int doLoadTables ) throws Exception {
        HashMap<String, String> tableFields = new HashMap<String, String>();
        if (view_list == null) {
            // guess what we forgot on the first attempt!
            tableFields = collectFields(null, 0);
        }
        if (doLoadTables == 0) {
            return tableFields;
        }
        switch( databaseType ) {
        case 10: {
            // GeoPackage Files [gpkg]
            // b_database_valid=false;
            tableFields = collectGpkgTables(doLoadTables);
        }
            break;
        case 3:
        case 4: {
            // Spatialite Files version 2+3=3 ; version 4=4
            // this will return a unique list of geometry-fields from all tables
            tableFields = collectSpatialiteTables(doLoadTables);
        }
            break;
        }
        if (isValid()) {
            switch( databaseType ) {
            case 3:
            case 4: { // Spatialite Files version 2+3=3 ; version 4=4
                      // 'table_fields' will have a unique list of geometry-fields from all tables
                for( int i = 0; i < view_list.size(); i++ ) {
                    for( Map.Entry<String, String> view_entry : view_list.entrySet() ) {
                        String s_view_name = view_entry.getKey();
                        // String s_view_data = view_entry.getValue(); // TODO remove newlines
                        GPLog.androidLog(-1, "SpatialiteDatabaseHandler[" + getDatabasePath() + "] view[" + s_view_name + "]   ");
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
        }
        return tableFields;
    }

    /**
      * Collects the fields of a table, also checking the database type.
      * 
      * <br>- name of Field
      * <br>- type of field as defined in Database
      * 
      * @param tableName name of table to read. 
      *             If <code>null</code>: list of all tables in Database.
      * @param doLoadTable [for use when table is empty] 
      *             <br>0=do not load table 
      *             <br>1=load tables
      * @return the {@link HashMap} of fields: [name of field, type of field]
      */
    private HashMap<String, String> collectFields( String tableName, int doLoadTable ) throws Exception {

        // views: vector_layers_statistics,vector_layers
        boolean b_vector_layers_statistics = false;
        boolean b_vector_layers = false;

        // tables: geometry_columns,raster_columns
        boolean b_geometry_columns = false;
        // boolean b_raster_columns = false;
        boolean b_gpkg_contents = false;
        boolean b_geopackage_contents = false;

        HashMap<String, String> fieldNamesToTypeMap = new LinkedHashMap<String, String>();
        String s_sql_command = "";
        if (tableName != null) {
            s_sql_command = "pragma table_info(" + tableName + ")";
        } else {
            s_sql_command = "SELECT name,type,sql FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
            if (view_list == null) {
                view_list = new HashMap<String, String>();
            } else {
                view_list.clear();
            }
            databaseType = 0;
            isDatabaseValid = false;
        }
        String tableType = "";
        String sqlCreationString = "";
        String name = "";
        Stmt statement = db_java.prepare(s_sql_command);
        try {
            while( statement.step() ) {
                if (tableName != null) {
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
                } else {
                    name = statement.column_string(0);
                    tableType = statement.column_string(1);
                    sqlCreationString = statement.column_string(2);
                    // GPLog.androidLog(-1,"SpatialiteDatabaseHandler.get_table_fields["+s_table+"] tablename["+s_name+"] type["+s_type+"] sql["
                    // + s_sql_create+ "] ");
                    if (tableType.equals("table")) {
                        if (name.equals("geometry_columns")) {
                            b_geometry_columns = true;
                        } else if (name.equals("gpkg_contents")) {
                            b_gpkg_contents = true;
                        } else if (name.equals("geopackage_contents")) {
                            b_geopackage_contents = true;
                        }
                        // if (name.equals("raster_columns")) {
                        // b_raster_columns = true;
                        // }
                    } else if (tableType.equals("view")) {
                        // we are looking for user-defined views only,
                        // filter out system known views.
                        if ((!name.equals("geom_cols_ref_sys")) && (!name.startsWith("vector_layers"))) {
                            view_list.put(name, sqlCreationString);
                        } else if (name.equals("vector_layers_statistics")) {
                            b_vector_layers_statistics = true;
                        } else if (name.equals("vector_layers")) {
                            b_vector_layers = true;
                        }
                    }
                }
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        if (tableName == null) {
            if (b_geopackage_contents) {
                // an old geopackage file, may look like a Spatialite Table
                // - but invalid srid
                isDatabaseValid = false;
                return fieldNamesToTypeMap;
            }
            if (b_gpkg_contents) {
                // this is a GeoPackage, this can also have
                // vector_layers_statistics and vector_layers
                // - the results are empty, it does reference the table
                // also referenced in gpkg_contents
                databaseType = 10;
                isDatabaseValid = true;
            } else {
                if ((b_vector_layers_statistics) && (b_vector_layers)) { // Spatialite 4.0
                    databaseType = 4;
                    isDatabaseValid = true;
                } else {
                    if (b_geometry_columns) { // Spatialite before 4.0
                        databaseType = 3;
                        isDatabaseValid = true;
                    }
                }
            }
        }
        // GPLog.androidLog(-1,"SpatialiteDatabaseHandler.get_table_fields["+s_table+"] ["+getFileNamePath()+"] valid["+b_database_valid+"] database_type["+i_database_type+"] sql["
        // + s_sql_command+ "] ");
        return fieldNamesToTypeMap;
    }

}
