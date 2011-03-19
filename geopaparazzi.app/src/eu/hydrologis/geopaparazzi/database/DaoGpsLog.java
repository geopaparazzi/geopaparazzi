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

import static java.lang.Math.abs;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView.Projection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Point;
import eu.hydrologis.geopaparazzi.maps.MapItem;
import eu.hydrologis.geopaparazzi.maps.MapView;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.LineArray;
import eu.hydrologis.geopaparazzi.util.LineScreenArray;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoGpsLog {
    private static final String COLUMN_ID = "_id";

    private static final String COLUMN_DATA_TS = "ts";
    private static final String COLUMN_DATA_ALTIM = "altim";
    private static final String COLUMN_DATA_LAT = "lat";
    private static final String COLUMN_DATA_LON = "lon";
    private static final String COLUMN_PROPERTIES_VISIBLE = "visible";
    private static final String COLUMN_PROPERTIES_WIDTH = "width";
    private static final String COLUMN_PROPERTIES_COLOR = "color";

    private static final String COLUMN_LOG_STARTTS = "startts";
    private static final String COLUMN_LOG_ENDTS = "endts";
    private static final String COLUMN_LOG_TEXT = "text";

    private static final String COLUMN_LOGID = "logid";

    public static final String TABLE_GPSLOGS = "gpslogs";
    public static final String TABLE_DATA = "gpslog_data";
    public static final String TABLE_PROPERTIES = "gpslogsproperties";

    private static SimpleDateFormat dateFormatter = Constants.TIME_FORMATTER_SQLITE;
    private static SimpleDateFormat dateFormatterForFile = Constants.TIMESTAMPFORMATTER;

    /**
     * Creates a new gpslog entry and returns the id.
     * 
     * @param startTs the start timestamp.
     * @param endTs the end timestamp.
     * @return the id of the gpslog.
     * @param text a description or null.
     * @return the id of the new created log.
     * @throws IOException 
     */
    public static long addGpsLog( Context context, Date startTs, Date endTs, String text, float width, String color,
            boolean visible ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        long rowId;
        try {
            // add new log
            ContentValues values = new ContentValues();
            values.put(COLUMN_LOG_STARTTS, dateFormatter.format(startTs));
            values.put(COLUMN_LOG_ENDTS, dateFormatter.format(endTs));
            if (text == null) {
                text = "log_" + dateFormatterForFile.format(startTs);
            }
            values.put(COLUMN_LOG_TEXT, text);
            rowId = sqliteDatabase.insertOrThrow(TABLE_GPSLOGS, null, values);

            // and some default properties
            ContentValues propValues = new ContentValues();
            propValues.put(COLUMN_LOGID, rowId);
            propValues.put(COLUMN_PROPERTIES_COLOR, color);
            propValues.put(COLUMN_PROPERTIES_WIDTH, width);
            propValues.put(COLUMN_PROPERTIES_VISIBLE, visible ? 1 : 0);
            sqliteDatabase.insertOrThrow(TABLE_PROPERTIES, null, propValues);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
        return rowId;
    }

    public static void addGpsLogDataPoint( SQLiteDatabase sqliteDatabase, long gpslogId, double lon, double lat, double altim,
            Date timestamp ) throws IOException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOGID, (int) gpslogId);
        values.put(COLUMN_DATA_LON, lon);
        values.put(COLUMN_DATA_LAT, lat);
        values.put(COLUMN_DATA_ALTIM, altim);
        values.put(COLUMN_DATA_TS, dateFormatter.format(timestamp));
        sqliteDatabase.insertOrThrow(TABLE_DATA, null, values);
    }

    /**
     * Get the gps logs.
     * 
     * @return the logs list
     * @throws IOException
     */
    public static List<MapItem> getGpslogs( Context context ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        List<MapItem> logsList = new ArrayList<MapItem>();

        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(COLUMN_ID);
        sB.append(" AS ");
        sB.append(COLUMN_ID);
        sB.append(", l.");
        sB.append(COLUMN_LOG_TEXT);
        sB.append(", p.");
        sB.append(COLUMN_PROPERTIES_COLOR);
        sB.append(", p.");
        sB.append(COLUMN_PROPERTIES_WIDTH);
        sB.append(", p.");
        sB.append(COLUMN_PROPERTIES_VISIBLE);
        sB.append(" from ");
        sB.append(TABLE_GPSLOGS);
        sB.append(" l, ");
        sB.append(TABLE_PROPERTIES);
        sB.append(" p where l.");
        sB.append(COLUMN_ID);
        sB.append(" = p.");
        sB.append(COLUMN_LOGID);
        sB.append(" order by ");
        sB.append(COLUMN_ID);
        String query = sB.toString();

        Cursor c = null;
        try {
            c = sqliteDatabase.rawQuery(query, null);
            c.moveToFirst();
            while( !c.isAfterLast() ) {
                long logid = c.getLong(0);
                String text = c.getString(1);
                String color = c.getString(2);
                double width = c.getDouble(3);
                int visible = c.getInt(4);
                // Logger.d(DEBUG_TAG, "Res: " + logid + "/" + color + "/" + width + "/" + visible +
                // "/" +
                // text);
                MapItem item = new MapItem();
                item.setId(logid);
                item.setName(text);
                item.setColor(color);
                item.setWidth((float) width);
                item.setVisible(visible == 1 ? true : false);
                logsList.add(item);
                c.moveToNext();
            }
        } finally {
            if (c != null)
                c.close();
        }

        // Logger.d(DEBUG_TAG, "Query: " + query);
        // Logger.d(DEBUG_TAG, "gave logs: " + logsList.size());

        return logsList;
    }

    public static void deleteGpslog( Context context, long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {
            // delete log
            String query = "delete from " + TABLE_GPSLOGS + " where " + COLUMN_ID + " = " + id;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            // delete properties
            query = "delete from " + TABLE_PROPERTIES + " where " + COLUMN_LOGID + " = " + id;
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            // delete data
            query = "delete from " + TABLE_DATA + " where " + COLUMN_LOGID + " = " + id;
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DOAGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void updateLogProperties( Context context, long logid, String color, float width, boolean visible, String name )
            throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_PROPERTIES);
            sb.append(" SET ");
            sb.append(COLUMN_PROPERTIES_COLOR).append("='").append(color).append("', ");
            sb.append(COLUMN_PROPERTIES_WIDTH).append("=").append(width).append(", ");
            sb.append(COLUMN_PROPERTIES_VISIBLE).append("=").append(visible ? 1 : 0).append(" ");
            sb.append("WHERE ").append(COLUMN_LOGID).append("=").append(logid);

            String query = sb.toString();
            Logger.i("DAOGPSLOG", query);
            // sqliteDatabase.execSQL(query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            if (name != null && name.length() > 0) {
                sb = new StringBuilder();
                sb.append("UPDATE ");
                sb.append(TABLE_GPSLOGS);
                sb.append(" SET ");
                sb.append(COLUMN_LOG_TEXT).append("='").append(name).append("' ");
                sb.append("WHERE ").append(COLUMN_ID).append("=").append(logid);

                query = sb.toString();
                Logger.i("DAOGPSLOG", query);
                sqlUpdate = sqliteDatabase.compileStatement(query);
                sqlUpdate.execute();
                sqlUpdate.close();
            }

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void setLogsVisibility( Context context, boolean visible ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_PROPERTIES);
            sb.append(" SET ");
            sb.append(COLUMN_PROPERTIES_VISIBLE).append("=").append(visible ? 1 : 0).append(" ");

            String query = sb.toString();
            Logger.i("DAOGPSLOG", query);
            // sqliteDatabase.execSQL(query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void mergeLogs( Context context, long logidToRemove, long destinationLogId ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {

            StringBuilder sb = new StringBuilder();
            sb.append("delete from ");
            sb.append(TABLE_GPSLOGS);
            sb.append(" where ");
            sb.append(COLUMN_ID);
            sb.append(" = ");
            sb.append(logidToRemove);
            String query = sb.toString();
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sb = new StringBuilder();
            sb.append("delete from ");
            sb.append(TABLE_PROPERTIES);
            sb.append(" where ");
            sb.append(COLUMN_LOGID);
            sb.append(" = ");
            sb.append(logidToRemove);
            query = sb.toString();
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_DATA);
            sb.append(" SET ");
            sb.append(COLUMN_LOGID).append("='").append(destinationLogId).append("' ");
            sb.append("WHERE ").append(COLUMN_LOGID).append("=").append(logidToRemove);

            query = sb.toString();
            Logger.i("DAOGPSLOG", query);
            // sqliteDatabase.execSQL(query);
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    // /**
    // * Get the collected lines from the database inside a given bound.
    // *
    // * @param n
    // * @param s
    // * @param w
    // * @param e
    // * @return the map of lines inside the bounds.
    // * @throws IOException
    // */
    // public static HashMap<Long, Line> getLinesInWorldBounds( Context context, float n, float s,
    // float w, float e )
    // throws IOException {
    // SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
    // HashMap<Long, Line> linesMap = new HashMap<Long, Line>();
    // n = n + DatabaseManager.BUFFER;
    // s = s - DatabaseManager.BUFFER;
    // e = e + DatabaseManager.BUFFER;
    // w = w - DatabaseManager.BUFFER;
    //
    // String asColumnsToReturn[] = {COLUMN_LOGID, COLUMN_DATA_LON, COLUMN_DATA_LAT,
    // COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
    // StringBuilder sB = new StringBuilder();
    // sB.append("(");
    // sB.append(COLUMN_DATA_LON);
    // sB.append(" BETWEEN ? AND ?) AND (");
    // sB.append(COLUMN_DATA_LAT);
    // sB.append(" BETWEEN ? AND ?)");
    // String strWhere = sB.toString();
    // String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s),
    // String.valueOf(n)};
    // String strSortOrder = COLUMN_LOGID + "," + COLUMN_DATA_TS + " ASC";
    // Cursor c = null;
    // try {
    // c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null,
    // strSortOrder);
    // c.moveToFirst();
    // while( !c.isAfterLast() ) {
    // long logid = c.getLong(0);
    // double lon = c.getDouble(1);
    // double lat = c.getDouble(2);
    // double altim = c.getDouble(3);
    // String date = c.getString(4);
    // Line line = linesMap.get(logid);
    // if (line == null) {
    // line = new Line("log_" + logid);
    // linesMap.put(logid, line);
    // }
    // line.addPoint(lon, lat, altim, date);
    // c.moveToNext();
    // }
    // } finally {
    // if (c != null)
    // c.close();
    // }
    // return linesMap;
    // }

    /**
     * Get the collected lines from the database inside a given bound - decimated for the given screen.
     * 
     * @param context
     * @param n
     * @param s
     * @param w
     * @param e
     * @param screenWidth
     * @param screenHeight
     * @param centerLon
     * @param centerLat
     * @param pixelDxInWorld
     * @param pixelDyInWorld
     * @param excludedId
     * @return
     * @throws IOException
     */
    public static HashMap<Long, LineArray> getLinesInWorldBoundsDecimated( Context context, float n, float s, float w, float e,
            int screenWidth, int screenHeight, float centerLon, float centerLat, float pixelDxInWorld, float pixelDyInWorld,
            long excludedId, int decimationFactor ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        HashMap<Long, LineArray> linesMap = new HashMap<Long, LineArray>();
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;

        String asColumnsToReturn[] = {COLUMN_LOGID, COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        StringBuilder sB = new StringBuilder();
        sB.append("(");
        sB.append(COLUMN_DATA_LON);
        sB.append(" BETWEEN ? AND ?) AND (");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" BETWEEN ? AND ?)");
        String strWhere = sB.toString();
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_LOGID + "," + COLUMN_DATA_TS + " ASC";
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null, strSortOrder);
            c.moveToFirst();

            int previousScreenX = Integer.MAX_VALUE;
            int previousScreenY = Integer.MAX_VALUE;

            int jump = 0;
            while( !c.isAfterLast() ) {
                long logid = c.getLong(0);
                if (excludedId != -1 && excludedId == logid) {
                    continue;
                }

                float lon = c.getFloat(1);
                float lat = c.getFloat(2);

                // check if on screen it would be placed on the same pixel
                int screenX = (int) MapView.lonToScreen(screenWidth, lon, centerLon, pixelDxInWorld);
                int screenY = (int) MapView.latToScreen(screenHeight, lat, centerLat, pixelDyInWorld);
                if (abs(screenX - previousScreenX) < decimationFactor && abs(screenY - previousScreenY) < decimationFactor) {
                    c.moveToNext();
                    jump++;
                    continue;
                }
                previousScreenX = screenX;
                previousScreenY = screenY;

                LineArray line = linesMap.get(logid);
                if (line == null) {
                    line = new LineArray("log_" + logid);
                    linesMap.put(logid, line);
                }
                line.addPoint(lon, lat);
                c.moveToNext();
            }
            // Logger.d("DAOGPSLOG", "Jumped: " + jump);
            // Set<Entry<Long, LineArray>> entrySet = linesMap.entrySet();
            // for( Entry<Long, LineArray> entry : entrySet ) {
            // Logger.d("DAOGPSLOG", "Found for log: " + entry.getKey() + " points: " +
            // entry.getValue().getIndex());
            // }
        } finally {
            if (c != null)
                c.close();
        }
        return linesMap;
    }

    public static LineArray getLinesInWorldBoundsByIdDecimated( Context context, float n, float s, float w, float e,
            int screenWidth, int screenHeight, float centerLon, float centerLat, float pixelDxInWorld, float pixelDyInWorld,
            long logId, int decimationFactor ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        StringBuilder sB = new StringBuilder();
        sB.append(COLUMN_LOGID);
        sB.append(" = ");
        sB.append(logId);
        sB.append(" AND (");
        sB.append(COLUMN_DATA_LON);
        sB.append(" BETWEEN ? AND ?) AND (");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" BETWEEN ? AND ?)");
        String strWhere = sB.toString();
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        LineArray line = new LineArray("log_" + logId);
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null, strSortOrder);
            c.moveToFirst();

            int previousScreenX = Integer.MAX_VALUE;
            int previousScreenY = Integer.MAX_VALUE;

            int jump = 0;
            while( !c.isAfterLast() ) {
                float lon = c.getFloat(0);
                float lat = c.getFloat(1);

                // check if on screen it would be placed on the same pixel
                int screenX = (int) MapView.lonToScreen(screenWidth, lon, centerLon, pixelDxInWorld);
                int screenY = (int) MapView.latToScreen(screenHeight, lat, centerLat, pixelDyInWorld);
                if (abs(screenX - previousScreenX) < decimationFactor && abs(screenY - previousScreenY) < decimationFactor) {
                    c.moveToNext();
                    jump++;
                    continue;
                }
                previousScreenX = screenX;
                previousScreenY = screenY;

                line.addPoint(lon, lat);
                c.moveToNext();
            }
            Logger.d("DAOGPSLOG", "Logs jumped: " + jump + " with thres: " + decimationFactor);
            // Set<Entry<Long, LineArray>> entrySet = linesMap.entrySet();
            // for( Entry<Long, LineArray> entry : entrySet ) {
            // Logger.d("DAOGPSLOG", "Found for log: " + entry.getKey() + " points: " +
            // entry.getValue().getIndex());
            // }
        } finally {
            if (c != null)
                c.close();
        }
        return line;
    }

    public static LineArray getLinesInWorldBoundsByIdDecimated2( Context context, float n, float s, float w, float e,
            Projection pj, long logId, int decimationFactor ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        StringBuilder sB = new StringBuilder();
        sB.append(COLUMN_LOGID);
        sB.append(" = ");
        sB.append(logId);
        sB.append(" AND (");
        sB.append(COLUMN_DATA_LON);
        sB.append(" BETWEEN ? AND ?) AND (");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" BETWEEN ? AND ?)");
        String strWhere = sB.toString();
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        LineArray line = new LineArray("log_" + logId);
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null, strSortOrder);
            c.moveToFirst();

            int previousScreenX = Integer.MAX_VALUE;
            int previousScreenY = Integer.MAX_VALUE;

            int jump = 0;
            while( !c.isAfterLast() ) {
                float lon = c.getFloat(0);
                float lat = c.getFloat(1);

                GeoPoint g = new GeoPoint(lat, lon);
                Point mapPixels = pj.toMapPixels(g, null);
                // check if on screen it would be placed on the same pixel
                int screenX = mapPixels.x;
                int screenY = mapPixels.y;
                if (abs(screenX - previousScreenX) < decimationFactor && abs(screenY - previousScreenY) < decimationFactor) {
                    c.moveToNext();
                    jump++;
                    continue;
                }
                previousScreenX = screenX;
                previousScreenY = screenY;

                line.addPoint(lon, lat);
                c.moveToNext();
            }
            Logger.d("DAOGPSLOG", "Logs jumped: " + jump + " with thres: " + decimationFactor);
            // Set<Entry<Long, LineArray>> entrySet = linesMap.entrySet();
            // for( Entry<Long, LineArray> entry : entrySet ) {
            // Logger.d("DAOGPSLOG", "Found for log: " + entry.getKey() + " points: " +
            // entry.getValue().getIndex());
            // }
        } finally {
            if (c != null)
                c.close();
        }
        return line;
    }

    public static LineScreenArray getScreenLinesInWorldBoundsByIdDecimated( Context context, float n, float s, float w, float e,
            Projection pj, long logId, int decimationFactor ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;
        
        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        StringBuilder sB = new StringBuilder();
        sB.append(COLUMN_LOGID);
        sB.append(" = ");
        sB.append(logId);
        sB.append(" AND (");
        sB.append(COLUMN_DATA_LON);
        sB.append(" BETWEEN ? AND ?) AND (");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" BETWEEN ? AND ?)");
        String strWhere = sB.toString();
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        LineScreenArray line = new LineScreenArray("log_" + logId);
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null, strSortOrder);
            c.moveToFirst();
            
            int previousScreenX = Integer.MAX_VALUE;
            int previousScreenY = Integer.MAX_VALUE;
            
            int jump = 0;
            while( !c.isAfterLast() ) {
                float lon = c.getFloat(0);
                float lat = c.getFloat(1);
                
                GeoPoint g = new GeoPoint(lat, lon);
                Point mapPixels = pj.toMapPixels(g, null);
                // check if on screen it would be placed on the same pixel
                int screenX = mapPixels.x;
                int screenY = mapPixels.y;
                if (abs(screenX - previousScreenX) < decimationFactor && abs(screenY - previousScreenY) < decimationFactor) {
                    c.moveToNext();
                    jump++;
                    continue;
                }
                previousScreenX = screenX;
                previousScreenY = screenY;
                
                line.addPoint(screenX, screenY);
                c.moveToNext();
            }
            Logger.d("DAOGPSLOG", "Logs jumped: " + jump + " with thres: " + decimationFactor);
            // Set<Entry<Long, LineArray>> entrySet = linesMap.entrySet();
            // for( Entry<Long, LineArray> entry : entrySet ) {
            // Logger.d("DAOGPSLOG", "Found for log: " + entry.getKey() + " points: " +
            // entry.getValue().getIndex());
            // }
        } finally {
            if (c != null)
                c.close();
        }
        return line;
    }

    /**
     * Get the map of lines from the db, having the gpslog id in the key.
     * 
     * @return the map of lines.
     * @throws IOException
     */
    public static LinkedHashMap<Long, Line> getLinesMap( Context context ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        LinkedHashMap<Long, Line> linesMap = new LinkedHashMap<Long, Line>();

        String asColumnsToReturn[] = {COLUMN_LOGID, COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_LOGID + "," + COLUMN_DATA_TS + " ASC";
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, null, null, null, null, strSortOrder);
            c.moveToFirst();
            while( !c.isAfterLast() ) {
                long logid = c.getLong(0);
                double lon = c.getDouble(1);
                double lat = c.getDouble(2);
                double altim = c.getDouble(3);
                String date = c.getString(4);
                Line line = linesMap.get(logid);
                if (line == null) {
                    line = new Line("log_" + logid);
                    linesMap.put(logid, line);
                }
                line.addPoint(lon, lat, altim, date);
                c.moveToNext();
            }
        } finally {
            if (c != null)
                c.close();
        }
        return linesMap;
    }

    /**
     * Get the linefor a certainlog id from the db
     * 
     * @param context
     * @param logId the id of the log.
     * @param pointsNum the max num of points that we want (-1 means all).
     * @return the line.
     * @throws IOException
     */
    public static Line getGpslogAsLine( Context context, long logId, int pointsNum ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        String strWhere = COLUMN_LOGID + "=" + logId;
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, null, null, null, strSortOrder);
            int count = c.getCount();
            int jump = 0;
            if (pointsNum != -1 && count > pointsNum) {
                jump = (int) Math.ceil((double) count / pointsNum);
            }

            c.moveToFirst();
            Line line = new Line("log_" + logId);
            while( !c.isAfterLast() ) {
                double lon = c.getDouble(0);
                double lat = c.getDouble(1);
                double altim = c.getDouble(2);
                String date = c.getString(3);
                line.addPoint(lon, lat, altim, date);
                c.moveToNext();
                for( int i = 1; i < jump; i++ ) {
                    c.moveToNext();
                    if (c.isAfterLast()) {
                        break;
                    }
                }
            }
            return line;
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * Get the first point of a gps log.
     * 
     * @param context
     * @param logId the id of the log to query.
     * @return the array of [lon, lat] of the first point.
     * @throws IOException
     */
    public static double[] getGpslogFirstPoint( Context context, long logId ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        String strWhere = COLUMN_LOGID + "=" + logId;
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, null, null, null, strSortOrder, "1");
            c.moveToFirst();
            double[] lonLat = new double[2];
            while( !c.isAfterLast() ) {
                lonLat[0] = c.getDouble(0);
                lonLat[1] = c.getDouble(1);
                break;
            }
            return lonLat;
        } finally {
            if (c != null)
                c.close();
        }
    }

    // public static void importGpxToGpslogs( Context context, GpxItem gpxItem ) throws IOException
    // {
    // SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
    // String filename = gpxItem.getFilename();
    // List<PointF3D> points = gpxItem.read();
    // Date date = new Date(System.currentTimeMillis());
    // long logid = addGpsLog(context, date, date, filename, 2f, "red", true);
    //
    // sqliteDatabase.beginTransaction();
    // try {
    // long currentTimeMillis = System.currentTimeMillis();
    // for( int i = 0; i < points.size(); i++ ) {
    // date = new Date(currentTimeMillis + i);
    // PointF3D point = points.get(i);
    // float z = point.getZ();
    // if (Float.isNaN(z)) {
    // z = 0f;
    // }
    // addGpsLogDataPoint(sqliteDatabase, logid, point.x, point.y, z, date);
    // }
    //
    // sqliteDatabase.setTransactionSuccessful();
    // } catch (Exception e) {
    // Logger.e("DAOGPSLOG", e.getLocalizedMessage(), e);
    // throw new IOException(e.getLocalizedMessage());
    // } finally {
    // sqliteDatabase.endTransaction();
    // }
    // }

    public static void createTables( Context context ) throws IOException {
        StringBuilder sB = new StringBuilder();

        /*
         * gps log data table
         */
        sB.append("CREATE TABLE ");
        sB.append(TABLE_DATA);
        sB.append(" (");
        sB.append(COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_DATA_LON).append(" REAL NOT NULL, ");
        sB.append(COLUMN_DATA_LAT).append(" REAL NOT NULL,");
        sB.append(COLUMN_DATA_ALTIM).append(" REAL NOT NULL,");
        sB.append(COLUMN_DATA_TS).append(" DATE NOT NULL,");
        sB.append(COLUMN_LOGID).append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT ");
        sB.append(COLUMN_LOGID);
        sB.append(" REFERENCES ");
        sB.append(TABLE_GPSLOGS);
        sB.append("(" + COLUMN_ID + ") ON DELETE CASCADE");
        sB.append(");");
        String CREATE_TABLE_GPSLOG_DATA = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_id_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_LOGID);
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_ID = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_ts_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_DATA_TS);
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_x_by_y_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_DATA_LON);
        sB.append(", ");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_logid_x_y_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_LOGID);
        sB.append(", ");
        sB.append(COLUMN_DATA_LON);
        sB.append(", ");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_LOGID_X_Y = sB.toString();

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        Logger.i("DAOGPSLOG", "Create the gpslog_data table.");
        sqliteDatabase.execSQL(CREATE_TABLE_GPSLOG_DATA);
        sqliteDatabase.execSQL(CREATE_INDEX_GPSLOG_ID);
        sqliteDatabase.execSQL(CREATE_INDEX_GPSLOG_TS);
        sqliteDatabase.execSQL(CREATE_INDEX_GPSLOG_X_BY_Y);
        sqliteDatabase.execSQL(CREATE_INDEX_GPSLOG_LOGID_X_Y);

        /*
         * gps log table
         */
        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_GPSLOGS);
        sB.append(" (");
        sB.append(COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_LOG_STARTTS).append(" DATE NOT NULL,");
        sB.append(COLUMN_LOG_ENDTS).append(" DATE NOT NULL,");
        sB.append(COLUMN_LOG_TEXT).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS = sB.toString();

        Logger.i("DAOGPSLOG", "Create the gpslogs table.");
        sqliteDatabase.execSQL(CREATE_TABLE_GPSLOGS);

        /*
         * properties table
         */
        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_PROPERTIES);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_LOGID);
        sB.append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT " + COLUMN_LOGID + " REFERENCES ");
        sB.append(TABLE_GPSLOGS);
        sB.append("(");
        sB.append(COLUMN_ID);
        sB.append(") ON DELETE CASCADE,");
        sB.append(COLUMN_PROPERTIES_COLOR).append(" TEXT NOT NULL, ");
        sB.append(COLUMN_PROPERTIES_WIDTH).append(" REAL NOT NULL, ");
        sB.append(COLUMN_PROPERTIES_VISIBLE).append(" INTEGER NOT NULL");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS_PROPERTIES = sB.toString();

        Logger.i("DAOGPSLOG", "Create the gpslogs properties table.");
        sqliteDatabase.execSQL(CREATE_TABLE_GPSLOGS_PROPERTIES);

    }

    public static void setEndTs( Context context, long logid, Date end ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        try {
            sqliteDatabase.beginTransaction();

            StringBuilder sb = new StringBuilder();
            sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_GPSLOGS);
            sb.append(" SET ");
            sb.append(COLUMN_LOG_ENDTS).append("='").append(dateFormatter.format(end)).append("' ");
            sb.append("WHERE ").append(COLUMN_ID).append("=").append(logid);

            String query = sb.toString();
            Logger.i("DAOGPSLOG", query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

}
