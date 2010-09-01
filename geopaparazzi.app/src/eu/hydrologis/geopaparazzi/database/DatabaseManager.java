/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.geopaparazzi.database;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;

/**
 * The database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DatabaseManager {
    public static final String DEBUG_TAG = "DATABASEMANAGER";

    public static final String DATABASE_NAME = "geopaparazzi.db";

    public static final float BUFFER = 0.001f;

    // TABLE NAMES

    private static final int VERSION = 1;

    private static DatabaseManager dbManager = null;

    private SQLiteDatabase sqliteDatabase;

    public static DatabaseManager getInstance() {
        if (dbManager == null) {
            dbManager = new DatabaseManager();
        }
        return dbManager;
    }

    public SQLiteDatabase getDatabase() throws IOException {
        if (sqliteDatabase == null) {
            File databaseFile = ApplicationManager.getInstance().getDatabaseFile();
            boolean doNew = false;
            // SharedPreferences preferences = GeoPaparazziActivity.preferences;
            // String newDbKey =
            // ApplicationManager.getInstance().getResource().getString(R.string.database_new);
            // doNew = preferences.getBoolean(newDbKey, false);
            // if (doNew) {
            // // need to backup the database file
            // String nameWithoutExtention = FileUtils.getNameWithoutExtention(databaseFile);
            // String backupFilePath = nameWithoutExtention + "_bkp_"
            // + Constants.TIMESTAMPFORMATTER.format(new java.util.Date()) + ".db";
            // File backupFile = new File(backupFilePath);
            // if (databaseFile.exists()) {
            // boolean renameTo = databaseFile.renameTo(backupFile);
            // if (!renameTo) {
            // throw new IOException("An error occurred while renaming the database.");
            // }
            // }
            //
            // }

            if (!databaseFile.exists()) {
                doNew = true;
            }
            sqliteDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
            Log.i(DEBUG_TAG, "Database: " + sqliteDatabase.getPath());
            Log.i(DEBUG_TAG, "Database Version: " + sqliteDatabase.getVersion());
            Log.i(DEBUG_TAG, "Database Page Size: " + sqliteDatabase.getPageSize());
            Log.i(DEBUG_TAG, "Database Max Size: " + sqliteDatabase.getMaximumSize());
            Log.i(DEBUG_TAG, "Database Open?  " + sqliteDatabase.isOpen());
            Log.i(DEBUG_TAG, "Database readonly?  " + sqliteDatabase.isReadOnly());
            Log.i(DEBUG_TAG, "Database Locked by current thread?  " + sqliteDatabase.isDbLockedByCurrentThread());
            if (doNew) {

                sqliteDatabase.setLocale(Locale.getDefault());
                sqliteDatabase.setLockingEnabled(false);
                sqliteDatabase.setVersion(VERSION);

                // CREATE TABLES
                try {
                    DaoNotes.createTables();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    DaoGpsLog.createTables();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    DaoMaps.createTables();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        return sqliteDatabase;
    }

    public void closeDatabase() {
        if (sqliteDatabase != null) {
            Log.i(DEBUG_TAG, "Closing database");
            sqliteDatabase.close();
            Log.i(DEBUG_TAG, "Database closed");
        }
    }

}
