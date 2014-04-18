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
package eu.geopaparazzi.library.database;

import java.io.IOException;

import android.database.sqlite.SQLiteDatabase;

/**
 * Db helper class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public enum ADbHelper {
    /**
     * The singleton instance.
     */
    INSTANCE;

    private SQLiteDatabase db = null;

    /**
     * @return the db.
     * @throws IOException  if something goes wrong.
     */
    public SQLiteDatabase getDatabase() throws IOException {
        return db;
    }

    /**
     * @param db the db to set.
     */
    public void setDatabase( SQLiteDatabase db ) {
        if (db == null) {
            GPLog.error(this, "Null database passed to ADbHelper.", new NullPointerException()); //$NON-NLS-1$
        } else {
            GPLog.addLogEntry(this, "Setting database in ADbHelper."); //$NON-NLS-1$
        }
        this.db = db;
    }

}
