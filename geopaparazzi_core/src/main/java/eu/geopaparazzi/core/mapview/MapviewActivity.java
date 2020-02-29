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
package eu.geopaparazzi.core.mapview;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoBookmarks;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.ui.activities.AddNotesActivity;
import eu.geopaparazzi.core.ui.activities.BookmarksListActivity;
import eu.geopaparazzi.core.ui.activities.GpsDataListActivity;
import eu.geopaparazzi.core.ui.activities.NotesListActivity;
import eu.geopaparazzi.core.utilities.Constants;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.activities.GeocodeActivity;
import eu.geopaparazzi.library.core.dialogs.InsertCoordinatesDialogFragment;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormInfoHolder;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.share.ShareUtilities;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.map.GPBBox;
import eu.geopaparazzi.map.GPMapPosition;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.MapsSupportService;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.editing.EditingView;
import eu.geopaparazzi.map.features.tools.MapTool;
import eu.geopaparazzi.map.features.tools.impl.LineMainEditingToolGroup;
import eu.geopaparazzi.map.features.tools.impl.NoEditableLayerToolGroup;
import eu.geopaparazzi.map.features.tools.impl.OnSelectionToolGroup;
import eu.geopaparazzi.map.features.tools.impl.PointMainEditingToolGroup;
import eu.geopaparazzi.map.features.tools.impl.PolygonMainEditingToolGroup;
import eu.geopaparazzi.map.features.tools.interfaces.Tool;
import eu.geopaparazzi.map.features.tools.interfaces.ToolGroup;
import eu.geopaparazzi.map.gui.MapLayerListActivity;
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IEditableLayer;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;
import eu.geopaparazzi.map.layers.interfaces.ILabeledLayer;
import eu.geopaparazzi.map.layers.systemlayers.BookmarkLayer;
import eu.geopaparazzi.map.layers.systemlayers.NotesLayer;
import eu.geopaparazzi.map.utils.MapUtilities;

import static eu.geopaparazzi.library.util.LibraryConstants.COORDINATE_FORMATTER;
import static eu.geopaparazzi.library.util.LibraryConstants.DEFAULT_LOG_WIDTH;
import static eu.geopaparazzi.library.util.LibraryConstants.LATITUDE;
import static eu.geopaparazzi.library.util.LibraryConstants.LONGITUDE;
import static eu.geopaparazzi.library.util.LibraryConstants.NAME;
import static eu.geopaparazzi.library.util.LibraryConstants.ROUTE;
import static eu.geopaparazzi.library.util.LibraryConstants.TMPPNGIMAGENAME;
import static eu.geopaparazzi.library.util.LibraryConstants.ZOOMLEVEL;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapviewActivity extends AppCompatActivity implements IActivitySupporter, OnTouchListener, OnClickListener, OnLongClickListener, InsertCoordinatesDialogFragment.IInsertCoordinateListener, GPMapView.GPMapUpdateListener {
    private final int INSERTCOORD_RETURN_CODE = 666;
    private final int ZOOM_RETURN_CODE = 667;
    private final int MENU_GO_TO = 1;
    private final int MENU_COMPASS_ID = 2;
    private final int MENU_SHAREPOSITION_ID = 3;

    private static final String ARE_BUTTONSVISIBLE_OPEN = "ARE_BUTTONSVISIBLE_OPEN"; //$NON-NLS-1$
    public static final String MAPSCALE_X = "MAPSCALE_X"; //$NON-NLS-1$
    public static final String MAPSCALE_Y = "MAPSCALE_Y"; //$NON-NLS-1$
    private DecimalFormat formatter = new DecimalFormat("00"); //$NON-NLS-1$
    private GPMapView mapView;
    private SharedPreferences mPeferences;


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

    private ImageButton centerOnGps;
    private ImageButton batteryButton;
    private BroadcastReceiver mapsSupportBroadcastReceiver;
    private TextView coordView;
    private String latString;
    private String lonString;
    private TextView batteryText;
    private ImageButton toggleEditingButton;
    private ImageButton toggleLabelsButton;
    private boolean hasLabelledLayers;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_mapview);

        mapsSupportBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST)) {
                    boolean centerOnPosition = intent.getBooleanExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST, false);
                    if (centerOnPosition) {
                        double lon = intent.getDoubleExtra(LONGITUDE, 0.0);
                        double lat = intent.getDoubleExtra(LATITUDE, 0.0);
                        setNewCenter(lon, lat);
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
        coordView = findViewById(R.id.coordsText);
        latString = getString(R.string.lat);
        lonString = getString(R.string.lon);

        // CENTER CROSS
        setCenterCross();

        // FLOATING BUTTONS
        FloatingActionButton menuButton = findViewById(R.id.menu_map_button);
        menuButton.setOnClickListener(this);
        menuButton.setOnLongClickListener(this);
        registerForContextMenu(menuButton);

        FloatingActionButton layerButton = findViewById(R.id.layers_map_button);
        layerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapTagsIntent = new Intent(MapviewActivity.this, MapLayerListActivity.class);
                startActivity(mapTagsIntent);
            }
        });


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
        mapView = new GPMapView(this);
        mapView.setClickable(true);
        mapView.setOnTouchListener(this);

        float mapScaleX = mPeferences.getFloat(MAPSCALE_X, 1f);
        float mapScaleY = mPeferences.getFloat(MAPSCALE_Y, 1f);
        if (mapScaleX > 1 || mapScaleY > 1) {
            mapView.setScaleX(mapScaleX);
            mapView.setScaleY(mapScaleY);
        }

        setTextScale();

        final RelativeLayout rl = findViewById(R.id.innerlayout);
        rl.addView(mapView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        ImageButton zoomInButton = findViewById(R.id.zoomin);
        zoomInButton.setOnClickListener(this);
        zoomInButton.setOnLongClickListener(this);

        zoomLevelText = findViewById(R.id.zoomlevel);

        ImageButton zoomOutButton = findViewById(R.id.zoomout);
        zoomOutButton.setOnClickListener(this);
        zoomOutButton.setOnLongClickListener(this);

        batteryButton = findViewById(R.id.battery);
        batteryText = findViewById(R.id.batterytext);

        centerOnGps = findViewById(R.id.center_on_gps_btn);
        centerOnGps.setOnClickListener(this);
        centerOnGps.setOnLongClickListener(this);

        ImageButton addnotebytagButton = findViewById(R.id.addnotebytagbutton);
        addnotebytagButton.setOnClickListener(this);
        addnotebytagButton.setOnLongClickListener(this);

        ImageButton addBookmarkButton = findViewById(R.id.addbookmarkbutton);
        addBookmarkButton.setOnClickListener(this);
        addBookmarkButton.setOnLongClickListener(this);

        final ImageButton toggleMeasuremodeButton = findViewById(R.id.togglemeasuremodebutton);
        toggleMeasuremodeButton.setOnClickListener(this);
        toggleMeasuremodeButton.setOnLongClickListener(this);

        final ImageButton toggleLogInfoButton = findViewById(R.id.toggleloginfobutton);
        toggleLogInfoButton.setOnClickListener(this);
        toggleLogInfoButton.setOnLongClickListener(this);

        toggleEditingButton = findViewById(R.id.toggleEditingButton);
        toggleEditingButton.setOnClickListener(this);

        toggleLabelsButton = findViewById(R.id.toggleLabels);
        toggleLabelsButton.setOnClickListener(this);

        if (mapCenterLocation != null)
            setNewCenterAtZoom(mapCenterLocation[0], mapCenterLocation[1], (int) mapCenterLocation[2]);

        setAllButtoonsEnablement(areButtonsVisible);
        batteryText.setVisibility(areButtonsVisible ? View.VISIBLE : View.INVISIBLE);
        EditingView editingView = findViewById(R.id.editingview);
        LinearLayout editingToolsLayout = findViewById(R.id.editingToolsLayout);
        EditManager.INSTANCE.setEditingView(editingView, editingToolsLayout);
        mapView.setEditingView(editingView);

        GpsServiceUtilities.registerForBroadcasts(this, gpsServiceBroadcastReceiver);
        GpsServiceUtilities.triggerBroadcast(this);


        mapView.addMapUpdateListener(this);

    }

    @Override
    public void onUpdate(GPMapPosition mapPosition) {
        setGuiZoomText(mapPosition.getZoomLevel(), (int) mapView.getScaleX());
    }

    /**
     * Returns the relative size of a map view in relation to the screen size of the device. This
     * is used for cache size calculations.
     * By default this returns 1.0, for a full size map view.
     *
     * @return the screen ratio of the mapview
     */
    private float getScreenRatio() {
        return 1f;
    }

    private void setCenterCross() {
        String crossColorStr = mPeferences.getString(Constants.PREFS_KEY_CROSS_COLOR, "red"); //$NON-NLS-1$
        int crossColor = ColorUtilities.toColor(crossColorStr);
        String crossWidthStr = mPeferences.getString(Constants.PREFS_KEY_CROSS_WIDTH, "3"); //$NON-NLS-1$
        int crossThickness = 3;
        try {
            crossThickness = (int) Double.parseDouble(Objects.requireNonNull(crossWidthStr));
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
        FrameLayout crossHor = findViewById(R.id.centerCrossHorizontal);
        FrameLayout crossVer = findViewById(R.id.centerCrossVertical);
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
        if (mapView != null) {
            LayerManager.INSTANCE.onPause(mapView);
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mapView != null) {
            mapView.onResume();
            LayerManager.INSTANCE.onResume(mapView, this);

            GPMapPosition mapPosition = mapView.getMapPosition();
            setNewCenter(mapPosition.getLongitude() + 0.000001, mapPosition.getLatitude() + 0.000001);
        }
        checkLabelButton();

        disableEditing();

        super.onResume();
    }

    private void checkLabelButton() {
        hasLabelledLayers = false;
        List<IGpLayer> layers = mapView.getLayers();
        for (IGpLayer layer : layers) {
            if (layer instanceof ILabeledLayer) {
                hasLabelledLayers = true;
                break;
            }
        }
        if (!hasLabelledLayers) {
            toggleLabelsButton.setVisibility(View.GONE);
        } else {
            toggleLabelsButton.setVisibility(View.VISIBLE);
        }
    }

    private void setTextScale() {
//        String textSizeFactorStr = mPeferences.getString(Constants.PREFS_KEY_MAPSVIEW_TEXTSIZE_FACTOR, "1.0"); //$NON-NLS-1$
//        float textSizeFactor = 1f;
//        try {
//            textSizeFactor = Float.parseFloat(textSizeFactorStr);
//        } catch (NumberFormatException e) {
//            // ignore
//        }
//        if (textSizeFactor < 0.5f) {
//            textSizeFactor = 1f;
//        }
//        mapView.setTextScale(textSizeFactor);
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


        try {
            LayerManager.INSTANCE.dispose(mapView);
        } catch (JSONException e) {
            GPLog.error(this, null, e);
        }
        if (mapView != null) {
            mapView.destroyAll();
        }

        super.onDestroy();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "onTouch issued with motionevent: " + action); //$NON-NLS-1$

        GPMapPosition mapPosition = mapView.getMapPosition();
        if (action == MotionEvent.ACTION_MOVE) {
            double lon = mapPosition.getLongitude();
            double lat = mapPosition.getLatitude();
            if (coordView != null) {
                coordView.setText(lonString + " " + COORDINATE_FORMATTER.format(lon) //
                        + "\n" + latString + " " + COORDINATE_FORMATTER.format(lat));
            }
        }
        if (action == MotionEvent.ACTION_UP) {
            if (coordView != null)
                coordView.setText("");
            saveCenterPref();
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
        }
        super.onWindowFocusChanged(hasFocus);
    }

    /**
     * Return current Zoom.
     *
     * @return integer current zoom level.
     */
    private int getZoom() {
        GPMapPosition mapPosition = mapView.getMapPosition();
        return (byte) mapPosition.getZoomLevel();
    }

    public void setZoom(int zoom) {
        GPMapPosition mapPosition = mapView.getMapPosition();
        mapPosition.setZoomLevel(zoom);
        mapView.setMapPosition(mapPosition);
        saveCenterPref();
    }

    private void setGuiZoomText(int newZoom, int newScale) {
        String scalePart = "";
        if (newScale > 1)
            scalePart = "*" + newScale;
        String text = formatter.format(newZoom) + scalePart;
        zoomLevelText.setText(text);
    }

    public void setNewCenterAtZoom(double lon, double lat, int zoom) {
        GPMapPosition mapPosition = mapView.getMapPosition();
        mapPosition.setZoomLevel(zoom);
        mapPosition.setPosition(lat, lon);
        mapView.setMapPosition(mapPosition);

        saveCenterPref(lon, lat, zoom);
    }


    public void setNewCenter(double lon, double lat) {
        GPMapPosition mapPosition = mapView.getMapPosition();
        mapPosition.setPosition(lat, lon);
        mapView.setMapPosition(mapPosition);

        saveCenterPref();
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.NONE, MENU_COMPASS_ID, 1, R.string.mapsactivity_menu_toggle_compass);//.setIcon(                android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_GO_TO, 2, R.string.go_to);//.setIcon(android.R.drawable.ic_menu_myplaces);
        menu.add(Menu.NONE, MENU_SHAREPOSITION_ID, 3, R.string.share_position);//.setIcon(android.R.drawable.ic_menu_send);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_COMPASS_ID:
                AppsUtilities.checkAndOpenGpsStatus(this);
                return true;
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
            case MENU_GO_TO: {
                return goTo();
            }
            default:
        }
        return super.onContextItemSelected(item);
    }

    private boolean goTo() {
        String[] items = new String[]{getString(R.string.goto_coordinate), getString(R.string.geocoding)};
        boolean[] checked = new boolean[2];
        GPDialogs.singleOptionDialog(this, items, checked, () -> runOnUiThread(() -> {
            int selectedPosition = checked[1] ? 1 : 0;
            if (selectedPosition == 0) {
                InsertCoordinatesDialogFragment insertCoordinatesDialogFragment = InsertCoordinatesDialogFragment.newInstance(null);
                insertCoordinatesDialogFragment.show(getSupportFragmentManager(), "Insert Coord"); //NON-NLS
            } else {
                Intent intent = new Intent(MapviewActivity.this, GeocodeActivity.class);
                startActivityForResult(intent, INSERTCOORD_RETURN_CODE);
            }
        }));
        return true;
    }

    /**
     * Retrieves the map world bounds in degrees.
     *
     * @return the [n,s,w,e] in degrees.
     */
    private double[] getMapWorldBounds() {
        GPBBox bbox = mapView.getBoundingBox();
        double[] nswe = {bbox.getMaxLatitude(), bbox.getMinLatitude(), bbox.getMinLongitude(), bbox.getMaxLongitude()};
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
            case (NotesLayer.FORMUPDATE_RETURN_CODE): {
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
            case (MapUtilities.SELECTED_FEATURES_UPDATED_RETURN_CODE):
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
        GPMapPosition mapPosition = mapView.getMapPosition();
        final double centerLat = mapPosition.getLatitude();
        final double centerLon = mapPosition.getLongitude();

        final String newDate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date());
        final String proposedName = "bookmark " + newDate;//NON-NLS

        String message = getString(R.string.mapsactivity_enter_bookmark_name);
        GPDialogs.inputMessageDialog(this, message, proposedName, new TextRunnable() {
            @Override
            public void run() {
                try {
                    if (theTextToRunOn.length() < 1) {
                        theTextToRunOn = proposedName;
                    }
                    int zoom = mapPosition.getZoomLevel();
                    DaoBookmarks.addBookmark(centerLon, centerLat, theTextToRunOn, zoom);
                    mapView.reloadLayer(BookmarkLayer.class);
                } catch (Exception e) {
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

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
            GPMapPosition mapPosition = mapView.getMapPosition();
            lat = mapPosition.getLatitude();
            lon = mapPosition.getLongitude();
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
        GPMapPosition mapPosition = mapView.getMapPosition();

        int zoomLevel = mapPosition.getZoomLevel();
        double cx = 0f;
        double cy = 0f;
        if (centerX != null) {
            cx = centerX.floatValue();
        } else {
            cx = mapPosition.getLongitude();
        }
        if (centerY != null) {
            cy = centerY.floatValue();
        } else {
            cy = mapPosition.getLatitude();
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
        batteryText.setText(sb.toString());
    }

    private void onGpsServiceUpdate(Intent intent) {
        GpsServiceStatus lastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        GpsLoggingStatus lastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);


        if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
            centerOnGps.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_center_gps_red_24dp));
        } else {
            if (lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                centerOnGps.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_center_gps_blue_24dp));
            } else {
                if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    centerOnGps.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_center_gps_green_24dp));
                } else {
                    centerOnGps.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_center_gps_orange_24dp));
                }
            }
        }
        if (lastGpsPosition == null) {
            return;
        }

        float[] lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
        int[] lastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);

        mapView.setGpsStatus(lastGpsServiceStatus, lastGpsPosition, lastGpsPositionExtras, lastGpsStatusExtras, lastGpsLoggingStatus);


        if (mapView.getViewportWidth() <= 0 || mapView.getViewportWidth() <= 0) {
            return;
        }
        try {
            double lat = lastGpsPosition[1];
            double lon = lastGpsPosition[0];

            // send updates to the editing framework
            EditManager.INSTANCE.onGpsUpdate(lon, lat);


        } catch (Exception e) {
            GPLog.error(this, "On location change error", e); //$NON-NLS-1$
        }
    }

    public boolean onLongClick(View v) {
        int i = v.getId();
        if (i == R.id.addnotebytagbutton) {
            Intent intent = new Intent(MapviewActivity.this, NotesListActivity.class);
            intent.putExtra(LibraryConstants.PREFS_KEY_MAP_ZOOM, true);
            startActivityForResult(intent, ZOOM_RETURN_CODE);
        } else if (i == R.id.toggleloginfobutton) {
            Intent gpsDatalistIntent = new Intent(this, GpsDataListActivity.class);
            startActivity(gpsDatalistIntent);
        } else if (i == R.id.addbookmarkbutton) {
            Intent bookmarksListIntent = new Intent(MapviewActivity.this, BookmarksListActivity.class);
            startActivityForResult(bookmarksListIntent, ZOOM_RETURN_CODE);
        } else if (i == R.id.menu_map_button) {
            boolean areButtonsVisible = mPeferences.getBoolean(ARE_BUTTONSVISIBLE_OPEN, false);
            setAllButtoonsEnablement(!areButtonsVisible);
            batteryText.setVisibility(!areButtonsVisible ? View.VISIBLE : View.INVISIBLE);
            Editor edit = mPeferences.edit();
            edit.putBoolean(ARE_BUTTONSVISIBLE_OPEN, !areButtonsVisible);
            edit.apply();
            return true;
        } else if (i == R.id.zoomin) {
            float scaleX1 = mapView.getScaleX() * 2;
            float scaleY1 = mapView.getScaleY() * 2;
            mapView.setScaleX(scaleX1);
            mapView.setScaleY(scaleY1);
            Editor edit1 = mPeferences.edit();
            edit1.putFloat(MAPSCALE_X, scaleX1);
            edit1.putFloat(MAPSCALE_Y, scaleY1);
            edit1.apply();
            return true;
        } else if (i == R.id.zoomout) {
            float scaleX2 = mapView.getScaleX();
            float scaleY2 = mapView.getScaleY();
            if (scaleX2 > 1 && scaleY2 > 1) {
                scaleX2 = scaleX2 / 2;
                scaleY2 = scaleY2 / 2;
                mapView.setScaleX(scaleX2);
                mapView.setScaleY(scaleY2);
            }

            Editor edit2 = mPeferences.edit();
            edit2.putFloat(MAPSCALE_X, scaleX2);
            edit2.putFloat(MAPSCALE_Y, scaleY2);
            edit2.apply();
            return true;
        } else if (i == R.id.center_on_gps_btn) {
            Context context = getContext();
            String[] items = new String[]{context.getString(R.string.option_center_on_gps), context.getString(R.string.option_rotate_with_bearing), context.getString(R.string.option_show_gps_info), context.getString(R.string.option_hide_gps_accuracy)};
            boolean[] checkedItems = new boolean[items.length];
            checkedItems[0] = mPeferences.getBoolean(LibraryConstants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
            checkedItems[1] = mPeferences.getBoolean(LibraryConstants.PREFS_KEY_ROTATE_MAP_WITH_GPS, false);
            checkedItems[2] = mPeferences.getBoolean(LibraryConstants.PREFS_KEY_SHOW_GPS_INFO, false);
            checkedItems[3] = mPeferences.getBoolean(LibraryConstants.PREFS_KEY_IGNORE_GPS_ACCURACY, false);

            DialogInterface.OnMultiChoiceClickListener dialogListener = (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;

                if (which == 0) {
                    // check center on gps
                    boolean centerOnGps = checkedItems[0];
                    Editor edit = mPeferences.edit();
                    edit.putBoolean(LibraryConstants.PREFS_KEY_AUTOMATIC_CENTER_GPS, centerOnGps);
                    edit.apply();
                } else if (which == 1) {
                    // check rotate map
                    boolean rotateMapWithGps = checkedItems[1];
                    Editor edit = mPeferences.edit();
                    edit.putBoolean(LibraryConstants.PREFS_KEY_ROTATE_MAP_WITH_GPS, rotateMapWithGps);
                    edit.apply();
                } else if (which == 2) {
                    // check show info
                    boolean showGpsInfo = checkedItems[2];
                    Editor edit = mPeferences.edit();
                    edit.putBoolean(LibraryConstants.PREFS_KEY_SHOW_GPS_INFO, showGpsInfo);
                    edit.apply();

                    mapView.toggleLocationTextLayer(showGpsInfo);
                } else {
                    // check ignore gps
                    boolean ignoreAccuracy = checkedItems[3];
                    Editor edit = mPeferences.edit();
                    edit.putBoolean(LibraryConstants.PREFS_KEY_IGNORE_GPS_ACCURACY, ignoreAccuracy);
                    edit.apply();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("");
            builder.setMultiChoiceItems(items, checkedItems, dialogListener);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        return false;
    }

    public void onClick(View v) {
        boolean isInNonClickableMode = !mapView.isClickable();
        ImageButton toggleLoginfoButton = findViewById(R.id.toggleloginfobutton);
        ImageButton toggleMeasuremodeButton = findViewById(R.id.togglemeasuremodebutton);
//        ImageButton toggleViewingconeButton;
        int i = v.getId();
        if (i == R.id.menu_map_button) {
            FloatingActionButton menuButton = findViewById(R.id.menu_map_button);
            openContextMenu(menuButton);

        } else if (i == R.id.zoomin) {
            int currentZoom = getZoom();
            int newZoom = currentZoom + 1;
            int maxZoom = 24;
            if (newZoom > maxZoom) newZoom = maxZoom;
            setZoom(newZoom);
            Tool activeTool = EditManager.INSTANCE.getActiveTool();
            if (activeTool instanceof MapTool) {
                ((MapTool) activeTool).onViewChanged();
            }

        } else if (i == R.id.zoomout) {
            int newZoom;
            int currentZoom;
            currentZoom = getZoom();
            newZoom = currentZoom - 1;
            int minZoom = 0;
            if (newZoom < minZoom) newZoom = minZoom;
            setZoom(newZoom);
            Tool activeTool1 = EditManager.INSTANCE.getActiveTool();
            if (activeTool1 instanceof MapTool) {
                ((MapTool) activeTool1).onViewChanged();
            }

        } else if (i == R.id.center_on_gps_btn) {
            if (lastGpsPosition != null) {
                setNewCenter(lastGpsPosition[0], lastGpsPosition[1]);
            }

        } else if (i == R.id.addnotebytagbutton) {// generate screenshot in background in order to not freeze
            try {
                File tempDir = ResourcesManager.getInstance(MapviewActivity.this).getTempDir();
                final File tmpImageFile = new File(tempDir, TMPPNGIMAGENAME);
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            mapView.saveMapView(tmpImageFile);
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

        } else if (i == R.id.addbookmarkbutton) {
            addBookmark();
        } else if (i == R.id.togglemeasuremodebutton) {
            if (!isInNonClickableMode) {
                toggleMeasuremodeButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_measuremode_on_24dp));
                toggleLoginfoButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_loginfo_off_24dp));
                toggleLabelsButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_toggle_labels_off_24dp));
                TapMeasureTool measureTool = new TapMeasureTool(mapView);
                EditManager.INSTANCE.setActiveTool(measureTool);
            } else {
                toggleMeasuremodeButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_measuremode_off_24dp));
                EditManager.INSTANCE.setActiveTool(null);
            }
        } else if (i == R.id.toggleloginfobutton) {
            if (!isInNonClickableMode) {
                toggleLoginfoButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_loginfo_on_24dp));
                toggleMeasuremodeButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_measuremode_off_24dp));
                toggleLabelsButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_toggle_labels_off_24dp));
                try {
                    GpsLogInfoTool measureTool = new GpsLogInfoTool(mapView);
                    EditManager.INSTANCE.setActiveTool(measureTool);
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }
                mapView.blockMap();
            } else {
                toggleLoginfoButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_loginfo_off_24dp));
                EditManager.INSTANCE.setActiveTool(null);
                mapView.releaseMapBlock();
            }
        } else if (i == R.id.toggleEditingButton) {
            toggleEditing();
        } else if (i == R.id.toggleLabels) {
            Tool activeTool = EditManager.INSTANCE.getActiveTool();
            if (activeTool instanceof PanLabelsTool) {
                toggleLabelsButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_toggle_labels_off_24dp));
                EditManager.INSTANCE.setActiveTool(null);
                mapView.releaseMapBlock();
            } else {
                toggleLabelsButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_toggle_labels_on_24dp));
                toggleMeasuremodeButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_measuremode_off_24dp));
                toggleLoginfoButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_loginfo_off_24dp));

                PanLabelsTool panLabelsTool = new PanLabelsTool(mapView);
                EditManager.INSTANCE.setActiveTool(panLabelsTool);
                mapView.blockMap();
            }
        }
    }

    private void toggleEditing() {
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        boolean isEditing = activeToolGroup != null;

        checkLabelButton();

        if (isEditing) {
            disableEditing();
            mapView.releaseMapBlock();
        } else {
            toggleEditingButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_toggle_editing_on_24dp));
            IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();
            if (editLayer == null) {
                // if not layer is
                activeToolGroup = new NoEditableLayerToolGroup(mapView);
//                GPDialogs.warningDialog(this, getString(R.string.no_editable_layer_set), null);
//                return;
            } else if (editLayer.getGeometryType().isPolygon())
                activeToolGroup = new PolygonMainEditingToolGroup(mapView);
            else if (editLayer.getGeometryType().isLine())
                activeToolGroup = new LineMainEditingToolGroup(mapView);
            else if (editLayer.getGeometryType().isPoint())
                activeToolGroup = new PointMainEditingToolGroup(mapView);
            EditManager.INSTANCE.setActiveToolGroup(activeToolGroup);
            setLeftButtoonsEnablement(false);

            mapView.blockMap();
        }

    }

    private void disableEditing() {
        toggleEditingButton.setImageDrawable(Compat.getDrawable(this, R.drawable.ic_mapview_toggle_editing_off_24dp));
        Tool activeTool = EditManager.INSTANCE.getActiveTool();
        if (activeTool != null) {
            activeTool.disable();
            EditManager.INSTANCE.setActiveTool(null);
        }
        ToolGroup activeToolGroup = EditManager.INSTANCE.getActiveToolGroup();
        if (activeToolGroup != null) {
            activeToolGroup.disable();
            EditManager.INSTANCE.setActiveToolGroup(null);
        }
        setLeftButtoonsEnablement(true);
    }

    private void setLeftButtoonsEnablement(boolean enable) {
        ImageButton addnotebytagButton = findViewById(R.id.addnotebytagbutton);
        ImageButton addBookmarkButton = findViewById(R.id.addbookmarkbutton);
        ImageButton toggleLoginfoButton = findViewById(R.id.toggleloginfobutton);
        ImageButton toggleMeasuremodeButton = findViewById(R.id.togglemeasuremodebutton);
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
        ImageButton addnotebytagButton = findViewById(R.id.addnotebytagbutton);
        ImageButton addBookmarkButton = findViewById(R.id.addbookmarkbutton);
        ImageButton toggleLoginfoButton = findViewById(R.id.toggleloginfobutton);
        ImageButton toggleMeasuremodeButton = findViewById(R.id.togglemeasuremodebutton);
        ImageButton zoomInButton = findViewById(R.id.zoomin);
        TextView zoomLevelTextview = findViewById(R.id.zoomlevel);
        ImageButton zoomOutButton = findViewById(R.id.zoomout);
        ImageButton toggleEditingButton = findViewById(R.id.toggleEditingButton);

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
        if (keyCode == KeyEvent.KEYCODE_BACK && EditManager.INSTANCE.getActiveToolGroup() != null) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCoordinateInserted(double lon, double lat) {
        setNewCenter(lon, lat);
    }


    @Override
    public Context getContext() {
        return this;
    }
}
