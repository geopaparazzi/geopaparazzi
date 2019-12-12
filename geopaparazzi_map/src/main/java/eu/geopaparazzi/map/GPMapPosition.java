package eu.geopaparazzi.map;

import org.locationtech.jts.geom.Coordinate;
import org.oscim.core.MapPosition;

public class GPMapPosition {

    MapPosition mapPosition;

    public GPMapPosition(MapPosition mapPosition) {
        this.mapPosition = mapPosition;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(mapPosition.getLongitude(), mapPosition.getLatitude());
    }

    public double getLongitude() {
        return mapPosition.getLongitude();
    }

    public double getLatitude() {
        return mapPosition.getLatitude();
    }

    public int getZoomLevel() {
        return mapPosition.getZoomLevel();
    }

    public void setZoomLevel(int zoom) {
        mapPosition.setZoomLevel(zoom);
    }

    public void setPosition(double lat, double lon) {
        mapPosition.setPosition(lat, lon);
    }
}
