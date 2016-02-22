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

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;

/**
 * Overlay is the abstract base class for all types of overlays. It handles the lifecycle of the overlay thread and
 * implements those parts of the redrawing process which all overlays have in common.
 * <p>
 * To add an overlay to a <code>MapView</code>, create a subclass of this class and add an instance to the list returned
 * by {@link MapView#getOverlays()}. When an overlay gets removed from the list, the corresponding thread is
 * automatically interrupted and all its resources are freed. Re-adding a previously removed overlay to the list will
 * therefore cause an {@link IllegalThreadStateException}.
 */
public abstract class Overlay extends Thread {
	/**
	 * Enumeration of all types of events.
	 */
	protected enum EventType {
		/**
		 * A long press event.
		 * 
		 * @see Overlay#onLongPress(GeoPoint, MapView)
		 */
		LONG_PRESS,

		/**
		 * A tap event.
		 * 
		 * @see Overlay#onTap(GeoPoint, MapView)
		 */
		TAP;
	}

	private static final String THREAD_NAME = "Overlay";

	/**
	 * Flag which is set whenever the MapView dimensions have been changed.
	 */
	private boolean changedSize;

	/**
	 * Flag to indicate if the overlay has a positive width and height.
	 */
	private boolean hasValidDimensions;

	/**
	 * Transformation matrix for the overlay.
	 */
	private final Matrix matrix;

	/**
	 * Used to calculate the scale of the transformation matrix.
	 */
	private float matrixScaleFactor;

	/**
	 * First internal bitmap for the overlay to draw on.
	 */
	private Bitmap overlayBitmap1;

	/**
	 * Second internal bitmap for the overlay to draw on.
	 */
	private Bitmap overlayBitmap2;

	/**
	 * Canvas that is used in the overlay for drawing.
	 */
	private final Canvas overlayCanvas;

	/**
	 * Stores the top-left map position at which the redraw should happen.
	 */
	private final Point point;

	/**
	 * Stores the map position before drawing starts.
	 */
	private Point positionAfterDraw;

	/**
	 * Stores the map position after drawing is finished.
	 */
	private Point positionBeforeDraw;

	/**
	 * Flag to indicate if the overlay should redraw itself.
	 */
	private boolean redraw;

	/**
	 * Reference to the MapView instance.
	 */
	protected MapView internalMapView;

	/**
	 * Default constructor which must be called by all subclasses.
	 */
	protected Overlay() {
		super();
		this.overlayCanvas = new Canvas();
		this.matrix = new Matrix();
		this.point = new Point();
		this.positionBeforeDraw = new Point();
		this.positionAfterDraw = new Point();
	}

	/**
	 * Draws the overlay on the given canvas.
	 * 
	 * @param canvas
	 *            the canvas on which the overlay should be drawn.
	 */
	public final void draw(Canvas canvas) {
		synchronized (this.matrix) {
			if (this.overlayBitmap1 != null) {
				canvas.drawBitmap(this.overlayBitmap1, this.matrix, null);
			}
		}
	}

	/**
	 * @param scaleX
	 *            the horizontal scale.
	 * @param scaleY
	 *            the vertical scale.
	 * @param pivotX
	 *            the horizontal pivot point.
	 * @param pivotY
	 *            the vertical pivot point.
	 */
	public final void matrixPostScale(float scaleX, float scaleY, float pivotX, float pivotY) {
		synchronized (this.matrix) {
			this.matrix.postScale(scaleX, scaleY, pivotX, pivotY);
		}
	}

	/**
	 * @param translateX
	 *            the horizontal translation.
	 * @param translateY
	 *            the vertical translation.
	 */
	public final void matrixPostTranslate(float translateX, float translateY) {
		synchronized (this.matrix) {
			this.matrix.postTranslate(translateX, translateY);
		}
	}

	/**
	 * Handles a long press event. A long press event is only triggered if the map was not moved. A return value of true
	 * indicates that the long press event has been handled by this overlay and stops its propagation to other overlays.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 * 
	 * @param geoPoint
	 *            the point which has been long pressed.
	 * @param mapView
	 *            the {@link MapView} that triggered the long press event.
	 * @return true if the long press event was handled, false otherwise.
	 */
	public boolean onLongPress(GeoPoint geoPoint, MapView mapView) {
		return false;
	}

	/**
	 * Marks the current dimensions of the overlay as dirty.
	 */
	public final void onSizeChanged() {
		synchronized (this) {
			this.changedSize = true;
			notify();
		}
	}

	/**
	 * Handles a tap event. A tap event is only triggered if the map was not moved and no long press event was handled
	 * within the same gesture. A return value of true indicates that the tap event has been handled by this overlay and
	 * stops its propagation to other overlays.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 * 
	 * @param geoPoint
	 *            the point which has been tapped.
	 * @param mapView
	 *            the {@link MapView} that triggered the tap event.
	 * @return true if the tap event was handled, false otherwise.
	 */
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		return false;
	}

	/**
	 * Requests a redraw of this overlay.
	 */
	public final void requestRedraw() {
		synchronized (this) {
			this.redraw = true;
			notify();
		}
	}

	@Override
	public final void run() {
		setName(getThreadName());

		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && !this.changedSize && !this.redraw) {
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

			if (this.changedSize) {
				changeSize();
			}

			if (this.redraw) {
				redrawOverlay();
			}
		}

		// help the GC
		internalMapView = null;

		// free the overlay bitmaps memory
		if (this.overlayBitmap1 != null) {
			this.overlayBitmap1.recycle();
			this.overlayBitmap1 = null;
		}

		if (this.overlayBitmap2 != null) {
			this.overlayBitmap2.recycle();
			this.overlayBitmap2 = null;
		}
	}

	/**
	 * This method is called by the MapView once on each new overlay.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	public final void setupOverlay(MapView mapView) {
		if (isInterrupted() || !isAlive()) {
			throw new IllegalThreadStateException("overlay thread already destroyed");
		}
		this.internalMapView = mapView;
		onSizeChanged();
	}

	private void redrawOverlay() {
		this.redraw = false;

		if (!this.hasValidDimensions) {
			// there is no area to draw on
			return;
		}

		Projection mapViewProjection = this.internalMapView.getProjection();

		// clear the second bitmap and make the canvas use it
		this.overlayBitmap2.eraseColor(Color.TRANSPARENT);
		this.overlayCanvas.setBitmap(this.overlayBitmap2);

		// workaround for http://code.google.com/p/skia/issues/detail?id=387
		this.overlayCanvas.setMatrix(this.overlayCanvas.getMatrix());

		// save the zoom level and map position before drawing
		byte zoomLevelBeforeDraw;
		synchronized (this.internalMapView) {
			zoomLevelBeforeDraw = this.internalMapView.getMapPosition().getZoomLevel();
			this.positionBeforeDraw = mapViewProjection.toPoint(this.internalMapView.getMapPosition().getMapCenter(),
					this.positionBeforeDraw, zoomLevelBeforeDraw);
		}

		// calculate the top-left point of the visible rectangle
		this.point.x = this.positionBeforeDraw.x - (this.overlayCanvas.getWidth() >> 1);
		this.point.y = this.positionBeforeDraw.y - (this.overlayCanvas.getHeight() >> 1);

		if (isInterrupted() || sizeHasChanged() || needRedraw()) {
			// stop working
			return;
		}

		// call the draw implementation of the subclass
		drawOverlayBitmap(this.overlayCanvas, this.point, mapViewProjection, zoomLevelBeforeDraw);

		if (isInterrupted() || sizeHasChanged() || needRedraw()) {
			// stop working
			return;
		}

		// save the zoom level and map position after drawing
		byte zoomLevelAfterDraw;
		synchronized (this.internalMapView) {
			zoomLevelAfterDraw = this.internalMapView.getMapPosition().getZoomLevel();
			this.positionAfterDraw = mapViewProjection.toPoint(this.internalMapView.getMapPosition().getMapCenter(),
					this.positionAfterDraw, zoomLevelBeforeDraw);
		}

		if (this.internalMapView.isZoomAnimatorRunning()) {
			// do not disturb the ongoing animation
			return;
		}

		// adjust the transformation matrix of the overlay
		synchronized (this.matrix) {
			this.matrix.reset();
			this.matrix.postTranslate(this.positionBeforeDraw.x - this.positionAfterDraw.x, this.positionBeforeDraw.y
					- this.positionAfterDraw.y);

			byte zoomLevelDiff = (byte) (zoomLevelAfterDraw - zoomLevelBeforeDraw);
			if (zoomLevelDiff > 0) {
				// zoom level has increased
				this.matrixScaleFactor = 1 << zoomLevelDiff;
				this.matrix.postScale(this.matrixScaleFactor, this.matrixScaleFactor,
						this.overlayCanvas.getWidth() >> 1, this.overlayCanvas.getHeight() >> 1);
			} else if (zoomLevelDiff < 0) {
				// zoom level has decreased
				this.matrixScaleFactor = 1.0f / (1 << -zoomLevelDiff);
				this.matrix.postScale(this.matrixScaleFactor, this.matrixScaleFactor,
						this.overlayCanvas.getWidth() >> 1, this.overlayCanvas.getHeight() >> 1);
			}

			// swap the two overlay bitmaps
			Bitmap overlayBitmapSwap = this.overlayBitmap1;
			this.overlayBitmap1 = this.overlayBitmap2;
			this.overlayBitmap2 = overlayBitmapSwap;
		}

		if (isInterrupted() || sizeHasChanged() || needRedraw()) {
			// stop working
			return;
		}

		// request the MapView to redraw
		this.internalMapView.postInvalidate();
	}

	/**
	 * Draws the overlay on the canvas. All subclasses need to implement this method.
	 * 
	 * @param canvas
	 *            the canvas to draw the overlay on.
	 * @param drawPosition
	 *            the top-left position of the map relative to the world map.
	 * @param projection
	 *            the projection to be used for the drawing process.
	 * @param drawZoomLevel
	 *            the zoom level of the map.
	 */
	protected abstract void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection,
			byte drawZoomLevel);

	/**
	 * Returns the name of the overlay implementation. It will be used as the name for the overlay thread. Subclasses
	 * should override this method to provide a more specific name.
	 * 
	 * @return the name of the overlay implementation.
	 */
	protected String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Changes the size of the overlay according to the MapView dimensions.
	 */
	public final void changeSize() {
		this.changedSize = false;

		// check if the previous overlay bitmaps must be recycled
		if (this.overlayBitmap1 != null) {
			this.overlayBitmap1.recycle();
			this.overlayBitmap1 = null;
		}
		if (this.overlayBitmap2 != null) {
			this.overlayBitmap2.recycle();
			this.overlayBitmap2 = null;
		}

		// check if the new dimensions are positive
		int width = this.internalMapView.getWidth();
		int height = this.internalMapView.getHeight();
		// Log.i("OVERLAY", "new Bitmap size" +width + "/" + height);
		if (width > 0 && height > 0) {
			// create the two overlay bitmaps with the correct dimensions
			this.overlayBitmap1 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.overlayBitmap2 = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			this.redraw = true;
			this.hasValidDimensions = true;
		} else {
			this.hasValidDimensions = false;
		}
	}

	/**
	 * @return true if the dimensions of the overlay have changed, false otherwise.
	 */
	public boolean sizeHasChanged() {
		return this.changedSize;
	}

	/**
	 * @return true if the overlay need to be redraw
	 */
	public boolean needRedraw() {
		return this.redraw;
	}
	
	public void dispose() {
		if (this.overlayBitmap1 != null) {
			this.overlayBitmap1.recycle();
			this.overlayBitmap1 = null;
		}
		if (this.overlayBitmap2 != null) {
			this.overlayBitmap2.recycle();
			this.overlayBitmap2 = null;
		}
	}
}
