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
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.util.Image;

import static eu.hydrologis.geopaparazzi.database.TableDescriptions.*;

/**
 * Data access object for images.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoImages {

    /**
     * Create the image tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables() throws IOException {
        StringBuilder sB = new StringBuilder();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_IMAGES);
        sB.append(" (");
        sB.append(ImageTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(ImageTableFields.COLUMN_LON.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(ImageTableFields.COLUMN_LAT.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_ALTIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_AZIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_IMAGE_ID.getFieldName());
        sB.append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT " + ImageTableFields.COLUMN_IMAGE_ID.getFieldName() + " REFERENCES ");
        sB.append(TABLE_IMAGE_DATA);
        sB.append("(");
        sB.append(ImageDataTableFields.COLUMN_ID);
        sB.append(") ON DELETE CASCADE,");
        sB.append(ImageTableFields.COLUMN_TS.getFieldName()).append(" DATE NOT NULL,");
        sB.append(ImageTableFields.COLUMN_TEXT.getFieldName()).append(" TEXT NOT NULL,");
        sB.append(ImageTableFields.COLUMN_NOTE_ID.getFieldName()).append(" INTEGER,");
        sB.append(ImageTableFields.COLUMN_ISDIRTY.getFieldName()).append(" INTEGER NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGES = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_ts_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_TS.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_x_by_y_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_LON.getFieldName());
        sB.append(", ");
        sB.append(ImageTableFields.COLUMN_LAT.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_noteid_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_NOTE_ID.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_NOTEID = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_isdirty_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_ISDIRTY.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_ISDIRTY = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_IMAGE_DATA);
        sB.append(" (");
        sB.append(ImageDataTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(ImageDataTableFields.COLUMN_IMAGE.getFieldName()).append(" BLOB NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGEDATA = sB.toString();

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        if (GPLog.LOG_HEAVY)
            Log.i("DAOIMAGES", "Create the images table.");

        sqliteDatabase.beginTransaction();
        try {
            sqliteDatabase.execSQL(CREATE_TABLE_IMAGES);
            sqliteDatabase.execSQL(CREATE_INDEX_IMAGES_TS);
            sqliteDatabase.execSQL(CREATE_INDEX_IMAGES_X_BY_Y);
            sqliteDatabase.execSQL(CREATE_INDEX_IMAGES_NOTEID);
            sqliteDatabase.execSQL(CREATE_INDEX_IMAGES_ISDIRTY);

            sqliteDatabase.execSQL(CREATE_TABLE_IMAGEDATA);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Add an image to the db.
     *
     * @param lon       lon
     * @param lat       lat
     * @param altim     elevation
     * @param azim      azimuth
     * @param timestamp the timestamp
     * @param text      a text
     * @param image     the image data.
     * @param noteId    an optional note id, to which it is connected.
     * @throws IOException if something goes wrong.
     */
    public static void addImage(double lon, double lat, double altim, double azim, long timestamp, String text, byte[] image, Integer noteId)
            throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // first insert image data
            ContentValues imageDataValues = new ContentValues();
            imageDataValues.put(ImageDataTableFields.COLUMN_IMAGE.getFieldName(), image);
            long imageDataId = sqliteDatabase.insertOrThrow(TABLE_IMAGE_DATA, null, imageDataValues);

            ContentValues values = new ContentValues();
            values.put(ImageTableFields.COLUMN_LON.getFieldName(), lon);
            values.put(ImageTableFields.COLUMN_LAT.getFieldName(), lat);
            values.put(ImageTableFields.COLUMN_ALTIM.getFieldName(), altim);
            values.put(ImageTableFields.COLUMN_TS.getFieldName(), timestamp);
            values.put(ImageTableFields.COLUMN_TEXT.getFieldName(), text);
            values.put(ImageTableFields.COLUMN_IMAGE_ID.getFieldName(), imageDataId);
            values.put(ImageTableFields.COLUMN_AZIM.getFieldName(), azim);
            if (noteId != null)
                values.put(ImageTableFields.COLUMN_NOTE_ID.getFieldName(), noteId);
            sqliteDatabase.insertOrThrow(TABLE_IMAGES, null, values);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Deletes images from the db.
     *
     * @param ids the ids of the images to remove.
     * @throws IOException if something goes wrong.
     */
    public static void deleteImages(long... ids) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            String asColumnsToReturn[] = {ImageTableFields.COLUMN_IMAGE_ID.getFieldName()};
            String imageIdsWhereStr = "";
            int count = 0;
            for (long id : ids) {
                if (count > 0) {
                    imageIdsWhereStr = imageIdsWhereStr + " || ";
                }
                imageIdsWhereStr = imageIdsWhereStr + ImageTableFields.COLUMN_ID.getFieldName() + " = " + id;
                count++;
            }
            Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, imageIdsWhereStr, null, null, null, null);
            c.moveToFirst();
            String imageDataIdsWhereStr = "";
            count = 0;
            while (!c.isAfterLast()) {
                long imageDataId = c.getLong(0);
                c.moveToNext();
                if (count > 0) {
                    imageDataIdsWhereStr = imageDataIdsWhereStr + " || ";
                }
                imageDataIdsWhereStr = imageDataIdsWhereStr + ImageDataTableFields.COLUMN_ID.getFieldName() + " = " + imageDataId;
                count++;

            }
            c.close();


            // delete images
            String query = "delete from " + TABLE_IMAGES + " where " + imageIdsWhereStr;
            SQLiteStatement deleteStmt = sqliteDatabase.compileStatement(query);
            deleteStmt.execute();

            // delete images data
            query = "delete from " + TABLE_IMAGE_DATA + " where " + imageDataIdsWhereStr;
            deleteStmt = sqliteDatabase.compileStatement(query);
            deleteStmt.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Deletes images from the db for given notes.
     *
     * @param noteIds the ids of the notes for which the images should be removed.
     * @throws IOException if something goes wrong.
     */
    public static void deleteImagesForNotes(long... noteIds) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            String asColumnsToReturn[] = {ImageTableFields.COLUMN_IMAGE_ID.getFieldName()};
            String notesIdsWhereStr = "";
            int count = 0;
            for (long id : noteIds) {
                if (count > 0) {
                    notesIdsWhereStr = notesIdsWhereStr + " || ";
                }
                notesIdsWhereStr = notesIdsWhereStr + ImageTableFields.COLUMN_NOTE_ID.getFieldName() + " = " + id;
                count++;
            }
            Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, notesIdsWhereStr, null, null, null, null);
            c.moveToFirst();
            String imageDataIdsWhereStr = "";
            count = 0;
            while (!c.isAfterLast()) {
                long imageDataId = c.getLong(0);
                c.moveToNext();
                if (count > 0) {
                    imageDataIdsWhereStr = imageDataIdsWhereStr + " || ";
                }
                imageDataIdsWhereStr = imageDataIdsWhereStr + ImageDataTableFields.COLUMN_ID.getFieldName() + " = " + imageDataId;
                count++;

            }
            c.close();


            // delete images
            String query = "delete from " + TABLE_IMAGES + " where " + notesIdsWhereStr;
            SQLiteStatement deleteStmt = sqliteDatabase.compileStatement(query);
            deleteStmt.execute();

            // delete images data
            query = "delete from " + TABLE_IMAGE_DATA + " where " + imageDataIdsWhereStr;
            deleteStmt = sqliteDatabase.compileStatement(query);
            deleteStmt.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOIMAGES", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }


//    /**
//     * Get the collected notes from the database inside a given bound.
//     *
//     * @param n north
//     * @param s south
//     * @param w west
//     * @param e east
//     * @return the list of notes inside the bounds.
//     * @throws IOException if something goes wrong.
//     */
//    public static List<Image> getImagesInWorldBounds(float n, float s, float w, float e) throws IOException {
//
//        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
//        String query = "SELECT _id, lon, lat, altim, azim, path, text, ts FROM XXX WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
//        // String[] args = new String[]{TABLE_NOTES, String.valueOf(w), String.valueOf(e),
//        // String.valueOf(s), String.valueOf(n)};
//
//        query = query.replaceFirst("XXX", TABLE_IMAGES);
//        query = query.replaceFirst("XXX", String.valueOf(w));
//        query = query.replaceFirst("XXX", String.valueOf(e));
//        query = query.replaceFirst("XXX", String.valueOf(s));
//        query = query.replaceFirst("XXX", String.valueOf(n));
//
//        // if (Debug.D) Logger.i("DAOIMAGES", "Query: " + query);
//
//        Cursor c = sqliteDatabase.rawQuery(query, null);
//        List<Image> images = new ArrayList<Image>();
//        c.moveToFirst();
//        while (!c.isAfterLast()) {
//            long id = c.getLong(0);
//            double lon = c.getDouble(1);
//            double lat = c.getDouble(2);
//            double altim = c.getDouble(3);
//            double azim = c.getDouble(4);
//            String path = c.getString(5);
//            String text = c.getString(6);
//            String date = c.getString(7);
//
//            Image image = new Image(id, text, lon, lat, altim, azim, path, date);
//            images.add(image);
//            c.moveToNext();
//        }
//        c.close();
//        return images;
//    }

    /**
     * Get the list of notes from the db.
     *
     * @param onlyDirty if true, only dirty notes will be retrieved.
     * @return list of notes.
     * @throws IOException if something goes wrong.
     */
    public static List<Image> getImagesList(boolean onlyDirty) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<Image> images = new ArrayList<Image>();
        String asColumnsToReturn[] = { //
                ImageTableFields.COLUMN_ID.getFieldName(),//
                ImageTableFields.COLUMN_LON.getFieldName(),//
                ImageTableFields.COLUMN_LAT.getFieldName(),//
                ImageTableFields.COLUMN_ALTIM.getFieldName(),//
                ImageTableFields.COLUMN_TS.getFieldName(),//
                ImageTableFields.COLUMN_AZIM.getFieldName(),//
                ImageTableFields.COLUMN_TEXT.getFieldName(),//
                ImageTableFields.COLUMN_NOTE_ID.getFieldName(),//
                ImageTableFields.COLUMN_IMAGE_ID.getFieldName()//
        };
        String strSortOrder = "_id ASC";
        String isDirtyString = null;
        if (onlyDirty) {
            isDirtyString = ImageTableFields.COLUMN_ISDIRTY.getFieldName() + " = " + 1;
        }
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, isDirtyString, null, null, null, strSortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            long id = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            double altim = c.getDouble(3);
            long ts = c.getLong(4);
            double azim = c.getDouble(5);
            String text = c.getString(6);
            long noteId = c.getLong(7);
            long imageDataId = c.getLong(8);

            Image image = new Image(id, text, lon, lat, altim, azim, imageDataId, noteId, ts);
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
     * @throws IOException if something goes wrong.
     */
    public static List<OverlayItem> getImagesOverlayList(Drawable marker) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<OverlayItem> images = new ArrayList<OverlayItem>();
        String asColumnsToReturn[] = {//
                ImageTableFields.COLUMN_LON.getFieldName(),//
                ImageTableFields.COLUMN_LAT.getFieldName(), //
                ImageTableFields.COLUMN_IMAGE_ID.getFieldName(),//
                ImageTableFields.COLUMN_TEXT.getFieldName()//
        };
        String strSortOrder = "_id ASC";
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, null, null, null, null, strSortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            long imageDataId = c.getLong(2);
            String text = c.getString(3);

            OverlayItem image = new OverlayItem(new GeoPoint(lat, lon), imageDataId + "", text, marker);
            images.add(image);
            c.moveToNext();
        }
        c.close();
        return images;
    }


}
