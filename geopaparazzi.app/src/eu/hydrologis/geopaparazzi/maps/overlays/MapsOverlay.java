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
package eu.hydrologis.geopaparazzi.maps.overlays;

import static eu.hydrologis.geopaparazzi.util.Constants.E6;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.maps.DataManager;
import eu.hydrologis.geopaparazzi.maps.MapItem;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.PointsContainer;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Overlay to show imported maps.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class MapsOverlay extends Overlay {

    private Paint gpxPaint = new Paint();
    private Context context;

    private int decimationFactor;

    final private Rect screenRect = new Rect();

    private boolean touchDragging = false;
    private boolean doDraw = true;
    private boolean gpsUpdate = false;
    private int zoomLevel1;
    private int zoomLevel2;
    private int zoomLevelLabelLength1;
    private int zoomLevelLabelLength2;
    private Paint gpxTextPaint;

    public MapsOverlay( final Context ctx, final ResourceProxy pResourceProxy ) {
        super(pResourceProxy);
        this.context = ctx;
        ApplicationManager applicationManager = ApplicationManager.getInstance(context);
        decimationFactor = applicationManager.getDecimationFactor();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        zoomLevel1 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM1, "14"));
        zoomLevel2 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM2, "16"));
        zoomLevelLabelLength1 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM1_LABELLENGTH, "4"));
        zoomLevelLabelLength2 = Integer.parseInt(preferences.getString(Constants.PREFS_KEY_ZOOM2_LABELLENGTH, "-1"));

        float textSizeMedium = ctx.getResources().getDimension(R.dimen.text_normal);
        gpxPaint = new Paint();
        gpxTextPaint = new Paint();
        gpxTextPaint.setAntiAlias(true);
        gpxTextPaint.setTextSize(textSizeMedium);
    }

    public void setDoDraw( boolean doDraw ) {
        this.doDraw = doDraw;
        Logger.d(this, "Will draw: " + doDraw);
    }

    public void setGpsUpdate( boolean gpsUpdate ) {
        this.gpsUpdate = gpsUpdate;
    }
    private HashMap<MapItem, PointsContainer> pointsContainerMap = new HashMap<MapItem, PointsContainer>();

    protected void draw( final Canvas canvas, final MapView mapsView, final boolean shadow ) {
        if (touchDragging || shadow || !doDraw || mapsView.isAnimating() || !DataManager.getInstance().areMapsVisible())
            return;

        BoundingBoxE6 boundingBox = mapsView.getBoundingBox();
        float y0 = boundingBox.getLatNorthE6() / E6;
        float y1 = boundingBox.getLatSouthE6() / E6;
        float x0 = boundingBox.getLonWestE6() / E6;
        float x1 = boundingBox.getLonEastE6() / E6;

        Projection pj = mapsView.getProjection();

        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        screenRect.contains(0, 0, screenWidth, screenHeight);
        mapsView.getScreenRect(screenRect);

        int zoomLevel = mapsView.getZoomLevel();

        try {
            List<MapItem> mapsMap = DaoMaps.getMaps(context);
            if (!gpsUpdate) {
                pointsContainerMap.clear();
                for( MapItem mapItem : mapsMap ) {
                    if (!mapItem.isVisible()) {
                        continue;
                    }
                    PointsContainer coordsContainer = DaoMaps.getCoordinatesInWorldBoundsForMapIdDecimated2(context,
                            mapItem.getId(), y0, y1, x0, x1, pj, decimationFactor);
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

                    final Point p = new Point();
                    for( int i = 0; i < coordsContainer.getIndex(); i++ ) {
                        pj.toMapPixels(new GeoPoint(latArray[i], lonArray[i]), p);
                        float screenX = p.x;
                        float screenY = p.y;
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
                    // boolean hasNames = namesArray.length == latArray.length;

                    final Point p = new Point();
                    for( int i = 0; i < coordsContainer.getIndex(); i++ ) {
                        pj.toMapPixels(new GeoPoint(latArray[i], lonArray[i]), p);
                        float screenX = p.x;
                        float screenY = p.y;

                        canvas.drawPoint(screenX, screenY, gpxPaint);
                        drawLabel(canvas, namesArray[i], screenX, screenY, gpxTextPaint, zoomLevel);
                    }
                }

            }

        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

    }

    private void drawLabel( Canvas canvas, String label, float positionX, float positionY, Paint paint, int zoom ) {
        if (label == null || label.length() == 0) {
            return;
        }
        if (zoom >= zoomLevel1) {
            if (zoom < zoomLevel2) {
                if (zoomLevelLabelLength1 != -1 && label.length() > zoomLevelLabelLength1) {
                    label = label.substring(0, zoomLevelLabelLength1);
                }
            } else {
                if (zoomLevelLabelLength2 != -1 && label.length() > zoomLevelLabelLength2) {
                    label = label.substring(0, zoomLevelLabelLength2);
                }
            }
            canvas.drawText(label, positionX, positionY, paint);
            // Logger.d(this, "WRITING: " + label);
        }
    }

    @Override
    public boolean onTouchEvent( MotionEvent event, MapView mapView ) {
        int action = event.getAction();
        switch( action ) {
        case MotionEvent.ACTION_MOVE:
            touchDragging = true;
            break;
        case MotionEvent.ACTION_UP:
            touchDragging = false;
            mapView.invalidate();
            break;
        }
        return super.onTouchEvent(event, mapView);
    }

}
