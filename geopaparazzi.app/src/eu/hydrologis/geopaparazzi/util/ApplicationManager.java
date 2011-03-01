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

import static eu.hydrologis.geopaparazzi.util.Constants.*;
import static eu.hydrologis.geopaparazzi.util.Constants.GPSLOGGINGINTERVALKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.GPS_LOGGING_INTERVAL;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_GEOPAPARAZZI;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_KMLEXPORT;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_MAPSCACHE;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_MEDIA;
import static java.lang.Math.toDegrees;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
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
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.gps.GpsLogger;
import eu.hydrologis.geopaparazzi.maps.MapView;
import eu.hydrologis.geopaparazzi.util.debug.Debug;
import eu.hydrologis.geopaparazzi.util.debug.Logger;
import eu.hydrologis.geopaparazzi.util.debug.TestMock;

/**
 * Singleton that takes care of all the sensors and gps and loggings.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ApplicationManager implements SensorEventListener, LocationListener, Serializable {
    private static final long serialVersionUID = 1L;

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
    private static GpsLogger gpsLogger;

    private double normalAzimuth = -1;
    // private double normalPitch = -1;
    // private double normalRoll = -1;
    private double pictureAzimuth = -1;
    // private double picturePitch = -1;
    // private double pictureRoll = -1;

    private Context context;

    private File databaseFile;
    private File geoPaparazziDir;
    private File mediaDir;
    private File mapsCacheDir;
    private File kmlExportDir;

    private List<ApplicationManagerListener> listeners = new ArrayList<ApplicationManagerListener>();

    private MapView mapView;

    private float[] mags;

    private boolean isReady;

    private float[] accels;

    private final static int matrix_size = 16;
    private final float[] RM = new float[matrix_size];
    private final float[] outR = new float[matrix_size];
    private final float[] I = new float[matrix_size];
    private final float[] values = new float[3];

    private ConnectivityManager connectivityManager;

    private File debugLogFile;

    private static ApplicationManager applicationManager;

    /**
     * The getter for the {@link ApplicationManager} singleton.
     * 
     * <p>This is a singletone but might require to be recreated
     * in every moment of the application. This is due to the fact
     * that when the application looses focus (for example because of
     * an incoming call, and therefore at a random moment, if the memory 
     * is too low, the parent activity could have been killed by 
     * the system in background. In which case we need to recreate it.) 
     * 
     * @param context the context to refer to.
     * @param osmCachePath the patch to the osmCache.
     * @return
     */
    public static ApplicationManager getInstance( Context context ) {
        if (applicationManager == null && context != null) {
            applicationManager = new ApplicationManager(context);
            applicationManager.activateManagers();
            applicationManager.checkGps();
            applicationManager.startListening();
        } else if (applicationManager == null && context == null) {
            throw new RuntimeException("this should not happen!");
        }
        return applicationManager;
    }

    public static void resetManager() {
        applicationManager = null;
    }

    public Context getContext() {
        return context;
    }

    private ApplicationManager( Context context ) {
        this.context = context;

        /*
         * take care to create all the folders needed
         * 
         * The default structure is:
         * 
         * geopaparazzi 
         *    | 
         *    |--- media 
         *    |       |-- IMG_***.jpg 
         *    |       |-- AUDIO_***.3gp 
         *    |       `-- etc 
         *    |--- geopaparazzi.db 
         *    |--- tags.json 
         *    |        
         *    |--- debug.log 
         *    |        
         *    `--- export
         * geopaparazzimapscache 
         *    |--.nomedia
         *    `-- zoomlevel 
         *        `-- xtile 
         *            `-- ytile.png
         */

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String baseFolder = preferences.getString(BASEFOLDERKEY, "");
        geoPaparazziDir = new File(baseFolder);
        mapsCacheDir = new File(geoPaparazziDir.getParentFile() + PATH_MAPSCACHE);
        if (baseFolder == null || baseFolder.length() == 0 || !geoPaparazziDir.getParentFile().exists()
                || !geoPaparazziDir.getParentFile().canWrite()) {
            // the folder doesn't exist for some reason, fallback on default
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
                geoPaparazziDir = new File(sdcardDir.getAbsolutePath() + PATH_GEOPAPARAZZI);
                mapsCacheDir = new File(sdcardDir.getAbsolutePath() + PATH_MAPSCACHE);
            } else {
                alertDialog(context.getResources().getString(R.string.sdcard_notexist));
                return;
            }
        }

        String geoPaparazziDirPath = geoPaparazziDir.getAbsolutePath();
        if (!geoPaparazziDir.exists())
            if (!geoPaparazziDir.mkdir())
                alert(MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard), geoPaparazziDirPath));
        databaseFile = new File(geoPaparazziDirPath, DatabaseManager.DATABASE_NAME);
        mediaDir = new File(geoPaparazziDirPath + PATH_MEDIA);

        if (!mediaDir.exists())
            if (!mediaDir.mkdir())
                alert(MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                        mediaDir.getAbsolutePath()));
        debugLogFile = new File(geoPaparazziDirPath, "debug.log");

        Logger.i(LOGTAG, "MAPSCACHEPATH:" + mapsCacheDir.getAbsolutePath());
        if (!mapsCacheDir.exists())
            if (!mapsCacheDir.mkdirs()) {
                String msg = MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                        mapsCacheDir.getAbsolutePath());
                alert(msg);
            }
        File noMediaFile = new File(mapsCacheDir, ".nomedia");
        if (!noMediaFile.exists()) {
            try {
                noMediaFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        kmlExportDir = new File(geoPaparazziDirPath + PATH_KMLEXPORT);
        if (!kmlExportDir.exists())
            if (!kmlExportDir.mkdir())
                alert(MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                        kmlExportDir.getAbsolutePath()));

    }

    public void createResetFile() throws IOException {
        File resetFile = new File(geoPaparazziDir, "doReset");
        resetFile.createNewFile();
    }

    /**
     * Add a listener to sensors and gps.
     * 
     * @param listener the listener to add.
     */
    public void addListener( ApplicationManagerListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener to sensors and gps.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( ApplicationManagerListener listener ) {
        listeners.remove(listener);
    }

    /**
     * Remove the osm listener.
     * 
     * @see #removeCompassListener().
     */
    public void removeOsmListener() {
        for( ApplicationManagerListener l : listeners ) {
            if (l instanceof MapView) {
                listeners.remove(l);
                break;
            }
        }
    }

    public void clearListeners() {
        listeners.clear();
    }

    public Resources getResource() {
        Resources resources = context.getResources();
        return resources;
    }

    /**
     * Get the location and sensor managers.
     */
    public void activateManagers() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isInternetOn() {
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    /**
     * Stops listening to all the devices.
     */
    public void stopListening() {
        if (applicationManager != null) {
            if (locationManager != null)
                locationManager.removeUpdates(applicationManager);
            if (sensorManager != null)
                sensorManager.unregisterListener(applicationManager);
        }
        if (TestMock.isOn) {
            TestMock.stopMocking(locationManager);
        }
    }

    /**
     * Starts listening to all the devices.
     */
    public void startListening() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String intervalStr = preferences.getString(GPSLOGGINGINTERVALKEY, String.valueOf(GPS_LOGGING_INTERVAL));
        int waitForMillis = (int) (Long.parseLong(intervalStr) * 1000);
        Logger.d(this, "LOG INTERVAL MILLIS: " + waitForMillis);
        if (Debug.doMock) {
            Logger.d(this, "Using Mock locations");
            TestMock.startMocking(locationManager, applicationManager);
        } else {
            Logger.d(this, "Using GPS");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, waitForMillis, 0f, applicationManager);
        }

        // locationManager.addGpsStatusListener(applicationManager);

        sensorManager.unregisterListener(applicationManager);
        sensorManager.registerListener(applicationManager, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(applicationManager, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(applicationManager, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public boolean isGpsEnabled() {
        if (locationManager == null) {
            return false;
        }
        boolean gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // List<String> allProviders = locationManager.getAllProviders();
        // for( String string : allProviders ) {
        // Logger.i(this, "Loctaion Providers: " + string);
        // }
        Logger.i(this, "Gps is on: " + gpsIsEnabled);
        return gpsIsEnabled;
    }

    public void checkGps() {
        if (!isGpsEnabled()) {
            String prompt = context.getResources().getString(R.string.prompt_gpsenable);
            String ok = context.getResources().getString(R.string.ok);
            String cancel = context.getResources().getString(R.string.cancel);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
        context.startActivity(gpsOptionsIntent);
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

            // Log.v(LOGTAG, "PAZIMUTH = " + pictureAzimuth);

            for( ApplicationManagerListener listener : listeners ) {
                listener.onSensorChanged(normalAzimuth, pictureAzimuth);
            }
        }

    }
    public void onLocationChanged( Location loc ) {
        gpsLoc = new GpsLocation(loc);
        if (previousLoc == null) {
            previousLoc = loc;
        }

        Logger.d(LOGTAG, "Position update: " + gpsLoc.getLongitude() + "/" + gpsLoc.getLatitude() + "/" + gpsLoc.getAltitude()); //$NON-NLS-1$ //$NON-NLS-2$
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
        // String statusString;
        // switch( status ) {
        // case LocationProvider.OUT_OF_SERVICE:
        // if (gpsLoc == null || gpsLoc.getProvider().equals(provider)) {
        // statusString = "No Service";
        // gpsLoc = null;
        // }
        // break;
        // case LocationProvider.TEMPORARILY_UNAVAILABLE:
        // if (gpsLoc == null || gpsLoc.getProvider().equals(provider)) {
        // statusString = "no fix";
        // }
        // break;
        // case LocationProvider.AVAILABLE:
        // statusString = "fix";
        // break;
        // }
    }

    // TODO
    // public void onGpsStatusChanged( int event ) {
    // int timeToFirstFix = -1;
    // if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
    // GpsStatus status = locationManager.getGpsStatus(null);
    // Iterable<GpsSatellite> sats = status.getSatellites();
    // timeToFirstFix = status.getTimeToFirstFix();
    // int max = status.getMaxSatellites();
    // Iterator<GpsSatellite> iterator = sats.iterator();
    // int num = 0;
    // while( iterator.hasNext() ) {
    // num++;
    // }
    // for( ApplicationManagerListener listener : listeners ) {
    // listener.onSatellitesStatusChanged(num, max);
    // }
    // }
    // Logger.d(LOGTAG, "Gps status event: " + event);
    // Logger.d(LOGTAG, "Time to first fix: " + timeToFirstFix);
    // }

    public int getAccuracy() {
        return accuracy;
    }

    public GpsLocation getLoc() {
        return gpsLoc;
    }

    public double getNormalAzimuth() {
        return normalAzimuth;
    }

    public double getPictureAzimuth() {
        return pictureAzimuth;
    }

    // public double getPitch() {
    // return pitch;
    // }
    //
    // public double getRoll() {
    // return roll;
    // }

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
        return mapsCacheDir;
    }

    public File getKmlExportDir() {
        return kmlExportDir;
    }

    public File getDebugLogFile() {
        return debugLogFile;
    }

    public File getMediaDir() {
        return mediaDir;
    }

    // public File getNotesDir() {
    // return notesDir;
    // }

    private void alert( String msg ) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public void alertDialog( String msg ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String ok = context.getResources().getString(R.string.ok);
        builder.setMessage(msg).setCancelable(false).setPositiveButton(ok, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int id ) {
                showGpsOptions();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isGpsLogging() {
        if (gpsLogger == null) {
            return false;
        }
        return gpsLogger.isLogging();
    }

    public void setMapView( MapView mapView ) {
        this.mapView = mapView;

        if (gpsLoc != null) {
            mapView.onLocationChanged(gpsLoc);
        }
    }

    public MapView getMapView() {
        return mapView;
    }

    public List<Float> getLast100Elevations() {
        return gpsLogger.getLast100Elevations();
    }

    public int getCurrentRunningGpsLogPointsNum() {
        return gpsLogger.getCurrentPointsNum();
    }

    public long getCurrentRecordedLogId() {
        if (gpsLogger == null) {
            return -1l;
        }
        return gpsLogger.getCurrentRecordedLogId();
    }

    public int getCurrentRunningGpsLogDistance() {
        return gpsLogger.getCurrentDistance();
    }

    private void checkLoggerExists() {
        if (gpsLogger == null) {
            gpsLogger = new GpsLogger(context);
        }
    }

    /**
     * Start gps logging.
     * 
     * @param logName a name for the new gps log or <code>null</code>.
     */
    public void startLogging( String logName ) {
        checkLoggerExists();
        addListener(gpsLogger);
        gpsLogger.startLogging(logName);
    }

    /**
     * Stop gps logging.
     */
    public void stopLogging() {
        checkLoggerExists();
        gpsLogger.stopLogging();
        removeListener(gpsLogger);
    }

    /**
     * Gets the list of pictures.
     * 
     * @return the list of pictures.
     */
    public List<Picture> getPictures() {
        List<Picture> picturesList = new ArrayList<Picture>();
        File picturesDir = getMediaDir();
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
                        Logger.e(this, e.getLocalizedMessage(), e);
                        continue;
                    }
                }
            }
        }
        return picturesList;
    }

    public static void openDialog( int message, Context activity ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int id ) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static void openDialog( String message, Context activity ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int id ) {
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public ActionItem getNotesQuickAction( final QuickAction qa ) {
        ActionItem notesQuickaction = new ActionItem();
        notesQuickaction.setTitle("Geonote");
        notesQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_notes));
        notesQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                GpsLocation loc = applicationManager.getLoc();
                if (loc != null) {
                    Intent intent = new Intent(Constants.TAKE_NOTE);
                    context.startActivity(intent);
                } else {
                    openDialog(R.string.gpslogging_only, context);
                }
                qa.dismiss();
            }
        });
        return notesQuickaction;
    }
    public ActionItem getPicturesQuickAction( final QuickAction qa ) {
        ActionItem pictureQuickaction = new ActionItem();
        pictureQuickaction.setTitle("Photo");
        pictureQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_pictures));
        pictureQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                try {
                    Logger.d(this, "Asking location");
                    GpsLocation loc = applicationManager.getLoc();
                    if (loc != null) {
                        Logger.d(this, "Location != null");
                        Intent intent = new Intent(Constants.TAKE_PICTURE);
                        context.startActivity(intent);
                    } else {
                        Logger.d(this, "Location == null");
                        openDialog(R.string.gpslogging_only, context);
                    }
                    qa.dismiss();
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                }
            }
        });
        return pictureQuickaction;
    }
    private MediaRecorder audioRecorder;

    public ActionItem getAudioQuickAction( final QuickAction qa ) {
        ActionItem audioQuickaction = new ActionItem();
        audioQuickaction.setTitle("Audio");
        audioQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_audio));
        audioQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                try {
                    GpsLocation loc = applicationManager.getLoc();
                    if (loc != null) {
                        double lat = loc.getLatitude();
                        double lon = loc.getLongitude();
                        double altim = loc.getAltitude();
                        String latString = String.valueOf(lat);
                        String lonString = String.valueOf(lon);
                        String altimString = String.valueOf(altim);

                        if (audioRecorder == null) {
                            audioRecorder = new MediaRecorder();
                        }
                        File mediaDir = applicationManager.getMediaDir();
                        final String currentDatestring = Constants.TIMESTAMPFORMATTER.format(new Date());
                        String audioFilePathNoExtention = mediaDir.getAbsolutePath() + "/AUDIO_" + currentDatestring;

                        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        audioRecorder.setOutputFile(audioFilePathNoExtention + ".3gp");
                        audioRecorder.prepare();
                        audioRecorder.start();

                        // create props file
                        String propertiesFilePath = audioFilePathNoExtention + ".properties";
                        File propertiesFile = new File(propertiesFilePath);
                        BufferedWriter bW = null;
                        try {
                            bW = new BufferedWriter(new FileWriter(propertiesFile));
                            bW.write("latitude=");
                            bW.write(latString);
                            bW.write("\nlongitude=");
                            bW.write(lonString);
                            bW.write("\naltim=");
                            bW.write(altimString);
                            bW.write("\nutctimestamp=");
                            bW.write(currentDatestring);
                        } catch (IOException e1) {
                            Logger.e(this, e1.getLocalizedMessage(), e1);
                            throw new IOException(e1.getLocalizedMessage());
                        } finally {
                            bW.close();
                        }

                        new AlertDialog.Builder(context).setTitle(R.string.audio_recording)
                                .setIcon(android.R.drawable.ic_menu_info_details)
                                .setNegativeButton(R.string.audio_recording_stop, new DialogInterface.OnClickListener(){
                                    public void onClick( DialogInterface dialog, int whichButton ) {
                                        if (audioRecorder != null) {
                                            audioRecorder.stop();
                                            audioRecorder.release();
                                            audioRecorder = null;
                                        }
                                    }
                                }).show();

                    } else {
                        openDialog(R.string.gpslogging_only, context);
                    }
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                }
                qa.dismiss();
            }
        });
        return audioQuickaction;
    }

    public ActionItem getStartLogQuickAction( final ActionBar actionBar, final QuickAction qa ) {
        ActionItem startLogQuickaction = new ActionItem();
        startLogQuickaction.setTitle("Start Log");
        startLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_start_log));
        startLogQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                if (!applicationManager.isGpsLogging()) {
                    GpsLocation loc = applicationManager.getLoc();
                    if (loc != null) {
                        final String defaultLogName = "log_" + Constants.TIMESTAMPFORMATTER.format(new Date());
                        final EditText input = new EditText(context);
                        input.setText(defaultLogName);
                        new AlertDialog.Builder(context).setTitle(R.string.gps_log).setMessage(R.string.gps_log_name)
                                .setView(input).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                    public void onClick( DialogInterface dialog, int whichButton ) {
                                        Editable value = input.getText();
                                        String newName = value.toString();
                                        if (newName == null || newName.length() < 1) {
                                            newName = defaultLogName;
                                        }
                                        applicationManager.startLogging(newName);
                                        actionBar.checkLogging();
                                    }
                                }).setCancelable(false).show();
                    } else {
                        ApplicationManager.openDialog(R.string.gpslogging_only, context);
                    }
                }
                qa.dismiss();
            }
        });
        return startLogQuickaction;
    }

    public ActionItem getStopLogQuickAction( final ActionBar actionBar, final QuickAction qa ) {
        ActionItem stopLogQuickaction = new ActionItem();
        stopLogQuickaction.setTitle("Stop Log");
        stopLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_stop_log));
        stopLogQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                if (applicationManager.isGpsLogging()) {
                    applicationManager.stopLogging();
                    actionBar.checkLogging();
                }
                qa.dismiss();
            }
        });
        return stopLogQuickaction;
    }

    public int getDecimationFactor() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String decimationFactorStr = preferences.getString(DECIMATION_FACTOR, "5");
        int decimationFactor = 5;
        try {
            decimationFactor = Integer.parseInt(decimationFactorStr);
        } catch (Exception e) {
            // use default
        }
        return decimationFactor;
    }

}
