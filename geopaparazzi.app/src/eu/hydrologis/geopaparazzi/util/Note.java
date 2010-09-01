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
 * Represents a note (log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Note {
    private final String name;
    private final String description;
    private final double lon;
    private final double lat;
    private final double altim;

    /**
     * A wrapper for a note.
     * 
     * @param name the text of the note.
     * @param description a description or the date if available.
     * @param lon
     * @param lat
     * @param altim
     */
    public Note( String name, String description, double lon, double lat , double altim) {
        if (name != null) {
            this.name = name;
        } else {
            this.name = ""; //$NON-NLS-1$
        }
        if (description != null) {
            this.description = description;
        } else {
            this.description = ""; //$NON-NLS-1$
        }
        this.lon = lon;
        this.lat = lat;
        this.altim = altim;
    }
    
    public double getLat() {
        return lat;
    }
    
    public double getLon() {
        return lon;
    }
    
    public double getAltim() {
        return altim;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    @SuppressWarnings("nls")
    public String toKmlString() {
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        sB.append("<styleUrl>#red-pushpin</styleUrl>\n");
        sB.append("<name>").append(name).append("</name>\n");
        sB.append("<description>\n");
        sB.append("<![CDATA[\n");
        sB.append("<p>").append(description).append("</p>\n");
        sB.append("]]>\n");
        sB.append("</description>\n");
        sB.append("<gx:balloonVisibility>1</gx:balloonVisibility>\n");
        sB.append("<Point>\n");
        sB.append("<coordinates>").append(lon).append(",").append(lat).append(",0</coordinates>\n");
        sB.append("</Point>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }
}
