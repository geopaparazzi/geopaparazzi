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
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import eu.hydrologis.geopaparazzi.gpx.GpxItem;
import eu.hydrologis.geopaparazzi.osm.MapItem;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.PointF3D;
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

    private static SimpleDateFormat dateFormatter = Constants.TIME_FORMATTER_SQLITE;

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
    public static long addMap( Date ts, int mapType, String text, float width, String color,
            boolean visible ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
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
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
        return rowId;
    }

    public static void addMapDataPoint( SQLiteDatabase sqliteDatabase, long mapId, double lon,
            double lat, String text, Date timestamp ) throws IOException {
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

    public static List<MapItem> getMaps() throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
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
            // Log.d(DEBUG_TAG, "Res: " + logid + "/" + color + "/" + width + "/" + visible + "/" +
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

        Log.d(TAG, "Query: " + query);
        Log.d(TAG, "gave logs: " + logsList.size());

        return logsList;
    }

    public static void deleteMap( long id ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        sqliteDatabase.beginTransaction();
        try {
            // delete log
            String query = "delete from " + TABLE_MAPS + " where " + COLUMN_ID + " = " + id;
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            // delete properties
            query = "delete from " + TABLE_PROPERTIES + " where " + COLUMN_MAPID + " = " + id;
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            // delete data
            query = "delete from " + TABLE_DATA + " where " + COLUMN_MAPID + " = " + id;
            sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void updateMapProperties( long mapid, String color, float width, boolean visible,
            String name ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
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
            Log.i(TAG, query);
            // sqliteDatabase.execSQL(query);
            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
            sqlUpdate.execute();

            if (name != null && name.length() > 0) {
                sb = new StringBuilder();
                sb.append("UPDATE ");
                sb.append(TABLE_MAPS);
                sb.append(" SET ");
                sb.append(COLUMN_MAP_TEXT).append("='").append(name).append("' ");
                sb.append("WHERE ").append(COLUMN_ID).append("=").append(mapid);

                query = sb.toString();
                Log.i(TAG, query);
                sqlUpdate = sqliteDatabase.compileStatement(query);
                sqlUpdate.execute();
            }

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    /**
     * Get the collected lines from the database inside a given bound.
     * 
     * @param n
     * @param s
     * @param w
     * @param e
     * @return the map of lines inside the bounds.
     * @throws IOException
     */
    public static HashMap<Long, PointsContainer> getCoordinatesInWorldBounds( float n, float s,
            float w, float e ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        HashMap<Long, PointsContainer> linesMap = new HashMap<Long, PointsContainer>();

        Log.d(TAG, "PRE  NSEW = " + n + "/" + s + "/" + e + "/" + w);
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;
        Log.d(TAG, "POST NSEW = " + n + "/" + s + "/" + e + "/" + w);

        String asColumnsToReturn[] = {COLUMN_MAPID, COLUMN_DATA_LON, COLUMN_DATA_LAT,
                COLUMN_DATA_TS, COLUMN_DATA_TEXT};
        StringBuilder sB = new StringBuilder();
        sB.append("(");
        sB.append(COLUMN_DATA_LON);
        sB.append(" BETWEEN ? AND ?) AND (");
        sB.append(COLUMN_DATA_LAT);
        sB.append(" BETWEEN ? AND ?)");
        String strWhere = sB.toString();
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e),
                String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_MAPID + "," + COLUMN_DATA_TS + " ASC";
        Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs,
                null, null, strSortOrder);
        c.moveToFirst();
        long previousMapid = -1;
        PointsContainer line = null;
        int index = 0;
        while( !c.isAfterLast() ) {
            long mapid = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            String date = c.getString(3);
            String name = c.getString(4);

            if (mapid != previousMapid) {
                line = new PointsContainer("log_" + mapid);
                linesMap.put(mapid, line);
                previousMapid = mapid;
            }
            line.addPoint(lon, lat, 0.0, date, name);
            c.moveToNext();
            index++;
        }
        c.close();
        Log.i(TAG, "Read points = " + index);
        return linesMap;
    }

    /**
     * Get the line of a given map from the database inside a given bound.
     * 
     * @param mapId the id of the map to pic.
     * @param n
     * @param s
     * @param w
     * @param e
     * @return the map of lines inside the bounds.
     * @throws IOException
     */
    public static PointsContainer getCoordinatesInWorldBoundsForMapId( long mapId, float n,
            float s, float w, float e ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();

        // Log.d(TAG, "PRE  NSEW = " + n + "/" + s + "/" + e + "/" + w);
        n = n + DatabaseManager.BUFFER;
        s = s - DatabaseManager.BUFFER;
        e = e + DatabaseManager.BUFFER;
        w = w - DatabaseManager.BUFFER;
        // Log.d(TAG, "POST NSEW = " + n + "/" + s + "/" + e + "/" + w);

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS,
                COLUMN_DATA_TEXT};
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
        // Log.d(TAG, "WHERESTR = " + strWhere);
        String[] strWhereArgs = new String[]{String.valueOf(w), String.valueOf(e),
                String.valueOf(s), String.valueOf(n)};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, strWhereArgs,
                null, null, strSortOrder);
        c.moveToFirst();
        PointsContainer line = new PointsContainer("log_" + mapId);
        int index = 0;
        while( !c.isAfterLast() ) {
            double lon = c.getDouble(0);
            double lat = c.getDouble(1);
            String date = c.getString(2);
            String name = c.getString(3);

            line.addPoint(lon, lat, 0.0, date, name);
            c.moveToNext();
            index++;
        }
        c.close();
        Log.i(TAG, "Read points = " + index);
        return line;
    }

    /**
     * Get the map of lines from the db, having the gpslog id in the key.
     * 
     * @return the map of lines.
     * @throws IOException
     */
    public static LinkedHashMap<Long, Line> getLinesMap() throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        LinkedHashMap<Long, Line> linesMap = new LinkedHashMap<Long, Line>();

        String asColumnsToReturn[] = {COLUMN_MAPID, COLUMN_DATA_LON, COLUMN_DATA_LAT,
                COLUMN_DATA_TS};
        String strSortOrder = COLUMN_MAPID + "," + COLUMN_DATA_TS + " ASC";
        Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, null, null, null, null,
                strSortOrder);
        c.moveToFirst();
        while( !c.isAfterLast() ) {
            long logid = c.getLong(0);
            double lon = c.getDouble(1);
            double lat = c.getDouble(2);
            String date = c.getString(3);
            Line line = linesMap.get(logid);
            if (line == null) {
                line = new Line("log_" + logid);
                linesMap.put(logid, line);
            }
            line.addPoint(lon, lat, 0.0, date);
            c.moveToNext();
        }
        c.close();
        return linesMap;
    }

    /**
     * Get the linefor a certainlog id from the db
     * 
     * @return the line.
     * @throws IOException
     */
    public static Line getMapAsLine( long logId ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();

        String asColumnsToReturn[] = {COLUMN_DATA_LON, COLUMN_DATA_LAT, COLUMN_DATA_TS};
        String strSortOrder = COLUMN_DATA_TS + " ASC";
        String strWhere = COLUMN_MAPID + "=" + logId;
        Cursor c = sqliteDatabase.query(TABLE_DATA, asColumnsToReturn, strWhere, null, null, null,
                strSortOrder);
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
     * Import a gpx in the database.
     * 
     * @param gpxItem
     * @param mapType
     * @throws IOException
     */
    public static void importGpxToMap( GpxItem gpxItem, int mapType ) throws IOException {
        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        String filename = gpxItem.getFilename();
        List<PointF3D> points = gpxItem.read();
        List<String> names = gpxItem.getNames();
        Date date = new Date(System.currentTimeMillis());
        long logid = addMap(date, mapType, filename, 2f, "red", false);

        sqliteDatabase.beginTransaction();
        try {
            long currentTimeMillis = System.currentTimeMillis();
            for( int i = 0; i < points.size(); i++ ) {
                date = new Date(currentTimeMillis + i * 1000l);
                PointF3D point = points.get(i);
                String text = null;
                if (names != null) {
                    text = names.get(i);
                }
                addMapDataPoint(sqliteDatabase, logid, point.x, point.y, text, date);
            }

            sqliteDatabase.setTransactionSuccessful();
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            sqliteDatabase.endTransaction();
        }
    }

    public static void createTables() throws IOException {
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

        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase();
        Log.i(TAG, "Create the maps_data table.");
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

        Log.i(TAG, "Create the maps table.");
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

        Log.i(TAG, "Create the maps properties table.");
        sqliteDatabase.execSQL(CREATE_TABLE_GPSLOGS_PROPERTIES);

    }

}
