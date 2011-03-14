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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoBookmarks {

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_LON = "lon";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_TEXT = "text";

    public static final String TABLE_BOOKMARKS = "bookmarks";

    public static void addBookmark( Context context, double lon, double lat, String text ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_LON, lon);
            values.put(COLUMN_LAT, lat);
            values.put(COLUMN_TEXT, text);
            sqliteDatabase.insertOrThrow(TABLE_BOOKMARKS, null, values);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOBOOKMARKS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void deleteBookmark( Context context, long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {
            // delete note
            String query = "delete from " + TABLE_BOOKMARKS + " where " + COLUMN_ID + " = " + id;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOBOOKMARKS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Get the collected notes from the database inside a given bound.
     * 
     * @param n
     * @param s
     * @param w
     * @param e
     * @return the list of notes inside the bounds.
     * @throws IOException
     */
    public static List<Bookmark> getBookmarksInWorldBounds( Context context, float n, float s, float w, float e )
            throws IOException {

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        String query = "SELECT lon, lat, text FROM XXX WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
        // String[] args = new String[]{TABLE_NOTES, String.valueOf(w), String.valueOf(e),
        // String.valueOf(s), String.valueOf(n)};

        query = query.replaceFirst("XXX", TABLE_BOOKMARKS);
        query = query.replaceFirst("XXX", String.valueOf(w));
        query = query.replaceFirst("XXX", String.valueOf(e));
        query = query.replaceFirst("XXX", String.valueOf(s));
        query = query.replaceFirst("XXX", String.valueOf(n));

        Logger.i("DAOBOOKMARKS", "Query: " + query);

        Cursor c = sqliteDatabase.rawQuery(query, null);
        List<Bookmark> bookmarks = new ArrayList<Bookmark>();
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            String text = c.getString(2);

            Bookmark note = new Bookmark(text, lon, lat);
            bookmarks.add(note);
            c.moveToNext();
        }
        c.close();
        return bookmarks;
    }

    public static void createTables( Context context ) throws IOException {
        StringBuilder sB = new StringBuilder();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_BOOKMARKS);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_LON).append(" REAL NOT NULL, ");
        sB.append(COLUMN_LAT).append(" REAL NOT NULL,");
        sB.append(COLUMN_TEXT).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_BOOKMARKS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX bookmarks_x_by_y_idx ON ");
        sB.append(TABLE_BOOKMARKS);
        sB.append(" ( ");
        sB.append(COLUMN_LON);
        sB.append(", ");
        sB.append(COLUMN_LAT);
        sB.append(" );");
        String CREATE_INDEX_BOOKMARKS_X_BY_Y = sB.toString();

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        Logger.i("DAOBOOKMARKS", "Create the bookmarks table.");

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE_BOOKMARKS);
            sqliteDatabase.execSQL(CREATE_INDEX_BOOKMARKS_X_BY_Y);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOBOOKMARKS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

}
