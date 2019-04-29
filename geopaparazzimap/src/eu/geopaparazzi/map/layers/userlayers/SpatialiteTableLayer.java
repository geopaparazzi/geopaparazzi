package eu.geopaparazzi.map.layers.userlayers;

import org.hortonmachine.dbs.utils.EGeometryType;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.oscim.backend.canvas.Paint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Layers;

import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IVectorLayer;
import eu.geopaparazzi.map.layers.utils.SpatialiteConnectionsHandler;

public class SpatialiteTableLayer extends VectorLayer implements IVectorLayer {

    private GPMapView mapView;
    private final String dbPath;
    private final String tableName;

    public SpatialiteTableLayer(GPMapView mapView, String dbPath, String tableName) {
        super(mapView.map());
        this.mapView = mapView;
        this.dbPath = dbPath;
        this.tableName = tableName;
    }

    public void load() {

        try {
            mDrawables.clear();
            tmpDrawables.clear();

            SpatialiteConnectionsHandler.INSTANCE.openTable(dbPath, tableName);

            EGeometryType geometryType = SpatialiteConnectionsHandler.INSTANCE.getGeometryType(dbPath, tableName);
            eu.geopaparazzi.library.style.Style gpStyle = SpatialiteConnectionsHandler.INSTANCE.getStyleForTable(dbPath, tableName, null);
            List<Geometry> geometries = SpatialiteConnectionsHandler.INSTANCE.getGeometries(dbPath, tableName);


            Style pointStyle = null;
            Style lineStyle = null;
            Style polygonStyle = null;

            for (Geometry geom : geometries) {
                if (geometryType == EGeometryType.POINT || geometryType == EGeometryType.MULTIPOINT) {
                    if (pointStyle == null) {
                        pointStyle = Style.builder()
                                .buffer(0.5)
                                .strokeWidth(gpStyle.width)
                                .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                                .fillColor(ColorUtilities.toColor(gpStyle.fillcolor))
                                .fillAlpha(gpStyle.fillalpha)
                                .build();
                    }
                    int numGeometries = geom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Geometry geometryN = geom.getGeometryN(i);
                        Coordinate c = geometryN.getCoordinate();
                        add(new PointDrawable(c.y, c.x, pointStyle));
                    }
                } else if (geometryType == EGeometryType.LINESTRING || geometryType == EGeometryType.MULTILINESTRING) {
                    int numGeometries = geom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Geometry geometryN = geom.getGeometryN(i);

                        if (lineStyle == null) {
                            lineStyle = Style.builder()
                                    .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                                    .strokeWidth(gpStyle.width)
                                    .cap(Paint.Cap.ROUND)
                                    .build();
                        }
                        add(new LineDrawable(geometryN, lineStyle));
                    }
                } else if (geometryType == EGeometryType.POLYGON || geometryType == EGeometryType.MULTIPOLYGON) {
                    if (polygonStyle == null) {
                        polygonStyle = Style.builder()
                                .strokeColor(ColorUtilities.toColor(gpStyle.strokecolor))
                                .strokeWidth(gpStyle.width)
                                .fillColor(ColorUtilities.toColor(gpStyle.fillcolor))
                                .fillAlpha(gpStyle.fillalpha)
                                .cap(Paint.Cap.ROUND)
                                .build();
                    }
                    int numGeometries = geom.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Geometry geometryN = geom.getGeometryN(i);
                        add(new PolygonDrawable(geometryN, polygonStyle));
                    }
                }
            }
            update();

            Layers layers = mapView.map().layers();
            layers.add(this, LayerGroups.GROUP_USERLAYERS.getGroupId());
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject jo = toDefaultJson();
        jo.put(LAYERPATH_TAG, dbPath);
        return jo;
    }

    @Override
    public void dispose() {
        try {
            SpatialiteConnectionsHandler.INSTANCE.disposeTable(dbPath, tableName);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }
}
