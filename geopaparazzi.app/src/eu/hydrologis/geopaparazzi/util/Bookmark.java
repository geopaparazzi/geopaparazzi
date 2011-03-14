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
package eu.hydrologis.geopaparazzi.util;

/**
 * Represents a bookmark.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Bookmark {
    private final String name;
    private final double lon;
    private final double lat;
    private final long id;

    /**
     * A wrapper for a Bookmark.
     * 
     * @param id 
     * @param name the name of the Bookmark.
     * @param lon
     * @param lat
     */
    public Bookmark( long id, String name, double lon, double lat ) {
        this.id = id;
        if (name != null) {
            this.name = name;
        } else {
            this.name = ""; //$NON-NLS-1$
        }
        this.lon = lon;
        this.lat = lat;
    }

    public long getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

}
