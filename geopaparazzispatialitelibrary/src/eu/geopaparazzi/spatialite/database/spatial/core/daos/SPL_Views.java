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
import jsqlite.Stmt;

/**
 * Created by hydrologis on 18/07/14.
 */
public class SPL_Views {
    /**
     * Get the Primary key of the SpatialView and read_only parameter.
     * - check if writable Views has at least 3 triggers, set to read only if not
     *
     * @param database     the db to use.
     * @param table_name   the view of the db to use.
     * @param databaseType SPATIALITE4 for version of spatialite that have a 'read_only' field.
     * @return formatted version for map.key
     * @throws jsqlite.Exception if something goes wrong.
     */
    public static String getViewRowid(Database database, String table_name, SpatialiteDatabaseType databaseType)
            throws jsqlite.Exception {
        String s_sql = "SELECT view_rowid,read_only FROM views_geometry_columns WHERE (view_name='" + table_name + "')";
        if (databaseType == SpatialiteDatabaseType.SPATIALITE3)
            s_sql = "SELECT view_rowid FROM views_geometry_columns WHERE (view_name='" + table_name + "')";
        String ROWID_PK = "";
        Stmt statement = null;
        try {
            statement = database.prepare(s_sql);
            if ((statement != null) && (statement.column_count()) > 0) {
                if (statement.step()) {
                    ROWID_PK = statement.column_string(0);
                    int i_read_only = 0;
                    if ((databaseType == SpatialiteDatabaseType.SPATIALITE4) && (statement.column_count() > 1)) {
                        i_read_only = statement.column_int(1);
                        // it is not possible to check the validity of the triggers
                        if (i_read_only == 1) {
                            // there must be at least 3 triggers, the view CANNOT be writable
                            if (DatabaseCreationAndProperties.spatialiteCountTriggers(database, table_name, databaseType) < 3) {
                                i_read_only = 0;
                            }
                        }
                    }
                    ROWID_PK = ROWID_PK + ";" + i_read_only;
                }
            }
        } catch (jsqlite.Exception e_stmt) {
            GPLog.error("DAOSPATIALIE", "getViewRowid[" + databaseType + "] sql[" + s_sql + "] db[" + database.getFilename() + "]",
                    e_stmt);
        } finally {
            if (statement != null)
                statement.close();
        }
        return ROWID_PK;
    }
}
