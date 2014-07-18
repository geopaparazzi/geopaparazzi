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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.gpx.GpxRepresenter;
import eu.geopaparazzi.library.gpx.GpxUtilities;
import eu.geopaparazzi.library.kml.KmlRepresenter;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Represents a note (log or map).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Note implements INote, KmlRepresenter, GpxRepresenter {
    private final String name;
    private final String description;
    private final String timeStamp;
    private final long id;
    private final double lon;
    private final double lat;
    private final double altim;
    private final String category;
    private final String section;
    private final int type;
    private List<String> images = null;

    /**
     * A wrapper for a note.
     * 
     * @param id the note's id.
     * @param name the text of the note.
     * @param description a description or the date if available.
     * @param timeStamp the timestamp.
     * @param lon lon
     * @param lat lat
     * @param altim elevation
     * @param category the category.
     * @param section the section .
     * @param type teh type.
     */
    public Note( long id, String name, String description, String timeStamp, double lon, double lat, double altim,
            String category, String section, int type ) {
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
        if (timeStamp != null) {
            this.timeStamp = timeStamp;
        } else {
            this.timeStamp = ""; //$NON-NLS-1$
        }
        if (category != null) {
            this.category = category;
        } else {
            this.category = ""; //$NON-NLS-1$
        }
        this.lon = lon;
        this.lat = lat;
        this.altim = altim;
        this.section = section;
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

    /**
     * @return the elevation.
     */
    public double getAltim() {
        return altim;
    }

    public String getName() {
        return name;
    }

    /**
     * @return description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return form json.
     */
    public String getForm() {
        return section;
    }

    /**
     * @return category.
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return the type.
     */
    public int getType() {
        return type;
    }

    @SuppressWarnings("nls")
    public String toKmlString() throws Exception {
        images = new ArrayList<String>();
        String name = Utilities.makeXmlSafe(this.name);
        StringBuilder sB = new StringBuilder();
        sB.append("<Placemark>\n");
        // sB.append("<styleUrl>#red-pushpin</styleUrl>\n");
        sB.append("<styleUrl>#info-icon</styleUrl>\n");
        sB.append("<name>").append(name).append("</name>\n");
        sB.append("<description>\n");

        if (section != null && section.length() > 0) {
            sB.append("<![CDATA[\n");
            JSONObject sectionObject = new JSONObject(section);
            if (sectionObject.has(FormUtilities.ATTR_SECTIONNAME)) {
                String sectionName = sectionObject.getString(FormUtilities.ATTR_SECTIONNAME);
                sB.append("<h1>").append(sectionName).append("</h1>\n");
            }

            List<String> formsNames = TagsManager.getFormNames4Section(sectionObject);
            for( String formName : formsNames ) {
                sB.append("<h2>").append(formName).append("</h2>\n");

                sB.append("<table style=\"text-align: left; width: 100%;\" border=\"1\" cellpadding=\"5\" cellspacing=\"2\">");
                sB.append("<tbody>");

                JSONObject form4Name = TagsManager.getForm4Name(formName, sectionObject);
                JSONArray formItems = TagsManager.getFormItems(form4Name);
                for( int i = 0; i < formItems.length(); i++ ) {
                    JSONObject formItem = formItems.getJSONObject(i);
                    if (!formItem.has(FormUtilities.TAG_KEY)) {
                        continue;
                    }

                    String type = formItem.getString(FormUtilities.TAG_TYPE);
                    String key = formItem.getString(FormUtilities.TAG_KEY);
                    String value = formItem.getString(FormUtilities.TAG_VALUE);

                    if (type.equals(FormUtilities.TYPE_PICTURES)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String[] imageSplit = value.split(";");
                        for( String image : imageSplit ) {
                            File imgFile = new File(image);
                            String imgName = imgFile.getName();
                            sB.append("<tr>");
                            sB.append("<td colspan=\"2\" style=\"text-align: left; vertical-align: top; width: 100%;\">");
                            sB.append("<img src=\"" + imgName + "\" width=\"300\">");
                            sB.append("</td>");
                            sB.append("</tr>");

                            images.add(image);
                        }
                    } else if (type.equals(FormUtilities.TYPE_MAP)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        sB.append("<tr>");
                        String image = value.trim();
                        File imgFile = new File(image);
                        String imgName = imgFile.getName();
                        sB.append("<td colspan=\"2\" style=\"text-align: left; vertical-align: top; width: 100%;\">");
                        sB.append("<img src=\"" + imgName + "\" width=\"300\">");
                        sB.append("</td>");
                        sB.append("</tr>");
                        images.add(image);
                    } else if (type.equals(FormUtilities.TYPE_SKETCH)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String[] imageSplit = value.split(";");
                        for( String image : imageSplit ) {
                            File imgFile = new File(image);
                            String imgName = imgFile.getName();
                            sB.append("<tr>");
                            sB.append("<td colspan=\"2\" style=\"text-align: left; vertical-align: top; width: 100%;\">");
                            sB.append("<img src=\"" + imgName + "\" width=\"300\">");
                            sB.append("</td>");
                            sB.append("</tr>");

                            images.add(image);
                        }
                    } else {
                        sB.append("<tr>");
                        sB.append("<td style=\"text-align: left; vertical-align: top; width: 50%;\">");
                        sB.append(key);
                        sB.append("</td>");
                        sB.append("<td style=\"text-align: left; vertical-align: top; width: 50%;\">");
                        sB.append(value);
                        sB.append("</td>");
                        sB.append("</tr>");
                    }
                }
                sB.append("</tbody>");
                sB.append("</table>");
            }
            sB.append("]]>\n");
        } else if (description != null) {
            String description = Utilities.makeXmlSafe(this.description);
            sB.append(description);
            sB.append("\n");
            sB.append(timeStamp);
        }

        sB.append("</description>\n");
        sB.append("<gx:balloonVisibility>1</gx:balloonVisibility>\n");
        sB.append("<Point>\n");
        sB.append("<coordinates>").append(lon).append(",").append(lat).append(",0</coordinates>\n");
        sB.append("</Point>\n");
        sB.append("</Placemark>\n");

        return sB.toString();
    }

    public boolean hasImages() {
        return images != null && images.size() > 0;
    }

    public List<String> getImagePaths() {
        if (images == null) {
            try {
                images = FormUtilities.getImages(section);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return images;
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
        String description = Utilities.makeXmlSafe(this.description);
        description = description.replaceAll("\n", "; "); //$NON-NLS-1$//$NON-NLS-2$
        String name = Utilities.makeXmlSafe(this.name);
        name = name.replaceAll("\n", "; "); //$NON-NLS-1$//$NON-NLS-2$
        String wayPointString = GpxUtilities.getWayPointString(lat, lon, altim, name, description);
        return wayPointString;
    }
}
