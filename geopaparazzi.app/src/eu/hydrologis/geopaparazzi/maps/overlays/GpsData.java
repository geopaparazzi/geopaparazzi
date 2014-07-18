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
package eu.hydrologis.geopaparazzi.maps.overlays;

import android.graphics.Paint;
import android.graphics.Point;

import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.model.GeoPoint;

/**
 * GpsData holds all parameters of a single circle on a Gpsoverlay. All rendering parameters like
 * color, stroke width, pattern and transparency can be configured via two {@link Paint} objects. Each circle is drawn
 * twice - once with each paint object - to allow for different outlines and fillings. The drawing quality can be
 * improved by enabling {@link Paint#setAntiAlias(boolean) anti-aliasing}.
 */
public class GpsData {
    /**
     * Geographical coordinate of the circle.
     */
    protected GeoPoint center;

    /**
     * Paint which will be used to fill the circle.
     */
    protected Paint paintFill;

    /**
     * Paint which will be used to draw the circle outline.
     */
    protected Paint paintOutline;

    /**
     * Radius of the circle in meters.
     */
    protected float radius;

    /**
     * Title of the circle.
     */
    protected String title;

    /**
     * Cached position of the circle on the map.
     */
    Point cachedCenterPosition;

    /**
     * Cached radius of the circle in pixels.
     */
    float cachedRadius;

    /**
     * Zoom level of the cached circle position.
     */
    byte cachedZoomLevel;

    /**
     * Flag to indicate if at least one paint is set for this circle.
     */
    boolean hasPaint;

    /**
     * Constructs a new GpsData.
     */
    public GpsData() {
        this(null, 0, null, null, null);
    }

    /**
     * @param center
     *            the geographical coordinates of the center point.
     * @param radius
     *            the radius of the circle in meters.
     * @param paintFill
     *            the paint which will be used to fill the circle (may be null).
     * @param paintOutline
     *            the paint which will be used to draw the circle outline (may be null).
     * @param title
     *            the title of the circle (may be null).
     */
    public GpsData( GeoPoint center, float radius, Paint paintFill, Paint paintOutline, String title ) {
        this.title = title;
        this.cachedCenterPosition = new Point();
        this.cachedZoomLevel = Byte.MIN_VALUE;
        setCircleDataInternal(center, radius);
        setPaintInternal(paintFill, paintOutline);
    }

    /**
     * @param center
     *            the geographical coordinates of the center point.
     * @param radius
     *            the radius of the circle in meters.
     * @param title
     *            the title of the circle (may be null).
     */
    public GpsData( GeoPoint center, float radius, String title ) {
        this(center, radius, null, null, title);
    }

    /**
     * @param paintFill
     *            the paint which will be used to fill the circle (may be null).
     * @param paintOutline
     *            the paint which will be used to draw the circle outline (may be null).
     */
    public GpsData( Paint paintFill, Paint paintOutline ) {
        this(null, 0, paintFill, paintOutline, null);
    }

    /**
     * @return the title of this circle (may be null).
     */
    public synchronized String getTitle() {
        return this.title;
    }

    /**
     * Sets the parameters of the circle.
     * <p>
     * Changes might not become visible until {@link Overlay#requestRedraw()} is called.
     * 
     * @param center
     *            the geographical coordinates of the center point.
     * @param radius
     *            the radius of the circle in meters.
     */
    public synchronized void setCircleData( GeoPoint center, float radius ) {
        setCircleDataInternal(center, radius);
    }

    /**
     * Sets the paints which will be used to draw the overlay.
     * <p>
     * Changes might not become visible until {@link Overlay#requestRedraw()} is called.
     * 
     * @param paintFill
     *            the paint which will be used to fill the circle (may be null).
     * @param paintOutline
     *            the paint which will be used to draw the circle outline (may be null).
     */
    public synchronized void setPaint( Paint paintFill, Paint paintOutline ) {
        setPaintInternal(paintFill, paintOutline);
    }

    /**
     * Sets the title of this circle.
     * 
     * @param title
     *            the title of this circle (may be null).
     */
    public synchronized void setTitle( String title ) {
        this.title = title;
    }

    private void setCircleDataInternal( GeoPoint center, float radius ) {
        this.center = center;
        this.radius = radius;
        this.cachedZoomLevel = Byte.MIN_VALUE;
    }

    private void setPaintInternal( Paint paintFill, Paint paintOutline ) {
        this.paintFill = paintFill;
        this.paintOutline = paintOutline;
        this.hasPaint = paintFill != null || paintOutline != null;
    }
}
