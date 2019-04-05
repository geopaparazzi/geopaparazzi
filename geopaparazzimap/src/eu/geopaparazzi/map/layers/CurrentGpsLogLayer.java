package eu.geopaparazzi.map.layers;

import android.database.sqlite.SQLiteDatabase;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.io.IOException;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.MapUtilities;
import eu.geopaparazzi.map.layers.items.GpsLog;

public class CurrentGpsLogLayer extends VectorLayer {
    private GeometryFactory gf = new GeometryFactory();
    private GpsLog lastLog;
    private Style lineStyle;

    public CurrentGpsLogLayer(GPMapView mapView) {
        super(mapView.map());
    }

    private void preLoadData() throws IOException {
        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();
        lastLog = MapUtilities.getLastGpsLog(sqliteDatabase);
        lineStyle = Style.builder()
                .strokeColor(ColorUtilities.toColor(lastLog.color))
                .strokeWidth((float) lastLog.width)
                .cap(Paint.Cap.ROUND)
                .build();

        reloadLog();
    }

    private void reloadLog() {
        LineString lineString = gf.createLineString(lastLog.gpslogGeoPoints.toArray(new Coordinate[0]));
        tmpDrawables.clear();
        mDrawables.clear();
        add(new LineDrawable(lineString, lineStyle));

        update();
    }


    /**
     * @param lastGpsServiceStatus
     * @param lastGpsPosition       lon, lat, elev
     * @param lastGpsPositionExtras accuracy, speed, bearing.
     * @param lastGpsStatusExtras   maxSatellites, satCount, satUsedInFixCount.
     * @param lastGpsLoggingStatus
     */
    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras, GpsLoggingStatus lastGpsLoggingStatus) {
        if (lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            if (lastLog == null) {
                try {
                    preLoadData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (lastGpsPosition != null) {
                lastLog.gpslogGeoPoints.add(new Coordinate(lastGpsPosition[0], lastGpsPosition[1]));
                reloadLog();
            }
        } else {
            lastLog = null;
            tmpDrawables.clear();
            mDrawables.clear();
        }
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }
}
