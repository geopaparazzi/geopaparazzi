///*
// * Geopaparazzi - Digital field mapping on Android based devices
// * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package eu.hydrologis.geopaparazzi.maps.overlays;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.osmdroid.ResourceProxy;
//import org.osmdroid.util.BoundingBoxE6;
//import org.osmdroid.util.GeoPoint;
//import org.osmdroid.views.MapView;
//import org.osmdroid.views.MapView.Projection;
//import org.osmdroid.views.overlay.Overlay;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.graphics.Point;
//import android.graphics.Rect;
//import android.view.MotionEvent;
//import eu.geopaparazzi.library.util.LibraryConstants;
//import eu.geopaparazzi.library.util.debug.Debug;
//import eu.geopaparazzi.library.util.debug.Logger;
//import eu.hydrologis.geopaparazzi.R;
//import eu.hydrologis.geopaparazzi.database.DaoImages;
//import eu.hydrologis.geopaparazzi.maps.DataManager;
//import eu.hydrologis.geopaparazzi.util.Image;
//
///**
// * Overlay to show images.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//@SuppressWarnings("nls")
//public class ImagesOverlay extends Overlay {
//
//    private static Bitmap imageIcon;
//    private static int imageIconWidth;
//    private static int imageIconHeight;
//
//    private Context context;
//
//    final private Rect screenRect = new Rect();
//
//    private boolean touchDragging = false;
//    private boolean doDraw = true;
//    private boolean gpsUpdate = false;
//
//    private List<Image> imageInWorldBounds = new ArrayList<Image>();
//
//    public ImagesOverlay( final Context ctx, final ResourceProxy pResourceProxy ) {
//        super(pResourceProxy);
//        this.context = ctx;
//
//        if (imageIcon == null) {
//            imageIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.image);
//            imageIconWidth = imageIcon.getWidth();
//            imageIconHeight = imageIcon.getHeight();
//        }
//
//    }
//
//    public void setDoDraw( boolean doDraw ) {
//        this.doDraw = doDraw;
//        if (Debug.D)
//            Logger.d(this, "Will draw: " + doDraw);
//    }
//
//    public void setGpsUpdate( boolean gpsUpdate ) {
//        this.gpsUpdate = gpsUpdate;
//    }
//
//    protected void draw( final Canvas canvas, final MapView mapsView, final boolean shadow ) {
//        if (touchDragging || shadow || !doDraw || mapsView.isAnimating() || !DataManager.getInstance().areImagesVisible())
//            return;
//
//        BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
//        float y0 = boundingBox.getLatNorthE6() / LibraryConstants.E6;
//        float y1 = boundingBox.getLatSouthE6() / LibraryConstants.E6;
//        float x0 = boundingBox.getLonWestE6() / LibraryConstants.E6;
//        float x1 = boundingBox.getLonEastE6() / LibraryConstants.E6;
//
//        Projection pj = mapsView.getProjection();
//
//        int screenWidth = canvas.getWidth();
//        int screenHeight = canvas.getHeight();
//        screenRect.contains(0, 0, screenWidth, screenHeight);
//        mapsView.getScreenRect(screenRect);
//
//        int zoomLevel = mapsView.getZoomLevel();
//
//        if (gpsUpdate) {
//            drawImages(canvas, pj, zoomLevel);
//            gpsUpdate = false;
//            return;
//        }
//
//        try {
//            imageInWorldBounds = DaoImages.getImagesInWorldBounds(context, y0, y1, x0, x1);
//            drawImages(canvas, pj, zoomLevel);
//
//        } catch (IOException e) {
//            Logger.e(this, e.getLocalizedMessage(), e);
//            e.printStackTrace();
//        }
//
//    }
//
//    private void drawImages( final Canvas canvas, Projection pj, int zoomLevel ) {
//        for( Image image : imageInWorldBounds ) {
//            float lat = (float) image.getLat();
//            float lon = (float) image.getLon();
//
//            GeoPoint g = new GeoPoint(lat, lon);
//            Point mapPixels = pj.toMapPixels(g, null);
//
//            canvas.drawBitmap(imageIcon, mapPixels.x - imageIconWidth / 2f, mapPixels.y - imageIconHeight / 2f, null);
//        }
//    }
//
//    @Override
//    public boolean onTouchEvent( MotionEvent event, MapView mapView ) {
//        int action = event.getAction();
//        switch( action ) {
//        case MotionEvent.ACTION_MOVE:
//            touchDragging = true;
//            break;
//        case MotionEvent.ACTION_UP:
//            touchDragging = false;
//            mapView.invalidate();
//            break;
//        }
//        return super.onTouchEvent(event, mapView);
//    }
//
//}
