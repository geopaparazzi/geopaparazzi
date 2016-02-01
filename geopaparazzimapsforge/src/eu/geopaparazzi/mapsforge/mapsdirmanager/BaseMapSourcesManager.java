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
package eu.geopaparazzi.mapsforge.mapsdirmanager;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.core.maps.BaseMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.CustomTileDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.MapDatabasesManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.mapsforge.mapsdirmanager.utils.DefaultMapurls;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Vectors;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.VectorLayerQueryModes;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import jsqlite.Exception;

/**
 * The base maps sources manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BaseMapSourcesManager {

    private SharedPreferences mPreferences;
    private List<BaseMap> mBaseMaps;

    private static BaseMapSourcesManager baseMapSourcesManager = null;

    private String selectedTileSourceType = "";
    private String selectedTableName = "";
    private String selectedTableTitle = "";
    private AbstractSpatialTable selectedBaseMapTable = null;

    private HashMap<BaseMap, AbstractSpatialTable> mBaseMaps2TablesMap = new HashMap<>();

    private File mMapnikFile;

    private boolean mReReadBasemaps = true;

    private BaseMapSourcesManager() {

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

//            boolean doSpatialiteRecoveryMode = mPreferences.getBoolean(SpatialiteLibraryConstants.PREFS_KEY_SPATIALITE_RECOVERY_MODE,
//                    false);
//            // doSpatialiteRecoveryMode=true;
//            if (doSpatialiteRecoveryMode) {
            // Turn on Spatialite Recovery Modus
            SPL_Vectors.VECTORLAYER_QUERYMODE = VectorLayerQueryModes.CORRECTIVEWITHINDEX;
//            }
            selectedTileSourceType = mPreferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE, ""); //$NON-NLS-1$
            selectedTableName = mPreferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_FILE, ""); //$NON-NLS-1$
            selectedTableTitle = mPreferences.getString(LibraryConstants.PREFS_KEY_TILESOURCE_TITLE, ""); //$NON-NLS-1$

            if (selectedTableName.length() == 0) {
                // select mapnik by default
                List<BaseMap> baseMaps = getBaseMaps();
                for (BaseMap baseMap : baseMaps) {
                    if (baseMap.databasePath.equals(mMapnikFile.getAbsolutePath())) {
                        selectedTableName = baseMap.databasePath;
                        selectedTableTitle = baseMap.title;
                        selectedTileSourceType = baseMap.mapType;

                        // TODO check if we need the table itself also
                        setTileSource(selectedTileSourceType, selectedTableName, selectedTableTitle);
                        break;
                    }
                }
            }

        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }

    }

    public static BaseMapSourcesManager getInstance() {
        if (baseMapSourcesManager == null) {
            baseMapSourcesManager = new BaseMapSourcesManager();
        }
        return baseMapSourcesManager;
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

                if (mBaseMaps.size() == 0) {
                    addBaseMapFromFile(mMapnikFile);
                }
                mReReadBasemaps = false;
            }
            return mBaseMaps;
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
        return Collections.emptyList();
    }

    /**
     * Reads the maps from preferences and extracts the tables necessary.
     *
     * @return the list of available BaseMaps.
     * @throws java.lang.Exception
     */
    private List<BaseMap> getBaseMapsFromPreferences() throws java.lang.Exception {
        String baseMapsJson = mPreferences.getString(BaseMap.BASEMAPS_PREF_KEY, "");
        List<BaseMap> baseMaps = BaseMap.fromJsonString(baseMapsJson);
        mBaseMaps2TablesMap.clear();

        // TODO this is ugly right now, needs to be changed
        for (BaseMap baseMap : baseMaps) {
            List<AbstractSpatialTable> tables = collectTables(new File(baseMap.databasePath));
            for (AbstractSpatialTable table : tables) {
                BaseMap tmpBaseMap = table2BaseMap(table);
                if (!mBaseMaps2TablesMap.containsKey(tmpBaseMap))
                    mBaseMaps2TablesMap.put(tmpBaseMap, table);
            }
        }
        return baseMaps;
    }

    private void saveBaseMapsToPreferences(List<BaseMap> baseMaps) throws JSONException {
        String baseMapJson = BaseMap.toJsonString(baseMaps);
        Editor editor = mPreferences.edit();
        editor.putString(BaseMap.BASEMAPS_PREF_KEY, baseMapJson);
        editor.apply();
    }

    public boolean addBaseMapFromFile(File file) {
        boolean foundBaseMap = false;
        try {
            if (mBaseMaps == null) mBaseMaps = new ArrayList<>();

            List<AbstractSpatialTable> collectedTables = collectTables(file);
            if (collectedTables.size() > 0) foundBaseMap = true;
            for (AbstractSpatialTable table : collectedTables) {
                saveToBaseMap(table);
            }
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
        return foundBaseMap;
    }

    public void removeBaseMap(BaseMap baseMap) throws JSONException {
        mBaseMaps.remove(baseMap);
        mBaseMaps2TablesMap.remove(baseMap);
        saveBaseMapsToPreferences(mBaseMaps);
    }

    @NonNull
    private List<AbstractSpatialTable> collectTables(File file) throws IOException, Exception {
        List<AbstractSpatialTable> collectedTables = new ArrayList<>();
            /*
             * add MAPURL TABLES
             */
        CustomTileDatabaseHandler customTileDatabaseHandler = CustomTileDatabasesManager.getInstance().getHandlerForFile(file);
        if (customTileDatabaseHandler != null) {
            List<CustomTileTable> tables = customTileDatabaseHandler.getTables(false);
            for (AbstractSpatialTable table : tables) {
                collectedTables.add(table);
            }
        } else {
            /*
             * add MAP TABLES
             */
            MapDatabasesManager mapDatabasesManager = MapDatabasesManager.getInstance();
            MapDatabaseHandler mapDatabaseHandler = mapDatabasesManager.getHandlerForFile(file);
            if (mapDatabaseHandler != null) {
                List<MapTable> tables = mapDatabaseHandler.getTables(false);
                for (AbstractSpatialTable table : tables) {
                    collectedTables.add(table);
                }
            } else {
                /*
                 * add MBTILES, GEOPACKAGE, RASTERLITE TABLES
                 */
                AbstractSpatialDatabaseHandler sdbHandler = SpatialDatabasesManager.getInstance().getRasterHandlerForFile(file);
                List<SpatialRasterTable> tables = sdbHandler.getSpatialRasterTables(false);
                for (AbstractSpatialTable table : tables) {
                    collectedTables.add(table);
                }
            }
        }
        return collectedTables;
    }

    private void saveToBaseMap(AbstractSpatialTable table) throws JSONException {
        BaseMap newBaseMap = table2BaseMap(table);
        mBaseMaps.add(newBaseMap);
        mBaseMaps2TablesMap.put(newBaseMap, table);
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
        newBaseMap.title = table.getTitle();
        return newBaseMap;
    }


    /**
     * Getter for the current selected map table.
     *
     * @return the current selected map table.
     */
    public AbstractSpatialTable getSelectedBaseMapTable() {
        return selectedBaseMapTable;
    }

    /**
     * Selected a Map through its BaseMap.
     *
     * @param baseMap the base map to use..
     * @throws jsqlite.Exception
     */
    public void setSelectedBaseMap(BaseMap baseMap) throws Exception {
        selectedTileSourceType = baseMap.mapType;
        selectedTableName = baseMap.databasePath;
        selectedTableTitle = baseMap.title;

        selectedBaseMapTable = mBaseMaps2TablesMap.get(baseMap);

        setTileSource(selectedTileSourceType, selectedTableName, selectedTableTitle);
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



}
