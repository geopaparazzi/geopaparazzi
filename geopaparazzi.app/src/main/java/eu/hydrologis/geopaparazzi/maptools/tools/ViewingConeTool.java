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

package eu.hydrologis.geopaparazzi.maptools.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.MotionEvent;

import org.mapsforge.android.maps.MapView;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.util.MercatorUtils;
import eu.hydrologis.geopaparazzi.maptools.MapTool;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to measure by means of drawing on the map.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ViewingConeTool extends MapTool implements SensorEventListener {
    private final Paint conePaint = new Paint();
    private final OrientationSensor orientationSensor;
    private Path conePath = new Path();

    private float lastX = -1;
    private float lastY = -1;

    private final Point tmpP = new Point();

    private final Rect rect = new Rect();
    private final SensorManager sensorManager;


    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     */
    public ViewingConeTool(MapView mapView, Activity activity) {
        super(mapView);

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = new OrientationSensor(sensorManager, null);
        orientationSensor.register(activity, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        sensorManager.registerListener(this, sensorGravity, SensorManager.SENSOR_DELAY_NORMAL);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        conePaint.setAntiAlias(true);
        conePaint.setColor(Color.RED);
        conePaint.setStyle(Paint.Style.FILL);
        conePaint.setAlpha(128);


    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);
    }

    public void onToolDraw(Canvas canvas) {

        double delta = 10;
        double azimuth = orientationSensor.getAzimuthDegrees();

        int cWidth = canvas.getWidth();
        int cHeight = canvas.getHeight();
        int halfY = cHeight / 2;
        conePath.moveTo(cWidth / 2, halfY);

        double az1 = azimuth - delta;

        double x1;
        double y1;
        if (az1 > 0 && az1 <= 90) {
            x1 = halfY * Math.atan(Math.toRadians(az1));
            y1 = 0;
        } else if (az1 > 90 && az1 <= 180) {
            az1 = az1-90;

            x1 = halfY * Math.atan(Math.toRadians(az1));
            y1 = 0;
        }

        // RectF retfF = new RectF();
        // conePath.computeBounds(retfF, true);
        // GPLog.androidLog(-1, "DRAWINFOLINE: " + retfF);
        canvas.drawPath(conePath, conePaint);
//        int upper = 70;
//        int delta = 5;
//        measureTextPaint.getTextBounds(distanceString, 0, distanceString.length(), rect);
//        int textWidth = rect.width();
//        int textHeight = rect.height();
//        int x = cWidth / 2 - textWidth / 2;
//        canvas.drawText(distanceString, x, upper, measureTextPaint);
//        textBuilder.setLength(0);
//        if (doImperial) {
//            double distanceInFeet = MercatorUtils.toFeet(measuredDistance);
//            textBuilder.append(String.valueOf((int) distanceInFeet));
//            textBuilder.append(" ft"); //$NON-NLS-1$
//        } else {
//            textBuilder.append(String.valueOf((int) measuredDistance));
//            textBuilder.append(" m"); //$NON-NLS-1$
//        }
//        String distanceText = textBuilder.toString();
//        measureTextPaint.getTextBounds(distanceText, 0, distanceText.length(), rect);
//        textWidth = rect.width();
//        x = cWidth / 2 - textWidth / 2;
//        canvas.drawText(distanceText, x, upper + delta + textHeight, measureTextPaint);
//        if (GPLog.LOG_HEAVY)
//            GPLog.addLogEntry(this, "Drawing measure path text: " + upper); //$NON-NLS-1$

    }

    public boolean onToolTouchEvent(MotionEvent event) {
        return false;
    }

    public void disable() {
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
        if (orientationSensor != null)
            orientationSensor.unregister();
        if (mapView != null) {
            mapView.setClickable(true);
            mapView = null;
        }
        conePath = null;
    }

    @Override
    public void onViewChanged() {
        // ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        EditManager.INSTANCE.invalidateEditingView();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
