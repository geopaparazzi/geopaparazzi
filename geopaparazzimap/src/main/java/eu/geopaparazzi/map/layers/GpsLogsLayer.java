package eu.geopaparazzi.map.layers;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import org.hortonmachine.dbs.utils.MercatorUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.JtsConverter;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;

import static eu.geopaparazzi.library.database.TableDescriptions.*;

import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.layers.items.GpsLog;

public class GpsLogsLayer extends VectorLayer {

    private final SharedPreferences peferences;

    public GpsLogsLayer(GPMapView mapView) {
        super(mapView.map());

        peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        try {
            reloadData();
        } catch (IOException e) {
            GPLog.error(this, null, e);
        }
    }

    public void reloadData() throws IOException {
        tmpDrawables.clear();
        mDrawables.clear();

        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();
        GeometryFactory gf = new GeometryFactory();

        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" AS ");
        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(", p.");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName());
        sB.append(", p.");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName());
        sB.append(", p.");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName());
        sB.append(" from ");
        sB.append(TABLE_GPSLOGS);
        sB.append(" l, ");
        sB.append(TABLE_GPSLOG_PROPERTIES);
        sB.append(" p where l.");
        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" = p.");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName());
        sB.append(" order by ");
        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
        String query = sB.toString();


        Cursor c = null;
        try {
            c = sqliteDatabase.rawQuery(query, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                try {
                    int visible = c.getInt(3);
                    if (visible == 1) {
                        long logid = c.getLong(0);
                        String color = c.getString(1);
                        double width = c.getDouble(2);

                        GpsLog log = new GpsLog();
                        log.color = color;
                        log.width = width;

                        List<Coordinate> gpslogGeoPoints = getGpslogGeoPoints(sqliteDatabase, logid, -1);
                        if (gpslogGeoPoints.size() > 1) {
                            LineString lineString = gf.createLineString(gpslogGeoPoints.toArray(new Coordinate[0]));
                            Style style = Style.builder()
                                    .strokeColor(color)
                                    .strokeWidth((float) width)
                                    .cap(Paint.Cap.ROUND)
                                    .build();
                            add(new LineDrawable(lineString, style));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                c.moveToNext();
            }
        } finally {
            if (c != null)
                c.close();
        }


        update();
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }


//    /**
//     * Get the gps logs.
//     *
//     * @return the logs list
//     * @throws IOException if something goes wrong.
//     */
//    public static List<GpsLog> getGpslogOverlays() throws IOException {
//        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();
//        List<GpsLog> logsList = new ArrayList<>();
//
//        StringBuilder sB = new StringBuilder();
//        sB.append("select l.");
//        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
//        sB.append(" AS ");
//        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
//        sB.append(", p.");
//        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName());
//        sB.append(", p.");
//        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName());
//        sB.append(", p.");
//        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName());
//        sB.append(" from ");
//        sB.append(TABLE_GPSLOGS);
//        sB.append(" l, ");
//        sB.append(TABLE_GPSLOG_PROPERTIES);
//        sB.append(" p where l.");
//        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
//        sB.append(" = p.");
//        sB.append(GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName());
//        sB.append(" order by ");
//        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName());
//        String query = sB.toString();
//
//        Cursor c = null;
//        try {
//            c = sqliteDatabase.rawQuery(query, null);
//            c.moveToFirst();
//            while (!c.isAfterLast()) {
//                int visible = c.getInt(3);
//                if (visible == 1) {
//                    long logid = c.getLong(0);
//                    String color = c.getString(1);
//                    double width = c.getDouble(2);
//
//                    GpsLog log = new GpsLog();
//                    log.color = color;
//                    log.width = width;
//
//                    List<Coordinate> gpslogGeoPoints = getGpslogGeoPoints(sqliteDatabase, logid, -1);
//                    if (gpslogGeoPoints.size() > 1) {
//                        logsList.add(log);
//                    }
//                }
//                c.moveToNext();
//            }
//        } finally {
//            if (c != null)
//                c.close();
//        }
//
//        // Logger.d(DEBUG_TAG, "Query: " + query);
//        // Logger.d(DEBUG_TAG, "gave logs: " + logsList.size());
//
//        return logsList;
//    }

    private static List<Coordinate> getGpslogGeoPoints(SQLiteDatabase sqliteDatabase, long logId, int pointsNum)
            throws IOException {

        String asColumnsToReturn[] = {GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName(), GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName()};
        String strSortOrder = GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName() + " ASC";
        String strWhere = GpsLogsDataTableFields.COLUMN_LOGID.getFieldName() + "=" + logId;
        Cursor c = null;
        try {
            c = sqliteDatabase.query(TABLE_GPSLOG_DATA, asColumnsToReturn, strWhere, null, null, null, strSortOrder);
            int count = c.getCount();
            int jump = 0;
            if (pointsNum != -1 && count > pointsNum) {
                jump = (int) Math.ceil((double) count / pointsNum);
            }

            c.moveToFirst();
            List<Coordinate> line = new ArrayList<>();
            while (!c.isAfterLast()) {
                double lon = c.getDouble(0);
                double lat = c.getDouble(1);
                try {
                    line.add(new Coordinate(lon, lat));
                } catch (Exception e) {
                    // ignore invalid coordinates
                }
                c.moveToNext();
                for (int i = 1; i < jump; i++) {
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
}
