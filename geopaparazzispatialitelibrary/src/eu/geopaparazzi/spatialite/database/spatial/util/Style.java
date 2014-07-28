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
package eu.geopaparazzi.spatialite.database.spatial.util;

/**
 * Simple style for shapes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class Style {
    /**
     * 
     */
    public long id;
    /**
     * 
     */
    public String name;
    /**
     * 
     */
    public float size = 5;
    /**
     * 
     */
    public String fillcolor = "red";
    /**
     * 
     */
    public String strokecolor = "black";
    /**
     * 
     */
    public float fillalpha = 0.3f;
    /**
     * 
     */
    public float strokealpha = 1.0f;
    /**
     * WKT shape name.
     */
    public String shape = "square";
    /**
     * The stroke width.
     */
    public float width = 3f;
    /**
     * The text size.
     */
    public float labelsize = 5f;

    /**
     * Field to use for labeling.
     */
    public String labelfield = "";
    /**
     * Defines if the labeling is enabled.
     * 
     * <ul>
     * <li>0 = false</li>
     * <li>1 = true</li>
     * </ul>
     */
    public int labelvisible = 0;

    /**
     * Defines if the layer is enabled.
     * 
     * <ul>
     * <li>0 = false</li>
     * <li>1 = true</li>
     * </ul>
     */
    public int enabled = 0;
    /**
     * Vertical order of the layer. 
     */
    public int order = 0;
    /**
     * The pattern to dash lines.
     */
    public String dashPattern = "";
    /**
     * Min possible zoom level.
     */
    public int minZoom = 0;
    /**
     * Max possible zoom level.
     */
    public int maxZoom = 22;
    /**
     * Decimation factor for geometries.
     */
    public float decimationFactor = 0.00001f;

    /**
     * @return a string that can be used in a sql insert statement with 
     *        all the values placed.
     */
    public String insertValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(name);
        sb.append("', ");
        sb.append(size);
        sb.append(", '");
        sb.append(fillcolor);
        sb.append("', '");
        sb.append(strokecolor);
        sb.append("', ");
        sb.append(fillalpha);
        sb.append(", ");
        sb.append(strokealpha);
        sb.append(", '");
        sb.append(shape);
        sb.append("', ");
        sb.append(width);
        sb.append(", ");
        sb.append(labelsize);
        sb.append(", '");
        sb.append(labelfield);
        sb.append("', ");
        sb.append(labelvisible);
        sb.append(", ");
        sb.append(enabled);
        sb.append(", ");
        sb.append(order);
        sb.append(", '");
        sb.append(dashPattern);
        sb.append("', ");
        sb.append(minZoom);
        sb.append(", ");
        sb.append(maxZoom);
        sb.append(", ");
        sb.append(decimationFactor);
        return sb.toString();
    }

}
