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
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileDatabaseHandler;
import eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles.CustomTileTable;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

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
    private CustomTileDatabasesManager() {
    }

    /**
     * @return the singleton instance.
     */
    public static CustomTileDatabasesManager getInstance() {
        if (customtileDbManager == null) {
            customtileDbManager = new CustomTileDatabasesManager();
        }
        return customtileDbManager;
    }

    /**
     * Reset the db manager.
     */
    public static void reset() {
        customtileDbManager = null;
    }

    /**
     * Initialize the {@link CustomTileDatabasesManager}.
     * 
     * @param context  the context to use.
     * @param mapsDir the folder to browse for custom tile sources. 
     * @return <code>true</code> if the current folder is marked as nomedia.
     */
    public boolean init( Context context, File mapsDir ) {
        File[] filesList = mapsDir.listFiles();
        List<CustomTileDatabaseHandler> customtile_Handlers = new ArrayList<CustomTileDatabaseHandler>();
        boolean b_nomedia_file = false;
        for( File currentFile : filesList ) {
            // nomedia logic: first check the files, if no
            // '.nomedia' found: then its directories
            if (currentFile.isFile()) { // mj10777: collect .mapurl databases
                String name = currentFile.getName();
                if (Utilities.isNameFromHiddenFile(name)) {
                    continue;
                }
                if (name.endsWith(SpatialDataType.MAPURL.getExtension())) {
                    try {
                        CustomTileDatabaseHandler map = new CustomTileDatabaseHandler(currentFile.getAbsolutePath(),
                                ResourcesManager.getInstance(context).getMapsDir().getAbsolutePath());
                        customtile_Handlers.add(map);
                    } catch (java.lang.Exception e) {
                        GPLog.error(this, "Error reading a Custom tile source.", e); //$NON-NLS-1$
                    }
                }
                if (name.equals(ResourcesManager.NO_MEDIA)) {
                    if (!currentFile.getParentFile().toURI().equals(mapsDir.toURI())) {
                        // ignore all files of this directory if not maps root
                        b_nomedia_file = true;
                        customtile_Handlers.clear();
                        return b_nomedia_file;
                    }
                }
            }
        }
        if (!b_nomedia_file) {
            for( int i = 0; i < customtile_Handlers.size(); i++ ) {
                customtileHandlers.add(customtile_Handlers.get(i));
            }
        }
        customtile_Handlers.clear();
        for( File this_file : filesList ) {
            if (this_file.isDirectory()) {
                // mj10777: read recursive directories inside the
                // sdcard/maps directory
                init(context, this_file);
            }
        }
        // GPLog.androidLog(-1,"CustomTileDatabasesManager init[" + mapsDir.getName() +
        // "] size["+customtileHandlers.size()+"]");
        return b_nomedia_file;
    }

    /**
     * @return the list of {@link CustomTileDatabaseHandler}s.
     */
    public List<CustomTileDatabaseHandler> getCustomTileDatabaseHandlers() {
        return customtileHandlers;
    }

    /**
     * @return the number of available {@link CustomTileDatabaseHandler}s.
     */
    public int size() {
        return customtileHandlers.size();
    }

    /**
     * Get the available tables.
     * 
     * @param forceRead force a re-reading of the resources.
     * @return the list of available tables.
     * @throws Exception  if something goes wrong.
     */
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
                GPLog.error(this, "Error", e); //$NON-NLS-1$
            }
        }
        // Collections.sort(tables, new OrderComparator());
        return tables;
    }

    /**
     * Get the {@link CustomTileDatabaseHandler} for a given {@link CustomTileTable}.
     * 
     * @param customtileTable the table to use.
     * @return the handler.
     * @throws Exception  if something goes wrong.
     */
    public CustomTileDatabaseHandler getCustomTileDatabaseHandler( CustomTileTable customtileTable ) throws Exception {
        CustomTileDatabaseHandler customTileDatabaseHandler = customtileTablesMap.get(customtileTable);
        return customTileDatabaseHandler;
    }

    /**
     * Get a {@link CustomTileTable} for the name.
     * 
     * @param table the table name.
     * @return the table object.
     * @throws Exception  if something goes wrong.
     */
    public CustomTileTable getCustomTileTableByName( String table ) throws Exception {
        List<CustomTileTable> customtileTables = getTables(false);
        for( CustomTileTable customtileTable : customtileTables ) {
            if (customtileTable.getDatabasePath().equals(table)) {
                return customtileTable;
            }
        }
        return null;
    }

    /**
     * Close  all Databases that may be open
     * 
     * <p>mbtiles 'MbtilesDatabaseHandler' database will be closed with '.close();' if active
     * <p>- 'update_bounds();' will be called beforhand
     * 
     * @throws Exception  if something goes wrong.
     */
    public void closeDatabases() throws Exception {
        for( CustomTileDatabaseHandler customtileHandler : customtileHandlers ) {
            customtileHandler.close();
        }
    }

}
