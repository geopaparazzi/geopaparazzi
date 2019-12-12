package eu.geopaparazzi.map.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.TableDescriptions;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.layers.utils.GpsLog;

import static eu.geopaparazzi.library.database.TableDescriptions.TABLE_GPSLOGS;
import static eu.geopaparazzi.library.database.TableDescriptions.TABLE_GPSLOG_DATA;
import static eu.geopaparazzi.library.database.TableDescriptions.TABLE_GPSLOG_PROPERTIES;

@SuppressWarnings("ALL")
public class MapUtilities {

    public static final int SELECTED_FEATURES_UPDATED_RETURN_CODE = 672;
    public static final String PREFERENCES_KEY_THEME = "preferences_key_theme";


    public static List<Coordinate> getGpslogGeoPoints(SQLiteDatabase sqliteDatabase, long logId, int pointsNum)
            throws IOException {

        String asColumnsToReturn[] = {TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName(), TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName()};
        String strSortOrder = TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName() + " ASC";
        String strWhere = TableDescriptions.GpsLogsDataTableFields.COLUMN_LOGID.getFieldName() + "=" + logId;
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


    @NonNull
    public static List<GpsLog> getGpsLogs(SQLiteDatabase sqliteDatabase) {
        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" AS ");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(", p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName());
        sB.append(", p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName());
        sB.append(", p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName());
        sB.append(" from ");
        sB.append(TABLE_GPSLOGS);
        sB.append(" l, ");
        sB.append(TABLE_GPSLOG_PROPERTIES);
        sB.append(" p where l.");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" = p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName());
        sB.append(" order by ");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        String query = sB.toString();

        List<GpsLog> logsList = new ArrayList<>();
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

                        log.gpslogGeoPoints = MapUtilities.getGpslogGeoPoints(sqliteDatabase, logid, -1);
                        if (log.gpslogGeoPoints.size() > 1) {
                            logsList.add(log);
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
        return logsList;
    }

    public static GpsLog getLastGpsLog(SQLiteDatabase sqliteDatabase) {
        StringBuilder sB = new StringBuilder();
        sB.append("select l.");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" AS ");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(", p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName());
        sB.append(", p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName());
        sB.append(", p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName());
        sB.append(" from ");
        sB.append(TABLE_GPSLOGS);
        sB.append(" l, ");
        sB.append(TABLE_GPSLOG_PROPERTIES);
        sB.append(" p where l.");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" = p.");
        sB.append(TableDescriptions.GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName());
        sB.append(" order by ");
        sB.append(TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName());
        sB.append(" desc limit 1 ");
        String query = sB.toString();

        List<GpsLog> logsList = new ArrayList<>();
        Cursor c = null;
        try {
            c = sqliteDatabase.rawQuery(query, null);
            if (c.moveToFirst()) {
                try {
                    long logid = c.getLong(0);
                    String color = c.getString(1);
                    double width = c.getDouble(2);

                    GpsLog log = new GpsLog();
                    log.color = color;
                    log.width = width;

                    log.gpslogGeoPoints = MapUtilities.getGpslogGeoPoints(sqliteDatabase, logid, -1);
                    return log;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }


    /**
     * Convert a {@link QueryResult} into a list of features for the editing engine.
     *
     * @param tableName   name of the table the feature is part of.
     * @param dbPath      path to the db.
     * @param queryResult the result to convert.
     * @return the list of features.
     */
    public static List<Feature> fromQueryResult(String tableName, String dbPath, QueryResult queryResult) {
        int pkIndex = queryResult.pkIndex;
        int geomIndex = queryResult.geometryIndex;
        List<Feature> featuresList = new ArrayList<>();
        for (Object[] data : queryResult.data) {
            Feature feature = new Feature(tableName, dbPath, pkIndex, geomIndex);
            for (int i = 0; i < data.length; i++) {
                String cName = queryResult.names.get(i);
                Object dataObj = data[i];
                String typeStr = queryResult.types.get(i);

                EDataType type = EDataType.getType4Name(typeStr);
                feature.addAttribute(cName, dataObj, type.name());
            }
            featuresList.add(feature);
        }
        return featuresList;
    }

}
