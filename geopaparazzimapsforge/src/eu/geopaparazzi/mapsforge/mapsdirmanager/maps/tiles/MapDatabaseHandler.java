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
package eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
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
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

/**
 * An utility class to handle a map database.
 *
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to work with map databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class MapDatabaseHandler extends AbstractSpatialDatabaseHandler {
    private List<MapTable> mapTableList;
    private FileOpenResult fileOpenResult;
    private MapDatabase mapDatabase = null;
    private MapFileInfo mapFileInfo = null;

    /**
     * Constructor.
     * 
     * @param dbPath the path to the database to handle.
     * @throws IOException  if something goes wrong.
     */
    public MapDatabaseHandler( String dbPath ) throws IOException {
        super(dbPath);
        try {
            mapDatabase = new MapDatabase();
            this.fileOpenResult = mapDatabase.openFile(databaseFile);
            if (!fileOpenResult.isSuccess()) {
                throw new IOException("Could not open the map database: " + databasePath);
            }
            mapFileInfo = mapDatabase.getMapFileInfo();
            tableName = mapFileInfo.comment;
            databaseFileName = databaseFile.getName();
            if ((tableName == null) || (tableName.length() == 0)) {
                tableName = this.databaseFile.getName().substring(0, this.databaseFile.getName().lastIndexOf("."));
            }
            boundsWest = (double) (mapFileInfo.boundingBox.getMinLongitude());
            boundsSouth = (double) (mapFileInfo.boundingBox.getMinLatitude());
            boundsEast = (double) (mapFileInfo.boundingBox.getMaxLongitude());
            boundsNorth = (double) (mapFileInfo.boundingBox.getMaxLatitude());
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
        } catch (java.lang.Exception e) {
            GPLog.error(this, "MapDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
        }
    }

    @Override
    public void open() {
        // TODO we should move opening here
    }

    /**
     * Get the available tables.
     * 
     * < p>Currently this is a list with a single table.
     * <p>[GeneralQueriesPreparer] Documentation of vector_key /  vector_value   
     * <ol>
     * <li>vector_key: 6 Fields will be returned with the following structure[They may not be empty, otherwise lenght of split will not return the correct amount]</li>
     * <li>0 table_name: tableName/li>
     * <li>1: geometry_column - not used '0'</li>
     * <li>2: layer_type - SpatialDataType.MAP.getTypeName()</li>
     * <li>3: ROWID - short discription[databaseFileNameNoExtension]</li>
     * <li>4: view_read_only - long discription [tableName]</li>
     * </ol>
     * <li>vector_data: Seperator: ';' 7 values minimum [more must be made known to parse_vector_key_value()]</li>
     * <li>0: geometry_type - this.minZoom</li>
     * <li>1: coord_dimension - this.maxZoom</li>
     * <li>2: srid - 4326</li>
     * <li>3: spatial_index_enabled - defaultZoom</li>
     * <li>4: rows - not used '0'
     * <li>5: extent_min/max - Seperator ',' - 4 values [mbtiles,map,mapurl: 7 values : centerX,centerY,defaultZoom]
     * <li>5.1:extent_min_x - boundsWest</li>
     * <li>5.2:extent_min_y - boundsSouth</li>
     * <li>5.3:extent_max_x - boundsEast</li>
     * <li>5.4:extent_max_y - boundsNorth</li>
     * <li>5.5: centerX - can be user defined</li>
     * <li>5.6: centerY - can be user defined</li>
     * <li>5.7: defaultZoom - can be user defined</li>
     * <li>6:last_verified - tileQuery as '?,?,?'</li>
     * <li>7:getDatabasePath()</li>
     * <li>8-?:not used</li>
     * </ol>
     * <p> vector_key/data documetation : 20150718
     * 
     * @param forceRead force a re-reading of the resources.
     * @return the list of available tables.
     * @throws Exception  if something goes wrong.
     */
    public List<MapTable> getTables( boolean forceRead ) throws Exception {
        if (mapTableList == null || forceRead) {
            mapTableList = new ArrayList<MapTable>();
           String layerType = SpatialDataType.MAP.getTypeName();
           String vector_key=tableName+";0;"+layerType+";"+databaseFileNameNoExtension+";"+tableName+"";
           String s_bounds=this.boundsWest + "," + this.boundsSouth + "," + this.boundsEast + "," + this.boundsNorth+ "," + this.centerX+ "," + this.centerY+","+defaultZoom;
           String vector_value=this.minZoom+";"+this.maxZoom+";4326;0;0;"+s_bounds+";?,?,?;"+getDatabasePath();
           MapTable table = new MapTable(null,vector_key,vector_value);
           if ((table != null) && (table.isValid()))
            mapTableList.add(table);
        }
        return mapTableList;
    }

    public void close() throws Exception {
        if (mapDatabase != null) {
            mapDatabase.closeFile();
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public float[] getTableBounds( AbstractSpatialTable spatialTable ) throws Exception {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new float[]{n, s, e, w};
    }

    @Override
    public byte[] getRasterTile( String query ) {
        throw new RuntimeException("should not be called.");
    }
}
