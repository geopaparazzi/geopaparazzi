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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewPosition;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.OpenCycleMapTileDownloader;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.map.reader.header.MapFileInfo;

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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsLocation;
import eu.geopaparazzi.library.gps.GpsManager;
import eu.geopaparazzi.library.gps.GpsManagerListener;
import eu.geopaparazzi.library.mixare.MixareHandler;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.sms.SmsData;
import eu.geopaparazzi.library.sms.SmsUtilities;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.activities.GeocodeActivity;
import eu.geopaparazzi.library.util.activities.InsertCoordActivity;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.activities.DataListActivity;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.ActionBar;
import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.maps.overlays.ArrayGeopaparazziOverlay;
import eu.hydrologis.geopaparazzi.maps.tiles.CustomTileDownloader;
import eu.hydrologis.geopaparazzi.maps.tiles.GeopackageTileDownloader;
import eu.hydrologis.geopaparazzi.maps.tiles.MapGeneratorInternal;
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
public class MapsActivity extends MapActivity implements GpsManagerListener, OnTouchListener {
    private final int INSERTCOORD_RETURN_CODE = 666;
    private final int ZOOM_RETURN_CODE = 667;
    private final int GPSDATAPROPERTIES_RETURN_CODE = 668;
    private final int DATAPROPERTIES_RETURN_CODE = 671;
    public static final int FORMUPDATE_RETURN_CODE = 669;
    private final int CONTACT_RETURN_CODE = 670;

    private final int MENU_GPSDATA = 1;
    private final int MENU_DATA = 2;
    private final int MENU_SCALE_ID = 4;
    private final int MENU_MIXARE_ID = 5;
    private final int GO_TO = 6;
    private final int CENTER_ON_MAP = 7;
    private final int MENU_COMPASS_ID = 8;
    private final int MENU_SENDDATA_ID = 9;

    private DecimalFormat formatter = new DecimalFormat("00"); //$NON-NLS-1$
    private SlidingDrawer slidingDrawer;
    private MapView mapView;
    private int maxZoomLevel = -1;
    private int minZoomLevel = -1;
    private SlidingDrawer osmSlidingDrawer;
    private SharedPreferences preferences;
    private boolean doOsm;

    private ArrayGeopaparazziOverlay dataOverlay;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button batteryButton;

    private SliderDrawView sliderDrawView;
    private List<String> smsString;
    private Drawable notesDrawable;

    public static MapGenerator createMapGenerator( MapGeneratorInternal mapGeneratorInternal ) {
        switch( mapGeneratorInternal ) {
        case DATABASE_RENDERER:
            return new DatabaseRenderer();
        case MAPNIK:
            return new MapnikTileDownloader();
        case OPENCYCLEMAP:
            return new OpenCycleMapTileDownloader();
        }

        throw new IllegalArgumentException("unknown enum value: " + mapGeneratorInternal); //$NON-NLS-1$
    }

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.mapsview);

        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
     preferences = PreferenceManager.getDefaultSharedPreferences(this);
     // mj10777: .mbtiles,.map and .mapurl files may know there bounds and desired center point
     // - 'checkCenterLocation' will change this value if out of range
     double[] mapCenterLocation = PositionUtilities.getMapCenterFromPreferences(preferences,true,true);
     // check for screen on
     boolean keepScreenOn = preferences.getBoolean(Constants.PREFS_KEY_SCREEN_ON, false);
     if (keepScreenOn)
     {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
     }

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
     { // get proper rendering engine
      MapGenerator mapGenerator;
      boolean b_map_file=false;
      String tileSourceName = preferences.getString(Constants.PREFS_KEY_TILESOURCE, ""); //$NON-NLS-1$
      String filePath = preferences.getString(Constants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
      // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate tileSource["+tileSourceName+"] filePath["+filePath+"]");
      MapGeneratorInternal mapGeneratorInternal = null;
      try
      {
       mapGeneratorInternal = MapGeneratorInternal.valueOf(tileSourceName);
      }
      catch (IllegalArgumentException e)
      {
       // ignore, is custom
      }
      SpatialRasterTable rasterTable = null;
      try
      {
       rasterTable = SpatialDatabasesManager.getInstance().getRasterTableByName(tileSourceName);
      }
      catch (jsqlite.Exception e1)
      {
       e1.printStackTrace();
      }
      if (rasterTable != null)
      {
       try
       {
        mapGenerator = new GeopackageTileDownloader(rasterTable);
        minZoomLevel = rasterTable.getMinZoom();
        maxZoomLevel = rasterTable.getMaxZoom();
        // mj10777: i_rc=0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct mapCenterLocation values.
        rasterTable.checkCenterLocation(mapCenterLocation,1) ;
        // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate GeopackageTileDownloader rasterTable["+rasterTable.getTableName()+"] zoom_min_max["+minZoomLevel+","+maxZoomLevel+"]");
       }
       catch (jsqlite.Exception e)
       {
        // e.printStackTrace();
        SpatialDatabasesManager.app_log(2,"MapActivity.onCreate GeopackageTileDownloader rasterTable[failed]",e);
        mapGenerator = createMapGenerator(MapGeneratorInternal.MAPNIK);
       }
      }
      else
      {
       if (tileSourceName.length() == 0 && filePath != null && new File(filePath).exists())
       {
        try
        {
         if (filePath.endsWith(".map"))
         {
          File mapfile = new File(filePath);
          mapView.setMapFile(mapfile);
          String nameNoExt = FileUtilities.getNameWithoutExtention(mapfile);
          File xmlFile = new File(mapfile.getParentFile(), nameNoExt + ".xml"); //$NON-NLS-1$
          if (xmlFile.exists())
          {
           try
           {
            mapView.setRenderTheme(xmlFile);
           }
           catch (FileNotFoundException e)
           {
            // ignore the theme
           }
          }
          mapGenerator = mapView.getMapGenerator();
          b_map_file=true;
          minZoomLevel = mapGenerator.getStartZoomLevel();
          maxZoomLevel = mapGenerator.getZoomLevelMax();
          // mj10777: i_rc=0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct mapCenterLocation values.
          checkCenterLocation(mapCenterLocation,1) ;
          // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate: .map["+filePath+"] zoom_min_max["+minZoomLevel+","+maxZoomLevel+"]");
         }
         else
         {
          File mapsDir = ResourcesManager.getInstance(this).getMapsDir();
          CustomTileDownloader custom_tile = CustomTileDownloader.file2TileDownloader(new File(filePath), mapsDir.getAbsolutePath());
          // mj10777: i_rc=0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct mapCenterLocation values.
          custom_tile.checkCenterLocation(mapCenterLocation,1) ;
          mapGenerator = custom_tile;
          minZoomLevel = mapGenerator.getStartZoomLevel();
          maxZoomLevel = mapGenerator.getZoomLevelMax();
          // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate Custom.mapurl["+filePath+"] zoom_min_max["+minZoomLevel+","+maxZoomLevel+"]");
         }
        }
        catch (Exception e)
        {
         // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate createMapGenerator[MAPNIK]["+filePath+"] [failed] [-1-]");
         mapGenerator = createMapGenerator(MapGeneratorInternal.MAPNIK);
        }
       }
       else
       {
        if (mapGeneratorInternal != null)
        {
         mapGenerator = createMapGenerator(mapGeneratorInternal);
         // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate createMapGenerator[mapGeneratorInternal] [being used]");
        }
        else
        {
         // SpatialDatabasesManager.app_log(-1,"MapActivity.onCreate createMapGenerator[MAPNIK] [failed] [-2-]");
         mapGenerator = createMapGenerator(MapGeneratorInternal.MAPNIK);
        }
       }
      }
      if (!b_map_file)
      { // with map files mapGenerator has allready been added.
       mapView.setMapGenerator(mapGenerator);
      }
      if (maxZoomLevel == -1)
      {
       maxZoomLevel = mapView.getMapZoomControls().getZoomLevelMax();
       minZoomLevel = mapView.getMapZoomControls().getZoomLevelMin();
      }
     }

        MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();

        boolean doImperial = preferences.getBoolean(Constants.PREFS_KEY_IMPERIAL, false);
        mapScaleBar.setImperialUnits(doImperial);
        if (doImperial) {
            mapScaleBar.setText(TextField.FOOT, " ft"); //$NON-NLS-1$
            mapScaleBar.setText(TextField.MILE, " mi"); //$NON-NLS-1$
        }else{
            mapScaleBar.setText(TextField.KILOMETER, " km"); //$NON-NLS-1$
            mapScaleBar.setText(TextField.METER, " m"); //$NON-NLS-1$
        }

        if (Debug.D) {
            // boolean drawTileFrames = preferences.getBoolean("drawTileFrames", false);
            // boolean drawTileCoordinates = preferences.getBoolean("drawTileCoordinates", false);
            // boolean highlightWaterTiles = preferences.getBoolean("highlightWaterTiles", false);
            DebugSettings debugSettings = new DebugSettings(true, true, false);
            this.mapView.setDebugSettings(debugSettings);
        }

        setTextScale();

        final RelativeLayout rl = (RelativeLayout) findViewById(R.id.innerlayout);
        rl.addView(mapView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        GpsManager.getInstance(this).addListener(this);

        // /* measure tool */
        // {
        // mMeasureOverlay = new MeasureToolOverlay(this, mResourceProxy);
        // this.mapsView.getOverlays().add(mMeasureOverlay);
        // }

        GeoPoint geoPoint = new GeoPoint(mapCenterLocation[1], mapCenterLocation[0]);
        mapView.getController().setZoom((int) mapCenterLocation[2]);
        mapView.getController().setCenter(geoPoint);

        int zoomInLevel = (int) mapCenterLocation[2] + 1;
        if (zoomInLevel > maxZoomLevel) {
            zoomInLevel = maxZoomLevel;
        }
        int zoomOutLevel = (int) mapCenterLocation[2] - 1;
        if (zoomOutLevel < minZoomLevel) {
            zoomOutLevel = minZoomLevel;
        }
        zoomInButton = (Button) findViewById(R.id.zoomin);
        zoomInButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String text = zoomLevelText.getText().toString();
                int currentZoom = Integer.parseInt(text);
                int newZoom = currentZoom + 1;
                setZoomGuiText(newZoom);
                mapView.getController().setZoom(newZoom);
                inalidateMap();
            }
        });

        zoomLevelText = (TextView) findViewById(R.id.zoomlevel);

        zoomOutButton = (Button) findViewById(R.id.zoomout);
        zoomOutButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                String text = zoomLevelText.getText().toString();
                int currentZoom = Integer.parseInt(text);
                int newZoom = currentZoom - 1;
                setZoomGuiText(newZoom);
                mapView.getController().setZoom(newZoom);
                inalidateMap();
            }
        });

        batteryButton = (Button) findViewById(R.id.battery);

        // center on gps button
        ImageButton centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);
        centerOnGps.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                GpsLocation location = GpsManager.getInstance(MapsActivity.this).getLocation();
                if (location != null) {
                    setNewCenter(location.getLongitude(), location.getLatitude(), false);
                }
            }
        });

        // slidingdrawer
        final int slidingId = R.id.mapslide;
        slidingDrawer = (SlidingDrawer) findViewById(slidingId);
        final ImageView slideHandleButton = (ImageView) findViewById(R.id.mapslidehandle);

        slidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener(){
            public void onDrawerOpened() {
                slideHandleButton.setBackgroundResource(R.drawable.min);
            }
        });
        slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener(){
            public void onDrawerClosed() {
                slideHandleButton.setBackgroundResource(R.drawable.max);
            }
        });

        sliderDrawView = (SliderDrawView) findViewById(R.id.sliderdrawview);

        /*
        * tool buttons
        */
        ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
        addnotebytagButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                // generate screenshot in background in order to not freeze
                try {
                    File mediaDir = ResourcesManager.getInstance(MapsActivity.this).getMediaDir();
                    final File tmpImageFile = new File(mediaDir.getParentFile(), LibraryConstants.TMPPNGIMAGENAME);
                    new Thread(new Runnable(){
                        public void run() {
                            try {
                                mapView.takeScreenshot(Bitmap.CompressFormat.PNG, 90, tmpImageFile);
                            } catch (Exception e) {
                            }
                        }
                    }).start();
                    MapViewPosition mapPosition = mapView.getMapPosition();
                    GeoPoint mapCenter = mapPosition.getMapCenter();
                    Intent mapTagsIntent = new Intent(MapsActivity.this, MapTagsActivity.class);
                    mapTagsIntent.putExtra(LibraryConstants.LATITUDE, (double) (mapCenter.latitudeE6 / LibraryConstants.E6));
                    mapTagsIntent.putExtra(LibraryConstants.LONGITUDE, (double) (mapCenter.longitudeE6 / LibraryConstants.E6));
                    mapTagsIntent.putExtra(LibraryConstants.ELEVATION, 0.0);
                    mapTagsIntent.putExtra(LibraryConstants.TMPPNGIMAGENAME, tmpImageFile.getAbsolutePath());
                    startActivity(mapTagsIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
        addBookmarkButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                addBookmark();
            }
        });

        ImageButton listNotesButton = (ImageButton) findViewById(R.id.listnotesbutton);
        listNotesButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(MapsActivity.this, NotesListActivity.class);
                startActivityForResult(intent, ZOOM_RETURN_CODE);
            }
        });

        ImageButton listBookmarksButton = (ImageButton) findViewById(R.id.bookmarkslistbutton);
        listBookmarksButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(MapsActivity.this, BookmarksListActivity.class);
                startActivityForResult(intent, ZOOM_RETURN_CODE);
            }
        });

        final ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
        toggleMeasuremodeButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                boolean isInMeasureMode = !mapView.isClickable();
                if (!isInMeasureMode) {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode_on);
                } else {
                    toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode);
                }
                if (isInMeasureMode) {
                    mapView.setClickable(true);
                    sliderDrawView.disableMeasureMode();
                } else {
                    mapView.setClickable(false);
                    sliderDrawView.enableMeasureMode(mapView);
                }
            }
        });

        final Button infoModeButton = (Button) findViewById(R.id.info);
        infoModeButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                boolean isInInfoMode = !mapView.isClickable();
                if (!isInInfoMode) {
                    infoModeButton.setBackgroundResource(R.drawable.infomode_on);
                } else {
                    infoModeButton.setBackgroundResource(R.drawable.infomode);
                }
                if (isInInfoMode) {
                    mapView.setClickable(true);
                    sliderDrawView.disableInfo();
                } else {
                    mapView.setClickable(false);
                    sliderDrawView.enableInfo(mapView);
                }
            }
        });

        try {
            handleOsmSliderView();
        } catch (Exception e) {
            e.printStackTrace();
        }

        saveCenterPref();

    }
    // -----------------------------------------------
    /**
     * Function to check and correct bounds / zoom level [for 'Map-Files only']
     * @param mapCenterLocation [point/zoom to check] result of PositionUtilities.getMapCenterFromPreferences(preferences,true,true);
     * @param i_parm 1= change mapCenterLocation values if out of range
     * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
     */
    public int checkCenterLocation(double[] mapCenterLocation,int i_parm)
    { // mj10777: i_rc=0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct mapCenterLocation values.
     int i_rc=0; // inside area
      // MapDatabase().openFile(File).getMapFileInfo()
      if (this.mapView.getMapDatabase() == null)
       return 100; // supported only with map files
     MapFileInfo mapFileInfo = this.mapView.getMapDatabase().getMapFileInfo();
     double bounds_west=(double)(mapFileInfo.boundingBox.getMinLongitude());
     double bounds_south=(double)(mapFileInfo.boundingBox.getMinLatitude());
     double bounds_east=(double)(mapFileInfo.boundingBox.getMaxLongitude());
     double bounds_north=(double)(mapFileInfo.boundingBox.getMaxLatitude());
     double centerX=mapFileInfo.boundingBox.getCenterPoint().getLongitude();
     double centerY=mapFileInfo.boundingBox.getCenterPoint().getLatitude();
     int maxZoom=this.mapView.getMapZoomControls().getZoomLevelMax();
     int minZoom=this.mapView.getMapZoomControls().getZoomLevelMin();
     // SpatialDatabasesManager.app_log(-1,"MapActivity.checkCenterLocation: center_location[x="+mapCenterLocation[0]+" ; y="+mapCenterLocation[1]+" ; z="+mapCenterLocation[2]+"] bbox=["+bounds_west+","+bounds_south+","+bounds_east+","+bounds_north+"]");
     if (((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east)) ||
          ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north)) ||
          ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom)))
      {
       if (((mapCenterLocation[0] >= bounds_west) && (mapCenterLocation[0] <= bounds_east)) &&
            ((mapCenterLocation[1] >=bounds_south) && (mapCenterLocation[1] <= bounds_north)))
       { // We are inside the Map-Area, but Zoom is not correct
        if  (mapCenterLocation[2] < minZoom)
        {
         i_rc=1;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=minZoom;
         }
        }
        if (mapCenterLocation[2] > maxZoom)
        {
         i_rc=2;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=maxZoom;
         }
        }
       }
       else
       {
        if  (mapCenterLocation[2] < minZoom)
        {
         i_rc=11;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=minZoom;
         }
        }
        if (mapCenterLocation[2] > maxZoom)
        {
         i_rc=12;
         if (i_parm == 1)
         {
          mapCenterLocation[2]=maxZoom;
         }
        }
        if ((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east))
        {
         i_rc=13;
         if (i_parm == 1)
         {
          mapCenterLocation[0]=centerX;
         }
        }
        if ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north))
        {
         i_rc=14;
         if (i_parm == 1)
         {
          mapCenterLocation[1]=centerY;
         }
        }
       }
       // SpatialDatabasesManager.app_log(-1,"MapActivity.checkCenterLocation: changed["+i_rc+"] : center_location[x="+mapCenterLocation[0]+" ; y="+mapCenterLocation[1]+" ; z="+mapCenterLocation[2]+"] bbox=["+bounds_west+","+bounds_south+","+bounds_east+","+bounds_north+"]");
      }
      return i_rc;
    }
    @Override
    protected void onResume() {

        // notes type
        boolean doCustom = preferences.getBoolean(Constants.PREFS_KEY_NOTES_CHECK, false);
        if (doCustom) {
            String opacityStr = preferences.getString(Constants.PREFS_KEY_NOTES_OPACITY, "100");
            String sizeStr = preferences.getString(Constants.PREFS_KEY_NOTES_SIZE, "15");
            String colorStr = preferences.getString(Constants.PREFS_KEY_NOTES_CUSTOMCOLOR, "blue");
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
        readData();

        super.onResume();
    }

    private void setTextScale() {
        String textSizeFactorStr = preferences.getString(Constants.PREFS_KEY_MAPSVIEW_TEXTSIZE_FACTOR, "1.0");
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
        unregisterReceiver(batteryReceiver);
        GpsManager.getInstance(this).removeListener(this);
        dataOverlay.dispose();
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
            GpsLocation location = GpsManager.getInstance(this).getLocation();
            if (location != null) {
                GeoPoint geoPoint = new GeoPoint((int) (location.getLatitude() * LibraryConstants.E6),
                        (int) (location.getLongitude() * LibraryConstants.E6));
                dataOverlay.setGpsPosition(geoPoint, 0f);
            }
            // dataOverlay.requestRedraw();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public boolean onTouch( View v, MotionEvent event ) {
        int action = event.getAction();
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "onTouch issued with motionevent: " + action); //$NON-NLS-1$

        if (action == MotionEvent.ACTION_UP) {
            saveCenterPref();

            // update zoom ui a bit later. This is ugly but
            // found no way until there is not event handling
            // in mapsforge
            new Thread(new Runnable(){
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable(){
                        public void run() {
                            int zoom = mapView.getMapPosition().getZoomLevel();
                            setZoomGuiText(zoom);
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

        int visibility = 0;
        if (categoriesNamesArray == null) {
            categoriesNamesArray = new String[]{""}; //$NON-NLS-1$
            visibility = 4; // invisible
        }
        doOsm = visibility != 4;
        boolean doOsmPref = preferences.getBoolean(Constants.PREFS_KEY_DOOSM, false);
        doOsm = doOsm && doOsmPref;
        if (!doOsm) {
            visibility = 4; // invisible
        }

        final String[] categoriesNamesArrayFinal = categoriesNamesArray;

        // slidingdrawer
        final int slidingId = R.id.osmslide;
        osmSlidingDrawer = (SlidingDrawer) findViewById(slidingId);
        osmSlidingDrawer.setVisibility(visibility);

        if (doOsm) {
            GridView buttonGridView = (GridView) findViewById(R.id.osmcategoriesview);

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, categoriesNamesArrayFinal){
                public View getView( final int position, View cView, ViewGroup parent ) {

                    final Button osmButton = new Button(MapsActivity.this);
                    osmButton.setText(categoriesNamesArrayFinal[position]);
                    osmButton.setBackgroundResource(R.drawable.osmcategory_button_drawable);
                    osmButton.setOnClickListener(new Button.OnClickListener(){
                        public void onClick( View v ) {
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
            syncOsmButton.setOnClickListener(new Button.OnClickListener(){
                public void onClick( View v ) {

                    if (!NetworkUtilities.isNetworkAvailable(getApplicationContext())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                        builder.setMessage(R.string.available_only_with_network).setCancelable(false)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                                    public void onClick( DialogInterface dialog, int id ) {
                                    }
                                });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        return;
                    }

                    final EditText input = new EditText(MapsActivity.this);
                    input.setText(""); //$NON-NLS-1$
                    Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle(R.string.set_description);
                    builder.setMessage(R.string.osm_insert_a_changeset_description);
                    builder.setView(input);
                    builder.setIcon(android.R.drawable.ic_dialog_alert)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                                public void onClick( DialogInterface dialog, int whichButton ) {
                                    sync(""); //$NON-NLS-1$
                                }
                            }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                                public void onClick( DialogInterface dialog, int whichButton ) {
                                    Editable value = input.getText();
                                    String newName = value.toString();
                                    sync(newName);
                                }
                            }).setCancelable(false).show();

                }

                private void sync( final String description ) {
                    final ProgressDialog progressDialog = ProgressDialog.show(MapsActivity.this,
                            "", getString(R.string.loading_data)); //$NON-NLS-1$
                    new AsyncTask<String, Void, String>(){
                        private Exception e = null;

                        protected String doInBackground( String... params ) {
                            String response = null;
                            try {
                                response = OsmUtilities.sendOsmNotes(MapsActivity.this, description);
                            } catch (Exception e) {
                                e.printStackTrace();
                                this.e = e;
                            }
                            return response;
                        }

                        protected void onPostExecute( String response ) {
                            progressDialog.dismiss();
                            if (e == null) {
                                String msg = getResources().getString(R.string.osm_notes_properly_uploaded);
                                if (response.toLowerCase().trim().startsWith(OsmUtilities.FEATURES_IMPORTED)) {
                                    String leftOver = response.replaceFirst(OsmUtilities.FEATURES_IMPORTED, ""); //$NON-NLS-1$
                                    if (leftOver.trim().length() > 0) {
                                        String text = leftOver.substring(1);
                                        text = text.replaceFirst("\\_", "/"); //$NON-NLS-1$//$NON-NLS-2$

                                        msg = MessageFormat.format(
                                                "Some of the features were uploaded, but not all of them ({0}).", text); //$NON-NLS-1$
                                        openAlertDialog(msg);
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                        builder.setMessage(msg)
                                                .setCancelable(false)
                                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                                                    public void onClick( DialogInterface dialog, int id ) {
                                                        try {
                                                            DaoNotes.deleteNotesByType(NoteType.OSM);
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                })
                                                .setNegativeButton(android.R.string.cancel,
                                                        new DialogInterface.OnClickListener(){
                                                            public void onClick( DialogInterface dialog, int id ) {
                                                            }
                                                        });
                                        AlertDialog alertDialog = builder.create();
                                        alertDialog.show();
                                    }
                                } else if (response.toLowerCase().trim().contains(OsmUtilities.ERROR_JSON)) {
                                    msg = getString(R.string.error_json_osm);
                                    openAlertDialog(msg);
                                } else if (response.toLowerCase().trim().contains(OsmUtilities.ERROR_OSM)) {
                                    msg = getString(R.string.error_osm_server);
                                    openAlertDialog(msg);
                                }

                            } else {
                                String msg = getResources().getString(R.string.an_error_occurred_while_uploading_osm_tags);
                                openAlertDialog(msg + e.getLocalizedMessage());
                            }
                        }
                    }.execute((String) null);
                }
            });
        }

    }

    /**
     * Open an alert dialog with a message and an ok button.
     *
     * @param msg the message to show.
     */
    private void openAlertDialog( String msg ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setMessage(msg).setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int id ) {
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onWindowFocusChanged( boolean hasFocus ) {
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (hasFocus) {
            double[] lastCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);

            GeoPoint geoPoint = new GeoPoint(lastCenter[1], lastCenter[0]);
            mapView.getController().setZoom((int) lastCenter[2]);
            mapView.getController().setCenter(geoPoint);

            setZoomGuiText((int) lastCenter[2]);

            readData();
        }
        saveCenterPref();
        super.onWindowFocusChanged(hasFocus);
    }

    private void setZoomGuiText( int newZoom ) {
        if (newZoom > maxZoomLevel) {
            newZoom = maxZoomLevel;
        }
        if (newZoom < minZoomLevel) {
            newZoom = minZoomLevel;
        }
        zoomLevelText.setText(formatter.format(newZoom));
    }

    public void setNewCenter( double lon, double lat, boolean drawIcon ) {
        mapView.getController().setCenter(new GeoPoint(lat, lon));
    }

    public void setNewCenterAtZoom( final double centerX, final double centerY, final int zoom ) {
        mapView.getController().setZoom(zoom);
        setZoomGuiText(zoom);
        mapView.getController().setCenter(
                new GeoPoint((int) (centerY * LibraryConstants.E6), (int) (centerX * LibraryConstants.E6)));
    }

    public double[] getCenterLonLat() {
        GeoPoint mapCenter = mapView.getMapPosition().getMapCenter();
        double[] lonLat = {mapCenter.longitudeE6 / LibraryConstants.E6, mapCenter.latitudeE6 / LibraryConstants.E6};
        return lonLat;
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect).setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_DATA, 2, "data").setIcon(android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, MENU_SCALE_ID, 3, R.string.mapsactivity_menu_toggle_scalebar).setIcon(R.drawable.ic_menu_scalebar);
        menu.add(Menu.NONE, MENU_COMPASS_ID, 4, R.string.mapsactivity_menu_toggle_compass).setIcon(
                android.R.drawable.ic_menu_compass);
        menu.add(Menu.NONE, CENTER_ON_MAP, 5, R.string.center_on_map).setIcon(android.R.drawable.ic_menu_mylocation);
        menu.add(Menu.NONE, GO_TO, 6, R.string.go_to).setIcon(android.R.drawable.ic_menu_myplaces);
        if (SmsUtilities.hasPhone(this)) {
            menu.add(Menu.NONE, MENU_SENDDATA_ID, 7, R.string.send_data).setIcon(android.R.drawable.ic_menu_send);
        }
        menu.add(Menu.NONE, MENU_MIXARE_ID, 8, R.string.view_in_mixare).setIcon(R.drawable.icon_datasource);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
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
            MixareHandler mixareHandler = new MixareHandler();
            if (!mixareHandler.isMixareInstalled(this)) {
                mixareHandler.installMixareFromMarket(this);
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
        case GO_TO: {
            return goTo();
        }
        case CENTER_ON_MAP: {
            MapGenerator mapGenerator = mapView.getMapGenerator();
            GeoPoint mapCenter;
            MapController controller = mapView.getController();
            if (mapGenerator instanceof DatabaseRenderer) {
                mapCenter = mapView.getMapDatabase().getMapFileInfo().mapCenter;
            } else {
                mapCenter = mapGenerator.getStartPoint();
                // Utilities.messageDialog(this,
                // "This operation works only for file based data maps", null);
            }
            controller.setCenter(mapCenter);
            controller.setZoom(minZoomLevel);
            saveCenterPref();
            return true;
        }
        default:
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void sendData() throws IOException {
        float[] nswe = getMapWorldBounds();
        List<SmsData> smsData = new ArrayList<SmsData>();
        List<Bookmark> bookmarksList = DaoBookmarks.getBookmarksInWorldBounds(nswe[0], nswe[1], nswe[2], nswe[3]);
        for( Bookmark bookmark : bookmarksList ) {
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
        for( Note note : notesList ) {
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
        for( SmsData data : smsData ) {
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
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                            for( String smsMsg : smsString ) {
                                SmsUtilities.sendSMSViaApp(MapsActivity.this, "", smsMsg); //$NON-NLS-1$
                            }
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int id ) {
                        }
                    });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

        }

    }
    private boolean goTo() {
        String[] items = new String[]{getString(R.string.goto_coordinate), getString(R.string.geocoding)};

        new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){

                    public void onClick( DialogInterface dialog, int whichButton ) {
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

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "Activity returned"); //$NON-NLS-1$
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (INSERTCOORD_RETURN_CODE): {
            if (resultCode == Activity.RESULT_OK) {

                float[] routePoints = data.getFloatArrayExtra(LibraryConstants.ROUTE);
                if (routePoints != null) {
                    // it is a routing request
                    try {
                        String name = data.getStringExtra(LibraryConstants.NAME);
                        if (name == null) {
                            name = "ROUTE_" + LibraryConstants.TIME_FORMATTER_GPX.format(new Date()); //$NON-NLS-1$
                        }
                        DaoGpsLog logDumper = new DaoGpsLog();
                        SQLiteDatabase sqliteDatabase = logDumper.getDatabase(this);
                        java.sql.Date now = new java.sql.Date(new java.util.Date().getTime());
                        long newLogId = logDumper.addGpsLog(this, now, now, name, 3, "blue", true); //$NON-NLS-1$

                        sqliteDatabase.beginTransaction();
                        try {
                            java.sql.Date nowPlus10Secs = now;
                            for( int i = 0; i < routePoints.length; i = i + 2 ) {
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
                                        ContactsContract.CommonDataKinds.Phone.TYPE}, null, null, null);

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
                        for( String sms : smsString ) {
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
        final String newDate = LibraryConstants.TIME_FORMATTER.format(new Date());
        final String proposedName = "bookmark " + newDate; //$NON-NLS-1$
        input.setText(proposedName);
        Builder builder = new AlertDialog.Builder(this).setTitle(R.string.mapsactivity_new_bookmark);
        builder.setMessage(R.string.mapsactivity_enter_bookmark_name);
        builder.setView(input);
        builder.setIcon(android.R.drawable.ic_dialog_info)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                    }
                }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        try {
                            Editable value = input.getText();
                            String newName = value.toString();
                            if (newName == null || newName.length() < 1) {
                                newName = proposedName;;
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
    private boolean bookmarksDeleted = false;
    private ProgressDialog bookmarksRemoveDialog;
    private Handler bookmarksRemoveHandler = new Handler(){
        public void handleMessage( Message msg ) {
            if (!bookmarksDeleted) {
                bookmarksRemoveDialog.incrementProgressBy(1);
            } else {
                bookmarksRemoveDialog.dismiss();
                mapView.invalidateOnUiThread();
            }
        }
    };

    private TextView zoomLevelText;

    public void inalidateMap() {
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

    private boolean boundsContain( int latE6, int lonE6, int nE6, int sE6, int wE6, int eE6 ) {
        return lonE6 > wE6 && lonE6 < eE6 && latE6 > sE6 && latE6 < nE6;
    }

    private synchronized void saveCenterPref() {
        MapViewPosition mapPosition = mapView.getMapPosition();
        GeoPoint mapCenter = mapPosition.getMapCenter();
        double lon = mapCenter.longitudeE6 / LibraryConstants.E6;
        double lat = mapCenter.latitudeE6 / LibraryConstants.E6;

        if (GPLog.LOG_ABSURD) {
            StringBuilder sb = new StringBuilder();
            sb.append("Map Center moved: "); //$NON-NLS-1$
            sb.append(lon);
            sb.append("/"); //$NON-NLS-1$
            sb.append(lat);
            GPLog.addLogEntry(this, sb.toString());
        }

        PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, mapPosition.getZoomLevel());
    }

    /**
     * Set center coords and zoom ready for the {@link MapsActivity} to focus again.
     *
     * <p>In {@link MapsActivity} the {@link MapsActivity#onWindowFocusChanged(boolean)}
     * will take care to zoom properly.
     *
     * @param centerX the lon coordinate. Can be <code>null</code>.
     * @param centerY the lat coordinate. Can be <code>null</code>.
     * @param zoom the zoom. Can be <code>null</code>.
     */
    public void setCenterAndZoomForMapWindowFocus( Double centerX, Double centerY, Integer zoom ) {
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

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive( Context context, Intent intent ) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int maxValue = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int chargedPct = (level * 100) / maxValue;
            updateBatteryCondition(chargedPct);
        }

    };

    private void updateBatteryCondition( int level ) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "BATTERY LEVEL GEOPAP: " + level); //$NON-NLS-1$
        StringBuilder sb = new StringBuilder();
        sb.append(level);
        if (level < 100) {
            sb.append("%"); //$NON-NLS-1$
        }
        batteryButton.setText(sb.toString());
    }

    public void onLocationChanged( Location loc ) {
        if (loc == null) {
            return;
        }

        if (this.mapView.getWidth() <= 0 || this.mapView.getWidth() <= 0) {
            return;
        }
        try {
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
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
                float accuracy = loc.getAccuracy();
                dataOverlay.setGpsPosition(point, accuracy);
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
                setNewCenter(lon, lat, false);
                if (GPLog.LOG_ABSURD)
                    GPLog.addLogEntry(this, "recentering triggered"); //$NON-NLS-1$
            }
        } catch (Exception e) {
            GPLog.error(this, "On location change error", e); //$NON-NLS-1$
            // finish the activity to reset
            finish();
        }
    }

    public void onProviderDisabled( String provider ) {
    }

    public void onProviderEnabled( String provider ) {
    }

    public void onStatusChanged( String provider, int status, Bundle extras ) {
    }

    public void gpsStart() {
    }

    public void gpsStop() {
    }

    public void onGpsStatusChanged( int event, GpsStatus status ) {
    }

    public boolean hasFix() {
        throw new RuntimeException("Not to be called");
    }

}
