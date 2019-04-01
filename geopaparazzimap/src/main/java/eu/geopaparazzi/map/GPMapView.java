package eu.geopaparazzi.map;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class GPMapView extends org.oscim.android.MapView {
    public GPMapView(Context context) {
        super(context);
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

    public void setBaseMap(String mapPath) {
        MapFileTileSource tileSource = new MapFileTileSource();
        if (tileSource.setMapFile(mapPath)) {
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
}
