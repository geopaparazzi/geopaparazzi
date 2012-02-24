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

import org.json.JSONArray;
import org.json.JSONObject;

import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gpx.GpxRepresenter;
import eu.geopaparazzi.library.gpx.GpxUtilities;
import eu.geopaparazzi.library.kml.KmlRepresenter;

/**
 * Represents a note (log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Note implements KmlRepresenter, GpxRepresenter {
    private final String name;
    private final String description;
    private final long id;
    private final double lon;
    private final double lat;
    private final double altim;
    private final String form;
    private final int type;

    /**
     * A wrapper for a note.
     * 
     * @param id the note's id.
     * @param name the text of the note.
     * @param description a description or the date if available.
     * @param lon
     * @param lat
     * @param altim
     * @param form the form.
     * @param type 
     */
    public Note( long id, String name, String description, double lon, double lat, double altim, String form, int type ) {
        this.id = id;
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
        this.form = form;
        this.type = type;
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

    public double getAltim() {
        return altim;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getForm() {
        return form;
    }

    public int getType() {
        return type;
    }

    @SuppressWarnings("nls")
    public String toKmlString() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        sB.append("<styleUrl>#red-pushpin</styleUrl>\n");
        sB.append("<name>").append(name).append("</name>\n");
        sB.append("<description>\n");
        sB.append("<![CDATA[\n");
        String descr = description.replaceAll("\n", "</BR></BR>");
        sB.append("<p>").append(descr).append("</p>\n");

        if (form != null) {
            JSONArray formItems = TagsManager.getFormItems(new JSONObject(form));

            // table head
            // <table style="text-align: left; width: 80%;" border="1" cellpadding="5"
            sB.append("<table style=\"text-align: left; width: 100%;\" border=\"1\" cellpadding=\"5\" cellspacing=\"2\">");
            // <tbody>
            sB.append("<tbody>");

            for( int i = 0; i < formItems.length(); i++ ) {
                JSONObject formItem = formItems.getJSONObject(i);
                String key = formItem.getString(FormUtilities.TAG_KEY);
                String value = formItem.getString(FormUtilities.TAG_VALUE);

                sB.append("<tr>");
                sB.append("<td style=\"text-align: left; vertical-align: top; width: 50%;\">");
                sB.append(key);
                sB.append("</td>");
                sB.append("<td style=\"text-align: left; vertical-align: top; width: 50%;\">");
                sB.append(value);
                sB.append("</td>");
                sB.append("</tr>");
            }
            sB.append("</tbody>");
            sB.append("</table>");
        }

        sB.append("]]>\n");
        sB.append("</description>\n");
        sB.append("<gx:balloonVisibility>1</gx:balloonVisibility>\n");
        sB.append("<Point>\n");
        sB.append("<coordinates>").append(lon).append(",").append(lat).append(",0</coordinates>\n");
        sB.append("</Point>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }

    public boolean hasImage() {
        return false;
    }

    public String getImagePath() {
        return null;
    }

    public double getMinLat() {
        return lat;
    }

    public double getMinLon() {
        return lon;
    }

    public double getMaxLat() {
        return lat;
    }

    public double getMaxLon() {
        return lon;
    }

    public String toGpxString() throws Exception {
        String descr = description.replaceAll("\n", "; ");  //$NON-NLS-1$//$NON-NLS-2$
        String wayPointString = GpxUtilities.getWayPointString(lat, lon, altim, name, descr);
        return wayPointString;
    }
}
