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

import static java.lang.Math.*;
import static org.osmdroid.util.constants.GeoConstants.FEET_PER_METER;
import static org.osmdroid.util.constants.GeoConstants.METERS_PER_NAUTICAL_MILE;
import static org.osmdroid.util.constants.GeoConstants.METERS_PER_STATUTE_MILE;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * Overlay to show the measure.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MeasureToolOverlay extends Overlay {

    private final Paint measurePaint = new Paint();
    private final Paint measureTextPaint = new Paint();
    private boolean doDraw = true;
    private boolean isOn = false;

    private final Path measurePath = new Path();
    private int currentX;
    private int currentY;
    private int lastX;
    private int lastY;

    private boolean imperial = false;
    private boolean nautical = false;

    private float measuredDistance = Float.NaN;
    private String distanceString;
    private final ResourceProxy resourceProxy;

    public MeasureToolOverlay( final Context ctx, final ResourceProxy pResourceProxy ) {
        super(pResourceProxy);
        this.resourceProxy = pResourceProxy;

        measurePaint.setAntiAlias(true);
        measurePaint.setColor(Color.DKGRAY);
        measurePaint.setStrokeWidth(3f);
        measurePaint.setStyle(Paint.Style.STROKE);

        Resources resources = ctx.getResources();
        float textSizeMedium = resources.getDimension(R.dimen.text_normal);
        measureTextPaint.setAntiAlias(true);
        measureTextPaint.setTextSize(textSizeMedium);
        measureTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        distanceString = resources.getString(R.string.distance);
    }

    public void setDoDraw( boolean doDraw ) {
        this.doDraw = doDraw;
        Logger.d(this, "Will draw: " + doDraw);
    }

    protected void draw( final Canvas canvas, final MapView mapsView, final boolean shadow ) {
        if (!isOn || shadow || !doDraw)
            return;

        Logger.d(this, "Drawing measure path....");
        canvas.drawPath(measurePath, measurePaint);

        Projection pj = mapsView.getProjection();
        GeoPoint mapCenter = mapsView.getMapCenter();
        Point center = pj.toMapPixels(mapCenter, null);

        int upper = 25;
        int delta = 5;
        Rect rect = new Rect();
        measureTextPaint.getTextBounds(distanceString, 0, distanceString.length(), rect);
        int textWidth = rect.width();
        int textHeight = rect.height();
        int x = center.x - textWidth / 2;
        canvas.drawText(distanceString, x, upper, measureTextPaint);

        String distanceText = distanceText((int) measuredDistance, imperial, nautical);
        measureTextPaint.getTextBounds(distanceText, 0, distanceText.length(), rect);
        textWidth = rect.width();
        x = center.x - textWidth / 2;
        canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);

    }

    public void setMeasureMode( boolean isOn ) {
        this.isOn = isOn;
    }

    public boolean isInMeasureMode() {
        return isOn;
    }

    @Override
    public boolean onTouchEvent( MotionEvent event, MapView mapView ) {
        if (!isOn) {
            return super.onTouchEvent(event, mapView);
        }
        Projection pj = mapView.getProjection();
        // handle drawing
        currentX = (int) round(event.getX());
        currentY = (int) round(event.getY());
        Logger.d(this, "point: " + currentX + "/" + currentY);

        if (lastX == -1 || lastY == -1) {
            // lose the first drag and set the delta
            lastX = currentX;
            lastY = currentY;
            return true;
        }

        int action = event.getAction();
        switch( action ) {
        case MotionEvent.ACTION_DOWN:
            Logger.d(this, "First point....");
            measurePath.reset();
            measurePath.moveTo(currentX, currentY);
            break;
        case MotionEvent.ACTION_MOVE:
            int dx = currentX - lastX;
            int dy = currentY - lastY;
            if (abs(dx) < 2 || abs(dy) < 2) {
                lastX = currentX;
                lastY = currentY;
                return true;
            }

            Logger.d(this, "Recording points....");
            measurePath.lineTo(currentX, currentY);

            // the measurement
            GeoPoint currentGeoPoint = pj.fromPixels(currentX, currentY);
            GeoPoint previousGeoPoint = pj.fromPixels(lastX, lastY);
            float distanceTo = currentGeoPoint.distanceTo(previousGeoPoint);
            if (Float.isNaN(measuredDistance)) {
                measuredDistance = 0;
            }
            lastX = currentX;
            lastY = currentY;
            measuredDistance = measuredDistance + distanceTo;
            mapView.invalidate();
            break;
        case MotionEvent.ACTION_UP:
            measuredDistance = Float.NaN;
            break;
        }
        return true;
    }

    private String distanceText( final int meters, final boolean imperial, final boolean nautical ) {
        if (imperial) {
            if (meters >= METERS_PER_STATUTE_MILE * 5) {
                return resourceProxy.getString(ResourceProxy.string.format_distance_miles,
                        (int) (meters / METERS_PER_STATUTE_MILE));

            } else if (meters >= METERS_PER_STATUTE_MILE / 5) {
                return resourceProxy.getString(ResourceProxy.string.format_distance_miles,
                        ((int) (meters / (METERS_PER_STATUTE_MILE / 10.0))) / 10.0);
            } else {
                return resourceProxy.getString(ResourceProxy.string.format_distance_feet, (int) (meters * FEET_PER_METER));
            }
        } else if (nautical) {
            if (meters >= METERS_PER_NAUTICAL_MILE * 5) {
                return resourceProxy.getString(ResourceProxy.string.format_distance_nautical_miles,
                        ((int) (meters / METERS_PER_NAUTICAL_MILE)));
            } else if (meters >= METERS_PER_NAUTICAL_MILE / 5) {
                return resourceProxy.getString(ResourceProxy.string.format_distance_nautical_miles,
                        (((int) (meters / (METERS_PER_NAUTICAL_MILE / 10.0))) / 10.0));
            } else {
                return resourceProxy.getString(ResourceProxy.string.format_distance_feet, ((int) (meters * FEET_PER_METER)));
            }
        } else {
            if (meters >= 1000 * 5) {
                return resourceProxy.getString(ResourceProxy.string.format_distance_kilometers, (meters / 1000));
            } else if (meters >= 1000 / 5) {
                return resourceProxy.getString(ResourceProxy.string.format_distance_kilometers, (int) (meters / 100.0) / 10.0);
            } else {
                return resourceProxy.getString(ResourceProxy.string.format_distance_meters, meters);
            }
        }
    }
}
