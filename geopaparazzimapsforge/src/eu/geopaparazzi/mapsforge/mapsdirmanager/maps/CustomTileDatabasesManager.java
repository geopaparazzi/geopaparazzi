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
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileTable;

/**
 * The custom tile database manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 *  adapted to work with custom tiles [mapsforge] Mark Johnson (www.mj10777.de)
 */
public class CustomTileDatabasesManager {

    private List<CustomTileDatabaseHandler> customtileHandlers = new ArrayList<CustomTileDatabaseHandler>();
    private HashMap<CustomTileTable, CustomTileDatabaseHandler> customtileTablesMap = new HashMap<CustomTileTable, CustomTileDatabaseHandler>();
    private static CustomTileDatabasesManager customtileDbManager = null;
    private static final String[] sa_extentions = new String[]{".mapurl"};
    private static final int i_extention_mapurl = 0;
    private CustomTileDatabasesManager() {
    }

    public static CustomTileDatabasesManager getInstance() {
        if (customtileDbManager == null) {
            customtileDbManager = new CustomTileDatabasesManager();
        }
        return customtileDbManager;
    }

    public static void reset() {
        customtileDbManager = null;
    }
    public static String get_mapurl_extention() {
        return sa_extentions[i_extention_mapurl];
    }
    public boolean init( Context context, File mapsDir ) {
        File[] list_files = mapsDir.listFiles();
        List<CustomTileDatabaseHandler> customtile_Handlers = new ArrayList<CustomTileDatabaseHandler>();
        boolean b_nomedia_file=false;
        for( File this_file : list_files )
        { // nomedia logic: first check the files, if no '.nomedia' found: then its directories
            if (this_file.isFile())
            {   // mj10777: collect .mapurl databases
                String name = this_file.getName();
                if (Utilities.isNameFromHiddenFile(name)) {
                    continue;
                }
                if (name.endsWith(get_mapurl_extention()) ) {
                    CustomTileDatabaseHandler map = new CustomTileDatabaseHandler(this_file.getAbsolutePath(),MapsDirManager.getInstance().get_maps_dir().getAbsolutePath());
                    customtile_Handlers.add(map);
                }
                if (name.equals(".nomedia"))
                { // ignore all files of this directory
                 b_nomedia_file=true;
                 customtile_Handlers.clear();
                 return b_nomedia_file;
                }
            }
        }
        if (!b_nomedia_file)
        {
         for (int i=0;i<customtile_Handlers.size();i++)
         {
          customtileHandlers.add(customtile_Handlers.get(i));
         }
        }
       customtile_Handlers.clear();
       for( File this_file : list_files )
       {
        if (this_file.isDirectory())
        {  // mj10777: read recursive directories inside the sdcard/maps directory
         init(context, this_file);
        }
       }
        // GPLog.androidLog(-1,"CustomTileDatabasesManager init[" + mapsDir.getName() + "] size["+customtileHandlers.size()+"]");
        return b_nomedia_file;
    }
    private boolean ignoreTileSource( String name ) {
        if (name.startsWith("_")) {
            return true;
        }
        return false;
    }
    public List<CustomTileDatabaseHandler> getCustomTileDatabaseHandlers() {
        return customtileHandlers;
    }
    public int size() {
        return customtileHandlers.size();
    }

    public List<CustomTileTable> getTables( boolean forceRead ) throws Exception {
        List<CustomTileTable> tables = new ArrayList<CustomTileTable>();
        for( CustomTileDatabaseHandler customtileHandler : customtileHandlers ) {
            try {
                List<CustomTileTable> customtileTables = customtileHandler.getTables(forceRead);
                for( CustomTileTable customtileTable : customtileTables ) {
                    tables.add(customtileTable);
                    customtileTablesMap.put(customtileTable, customtileHandler);
                }
            } catch (java.lang.Exception e) {
                // ignore the handler and try to g on
            }
        }
        // Collections.sort(tables, new OrderComparator());
        return tables;
    }

    public CustomTileDatabaseHandler getCustomTileDatabaseHandler( CustomTileTable customtileTable ) throws Exception {
        CustomTileDatabaseHandler CustomTileDatabaseHandler = customtileTablesMap.get(customtileTable);
        return CustomTileDatabaseHandler;
    }

    public CustomTileTable getCustomTileTableByName( String table ) throws Exception {
        List<CustomTileTable> customtileTables = getTables(false);
        for( CustomTileTable customtileTable : customtileTables ) {
             if (customtileTable.getFileNamePath().equals(table)) {
                return customtileTable;
            }
        }
        return null;
    }
    /**
     * Close  all Databases that may be open
     * <p>mbtiles 'MbtilesDatabaseHandler' database will be closed with '.close();' if active
     * <p>- 'update_bounds();' will be called beforhand
     */
    public void closeDatabases() throws Exception {
        for( CustomTileDatabaseHandler customtileHandler : customtileHandlers ) {
            customtileHandler.close();
        }
    }

}
