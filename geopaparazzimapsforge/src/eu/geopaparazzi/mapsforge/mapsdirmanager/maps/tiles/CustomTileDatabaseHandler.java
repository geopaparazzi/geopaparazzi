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

import eu.geopaparazzi.spatialite.database.spatial.core.tables.AbstractSpatialTable;
import jsqlite.Exception;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.SpatialDataType;

/**
 * An utility class to handle an custom tiles database.
 *
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to work with custom tiles databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class CustomTileDatabaseHandler extends AbstractSpatialDatabaseHandler {
    private List<CustomTileTable> customtileTableList = null;

    private CustomTileDownloader customTileDownloader = null;

    /**
     * Constructor.
     * 
     * @param dbPath the path to the source to handle.
     * @param parentPath the parent path to use.
     * @throws IOException  if something goes wrong.
     */
    public CustomTileDatabaseHandler( String dbPath, String parentPath ) throws IOException {
        super(dbPath);
        try {
            customTileDownloader = new CustomTileDownloader(databaseFile, parentPath);
            boundsWest = customTileDownloader.getMinLongitude();
            boundsSouth = customTileDownloader.getMinLatitude();
            boundsEast = customTileDownloader.getMaxLongitude();
            boundsNorth = customTileDownloader.getMaxLatitude();
            centerX = customTileDownloader.getCenterX();
            centerY = customTileDownloader.getCenterY();
            maxZoom = customTileDownloader.getMaxZoom();
            minZoom = customTileDownloader.getMinZoom();
            defaultZoom = customTileDownloader.getDefaultZoom();
            // mbtiles_db = customTileDownloader.getmbtiles();
            tableName = customTileDownloader.getName();
            if ((tableName == null) || (tableName.length() == 0)) {
                tableName = this.databaseFile.getName().substring(0, this.databaseFile.getName().lastIndexOf("."));
            }
        } catch (java.lang.Exception e) {
            GPLog.error(this, "CustomTileDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
        }
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
     * 
     * <p>Currently this is a list with a single table.
     * <p>[GeneralQueriesPreparer] Documentation of vector_key /  vector_value   
     * <ol>
     * <li>vector_key: 6 Fields will be returned with the following structure[They may not be empty, otherwise lenght of split will not return the correct amount]</li>
     * <li>0 table_name: tableName/li>
     * <li>1: geometry_column - not used '0'</li>
     * <li>2: layer_type - SpatialDataType.MAPURL.getTypeName()</li>
     * <li>3: ROWID - short description[databaseFileNameNoExtension]</li>
     * <li>4: view_read_only - long description [tableName]</li>
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
     * <p>vector_key and vector_value
     * <ol>
     * <li>vector_key[map]='databaseFileNameNoExtension;0;Mapurl;title;abstract' = length=5[0-4]</li>
     * <li>vector_data[map]='minZoom;maxZoom;srid;0' = length=4[0-3]</li>
     * <li>vector_extent[map]='0;extent_0-6;?,?,?;getDatabasePath()' = length=4[0-3]</li>
     * </ol>
     * Note: title and abstract (short/long descriptions) replace ';' with '-'
     * <p> vector_key/data documetation : 20150718
     * 
     * @param forceRead force a re-reading of the resources.
     * @return the list of available tables.
     * @throws Exception  if something goes wrong.
     */
    public List<CustomTileTable> getTables( boolean forceRead ) throws Exception {
        if (customtileTableList == null || forceRead) {
            customtileTableList = new ArrayList<CustomTileTable>();
           String layerType = SpatialDataType.MAPURL.getTypeName();
           String s_title=databaseFileNameNoExtension.replace(";","-");
           String s_abstract=tableName.replace(";","-");
           String vector_key=s_abstract+";0;"+layerType+";"+s_title+";"+s_abstract+"";
           String s_bounds=this.boundsWest + "," + this.boundsSouth + "," + this.boundsEast + "," + this.boundsNorth+ "," + this.centerX+ "," + this.centerY+","+defaultZoom;
           String vector_value=this.minZoom+";"+this.maxZoom+";4326;0;0;"+s_bounds+";?,?,?;"+getDatabasePath();
           CustomTileTable table = new CustomTileTable(null,vector_key,vector_value);
           if ((table != null) && (table.isValid())) {
               customtileTableList.add(table);
           }
        }
        return customtileTableList;
    }

    // /**
    // * Function to insert a new Tile Bitmap to the mbtiles Database
    // *
    // * <ul>
    // * <li>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will
    // * be converted to 'tms' notation if needed]</li>
    // * <li>checking will be done to determine if the Bitmap is blank [i.e.
    // * all pixels have the same RGB]</li>
    // * </ul>
    // *
    // * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id
    // when image is not blank
    // * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id
    // when image is not blank
    // * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id
    // when image is not blank
    // * @param tile_bitmap the Bitmap to extract image-data extracted from. [Will be converted to
    // JPG or PNG depending on metdata setting]
    // * @return 0: correct, otherwise error
    // */
    // public int insertBitmapTile( int i_x, int i_y_osm, int i_z, Bitmap tile_bitmap, int
    // i_force_unique ) throws IOException {
    // try { // i_rc=0: inserted [if needed bounds min/max zoom have been updated]
    // return mbtiles_db.insertBitmapTile(i_x, i_y_osm, i_z, tile_bitmap, i_force_unique);
    // } catch (IOException e) {
    // return 1;
    // }
    // }

    public void close() throws Exception {
        if (customTileDownloader != null) {
            customTileDownloader.cleanup();
        }
    }

    public byte[] getRasterTile( String query ) {
        throw new RuntimeException("should not be called");
    }

    @Override
    public void open() {
        // TODO we should move opening here
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
}
