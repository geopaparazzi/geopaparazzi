package eu.geopaparazzi.map;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.nsd.NsdServiceInfo;
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
import org.oscim.layers.LocationLayer;
import org.oscim.layers.LocationTextureLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.renderer.bucket.TextureItem;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.utils.ColorUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.map.layers.GpsPositionLayer;
import eu.geopaparazzi.map.layers.NotesLayer;

public class GPMapView extends org.oscim.android.MapView {
    private GpsServiceStatus lastGpsServiceStatus;
    private float[] lastGpsPositionExtras;
    private int[] lastGpsStatusExtras;
    private double[] lastGpsPosition;
    private GpsPositionLayer locationLayer;
    private NotesLayer notesLayer;
    private final SharedPreferences peferences;

    public GPMapView(Context context) {
        super(context);

        peferences = PreferenceManager.getDefaultSharedPreferences(context);

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
            VectorTileLayer tileLayer = map().setBaseMap(tileSource);

            // Building layer
            map().layers().add(new BuildingLayer(map(), tileLayer));

            // Label layer
            map().layers().add(new LabelLayer(map(), tileLayer));

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
                .fillAlpha(0.6f)
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

        map().layers().add(vectorLayer);
    }

    public void toggleNotesLayer(boolean enable) {
        if (enable) {
            if (notesLayer == null) {
                notesLayer = new NotesLayer(this);
                map().layers().add(notesLayer);
            }
            notesLayer.enable();
        } else {
            if (notesLayer != null)
                notesLayer.disable();
        }
    }


    public void toggleLocationLayer(boolean enable) {
        if (enable) {
            try {
                if (locationLayer == null) {
                    locationLayer = new GpsPositionLayer(this);
                    map().layers().add(locationLayer);
                }
                locationLayer.enable();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (locationLayer != null) {
                locationLayer.disable();
            }
        }

    }

    /**
     * Get a gps status update.
     *
     * @param lastGpsServiceStatus
     * @param lastGpsPosition       [lon, lat, elev]
     * @param lastGpsPositionExtras [accuracy, speed, bearing]
     * @param lastGpsStatusExtras   [maxSatellites, satCount, satUsedInFixCount]
     */
    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras) {
        this.lastGpsServiceStatus = lastGpsServiceStatus;
        this.lastGpsPositionExtras = lastGpsPositionExtras;
        this.lastGpsStatusExtras = lastGpsStatusExtras;
        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
            this.lastGpsPosition = lastGpsPosition;

            if (locationLayer != null && locationLayer.isEnabled()) {
                locationLayer.setGpsStatus(lastGpsServiceStatus, lastGpsPosition, lastGpsPositionExtras, lastGpsStatusExtras);
            }
        }
    }
}
