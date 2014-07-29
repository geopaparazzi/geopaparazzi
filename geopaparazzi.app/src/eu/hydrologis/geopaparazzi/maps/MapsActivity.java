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
package eu.hydrologis.geopaparazzi.maps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.ScreenPosition;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.model.GeoPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.EditingView;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.mixare.MixareHandler;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.sms.SmsData;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.GeocodeActivity;
import eu.geopaparazzi.library.util.activities.InsertCoordActivity;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.activities.DataListActivity;
import eu.geopaparazzi.spatialite.database.spatial.activities.EditableLayersListActivity;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.maps.overlays.ArrayGeopaparazziOverlay;
import eu.hydrologis.geopaparazzi.maptools.tools.MainEditingToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.TapMeasureTool;
import eu.hydrologis.geopaparazzi.osm.OsmCategoryActivity;
import eu.hydrologis.geopaparazzi.osm.OsmTagsManager;
import eu.hydrologis.geopaparazzi.osm.OsmUtilities;
import eu.hydrologis.geopaparazzi.util.Bookmark;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.MixareUtilities;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsActivity extends MapActivity implements OnTouchListener, OnClickListener, OnLongClickListener {
    private final int INSERTCOORD_RETURN_CODE = 666;
    private final int ZOOM_RETURN_CODE = 667;
    private final int GPSDATAPROPERTIES_RETURN_CODE = 668;
    private final int DATAPROPERTIES_RETURN_CODE = 671;
    /**
     * The form update return code.
     */
    public static final int FORMUPDATE_RETURN_CODE = 669;
    private final int CONTACT_RETURN_CODE = 670;
    // private static final int MAPSDIR_FILETREE = 777;

    private final int MENU_GPSDATA = 1;
    private final int MENU_DATA = 2;
    private final int MENU_CENTER_ON_GPS = 3;
    private final int MENU_SCALE_ID = 4;
    private final int MENU_MIXARE_ID = 5;
    private final int MENU_GO_TO = 6;
    private final int MENU_CENTER_ON_MAP = 7;
    private final int MENU_COMPASS_ID = 8;
    private final int MENU_SENDDATA_ID = 9;

    private static final String ARE_BUTTONSVISIBLE_OPEN = "ARE_BUTTONSVISIBLE_OPEN"; //$NON-NLS-1$
    private DecimalFormat formatter = new DecimalFormat("00"); //$NON-NLS-1$
    private MapView mapView;
    private SlidingDrawer osmSlidingDrawer;
    private SharedPreferences preferences;
    private boolean doOsm;

    private ArrayGeopaparazziOverlay dataOverlay;

    private List<String> smsString;
    private Drawable notesDrawable;
    private ProgressDialog syncProgressDialog;
    private BroadcastReceiver gpsServiceBroadcastReceiver;
    private double[] lastGpsPosition;

    private TextView zoomLevelText;
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int maxValue = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int chargedPct = (level * 100) / maxValue;
            updateBatteryCondition(chargedPct);
        }

    };

    private GpsServiceStatus lastGpsServiceStatus = GpsServiceStatus.GPS_OFF;
    private GpsLoggingStatus lastGpsLoggingStatus = GpsLoggingStatus.GPS_DATABASELOGGING_OFF;
    private ImageButton centerOnGps;
    private Button batteryButton;
    private BroadcastReceiver mapsSupportBroadcastReceiver;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.mapsview);

        mapsSupportBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(MapsSupportService.REREAD_MAP_REQUEST)) {
                    boolean rereadMap = intent.getBooleanExtra(MapsSupportService.REREAD_MAP_REQUEST, false);
                    if (rereadMap) {
                        readData();
                        mapView.invalidate();
                    }
                } else if (intent.hasExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST)) {
                    boolean centerOnPosition = intent.getBooleanExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST, false);
                    if (centerOnPosition) {
                        double lon = intent.getDoubleExtra(LibraryConstants.LONGITUDE, 0.0);
                        double lat = intent.getDoubleExtra(LibraryConstants.LATITUDE, 0.0);
                        setNewCenter(lon, lat);
//                        readData();
//                        mapView.invalidate();
                    }
                }
            }
        };
        registerReceiver(mapsSupportBroadcastReceiver, new IntentFilter(
                MapsSupportService.MAPSSUPPORT_SERVICE_BROADCAST_NOTIFICATION));

        gpsServiceBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                onGpsServiceUpdate(intent);
            }
        };
        GpsServiceUtilities.registerForBroadcasts(this, gpsServiceBroadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);

        Button menuButton = (Button) findViewById(R.id.menu_map_btn);
        menuButton.setOnClickListener(this);
        menuButton.setOnLongClickListener(this);
        registerForContextMenu(menuButton);

        // register for battery updates
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // mj10777: .mbtiles,.map and .mapurl files may know their bounds and desired center point
        // - 'checkCenterLocation' will change this value if out of range
        double[] mapCenterLocation = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
        // check for screen on
        boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        boolean areButtonsVisible = preferences.getBoolean(ARE_BUTTONSVISIBLE_OPEN, false);

        /*
         * create main mapview
        */
        mapView = new MapView(this);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(false);
        mapView.setOnTouchListener(this);

        // TODO
        // boolean persistent = preferences.getBoolean("cachePersistence", false);
        // int capacity = Math.min(preferences.getInt("cacheSize", FILE_SYSTEM_CACHE_SIZE_DEFAULT),
        // FILE_SYSTEM_CACHE_SIZE_MAX);
        // TileCache fileSystemTileCache = this.mapView.getFileSystemTileCache();
        // fileSystemTileCache.setPersistent(persistent);
        // fileSystemTileCache.setCapacity(capacity);
        MapsDirManager mapsDirManager = MapsDirManager.getInstance();
        mapsDirManager.loadSelectedMap(mapView, mapCenterLocation);

        MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();

        boolean doImperial = preferences.getBoolean(Constants.PREFS_KEY_IMPERIAL, false);
        mapScaleBar.setImperialUnits(doImperial);
        if (doImperial) {
            mapScaleBar.setText(TextField.FOOT, " ft"); //$NON-NLS-1$
            mapScaleBar.setText(TextField.MILE, " mi"); //$NON-NLS-1$
        } else {
            mapScaleBar.setText(TextField.KILOMETER, " km"); //$NON-NLS-1$
            mapScaleBar.setText(TextField.METER, " m"); //$NON-NLS-1$
        }
        mapScaleBar.setScreenPosition(ScreenPosition.TOPLEFT);

        if (Debug.D) {
            // boolean drawTileFrames = preferences.getBoolean("drawTileFrames", false);
            // boolean drawTileCoordinates = preferences.getBoolean("drawTileCoordinates", false);
            // boolean highlightWaterTiles = preferences.getBoolean("highlightWaterTiles", false);
            DebugSettings debugSettings = new DebugSettings(true, true, false);
            this.mapView.setDebugSettings(debugSettings);
        }

        setTextScale();

        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.innerlayout);
        rl.addView(mapView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        Button zoomInButton = (Button) findViewById(R.id.zoomin);
        zoomInButton.setOnClickListener(this);

        zoomLevelText = (TextView) findViewById(R.id.zoomlevel);

        Button zoomOutButton = (Button) findViewById(R.id.zoomout);
        zoomOutButton.setOnClickListener(this);

        batteryButton = (Button) findViewById(R.id.battery);

        centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);
        centerOnGps.setOnClickListener(this);

        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        addnotebytagButton.setOnClickListener(this);

        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        addBookmarkButton.setOnClickListener(this);

        ImageButton listNotesButton = (ImageButton) findViewById(R.id.listnotesbutton);
        listNotesButton.setOnClickListener(this);

        ImageButton listBookmarksButton = (ImageButton) findViewById(R.id.bookmarkslistbutton);
        listBookmarksButton.setOnClickListener(this);

        final ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        toggleMeasuremodeButton.setOnClickListener(this);

        final Button toggleEditingButton = (Button) findViewById(R.id.toggleEditingButton);
        toggleEditingButton.setOnClickListener(this);
        toggleEditingButton.setOnLongClickListener(this);

        try {
            handleOsmSliderView();
        } catch (Exception e) {
            e.printStackTrace();
        }
        saveCenterPref();

        if (areButtonsVisible) {
            setAllButtoonsEnablement(true);
        } else {
            setAllButtoonsEnablement(false);
        }
        EditingView editingView = (EditingView) findViewById(R.id.editingview);
        LinearLayout editingToolsLayout = (LinearLayout) findViewById(R.id.editingToolsLayout);
        EditManager.INSTANCE.setEditingView(editingView, editingToolsLayout);

        // if after rotation a toolgroup is there, enable ti with its icons
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup != null) {
            toggleEditingButton.setBackgroundResource(R.drawable.ic_toggle_editing_on);
            activeToolGroup.initUI();
            setLeftButtoonsEnablement(true);
        }
    }

    @Override
    protected void onPause() {
        Utilities.dismissProgressDialog(syncProgressDialog);
        super.onPause();
    }

    @Override
    protected void onResume() {

        // notes type
        boolean doCustom = preferences.getBoolean(Constants.PREFS_KEY_NOTES_CHECK, false);
        if (doCustom) {
            String opacityStr = preferences.getString(Constants.PREFS_KEY_NOTES_OPACITY, "100"); //$NON-NLS-1$
            String sizeStr = preferences.getString(Constants.PREFS_KEY_NOTES_SIZE, "15"); //$NON-NLS-1$
            String colorStr = preferences.getString(Constants.PREFS_KEY_NOTES_CUSTOMCOLOR, "blue"); //$NON-NLS-1$
            int noteSize = Integer.parseInt(sizeStr);
            float opacity = Float.parseFloat(opacityStr) * 255 / 100;

            OvalShape notesShape = new OvalShape();
            Paint notesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            notesPaint.setStyle(Paint.Style.FILL);
            notesPaint.setColor(ColorUtilities.toColor(colorStr));
            notesPaint.setAlpha((int) opacity);

            ShapeDrawable notesShapeDrawable = new ShapeDrawable(notesShape);
            Paint paint = notesShapeDrawable.getPaint();
            paint.set(notesPaint);
            notesShapeDrawable.setIntrinsicHeight(noteSize);
            notesShapeDrawable.setIntrinsicWidth(noteSize);
            notesDrawable = notesShapeDrawable;
        } else {
            notesDrawable = getResources().getDrawable(R.drawable.information);
        }

        dataOverlay = new ArrayGeopaparazziOverlay(this);
        List<Overlay> overlays = mapView.getOverlays();
        overlays.clear();
        overlays.add(dataOverlay);

        super.onResume();
    }

    private void setTextScale() {
        String textSizeFactorStr = preferences.getString(Constants.PREFS_KEY_MAPSVIEW_TEXTSIZE_FACTOR, "1.0"); //$NON-NLS-1$
        float textSizeFactor = 1f;
        try {
            textSizeFactor = Float.parseFloat(textSizeFactorStr);
        } catch (NumberFormatException e) {
            // ignore
        }
        if (textSizeFactor < 0.5f) {
            textSizeFactor = 1f;
        }
        mapView.setTextScale(textSizeFactor);
    }

    @Override
    protected void onDestroy() {
        EditManager.INSTANCE.setEditingView(null, null);
        unregisterReceiver(batteryReceiver);

        if (mapsSupportBroadcastReceiver != null) {
            unregisterReceiver(mapsSupportBroadcastReceiver);
        }

        if (gpsServiceBroadcastReceiver != null)
            GpsServiceUtilities.unregisterFromBroadcasts(this, gpsServiceBroadcastReceiver);

        if (dataOverlay != null)
            dataOverlay.dispose();

        if (mapView != null) {
            MapGenerator mapGenerator = mapView.getMapGenerator();
            if (mapGenerator != null) {
                mapGenerator.cleanup();
            }
        }

        super.onDestroy();
    }

    private void readData() {
        try {
            dataOverlay.clearItems();
            dataOverlay.clearWays();

            List<OverlayWay> logOverlaysList = DaoGpsLog.getGpslogOverlays();
            dataOverlay.addWays(logOverlaysList);

            /* images */
            if (DataManager.getInstance().areImagesVisible()) {
                Drawable imageMarker = getResources().getDrawable(R.drawable.photo);
                Drawable newImageMarker = ArrayGeopaparazziOverlay.boundCenter(imageMarker);
                List<OverlayItem> imagesOverlaysList = DaoImages.getImagesOverlayList(newImageMarker);
                dataOverlay.addItems(imagesOverlaysList);
            }

            /* gps notes */
            if (DataManager.getInstance().areNotesVisible()) {
                Drawable newNotesMarker = ArrayGeopaparazziOverlay.boundCenter(notesDrawable);
                List<OverlayItem> noteOverlaysList = DaoNotes.getNoteOverlaysList(newNotesMarker);
                dataOverlay.addItems(noteOverlaysList);
            }

            /* bookmarks */
            Drawable bookmarkMarker = getResources().getDrawable(R.drawable.bookmark);
            Drawable newBookmarkMarker = ArrayGeopaparazziOverlay.boundCenter(bookmarkMarker);
            List<OverlayItem> bookmarksOverlays = DaoBookmarks.getBookmarksOverlays(newBookmarkMarker);
            dataOverlay.addItems(bookmarksOverlays);

            // read last known gps position
            if (lastGpsPosition != null) {
                GeoPoint geoPoint = new GeoPoint((int) (lastGpsPosition[1] * LibraryConstants.E6),
                        (int) (lastGpsPosition[0] * LibraryConstants.E6));
                dataOverlay.setGpsPosition(geoPoint, 0f, lastGpsServiceStatus, lastGpsLoggingStatus);
            }
            // dataOverlay.requestRedraw();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "onTouch issued with motionevent: " + action); //$NON-NLS-1$

        if (action == MotionEvent.ACTION_UP) {
            saveCenterPref();

            // update zoom ui a bit later. This is ugly but
            // found no way until there is not event handling
            // in mapsforge
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            int zoom = mapView.getMapPosition().getZoomLevel();
                            zoom = setCurrentZoom(zoom);
                            setGuiZoomText(zoom);
                            saveCenterPref();
                        }
                    });
                }
            }).start();
        }
        return false;
    }

    private void handleOsmSliderView() throws Exception {
        OsmTagsManager osmTagsManager = OsmTagsManager.getInstance();
        String[] categoriesNamesArray = osmTagsManager.getTagCategories(this);

        int visibility = View.VISIBLE;
        if (categoriesNamesArray == null) {
            categoriesNamesArray = new String[]{""}; //$NON-NLS-1$
            visibility = View.GONE; // invisible
        }
        doOsm = visibility != View.GONE;
        boolean doOsmPref = preferences.getBoolean(Constants.PREFS_KEY_DOOSM, false);
        doOsm = doOsm && doOsmPref;
        if (!doOsm) {
            visibility = View.GONE; // invisible
        }

        final String[] categoriesNamesArrayFinal = categoriesNamesArray;

        // slidingdrawer
        final int slidingId = R.id.osmslide;
        osmSlidingDrawer = (SlidingDrawer) findViewById(slidingId);
        osmSlidingDrawer.setVisibility(visibility);

        if (doOsm) {
            GridView buttonGridView = (GridView) findViewById(R.id.osmcategoriesview);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, categoriesNamesArrayFinal) {
                public View getView(final int position, View cView, ViewGroup parent) {

                    final Button osmButton = new Button(MapsActivity.this);
                    osmButton.setText(categoriesNamesArrayFinal[position]);
                    osmButton.setBackgroundResource(R.drawable.osmcategory_button_drawable);
                    osmButton.setOnClickListener(new Button.OnClickListener() {
                        public void onClick(View v) {
                            String categoryName = osmButton.getText().toString();
                            Intent osmCategoryIntent = new Intent(MapsActivity.this, OsmCategoryActivity.class);
                            osmCategoryIntent.putExtra(Constants.OSM_CATEGORY_KEY, categoryName);
                            startActivity(osmCategoryIntent);
                        }
                    });
                    return osmButton;
                }
            };
            buttonGridView.setAdapter(arrayAdapter);

            Button syncOsmButton = (Button) findViewById(R.id.syncosmbutton);
            syncOsmButton.setOnClickListener(new Button.OnClickListener() {

                public void onClick(View v) {

                    if (!NetworkUtilities.isNetworkAvailable(getApplicationContext())) {
                        Utilities.messageDialog(MapsActivity.this, R.string.available_only_with_network, null);
                        return;
                    }

                    Utilities.inputMessageDialog(MapsActivity.this, getString(R.string.set_description),
                            getString(R.string.osm_insert_a_changeset_description), "", new TextRunnable() {
                                public void run() {
                                    sync(theTextToRunOn);
                                }
                            }
                    );
                }

                private void sync(final String description) {
                    syncProgressDialog = ProgressDialog.show(MapsActivity.this, "", getString(R.string.loading_data));
                    new AsyncTask<String, Void, String>() {
                        private Exception e = null;

                        protected String doInBackground(String... params) {
                            String response = null;
                            try {
                                response = OsmUtilities.sendOsmNotes(MapsActivity.this, description);
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.e = e;
                            }
                            return response;
                        }

                        protected void onPostExecute(String response) {
                            Utilities.dismissProgressDialog(syncProgressDialog);
                            if (e == null) {
                                String msg = getResources().getString(R.string.osm_notes_properly_uploaded);
                                if (response.toLowerCase().trim().startsWith(OsmUtilities.FEATURES_IMPORTED)) {
                                    String leftOver = response.replaceFirst(OsmUtilities.FEATURES_IMPORTED, ""); //$NON-NLS-1$
                                    if (leftOver.trim().length() > 0) {
                                        String text = leftOver.substring(1);
                                        text = text.replaceFirst("\\_", "/"); //$NON-NLS-1$//$NON-NLS-2$

                                        msg = MessageFormat.format(
                                                "Some of the features were uploaded, but not all of them ({0}).", text); //$NON-NLS-1$
                                        Utilities.warningDialog(MapsActivity.this, msg, null);
                                    } else {
                                        Utilities.yesNoMessageDialog(MapsActivity.this, msg, new Runnable() {
                                            public void run() {
                                                try {
                                                    DaoNotes.deleteNotesByType(NoteType.OSM);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, null);
                                    }
                                } else if (response.toLowerCase().trim().contains(OsmUtilities.ERROR_JSON)) {
                                    msg = getString(R.string.error_json_osm);
                                    Utilities.warningDialog(MapsActivity.this, msg, null);
                                } else if (response.toLowerCase().trim().contains(OsmUtilities.ERROR_OSM)) {
                                    msg = getString(R.string.error_osm_server);
                                    Utilities.warningDialog(MapsActivity.this, msg, null);
                                }

                            } else {
                                String msg = getResources().getString(R.string.an_error_occurred_while_uploading_osm_tags);
                                Utilities.warningDialog(MapsActivity.this, msg + e.getLocalizedMessage(), null);
                            }
                        }
                    }.execute((String) null);
                }
            });
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            double[] lastCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
            MapsDirManager.getInstance().setMapViewCenter(mapView, lastCenter, MapsDirManager.ZOOMTYPE.DEFAULT);
            int currentZoomLevel = getCurrentZoomLevel();
            setGuiZoomText(currentZoomLevel);

            readData();
            saveCenterPref();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * Return current Zoom [from MapsDirManager].
     *
     * @return integer current zoom level.
     */
    private static int getCurrentZoomLevel() {
        return MapsDirManager.getInstance().getCurrentZoom();
    }

    /**
     * Set current Zoom level in gui and MapsDirManager.
     * <p/>
     * <p>checking is done to insure that the new Zoom is
     * inside the supported min/max Zoom-levels
     * <p>the present value will be retained if invalid
     */
    private static int setCurrentZoom(int newZoom) {
        newZoom = MapsDirManager.getInstance().setCurrentZoom(newZoom);
        return newZoom;
    }

    private void setGuiZoomText(int newZoom) {
        zoomLevelText.setText(formatter.format(newZoom));
    }

    /**
     * set MapView Center point [in MapsDirManager]
     * <p/>
     * - this should be the only function used to compleate this task
     * -- error logic has been build in use value incase the function was incorrectly called
     * <p>if (mapCenterLocation == null)
     * <p>- the default Center of the loaded map will be taken
     * <p>-  if (i_default_zoom == 1)
     * <p>-- the default Zoom of the loaded map will be taken
     * <p>-  if (i_default_zoom == 2)
     * <p>-- the getMinZoom() of the loaded map will be taken
     *
     * @param lon center lon
     * @param lat center lat
     */
    public void setNewCenter(double lon, double lat) {
        double[] mapCenterLocation = new double[]{lon, lat, (double) getCurrentZoomLevel()};
        MapsDirManager.getInstance().setMapViewCenter(mapView, mapCenterLocation, MapsDirManager.ZOOMTYPE.DEFAULT);
        saveCenterPref();
    }

    /**
     * Set new center.
     *
     * @param lon  center lon
     * @param lat  center lat
     * @param zoom the zoom level to set.
     */
    public void setNewCenterAtZoom(double lon, double lat, int zoom) {
        zoom = setCurrentZoom(zoom);
        double[] mapCenterLocation = new double[]{lon, lat, (double) zoom};
        MapsDirManager.getInstance().setMapViewCenter(mapView, mapCenterLocation, MapsDirManager.ZOOMTYPE.DEFAULT);
        setGuiZoomText(zoom);
    }

    /**
     * @return the center [lon, lat]
     */
    public static double[] getCenterLonLat() {
        return MapsDirManager.getInstance().getMapCenter();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_DATA, 2, R.string.base_maps).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_SCALE_ID, 3, R.string.mapsactivity_menu_toggle_scalebar).setIcon(R.drawable.ic_menu_scalebar);
        menu.add(Menu.NONE, MENU_COMPASS_ID, 4, R.string.mapsactivity_menu_toggle_compass).setIcon(
                android.R.drawable.ic_menu_compass);
        boolean centerOnGps = preferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
        if (centerOnGps) {
            menu.add(Menu.NONE, MENU_CENTER_ON_GPS, 6, R.string.disable_center_on_gps).setIcon(
                    android.R.drawable.ic_menu_mylocation);
        } else {
            menu.add(Menu.NONE, MENU_CENTER_ON_GPS, 6, R.string.enable_center_on_gps).setIcon(
                    android.R.drawable.ic_menu_mylocation);
        }

        menu.add(Menu.NONE, MENU_CENTER_ON_MAP, 7, R.string.center_on_map).setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(Menu.NONE, MENU_GO_TO, 8, R.string.go_to).setIcon(android.R.drawable.ic_menu_myplaces);
        if (SmsUtilities.hasPhone(this)) {
            menu.add(Menu.NONE, MENU_SENDDATA_ID, 8, R.string.send_data).setIcon(android.R.drawable.ic_menu_send);
        }
        menu.add(Menu.NONE, MENU_MIXARE_ID, 9, R.string.view_in_mixare).setIcon(R.drawable.icon_datasource);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // THIS IS CURRENTLY DISABLED
            //
            // case MENU_TILE_SOURCE_ID:
            // startMapsDirTreeViewList();
            // return true;
            case MENU_GPSDATA:
                Intent gpsDatalistIntent = new Intent(this, GpsDataListActivity.class);
                startActivityForResult(gpsDatalistIntent, GPSDATAPROPERTIES_RETURN_CODE);
                return true;
            case MENU_DATA:
                Intent datalistIntent = new Intent(this, DataListActivity.class);
                startActivityForResult(datalistIntent, DATAPROPERTIES_RETURN_CODE);
                return true;
            case MENU_SCALE_ID:
                MapScaleBar mapScaleBar = mapView.getMapScaleBar();
                boolean showMapScaleBar = mapScaleBar.isShowMapScaleBar();
                mapScaleBar.setShowMapScaleBar(!showMapScaleBar);
                return true;
            case MENU_COMPASS_ID:
                ActionBar.openCompass(this);
                return true;
            case MENU_MIXARE_ID:
                if (!MixareHandler.isMixareInstalled(this)) {
                    MixareHandler.installMixareFromMarket(this);
                    return true;
                }
                float[] nswe = getMapWorldBounds();

                try {
                    MixareUtilities.runRegionOnMixare(this, nswe[0], nswe[1], nswe[2], nswe[3]);
                    return true;
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return false;
                }
            case MENU_SENDDATA_ID:
                try {
                    sendData();
                    return true;
                } catch (Exception e1) {
                    e1.printStackTrace();
                    return false;
                }
            case MENU_GO_TO: {
                return goTo();
            }
            case MENU_CENTER_ON_MAP: {
                MapsDirManager.getInstance().setMapViewCenter(mapView, null, MapsDirManager.ZOOMTYPE.DEFAULT);
                saveCenterPref();
                return true;
            }
            case MENU_CENTER_ON_GPS: {
                boolean centerOnGps = preferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
                Editor edit = preferences.edit();
                edit.putBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, !centerOnGps);
                edit.commit();
                return true;
            }
            default:
        }
        return super.onContextItemSelected(item);
    }
    // THIS IS CURRENTLY DISABLED
    //
    // /**
    // * Start the Dialog to select a map
    // *
    // * <p>
    // * MapDirManager creates a static-list of maps and sends it to the MapsDirTreeViewList class
    // * - when first called this list will build a diretory/file list AND a map-type/Diretory/File
    // list
    // * - once created, this list will be retained during the Application
    // * - the user can switch from a sorted list as Directory/File OR Map-Type/Diretory/File view
    // * </p>
    // * result will be sent to MapDirManager and saved there and stored to preferences
    // * - when the MapView is created, this stroed value will be read and loaded
    // */
    // private void startMapsDirTreeViewList() {
    // try {
    // startActivityForResult(new Intent(this, MapsDirTreeViewList.class), MAPSDIR_FILETREE);
    // } catch (Exception e) {
    // GPLog.androidLog(4,
    // "GeoPaparazziActivity -E-> failed[startActivity(new Intent(this,MapsDirTreeViewList.class));]",
    // e);
    // }
    // }

    private void sendData() throws IOException {
        float[] nswe = getMapWorldBounds();
        List<SmsData> smsData = new ArrayList<SmsData>();
        List<Bookmark> bookmarksList = DaoBookmarks.getBookmarksInWorldBounds(nswe[0], nswe[1], nswe[2], nswe[3]);
        for (Bookmark bookmark : bookmarksList) {
            double lat = bookmark.getLat();
            double lon = bookmark.getLon();
            String title = bookmark.getName();

            SmsData data = new SmsData();
            data.TYPE = SmsData.BOOKMARK;
            data.x = (float) lon;
            data.y = (float) lat;
            data.z = 16f;
            data.text = title;
            smsData.add(data);
        }

        List<Note> notesList = DaoNotes.getNotesInWorldBounds(nswe[0], nswe[1], nswe[2], nswe[3]);
        for (Note note : notesList) {
            double lat = note.getLat();
            double lon = note.getLon();
            double elevation = note.getAltim();
            String title = note.getName();

            SmsData data = new SmsData();
            data.TYPE = SmsData.NOTE;
            data.x = (float) lon;
            data.y = (float) lat;
            data.z = (float) elevation;
            data.text = title;
            smsData.add(data);
        }

        smsString = new ArrayList<String>();
        String schemaHost = SmsUtilities.SMSHOST + "/"; //$NON-NLS-1$
        StringBuilder sb = new StringBuilder(schemaHost);
        int limit = 160;
        for (SmsData data : smsData) {
            String smsDataString = data.toSmsDataString();
            String tmp = sb.toString() + ";" + smsDataString; //$NON-NLS-1$
            if (tmp.length() <= limit) {
                if (sb.length() > schemaHost.length())
                    sb.append(";"); //$NON-NLS-1$
            } else {
                smsString.add(sb.toString());
                sb = new StringBuilder(schemaHost);
            }
            sb.append(smsDataString);
        }

        if (sb.length() > schemaHost.length()) {
            smsString.add(sb.toString());
        }

        if (smsString.size() == 0) {
            Utilities.messageDialog(this, R.string.found_no_data_to_send, null);
        } else {

            String message = smsString.size() + getString(R.string.insert_phone_to_send);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message).setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for (String smsMsg : smsString) {
                                SmsUtilities.sendSMSViaApp(MapsActivity.this, "", smsMsg); //$NON-NLS-1$
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // ignore
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

        }

    }

    private boolean goTo() {
        String[] items = new String[]{getString(R.string.goto_coordinate), getString(R.string.geocoding)};

        new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();

                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (selectedPosition == 0) {
                            Intent intent = new Intent(MapsActivity.this, InsertCoordActivity.class);
                            startActivityForResult(intent, INSERTCOORD_RETURN_CODE);
                        } else {
                            Intent intent = new Intent(MapsActivity.this, GeocodeActivity.class);
                            startActivityForResult(intent, INSERTCOORD_RETURN_CODE);
                        }

                    }
                }).show();

        return true;
    }

    /**
     * Retrieves the map world bounds in degrees.
     *
     * @return the [n,s,w,e] in degrees.
     */
    private float[] getMapWorldBounds() {
        float[] nswe = getMapWorldBoundsE6();
        float n = nswe[0] / LibraryConstants.E6;
        float s = nswe[1] / LibraryConstants.E6;
        float w = nswe[2] / LibraryConstants.E6;
        float e = nswe[3] / LibraryConstants.E6;
        return new float[]{n, s, w, e};
    }

    /**
     * Retrieves the map world bounds in microdegrees.
     *
     * @return the [n,s,w,e] in midrodegrees.
     */
    private float[] getMapWorldBoundsE6() {
        Projection projection = mapView.getProjection();
        int latitudeSpan = projection.getLatitudeSpan();
        int longitudeSpan = projection.getLongitudeSpan();
        MapViewPosition mapPosition = mapView.getMapPosition();
        GeoPoint c = mapPosition.getMapCenter();
        float n = (c.latitudeE6 + latitudeSpan / 2);
        float s = (c.latitudeE6 - latitudeSpan / 2);
        float w = (c.longitudeE6 - longitudeSpan / 2);
        float e = (c.longitudeE6 + longitudeSpan / 2);
        float[] nswe = {n, s, w, e};
        return nswe;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "Activity returned"); //$NON-NLS-1$
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            // THIS IS CURRENTLY DISABLED
            //
            // case MAPSDIR_FILETREE: {
            // if (resultCode == Activity.RESULT_OK) {
            // // String s_SELECTED_FILE=data.getStringExtra(MapsDirTreeViewList.SELECTED_FILE);
            // // String s_SELECTED_TYPE=data.getStringExtra(MapsDirTreeViewList.SELECTED_TYPE);
            // // selected_classinfo will contain all information about the map
            // // MapsDirManager will store this internaly and store the values to preferences
            // // - if this is called from a non-Map-Activity:
            // // -- the map_view parameter und the position parameter MUST be null
            // // - if this is called from a Map-Activity:
            // // -- the map_view parameter and the position parameter should be given
            // // --- the position parameter is not given [null], it will use the position of the
            // // map_view
            // // -- if not null : selected_MapClassInfo() will call
            // // MapsDirManager.load_Map(map_view,mapCenterLocation);
            //
            // if (MapsDirTreeViewList.selected_classinfo != null) {
            // // MapsDirManager.load_Map(mapView,null);
            // // GPLog.androidLog(-1,"MapsActivity -I->  onActivityResult s_selected_map["
            // // +MapsDirTreeViewList.selected_classinfo.getShortDescription()+ "] ");
            // MapsDirManager.getInstance().selectMapClassInfo(this,
            // MapsDirTreeViewList.selected_classinfo, mapView, null);
            // // mj10777: not sure what to do with these values ??
            // minZoomLevel = MapsDirManager.getInstance().getMinZoom();
            // maxZoomLevel = MapsDirManager.getInstance().getMaxZoom();
            // }
            // }
            // }
            // break;
            case (INSERTCOORD_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {

                    float[] routePoints = data.getFloatArrayExtra(LibraryConstants.ROUTE);
                    if (routePoints != null) {
                        // it is a routing request
                        try {
                            String name = data.getStringExtra(LibraryConstants.NAME);
                            if (name == null) {
                                name = "ROUTE_" + TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$
                            }
                            DaoGpsLog logDumper = new DaoGpsLog();
                            SQLiteDatabase sqliteDatabase = logDumper.getDatabase();
                            java.sql.Date now = new java.sql.Date(new java.util.Date().getTime());
                            long newLogId = logDumper.addGpsLog(now, now, 0, name, 3, "blue", true); //$NON-NLS-1$

                            sqliteDatabase.beginTransaction();
                            try {
                                java.sql.Date nowPlus10Secs = now;
                                for (int i = 0; i < routePoints.length; i = i + 2) {
                                    double lon = routePoints[i];
                                    double lat = routePoints[i + 1];
                                    double altim = -1;

                                    // dummy time increment
                                    nowPlus10Secs = new java.sql.Date(nowPlus10Secs.getTime() + 10000);
                                    logDumper.addGpsLogDataPoint(sqliteDatabase, newLogId, lon, lat, altim, nowPlus10Secs);
                                }

                                sqliteDatabase.setTransactionSuccessful();
                            } finally {
                                sqliteDatabase.endTransaction();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            GPLog.error(this, "Cannot draw route.", e); //$NON-NLS-1$
                        }

                    } else {
                        // it is a single point geocoding request
                        double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
                        double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
                        setCenterAndZoomForMapWindowFocus(lon, lat, null);
                    }
                }
                break;
            }
            case (ZOOM_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
                    double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
                    int zoom = data.getIntExtra(LibraryConstants.ZOOMLEVEL, 1);
                    setCenterAndZoomForMapWindowFocus(lon, lat, zoom);
                }
                break;
            }
            case (GPSDATAPROPERTIES_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
                    double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
                    setCenterAndZoomForMapWindowFocus(lon, lat, null);
                }
                break;
            }
            case (DATAPROPERTIES_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, -9999d);
                        double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, -9999d);
                        if (lon < -9000d) {
                            // no coordinate passed
                            // re-read
                            SpatialDatabasesManager.getInstance().getSpatialVectorTables(true);
                        } else {
                            setCenterAndZoomForMapWindowFocus(lon, lat, null);
                        }
                    } catch (jsqlite.Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case (CONTACT_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    String number = null;
                    if (smsString != null && uri != null) {
                        Cursor c = null;
                        try {
                            c = getContentResolver().query(
                                    uri,
                                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                            ContactsContract.CommonDataKinds.Phone.TYPE}, null, null, null
                            );

                            if (c != null && c.moveToFirst()) {
                                number = c.getString(0);
                                // int type = c.getInt(1);
                                // showSelectedNumber(type, number);
                            }
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }

                        if (number != null) {
                            int count = 1;
                            for (String sms : smsString) {
                                sms = sms.replaceAll("\\s+", "_"); //$NON-NLS-1$//$NON-NLS-2$
                                SmsUtilities.sendSMS(MapsActivity.this, number, sms, false);
                                String msg = getString(R.string.sent_sms) + count++;
                                Utilities.toast(this, msg, Toast.LENGTH_SHORT);
                            }
                        }
                    }
                }
                break;
            }
            case (FORMUPDATE_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    String[] formArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_FORM);
                    if (formArray != null) {
                        try {
                            double lon = Double.parseDouble(formArray[0]);
                            double lat = Double.parseDouble(formArray[1]);
                            String textStr = formArray[4];
                            String jsonStr = formArray[6];

                            float n = (float) (lat + 0.00001f);
                            float s = (float) (lat - 0.00001f);
                            float w = (float) (lon - 0.00001f);
                            float e = (float) (lon + 0.00001f);

                            List<Note> notesInWorldBounds = DaoNotes.getNotesInWorldBounds(n, s, w, e);
                            if (notesInWorldBounds.size() > 0) {
                                Note note = notesInWorldBounds.get(0);
                                long id = note.getId();
                                DaoNotes.updateForm(id, textStr, jsonStr);
                            }

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

    private void addBookmark() {
        GeoPoint mapCenter = mapView.getMapPosition().getMapCenter();
        final float centerLat = mapCenter.latitudeE6 / LibraryConstants.E6;
        final float centerLon = mapCenter.longitudeE6 / LibraryConstants.E6;
        final EditText input = new EditText(this);
        final String newDate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date());
        final String proposedName = "bookmark " + newDate; //$NON-NLS-1$
        input.setText(proposedName);
        Builder builder = new AlertDialog.Builder(this).setTitle(R.string.mapsactivity_new_bookmark);
        builder.setMessage(R.string.mapsactivity_enter_bookmark_name);
        builder.setView(input);
        builder.setIcon(android.R.drawable.ic_dialog_info)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // ignore
                    }
                }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    Editable value = input.getText();
                    String newName = value.toString();
                    if (newName == null || newName.length() < 1) {
                        newName = proposedName;
                        ;
                    }

                    int zoom = mapView.getMapPosition().getZoomLevel();
                    float[] nswe = getMapWorldBounds();
                    DaoBookmarks.addBookmark(centerLon, centerLat, newName, zoom, nswe[0], nswe[1], nswe[2], nswe[3]);
                    mapView.invalidateOnUiThread();
                } catch (IOException e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).setCancelable(false).show();
    }

    /**
     * Calls the mapview redraw.
     */
    public void invalidateMap() {
        mapView.invalidateOnUiThread();
    }

    // public boolean onKeyDown( int keyCode, KeyEvent event ) {
    // // force to exit through the exit button
    // if (keyCode == KeyEvent.KEYCODE_BACK && sliderIsOpen) {
    // slidingDrawer.animateClose();
    // return true;
    // } else if (keyCode == KeyEvent.KEYCODE_BACK && osmSliderIsOpen) {
    // osmSlidingDrawer.animateClose();
    // return true;
    // }
    // return super.onKeyDown(keyCode, event);
    // }

    private boolean boundsContain(int latE6, int lonE6, int nE6, int sE6, int wE6, int eE6) {
        return lonE6 > wE6 && lonE6 < eE6 && latE6 > sE6 && latE6 < nE6;
    }

    private synchronized void saveCenterPref() {
        MapViewPosition mapPosition = mapView.getMapPosition();
        GeoPoint mapCenter = mapPosition.getMapCenter();
        double lon = mapCenter.longitudeE6 / LibraryConstants.E6;
        double lat = mapCenter.latitudeE6 / LibraryConstants.E6;
        int zoomLevel = mapPosition.getZoomLevel();

        if (GPLog.LOG_ABSURD) {
            StringBuilder sb = new StringBuilder();
            sb.append("Map Center moved: "); //$NON-NLS-1$
            sb.append(lon);
            sb.append("/"); //$NON-NLS-1$
            sb.append(lat);
            GPLog.addLogEntry(this, sb.toString());
        }

        PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, zoomLevel);

        EditManager.INSTANCE.invalidateEditingView();
    }

    /**
     * Set center coords and zoom ready for the {@link MapsActivity} to focus again.
     * <p/>
     * <p>In {@link MapsActivity} the {@link MapsActivity#onWindowFocusChanged(boolean)}
     * will take care to zoom properly.
     *
     * @param centerX the lon coordinate. Can be <code>null</code>.
     * @param centerY the lat coordinate. Can be <code>null</code>.
     * @param zoom    the zoom. Can be <code>null</code>.
     */
    public void setCenterAndZoomForMapWindowFocus(Double centerX, Double centerY, Integer zoom) {
        MapViewPosition mapPosition = mapView.getMapPosition();
        GeoPoint mapCenter = mapPosition.getMapCenter();
        int zoomLevel = mapPosition.getZoomLevel();
        float cx = 0f;
        float cy = 0f;
        if (centerX != null) {
            cx = centerX.floatValue();
        } else {
            cx = (float) (mapCenter.longitudeE6 / LibraryConstants.E6);
        }
        if (centerY != null) {
            cy = centerY.floatValue();
        } else {
            cy = (float) (mapCenter.latitudeE6 / LibraryConstants.E6);
        }
        if (zoom != null) {
            zoomLevel = zoom;
        }
        PositionUtilities.putMapCenterInPreferences(preferences, cx, cy, zoomLevel);
    }

    private void updateBatteryCondition(int level) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "BATTERY LEVEL GEOPAP: " + level); //$NON-NLS-1$
        StringBuilder sb = new StringBuilder();
        sb.append(level);
        if (level < 100) {
            sb.append("%"); //$NON-NLS-1$
        }
        batteryButton.setText(sb.toString());
    }

    private void onGpsServiceUpdate(Intent intent) {
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);
        if (lastGpsPosition == null) {
            return;
        }
        lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);

        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
        float accuracy = 0;
        if (lastGpsPositionExtras != null) {
            accuracy = lastGpsPositionExtras[0];
        }

        if (this.mapView.getWidth() <= 0 || this.mapView.getWidth() <= 0) {
            return;
        }
        try {
            double lat = lastGpsPosition[1];
            double lon = lastGpsPosition[0];

            // send updates to the editing framework
            EditManager.INSTANCE.onGpsUpdate(lon, lat);

            float[] nsweE6 = getMapWorldBoundsE6();
            int latE6 = (int) ((float) lat * LibraryConstants.E6);
            int lonE6 = (int) ((float) lon * LibraryConstants.E6);
            boolean centerOnGps = preferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);

            int nE6 = (int) nsweE6[0];
            int sE6 = (int) nsweE6[1];
            int wE6 = (int) nsweE6[2];
            int eE6 = (int) nsweE6[3];

            // Rect bounds = new Rect(wE6, nE6, eE6, sE6);
            if (boundsContain(latE6, lonE6, nE6, sE6, wE6, eE6)) {
                GeoPoint point = new GeoPoint(latE6, lonE6);
                dataOverlay.setGpsPosition(point, accuracy, lastGpsServiceStatus, lastGpsLoggingStatus);
                dataOverlay.requestRedraw();
            }

            Projection p = mapView.getProjection();
            int paddingX = (int) (p.getLongitudeSpan() * 0.2);
            int paddingY = (int) (p.getLatitudeSpan() * 0.2);
            int newEE6 = eE6 - paddingX;
            int newWE6 = wE6 + paddingX;
            int newSE6 = sE6 + paddingY;
            int newNE6 = nE6 - paddingY;

            boolean doCenter = false;
            if (!boundsContain(latE6, lonE6, newNE6, newSE6, newWE6, newEE6)) {
                if (centerOnGps) {
                    doCenter = true;
                }
            }
            if (doCenter) {
                setNewCenter(lon, lat);
                if (GPLog.LOG_ABSURD)
                    GPLog.addLogEntry(this, "recentering triggered"); //$NON-NLS-1$
            }
        } catch (Exception e) {
            GPLog.error(this, "On location change error", e); //$NON-NLS-1$
            // finish the activity to reset
            finish();
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu_map_btn:
                Button menuButton = (Button) findViewById(R.id.menu_map_btn);
                openContextMenu(menuButton);
                break;
            case R.id.zoomin:
                int currentZoom = getCurrentZoomLevel();
                int newZoom = currentZoom + 1;
                newZoom = setCurrentZoom(newZoom);
                setGuiZoomText(newZoom);
                mapView.getController().setZoom(newZoom);
                invalidateMap();
                saveCenterPref();
                break;
            case R.id.zoomout:
                currentZoom = getCurrentZoomLevel();
                newZoom = currentZoom - 1;
                newZoom = setCurrentZoom(newZoom);
                setGuiZoomText(newZoom);
                mapView.getController().setZoom(newZoom);
                invalidateMap();
                saveCenterPref();
                break;
            case R.id.center_on_gps_btn:
                if (lastGpsPosition != null) {
                    setNewCenter(lastGpsPosition[0], lastGpsPosition[1]);
                }
                break;
            case R.id.addnotebytagbutton:
                // generate screenshot in background in order to not freeze
                try {
                    File mediaDir = ResourcesManager.getInstance(MapsActivity.this).getMediaDir();
                    final File tmpImageFile = new File(mediaDir.getParentFile(), LibraryConstants.TMPPNGIMAGENAME);
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Rect t = new Rect();
                                mapView.getDrawingRect(t);
                                Bitmap bufferedBitmap = Bitmap.createBitmap(t.width(), t.height(), Bitmap.Config.ARGB_8888);
                                Canvas bufferedCanvas = new Canvas(bufferedBitmap);
                                mapView.draw(bufferedCanvas);
                                FileOutputStream out = new FileOutputStream(tmpImageFile);
                                bufferedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                                out.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }).start();
                    Intent mapTagsIntent = new Intent(MapsActivity.this, MapTagsActivity.class);
                    startActivity(mapTagsIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.addbookmarkbutton:
                addBookmark();
                break;
            case R.id.listnotesbutton:
                Intent intent = new Intent(MapsActivity.this, NotesListActivity.class);
                startActivityForResult(intent, ZOOM_RETURN_CODE);
                break;
            case R.id.bookmarkslistbutton:
                intent = new Intent(MapsActivity.this, BookmarksListActivity.class);
                startActivityForResult(intent, ZOOM_RETURN_CODE);
                break;
            case R.id.togglemeasuremodebutton:
                boolean isInMeasureMode = !mapView.isClickable();
                final ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
                if (!isInMeasureMode) {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode_on);
                } else {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode);
                }
                if (isInMeasureMode) {
                    EditManager.INSTANCE.setActiveTool(null);
                } else {
                    TapMeasureTool measureTool = new TapMeasureTool(mapView);
                    EditManager.INSTANCE.setActiveTool(measureTool);
                }
                break;
            case R.id.toggleEditingButton:
                toggleEditing();
                break;

            default:
                break;
        }
    }

    private void toggleEditing() {
        final Button toggleEditingButton = (Button) findViewById(R.id.toggleEditingButton);
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup == null) {
            toggleEditingButton.setBackgroundResource(R.drawable.ic_toggle_editing_on);

            activeToolGroup = new MainEditingToolGroup(mapView);
            EditManager.INSTANCE.setActiveToolGroup(activeToolGroup);
            setLeftButtoonsEnablement(false);
        } else {
            toggleEditingButton.setBackgroundResource(R.drawable.ic_toggle_editing_off);
            EditManager.INSTANCE.setActiveTool(null);
            EditManager.INSTANCE.setActiveToolGroup(null);
            setLeftButtoonsEnablement(true);
        }
    }

    private void setLeftButtoonsEnablement(boolean enable) {
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        ImageButton listNotesButton = (ImageButton) findViewById(R.id.listnotesbutton);
        ImageButton listBookmarksButton = (ImageButton) findViewById(R.id.bookmarkslistbutton);
        ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        if (enable) {
            addnotebytagButton.setVisibility(View.VISIBLE);
            addBookmarkButton.setVisibility(View.VISIBLE);
            listNotesButton.setVisibility(View.VISIBLE);
            listBookmarksButton.setVisibility(View.VISIBLE);
            toggleMeasuremodeButton.setVisibility(View.VISIBLE);
        } else {
            addnotebytagButton.setVisibility(View.GONE);
            addBookmarkButton.setVisibility(View.GONE);
            listNotesButton.setVisibility(View.GONE);
            listBookmarksButton.setVisibility(View.GONE);
            toggleMeasuremodeButton.setVisibility(View.GONE);
        }
    }

    private void setAllButtoonsEnablement(boolean enable) {
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        ImageButton listNotesButton = (ImageButton) findViewById(R.id.listnotesbutton);
        ImageButton listBookmarksButton = (ImageButton) findViewById(R.id.bookmarkslistbutton);
        ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        Button zoomInButton = (Button) findViewById(R.id.zoomin);
        TextView zoomLevelTextview = (TextView) findViewById(R.id.zoomlevel);
        Button zoomOutButton = (Button) findViewById(R.id.zoomout);
        Button toggleEditingButton = (Button) findViewById(R.id.toggleEditingButton);

        int visibility = View.VISIBLE;
        if (!enable) {
            visibility = View.GONE;
        }
        addnotebytagButton.setVisibility(visibility);
        addBookmarkButton.setVisibility(visibility);
        listNotesButton.setVisibility(visibility);
        listBookmarksButton.setVisibility(visibility);
        toggleMeasuremodeButton.setVisibility(visibility);
        batteryButton.setVisibility(visibility);
        centerOnGps.setVisibility(visibility);
        zoomInButton.setVisibility(visibility);
        zoomLevelTextview.setVisibility(visibility);
        zoomOutButton.setVisibility(visibility);
        toggleEditingButton.setVisibility(visibility);
    }

    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.toggleEditingButton:
                Intent editableLayersIntent = new Intent(MapsActivity.this, EditableLayersListActivity.class);
                startActivity(editableLayersIntent);
                return true;
            case R.id.menu_map_btn:
                boolean areButtonsVisible = preferences.getBoolean(ARE_BUTTONSVISIBLE_OPEN, false);
                setAllButtoonsEnablement(!areButtonsVisible);
                Editor edit = preferences.edit();
                edit.putBoolean(ARE_BUTTONSVISIBLE_OPEN, !areButtonsVisible);
                edit.commit();
                return true;
            default:
                break;
        }
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // force to exit through the exit button
        // System.out.println(keyCode + "/" + KeyEvent.KEYCODE_BACK);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (EditManager.INSTANCE.getActiveToolGroup() != null) {
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
    }
}
