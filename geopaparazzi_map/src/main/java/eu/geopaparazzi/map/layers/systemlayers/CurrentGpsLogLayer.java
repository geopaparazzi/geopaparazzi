package eu.geopaparazzi.map.layers.systemlayers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Layers;

import java.io.IOException;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IPositionLayer;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;
import eu.geopaparazzi.map.layers.utils.GpsLog;
import eu.geopaparazzi.map.utils.MapUtilities;

public class CurrentGpsLogLayer extends VectorLayer implements IPositionLayer, ISystemLayer {
    public static String NAME = null;
    private GeometryFactory gf = new GeometryFactory();
    private GpsLog lastLog;
    private Style lineStyle;
    private GPMapView mapView;

    public CurrentGpsLogLayer(GPMapView mapView) {
        super(mapView.map());
        this.mapView = mapView;
        getName(mapView.getContext());
    }

    public static String getName(Context context) {
        if (NAME == null) {
            NAME = context.getString(R.string.layername_current_log);
        }
        return NAME;
    }

    private void preLoadData() throws IOException {
        createLogAndStyle();

        reloadData();
    }

    private void createLogAndStyle() throws IOException {
        if (lastLog == null) {
            SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();
            lastLog = MapUtilities.getLastGpsLog(sqliteDatabase);
        }
        if (lastLog != null && lineStyle == null)
            lineStyle = Style.builder()
                    .strokeColor(ColorUtilities.toColor(lastLog.color))
                    .strokeWidth((float) lastLog.width)
                    .cap(Paint.Cap.ROUND)
                    .build();
    }

    public void reloadData() {
        try {
            createLogAndStyle();
        } catch (IOException e) {
            GPLog.error(this, "ERRROR loading log/style", e);//NON-NLS
        }
        if (lastLog.gpslogGeoPoints.size() > 1) {
            LineString lineString = gf.createLineString(lastLog.gpslogGeoPoints.toArray(new Coordinate[0]));
            tmpDrawables.clear();
            mDrawables.clear();
            add(new LineDrawable(lineString, lineStyle));
            update();
        }
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
                reloadData();
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

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public void load() {
        Layers layers = map().layers();
        layers.add(this, LayerGroups.GROUP_PROJECTLAYERS.getGroupId());
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return toDefaultJson();
    }

    @Override
    public void dispose() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }
}
