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
import org.mapsforge.android.maps.PausableThread;

import android.os.SystemClock;

/**
 * A ZoomAnimator handles the zoom-in and zoom-out animations of the corresponding MapView. It runs in a separate thread
 * to avoid blocking the UI thread.
 */
public class ZoomAnimator extends PausableThread {
	private static final int DEFAULT_DURATION = 250;
	private static final int FRAME_LENGTH_IN_MS = 15;
	private static final String THREAD_NAME = "ZoomAnimator";

	private boolean executeAnimation;
	private MapView mapView;
	private float pivotX;
	private float pivotY;
	private float scaleFactorApplied;
	private long timeStart;
	private float zoomDifference;
	private float zoomEnd;
	private float zoomStart;

	/**
	 * @param mapView
	 *            the MapView whose zoom level changes should be animated.
	 */
	public ZoomAnimator(MapView mapView) {
		super();
		this.mapView = mapView;
	}

	/**
	 * @return true if the ZoomAnimator is working, false otherwise.
	 */
	public boolean isExecuting() {
		return this.executeAnimation;
	}

	/**
	 * Sets the parameters for the zoom animation.
	 * 
	 * @param zoomStart
	 *            the zoom factor at the begin of the animation.
	 * @param zoomEnd
	 *            the zoom factor at the end of the animation.
	 * @param pivotX
	 *            the x coordinate of the animation center.
	 * @param pivotY
	 *            the y coordinate of the animation center.
	 */
	public void setParameters(float zoomStart, float zoomEnd, float pivotX, float pivotY) {
		this.zoomStart = zoomStart;
		this.zoomEnd = zoomEnd;
		this.pivotX = pivotX;
		this.pivotY = pivotY;
	}

	/**
	 * Starts a zoom animation with the current parameters.
	 */
	public void startAnimation() {
		this.zoomDifference = this.zoomEnd - this.zoomStart;
		this.scaleFactorApplied = this.zoomStart;
		this.executeAnimation = true;
		this.timeStart = SystemClock.uptimeMillis();
		synchronized (this) {
			notify();
		}
	}

	@Override
	protected void doWork() throws InterruptedException {
		// calculate the elapsed time
		long timeElapsed = SystemClock.uptimeMillis() - this.timeStart;
		float timeElapsedPercent = Math.min(1, timeElapsed / (float) DEFAULT_DURATION);

		// calculate the zoom and scale values at the current moment
		float currentZoom = this.zoomStart + timeElapsedPercent * this.zoomDifference;
		float scaleFactor = currentZoom / this.scaleFactorApplied;
		this.scaleFactorApplied *= scaleFactor;
		this.mapView.getFrameBuffer().matrixPostScale(scaleFactor, scaleFactor, this.pivotX, this.pivotY);

		// check if the animation time is over
		if (timeElapsed >= DEFAULT_DURATION) {
			this.executeAnimation = false;
			this.mapView.redrawTiles();
			onZoomFinish();
		} else {
			this.mapView.postInvalidate();
			sleep(FRAME_LENGTH_IN_MS);
		}
	}

	@Override
	protected void afterRun() {
		this.mapView = null;
 	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected boolean hasWork() {
		return this.executeAnimation;
	}
	
	void onZoomFinish() {
		//Call to requestRedraw for overlays to cancel current drawing and redraw
		for (int i = 0, n = this.mapView.getOverlays().size(); i < n; ++i) {
			this.mapView.getOverlays().get(i).requestRedraw();
		}
	}
}
