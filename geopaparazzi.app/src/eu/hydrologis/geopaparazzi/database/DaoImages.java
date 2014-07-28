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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.util.Image;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoImages {

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_LON = "lon";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_ALTIM = "altim";
    private static final String COLUMN_AZIM = "azim";
    private static final String COLUMN_PATH = "path";
    private static final String COLUMN_TS = "ts";
    private static final String COLUMN_TEXT = "text";

    /**
     * Image table name.
     */
    public static final String TABLE_IMAGES = "images";

    private static long LASTINSERTEDIMAGE_ID = -1;

    private static SimpleDateFormat dateFormatter = TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC;

    /**
     * Add an image to the db.
     * 
     * @param lon lon
     * @param lat lat
     * @param altim elevation
     * @param azim azimuth
     * @param timestamp the timestamp
     * @param text a text
     * @param path the image path.
     * @throws IOException  if something goes wrong.
     */
    public static void addImage( double lon, double lat, double altim, double azim, Date timestamp, String text, String path )
            throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_LON, lon);
            values.put(COLUMN_LAT, lat);
            values.put(COLUMN_ALTIM, altim);
            values.put(COLUMN_TS, dateFormatter.format(timestamp));
            values.put(COLUMN_TEXT, text);
            values.put(COLUMN_PATH, path);
            values.put(COLUMN_AZIM, azim);
            LASTINSERTEDIMAGE_ID = sqliteDatabase.insertOrThrow(TABLE_IMAGES, null, values);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Deletes the image reference without actually deleting the image from disk.
     * 
     * @param id the id of the image note.
     * @throws IOException  if something goes wrong.
     */
    public static void deleteImage( long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // delete note
            String query = "delete from " + TABLE_IMAGES + " where " + COLUMN_ID + " = " + id;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Removes last image.
     * 
     * @throws IOException  if something goes wrong.
     */
    public static void deleteLastInsertedImage() throws IOException {
        if (LASTINSERTEDIMAGE_ID != -1) {
            deleteImage(LASTINSERTEDIMAGE_ID);
        }
    }

    /**
     * Get the collected notes from the database inside a given bound.
     * @param n north
     * @param s south
     * @param w west
     * @param e east
     * 
     * @return the list of notes inside the bounds.
     * @throws IOException  if something goes wrong.
     */
    public static List<Image> getImagesInWorldBounds( float n, float s, float w, float e ) throws IOException {

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        String query = "SELECT _id, lon, lat, altim, azim, path, text, ts FROM XXX WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
        // String[] args = new String[]{TABLE_NOTES, String.valueOf(w), String.valueOf(e),
        // String.valueOf(s), String.valueOf(n)};

        query = query.replaceFirst("XXX", TABLE_IMAGES);
        query = query.replaceFirst("XXX", String.valueOf(w));
        query = query.replaceFirst("XXX", String.valueOf(e));
        query = query.replaceFirst("XXX", String.valueOf(s));
        query = query.replaceFirst("XXX", String.valueOf(n));

        // if (Debug.D) Logger.i("DAOIMAGES", "Query: " + query);

        Cursor c = sqliteDatabase.rawQuery(query, null);
        List<Image> images = new ArrayList<Image>();
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            long id = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            double altim = c.getDouble(3);
            double azim = c.getDouble(4);
            String path = c.getString(5);
            String text = c.getString(6);
            String date = c.getString(7);

            Image image = new Image(id, text, lon, lat, altim, azim, path, date);
            images.add(image);
            c.moveToNext();
        }
        c.close();
        return images;
    }

    /**
     * Get the list of notes from the db.
     * 
     * @return list of notes.
     * @throws IOException  if something goes wrong.
     */
    public static List<Image> getImagesList() throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<Image> images = new ArrayList<Image>();
        String asColumnsToReturn[] = {COLUMN_ID, COLUMN_LON, COLUMN_LAT, COLUMN_ALTIM, COLUMN_AZIM, COLUMN_PATH, COLUMN_TS,
                COLUMN_TEXT};
        String strSortOrder = "_id ASC";
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, null, null, null, null, strSortOrder);
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            long id = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            double altim = c.getDouble(3);
            double azim = c.getDouble(4);
            String path = c.getString(5);
            String date = c.getString(6);
            String text = c.getString(7);

            Image image = new Image(id, text, lon, lat, altim, azim, path, date);
            images.add(image);
            c.moveToNext();
        }
        c.close();
        return images;
    }

    /**
     * Get all image overlays.
     * 
     * @param marker the marker to use.
     * @return the list of {@link OverlayItem}s.
     * @throws IOException  if something goes wrong.
     */
    public static List<OverlayItem> getImagesOverlayList( Drawable marker ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<OverlayItem> images = new ArrayList<OverlayItem>();
        String asColumnsToReturn[] = {COLUMN_LON, COLUMN_LAT, COLUMN_PATH, COLUMN_TEXT};
        String strSortOrder = "_id ASC";
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, null, null, null, null, strSortOrder);
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            String path = c.getString(2);
            String text = c.getString(3);

            OverlayItem image = new OverlayItem(new GeoPoint(lat, lon), path, text, marker);
            images.add(image);
            c.moveToNext();
        }
        c.close();
        return images;
    }

    /**
     * Create the image tables.
     * 
     * @throws IOException  if something goes wrong.
     */
    public static void createTables() throws IOException {
        StringBuilder sB = new StringBuilder();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_IMAGES);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_LON).append(" REAL NOT NULL, ");
        sB.append(COLUMN_LAT).append(" REAL NOT NULL,");
        sB.append(COLUMN_ALTIM).append(" REAL NOT NULL,");
        sB.append(COLUMN_AZIM).append(" REAL NOT NULL,");
        sB.append(COLUMN_PATH).append(" TEXT NOT NULL,");
        sB.append(COLUMN_TS).append(" DATE NOT NULL,");
        sB.append(COLUMN_TEXT).append(" TEXT NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGES = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_ts_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(COLUMN_TS);
        sB.append(" );");
        String CREATE_INDEX_IMAGES_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_x_by_y_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(COLUMN_LON);
        sB.append(", ");
        sB.append(COLUMN_LAT);
        sB.append(" );");
        String CREATE_INDEX_IMAGES_X_BY_Y = sB.toString();

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        if (GPLog.LOG_HEAVY)
            Log.i("DAOIMAGES", "Create the images table.");

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE_IMAGES);
            sqliteDatabase.execSQL(CREATE_INDEX_IMAGES_TS);
            sqliteDatabase.execSQL(CREATE_INDEX_IMAGES_X_BY_Y);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

}
