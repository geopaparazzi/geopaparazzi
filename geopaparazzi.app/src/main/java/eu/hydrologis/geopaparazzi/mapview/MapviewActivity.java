/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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
package eu.hydrologis.geopaparazzi.mapview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
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
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.activities.GeocodeActivity;
import eu.geopaparazzi.library.core.dialogs.InsertCoordinatesDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.EditingView;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.Tool;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.forms.FormInfoHolder;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.mixare.MixareHandler;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.share.ShareUtilities;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PointF3D;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.activities.EditableLayersListActivity;
import eu.geopaparazzi.spatialite.database.spatial.activities.databasesview.SpatialiteDatabasesTreeListActivity;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.objects.Bookmark;
import eu.hydrologis.geopaparazzi.database.objects.Note;
import eu.hydrologis.geopaparazzi.maptools.FeatureUtilities;
import eu.hydrologis.geopaparazzi.maptools.MapTool;
import eu.hydrologis.geopaparazzi.maptools.tools.GpsLogInfoTool;
import eu.hydrologis.geopaparazzi.maptools.tools.LineMainEditingToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.NoEditableLayerToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.OnSelectionToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.PointMainEditingToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.PolygonMainEditingToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.PolygonOnSelectionToolGroup;
import eu.hydrologis.geopaparazzi.maptools.tools.TapMeasureTool;
import eu.hydrologis.geopaparazzi.mapview.overlays.ArrayGeopaparazziOverlay;
import eu.hydrologis.geopaparazzi.ui.activities.AddNotesActivity;
import eu.hydrologis.geopaparazzi.ui.activities.BookmarksListActivity;
import eu.hydrologis.geopaparazzi.ui.activities.GpsDataListActivity;
import eu.hydrologis.geopaparazzi.ui.activities.ImportMapsforgeActivity;
import eu.hydrologis.geopaparazzi.ui.activities.NotesListActivity;
import eu.hydrologis.geopaparazzi.utilities.Constants;

import static eu.geopaparazzi.library.util.LibraryConstants.*;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapviewActivity extends MapActivity implements OnTouchListener, OnClickListener, OnLongClickListener, InsertCoordinatesDialogFragment.IInsertCoordinateListener {
    private final int INSERTCOORD_RETURN_CODE = 666;
    private final int ZOOM_RETURN_CODE = 667;
    private final int GPSDATAPROPERTIES_RETURN_CODE = 668;
    private final int DATAPROPERTIES_RETURN_CODE = 671;
    /**
     * The form update return code.
     */
    public static final int FORMUPDATE_RETURN_CODE = 669;
    private final int CONTACT_RETURN_CODE = 670;
    public static final int SELECTED_FEATURES_UPDATED_RETURN_CODE = 672;
    // private static final int MAPSDIR_FILETREE = 777;

    private final int MENU_GPSDATA = 1;
    private final int MENU_DATA = 2;
    private final int MENU_CENTER_ON_GPS = 3;
    private final int MENU_SCALE_ID = 4;
    private final int MENU_MIXARE_ID = 5;
    private final int MENU_GO_TO = 6;
    private final int MENU_CENTER_ON_MAP = 7;
    private final int MENU_COMPASS_ID = 8;
    private final int MENU_SHAREPOSITION_ID = 9;
    private final int MENU_LOADMAPSFORGE_VECTORS_ID = 10;

    private static final String ARE_BUTTONSVISIBLE_OPEN = "ARE_BUTTONSVISIBLE_OPEN"; //$NON-NLS-1$
    private DecimalFormat formatter = new DecimalFormat("00"); //$NON-NLS-1$
    private MapView mMapView;
    private SharedPreferences mPeferences;

    private ArrayGeopaparazziOverlay mDataOverlay;

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
    private TextView coordView;
    private String latString;
    private String lonString;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_mapview);

        mapsSupportBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(MapsSupportService.REREAD_MAP_REQUEST)) {
                    boolean rereadMap = intent.getBooleanExtra(MapsSupportService.REREAD_MAP_REQUEST, false);
                    if (rereadMap) {
                        readData();
                        mMapView.invalidate();
                    }
                } else if (intent.hasExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST)) {
                    boolean centerOnPosition = intent.getBooleanExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST, false);
                    if (centerOnPosition) {
                        double lon = intent.getDoubleExtra(LONGITUDE, 0.0);
                        double lat = intent.getDoubleExtra(LATITUDE, 0.0);
                        setNewCenter(lon, lat);
//                        readData();
//                        mMapView.invalidate();
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

        mPeferences = PreferenceManager.getDefaultSharedPreferences(this);

        // COORDINATE TEXT VIEW
        coordView = (TextView) findViewById(R.id.coordsText);
        latString = getString(R.string.lat);
        lonString = getString(R.string.lon);

        // CENTER CROSS
        setCenterCross();

        FloatingActionButton menuButton = (FloatingActionButton) findViewById(R.id.menu_map_button);
        menuButton.setOnClickListener(this);
        menuButton.setOnLongClickListener(this);
        registerForContextMenu(menuButton);

        // register for battery updates
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        double[] mapCenterLocation = PositionUtilities.getMapCenterFromPreferences(mPeferences, true, true);
        // check for screen on
        boolean keepScreenOn = mPeferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        boolean areButtonsVisible = mPeferences.getBoolean(ARE_BUTTONSVISIBLE_OPEN, true);

        /*
         * create main mapview
        */
        mMapView = new MapView(this);
        mMapView.setClickable(true);
        mMapView.setBuiltInZoomControls(false);
        mMapView.setOnTouchListener(this);

        // TODO
        // boolean persistent = mPeferences.getBoolean("cachePersistence", false);
        // int capacity = Math.min(mPeferences.getInt("cacheSize", FILE_SYSTEM_CACHE_SIZE_DEFAULT),
        // FILE_SYSTEM_CACHE_SIZE_MAX);
        // TileCache fileSystemTileCache = this.mMapView.getFileSystemTileCache();
        // fileSystemTileCache.setPersistent(persistent);
        // fileSystemTileCache.setCapacity(capacity);
        BaseMapSourcesManager.INSTANCE.loadSelectedBaseMap(mMapView);

        MapScaleBar mapScaleBar = this.mMapView.getMapScaleBar();

        boolean doImperial = mPeferences.getBoolean(Constants.PREFS_KEY_IMPERIAL, false);
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
            // boolean drawTileFrames = mPeferences.getBoolean("drawTileFrames", false);
            // boolean drawTileCoordinates = mPeferences.getBoolean("drawTileCoordinates", false);
            // boolean highlightWaterTiles = mPeferences.getBoolean("highlightWaterTiles", false);
            DebugSettings debugSettings = new DebugSettings(true, true, false);
            this.mMapView.setDebugSettings(debugSettings);
        }

        setTextScale();

        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.innerlayout);
        rl.addView(mMapView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ImageButton zoomInButton = (ImageButton) findViewById(R.id.zoomin);
        zoomInButton.setOnClickListener(this);

        zoomLevelText = (TextView) findViewById(R.id.zoomlevel);

        ImageButton zoomOutButton = (ImageButton) findViewById(R.id.zoomout);
        zoomOutButton.setOnClickListener(this);

        batteryButton = (Button) findViewById(R.id.battery);

        centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);
        centerOnGps.setOnClickListener(this);

        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        addnotebytagButton.setOnClickListener(this);
        addnotebytagButton.setOnLongClickListener(this);

        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        addBookmarkButton.setOnClickListener(this);
        addBookmarkButton.setOnLongClickListener(this);

        final ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        toggleMeasuremodeButton.setOnClickListener(this);

        final ImageButton toggleLogInfoButton = (ImageButton) findViewById(R.id.toggleloginfobutton);
        toggleLogInfoButton.setOnClickListener(this);

//        final ImageButton toggleviewingconeButton = (ImageButton) findViewById(R.id.toggleviewingconebutton);
//        toggleviewingconeButton.setOnClickListener(this);

        final ImageButton toggleEditingButton = (ImageButton) findViewById(R.id.toggleEditingButton);
        toggleEditingButton.setOnClickListener(this);
        toggleEditingButton.setOnLongClickListener(this);

        if (mapCenterLocation != null)
            setNewCenterAtZoom(mapCenterLocation[0], mapCenterLocation[1], (int) mapCenterLocation[2]);

        setAllButtoonsEnablement(areButtonsVisible);
        EditingView editingView = (EditingView) findViewById(R.id.editingview);
        LinearLayout editingToolsLayout = (LinearLayout) findViewById(R.id.editingToolsLayout);
        EditManager.INSTANCE.setEditingView(editingView, editingToolsLayout);

        // if after rotation a toolgroup is there, enable ti with its icons
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup != null) {
            toggleEditingButton.setBackgroundResource(R.drawable.mapview_toggle_editing_on);
            activeToolGroup.initUI();
            setLeftButtoonsEnablement(true);
        }

        GpsServiceUtilities.registerForBroadcasts(this, gpsServiceBroadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);
    }

    private void setCenterCross() {
        String crossColorStr = mPeferences.getString(Constants.PREFS_KEY_CROSS_COLOR, "red"); //$NON-NLS-1$
        int crossColor = ColorUtilities.toColor(crossColorStr);
        String crossWidthStr = mPeferences.getString(Constants.PREFS_KEY_CROSS_WIDTH, "3"); //$NON-NLS-1$
        int crossThickness = 3;
        try {
            crossThickness = (int) Double.parseDouble(crossWidthStr);
        } catch (NumberFormatException e) {
            // ignore and use default
        }
        String crossSizeStr = mPeferences.getString(Constants.PREFS_KEY_CROSS_SIZE, "50"); //$NON-NLS-1$
        int crossLength = 20;
        try {
            crossLength = (int) Double.parseDouble(crossSizeStr);
        } catch (NumberFormatException e) {
            // ignore and use default
        }
        FrameLayout crossHor = (FrameLayout) findViewById(R.id.centerCrossHorizontal);
        FrameLayout crossVer = (FrameLayout) findViewById(R.id.centerCrossVertical);
        crossHor.setBackgroundColor(crossColor);
        ViewGroup.LayoutParams layHor = crossHor.getLayoutParams();
        layHor.width = crossLength;
        layHor.height = crossThickness;
        crossVer.setBackgroundColor(crossColor);
        ViewGroup.LayoutParams layVer = crossVer.getLayoutParams();
        layVer.width = crossThickness;
        layVer.height = crossLength;
    }

    @Override
    protected void onPause() {
        GPDialogs.dismissProgressDialog(syncProgressDialog);
        super.onPause();
    }

    @Override
    protected void onResume() {

        // notes type
        boolean doCustom = mPeferences.getBoolean(Constants.PREFS_KEY_NOTES_CHECK, true);
        if (doCustom) {
            String opacityStr = mPeferences.getString(Constants.PREFS_KEY_NOTES_OPACITY, "255"); //$NON-NLS-1$
            String sizeStr = mPeferences.getString(Constants.PREFS_KEY_NOTES_SIZE, DEFAULT_NOTES_SIZE + ""); //$NON-NLS-1$
            String colorStr = mPeferences.getString(Constants.PREFS_KEY_NOTES_CUSTOMCOLOR, ColorUtilities.BLUE.getHex()); //$NON-NLS-1$
            int noteSize = Integer.parseInt(sizeStr);
            float opacity = Integer.parseInt(opacityStr);

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
            notesDrawable = Compat.getDrawable(this, R.drawable.ic_place_accent_24dp);
        }

        mDataOverlay = new ArrayGeopaparazziOverlay(this);
        List<Overlay> overlays = mMapView.getOverlays();
        overlays.clear();
        overlays.add(mDataOverlay);

        super.onResume();
    }

    private void setTextScale() {
        String textSizeFactorStr = mPeferences.getString(Constants.PREFS_KEY_MAPSVIEW_TEXTSIZE_FACTOR, "1.0"); //$NON-NLS-1$
        float textSizeFactor = 1f;
        try {
            textSizeFactor = Float.parseFloat(textSizeFactorStr);
        } catch (NumberFormatException e) {
            // ignore
        }
        if (textSizeFactor < 0.5f) {
            textSizeFactor = 1f;
        }
        mMapView.setTextScale(textSizeFactor);
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

        if (mDataOverlay != null)
            mDataOverlay.dispose();

        if (mMapView != null) {
            MapGenerator mapGenerator = mMapView.getMapGenerator();
            if (mapGenerator != null) {
                mapGenerator.cleanup();
            }
        }

        super.onDestroy();
    }

    private void readData() {
        try {
            mDataOverlay.clearItems();
            mDataOverlay.clearWays();

            List<OverlayWay> logOverlaysList = DaoGpsLog.getGpslogOverlays();
            mDataOverlay.addWays(logOverlaysList);

            boolean imagesVisible = mPeferences.getBoolean(Constants.PREFS_KEY_IMAGES_VISIBLE, true);
            boolean notesVisible = mPeferences.getBoolean(Constants.PREFS_KEY_NOTES_VISIBLE, true);

            /* images */
            if (imagesVisible) {
                Drawable imageMarker = Compat.getDrawable(this, R.drawable.ic_images_48dp);
                Drawable newImageMarker = ArrayGeopaparazziOverlay.boundCenter(imageMarker);
                List<OverlayItem> imagesOverlaysList = DaoImages.getImagesOverlayList(newImageMarker, true);
                mDataOverlay.addItems(imagesOverlaysList);
            }

            /* gps notes */
            if (notesVisible) {
                notesDrawable.setBounds(notesDrawable.getIntrinsicWidth() , notesDrawable.getIntrinsicHeight() / -2, notesDrawable.getIntrinsicWidth() / 2,
                        notesDrawable.getIntrinsicHeight() / 2);
                Drawable newNotesMarker = ArrayGeopaparazziOverlay.boundCenter(notesDrawable);
                List<OverlayItem> noteOverlaysList = DaoNotes.getNoteOverlaysList(newNotesMarker);
                mDataOverlay.addItems(noteOverlaysList);
            }

            /* bookmarks */
            Drawable bookmarkMarker = Compat.getDrawable(this, R.drawable.ic_bookmarks_48dp);
            Drawable newBookmarkMarker = ArrayGeopaparazziOverlay.boundCenter(bookmarkMarker);
            List<OverlayItem> bookmarksOverlays = DaoBookmarks.getBookmarksOverlays(newBookmarkMarker);
            mDataOverlay.addItems(bookmarksOverlays);

            // read last known gps position
            if (lastGpsPosition != null) {
                GeoPoint geoPoint = toGeopoint((int) (lastGpsPosition[0] * E6), (int) (lastGpsPosition[1] * E6));
                if (geoPoint != null) {
                    mDataOverlay.setGpsPosition(geoPoint, 0f, lastGpsServiceStatus, lastGpsLoggingStatus);
                    mDataOverlay.requestRedraw();
                }
            }
            // mDataOverlay.requestRedraw();
        } catch (IOException e1) {
            GPLog.error(this, null, e1); //$NON-NLS-1$
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "onTouch issued with motionevent: " + action); //$NON-NLS-1$

        if (action == MotionEvent.ACTION_MOVE) {
            MapViewPosition mapPosition = mMapView.getMapPosition();
            GeoPoint mapCenter = mapPosition.getMapCenter();
            double lon = mapCenter.longitudeE6 / E6;
            double lat = mapCenter.latitudeE6 / E6;
            if (coordView != null) {
                coordView.setText(lonString + " " + COORDINATE_FORMATTER.format(lon) //
                        + "\n" + latString + " " + COORDINATE_FORMATTER.format(lat));
            }
        }
        if (action == MotionEvent.ACTION_UP) {
            if (coordView != null)
                coordView.setText("");
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
                            int zoom = mMapView.getMapPosition().getZoomLevel();
                            setZoom(zoom);
                        }
                    });
                }
            }).start();
        }
        return false;
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            double[] lastCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
            if (lastCenter != null)
                setNewCenterAtZoom(lastCenter[0], lastCenter[1], (int) lastCenter[2]);
            readData();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * Return current Zoom.
     *
     * @return integer current zoom level.
     */
    private int getZoom() {
        MapViewPosition mapPosition = mMapView.getMapPosition();
        byte zoom = mapPosition.getZoomLevel();
        return zoom;
    }

    public void setZoom(int zoom) {
        mMapView.getController().setZoom(zoom);
        setGuiZoomText(zoom);
        saveCenterPref();
    }

    private void setGuiZoomText(int newZoom) {
        zoomLevelText.setText(formatter.format(newZoom));
    }

    /**
     * @return the center [lon, lat]
     */
    public double[] getCenterLonLat() {
        MapViewPosition mapPosition = mMapView.getMapPosition();
        GeoPoint mapCenter = mapPosition.getMapCenter();
        double lon = mapCenter.longitudeE6 / E6;
        double lat = mapCenter.latitudeE6 / E6;
//        zoom = mapPosition.getZoomLevel();
        return new double[]{lon, lat};
    }

    public void setNewCenterAtZoom(double lon, double lat, int zoom) {
        GeoPoint geoPoint = toGeopoint(lon, lat);
        if (geoPoint != null) {
            mMapView.getController().setZoom(zoom);
            mMapView.getController().setCenter(geoPoint);
            setGuiZoomText(zoom);
            saveCenterPref(lon, lat, zoom);
        }
    }

    private GeoPoint toGeopoint(double lon, double lat) {
        try {
            return new GeoPoint(lat, lon);
        } catch (Exception e) {
            GPLog.error(this, "ERROR", e);
            return null;
        }
    }

    private GeoPoint toGeopoint(int lonE6, int latE6) {
        try {
            return new GeoPoint(latE6, lonE6);
        } catch (Exception e) {
            GPLog.error(this, "ERROR", e);
            return null;
        }
    }

    public void setNewCenter(double lon, double lat) {
        GeoPoint geoPoint = toGeopoint(lon, lat);
        if (geoPoint != null) {
            mMapView.getController().setCenter(geoPoint);
            saveCenterPref();
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect);//.setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_DATA, 2, R.string.base_maps);//.setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_SCALE_ID, 3, R.string.mapsactivity_menu_toggle_scalebar);//.setIcon(R.drawable.ic_menu_scalebar);
        menu.add(Menu.NONE, MENU_COMPASS_ID, 4, R.string.mapsactivity_menu_toggle_compass);//.setIcon(                android.R.drawable.ic_menu_compass);
        boolean centerOnGps = mPeferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
        if (centerOnGps) {
            menu.add(Menu.NONE, MENU_CENTER_ON_GPS, 6, R.string.disable_center_on_gps);//.setIcon(                    android.R.drawable.ic_menu_mylocation);
        } else {
            menu.add(Menu.NONE, MENU_CENTER_ON_GPS, 6, R.string.enable_center_on_gps);//.setIcon(                     android.R.drawable.ic_menu_mylocation);
        }

        menu.add(Menu.NONE, MENU_CENTER_ON_MAP, 7, R.string.center_on_map);//.setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(Menu.NONE, MENU_GO_TO, 8, R.string.go_to);//.setIcon(android.R.drawable.ic_menu_myplaces);
        menu.add(Menu.NONE, MENU_SHAREPOSITION_ID, 8, R.string.share_position);//.setIcon(android.R.drawable.ic_menu_send);
        menu.add(Menu.NONE, MENU_MIXARE_ID, 9, R.string.view_in_mixare);//.setIcon(R.drawable.icon_datasource);
        menu.add(Menu.NONE, MENU_LOADMAPSFORGE_VECTORS_ID, 9, getString(R.string.menu_extract_mapsforge_data));//"Import mapsforge data");//.setIcon(R.drawable.icon_datasource);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_GPSDATA:
                Intent gpsDatalistIntent = new Intent(this, GpsDataListActivity.class);
                startActivity(gpsDatalistIntent);
                return true;
            case MENU_DATA:
                Intent datalistIntent = new Intent(this, SpatialiteDatabasesTreeListActivity.class);
                startActivityForResult(datalistIntent, DATAPROPERTIES_RETURN_CODE);
                return true;
            case MENU_SCALE_ID:
                MapScaleBar mapScaleBar = mMapView.getMapScaleBar();
                boolean showMapScaleBar = mapScaleBar.isShowMapScaleBar();
                mapScaleBar.setShowMapScaleBar(!showMapScaleBar);
                return true;
            case MENU_COMPASS_ID:
                AppsUtilities.checkAndOpenGpsStatus(this);
                return true;
            case MENU_MIXARE_ID:
                if (!MixareHandler.isMixareInstalled(this)) {
                    MixareHandler.installMixareFromMarket(this);
                    return true;
                }

                try {
                    float[] nswe = getMapWorldBounds();
                    List<PointF3D> points = new ArrayList<>();
                    List<Bookmark> bookmarksList = DaoBookmarks.getBookmarksInWorldBounds(nswe[0], nswe[1], nswe[2], nswe[3]);
                    for (Bookmark bookmark : bookmarksList) {
                        double lat = bookmark.getLat();
                        double lon = bookmark.getLon();
                        String title = bookmark.getName();

                        PointF3D p = new PointF3D((float) lon, (float) lat, 0f, title);
                        points.add(p);
                    }
                    List<Note> notesList = DaoNotes.getNotesList(new float[]{nswe[0], nswe[1], nswe[2], nswe[3]}, false);
                    for (Note note : notesList) {
                        double lat = note.getLat();
                        double lon = note.getLon();
                        double elevation = note.getAltim();
                        String title = note.getName(); // note.getName() + " (" + note.getDescription() +

                        PointF3D p = new PointF3D((float) lon, (float) lat, (float) elevation, title);
                        points.add(p);
                    }
                    MixareHandler.runRegionOnMixare(this, points);
                    return true;
                } catch (Exception e1) {
                    GPLog.error(this, null, e1); //$NON-NLS-1$
                    return false;
                }
            case MENU_SHAREPOSITION_ID:
                try {
                    if (!NetworkUtilities.isNetworkAvailable(this)) {
                        GPDialogs.infoDialog(this, getString(R.string.available_only_with_network), null);
                    } else {
                        // sendData();
                        ShareUtilities.sharePositionUrl(this);
                    }
                    return true;
                } catch (Exception e1) {
                    GPLog.error(this, null, e1); //$NON-NLS-1$
                    return false;
                }
            case MENU_LOADMAPSFORGE_VECTORS_ID: {
                float[] mapWorldBounds = getMapWorldBounds();
                int currentZoomLevel = getZoom();
                Intent mapsforgeIntent = new Intent(this, ImportMapsforgeActivity.class);
                mapsforgeIntent.putExtra(LibraryConstants.NSWE, mapWorldBounds);
                mapsforgeIntent.putExtra(LibraryConstants.ZOOMLEVEL, currentZoomLevel);
                startActivity(mapsforgeIntent);
                return true;
            }
            case MENU_GO_TO: {
                return goTo();
            }
            case MENU_CENTER_ON_MAP: {
                AbstractSpatialTable selectedBaseMapTable = BaseMapSourcesManager.INSTANCE.getSelectedBaseMapTable();
                double lon = selectedBaseMapTable.getCenterX();
                double lat = selectedBaseMapTable.getCenterY();
                int zoom = selectedBaseMapTable.getDefaultZoom();
                setNewCenterAtZoom(lon, lat, zoom);
                return true;
            }
            case MENU_CENTER_ON_GPS: {
                boolean centerOnGps = mPeferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
                Editor edit = mPeferences.edit();
                edit.putBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, !centerOnGps);
                edit.apply();
                return true;
            }
            default:
        }
        return super.onContextItemSelected(item);
    }

    private boolean goTo() {
        String[] items = new String[]{getString(R.string.goto_coordinate), getString(R.string.geocoding)};

        new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();

                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        if (selectedPosition == 0) {
                            InsertCoordinatesDialogFragment insertCoordinatesDialogFragment = InsertCoordinatesDialogFragment.newInstance(null);
                            insertCoordinatesDialogFragment.show(getSupportFragmentManager(), "Insert Coord");
                        } else {
                            Intent intent = new Intent(MapviewActivity.this, GeocodeActivity.class);
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
        float n = nswe[0] / E6;
        float s = nswe[1] / E6;
        float w = nswe[2] / E6;
        float e = nswe[3] / E6;
        return new float[]{n, s, w, e};
    }

    /**
     * Retrieves the map world bounds in microdegrees.
     *
     * @return the [n,s,w,e] in midrodegrees.
     */
    private float[] getMapWorldBoundsE6() {
        Projection projection = mMapView.getProjection();
        int latitudeSpan = projection.getLatitudeSpan();
        int longitudeSpan = projection.getLongitudeSpan();
        MapViewPosition mapPosition = mMapView.getMapPosition();
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
            case (INSERTCOORD_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {

                    float[] routePoints = data.getFloatArrayExtra(ROUTE);
                    if (routePoints != null) {
                        // it is a routing request
                        try {
                            String name = data.getStringExtra(NAME);
                            if (name == null) {
                                name = "ROUTE_" + TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date()); //$NON-NLS-1$
                            }
                            DaoGpsLog logDumper = new DaoGpsLog();
                            SQLiteDatabase sqliteDatabase = logDumper.getDatabase();
                            long now = new java.util.Date().getTime();
                            long newLogId = logDumper.addGpsLog(now, now, 0, name, DEFAULT_LOG_WIDTH, ColorUtilities.BLUE.getHex(), true); //$NON-NLS-1$

                            sqliteDatabase.beginTransaction();
                            try {
                                long nowPlus10Secs = now;
                                for (int i = 0; i < routePoints.length; i = i + 2) {
                                    double lon = routePoints[i];
                                    double lat = routePoints[i + 1];
                                    double altim = -1;

                                    // dummy time increment
                                    nowPlus10Secs = nowPlus10Secs + 10000;
                                    logDumper.addGpsLogDataPoint(sqliteDatabase, newLogId, lon, lat, altim, nowPlus10Secs);
                                }

                                sqliteDatabase.setTransactionSuccessful();
                            } finally {
                                sqliteDatabase.endTransaction();
                            }
                        } catch (Exception e) {
                            GPLog.error(this, "Cannot draw route.", e); //$NON-NLS-1$
                        }

                    } else {
                        // it is a single point geocoding request
                        double lon = data.getDoubleExtra(LONGITUDE, 0d);
                        double lat = data.getDoubleExtra(LATITUDE, 0d);
                        setCenterAndZoomForMapWindowFocus(lon, lat, null);
                    }
                }
                break;
            }
            case (ZOOM_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    double lon = data.getDoubleExtra(LONGITUDE, 0d);
                    double lat = data.getDoubleExtra(LATITUDE, 0d);
                    int zoom = data.getIntExtra(ZOOMLEVEL, 1);
                    setCenterAndZoomForMapWindowFocus(lon, lat, zoom);
                }
                break;
            }
//            case (GPSDATAPROPERTIES_RETURN_CODE): {
//                if (resultCode == Activity.RESULT_OK) {
//                    double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
//                    double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
//                    setCenterAndZoomForMapWindowFocus(lon, lat, null);
//                }
//                break;
//            }
            case (DATAPROPERTIES_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    double lon = data.getDoubleExtra(LONGITUDE, 0d);
                    double lat = data.getDoubleExtra(LATITUDE, 0d);
                    int zoom = data.getIntExtra(ZOOMLEVEL, 1);
                    setCenterAndZoomForMapWindowFocus(lon, lat, zoom);
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
                                SmsUtilities.sendSMS(MapviewActivity.this, number, sms, false);
                                String msg = getString(R.string.sent_sms) + count++;
                                GPDialogs.toast(this, msg, Toast.LENGTH_SHORT);
                            }
                        }
                    }
                }
                break;
            }
            case (FORMUPDATE_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    FormInfoHolder formInfoHolder = (FormInfoHolder) data.getSerializableExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER);
                    if (formInfoHolder != null) {
                        try {
                            long noteId = formInfoHolder.noteId;
                            String nameStr = formInfoHolder.renderingLabel;
                            String jsonStr = formInfoHolder.sectionObjectString;

                            DaoNotes.updateForm(noteId, nameStr, jsonStr);
                        } catch (Exception e) {
                            GPLog.error(this, null, e);
                            GPDialogs.warningDialog(this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
                        }
                    }
                }
                break;
            }
            case (SELECTED_FEATURES_UPDATED_RETURN_CODE):
                if (resultCode == Activity.RESULT_OK) {
                    ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
                    if (activeToolGroup != null) {
                        if (activeToolGroup instanceof OnSelectionToolGroup) {
                            Bundle extras = data.getExtras();
                            ArrayList<Feature> featuresList = extras.getParcelableArrayList(FeatureUtilities.KEY_FEATURESLIST);
                            OnSelectionToolGroup selectionGroup = (OnSelectionToolGroup) activeToolGroup;
                            selectionGroup.setSelectedFeatures(featuresList);
                        }
                    }
                }
                break;
        }
    }

    private void addBookmark() {
        GeoPoint mapCenter = mMapView.getMapPosition().getMapCenter();
        final float centerLat = mapCenter.latitudeE6 / E6;
        final float centerLon = mapCenter.longitudeE6 / E6;

        final String newDate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date());
        final String proposedName = "bookmark " + newDate;

        String message = getString(R.string.mapsactivity_enter_bookmark_name);
        GPDialogs.inputMessageDialog(this, message, proposedName, new TextRunnable() {
            @Override
            public void run() {
                try {
                    if (theTextToRunOn.length() < 1) {
                        theTextToRunOn = proposedName;
                    }
                    int zoom = mMapView.getMapPosition().getZoomLevel();
                    float[] nswe = getMapWorldBounds();
                    DaoBookmarks.addBookmark(centerLon, centerLat, theTextToRunOn, zoom, nswe[0], nswe[1], nswe[2], nswe[3]);
                    readData();
                } catch (IOException e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    /**
     * Calls the mapview redraw.
     */
    public void invalidateMap() {
        mMapView.invalidateOnUiThread();
    }


    private boolean boundsContain(int latE6, int lonE6, int nE6, int sE6, int wE6, int eE6) {
        return lonE6 > wE6 && lonE6 < eE6 && latE6 > sE6 && latE6 < nE6;
    }

    /**
     * Save the current mapview position to preferences.
     *
     * @param lonLatZoom optional position + zoom. If null, position is taken from the mapview.
     */
    private synchronized void saveCenterPref(double... lonLatZoom) {
        double lon;
        double lat;
        int zoom;
        if (lonLatZoom != null && lonLatZoom.length == 3) {
            lon = lonLatZoom[0];
            lat = lonLatZoom[1];
            zoom = (int) lonLatZoom[2];
        } else {
            MapViewPosition mapPosition = mMapView.getMapPosition();
            GeoPoint mapCenter = mapPosition.getMapCenter();
            lon = mapCenter.longitudeE6 / E6;
            lat = mapCenter.latitudeE6 / E6;
            zoom = mapPosition.getZoomLevel();
        }

        if (GPLog.LOG_ABSURD) {
            StringBuilder sb = new StringBuilder();
            sb.append("Map Center moved: "); //$NON-NLS-1$
            sb.append(lon);
            sb.append("/"); //$NON-NLS-1$
            sb.append(lat);
            GPLog.addLogEntry(this, sb.toString());
        }

        PositionUtilities.putMapCenterInPreferences(mPeferences, lon, lat, zoom);

        EditManager.INSTANCE.invalidateEditingView();
    }

    /**
     * Set center coords and zoom ready for the {@link MapviewActivity} to focus again.
     * <p/>
     * <p>In {@link MapviewActivity} the {@link MapviewActivity#onWindowFocusChanged(boolean)}
     * will take care to zoom properly.
     *
     * @param centerX the lon coordinate. Can be <code>null</code>.
     * @param centerY the lat coordinate. Can be <code>null</code>.
     * @param zoom    the zoom. Can be <code>null</code>.
     */
    public void setCenterAndZoomForMapWindowFocus(Double centerX, Double centerY, Integer zoom) {
        MapViewPosition mapPosition = mMapView.getMapPosition();
        GeoPoint mapCenter = mapPosition.getMapCenter();
        int zoomLevel = mapPosition.getZoomLevel();
        float cx = 0f;
        float cy = 0f;
        if (centerX != null) {
            cx = centerX.floatValue();
        } else {
            cx = (float) (mapCenter.longitudeE6 / E6);
        }
        if (centerY != null) {
            cy = centerY.floatValue();
        } else {
            cy = (float) (mapCenter.latitudeE6 / E6);
        }
        if (zoom != null) {
            zoomLevel = zoom;
        }
        PositionUtilities.putMapCenterInPreferences(mPeferences, cx, cy, zoomLevel);
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
        lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);

        Resources resources = getResources();
        if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
            centerOnGps.setBackground(Compat.getDrawable(this, R.drawable.mapview_center_gps_red));
        } else {
            if (lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                centerOnGps.setBackground(Compat.getDrawable(this, R.drawable.mapview_center_gps_blue));
            } else {
                if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    centerOnGps.setBackground(Compat.getDrawable(this, R.drawable.mapview_center_gps_green));
                } else {
                    centerOnGps.setBackground(Compat.getDrawable(this, R.drawable.mapview_center_gps_orange));
                }
            }
        }
        if (lastGpsPosition == null) {
            return;
        }

        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
        float accuracy = 0;
        if (lastGpsPositionExtras != null) {
            accuracy = lastGpsPositionExtras[0];
        }

        if (this.mMapView.getWidth() <= 0 || this.mMapView.getWidth() <= 0) {
            return;
        }
        try {
            double lat = lastGpsPosition[1];
            double lon = lastGpsPosition[0];

            // send updates to the editing framework
            EditManager.INSTANCE.onGpsUpdate(lon, lat);

            float[] nsweE6 = getMapWorldBoundsE6();
            int latE6 = (int) ((float) lat * E6);
            int lonE6 = (int) ((float) lon * E6);
            boolean centerOnGps = mPeferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);

            int nE6 = (int) nsweE6[0];
            int sE6 = (int) nsweE6[1];
            int wE6 = (int) nsweE6[2];
            int eE6 = (int) nsweE6[3];

            // Rect bounds = new Rect(wE6, nE6, eE6, sE6);
            if (boundsContain(latE6, lonE6, nE6, sE6, wE6, eE6)) {
                GeoPoint point = toGeopoint(lonE6, latE6);
                if (point != null) {
                    mDataOverlay.setGpsPosition(point, accuracy, lastGpsServiceStatus, lastGpsLoggingStatus);
                    mDataOverlay.requestRedraw();
                }
            }

            Projection p = mMapView.getProjection();
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

    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.toggleEditingButton:
                Intent editableLayersIntent = new Intent(MapviewActivity.this, EditableLayersListActivity.class);
                startActivity(editableLayersIntent);
                return true;
            case R.id.addnotebytagbutton:
                Intent intent = new Intent(MapviewActivity.this, NotesListActivity.class);
                startActivityForResult(intent, ZOOM_RETURN_CODE);
                break;
            case R.id.addbookmarkbutton:
                Intent bookmarksListIntent = new Intent(MapviewActivity.this, BookmarksListActivity.class);
                startActivityForResult(bookmarksListIntent, ZOOM_RETURN_CODE);
                break;
            case R.id.menu_map_button:
                boolean areButtonsVisible = mPeferences.getBoolean(ARE_BUTTONSVISIBLE_OPEN, false);
                setAllButtoonsEnablement(!areButtonsVisible);
                Editor edit = mPeferences.edit();
                edit.putBoolean(ARE_BUTTONSVISIBLE_OPEN, !areButtonsVisible);
                edit.apply();
                return true;
            default:
                break;
        }
        return false;
    }

    public void onClick(View v) {
        boolean isInNonClickableMode;
        ImageButton toggleMeasuremodeButton;
        ImageButton toggleLoginfoButton;
//        ImageButton toggleViewingconeButton;
        switch (v.getId()) {
            case R.id.menu_map_button:
                FloatingActionButton menuButton = (FloatingActionButton) findViewById(R.id.menu_map_button);
                openContextMenu(menuButton);
                break;
            case R.id.zoomin:
                int currentZoom = getZoom();
                int newZoom = currentZoom + 1;
                int maxZoom = BaseMapSourcesManager.INSTANCE.getSelectedBaseMapTable().getMaxZoom();
                if (newZoom > maxZoom) newZoom = maxZoom;
                setZoom(newZoom);
                invalidateMap();
                Tool activeTool = EditManager.INSTANCE.getActiveTool();
                if (activeTool instanceof MapTool) {
                    ((MapTool) activeTool).onViewChanged();
                }
                break;
            case R.id.zoomout:
                currentZoom = getZoom();
                newZoom = currentZoom - 1;
                int minZoom = BaseMapSourcesManager.INSTANCE.getSelectedBaseMapTable().getMinZoom();
                if (newZoom < minZoom) newZoom = minZoom;
                setZoom(newZoom);
                invalidateMap();
                Tool activeTool1 = EditManager.INSTANCE.getActiveTool();
                if (activeTool1 instanceof MapTool) {
                    ((MapTool) activeTool1).onViewChanged();
                }
                break;
            case R.id.center_on_gps_btn:
                if (lastGpsPosition != null) {
                    setNewCenter(lastGpsPosition[0], lastGpsPosition[1]);
                }
                break;
            case R.id.addnotebytagbutton:
                // generate screenshot in background in order to not freeze
                try {
                    File tempDir = ResourcesManager.getInstance(MapviewActivity.this).getTempDir();
                    final File tmpImageFile = new File(tempDir, TMPPNGIMAGENAME);
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Rect t = new Rect();
                                mMapView.getDrawingRect(t);
                                Bitmap bufferedBitmap = Bitmap.createBitmap(t.width(), t.height(), Bitmap.Config.ARGB_8888);
                                Canvas bufferedCanvas = new Canvas(bufferedBitmap);
                                mMapView.draw(bufferedCanvas);
                                FileOutputStream out = new FileOutputStream(tmpImageFile);
                                bufferedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                                out.close();
                            } catch (Exception e) {
                                GPLog.error(this, null, e); //$NON-NLS-1$
                            }
                        }
                    }).start();
                    Intent mapTagsIntent = new Intent(MapviewActivity.this, AddNotesActivity.class);
                    startActivity(mapTagsIntent);
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                    GPDialogs.errorDialog(this, e, null);
                }
                break;
            case R.id.addbookmarkbutton:
                addBookmark();
                break;
            case R.id.togglemeasuremodebutton:
                isInNonClickableMode = !mMapView.isClickable();
                toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
                toggleLoginfoButton = (ImageButton) findViewById(R.id.toggleloginfobutton);
//                toggleViewingconeButton = (ImageButton) findViewById(R.id.toggleviewingconebutton);
                if (!isInNonClickableMode) {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.mapview_measuremode_on);
                    toggleLoginfoButton.setBackgroundResource(R.drawable.mapview_loginfo_off);
//                    toggleViewingconeButton.setBackgroundResource(R.drawable.mapview_viewingcone_off);

                    TapMeasureTool measureTool = new TapMeasureTool(mMapView);
                    EditManager.INSTANCE.setActiveTool(measureTool);
                } else {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.mapview_measuremode_off);

                    EditManager.INSTANCE.setActiveTool(null);
                }
                break;
            case R.id.toggleloginfobutton:
                isInNonClickableMode = !mMapView.isClickable();
                toggleLoginfoButton = (ImageButton) findViewById(R.id.toggleloginfobutton);
                toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
//                toggleViewingconeButton = (ImageButton) findViewById(R.id.toggleviewingconebutton);
                if (!isInNonClickableMode) {
                    toggleLoginfoButton.setBackgroundResource(R.drawable.mapview_loginfo_on);
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.mapview_measuremode_off);
//                    toggleViewingconeButton.setBackgroundResource(R.drawable.mapview_viewingcone_off);

                    try {
                        GpsLogInfoTool measureTool = new GpsLogInfoTool(mMapView);
                        EditManager.INSTANCE.setActiveTool(measureTool);
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                    }
                } else {
                    toggleLoginfoButton.setBackgroundResource(R.drawable.mapview_loginfo_off);
                    EditManager.INSTANCE.setActiveTool(null);
                }
                break;
//            case R.id.toggleviewingconebutton:
//                if (lastGpsPosition != null) {
//                    setNewCenter(lastGpsPosition[0], lastGpsPosition[1]);
//
//                    isInNonClickableMode = !mMapView.isClickable();
//                    toggleLoginfoButton = (ImageButton) findViewById(R.id.toggleloginfobutton);
//                    toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
//                    toggleViewingconeButton = (ImageButton) findViewById(R.id.toggleviewingconebutton);
//                    if (!isInNonClickableMode) {
//                        toggleViewingconeButton.setBackgroundResource(R.drawable.mapview_viewingcone_on);
//                        toggleLoginfoButton.setBackgroundResource(R.drawable.mapview_loginfo_off);
//                        toggleMeasuremodeButton.setBackgroundResource(R.drawable.mapview_measuremode_off);
//
//                        try {
//                            ViewingConeTool viewingConeTool = new ViewingConeTool(mMapView, this);
//                            EditManager.INSTANCE.setActiveTool(viewingConeTool);
//                        } catch (Exception e) {
//                            GPLog.error(this, null, e);
//                        }
//                    } else {
//                        toggleViewingconeButton.setBackgroundResource(R.drawable.mapview_viewingcone_off);
//                        EditManager.INSTANCE.setActiveTool(null);
//                    }
//                }else{
//                    GPDialogs.warningDialog(this,getString(R.string.warning_viewcone_gps) ,null);
//                }
//                break;
            case R.id.toggleEditingButton:
                toggleEditing();
                break;

            default:
                break;
        }
    }

    private void toggleEditing() {
        final ImageButton toggleEditingButton = (ImageButton) findViewById(R.id.toggleEditingButton);
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup == null) {
            toggleEditingButton.setBackgroundResource(R.drawable.mapview_toggle_editing_on);
            ILayer editLayer = EditManager.INSTANCE.getEditLayer();
            if (editLayer == null) {
                // if not layer is
                activeToolGroup = new NoEditableLayerToolGroup(mMapView);
//                GPDialogs.warningDialog(this, getString(R.string.no_editable_layer_set), null);
//                return;
            } else if (editLayer.isPolygon())
                activeToolGroup = new PolygonMainEditingToolGroup(mMapView);
            else if (editLayer.isLine())
                activeToolGroup = new LineMainEditingToolGroup(mMapView);
            else if (editLayer.isPoint())
                activeToolGroup = new PointMainEditingToolGroup(mMapView);
            EditManager.INSTANCE.setActiveToolGroup(activeToolGroup);
            setLeftButtoonsEnablement(false);
        } else {
            toggleEditingButton.setBackgroundResource(R.drawable.mapview_toggle_editing_off);
            EditManager.INSTANCE.setActiveTool(null);
            EditManager.INSTANCE.setActiveToolGroup(null);
            setLeftButtoonsEnablement(true);
        }
    }

    private void setLeftButtoonsEnablement(boolean enable) {
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        ImageButton toggleLoginfoButton = (ImageButton) findViewById(R.id.toggleloginfobutton);
        ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        if (enable) {
            addnotebytagButton.setVisibility(View.VISIBLE);
            addBookmarkButton.setVisibility(View.VISIBLE);
            toggleLoginfoButton.setVisibility(View.VISIBLE);
            toggleMeasuremodeButton.setVisibility(View.VISIBLE);
        } else {
            addnotebytagButton.setVisibility(View.GONE);
            addBookmarkButton.setVisibility(View.GONE);
            toggleLoginfoButton.setVisibility(View.GONE);
            toggleMeasuremodeButton.setVisibility(View.GONE);
        }
    }

    private void setAllButtoonsEnablement(boolean enable) {
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        ImageButton toggleLoginfoButton = (ImageButton) findViewById(R.id.toggleloginfobutton);
        ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        ImageButton zoomInButton = (ImageButton) findViewById(R.id.zoomin);
        TextView zoomLevelTextview = (TextView) findViewById(R.id.zoomlevel);
        ImageButton zoomOutButton = (ImageButton) findViewById(R.id.zoomout);
        ImageButton toggleEditingButton = (ImageButton) findViewById(R.id.toggleEditingButton);

        int visibility = View.VISIBLE;
        if (!enable) {
            visibility = View.GONE;
        }
        addnotebytagButton.setVisibility(visibility);
        addBookmarkButton.setVisibility(visibility);
        toggleLoginfoButton.setVisibility(visibility);
        toggleMeasuremodeButton.setVisibility(visibility);
        batteryButton.setVisibility(visibility);
        centerOnGps.setVisibility(visibility);
        zoomInButton.setVisibility(visibility);
        zoomLevelTextview.setVisibility(visibility);
        zoomOutButton.setVisibility(visibility);
        toggleEditingButton.setVisibility(visibility);
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

    @Override
    public void onCoordinateInserted(double lon, double lat) {
        setNewCenter(lon, lat);
    }
}
