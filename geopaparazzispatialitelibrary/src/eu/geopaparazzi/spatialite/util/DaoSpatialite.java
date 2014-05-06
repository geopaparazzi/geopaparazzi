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

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
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
                throw new IOException("SpatialiteUtilities: create_db: dir_db[" + dir_db.getAbsolutePath() + "] creation failed"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        spatialiteDatabase = new jsqlite.Database();
        if (spatialiteDatabase != null) {
            try {
                spatialiteDatabase.open(file_db.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                        | jsqlite.Constants.SQLITE_OPEN_CREATE);
                createSpatialiteDb(spatialiteDatabase, 0); // i_rc should be 4
            } catch (jsqlite.Exception e_stmt) {
                GPLog.androidLog(4, "SpatialiteUtilities: create_spatialite[spatialite] dir_file[" + file_db.getAbsolutePath() //$NON-NLS-1$
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
                GPLog.androidLog(4, "SpatialiteUtilities: create_spatialite sql[" + s_sql_command + "] rc=" + i_rc + "]", e_stmt); //$NON-NLS-1$ //$NON-NLS-2$
            }
            // GPLog.androidLog(2,
            // "SpatialiteUtilities: create_spatialite sql["+s_sql_command+"] rc="+i_rc+"]");
            i_rc = getSpatialiteDatabaseVersion(sqliteDatabase, ""); //$NON-NLS-1$
            if (i_rc < 3) { // error, should be 3 or 4
                GPLog.androidLog(4, "SpatialiteUtilities: create_spatialite spatialite_version[" + i_rc + "]"); //$NON-NLS-1$ //$NON-NLS-2$
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
        Stmt stmt = database.prepare(checkTableQuery);
        try {
            if (stmt.step()) {
                String creationSql = stmt.column_string(0);
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
            stmt.close();
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
    public static Style createDefaultPropertiesForTable( Database database, String spatialTableUniqueName ) throws Exception {
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
    public static Style getStyle4Table( Database database, String spatialTableUniqueName ) throws Exception {
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
            style = createDefaultPropertiesForTable(database, spatialTableUniqueName);
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
     * Return info of supported versions in JavaSqlite.
     *
     * <br>- JavaSqlite
     * <br>- Spatialite
     * <br>- Proj4
     * <br>- Geos
     * <br>-- there is no Spatialite function to retrieve the Sqlite version
     * <br>-- the Has() functions do not work with spatialite 3.0.1
     *
     * @param database the db to use.
     * @param name a name for the log.
     * @return info of supported versions in JavaSqlite.
     */
    public static String getJavaSqliteDescription( Database database, String name ) {
        String s_javasqlite_description = "";
        try {
            s_javasqlite_description = "javasqlite[" + getJavaSqliteVersion() + "],";
            s_javasqlite_description += "spatialite[" + getSpatialiteVersion(database) + "],";
            s_javasqlite_description += "proj4[" + getProj4Version(database) + "],";
            s_javasqlite_description += "geos[" + getGeosVersion(database) + "],";
            s_javasqlite_description += "spatialite_properties[" + getSpatialiteProperties(database) + "]]";
        } catch (Exception e) {
            s_javasqlite_description += "exception[? not a spatialite database, or spatialite < 4 ?]]";
            GPLog.androidLog(4, "SpatialiteDatabaseHandler[" + name + "].getJavaSqliteDescription[" + s_javasqlite_description
                    + "]", e);
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

    /**
     * Get the version of Spatialite.
     *
     * @param database the db to use.
     * @return the version of Spatialite.
     * @throws Exception  if something goes wrong.
     */
    public static String getSpatialiteVersion( Database database ) throws Exception {
        Stmt stmt = database.prepare("SELECT spatialite_version();");
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
     * Checks the database type and its validity.
     * 
     * @param database the database to check.
     * @param databaseViewsMap the {@link HashMap} of database views data to clear and repopulate.
     * @return the {@link SpatialiteDatabaseType}.
     */
    public static SpatialiteDatabaseType checkDatabaseTypeAndValidity( Database database, HashMap<String, String> databaseViewsMap ) {
        try {
            // clear views
            databaseViewsMap.clear();

            // views: vector_layers_statistics,vector_layers
            boolean b_vector_layers_statistics = false;
            boolean b_vector_layers = false;

            // tables: geometry_columns,raster_columns
            boolean b_geometry_columns = false;
            // boolean b_raster_columns = false;
            boolean b_gpkg_contents = false;
            boolean b_geopackage_contents = false;

            String sqlCommand = "SELECT name,type,sql FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
            String tableType = "";
            String sqlCreationString = "";
            String name = "";
            Stmt statement = database.prepare(sqlCommand);
            try {
                while( statement.step() ) {
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
                            databaseViewsMap.put(name, sqlCreationString);
                        } else if (name.equals("vector_layers_statistics")) {
                            b_vector_layers_statistics = true;
                        } else if (name.equals("vector_layers")) {
                            b_vector_layers = true;
                        }
                    }
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
            if (b_geopackage_contents) {
                // an old geopackage file, may look like a Spatialite Table
                // - but invalid srid
                // isDatabaseValid = false;
                return SpatialiteDatabaseType.UNKNOWN;
            }
            if (b_gpkg_contents) {
                // this is a GeoPackage, this can also have
                // vector_layers_statistics and vector_layers
                // - the results are empty, it does reference the table
                // also referenced in gpkg_contents
                return SpatialiteDatabaseType.GEOPACKAGE;
            } else {
                if ((b_vector_layers_statistics) && (b_vector_layers)) { // Spatialite 4.0
                    return SpatialiteDatabaseType.SPATIALITE4;
                } else {
                    if (b_geometry_columns) { // Spatialite before 4.0
                        return SpatialiteDatabaseType.SPATIALITE3;
                    }
                }
            }
        } catch (Exception e) {
            GPLog.error("DAOSPATIALITE", "Error in checkDatabaseTypeAndValidity", e);
        }
        return SpatialiteDatabaseType.UNKNOWN;
    }

    /**
     * Determine the Spatialite version of the Database being used.
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
                        // if (s_name.equals("views_geometry_columns")) {
                        // b_views_geometry_columns = true;
                        // }
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
            GPLog.androidLog(-1, "SpatialiteUtilities: get_table_fields sql[" + s_sql_command + "] geometry_columns["
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
        String s_sql_command = "pragma table_info(" + tableName + ")";
        String tableType = "";
        String sqlCreationString = "";
        String name = "";
        Stmt statement = database.prepare(s_sql_command);
        try {
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
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return fieldNamesToTypeMap;
    }
}
