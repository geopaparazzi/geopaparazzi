package eu.geopaparazzi.map;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.View;

import org.oscim.backend.canvas.Color;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.layers.Layer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.VtmThemes;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;
import eu.geopaparazzi.map.layers.interfaces.IPositionLayer;
import eu.geopaparazzi.map.layers.systemlayers.GpsPositionTextLayer;

public class GPMapView extends org.oscim.android.MapView {
    public interface GPMapUpdateListener {
        void onUpdate(GPMapPosition mapPosition);
    }

    private GpsServiceStatus lastGpsServiceStatus;
    private float[] lastGpsPositionExtras;
    private int[] lastGpsStatusExtras;
    private double[] lastGpsPosition;
    private final SharedPreferences peferences;


    private float lastUsedBearing = -1;
    private View editingView;

    private List<GPMapUpdateListener> mapUpdateListeners = new ArrayList<>();

    public GPMapView(Context context) {
        super(context);

        MapRenderer.setBackgroundColor(Color.WHITE);
        peferences = PreferenceManager.getDefaultSharedPreferences(context);

        map().events.bind((e, mapPosition) -> {
            for (GPMapUpdateListener mapUpdateListener : mapUpdateListeners) {
                mapUpdateListener.onUpdate(new GPMapPosition(mapPosition));
            }
        });

        LayerManager.INSTANCE.createGroups(this);
    }

    public void addMapUpdateListener(GPMapUpdateListener mapUpdateListener) {
        if (!mapUpdateListeners.contains(mapUpdateListener)) {
            mapUpdateListeners.add(mapUpdateListener);
        }
    }

    public void removeMapUpdateListener(GPMapUpdateListener mapUpdateListener) {
        mapUpdateListeners.remove(mapUpdateListener);
    }

    public void setClickable(boolean b) {
        super.setClickable(b);
    }

    public void setOnTouchListener(OnTouchListener l) {
        super.setOnTouchListener(l);
    }

    public void setTheme(GPMapThemes aDefault) {
        for (VtmThemes value : VtmThemes.values()) {
            if (value.name().equals(aDefault.name())) {
                map().setTheme(value);
                return;
            }
        }
    }

    public void setScaleX(float mapScaleX) {
        super.setScaleX(mapScaleX);
    }

    public void setScaleY(float mapScaleY) {
        super.setScaleY(mapScaleY);
    }

    public void destroyAll() {
        onDestroy();
    }

    public GPMapPosition getMapPosition() {
        MapPosition mapPosition = map().getMapPosition();
        return new GPMapPosition(mapPosition);
    }

    public void setMapPosition(GPMapPosition mapPosition) {
        map().setMapPosition(mapPosition.mapPosition);
    }

    public GPBBox getBoundingBox() {
        BoundingBox bb = map().getBoundingBox(0);
        return new GPBBox(bb.getMinLatitude(), bb.getMinLongitude(), bb.getMaxLatitude(), bb.getMaxLongitude());
    }

    public int getViewportWidth() {
        return super.getWidth();
    }

    public int getViewportHeight() {
        return super.getHeight();
    }

    public float getScaleX() {
        return super.getScaleX();
    }

    public float getScaleY() {
        return super.getScaleY();
    }

    public void saveMapView(File imageFile) throws Exception {
        Rect t = new Rect();
        super.getDrawingRect(t);
        Bitmap bufferedBitmap = Bitmap.createBitmap(t.width(), t.height(), Bitmap.Config.ARGB_8888);
        Canvas bufferedCanvas = new Canvas(bufferedBitmap);
        super.draw(bufferedCanvas);
        FileOutputStream out = new FileOutputStream(imageFile);
        bufferedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        out.close();
    }

    public List<IGpLayer> getLayers() {
        List<IGpLayer> layerList = new ArrayList<>();
        for (Layer layer : map().layers()) {
            if (layer instanceof IGpLayer) {
                IGpLayer gpLayer = (IGpLayer) layer;
                layerList.add(gpLayer);
            }
        }
        return layerList;
    }


    /**
     * Get a gps status update.
     *
     * @param lastGpsServiceStatus
     * @param lastGpsPosition       [lon, lat, elev]
     * @param lastGpsPositionExtras [accuracy, speed, bearing]
     * @param lastGpsStatusExtras   [maxSatellites, satCount, satUsedInFixCount]
     * @param lastGpsLoggingStatus
     */
    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras, GpsLoggingStatus lastGpsLoggingStatus) {
        this.lastGpsServiceStatus = lastGpsServiceStatus;
        this.lastGpsPositionExtras = lastGpsPositionExtras;
        this.lastGpsStatusExtras = lastGpsStatusExtras;
        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
            this.lastGpsPosition = lastGpsPosition;
        }

        for (Layer layer : map().layers()) {
            if (layer instanceof IPositionLayer) {
                IPositionLayer positionLayer = (IPositionLayer) layer;
                positionLayer.setGpsStatus(lastGpsServiceStatus, lastGpsPosition, lastGpsPositionExtras, lastGpsStatusExtras, lastGpsLoggingStatus);
            }
        }

        if (isRotationGestureEnabled()) {
            boolean centerOnGps = peferences.getBoolean(LibraryConstants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
            if (centerOnGps) {
                GPMapPosition mapPosition = getMapPosition();
                mapPosition.setPosition(lastGpsPosition[1], lastGpsPosition[0]);
                setMapPosition(mapPosition);
                saveCenterPref();
            }
            boolean rotateWithGps = peferences.getBoolean(LibraryConstants.PREFS_KEY_ROTATE_MAP_WITH_GPS, false);
            if (rotateWithGps && lastGpsPositionExtras != null) {
                float bearing = lastGpsPositionExtras[2];
                if (bearing == 0 && lastUsedBearing != -1) {
                    bearing = lastUsedBearing;
                } else {
                    lastUsedBearing = bearing;
                }

                float mapBearing = 360f - bearing;
                map().viewport().setRotation(mapBearing);
            }
        }

    }

    public void toggleLocationTextLayer(boolean showGpsInfo) {
        for (Layer layer : map().layers()) {
            if (layer instanceof GpsPositionTextLayer) {
                GpsPositionTextLayer positionTextLayer = (GpsPositionTextLayer) layer;
                positionTextLayer.setEnabled(showGpsInfo);
                break;
            }
        }
    }

    /**
     * Reload the data of a layer type.
     *
     * @param layerClass teh class of the layer to reload.
     */
    public void reloadLayer(Class<? extends IGpLayer> layerClass) throws Exception {
        for (Layer layer : map().layers()) {
            if (layer.getClass().isAssignableFrom(layerClass)) {
                ((IGpLayer) layer).reloadData();
            }
        }
    }

    /**
     * Save the current mapview position to preferences.
     *
     * @param lonLatZoom optional position + zoom. If null, position is taken from the mapview.
     */
    private synchronized void saveCenterPref(double... lonLatZoom) {
        double lon;
        double lat;
        int zoom;
        if (lonLatZoom != null && lonLatZoom.length == 3) {
            lon = lonLatZoom[0];
            lat = lonLatZoom[1];
            zoom = (int) lonLatZoom[2];
        } else {
            GPMapPosition mapPosition = getMapPosition();
            lat = mapPosition.getLatitude();
            lon = mapPosition.getLongitude();
            zoom = mapPosition.getZoomLevel();
        }

        if (GPLog.LOG_ABSURD) {
            String sb = "Map Center moved: " + //$NON-NLS-1$
                    lon +
                    "/" + //$NON-NLS-1$
                    lat;
            GPLog.addLogEntry(this, sb);
        }

        PositionUtilities.putMapCenterInPreferences(peferences, lon, lat, zoom);
    }

    public void setEditingView(View editingView) {
        this.editingView = editingView;
    }

    public void enableRotationGesture(boolean enable) {
        map().getEventLayer().enableRotation(enable);
    }

    public boolean isRotationGestureEnabled() {
        return map().getEventLayer().rotationEnabled();
    }

    public void enableTiltGesture(boolean enable) {
        map().getEventLayer().enableTilt(enable);
    }

    public void setMapRotation(double degrees) {
        map().viewport().setRotation(degrees);
    }

    public void setMapTilt(double tilt) {
        map().viewport().setTilt((float) tilt);
    }

    public void blockMap(boolean resetRotation, boolean resetTilt) {
        if (resetRotation) {
            setMapRotation(0.0d);
        }
        if (resetTilt) {
            setMapTilt(0);
        }
        enableRotationGesture(false);
        enableTiltGesture(false);
    }

    public void blockMap() {
        blockMap(true, true);
    }

    public void releaseMapBlock() {
        enableRotationGesture(true);
        enableTiltGesture(true);
    }
}

