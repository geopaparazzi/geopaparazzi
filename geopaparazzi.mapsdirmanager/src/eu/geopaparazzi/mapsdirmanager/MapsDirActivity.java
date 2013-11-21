/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.mapsdirmanager;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.Date;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorFactory;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.overlay.ArrayCircleOverlay;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayCircle;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.rendertheme.InternalRenderTheme;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import eu.geopaparazzi.mapsdirmanager.filefilter.FilterByFileExtension;
import eu.geopaparazzi.mapsdirmanager.filefilter.ValidMapFile;
import eu.geopaparazzi.mapsdirmanager.filefilter.ValidRenderTheme;
import eu.geopaparazzi.mapsdirmanager.filepicker.FilePicker;
import eu.geopaparazzi.mapsdirmanager.preferences.EditPreferences;
import eu.geopaparazzi.mapsdirmanager.MyLocationListener;
import eu.geopaparazzi.mapsdirmanager.ScreenshotCapturer;
// -begin- MapsDir specific
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.MapsDirTreeViewList;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfo;
// -end-  MapsDir specific
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.GPLogPreferencesHandler;
import eu.geopaparazzi.library.gps.GpsLocation;
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.sensors.SensorsManager;
import eu.geopaparazzi.library.sms.SmsData;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.AboutActivity;
import eu.geopaparazzi.library.util.activities.DirectoryBrowserActivity;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;

/**
 * A map application which uses the features from the mapsforge map library. The map can be centered to the current
 * location. A simple file browser for selecting the map file is also included. Some preferences can be adjusted via the
 * {@link EditPreferences} activity and screenshots of the map may be taken in different image formats.
 */
public class MapsDirActivity extends MapActivity {
        /**
         * The default number of tiles in the file system cache.
         */
        public static final int FILE_SYSTEM_CACHE_SIZE_DEFAULT = 250;

        /**
         * The maximum number of tiles in the file system cache.
         */
        public static final int FILE_SYSTEM_CACHE_SIZE_MAX = 500;

        /**
         * The default move speed factor of the map.
         */
        public static final int MOVE_SPEED_DEFAULT = 10;

        /**
         * The maximum move speed factor of the map.
         */
        public static final int MOVE_SPEED_MAX = 30;

        private static final String BUNDLE_CENTER_AT_FIRST_FIX = "centerAtFirstFix";
        private static final String BUNDLE_SHOW_MY_LOCATION = "showMyLocation";
        private static final String BUNDLE_SNAP_TO_LOCATION = "snapToLocation";
        private static final int DIALOG_ENTER_COORDINATES = 0;
        private static final int DIALOG_INFO_MAP_FILE = 1;
        private static final int DIALOG_LOCATION_PROVIDER_DISABLED = 2;
        private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
        private static final FileFilter FILE_FILTER_EXTENSION_XML = new FilterByFileExtension(".xml");
        private static final int SELECT_MAP_FILEPICKER = 0;
        private static final int MAPSDIR_FILETREE = 1;
        private static final int SELECT_RENDER_THEME_FILE = 1;

        private Paint circleOverlayFill;
        private Paint circleOverlayOutline;
        private LocationManager locationManager;
        private MapGeneratorInternal mapGeneratorInternal;
        private MyLocationListener myLocationListener;
        private ScreenshotCapturer screenshotCapturer;
        private boolean showMyLocation;
        private boolean snapToLocation;
        private ToggleButton snapToLocationView;
        private WakeLock wakeLock;
        ArrayCircleOverlay circleOverlay;
        ArrayItemizedOverlay itemizedOverlay;
        MapController mapController;
        MapView mapView;
        OverlayCircle overlayCircle;
        OverlayItem overlayItem;
        private ResourcesManager resourcesManager;
        private File maps_dir=null;

        private void initializeResourcesManager() throws Exception {
        Object stateObj = getLastNonConfigurationInstance();
        if (stateObj instanceof ResourcesManager) {
            resourcesManager = (ResourcesManager) stateObj;
        } else {
            ResourcesManager.resetManager();
            resourcesManager = ResourcesManager.getInstance(this);
        }

        if (resourcesManager == null) {
         /*
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(eu.geopaparazzi.mapsforge.mapsdirmanager.R.string.no_sdcard_use_internal_memory).setCancelable(false)
                    .setPositiveButton(this.getString(android.R.string.yes), new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            ResourcesManager.setUseInternalMemory(true);
                            try {
                                resourcesManager = ResourcesManager.getInstance(MapsDirActivity.this);
                                initIfOk();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).setNegativeButton(this.getString(android.R.string.no), new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            finish();
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            * */
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

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            //maps_dir = ResourcesManager.getInstance(this).getMapsDir();
             maps_dir = new File("/mnt/extSdCard/maps");
            // define in MapsDirTreeViewList, which Context-Menues should be suppoted for this Application
            // Should the Properties-Menu be supported/shown?
            MapsDirTreeViewList.b_properties_file=true;
            // Should the Edit-Menu be supported/shown?
            MapsDirTreeViewList.b_edit_file=true;
            // Should the Delete-Menu be supported/shown?
            MapsDirTreeViewList.b_delete_file=false;
            MapsDirManager.reset();
            MapsDirManager.init(this,maps_dir);
            maps_dir=MapsDirManager.get_maps_dir();
            GPLog.androidLog(-1,getClass().getSimpleName()+" maps_dir["+maps_dir.getAbsolutePath()+"]");
            FilePicker.DEFAULT_DIRECTORY=maps_dir.getAbsolutePath();
        } catch (Exception e) {
            GPLog.androidLog(4,getClass().getSimpleName()+"[MapsDirManager]", e);
        }

        // check for screen on
        /*
        boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        * */
    }
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                getMenuInflater().inflate(R.menu.options_menu, menu);
                return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
         // GPLog.androidLog(-1,getClass().getSimpleName()+" onOptionsItemSelected["+item.getItemId()+"]");
                switch (item.getItemId()) {
                        case R.id.menu_info:
                                return true;

                        case R.id.menu_info_map_file:
                                showDialog(DIALOG_INFO_MAP_FILE);
                                return true;

                        case R.id.menu_info_about:
                                startActivity(new Intent(this, InfoView.class));
                                return true;

                        case R.id.menu_position:
                                return true;

                        case R.id.menu_position_my_location_disable:
                                disableShowMyLocation();
                                return true;

                        case R.id.menu_position_last_known:
                                gotoLastKnownPosition();
                                return true;

                        case R.id.menu_position_enter_coordinates:
                                showDialog(DIALOG_ENTER_COORDINATES);
                                return true;

                        case R.id.menu_position_map_center:
                                // disable GPS follow mode if it is enabled
                                disableSnapToLocation(true);
                                this.mapController.setCenter(this.mapView.getMapDatabase().getMapFileInfo().mapCenter);
                                return true;

                        case R.id.menu_screenshot:
                                return true;

                        case R.id.menu_screenshot_jpeg:
                                this.screenshotCapturer.captureScreenShot(CompressFormat.JPEG);
                                return true;

                        case R.id.menu_screenshot_png:
                                this.screenshotCapturer.captureScreenShot(CompressFormat.PNG);
                                return true;

                        case R.id.menu_preferences:
                                startActivity(new Intent(this, EditPreferences.class));
                                return true;

                        case R.id.menu_mapsdir_tree:
                                startMapsDirTreeViewList();
                                return true;

                        case R.id.menu_render_theme:
                                return true;

                        case R.id.menu_render_theme_osmarender:
                                this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
                                return true;

                        case R.id.menu_render_theme_select_file:
                                startRenderThemePicker();
                                return true;

                        case R.id.menu_mapfile:
                                // startMapFilePicker();
                                startMapsDirTreeViewList();
                                return true;

                        default:
                                return false;
                }
        }

        @Override
        public boolean onPrepareOptionsMenu(Menu menu) {
                MapGenerator mapGenerator = this.mapView.getMapGenerator();
                try
                {
                 if (mapGenerator.requiresInternetConnection())
                 {
                  menu.findItem(R.id.menu_info_map_file).setEnabled(false);
                 }
                 else
                 {
                  menu.findItem(R.id.menu_info_map_file).setEnabled(true);
                 }
                }
                catch (Exception e)
                {
                 GPLog.androidLog(4,getClass().getSimpleName()+"[onPrepareOptionsMenu.menu_info_map_file]", e);
                }
                if (isShowMyLocationEnabled()) {
                        menu.findItem(R.id.menu_position_my_location_enable).setVisible(false);
                        menu.findItem(R.id.menu_position_my_location_enable).setEnabled(false);
                        menu.findItem(R.id.menu_position_my_location_disable).setVisible(true);
                        menu.findItem(R.id.menu_position_my_location_disable).setEnabled(true);
                } else {
                        menu.findItem(R.id.menu_position_my_location_enable).setVisible(true);
                        menu.findItem(R.id.menu_position_my_location_enable).setEnabled(true);
                        menu.findItem(R.id.menu_position_my_location_disable).setVisible(false);
                        menu.findItem(R.id.menu_position_my_location_disable).setEnabled(false);
                }

                if (mapGenerator.requiresInternetConnection()) {
                        menu.findItem(R.id.menu_position_map_center).setEnabled(false);
                } else {
                        menu.findItem(R.id.menu_position_map_center).setEnabled(true);
                }

                if (mapGenerator.requiresInternetConnection()) {
                        menu.findItem(R.id.menu_render_theme).setEnabled(false);
                } else {
                        menu.findItem(R.id.menu_render_theme).setEnabled(true);
                }

                if (mapGenerator.requiresInternetConnection()) {
                        menu.findItem(R.id.menu_mapfile).setEnabled(false);
                } else {
                        menu.findItem(R.id.menu_mapfile).setEnabled(true);
                }

                return true;
        }

        @Override
        public boolean onTrackballEvent(MotionEvent event) {
                // forward the event to the MapView
                return this.mapView.onTrackballEvent(event);
        }

        private void configureMapView() {
                // configure the MapView and activate the zoomLevel buttons
                this.mapView.setClickable(true);
                this.mapView.setBuiltInZoomControls(true);
                this.mapView.setFocusable(true);

                // set the localized text fields
                MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
                mapScaleBar.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
                mapScaleBar.setText(TextField.METER, getString(R.string.unit_symbol_meter));

                // get the map controller for this MapView
                this.mapController = this.mapView.getController();
        }

        /**
         * Enables the "show my location" mode.
         *
         * @param centerAtFirstFix
         *            defines whether the map should be centered to the first fix.
         */
        private void enableShowMyLocation(boolean centerAtFirstFix) {
                if (!this.showMyLocation) {
                        Criteria criteria = new Criteria();
                        criteria.setAccuracy(Criteria.ACCURACY_FINE);
                        String bestProvider = this.locationManager.getBestProvider(criteria, true);
                        if (bestProvider == null) {
                                showDialog(DIALOG_LOCATION_PROVIDER_DISABLED);
                                return;
                        }

                        this.showMyLocation = true;

                        this.circleOverlay = new ArrayCircleOverlay(this.circleOverlayFill, this.circleOverlayOutline);
                        this.overlayCircle = new OverlayCircle();
                        this.circleOverlay.addCircle(this.overlayCircle);
                        this.mapView.getOverlays().add(this.circleOverlay);

                        this.itemizedOverlay = new ArrayItemizedOverlay(null);
                        this.overlayItem = new OverlayItem();
                        this.overlayItem.setMarker(ItemizedOverlay.boundCenter(getResources().getDrawable(R.drawable.my_location)));
                        this.itemizedOverlay.addItem(this.overlayItem);
                        this.mapView.getOverlays().add(this.itemizedOverlay);

                        this.myLocationListener.setCenterAtFirstFix(centerAtFirstFix);
                        this.locationManager.requestLocationUpdates(bestProvider, 1000, 0, this.myLocationListener);
                        this.snapToLocationView.setVisibility(View.VISIBLE);
                }
        }

        /**
         * Centers the map to the last known position as reported by the most accurate location provider. If the last
         * location is unknown, a toast message is displayed instead.
         */
        private void gotoLastKnownPosition() {
                Location currentLocation;
                Location bestLocation = null;
                for (String provider : this.locationManager.getProviders(true)) {
                        currentLocation = this.locationManager.getLastKnownLocation(provider);
                        if (bestLocation == null || currentLocation.getAccuracy() < bestLocation.getAccuracy()) {
                                bestLocation = currentLocation;
                        }
                }

                // check if a location has been found
                if (bestLocation != null) {
                        GeoPoint point = new GeoPoint(bestLocation.getLatitude(), bestLocation.getLongitude());
                        this.mapController.setCenter(point);
                } else {
                        showToastOnUiThread(getString(R.string.error_last_location_unknown));
                }
        }
    /**
     * Start the Dialog to select a map
     *
     * <p>
     * MapDirManager creates a static-list of maps and sends it to the MapsDirTreeViewList class
     * - when first called this list will build a diretory/file list AND a map-type/Diretory/File list
     * - once created, this list will be retained during the Application
     * - the user can switch from a sorted list as Directory/File OR Map-Type/Diretory/File view
     * </p>
     *  result will be sent to MapDirManager and saved there and stored to preferences
     *  - when the MapView is created, this will be read and loaded
     */
        private void startMapsDirTreeViewList() {
         try
         {
          startActivityForResult(new Intent(this,MapsDirTreeViewList.class),MAPSDIR_FILETREE);
          }
         catch (Exception e)
         {
          GPLog.androidLog(4,getClass().getSimpleName()+" -E-> menu_mapsdir_tree[startActivity(new Intent(this, MapsDirTreeViewList.class));]",e);
         }
        }
        /**
         * Sets all file filters and starts the FilePicker to select a map file.
         */
        private void startMapFilePicker() {
                FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
                FilePicker.setFileSelectFilter(new ValidMapFile());
                startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILEPICKER);
        }

        /**
         * Sets all file filters and starts the FilePicker to select an XML file.
         */
        private void startRenderThemePicker() {
                FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
                FilePicker.setFileSelectFilter(new ValidRenderTheme());
                startActivityForResult(new Intent(this, FilePicker.class), SELECT_RENDER_THEME_FILE);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent intent)
        {
          int i_rc=0;
         // GPLog.androidLog(-1,"MapsDirActivity onActivityResult MapsDirTreeViewList request["+requestCode+"]");
         if ((requestCode == SELECT_MAP_FILEPICKER) || (requestCode == MAPSDIR_FILETREE))
         {
          String s_SELECTED_FILE="";
          String s_SELECTED_TYPE="";
          String s_this_classinfo="";
          if (resultCode == RESULT_OK)
          {
           disableSnapToLocation(true);
           if (requestCode == SELECT_MAP_FILEPICKER)
           {
            s_SELECTED_FILE=intent.getStringExtra(FilePicker.SELECTED_FILE);
            s_SELECTED_TYPE=intent.getStringExtra(FilePicker.SELECTED_TYPE);
           }
           if (requestCode == MAPSDIR_FILETREE)
           {
            s_SELECTED_FILE=intent.getStringExtra(MapsDirTreeViewList.SELECTED_FILE);
            s_SELECTED_TYPE=intent.getStringExtra(MapsDirTreeViewList.SELECTED_TYPE);
            s_this_classinfo="type["+s_SELECTED_TYPE+"] file["+s_SELECTED_FILE+"]";
            if (MapsDirTreeViewList.selected_classinfo != null)
            {
             ClassNodeInfo selected_classinfo=MapsDirTreeViewList.selected_classinfo;
             s_this_classinfo=selected_classinfo.toString();
             i_rc=MapsDirManager.selected_MapClassInfo(selected_classinfo,this.mapView,null);
             // minZoomLevel = MapsDirManager.getMinZoom();
             // maxZoomLevel = MapsDirManager.getMaxZoom();
             s_SELECTED_FILE=selected_classinfo.getFileNamePath();
             // this.mapView.setMapFile(new File(s_SELECTED_FILE));
            }
            // GPLog.androidLog(-1,"MapsDirActivity onActivityResult SELECTED_FILE[" +s_SELECTED_FILE+ "] i_rc["+i_rc+"]");
            s_SELECTED_FILE="";
           }
           // GPLog.androidLog(-1,"MapsDirActivity onActivityResult file[" +s_SELECTED_FILE+ "] type[" +s_SELECTED_TYPE+ "]");
           if (intent != null && s_SELECTED_FILE != null)
           { // check for map type
            if (s_SELECTED_TYPE.equals("map"))
            {
             this.mapView.setMapFile(new File(s_SELECTED_FILE));
            }
            else
            {
            }
           }
          }
          else
          {
           if (resultCode == RESULT_CANCELED && !this.mapView.getMapGenerator().requiresInternetConnection() && this.mapView.getMapFile() == null)
           {
            finish();
           }
          }
         }
         else
         {
          if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK && intent != null && intent.getStringExtra(FilePicker.SELECTED_FILE) != null)
          {
           try
           {
            this.mapView.setRenderTheme(new File(intent.getStringExtra(FilePicker.SELECTED_FILE)));
           }
           catch (FileNotFoundException e)
           {
            showToastOnUiThread(e.getLocalizedMessage());
           }
          }
         }
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
         GPLog.LOG_HEAVY = false; // this application has no database logging system
         try {
          initializeResourcesManager();
         } catch (Exception e) {
            GPLog.androidLog(4,getClass().getSimpleName()+"[MapsDirManager.onCreate]", e);
         }
                super.onCreate(savedInstanceState);

                this.screenshotCapturer = new ScreenshotCapturer(this);
                this.screenshotCapturer.start();

                // set up the layout views
                setContentView(R.layout.activity_advanced_map_viewer);
                this.mapView = (MapView) findViewById(R.id.mapView);
                configureMapView();

                this.snapToLocationView = (ToggleButton) findViewById(R.id.snapToLocationView);
                this.snapToLocationView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                                if (isSnapToLocationEnabled()) {
                                        disableSnapToLocation(true);
                                } else {
                                        enableSnapToLocation(true);
                                }
                        }
                });

                // get the pointers to different system services
                this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                this.myLocationListener = new MyLocationListener(this);
                PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

                // set up the paint objects for the location overlay
                this.circleOverlayFill = new Paint(Paint.ANTI_ALIAS_FLAG);
                this.circleOverlayFill.setStyle(Paint.Style.FILL);
                this.circleOverlayFill.setColor(Color.BLUE);
                this.circleOverlayFill.setAlpha(48);

                this.circleOverlayOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
                this.circleOverlayOutline.setStyle(Paint.Style.STROKE);
                this.circleOverlayOutline.setColor(Color.BLUE);
                this.circleOverlayOutline.setAlpha(128);
                this.circleOverlayOutline.setStrokeWidth(2);

                if (savedInstanceState != null && savedInstanceState.getBoolean(BUNDLE_SHOW_MY_LOCATION)) {
                        enableShowMyLocation(savedInstanceState.getBoolean(BUNDLE_CENTER_AT_FIRST_FIX));
                        if (savedInstanceState.getBoolean(BUNDLE_SNAP_TO_LOCATION)) {
                                enableSnapToLocation(false);
                        }
                }
        }

        @Override
        protected Dialog onCreateDialog(int id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                if (id == DIALOG_ENTER_COORDINATES) {
                        builder.setIcon(android.R.drawable.ic_menu_mylocation);
                        builder.setTitle(R.string.menu_position_enter_coordinates);
                        LayoutInflater factory = LayoutInflater.from(this);
                        final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
                        builder.setView(view);
                        builder.setPositiveButton(R.string.go_to_position, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        // disable GPS follow mode if it is enabled
                                        disableSnapToLocation(true);

                                        // set the map center and zoom level
                                        EditText latitudeView = (EditText) view.findViewById(R.id.latitude);
                                        EditText longitudeView = (EditText) view.findViewById(R.id.longitude);
                                        double latitude = Double.parseDouble(latitudeView.getText().toString());
                                        double longitude = Double.parseDouble(longitudeView.getText().toString());
                                        GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                                        MapsDirActivity.this.mapController.setCenter(geoPoint);
                                        SeekBar zoomLevelView = (SeekBar) view.findViewById(R.id.zoomLevel);
                                        MapsDirActivity.this.mapController.setZoom(zoomLevelView.getProgress());
                                }
                        });
                        builder.setNegativeButton(R.string.cancel, null);
                        return builder.create();
                } else if (id == DIALOG_LOCATION_PROVIDER_DISABLED) {
                        builder.setIcon(android.R.drawable.ic_menu_info_details);
                        builder.setTitle(R.string.error);
                        builder.setMessage(R.string.no_location_provider_available);
                        builder.setPositiveButton(R.string.ok, null);
                        return builder.create();
                } else if (id == DIALOG_INFO_MAP_FILE) {
                        builder.setIcon(android.R.drawable.ic_menu_info_details);
                        builder.setTitle(R.string.menu_info_map_file);
                        LayoutInflater factory = LayoutInflater.from(this);
                        builder.setView(factory.inflate(R.layout.dialog_info_map_file, null));
                        builder.setPositiveButton(R.string.ok, null);
                        return builder.create();
                } else {
                        // do dialog will be created
                        return null;
                }
        }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                this.screenshotCapturer.interrupt();
                disableShowMyLocation();
        }

        @Override
        protected void onPause() {
                super.onPause();
                // release the wake lock if necessary
                if (this.wakeLock.isHeld()) {
                        this.wakeLock.release();
                }
        }

        @Override
        protected void onPrepareDialog(int id, final Dialog dialog) {
                if (id == DIALOG_ENTER_COORDINATES) {
                        // latitude
                        EditText editText = (EditText) dialog.findViewById(R.id.latitude);
                        GeoPoint mapCenter = this.mapView.getMapPosition().getMapCenter();
                        editText.setText(Double.toString(mapCenter.getLatitude()));

                        // longitude
                        editText = (EditText) dialog.findViewById(R.id.longitude);
                        editText.setText(Double.toString(mapCenter.getLongitude()));

                        // zoom level
                        SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomLevel);
                        zoomlevel.setMax(this.mapView.getMapGenerator().getZoomLevelMax());
                        zoomlevel.setProgress(this.mapView.getMapPosition().getZoomLevel());

                        // zoom level value
                        final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
                        textView.setText(String.valueOf(zoomlevel.getProgress()));
                        zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
                } else if (id == DIALOG_INFO_MAP_FILE) {
                        MapFileInfo mapFileInfo = this.mapView.getMapDatabase().getMapFileInfo();

                        // map file name
                        TextView textView = (TextView) dialog.findViewById(R.id.infoMapFileViewName);
                        textView.setText(this.mapView.getMapFile().getAbsolutePath());

                        // map file size
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewSize);
                        textView.setText(FileUtils.formatFileSize(mapFileInfo.fileSize, getResources()));

                        // map file version
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewVersion);
                        textView.setText(String.valueOf(mapFileInfo.fileVersion));

                        // map file debug
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDebug);
                        if (mapFileInfo.debugFile) {
                                textView.setText(R.string.info_map_file_debug_yes);
                        } else {
                                textView.setText(R.string.info_map_file_debug_no);
                        }

                        // map file date
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDate);
                        Date date = new Date(mapFileInfo.mapDate);
                        textView.setText(DateFormat.getDateTimeInstance().format(date));

                        // map file area
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewArea);
                        BoundingBox boundingBox = mapFileInfo.boundingBox;
                        textView.setText(boundingBox.getMinLatitude() + ", " + boundingBox.getMinLongitude() + " – \n"
                                        + boundingBox.getMaxLatitude() + ", " + boundingBox.getMaxLongitude());

                        // map file start position
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartPosition);
                        GeoPoint startPosition = mapFileInfo.startPosition;
                        if (startPosition == null) {
                                textView.setText(null);
                        } else {
                                textView.setText(startPosition.getLatitude() + ", " + startPosition.getLongitude());
                        }

                        // map file start zoom level
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartZoomLevel);
                        Byte startZoomLevel = mapFileInfo.startZoomLevel;
                        if (startZoomLevel == null) {
                                textView.setText(null);
                        } else {
                                textView.setText(startZoomLevel.toString());
                        }

                        // map file language preference
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewLanguagePreference);
                        textView.setText(mapFileInfo.languagePreference);

                        // map file comment text
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewComment);
                        textView.setText(mapFileInfo.comment);

                        // map file created by text
                        textView = (TextView) dialog.findViewById(R.id.infoMapFileViewCreatedBy);
                        textView.setText(mapFileInfo.createdBy);
                } else {
                        super.onPrepareDialog(id, dialog);
                }
        }

        @Override
        protected void onResume() {
                super.onResume();

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

                MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
                mapScaleBar.setShowMapScaleBar(preferences.getBoolean("showScaleBar", false));
                String scaleBarUnitDefault = getString(R.string.preferences_scale_bar_unit_default);
                String scaleBarUnit = preferences.getString("scaleBarUnit", scaleBarUnitDefault);
                mapScaleBar.setImperialUnits(scaleBarUnit.equals("imperial"));

                if (preferences.contains("mapGenerator")) {
                        String name = preferences.getString("mapGenerator", MapGeneratorInternal.DATABASE_RENDERER.name());
                        MapGeneratorInternal mapGeneratorInternalNew;
                        try {
                                mapGeneratorInternalNew = MapGeneratorInternal.valueOf(name);
                        } catch (IllegalArgumentException e) {
                                mapGeneratorInternalNew = MapGeneratorInternal.DATABASE_RENDERER;
                        }

                        if (mapGeneratorInternalNew != this.mapGeneratorInternal) {
                                MapGenerator mapGenerator = MapGeneratorFactory.createMapGenerator(mapGeneratorInternalNew);
                                this.mapView.setMapGenerator(mapGenerator);
                                this.mapGeneratorInternal = mapGeneratorInternalNew;
                        }
                }
                try {
                        String textScaleDefault = getString(R.string.preferences_text_scale_default);
                        this.mapView.setTextScale(Float.parseFloat(preferences.getString("textScale", textScaleDefault)));
                } catch (NumberFormatException e) {
                        this.mapView.setTextScale(1);
                }

                if (preferences.getBoolean("fullscreen", false)) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                }
                if (preferences.getBoolean("wakeLock", false) && !this.wakeLock.isHeld()) {
                        this.wakeLock.acquire();
                }

                boolean persistent = preferences.getBoolean("cachePersistence", false);
                int capacity = Math.min(preferences.getInt("cacheSize", FILE_SYSTEM_CACHE_SIZE_DEFAULT),
                                FILE_SYSTEM_CACHE_SIZE_MAX);
                TileCache fileSystemTileCache = this.mapView.getFileSystemTileCache();
                fileSystemTileCache.setPersistent(persistent);
                fileSystemTileCache.setCapacity(capacity);

                float moveSpeedFactor = Math.min(preferences.getInt("moveSpeed", MOVE_SPEED_DEFAULT), MOVE_SPEED_MAX) / 10f;
                this.mapView.getMapMover().setMoveSpeedFactor(moveSpeedFactor);

                this.mapView.getFpsCounter().setFpsCounter(preferences.getBoolean("showFpsCounter", false));

                boolean drawTileFrames = preferences.getBoolean("drawTileFrames", false);
                boolean drawTileCoordinates = preferences.getBoolean("drawTileCoordinates", false);
                boolean highlightWaterTiles = preferences.getBoolean("highlightWaterTiles", false);
                DebugSettings debugSettings = new DebugSettings(drawTileCoordinates, drawTileFrames, highlightWaterTiles);
                this.mapView.setDebugSettings(debugSettings);
           if (MapsDirTreeViewList.selected_classinfo != null)
           {
            // MapsDirManager.load_Map(mapView,null);
            // GPLog.androidLog(-1,"MapsActivity -I->  onActivityResult s_selected_map[" +MapsDirTreeViewList.selected_classinfo.getShortDescription()+ "] ");
            MapsDirManager.selected_MapClassInfo(MapsDirTreeViewList.selected_classinfo,this.mapView,null);
            // mj10777: not sure what to do with these values ??
            // minZoomLevel = MapsDirManager.getMinZoom();
            // maxZoomLevel = MapsDirManager.getMaxZoom();
           }
           else
           {
            startMapsDirTreeViewList();
           }
           //     if (!this.mapView.getMapGenerator().requiresInternetConnection() && this.mapView.getMapFile() == null) {
                 //       startMapFilePicker();
          //      }
        }

        @Override
        protected void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                outState.putBoolean(BUNDLE_SHOW_MY_LOCATION, isShowMyLocationEnabled());
                outState.putBoolean(BUNDLE_CENTER_AT_FIRST_FIX, this.myLocationListener.isCenterAtFirstFix());
                outState.putBoolean(BUNDLE_SNAP_TO_LOCATION, this.snapToLocation);
        }

        /**
         * Disables the "show my location" mode.
         */
        void disableShowMyLocation() {
                if (this.showMyLocation) {
                        this.showMyLocation = false;
                        disableSnapToLocation(false);
                        this.locationManager.removeUpdates(this.myLocationListener);
                        if (this.circleOverlay != null) {
                                this.mapView.getOverlays().remove(this.circleOverlay);
                                this.mapView.getOverlays().remove(this.itemizedOverlay);
                                this.circleOverlay = null;
                                this.itemizedOverlay = null;
                        }
                        this.snapToLocationView.setVisibility(View.GONE);
                }
        }

        /**
         * Disables the "snap to location" mode.
         *
         * @param showToast
         *            defines whether a toast message is displayed or not.
         */
        void disableSnapToLocation(boolean showToast) {
                if (this.snapToLocation) {
                        this.snapToLocation = false;
                        this.snapToLocationView.setChecked(false);
                        this.mapView.setClickable(true);
                        if (showToast) {
                                showToastOnUiThread(getString(R.string.snap_to_location_disabled));
                        }
                }
        }

        /**
         * Enables the "snap to location" mode.
         *
         * @param showToast
         *            defines whether a toast message is displayed or not.
         */
        void enableSnapToLocation(boolean showToast) {
                if (!this.snapToLocation) {
                        this.snapToLocation = true;
                        this.mapView.setClickable(false);
                        if (showToast) {
                                showToastOnUiThread(getString(R.string.snap_to_location_enabled));
                        }
                }
        }

        /**
         * Returns the status of the "show my location" mode.
         *
         * @return true if the "show my location" mode is enabled, false otherwise.
         */
        boolean isShowMyLocationEnabled() {
                return this.showMyLocation;
        }

        /**
         * Returns the status of the "snap to location" mode.
         *
         * @return true if the "snap to location" mode is enabled, false otherwise.
         */
        boolean isSnapToLocationEnabled() {
                return this.snapToLocation;
        }

        /**
         * Uses the UI thread to display the given text message as toast notification.
         *
         * @param text
         *            the text message to display
         */
        void showToastOnUiThread(final String text) {

                if (AndroidUtils.currentThreadIsUiThread()) {
                        Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
                        toast.show();
                } else {
                        runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                        Toast toast = Toast.makeText(MapsDirActivity.this, text, Toast.LENGTH_LONG);
                                        toast.show();
                                }
                        });
                }
        }
}
