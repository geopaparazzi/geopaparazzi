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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
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
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.NoteActivity;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.maps.DataManager;

/**
 * A factory for quick actions.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum QuickActionsFactory {
    INSTANCE;

    /**
     * Create a {@link QuickAction} for notes collection.
     * 
     * @param qa the {@link QuickAction} to attache the {@link ActionItem} to.
     * @param activity the context to use.
     * @return the {@link ActionItem} created.
     */
    public ActionItem getNotesQuickAction( final QuickAction qa, final Activity activity, final int requestCode ) {
        ActionItem notesQuickaction = new ActionItem();
        notesQuickaction.setTitle("Geonote"); //$NON-NLS-1$
        notesQuickaction.setIcon(activity.getResources().getDrawable(R.drawable.quickaction_notes));
        notesQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                boolean isValid = false;
                if (GpsManager.getInstance(activity).hasValidData()) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
                    if (gpsLocation != null) {
                        Intent noteIntent = new Intent(activity, NoteActivity.class);
                        noteIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
                        noteIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
                        noteIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
                        activity.startActivityForResult(noteIntent, requestCode);
                        isValid = true;
                    }
                }
                if (!isValid)
                    Utilities.messageDialog(activity, R.string.gpslogging_only, null);
                qa.dismiss();
            }
        });
        return notesQuickaction;
    }

    /**
     * Create a {@link QuickAction} for pictures collection.
     * 
     * @param qa the {@link QuickAction} to attache the {@link ActionItem} to.
     * @param activity the context to use.
     * @return the {@link ActionItem} created.
     */
    @SuppressWarnings("nls")
    public ActionItem getPicturesQuickAction( final QuickAction qa, final Activity activity, final int requestCode ) {
        ActionItem pictureQuickaction = new ActionItem();
        pictureQuickaction.setTitle("Photo");
        pictureQuickaction.setIcon(activity.getResources().getDrawable(R.drawable.quickaction_pictures));
        pictureQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                try {
                    boolean isValid = false;
                    if (GpsManager.getInstance(activity).hasValidData()) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                        double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
                        if (gpsLocation != null) {
                            Intent cameraIntent = new Intent(activity, CameraActivity.class);
                            cameraIntent.putExtra(LibraryConstants.LATITUDE, gpsLocation[1]);
                            cameraIntent.putExtra(LibraryConstants.LONGITUDE, gpsLocation[0]);
                            cameraIntent.putExtra(LibraryConstants.ELEVATION, gpsLocation[2]);
                            activity.startActivityForResult(cameraIntent, requestCode);
                            isValid = true;
                        }
                    }
                    if (!isValid)
                        Utilities.messageDialog(activity, R.string.gpslogging_only, null);
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
    public ActionItem getAudioQuickAction( final QuickAction qa, final Context context, final File mediaDir ) {
        ActionItem audioQuickaction = new ActionItem();
        audioQuickaction.setTitle("Audio");
        audioQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_audio));
        audioQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                try {
                    boolean isValid = false;
                    if (GpsManager.getInstance(context).hasValidData()) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
                        if (gpsLocation != null) {
                            String latString = String.valueOf(gpsLocation[1]);
                            String lonString = String.valueOf(gpsLocation[0]);
                            String altimString = String.valueOf(gpsLocation[2]);

                            if (audioRecorder == null) {
                                audioRecorder = new MediaRecorder();
                            }
                            final String currentDatestring = LibraryConstants.TIMESTAMPFORMATTER.format(new Date());
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
                            isValid = true;
                        }
                    }
                    if (!isValid)
                        Utilities.messageDialog(context, R.string.gpslogging_only, null);
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                }
                qa.dismiss();
            }
        });
        return audioQuickaction;
    }

    public ActionItem getStartLogQuickAction( final ActionBar actionBar, final QuickAction qa, final Context context ) {
        ActionItem startLogQuickaction = new ActionItem();
        startLogQuickaction.setTitle("Start Log"); //$NON-NLS-1$
        startLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_start_log));
        startLogQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                final GpsManager gpsManager = GpsManager.getInstance(context);
                if (gpsManager.hasValidData()) {
                    final String defaultLogName = "log_" + LibraryConstants.TIMESTAMPFORMATTER.format(new Date()); //$NON-NLS-1$
                    final EditText input = new EditText(context);
                    input.setText(defaultLogName);
                    new AlertDialog.Builder(context).setTitle(R.string.gps_log).setMessage(R.string.gps_log_name).setView(input)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
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
                qa.dismiss();
            }
        });
        return startLogQuickaction;
    }

    public ActionItem getStopLogQuickAction( final ActionBar actionBar, final QuickAction qa, final Context context ) {
        ActionItem stopLogQuickaction = new ActionItem();
        stopLogQuickaction.setTitle("Stop Log"); //$NON-NLS-1$
        stopLogQuickaction.setIcon(context.getResources().getDrawable(R.drawable.quickaction_stop_log));
        stopLogQuickaction.setOnClickListener(new OnClickListener(){
            public void onClick( View v ) {
                GpsManager gpsManager = GpsManager.getInstance(context);
                if (gpsManager.isLogging()) {
                    gpsManager.stopLogging();
                    actionBar.checkLogging();
                }
                qa.dismiss();
            }
        });
        return stopLogQuickaction;
    }
}
