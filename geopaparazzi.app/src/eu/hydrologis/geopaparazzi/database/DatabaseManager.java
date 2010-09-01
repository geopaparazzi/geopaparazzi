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

    public static final int DATABASE_VERSION = 1;

    public static final String DEBUG_TAG = "DATABASEMANAGER";

    public static final String DATABASE_NAME = "geopaparazzi.db";

    public static final float BUFFER = 0.001f;

    // TABLE NAMES

    private static final int VERSION = 1;

    private static DatabaseManager dbManager = null;

    private DatabaseOpenHelper databaseHelper;

    public static DatabaseManager getInstance() {
        if (dbManager == null) {
            dbManager = new DatabaseManager();
        }
        return dbManager;
    }

    public SQLiteDatabase getDatabase() throws IOException {
        if (databaseHelper == null) {
            File databaseFile = ApplicationManager.getInstance().getDatabaseFile();
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

            databaseHelper = new DatabaseOpenHelper(databaseFile);

            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            Log.i(DEBUG_TAG, "Database: " + db.getPath());
            Log.i(DEBUG_TAG, "Database Version: " + db.getVersion());
            Log.i(DEBUG_TAG, "Database Page Size: " + db.getPageSize());
            Log.i(DEBUG_TAG, "Database Max Size: " + db.getMaximumSize());
            Log.i(DEBUG_TAG, "Database Open?  " + db.isOpen());
            Log.i(DEBUG_TAG, "Database readonly?  " + db.isReadOnly());
            Log.i(DEBUG_TAG, "Database Locked by current thread?  " + db.isDbLockedByCurrentThread());
        }

        return databaseHelper.getWritableDatabase();
    }

    public void closeDatabase() {
        if (databaseHelper != null) {
            Log.i(DEBUG_TAG, "Closing database");
            databaseHelper.close();
            Log.i(DEBUG_TAG, "Database closed");
        }
    }

    private static class DatabaseOpenHelper {
        private SQLiteDatabase db;

        private File databaseFile;

        private DatabaseOpenHelper( File databaseFile ) {
            this.databaseFile = databaseFile;
        }

        public void open() throws IOException {
            if (databaseFile.exists()) {
                Log.i("SQLiteHelper", "Opening database at " + databaseFile);
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                if (DATABASE_VERSION > db.getVersion())
                    upgrade();
            } else {
                Log.i("SQLiteHelper", "Creating database at " + databaseFile);
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                create();
            }
        }

        public void close() {
            db.close();
        }

        public void create() throws IOException {
            db.setLocale(Locale.getDefault());
            db.setLockingEnabled(false);
            db.setVersion(VERSION);

            // CREATE TABLES
            DaoNotes.createTables();
            DaoGpsLog.createTables();
            DaoMaps.createTables();
        }

        public void upgrade() throws IOException {
            throw new RuntimeException("Method not implemented.");
        }

        public SQLiteDatabase getWritableDatabase() throws IOException {
            if (db == null)
                open();
            return db;
        }
    }

}
