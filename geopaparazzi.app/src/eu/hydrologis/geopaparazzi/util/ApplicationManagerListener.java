package eu.hydrologis.geopaparazzi.util;

import eu.hydrologis.geopaparazzi.gps.GpsLocation;

public interface ApplicationManagerListener {
    
    public void onLocationChanged( GpsLocation loc );

}
