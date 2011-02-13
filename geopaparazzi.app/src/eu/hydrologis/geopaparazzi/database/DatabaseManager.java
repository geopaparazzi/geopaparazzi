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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DatabaseManager {

    public static final int DATABASE_VERSION = 2;

    public static final String DEBUG_TAG = "DATABASEMANAGER";

    public static final String DATABASE_NAME = "geopaparazzi.db";

    public static final float BUFFER = 0.001f;

    // TABLE NAMES

    private static DatabaseManager dbManager = null;

    private DatabaseOpenHelper databaseHelper;

    public static DatabaseManager getInstance() {
        if (dbManager == null) {
            dbManager = new DatabaseManager();
        }
        return dbManager;
    }

    public SQLiteDatabase getDatabase( Context context ) throws IOException {
        if (databaseHelper == null) {
            // SharedPreferences preferences = GeoPaparazziActivity.preferences;
            // String newDbKey =
            // ApplicationManager.getInstance(getContext()).getResource().getString(R.string.database_new);
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
            File databaseFile = ApplicationManager.getInstance(context).getDatabaseFile();

            databaseHelper = new DatabaseOpenHelper(databaseFile);

            SQLiteDatabase db = databaseHelper.getWritableDatabase(context);
            Logger.i(DEBUG_TAG, "Database: " + db.getPath());
            Logger.i(DEBUG_TAG, "Database Version: " + db.getVersion());
            Logger.i(DEBUG_TAG, "Database Page Size: " + db.getPageSize());
            Logger.i(DEBUG_TAG, "Database Max Size: " + db.getMaximumSize());
            Logger.i(DEBUG_TAG, "Database Open?  " + db.isOpen());
            Logger.i(DEBUG_TAG, "Database readonly?  " + db.isReadOnly());
            Logger.i(DEBUG_TAG, "Database Locked by current thread?  " + db.isDbLockedByCurrentThread());
        }

        return databaseHelper.getWritableDatabase(context);
    }

    public void closeDatabase() {
        if (databaseHelper != null) {
            Logger.i(DEBUG_TAG, "Closing database");
            databaseHelper.close();
            Logger.i(DEBUG_TAG, "Database closed");
        }
    }

    private static class DatabaseOpenHelper {
        private SQLiteDatabase db;

        private File databaseFile;

        private DatabaseOpenHelper( File databaseFile ) {
            this.databaseFile = databaseFile;
        }

        public void open( Context context ) throws IOException {
            if (databaseFile.exists()) {
                Logger.i("SQLiteHelper", "Opening database at " + databaseFile);
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                int dbVersion = db.getVersion();
                if (DATABASE_VERSION > dbVersion)
                    upgrade(DATABASE_VERSION, dbVersion);
            } else {
                Logger.i("SQLiteHelper", "Creating database at " + databaseFile);
                Logger.d(dbManager, "db folder exists: " + databaseFile.getParentFile().exists());
                Logger.d(dbManager, "db folder is writable: " + databaseFile.getParentFile().canWrite());
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                create(context);
            }
        }

        public void close() {
            if (!db.isOpen()) {
                return;
            }
            db.close();
            db = null;
        }

        public void create( Context context ) throws IOException {
            db.setLocale(Locale.getDefault());
            db.setLockingEnabled(false);
            db.setVersion(DATABASE_VERSION);

            // CREATE TABLES
            DaoNotes.createTables(context);
            DaoGpsLog.createTables(context);
            DaoMaps.createTables(context);
        }

        public void upgrade( int newDbVersion, int oldDbVersion ) throws IOException {
            if (newDbVersion == 2 && oldDbVersion == 1) {
                DaoNotes.upgradeNotesFromDB1ToDB2(db);
            } else {
                throw new RuntimeException("Method not implemented.");
            }
        }

        public SQLiteDatabase getWritableDatabase( Context context ) throws IOException {
            if (db == null)
                open(context);
            return db;
        }
    }

}
