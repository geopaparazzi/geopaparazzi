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

package eu.geopaparazzi.mapsforge.core.proj;

import android.graphics.PointF;

import org.locationtech.jts.android.PointTransformation;
import org.locationtech.jts.geom.Coordinate;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.util.MapViewProjection;


/**
 * Transformation that handles mapsforge transforms.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgePointTransformation implements PointTransformation {
    private byte drawZoom;
    private int tileSize;
    private MapViewProjection projection;
    private Point drawPosition;

    /**
     * Constructor.
     *
     * @param projection   projection.
     * @param drawPosition the position.
     * @param drawZoom     the zoom level.
     */
    public MapsforgePointTransformation(MapViewProjection projection, Point drawPosition, byte drawZoom, int tileSize) {
        this.projection = projection;
        this.drawPosition = drawPosition;
        this.drawZoom = drawZoom;
        this.tileSize = tileSize;
    }

    public void transform(Coordinate model, PointF view) {
        org.mapsforge.core.model.Point point = toPoint(new LatLong(model.y, model.x), drawZoom);
        view.set((float) (point.x - drawPosition.x), (float) (point.y - drawPosition.y));
    }

    public org.mapsforge.core.model.Point toPoint(LatLong in, byte zoom) {
        // create a new point and return it
        return new org.mapsforge.core.model.Point((int) MercatorProjection.longitudeToPixelX(in.longitude, zoom, tileSize),
                (int) MercatorProjection.latitudeToPixelY(in.latitude, zoom, tileSize));
    }
}