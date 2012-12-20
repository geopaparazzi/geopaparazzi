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
