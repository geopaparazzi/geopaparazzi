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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * ItemizedOverlay is an abstract base class to display {@link OverlayItem OverlayItems}. The class defines some methods
 * to access the backing data structure of deriving subclasses. Besides organizing the redrawing process it handles long
 * press and tap events and calls {@link #onLongPress(int)} and {@link #onTap(int)} respectively.
 * 
 * @param <Item>
 *            the type of items handled by this overlay.
 */
public abstract class ItemizedOverlay<Item extends OverlayItem> extends Overlay {
	private static final int INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "ItemizedOverlay";

	/**
	 * Sets the bounds of the given drawable so that (0,0) is the center of the bottom row.
	 * 
	 * @param balloon
	 *            the drawable whose bounds should be set.
	 * @return the given drawable with set bounds.
	 */
	public static Drawable boundCenter(Drawable balloon) {
		balloon.setBounds(balloon.getIntrinsicWidth() / -2, balloon.getIntrinsicHeight() / -2,
				balloon.getIntrinsicWidth() / 2, balloon.getIntrinsicHeight() / 2);
		return balloon;
	}

	/**
	 * Sets the bounds of the given drawable so that (0,0) is the center of the bounding box.
	 * 
	 * @param balloon
	 *            the drawable whose bounds should be set.
	 * @return the given drawable with set bounds.
	 */
	public static Drawable boundCenterBottom(Drawable balloon) {
		balloon.setBounds(balloon.getIntrinsicWidth() / -2, -balloon.getIntrinsicHeight(),
				balloon.getIntrinsicWidth() / 2, 0);
		return balloon;
	}

	private int bottom;
	private final Drawable defaultMarker;
	private Drawable itemMarker;
	private final Point itemPosition;
	private int left;
	private int right;
	private int top;
	private List<Integer> visibleItems;
	private List<Integer> visibleItemsRedraw;

	/**
	 * @param defaultMarker
	 *            the default marker (may be null).
	 */
	public ItemizedOverlay(Drawable defaultMarker) {
		super();
		this.defaultMarker = defaultMarker;
		this.itemPosition = new Point();
		this.visibleItems = new ArrayList<Integer>(INITIAL_CAPACITY);
		this.visibleItemsRedraw = new ArrayList<Integer>(INITIAL_CAPACITY);
	}

	/**
	 * Checks whether an item has been long pressed.
	 */
	@Override
	public boolean onLongPress(GeoPoint geoPoint, MapView mapView) {
		return checkItemHit(geoPoint, mapView, EventType.LONG_PRESS);
	}

	/**
	 * Checks whether an item has been tapped.
	 */
	@Override
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		return checkItemHit(geoPoint, mapView, EventType.TAP);
	}

	/**
	 * @return the numbers of items in this overlay.
	 */
	public abstract int size();

	/**
	 * Checks whether an item has been hit by an event and calls the appropriate handler.
	 * 
	 * @param geoPoint
	 *            the point of the event.
	 * @param mapView
	 *            the {@link MapView} that triggered the event.
	 * @param eventType
	 *            the type of the event.
	 * @return true if an item has been hit, false otherwise.
	 */
	protected boolean checkItemHit(GeoPoint geoPoint, MapView mapView, EventType eventType) {
		Projection projection = mapView.getProjection();
		Point eventPosition = projection.toPixels(geoPoint, null);

		// check if the translation to pixel coordinates has failed
		if (eventPosition == null) {
			return false;
		}

		Point checkItemPoint = new Point();

		synchronized (this.visibleItems) {
			// iterate over all visible items
			for (int i = this.visibleItems.size() - 1; i >= 0; --i) {
				Integer itemIndex = this.visibleItems.get(i);

				// get the current item
				Item checkOverlayItem = createItem(itemIndex.intValue());
				if (checkOverlayItem == null) {
					continue;
				}

				synchronized (checkOverlayItem) {
					// make sure that the current item has a position
					if (checkOverlayItem.getPoint() == null) {
						continue;
					}

					checkItemPoint = projection.toPixels(checkOverlayItem.getPoint(), checkItemPoint);
					// check if the translation to pixel coordinates has failed
					if (checkItemPoint == null) {
						continue;
					}

					// select the correct marker for the item and get the position
					Rect checkMarkerBounds;
					if (checkOverlayItem.getMarker() == null) {
						if (this.defaultMarker == null) {
							// no marker to draw the item
							continue;
						}
						checkMarkerBounds = this.defaultMarker.getBounds();
					} else {
						checkMarkerBounds = checkOverlayItem.getMarker().getBounds();
					}

					// calculate the bounding box of the marker
					int checkLeft = checkItemPoint.x + checkMarkerBounds.left;
					int checkRight = checkItemPoint.x + checkMarkerBounds.right;
					int checkTop = checkItemPoint.y + checkMarkerBounds.top;
					int checkBottom = checkItemPoint.y + checkMarkerBounds.bottom;

					// check if the event position is within the bounds of the marker
					if (checkRight >= eventPosition.x && checkLeft <= eventPosition.x && checkBottom >= eventPosition.y
							&& checkTop <= eventPosition.y) {
						switch (eventType) {
							case LONG_PRESS:
								if (onLongPress(itemIndex.intValue())) {
									return true;
								}
								break;

							case TAP:
								if (onTap(itemIndex.intValue())) {
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
	 * Creates an item in this overlay.
	 * 
	 * @param index
	 *            the index of the item.
	 * @return the item.
	 */
	protected abstract Item createItem(int index);

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
		// erase the list of visible items
		this.visibleItemsRedraw.clear();

		int numberOfItems = size();
		for (int itemIndex = 0; itemIndex < numberOfItems; ++itemIndex) {
			if (isInterrupted() || sizeHasChanged()) {
				// stop working
				return;
			}

			// get the current item
			Item overlayItem = createItem(itemIndex);
			if (overlayItem == null) {
				continue;
			}

			synchronized (overlayItem) {
				// make sure that the current item has a position
				if (overlayItem.getPoint() == null) {
					continue;
				}

				// make sure that the cached item position is valid
				if (drawZoomLevel != overlayItem.cachedZoomLevel) {
					overlayItem.cachedMapPosition = projection.toPoint(overlayItem.getPoint(),
							overlayItem.cachedMapPosition, drawZoomLevel);
					overlayItem.cachedZoomLevel = drawZoomLevel;
				}

				// calculate the relative item position on the canvas
				this.itemPosition.x = overlayItem.cachedMapPosition.x - drawPosition.x;
				this.itemPosition.y = overlayItem.cachedMapPosition.y - drawPosition.y;

				// get the correct marker for the item
				if (overlayItem.getMarker() == null) {
					if (this.defaultMarker == null) {
						// no marker to draw the item
						continue;
					}
					this.itemMarker = this.defaultMarker;
				} else {
					this.itemMarker = overlayItem.getMarker();
				}

				// get the position of the marker
				Rect markerBounds = this.itemMarker.copyBounds();

				// calculate the bounding box of the marker
				this.left = this.itemPosition.x + markerBounds.left;
				this.right = this.itemPosition.x + markerBounds.right;
				this.top = this.itemPosition.y + markerBounds.top;
				this.bottom = this.itemPosition.y + markerBounds.bottom;

				// check if the bounding box of the marker intersects with the canvas
				if (this.right >= 0 && this.left <= canvas.getWidth() && this.bottom >= 0
						&& this.top <= canvas.getHeight()) {
					// set the position of the marker
					this.itemMarker.setBounds(this.left, this.top, this.right, this.bottom);

					// draw the item marker on the canvas
					this.itemMarker.draw(canvas);

					// restore the position of the marker
					this.itemMarker.setBounds(markerBounds);

					// add the current item index to the list of visible items
					this.visibleItemsRedraw.add(Integer.valueOf(itemIndex));
				}
			}
		}

		// swap the two visible item lists
		synchronized (this.visibleItems) {
			List<Integer> visibleItemsTemp = this.visibleItems;
			this.visibleItems = this.visibleItemsRedraw;
			this.visibleItemsRedraw = visibleItemsTemp;
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
	 *            the index of the item that has been long pressed.
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
	 *            the index of the item that has been tapped.
	 * @return true if the event was handled, false otherwise.
	 */
	protected boolean onTap(int index) {
		return false;
	}

	/**
	 * This method should be called after items have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}
