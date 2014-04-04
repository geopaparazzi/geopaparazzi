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
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;

/**
 * The Gps engine, used to put logs into the database.
 * 
 * <p>This class takes care to make the logging occur at preferences settings.
 * That is why it is not listening directly to the gps, but instead to the gps manager.
 * It is the manager that updates the position.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class GpsDatabaseLogger implements GpsManagerListener {
    private final Context context;

    /**
     * The last taken gps location.
     */
    private GpsLocation gpsLoc = null;

    /**
     * The previous gpslog location.
     * 
     * <p>This changes with every gps log waiting time.</p>
     */
    private Location previousLogLoc = null;

    private boolean isDatabaseLogging = false;
    private boolean isShutdown = false;

    // private MediaPlayer mMediaPlayer;
    // private boolean doPlayAlarm = false;

    private int currentPointsNum;
    /**
     * The current total distance of the track from start to the current point.
     */
    private double currentDistance;

    /**
     * @param context  the context to use.
     */
    public GpsDatabaseLogger( Context context ) {
        this.context = context;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);

    }

    private long currentRecordedLogId = -1;

    private volatile boolean gotFix;

    private long lastLocationupdateMillis;

    private boolean isMockMode;
    /**
     * @return the log id.
     */
    public long getCurrentRecordedLogId() {
        return currentRecordedLogId;
    }

    /**
     * Getter for the logging info.
     * 
     * @return true if the logger is active and recording points into the database.
     */
    public boolean isDatabaseLogging() {
        return isDatabaseLogging;
    }

    /**
     * @return <code>true</code> only if the gps thread has finished.
     */
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Starts logging into the database.
     * 
     * @param logName a name for the new log or <code>null</code>.
     * @param dbHelper the db helper.
     */
    public void startDatabaseLogging( final String logName, final IGpsLogDbHelper dbHelper ) {
        if (isDatabaseLogging) {
            // we do not start twice
            return;
        }
        isDatabaseLogging = true;

        Thread t = new Thread(){
            public void run() {
                try {
                    isShutdown = false;

                    SQLiteDatabase sqliteDatabase = dbHelper.getDatabase(context);
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    long gpsLogId = dbHelper.addGpsLog(context, now, now, 0, logName, 2f, "red", true);
                    currentRecordedLogId = gpsLogId;
                    logH("Starting gps logging. Logid: " + gpsLogId);

                    // get preferences
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
                    logH("Waiting interval: " + waitForSecs);

                    currentPointsNum = 0;
                    currentDistance = 0;
                    previousLogLoc = null;
                    while( isDatabaseLogging ) {
                        if (gotFix || isMockMode) {
                            if (gpsLoc == null) {
                                if (!holdABitAndCheckLogging(waitForSecs)) {
                                    break;
                                }
                                continue;
                            }
                            if (previousLogLoc == null) {
                                previousLogLoc = gpsLoc;
                            }
                            double recLon = gpsLoc.getLongitude();
                            double recLat = gpsLoc.getLatitude();
                            double recAlt = gpsLoc.getAltitude();
                            double lastDistance = previousLogLoc.distanceTo(gpsLoc);
                            logABS("gpsloc: " + gpsLoc.getLatitude() + "/" + gpsLoc.getLongitude());
                            logABS("previousLoc: " + previousLogLoc.getLatitude() + "/" + previousLogLoc.getLongitude());
                            logABS("distance: " + lastDistance + " - mindistance: " + minDistance);
                            // ignore near points
                            if (lastDistance < minDistance) {
                                if (!holdABitAndCheckLogging(waitForSecs)) {
                                    break;
                                }
                                continue;
                            }
                            try {
                                if (isDatabaseLogging) {
                                    dbHelper.addGpsLogDataPoint(sqliteDatabase, gpsLogId, recLon, recLat, recAlt,
                                            gpsLoc.getSqlDate());
                                }
                            } catch (Exception e) {
                                // we log the exception and try to go on
                                GPLog.error(this, "Point in db writing error!", e);
                            }
                            currentPointsNum++;
                            currentDistance = currentDistance + lastDistance;
                            previousLogLoc = gpsLoc;
                        }
                        if (!holdABitAndCheckLogging(waitForSecs)) {
                            break;
                        }
                    }

                    if (currentPointsNum < 4) {
                        logH("Removing gpslog, since too few points were added. Logid: " + gpsLogId);
                        dbHelper.deleteGpslog(context, gpsLogId);
                    } else {
                        // set the end time stamp and the total distance for the track
                        java.sql.Date end = new java.sql.Date(System.currentTimeMillis());
                        dbHelper.setEndTs(context, gpsLogId, end);
                        dbHelper.setTrackLengthm(context, gpsLogId, (double) currentDistance);
                    }

                    currentPointsNum = 0;
                    currentDistance = 0;
                    currentRecordedLogId = -1;

                } catch (SQLiteFullException e) {
                    e.printStackTrace();
                    String msg = context.getResources().getString(R.string.error_disk_full);
                    GPLog.error(this, msg, e);
                    Utilities.toast(context, msg, Toast.LENGTH_LONG);
                } catch (Exception e) {
                    e.printStackTrace();
                    String msg = context.getResources().getString(R.string.cantwrite_gpslog);
                    GPLog.error(this, msg, e);
                    Utilities.toast(context, msg, Toast.LENGTH_LONG);
                } finally {
                    isDatabaseLogging = false;
                    isShutdown = true;
                }
                logABS("Exit logging...");

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
                    String msg = context.getResources().getString(R.string.cantwrite_gpslog);
                    GPLog.error(this, msg, e);
                    return true;
                }
            }
        };
        t.start();

        Utilities.toast(context, R.string.gpsloggingon, Toast.LENGTH_SHORT);
    }
    /**
     * Stop logging.
     */
    public void stopDatabaseLogging() {
        isDatabaseLogging = false;
        Utilities.toast(context, R.string.gpsloggingoff, Toast.LENGTH_SHORT);
    }

    /**
     * @return the current points num of the log.
     */
    public int getCurrentPointsNum() {
        return currentPointsNum;
    }

    /**
     * @return the current distance rounded to meters.
     */
    public int getCurrentDistance() {
        return (int) Math.round(currentDistance);
    }

    public void onLocationChanged( Location location ) {
        if (location == null) {
            return;
        }
        lastLocationupdateMillis = SystemClock.elapsedRealtime();

        gpsLoc = new GpsLocation(location);
    }

    public void onStatusChanged( String provider, int status, Bundle extras ) {
        // ignore
    }

    public void onProviderEnabled( String provider ) {
        // ignore
    }

    public void onProviderDisabled( String provider ) {
        // ignore
    }

    public void gpsStart() {
        gotFix = false;
        logH("gpsStart called");
    }

    public void gpsStop() {
        lastLocationupdateMillis = 0;
        gotFix = false;
        logH("gpsStop called");
    }

    public void onGpsStatusChanged( int event, GpsStatus status ) {
        // check fix
        boolean tmpGotFix = GpsStatusInfo.checkFix(gotFix, lastLocationupdateMillis, event);
        if (!tmpGotFix) {
            // check if it is just standing still
            GpsStatusInfo info = new GpsStatusInfo(status);
            int satForFixCount = info.getSatUsedInFixCount();
            if (satForFixCount > 2) {
                tmpGotFix = true;
                // updating loc update, assuming the still filter is giving troubles
                lastLocationupdateMillis = SystemClock.elapsedRealtime();
            }
        }
        gotFix = tmpGotFix;
    }

    private void logH( String msg ) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, null, null, msg);
    }
    private void logABS( String msg ) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, null, null, msg);
    }

    public boolean hasFix() {
        return gotFix;
    }

    // /////////////////////////////////////////////////
    // SOUND HANDLING
    // /////////////////////////////////////////////////

    // private Handler alertDialogHandler = new Handler(){
    //
    // public void handleMessage( android.os.Message msg ) {
    // mMediaPlayer = new MediaPlayer();
    // doPlayAlarm = true;
    // AlertDialog.Builder builder = new AlertDialog.Builder(context);
    // String ok = context.getResources().getString(R.string.ok);
    // builder.setMessage(R.string.gpsloggingalarm).setCancelable(false)
    // .setPositiveButton(ok, new DialogInterface.OnClickListener(){
    // public void onClick( DialogInterface dialog, int id ) {
    // doPlayAlarm = false;
    // if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
    // mMediaPlayer.stop();
    // mMediaPlayer = null;
    // }
    // }
    // });
    // AlertDialog alertDialog = builder.create();
    // alertDialog.show();
    // };
    // };

    // private Handler alertSoundHandler = new Handler(){
    // public void handleMessage( android.os.Message msg ) {
    // try {
    // if (doPlayAlarm == false) {
    // return;
    // }
    // Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    // mMediaPlayer.setDataSource(context, alert);
    // final AudioManager audioManager = (AudioManager)
    // context.getSystemService(Context.AUDIO_SERVICE);
    // if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
    // mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
    // mMediaPlayer.setLooping(true);
    // mMediaPlayer.prepare();
    // mMediaPlayer.start();
    // }
    // } catch (Exception e) {
    // Logger.e(this, e.getLocalizedMessage(), e);
    // e.printStackTrace();
    // }
    // };
    // };

    // private void playAlert() {
    // try {
    // alertDialogHandler.sendEmptyMessage(0);
    // int index = 0;
    // while( index < 3 ) {
    // Thread.sleep(1000);
    // index++;
    // }
    // alertSoundHandler.sendEmptyMessage(0);
    // } catch (InterruptedException e) {
    // Logger.e(this, e.getLocalizedMessage(), e);
    // e.printStackTrace();
    // }
    // }

}
