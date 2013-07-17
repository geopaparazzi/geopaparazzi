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

public class ADbHelper {
    private static ADbHelper dbHelper = null;
    private SQLiteDatabase db = null;

    private ADbHelper() {
    }

    public static ADbHelper getInstance() {
        if (dbHelper == null) {
            dbHelper = new ADbHelper();
        }
        return dbHelper;
    }

    public SQLiteDatabase getDatabase() throws IOException {
        return db;
    }

    public void setDatabase( SQLiteDatabase db ) {
        this.db = db;
    }

    
}
