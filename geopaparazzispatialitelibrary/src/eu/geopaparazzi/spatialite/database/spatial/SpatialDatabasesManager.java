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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jsqlite.Exception;
import android.content.Context;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.ISpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialiteDatabaseHandler;

/**
 * The spatial database manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDatabasesManager {

    private List<ISpatialDatabaseHandler> sdbHandlers = new ArrayList<ISpatialDatabaseHandler>();
    private HashMap<SpatialVectorTable, ISpatialDatabaseHandler> vectorTablesMap = new HashMap<SpatialVectorTable, ISpatialDatabaseHandler>();
    private HashMap<SpatialRasterTable, ISpatialDatabaseHandler> rasterTablesMap = new HashMap<SpatialRasterTable, ISpatialDatabaseHandler>();

    private static SpatialDatabasesManager spatialDbManager = null;
    private static final String[] sa_extentions = new String[]{".mbtiles",".db",".sqlite",".gpkg"};
    private static final int i_extention_mbtiles = 0;
    private static final int i_extention_db = 1;
    private static final int i_extention_sqlite = 2;
    private static final int i_extention_gpkt = 3;
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
    public static String get_mbtiles_extention() {
        return sa_extentions[i_extention_mbtiles];
    }
    public static String get_db_extention() {
        return sa_extentions[i_extention_db];
    }
    public static String get_sqlite_extention() {
        return sa_extentions[i_extention_sqlite];
    }
    public static String get_gpkt_extention() {
        return sa_extentions[i_extention_gpkt];
    }
    public void init( Context context, File mapsDir ) {
        File[] list_files = mapsDir.listFiles();
        for( File this_file : list_files ) {
            // mj10777: collect spatialite.geometries and .mbtiles databases
            if (this_file.isDirectory()) {
                // mj10777: read recursive directories inside the sdcard/maps directory
                init(context, this_file);
            } else {
                for (int i=0;i<sa_extentions.length;i++)  {
                  if (this_file.getName().endsWith(sa_extentions[i])) {
                    ISpatialDatabaseHandler sdb = null;
                    if (this_file.getName().endsWith(get_mbtiles_extention())) {
                        sdb = new MbtilesDatabaseHandler(this_file.getAbsolutePath(), null);
                    } else {
                        sdb = new SpatialiteDatabaseHandler(this_file.getAbsolutePath());
                    }
                    // GPLog.androidLog(-1,"SpatialDatabasesManager["+i+"]["+sa_extentions[i]+"]: init["+this_file.getAbsolutePath()+"] ");
                    sdbHandlers.add(sdb);
                   }
                }
            }
        }
        // GPLog.androidLog(-1,"SpatialDatabasesManager init[" + mapsDir.getName() + "] size["+sdbHandlers.size()+"]");
    }
    private boolean ignoreTileSource( String name ) {
        if (name.startsWith("_")) {
            return true;
        }
        return false;
    }
    public int size() {
        return sdbHandlers.size();
    }
    public int size_raster() {
        return rasterTablesMap.size();
    }
    public int size_vector() {
        return vectorTablesMap.size();
    }
    public List<ISpatialDatabaseHandler> getSpatialDatabaseHandlers() {
        return sdbHandlers;
    }

    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        List<SpatialVectorTable> tables = new ArrayList<SpatialVectorTable>();
        for( ISpatialDatabaseHandler sdbHandler : sdbHandlers ) {
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
        for( ISpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            try {
                List<SpatialRasterTable> spatialTables = sdbHandler.getSpatialRasterTables(forceRead);
                for( SpatialRasterTable spatialTable : spatialTables ) {
                    tables.add(spatialTable);
                    rasterTablesMap.put(spatialTable, sdbHandler);
                }
            } catch (java.lang.Exception e) {
                // ignore the handler and try to g on
            }
        }
        // Collections.sort(tables, new OrderComparator());
        return tables;
    }

    public void updateStyles() throws Exception {
        Set<Entry<SpatialVectorTable, ISpatialDatabaseHandler>> entrySet = vectorTablesMap.entrySet();
        for( Entry<SpatialVectorTable, ISpatialDatabaseHandler> entry : entrySet ) {
            SpatialVectorTable key = entry.getKey();
            ISpatialDatabaseHandler value = entry.getValue();
            value.updateStyle(key.getStyle());
        }
    }

    public void updateStyle( SpatialVectorTable spatialTable ) throws Exception {
        ISpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        if (spatialDatabaseHandler != null) {
            spatialDatabaseHandler.updateStyle(spatialTable.getStyle());
        }
    }

    public ISpatialDatabaseHandler getVectorHandler( SpatialVectorTable spatialTable ) throws Exception {
        ISpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        return spatialDatabaseHandler;
    }

    public ISpatialDatabaseHandler getRasterHandler( SpatialRasterTable spatialTable ) throws Exception {
        ISpatialDatabaseHandler spatialDatabaseHandler = rasterTablesMap.get(spatialTable);
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
            if (spatialTable.getFileNamePath().equals(table)) {
                return spatialTable;
            }
        }
        return null;
    }

    public void intersectionToString( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e, double w,
            StringBuilder sb, String indentStr ) throws Exception {
        ISpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        spatialDatabaseHandler.intersectionToStringBBOX(boundsSrid, spatialTable, n, s, e, w, sb, indentStr);
    }

    public void intersectionToString( String boundsSrid, SpatialVectorTable spatialTable, double n, double e, StringBuilder sb,
            String indentStr ) throws Exception {
        ISpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        spatialDatabaseHandler.intersectionToString4Polygon(boundsSrid, spatialTable, n, e, sb, indentStr);
    }

    public void closeDatabases() throws Exception {
        for( ISpatialDatabaseHandler sdbHandler : sdbHandlers ) {
            sdbHandler.close();
        }
    }

}
