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

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.SpatialiteDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.util.comparators.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import jsqlite.Exception;

/**
 * The spatial database manager.
 *
 * <p>This manager is the entry point to all available
 * spatial databases.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDatabasesManager {

    private List<AbstractSpatialDatabaseHandler> spatialDbHandlers = new ArrayList<AbstractSpatialDatabaseHandler>();
    private HashMap<SpatialVectorTable, AbstractSpatialDatabaseHandler> vectorTablesMap = new HashMap<SpatialVectorTable, AbstractSpatialDatabaseHandler>();
    private HashMap<SpatialRasterTable, AbstractSpatialDatabaseHandler> rasterTablesMap = new HashMap<SpatialRasterTable, AbstractSpatialDatabaseHandler>();

    private static SpatialDatabasesManager spatialDbManager = null;
    private SpatialDatabasesManager() {
    }

    /**
     * @return the singleton instance.
     */
    public static SpatialDatabasesManager getInstance() {
        if (spatialDbManager == null) {
            spatialDbManager = new SpatialDatabasesManager();
        }
        return spatialDbManager;
    }

    /**
     * Reset the manager.
     *
     * TODO check with mj10777 if this should call also close first.
     */
    public static void reset() {
        spatialDbManager = null;
    }

    /**
     * Initialie the manager on a given maps folder.
     *
     * @param context  the context to use.
     * @param mapsDir the maps folder.
     * @return <code>true</code>, when recursing a nomedia folder has been hit.
     */
    public boolean init( Context context, File mapsDir ) {
        List<AbstractSpatialDatabaseHandler> tmpSpatialdbHandlers = new ArrayList<AbstractSpatialDatabaseHandler>();
        boolean b_nomedia_file = false;
        File[] filesInFolder = mapsDir.listFiles();
        for( File currentFile : filesInFolder ) {
            // nomedia logic: first check the files, if no
            // '.nomedia' found: then its directories
            if (currentFile.isFile()) {
                // mj10777: collect spatialite.geometries and .mbtiles
                // databases
                for( SpatialDataType spatialiteType : SpatialDataType.values() ) {
                    if (!spatialiteType.isSpatialiteBased()) {
                        continue;
                    }
                    String extension = spatialiteType.getExtension();
                    String name = currentFile.getName();
                    if (Utilities.isNameFromHiddenFile(name)) {
                        continue;
                    }
                    if (name.endsWith(extension)) {
                        try {
                            AbstractSpatialDatabaseHandler sdb = null;
                            if (name.endsWith(SpatialDataType.MBTILES.getExtension())) {
                                sdb = new MbtilesDatabaseHandler(currentFile.getAbsolutePath(), null);
                            } else {
                                sdb = new SpatialiteDatabaseHandler(currentFile.getAbsolutePath());
                            }
                            if (sdb.isValid()) {
                                // GPLog.androidLog(-1,"SpatialDatabasesManager["+extension+"]: init["+currentFile.getAbsolutePath()+"] ");
                                tmpSpatialdbHandlers.add(sdb);
                            }
                        } catch (IOException e) {
                            GPLog.error(this, "Error [SpatialDatabasesManager.init]", e); //$NON-NLS-1$
                        }
                    }
                    if (name.equals(ResourcesManager.NO_MEDIA)) {
                        if (!currentFile.getParentFile().toURI().equals(mapsDir.toURI())) {
                            // ignore all files of this directory if not map root
                            b_nomedia_file = true;
                            tmpSpatialdbHandlers.clear();
                            return b_nomedia_file;
                        }
                    }
                }
            }
        }
        if (!b_nomedia_file) {
            for( int i = 0; i < tmpSpatialdbHandlers.size(); i++ ) {
                spatialDbHandlers.add(tmpSpatialdbHandlers.get(i));
            }
        }
        tmpSpatialdbHandlers.clear();
        for( File this_file : filesInFolder ) {
            if (this_file.isDirectory()) {
                // mj10777: read recursive directories inside the
                // sdcard/maps directory
                init(context, this_file);
            }
        }
        return b_nomedia_file;
    }

    /**
     * Get all available database count.
     *
     * @return the number of available databases.
     */
    public int getCount() {
        return spatialDbHandlers.size();
    }

    /**
     * Get the count of raster dbs.
     *
     * @return the number of available raster dbs.
     */
    public int getRasterDbCount() {
        return rasterTablesMap.size();
    }

    /**
     * Get the count of vector dbs.
     *
     * @return the number of available vector dbs.
     */
    public int getVectorDbCount() {
        return vectorTablesMap.size();
    }

    /**
     * Get the list of available {@link eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler}.
     *
     * @return the list of spatial db handlers.
     */
    public List<AbstractSpatialDatabaseHandler> getSpatialDatabaseHandlers() {
        return spatialDbHandlers;
    }

    /**
     * Get the list of all available spatial vector tables.
     *
     * @param forceRead if <code>true</code>, a re-reading of the dbs is forced.
     * @return the list of spatial vector tables.
     * @throws Exception  if something goes wrong.
     */
    public synchronized List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        List<SpatialVectorTable> tables = new ArrayList<SpatialVectorTable>();
        for( AbstractSpatialDatabaseHandler sdbHandler : spatialDbHandlers ) {
            List<SpatialVectorTable> spatialTables = sdbHandler.getSpatialVectorTables(forceRead);
            if (sdbHandler.isValid()) {
                for( SpatialVectorTable spatialTable : spatialTables ) {
                    tables.add(spatialTable);
                    vectorTablesMap.put(spatialTable, sdbHandler);
                }
            }
        }
        Collections.sort(tables, new OrderComparator());
        // set proper order index across tables
        for( int i = 0; i < tables.size(); i++ ) {
            tables.get(i).getStyle().order = i;
        }
        return tables;
    }

    /**
     * Get the list of all available spatial raster tables.
     *
     * @param forceRead if <code>true</code>, a re-reading of the dbs is forced.
     * @return the list of spatial raster tables.
     * @throws Exception  if something goes wrong.
     */
    public synchronized List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        List<SpatialRasterTable> tables = new ArrayList<SpatialRasterTable>();
        for( AbstractSpatialDatabaseHandler sdbHandler : spatialDbHandlers ) {
            try {
                List<SpatialRasterTable> spatialTables = sdbHandler.getSpatialRasterTables(forceRead);
                if (sdbHandler.isValid()) {
                    for( SpatialRasterTable spatialTable : spatialTables ) {
                        tables.add(spatialTable);
                        rasterTablesMap.put(spatialTable, sdbHandler);
                    }
                }
            } catch (java.lang.Exception e) {
                // ignore the handler and try to go on
                GPLog.error(this, "Error [SpatialDatabasesManager.getSpatialRasterTables]", e); //$NON-NLS-1$
            }
        }
        return tables;
    }

    /**
     * Update all styles in the dbs with the current layers values.
     *
     * @throws Exception  if something goes wrong.
     */
    public void updateStyles() throws Exception {
        for( Map.Entry<SpatialVectorTable, AbstractSpatialDatabaseHandler> entry : vectorTablesMap.entrySet() ) {
            SpatialVectorTable key = entry.getKey();
            AbstractSpatialDatabaseHandler spatialiteDatabaseHandler = entry.getValue();
            if (spatialiteDatabaseHandler instanceof SpatialiteDatabaseHandler) {
                ((SpatialiteDatabaseHandler) spatialiteDatabaseHandler).updateStyle(key.getStyle());
            }
        }
    }

    /**
     * Update the style in the dbs with the given layer values.
     *
     * @param spatialTable the current table to update.
     * @throws Exception  if something goes wrong.
     */
    public void updateStyle( SpatialVectorTable spatialTable ) throws Exception {
        AbstractSpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        if (spatialDatabaseHandler instanceof SpatialiteDatabaseHandler) {
            ((SpatialiteDatabaseHandler) spatialDatabaseHandler).updateStyle(spatialTable.getStyle());
        }
    }

    /**
     * Get the {@link eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler} that contains a given vector table.
     *
     * @param spatialTable the vector table.
     * @return the db handler.
     * @throws Exception  if something goes wrong.
     */
    public AbstractSpatialDatabaseHandler getVectorHandler( SpatialVectorTable spatialTable ) throws Exception {
        AbstractSpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        return spatialDatabaseHandler;
    }

    /**
     * Get the {@link eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler} that contains a given raster table.
     *
     * @param spatialTable the raster table.
     * @return the db handler.
     * @throws Exception  if something goes wrong.
     */
    public AbstractSpatialDatabaseHandler getRasterHandler( SpatialRasterTable spatialTable ) throws Exception {
        AbstractSpatialDatabaseHandler spatialDatabaseHandler = rasterTablesMap.get(spatialTable);
        return spatialDatabaseHandler;
    }

    /**
     * Get a {@link SpatialVectorTable} by its name.
     *
     * @param tableName the table name.
     * @return the vector table or <code>null</code>.
     * @throws Exception  if something goes wrong.
     */
    public SpatialVectorTable getVectorTableByName( String tableName ) throws Exception {
        List<SpatialVectorTable> spatialTables = getSpatialVectorTables(false);
        for( SpatialVectorTable spatialTable : spatialTables ) {
            if (spatialTable.getUniqueNameBasedOnDbFilePath().equals(tableName)) {
                return spatialTable;
            }
        }
        return null;
    }

    /**
     * Get a {@link SpatialRasterTable} by its name.
     *
     * @param tableName the table name.
     * @return the raster table or <code>null</code>.
     * @throws Exception  if something goes wrong.
     */
    public SpatialRasterTable getRasterTableByName( String tableName,String tableTitle  ) throws Exception {
        List<SpatialRasterTable> spatialTables = getSpatialRasterTables(false);
        for( SpatialRasterTable spatialTable : spatialTables ) {
            if ((spatialTable.getDatabasePath().equals(tableName)) && (spatialTable.getTitle().equals(tableTitle))) {
                return spatialTable;
            }
        }
        return null;
    }

    /**
     * Performs an intersection query on a vector table and returns a string info version of the result.
     *
     * @param boundsSrid the srid of the bounds supplied.
     * @param spatialTable the vector table to query.
     * @param n north bound.
     * @param s south bound.
     * @param e east bound.
     * @param w west bound.
     * @param resultStringBuilder the builder of the result.
     * @param indentStr the indenting to use for formatting.
     * @throws Exception  if something goes wrong.
     */
    public void intersectionToString( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e, double w,
            StringBuilder resultStringBuilder, String indentStr ) throws Exception {
        AbstractSpatialDatabaseHandler spatialDatabaseHandler = vectorTablesMap.get(spatialTable);
        if (spatialDatabaseHandler instanceof SpatialiteDatabaseHandler) {
            ((SpatialiteDatabaseHandler) spatialDatabaseHandler).intersectionToStringBBOX(boundsSrid, spatialTable, n, s, e, w,
                    resultStringBuilder, indentStr);
        }
    }

    /**
     * Close all available databases.
     *
     * @throws Exception  if something goes wrong.
     */
    public void closeDatabases() throws Exception {
        for( AbstractSpatialDatabaseHandler sdbHandler : spatialDbHandlers ) {
            sdbHandler.close();
        }
    }

}
