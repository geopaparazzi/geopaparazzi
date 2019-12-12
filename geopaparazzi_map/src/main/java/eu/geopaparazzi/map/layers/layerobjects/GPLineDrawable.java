package eu.geopaparazzi.map.layers.layerobjects;

import org.locationtech.jts.geom.Geometry;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.util.List;

/**
 * Extension of {@link LineDrawable} with id.
 *
 * @author Antonello Andrea (www.hydrologis.com)
 */
public class GPLineDrawable extends LineDrawable implements IGPDrawable {
    private long id;

    public GPLineDrawable(Geometry line, Style style, long id) {
        super(line, style);
        this.id = id;
    }

    public GPLineDrawable(List<GeoPoint> points, long id) {
        super(points);
        this.id = id;
    }

    public GPLineDrawable(List<GeoPoint> points, Style style, long id) {
        super(points, style);
        this.id = id;
    }

    public GPLineDrawable(double[] lonLat, Style style, long id) {
        super(lonLat, style);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
