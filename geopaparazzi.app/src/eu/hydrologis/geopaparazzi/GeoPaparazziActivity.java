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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sensors.SensorsManager;
import eu.geopaparazzi.library.sms.SmsData;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.AboutActivity;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.util.debug.TestMock;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.sourcesview.SourcesTreeListActivity;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.maps.DataManager;
import eu.hydrologis.geopaparazzi.maps.LogMapItem;
import eu.hydrologis.geopaparazzi.maps.MapTagsActivity;
import eu.hydrologis.geopaparazzi.maps.MapsActivity;
import eu.hydrologis.geopaparazzi.osm.OsmUtilities;
import eu.hydrologis.geopaparazzi.preferences.PreferencesActivity;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.ExportActivity;
import eu.hydrologis.geopaparazzi.util.GpUtilities;
import eu.hydrologis.geopaparazzi.util.ImportActivity;
import eu.hydrologis.geopaparazzi.util.SecretActivity;

/**
 * The main {@link Activity activity} of GeoPaparazzi.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoPaparazziActivity extends Activity {

    private static final int MENU_ABOUT = Menu.FIRST;
    private static final int MENU_EXIT = 2;
    private static final int MENU_TILE_SOURCE_ID = 3;
    private static final int MENU_SETTINGS = 4;
    private static final int MENU_RESET = 5;
    private static final int MENU_LOAD = 6;
    private static final int MAPSDIR_FILETREE = 777;

    private ResourcesManager resourcesManager;
    private ActionBar actionBar;

    private final int RETURNCODE_BROWSE_FOR_NEW_PREOJECT = 665;
    private final int RETURNCODE_NOTES = 666;
    private final int RETURNCODE_PICS = 667;
    private final int RETURNCODE_SKETCH = 668;

    private boolean sliderIsOpen = false;
    private SensorsManager sensorManager;
    private SlidingDrawer slidingDrawer;
    private ProgressDialog initMapsdirDialog;
    private BroadcastReceiver gpsServiceBroadcastReceiver;
    private GpsServiceStatus lastGpsServiceStatus = GpsServiceStatus.GPS_OFF;
    private GpsLoggingStatus lastGpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;
    private double[] lastGpsPosition;

    private static boolean checkedGps = false;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        GpsServiceUtilities.startGpsService(this);
        gpsServiceBroadcastReceiver = new BroadcastReceiver(){
            public void onReceive( Context context, Intent intent ) {
                onGpsServiceUpdate(intent);
                checkFirstTimeGps(context);
            }
        };
        GpsServiceUtilities.registerForBroadcasts(this, gpsServiceBroadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);

        try {
            checkMockLocations();
            // clearCacheIfneeded();
            initializeResourcesManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
        checkIncomingGeosms();
        checkIncomingSmsData();
    }

    private void checkMockLocations() {
        /*
         * check mock locations availability
         */
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isMockMode = preferences.getBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
        if (isMockMode) {
            if (!TestMock.isMockEnabled(getContentResolver())) {
                Utilities
                        .messageDialog(
                                this,
                                "To use the demo mode you need to enable Androids mock locations in the developer settings. Disabling demo mode.",
                                null);
                Editor edit = preferences.edit();
                edit.putBoolean(LibraryConstants.PREFS_KEY_MOCKMODE, false);
                edit.commit();
            }
        }

    }

    @SuppressLint("DefaultLocale")
    /**
     * Checks if it was opened for a link of the kind:<br>
     * http://maps.google.com/maps?q=46.068941,11.169849&GeoSMS#usertext<br>
     * or<br>
     * http://www.openstreetmap.org/#map=19/46.67695/11.12605&layers=N#usertext<br>
     * in which case the point is imported as bookmark.
     */
    private void checkIncomingGeosms() {
        Uri data = getIntent().getData();
        if (data != null) {
            try {
                String path = data.toString();
                if (path.toLowerCase().contains(SmsUtilities.SMSHOST)) {
                    return;
                }
                // try google
                String[] latLonTextFromGmapUrl = Utilities.getLatLonTextFromGmapUrl(path);
                if (latLonTextFromGmapUrl != null) {
                    double lat = Double.parseDouble(latLonTextFromGmapUrl[0]);
                    double lon = Double.parseDouble(latLonTextFromGmapUrl[1]);
                    DaoBookmarks.addBookmark(lon, lat, latLonTextFromGmapUrl[2], 16, -1, -1, -1, -1);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, 16);

                    checkMapsLoadingFinished();
                    Intent mapIntent = new Intent(this, MapsActivity.class);
                    startActivity(mapIntent);
                } else {
                    // try osm
                    String[] latLonTextFromOsmUrl = Utilities.getLatLonTextFromOsmUrl(path);
                    if (latLonTextFromOsmUrl != null) {
                        double lat = Double.parseDouble(latLonTextFromOsmUrl[0]);
                        double lon = Double.parseDouble(latLonTextFromOsmUrl[1]);
                        int zoom = (int) Double.parseDouble(latLonTextFromOsmUrl[3]);
                        DaoBookmarks.addBookmark(lon, lat, latLonTextFromOsmUrl[2], zoom, -1, -1, -1, -1);
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                        PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, 16);
                        checkMapsLoadingFinished();
                        Intent mapIntent = new Intent(this, MapsActivity.class);
                        startActivity(mapIntent);
                    }
                }

            } catch (IOException e) {
                GPLog.error(this, "Error parsing URI", e); //$NON-NLS-1$
                Utilities
                        .messageDialog(
                                this,
                                "Could not open the passed URI. Geopaparazzi is able to open only GeoSMS URIs that contain a part like: ...&q=46.068941,11.169849&GeoSMS ",
                                null);
            }
        }
    }

    private void checkIncomingSmsData() {
        /*
         * check if it was opened for a link of the kind
         *
         * http://maps.google.com/maps?q=46.068941,11.169849&GeoSMS
         */
        Uri data = getIntent().getData();
        if (data != null) {
            try {
                String path = data.toString();
                String scheme = data.getScheme(); // "http"
                if (scheme != null && scheme.equals("http")) { //$NON-NLS-1$
                    String host = data.getHost();
                    if (host.equals(SmsUtilities.SMSHOST)) {

                        List<SmsData> sms2Data = SmsUtilities.sms2Data(path);
                        int notesNum = 0;
                        int bookmarksNum = 0;
                        if (sms2Data.size() > 0) {
                            for( SmsData smsData : sms2Data ) {
                                String text = smsData.text.replaceAll("\\_", " "); //$NON-NLS-1$//$NON-NLS-2$
                                if (smsData.TYPE == SmsData.NOTE) {
                                    String dateStr = TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC.format(new Date());
                                    DaoNotes.addNote(smsData.x, smsData.y, smsData.z, dateStr, text, NoteType.POI.getDef(), null,
                                            NoteType.POI.getTypeNum());
                                    notesNum++;
                                } else if (smsData.TYPE == SmsData.BOOKMARK) {
                                    DaoBookmarks.addBookmark(smsData.x, smsData.y, text, smsData.z, -1, -1, -1, -1);
                                    bookmarksNum++;
                                }
                            }
                        }

                        Utilities.messageDialog(this, MessageFormat.format(
                                getString(eu.hydrologis.geopaparazzi.R.string.imported_notes_and_bookmarks), notesNum,
                                bookmarksNum), null);
                    }
                }
            } catch (Exception e) {
                Utilities.messageDialog(this, getString(eu.hydrologis.geopaparazzi.R.string.could_not_open_sms), null);
            }
        }
    }


    private void checkMapsLoadingFinished() {
        int maxTimes = 0;
        while (!MapsDirManager.getInstance().finishedLoading() && maxTimes++ < 200){ // max wait 60 seconds
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        Utilities.dismissProgressDialog(initMapsdirDialog);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        checkActionBar();
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        // avoid oncreate call when rotating device
        super.onConfigurationChanged(newConfig);
    }

    public void onWindowFocusChanged( boolean hasFocus ) {
        super.onWindowFocusChanged(hasFocus);
        checkActionBar();
    }

    private void checkActionBar() {
        if (actionBar == null) {
            actionBar = ActionBar.getActionBar(this, R.id.action_bar, sensorManager);
            actionBar.setTitle(R.string.app_name, R.id.action_bar_title);

            final ImageButton menuButton = actionBar.getMenuButton();
            menuButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {
                    openContextMenu(menuButton);
                }
            });
            registerForContextMenu(menuButton);
        }
        // actionBar.checkLogging();
    }

    private void initializeResourcesManager() throws Exception {
        Object stateObj = getLastNonConfigurationInstance();
        if (stateObj instanceof ResourcesManager) {
            resourcesManager = (ResourcesManager) stateObj;
        } else {
            ResourcesManager.resetManager();
            resourcesManager = ResourcesManager.getInstance(this);
        }

        if (resourcesManager == null) {
            Utilities.yesNoMessageDialog(this, getString(eu.hydrologis.geopaparazzi.R.string.no_sdcard_use_internal_memory),
                    new Runnable(){
                        public void run() {
                            ResourcesManager.setUseInternalMemory(true);
                            try {
                                resourcesManager = ResourcesManager.getInstance(GeoPaparazziActivity.this);
                                initIfOk();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Runnable(){
                        public void run() {
                            finish();
                        }
                    });
        } else {
            initIfOk();
        }

    }

    private void initIfOk() {
        if (resourcesManager == null) {
            Utilities.messageDialog(this, R.string.sdcard_notexist, new Runnable(){
                public void run() {
                    finish();
                }
            });
            return;
        }
        setContentView(R.layout.geopap_main);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        GPLogPreferencesHandler.checkLog(preferences);
        GPLogPreferencesHandler.checkLogHeavy(preferences);
        GPLogPreferencesHandler.checkLogAbsurd(preferences);

        sensorManager = SensorsManager.getInstance(this);

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
        logButton = (ImageButton) findViewById(logButtonId);
        // isChecked = applicationManager.isGpsLogging();
        logButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(logButtonId, v);
            }
        });
        if (lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
            logButton.setImageResource(R.drawable.dashboard_stop_log_item);
        } else {
            logButton.setImageResource(R.drawable.dashboard_log_item);
        }

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

        boolean doOsmPref = preferences.getBoolean(Constants.PREFS_KEY_DOOSM, false);
        if (doOsmPref)
            OsmUtilities.handleOsmTagsDownload(this);

        Utilities.toast(this, getString(eu.hydrologis.geopaparazzi.R.string.loaded_project_in)
                + resourcesManager.getApplicationDir().getAbsolutePath(), Toast.LENGTH_LONG);

        // check for screen on
        boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        try {
            GeopaparazziApplication.getInstance().getDatabase();
            checkMapsAndLogsVisibility();

            initMapsDirManager();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
            Utilities.toast(this, R.string.databaseError, Toast.LENGTH_LONG);
        }
    }

    private void initMapsDirManager() throws jsqlite.Exception, IOException, FileNotFoundException {
        MapsDirManager.reset();

        initMapsdirDialog = new ProgressDialog(this);
        initMapsdirDialog.setCancelable(true);
        initMapsdirDialog.setTitle(getString(R.string.maps_manager));
        initMapsdirDialog.setMessage(getString(R.string.loading_maps));
        initMapsdirDialog.setCancelable(false);
        initMapsdirDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        initMapsdirDialog.setIndeterminate(true);
        initMapsdirDialog.show();

        AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                try {
                    MapsDirManager.getInstance().init(GeoPaparazziActivity.this, null);
                    return "";
                } catch (Exception e) {
                    return "ERROR: " + e.getLocalizedMessage();
                }
            }
            protected void onPostExecute( String response ) { // on UI thread!
                Utilities.dismissProgressDialog(initMapsdirDialog);
                if (response.startsWith("ERROR")) {
                    Utilities.messageDialog(GeoPaparazziActivity.this, response, null);
                }
            }
        };
        try {
            asyncTask.execute((String) null);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getLocalizedMessage(), e);
        }
        // MapsDirManager will read the preferences values for the last map
        // - it will collect all information about the maps on the sdcard/maps
        // -- when the preferred map is found, this data will be stored
        // --- when the Map-Activity is created:
        // --- the selected map will be loaded with
        // MapsDirManager.load_Map(map_view,mapCenterLocation);
    }

    /**
     * Push action.
     * 
     * @param id the calling id.
     * @param v parent view.
     */
    public void push( int id, View v ) {
        switch( id ) {
        case R.id.dashboard_note_item_button: {
            boolean isValid = false;
            if (lastGpsServiceStatus.getCode() >= GpsServiceStatus.GPS_FIX.getCode()) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                double[] gpsLocation = PositionUtilities.getGpsLocationFromPreferences(preferences);
                if (gpsLocation != null) {
                    try {
                        Intent mapTagsIntent = new Intent(this, MapTagsActivity.class);
                        startActivity(mapTagsIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isValid = true;
                }
            }
            if (!isValid)
                Utilities.messageDialog(this, R.string.gpslogging_only, null);

            break;
        }
        case R.id.dashboard_undonote_item_button: {
            Utilities.yesNoMessageDialog(this, getString(R.string.remove_last_note_prompt), new Runnable(){
                public void run() {
                    try {
                        DaoNotes.deleteLastInsertedNote();
                        Utilities.toast(GeoPaparazziActivity.this, R.string.last_note_deleted, Toast.LENGTH_LONG);
                    } catch (IOException e) {
                        GPLog.error(this, e.getLocalizedMessage(), e);
                        e.printStackTrace();
                        Utilities.toast(GeoPaparazziActivity.this, R.string.last_note_not_deleted, Toast.LENGTH_LONG);
                    }
                }
            }, null);

            break;
        }
        case R.id.dashboard_log_item_button: {
            final GPApplication appContext = GeopaparazziApplication.getInstance();
            if (lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                Utilities.yesNoMessageDialog(GeoPaparazziActivity.this, getString(R.string.do_you_want_to_stop_logging),
                        new Runnable(){
                            public void run() {
                                runOnUiThread(new Runnable(){
                                    public void run() {
                                        // stop logging
                                        GpsServiceUtilities.stopDatabaseLogging(appContext);
                                        logButton.setImageResource(R.drawable.dashboard_log_item);
                                        actionBar.checkLogging();
                                    }
                                });
                            }
                        }, null);

            } else {
                // start logging
                final Context context = this;
                if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    final String defaultLogName = "log_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$

                    Utilities.inputMessageDialog(context, getString(R.string.gps_log), getString(R.string.gps_log_name),
                            defaultLogName, new TextRunnable(){
                                public void run() {
                                    runOnUiThread(new Runnable(){
                                        public void run() {
                                            String newName = theTextToRunOn;
                                            if (newName == null || newName.length() < 1) {
                                                newName = defaultLogName;
                                            }

                                            logButton.setImageResource(R.drawable.dashboard_stop_log_item);
                                            GpsServiceUtilities.startDatabaseLogging(appContext, newName,
                                                    DaoGpsLog.class.getCanonicalName());
                                            actionBar.checkLogging();
                                            DataManager.getInstance().setLogsVisible(true);
                                        }
                                    });
                                }
                            });

                } else {
                    Utilities.messageDialog(context, R.string.gpslogging_only, null);
                }
            }

            break;
        }
        case R.id.dashboard_map_item_button: {
            if (!MapsDirManager.getInstance().finishedLoading()) {
                Utilities.messageDialog(this, getString(R.string.maps_loading_not_finished_yet), null);
                return;
            }
            Intent mapIntent = new Intent(this, MapsActivity.class);
            startActivity(mapIntent);
            break;
        }
        case R.id.dashboard_import_item_button: {
            Intent exportIntent = new Intent(this, ImportActivity.class);
            startActivity(exportIntent);
            break;
        }
        case R.id.dashboard_export_item_button: {
            Intent exportIntent = new Intent(this, ExportActivity.class);
            startActivity(exportIntent);
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

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, MENU_TILE_SOURCE_ID, 0, R.string.mapsactivity_menu_tilesource).setIcon(R.drawable.ic_menu_tilesource);
        menu.add(Menu.NONE, MENU_RESET, 1, R.string.reset).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(Menu.NONE, MENU_LOAD, 2, R.string.load).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(Menu.NONE, MENU_SETTINGS, 3, R.string.mainmenu_preferences).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(Menu.NONE, MENU_ABOUT, 4, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(Menu.NONE, MENU_EXIT, 45, R.string.exit).setIcon(android.R.drawable.ic_lock_power_off);
    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_ABOUT:
            Intent intent = new Intent(this, AboutActivity.class);
            intent.putExtra(LibraryConstants.PREFS_KEY_TEXT, "eu.hydrologis.geopaparazzi"); //$NON-NLS-1$
            startActivity(intent);
            return true;
        case MENU_SETTINGS:
            Intent preferencesIntent = new Intent(this, PreferencesActivity.class);
            startActivity(preferencesIntent);
            return true;
        case MENU_RESET:
            resetData();
            return true;
        case MENU_LOAD:
            Intent browseIntent = new Intent(this, DirectoryBrowserActivity.class);
            browseIntent.putExtra(DirectoryBrowserActivity.EXTENTION, DirectoryBrowserActivity.FOLDER);
            browseIntent.putExtra(DirectoryBrowserActivity.STARTFOLDERPATH, resourcesManager.getApplicationDir()
                    .getAbsolutePath());
            startActivityForResult(browseIntent, RETURNCODE_BROWSE_FOR_NEW_PREOJECT);
            return true;
        case MENU_EXIT:
            finish();
            return true;
        case MENU_TILE_SOURCE_ID:
            startMapsDirTreeViewList();
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Start the Dialog to select a map
     *
     * <p>
     * MapDirManager creates a static-list of maps and sends it to the MapsDirTreeViewList class
     * - when first called this list will build a directory/file list AND a map-type/Directory/File list
     * - once created, this list will be retained during the Application
     * - the user can switch from a sorted list as Directory/File OR Map-Type/Directory/File view
     * </p>
     *  result will be sent to MapDirManager and saved there and stored to preferences
     *  - when the MapView is created, this stored value will be read and loaded
     */
    private void startMapsDirTreeViewList() {
        try {
            // startActivityForResult(new Intent(this, MapsDirTreeViewList.class),
            // MAPSDIR_FILETREE);
            startActivityForResult(new Intent(this, SourcesTreeListActivity.class), MAPSDIR_FILETREE);
        } catch (Exception e) {
            GPLog.error(this, "GeoPaparazziActivity -E-> failed[startActivity(new Intent(this,MapsDirTreeViewList.class));]", e); //$NON-NLS-1$
        }
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case MAPSDIR_FILETREE: {
            if (resultCode == Activity.RESULT_OK) {
                // String s_SELECTED_FILE=data.getStringExtra(MapsDirTreeViewList.SELECTED_FILE);
                // String s_SELECTED_TYPE=data.getStringExtra(MapsDirTreeViewList.SELECTED_TYPE);
                // selected_classinfo will contain all information about the map
                // MapsDirManager will store this internally and store the values to preferences
                // - if this is called from a non-Map-Activity:
                // -- the map_view parameter and the position parameter MUST be null
                // - if this is called from a Map-Activity:
                // -- the map_view parameter and the position parameter should be given
                // --- the position parameter is not given [null], it will use the position of the
                // map_view
                // -- if not null : selected_MapClassInfo() will call
                // MapsDirManager.load_Map(map_view,mapCenterLocation);

                // if (MapsDirTreeViewList.selected_classinfo != null) {
                // MapsDirManager.getInstance().selectMapClassInfo(this,
                // MapsDirTreeViewList.selected_classinfo, null, null);
                // }
            }
        }
            break;
        case (RETURNCODE_BROWSE_FOR_NEW_PREOJECT): {
            if (resultCode == Activity.RESULT_OK) {
                String chosenFolderToLoad = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                if (chosenFolderToLoad != null && new File(chosenFolderToLoad).getParentFile().exists()) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    Editor editor = preferences.edit();
                    editor.putString(LibraryConstants.PREFS_KEY_BASEFOLDER, chosenFolderToLoad);
                    editor.commit();

                    GeopaparazziApplication.getInstance().closeDatabase();
                    ResourcesManager.resetManager();

                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
            break;
        }
        case (RETURNCODE_NOTES): {
            if (resultCode == Activity.RESULT_OK) {
                String[] noteArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_NOTE);
                if (noteArray != null) {
                    try {
                        double lon = Double.parseDouble(noteArray[0]);
                        double lat = Double.parseDouble(noteArray[1]);
                        double elev = Double.parseDouble(noteArray[2]);
                        DaoNotes.addNote(lon, lat, elev, noteArray[3], noteArray[4], NoteType.POI.getDef(), null,
                                NoteType.POI.getTypeNum());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                    }
                }
            }
            break;
        }
        case (RETURNCODE_PICS): {
            if (resultCode == Activity.RESULT_OK) {
                String relativeImagePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                if (relativeImagePath != null) {
                    File imgFile = new File(resourcesManager.getMediaDir().getParentFile(), relativeImagePath);
                    if (!imgFile.exists()) {
                        return;
                    }
                    try {
                        double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
                        double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
                        double elev = data.getDoubleExtra(LibraryConstants.ELEVATION, 0.0);
                        double azim = data.getDoubleExtra(LibraryConstants.AZIMUTH, 0.0);
                        DaoImages.addImage(lon, lat, elev, azim, new java.sql.Date(new Date().getTime()), "", relativeImagePath);
                    } catch (Exception e) {
                        e.printStackTrace();

                        Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                    }
                }
            }
            break;
        }
        case (RETURNCODE_SKETCH): {
            if (data != null) {
                String absoluteImagePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                if (absoluteImagePath != null) {
                    File imgFile = new File(absoluteImagePath);
                    if (!imgFile.exists()) {
                        return;
                    }
                    try {
                        double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
                        double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
                        double elev = data.getDoubleExtra(LibraryConstants.ELEVATION, 0.0);

                        DaoImages.addImage(lon, lat, elev, -9999.0, new java.sql.Date(new Date().getTime()), "",
                                absoluteImagePath);
                    } catch (Exception e) {
                        e.printStackTrace();

                        Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                    }
                }
            }
            break;
        }
        }
    }

    private int backCount = 0;
    private long previousBackTime = System.currentTimeMillis();
    private ImageButton logButton;

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button
        // System.out.println(keyCode + "/" + KeyEvent.KEYCODE_BACK);
        switch( keyCode ) {
        case KeyEvent.KEYCODE_BACK:
            if (sliderIsOpen) {
                slidingDrawer.animateClose();
            }
            backCount++;
            long backTime = System.currentTimeMillis();

            if (backTime - previousBackTime < 1000) {
                if (backCount > 10) {
                    Intent hiddenIntent = new Intent(this, SecretActivity.class);
                    startActivity(hiddenIntent);
                }
            } else {
                backCount = 0;
            }
            previousBackTime = backTime;
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void finish() {
        try {
            if (GPLog.LOG)
                Log.i("GEOPAPARAZZIACTIVITY", "Finish called!"); //$NON-NLS-1$ //$NON-NLS-2$

            TagsManager.reset(this);

            // save last location just in case
            if (resourcesManager == null) {
                super.finish();
                return;
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (lastGpsPosition != null) {
                PositionUtilities.putGpsLocationInPreferences(preferences, lastGpsPosition[0], lastGpsPosition[1],
                        lastGpsPosition[2]);
            }
            Utilities.toast(this, R.string.loggingoff, Toast.LENGTH_LONG);

            if (gpsServiceBroadcastReceiver != null) {
                GpsServiceUtilities.stopDatabaseLogging(this);
                GpsServiceUtilities.stopGpsService(this);
                GpsServiceUtilities.unregisterFromBroadcasts(this, gpsServiceBroadcastReceiver);
            }
            try {
                MapsDirManager.getInstance().finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
            GeopaparazziApplication.getInstance().closeDatabase();
            ResourcesManager.resetManager();
            resourcesManager = null;

            // Editor edit = preferences.edit();
            // edit.putBoolean("EXIT_THROUGH_FINISH", true);
            // edit.commit();
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            checkedGps = false;
            super.finish();
        }
    }

    // private void clearCacheIfneeded() {
    // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    // boolean exitCalled = preferences.getBoolean("EXIT_THROUGH_FINISH", true);
    // Editor edit = preferences.edit();
    // edit.putBoolean("EXIT_THROUGH_FINISH", false);
    // edit.commit();
    // if (exitCalled) {
    // File cache = getCacheDir();
    // if (cache.exists()) {
    // GPLog.addLogEntry(this, "Deleting cache folder: " + cache);
    // FileUtilities.deleteFileOrDir(cache);
    // }
    // }
    // }

    private void resetData() {
        final String enterNewProjectString = getString(eu.hydrologis.geopaparazzi.R.string.enter_a_name_for_the_new_project);
        final String projectExistingString = getString(eu.hydrologis.geopaparazzi.R.string.chosen_project_exists);

        final File applicationParentDir = resourcesManager.getApplicationParentDir();
        final String newGeopaparazziDirName = Constants.GEOPAPARAZZI
                + "_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(eu.geopaparazzi.library.R.layout.inputdialog);
        dialog.getWindow().setBackgroundDrawableResource(getResources().getColor(android.R.color.transparent));
        final TextView text = (TextView) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogtext);
        text.setText(enterNewProjectString);
        final EditText editText = (EditText) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogEdittext);
        final Button yesButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonOK);
        editText.setText(newGeopaparazziDirName);
        editText.addTextChangedListener(new TextWatcher(){
            public void onTextChanged( CharSequence s, int start, int before, int count ) {
                // ignore
            }
            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
                // ignore
            }
            public void afterTextChanged( Editable s ) {
                String newName = s.toString();
                File newProjectFile = new File(applicationParentDir, newName);
                if (newName.length() < 1) {
                    text.setText(enterNewProjectString);
                    yesButton.setEnabled(false);
                } else if (newProjectFile.exists()) {
                    yesButton.setEnabled(false);
                    text.setText(projectExistingString);
                } else {
                    text.setText(enterNewProjectString);
                    yesButton.setEnabled(true);
                }
            }
        });
        yesButton.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                try {
                    Editable value = editText.getText();
                    String newName = value.toString();
                    GeopaparazziApplication.getInstance().closeDatabase();
                    File newGeopaparazziDirFile = new File(applicationParentDir.getAbsolutePath(), newName);
                    if (!newGeopaparazziDirFile.mkdir()) {
                        throw new IOException("Unable to create the geopaparazzi folder."); //$NON-NLS-1$
                    }
                    ResourcesManager.getInstance(GeoPaparazziActivity.this).setApplicationDir(GeoPaparazziActivity.this,
                            newGeopaparazziDirFile.getAbsolutePath());

                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    Toast.makeText(GeoPaparazziActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        Button cancelButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    /**
     * Check for:
     *  visibility of gpslogs (turned on or off?)
     */
    private static void checkMapsAndLogsVisibility() throws IOException {
        List<LogMapItem> maps = DaoGpsLog.getGpslogs();
        boolean oneVisible = false;
        for( LogMapItem item : maps ) {
            if (!oneVisible && item.isVisible()) {
                oneVisible = true;
            }
        }
        DataManager.getInstance().setLogsVisible(oneVisible);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return resourcesManager;
    }

    /**
     * Send the panic or status update message.
     */
    @SuppressWarnings("nls")
    private void sendPosition( boolean isPanic ) {
        if (isPanic) {
            String lastPosition = getString(R.string.help_needed);
            String[] panicNumbers = GpUtilities.getPanicNumbers(this);
            if (panicNumbers == null) {
                String positionText = SmsUtilities.createPositionText(this, lastPosition);
                SmsUtilities.sendSMSViaApp(this, "", positionText);
            } else {
                for( String number : panicNumbers ) {
                    number = number.trim();
                    if (number.length() == 0) {
                        continue;
                    }

                    String positionText = SmsUtilities.createPositionText(this, lastPosition);
                    SmsUtilities.sendSMS(this, number, positionText, true);
                }
            }
        } else {
            // just sending a single geosms
            String positionText = SmsUtilities.createPositionText(this, "");
            SmsUtilities.sendSMSViaApp(this, "", positionText);
        }

    }

    private void onGpsServiceUpdate( Intent intent ) {
        lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);
        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
        int[] lastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        long lastPositiontime = GpsServiceUtilities.getPositionTime(intent);
        actionBar.setStatus(lastGpsServiceStatus, lastGpsLoggingStatus, lastGpsPosition, lastGpsPositionExtras,
                lastGpsStatusExtras, lastPositiontime);
    }

    private void checkFirstTimeGps( Context context ) {
        if (!checkedGps) {
            checkedGps = true;
            if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                Utilities.yesNoMessageDialog(context, prompt, new Runnable(){
                    public void run() {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
                }, null);
            }
        }
    }

}
