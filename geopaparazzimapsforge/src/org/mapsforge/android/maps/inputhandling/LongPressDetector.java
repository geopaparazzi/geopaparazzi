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

import android.os.SystemClock;

class LongPressDetector extends Thread {
	private static final String THREAD_NAME = "LongPressDetector";

	private boolean eventHandled;
	private long pressStartTime;
	private TouchEventHandler touchEventHandler;

	LongPressDetector(TouchEventHandler touchEventHandler) {
		super();
		this.touchEventHandler = touchEventHandler;
	}

	@Override
	public void run() {
		setName(THREAD_NAME);

		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && this.pressStartTime == 0) {
					try {
						wait();
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			if (isInterrupted()) {
				break;
			}

			synchronized (this) {
				// calculate the elapsed time since the press has started
				long timeElapsed = SystemClock.uptimeMillis() - this.pressStartTime;
				while (!isInterrupted() && this.pressStartTime > 0
						&& timeElapsed < this.touchEventHandler.longPressTimeout) {
					try {
						// wait for the remaining time of the whole timeout
						wait(this.touchEventHandler.longPressTimeout - timeElapsed);
						timeElapsed = SystemClock.uptimeMillis() - this.pressStartTime;
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			if (isInterrupted()) {
				break;
			}

			if (this.pressStartTime > 0) {
				this.eventHandled = this.touchEventHandler.forwardLongPressEvent();
				// stop even if a new long press event has already been started
				pressStop();
			}
		}
		touchEventHandler = null;
	}

	/**
	 * @return true if a long press event has been handled, false otherwise.
	 */
	boolean isEventHandled() {
		return this.eventHandled;
	}

	/**
	 * Informs the LongTapDetector that a potential long press event has started.
	 */
	void pressStart() {
		if (this.pressStartTime == 0) {
			this.eventHandled = false;
			this.pressStartTime = SystemClock.uptimeMillis();
			synchronized (this) {
				notify();
			}
		}
	}

	/**
	 * Informs the LongTapDetector that no long press event is happening.
	 */
	void pressStop() {
		if (this.pressStartTime > 0) {
			this.pressStartTime = 0;
			synchronized (this) {
				notify();
			}
		}
	}
}
