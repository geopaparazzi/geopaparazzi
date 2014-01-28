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
import android.util.Log;
import eu.geopaparazzi.library.database.ADbHelper;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;

/**
 * The database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DatabaseManager {

    /**
     * The db version.
     */
    public static final int DATABASE_VERSION = 7;

    private static final String DEBUG_TAG = "DATABASEMANAGER";

    /**
    * Buffer for bounds expansion.
    */
    public static final float BUFFER = 0.001f;

    private static DatabaseManager dbManager = null;

    private DatabaseManager() {
    }

    private DatabaseOpenHelper databaseHelper;

    /**
     * Singleton access.
     * 
     * @return the {@link DatabaseManager}.
     */
    public static DatabaseManager getInstance() {
        if (dbManager == null) {
            dbManager = new DatabaseManager();
        }
        return dbManager;
    }

    /**
     * @return the db.
     * @throws IOException  if something goes wrong.
     */
    public SQLiteDatabase getDatabase() throws IOException {
        Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        File databaseFile;
        try {
            databaseFile = ResourcesManager.getInstance(context).getDatabaseFile();
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }
        if (databaseHelper == null || !databaseFile.exists()) {

            databaseHelper = new DatabaseOpenHelper(databaseFile);

            SQLiteDatabase db = databaseHelper.getWritableDatabase(context);
            if (GPLog.LOG_ANDROID) {
                Log.i(DEBUG_TAG, "Database: " + db.getPath());
                Log.i(DEBUG_TAG, "Database Version: " + db.getVersion());
                Log.i(DEBUG_TAG, "Database Page Size: " + db.getPageSize());
                Log.i(DEBUG_TAG, "Database Max Size: " + db.getMaximumSize());
                Log.i(DEBUG_TAG, "Database Open?  " + db.isOpen());
                Log.i(DEBUG_TAG, "Database readonly?  " + db.isReadOnly());
                Log.i(DEBUG_TAG, "Database Locked by current thread?  " + db.isDbLockedByCurrentThread());
            }
        }

        return databaseHelper.getWritableDatabase(context);
    }

    /**
     * Close the database.
     */
    public void closeDatabase() {
        if (databaseHelper != null) {
            if (Debug.D)
                Log.i(DEBUG_TAG, "Closing database");
            databaseHelper.close();
            if (Debug.D)
                Log.i(DEBUG_TAG, "Database closed");
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
                    Log.i("SQLiteHelper", "Opening database at " + databaseFile);
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                ADbHelper.getInstance().setDatabase(db);
                int dbVersion = db.getVersion();
                if (DATABASE_VERSION > dbVersion)
                    upgrade(DATABASE_VERSION, dbVersion, context);
            } else {
                if (Debug.D) {
                    Log.i("SQLiteHelper", "Creating database at " + databaseFile);
                    Log.i("SQLiteHelper", "db folder exists: " + databaseFile.getParentFile().exists());
                    Log.i("SQLiteHelper", "db folder is writable: " + databaseFile.getParentFile().canWrite());
                }
                db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
                ADbHelper.getInstance().setDatabase(db);
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
         * @param context  the context to use.
         * @throws IOException  if something goes wrong.
         */
        public void create( Context context ) throws IOException {
            db.setLocale(Locale.getDefault());
            db.setLockingEnabled(false);
            db.setVersion(DATABASE_VERSION);

            // CREATE TABLES
            GPLog.createTables(db);
            DaoNotes.createTables();
            DaoGpsLog.createTables();
            DaoBookmarks.createTables();
            DaoImages.createTables();
        }

        /**
         * Upgrade the db if necessary.
         * 
         * @param newDbVersion the new db version.
         * @param oldDbVersion the old db version.
         * @param context  the context to use.
         * @throws IOException  if something goes wrong.
         */
        public void upgrade( int newDbVersion, int oldDbVersion, Context context ) throws IOException {
            if (oldDbVersion == 1) {
                DaoNotes.upgradeNotesFromDB1ToDB2(db);
            }
            if (oldDbVersion <= 2) {
                DaoBookmarks.createTables();
            }
            if (oldDbVersion <= 3) {
                DaoImages.createTables();
            }
            if (oldDbVersion <= 4) {
                DaoNotes.upgradeNotesFromDB4ToDB5(db);
            }
            if (oldDbVersion <= 5) {
                DaoNotes.upgradeNotesFromDB5ToDB6(db);
            }
            if (oldDbVersion <= 6) {
                GPLog.createTables(db);
            }
            db.beginTransaction();
            try {
                db.setTransactionSuccessful();
                db.setVersion(newDbVersion);
            } catch (Exception e) {
                Log.e("DATABASEMANAGER", e.getLocalizedMessage(), e);
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
