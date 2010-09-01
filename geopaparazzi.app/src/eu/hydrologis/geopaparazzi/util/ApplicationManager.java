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
package eu.hydrologis.geopaparazzi.util;

import static eu.hydrologis.geopaparazzi.util.Constants.GPSLOGGINGINTERVALKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.GPS_LOGGING_INTERVAL;
import static eu.hydrologis.geopaparazzi.util.Constants.OSMFOLDERKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_GEOPAPARAZZI;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_GEOPAPARAZZIDATA;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_KMLEXPORT;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_OSMCACHE;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_PICTURES;
import static eu.hydrologis.geopaparazzi.util.Constants.SENSORTHRESHOLD;
import static java.lang.Math.toDegrees;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.GeoPaparazziActivity;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.gps.GpsLogger;
import eu.hydrologis.geopaparazzi.osm.OsmView;

/**
 * Singleton that takes care of all the sensors and gps and loggings.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ApplicationManager implements SensorEventListener, LocationListener {

    private static final String LOGTAG = "APPLICATIONMANAGER";

    private LocationManager locationManager;
    private SensorManager sensorManager;

    private int accuracy;

    /**
     * The last taken gps location.
     */
    private GpsLocation gpsLoc = null;

    /**
     * The previous gps location or null if no gps location was taken yet.
     * 
     * <p>This changes with every {@link #onLocationChanged(Location)}.</p>
     */
    private Location previousLoc = null;

    /**
     * The object responsible to log traces into the database. 
     */
    private GpsLogger gpsLogger;

    private double azimuth = -1;
    private double pitch = -1;
    private double roll = -1;

    private static GeoPaparazziActivity mainActivity;
    private static ApplicationManager deviceManager = null;

    private File databaseFile;
    private File geoPaparazziDir;
    private File picturesDir;
    private File osmCacheDir;
    private File kmlExportDir;

    private boolean doShowTileFrames = false;

    private List<ApplicationManagerListener> listeners = new ArrayList<ApplicationManagerListener>();

    private OsmView osmView;

    private float[] mags;

    private boolean isReady;

    private float[] accels;

    // private float[] orients;

    private final static int matrix_size = 16;
    private final float[] RM = new float[matrix_size];
    private final float[] outR = new float[matrix_size];
    private final float[] I = new float[matrix_size];
    private final float[] values = new float[3];

    private ConnectivityManager connectivityManager;

    public void addListener( ApplicationManagerListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener( ApplicationManagerListener listener ) {
        listeners.remove(listener);
    }

    /**
     * Gets the apps manager singleton. 
     * 
     * @param mainActivity The reference to the main activity. Can be set only once.
     * @return the {@link ApplicationManager} singleton.
     */
    public synchronized static ApplicationManager getInstance( GeoPaparazziActivity mainActivity ) {
        if (mainActivity != null && ApplicationManager.mainActivity == null) {
            setMainActivity(mainActivity);
        }
        if (deviceManager == null) {
            deviceManager = new ApplicationManager();
        }
        return deviceManager;
    }

    /**
     * Utility to get back the {@link ApplicationManager} with default params.
     * 
     * @return the {@link ApplicationManager} singleton.
     */
    public static ApplicationManager getInstance() {
        return getInstance(null);
    }

    private ApplicationManager() {
        /*
         * take care to create all the folders needed
         * 
         * The default structure is:
         * 
         * geopaparazzi 
         *    | 
         *    |--- pictures 
         *    |       |-- IMG_***.jpg 
         *    |       `-- etc 
         *    |--- geopaparazzi.db 
         *    |--- osmtags.properties 
         *    |        
         *    |--- osmcache 
         *    |    `-- zoomlevel 
         *    |          `-- xtile 
         *    |               `-- ytile.png
         *    `--- export
         */
        String state = Environment.getExternalStorageState();
        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (mExternalStorageAvailable && mExternalStorageWriteable) {
            File sdcardDir = Environment.getExternalStorageDirectory();// new
            // File(Constants.PATH_SDCARD);
            geoPaparazziDir = new File(sdcardDir.getAbsolutePath() + PATH_GEOPAPARAZZI);
            String geoPaparazziDirPath = geoPaparazziDir.getAbsolutePath();

            if (!geoPaparazziDir.exists())
                if (!geoPaparazziDir.mkdir())
                    alert(MessageFormat.format(mainActivity.getResources().getString(R.string.cantcreate_sdcard),
                            geoPaparazziDirPath));
            databaseFile = new File(geoPaparazziDirPath, DatabaseManager.DATABASE_NAME);
            picturesDir = new File(geoPaparazziDirPath + PATH_PICTURES);
            if (!picturesDir.exists())
                if (!picturesDir.mkdir())
                    alert(MessageFormat.format(mainActivity.getResources().getString(R.string.cantcreate_sdcard),
                            picturesDir.getAbsolutePath()));

            // notesDir = new File(geoPaparazziDirPath + PATH_NOTES);
            // if (!notesDir.exists())
            // if (!notesDir.mkdir())
            // alert(MessageFormat.format(mainActivity.getResources().getString(R.string.cantcreate_sdcard),
            // notesDir.getAbsolutePath()));
            //
            // gpslogDir = new File(geoPaparazziDirPath + PATH_GPSLOGS);
            // if (!gpslogDir.exists())
            // if (!gpslogDir.mkdir())
            // alert(MessageFormat.format(mainActivity.getResources().getString(R.string.cantcreate_sdcard),
            // gpslogDir.getAbsolutePath()));
            File geoPaparazziDataDir = new File(sdcardDir.getAbsolutePath() + PATH_GEOPAPARAZZIDATA);
            String geoPaparazziDataDirPath = geoPaparazziDataDir.getAbsolutePath();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            osmCacheDir = new File(preferences.getString(OSMFOLDERKEY, geoPaparazziDataDirPath + PATH_OSMCACHE));
            Log.i(LOGTAG, "OSMPATH:" + osmCacheDir.getAbsolutePath());
            if (!osmCacheDir.exists())
                if (!osmCacheDir.mkdirs()) {
                    String msg = MessageFormat.format(mainActivity.getResources().getString(R.string.cantcreate_sdcard),
                            osmCacheDir.getAbsolutePath());
                    alert(msg);
                }
            kmlExportDir = new File(geoPaparazziDirPath + PATH_KMLEXPORT);
            if (!kmlExportDir.exists())
                if (!kmlExportDir.mkdir())
                    alert(MessageFormat.format(mainActivity.getResources().getString(R.string.cantcreate_sdcard),
                            kmlExportDir.getAbsolutePath()));

        } else {
            alertDialog(mainActivity.getResources().getString(R.string.sdcard_notexist));
        }

    }

    /**
     * Setter for the main activity, used for certain tasks.
     * 
     * @param mainActivity the activity needed to retrieve the managers.
     */
    private static void setMainActivity( GeoPaparazziActivity mainActivity ) {
        ApplicationManager.mainActivity = mainActivity;
    }

    public Resources getResource() {
        Resources resources = mainActivity.getResources();
        return resources;
    }

    /**
     * Get the location and sensor managers.
     */
    public void activateManagers() {
        locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) mainActivity.getSystemService(Context.SENSOR_SERVICE);
        connectivityManager = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isInternetOn() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    // /**
    // * Trigger to send to all listeners the last known location of the gps.
    // */
    // public void triggerGetLastKnowLocationBroadcast() {
    // Location lastKnownLocation =
    // locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    //
    // for( ApplicationManagerListener listener : listeners ) {
    // listener.onLocationChanged(new GpsLocation(lastKnownLocation));
    // }
    // }

    /**
     * Stops listening to all the devices.
     */
    public void stopListening() {
        locationManager.removeUpdates(this);
        sensorManager.unregisterListener(this);
    }

    /**
     * Starts listening to all the devices.
     */
    public void startListening() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String intervalStr = preferences.getString(GPSLOGGINGINTERVALKEY, String.valueOf(GPS_LOGGING_INTERVAL));
        int waitForMillis = (int) (Long.parseLong(intervalStr) * 1000);
        Log.d(LOGTAG, "LOG INTERVAL MILLIS: " + waitForMillis);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, waitForMillis,
        // TIMETHRESHOLD,
                SENSORTHRESHOLD, this);
        // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
        // TIMETHRESHOLD, SENSORTHRESHOLD, this);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public boolean isGpsEnabled() {
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void checkGps() {
        if (!isGpsEnabled()) {
            String prompt = mainActivity.getResources().getString(R.string.prompt_gpsenable);
            String ok = mainActivity.getResources().getString(R.string.ok);
            String cancel = mainActivity.getResources().getString(R.string.cancel);
            AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
            builder.setMessage(prompt).setCancelable(false).setPositiveButton(ok, new DialogInterface.OnClickListener(){
                public void onClick( DialogInterface dialog, int id ) {
                    showGpsOptions();
                }
            });
            builder.setNegativeButton(cancel, new DialogInterface.OnClickListener(){
                public void onClick( DialogInterface dialog, int id ) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void showGpsOptions() {
        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        mainActivity.startActivity(gpsOptionsIntent);
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
            SensorManager.remapCoordinateSystem(RM, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, values);

            azimuth = toDegrees(values[0]);
            pitch = toDegrees(values[1]);
            roll = toDegrees(values[2]);

            azimuth = azimuth > 0 ? azimuth : (360f + azimuth);

            // Log.v(LOGTAG, azimuth + "\t\t" + pitch + "\t\t" + roll);
            mainActivity.updateFromDeviceManager();
        }

    }

    public void onLocationChanged( Location loc ) {
        gpsLoc = new GpsLocation(loc);
        if (previousLoc == null) {
            previousLoc = loc;
        }

        mainActivity.updateFromDeviceManager();

        Log.d(LOGTAG, "Position update: " + gpsLoc.getLongitude() + "/" + gpsLoc.getLatitude()); //$NON-NLS-1$ //$NON-NLS-2$

        gpsLoc.setPreviousLoc(previousLoc);

        for( ApplicationManagerListener listener : listeners ) {
            listener.onLocationChanged(gpsLoc);
        }

        previousLoc = loc;
    }

    public void onProviderDisabled( String provider ) {
    }

    public void onProviderEnabled( String provider ) {
    }

    public void onStatusChanged( String provider, int status, Bundle extras ) {
    }

    public int getAccuracy() {
        return accuracy;
    }

    public GpsLocation getLoc() {
        return gpsLoc;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getPitch() {
        return pitch;
    }

    public double getRoll() {
        return roll;
    }

    public File getGeoPaparazziDir() {
        return geoPaparazziDir;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    // public File getGpslogDir() {
    // return gpslogDir;
    // }

    public File getOsmCacheDir() {
        return osmCacheDir;
    }

    public File getKmlExportDir() {
        return kmlExportDir;
    }

    public File getPicturesDir() {
        return picturesDir;
    }

    // public File getNotesDir() {
    // return notesDir;
    // }

    private void alert( String msg ) {
        Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();
    }

    public void alertDialog( String msg ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        String ok = mainActivity.getResources().getString(R.string.ok);
        builder.setMessage(msg).setCancelable(false).setPositiveButton(ok, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int id ) {
                showGpsOptions();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean doShowTilesFrames() {
        return doShowTileFrames;
    }

    public boolean isGpsLogging() {
        if (gpsLogger == null) {
            return false;
        }
        return gpsLogger.isLogging();
    }

    public void setOsmView( OsmView osmView ) {
        this.osmView = osmView;

        if (gpsLoc != null) {
            osmView.onLocationChanged(gpsLoc);
        }
    }

    public OsmView getOsmView() {
        return osmView;
    }

    public List<Float> getLast100Elevations() {
        return gpsLogger.getLast100Elevations();
    }

    public int getCurrentRunningGpsLogPointsNum() {
        return gpsLogger.getCurrentPointsNum();
    }

    public int getCurrentRunningGpsLogDistance() {
        return gpsLogger.getCurrentDistance();
    }

    public void doLogGps( boolean doLogGps ) {
        if (gpsLogger == null) {
            gpsLogger = new GpsLogger(mainActivity);
        }
        if (doLogGps) {
            addListener(gpsLogger);
            gpsLogger.startLogging();
        } else {
            gpsLogger.stopLogging();
            removeListener(gpsLogger);
        }
    }

    /**
     * Gets the list of pictures.
     * 
     * @return the list of pictures.
     */
    public List<Picture> getPictures() {
        List<Picture> picturesList = new ArrayList<Picture>();
        File picturesDir = getPicturesDir();
        File[] pictures = picturesDir.listFiles();
        for( int i = 0; i < pictures.length; i++ ) {
            File picture = pictures[i];
            String name = picture.getName();

            if (name.endsWith("jpg") || name.endsWith("JPG")) {
                String nameWithoutExtention = FileUtils.getNameWithoutExtention(picture);
                String propsName = nameWithoutExtention + ".properties";
                File propsFile = new File(picture.getParentFile(), propsName);
                if (propsFile.exists()) {
                    try {
                        Properties p = new Properties();
                        p.load(new FileInputStream(propsFile));

                        double lat = Double.parseDouble(p.getProperty("latitude"));
                        double lon = Double.parseDouble(p.getProperty("longitude"));

                        Picture pic = new Picture(lon, lat, picture.getAbsolutePath());
                        picturesList.add(pic);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        return picturesList;
    }

    public static void openDialog( int messageId, Context activity ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(messageId).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int id ) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
