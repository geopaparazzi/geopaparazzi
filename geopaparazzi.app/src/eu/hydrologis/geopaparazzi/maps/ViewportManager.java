/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps;

import org.osmdroid.util.BoundingBoxE6;

/**
 * Singleton that takes care of viewport sync.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum ViewportManager {
    INSTANCE;

    private MapsActivity mapsActivity;

    private ViewportManager() {
    }

    public void setMapActivity( MapsActivity mapsActivity ) {
        this.mapsActivity = mapsActivity;
    }

    public void setZoomTo( int zoom ) {
        if (mapsActivity == null) {
            return;
        }
        // mapsActivity.setZoomGuiText(zoom);
        mapsActivity.getMapController().setZoom(zoom);
    }

    public void setBoundsTo( double n, double s, double w, double e ) {
        throw new RuntimeException();
    }

    public void setCenterTo( double centerX, double centerY, boolean drawIcon ) {
        if (mapsActivity == null) {
            return;
        }
        mapsActivity.setNewCenter(centerX, centerY, drawIcon);
    }

    public void setCenterAndZoomTo( double centerX, double centerY, int zoom ) {
        if (mapsActivity == null) {
            return;
        }
        mapsActivity.setNewCenterAtZoom(centerX, centerY, zoom);
    }

    public void zoomToSpan( BoundingBoxE6 bbox ) {
        if (mapsActivity == null) {
            return;
        }
        mapsActivity.getMapController().zoomToSpan(bbox);
    }

    public void invalidateMap() {
        if (mapsActivity == null) {
            return;
        }
        mapsActivity.inalidateMap();
    }
    public void postInvalidateMap() {
        if (mapsActivity == null) {
            return;
        }
        mapsActivity.getMapsView().postInvalidate();
    }

    public double[] getCenterLonLat() {
        if (mapsActivity == null) {
            return null;
        }
        return mapsActivity.getCenterLonLat();
    }

}
