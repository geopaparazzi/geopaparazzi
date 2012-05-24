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

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.activities.ProximityIntentReceiver;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.geopaparazzi.library.util.debug.TestMock;

/**
 * Singleton that takes care of gps matters.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GpsManager implements LocationListener, Listener {

    private static GpsManager gpsManager;

    private List<GpsManagerListener> listeners = new ArrayList<GpsManagerListener>();

    /**
     * The object responsible to log traces into the database. 
     */
    private static GpsLogger gpsLogger;

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

    private LocationManager locationManager;
    private long mLastLocationMillis;
    private boolean hasGPSFix = false;
    private boolean isListening = false;
    private SharedPreferences preferences;
    private boolean useNetworkPositions;

    private GpsManager( Context context ) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        useNetworkPositions = preferences.getBoolean(LibraryConstants.PREFS_KEY_GPS_USE_NETWORK_POSITION, false);
    }

    public synchronized static GpsManager getInstance( Context context ) {
        if (gpsManager == null) {
            gpsManager = new GpsManager(context);
            gpsManager.checkLoggerExists(context);
            gpsManager.checkGps(context);
            gpsManager.startListening();
            if (Debug.D)
                Logger.d(gpsManager, "STARTED LISTENING");
        }
        // woke up from death and has the manager already but isn't listening any more
        if (!gpsManager.isGpsListening()) {
            gpsManager = new GpsManager(context);
            gpsManager.checkLoggerExists(context);
            gpsManager.checkGps(context);
            gpsManager.startListening();
            if (Debug.D)
                Logger.d(gpsManager, "STARTED LISTENING AFTER REVIEW");
        }
        return gpsManager;
    }

    /**
     * Add a listener to gps.
     * 
     * @param listener the listener to add.
     */
    public void addListener( GpsManagerListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener to gps.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( GpsManagerListener listener ) {
        listeners.remove(listener);
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }
    
    public void addProximityAlert( Context context, double lat, double lon, float radius ) {
        String PROX_ALERT_INTENT = "com.javacodegeeks.android.lbs.ProximityAlert";
        Intent intent = new Intent(PROX_ALERT_INTENT);
        // intent.putExtra(PlaySoundActivity.MESSAGE, "Proximity alert triggered!");
        PendingIntent proximityIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        locationManager.addProximityAlert(lat, lon, radius, -1, proximityIntent);

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        context.registerReceiver(new ProximityIntentReceiver(), filter);

    }

    /**
     * Disposes the GpsManager and with it all connected services.
     */
    public void dispose( Context context ) {
        if (isLogging()) {
            stopLogging(context);
        }

        if (locationManager != null) {
            locationManager.removeUpdates(gpsManager);
            locationManager.removeGpsStatusListener(gpsManager);
        }
        if (TestMock.isOn) {
            TestMock.stopMocking(locationManager);
        }
        isListening = false;
    }

    /**
     * Starts listening to the gps provider.
     */
    private void startListening() {
        if (Debug.doMock) {
            if (Debug.D)
                Logger.d(this, "Using Mock locations");
            TestMock.startMocking(locationManager, gpsManager);
        } else {
            if (Debug.D)
                Logger.d(this, "Using GPS");
            if (useNetworkPositions) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000l, 0f, gpsManager);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000l, 0f, gpsManager);
            }
            locationManager.addGpsStatusListener(gpsManager);
        }
        isListening = true;
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
        if (Debug.D)
            Logger.i(this, "Gps is on: " + gpsIsEnabled);
        return gpsIsEnabled;
    }

    /**
     * Checks if the GPS has a valid fix, i.e. valid data to serve.
     * 
     * @return <code>true</code> if the GPS is in a usable logging state.
     */
    public boolean hasValidData() {
        if (Debug.doMock) {
            if (TestMock.isOn)
                return true;
        }
        return hasGPSFix && getLocation() != null;
    }

    /**
     * Checks if the GPS is currently recording a log.
     * 
     * @return <code>true</code> if the GPS is currently used to record data.
     */
    public boolean isLogging() {
        // if (Debug.doMock) {
        // if (TestMock.isOn)
        // return true;
        // }

        if (gpsLogger == null) {
            return false;
        }
        return gpsLogger.isLogging();
    }

    private boolean isGpsListening() {
        return isListening;
    }

    private void checkGps( final Context context ) {
        if (!isEnabled()) {
            String prompt = context.getResources().getString(R.string.prompt_gpsenable);
            String ok = context.getResources().getString(R.string.yes);
            String cancel = context.getResources().getString(R.string.no);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(prompt).setCancelable(false).setPositiveButton(ok, new DialogInterface.OnClickListener(){
                public void onClick( DialogInterface dialog, int id ) {
                    Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(gpsOptionsIntent);
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

    public GpsLocation getLocation() {
        return gpsLoc;
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

    /**
     * Returns the points of the gps log, if one is being recorded or <code>null</code>. 
     * 
     * @return the gps log or <code>null</code>.
     */
    public List<double[]> getCurrentRecordedGpsLog() {
        return gpsLogger.getCurrentRecordedLog();
    }

    private void checkLoggerExists( Context context ) {
        if (gpsLogger == null) {
            gpsLogger = new GpsLogger(context);
        }
    }

    /**
     * Start gps logging.
     * 
     * @param logName a name for the new gps log or <code>null</code>.
     * @param dbHelper the db helper.
     */
    public void startLogging( Context context, String logName, IGpsLogDbHelper dbHelper ) {
        checkLoggerExists(context);
        addListener(gpsLogger);
        gpsLogger.startLogging(logName, dbHelper);
    }

    /**
     * Stop gps logging.
     */
    public void stopLogging( Context context ) {
        checkLoggerExists(context);
        gpsLogger.stopLogging();
        removeListener(gpsLogger);
    }

    public void onLocationChanged( Location loc ) {
        if (loc == null)
            return;
        mLastLocationMillis = SystemClock.elapsedRealtime();

        gpsLoc = new GpsLocation(loc);
        if (previousLoc == null) {
            previousLoc = loc;
        }

        // Logger.d(gpsManager,
        //                "Position update: " + gpsLoc.getLongitude() + "/" + gpsLoc.getLatitude() + "/" + gpsLoc.getAltitude()); //$NON-NLS-1$ //$NON-NLS-2$
        gpsLoc.setPreviousLoc(previousLoc);
        for( GpsManagerListener listener : listeners ) {
            listener.onLocationChanged(gpsLoc);
        }
        // save last known location
        double recLon = gpsLoc.getLongitude();
        double recLat = gpsLoc.getLatitude();
        double recAlt = gpsLoc.getAltitude();
        PositionUtilities.putGpsLocationInPreferences(preferences, recLon, recLat, recAlt);

        previousLoc = loc;
    }

    public void onProviderDisabled( String provider ) {
        // if (isGpsLogging()) {
        // stopLogging();
        // }
        // stopListening();
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

    public void onGpsStatusChanged( int event ) {
        switch( event ) {
        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            if (gpsLoc != null)
                hasGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;
            // if (hasGPSFix) { // A fix has been acquired.
            // if (Debug.D) Logger.i(this, "Fix acquired");
            // } else { // The fix has been lost.
            // if (Debug.D) Logger.i(this, "Fix lost");
            // }

            break;
        case GpsStatus.GPS_EVENT_FIRST_FIX:
            if (Debug.D)
                Logger.i(this, "First fix");
            hasGPSFix = true;
            break;
        }
        for( GpsManagerListener listener : listeners ) {
            listener.onStatusChanged(hasGPSFix);
        }
    }

}
