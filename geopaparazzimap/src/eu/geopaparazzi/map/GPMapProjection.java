package eu.geopaparazzi.map;

import android.graphics.Point;

import org.hortonmachine.dbs.utils.MercatorUtils;
import org.locationtech.jts.geom.Coordinate;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;

public class GPMapProjection {

    private GPMapView mapView;

    public GPMapProjection(GPMapView mapView) {
        this.mapView = mapView;
    }

    public Coordinate fromPixels(int x, int y, int zoom) {
        double[] meters = MercatorUtils.pixelsToMeters(x, y, zoom, Tile.SIZE);
        double lon = MercatorUtils.metersXToLongitude(meters[0]);
        double lat = MercatorUtils.metersYToLatitude(meters[1]);
        long mapSize = MercatorProjection.getMapSize((byte) zoom);
        GeoPoint point = MercatorProjection.fromPixels(x, y, mapSize);
        return new Coordinate(point.getLongitude(), point.getLatitude());
    }

    public void toPixels(Coordinate coordinate, Point point, int zoom) {
        double x = MercatorProjection.longitudeToPixelX(coordinate.x, zoom);
        double y = MercatorProjection.latitudeToPixelY(coordinate.y, zoom);
        point.x = (int) x;
        point.y = (int) y;
    }
}
