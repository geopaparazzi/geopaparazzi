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

import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.mapsforge.databasehandlers.core.CustomTileDownloader;
import eu.geopaparazzi.mapsforge.databasehandlers.core.CustomTileTable;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import jsqlite.Exception;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

/**
 * An utility class to handle an custom tiles database.
 * <p/>
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to work with custom tiles databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class CustomTileDatabaseHandler extends AbstractSpatialDatabaseHandler implements AutoCloseable {
    private List<CustomTileTable> customtileTableList = null;

    private CustomTileDownloader customTileDownloader = null;

    /**
     * Constructor.
     *
     * @param dbPath the path to the source to handle.
     * @throws IOException if something goes wrong.
     */
    private CustomTileDatabaseHandler(String dbPath) throws IOException {
        super(dbPath);
        open();
    }

    /**
     * Create a handler for the given file.
     *
     * @param file the file.
     * @return the handler or null if the file didn't fit the .
     */
    public static CustomTileDatabaseHandler getHandlerForFile(File file) throws IOException {
        if (file.exists() && file.isFile()) {
            String name = file.getName();
            if (Utilities.isNameFromHiddenFile(name)) {
                return null;
            }
            if (name.endsWith(SpatialDataType.MAPURL.getExtension())) {
                CustomTileDatabaseHandler map = new CustomTileDatabaseHandler(file.getAbsolutePath());
                return map;
            }
        }
        return null;
    }

    /**
     * Getter for the {@link CustomTileDownloader}.
     *
     * @return the tile downloader.
     */
    public CustomTileDownloader getCustomTileDownloader() {
        return customTileDownloader;
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
    public List<CustomTileTable> getTables(boolean forceRead) throws Exception {
        if (customtileTableList == null || forceRead) {
            customtileTableList = new ArrayList<>();
            double[] d_bounds = {boundsWest, boundsSouth, boundsEast, boundsNorth};
            CustomTileTable table = new CustomTileTable(databasePath, tableName, LibraryConstants.SRID_MERCATOR_3857, minZoom, maxZoom, centerX, centerY,
                    "?,?,?", d_bounds);
            table.setDefaultZoom(defaultZoom);
            customtileTableList.add(table);
        }
        return customtileTableList;
    }

    public void close() throws Exception {
        if (customTileDownloader != null) {
            customTileDownloader.cleanup();
        }
    }

    public byte[] getRasterTile(String query) {
        throw new RuntimeException("should not be called");
    }

    @Override
    public void open() throws IOException {
        customTileDownloader = new CustomTileDownloader(databaseFile);
        boundsWest = customTileDownloader.getMinLongitude();
        boundsSouth = customTileDownloader.getMinLatitude();
        boundsEast = customTileDownloader.getMaxLongitude();
        boundsNorth = customTileDownloader.getMaxLatitude();
        centerX = customTileDownloader.getCenterX();
        centerY = customTileDownloader.getCenterY();
        maxZoom = customTileDownloader.getMaxZoom();
        minZoom = customTileDownloader.getMinZoom();
        defaultZoom = customTileDownloader.getDefaultZoom();
        tableName = customTileDownloader.getName();
        if ((tableName == null) || (tableName.length() == 0)) {
            tableName = this.databaseFile.getName().substring(0, this.databaseFile.getName().lastIndexOf("."));
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
}
