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

import static eu.hydrologis.geopaparazzi.util.Constants.DECIMATION_FACTOR;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_KMLEXPORT;
import static eu.hydrologis.geopaparazzi.util.Constants.PATH_MEDIA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import eu.geopaparazzi.library.camera.CameraActivity;
import eu.geopaparazzi.library.gps.GpsLocation;
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.maps.DataManager;

/**
 * Singleton that takes care of all the sensors and gps and loggings.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ApplicationManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private Context context;

    private File databaseFile;
    private File geoPaparazziDir;
    private File mediaDir;
    private File kmlExportDir;

    private File debugLogFile;

    private ResourcesManager resourcesManager;
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
     * @return
     */
    public static ApplicationManager getInstance( Context context ) {
        if (applicationManager == null) {
            try {
                applicationManager = new ApplicationManager(context);
            } catch (IOException e) {
                return null;
            }
        }
        return applicationManager;
    }

    public static void resetManager() {
        applicationManager = null;
    }

    public Context getContext() {
        return context;
    }

    private ApplicationManager( Context context ) throws IOException {
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
         */
        resourcesManager = ResourcesManager.getInstance(context);
        geoPaparazziDir = resourcesManager.getApplicationDir();
        databaseFile = resourcesManager.getDatabaseFile();
        mediaDir = new File(geoPaparazziDir, PATH_MEDIA);
        if (!mediaDir.exists())
            if (!mediaDir.mkdir())
                Utilities.messageDialog(
                        context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                                mediaDir.getAbsolutePath()), null);
        debugLogFile = resourcesManager.getDebugLogFile();

        kmlExportDir = new File(geoPaparazziDir, PATH_KMLEXPORT);
        if (!kmlExportDir.exists())
            if (!kmlExportDir.mkdir())
                Utilities.messageDialog(
                        context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard),
                                kmlExportDir.getAbsolutePath()), null);

    }

    public File getGeoPaparazziDir() {
        return geoPaparazziDir;
    }

    public File getDatabaseFile() {
        return databaseFile;
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

    public ActionItem getNotesQuickAction( final QuickAction qa ) {
        ActionItem notesQuickaction = new ActionItem();
        notesQuickaction.setTitle("Geonote"); //$NON-NLS-1$
        notesQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_notes));
        notesQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                GpsLocation loc = GpsManager.getInstance(context).getLocation();
                if (loc != null) {
                    // TODO check if this is better than having the simple notes upfront
                    // Intent intent = new Intent(Constants.TAKE_NOTE);
                    // context.startActivity(intent);

                    double latitude = loc.getLatitude();
                    double longitude = loc.getLongitude();
                    Intent osmTagsIntent = new Intent(Constants.TAGS);
                    osmTagsIntent.putExtra(Constants.PREFS_KEY_MAPCENTER_LAT, latitude);
                    osmTagsIntent.putExtra(Constants.PREFS_KEY_MAPCENTER_LON, longitude);
                    context.startActivity(osmTagsIntent);
                } else {
                    Utilities.messageDialog(context, R.string.gpslogging_only, null);
                }
                qa.dismiss();
            }
        });
        return notesQuickaction;
    }
    @SuppressWarnings("nls")
    public ActionItem getPicturesQuickAction( final QuickAction qa ) {
        ActionItem pictureQuickaction = new ActionItem();
        pictureQuickaction.setTitle("Photo");
        pictureQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_pictures));
        pictureQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                try {
                    if (Debug.D)
                        Logger.d(this, "Asking location");
                    GpsLocation loc = GpsManager.getInstance(context).getLocation();
                    if (loc != null) {
                        if (Debug.D)
                            Logger.d(this, "Location != null");
                        Intent intent = new Intent(context, CameraActivity.class);
                        context.startActivity(intent);
                    } else {
                        if (Debug.D)
                            Logger.d(this, "Location == null");
                        Utilities.messageDialog(context, R.string.gpslogging_only, null);
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

    @SuppressWarnings("nls")
    public ActionItem getAudioQuickAction( final QuickAction qa ) {
        ActionItem audioQuickaction = new ActionItem();
        audioQuickaction.setTitle("Audio");
        audioQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_audio));
        audioQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                try {
                    GpsLocation loc = GpsManager.getInstance(context).getLocation();
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
                        Utilities.messageDialog(context, R.string.gpslogging_only, null);
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
        startLogQuickaction.setTitle("Start Log"); //$NON-NLS-1$
        startLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_start_log));
        startLogQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                final GpsManager gpsManager = GpsManager.getInstance(context);
                if (!gpsManager.isGpsLogging()) {
                    GpsLocation loc = gpsManager.getLocation();
                    if (loc != null) {
                        final String defaultLogName = "log_" + Constants.TIMESTAMPFORMATTER.format(new Date()); //$NON-NLS-1$
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

                                        DaoGpsLog daoGpsLog = new DaoGpsLog();
                                        gpsManager.startLogging(newName, daoGpsLog);
                                        actionBar.checkLogging();
                                        DataManager.getInstance().setLogsVisible(true);
                                    }
                                }).setCancelable(false).show();
                    } else {
                        Utilities.messageDialog(context, R.string.gpslogging_only, null);
                    }
                }
                qa.dismiss();
            }
        });
        return startLogQuickaction;
    }

    public ActionItem getStopLogQuickAction( final ActionBar actionBar, final QuickAction qa ) {
        ActionItem stopLogQuickaction = new ActionItem();
        stopLogQuickaction.setTitle("Stop Log"); //$NON-NLS-1$
        stopLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_stop_log));
        stopLogQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                GpsManager gpsManager = GpsManager.getInstance(context);
                if (gpsManager.isGpsLogging()) {
                    gpsManager.stopLogging();
                    actionBar.checkLogging();
                }
                qa.dismiss();
            }
        });
        return stopLogQuickaction;
    }

    public int getDecimationFactor() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String decimationFactorStr = preferences.getString(DECIMATION_FACTOR, "5"); //$NON-NLS-1$
        int decimationFactor = 5;
        try {
            decimationFactor = Integer.parseInt(decimationFactorStr);
        } catch (Exception e) {
            // use default
        }
        return decimationFactor;
    }

}
