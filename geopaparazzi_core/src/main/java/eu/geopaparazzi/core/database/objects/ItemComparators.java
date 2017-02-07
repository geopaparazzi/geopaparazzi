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
package eu.geopaparazzi.core.database.objects;

import java.util.Comparator;

import eu.geopaparazzi.library.database.ANote;
import eu.geopaparazzi.library.util.PointF3D;

/**
 * {@link MapItem} comparators to sort them.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ItemComparators {

    /**
     * Sorts {@link MapItem}s by name.
     */
    public static class MapItemNameComparator implements Comparator<MapItem> {
        private boolean doInverse = false;

        /**
         *
         */
        public MapItemNameComparator() {
        }

        /**
         * @param doInverse invert comparator.
         */
        public MapItemNameComparator(boolean doInverse) {
            this.doInverse = doInverse;
        }

        public int compare(MapItem m1, MapItem m2) {
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

        /**
         *
         */
        public MapItemIdComparator() {
        }

        /**
         * @param doInverse invert comparator.
         */
        public MapItemIdComparator(boolean doInverse) {
            this.doInverse = doInverse;
        }

        public int compare(MapItem m1, MapItem m2) {
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

    /**
     * Sorts {@link Bookmark}s by text.
     */
    public static class BookmarksComparator implements Comparator<Bookmark> {
        private boolean doInverse = false;

        /**
         *
         */
        public BookmarksComparator() {
        }

        /**
         * @param doInverse invert comparator.
         */
        public BookmarksComparator(boolean doInverse) {
            this.doInverse = doInverse;
        }

        public int compare(Bookmark m1, Bookmark m2) {
            String id1 = m1.getName();
            String id2 = m2.getName();

            int compareTo = id1.compareTo(id2);

            if (compareTo < 0) {
                return doInverse ? 1 : -1;
            } else if (compareTo > 0) {
                return doInverse ? -1 : 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Sort notes by id.
     */
    public static class NotesComparator implements Comparator<ANote> {
        private boolean doInverse = false;

        /**
         *
         */
        public NotesComparator() {
        }

        /**
         * @param doInverse invert comparator.
         */
        public NotesComparator(boolean doInverse) {
            this.doInverse = doInverse;
        }

        public int compare(ANote m1, ANote m2) {
            String id1 = m1.getName();
            String id2 = m2.getName();

            int compareTo = id1.compareTo(id2);

            if (compareTo < 0) {
                return doInverse ? 1 : -1;
            } else if (compareTo > 0) {
                return doInverse ? -1 : 1;
            } else {
                return 0;
            }
        }
    }

    public static class NotesDistanceFromPointComparator implements Comparator<ANote> {
        private boolean doInverse = false;
        private PointF3D p;

        /**
         *
         */
        public NotesDistanceFromPointComparator(double lon, double lat) {
            p = new PointF3D((float) lon, (float) lat);
        }

        /**
         * @param doInverse invert comparator.
         */
        public NotesDistanceFromPointComparator(double lon, double lat, boolean doInverse) {
            p = new PointF3D((float) lon, (float) lat);
            this.doInverse = doInverse;
        }

        public int compare(ANote m1, ANote m2) {
            float lon1 = (float) m1.getLon();
            float lat1 = (float) m1.getLat();
            float lon2 = (float) m2.getLon();
            float lat2 = (float) m2.getLat();

            PointF3D p1 = new PointF3D(lon1, lat1);
            PointF3D p2 = new PointF3D(lon2, lat2);
            float d1 = p.distance2d(p1);
            float d2 = p.distance2d(p2);

            if (d1 < d2) {
                return doInverse ? 1 : -1;
            } else if (d1 > d2) {
                return doInverse ? -1 : 1;
            } else {
                return 0;
            }
        }
    }

}
