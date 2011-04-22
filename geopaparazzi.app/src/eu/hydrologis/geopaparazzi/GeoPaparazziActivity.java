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
package eu.hydrologis.geopaparazzi;

import static eu.hydrologis.geopaparazzi.util.Constants.BASEFOLDERKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.PANICKEY;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_LAT;
import static eu.hydrologis.geopaparazzi.util.Constants.PREFS_KEY_LON;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.gps.GpsLocation;
import eu.hydrologis.geopaparazzi.gps.GpsManager;
import eu.hydrologis.geopaparazzi.kml.KmlExport;
import eu.hydrologis.geopaparazzi.maps.DataManager;
import eu.hydrologis.geopaparazzi.maps.MapItem;
import eu.hydrologis.geopaparazzi.sensors.SensorsManager;
import eu.hydrologis.geopaparazzi.util.ApplicationManager;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.DirectoryBrowserActivity;
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.Picture;
import eu.hydrologis.geopaparazzi.util.debug.Debug;
import eu.hydrologis.geopaparazzi.util.debug.Logger;

/**
 * The main {@link Activity activity} of GeoPaparazzi.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoPaparazziActivity extends Activity {

    private static final int MENU_ABOUT = Menu.FIRST;
    private static final int MENU_EXIT = 2;
    private static final int MENU_SETTINGS = 3;
    private static final int MENU_RESET = 4;
    private static final int MENU_LOAD = 5;

    private ApplicationManager applicationManager;
    private ActionBar actionBar;
    private ProgressDialog kmlProgressDialog;

    private File kmlOutputFile = null;
    private static final int BROWSERRETURNCODE = 666;

    private boolean sliderIsOpen = false;
    private GpsManager gpsManager;
    private SensorsManager sensorManager;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        init();
    }

    protected void onResume() {
        super.onResume();
        checkActionBar();
    }

    public void onWindowFocusChanged( boolean hasFocus ) {
        super.onWindowFocusChanged(hasFocus);
        checkActionBar();
    }

    private void checkActionBar() {
        checkDebugLogger();

        if (actionBar == null) {
            actionBar = ActionBar.getActionBar(this, R.id.action_bar, applicationManager, gpsManager, sensorManager);
            actionBar.setTitle(R.string.app_name, R.id.action_bar_title);
        }
        actionBar.checkLogging();

    }

    private void init() {
        setContentView(R.layout.geopap_main);

        gpsManager = GpsManager.getInstance(this);
        sensorManager = SensorsManager.getInstance(this);

        Object stateObj = getLastNonConfigurationInstance();
        if (stateObj instanceof ApplicationManager) {
            applicationManager = (ApplicationManager) stateObj;
        } else {
            ApplicationManager.resetManager();
            applicationManager = ApplicationManager.getInstance(this);
        }
        if (applicationManager == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String ok = getResources().getString(R.string.ok);
            builder.setMessage(R.string.sdcard_notexist).setCancelable(false)
                    .setPositiveButton(ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }

        checkActionBar();

        /*
         * the buttons
         */
        final int notesButtonId = R.id.dashboard_note_item_button;
        ImageButton notesButton = (ImageButton) findViewById(notesButtonId);
        notesButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(notesButtonId, v);
            }
        });

        final int undoNotesButtonId = R.id.dashboard_undonote_item_button;
        ImageButton undoNotesButton = (ImageButton) findViewById(undoNotesButtonId);
        undoNotesButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(undoNotesButtonId, v);
            }
        });

        final int logButtonId = R.id.dashboard_log_item_button;
        ImageButton logButton = (ImageButton) findViewById(logButtonId);
        // isChecked = applicationManager.isGpsLogging();
        logButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(logButtonId, v);
            }
        });

        final int mapButtonId = R.id.dashboard_map_item_button;
        ImageButton mapButton = (ImageButton) findViewById(mapButtonId);
        mapButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(mapButtonId, v);
            }
        });

        final int importButtonId = R.id.dashboard_import_item_button;
        ImageButton importButton = (ImageButton) findViewById(importButtonId);
        importButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(importButtonId, v);
            }
        });

        final int exportButtonId = R.id.dashboard_export_item_button;
        ImageButton exportButton = (ImageButton) findViewById(exportButtonId);
        exportButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(exportButtonId, v);
            }
        });

        // slidingdrawer
        final int slidingId = R.id.slide;
        slidingDrawer = (SlidingDrawer) findViewById(slidingId);
        slidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener(){
            public void onDrawerOpened() {
                sliderIsOpen = true;
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener(){
            public void onDrawerClosed() {
                sliderIsOpen = false;
            }
        });

        // panic buttons part
        final int panicButtonId = R.id.panicbutton;
        Button panicButton = (Button) findViewById(panicButtonId);
        panicButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(panicButtonId, v);
            }
        });
        final int statusUpdateButtonId = R.id.statusupdatebutton;
        Button statusUpdateButton = (Button) findViewById(statusUpdateButtonId);
        statusUpdateButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(statusUpdateButtonId, v);
            }
        });

        try {
            DatabaseManager.getInstance().getDatabase(this);
            checkMapsAndLogsVisibility();
        } catch (IOException e) {
            Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, R.string.databaseError, Toast.LENGTH_LONG).show();
        }
    }

    private void checkDebugLogger() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(R.string.enable_debug);
        boolean logToFile = preferences.getBoolean(key, false);
        if (logToFile) {
            File debugLogFile = applicationManager.getDebugLogFile();
            new Logger(debugLogFile);
        } else {
            new Logger(null);
        }

    }

    public void push( int id, View v ) {
        switch( id ) {
        case R.id.dashboard_note_item_button: {
            QuickAction qa = new QuickAction(v);
            qa.addActionItem(applicationManager.getNotesQuickAction(qa));
            qa.addActionItem(applicationManager.getPicturesQuickAction(qa));
            qa.addActionItem(applicationManager.getAudioQuickAction(qa));
            qa.setAnimStyle(QuickAction.ANIM_AUTO);
            qa.show();
            break;
        }
        case R.id.dashboard_undonote_item_button: {
            new AlertDialog.Builder(this).setTitle(R.string.text_undo_a_gps_note).setMessage(R.string.remove_last_note_prompt)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                        }
                    }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                            try {
                                DaoNotes.deleteLastInsertedNote(GeoPaparazziActivity.this);
                                Toast.makeText(GeoPaparazziActivity.this, R.string.last_note_deleted, Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                Logger.e(this, e.getLocalizedMessage(), e);
                                e.printStackTrace();
                                Toast.makeText(GeoPaparazziActivity.this, R.string.last_note_not_deleted, Toast.LENGTH_LONG)
                                        .show();
                            }
                        }
                    }).show();

            break;
        }
        case R.id.dashboard_log_item_button: {
            QuickAction qa = new QuickAction(v);
            if (gpsManager.isGpsLogging()) {
                ActionItem stopLogQuickAction = applicationManager.getStopLogQuickAction(actionBar, qa);
                qa.addActionItem(stopLogQuickAction);
            } else {
                ActionItem startLogQuickAction = applicationManager.getStartLogQuickAction(actionBar, qa);
                qa.addActionItem(startLogQuickAction);
            }
            qa.setAnimStyle(QuickAction.ANIM_AUTO);
            qa.show();
            break;
        }
        case R.id.dashboard_map_item_button: {
            Intent mapIntent = new Intent(Constants.MAP_VIEW);
            startActivity(mapIntent);
            break;
        }
        case R.id.dashboard_import_item_button: {
            Intent browseIntent = new Intent(Constants.DIRECTORYBROWSER);
            browseIntent.putExtra(Constants.INTENT_ID, Constants.GPXIMPORT);
            browseIntent.putExtra(Constants.EXTENTION, ".gpx"); //$NON-NLS-1$
            startActivity(browseIntent);
            break;
        }
        case R.id.dashboard_export_item_button: {
            new AlertDialog.Builder(this).setTitle(R.string.export_for_real).setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                        }
                    }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                            exportToKml();
                        }
                    }).show();

            break;
        }
        case R.id.panicbutton: {
            sendPosition(true);
            break;
        }
        case R.id.statusupdatebutton: {
            sendPosition(false);
            break;
        }
        default:
            break;
        }
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.mainmenu_preferences).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, MENU_RESET, 1, R.string.reset).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(Menu.NONE, MENU_LOAD, 2, R.string.load).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(Menu.NONE, MENU_EXIT, 3, R.string.exit).setIcon(android.R.drawable.ic_lock_power_off);
        menu.add(Menu.NONE, MENU_ABOUT, 4, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);

        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_ABOUT:
            Intent intent = new Intent(Constants.ABOUT);
            startActivity(intent);
            return true;
        case MENU_SETTINGS:
            Intent preferencesIntent = new Intent(Constants.PREFERENCES);
            startActivity(preferencesIntent);
            return true;
        case MENU_RESET:
            resetData();
            return true;
        case MENU_LOAD:
            Intent browseIntent = new Intent(Constants.DIRECTORYBROWSER);
            browseIntent.putExtra(Constants.EXTENTION, DirectoryBrowserActivity.FOLDER);
            startActivityForResult(browseIntent, BROWSERRETURNCODE);
            return true;
        case MENU_EXIT:
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (BROWSERRETURNCODE): {
            if (resultCode == Activity.RESULT_OK) {
                String chosenFolderToLoad = data.getStringExtra(Constants.PATH);
                if (chosenFolderToLoad != null && new File(chosenFolderToLoad).getParentFile().exists()) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    Editor editor = preferences.edit();
                    editor.putString(BASEFOLDERKEY, chosenFolderToLoad);
                    editor.commit();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
            break;
        }
        }
    }

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (sliderIsOpen) {
                slidingDrawer.animateClose();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void finish() {
        if (Debug.D) Logger.d(this, "Finish called!"); //$NON-NLS-1$
        // save last location just in case
        GpsLocation loc = gpsManager.getLocation();
        if (loc != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Editor editor = preferences.edit();
            editor.putFloat(PREFS_KEY_LON, (float) loc.getLongitude());
            editor.putFloat(PREFS_KEY_LAT, (float) loc.getLatitude());
            editor.commit();
        }

        Toast.makeText(this, R.string.loggingoff, Toast.LENGTH_LONG).show();
        // stop all logging
        gpsManager.stopListening();
        gpsManager.stopLogging();
        DatabaseManager.getInstance().closeDatabase();
        applicationManager = null;
        super.finish();
    }

    private boolean doRename = false;
    private void resetData() {

        File geoPaparazziDir = applicationManager.getGeoPaparazziDir();
        String name = geoPaparazziDir.getName();
        doRename = false;
        if (name.equals(Constants.GEOPAPARAZZI)) {
            doRename = true;
        }
        final String defaultLogName = Constants.GEOPAPARAZZI + "_" + Constants.TIMESTAMPFORMATTER.format(new Date()); //$NON-NLS-1$
        final EditText input = new EditText(this);
        input.setText(defaultLogName);
        Builder builder = new AlertDialog.Builder(this).setTitle(R.string.reset);
        if (doRename) {
            builder.setMessage(R.string.reset_prompt);
            builder.setView(input);
        }
        builder.setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        try {
                            SharedPreferences preferences = PreferenceManager
                                    .getDefaultSharedPreferences(getApplicationContext());
                            Editor editor = preferences.edit();
                            if (doRename) {
                                Editable value = input.getText();
                                String newName = value.toString();
                                if (newName == null || newName.length() < 1) {
                                    newName = defaultLogName;
                                }
                                File geopaparazziDirFile = applicationManager.getGeoPaparazziDir();
                                DatabaseManager.getInstance().closeDatabase();
                                File geopaparazziParentFile = geopaparazziDirFile.getParentFile();
                                File newGeopaparazziDirFile = new File(geopaparazziParentFile.getAbsolutePath(), newName);
                                if (!geopaparazziDirFile.renameTo(newGeopaparazziDirFile)) {
                                    throw new IOException("Unable to rename the geopaparazzi folder."); //$NON-NLS-1$
                                }
                                // editor.putString(BASEFOLDERKEY,
                                // newGeopaparazziDirFile.getAbsolutePath());
                            }
                            editor.putString(BASEFOLDERKEY, ""); //$NON-NLS-1$
                            editor.commit();

                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        } catch (IOException e) {
                            Logger.e(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                            Toast.makeText(GeoPaparazziActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }).setCancelable(false).show();
    }

    private Handler kmlHandler = new Handler(){
        public void handleMessage( android.os.Message msg ) {
            kmlProgressDialog.dismiss();
            if (kmlOutputFile.exists()) {
                Toast.makeText(GeoPaparazziActivity.this, R.string.kmlsaved + kmlOutputFile.getAbsolutePath(), Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(GeoPaparazziActivity.this, R.string.kmlnonsaved, Toast.LENGTH_LONG).show();
            }
        };
    };
    private SlidingDrawer slidingDrawer;

    private void exportToKml() {

        kmlProgressDialog = ProgressDialog.show(this, getString(R.string.geopaparazziactivity_exporting_kmz), "", true, true); //$NON-NLS-1$
        new Thread(){

            public void run() {
                try {
                    /*
                     * add gps logs
                     */
                    HashMap<Long, Line> linesList = DaoGpsLog.getLinesMap(GeoPaparazziActivity.this);
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(GeoPaparazziActivity.this);
                    /*
                     * add pictures
                     */
                    List<Picture> picturesList = applicationManager.getPictures();

                    File kmlExportDir = applicationManager.getKmlExportDir();
                    String filename = "geopaparazzi_" + Constants.TIMESTAMPFORMATTER.format(new Date()) + ".kmz"; //$NON-NLS-1$ //$NON-NLS-2$
                    kmlOutputFile = new File(kmlExportDir, filename);
                    KmlExport export = new KmlExport(null, kmlOutputFile);
                    export.export(notesList, linesList, picturesList);

                    kmlHandler.sendEmptyMessage(0);
                } catch (IOException e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    Toast.makeText(GeoPaparazziActivity.this, R.string.kmlnonsaved, Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    private void checkMapsAndLogsVisibility() throws IOException {
        List<MapItem> maps = DaoMaps.getMaps(this);
        boolean oneVisible = false;
        for( MapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setMapsVisible(oneVisible);

        maps = DaoGpsLog.getGpslogs(this);
        oneVisible = false;
        for( MapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setLogsVisible(oneVisible);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return applicationManager;
    }

    /**
     * Send the panic or status update message.
     * 
     * @param doPanic make the panic message as opposed to just a status update.
     */
    @SuppressWarnings("nls")
    private void sendPosition( boolean doPanic ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String panicNumbersString = preferences.getString(PANICKEY, "");
        // Make sure there's a valid return address.

        if (panicNumbersString == null || panicNumbersString.length() == 0 || panicNumbersString.matches(".*[A-Za-z].*")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.panic_number_notset).setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            String[] numbers = panicNumbersString.split(";");
            for( String number : numbers ) {
                number = number.trim();
                if (number.length() == 0) {
                    continue;
                }
                GpsLocation loc = gpsManager.getLocation();
                if (loc != null) {
                    SmsManager mng = SmsManager.getDefault();
                    PendingIntent dummyEvent = PendingIntent
                            .getBroadcast(this, 0, new Intent("com.devx.SMSExample.IGNORE_ME"), 0);

                    String latString = String.valueOf(loc.getLatitude()).replaceAll(",", ".");
                    String lonString = String.valueOf(loc.getLongitude()).replaceAll(",", ".");
                    StringBuilder sB = new StringBuilder();
                    String lastPosition;
                    if (doPanic) {
                        lastPosition = getString(R.string.help_needed);
                    } else {
                        lastPosition = getString(R.string.last_position);
                    }
                    sB.append(lastPosition).append(":");
                    sB.append("http://www.openstreetmap.org/?lat=");
                    sB.append(latString);
                    sB.append("&lon=");
                    sB.append(lonString);
                    sB.append("&zoom=14");
                    sB.append("&layers=M&mlat=");
                    sB.append(latString);
                    sB.append("&mlon=");
                    sB.append(lonString);
                    String msg = sB.toString();

                    try {
                        if (msg.length() > 160) {
                            msg = msg.substring(0, 160);
                            if (Debug.D) Logger.i("SmsIntent", "Trimming msg to: " + msg);
                        }
                        mng.sendTextMessage(number, null, msg, dummyEvent, dummyEvent);
                        Toast.makeText(this, R.string.message_sent, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Logger.e(this, e.getLocalizedMessage(), e);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(R.string.panic_number_notset).setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                    public void onClick( DialogInterface dialog, int id ) {
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.gpslogging_only).setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                public void onClick( DialogInterface dialog, int id ) {
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        }

    }

}