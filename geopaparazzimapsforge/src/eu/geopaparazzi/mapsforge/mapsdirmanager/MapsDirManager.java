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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Vectors;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.VectorLayerQueryModes;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import jsqlite.Exception;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.core.model.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.CustomTileDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.MapDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.GeopackageTileDownloader;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapGeneratorInternal;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.utils.DefaultMapurls;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;

/**
 * The manager of supported maps in the Application maps dir.
 *
 * @author Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings({"nls"})
public class MapsDirManager {

    /**
     * Zoom type to use when taking from internal values.
     */
    public static enum ZOOMTYPE {
        /** the default value. */
        DEFAULT,
        /** min */
        MIN,
        /** max */
        MAX,
        /** retain the same value as before. */
        SAME;
    }

    private File mapsDir = null;
    private static MapsDirManager mapsdirManager = null;
    private int selectedSpatialDataTypeCode = SpatialDataType.MBTILES.getCode();
    private String selectedTileSourceType = "";
    private String selectedTableName = "";
    private String selectedTableTitle = "";
    private AbstractSpatialTable selectedSpatialTable = null;
    private MapGenerator selectedMapGenerator;
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
    private String s_bounds_zoom = "";
    private File mapnikFile;
    private LinkedHashMap<String, List<String[]>> folderPath2TablesDataMap;
    private static boolean finishedLoading = false;

    private MapsDirManager() {
    }

    /**
      * Singleton getter for MapsDirManager.
      *
      * <ul>
      *  <li>Administration of sdcard/maps directory</li>
      *  <li>- collect all known maps and store basic information [init()] : call from application-Activity onCreate</li>
      *  <li>- filter information as needed [handleTileSources] </li>
      *  <li>- map selection : call from application-Activity  onMenuItemSelected</li>
      *  <li>- free resources : call from application-Activity  finish()</li>
      * </ul>
      * @return the {@link MapsDirManager}.
      *
      */
    public static MapsDirManager getInstance() {
        if (mapsdirManager == null) {
            finishedLoading = false;
            mapsdirManager = new MapsDirManager();
        }
        return mapsdirManager;
    }

    /**
     * Resets the manager setting it to null.
     */
    public static void reset() {
        mapsdirManager = null;
    }

    /**
      * Collect map-information in maps directory.
      * 
      * <p>call during application-Activity onCreate, when Application starts
      * <p>- initIfOk() after initializeResourcesManager() has run correctly
      * <ul>
      *  <li>DatabasesManagers will loop through Diretory, collecting basic Information for found [valid] maps</li>
      *  <li>- dependend on map-type</li>
      *  <li>call handleTileSources() to gather information to be used in application from results</li>
      * </ul>
      * 
      * @param context 'this' of Application Activity class
      * @param mapsDir Directory to search [ResourcesManager.getInstance(this).getMapsDir();]
      * @throws java.lang.Exception  if something goes wrong.
      */
    public void init( Context context, File mapsDir ) throws java.lang.Exception {
        try {
            if (mapsDir == null || !mapsDir.exists()) {
                mapsDir = ResourcesManager.getInstance(context).getMapsDir();
            }
            this.mapsDir = mapsDir;
        } catch (Throwable t) {
            GPLog.error(this, "MapsDirManager init[invalid maps directory]", t); //$NON-NLS-1$
        }
        /*
         * if they do not exist add two mapurl based mapnik and opencycle
         * tile sources as default ones. They will automatically
         * be backed into a mbtiles db.
        */
        mapnikFile = new File(mapsDir, DefaultMapurls.Mapurls.mapnik.toString() + DefaultMapurls.MAPURL_EXTENSION);
        DefaultMapurls.checkAllSourcesExistence(context, mapsDir);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean doSpatialiteRecoveryMode = preferences.getBoolean(SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE,
                false);
        // doSpatialiteRecoveryMode=true;
        if (doSpatialiteRecoveryMode) {
            // Turn on Spatialite Recovery Modus
            SPL_Vectors.VECTORLAYER_QUERYMODE = VectorLayerQueryModes.CORRECTIVEWITHINDEX;
        }
        selectedTileSourceType = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE, ""); //$NON-NLS-1$
        selectedTableName = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
        selectedTableTitle = preferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_TITLE, ""); //$NON-NLS-1$

        GPLog.GLOBAL_LOG_LEVEL = -1;
        SpatialDatabasesManager.reset();
        MapDatabasesManager.reset();
        CustomTileDatabasesManager.reset();
        if (mapsDir != null && mapsDir.exists()) {
            try {
                SpatialDatabasesManager.getInstance().init(context, mapsDir);
                MapDatabasesManager.getInstance().init(context, mapsDir);
                CustomTileDatabasesManager.getInstance().init(context, mapsDir);
                StringBuilder sb = new StringBuilder();
                sb.append("MapsDirManager manager[SpatialDatabasesManager] size[");
                sb.append(SpatialDatabasesManager.getInstance().getCount());
                sb.append("]\n");
                sb.append("MapsDirManager manager[SpatialDatabasesManager] size_raster[");
                sb.append(SpatialDatabasesManager.getInstance().getRasterDbCount());
                sb.append("]\n");
                sb.append("MapsDirManager manager[SpatialDatabasesManager] size_vector[");
                sb.append(SpatialDatabasesManager.getInstance().getVectorDbCount());
                sb.append("]\n");
                sb.append("MapsDirManager manager[MapDatabasesManager] size[");
                sb.append(MapDatabasesManager.getInstance().size());
                sb.append("]\n");
                sb.append("MapsDirManager manager[CustomTileDatabasesManager] size[");
                sb.append(CustomTileDatabasesManager.getInstance().size());
                sb.append("]\n");
                sb.append("MapsDirManager init[");
                sb.append(mapsDir.getAbsolutePath());
                sb.append("]");
                if (GPLog.LOG)
                    GPLog.addLogEntry(this, sb.toString());
                handleTileSources(context);
                if (doSpatialiteRecoveryMode) {
                    // Turn off Spatialite Recovery Modus after compleation
                    SPL_Vectors.VECTORLAYER_QUERYMODE = VectorLayerQueryModes.STRICT;
                    Editor editor = preferences.edit();
                    editor.putBoolean(SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE, false);
                    editor.commit();
                }
            } catch (Exception e) {
                GPLog.error(this, "MapsDirManager init[" + mapsDir.getAbsolutePath() + "]", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }
    /**
      * Collect information found about all raster tile supporting sources.
      * 
      * <p>call from init((), where the maps-directory has been read
      * <ul>
      *  <li>filter out information about collected maps</li>
      *  <li>store in a form needed be the application</li>
      *  <li>filter out portions not desired</li>
      * </ul>
      * <p>
      * 
     * @param context  the context to use.
      */
    private void handleTileSources( Context context ) throws Exception, IOException, FileNotFoundException {
        List<AbstractSpatialTable> tilesBasedTables = new ArrayList<AbstractSpatialTable>();
        AbstractSpatialTable mapnikTable = null;
        /*
          * add MAPURL TABLES
          */
        try {
            List<CustomTileTable> customtileTables = CustomTileDatabasesManager.getInstance().getTables(false);
            for( CustomTileTable table : customtileTables ) {
                String tableName = table.getTableName();
                if (!ignoreTileSource(tableName)) {
                    tilesBasedTables.add(table);
                    if (table.getDatabasePath().equals(mapnikFile.getAbsolutePath())) {
                        // if nothing is selected, this will be the default
                        mapnikTable = table;
                    }
                    if ((selectedSpatialTable == null) && (selectedTableName.equals(table.getDatabasePath()))) {
                        selectedSpatialTable = table;
                        selectedTileSourceType = table.getMapType();
                        selectedSpatialDataTypeCode = SpatialDataType.getCode4Name(selectedTileSourceType);
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            GPLog.error(this, "MapsDirManager handleTileSources CustomTileTable[" + mapsDir.getAbsolutePath() + "]", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        /*
          * add MAP TABLES
          */
        try {
            List<MapTable> mapTables = MapDatabasesManager.getInstance().getTables(false);
            for( MapTable table : mapTables ) {
                String tableName = table.getTableName();
                if (!ignoreTileSource(tableName)) {
                    tilesBasedTables.add(table);
                    // GPLog.androidLog(-1, "TreeNode[" + this_mapinfo.toString() + "]");
                    if ((selectedSpatialTable == null) && (selectedTableName.equals(table.getDatabasePath()))) {
                        selectedSpatialTable = table;
                        selectedTileSourceType = table.getMapType();
                        selectedSpatialDataTypeCode = SpatialDataType.getCode4Name(selectedTileSourceType);
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            GPLog.error(this, "MapsDirManager handleTileSources MapTable[" + mapsDir.getAbsolutePath() + "]", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        /*
         * add VECTOR TABLES
         */
        // loadVectorTables();
        /*
         * add MBTILES, GEOPACKAGE TABLES
         */
        try {
            List<SpatialRasterTable> spatialRasterTables = SpatialDatabasesManager.getInstance().getSpatialRasterTables(false);
            if (GPLog.LOG) {
                StringBuilder sb = new StringBuilder();
                sb.append("MapsDirManager manager[SpatialDatabasesManager] size_raster[");
                sb.append(SpatialDatabasesManager.getInstance().getRasterDbCount());
                sb.append("]");
                sb.append("MapsDirManager manager[SpatialDatabasesManager] size_vector[");
                sb.append(SpatialDatabasesManager.getInstance().getVectorDbCount());
                sb.append("]");
                GPLog.addLogEntry(this, sb.toString());
            }
            for( SpatialRasterTable table : spatialRasterTables ) {
                String tableName = table.getTableName(); //$NON-NLS-1$//$NON-NLS-2$
                if (!ignoreTileSource(tableName)) {
                    tilesBasedTables.add(table);
                    if ((selectedSpatialTable == null) && 
                        (selectedTableName.equals(table.getDatabasePath())) && (selectedTableTitle.equals(table.getTitle()))) {
                        selectedSpatialTable = table;
                        selectedTileSourceType = table.getMapType();
                        selectedSpatialDataTypeCode = SpatialDataType.getCode4Name(selectedTileSourceType);
                    }
                }
            }
        } catch (jsqlite.Exception e) {
            GPLog.error(this, "MapsDirManager handleTileSources SpatialRasterTable[" + mapsDir.getAbsolutePath() + "]", e);
        }
        if ((selectedSpatialTable == null) && (mapnikTable != null)) {
            // if nothing was selected OR the selected not found then
            // 'mapnik' as default [this should always exist]
            selectedSpatialTable = mapnikTable;
            selectedTileSourceType = selectedSpatialTable.getMapType();
            selectedSpatialDataTypeCode = SpatialDataType.getCode4Name(selectedTileSourceType);
            selectedTableName = selectedSpatialTable.getDatabasePath();
            selectedTableTitle = selectedSpatialTable.getTitle();
        }
        if (GPLog.LOG)
            GPLog.addLogEntry(this, "MapsDirManager handleTileSources selected_map[" + selectedTableName + ","+selectedTableTitle+"]");

        createTree(tilesBasedTables);
    }

    /**
     * Getter for the map of folders and contained tables.
     * 
     * @return the map of folders and tables descriptions.
     */
    public LinkedHashMap<String, List<String[]>> getFolder2TablesMap() {
        return folderPath2TablesDataMap;
    }

    /**
     * Check if the loading of sources has finished.
     *
     * @return <code>true</code>, if the loading process has finished.
     */
    public boolean finishedLoading() {
        return finishedLoading;
    }

    private void createTree( List<AbstractSpatialTable> tilesBasedTables ) {
        folderPath2TablesDataMap = new LinkedHashMap<String, List<String[]>>();
        List<String> parentPaths = new ArrayList<String>();
        for( AbstractSpatialTable spatialTable : tilesBasedTables ) {
            File file = spatialTable.getDatabaseFile();
            File parentFolder = file.getParentFile();
            String absolutePath = parentFolder.getAbsolutePath();
            if (!parentPaths.contains(absolutePath))
                parentPaths.add(absolutePath);
        }

        Comparator<String> pathComparator = new Comparator<String>(){
            public int compare( String p1, String p2 ) {
                if (p2.contains(p1)) {
                    return -1;
                } else if (p1.contains(p2)) {
                    return 1;
                }

                return 0;
            }
        };
        Collections.sort(parentPaths, pathComparator);

        for( String parentPath : parentPaths ) {
            folderPath2TablesDataMap.put(parentPath, new ArrayList<String[]>());
        }
        for( AbstractSpatialTable spatialTable : tilesBasedTables ) {
            File file = spatialTable.getDatabaseFile();
            File parentFolder = file.getParentFile();
            String absolutePath = parentFolder.getAbsolutePath();
            List<String[]> list = folderPath2TablesDataMap.get(absolutePath);
            String[] data = new String[]{//
            spatialTable.getDatabasePath(),//
                    spatialTable.getMapType()
                    ,spatialTable.getTitle()//
            };
            list.add(data);
        }

        // sort the sources
        for( Entry<String, List<String[]>> entry : folderPath2TablesDataMap.entrySet() ) {
            List<String[]> value = entry.getValue();
            Comparator<String[]> sourceNameComparator = new Comparator<String[]>(){
                public int compare( String[] p1, String[] p2 ) {
                    String path1 = p1[0];
                    String path2 = p2[0];
                    File file1 = new File(path1);
                    File file2 = new File(path2);
                    String name1 = file1.getName();
                    String name2 = file2.getName();
                    return name1.compareTo(name2);
                }
            };
            Collections.sort(value, sourceNameComparator);
        }

        finishedLoading = true;
    }
    /**
      * Filter out certain file-types
      *
      * @param name Filename to check
      * @return true if condition is fulfilled ; else false not fulfilled
      */
    private static boolean ignoreTileSource( String name ) {
        if (name.startsWith("_")) {
            return true;
        }
        return false;
    }

    /**
     * Getter for the current selected map table.
     * 
     * @return the current selected map table.
     */
    public AbstractSpatialTable getSelectedSpatialTable() {
        return selectedSpatialTable;
    }

    /**
      * Selected a Map through its {@link eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable}.
      *
      * <p>call from Application or Map-Activity
      * 
      * @param context  the context to use.
      * @param spatialTableData the table.
     * @throws Exception 
      */
    public void setSelectedSpatialTable( Context context, String[] spatialTableData ) throws Exception {
        // selectedNode = spatialTable;
        // selectedSpatialDataTypeCode = selectedNode.getType();
        selectedTileSourceType = spatialTableData[1];
        selectedSpatialDataTypeCode = SpatialDataType.getCode4Name(selectedTileSourceType);
        selectedTableName = spatialTableData[0];
        selectedTableTitle = spatialTableData[2];

        SpatialDataType selectedSpatialDataType = SpatialDataType.getType4Code(selectedSpatialDataTypeCode);
        switch( selectedSpatialDataType ) {
        case MAP: {
            MapTable selected_table = MapDatabasesManager.getInstance().getMapTableByName(selectedTableName);
            if (selected_table != null) {
                selectedSpatialTable = selected_table;
            }
        }
            break;
        case MBTILES:
        case GPKG:
        case RASTERLITE2:
        case SQLITE: {
            SpatialRasterTable selected_table = SpatialDatabasesManager.getInstance().getRasterTableByName(selectedTableName, selectedTableTitle);
            if (selected_table != null) {
                selectedSpatialTable = selected_table;
            }
        }
            break;
        case MAPURL: {
            CustomTileTable selected_table = CustomTileDatabasesManager.getInstance().getCustomTileTableByName(selectedTableName);
            if (selected_table != null) {
                selectedSpatialTable = selected_table;
            }
        }
            break;
        default:
            break;
        }

        // This will save the values to the user-proverences
        setTileSource(context, selectedTileSourceType, selectedTableName,selectedTableTitle);
    }

    /**
      * Load previously selected Map.
      * 
      * <p>This method should be called from within the activity defining the
      * {@link MapView}.
      *
      * @param mapView Map-View to set.
      * @param mapCenterLocation [point/zoom to check].
      */
    public void loadSelectedMap( MapView mapView, double[] mapCenterLocation ) {
        if (selectedSpatialTable != null) {
            try {
                selectedMapGenerator = null;
                minZoom = 0;
                maxZoom = 18;
                defaultZoom = 17;
                bounds_west = -180.0;
                bounds_south = -85.05113;
                bounds_east = 180.0;
                bounds_north = 85.05113;
                centerX = 0.0;
                centerY = 0.0;
                SpatialDataType selectedSpatialDataType = SpatialDataType.getType4Code(selectedSpatialDataTypeCode);
                switch( selectedSpatialDataType ) {
                case MAP: {
                    MapTable selectedMapTable = (MapTable) selectedSpatialTable;
                    if (selectedMapTable != null) {
                        minZoom = selectedMapTable.getMinZoom();
                        maxZoom = selectedMapTable.getMaxZoom();
                        defaultZoom = selectedMapTable.getDefaultZoom();
                        bounds_west = selectedMapTable.getMinLongitude();
                        bounds_east = selectedMapTable.getMaxLongitude();
                        bounds_south = selectedMapTable.getMinLatitude();
                        bounds_north = selectedMapTable.getMaxLatitude();
                        centerX = selectedMapTable.getCenterX();
                        centerY = selectedMapTable.getCenterY();
                        clearTileCache(mapView);
                        mapView.setMapFile(selectedMapTable.getDatabaseFile());
                        if (selectedMapTable.getXmlFile().exists()) {
                            try {
                                mapView.setRenderTheme(selectedMapTable.getXmlFile());
                            } catch (java.lang.Exception e) {
                                // ignore the theme
                                GPLog.error(this, "ERROR", e);
                            }
                        }
                        selectedMapGenerator = mapView.getMapGenerator();
                    }
                }
                    break;
                case MBTILES:
                case GPKG:
                case RASTERLITE2:
                case SQLITE: {
                    SpatialRasterTable selectedSpatialRasterTable = (SpatialRasterTable) selectedSpatialTable;
                    if (selectedSpatialRasterTable != null) {
                        minZoom = selectedSpatialRasterTable.getMinZoom();
                        maxZoom = selectedSpatialRasterTable.getMaxZoom();
                        defaultZoom = selectedSpatialRasterTable.getDefaultZoom();
                        bounds_west = selectedSpatialRasterTable.getMinLongitude();
                        bounds_east = selectedSpatialRasterTable.getMaxLongitude();
                        bounds_south = selectedSpatialRasterTable.getMinLatitude();
                        bounds_north = selectedSpatialRasterTable.getMaxLatitude();
                        centerX = selectedSpatialRasterTable.getCenterX();
                        centerY = selectedSpatialRasterTable.getCenterY();
                        selectedMapGenerator = new GeopackageTileDownloader(selectedSpatialRasterTable);
                        clearTileCache(mapView);
                        mapView.setMapGenerator(selectedMapGenerator);
                    }
                }
                    break;
                case MAPURL: {
                    CustomTileTable selectedCustomTilesTable = (CustomTileTable) selectedSpatialTable;
                    CustomTileDatabaseHandler customTileDatabaseHandler = CustomTileDatabasesManager.getInstance()
                            .getCustomTileDatabaseHandler(selectedCustomTilesTable);
                    if (selectedCustomTilesTable != null) {
                        minZoom = selectedCustomTilesTable.getMinZoom();
                        maxZoom = selectedCustomTilesTable.getMaxZoom();
                        defaultZoom = selectedCustomTilesTable.getDefaultZoom();
                        bounds_west = selectedCustomTilesTable.getMinLongitude();
                        bounds_east = selectedCustomTilesTable.getMaxLongitude();
                        bounds_south = selectedCustomTilesTable.getMinLatitude();
                        bounds_north = selectedCustomTilesTable.getMaxLatitude();
                        centerX = selectedCustomTilesTable.getCenterX();
                        centerY = selectedCustomTilesTable.getCenterY();
                        selectedMapGenerator = customTileDatabaseHandler.getCustomTileDownloader();
                        try {
                            clearTileCache(mapView);
                            mapView.setMapGenerator(selectedMapGenerator);
                            if (GPLog.LOG_HEAVY)
                                GPLog.addLogEntry(this, "MapsDirManager -I-> MAPURL setMapGenerator[" + selectedTileSourceType
                                        + "] selected_map[" + selectedTableName + "]");
                        } catch (java.lang.NullPointerException e_mapurl) {
                            GPLog.error(this, "MapsDirManager setMapGenerator[" + selectedTileSourceType + "] selected_map["
                                    + selectedTableName + "]", e_mapurl);
                        }
                    }
                }
                    break;
                default:
                    break;
                }
            } catch (jsqlite.Exception e) {
                selectedMapGenerator = MapGeneratorInternal.createMapGenerator(MapGeneratorInternal.mapnik);
                mapView.setMapGenerator(selectedMapGenerator);
                GPLog.error(this, "ERROR", e);
            }
            if (selectedMapGenerator != null) {
                // if mapCenterLocation == null, default values from seleted map will be used
                setMapViewCenter(mapView, mapCenterLocation, ZOOMTYPE.DEFAULT);
            }
        }
    }

    /**
      * Collect Information about found Vector-Tables
      *
      * @return amount of tables found
      */
    // private int loadVectorTables() {
    // if (vectorNodesList == null) {
    // vectorNodesList = new LinkedList<TreeNode< ? >>();
    // } else {
    // vectorNodesList.clear();
    // }
    // vectorinfoCount = vectorNodesList.size();
    // try {
    // List<SpatialVectorTable> spatialVectorTables =
    // SpatialDatabasesManager.getInstance().getSpatialVectorTables(false);
    // TreeNode this_vectorinfo = null;
    // for( int i = 0; i < spatialVectorTables.size(); i++ ) {
    // SpatialVectorTable table = spatialVectorTables.get(i);
    // this_vectorinfo = new TreeNode(vectorinfoCount++, table, null);
    // this_vectorinfo.setEnabled(table.isTableEnabled() == 1);
    // vectorNodesList.add(this_vectorinfo);
    // }
    // } catch (jsqlite.Exception e) {
    // GPLog.error(this, "MapsDirManager load_vector_classes() SpatialVectorTable[" +
    // mapsDir.getAbsolutePath() + "]", e);
    // }
    // Comparator<TreeNode< ? >> cp_directory_file =
    // TreeNode.getComparator(NodeSortParameter.SORT_ENABLED,
    // NodeSortParameter.SORT_DIRECTORY, NodeSortParameter.SORT_FILE_NAME);
    // Collections.sort(vectorNodesList, cp_directory_file);
    // return vectorinfoCount;
    // }

    /**
      * Clear MapView TileCache.
      * 
      * @param mapView the {@link MapView}.
      */
    private static void clearTileCache( MapView mapView ) {
        if (mapView != null) {
            mapView.getInMemoryTileCache().destroy();
            if (mapView.getFileSystemTileCache().isPersistent()) {
                mapView.getFileSystemTileCache().setPersistent(false);
            }
            mapView.getFileSystemTileCache().destroy();
        }
    }

    /**
      * Return MapView Bounds with present Zoom-level
      * 
      * @param mapView the map view to query.
      * @return the bounds array containing: [west,south,east,north wsg84 values, zoom-level, meters-width,meters-height]
      */
    public double[] getMapViewBoundsInfo( MapView mapView ) {
        double[] bounds_zoom = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        if (mapView != null) {
            Projection projection = mapView.getProjection();
            GeoPoint nw_Point = projection.fromPixels(0, 0);
            GeoPoint se_Point = projection.fromPixels(mapView.getWidth(), mapView.getHeight());
            bounds_zoom[0] = nw_Point.getLongitude(); // West
            bounds_zoom[3] = nw_Point.getLatitude(); // North
            bounds_zoom[1] = se_Point.getLatitude(); // South
            bounds_zoom[2] = se_Point.getLongitude(); // East
            bounds_zoom[4] = (double) mapView.getMapPosition().getZoomLevel();
            bounds_zoom[5] = Utilities.longitudeToMeters(se_Point.getLongitude(), nw_Point.getLongitude());
            bounds_zoom[6] = Utilities.latitudeToMeters(nw_Point.getLatitude(), se_Point.getLatitude());
            s_bounds_zoom = bounds_zoom[0] + "," + bounds_zoom[1] + "," + bounds_zoom[2] + "," + bounds_zoom[3] + ";"
                    + (int) bounds_zoom[4] + ";" + bounds_zoom[5] + "," + bounds_zoom[6];
        }
        return bounds_zoom;
    }

    /**
      * Return MapView Bounds with present Zoom-level as string
      * 
      * @param mapView the map view. 
      * @param doRecalculate if <code>true</code>, recalculates bounds.
      * @return s_bounds_zoom 5 values: west,south,east,north;zoom-level;meters-width;meters-height
      */
    public String getMapViewBoundsInfoAsString( MapView mapView, boolean doRecalculate ) {
        if (doRecalculate)
            getMapViewBoundsInfo(mapView);
        return s_bounds_zoom;
    }

    // TODO @mj10777 I really do not like to have this here.
    // this is a responsibility of the SpatialDatabasesManager,
    // which already has the method to do so.
    //
    // /**
    // * Return a list of VectorTables within these bounds an zoom-level
    // *
    // * we must have 5 values: west,south,east,north wsg84 values and a zoom-level
    // * if called with 'bounds_zoom == null': then all Tables, without checking, will be returned
    // *
    // * @param bounds_zoom 5 values: west,south,east,north wsg84 values and zoom-level
    // * @param i_check_enabled 0: return all ; 1= return only those that are enabled
    // * @param b_reread true: force new creation of vector-list ; false= read as is
    // * @return List<SpatialVectorTable> vector_TableList
    // */
    // public List<SpatialVectorTable> getSpatialVectorTables( double[] bounds_zoom, int
    // i_check_enabled, boolean b_reread ) {
    // List<SpatialVectorTable> vector_TableList = new ArrayList<SpatialVectorTable>();
    // // String
    // //
    // s_bounds_zoom_sent=bounds_zoom[0]+","+bounds_zoom[1]+","+bounds_zoom[2]+","+bounds_zoom[3]+";"+(int)bounds_zoom[4];
    // // String s_bounds_zoom_calc=get_bounds_zoom_meters_toString(1);
    // // GPLog.androidLog(-1, "getSpatialVectorTables: bounds_zoom_sent[" + s_bounds_zoom_sent+
    // // "] bounds_zoom_calc[" + s_bounds_zoom_calc+ "]");
    // if (b_reread)
    // vectorinfoCount = -1;
    // if ((vectorinfoCount < 0) && (vectorNodesList.size() == 0)) {
    // // if not loaded,
    // // load it
    // loadTreeNodesList();
    // }
    // SpatialDatabasesManager sdManager = SpatialDatabasesManager.getInstance();
    // for( int i = 0; i < vectorNodesList.size(); i++ ) {
    // TreeNode this_vectorinfo = vectorNodesList.get(i);
    // SpatialVectorTable vector_table = null;
    // try { // until DataListActivity is incorperted into MapsDirManager, we must read the
    // // enabled status in case it changed - getUniqueName()
    // vector_table = sdManager.getVectorTableByName(this_vectorinfo.getFilePath());
    // if (vector_table != null) {
    // this_vectorinfo.setEnabled(vector_table.isTableEnabled() == 1);
    // }
    // } catch (jsqlite.Exception e) {
    // // GPLog.androidLog(4, "MapsDirManager getSpatialVectorTables SpatialVectorTable[" +
    // // maps_dir.getAbsolutePath() + "]", e);
    // }
    // if (this_vectorinfo.checkPositionValues(bounds_zoom, i_check_enabled) > 0) {
    // /*
    // * 0=conditions not fullfilled ;
    // * 1=compleatly inside valid bounds ;
    // * 2=partially inside valid bounds
    // */
    // /* try
    // {
    // */
    // // vector_table=sdManager.getVectorTableByName(this_vectorinfo.getFileNamePath());
    // if (vector_table != null) {
    // vector_TableList.add(vector_table);
    // // GPLog.androidLog(-1, "TreeNode[" + this_vectorinfo.toString() + "]");
    // }
    // /*
    // }
    // catch (jsqlite.Exception e) {
    // GPLog.androidLog(4, "MapsDirManager getSpatialVectorTables SpatialVectorTable[" +
    // maps_dir.getAbsolutePath() + "]", e);
    // }
    // */
    // }
    // }
    // return vector_TableList;
    // }

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

    /**
      * Return current Zoom
      *
      * @return integer minzoom
      */
    public int getCurrentZoom() {
        return currentZoom;
    }

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
      * Return the map current center location as [x, y, zoom].
      * 
      * @param zoomType the type of zoom to pick. 
      * @return imapCenterLocation [point/zoom to set]
      */
    public double[] getMapCenterZoom( ZOOMTYPE zoomType ) {
        double zoomLevel;
        switch( zoomType ) {
        case MIN:
            zoomLevel = (double) getMinZoom();
            break;
        case MAX:
            zoomLevel = (double) getMaxZoom();
            break;
        default:
            zoomLevel = (double) defaultZoom;
            break;
        }
        double[] mapCenterLocation = new double[]{getCenterX(), getCenterY(), zoomLevel};
        return mapCenterLocation;
    }

    /**
      * Set MapView Center point.
      * 
      * @param mapView MapView to set (if not null).
      * @param mapCenterLocation [x, y, zoom] to set.
      * @param zoomType zoom tye to pick.
      * @return zoom-level
      */
    public int setMapViewCenter( MapView mapView, double[] mapCenterLocation, ZOOMTYPE zoomType ) {
        if (mapView == null)
            return defaultZoom;
        double centerX = 0;
        double centerY = 0;

        int zoom = 0;

        if (mapCenterLocation == null) {
            mapCenterLocation = getMapCenterZoom(zoomType);
            centerX = mapCenterLocation[0];
            centerY = mapCenterLocation[1];
            zoom = (int) mapCenterLocation[2];
            if (zoomType == ZOOMTYPE.SAME)
                zoom = mapView.getMapPosition().getZoomLevel();
        } else {
            if (mapCenterLocation.length > 1) {
                centerX = mapCenterLocation[0];
                centerY = mapCenterLocation[1];
                if (mapCenterLocation.length > 2) {
                    zoom = (int) mapCenterLocation[2];
                } else {
                    // function was incorrectly called with only 2 parameters, instead of 3
                    zoom = mapView.getMapPosition().getZoomLevel();
                }
            } else {
                // function was incorrectly called, use default postions from active map
                centerX = getCenterY();
                centerY = getCenterY();
                zoom = getDefaultZoom();
            }
            // GPLog.androidLog(-1,
            // "MapsDirInfo: setMapViewCenter[mapCenterLocation != null] ["+mapCenterLocation.length+"]");
        }
        checkPosition(centerX, centerY, zoom);
        GeoPoint geoPoint = new GeoPoint(getCurrentY(), getCurrentX());
        int setZoom = getCurrentZoom();
        mapView.getController().setZoom(setZoom);
        mapView.getController().setCenter(geoPoint);
        return setZoom;
    }

    /**
      * Check current position and correct if needed.
      * 
      * @param positionX x-position to check.
      * @param positionY y-position to check.
      * @param zoom zoom to check.
      */
    private void checkPosition( double positionX, double positionY, int zoom ) {
        // correct to position inside bounds/zoom-level
        if (positionX < bounds_west || positionX > bounds_east || positionY < bounds_south || positionY > bounds_north) {
            /* 
             * this is out of bounds, set center position even if
             * only one of the values are incorrect: the correct value may
             * not show something (.map files), thus force the change
             */
            positionX = getCenterX();
            positionY = getCenterY();
        }
        if (positionX < bounds_west || positionX > bounds_east) {
            if (positionX < bounds_west)
                positionX = bounds_west;
            if (positionX > bounds_east)
                positionX = bounds_east;
        }
        currentX = positionX;
        if ((positionY < bounds_south) || (positionY > bounds_north)) {
            if (positionY < bounds_south)
                positionY = bounds_south;
            if (positionY > bounds_north)
                positionY = bounds_north;
        }
        currentY = positionY;
        setCurrentZoom(zoom);
    }

    // /**
    // * Function to check and correct bounds / zoom level [for 'SpatialiteDatabaseHandler']
    // *
    // * @param mapCenterLocation [point/zoom to check] (most probably result of
    // PositionUtilities.getMapCenterFromPreferences(preferences,true,true);)
    // * @param doCorrectIfOutOfRange if <code>true</code>, change mapCenterLocation values if out
    // of range.
    // * @return 0=inside valid area/zoom ; i_rc > 0 outside area or zoom ; i_parm=0 no corrections
    // ; 1= correct tileBounds values.
    // */
    // public int checkCenterLocation( double[] mapCenterLocation, boolean doCorrectIfOutOfRange ) {
    // int i_rc = 0; // inside area
    // if (((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east))
    // || ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north))
    // || ((mapCenterLocation[2] < minZoom) || (mapCenterLocation[2] > maxZoom))) {
    // if (((mapCenterLocation[0] >= bounds_west) && (mapCenterLocation[0] <= bounds_east))
    // && ((mapCenterLocation[1] >= bounds_south) && (mapCenterLocation[1] <= bounds_north))) {
    // /*
    // * We are inside the Map-Area, but Zoom is not correct
    // */
    // if (mapCenterLocation[2] < minZoom) {
    // i_rc = 1;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = minZoom;
    // }
    // }
    // if (mapCenterLocation[2] > maxZoom) {
    // i_rc = 2;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = maxZoom;
    // }
    // }
    // } else {
    // if (mapCenterLocation[2] < minZoom) {
    // i_rc = 11;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = minZoom;
    // }
    // }
    // if (mapCenterLocation[2] > maxZoom) {
    // i_rc = 12;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[2] = maxZoom;
    // }
    // }
    // if ((mapCenterLocation[0] < bounds_west) || (mapCenterLocation[0] > bounds_east)) {
    // i_rc = 13;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[0] = centerX;
    // }
    // }
    // if ((mapCenterLocation[1] < bounds_south) || (mapCenterLocation[1] > bounds_north)) {
    // i_rc = 14;
    // if (doCorrectIfOutOfRange) {
    // mapCenterLocation[1] = centerY;
    // }
    // }
    // }
    // }
    // return i_rc;
    // }

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
    private static void setTileSource( Context context, String s_selected_type, String s_selected_map , String s_selected_title) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = preferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE, s_selected_type);
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, s_selected_map);
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE_TITLE, s_selected_title);
        editor.commit();
    }

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
            GPLog.androidLog(4, "MapsDirManager finish[" + mapsDir.getName() + "]", e);
        }
    }

}
