package eu.hydrologis.geopaparazzi.maps.overlays;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.graphics.Point;
import android.graphics.PointF;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Transformation that handles mapsforge transforms.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgePointTransformation implements PointTransformation {
    private byte drawZoom;
    private Projection projection;
    private final Point tmpPoint = new Point();
    private Point drawPosition;

    public MapsforgePointTransformation( Projection projection, Point drawPosition, byte drawZoom ) {
        this.projection = projection;
        this.drawPosition = drawPosition;
        this.drawZoom = drawZoom;
    }

    public void transform( Coordinate model, PointF view ) {
        projection.toPoint(new GeoPoint(model.y, model.x), tmpPoint, drawZoom);
        view.set(tmpPoint.x - drawPosition.x, tmpPoint.y - drawPosition.y);
    }
}