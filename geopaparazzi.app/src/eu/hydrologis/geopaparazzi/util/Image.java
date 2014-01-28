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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Represents an image.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Image implements INote, KmlRepresenter {
    /**
     * 
     */
    public static final String IMAGE_NOTE = "image note";
    private String name;
    private final long id;
    private final double lon;
    private final double lat;
    private final double altim;
    private final double azim;
    private final String path;
    private final String ts;

    /**
     * A wrapper for an image.
     * 
     * @param id the image id.
     * @param name the text of the note.
     * @param lon lon
     * @param lat lat
     * @param altim elevation
     * @param azim azimuth
     * @param path image path.
     * @param ts the timestamp.
     */
    public Image( long id, String name, double lon, double lat, double altim, double azim, String path, String ts ) {
        this.id = id;
        if (name != null) {
            this.name = name;
        } else {
            this.name = IMAGE_NOTE;
        }
        this.lon = lon;
        this.lat = lat;
        this.altim = altim;
        this.azim = azim;
        this.path = path;
        this.ts = ts;
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

    /**
     * @return teh elevation.
     */
    public double getAltim() {
        return altim;
    }

    /**
     * @return the azimuth.
     */
    public double getAzim() {
        return azim;
    }

    public String getName() {
        if (name.length() == 0) {
            name = IMAGE_NOTE;
        }
        return name;
    }

    /**
     * @return the image path.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the timestamp.
     */
    public String getTs() {
        return ts;
    }

    @SuppressWarnings("nls")
    public String toKmlString() {
        File img = new File(path);
        String imgName = img.getName();

        String name = Utilities.makeXmlSafe(this.name);
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        if (name != null && name.length() > 0) {
            sB.append("<name>").append(name).append(" (").append(ts).append(")").append("</name>\n");
        } else {
            sB.append("<name>").append(ts).append("</name>\n");
        }
        sB.append("<description><![CDATA[<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
        sB.append("<html><head><title></title>");
        sB.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
        sB.append("</head><body>");
        sB.append("<img src=\"" + imgName + "\" width=\"300\">");
        sB.append("</body></html>]]></description>\n");
        // sB.append("<styleUrl>#yellow-pushpin</styleUrl>\n");
        sB.append("<styleUrl>#camera-icon</styleUrl>\n");
        sB.append("<Point>\n");
        sB.append("<coordinates>").append(lon).append(",").append(lat).append(",").append(altim);
        sB.append("</coordinates>\n");
        sB.append("</Point>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }

    public boolean hasImages() {
        return true;
    }

    public List<String> getImagePaths() {
        List<String> imgs = new ArrayList<String>();
        imgs.add(path);
        return imgs;
    }
}
