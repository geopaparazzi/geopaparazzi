package eu.geopaparazzi.map.layers.layerobjects;

import org.locationtech.jts.geom.Geometry;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.util.List;

/**
 * Extension of {@link PolygonDrawable} with id.
 *
 * @author Antonello Andrea (www.hydrologis.com)
 */
public class GPPolygonDrawable extends PolygonDrawable implements IGPDrawable {
    private long id;

    public GPPolygonDrawable(Geometry polygon, Style style, long id) {
        super(polygon, style);
        this.id = id;
    }

    public GPPolygonDrawable(List<GeoPoint> points, long id) {
        super(points);
        this.id = id;
    }

    public GPPolygonDrawable(Style style, long id, GeoPoint... points) {
        super(style, points);
        this.id = id;
    }

    public GPPolygonDrawable(List<GeoPoint> points, Style style, long id) {
        super(points, style);
        this.id = id;
    }

    public GPPolygonDrawable(GeoPoint[] points, GeoPoint[] holePoints, float lineWidth, int lineColor, int fillColor, float fillAlpha, long id) {
        super(points, holePoints, lineWidth, lineColor, fillColor, fillAlpha);
        this.id = id;
    }

    public GPPolygonDrawable(List<GeoPoint> points, List<GeoPoint> holePoints, float lineWidth, int lineColor, int fillColor, float fillAlpha, long id) {
        super(points, holePoints, lineWidth, lineColor, fillColor, fillAlpha);
        this.id = id;
    }

    public GPPolygonDrawable(List<GeoPoint> points, List<GeoPoint> holePoints, Style style, long id) {
        super(points, holePoints, style);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
