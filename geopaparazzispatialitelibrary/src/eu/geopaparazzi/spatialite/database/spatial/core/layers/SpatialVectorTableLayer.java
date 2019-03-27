/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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
package eu.geopaparazzi.spatialite.database.spatial.core.layers;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;

import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

/**
 * A layer wrapper for the {@link eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialVectorTableLayer extends Layer {
    private SpatialVectorTable spatialVectorTable;

    /**
     * Constructor.
     * 
     * @param spatialVectorTable the table to wrap.
     */
    public SpatialVectorTableLayer( SpatialVectorTable spatialVectorTable ) {
        this.spatialVectorTable = spatialVectorTable;
    }

    /**
     * @return the wrapped table.
     */
    public SpatialVectorTable getSpatialVectorTable() {
        return spatialVectorTable;
    }

    public boolean isPolygon() {
        return spatialVectorTable.isPolygon();
    }

    public boolean isLine() {
        return spatialVectorTable.isLine();
    }

    public boolean isPoint() {
        return spatialVectorTable.isPoint();
    }

    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
    // TODO fixem
    }
}
