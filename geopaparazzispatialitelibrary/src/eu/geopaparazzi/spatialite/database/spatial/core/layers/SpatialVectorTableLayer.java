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

import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

/**
 * A layer wrapper for the {@link eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialVectorTableLayer implements ILayer {
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

}
