package eu.geopaparazzi.library.routing.google;

import java.io.Serializable;

public class Location implements Serializable {

    private double latitude;

    private double longitude;

    public Location() {

    }

    public Location( Double latitude, Double longitude ) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }
}