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

import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_ZOOM;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.hydrologis.geopaparazzi.util.Constants;

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

        MapView mapsView = mapsActivity.getMapsView();
        IGeoPoint mapCenter = mapsView.getMapCenter();
        int zoomLevel = mapsView.getZoomLevel();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mapsActivity);
        Editor editor = preferences.edit();
        if (centerX != null) {
            editor.putFloat(Constants.PREFS_KEY_MAPCENTER_LON, centerX.floatValue());
        } else {
            editor.putFloat(Constants.PREFS_KEY_MAPCENTER_LON, (float) (mapCenter.getLongitudeE6() / LibraryConstants.E6));
        }
        if (centerY != null) {
            editor.putFloat(Constants.PREFS_KEY_MAPCENTER_LAT, centerY.floatValue());
        } else {
            editor.putFloat(Constants.PREFS_KEY_MAPCENTER_LAT, (float) (mapCenter.getLatitudeE6() / LibraryConstants.E6));
        }
        if (zoom != null) {
            editor.putInt(PREFS_KEY_ZOOM, zoom);
        } else {
            editor.putInt(PREFS_KEY_ZOOM, zoomLevel);
        }
        editor.commit();
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
        mapsActivity.getMapsView().postInvalidate();
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
        return mapsActivity.getMapsView().getZoomLevel();
    }

}
