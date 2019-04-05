package eu.geopaparazzi.map.layers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.layers.LocationTextureLayer;
import org.oscim.map.Map;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.renderer.bucket.TextureItem;

import java.io.IOException;

import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.vtm.VtmUtilities;

public class GpsPositionLayer extends LocationTextureLayer {
    private static TextureRegion activeTexture;
    private static TextureRegion staleTexture;
    private static TextureRegion movingTexture;
    private SharedPreferences peferences = null;

    private float lastUsedBearing = -1;

    public GpsPositionLayer(GPMapView mapView) throws IOException {
        super(mapView.map(), createTextures(mapView.getContext()));
        peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());


        // set color of accuracy circle (Color.BLUE is default)
        locationRenderer.setAccuracyColor(Color.get(50, 50, 255));

        // set color of indicator circle (Color.RED is default)
        locationRenderer.setIndicatorColor(Color.MAGENTA);

        // set billboard rendering for TextureRegion (false is default)
        locationRenderer.setBillboard(false);
    }

    private static TextureRegion createTextures(Context context) throws IOException {
        if (staleTexture == null) {
            Bitmap activeGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_black_24dp);
            Bitmap staleGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_grey_24dp);
            Bitmap movingGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_moving_24dp);
            activeTexture = new TextureRegion(new TextureItem(activeGpsBitmap), new TextureAtlas.Rect(0, 0, activeGpsBitmap.getWidth(), activeGpsBitmap.getHeight()));
            staleTexture = new TextureRegion(new TextureItem(staleGpsBitmap), new TextureAtlas.Rect(0, 0, staleGpsBitmap.getWidth(), staleGpsBitmap.getHeight()));
            movingTexture = new TextureRegion(new TextureItem(movingGpsBitmap), new TextureAtlas.Rect(0, 0, movingGpsBitmap.getWidth(), movingGpsBitmap.getHeight()));
        }
        return staleTexture;
    }

    public GpsPositionLayer(Map map, TextureRegion textureRegion) {
        super(map, textureRegion);
    }

    public void setActive() {
        locationRenderer.setTextureRegion(activeTexture);
    }

    public void setStale() {
        locationRenderer.setTextureRegion(staleTexture);
    }

    public void setMoving() {
        locationRenderer.setTextureRegion(movingTexture);
    }


    /**
     * @param lastGpsServiceStatus
     * @param lastGpsPosition       lon, lat, elev
     * @param lastGpsPositionExtras accuracy, speed, bearing.
     * @param lastGpsStatusExtras   maxSatellites, satCount, satUsedInFixCount.
     * @param lastGpsLoggingStatus
     */
    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras, GpsLoggingStatus lastGpsLoggingStatus) {
        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
            if (lastGpsPositionExtras != null && lastGpsPositionExtras[2] != 0) {
                setMoving();
            } else {
                setActive();
            }
        } else if (lastGpsServiceStatus == GpsServiceStatus.GPS_LISTENING__NO_FIX) {
            setStale();
        }

        float bearing = 0;
        float accuracy = 0;
        if (lastGpsPositionExtras != null) {
            bearing = lastGpsPositionExtras[2];
            accuracy = lastGpsPositionExtras[0];
            if (bearing == 0 && lastUsedBearing != -1) {
                bearing = lastUsedBearing;
            } else {
                lastUsedBearing = bearing;
            }

            bearing = 360f - bearing;

            boolean ignoreAccuracy = peferences.getBoolean(LibraryConstants.PREFS_KEY_IGNORE_GPS_ACCURACY, false);
            if (ignoreAccuracy) {
                accuracy = 0;
            }
        }
        if (lastGpsPosition != null)
            setPosition(lastGpsPosition[1], lastGpsPosition[0], bearing, accuracy);
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }
}
