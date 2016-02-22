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
import java.util.Collection;
import java.util.List;

import android.graphics.drawable.Drawable;

/**
 * ArrayItemizedOverlay is a thread-safe implementation of the {@link ItemizedOverlay} class using an {@link ArrayList}
 * as internal data structure. A default marker for all {@link OverlayItem OverlayItems} without an individual marker
 * can be defined via the constructor.
 */
public class ArrayItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private static final int INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "ArrayItemizedOverlay";

	private final List<OverlayItem> overlayItems;

	/**
	 * @param defaultMarker
	 *            the default marker (may be null). This marker is aligned to the center of its bottom line to allow for
	 *            a conical symbol such as a pin or a needle.
	 */
	public ArrayItemizedOverlay(Drawable defaultMarker) {
		this(defaultMarker, true);
	}

	/**
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param alignMarker
	 *            whether the default marker should be aligned or not. If true, the marker is aligned to the center of
	 *            its bottom line to allow for a conical symbol such as a pin or a needle.
	 */
	public ArrayItemizedOverlay(Drawable defaultMarker, boolean alignMarker) {
		super(defaultMarker != null && alignMarker ? ItemizedOverlay.boundCenterBottom(defaultMarker) : defaultMarker);
		this.overlayItems = new ArrayList<OverlayItem>(INITIAL_CAPACITY);
	}

	/**
	 * Adds the given item to the overlay.
	 * 
	 * @param overlayItem
	 *            the item that should be added to the overlay.
	 */
	public void addItem(OverlayItem overlayItem) {
		synchronized (this.overlayItems) {
			this.overlayItems.add(overlayItem);
		}
		populate();
	}

	/**
	 * Adds all items of the given collection to the overlay.
	 * 
	 * @param c
	 *            collection whose items should be added to the overlay.
	 */
	public void addItems(Collection<? extends OverlayItem> c) {
		synchronized (this.overlayItems) {
			this.overlayItems.addAll(c);
		}
		populate();
	}

	/**
	 * Removes all items from the overlay.
	 */
	public void clear() {
		synchronized (this.overlayItems) {
			this.overlayItems.clear();
		}
		populate();
	}

	@Override
	public String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Removes the given item from the overlay.
	 * 
	 * @param overlayItem
	 *            the item that should be removed from the overlay.
	 */
	public void removeItem(OverlayItem overlayItem) {
		synchronized (this.overlayItems) {
			this.overlayItems.remove(overlayItem);
		}
		populate();
	}

	@Override
	public int size() {
		synchronized (this.overlayItems) {
			return this.overlayItems.size();
		}
	}

	@Override
	protected OverlayItem createItem(int index) {
		synchronized (this.overlayItems) {
			if (index >= this.overlayItems.size()) {
				return null;
			}
			return this.overlayItems.get(index);
		}
	}
}
