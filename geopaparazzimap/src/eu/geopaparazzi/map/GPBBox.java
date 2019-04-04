package eu.geopaparazzi.map;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

public class GPBBox {
    private final BoundingBox bbox;

    public GPBBox(double minLatitude, double minLongitude, double maxLatitude, double maxLongitude) {
        bbox = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
    }


    public double getLongitudeSpan() {
        return bbox.getLongitudeSpan();
    }

    public double getLatitudeSpan() {
        return bbox.getLatitudeSpan();
    }

    public double getMaxLatitude() {
        return bbox.getMaxLatitude();
    }

    public double getMinLatitude() {
        return bbox.getMinLatitude();
    }

    public double getMinLongitude() {
        return bbox.getMinLongitude();
    }

    public double getMaxLongitude() {
        return bbox.getMaxLongitude();
    }

    public boolean contains(double lat, double lon) {
        return bbox.contains(new GeoPoint(lat, lon));
    }
}
