/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * Database helper methods.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DatabaseUtilities {

    /**
     * Set a new database to be used by the application.
     * <p>
     * <p>The old database is disconnected and a new connection is created.</p>
     *
     * @param context            the context to use.
     * @param gpApplication      the GPApplication to use.
     * @param databasePathToLoad the path to the new database to use.
     * @throws IOException
     */
    public static void setNewDatabase(Context context, GPApplication gpApplication, String databasePathToLoad) throws IOException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_DATABASE_TO_LOAD, databasePathToLoad);
        editor.apply();
        gpApplication.closeDatabase();
        ResourcesManager.resetManager();
        gpApplication.getDatabase();
    }
}
