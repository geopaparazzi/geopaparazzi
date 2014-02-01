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

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.SpatialRasterTable;

/**
 * A MapGenerator that downloads tiles from geopackage databases.
 */
@SuppressWarnings("nls")
public class GeopackageTileDownloader extends TileDownloader {

    private byte ZOOM_MIN = 0;
    private byte ZOOM_MAX = 18;

    private GeoPoint centerPoint = new GeoPoint(0, 0);

    private String tilePart;
    private SpatialDatabaseHandler spatialDatabaseHandler;

    public GeopackageTileDownloader( SpatialRasterTable table ) throws jsqlite.Exception {
        super();
        SpatialDatabasesManager sdManager = SpatialDatabasesManager.getInstance();
        spatialDatabaseHandler = sdManager.getRasterHandler(table);

        ZOOM_MAX = (byte) table.getMaxZoom();
        ZOOM_MIN = (byte) table.getMinZoom();

        tilePart = table.getTileQuery();
    }

    public String getHostName() {
        return "";
    }

    public String getProtocol() {
        return "";
    }

    @Override
    public GeoPoint getStartPoint() {
        return centerPoint;
    }

    @Override
    public Byte getStartZoomLevel() {
        return ZOOM_MIN;
    }

    public String getTilePath( Tile tile ) {
        int zoomLevel = tile.zoomLevel;
        int tileX = (int) tile.tileX;
        int tileY = (int) tile.tileY;

        String tmpTilePart = tilePart.replaceFirst("\\?", String.valueOf(zoomLevel)); //$NON-NLS-1$
        tmpTilePart = tmpTilePart.replaceFirst("\\?", String.valueOf(tileX)); //$NON-NLS-1$
        tmpTilePart = tmpTilePart.replaceFirst("\\?", String.valueOf(tileY)); //$NON-NLS-1$

        return tmpTilePart;
    }

    @Override
    public boolean executeJob( MapGeneratorJob mapGeneratorJob, Bitmap bitmap ) {
        try {
            Tile tile = mapGeneratorJob.tile;

            String tileQuery = getTilePath(tile);

            byte[] rasterBytes = spatialDatabaseHandler.getRasterTile(tileQuery);
            Bitmap decodedBitmap = null;
            try {
                decodedBitmap = BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.length);
            } catch (Exception e) {
                // ignore and set the image as empty
                if (GPLog.LOG_HEAVY)
                    GPLog.addLogEntry(this, "Could not find image: " + tileQuery); //$NON-NLS-1$
            }
            // check if the input stream could be decoded into a bitmap
            if (decodedBitmap != null) {
                // copy all pixels from the decoded bitmap to the color array
                decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
                decodedBitmap.recycle();
            } else {
                for( int i = 0; i < pixels.length; i++ ) {
                    pixels[i] = Color.WHITE;
                }
            }

            // copy all pixels from the color array to the tile bitmap
            bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
            return true;
        } catch (Exception e) {
            GPLog.androidLog(4, "GeopackageTileDownloader.executeJob]", e);
            return false;
        }
    }
    public byte getZoomLevelMax() {
        return ZOOM_MAX;
    }
}
