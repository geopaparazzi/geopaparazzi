package eu.geopaparazzi.map.layers.systemlayers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.layers.LocationTextureLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.renderer.bucket.TextureItem;

import java.io.IOException;

import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IPositionLayer;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;
import eu.geopaparazzi.map.vtm.VtmUtilities;

public class GpsPositionLayer extends LocationTextureLayer implements IPositionLayer, ISystemLayer {
    public static String NAME = null;
    //    private static TextureRegion activeTexture;
//    private static TextureRegion staleTexture;
//    private static TextureRegion movingTexture;
    private SharedPreferences peferences = null;
    private GPMapView mapView;

    public GpsPositionLayer(GPMapView mapView) throws IOException {
        super(mapView.map());
        getName(mapView.getContext());

        peferences = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
        this.mapView = mapView;

        // set billboard rendering for TextureRegion (false is default)
        locationRenderer.setBillboard(false);

        Bitmap activeGpsBitmap = VtmUtilities.getBitmapFromResource(mapView.getContext(), eu.geopaparazzi.library.R.drawable.ic_my_location_moving_24dp);
        locationRenderer.setBitmapMarker(activeGpsBitmap);
        locationRenderer.setBitmapArrow(activeGpsBitmap);
    }

    public static String getName(Context context) {
        if (NAME == null) {
            NAME = context.getString(R.string.layername_gpsposition);
        }
        return NAME;
    }

//    private static TextureRegion createTextures(Context context) throws IOException {
//        Bitmap activeGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_moving_24dp);
//        return new TextureRegion(new TextureItem(activeGpsBitmap), new TextureAtlas.Rect(0, 0, activeGpsBitmap.getWidth(), activeGpsBitmap.getHeight()));
//        if (staleTexture == null) {
//            Bitmap activeGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_black_24dp);
//            Bitmap staleGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_grey_24dp);
//            Bitmap movingGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_moving_24dp);
//            activeTexture = new TextureRegion(new TextureItem(activeGpsBitmap), new TextureAtlas.Rect(0, 0, activeGpsBitmap.getWidth(), activeGpsBitmap.getHeight()));
//            staleTexture = new TextureRegion(new TextureItem(staleGpsBitmap), new TextureAtlas.Rect(0, 0, staleGpsBitmap.getWidth(), staleGpsBitmap.getHeight()));
//            movingTexture = new TextureRegion(new TextureItem(movingGpsBitmap), new TextureAtlas.Rect(0, 0, movingGpsBitmap.getWidth(), movingGpsBitmap.getHeight()));
//        }
//        return staleTexture;
//    }
//
//    public void setActive() {
//        locationRenderer.setTextureRegion(activeTexture);
//    }
//
//    public void setStale() {
//        locationRenderer.setTextureRegion(staleTexture);
//    }
//
//    public void setMoving() {
//        locationRenderer.setTextureRegion(movingTexture);
//    }


    /**
     * @param lastGpsServiceStatus
     * @param lastGpsPosition       lon, lat, elev
     * @param lastGpsPositionExtras accuracy, speed, bearing.
     * @param lastGpsStatusExtras   maxSatellites, satCount, satUsedInFixCount.
     * @param lastGpsLoggingStatus
     */
    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras, GpsLoggingStatus lastGpsLoggingStatus) {
        // TODO check if this makes everything better
//        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
//            if (lastGpsPositionExtras != null && lastGpsPositionExtras[2] != 0) {
//                setMoving();
//            } else {
//                setActive();
//            }
//        } else {
//            setStale();
//        }

        float bearing = 0;
        float accuracy = 0;
        if (lastGpsPositionExtras != null) {
            bearing = lastGpsPositionExtras[2];
            accuracy = lastGpsPositionExtras[0];
            bearing = 360f - bearing;
            boolean ignoreAccuracy = peferences.getBoolean(LibraryConstants.PREFS_KEY_IGNORE_GPS_ACCURACY, false);
            if (ignoreAccuracy) {
                accuracy = 0;
            }
        }
        if (lastGpsPosition != null)
            setPosition(lastGpsPosition[1], lastGpsPosition[0], accuracy);
    }


    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }


    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public void load() {
        Layers layers = map().layers();
        layers.add(this, LayerGroups.GROUP_SYSTEM_TOP.getGroupId());
    }

    @Override
    public void reloadData() {
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return toDefaultJson();
    }

    @Override
    public void dispose() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }
}
