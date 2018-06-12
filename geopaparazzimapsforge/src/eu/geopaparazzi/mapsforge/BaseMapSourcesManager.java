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
package eu.geopaparazzi.mapsforge;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.maps.BaseMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.profiles.ProfilesHandler;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.mapsforge.databasehandlers.CustomTileDatabaseHandler;
import eu.geopaparazzi.mapsforge.databasehandlers.core.CustomTileDownloader;
import eu.geopaparazzi.mapsforge.databasehandlers.core.CustomTileTable;
import eu.geopaparazzi.mapsforge.databasehandlers.core.GeopackageTileDownloader;
import eu.geopaparazzi.mapsforge.databasehandlers.MapDatabaseHandler;
import eu.geopaparazzi.mapsforge.databasehandlers.core.MapGeneratorInternal;
import eu.geopaparazzi.mapsforge.databasehandlers.core.MapTable;
import eu.geopaparazzi.mapsforge.utils.DefaultMapurls;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Vectors;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.VectorLayerQueryModes;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;
import jsqlite.Exception;

/**
 * The base maps sources manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum BaseMapSourcesManager {
    INSTANCE;

    private SharedPreferences mPreferences;
    private List<BaseMap> mBaseMaps;

    private String selectedTileSourceType = "";
    private String selectedTableDatabasePath = "";
    private String selectedTableTitle = "";
    private AbstractSpatialTable selectedBaseMapTable = null;

    private HashMap<BaseMap, AbstractSpatialTable> mBaseMaps2TablesMap = new HashMap<>();

    private File mMapnikFile;

    private boolean mReReadBasemaps = true;

    BaseMapSourcesManager() {

        try {
            GPApplication gpApplication = GPApplication.getInstance();
            mPreferences = PreferenceManager.getDefaultSharedPreferences(gpApplication);
            /*
             * if they do not exist add two mapurl based mapnik and opencycle
             * tile sources as default ones. They will automatically
             * be backed into a mbtiles db.
             */
            File applicationSupporterDir = ResourcesManager.getInstance(gpApplication).getApplicationSupporterDir();
            mMapnikFile = new File(applicationSupporterDir, DefaultMapurls.Mapurls.mapnik.toString() + DefaultMapurls.MAPURL_EXTENSION);
            DefaultMapurls.checkAllSourcesExistence(gpApplication, applicationSupporterDir);

            boolean doSpatialiteRecoveryMode = mPreferences.getBoolean(SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE,
                    false);
            // doSpatialiteRecoveryMode=true;
            if (doSpatialiteRecoveryMode) {
                // Turn on Spatialite Recovery Modus
                SPL_Vectors.VECTORLAYER_QUERYMODE = VectorLayerQueryModes.CORRECTIVEWITHINDEX;
                // and reset it in the preferences
                Editor editor = mPreferences.edit();
                editor.putBoolean(SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE, false);
                editor.apply();
            }
            selectedTileSourceType = mPreferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE, ""); //$NON-NLS-1$
            selectedTableDatabasePath = mPreferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
            selectedTableTitle = mPreferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_TITLE, ""); //$NON-NLS-1$

            List<BaseMap> baseMaps = getBaseMaps();
            if (selectedTableDatabasePath.length() == 0 || !new File(selectedTableDatabasePath).exists()) {
                // select mapnik by default
                for (BaseMap baseMap : baseMaps) {
                    if (baseMap.databasePath.equals(mMapnikFile.getAbsolutePath())) {
                        selectedTableDatabasePath = baseMap.databasePath;
                        selectedTableTitle = baseMap.title;
                        selectedTileSourceType = baseMap.mapType;

                        setTileSource(selectedTileSourceType, selectedTableDatabasePath, selectedTableTitle);
                        selectedBaseMapTable = mBaseMaps2TablesMap.get(baseMap);
                        break;
                    }
                }
            } else {

                for (BaseMap baseMap : baseMaps) {
                    if (baseMap.databasePath.equals(selectedTableDatabasePath)) {
                        selectedBaseMapTable = mBaseMaps2TablesMap.get(baseMap);
                        break;
                    }
                }
            }

        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }

    }

    /**
     * Getter for the current available basemaps.
     *
     * @return the list of basemaps.
     */
    public List<BaseMap> getBaseMaps() {
        try {
            if (mBaseMaps == null || mReReadBasemaps) {
                mBaseMaps = getBaseMapsFromPreferences();

                mReReadBasemaps = false;
            }
            if (mBaseMaps.size() == 0) {
                addBaseMapsFromFile(mMapnikFile);
            }
            return mBaseMaps;
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
        return Collections.emptyList();
    }

    public void forceBasemapsreRead() {
        mBaseMaps = null;
        mReReadBasemaps = true;
    }

    /**
     * Reads the maps from preferences and extracts the tables necessary.
     *
     * @return the list of available BaseMaps.
     * @throws java.lang.Exception
     */
    private List<BaseMap> getBaseMapsFromPreferences() throws java.lang.Exception {
        mBaseMaps2TablesMap.clear();

        List<BaseMap> baseMaps;
        if (ProfilesHandler.INSTANCE.getActiveProfile() == null) {
            String baseMapsJson = mPreferences.getString(BaseMap.BASEMAPS_PREF_KEY, "");
            baseMaps = BaseMap.fromJsonString(baseMapsJson);
        } else {
            baseMaps = ProfilesHandler.INSTANCE.getBaseMaps();
        }

        // TODO this is ugly right now, needs to be changed
        for (BaseMap baseMap : baseMaps) {
            List<AbstractSpatialTable> tables = collectTablesFromFile(new File(baseMap.databasePath));
            for (AbstractSpatialTable table : tables) {
                BaseMap tmpBaseMap = table2BaseMap(table);
                if (!mBaseMaps2TablesMap.containsKey(tmpBaseMap))
                    mBaseMaps2TablesMap.put(tmpBaseMap, table);
            }
        }

        if (ProfilesHandler.INSTANCE.getActiveProfile() == null) {
            if ((selectedTableDatabasePath == null || selectedTableDatabasePath.length() == 0 || !new File(selectedTableDatabasePath).exists()) && baseMaps.size() > 0) {
                setSelectedBaseMap(baseMaps.get(0));
            }
        }
        return baseMaps;
    }

    public void saveBaseMapsToPreferences(List<BaseMap> baseMaps) throws JSONException {
        if (ProfilesHandler.INSTANCE.getActiveProfile() != null) {
            // if profiles are active, the dataset configs are readonly
            return;
        }
        String baseMapJson = BaseMap.toJsonString(baseMaps);
        Editor editor = mPreferences.edit();
        editor.putString(BaseMap.BASEMAPS_PREF_KEY, baseMapJson);
        editor.apply();
    }

    /**
     * Add basemaps from a given file.
     *
     * @param file the file to get the maps from.
     * @return the list of added basemaps or null if the map is not supported.
     */
    public List<BaseMap> addBaseMapsFromFile(File file) {
        List<BaseMap> foundBaseMaps = new ArrayList<>();
        if (!file.getName().startsWith("_")) {
            try {
                if (mBaseMaps == null) mBaseMaps = new ArrayList<>();

                List<AbstractSpatialTable> collectedTables = collectTablesFromFile(file);
                saveToBaseMap(collectedTables, foundBaseMaps);
            } catch (java.lang.Exception e) {
                GPLog.error(this, null, e);
                return null;
            }
        }
        return foundBaseMaps;
    }

    public void removeBaseMap(BaseMap baseMap) throws JSONException {
        try {
            mBaseMaps.remove(baseMap);
            mBaseMaps2TablesMap.remove(baseMap);
            saveBaseMapsToPreferences(mBaseMaps);
        } catch (java.lang.Exception e) {
            GPLog.error(this, "Unable to remove basemap " + baseMap, e);
        }
    }

    public void removeAllBaseMaps() throws JSONException {
        try {
            mBaseMaps.clear();
            mBaseMaps2TablesMap.clear();
            saveBaseMapsToPreferences(mBaseMaps);
        } catch (java.lang.Exception e) {
            GPLog.error(this, "Unable to remove all basemaps.", e);
        }
    }

    @NonNull
    private List<AbstractSpatialTable> collectTablesFromFile(File file) throws IOException, Exception {
//        GPLog.addLogEntry(this, "Processing file: " + file);
        List<AbstractSpatialTable> collectedTables = new ArrayList<>();
        /*
         * add MAPURL TABLES
         */
        try {
            CustomTileDatabaseHandler customTileDatabaseHandler = CustomTileDatabaseHandler.getHandlerForFile(file);
            if (customTileDatabaseHandler != null) {
                try {
                    List<CustomTileTable> tables = customTileDatabaseHandler.getTables(false);
                    for (AbstractSpatialTable table : tables) {
                        collectedTables.add(table);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                customTileDatabaseHandler.close();
            } else {
                /*
                 * add MAP TABLES
                 */
                MapDatabaseHandler mapDatabaseHandler = MapDatabaseHandler.getHandlerForFile(file);
                if (mapDatabaseHandler != null) {
                    try {
                        List<MapTable> tables = mapDatabaseHandler.getTables(false);
                        for (AbstractSpatialTable table : tables) {
                            collectedTables.add(table);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mapDatabaseHandler.close();
                } else {
                    /*
                     * add MBTILES, GEOPACKAGE, RASTERLITE TABLES
                     */
                    AbstractSpatialDatabaseHandler sdbHandler = getRasterHandlerForFile(file);
                    if (sdbHandler != null) {
                        try {
                            List<SpatialRasterTable> tables = sdbHandler.getSpatialRasterTables(false);
                            for (AbstractSpatialTable table : tables) {
                                collectedTables.add(table);
                            }
                        } finally {
                            sdbHandler.close();
                        }
                    }

                }

            }
        } catch (Exception e) {
            GPLog.error(this, "error reading file: " + file, e);
        }

        return collectedTables;
    }

    /**
     * Create a raster handler for the given file.
     *
     * @param file the file.
     * @return the handler or null if the file didn't fit the .
     */
    public static AbstractSpatialDatabaseHandler getRasterHandlerForFile(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            String name = file.getName();
            for (ESpatialDataSources spatialiteType : ESpatialDataSources.values()) {
                if (!spatialiteType.isSpatialiteBased()) {
                    continue;
                }
                String extension = spatialiteType.getExtension();
                if (name.endsWith(extension)) {
                    AbstractSpatialDatabaseHandler sdb = null;
                    if (name.endsWith(ESpatialDataSources.MBTILES.getExtension())) {
                        sdb = new MbtilesDatabaseHandler(file.getAbsolutePath(), null);
                    } else {
                        sdb = new SpatialiteDatabaseHandler(file.getAbsolutePath());
                    }
                    if (sdb.isValid()) {
                        return sdb;
                    }
                }
            }
        }
        return null;
    }

    private void saveToBaseMap(List<AbstractSpatialTable> tablesList, List<BaseMap> foundBaseMaps) throws JSONException {
        for (AbstractSpatialTable table : tablesList) {
            BaseMap newBaseMap = table2BaseMap(table);
            if (mBaseMaps.contains(newBaseMap))
                continue;
            mBaseMaps.add(newBaseMap);
            mBaseMaps2TablesMap.put(newBaseMap, table);
            foundBaseMaps.add(newBaseMap);
        }
        saveBaseMapsToPreferences(mBaseMaps);
    }

    @NonNull
    private BaseMap table2BaseMap(AbstractSpatialTable table) {
        BaseMap newBaseMap = new BaseMap();
        String databasePath = table.getDatabasePath();
        File databaseFile = new File(databasePath);
        newBaseMap.parentFolder = databaseFile.getParent();
        newBaseMap.databasePath = table.getDatabasePath();
        newBaseMap.mapType = table.getMapType();
        String title = table.getTitle();
        if (title == null) {
            title = table.getFileName();
        }
        newBaseMap.title = title;
        return newBaseMap;
    }


    /**
     * Getter for the current selected map table.
     *
     * @return the current selected map table.
     */
    public AbstractSpatialTable getSelectedBaseMapTable() {
        if (selectedBaseMapTable == null) {
            if (mBaseMaps2TablesMap.size() == 0) {
                try {
                    getBaseMaps();
                } catch (java.lang.Exception e) {
                    GPLog.error(this, null, e);
                }
            }
            try {
                BaseMap baseMap = null;
                if (mBaseMaps2TablesMap.size() > 0) {
                    baseMap = mBaseMaps2TablesMap.keySet().iterator().next();
                } else {
                    List<BaseMap> baseMaps = addBaseMapsFromFile(mMapnikFile);
                    if (baseMaps != null && baseMaps.size() > 0)
                        baseMap = baseMaps.get(0);
                }
                if (baseMap != null)
                    setSelectedBaseMap(baseMap);
            } catch (Exception e) {
                GPLog.error(this, "Error on setting selected basemap", e);
            }
        }
        return selectedBaseMapTable;
    }

    /**
     * Getter for the current selected basemap.
     *
     * @return the current selected basemap.
     */
    public BaseMap getSelectedBaseMap() {
        AbstractSpatialTable selectedBaseMapTable = getSelectedBaseMapTable();
        for (Map.Entry<BaseMap, AbstractSpatialTable> entry : mBaseMaps2TablesMap.entrySet()) {
            if (entry.getValue().getDatabasePath().equals(selectedBaseMapTable.getDatabasePath())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Selected a Map through its BaseMap.
     *
     * @param baseMap the base map to use..
     * @throws jsqlite.Exception
     */
    public void setSelectedBaseMap(BaseMap baseMap) throws Exception {
        if (baseMap == null) {
            selectedBaseMapTable = null;
            return;
        }
        try {
            selectedTileSourceType = baseMap.mapType;
            selectedTableDatabasePath = baseMap.databasePath;
            selectedTableTitle = baseMap.title;
            selectedBaseMapTable = mBaseMaps2TablesMap.get(baseMap);
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
            // fallback on mapnik
            List<BaseMap> addedBaseMaps = addBaseMapsFromFile(mMapnikFile);
            if (addedBaseMaps != null && addedBaseMaps.size() > 0) {
                BaseMap setBaseMap = addedBaseMaps.get(0);
                selectedTileSourceType = setBaseMap.mapType;
                selectedTableDatabasePath = setBaseMap.databasePath;
                selectedTableTitle = setBaseMap.title;
                selectedBaseMapTable = mBaseMaps2TablesMap.get(setBaseMap);
            } else {
                // give up
                return;
            }
        }

        setTileSource(selectedTileSourceType, selectedTableDatabasePath, selectedTableTitle);
    }

    /**
     * Sets the tilesource for the map.
     */
    private void setTileSource(String selectedTileSourceType, String selectedTileSourceFile, String selectedTableTitle) {
        Editor editor = mPreferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE, selectedTileSourceType);
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, selectedTileSourceFile);
        editor.putString(LibraryConstants.PREFS_KEY_TILESOURCE_TITLE, selectedTableTitle);
        editor.apply();
    }


    /**
     * Load the currently selected BaseMap.
     * <p/>
     * <p>This method should be called from within the activity defining the
     * {@link MapView}.
     * <p/>
     *
     * @param mapView Map-View to set.
     */
    public void loadSelectedBaseMap(MapView mapView) {
        AbstractSpatialTable selectedSpatialTable = getSelectedBaseMapTable();
        if (selectedSpatialTable != null) {
            int selectedSpatialDataTypeCode = ESpatialDataSources.getCode4Name(selectedTileSourceType);
            MapGenerator selectedMapGenerator = null;
            try {
                ESpatialDataSources selectedSpatialDataType = ESpatialDataSources.getType4Code(selectedSpatialDataTypeCode);
                switch (selectedSpatialDataType) {
                    case MAP:
                        MapTable selectedMapTable = (MapTable) selectedSpatialTable;
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
                        break;
                    case MBTILES:
                    case GPKG:
                    case RASTERLITE2:
                    case SQLITE: {
                        // TODO check
                        SpatialRasterTable selectedSpatialRasterTable = (SpatialRasterTable) selectedSpatialTable;
                        selectedMapGenerator = new GeopackageTileDownloader(selectedSpatialRasterTable);
                        clearTileCache(mapView);
                        mapView.setMapGenerator(selectedMapGenerator);
                    }
                    break;
                    case MAPURL: {
                        selectedMapGenerator = new CustomTileDownloader(selectedSpatialTable.getDatabaseFile());
                        try {
                            clearTileCache(mapView);
                            mapView.setMapGenerator(selectedMapGenerator);
                            if (GPLog.LOG_HEAVY)
                                GPLog.addLogEntry(this, "MapsDirManager -I-> MAPURL setMapGenerator[" + selectedTileSourceType
                                        + "] selected_map[" + selectedTableDatabasePath + "]");
                        } catch (java.lang.NullPointerException e_mapurl) {
                            GPLog.error(this, "MapsDirManager setMapGenerator[" + selectedTileSourceType + "] selected_map["
                                    + selectedTableDatabasePath + "]", e_mapurl);
                        }
                    }
                    break;
                    default:
                        break;
                }
            } catch (java.lang.Exception e) {
                selectedMapGenerator = MapGeneratorInternal.createMapGenerator(MapGeneratorInternal.mapnik);
                mapView.setMapGenerator(selectedMapGenerator);
                GPLog.error(this, "ERROR", e);
            }
        }
    }

    /**
     * Clear MapView TileCache.
     *
     * @param mapView the {@link MapView}.
     */
    private static void clearTileCache(MapView mapView) {
        if (mapView != null) {
            mapView.getInMemoryTileCache().destroy();
            if (mapView.getFileSystemTileCache().isPersistent()) {
                mapView.getFileSystemTileCache().setPersistent(false);
            }
            mapView.getFileSystemTileCache().destroy();
        }
    }

}
