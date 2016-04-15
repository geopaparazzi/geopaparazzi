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
package org.mapsforge.android.maps.inputhandling;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.core.model.GeoPoint;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Abstract base class for the single-touch and the multi-touch handler.
 */
public abstract class TouchEventHandler {

	/**
	 * @param context
	 *            a reference to the global application environment.
	 * @param mapView
	 *            the MapView from which the touch events are coming from.
	 * @return a new TouchEventHandler instance, depending on the current Android version.
	 */
	public static TouchEventHandler getInstance(Context context, MapView mapView) {
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			return new SingleTouchHandler(context, mapView);
		}
		return new MultiTouchHandler(context, mapView);
	}

	/**
	 * Absolute threshold value for a double-tap event.
	 */
	final float doubleTapDelta;

	/**
	 * Maximum time difference in milliseconds for a double-tap event.
	 */
	final int doubleTapTimeout;

	/**
	 * Thread for detecting long press events.
	 */
	final LongPressDetector longPressDetector;

	/**
	 * Duration in milliseconds for a long press event.
	 */
	final int longPressTimeout;

	/**
	 * Absolute threshold value of a motion event to be interpreted as a move.
	 */
	final float mapMoveDelta;

	/**
	 * The MapView from which the touch events are coming from.
	 */
	final MapView mapView;

	/**
	 * Flag to indicate if the map movement threshold has been reached.
	 */
	boolean moveThresholdReached;

	/**
	 * Flag to store if the previous event was a touch event.
	 */
	boolean previousEventTap;

	/**
	 * Stores the x coordinate of the previous touch event.
	 */
	float previousPositionX;

	/**
	 * Stores the y coordinate of the previous touch event.
	 */
	float previousPositionY;

	/**
	 * Stores the time of the previous tap event.
	 */
	long previousTapTime;

	/**
	 * Stores the X position of the previous tap event.
	 */
	float previousTapX;

	/**
	 * Stores the Y position of the previous tap event.
	 */
	float previousTapY;

	TouchEventHandler(Context context, MapView mapView) {
		this.mapView = mapView;
		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		this.mapMoveDelta = viewConfiguration.getScaledTouchSlop();
		this.doubleTapDelta = viewConfiguration.getScaledDoubleTapSlop();
		this.doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
		this.longPressTimeout = ViewConfiguration.getLongPressTimeout();
		this.longPressDetector = new LongPressDetector(this);
		this.longPressDetector.start();
	}

	/**
	 * Destroys this TouchEventHandler.
	 */
	public final void destroy() {
		this.longPressDetector.interrupt();
	}

	/**
	 * @param motionEvent
	 *            the event to extract the action from.
	 * @return the action from the given motion event.
	 */
	public abstract int getAction(MotionEvent motionEvent);

	/**
	 * Handles a motion event on the touch screen.
	 * 
	 * @param motionEvent
	 *            the motion event.
	 * @return true if the event was handled, false otherwise.
	 */
	public final boolean handleTouchEvent(MotionEvent motionEvent) {
		if (!this.mapView.isClickable()) {
			return true;
		}

		// round the event coordinates to integers
		motionEvent.setLocation((int) motionEvent.getX(), (int) motionEvent.getY());

		return handleMotionEvent(motionEvent);
	}

	/**
	 * Forwards a long press event to all overlays until it has been handled.
	 * 
	 * @return true if the long press event has been handled, false otherwise.
	 */
	final boolean forwardLongPressEvent() {
		GeoPoint longPressPoint = this.mapView.getProjection().fromPixels((int) this.previousPositionX,
				(int) this.previousPositionY);
		if (longPressPoint != null) {
			synchronized (this.mapView.getOverlays()) {
				for (int i = this.mapView.getOverlays().size() - 1; i >= 0; --i) {
					if (this.mapView.getOverlays().get(i).onLongPress(longPressPoint, this.mapView)) {
						// the long press event has been handled
						return true;
					}
				}
			}
		}
		return false;
	}

	abstract boolean handleMotionEvent(MotionEvent motionEvent);
}
