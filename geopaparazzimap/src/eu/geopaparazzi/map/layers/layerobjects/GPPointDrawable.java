package eu.geopaparazzi.map.layers.layerobjects;

import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;

/**
 * Extension of {@link PointDrawable} with id.
 *
 * @author Antonello Andrea (www.hydrologis.com)
 */
public class GPPointDrawable extends PointDrawable implements IGPDrawable {
    private long id;

    public GPPointDrawable(GeoPoint point, long id) {
        super(point);
        this.id = id;
    }

    public GPPointDrawable(GeoPoint point, Style style, long id) {
        super(point, style);
        this.id = id;
    }

    public GPPointDrawable(double lat, double lon, Style style, long id) {
        super(lat, lon, style);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
