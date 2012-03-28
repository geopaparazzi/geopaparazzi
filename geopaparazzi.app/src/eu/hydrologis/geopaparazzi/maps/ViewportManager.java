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

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.core.GeoPoint;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;

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
        mapsActivity.getMapView().getController().setZoom(zoom);
    }

    public void setBoundsTo( double n, double s, double w, double e ) {
        throw new RuntimeException();
    }

    // public void setCenterTo( double centerX, double centerY, boolean drawIcon ) {
    // if (mapsActivity == null) {
    // return;
    // }
    // mapsActivity.setNewCenter(centerX, centerY, drawIcon);
    // }
    //
    /**
     * Set center coords and zoom ready for the {@link MapsActivity} to focus again.
     * 
     * <p>In {@link MapsActivity} the {@link MapsActivity#onWindowFocusChanged(boolean)}
     * will take care to zoom properly.
     * 
     * @param centerX the lon coordinate. Can be <code>null</code>.
     * @param centerY the lat coordinate. Can be <code>null</code>.
     * @param zoom the zoom. Can be <code>null</code>.
     */
    public void setCenterAndZoomForMapWindowFocus( Double centerX, Double centerY, Integer zoom ) {
        if (mapsActivity == null) {
            return;
        }

        MapView mapsView = mapsActivity.getMapView();
        MapViewPosition mapPosition = mapsView.getMapPosition();
        GeoPoint mapCenter = mapPosition.getMapCenter();
        int zoomLevel = mapPosition.getZoomLevel();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mapsActivity);
        float cx = 0f;
        float cy = 0f;
        if (centerX != null) {
            cx = centerX.floatValue();
        } else {
            cx = (float) (mapCenter.longitudeE6 / LibraryConstants.E6);
        }
        if (centerY != null) {
            cy = centerY.floatValue();
        } else {
            cy = (float) (mapCenter.latitudeE6 / LibraryConstants.E6);
        }
        if (zoom != null) {
            zoomLevel = zoom;
        }
        PositionUtilities.putMapCenterInPreferences(preferences, cx, cy, zoomLevel);
    }

    // public void zoomToSpan( BoundingBoxE6 bbox ) {
    // if (mapsActivity == null) {
    // return;
    // }
    // mapsActivity.getMapController().zoomToSpan(bbox);
    // }

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
        mapsActivity.getMapView().postInvalidate();
    }

    public double[] getCenterLonLat() {
        if (mapsActivity == null) {
            return null;
        }
        return mapsActivity.getCenterLonLat();
    }

    public int getZoom() {
        if (mapsActivity == null) {
            return 0;
        }
        return mapsActivity.getMapView().getMapPosition().getZoomLevel();
    }

}
