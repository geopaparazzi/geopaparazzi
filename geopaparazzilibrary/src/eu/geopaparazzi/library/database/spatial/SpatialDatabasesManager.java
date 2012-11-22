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
package eu.geopaparazzi.library.database.spatial;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jsqlite.Exception;

import eu.geopaparazzi.library.database.spatial.core.OrderComparator;
import eu.geopaparazzi.library.database.spatial.core.SpatialDatabaseHandler;
import eu.geopaparazzi.library.database.spatial.core.SpatialTable;
import eu.geopaparazzi.library.util.ResourcesManager;
import android.content.Context;

/**
 * The spatial database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDatabasesManager {

    private List<SpatialDatabaseHandler> sdbHandlers = new ArrayList<SpatialDatabaseHandler>();
    private HashMap<SpatialTable, SpatialDatabaseHandler> tablesMap = new HashMap<SpatialTable, SpatialDatabaseHandler>();

    private static SpatialDatabasesManager spatialDbManager = null;
    private SpatialDatabasesManager() {
    }

    public static SpatialDatabasesManager getInstance() {
        if (spatialDbManager == null) {
            spatialDbManager = new SpatialDatabasesManager();
        }
        return spatialDbManager;
    }

    public void init( Context context ) {
        File mapsDir = ResourcesManager.getInstance(context).getMapsDir();
        File[] sqliteFiles = mapsDir.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String filename ) {
                return filename.endsWith(".sqlite");
            }
        });

        for( File sqliteFile : sqliteFiles ) {
            SpatialDatabaseHandler sdb = new SpatialDatabaseHandler(sqliteFile.getAbsolutePath());
            sdbHandlers.add(sdb);
        }
    }

    public List<SpatialDatabaseHandler> getSpatialDatabaseHandlers() {
        return sdbHandlers;
    }

    public List<SpatialTable> getSpatialTables( boolean forceRead ) throws Exception {
        List<SpatialTable> tables = new ArrayList<SpatialTable>();
        for( SpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            List<SpatialTable> spatialTables = sdbHandler.getSpatialTables(forceRead);
            for( SpatialTable spatialTable : spatialTables ) {
                tables.add(spatialTable);
                tablesMap.put(spatialTable, sdbHandler);
            }
        }

        Collections.sort(tables, new OrderComparator());
        // set proper order index across tables
        for( int i = 0; i < tables.size(); i++ ) {
            tables.get(i).style.order = i;
        }
        return tables;
    }

    public void updateStyles() throws Exception {
        Set<Entry<SpatialTable, SpatialDatabaseHandler>> entrySet = tablesMap.entrySet();
        for( Entry<SpatialTable, SpatialDatabaseHandler> entry : entrySet ) {
            SpatialTable key = entry.getKey();
            SpatialDatabaseHandler value = entry.getValue();
            value.updateStyle(key.style);
        }
    }

    public void updateStyle( SpatialTable spatialTable ) throws Exception {
        SpatialDatabaseHandler spatialDatabaseHandler = tablesMap.get(spatialTable);
        if (spatialDatabaseHandler != null) {
            spatialDatabaseHandler.updateStyle(spatialTable.style);
        }
    }

    public void closeDatabases() throws Exception {
        for( SpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            sdbHandler.close();
        }
    }

}
