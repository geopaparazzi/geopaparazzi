package eu.geopaparazzi.map;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.View;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.layers.Layer;
import org.oscim.map.Layers;
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
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;
import eu.geopaparazzi.map.layers.systemlayers.BookmarkLayer;
import eu.geopaparazzi.map.layers.systemlayers.GpsPositionTextLayer;

public class GPMapView extends org.oscim.android.MapView {
    public static interface GPMapUpdateListener {
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
        if (mapUpdateListeners.contains(mapUpdateListener)) {
            mapUpdateListeners.remove(mapUpdateListener);
        }
    }

    public void repaint() {
        invalidate();
    }

    public void setClickable(boolean b) {
        super.setClickable(b);
    }

    public void setOnTouchListener(OnTouchListener l) {
        super.setOnTouchListener(l);
    }

    public GPLayers getLayers() {
        return new GPLayers(this);
    }

    public void setTheme(GPMapThemes aDefault) {
        for (VtmThemes value : VtmThemes.values()) {
            if (value.name().equals(aDefault.name())) {
                map().setTheme(value);
                return;
            }
        }
    }

    /**
     * @return the position of the first system layer. from there only system layers should be.
     */
    public int getSystemLayersIndex() {
        Layers layers = map().layers();
        int index = 0;
        for (Layer layer : layers) {
            if (layer instanceof ISystemLayer) {
                return index;
            }
            index++;
        }
        return index;
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

//    public void addVectorTilesLayer(String url, String tilePath) {
//
//        UrlTileSource tileSource = VectorTilesOnlineSource.builder()
////                .apiKey("xxxxxxx") // Put a proper API key
//                .url(url).tilePath(tilePath)
//                .zoomMin(0).zoomMax(20)
//                .httpFactory(new OkHttpEngine.OkHttpFactory())
//                //.locale("en")
//                .build();
//
////        if (USE_CACHE) {
////            // Cache the tiles into a local SQLite database
////            mCache = new TileCache(this, null, "tile.db");
////            mCache.setCacheSize(512 * (1 << 10));
////            tileSource.setCache(mCache);
////        }
//
//        VectorTileLayer l = new OsmTileLayer(map());
//        l.setTileSource(tileSource);
//        l.setEnabled(true);
//        Layers layers = map().layers();
//        layers.add(l, OVERLAYS);
//    }
//
//    public void addSpatialDbLayer(ASpatialDb spatialDb, String tableName) throws Exception {
//        VectorLayer vectorLayer = new VectorLayer(mMap);
//
//        Style pointStyle = Style.builder()
//                .buffer(0.5)
//                .fillColor(Color.RED)
//                .fillAlpha(0.2f).buffer(Math.random() + 0.2)
//                .fillColor(ColorUtil.setHue(Color.RED,
//                        (int) (Math.random() * 50) / 50.0))
//                .fillAlpha(0.5f)
//                .build();
//        Style lineStyle = Style.builder()
//                .strokeColor("#FF0000")
//                .strokeWidth(3f)
//                .cap(Paint.Cap.ROUND)
//                .build();
//        Style polygonStyle = Style.builder()
//                .strokeColor("#0000FF")
//                .strokeWidth(3f)
//                .fillColor("#0000FF")
//                .fillAlpha(0.0f)
//                .cap(Paint.Cap.ROUND)
//                .build();
//        List<Geometry> geoms = spatialDb.getGeometriesIn(tableName, (Envelope) null, null);
//        for (Geometry geom : geoms) {
//            EGeometryType type = EGeometryType.forGeometry(geom);
//            if (type == EGeometryType.POINT || type == EGeometryType.MULTIPOINT) {
//                int numGeometries = geom.getNumGeometries();
//                for (int i = 0; i < numGeometries; i++) {
//                    Geometry geometryN = geom.getGeometryN(i);
//                    Coordinate c = geometryN.getCoordinate();
//                    vectorLayer.add(new PointDrawable(c.y, c.x, pointStyle));
//                }
//            } else if (type == EGeometryType.LINESTRING || type == EGeometryType.MULTILINESTRING) {
//                int numGeometries = geom.getNumGeometries();
//                for (int i = 0; i < numGeometries; i++) {
//                    Geometry geometryN = geom.getGeometryN(i);
//                    vectorLayer.add(new LineDrawable(geometryN, lineStyle));
//                }
//            } else if (type == EGeometryType.POLYGON || type == EGeometryType.MULTIPOLYGON) {
//                int numGeometries = geom.getNumGeometries();
//                for (int i = 0; i < numGeometries; i++) {
//                    Geometry geometryN = geom.getGeometryN(i);
//                    vectorLayer.add(new PolygonDrawable(geometryN, polygonStyle));
//                }
//            }
//        }
//        vectorLayer.update();
//
//        Layers layers = map().layers();
//        layers.add(vectorLayer, OVERLAYS);
////        map().layers().add(vectorLayer);
//    }

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
}
