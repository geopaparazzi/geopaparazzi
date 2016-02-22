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

import android.graphics.Paint;
import android.graphics.Point;

/**
 * OverlayWay holds all parameters of a single way on a {@link WayOverlay}. All rendering parameters like color, stroke
 * width, pattern and transparency can be configured via two {@link Paint} objects. Each way is drawn twice - once with
 * each paint object - to allow for different outlines and fillings. The drawing quality can be improved by enabling
 * {@link Paint#setAntiAlias(boolean) anti-aliasing}.
 * <p>
 * The way data is represented as a two-dimensional array in order to support multi-polygons. A multi-polygon consists
 * of several polygons and can for example be used to draw a polygon with holes. Each array element on the first level
 * stores on the second level the coordinates of one polygon.
 */
public class OverlayWay {
	/**
	 * Checks whether the given arrays have the same lengths on each dimension.
	 * 
	 * @param array1
	 *            the first array to check.
	 * @param array2
	 *            the second array to check.
	 * @return true if the arrays have the same length on each dimension, false otherwise.
	 */
	private static boolean arrayLengthsEqual(Object[][] array1, Object[][] array2) {
		if (array1.length != array2.length) {
			return false;
		}
		for (int i = array1.length - 1; i >= 0; --i) {
			if (array1[i].length != array2[i].length) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks the given way nodes for null elements.
	 * 
	 * @param wayNodes
	 *            the way nodes to check for null elements.
	 * @return true if the way nodes contain at least one null element, false otherwise.
	 */
	private static boolean containsNullElements(GeoPoint[][] wayNodes) {
		for (int i = wayNodes.length - 1; i >= 0; --i) {
			if (wayNodes[i] == null) {
				return true;
			}
			for (int j = wayNodes[i].length - 1; j >= 0; --j) {
				if (wayNodes[i][j] == null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Paint which will be used to fill the way.
	 */
	public Paint paintFill;

	/**
	 * Paint which will be used to draw the way outline.
	 */
	public Paint paintOutline;

	/**
	 * Geographical coordinates of the way nodes.
	 */
	public GeoPoint[][] wayNodes;

	/**
	 * Cached way positions of the way nodes on the map.
	 */
	public Point[][] cachedWayPositions;

	/**
	 * Zoom level of the cached way node positions.
	 */
	public byte cachedZoomLevel;

	/**
	 * Flag to indicate if at least one paint is set for this way.
	 */
	public boolean hasPaint;

	/**
	 * Constructs a new OverlayWay.
	 */
	public OverlayWay() {
		this(null, null, null);
	}

	/**
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes, must not contain null elements.
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public OverlayWay(GeoPoint[][] wayNodes) {
		this(wayNodes, null, null);
	}

	/**
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes, must not contain null elements.
	 * @param paintFill
	 *            the paint which will be used to fill the way (may be null).
	 * @param paintOutline
	 *            the paint which will be used to draw the way outline (may be null).
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public OverlayWay(GeoPoint[][] wayNodes, Paint paintFill, Paint paintOutline) {
		this.cachedWayPositions = new Point[0][0];
		this.cachedZoomLevel = Byte.MIN_VALUE;
		setWayNodesInternal(wayNodes);
		setPaintInternal(paintFill, paintOutline);
	}

	/**
	 * @param paintFill
	 *            the paint which will be used to fill the way (may be null).
	 * @param paintOutline
	 *            the paint which will be used to draw the way outline (may be null).
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public OverlayWay(Paint paintFill, Paint paintOutline) {
		this(null, paintFill, paintOutline);
	}

	/**
	 * @return a copy of the way nodes of this way.
	 */
	public synchronized GeoPoint[][] getWayNodes() {
		return this.wayNodes.clone();
	}

	/**
	 * Sets the paints which will be used to draw this way.
	 * <p>
	 * Changes might not become visible until {@link Overlay#requestRedraw()} is called.
	 * 
	 * @param paintFill
	 *            the paint which will be used to fill the way (may be null).
	 * @param paintOutline
	 *            the paint which will be used to draw the way outline (may be null).
	 */
	public synchronized void setPaint(Paint paintFill, Paint paintOutline) {
		setPaintInternal(paintFill, paintOutline);
	}

	/**
	 * Sets the way nodes of this way.
	 * <p>
	 * Changes might not become visible until {@link Overlay#requestRedraw()} is called.
	 * 
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes, must not contain null elements.
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public synchronized void setWayNodes(GeoPoint[][] wayNodes) {
		setWayNodesInternal(wayNodes);
	}

	private void setPaintInternal(Paint paintFill, Paint paintOutline) {
		this.paintFill = paintFill;
		this.paintOutline = paintOutline;
		this.hasPaint = paintFill != null || paintOutline != null;
	}

	private void setWayNodesInternal(GeoPoint[][] wayNodes) {
		if (wayNodes == null) {
			this.wayNodes = null;
		} else if (containsNullElements(wayNodes)) {
			throw new IllegalArgumentException("way nodes must not contain null elements");
		} else {
			this.wayNodes = wayNodes.clone();
		}

		if (this.wayNodes == null) {
			this.cachedWayPositions = new Point[0][0];
		} else if (!arrayLengthsEqual(this.wayNodes, this.cachedWayPositions)) {
			this.cachedWayPositions = new Point[this.wayNodes.length][];
			for (int i = this.wayNodes.length - 1; i >= 0; --i) {
				this.cachedWayPositions[i] = new Point[this.wayNodes[i].length];
			}
		}
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}
}
