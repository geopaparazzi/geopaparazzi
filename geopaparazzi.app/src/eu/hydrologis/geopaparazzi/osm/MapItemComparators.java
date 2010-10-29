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
package eu.hydrologis.geopaparazzi.osm;

import java.util.Comparator;

/**
 * {@link MapItem} comparators to sort them.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapItemComparators {

    /**
     * Sorts {@link MapItem}s by name. 
     */
    public static class MapItemNameComparator implements Comparator<MapItem> {
        private boolean doInverse = false;
        public MapItemNameComparator() {
        }
        public MapItemNameComparator( boolean doInverse ) {
            this.doInverse = doInverse;
        }
        public int compare( MapItem m1, MapItem m2 ) {
            int compareTo = m1.getName().compareTo(m2.getName());
            if (compareTo == 0) {
                return 0;
            }
            if (doInverse) {
                return -1 * compareTo;
            } else {
                return compareTo;
            }
        }
    }

    /**
     * Sorts {@link MapItem}s by id, which is equivalent to time order. 
     */
    public static class MapItemIdComparator implements Comparator<MapItem> {
        private boolean doInverse = false;
        public MapItemIdComparator() {
        }
        public MapItemIdComparator( boolean doInverse ) {
            this.doInverse = doInverse;
        }
        public int compare( MapItem m1, MapItem m2 ) {
            long id1 = m1.getId();
            long id2 = m2.getId();
            if (id1 < id2) {
                return doInverse ? 1 : -1;
            } else if (id1 > id2) {
                return doInverse ? -1 : 1;
            } else {
                return 0;
            }
        }
    }

}
