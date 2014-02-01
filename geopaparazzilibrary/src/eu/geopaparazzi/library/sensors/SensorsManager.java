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
package eu.geopaparazzi.library.sensors;

import static java.lang.Math.toDegrees;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Singleton that takes care of sensor matters.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SensorsManager implements SensorEventListener {

    private static SensorsManager sensorManager;

    private SensorManager sensorManagerInternal;
    private ConnectivityManager connectivityManager;

    private int accuracy;

    private double normalAzimuth = -1;
    // private double normalPitch = -1;
    // private double normalRoll = -1;
    private double pictureAzimuth = -1;
    // private double picturePitch = -1;
    // private double pictureRoll = -1;
    private float[] mags;

    private boolean isReady;

    private float[] accels;

    private final static int matrix_size = 16;
    private final float[] RM = new float[matrix_size];
    private final float[] outR = new float[matrix_size];
    private final float[] I = new float[matrix_size];
    private final float[] values = new float[3];

    private final Context context;

    private List<SensorsManagerListener> listeners = new ArrayList<SensorsManagerListener>();

    private SensorsManager( Context context ) {
        this.context = context;

    }

    /**
     * @param context  the context to use.
     * @return the singleton instance.
     */
    public synchronized static SensorsManager getInstance( Context context ) {
        if (sensorManager == null) {
            sensorManager = new SensorsManager(context);
            sensorManager.activateSensorManagers();
            sensorManager.startSensorListening();
        }
        return sensorManager;
    }

    /**
     * Add a listener to gps.
     * 
     * @param listener the listener to add.
     */
    public void addListener( SensorsManagerListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener to gps.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( SensorsManagerListener listener ) {
        listeners.remove(listener);
    }

    /**
     * Get the location and sensor managers.
     */
    public void activateSensorManagers() {
        sensorManagerInternal = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * @return <code>true</code> if internet is on.
     */
    public boolean isInternetOn() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    /**
     * @return accuracy.
     */
    public int getAccuracy() {
        return accuracy;
    }

    /**
     * @return normal azimuth.
     */
    public double getNormalAzimuth() {
        return normalAzimuth;
    }

    /**
     * @return picture azimuth.
     */
    public double getPictureAzimuth() {
        return pictureAzimuth;
    }

    /**
     * Stops listening to all the devices.
     */
    public void stopSensorListening() {
        if (sensorManagerInternal != null && sensorManager != null)
            sensorManagerInternal.unregisterListener(sensorManager);
    }

    /**
     * Starts listening to all the devices.
     */
    public void startSensorListening() {

        sensorManagerInternal.unregisterListener(sensorManager);
        sensorManagerInternal.registerListener(sensorManager, sensorManagerInternal.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManagerInternal.registerListener(sensorManager, sensorManagerInternal.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManagerInternal.registerListener(sensorManager, sensorManagerInternal.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onAccuracyChanged( Sensor sensor, int accuracy ) {
        int type = sensor.getType();
        if (type == SensorManager.SENSOR_ORIENTATION) {
            this.accuracy = accuracy;
        }
    }

    public void onSensorChanged( SensorEvent event ) {
        Sensor sensor = event.sensor;
        int type = sensor.getType();

        switch( type ) {
        case Sensor.TYPE_MAGNETIC_FIELD:
            mags = event.values.clone();
            isReady = true;
            break;
        case Sensor.TYPE_ACCELEROMETER:
            accels = event.values.clone();
            break;
        // case Sensor.TYPE_ORIENTATION:
        // orients = event.values.clone();
        // break;
        }

        if (mags != null && accels != null && isReady) {
            isReady = false;

            SensorManager.getRotationMatrix(RM, I, accels, mags);
            SensorManager.remapCoordinateSystem(RM, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            SensorManager.getOrientation(outR, values);
            normalAzimuth = toDegrees(values[0]);
            // normalPitch = toDegrees(values[1]);
            // normalRoll = toDegrees(values[2]);
            // int orientation = getContext().getResources().getConfiguration().orientation;
            // switch( orientation ) {
            // case Configuration.ORIENTATION_LANDSCAPE:
            // normalAzimuth = -1 * (normalAzimuth - 135);
            // case Configuration.ORIENTATION_PORTRAIT:
            // default:
            // break;
            // }
            // normalAzimuth = normalAzimuth > 0 ? normalAzimuth : (360f + normalAzimuth);
            // Logger.d(this, "NAZIMUTH = " + normalAzimuth);

            SensorManager.remapCoordinateSystem(RM, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, values);

            pictureAzimuth = toDegrees(values[0]);
            // picturePitch = toDegrees(values[1]);
            // pictureRoll = toDegrees(values[2]);
            pictureAzimuth = pictureAzimuth > 0 ? pictureAzimuth : (360f + pictureAzimuth);

            // Logger.d(sensorManager, "PAZIMUTH = " + pictureAzimuth);

            for( SensorsManagerListener listener : listeners ) {
                listener.onSensorChanged(normalAzimuth, pictureAzimuth);
            }
        }

    }

}
