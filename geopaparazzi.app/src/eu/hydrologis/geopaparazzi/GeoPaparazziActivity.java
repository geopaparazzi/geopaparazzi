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

import static eu.hydrologis.geopaparazzi.util.Constants.PANICKEY;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SlidingDrawer;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.gps.GpsLocation;
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.sensors.SensorsManager;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.dashboard.QuickAction;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoMaps;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.DatabaseManager;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.maps.DataManager;
import eu.hydrologis.geopaparazzi.maps.MapItem;
import eu.hydrologis.geopaparazzi.maps.MapsActivity;
import eu.hydrologis.geopaparazzi.maps.tiles.MapGeneratorInternal;
import eu.hydrologis.geopaparazzi.osm.OsmUtilities;
import eu.hydrologis.geopaparazzi.preferences.PreferencesActivity;
import eu.hydrologis.geopaparazzi.util.AboutActivity;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.ExportActivity;
import eu.hydrologis.geopaparazzi.util.ImportActivity;
import eu.hydrologis.geopaparazzi.util.QuickActionsFactory;

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

    private ResourcesManager resourcesManager;
    private ActionBar actionBar;

    private final int RETURNCODE_BROWSE_FOR_NEW_PREOJECT = 665;
    private final int RETURNCODE_NOTES = 666;
    private final int RETURNCODE_PICS = 667;

    private boolean sliderIsOpen = false;
    private GpsManager gpsManager;
    private SensorsManager sensorManager;
    private HashMap<Integer, String> tileSourcesMap = null;
    private HashMap<String, String> fileSourcesMap = null;
    private AlertDialog mapChoiceDialog;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        init();

        fileSourcesMap = new HashMap<String, String>();

        tileSourcesMap = new LinkedHashMap<Integer, String>();
        tileSourcesMap.put(1001, MapGeneratorInternal.DATABASE_RENDERER.name());
        tileSourcesMap.put(1002, MapGeneratorInternal.MAPNIK.name());
        tileSourcesMap.put(1003, MapGeneratorInternal.OPENCYCLEMAP.name());

        File sdcardDir = ResourcesManager.getInstance(this).getSdcardDir();
        if (sdcardDir != null && sdcardDir.exists()) {
            File[] mapFiles = sdcardDir.listFiles(new FilenameFilter(){
                public boolean accept( File dir, String filename ) {
                    return filename.endsWith(".mapurl");
                }
            });
            
            Arrays.sort(mapFiles);

            int i = 1004;
            for( File file : mapFiles ) {
                String name = FileUtilities.getNameWithoutExtention(file);
                tileSourcesMap.put(i++, name);
                fileSourcesMap.put(name, file.getAbsolutePath());
            }
        }

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
            actionBar = ActionBar.getActionBar(this, R.id.action_bar, gpsManager, sensorManager);
            actionBar.setTitle(R.string.app_name, R.id.action_bar_title);
        }
        actionBar.checkLogging();

    }
    private void init() {
        Object stateObj = getLastNonConfigurationInstance();
        if (stateObj instanceof ResourcesManager) {
            resourcesManager = (ResourcesManager) stateObj;
        } else {
            ResourcesManager.resetManager();
            resourcesManager = ResourcesManager.getInstance(this);
        }

        if (resourcesManager == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(eu.hydrologis.geopaparazzi.R.string.no_sdcard_use_internal_memory).setCancelable(false)
                    .setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            ResourcesManager.setUseInternalMemory(true);
                            resourcesManager = ResourcesManager.getInstance(GeoPaparazziActivity.this);
                            initIfOk();
                        }
                    }).setNegativeButton(this.getString(R.string.no), new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            finish();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
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

        gpsManager = GpsManager.getInstance(this);
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
            Utilities.toast(this, R.string.databaseError, Toast.LENGTH_LONG);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean doOsmPref = preferences.getBoolean(Constants.PREFS_KEY_DOOSM, false);
        if (doOsmPref)
            OsmUtilities.handleOsmTagsDownload(this);

        Utilities.toast(this, getString(eu.hydrologis.geopaparazzi.R.string.loaded_project_in)
                + resourcesManager.getApplicationDir().getAbsolutePath(), Toast.LENGTH_LONG);
    }

    private void checkDebugLogger() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(R.string.enable_debug);
        boolean logToFile = preferences.getBoolean(key, false);
        if (logToFile) {
            File debugLogFile = resourcesManager.getDebugLogFile();
            new Logger(debugLogFile);
        } else {
            new Logger(null);
        }

    }

    public void push( int id, View v ) {
        switch( id ) {
        case R.id.dashboard_note_item_button: {
            QuickAction qa = new QuickAction(v);
            qa.addActionItem(QuickActionsFactory.INSTANCE.getNotesQuickAction(qa, this, RETURNCODE_NOTES));
            qa.addActionItem(QuickActionsFactory.INSTANCE.getPicturesQuickAction(qa, this, RETURNCODE_PICS));
            qa.addActionItem(QuickActionsFactory.INSTANCE.getAudioQuickAction(qa, this, resourcesManager.getMediaDir()));
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
                                Utilities.toast(GeoPaparazziActivity.this, R.string.last_note_deleted, Toast.LENGTH_LONG);
                            } catch (IOException e) {
                                Logger.e(this, e.getLocalizedMessage(), e);
                                e.printStackTrace();
                                Utilities.toast(GeoPaparazziActivity.this, R.string.last_note_not_deleted, Toast.LENGTH_LONG);
                            }
                        }
                    }).show();

            break;
        }
        case R.id.dashboard_log_item_button: {
            QuickAction qa = new QuickAction(v);
            if (gpsManager.isLogging()) {
                ActionItem stopLogQuickAction = QuickActionsFactory.INSTANCE.getStopLogQuickAction(actionBar, qa, this);
                qa.addActionItem(stopLogQuickAction);
            } else {
                ActionItem startLogQuickAction = QuickActionsFactory.INSTANCE.getStartLogQuickAction(actionBar, qa, this);
                qa.addActionItem(startLogQuickAction);
            }
            qa.setAnimStyle(QuickAction.ANIM_AUTO);
            qa.show();
            break;
        }
        case R.id.dashboard_map_item_button: {
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
            sendPosition(null);
            break;
        }
        case R.id.statusupdatebutton: {
            final String lastPositionStr = getString(R.string.last_position);
            final EditText input = new EditText(this);
            input.setText(lastPositionStr);
            Builder builder = new AlertDialog.Builder(this).setTitle(eu.hydrologis.geopaparazzi.R.string.add_message);
            builder.setMessage(eu.hydrologis.geopaparazzi.R.string.insert_an_optional_text_to_send_with_the_geosms);
            builder.setView(input);
            builder.setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                        }
                    }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                            Editable value = input.getText();
                            String newText = value.toString();
                            if (newText == null || newText.length() < 1) {
                                newText = lastPositionStr;
                            }
                            sendPosition(newText);
                        }
                    }).setCancelable(false).show();
            break;
        }
        default:
            break;
        }
    }

    // public boolean onPrepareOptionsMenu (Menu menu) {
    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.mainmenu_preferences).setIcon(android.R.drawable.ic_menu_preferences);
        final SubMenu subMenu = menu.addSubMenu(Menu.NONE, MENU_TILE_SOURCE_ID, 1, R.string.mapsactivity_menu_tilesource)
                .setIcon(R.drawable.ic_menu_tilesource);
        {
            Set<Entry<Integer, String>> entrySet = tileSourcesMap.entrySet();
            for( Entry<Integer, String> entry : entrySet ) {
                subMenu.add(0, entry.getKey(), Menu.NONE, entry.getValue());
            }
        }
        menu.add(Menu.NONE, MENU_RESET, 2, R.string.reset).setIcon(android.R.drawable.ic_menu_revert);
        menu.add(Menu.NONE, MENU_LOAD, 3, R.string.load).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(Menu.NONE, MENU_EXIT, 4, R.string.exit).setIcon(android.R.drawable.ic_lock_power_off);
        menu.add(Menu.NONE, MENU_ABOUT, 5, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);

        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_ABOUT:
            Intent intent = new Intent(this, AboutActivity.class);
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
            return true;
        default: {
            String name = item.getTitle().toString();
            MapGeneratorInternal mapGeneratorInternalNew = null;
            try {
                mapGeneratorInternalNew = MapGeneratorInternal.valueOf(name);
            } catch (IllegalArgumentException e) {
                // ignore, is custom
            }
            if (mapGeneratorInternalNew != null) {
                if (mapGeneratorInternalNew.equals(MapGeneratorInternal.DATABASE_RENDERER)) {
                    // check existing maps and ask for which to load
                    File sdcardDir = ResourcesManager.getInstance(this).getSdcardDir();
                    if (sdcardDir == null || !sdcardDir.exists()) {
                        Utilities
                                .messageDialog(
                                        this,
                                        "Database rendering is supported only from external storage. Could not find external storage, is one available?",
                                        null);
                        return true;
                    }

                    final List<String> mapPaths = new ArrayList<String>();
                    final List<String> mapNames = new ArrayList<String>();

                    File[] mapFiles = sdcardDir.listFiles(new FilenameFilter(){
                        public boolean accept( File dir, String filename ) {
                            return filename.endsWith(".map");
                        }
                    });

                    if (mapFiles == null || mapFiles.length == 0) {
                        Utilities
                                .messageDialog(
                                        this,
                                        "No map files were found on the root of your external storage. Switching to online maps.\nMaps can be downloaded from: http://download.mapsforge.org",
                                        null);
                        return true;
                    }

                    for( File mapFile : mapFiles ) {
                        mapPaths.add(mapFile.getAbsolutePath());
                        mapNames.add(FileUtilities.getNameWithoutExtention(mapFile));
                    }

                    String[] mapNamesArrays = mapNames.toArray(new String[0]);
                    boolean[] mapNamesChecked = new boolean[mapNamesArrays.length];
                    DialogInterface.OnMultiChoiceClickListener dialogListener = new DialogInterface.OnMultiChoiceClickListener(){
                        public void onClick( DialogInterface dialog, int which, boolean isChecked ) {
                            String mapPath = mapPaths.get(which);
                            setTileSource(MapGeneratorInternal.DATABASE_RENDERER.toString(), new File(mapPath));
                            mapChoiceDialog.dismiss();
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Select map to use");
                    builder.setMultiChoiceItems(mapNamesArrays, mapNamesChecked, dialogListener);
                    mapChoiceDialog = builder.create();
                    mapChoiceDialog.show();
                } else {
                    setTileSource(mapGeneratorInternalNew.toString(), null);
                }
            } else {

                String fileSource = fileSourcesMap.get(name);
                if (fileSource != null) {
                    setTileSource(null, new File(fileSource));
                }

            }
        }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Sets the tilesource.
     * 
     * <p>
     * If both arguments are set null, it wil try to get info from the preferences,
     * and used sources are saved into preferences.
     * </p>
     * 
     * @param sourceName if source is <code>null</code>, mapnik is used.
     * @param mapfile the map file to use in case it is a database based source. 
     */
    private void setTileSource( String sourceName, File mapfile ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        MapGeneratorInternal mapGeneratorInternal = MapGeneratorInternal.MAPNIK;
        if (sourceName != null) {
            mapGeneratorInternal = MapGeneratorInternal.valueOf(sourceName);

            if (mapGeneratorInternal.equals(MapGeneratorInternal.DATABASE_RENDERER)) {
                if (mapfile == null || !mapfile.exists()) {
                    // try from preferences
                    String filePath = preferences.getString(Constants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
                    mapfile = new File(filePath);
                    if (!mapfile.exists()) {
                        mapGeneratorInternal = MapGeneratorInternal.MAPNIK;
                        Utilities.messageDialog(this, "Could not find map file, switching to MAPNIK tile source.", null);
                        mapfile = null;
                    }
                }
            }
        }

        Editor editor = preferences.edit();
        editor.putString(Constants.PREFS_KEY_TILESOURCE, sourceName);
        if (mapfile != null)
            editor.putString(Constants.PREFS_KEY_TILESOURCE_FILE, mapfile.getAbsolutePath());
        editor.commit();

    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (RETURNCODE_BROWSE_FOR_NEW_PREOJECT): {
            if (resultCode == Activity.RESULT_OK) {
                String chosenFolderToLoad = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                if (chosenFolderToLoad != null && new File(chosenFolderToLoad).getParentFile().exists()) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    Editor editor = preferences.edit();
                    editor.putString(LibraryConstants.PREFS_KEY_BASEFOLDER, chosenFolderToLoad);
                    editor.commit();

                    DatabaseManager.getInstance().closeDatabase();
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
                        Date date = LibraryConstants.TIME_FORMATTER.parse(noteArray[3]);
                        DaoNotes.addNote(this, lon, lat, elev, new java.sql.Date(date.getTime()), noteArray[4], null,
                                NoteType.SIMPLE.getTypeNum());
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
                        DaoImages.addImage(this, lon, lat, elev, azim, new java.sql.Date(new Date().getTime()), "", //$NON-NLS-1$
                                relativeImagePath);
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

    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        // force to exit through the exit button
        switch( keyCode ) {
        case KeyEvent.KEYCODE_BACK:
            if (sliderIsOpen) {
                slidingDrawer.animateClose();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);

    }

    public void finish() {
        if (Debug.D)
            Logger.d(this, "Finish called!"); //$NON-NLS-1$
        // save last location just in case
        if (resourcesManager == null) {
            super.finish();
            return;
        }

        GpsLocation loc = gpsManager.getLocation();
        if (loc != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            PositionUtilities.putGpsLocationInPreferences(preferences, loc.getLongitude(), loc.getLatitude(), loc.getAltitude());
        }

        Utilities.toast(this, R.string.loggingoff, Toast.LENGTH_LONG);

        gpsManager.dispose(this);

        ResourcesManager.resetManager();
        resourcesManager = null;
        super.finish();
    }

    private AlertDialog alertDialog = null;
    private void resetData() {
        final String enterNewProjectString = getString(eu.hydrologis.geopaparazzi.R.string.enter_a_name_for_the_new_project);
        final String projectExistingString = getString(eu.hydrologis.geopaparazzi.R.string.chosen_project_exists);

        final File applicationParentDir = resourcesManager.getApplicationParentDir();
        final String newGeopaparazziDirName = Constants.GEOPAPARAZZI
                + "_" + LibraryConstants.TIMESTAMPFORMATTER.format(new Date()); //$NON-NLS-1$
        final EditText input = new EditText(this);
        input.setText(newGeopaparazziDirName);
        input.addTextChangedListener(new TextWatcher(){
            public void onTextChanged( CharSequence s, int start, int before, int count ) {
            }
            public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            }
            public void afterTextChanged( Editable s ) {
                String newName = s.toString();
                File newProjectFile = new File(applicationParentDir, newName);
                if (newName == null || newName.length() < 1) {
                    alertDialog.setMessage(enterNewProjectString);
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else if (newProjectFile.exists()) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    alertDialog.setMessage(projectExistingString);
                } else {
                    alertDialog.setMessage(enterNewProjectString);
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
        Builder builder = new AlertDialog.Builder(this).setTitle(R.string.reset);
        builder.setMessage(enterNewProjectString);
        builder.setView(input);
        alertDialog = builder.setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        try {
                            Editable value = input.getText();
                            String newName = value.toString();
                            DatabaseManager.getInstance().closeDatabase();
                            File newGeopaparazziDirFile = new File(applicationParentDir.getAbsolutePath(), newName);
                            if (!newGeopaparazziDirFile.mkdir()) {
                                throw new IOException("Unable to create the geopaparazzi folder."); //$NON-NLS-1$
                            }
                            ResourcesManager.getInstance(GeoPaparazziActivity.this).setApplicationDir(
                                    newGeopaparazziDirFile.getAbsolutePath());

                            Intent intent = getIntent();
                            finish();
                            startActivity(intent);
                        } catch (IOException e) {
                            Logger.e(this, e.getLocalizedMessage(), e);
                            e.printStackTrace();
                            Toast.makeText(GeoPaparazziActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }).setCancelable(false).create();
        alertDialog.show();

    }

    private SlidingDrawer slidingDrawer;

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
        return resourcesManager;
    }

    /**
     * Send the panic or status update message.
     * 
     * @param theTextToRunOn make the panic message as opposed to just a status update.
     */
    @SuppressWarnings("nls")
    private void sendPosition( String theTextToRunOn ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String panicNumbersString = preferences.getString(PANICKEY, "");
        // Make sure there's a valid return address.
        if (panicNumbersString == null || panicNumbersString.length() == 0 || panicNumbersString.matches(".*[A-Za-z].*")) {
            Utilities.messageDialog(this, R.string.panic_number_notset, null);
        } else {
            String[] numbers = panicNumbersString.split(";");
            for( String number : numbers ) {
                number = number.trim();
                if (number.length() == 0) {
                    continue;
                }
                String lastPosition;
                if (theTextToRunOn == null) {
                    lastPosition = getString(R.string.help_needed);
                } else {
                    lastPosition = theTextToRunOn;
                }
                String positionText = SmsUtilities.createPositionText(this, lastPosition);
                SmsUtilities.sendSMS(this, number, positionText);
            }
        }

    }

}