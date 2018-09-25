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
package eu.geopaparazzi.mapsforge.databasehandlers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.databasehandlers.core.MapTable;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;
import jsqlite.Exception;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

/**
 * An utility class to handle a map database.
 * <p/>
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to work with map databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class MapDatabaseHandler extends AbstractSpatialDatabaseHandler {
    private List<MapTable> mapTableList;
    private FileOpenResult fileOpenResult;
    private MapDatabase mapDatabase = null;
    private MapFileInfo mapFileInfo = null;
    private volatile boolean isOpen = false;

    /**
     * Constructor.
     *
     * @param dbPath the path to the database to handle.
     * @throws IOException if something goes wrong.
     */
    private MapDatabaseHandler(String dbPath) throws IOException {
        super(dbPath);
        open();
    }

    /**
     * Create a handler for the given file.
     *
     * @param file the file.
     * @return the handler or null if the file didn't fit the .
     */
    public static MapDatabaseHandler getHandlerForFile(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            String name = file.getName();
            if (Utilities.isNameFromHiddenFile(name)) {
                return null;
            }
            if (name.endsWith(ESpatialDataSources.MAP.getExtension())) {
                MapDatabaseHandler map = new MapDatabaseHandler(file.getAbsolutePath());
                return map;
            }
        }
        return null;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void open() {
        try {
            mapDatabase = new MapDatabase();
            this.fileOpenResult = mapDatabase.openFile(databaseFile);
            if (!fileOpenResult.isSuccess()) {
                throw new IOException("Could not open the map database: " + databasePath);
            }
            mapFileInfo = mapDatabase.getMapFileInfo();
            databaseFileName = databaseFile.getName();
            try {
                tableName = this.databaseFile.getName().substring(0, this.databaseFile.getName().lastIndexOf("."));
            } catch (java.lang.Exception ex) {
                // try comment tag
                tableName = mapFileInfo.comment;
            }
            boundsWest = mapFileInfo.boundingBox.getMinLongitude();
            boundsSouth = mapFileInfo.boundingBox.getMinLatitude();
            boundsEast = mapFileInfo.boundingBox.getMaxLongitude();
            boundsNorth = mapFileInfo.boundingBox.getMaxLatitude();
            GeoPoint startPosition = mapFileInfo.startPosition;
            // long_description[california bounds[-125.8935,32.48171,-114.1291,42.01618]
            // center[-120.0113,37.248945,14][-121.4944,38.58157]]
            if (startPosition == null) { // true center of map
                centerX = mapFileInfo.boundingBox.getCenterPoint().getLongitude();
                centerY = mapFileInfo.boundingBox.getCenterPoint().getLatitude();
            } else { // user-defined center of map
                centerY = startPosition.getLatitude();
                centerX = startPosition.getLongitude();
            }
            Byte startZoomLevel = mapFileInfo.startZoomLevel;
            // Byte startZoomLevel = getMinZoomlevel(map_Database);
            if (startZoomLevel != null) {
                defaultZoom = startZoomLevel;
            } else {
                defaultZoom = 14;
            }
            minZoom = 0;
            maxZoom = 22;

            isOpen = true;
        } catch (java.lang.Exception e) {
            GPLog.error(this, "MapDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
        }
    }

    /**
     * Get the available tables.
     * <p/>
     * <p>Currently this is a list with a single table.
     *
     * @param forceRead force a re-reading of the resources.
     * @return the list of available tables.
     * @throws Exception if something goes wrong.
     */
    public List<MapTable> getTables(boolean forceRead) throws Exception {
        if (mapTableList == null || forceRead) {
            mapTableList = new ArrayList<>();
            double[] d_bounds = {boundsWest, boundsSouth, boundsEast, boundsNorth};
            MapTable table = new MapTable(databasePath, tableName, LibraryConstants.SRID_MERCATOR_3857, minZoom, maxZoom,
                    centerX, centerY, "?,?,?", d_bounds);
            table.setDefaultZoom(defaultZoom);
            mapTableList.add(table);
        }
        return mapTableList;
    }

    public void close() throws Exception {
        isOpen = false;
        if (mapDatabase != null) {
            mapDatabase.closeFile();
        }

    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<SpatialVectorTable> getSpatialVectorTables(boolean forceRead) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<SpatialRasterTable> getSpatialRasterTables(boolean forceRead) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public float[] getTableBounds(AbstractSpatialTable spatialTable) throws Exception {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new float[]{n, s, e, w};
    }

    @Override
    public byte[] getRasterTile(String query) {
        throw new RuntimeException("should not be called.");
    }
}
