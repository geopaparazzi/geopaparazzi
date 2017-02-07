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

package eu.geopaparazzi.core.maptools.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;

import org.mapsforge.android.maps.MapView;

import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.core.maptools.MapTool;

import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * A tool to measure by means of drawing on the map.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ViewingConeTool extends MapTool implements SensorEventListener {
    private final Paint conePaint = new Paint();
    private OrientationSensor orientationSensor;
    private Path conePath = new Path();

    private SensorManager sensorManager;
    private Activity activity;

    private float[] mGravity = null;
    private float[] mGeomagnetic = null;
    private float azimuth360 = -1;

    /**
     * Constructor.
     *
     * @param mapView the mapview reference.
     */
    public ViewingConeTool(MapView mapView, Activity activity) {
        super(mapView);
        this.activity = activity;


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        conePaint.setAntiAlias(true);
        conePaint.setColor(Color.RED);
        conePaint.setStyle(Paint.Style.FILL);
        conePaint.setAlpha(128);


    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(false);

        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
//        orientationSensor = new OrientationSensor(sensorManager, null);
//        orientationSensor.register(activity, SensorManager.SENSOR_DELAY_NORMAL);
//
//        Sensor sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
//        sensorManager.registerListener(this, sensorGravity, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensorAcc, SensorManager.SENSOR_DELAY_NORMAL);
        Sensor sensorMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, sensorMag, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onToolDraw(Canvas canvas) {

        double delta = 5;
//        double azimuth = orientationSensor.getAzimuthDegrees();

        int cWidth = canvas.getWidth();
        int cHeight = canvas.getHeight();
        int halfX = cWidth / 2;
        int halfY = cHeight / 2;
        conePath.reset();
        conePath.moveTo(halfX, halfY);


        double az1 = azimuth360 - delta;
        if (az1 < 0) az1 = 360 + az1;
        double az2 = azimuth360 + delta;
        if (az2 > 360) az2 = az2 - 360;

//        GPLog.addLogEntry(this, "1: " + halfX + "/" + halfY);
//        GPLog.addLogEntry(this, "ang: " + az1 + "/" + az2);

        moveToXYByAngle(cWidth, cHeight, halfX, halfY, az1);
        moveToXYByAngle(cWidth, cHeight, halfX, halfY, az2);
        conePath.lineTo(halfX, halfY);

        canvas.drawPath(conePath, conePaint);
    }

    private void moveToXYByAngle(int cWidth, int cHeight, int halfX, int halfY, double az) {
        float x = 0;
        float y = 0;
        if (az > 0 && az < 90) {
            x = halfX + (float) (halfY * Math.tan(Math.toRadians(az)));
            y = 0;
            if (x > cWidth) {
                y = halfY * (x - halfX) / x;
                x = cWidth;
            }
        } else if (az > 90 && az < 180) {
            az = az - 90;
            x = halfX + (float) (halfY / Math.tan(Math.toRadians(az)));
            y = cHeight;

            if (x > cWidth) {
                y = cHeight - halfY * (x - halfX) / x;
                x = cWidth;
            }
        } else if (az > 180 && az < 270) {
            az = az - 180;
            x = halfX - (float) (halfY * Math.tan(Math.toRadians(az)));
            y = cHeight;
            if (x < 0) {
                y = cHeight - halfY * (-x) / (-x + halfX);
                x = 0;
            }
        } else if (az > 270 && az < 360) {
            az = az - 270;
            x = halfX - (float) (halfY / Math.tan(Math.toRadians(az)));
            y = 0;
            if (x < 0) {
                y = halfY * (-x) / (-x + halfX);
                x = 0;
            }
        } else if (az == 90) {
            x = cWidth;
            y = halfY;
        } else if (az == 180) {
            x = halfX;
            y = cHeight;
        } else if (az == 270) {
            x = 0;
            y = halfY;
        } else if (az == 0) {
            x = halfX;
            y = 0;
        }
        conePath.lineTo(x, y);
//        GPLog.addLogEntry(this, "2: " + x + "/" + y);
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
        activity = null;
    }

    @Override
    public void onViewChanged() {
        // ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimuth360, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azRad = orientation[0]; // az between -PI and PI
                if (azRad < 0) {
                    azRad = (float) (2 * Math.PI + azRad);
                }

                azimuth360 = (float) Math.toDegrees(azRad);
                Log.i("AZIM", "AZ: " + azimuth360);
            }
        }


        EditManager.INSTANCE.invalidateEditingView();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
