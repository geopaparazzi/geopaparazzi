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
package eu.geopaparazzi.mapsforge.mapsdirmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Comparator;
import java.util.LinkedList;

import jsqlite.Exception;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.core.model.GeoPoint;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.MapDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.GeopackageTileDownloader;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.CustomTileDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileDownloader;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapGeneratorInternal;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.MapsDirTreeViewList;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfo;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.OrderComparator;

/**
 * The manager of supported maps in the Application maps dir.
 *
 * @author Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MapsDirManager {
    private static final int MAP = 0;
    private static final int MBTILES = 1;
    private static final int GPKG = 2;
    private static final int SPATIALITE = 3;
    private static final int MAPURL = 4;
    private List<ClassNodeInfo> maptype_classes = new LinkedList<ClassNodeInfo>();
    private File maps_dir = null;
    private List<CustomTileDatabaseHandler> customtileHandlers = new ArrayList<CustomTileDatabaseHandler>();
    private HashMap<CustomTileTable, CustomTileDatabaseHandler> customtileTablesMap = new HashMap<CustomTileTable, CustomTileDatabaseHandler>();
    private static MapsDirManager mapsdirManager = null;
    private final String[] sa_extentions = new String[]{".mapurl"};
    private final int i_extention_mapurl = 0;
    private int i_selected_type = MBTILES;
    private String s_selected_type = "";
    private String s_selected_map = "";
    private ClassNodeInfo selected_mapinfo = null;
    private MapGenerator selected_mapGenerator;
    private double bounds_west = 180.0;
    private double bounds_south = -85.05113;
    private double bounds_east = 180.0;
    private double bounds_north = 85.05113;
    private int minZoom = 0;
    private int maxZoom = 18;
    private int defaultZoom;
    private double centerX = 0.0;
    private double centerY = 0.0;
    private double[] mapCenterLocation;
    private MapsDirManager() {
    }
    // -----------------------------------------------
    /**
      * Constructor MapsDirManager
      *
      * <ul>
      *  <li>Administration of sdcard/maps directory</li>
      *  <li>- collect all known maps and store basic information [init()] : call from application-Activity onCreate</li>
      *  <li>- filter information as needed [handleTileSources] </li>
      *  <li>- map selection : call from application-Activity  onMenuItemSelected</li>
      *  <li>- free resources : call from application-Activity  finish()</li>
      *
      * </ul>
      *
      * @param s_mbtiles_path full path to mbtiles file to open
      * @param mbtiles_metadata list of initial metadata values to set upon creation [otherwise can be null]
      * @return void
      */
    public static MapsDirManager getInstance() {
        if (mapsdirManager == null) {
            mapsdirManager = new MapsDirManager();
        }
        return mapsdirManager;
    }
    public File get_maps_dir() {
        return maps_dir;
    }
    public void reset() {
        mapsdirManager = null;
    }
    // -----------------------------------------------
    /**
      * Collect map-information in sdcard/maps directory
      * <p>call during application-Activity onCreate, when Application starts
      * <p>- initIfOk() after initializeResourcesManager() has run correctly
      * <ul>
      *  <li>DatabasesManagers will loop through Diretory, collecting basic Information for found [valid] maps</li>
      *  <li>- dependend on map-type</li>
      *  <li>call handleTileSources() to gather information to be used in application from results</li>
      * </ul>
      * @param context 'this' of Application Activity class
      * @param mapsDir Directory to search [ResourcesManager.getInstance(this).getMapsDir();]
      *
      */
    public void init( Context context, File mapsDir ) throws Exception, IOException, FileNotFoundException {
        try {
            if ((mapsDir == null) || (!mapsDir.exists())) { // a maps directory has not been
                                                            // supplied [default] or the given does
                                                            // not exist:
                                                            // - use the library logic to create a
                                                            // usable map directory
                mapsDir = ResourcesManager.getInstance(context).getMapsDir();
            }
        } catch (Throwable t) {
            GPLog.androidLog(4, "MapsDirManager init[invalid maps directory]", t);
        }
        maps_dir = mapsDir;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        s_selected_type = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE, ""); //$NON-NLS-1$
        s_selected_map = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
        // The TreeView type can be set here - depending on user/application preference
        // MapsDirTreeViewList.use_treeType=TreeType.FILEDIRECTORY; // [default]
        // MapsDirTreeViewList.use_treeType=MapsDirTreeViewList.TreeType.MAPTYPE;
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mapCenterLocation = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
        GPLog.GLOBAL_LOG_LEVEL = 1;
        GPLog.GLOBAL_LOG_TAG = "mj10777";
        SpatialDatabasesManager.reset();
        MapDatabasesManager.reset();
        CustomTileDatabasesManager.reset();
        if (maps_dir != null && maps_dir.exists()) {
            try {
                SpatialDatabasesManager.getInstance().init(context, maps_dir);
                GPLog.androidLog(-1, "MapsDirManager manager[SpatialDatabasesManager] size["
                        + SpatialDatabasesManager.getInstance().size() + "]");
                GPLog.androidLog(-1, "MapsDirManager manager[SpatialDatabasesManager] size_raster["
                        + SpatialDatabasesManager.getInstance().size_raster() + "]");
                GPLog.androidLog(-1, "MapsDirManager manager[SpatialDatabasesManager] size_vector["
                        + SpatialDatabasesManager.getInstance().size_vector() + "]");
                MapDatabasesManager.getInstance().init(context, maps_dir);
                GPLog.androidLog(-1, "MapsDirManager manager[MapDatabasesManager] size["
                        + MapDatabasesManager.getInstance().size() + "]");
                CustomTileDatabasesManager.getInstance().init(context, maps_dir);
                GPLog.androidLog(-1, "MapsDirManager manager[CustomTileDatabasesManager] size["
                        + CustomTileDatabasesManager.getInstance().size() + "]");
                GPLog.GLOBAL_LOG_LEVEL = 1;
                GPLog.androidLog(1, "MapsDirManager init[" + maps_dir.getAbsolutePath() + "]");
                handleTileSources(context);
            } catch (Exception e) {
                GPLog.androidLog(4, "MapsDirManager init[" + maps_dir.getAbsolutePath() + "]", e);
            }
        }
    }
    // -----------------------------------------------
    /**
      * Collect information found about all map-tables
      * <p>call from init((), where the maps-directory has been read
      * <ul>
      *  <li>filter out information about collected maps</li>
      *  <li>store in a form needed be the application</li>
      *  <li>filter out portions not desired</li>
      * </ul>
      * <p>from GeoPaparazziActivity.java [2013-10-11]
     * @param context
      */
    private void handleTileSources( Context context ) throws Exception, IOException, FileNotFoundException {
        int i_count_classes = 0;
        int i_type = MAPURL;
        ClassNodeInfo this_mapinfo = null;
        ClassNodeInfo mapnik_mapinfo = null;
        // This will be our default map if everything else fails
        File mapnikFile = new File(maps_dir, "mapnik.mapurl");
        /*
          * add mapurl tables
          */
        try {
            List<CustomTileTable> customtileTables = CustomTileDatabasesManager.getInstance().getTables(false);
            for( CustomTileTable table : customtileTables ) {
                String name = "[" + table.getMapType() + "] " + table.getName();
                // GPLog.androidLog(-1,"MapsDirManager CustomTileTable name[" + name+
                // "] getFileName[" + table.getFileNamePath()+ "] getName[" + table.getName()+
                // "] getDescription["+table.getDescription()+"]");
                if (!ignoreTileSource(name)) {
                    this_mapinfo = new ClassNodeInfo(i_count_classes++, i_type, table.getMapType(), "CustomTileTable",
                            table.getFileNamePath(), table.getFileName(), table.getFileNamePath(), table.getName(),
                            table.getDescription(), table.getBounds_toString(), table.getCenter_toString(),
                            table.getZoom_Levels());
                    maptype_classes.add(this_mapinfo);
                    if (table.getFileNamePath().equals(mapnikFile.getAbsolutePath())) { // if
                                                                                        // nothing
                                                                                        // is
                                                                                        // selected,
                                                                                        // this will
                                                                                        // be the
                                                                                        // default
                        mapnik_mapinfo = this_mapinfo;
                    }
                    if ((selected_mapinfo == null) && (s_selected_map.equals(table.getFileNamePath()))) {
                        selected_mapinfo = this_mapinfo;
                        s_selected_type = selected_mapinfo.getTypeText();
                        i_selected_type = selected_mapinfo.getType();
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            GPLog.androidLog(4, "MapsDirManager handleTileSources CustomTileTable[" + maps_dir.getAbsolutePath() + "]", e);
        }
        /*
          * add also map tables
          */
        i_type = MAP;
        try {
            List<MapTable> mapTables = MapDatabasesManager.getInstance().getTables(false);
            for( MapTable table : mapTables ) {
                String name = "[" + table.getMapType() + "] " + table.getName();
                // GPLog.androidLog(-1,"MapsDirManager MapTable name[" + name+ "] getFileName[" +
                // table.getFileNamePath()+ "] getName[" + table.getName()+
                // "] getDescription["+table.getDescription()+"]");
                if (!ignoreTileSource(name)) {
                    this_mapinfo = new ClassNodeInfo(i_count_classes++, i_type, table.getMapType(), "MapTable",
                            table.getFileNamePath(), table.getFileName(), table.getFileNamePath(), table.getName(),
                            table.getDescription(), table.getBounds_toString(), table.getCenter_toString(),
                            table.getZoom_Levels());
                    maptype_classes.add(this_mapinfo);
                    if ((selected_mapinfo == null) && (s_selected_map.equals(table.getFileNamePath()))) {
                        selected_mapinfo = this_mapinfo;
                        s_selected_type = selected_mapinfo.getTypeText();
                        i_selected_type = selected_mapinfo.getType();
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            GPLog.androidLog(4, "MapsDirManager handleTileSources MapTable[" + maps_dir.getAbsolutePath() + "]", e);
        }
        /*
         * add also mbtiles,geopackage tables
         */
        try {
            // List<SpatialVectorTable> spatialVectorTables =  SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
            List<SpatialRasterTable> spatialRasterTables = SpatialDatabasesManager.getInstance().getSpatialRasterTables(false);
            GPLog.androidLog(-1,"MapsDirManager manager[SpatialDatabasesManager] size_raster["+SpatialDatabasesManager.getInstance().size_raster()+"]");
            GPLog.androidLog(-1,"MapsDirManager manager[SpatialDatabasesManager] size_vector["+SpatialDatabasesManager.getInstance().size_vector()+"]");
            // [26] [/mnt/extSdCard/maps/geopackage_files/Luciad_GeoPackage.gpkg]
            // [27] [/mnt/extSdCard/maps/geopackage_files/Luciad_GeoPackage.gpkg]
            for( SpatialRasterTable table : spatialRasterTables ) {
                String name = "[" + table.getMapType() + "] " + table.getName();
                // GPLog.androidLog(-1,"MapsDirManager SpatialRasterTable name[" + name+
                // "] getFileName[" + table.getFileNamePath()+ "] getName[" + table.getName()+
                // "] getDescription["+table.getDescription()+"]");
                if (!ignoreTileSource(name)) {
                    i_type = MBTILES;
                    String s_type = table.getMapType();
                    if (!s_type.equals("mbtiles")) {
                        i_type = SPATIALITE;
                        if (s_type.equals("gpkg"))
                            i_type = GPKG;
                    }
                    this_mapinfo = new ClassNodeInfo(i_count_classes++, i_type, s_type, "SpatialRasterTable",
                            table.getFileNamePath(), table.getFileName(), table.getFileNamePath(), table.getName(),
                            table.getDescription(), table.getBounds_toString(), table.getCenter_toString(),
                            table.getZoom_Levels());
                    maptype_classes.add(this_mapinfo);
                    if ((selected_mapinfo == null) && (s_selected_map.equals(table.getFileNamePath()))) {
                        selected_mapinfo = this_mapinfo;
                        s_selected_type = selected_mapinfo.getTypeText();
                        i_selected_type = selected_mapinfo.getType();
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            GPLog.androidLog(4, "MapsDirManager handleTileSources SpatialRasterTable[" + maps_dir.getAbsolutePath() + "]", e);
        }
        /*
         * if they do not exist add two mbtiles based mapnik and opencycle
         * tile sources as default ones. They will automatically
         * be backed into a mbtiles db.
        */
        i_type = MAPURL;
        AssetManager assetManager = context.getAssets();
        File opencycleFile = new File(maps_dir, "opencycle.mapurl");
        if ((!opencycleFile.exists()) && (assetManager != null)) {
            InputStream inputStream = assetManager.open("tilesources/opencycle.mapurl");
            FileOutputStream outputStream = new FileOutputStream(opencycleFile);
            FileUtilities.copyFile(inputStream, outputStream);
            if (opencycleFile.exists()) {
                this_mapinfo = new ClassNodeInfo(i_count_classes++, i_type, "mapurl", "CustomTileTable",
                        opencycleFile.getAbsolutePath(), opencycleFile.getName(), opencycleFile.getAbsolutePath(), "opencycle",
                        "opencycle", "-180.00000,-85.05113,180.00000,85.05113", "13.3777065575123,52.5162690144797", "0-18");
                maptype_classes.add(this_mapinfo);
                if ((selected_mapinfo == null) && (s_selected_map.equals(opencycleFile.getAbsolutePath()))) {
                    selected_mapinfo = this_mapinfo;
                    s_selected_type = selected_mapinfo.getTypeText();
                    i_selected_type = selected_mapinfo.getType();
                }
            }
        }
        if ((!mapnikFile.exists()) && (assetManager != null)) {
            InputStream inputStream = assetManager.open("tilesources/mapnik.mapurl");
            OutputStream outputStream = new FileOutputStream(mapnikFile);
            FileUtilities.copyFile(inputStream, outputStream);
            if (mapnikFile.exists()) { // this should be done as the last to insure a default
                                       // setting
                mapnik_mapinfo = new ClassNodeInfo(i_count_classes++, i_type, "mapurl", "CustomTileTable",
                        mapnikFile.getAbsolutePath(), mapnikFile.getName(), mapnikFile.getAbsolutePath(), "mapnik", "mapnik",
                        "-180.00000,-85.05113,180.00000,85.05113", "13.3777065575123,52.5162690144797", "0-18");
                maptype_classes.add(mapnik_mapinfo);
            }
        }
        if ((selected_mapinfo == null) && (mapnik_mapinfo != null)) { // if nothing was selected OR
                                                                      // the selected not found then
                                                                      // 'mapnick' as default [this
                                                                      // should always exist]
            selected_mapinfo = mapnik_mapinfo;
            s_selected_type = selected_mapinfo.getTypeText();
            i_selected_type = selected_mapinfo.getType();
        }
        GPLog.androidLog(-1, "MapsDirManager MapsDirTreeViewList.setMapTypeClasses count[" + maptype_classes.size() + "] ");
        // List will be returned sorted as Directory-File with levels set.
        maptype_classes = MapsDirTreeViewList.setMapTypeClasses(maptype_classes, get_maps_dir());
    }
    // -----------------------------------------------
    /**
      * Filter out certain file-types
      *
      * <p>call from handleTileSources()
      *
      * @param name Filename to check
      * @return true if condition is fullfilled ; else false not fullfilled
      */
    private boolean ignoreTileSource( String name ) {
        if (name.startsWith("_")) {
            return true;
        }
        return false;
    }
    // -----------------------------------------------
    /**
      * Application has selected a Map
      *
      * <p>call from Application or Map-Activity
      *
      * @param selected_classinfo Map that was selected
      * @param map_View Map-View to set (if not null)
      * @return i_rc 0 ir correct ; else false not fullfilled
      */
    public int selected_MapClassInfo( Context context, ClassNodeInfo selected_classinfo, MapView map_View,
            double[] mapCenterLocation ) {
        int i_rc = -1;
        if (selected_classinfo != null) {
            selected_mapinfo = selected_classinfo;
            s_selected_type = selected_mapinfo.getTypeText();
            i_selected_type = selected_mapinfo.getType();
            s_selected_map = selected_mapinfo.getFileNamePath();
            // This will save the values to the user-proverences
            setTileSource(context, s_selected_type, s_selected_map);
            i_rc = 0;
            if (map_View != null) {
                i_rc = load_Map(map_View, mapCenterLocation);
            }
        }
        // GPLog.androidLog(-1,"MapsDirManager -I-> selected_MapClassInf mapinfo["
        // +selected_mapinfo.getShortDescription()+ "] i_rc["+i_rc+"]");
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Load selected Map
      *
      * <p>call  directly from  Map-Activity
      *
      * @param selected_classinfo Map that was selected
      * @param map_View Map-View to set (if not null)
      * @return i_rc 0 if correct ; else false not fullfilled
      */
    public int load_Map( MapView map_View, double[] mapCenterLocation ) {
        int i_rc = -1;
        if ((map_View != null) && (selected_mapinfo != null)) {
            try {
                boolean b_redrawTiles = false;
                GPLog.androidLog(-1,
                        "MapsDirManager -I-> load_Map[" + s_selected_type + "] mapinfo[" + selected_mapinfo.getShortDescription()
                                + "]");
                selected_mapGenerator = null;
                minZoom = 0;
                maxZoom = 18;
                defaultZoom = 17;
                bounds_west = 180.0;
                bounds_south = -85.05113;
                bounds_east = 180.0;
                bounds_north = 85.05113;
                centerX = 0.0;
                centerY = 0.0;
                switch( i_selected_type ) {
                case MAP: {
                    MapTable selected_table = MapDatabasesManager.getInstance().getMapTableByName(s_selected_map);
                    if (selected_table != null) {
                        minZoom = selected_table.getMinZoom();
                        maxZoom = selected_table.getMaxZoom();
                        defaultZoom = selected_table.getDefaultZoom();
                        bounds_west = selected_table.getMinLongitude();
                        bounds_east = selected_table.getMaxLongitude();
                        bounds_south = selected_table.getMinLatitude();
                        bounds_north = selected_table.getMaxLatitude();
                        centerX = selected_table.getCenterX();
                        centerY = selected_table.getCenterY();
                        map_View.setMapFile(selected_table.getFile());
                        if (selected_table.getXmlFile().exists()) {
                            try {
                                map_View.setRenderTheme(selected_table.getXmlFile());
                            } catch (FileNotFoundException e) { // ignore the theme
                            }
                        }
                        selected_mapGenerator = map_View.getMapGenerator();
                    }
                }
                    break;
                case MBTILES:
                case GPKG:
                case SPATIALITE: {
                    SpatialRasterTable selected_table = SpatialDatabasesManager.getInstance()
                            .getRasterTableByName(s_selected_map);
                    if (selected_table != null) {
                        minZoom = selected_table.getMinZoom();
                        maxZoom = selected_table.getMaxZoom();
                        defaultZoom = selected_table.getDefaultZoom();
                        bounds_west = selected_table.getMinLongitude();
                        bounds_east = selected_table.getMaxLongitude();
                        bounds_south = selected_table.getMinLatitude();
                        bounds_north = selected_table.getMaxLatitude();
                        centerX = selected_table.getCenterX();
                        centerY = selected_table.getCenterY();
                        selected_mapGenerator = new GeopackageTileDownloader(selected_table);
                        map_View.setMapGenerator(selected_mapGenerator);
                    }
                }
                    break;
                case MAPURL: {
                    CustomTileTable selected_table = CustomTileDatabasesManager.getInstance().getCustomTileTableByName(
                            s_selected_map);
                    if (selected_table != null) {
                        minZoom = selected_table.getMinZoom();
                        maxZoom = selected_table.getMaxZoom();
                        defaultZoom = selected_table.getDefaultZoom();
                        bounds_west = selected_table.getMinLongitude();
                        bounds_east = selected_table.getMaxLongitude();
                        bounds_south = selected_table.getMinLatitude();
                        bounds_north = selected_table.getMaxLatitude();
                        centerX = selected_table.getCenterX();
                        centerY = selected_table.getCenterY();
                        selected_mapGenerator = selected_table.getCustomTileDownloader();
                        map_View.setMapGenerator(selected_mapGenerator);
                    }
                }
                    break;
                default:
                    i_selected_type = i_rc;
                    break;
                }
            } catch (jsqlite.Exception e) {
                selected_mapGenerator = MapGeneratorInternal.createMapGenerator(MapGeneratorInternal.mapnik);
                map_View.setMapGenerator(selected_mapGenerator);
            }
            if (selected_mapGenerator != null) {
                i_rc = 0;
                if (mapCenterLocation == null) { // if the user has not given a desired position,
                                                 // retrieve it from the map-view
                    GeoPoint mapCenter = map_View.getMapPosition().getMapCenter();
                    mapCenterLocation = new double[]{mapCenter.getLongitude(), mapCenter.getLatitude(),
                            (double) map_View.getMapPosition().getZoomLevel()};
                }
                if (mapCenterLocation != null) { // this will adapt the present position of the
                                                 // map-view to the supported area of the map [true]
                    checkCenterLocation(mapCenterLocation, true);
                    GeoPoint geoPoint = new GeoPoint(mapCenterLocation[1], mapCenterLocation[0]);
                    map_View.getController().setZoom((int) mapCenterLocation[2]);
                    map_View.getController().setCenter(geoPoint);
                }
                // 20131108 mj10777: when a new map is loaded inside a MapActivity, the old tiles
                // are still shown
                map_View.invalidateOnUiThread();
                // if (b_redrawTiles)
                // map_View.redrawTiles();
                // else
                // map_View.invalidateOnUiThread();
            }
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Return Min Zoom
      *
      * <p>default :  0
      * <p>mbtiles : taken from value of metadata 'minzoom'
      * <p>map : value is given in 'StartZoomLevel'
      *
      * @return integer minzoom
      */
    public int getMinZoom() {
        return minZoom;
    }
    // -----------------------------------------------
    /**
      * Return Max Zoom
      *
      * <p>default :  22
      * <p>mbtiles : taken from value of metadata 'maxzoom'
      * <p>map : value not defined, seems to calculate bitmap from vector data [18]
      *
      * @return integer maxzoom
      */
    public int getMaxZoom() {
        return maxZoom;
    }
    /**
     * Function to check and correct bounds / zoom level [for 'SpatialiteDatabaseHandler']
     *
     * @param mapCenterLocation [point/zoom to check] (most probably result of PositionUtilities.getMapCenterFromPreferences(preferences,true,true);)
     * @param doCorrectIfOutOfRange if <code>true</code>, change mapCenterLocation values if out of range.
     * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections ; 1= correct tileBounds values.
     */
    public int checkCenterLocation( double[] mapCenterLocation, boolean doCorrectIfOutOfRange ) {
        int i_rc = 0; // inside area
        if (((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east))
                || ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north))
                || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
            if (((mapCenterLocation[0] >= bounds_west) && (mapCenterLocation[0] <= bounds_east))
                    && ((mapCenterLocation[1] >= bounds_south) && (mapCenterLocation[1] <= bounds_north))) {
                /*
                 *  We are inside the Map-Area, but Zoom is not correct
                 */
                if (mapCenterLocation[2] < minZoom) {
                    i_rc = 1;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = minZoom;
                    }
                }
                if (mapCenterLocation[2] > maxZoom) {
                    i_rc = 2;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = maxZoom;
                    }
                }
            } else {
                if (mapCenterLocation[2] < minZoom) {
                    i_rc = 11;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = minZoom;
                    }
                }
                if (mapCenterLocation[2] > maxZoom) {
                    i_rc = 12;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[2] = maxZoom;
                    }
                }
                if ((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east)) {
                    i_rc = 13;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[0] = centerX;
                    }
                }
                if ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north)) {
                    i_rc = 14;
                    if (doCorrectIfOutOfRange) {
                        mapCenterLocation[1] = centerY;
                    }
                }
            }
        }
        return i_rc;
    }
    /**
     * Sets the tilesource.
     *
     * <p>
     * If both arguments are set null, it will try to get info from the preferences,
     * and used sources are saved into preferences.
     * </p>
     * // sourceName if source is <code>null</code>, mapnik is used.
     * // mapfile the map file to use in case it is a database based source.
     * @param s_selected_type map-type of file
     * @param s_selected_map absolute path of file
     */
    private void setTileSource( Context context, String s_selected_type, String s_selected_map ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = preferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE, s_selected_type);
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, s_selected_map);
        editor.commit();
    }
    // -----------------------------------------------
    /**
      * Close and cleanup any resourced used by this manager
      *
      * <p>call from application-Activity  finish()
      *
      */
    public void finish() {
        try {
            SpatialDatabasesManager.getInstance().closeDatabases();
            MapDatabasesManager.getInstance().closeDatabases();
            CustomTileDatabasesManager.getInstance().closeDatabases();
        } catch (Exception e) {
            GPLog.androidLog(4, "MapsDirManager finish[" + maps_dir.getName() + "]", e);
        }
    }

}
