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
package eu.geopaparazzi.library.gps;

import static eu.geopaparazzi.library.util.LibraryConstants.GPS_LOGGING_DISTANCE;
import static eu.geopaparazzi.library.util.LibraryConstants.GPS_LOGGING_INTERVAL;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSLOGGINGDISTANCE;
import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_GPSLOGGINGINTERVAL;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.debug.TestMock;

/**
 * 
 * A service to handle the GPS data.
 * 
 * <p>
 * use this to start and trigger a service</br>
 * <code>Intent i= new Intent(context, GpsService.class)</code>;</br>
 * add data to the intent</br>
 * <code>i.putExtra("KEY1", "Value to be used by the service");</br>
 * context.startService(i);</code> 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GpsService extends Service implements LocationListener, Listener {
    private static final boolean DOLOGPOSITION = GPLog.LOG_ABSURD;
    private static final boolean DO_WHILE_LOOP_LOG = GPLog.LOG_ABSURD;

    /**
     * Intent key to pass the boolean to start gps database logging.
     */
    public static final String START_GPS_LOGGING = "START_GPS_LOGGING";
    /**
     * Intent key to pass the boolean to stop gps database logging.
     */
    public static final String STOP_GPS_LOGGING = "STOP_GPS_LOGGING";
    /**
     * Intent key to pass the string gps database log helper class.
     */
    public static final String START_GPS_LOG_HELPER_CLASS = "START_GPS_LOG_HELPER_CLASS";
    /**
     * Intent key to pass the string gps log name.
     */
    public static final String START_GPS_LOG_NAME = "START_GPS_LOG_NAME";

    /**
     * Intent key to use for broadcasts.
     */
    public static final String GPS_SERVICE_BROADCAST_NOTIFICATION = "eu.geopaparazzi.library.gps.GpsService";

    /**
     * Intent key to use for int gps status.
     * 
     * <p>Status can be:
     * <ul>
     * <li>gps off = 0</li>
     * <li>gps on but not listening for updates = 1</li>
     * <li>gps on and listening for updates but no fix= 2</li>
     * <li>gps has fix = 3</li>
     * </ul>
     */
    public static final String GPS_SERVICE_STATUS = "GPS_SERVICE_STATUS";

    /**
     * Intent key to use for int gps logging status.
     * 
     * <p>Status can be:
     * <ul>
     * <li>gps logging off = 0</li>
     * <li>gps logging on = 1</li>
     * </ul>
     */
    public static final String GPS_LOGGING_STATUS = "GPS_LOGGING_STATUS";
    /**
     * Intent key to use for double array position data [lon, lat, elev].
     */
    public static final String GPS_SERVICE_POSITION = "GPS_SERVICE_POSITION";
    /**
     * Intent key to use for double array position extra data [accuracy, speed, bearing].
     */
    public static final String GPS_SERVICE_POSITION_EXTRAS = "GPS_SERVICE_POSITION_EXTRAS";
    /**
     * Intent key to use for long time.
     */
    public static final String GPS_SERVICE_POSITION_TIME = "GPS_SERVICE_POSITION_TIME";
    /**
     * Intent key to use for current recorded log id.
     */
    public static final String GPS_SERVICE_CURRENT_LOG_ID = "GPS_SERVICE_CURRENT_LOG_ID";
    /**
     * Intent key to use for int array gps extra data [maxSatellites, satCount, satUsedInFixCount].
     */
    public static final String GPS_SERVICE_GPSSTATUS_EXTRAS = "GPS_SERVICE_GPSSTATUS_EXTRAS";
    /**
     * Intent key to use to trigger a broadcast.
     */
    public static final String GPS_SERVICE_DO_BROADCAST = "GPS_SERVICE_DO_BROADCAST";

    private SharedPreferences preferences;
    private LocationManager locationManager;
    private boolean useNetworkPositions = false;
    private boolean isMockMode = false;

    /**
     * The last taken gps location.
     */
    private GpsLocation lastGpsLocation = null;

    /**
     * The previous gps location or null if no gps location was taken yet.
     * 
     * <p>This changes with every {@link #onLocationChanged(Location)}.</p>
     */
    private Location previousLoc = null;

    private long lastLocationupdateMillis;
    private int currentPointsNum;
    /**
     * The current total distance of the track from start to the current point.
     */
    private double currentDistance;

    /**
     * GPS time interval.
     */
    private static int WAITSECONDS = 1;

    private GpsStatus mStatus;
    private long currentRecordedLogId = -1;
    private volatile boolean gotFix;
    private boolean isDatabaseLogging = false;
    private boolean isListeningForUpdates = false;
    private boolean isProviderEnabled;
    private Handler toastHandler;

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {

        // GPLog.addLogEntry(this, "onStartCommand called with intent: " + intent);

        /*
         * If startService(intent) is called while the service is running, 
         * its onStartCommand() is also called. Therefore your service needs 
         * to be prepared that onStartCommand() can be called several times.
         */
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            useNetworkPositions = preferences.getBoolean(LibraryConstants.PREFS_KEY_GPS_USE_NETWORK_POSITION, false);
            isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
            toastHandler = new Handler();
            log("onStartCommand: Preferences created");
        }
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.addGpsStatusListener(this);
            isProviderEnabled = isGpsOn();

            log("onStartCommand: LocationManager created + GpsService started");
        }
        if (!isListeningForUpdates) {
            registerForLocationUpdates();
            log("onStartCommand: Registered for location updates");
        }
        if (intent != null) {
            /*
             * START GPS logging
             */
            if (intent.hasExtra(START_GPS_LOGGING)) {
                boolean startGpsLogging = intent.getBooleanExtra(START_GPS_LOGGING, false);
                if (startGpsLogging) {
                    log("onStartCommand: Start GPS logging called");
                    if (!isDatabaseLogging) {
                        String gpsLogName = intent.getStringExtra(START_GPS_LOG_NAME);
                        String gpsLogHelperClass = intent.getStringExtra(START_GPS_LOG_HELPER_CLASS);
                        try {
                            Class< ? > logHelper = Class.forName(gpsLogHelperClass);
                            IGpsLogDbHelper newInstance = (IGpsLogDbHelper) logHelper.newInstance();
                            startDatabaseLogging(gpsLogName, newInstance);
                        } catch (Exception e) {
                            GPLog.error(this, "Could not start logging", e);
                        }
                    }
                }
            }
            if (intent.hasExtra(STOP_GPS_LOGGING)) {
                boolean stopGpsLogging = intent.getBooleanExtra(STOP_GPS_LOGGING, false);
                if (stopGpsLogging) {
                    log("onStartCommand: Stop GPS logging called");
                    if (isDatabaseLogging) {
                        stopDatabaseLogging();
                    }
                }
            }
            if (intent.hasExtra(GPS_SERVICE_DO_BROADCAST)) {
                log("onStartCommand: broadcast trigger");
                boolean doBroadcast = intent.getBooleanExtra(GPS_SERVICE_DO_BROADCAST, false);
                if (doBroadcast) {
                    broadcast("triggered by onStartCommand Intent");
                }
            }

        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        log("onDestroy Gpsservice.");
        if (isDatabaseLogging) {
            stopDatabaseLogging();
        }

        if (locationManager != null && isListeningForUpdates) {
            locationManager.removeUpdates(this);
            locationManager.removeGpsStatusListener(this);
            isListeningForUpdates = false;
        }
        if (TestMock.isOn) {
            TestMock.stopMocking(locationManager);
        }
        super.onDestroy();
    }

    /**
     * Starts listening to the gps provider.
     */
    private void registerForLocationUpdates() {
        if (isMockMode) {
            log("Gps started using Mock locations");
            TestMock.startMocking(locationManager, this);
            isListeningForUpdates = true;
        } else {
            float minDistance = 0.2f;
            long waitForSecs = WAITSECONDS;

            if (useNetworkPositions) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, waitForSecs * 1000l, minDistance, this);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, waitForSecs * 1000l, minDistance, this);
            }
            isListeningForUpdates = true;
            log("registered for updates.");
        }
        broadcast("triggered by registerForLocationUpdates");
    }

    /**
     * Starts logging into the database.
     * 
     * @param logName a name for the new log or <code>null</code>.
     * @param dbHelper the db helper.
     */
    private void startDatabaseLogging( final String logName, final IGpsLogDbHelper dbHelper ) {
        if (isDatabaseLogging) {
            // we do not start twice
            return;
        }
        isDatabaseLogging = true;

        Thread t = new Thread(){

            public void run() {
                try {

                    SQLiteDatabase sqliteDatabase = dbHelper.getDatabase();
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    long gpsLogId = dbHelper.addGpsLog(now, now, 0, logName, 2f, "red", true);
                    currentRecordedLogId = gpsLogId;
                    log("GPS Start logging. Logid: " + gpsLogId);

                    // get preferences
                    String minDistanceStr = preferences.getString(PREFS_KEY_GPSLOGGINGDISTANCE,
                            String.valueOf(GPS_LOGGING_DISTANCE));
                    float minDistance = 1f;
                    try {
                        minDistance = Float.parseFloat(minDistanceStr);
                    } catch (Exception e) {
                        // ignore and use default
                    }
                    String intervalStr = preferences
                            .getString(PREFS_KEY_GPSLOGGINGINTERVAL, String.valueOf(GPS_LOGGING_INTERVAL));
                    int waitForSecs = 3;
                    try {
                        waitForSecs = Integer.parseInt(intervalStr);
                    } catch (Exception e) {
                        // ignore and use default
                    }
                    if (DO_WHILE_LOOP_LOG) {
                        GPLog.addLogEntry(GpsService.this, "GPS waiting interval: " + waitForSecs);
                        GPLog.addLogEntry(GpsService.this, "GPS min distance: " + minDistance);
                    }

                    long previousGpsLocationTime = -1;
                    currentPointsNum = 0;
                    currentDistance = 0;
                    while( isDatabaseLogging ) {
                        if (gotFix || isMockMode) {
                            if (DO_WHILE_LOOP_LOG)
                                GPLog.addLogEntry(GpsService.this, "GPS DEBUG: loop while at: " + System.nanoTime());
                            if (lastGpsLocation == null) {
                                if (DO_WHILE_LOOP_LOG)
                                    GPLog.addLogEntry(GpsService.this, "GPS JUMP POINT: lastGpsLocation == null");
                                if (!holdABitAndCheckLogging(waitForSecs)) {
                                    break;
                                }
                                continue;
                            }
                            long time = lastGpsLocation.getTime();
                            if (previousGpsLocationTime == time) {
                                if (DO_WHILE_LOOP_LOG)
                                    GPLog.addLogEntry(GpsService.this,
                                            "GPS JUMP POINT: lastGpsLocation == previous (no new point incoming)");
                                if (!holdABitAndCheckLogging(waitForSecs)) {
                                    break;
                                }
                                continue;
                            }
                            previousGpsLocationTime = time;
                            if (lastGpsLocation.getPreviousLoc() == null) {
                                if (DO_WHILE_LOOP_LOG)
                                    GPLog.addLogEntry(GpsService.this,
                                            "GPS JUMP POINT: waiting for second valid point to come in.");
                                if (!holdABitAndCheckLogging(waitForSecs)) {
                                    break;
                                }
                                continue;
                            }
                            double recLon = lastGpsLocation.getLongitude();
                            double recLat = lastGpsLocation.getLatitude();
                            double recAlt = lastGpsLocation.getAltitude();

                            if (DO_WHILE_LOOP_LOG)
                                GPLog.addLogEntry(GpsService.this, "GPS DEBUG: loop while 1: " + System.nanoTime());
                            double lastDistance = lastGpsLocation.distanceToPrevious();
                            if (DO_WHILE_LOOP_LOG) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("GPS\ngpsloc: ");
                                sb.append(lastGpsLocation.getLatitude());
                                sb.append("/");
                                sb.append(lastGpsLocation.getLongitude());
                                sb.append("\n");
                                if (lastGpsLocation.getPreviousLoc() != null) {
                                    sb.append("previousLoc: ");
                                    sb.append(lastGpsLocation.getPreviousLoc().getLatitude());
                                    sb.append("/");
                                    sb.append(lastGpsLocation.getPreviousLoc().getLongitude());
                                    sb.append("\n");
                                }
                                sb.append("distance: ");
                                sb.append(lastDistance);
                                sb.append(" - mindistance: ");
                                sb.append(minDistance);
                                logABS(sb.toString());
                            }
                            if (DO_WHILE_LOOP_LOG)
                                GPLog.addLogEntry(GpsService.this, "GPS DEBUG: loop while 2: " + System.nanoTime());
                            // ignore near points
                            if (lastDistance < minDistance) {
                                if (DO_WHILE_LOOP_LOG)
                                    GPLog.addLogEntry(GpsService.this, "GPS JUMP POINT: distance from previous");
                                if (!holdABitAndCheckLogging(waitForSecs)) {
                                    break;
                                }
                                continue;
                            }
                            if (DO_WHILE_LOOP_LOG)
                                GPLog.addLogEntry(GpsService.this, "GPS DEBUG: loop while 3: " + System.nanoTime());
                            try {
                                if (isDatabaseLogging) {
                                    dbHelper.addGpsLogDataPoint(sqliteDatabase, gpsLogId, recLon, recLat, recAlt,
                                            lastGpsLocation.getSqlDate());
                                }
                            } catch (Exception e) {
                                // we log the exception and try to go on
                                GPLog.error(this, "Point in db writing error!", e);
                            }
                            if (DO_WHILE_LOOP_LOG)
                                GPLog.addLogEntry(GpsService.this, "GPS DEBUG: loop while 4: " + System.nanoTime());
                            currentPointsNum++;
                            currentDistance = currentDistance + lastDistance;
                        }
                        if (!holdABitAndCheckLogging(waitForSecs)) {
                            break;
                        }
                    }

                    if (currentPointsNum < 4) {
                        log("Removing gpslog, since too few points were added. Logid: " + gpsLogId);
                        dbHelper.deleteGpslog(gpsLogId);
                    } else {
                        // set the end time stamp and the total distance for the track
                        java.sql.Date end = new java.sql.Date(System.currentTimeMillis());
                        dbHelper.setEndTs(gpsLogId, end);
                        dbHelper.setTrackLengthm(gpsLogId, (double) currentDistance);
                    }

                    currentPointsNum = 0;
                    currentDistance = 0;
                    currentRecordedLogId = -1;

                } catch (SQLiteFullException e) {
                    e.printStackTrace();
                    String msg = getResources().getString(R.string.error_disk_full);
                    GPLog.error(this, msg, e);
                    toastHandler.post(new ToastRunnable(msg));
                } catch (Exception e) {
                    e.printStackTrace();
                    String msg = getResources().getString(R.string.cantwrite_gpslog);
                    GPLog.error(this, msg, e);
                    toastHandler.post(new ToastRunnable(msg));
                } finally {
                    isDatabaseLogging = false;
                }
                log("GPS Exit logging...");
            }

            /**
             * Waits a bit before next gps query.
             * 
             * @param waitForSecs seconds to wait.
             * @return <code>false</code> if the gps got interrupted, <code>true</code> else.
             */
            private boolean holdABitAndCheckLogging( int waitForSecs ) {
                try {
                    for( int i = 0; i < waitForSecs; i++ ) {
                        Thread.sleep(1000L);
                        if (!isDatabaseLogging) {
                            return false;
                        }
                    }
                    return true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    String msg = getResources().getString(R.string.cantwrite_gpslog);
                    GPLog.error(this, msg, e);
                    return true;
                }
            }
        };
        t.start();

        Toast.makeText(GpsService.this, R.string.gpsloggingon, Toast.LENGTH_SHORT).show();
    }

    private void stopDatabaseLogging() {
        isDatabaseLogging = false;
    }

    private static void log( String msg ) {
        try {
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("GPSSERVICE", null, null, msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logABS( String msg ) {
        try {
            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("GPSSERVICE", null, null, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the GPS is switched on.
     * 
     * <p>Does not say if the GPS is supplying valid data.</p>
     * 
     * @return <code>true</code> if the GPS is switched on.
     */
    private boolean isGpsOn() {
        if (locationManager == null) {
            return false;
        }
        boolean gpsIsEnabled;
        if (useNetworkPositions) {
            gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } else {
            gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        logABS("Gps is enabled: " + gpsIsEnabled);
        return gpsIsEnabled;
    }

    public void onLocationChanged( Location loc ) {
        if (loc == null) {
            lastGpsLocation = null;
            return;
        }
        lastGpsLocation = new GpsLocation(loc);
        synchronized (lastGpsLocation) {
            lastLocationupdateMillis = SystemClock.elapsedRealtime();
            lastGpsLocation.setPreviousLoc(previousLoc);
            // save last known location
            double recLon = lastGpsLocation.getLongitude();
            double recLat = lastGpsLocation.getLatitude();
            double recAlt = lastGpsLocation.getAltitude();
            PositionUtilities.putGpsLocationInPreferences(preferences, recLon, recLat, recAlt);
            previousLoc = loc;

            broadcast("triggered by onLocationChanged");
        }
    }

    public void onStatusChanged( String provider, int status, Bundle extras ) {
        // for( GpsManagerListener activity : listeners ) {
        // activity.onStatusChanged(provider, status, extras);
        // }
    }

    public void onProviderEnabled( String provider ) {
        isProviderEnabled = true;
        if (!isListeningForUpdates) {
            registerForLocationUpdates();
        }
        broadcast("triggered by onProviderEnabled");
    }

    public void onProviderDisabled( String provider ) {
        isProviderEnabled = false;
        broadcast("triggered by onProviderDisabled");
    }

    public void onGpsStatusChanged( int event ) {
        mStatus = locationManager.getGpsStatus(mStatus);

        // check fix
        boolean tmpGotFix = GpsStatusInfo.checkFix(gotFix, lastLocationupdateMillis, event);
        if (!tmpGotFix) {
            // check if it is just standing still
            GpsStatusInfo info = new GpsStatusInfo(mStatus);
            int satForFixCount = info.getSatUsedInFixCount();
            if (satForFixCount > 2) {
                tmpGotFix = true;
                // updating loc update, assuming the still filter is giving troubles
                lastLocationupdateMillis = SystemClock.elapsedRealtime();
            }
        }

        // if (DOLOGPOSITION) {
        // StringBuilder sb = new StringBuilder();
        // sb.append("gotFix: ").append(gotFix).append(" tmpGotFix: ").append(tmpGotFix).append("\n");
        // GPLog.addLogEntry("GPSSERVICE", sb.toString());
        // }

        if (tmpGotFix != gotFix) {
            gotFix = tmpGotFix;
            broadcast("triggered by onGpsStatusChanged on fix change: " + gotFix);
        } else {
            gotFix = tmpGotFix;
            if (!tmpGotFix && isProviderEnabled) {
                broadcast("triggered by onGpsStatusChanged on fix change: " + gotFix);
            }
        }

        if (!gotFix) {
            lastGpsLocation = null;
        }
    }

    /**
     * @param message a message that can be used for logging.
     */
    private void broadcast( String message ) {
        Intent intent = new Intent(GPS_SERVICE_BROADCAST_NOTIFICATION);

        int status = 0; // gps off
        if (isProviderEnabled) {
            status = 1; // gps on
        }
        if (isProviderEnabled && isListeningForUpdates && !gotFix) {
            status = 2; // listening for updates but has no fix
        }
        if ((isProviderEnabled && isListeningForUpdates && gotFix && lastGpsLocation != null) || isMockMode) {
            status = 3; // listening for updates and has fix
        }
        intent.putExtra(GPS_SERVICE_STATUS, status);
        if (isDatabaseLogging || (isDatabaseLogging && isMockMode)) {
            intent.putExtra(GPS_SERVICE_CURRENT_LOG_ID, currentRecordedLogId);
            intent.putExtra(GPS_LOGGING_STATUS, 1);
        } else {
            intent.putExtra(GPS_LOGGING_STATUS, 0);
        }
        double lon = -1;
        double lat = -1;
        double elev = -1;
        float accuracy = -1;
        float speed = -1;
        float bearing = -1;
        long time = -1;
        if (lastGpsLocation != null) {
            lon = lastGpsLocation.getLongitude();
            lat = lastGpsLocation.getLatitude();
            elev = lastGpsLocation.getAltitude();
            double[] lastPositionArray = new double[]{lon, lat, elev};
            intent.putExtra(GPS_SERVICE_POSITION, lastPositionArray);
            accuracy = lastGpsLocation.getAccuracy();
            speed = lastGpsLocation.getSpeed();
            bearing = lastGpsLocation.getBearing();
            float[] lastPositionExtrasArray = new float[]{accuracy, speed, bearing};
            intent.putExtra(GPS_SERVICE_POSITION_EXTRAS, lastPositionExtrasArray);
            time = lastGpsLocation.getTime();
            intent.putExtra(GPS_SERVICE_POSITION_TIME, time);
        }
        int maxSatellites = -1;
        int satCount = -1;
        int satUsedInFixCount = -1;
        if (mStatus != null) {
            GpsStatusInfo info = new GpsStatusInfo(mStatus);
            maxSatellites = info.getMaxSatellites();
            satCount = info.getSatCount();
            satUsedInFixCount = info.getSatUsedInFixCount();
            intent.putExtra(GPS_SERVICE_GPSSTATUS_EXTRAS, new int[]{maxSatellites, satCount, satUsedInFixCount});
        }

        if (DOLOGPOSITION) {
            StringBuilder sb = new StringBuilder();
            sb.append("GPS SERVICE INFO: ").append(message).append("\n");
            sb.append("---------------------------\n");
            sb.append("gps status=").append(GpsServiceStatus.getStatusForCode(status)).append("(" + status).append(")\n");
            sb.append("lon=").append(lon).append("\n");
            sb.append("lat=").append(lat).append("\n");
            sb.append("elev=").append(elev).append("\n");
            sb.append("accuracy=").append(accuracy).append("\n");
            sb.append("speed=").append(speed).append("\n");
            sb.append("bearing=").append(bearing).append("\n");
            sb.append("time=").append(time).append("\n");
            sb.append("maxSatellites=").append(maxSatellites).append("\n");
            sb.append("satCount=").append(satCount).append("\n");
            sb.append("satUsedInFix=").append(satUsedInFixCount).append("\n");
            GPLog.addLogEntry("GPSSERVICE", sb.toString());
        }

        sendBroadcast(intent);
    }

    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable( String text ) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_LONG).show();
        }
    }

    // /////////////////////////////////////////////
    // UNUSET METHODS
    // /////////////////////////////////////////////
    // @Override
    // public void onCreate() {
    // super.onCreate();
    // /*
    // * If the startService(intent) method is called and the service is not
    // * yet running, the service object is created and the onCreate()
    // * method of the service is called.
    // */
    // }
    //
    // @Override
    // public ComponentName startService( Intent service ) {
    // /*
    // * Once the service is started, the startService(intent) method in the
    // * service is called. It passes in the Intent object from the
    // * startService(intent) call.
    // */
    // return super.startService(service);
    // }
    //
    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }
    //
    // @Override
    // public boolean stopService( Intent name ) {
    // /*
    // * You stop a service via the stopService() method. No matter how
    // * frequently you called the startService(intent) method, one call
    // * to the stopService() method stops the service.
    // *
    // * A service can terminate itself by calling the stopSelf() method.
    // * This is typically done if the service finishes its work.
    // */
    // return super.stopService(name);
    // }
}
