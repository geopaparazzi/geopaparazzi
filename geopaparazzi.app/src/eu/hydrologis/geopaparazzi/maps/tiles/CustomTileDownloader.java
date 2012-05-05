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

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import eu.geopaparazzi.library.util.FileUtilities;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
public class CustomTileDownloader extends TileDownloader {

    // http://88.53.214.52/sitr/rest/services/CACHED/ortofoto_ata20072008_webmercatore/MapServer/tile/{z}/{y}/{x}

    private static String HOST_NAME;
    private static final String PROTOCOL = "http"; //$NON-NLS-1$
    private static final byte ZOOM_MAX = 18;

    private String tilePart;

    public CustomTileDownloader( String urlTemplate ) {
        super();
        int indexOfZ = urlTemplate.indexOf("ZZZ"); //$NON-NLS-1$
        HOST_NAME = urlTemplate.substring(0, indexOfZ);
        tilePart = urlTemplate.substring(indexOfZ);
        HOST_NAME = HOST_NAME.substring(7);

    }

    public String getHostName() {
        return HOST_NAME;
    }

    public String getProtocol() {
        return PROTOCOL;
    }

    public String getTilePath( Tile tile ) {
        String tmpTilePart = tilePart.replaceFirst("ZZZ", String.valueOf(tile.zoomLevel)); //$NON-NLS-1$
        tmpTilePart = tmpTilePart.replaceFirst("XXX", String.valueOf(tile.tileX)); //$NON-NLS-1$
        tmpTilePart = tmpTilePart.replaceFirst("YYY", String.valueOf(tile.tileY)); //$NON-NLS-1$

        return tmpTilePart;
    }

    @Override
    public boolean executeJob( MapGeneratorJob mapGeneratorJob, Bitmap bitmap ) {
        try {
            Tile tile = mapGeneratorJob.tile;

            StringBuilder sb = new StringBuilder();
            sb.append("http://"); //$NON-NLS-1$
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

    public static CustomTileDownloader file2TileDownloader( File file ) throws IOException {
        String text = FileUtilities.readfile(file).trim();
        return new CustomTileDownloader(text);
    }

}
