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
import java.util.List;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView.Projection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Point;
import eu.geopaparazzi.library.gpx.GpxItem;
import eu.geopaparazzi.library.gpx.parser.GpxParser.Route;
import eu.geopaparazzi.library.gpx.parser.GpxParser.TrackSegment;
import eu.geopaparazzi.library.gpx.parser.RoutePoint;
import eu.geopaparazzi.library.gpx.parser.TrackPoint;
import eu.geopaparazzi.library.gpx.parser.WayPoint;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.maps.MapItem;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.PointsContainer;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoMaps {
    private static final String COLUMN_ID = "_id";

    private static final String COLUMN_MAPID = "mapid";

    private static final String COLUMN_DATA_LAT = "lat";
    private static final String COLUMN_DATA_LON = "lon";
    private static final String COLUMN_DATA_TS = "ts";
    private static final String COLUMN_DATA_TEXT = "text";

    private static final String COLUMN_PROPERTIES_VISIBLE = "visible";
    private static final String COLUMN_PROPERTIES_WIDTH = "width";
    private static final String COLUMN_PROPERTIES_COLOR = "color";

    private static final String COLUMN_MAP_TEXT = "text";
    private static final String COLUMN_MAP_TS = "ts";
    private static final String COLUMN_MAP_TYPE = "type";

    private static final String TAG = "DAOMAPS";

    public static final String TABLE_MAPS = "maps";
    public static final String TABLE_DATA = "maps_data";
    public static final String TABLE_PROPERTIES = "mapsproperties";

    private static SimpleDateFormat dateFormatter = LibraryConstants.TIME_FORMATTER_SQLITE;

    /**
     * Creates a new map entry and returns the id.
     * 
     * @param ts the start timestamp.
     * @param endTs the end timestamp.
     * @return the id of the gpslog.
     * @param text a description or null.
     * @return the id of the new created log.
     * @throws IOException 
     */
    public static long addMap( Context context, Date ts, int mapType, String text, float width, String color, boolean visible )
            throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        long rowId;
        try {
            // add new log
            ContentValues values = new ContentValues();
            values.put(COLUMN_MAP_TS, dateFormatter.format(ts));
            values.put(COLUMN_MAP_TYPE, mapType);
            if (text == null) {
                text = "log_" + dateFormatter.format(ts);
            }
            values.put(COLUMN_MAP_TEXT, text);
            rowId = sqliteDatabase.insertOrThrow(TABLE_MAPS, null, values);

            // and some default properties
            ContentValues propValues = new ContentValues();
            propValues.put(COLUMN_MAPID, rowId);
            propValues.put(COLUMN_PROPERTIES_COLOR, color);
            propValues.put(COLUMN_PROPERTIES_WIDTH, width);
            propValues.put(COLUMN_PROPERTIES_VISIBLE, visible ? 1 : 0);
            sqliteDatabase.insertOrThrow(TABLE_PROPERTIES, null, propValues);

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
        return rowId;
    }

    public static void addMapDataPoint( SQLiteDatabase sqliteDatabase, long mapId, double lon, double lat, String text,
            Date timestamp ) throws IOException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAPID, (int) mapId);
        values.put(COLUMN_DATA_LON, lon);
        values.put(COLUMN_DATA_LAT, lat);
        if (text != null) {
            values.put(COLUMN_DATA_TEXT, text);
        }
        values.put(COLUMN_DATA_TS, dateFormatter.format(timestamp));
        sqliteDatabase.insertOrThrow(TABLE_DATA, null, values);
    }

    public static List<MapItem> getMaps( Context context ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        List<MapItem> logsList = new ArrayList<MapItem>();

        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(COLUMN_ID);
        sB.append(" AS ");
        sB.append(COLUMN_ID);
        sB.append(", l.");
        sB.append(COLUMN_MAP_TEXT);
        sB.append(", l.");
        sB.append(COLUMN_MAP_TYPE);
        sB.append(", p.");
        sB.append(COLUMN_PROPERTIES_COLOR);
        sB.append(", p.");
        sB.append(COLUMN_PROPERTIES_WIDTH);
        sB.append(", p.");
        sB.append(COLUMN_PROPERTIES_VISIBLE);
        sB.append(" from ");
        sB.append(TABLE_MAPS);
        sB.append(" l, ");
        sB.append(TABLE_PROPERTIES);
        sB.append(" p where l.");
        sB.append(COLUMN_ID);
        sB.append(" = p.");
        sB.append(COLUMN_MAPID);
        sB.append(" order by ");
        sB.append(COLUMN_ID);
        String query = sB.toString();

        Cursor c = sqliteDatabase.rawQuery(query, null);
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            long mapid = c.getLong(0);
            String text = c.getString(1);
            int type = c.getInt(2);
            String color = c.getString(3);
            double width = c.getDouble(4);
            int visible = c.getInt(5);
            // Logger.d(DEBUG_TAG, "Res: " + logid + "/" + color + "/" + width + "/" + visible + "/"
            // +
            // text);
            MapItem item = new MapItem();
            item.setId(mapid);
            item.setName(text);
            item.setColor(color);
            item.setWidth((float) width);
            item.setType(type);
            item.setVisible(visible == 1 ? true : false);
            logsList.add(item);
            c.moveToNext();
        }
        c.close();

        // Logger.d("DAOMAPS", "Query: " + query);
        // Logger.d("DAOMAPS", "gave logs: " + logsList.size());

        return logsList;
    }

    public static void deleteMap( Context context, long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {
            // delete log
            String query = "delete from " + TABLE_MAPS + " where " + COLUMN_ID + " = " + id;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            // delete properties
            query = "delete from " + TABLE_PROPERTIES + " where " + COLUMN_MAPID + " = " + id;
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            // delete data
            query = "delete from " + TABLE_DATA + " where " + COLUMN_MAPID + " = " + id;
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void updateMapProperties( Context context, long mapid, String color, float width, boolean visible, String name )
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
            sb.append("WHERE ").append(COLUMN_MAPID).append("=").append(mapid);

            String query = sb.toString();
            if (Debug.D)
                Logger.i(TAG, query);
            // sqliteDatabase.execSQL(query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            if (name != null && name.length() > 0) {
                sb = new StringBuilder();
                sb.append("UPDATE ");
                sb.append(TABLE_MAPS);
                sb.append(" SET ");
                sb.append(COLUMN_MAP_TEXT).append("='").append(name).append("' ");
                sb.append("WHERE ").append(COLUMN_ID).append("=").append(mapid);

                query = sb.toString();
                if (Debug.D)
                    Logger.i(TAG, query);
                sqlUpdate = sqliteDatabase.compileStatement(query);
                sqlUpdate.execute();
                sqlUpdate.close();
            }

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void setMapsVisibility( Context context, boolean visible ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        sqliteDatabase.beginTransaction();
        try {

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE ");
            sb.append(TABLE_PROPERTIES);
            sb.append(" SET ");
            sb.append(COLUMN_PROPERTIES_VISIBLE).append("=").append(visible ? 1 : 0).append(" ");

            String query = sb.toString();
            if (Debug.D)
                Logger.i(TAG, query);
            // sqliteDatabase.execSQL(query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();
            sqlUpdate.close();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
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
    // public static HashMap<Long, PointsContainer> getCoordinatesInWorldBounds( Context context,
    // float n, float s, float w, float e )
    // throws IOException {
    // SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
    // HashMap<Long, PointsContainer> linesMap = new HashMap<Long, PointsContainer>();
    //
    // Logger.d(TAG, "PRE  NSEW = " + n + "/" + s + "/" + e + "/" + w);
    // n = n + DatabaseManager.BUFFER;
    // s = s - DatabaseManager.BUFFER;
    // e = e + DatabaseManager.BUFFER;
    // w = w - DatabaseManager.BUFFER;
    // Logger.d(TAG, "POST NSEW = " + n + "/" + s + "/" + e + "/" + w);
    //
    // String asColumnsToReturn[] = {COLUMN_MAPID, COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS,
    // COLUMN_DATA_TEXT};
    // StringBuilder sB = new StringBuilder();
    // sB.append("(");
    // sB.append(COLUMN_DATA_LON);
    // sB.append(" BETWEEN ? AND ?) AND (");
    // sB.append(COLUMN_DATA_LAT);
    // sB.append(" BETWEEN ? AND ?)");
    // String strWhere = sB.toString();
    // String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s),
    // String.valueOf(n)};
    // String strSortOrder = COLUMN_MAPID + "," + COLUMN_DATA_TS + " ASC";
    // Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null,
    // null, strSortOrder);
    // c.moveToFirst();
    // long previousMapid = -1;
    // PointsContainer line = null;
    // int index = 0;
    // while( !c.isAfterLast() ) {
    // long mapid = c.getLong(0);
    // double lon = c.getDouble(1);
    // double lat = c.getDouble(2);
    // String date = c.getString(3);
    // String name = c.getString(4);
    //
    // if (mapid != previousMapid) {
    // line = new PointsContainer("log_" + mapid);
    // linesMap.put(mapid, line);
    // previousMapid = mapid;
    // }
    // line.addPoint(lon, lat, 0.0, date, name);
    // c.moveToNext();
    // index++;
    // }
    // c.close();
    // Logger.i(TAG, "Read points = " + index);
    // return linesMap;
    // }

    public static PointsContainer getCoordinatesInWorldBoundsForMapIdDecimated2( Context context, long mapId, float n, float s,
            float w, float e, Projection pj, int decimationFactor ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);

        // Logger.d(TAG, "PRE  NSEW = " + n + "/" + s + "/" + e + "/" + w);
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;
        // Logger.d(TAG, "POST NSEW = " + n + "/" + s + "/" + e + "/" + w);

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS, COLUMN_DATA_TEXT};
        StringBuilder sB = new StringBuilder();
        sB.append("(");
        sB.append(COLUMN_DATA_LON);
        sB.append(" BETWEEN ? AND ?) AND (");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" BETWEEN ? AND ?)");
        sB.append(" AND ");
        sB.append(COLUMN_MAPID);
        sB.append(" = ");
        sB.append(mapId);
        String strWhere = sB.toString();
        // Logger.d(TAG, "WHERESTR = " + strWhere);
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e), String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs, null, null, strSortOrder);
        c.moveToFirst();

        int previousScreenX = Integer.MAX_VALUE;
        int previousScreenY = Integer.MAX_VALUE;

        PointsContainer pointsContainer = new PointsContainer("log_" + mapId);
        int index = 0;
        int jump = 0;
        while( !c.isAfterLast() ) {
            float lon = c.getFloat(0);
            float lat = c.getFloat(1);
            String name = c.getString(3);

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

            pointsContainer.addPoint(lon, lat, name);
            c.moveToNext();
            index++;
        }
        c.close();
        if (Debug.D) {
            Logger.d("DAOMAPS", "Maps jumped: " + jump);
            Logger.i(TAG, "Read points = " + index);
        }
        return pointsContainer;
    }

    // /**
    // * Get the map of lines from the db, having the gpslog id in the key.
    // *
    // * @return the map of lines.
    // * @throws IOException
    // */
    // public static LinkedHashMap<Long, Line> getLinesMap( Context context ) throws IOException {
    // SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
    // LinkedHashMap<Long, Line> linesMap = new LinkedHashMap<Long, Line>();
    //
    // String asColumnsToReturn[] = {COLUMN_MAPID, COLUMN_DATA_LON, COLUMN_DATA_LAT,
    // COLUMN_DATA_TS};
    // String strSortOrder = COLUMN_MAPID + "," + COLUMN_DATA_TS + " ASC";
    // Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, null, null, null, null,
    // strSortOrder);
    // c.moveToFirst();
    // while( !c.isAfterLast() ) {
    // long logid = c.getLong(0);
    // double lon = c.getDouble(1);
    // double lat = c.getDouble(2);
    // String date = c.getString(3);
    // Line line = linesMap.get(logid);
    // if (line == null) {
    // line = new Line("log_" + logid);
    // linesMap.put(logid, line);
    // }
    // line.addPoint(lon, lat, 0.0, date);
    // c.moveToNext();
    // }
    // c.close();
    // return linesMap;
    // }

    /**
     * Get the linefor a certainlog id from the db
     * 
     * @return the line.
     * @throws IOException
     */
    public static Line getMapAsLine( Context context, long logId ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        String strWhere = COLUMN_MAPID + "=" + logId;
        Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, null, null, null, strSortOrder);
        c.moveToFirst();
        Line line = new Line("log_" + logId);
        while( !c.isAfterLast() ) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            String date = c.getString(2);
            line.addPoint(lon, lat, 0.0, date);
            c.moveToNext();
        }
        c.close();
        return line;
    }

    /**
     * Get the first point of a certain map.
     * 
     * @param context
     * @param logId the id of the log to query.
     * @return the array of [lon, lat] of the first point.
     * @throws IOException
     */
    public static double[] getMapFirstPoint( Context context, long logId ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        String strWhere = COLUMN_MAPID + "=" + logId;
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
     * TODO refactor a better design, with the new gox parser this is ugly.
     * 
     * @param context
     * @param gpxItem the gpx wrapper.
     * @param forceLines if true, forces also waypoints to be imported as tracks.
     * @throws IOException
     */
    public static void importGpxToMap( Context context, GpxItem gpxItem, boolean forceLines ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);

        // waypoints
        List<WayPoint> wayPoints = gpxItem.getWayPoints();
        if (wayPoints.size() > 0) {
            String name = gpxItem.getName();
            int mapType = Constants.MAP_TYPE_POINT;
            float width = 5f;
            if (forceLines) {
                mapType = Constants.MAP_TYPE_LINE;
                width = 2f;
            }
            Date date = new Date(System.currentTimeMillis());
            long logid = addMap(context, date, mapType, name, width, "red", false);

            sqliteDatabase.beginTransaction();
            try {
                long currentTimeMillis = System.currentTimeMillis();
                for( int i = 0; i < wayPoints.size(); i++ ) {
                    date = new Date(currentTimeMillis + i * 1000l);
                    WayPoint point = wayPoints.get(i);
                    addMapDataPoint(sqliteDatabase, logid, point.getLongitude(), point.getLatitude(), point.getName(), date);
                }
                sqliteDatabase.setTransactionSuccessful();
            } catch (Exception e) {
                Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
                throw new IOException(e.getLocalizedMessage());
            } finally {
                sqliteDatabase.endTransaction();
            }
        }
        // tracks
        List<TrackSegment> trackSegments = gpxItem.getTrackSegments();
        if (trackSegments.size() > 0) {
            for( TrackSegment trackSegment : trackSegments ) {
                String name = trackSegment.getName();
                int mapType = Constants.MAP_TYPE_LINE;
                Date date = new Date(System.currentTimeMillis());
                long logid = addMap(context, date, mapType, name, 2f, "blue", false);

                sqliteDatabase.beginTransaction();
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    List<TrackPoint> points = trackSegment.getPoints();
                    for( int i = 0; i < points.size(); i++ ) {
                        date = new Date(currentTimeMillis + i * 1000l);
                        TrackPoint point = points.get(i);
                        addMapDataPoint(sqliteDatabase, logid, point.getLongitude(), point.getLatitude(), null, date);
                    }
                    sqliteDatabase.setTransactionSuccessful();
                } catch (Exception e) {
                    Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
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
                String name = route.getName();
                int mapType = Constants.MAP_TYPE_LINE;
                Date date = new Date(System.currentTimeMillis());
                long logid = addMap(context, date, mapType, name, 2f, "green", false);

                sqliteDatabase.beginTransaction();
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    List<RoutePoint> points = route.getPoints();
                    for( int i = 0; i < points.size(); i++ ) {
                        date = new Date(currentTimeMillis + i * 1000l);
                        RoutePoint point = points.get(i);
                        addMapDataPoint(sqliteDatabase, logid, point.getLongitude(), point.getLatitude(), null, date);
                    }
                    sqliteDatabase.setTransactionSuccessful();
                } catch (Exception e) {
                    Logger.e("DAOMAPS", e.getLocalizedMessage(), e);
                    throw new IOException(e.getLocalizedMessage());
                } finally {
                    sqliteDatabase.endTransaction();
                }
            }
        }
    }

    public static void createTables( Context context ) throws IOException {
        StringBuilder sB = new StringBuilder();

        /*
         * maps data table
         */
        sB.append("CREATE TABLE ");
        sB.append(TABLE_DATA);
        sB.append(" (");
        sB.append(COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_DATA_LON).append(" REAL NOT NULL, ");
        sB.append(COLUMN_DATA_LAT).append(" REAL NOT NULL,");
        sB.append(COLUMN_DATA_TS).append(" DATE NOT NULL,");
        sB.append(COLUMN_DATA_TEXT).append(" TEXT,");
        sB.append(COLUMN_MAPID).append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT ");
        sB.append(COLUMN_MAPID);
        sB.append(" REFERENCES ");
        sB.append(TABLE_MAPS);
        sB.append("(" + COLUMN_ID + ") ON DELETE CASCADE");
        sB.append(");");
        String CREATE_TABLE_MAPS_DATA = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX maps_id_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_MAPID);
        sB.append(" );");
        String CREATE_INDEX_MAPS_ID = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX maps_x_by_y_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_DATA_LON);
        sB.append(", ");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" );");
        String CREATE_INDEX_MAPS_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX maps_mapid_x_y_idx ON ");
        sB.append(TABLE_DATA);
        sB.append(" ( ");
        sB.append(COLUMN_MAPID);
        sB.append(", ");
        sB.append(COLUMN_DATA_LON);
        sB.append(", ");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" );");
        String CREATE_INDEX_MAPS_MAPID_X_Y = sB.toString();

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
        if (Debug.D)
            Logger.i(TAG, "Create the maps_data table.");
        sqliteDatabase.execSQL(CREATE_TABLE_MAPS_DATA);
        sqliteDatabase.execSQL(CREATE_INDEX_MAPS_ID);
        sqliteDatabase.execSQL(CREATE_INDEX_MAPS_X_BY_Y);
        sqliteDatabase.execSQL(CREATE_INDEX_MAPS_MAPID_X_Y);

        /*
         * maps table
         */
        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_MAPS);
        sB.append(" (");
        sB.append(COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_MAP_TS).append(" DATE NOT NULL,");
        sB.append(COLUMN_MAP_TYPE).append(" INTEGER NOT NULL,");
        sB.append(COLUMN_MAP_TEXT).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS = sB.toString();

        if (Debug.D)
            Logger.i(TAG, "Create the maps table.");
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
        sB.append(COLUMN_MAPID);
        sB.append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT " + COLUMN_MAPID + " REFERENCES ");
        sB.append(TABLE_MAPS);
        sB.append("(");
        sB.append(COLUMN_ID);
        sB.append(") ON DELETE CASCADE,");
        sB.append(COLUMN_PROPERTIES_COLOR).append(" TEXT NOT NULL, ");
        sB.append(COLUMN_PROPERTIES_WIDTH).append(" REAL NOT NULL, ");
        sB.append(COLUMN_PROPERTIES_VISIBLE).append(" INTEGER NOT NULL");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS_PROPERTIES = sB.toString();

        if (Debug.D)
            Logger.i(TAG, "Create the maps properties table.");
        sqliteDatabase.execSQL(CREATE_TABLE_GPSLOGS_PROPERTIES);

    }

}
