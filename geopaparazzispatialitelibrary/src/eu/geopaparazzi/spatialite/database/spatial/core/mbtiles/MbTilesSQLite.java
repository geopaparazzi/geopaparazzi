package eu.geopaparazzi.spatialite.database.spatial.core.mbtiles;

/** @author Simon Thépot aka djcoin <simon.thepot@gmail.com, simon.thepot@makina-corpus.com> */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

// sqlite3 <your_sqlite_db>
// private final static String ANDROID_REQUIREMENT_TABLE = "CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT DEFAULT 'en_US');";
// private final static String ANDROID_REQUIREMENT_INSERT = "INSERT INTO android_metadata VALUES ('en_US');";

public class MbTilesSQLite extends SQLiteOpenHelper {

    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    public final static String TABLE_TILES = "tiles";
    public final static String COL_TILES_ZOOM_LEVEL = "zoom_level";
    public final static String COL_TILES_TILE_COLUMN = "tile_column";
    public final static String COL_TILES_TILE_ROW = "tile_row";
    public final static String COL_TILES_TILE_DATA = "tile_data";

    private final static String CREATE_TILES = //
    "CREATE TABLE " + TABLE_TILES + "( " + //
            COL_TILES_ZOOM_LEVEL + " INTEGER, " + //
            COL_TILES_TILE_COLUMN + " INTEGER, " + //
            COL_TILES_TILE_ROW + " INTEGER, " + //
            COL_TILES_TILE_DATA + " BLOB" + //
            ")";

    // TABLE METADATA (name TEXT, value TEXT);
    public final static String TABLE_METADATA = "metadata";
    public final static String COL_METADATA_NAME = "name";
    public final static String COL_METADATA_VALUE = "value";

    private final static String CREATE_METADATA = //
    "CREATE TABLE" + TABLE_METADATA + "( " + //
            COL_METADATA_NAME + " TEXT, " + //
            COL_METADATA_VALUE + " TEXT " + //
            ")";

    // INDEXES on Metadata and Tiles tables
    private final static String INDEX_TILES = "CREATE UNIQUE INDEX tile_index ON " + TABLE_TILES + " (" + COL_TILES_ZOOM_LEVEL
            + ", " + COL_TILES_TILE_COLUMN + ", " + COL_TILES_TILE_ROW + ")";
    private final static String INDEX_METADATA = "CREATE UNIQUE INDEX name ON " + TABLE_METADATA + "( " + COL_METADATA_NAME + ")";

    public MbTilesSQLite( Context context, String name, CursorFactory factory, int version ) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate( SQLiteDatabase db ) {
        db.execSQL(CREATE_TILES);
        db.execSQL(CREATE_METADATA);
        db.execSQL(INDEX_TILES);
        db.execSQL(INDEX_METADATA);
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_METADATA);
        onCreate(db);
    }

}
