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

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

/**
 * CircleOverlay is an abstract base class to display {@link OverlayCircle OverlayCircles}. The class defines some
 * methods to access the backing data structure of deriving subclasses. Besides organizing the redrawing process it
 * handles long press and tap events and calls {@link #onLongPress(int)} and {@link #onTap(int)} respectively.
 * <p>
 * The overlay may be used to indicate positions which have a known accuracy, such as GPS fixes. The radius of the
 * circles is specified in meters and will be automatically converted to pixels at each redraw.
 * 
 * @param <Circle>
 *            the type of circles handled by this overlay.
 */
public abstract class CircleOverlay<Circle extends OverlayCircle> extends Overlay {
	private static final int INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "CircleOverlay";

	private final Point circlePosition;
	private final Paint defaultPaintFill;
	private final Paint defaultPaintOutline;
	private final boolean hasDefaultPaint;
	private final Path path;
	private List<Integer> visibleCircles;
	private List<Integer> visibleCirclesRedraw;

	/**
	 * @param defaultPaintFill
	 *            the default paint which will be used to fill the circles (may be null).
	 * @param defaultPaintOutline
	 *            the default paint which will be used to draw the circle outlines (may be null).
	 */
	public CircleOverlay(Paint defaultPaintFill, Paint defaultPaintOutline) {
		super();
		this.defaultPaintFill = defaultPaintFill;
		this.defaultPaintOutline = defaultPaintOutline;
		this.hasDefaultPaint = defaultPaintFill != null || defaultPaintOutline != null;
		this.circlePosition = new Point();
		this.visibleCircles = new ArrayList<Integer>(INITIAL_CAPACITY);
		this.visibleCirclesRedraw = new ArrayList<Integer>(INITIAL_CAPACITY);
		this.path = new Path();
	}

	/**
	 * Checks whether a circle has been long pressed.
	 */
	@Override
	public boolean onLongPress(GeoPoint geoPoint, MapView mapView) {
		return checkItemHit(geoPoint, mapView, EventType.LONG_PRESS);
	}

	/**
	 * Checks whether a circle has been tapped.
	 */
	@Override
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		return checkItemHit(geoPoint, mapView, EventType.TAP);
	}

	/**
	 * @return the numbers of circles in this overlay.
	 */
	public abstract int size();

	private void drawPathOnCanvas(Canvas canvas, Circle overlayCircle) {
		if (overlayCircle.hasPaint) {
			// use the paints from the current circle
			if (overlayCircle.paintOutline != null) {
				canvas.drawPath(this.path, overlayCircle.paintOutline);
			}
			if (overlayCircle.paintFill != null) {
				canvas.drawPath(this.path, overlayCircle.paintFill);
			}
		} else if (this.hasDefaultPaint) {
			// use the default paint objects
			if (this.defaultPaintOutline != null) {
				canvas.drawPath(this.path, this.defaultPaintOutline);
			}
			if (this.defaultPaintFill != null) {
				canvas.drawPath(this.path, this.defaultPaintFill);
			}
		}
	}

	/**
	 * Checks whether a circle has been hit by an event and calls the appropriate handler.
	 * 
	 * @param geoPoint
	 *            the point of the event.
	 * @param mapView
	 *            the {@link MapView} that triggered the event.
	 * @param eventType
	 *            the type of the event.
	 * @return true if a circle has been hit, false otherwise.
	 */
	protected boolean checkItemHit(GeoPoint geoPoint, MapView mapView, EventType eventType) {
		Projection projection = mapView.getProjection();
		Point eventPosition = projection.toPixels(geoPoint, null);

		// check if the translation to pixel coordinates has failed
		if (eventPosition == null) {
			return false;
		}

		Point checkCirclePoint = new Point();

		synchronized (this.visibleCircles) {
			// iterate over all visible circles
			for (int i = this.visibleCircles.size() - 1; i >= 0; --i) {
				Integer circleIndex = this.visibleCircles.get(i);

				// get the current circle
				Circle checkOverlayCircle = createCircle(circleIndex.intValue());
				if (checkOverlayCircle == null) {
					continue;
				}

				synchronized (checkOverlayCircle) {
					// make sure that the current circle has a center position and a radius
					if (checkOverlayCircle.center == null || checkOverlayCircle.radius < 0) {
						continue;
					}

					checkCirclePoint = projection.toPixels(checkOverlayCircle.center, checkCirclePoint);
					// check if the translation to pixel coordinates has failed
					if (checkCirclePoint == null) {
						continue;
					}

					// calculate the Euclidian distance between the circle and the event position
					float diffX = checkCirclePoint.x - eventPosition.x;
					float diffY = checkCirclePoint.y - eventPosition.y;
					double distance = Math.sqrt(diffX * diffX + diffY * diffY);

					// check if the event position is within the circle radius
					if (distance <= checkOverlayCircle.cachedRadius) {
						switch (eventType) {
							case LONG_PRESS:
								if (onLongPress(circleIndex.intValue())) {
									return true;
								}
								break;

							case TAP:
								if (onTap(circleIndex.intValue())) {
									return true;
								}
								break;
						}
					}
				}
			}
		}

		// no hit
		return false;
	}

	/**
	 * Creates a circle in this overlay.
	 * 
	 * @param index
	 *            the index of the circle.
	 * @return the circle.
	 */
	protected abstract Circle createCircle(int index);

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
		// erase the list of visible circles
		this.visibleCirclesRedraw.clear();

		int numberOfCircles = size();
		for (int circleIndex = 0; circleIndex < numberOfCircles; ++circleIndex) {
			if (isInterrupted() || sizeHasChanged()) {
				// stop working
				return;
			}

			// get the current circle
			Circle overlayCircle = createCircle(circleIndex);
			if (overlayCircle == null) {
				continue;
			}

			synchronized (overlayCircle) {
				// make sure that the current circle has a center position and a radius
				if (overlayCircle.center == null || overlayCircle.radius < 0) {
					continue;
				}

				// make sure that the cached center position is valid
				if (drawZoomLevel != overlayCircle.cachedZoomLevel) {
					overlayCircle.cachedCenterPosition = projection.toPoint(overlayCircle.center,
							overlayCircle.cachedCenterPosition, drawZoomLevel);
					overlayCircle.cachedZoomLevel = drawZoomLevel;
					overlayCircle.cachedRadius = projection.metersToPixels(overlayCircle.radius, drawZoomLevel);
				}

				// calculate the relative circle position on the canvas
				this.circlePosition.x = overlayCircle.cachedCenterPosition.x - drawPosition.x;
				this.circlePosition.y = overlayCircle.cachedCenterPosition.y - drawPosition.y;
				float circleRadius = overlayCircle.cachedRadius;

				// check if the bounding box of the circle intersects with the canvas
				if ((this.circlePosition.x + circleRadius) >= 0
						&& (this.circlePosition.x - circleRadius) <= canvas.getWidth()
						&& (this.circlePosition.y + circleRadius) >= 0
						&& (this.circlePosition.y - circleRadius) <= canvas.getHeight()) {
					// assemble the path
					this.path.reset();
					this.path.addCircle(this.circlePosition.x, this.circlePosition.y, circleRadius, Path.Direction.CCW);

					if (overlayCircle.hasPaint || this.hasDefaultPaint) {
						drawPathOnCanvas(canvas, overlayCircle);

						// add the current circle index to the list of visible circles
						this.visibleCirclesRedraw.add(Integer.valueOf(circleIndex));
					}
				}
			}
		}

		// swap the two visible circle lists
		synchronized (this.visibleCircles) {
			List<Integer> visibleCirclesTemp = this.visibleCircles;
			this.visibleCircles = this.visibleCirclesRedraw;
			this.visibleCirclesRedraw = visibleCirclesTemp;
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Handles a long press event.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 * 
	 * @param index
	 *            the index of the circle that has been long pressed.
	 * @return true if the event was handled, false otherwise.
	 */
	protected boolean onLongPress(int index) {
		return false;
	}

	/**
	 * Handles a tap event.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 * 
	 * @param index
	 *            the index of the circle that has been tapped.
	 * @return true if the event was handled, false otherwise.
	 */
	protected boolean onTap(int index) {
		return false;
	}

	/**
	 * This method should be called after circles have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}
