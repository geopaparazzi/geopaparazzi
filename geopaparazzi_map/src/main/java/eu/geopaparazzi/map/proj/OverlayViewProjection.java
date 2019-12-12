package eu.geopaparazzi.map.proj;

import android.graphics.Point;
import android.view.View;

import org.locationtech.jts.geom.Coordinate;
import org.oscim.core.MercatorProjection;

import eu.geopaparazzi.map.GPMapPosition;
import eu.geopaparazzi.map.GPMapView;

/**
 * @author mapsforge
 */
public class OverlayViewProjection {
    private static final String INVALID_MAP_VIEW_DIMENSIONS = "invalid MapView dimensions"; //NON-NLS

    private final GPMapView mapView;

    private View view;

    /**
     * Constructor.
     *
     * @param mapView the map view.
     * @param view    the parent view.
     */
    public OverlayViewProjection(GPMapView mapView, View view) {
        this.mapView = mapView;
        this.view = view;
    }

    public Coordinate fromPixels(int x, int y) {
        if (this.view.getWidth() <= 0 || this.view.getHeight() <= 0) {
            return null;
        }

        GPMapPosition mapPosition = this.mapView.getMapPosition();

        // calculate the pixel coordinates of the top left corner
        byte zoomLevel = (byte) mapPosition.getZoomLevel();
        double pixelX = MercatorProjection.longitudeToPixelX(mapPosition.getLongitude(), zoomLevel);
        double pixelY = MercatorProjection.latitudeToPixelY(mapPosition.getLatitude(), zoomLevel);
        pixelX -= this.view.getWidth() >> 1;
        pixelY -= this.view.getHeight() >> 1;

        // convert the pixel coordinates to a GeoPoint and return it
        long mapSize = MercatorProjection.getMapSize(zoomLevel);
        double lat = MercatorProjection.pixelYToLatitude(pixelY + y, mapSize);
        double lon = MercatorProjection.pixelXToLongitude(pixelX + x, mapSize);

        if (lat < -90 || lat > 90) {
            return new Coordinate(0, 0);
        }
        if (lon < -180 || lon > 180) {
            return new Coordinate(0, 0);
        }
        return new Coordinate(lon, lat);
    }

    public int getLatitudeSpan() {
        if (this.view.getWidth() > 0 && this.view.getHeight() > 0) {
            Coordinate top = fromPixels(0, 0);
            Coordinate bottom = fromPixels(0, this.view.getHeight());
            return (int) Math.abs(top.y - bottom.y);
        }
        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
    }

    public int getLongitudeSpan() {
        if (this.view.getWidth() > 0 && this.view.getHeight() > 0) {
            Coordinate left = fromPixels(0, 0);
            Coordinate right = fromPixels(this.view.getWidth(), 0);
            return (int) Math.abs(left.x - right.x);
        }
        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
    }

    public float metersToPixels(float meters, byte zoom) {
        double groundResolution = MercatorProjection.groundResolution(mapView.map().getMapPosition());
        return (float) (meters * (1 / groundResolution));
    }

    public Point toPixels(Coordinate in, Point out) {
        if (this.view.getWidth() <= 0 || this.view.getHeight() <= 0) {
            return null;
        }

        GPMapPosition mapPosition = this.mapView.getMapPosition();
        byte zoomLevel = (byte) mapPosition.getZoomLevel();
        // calculate the pixel coordinates of the top left corner
        double pixelX = MercatorProjection.longitudeToPixelX(mapPosition.getLongitude(), zoomLevel);
        double pixelY = MercatorProjection.latitudeToPixelY(mapPosition.getLatitude(), zoomLevel);
        pixelX -= this.view.getWidth() >> 1;
        pixelY -= this.view.getHeight() >> 1;

        if (out == null) {
            // create a new point and return it
            out = new Point();
        }

        // reuse the existing point
        out.x = (int) (MercatorProjection.longitudeToPixelX(in.x, zoomLevel) - pixelX);
        out.y = (int) (MercatorProjection.latitudeToPixelY(in.y, zoomLevel) - pixelY);
        return out;
    }

    public Point toPoint(Coordinate in, Point out, byte zoom) {
        if (out == null) {
            // create a new point and return it
            return new Point((int) MercatorProjection.longitudeToPixelX(in.x, zoom),
                    (int) MercatorProjection.latitudeToPixelY(in.y, zoom));
        }

        // reuse the existing point
        out.x = (int) MercatorProjection.longitudeToPixelX(in.x, zoom);
        out.y = (int) MercatorProjection.latitudeToPixelY(in.y, zoom);
        return out;
    }
}