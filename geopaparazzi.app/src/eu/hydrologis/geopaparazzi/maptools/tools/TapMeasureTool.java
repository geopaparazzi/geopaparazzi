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
package eu.hydrologis.geopaparazzi.maptools.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.MotionEvent;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maptools.core.MapTool;
import eu.hydrologis.geopaparazzi.util.Constants;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to measure by means of drawing on the map.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TapMeasureTool extends MapTool {
    private final Paint measurePaint = new Paint();
    private final Paint measureTextPaint = new Paint();
    private Path measurePath = new Path();

    private float measuredDistance = Float.NaN;
    private String distanceString;

    private float currentX;
    private float currentY;
    private float lastX = -1;
    private float lastY = -1;

    private final Point tmpP = new Point();

    private final Rect rect = new Rect();

    private StringBuilder textBuilder = new StringBuilder();
    private boolean doImperial = false;

    /**
     * Constructor.
     * 
     * @param mapView the mapview reference.
     */
    public TapMeasureTool( MapView mapView ) {
        super(mapView);

        Context context = GeopaparazziApplication.getInstance().getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        doImperial = preferences.getBoolean(Constants.PREFS_KEY_IMPERIAL, false);

        measurePaint.setAntiAlias(true);
        measurePaint.setColor(Color.DKGRAY);
        measurePaint.setStrokeWidth(3f);
        measurePaint.setStyle(Paint.Style.STROKE);

        measureTextPaint.setAntiAlias(true);
        int pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
        measureTextPaint.setTextSize(pixel);
        measureTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

        distanceString = context.getString(R.string.distance);
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw( Canvas canvas ) {
        int cWidth = canvas.getWidth();
        // RectF retfF = new RectF();
        // measurePath.computeBounds(retfF, true);
        // GPLog.androidLog(-1, "DRAWINFOLINE: " + retfF);
        canvas.drawPath(measurePath, measurePaint);
        int upper = 70;
        int delta = 5;
        measureTextPaint.getTextBounds(distanceString, 0, distanceString.length(), rect);
        int textWidth = rect.width();
        int textHeight = rect.height();
        int x = cWidth / 2 - textWidth / 2;
        canvas.drawText(distanceString, x, upper, measureTextPaint);
        textBuilder.setLength(0);
        if (doImperial) {
            double distanceInFeet = Utilities.toFeet(measuredDistance);
            textBuilder.append(String.valueOf((int) distanceInFeet));
            textBuilder.append(" ft"); //$NON-NLS-1$
        } else {
            textBuilder.append(String.valueOf((int) measuredDistance));
            textBuilder.append(" m"); //$NON-NLS-1$
        }
        String distanceText = textBuilder.toString();
        measureTextPaint.getTextBounds(distanceText, 0, distanceText.length(), rect);
        textWidth = rect.width();
        x = cWidth / 2 - textWidth / 2;
        canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "Drawing measure path text: " + upper); //$NON-NLS-1$

    }

    public boolean onToolTouchEvent( MotionEvent event ) {
        if (mapView == null || mapView.isClickable()) {
            return false;
        }

        Projection pj = mapView.getProjection();
        // handle drawing
        currentX = event.getX();
        currentY = event.getY();

        tmpP.set(round(currentX), round(currentY));

        int action = event.getAction();
        switch( action ) {
        case MotionEvent.ACTION_DOWN:
            measuredDistance = 0;
            measurePath.reset();
            GeoPoint firstGeoPoint = pj.fromPixels(round(currentX), round(currentY));
            pj.toPixels(firstGeoPoint, tmpP);
            measurePath.moveTo(tmpP.x, tmpP.y);

            lastX = currentX;
            lastY = currentY;

            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry(this, "TOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
            break;
        case MotionEvent.ACTION_MOVE:
            float dx = currentX - lastX;
            float dy = currentY - lastY;
            if (abs(dx) < 1 && abs(dy) < 1) {
                lastX = currentX;
                lastY = currentY;
                return true;
            }

            GeoPoint currentGeoPoint = pj.fromPixels(round(currentX), round(currentY));
            pj.toPixels(currentGeoPoint, tmpP);
            measurePath.lineTo(tmpP.x, tmpP.y);
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry(this, "DRAG: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$ //$NON-NLS-2$
            // the measurement
            GeoPoint previousGeoPoint = pj.fromPixels(round(lastX), round(lastY));

            Location l1 = new Location("gps"); //$NON-NLS-1$
            l1.setLatitude(previousGeoPoint.getLatitude());
            l1.setLongitude(previousGeoPoint.getLongitude());
            Location l2 = new Location("gps"); //$NON-NLS-1$
            l2.setLatitude(currentGeoPoint.getLatitude());
            l2.setLongitude(currentGeoPoint.getLongitude());

            float distanceTo = l1.distanceTo(l2);
            lastX = currentX;
            lastY = currentY;
            measuredDistance = measuredDistance + distanceTo;
            EditManager.INSTANCE.invalidateEditingView();
            break;
        case MotionEvent.ACTION_UP:
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry(this, "UNTOUCH: " + tmpP.x + "/" + tmpP.y); //$NON-NLS-1$//$NON-NLS-2$
            break;
        }

        return true;
    }

    public void disable() {
        if (mapView != null) {
            mapView.setClickable(true);
            mapView = null;
        }
        measuredDistance = 0;
        measurePath = null;
    }

}
