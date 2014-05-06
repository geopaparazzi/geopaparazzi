/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2013  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.library;

import java.io.IOException;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Application extension for Geopaparazzi.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class GPApplication extends Application {
    private static GPApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        if (GPLog.LOG_ANDROID) {
            Log.i(getClass().getSimpleName(), "GPApplication singleton created."); //$NON-NLS-1$
        }
    }

    /**
     * Getter for the database.
     * 
     * <p>In this method the database connection should be created.
     * 
     * @return the database connection.
     * @throws IOException if something goes wrong.
     */
    public abstract SQLiteDatabase getDatabase() throws IOException;

    /**
     * Closes the database.
     */
    public abstract void closeDatabase();

    /**
     * @return the singleton instance.
     */
    public static GPApplication getInstance() {
        return instance;
    }
}
