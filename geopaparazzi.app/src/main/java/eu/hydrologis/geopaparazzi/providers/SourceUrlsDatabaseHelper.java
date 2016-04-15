// AddressBookDatabaseHelper.java
// SQLiteOpenHelper subclass that defines the app's database
package eu.hydrologis.geopaparazzi.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class SourceUrlsDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SourceUrls.db";
    private static final int DATABASE_VERSION = 1;

    // constructor
    public SourceUrlsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // creates the contacts table when the database is created
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL for creating the contacts table
        final String CREATE_SOURCEURLS_TABLE =
                "CREATE TABLE " + DatabaseDescription.SourceUrl.TABLE_NAME + "(" +
                        DatabaseDescription.SourceUrl._ID + " integer primary key, " +
                        DatabaseDescription.SourceUrl.COLUMN_NAME + " TEXT, " +
                        DatabaseDescription.SourceUrl.COLUMN_URL + " TEXT);";
        db.execSQL(CREATE_SOURCEURLS_TABLE); // create the contacts table
    }

    // normally defines how to upgrade the database when the schema changes
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {
    }
}

