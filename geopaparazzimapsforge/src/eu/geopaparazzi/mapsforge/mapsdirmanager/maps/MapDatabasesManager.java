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
import java.util.HashMap;
import java.util.List;

import jsqlite.Exception;
import android.content.Context;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.MapTable;

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
    private final String[] sa_extentions = new String[]{".map",".xml"};
    private final int i_extention_map = 0;
    private static final int i_extention_xml = 1;

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
    public  String get_map_extention() {
        return sa_extentions[i_extention_map];
    }
    public  String get_xml_extention() {
        return sa_extentions[i_extention_xml];
    }
    public boolean  init( Context context, File mapsDir ) {
        File[] list_files = mapsDir.listFiles();
        List<MapDatabaseHandler> map_Handlers = new ArrayList<MapDatabaseHandler>();
        boolean b_nomedia_file=false;
        for( File this_file : list_files )
        { // nomedia logic: first check the files, if no '.nomedia' found: then its directories
             if (this_file.isFile())
             { // mj10777: collect .map databases
                String name = this_file.getName();
                if (Utilities.isNameFromHiddenFile(name)) {
                    continue;
                }
                if (name.endsWith(get_map_extention()) ) {
                    MapDatabaseHandler map = new MapDatabaseHandler(this_file.getAbsolutePath());
                    map_Handlers.add(map);

                }
                if (name.equals(".nomedia"))
                { // ignore all files of this directory
                 b_nomedia_file=true;
                 map_Handlers.clear();
                 return b_nomedia_file;
                }
            }
        }
        if (!b_nomedia_file)
        {
         for (int i=0;i<map_Handlers.size();i++)
         {
          mapHandlers.add(map_Handlers.get(i));
         }
        }
        map_Handlers.clear();
        for( File this_file : list_files )
        {
         if (this_file.isDirectory())
         {
          // mj10777: read recursive directories inside the sdcard/maps directory
          init(context, this_file);
         }
        }
        // GPLog.androidLog(-1,"MapDatabasesManager init[" + mapsDir.getName() + "] size["+mapHandlers.size()+"]");
        return b_nomedia_file;
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
     * Close  all Databases that may be open
     * <p>mapforge 'MapDatabase' file will be closed with '.closeFile();' if active
     */
    public void closeDatabases() throws Exception {
        for( MapDatabaseHandler mapHandler : mapHandlers ) {
            mapHandler.close();
        }
    }

}
