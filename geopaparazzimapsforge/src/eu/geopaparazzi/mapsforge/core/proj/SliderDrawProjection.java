///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.geopaparazzi.mapsforge.core.proj;
//
//import android.view.View;
//
//import org.mapsforge.core.model.LatLong;
//import org.mapsforge.core.model.MapPosition;
//import org.mapsforge.core.model.Point;
//import org.mapsforge.core.util.MercatorProjection;
//import org.mapsforge.map.android.view.MapView;
//import org.mapsforge.map.model.IMapViewPosition;
//import org.mapsforge.map.util.MapViewProjection;
//
//
///**
// * @author mapsforge
// */
//public class SliderDrawProjection extends MapViewProjection {
//    private static final String INVALID_MAP_VIEW_DIMENSIONS = "invalid MapView dimensions";
//
//    private final MapView mapView;
//
//    private View view;
//
//    /**
//     * Constructor.
//     *
//     * @param mapView the map view.
//     * @param view    the parent view.
//     */
//    public SliderDrawProjection(MapView mapView, View view) {
//        super(mapView);
//        this.mapView = mapView;
//        this.view = view;
//    }
//
//    public LatLong fromPixels(double x, double y) {
//        if (this.view.getWidth() <= 0 || this.view.getHeight() <= 0) {
//            return null;
//        }
//
//        IMapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;
//
//        // calculate the pixel coordinates of the top left corner
//        LatLong geoPoint = mapPosition.getCenter();
//        double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.getZoomLevel());
//        double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.getZoomLevel());
//        pixelX -= this.view.getWidth() >> 1;
//        pixelY -= this.view.getHeight() >> 1;
//
//        // convert the pixel coordinates to a GeoPoint and return it
//        double lat = MercatorProjection.pixelYToLatitude(pixelY + y, mapPosition.getZoomLevel());
//        double lon = MercatorProjection.pixelXToLongitude(pixelX + x, mapPosition.getZoomLevel());
//
//        if (lat < -90 || lat > 90) {
//            return new LatLong(0, 0);
//        }
//        if (lon < -180 || lon > 180) {
//            return new LatLong(0, 0);
//        }
//        return new LatLong(lat, lon);
//    }
//
//    public double getLatitudeSpan() {
//        if (this.view.getWidth() > 0 && this.view.getHeight() > 0) {
//            LatLong top = fromPixels(0, 0);
//            LatLong bottom = fromPixels(0, this.view.getHeight());
//            return Math.abs(top.getLatitudeE6() - bottom.getLatitudeE6());
//        }
//        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
//    }
//
//    public double getLongitudeSpan() {
//        if (this.view.getWidth() > 0 && this.view.getHeight() > 0) {
//            LatLong left = fromPixels(0, 0);
//            LatLong right = fromPixels(this.view.getWidth(), 0);
//            return Math.abs(left.getLongitudeE6() - right.getLongitudeE6());
//        }
//        throw new IllegalStateException(INVALID_MAP_VIEW_DIMENSIONS);
//    }
//
//    public float metersToPixels(float meters, byte zoom) {
//        IMapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;
//        double latitude = mapPosition.getCenter().getLatitude();
//        double groundResolution = MercatorProjection.calculateGroundResolution(latitude, zoom);
//        return (float) (meters * (1 / groundResolution));
//    }
//
//    public Point toPixels(LatLong in) {
//        if (this.view.getWidth() <= 0 || this.view.getHeight() <= 0) {
//            return null;
//        }
//        IMapViewPosition mapPosition = this.mapView.getModel().mapViewPosition;
//
//        // calculate the pixel coordinates of the top left corner
//        LatLong geoPoint = mapPosition.getCenter();
//        double pixelX = MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(), mapPosition.getZoomLevel());
//        double pixelY = MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(), mapPosition.getZoomLevel());
//        pixelX -= this.view.getWidth() >> 1;
//        pixelY -= this.view.getHeight() >> 1;
//
//        // create a new point and return it
//
//        return new Point((int) (MercatorProjection.longitudeToPixelX(in.longitude, mapPosition.getZoomLevel()) - pixelX),
//                (int) (MercatorProjection.latitudeToPixelY(in.latitude, mapPosition.getZoomLevel()) - pixelY));
//    }
//
//    public Point toPoint(LatLong in, byte zoom) {
//        // create a new point and return it
//        return new Point((int) MercatorProjection.longitudeToPixelX(in.longitude, zoom),
//                (int) MercatorProjection.latitudeToPixelY(in.latitude, zoom));
//    }
//}
