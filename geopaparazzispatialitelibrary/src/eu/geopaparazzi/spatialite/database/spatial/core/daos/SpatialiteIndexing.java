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

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialiteDatabaseType;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * SpatialiteIndexing related dao.
 *
 * @author Mark Johnson.
 */
public class SpatialiteIndexing {

    /**
     * Attemt to create GeoPackage-SpatialIndex for this geometry field.
     * returned if the SpatialIndex was created (and therefore useable) or not
     * - This should NOT be the default behavior, there may be a reason why no SpatialIndex was created
     *
     * @param database        the db to use.
     * @param table_name      the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param databaseType    for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws jsqlite.Exception if something goes wrong.
     */
    private static int spatialitegpkgAddSpatialIndex(Database database, String table_name, String geometry_column,
                                                     SpatialiteDatabaseType databaseType) throws jsqlite.Exception {
        int i_spatialindex = 0;
        if ((table_name.equals("")) || (geometry_column.equals("")))
            return i_spatialindex;
        // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_CreateSpatialIndex = "SELECT gpkgAddSpatialIndex('" + table_name + "','" + geometry_column + "');";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CreateSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                return i_spatialindex;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "gpkgAddSpatialIndex[" + databaseType + "] sql[" + s_CreateSpatialIndex + "] db["
                    + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Attemt to create SpatialIndex for this geometry field.
     * returned if the SpatialIndex was created (and therefore useable) or not
     * - This should NOT be the default behavior, there may be a reason why no SpatialIndex was created
     *
     * @param database        the db to use.
     * @param table_name      the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param databaseType    for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static int spatialiteCreateSpatialIndex(Database database, String table_name, String geometry_column,
                                                    SpatialiteDatabaseType databaseType) throws Exception {
        int i_spatialindex = 0;
        if ((table_name.equals("")) || (geometry_column.equals("")))
            return i_spatialindex;
        // SELECT CreateSpatialIndex('prov2008_s','Geometry');
        String s_CreateSpatialIndex = "SELECT CreateSpatialIndex('" + table_name + "','" + geometry_column + "');";
        Stmt statement = null;
        try {
            statement = database.prepare(s_CreateSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                return i_spatialindex;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteCreateSpatialIndex[" + databaseType + "] sql[" + s_CreateSpatialIndex
                    + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Create Virtual-table 'SpatialIndex' if it does not exist.
     * - Note: only needed for pre-spatiallite 3.0 Databases.
     *
     * @param database     the db to use.
     * @param databaseType for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception if something goes wrong.
     */
    public static int spatialiteVirtualSpatialIndex(Database database, SpatialiteDatabaseType databaseType) throws Exception {
        String s_VirtualSpatialIndex = "CREATE VIRTUAL TABLE SpatialIndex USING VirtualSpatialIndex();";
        int i_spatialindex = 0;
        Stmt statement = null;
        try {
            database.exec(s_VirtualSpatialIndex, null);
            s_VirtualSpatialIndex = "SELECT count(*) FROM sqlite_master WHERE name = 'SpatialIndex';";
            statement = database.prepare(s_VirtualSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                return i_spatialindex;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteVirtualSpatialIndex[" + databaseType + "] sql[" + s_VirtualSpatialIndex
                    + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_spatialindex;
    }

    /**
     * Attemt to execute a RecoverSpatialIndex for this geometry field or whole Database.
     * - Note: only for AbstractSpatialTable, SpatialViews ALWAYS returns 0.
     * - if table_name and geometry_column are empty: for whole Database
     *
     * @param database        the db to use.
     * @param table_name      the table of the db to use.
     * @param geometry_column the geometry field of the table to use.
     * @param i_spatialindex  0=recover on when needed [default], 1=force rebuild.
     * @param databaseType    for Spatialite 3 and 4 specific Tasks
     * @return 0=invalid SpatialIndex ; 1=valid SpatialIndex
     * @throws Exception if something goes wrong.
     */
    public static int spatialiteRecoverSpatialIndex(Database database, String table_name, String geometry_column,
                                                     int i_spatialindex, SpatialiteDatabaseType databaseType) throws Exception {
        String s_RecoverSpatialIndex = "SELECT RecoverSpatialIndex(" + i_spatialindex + ");";
        if ((!table_name.equals("")) && (!geometry_column.equals("")))
            s_RecoverSpatialIndex = "SELECT RecoverSpatialIndex('" + table_name + "','" + geometry_column + "'," + i_spatialindex
                    + ");";
        i_spatialindex = 0;
        Stmt statement = null;
        try {
            statement = database.prepare(s_RecoverSpatialIndex);
            if (statement.step()) {
                i_spatialindex = statement.column_int(0);
                return i_spatialindex;
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "spatialiteRecoverSpatialIndex[" + databaseType + "] sql[" + s_RecoverSpatialIndex
                    + "] db[" + database.getFilename() + "]", e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return i_spatialindex;
    }

}
