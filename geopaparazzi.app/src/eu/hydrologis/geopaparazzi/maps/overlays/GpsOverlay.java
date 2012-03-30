/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps.overlays;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * GpsOverlay is an abstract base class to display {@link GpsData OverlayCircles}. The class defines some
 * methods to access the backing data structure of deriving subclasses. Besides organizing the redrawing process it
 * handles long press and tap events and calls {@link #onLongPress(int)} and {@link #onTap(int)} respectively.
 * <p>
 * The overlay may be used to indicate positions which have a known accuracy, such as GPS fixes. The radius of the
 * circles is specified in meters and will be automatically converted to pixels at each redraw.
 * 
 * @param <Circle>
 *            the type of circles handled by this overlay.
 */
public class GpsOverlay extends Overlay {
    private static final int INITIAL_CAPACITY = 8;
    private static final String THREAD_NAME = "GpsOverlay";

    private final Point circlePosition;
    private final Paint defaultPaintFill;
    private final Paint defaultPaintOutline;
    private final boolean hasDefaultPaint;
    private final Path path;
    private List<Integer> visibleCircles;
    private List<Integer> visibleCirclesRedraw;

    private GpsData overlayGps;
    private final Drawable marker;

    /**
     * @param defaultPaintFill
     *            the default paint which will be used to fill the circles (may be null).
     * @param defaultPaintOutline
     *            the default paint which will be used to draw the circle outlines (may be null).
     */
    public GpsOverlay( Paint defaultPaintFill, Paint defaultPaintOutline, Drawable marker ) {
        super();
        this.marker = marker;
        overlayGps = new GpsData();
        this.defaultPaintFill = defaultPaintFill;
        this.defaultPaintOutline = defaultPaintOutline;
        this.hasDefaultPaint = defaultPaintFill != null || defaultPaintOutline != null;
        this.circlePosition = new Point();
        this.visibleCircles = new ArrayList<Integer>(INITIAL_CAPACITY);
        this.visibleCirclesRedraw = new ArrayList<Integer>(INITIAL_CAPACITY);
        this.path = new Path();

        marker = ItemizedOverlay.boundCenter(marker);
    }

    /**
     * Checks whether a circle has been long pressed.
     */
    @Override
    public boolean onLongPress( GeoPoint geoPoint, MapView mapView ) {
        return false;
    }

    /**
     * Checks whether a circle has been tapped.
     */
    @Override
    public boolean onTap( GeoPoint geoPoint, MapView mapView ) {
        return false;
    }

    private void drawPathOnCanvas( Canvas canvas, GpsData overlayCircle ) {
        if (overlayCircle.hasPaint) {
            // use the paints from the current circle
            if (overlayCircle.paintOutline != null) {
                canvas.drawPath(this.path, overlayCircle.paintOutline);
            }
            if (overlayCircle.paintFill != null) {
                canvas.drawPath(this.path, overlayCircle.paintFill);
            }
        } else if (this.hasDefaultPaint) {
            // use the default paint objects
            if (this.defaultPaintOutline != null) {
                canvas.drawPath(this.path, this.defaultPaintOutline);
            }
            if (this.defaultPaintFill != null) {
                canvas.drawPath(this.path, this.defaultPaintFill);
            }
        }
    }

    public void setGpsPosition( GeoPoint position, float accuracy ) {
        overlayGps.setCircleData(position, accuracy);
    }

    @Override
    protected void drawOverlayBitmap( Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel ) {
        // erase the list of visible circles
        this.visibleCirclesRedraw.clear();

        int numberOfCircles = 1;
        for( int circleIndex = 0; circleIndex < numberOfCircles; ++circleIndex ) {
            if (isInterrupted() || sizeHasChanged()) {
                // stop working
                return;
            }

            // get the current circle
            if (overlayGps == null || overlayGps.center == null) {
                continue;
            }

            synchronized (overlayGps) {
                // make sure that the current circle has a center position and a radius
                if (overlayGps.center == null || overlayGps.radius < 0) {
                    continue;
                }

                // make sure that the cached center position is valid
                if (drawZoomLevel != overlayGps.cachedZoomLevel) {
                    overlayGps.cachedCenterPosition = projection.toPoint(overlayGps.center, overlayGps.cachedCenterPosition,
                            drawZoomLevel);
                    overlayGps.cachedZoomLevel = drawZoomLevel;
                    overlayGps.cachedRadius = projection.metersToPixels(overlayGps.radius, drawZoomLevel);
                }

                // calculate the relative circle position on the canvas
                this.circlePosition.x = overlayGps.cachedCenterPosition.x - drawPosition.x;
                this.circlePosition.y = overlayGps.cachedCenterPosition.y - drawPosition.y;
                float circleRadius = overlayGps.cachedRadius;

                // check if the bounding box of the circle intersects with the canvas
                if ((this.circlePosition.x + circleRadius) >= 0 && (this.circlePosition.x - circleRadius) <= canvas.getWidth()
                        && (this.circlePosition.y + circleRadius) >= 0
                        && (this.circlePosition.y - circleRadius) <= canvas.getHeight()) {
                    // assemble the path
                    this.path.reset();
                    this.path.addCircle(this.circlePosition.x, this.circlePosition.y, circleRadius, Path.Direction.CCW);

                    if (overlayGps.hasPaint || this.hasDefaultPaint) {
                        if (circleRadius > 0) {
                            drawPathOnCanvas(canvas, overlayGps);
                        }

                        // get the position of the marker
                        Rect markerBounds = marker.copyBounds();
                        // calculate the bounding box of the marker
                        int left = this.circlePosition.x + markerBounds.left;
                        int right = this.circlePosition.x + markerBounds.right;
                        int top = this.circlePosition.y + markerBounds.top;
                        int bottom = this.circlePosition.y + markerBounds.bottom;
                        // check if the bounding box of the marker intersects with the canvas
                        if (right >= 0 && left <= canvas.getWidth() && bottom >= 0 && top <= canvas.getHeight()) {
                            // set the position of the marker
                            marker.setBounds(left, top, right, bottom);
                            // draw the item marker on the canvas
                            marker.draw(canvas);
                            // restore the position of the marker
                            marker.setBounds(markerBounds);
                        }

                        // add the current circle index to the list of visible circles
                        this.visibleCirclesRedraw.add(Integer.valueOf(circleIndex));
                    }
                }
            }
        }

        // swap the two visible circle lists
        synchronized (this.visibleCircles) {
            List<Integer> visibleCirclesTemp = this.visibleCircles;
            this.visibleCircles = this.visibleCirclesRedraw;
            this.visibleCirclesRedraw = visibleCirclesTemp;
        }
    }

    @Override
    protected String getThreadName() {
        return THREAD_NAME;
    }

    /**
     * Handles a long press event.
     * <p>
     * The default implementation of this method does nothing and returns false.
     * 
     * @param index
     *            the index of the circle that has been long pressed.
     * @return true if the event was handled, false otherwise.
     */
    protected boolean onLongPress( int index ) {
        return false;
    }

    /**
     * Handles a tap event.
     * <p>
     * The default implementation of this method does nothing and returns false.
     * 
     * @param index
     *            the index of the circle that has been tapped.
     * @return true if the event was handled, false otherwise.
     */
    protected boolean onTap( int index ) {
        return false;
    }

    /**
     * This method should be called after circles have been added to the overlay.
     */
    protected final void populate() {
        super.requestRedraw();
    }
}
