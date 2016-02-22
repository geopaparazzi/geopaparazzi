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

import org.mapsforge.core.model.Tile;

/**
 * A MapGenerator that downloads tiles from the Mapnik server at OpenStreetMap.
 */
public class MapnikTileDownloader extends TileDownloader {
	private static final String HOST_NAME = "tile.openstreetmap.org";
	private static final String PROTOCOL = "http";
	private static final byte ZOOM_MAX = 18;

	private final StringBuilder stringBuilder;

	/**
	 * Constructs a new MapnikTileDownloader.
	 */
	public MapnikTileDownloader() {
		super();
		this.stringBuilder = new StringBuilder();
	}

	@Override
	public String getHostName() {
		return HOST_NAME;
	}

	@Override
	public String getProtocol() {
		return PROTOCOL;
	}

	@Override
	public String getTilePath(Tile tile) {
		this.stringBuilder.setLength(0);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.zoomLevel);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.tileX);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.tileY);
		this.stringBuilder.append(".png");

		return this.stringBuilder.toString();
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}
}
