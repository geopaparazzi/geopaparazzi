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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jsqlite.Exception;
import android.content.Context;

import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.core.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.OrderComparator;

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
    private static final String[] sa_extentions = new String[]{".map",".xml"};
    private static final int i_extention_map = 0;
    private static final int i_extention_xml = 1;
    public static boolean isConnectedToInternet=false;
    private MapDatabasesManager() {
    }

    public static MapDatabasesManager getInstance() {
        if (mapDbManager == null) {
            mapDbManager = new MapDatabasesManager();
        }
        return mapDbManager;
    }

    public static void reset() {
        mapDbManager = null;
    }
    public static String get_map_extention() {
        return sa_extentions[i_extention_map];
    }
    public static String get_xml_extention() {
        return sa_extentions[i_extention_xml];
    }
    public void init( Context context, File mapsDir ) {
        File[] list_files = mapsDir.listFiles();
        for( File this_file : list_files ) {
            // mj10777: collect spatialite.geometries and .mbtiles databases
            if (this_file.isDirectory()) {
                // mj10777: read recursive directories inside the sdcard/maps directory
                init(context, this_file);
            } else {
                String name = this_file.getName();
                if (Utilities.isNameFromHiddenFile(name)) {
                    continue;
                }
                if (name.endsWith(get_map_extention()) ) {
                    MapDatabaseHandler map = new MapDatabaseHandler(this_file.getAbsolutePath());
                    mapHandlers.add(map);
                }
            }
        }
        // GPLog.androidLog(-1,"MapDatabasesManager init[" + mapsDir.getName() + "] size["+mapHandlers.size()+"]");
    }
    private boolean ignoreTileSource( String name ) {
        if (name.startsWith("_")) {
            return true;
        }
        return false;
    }
    public int size() {
        return mapHandlers.size();
    }
    public List<MapDatabaseHandler> getMapDatabaseHandlers() {
        return mapHandlers;
    }

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
                // ignore the handler and try to g on
            }
        }
        // Collections.sort(tables, new OrderComparator());
        return tables;
    }

    public MapDatabaseHandler getMapHandler( MapTable mapTable ) throws Exception {
        MapDatabaseHandler mapDatabaseHandler = mapTablesMap.get(mapTable);
        return mapDatabaseHandler;
    }

    public MapTable getMapTableByName( String table ) throws Exception {
        List<MapTable> mapTables = getTables(false);
        for( MapTable mapTable : mapTables ) {
            if (mapTable.getFileNamePath().equals(table)) {
                return mapTable;
            }
        }
        return null;
    }
    /**
     * Check for active Internet connection
     * <p>done in MapsDirManager
     */
    public boolean isConnectedToInternet()
    {
     return MapsDirManager.getInstance().isConnectedToInternet();
    }
    /**
     * Close  all Databases that may be open
     * <p>mapforge 'MapDatabase' file will be closed with '.closeFile();' if active
     */
    public void closeDatabases() throws Exception {
        for( MapDatabaseHandler mapHandler : mapHandlers ) {
            mapHandler.close();
        }
    }

}
