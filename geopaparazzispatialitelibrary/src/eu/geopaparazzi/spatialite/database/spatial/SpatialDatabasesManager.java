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

import eu.geopaparazzi.spatialite.database.spatial.core.ISpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.core.ISpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialiteDatabaseHandler;
import android.content.Context;
import android.util.Log;

/**
 * The spatial database manager.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDatabasesManager {

    public static String s_log_tag="";
    public static int i_log_debug=0;
    // -----------------------------------------------
    /**
    * Function for global logging
    * - s_log_tag value will be shown in as TAG in logcat [default Constants.GEOPAPARAZZI;]
    *  -- change values in GeoPaparazziActivity.onCreate((
    *  use 'import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;' where needed
    * @param i_parm -1=use global value ; 0=no message ; 1=info ; 2=warning ;3=error ; 4=debug
    * @param s_message message text to be shown in logcat
    */
    public static void app_log(int i_parm,String s_message)
    { // if < 0: use global setting
     if (i_parm < 0)
      i_parm = i_log_debug;
     if (s_log_tag == "")
      s_log_tag="mj10777";
     switch (i_parm)
     {
      case 0:
       // ignore
      break;
      case 2:
       Log.w(s_log_tag,s_message);
      break;
      case 3:
       Log.e(s_log_tag,s_message);
      break;
      case 4:
       Log.d(s_log_tag,s_message);
      break;
      case 1:
      default:
       Log.i(s_log_tag,s_message);
      break;
     }
    }
    private List<ISpatialDatabaseHandler> sdbHandlers = new ArrayList<ISpatialDatabaseHandler>();
    private HashMap<SpatialVectorTable, ISpatialDatabaseHandler> vectorTablesMap = new HashMap<SpatialVectorTable, ISpatialDatabaseHandler>();
    private HashMap<SpatialRasterTable, ISpatialDatabaseHandler> rasterTablesMap = new HashMap<SpatialRasterTable, ISpatialDatabaseHandler>();

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

    public void init(Context context,File mapsDir )
    {
      File[] list_files = mapsDir.listFiles();
      for( File this_file : list_files )
      { // mj10777: collect spatialite.geometries and .mbtiles databases
       if (this_file.isDirectory())
       {  // mj10777: read recursive directories inside the sdcard/maps directory
        init(context,this_file);
       }
       else
       {
        if (this_file.getName().endsWith(".sqlite") || this_file.getName().endsWith(".db") || this_file.getName().endsWith(".mbtiles"))
        { // mj10777: added '.db' - should cause no conflicts inside  sdcard/maps directory
         ISpatialDatabaseHandler sdb = null;
         if (this_file.getName().endsWith(".mbtiles"))
         {
          sdb = new MbtilesDatabaseHandler(this_file.getAbsolutePath(),null);
          // SpatialDatabasesManager.app_log(i_debug,"MbtilesDatabase: ["+this_file.getAbsolutePath()+"]");
         }
        else
        {
         sdb = new SpatialiteDatabaseHandler(this_file.getAbsolutePath());
         // SpatialDatabasesManager.app_log(i_debug,"SpatialiteDatabase: ["+this_file.getAbsolutePath()+"]");
        }
        sdbHandlers.add(sdb);
       }
      }
     }
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
            if (spatialTable.getTableName().equals(table)) {
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
