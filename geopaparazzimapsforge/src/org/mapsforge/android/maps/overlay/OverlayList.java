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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import org.mapsforge.android.maps.MapView;

/**
 * An OverlayList manages the overlay list of a MapView.
 */
public class OverlayList implements List<Overlay>, RandomAccess {
	private static final int INITIAL_CAPACITY = 2;

	private final List<Overlay> list;
	private final MapView mapView;

	/**
	 * @param mapView
	 *            the MapView whose overlays should be handled.
	 */
	public OverlayList(MapView mapView) {
		this.mapView = mapView;
		this.list = Collections.synchronizedList(new ArrayList<Overlay>(INITIAL_CAPACITY));
	}

	@Override
	public void add(int index, Overlay overlay) {
		setupOverlay(overlay);
		this.list.add(index, overlay);
	}

	@Override
	public boolean add(Overlay overlay) {
		setupOverlay(overlay);
		return this.list.add(overlay);
	}

	@Override
	public boolean addAll(Collection<? extends Overlay> collection) {
		for (Overlay overlay : collection) {
			setupOverlay(overlay);
		}
		return this.list.addAll(collection);
	}

	@Override
	public boolean addAll(int index, Collection<? extends Overlay> collection) {
		for (Overlay overlay : collection) {
			setupOverlay(overlay);
		}
		return this.list.addAll(index, collection);
	}

	@Override
	public void clear() {
		for (int i = size() - 1; i >= 0; --i) {
			get(i).interrupt();
		}
		this.list.clear();
		this.mapView.invalidateOnUiThread();
	}

	@Override
	public boolean contains(Object o) {
		return this.list.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.list.containsAll(c);
	}

	@Override
	public Overlay get(int index) {
		return this.list.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return this.list.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	@Override
	public Iterator<Overlay> iterator() {
		return this.list.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.list.lastIndexOf(o);
	}

	@Override
	public ListIterator<Overlay> listIterator() {
		return this.list.listIterator();
	}

	@Override
	public ListIterator<Overlay> listIterator(int index) {
		return this.list.listIterator(index);
	}

	@Override
	public Overlay remove(int index) {
		Overlay removedElement = this.list.remove(index);
		removedElement.interrupt();
		this.mapView.invalidateOnUiThread();
		return removedElement;
	}

	@Override
	public boolean remove(Object object) {
		boolean listChanged = this.list.remove(object);
		if (object instanceof Overlay) {
			((Overlay) object).interrupt();
		}
		this.mapView.invalidateOnUiThread();
		return listChanged;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean listChanged = this.list.removeAll(collection);
		for (Object object : collection) {
			if (object instanceof Overlay) {
				((Overlay) object).interrupt();
			}
		}
		this.mapView.invalidateOnUiThread();
		return listChanged;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.list.retainAll(c);
	}

	@Override
	public Overlay set(int index, Overlay overlay) {
		setupOverlay(overlay);
		Overlay previousElement = this.list.set(index, overlay);
		previousElement.interrupt();
		this.mapView.invalidateOnUiThread();
		return previousElement;
	}

	@Override
	public int size() {
		return this.list.size();
	}

	@Override
	public List<Overlay> subList(int fromIndex, int toIndex) {
		return this.list.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.list.toArray(a);
	}

	private void setupOverlay(Overlay overlay) {
		if (!overlay.isAlive()) {
			overlay.start();
		}
		overlay.setupOverlay(this.mapView);
	}
}
