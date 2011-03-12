/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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
package eu.hydrologis.geopaparazzi.maps;

import static eu.hydrologis.geopaparazzi.util.Constants.GPSLAST_LATITUDE;
import static eu.hydrologis.geopaparazzi.util.Constants.GPSLAST_LONGITUDE;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManagerListener;
import eu.hydrologis.geopaparazzi.util.BoundingBox;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.LineArray;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.PointsContainer;
import eu.hydrologis.geopaparazzi.util.debug.Debug;
import eu.hydrologis.geopaparazzi.util.debug.Logger;
/**
 * The view showing the gps position on OSM tiles. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapView extends View implements ApplicationManagerListener {
    private static final int TILESIZE = 256;
    public static final String LOGTAG = "MAPVIEW"; //$NON-NLS-1$

    protected static final int ZOOMIN = 0;

    private float centerLat = 46.674056f;
    private float centerLon = 11.132294f;
    private float gpsLat = 46.674056f;
    private float gpsLon = 11.132294f;
    private float gotoLat = -1;
    private float gotoLon = -1;

    private float screenNorth;
    private float screenWest;
    private float screenSouth;
    private float screenEast;

    private int zoom = 16;
    private static Paint gpxTextPaint;
    private static Paint gpxPaint;
    private static Paint xPaint;
    private int lastX = -1;
    private int lastY = -1;
    private float pixelDxInWorld;
    private float pixelDyInWorld;
    private static Bitmap positionIcon;
    private static Bitmap gotoIcon;
    private static int gpsIconWidth;
    private static int gpsIconHeight;

    private boolean isMeasureMode = false;
    private float measuredDistance = -1;
    private List<PointF> dragList = new ArrayList<PointF>();

    private TileCache tileCache = null;

    private int width;
    private int height;
    private int currentX;
    private int currentY;
    private static Paint crossPaint;
    private static Paint measurePaint;
    private static Paint measureTextPaint;
    private static String distanceString;
    private static List<Float> measureCoordinatesX = new ArrayList<Float>(30);
    private static List<Float> measureCoordinatesY = new ArrayList<Float>(30);
    private static String metersString;
    private static float actionBarHeight;

    private int zoomLevel1;
    private int zoomLevelLabelLength1;
    private int zoomLevel2;
    private int zoomLevelLabelLength2;

    private boolean touchDragging;
    private SharedPreferences preferences;
    private Context context;

    public MapView( Context context, AttributeSet set ) {
        super(context, set);
        this.context = context;
        init();
    }

    public MapView( Context context ) {
        super(context);
        this.context = context;
        init();
    }

    private void init() {
        if (xPaint == null) {

            xPaint = new Paint();
            xPaint.setColor(Color.rgb(175, 198, 233));

            String textSizeMediumStr = getResources().getString(R.string.text_normal);
            float textSizeNormal = Float.parseFloat(textSizeMediumStr);
            gpxPaint = new Paint();
            gpxTextPaint = new Paint();
            gpxTextPaint.setAntiAlias(true);
            gpxTextPaint.setTextSize(textSizeNormal);

            measurePaint = new Paint();
            measurePaint.setAntiAlias(true);
            measurePaint.setColor(Color.DKGRAY);
            measurePaint.setStrokeWidth(3f);
            measurePaint.setStyle(Paint.Style.STROKE);
            // measurePaint.setTextSize(measurePaint.getTextSize() + 3f);

            crossPaint = new Paint();
            crossPaint.setAntiAlias(true);
            crossPaint.setColor(Color.GRAY);
            crossPaint.setStrokeWidth(0.5f);
            crossPaint.setStyle(Paint.Style.STROKE);

            measureTextPaint = new Paint();
            measureTextPaint.setAntiAlias(true);
            measureTextPaint.setTextSize(textSizeNormal);

            distanceString = getResources().getString(R.string.distance);
            metersString = getResources().getString(R.string.meters);

            positionIcon = BitmapFactory.decodeResource(getResources(), R.drawable.current_position);
            gotoIcon = BitmapFactory.decodeResource(getResources(), R.drawable.goto_position);
            gpsIconWidth = positionIcon.getWidth();
            gpsIconHeight = positionIcon.getHeight();

            actionBarHeight = getResources().getDimension(R.dimen.action_bar_height);
        }

        ApplicationManager applicationManager = ApplicationManager.getInstance(context);
        decimationFactor = applicationManager.getDecimationFactor();
        File osmCacheDir = applicationManager.getMapsCacheDir();
        boolean internetIsOn = applicationManager.isInternetOn();
        tileCache = new TileCache(osmCacheDir, internetIsOn, null);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        GpsLocation loc = applicationManager.getLoc();
        if (loc != null) {
            gpsLat = (float) loc.getLatitude();
            gpsLon = (float) loc.getLongitude();
        }
        gpsLon = preferences.getFloat(GPSLAST_LONGITUDE, gpsLon);
        gpsLat = preferences.getFloat(GPSLAST_LATITUDE, gpsLat);
        centerLat = gpsLat;
        centerLon = gpsLon;

        zoom = preferences.getInt(Constants.PREFS_KEY_ZOOM, 16);

        // displayMetrics = new DisplayMetrics();
        // context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    }

    @Override
    protected void onWindowVisibilityChanged( int visibility ) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == 8) {
            Editor editor = preferences.edit();
            editor.putFloat(GPSLAST_LONGITUDE, centerLon);
            editor.putFloat(GPSLAST_LATITUDE, centerLat);
            editor.commit();
        }
    }

    protected void onDraw( Canvas canvas ) {
        try {
            width = getMeasuredWidth();
            height = getMeasuredHeight();

            canvas.drawARGB(255, 215, 227, 244);
            int dx = (int) (width / 10f * 0.3f);
            int offset = (int) (width / 10f * 0.7f);
            for( int runY = 0; runY < height; runY = runY + dx + offset ) {
                for( int runX = 0; runX < width; runX = runX + dx + offset ) {
                    RectF r = new RectF(runX, runY, runX + dx, runY + dx);
                    canvas.drawOval(r, xPaint);
                }
            }

            // http://wiki.openstreetmap.org/index.php/Slippy_map_tilenames
            int[] xyTile = TileCache.latLon2ContainingTileNumber(centerLat, centerLon, zoom);
            // get central tile info
            BoundingBox centralBB = tileNumber2BoundingBox(xyTile[0], xyTile[1]);

            screenNorth = screenToLat(height, 0, centerLat, pixelDyInWorld);
            screenWest = screenToLon(width, 0, centerLon, pixelDxInWorld);
            screenSouth = screenToLat(height, height, centerLat, pixelDyInWorld);
            screenEast = screenToLon(width, width, centerLon, pixelDxInWorld);

            // Log.v(LOGTAG, "0:0 - " + centralBB.toString());
            Bitmap tileBitmap;
            if (Debug.doDrawNormal) {
                tileBitmap = tileCache.get(zoom, xyTile[0], xyTile[1]);
                if (tileBitmap != null) {
                    canvas.drawBitmap(tileBitmap, centralBB.left, centralBB.top, null);
                }
            } else {
                drawTileFrame(canvas, xyTile[0], xyTile[1], centralBB.left, centralBB.top, "c:" + xyTile[0] + "-" + xyTile[1]);
            }

            // calculate upper and lower tiles needed
            int upperDiff = centralBB.top;
            int lowerDiff = height - centralBB.bottom;
            int upperNum = 0;
            if (upperDiff > 0) {
                upperNum = (int) Math.ceil((float) upperDiff / TILESIZE);
            }
            int lowerNum = 0;
            if (lowerDiff > 0) {
                lowerNum = (int) Math.ceil((float) lowerDiff / TILESIZE);
            }
            int leftDiff = centralBB.left;
            int rightDiff = width - centralBB.right;
            int leftNum = 0;
            if (leftDiff > 0) {
                leftNum = (int) Math.ceil((float) leftDiff / TILESIZE);
            }
            int rightNum = 0;
            if (rightDiff > 0) {
                rightNum = (int) Math.ceil((float) rightDiff / TILESIZE);
            }
            // Log.v(LOGTAG, "upper/lower num: " + upperNum + " / " + lowerNum);
            // Log.v(LOGTAG, "left/right num: " + leftNum + " / " + rightNum);

            for( int i = -leftNum; i <= rightNum; i++ ) {
                for( int j = -upperNum; j <= lowerNum; j++ ) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    int xtile = xyTile[0] + i;
                    int ytile = xyTile[1] + j;
                    int left = centralBB.left + i * TILESIZE;
                    int top = centralBB.top + j * TILESIZE;

                    if (Debug.doDrawNormal) {
                        tileBitmap = tileCache.get(zoom, xtile, ytile);
                        if (tileBitmap == null) {
                            continue;
                        }
                        canvas.drawBitmap(tileBitmap, left, top, null);
                    } else {
                        drawTileFrame(canvas, xtile, ytile, left, top, xtile + "-" + ytile);
                    }

                }
            }

            if (!touchDragging) {
                drawMaps(canvas, width, height);
                drawGpslogs(canvas, width, height);
                drawNotes(canvas, width, height);
            }
            gpsUpdate = false;

            // gps position
            float gpsX = lonToScreen(width, gpsLon, centerLon, pixelDxInWorld);
            float gpsY = latToScreen(height, gpsLat, centerLat, pixelDyInWorld);

            // logGpsCoords(gpsX, gpsY);
            if ((gpsX >= 0 && gpsX <= width) && (gpsY >= 0 && gpsY <= height)) {
                canvas.drawBitmap(positionIcon, gpsX - gpsIconWidth / 2f, gpsY - gpsIconHeight / 2f, null);
            }

            // measure
            if (isMeasureMode && measuredDistance != -1) {
                for( int i = 0; i < dragList.size() - 1; i++ ) {
                    PointF f = dragList.get(i);
                    PointF s = dragList.get(i + 1);
                    canvas.drawLine(f.x, f.y, s.x, s.y, measurePaint);
                }

                StringBuilder sb = new StringBuilder();
                sb.append(distanceString);
                sb.append((int) measuredDistance);
                sb.append(metersString);
                canvas.drawText(sb.toString(), 5, 15 + actionBarHeight, measureTextPaint); //$NON-NLS-1$
            }

            if (gotoLat != -1) {

                float gotoX = width / 2f + (gotoLon - centerLon) / pixelDxInWorld;
                float gotoY = height / 2f - (gotoLat - centerLat) / pixelDyInWorld;

                if ((gotoX >= 0 && gotoX <= width) && (gotoY >= 0 && gotoY <= height)) {
                    canvas.drawBitmap(gotoIcon, gotoX - 9f, gotoY - 9f, null);
                }
            }

            canvas.drawLine(width / 2f, 0f, width / 2f, height, crossPaint);
            canvas.drawLine(0, height / 2f, width, height / 2f, crossPaint);

        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

    }

    // private void logGpsCoords( float gpsX, float gpsY ) {
    // StringBuilder sb = new StringBuilder();
    // sb.append("Pre-Drawing coords: ");
    // sb.append(gpsLon);
    // sb.append("/");
    // sb.append(gpsLat);
    // sb.append("=  screen: ");
    // sb.append(gpsX);
    // sb.append("/");
    // sb.append(gpsY);
    // Logger.d(this, sb.toString());
    // }

    /**
     * Draws the tile frames. Meant for debugging purposes.
     * 
     * @param canvas
     * @param xtile
     * @param ytile
     * @param left
     * @param top
     * @param prefix
     */
    private void drawTileFrame( Canvas canvas, int xtile, int ytile, int left, int top, String prefix ) {
        Paint redPaint = new Paint();
        redPaint.setColor(Color.RED);
        redPaint.setTextSize(redPaint.getTextSize() + 1f);

        Paint redRectPaint = new Paint();
        redRectPaint.setColor(Color.RED);
        redRectPaint.setStrokeWidth(3f);
        redRectPaint.setStyle(Paint.Style.STROKE);
        if (left > 0) {
            canvas.drawLine(left, 0, left, height, redRectPaint);
        }
        if (left + TILESIZE > 0) {
            canvas.drawLine(left + TILESIZE, 0, left + TILESIZE, height, redRectPaint);
        }
        if (top > 0) {
            canvas.drawLine(0, top, width, top, redRectPaint);
        }
        if (top - TILESIZE > 0) {
            canvas.drawLine(0, top - TILESIZE, width, top - TILESIZE, redRectPaint);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(prefix); //$NON-NLS-1$
        sb.append("/"); //$NON-NLS-1$
        sb.append(zoom);
        sb.append("/"); //$NON-NLS-1$
        sb.append(xtile);
        sb.append("/"); //$NON-NLS-1$
        sb.append(ytile);
        sb.append(".png"); //$NON-NLS-1$
        String tileDef = sb.toString();
        canvas.drawText(tileDef, left + 5, top + 20, redPaint);
    }

    /**
     * Draws the external loaded maps on map.
     * 
     * @param canvas
     * @param width
     * @param height
     */
    private HashMap<MapItem, PointsContainer> pointsContainerMap = new HashMap<MapItem, PointsContainer>();
    private void drawMaps( Canvas canvas, int width, int height ) {
        if (!DataManager.getInstance().areMapsVisible())
            return;
        float y0 = screenNorth;
        float x0 = screenWest;
        float y1 = screenSouth;
        float x1 = screenEast;

        try {

            List<MapItem> mapsMap = DaoMaps.getMaps(context);
            if (!gpsUpdate) {
                pointsContainerMap.clear();
                for( MapItem mapItem : mapsMap ) {
                    if (!mapItem.isVisible()) {
                        continue;
                    }
                    PointsContainer coordsContainer = DaoMaps.getCoordinatesInWorldBoundsForMapIdDecimated(context,
                            mapItem.getId(), y0, y1, x0, x1, width, height, centerLon, centerLat, pixelDxInWorld, pixelDyInWorld);
                    if (coordsContainer == null) {
                        continue;
                    }

                    pointsContainerMap.put(mapItem, coordsContainer);
                }
            }

            Set<Entry<MapItem, PointsContainer>> entrySet = pointsContainerMap.entrySet();
            for( Entry<MapItem, PointsContainer> entry : entrySet ) {
                MapItem mapItem = entry.getKey();
                PointsContainer coordsContainer = entry.getValue();
                if (mapItem.getType() == Constants.MAP_TYPE_LINE) {
                    gpxPaint.setAntiAlias(true);
                    gpxPaint.setColor(Color.parseColor(mapItem.getColor()));
                    gpxPaint.setStrokeWidth(mapItem.getWidth());
                    gpxPaint.setStyle(Paint.Style.STROKE);
                    gpxPaint.setStrokeJoin(Paint.Join.ROUND);
                    gpxPaint.setStrokeCap(Paint.Cap.ROUND);
                    Path path = new Path();
                    float prevScreenX = Float.POSITIVE_INFINITY;
                    float prevScreenY = Float.POSITIVE_INFINITY;

                    float[] latArray = coordsContainer.getLatArray();
                    float[] lonArray = coordsContainer.getLonArray();

                    for( int i = 0; i < coordsContainer.getIndex(); i++ ) {
                        float screenX = lonToScreen(width, lonArray[i], centerLon, pixelDxInWorld);
                        float screenY = latToScreen(height, latArray[i], centerLat, pixelDyInWorld);
                        // Logger.d(LOGTAG, screenX + "/" + screenY);
                        if (i == 0) {
                            path.moveTo(screenX, screenY);
                        } else {
                            if (prevScreenX == screenX && prevScreenY == screenY) {
                                continue;
                            }
                            path.lineTo(screenX, screenY);
                            prevScreenX = screenX;
                            prevScreenY = screenY;
                        }
                    }
                    canvas.drawPath(path, gpxPaint);
                } else if (mapItem.getType() == Constants.MAP_TYPE_POINT) {
                    gpxPaint.setAntiAlias(true);
                    gpxPaint.setColor(Color.parseColor(mapItem.getColor()));
                    gpxPaint.setStrokeWidth(mapItem.getWidth());
                    gpxPaint.setStyle(Paint.Style.FILL);

                    float[] latArray = coordsContainer.getLatArray();
                    float[] lonArray = coordsContainer.getLonArray();
                    String[] namesArray = coordsContainer.getNamesArray();
                    boolean hasNames = namesArray.length == latArray.length;

                    for( int i = 0; i < coordsContainer.getIndex(); i++ ) {
                        float screenX = lonToScreen(width, lonArray[i], centerLon, pixelDxInWorld);
                        float screenY = latToScreen(height, latArray[i], centerLat, pixelDyInWorld);

                        canvas.drawPoint(screenX, screenY, gpxPaint);
                        if (zoom >= zoomLevel1 && hasNames && namesArray[i] != null) {
                            String text = namesArray[i];
                            if (zoom < zoomLevel2) {
                                if (zoomLevelLabelLength1 != -1 && text.length() > zoomLevelLabelLength1) {
                                    text = text.substring(0, zoomLevelLabelLength1);
                                }
                            } else {
                                if (zoomLevelLabelLength2 != -1 && text.length() > zoomLevelLabelLength2) {
                                    text = text.substring(0, zoomLevelLabelLength2);
                                }
                            }
                            canvas.drawText(text, screenX, screenY, gpxTextPaint);
                        }
                    }
                }

            }
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }
    }
    /**
     * Draw the geopap registered logs on map.
     * 
     * @param canvas
     * @param width
     * @param height
     */
    private HashMap<Long, LineArray> linesInWorldBounds = new HashMap<Long, LineArray>();
    private void drawGpslogs( Canvas canvas, int width, int height ) {
        if (!DataManager.getInstance().areLogsVisible())
            return;
        float y0 = screenNorth;
        float x0 = screenWest;
        float y1 = screenSouth;
        float x1 = screenEast;

        try {

            List<MapItem> gpslogs = DaoGpsLog.getGpslogs(context);
            ApplicationManager applicationManager = ApplicationManager.getInstance(context);
            long currentLogId = applicationManager.getCurrentRecordedLogId();

            // if (!gpsUpdate || linesInWorldBounds == null) {
            // linesInWorldBounds = DaoGpsLog.getLinesInWorldBoundsDecimated(context, y0, y1, x0,
            // x1, width, height, centerLon,
            // centerLat, pixelDxInWorld, pixelDyInWorld, lastLogId);
            // }
            // LineArray currentLine = DaoGpsLog.getLinesInWorldBoundsByIdDecimated(context, y0, y1,
            // x0, x1, width, height,
            // centerLon, centerLat, pixelDxInWorld, pixelDyInWorld, lastLogId);

            // HashMap<Long, Line> linesInWorldBounds = DaoGpsLog.getLinesInWorldBounds(context, y0,
            // y1, x0, x1);
            for( MapItem gpslogItem : gpslogs ) {
                if (!gpslogItem.isVisible()) {
                    continue;
                }
                Long id = gpslogItem.getId();
                LineArray line;
                if (id == currentLogId) {
                    // we always reread the current log to make it proceed
                    line = DaoGpsLog.getLinesInWorldBoundsByIdDecimated(context, y0, y1, x0, x1, width, height, centerLon,
                            centerLat, pixelDxInWorld, pixelDyInWorld, id, decimationFactor);
                } else {
                    // for the other logs we cache depending on a gps update or a touch draw event
                    if (gpsUpdate) {
                        // draw was triggered by gps moving, we get the cached from before
                        line = linesInWorldBounds.get(id);
                    } else {
                        // if the draw comes from no gps update, reread the track
                        linesInWorldBounds.remove(id);
                        line = DaoGpsLog.getLinesInWorldBoundsByIdDecimated(context, y0, y1, x0, x1, width, height, centerLon,
                                centerLat, pixelDxInWorld, pixelDyInWorld, id, decimationFactor);
                        linesInWorldBounds.put(id, line);
                    }
                }

                if (line == null) {
                    continue;
                }
                gpxPaint.setAntiAlias(true);
                gpxPaint.setColor(Color.parseColor(gpslogItem.getColor()));
                gpxPaint.setStrokeWidth(gpslogItem.getWidth());
                gpxPaint.setStyle(Paint.Style.STROKE);
                gpxPaint.setStrokeJoin(Paint.Join.ROUND);
                gpxPaint.setStrokeCap(Paint.Cap.ROUND);
                Path path = new Path();
                float prevScreenX = Float.POSITIVE_INFINITY;
                float prevScreenY = Float.POSITIVE_INFINITY;

                float[] lonArray = line.getLonArray();
                float[] latArray = line.getLatArray();
                int index = line.getIndex();
                for( int i = 0; i < index; i++ ) {
                    float screenX = lonToScreen(width, lonArray[i], centerLon, pixelDxInWorld);
                    float screenY = latToScreen(height, latArray[i], centerLat, pixelDyInWorld);
                    // Logger.d(LOGTAG, screenX + "/" + screenY);
                    if (i == 0) {
                        path.moveTo(screenX, screenY);
                    } else {
                        if (prevScreenX == screenX && prevScreenY == screenY) {
                            continue;
                        }
                        path.lineTo(screenX, screenY);
                        prevScreenX = screenX;
                        prevScreenY = screenY;
                    }
                }
                canvas.drawPath(path, gpxPaint);

            }
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }
    }
    private List<Note> notesInWorldBounds;
    private void drawNotes( Canvas canvas, int width, int height ) {
        if (!DataManager.getInstance().areNotesVisible())
            return;

        float y0 = screenToLat(height, 0, centerLat, pixelDyInWorld);
        float y1 = screenToLat(height, height, centerLat, pixelDyInWorld);
        float x0 = screenToLon(width, 0, centerLon, pixelDxInWorld);
        float x1 = screenToLon(width, width, centerLon, pixelDxInWorld);

        try {
            if (!gpsUpdate || notesInWorldBounds == null)
                notesInWorldBounds = DaoNotes.getNotesInWorldBounds(getContext(), y0, y1, x0, x1);
            int notesColor = DataManager.getInstance().getNotesColor();
            float notesWidth = DataManager.getInstance().getNotesWidth();
            for( Note note : notesInWorldBounds ) {
                gpxPaint.setAntiAlias(true);
                gpxPaint.setColor(notesColor);
                gpxPaint.setStrokeWidth(notesWidth);
                gpxPaint.setStyle(Paint.Style.FILL);

                float lat = (float) note.getLat();
                float lon = (float) note.getLon();

                float screenX = lonToScreen(width, lon, centerLon, pixelDxInWorld);
                float screenY = latToScreen(height, lat, centerLat, pixelDyInWorld);

                canvas.drawPoint(screenX, screenY, gpxPaint);
                if (zoom >= zoomLevel1) {
                    String text = note.getName();
                    if (zoom < zoomLevel2) {
                        if (zoomLevelLabelLength1 != -1 && text.length() > zoomLevelLabelLength1) {
                            text = text.substring(0, zoomLevelLabelLength1);
                        }
                    } else {
                        if (zoomLevelLabelLength2 != -1 && text.length() > zoomLevelLabelLength2) {
                            text = text.substring(0, zoomLevelLabelLength2);
                        }
                    }
                    canvas.drawText(text, screenX, screenY, gpxTextPaint);
                }
            }
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }
    }

    public static float latToScreen( float height, float lat, float centerY, float resolutionY ) {
        return height / 2f - (lat - centerY) / resolutionY;
    }
    public static float screenToLat( float height, float screenY, float centerLat, float resolutionY ) {
        float lat = centerLat + (height / 2f - screenY) * resolutionY;
        return lat;
    }

    public static float lonToScreen( float width, float lon, float centerX, float resolutionX ) {
        return width / 2f + (lon - centerX) / resolutionX;
    }

    public static float screenToLon( float width, float screenX, float centerLon, float resolutionX ) {
        float lon = screenX * resolutionX - width * resolutionX / 2 + centerLon;
        return lon;
    }

    public boolean requestFocus( int direction, Rect previouslyFocusedRect ) {
        boolean requestFocus = super.requestFocus(direction, previouslyFocusedRect);

        invalidateWithProgress();

        return requestFocus;
    }

    private float previousGpsLat = Float.MAX_VALUE;
    private float previousGpsLon = Float.MAX_VALUE;
    private boolean gpsUpdate = false;
    public void onLocationChanged( GpsLocation loc ) {
        if (!isShown()) {
            return;
        }
        this.gpsLat = (float) loc.getLatitude();
        this.gpsLon = (float) loc.getLongitude();

        // if gpspoints are the same, do not redraw
        float thres = 0.0001f;
        if (Math.abs(gpsLon - previousGpsLon) < thres && Math.abs(gpsLat - previousGpsLat) < thres) {
            return;
        }

        // if gps it outside of screen do not redraw
        float screenX = lonToScreen(width, gpsLon, centerLon, pixelDxInWorld);
        if (screenX > width || screenX < 0) {
            return;
        }
        float screenY = latToScreen(height, gpsLat, centerLat, pixelDyInWorld);
        if (screenY > height || screenY < 0) {
            return;
        }

        gpsUpdate = true;
        invalidateWithProgress();
    }

    public void onSensorChanged( double normalAzimuth, double pictureAzimuth ) {
    }

    public void zoomIn() {
        zoom = zoom + 1;
        if (zoom > 18)
            zoom = 18;

        Editor editor = preferences.edit();
        editor.putInt(Constants.PREFS_KEY_ZOOM, zoom);
        editor.commit();
        invalidateWithProgress();

    }

    public void zoomOut() {
        zoom = zoom - 1;
        if (zoom < 0)
            zoom = 0;

        Editor editor = preferences.edit();
        editor.putInt(Constants.PREFS_KEY_ZOOM, zoom);
        editor.commit();
        invalidateWithProgress();
    }

    public void zoomTo( int zoomLevel ) {
        zoom = zoomLevel;

        Editor editor = preferences.edit();
        editor.putInt(Constants.PREFS_KEY_ZOOM, zoom);
        editor.commit();
        invalidateWithProgress();
    }

    public void setGotoCoordinate( double lon, double lat ) {
        centerLat = (float) lat;
        centerLon = (float) lon;
        gotoLat = centerLat;
        gotoLon = centerLon;

        invalidate();
    }

    public void centerOnGps() {
        centerLat = gpsLat;
        centerLon = gpsLon;
        invalidateWithProgress();
    }

    public float getCenterLat() {
        return centerLat;
    }

    public float getCenterLon() {
        return centerLon;
    }

    /**
     * Fetches tiles wherever they are and puts them in cache for immediate use.
     * 
     * @throws IOException
     */
    private void fetchTiles() throws IOException {
        int[] xyTile = TileCache.latLon2ContainingTileNumber(centerLat, centerLon, zoom);
        tileCache.get(zoom, xyTile[0], xyTile[1]);

        for( int i = -1; i < 2; i++ ) {
            for( int j = -1; j < 2; j++ ) {
                if (i == 0 && j == 0) {
                    continue;
                }
                int xtile = xyTile[0] + i;
                int ytile = xyTile[1] + j;
                tileCache.get(zoom, xtile, ytile);
            }
        }
    }

    public void clearCache() {
        if (tileCache != null) {
            tileCache.clear();
        }
    }

    private int downX = 0;
    private int downY = 0;
    private int decimationFactor;
    public boolean onTouchEvent( MotionEvent event ) {
        int action = event.getAction();

        currentX = (int) round(event.getX());
        currentY = (int) round(event.getY());

        if (lastX == -1 || lastY == -1) {
            // lose the first drag and set the delta
            lastX = currentX;
            lastY = currentY;
            return true;
        }
        switch( action ) {
        case MotionEvent.ACTION_DOWN:
            downX = currentX;
            downY = currentY;
            // Logger.i(LOGTAG, "Time: " + thisTime + " Delta: " + delta);
            float currentYscreenToLat = screenToLat(height, currentY, centerLat, pixelDyInWorld);
            float currentXscreenToLon = screenToLon(width, currentX, centerLon, pixelDxInWorld);
            if (isMeasureMode) {
                measureCoordinatesX.add(currentXscreenToLon);
                measureCoordinatesY.add(currentYscreenToLat);
            }
            break;
        case MotionEvent.ACTION_MOVE:
            touchDragging = true;
            int dx = currentX - lastX;
            int dy = currentY - lastY;
            if (abs(dx) > 40 || abs(dy) > 40) {
                // Log.e(LOGTAG, "dx/dy = " + dx + "/" + dy);
                lastX = currentX;
                lastY = currentY;
                /*
                 * assume it is a touchscreen malfunctioning,
                 * since often it happens that a single short touch 
                 * triggers -180, which doesn't make any sense.
                 */
                return true;
            }

            if (isMeasureMode) {
                Location first = new Location("dummy"); //$NON-NLS-1$
                float movingYscreenToLat = screenToLat(height, currentY, centerLat, pixelDyInWorld);
                float movingXscreenToLon = screenToLon(width, currentX, centerLon, pixelDxInWorld);
                measureCoordinatesX.add(movingXscreenToLon);
                measureCoordinatesY.add(movingYscreenToLat);
                first.setLatitude(movingYscreenToLat);
                first.setLongitude(movingXscreenToLon);
                Location second = new Location("dummy"); //$NON-NLS-1$
                second.setLatitude(screenToLat(height, lastY, centerLat, pixelDyInWorld));
                second.setLongitude(screenToLon(width, lastX, centerLon, pixelDxInWorld));

                float distanceTo = first.distanceTo(second);
                if (measuredDistance == -1) {
                    measuredDistance = 0;
                    dragList.clear();
                }
                measuredDistance = measuredDistance + distanceTo;
                dragList.add(new PointF(currentX, currentY));
            } else {
                // if not measuring, we should pan
                // the center has to move of the same inverse distance
                centerLon = centerLon - (float) dx * pixelDxInWorld;
                centerLat = centerLat + (float) dy * pixelDyInWorld;
            }

            lastX = currentX;
            lastY = currentY;
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            touchDragging = false;
            if (isMeasureMode) {
                // open info view
                int size = measureCoordinatesX.size();
                float[] xArray = new float[size];
                float[] yArray = new float[size];
                for( int i = 0; i < xArray.length; i++ ) {
                    xArray[i] = measureCoordinatesX.get(i);
                    yArray[i] = measureCoordinatesY.get(i);
                }
                measureCoordinatesX.clear();
                measureCoordinatesY.clear();

                Intent intent = new Intent(Constants.MEASUREMENT_INFO);
                intent.putExtra(Constants.MEASURECOORDSX, xArray);
                intent.putExtra(Constants.MEASURECOORDSY, yArray);
                intent.putExtra(Constants.MEASUREDIST, measuredDistance);
                context.startActivity(intent);
            }
            measuredDistance = -1;
            double down2up = sqrt(pow(downX - currentX, 2.0) + pow(downY - currentY, 2.0));
            Logger.i(this, "down2up: " + down2up);
            if (down2up > 8) {
                invalidate();
            }
            break;
        }

        return true;
    }

    /**
     * Fetches needed tiles outside the UI thread, so that feedback can be given to the user.
     */
    private void invalidateWithProgress() {
        try {
            fetchTiles();
            invalidate();
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Creates a {@link BoundingBox} object from the zoom level and tiles numbers.
     * 
     * @param x tile column.
     * @param y tile row.
     * @return the {@link BoundingBox} of the tile.
     */
    private BoundingBox tileNumber2BoundingBox( int x, int y ) {

        BoundingBox bb = new BoundingBox();
        bb.north = (float) tile2lat(y, zoom);
        bb.south = (float) tile2lat(y + 1, zoom);
        bb.west = (float) tile2lon(x, zoom);
        bb.east = (float) tile2lon(x + 1, zoom);

        pixelDxInWorld = (bb.east - bb.west) / (float) TILESIZE;
        pixelDyInWorld = (bb.north - bb.south) / (float) TILESIZE;
        int screenWidth = getMeasuredWidth();
        int screenHeight = getMeasuredHeight();

        double halfScreenWorldWidth = ((double) screenWidth / 2.0) * pixelDxInWorld;
        double halfScreenWorldHeight = ((double) screenHeight / 2.0) * pixelDyInWorld;

        double leftBorderLongitude = centerLon - halfScreenWorldWidth;
        double upperBorderLatitude = centerLat + halfScreenWorldHeight;
        double xOffsetWorld = bb.west - leftBorderLongitude;
        double yOffsetWorld = upperBorderLatitude - bb.north;
        double xOffsetScreen = (float) (xOffsetWorld / pixelDxInWorld);
        double yOffsetScreen = (float) (yOffsetWorld / pixelDyInWorld);

        bb.left = (int) round(xOffsetScreen);
        bb.top = (int) round(yOffsetScreen);
        bb.right = (int) round(xOffsetScreen) + TILESIZE;
        bb.bottom = (int) round(yOffsetScreen) + TILESIZE;

        return bb;
    }

    private double tile2lon( int x, int z ) {
        return (x / pow(2.0, z) * 360.0) - 180.0;
    }

    private double tile2lat( int y, int z ) {
        double n = PI - ((2.0 * PI * y) / pow(2.0, z));
        return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)));
    }

    public boolean isMeasureMode() {
        return isMeasureMode;
    }

    public void setMeasureMode( boolean isMeasureMode ) {
        // if (isMeasureMode) {
        // setLongClickable(false);
        // } else {
        // setLongClickable(true);
        // }
        this.isMeasureMode = isMeasureMode;
        if (!isMeasureMode) {
            // reset distance
            measuredDistance = -1;
        }
    }

    public void onSatellitesStatusChanged( int num, int max ) {
    }

    public void setZoomLabelsParams( int zoomLevel1, int zoomLevelLabelLength1, int zoomLevel2, int zoomLevelLabelLength2 ) {
        this.zoomLevel1 = zoomLevel1;
        this.zoomLevelLabelLength1 = zoomLevelLabelLength1;
        this.zoomLevel2 = zoomLevel2;
        this.zoomLevelLabelLength2 = zoomLevelLabelLength2;
    }

    public float getScreenNorth() {
        return screenNorth;
    }

    public float getScreenSouth() {
        return screenSouth;
    }

    public float getScreenEast() {
        return screenEast;
    }

    public float getScreenWest() {
        return screenWest;
    }

}