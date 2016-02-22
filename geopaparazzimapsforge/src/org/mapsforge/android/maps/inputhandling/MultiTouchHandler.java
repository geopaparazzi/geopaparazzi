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
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Implementation for multi-touch capable devices.
 */
public class MultiTouchHandler extends TouchEventHandler {
	private static final int INVALID_POINTER_ID = -1;

	private int activePointerId;
	private long multiTouchDownTime;
	private final ScaleGestureDetector scaleGestureDetector;

	MultiTouchHandler(Context context, MapView mapView) {
		super(context, mapView);
		this.activePointerId = INVALID_POINTER_ID;
		this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(this.mapView));
	}

	@Override
	public int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private boolean onActionCancel() {
		this.longPressDetector.pressStop();
		this.activePointerId = INVALID_POINTER_ID;
		return true;
	}

	private boolean onActionDown(MotionEvent motionEvent) {
		this.longPressDetector.pressStart();
		this.previousPositionX = motionEvent.getX();
		this.previousPositionY = motionEvent.getY();
		this.moveThresholdReached = false;
		// save the ID of the pointer
		this.activePointerId = motionEvent.getPointerId(0);
		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);

		if (this.scaleGestureDetector.isInProgress()) {
			return true;
		}

		// calculate the distance between previous and current position
		float moveX = motionEvent.getX(pointerIndex) - this.previousPositionX;
		float moveY = motionEvent.getY(pointerIndex) - this.previousPositionY;

		if (!this.moveThresholdReached) {
			if (Math.abs(moveX) > this.mapMoveDelta || Math.abs(moveY) > this.mapMoveDelta) {
				// the map movement threshold has been reached
				this.longPressDetector.pressStop();
				this.moveThresholdReached = true;

				// save the position of the event
				this.previousPositionX = motionEvent.getX(pointerIndex);
				this.previousPositionY = motionEvent.getY(pointerIndex);
			}
			return true;
		}

		// save the position of the event
		this.previousPositionX = motionEvent.getX(pointerIndex);
		this.previousPositionY = motionEvent.getY(pointerIndex);

		this.mapView.getFrameBuffer().matrixPostTranslate(moveX, moveY);
		this.mapView.getMapPosition().moveMap(moveX, moveY);
		this.mapView.redrawTiles();
		return true;
	}

	private boolean onActionPointerDown(MotionEvent motionEvent) {
		this.longPressDetector.pressStop();
		this.multiTouchDownTime = motionEvent.getEventTime();
		return true;
	}

	private boolean onActionPointerUp(MotionEvent motionEvent) {
		this.longPressDetector.pressStop();
		// extract the index of the pointer that left the touch sensor
		int pointerIndex = (motionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		if (motionEvent.getPointerId(pointerIndex) == this.activePointerId) {
			// the active pointer has gone up, choose a new one
			if (pointerIndex == 0) {
				pointerIndex = 1;
			} else {
				pointerIndex = 0;
			}
			// save the position of the event
			this.previousPositionX = motionEvent.getX(pointerIndex);
			this.previousPositionY = motionEvent.getY(pointerIndex);
			this.activePointerId = motionEvent.getPointerId(pointerIndex);
		}

		// calculate the time difference since the pointer has gone down
		long multiTouchTime = motionEvent.getEventTime() - this.multiTouchDownTime;
		if (multiTouchTime < this.doubleTapTimeout) {
			// multi-touch tap event, zoom out
			this.previousEventTap = false;
			this.mapView.zoom((byte) -1, 1);
		}
		return true;
	}

	private boolean onActionUp(MotionEvent motionEvent) {
		this.longPressDetector.pressStop();
		int pointerIndex = motionEvent.findPointerIndex(this.activePointerId);
		this.activePointerId = INVALID_POINTER_ID;
		if (this.moveThresholdReached || this.longPressDetector.isEventHandled()) {
			this.previousEventTap = false;
		} else {
			if (this.previousEventTap) {
				// calculate the distance to the previous tap position
				float tapDiffX = Math.abs(motionEvent.getX(pointerIndex) - this.previousTapX);
				float tapDiffY = Math.abs(motionEvent.getY(pointerIndex) - this.previousTapY);
				long tapDiffTime = motionEvent.getEventTime() - this.previousTapTime;

				// check if a double-tap event occurred
				if (tapDiffX < this.doubleTapDelta && tapDiffY < this.doubleTapDelta
						&& tapDiffTime < this.doubleTapTimeout) {
					// double-tap event, zoom in
					this.previousEventTap = false;
					this.mapView.setCenter(this.mapView.getProjection().fromPixels((int) motionEvent.getX(),
							(int) motionEvent.getY()));
					this.mapView.zoom((byte) 1, 1);
					return true;
				}
			} else {
				this.previousEventTap = true;
			}

			// store the position and the time of this tap event
			this.previousTapX = motionEvent.getX(pointerIndex);
			this.previousTapY = motionEvent.getY(pointerIndex);
			this.previousTapTime = motionEvent.getEventTime();

			GeoPoint tapPoint = this.mapView.getProjection().fromPixels((int) motionEvent.getX(pointerIndex),
					(int) motionEvent.getY(pointerIndex));
			if (tapPoint != null) {
				synchronized (this.mapView.getOverlays()) {
					for (int i = this.mapView.getOverlays().size() - 1; i >= 0; --i) {
						if (this.mapView.getOverlays().get(i).onTap(tapPoint, this.mapView)) {
							// the tap event has been handled
							break;
						}
					}
				}
			}
		}
		return true;
	}

	@Override
	boolean handleMotionEvent(MotionEvent motionEvent) {
		// workaround for a bug in the ScaleGestureDetector, see Android issue #12976
		if (motionEvent.getAction() != MotionEvent.ACTION_MOVE || motionEvent.getPointerCount() > 1) {
			this.scaleGestureDetector.onTouchEvent(motionEvent);
		}

		int action = getAction(motionEvent);

		if (action == MotionEvent.ACTION_DOWN) {
			return onActionDown(motionEvent);
		} else if (action == MotionEvent.ACTION_MOVE) {
			return onActionMove(motionEvent);
		} else if (action == MotionEvent.ACTION_UP) {
			return onActionUp(motionEvent);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			return onActionCancel();
		} else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			return onActionPointerDown(motionEvent);
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			return onActionPointerUp(motionEvent);
		}

		// the event was not handled
		return false;
	}
}
