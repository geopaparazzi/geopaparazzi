/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maps.tiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
public class CustomTileDownloader extends TileDownloader {

    /**
     * Possible schemas
     */
    private enum TILESCHEMA {
        tms, google, wms
    }

    private static String HOST_NAME;
    private static String PROTOCOL = "http"; //$NON-NLS-1$
    private static byte ZOOM_MIN = 0;
    private static byte ZOOM_MAX = 18;

    private GeoPoint centerPoint = new GeoPoint(0, 0);

    private String tilePart;
    private boolean isFile = false;
    private TILESCHEMA type = TILESCHEMA.google;

    @SuppressWarnings("nls")
    public CustomTileDownloader( List<String> fileLines, String parentPath ) {
        super();

        for( String line : fileLines ) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }

            int split = line.indexOf('=');
            if (split != -1) {
                String value = line.substring(split + 1).trim();
                if (line.startsWith("url")) {

                    int indexOfZ = value.indexOf("ZZZ");
                    if (indexOfZ != -1) {
                        HOST_NAME = value.substring(0, indexOfZ);
                        tilePart = value.substring(indexOfZ);
                    } else {
                        HOST_NAME = "http://";
                    }
                    if (value.startsWith("http")) {
                        // remove http
                        HOST_NAME = HOST_NAME.substring(7);
                        tilePart = value;
                    } else {
                        PROTOCOL = "file";
                        HOST_NAME = parentPath + File.separator + HOST_NAME;
                        isFile = true;
                    }
                }
                if (line.startsWith("minzoom")) {
                    try {
                        ZOOM_MIN = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("maxzoom")) {
                    try {
                        ZOOM_MAX = Byte.valueOf(value);
                    } catch (Exception e) {
                        // use default: handle exception
                    }
                }
                if (line.startsWith("center")) {
                    try {
                        String[] coord = value.split("\\s+"); //$NON-NLS-1$
                        double x = Double.parseDouble(coord[0]);
                        double y = Double.parseDouble(coord[1]);
                        centerPoint = new GeoPoint(y, x);
                    } catch (NumberFormatException e) {
                        // use default
                    }
                }
                if (line.startsWith("type")) {
                    if (value.equals(TILESCHEMA.tms.toString())) {
                        type = TILESCHEMA.tms;
                    }
                    if (value.equals(TILESCHEMA.wms.toString())) {
                        type = TILESCHEMA.wms;
                    }
                }
            }
        }

    }
    public String getHostName() {
        return HOST_NAME;
    }

    public String getProtocol() {
        return PROTOCOL;
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

        if (type == TILESCHEMA.tms) {
            int[] tmsTiles = Utilities.googleTile2TmsTile(tileX, tileY, zoomLevel);
            tileX = tmsTiles[0];
            tileY = tmsTiles[1];
        }

        if (type == TILESCHEMA.tms || type == TILESCHEMA.google) {
            String tmpTilePart = tilePart.replaceFirst("ZZZ", String.valueOf(zoomLevel)); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("XXX", String.valueOf(tileX)); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tileY)); //$NON-NLS-1$
            return tmpTilePart;
        }
        if (type == TILESCHEMA.wms) {
            // minx, miny, maxx, maxy
            double[] tileBounds = Utilities.tileLatLonBounds(tileX, tileY, zoomLevel, Tile.TILE_SIZE);
            String tmpTilePart = tilePart.replaceFirst("XXX", String.valueOf(tileBounds[0])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tileBounds[1])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("XXX", String.valueOf(tileBounds[2])); //$NON-NLS-1$
            tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tileBounds[3])); //$NON-NLS-1$
            return tmpTilePart;
        }
        return ""; //$NON-NLS-1$
    }

    @Override
    public boolean executeJob( MapGeneratorJob mapGeneratorJob, Bitmap bitmap ) {
        try {
            Tile tile = mapGeneratorJob.tile;
            String tilePath = getTilePath(tile);

            StringBuilder sb = new StringBuilder();
            if (isFile) {
                sb.append("file:"); //$NON-NLS-1$
            } else {
                if (!tilePath.startsWith("http")) //$NON-NLS-1$
                    sb.append("http://"); //$NON-NLS-1$
            }
            sb.append(HOST_NAME);
            sb.append(tilePath);

            URL url = new URL(sb.toString());
            InputStream inputStream = url.openStream();
            Bitmap decodedBitmap = null;
            try {
                decodedBitmap = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                // ignore and set the image as empty
                if (Debug.D)
                    Logger.i(this, "Could not find image: " + sb.toString()); //$NON-NLS-1$
            } finally {
                inputStream.close();
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
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public byte getZoomLevelMax() {
        return ZOOM_MAX;
    }

    public static CustomTileDownloader file2TileDownloader( File file, String parentPath ) throws IOException {
        List<String> fileLines = FileUtilities.readfileToList(file);
        return new CustomTileDownloader(fileLines, parentPath);
    }
    // public void setMapView( MapView mapView ) {
    // this.mapView = mapView;
    // }

}
