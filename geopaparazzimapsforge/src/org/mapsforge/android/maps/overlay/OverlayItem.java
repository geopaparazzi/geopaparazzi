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
package org.mapsforge.android.maps.overlay;

import org.mapsforge.core.model.GeoPoint;

import android.graphics.Point;
import android.graphics.drawable.Drawable;

/**
 * OverlayItem holds all parameters of a single element on an {@link ItemizedOverlay}, such as position, marker, title
 * and textual description. If the marker is null, the default marker of the overlay will be drawn instead.
 */
public class OverlayItem {
	/**
	 * Marker used to indicate the item.
	 */
	public Drawable marker;

	/**
	 * Geographical position of the item.
	 */
	public GeoPoint point;

	/**
	 * Short description of the item.
	 */
	public String snippet;

	/**
	 * Title of the item.
	 */
	public String title;

	/**
	 * Cached position of the item on the map.
	 */
	public Point cachedMapPosition;

	/**
	 * Zoom level of the cached map position.
	 */
	public byte cachedZoomLevel;

	/**
	 * Constructs a new OverlayItem.
	 */
	public OverlayItem() {
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}

	/**
	 * @param point
	 *            the geographical position of the item (may be null).
	 * @param title
	 *            the title of the item (may be null).
	 * @param snippet
	 *            the short description of the item (may be null).
	 */
	public OverlayItem(GeoPoint point, String title, String snippet) {
		this.point = point;
		this.title = title;
		this.snippet = snippet;
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}

	/**
	 * @param point
	 *            the geographical position of the item (may be null).
	 * @param title
	 *            the title of the item (may be null).
	 * @param snippet
	 *            the short description of the item (may be null).
	 * @param marker
	 *            the marker that is drawn for the item (may be null). The bounds of the marker must already have been
	 *            set properly, for example by calling {@link ItemizedOverlay#boundCenterBottom(Drawable)}.
	 */
	public OverlayItem(GeoPoint point, String title, String snippet, Drawable marker) {
		this.point = point;
		this.title = title;
		this.snippet = snippet;
		this.marker = marker;
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}

	/**
	 * @return the marker used to indicate this item (may be null).
	 */
	public synchronized Drawable getMarker() {
		return this.marker;
	}

	/**
	 * @return the position of this item (may be null).
	 */
	public synchronized GeoPoint getPoint() {
		return this.point;
	}

	/**
	 * @return the short description of this item (may be null).
	 */
	public synchronized String getSnippet() {
		return this.snippet;
	}

	/**
	 * @return the title of this item (may be null).
	 */
	public synchronized String getTitle() {
		return this.title;
	}

	/**
	 * Sets the marker that is drawn for this item. If the marker is null, the default marker of the overlay will be
	 * drawn instead.
	 * <p>
	 * The bounds of the marker must already have been set properly, for example by calling
	 * {@link ItemizedOverlay#boundCenterBottom(Drawable)}.
	 * <p>
	 * Changes might not become visible until {@link Overlay#requestRedraw()} is called.
	 * 
	 * @param marker
	 *            the marker that is drawn for this item (may be null).
	 */
	public synchronized void setMarker(Drawable marker) {
		this.marker = marker;
	}

	/**
	 * Sets the geographical position of this item.
	 * <p>
	 * Changes might not become visible until {@link Overlay#requestRedraw()} is called.
	 * 
	 * @param point
	 *            the geographical position of the item (may be null).
	 */
	public synchronized void setPoint(GeoPoint point) {
		this.point = point;
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}

	/**
	 * Sets the short description of this item.
	 * 
	 * @param snippet
	 *            the short description of the item (may be null).
	 */
	public synchronized void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	/**
	 * Sets the title of this item.
	 * 
	 * @param title
	 *            the title of the item (may be null).
	 */
	public synchronized void setTitle(String title) {
		this.title = title;
	}
}
