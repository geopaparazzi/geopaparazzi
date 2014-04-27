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

import static eu.geopaparazzi.library.gps.GpsService.GPS_LOGGING_STATUS;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_BROADCAST_NOTIFICATION;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_DO_BROADCAST;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_GPSSTATUS_EXTRAS;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_POSITION;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_POSITION_EXTRAS;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_POSITION_TIME;
import static eu.geopaparazzi.library.gps.GpsService.GPS_SERVICE_STATUS;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_LOGGING;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_LOG_HELPER_CLASS;
import static eu.geopaparazzi.library.gps.GpsService.START_GPS_LOG_NAME;
import static eu.geopaparazzi.library.gps.GpsService.STOP_GPS_LOGGING;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
/**
 * 
 * A service utils class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsServiceUtilities {

    /**
     * Start the service.
     * 
     * @param activity the activity to use.
     */
    public static void startGpsService( Activity activity ) {
        Intent intent = new Intent(activity, GpsService.class);
        activity.startService(intent);
    }

    /**
     * Stop the service.
     * 
     * @param activity the activity to use.
     */
    public static void stopGpsService( Activity activity ) {
        Intent intent = new Intent(activity, GpsService.class);
        activity.stopService(intent);
    }

    /**
     * Utility to get the {@link GpsServiceStatus} from an intent.
     * 
     * @param intent the intent.
     * @return the status.
     */
    public static GpsServiceStatus getGpsServiceStatus( Intent intent ) {
        if (intent == null) {
            return null;
        }
        int gpsServiceStatusCode = intent.getIntExtra(GPS_SERVICE_STATUS, 0);
        return GpsServiceStatus.getStatusForCode(gpsServiceStatusCode);
    }

    /**
     * Utility to get the {@link GpsLoggingStatus} from an intent.
     * 
     * @param intent the intent.
     * @return the status.
     */
    public static GpsLoggingStatus getGpsLoggingStatus( Intent intent ) {
        if (intent == null) {
            return null;
        }
        int gpsServiceStatusCode = intent.getIntExtra(GPS_LOGGING_STATUS, 0);
        return GpsLoggingStatus.getStatusForCode(gpsServiceStatusCode);
    }

    /**
     * Utility to get the position from an intent.
     * 
     * @param intent the intent.
     * @return the position as lon, lat, elev.
     */
    public static double[] getPosition( Intent intent ) {
        if (intent == null) {
            return null;
        }
        double[] position = intent.getDoubleArrayExtra(GPS_SERVICE_POSITION);
        return position;
    }

    /**
     * Utility to get the position extras from an intent.
     * 
     * @param intent the intent.
     * @return the position as accuracy, speed, bearing.
     */
    public static float[] getPositionExtras( Intent intent ) {
        if (intent == null) {
            return null;
        }
        float[] positionExtras = intent.getFloatArrayExtra(GPS_SERVICE_POSITION_EXTRAS);
        return positionExtras;
    }

    /**
     * Utility to get the position time from an intent.
     * 
     * @param intent the intent.
     * @return the position time.
     */
    public static long getPositionTime( Intent intent ) {
        if (intent == null) {
            return -1;
        }
        long time = intent.getLongExtra(GPS_SERVICE_POSITION_TIME, -1);
        return time;
    }

    /**
     * Utility to get the gps status extras from an intent.
     * 
     * @param intent the intent.
     * @return the position as maxSatellites, satCount, satUsedInFixCount.
     */
    public static int[] getGpsStatusExtras( Intent intent ) {
        if (intent == null) {
            return null;
        }
        int[] gpsstatusExtras = intent.getIntArrayExtra(GPS_SERVICE_GPSSTATUS_EXTRAS);
        return gpsstatusExtras;
    }

    /**
     * Register an activity for {@link GpsService} broadcasts.
     * 
     * @param activity the activity.
     * @param receiver the receiver.
     */
    public static void registerForBroadcasts( Activity activity, BroadcastReceiver receiver ) {
        activity.registerReceiver(receiver, new IntentFilter(GPS_SERVICE_BROADCAST_NOTIFICATION));
    }

    /**
     * Unregister an activity from {@link GpsService} broadcasts.
     * 
     * @param activity the activity.
     * @param receiver the receiver.
     */
    public static void unregisterFromBroadcasts( Activity activity, BroadcastReceiver receiver ) {
        if (receiver != null)
            activity.unregisterReceiver(receiver);
    }

    /**
     * Trigger a broadcast.
     * 
     * @param context the {@link Context} to use.
     */
    public static void triggerBroadcast( Context context ) {
        Intent intent = new Intent(context, GpsService.class);
        intent.putExtra(GPS_SERVICE_DO_BROADCAST, true);
        context.startService(intent);
    }

    /**
     * Start logging to the database.
     * 
     * @param context the context to use.
     * @param logName the name of the log.
     * @param className the class to use as helper.
     */
    public static void startDatabaseLogging( Context context, String logName, String className ) {
        Intent intent = new Intent(context, GpsService.class);
        intent.putExtra(START_GPS_LOGGING, true);
        intent.putExtra(START_GPS_LOG_NAME, logName);
        intent.putExtra(START_GPS_LOG_HELPER_CLASS, className);
        context.startService(intent);
    }

    /**
     * Stop logging to the database.
     * 
     * @param context the context to use.
     */
    public static void stopDatabaseLogging( Context context ) {
        Intent intent = new Intent(context, GpsService.class);
        intent.putExtra(STOP_GPS_LOGGING, true);
        context.startService(intent);
    }
}
