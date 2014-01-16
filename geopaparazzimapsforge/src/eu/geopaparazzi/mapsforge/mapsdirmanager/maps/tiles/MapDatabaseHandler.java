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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jsqlite.Exception;

import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.graphics.Bitmap;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.core.MbtilesDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialTable;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialVectorTable;

/**
 * An utility class to handle a map database.
 *
 * author Andrea Antonello (www.hydrologis.com)
 * adapted to work with map databases [mapsforge] Mark Johnson (www.mj10777.de)
 */
@SuppressWarnings("nls")
public class MapDatabaseHandler extends SpatialDatabaseHandler {
    private List<MapTable> mapTableList;
    private FileOpenResult fileOpenResult;
    private MapDatabase mapDatabase = null;
    private MapFileInfo mapFileInfo = null;

    // mbtiles specific for
    private String s_mbtiles_file;
    private File file_mbtiles = null; // mbtiles specific
    private String s_format; // mbtiles specific

    private String s_tile_row_type = "tms"; // mbtiles specific
    private int i_force_unique = 0;
    private int i_force_bounds = 1; // after each insert, update bounds and min/max zoom levels
    private MbtilesDatabaseHandler mbtiles_db = null;
    private HashMap<String, String> mbtiles_metadata = null;

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
            GPLog.androidLog(4, "MapDatabaseHandler[" + databaseFile.getAbsolutePath() + "]", e);
        }
    }

    @Override
    public void open() {
        // TODO we should move opening here
    }

    public List<MapTable> getTables( boolean forceRead ) throws Exception {
        if (mapTableList == null || forceRead) {
            mapTableList = new ArrayList<MapTable>();
            double[] d_bounds = {boundsWest, boundsSouth, boundsEast, boundsNorth};
            // String tableName = metadata.name;
            // String columnName = null;
            MapTable table = new MapTable(databasePath, tableName, "3857", minZoom, maxZoom, centerX, centerY, "?,?,?", d_bounds);
            table.setDefaultZoom(defaultZoom);
            // for mbtiles the desired center can be set by the
            // database developer and may be different than the
            // true center/zoom
            mapTableList.add(table);
        }
        return mapTableList;
    }

    // -----------------------------------------------
    /**
      * Function to retrieve Tile byte[] from the mbtiles Database [for 'SpatialiteDatabaseHandler']
      *
      * <p>i_y_osm must be in is Open-Street-Map 'Slippy Map'
      * notation [will be converted to 'tms' notation if needed]
      *
      * @param query Format 'z,x,y_osm'
      * @return byte[] of the tile or null if no tile matched the given parameters
      */
    public byte[] getRasterTile( String query ) {
        String[] split = query.split(",");
        if (split.length != 3) {
            return null;
        }
        int i_z = 0;
        int i_x = 0;
        int i_y_osm = 0;
        try {
            i_z = Integer.parseInt(split[0]);
            i_x = Integer.parseInt(split[1]);
            i_y_osm = Integer.parseInt(split[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        Tile tile = new Tile((long) i_x, (long) i_y_osm, (byte) i_z);
        // mbtiles_db
        byte[] tileAsBytes = null; // db_mbtiles.getTileAsBytes(i_x, i_y_osm, i_z);
        return tileAsBytes;
    }

    /**
      * Function to retrieve Tile Bitmap from the mbtiles Database [for 'CustomTileDownloader']
      *
      * <p>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will be converted to 'tms' notation if needed]
      *
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_pixel_size the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param tile_bitmap retrieve the Bitmap as done in 'CustomTileDownloader'
      * @return Bitmap of the tile or null if no tile matched the given parameters
      */
    public boolean getBitmapTile( int i_x, int i_y_osm, int i_z, int i_pixel_size, Bitmap tile_bitmap ) {
        boolean b_rc = true;
        Tile tile = new Tile((long) i_x, (long) i_y_osm, (byte) i_z);
        /*
        if (db_mbtiles.getmbtiles() == null) { // in case .'open' was forgotten
            db_mbtiles.open(true, ""); // "" : default value will be used '1.1'
        }
        int[] pixels = new int[i_pixel_size * i_pixel_size];
        byte[] rasterBytes = db_mbtiles.getTileAsBytes(i_x, i_y_osm, i_z);
        if (rasterBytes == null) {
            b_rc = false;
            return b_rc;
        }
        Bitmap decodedBitmap = null;
        decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);
        // check if the input stream could be decoded into a bitmap
        if (decodedBitmap != null) { // copy all pixels from the decoded bitmap to the color array
            decodedBitmap.getPixels(pixels, 0, i_pixel_size, 0, 0, i_pixel_size, i_pixel_size);
            decodedBitmap.recycle();
        } else {
            b_rc = false;
            return b_rc;
        }
        // copy all pixels from the color array to the tile bitmap
        tile_bitmap.setPixels(pixels, 0, i_pixel_size, 0, 0, i_pixel_size, i_pixel_size);
        */
        return b_rc;
    }

    /**
      * Function to insert a new Tile Bitmap to the mbtiles Database
      *
      * <ul>
      *  <li>i_y_osm must be in is Open-Street-Map 'Slippy Map' notation [will
      *      be converted to 'tms' notation if needed]</li>
      *  <li>checking will be done to determine if the Bitmap is blank [i.e.
      *      all pixels have the same RGB]</li>
      * </ul>
      *
      * @param i_x the value for tile_column field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_y_osm the value for tile_row field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param i_z the value for zoom_level field in the map,tiles Tables and part of the tile_id when image is not blank
      * @param tile_bitmap the Bitmap to extract image-data extracted from. [Will be converted to JPG or PNG depending on metdata setting]
      * @return 0: correct, otherwise error
      */
    public int insertBitmapTile( int i_x, int i_y_osm, int i_z, Bitmap tile_bitmap, int i_force_unique ) throws IOException { // i_rc=
                                                                                                                              // correct,
                                                                                                                              // otherwise
                                                                                                                              // error
        int i_rc = 0;
        try { // i_rc=0: inserted [if needed bounds min/max zoom have been updated]
            i_rc = mbtiles_db.insertBitmapTile(i_x, i_y_osm, i_z, tile_bitmap, i_force_unique);
        } catch (IOException e) {
            i_rc = 1;
            // e.printStackTrace();
        }
        return i_rc;
    }

    public void close() throws Exception {
        if (mapDatabase != null) {
            mapDatabase.closeFile();
        }
    }

    /**
      * Update mbtiles Bounds / Zoom (min/max) levels
      */
    public void update_bounds() {
        if (mbtiles_db != null) {
            mbtiles_db.updateBounds(0);
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
    public float[] getTableBounds( SpatialTable spatialTable ) throws Exception {
        float w = (float) boundsWest;
        float s = (float) boundsSouth;
        float e = (float) boundsEast;
        float n = (float) boundsNorth;
        return new float[]{n, s, e, w};
    }
}
