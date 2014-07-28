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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteDatabaseType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteVersion;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * Dao that handles database engine properties.
 */
public class DatabaseCreationAndProperties implements ISpatialiteTableAndFieldsNames {

    /**
     * Return info of supported versions.
     * - will be filled on first Database connection when empty
     * -- called in checkDatabaseTypeAndValidity
     */
    public static String JavaSqliteDescription = "";

    /**
     * General Function to create jsqlite.Database with spatialite support.
     * <ol>
     * <li> parent directories will be created, if needed</li>
     * <li> needed Tables/View and default values for metadata-table will be created</li>
     * </ol>
     *
     * @param databasePath name of Database file to create
     * @return sqlite_db: pointer to Database created
     * @throws java.io.IOException if something goes wrong.
     */
    public static Database createDb(String databasePath) throws IOException {
        File file_db = new File(databasePath);
        if (!file_db.getParentFile().exists()) {
            File dir_db = file_db.getParentFile();
            if (!dir_db.mkdir()) {
                throw new IOException("DaoSpatialite: create_db: dir_db[" + dir_db.getAbsolutePath() + "] creation failed"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        Database spatialiteDatabase = new Database();
        try {
            spatialiteDatabase.open(file_db.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
            createSpatialiteDb(spatialiteDatabase, false);
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "create_spatialite[spatialite] dir_file[" + file_db.getAbsolutePath() //$NON-NLS-1$
                    + "]", e_stmt); //$NON-NLS-1$
        }
        return spatialiteDatabase;
    }

    /**
     * General Function to create jsqlite.Database with spatialite support.
     * <p/>
     * <ol>
     * <li> parent directories will be created, if needed</li>
     * <li> needed Tables/View and default values for metadata-table will be created</li>
     * </ol>
     *
     * @param sqliteDatabase pointer to Database
     * @param doCheck        is true, a new Database is created without checking if it is already one.
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static void createSpatialiteDb(Database sqliteDatabase, boolean doCheck) throws jsqlite.Exception {
        boolean createDb = true;
        if (doCheck) {
            SpatialiteVersion spatialiteVersion = getSpatialiteDatabaseVersion(sqliteDatabase, "");
            // this is a spatialite Database, do not create
            if (spatialiteVersion.getCode() > SpatialiteVersion.NO_SPATIALITE.getCode()) {
                createDb = false;
                if (spatialiteVersion.getCode() < SpatialiteVersion.UNTIL_3_1_0_RC2.getCode()) {
                    // TODO: logic for conversion to latest Spatialite
                    // Version [open]
                    throw new Exception("Spatialite version < 3 not supported.");
                }
            }
        }
        if (createDb) {
            String s_sql_command = "SELECT InitSpatialMetadata();"; //$NON-NLS-1$
            try {
                sqliteDatabase.exec(s_sql_command, null);
            } catch (jsqlite.Exception e_stmt) {
                int errorCode = sqliteDatabase.last_error();
                GPLog.error("DAOSPATIALIE", "create_spatialite sql[" + s_sql_command + "] errorCode=" + errorCode + "]", e_stmt); //$NON-NLS-1$ //$NON-NLS-2$
            }

            SpatialiteVersion spatialiteVersion = getSpatialiteDatabaseVersion(sqliteDatabase, ""); //$NON-NLS-1$
            if (spatialiteVersion.getCode() < 3) { // error, should be 3 or 4
                GPLog.addLogEntry("DAOSPATIALIE", "create_spatialite spatialite_version[" + spatialiteVersion + "]"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }


    /**
     * Checks the database type and its validity.
     * - Spatialite 2.4 to present version are supported (2.4 will be set as 3)
     *
     * @param database               the database to check.
     * @param spatialVectorMap       the {@link java.util.HashMap} of database views data to clear and repopulate.
     * @param spatialVectorMapErrors
     * @return the {@link SpatialiteDatabaseType}.
     */
    public static SpatialiteDatabaseType checkDatabaseTypeAndValidity(Database database, HashMap<String, String> spatialVectorMap, HashMap<String, String> spatialVectorMapErrors) throws Exception {
        // clear views
        spatialVectorMap.clear();
        spatialVectorMapErrors.clear();
        if (DatabaseCreationAndProperties.JavaSqliteDescription.equals("")) { // Rasterlite2Version_CPU will NOT be empty, if the
            // Driver was compiled with RasterLite2 support
            DatabaseCreationAndProperties.getJavaSqliteDescription(database, "DaoSpatialite.checkDatabaseTypeAndValidity");
            GPLog.addLogEntry("DAOSPATIALIE", "JavaSqliteDescription[" + DatabaseCreationAndProperties.JavaSqliteDescription + "] recovery_mode["
                    + SPL_Vectors.VECTORLAYER_QUERYMODE + "]");
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
        // this table dissapered (maybe 4.1.0) - when vector_layers_statistics is not set, this may
        // be used for the bounds
        boolean b_layers_statistics = false;
        // boolean b_raster_columns = false;
        boolean b_gpkg_contents = false;
        String sqlCommand = "SELECT name,type,sql FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
        String tableType = "";
        String name = "";
        Stmt statement = null;
        try {
            statement = database.prepare(sqlCommand);
            while (statement.step()) {
                name = statement.column_string(0);
                tableType = statement.column_string(1);
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
            GPLog.error("DAOSPATIALITE",
                    "Error in checkDatabaseTypeAndValidity sql[" + sqlCommand + "] db[" + database.getFilename() + "]", e);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        if (b_gpkg_contents) {
            // this is a GeoPackage, this can also have
            // vector_layers_statistics and vector_layers
            // - the results are empty, it does reference the table
            // also referenced in gpkg_contents
            SPL_Geopackage.getGeoPackageMap_R10(database, spatialVectorMap, spatialVectorMapErrors);
            if (spatialVectorMap.size() > 0)
                return SpatialiteDatabaseType.UNKNOWN;
                // return SpatialiteDatabaseType.GEOPACKAGE;
            else
                // if empty, nothing to load
                return SpatialiteDatabaseType.UNKNOWN;
        } else {
            if ((b_vector_layers_statistics) && (b_vector_layers)) { // Spatialite 4.0
                SPL_Vectors.getSpatialVectorMap_V4(database, spatialVectorMap, spatialVectorMapErrors, b_layers_statistics,
                        b_raster_coverages);
                if (spatialVectorMap.size() > 0)
                    return SpatialiteDatabaseType.SPATIALITE4;
                else
                    // if empty, nothing to load
                    return SpatialiteDatabaseType.UNKNOWN;
            } else {
                if ((b_geometry_columns) && (b_views_geometry_columns)) { // Spatialite from 2.4
                    // until 4.0
                    SPL_Vectors.getSpatialVectorMap_V3(database, spatialVectorMap, spatialVectorMapErrors, b_layers_statistics,
                            b_SpatialIndex);
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
     * Checks if a table exists.
     *
     * @param database the db to use.
     * @param name     the table name to check.
     * @return the number of columns, if the table exists or 0 if the table doesn't exist.
     * @throws Exception if something goes wrong.
     */
    public static int checkTableExistence(Database database, String name) throws Exception {
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
            if (statement != null)
                statement.close();
        }
    }

    /**
     * Return info of supported versions in JavaSqlite.
     * <p/>
     * <br>- SQLite used by the Database-Driver
     * <br>- Spatialite
     * <br>- Proj4
     * <br>- Geos
     * <br>-- there is no Spatialite function to retrieve the Sqlite version
     * <br>-- the Has() functions do not work with spatialite 3.0.1
     *
     * @param database the db to use.
     * @param name     a name for the log.
     * @return info of supported versions in JavaSqlite.
     */
    public static String getJavaSqliteDescription(Database database, String name) {
        if (JavaSqliteDescription.equals("")) {
            int majorVersion = 0;
            try {
                // s_javasqlite_description = "javasqlite[" + getJavaSqliteVersion() + "],";
                JavaSqliteDescription = "sqlite[" + getSqliteVersion(database) + "],";

                String spatialiteVersionNumber = getSpatialiteVersionNumber(database);
                if (!spatialiteVersionNumber.equals("-"))
                    majorVersion = Integer.parseInt(spatialiteVersionNumber.substring(0, 1));
                JavaSqliteDescription += "spatialite[" + spatialiteVersionNumber + "],";
                JavaSqliteDescription += "proj4[" + getProj4Version(database) + "],";
                JavaSqliteDescription += "geos[" + getGeosVersion(database) + "],";
                JavaSqliteDescription += "spatialite_properties[" + getSpatialiteProperties(database) + "],";
                JavaSqliteDescription += "rasterlite2_properties[" + getRaster2Version(database) + "]]";
            } catch (Exception e) {
                if (majorVersion > 3) {
                    JavaSqliteDescription += "rasterlite2_properties[none]]";
                } else {
                    JavaSqliteDescription += "exception[? not a spatialite database, or spatialite < 4 ?]]";
                    GPLog.error("DAOSPATIALIE", "[" + name + "].getJavaSqliteDescription[" + JavaSqliteDescription + "]", e);
                }
            }
        }
        return JavaSqliteDescription;
    }

    /**
     * Return SQLite version number as string.
     * - as used by the Driver that queries for Spatialite
     *
     * @param database the db to use.
     * @return the version of sqlite.
     * @throws Exception if something goes wrong.
     */
    public static String getSqliteVersion(Database database) throws Exception {
        return database.dbversion();
    }


    /**
     * Get the version of Spatialite.
     *
     * @param database the db to use.
     * @return the version of Spatialite.
     * @throws Exception if something goes wrong.
     */
    public static String getSpatialiteVersionNumber(Database database) throws Exception {
        Stmt stmt = database.prepare("SELECT spatialite_version();");
        try {
            if (stmt.step()) {
                return stmt.column_string(0);
            }
        } finally {
            stmt.close();
        }
        return "-";
    }


    /**
     * Determine the Spatialite version of the Database being used.
     * <p/>
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
     * <p/>
     * <p>20131129: at the moment not possible to distinguish between 2.4.0 and 3.0.0 [no '2']
     *
     * @param database Database connection to use
     * @param table    name of table to read [if empty: list of tables in Database]
     * @return the {@link eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteVersion}.
     * @throws Exception if something goes wrong.
     */
    public static SpatialiteVersion getSpatialiteDatabaseVersion(Database database, String table) throws Exception {
        // views: vector_layers_statistics,vector_layers
        // boolean b_vector_layers_statistics = false;
        // boolean b_vector_layers = false;
        // tables: geometry_columns,raster_columns

        /*
         * false = not a spatialite Database
         * true = a spatialite Database
         */
        boolean b_geometry_columns = false;

        SpatialiteVersion versionFromSrswktPresence = SpatialiteVersion.SRS_WKT__NOTFOUND_PRE_2_4_0;
        boolean b_spatial_ref_sys = false;
        // boolean b_views_geometry_columns = false;
        SpatialiteVersion spatialiteVersion = SpatialiteVersion.NO_SPATIALITE;
        String s_sql_command;
        if (!table.equals("")) { // pragma table_info(geodb_geometry)
            s_sql_command = "pragma table_info(" + table + ")";
        } else {
            s_sql_command = "SELECT name,type FROM sqlite_master WHERE ((type='table') OR (type='view')) ORDER BY type DESC,name ASC";
        }
        String type;
        String name;
        Stmt this_stmt = database.prepare(s_sql_command);
        try {
            while (this_stmt.step()) {
                if (!table.equals("")) { // pragma table_info(berlin_strassen_geometry)
                    name = this_stmt.column_string(1);
                    // 'proj4text' must always exist - otherwise invalid
                    if (name.equals("proj4text"))
                        b_spatial_ref_sys = true;
                    if (name.equals("srs_wkt"))
                        versionFromSrswktPresence = SpatialiteVersion.SRS_WKT__2_4_0_to_3_1_0;
                    if (name.equals("srtext"))
                        versionFromSrswktPresence = SpatialiteVersion.SRS_WKT__FROM_4_0_0;
                }
                if (table.equals("")) {
                    name = this_stmt.column_string(0);
                    type = this_stmt.column_string(1);
                    if (type.equals("table")) {
                        // if (s_name.equals("geometry_columns")) {
                        // b_geometry_columns = true;
                        // }
                        if (name.equals("spatial_ref_sys")) {
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
            if ((b_geometry_columns) && (b_spatial_ref_sys)) {
                if (b_spatial_ref_sys) {
                    versionFromSrswktPresence = getSpatialiteDatabaseVersion(database, "spatial_ref_sys");
                    if (versionFromSrswktPresence == SpatialiteVersion.AFTER_4_0_0_RC1) { // Spatialite 4.0
                        spatialiteVersion = SpatialiteVersion.AFTER_4_0_0_RC1;
                    } else {
                        spatialiteVersion = versionFromSrswktPresence;
                    }
                }
            }
        } else {
            if (b_spatial_ref_sys) { // 'proj4text' must always exist - otherwise invalid
                switch (versionFromSrswktPresence) {
                    case SRS_WKT__NOTFOUND_PRE_2_4_0:
                        spatialiteVersion = SpatialiteVersion.UNTIL_2_4_0; // no 'srs_wkt' or 'srtext' fields
                        break;
                    case SRS_WKT__2_4_0_to_3_1_0:
                        spatialiteVersion = SpatialiteVersion.UNTIL_3_1_0_RC2; // 'srs_wkt'
                        break;
                    case SRS_WKT__FROM_4_0_0:
                        spatialiteVersion = SpatialiteVersion.AFTER_4_0_0_RC1; // 'srtext'
                        break;
                }
            }
        }
        return spatialiteVersion;
    }


    /**
     * Get the version of Rasterlite2 with cpu-type.
     * - used by: mapsforge.mapsdirmanager.sourcesview.SourcesTreeListActivity
     * -- to prevent RaterLite2 button being shown when empty
     * note: this is returning the version number of the first static lib being compilrd into it
     * - 2014-05-22: libpng 1.6.10
     *
     * @param database the db to use.
     * @return the version of Spatialite.
     * @throws Exception if something goes wrong.
     */
    public static String getRaster2Version(Database database) throws Exception {
        Stmt stmt = database.prepare("SELECT RL2_Version();");
        try {
            if (stmt.step()) {
                String value = stmt.column_string(0);
                if (SPL_Rasterlite.Rasterlite2Version_CPU.equals("")) {
                    SPL_Rasterlite.Rasterlite2Version_CPU = value;
                }
                return value;
            }
        } finally {
            stmt.close();
        }
        return "";
    }


    /**
     * Get the properties of Spatialite.
     * <p/>
     * <br>- use the known 'SELECT Has..' functions
     * <br>- when HasIconv=0: no VirtualShapes,VirtualXL
     *
     * @param database the db to use.
     * @return the properties of Spatialite.
     * @throws Exception if something goes wrong.
     */
    public static String getSpatialiteProperties(Database database) throws Exception {
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
                if (stmt.column_int(0) == 1)
                    SPL_Geopackage.hasGeoPackage = true;
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
     * @throws Exception if something goes wrong.
     */
    public static String getProj4Version(Database database) throws Exception {
        Stmt stmt = database.prepare("SELECT proj4_version();");
        try {
            if (stmt.step()) {
                return stmt.column_string(0);
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
     * @throws Exception if something goes wrong.
     */
    public static String getGeosVersion(Database database) throws Exception {
        Stmt stmt = database.prepare("SELECT geos_version();");
        try {
            if (stmt.step()) {
                return stmt.column_string(0);
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
     *
     * @param database     the db to use.
     * @param table_name   the table of the db to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return count of Triggers found
     * @throws Exception if something goes wrong.
     */
    public static int spatialiteCountTriggers(Database database, String table_name, SpatialiteDatabaseType databaseType)
            throws Exception {
        int i_count = 0;
        if (table_name.equals(""))
            return i_count;
        String s_CountTriggers = "SELECT count(name) FROM sqlite_master WHERE (type = 'trigger' AND tbl_name= '" + table_name
                + "');";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CountTriggers);
            if (statement.step()) {
                i_count = statement.column_int(0);
                return i_count;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteCountTriggers[" + databaseType + "] sql[" + s_CountTriggers + "] db["
                    + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_count;
    }

}
