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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.util.Log;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.model.GeoPoint;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.IGpsLogDbHelper;
import eu.geopaparazzi.library.gpx.GpxItem;
import eu.geopaparazzi.library.gpx.parser.GpxParser.Route;
import eu.geopaparazzi.library.gpx.parser.GpxParser.TrackSegment;
import eu.geopaparazzi.library.gpx.parser.RoutePoint;
import eu.geopaparazzi.library.gpx.parser.TrackPoint;
import eu.geopaparazzi.library.gpx.parser.WayPoint;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.maps.LogMapItem;
import eu.hydrologis.geopaparazzi.util.Line;

import static java.lang.Math.abs;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoGpsLog implements IGpsLogDbHelper {
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
    private static final String COLUMN_LOG_LENGTHM = "lengthm";
    private static final String COLUMN_LOG_TEXT = "text";

    private static final String COLUMN_LOGID = "logid";

    /**
     * gpslog table name.
     */
    public static final String TABLE_GPSLOGS = "gpslogs";
    /**
     * gpslog data table name.
     */
    public static final String TABLE_DATA = "gpslog_data";
    /**
     * gpslog properties table name.
     */
    public static final String TABLE_PROPERTIES = "gpslogsproperties";

    private static SimpleDateFormat dateFormatter = TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC;
    private static SimpleDateFormat dateFormatterForLabelInLocalTime = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL;

    public SQLiteDatabase getDatabase() throws Exception {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        return sqliteDatabase;
    }

    public long addGpsLog( Date startTs, Date endTs, double lengthm, String text, float width, String color, boolean visible )
            throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        long rowId;
        try {
            // add new log
            ContentValues values = new ContentValues();
            values.put(COLUMN_LOG_STARTTS, dateFormatter.format(startTs));
            values.put(COLUMN_LOG_ENDTS, dateFormatter.format(endTs));
            if (text == null) {
                text = "log_" + dateFormatterForLabelInLocalTime.format(startTs);
            }
            values.put(COLUMN_LOG_LENGTHM, lengthm);
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
            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
        return rowId;
    }

    /**
     * Adds a new XY entry to the gps table.
     * 
     * @param gpslogId the ID from the GPS log table.
     * @param lon longitude.
     * @param lat latitude
     * @param altim altitude/elevation
     * @throws IOException if something goes wrong
     */
    public void addGpsLogDataPoint( SQLiteDatabase sqliteDatabase, long gpslogId, double lon, double lat, double altim,
            Date timestamp ) throws IOException {

        try {
            new GeoPoint(lat, lon);
        } catch (Exception e) {
            // if the point is not valid, do not insert it
            return;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_LOGID, (int) gpslogId);
        values.put(COLUMN_DATA_LON, lon);
        values.put(COLUMN_DATA_LAT, lat);
        values.put(COLUMN_DATA_ALTIM, altim);
        values.put(COLUMN_DATA_TS, dateFormatter.format(timestamp));
        sqliteDatabase.insertOrThrow(TABLE_DATA, null, values);
    }

    public void deleteGpslog( long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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
            GPLog.error("DOAGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public void setEndTs( long logid, Date end ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("DAOGPSLOG", query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public void setTrackLengthm( long logid, double lengthm ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        try {
            sqliteDatabase.beginTransaction();

            StringBuilder sb = new StringBuilder();
            sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_GPSLOGS);
            sb.append(" SET ");
            sb.append(COLUMN_LOG_LENGTHM).append("=").append(lengthm).append(" ");
            sb.append("WHERE ").append(COLUMN_ID).append("=").append(logid);

            String query = sb.toString();
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("DAOGPSLOG", query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Get the gps logs.
     * 
     * @return the logs list
     * @throws IOException  if something goes wrong.
     */
    public static List<LogMapItem> getGpslogs() throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<LogMapItem> logsList = new ArrayList<LogMapItem>();

        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(COLUMN_ID);
        sB.append(" AS ");
        sB.append(COLUMN_ID);
        sB.append(", l.");
        sB.append(COLUMN_LOG_TEXT);
        sB.append(", l.");
        sB.append(COLUMN_LOG_STARTTS);
        sB.append(", l.");
        sB.append(COLUMN_LOG_ENDTS);
        sB.append(", l.");
        sB.append(COLUMN_LOG_LENGTHM);
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
                String start = c.getString(2);
                String end = c.getString(3);
                double lengthm = c.getDouble(4);
                String color = c.getString(5);
                double width = c.getDouble(6);
                int visible = c.getInt(7);
                // Logger.d(DEBUG_TAG, "Res: " + logid + "/" + color + "/" + width + "/" + visible +
                // "/" +
                // text);
                LogMapItem item = new LogMapItem(logid, text, color, (float) width, visible == 1 ? true : false, start, end,
                        (double) lengthm);
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

    /**
     * Get the gps logs.
     * 
     * @return the logs list
     * @throws IOException  if something goes wrong.
     */
    public static List<OverlayWay> getGpslogOverlays() throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        List<OverlayWay> logsList = new ArrayList<OverlayWay>();

        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(COLUMN_ID);
        sB.append(" AS ");
        sB.append(COLUMN_ID);
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
                int visible = c.getInt(3);
                if (visible == 1) {
                    long logid = c.getLong(0);
                    String color = c.getString(1);
                    double width = c.getDouble(2);
                    // Logger.d(DEBUG_TAG, "Res: " + logid + "/" + color + "/" + width + "/" +
                    // visible +
                    // "/" +
                    // text);

                    Paint wayPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
                    wayPaintOutline.setStyle(Paint.Style.STROKE);
                    int lineColor = ColorUtilities.toColor(color);
                    wayPaintOutline.setColor(lineColor);
                    wayPaintOutline.setAlpha(255);
                    wayPaintOutline.setStrokeWidth((float) width);
                    wayPaintOutline.setStrokeJoin(Paint.Join.ROUND);

                    OverlayWay way = new OverlayWay();
                    List<GeoPoint> gpslogGeoPoints = getGpslogGeoPoints(sqliteDatabase, logid, -1);
                    if (gpslogGeoPoints.size() > 1) {
                        way.setPaint(null, wayPaintOutline);
                        GeoPoint[] geoPoints = gpslogGeoPoints.toArray(new GeoPoint[0]);
                        way.setWayNodes(new GeoPoint[][]{geoPoints});
                        // item.setId(logid);
                        // item.setVisible(visible == 1 ? true : false);
                        logsList.add(way);
                    }
                }
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

    /**
     * Get a gpslog by id.
     * @param logId the log id.
     * @param paintOutline the paint to use.
     * 
     * @return the way overlay.
     * @throws IOException  if something goes wrong.
     */
    public static OverlayWay getGpslogOverlayById( long logId, Paint paintOutline ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        OverlayWay way = new OverlayWay();
        List<GeoPoint> gpslogGeoPoints = getGpslogGeoPoints(sqliteDatabase, logId, -1);
        way.setPaint(null, paintOutline);
        GeoPoint[] geoPoints = gpslogGeoPoints.toArray(new GeoPoint[0]);
        way.setWayNodes(new GeoPoint[][]{geoPoints});
        return way;
    }

    private static List<GeoPoint> getGpslogGeoPoints( SQLiteDatabase sqliteDatabase, long logId, int pointsNum )
            throws IOException {

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT};
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
            List<GeoPoint> line = new ArrayList<GeoPoint>();
            while( !c.isAfterLast() ) {
                double lon = c.getDouble(0);
                double lat = c.getDouble(1);
                try {
                    line.add(new GeoPoint(lat, lon));
                } catch (Exception e) {
                    // ignore invalid coordinates
                }
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
     * Update the properties of a log.
     * 
     * @param logid the id of the log.
     * @param color color
     * @param width width
     * @param visible whether it is visible.
     * @param name the name.
     * @throws IOException  if something goes wrong.
     */
    public static void updateLogProperties( long logid, String color, float width, boolean visible, String name )
            throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("DAOGPSLOG", query);
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
                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry("DAOGPSLOG", query);
                sqlUpdate = sqliteDatabase.compileStatement(query);
                sqlUpdate.execute();
                sqlUpdate.close();
            }

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Change visibility of log.
     * 
     * @param visible if visible.
     * @throws IOException  if something goes wrong.
     */
    public static void setLogsVisibility( boolean visible ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_PROPERTIES);
            sb.append(" SET ");
            sb.append(COLUMN_PROPERTIES_VISIBLE).append("=").append(visible ? 1 : 0).append(" ");

            String query = sb.toString();
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("DAOGPSLOG", query);
            // sqliteDatabase.execSQL(query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Merge two logs.
     * 
     * @param logidToRemove log to merge into the second.
     * @param destinationLogId log to accept the points of the first.
     * @throws IOException  if something goes wrong.
     */
    public static void mergeLogs( long logidToRemove, long destinationLogId ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("DAOGPSLOG", query);
            // sqliteDatabase.execSQL(query);
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
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
    // SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
    // HashMap<Long, Line> linesMap = new HashMap<Long, Line>();
    // n = n + GeopaparazziApplication.BUFFER;
    // s = s - GeopaparazziApplication.BUFFER;
    // e = e + GeopaparazziApplication.BUFFER;
    // w = w - GeopaparazziApplication.BUFFER;
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

    // /**
    // * @param n
    // * @param s
    // * @param w
    // * @param e
    // * @param pj
    // * @param logId
    // * @param decimationFactor
    // * @return
    // * @throws IOException
    // */
    // public static LineArray getLinesInWorldBoundsByIdDecimated2( float n, float s, float w, float
    // e, Projection pj, long logId,
    // int decimationFactor ) throws IOException {
    // SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
    // n = n + GeopaparazziApplication.BUFFER;
    // s = s - GeopaparazziApplication.BUFFER;
    // e = e + GeopaparazziApplication.BUFFER;
    // w = w - GeopaparazziApplication.BUFFER;
    //
    // String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM,
    // COLUMN_DATA_TS};
    // StringBuilder sB = new StringBuilder();
    // sB.append(COLUMN_LOGID);
    // sB.append(" = ");
    // sB.append(logId);
    // sB.append(" AND (");
    // sB.append(COLUMN_DATA_LON);
    // sB.append(" BETWEEN ? AND ?) AND (");
    // sB.append(COLUMN_DATA_LAT);
    // sB.append(" BETWEEN ? AND ?)");
    // String strWhere = sB.toString();
    // String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s),
    // String.valueOf(n)};
    // String strSortOrder = COLUMN_DATA_TS + " ASC";
    // LineArray line = new LineArray("log_" + logId);
    // Cursor c = null;
    // try {
    // c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null,
    // strSortOrder);
    // c.moveToFirst();
    //
    // int previousScreenX = Integer.MAX_VALUE;
    // int previousScreenY = Integer.MAX_VALUE;
    //
    // @SuppressWarnings("unused")
    // int jump = 0;
    // while( !c.isAfterLast() ) {
    // float lon = c.getFloat(0);
    // float lat = c.getFloat(1);
    //
    // GeoPoint g = new GeoPoint(lat, lon);
    // Point mapPixels = pj.toPixels(g, null);
    // // check if on screen it would be placed on the same pixel
    // int screenX = mapPixels.x;
    // int screenY = mapPixels.y;
    // if (abs(screenX - previousScreenX) < decimationFactor && abs(screenY - previousScreenY) <
    // decimationFactor) {
    // c.moveToNext();
    // jump++;
    // continue;
    // }
    // previousScreenX = screenX;
    // previousScreenY = screenY;
    //
    // line.addPoint(lon, lat);
    // c.moveToNext();
    // }
    // // if (Debug.D)
    // // Logger.d("DAOGPSLOG", "Logs jumped: " + jump + " with thres: " + decimationFactor);
    // // Set<Entry<Long, LineArray>> entrySet = linesMap.entrySet();
    // // for( Entry<Long, LineArray> entry : entrySet ) {
    // // Logger.d("DAOGPSLOG", "Found for log: " + entry.getKey() + " points: " +
    // // entry.getValue().getIndex());
    // // }
    // } finally {
    // if (c != null)
    // c.close();
    // }
    // return line;
    // }

    /**
     * Retrieve a log in the given world bounds as {@link Path} to be drawn.
     * 
     * @param n north bound
     * @param s south bound
     * @param w west bound
     * @param e east bound
     * @param path the path into which the log is put.
     * @param pj the projection
     * @param logId the log id.
     * @param decimationFactor the decmation factor.
     * 
     * @throws IOException  if something goes wrong.
     */
    public static void getPathInWorldBoundsByIdDecimated( float n, float s, float w, float e, Path path, Projection pj,
            long logId, int decimationFactor ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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

        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null, strSortOrder);
            c.moveToFirst();

            int previousScreenX = Integer.MAX_VALUE;
            int previousScreenY = Integer.MAX_VALUE;

            // int jump = 0;
            boolean first = true;
            while( !c.isAfterLast() ) {
                float lon = c.getFloat(0);
                float lat = c.getFloat(1);

                GeoPoint g = new GeoPoint(lat, lon);
                Point mapPixels = pj.toPixels(g, null);
                // check if on screen it would be placed on the same pixel
                int screenX = mapPixels.x;
                int screenY = mapPixels.y;
                if (abs(screenX - previousScreenX) < decimationFactor && abs(screenY - previousScreenY) < decimationFactor) {
                    c.moveToNext();
                    // jump++;
                    continue;
                }
                previousScreenX = screenX;
                previousScreenY = screenY;

                if (first) {
                    path.moveTo(screenX, screenY);
                    first = false;
                } else {
                    path.lineTo(screenX, screenY);
                }
                c.moveToNext();
            }
            // if (Debug.D)
            // Logger.d("DAOGPSLOG", "Log points jumped: " + jump + " with thres: " +
            // decimationFactor);
        } finally {
            if (c != null)
                c.close();
        }
    }

    /**
     * Get the map of lines from the db, having the gpslog id in the key.
     * 
     * @return the map of lines.
     * @throws IOException  if something goes wrong.
     */
    public static LinkedHashMap<Long, Line> getLinesMap() throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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
     * Get the line for a certain log id from the db
     * 
     * @param logId the id of the log.
     * @param pointsNum the max num of points that we want (-1 means all).
     * 
     * @return the line.
     * @throws IOException  if something goes wrong.
     */
    public static Line getGpslogAsLine( long logId, int pointsNum ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();

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
     * @param logId the id of the log to query.
     * 
     * @return the array of [lon, lat] of the first point.
     * @throws IOException  if something goes wrong.
     */
    public static double[] getGpslogFirstPoint( long logId ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();

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

    /**
     * Get the last point of a gps log.
     * @param logId the id of the log to query.
     * 
     * @return the array of [lon, lat] of the last point.
     * @throws IOException  if something goes wrong.
     */
    public static double[] getGpslogLastPoint( long logId ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_ALTIM, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_DATA_TS + " DESC";
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

    /**
     * Import a gpx in the database.
     * 
     * TODO refactor a better design, with the new gpx parser this is ugly.
     * 
     * @param context the context to use.
     * @param gpxItem the gpx wrapper.
     * @throws IOException  if something goes wrong.
     */
    public static void importGpxToMap( Context context, GpxItem gpxItem ) throws IOException {
        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        String gpxName = gpxItem.getName();

        // waypoints
        List<WayPoint> wayPoints = gpxItem.getWayPoints();
        if (wayPoints.size() > 0) {
            Date date = new Date(System.currentTimeMillis());

            sqliteDatabase.beginTransaction();
            try {
                for( int i = 0; i < wayPoints.size(); i++ ) {
                    WayPoint point = wayPoints.get(i);
                    String dateStr = TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC.format(date);
                    String nameDescr = "";
                    String name = point.getName();
                    if (name != null) {
                        nameDescr = name;
                    }
                    String desc = point.getDescription();
                    if (name != null && desc != null) {
                        nameDescr = nameDescr + ":\n";
                    }
                    if (desc != null) {
                        nameDescr = nameDescr + desc;
                    }
                    DaoNotes.addNoteNoTransaction(point.getLongitude(), point.getLatitude(), point.getElevation(), dateStr,
                            nameDescr, NoteType.POI.getDef(), "", NoteType.POI.getTypeNum(), sqliteDatabase);
                }
                sqliteDatabase.setTransactionSuccessful();
            } catch (Exception e) {
                GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
                throw new IOException(e.getLocalizedMessage());
            } finally {
                sqliteDatabase.endTransaction();
            }
        }
        // tracks
        List<TrackSegment> trackSegments = gpxItem.getTrackSegments();
        if (trackSegments.size() > 0) {
            float width = 2f;
            for( TrackSegment trackSegment : trackSegments ) {
                String tsName = trackSegment.getName();
                if (tsName == null) {
                    tsName = "";
                } else {
                    tsName = " - " + tsName;
                }
                String name = gpxName + tsName;

                Date date = new Date(System.currentTimeMillis());

                DaoGpsLog helper = new DaoGpsLog();
                long logId = helper.addGpsLog(date, date, 0, name, width, "blue", true);

                sqliteDatabase.beginTransaction();
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    List<TrackPoint> points = trackSegment.getPoints();
                    for( int i = 0; i < points.size(); i++ ) {
                        TrackPoint point = points.get(i);
                        if (point.getTime() > 0) {
                            date = new Date(point.getTime());
                        } else {
                            date = new Date(currentTimeMillis + i * 1000l);
                        }
                        helper.addGpsLogDataPoint(sqliteDatabase, logId, point.getLongitude(), point.getLatitude(),
                                point.getElevation(), date);
                    }
                    sqliteDatabase.setTransactionSuccessful();
                } catch (Exception e) {
                    GPLog.error("DAOMAPS", e.getLocalizedMessage(), e);
                    throw new IOException(e.getLocalizedMessage());
                } finally {
                    sqliteDatabase.endTransaction();
                }
            }
        }
        // routes
        List<Route> routes = gpxItem.getRoutes();
        if (routes.size() > 0) {
            for( Route route : routes ) {
                String rName = route.getName();
                if (rName == null) {
                    rName = gpxName;
                }
                long startL = route.getFirstPointTime();
                long endL = route.getLastPointTime();
                Date startDate;
                Date endDate;
                if (startL > 0) {
                    startDate = new Date(startL);
                } else {
                    startDate = new Date(System.currentTimeMillis());
                }
                if (endL > 0) {
                    endDate = new Date(endL);
                } else {
                    endDate = new Date(System.currentTimeMillis());
                }
                Date date = new Date(System.currentTimeMillis());
                DaoGpsLog helper = new DaoGpsLog();
                long logId = helper.addGpsLog(startDate, endDate, 0, rName, 2f, "green", true);

                sqliteDatabase.beginTransaction();
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    List<RoutePoint> points = route.getPoints();
                    for( int i = 0; i < points.size(); i++ ) {
                        RoutePoint point = points.get(i);
                        if (point.getTime() > 0) {
                            date = new Date(point.getTime());
                        } else {
                            date = new Date(currentTimeMillis + i * 1000l);
                        }
                        helper.addGpsLogDataPoint(sqliteDatabase, logId, point.getLongitude(), point.getLatitude(),
                                point.getElevation(), date);
                    }
                    sqliteDatabase.setTransactionSuccessful();
                } catch (Exception e) {
                    GPLog.error("DAOMAPS", e.getLocalizedMessage(), e);
                    throw new IOException(e.getLocalizedMessage());
                } finally {
                    sqliteDatabase.endTransaction();
                }
            }
        }
    }
    // public static void importGpxToGpslogs( Context context, GpxItem gpxItem ) throws IOException
    // {
    // SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
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
    // GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
    // throw new IOException(e.getLocalizedMessage());
    // } finally {
    // sqliteDatabase.endTransaction();
    // }
    // }

    /**
     * Create log tables.
     * 
     * @throws IOException  if something goes wrong.
     */
    public static void createTables() throws IOException {
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

        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
        if (GPLog.LOG_ANDROID)
            Log.i("DAOGPSLOG", "Create the gpslog_data table.");
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
        sB.append(COLUMN_LOG_LENGTHM).append(" REAL NOT NULL, ");
        sB.append(COLUMN_LOG_TEXT).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS = sB.toString();

        if (GPLog.LOG_ANDROID)
            Log.i("DAOGPSLOG", "Create the gpslogs table.");
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

        if (GPLog.LOG_ANDROID)
            Log.i("DAOGPSLOG", "Create the gpslogs properties table.");
        sqliteDatabase.execSQL(CREATE_TABLE_GPSLOGS_PROPERTIES);

    }

    /**
     * Check to see if a column is in a table.
     * 
     * @param sqliteDatabase the database to check. 
     * @param inTable the name of the table to test.
     * @param columnToCheck the column to check for presence in table.
     * @return true or false for column presence in table.
     * @throws IOException  if something goes wrong.
     */

    public static boolean existsColumnInTable( SQLiteDatabase sqliteDatabase, String inTable, String columnToCheck )
            throws IOException {
        try {
            // query 1 row
            Cursor mCursor = sqliteDatabase.rawQuery("SELECT * FROM " + inTable + " LIMIT 0", null);

            // getColumnIndex gives us the index (0 to ...) of the column - otherwise we get a -1
            if (mCursor.getColumnIndex(columnToCheck) != -1)
                return true;
            else
                return false;

        } catch (Exception e) {
            // something went wrong. Missing the database? The table?
            Log.d("... - existsColumnInTable",
                    "When checking whether a column exists in the table, an error occurred: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add a field to a table.
     * 
     * <p>This is a very simple "add" and should not be used for 
     * columns needing indexing or keys
     * 
     * @param sqliteDatabase the database to use.
     * @param tableName the name of the table to add the field to
     * @param colName the name of the column
     * @param colType the type of column to add (REAL, DATE, INTEGER, TEXT)
     * @throws IOException  if something goes wrong.
     */
    public static void addFieldGPSTables( SQLiteDatabase sqliteDatabase, String tableName, String colName, String colType )
            throws IOException {

        StringBuilder sB = new StringBuilder();
        sB.append("ALTER TABLE ");
        sB.append(tableName);
        sB.append(" ADD COLUMN ");
        sB.append(colName).append(" ");
        sB.append(colType).append(" ; ");

        String ADD_FIELD_TO_TABLE = sB.toString();

        if (GPLog.LOG_ANDROID) {
            StringBuilder sB2 = new StringBuilder();
            sB2.append("Added ").append(colName).append(" to ").append(tableName);
            Log.i("DAOGPSLOG", sB2.toString());
        }
        sqliteDatabase.execSQL(ADD_FIELD_TO_TABLE);

    }

    /**
     * update the length of a log
     * 
     * 
     * @param logId the id of the log.
     * @return log length as double
     * @throws IOException  if something goes wrong.
     */
    public static double updateLogLength( long logId ) throws IOException {

        try {
            // get the log data, sum up the distances
            SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
            String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS};
            String strSortOrder = COLUMN_DATA_TS + " ASC";
            String strWhere = COLUMN_LOGID + "=" + logId;
            Cursor c = null;
            double summedDistance = 0.0;
            double lon = 0.0;
            double lat = 0.0;
            double prevLon = 0.0;
            double prevLat = 0.0;

            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("DAOGPSLOG", strWhere);
            try {
                c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, null, null, null, strSortOrder);
                c.moveToFirst();
                while( !c.isAfterLast() ) {
                    lon = c.getDouble(0);
                    lat = c.getDouble(1);

                    Location newLoc = new Location("tempLoc1"); //$NON-NLS-1$
                    newLoc.setLongitude(lon);
                    newLoc.setLatitude(lat);
                    Location prevLoc = new Location("tempLoc2"); //$NON-NLS-1$

                    if (GPLog.LOG_ABSURD) {
                        GPLog.addLogEntry("DAOGPSLOG", "lon: " + String.valueOf(lon));
                        GPLog.addLogEntry("DAOGPSLOG", "lat: " + String.valueOf(lat));
                        GPLog.addLogEntry("DAOGPSLOG", "prevlon: " + String.valueOf(prevLon));
                        GPLog.addLogEntry("DAOGPSLOG", "prevlat: " + String.valueOf(prevLat));
                    }
                    if (prevLon == 0.0) {
                        prevLon = lon;
                        prevLat = lat;
                    }
                    prevLoc.setLongitude(prevLon);
                    prevLoc.setLatitude(prevLat);
                    double lastDistance = newLoc.distanceTo(prevLoc);
                    if (GPLog.LOG_ABSURD) {
                        GPLog.addLogEntry("DAOGPSLOG", "distance: " + String.valueOf(lastDistance));
                    }
                    summedDistance = summedDistance + lastDistance;
                    prevLon = lon;
                    prevLat = lat;

                    c.moveToNext();
                }
            } finally {
                if (c != null)
                    c.close();
            }

            // update the gpslogs table with the summed distance
            sqliteDatabase.beginTransaction();
            String query = "update " + TABLE_GPSLOGS + " set lengthm = " + summedDistance + " where " + COLUMN_ID + " = " + logId;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();
            sqliteDatabase.setTransactionSuccessful();
            sqliteDatabase.endTransaction();

            // send the summed distance back so we don't have to query the table again
            return (summedDistance);
        } catch (IOException e) {
            GPLog.error("DAOMAPS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        }
    }
}
