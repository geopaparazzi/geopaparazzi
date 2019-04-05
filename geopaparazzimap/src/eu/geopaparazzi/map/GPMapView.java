package eu.geopaparazzi.map;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.preference.PreferenceManager;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.utils.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Layers;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.utils.ColorUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.map.layers.CurrentGpsLogLayer;
import eu.geopaparazzi.map.layers.GpsLogsLayer;
import eu.geopaparazzi.map.layers.GpsPositionLayer;
import eu.geopaparazzi.map.layers.GpsPositionTextLayer;
import eu.geopaparazzi.map.layers.ImagesLayer;
import eu.geopaparazzi.map.layers.MBTilesTileSource;
import eu.geopaparazzi.map.layers.NotesLayer;

public class GPMapView extends org.oscim.android.MapView {
    private GpsServiceStatus lastGpsServiceStatus;
    private float[] lastGpsPositionExtras;
    private int[] lastGpsStatusExtras;
    private double[] lastGpsPosition;
    private GpsPositionLayer locationLayer;
    private NotesLayer notesLayer;
    private final SharedPreferences peferences;
    private GpsLogsLayer gpsLogsLayer;
    private ImagesLayer imagesLayer;
    private GpsPositionTextLayer locationTextLayer;

    public static final int MAPSFORGE_PRE = 0;
    public static final int OVERLAYS = 1;
    public static final int GEOPAPARAZZI = 2;
    public static final int MAPSFORGE_POST = 3;
    public static final int SYSTEM = 4;
    public static final int ON_TOP_GEOPAPARAZZI = 5;

    private float lastUsedBearing = -1;
    private CurrentGpsLogLayer currentGpsLogLayer;

    public GPMapView(Context context) {
        super(context);

        peferences = PreferenceManager.getDefaultSharedPreferences(context);

        Layers layers = map().layers();
        layers.addGroup(MAPSFORGE_PRE);
        layers.addGroup(OVERLAYS);
        layers.addGroup(GEOPAPARAZZI);
        layers.addGroup(MAPSFORGE_POST);
        layers.addGroup(SYSTEM);
        layers.addGroup(ON_TOP_GEOPAPARAZZI);

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

    public void setBaseMap(String... mapPaths) {
        MultiMapFileTileSource tileSource = new MultiMapFileTileSource();
        boolean okToLoad = false;
        for (String mapPath : mapPaths) {
            MapFileTileSource ts = new MapFileTileSource();
            okToLoad = ts.setMapFile(mapPath);
            if (okToLoad) {
                tileSource.add(ts);
            }
        }
        if (okToLoad) {
            // Vector layer
            VectorTileLayer tileLayer = new OsmTileLayer(map());
            tileLayer.setTileSource(tileSource);
//            VectorTileLayer tileLayer = map().setBaseMap(tileSource);
            Layers layers = map().layers();
            layers.add(tileLayer, MAPSFORGE_PRE);

            // Building layer
            layers.add(new BuildingLayer(map(), tileLayer), MAPSFORGE_POST);
//            map().layers().add(new BuildingLayer(map(), tileLayer));
            // Label layer
            layers.add(new LabelLayer(map(), tileLayer), MAPSFORGE_POST);
//            map().layers().add(new LabelLayer(map(), tileLayer));

            // Render theme
            setTheme(GPMapThemes.DEFAULT);

        }
    }

    public void addSpatialDbLayer(ASpatialDb spatialDb, String tableName) throws Exception {
        VectorLayer vectorLayer = new VectorLayer(mMap);

        Style pointStyle = Style.builder()
                .buffer(0.5)
                .fillColor(Color.RED)
                .fillAlpha(0.2f).buffer(Math.random() + 0.2)
                .fillColor(ColorUtil.setHue(Color.RED,
                        (int) (Math.random() * 50) / 50.0))
                .fillAlpha(0.5f)
                .build();
        Style lineStyle = Style.builder()
                .strokeColor("#FF0000")
                .strokeWidth(3f)
                .cap(Paint.Cap.ROUND)
                .build();
        Style polygonStyle = Style.builder()
                .strokeColor("#0000FF")
                .strokeWidth(3f)
                .fillColor("#0000FF")
                .fillAlpha(0.0f)
                .cap(Paint.Cap.ROUND)
                .build();
        List<Geometry> geoms = spatialDb.getGeometriesIn(tableName, (Envelope) null, null);
        for (Geometry geom : geoms) {
            EGeometryType type = EGeometryType.forGeometry(geom);
            if (type == EGeometryType.POINT || type == EGeometryType.MULTIPOINT) {
                int numGeometries = geom.getNumGeometries();
                for (int i = 0; i < numGeometries; i++) {
                    Geometry geometryN = geom.getGeometryN(i);
                    Coordinate c = geometryN.getCoordinate();
                    vectorLayer.add(new PointDrawable(c.y, c.x, pointStyle));
                }
            } else if (type == EGeometryType.LINESTRING || type == EGeometryType.MULTILINESTRING) {
                int numGeometries = geom.getNumGeometries();
                for (int i = 0; i < numGeometries; i++) {
                    Geometry geometryN = geom.getGeometryN(i);
                    vectorLayer.add(new LineDrawable(geometryN, lineStyle));
                }
            } else if (type == EGeometryType.POLYGON || type == EGeometryType.MULTIPOLYGON) {
                int numGeometries = geom.getNumGeometries();
                for (int i = 0; i < numGeometries; i++) {
                    Geometry geometryN = geom.getGeometryN(i);
                    vectorLayer.add(new PolygonDrawable(geometryN, polygonStyle));
                }
            }
        }
        vectorLayer.update();

        Layers layers = map().layers();
        layers.add(vectorLayer, OVERLAYS);
//        map().layers().add(vectorLayer);
    }

    public void addMBTilesLayer(String dbPath, Integer alpha, Integer transparentColor) throws Exception {
        BitmapTileLayer bitmapLayer = new BitmapTileLayer(map(), new MBTilesTileSource(dbPath, alpha, transparentColor));
        Layers layers = map().layers();
        layers.add(bitmapLayer, OVERLAYS);
    }

    public void toggleNotesLayer(boolean enable) {
        if (enable) {
            if (notesLayer == null) {
                notesLayer = new NotesLayer(this);
//                map().layers().add(notesLayer);
                Layers layers = map().layers();
                layers.add(notesLayer, ON_TOP_GEOPAPARAZZI);
            }
            notesLayer.enable();
        } else {
            if (notesLayer != null)
                notesLayer.disable();
        }
    }

    public void toggleImagesLayer(boolean enable) {
        if (enable) {
            if (imagesLayer == null) {
                imagesLayer = new ImagesLayer(this);
//                map().layers().add(imagesLayer);
                Layers layers = map().layers();
                layers.add(imagesLayer, ON_TOP_GEOPAPARAZZI);
            }
            imagesLayer.enable();
        } else {
            if (imagesLayer != null)
                imagesLayer.disable();
        }
    }

    public void toggleGpsLogsLayer(boolean enable) {
        if (enable) {
            if (gpsLogsLayer == null) {
                gpsLogsLayer = new GpsLogsLayer(this);
//                map().layers().add(gpsLogsLayer);
                Layers layers = map().layers();
                layers.add(gpsLogsLayer, GEOPAPARAZZI);
            }
            gpsLogsLayer.enable();
        } else {
            if (gpsLogsLayer != null)
                gpsLogsLayer.disable();
        }
    }

    public void toggleCurrentGpsLogLayer(boolean enable) {
        if (enable) {
            if (currentGpsLogLayer == null) {
                currentGpsLogLayer = new CurrentGpsLogLayer(this);
                Layers layers = map().layers();
                layers.add(currentGpsLogLayer, GEOPAPARAZZI);
            }
            currentGpsLogLayer.enable();
        } else {
            if (currentGpsLogLayer != null)
                currentGpsLogLayer.disable();
        }
    }

    public void toggleLocationLayer(boolean enable) {
        if (enable) {
            try {
                if (locationLayer == null) {
                    locationLayer = new GpsPositionLayer(this);
                    locationLayer.enable();
//                    map().layers().add(locationLayer);
                    Layers layers = map().layers();
                    layers.add(locationLayer, SYSTEM);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (locationLayer != null) {
                locationLayer.disable();
            }
        }

    }

    public void toggleLocationTextLayer(boolean enable) {
        if (enable) {
            try {
                if (locationTextLayer == null) {
                    locationTextLayer = new GpsPositionTextLayer(this);
                    locationTextLayer.disable();
//                    map().layers().add(locationTextLayer);
                    Layers layers = map().layers();
                    layers.add(locationTextLayer, SYSTEM);
                }
                if (peferences.getBoolean(LibraryConstants.PREFS_KEY_SHOW_GPS_INFO, false))
                    locationTextLayer.enable();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (locationTextLayer != null) {
                locationTextLayer.disable();
            }
        }

    }

    /**
     * Get a gps status update.
     *  @param lastGpsServiceStatus
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
        if (locationLayer != null) {
            locationLayer.setGpsStatus(lastGpsServiceStatus, lastGpsPosition, lastGpsPositionExtras, lastGpsStatusExtras, lastGpsLoggingStatus);
        }
        if (locationTextLayer != null) {
            locationTextLayer.setGpsStatus(lastGpsServiceStatus, lastGpsPosition, lastGpsPositionExtras, lastGpsStatusExtras, lastGpsLoggingStatus);
        }
        if (currentGpsLogLayer != null) {
            currentGpsLogLayer.setGpsStatus(lastGpsServiceStatus, lastGpsPosition, lastGpsPositionExtras, lastGpsStatusExtras, lastGpsLoggingStatus);
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
            StringBuilder sb = new StringBuilder();
            sb.append("Map Center moved: "); //$NON-NLS-1$
            sb.append(lon);
            sb.append("/"); //$NON-NLS-1$
            sb.append(lat);
            GPLog.addLogEntry(this, sb.toString());
        }

        PositionUtilities.putMapCenterInPreferences(peferences, lon, lat, zoom);
    }
}
