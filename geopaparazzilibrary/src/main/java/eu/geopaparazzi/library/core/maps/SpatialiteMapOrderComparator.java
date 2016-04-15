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

package eu.geopaparazzi.library.core.maps;

import java.util.Comparator;

/**
 * Class to order SpatialiteMap.
 */
public class SpatialiteMapOrderComparator implements Comparator<SpatialiteMap> {
    @Override
    public int compare(SpatialiteMap m1, SpatialiteMap m2) {
        if (m1.order < m2.order) {
            return -1;
        } else if (m1.order > m2.order) {
            return 1;
        } else {
            return 0;
        }
    }
}
