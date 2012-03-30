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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;

import eu.geopaparazzi.library.util.ResourcesManager;
import eu.hydrologis.geopaparazzi.maps.DataManager;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;

/**
 * GenericOverlay is an abstract base class to display {@link OverlayWay OverlayWays}. The class defines some methods to
 * access the backing data structure of deriving subclasses.
 * <p>
 * The overlay may be used to show additional ways such as calculated routes. Closed polygons, for example buildings or
 * areas, are also supported. A way node sequence is considered as a closed polygon if the first and the last way node
 * are equal.
 * 
 * @param <Generic>
 *            the type of ways handled by this overlay.
 */
public abstract class GenericOverlay<Generic extends IOverlayGeneric> extends Overlay {
    private static final String THREAD_NAME = "GenericOverlay";
    private static final int ITEM_INITIAL_CAPACITY = 8;

    /**
     * Sets the bounds of the given drawable so that (0,0) is the center of the bottom row.
     * 
     * @param balloon
     *            the drawable whose bounds should be set.
     * @return the given drawable with set bounds.
     */
    public static Drawable boundCenter( Drawable balloon ) {
        balloon.setBounds(balloon.getIntrinsicWidth() / -2, balloon.getIntrinsicHeight() / -2, balloon.getIntrinsicWidth() / 2,
                balloon.getIntrinsicHeight() / 2);
        return balloon;
    }

    /**
     * Sets the bounds of the given drawable so that (0,0) is the center of the bounding box.
     * 
     * @param balloon
     *            the drawable whose bounds should be set.
     * @return the given drawable with set bounds.
     */
    public static Drawable boundCenterBottom( Drawable balloon ) {
        balloon.setBounds(balloon.getIntrinsicWidth() / -2, -balloon.getIntrinsicHeight(), balloon.getIntrinsicWidth() / 2, 0);
        return balloon;
    }

    /*
     * way stuff
     */
    private Paint defaultWayPaintFill;
    private Paint defaultWayPaintOutline;
    private Path wayPath;

    /*
     * item stuff
     */
    private int itemBottom;
    private Drawable itemDefaultMarker;
    private Drawable itemMarker;
    private Point itemPosition;
    private int left;
    private int right;
    private int top;
    private List<Integer> visibleItems;
    private List<Integer> visibleItemsRedraw;
    private final Context context;

    /*
     * cross stuff
     */
    private Path crossPath;
    private Paint crossPaint = new Paint();

    /**
     * Create a {@link OverlayWay} wrapped type.
     */
    public GenericOverlay( Context context ) {
        super();
        this.context = context;
        this.wayPath = new Path();
        this.wayPath.setFillType(Path.FillType.EVEN_ODD);

        this.itemPosition = new Point();
        this.visibleItems = new ArrayList<Integer>(ITEM_INITIAL_CAPACITY);
        this.visibleItemsRedraw = new ArrayList<Integer>(ITEM_INITIAL_CAPACITY);

        crossPath = new Path();
        crossPaint.setAntiAlias(true);
        crossPaint.setColor(Color.GRAY);
        crossPaint.setStrokeWidth(1f);
        crossPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Checks whether an item has been long pressed.
     */
    @Override
    public boolean onLongPress( GeoPoint geoPoint, MapView mapView ) {
        return checkItemHit(geoPoint, mapView, EventType.LONG_PRESS);
        // return super.onLongPress(geoPoint, mapView);
    }

    /**
     * Checks whether an item has been tapped.
     */
    @Override
    public boolean onTap( GeoPoint geoPoint, MapView mapView ) {
        return checkItemHit(geoPoint, mapView, EventType.TAP);
        // return super.onTap(geoPoint, mapView);
    }

    /**
     * @return the numbers of ways in this overlay.
     */
    public abstract int waySize();

    /**
     * @return the numbers of items in this overlay.
     */
    public abstract int itemSize();

    private void assembleWayPath( Point drawPosition, OverlayWay overlayWay ) {
        this.wayPath.reset();
        for( int i = 0; i < overlayWay.cachedWayPositions.length; ++i ) {
            this.wayPath.moveTo(overlayWay.cachedWayPositions[i][0].x - drawPosition.x, overlayWay.cachedWayPositions[i][0].y
                    - drawPosition.y);
            for( int j = 1; j < overlayWay.cachedWayPositions[i].length; ++j ) {
                this.wayPath.lineTo(overlayWay.cachedWayPositions[i][j].x - drawPosition.x, overlayWay.cachedWayPositions[i][j].y
                        - drawPosition.y);
            }
        }
    }

    private void drawWayPathOnCanvas( Canvas canvas, OverlayWay overlayWay ) {
        if (overlayWay.hasPaint) {
            // use the paints from the current way
            if (overlayWay.paintOutline != null) {
                canvas.drawPath(this.wayPath, overlayWay.paintOutline);
            }
            if (overlayWay.paintFill != null) {
                canvas.drawPath(this.wayPath, overlayWay.paintFill);
            }
        } else {
            // use the default paint objects
            if (this.defaultWayPaintOutline != null) {
                canvas.drawPath(this.wayPath, this.defaultWayPaintOutline);
            }
            if (this.defaultWayPaintFill != null) {
                canvas.drawPath(this.wayPath, this.defaultWayPaintFill);
            }
        }
    }

    /**
     * Creates a way in this overlay.
     * 
     * @param index
     *            the index of the way.
     * @return the way.
     */
    protected abstract OverlayWay createWay( int index );

    @Override
    protected void drawOverlayBitmap( Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel ) {
        /*
         * WAYS
         */
        int numberOfWays = waySize();
        for( int wayIndex = 0; wayIndex < numberOfWays; ++wayIndex ) {
            if (isInterrupted() || sizeHasChanged()) {
                // stop working
                return;
            }

            // get the current way
            OverlayWay overlayWay = createWay(wayIndex);
            if (overlayWay == null) {
                continue;
            }

            synchronized (overlayWay) {
                // make sure that the current way has way nodes
                if (overlayWay.wayNodes == null || overlayWay.wayNodes.length == 0) {
                    continue;
                }

                // make sure that the cached way node positions are valid
                if (drawZoomLevel != overlayWay.cachedZoomLevel) {
                    for( int i = 0; i < overlayWay.cachedWayPositions.length; ++i ) {
                        for( int j = 0; j < overlayWay.cachedWayPositions[i].length; ++j ) {
                            overlayWay.cachedWayPositions[i][j] = projection.toPoint(overlayWay.wayNodes[i][j],
                                    overlayWay.cachedWayPositions[i][j], drawZoomLevel);
                        }
                    }
                    overlayWay.cachedZoomLevel = drawZoomLevel;
                }

                assembleWayPath(drawPosition, overlayWay);
                drawWayPathOnCanvas(canvas, overlayWay);
            }
        }

        /*
         * ITEMS
         */

        // erase the list of visible items
        this.visibleItemsRedraw.clear();

        int numberOfItems = itemSize();
        for( int itemIndex = 0; itemIndex < numberOfItems; ++itemIndex ) {
            if (isInterrupted() || sizeHasChanged()) {
                // stop working
                return;
            }

            // get the current item
            OverlayItem overlayItem = createItem(itemIndex);
            if (overlayItem == null) {
                continue;
            }

            synchronized (overlayItem) {
                // make sure that the current item has a position
                if (overlayItem.getPoint() == null) {
                    continue;
                }

                // make sure that the cached item position is valid
                if (drawZoomLevel != overlayItem.cachedZoomLevel) {
                    overlayItem.cachedMapPosition = projection.toPoint(overlayItem.getPoint(), overlayItem.cachedMapPosition,
                            drawZoomLevel);
                    overlayItem.cachedZoomLevel = drawZoomLevel;
                }

                // calculate the relative item position on the canvas
                this.itemPosition.x = overlayItem.cachedMapPosition.x - drawPosition.x;
                this.itemPosition.y = overlayItem.cachedMapPosition.y - drawPosition.y;

                // get the correct marker for the item
                if (overlayItem.getMarker() == null) {
                    if (this.itemDefaultMarker == null) {
                        // no marker to draw the item
                        continue;
                    }
                    this.itemMarker = this.itemDefaultMarker;
                } else {
                    this.itemMarker = overlayItem.getMarker();
                }

                // get the position of the marker
                Rect markerBounds = this.itemMarker.copyBounds();

                // calculate the bounding box of the marker
                this.left = this.itemPosition.x + markerBounds.left;
                this.right = this.itemPosition.x + markerBounds.right;
                this.top = this.itemPosition.y + markerBounds.top;
                this.itemBottom = this.itemPosition.y + markerBounds.bottom;

                // check if the bounding box of the marker intersects with the canvas
                if (this.right >= 0 && this.left <= canvas.getWidth() && this.itemBottom >= 0 && this.top <= canvas.getHeight()) {
                    // set the position of the marker
                    this.itemMarker.setBounds(this.left, this.top, this.right, this.itemBottom);

                    // draw the item marker on the canvas
                    this.itemMarker.draw(canvas);

                    // restore the position of the marker
                    this.itemMarker.setBounds(markerBounds);

                    // add the current item index to the list of visible items
                    this.visibleItemsRedraw.add(Integer.valueOf(itemIndex));
                }
            }
        }

        // swap the two visible item lists
        synchronized (this.visibleItems) {
            List<Integer> visibleItemsTemp = this.visibleItems;
            this.visibleItems = this.visibleItemsRedraw;
            this.visibleItemsRedraw = visibleItemsTemp;
        }

        /*
         * draw cross on top
         */
        Point center = new Point(canvas.getWidth() / 2, canvas.getHeight() / 2);
        crossPath.reset();
        crossPath.moveTo(center.x, center.y - 20);
        crossPath.lineTo(center.x, center.y + 20);
        crossPath.moveTo(center.x - 20, center.y);
        crossPath.lineTo(center.x + 20, center.y);
        canvas.drawPath(crossPath, crossPaint);

    }

    @Override
    protected String getThreadName() {
        return THREAD_NAME;
    }

    /**
     * This method should be called after ways have been added to the overlay.
     */
    protected final void populate() {
        super.requestRedraw();
    }

    /**
     * Creates an item in this overlay.
     * 
     * @param index
     *            the index of the item.
     * @return the item.
     */
    protected abstract OverlayItem createItem( int index );

    /**
     * Checks whether an item has been hit by an event and calls the appropriate handler.
     * 
     * @param geoPoint
     *            the point of the event.
     * @param mapView
     *            the {@link MapView} that triggered the event.
     * @param eventType
     *            the type of the event.
     * @return true if an item has been hit, false otherwise.
     */
    protected boolean checkItemHit( GeoPoint geoPoint, MapView mapView, EventType eventType ) {
        Projection projection = mapView.getProjection();
        Point eventPosition = projection.toPixels(geoPoint, null);

        // check if the translation to pixel coordinates has failed
        if (eventPosition == null) {
            return false;
        }

        Point checkItemPoint = new Point();

        synchronized (this.visibleItems) {
            // iterate over all visible items
            for( int i = this.visibleItems.size() - 1; i >= 0; --i ) {
                Integer itemIndex = this.visibleItems.get(i);

                // get the current item
                OverlayItem checkOverlayItem = createItem(itemIndex.intValue());
                if (checkOverlayItem == null) {
                    continue;
                }

                synchronized (checkOverlayItem) {
                    // make sure that the current item has a position
                    if (checkOverlayItem.getPoint() == null) {
                        continue;
                    }

                    checkItemPoint = projection.toPixels(checkOverlayItem.getPoint(), checkItemPoint);
                    // check if the translation to pixel coordinates has failed
                    if (checkItemPoint == null) {
                        continue;
                    }

                    // select the correct marker for the item and get the position
                    Rect checkMarkerBounds;
                    if (checkOverlayItem.getMarker() == null) {
                        if (this.itemDefaultMarker == null) {
                            // no marker to draw the item
                            continue;
                        }
                        checkMarkerBounds = this.itemDefaultMarker.getBounds();
                    } else {
                        checkMarkerBounds = checkOverlayItem.getMarker().getBounds();
                    }

                    // calculate the bounding box of the marker
                    int checkLeft = checkItemPoint.x + checkMarkerBounds.left;
                    int checkRight = checkItemPoint.x + checkMarkerBounds.right;
                    int checkTop = checkItemPoint.y + checkMarkerBounds.top;
                    int checkBottom = checkItemPoint.y + checkMarkerBounds.bottom;

                    // check if the event position is within the bounds of the marker
                    if (checkRight >= eventPosition.x && checkLeft <= eventPosition.x && checkBottom >= eventPosition.y
                            && checkTop <= eventPosition.y) {
                        switch( eventType ) {
                        case LONG_PRESS:
                            if (onLongPress(itemIndex.intValue())) {
                                return true;
                            }
                            break;

                        case TAP:
                            if (onTap(itemIndex.intValue())) {
                                return true;
                            }
                            break;
                        }
                    }
                }
            }
        }

        // no hit
        return false;
    }

    /**
     * Handles a long press event.
     * <p>
     * The default implementation of this method does nothing and returns false.
     * 
     * @param index
     *            the index of the item that has been long pressed.
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
     *            the index of the item that has been tapped.
     * @return true if the event was handled, false otherwise.
     */
    protected boolean onTap( int index ) {
        OverlayItem item = createItem(index);
        if (item != null) {
            String title = item.getTitle();
            String snippet = item.getSnippet();
            if (title.toLowerCase().endsWith("jpg")) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                String relativePath = title;
                File mediaDir = ResourcesManager.getInstance(context).getMediaDir();
                intent.setDataAndType(Uri.fromFile(new File(mediaDir.getParentFile(), relativePath)), "image/jpg");
                context.startActivity(intent);
            } else {
                Builder builder = new AlertDialog.Builder(this.context);
                builder.setIcon(android.R.drawable.ic_menu_info_details);
                builder.setTitle(title);
                if (snippet != null && snippet.length() > 0)
                    builder.setMessage(snippet);
                builder.setPositiveButton("OK", null);
                builder.show();
            }
            return true;
        }
        return false;
    }
}
