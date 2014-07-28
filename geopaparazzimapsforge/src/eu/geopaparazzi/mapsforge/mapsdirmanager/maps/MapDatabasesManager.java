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
package eu.geopaparazzi.mapsforge.mapsdirmanager.maps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jsqlite.Exception;
import android.content.Context;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

/**
 * The map database manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 *  adapted to work with map databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
public class MapDatabasesManager {

    private List<MapDatabaseHandler> mapHandlers = new ArrayList<MapDatabaseHandler>();
    private HashMap<MapTable, MapDatabaseHandler> mapTablesMap = new HashMap<MapTable, MapDatabaseHandler>();
    private static MapDatabasesManager mapDbManager = null;

    private MapDatabasesManager() {
    }

    /**
     * @return the singleton instance.
     */
    public static MapDatabasesManager getInstance() {
        if (mapDbManager == null) {
            mapDbManager = new MapDatabasesManager();
        }
        return mapDbManager;
    }

    /**
     * Reset the manager.
     */
    public static void reset() {
        mapDbManager = null;
    }

    /**
     * Initialize the manager.
     * 
     * @param context the contect to use.
     * @param mapsDir the folder to initialize on.
     * @return <code>true</code> if a nomedia folder was hit.
     * @throws IOException if something went wrong.
     */
    public boolean init( Context context, File mapsDir ) throws IOException {
        File[] filesList = mapsDir.listFiles();
        List<MapDatabaseHandler> tmpMapHandlers = new ArrayList<MapDatabaseHandler>();
        boolean foundNomediaFile = false;
        for( File currentFile : filesList ) {
            // nomedia logic: first check the files, if no
            // '.nomedia' found: then its directories
            if (currentFile.isFile()) { // mj10777: collect .map databases
                String name = currentFile.getName();
                if (Utilities.isNameFromHiddenFile(name)) {
                    continue;
                }
                if (name.endsWith(SpatialDataType.MAP.getExtension())) {
                    MapDatabaseHandler map = new MapDatabaseHandler(currentFile.getAbsolutePath());
                    tmpMapHandlers.add(map);
                }
                if (name.equals(ResourcesManager.NO_MEDIA)) {
                    if (!currentFile.getParentFile().toURI().equals(mapsDir.toURI())) {
                        // ignore all files of this directory
                        // apart of the maps root folder
                        foundNomediaFile = true;
                        tmpMapHandlers.clear();
                        return foundNomediaFile;
                    }
                }
            }
        }
        if (!foundNomediaFile) {
            mapHandlers.addAll(tmpMapHandlers);
        }
        tmpMapHandlers.clear();
        for( File this_file : filesList ) {
            if (this_file.isDirectory()) {
                // mj10777: read recursive directories inside the sdcard/maps directory
                init(context, this_file);
            }
        }
        // GPLog.androidLog(-1,"MapDatabasesManager init[" + mapsDir.getName() +
        // "] size["+mapHandlers.size()+"]");
        return foundNomediaFile;
    }

    /**
     * @return the count of map handlers.
     */
    public int size() {
        return mapHandlers.size();
    }

    /**
     * @return the list of manhandlers.
     */
    public List<MapDatabaseHandler> getMapDatabaseHandlers() {
        return mapHandlers;
    }

    /**
     * Get the available tables.
     * 
     * @param forceRead force a re-reading of the resources.
     * @return the list of available tables.
     * @throws Exception  if something goes wrong.
     */
    public List<MapTable> getTables( boolean forceRead ) throws Exception {
        List<MapTable> tables = new ArrayList<MapTable>();
        for( MapDatabaseHandler mapHandler : mapHandlers ) {
            try {
                List<MapTable> mapTables = mapHandler.getTables(forceRead);
                for( MapTable mapTable : mapTables ) {
                    tables.add(mapTable);
                    mapTablesMap.put(mapTable, mapHandler);
                }
            } catch (java.lang.Exception e) {
                // ignore the handler and try to go on
                GPLog.error(this, "Error", e); //$NON-NLS-1$
            }
        }
        // Collections.sort(tables, new OrderComparator());
        return tables;
    }

    /**
     * Get the {@link MapDatabaseHandler} for a given {@link MapTable}.
     * 
     * @param mapTable the table.
     * @return the handler.
     * @throws Exception if something went wrong.
     */
    public MapDatabaseHandler getMapHandler( MapTable mapTable ) throws Exception {
        MapDatabaseHandler mapDatabaseHandler = mapTablesMap.get(mapTable);
        return mapDatabaseHandler;
    }

    /**
     * Get the {@link MapTable} for a given name.
     * 
     * @param table the tablename.
     * @return the table object.
     * @throws Exception if something goes wrong.
     */
    public MapTable getMapTableByName( String table ) throws Exception {
        List<MapTable> mapTables = getTables(false);
        for( MapTable mapTable : mapTables ) {
            if (mapTable.getDatabasePath().equals(table)) {
                return mapTable;
            }
        }
        return null;
    }
    /**
     * Close  all Databases that may be open
     * 
     * <p>mapforge 'MapDatabase' file will be closed with '.closeFile();' if active
     * 
     * @throws Exception if something goes wrong.
     */
    public void closeDatabases() throws Exception {
        for( MapDatabaseHandler mapHandler : mapHandlers ) {
            mapHandler.close();
        }
    }

}
