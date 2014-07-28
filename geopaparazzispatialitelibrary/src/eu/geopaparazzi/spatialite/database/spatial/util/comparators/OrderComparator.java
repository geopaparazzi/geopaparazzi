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
package eu.geopaparazzi.spatialite.database.spatial.util.comparators;

import java.util.Comparator;

import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;

/**
 * Comparator for layers order.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OrderComparator implements Comparator<SpatialVectorTable> {

    @Override
    public int compare( SpatialVectorTable t1, SpatialVectorTable t2 ) {
        if (t1.getStyle() == null) {
            t1.makeDefaultStyle();
        }
        if (t2.getStyle() == null) {
            t2.makeDefaultStyle();
        }

        if (t1.getStyle().order < t2.getStyle().order) {
            return -1;
        } else if (t1.getStyle().order > t2.getStyle().order) {
            return 1;
        } else
            return 0;
    }

}
