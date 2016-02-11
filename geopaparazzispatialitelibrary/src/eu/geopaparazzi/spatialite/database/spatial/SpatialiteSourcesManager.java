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

package eu.geopaparazzi.spatialite.database.spatial;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import eu.geopaparazzi.library.GPApplication;
import eu.geopaparazzi.library.core.maps.SpatialiteMap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.SPL_Vectors;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.VectorLayerQueryModes;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;
import jsqlite.Exception;

/**
 * The base maps sources manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum SpatialiteSourcesManager {
    INSTANCE;

    private SharedPreferences mPreferences;
    private List<SpatialiteMap> mSpatialiteMaps;

    private HashMap<SpatialiteMap, SpatialVectorTable> mSpatialiteMaps2TablesMap = new HashMap<>();

    private boolean mReReadBasemaps = true;

    SpatialiteSourcesManager() {
        try {
            GPApplication gpApplication = GPApplication.getInstance();
            mPreferences = PreferenceManager.getDefaultSharedPreferences(gpApplication);

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
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }

    }

    /**
     * Getter for the current available spatialite maps.
     *
     * @return the list of spatialite maps.
     */
    public List<SpatialiteMap> getSpatialiteMaps() {
        try {
            if (mSpatialiteMaps == null || mReReadBasemaps) {
                mSpatialiteMaps = getSpatialiteMapsFromPreferences();
                mReReadBasemaps = false;
            }
            return mSpatialiteMaps;
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
    private List<SpatialiteMap> getSpatialiteMapsFromPreferences() throws java.lang.Exception {
        String baseMapsJson = mPreferences.getString(SpatialiteMap.SPATIALITEMAPS_PREF_KEY, "");
        List<SpatialiteMap> spatialiteMaps = SpatialiteMap.fromJsonString(baseMapsJson);
        mSpatialiteMaps2TablesMap.clear();

        // TODO this is ugly right now, needs to be changed
        for (SpatialiteMap spatialiteMap : spatialiteMaps) {
            List<SpatialVectorTable> tables = collectTablesFromFile(new File(spatialiteMap.databasePath));
            for (SpatialVectorTable table : tables) {
                SpatialiteMap tmpSpatialiteMap = table2BaseMap(table);
                if (!mSpatialiteMaps2TablesMap.containsKey(tmpSpatialiteMap))
                    mSpatialiteMaps2TablesMap.put(tmpSpatialiteMap, table);
            }
        }
        return spatialiteMaps;
    }

    public void saveSpatialiteMapsToPreferences(List<SpatialiteMap> spatialiteMaps) throws JSONException {
        String spatialiteMapJson = SpatialiteMap.toJsonString(spatialiteMaps);
        Editor editor = mPreferences.edit();
        editor.putString(SpatialiteMap.SPATIALITEMAPS_PREF_KEY, spatialiteMapJson);
        editor.apply();
    }

    public boolean addSpatialiteMapFromFile(File file) {
        boolean foundSpatialiteMap = false;
        try {
            if (mSpatialiteMaps == null) mSpatialiteMaps = new ArrayList<>();

            List<SpatialVectorTable> collectedTables = collectTablesFromFile(file);
            if (collectedTables.size() > 0) foundSpatialiteMap = true;
            saveToSpatialiteMap(collectedTables);
        } catch (java.lang.Exception e) {
            GPLog.error(this, null, e);
        }
        return foundSpatialiteMap;
    }

    public void removeSpatialiteMap(SpatialiteMap spatialiteMap) throws JSONException {
        mSpatialiteMaps.remove(spatialiteMap);
        mSpatialiteMaps2TablesMap.remove(spatialiteMap);
        saveSpatialiteMapsToPreferences(mSpatialiteMaps);
    }

    public void removeSpatialiteMaps(List<SpatialiteMap> spatialiteMaps) throws JSONException {
        mSpatialiteMaps.removeAll(spatialiteMaps);
        for (SpatialiteMap spatialiteMap : spatialiteMaps) {
            mSpatialiteMaps2TablesMap.remove(spatialiteMap);
        }
        saveSpatialiteMapsToPreferences(mSpatialiteMaps);
    }

    @NonNull
    private List<SpatialVectorTable> collectTablesFromFile(File file) throws IOException, Exception {
        List<SpatialVectorTable> collectedTables = new ArrayList<>();
        /*
         * SPATIALITE TABLES
         */
        try (AbstractSpatialDatabaseHandler sdbHandler = SpatialDatabasesManager.getInstance().getVectorHandlerForFile(file)) {
            List<SpatialVectorTable> tables = sdbHandler.getSpatialVectorTables(false);
            for (SpatialVectorTable table : tables) {
                collectedTables.add(table);
            }
        }
        return collectedTables;
    }

    private void saveToSpatialiteMap(List<SpatialVectorTable> tablesList) throws JSONException {
        for (SpatialVectorTable table : tablesList) {
            SpatialiteMap newSpatialiteMap = table2BaseMap(table);
            mSpatialiteMaps.add(newSpatialiteMap);
            mSpatialiteMaps2TablesMap.put(newSpatialiteMap, table);
        }
        saveSpatialiteMapsToPreferences(mSpatialiteMaps);
    }


    public HashMap<SpatialiteMap, SpatialVectorTable> getSpatialiteMaps2TablesMap() {
        getSpatialiteMaps();
        return mSpatialiteMaps2TablesMap;
    }

    @NonNull
    private SpatialiteMap table2BaseMap(AbstractSpatialTable table) {
        SpatialiteMap newSpatialiteMap = new SpatialiteMap();
        newSpatialiteMap.databasePath = table.getDatabasePath();
        newSpatialiteMap.title = table.getTitle();
        if (table instanceof SpatialVectorTable) {
            try {
                SpatialVectorTable vectorTable = (SpatialVectorTable) table;
                newSpatialiteMap.tableType = vectorTable.getTableTypeDescription();
                GeometryType TYPE = GeometryType.forValue(vectorTable.getGeomType());
                newSpatialiteMap.geometryType = TYPE.getDescription();
            } catch (java.lang.Exception e) {
                GPLog.error(this, null, e);
            }
        }
        return newSpatialiteMap;
    }
}
