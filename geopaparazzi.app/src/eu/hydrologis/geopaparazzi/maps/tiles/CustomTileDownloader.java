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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.Tile;

import eu.geopaparazzi.library.util.debug.Logger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
public class CustomTileDownloader extends TileDownloader {

    // http://88.53.214.52/sitr/rest/services/CACHED/ortofoto_ata20072008_webmercatore/MapServer/tile/{z}/{y}/{x}

    private static final String HOST_NAME = "88.53.214.52/sitr/rest/services/CACHED/ortofoto_ata20072008_webmercatore/MapServer/tile";
    private static final String PROTOCOL = "http";
    private static final byte ZOOM_MAX = 18;

    private final StringBuilder stringBuilder;

    /**
     * Constructs a new CustomTileDownloader.
     */
    public CustomTileDownloader() {
        super();
        this.stringBuilder = new StringBuilder();

        try {
            URL url = new URL(getProtocol(), getHostName(), "/10/394/549.png");
//            URL url = new URL(
//                    "http://88.53.214.52/sitr/rest/services/CACHED/ortofoto_ata20072008_webmercatore/MapServer/tile/10/394/549.png");
            InputStream inputStream = url.openStream();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getHostName() {
        return HOST_NAME;
    }

    public String getProtocol() {
        return PROTOCOL;
    }

    public String getTilePath( Tile tile ) {
        this.stringBuilder.setLength(0);
        this.stringBuilder.append('/');
        this.stringBuilder.append(tile.zoomLevel);
        this.stringBuilder.append('/');
        this.stringBuilder.append(tile.tileY);
        this.stringBuilder.append('/');
        this.stringBuilder.append(tile.tileX);
        this.stringBuilder.append(".png");

        return this.stringBuilder.toString();
    }
    
    @Override
    public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
        try {
            Tile tile = mapGeneratorJob.tile;
            
            StringBuilder sb = new StringBuilder();
            sb.append("http://");
            sb.append(HOST_NAME);
            sb.append(getTilePath(tile));
            
            URL url = new URL(sb.toString());
            InputStream inputStream = url.openStream();
            Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // check if the input stream could be decoded into a bitmap
            if (decodedBitmap == null) {
                return false;
            }

            // copy all pixels from the decoded bitmap to the color array
            decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
            decodedBitmap.recycle();

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
}
