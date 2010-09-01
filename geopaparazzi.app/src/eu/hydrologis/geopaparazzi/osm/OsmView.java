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
package eu.hydrologis.geopaparazzi.osm;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.round;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.location.Location;
import android.preference.PreferenceManager;
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
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.PointsContainer;
/**
 * The view showing the gps position on OSM tiles. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OsmView extends View implements ApplicationManagerListener {
    private static final int TILESIZE = 256;
    public static final String LOGTAG = "OSMVIEW"; //$NON-NLS-1$

    protected static final int ZOOMIN = 0;

    private float centerLat = 46.674056f;
    private float centerLon = 11.132294f;
    private float gpsLat = 46.674056f;
    private float gpsLon = 11.132294f;
    private float gotoLat = -1;
    private float gotoLon = -1;

    private int zoom = 16;
    private Paint gpxTextPaint;
    private Paint gpxPaint;
    private Paint redPaint;
    private Paint redRectPaint;
    private int lastX = -1;
    private int lastY = -1;
    private float pixelDxInWorld;
    private float pixelDyInWorld;
    private Bitmap positionIcon;
    private Bitmap gotoIcon;

    private boolean isMeasureMode = false;
    private float measuredDistance = -1;
    private List<PointF> dragList = new ArrayList<PointF>();

    private TileCache tileCache = null;
    private boolean doShowTilesFrames;
    private int width;
    private int height;
    private int currentX;
    private int currentY;
    private Paint crossPaint;
    private Paint measurePaint;
    private Paint measureTextPaint;
    private String distanceString;
    private boolean touchDragging;
    private SharedPreferences preferences;
    private long lastTouchTime;
    private final OsmActivity osmActivity;

    public OsmView( final OsmActivity osmActivity ) {
        super(osmActivity);
        this.osmActivity = osmActivity;
        tileCache = new TileCache(null);

        redPaint = new Paint();
        redPaint.setColor(Color.RED);
        redPaint.setAlpha(255);
        redPaint.setTextSize(redPaint.getTextSize() + 1f);

        redRectPaint = new Paint();
        redRectPaint.setColor(Color.RED);
        redRectPaint.setStyle(Paint.Style.STROKE);

        gpxPaint = new Paint();
        gpxTextPaint = new Paint();
        gpxTextPaint.setAntiAlias(true);

        measurePaint = new Paint();
        measurePaint.setAntiAlias(true);
        measurePaint.setColor(Color.DKGRAY);
        measurePaint.setStrokeWidth(3f);
        measurePaint.setStyle(Paint.Style.STROKE);
        measurePaint.setTextSize(measurePaint.getTextSize() + 3f);

        crossPaint = new Paint();
        crossPaint.setAntiAlias(true);
        crossPaint.setColor(Color.GRAY);
        crossPaint.setStrokeWidth(0.5f);
        crossPaint.setStyle(Paint.Style.STROKE);

        measureTextPaint = new Paint();
        measureTextPaint.setAntiAlias(true);
        measureTextPaint.setTextSize(measureTextPaint.getTextSize() + 3f);

        distanceString = getResources().getString(R.string.distance);

        ApplicationManager deviceManager = ApplicationManager.getInstance();
        GpsLocation loc = deviceManager.getLoc();
        if (loc != null) {
            gpsLat = (float) loc.getLatitude();
            gpsLon = (float) loc.getLongitude();
            centerLat = (float) loc.getLatitude();
            centerLon = (float) loc.getLongitude();
        }

        doShowTilesFrames = deviceManager.doShowTilesFrames();

        positionIcon = BitmapFactory.decodeResource(getResources(), R.drawable.current_position);
        gotoIcon = BitmapFactory.decodeResource(getResources(), R.drawable.goto_position);

        deviceManager.setOsmView(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(osmActivity);
        zoom = preferences.getInt(Constants.PREFS_KEY_ZOOM, 16);

        // setLongClickable(true);
        // this.setOnLongClickListener(new View.OnLongClickListener(){
        //
        // @Override
        // public boolean onLongClick( View v ) {
        // Intent intent = new Intent(Constants.TAKE_NOTE);
        // intent.putExtra(Constants.PREFS_KEY_LAT, currentYscreenToLat);
        // intent.putExtra(Constants.PREFS_KEY_LON, currentXscreenToLon);
        // osmActivity.startActivity(intent);
        // return true;
        // }
        //
        // });

    }

    protected void onDraw( Canvas canvas ) {

        try {
            width = getMeasuredWidth();
            height = getMeasuredHeight();

            // http://wiki.openstreetmap.org/index.php/Slippy_map_tilenames
            int[] xyTile = TileCache.latLon2ContainingTileNumber(centerLat, centerLon, zoom);
            // get central tile info
            BoundingBox centralBB = tileNumber2BoundingBox(xyTile[0], xyTile[1]);
            // Log.v(LOGTAG, "0:0 - " + centralBB.toString());
            Bitmap tileBitmap = tileCache.get(zoom, xyTile[0], xyTile[1]);
            canvas.drawBitmap(tileBitmap, centralBB.left, centralBB.top, null);

            for( int i = -1; i < 2; i++ ) {
                for( int j = -1; j < 2; j++ ) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    int xtile = xyTile[0] + i;
                    int ytile = xyTile[1] + j;
                    tileBitmap = tileCache.get(zoom, xtile, ytile);
                    if (tileBitmap == null) {
                        continue;
                    }

                    int left = centralBB.left + i * TILESIZE;
                    int top = centralBB.top + j * TILESIZE;
                    canvas.drawBitmap(tileBitmap, left, top, null);

                    if (doShowTilesFrames) {
                        canvas.drawRect(left, top, left + TILESIZE, top - TILESIZE, redRectPaint);
                        StringBuilder sb = new StringBuilder();
                        sb.append("/"); //$NON-NLS-1$
                        sb.append(zoom);
                        sb.append("/"); //$NON-NLS-1$
                        sb.append(xtile);
                        sb.append("/"); //$NON-NLS-1$
                        String folder = sb.toString();
                        String img = ytile + ".png"; //$NON-NLS-1$
                        String tileDef = folder + img;
                        canvas.drawText(tileDef, left + 5, top + 20, redPaint);
                    }

                }
            }

            if (!touchDragging) {
                drawMaps(canvas, width, height);
                drawGpslogs(canvas, width, height);
                drawNotes(canvas, width, height);
            }

            // gps position
            float gpsX = lonToScreen(width, gpsLon, centerLon, pixelDxInWorld);
            float gpsY = latToScreen(height, gpsLat, centerLat, pixelDyInWorld);

            if ((gpsX >= 0 && gpsX <= width) && (gpsY >= 0 && gpsY <= height)) {
                canvas.drawBitmap(positionIcon, gpsX - 20f, gpsY - 20f, null);
            }

            // measure
            if (isMeasureMode && measuredDistance != -1) {
                for( int i = 0; i < dragList.size() - 1; i++ ) {
                    PointF f = dragList.get(i);
                    PointF s = dragList.get(i + 1);
                    canvas.drawLine(f.x, f.y, s.x, s.y, measurePaint);
                }
                canvas.drawText(distanceString + (int) measuredDistance + " meters", 5, 15, measureTextPaint); //$NON-NLS-1$
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
            e.printStackTrace();
        }

    }

    private void drawMaps( Canvas canvas, int width, int height ) {
        if (!DataManager.getInstance().areMapsVisible())
            return;
        float y0 = screenToLat(height, 0, centerLat, pixelDyInWorld);
        float x0 = screenToLon(width, 0, centerLon, pixelDxInWorld);
        float y1 = screenToLat(height, height, centerLat, pixelDyInWorld);
        float x1 = screenToLon(width, width, centerLon, pixelDxInWorld);

        try {
            List<MapItem> mapsMap = DaoMaps.getMaps();
            HashMap<Long, PointsContainer> coordsInWorldBounds = DaoMaps.getCoordinatesInWorldBounds(y0, y1, x0, x1);
            if (coordsInWorldBounds.size() == 0) {
                return;
            }
            for( MapItem mapItem : mapsMap ) {
                if (!mapItem.isVisible()) {
                    continue;
                }
                PointsContainer coordsContainer = coordsInWorldBounds.get(mapItem.getId());
                if (coordsContainer == null) {
                    continue;
                }
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

                    List<Double> latList = coordsContainer.getLatList();
                    List<Double> lonList = coordsContainer.getLonList();

                    for( int i = 0; i < latList.size(); i++ ) {
                        float screenX = lonToScreen(width, lonList.get(i).floatValue(), centerLon, pixelDxInWorld);
                        float screenY = latToScreen(height, latList.get(i).floatValue(), centerLat, pixelDyInWorld);
                        // Log.d(LOGTAG, screenX + "/" + screenY);
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

                    List<Double> latList = coordsContainer.getLatList();
                    List<Double> lonList = coordsContainer.getLonList();
                    List<String> namesList = coordsContainer.getNamesList();
                    boolean hasNames = namesList.size() == latList.size();

                    for( int i = 0; i < latList.size(); i++ ) {
                        float screenX = lonToScreen(width, lonList.get(i).floatValue(), centerLon, pixelDxInWorld);
                        float screenY = latToScreen(height, latList.get(i).floatValue(), centerLat, pixelDyInWorld);

                        canvas.drawPoint(screenX, screenY, gpxPaint);
                        if (zoom > 12 && hasNames) {
                            canvas.drawText(namesList.get(i), screenX, screenY, gpxTextPaint);
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void drawGpslogs( Canvas canvas, int width, int height ) {
        if (!DataManager.getInstance().areLogsVisible())
            return;
        float y0 = screenToLat(height, 0, centerLat, pixelDyInWorld);
        float x0 = screenToLon(width, 0, centerLon, pixelDxInWorld);
        float y1 = screenToLat(height, height, centerLat, pixelDyInWorld);
        float x1 = screenToLon(width, width, centerLon, pixelDxInWorld);

        try {
            List<MapItem> gpslogs = DaoGpsLog.getGpslogs();
            HashMap<Long, Line> linesInWorldBounds = DaoGpsLog.getLinesInWorldBounds(y0, y1, x0, x1);
            for( MapItem gpslogItem : gpslogs ) {
                if (!gpslogItem.isVisible()) {
                    continue;
                }
                Line line = linesInWorldBounds.get(gpslogItem.getId());
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

                List<Double> latList = line.getLatList();
                List<Double> lonList = line.getLonList();

                for( int i = 0; i < latList.size(); i++ ) {
                    float screenX = lonToScreen(width, lonList.get(i).floatValue(), centerLon, pixelDxInWorld);
                    float screenY = latToScreen(height, latList.get(i).floatValue(), centerLat, pixelDyInWorld);
                    // Log.d(LOGTAG, screenX + "/" + screenY);
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
            e.printStackTrace();
        }
    }

    private void drawNotes( Canvas canvas, int width, int height ) {
        if (!DataManager.getInstance().areNotesVisible())
            return;

        float y0 = screenToLat(height, 0, centerLat, pixelDyInWorld);
        float y1 = screenToLat(height, height, centerLat, pixelDyInWorld);
        float x0 = screenToLon(width, 0, centerLon, pixelDxInWorld);
        float x1 = screenToLon(width, width, centerLon, pixelDxInWorld);

        try {
            List<Note> notesInWorldBounds = DaoNotes.getNotesInWorldBounds(y0, y1, x0, x1);
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
                if (zoom > 12) {
                    canvas.drawText(note.getName(), screenX, screenY, gpxTextPaint);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // private void drawGpx( Canvas canvas, int width, int height ) {
    // RectF viewPort = new RectF(0, 0, width, height);
    // List<GpxItem> gpxItems = GpxFilesManager.getInstance().getGpxItems();
    // for( GpxItem gpxItem : gpxItems ) {
    // // visible?
    // boolean isVisible = gpxItem.isVisible();
    // if (!isVisible) {
    // continue;
    // }
    //
    // String colorStr = gpxItem.getColor();
    // int color = Color.parseColor(colorStr);
    // float gWidth = Float.parseFloat(gpxItem.getWidth());
    //
    // List<PointF3D> points = gpxItem.read();
    // // intersecting viewport?
    // float screenE = lonToScreen(width, gpxItem.getE(), centerLon, pixelDxInWorld);
    // float screenW = lonToScreen(width, gpxItem.getW(), centerLon, pixelDxInWorld);
    // float screenS = latToScreen(height, gpxItem.getS(), centerLat, pixelDyInWorld);
    // float screenN = latToScreen(height, gpxItem.getN(), centerLat, pixelDyInWorld);
    // RectF dataRect = new RectF(screenW, screenN, screenE, screenS);
    // if (!dataRect.intersect(viewPort)) {
    // Log.d(LOGTAG, gpxItem.getFilename() + " out of viewport");
    // continue;
    // }
    // // ok, let's do this
    // if (!gpxItem.isLine()) {
    // List<String> names = gpxItem.getNames();
    // if (names.size() != points.size()) {
    // names = null;
    // }
    // gpxPaint.setColor(color);
    // gpxPaint.setStrokeWidth(gWidth);
    // // gpxPaint.setAlpha(255);
    // // gpxPaint.setTextSize(redPaint.getTextSize() + 1f);
    // // gpxPaint.setStyle(Paint.Style.STROKE);
    // float prevScreenX = Float.POSITIVE_INFINITY;
    // float prevScreenY = Float.POSITIVE_INFINITY;
    // for( int i = 0; i < points.size(); i++ ) {
    // PointF point = points.get(i);
    // float screenX = lonToScreen(width, point.x, centerLon, pixelDxInWorld);
    // float screenY = latToScreen(height, point.y, centerLat, pixelDyInWorld);
    // if (prevScreenX == screenX && prevScreenY == screenY) {
    // continue;
    // }
    // if (viewPort.contains(screenX, screenY)) {
    // canvas.drawPoint(screenX, screenY, gpxPaint);
    // if (names != null) {
    // canvas.drawText(names.get(i), screenX, screenY, gpxTextPaint);
    // }
    // prevScreenX = screenX;
    // prevScreenY = screenY;
    // }
    // }
    //
    // } else {
    // gpxPaint.setAntiAlias(true);
    // gpxPaint.setColor(color);
    // gpxPaint.setStrokeWidth(gWidth);
    // // gpxPaint.setAlpha(255);
    // // gpxPaint.setTextSize(redPaint.getTextSize() + 1f);
    // gpxPaint.setStyle(Paint.Style.STROKE);
    // // gpxPaint.setDither(true);
    // gpxPaint.setStrokeJoin(Paint.Join.ROUND);
    // gpxPaint.setStrokeCap(Paint.Cap.ROUND);
    // Path path = new Path();
    // float prevScreenX = Float.POSITIVE_INFINITY;
    // float prevScreenY = Float.POSITIVE_INFINITY;
    // boolean doNew = true;
    // for( int i = 0; i < points.size(); i++ ) {
    // PointF point = points.get(i);
    // float screenX = lonToScreen(width, point.x, centerLon, pixelDxInWorld);
    // float screenY = latToScreen(height, point.y, centerLat, pixelDyInWorld);
    // // Log.d(LOGTAG, screenX + "/" + screenY);
    // if (viewPort.contains(screenX, screenY)) {
    // if (doNew) {
    // path.moveTo(screenX, screenY);
    // doNew = false;
    // } else {
    // if (prevScreenX == screenX && prevScreenY == screenY) {
    // continue;
    // }
    // path.lineTo(screenX, screenY);
    // prevScreenX = screenX;
    // prevScreenY = screenY;
    // }
    // } else {
    // doNew = true;
    // }
    // }
    // canvas.drawPath(path, gpxPaint);
    // }
    // }
    // }
    private float latToScreen( float height, float lat, float centerY, float resolutionY ) {
        return height / 2f - (lat - centerY) / resolutionY;
    }
    private float screenToLat( float height, float screenY, float centerLat, float resolutionY ) {
        float lat = centerLat + (height / 2f - screenY) * resolutionY;
        return lat;
    }

    private float lonToScreen( float width, float lon, float centerX, float resolutionX ) {
        return width / 2f + (lon - centerX) / resolutionX;
    }

    private float screenToLon( float width, float screenX, float centerLon, float resolutionX ) {
        float lon = screenX * resolutionX - width * resolutionX / 2 + centerLon;
        return lon;
    }

    public boolean requestFocus( int direction, Rect previouslyFocusedRect ) {
        boolean requestFocus = super.requestFocus(direction, previouslyFocusedRect);

        invalidateWithProgress();

        return requestFocus;
    }

    public void onLocationChanged( GpsLocation loc ) {
        if (!isShown()) {
            return;
        }
        this.gpsLat = (float) loc.getLatitude();
        this.gpsLon = (float) loc.getLongitude();

        invalidateWithProgress();
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
            long thisTime = System.currentTimeMillis();
            long delta = thisTime - lastTouchTime;
            // Log.i(LOGTAG, "Time: " + thisTime + " Delta: " + delta);
            if (delta < 300) {
                // double tag emulation
                Intent intent = new Intent(Constants.TAKE_NOTE);
                float currentYscreenToLat = screenToLat(height, currentY, centerLat, pixelDyInWorld);
                float currentXscreenToLon = screenToLon(width, currentX, centerLon, pixelDxInWorld);
                intent.putExtra(Constants.PREFS_KEY_LAT, currentYscreenToLat);
                intent.putExtra(Constants.PREFS_KEY_LON, currentXscreenToLon);
                osmActivity.startActivity(intent);
                return true;
            } else {
                lastTouchTime = thisTime;
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
                float currentYscreenToLat = screenToLat(height, currentY, centerLat, pixelDyInWorld);
                float currentXscreenToLon = screenToLon(width, currentX, centerLon, pixelDxInWorld);
                first.setLatitude(currentYscreenToLat);
                first.setLongitude(currentXscreenToLon);
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
            break;
        case MotionEvent.ACTION_UP:
            touchDragging = false;
            measuredDistance = -1;
            break;
        }

        invalidate();
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
}