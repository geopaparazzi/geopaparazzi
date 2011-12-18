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
import eu.hydrologis.geopaparazzi.util.debug.Debug;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DatabaseManager {

    public static final int DATABASE_VERSION = 5;

    public static final String DEBUG_TAG = "DATABASEMANAGER";

    public static final String DATABASE_NAME = "geopaparazzi.db";

    public static final float BUFFER = 0.001f;

    private static DatabaseManager dbManager = null;

    private DatabaseManager() {
    }

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
            if (Debug.D) {
                Logger.i(DEBUG_TAG, "Database: " + db.getPath());
                Logger.i(DEBUG_TAG, "Database Version: " + db.getVersion());
                Logger.i(DEBUG_TAG, "Database Page Size: " + db.getPageSize());
                Logger.i(DEBUG_TAG, "Database Max Size: " + db.getMaximumSize());
                Logger.i(DEBUG_TAG, "Database Open?  " + db.isOpen());
                Logger.i(DEBUG_TAG, "Database readonly?  " + db.isReadOnly());
                Logger.i(DEBUG_TAG, "Database Locked by current thread?  " + db.isDbLockedByCurrentThread());
            }
        }

        return databaseHelper.getWritableDatabase(context);
    }

    public void closeDatabase() {
        if (databaseHelper != null) {
            if (Debug.D)
                Logger.i(DEBUG_TAG, "Closing database");
            databaseHelper.close();
            if (Debug.D)
                Logger.i(DEBUG_TAG, "Database closed");
            databaseHelper = null;
            dbManager = null;
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
                if (Debug.D)
                    Logger.i("SQLiteHelper", "Opening database at " + databaseFile);
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                int dbVersion = db.getVersion();
                if (DATABASE_VERSION > dbVersion)
                    upgrade(DATABASE_VERSION, dbVersion, context);
            } else {
                if (Debug.D) {
                    Logger.i("SQLiteHelper", "Creating database at " + databaseFile);
                    Logger.d(dbManager, "db folder exists: " + databaseFile.getParentFile().exists());
                    Logger.d(dbManager, "db folder is writable: " + databaseFile.getParentFile().canWrite());
                }
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

        /**
         * Create the db from scratch.
         * 
         * @param context
         * @throws IOException
         */
        public void create( Context context ) throws IOException {
            db.setLocale(Locale.getDefault());
            db.setLockingEnabled(false);
            db.setVersion(DATABASE_VERSION);

            // CREATE TABLES
            DaoNotes.createTables(context);
            DaoGpsLog.createTables(context);
            DaoMaps.createTables(context);
            DaoBookmarks.createTables(context);
            DaoImages.createTables(context);
        }

        /**
         * Upgrade the db if necessary.
         * 
         * @param newDbVersion
         * @param oldDbVersion
         * @param context
         * @throws IOException
         */
        public void upgrade( int newDbVersion, int oldDbVersion, Context context ) throws IOException {
            if (oldDbVersion == 1) {
                DaoNotes.upgradeNotesFromDB1ToDB2(db);
            }
            if (oldDbVersion <= 2) {
                DaoBookmarks.createTables(context);
            }
            if (oldDbVersion <= 3) {
                DaoImages.createTables(context);
            }
            if (oldDbVersion <= 4) {
                DaoNotes.upgradeNotesFromDB4ToDB5(db);
            }
            db.beginTransaction();
            try {
                db.setTransactionSuccessful();
                db.setVersion(newDbVersion);
            } catch (Exception e) {
                Logger.e(this, e.getLocalizedMessage(), e);
                throw new IOException(e.getLocalizedMessage());
            } finally {
                db.endTransaction();
            }
        }


        public SQLiteDatabase getWritableDatabase( Context context ) throws IOException {
            if (db == null)
                open(context);
            return db;
        }
    }

}
