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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jsqlite.Exception;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.model.GeoPoint;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.android.ShapeWriter;
import com.vividsolutions.jts.android.geom.DrawableShape;
import com.vividsolutions.jts.geom.Geometry;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.ISpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.Style;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.maps.MapsActivity;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * GeopaparazziOverlay is an abstract base class to display {@link OverlayWay OverlayWays}. The class defines some methods to
 * access the backing data structure of deriving subclasses.
 * <p>
 * The overlay may be used to show additional ways such as calculated routes. Closed polygons, for example buildings or
 * areas, are also supported. A way node sequence is considered as a closed polygon if the first and the last way node
 * are equal.
 *
 * @param <Generic>
 *            the type of ways handled by this overlay.
 */
public abstract class GeopaparazziOverlay extends Overlay {
    private int crossSize = 20;
    private static final String THREAD_NAME = "GeopaparazziOverlay"; //$NON-NLS-1$
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
    private Paint wayStartPaintFill;
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

    /*
     * cross stuff
     */
    private Path crossPath;
    private Paint crossPaint = new Paint();

    /*
     * gps stuff
     */
    private final Point circlePosition;
    private final Path path;

    private GpsData overlayGps;
    private Drawable gpsMarker;

    private Path gpsPath;
    private OverlayWay gpslogOverlay;
    private Paint gpsTrackPaintYellow;
    private Paint gpsTrackPaintBlack;
    private Paint gpsOutline;
    private Paint gpsFill;

    private Path gpsStatusPath;
    private Paint gpsRedFill;
    private Paint gpsOrangeFill;
    private Paint gpsGreenFill;
    private Paint gpsBlueFill;

    private List<GeoPoint> currentGpsLog = new ArrayList<GeoPoint>();
    private Context context;
    private int inset = 5;
    private Paint textPaint;
    private Paint textHaloPaint;
    private boolean isNotesTextVisible;
    private boolean doNotesTextHalo;

    /**
     * Create a {@link OverlayWay} wrapped type.
     */
    public GeopaparazziOverlay( Context context ) {
        super();
        this.context = context;
        this.wayPath = new Path();
        this.wayPath.setFillType(Path.FillType.EVEN_ODD);

        this.itemPosition = new Point();
        this.visibleItems = new ArrayList<Integer>(ITEM_INITIAL_CAPACITY);
        this.visibleItemsRedraw = new ArrayList<Integer>(ITEM_INITIAL_CAPACITY);

        // cross
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String crossColorStr = preferences.getString(Constants.PREFS_KEY_CROSS_COLOR, "red");
        int crossColor = ColorUtilities.toColor(crossColorStr);
        String crossWidthStr = preferences.getString(Constants.PREFS_KEY_CROSS_WIDTH, "3");
        float crossWidth = 3f;
        try {
            crossWidth = (float) Double.parseDouble(crossWidthStr);
        } catch (NumberFormatException e) {
            // ignore and use default
        }
        String crossSizeStr = preferences.getString(Constants.PREFS_KEY_CROSS_SIZE, "20");
        try {
            crossSize = (int) Double.parseDouble(crossSizeStr);
        } catch (NumberFormatException e) {
            // ignore and use default
        }

        crossPath = new Path();
        crossPaint.setAntiAlias(true);
        crossPaint.setColor(crossColor);
        crossPaint.setStrokeWidth(crossWidth);
        crossPaint.setStyle(Paint.Style.STROKE);

        // gps
        overlayGps = new GpsData();
        this.circlePosition = new Point();
        this.path = new Path();
        this.gpsPath = new Path();

        wayStartPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        wayStartPaintFill.setStyle(Paint.Style.FILL);

        gpsMarker = context.getResources().getDrawable(R.drawable.current_position);
        gpsFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsFill.setStyle(Paint.Style.FILL);
        gpsFill.setColor(Color.BLUE);
        gpsFill.setAlpha(48);

        gpsOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsOutline.setStyle(Paint.Style.STROKE);
        gpsOutline.setColor(Color.BLUE);
        gpsOutline.setAlpha(128);
        gpsOutline.setStrokeWidth(2);

        gpsTrackPaintYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsTrackPaintYellow.setStyle(Paint.Style.STROKE);
        gpsTrackPaintYellow.setColor(Color.YELLOW);
        gpsTrackPaintYellow.setStrokeWidth(3);

        gpsTrackPaintBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsTrackPaintBlack.setStyle(Paint.Style.STROKE);
        gpsTrackPaintBlack.setColor(Color.BLACK);
        gpsTrackPaintBlack.setStrokeWidth(5);

        Resources resources = context.getResources();

        gpsStatusPath = new Path();
        gpsRedFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsRedFill.setStyle(Paint.Style.FILL);
        gpsRedFill.setColor(resources.getColor(R.color.gpsred_fill));
        gpsOrangeFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsOrangeFill.setStyle(Paint.Style.FILL);
        gpsOrangeFill.setColor(resources.getColor(R.color.gpsorange_fill));
        gpsGreenFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsGreenFill.setStyle(Paint.Style.FILL);
        gpsGreenFill.setColor(resources.getColor(R.color.gpsgreen_fill));
        gpsBlueFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsBlueFill.setStyle(Paint.Style.FILL);
        gpsBlueFill.setColor(resources.getColor(R.color.gpsblue_fill));

        isNotesTextVisible = preferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_VISIBLE, false);
        if (isNotesTextVisible) {
            String notesTextSizeStr = preferences.getString(Constants.PREFS_KEY_NOTES_TEXT_SIZE, "30");
            float notesTextSize = 30f;
            try {
                notesTextSize = (float) Double.parseDouble(notesTextSizeStr);
            } catch (NumberFormatException e) {
                // ignore and use default
            }
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(notesTextSize);
            doNotesTextHalo = preferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_DOHALO, false);
            textHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textHaloPaint.setStyle(Paint.Style.STROKE);
            textHaloPaint.setStrokeWidth(3);
            textHaloPaint.setColor(Color.WHITE);
            textHaloPaint.setTextSize(notesTextSize);
        }

        gpsMarker = ItemizedOverlay.boundCenter(gpsMarker);
        gpslogOverlay = new OverlayWay(null, gpsOutline);

        currentGpsLog.clear();
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

    private void drawWayPathOnCanvas( Canvas canvas, Point drawPosition, OverlayWay overlayWay ) {
        // assemble the ways
        this.wayPath.reset();
        for( int i = 0; i < overlayWay.cachedWayPositions.length; ++i ) {
            int x = overlayWay.cachedWayPositions[i][0].x - drawPosition.x;
            int y = overlayWay.cachedWayPositions[i][0].y - drawPosition.y;
            this.wayPath.moveTo(x, y);
            int lastX = 0;
            int lastY = 0;
            for( int j = 1; j < overlayWay.cachedWayPositions[i].length; ++j ) {
                lastX = overlayWay.cachedWayPositions[i][j].x - drawPosition.x;
                lastY = overlayWay.cachedWayPositions[i][j].y - drawPosition.y;
                this.wayPath.lineTo(lastX, lastY);
            }

            // draw start points
            float size = 2;
            if (overlayWay.hasPaint) {
                // use the paints from the current way
                if (overlayWay.paintOutline != null) {
                    wayStartPaintFill.setColor(overlayWay.paintOutline.getColor());
                    size = overlayWay.paintOutline.getStrokeWidth();
                } else if (overlayWay.paintFill != null) {
                    wayStartPaintFill.setColor(overlayWay.paintFill.getColor());
                }
            } else {
                // use the default paint objects
                if (this.defaultWayPaintOutline != null) {
                    wayStartPaintFill.setColor(defaultWayPaintOutline.getColor());
                    size = defaultWayPaintOutline.getStrokeWidth();
                } else if (this.defaultWayPaintFill != null) {
                    wayStartPaintFill.setColor(defaultWayPaintFill.getColor());
                }
            }
            size = size * 2;
            canvas.drawCircle(lastX, lastY, size, wayStartPaintFill);
            canvas.drawRect(x - size, y - size, x + size, y + size, wayStartPaintFill);
        }

        // draw them
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

    private void assembleGpsWayPath( Point drawPosition, OverlayWay overlayWay ) {
        this.gpsPath.reset();
        for( int i = 0; i < overlayWay.cachedWayPositions.length; ++i ) {
            this.gpsPath.moveTo(overlayWay.cachedWayPositions[i][0].x - drawPosition.x, overlayWay.cachedWayPositions[i][0].y
                    - drawPosition.y);
            for( int j = 1; j < overlayWay.cachedWayPositions[i].length; ++j ) {
                this.gpsPath.lineTo(overlayWay.cachedWayPositions[i][j].x - drawPosition.x, overlayWay.cachedWayPositions[i][j].y
                        - drawPosition.y);
            }
        }
    }

    private void drawGpsWayPathOnCanvas( Canvas canvas, OverlayWay overlayWay ) {
        canvas.drawPath(this.gpsPath, this.gpsTrackPaintBlack);
        canvas.drawPath(this.gpsPath, this.gpsTrackPaintYellow);
    }

    private void drawGpsOnCanvas( Canvas canvas, GpsData gpsCircle ) {
        canvas.drawPath(this.path, gpsOutline);
        canvas.drawPath(this.path, gpsFill);
    }

    @SuppressWarnings("nls")
    public void setGpsPosition( GeoPoint position, float accuracy ) {
        GpsManager gpsManager = GpsManager.getInstance(context);
        if (gpsManager.isDatabaseLogging()) {
            currentGpsLog.add(position);
        } else {
            currentGpsLog.clear();
        }
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "Set gps data: " + position.getLongitude() + "/" + position.getLatitude() + "/" + accuracy);
        overlayGps.setCircleData(position, accuracy);
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
         * first spatialite layers, if any
         */
        drawFromSpatialite(canvas, drawPosition, projection, drawZoomLevel);

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

                drawWayPathOnCanvas(canvas, drawPosition, overlayWay);
            }
        }

        /*
         * ITEMS
         */

        // erase the list of visible items
        this.visibleItemsRedraw.clear();

        int canvasHeight = canvas.getHeight();
        int canvasWidth = canvas.getWidth();

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
                if (this.right >= 0 && this.left <= canvasWidth && this.itemBottom >= 0 && this.top <= canvasHeight) {
                    // set the position of the marker
                    this.itemMarker.setBounds(this.left, this.top, this.right, this.itemBottom);

                    // draw the item marker on the canvas
                    this.itemMarker.draw(canvas);

                    // restore the position of the marker
                    this.itemMarker.setBounds(markerBounds);

                    // add the current item index to the list of visible items
                    this.visibleItemsRedraw.add(Integer.valueOf(itemIndex));

                    if (isNotesTextVisible && overlayItem instanceof NoteOverlayItem) {
                        String title = overlayItem.getTitle();
                        float delta = markerBounds.width() / 4f;
                        float x = right - delta;
                        float y = top + delta;
                        if (doNotesTextHalo)
                            canvas.drawText(title, x, y, textHaloPaint);
                        canvas.drawText(title, x, y, textPaint);
                    }
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
         * gps logging track
         */
        GpsManager gpsManager = GpsManager.getInstance(context);
        if (gpsManager.isDatabaseLogging()) {
            // if a track is recorded, show it
            synchronized (gpslogOverlay) {
                int size = currentGpsLog.size();
                if (size > 1) {
                    GeoPoint[] geoPoints = currentGpsLog.toArray(new GeoPoint[0]);
                    gpslogOverlay.setWayNodes(new GeoPoint[][]{geoPoints});
                    // make sure that the current way has way nodes
                    if (gpslogOverlay.wayNodes != null && gpslogOverlay.wayNodes.length != 0) {
                        // make sure that the cached way node positions are valid
                        if (drawZoomLevel != gpslogOverlay.cachedZoomLevel) {
                            for( int i = 0; i < gpslogOverlay.cachedWayPositions.length; ++i ) {
                                for( int j = 0; j < gpslogOverlay.cachedWayPositions[i].length; ++j ) {
                                    gpslogOverlay.cachedWayPositions[i][j] = projection.toPoint(gpslogOverlay.wayNodes[i][j],
                                            gpslogOverlay.cachedWayPositions[i][j], drawZoomLevel);
                                }
                            }
                            gpslogOverlay.cachedZoomLevel = drawZoomLevel;
                        }

                        assembleGpsWayPath(drawPosition, gpslogOverlay);
                        drawGpsWayPathOnCanvas(canvas, gpslogOverlay);
                    }
                }
            }
        }

        /*
         * GPS position
         */

        if (isInterrupted() || sizeHasChanged()) {
            // stop working
            return;
        }

        // get the current circle
        if (overlayGps != null && overlayGps.center != null) {
            synchronized (overlayGps) {
                // make sure that the current circle has a center position and a radius
                if (overlayGps.center != null && overlayGps.radius >= 0) {
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
                    if ((this.circlePosition.x + circleRadius) >= 0 && (this.circlePosition.x - circleRadius) <= canvasWidth
                            && (this.circlePosition.y + circleRadius) >= 0
                            && (this.circlePosition.y - circleRadius) <= canvasHeight) {
                        // assemble the path
                        this.path.reset();
                        this.path.addCircle(this.circlePosition.x, this.circlePosition.y, circleRadius, Path.Direction.CCW);

                        if (circleRadius > 0) {
                            drawGpsOnCanvas(canvas, overlayGps);
                        }

                        // get the position of the marker
                        Rect markerBounds = gpsMarker.copyBounds();
                        // calculate the bounding box of the marker
                        int left = this.circlePosition.x + markerBounds.left;
                        int right = this.circlePosition.x + markerBounds.right;
                        int top = this.circlePosition.y + markerBounds.top;
                        int bottom = this.circlePosition.y + markerBounds.bottom;
                        // check if the bounding box of the marker intersects with the canvas
                        if (right >= 0 && left <= canvasWidth && bottom >= 0 && top <= canvasHeight) {
                            // set the position of the marker
                            gpsMarker.setBounds(left, top, right, bottom);
                            // draw the item marker on the canvas
                            gpsMarker.draw(canvas);
                            // restore the position of the marker
                            gpsMarker.setBounds(markerBounds);
                        }

                    }
                }
            }
        }

        /*
         * show gps status
         */
        Paint gpsStatusFill = null;
        if (gpsManager.isEnabled()) {
            if (gpsManager.isDatabaseLogging()) {
                gpsStatusFill = gpsBlueFill;
            } else {
                if (gpsManager.hasFix()) {
                    gpsStatusFill = gpsGreenFill;
                } else {
                    gpsStatusFill = gpsOrangeFill;
                }
            }
        } else {
            gpsStatusFill = gpsRedFill;
        }
        gpsStatusPath.reset();
        gpsStatusPath.moveTo(0, canvasHeight);
        gpsStatusPath.lineTo(0, canvasHeight - inset);
        gpsStatusPath.lineTo(canvasWidth, canvasHeight - inset);
        gpsStatusPath.lineTo(canvasWidth, canvasHeight);
        gpsStatusPath.close();
        canvas.drawPath(gpsStatusPath, gpsStatusFill);

        /*
         * draw cross on top
         */
        Point center = new Point(canvasWidth / 2, canvasHeight / 2);
        crossPath.reset();
        crossPath.moveTo(center.x, center.y - crossSize);
        crossPath.lineTo(center.x, center.y + crossSize);
        crossPath.moveTo(center.x - crossSize, center.y);
        crossPath.lineTo(center.x + crossSize, center.y);
        canvas.drawPath(crossPath, crossPaint);

    }
    private void drawFromSpatialite( Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel ) {
        /*
         * draw from spatialite
         */
        GeoPoint zeroPoint = projection.fromPixels(0, 0);
        GeoPoint whPoint = projection.fromPixels(canvas.getWidth(), canvas.getHeight());
        double n = zeroPoint.getLatitude();
        double w = zeroPoint.getLongitude();
        double s = whPoint.getLatitude();
        double e = whPoint.getLongitude();
        try {
            SpatialDatabasesManager sdManager = SpatialDatabasesManager.getInstance();
            double[] bounds_zoom = new double[]{w, s, e, n, (double) drawZoomLevel};
            List<SpatialVectorTable> spatialTables = MapsDirManager.getInstance().getSpatialVectorTables(bounds_zoom, 1, false);
            // GPLog.androidLog(-1,"GeopaparazziOverlay.drawFromSpatialite size["+spatialTables.size()+"]: ["+drawZoomLevel+"]");
            for( int i = 0; i < spatialTables.size(); i++ ) {
                SpatialVectorTable spatialTable = spatialTables.get(i);
                if (isInterrupted() || sizeHasChanged()) {
                    // stop working
                    return;
                }
                Style style4Table = spatialTable.getStyle();
                if (drawZoomLevel < style4Table.minZoom || drawZoomLevel > style4Table.maxZoom) {
                    // we do not draw outside of the zoom levels
                    continue;
                }

                ISpatialDatabaseHandler spatialDatabaseHandler = sdManager.getVectorHandler(spatialTable);
                int i_geometryIterator = 0;
                GeometryIterator geometryIterator = null;
                try {
                    Paint fill = null;
                    Paint stroke = null;
                    if (style4Table.fillcolor != null && style4Table.fillcolor.trim().length() > 0)
                        fill = spatialDatabaseHandler.getFillPaint4Style(style4Table);
                    if (style4Table.strokecolor != null && style4Table.strokecolor.trim().length() > 0)
                        stroke = spatialDatabaseHandler.getStrokePaint4Style(style4Table);
                    PointTransformation pointTransformer = new MapsforgePointTransformation(projection, drawPosition,
                            drawZoomLevel);
                    ShapeWriter shape_writer = null;
                    ShapeWriter shape_writer_point = null;
                    if (spatialTable.isPoint()) {
                        shape_writer = new ShapeWriter(pointTransformer, spatialTable.getStyle().shape,
                                spatialTable.getStyle().size);
                    } else {
                        shape_writer = new ShapeWriter(pointTransformer);
                        if (spatialTable.isGeometryCollection()) {
                            shape_writer_point = new ShapeWriter(pointTransformer, spatialTable.getStyle().shape,
                                    spatialTable.getStyle().size);
                        }
                    }
                    shape_writer.setRemoveDuplicatePoints(true);
                    shape_writer.setDecimation(spatialTable.getStyle().decimationFactor);
                    geometryIterator = spatialDatabaseHandler.getGeometryIteratorInBounds("4326", spatialTable, n, s, e, w);
                    while( geometryIterator.hasNext() ) {
                        i_geometryIterator++;
                        Geometry geom = geometryIterator.next();
                        if (geom != null) {
                            String s_label=geometryIterator.get_label_text();
                            String s_geometry_type = geom.getGeometryType();
                            if (spatialTable.isGeometryCollection()) {
                                int i_count_geometries = geom.getNumGeometries();
                                // GPLog.androidLog(-1,"GeopaparazziOverlay.drawFromSpatialite type["+s_geometry_type+"]: count_geometries["+i_count_geometries+"]: ["+drawZoomLevel+"]");
                                for( int j = 0; j < i_count_geometries; j++ ) {
                                    Geometry geom_collect = geom.getGeometryN(j);
                                    if (geom_collect != null) {
                                        s_geometry_type = geom_collect.getGeometryType();
                                        // GPLog.androidLog(-1,"GeopaparazziOverlay.drawFromSpatialite type["+s_geometry_type+"]: ["+drawZoomLevel+"]");
                                        if (s_geometry_type.toUpperCase().indexOf("POINT") != -1) {
                                            drawGeometry(geom_collect, canvas, shape_writer_point, fill, stroke);
                                        } else {
                                            drawGeometry(geom_collect, canvas, shape_writer, fill, stroke);
                                        }
                                        if (isInterrupted() || sizeHasChanged()) { // stop working
                                            return;
                                        }
                                    }
                                }
                            } else {
                                drawGeometry(geom, canvas, shape_writer, fill, stroke);
                                if (isInterrupted() || sizeHasChanged()) { // stop working
                                    return;
                                }
                            }
                            if (!s_label.equals(""))
                            { // Draw Label
                             GPLog.androidLog(-1, "GeopaparazziOverlay.drawFromSpatialite  label["+s_label+"] description["+ spatialTable.getDescription() + "]");
                            }
                        } else {
                            GPLog.androidLog(-1, "GeopaparazziOverlay.drawFromSpatialite  [geom == null] description["
                                    + spatialTable.getDescription() + "]");
                        }
                    }
                    if (i_geometryIterator == 0) {
                       // GPLog.androidLog(-1, "GeopaparazziOverlay.drawFromSpatialite  geometryIterator[" + i_geometryIterator
                       //          + "] description[" + spatialTable.getDescription() + "]");
                    }
                } finally {
                    if (geometryIterator != null)
                        geometryIterator.close();
                }
            }
        } catch (Exception e1) {
            GPLog.androidLog(4, "GeopaparazziOverlay.drawFromSpatialite [failed]", e1);
        }
    }

    private void drawGeometry( Geometry geom, Canvas canvas, ShapeWriter shape_writer, Paint fill, Paint stroke ) {
        String s_geometry_type = geom.getGeometryType();
        int i_geometry_type = GeometryType.forValue(s_geometry_type);
        GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
        DrawableShape shape = shape_writer.toShape(geom);
        switch( geometry_type ) {
        case POINT_XY:
        case POINT_XYM:
        case POINT_XYZ:
        case POINT_XYZM:
        case MULTIPOINT_XY:
        case MULTIPOINT_XYM:
        case MULTIPOINT_XYZ:
        case MULTIPOINT_XYZM: {
            if (fill != null)
                shape.fill(canvas, fill);
            if (stroke != null)
                shape.draw(canvas, stroke);
        }
            break;
        case LINESTRING_XY:
        case LINESTRING_XYM:
        case LINESTRING_XYZ:
        case LINESTRING_XYZM:
        case MULTILINESTRING_XY:
        case MULTILINESTRING_XYM:
        case MULTILINESTRING_XYZ:
        case MULTILINESTRING_XYZM: {
            if (stroke != null)
                shape.draw(canvas, stroke);
        }
            break;
        case POLYGON_XY:
        case POLYGON_XYM:
        case POLYGON_XYZ:
        case POLYGON_XYZM:
        case MULTIPOLYGON_XY:
        case MULTIPOLYGON_XYM:
        case MULTIPOLYGON_XYZ:
        case MULTIPOLYGON_XYZM: {
            if (fill != null)
                shape.fill(canvas, fill);
            if (stroke != null)
                shape.draw(canvas, stroke);
        }
            break;
        default:
            break;
        }
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
        Context context = mapView.getContext();
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
                            if (onTap(context, itemIndex.intValue())) {
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
    protected boolean onTap( Context context, int index ) {
        OverlayItem item = createItem(index);
        if (item != null) {
            String title = item.getTitle();
            String snippet = item.getSnippet();

            GeoPoint position = item.getPoint();
            int latE6 = position.latitudeE6;
            int lonE6 = position.longitudeE6;
            float lat = latE6 / LibraryConstants.E6;
            float lon = lonE6 / LibraryConstants.E6;
            if (title.toLowerCase().endsWith("jpg") || title.toLowerCase().endsWith("png")) { //$NON-NLS-1$ //$NON-NLS-2$
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                File absolutePath = new File(title);
                if (!absolutePath.exists()) {
                    // try relative to media
                    try {
                        File mediaDir = ResourcesManager.getInstance(context).getMediaDir();
                        absolutePath = new File(mediaDir.getParentFile(), title);
                    } catch (java.lang.Exception e) {
                        e.printStackTrace();
                    }
                }
                intent.setDataAndType(Uri.fromFile(absolutePath), "image/*"); //$NON-NLS-1$
                context.startActivity(intent);
            } else {

                boolean doInfo = true;
                if (context instanceof MapsActivity) {
                    MapsActivity mapActivity = (MapsActivity) context;

                    float n = (float) (lat + 0.00001f);
                    float s = (float) (lat - 0.00001f);
                    float w = (float) (lon - 0.00001f);
                    float e = (float) (lon + 0.00001f);

                    try {
                        List<Note> notesInWorldBounds = DaoNotes.getNotesInWorldBounds(n, s, w, e);
                        if (notesInWorldBounds.size() > 0) {
                            Note note = notesInWorldBounds.get(0);
                            int type = note.getType();
                            String form = note.getForm();
                            if (form != null && form.length() > 0 && type != NoteType.OSM.getTypeNum()) {
                                String name = note.getName();
                                double altim = note.getAltim();
                                Intent formIntent = new Intent(context, FormActivity.class);
                                formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_JSON, form);
                                formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_NAME, name);
                                formIntent.putExtra(LibraryConstants.LATITUDE, (double) lat);
                                formIntent.putExtra(LibraryConstants.LONGITUDE, (double) lon);
                                formIntent.putExtra(LibraryConstants.ELEVATION, (double) altim);
                                mapActivity.startActivityForResult(formIntent, MapsActivity.FORMUPDATE_RETURN_CODE);
                                doInfo = false;
                            }
                        }
                    } catch (IOException e1) {
                    }
                }

                if (doInfo) {
                    Builder builder = new AlertDialog.Builder(context);
                    builder.setIcon(android.R.drawable.ic_menu_info_details);
                    builder.setTitle(title);
                    StringBuilder sb = new StringBuilder();
                    if (snippet != null && snippet.length() > 0) {
                        sb.append(snippet);
                        sb.append("\n\n"); //$NON-NLS-1$
                    }

                    String latStr = context.getString(R.string.lat);
                    String lonStr = context.getString(R.string.lon);
                    sb.append(latStr).append(" ").append(lat).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    sb.append(lonStr).append(" ").append(lon); //$NON-NLS-1$

                    builder.setMessage(sb.toString());
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
            }
            return true;
        }
        return false;
    }
    @Override
    public void dispose() {
        context = null;
        super.dispose();
    }
}
