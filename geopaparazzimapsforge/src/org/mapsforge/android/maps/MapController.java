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

import org.mapsforge.core.model.GeoPoint;

import android.view.KeyEvent;
import android.view.View;

/**
 * A MapController is used to programmatically modify the position and zoom level of a MapView. Each MapController is
 * assigned to a single MapView instance. To retrieve a MapController for a given MapView, use the
 * {@link MapView#getController()} method.
 */
public final class MapController implements View.OnKeyListener {
	private final MapView mapView;

	/**
	 * @param mapView
	 *            the MapView which should be controlled by this MapController.
	 */
	MapController(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
		if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
			// forward the event to the MapView
			return this.mapView.onKeyDown(keyCode, keyEvent);
		} else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
			// forward the event to the MapView
			return this.mapView.onKeyUp(keyCode, keyEvent);
		}
		return false;
	}

	/**
	 * Sets the center of the MapView without an animation to the given point.
	 * 
	 * @param geoPoint
	 *            the new center point of the map.
	 */
	public void setCenter(GeoPoint geoPoint) {
		this.mapView.setCenter(geoPoint);
	}

	/**
	 * Sets the zoom level of the MapView.
	 * 
	 * @param zoomLevel
	 *            the new zoom level, will be limited by the maximum and minimum possible zoom level.
	 * @return the new zoom level.
	 */
	public int setZoom(int zoomLevel) {
		this.mapView.zoom((byte) (zoomLevel - this.mapView.getMapPosition().getZoomLevel()), 1);
		return this.mapView.getMapPosition().getZoomLevel();
	}

	/**
	 * Increases the zoom level of the MapView, unless the maximum zoom level has been reached.
	 * 
	 * @return true if the zoom level has been changed, false otherwise.
	 */
	public boolean zoomIn() {
		return this.mapView.zoom((byte) 1, 1);
	}

	/**
	 * Decreases the zoom level of the MapView, unless the minimum zoom level has been reached.
	 * 
	 * @return true if the zoom level has been changed, false otherwise.
	 */
	public boolean zoomOut() {
		return this.mapView.zoom((byte) -1, 1);
	}
}
