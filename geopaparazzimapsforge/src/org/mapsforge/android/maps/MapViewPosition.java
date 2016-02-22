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
package org.mapsforge.android.maps;

import org.mapsforge.core.model.Coordinates;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;

/**
 * A MapPosition stores the latitude and longitude coordinate of a MapView together with its zoom level.
 */
public class MapViewPosition {
	private double latitude;
	private double longitude;
	private final MapView mapView;
	private byte zoomLevel;

	MapViewPosition(MapView mapView) {
		this.mapView = mapView;

		this.latitude = Double.NaN;
		this.longitude = Double.NaN;
		this.zoomLevel = -1;
	}

	/**
	 * @return the current center point of the MapView.
	 */
	public synchronized GeoPoint getMapCenter() {
		return new GeoPoint(this.latitude, this.longitude);
	}

	/**
	 * @return an immutable MapPosition or null, if this map position is not valid.
	 * @see #isValid()
	 */
	public synchronized MapPosition getMapPosition() {
		if (!isValid()) {
			return null;
		}
		GeoPoint geoPoint = new GeoPoint(this.latitude, this.longitude);
		return new MapPosition(geoPoint, this.zoomLevel);
	}

	/**
	 * @return the current zoom level of the MapView.
	 */
	public synchronized byte getZoomLevel() {
		return this.zoomLevel;
	}

	/**
	 * @return true if this MapViewPosition is valid, false otherwise.
	 */
	public synchronized boolean isValid() {
		if (Double.isNaN(this.latitude)) {
			return false;
		} else if (this.latitude < MercatorProjection.LATITUDE_MIN) {
			return false;
		} else if (this.latitude > MercatorProjection.LATITUDE_MAX) {
			return false;
		}

		if (Double.isNaN(this.longitude)) {
			return false;
		} else if (this.longitude < Coordinates.LONGITUDE_MIN) {
			return false;
		} else if (this.longitude > Coordinates.LONGITUDE_MAX) {
			return false;
		}

		return true;
	}

	/**
	 * Moves this MapViewPosition by the given amount of pixels.
	 * 
	 * @param moveHorizontal
	 *            the amount of pixels to move the map horizontally.
	 * @param moveVertical
	 *            the amount of pixels to move the map vertically.
	 */
	public synchronized void moveMap(float moveHorizontal, float moveVertical) {
		double pixelX = MercatorProjection.longitudeToPixelX(this.longitude, this.zoomLevel);
		double pixelY = MercatorProjection.latitudeToPixelY(this.latitude, this.zoomLevel);

		this.latitude = MercatorProjection.pixelYToLatitude(pixelY - moveVertical, this.zoomLevel);
		this.latitude = MercatorProjection.limitLatitude(this.latitude);

		this.longitude = MercatorProjection.pixelXToLongitude(pixelX - moveHorizontal, this.zoomLevel);
		this.longitude = MercatorProjection.limitLongitude(this.longitude);
	}

	synchronized void setMapCenter(GeoPoint geoPoint) {
		this.latitude = MercatorProjection.limitLatitude(geoPoint.getLatitude());
		this.longitude = MercatorProjection.limitLongitude(geoPoint.getLongitude());
	}

	synchronized void setMapCenterAndZoomLevel(MapPosition mapPosition) {
		GeoPoint geoPoint = mapPosition.geoPoint;
		this.latitude = MercatorProjection.limitLatitude(geoPoint.getLatitude());
		this.longitude = MercatorProjection.limitLongitude(geoPoint.getLongitude());
		this.zoomLevel = this.mapView.limitZoomLevel(mapPosition.zoomLevel);
	}

	synchronized void setZoomLevel(byte zoomLevel) {
		this.zoomLevel = this.mapView.limitZoomLevel(zoomLevel);
	}
}
