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

/**
 * Implementation for single-touch capable devices.
 */
public class SingleTouchHandler extends TouchEventHandler {

	SingleTouchHandler(Context context, MapView mapView) {
		super(context, mapView);
	}

	@Override
	public int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction();
	}

	private boolean onActionCancel() {
		this.longPressDetector.pressStop();
		return true;
	}

	private boolean onActionDown(MotionEvent motionEvent) {
		this.longPressDetector.pressStart();
		// save the position of the event
		this.previousPositionX = motionEvent.getX();
		this.previousPositionY = motionEvent.getY();
		this.moveThresholdReached = false;
		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		// calculate the distance between previous and current position
		float moveX = motionEvent.getX() - this.previousPositionX;
		float moveY = motionEvent.getY() - this.previousPositionY;

		if (!this.moveThresholdReached) {
			if (Math.abs(moveX) > this.mapMoveDelta || Math.abs(moveY) > this.mapMoveDelta) {
				// the map movement threshold has been reached
				this.longPressDetector.pressStop();
				this.moveThresholdReached = true;

				// save the position of the event
				this.previousPositionX = motionEvent.getX();
				this.previousPositionY = motionEvent.getY();
			}
			return true;
		}

		// save the position of the event
		this.previousPositionX = motionEvent.getX();
		this.previousPositionY = motionEvent.getY();

		this.mapView.getFrameBuffer().matrixPostTranslate(moveX, moveY);
		this.mapView.getMapPosition().moveMap(moveX, moveY);
		this.mapView.redrawTiles();
		return true;
	}

	private boolean onActionUp(MotionEvent motionEvent) {
		this.longPressDetector.pressStop();
		if (this.moveThresholdReached || this.longPressDetector.isEventHandled()) {
			this.previousEventTap = false;
		} else {
			if (this.previousEventTap) {
				// calculate the distance to the previous tap position
				float tapDiffX = Math.abs(motionEvent.getX() - this.previousTapX);
				float tapDiffY = Math.abs(motionEvent.getY() - this.previousTapY);
				long tapDiffTime = motionEvent.getEventTime() - this.previousTapTime;

				// check if a double-tap event occurred
				if (tapDiffX < this.doubleTapDelta && tapDiffY < this.doubleTapDelta
						&& tapDiffTime < this.doubleTapTimeout) {
					// double-tap event
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
			this.previousTapX = motionEvent.getX();
			this.previousTapY = motionEvent.getY();
			this.previousTapTime = motionEvent.getEventTime();

			GeoPoint tapPoint = this.mapView.getProjection().fromPixels((int) motionEvent.getX(),
					(int) motionEvent.getY());
			synchronized (this.mapView.getOverlays()) {
				for (int i = this.mapView.getOverlays().size() - 1; i >= 0; --i) {
					if (this.mapView.getOverlays().get(i).onTap(tapPoint, this.mapView)) {
						// the tap event has been handled
						break;
					}
				}
			}
		}
		return true;
	}

	@Override
	boolean handleMotionEvent(MotionEvent motionEvent) {
		if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
			return onActionDown(motionEvent);
		} else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
			return onActionMove(motionEvent);
		} else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
			return onActionUp(motionEvent);
		} else if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
			return onActionCancel();
		}

		// the event was not handled
		return false;
	}
}
