package eu.geopaparazzi.map.layers;

import android.content.Context;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.layers.LocationTextureLayer;
import org.oscim.map.Map;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.renderer.bucket.TextureItem;

import java.io.IOException;

import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.vtm.VtmUtilities;

public class GpsPositionLayer extends LocationTextureLayer {
    private static TextureRegion activeTexture;
    private static TextureRegion staleTexture;
    private static TextureRegion movingTexture;

    public GpsPositionLayer(GPMapView mapView, Context context) throws IOException {
        super(mapView.map(), createTextures(context));

        // set color of accuracy circle (Color.BLUE is default)
        locationRenderer.setAccuracyColor(Color.get(50, 50, 255));

        // set color of indicator circle (Color.RED is default)
        locationRenderer.setIndicatorColor(Color.MAGENTA);

        // set billboard rendering for TextureRegion (false is default)
        locationRenderer.setBillboard(false);

        setEnabled(true);
    }

    private static TextureRegion createTextures(Context context) throws IOException {
        Bitmap activeGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_black_24dp);
        Bitmap staleGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_grey_24dp);
        Bitmap movingGpsBitmap = VtmUtilities.getBitmapFromResource(context, eu.geopaparazzi.library.R.drawable.ic_my_location_moving_24dp);
        activeTexture = new TextureRegion(new TextureItem(activeGpsBitmap), new TextureAtlas.Rect(0, 0, activeGpsBitmap.getWidth(), activeGpsBitmap.getHeight()));
        staleTexture = new TextureRegion(new TextureItem(staleGpsBitmap), new TextureAtlas.Rect(0, 0, staleGpsBitmap.getWidth(), staleGpsBitmap.getHeight()));
        movingTexture = new TextureRegion(new TextureItem(movingGpsBitmap), new TextureAtlas.Rect(0, 0, movingGpsBitmap.getWidth(), movingGpsBitmap.getHeight()));
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

    public void disable(){
        setEnabled(false);
    }


}
