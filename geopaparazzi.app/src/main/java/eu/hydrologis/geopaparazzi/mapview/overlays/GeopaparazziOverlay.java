/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.mapview.overlays;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.android.ShapeWriter;
import com.vividsolutions.jts.android.geom.DrawableShape;
import com.vividsolutions.jts.android.geom.PathShape;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.model.GeoPoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.core.maps.SpatialiteMapOrderComparator;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.FormInfoHolder;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsService;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.geometry.GeometryIterator;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.library.style.Style;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.objects.Note;
import eu.hydrologis.geopaparazzi.database.objects.NoteOverlayItem;
import eu.hydrologis.geopaparazzi.mapview.MapviewActivity;
import eu.hydrologis.geopaparazzi.utilities.Constants;
import jsqlite.Exception;

/**
 * GeopaparazziOverlay is an abstract base class to display {@link OverlayWay OverlayWays}. The class defines some methods to
 * access the backing data structure of deriving subclasses.
 * <p/>
 * The overlay may be used to show additional ways such as calculated routes. Closed polygons, for example buildings or
 * areas, are also supported. A way node sequence is considered as a closed polygon if the first and the last way node
 * are equal.
 */
public abstract class GeopaparazziOverlay extends Overlay {

    private static final String THREAD_NAME = "GeopaparazziOverlay"; //$NON-NLS-1$

    /**
     * Sets the bounds of the given drawable so that (0,0) is the center of the bottom row.
     *
     * @param balloon the drawable whose bounds should be set.
     * @return the given drawable with set bounds.
     */
    public static Drawable boundCenter(Drawable balloon) {
        balloon.setBounds(balloon.getIntrinsicWidth() / -2, balloon.getIntrinsicHeight() / -2, balloon.getIntrinsicWidth() / 2,
                balloon.getIntrinsicHeight() / 2);
        return balloon;
    }

    /*
     * way stuff
     */
    private Paint wayStartPaintFill;
    private Paint defaultWayPaintFill;
    private Paint defaultWayPaintOutline;
    private Path wayPath;

    private Point itemPosition;
    private final List<Integer> visibleItems = new ArrayList<>();
    private final List<Integer> visibleItemsRedraw = new ArrayList<>();

    /*
     * gps stuff
     */
    private final Point circlePosition;
    private final Path path;

    private final GpsData overlayGps;
    private Drawable gpsMarker;

    private Path gpsPath;
    private final OverlayWay gpslogOverlay;
    private Paint gpsTrackPaintYellow;
    private Paint gpsTrackPaintBlack;
    private Paint gpsOutline;
    private Paint gpsFill;

    private List<GeoPoint> currentGpsLog = new ArrayList<>();
    private Paint textPaint;
    private Paint textHaloPaint;
    private boolean isNotesTextVisible;
    private boolean doNotesTextHalo;
    private GpsServiceStatus gpsServiceStatus = GpsServiceStatus.GPS_OFF;
    private GpsLoggingStatus gpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;

    /**
     * Create a {@link OverlayWay} wrapped type.
     *
     * @param context the context to use.
     */
    public GeopaparazziOverlay(Context context) {
        super();
        this.wayPath = new Path();
        this.wayPath.setFillType(Path.FillType.EVEN_ODD);

        this.itemPosition = new Point();

        // cross
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GeopaparazziApplication.getInstance());
        boolean isHighDensity = preferences.getBoolean(Constants.PREFS_KEY_RETINA, false);

        // gps
        overlayGps = new GpsData();
        this.circlePosition = new Point();
        this.path = new Path();
        this.gpsPath = new Path();

        wayStartPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        wayStartPaintFill.setStyle(Paint.Style.FILL);

        defaultWayPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        defaultWayPaintFill.setStyle(Paint.Style.FILL);
        defaultWayPaintFill.setColor(Color.RED);
        defaultWayPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
        defaultWayPaintOutline.setStyle(Paint.Style.STROKE);
        defaultWayPaintOutline.setColor(Color.BLACK);

        gpsMarker = Compat.getDrawable(context, R.drawable.current_position);
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

        gpsTrackPaintBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
        gpsTrackPaintBlack.setStyle(Paint.Style.STROKE);
        gpsTrackPaintBlack.setColor(Color.BLACK);

        if (!isHighDensity) {
            gpsTrackPaintYellow.setStrokeWidth(3);
            gpsTrackPaintBlack.setStrokeWidth(5);
        } else {
            gpsTrackPaintYellow.setStrokeWidth(8);
            gpsTrackPaintBlack.setStrokeWidth(12);
        }

        isNotesTextVisible = preferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_VISIBLE, true);
        if (isNotesTextVisible) {
            String notesTextSizeStr = preferences.getString(Constants.PREFS_KEY_NOTES_TEXT_SIZE, LibraryConstants.DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
            float notesTextSize = LibraryConstants.DEFAULT_NOTES_SIZE;
            try {
                notesTextSize = (float) Double.parseDouble(notesTextSizeStr);
            } catch (NumberFormatException e) {
                // ignore and use default
            }
            doNotesTextHalo = preferences.getBoolean(Constants.PREFS_KEY_NOTES_TEXT_DOHALO, true);
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(notesTextSize);
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
    public boolean onLongPress(GeoPoint geoPoint, MapView mapView) {
        return checkItemHit(geoPoint, mapView, EventType.LONG_PRESS);
        // return super.onLongPress(geoPoint, mapView);
    }

    /**
     * Checks whether an item has been tapped.
     */
    @Override
    public boolean onTap(GeoPoint geoPoint, MapView mapView) {
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

    private void drawWayPathOnCanvas(Canvas canvas, Point drawPosition, OverlayWay overlayWay) {
        // assemble the ways
        this.wayPath.reset();
        for (int i = 0; i < overlayWay.cachedWayPositions.length; ++i) {
            int x = overlayWay.cachedWayPositions[i][0].x - drawPosition.x;
            int y = overlayWay.cachedWayPositions[i][0].y - drawPosition.y;
            this.wayPath.moveTo(x, y);
            int lastX = 0;
            int lastY = 0;
            for (int j = 1; j < overlayWay.cachedWayPositions[i].length; ++j) {
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

    private void assembleGpsWayPath(Point drawPosition, OverlayWay overlayWay) {
        this.gpsPath.reset();
        for (int i = 0; i < overlayWay.cachedWayPositions.length; ++i) {
            this.gpsPath.moveTo(overlayWay.cachedWayPositions[i][0].x - drawPosition.x, overlayWay.cachedWayPositions[i][0].y
                    - drawPosition.y);
            for (int j = 1; j < overlayWay.cachedWayPositions[i].length; ++j) {
                this.gpsPath.lineTo(overlayWay.cachedWayPositions[i][j].x - drawPosition.x, overlayWay.cachedWayPositions[i][j].y
                        - drawPosition.y);
            }
        }
    }

    private void drawGpsWayPathOnCanvas(Canvas canvas) {
        canvas.drawPath(this.gpsPath, this.gpsTrackPaintBlack);
        canvas.drawPath(this.gpsPath, this.gpsTrackPaintYellow);
    }

    private void drawGpsOnCanvas(Canvas canvas) {
        canvas.drawPath(this.path, gpsOutline);
        canvas.drawPath(this.path, gpsFill);
    }

    /**
     * Set the current gps position.
     *
     * @param position         the {@link GeoPoint}.
     * @param accuracy         the accuracy.
     * @param gpsServiceStatus the gps status as defined by {@link GpsService#GPS_SERVICE_STATUS}.
     * @param gpsLoggingStatus the database logging status as defined by {@link GpsService#GPS_LOGGING_STATUS}.
     */
    @SuppressWarnings("nls")
    public void setGpsPosition(GeoPoint position, float accuracy, GpsServiceStatus gpsServiceStatus,
                               GpsLoggingStatus gpsLoggingStatus) {
        this.gpsServiceStatus = gpsServiceStatus;
        this.gpsLoggingStatus = gpsLoggingStatus;
        if (gpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            currentGpsLog.add(position);
        } else {
            currentGpsLog.clear();
        }
        if (GPLog.LOG_ABSURD && position != null)
            GPLog.addLogEntry(this, "Set gps data: " + position.getLongitude() + "/" + position.getLatitude() + "/" + accuracy);

        if (position != null) {
            overlayGps.setCircleData(position, accuracy);
        }
    }

    /**
     * Creates a way in this overlay.
     *
     * @param index the index of the way.
     * @return the way.
     */
    protected abstract OverlayWay createWay(int index);

    @Override
    protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {

        /*
         * first spatialite layers, if any
         */
        drawFromSpatialite(canvas, drawPosition, projection, drawZoomLevel);

        /*
         * WAYS
         */
        int numberOfWays = waySize();
        for (int wayIndex = 0; wayIndex < numberOfWays; ++wayIndex) {
            if (stopDrawing()) {
                // stop working
                return;
            }

            // get the current way
            OverlayWay overlayWay = createWay(wayIndex);
            if (overlayWay == null) {
                continue;
            }

            // make sure that the current way has way nodes
            if (overlayWay.wayNodes == null || overlayWay.wayNodes.length == 0) {
                continue;
            }

            // make sure that the cached way node positions are valid
            if (drawZoomLevel != overlayWay.cachedZoomLevel) {
                for (int i = 0; i < overlayWay.cachedWayPositions.length; ++i) {
                    for (int j = 0; j < overlayWay.cachedWayPositions[i].length; ++j) {
                        overlayWay.cachedWayPositions[i][j] = projection.toPoint(overlayWay.wayNodes[i][j],
                                overlayWay.cachedWayPositions[i][j], drawZoomLevel);
                    }
                }
                overlayWay.cachedZoomLevel = drawZoomLevel;
            }

            drawWayPathOnCanvas(canvas, drawPosition, overlayWay);
        }

        /*
         * ITEMS
         */

        // erase the list of visible items
        this.visibleItemsRedraw.clear();

        int canvasHeight = canvas.getHeight();
        int canvasWidth = canvas.getWidth();

        int numberOfItems = itemSize();
        for (int itemIndex = 0; itemIndex < numberOfItems; ++itemIndex) {
            if (stopDrawing()) {
                // stop working
                return;
            }

            // get the current item
            OverlayItem overlayItem = createItem(itemIndex);
            if (overlayItem == null) {
                continue;
            }

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
            Drawable itemMarker = overlayItem.getMarker();
            if (itemMarker == null) continue;

            // get the position of the marker
            Rect markerBounds = itemMarker.copyBounds();
            int intrinsicWidth = itemMarker.getIntrinsicWidth() / 2;
            int intrinsicHeight = itemMarker.getIntrinsicHeight() / 2;

            // calculate the bounding box of the marker
            int left;
            int right;
            int top;
            int itemBottom;
            if (overlayItem instanceof NoteOverlayItem) {
                left = this.itemPosition.x - intrinsicWidth;
                right = this.itemPosition.x + intrinsicWidth;
                top = this.itemPosition.y - intrinsicHeight;
                itemBottom = this.itemPosition.y + intrinsicHeight;
            } else {
                left = this.itemPosition.x + intrinsicWidth / 2 - intrinsicWidth;
                right = this.itemPosition.x + intrinsicWidth / 2 + intrinsicWidth;
                top = this.itemPosition.y + intrinsicHeight / 2 - intrinsicHeight;
                itemBottom = this.itemPosition.y + intrinsicHeight / 2 + intrinsicHeight;
            }

            // check if the bounding box of the marker intersects with the canvas
            if (right >= 0 && left <= canvasWidth && itemBottom >= 0 && top <= canvasHeight) {
                // set the position of the marker
                itemMarker.setBounds(left, top, right, itemBottom);

                // draw the item marker on the canvas
                itemMarker.draw(canvas);

                // restore the position of the marker
                itemMarker.setBounds(markerBounds);

                // add the current item index to the list of visible items
                this.visibleItemsRedraw.add(itemIndex);

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

        // swap the two visible item lists
        synchronized (visibleItems) {
            List<Integer> visibleItemsTemp = new ArrayList<>(visibleItems);
            visibleItems.clear();
            visibleItems.addAll(visibleItemsRedraw);
            visibleItemsRedraw.addAll(visibleItemsTemp);
        }

        /*
         * gps logging track
         */
        if (gpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            // if a track is recorded, show it
            synchronized (gpslogOverlay) {
                int size = currentGpsLog.size();
                if (size > 1) {
                    GeoPoint[] geoPoints = currentGpsLog.toArray(new GeoPoint[currentGpsLog.size()]);
                    gpslogOverlay.setWayNodes(new GeoPoint[][]{geoPoints});
                    // make sure that the current way has way nodes
                    if (gpslogOverlay.wayNodes != null && gpslogOverlay.wayNodes.length != 0) {
                        // make sure that the cached way node positions are valid
                        if (drawZoomLevel != gpslogOverlay.cachedZoomLevel) {
                            for (int i = 0; i < gpslogOverlay.cachedWayPositions.length; ++i) {
                                for (int j = 0; j < gpslogOverlay.cachedWayPositions[i].length; ++j) {
                                    gpslogOverlay.cachedWayPositions[i][j] = projection.toPoint(gpslogOverlay.wayNodes[i][j],
                                            gpslogOverlay.cachedWayPositions[i][j], drawZoomLevel);
                                }
                            }
                            gpslogOverlay.cachedZoomLevel = drawZoomLevel;
                        }

                        assembleGpsWayPath(drawPosition, gpslogOverlay);
                        drawGpsWayPathOnCanvas(canvas);
                    }
                }
            }
        }

        /*
         * GPS position
         */

        if (stopDrawing()) {
            // stop working
            return;
        }

        // get the current circle
        if (gpsServiceStatus == GpsServiceStatus.GPS_FIX && overlayGps.center != null) {
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
                            drawGpsOnCanvas(canvas);
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

    }

    private void drawFromSpatialite(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
        /*
         * draw from spatialite
         */
        double n = 90;
        double w = -180;
        double s = -90;
        double e = 180;
        try {
            GeoPoint zeroPoint = projection.fromPixels(0, 0);
            GeoPoint whPoint = projection.fromPixels(canvas.getWidth(), canvas.getHeight());
            n = zeroPoint.getLatitude();
            w = zeroPoint.getLongitude();
            s = whPoint.getLatitude();
            e = whPoint.getLongitude();
        } catch (java.lang.Exception e2) {
            GPLog.error(this, "Problems retrieving viewport bounds", e2); //$NON-NLS-1$
        }
        Envelope canvasEnvelope = new Envelope(w, e, s, n);
        try {
            HashMap<SpatialiteMap, SpatialVectorTable> spatialiteMaps2TablesMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap();
            HashMap<SpatialiteMap, SpatialiteDatabaseHandler> spatialiteMaps2DbHandlersMap = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2DbHandlersMap();

            List<SpatialiteMap> spatialiteMaps = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps();
            Collections.sort(spatialiteMaps, new SpatialiteMapOrderComparator());
            try {
                for (SpatialiteMap spatialiteMap : spatialiteMaps) {
                    if (stopDrawing()) {
                        // stop working
                        return;
                    }
                    if (!spatialiteMap.isVisible) {
                        continue;
                    }
                    SpatialiteDatabaseHandler spatialDatabaseHandler = spatialiteMaps2DbHandlersMap.get(spatialiteMap);
                    SpatialVectorTable spatialTable = spatialiteMaps2TablesMap.get(spatialiteMap);
                    Style style = spatialTable.getStyle();
                    if (drawZoomLevel < style.minZoom || drawZoomLevel > style.maxZoom) {
                        // we do not draw outside of the zoom levels
                        continue;
                    }

                    GeometryIterator geometryIterator = null;
                    try {
                        Paint fill = null;
                        Paint stroke = null;
                        if (style.fillcolor != null && style.fillcolor.trim().length() > 0)
                            fill = spatialDatabaseHandler.getFillPaint4Style(style);
                        if (style.strokecolor != null && style.strokecolor.trim().length() > 0)
                            stroke = spatialDatabaseHandler.getStrokePaint4Style(style);
                        PointTransformation pointTransformer = new MapsforgePointTransformation(projection, drawPosition,
                                drawZoomLevel);
                        ShapeWriter shapeWriter;
                        ShapeWriter shape_writer_point = null;
                        if (spatialTable.isPoint()) {
                            shapeWriter = new ShapeWriter(pointTransformer, style.shape,
                                    style.size);
                        } else {
                            shapeWriter = new ShapeWriter(pointTransformer);
                            if (spatialTable.isGeometryCollection()) {
                                shape_writer_point = new ShapeWriter(pointTransformer, style.shape,
                                        style.size);
                            }
                        }
                        shapeWriter.setRemoveDuplicatePoints(true);
                        shapeWriter.setDecimation(style.decimationFactor);
                        geometryIterator = spatialDatabaseHandler.getGeometryIteratorInBounds(
                                LibraryConstants.SRID_WGS84_4326, spatialTable, n, s, e, w);
                        while (geometryIterator.hasNext()) {
                            Geometry geom = geometryIterator.next();
                            if (geom != null) {
                                if (!canvasEnvelope.intersects(geom.getEnvelopeInternal())) {
                                    // TODO check the performance impact of this
                                    continue;
                                }
                                if (spatialTable.isGeometryCollection()) {
                                    int geometriesCount = geom.getNumGeometries();
                                    for (int j = 0; j < geometriesCount; j++) {
                                        Geometry geom_collect = geom.getGeometryN(j);
                                        if (geom_collect != null) {
                                            String geometryType = geom_collect.getGeometryType();
                                            if (geometryType.toUpperCase().contains("POINT")) {
                                                drawGeometry(geom_collect, canvas, shape_writer_point, fill, stroke);
                                            } else {
                                                drawGeometry(geom_collect, canvas, shapeWriter, fill, stroke);
                                            }
                                            if (stopDrawing()) { // stop working
                                                return;
                                            }
                                        }
                                    }
                                } else {
                                    drawGeometry(geom, canvas, shapeWriter, fill, stroke);
                                    if (stopDrawing()) { // stop working
                                        return;
                                    }
                                }
                            } else {
                                GPLog.error(this, "GeopaparazziOverlay.drawFromSpatialite  [geom == null] description["
                                        + spatialTable.getTableName() + "]", new NullPointerException());
                            }
                        }
                    } finally {
                        if (geometryIterator != null)
                            geometryIterator.close();
                    }


                }
            } catch (ConcurrentModificationException cme) {
                GPLog.error(this, "Error while looping on spatialite maps, skipped rendering.", cme);
                return;
            }


            /*
             * draw labels
             */
            for (Map.Entry<SpatialiteMap, SpatialiteDatabaseHandler> entry : spatialiteMaps2DbHandlersMap.entrySet()) {
                if (stopDrawing()) {
                    // stop working
                    return;
                }
                SpatialiteMap spatialiteMap = entry.getKey();
                if (!spatialiteMap.isVisible) {
                    continue;
                }

                SpatialiteDatabaseHandler spatialDatabaseHandler = entry.getValue();

                SpatialVectorTable spatialTable = spatialiteMaps2TablesMap.get(spatialiteMap);
                Style style = spatialTable.getStyle();

                if (style.labelvisible == 0) {
                    continue;
                }
                if (drawZoomLevel < style.minZoom || drawZoomLevel > style.maxZoom) {
                    // we do not draw outside of the zoom levels
                    continue;
                }

                float delta = style.size / 2f;
                if (delta < 2) {
                    delta = 2;
                }

                Paint dbTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                dbTextPaint.setStyle(Paint.Style.FILL);
                dbTextPaint.setColor(Color.BLACK);
                dbTextPaint.setTextSize(style.labelsize);
                Paint dbTextHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                dbTextHaloPaint.setStyle(Paint.Style.STROKE);
                dbTextHaloPaint.setStrokeWidth(3);
                dbTextHaloPaint.setColor(Color.WHITE);
                dbTextHaloPaint.setTextSize(style.labelsize);

                GeometryIterator geometryIterator = null;
                try {
                    PointTransformation pointTransformer = new MapsforgePointTransformation(projection, drawPosition,
                            drawZoomLevel);
                    ShapeWriter linesWriter = null;
                    if (spatialTable.isLine()) {
                        linesWriter = new ShapeWriter(pointTransformer, spatialTable.getStyle().shape,
                                spatialTable.getStyle().size);
                        dbTextHaloPaint.setTextAlign(Align.CENTER);
                        dbTextPaint.setTextAlign(Align.CENTER);
                    } else {
                        dbTextHaloPaint.setTextAlign(Align.LEFT);
                        dbTextPaint.setTextAlign(Align.LEFT);
                    }

                    if (spatialDatabaseHandler.isOpen()) {
                        geometryIterator = spatialDatabaseHandler.getGeometryIteratorInBounds(
                                LibraryConstants.SRID_WGS84_4326, spatialTable, n, s, e, w);
                        while (geometryIterator.hasNext()) {
                            Geometry geom = geometryIterator.next();
                            if (geom != null) {
                                if (!canvasEnvelope.intersects(geom.getEnvelopeInternal())) {
                                    // TODO check the performance impact of this
                                    continue;
                                }
                                String labelText = geometryIterator.getLabelText();
                                if (labelText == null || labelText.length() == 0) {
                                    continue;
                                }
                                if (spatialTable.isGeometryCollection()) {
                                    int geometriesCount = geom.getNumGeometries();
                                    for (int j = 0; j < geometriesCount; j++) {
                                        Geometry geom_collect = geom.getGeometryN(j);
                                        if (geom_collect != null) {
                                            drawLabel(pointTransformer, geom_collect, labelText, canvas, dbTextPaint,
                                                    dbTextHaloPaint, delta, linesWriter);
                                            if (stopDrawing()) { // stop working
                                                return;
                                            }
                                        }
                                    }
                                } else {
                                    drawLabel(pointTransformer, geom, labelText, canvas, dbTextPaint, dbTextHaloPaint, delta,
                                            linesWriter);
                                    if (stopDrawing()) { // stop working
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (geometryIterator != null)
                        geometryIterator.close();
                }
            }
        } catch (Exception e1) {
            GPLog.error(this, "GeopaparazziOverlay.drawFromSpatialite [failed]", e1); //$NON-NLS-1$
        }
    }

    private boolean stopDrawing() {
        return isInterrupted() || sizeHasChanged() || needRedraw();
    }

    private static void drawGeometry(Geometry geom, Canvas canvas, ShapeWriter shape_writer, Paint fill, Paint stroke) {
        String s_geometry_type = geom.getGeometryType();
        int i_geometry_type = GeometryType.forValue(s_geometry_type);
        GeometryType geometry_type = GeometryType.forValue(i_geometry_type);
        DrawableShape shape = shape_writer.toShape(geom);
        switch (geometry_type) {
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
                // GPLog.androidLog(-1,"GeopaparazziOverlay.drawGeometry geometry_type["+s_geometry_type+"]: ["+i_geometry_type+"]");
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

    private static void drawLabel(PointTransformation pointTransformer, Geometry geom, String label, Canvas canvas,
                                  Paint dbTextPaint, Paint dbTextHaloPaint, float delta, ShapeWriter linesWriter) {

        if (linesWriter == null) {
            /*
             * for points and polygons for now just use the centroid
             */
            com.vividsolutions.jts.geom.Point centroid = geom.getCentroid();
            Coordinate coordinate = centroid.getCoordinate();
            PointF dest = new PointF();
            pointTransformer.transform(coordinate, dest);
            float x = dest.x + delta;
            float y = dest.y - delta;
            // if (doNotesTextHalo)
            canvas.drawText(label, x, y, dbTextHaloPaint);
            canvas.drawText(label, x, y, dbTextPaint);
        } else {
            DrawableShape shape = linesWriter.toShape(geom);
            if (shape instanceof PathShape) {
                PathShape lineShape = (PathShape) shape;
                Path linePath = lineShape.getPath();
                // if (doNotesTextHalo)
                int hOffset = 15;
                int vOffset = -5;
                canvas.drawTextOnPath(label, linePath, hOffset, vOffset, dbTextHaloPaint);
                canvas.drawTextOnPath(label, linePath, hOffset, vOffset, dbTextPaint);
            }
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
     * @param index the index of the item.
     * @return the item.
     */
    protected abstract OverlayItem createItem(int index);

    /**
     * Checks whether an item has been hit by an event and calls the appropriate handler.
     *
     * @param geoPoint  the point of the event.
     * @param mapView   the {@link MapView} that triggered the event.
     * @param eventType the type of the event.
     * @return true if an item has been hit, false otherwise.
     */
    protected boolean checkItemHit(GeoPoint geoPoint, MapView mapView, EventType eventType) {
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
            for (int i = this.visibleItems.size() - 1; i >= 0; --i) {
                Integer itemIndex = this.visibleItems.get(i);

                // get the current item
                OverlayItem checkOverlayItem = createItem(itemIndex);
                if (checkOverlayItem == null) {
                    continue;
                }

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
                Drawable marker = checkOverlayItem.getMarker();
                if (marker == null) return false;
                Rect checkMarkerBounds = marker.getBounds();

                // calculate the bounding box of the marker
                int checkLeft = checkItemPoint.x + checkMarkerBounds.left;
                int checkRight = checkItemPoint.x + checkMarkerBounds.right;
                int checkTop = checkItemPoint.y + checkMarkerBounds.top;
                int checkBottom = checkItemPoint.y + checkMarkerBounds.bottom;

                // check if the event position is within the bounds of the marker
                if (checkRight >= eventPosition.x && checkLeft <= eventPosition.x && checkBottom >= eventPosition.y
                        && checkTop <= eventPosition.y) {
                    switch (eventType) {
                        case LONG_PRESS:
                            if (onLongPress(itemIndex)) {
                                return true;
                            }
                            break;

                        case TAP:
                            if (onTap(context, itemIndex)) {
                                return true;
                            }
                            break;
                    }
                }
            }
        }

        // no hit
        return false;
    }

    /**
     * Handles a long press event.
     * <p/>
     * The default implementation of this method does nothing and returns false.
     *
     * @param index the index of the item that has been long pressed.
     * @return true if the event was handled, false otherwise.
     */
    protected boolean onLongPress(int index) {
        return false;
    }

    /**
     * Handles a tap event.
     * <p/>
     * The default implementation of this method does nothing and returns false.
     *
     * @param index the index of the item that has been tapped.
     * @return true if the event was handled, false otherwise.
     */
    protected boolean onTap(Context context, int index) {
        OverlayItem item = createItem(index);
        if (item != null) {
            String title = item.getTitle();
            String snippet = item.getSnippet();

            GeoPoint position = item.getPoint();
            int latE6 = position.latitudeE6;
            int lonE6 = position.longitudeE6;
            float lat = latE6 / LibraryConstants.E6;
            float lon = lonE6 / LibraryConstants.E6;
            if (title != null && ImageUtilities.isImagePath(title)) {
                openImage(context, title, snippet);
            } else {

                boolean doInfo = true;
                if (context instanceof MapviewActivity) {
                    MapviewActivity mapActivity = (MapviewActivity) context;

                    float n = lat + LibraryConstants.PICKRADIUS;
                    float s = lat - LibraryConstants.PICKRADIUS;
                    float w = lon - LibraryConstants.PICKRADIUS;
                    float e = lon + LibraryConstants.PICKRADIUS;

                    try {
                        List<Note> notesInWorldBounds = DaoNotes.getNotesList(new float[]{n, s, w, e}, false);
                        if (notesInWorldBounds.size() > 0) {
                            Note note = notesInWorldBounds.get(0);
//                            String description = note.getDescription();
                            String name = note.getName();
                            String form = note.getForm();
                            if (form != null && form.length() > 0) {
                                double altim = note.getAltim();

                                Intent formIntent = new Intent(context, FormActivity.class);
                                FormInfoHolder formInfoHolder = new FormInfoHolder();
                                formInfoHolder.sectionName = name;
                                formInfoHolder.formName = null;
                                formInfoHolder.noteId = note.getId();
                                formInfoHolder.longitude = lon;
                                formInfoHolder.latitude = lat;
                                formInfoHolder.elevation = altim;
                                formInfoHolder.sectionObjectString = form;
                                formInfoHolder.objectExists = true;
                                formIntent.putExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER, formInfoHolder);

                                mapActivity.startActivityForResult(formIntent, MapviewActivity.FORMUPDATE_RETURN_CODE);
                                doInfo = false;
                            }
                        }
                    } catch (IOException e1) {
                        GPLog.error(this, "Error", e1);
                    }
                }

                if (doInfo) {
                    StringBuilder sb = new StringBuilder();
                    if (snippet != null && snippet.length() > 0) {
                        sb.append(snippet);
                        sb.append("\n"); //$NON-NLS-1$
                    }

                    String latStr = context.getString(R.string.lat);
                    String lonStr = context.getString(R.string.lon);
                    sb.append(latStr).append(" ").append(lat).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
                    sb.append(lonStr).append(" ").append(lon); //$NON-NLS-1$

                    GPDialogs.infoDialog(context, sb.toString(), null);
                }
            }
            return true;
        }
        return false;
    }

    private void openImage(Context context, String title, String snippet) {

        try {
            File tempDir = ResourcesManager.getInstance(context).getTempDir();
            // get image from db
            long imageID = Long.parseLong(snippet);

            int length = title.length();
            String ext = title.substring(length - 4, length);
            byte[] imageData = new DaoImages().getImageData(imageID);


            final File newTempFile = new File(tempDir, ImageUtilities.getTempImageName(ext));
            ImageUtilities.writeImageDataToFile(imageData, newTempFile.getAbsolutePath());
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(newTempFile), "image/*"); //$NON-NLS-1$
            context.startActivity(intent);
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
