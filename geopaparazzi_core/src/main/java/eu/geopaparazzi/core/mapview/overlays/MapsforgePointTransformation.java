/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.geopaparazzi.core.mapview.overlays;

import android.graphics.Point;
import android.graphics.PointF;

import com.vividsolutions.jts.android.PointTransformation;
import com.vividsolutions.jts.geom.Coordinate;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.model.GeoPoint;

/**
 * Transformation that handles mapsforge transforms.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgePointTransformation implements PointTransformation {
    private byte drawZoom;
    private Projection projection;
    private final Point tmpPoint = new Point();
    private Point drawPosition;

    /**
     * Constructor.
     * 
     * @param projection projection.
     * @param drawPosition the position.
     * @param drawZoom the zoom level.
     */
    public MapsforgePointTransformation( Projection projection, Point drawPosition, byte drawZoom ) {
        this.projection = projection;
        this.drawPosition = drawPosition;
        this.drawZoom = drawZoom;
    }

    public void transform( Coordinate model, PointF view ) {
        projection.toPoint(new GeoPoint(model.y, model.x), tmpPoint, drawZoom);
        view.set(tmpPoint.x - drawPosition.x, tmpPoint.y - drawPosition.y);
    }
}