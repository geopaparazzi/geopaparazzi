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

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.ProximityIntentReceiver;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.TestMock;

/**
 * Singleton that takes care of gps matters.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GpsManager implements LocationListener, Listener {

    /**
     * GPS time interval.
     */
    public static int WAITSECONDS = 1;

    private static GpsManager gpsManager;
    private GpsStatus mStatus;

    private List<GpsManagerListener> listeners = new ArrayList<GpsManagerListener>();

    /**
     * The object responsible to log traces into the database. 
     */
    private static GpsDatabaseLogger gpsLogger;

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

    private LocationManager locationManager;
    private boolean gpsStarted = false;
    private SharedPreferences preferences;
    private boolean useNetworkPositions;

    private boolean isMockMode;

    private long lastLocationupdateMillis;

    private boolean gotFix;

    private GpsManager( Context context ) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        useNetworkPositions = preferences.getBoolean(LibraryConstants.PREFS_KEY_GPS_USE_NETWORK_POSITION, false);
        isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
    }

    /**
     * @param context  the context to use.
     * @return the singleton instance.
     */
    public synchronized static GpsManager getInstance( Context context ) {
        if (gpsManager == null) {
            gpsManager = new GpsManager(context);
            checkLoggerExists(context);
            gpsManager.checkGps(context);
            gpsManager.gpsStart();
            log("STARTED LISTENING");
        }
        // woke up from death and has the manager already but isn't listening any more
        if (!gpsManager.isGpsListening()) {
            gpsManager = new GpsManager(context);
            checkLoggerExists(context);
            gpsManager.checkGps(context);
            gpsManager.gpsStart();
            log("STARTED LISTENING AFTER REVIEW");
        }
        return gpsManager;
    }

    /**
     * Add a listener to gps.
     * 
     * @param listener the listener to add.
     */
    public void addListener( GpsManagerListener listener ) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Remove a listener to gps.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( GpsManagerListener listener ) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * @return the {@link LocationManager}.
     */
    public LocationManager getLocationManager() {
        return locationManager;
    }

    /**
     * Add a proximity alert.
     * 
     * @param context  the context to use.
     * @param lat lat 
     * @param lon lon
     * @param radius radius in meters.
     */
    public void addProximityAlert( Context context, double lat, double lon, float radius ) {
        String PROX_ALERT_INTENT = "com.javacodegeeks.android.lbs.ProximityAlert";
        Intent intent = new Intent(PROX_ALERT_INTENT);
        PendingIntent proximityIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        locationManager.addProximityAlert(lat, lon, radius, -1, proximityIntent);

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        context.registerReceiver(new ProximityIntentReceiver(), filter);
    }

    /**
     * Disposes the GpsManager and with it all connected services.
     * 
     * @param context  the context to use.
     */
    public void dispose( Context context ) {
        if (isDatabaseLogging()) {
            stopDatabaseLogging(context);
        }

        if (locationManager != null && gpsStarted) {
            locationManager.removeUpdates(gpsManager);
            locationManager.removeGpsStatusListener(gpsManager);
            for( GpsManagerListener activity : listeners ) {
                activity.gpsStop();
            }
        }
        if (TestMock.isOn) {
            TestMock.stopMocking(locationManager);
        }
        gpsStarted = false;
        log("GpsManager disposed.");
    }

    /**
     * @return <code>true</code> if the logger is closed.
     */
    public static boolean hasLoggerShutdown() {
        return gpsLogger.isShutdown();
    }

    /**
     * Starts listening to the gps provider.
     */
    private void gpsStart() {
        if (Debug.doMock || isMockMode) {
            log("Gps started using Mock locations");
            TestMock.startMocking(locationManager, gpsManager);
        } else {
            log("Gps started.");

            float minDistance = 0.2f;
            long waitForSecs = WAITSECONDS;

            // boolean doAtAndroidLevel = false;//
            // preferences.getBoolean(PREFS_KEY_GPSDOATANDROIDLEVEL,
            // // true);
            // if (doAtAndroidLevel) {
            // String minDistanceStr = preferences.getString(PREFS_KEY_GPSLOGGINGDISTANCE,
            // String.valueOf(GPS_LOGGING_DISTANCE));
            // try {
            // minDistance = Float.parseFloat(minDistanceStr);
            // } catch (Exception e) {
            // // ignore and use default
            // }
            // String intervalStr = preferences.getString(PREFS_KEY_GPSLOGGINGINTERVAL,
            // String.valueOf(GPS_LOGGING_INTERVAL));
            // try {
            // waitForSecs = Long.parseLong(intervalStr);
            // } catch (Exception e) {
            // // ignore and use default
            // }
            // }

            if (useNetworkPositions) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, waitForSecs * 1000l, minDistance,
                        gpsManager);
            } else {
                locationManager
                        .requestLocationUpdates(LocationManager.GPS_PROVIDER, waitForSecs * 1000l, minDistance, gpsManager);
            }
            locationManager.addGpsStatusListener(gpsManager);
        }
        gpsStarted = true;
        for( GpsManagerListener activity : listeners ) {
            activity.gpsStart();
        }
    }

    /**
     * Checks if the GPS is switched on.
     * 
     * <p>Does not say if the GPS is supplying valid data.</p>
     * 
     * @return <code>true</code> if the GPS is switched on.
     */
    public boolean isEnabled() {
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

    /**
     * Checks if the GPS has a valid fix, i.e. valid data to serve.
     * 
     * @return <code>true</code> if the GPS is in a usable logging state.
     */
    public boolean hasFix() {
        if (Debug.doMock || isMockMode) {
            if (TestMock.isOn)
                return true;
        }
        return gotFix;
    }

    /**
     * Checks if the GPS is currently recording a log.
     * 
     * @return <code>true</code> if the GPS is currently used to record data.
     */
    public static boolean isDatabaseLogging() {
        if (gpsLogger == null) {
            log("isDatabaseLogging called with gpsLogger == null");
            return false;
        }
        return gpsLogger.isDatabaseLogging();
    }

    /**
     * @return <code>true</code> if the gps is listening.
     */
    public boolean isGpsListening() {
        return gpsStarted;
    }

    /**
     * Check the GPS status.
     * 
     * @param context  the context to use.
     */
    public void checkGps( final Context context ) {
        if (!isEnabled()) {
            String prompt = context.getResources().getString(R.string.prompt_gpsenable);
            Utilities.yesNoMessageDialog(context, prompt, new Runnable(){
                public void run() {
                    Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(gpsOptionsIntent);
                }
            }, null);
        }
    }

    /**
     * @return the location.
     */
    public GpsLocation getLocation() {
        if (lastGpsLocation == null)
            return null;
        synchronized (lastGpsLocation) {
            return lastGpsLocation;
        }
    }

    /**
     * @return current gps log points num.
     */
    public static int getCurrentRunningGpsLogPointsNum() {
        return gpsLogger.getCurrentPointsNum();
    }

    /**
     * @return current log id.
     */
    public static long getCurrentRecordedLogId() {
        if (gpsLogger == null) {
            return -1l;
        }
        return gpsLogger.getCurrentRecordedLogId();
    }

    /**
     * @return current log distance.
     */
    public static int getCurrentRunningGpsLogDistance() {
        return gpsLogger.getCurrentDistance();
    }

    private static void checkLoggerExists( Context context ) {
        if (gpsLogger == null) {
            gpsLogger = new GpsDatabaseLogger(context);
        }
    }

    /**
     * Start gps logging.
     * 
     * @param context  the context to use. 
     * @param logName a name for the new gps log or <code>null</code>.
     * @param dbHelper the db helper.
     */
    public void startDatabaseLogging( Context context, String logName, IGpsLogDbHelper dbHelper ) {
        checkLoggerExists(context);
        addListener(gpsLogger);
        gpsLogger.startDatabaseLogging(logName, dbHelper);
    }

    /**
     * Stop gps logging.
     * 
     * @param context  the context to use.
     */
    public void stopDatabaseLogging( Context context ) {
        if (gpsLogger != null) {
            gpsLogger.stopDatabaseLogging();
            removeListener(gpsLogger);
        }
    }

    public void onLocationChanged( Location loc ) {
        if (loc == null)
            return;
        lastGpsLocation = new GpsLocation(loc);
        synchronized (lastGpsLocation) {
            lastLocationupdateMillis = SystemClock.elapsedRealtime();
            // Logger.d(gpsManager,
            //                "Position update: " + gpsLoc.getLongitude() + "/" + gpsLoc.getLatitude() + "/" + gpsLoc.getAltitude()); //$NON-NLS-1$ //$NON-NLS-2$
            lastGpsLocation.setPreviousLoc(previousLoc);
            // save last known location
            double recLon = lastGpsLocation.getLongitude();
            double recLat = lastGpsLocation.getLatitude();
            double recAlt = lastGpsLocation.getAltitude();
            PositionUtilities.putGpsLocationInPreferences(preferences, recLon, recLat, recAlt);
            previousLoc = loc;
            for( GpsManagerListener activity : listeners ) {
                activity.onLocationChanged(lastGpsLocation);
            }
        }
    }

    public void onStatusChanged( String provider, int status, Bundle extras ) {
        for( GpsManagerListener activity : listeners ) {
            activity.onStatusChanged(provider, status, extras);
        }
    }

    public void onProviderEnabled( String provider ) {
        for( GpsManagerListener activity : listeners ) {
            activity.onProviderEnabled(provider);
        }
    }

    public void onProviderDisabled( String provider ) {
        for( GpsManagerListener activity : listeners ) {
            activity.onProviderDisabled(provider);
        }
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
        gotFix = tmpGotFix;
        for( GpsManagerListener activity : listeners ) {
            activity.onGpsStatusChanged(event, mStatus);
        }
    }

    private static void log( String msg ) {
        try {
            if (GPLog.LOG_HEAVY)
                GPLog.addLogEntry("GPSMANAGER", null, null, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logABS( String msg ) {
        try {
            if (GPLog.LOG_ABSURD)
                GPLog.addLogEntry("GPSMANAGER", null, null, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
