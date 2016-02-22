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
package org.mapsforge.android.maps.mapgenerator.tiledownloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Abstract base class for downloading map tiles from a server.
 */
public abstract class TileDownloader implements MapGenerator {
	private static final Logger LOGGER = Logger.getLogger(TileDownloader.class.getName());
	private static final GeoPoint START_POINT = new GeoPoint(51.33, 10.45);
	private static final Byte START_ZOOM_LEVEL = Byte.valueOf((byte) 5);

	protected final int[] pixels;

	/**
	 * Default constructor that must be called by subclasses.
	 */
	protected TileDownloader() {
		this.pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
	}

	@Override
	public void cleanup() {
		// do nothing
	}

	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		try {
			Tile tile = mapGeneratorJob.tile;
			URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
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
			LOGGER.log(Level.SEVERE, null, e);
			return false;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
			return false;
		}
	}

	/**
	 * @return the host name of the tile download server.
	 */
	public abstract String getHostName();

	/**
	 * @return the protocol which is used to connect to the server.
	 */
	public abstract String getProtocol();

	@Override
	public GeoPoint getStartPoint() {
		return START_POINT;
	}

	@Override
	public Byte getStartZoomLevel() {
		return START_ZOOM_LEVEL;
	}

	/**
	 * @param tile
	 *            the tile for which a map image is required.
	 * @return the absolute path to the map image.
	 */
	public abstract String getTilePath(Tile tile);

	@Override
	public boolean requiresInternetConnection() {
		return true;
	}
}
