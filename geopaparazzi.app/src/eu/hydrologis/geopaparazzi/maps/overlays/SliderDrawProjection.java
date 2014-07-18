/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps.overlays;

import android.graphics.Point;
import android.view.View;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;

/**
 * @author mapsforge
 *
 */
public class SliderDrawProjection implements Projection {
    private static final String INVALID_MAP_VIEW_DIMENSIONS = "invalid MapView dimensions";

    private final MapView mapView;

    private View view;

    /**
     * Constructor.
     * 
     * @param mapView the map view.
     * @param view the parent view.
     */
    public SliderDrawProjection( MapView mapView, View view ) {
        this.mapView = mapView;
        this.view = view;
    }

    public GeoPoint fromPixels( int x, int y ) {
        if (this.view.getWidth() <= 0 || this.view.getHeight() <= 0) {
            return null;
        }

        MapPosition mapPosition = this.mapView.getMapPosition().getMapPosition();

        // calculate the pixel coordinates of the top left corner
        GeoPoint geoPoint = mapPosition.geoPoint;
        double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.zoomLevel);
        double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.zoomLevel);
        pixelX -= this.view.getWidth() >> 1;
        pixelY -= this.view.getHeight() >> 1;

        // convert the pixel coordinates to a GeoPoint and return it
        double lat = MercatorProjection.pixelYToLatitude(pixelY + y, mapPosition.zoomLevel);
        double lon = MercatorProjection.pixelXToLongitude(pixelX + x, mapPosition.zoomLevel);

        if (lat < -90 || lat > 90) {
            return new GeoPoint(0, 0);
        }
        if (lon < -180 || lon > 180) {
            return new GeoPoint(0, 0);
        }
        return new GeoPoint(lat, lon);
    }

    public int getLatitudeSpan() {
        if (this.view.getWidth() > 0 && this.view.getHeight() > 0) {
            GeoPoint top = fromPixels(0, 0);
            GeoPoint bottom = fromPixels(0, this.view.getHeight());
            return Math.abs(top.latitudeE6 - bottom.latitudeE6);
        }
        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
    }

    public int getLongitudeSpan() {
        if (this.view.getWidth() > 0 && this.view.getHeight() > 0) {
            GeoPoint left = fromPixels(0, 0);
            GeoPoint right = fromPixels(this.view.getWidth(), 0);
            return Math.abs(left.longitudeE6 - right.longitudeE6);
        }
        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
    }

    public float metersToPixels( float meters, byte zoom ) {
        double latitude = this.mapView.getMapPosition().getMapCenter().getLatitude();
        double groundResolution = MercatorProjection.calculateGroundResolution(latitude, zoom);
        return (float) (meters * (1 / groundResolution));
    }

    public Point toPixels( GeoPoint in, Point out ) {
        if (this.view.getWidth() <= 0 || this.view.getHeight() <= 0) {
            return null;
        }

        MapPosition mapPosition = this.mapView.getMapPosition().getMapPosition();

        // calculate the pixel coordinates of the top left corner
        GeoPoint geoPoint = mapPosition.geoPoint;
        double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.zoomLevel);
        double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.zoomLevel);
        pixelX -= this.view.getWidth() >> 1;
        pixelY -= this.view.getHeight() >> 1;

        if (out == null) {
            // create a new point and return it
            return new Point((int) (MercatorProjection.longitudeToPixelX(in.getLongitude(), mapPosition.zoomLevel) - pixelX),
                    (int) (MercatorProjection.latitudeToPixelY(in.getLatitude(), mapPosition.zoomLevel) - pixelY));
        }

        // reuse the existing point
        out.x = (int) (MercatorProjection.longitudeToPixelX(in.getLongitude(), mapPosition.zoomLevel) - pixelX);
        out.y = (int) (MercatorProjection.latitudeToPixelY(in.getLatitude(), mapPosition.zoomLevel) - pixelY);
        return out;
    }

    public Point toPoint( GeoPoint in, Point out, byte zoom ) {
        if (out == null) {
            // create a new point and return it
            return new Point((int) MercatorProjection.longitudeToPixelX(in.getLongitude(), zoom),
                    (int) MercatorProjection.latitudeToPixelY(in.getLatitude(), zoom));
        }

        // reuse the existing point
        out.x = (int) MercatorProjection.longitudeToPixelX(in.getLongitude(), zoom);
        out.y = (int) MercatorProjection.latitudeToPixelY(in.getLatitude(), zoom);
        return out;
    }
}
