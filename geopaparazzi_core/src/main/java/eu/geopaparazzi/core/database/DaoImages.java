/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.core.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.model.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.core.GeopaparazziApplication;

import static eu.geopaparazzi.core.database.TableDescriptions.ImageDataTableFields;
import static eu.geopaparazzi.core.database.TableDescriptions.ImageTableFields;
import static eu.geopaparazzi.core.database.TableDescriptions.TABLE_IMAGES;
import static eu.geopaparazzi.core.database.TableDescriptions.TABLE_IMAGE_DATA;

/**
 * Data access object for images.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoImages implements IImagesDbHelper {

    /**
     * Create the image tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables() throws IOException {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_IMAGES);
        sB.append(" (");
        sB.append(ImageTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(ImageTableFields.COLUMN_LON.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(ImageTableFields.COLUMN_LAT.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_ALTIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_AZIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName());
        sB.append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT " + ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName() + " REFERENCES ");
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
        sB.append(ImageDataTableFields.COLUMN_IMAGE.getFieldName()).append(" BLOB NOT NULL,");
        sB.append(ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName()).append(" BLOB NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGEDATA = sB.toString();

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        if (GPLog.LOG_HEAVY)
            Log.i("DAOIMAGES", "Create the images tables.");

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
            throw new IOException(e);
        } finally {
            sqliteDatabase.endTransaction();
        }
    }


    public long addImage(double lon, double lat, double altim, double azim, long timestamp, String text, byte[] image, byte[] thumb, long noteId)
            throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // first insert image data
            ContentValues imageDataValues = new ContentValues();
            imageDataValues.put(ImageDataTableFields.COLUMN_IMAGE.getFieldName(), image);
            imageDataValues.put(ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName(), thumb);
            long imageDataId = sqliteDatabase.insertOrThrow(TABLE_IMAGE_DATA, null, imageDataValues);

            // then insert the image properties and reference to the image itself
            ContentValues values = new ContentValues();
            values.put(ImageTableFields.COLUMN_LON.getFieldName(), lon);
            values.put(ImageTableFields.COLUMN_LAT.getFieldName(), lat);
            values.put(ImageTableFields.COLUMN_ALTIM.getFieldName(), altim);
            values.put(ImageTableFields.COLUMN_TS.getFieldName(), timestamp);
            values.put(ImageTableFields.COLUMN_TEXT.getFieldName(), text);
            values.put(ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName(), imageDataId);
            values.put(ImageTableFields.COLUMN_AZIM.getFieldName(), azim);
            values.put(ImageTableFields.COLUMN_ISDIRTY.getFieldName(), 1);
            values.put(ImageTableFields.COLUMN_NOTE_ID.getFieldName(), noteId);
            long imageId = sqliteDatabase.insertOrThrow(TABLE_IMAGES, null, values);

            sqliteDatabase.setTransactionSuccessful();

            return imageId;
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
            String asColumnsToReturn[] = {ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName()};
            String imageIdsWhereStr = "";
            int count = 0;
            for (long id : ids) {
                if (count > 0) {
                    imageIdsWhereStr = imageIdsWhereStr + " or ";
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
                    imageDataIdsWhereStr = imageDataIdsWhereStr + " or ";
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
            String asColumnsToReturn[] = {ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName()};
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
     * Get the list of {@link eu.geopaparazzi.library.database.Image}s from the db.
     *
     * @param onlyDirty      if true, only dirty notes will be retrieved.
     * @param onlyStandalone if true, only pure image notes are returned.
     *                       One example of non pure image notes is the image
     *                       that belongs to a form based note.
     * @return list of notes.
     * @throws IOException if something goes wrong.
     */
    public static List<Image> getImagesList(boolean onlyDirty, boolean onlyStandalone) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<Image> images = new ArrayList<>();
        String asColumnsToReturn[] = { //
                ImageTableFields.COLUMN_ID.getFieldName(),//
                ImageTableFields.COLUMN_LON.getFieldName(),//
                ImageTableFields.COLUMN_LAT.getFieldName(),//
                ImageTableFields.COLUMN_ALTIM.getFieldName(),//
                ImageTableFields.COLUMN_TS.getFieldName(),//
                ImageTableFields.COLUMN_AZIM.getFieldName(),//
                ImageTableFields.COLUMN_TEXT.getFieldName(),//
                ImageTableFields.COLUMN_NOTE_ID.getFieldName(),//
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName()//
        };
        String strSortOrder = "_id ASC";
        String whereString = null;
        if (onlyDirty) {
            whereString = ImageTableFields.COLUMN_ISDIRTY.getFieldName() + " = " + 1;
        }
        if (onlyStandalone) {
            if (whereString != null) {
                whereString = whereString + " && ";
            } else {
                whereString = "";
            }
            whereString = whereString + ImageTableFields.COLUMN_NOTE_ID.getFieldName() + " < 0";
        }

        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, whereString, null, null, null, strSortOrder);
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

    public Image getImage(long imageId) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        String asColumnsToReturn[] = { //
                ImageTableFields.COLUMN_ID.getFieldName(),//
                ImageTableFields.COLUMN_LON.getFieldName(),//
                ImageTableFields.COLUMN_LAT.getFieldName(),//
                ImageTableFields.COLUMN_ALTIM.getFieldName(),//
                ImageTableFields.COLUMN_TS.getFieldName(),//
                ImageTableFields.COLUMN_AZIM.getFieldName(),//
                ImageTableFields.COLUMN_TEXT.getFieldName(),//
                ImageTableFields.COLUMN_NOTE_ID.getFieldName(),//
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName()//
        };
        String whereStr = ImageTableFields.COLUMN_ID.getFieldName() + " = " + imageId;
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, whereStr, null, null, null, null);
        try {
            c.moveToFirst();
            if (!c.isAfterLast()) {
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
                return image;
            }
            return null;
        } finally {
            c.close();
        }
    }

    public byte[] getImageData(long imageId) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        String[] asColumnsToReturn = { //
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName()//
        };
        String whereStr = ImageTableFields.COLUMN_ID.getFieldName() + " = " + imageId;
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, whereStr, null, null, null, null);
        c.moveToFirst();
        long imageDataId = -1;
        if (!c.isAfterLast()) {
            imageDataId = c.getLong(0);
        }
        c.close();

        if (imageDataId != -1) {
            byte[] imageData = getImageDataById(imageDataId, sqliteDatabase);
            return imageData;
        }

        return null;
    }

    public byte[] getImageDataById(long imageDataId, SQLiteDatabase sqliteDatabase) throws IOException {
        if (sqliteDatabase == null) {
            sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        }
        String[] asColumnsToReturn;
        String whereStr;
        Cursor c;
        asColumnsToReturn = new String[]{ //
                ImageDataTableFields.COLUMN_IMAGE.getFieldName()//
        };
        whereStr = ImageDataTableFields.COLUMN_ID.getFieldName() + " = " + imageDataId;
        c = sqliteDatabase.query(TABLE_IMAGE_DATA, asColumnsToReturn, whereStr, null, null, null, null);
        byte[] imageData = null;
        try {
            c.moveToFirst();
            imageData = null;
            if (!c.isAfterLast()) {
                imageData = c.getBlob(0);
            }
        } catch (Exception ex) {
//            if (ex.getLocalizedMessage().contains("Couldn't read row")) {
              try{
                String sizeQuery = "SELECT " + ImageDataTableFields.COLUMN_ID.getFieldName() +//
                        ", length(" + ImageDataTableFields.COLUMN_IMAGE.getFieldName() + ") " +//
                        "FROM " + TABLE_IMAGE_DATA +//
                        " WHERE " + whereStr;
                //"length(" + ImageDataTableFields.COLUMN_IMAGE.getFieldName() + ") > 1000000";
                Cursor sizeCursor = sqliteDatabase.rawQuery(sizeQuery, null);
                sizeCursor.moveToFirst();
                long blobSize = 0;
                if (!sizeCursor.isAfterLast()) {
                    blobSize = sizeCursor.getLong(1);
                }
                sizeCursor.close();

                ByteArrayOutputStream bout = new ByteArrayOutputStream();

                int maxBlobSize = ImageUtilities.MAXBLOBSIZE;
                if (blobSize > maxBlobSize) {
                    for (long i = 1; i <= blobSize; i = i + maxBlobSize) {
                        long from = i;
                        long size = maxBlobSize;
                        if (from + size > blobSize) {
                            size = blobSize - from + 1;
                        }
                        String tmpQuery = "SELECT substr(" + ImageDataTableFields.COLUMN_IMAGE.getFieldName() + //
                                "," + from + ", " + size + ") FROM " + TABLE_IMAGE_DATA + " WHERE " + whereStr;
                        if (GPLog.LOG_HEAVY)
                            GPLog.addLogEntry(this, "ISSUE QUERY: " + tmpQuery);
                        Cursor imageCunchCursor = sqliteDatabase.rawQuery(tmpQuery, null);
                        imageCunchCursor.moveToFirst();
                        if (!imageCunchCursor.isAfterLast()) {
                            byte[] blobData = imageCunchCursor.getBlob(0);
                            bout.write(blobData);
                        }
                        imageCunchCursor.close();
                    }
                    imageData = bout.toByteArray();
                    bout.close();
                }
            } catch (Exception e){
                  Throwable throwable = e.initCause(ex);
                  GPLog.error(this, null, throwable);
            }

        } finally {
            c.close();
        }
        return imageData;
    }


    public byte[] getImageThumbnail(long imageId) throws Exception {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        String[] asColumnsToReturn = { //
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName()//
        };
        String whereStr = ImageTableFields.COLUMN_ID.getFieldName() + " = " + imageId;
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, whereStr, null, null, null, null);
        c.moveToFirst();
        long imageDataId = -1;
        if (!c.isAfterLast()) {
            imageDataId = c.getLong(0);
        }
        c.close();

        if (imageDataId != -1) {
            byte[] imageData = getImageThumbnailById(sqliteDatabase, imageDataId);
            return imageData;
        }

        return null;
    }

    public byte[] getImageThumbnailById(SQLiteDatabase sqliteDatabase, long imageDataId) throws Exception {
        String[] asColumnsToReturn;
        String whereStr;
        asColumnsToReturn = new String[]{ //
                ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName()//
        };
        whereStr = ImageDataTableFields.COLUMN_ID.getFieldName() + " = " + imageDataId;
        Cursor c = sqliteDatabase.query(TABLE_IMAGE_DATA, asColumnsToReturn, whereStr, null, null, null, null);
        byte[] imageData = null;
        try {
            c.moveToFirst();
            imageData = null;
            if (!c.isAfterLast()) {
                imageData = c.getBlob(0);
            }
        } finally {
            c.close();
        }
        return imageData;
    }

    /**
     * Get all image overlays.
     *
     * @param marker the marker to use.
     * @param onlyStandalone if true, only pure image notes are returned.
     *                       One example of non pure image notes is the image
     *                       that belongs to a form based note.
     * @return the list of {@link OverlayItem}s.
     * @throws IOException if something goes wrong.
     */
    public static List<OverlayItem> getImagesOverlayList(Drawable marker, boolean onlyStandalone) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<OverlayItem> images = new ArrayList<>();
        String asColumnsToReturn[] = {//
                ImageTableFields.COLUMN_LON.getFieldName(),//
                ImageTableFields.COLUMN_LAT.getFieldName(), //
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName(),//
                ImageTableFields.COLUMN_TEXT.getFieldName()//
        };
        String strSortOrder = "_id ASC";
        String whereString = null;
        if (onlyStandalone) {
            whereString = ImageTableFields.COLUMN_NOTE_ID.getFieldName() + " < 0";
        }
        Cursor c = sqliteDatabase.query(TABLE_IMAGES, asColumnsToReturn, whereString, null, null, null, strSortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            long imageDataId = c.getLong(2);
            String text = c.getString(3);

            OverlayItem image = new OverlayItem(new GeoPoint(lat, lon), text, imageDataId + "", marker);
            images.add(image);
            c.moveToNext();
        }
        c.close();
        return images;
    }


}
