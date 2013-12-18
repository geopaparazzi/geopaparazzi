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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import jsqlite.Exception;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.core.model.GeoPoint;

import android.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.CustomTileDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.MapDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.GeopackageTileDownloader;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapGeneratorInternal;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfo;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.MapsDirTreeViewList;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;

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
    private List<ClassNodeInfo> vector_classes = new LinkedList<ClassNodeInfo>();
    private int i_vectorinfo_count = -1;
    private File maps_dir = null;
    private static MapsDirManager mapsdirManager = null;
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
    private int defaultZoom = 0;
    private int currentZoom = 0;
    private double centerX = 0.0;
    private double centerY = 0.0;
    private double currentX = 0.0;
    private double currentY = 0.0;
    private double[] mapCenterLocation;
    private MapView map_View = null;
    private String s_bounds_zoom = "";
    private File mapnikFile;
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
    public static void reset() {
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

        /*
         * if they do not exist add two mbtiles based mapnik and opencycle
         * tile sources as default ones. They will automatically
         * be backed into a mbtiles db.
        */
        mapnikFile = new File(maps_dir, "mapnik.mapurl");
        if (!mapnikFile.exists()) {
            InputStream inputStream = context.getResources().openRawResource(eu.geopaparazzi.mapsforge.R.raw.mapnik);
            OutputStream outputStream = new FileOutputStream(mapnikFile);
            FileUtilities.copyFile(inputStream, outputStream);
        }
        File opencycleFile = new File(maps_dir, "opencycle.mapurl");
        if (!opencycleFile.exists()) {
            InputStream inputStream = context.getResources().openRawResource(eu.geopaparazzi.mapsforge.R.raw.opencycle);
            FileOutputStream outputStream = new FileOutputStream(opencycleFile);
            FileUtilities.copyFile(inputStream, outputStream);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        s_selected_type = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE, ""); //$NON-NLS-1$
        s_selected_map = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
        // The TreeView type can be set here - depending on user/application preference
        // MapsDirTreeViewList.use_treeType=TreeType.FILEDIRECTORY; // [default]
        // MapsDirTreeViewList.use_treeType=MapsDirTreeViewList.TreeType.MAPTYPE;
        // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mapCenterLocation = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
        GPLog.GLOBAL_LOG_LEVEL = -1;
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
                GPLog.GLOBAL_LOG_LEVEL = -1;
                // GPLog.GLOBAL_LOG_TAG="mj10777";
                GPLog.androidLog(1, "MapsDirManager init[" + maps_dir.getAbsolutePath() + "]");
                // SpatialiteUtilities.find_shapes( context, maps_dir);
                /*
                List<String> list_sdcards = FileUtilities.list_sdcards();
                for (int i=0;i<list_sdcards.size();i++)
                {
                 GPLog.androidLog(1, "MapsDirManager sdcards[" + list_sdcards.get(i)+ "]");
                }
                * */
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
        int i_count_raster = 0;
        int i_type = MAPURL;
        ClassNodeInfo this_mapinfo = null;
        ClassNodeInfo mapnik_mapinfo = null;

        // GPLog.androidLog(-1, "MapsDirManager handleTileSources  selected_map[" + s_selected_map +
        // "]");
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
                    this_mapinfo = new ClassNodeInfo(i_count_raster++, i_type, table.getMapType(), "CustomTileTable",
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
                    this_mapinfo = new ClassNodeInfo(i_count_raster++, i_type, table.getMapType(), "MapTable",
                            table.getFileNamePath(), table.getFileName(), table.getFileNamePath(), table.getName(),
                            table.getDescription(), table.getBounds_toString(), table.getCenter_toString(),
                            table.getZoom_Levels());
                    maptype_classes.add(this_mapinfo);
                    // GPLog.androidLog(-1, "ClassNodeInfo[" + this_mapinfo.toString() + "]");
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
         * collect vector tables
         */
        load_vector_classes();
        /*
         * add also mbtiles,geopackage tables
         */
        try {
            List<SpatialRasterTable> spatialRasterTables = SpatialDatabasesManager.getInstance().getSpatialRasterTables(false);
            GPLog.androidLog(-1, "MapsDirManager manager[SpatialDatabasesManager] size_raster["
                    + SpatialDatabasesManager.getInstance().size_raster() + "]");
            GPLog.androidLog(-1, "MapsDirManager manager[SpatialDatabasesManager] size_vector["
                    + SpatialDatabasesManager.getInstance().size_vector() + "]");
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
                    this_mapinfo = new ClassNodeInfo(i_count_raster++, i_type, s_type, "SpatialRasterTable",
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
        if ((selected_mapinfo == null) && (mapnik_mapinfo != null)) {
            // if nothing was selected OR the selected not found then
            // 'mapnik' as default [this should always exist]
            selected_mapinfo = mapnik_mapinfo;
            s_selected_type = selected_mapinfo.getTypeText();
            i_selected_type = selected_mapinfo.getType();
            s_selected_map = selected_mapinfo.getFileNamePath();
        }
        GPLog.androidLog(-1, "MapsDirManager handleTileSources maptype_classes.count[" + maptype_classes.size()
                + "] selected_map[" + s_selected_map + "]");
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
      * @param map_View Map-View to set (if not null)
      * @param mapCenterLocation [point/zoom to check]
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
            if (this.map_View == null) {
                this.map_View = map_View;
            }
            try {
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
                        clear_TileCache();
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
                        clear_TileCache();
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
                        try {
                            clear_TileCache();
                            map_View.setMapGenerator(selected_mapGenerator);
                            GPLog.androidLog(1, "MapsDirManager -I-> MAPURL setMapGenerator[" + s_selected_type
                                    + "] selected_map[" + s_selected_map + "]");
                        } catch (java.lang.NullPointerException e_mapurl) {
                            GPLog.androidLog(4, "MapsDirManager -E-> MAPURL setMapGenerator[" + s_selected_type
                                    + "] selected_map[" + s_selected_map + "]", e_mapurl);
                        }
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
                GPLog.androidLog(4,
                        "MapsDirManager -E-> load_Map[" + s_selected_type + "] mapinfo[" + selected_mapinfo.getShortDescription()
                                + "]", e);
            }
            if (selected_mapGenerator != null) {
                i_rc = 0;
                // if mapCenterLocation == null, default values from seleted map will be used
                setMapViewCenter(map_View, mapCenterLocation, 1);
            }
        }
        return i_rc;
    }
    // -----------------------------------------------
    /**
      * Fill vector_classes with Information about found Vector-Tables
      *
      * @return i_vectorinfo_count amount of tables found
      */
    public int load_vector_classes() {
        if (vector_classes == null) {
            vector_classes = new LinkedList<ClassNodeInfo>();
        } else {
            vector_classes.clear();
        }
        i_vectorinfo_count = vector_classes.size();
        try {
            List<SpatialVectorTable> spatialVectorTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
            ClassNodeInfo this_vectorinfo = null;
            for( int i = 0; i < spatialVectorTables.size(); i++ ) {
                SpatialVectorTable table = spatialVectorTables.get(i);
                this_vectorinfo = new ClassNodeInfo(i_vectorinfo_count++, table.getGeomType(), table.getMapType(),
                        "SpatialVectorTable", table.getUniqueName(), table.getFileName(), table.getName(), table.getGeomName(),
                        table.getName() + File.separator + table.getGeomName(), table.getBounds_toString(),
                        table.getCenter_toString(), table.getZoom_Levels());
                this_vectorinfo.setEnabled(table.IsStyle());
                vector_classes.add(this_vectorinfo);
                // GPLog.androidLog(-1, "ClassNodeInfo[" + this_vectorinfo.toString() + "]");
            }
        } catch (jsqlite.Exception e) {
            GPLog.androidLog(4, "MapsDirManager load_vector_classes() SpatialVectorTable[" + maps_dir.getAbsolutePath() + "]", e);
        }
        Comparator<ClassNodeInfo> cp_directory_file = ClassNodeInfo.getComparator(ClassNodeInfo.SortParameter.SORT_ENABLED,
                ClassNodeInfo.SortParameter.SORT_DIRECTORY, ClassNodeInfo.SortParameter.SORT_FILE);
        Collections.sort(vector_classes, cp_directory_file);
        return i_vectorinfo_count;
    }
    // -----------------------------------------------
    /**
      * Clear MapView TileCache
      * - this.map_View is set in load_Map()
      * - to be used when a new map is loaded
      * @return nothing
      */
    private void clear_TileCache() {
        if (this.map_View != null) {
            this.map_View.getInMemoryTileCache().destroy();
            if (this.map_View.getFileSystemTileCache().isPersistent()) {
                this.map_View.getFileSystemTileCache().setPersistent(false);
            }
            this.map_View.getFileSystemTileCache().destroy();
        }
    }
    // -----------------------------------------------
    /**
      * Return MapView Bounds with present Zoom-level
      * 
      * <p>this.map_View is set in load_Map()
      * 
      * @return bounds_zoom 7 values: west,south,east,north wsg84 values and zoom-level, meters-width,meters-height
      */
    public double[] get_bounds_zoom_meters() {
        double[] bounds_zoom = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        if (this.map_View != null) {
            Projection projection = this.map_View.getProjection();
            GeoPoint nw_Point = projection.fromPixels(0, 0);
            GeoPoint se_Point = projection.fromPixels(this.map_View.getWidth(), this.map_View.getHeight());
            bounds_zoom[0] = nw_Point.getLongitude(); // West
            bounds_zoom[3] = nw_Point.getLatitude(); // North
            bounds_zoom[1] = se_Point.getLatitude(); // South
            bounds_zoom[2] = se_Point.getLongitude(); // East
            bounds_zoom[4] = (double) map_View.getMapPosition().getZoomLevel();
            bounds_zoom[5] = Utilities.longitudeToMeters(se_Point.getLongitude(), nw_Point.getLongitude());
            bounds_zoom[6] = Utilities.latitudeToMeters(nw_Point.getLatitude(), se_Point.getLatitude());
            s_bounds_zoom = bounds_zoom[0] + "," + bounds_zoom[1] + "," + bounds_zoom[2] + "," + bounds_zoom[3] + ";"
                    + (int) bounds_zoom[4] + ";" + bounds_zoom[5] + "," + bounds_zoom[6];
        }
        return bounds_zoom;
    }
    // -----------------------------------------------
    /**
      * Return MapView Bounds with present Zoom-level as string
      * - 0: return last set value ; 1: recalulate
      * @return s_bounds_zoom 5 values: west,south,east,north;zoom-level;meters-width;meters-height
      */
    public String get_bounds_zoom_meters_toString( int i_parm ) {
        switch( i_parm ) {
        case 1: { // recalulate and set s_bounds_zoom
            double[] bounds_zoom = get_bounds_zoom_meters();
        }
            break;
        }
        return s_bounds_zoom;
    }
    // -----------------------------------------------
    /**
      * Return a list of VectorTables within these bounds an zoom-level
      *
      * we must have 5 values: west,south,east,north wsg84 values and a zoom-level
      * if called with 'bounds_zoom == null': then all Tables, without checking, will be returned
      * @param bounds_zoom 5 values: west,south,east,north wsg84 values and zoom-level
      * @param i_check_enabled 0: return all ; 1= return only those that are enabled
      * @param b_reread true: force new creation of vector-list ; false= read as is
      * @return List<SpatialVectorTable> vector_TableList
      */
    public List<SpatialVectorTable> getSpatialVectorTables( double[] bounds_zoom, int i_check_enabled, boolean b_reread ) {
        List<SpatialVectorTable> vector_TableList = new ArrayList<SpatialVectorTable>();
        // String
        // s_bounds_zoom_sent=bounds_zoom[0]+","+bounds_zoom[1]+","+bounds_zoom[2]+","+bounds_zoom[3]+";"+(int)bounds_zoom[4];
        // String s_bounds_zoom_calc=get_bounds_zoom_meters_toString(1);
        // GPLog.androidLog(-1, "getSpatialVectorTables: bounds_zoom_sent[" + s_bounds_zoom_sent+
        // "] bounds_zoom_calc[" + s_bounds_zoom_calc+ "]");
        if (b_reread)
            i_vectorinfo_count = -1;
        if ((i_vectorinfo_count < 0) && (vector_classes.size() == 0)) { // if not loaded, load it
            load_vector_classes();
        }
        SpatialDatabasesManager sdManager = SpatialDatabasesManager.getInstance();
        for( int i = 0; i < vector_classes.size(); i++ ) {
            ClassNodeInfo this_vectorinfo = vector_classes.get(i);
            SpatialVectorTable vector_table = null;
            try { // until DataListActivity is incorperted into MapsDirManager, we must read the
                  // enabled status in case it changed
                vector_table = sdManager.getVectorTableByName(this_vectorinfo.getFileNamePath());
                if (vector_table != null) {
                    this_vectorinfo.setEnabled(vector_table.IsStyle());
                }
            } catch (jsqlite.Exception e) {
                // GPLog.androidLog(4, "MapsDirManager getSpatialVectorTables SpatialVectorTable[" +
                // maps_dir.getAbsolutePath() + "]", e);
            }
            if (this_vectorinfo.checkPositionValues(bounds_zoom, i_check_enabled) > 0) { // 0=conditions
                                                                                         // not
                                                                                         // fullfilled
                                                                                         // ;
                                                                                         // 1=compleatly
                                                                                         // inside
                                                                                         // valid
                                                                                         // bounds ;
                                                                                         // 2=partially
                                                                                         // inside
                                                                                         // valid
                                                                                         // bounds
                /* try
                {
                 */
                // vector_table=sdManager.getVectorTableByName(this_vectorinfo.getFileNamePath());
                if (vector_table != null) {
                    vector_TableList.add(vector_table);
                    // GPLog.androidLog(-1, "ClassNodeInfo[" + this_vectorinfo.toString() + "]");
                }
                /*
                }
                catch (jsqlite.Exception e) {
                    GPLog.androidLog(4, "MapsDirManager getSpatialVectorTables SpatialVectorTable[" + maps_dir.getAbsolutePath() + "]", e);
                }
                */
            }
        }
        return vector_TableList;
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
      * Return current Zoom
      *
      * @return integer minzoom
      */
    public int getCurrentZoom() {
        return currentZoom;
    }
    // -----------------------------------------------
    /**
      * Sets current Zoom
      * 
      * <p>Checking is done to insure that the new Zoom is 
      * inside the supported min/max Zoom-levels
      * <p>If the incoming value is invalid, the nearest max 
      * or min values will be set.  
      * 
      * @param zoomToSet the new zoom to set.
      * @return the current zoom integer vale.
      */
    public int setCurrentZoom( int zoomToSet ) {
        int minZoom = getMinZoom();
        int maxZoom = getMaxZoom();
        if (zoomToSet < minZoom) {
            currentZoom = minZoom;
        } else if (zoomToSet > maxZoom) {
            currentZoom = maxZoom;
        } else {
            currentZoom = zoomToSet;
        }
        return currentZoom;
    }
    // -----------------------------------------------
    /**
      * Return Default Zoom
      *
      * <p>default :  0
      * <p>mbtiles : taken from value of metadata 'minzoom'
      * <p>map : value is given in 'StartZoomLevel'
      *
      * @return integer minzoom
      */
    public int getDefaultZoom() {
        return defaultZoom;
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
    // -----------------------------------------------
    /**
      * Return CenterX postion of of active Map
      *
      * @return double centerX
      */
    public double getCenterX() {
        return centerX;
    }
    // -----------------------------------------------
    /**
      * Return CenterY postion of of active Map
      *
      * @return double centerY
      */
    public double getCenterY() {
        return centerY;
    }
    // -----------------------------------------------
    /**
      * Return CurrentX postion of of active Map
      *
      * @return double centerX
      */
    public double getCurrentX() {
        return currentX;
    }
    // -----------------------------------------------
    /**
      * Return CurrentY postion of of active Map
      *
      * @return double centerY
      */
    public double getCurrentY() {
        return currentY;
    }
    // -----------------------------------------------
    /**
      * Return MapCenter of active Map
      * - without zoom-level
      * @return imapCenterLocation [centerX, centerY]
      */
    public double[] getMapCenter() {
        double[] mapCenterLocation = new double[]{getCenterX(), getCenterY()};
        return mapCenterLocation;
    }
    // -----------------------------------------------
    /**
      * Return MapCenter of active Map
      * - with zoom-level
      * <p>-  if (i_default_zoom == 0)
      * <p>-- the getMaxZoom of the loaded map will be taken
      * <p>-  if (i_default_zoom == 1)
      * <p>-- the default Zoom of the loaded map will be taken
      * <p>-  if (i_default_zoom == 2)
      * <p>-- the getMinZoom() of the loaded map will be taken
      * @return imapCenterLocation [point/zoom to set]
      */
    public double[] getMapCenterZoom( int i_default_zoom ) {
        double d_zoom = (double) defaultZoom;
        if (i_default_zoom == 0)
            d_zoom = (double) getMaxZoom();
        if (i_default_zoom == 2)
            d_zoom = (double) getMinZoom();
        double[] mapCenterLocation = new double[]{getCenterX(), getCenterY(), d_zoom};
        return mapCenterLocation;
    }
    // -----------------------------------------------
    /**
      * set MapView Center point
      * - this should be the only function used to compleate this task
      * -- error logic has been build in use value incase the function was incorrectly called
      * <p>if (mapCenterLocation == null)
      * <p>- the getMinZoom() of the loaded map will be taken
      * <p>-  if (i_default_zoom == 0)
      * <p>-- the default Zoom of the loaded map will be taken
      * <p>-  if (i_default_zoom == 1)
      * <p>-- the default Zoom of the loaded map will be taken
      * <p>-  if (i_default_zoom == 2)
      * <p>-- the getMinZoom() of the loaded map will be taken
      * <p>-  if (i_default_zoom == 3)
      * <p>-- retain the present zoom-level of the MapView
      * @param map_View Map-View to set (if not null)
      * @param mapCenterLocation [point/zoom to set]
      * @param i_default_zoom [point/zoom to set]
      * @return zoom-level
      */
    public int setMapViewCenter( MapView map_View, double[] mapCenterLocation, int i_default_zoom ) {
        if (map_View == null)
            return defaultZoom;
        double d_position_x = 0, d_position_y = 0;
        // 0=correct to position inside bounds/zoom-level; 1: future: offer selection-list of valid
        // maps inside this area
        int i_position_correction_type = 0;
        int i_zoom = 0;

        if (mapCenterLocation == null) { // if the user has not given a desired position, retrieve
                                         // it from the active-map
                                         // GPLog.androidLog(-1,
                                         // "MapsDirInfo: setMapViewCenter[mapCenterLocation == null]");
            mapCenterLocation = getMapCenterZoom(i_default_zoom);
            d_position_x = mapCenterLocation[0];
            d_position_y = mapCenterLocation[1];
            i_zoom = (int) mapCenterLocation[2];
            if (i_default_zoom == 3)
                i_zoom = map_View.getMapPosition().getZoomLevel();
            // GPLog.androidLog(-1,
            // "MapsDirInfo: setMapViewCenter[mapCenterLocation == null] ["+d_position_x+","+d_position_y+";"+i_zoom+"] parm["+i_default_zoom+"]");
        } else {
            if (mapCenterLocation.length > 1) {
                d_position_x = mapCenterLocation[0];
                d_position_y = mapCenterLocation[1];
                if (mapCenterLocation.length > 2) {
                    i_zoom = (int) mapCenterLocation[2];
                } else { // function was incorrectly called with only 2 parameters, instead of 3
                    i_zoom = map_View.getMapPosition().getZoomLevel();
                }
                // GPLog.androidLog(-1,
                // "MapsDirInfo: setMapViewCenter[mapCenterLocation != null] ["+d_position_x+","+d_position_y+";"+i_zoom+"]");
            } else { // function was incorrectly called, use default postions from active map
                d_position_x = getCenterY();
                d_position_y = getCenterY();
                i_zoom = getDefaultZoom();
            }
            // GPLog.androidLog(-1,
            // "MapsDirInfo: setMapViewCenter[mapCenterLocation != null] ["+mapCenterLocation.length+"]");
        }
        check_valid_position(d_position_x, d_position_y, i_zoom, i_position_correction_type);
        GeoPoint geoPoint = new GeoPoint(getCurrentY(), getCurrentX());
        map_View.getController().setZoom(getCurrentZoom());
        map_View.getController().setCenter(geoPoint);
        // GPLog.androidLog(-1, "MapsDirManager setMapViewCenter[" + getCurrentX() + "," +
        // getCurrentY() + ", min_z[" + getMinZoom()+"], max_z[" + getMaxZoom()+"], current[" +
        // getCurrentZoom() + "]; default[" + getDefaultZoom() + "]");
        return map_View.getMapPosition().getZoomLevel();
    }
    // -----------------------------------------------
    /**
      * Check current position and correct if needed
      * - i_parm=0: correct to position inside bounds/zoom-level
      * -- positions will get 'stuck' at the min/max bounds
      * -- zoom will get 'stuck' at the min/max zoom
      * - i_parm=1: [TODO] offer selection-list of valid maps inside this area
      * -- pre-condition for this is the posibility to change maps inside the map_view
      * --- this at the moment is causing problems
      * @param d_position_x x-position to move to
      * @param d_position_y y-position to move to
      * @param i_zoom zoom to set
      * @param i_parm parameter: how to deal if out of bounds
      * @return integer return code
      */
    private int check_valid_position( double d_position_x, double d_position_y, int i_zoom, int i_parm ) {
        int i_rc = 0;
        switch( i_parm ) {
        default:
        case 0: { // correct to position inside bounds/zoom-level
            if (((d_position_x < bounds_west) || (d_position_x > bounds_east))
                    || ((d_position_y < bounds_south) || (d_position_y > bounds_north))) { // this
                                                                                           // is out
                                                                                           // of
                                                                                           // bounds,
                                                                                           // set
                                                                                           // center
                                                                                           // position
                                                                                           // even
                                                                                           // of
                                                                                           // only
                                                                                           // one of
                                                                                           // the
                                                                                           // values
                                                                                           // are
                                                                                           // incorrect:
                                                                                           // - the
                                                                                           // correct
                                                                                           // value
                                                                                           // may
                                                                                           // not
                                                                                           // show
                                                                                           // someting
                                                                                           // (.map
                                                                                           // files),
                                                                                           // thus
                                                                                           // force
                                                                                           // the
                                                                                           // change
                d_position_x = getCenterX();
                d_position_y = getCenterY();
                // GPLog.androidLog(-1,
                // "MapsDirInfo: setMapViewCenter[correction center] ["+d_position_x+","+d_position_y+";"+i_zoom+"]");
            }
            if ((d_position_x < bounds_west) || (d_position_x > bounds_east)) {
                if (d_position_x < bounds_west)
                    d_position_x = bounds_west;
                if (d_position_x > bounds_east)
                    d_position_x = bounds_east;
                // GPLog.androidLog(-1,
                // "MapsDirInfo: setMapViewCenter[correction X] ["+d_position_x+","+d_position_y+";"+i_zoom+"]");
            }
            currentX = d_position_x;
            if ((d_position_y < bounds_south) || (d_position_y > bounds_north)) {
                if (d_position_y < bounds_south)
                    d_position_y = bounds_south;
                if (d_position_y > bounds_north)
                    d_position_y = bounds_north;
                // GPLog.androidLog(-1,
                // "MapsDirInfo: setMapViewCenter[correction Y] ["+d_position_x+","+d_position_y+";"+i_zoom+"]");
            }
            currentY = d_position_y;
            setCurrentZoom(i_zoom);
        }
            break;
        case 1: { // offer selection-list of valid maps inside this area
        }
            break;
        }
        return i_rc;
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
