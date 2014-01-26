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

import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Represents a bookmark.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Bookmark implements KmlRepresenter {
    private String name;
    private double lon;
    private double lat;
    private long id;
    private double zoom;
    private double north;
    private double south;
    private double west;
    private double east;

    /**
     * A wrapper for a Bookmark.
     * 
     * @param id the id
     * @param name the name of the Bookmark.
     * @param lon lon
     * @param lat lat
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

    /**
     * Constructor.
     * 
     * @param id id
     * @param name name
     * @param lon lon
     * @param lat lat
     * @param zoom zoom
     * @param north north
     * @param south south
     * @param west west 
     * @param east east
     */
    public Bookmark( long id, String name, double lon, double lat, double zoom, double north, double south, double west,
            double east ) {
        this.id = id;
        this.zoom = zoom;
        this.north = north;
        this.south = south;
        this.west = west;
        this.east = east;
        if (name != null) {
            this.name = name;
        } else {
            this.name = ""; //$NON-NLS-1$
        }
        this.lon = lon;
        this.lat = lat;
    }

    @SuppressWarnings("nls")
    public String toKmlString() throws Exception {
        String name = Utilities.makeXmlSafe(this.name);
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        // sB.append("<styleUrl>#red-pushpin</styleUrl>\n");
        sB.append("<styleUrl>#bookmark-icon</styleUrl>\n");
        sB.append("<name>").append(name).append("</name>\n");
        sB.append("<description>\n");
        sB.append(name);
        sB.append("</description>\n");
        sB.append("<gx:balloonVisibility>1</gx:balloonVisibility>\n");
        sB.append("<Point>\n");
        sB.append("<coordinates>").append(lon).append(",").append(lat).append(",0</coordinates>\n");
        sB.append("</Point>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @return lat.
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return lon.
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the zoomlevel.
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * @return the north.
     */
    public double getNorth() {
        return north;
    }

    /**
     * @return teh south.
     */
    public double getSouth() {
        return south;
    }

    /**
     * @return the west.
     */
    public double getWest() {
        return west;
    }

    /**
     * @return the east.
     */
    public double getEast() {
        return east;
    }

    @SuppressWarnings("nls")
    public String toString() {
        return "Bookmark [name=" + name + ", lon=" + lon + ", lat=" + lat + ", id=" + id + ", zoom=" + zoom + ", north=" + north
                + ", south=" + south + ", west=" + west + ", east=" + east + "]";
    }

    public boolean hasImages() {
        return false;
    }

    public List<String> getImagePaths() {
        return Collections.emptyList();
    }
}
