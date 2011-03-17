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
package eu.hydrologis.geopaparazzi.gps;

import static eu.hydrologis.geopaparazzi.util.Constants.GPSLAST_LATITUDE;
import static eu.hydrologis.geopaparazzi.util.Constants.GPSLAST_LONGITUDE;
import static eu.hydrologis.geopaparazzi.util.Constants.GPSLOGGINGDISTANCEKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.GPSLOGGINGINTERVALKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.GPS_LOGGING_DISTANCE;
import static eu.hydrologis.geopaparazzi.util.Constants.GPS_LOGGING_INTERVAL;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The Gps engine, used to put logs into the database.
 * 
 * <p>This class takes care to make the logging occur at preferences settings.
 * That is why it is not listening directly to the gps, but instead to the gps manager.
 * It is the manager that updates the position.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsLogger implements GpsManagerListener {
    private static final String LOGTAG = "GPSLOGGER";

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

    private boolean isLogging = false;

    private MediaPlayer mMediaPlayer;
    private boolean doPlayAlarm = false;

    private int currentPointsNum;
    private float currentDistance;

    public GpsLogger( Context context ) {
        this.context = context;
    }

    /**
     * Getter for the logging info.
     * 
     * @return true if the logger is active and recording points into the database.
     */
    public boolean isLogging() {
        return isLogging;
    }

    private long currentRecordedLogId = -1;
    public long getCurrentRecordedLogId() {
        return currentRecordedLogId;
    }

    /**
     * Starts logging into the database.
     * 
     * @param logName a name for the new log or <code>null</code>.
     */
    public void startLogging( final String logName ) {
        isLogging = true;

        Thread t = new Thread(){

            public void run() {
                try {
                    java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
                    long gpsLogId = DaoGpsLog.addGpsLog(context, now, now, logName, 2f, "red", true);
                    currentRecordedLogId = gpsLogId;
                    Logger.i(LOGTAG, "Starting gps logging. Logid: " + gpsLogId);

                    // get preferences
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                    String minDistanceStr = preferences.getString(GPSLOGGINGDISTANCEKEY, String.valueOf(GPS_LOGGING_DISTANCE));
                    float minDistance = Float.parseFloat(minDistanceStr);
                    String intervalStr = preferences.getString(GPSLOGGINGINTERVALKEY, String.valueOf(GPS_LOGGING_INTERVAL));
                    long waitForSecs = Long.parseLong(intervalStr);
                    Logger.d(LOGTAG, "Waiting interval: " + waitForSecs);

                    currentPointsNum = 0;
                    currentDistance = 0;
                    previousLogLoc = null;
                    while( isLogging ) {
                        if (gpsLoc == null) {
                            waitGpsInterval(waitForSecs);
                            continue;
                        }
                        if (previousLogLoc == null) {
                            previousLogLoc = gpsLoc;
                        }

                        float lastDistance = previousLogLoc.distanceTo(gpsLoc);
                        Logger.d(LOGTAG, "gpsloc: " + gpsLoc.getLatitude() + "/" + gpsLoc.getLongitude());
                        Logger.d(LOGTAG, "previousLoc: " + previousLogLoc.getLatitude() + "/" + previousLogLoc.getLongitude());
                        Logger.d(LOGTAG, "distance: " + lastDistance + " - mindistance: " + minDistance);
                        // ignore near points
                        if (lastDistance < minDistance) {
                            waitGpsInterval(waitForSecs);
                            continue;
                        }

                        StringBuilder sB = new StringBuilder();
                        double recLon = gpsLoc.getLongitude();
                        double recLat = gpsLoc.getLatitude();
                        double recAlt = gpsLoc.getAltitude();
                        String timeStringSql = gpsLoc.getTimeStringSql();
                        sB.append(recLon).append(",");
                        sB.append(recLat).append(",");
                        sB.append(recAlt).append(",");
                        sB.append(timeStringSql);
                        sB.append("\n");

                        SQLiteDatabase sqliteDatabase = DatabaseManager.getInstance().getDatabase(context);
                        sqliteDatabase.beginTransaction();
                        try {
                            DaoGpsLog.addGpsLogDataPoint(sqliteDatabase, gpsLogId, recLon, recLat, recAlt, gpsLoc.getSqlDate());
                            sqliteDatabase.setTransactionSuccessful();
                        } catch (Exception e) {
                            Logger.e(this, e.getLocalizedMessage(), e);
                            throw new IOException(e.getLocalizedMessage());
                        } finally {
                            sqliteDatabase.endTransaction();
                        }
                        currentPointsNum++;
                        currentDistance = currentDistance + lastDistance;

                        previousLogLoc = gpsLoc;

                        // save last known location
                        Editor editor = preferences.edit();
                        editor.putFloat(GPSLAST_LONGITUDE, (float) gpsLoc.getLongitude());
                        editor.putFloat(GPSLAST_LATITUDE, (float) gpsLoc.getLatitude());
                        editor.commit();

                        // and wait
                        waitGpsInterval(waitForSecs);
                    }

                    if (currentPointsNum < 2) {
                        Logger.i(LOGTAG, "Removing gpslog, since too few points were added. Logid: " + gpsLogId);
                        DaoGpsLog.deleteGpslog(context, gpsLogId);
                    } else {
                        // set the end timestamp
                        java.sql.Date end = new java.sql.Date(System.currentTimeMillis());
                        DaoGpsLog.setEndTs(context, gpsLogId, end);
                    }

                    currentPointsNum = 0;
                    currentDistance = 0f;
                    currentRecordedLogId = -1;

                } catch (SQLiteFullException e) {
                    e.printStackTrace();
                    String msg = context.getResources().getString(R.string.error_disk_full);
                    Logger.e(this, msg, e);
                    // ApplicationManager.getInstance(getContext()).alertDialog(msg);
                    // FIXME
                    // Toasts.longAsyncToast(context, msg);
                    // Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    isLogging = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    String msg = context.getResources().getString(R.string.cantwrite_gpslog);
                    Logger.e(this, msg, e);
                    // FIXME
                    // Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    playAlert();
                    isLogging = false;
                }
                Logger.d(this, "Exit logging...");
            }

            private void waitGpsInterval( long waitForSecs ) {
                try {
                    // get interval and wait
                    Thread.sleep(waitForSecs * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    String msg = context.getResources().getString(R.string.cantwrite_gpslog);
                    Logger.e(this, msg, e);
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            }
        };
        t.start();

        Toast.makeText(context, R.string.gpsloggingon, Toast.LENGTH_SHORT).show();
    }

    public void stopLogging() {
        isLogging = false;
        Toast.makeText(context, R.string.gpsloggingoff, Toast.LENGTH_SHORT).show();
    }

    public int getCurrentPointsNum() {
        return currentPointsNum;
    }

    public int getCurrentDistance() {
        return (int) currentDistance;
    }

    // /////////////////////////////////////////////////
    // SOUND HANDLING
    // /////////////////////////////////////////////////

    private Handler alertDialogHandler = new Handler(){

        public void handleMessage( android.os.Message msg ) {
            mMediaPlayer = new MediaPlayer();
            doPlayAlarm = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            String ok = context.getResources().getString(R.string.ok);
            builder.setMessage(R.string.gpsloggingalarm).setCancelable(false)
                    .setPositiveButton(ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            doPlayAlarm = false;
                            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                                mMediaPlayer.stop();
                                mMediaPlayer = null;
                            }
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        };
    };

    private Handler alertSoundHandler = new Handler(){
        public void handleMessage( android.os.Message msg ) {
            try {
                if (doPlayAlarm == false) {
                    return;
                }
                Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mMediaPlayer.setDataSource(context, alert);
                final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mMediaPlayer.setLooping(true);
                    mMediaPlayer.prepare();
                    mMediaPlayer.start();
                }
            } catch (Exception e) {
                Logger.e(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }
        };
    };

    private void playAlert() {
        try {
            alertDialogHandler.sendEmptyMessage(0);
            int index = 0;
            while( index < 3 ) {
                Thread.sleep(1000);
                index++;
            }
            alertSoundHandler.sendEmptyMessage(0);
        } catch (InterruptedException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }
    }

    public void onLocationChanged( GpsLocation loc ) {
        gpsLoc = new GpsLocation(loc);
    }

    public void onStatusChanged( boolean hasFix ) {
    }

}
