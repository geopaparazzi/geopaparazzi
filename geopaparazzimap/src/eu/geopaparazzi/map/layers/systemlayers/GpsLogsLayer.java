package eu.geopaparazzi.map.layers.systemlayers;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;

import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.MapUtilities;
import eu.geopaparazzi.map.layers.persistence.ISystemLayer;
import eu.geopaparazzi.map.layers.utils.GpsLog;

public class GpsLogsLayer extends VectorLayer implements ISystemLayer{

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
        SQLiteDatabase sqliteDatabase = GPApplication.getInstance().getDatabase();
        List<GpsLog> logsList = MapUtilities.getGpsLogs(sqliteDatabase);
        GeometryFactory gf = new GeometryFactory();

        tmpDrawables.clear();
        mDrawables.clear();
        for (GpsLog gpsLog : logsList) {
            LineString lineString = gf.createLineString(gpsLog.gpslogGeoPoints.toArray(new Coordinate[0]));
            Style lineStyle = Style.builder()
                    .strokeColor(ColorUtilities.toColor(gpsLog.color))
                    .strokeWidth((float) gpsLog.width)
                    .cap(Paint.Cap.ROUND)
                    .build();
            add(new LineDrawable(lineString, lineStyle));

            Point startPoint = lineString.getStartPoint();

            Style pointStyle = Style.builder()
                    .buffer(gpsLog.width)
                    .fillColor(ColorUtilities.toColor(gpsLog.color))
                    .strokeColor(ColorUtilities.toColor(gpsLog.color))
                    .scaleZoomLevel(19)
                    .fillAlpha(1)
                    .build();
            add(new PointDrawable(startPoint.getY(), startPoint.getX(), pointStyle));
        }
        update();
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }

    @Override
    public String getName() {
        return "Gps Log";
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return toDefaultJson();
    }
}
