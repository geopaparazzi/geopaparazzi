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
package eu.geopaparazzi.spatialite.database.spatial;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jsqlite.Exception;

import eu.geopaparazzi.spatialite.database.spatial.core.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import android.content.Context;

/**
 * The spatial database manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDatabasesManager {

    private List<SpatialDatabaseHandler> sdbHandlers = new ArrayList<SpatialDatabaseHandler>();
    private HashMap<SpatialVectorTable, SpatialDatabaseHandler> vectorTablesMap = new HashMap<SpatialVectorTable, SpatialDatabaseHandler>();
    private HashMap<SpatialRasterTable, SpatialDatabaseHandler> rasterTablesMap = new HashMap<SpatialRasterTable, SpatialDatabaseHandler>();

    private static SpatialDatabasesManager spatialDbManager = null;
    private SpatialDatabasesManager() {
    }

    public static SpatialDatabasesManager getInstance() {
        if (spatialDbManager == null) {
            spatialDbManager = new SpatialDatabasesManager();
        }
        return spatialDbManager;
    }

    public static void reset() {
        spatialDbManager = null;
    }

    public void init( Context context, File mapsDir ) {
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

    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        List<SpatialVectorTable> tables = new ArrayList<SpatialVectorTable>();
        for( SpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            List<SpatialVectorTable> spatialTables = sdbHandler.getSpatialVectorTables(forceRead);
            for( SpatialVectorTable spatialTable : spatialTables ) {
                tables.add(spatialTable);
                vectorTablesMap.put(spatialTable, sdbHandler);
            }
        }

        Collections.sort(tables, new OrderComparator());
        // set proper order index across tables
        for( int i = 0; i < tables.size(); i++ ) {
            tables.get(i).getStyle().order = i;
        }
        return tables;
    }

    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        List<SpatialRasterTable> tables = new ArrayList<SpatialRasterTable>();
        for( SpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            List<SpatialRasterTable> spatialTables = sdbHandler.getSpatialRasterTables(forceRead);
            for( SpatialRasterTable spatialTable : spatialTables ) {
                tables.add(spatialTable);
                rasterTablesMap.put(spatialTable, sdbHandler);
            }
        }
        // Collections.sort(tables, new OrderComparator());
        return tables;
    }

    public void updateStyles() throws Exception {
        Set<Entry<SpatialVectorTable, SpatialDatabaseHandler>> entrySet = vectorTablesMap.entrySet();
        for( Entry<SpatialVectorTable, SpatialDatabaseHandler> entry : entrySet ) {
            SpatialVectorTable key = entry.getKey();
            SpatialDatabaseHandler value = entry.getValue();
            value.updateStyle(key.getStyle());
        }
    }

    public void updateStyle( SpatialVectorTable spatialTable ) throws Exception {
        SpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        if (spatialDatabaseHandler != null) {
            spatialDatabaseHandler.updateStyle(spatialTable.getStyle());
        }
    }

    public SpatialDatabaseHandler getVectorHandler( SpatialVectorTable spatialTable ) throws Exception {
        SpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        return spatialDatabaseHandler;
    }

    public SpatialDatabaseHandler getRasterHandler( SpatialRasterTable spatialTable ) throws Exception {
        SpatialDatabaseHandler spatialDatabaseHandler = rasterTablesMap.get(spatialTable);
        return spatialDatabaseHandler;
    }

    public SpatialVectorTable getVectorTableByName( String table ) throws Exception {
        List<SpatialVectorTable> spatialTables = getSpatialVectorTables(false);
        for( SpatialVectorTable spatialTable : spatialTables ) {
            if (spatialTable.getName().equals(table)) {
                return spatialTable;
            }
        }
        return null;
    }

    public SpatialRasterTable getRasterTableByName( String table ) throws Exception {
        List<SpatialRasterTable> spatialTables = getSpatialRasterTables(false);
        for( SpatialRasterTable spatialTable : spatialTables ) {
            if (spatialTable.getTableName().equals(table)) {
                return spatialTable;
            }
        }
        return null;
    }

    public void intersectionToString( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e, double w,
            StringBuilder sb, String indentStr ) throws Exception {
        SpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        spatialDatabaseHandler.intersectionToStringBBOX(boundsSrid, spatialTable, n, s, e, w, sb, indentStr);
    }

    public void intersectionToString( String boundsSrid, SpatialVectorTable spatialTable, double n, double e, StringBuilder sb,
            String indentStr ) throws Exception {
        SpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        spatialDatabaseHandler.intersectionToString4Polygon(boundsSrid, spatialTable, n, e, sb, indentStr);
    }

    public void closeDatabases() throws Exception {
        for( SpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            sdbHandler.close();
        }
    }

}
